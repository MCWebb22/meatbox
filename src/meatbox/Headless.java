package meatbox;

/**
 * =============================================================================
 *  HEADLESS  -  simulate N years with no window, print the demographics.
 * =============================================================================
 *  Fixed seed, so every run gives the same numbers. (Up to v0.1 the output
 *  matched meatbox_bench.exe exactly; v0.2 changed the rules, so it no longer does.)
 *
 *  Columns: food(d/p) = store in days per person, wood/p = loads per person,
 *  fire% = the worst day's share of heating met that year, fat% = average
 *  energy reserve, land(d/p) = wild food standing on the map, gath(d/p) = food
 *  gathered per person per year, return% = what foragers find vs look for,
 *  coldest = that year's coldest day, winter-dead = deaths that winter.
 *  Rows every 25 years (every 5 for runs of 100 years or less; -Devery=N to change).
 */
final class Headless {
    private Headless() {}

    static void run(World w, Sim sim, int years) { run(w, sim, years, System.out); }

    /** Same as above, but writes to {@code out} - the regression test captures it this way. */
    static void run(World w, Sim sim, int years, java.io.PrintStream out) {
        long t0 = System.nanoTime();
        StringBuilder sb = new StringBuilder(4096);
        int every = Integer.getInteger("every", years <= 100 ? 5 : 25);
        out.println("year    pop  births  deaths  disab  food(d/p)  wood/p  fire%  trees  fat%  harvest%"
                  + "  land(d/p)  gath(d/p)  return%  coldest  winter-dead");
        long gath0 = 0;
        for (int yr = 1; yr <= years; yr++) {
            int minT = Integer.MAX_VALUE, minFire = 65536, winterDead = 0;
            for (int d = 0; d < 365; d++) {
                int before = w.deaths;
                sim.day();
                minT = Math.min(minT, w.outT);
                minFire = Math.min(minFire, w.fireQ);
                if (Climate.season(w.day % 365) == 0) winterDead += w.deaths - before;
            }
            if (yr % every == 0) {
                int trees = 0;
                for (byte t : w.map) if (t == Config.T_TREE) trees++;
                long fatSum = 0;
                for (int i = 0; i < w.popHi; i++) if (w.alive[i]) fatSum += w.fat[i];
                int pop = Math.max(1, w.popCount);
                long land = 0;
                for (int f : w.forage) land += f;
                out.printf("%4d  %5d  %6d  %6d  %5d  %9d  %6d  %5d  %5d  %4d  %8d  %9d  %9d  %7d  %6.1fC  %11d%n",
                        yr, w.popCount, w.births, w.deaths, w.disabledN,
                        w.food / Config.RATION / pop, w.wood / pop, minFire * 100 / 65536, trees,
                        fatSum / pop / 100, w.harvestPct, land / Config.RATION / pop,
                        (w.gathered - gath0) / Config.RATION / pop / every, w.returnQ * 100 / 65536,
                        minT / 100.0, winterDead);
                gath0 = w.gathered;
            }
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;

        int[] b = w.ageBucket;
        sb.append(String.format("%ndeaths by age:  <1 %d  1-14 %d  15-49 %d  50-69 %d  70+ %d%n",
                b[0], b[1], b[2], b[3], b[4]));
        sb.append("infant deaths per 1000 births: ")
           .append(w.births == 0 ? 0 : b[0] * 1000 / w.births).append('\n');
        sb.append("mean age at death (all): ")
           .append(w.deaths == 0 ? 0 : w.allAgeSum / w.deaths)
           .append("   (those who reached 15): ")
           .append(w.adultDeaths == 0 ? 0 : w.adultAgeSum / w.adultDeaths).append('\n');
        sb.append(String.format("deaths by season:  winter %d  spring %d  summer %d  autumn %d%n",
                w.seasonDeaths[0], w.seasonDeaths[1], w.seasonDeaths[2], w.seasonDeaths[3]));
        sb.append("causes of death:\n");
        for (int id = 0; id < Afflictions.COUNT; id++) {
            int n = w.causeCount[id];
            if (n != 0) sb.append(String.format("  %-20s%6d%n", Afflictions.TABLE[id].name, n));
        }
        int aliveFlags = 0;
        for (int i = 0; i < w.popHi; i++) if (w.alive[i]) aliveFlags++;
        if (aliveFlags != w.popCount) sb.append("INVARIANT FAILED: alive flags != pop_count\n");
        sb.append("person-days simulated: ").append(w.personDays).append('\n');
        sb.append("elapsed ms: ").append(ms).append('\n');
        if (w.personDays > 0)
            sb.append(String.format("ns per person-day: %.1f%n", ms * 1e6 / w.personDays));
        out.print(sb);
        out.flush();
    }
}
