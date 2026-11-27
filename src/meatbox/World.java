package meatbox;

import static meatbox.Config.*;
import static meatbox.Body.*;

/**
 * =============================================================================
 *  WORLD  -  all the state: the map, every person, the counters, the log.
 * =============================================================================
 *  KEY IDEA: STRUCTURE OF ARRAYS (SoA)
 *  -----------------------------------
 *  The obvious Java design is   class Person { int age; short[] hp; ... }
 *  and a list of 4096 Person objects. Each object lives somewhere random on
 *  the heap, so walking the list jumps all over memory (slow cache misses).
 *
 *  Instead, ONE array per field, indexed by person number:
 *
 *      age[ 0 ] age[ 1 ] age[ 2 ] ...      <- all ages side by side
 *      hp [ person*16 + part ]             <- 16 organs per person, packed
 *
 *  This is exactly the memory layout of the assembly version, and it is why
 *  the Java port stays fast: the daily loop streams through memory in order.
 *
 *  KEY IDEA: "char" AS AN UNSIGNED 16-BIT NUMBER
 *  ---------------------------------------------
 *  Java has no unsigned types... except char: 16 bits, 0..65535. The asm stores
 *  HP, age, names, masks as unsigned 16-bit words, so char[] is the exact match.
 *  (We never print these as letters; they are just numbers.)
 */
final class World {

    // ---- the map: one byte per tile (T_GRASS .. T_STUMP) ----------------------
    final byte[] map  = new byte[MAP_SIZE];
    final byte[] map2 = new byte[MAP_SIZE];      // scratch for smoothing

    // ---- people (index 0 .. MAX_POP-1) ---------------------------------------------
    final char[]  hp      = new char[MAX_POP * NPARTS];  // 16 parts each
    final int[]   aff     = new int[MAX_POP * NSLOTS];   // id | part<<8 | days<<16
    final char[]  blood   = new char[MAX_POP * 8];       // B_* below
    final byte[]  mind    = new byte[MAX_POP * 16];      // RESERVED: mood, wants
    final long[]  immune  = new long[MAX_POP];           // bit n = immune to aff n
    final char[]  age     = new char[MAX_POP];           // days
    final short[] preg    = new short[MAX_POP];          // >0 pregnant, <0 postpartum
    final char[]  name    = new char[MAX_POP];           // name seed
    final char[]  mother  = new char[MAX_POP];           // 0xFFFF = none
    final char[]  lame    = new char[MAX_POP];           // parts disabled for good
    final char[]  cap     = new char[MAX_POP];           // parts usable today
    final boolean[] alive = new boolean[MAX_POP];
    final byte[]  sex     = new byte[MAX_POP];
    final byte[]  x       = new byte[MAX_POP];
    final byte[]  y       = new byte[MAX_POP];
    final byte[]  konst   = new byte[MAX_POP];           // constitution 0..16
    final byte[]  cause   = new byte[MAX_POP];           // what zeroed a vital organ
    final byte[]  bleeder = new byte[MAX_POP];           // last thing that bled
    final boolean[] bed   = new boolean[MAX_POP];        // bedridden today
    final byte[]  recent  = new byte[MAX_POP];           // last serious affliction

    // ---- bloodstream slots: blood[person*8 + B_xxx] ---------------------------------
    static final int B_VOL = 0, B_O2 = 1, B_PERF = 2, B_PATH = 3,
                     B_WASTE = 4, B_TEMP = 5, B_HR = 6, B_PAIN = 7;

    // ---- free list: dead people's numbers get reused ----------------------------------
    final char[] freeStack = new char[MAX_POP];
    int freeTop;
    int popHi;          // highest number ever used + 1 (loop bound)
    int popCount;       // people alive now

    // ---- counters -------------------------------------------------------------------------
    int menCount, menPrev, day, births, deaths, wood, burnAcc, disabledN;
    final int[] causeCount = new int[64];
    final int[] ageBucket  = new int[5];     // <1, 1-14, 15-49, 50-69, 70+
    int adultAgeSum, adultDeaths, allAgeSum;
    long personDays;

    final Rng rng;
    World(long seed) { rng = new Rng(seed); }

    // =====================================================================================
    //  WORLD START
    // =====================================================================================
    static final int[] DIR_DX = {-1, 0, 1, -1, 1, -1, 0, 1};
    static final int[] DIR_DY = {-1, -1, -1, 0, 0, 1, 1, 1};

    /** Random forest, then 4 passes of "cellular automaton" smoothing into clumps. */
    void worldInit() {
        for (int i = 0; i < MAP_SIZE; i++)
            map[i] = rng.next32() < TREE_INIT_P32 ? T_TREE : T_GRASS;
        for (int pass = 0; pass < 4; pass++) {
            for (int i = 0; i < MAP_SIZE; i++) {
                int n = countTrees(i & (MAP_W - 1), i >>> 6);
                boolean tree = n >= 5 || (n >= 4 && map[i] == T_TREE);
                map2[i] = tree ? T_TREE : T_GRASS;
            }
            System.arraycopy(map2, 0, map, 0, MAP_SIZE);
        }
    }

