package meatbox;

import static meatbox.Config.*;
import static meatbox.Body.*;
import static meatbox.World.*;
import static meatbox.Afflictions.*;

/**
 * =============================================================================
 *  HEALTH  -  conditions running in a body, organ function, abilities.
 * =============================================================================
 *  Sim.update calls, per person per day:
 *    afflictions()  run every active condition: damage, bleeding, infection,
 *                   contagion, complications, and add up their effects on
 *                   abilities, mind and blood chemistry
 *    blood()        organ function -> blood: O2, haemoglobin, glucose, blood
 *                   pressure, metabolism, toxins, infection, heart rate
 *    wear()         slow damage that bypasses healing: high glucose and high
 *                   blood pressure scar vessels, kidneys, eyes, nerves, heart
 *    functions()    organs + bones + joints + muscles + spinal cord ->
 *                   walking, each hand, sight, hearing, stamina, brain, speech
 *    monthly()      bone density, condition severity growth, emergent labels
 *
 *  Hooks for other code:
 *    nutrition(w,p), exposure(w,p)   read the food model's fat / warmth
 *    addStress(w,p,amount)           behaviour code: loneliness, conflict...
 *    injure(p, mechanism, energy)    behaviour code: fights, accidents
 */
final class Health {
    final World w;
    final Rng rng;
    Health(World w) { this.w = w; this.rng = w.rng; w.health = this; }

    // ---- risk channels (Health.channel) -------------------------------------------------
    static final int CH_STRESS = 0, CH_ACE = 1, CH_MALNUT = 2, CH_COLD = 3, CH_IMMOBILE = 4, CH_BP = 5,
        CH_GLU = 6, CH_ATHERO = 7, CH_IMMUNO = 8, CH_PAIN = 9, CH_POSTPARTUM = 10, CH_PREGNANT = 11,
        CH_EARLYPREG = 12, CH_LATEPREG = 13, CH_SMOKE = 14, CH_TOBACCO = 15, CH_ALCOHOL = 16,
        CH_HEADINJ = 17, CH_M_AGE = 18, CH_M_AGE_DS = 19, CH_M_MALNUT = 20, CH_M_ALCOHOL = 21,
        CH_M_RUBELLA = 22, CH_M_SYPHILIS = 23, CH_M_HEPB = 24, CH_LABOUR = 25, CH_SEXACTIVE = 26,
        CH_LOWBMD = 27, CH_WATER = 28, CH_MOSQUITO = 29, CH_RODENTS = 30, CH_ANIMALS = 31,
        CH_VITD_DEF = 32, CH_IODINE_DEF = 33, CH_SALT = 34, CH_OBESE = 35, CH_FRAIL = 36,
        CH_LOWVISION = 37, CH_SUN = 38, CH_SICKLE_TRAIT = 39, CH_GENO_HD = 40, CH_OPIATES = 41,
        CH_G6PD = 42, CH_THAL_TRAIT = 43, CH_CROWDING = 44, CH_M_HIV = 45, CH_M_ZIKA = 46, CH_ENV = 64;
    // environment exposures from Config.ENV (0 = absent from this world)
    static final int CH_AEDES = CH_ENV + E_AEDES, CH_SANDFLY = CH_ENV + E_SANDFLY, CH_TSETSE = CH_ENV + E_TSETSE,
        CH_TRIATOMINE = CH_ENV + E_TRIATOMINE, CH_SNAILS = CH_ENV + E_SNAILS, CH_PIGS = CH_ENV + E_PIGS,
        CH_DOGS_SHEEP = CH_ENV + E_DOGS_SHEEP, CH_BLACKFLY = CH_ENV + E_BLACKFLY, CH_CULEX = CH_ENV + E_CULEX,
        CH_RAW_FISH = CH_ENV + E_RAW_FISH, CH_COPEPODS = CH_ENV + E_COPEPODS, CH_SNAKES = CH_ENV + E_SNAKES,
        CH_DEEP_WATER = CH_ENV + E_DEEP_WATER, CH_SILICA = CH_ENV + E_SILICA, CH_COAL_DUST = CH_ENV + E_COAL_DUST,
        CH_ASBESTOS = CH_ENV + E_ASBESTOS, CH_COCAINE = CH_ENV + E_COCAINE, CH_AMPHETAMINE = CH_ENV + E_AMPHETAMINE,
        CH_CANNABIS = CH_ENV + E_CANNABIS, CH_HALLUCINOGENS = CH_ENV + E_HALLUCINOGENS,
        CH_SEDATIVES = CH_ENV + E_SEDATIVES, CH_INHALANTS = CH_ENV + E_INHALANTS, CH_CAFFEINE = CH_ENV + E_CAFFEINE,
        CH_GAMBLING = CH_ENV + E_GAMBLING, CH_HIV = CH_ENV + E_HIV, CH_EBOLA = CH_ENV + E_EBOLA,
        CH_SARS_COV_2 = CH_ENV + E_SARS_COV_2, CH_BLOOD_BORNE = CH_ENV + E_BLOOD_BORNE, CH_TICKS = CH_ENV + E_TICKS,
        CH_LIVESTOCK = CH_ENV + E_LIVESTOCK, CH_VEHICLES = CH_ENV + E_VEHICLES, CH_FIREARMS = CH_ENV + E_FIREARMS,
        CH_ELECTRICITY = CH_ENV + E_ELECTRICITY, CH_MEDICINE = CH_ENV + E_MEDICINE,
        CH_FILARIA = CH_ENV + E_FILARIA, CH_YAWS = CH_ENV + E_YAWS;

    // ---- hooks ----------------------------------------------------------------------------
    static int nutrition(World w, int p) { return w.fat[p]; }
    static int exposure(World w, int p)  { return w.warmth[p]; }
    static void addStress(World w, int p, int amount) {
        w.stress[p] = (char) Math.max(0, Math.min(HP_MAX, w.stress[p] + amount));
    }
    /** For behaviour code: an assault or accident. mech = Bones.M_*; energy ~20 (fall) .. 150 (crush). */
    int injure(int p, int mech, int energy, int causeId) {
        return Skeleton.injure(w, this, p, mech, energy, 1, causeId, Config.years(w.age[p]));
    }

