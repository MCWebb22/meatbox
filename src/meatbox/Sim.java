package meatbox;

import static meatbox.Config.*;
import static meatbox.Body.*;
import static meatbox.Afflictions.*;
import static meatbox.World.*;

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
 *    1 age   2 afflictions   3 bloodstream   4 body   5 capability + pain
 *    6 pregnancy   7 new disease?   8 action   9 death check
 */
final class Sim {
    final World w;
    final Rng rng;

    Sim(World w) { this.w = w; this.rng = w.rng; }

    // =====================================================================================
    //  ONE DAY
    // =====================================================================================
    void day() {
        w.day++;
        treeTick();

        // firewood: each person burns 1/365 of a log per day. Keep the leftover
        // fraction in burnAcc so nothing is lost to rounding.
        int acc = w.burnAcc + w.popCount;
        while (acc >= 365) {
            acc -= 365;
            if (w.wood > 0) w.wood--;
        }
        w.burnAcc = acc;

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
                map[i] = (r & 1) != 0 ? T_SEED : T_GRASS;                    // resprout or rot
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
        final int hb = p * NPARTS;       // first HP slot of this person
        final int bb = p * 8;            // first blood slot
        final int sb = p * NSLOTS;       // first affliction slot
        final char[] hp = w.hp, blood = w.blood;

        // ---- 1. AGE -----------------------------------------------------------
        int ageDays = w.age[p];
        if (ageDays < 0xFFFF) ageDays++;
        w.age[p] = (char) ageDays;
        final int years = Config.years(ageDays);
        if (years >= FERTILE_MIN && w.sex[p] == SEX_M) w.menCount++;

        // ---- 2. AFFLICTIONS -----------------------------------------------------
        int dis = 0;            // parts disabled today (bit mask)
        boolean bedAff = false;
        int painSum = 0;
        int drv = 0, sev = 0;   // the worst active affliction ("driver") and its score

        boolean any = false;
        for (int s = 0; s < NSLOTS; s++) if (w.aff[sb + s] != 0) { any = true; break; }

        if (any) {
            for (int s = 0; s < NSLOTS; s++) {
                int v = w.aff[sb + s];
                int id = v & 0xFF;
                if (id == 0) continue;
                int part = (v >>> 8) & 0xFF;
                Aff a = TABLE[id];

                if (a.has(DISABLE)) dis |= 1 << part;
                if (a.has(BEDRIDDEN)) bedAff = true;
                painSum += a.pain;

                // severity: the worst one is blamed for indirect deaths
                int score = a.dot + a.bleed + a.sepsis + 1;
                if (score > sev) { sev = score; drv = id; }

                // --- damage over time to its part
                if (a.dot != 0) {
                    int c = a.dot;
                    if (a.has(GROWS)) c += (v >>> 16) * a.dot / a.durMin;   // cancer grows
                    c = ageScale(c, years);
                    int r = rng.below(c);
                    hurt(p, part, (c >>> 1) + r, id);                      // 50%..150%
                }
                // --- bleeding / fluid loss drains blood volume
                if (a.bleed != 0) {
                    int c = ageScale(a.bleed, years);
                    int vol = blood[bb + B_VOL] - c;
                    blood[bb + B_VOL] = (char) Math.max(0, vol);
                    w.bleeder[p] = (byte) id;
                }
                // --- infection seeded into the blood
                if (a.sepsis != 0)
                    blood[bb + B_PATH] = (char) Math.min(HP_MAX, blood[bb + B_PATH] + a.sepsis);

                // --- contagion: test ONE random person; catch it if close enough
                if (a.spread != 0) {
                    int j = rng.below(w.popHi);
                    if (w.alive[j]
                            && Math.abs((w.x[j] & 0xFF) - (w.x[p] & 0xFF)) <= SPREAD_DIST
                            && Math.abs((w.y[j] & 0xFF) - (w.y[p] & 0xFF)) <= SPREAD_DIST
                            && (rng.next32() & 0xFF) < a.spread)
                        giveAff(j, id, SAME);
                }

                // --- time passes
                v = w.aff[sb + s];
                int days = v >>> 16;
                if (a.has(CHRONIC)) {
                    if (days < 0xFFFF) days++;                 // chronic: count UP
                    w.aff[sb + s] = (v & 0xFFFF) | (days << 16);
                } else {
                    days--;
                    if (days == 0) {                           // recovered
                        w.aff[sb + s] = 0;
                        if (a.has(IMMUNE)) w.immune[p] |= 1L << id;
                    } else {
                        w.aff[sb + s] = (v & 0xFFFF) | (days << 16);
                    }
                }
            }
            if (drv != 0 && sev >= SERIOUS_SEV) w.recent[p] = (byte) drv;
        }

        // ---- 3. BLOODSTREAM ------------------------------------------------------
        int vol = Math.min(HP_MAX, blood[bb + B_VOL] + VOL_REGEN);
        blood[bb + B_VOL] = (char) vol;
        int o2 = hp[hb + LUNGS] * 4 / 10 + 6000;               // SpO2 = 60% + 40% * lungs
        blood[bb + B_O2] = (char) o2;
        int volc = Math.min(HP_MAX, (vol * 5) >>> 2);          // vessels hide early loss
        int t = o2 * volc / HP_MAX;
        int pump = Math.min(HP_MAX, hp[hb + HEART] * 15 / 10); // heart reserve
        int perf = pump * t / HP_MAX;
        blood[bb + B_PERF] = (char) perf;

        int path = blood[bb + B_PATH];
        if (path != 0) {
            path += path >>> PATH_GROWTH_SH;                   // infection grows...
            path = Math.max(0, path - immuneStrength(years));  // ...immune system clears
            path = Math.min(HP_MAX, path);
            blood[bb + B_PATH] = (char) path;
        }
        int temp = Math.min(4300, (path >>> 3) + 3700);        // fever
        blood[bb + B_TEMP] = (char) temp;

        int clear = ((hp[hb + KIDNEY] * 2 + hp[hb + LIVER]) * perf) / (100 * HP_MAX);
        int waste = blood[bb + B_WASTE] + WASTE_GEN - clear;
        waste = Math.min(HP_MAX, Math.max(0, waste));
        blood[bb + B_WASTE] = (char) waste;

        int hr = years < 1 ? 130 : years < 10 ? 95 : 75;
        hr += (HP_MAX - vol) >>> 6;
        hr += (temp - 3700) >>> 4;
        blood[bb + B_HR] = (char) Math.min(220, hr);

        // SYSTEMIC STRESS: "the blood is failing the organs", one number
        int stress = 0;
        if (perf < PERF_OK) stress += PERF_OK - perf;          // starvation
        if (path > SEPSIS_T) stress += path - SEPSIS_T;        // sepsis
        if (waste > UREMIA_T) stress += (waste - UREMIA_T) >>> 1;  // toxins
        if (temp > HEAT_T) stress += (temp - HEAT_T) << 2;     // heat
        stress = Math.min(16383, stress);

        // ---- 4. BODY: heal, age, suffer the stress -------------------------------
        int sen = 0;
        int over = years - ((w.konst[p] & 0xFF) + SEN_ONSET - 8);
        if (over > 0) sen = Math.min(0xFFFF, (over * over) >>> SEN_SHIFT);
        final int stress4 = stress * 4;
        for (int k = 0; k < NPARTS; k++) {
            int h = hp[hb + k];
            if (h == 0) continue;                              // lost stays lost
            h += REGEN[k];
            if (AGEING[k] != 0) h = Math.max(0, h - sen);
            h = Math.max(0, h - ((stress4 * O2X64[k]) >>> 16)); // = stress * o2 / 256
            hp[hb + k] = (char) Math.min(HP_MAX, h);
        }

        // ---- 5. CAPABILITY + PAIN ---------------------------------------------------
        int usable = 0, painParts = 0;
        for (int k = 0; k < NPARTS; k++) {
            int h = hp[hb + k];
            if (h > USABLE_HP - 1) usable |= 1 << k;
            painParts += (HP_MAX - h) * PAINW[k];
        }
        usable &= ~dis & ~w.lame[p] & 0xFFFF;
        if ((usable & (1 << SPINE)) == 0) usable &= ~((1 << LLEG) | (1 << RLEG));  // paralysis
        w.cap[p] = (char) usable;

        int pain = Math.min(HP_MAX, (painParts >>> PAIN_SHIFT) + painSum * PAIN_AFF_SCALE);
        blood[bb + B_PAIN] = (char) pain;
        w.bed[p] = bedAff || pain >= PAIN_BED || perf < BED_PERF || temp >= BED_TEMP;

        // ---- 6. PREGNANCY ---------------------------------------------------------------
        int pr = w.preg[p];
        if (pr > 0) {
            w.preg[p] = (short) --pr;
            if (pr == 0) giveBirth(p);
        } else if (pr < 0) {
            w.preg[p] = (short) (pr + 1);                       // breastfeeding pause
        } else if (w.sex[p] == SEX_F && years >= FERTILE_MIN && years <= FERTILE_MAX
                   && w.menPrev != 0 && rng.next32() < CONCEIVE_P32) {
            w.preg[p] = GESTATION_DAYS;
        }

        // ---- 7. NEW DISEASE? ----------------------------------------------------------------
        rollTrigger(p, DISEASE, years < CHILD_AGE, years);

        // ---- 8. ACTION ------------------------------------------------------------------------
        act(p, years);

        // ---- 9. DEATH CHECK ---------------------------------------------------------------------
        if (blood[bb + B_VOL] < VOL_DEATH) {                    // bled out
            int b = w.bleeder[p];
            w.cause[p] = (byte) (b != 0 ? b : ORGANFAIL.id);
            w.kill(p);
            return;
        }
        boolean vitalGone = false;
        for (int k = 0; k < NPARTS; k++)
            if (hp[hb + k] == 0 && ((vitalMask >>> k) & 1) != 0) { vitalGone = true; break; }
        if (vitalGone) {
            if (w.cause[p] == 0) {
                // indirect death: blame the worst active affliction; else "Old age"
                // if old; else the last serious illness; else "Organ failure".
                int c;
                if (sev >= SERIOUS_SEV) c = drv;
                else if (years >= OLD_AGE_YEARS) c = OLDAGE.id;
                else if (w.recent[p] != 0) c = w.recent[p];
                else c = ORGANFAIL.id;
                w.cause[p] = (byte) c;
            }
            w.kill(p);
        }
    }

