package meatbox;

import static meatbox.Config.*;
import static meatbox.Body.*;
import static meatbox.Afflictions.*;
import static meatbox.World.*;
import static meatbox.Catalogue.*;

/**
 * =============================================================================
 *  SIM  -  the rules. One call of day() = one day for the whole world.
 * =============================================================================
 *  Every function here mirrors one routine in meatbox.asm, in the same order,
 *  calling the random number generator in the same order. That is what makes
 *  the Java and assembly runs produce IDENTICAL statistics from one seed.
 *
 *  If you change the rules, the parity check (README) will stop matching -
 *  that is expected. It only proves the PORT; after that, the Java is the boss.
 *
 *  A person's day (update):
 *    1 age   2 conditions (Health)   2b food + cold   3 bloodstream + chemistry
 *    4 body: heal, age, wear, bones   5 abilities + pain + mind (Psych)
 *    6 pregnancy   7 new disease?   8 action   9 death check
 *
 *  v0.3: 48 organs/muscles/joints, 206 bones, ~230 conditions, psychology.
 *
 *  v0.2: seasons, wild food, a shared food store, firewood heating, and what
 *  hunger and cold do to the body. Parity with meatbox.asm is gone from here on.
 */
final class Sim {
    final World w;
    final Rng rng;

    final Health h;

    Sim(World w) { this.w = w; this.rng = w.rng; this.h = new Health(w); }

    // =====================================================================================
    //  ONE DAY
    // =====================================================================================
    void day() {
        w.day++;
        int doy = w.day % 365;

        // ---- season & weather ----
        if (doy == 60) {                                       // early March: this year's luck
            w.harvestPct = rng.below(FAMINE_YEAR_IN) == 0 ? FAMINE_PCT
                         : HARVEST_MIN + rng.below(HARVEST_RNG + 1);
        }
        w.anomaly = w.anomaly * WEATHER_KEEP / 8 + rng.below(2 * WEATHER_KICK + 1) - WEATHER_KICK;
        w.outT = Climate.MEAN[doy] + w.anomaly;

        // yesterday's foraging return, folded into a ~6-month running average
        if (w.gatherTry > 0) {
            int r = (int) (w.gatherGot * 65536 / w.gatherTry);
            w.returnQ += (r - w.returnQ) / 180;
        }
        w.gatherTry = w.gatherGot = 0;

        treeTick();
        Food.growDay(w);

        // ---- the fire: cooking all year, heating when the hut is colder than comfort.
        // Wood is in loads; burnAcc holds the hundredths of a load left over.
        int heatDef = Math.max(0, COLD_COMFORT - (w.outT + SHELTER_BONUS));      // hundredths C
        long burn = (long) w.popCount * (BURN_COOK * 100 + BURN_HEAT * heatDef) / 100;
        long have = (long) w.wood * 100 + w.burnAcc;
        if (have >= burn) { have -= burn; w.fireQ = 65536; }
        else { w.fireQ = burn == 0 ? 65536 : (int) (have * 65536 / burn); have = 0; }
        w.wood = (int) (have / 100);
        w.burnAcc = (int) (have % 100);

        // ---- the store: some spoils; today's ration = store / yesterday's demand
        w.food -= w.food / STORE_SPOIL;
        // Under RATION_DAYS left, everyone tightens their belt to stretch it.
        w.foodQ = w.demandPrev == 0 ? 65536
                : (int) Math.min(65536, w.food * 65536 / (w.demandPrev * RATION_DAYS));
        long secure = (long) w.popCount * RATION * STORE_DAYS * STORE_FERTILE_PCT / 100;
        int fq = secure == 0 ? 65536 : (int) Math.min(65536, w.food * 65536 / secure);
        int rpct = w.returnQ * 100 / 65536;
        int rq = rpct >= RETURN_FERTILE_HI ? 65536 : rpct <= RETURN_FERTILE_LO ? 0
               : (rpct - RETURN_FERTILE_LO) * 65536 / (RETURN_FERTILE_HI - RETURN_FERTILE_LO);
        w.fertQ = Math.min(fq, rq);
        w.demandPrev = w.demand;
        w.demand = 0;

        w.menPrev = w.menCount;          // "is there a man?" uses YESTERDAY's count
        w.menCount = 0;

        int hi = w.popHi;                // babies born today start tomorrow
        for (int i = 0; i < hi; i++) {
            if (!w.alive[i]) continue;
            update(i);
            w.personDays++;
        }
    }