    /** Mature trees among the 8 neighbours of (x, y). */
    int countTrees(int cx, int cy) {
        int n = 0;
        for (int d = 0; d < 8; d++) {
            int nx = cx + DIR_DX[d], ny = cy + DIR_DY[d];
            // Integer.compareUnsigned trick from the asm: a negative number seen as
            // unsigned is huge, so ONE compare rejects both  < 0  and  >= MAP_W.
            if (Integer.compareUnsigned(nx, MAP_W) >= 0) continue;
            if (Integer.compareUnsigned(ny, MAP_H) >= 0) continue;
            if (map[ny * MAP_W + nx] == T_TREE) n++;
        }
        return n;
    }

    /** START_POP adults and children, scattered in the middle of the map. */
    void popInit() {
        for (int k = 0; k < START_POP; k++) {
            int p = newPerson();
            if (p == -1) break;
            age[p] = (char) rng.below(40 * 365);
            x[p] = (byte) (rng.below(20) + MAP_W / 2 - 10);
            y[p] = (byte) (rng.below(10) + MAP_H / 2 - 5);
        }
    }

    // =====================================================================================
    //  BIRTH OF A RECORD  /  DEATH OF A RECORD
    // =====================================================================================
    /** A healthy newborn record. Returns its number, or -1 if the world is full. */
    int newPerson() {
        int p;
        if (freeTop > 0) p = freeStack[--freeTop];          // reuse a dead slot
        else if (popHi < MAX_POP) p = popHi++;               // or take a fresh one
        else return -1;

        alive[p] = true;
        sex[p] = (byte) (rng.next32() & 1);
        age[p] = 0; preg[p] = 0; lame[p] = 0;
        cause[p] = 0; bleeder[p] = 0; bed[p] = false; recent[p] = 0;
        immune[p] = 0;
        mother[p] = 0xFFFF;
        cap[p] = 0xFFFF;
        name[p] = (char) rng.next32();                       // low 16 bits
        konst[p] = (byte) rng.below(17);
        for (int i = 0; i < NPARTS; i++) hp[p * NPARTS + i] = HP_MAX;
        for (int i = 0; i < NSLOTS; i++) aff[p * NSLOTS + i] = 0;
        for (int i = 0; i < 16; i++) mind[p * 16 + i] = 0;
        int b = p * 8;
        blood[b + B_VOL] = HP_MAX;  blood[b + B_O2] = HP_MAX;  blood[b + B_PERF] = HP_MAX;
        blood[b + B_PATH] = 0;      blood[b + B_WASTE] = 0;    blood[b + B_TEMP] = 3700;
        blood[b + B_HR] = 75;       blood[b + B_PAIN] = 0;
        popCount++;
        return p;                    // NOTE: x, y are NOT reset - the caller sets them
    }

    /** Death: statistics, then the number goes back on the free stack. */
    void kill(int p) {
        alive[p] = false;
        popCount--;
        deaths++;
        causeCount[cause[p] & 0xFF]++;
        int yrs = years(age[p]);
        allAgeSum += yrs;
        int bucket;
        if (yrs < 1) bucket = 0;
        else if (yrs < 15) bucket = 1;
        else {
            adultAgeSum += yrs;            // "those who reached 15"
            adultDeaths++;
            bucket = yrs < 50 ? 2 : yrs < 70 ? 3 : 4;
        }
        ageBucket[bucket]++;
        freeStack[freeTop++] = (char) p;
        log(personName(p) + " died: " + Afflictions.TABLE[cause[p] & 0xFF].name + " (age " + yrs + ")");
    }

    // =====================================================================================
    //  EVENT LOG (last 4 lines) and NAMES
    // =====================================================================================
    static final int LOG_N = 4, LOG_MAX = 79;
    final String[] logLines = {"", "", "", ""};

    void log(String text) {
        String line = "Y" + (day / 365) + " " + text;
        if (line.length() > LOG_MAX) line = line.substring(0, LOG_MAX);
        System.arraycopy(logLines, 1, logLines, 0, LOG_N - 1);   // scroll up
        logLines[LOG_N - 1] = line;
    }

    /** 16 two-letter syllables. A name = 2 or 3 of them, chosen by bits of the seed. */
    static final String SYLLABLES = "karitominasuhakoraniyutakemosaji";

    static String nameFromSeed(int s) {
        StringBuilder sb = new StringBuilder(6);
        int a = (s & 15) * 2;
        sb.append(Character.toUpperCase(SYLLABLES.charAt(a))).append(SYLLABLES.charAt(a + 1));
        int b = ((s >>> 4) & 15) * 2;
        sb.append(SYLLABLES, b, b + 2);
        if ((s & 0x1000) != 0) {                     // bit 12 set: third syllable
            int c = ((s >>> 8) & 15) * 2;
            sb.append(SYLLABLES, c, c + 2);
        }
        return sb.toString();
    }

    String personName(int p) { return nameFromSeed(name[p]); }

    int years(int days) { return Config.years(days); }
}