    /** Babies get hit 4x as hard, toddlers 2x. */
    static int ageScale(int v, int years) {
        if (years < 1) return v << 2;
        if (years < 5) return v << 1;
        return v;
    }

    /** Infection cleared per day: babies 50%, toddlers 75%, adults 100%, -2%/yr after 60. */
    static int immuneStrength(int years) {
        if (years < 1) return IMMUNE_CLEAR >>> 1;
        if (years < 5) return IMMUNE_CLEAR * 3 / 4;
        if (years <= 60) return IMMUNE_CLEAR;
        int pct = Math.max(30, 100 - 2 * (years - 60));
        return pct * IMMUNE_CLEAR / 100;
    }

    // =====================================================================================
    //  DAMAGE, DISEASE ROLLS, GIVING AN AFFLICTION
    // =====================================================================================
    /** Remove HP from one part. If a VITAL part hits 0, remember who did it. */
    void hurt(int p, int part, int amount, int id) {
        int i = p * NPARTS + part;
        int h = w.hp[i];
        if (h == 0) return;                                    // already gone
        int nv = h - amount;
        if (nv > 0) { w.hp[i] = (char) nv; return; }
        w.hp[i] = 0;
        if (((vitalMask >>> part) & 1) != 0) w.cause[p] = (byte) id;
    }

