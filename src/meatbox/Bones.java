package meatbox;

import java.util.ArrayList;
import java.util.List;
import static meatbox.Body.*;

/**
 * =============================================================================
 *  BONES  -  all 206 bones of the adult skeleton.  EDIT HERE.
 * =============================================================================
 *  Every person has an integrity value (0..10000) for every bone, plus one
 *  bone mineral density (BMD, 10000 = young-adult peak) for the whole skeleton.
 *
 *  strength : impact energy the bone survives at full density, in the same
 *             arbitrary units as the injury energies below. Fracture threshold =
 *             strength * (BMD)^2 * personal factor, because bone strength rises
 *             roughly with the square of density (Carter & Hayes 1977) [src].
 *             The absolute numbers are [tune]: they were set so ~1 in 20 falls
 *             breaks a young wrist and ~1 in 12 falls on the hip breaks an
 *             osteoporotic hip.
 *  heal     : days to knit in a healthy adult (orthopaedic textbook ranges, e.g.
 *             phalanx 3-4 wk, radius 6 wk, femur 3-4 mo, tibia 4-5 mo,
 *             scaphoid 3 mo+) [src~]. Children heal about twice as fast.
 *  exp      : how often THIS bone is the one hit when its region takes a blow.
 *  key      : load-bearing bone. A region works only as well as its worst key
 *             bone (a broken femur stops the leg; a broken toe does not).
 *
 *  Bones are touched only when something happens to them (injury, bone
 *  disease) and while they heal, so 206 bones cost almost nothing per day.
 */
final class Bones {
    private Bones() {}

    // ---- regions: groups of bones that share one job ----------------------------
    static final int SKULL = 0, FACE = 1, OSSICLES = 2, NECK = 3, THORACIC = 4, LUMBAR = 5,
        PELVIS = 6, CHEST = 7, L_GIRDLE = 8, R_GIRDLE = 9, L_UPARM = 10, R_UPARM = 11,
        L_FOREARM = 12, R_FOREARM = 13, L_HAND = 14, R_HAND = 15, L_THIGH = 16, R_THIGH = 17,
        L_SHIN = 18, R_SHIN = 19, L_FOOT = 20, R_FOOT = 21;
    static final int NREG = 22;
    static final String[] REG_NAME = {"Skull", "Face", "Ear bones", "Neck", "Upper spine",
        "Lower spine", "Pelvis", "Ribs", "L shoulder", "R shoulder", "L upper arm", "R upper arm",
        "L forearm", "R forearm", "L hand", "R hand", "L thigh", "R thigh", "L shin", "R shin",
        "L foot", "R foot"};

    // ---- injury mechanisms ---------------------------------------------------------
    static final int M_FALL = 0, M_BLOW = 1, M_CRUSH = 2, M_TWIST = 3, M_CUT = 4, NMECH = 5;
    static final String[] MECH_NAME = {"fall", "blow", "crush", "twist", "cut"};

    /**
     * Which region takes the impact, by mechanism. FALL has two rows: under 60 and
     * 60+. Older people fall sideways onto the hip and onto the outstretched
     * wrist; younger people mostly onto the wrist and forearm. Pattern follows
     * Court-Brown & Caesar 2006 fracture epidemiology [src~]; weights [tune].
     *
     *              Sku Fac Oss Nck Tho Lum Pel Chs LGi RGi LUA RUA LFA RFA LHa RHa LTh RTh LSh RSh LFo RFo */
    static final int[][] MECH_W = {
        /*fall<60*/ { 3,  4,  0,  2,  2,  2,  2,  6,  5,  5,  3,  3, 15, 15,  6,  6,  2,  2,  3,  3,  4,  4},
        /*blow   */ {20, 25,  1,  3,  1,  1,  1, 12,  4,  4,  4,  4,  6,  6, 10, 10,  2,  2,  6,  6,  5,  5},
        /*crush  */ { 8,  3,  0,  6, 10, 10, 14, 20,  3,  3,  3,  3,  3,  3,  5,  5,  8,  8,  8,  8,  6,  6},
        /*twist  */ { 0,  0,  0,  0,  0,  1,  0,  0,  0,  0,  0,  0,  1,  1,  1,  1,  1,  1,  5,  5, 10, 10},
        /*cut    */ { 2,  2,  0,  0,  0,  0,  0,  1,  0,  0,  1,  1,  3,  3, 15, 15,  2,  2,  6,  6, 10, 10},
        /*fall60+*/ { 3,  3,  0,  3,  8,  8,  6, 10,  4,  4,  6,  6, 12, 12,  3,  3, 14, 14,  2,  2,  2,  2},
    };
    static final int FALL_OLD_ROW = 5, FALL_OLD_AGE = 60;

    /** The soft part bruised when a region is struck (always, fracture or not). */
    static final int[] REG_SOFT = {BRAIN, MOUTH, L_EAR, CORD, CORE_M, SPINE_J, L_HIP, CORE_M,
        L_SHOULDER, R_SHOULDER, L_ARM_M, R_ARM_M, L_ARM_M, R_ARM_M, L_WRIST, R_WRIST,
        L_LEG_M, R_LEG_M, L_LEG_M, R_LEG_M, L_ANKLE, R_ANKLE};

