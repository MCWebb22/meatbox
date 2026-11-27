package meatbox;

import java.util.ArrayList;
import java.util.List;
import static meatbox.Body.*;

/**
 * =============================================================================
 *  AFFLICTION TABLE  -  diseases and injuries.  EDIT HERE to add illnesses.
 * =============================================================================
 *  Each entry reads like a sentence:
 *
 *      POLIO = add(new Aff("Polio", DISEASE)   // name, what starts it
 *          .rate(500)            // cases per 100,000 (unit depends on trigger)
 *          .kids(20)             // x20 for under-10s (0 = never in children)
 *          .on(SPINE)            // where it sits
 *          .dot(30)              // damage per day to that part
 *          .lasts(3, 4)          // 3 days + random 0..4
 *          .pain(20)             // pain while active (x40)
 *          .spreads(10)          // 10 / 256 chance per day to infect someone near
 *          .permanent(500, ANY_LEG)  // 500 per 100,000: a leg disabled for good
 *          .effects(IMMUNE));    // lifelong immunity after recovery
 *
 *  Lethality is never a number. It EMERGES from damage vs. the organ's healing,
 *  bleeding vs. blood rebuilding, infection vs. the immune system.
 *  [src] = rate from a cited source (README).  [tune] = game-balance guess.
 *
 *  Order matters: the id of each entry is its position in the list, and the
 *  random rolls walk the list in this order.
 */
final class Afflictions {
    private Afflictions() {}

    // ---- triggers: what can start an affliction ---------------------------------
    static final int NONE    = 0;  // only given by code (e.g. wound infection)
    static final int DISEASE = 1;  // rate per 100,000 people per YEAR
    static final int CHOP    = 2;  // rate per 100,000 chops
    static final int WALK    = 3;  // rate per 100,000 people per YEAR
    static final int BIRTH   = 4;  // rate per 100,000 births (rolled on the mother)
    static final int NEWBORN = 5;  // rate per 100,000 births (rolled on the baby)
    static final int NTRIG   = 6;

    // ---- effect switches (combine with |) -----------------------------------------
    static final int DISABLE   = 1;   // its part can't be used while active
    static final int BEDRIDDEN = 2;   // can't walk or work while active
    static final int CHRONIC   = 4;   // never ends by itself (days count UP)
    static final int GROWS     = 8;   // damage grows with time (cancer)
    static final int IMMUNE    = 16;  // lifelong immunity after recovery

    // ---- special part codes ---------------------------------------------------------
    static final int SAME = 255, ANY_ARM = 254, ANY_LEG = 253, ANY_LIMB = 252, ANY_ORGAN = 251;

    /** One affliction. Built with the chain of setters below; then never changed. */
    static final class Aff {
        final String name;
        final int trigger;
        int id;
        long rate32;          // chance per exposure, scaled to 2^32 (set by add())
        int rateRaw, kids = 1, part = TORSO, hit, dot, durMin = 1, durRng,
            bleed, sepsis, perm16, permPart = SAME, spread, infect, pain, minAge, fx;

        Aff(String name, int trigger) { this.name = name; this.trigger = trigger; }

        // Each setter returns "this" so calls can be chained:  .rate(5).kids(2)...
        Aff rate(int r)          { rateRaw = r;   return this; }
        Aff kids(int k)          { kids = k;      return this; }
        Aff on(int p)            { part = p;      return this; }
        Aff hit(int h)           { hit = h;       return this; }
        Aff dot(int d)           { dot = d;       return this; }
        Aff bleeds(int b)        { bleed = b;     return this; }
        Aff sepsis(int s)        { sepsis = s;    return this; }
        Aff lasts(int min, int rng) { durMin = min; durRng = rng; return this; }
        Aff pain(int p)          { pain = p;      return this; }
        Aff spreads(int s)       { spread = s;    return this; }
        Aff infects(int i)       { infect = i;    return this; }
        Aff minAge(int a)        { minAge = a;    return this; }
        Aff effects(int f)       { fx = f;        return this; }
        /** chance per 100,000 that a part is disabled for good, and which part. */
        Aff permanent(int per100k, int whichPart) {
            perm16 = (int) ((long) per100k * 65536 / 100000);   // stored out of 65536
            permPart = whichPart;
            return this;
        }
        boolean has(int flag)    { return (fx & flag) != 0; }
    }