    // ---- today's accumulators for the person being updated ---------------------------------
    long dis;              // parts disabled today
    int disReg;            // bone regions disabled today
    boolean bedAff;
    int painSum, drv, sev;
    final int[] psyAcc = new int[NPSY];
    final int[] fnPen = new int[NFN];
    final int[] blOff = new int[4];
    int stressAdd, immunoPct, fertPct, bleedxPct, bmdMonth;
    boolean newbornCtx;

    void begin() {
        dis = 0; disReg = 0; bedAff = false; painSum = 0; drv = 0; sev = 0;
        for (int i = 0; i < NPSY; i++) psyAcc[i] = 0;
        for (int i = 0; i < NFN; i++) fnPen[i] = 0;
        for (int i = 0; i < 4; i++) blOff[i] = 0;
        stressAdd = 0; immunoPct = 0; fertPct = 100; bleedxPct = 100; bmdMonth = 0;
    }

    // =====================================================================================
    //  1. RUN ACTIVE CONDITIONS
    // =====================================================================================
    void afflictions(int p, int years, boolean monthly) {
        if (w.affN[p] == 0) return;
        final int sb = p * NSLOTS;
        final int bb = p * 8;
        int left = w.affN[p];                                          // stop after the last one held this morning
        for (int s = 0; s < NSLOTS && left > 0; s++) {
            int id = w.affId[sb + s];
            if (id == 0) continue;
            left--;
            Aff a = TABLE[id];
            int part = w.affPart[sb + s];
            int raw = w.affSev[sb + s];
            boolean dormant = (raw & 0x8000) != 0;
            int sv = raw & 0x7FFF;
            boolean onBone = a.has(ON_BONE);

            if (!dormant) {
                if (a.has(DISABLE)) {
                    if (onBone) disReg |= 1 << Bones.REGION[part];
                    else dis |= 1L << part;
                }
                if (a.has(BEDRIDDEN)) bedAff = true;
                painSum += a.pain * sv / HP_MAX;
                int score = ((a.dot + a.bleed + a.sepsis) * sv) / HP_MAX + 1;
                for (int i = 0; i < a.nbl; i++) if (a.blSlot[i] == B2_GLU) score += a.blD[i] * sv / HP_MAX / 10;
                if (score > sev) { sev = score; drv = id; }

                if (a.dot != 0) {
                    int c = Math.max(1, a.dot * sv / HP_MAX);
                    c = ageScale(c, years);
                    int amt = (c >>> 1) + rng.below(c);                 // 50%..150%
                    if (onBone) Skeleton.hurtBone(w, this, p, part, amt, id, years);
                    else {
                        hurt(p, part, amt, id);
                        if (a.has(BOTH) && MIRROR[part] >= 0) hurt(p, MIRROR[part], amt, id);
                    }
                    for (int k : a.alsoHit) hurt(p, k, amt >>> 1, id);
                }
                if (a.bleed != 0) {
                    int amt = ageScale(Math.max(1, a.bleed * sv / HP_MAX), years);
                    if (a.has(FLUID)) fluidLoss(p, amt, id); else bleed(p, amt, id);
                }
                if (a.sepsis != 0)
                    w.blood[bb + B_PATH] = (char) Math.min(HP_MAX, w.blood[bb + B_PATH] + a.sepsis * sv / HP_MAX);
                if (a.spread != 0) {
                    int j = rng.below(w.popHi);
                    if (w.alive[j]
                            && Math.abs((w.x[j] & 0xFF) - (w.x[p] & 0xFF)) <= SPREAD_DIST
                            && Math.abs((w.y[j] & 0xFF) - (w.y[p] & 0xFF)) <= SPREAD_DIST
                            && (rng.next32() & 0xFF) < a.spread)
                        giveAff(j, a.spreadId, SAME, -1);
                }
                for (int i = 0; i < a.npsy; i++) psyAcc[a.psySlot[i]] += a.psyD[i] * sv / HP_MAX;
                for (int i = 0; i < a.nfn; i++) {
                    int pen = a.fnPen[i] * sv / HP_MAX;
                    if (a.fnSlot[i] == FN_MANIP) { fnPen[FN_MANIP_L] += pen; fnPen[FN_MANIP_R] += pen; }
                    else fnPen[a.fnSlot[i]] += pen;
                }
                for (int i = 0; i < a.nbl; i++) blOff[a.blSlot[i]] += a.blD[i] * sv / HP_MAX;
                stressAdd += a.stressDay;
                immunoPct = Math.max(immunoPct, a.immunoPct);
                fertPct = fertPct * a.fertPct / 100;
                bleedxPct = Math.max(bleedxPct, a.bleedxPct);
                if (monthly) bmdMonth += a.bmdMonth * sv / HP_MAX;
            }

            // ---- one draw for today's rare events: leads, death, remission. The chances are tiny, so
            // splitting a single draw into intervals keeps each one right while drawing once, not 2-6 times.
            long act = dormant ? 0 : a.act32;
            boolean remitNow = false;
            if (act + a.remit32 != 0) {
                long r = rng.next32();
                if (r < act) {
                    for (int i = 0; i < a.nlead; i++) {
                        if (r < a.lead32[i]) { giveAff(p, a.leadId[i], SAME, -1); r = -1; break; }
                        r -= a.lead32[i];
                    }
                    if (r >= 0 && !w.doomed[p]) { w.doomed[p] = true; w.cause[p] = (char) id; }
                } else remitNow = r < act + a.remit32;
            }

            // ---- time passes
            if (w.affId[sb + s] != id) continue;                       // replaced meanwhile
            int days = w.affDays[sb + s];
            if (a.episodic()) {
                if (--days <= 0) {
                    dormant = !dormant;
                    days = dormant ? a.epiOff + rng.below(a.epiOffRng + 1) : a.epiOn + rng.below(a.epiOnRng + 1);
                }
            } else if (a.has(CHRONIC)) {
                if (days < 0xFFFF) days++;
            } else if (--days <= 0) {                                   // recovered
                clearSlot(p, s);
                if (a.immIdx >= 0) w.immune[p] |= 1L << a.immIdx;
                if (a.thenId >= 0 && (a.then16 >= 65536 || (rng.next32() >>> 16) < a.then16))
                    giveAff(p, a.thenId, SAME, -1);
                continue;
            }
            if (remitNow) { clearSlot(p, s); continue; }
            if (monthly && a.progress != 0) sv = Math.min(HP_MAX, sv + a.progress / 12);
            w.affDays[sb + s] = (char) Math.max(1, days);
            w.affSev[sb + s] = (char) (sv | (dormant ? 0x8000 : 0));
        }
        if (drv != 0 && sev >= SERIOUS_SEV) w.recent[p] = (char) drv;
    }

