package meatbox;

import static meatbox.Afflictions.*;
import static meatbox.Body.*;
import static meatbox.Bones.*;
import static meatbox.Health.*;
import static meatbox.Genes.*;
import static meatbox.World.*;

/**
 * =============================================================================
 *  CATALOGUE  -  every condition in the sim.  EDIT HERE to add illnesses.
 * =============================================================================
 *  Grammar: see Afflictions.java.  Order matters only for "after(..)" checks on
 *  event triggers (a baby's conditions are rolled top to bottom), and the id of
 *  each entry is its position. Names are how entries link to each other.
 *
 *  Rates are for a population WITHOUT modern medicine unless noted, because
 *  the sim has none yet. Where only a modern figure exists, it says so.
 *  [src] figure from the named source   [src~] recalled from it, not re-checked
 *  [tune] set to make the sim behave    [guess] no source
 *
 *  Not modelled as illness on purpose: things ICD-11 / DSM-5 do not class as
 *  disorders (e.g. sexual orientation, gender incongruence was moved out of the
 *  mental disorders chapter in ICD-11), normal grief, menopause, ageing itself.
 */
final class Catalogue {
    private Catalogue() {}
    static void init() {}                 // touching the class runs everything below

    static final int LIMBS_R[] = {L_UPARM, R_UPARM, L_FOREARM, R_FOREARM, L_HAND, R_HAND,
                                  L_THIGH, R_THIGH, L_SHIN, R_SHIN, L_FOOT, R_FOOT};

    // =================================================================================
    //  CAUSE LABELS (given by code)
    // =================================================================================
    static final Aff OLDAGE      = add(new Aff("Old age", NONE));          // id 0 = no affliction
    static final Aff ORGANFAIL   = add(new Aff("Organ failure", NONE));
    static final Aff STARVATION  = add(new Aff("Starvation", NONE));       // food thread: organ wasting
    static final Aff HYPOTHERMIA = add(new Aff("Hypothermia", NONE));      // food thread: cold heart
    static final Aff SUICIDE     = add(new Aff("Suicide", NONE).src("emergent from mood, impulsivity, psychosis"));
    static final Aff HEATSTROKE  = add(new Aff("Heat stroke", NONE));
    static final Aff DKA         = add(new Aff("Diabetic coma", NONE).src("emergent: glucose > 30 mmol/L"));

    // =================================================================================
    //  INJURIES  (Bones.injure does the fracture physics; these are the events)
    // =================================================================================
    static { section(C_INJURY); }
    static final Aff FALL = add(new Aff("Fall", WALK)
        .rate(8000).doubles(18, 30).kids(2)                    // [tune] ~30%/yr at 75 (CDC: 1 in 4 over-65s fall each year) [src~]
        .risk(CH_FRAIL, 3).risk(CH_LOWVISION, 2).after("Alcohol use disorder", 2.5)
        .injury(M_FALL, 20, 1).lasts(1, 2).pain(10).effects(QUIET));
    static final Aff TWIST = add(new Aff("Twisted ankle", WALK)       // [tune]
        .rate(215).on(L_ANKLE, R_ANKLE).hit(3000).injury(M_TWIST, 25, 1).lasts(7, 14).pain(30)
        .effects(DISABLE | QUIET));
    static final Aff DISLOC = add(new Aff("Dislocated shoulder", WALK) // [src~] ~24/100k/yr
        .rate(24).men(2.5).on(L_SHOULDER, R_SHOULDER).hit(4000).lasts(14, 21).pain(60).effects(DISABLE));
    static final Aff AXECUT = add(new Aff("Axe cut", CHOP)             // [tune] 1% of chop days
        .rate(1000).on(LIMB_MUSCLES).injury(M_CUT, 15, 1).hit(1500).bleeds(600).lasts(2, 3).pain(40)
        .infects(40).onset("Tetanus", 300).effects(DISABLE));
    static final Aff CRUSH = add(new Aff("Crushed by tree", CHOP)      // [tune] logging = deadliest job (BLS) [src~]
        .rate(20).on(CORE_M).injury(M_CRUSH, 120, 4).hit(2000).bleeds(1200).lasts(2, 4).pain(90).infects(30)
        .effects(BEDRIDDEN));
    static final Aff BRANCH = add(new Aff("Struck by branch", CHOP)    // [tune]
        .rate(80).on(LIMB_MUSCLES).injury(M_BLOW, 60, 1).hit(1000).lasts(3, 5).pain(40).infects(10));
    static final Aff STRAIN = add(new Aff("Muscle strain", CHOP)       // [tune] 2% of chop days
        .rate(2000).on(L_ARM_M, R_ARM_M, CORE_M, CORE_M).hit(1500).lasts(3, 10).pain(25).effects(QUIET));
    static final Aff BURN = add(new Aff("Burn", DISEASE)               // [guess] hearth fires; WHO: 11M severe burns/yr worldwide
        .rate(100).kids(4).on(SKIN).hit(3000).dot(30).bleeds(150).lasts(14, 28).pain(70).infects(60).effects(FLUID)
        .psy(PSY_SOCIAL, -1000).src("WHO burns fact sheet: children and open fires"));
    static final Aff FROSTBITE = add(new Aff("Frostbite", DISEASE)
        .rate(20000).needs(CH_COLD).on(L_WRIST, R_WRIST, L_ANKLE, R_ANKLE).hit(2500).lasts(14, 28).pain(40)
        .permanent(10000, SAME).infects(30).src("[guess] 1 in 10 lose the digits"));
    static final Aff HERNIA = add(new Aff("Inguinal hernia", DISEASE)  // [src] Primatesta 1996: lifetime men 27%, women 3%
        .rate(400).men(1).women(0.1).ages(15, 120).risk(CH_LABOUR, 2).on(CORE_M).chronic().pain(10)
        .fn(FN_MOBILITY, 500).leads("Strangulated hernia", 300));
    static final Aff STRANG = add(new Aff("Strangulated hernia", NONE) // untreated: mostly fatal
        .on(SMALL_INT).dot(300).sepsis(200).lasts(3, 5).pain(85).effects(BEDRIDDEN));
    static final Aff CONCUSSION = add(new Aff("Concussion", NONE)      // given by skull/face blows
        .on(BRAIN).lasts(7, 21).pain(15).psy(PSY_COGNITION, -3000).psy(PSY_ENERGY, -2000)
        .psy(PSY_MOOD, -1500).psy(PSY_ANXIETY, 1000).effects(QUIET));
    static final Aff TBI = add(new Aff("Traumatic brain injury", NONE) // severe skull fracture / bleed
        .on(BRAIN).chronic().sev(3000, 10000).psy(PSY_COGNITION, -4000).psy(PSY_IMPULSE, 3000)
        .psy(PSY_AGGRESSION, 2500).psy(PSY_MOOD, -2000).fn(FN_SPEECH, 1500)
        .src("[src~] TBI raises impulsivity/aggression and epilepsy, depression, dementia risk"));
    static final Aff PARA = add(new Aff("Paraplegia", NONE).chronic().psy(PSY_MOOD, -2000)
        .src("label: spinal cord injury below the neck"));
    static final Aff TETRA = add(new Aff("Tetraplegia", NONE).chronic().psy(PSY_MOOD, -3000)
        .src("label: cervical spinal cord injury"));
    static final Aff WOUNDINF = add(new Aff("Wound infection", NONE)
        .dot(50).sepsis(300).lasts(7, 14).pain(30));
    static final Aff PNEUMOTHORAX_T = add(new Aff("Collapsed lung", NONE)  // from a rib fracture
        .on(L_LUNG, R_LUNG).hit(4000).dot(50).lasts(7, 14).pain(50).effects(BEDRIDDEN));

