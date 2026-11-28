package meatbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * =============================================================================
 *  AFFLICTION ENGINE  -  how a condition is described and pre-computed.
 * =============================================================================
 *  The conditions themselves are in Catalogue.java. Each one is a chain:
 *
 *      add(new Aff("Major depressive disorder", DISEASE)
 *          .rate(3000).ages(12, 120).women(2)        // who gets it, how often
 *          .gene(G_MOOD, 3).risk(CH_STRESS, 6)       // where it comes from
 *          .lasts(90, 180).episodes(365, 1460)       // how it runs
 *          .psy(PSY_MOOD, -6000).psy(PSY_ENERGY, -4000)   // what it does
 *          .src("WHO 2023: ~5% of adults; women ~1.5-2x"));
 *
 *  SOURCES OF RISK (all multiply the base rate; only the ones you name apply)
 *    rate(r)        base cases per 100,000 per YEAR (DISEASE, WALK) or per
 *                   100,000 events (CHOP, BIRTH, NEWBORN, BEREAVE, TRAUMA)
 *    ages(a, b)     only between these ages
 *    doubles(y, at) rate given is AT age 'at'; doubles every y years
 *    peak(a, sd)    bell-shaped onset around age a
 *    kids(k)        x k under 10 (0 = never in children)
 *    women(k)/men(k) x k for that sex.  womenOnly() / menOnly()
 *    gene(family, f) heritable liability: x 1/f .. x f (see Genes)
 *    risk(ch, m)    x 1 .. x m as a person's channel goes 0 .. 10000 (Health.channel)
 *    needs(ch)      zero unless the channel is on (e.g. alcohol must exist)
 *    protect(ch, %) channel cuts the risk to % (sickle trait vs malaria)
 *    after(name, m) x m if the person has that other condition now
 *    requires(name) only if they have that other condition now
 *    requiresImmunity(name) only if they have had it (shingles after chickenpox)
 *
 *  Exact sampling stays cheap: the daily roll compares ONE random number to
 *  the pre-summed "worst case" total for the person's sex and 5-year age band,
 *  and only on a hit computes the real multiplier and accepts it with chance
 *  real / worst-case ("thinning"). So adding conditions barely slows the sim.
 *
 *  COURSE:   lasts(min, rng) acute | CHRONIC | episodes(on.., off..) relapsing |
 *            progress(per year) severity grows | remits(per 100k/yr) | then(..)
 *  EFFECTS:  hit dot bleeds sepsis pain (body), fn(..) (abilities), psy(..)
 *            (mind), blood(..) (glucose, BP, metabolism, haemoglobin), stress,
 *            immuno, bmd, fert, bleedx, leads(..) complications, fatal(..) a
 *            yearly hazard of sudden death while active, caseFatal(..) a chance
 *            per case at onset, both for mechanisms not modelled organ by organ.
 *  SOURCES:  Coverage.java maps every GBD 2021 cause and DSM-5-TR disorder
 *            (the lists that decide what is in the sim) to entries here;
 *            src(..) says where an entry's numbers come from.
 *  All effects scale with the case's severity (0..10000).
 *
 *  Lethality is still never a number: it EMERGES from damage vs. healing,
 *  bleeding vs. blood rebuilding, infection vs. the immune system, glucose
 *  and pressure vs. the organs they wear out. fatal() is the documented
 *  exception (SUDEP, arrhythmia, rupture).
 *
 *  Labels: [src] figure from the named source, [src~] recalled from it and
 *  not re-checked here, [tune] chosen to make the sim behave, [guess] no source.
 */
final class Afflictions {
    private Afflictions() {}

    // ---- triggers: what can start an affliction ---------------------------------
    static final int NONE    = 0;  // only given by code (complications, labels, injuries)
    static final int DISEASE = 1;  // per 100,000 people per YEAR
    static final int CHOP    = 2;  // per 100,000 chops
    static final int WALK    = 3;  // per 100,000 people per YEAR (while walking about)
    static final int BIRTH   = 4;  // per 100,000 births (rolled on the mother)
    static final int NEWBORN = 5;  // per 100,000 births (rolled on the baby)
    static final int BEREAVE = 6;  // per 100,000 bereavements (close relative died)
    static final int TRAUMA  = 7;  // per 100,000 serious injuries / near-death events
    static final int NTRIG   = 8;
    /** Event triggers roll every condition independently (a baby can have two). */
    static boolean isEvent(int t) { return t == BIRTH || t == NEWBORN || t == BEREAVE || t == TRAUMA; }