    void clearSlot(int p, int s) {
        int i = p * NSLOTS + s;
        w.affId[i] = 0; w.affPart[i] = 0; w.affDays[i] = 0; w.affSev[i] = 0;
        w.affN[p]--;
    }

    /** Babies get hit 4x as hard, toddlers 2x. */
    static int ageScale(int v, int years) {
        if (years < 1) return v << 2;
        if (years < 5) return v << 1;
        return v;
    }

    // =====================================================================================
    //  DAMAGE
    // =====================================================================================
    /** Remove HP from one part. If that loses a VITAL part (or the second of a pair), remember who did it. */
    void hurt(int p, int part, int amount, int id) {
        if (amount <= 0) return;
        int i = p * NPARTS + part;
        int h = w.hp[i];
        if (h == 0) return;
        int nv = h - amount;
        if (nv > 0) { w.hp[i] = (char) nv; w.hurtMask[p] |= 1L << part; return; }
        w.hp[i] = 0;
        w.hurtMask[p] &= ~(1L << part);
        if (((vitalMask >>> part) & 1) != 0) w.cause[p] = (char) id;
        else if (((pairMask >>> part) & 1) != 0) {
            int other = (part & 1) == 1 ? part + 1 : part - 1;          // L_LUNG=11/R_LUNG=12, L_KIDNEY=19/R=20
            if (other >= 0 && other < NPARTS && ((pairMask >>> other) & 1) != 0 && w.hp[p * NPARTS + other] == 0)
                w.cause[p] = (char) id;
        }
    }

    /** Blood loss. Clotting: liver failure and haemophilia make the same wound bleed more. */
    void bleed(int p, int amount, int id) {
        if (amount <= 0) return;
        int liver = w.hp[p * NPARTS + LIVER];
        int mul = bleedxPct + (liver < 5000 ? (5000 - liver) / 50 : 0);
        amount = amount * mul / 100;
        int bb = p * 8;
        int vol = w.blood[bb + B_VOL];
        w.blood[bb + B_VOL] = (char) Math.max(0, vol - amount);
        int hb = w.blood2[p * 4 + B2_HB];                              // lost red cells: refilled with plasma
        w.blood2[p * 4 + B2_HB] = (char) Math.max(500, hb - hb * amount / HP_MAX);
        w.bleeder[p] = (char) id;
    }

    /** Dehydration: volume lost, red cells kept. */
    void fluidLoss(int p, int amount, int id) {
        int bb = p * 8;
        w.blood[bb + B_VOL] = (char) Math.max(0, w.blood[bb + B_VOL] - amount);
        w.bleeder[p] = (char) id;
    }

    // =====================================================================================
    //  GIVING A CONDITION
    // =====================================================================================
    boolean hasAff(int p, int id) {
        if (w.affN[p] == 0) return false;
        int sb = p * NSLOTS;
        for (int s = 0; s < NSLOTS; s++) if (w.affId[sb + s] == id) return true;
        return false;
    }