    // =================================================================================
    //  INFECTIOUS
    // =================================================================================
    static { section(C_INFECT); }
    static final Aff COLD = add(new Aff("Common cold", DISEASE)        // [src] CDC: adults 2-3/yr
        .rate(250000).kids(4).on(AIRWAY).dot(15).lasts(5, 5).pain(5).spreads(12)
        .psy(PSY_ENERGY, -1000).effects(QUIET));
    static final Aff FLU = add(new Aff("Influenza", DISEASE).both()           // [src] CDC: ~8% per season
        .rate(8000).kids(2).on(L_LUNG, R_LUNG).dot(60).sepsis(20).lasts(3, 4).pain(25)
        .spreads(20).psy(PSY_ENERGY, -5000).effects(BEDRIDDEN | QUIET));
    static final Aff GASTRO = add(new Aff("Gastroenteritis", DISEASE)  // [src] CDC: 1.27/person/yr
        .rate(127000).kids(2).risk(CH_WATER, 1.5).on(SMALL_INT).dot(20).bleeds(150)
        .lasts(1, 4).pain(20).spreads(8).effects(QUIET | FLUID));
    static final Aff PNEUMO = add(new Aff("Pneumonia", DISEASE)        // [src] 649/100k adults/yr; kids WHO 0.29/child-yr
        .rate(649).kids(15).risk(CH_IMMOBILE, 4).risk(CH_SMOKE, 1.5)
        .after("Chronic obstructive pulmonary disease", 3).after("Measles", 5)
        .on(L_LUNG, R_LUNG).dot(125).alsoHit(AIRWAY).sepsis(150).lasts(10, 20).pain(30)
        .psy(PSY_ENERGY, -5000).effects(BEDRIDDEN));
    static final Aff LATENT_TB = add(new Aff("Latent tuberculosis", NONE)  // [src] WHO: ~1/4 of humanity infected
        .chronic().leads("Tuberculosis", 100).effects(QUIET).src("[src~] 5-10% lifetime reactivation"));
    static final Aff TB = add(new Aff("Tuberculosis", DISEASE)         // [src] WHO: 134/100k/yr
        .rate(30).risk(CH_IMMUNO, 4).risk(CH_MALNUT, 2).after("HIV infection", 5).on(L_LUNG, R_LUNG).dot(13).lasts(365, 2920).pain(10)
        .spreads(6).spreadsAs("Latent tuberculosis").psy(PSY_ENERGY, -3000).fn(FN_STAMINA, 2000)
        .src("untreated: ~70% dead within 10 yr (Tiemersma 2011)"));
    static final Aff APPEND = add(new Aff("Appendicitis", DISEASE)     // [src] GBD: ~230/100k/yr
        .rate(230).ages(2, 120).on(COLON).hit(1000).dot(300).sepsis(250).lasts(3, 10).pain(80)
        .effects(BEDRIDDEN));
    static final Aff POLIO = add(new Aff("Polio", DISEASE)             // [src] 1 in 200 infections paralyse
        .rate(500).kids(20).on(CORD).dot(30).lasts(3, 4).pain(20).spreads(10)
        .permanent(500, ANY_LEG).effects(IMMUNE));
    static final Aff MEASLES = add(new Aff("Measles", DISEASE).both()         // [src] almost all children before vaccines
        .rate(1000).kids(15).on(L_LUNG, R_LUNG).dot(75).sepsis(30).lasts(7, 7).pain(20)
        .spreads(40).immuno(30).onset("Blinding eye damage", 1000).effects(BEDRIDDEN | IMMUNE));
    static final Aff SMALLPOX = add(new Aff("Smallpox", DISEASE)       // historical; ~30% case fatality [src]
        .rate(20).kids(3).on(SKIN).dot(200).sepsis(250).lasts(14, 14).pain(50).spreads(25)
        .onset("Blinding eye damage", 2000).effects(BEDRIDDEN | IMMUNE).src("[tune] rate; CFR from WHO"));
    static final Aff PERTUSSIS = add(new Aff("Whooping cough", DISEASE) // [tune]; infant deaths emerge via age scaling
        .rate(500).kids(20).on(AIRWAY).dot(20).lasts(40, 40).pain(15).spreads(30).effects(IMMUNE));
    static final Aff DIPHTHERIA = add(new Aff("Diphtheria", DISEASE)   // [src~] CFR 5-10%, higher in infants
        .rate(50).kids(10).on(AIRWAY).dot(300).alsoHit(HEART).lasts(7, 7).pain(40).spreads(15)
        .effects(BEDRIDDEN | IMMUNE));
    static final Aff STREP = add(new Aff("Strep throat", DISEASE)      // [tune]
        .rate(5000).kids(3).on(AIRWAY).dot(10).lasts(5, 5).pain(20).spreads(10)
        .then("Rheumatic fever", 300).effects(QUIET)
        .src("[src] ~0.3-3% of untreated strep throat -> rheumatic fever"));
    static final Aff RHEUM_FEVER = add(new Aff("Rheumatic fever", NONE)
        .on(HEART).dot(30).lasts(60, 60).pain(30).effects(BEDRIDDEN)
        .then("Rheumatic heart disease", 50000));
    static final Aff CHICKENPOX = add(new Aff("Chickenpox", DISEASE)   // [tune]
        .rate(2000).kids(20).on(SKIN).dot(50).lasts(7, 7).pain(10).spreads(35)
        .effects(IMMUNE | QUIET));
    static final Aff SHINGLES = add(new Aff("Shingles", DISEASE)       // [src~] ~3-5/1000/yr, rising with age
        .rate(400).doubles(20, 60).requiresImmunity("Chickenpox").risk(CH_IMMUNO, 3)
        .on(NERVES).dot(5).lasts(14, 14).pain(50).then("Postherpetic neuralgia", 15000));
    static final Aff PHN = add(new Aff("Postherpetic neuralgia", NONE)
        .on(NERVES).chronic().pain(40).remits(30000).psy(PSY_MOOD, -1000));
    static final Aff MUMPS = add(new Aff("Mumps", DISEASE)             // [tune]
        .rate(1000).kids(10).on(MOUTH).dot(20).lasts(7, 7).pain(20).spreads(25)
        .onset("Mumps orchitis", 20000).effects(IMMUNE | QUIET));
    static final Aff ORCHITIS = add(new Aff("Mumps orchitis", NONE)    // [src~] ~20-30% of post-pubertal males
        .menOnly().ages(12, 120).on(GONADS).dot(150).lasts(7, 7).pain(40));
    static final Aff RUBELLA = add(new Aff("Rubella", DISEASE)         // harmless except in pregnancy
        .rate(800).kids(10).on(SKIN).dot(5).lasts(3, 4).spreads(20).effects(IMMUNE | QUIET));
    static final Aff MALARIA = add(new Aff("Malaria", DISEASE)         // needs mosquitoes (Config.MOSQUITO)
        .rate(20000).kids(3).needs(CH_MOSQUITO).protect(CH_SICKLE_TRAIT, 10).protect(CH_THAL_TRAIT, 50).on(SPLEEN).dot(60).alsoHit(BRAIN, L_KIDNEY)
        .sepsis(200).blood(B2_HB, -3000).lasts(7, 14).pain(30).effects(BEDRIDDEN)
        .src("[src] sickle-cell trait gives ~90% protection from severe malaria (Williams 2005)"));
    static final Aff CHOLERA = add(new Aff("Cholera", DISEASE)         // needs contaminated water
        .rate(200).needs(CH_WATER).on(SMALL_INT).bleeds(1200).lasts(2, 4).pain(10).spreads(5)
        .effects(BEDRIDDEN | FLUID).src("[src] untreated severe cholera CFR 25-50%"));
    static final Aff TYPHOID = add(new Aff("Typhoid fever", DISEASE)
        .rate(300).needs(CH_WATER).on(SMALL_INT).dot(50).sepsis(100).lasts(14, 14).pain(30)
        .effects(BEDRIDDEN).src("[src~] untreated CFR 10-20%"));
    static final Aff DYSENTERY = add(new Aff("Dysentery", DISEASE)     // shigella
        .rate(1000).kids(4).needs(CH_WATER).on(COLON).dot(80).bleeds(300).lasts(5, 7).pain(40).spreads(8)
        .blood(B2_HB, -300).effects(FLUID));   // bloody stools: mostly water lost, some red cells
    static final Aff HEPA = add(new Aff("Hepatitis A", DISEASE)
        .rate(500).kids(3).needs(CH_WATER).on(LIVER).dot(60).lasts(14, 28).pain(10).spreads(5)
        .psy(PSY_ENERGY, -4000).effects(IMMUNE));
    static final Aff HEPB = add(new Aff("Hepatitis B", DISEASE)        // [guess] adult infection rate
        .rate(100).ages(12, 120).on(LIVER).dot(40).lasts(30, 60).spreads(2).psy(PSY_ENERGY, -3000)
        .then("Chronic hepatitis B", 5000).effects(IMMUNE).src("[src] ~5% of adult infections become chronic"));
    static final Aff HEPB_C = add(new Aff("Chronic hepatitis B", NEWBORN)
        .rate(90000).needs(CH_M_HEPB).on(LIVER).dot(2).chronic()
        .leads("Cirrhosis", 1000).leads("Liver cancer", 300)
        .src("[src] ~90% of babies of HBeAg+ mothers become chronic carriers without vaccine"));
    static final Aff UTI = add(new Aff("Urinary tract infection", DISEASE)  // [src~] ~10% of women/yr
        .rate(1200).women(8).ages(2, 120).risk(CH_IMMOBILE, 3).risk(CH_PREGNANT, 2).risk(CH_GLU, 2)
        .on(BLADDER).dot(40).lasts(3, 5).pain(20).then("Pyelonephritis", 2000).effects(QUIET));
    static final Aff PYELO = add(new Aff("Pyelonephritis", NONE)
        .on(L_KIDNEY, R_KIDNEY).dot(150).sepsis(150).lasts(7, 7).pain(50).effects(BEDRIDDEN));
    static final Aff CELLULITIS = add(new Aff("Cellulitis", DISEASE)   // [src~] ~200/100k/yr
        .rate(200).doubles(20, 50).risk(CH_GLU, 3).risk(CH_IMMOBILE, 2)
        .on(SKIN).dot(60).sepsis(60).lasts(7, 7).pain(30));
    static final Aff BEDSORE = add(new Aff("Pressure ulcer", DISEASE)  // [src~] 10-20% of long-term bedridden
        .rate(30000).needs(CH_IMMOBILE).ages(50, 120).on(SKIN).dot(30).sepsis(50).lasts(30, 60).pain(30).infects(60));
    static final Aff OSTEOMYELITIS = add(new Aff("Osteomyelitis", DISEASE)  // bone infection; also after open fractures
        .rate(10).kids(5).onBone(L_THIGH, R_THIGH, L_SHIN, R_SHIN).dot(60).sepsis(80).lasts(42, 60).pain(40)
        .then("Chronic osteomyelitis", 20000).effects(BEDRIDDEN));
    static final Aff OSTEOMYEL_C = add(new Aff("Chronic osteomyelitis", NONE)
        .onBone(L_THIGH, R_THIGH, L_SHIN, R_SHIN).dot(3).sepsis(10).chronic().remits(5000).pain(20));
    static final Aff TETANUS = add(new Aff("Tetanus", NONE)            // from wounds; [src] untreated CFR ~50%
        .on(CORE_M).dot(250).alsoHit(AIRWAY).lasts(14, 14).pain(80).effects(BEDRIDDEN));
    static final Aff NEO_TETANUS = add(new Aff("Neonatal tetanus", NEWBORN)  // [guess] unclean cord cutting
        .rate(500).on(CORE_M).dot(600).lasts(7, 7).pain(60).effects(BEDRIDDEN));
    static final Aff RABIES = add(new Aff("Rabies", DISEASE)           // needs biting animals; ~100% fatal [src]
        .rate(5).needs(CH_ANIMALS).on(BRAIN).dot(1000).lasts(7, 7).psy(PSY_REALITY, 6000)
        .psy(PSY_AGGRESSION, 5000).psy(PSY_ANXIETY, 6000));
    static final Aff PLAGUE = add(new Aff("Plague", DISEASE)           // needs rats/fleas; untreated ~50-60% [src~]
        .rate(50).needs(CH_RODENTS).on(SPLEEN).dot(100).sepsis(600).lasts(5, 5).pain(60).spreads(20)
        .effects(BEDRIDDEN));
    static final Aff LEPROSY = add(new Aff("Leprosy", DISEASE)         // [src] WHO ~2.5/100k new cases/yr
        .rate(3).spreads(1).on(NERVES).dot(3).chronic().progress(500).sev(1000, 3000)
        .fn(FN_MANIP, 3000).psy(PSY_SOCIAL, -2000).psy(PSY_MOOD, -2000));
    static final Aff SYPHILIS = add(new Aff("Syphilis", DISEASE)       // [guess] rate; no sexual network yet
        .rate(30).needs(CH_SEXACTIVE).on(SKIN).dot(5).lasts(30, 60).spreads(2)
        .then("Latent syphilis").effects(QUIET));
    static final Aff SYPH_L = add(new Aff("Latent syphilis", NONE).chronic().remits(5000)
        .leads("Neurosyphilis", 300).effects(QUIET).src("[src~] ~1/3 of untreated reach tertiary stage"));
    static final Aff NEUROSYPH = add(new Aff("Neurosyphilis", NONE)    // general paresis
        .on(BRAIN).dot(8).chronic().psy(PSY_COGNITION, -5000).psy(PSY_REALITY, 4000).psy(PSY_IMPULSE, 2000));
    static final Aff CONG_SYPH = add(new Aff("Congenital syphilis", NEWBORN)
        .rate(40000).needs(CH_M_SYPHILIS).on(LIVER).dot(80).sepsis(150).lasts(30, 60)
        .src("[src~] up to 40% of untreated maternal syphilis ends in fetal/infant death"));
    static final Aff GONORRHEA = add(new Aff("Gonorrhoea", DISEASE)    // [guess] rate
        .rate(200).needs(CH_SEXACTIVE).on(BLADDER).dot(5).lasts(30, 60).pain(15).spreads(3)
        .onset("Pelvic inflammatory disease", 15000).effects(QUIET));
    static final Aff PID = add(new Aff("Pelvic inflammatory disease", NONE)
        .womenOnly().on(UTERUS).dot(60).sepsis(60).lasts(14, 14).pain(40)
        .src("[src~] ~10-20% of PID episodes cause tubal infertility"));
    static final Aff MENINGITIS = add(new Aff("Meningitis", DISEASE)   // [src~] 1-10/100k, untreated CFR ~70%
        .rate(10).kids(10).on(BRAIN).dot(400).sepsis(300).lasts(5, 10).pain(70)
        .onset("Deafness from infection", 10000).effects(BEDRIDDEN));
    static final Aff DEAF_INF = add(new Aff("Deafness from infection", NONE)
        .on(L_EAR).hit(8000).alsoHit(R_EAR).lasts(1, 0).effects(QUIET));
    static final Aff WORMS = add(new Aff("Roundworm infection", DISEASE)  // ascariasis; [src] WHO: ~800M infected
        .rate(2000).kids(3).risk(CH_WATER, 3).on(SMALL_INT).chronic().remits(50000)
        .psy(PSY_ENERGY, -500).psy(PSY_COGNITION, -500).leads("Bowel obstruction", 100).effects(QUIET)
        .src("[src~] heavy loads stunt children and can block the gut"));
    static final Aff SCABIES = add(new Aff("Scabies", DISEASE)
        .rate(2000).spreads(15).on(SKIN).lasts(30, 60).pain(15).psy(PSY_ENERGY, -500).effects(QUIET));
    static final Aff FUNGAL = add(new Aff("Fungal skin infection", DISEASE)
        .rate(5000).spreads(5).on(SKIN).lasts(14, 28).pain(3).effects(QUIET));
    static final Aff TRACHOMA = add(new Aff("Trachoma", DISEASE).both()       // [src] WHO: leading infectious cause of blindness
        .rate(500).kids(3).needs(CH_WATER, 2).on(L_EYE, R_EYE).dot(10).lasts(30, 30).spreads(8)
        .then("Blinding eye damage", 2000));
    static final Aff BLIND_DMG = add(new Aff("Blinding eye damage", NONE)
        .on(L_EYE, R_EYE).hit(6000).dot(20).lasts(30, 60));
    static final Aff CONJ = add(new Aff("Conjunctivitis", DISEASE).both()
        .rate(2000).spreads(10).on(L_EYE, R_EYE).dot(5).lasts(3, 7).pain(10).effects(QUIET));
    static final Aff OTITIS = add(new Aff("Ear infection", DISEASE)    // otitis media
        .rate(2000).kids(20).on(L_EAR, R_EAR).dot(60).lasts(3, 7).pain(40).then("Chronic ear infection", 3000)
        .effects(QUIET));
    static final Aff OTITIS_C = add(new Aff("Chronic ear infection", NONE)
        .on(L_EAR, R_EAR).dot(3).chronic().remits(10000));
    static final Aff CARIES = add(new Aff("Tooth decay", DISEASE)      // [src] GBD: most common condition on Earth
        .rate(8000).ages(3, 120).on(MOUTH).dot(2).lasts(365, 730).pain(5).leads("Tooth abscess", 3000).effects(QUIET));
    static final Aff ABSCESS = add(new Aff("Tooth abscess", NONE)
        .on(MOUTH).hit(1000).dot(100).sepsis(60).lasts(7, 14).pain(60));

