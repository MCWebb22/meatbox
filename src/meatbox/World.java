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
    final int[]  forage = new int[MAP_SIZE];     // standing wild food on each tile, kcal

    // ---- people (index 0 .. MAX_POP-1) ---------------------------------------------
    final char[]  hp      = new char[MAX_POP * NPARTS];  // 48 organs, muscles, joints each
    final char[]  affId   = new char[MAX_POP * NSLOTS];  // condition id (0 = empty slot)
    final char[]  affPart = new char[MAX_POP * NSLOTS];  // body part, or bone number if ON_BONE
    final char[]  affDays = new char[MAX_POP * NSLOTS];  // days left (acute) / so far (chronic)
    final char[]  affSev  = new char[MAX_POP * NSLOTS];  // severity 0..10000; bit 15 = dormant phase
    final byte[]  affN    = new byte[MAX_POP];           // slots in use
    final char[]  blood   = new char[MAX_POP * 8];       // B_* below (stride 8 kept for v0.1 code)
    final char[]  blood2  = new char[MAX_POP * 4];       // B2_* below: haemoglobin, glucose, BP, metabolism
    final byte[]  mind    = new byte[MAX_POP * 16];      // RESERVED for the behaviour code
    final long[]  immune  = new long[MAX_POP];           // bit n = immune to the n-th IMMUNE condition
    final char[]  age     = new char[MAX_POP];           // days
    final short[] preg    = new short[MAX_POP];          // >0 pregnant, <0 postpartum
    final char[]  name    = new char[MAX_POP];           // name seed
    final char[]  mother  = new char[MAX_POP];           // 0xFFFF = none
    final char[]  father  = new char[MAX_POP];           // 0xFFFF = unknown
    final long[]  lame    = new long[MAX_POP];           // parts disabled for good (bit = part)
    final char[]  cap     = new char[MAX_POP];           // legacy usable-parts bits (Body.EYES, LARM...)
    final boolean[] alive = new boolean[MAX_POP];
    final byte[]  sex     = new byte[MAX_POP];
    final byte[]  x       = new byte[MAX_POP];
    final byte[]  y       = new byte[MAX_POP];
    final byte[]  konst   = new byte[MAX_POP];           // constitution 0..16
    final char[]  cause   = new char[MAX_POP];           // what zeroed a vital organ
    final char[]  bleeder = new char[MAX_POP];           // last thing that bled
    final boolean[] bed   = new boolean[MAX_POP];        // bedridden today
    final char[]  recent  = new char[MAX_POP];           // last serious affliction
    final char[]  fat     = new char[MAX_POP];           // energy reserve 0..10000 (10000 = well fed)
    final byte[]  rank    = new byte[MAX_POP];           // standing 0..255: low = fed first when short
    final char[]  warmth  = new char[MAX_POP];           // 10000 = comfortable, 0 = core cooled 7 C+

    // ---- skeleton (Bones / Skeleton) ---------------------------------------------------
    final char[]  bone     = new char[MAX_POP * Bones.NBONES]; // integrity per bone, 10000 = whole
    final char[]  bmd      = new char[MAX_POP];          // bone mineral density, 10000 = young peak
    final byte[]  boneStr  = new byte[MAX_POP];          // personal bone strength %, 100 normal (OI: 40)
    final char[]  boneHurt = new char[MAX_POP];          // bones currently below 10000
    final char[]  regInt   = new char[MAX_POP * Bones.NREG]; // worst-bone integrity per region (cached)
    final byte[]  regCap   = new byte[MAX_POP * Bones.NREG]; // % a region can ever reach (malunion, deformity)
    final char[]  bonePain = new char[MAX_POP];
    final byte[]  cordLvl  = new byte[MAX_POP];          // highest spinal cord injury: 1=C1 .. 24=L5, 0 = none
    final boolean[] boneBed = new boolean[MAX_POP];      // a broken weight-bearing bone keeps them in bed

    // ---- what the body can do, and the state of mind (read by behaviour) -----------------
    final char[]  func    = new char[MAX_POP * NFN];     // FN_* 0..10000
    final short[] psy     = new short[MAX_POP * NPSY];   // PSY_* (mood -10000..10000, rest 0..10000)
    final char[]  stress  = new char[MAX_POP];           // chronic stress load 0..10000
    final char[]  immun   = new char[MAX_POP];           // immune strength today, 10000 = healthy adult
    final byte[]  ace     = new byte[MAX_POP];           // adverse childhood experiences (bereavement < 15)
    final byte[]  headInj = new byte[MAX_POP];           // count of brain injuries
    final char[]  labour  = new char[MAX_POP];           // recent heavy work (chopping) 0..10000
    final byte[]  pregFlags = new byte[MAX_POP];         // infections caught while pregnant (PF_*)
    final byte[]  bedDays = new byte[MAX_POP];           // days in bed this month (bone loss)
    final boolean[] doomed = new boolean[MAX_POP];       // sudden death today (cause already set)

    // ---- genes (Genes) ------------------------------------------------------------------
    final byte[]  liab    = new byte[MAX_POP * Genes.NFAM];  // heritable liability per family, 128 = average
    final int[]   alleles = new int[MAX_POP];            // 2 bits per Mendelian gene

    // ---- bloodstream slots: blood[person*8 + B_xxx] ---------------------------------
    static final int B_VOL = 0, B_O2 = 1, B_PERF = 2, B_PATH = 3,
                     B_WASTE = 4, B_TEMP = 5, B_HR = 6, B_PAIN = 7;
    // blood2[person*4 + B2_xxx]
    static final int B2_HB = 0,    // haemoglobin, 10000 = normal (14 g/dL)
                     B2_GLU = 1,   // glucose, hundredths of mmol/L (500 = 5.0)
                     B2_BP = 2,    // systolic blood pressure, mmHg
                     B2_META = 3;  // metabolic rate (thyroid), 10000 = normal

    // ---- function scores: func[person*NFN + FN_xxx] (0..10000) ------------------------
    static final int FN_MOBILITY = 0, FN_MANIP_L = 1, FN_MANIP_R = 2, FN_VISION = 3, FN_HEARING = 4,
                     FN_STAMINA = 5, FN_COGNITION = 6, FN_SPEECH = 7, FN_WORK = 8, NFN = 9;
    static final int FN_MANIP = 99;   // catalogue shorthand: both hands
    // ---- psych: psy[person*NPSY + PSY_xxx] ----------------------------------------------
    static final int PSY_MOOD = 0,       // -10000 despair .. 0 neutral .. +10000 elation
                     PSY_ANXIETY = 1,    // 0..10000
                     PSY_ENERGY = 2,     // motivation / fatigue, 0..10000 (8000 typical)
                     PSY_COGNITION = 3,  // clarity right now, 0..10000
                     PSY_IMPULSE = 4,    // 0..10000
                     PSY_AGGRESSION = 5, // 0..10000
                     PSY_SOCIAL = 6,     // -10000 withdrawn .. +10000 seeking company
                     PSY_REALITY = 7,    // 0 = sound .. 10000 = fully psychotic
                     NPSY = 8;
    static final String[] PSY_NAME = {"mood", "anxiety", "energy", "clarity", "impulse", "aggression",
                                      "social", "psychosis"};
    static final String[] FN_NAME = {"walk", "L hand", "R hand", "sight", "hearing", "stamina",
                                     "brain", "speech", "work"};
    static final int PF_RUBELLA = 1, PF_SYPHILIS = 2;
    static final int NBAND = 21;      // 5-year age bands for disease rates (0-4 .. 100+)

    // ---- free list: dead people's numbers get reused ----------------------------------
    final char[] freeStack = new char[MAX_POP];
    int freeTop;
    int popHi;          // highest number ever used + 1 (loop bound)
    int popCount;       // people alive now

    // ---- counters -------------------------------------------------------------------------
    int menCount, menPrev, day, births, deaths, wood, burnAcc, disabledN;
    final int[] causeCount = new int[4096];
    final int[] caseCount  = new int[4096];  // new cases started, by condition
    final int[] causeAgeSum = new int[4096], causeKids = new int[4096];   // by cause: sum of ages, deaths under 15
    final int[] fracCount  = new int[Bones.NBONES];
    int fractures, stillbirths, suicides, miscarriages;
    final int[] ageBucket  = new int[5];     // <1, 1-14, 15-49, 50-69, 70+
    int adultAgeSum, adultDeaths, allAgeSum;
    long personDays;

    // ---- seasons, food, fire ----------------------------------------------------------------
    int  outT;                 // today's outdoor temperature (hundredths C)
    int  anomaly;              // weather: today's departure from the seasonal mean
    int  harvestPct = 100;     // this year's luck for wild food
    long food;                 // the shared store, kcal
    long demand, demandPrev;   // kcal everyone wanted today / yesterday
    int  foodQ = 65536;        // today's ration: share of need eaten, out of 65536
    int  fertQ = 65536;        // today's conception multiplier from food security, out of 65536
    int  fireQ = 65536;        // today's fire: share of heating demand met, out of 65536
    long gathered, eaten;      // kcal totals (statistics)
    long gatherTry, gatherGot; // today's foraging: kcal sought vs found
    int  returnQ = 65536;      // foraging return (found / sought), ~1-year running average, /65536
    final int[] seasonDeaths = new int[4];

    int slotFull;              // conditions not given because all slots were taken
    Health health;             // set by Sim

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
        for (int i = 0; i < MAP_SIZE; i++)                  // start half-full
            forage[i] = FORAGE_YIELD[map[i]] * FORAGE_CAP_PCT / 200;
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
            fat[p] = FAT_START;
        }
        food = (long) popCount * RATION * START_STORE_DAYS;
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
        father[p] = 0xFFFF; doomed[p] = false;
        fat[p] = FAT_NEWBORN; warmth[p] = HP_MAX;
        rank[p] = (byte) rng.below(256);
        immune[p] = 0;
        mother[p] = 0xFFFF;
        cap[p] = 0xFFFF;
        name[p] = (char) rng.next32();                       // low 16 bits
        konst[p] = (byte) rng.below(17);
        long absent = sex[p] == SEX_M ? Body.femaleOnly : Body.maleOnly;
        for (int i = 0; i < NPARTS; i++) hp[p * NPARTS + i] = ((absent >>> i) & 1) != 0 ? 0 : (char) HP_MAX;
        for (int i = 0; i < NSLOTS; i++) {
            affId[p * NSLOTS + i] = 0; affPart[p * NSLOTS + i] = 0;
            affDays[p * NSLOTS + i] = 0; affSev[p * NSLOTS + i] = 0;
        }
        affN[p] = 0;
        for (int i = 0; i < 16; i++) mind[p * 16 + i] = 0;
        Skeleton.reset(this, p);
        int b2 = p * 4;
        blood2[b2 + B2_HB] = HP_MAX; blood2[b2 + B2_GLU] = 500; blood2[b2 + B2_BP] = 120; blood2[b2 + B2_META] = HP_MAX;
        for (int i = 0; i < NFN; i++) func[p * NFN + i] = HP_MAX;
        for (int i = 0; i < NPSY; i++) psy[p * NPSY + i] = 0;
        stress[p] = 0; ace[p] = 0; headInj[p] = 0; labour[p] = 0; pregFlags[p] = 0; bedDays[p] = 0;
        Genes.founder(this, p);
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
        causeCount[cause[p]]++;
        int yrs = years(age[p]);
        causeAgeSum[cause[p]] += yrs;
        if (yrs < 15) causeKids[cause[p]]++;
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
        seasonDeaths[Climate.season(day % 365)]++;
        freeStack[freeTop++] = (char) p;
        if (cause[p] == Catalogue.SUICIDE.id) suicides++;
        log(personName(p) + " died: " + Afflictions.TABLE[cause[p]].name + " (age " + yrs + ")");
        Psych.bereave(this, p);
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