    // ---- categories (for reports) -----------------------------------------------------
    static final int C_OTHER = 0, C_INFECT = 1, C_INJURY = 2, C_CARDIO = 3, C_RESP = 4, C_METAB = 5,
        C_DIGEST = 6, C_RENAL = 7, C_BLOOD = 8, C_CANCER = 9, C_MSK = 10, C_NEURO = 11, C_SENSE = 12,
        C_SKIN = 13, C_REPRO = 14, C_NUTRI = 15, C_CONGEN = 16, C_MENTAL = 17, C_LABEL = 18;
    static final String[] CAT_NAME = {"Other", "Infectious", "Injury", "Heart & vessels", "Lungs",
        "Endocrine & metabolic", "Digestive", "Kidney & urinary", "Blood", "Cancer", "Bones, joints & muscles",
        "Neurological", "Eyes & ears", "Skin", "Pregnancy & reproductive", "Nutritional", "Congenital & genetic",
        "Mental & behavioural", "Emergent state"};

    // ---- effect switches (combine with |) -----------------------------------------
    static final int DISABLE   = 1;    // its part (or bone region) can't be used while active
    static final int BEDRIDDEN = 2;    // can't walk or work while active
    static final int CHRONIC   = 4;    // never ends by itself (days count UP)
    static final int IMMUNE    = 16;   // lifelong immunity after recovery
    static final int ON_BONE   = 32;   // sites are bone REGIONS; the case sits on one bone
    static final int SUDDEN    = 64;   // death at onset (SIDS, anencephaly)
    static final int END_PREG  = 128;  // ends a pregnancy (miscarriage, ectopic)
    static final int STILLBORN = 256;  // birth trigger: the baby is born dead
    static final int QUIET     = 512;  // don't write to the event log
    static final int FLUID     = 1024; // bleeds() is fluid loss (diarrhoea, burns): no red cells lost
    static final int BOTH      = 2048; // both sides: the mirror part (left/right) takes the same damage

    static final int SAME = 0xFFFF, ANY_LIMB = 0xFFFE, ANY_LEG = 0xFFFD;   // part codes for permanent()

    /** One condition. Built with the chain of setters below; then never changed. */
    static final class Aff {
        final String name;
        final int trigger;
        int id, cat, immIdx = -1;
        String src = "";
        // who / how often
        double rateRaw; int minAge = 0, maxAge = 200;
        double kids = 1, fMul = 1, mMul = 1, dblYears = 0, dblAt = 0, peakAge = -1, peakSd = 0;
        int geneFam = -1; double geneFold = 1;
        final int[] ch = new int[6]; final double[] chMax = new double[6]; final int[] chMode = new int[6]; int nch;
        final String[] afterName = new String[6]; final double[] afterMul = new double[6];
        final int[] afterId = new int[6]; int nafter;
        double cap = 1;                        // worst-case multiplier (for thinning)
        final long[][] rate32 = new long[2][World.NBAND];   // [sex][band], includes cap
        long eventRate32;                      // event triggers: plain chance, no bands
        // where
        int[] sites = {Body.SKIN};
        int[] alsoHit = {};
        // body effects
        int hit, dot, durMin = 1, durRng, bleed, sepsis, pain, spread, fx;
        int perm16, permPart = SAME;
        int sevMin = 10000, sevMax = 10000, progress;   // progress = severity per year
        int epiOn, epiOnRng, epiOff, epiOffRng;
        long remit32, fatal32;
        long act32;   // leads + fatal per day, for the one-draw daily roll in Health.afflictions
        int mech = -1, energy, impacts = 1;   // injury: Bones mechanism
        int deformReg = -1, deformPct;
        // abilities, mind, blood
        final int[] fnSlot = new int[5]; final int[] fnPen = new int[5]; int nfn;
        final int[] psySlot = new int[8]; final int[] psyD = new int[8]; int npsy;
        final int[] blSlot = new int[3]; final int[] blD = new int[3]; int nbl;
        int stressDay, immunoPct, bmdMonth, fertPct = 100, bleedxPct = 100;
        // links
        final String[] leadName = new String[4]; final long[] lead32 = new long[4];
        final int[] leadId = new int[4]; final int[] leadRaw = new int[4]; int nlead;
        final String[] onsetName = new String[3]; final int[] onset16 = new int[3]; final int[] onsetId = new int[3]; int nonset;
        String thenName; int thenId = -1, then16 = 65536;
        String spreadName; int spreadId = -1;
        int fatalRaw, remitRaw;
        long caseFatal32;                      // chance per case of dying at onset