    // =================================================================================
    //  HEART & VESSELS
    // =================================================================================
    static { section(C_CARDIO); }
    static final Aff HYPERTENSION = add(new Aff("High blood pressure", DISEASE)
        .rate(1500).doubles(15, 50).ages(18, 120).men(1.2).gene(G_CARDIO, 3).needs(CH_SALT, 2)
        .risk(CH_STRESS, 1.5).after("Chronic kidney disease", 3).on(VESSELS).chronic().sev(3000, 10000)
        .blood(B2_BP, 40).leads("Hypertensive heart disease", 1500).effects(QUIET)
        .src("[src~] modern incidence; rare in low-salt societies (INTERSALT 1988) - see Config.SALT"));
    static final Aff MI = add(new Aff("Heart attack", DISEASE)         // [src~] 300-600/100k/yr at 65+
        .rate(60).doubles(9, 60).ages(25, 120).men(2).gene(G_CARDIO, 3)
        .risk(CH_ATHERO, 8).risk(CH_BP, 3).risk(CH_GLU, 2).risk(CH_STRESS, 1.5).risk(CH_TOBACCO, 3)
        .on(HEART).hit(4000).dot(200).lasts(2, 7).pain(70).caseFatal(25000).effects(BEDRIDDEN)
        .then("Heart failure", 20000)
        .src("[src~] ~25-30% die suddenly, mostly before reaching care (WHO MONICA); the rest from heart damage"));
    static final Aff AF = add(new Aff("Atrial fibrillation", DISEASE)  // [src~] ~1%/yr at 75
        .rate(150).doubles(10, 70).ages(35, 120).men(1.5).risk(CH_BP, 2)
        .after("Rheumatic heart disease", 5).after("Overactive thyroid", 3)
        .on(HEART).dot(1).chronic().psy(PSY_ENERGY, -1000).fn(FN_STAMINA, 1500));
    static final Aff STROKE = add(new Aff("Ischaemic stroke", DISEASE)  // [src~] GBD 2021: ~90/100k all strokes ~150
        .rate(40).doubles(8, 65).ages(20, 120).gene(G_CARDIO, 2).risk(CH_BP, 6).risk(CH_ATHERO, 3)
        .risk(CH_GLU, 2).after("Atrial fibrillation", 5).after("Sickle cell disease", 20)
        .on(BRAIN).hit(3000).dot(200).lasts(3, 7).pain(5)
        .permanent(40000, ANY_LIMB).fn(FN_SPEECH, 4000).effects(BEDRIDDEN)
        .then("Major depressive disorder", 30000).src("[src] ~1/3 develop post-stroke depression (Hackett 2014)"));
    static final Aff HF = add(new Aff("Heart failure", DISEASE)        // also given when the heart is < 40%
        .rate(20).doubles(8, 70).ages(30, 120).risk(CH_BP, 3).after("Rheumatic heart disease", 5)
        .on(HEART).dot(2).chronic().fn(FN_STAMINA, 5000).psy(PSY_ENERGY, -3000).psy(PSY_MOOD, -1000));
    static final Aff DVT = add(new Aff("Deep vein thrombosis", DISEASE) // [src~] VTE 1-2/1000/yr
        .rate(60).doubles(15, 60).risk(CH_IMMOBILE, 10).risk(CH_PREGNANT, 4).risk(CH_POSTPARTUM, 10)
        .after("Lung cancer", 4).on(L_LEG_M, R_LEG_M).dot(20).lasts(14, 28).pain(30)
        .fn(FN_MOBILITY, 2000).leads("Pulmonary embolism", 300000));
    static final Aff PE = add(new Aff("Pulmonary embolism", NONE)
        .on(L_LUNG, R_LUNG).hit(4000).alsoHit(HEART).lasts(3, 7).pain(50).caseFatal(20000).effects(BEDRIDDEN)
        .src("[src~] untreated PE mortality ~25-30% (Barritt & Jordan 1960); most deaths within hours"));
    static final Aff RHD = add(new Aff("Rheumatic heart disease", NONE)
        .on(HEART).dot(3).chronic().progress(300).sev(2000, 6000).fn(FN_STAMINA, 3000));
    static final Aff CHD_CONG = add(new Aff("Congenital heart defect", NEWBORN)  // [src] ~8 per 1000 births
        .rate(800).after("Down syndrome", 40).risk(CH_M_RUBELLA, 10)
        .on(HEART).hit(3000).dot(3).chronic().sev(1000, 10000).fn(FN_STAMINA, 4000));
    static final Aff CARDIOMYO = add(new Aff("Cardiomyopathy", DISEASE)
        .rate(8).gene(G_CARDIO, 4).on(HEART).dot(3).chronic().fatal(1500).fn(FN_STAMINA, 3000));
    static final Aff AAA = add(new Aff("Aortic aneurysm", DISEASE)     // [src~] men 4-6x
        .rate(40).doubles(7, 70).ages(50, 120).men(4).risk(CH_BP, 2).risk(CH_TOBACCO, 3)
        .on(VESSELS).chronic().fatal(1500).effects(QUIET));
    static final Aff VARICOSE = add(new Aff("Varicose veins", DISEASE)
        .rate(500).women(2).ages(20, 120).on(VESSELS).chronic().pain(3).effects(QUIET));

    // =================================================================================
    //  LUNGS
    // =================================================================================
    static { section(C_RESP); }
    static final Aff ASTHMA = add(new Aff("Asthma", DISEASE)           // [src~] childhood incidence ~1%/yr
        .rate(300).kids(4).gene(G_AUTOIMM, 2).risk(CH_SMOKE, 2).on(AIRWAY).chronic().remits(3000)
        .leads("Asthma attack", 50000).fn(FN_STAMINA, 1000));
    static final Aff ASTHMA_A = add(new Aff("Asthma attack", NONE)
        .on(AIRWAY).dot(200).alsoHit(L_LUNG, R_LUNG).lasts(1, 2).pain(20).psy(PSY_ANXIETY, 4000).effects(BEDRIDDEN | QUIET));
    static final Aff COPD = add(new Aff("Chronic obstructive pulmonary disease", DISEASE).both()
        .rate(100).doubles(10, 60).ages(35, 120).needs(CH_SMOKE, 2).risk(CH_TOBACCO, 8)
        .on(L_LUNG, R_LUNG).dot(3).chronic().progress(500).sev(2000, 5000)
        .fn(FN_STAMINA, 5000).psy(PSY_ENERGY, -2000)
        .src("[src] WHO: household biomass smoke is a leading COPD cause in women in LMICs"));
    static final Aff PTX = add(new Aff("Spontaneous collapsed lung", DISEASE)  // [src~] 7-18/100k men
        .rate(4).men(4).ages(15, 40).on(L_LUNG, R_LUNG).hit(4000).lasts(7, 14).pain(40));
    static final Aff APNEA = add(new Aff("Sleep apnoea", DISEASE)
        .rate(200).men(2).ages(30, 120).risk(CH_OBESE, 4).on(AIRWAY).chronic()
        .psy(PSY_ENERGY, -2000).psy(PSY_COGNITION, -1000).blood(B2_BP, 8).effects(QUIET));

    // =================================================================================
    //  ENDOCRINE & METABOLIC
    // =================================================================================
    static { section(C_METAB); }
    static final Aff T1D = add(new Aff("Type 1 diabetes", DISEASE)     // [src~] 15-30/100k children
        .rate(20).peak(12, 7).ages(0, 50).gene(G_AUTOIMM, 4).on(PANCREAS).chronic().progress(5000).sev(3000, 6000)
        .blood(B2_GLU, 2800).psy(PSY_ENERGY, -2000)
        .src("[src] before insulin (1922) most died within 1-2 years of diagnosis"));
    static final Aff T2D = add(new Aff("Type 2 diabetes", DISEASE)     // [src~] modern 5-10/1000/yr in adults
        .rate(30).doubles(12, 55).ages(20, 120).gene(G_METAB, 4).risk(CH_OBESE, 25).risk(CH_STRESS, 1.3)
        .after("Gestational diabetes", 5).on(PANCREAS).chronic().progress(300).sev(2000, 6000)
        .blood(B2_GLU, 900).immuno(15)
        .src("[src~] rare but present in lean people (lean T2D); obesity multiplies it up to ~25x (Config.OBESITY)"));
    static final Aff GDM = add(new Aff("Gestational diabetes", DISEASE) // [src~] ~7% of pregnancies
        .rate(10000).needs(CH_LATEPREG).gene(G_METAB, 2).on(PANCREAS).lasts(90, 30).blood(B2_GLU, 400).effects(QUIET));
    static final Aff HYPOTHY = add(new Aff("Underactive thyroid", DISEASE)  // [src~] ~3.5/1000/yr women
        .rate(80).women(5).doubles(20, 50).gene(G_AUTOIMM, 3).risk(CH_IODINE_DEF, 5)
        .on(THYROID).chronic().blood(B2_META, -3000).psy(PSY_MOOD, -1500).psy(PSY_ENERGY, -1000)
        .psy(PSY_COGNITION, -1000).fert(60));
    static final Aff HYPERTHY = add(new Aff("Overactive thyroid", DISEASE)  // Graves
        .rate(25).women(5).ages(15, 80).gene(G_AUTOIMM, 3).on(THYROID).chronic().remits(10000)
        .blood(B2_META, 3500).psy(PSY_ANXIETY, 2500).alsoHit(HEART).bmd(15).fert(70));
    static final Aff GOITRE = add(new Aff("Goitre", DISEASE)           // iodine deficiency
        .rate(1000).women(2).needs(CH_IODINE_DEF).on(THYROID).chronic().remits(5000).blood(B2_META, -800).effects(QUIET)
        .src("[src] WHO: iodine deficiency is the leading preventable cause of brain damage"));
    static final Aff ADDISON = add(new Aff("Adrenal failure", DISEASE) // [src] ~0.5/100k/yr
        .rate(1).gene(G_AUTOIMM, 3).on(ADRENALS).dot(15).chronic().psy(PSY_ENERGY, -3000));
    static final Aff GOUT = add(new Aff("Gout", DISEASE)               // [src~] 1-3/1000/yr
        .rate(120).men(4).doubles(15, 50).ages(25, 120).gene(G_METAB, 2)
        .after("Alcohol use disorder", 2).after("Chronic kidney disease", 3)
        .on(L_ANKLE, R_ANKLE).chronic().leads("Gout attack", 50000).effects(QUIET));
    static final Aff GOUT_A = add(new Aff("Gout attack", NONE)
        .on(L_ANKLE, R_ANKLE).hit(800).lasts(5, 7).pain(70).effects(DISABLE));
    static final Aff PCOS = add(new Aff("Polycystic ovary syndrome", DISEASE)
        .rate(300).womenOnly().ages(13, 45).gene(G_METAB, 2).on(GONADS).chronic().fert(40)
        .psy(PSY_MOOD, -500).effects(QUIET));