    record Bone(String name, int region, int strength, int heal, int exp, boolean key, int level) {}

    static final List<Bone> L = new ArrayList<>();
    private static void b(String n, int reg, int str, int heal, int exp, boolean key) {
        L.add(new Bone(n, reg, str, heal, exp, key, 0));
    }
    /** Vertebra: 'level' 1..24 = C1..L5, used for spinal cord injury height. */
    private static void v(String n, int reg, int str, int heal, int exp, int level) {
        L.add(new Bone(n, reg, str, heal, exp, true, level));
    }
    /** A left and a right copy. */
    private static void lr(String n, int regL, int str, int heal, int exp, boolean key) {
        b("L " + n, regL, str, heal, exp, key);
        b("R " + n, regL + 1, str, heal, exp, key);
    }
    private static void hand(int reg, String s) {
        String[] carp = {"scaphoid", "lunate", "triquetrum", "pisiform", "trapezium", "trapezoid", "capitate", "hamate"};
        for (String c : carp) b(s + c, reg, c.equals("scaphoid") ? 35 : 45, c.equals("scaphoid") ? 90 : 42,
                                c.equals("scaphoid") ? 6 : 1, true);
        for (int i = 1; i <= 5; i++) b(s + "metacarpal " + i, reg, i == 5 ? 30 : 35, 35, i == 5 ? 6 : 3, true);
        for (int i = 1; i <= 5; i++) b(s + "proximal phalanx " + i, reg, 20, 28, 2, false);
        for (int i = 2; i <= 5; i++) b(s + "middle phalanx " + i, reg, 20, 28, 1, false);
        for (int i = 1; i <= 5; i++) b(s + "distal phalanx " + i, reg, 18, 24, 2, false);
    }
    private static void foot(int reg, String s) {
        b(s + "calcaneus", reg, 90, 84, 4, true);
        b(s + "talus", reg, 70, 84, 2, true);
        b(s + "navicular", reg, 50, 70, 1, true);
        b(s + "cuboid", reg, 50, 70, 1, true);
        b(s + "medial cuneiform", reg, 45, 56, 1, false);
        b(s + "intermediate cuneiform", reg, 45, 56, 1, false);
        b(s + "lateral cuneiform", reg, 45, 56, 1, false);
        for (int i = 1; i <= 5; i++) b(s + "metatarsal " + i, reg, i == 5 ? 35 : 40, 42, i == 5 ? 5 : 3, i == 1);
        for (int i = 1; i <= 5; i++) b(s + "proximal toe phalanx " + i, reg, 20, 28, i == 1 ? 3 : 1, false);
        for (int i = 2; i <= 5; i++) b(s + "middle toe phalanx " + i, reg, 18, 28, 1, false);
        for (int i = 1; i <= 5; i++) b(s + "distal toe phalanx " + i, reg, 18, 24, i == 1 ? 2 : 1, false);
    }

    static {
        //      name                region     str heal exp key
        // --- cranium (8)
        b("Frontal bone",        SKULL,      90,  56, 3, true);
        lr("parietal bone",      0, 80, 56, 3, true);          // both halves in SKULL (see fix below)
        lr("temporal bone",      0, 60, 56, 2, true);
        b("Occipital bone",      SKULL,      90,  56, 2, true);
        b("Sphenoid bone",       SKULL,      70,  56, 1, true);
        b("Ethmoid bone",        SKULL,      40,  42, 1, false);
        // --- face (14)
        lr("nasal bone",         1, 15, 21, 5, false);
        lr("maxilla",            1, 45, 42, 2, true);
        lr("lacrimal bone",      1, 15, 28, 1, false);
        lr("zygomatic bone",     1, 40, 42, 3, false);
        lr("palatine bone",      1, 40, 42, 1, false);
        lr("inferior nasal concha", 1, 15, 28, 1, false);
        b("Vomer",               FACE,       25,  28, 1, false);
        b("Mandible",            FACE,       60,  42, 6, true);
        // --- middle ear (6)
        lr("malleus",            2, 30, 60, 1, true);
        lr("incus",              2, 30, 60, 1, true);
        lr("stapes",             2, 25, 60, 1, true);
        // --- throat (1)
        b("Hyoid",               NECK,       25,  42, 1, false);
        // --- spine (26): 7 cervical, 12 thoracic, 5 lumbar, sacrum, coccyx
        int[] cExp = {1, 3, 1, 1, 2, 3, 2};
        for (int i = 1; i <= 7; i++)  v("C" + i + " vertebra", NECK, 60, 84, cExp[i - 1], i);
        for (int i = 1; i <= 12; i++) v("T" + i + " vertebra", THORACIC, 65, 84, i >= 11 ? 3 : 1, 7 + i);
        for (int i = 1; i <= 5; i++)  v("L" + i + " vertebra", LUMBAR, 80, 84, i == 1 ? 4 : i == 2 ? 2 : 1, 19 + i);
        b("Sacrum",              PELVIS,     90,  84, 2, true);
        b("Coccyx",              PELVIS,     30,  56, 2, false);
        // --- chest (25): 24 ribs + sternum
        for (int i = 1; i <= 12; i++) lr("rib " + i, 0, 30, 42, (i <= 2 || i >= 11) ? 1 : 3, false);
        b("Sternum",             CHEST,      60,  56, 2, true);
        // --- shoulder girdle (4)
        lr("clavicle",           L_GIRDLE,   45,  42, 6, true);
        lr("scapula",            L_GIRDLE,   70,  42, 1, true);
        // --- arms (6)
        lr("humerus",            L_UPARM,    80,  56, 1, true);
        lr("radius",             L_FOREARM,  55,  42, 7, true);
        lr("ulna",               L_FOREARM,  50,  42, 3, true);
        // --- hands (54)
        hand(L_HAND, "L ");
        hand(R_HAND, "R ");
        // --- pelvis (2)
        lr("hip bone",           0, 110, 84, 4, true);
        // --- legs (8)
        lr("femur",              L_THIGH,   120, 105, 8, true);
        lr("patella",            L_THIGH,    60,  56, 2, false);
        lr("tibia",              L_SHIN,    100, 120, 5, true);
        lr("fibula",             L_SHIN,     45,  42, 5, false);
        // --- feet (52)
        foot(L_FOOT, "L ");
        foot(R_FOOT, "R ");
    }