    static final List<Aff> LIST = new ArrayList<>();

    /** Registers an affliction: gives it the next id and converts its rate. */
    private static Aff add(Aff a) {
        a.id = LIST.size();
        long r = a.rateRaw;
        // per 100k per YEAR -> per day: / (100,000 * 365) = / 36,500,000
        a.rate32 = (a.trigger == DISEASE || a.trigger == WALK)
                 ? (r * 4294967296L) / 36500000L
                 : (r * 4294967296L) / 100000L;
        if (a.rate32 > 0xFFFFFFFFL) throw new IllegalStateException(a.name + ": rate too high");
        LIST.add(a);
        return a;
    }

    // =================================================================================
    //  THE TABLE
    // =================================================================================
    static final Aff OLDAGE    = add(new Aff("Old age", NONE));        // id 0 = no affliction
    static final Aff ORGANFAIL = add(new Aff("Organ failure", NONE));  // label for unexplained deaths

    static final Aff COLD = add(new Aff("Common cold", DISEASE)        // [src] CDC: 2-3/yr adults
        .rate(250000).kids(4).on(LUNGS).dot(15).lasts(5, 5).pain(5).spreads(12));

    static final Aff FLU = add(new Aff("Influenza", DISEASE)           // [src] CDC: ~8% per season
        .rate(8000).kids(2).on(LUNGS).dot(120).sepsis(20).lasts(3, 4).pain(25)
        .spreads(20).effects(BEDRIDDEN));

    static final Aff GASTRO = add(new Aff("Gastroenteritis", DISEASE)  // [src] CDC: 1.27/person/yr
        .rate(127000).kids(2).on(INTEST).dot(20)
        .bleeds(150)                                                   // fluid loss = dehydration
        .lasts(1, 4).pain(20).spreads(8));

    static final Aff PNEUMO = add(new Aff("Pneumonia", DISEASE)        // [src] 649/100k adults/yr;
        .rate(649).kids(15)                                            // kids: WHO 0.29 per child-yr
        .on(LUNGS).dot(250).sepsis(150).lasts(10, 20).pain(30).effects(BEDRIDDEN));

    static final Aff TB = add(new Aff("Tuberculosis", DISEASE)         // [src] WHO: 134/100k/yr
        .rate(134).on(LUNGS).dot(25).lasts(365, 2920).pain(10).spreads(3));

    static final Aff APPEND = add(new Aff("Appendicitis", DISEASE)     // [src] GBD: ~230/100k/yr
        .rate(230).on(INTEST).hit(1000).dot(300)
        .sepsis(250)                                                   // rupture -> peritonitis
        .lasts(3, 10).pain(80).effects(BEDRIDDEN));

    static final Aff STROKE = add(new Aff("Stroke", DISEASE)           // [src] GBD 2021: 151/100k
        .rate(151).kids(0).minAge(30).on(BRAIN).hit(3000).dot(200).lasts(3, 7).pain(5)
        .permanent(40000, ANY_LIMB)                                    // [tune] lasting weakness
        .effects(BEDRIDDEN));

    static final Aff HEART_ATTACK = add(new Aff("Heart attack", DISEASE) // [src] modern ~100/100k
        .rate(100).kids(0).minAge(30).on(HEART).hit(4000).dot(200).lasts(2, 7).pain(70)
        .effects(BEDRIDDEN));

    static final Aff POLIO = add(new Aff("Polio", DISEASE)             // [src] 1 in 200 paralysed
        .rate(500)                                                     // [tune] infection rate
        .kids(20).on(SPINE).dot(30).lasts(3, 4).pain(20).spreads(10)
        .permanent(500, ANY_LEG)                                       // 500/100k = 1 in 200
        .effects(IMMUNE));

    static final Aff MEASLES = add(new Aff("Measles", DISEASE)         // [src] ~all kids by 15
        .rate(1000)                                                    // [tune] adults rarely
        .kids(15).on(LUNGS).dot(150).sepsis(30).lasts(7, 7).pain(20)
        .spreads(40)                                                   // extremely contagious
        .effects(BEDRIDDEN | IMMUNE));

    static final Aff CANCER = add(new Aff("Cancer", DISEASE)           // [src] GLOBOCAN 197/100k,
        .rate(250).kids(0).minAge(40)                                  // concentrated after 40 [tune]
        .on(ANY_ORGAN)
        .dot(10)                                                       // grows: 10 + 10*days/90
        .lasts(90, 0)                                                  // = growth time scale
        .pain(15).effects(CHRONIC | GROWS));