    // =====================================================================================
    //  TREES: "random tick" - only TREE_TICKS random tiles change per day
    // =====================================================================================
    void treeTick() {
        byte[] map = w.map;
        for (int t = 0; t < TREE_TICKS; t++) {
            int i = rng.below(MAP_SIZE);
            int tile = map[i];
            if (tile == T_GRASS) {
                if (rng.next32() < RAIN_P32) { map[i] = T_SEED; continue; }   // wind-blown seed
                int d = rng.below(8);
                int nx = (i & (MAP_W - 1)) + DIR_DX[d];
                int ny = (i >>> 6) + DIR_DY[d];
                if (Integer.compareUnsigned(nx, MAP_W) >= 0) continue;
                if (Integer.compareUnsigned(ny, MAP_H) >= 0) continue;
                if (map[ny * MAP_W + nx] != T_TREE) continue;
                if (rng.next32() < SEED_P32) map[i] = T_SEED;                // seed from neighbour
            } else if (tile == T_TREE) {
                // mature: nothing to do
            } else if (tile == T_STUMP) {
                long r = rng.next32();
                if (r >= ROT_P32) continue;
                map[i] = (r & 0xFFFF) % 100 < COPPICE_PCT ? T_SAPLING : T_GRASS;  // resprout or rot
            } else {                                                          // seed/sapling/young
                if (rng.next32() < GROW_P32) map[i]++;
            }
        }
    }