        Aff(String name, int trigger) { this.name = name; this.trigger = trigger; }

        // ---- who / how often
        Aff rate(double r)           { rateRaw = r; return this; }
        Aff kids(double k)           { kids = k; return this; }
        Aff ages(int a, int b)       { minAge = a; maxAge = b; return this; }
        Aff minAge(int a)            { minAge = a; return this; }
        Aff doubles(double y, int at){ dblYears = y; dblAt = at; return this; }
        Aff peak(int a, int sd)      { peakAge = a; peakSd = sd; return this; }
        Aff women(double k)          { fMul = k; return this; }
        Aff men(double k)            { mMul = k; return this; }
        Aff womenOnly()              { mMul = 0; return this; }
        Aff menOnly()                { fMul = 0; return this; }
        Aff gene(int fam, double fold) { geneFam = fam; geneFold = fold; return this; }
        Aff risk(int c, double m)    { return chan(c, m, 0); }
        Aff needs(int c)             { return chan(c, 1, 1); }
        Aff needs(int c, double m)   { return chan(c, m, 1); }
        Aff protect(int c, int pct)  { return chan(c, pct / 100.0, 2); }
        private Aff chan(int c, double m, int mode) { ch[nch] = c; chMax[nch] = m; chMode[nch] = mode; nch++; return this; }
        Aff after(String n, double m){ afterName[nafter] = n; afterMul[nafter] = m; nafter++; return this; }
        Aff requires(String n)       { afterName[nafter] = n; afterMul[nafter] = -1; nafter++; return this; }
        /** only if immune to (= has had) that condition: latent viruses without a slot */
        Aff requiresImmunity(String n) { afterName[nafter] = n; afterMul[nafter] = -2; nafter++; return this; }
        // ---- where
        Aff on(int... parts)         { sites = parts; return this; }
        Aff onBone(int... regions)   { sites = regions; fx |= ON_BONE; return this; }
        Aff alsoHit(int... parts)    { alsoHit = parts; return this; }
        // ---- body
        Aff hit(int h)               { hit = h; return this; }
        Aff dot(int d)               { dot = d; return this; }
        Aff bleeds(int b)            { bleed = b; return this; }
        Aff sepsis(int s)            { sepsis = s; return this; }
        Aff lasts(int min, int rng)  { durMin = min; durRng = rng; return this; }
        Aff pain(int p)              { pain = p; return this; }
        Aff spreads(int s)           { spread = s; return this; }
        Aff spreadsAs(String n)      { spreadName = n; return this; }
        Aff infects(int i)           { return onset("Wound infection", i * 100000 / 256); }
        Aff effects(int f)           { fx |= f; return this; }
        /** bilateral: whichever side the case sits on, the other side is hit just as hard */
        Aff both()                   { fx |= BOTH; return this; }
        Aff chronic()                { fx |= CHRONIC; return this; }
        Aff sev(int a, int b)        { sevMin = a; sevMax = b; return this; }
        Aff progress(int perYear)    { progress = perYear; return this; }
        Aff episodes(int on, int onRng, int off, int offRng) {
            epiOn = on; epiOnRng = onRng; epiOff = off; epiOffRng = offRng; return this;
        }
        Aff remits(int per100kYr)    { remitRaw = per100kYr; return this; }
        Aff fatal(int per100kYr)     { fatalRaw = per100kYr; return this; }
        /** chance per 100,000 CASES of sudden death at onset (fatal() is a yearly hazard while active). */
        Aff caseFatal(int per100k)   { caseFatal32 = (long) per100k * 4294967296L / 100000; return this; }
        Aff injury(int mechanism, int e, int n) { mech = mechanism; energy = e; impacts = n; return this; }
        Aff deform(int region, int pct) { deformReg = region; deformPct = pct; return this; }
        /** chance per 100,000 that a part is disabled for good, and which part. */
        Aff permanent(int per100k, int whichPart) {
            perm16 = (int) ((long) per100k * 65536 / 100000); permPart = whichPart; return this;
        }
        // ---- abilities, mind, blood
        Aff fn(int slot, int pen)    { fnSlot[nfn] = slot; fnPen[nfn] = pen; nfn++; return this; }
        Aff psy(int slot, int d)     { psySlot[npsy] = slot; psyD[npsy] = d; npsy++; return this; }
        Aff blood(int slot, int d)   { blSlot[nbl] = slot; blD[nbl] = d; nbl++; return this; }
        Aff stress(int perDay)       { stressDay = perDay; return this; }
        Aff immuno(int pct)          { immunoPct = pct; return this; }
        Aff bmd(int perMonth)        { bmdMonth = perMonth; return this; }
        Aff fert(int pct)            { fertPct = pct; return this; }
        Aff bleedx(int pct)          { bleedxPct = pct; return this; }
        // ---- links
        Aff leads(String n, int per100kYr) { leadName[nlead] = n; leadRaw[nlead] = per100kYr; nlead++; return this; }
        Aff onset(String n, int per100k) {
            onsetName[nonset] = n; onset16[nonset] = (int) ((long) per100k * 65536 / 100000); nonset++; return this;
        }
        Aff then(String n)           { thenName = n; then16 = 65536; return this; }
        Aff then(String n, int per100k) { thenName = n; then16 = (int) ((long) per100k * 65536 / 100000); return this; }
        Aff src(String s)            { src = s; return this; }

