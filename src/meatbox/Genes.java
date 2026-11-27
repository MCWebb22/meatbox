package meatbox;

/**
 * =============================================================================
 *  GENES  -  inherited risk, passed from parents to children.
 * =============================================================================
 *  Two kinds:
 *
 *  1. LIABILITY FAMILIES (polygenic risk). Each person has one byte per family,
 *     128 = population average, spread ~ +-32 (one standard deviation).
 *     A child gets the parents' average plus fresh noise, so risk runs in
 *     families without being destiny. A condition that names a family with
 *     fold f has its rate multiplied by f^((liability-128)/64), clamped to
 *     1/f .. f. So 2 SD above average = x f.  The folds in Catalogue roughly
 *     follow twin-study heritability (schizophrenia ~80%, bipolar ~70%,
 *     depression ~37%) [src~]; the mapping itself is [tune].
 *
 *  2. MENDELIAN GENES: two copies each, one from each parent; X-linked genes
 *     have one copy in males. Starting allele frequencies are from carrier
 *     rates [src~] and can be changed here for other populations.
 */
final class Genes {
    private Genes() {}

    // ---- liability families ---------------------------------------------------------
    static final int G_CARDIO = 0, G_METAB = 1, G_CANCER = 2, G_MOOD = 3, G_PSYCHOSIS = 4,
        G_ANXIETY = 5, G_NEURODEV = 6, G_EXTERNAL = 7, G_AUTOIMM = 8, G_DEMENTIA = 9;
    static final int NFAM = 10;
    static final String[] FAM_NAME = {"heart", "metabolic", "cancer", "mood", "psychosis", "anxiety",
        "neurodevelopmental", "addiction/externalising", "autoimmune", "dementia"};

    // ---- Mendelian genes --------------------------------------------------------------
    static final int REC = 0, DOM = 1, XREC = 2;
    record Mendel(String gene, String condition, int mode, double q, double mutation) {}
    static final Mendel[] MENDEL = {
        //          gene      condition                       mode  allele freq     new mutations/gamete
        new Mendel("HBB-S",  "Sickle cell disease",          REC,  Config.SICKLE_Q_PERMILLE / 1000.0, 0),
        new Mendel("CFTR",   "Cystic fibrosis",              REC,  0.02,     0),        // [src] 1/2500 European births
        new Mendel("PAH",    "Phenylketonuria",              REC,  0.01,     0),        // [src] ~1/10,000
        new Mendel("F8",     "Haemophilia",                  XREC, 0.0002,   0.00007),  // [src] 1/5000 male births; 1/3 new
        new Mendel("DMD",    "Duchenne muscular dystrophy",  XREC, 0.00025,  0.0001),   // [src] 1/3500-5000 boys; 1/3 new
        new Mendel("HTT",    "Huntington's disease",         DOM,  0.00005,  0),        // [src~] 5-10/100k prevalence
        new Mendel("FGFR3",  "Achondroplasia",               DOM,  0.00001,  0.00003),  // [src] 1/25,000; 80% new
        new Mendel("HBB-T",  "Thalassaemia major",           REC,  0.01,     0),        // [src~] regional; 1% generic
        new Mendel("COL1A1", "Brittle bone disease",         DOM,  0.00003,  0.00002),  // [src~] 1/15,000-20,000
        new Mendel("TYR",    "Albinism",                     REC,  0.0077,   0),        // [src~] 1/17,000
    };
    static final int M_SICKLE = 0, M_HD = 5, M_OI = 8;
    static final int NMEND = MENDEL.length;

    static int copies(World w, int p, int g) { return ((w.alleles[p] >>> (2 * g)) & 1) + ((w.alleles[p] >>> (2 * g + 1)) & 1); }

    /** Is the person affected by Mendelian gene g? */
    static boolean affected(World w, int p, int g) {
        int c = copies(w, p, g);
        switch (MENDEL[g].mode()) {
            case DOM: return c >= 1;
            case XREC: return w.sex[p] == Config.SEX_M ? c >= 1 : c == 2;
            default: return c == 2;
        }
    }
    static boolean carrier(World w, int p, int g) { return copies(w, p, g) == 1; }

    /** Founders (and anyone without known parents): draw from population frequencies. */
    static void founder(World w, int p) {
        Rng r = w.rng;
        int base = p * NFAM;
        for (int f = 0; f < NFAM; f++) w.liab[base + f] = (byte) clamp(128 + noise(r, 3));
        int a = 0;
        for (int g = 0; g < NMEND; g++) {
            double q = MENDEL[g].q();
            if (q <= 0) continue;
            long t = (long) (q * 4294967296.0);
            if (r.next32() < t) a |= 1 << (2 * g);
            boolean xMale = MENDEL[g].mode() == XREC && w.sex[p] == Config.SEX_M;
            if (!xMale && r.next32() < t) a |= 1 << (2 * g + 1);
        }
        w.alleles[p] = a;
    }

    /** Child c of mother m and father f (f may be -1 = unknown: drawn from the population). */
    static void inherit(World w, int c, int m, int f) {
        Rng r = w.rng;
        int cb = c * NFAM, mb = m * NFAM;
        for (int k = 0; k < NFAM; k++) {
            int lm = w.liab[mb + k] & 0xFF;
            int lf = f >= 0 ? w.liab[f * NFAM + k] & 0xFF : 128 + noise(r, 3);
            // midparent + half the population variance as new segregation noise
            w.liab[cb + k] = (byte) clamp((lm + lf) / 2 + noise(r, 2));
        }
        int a = 0;
        for (int g = 0; g < NMEND; g++) {
            Mendel mg = MENDEL[g];
            int fromM = pick(r, w.alleles[m], g);
            int fromF;
            if (f >= 0) fromF = mg.mode() == XREC ? ((w.alleles[f] >>> (2 * g)) & 1) : pick(r, w.alleles[f], g);
            else fromF = r.next32() < (long) (mg.q() * 4294967296.0) ? 1 : 0;
            long mu = (long) (mg.mutation() * 4294967296.0);
            if (mu > 0 && r.next32() < mu) fromM = 1;
            if (mu > 0 && r.next32() < mu) fromF = 1;
            boolean male = w.sex[c] == Config.SEX_M;
            if (mg.mode() == XREC && male) fromF = 0;           // sons get their only X from the mother
            a |= fromM << (2 * g);
            a |= fromF << (2 * g + 1);
        }
        w.alleles[c] = a;
    }

    /** At birth: give the conditions this genotype causes (late-onset HD comes via its rate). */
    static void express(World w, Health h, int p) {
        for (int g = 0; g < NMEND; g++) {
            if (g == M_HD || !affected(w, p, g)) continue;
            h.giveAff(p, Afflictions.get(MENDEL[g].condition()).id, Afflictions.SAME, -1);
            if (g == M_OI) w.boneStr[p] = 40;
        }
    }

    /** Risk multiplier for a liability byte: fold^((liab-128)/64), clamped. */
    static double mult(int liab, double fold) {
        double m = Math.pow(fold, (liab - 128) / 64.0);
        return Math.max(1 / fold, Math.min(fold, m));
    }

    private static int pick(Rng r, int alleles, int g) { return (alleles >>> (2 * g + (int) (r.next32() & 1))) & 1; }
    /** Sum of n uniforms, centred: sd ~ 32 for n = 3, ~ 26 for n = 2. */
    private static int noise(Rng r, int n) {
        int s = 0;
        for (int i = 0; i < n; i++) s += r.below(n == 3 ? 65 : 91);
        return s - (n == 3 ? 96 : 90);
    }
    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }
}
