package meatbox;

import static meatbox.World.*;

/**
 * =============================================================================
 *  HEALTH REPORT  -  the end-of-run health statistics (part of --headless).
 * =============================================================================
 *  Prevalence = share of the living who have it now. Cases = new cases started
 *  during the whole run. Suicide rate is per 100,000 person-years.
 */
final class HealthReport {
    private HealthReport() {}

    static void append(World w, StringBuilder sb) {
        int pop = Math.max(1, w.popCount);
        int[] prev = new int[Afflictions.COUNT];
        int adults = 0, kids = 0;
        long[] psySum = new long[NPSY];
        long[] fnSum = new long[NFN];
        int osteo = 0, women50 = 0;
        for (int p = 0; p < w.popHi; p++) {
            if (!w.alive[p]) continue;
            int y = Config.years(w.age[p]);
            if (y >= 15) adults++; else kids++;
            for (int s = 0; s < Config.NSLOTS; s++) {
                int id = w.affId[p * Config.NSLOTS + s];
                if (id != 0) prev[id]++;
            }
            for (int i = 0; i < NPSY; i++) psySum[i] += w.psy[p * NPSY + i];
            for (int i = 0; i < NFN; i++) fnSum[i] += w.func[p * NFN + i];
            if (w.sex[p] == Config.SEX_F && y >= 50) { women50++; if (w.bmd[p] < Config.OSTEO_BMD) osteo++; }
        }
        double py = w.personDays / 365.0;
        sb.append(String.format("%nhealth (living: %d adults, %d children)%n", adults, kids));
        sb.append(String.format("  suicides %d = %.1f per 100k person-years   stillbirths %d   miscarriages %d%n",
                w.suicides, py == 0 ? 0 : w.suicides * 100000.0 / py, w.stillbirths, w.miscarriages));
        sb.append(String.format("  fractures %d = %.2f per 100 person-years   osteoporosis in women 50+: %d of %d%n",
                w.fractures, py == 0 ? 0 : w.fractures * 100.0 / py, osteo, women50));
        sb.append("  most broken bones:");
        boolean[] used = new boolean[Bones.NBONES];
        for (int k = 0; k < 6; k++) {
            int best = -1;
            for (int b = 0; b < Bones.NBONES; b++) if (!used[b] && w.fracCount[b] > 0 && (best < 0 || w.fracCount[b] > w.fracCount[best])) best = b;
            if (best < 0) break;
            used[best] = true;
            sb.append(' ').append(Bones.name(best)).append(' ').append(w.fracCount[best]).append(k < 5 ? "," : "");
        }
        sb.append('\n');
        sb.append("  mind (mean of the living):");
        for (int i = 0; i < NPSY; i++) sb.append(' ').append(PSY_NAME[i]).append(' ').append(psySum[i] / pop / 100);
        sb.append("\n  body (mean of the living):");
        for (int i = 0; i < NFN; i++) sb.append(' ').append(FN_NAME[i]).append(' ').append(fnSum[i] / pop / 100);
        sb.append('\n');
        if (w.slotFull > 0) sb.append("  conditions dropped because all ").append(Config.NSLOTS).append(" slots were full: ").append(w.slotFull).append('\n');

        sb.append("  by group:                    cases   now  prevalence%\n");
        for (int c = 1; c < Afflictions.CAT_NAME.length; c++) {
            long cases = 0; int now = 0;
            for (Afflictions.Aff a : Afflictions.TABLE) if (a.cat == c) { cases += w.caseCount[a.id]; now += prev[a.id]; }
            if (cases == 0 && now == 0) continue;
            sb.append(String.format("    %-26s%8d%6d%n", Afflictions.CAT_NAME[c], cases, now));
        }
        sb.append("  commonest now (prevalence among the living):\n");
        boolean[] u2 = new boolean[Afflictions.COUNT];
        for (int k = 0; k < 25; k++) {
            int best = -1;
            for (int id = 1; id < Afflictions.COUNT; id++)
                if (!u2[id] && prev[id] > 0 && (best < 0 || prev[id] > prev[best])) best = id;
            if (best < 0) break;
            u2[best] = true;
            sb.append(String.format("    %-40s%5d  %5.1f%%   (%d cases in run)%n", Afflictions.TABLE[best].name, prev[best],
                    prev[best] * 100.0 / pop, w.caseCount[best]));
        }
        sb.append("  mental & behavioural, cases in run:");
        int n = 0;
        for (Afflictions.Aff a : Afflictions.TABLE)
            if (a.cat == Afflictions.C_MENTAL && w.caseCount[a.id] > 0) {
                sb.append(n++ % 4 == 0 ? "\n    " : "   ").append(a.name).append(' ').append(w.caseCount[a.id]);
            }
        sb.append('\n');
    }
}
