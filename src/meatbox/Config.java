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
    static final long CHOP_P32    = 4294967296L / 3;      // [tune] while wood is needed: 1 day in 3
    static final int  FOLLOW_AGE  = 4;      // toddlers stay with mother
    static final int  CHILD_AGE   = 10;     // under this, KIDS multipliers apply
    static final int  SPREAD_DIST = 2;      // contagion reach in tiles

    // --- trees -------------------------------------------------------------------------
    // A tile ticks about 7.6 times a year (32 random tiles a day out of 1536).
    static final long TREE_INIT_P32 = 1932735283L;          // 45 % start as trees
    static final int  TREE_TICKS    = 32;                   // random tiles per day
    static final long GROW_P32      = 4294967296L / 40;     // [tune] ~5 yr per stage: seed->tree ~16 yr
    static final long SEED_P32      = 4294967296L / 8;
    static final long ROT_P32       = 4294967296L / 8;      // stump decides in ~1 yr
    static final int  COPPICE_PCT   = 75;                   // [tune] stumps that resprout (most hardwoods do)
    static final long RAIN_P32      = 4294967296L / 300;    // [tune] long-distance seed: ~2.5 %/yr per open tile

    // --- climate (all temperatures in hundredths of a degree C) --------------------------
    // A cool temperate climate: mean 8 C, coldest around 20 Jan (-5 C), warmest ~21 C.
    static final int CLIM_MEAN      = 800;   // [tune] roughly central Europe / New England
    static final int CLIM_AMP       = 1300;  // [tune]
    static final int CLIM_COLD_DAY  = 20;    // day of year of the coldest mean
    static final int WEATHER_KEEP   = 7;     // weather anomaly keeps 7/8 of itself per day
    static final int WEATHER_KICK   = 400;   // +-4 C daily kick -> anomaly sd ~ 4.8 C  [tune]
    static final int GDD_BASE       = 500;   // [src] plants grow above ~5 C (growing degree days)

    // --- food (kcal) -------------------------------------------------------------------------
    // One tile ~ 0.25 km2 (map ~ 32 x 12 km). Yields are what foragers can harvest per tile
    // per year; tuned so the whole map feeds a few hundred people (~1-1.5 per km2, the
    // rich end of recorded forager densities).
    static final int[] FORAGE_YIELD = {      // [tune] kcal per tile per year, by tile state
        200_000,   // grass: roots, seeds, grazing game
        220_000,   // seed (new growth)
        260_000,   // sapling: berries, browse game (forest edge is richest)
        260_000,   // young trees
        150_000,   // mature forest: nuts, game; little understorey
        220_000,   // stump / clearing
    };
    static final int FORAGE_FLOOR_PCT = 15;  // [tune] share of yield spread over the year (hunting)
    static final int FORAGE_CAP_PCT   = 30;  // [tune] most standing food a tile holds (% of yield)
    static final int FORAGE_DECAY_SH  = 8;   // standing food rots 1/256 per day
    static final int HARVEST_MIN = 75, HARVEST_RNG = 50;  // [tune] yearly luck: 75..125 %
    static final int FAMINE_YEAR_IN   = 12;  // [tune] 1 year in 12 is a bad one...
    static final int FAMINE_PCT       = 55;  // ...at 55 % of normal

    static final int GATHER_AGE    = 8;
    static final int GATHER_ADULT  = 8000;   // [tune] kcal a healthy adult brings in per day
    static final int STORE_DAYS    = 120;    // gather while the store holds less than this
    static final int STORE_SPOIL   = 800;    // stores lose 1/800 per day (dried, smoked) [tune]
    static final int START_STORE_DAYS = 150; // the sim starts on 1 Jan, so give a winter's worth
    static final int RATION_DAYS   = 30;     // [tune] under 30 days left, rations shrink to stretch it
    static final int RATION        = 2400;   // "one person-day" for store bookkeeping

    // daily need, kcal  [src] roughly FAO/WHO 2004 energy requirements, active lifestyle
    static final int NEED_INFANT = 700, NEED_1_3 = 1100, NEED_4_8 = 1500, NEED_9_13 = 2000,
                     NEED_TEEN = 2500, NEED_F = 2300, NEED_M = 2800;
    static final int NEED_PREG   = 300;      // [src] 3rd-trimester-ish average
    static final int NEED_NURSE  = 500;      // [src] lactation ~500 kcal/day
    static final int NEED_COLD_PCT = 2;      // [tune] +2 % need per degree of cold (shivering)
    static final int NEED_THRIFT_PCT = 20;   // [src~] starving bodies burn up to 20 % less
                                             //   (Minnesota Starvation Experiment)
    // Short rations are not shared equally: each person has a random "standing" r in 0..1
    // (kin, status, luck), and gets  clamp(2 * share - r)  of their need. Hard times hit the
    // low-standing first, which leaves enough for the rest to live through it.  [tune]
    static final int ORPHAN_MILK_PCT = 50;   // [tune] motherless infants get half the good of food

    // --- energy reserve ("fat"), 0..10000 = 0..100 % of a healthy reserve ----------------------
    static final int FAT_START   = 8000;
    static final int FAT_NEWBORN = 6000;
    static final int RES_ADULT = 35, RES_CHILD = 20, RES_TODDLER = 12, RES_INFANT = 8;  // [src~] days
        // a healthy adult survives ~45-70 days of no food (reserve, then ~30 days of wasting)
    static final int REFEED_PCT  = 25;       // below a full reserve, eat 25 % extra to rebuild...
    static final int REFEED_EFF  = 70;       // ...at 70 % efficiency
    static final int FAT_IMMUNE  = 5000;     // under 50 %: immune clearing falls (to 40 % at zero)
    static final int FAT_HEAL    = 3000;     // under 30 %: healing slows (to 25 % at zero)
    static final int FAT_FERTILE_HI = 5000, FAT_FERTILE_LO = 2000;  // [src~] famine amenorrhoea
    static final int STORE_FERTILE_PCT = 50; // [tune] store under 50 % of target: fewer conceptions
    static final int RETURN_FERTILE_HI = 95, RETURN_FERTILE_LO = 70;  // [tune] foraging return %:
        // full fertility when gatherers find 95 %+ of what they look for, none at 70 %.
        // Stands in for harder work, thinner mothers and longer breastfeeding [src~ Hadza/!Kung].
    static final int FAT_WASTING = 1000;     // under 10 %: the body eats its own organs
    static final int FAT_BED     = 500;      // under 5 %: too weak to get up
    static final int FAT_MILK    = 2000;     // mother under 20 %: milk dries up in proportion
    static final int FAT_HUNGER_PAIN = 3000; // hunger pain under 30 %

    // --- cold & firewood ---------------------------------------------------------------------
    static final int SHELTER_BONUS = 800;    // [tune] hut + clothing: +8 C over outdoors
    static final int COLD_COMFORT  = 1200;   // [tune] below 12 C (sheltered, clothed) the body cools
    static final int COLD_K_ADULT  = 25;     // [tune] core drops 0.25 C per degree of cold
    static final int HYPO_T        = 3500;   // [src] hypothermia below 35 C
    static final int HYPO_BED_T    = 3200;   // [src] moderate hypothermia: confused, can't work
    static final int HYPO_HEART_T  = 3000;   // [src] below ~30 C the heart goes into arrhythmia
    static final int BURN_COOK     = 3;      // [tune] hundredths of a load per person per day
    static final int BURN_HEAT     = 1;      // [tune] ... plus this per degree C of cold
    static final int CHOP_YIELD    = 12;     // [tune] loads of firewood from one day of chopping
    static final long CLEAR_P32    = 4294967296L / 12;  // [tune] chance a chop day clears the tile
                                             //   -> ~150 loads per tree tile on average
    static final int WOOD_TARGET   = 8;      // chop while stock < 8 loads per person (~half a year)

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