        boolean has(int flag)        { return (fx & flag) != 0; }
        boolean episodic()           { return epiOn > 0; }
    }

    // ---- the registry -------------------------------------------------------------
    static final List<Aff> LIST = new ArrayList<>();
    static int curCat = C_OTHER;
    static void section(int cat) { curCat = cat; }

    static Aff add(Aff a) {
        a.id = LIST.size();
        a.cat = curCat;
        LIST.add(a);
        return a;
    }

    static Aff[] TABLE;
    static int COUNT;
    static final Map<String, Aff> BY_NAME = new HashMap<>();

    /** Worst-case cumulative rates per trigger, sex and age band, for the daily roll. */
    static long[][][][] CUM;     // [trig][sex][band][k]
    static int[][][][] CUM_ID;
    static long[][][] TOTAL;     // [trig][sex][band]
    static int[][] EVENT_IDS;    // [trig] for event triggers
    static int IMM_COUNT;

    static long perYear32(long per100kYr) { return per100kYr * 4294967296L / 36500000L; }

    static double ageMult(Aff a, double age) {
        if (age < a.minAge || age > a.maxAge) return 0;
        double m = 1;
        if (a.dblYears > 0) m *= Math.max(1.0 / 256, Math.min(64, Math.pow(2, (age - a.dblAt) / a.dblYears)));
        if (a.peakAge >= 0) { double z = (age - a.peakAge) / a.peakSd; m *= Math.exp(-0.5 * z * z); }
        if (age < 10) m *= a.kids;
        return m;
    }

