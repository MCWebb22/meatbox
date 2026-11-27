package meatbox;

/**
 * =============================================================================
 *  HEADLESS  -  simulate N years with no window, print the demographics.
 * =============================================================================
 *  Fixed seed, so every run gives the same numbers. With 300 years the output
 *  is byte-for-byte the same as  meatbox_bench.exe  (the assembly version) -
 *  apart from "elapsed ms", which depends on the machine.
 */
final class Headless {
    private Headless() {}

    static void run(World w, Sim sim, int years) {
        long t0 = System.nanoTime();
        StringBuilder out = new StringBuilder(4096);
        for (int yr = 1; yr <= years; yr++) {
            for (int d = 0; d < 365; d++) sim.day();
            if (yr % 25 == 0) {
                System.out.printf("year %4d  pop %5d  births %6d  deaths %6d  disabled %4d  wood %6d%n",
                        yr, w.popCount, w.births, w.deaths, w.disabledN, w.wood);
            }
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;

        int[] b = w.ageBucket;
        out.append(String.format("%ndeaths by age:  <1 %d  1-14 %d  15-49 %d  50-69 %d  70+ %d%n",
                b[0], b[1], b[2], b[3], b[4]));
        out.append("infant deaths per 1000 births: ")
           .append(w.births == 0 ? 0 : b[0] * 1000 / w.births).append('\n');
        out.append("mean age at death (all): ")
           .append(w.deaths == 0 ? 0 : w.allAgeSum / w.deaths)
           .append("   (those who reached 15): ")
           .append(w.adultDeaths == 0 ? 0 : w.adultAgeSum / w.adultDeaths).append('\n');
        out.append("causes of death:\n");
        for (int id = 0; id < Afflictions.COUNT; id++) {
            int n = w.causeCount[id];
            if (n != 0) out.append(String.format("  %-20s%6d%n", Afflictions.TABLE[id].name, n));
        }
        int aliveFlags = 0;
        for (int i = 0; i < w.popHi; i++) if (w.alive[i]) aliveFlags++;
        if (aliveFlags != w.popCount) out.append("INVARIANT FAILED: alive flags != pop_count\n");
        out.append("person-days simulated: ").append(w.personDays).append('\n');
        out.append("elapsed ms: ").append(ms).append('\n');
        if (w.personDays > 0)
            out.append(String.format("ns per person-day: %.1f%n", ms * 1e6 / w.personDays));
        System.out.print(out);
    }
}
