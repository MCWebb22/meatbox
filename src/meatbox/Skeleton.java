package meatbox;

import static meatbox.Bones.*;
import static meatbox.Config.*;

/**
 * =============================================================================
 *  SKELETON  -  what happens to the 206 bones: impacts, fractures, healing,
 *  bone density over a lifetime.
 * =============================================================================
 *  FRACTURE PHYSICS (injure):
 *    1. the mechanism (fall, blow, crush, twist, cut) picks a region, then a bone
 *    2. impact force = energy x a random factor with a long tail (-ln u, mean 1),
 *       so most falls are harmless and a few are hard
 *    3. threshold = bone strength x BMD^2 x personal factor (children's bones
 *       bend: x0.7 but heal twice as fast; brittle-bone disease x0.4)
 *    4. force above threshold = fracture; how far above = severity
 *    5. complications by region: femur and pelvis bleed heavily, ribs can
 *       puncture a lung, skull fractures injure the brain, vertebrae can cut the
 *       spinal cord (cervical = all four limbs and possibly breathing), open
 *       fractures get infected
 *    6. without anyone to set them, bad fractures heal crooked (malunion):
 *       the region never gets back to full function [guess: 40% of displaced]
 */
final class Skeleton {
    private Skeleton() {}

    static final int MALUNION_SEV = 6000, MALUNION_PCT = 40;   // [guess]
    static final int OPEN_SEV = 7000, OPEN_PCT = 25;            // [guess]

    static void reset(World w, int p) {
        int b = p * NBONES;
        for (int i = 0; i < NBONES; i++) w.bone[b + i] = HP_MAX;
        int r = p * NREG;
        for (int i = 0; i < NREG; i++) { w.regInt[r + i] = HP_MAX; w.regCap[r + i] = 100; }
        w.bmd[p] = HP_MAX; w.boneStr[p] = 100; w.boneHurt[p] = 0; w.bonePain[p] = 0;
        w.cordLvl[p] = 0; w.boneBed[p] = false;
    }

    /** One injury event. Returns the worst fracture severity (0 = only bruising). */
    static int injure(World w, Health h, int p, int mech, int energy, int impacts, int causeId, int years) {
        Rng rng = w.rng;
        int worst = 0;
        for (int n = 0; n < impacts; n++) {
            int row = mech == M_FALL && years >= FALL_OLD_AGE ? FALL_OLD_ROW : mech;
            int r = pickRegion(rng, MECH_W[row]);
            int b = pickBone(rng, r);
            double u = (rng.next32() + 1) / 4294967297.0;
            double force = energy * -Math.log(u);
            double dens = w.bmd[p] / 10000.0;
            double thr = STRENGTH[b] * dens * dens * (w.boneStr[p] & 0xFF) / 100.0 * (years < 15 ? 0.7 : 1.0);
            // soft tissue always takes some of it
            h.hurt(p, REG_SOFT[r], (int) Math.min(3000, force * 40), causeId);
            if (r == SKULL && force > thr * 0.5) concussion(w, h, p, (int) (force / thr * 5000), causeId);
            if (force < thr) continue;
            int sev = (int) Math.min(HP_MAX, 2500 + (force - thr) * 7500 / thr);
            fracture(w, h, p, b, sev, causeId, years);
            worst = Math.max(worst, sev);
        }
        if (worst >= 6000) h.rollEvent(p, Afflictions.TRAUMA, -1);
        return worst;
    }

    static int pickRegion(Rng rng, int[] wts) {
        int tot = 0;
        for (int x : wts) tot += x;
        int k = rng.below(tot);
        for (int i = 0; i < wts.length; i++) { k -= wts[i]; if (k < 0) return i; }
        return 0;
    }

