package meatbox;

import static meatbox.Config.*;
import static meatbox.World.*;

/**
 * =============================================================================
 *  PSYCH  -  the state of mind, rebuilt every day from body and circumstances.
 * =============================================================================
 *  psy[p*NPSY + PSY_xxx] is an OUTPUT for the behaviour code: it does not decide
 *  anything here. Each dimension = temperament (from genes) + stress + pain +
 *  disability + sickness + blood chemistry + every active condition's effect.
 *
 *  Grounding [src~ unless marked]:
 *   - chronic pain roughly doubles to quadruples depression risk and lowers mood
 *   - "sickness behaviour": fever and inflammation cut energy and social drive
 *   - anaemia, low O2 delivery, low glucose and uraemia cloud thinking
 *   - hypothyroid: low mood, low energy; hyperthyroid: anxiety, restlessness
 *   - hunger makes people irritable and low ("hangry") [src~ Keys 1950 Minnesota study]
 *   - aggression peaks in young men (age-crime curve) [src~]
 *
 *  Suicide is the one death decided here, as a daily hazard that rises with
 *  very low mood, impulsivity and psychosis. SUICIDE_BASE is [tune]d so a
 *  population lands near real rates (~10/100k/yr worldwide, WHO 2019).
 */
final class Psych {
    private Psych() {}

    static void compute(World w, Health h, int p, int years) {
        final int pb = p * NPSY, fb = p * NFN, b2 = p * 4, bb = p * 8;
        final int lb = p * Genes.NFAM;
        int gMood = (w.liab[lb + Genes.G_MOOD] & 0xFF) - 128;
        int gAnx = (w.liab[lb + Genes.G_ANXIETY] & 0xFF) - 128;
        int gExt = (w.liab[lb + Genes.G_EXTERNAL] & 0xFF) - 128;

        int mood = 1000 - gMood * 15;
        int anx = 1500 + gAnx * 15;
        int energy = 8000;
        int cog = w.func[fb + FN_COGNITION];
        int imp = 2000 + gExt * 15 + (years < 25 ? (25 - years) * 120 : 0);   // impulse control matures ~25
        int agg = 1000 + gExt * 10;
        if (w.sex[p] == SEX_M && years >= 15 && years <= 30) agg += 1500;
        int social = 2000;
        int reality = 0;

        int st = w.stress[p];
        mood -= st * 6 / 10; anx += st / 2; energy -= st / 4; agg += st / 8; social -= st / 4;

        int pain = w.blood[bb + B_PAIN];
        mood -= pain / 3; energy -= pain / 2; agg += pain / 10; social -= pain / 5;

        if (years >= 5) mood -= (HP_MAX - w.func[fb + FN_MOBILITY]) / 6;

        int temp = w.blood[bb + B_TEMP];
        if (temp > 3750) { energy -= (temp - 3750) * 10; social -= (temp - 3750) * 10; }
        int perf = w.blood[bb + B_PERF];
        energy -= (HP_MAX - perf) / 2;
        int glu = w.blood2[b2 + B2_GLU];
        if (glu < 350) { cog -= (350 - glu) * 20; agg += (350 - glu) * 10; }
        else if (glu > 1500) { energy -= (glu - 1500) / 2; cog -= (glu - 1500) / 3; }
        int meta = w.blood2[b2 + B2_META] - HP_MAX;
        energy += meta / 2;
        if (meta > 0) anx += meta / 2; else { mood += meta / 3; cog += meta / 4; }
        int fat = Health.nutrition(w, p);
        if (fat < 5000) { mood -= (5000 - fat) / 3; agg += (5000 - fat) / 5; energy -= (5000 - fat) / 3; }
        int warm = Health.exposure(w, p);
        if (warm < 7000) mood -= (7000 - warm) / 4;

        int[] acc = h.psyAcc;
        mood += acc[PSY_MOOD]; anx += acc[PSY_ANXIETY]; energy += acc[PSY_ENERGY]; cog += acc[PSY_COGNITION];
        imp += acc[PSY_IMPULSE]; agg += acc[PSY_AGGRESSION]; social += acc[PSY_SOCIAL]; reality += acc[PSY_REALITY];

        w.psy[pb + PSY_MOOD] = (short) clamp(mood, -HP_MAX, HP_MAX);
        w.psy[pb + PSY_ANXIETY] = (short) clamp(anx, 0, HP_MAX);
        w.psy[pb + PSY_ENERGY] = (short) clamp(energy, 0, HP_MAX);
        w.psy[pb + PSY_COGNITION] = (short) clamp(cog, 0, HP_MAX);
        w.psy[pb + PSY_IMPULSE] = (short) clamp(imp, 0, HP_MAX);
        w.psy[pb + PSY_AGGRESSION] = (short) clamp(agg, 0, HP_MAX);
        w.psy[pb + PSY_SOCIAL] = (short) clamp(social, -HP_MAX, HP_MAX);
        w.psy[pb + PSY_REALITY] = (short) clamp(reality, 0, HP_MAX);

        // stress load: decays, rises with pain, sickness and condition effects
        int s = w.stress[p];
        s -= s >> STRESS_DECAY_SH;
        s += pain / 400 + h.stressAdd;
        if (years >= 5 && w.func[fb + FN_MOBILITY] < 3000) s += 3;
        w.stress[p] = (char) clamp(s, 0, HP_MAX);
    }

    /** Daily suicide hazard. Returns true if the person dies today. */
    static boolean suicide(World w, int p, int years) {
        if (years < 10) return false;
        int pb = p * NPSY;
        int mood = w.psy[pb + PSY_MOOD];
        if (mood > SUICIDE_MOOD) return false;
        double x = (SUICIDE_MOOD - mood) / (double) (HP_MAX + SUICIDE_MOOD);       // 0..1
        double rate = SUICIDE_BASE * x * x
                    * (1 + w.psy[pb + PSY_IMPULSE] / 10000.0)
                    * (1 + w.psy[pb + PSY_REALITY] / 10000.0);
        long p32 = (long) (rate * 4294967296.0 / 36500000.0);
        return w.rng.next32() < p32;
    }

    /**
     * Someone died: their close family grieve. Mother/father of the dead, the dead's
     * children, and siblings (same mother). Losing a child is weighted heaviest,
     * then a parent in childhood [src~ bereavement literature]; children who lose
     * a parent carry it as an adverse childhood experience.
     */
    static void bereave(World w, int dead) {
        Health h = w.health;
        if (h == null) return;
        int deadYears = Config.years(w.age[dead]);
        int m = w.mother[dead], f = w.father[dead];
        if (m != 0xFFFF && w.alive[m]) grieve(w, h, m, deadYears < 1 ? 7000 : 10000);
        if (f != 0xFFFF && w.alive[f]) grieve(w, h, f, deadYears < 1 ? 5000 : 8000);
        for (int i = 0; i < w.popHi; i++) {
            if (!w.alive[i] || i == dead) continue;
            if (w.mother[i] == dead || w.father[i] == dead) {
                int y = Config.years(w.age[i]);
                if (y < 15 && w.ace[i] < 100) w.ace[i]++;
                grieve(w, h, i, y < 2 ? 3000 : y < 15 ? 9000 : 6000);
            } else if (m != 0xFFFF && w.mother[i] == m) {
                grieve(w, h, i, 4000);
            }
        }
    }

    static void grieve(World w, Health h, int p, int sev) {
        Health.addStress(w, p, sev / 2);
        h.rollEvent(p, Afflictions.BEREAVE, sev);
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