    /** Give condition id to p. override = a part (or bone) or SAME; sevOverride = -1 for random. */
    boolean giveAff(int p, int id, int override, int sevOverride) {
        Aff a = TABLE[id];
        boolean male = w.sex[p] == SEX_M;
        if ((male ? a.mMul : a.fMul) == 0) return false;
        if (a.immIdx >= 0 && ((w.immune[p] >>> a.immIdx) & 1) != 0) return false;
        int years = Config.years(w.age[p]);
        if (a.trigger == NONE && (years < a.minAge || years > a.maxAge)) return false;
        int sb = p * NSLOTS, free = -1;
        for (int s = 0; s < NSLOTS; s++) {
            int cur = w.affId[sb + s];
            if (cur == id) return false;
            if (cur == 0 && free == -1) free = s;
        }
        if (free == -1) { w.slotFull++; return false; }

        int part;
        boolean onBone = a.has(ON_BONE);
        if (override != SAME && !(onBone && override >= Bones.NBONES)) part = override;
        else {
            int site = a.sites[rng.below(a.sites.length)];
            part = onBone ? Bones.pickBone(rng, site) : site;
        }
        int days;
        if (a.episodic()) days = a.epiOn + rng.below(a.epiOnRng + 1);
        else if (a.has(CHRONIC)) days = 1;
        else days = Math.max(1, Math.min(0xFFFF, a.durMin + rng.below(a.durRng + 1)));
        int sv = sevOverride >= 0 ? sevOverride : a.sevMin + rng.below(a.sevMax - a.sevMin + 1);
        int i = sb + free;
        w.affId[i] = (char) id; w.affPart[i] = (char) part; w.affDays[i] = (char) days;
        w.affSev[i] = (char) Math.max(1, Math.min(HP_MAX, sv));
        w.affN[p]++;
        w.caseCount[id]++;

        if (a.hit != 0) {
            int h = a.hit * sv / HP_MAX;
            if (onBone) Skeleton.hurtBone(w, this, p, part, h, id, years);
            else {
                hurt(p, part, h, id);
                if (a.has(BOTH) && MIRROR[part] >= 0) hurt(p, MIRROR[part], h, id);
            }
            for (int k : a.alsoHit) hurt(p, k, h, id);
        }
        if (a.perm16 != 0 && (rng.next32() >>> 16) < a.perm16) {
            int pp = a.permPart == SAME ? (onBone ? -1 : part)
                   : a.permPart == ANY_LIMB ? LIMB_MUSCLES[rng.below(4)]
                   : a.permPart == ANY_LEG ? (rng.below(2) == 0 ? L_LEG_M : R_LEG_M) : a.permPart;
            if (pp >= 0 && ((w.lame[p] >>> pp) & 1) == 0) {
                w.lame[p] |= 1L << pp;
                w.disabledN++;
                w.log(w.personName(p) + ": " + Body.name(pp) + " disabled for good (" + a.name + ")");
            }
        }
        if (a.deformReg >= 0) {
            int ri = p * Bones.NREG + a.deformReg;
            int loss = rng.below(a.deformPct + 1) * sv / HP_MAX;
            w.regCap[ri] = (byte) Math.max(20, (w.regCap[ri] & 0xFF) - loss);
            Skeleton.regionUpdate(w, p, a.deformReg);
        }
        if (a.mech >= 0) Skeleton.injure(w, this, p, a.mech, a.energy, a.impacts, id, years);
        for (int k = 0; k < a.nonset; k++)
            if ((rng.next32() >>> 16) < a.onset16[k]) giveAff(p, a.onsetId[k], SAME, -1);
        if (a.has(SUDDEN) && !w.doomed[p]) { w.doomed[p] = true; w.cause[p] = (char) id; }
        else if (a.caseFatal32 != 0 && !w.doomed[p] && rng.next32() < a.caseFatal32) { w.doomed[p] = true; w.cause[p] = (char) id; }
        if (w.preg[p] > 0) {
            if (a.has(END_PREG)) { w.preg[p] = -90; w.miscarriages++; }
            else if (a == Catalogue.RUBELLA) w.pregFlags[p] |= PF_RUBELLA;
            else if (a == Catalogue.SYPHILIS) w.pregFlags[p] |= PF_SYPHILIS;
            else if (a == CatalogueGbd.ZIKA) w.pregFlags[p] |= PF_ZIKA;
        }
        if (!a.has(QUIET) && (a.cat == C_CANCER || a.cat == C_MENTAL || a.cat == C_NEURO || a.cat == C_CONGEN))
            w.log(w.personName(p) + ": " + a.name);
        return true;
    }

    // =====================================================================================
    //  ROLLS: is there a new case today?
    // =====================================================================================
    /**
     * ONE random number against the pre-summed worst-case total for this person's
     * sex and age band: usually "no". On a hit, find which condition (binary search)
     * and accept it with chance (this person's real multiplier / worst case).
     */
    void rollTrigger(int p, int trig, boolean child, int years) {
        if (trig == CHOP) w.labour[p] = (char) Math.min(HP_MAX, w.labour[p] + 300);
        int sex = w.sex[p];
        int band = Math.min(NBAND - 1, years / 5);
        long total = TOTAL[trig][sex][band];
        if (total == 0) return;
        long r = rng.next32();
        if (r >= total) return;
        long[] cum = CUM[trig][sex][band];
        int lo = 0, hi = cum.length - 1;
        while (lo < hi) { int mid = (lo + hi) >>> 1; if (r < cum[mid]) hi = mid; else lo = mid + 1; }
        Aff a = TABLE[CUM_ID[trig][sex][band][lo]];
        if (accept(p, a, years)) giveAff(p, a.id, SAME, -1);
    }

    /** Event triggers (birth, bereavement, trauma): every condition rolls independently. */
    void rollEvent(int p, int trig, int sevOverride) {
        int years = Config.years(w.age[p]);
        newbornCtx = trig == NEWBORN;
        for (int id : EVENT_IDS[trig]) {
            Aff a = TABLE[id];
            if (a.eventRate32 == 0 || rng.next32() >= a.eventRate32) continue;
            if (accept(p, a, years)) giveAff(p, id, SAME, sevOverride);
        }
        newbornCtx = false;
    }

    boolean accept(int p, Aff a, int years) {
        if (years < a.minAge || years > a.maxAge) return false;
        double m = mult(p, a);
        if (m <= 0) return false;
        if (m >= a.cap) return true;
        return rng.next32() < (long) (m / a.cap * 4294967296.0);
    }

    /** This person's real risk multiplier for a condition (<= a.cap). */
    double mult(int p, Aff a) {
        double m = 1;
        if (a.geneFam >= 0) m *= Genes.mult(w.liab[p * Genes.NFAM + a.geneFam] & 0xFF, a.geneFold);
        for (int i = 0; i < a.nch; i++) {
            double v = channel(p, a.ch[i]) / 10000.0;
            switch (a.chMode[i]) {
                case 0: m *= 1 + (a.chMax[i] - 1) * v; break;
                case 1: if (v <= 0) return 0; m *= a.chMax[i] * v; break;
                default: m *= 1 - (1 - a.chMax[i]) * v; break;
            }
        }
        for (int i = 0; i < a.nafter; i++) {
            if (a.afterMul[i] == -2) {
                int ix = TABLE[a.afterId[i]].immIdx;
                if (ix < 0 || ((w.immune[p] >>> ix) & 1) == 0) return 0;
                continue;
            }
            boolean has = hasAff(p, a.afterId[i]);
            if (a.afterMul[i] < 0) { if (!has) return 0; }
            else if (has) m *= a.afterMul[i];
        }
        return m;
    }