    // =================================================================================
    //  DIGESTIVE
    // =================================================================================
    static { section(C_DIGEST); }
    static final Aff ULCER = add(new Aff("Stomach ulcer", DISEASE)     // [src~] ~0.1-0.2%/yr (H. pylori)
        .rate(120).ages(15, 120).risk(CH_STRESS, 1.5).after("Alcohol use disorder", 2)
        .on(STOMACH).dot(5).chronic().remits(30000).pain(25)
        .leads("Bleeding ulcer", 2000).leads("Perforated ulcer", 500));
    static final Aff ULCER_B = add(new Aff("Bleeding ulcer", NONE).on(STOMACH).bleeds(800).lasts(2, 3).pain(30));
    static final Aff ULCER_P = add(new Aff("Perforated ulcer", NONE)
        .on(STOMACH).hit(3000).sepsis(400).lasts(3, 5).pain(90).effects(BEDRIDDEN));
    static final Aff GALLSTONES = add(new Aff("Gallstones", DISEASE)
        .rate(250).women(2.5).doubles(20, 50).ages(15, 120).on(GALLBLADDER).chronic()
        .leads("Biliary colic", 30000).leads("Gallbladder infection", 1000).leads("Acute pancreatitis", 200).effects(QUIET));
    static final Aff COLIC = add(new Aff("Biliary colic", NONE).on(GALLBLADDER).lasts(1, 1).pain(70).effects(QUIET));
    static final Aff CHOLECYST = add(new Aff("Gallbladder infection", NONE)
        .on(GALLBLADDER).dot(50).alsoHit(LIVER).sepsis(150).lasts(5, 7).pain(70).effects(BEDRIDDEN));
    static final Aff PANCREATITIS = add(new Aff("Acute pancreatitis", DISEASE)  // [src~] ~30/100k/yr
        .rate(10).ages(15, 120).after("Alcohol use disorder", 10).on(PANCREAS).hit(3000).dot(100).sepsis(150)
        .lasts(5, 10).pain(90).then("Chronic pancreatitis", 10000).effects(BEDRIDDEN));
    static final Aff PANC_C = add(new Aff("Chronic pancreatitis", NONE)
        .on(PANCREAS).dot(3).chronic().pain(30).blood(B2_GLU, 300));
    static final Aff CIRRHOSIS = add(new Aff("Cirrhosis", DISEASE)
        .rate(5).ages(25, 120).after("Alcohol use disorder", 30).after("Chronic hepatitis B", 20)
        .on(LIVER).dot(6).chronic().progress(800).sev(2000, 5000).bleedx(150)
        .psy(PSY_COGNITION, -2000).psy(PSY_ENERGY, -2000).leads("Liver cancer", 1500));
    static final Aff IBD = add(new Aff("Crohn's disease", DISEASE)   // [src~] ~5-10/100k/yr in the West
        .rate(7).peak(25, 12).gene(G_AUTOIMM, 3).on(SMALL_INT).alsoHit(COLON).chronic().episodes(30, 60, 180, 540)
        .dot(30).bleeds(50).pain(40).psy(PSY_ENERGY, -3000).psy(PSY_MOOD, -1500).bmd(10));
    static final Aff COELIAC = add(new Aff("Coeliac disease", DISEASE) // [src~] ~15-20/100k/yr (needs wheat)
        .rate(15).gene(G_AUTOIMM, 3).on(SMALL_INT).dot(2).chronic().blood(B2_HB, -1000)
        .psy(PSY_ENERGY, -1500).bmd(10).effects(QUIET));
    static final Aff IBS = add(new Aff("Irritable bowel syndrome", DISEASE)
        .rate(800).women(1.5).ages(15, 70).gene(G_ANXIETY, 2).risk(CH_STRESS, 3)
        .on(COLON).chronic().remits(10000).pain(15).psy(PSY_ANXIETY, 1000).effects(QUIET));
    static final Aff OBSTRUCT = add(new Aff("Bowel obstruction", DISEASE)  // untreated: mostly fatal
        .rate(30).doubles(15, 50).on(SMALL_INT).dot(200).sepsis(200).lasts(3, 5).pain(80).effects(BEDRIDDEN));
    static final Aff DIVERTIC = add(new Aff("Diverticulitis", DISEASE)
        .rate(50).doubles(10, 60).ages(30, 120).on(COLON).dot(50).sepsis(100).lasts(5, 10).pain(50));
    static final Aff HEMORR = add(new Aff("Haemorrhoids", DISEASE)
        .rate(1000).ages(18, 120).risk(CH_PREGNANT, 3).on(COLON).bleeds(5).lasts(7, 21).pain(15).effects(QUIET));

    // =================================================================================
    //  KIDNEY & URINARY
    // =================================================================================
    static { section(C_RENAL); }
    static final Aff STONE = add(new Aff("Kidney stone", DISEASE)      // [src~] 1-2/1000/yr
        .rate(150).men(2).ages(18, 120).on(L_KIDNEY, R_KIDNEY).dot(20).lasts(2, 7).pain(90)
        .then("Kidney stone disease", 50000).effects(BEDRIDDEN));
    static final Aff STONE_C = add(new Aff("Kidney stone disease", NONE)
        .on(L_KIDNEY, R_KIDNEY).chronic().leads("Kidney stone", 20000).effects(QUIET));
    static final Aff CKD = add(new Aff("Chronic kidney disease", NONE) // label: given when kidneys < 50%
        .chronic().blood(B2_BP, 15).blood(B2_HB, -1500).psy(PSY_ENERGY, -2000));
    static final Aff PSGN = add(new Aff("Post-strep kidney inflammation", NONE)
        .on(L_KIDNEY).dot(40).alsoHit(R_KIDNEY).lasts(14, 28).blood(B2_BP, 20));
    static final Aff PKD = add(new Aff("Polycystic kidney disease", NEWBORN).both()  // [src~] ~1/1000 (dominant)
        .rate(100).on(L_KIDNEY, R_KIDNEY).chronic().progress(250).sev(500, 1000).dot(8).blood(B2_BP, 20));
    static final Aff BPH = add(new Aff("Enlarged prostate", DISEASE)
        .rate(1500).menOnly().doubles(15, 60).ages(40, 120).on(PROSTATE).chronic().progress(300).sev(1000, 3000)
        .pain(5).psy(PSY_ENERGY, -500).leads("Urinary tract infection", 5000).effects(QUIET));
    static final Aff STRESS_INC = add(new Aff("Incontinence", DISEASE)
        .rate(500).women(3).doubles(15, 60).ages(30, 120).after("Obstetric fistula", 50)
        .on(BLADDER).chronic().psy(PSY_SOCIAL, -1500).psy(PSY_MOOD, -800).effects(QUIET));

    // =================================================================================
    //  BLOOD
    // =================================================================================
    static { section(C_BLOOD); }
    static final Aff IRON = add(new Aff("Iron-deficiency anaemia", DISEASE)  // [src] WHO: ~1/3 of women anaemic
        .rate(2000).women(3).kids(2).risk(CH_MALNUT, 4).after("Hookworm disease", 6).after("Whipworm infection", 2).risk(CH_PREGNANT, 3)
        .on(MARROW).chronic().remits(30000).blood(B2_HB, -3000).effects(QUIET));
    static final Aff SICKLE = add(new Aff("Sickle cell disease", NONE) // genotype HbSS (Genes)
        .on(SPLEEN).dot(5).chronic().blood(B2_HB, -3500).immuno(30).leads("Sickle cell crisis", 100000)
        .fatal(2000).src("[src] 50-90% of children with SCD in Africa die before 5 without care (Grosse 2011)"));
    static final Aff SICKLE_C = add(new Aff("Sickle cell crisis", NONE)
        .on(MARROW).dot(30).lasts(3, 7).pain(80).effects(BEDRIDDEN | QUIET));
    static final Aff THAL = add(new Aff("Thalassaemia major", NONE)    // genotype
        .on(SPLEEN).dot(5).chronic().blood(B2_HB, -6000).src("[src~] untreated: death in childhood"));
    static final Aff HEMOPHILIA = add(new Aff("Haemophilia", NONE)     // genotype, X-linked
        .chronic().bleedx(500).leads("Joint bleed", 300000)
        .src("[src~] untreated life expectancy ~11-20 years"));
    static final Aff JOINT_BLEED = add(new Aff("Joint bleed", NONE)
        .on(L_KNEE, R_KNEE, L_ELBOW, R_ELBOW, L_ANKLE, R_ANKLE).hit(800).bleeds(100).lasts(3, 7).pain(50)
        .effects(DISABLE | QUIET));
    static final Aff LEUK_C = add(new Aff("Acute lymphoid leukaemia", DISEASE)  // [src~] ~4/100k children, peak 2-5
        .rate(4).peak(4, 3).ages(0, 70).after("Down syndrome", 20).on(MARROW).chronic().progress(4000)
        .sev(500, 1500).dot(80).immuno(50).blood(B2_HB, -3000).psy(PSY_ENERGY, -3000));
    static final Aff LEUK_A = add(new Aff("Acute myeloid leukaemia", DISEASE)  // [src~] ~4/100k, median age ~68
        .rate(5).doubles(10, 65).ages(20, 120).on(MARROW).chronic().progress(2500).sev(500, 1500)
        .dot(60).immuno(40).blood(B2_HB, -2500).psy(PSY_ENERGY, -3000));
    static final Aff LYMPHOMA = add(new Aff("Non-Hodgkin lymphoma", DISEASE)  // [src~] GLOBOCAN ~6/100k
        .rate(12).doubles(15, 60).on(LYMPH).alsoHit(SPLEEN).chronic().progress(2000).sev(500, 1500).dot(60).immuno(40)
        .psy(PSY_ENERGY, -3000));
    static final Aff MYELOMA = add(new Aff("Myeloma", DISEASE)
        .rate(10).doubles(8, 70).ages(35, 120).on(MARROW).chronic().progress(2000).sev(500, 1500).dot(40)
        .bmd(60).immuno(30).pain(30).alsoHit(L_KIDNEY, R_KIDNEY));

