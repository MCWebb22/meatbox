package meatbox;

/**
 * =============================================================================
 *  CONFIG  -  every tuning number in one place.
 * =============================================================================
 *  Same numbers, same meaning as section 1 of meatbox.asm.
 *
 *  Units used everywhere in Meatbox:
 *    - HP and most blood values: 0..10000 = 0..100.00 %   (1 unit = 0.01 %)
 *    - Temperature: hundredths of a degree C  (3700 = 37.00 C)
 *    - Chances ending in _P32: out of 2^32 (4,294,967,296), so they can be
 *      compared directly with one 32-bit random number. 2^32 / 40 = "1 in 40".
 *
 *  "static final" = a constant: Java pastes the value in wherever it's used,
 *  exactly like NASM's "equ". It costs nothing at run time.
 */
final class Config {
    private Config() {}                     // nobody makes a Config object

    // --- population & world ------------------------------------------------
    static final int MAX_POP    = 4096;     // hard cap (arrays are fixed-size)
    static final int MAP_W      = 64;       // tile index = y * 64 + x
    static final int MAP_H      = 24;
    static final int MAP_SIZE   = MAP_W * MAP_H;
    static final int START_POP  = 40;
    static final int HP_MAX     = 10000;    // 100.00 %
    static final int NSLOTS     = 8;        // afflictions carried at once
    static final int USABLE_HP  = 2500;     // a part under 25 % can't be used

    // --- ageing --------------------------------------------------------------
    // Ageing organs lose (years - onset)^2 >> SEN_SHIFT HP per day.
    static final int SEN_ONSET  = 55;       // +- 8 by personal constitution
    static final int SEN_SHIFT  = 4;        // >> 4 = divide by 16

    // --- bloodstream ------------------------------------------------------------
    static final int VOL_REGEN      = 120;  // blood rebuilt per day (1.2 %)
    static final int VOL_DEATH      = 5000; // under 50 % volume = death
    static final int PERF_OK        = 5500; // O2 delivery under 55 % hurts organs
    static final int PATH_GROWTH_SH = 4;    // infection grows by load/16 per day
    static final int IMMUNE_CLEAR   = 350;  // infection cleared per day (adult)
    static final int SEPSIS_T       = 3000; // infection above this hurts organs
    static final int WASTE_GEN      = 150;  // toxins made per day
    static final int UREMIA_T       = 4000; // toxins above this hurt organs
    static final int HEAT_T         = 4100; // above 41.00 C the brain cooks

    // --- pain & behaviour -------------------------------------------------------
    static final int PAIN_SHIFT     = 6;    // pain = sum(missing HP * weight) >> 6
    static final int PAIN_AFF_SCALE = 40;   // each affliction's pain value x 40
    static final int PAIN_WORK      = 5000; // 50 % pain+: won't chop
    static final int PAIN_BED       = 8000; // 80 % pain+: bedridden
    static final int BED_PERF       = 6000; // O2 delivery under 60 %: bedridden
    static final int BED_TEMP       = 3950; // 39.5 C+: bedridden
    static final int OLD_AGE_YEARS  = 50;   // unexplained organ failure = "Old age"
    static final int SERIOUS_SEV    = 30;   // dot+bleed+sepsis needed to be blamed

    // --- reproduction --------------------------------------------------------------
    static final int  FERTILE_MIN     = 16;
    static final int  FERTILE_MAX     = 44;
    static final long CONCEIVE_P32    = 4294967296L / 600;  // daily chance
    static final int  GESTATION_DAYS  = 270;
    static final int  POSTPARTUM_DAYS = 730;                // breastfeeding pause

    // --- behaviour -------------------------------------------------------------------
    static final int  CHOP_AGE    = 14;
    static final long CHOP_P32    = 4294967296L / 40;
    static final int  WOOD_TARGET = 3;      // chop only while stock < 3 per person
    static final int  FOLLOW_AGE  = 4;      // toddlers stay with mother
    static final int  CHILD_AGE   = 10;     // under this, KIDS multipliers apply
    static final int  SPREAD_DIST = 2;      // contagion reach in tiles

    // --- trees -------------------------------------------------------------------------
    static final long TREE_INIT_P32 = 1932735283L;          // 45 % start as trees
    static final int  TREE_TICKS    = 32;                   // random tiles per day
    static final long GROW_P32      = 4294967296L / 10;
    static final long SEED_P32      = 4294967296L / 8;
    static final long ROT_P32       = 4294967296L / 8;
    static final long RAIN_P32      = 4294967296L / 4000;   // wind-blown seed

    // tile states
    static final byte T_GRASS = 0, T_SEED = 1, T_SAPLING = 2, T_YOUNG = 3,
                      T_TREE = 4, T_STUMP = 5;

    static final int SEX_F = 0, SEX_M = 1;

    // --- display -------------------------------------------------------------------------
    static final int   FRAME_MS = 50;
    static final int[] SPEEDS   = {1, 7, 30, 365};          // days per frame

    /** years = days * (2^22 / 365) >> 22  : a multiply instead of a divide. */
    static int years(int days) {
        return (days * 11492) >>> 22;
    }
}
