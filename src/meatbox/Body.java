package meatbox;

/**
 * =============================================================================
 *  BODY TABLE  -  organs, muscles and joints (the soft body).  EDIT HERE.
 * =============================================================================
 *  The 206 bones live in Bones.java. This table is everything else that has
 *  its own health (HP 0..10000) and its own job.
 *
 *  regen : HP healed per day (10000 = full). Cartilage and nerve tissue heal
 *          very little, which is why joint wear and cord injury are permanent.
 *  flags : VITAL  0 HP = death
 *          PAIR   vital only when BOTH halves of a pair (this row and the next)
 *                 are gone: one lung or one kidney is enough to live
 *          FEMALE / MALE  only that sex has the part (others start at 0 HP)
 *  o2    : how fast it dies when the blood fails (brain highest)
 *  pain  : pain per missing HP (brain has no pain receptors: 0)
 *  age   : how fast it wears out with age, in 1/16 of the standard rate
 *          (0 = does not age). Vital organs keep 16 so overall lifespan
 *          stays calibrated as in v0.1; joints, lenses and ears wear faster [tune].
 *
 *  A part at 0 HP is lost for good (blind eye, dead kidney).
 *  The part NUMBERS below must match the row order; init() checks.
 */
final class Body {
    private Body() {}

    static final int VITAL = 1, PAIR = 2, FEMALE = 4, MALE = 8;

    // ---- part numbers (row index in PARTS) ----------------------------------
    static final int BRAIN = 0, CORD = 1, NERVES = 2, L_EYE = 3, R_EYE = 4, L_EAR = 5, R_EAR = 6,
        MOUTH = 7, AIRWAY = 8, HEART = 9, VESSELS = 10, L_LUNG = 11, R_LUNG = 12, LIVER = 13,
        STOMACH = 14, SMALL_INT = 15, COLON = 16, PANCREAS = 17, SPLEEN = 18, L_KIDNEY = 19,
        R_KIDNEY = 20, BLADDER = 21, THYROID = 22, ADRENALS = 23, GONADS = 24, UTERUS = 25,
        PROSTATE = 26, BREASTS = 27, SKIN = 28, MARROW = 29,
        L_ARM_M = 30, R_ARM_M = 31, L_LEG_M = 32, R_LEG_M = 33, CORE_M = 34,
        L_SHOULDER = 35, R_SHOULDER = 36, L_ELBOW = 37, R_ELBOW = 38, L_WRIST = 39, R_WRIST = 40,
        L_HIP = 41, R_HIP = 42, L_KNEE = 43, R_KNEE = 44, L_ANKLE = 45, R_ANKLE = 46, SPINE_J = 47;
    static final int NPARTS = 48;

    // ---- legacy capability bits --------------------------------------------------
    // Sim.act() (and the behaviour code that replaces it) reads w.cap[p] as the
    // old 16-bit "usable parts" mask. These keep their v0.1 bit numbers so that
    // code keeps working; Health fills cap from the function scores each day.
    static final int EYES = 2, SPINE = 3, LARM = 10, RARM = 11, LLEG = 12, RLEG = 13;

    record Part(String name, String abbr, int regen, int flags, int o2, int pain, int age) {}