    // =================================================================================
    //  CANCER  (rates are per 100k per year AT the reference age; roughly GLOBOCAN 2020 [src~])
    //  Severity starts small and grows; damage = dot * severity. Untreated median
    //  survival of 1-5 years emerges from progress vs. organ healing [tune].
    // =================================================================================
    static { section(C_CANCER); }
    static final Aff LUNG_CA = add(new Aff("Lung cancer", DISEASE)
        .rate(25).doubles(8, 65).ages(30, 120).men(1.5).gene(G_CANCER, 2).risk(CH_TOBACCO, 15).risk(CH_SMOKE, 2)
        .on(L_LUNG, R_LUNG).chronic().progress(3500).sev(300, 1000).dot(90).pain(20).psy(PSY_ENERGY, -3000)
        .leads("Bone metastases", 8000).leads("Brain metastases", 5000)
        .src("[src] smokers ~15-30x never-smokers; tobacco needs Config.TOBACCO"));
    static final Aff BREAST_CA = add(new Aff("Breast cancer", DISEASE)
        .rate(150).doubles(14, 55).ages(25, 120).women(1).men(0.01).gene(G_CANCER, 3)
        .on(BREASTS).chronic().progress(2000).sev(300, 1000).dot(40).pain(15)
        .leads("Bone metastases", 15000).leads("Liver metastases", 8000).leads("Brain metastases", 4000));
    static final Aff PROSTATE_CA = add(new Aff("Prostate cancer", DISEASE)
        .rate(150).menOnly().doubles(7, 70).ages(40, 120).gene(G_CANCER, 3)
        .on(PROSTATE).chronic().progress(800).sev(300, 1000).dot(30).pain(10).leads("Bone metastases", 6000));
    static final Aff COLON_CA = add(new Aff("Bowel cancer", DISEASE)
        .rate(80).doubles(9, 65).ages(30, 120).men(1.3).gene(G_CANCER, 3).after("Crohn's disease", 2).after("Ulcerative colitis", 3)
        .on(COLON).chronic().progress(2000).sev(300, 1000).dot(60).bleeds(5).pain(20).leads("Liver metastases", 10000));
    static final Aff STOMACH_CA = add(new Aff("Stomach cancer", DISEASE)
        .rate(40).doubles(9, 65).ages(30, 120).men(2).gene(G_CANCER, 2).after("Stomach ulcer", 3)
        .on(STOMACH).chronic().progress(3000).sev(300, 1000).dot(70).pain(25));
    static final Aff LIVER_CA = add(new Aff("Liver cancer", DISEASE)
        .rate(20).doubles(10, 65).ages(20, 120).men(2.5).after("Cirrhosis", 20).after("Chronic hepatitis B", 15)
        .on(LIVER).chronic().progress(4000).sev(300, 1000).dot(90).pain(25));
    static final Aff PANC_CA = add(new Aff("Pancreatic cancer", DISEASE)
        .rate(20).doubles(8, 70).ages(30, 120).after("Chronic pancreatitis", 10).risk(CH_TOBACCO, 2)
        .on(PANCREAS).chronic().progress(6000).sev(500, 1500).dot(100).pain(40).blood(B2_GLU, 300));
    static final Aff CERVIX_CA = add(new Aff("Cervical cancer", DISEASE)  // HPV
        .rate(30).womenOnly().peak(48, 15).needs(CH_SEXACTIVE, 2).on(UTERUS).chronic().progress(1500)
        .sev(300, 1000).dot(50).bleeds(10).pain(20));
    static final Aff UTERUS_CA = add(new Aff("Womb cancer", DISEASE)
        .rate(25).womenOnly().doubles(12, 60).ages(30, 120).risk(CH_OBESE, 3)
        .on(UTERUS).chronic().progress(1500).sev(300, 1000).dot(50).bleeds(10).pain(15));
    static final Aff OVARY_CA = add(new Aff("Ovarian cancer", DISEASE)
        .rate(20).womenOnly().doubles(12, 60).ages(25, 120).gene(G_CANCER, 3)
        .on(GONADS).chronic().progress(3000).sev(300, 1000).dot(60).pain(20));
    static final Aff TESTIS_CA = add(new Aff("Testicular cancer", DISEASE)  // [src~] peak 25-35
        .rate(6).menOnly().peak(30, 8).on(GONADS).chronic().progress(2500).sev(300, 1000).dot(40).pain(10));
    static final Aff BLADDER_CA = add(new Aff("Bladder cancer", DISEASE)
        .rate(30).doubles(9, 70).ages(35, 120).men(3).risk(CH_TOBACCO, 3)
        .on(BLADDER).chronic().progress(1500).sev(300, 1000).dot(40).bleeds(5).pain(15));
    static final Aff KIDNEY_CA = add(new Aff("Kidney cancer", DISEASE)
        .rate(20).doubles(10, 65).ages(30, 120).men(2).on(L_KIDNEY, R_KIDNEY).chronic().progress(2000)
        .sev(300, 1000).dot(50).pain(15));
    static final Aff OESOPH_CA = add(new Aff("Oesophageal cancer", DISEASE)
        .rate(20).doubles(9, 65).ages(30, 120).men(2.5).after("Alcohol use disorder", 4).risk(CH_TOBACCO, 4)
        .on(OESOPHAGUS).chronic().progress(4000).sev(300, 1000).dot(80).pain(25));
    static final Aff THYROID_CA = add(new Aff("Thyroid cancer", DISEASE)
        .rate(10).women(3).ages(15, 120).on(THYROID).chronic().progress(300).sev(300, 1000).dot(10).effects(QUIET));
    static final Aff BRAIN_CA = add(new Aff("Brain tumour", DISEASE)
        .rate(6).doubles(20, 55).kids(1).on(BRAIN).chronic().progress(3000).sev(300, 1000).dot(30).pain(20)
        .psy(PSY_COGNITION, -3000).psy(PSY_IMPULSE, 1500).leads("Epilepsy", 30000));
    static final Aff MELANOMA = add(new Aff("Melanoma", DISEASE)
        .rate(8).doubles(15, 60).ages(15, 120).risk(CH_SUN, 4).after("Albinism", 50)
        .on(SKIN).chronic().progress(2500).sev(300, 1000).dot(20).leads("Brain metastases", 8000).leads("Liver metastases", 8000));
    static final Aff SKIN_CA = add(new Aff("Skin cancer (non-melanoma)", DISEASE)
        .rate(60).doubles(10, 65).ages(30, 120).risk(CH_SUN, 4).after("Albinism", 50)
        .on(SKIN).chronic().progress(200).sev(300, 1000).dot(3).effects(QUIET));
    static final Aff OSTEOSARC = add(new Aff("Bone cancer", DISEASE)   // osteosarcoma: teens, and after Paget's
        .rate(1).peak(16, 5).after("Paget's disease of bone", 30)
        .onBone(L_THIGH, R_THIGH, L_SHIN, R_SHIN, L_UPARM, R_UPARM).chronic().progress(3000).sev(500, 1500)
        .dot(120).pain(40).leads("Lung metastases", 30000));
    static final Aff BONE_METS = add(new Aff("Bone metastases", NONE)
        .onBone(THORACIC, LUMBAR, PELVIS, L_THIGH, R_THIGH, CHEST).chronic().progress(3000).sev(1000, 3000)
        .dot(60).bmd(20).pain(50));
    static final Aff LIVER_METS = add(new Aff("Liver metastases", NONE)
        .on(LIVER).chronic().progress(4000).sev(1000, 3000).dot(80).pain(25).psy(PSY_ENERGY, -3000));
    static final Aff BRAIN_METS = add(new Aff("Brain metastases", NONE)
        .on(BRAIN).chronic().progress(4000).sev(1000, 3000).dot(30).psy(PSY_COGNITION, -3000));
    static final Aff LUNG_METS = add(new Aff("Lung metastases", NONE).both()
        .on(L_LUNG, R_LUNG).chronic().progress(4000).sev(1000, 3000).dot(80));

    // =================================================================================
    //  BONES, JOINTS & MUSCLES  (osteoporosis and fracture risk EMERGE from Bones/BMD)
    // =================================================================================
    static { section(C_MSK); }
    static final Aff OSTEOPOROSIS = add(new Aff("Osteoporosis", NONE)  // label: BMD < 70% of peak (T-score ~ -2.5)
        .chronic().src("[src] WHO 1994 definition: BMD 2.5 SD below young-adult mean"));
    static final Aff OA = add(new Aff("Osteoarthritis", DISEASE)       // joint wear also emerges from ageing
        .rate(400).doubles(12, 60).ages(35, 120).women(1.5).risk(CH_LABOUR, 2).risk(CH_OBESE, 2)
        .on(BIG_JOINTS).dot(2).chronic().progress(500).sev(2000, 5000).pain(15));
    static final Aff RA = add(new Aff("Rheumatoid arthritis", DISEASE).both() // [src~] ~40/100k/yr
        .rate(40).women(3).peak(50, 15).gene(G_AUTOIMM, 4).on(L_WRIST, R_WRIST).dot(5).chronic()
        .episodes(60, 90, 90, 180).alsoHit(L_KNEE, R_KNEE).fn(FN_MANIP, 3000).fn(FN_MOBILITY, 2000)
        .pain(30).psy(PSY_ENERGY, -2500).bmd(10));
    static final Aff AS = add(new Aff("Ankylosing spondylitis", DISEASE)
        .rate(7).men(3).ages(15, 45).gene(G_AUTOIMM, 4).on(SPINE_J).dot(2).chronic().pain(25).fn(FN_MOBILITY, 1500));
    static final Aff LUPUS = add(new Aff("Lupus", DISEASE).both()             // [src~] women ~9:1
        .rate(5).women(9).ages(14, 50).gene(G_AUTOIMM, 5).on(L_KIDNEY, R_KIDNEY).dot(5).chronic()
        .episodes(30, 60, 180, 360).pain(25).psy(PSY_ENERGY, -3000).alsoHit(SKIN));
    static final Aff SCOLIOSIS = add(new Aff("Scoliosis", DISEASE)     // [src~] 2-3% adolescents, girls more severe
        .rate(300).women(3).ages(10, 16).on(SPINE_J).hit(1500).chronic().sev(1000, 10000).deform(THORACIC, 30)
        .fn(FN_MOBILITY, 500).effects(QUIET));
    static final Aff RICKETS = add(new Aff("Rickets", DISEASE)         // vitamin D: no sun / poor diet
        .rate(4000).ages(0, 5).needs(CH_VITD_DEF).on(MARROW).lasts(365, 730).bmd(150)
        .deform(L_SHIN, 40).fn(FN_MOBILITY, 2000).src("[src] very common in 19th-c. industrial cities"));
    static final Aff OSTEOMALACIA = add(new Aff("Osteomalacia", DISEASE)
        .rate(100).women(2).ages(18, 120).needs(CH_VITD_DEF).on(MARROW).chronic().remits(20000).bmd(40)
        .pain(20).fn(FN_MOBILITY, 1500));
    static final Aff PAGET = add(new Aff("Paget's disease of bone", DISEASE)
        .rate(40).doubles(8, 70).ages(50, 120).men(1.5).onBone(PELVIS, LUMBAR, L_THIGH, R_THIGH, SKULL)
        .dot(2).chronic().pain(20));
    static final Aff OI = add(new Aff("Brittle bone disease", NONE)    // osteogenesis imperfecta, genotype
        .chronic().src("bones at 40% strength (Bones)"));
    static final Aff ACH = add(new Aff("Achondroplasia", NONE)         // genotype
        .chronic().fn(FN_MOBILITY, 1000).on(SPINE_J).dot(1));
    static final Aff DMD = add(new Aff("Duchenne muscular dystrophy", NONE).both()  // genotype, X-linked
        .on(L_LEG_M, R_LEG_M).dot(6).alsoHit(CORE_M, HEART).chronic().fn(FN_MOBILITY, 4000)
        .src("[src] untreated: wheelchair ~12, death late teens"));
    static final Aff BACK = add(new Aff("Low back pain", DISEASE)      // [src] GBD: leading cause of disability
        .rate(6000).ages(15, 120).peak(45, 20).risk(CH_LABOUR, 2).risk(CH_STRESS, 1.5)
        .on(SPINE_J).lasts(14, 42).pain(30).fn(FN_MOBILITY, 2000).then("Chronic back pain", 10000).effects(QUIET));
    static final Aff BACK_C = add(new Aff("Chronic back pain", NONE)
        .on(SPINE_J).chronic().remits(20000).pain(25).fn(FN_MOBILITY, 1000));
    static final Aff FIBRO = add(new Aff("Fibromyalgia", DISEASE)
        .rate(100).women(4).ages(20, 70).risk(CH_STRESS, 2).risk(CH_ACE, 2)
        .on(CORE_M).chronic().pain(25).psy(PSY_ENERGY, -2000).psy(PSY_COGNITION, -1000));
    static final Aff CARPAL = add(new Aff("Carpal tunnel syndrome", DISEASE)
        .rate(300).women(3).ages(25, 80).risk(CH_LABOUR, 2).risk(CH_PREGNANT, 2).after("Underactive thyroid", 2)
        .on(L_WRIST, R_WRIST).chronic().remits(20000).pain(15).fn(FN_MANIP, 1500).effects(QUIET));
    static final Aff FROZEN = add(new Aff("Frozen shoulder", DISEASE)
        .rate(150).women(1.5).ages(35, 75).after("Type 2 diabetes", 3)
        .on(L_SHOULDER, R_SHOULDER).hit(2000).lasts(365, 365).pain(30).effects(DISABLE | QUIET));
    static final Aff CLUBFOOT = add(new Aff("Clubfoot", NEWBORN)       // [src] ~1/1000 births, boys 2x
        .rate(100).men(2).on(L_ANKLE, R_ANKLE).hit(5000).chronic().deform(L_FOOT, 60).fn(FN_MOBILITY, 3000));
    static final Aff DDH = add(new Aff("Hip dysplasia", NEWBORN)
        .rate(150).women(4).on(L_HIP, R_HIP).hit(4000).dot(1).chronic().fn(FN_MOBILITY, 1500));