    private static int clamp(long v) { return (int) Math.max(0, Math.min(HP_MAX, v)); }

    /** A person's exposure on one risk channel, 0..10000. Computed only when a roll needs it. */
    int channel(int p, int ch) {
        int yrs = Config.years(w.age[p]);
        int mo = newbornCtx && w.mother[p] != 0xFFFF && w.alive[w.mother[p]] ? w.mother[p] : p;
        switch (ch) {
            case CH_STRESS:     return w.stress[p];
            case CH_ACE:        return Math.min(HP_MAX, w.ace[p] * 2500);
            case CH_MALNUT:     return clamp((5000L - nutrition(w, p)) * 2);
            case CH_COLD:       return clamp((7000L - exposure(w, p)) * 10000 / 7000);
            case CH_IMMOBILE:   return w.bed[p] ? HP_MAX : clamp((5000L - w.func[p * NFN + FN_MOBILITY]) * 2);
            case CH_BP:         return clamp((w.blood2[p * 4 + B2_BP] - 120L) * 200);
            case CH_GLU:        return clamp((w.blood2[p * 4 + B2_GLU] - 600L) * 5);
            case CH_ATHERO:     return HP_MAX - w.hp[p * NPARTS + VESSELS];
            case CH_IMMUNO:     return HP_MAX - Math.min(HP_MAX, w.immun[p]);
            case CH_PAIN:       return w.blood[p * 8 + B_PAIN];
            case CH_POSTPARTUM: return w.preg[p] < 0 && POSTPARTUM_DAYS + w.preg[p] < 180 ? HP_MAX : 0;
            case CH_PREGNANT:   return w.preg[p] > 0 ? HP_MAX : 0;
            case CH_EARLYPREG:  return w.preg[p] > GESTATION_DAYS - 140 ? HP_MAX : 0;
            case CH_LATEPREG:   return w.preg[p] > 0 && w.preg[p] < 100 ? HP_MAX : 0;
            case CH_SMOKE:      return INDOOR_SMOKE;
            case CH_TOBACCO:    return yrs >= 15 ? TOBACCO : 0;
            case CH_ALCOHOL:    return ALCOHOL;
            case CH_OPIATES:    return OPIATES;
            case CH_HEADINJ:    return w.headInj[p] > 0 ? HP_MAX : 0;
            case CH_M_AGE:      return clamp((Config.years(w.age[mo]) - 30L) * 1000);
            case CH_M_AGE_DS:   return clamp((long) (10000 * Math.pow(2, (Config.years(w.age[mo]) - 30) / 3.0) / 30));
            case CH_M_MALNUT:   return clamp((5000L - nutrition(w, mo)) * 2);
            case CH_M_ALCOHOL:  return hasAff(mo, Catalogue.AUD.id) ? HP_MAX : 0;
            case CH_M_RUBELLA:  return (w.pregFlags[mo] & PF_RUBELLA) != 0 ? HP_MAX : 0;
            case CH_M_SYPHILIS: return (w.pregFlags[mo] & PF_SYPHILIS) != 0 || hasAff(mo, Catalogue.SYPH_L.id) ? HP_MAX : 0;
            case CH_M_HEPB:     return hasAff(mo, Catalogue.HEPB_C.id) ? HP_MAX : 0;
            case CH_LABOUR:     return w.labour[p];
            case CH_SEXACTIVE:  return yrs >= SEX_ACTIVE_MIN && yrs <= SEX_ACTIVE_MAX ? HP_MAX : 0;
            case CH_LOWBMD:     return HP_MAX - w.bmd[p];
            case CH_WATER:      return WATER_CONTAM;
            case CH_MOSQUITO:   return MOSQUITO;
            case CH_RODENTS:    return RODENTS;
            case CH_ANIMALS:    return ANIMALS;
            case CH_VITD_DEF:   return Math.max(HP_MAX - SUNLIGHT, clamp((5000L - nutrition(w, p))));
            case CH_IODINE_DEF: return HP_MAX - IODINE;
            case CH_SALT:       return SALT;
            case CH_OBESE:      return OBESITY;
            case CH_FRAIL:      return clamp(7000L - w.func[p * NFN + FN_MOBILITY]);
            case CH_LOWVISION:  return clamp(7000L - w.func[p * NFN + FN_VISION]);
            case CH_SUN:        return UV;
            case CH_SICKLE_TRAIT: return Genes.carrier(w, p, Genes.M_SICKLE) ? HP_MAX : 0;
            case CH_GENO_HD:    return Genes.affected(w, p, Genes.M_HD) ? HP_MAX : 0;
            case CH_G6PD:       return Genes.affected(w, p, Genes.M_G6PD) ? HP_MAX : 0;
            case CH_THAL_TRAIT: return Genes.carrier(w, p, Genes.M_THAL) ? HP_MAX : 0;
            case CH_M_HIV:      return hasAff(mo, CatalogueGbd.HIV.id) || hasAff(mo, CatalogueGbd.AIDS.id) ? HP_MAX : 0;
            case CH_M_ZIKA:     return (w.pregFlags[mo] & PF_ZIKA) != 0 ? HP_MAX : 0;
            case CH_CROWDING:   return clamp((w.popCount - 50L) * 50);          // 250 people = 10000 [tune]
            default: return ch >= CH_ENV && ch < CH_ENV + NENV ? Config.ENV[ch - CH_ENV] : 0;
        }
    }

    // =====================================================================================
    //  2. ORGANS -> BLOOD  (returns perfusion; the food model's cold drop is applied by Sim)
    // =====================================================================================
    /** Immune clearing per day: babies 50%, toddlers 75%, adults 100%, -2%/yr after 60. */
    static int immuneStrength(int years) {
        if (years < 1) return IMMUNE_CLEAR >>> 1;
        if (years < 5) return IMMUNE_CLEAR * 3 / 4;
        if (years <= 60) return IMMUNE_CLEAR;
        int pct = Math.max(30, 100 - 2 * (years - 60));
        return pct * IMMUNE_CLEAR / 100;
    }