    /**
     * Does something of this trigger happen today?  ONE random number vs. the
     * pre-summed total: usually "no" and we're done. Only on a hit do we walk
     * the table to find WHICH one (like spinning a wheel with weighted slices).
     */
    void rollTrigger(int p, int trig, boolean child, int years) {
        long total = TOTALS[trig][child ? 1 : 0];
        if (total == 0) return;
        long r = rng.next32();
        if (r >= total) return;                                // the usual outcome
        long acc = 0;
        for (int id = 1; id < COUNT; id++) {
            Aff a = TABLE[id];
            if (a.trigger != trig) continue;
            acc += child ? a.rate32 * a.kids : a.rate32;
            if (r < acc) {
                if (years >= a.minAge) giveAff(p, id, SAME);
                return;
            }
        }
    }

    /** Turn a special part code (ANY_LEG ...) into a real part number. */
    int resolve(int code) {
        if (code < ANY_ORGAN) return code;
        if (code == ANY_ORGAN) return ORGAN_PICK[rng.below(ORGAN_PICK.length)];
        int r = rng.below(code == ANY_LIMB ? 4 : 2);
        return (code == ANY_LEG ? LLEG : LARM) + r;
    }

    /** Give affliction 'id' to person p (on 'override' part, or SAME = its usual part). */
    void giveAff(int p, int id, int override) {
        if (((w.immune[p] >>> id) & 1) != 0) return;           // immune for life
        int sb = p * NSLOTS, free = -1;
        for (int s = 0; s < NSLOTS; s++) {
            int cur = w.aff[sb + s] & 0xFF;
            if (cur == id) return;                             // already has it
            if (cur == 0 && free == -1) free = s;
        }
        if (free == -1) return;                                // 8 problems already

        Aff a = TABLE[id];
        int part = resolve(override == SAME ? a.part : override);
        int days;
        if (a.has(CHRONIC)) days = 1;                          // elapsed counter starts at 1
        else {
            days = a.durMin + rng.below(a.durRng + 1);
            if (days == 0) days = 1;
            days = Math.min(0xFFFF, days);
        }
        w.aff[sb + free] = id | (part << 8) | (days << 16);

        if (a.hit != 0) hurt(p, part, a.hit, id);              // instant damage

        if (a.perm16 != 0 && (rng.next32() >>> 16) < a.perm16) {   // disabled for good?
            int pp = resolve(a.permPart == SAME ? part : a.permPart);
            int lame = w.lame[p];
            if ((lame & (1 << pp)) == 0) {
                w.lame[p] = (char) (lame | (1 << pp));
                w.disabledN++;
                w.log(w.personName(p) + ": " + Body.name(pp) + " permanently disabled (" + a.name + ")");
            }
        }
        if (a.infect != 0 && (rng.next32() & 0xFF) < a.infect)   // wound gets infected?
            giveAff(p, WOUNDINF.id, part);
    }

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
                    w.map[i] = T_STUMP;
                    w.wood++;
                    rollTrigger(p, CHOP, false, years);        // axe accidents
                    return;
                }
            }
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
        int dx = rng.below(3) - 1;
        int dy = rng.below(3) - 1;
        int nx = (w.x[p] & 0xFF) + dx, ny = (w.y[p] & 0xFF) + dy;
        if (Integer.compareUnsigned(nx, MAP_W) < 0) w.x[p] = (byte) nx;
        if (Integer.compareUnsigned(ny, MAP_H) < 0) w.y[p] = (byte) ny;
        rollTrigger(p, WALK, false, years);                    // trips and falls
    }

    // =====================================================================================
    //  BIRTH
    // =====================================================================================
    void giveBirth(int m) {
        w.preg[m] = -POSTPARTUM_DAYS;
        int c = w.newPerson();
        if (c != -1) {
            w.x[c] = w.x[m];
            w.y[c] = w.y[m];
            w.mother[c] = (char) m;
            w.births++;
            w.log(w.personName(c) + " born to " + w.personName(m));
            rollTrigger(c, NEWBORN, false, 0);                 // the baby's risks
        }
        rollTrigger(m, BIRTH, false, 99);                      // the mother's risks
    }
}
