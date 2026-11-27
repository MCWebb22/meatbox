package meatbox;

/**
 * =============================================================================
 *  BODY TABLE  -  the 16 body parts.  EDIT HERE to change anatomy.
 * =============================================================================
 *  regen : HP healed per day (10000 = full)
 *  flags : VITAL (0 HP = death), AGES (wears out with age)
 *  o2    : how fast it dies when the blood fails (brain highest)
 *  pain  : pain per missing HP (the brain has no pain receptors: 0)
 *
 *  A NON-vital part at 0 HP is lost for good (blind eyes, lost arm).
 *  Spine unusable -> both legs unusable (paralysis).
 *
 *  The part NUMBERS below must match the row order of the table.
 *  check() at start-up stops the program if they don't.
 */
final class Body {
    private Body() {}

    static final int VITAL = 1, AGES = 2;

    // ---- part numbers (row index in PARTS) ----------------------------------
    static final int HEAD = 0, BRAIN = 1, EYES = 2, SPINE = 3, HEART = 4,
                     LUNGS = 5, LIVER = 6, STOMACH = 7, INTEST = 8, KIDNEY = 9,
                     LARM = 10, RARM = 11, LLEG = 12, RLEG = 13, SKIN = 14,
                     TORSO = 15;
    static final int NPARTS = 16;

    /** One row of the table. A Java "record" = a small read-only data class. */
    record Part(String name, int regen, int flags, int o2, int pain) {}

    static final Part[] PARTS = {
        //        name         regen  flags          o2  pain
        new Part("Head",         20,  VITAL,          8,  8),
        new Part("Brain",         5,  VITAL | AGES,  96,  0),
        new Part("Eyes",         10,  AGES,          16,  6),
        new Part("Spine",         3,  0,             24,  8),
        new Part("Heart",         5,  VITAL | AGES,  64,  6),
        new Part("Lungs",        15,  VITAL | AGES,  40,  4),
        new Part("Liver",        30,  VITAL | AGES,  40,  2),
        new Part("Stomach",      25,  0,             12,  6),
        new Part("Intestine",    25,  VITAL,         24,  6),
        new Part("Kidneys",      10,  VITAL | AGES,  64,  6),
        new Part("L arm",        15,  0,              4,  5),
        new Part("R arm",        15,  0,              4,  5),
        new Part("L leg",        15,  0,              4,  5),
        new Part("R leg",        15,  0,              4,  5),
        new Part("Skin",         40,  0,              4,  8),
        new Part("Torso",        20,  VITAL,          8,  6),
    };

    /** Parts a cancer (Afflictions.ANY_ORGAN) can start in. Lungs twice = 2x as likely. */
    static final int[] ORGAN_PICK = {BRAIN, LUNGS, LIVER, STOMACH, INTEST, KIDNEY, SKIN, LUNGS};

    // ---- fast copies, built once by init() ------------------------------------
    // Plain int arrays: the daily loop reads these instead of the Part objects.
    // (The assembly version packed these into SSE vectors.)
    static final int[] REGEN  = new int[NPARTS];
    static final int[] AGEING = new int[NPARTS];  // 1 if the part ages
    static final int[] O2X64  = new int[NPARTS];  // o2 << 6, ready for the >>16 trick
    static final int[] PAINW  = new int[NPARTS];
    static int vitalMask;                          // bit n = part n is vital

    static void init() {
        if (PARTS.length != NPARTS) throw new IllegalStateException("need exactly 16 parts");
        vitalMask = 0;
        for (int i = 0; i < NPARTS; i++) {
            Part p = PARTS[i];
            REGEN[i]  = p.regen();
            AGEING[i] = (p.flags() & AGES) != 0 ? 1 : 0;
            O2X64[i]  = p.o2() << 6;
            PAINW[i]  = p.pain();
            if ((p.flags() & VITAL) != 0) vitalMask |= 1 << i;
        }
        if (!PARTS[SPINE].name().equals("Spine") || !PARTS[TORSO].name().equals("Torso"))
            throw new IllegalStateException("part numbers don't match the table order");
    }

    static String name(int part) { return PARTS[part].name(); }
}