    // =====================================================================================
    //  ONE PERSON, ONE DAY
    // =====================================================================================
    void update(int p) {
        final World w = this.w;
        final Health h = this.h;
        final int hb = p * NPARTS;       // first HP slot of this person
        final int bb = p * 8;            // first blood slot
        final int b2 = p * 4;            // first blood2 slot
        final char[] hp = w.hp, blood = w.blood;

        // ---- 1. AGE -----------------------------------------------------------
        int ageDays = w.age[p];
        if (ageDays < 0xFFFF) ageDays++;
        w.age[p] = (char) ageDays;
        final int years = Config.years(ageDays);
        if (years >= FERTILE_MIN && w.sex[p] == SEX_M) w.menCount++;
        final boolean monthly = (w.day + p) % 30 == 0;

        // ---- 2. CONDITIONS (Health.afflictions) ----------------------------------
        h.begin();
        h.afflictions(p, years, monthly);
        int painSum = h.painSum;
        final int sev = h.sev, drv = h.drv;

        // ---- 2b. FOOD AND COLD -----------------------------------------------------
        // Cold: how far the hut is below comfort, minus whatever the fire covers.
        int coldDef = Math.max(0, COLD_COMFORT - (w.outT + SHELTER_BONUS));
        coldDef -= (int) (((long) coldDef * w.fireQ) >>> 16);

        int need = Food.baseNeed(w, p, years);
        need += need * NEED_COLD_PCT * coldDef / 10000;        // shivering burns food
        int fat = w.fat[p];
        if (fat < FAT_IMMUNE)                                  // metabolism slows when starving
            need -= need * NEED_THRIFT_PCT * (FAT_IMMUNE - fat) / (100 * FAT_IMMUNE);
        int want = fat < HP_MAX ? need + need * REFEED_PCT / 100 : need;
        int ate;
        int mom = w.mother[p];
        if (years < 1 && mom != 0xFFFF && w.alive[mom]) {      // breastfed: mother eats for both
            int milkQ = w.foodQ;
            if (w.fat[mom] < FAT_MILK) milkQ = milkQ * w.fat[mom] / FAT_MILK;
            ate = (int) (((long) want * milkQ) >>> 16);
        } else {
            w.demand += want;
            int q = w.foodQ;                                   // short rations aren't shared equally
            if (q < 65536) q = Math.max(0, Math.min(65536, 2 * q - ((w.rank[p] & 0xFF) << 8)));
            ate = (int) Math.min(w.food, ((long) want * q) >>> 16);
            w.food -= ate;
            w.eaten += ate;
            if (years < 1) ate = ate * ORPHAN_MILK_PCT / 100;   // no milk: a poor substitute
        }
        int bal = ate - need, resDays = Food.reserveDays(years) * (24 + w.konst[p]) / 32;
        if (bal >= 0) fat += bal * REFEED_EFF * (HP_MAX / 100) / (need * resDays);
        else fat += bal * HP_MAX / (need * resDays);
        fat = Math.max(0, Math.min(HP_MAX, fat));
        w.fat[p] = (char) fat;
        if (fat < FAT_HUNGER_PAIN) painSum += (FAT_HUNGER_PAIN - fat) / 100;

        // How much the core cools: babies 3x, toddlers 2x, the old 1.5x; thin people
        // up to 2x (no insulation, no fuel to shiver with); constitution +-25 %.
        int ageQ = years < 1 ? 12 : years < 5 ? 8 : years < 15 ? 5 : years >= 70 ? 6 : 4;
        long drop = (long) coldDef * COLD_K_ADULT * ageQ / 400;
        drop = drop * (HP_MAX - Math.min(fat, 5000)) / 5000;
        drop = drop * (24 + w.konst[p]) / 32;
        w.warmth[p] = (char) Math.max(0, HP_MAX - drop * HP_MAX / 700);

        // ---- 3. BLOODSTREAM -------------------------------------------------------
        h.chemistry(p, years, fat);
        int vol = Math.min(HP_MAX, blood[bb + B_VOL] + VOL_REGEN);
        blood[bb + B_VOL] = (char) vol;
        // breathing needs both lungs' worth of tissue, an open airway and a working diaphragm
        int lungs = (hp[hb + L_LUNG] + hp[hb + R_LUNG]) / 2;
        int breath = Math.min(Math.min(HP_MAX, hp[hb + AIRWAY] * 2), Math.min(HP_MAX, hp[hb + CORE_M] * 3 / 2));
        int o2 = (lungs * 4 / 10 + 6000) * breath / HP_MAX;     // SpO2 = 60% + 40% * lungs
        blood[bb + B_O2] = (char) o2;
        int hbv = w.blood2[b2 + B2_HB];
        int carry = (int) ((long) o2 * Math.min(HP_MAX, hbv * 10 / 8) / HP_MAX);   // anaemia: the heart compensates down to ~80% Hb [tune]
        int volc = Math.min(HP_MAX, (vol * 5) >>> 2);           // vessels hide early loss
        int t = (int) ((long) carry * volc / HP_MAX);
        int pump = Math.min(HP_MAX, hp[hb + HEART] * 15 / 10);  // heart reserve
        pump = pump * (6000 + hp[hb + VESSELS] * 4 / 10) / HP_MAX;   // hardened arteries
        int perf = Math.min(HP_MAX, (int) ((long) pump * t / HP_MAX));
        blood[bb + B_PERF] = (char) perf;
        h.pressure(p, vol);

        int imm = h.immunity(p, years) * IMMUNE_CLEAR / HP_MAX;
        int path = blood[bb + B_PATH];
        if (path != 0) {
            path += path >>> PATH_GROWTH_SH;                   // infection grows...
            if (fat < FAT_IMMUNE) imm = imm * (40 + 60 * fat / FAT_IMMUNE) / 100;  // [src] malnutrition
            path = Math.max(0, path - imm);                    // ...immune system clears
            path = Math.min(HP_MAX, path);
            blood[bb + B_PATH] = (char) path;
        }
        int meta = w.blood2[b2 + B2_META];
        int temp = Math.min(4300, (path >>> 3) + 3700 + (meta - HP_MAX) / 20);   // fever; thyroid heat
        temp = (int) Math.max(2000, temp - drop);              // cold
        blood[bb + B_TEMP] = (char) temp;

        int kid = hp[hb + L_KIDNEY] + hp[hb + R_KIDNEY];
        int clear = (int) (((long) (kid + hp[hb + LIVER]) * perf) / (100L * HP_MAX));
        int waste = blood[bb + B_WASTE] + WASTE_GEN - clear;
        waste = Math.min(HP_MAX, Math.max(0, waste));
        blood[bb + B_WASTE] = (char) waste;

        int hr = years < 1 ? 130 : years < 10 ? 95 : 75;
        hr += (HP_MAX - vol) >>> 6;
        hr += (temp - 3700) >> 4;                              // fever up, hypothermia down
        hr += (meta - HP_MAX) / 200;
        if (hbv < HP_MAX) hr += (HP_MAX - hbv) / 150;          // anaemia: the heart races
        blood[bb + B_HR] = (char) Math.max(20, Math.min(220, hr));

        // SYSTEMIC STRESS: "the blood is failing the organs", one number
        int glu = w.blood2[b2 + B2_GLU];
        int stress = 0;
        if (perf < PERF_OK) stress += PERF_OK - perf;          // starvation of O2
        if (path > SEPSIS_T) stress += path - SEPSIS_T;        // sepsis
        if (waste > UREMIA_T) stress += (waste - UREMIA_T) >>> 1;  // toxins
        if (temp > HEAT_T) stress += (temp - HEAT_T) << 2;     // heat
        int coldStress = temp < HYPO_T ? (HYPO_T - temp) << 1 : 0;   // hypothermia [tune]
        stress += coldStress;
        int sugarStress = glu > GLU_COMA ? (glu - GLU_COMA) : glu < 250 ? (250 - glu) * 20 : 0;  // ketoacidosis / hypo
        stress += sugarStress;
        stress = Math.min(16383, stress);

        // ---- 4. BODY: heal, age, wear, suffer the stress ----------------------------
        int sen = 0;
        int over = years - ((w.konst[p] & 0xFF) + SEN_ONSET - 8);
        if (over > 0) sen = Math.min(0xFFFF, (over * over) >>> SEN_SHIFT);
        final int stress4 = stress * 4;
        final int healPct = fat >= FAT_HEAL ? 100 : 25 + 75 * fat / FAT_HEAL;   // hungry heal slowly
        final boolean worn = h.wear(p);
        final int[] wear = h.wear;
        final int age16 = sen;                                  // ageing loss = sen * AGEW / 16
        long painParts = 0;
        // Fast path (most days for most people): no ageing, stress or wear, so only
        // parts below full can change. Same result as the full loop, less work.
        if (age16 == 0 && stress4 == 0 && !worn) {
            for (int k = 0; k < NPARTS; k++) {
                int v = hp[hb + k];
                if (v == 0 || v >= HP_MAX) continue;
                v += healPct == 100 ? REGEN[k] : REGEN[k] * healPct / 100;
                if (v > HP_MAX) v = HP_MAX;
                hp[hb + k] = (char) v;
                painParts += (HP_MAX - v) * PAINW[k];
            }
        } else for (int k = 0; k < NPARTS; k++) {
            int v = hp[hb + k];
            if (v == 0) continue;                              // lost stays lost
            if (worn && wear[k] != 0) v -= wear[k];            // scarring: no healing today
            else v += healPct == 100 ? REGEN[k] : REGEN[k] * healPct / 100;
            if (age16 != 0) v -= (age16 * AGEW[k]) >>> 4;
            if (stress4 != 0) v -= (stress4 * O2X64[k]) >>> 16; // = stress * o2 / 256
            v = v < 0 ? 0 : v > HP_MAX ? HP_MAX : v;
            hp[hb + k] = (char) v;
            painParts += (HP_MAX - v) * PAINW[k];
        }
        if (fat < FAT_WASTING) {                               // [tune] reserve gone: wasting
            int s = Health.ageScale(FAT_WASTING - fat, years); // 0..1000, children x2-4
            h.hurt(p, HEART,  s * 3 / 10, STARVATION.id);      // cardiac wasting, arrhythmia
            for (int m : LIMB_MUSCLES) h.hurt(p, m, s / 5, STARVATION.id);   // muscle
            h.hurt(p, SMALL_INT, s * 3 / 20, STARVATION.id);   // gut lining
            h.hurt(p, LIVER,  s / 10,     STARVATION.id);
        }
        if (temp < HYPO_HEART_T)                               // [tune] cold heart misfires
            h.hurt(p, HEART, HYPO_HEART_T - temp, HYPOTHERMIA.id);
        Skeleton.dailyHeal(w, p, years, healPct);
        if (monthly) h.monthly(p, years);

        // ---- 5. ABILITIES + PAIN + MIND ---------------------------------------------
        int pain = (int) Math.min(HP_MAX, (painParts >>> PAIN_SHIFT) + painSum * PAIN_AFF_SCALE + w.bonePain[p]);
        blood[bb + B_PAIN] = (char) pain;
        h.functions(p, years, perf, pain);
        w.bed[p] = h.bedAff || pain >= PAIN_BED || perf < BED_PERF || temp >= BED_TEMP
                 || temp < HYPO_BED_T || fat < FAT_BED || w.boneBed[p];
        if (w.bed[p] && w.bedDays[p] < 31) w.bedDays[p]++;
        h.delirium(p, years, perf, temp, waste, glu, path);
        Psych.compute(w, h, p, years);
        h.workCapacity(p);

        // ---- 6. PREGNANCY ---------------------------------------------------------------
        int pr = w.preg[p];
        if (pr > 0) {
            w.preg[p] = (short) --pr;
            if (pr == 0) giveBirth(p);
        } else if (pr < 0) {
            w.preg[p] = (short) (pr + 1);                       // breastfeeding pause
        } else if (w.sex[p] == SEX_F && years >= FERTILE_MIN && years <= FERTILE_MAX
                   && w.menPrev != 0 && fat > FAT_FERTILE_LO) {
            // fertility: conditions, and the ovaries / womb themselves
            long fert = (long) h.fertPct * Math.min(hp[hb + GONADS], hp[hb + UTERUS]) / HP_MAX;
            if (rng.next32() < conceiveP32(fat) * fert / 100) {
                w.preg[p] = GESTATION_DAYS;
                w.pregFlags[p] = 0;
            }
        }

        // ---- 7. NEW DISEASE? ----------------------------------------------------------------
        h.rollTrigger(p, DISEASE, years < CHILD_AGE, years);

        // ---- 8. ACTION ------------------------------------------------------------------------
        act(p, years);

        // ---- 9. DEATH CHECK ---------------------------------------------------------------------
        if (w.doomed[p]) { w.kill(p); return; }                // sudden death; cause already set
        if (Psych.suicide(w, p, years)) { w.cause[p] = (char) SUICIDE.id; w.kill(p); return; }
        if (blood[bb + B_VOL] < VOL_DEATH) {                    // bled out
            int b = w.bleeder[p];
            w.cause[p] = (char) (b != 0 ? b : ORGANFAIL.id);
            w.kill(p);
            return;
        }
        boolean vitalGone = false;
        for (int k : VITALS) if (hp[hb + k] == 0) { vitalGone = true; break; }
        if (!vitalGone && ((hp[hb + L_LUNG] | hp[hb + R_LUNG]) == 0 || (hp[hb + L_KIDNEY] | hp[hb + R_KIDNEY]) == 0))
            vitalGone = true;
        if (vitalGone) {
            if (w.cause[p] == 0) {
                // indirect death: blame the worst active affliction; else "Old age"
                // if old; else the last serious illness; else "Organ failure".
                int c;
                if (coldStress * 2 > stress) c = HYPOTHERMIA.id;   // mostly the cold
                else if (sugarStress * 2 > stress && glu > GLU_COMA) c = DKA.id;
                else if (sev >= SERIOUS_SEV) c = drv;
                else if (fat == 0) c = STARVATION.id;
                else if (years >= OLD_AGE_YEARS) c = OLDAGE.id;
                else if (w.recent[p] != 0) c = w.recent[p];
                else c = ORGANFAIL.id;
                w.cause[p] = (char) c;
            }
            w.kill(p);
        }
    }

