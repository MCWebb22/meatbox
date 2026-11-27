package meatbox;

/**
 * =============================================================================
 *  SHORTAGE TEST  -  food, seasons and firewood behave sensibly
 * =============================================================================
 *  1. Ten different seeds, 300 years each: the tribe must never die out, and
 *     hunger must actually kill sometimes (or food isn't limiting anything).
 *  2. Take away all firewood for five winters: people must die of hypothermia.
 *
 *  Run with  test.sh  /  test.bat  (after the regression test). Exit 0 = pass.
 */
public final class ShortageTest {
    public static void main(String[] args) {
        int fails = 0;

        int starved = 0;
        for (int k = 1; k <= 10; k++) {
            World w = Main.newWorld(Main.FIXED_SEED * (2L * k + 1));
            Sim sim = new Sim(w);
            int min = Integer.MAX_VALUE, max = 0;
            for (int d = 0; d < 300 * 365; d++) {
                sim.day();
                if (d > 50 * 365) { min = Math.min(min, w.popCount); max = Math.max(max, w.popCount); }
            }
            starved += w.causeCount[Catalogue.STARVATION.id];
            System.out.printf("seed %2d: population after year 50 ranged %d..%d%n", k, min, max);
            if (min == 0) { System.out.println("  FAIL: died out"); fails++; }
        }
        System.out.println("starvation deaths over all seeds: " + starved);
        if (starved == 0) { System.out.println("  FAIL: nobody ever starved"); fails++; }

        World w = Main.newWorld(Main.FIXED_SEED);
        Sim sim = new Sim(w);
        for (int d = 0; d < 30 * 365; d++) sim.day();
        int before = w.causeCount[Catalogue.HYPOTHERMIA.id];
        for (int d = 0; d < 5 * 365; d++) { w.wood = 0; w.burnAcc = 0; sim.day(); }
        int cold = w.causeCount[Catalogue.HYPOTHERMIA.id] - before;
        System.out.println("hypothermia deaths in 5 winters without firewood: " + cold);
        if (cold < 10) { System.out.println("  FAIL: cold without fire should kill"); fails++; }

        System.out.println(fails == 0 ? "PASS" : "FAIL (" + fails + ")");
        System.exit(fails == 0 ? 0 : 1);
    }
}