    /** Haemoglobin, glucose, blood pressure, metabolism. Call before perfusion. */
    void chemistry(int p, int years, int fat) {
        final int hb0 = p * NPARTS, b2 = p * 4;
        final char[] hp = w.hp;
        // haemoglobin: marrow makes red cells (needs kidney EPO and food); ~1/60 per day toward target
        int target = (w.sex[p] == SEX_F && years >= 13 ? 9300 : HP_MAX) + blOff[B2_HB];
        int kid = Math.min(HP_MAX, hp[hb0 + L_KIDNEY] + hp[hb0 + R_KIDNEY]);
        long prod = (long) target * hp[hb0 + MARROW] / HP_MAX * (5000 + kid / 2) / HP_MAX;
        if (fat < 3000) prod = prod * (6000 + fat * 4 / 3) / HP_MAX;
        prod = Math.max(1500, prod);
        int hb = w.blood2[b2 + B2_HB];
        hb += (int) ((prod - hb) / 60);
        w.blood2[b2 + B2_HB] = (char) Math.max(500, Math.min(12000, hb));

        // glucose: pancreas under ~40% can't make enough insulin; starvation, liver and
        // adrenal failure drop it [src~ physiology]
        int panc = hp[hb0 + PANCREAS];
        int glu = 500 + blOff[B2_GLU] + (panc < 4000 ? (4000 - panc) / 3 : 0);
        if (fat < 1000) glu -= (1000 - fat) / 5;
        // the pituitary drives the thyroid and the adrenals' cortisol: losing it fails both [src~]
        final int pit = hp[hb0 + PITUITARY];
        int liver = hp[hb0 + LIVER], adr = Math.min(hp[hb0 + ADRENALS], pit);
        if (liver < 2500) glu -= (2500 - liver) / 15;
        if (adr < 2500) glu -= (2500 - adr) / 20;
        w.blood2[b2 + B2_GLU] = (char) Math.max(150, Math.min(6000, glu));

        // metabolic rate: thyroid output
        int thy = Math.min(hp[hb0 + THYROID], pit);
        int meta = HP_MAX + blOff[B2_META] - (thy < 5000 ? (5000 - thy) / 2 : 0);
        w.blood2[b2 + B2_META] = (char) Math.max(3000, Math.min(20000, meta));
    }

    /** Blood pressure from heart, volume, adrenals and conditions. Call after volume is known. */
    void pressure(int p, int vol) {
        final int hb0 = p * NPARTS;
        int bp = 120 + blOff[B2_BP];
        if (vol < 9000) bp -= (9000 - vol) / 50;
        int heart = w.hp[hb0 + HEART], adr = w.hp[hb0 + ADRENALS];
        if (heart < 5000) bp -= (5000 - heart) / 100;
        if (adr < 3000) bp -= (3000 - adr) / 60;
        int kid = w.hp[hb0 + L_KIDNEY] + w.hp[hb0 + R_KIDNEY];
        if (kid < 8000) bp += (8000 - kid) / 250;                     // failing kidneys raise pressure
        w.blood2[p * 4 + B2_BP] = (char) Math.max(30, Math.min(280, bp));
    }

    /** Immune strength today, 0..10000 relative to a healthy adult. */
    int immunity(int p, int years) {
        final int hb0 = p * NPARTS;
        long im = (long) immuneStrength(years) * HP_MAX / IMMUNE_CLEAR;
        im = im * (7000 + w.hp[hb0 + SPLEEN] * 3 / 10) / HP_MAX;       // no spleen: ~70%
        im = im * (3000 + w.hp[hb0 + MARROW] * 7 / 10) / HP_MAX;
        im = im * (8000 + w.hp[hb0 + LYMPH] / 5) / HP_MAX;              // lymph nodes: ~80% without them [guess]
        im = im * (100 - immunoPct) / 100;
        int glu = w.blood2[p * 4 + B2_GLU];
        if (glu > 1000) im = im * 8 / 10;                              // [src~] hyperglycaemia impairs neutrophils
        im = im * (HP_MAX - w.stress[p] / 5) / HP_MAX;                // chronic stress (cortisol)
        w.immun[p] = (char) Math.max(0, Math.min(HP_MAX, im));
        return (int) im;
    }

    // =====================================================================================
    //  3. SLOW DAMAGE THAT HEALING DOES NOT UNDO
    // =====================================================================================
    /** Per-part chronic wear for today, written into wear[]. Returns true if any. */
    final int[] wear = new int[NPARTS];
    boolean wear(int p) {
        int glu = w.blood2[p * 4 + B2_GLU], bp = w.blood2[p * 4 + B2_BP];
        boolean any = false;
        int g = glu > GLU_DAMAGE ? (glu - GLU_DAMAGE) >> 7 : 0;        // [tune] ~1/day at 9 mmol/L
        int b = bp > BP_DAMAGE ? (bp - BP_DAMAGE) >> 4 : 0;            // [tune] ~1/day at 156 mmHg
        if (g == 0 && b == 0) return false;
        for (int k = 0; k < NPARTS; k++) wear[k] = 0;
        if (g > 0) {
            wear[VESSELS] += g; wear[L_KIDNEY] += g; wear[R_KIDNEY] += g;
            wear[L_EYE] += g; wear[R_EYE] += g; wear[NERVES] += g;    // diabetic micro/macrovascular damage [src]
            any = true;
        }
        if (b > 0) {
            wear[VESSELS] += b; wear[L_KIDNEY] += b; wear[R_KIDNEY] += b; wear[HEART] += (b + 1) >> 1;
            if (bp > 180) { wear[BRAIN] += 1; wear[L_EYE] += 1; wear[R_EYE] += 1; }
            any = true;
        }
        return any;
    }