    static final Aff SPRAIN = add(new Aff("Sprained ankle", WALK)      // [tune]
        .rate(215).on(ANY_LEG).hit(3000).lasts(7, 14).pain(30).effects(DISABLE));

    static final Aff BRKARM = add(new Aff("Broken arm", WALK)          // [tune] falls
        .rate(300).on(ANY_ARM).hit(7000).lasts(35, 21).pain(50)
        .effects(DISABLE));                                            // that arm is useless

    static final Aff AXECUT = add(new Aff("Axe cut", CHOP)             // [tune] 1% of chops
        .rate(1000).on(ANY_LIMB).hit(1500).bleeds(600).lasts(2, 3).pain(40).infects(40)
        .effects(DISABLE));

    static final Aff CRUSH = add(new Aff("Crushed by tree", CHOP)      // [tune] logging = deadliest job
        .rate(50).on(TORSO).hit(5000).bleeds(1500).lasts(2, 4).pain(90).infects(30)
        .permanent(5000, SPINE)                                        // 5%: spinal cord -> paraplegia
        .effects(BEDRIDDEN));

    static final Aff LEGBRK = add(new Aff("Broken leg", CHOP)          // [tune]
        .rate(80).on(ANY_LEG).hit(7000).bleeds(300).lasts(40, 30).pain(60).infects(10)
        .effects(DISABLE));

    static final Aff WOUNDINF = add(new Aff("Wound infection", NONE)   // follow-up of injuries
        .dot(50).sepsis(300).lasts(7, 14).pain(30));

    static final Aff HEMOR = add(new Aff("Birth hemorrhage", BIRTH)    // [tune]
        .rate(3000).on(TORSO).bleeds(2000).lasts(1, 2).pain(30).effects(BEDRIDDEN));

    static final Aff PUERP = add(new Aff("Childbed fever", BIRTH)      // [tune]
        .rate(2000).on(TORSO).sepsis(400).lasts(5, 10).pain(40).effects(BEDRIDDEN));

    // ---- newborn (rolled on the baby at birth) ------------------------------------
    // WHO: preterm birth, birth asphyxia and infections lead newborn deaths.
    static final Aff PRETERM = add(new Aff("Premature birth", NEWBORN) // [tune] ~1 in 10 births
        .rate(10000).on(LUNGS)                                         // immature lungs
        .dot(300).lasts(7, 21).effects(BEDRIDDEN));

    static final Aff ASPHYX = add(new Aff("Birth asphyxia", NEWBORN)   // [tune]
        .rate(3000).on(BRAIN).hit(5000).dot(250).lasts(2, 4)
        .permanent(20000, ANY_LIMB)                                    // [tune] cerebral palsy-like
        .effects(BEDRIDDEN));

    static final Aff NEOSEP = add(new Aff("Newborn sepsis", NEWBORN)   // [tune]
        .sepsis(450).rate(3000).lasts(5, 10).pain(30).effects(BEDRIDDEN));

    // ---- shortage deaths: given by code, used as causes of death ----------------------
    // Added at the END so the ids of everything above stay the same.
    static final Aff STARVATION  = add(new Aff("Starvation", NONE));   // organ wasting, Sim.update
    static final Aff HYPOTHERMIA = add(new Aff("Hypothermia", NONE));  // cold heart, Sim.update

    // =================================================================================
    /** The table as an array (fast lookup by id). Built after every add() above ran. */
    static final Aff[] TABLE = LIST.toArray(new Aff[0]);
    static final int COUNT = TABLE.length;

    /**
     * TOTALS[trigger][0 = adult, 1 = child] = all rates of that trigger added up.
     * One random number below this total means "something happens"; above it,
     * nothing does. That is the trick that makes the daily disease roll cheap.
     */
    static final long[][] TOTALS = new long[NTRIG][2];
    static {
        if (COUNT > 64) throw new IllegalStateException("immunity mask holds 64 afflictions max");
        for (int id = 1; id < COUNT; id++) {
            Aff a = TABLE[id];
            TOTALS[a.trigger][0] += a.rate32;
            TOTALS[a.trigger][1] += a.rate32 * a.kids;
        }
    }
}