    // =================================================================================
    //  NEUROLOGICAL
    // =================================================================================
    static { section(C_NEURO); }
    static final Aff EPILEPSY = add(new Aff("Epilepsy", DISEASE)       // [src] Fiest 2017: 61/100k/yr
        .rate(40).kids(2).gene(G_NEURODEV, 2).risk(CH_HEADINJ, 10).after("Ischaemic stroke", 10).after("Intracerebral haemorrhage", 10).after("Meningitis", 5)
        .after("Birth asphyxia", 5).on(BRAIN).chronic().remits(3000)
        .leads("Fall", 20000).leads("Burn", 2000).fatal(116)
        .psy(PSY_MOOD, -1000).psy(PSY_ANXIETY, 1000).psy(PSY_COGNITION, -800)
        .src("fatal(): SUDEP ~1.16/1000/yr (Thurman 2014)"));
    static final Aff MIGRAINE = add(new Aff("Migraine", DISEASE)       // [src~] ~15% prevalence, women 3x
        .rate(700).women(3).ages(8, 70).on(BRAIN).chronic().remits(3000).episodes(1, 2, 20, 40)
        .pain(40).psy(PSY_ENERGY, -4000).psy(PSY_SOCIAL, -3000).effects(QUIET));
    static final Aff PARKINSONS = add(new Aff("Parkinson's disease", DISEASE)
        .rate(80).doubles(7, 70).ages(40, 120).men(1.5).on(BRAIN).dot(1).chronic().progress(1000).sev(1000, 2000)
        .fn(FN_MOBILITY, 5000).fn(FN_MANIP, 4000).fn(FN_SPEECH, 3000).psy(PSY_MOOD, -2000).psy(PSY_COGNITION, -1500)
        .leads("Fall", 50000).leads("Parkinson's dementia", 4000));
    static final Aff PD_DEM = add(new Aff("Parkinson's dementia", NONE)
        .on(BRAIN).dot(3).chronic().progress(1500).sev(2000, 4000).psy(PSY_COGNITION, -7000).psy(PSY_REALITY, 3000));
    static final Aff MS = add(new Aff("Multiple sclerosis", DISEASE)   // [src~] 3-7/100k/yr, women 2-3x
        .rate(5).women(2.5).peak(30, 9).gene(G_AUTOIMM, 3).on(CORD).dot(3).alsoHit(NERVES).chronic()
        .episodes(30, 30, 365, 365).progress(400).fn(FN_MOBILITY, 4000).fn(FN_VISION, 2000)
        .psy(PSY_ENERGY, -4000).psy(PSY_MOOD, -2500).psy(PSY_COGNITION, -1500));
    static final Aff ALS = add(new Aff("Motor neurone disease", DISEASE)  // [src~] ~2/100k/yr
        .rate(3).doubles(10, 60).ages(30, 120).men(1.3).on(CORE_M).dot(15).alsoHit(L_LEG_M, R_LEG_M, L_ARM_M, R_ARM_M)
        .chronic().progress(3500).sev(1000, 2000).fn(FN_MOBILITY, 8000).fn(FN_MANIP, 8000).fn(FN_SPEECH, 8000)
        .psy(PSY_MOOD, -2000));
    static final Aff HD = add(new Aff("Huntington's disease", DISEASE) // genotype, dominant
        .rate(5000).ages(28, 80).needs(CH_GENO_HD).on(BRAIN).dot(3).chronic().progress(700).sev(1000, 2000)
        .fn(FN_MOBILITY, 4000).fn(FN_MANIP, 4000).fn(FN_SPEECH, 4000).psy(PSY_COGNITION, -6000)
        .psy(PSY_MOOD, -3000).psy(PSY_IMPULSE, 3000).psy(PSY_AGGRESSION, 2500).psy(PSY_REALITY, 1500)
        .src("[src] mean onset ~40, death ~15-20 yr later"));
    static final Aff CP = add(new Aff("Cerebral palsy", NEWBORN)       // [src] ~2 per 1000 births
        .rate(100).after("Birth asphyxia", 20).after("Premature birth", 8)
        .on(BRAIN).chronic().sev(2000, 10000).fn(FN_MOBILITY, 6000).fn(FN_MANIP, 3000).fn(FN_SPEECH, 3000)
        .psy(PSY_COGNITION, -1500));
    static final Aff SPINA_BIFIDA = add(new Aff("Spina bifida", NEWBORN)  // [src~] ~0.5/1000; folate
        .rate(50).risk(CH_M_MALNUT, 5).on(CORD).hit(6000).chronic().sev(3000, 10000).fn(FN_MOBILITY, 8000)
        .alsoHit(BLADDER).leads("Urinary tract infection", 100000).fatal(3000));
    static final Aff ANENCEPHALY = add(new Aff("Anencephaly", NEWBORN)
        .rate(30).risk(CH_M_MALNUT, 5).on(BRAIN).hit(10000).effects(SUDDEN));
    static final Aff HYDRO = add(new Aff("Hydrocephalus", NEWBORN)
        .rate(80).after("Spina bifida", 50).on(BRAIN).dot(20).chronic().progress(3000).sev(1000, 3000)
        .psy(PSY_COGNITION, -4000));
    static final Aff BELLS = add(new Aff("Bell's palsy", DISEASE)      // [src~] 20-30/100k/yr
        .rate(25).on(NERVES).lasts(30, 60).fn(FN_SPEECH, 2000).psy(PSY_SOCIAL, -1000).effects(QUIET));
    static final Aff NEUROPATHY = add(new Aff("Peripheral neuropathy", NONE)   // label: nerves < 60%
        .chronic().pain(10));
    static final Aff SIDS = add(new Aff("Sudden infant death", DISEASE) // [src~] 0.5-1 per 1000 live births
        .rate(80).ages(0, 1).men(1.5).effects(SUDDEN));
    static final Aff DELIRIUM = add(new Aff("Delirium", NONE)          // label: fever, sepsis, toxins, low O2
        .on(BRAIN).lasts(3, 4).psy(PSY_COGNITION, -5000).psy(PSY_REALITY, 4000).psy(PSY_AGGRESSION, 1500)
        .psy(PSY_ANXIETY, 2000).effects(QUIET).src("[src] up to 1/3 of elderly hospital patients"));

    // =================================================================================
    //  EYES & EARS  (presbyopia and age hearing loss emerge from Body ageing)
    // =================================================================================
    static { section(C_SENSE); }
    static final Aff CATARACT = add(new Aff("Cataract", DISEASE).both()       // [src] WHO: leading cause of blindness
        .rate(1500).doubles(8, 70).ages(40, 120).risk(CH_GLU, 2).risk(CH_SUN, 2)
        .on(L_EYE, R_EYE).dot(4).chronic().progress(1500).sev(500, 1500));
    static final Aff GLAUCOMA = add(new Aff("Glaucoma", DISEASE).both()
        .rate(80).doubles(10, 65).ages(35, 120).on(L_EYE, R_EYE).dot(3).chronic().progress(800).sev(500, 1500));
    static final Aff AMD = add(new Aff("Macular degeneration", DISEASE).both()
        .rate(100).doubles(7, 75).ages(50, 120).on(L_EYE, R_EYE).dot(3).chronic().progress(800).sev(500, 1500));
    static final Aff MYOPIA = add(new Aff("Short sight", DISEASE)      // no glasses in this world
        .rate(800).ages(6, 25).peak(12, 5).on(L_EYE).chronic().fn(FN_VISION, 1500).effects(QUIET));
    static final Aff XEROPHTH = add(new Aff("Vitamin A blindness", DISEASE).both()  // [src] WHO: VAD top cause of child blindness
        .rate(3000).ages(0, 10).needs(CH_MALNUT).on(L_EYE, R_EYE).dot(25).lasts(30, 60).immuno(20));
    static final Aff TINNITUS = add(new Aff("Tinnitus", DISEASE)
        .rate(300).doubles(20, 50).ages(15, 120).on(L_EAR, R_EAR).chronic().psy(PSY_ANXIETY, 1000)
        .psy(PSY_MOOD, -800).psy(PSY_ENERGY, -500).effects(QUIET));
    static final Aff VERTIGO = add(new Aff("Vertigo", DISEASE)         // BPPV
        .rate(100).women(2).ages(35, 120).on(L_EAR, R_EAR).lasts(7, 21).fn(FN_MOBILITY, 3000)
        .leads("Fall", 300000).effects(QUIET));
    static final Aff BLINDNESS = add(new Aff("Blindness", NONE)        // label: vision < 10%
        .chronic().psy(PSY_MOOD, -1500).psy(PSY_SOCIAL, -1000));
    static final Aff DEAFNESS = add(new Aff("Deafness", NONE)          // label: hearing < 10%
        .chronic().psy(PSY_SOCIAL, -2500).psy(PSY_MOOD, -1000)
        .src("[src~] hearing loss doubles depression and raises dementia risk (Livingston 2020)"));

    // =================================================================================
    //  SKIN
    // =================================================================================
    static { section(C_SKIN); }
    static final Aff ECZEMA = add(new Aff("Eczema", DISEASE)
        .rate(1500).kids(5).gene(G_AUTOIMM, 2).on(SKIN).chronic().remits(15000).pain(10)
        .psy(PSY_ENERGY, -500).effects(QUIET));
    static final Aff PSORIASIS = add(new Aff("Psoriasis", DISEASE)
        .rate(80).ages(15, 70).gene(G_AUTOIMM, 3).on(SKIN).chronic().episodes(90, 90, 180, 360).pain(8)
        .psy(PSY_MOOD, -1500).psy(PSY_SOCIAL, -1500).effects(QUIET));
    static final Aff ACNE = add(new Aff("Acne", DISEASE)               // [src~] ~85% of adolescents
        .rate(15000).peak(16, 3).on(SKIN).lasts(365, 730).psy(PSY_MOOD, -500).psy(PSY_SOCIAL, -800).effects(QUIET));
    static final Aff VITILIGO = add(new Aff("Vitiligo", DISEASE)
        .rate(20).gene(G_AUTOIMM, 3).on(SKIN).chronic().psy(PSY_SOCIAL, -500).effects(QUIET));

    // =================================================================================
    //  PREGNANCY & REPRODUCTIVE
    // =================================================================================
    static { section(C_REPRO); }
    static final Aff MISCARRIAGE = add(new Aff("Miscarriage", DISEASE) // [src] ~15% of recognised pregnancies
        .rate(40000).needs(CH_EARLYPREG).risk(CH_M_AGE, 3).on(UTERUS).bleeds(300).lasts(2, 3).pain(40)
        .effects(END_PREG).then("Grief", 30000));
    static final Aff ECTOPIC = add(new Aff("Ectopic pregnancy", DISEASE) // [src~] 1-2% of pregnancies
        .rate(2500).needs(CH_EARLYPREG).after("Pelvic inflammatory disease", 6).on(UTERUS).hit(3000).bleeds(1500)
        .lasts(2, 3).pain(80).effects(END_PREG | BEDRIDDEN));
    static final Aff PREECLAMPSIA = add(new Aff("Pre-eclampsia", DISEASE)  // [src] 2-8% of pregnancies
        .rate(20000).needs(CH_LATEPREG).after("High blood pressure", 4).on(L_KIDNEY).dot(10).alsoHit(R_KIDNEY, LIVER).lasts(30, 60)
        .blood(B2_BP, 40).leads("Eclampsia", 20000));
    static final Aff ECLAMPSIA = add(new Aff("Eclampsia", NONE)
        .on(BRAIN).hit(2000).dot(100).lasts(2, 3).effects(BEDRIDDEN));
    static final Aff STILLBIRTH = add(new Aff("Stillbirth", BIRTH)     // [src~] historical 3-4% of births
        .rate(2500).risk(CH_M_AGE, 2).after("Pre-eclampsia", 3).after("Malaria", 3).after("Syphilis", 5)
        .after("Latent syphilis", 3).on(UTERUS).lasts(1, 0).effects(STILLBORN).then("Grief"));
    static final Aff OBSTRUCTED = add(new Aff("Obstructed labour", BIRTH)  // [src~] ~5% without obstetrics
        .rate(3000).after("Rickets", 3).on(UTERUS).hit(4000).sepsis(300).lasts(3, 5).pain(80)
        .effects(BEDRIDDEN).then("Obstetric fistula", 20000));
    static final Aff FISTULA = add(new Aff("Obstetric fistula", NONE)  // [src] WHO; social exclusion common
        .on(BLADDER).chronic().dot(1).psy(PSY_SOCIAL, -5000).psy(PSY_MOOD, -3000).leads("Urinary tract infection", 50000));
    static final Aff HEMOR = add(new Aff("Birth haemorrhage", BIRTH)   // [tune]
        .rate(3000).on(UTERUS).bleeds(2000).lasts(1, 2).pain(30).effects(BEDRIDDEN));
    static final Aff PUERP = add(new Aff("Childbed fever", BIRTH)      // [tune]
        .rate(2000).after("Obstructed labour", 4).on(UTERUS).sepsis(400).lasts(5, 10).pain(40).effects(BEDRIDDEN));
    static final Aff ENDOMETRIOSIS = add(new Aff("Endometriosis", DISEASE)  // [src~] ~10% of women
        .rate(250).womenOnly().ages(13, 45).on(UTERUS).chronic().pain(25).fert(50).psy(PSY_MOOD, -800));
    static final Aff FIBROIDS = add(new Aff("Uterine fibroids", DISEASE)
        .rate(500).womenOnly().ages(25, 55).on(UTERUS).chronic().bleeds(10).pain(10).fert(70).effects(QUIET));

