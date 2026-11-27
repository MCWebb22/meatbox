package meatbox;

/**
 * =============================================================================
 *  MEATBOX (Java)  -  entry point
 * =============================================================================
 *    java -cp out meatbox.Main                   window (Swing)
 *    java -cp out meatbox.Main --headless        300 years, print statistics
 *    java -cp out meatbox.Main --headless 1000   any number of years
 *
 *  Start-up order matters (same as the asm): tables, then the map, then people.
 *  Each step draws random numbers; changing the order changes the world.
 */
public final class Main {
    /** The headless seed of meatbox.asm: 2^64 / golden ratio. */
    static final long FIXED_SEED = 0x9E3779B97F4A7C15L;

    public static void main(String[] args) throws Exception {
        boolean headless = args.length > 0 && args[0].equals("--headless");
        int years = 300;
        if (headless && args.length > 1) years = Integer.parseInt(args[1]);

        // Interactive runs get a fresh world each time; "| 1" keeps it non-zero.
        long seed = headless ? FIXED_SEED : (System.nanoTime() * FIXED_SEED) | 1;

        Body.init();                     // body table -> fast arrays (+ sanity checks)
        World w = new World(seed);       // Afflictions loads itself on first use
        w.worldInit();
        w.popInit();
        Sim sim = new Sim(w);

        if (args.length > 1 && args[0].equals("--snapshot")) {   // test: frame -> PNG
            Screen.snapshot(w, sim, Integer.parseInt(args[1]) * 365, "snapshot.png");
            return;
        }
        if (headless) Headless.run(w, sim, years);
        else Screen.open(w, sim);
    }
}