    /** Hungry women conceive less: full chance above FAT_FERTILE_HI, none at FAT_FERTILE_LO;
     *  and fewer when the store is low (w.fertQ). */
    long conceiveP32(int fat) {
        long c = (CONCEIVE_P32 * w.fertQ) >>> 16;
        if (fat >= FAT_FERTILE_HI) return c;
        return c * (fat - FAT_FERTILE_LO) / (FAT_FERTILE_HI - FAT_FERTILE_LO);
    }

    // =====================================================================================
    //  DISEASE ROLLS (kept here so behaviour code can call them as before)
    // =====================================================================================
    /** Does something of this trigger (CHOP, WALK, DISEASE) happen to p today? */
    void rollTrigger(int p, int trig, boolean child, int years) { h.rollTrigger(p, trig, child, years); }
    void hurt(int p, int part, int amount, int id) { h.hurt(p, part, amount, id); }

    // =====================================================================================
    //  BEHAVIOUR: rest, chop, or walk - limited by what the body can do
    // =====================================================================================
    static final int ARMS = (1 << LARM) | (1 << RARM);
    static final int LEGS = (1 << LLEG) | (1 << RLEG);

    void act(int p, int years) {
        if (w.bed[p]) return;                                  // bedridden: stays put
        int c = w.cap[p];

        chop:
        if (years >= CHOP_AGE
                && w.wood < w.popCount * WOOD_TARGET           // wood actually needed
                && w.blood[p * 8 + B_PAIN] < PAIN_WORK         // not in too much pain
                && (c & (1 << EYES)) != 0                      // can see the tree
                && (c & ARMS) != 0) {                          // has a usable arm
            if (rng.next32() >= CHOP_P32) break chop;
            if ((c & ARMS) != ARMS && (rng.next32() & 1) == 0) break chop;   // one arm: half as often
            int px = w.x[p] & 0xFF, py = w.y[p] & 0xFF;
            for (int d = 0; d < 8; d++) {
                int nx = px + DIR_DX[d], ny = py + DIR_DY[d];
                if (Integer.compareUnsigned(nx, MAP_W) >= 0) continue;
                if (Integer.compareUnsigned(ny, MAP_H) >= 0) continue;
                int i = ny * MAP_W + nx;
                if (w.map[i] == T_TREE) {
                    if (rng.next32() < CLEAR_P32) w.map[i] = T_STUMP;   // stand used up
                    w.wood += CHOP_YIELD;
                    rollTrigger(p, CHOP, false, years);        // axe accidents
                    return;
                }
            }
        }

        // ---- forage (v0.2): only while the store is below target ----
        boolean foraging = false;
        if (years >= GATHER_AGE
                && w.food < (long) w.popCount * RATION * STORE_DAYS
                && w.blood[p * 8 + B_PAIN] < PAIN_WORK
                && (c & ARMS) != 0) {
            Food.forageAct(w, p, years, c);
            foraging = true;
        }

        // ---- walk ----
        int m = w.mother[p];
        if (years < FOLLOW_AGE && m != 0xFFFF && w.alive[m]) { // toddlers stay with mother
            w.x[p] = w.x[m];
            w.y[p] = w.y[m];
            return;
        }
        int legs = Integer.bitCount(c & LEGS);
        if (legs < 2) {                                        // 1 leg: 1/2 days. 0: crawl 1/4
            long r = rng.next32();
            if ((r & (legs == 1 ? 1 : 3)) != 0) return;
        }
        // foragers head for food half the time; otherwise (or if nothing is better) wander
        if (!(foraging && (rng.next32() & 1) == 0 && Food.stepToFood(w, p))) {
            int dx = rng.below(3) - 1;
            int dy = rng.below(3) - 1;
            int nx = (w.x[p] & 0xFF) + dx, ny = (w.y[p] & 0xFF) + dy;
            if (Integer.compareUnsigned(nx, MAP_W) < 0) w.x[p] = (byte) nx;
            if (Integer.compareUnsigned(ny, MAP_H) < 0) w.y[p] = (byte) ny;
        }
        rollTrigger(p, WALK, false, years);                    // trips and falls
    }