    // =================================================================================
    //  NUTRITIONAL  (one malnutrition channel for now: the food model has no vitamins yet)
    // =================================================================================
    static { section(C_NUTRI); }
    static final Aff KWASH = add(new Aff("Kwashiorkor", DISEASE)       // protein deficiency in toddlers
        .rate(8000).ages(1, 5).needs(CH_MALNUT).on(LIVER).dot(10).lasts(60, 120).immuno(40)
        .psy(PSY_ENERGY, -3000).psy(PSY_MOOD, -2000));
    static final Aff SCURVY = add(new Aff("Scurvy", DISEASE)           // vitamin C
        .rate(2000).needs(CH_MALNUT).on(MOUTH).dot(10).alsoHit(SKIN).bleeds(20).lasts(60, 120)
        .psy(PSY_ENERGY, -3000).psy(PSY_MOOD, -2000));
    static final Aff PELLAGRA = add(new Aff("Pellagra", DISEASE)       // niacin: dermatitis, diarrhoea, dementia [src]
        .rate(500).needs(CH_MALNUT).on(SKIN).dot(5).alsoHit(SMALL_INT).bleeds(50).lasts(60, 120).effects(FLUID)
        .psy(PSY_COGNITION, -3000).psy(PSY_REALITY, 2000));
    static final Aff BERIBERI = add(new Aff("Beriberi", DISEASE)       // thiamine
        .rate(500).needs(CH_MALNUT).on(HEART).dot(15).alsoHit(NERVES).lasts(60, 120).fn(FN_MOBILITY, 3000));
    static final Aff CRETINISM = add(new Aff("Congenital iodine deficiency", NEWBORN)
        .rate(500).needs(CH_IODINE_DEF, 4).on(THYROID).chronic().sev(3000, 10000).psy(PSY_COGNITION, -7000)
        .blood(B2_META, -2000).fn(FN_HEARING, 3000));
    static final Aff MALNUTRITION = add(new Aff("Malnutrition", NONE)  // label: energy reserve < 25%
        .lasts(30, 0).psy(PSY_ENERGY, -2000).effects(QUIET));

    // =================================================================================
    //  CONGENITAL & GENETIC  (Mendelian ones are given at birth by Genes)
    // =================================================================================
    static { section(C_CONGEN); }
    static final Aff PRETERM = add(new Aff("Premature birth", NEWBORN) // [tune] ~1 in 10 births
        .rate(10000).risk(CH_M_MALNUT, 2).risk(CH_M_AGE, 1.5).on(L_LUNG).dot(150).alsoHit(R_LUNG).lasts(7, 21)
        .effects(BEDRIDDEN));
    static final Aff ASPHYX = add(new Aff("Birth asphyxia", NEWBORN)   // [tune]
        .rate(3000).on(BRAIN).hit(5000).dot(250).lasts(2, 4).effects(BEDRIDDEN)
        .then("Intellectual disability", 15000));
    static final Aff NEOSEP = add(new Aff("Newborn sepsis", NEWBORN)   // [tune]
        .rate(3000).on(SKIN).sepsis(450).lasts(5, 10).pain(30).effects(BEDRIDDEN));
    static final Aff DOWN = add(new Aff("Down syndrome", NEWBORN)      // [src] ~1/900 at maternal age 30, 1/100 at 40
        .rate(110).needs(CH_M_AGE_DS, 30).on(BRAIN).chronic().sev(5000, 10000).psy(PSY_COGNITION, -6000)
        .fn(FN_MOBILITY, 1000).immuno(20).fert(20)
        .src("untreated life expectancy ~10-25 yr historically; risk doubles ~every 3 yr of maternal age"));
    static final Aff TRISOMY = add(new Aff("Trisomy 13/18", NEWBORN)   // [src~] ~1/5000; most die in year 1
        .rate(10).needs(CH_M_AGE_DS, 30).on(HEART).hit(5000).chronic().fatal(100000));
    static final Aff CLEFT = add(new Aff("Cleft lip and palate", NEWBORN)  // [src] ~1/700
        .rate(140).on(MOUTH).hit(5000).chronic().fn(FN_SPEECH, 4000).psy(PSY_SOCIAL, -1500).fatal(3000)
        .src("fatal(): feeding difficulty without surgery [guess]"));
    static final Aff CRS = add(new Aff("Congenital rubella syndrome", NEWBORN)
        .rate(25000).needs(CH_M_RUBELLA).on(L_EAR).hit(7000).alsoHit(R_EAR, L_EYE, HEART).chronic()
        .psy(PSY_COGNITION, -3000).src("[src~] up to 90% after first-trimester infection; ~25% overall"));
    static final Aff FAS = add(new Aff("Fetal alcohol syndrome", NEWBORN)
        .rate(15000).needs(CH_M_ALCOHOL).on(BRAIN).chronic().sev(3000, 10000).psy(PSY_COGNITION, -4000)
        .psy(PSY_IMPULSE, 3000).psy(PSY_SOCIAL, -1000));
    static final Aff CONG_DEAF = add(new Aff("Congenital deafness", NEWBORN)  // [src] ~1-2/1000
        .rate(100).on(L_EAR).hit(10000).alsoHit(R_EAR).chronic().effects(QUIET));
    static final Aff CONG_BLIND = add(new Aff("Congenital cataract", NEWBORN)
        .rate(30).risk(CH_M_RUBELLA, 20).on(L_EYE).hit(8000).alsoHit(R_EYE).chronic().effects(QUIET));
    static final Aff KLINEFELTER = add(new Aff("Klinefelter syndrome", NEWBORN)  // [src] ~1/600 males
        .rate(170).menOnly().on(GONADS).chronic().fert(0).psy(PSY_COGNITION, -500).effects(QUIET));
    static final Aff TURNER = add(new Aff("Turner syndrome", NEWBORN)  // [src] ~1/2500 females
        .rate(40).womenOnly().on(GONADS).chronic().fert(0).alsoHit(HEART).effects(QUIET));
    static final Aff FRAGILEX = add(new Aff("Fragile X syndrome", NEWBORN)  // [src~] 1/4000 M, 1/8000 F
        .rate(25).men(1).women(0.5).on(BRAIN).chronic().psy(PSY_COGNITION, -5000).psy(PSY_SOCIAL, -3000)
        .psy(PSY_ANXIETY, 2000));
    static final Aff CF = add(new Aff("Cystic fibrosis", NONE).both()         // genotype
        .on(L_LUNG, R_LUNG).dot(5).alsoHit(PANCREAS).chronic().progress(800).sev(2000, 5000).fert(30)
        .src("[src~] before modern care most died in childhood"));
    static final Aff PKU = add(new Aff("Phenylketonuria", NONE)        // genotype; untreated: brain damage
        .on(BRAIN).dot(4).chronic().progress(3000).sev(1000, 2000).psy(PSY_COGNITION, -7000).psy(PSY_AGGRESSION, 1000));
    static final Aff ALBINISM = add(new Aff("Albinism", NONE)          // genotype
        .chronic().on(L_EYE).alsoHit(R_EYE).hit(4000).src("vision, sunburn, skin cancer; persecution is social (Mind)"));
    static final Aff INTELL = add(new Aff("Intellectual disability", NEWBORN)  // [src~] 1-2%
        .rate(1000).gene(G_NEURODEV, 3).risk(CH_M_ALCOHOL, 3).risk(CH_M_MALNUT, 2).on(BRAIN).chronic()
        .sev(2000, 10000).psy(PSY_COGNITION, -6000).psy(PSY_SOCIAL, -1000));