    // =====================================================================================
    //  4. WHAT THE BODY CAN DO
    // =====================================================================================
    private int effHp(int p, int k, long off) { return ((off >>> k) & 1) != 0 ? 0 : w.hp[p * NPARTS + k]; }
    private int reg(int p, int r) {
        int v = w.regInt[p * Bones.NREG + r];
        return ((disReg >>> r) & 1) != 0 ? 0 : v;
    }
    /** a * b / 10000 without a division: 53687 / 2^29 ~ 1/10000 (error < 0.001%). */
    private static int mul(int a, int b) { return (int) (((long) a * b * 53687L) >>> 29); }
    private static int soft(int v, int floor) { return floor + mul(v, HP_MAX - floor); }   // floor..10000


    void functions(int p, int years, int perf, int pain) {
        // Hot path: every person, every day. Arrays and masks go into locals once;
        // a disabled part (dis/lame bit) or region reads as 0.
        final long off = dis | w.lame[p];
        final int rmask = disReg;
        final char[] hp = w.hp, ri = w.regInt;
        final int hb = p * NPARTS, rb = p * Bones.NREG, fb = p * NFN;
        // spinal cord: an injury at level L takes everything below it
        final int cord = (off & (1L << CORD)) != 0 ? 0 : hp[hb + CORD], lvl = w.cordLvl[p];
        final int cordArms = lvl == 0 || lvl > 8 ? soft(cord, 5000) : lvl <= 4 ? cord : soft(cord, (lvl - 4) * 2000);
        final int nerves = soft((off & (1L << NERVES)) != 0 ? 0 : hp[hb + NERVES], 3000);

        // legs: bones x muscle x joints x cord x nerves
        int legL = 0, legR = 0;
        for (int s = 0; s < 2; s++) {
            int bone = Math.min(Math.min(rg(ri, rb, rmask, Bones.L_THIGH + s), rg(ri, rb, rmask, Bones.L_SHIN + s)),
                                soft(rg(ri, rb, rmask, Bones.L_FOOT + s), 3000));
            int muscle = soft(hpo(hp, hb, off, L_LEG_M + s), 2000);
            int joint = Math.min(Math.min(hpo(hp, hb, off, L_HIP + s), hpo(hp, hb, off, L_KNEE + s)),
                                 soft(hpo(hp, hb, off, L_ANKLE + s), 4000));
            int leg = mul(mul(mul(mul(bone, muscle), soft(joint, 2500)), cord), nerves);   // worn joints hurt but still work
            if (s == 0) legL = leg; else legR = leg;
        }
        int trunk = Math.min(rg(ri, rb, rmask, Bones.PELVIS),
                    Math.min(soft(rg(ri, rb, rmask, Bones.LUMBAR), 2000), soft(hpo(hp, hb, off, SPINE_J), 4000)));
        int mob = legL >= 2500 && legR >= 2500 ? (legL + legR) >> 1 : Math.max(legL, legR) >> 2;
        mob = mul(mul(mob, trunk), HP_MAX - Math.min(7000, pain * 2 / 3));
        if (years < 1) mob = 0;
        else if (years < 2) mob = mob / 3;

        // hands
        int handL = 0, handR = 0;
        for (int s = 0; s < 2; s++) {
            int bone = Math.min(Math.min(rg(ri, rb, rmask, Bones.L_UPARM + s), rg(ri, rb, rmask, Bones.L_FOREARM + s)),
                                soft(rg(ri, rb, rmask, Bones.L_GIRDLE + s), 3000));
            bone = mul(bone, soft(rg(ri, rb, rmask, Bones.L_HAND + s), 1000));
            int muscle = soft(hpo(hp, hb, off, L_ARM_M + s), 2000);
            int joint = soft(Math.min(Math.min(hpo(hp, hb, off, L_SHOULDER + s), hpo(hp, hb, off, L_ELBOW + s)),
                                      hpo(hp, hb, off, L_WRIST + s)), 2500);
            int hand = mul(mul(mul(mul(bone, muscle), joint), cordArms), nerves);
            if (years < 3) hand = hand * (years + 1) / 4;
            if (s == 0) handL = hand; else handR = hand;
        }

        final int brain = hpo(hp, hb, off, BRAIN);
        int eL = hpo(hp, hb, off, L_EYE), eR = hpo(hp, hb, off, R_EYE);
        int vision = mul(Math.min(HP_MAX, Math.max(eL, eR) + (Math.min(eL, eR) >> 2)), soft(brain, 5000));
        int oss = rg(ri, rb, rmask, Bones.OSSICLES);
        int earL = mul(hpo(hp, hb, off, L_EAR), oss), earR = mul(hpo(hp, hb, off, R_EAR), oss);
        int hearing = Math.min(HP_MAX, Math.max(earL, earR) + (Math.min(earL, earR) >> 2));

        int musc = (hp[hb + L_LEG_M] + hp[hb + R_LEG_M] + hp[hb + L_ARM_M] + hp[hb + R_ARM_M] + 2 * hp[hb + CORE_M]) / 6;
        int stamina = mul(perf, soft(musc, 2000));
        int temp = w.blood[p * 8 + B_TEMP];
        if (temp > 3800) stamina = mul(stamina, Math.max(2000, HP_MAX - (temp - 3800) * 40));

        int cog = brain;
        if (perf < 7000) cog = mul(cog, soft(perf * 10 / 7, 3000));
        int glu = w.blood2[p * 4 + B2_GLU];
        if (glu < 300) cog = mul(cog, soft(glu * 10000 / 300, 2000));
        int waste = w.blood[p * 8 + B_WASTE];
        if (waste > UREMIA_T) cog = mul(cog, Math.max(2000, HP_MAX - (waste - UREMIA_T)));
        if (years < 18) cog = mul(cog, 3000 + years * 7000 / 18);      // still developing

        int speech = Math.min(HP_MAX, mul(soft(brain, 0), Math.min(hpo(hp, hb, off, AIRWAY) * 2, soft(hpo(hp, hb, off, MOUTH), 5000))));
        speech = mul(speech, soft(rg(ri, rb, rmask, Bones.FACE), 5000));
        if (years < 2) speech = speech * years / 2;

        final char[] fn = w.func;
        final int[] pen = fnPen;
        int vVis, vL, vR, vMob;
        fn[fb + FN_MOBILITY]  = (char) (vMob = clampHp(mob - pen[FN_MOBILITY]));
        fn[fb + FN_MANIP_L]   = (char) (vL = clampHp(handL - pen[FN_MANIP_L]));
        fn[fb + FN_MANIP_R]   = (char) (vR = clampHp(handR - pen[FN_MANIP_R]));
        fn[fb + FN_VISION]    = (char) (vVis = clampHp(vision - pen[FN_VISION]));
        fn[fb + FN_HEARING]   = (char) clampHp(hearing - pen[FN_HEARING]);
        fn[fb + FN_STAMINA]   = (char) clampHp(stamina - pen[FN_STAMINA]);
        fn[fb + FN_COGNITION] = (char) clampHp(cog - pen[FN_COGNITION]);
        fn[fb + FN_SPEECH]    = (char) clampHp(speech - pen[FN_SPEECH]);

        // legacy capability bits for v0.1 behaviour code
        int c = 0;
        if (vVis >= USABLE_HP) c |= 1 << EYES;
        if (cord >= USABLE_HP) c |= 1 << SPINE;
        if (vL >= USABLE_HP) c |= 1 << LARM;
        if (vR >= USABLE_HP) c |= 1 << RARM;
        if (vMob >= USABLE_HP) c |= (1 << LLEG) | (1 << RLEG);
        else if (vMob >= 600) c |= 1 << (legL >= legR ? LLEG : RLEG);   // hobbling on one leg
        w.cap[p] = (char) c;
    }