    /** Break bone b with severity sev (also used by bone disease: pathological fracture). */
    static void fracture(World w, Health h, int p, int b, int sev, int causeId, int years) {
        Rng rng = w.rng;
        int i = p * NBONES + b;
        int before = w.bone[i];
        int now = Math.max(0, before - sev);
        w.bone[i] = (char) now;
        if (before == HP_MAX) w.boneHurt[p]++;
        w.fractures++;
        w.fracCount[b]++;
        int r = REGION[b];
        int bb = p * 8;
        // malunion: no bone-setting in this world
        if (sev >= MALUNION_SEV && rng.below(100) < MALUNION_PCT) {
            int ri = p * NREG + r;
            int loss = (sev - MALUNION_SEV) / 150 + 5;           // 5..32 %
            w.regCap[ri] = (byte) Math.max(20, w.regCap[ri] - loss);
        }
        // open fracture: bone through skin
        boolean open = sev >= OPEN_SEV && rng.below(100) < OPEN_PCT && r != SKULL && r != THORACIC && r != LUMBAR;
        if (open) {
            h.bleed(p, 500, causeId);
            h.hurt(p, Body.SKIN, 2000, causeId);
            if (rng.below(100) < 40) h.giveAff(p, Catalogue.WOUNDINF.id, Body.SKIN, -1);
            if (rng.below(100) < 10) h.giveAff(p, Catalogue.OSTEOMYELITIS.id, b, -1);
        }
        switch (r) {
            case L_THIGH: case R_THIGH:
                if (b == FEMUR[0] || b == FEMUR[1]) h.bleed(p, sev / 4, causeId);   // [src~] 1-1.5 L into the thigh
                break;
            case PELVIS: h.bleed(p, sev * 2 / 5, causeId); break;                   // [src~] pelvic fractures can lose litres
            case L_SHIN: case R_SHIN: h.bleed(p, sev / 12, causeId); break;
            case CHEST:
                if (sev > 5000 && rng.below(100) < 30) h.giveAff(p, Catalogue.PNEUMOTHORAX_T.id, Afflictions.SAME, -1);
                break;
            case SKULL:
                h.hurt(p, Body.BRAIN, sev / 3, causeId);
                if (rng.below(100) < 15) h.hurt(p, Body.BRAIN, sev, causeId);        // bleed inside the skull
                if (sev > 5000) h.giveAff(p, Catalogue.TBI.id, Body.BRAIN, sev);
                if (w.headInj[p] < 100) w.headInj[p]++;
                break;
            case OSSICLES: h.hurt(p, b % 2 == 0 ? Body.L_EAR : Body.R_EAR, sev / 2, causeId); break;
            case FACE: h.hurt(p, Body.MOUTH, sev / 3, causeId); break;
            case NECK: case THORACIC: case LUMBAR: cord(w, h, p, b, sev, causeId, r); break;
            default: break;
        }
        regionUpdate(w, p, r);
    }

    static void concussion(World w, Health h, int p, int sev, int causeId) {
        h.hurt(p, Body.BRAIN, Math.min(2000, sev / 4), causeId);
        h.giveAff(p, Catalogue.CONCUSSION.id, Body.BRAIN, Math.min(HP_MAX, sev));
        if (w.headInj[p] < 100) w.headInj[p]++;
    }

    /** A broken vertebra can injure the spinal cord. [src~] ~cervical 40%, thoracic 25%, lumbar 15% of bad ones. */
    static void cord(World w, Health h, int p, int b, int sev, int causeId, int r) {
        int pct = r == NECK ? 40 : r == THORACIC ? 25 : 15;
        if (LEVEL[b] == 0 || w.rng.below(10000) >= pct * sev / 100) return;
        h.hurt(p, Body.CORD, sev, causeId);
        int lvl = LEVEL[b];
        int old = w.cordLvl[p];
        if (old == 0 || lvl < old) w.cordLvl[p] = (byte) lvl;
        if (w.hp[p * Body.NPARTS + Body.CORD] < 5000) {
            h.giveAff(p, lvl <= 7 ? Catalogue.TETRA.id : Catalogue.PARA.id, Body.CORD, -1);
            // C1-C4 complete: the diaphragm stops (C3-5 keep it alive)
            if (lvl <= 4 && w.hp[p * Body.NPARTS + Body.CORD] < 2000) h.hurt(p, Body.CORE_M, HP_MAX, causeId);
        }
    }