    // =================================================================================
    //  MENTAL & BEHAVIOURAL
    //  Effects are on the 8 psych dimensions (World.psy) the behaviour code reads.
    //  Suicide is not rolled: it emerges in Psych from low mood, impulsivity and psychosis.
    // =================================================================================
    static { section(C_MENTAL); }
    static final Aff MDD = add(new Aff("Major depressive disorder", DISEASE)
        .rate(1200).ages(10, 120).women(2).gene(G_MOOD, 3).risk(CH_STRESS, 6).risk(CH_ACE, 2.5).risk(CH_PAIN, 2.5)
        .on(BRAIN).chronic().episodes(90, 180, 365, 1460).remits(15000).sev(4000, 10000)
        .psy(PSY_MOOD, -5000).psy(PSY_ENERGY, -4000).psy(PSY_SOCIAL, -4000).psy(PSY_COGNITION, -1500)
        .psy(PSY_ANXIETY, 2000).fert(80)
        .src("[src] WHO: ~5% of adults, women ~1.5-2x; episodes ~6 months; ~50% recur; heritability ~37%"));
    static final Aff DYSTHYMIA = add(new Aff("Persistent depressive disorder", DISEASE)
        .rate(200).ages(10, 120).women(2).gene(G_MOOD, 3).risk(CH_STRESS, 3).risk(CH_ACE, 3)
        .on(BRAIN).chronic().remits(5000).psy(PSY_MOOD, -3000).psy(PSY_ENERGY, -2000).psy(PSY_SOCIAL, -1500));
    static final Aff BIPOLAR = add(new Aff("Bipolar disorder", DISEASE) // [src~] ~1% lifetime; heritability ~70-80%
        .rate(25).peak(22, 8).gene(G_MOOD, 8).risk(CH_STRESS, 2).on(BRAIN).chronic()
        .leads("Manic episode", 30000).leads("Bipolar depression", 40000).psy(PSY_MOOD, -500));
    static final Aff MANIA = add(new Aff("Manic episode", NONE)
        .on(BRAIN).lasts(30, 60).psy(PSY_MOOD, 6000).psy(PSY_ENERGY, 5000).psy(PSY_IMPULSE, 6000)
        .psy(PSY_AGGRESSION, 2000).psy(PSY_REALITY, 2500).psy(PSY_SOCIAL, 4000));
    static final Aff BP_DEP = add(new Aff("Bipolar depression", NONE)
        .on(BRAIN).lasts(60, 120).psy(PSY_MOOD, -6500).psy(PSY_ENERGY, -5000).psy(PSY_SOCIAL, -4000)
        .psy(PSY_IMPULSE, 1500));
    static final Aff GAD = add(new Aff("Generalised anxiety disorder", DISEASE)  // [src~] ~3% 12-month
        .rate(600).ages(8, 120).women(2).gene(G_ANXIETY, 3).risk(CH_STRESS, 4).risk(CH_ACE, 2)
        .on(BRAIN).chronic().remits(8000).psy(PSY_ANXIETY, 5000).psy(PSY_ENERGY, -1500).psy(PSY_MOOD, -1500)
        .psy(PSY_COGNITION, -1000).stress(3));
    static final Aff PANIC = add(new Aff("Panic disorder", DISEASE)
        .rate(250).ages(12, 70).women(2).gene(G_ANXIETY, 3).risk(CH_STRESS, 3)
        .on(BRAIN).chronic().remits(8000).psy(PSY_ANXIETY, 3500).psy(PSY_SOCIAL, -1500));
    static final Aff SOCIAL_ANX = add(new Aff("Social anxiety disorder", DISEASE)
        .rate(400).peak(13, 5).gene(G_ANXIETY, 3).risk(CH_ACE, 2).on(BRAIN).chronic().remits(4000)
        .psy(PSY_SOCIAL, -5000).psy(PSY_ANXIETY, 3000));
    static final Aff PHOBIA = add(new Aff("Specific phobia", DISEASE)  // [src] Wardenaar 2017: ~7% lifetime
        .rate(500).kids(2).ages(3, 120).gene(G_ANXIETY, 2).on(BRAIN).chronic().remits(5000)
        .psy(PSY_ANXIETY, 800).effects(QUIET));
    static final Aff PTSD = add(new Aff("Post-traumatic stress disorder", TRAUMA)  // [src~] ~10-20% after serious injury
        .rate(12000).women(2).gene(G_ANXIETY, 2).risk(CH_ACE, 2).risk(CH_STRESS, 2)
        .on(BRAIN).chronic().remits(30000).psy(PSY_ANXIETY, 5000).psy(PSY_MOOD, -3000).psy(PSY_AGGRESSION, 2000)
        .psy(PSY_SOCIAL, -3000).psy(PSY_IMPULSE, 1500).psy(PSY_ENERGY, -1500).stress(5)
        .src("~half remit within ~2 years [src~]"));
    static final Aff OCD = add(new Aff("Obsessive-compulsive disorder", DISEASE)
        .rate(60).peak(20, 10).gene(G_ANXIETY, 4).on(BRAIN).chronic().remits(3000)
        .psy(PSY_ANXIETY, 3500).psy(PSY_ENERGY, -1500).psy(PSY_SOCIAL, -1500));
    static final Aff SCHIZO = add(new Aff("Schizophrenia", DISEASE)    // [src] McGrath 2008: median 15.2/100k/yr; M 1.4x
        .rate(15).peak(24, 7).men(1.4).gene(G_PSYCHOSIS, 10).risk(CH_STRESS, 2).risk(CH_ACE, 2)
        .risk(CH_HEADINJ, 1.5).on(BRAIN).chronic().leads("Psychotic episode", 50000)
        .psy(PSY_SOCIAL, -2500).psy(PSY_ENERGY, -2500).psy(PSY_COGNITION, -2000)
        .src("[src] ~5% lifetime suicide (Hor & Taylor 2010); heritability ~80%"));
    static final Aff PSYCHOSIS = add(new Aff("Psychotic episode", NONE)
        .on(BRAIN).lasts(60, 120).psy(PSY_REALITY, 7000).psy(PSY_COGNITION, -3000).psy(PSY_SOCIAL, -4000)
        .psy(PSY_ANXIETY, 3000).psy(PSY_MOOD, -2000).psy(PSY_AGGRESSION, 1000)
        .src("violence risk modestly raised, mostly with substance use (Fazel 2009)"));
    static final Aff PP_DEP = add(new Aff("Postpartum depression", BIRTH)  // [src~] 10-15% of mothers
        .rate(12000).gene(G_MOOD, 3).risk(CH_STRESS, 3).on(BRAIN).lasts(90, 180)
        .psy(PSY_MOOD, -5000).psy(PSY_ENERGY, -4000).psy(PSY_SOCIAL, -3000).psy(PSY_ANXIETY, 2500));
    static final Aff PP_PSY = add(new Aff("Postpartum psychosis", BIRTH)  // [src] 1-2 per 1000 births
        .rate(150).gene(G_MOOD, 4).after("Bipolar disorder", 50).on(BRAIN).lasts(30, 60)
        .psy(PSY_REALITY, 7000).psy(PSY_MOOD, -2000).psy(PSY_IMPULSE, 4000).psy(PSY_COGNITION, -3000));
    static final Aff GRIEF = add(new Aff("Grief", BEREAVE)             // normal grief: not a disorder, but it moves the mind
        .rate(100000).on(BRAIN).lasts(60, 120).psy(PSY_MOOD, -3500).psy(PSY_ENERGY, -2000)
        .psy(PSY_SOCIAL, -2500).effects(QUIET));
    static final Aff PGD = add(new Aff("Prolonged grief disorder", BEREAVE)  // [src] Lundorff 2017: ~9.8% of bereaved
        .rate(10000).gene(G_MOOD, 2).risk(CH_STRESS, 2).on(BRAIN).lasts(365, 730)
        .psy(PSY_MOOD, -4500).psy(PSY_SOCIAL, -4000).psy(PSY_ENERGY, -3000).stress(4));
    static final Aff TRAUMATIC_GRIEF = add(new Aff("Post-traumatic stress (bereavement)", BEREAVE)
        .rate(3000).gene(G_ANXIETY, 2).on(BRAIN).chronic().remits(30000)
        .psy(PSY_ANXIETY, 4000).psy(PSY_MOOD, -2500).psy(PSY_SOCIAL, -2000).stress(4));
    static final Aff INSOMNIA = add(new Aff("Insomnia disorder", DISEASE)  // [src~] ~10% chronic insomnia
        .rate(1500).doubles(30, 40).ages(12, 120).women(1.4).risk(CH_STRESS, 4).risk(CH_PAIN, 3)
        .on(BRAIN).chronic().remits(15000).psy(PSY_ENERGY, -1500).psy(PSY_MOOD, -1500)
        .psy(PSY_COGNITION, -1500).stress(2).effects(QUIET));
    static final Aff AUD = add(new Aff("Alcohol use disorder", DISEASE)    // needs alcohol (Config.ALCOHOL)
        .rate(1500).men(3).ages(12, 120).needs(CH_ALCOHOL).gene(G_EXTERNAL, 4).risk(CH_STRESS, 3)
        .risk(CH_ACE, 3).after("Post-traumatic stress disorder", 3)
        .on(LIVER).dot(4).chronic().remits(4000).psy(PSY_IMPULSE, 3000).psy(PSY_AGGRESSION, 2500)
        .psy(PSY_COGNITION, -1500).psy(PSY_MOOD, -2000).leads("Alcohol withdrawal", 3000)
        .src("[src~] heritability ~50%; ~1/3 remit within a few years"));
    static final Aff WITHDRAWAL = add(new Aff("Alcohol withdrawal", NONE)  // delirium tremens
        .on(BRAIN).dot(50).lasts(3, 4).psy(PSY_REALITY, 5000).psy(PSY_ANXIETY, 6000).leads("Epilepsy", 50000));
    static final Aff DRUG = add(new Aff("Opioid use disorder", DISEASE)  // needs opium or similar (Config.OPIATES)
        .rate(500).men(2).ages(12, 120).needs(CH_OPIATES).gene(G_EXTERNAL, 4).risk(CH_PAIN, 4).risk(CH_ACE, 3)
        .on(BRAIN).chronic().remits(3000).fatal(1000).psy(PSY_IMPULSE, 3000).psy(PSY_ENERGY, -2000)
        .psy(PSY_MOOD, -2000).psy(PSY_SOCIAL, -2000).leads("Opioid withdrawal", 3000)
        .src("fatal(): overdose; [src~] untreated opioid dependence ~1-2% die per year (Degenhardt 2011)"));
    static final Aff ADHD = add(new Aff("ADHD", NEWBORN)               // [src] Polanczyk 2007: ~5.3%
        .rate(3000).men(2.5).gene(G_NEURODEV, 5).risk(CH_M_ALCOHOL, 2).after("Premature birth", 2)
        .on(BRAIN).chronic().remits(4000).psy(PSY_IMPULSE, 4000).psy(PSY_COGNITION, -1000)
        .psy(PSY_ENERGY, 1500).psy(PSY_AGGRESSION, 500).effects(QUIET));
    static final Aff AUTISM = add(new Aff("Autism", NEWBORN)           // [src] Zeidan 2022: ~1%; boys ~4x; heritability ~80%
        .rate(400).men(4).gene(G_NEURODEV, 10).on(BRAIN).chronic().sev(2000, 10000)
        .psy(PSY_SOCIAL, -5000).psy(PSY_ANXIETY, 2000).psy(PSY_COGNITION, -1500).effects(QUIET));
    static final Aff TOURETTE = add(new Aff("Tourette syndrome", NEWBORN)
        .rate(300).men(3).gene(G_NEURODEV, 4).on(BRAIN).chronic().remits(3000)
        .psy(PSY_IMPULSE, 1000).psy(PSY_SOCIAL, -1000).effects(QUIET));
    static final Aff CONDUCT = add(new Aff("Conduct disorder", DISEASE)
        .rate(400).men(3).ages(6, 17).gene(G_EXTERNAL, 3).risk(CH_ACE, 4).after("ADHD", 3)
        .on(BRAIN).lasts(1095, 1095).psy(PSY_AGGRESSION, 4000).psy(PSY_IMPULSE, 3500).psy(PSY_SOCIAL, -1000)
        .then("Antisocial personality disorder", 40000).src("[src~] ~40% go on to adult antisocial PD"));
    static final Aff ASPD = add(new Aff("Antisocial personality disorder", NONE)
        .on(BRAIN).chronic().remits(2000).psy(PSY_AGGRESSION, 4500).psy(PSY_IMPULSE, 4000)
        .psy(PSY_ANXIETY, -800));
    static final Aff BPD = add(new Aff("Borderline personality disorder", DISEASE)
        .rate(150).ages(13, 35).gene(G_MOOD, 2).risk(CH_ACE, 5).on(BRAIN).chronic().remits(8000)
        .psy(PSY_MOOD, -3000).psy(PSY_IMPULSE, 4500).psy(PSY_ANXIETY, 3000).psy(PSY_AGGRESSION, 1500)
        .src("[src] most remit over 10 years (Zanarini 2010)"));
    static final Aff ANOREXIA = add(new Aff("Anorexia nervosa", DISEASE)  // [src~] ~8/100k/yr
        .rate(40).women(1).men(0.1).peak(16, 4).gene(G_ANXIETY, 2).on(HEART).dot(3).chronic().remits(15000)
        .bmd(30).fert(20).psy(PSY_MOOD, -2500).psy(PSY_ANXIETY, 3000)
        .src("eating less needs the behaviour code to read this condition"));
    static final Aff BULIMIA = add(new Aff("Bulimia nervosa", DISEASE)
        .rate(40).women(1).men(0.12).peak(19, 4).gene(G_ANXIETY, 2).on(MOUTH).dot(1).alsoHit(STOMACH)
        .chronic().remits(15000).psy(PSY_MOOD, -2000).psy(PSY_ANXIETY, 2000).psy(PSY_IMPULSE, 1500));
    static final Aff ALZ = add(new Aff("Alzheimer's disease", DISEASE)  // [src] incidence doubles ~every 5.9 yr (Jorm & Jolley 1998)
        .rate(1000).doubles(5.9, 80).ages(45, 120).women(1.2).gene(G_DEMENTIA, 4).risk(CH_HEADINJ, 2)
        .risk(CH_BP, 1.5).risk(CH_GLU, 1.5).after("Down syndrome", 20).after("Deafness", 2)
        .on(BRAIN).dot(8).chronic().progress(1500).sev(1000, 2500)
        .psy(PSY_COGNITION, -8000).psy(PSY_SOCIAL, -3000).psy(PSY_MOOD, -1500).psy(PSY_AGGRESSION, 1500)
        .psy(PSY_REALITY, 2500).psy(PSY_ANXIETY, 1500).fn(FN_SPEECH, 3000)
        .src("[src~] median survival 4-8 yr after diagnosis"));
    static final Aff VASC_DEM = add(new Aff("Vascular dementia", DISEASE)
        .rate(250).doubles(5.3, 80).ages(45, 120).after("Ischaemic stroke", 10).after("Intracerebral haemorrhage", 8).risk(CH_BP, 3).risk(CH_GLU, 2)
        .on(BRAIN).dot(6).chronic().progress(1200).sev(1500, 3000)
        .psy(PSY_COGNITION, -7000).psy(PSY_MOOD, -2000).psy(PSY_ENERGY, -2000));
    static final Aff CFS = add(new Aff("Chronic fatigue syndrome", DISEASE)
        .rate(20).women(3).ages(15, 60).on(BRAIN).chronic().remits(5000).psy(PSY_ENERGY, -5000).psy(PSY_MOOD, -1500));

    // ---- emergent labels (given by code; checked monthly) ----------------------------
    static { section(C_LABEL); }
    static final Aff ATHERO = add(new Aff("Hardened arteries", NONE).chronic().fn(FN_STAMINA, 1000)); // vessels < 60%
    static final Aff ANAEMIA = add(new Aff("Anaemia", NONE).lasts(30, 0).effects(QUIET));          // Hb < 75%
    static final Aff SARCOPENIA = add(new Aff("Muscle wasting", NONE).chronic());                  // limb muscles < 50%
    static final Aff OA_LABEL = add(new Aff("Worn joints", NONE).chronic().effects(QUIET));        // any big joint < 50%

    // v0.4: the rest of the GBD 2021 cause list and the DSM-5-TR (see Coverage.java)
    static { CatalogueGbd.register(); CatalogueDsm.register(); }

    static { Afflictions.build(); }
}