    /** Called once after Catalogue has registered everything. */
    static void build() {
        TABLE = LIST.toArray(new Aff[0]);
        COUNT = TABLE.length;
        if (COUNT > 4000) throw new IllegalStateException("too many conditions");
        for (Aff a : TABLE) {
            if (BY_NAME.put(a.name, a) != null) throw new IllegalStateException("duplicate name " + a.name);
        }
        int imm = 0;
        for (Aff a : TABLE) {
            for (int i = 0; i < a.nafter; i++) a.afterId[i] = idOf(a, a.afterName[i]);
            for (int i = 0; i < a.nlead; i++) { a.leadId[i] = idOf(a, a.leadName[i]); a.lead32[i] = perYear32(a.leadRaw[i]); }
            for (int i = 0; i < a.nonset; i++) a.onsetId[i] = idOf(a, a.onsetName[i]);
            if (a.thenName != null) a.thenId = idOf(a, a.thenName);
            a.spreadId = a.spreadName != null ? idOf(a, a.spreadName) : a.id;
            a.remit32 = perYear32(a.remitRaw);
            a.fatal32 = perYear32(a.fatalRaw);
            a.act32 = a.fatal32;
            for (int i = 0; i < a.nlead; i++) a.act32 += a.lead32[i];
            if (a.act32 + a.remit32 >= 1L << 32) throw new IllegalStateException(a.name + ": daily chances add past 1");
            if (a.has(IMMUNE)) {
                if (imm >= 64) throw new IllegalStateException("immunity mask holds 64 conditions");
                a.immIdx = imm++;
            }
            // worst-case multiplier
            double cap = Math.max(1, a.geneFold);
            for (int i = 0; i < a.nch; i++) cap *= a.chMode[i] == 2 ? 1 : Math.max(1, a.chMax[i]);
            for (int i = 0; i < a.nafter; i++) if (a.afterMul[i] > 1) cap *= a.afterMul[i];
            a.cap = cap;
            if (isEvent(a.trigger)) {
                a.eventRate32 = Math.min(0xFFFFFFFFL, (long) (a.rateRaw * cap * 4294967296.0 / 100000));
            } else if (a.trigger != NONE) {
                boolean yearly = a.trigger == DISEASE || a.trigger == WALK;
                for (int sex = 0; sex < 2; sex++) {
                    double sm = sex == Config.SEX_F ? a.fMul : a.mMul;
                    for (int b = 0; b < World.NBAND; b++) {
                        double age = b * 5 + 2.5;
                        if (b == 0) age = 1.5;
                        double r = a.rateRaw * sm * ageMultBand(a, b) * cap;
                        a.rate32[sex][b] = (long) (yearly ? r * 4294967296.0 / 36500000.0 : r * 4294967296.0 / 100000.0);
                    }
                }
            }
        }
        IMM_COUNT = imm;
        // cumulative tables
        CUM = new long[NTRIG][2][World.NBAND][];
        CUM_ID = new int[NTRIG][2][World.NBAND][];
        TOTAL = new long[NTRIG][2][World.NBAND];
        EVENT_IDS = new int[NTRIG][];
        for (int t = 0; t < NTRIG; t++) {
            List<Integer> ev = new ArrayList<>();
            for (Aff a : TABLE) if (a.trigger == t) ev.add(a.id);
            EVENT_IDS[t] = ev.stream().mapToInt(Integer::intValue).toArray();
            if (isEvent(t) || t == NONE) continue;
            for (int sex = 0; sex < 2; sex++) for (int b = 0; b < World.NBAND; b++) {
                List<Long> c = new ArrayList<>(); List<Integer> ids = new ArrayList<>();
                long acc = 0;
                for (Aff a : TABLE) {
                    if (a.trigger != t || a.rate32[sex][b] == 0) continue;
                    acc += a.rate32[sex][b];
                    c.add(acc); ids.add(a.id);
                }
                if (acc >= 4294967296L)
                    throw new IllegalStateException("trigger " + t + " band " + b + ": worst-case daily chance >= 1; lower a cap");
                TOTAL[t][sex][b] = acc;
                CUM[t][sex][b] = c.stream().mapToLong(Long::longValue).toArray();
                CUM_ID[t][sex][b] = ids.stream().mapToInt(Integer::intValue).toArray();
            }
        }
    }

    /** Average of the age multiplier over a 5-year band (sampled yearly). */
    static double ageMultBand(Aff a, int b) {
        double s = 0;
        for (int y = 0; y < 5; y++) s += ageMult(a, b * 5 + y + 0.5);
        return s / 5;
    }

    static int idOf(Aff from, String n) {
        Aff x = BY_NAME.get(n);
        if (x == null) throw new IllegalStateException(from.name + " links to unknown condition '" + n + "'");
        return x.id;
    }

    static Aff get(String n) {
        Aff x = BY_NAME.get(n);
        if (x == null) throw new IllegalStateException("unknown condition '" + n + "'");
        return x;
    }
}