    static final Part[] PARTS = {
        //        name              abbr   regen flags           o2 pain age
        new Part("Brain",           "Brai",   5, VITAL,          96,  0, 16),
        new Part("Spinal cord",     "Cord",   1, 0,              24,  8,  0),
        new Part("Nerves",          "Nerv",   3, 0,              16,  6,  6),
        new Part("L eye",           "LEye",  10, 0,              16,  6, 12),  // lens: cataract, presbyopia
        new Part("R eye",           "REye",  10, 0,              16,  6, 12),
        new Part("L ear",           "LEar",   5, 0,               8,  6, 12),  // presbycusis
        new Part("R ear",           "REar",   5, 0,               8,  6, 12),
        new Part("Mouth & teeth",   "Mout",   2, 0,               4,  8, 12),
        new Part("Airway",          "Airw",  20, VITAL,           8,  6,  0),
        new Part("Heart",           "Hart",   5, VITAL,          64,  6, 16),
        new Part("Blood vessels",   "Vess",   5, 0,              16,  2, 12),  // arteries stiffen
        new Part("L lung",          "LLun",  15, VITAL | PAIR,   40,  4, 16),
        new Part("R lung",          "RLun",  15, VITAL | PAIR,   40,  4, 16),
        new Part("Liver",           "Livr",  30, VITAL,          40,  2, 16),
        new Part("Stomach",         "Stom",  25, 0,              12,  6,  0),
        new Part("Small intestine", "SmIn",  25, VITAL,          24,  6,  0),
        new Part("Colon",           "Colo",  20, 0,              16,  6,  0),
        new Part("Pancreas",        "Panc",  10, 0,              32,  8,  4),
        new Part("Spleen",          "Sple",  20, 0,               8,  4,  0),
        new Part("L kidney",        "LKid",  10, VITAL | PAIR,   64,  6, 16),
        new Part("R kidney",        "RKid",  10, VITAL | PAIR,   64,  6, 16),
        new Part("Bladder",         "Blad",  20, 0,               8,  6,  0),
        new Part("Thyroid",         "Thyr",  10, 0,              16,  2,  0),
        new Part("Adrenals",        "Adre",  10, 0,              32,  2,  0),
        new Part("Gonads",          "Gona",  10, 0,               8,  6,  0),  // fertility handled by age rules
        new Part("Uterus",          "Uter",  20, FEMALE,          8,  0,  0),
        new Part("Prostate",        "Pros",  10, MALE,            8,  0,  0),
        new Part("Breasts",         "Brea",  15, 0,               4,  0,  0),
        new Part("Skin",            "Skin",  40, 0,               4,  8,  4),
        new Part("Bone marrow",     "Marr",  20, 0,              32,  2,  6),
        new Part("L arm muscle",    "LArM",  15, 0,               4,  5,  8),  // sarcopenia
        new Part("R arm muscle",    "RArM",  15, 0,               4,  5,  8),
        new Part("L leg muscle",    "LLgM",  15, 0,               4,  5,  8),
        new Part("R leg muscle",    "RLgM",  15, 0,               4,  5,  8),
        new Part("Core & diaphragm","Core",  15, 0,               8,  5,  6),
        new Part("L shoulder",      "LSho",   1, 0,               2,  5,  6),  // cartilage: osteoarthritis
        new Part("R shoulder",      "RSho",   1, 0,               2,  5,  6),
        new Part("L elbow",         "LElb",   1, 0,               2,  5,  4),
        new Part("R elbow",         "RElb",   1, 0,               2,  5,  4),
        new Part("L wrist & hand",  "LWri",   1, 0,               2,  5,  6),
        new Part("R wrist & hand",  "RWri",   1, 0,               2,  5,  6),
        new Part("L hip",           "LHip",   1, 0,               2,  5,  8),
        new Part("R hip",           "RHip",   1, 0,               2,  5,  8),
        new Part("L knee",          "LKne",   1, 0,               2,  5, 10),
        new Part("R knee",          "RKne",   1, 0,               2,  5, 10),
        new Part("L ankle & foot",  "LAnk",   1, 0,               2,  5,  4),
        new Part("R ankle & foot",  "RAnk",   1, 0,               2,  5,  4),
        new Part("Spinal joints",   "SpJt",   1, 0,               2,  5,  8),  // discs, facets
    };

    /** Where a cancer of "any organ" can start (rough GLOBOCAN shares) [src~]. */
    static final int[] ORGANS = {BRAIN, L_LUNG, R_LUNG, LIVER, STOMACH, COLON, PANCREAS,
                                 L_KIDNEY, BLADDER, SKIN};
    static final int[] LIMB_MUSCLES = {L_ARM_M, R_ARM_M, L_LEG_M, R_LEG_M};
    static final int[] JOINTS = {L_SHOULDER, R_SHOULDER, L_ELBOW, R_ELBOW, L_WRIST, R_WRIST,
                                 L_HIP, R_HIP, L_KNEE, R_KNEE, L_ANKLE, R_ANKLE, SPINE_J};
    static final int[] BIG_JOINTS = {L_HIP, R_HIP, L_KNEE, R_KNEE, L_KNEE, R_KNEE, SPINE_J,
                                     L_WRIST, R_WRIST};   // knees twice: OA is commonest there
    static final int[] LUNGS = {L_LUNG, R_LUNG};
    static final int[] KIDNEYS = {L_KIDNEY, R_KIDNEY};
    static final int[] EYES_P = {L_EYE, R_EYE};
    static final int[] EARS_P = {L_EAR, R_EAR};

    // ---- fast copies, built once by init() ------------------------------------
    static final int[] REGEN  = new int[NPARTS];
    static final int[] AGEW   = new int[NPARTS];  // ageing weight /16
    static final int[] O2X64  = new int[NPARTS];  // o2 << 6, ready for the >>16 trick
    static final int[] PAINW  = new int[NPARTS];
    static long vitalMask, pairMask, femaleOnly, maleOnly;
    static int[] VITALS = {};             // single vital parts (pairs checked separately)

    static void init() {
        if (PARTS.length != NPARTS) throw new IllegalStateException("need exactly " + NPARTS + " parts");
        vitalMask = pairMask = femaleOnly = maleOnly = 0;
        for (int i = 0; i < NPARTS; i++) {
            Part p = PARTS[i];
            REGEN[i] = p.regen();
            AGEW[i]  = p.age();
            O2X64[i] = p.o2() << 6;
            PAINW[i] = p.pain();
            int f = p.flags();
            if ((f & VITAL) != 0 && (f & PAIR) == 0) vitalMask |= 1L << i;
            if ((f & PAIR) != 0) pairMask |= 1L << i;
            if ((f & FEMALE) != 0) femaleOnly |= 1L << i;
            if ((f & MALE) != 0) maleOnly |= 1L << i;
        }
        VITALS = java.util.stream.IntStream.range(0, NPARTS).filter(i -> ((vitalMask >>> i) & 1) != 0).toArray();
        if (!PARTS[SPINE_J].name().equals("Spinal joints") || !PARTS[MARROW].name().equals("Bone marrow"))
            throw new IllegalStateException("part numbers don't match the table order");
        Bones.init();
    }

    static String name(int part) { return PARTS[part].name(); }
}