    // lr() with region 0 means "both copies in one midline region": fix those up.
    static final Bone[] TABLE;
    static {
        Bone[] t = L.toArray(new Bone[0]);
        for (int i = 0; i < t.length; i++) {
            Bone x = t[i];
            String n = x.name();
            int reg = x.region();
            if (n.endsWith("parietal bone") || n.endsWith("temporal bone")) reg = SKULL;
            else if (n.endsWith("nasal bone") || n.endsWith("maxilla") || n.endsWith("lacrimal bone")
                    || n.endsWith("zygomatic bone") || n.endsWith("palatine bone") || n.endsWith("concha")) reg = FACE;
            else if (n.endsWith("malleus") || n.endsWith("incus") || n.endsWith("stapes")) reg = OSSICLES;
            else if (n.contains(" rib ")) reg = CHEST;
            else if (n.endsWith("hip bone")) reg = PELVIS;
            t[i] = new Bone(n, reg, x.strength(), x.heal(), x.exp(), x.key(), x.level());
        }
        TABLE = t;
    }
    static final int NBONES = TABLE.length;

    // ---- fast arrays ------------------------------------------------------------
    static final int[] REGION = new int[NBONES], STRENGTH = new int[NBONES], HEAL_PER_DAY = new int[NBONES];
    static final int[] LEVEL = new int[NBONES];
    static final boolean[] KEY = new boolean[NBONES];
    static int[][] regBones;            // bones of each region
    static int[][] regCum;              // cumulative exposure weights inside a region
    static final int[] FEMUR = new int[2], TIBIA = new int[2], RADIUS = new int[2], HUMERUS = new int[2];

    static void init() {
        if (NBONES != 206) throw new IllegalStateException("skeleton has " + NBONES + " bones, want 206");
        List<List<Integer>> rb = new ArrayList<>();
        for (int r = 0; r < NREG; r++) rb.add(new ArrayList<>());
        for (int i = 0; i < NBONES; i++) {
            Bone x = TABLE[i];
            REGION[i] = x.region();
            STRENGTH[i] = x.strength();
            HEAL_PER_DAY[i] = Math.max(1, 10000 / x.heal());
            KEY[i] = x.key();
            LEVEL[i] = x.level();
            rb.get(x.region()).add(i);
            if (x.name().endsWith("femur")) FEMUR[x.name().charAt(0) == 'L' ? 0 : 1] = i;
            if (x.name().endsWith("tibia")) TIBIA[x.name().charAt(0) == 'L' ? 0 : 1] = i;
            if (x.name().endsWith("radius")) RADIUS[x.name().charAt(0) == 'L' ? 0 : 1] = i;
            if (x.name().endsWith("humerus")) HUMERUS[x.name().charAt(0) == 'L' ? 0 : 1] = i;
        }
        regBones = new int[NREG][];
        regCum = new int[NREG][];
        for (int r = 0; r < NREG; r++) {
            List<Integer> l = rb.get(r);
            if (l.isEmpty()) throw new IllegalStateException("region " + REG_NAME[r] + " has no bones");
            regBones[r] = new int[l.size()];
            regCum[r] = new int[l.size()];
            int acc = 0;
            for (int k = 0; k < l.size(); k++) {
                regBones[r][k] = l.get(k);
                acc += TABLE[l.get(k)].exp();
                regCum[r][k] = acc;
            }
        }
    }

    static String name(int b) { return TABLE[b].name(); }

    /** Pick a bone in region r, weighted by exposure. */
    static int pickBone(Rng rng, int r) {
        int[] cum = regCum[r];
        int x = rng.below(cum[cum.length - 1]);
        for (int k = 0; k < cum.length; k++) if (x < cum[k]) return regBones[r][k];
        return regBones[r][0];
    }
}