    /** Region integrity = min(worst key bone, average of all bones), capped by malunion. */
    static void regionUpdate(World w, int p, int r) {
        int[] bs = regBones[r];
        int base = p * NBONES, minKey = HP_MAX;
        long sum = 0;
        for (int b : bs) {
            int v = w.bone[base + b];
            sum += v;
            if (KEY[b] && v < minKey) minKey = v;
        }
        int v = (int) Math.min(minKey, sum / bs.length);
        v = Math.min(v, (w.regCap[p * NREG + r] & 0xFF) * 100);
        w.regInt[p * NREG + r] = (char) v;
    }

    static final int[] BED_REGIONS = {PELVIS, LUMBAR, THORACIC, L_THIGH, R_THIGH, L_SHIN, R_SHIN, NECK};

    /** Daily: heal broken bones (only for people who have any). */
    static void dailyHeal(World w, int p, int years, int healPct) {
        if (w.boneHurt[p] == 0) { w.bonePain[p] = 0; w.boneBed[p] = false; return; }
        int base = p * NBONES;
        int pct = healPct * (years < 15 ? 200 : years >= 60 ? 60 : 100) / 100;
        int hurt = 0;
        long pain = 0;
        int regMask = 0;
        for (int b = 0; b < NBONES; b++) {
            int v = w.bone[base + b];
            if (v >= HP_MAX) continue;
            v = Math.min(HP_MAX, v + Math.max(1, HEAL_PER_DAY[b] * pct / 100));
            w.bone[base + b] = (char) v;
            if (v < HP_MAX) { hurt++; pain += HP_MAX - v; }
            regMask |= 1 << REGION[b];
        }
        w.boneHurt[p] = (char) hurt;
        w.bonePain[p] = (char) Math.min(HP_MAX, pain / 2);
        for (int r = 0; r < NREG; r++) if (((regMask >>> r) & 1) != 0) regionUpdate(w, p, r);
        boolean bedB = false;
        for (int r : BED_REGIONS) if (w.regInt[p * NREG + r] < 3000) { bedB = true; break; }
        w.boneBed[p] = bedB;
    }

    /**
     * Monthly: bone density. [src~] peak around 30; ~0.4%/yr loss after 35 in both
     * sexes; women lose ~2%/yr for the 5-10 years after menopause (~50), then ~0.7%/yr;
     * bed rest costs ~0.5%/week of mostly trabecular bone; malnutrition and some
     * diseases (thyroid, myeloma, steroids, anorexia) add to it.
     */
    static void monthly(World w, int p, int years, int bmdLoss) {
        int v = w.bmd[p];
        if (years < 30) v += (HP_MAX - v) / 20;
        else if (years >= 35) {
            int loss = 3;                                                   // ~0.4 %/yr
            if (w.sex[p] == SEX_F && years >= 50) loss = years < 58 ? 17 : 6;
            else if (years >= 70) loss = 6;
            v -= loss;
        }
        v -= (w.bedDays[p] & 0xFF) * 4;                                      // [tune] 0.04 %/day in bed
        if (Health.nutrition(w, p) < 3000) v -= 10;
        v -= bmdLoss;
        v = Math.max(1500, Math.min(HP_MAX, v));
        w.bmd[p] = (char) v;
        w.bedDays[p] = 0;
    }

    /** Bone disease eating a bone (dot on an ON_BONE condition). */
    static void hurtBone(World w, Health h, int p, int b, int amount, int causeId, int years) {
        int i = p * NBONES + b;
        int before = w.bone[i];
        int now = Math.max(0, before - amount);
        w.bone[i] = (char) now;
        if (before == HP_MAX && now < HP_MAX) w.boneHurt[p]++;
        // weakened bone gives way: pathological fracture
        if (before >= 3000 && now < 3000) { w.fractures++; w.fracCount[b]++; }
        regionUpdate(w, p, REGION[b]);
    }
}