    private static int hpo(char[] hp, int hb, long off, int k) { return ((off >>> k) & 1) != 0 ? 0 : hp[hb + k]; }
    private static int rg(char[] ri, int rb, int mask, int r) { return ((mask >>> r) & 1) != 0 ? 0 : ri[rb + r]; }
    private static int clampHp(int v) { return v < 0 ? 0 : v > HP_MAX ? HP_MAX : v; }

    /** Work capacity once the mind is known: body x energy. */
    void workCapacity(int p) {
        final int fb = p * NFN;
        int hands = Math.max(w.func[fb + FN_MANIP_L], w.func[fb + FN_MANIP_R]);
        int body = Math.min(w.func[fb + FN_STAMINA], Math.min(hands, Math.max(w.func[fb + FN_MOBILITY], 3000)));
        int energy = Math.max(0, w.psy[p * NPSY + PSY_ENERGY]);
        int pain = w.blood[p * 8 + B_PAIN];
        int v = mul(mul(body, Math.min(HP_MAX, energy * 5 / 4)), HP_MAX - pain / 2);
        if (w.bed[p]) v = 0;
        w.func[fb + FN_WORK] = (char) Math.max(0, Math.min(HP_MAX, v));
    }

    // =====================================================================================
    //  5. MONTHLY: bones, emergent labels
    // =====================================================================================
    void monthly(int p, int years) {
        Skeleton.monthly(w, p, years, bmdMonth);
        w.labour[p] = (char) (w.labour[p] * 4 / 5);
        final int hb0 = p * NPARTS;
        final char[] hp = w.hp;
        if (w.bmd[p] < OSTEO_BMD) giveAff(p, Catalogue.OSTEOPOROSIS.id, SAME, -1);
        if (w.blood2[p * 4 + B2_HB] < HB_ANAEMIA) giveAff(p, Catalogue.ANAEMIA.id, SAME, -1);
        if (hp[hb0 + L_KIDNEY] + hp[hb0 + R_KIDNEY] < HP_MAX) giveAff(p, Catalogue.CKD.id, SAME, -1);
        if (hp[hb0 + HEART] < 4000) giveAff(p, Catalogue.HF.id, SAME, -1);
        if (hp[hb0 + VESSELS] < 6000) giveAff(p, Catalogue.ATHERO.id, SAME, -1);
        if (hp[hb0 + NERVES] < 6000) giveAff(p, Catalogue.NEUROPATHY.id, SAME, -1);
        if (years >= 3 && w.func[p * NFN + FN_VISION] < 1000) giveAff(p, Catalogue.BLINDNESS.id, SAME, -1);
        if (years >= 3 && w.func[p * NFN + FN_HEARING] < 1000) giveAff(p, Catalogue.DEAFNESS.id, SAME, -1);
        if (years >= 50 && (hp[hb0 + L_LEG_M] + hp[hb0 + R_LEG_M] + hp[hb0 + L_ARM_M] + hp[hb0 + R_ARM_M]) < 2 * HP_MAX)
            giveAff(p, Catalogue.SARCOPENIA.id, SAME, -1);
        if (Math.min(Math.min(hp[hb0 + L_KNEE], hp[hb0 + R_KNEE]), Math.min(hp[hb0 + L_HIP], hp[hb0 + R_HIP])) < 5000)
            giveAff(p, Catalogue.OA_LABEL.id, SAME, -1);
        if (nutrition(w, p) < 2500) giveAff(p, Catalogue.MALNUTRITION.id, SAME, -1);
        if (years >= 30 && hp[hb0 + MOUTH] < 3000) giveAff(p, CatalogueGbd.EDENTULISM.id, SAME, -1);
    }

    /** Daily: acute delirium in the vulnerable (elderly, small children) when the blood is failing the brain. */
    void delirium(int p, int years, int perf, int temp, int waste, int glu, int path) {
        if (years >= 5 && years < 65) return;
        if (temp >= 3900 || waste > UREMIA_T || glu < 300 || perf < 6500 || path > SEPSIS_T)
            giveAff(p, Catalogue.DELIRIUM.id, SAME, -1);
    }
}