    // =====================================================================================
    //  BIRTH
    // =====================================================================================
    void giveBirth(int m) {
        w.preg[m] = -POSTPARTUM_DAYS;
        h.rollEvent(m, BIRTH, -1);                             // the mother's risks, stillbirth first
        if (h.hasAff(m, STILLBIRTH.id)) {
            w.stillbirths++;
            w.pregFlags[m] = 0;
            w.log(w.personName(m) + ": stillbirth");
            Psych.grieve(w, h, m, 8000);
            return;
        }
        int c = w.newPerson();
        if (c != -1) {
            w.x[c] = w.x[m];
            w.y[c] = w.y[m];
            w.mother[c] = (char) m;
            int f = pickFather(m);
            w.father[c] = (char) (f < 0 ? 0xFFFF : f);
            Genes.inherit(w, c, m, f);
            w.births++;
            w.log(w.personName(c) + " born to " + w.personName(m));
            h.rollEvent(c, NEWBORN, -1);                       // the baby's risks
            Genes.express(w, h, c);
        }
        w.pregFlags[m] = 0;
    }

    /** No couples yet: the father is a random living man of 16+ (behaviour code can replace this). */
    int pickFather(int m) {
        for (int k = 0; k < 24; k++) {
            int j = rng.below(w.popHi);
            if (j != m && w.alive[j] && w.sex[j] == SEX_M && Config.years(w.age[j]) >= FERTILE_MIN) return j;
        }
        return -1;
    }
}
