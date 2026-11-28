package meatbox;

import static meatbox.Afflictions.*;
import static meatbox.Body.*;
import static meatbox.Bones.*;
import static meatbox.Health.*;
import static meatbox.Genes.*;
import static meatbox.World.*;

/**
 * =============================================================================
 *  CATALOGUE (GBD)  -  physical conditions added in v0.4 so that every cause in
 *  the Global Burden of Disease 2021 cause list has a place in the sim.
 * =============================================================================
 *  Grammar: see Afflictions.java. Coverage.java says which GBD cause each entry
 *  stands for. Rates are per 100,000 per year (per 100,000 births for NEWBORN
 *  and BIRTH) for a population WITHOUT modern medicine; where only a modern or
 *  global figure exists it says so. Many need an exposure that is off in the
 *  default village (Config.ENV): they cost nothing until a world turns it on.
 *
 *  [src] figure from the named source   [src~] recalled from it, not re-checked
 *  [tune] set to make the sim behave    [guess] no source
 */
final class CatalogueGbd {
    private CatalogueGbd() {}
    static void register() {}             // touching the class runs everything below

    // =================================================================================
    //  INFECTIOUS (GBD A.1 - A.5)
    // =================================================================================
    static { section(C_INFECT); }
    // ---- HIV and sexually transmitted infections (A.1) ------------------------------
    static final Aff HIV = add(new Aff("HIV infection", DISEASE)       // needs HIV in the population
        .rate(300).ages(14, 70).needs(CH_HIV).needs(CH_SEXACTIVE).on(LYMPH).dot(1).chronic()
        .progress(1000).sev(500, 1500).immuno(85).spreads(1).leads("AIDS", 1500).fert(80)
        .src("[src] untreated median ~10-11 yr from infection to death (CASCADE 2000); CD4 loss = immuno()"));
    static final Aff AIDS = add(new Aff("AIDS", NONE)
        .on(LYMPH).dot(5).alsoHit(CORE_M, L_LEG_M, R_LEG_M).chronic().immuno(95).fatal(40000)
        .psy(PSY_ENERGY, -5000).psy(PSY_COGNITION, -2000).fn(FN_STAMINA, 4000)
        .src("[src~] untreated survival after AIDS ~1-2 yr; fatal() = opportunistic infections not modelled one by one"));
    static final Aff HIV_CHILD = add(new Aff("HIV infection (from mother)", NEWBORN)
        .rate(30000).needs(CH_M_HIV).on(LYMPH).dot(2).chronic().progress(4000).sev(1000, 2000).immuno(90)
        .fatal(30000).src("[src] 15-45% transmission without prevention (WHO); ~half die by age 2 untreated (Newell 2004)"));
    static final Aff CHLAMYDIA = add(new Aff("Chlamydia", DISEASE)     // [src~] WHO ~130M new cases/yr
        .rate(1500).ages(14, 50).needs(CH_SEXACTIVE).on(BLADDER).dot(2).lasts(180, 365).spreads(2)
        .onset("Pelvic inflammatory disease", 10000).fert(85).effects(QUIET)
        .src("[src~] mostly silent; ~10% of untreated women get PID (Oakeshott 2010)"));
    static final Aff TRICHOMONAS = add(new Aff("Trichomoniasis", DISEASE)  // [src~] WHO ~156M new cases/yr
        .rate(2000).women(2).ages(14, 60).needs(CH_SEXACTIVE).on(BLADDER).dot(1).lasts(60, 300).pain(5)
        .spreads(2).effects(QUIET));
    static final Aff HERPES_G = add(new Aff("Genital herpes", DISEASE) // HSV-2: lifelong, flares
        .rate(500).women(1.5).ages(14, 60).needs(CH_SEXACTIVE).on(SKIN).chronic().episodes(5, 7, 60, 300)
        .pain(25).spreads(1).psy(PSY_MOOD, -500).psy(PSY_SOCIAL, -500).effects(QUIET)
        .src("[src~] WHO: ~13% of 15-49 year olds carry HSV-2"));
    static final Aff CHANCROID = add(new Aff("Chancroid", DISEASE)     // GBD "other STIs"; LGV and donovanosis behave alike
        .rate(30).ages(14, 50).needs(CH_SEXACTIVE).on(SKIN).dot(5).alsoHit(LYMPH).lasts(30, 60).pain(40)
        .spreads(1).infects(20));

    // ---- respiratory and enteric infections (A.2, A.3) -------------------------------
    static final Aff COVID = add(new Aff("COVID-19", DISEASE).both()          // needs the virus (Config.ENV)
        .rate(20000).doubles(12, 50).needs(CH_SARS_COV_2).on(L_LUNG, R_LUNG).dot(30).alsoHit(VESSELS)
        .sepsis(40).lasts(7, 14).pain(15).spreads(30).psy(PSY_ENERGY, -4000).onset("Post-COVID condition", 8000)
        .src("[src~] infection fatality ~0.01% at 20 to ~5% at 80 pre-vaccine (Levin 2020): from age scaling of damage"));
    static final Aff LONG_COVID = add(new Aff("Post-COVID condition", NONE)
        .on(BRAIN).lasts(90, 270).psy(PSY_ENERGY, -3000).psy(PSY_COGNITION, -1500).fn(FN_STAMINA, 2000)
        .effects(QUIET));
    static final Aff PARATYPHOID = add(new Aff("Paratyphoid fever", DISEASE)  // milder cousin of typhoid
        .rate(100).needs(CH_WATER).on(SMALL_INT).dot(30).alsoHit(SPLEEN).sepsis(60).lasts(10, 14).pain(25)
        .effects(BEDRIDDEN).src("[src~] untreated CFR a few %"));
    static final Aff INTS = add(new Aff("Invasive salmonella", DISEASE)  // iNTS: bloodstream infection
        .rate(30).kids(5).needs(CH_WATER).risk(CH_IMMUNO, 6).risk(CH_MALNUT, 3).after("Malaria", 5)
        .after("Sickle cell disease", 5).after("HIV infection", 10).on(SPLEEN).sepsis(350).lasts(5, 10).pain(30)
        .effects(BEDRIDDEN).src("[src~] Africa: children with malaria/anaemia and adults with HIV; CFR ~15-20% even treated"));
    static final Aff AMOEBIASIS = add(new Aff("Amoebic dysentery", DISEASE)
        .rate(300).needs(CH_WATER).on(COLON).dot(40).bleeds(60).lasts(14, 28).pain(30).effects(FLUID)
        .leads("Amoebic liver abscess", 2000).src("[src~] ~1% of infections invasive; liver abscess the main killer"));
    static final Aff AMOEBIC_LIVER = add(new Aff("Amoebic liver abscess", NONE)
        .on(LIVER).dot(120).sepsis(200).lasts(14, 28).pain(60).effects(BEDRIDDEN));
    static final Aff GIARDIA = add(new Aff("Giardiasis", DISEASE)
        .rate(2000).kids(3).needs(CH_WATER).on(SMALL_INT).bleeds(20).lasts(14, 42).pain(10)
        .psy(PSY_ENERGY, -1000).effects(QUIET | FLUID).src("[src~] chronic malabsorption stunts children"));

    // ---- neglected tropical diseases and malaria (A.4) -------------------------------
    static final Aff CHAGAS = add(new Aff("Chagas disease", DISEASE)   // needs kissing bugs
        .rate(1000).kids(2).needs(CH_TRIATOMINE).on(HEART).chronic().effects(QUIET)
        .leads("Chagas heart disease", 1000).leads("Chagas megacolon", 300)
        .src("[src] ~30% develop heart disease decades later (Rassi 2010)"));
    static final Aff CHAGAS_HEART = add(new Aff("Chagas heart disease", NONE)
        .on(HEART).dot(3).chronic().progress(500).sev(2000, 4000).fn(FN_STAMINA, 3000).fatal(4000)
        .then("Heart failure", 30000).src("fatal(): sudden arrhythmic death, the commonest end [src~]"));
    static final Aff CHAGAS_COLON = add(new Aff("Chagas megacolon", NONE)
        .on(COLON).alsoHit(OESOPHAGUS).dot(1).chronic().pain(15).leads("Bowel obstruction", 3000).effects(QUIET));
    static final Aff KALA_AZAR = add(new Aff("Visceral leishmaniasis", DISEASE)  // kala-azar
        .rate(300).kids(2).needs(CH_SANDFLY).risk(CH_MALNUT, 3).after("HIV infection", 20)
        .on(SPLEEN).dot(30).alsoHit(LIVER, MARROW).chronic().immuno(50).blood(B2_HB, -3500).fatal(250000)
        .psy(PSY_ENERGY, -4000).src("[src] WHO: >95% fatal without treatment; fatal() = wasting and secondary infection"));
    static final Aff LEISH_CUT = add(new Aff("Cutaneous leishmaniasis", DISEASE)
        .rate(1500).needs(CH_SANDFLY).on(SKIN).dot(2).lasts(180, 365).pain(5).psy(PSY_SOCIAL, -1000)
        .onset("Mucocutaneous leishmaniasis", 3000).effects(IMMUNE | QUIET)
        .src("[src~] heals by itself in months, leaving scars that carry stigma (WHO)"));
    static final Aff LEISH_MUC = add(new Aff("Mucocutaneous leishmaniasis", NONE)
        .on(AIRWAY).dot(3).alsoHit(MOUTH).chronic().progress(500).sev(1000, 3000).pain(20)
        .psy(PSY_SOCIAL, -3000).psy(PSY_MOOD, -1500));
    static final Aff SLEEPING = add(new Aff("Sleeping sickness", DISEASE)  // African trypanosomiasis
        .rate(100).needs(CH_TSETSE).on(LYMPH).alsoHit(BRAIN).dot(8).chronic().progress(3000).sev(1000, 2000)
        .psy(PSY_ENERGY, -6000).psy(PSY_COGNITION, -4000).psy(PSY_REALITY, 3000).psy(PSY_AGGRESSION, 1500)
        .src("[src] WHO: almost always fatal without treatment; stage 2 = brain"));
    static final Aff SCHISTO = add(new Aff("Schistosomiasis", DISEASE) // needs snails in fresh water
        .rate(5000).kids(3).needs(CH_SNAILS).on(BLADDER, LIVER).dot(1).bleeds(5).chronic().remits(10000)
        .blood(B2_HB, -1000).leads("Bladder cancer", 50).psy(PSY_ENERGY, -800).effects(QUIET)
        .src("[src] IARC: S. haematobium causes bladder cancer; liver form -> portal hypertension"));
    static final Aff CYSTICERCOSIS = add(new Aff("Neurocysticercosis", DISEASE)  // pork tapeworm larvae in the brain
        .rate(100).needs(CH_PIGS).on(BRAIN).dot(1).chronic().remits(5000).pain(10).leads("Epilepsy", 5000)
        .src("[src] WHO: ~30% of epilepsy in endemic areas"));
    static final Aff HYDATID = add(new Aff("Hydatid disease", DISEASE) // cystic echinococcosis
        .rate(5).needs(CH_DOGS_SHEEP).on(LIVER, LIVER, LIVER, L_LUNG, R_LUNG).dot(2).chronic().progress(300)
        .sev(500, 1500).pain(15).fatal(300).src("[src~] 70% liver, 20% lung; cyst rupture can kill (fatal)"));
    static final Aff FILARIASIS = add(new Aff("Lymphatic filariasis", DISEASE)
        .rate(1000).needs(CH_CULEX).needs(CH_FILARIA).on(LYMPH).dot(1).chronic().leads("Elephantiasis", 1000)
        .leads("Hydrocele", 1500).effects(QUIET).src("[src] WHO: ~36M with chronic disfigurement"));
    static final Aff ELEPHANT = add(new Aff("Elephantiasis", NONE)
        .on(L_LEG_M, R_LEG_M).dot(1).chronic().pain(15).fn(FN_MOBILITY, 3000).infects(10)
        .psy(PSY_SOCIAL, -3000).psy(PSY_MOOD, -2000).src("[src] stigma and depression are common (WHO)"));
    static final Aff HYDROCELE = add(new Aff("Hydrocele", NONE)
        .menOnly().on(GONADS).chronic().pain(10).fert(70).psy(PSY_SOCIAL, -1500).fn(FN_MOBILITY, 500));
    static final Aff ONCHO = add(new Aff("River blindness", DISEASE)   // onchocerciasis
        .rate(3000).needs(CH_BLACKFLY).on(SKIN).dot(1).alsoHit(L_EYE, R_EYE).chronic().pain(20)
        .psy(PSY_ENERGY, -800).psy(PSY_MOOD, -800).leads("Blinding eye damage", 300)
        .src("[src] WHO: severe itching; second infectious cause of blindness after trachoma"));
    static final Aff DENGUE = add(new Aff("Dengue", DISEASE)           // needs Aedes mosquitoes
        .rate(5000).needs(CH_AEDES).on(VESSELS).dot(20).lasts(5, 3).pain(50).spreads(10)
        .psy(PSY_ENERGY, -5000).onset("Severe dengue", 5000).effects(BEDRIDDEN)
        .src("[src] WHO: ~1 in 20 get severe dengue; untreated severe CFR up to ~20%"));
    static final Aff DENGUE_S = add(new Aff("Severe dengue", NONE)     // plasma leak, bleeding, shock
        .on(VESSELS).dot(100).alsoHit(LIVER).bleeds(400).lasts(2, 3).pain(40).effects(BEDRIDDEN | FLUID));
    static final Aff YELLOW = add(new Aff("Yellow fever", DISEASE)
        .rate(200).needs(CH_AEDES).on(LIVER).dot(150).alsoHit(L_KIDNEY, R_KIDNEY).bleeds(80).lasts(7, 7).pain(40)
        .spreads(8).effects(BEDRIDDEN | IMMUNE).src("[src] WHO: ~15% enter the toxic phase; half of those die"));
    static final Aff WHIPWORM = add(new Aff("Whipworm infection", DISEASE)  // trichuriasis
        .rate(1500).kids(3).risk(CH_WATER, 3).on(COLON).bleeds(2).chronic().remits(50000)
        .blood(B2_HB, -300).psy(PSY_COGNITION, -300).effects(QUIET));
    static final Aff HOOKWORM = add(new Aff("Hookworm disease", DISEASE)   // blood-feeding: iron loss
        .rate(1500).kids(2).risk(CH_WATER, 3).on(SMALL_INT).chronic().remits(30000).blood(B2_HB, -2500)
        .psy(PSY_ENERGY, -1000).psy(PSY_COGNITION, -500).effects(QUIET)
        .src("[src] leading cause of iron-deficiency anaemia in the tropics (Hotez 2004)"));
    static final Aff FLUKE = add(new Aff("Liver fluke infection", DISEASE)  // food-borne trematodes
        .rate(1000).needs(CH_RAW_FISH).on(GALLBLADDER).dot(1).alsoHit(LIVER).chronic().pain(5)
        .leads("Gallbladder and bile duct cancer", 150).effects(QUIET)
        .src("[src] IARC group 1: Opisthorchis and Clonorchis cause bile duct cancer"));
    static final Aff EBOLA = add(new Aff("Ebola", DISEASE)             // needs a wildlife reservoir
        .rate(20).needs(CH_EBOLA).on(VESSELS).dot(200).alsoHit(LIVER, L_KIDNEY, R_KIDNEY).bleeds(500)
        .sepsis(250).lasts(7, 7).pain(50).spreads(30).effects(BEDRIDDEN | IMMUNE | FLUID)
        .src("[src] WHO: average CFR ~50% (25-90% across outbreaks)"));
    static final Aff ZIKA = add(new Aff("Zika", DISEASE)
        .rate(1000).needs(CH_AEDES).on(SKIN).dot(1).lasts(5, 3).spreads(5).onset("Guillain-Barre syndrome", 25)
        .effects(IMMUNE | QUIET).src("[src~] mostly mild; harms the fetus (see Congenital Zika syndrome)"));
    static final Aff GUINEA = add(new Aff("Guinea worm disease", DISEASE)
        .rate(500).needs(CH_COPEPODS).on(L_LEG_M, R_LEG_M).dot(8).lasts(30, 30).pain(60).fn(FN_MOBILITY, 5000)
        .infects(25).src("[src] worm emerges through the skin over weeks; disables farmers at harvest (Carter Center)"));
    static final Aff YAWS = add(new Aff("Yaws", DISEASE)               // GBD "other NTDs"
        .rate(300).kids(4).needs(CH_YAWS).spreads(8).on(SKIN).dot(2).chronic().remits(15000).pain(10)
        .leads("Chronic osteomyelitis", 100).effects(QUIET).src("[src] WHO: 10% of untreated go on to bone and skin destruction"));
    static final Aff TYPHUS = add(new Aff("Epidemic typhus", DISEASE)  // louse-borne: war, famine, crowding
        .rate(30).ages(5, 120).risk(CH_CROWDING, 5).risk(CH_MALNUT, 4).risk(CH_COLD, 3).on(VESSELS).dot(60)
        .alsoHit(BRAIN).sepsis(200).lasts(14, 7).pain(50).spreads(15).psy(PSY_REALITY, 3000).effects(BEDRIDDEN | IMMUNE)
        .src("[src~] untreated CFR 10-60%, highest in the old"));
    static final Aff LYME = add(new Aff("Lyme disease", DISEASE)
        .rate(30).needs(CH_TICKS).on(SKIN).dot(1).lasts(30, 30).pain(15).onset("Lyme arthritis", 30000)
        .onset("Bell's palsy", 5000).src("[src~] untreated ~60% get arthritis attacks (Steere 1987)"));
    static final Aff LYME_A = add(new Aff("Lyme arthritis", NONE)
        .on(L_KNEE, R_KNEE).dot(3).chronic().episodes(7, 14, 30, 120).remits(20000).pain(30).fn(FN_MOBILITY, 1500));
    static final Aff BRUCELLA = add(new Aff("Brucellosis", DISEASE)    // raw milk, livestock
        .rate(20).needs(CH_LIVESTOCK).on(SPINE_J).dot(2).alsoHit(LIVER, SPLEEN).lasts(60, 120).pain(30)
        .psy(PSY_ENERGY, -3000).psy(PSY_MOOD, -1500).src("[src~] undulant fever; spine and joints; CFR <2%"));
    static final Aff ANTHRAX = add(new Aff("Anthrax", DISEASE)         // skin form from hides and carcasses
        .rate(2).needs(CH_LIVESTOCK).on(SKIN).dot(20).sepsis(80).lasts(10, 10).pain(10)
        .src("[src~] cutaneous: untreated CFR ~20%"));
    static final Aff LEPTO = add(new Aff("Leptospirosis", DISEASE).both()     // rat urine in water
        .rate(20).needs(CH_RODENTS).risk(CH_WATER, 3).on(L_KIDNEY, R_KIDNEY).dot(40).alsoHit(LIVER).sepsis(50)
        .lasts(7, 7).pain(40).effects(BEDRIDDEN).src("[src~] severe (Weil's) CFR 5-15%"));
    static final Aff ENCEPH = add(new Aff("Encephalitis", DISEASE)     // viral: Japanese, tick-borne, herpes
        .rate(7).kids(2).risk(CH_CULEX, 4).risk(CH_TICKS, 3).on(BRAIN).dot(250).lasts(7, 14).pain(40)
        .psy(PSY_REALITY, 4000).psy(PSY_COGNITION, -5000).leads("Epilepsy", 10000).effects(BEDRIDDEN)
        .src("[src~] 5-10/100k/yr; CFR 10-30%, many survivors left with brain damage"));
    static final Aff HEPC = add(new Aff("Hepatitis C", DISEASE)        // blood contact
        .rate(30).ages(10, 120).needs(CH_BLOOD_BORNE).on(LIVER).dot(10).lasts(30, 60)
        .then("Chronic hepatitis C", 75000).effects(QUIET).src("[src] WHO: ~70% become chronic"));
    static final Aff HEPC_C = add(new Aff("Chronic hepatitis C", NONE)
        .on(LIVER).dot(1).chronic().leads("Cirrhosis", 1000).leads("Liver cancer", 100).psy(PSY_ENERGY, -800)
        .effects(QUIET).src("[src] ~15-30% cirrhosis within 20 years (WHO)"));
    static final Aff HEPE = add(new Aff("Hepatitis E", DISEASE)
        .rate(300).ages(10, 120).needs(CH_WATER).on(LIVER).dot(50).lasts(14, 21).psy(PSY_ENERGY, -3000)
        .onset("Acute liver failure", 1000).effects(IMMUNE)
        .src("[src] WHO: CFR ~1%, but 20-25% in the third trimester (not yet modelled by trimester)"));
    static final Aff LIVER_FAIL = add(new Aff("Acute liver failure", NONE)
        .on(LIVER).hit(6000).dot(300).lasts(3, 7).psy(PSY_COGNITION, -5000).psy(PSY_REALITY, 3000).effects(BEDRIDDEN));

    // =================================================================================
    //  MATERNAL AND NEWBORN (GBD A.6)
    // =================================================================================
    static { section(C_REPRO); }
    static final Aff HYPEREMESIS = add(new Aff("Hyperemesis gravidarum", DISEASE)  // [src~] 0.3-2% of pregnancies
        .rate(1500).needs(CH_EARLYPREG).on(STOMACH).bleeds(60).lasts(14, 28).psy(PSY_ENERGY, -4000)
        .psy(PSY_MOOD, -2000).effects(BEDRIDDEN | FLUID));
    static final Aff UT_RUPTURE = add(new Aff("Uterine rupture", BIRTH)  // [src~] ~1/1000 unscarred; mostly after obstruction
        .rate(50).after("Obstructed labour", 50).on(UTERUS).hit(9000).bleeds(2500).lasts(1, 2).pain(90)
        .effects(STILLBORN | BEDRIDDEN));
    static final Aff AFE = add(new Aff("Amniotic fluid embolism", BIRTH)  // [src~] 2-8/100k births; CFR ~60% even in hospital
        .rate(5).on(L_LUNG).hit(6000).alsoHit(R_LUNG, HEART).bleeds(800).lasts(1, 1).caseFatal(60000).effects(BEDRIDDEN));
    static final Aff JAUNDICE = add(new Aff("Severe newborn jaundice", NEWBORN)  // kernicterus risk
        .rate(500).risk(CH_G6PD, 10).after("Premature birth", 3).on(BRAIN).dot(60).lasts(5, 5)
        .then("Cerebral palsy", 10000).src("[src~] ~1% of births worldwide; G6PD deficiency a leading cause"));
    static final Aff RH_DISEASE = add(new Aff("Rhesus disease of the newborn", NEWBORN)  // GBD A.6.2.4
        .rate(200).on(MARROW).blood(B2_HB, -4000).alsoHit(LIVER).dot(30).lasts(14, 14).then("Severe newborn jaundice", 50000)
        .src("[src~] ~1% of births before anti-D; Rh- mothers ~15% in Europe"));
    static final Aff BIRTH_INJ = add(new Aff("Birth injury (brachial plexus)", NEWBORN)  // GBD "other neonatal"
        .rate(150).after("Obstructed labour", 5).on(NERVES).alsoHit(L_ARM_M).hit(3000).lasts(90, 180)
        .permanent(20000, L_ARM_M).effects(QUIET).src("[src~] ~1.5/1000 births; most recover"));
    static final Aff CONG_ZIKA = add(new Aff("Congenital Zika syndrome", NEWBORN)
        .rate(5000).needs(CH_M_ZIKA).on(BRAIN).hit(5000).chronic().psy(PSY_COGNITION, -7000)
        .fn(FN_MOBILITY, 4000).leads("Epilepsy", 30000).src("[src~] ~5-10% of pregnancies with Zika; microcephaly"));

    // =================================================================================
    //  CANCERS (GBD B.1)  rates per 100k at the reference age (GLOBOCAN 2020 [src~])
    // =================================================================================
    static { section(C_CANCER); }
    static final Aff ORAL_CA = add(new Aff("Mouth cancer", DISEASE)
        .rate(5).doubles(10, 60).ages(30, 120).men(2).risk(CH_TOBACCO, 5).after("Alcohol use disorder", 3)
        .on(MOUTH).chronic().progress(2500).sev(300, 1000).dot(40).pain(30).fn(FN_SPEECH, 2000));
    static final Aff NASOPH_CA = add(new Aff("Nasopharynx cancer", DISEASE)
        .rate(2).ages(20, 120).peak(50, 15).men(2.5).risk(CH_SMOKE, 2).on(AIRWAY).chronic().progress(2000)
        .sev(300, 1000).dot(30).alsoHit(L_EAR).pain(20).src("[src~] Epstein-Barr virus, salted fish; S. China"));
    static final Aff PHARYNX_CA = add(new Aff("Throat cancer", DISEASE)
        .rate(2).doubles(10, 60).ages(30, 120).men(4).risk(CH_TOBACCO, 6).after("Alcohol use disorder", 4)
        .on(AIRWAY).chronic().progress(3000).sev(300, 1000).dot(40).alsoHit(OESOPHAGUS).pain(30));
    static final Aff LARYNX_CA = add(new Aff("Larynx cancer", DISEASE)
        .rate(3).doubles(10, 60).ages(30, 120).men(6).risk(CH_TOBACCO, 10).after("Alcohol use disorder", 2)
        .on(AIRWAY).chronic().progress(2000).sev(300, 1000).dot(40).fn(FN_SPEECH, 5000).pain(15));
    static final Aff HEPATOBLASTOMA = add(new Aff("Hepatoblastoma", DISEASE)
        .rate(0.4).ages(0, 4).after("Premature birth", 10).on(LIVER).chronic().progress(4000).sev(300, 1000).dot(80)
        .src("[src~] ~1.5 per million under-15s, mostly under 3; rate given is for ages 0-4"));
    static final Aff BILE_CA = add(new Aff("Gallbladder and bile duct cancer", DISEASE)
        .rate(3).doubles(9, 65).ages(30, 120).women(1.5).after("Gallstones", 5).after("Liver fluke infection", 15)
        .on(GALLBLADDER).alsoHit(LIVER).chronic().progress(4000).sev(300, 1000).dot(70).pain(30));
    static final Aff BCC = add(new Aff("Basal cell carcinoma", DISEASE)  // the commonest cancer; rarely spreads
        .rate(100).doubles(10, 65).ages(30, 120).risk(CH_SUN, 5).after("Albinism", 50).on(SKIN).chronic()
        .progress(100).sev(300, 1000).dot(1).psy(PSY_SOCIAL, -300).effects(QUIET));
    static final Aff SARCOMA = add(new Aff("Soft tissue sarcoma", DISEASE)
        .rate(3).doubles(20, 55).on(L_LEG_M, R_LEG_M, L_ARM_M, R_ARM_M, CORE_M).chronic().progress(2000)
        .sev(300, 1000).dot(60).pain(25).leads("Lung metastases", 15000));
    static final Aff RETINOBLASTOMA = add(new Aff("Retinoblastoma", DISEASE)
        .rate(1.2).ages(0, 5).on(L_EYE, R_EYE).chronic().progress(3000).sev(300, 1000).dot(80).leads("Brain metastases", 20000)
        .src("[src] ~1 in 15-20,000 births; untreated almost always fatal; rate given is for ages 0-5"));
    static final Aff UVEAL = add(new Aff("Eye melanoma", DISEASE)      // GBD "other eye cancers"
        .rate(1).doubles(15, 60).ages(20, 120).on(L_EYE, R_EYE).chronic().progress(1500).sev(300, 1000).dot(30)
        .leads("Liver metastases", 10000));
    static final Aff NEUROBLASTOMA = add(new Aff("Neuroblastoma", DISEASE)
        .rate(2.5).ages(0, 6).on(ADRENALS).chronic().progress(3000).sev(300, 1000).dot(60).leads("Bone metastases", 20000)
        .src("[src~] ~1 in 7,000 children; rate given is for ages 0-6"));
    static final Aff MESOTHELIOMA = add(new Aff("Mesothelioma", DISEASE)  // needs asbestos
        .rate(30).ages(40, 120).needs(CH_ASBESTOS).on(L_LUNG, R_LUNG).chronic().progress(5000).sev(300, 1000).dot(80)
        .pain(40).src("[src] median survival under a year untreated"));
    static final Aff HODGKIN = add(new Aff("Hodgkin lymphoma", DISEASE)
        .rate(3).peak(25, 10).on(LYMPH).alsoHit(SPLEEN).chronic().progress(1500).sev(300, 1000).dot(40).immuno(30)
        .psy(PSY_ENERGY, -2500).src("[src~] untreated 5-year survival <10% (pre-1960s series)"));
    static final Aff BURKITT = add(new Aff("Burkitt lymphoma", DISEASE)  // jaw tumour of malaria belt children
        .rate(0.1).kids(10).ages(0, 30).after("Malaria", 50).after("HIV infection", 20).on(LYMPH).alsoHit(MOUTH)
        .chronic().progress(8000).sev(300, 1000).dot(80).deform(FACE, 40)
        .src("[src] fastest-growing human tumour; malaria + Epstein-Barr virus"));
    static final Aff CLL = add(new Aff("Chronic lymphoid leukaemia", DISEASE)
        .rate(4).doubles(8, 70).ages(40, 120).men(1.7).gene(G_CANCER, 3).on(MARROW).alsoHit(LYMPH).chronic()
        .progress(500).sev(300, 1000).dot(10).immuno(40).psy(PSY_ENERGY, -1500)
        .src("[src~] slow: many live 10+ years; infections are the usual end"));
    static final Aff CML = add(new Aff("Chronic myeloid leukaemia", DISEASE)
        .rate(1.5).doubles(15, 55).ages(15, 120).on(MARROW).alsoHit(SPLEEN).chronic().progress(2000).sev(300, 1000)
        .dot(30).blood(B2_HB, -1500).src("[src~] chronic phase ~3-5 yr, then blast crisis"));
    static final Aff MDS = add(new Aff("Myelodysplastic syndrome", DISEASE)  // GBD B.1.34.1
        .rate(4).doubles(8, 70).ages(40, 120).on(MARROW).chronic().progress(800).sev(500, 1500).dot(10)
        .blood(B2_HB, -2500).immuno(25).leads("Acute myeloid leukaemia", 3000));
    static final Aff PV = add(new Aff("Polycythaemia vera", DISEASE)  // myeloproliferative
        .rate(1).doubles(12, 60).ages(30, 120).on(MARROW).chronic().blood(B2_HB, 2500).blood(B2_BP, 10)
        .leads("Ischaemic stroke", 2000).leads("Deep vein thrombosis", 3000).effects(QUIET));
    static final Aff OTHER_CA = add(new Aff("Cancer of other site", DISEASE)  // GBD "other malignant neoplasms"
        .rate(10).doubles(9, 65).ages(20, 120).gene(G_CANCER, 2).on(ORGANS).chronic().progress(3000).sev(300, 1000)
        .dot(60).pain(20).leads("Liver metastases", 8000).src("[src~] ~5% of cancers: small intestine, vulva, penis, adrenal..."));
    static final Aff POLYPS = add(new Aff("Bowel polyps", DISEASE)    // benign and in situ intestinal neoplasms
        .rate(300).doubles(12, 55).ages(30, 120).men(1.5).gene(G_CANCER, 2).on(COLON).chronic().bleeds(1)
        .leads("Bowel cancer", 250).effects(QUIET).src("[src~] ~0.25%/yr of adenomas become cancer"));
    static final Aff MENINGIOMA = add(new Aff("Meningioma", DISEASE)  // GBD "other benign neoplasms"
        .rate(8).doubles(15, 60).ages(20, 120).women(2).on(BRAIN).chronic().progress(200).sev(500, 1500).dot(2)
        .pain(10).leads("Epilepsy", 5000).psy(PSY_COGNITION, -1000));
    static final Aff LIPOMA = add(new Aff("Benign skin and soft tissue lump", DISEASE)
        .rate(500).ages(20, 120).on(SKIN).chronic().effects(QUIET));

    // =================================================================================
    //  HEART & VESSELS (GBD B.2)
    // =================================================================================
    static { section(C_CARDIO); }
    static final Aff ANGINA = add(new Aff("Angina", DISEASE)          // stable ischaemic heart disease
        .rate(150).doubles(9, 60).ages(30, 120).men(1.7).gene(G_CARDIO, 3).risk(CH_ATHERO, 8).risk(CH_TOBACCO, 3)
        .risk(CH_GLU, 2).on(HEART).chronic().episodes(1, 2, 7, 21).pain(30).fn(FN_STAMINA, 3000)
        .leads("Heart attack", 2000).src("[src~] Framingham: ~3%/yr of men with angina have an MI"));
    static final Aff ICH = add(new Aff("Intracerebral haemorrhage", DISEASE)
        .rate(25).doubles(9, 65).ages(20, 120).risk(CH_BP, 8).after("Alcohol use disorder", 2).after("Cirrhosis", 2)
        .on(BRAIN).hit(5000).dot(250).bleeds(100).lasts(3, 7).pain(40).caseFatal(30000).permanent(50000, ANY_LIMB)
        .fn(FN_SPEECH, 4000).effects(BEDRIDDEN).then("Major depressive disorder", 30000)
        .src("[src] ~40% dead within a month (van Asch 2010); the commonest stroke in Asia and Africa"));
    static final Aff SAH = add(new Aff("Subarachnoid haemorrhage", DISEASE)  // burst aneurysm
        .rate(8).peak(55, 15).women(1.6).risk(CH_BP, 3).risk(CH_TOBACCO, 2).on(BRAIN).hit(4000).dot(200).lasts(7, 14)
        .pain(95).caseFatal(30000).effects(BEDRIDDEN).src("[src] ~1/3 die before reaching care; ~50% at a month"));
    static final Aff HHD = add(new Aff("Hypertensive heart disease", NONE)  // thick, stiff heart from pressure
        .on(HEART).dot(1).chronic().progress(300).sev(1000, 3000).fn(FN_STAMINA, 2500).leads("Atrial fibrillation", 3000)
        .then("Heart failure", 10000));
    static final Aff AORTIC_ST = add(new Aff("Aortic valve stenosis", DISEASE)  // calcific
        .rate(40).doubles(8, 70).ages(50, 120).men(1.5).risk(CH_ATHERO, 2).on(HEART).dot(1).chronic().progress(400)
        .sev(1000, 3000).fn(FN_STAMINA, 3000).fatal(3000).leads("Heart failure", 3000)
        .src("[src] Ross & Braunwald 1968: after symptoms, ~2-3 yr; sudden death common (fatal)"));
    static final Aff MITRAL = add(new Aff("Mitral valve prolapse and regurgitation", DISEASE)  // degenerative
        .rate(30).doubles(12, 60).ages(30, 120).on(HEART).chronic().progress(200).sev(500, 1500).fn(FN_STAMINA, 2000)
        .leads("Atrial fibrillation", 3000).leads("Heart failure", 1000).effects(QUIET));
    static final Aff VALVE_OTHER = add(new Aff("Other heart valve disease", DISEASE)
        .rate(5).doubles(15, 60).on(HEART).chronic().fn(FN_STAMINA, 1500).leads("Heart failure", 1000).effects(QUIET));
    static final Aff MYOCARDITIS = add(new Aff("Myocarditis", DISEASE)  // mostly after viral infections
        .rate(10).after("Influenza", 20).after("Diphtheria", 50).after("COVID-19", 20).after("Chagas disease", 5)
        .on(HEART).dot(60).lasts(14, 30).pain(20).fn(FN_STAMINA, 4000).fatal(20000).then("Cardiomyopathy", 15000)
        .src("[src~] a leading cause of sudden death in the young; ~1/5 progress to dilated cardiomyopathy"));
    static final Aff ALC_CMP = add(new Aff("Alcoholic cardiomyopathy", DISEASE)
        .rate(200).ages(25, 120).men(4).requires("Alcohol use disorder").on(HEART).dot(2).chronic().progress(300)
        .sev(1000, 3000).fn(FN_STAMINA, 3000).fatal(2000).then("Heart failure", 20000));
    static final Aff PAH = add(new Aff("Pulmonary arterial hypertension", DISEASE)
        .rate(1).women(3).ages(15, 120).after("Schistosomiasis", 20).after("Sickle cell disease", 30).after("HIV infection", 10)
        .on(HEART).dot(2).alsoHit(L_LUNG, R_LUNG).chronic().progress(1000).sev(1000, 3000).fn(FN_STAMINA, 6000)
        .fatal(5000).src("[src] NIH registry 1991: untreated median survival 2.8 yr"));
    static final Aff PAD = add(new Aff("Peripheral artery disease", DISEASE).both()
        .rate(100).doubles(10, 65).ages(40, 120).risk(CH_ATHERO, 8).risk(CH_TOBACCO, 4).risk(CH_GLU, 3)
        .on(L_LEG_M, R_LEG_M).dot(1).chronic().pain(15).fn(FN_MOBILITY, 2500).leads("Gangrene", 300)
        .src("[src~] ~1% a year progress to critical limb ischaemia"));
    static final Aff GANGRENE = add(new Aff("Gangrene", NONE)
        .on(L_LEG_M, R_LEG_M).hit(5000).dot(150).sepsis(200).lasts(14, 28).pain(70).permanent(50000, SAME)
        .effects(BEDRIDDEN));
    static final Aff ENDOCARDITIS = add(new Aff("Endocarditis", DISEASE)
        .rate(5).doubles(20, 60).after("Rheumatic heart disease", 20).after("Aortic valve stenosis", 5)
        .after("Congenital heart defect", 10).risk(CH_BLOOD_BORNE, 3).on(HEART).dot(40).sepsis(120).chronic()
        .fatal(200000).leads("Ischaemic stroke", 20000).psy(PSY_ENERGY, -4000)
        .src("[src] Osler: untreated almost always fatal within months"));
    static final Aff RAYNAUD = add(new Aff("Raynaud's phenomenon", DISEASE)  // GBD "other cardiovascular"
        .rate(150).women(4).ages(15, 60).risk(CH_COLD, 3).on(VESSELS).chronic().episodes(1, 1, 7, 30).pain(10)
        .fn(FN_MANIP, 1000).effects(QUIET));
    static final Aff VENOUS_ULCER = add(new Aff("Leg ulcer", DISEASE)
        .rate(80).doubles(10, 70).ages(40, 120).after("Varicose veins", 10).after("Deep vein thrombosis", 10)
        .after("Peripheral artery disease", 5).on(SKIN).dot(5).lasts(90, 270).pain(25).infects(40));

    // =================================================================================
    //  LUNGS (GBD B.3)
    // =================================================================================
    static { section(C_RESP); }
    static final Aff SILICOSIS = add(new Aff("Silicosis", DISEASE).both()
        .rate(500).ages(20, 120).needs(CH_SILICA).on(L_LUNG, R_LUNG).dot(1).chronic().progress(300).sev(1000, 3000)
        .fn(FN_STAMINA, 4000).leads("Tuberculosis", 500).src("[src] silica multiplies TB risk ~3x"));
    static final Aff ASBESTOSIS = add(new Aff("Asbestosis", DISEASE).both()
        .rate(300).ages(25, 120).needs(CH_ASBESTOS).on(L_LUNG, R_LUNG).dot(1).chronic().progress(300).sev(1000, 3000)
        .fn(FN_STAMINA, 4000).leads("Lung cancer", 300));
    static final Aff BLACK_LUNG = add(new Aff("Coal workers' lung disease", DISEASE).both()
        .rate(300).ages(20, 120).needs(CH_COAL_DUST).on(L_LUNG, R_LUNG).dot(1).chronic().progress(200).sev(1000, 3000)
        .fn(FN_STAMINA, 3000));
    static final Aff FARMERS_LUNG = add(new Aff("Farmer's lung", DISEASE).both()  // mouldy hay: other pneumoconiosis / ILD
        .rate(10).ages(15, 120).needs(CH_LIVESTOCK).risk(CH_LABOUR, 3).on(L_LUNG, R_LUNG).dot(2).chronic()
        .remits(10000).fn(FN_STAMINA, 2000).effects(QUIET));
    static final Aff IPF = add(new Aff("Pulmonary fibrosis", DISEASE).both()
        .rate(10).doubles(8, 70).ages(45, 120).men(2).risk(CH_SMOKE, 2).risk(CH_TOBACCO, 2).on(L_LUNG, R_LUNG).dot(3)
        .chronic().progress(1000).sev(1000, 3000).fn(FN_STAMINA, 5000).src("[src] median survival 3-5 yr"));
    static final Aff SARCOID = add(new Aff("Sarcoidosis", DISEASE).both()
        .rate(5).peak(40, 12).women(1.3).gene(G_AUTOIMM, 2).on(L_LUNG, R_LUNG).dot(1).alsoHit(LYMPH, L_EYE).chronic()
        .remits(30000).fn(FN_STAMINA, 1500).psy(PSY_ENERGY, -1500).src("[src~] 2/3 remit within a decade"));
    static final Aff BRONCHIECTASIS = add(new Aff("Bronchiectasis", DISEASE).both()
        .rate(10).after("Whooping cough", 10).after("Tuberculosis", 10).after("Measles", 5).after("Cystic fibrosis", 50)
        .on(L_LUNG, R_LUNG).dot(1).chronic().leads("Pneumonia", 20000).fn(FN_STAMINA, 2000));
    static final Aff SINUSITIS = add(new Aff("Chronic sinusitis", DISEASE)
        .rate(500).ages(10, 120).risk(CH_SMOKE, 2).on(AIRWAY).chronic().remits(10000).pain(5).psy(PSY_ENERGY, -300)
        .effects(QUIET));
    static final Aff HAYFEVER = add(new Aff("Hay fever", DISEASE)     // allergic rhinitis
        .rate(800).peak(14, 8).gene(G_AUTOIMM, 2).on(AIRWAY).chronic().episodes(30, 30, 300, 30).remits(5000)
        .psy(PSY_ENERGY, -500).effects(QUIET));

    // =================================================================================
    //  DIGESTIVE (GBD B.4)
    // =================================================================================
    static { section(C_DIGEST); }
    static final Aff NAFLD = add(new Aff("Fatty liver disease", DISEASE)  // non-alcoholic
        .rate(1000).ages(15, 120).needs(CH_OBESE, 3).gene(G_METAB, 2).on(LIVER).chronic().dot(1)
        .leads("Cirrhosis", 100).leads("Liver cancer", 20).effects(QUIET).src("needs Config.OBESITY > 0"));
    static final Aff GASTRITIS = add(new Aff("Gastritis", DISEASE)
        .rate(1000).ages(10, 120).after("Alcohol use disorder", 3).risk(CH_STRESS, 1.5).on(STOMACH).dot(2)
        .lasts(7, 30).pain(15).effects(QUIET));
    static final Aff GERD = add(new Aff("Acid reflux disease", DISEASE)  // GORD
        .rate(800).ages(18, 120).risk(CH_OBESE, 2).risk(CH_PREGNANT, 3).on(OESOPHAGUS).dot(1).chronic().remits(10000)
        .pain(10).leads("Oesophageal cancer", 20).psy(PSY_ENERGY, -300).effects(QUIET));
    static final Aff FEMORAL_H = add(new Aff("Femoral hernia", DISEASE)
        .rate(20).women(4).ages(30, 120).on(SKIN).chronic().pain(5).leads("Strangulated hernia", 3000).effects(QUIET)
        .src("[src~] ~3-4% of groin hernias but the most likely to strangulate"));
    static final Aff UC = add(new Aff("Ulcerative colitis", DISEASE)
        .rate(10).peak(30, 12).gene(G_AUTOIMM, 3).on(COLON).chronic().episodes(30, 60, 180, 540)
        .dot(30).bleeds(80).pain(35).psy(PSY_ENERGY, -3000).psy(PSY_MOOD, -1500).bmd(5)
        .src("[src~] untreated severe attacks killed ~1/4 before steroids (Truelove 1955)"));
    static final Aff MESENTERIC = add(new Aff("Bowel ischaemia", DISEASE)  // vascular intestinal disorders
        .rate(10).doubles(8, 70).ages(40, 120).after("Atrial fibrillation", 5).risk(CH_ATHERO, 4).on(SMALL_INT)
        .hit(5000).dot(300).sepsis(250).lasts(2, 4).pain(90).effects(BEDRIDDEN)
        .src("[src~] acute mesenteric ischaemia ~60-80% fatal even with surgery"));
    static final Aff ANAL_FISSURE = add(new Aff("Anal fissure", DISEASE)  // GBD "other digestive"
        .rate(100).ages(15, 70).on(COLON).bleeds(2).lasts(14, 42).pain(25).effects(QUIET));

    // =================================================================================
    //  NEUROLOGICAL (GBD B.5)
    // =================================================================================
    static { section(C_NEURO); }
    static final Aff TENSION_HA = add(new Aff("Tension-type headache", DISEASE)  // [src] GBD: commonest disorder
        .rate(3000).women(1.3).ages(10, 120).on(BRAIN).chronic().episodes(1, 1, 5, 20).remits(10000).pain(15)
        .risk(CH_STRESS, 2).psy(PSY_ENERGY, -1000).effects(QUIET));
    static final Aff LEWY = add(new Aff("Dementia with Lewy bodies", DISEASE)
        .rate(80).doubles(6, 80).ages(50, 120).men(1.4).on(BRAIN).dot(6).chronic().progress(1800).sev(1000, 2500)
        .psy(PSY_COGNITION, -7000).psy(PSY_REALITY, 5000).fn(FN_MOBILITY, 3000).leads("Fall", 50000)
        .src("[src~] visual hallucinations and parkinsonism; faster than Alzheimer's"));
    static final Aff FTD = add(new Aff("Frontotemporal dementia", DISEASE)
        .rate(10).peak(58, 8).gene(G_DEMENTIA, 4).on(BRAIN).dot(6).chronic().progress(1500).sev(1000, 2500)
        .psy(PSY_COGNITION, -5000).psy(PSY_IMPULSE, 5000).psy(PSY_SOCIAL, -2000).psy(PSY_AGGRESSION, 2000)
        .fn(FN_SPEECH, 4000).src("[src~] personality change and loss of restraint come first"));
    static final Aff GBS = add(new Aff("Guillain-Barre syndrome", DISEASE)
        .rate(2).doubles(25, 40).after("Gastroenteritis", 5).after("Influenza", 3).on(NERVES).dot(150)
        .alsoHit(CORE_M).lasts(30, 60).pain(30).fn(FN_MOBILITY, 8000).fn(FN_MANIP, 5000).effects(BEDRIDDEN)
        .src("[src~] ~1/4 need breathing support; untreated mortality ~10-15%"));
    static final Aff MYASTHENIA = add(new Aff("Myasthenia gravis", DISEASE)
        .rate(1).women(2).ages(15, 90).gene(G_AUTOIMM, 3).on(CORE_M).chronic().episodes(14, 30, 90, 180)
        .fn(FN_STAMINA, 4000).fn(FN_SPEECH, 2500).fn(FN_VISION, 1500).fatal(2000)
        .src("[src~] before treatment ~30% died, mostly from breathing crises"));
    static final Aff TRIGEMINAL = add(new Aff("Trigeminal neuralgia", DISEASE)
        .rate(5).doubles(15, 60).ages(30, 120).women(1.7).on(NERVES).chronic().episodes(14, 30, 90, 300).pain(90)
        .psy(PSY_MOOD, -2500).psy(PSY_ANXIETY, 2000).src("[src~] among the worst pains known"));
    static final Aff TREMOR = add(new Aff("Essential tremor", DISEASE)
        .rate(40).doubles(15, 60).ages(30, 120).gene(G_NEURODEV, 2).on(BRAIN).chronic().progress(200).sev(1000, 3000)
        .fn(FN_MANIP, 3000).psy(PSY_SOCIAL, -500).effects(QUIET));
    static final Aff PRION = add(new Aff("Prion disease", DISEASE)    // CJD, kuru
        .rate(0.15).ages(40, 120).on(BRAIN).dot(40).chronic().progress(8000).sev(1000, 2000)
        .psy(PSY_COGNITION, -8000).psy(PSY_REALITY, 4000).fn(FN_MOBILITY, 6000)
        .src("[src] ~1-2 per million a year (~0.15 per 100k)"));

    // =================================================================================
    //  SKIN (GBD B.9)
    // =================================================================================
    static { section(C_SKIN); }
    static final Aff CONTACT_D = add(new Aff("Contact dermatitis", DISEASE)
        .rate(3000).ages(5, 120).risk(CH_LABOUR, 2).on(SKIN).lasts(7, 14).pain(8).effects(QUIET));
    static final Aff SEB_D = add(new Aff("Seborrhoeic dermatitis", DISEASE)
        .rate(500).on(SKIN).chronic().remits(10000).pain(2).after("HIV infection", 10).effects(QUIET));
    static final Aff IMPETIGO = add(new Aff("Impetigo", DISEASE)      // pyoderma
        .rate(1000).kids(5).spreads(10).on(SKIN).dot(2).lasts(7, 14).pain(5)
        .onset("Post-strep kidney inflammation", 300).effects(QUIET));
    static final Aff WARTS = add(new Aff("Warts", DISEASE)            // viral skin disease
        .rate(3000).kids(2).spreads(3).on(SKIN).lasts(180, 540).effects(QUIET));
    static final Aff ALOPECIA = add(new Aff("Alopecia areata", DISEASE)
        .rate(20).gene(G_AUTOIMM, 3).on(SKIN).lasts(180, 360).psy(PSY_SOCIAL, -1500).psy(PSY_MOOD, -800).effects(QUIET));
    static final Aff PRURITUS = add(new Aff("Chronic itch", DISEASE)  // pruritus
        .rate(300).doubles(20, 60).after("Chronic kidney disease", 5).after("Cirrhosis", 5).on(SKIN).chronic()
        .remits(20000).pain(8).psy(PSY_ENERGY, -800).psy(PSY_MOOD, -800).effects(QUIET));
    static final Aff URTICARIA = add(new Aff("Hives", DISEASE)        // urticaria
        .rate(1500).on(SKIN).lasts(2, 12).pain(5).effects(QUIET));

    // =================================================================================
    //  EYES & EARS (GBD B.10) - near-vision and age hearing loss come from Body ageing
    // =================================================================================
    static { section(C_SENSE); }
    static final Aff PRESBYOPIA = add(new Aff("Presbyopia", DISEASE)  // near vision loss; no reading glasses
        .rate(15000).ages(40, 60).peak(47, 5).on(L_EYE).chronic().fn(FN_MANIP, 500).effects(QUIET)
        .src("[src] near-universal by ~50; fine work suffers (FN_MANIP)"));
    static final Aff MENIERE = add(new Aff("Meniere's disease", DISEASE)
        .rate(15).ages(30, 80).on(L_EAR, R_EAR).dot(1).chronic().episodes(1, 1, 30, 120).progress(200)
        .fn(FN_MOBILITY, 5000).psy(PSY_ANXIETY, 1500).leads("Fall", 100000).effects(QUIET));

    // =================================================================================
    //  BONES, JOINTS & MUSCLES (GBD B.11)
    // =================================================================================
    static { section(C_MSK); }
    static final Aff NECK = add(new Aff("Neck pain", DISEASE)
        .rate(4000).ages(15, 120).peak(45, 20).women(1.3).risk(CH_LABOUR, 2).risk(CH_STRESS, 1.5)
        .on(SPINE_J).lasts(30, 120).pain(25).fn(FN_MANIP, 500).effects(QUIET));
    static final Aff PMR = add(new Aff("Polymyalgia rheumatica", DISEASE)
        .rate(50).doubles(10, 70).ages(50, 120).women(2).on(CORE_M).alsoHit(L_SHOULDER, R_SHOULDER).chronic()
        .remits(30000).pain(40).fn(FN_MOBILITY, 2000).psy(PSY_ENERGY, -2500));
    static final Aff SCLERODERMA = add(new Aff("Systemic sclerosis", DISEASE)
        .rate(2).women(4).ages(25, 70).gene(G_AUTOIMM, 4).on(SKIN).dot(1).alsoHit(L_LUNG, R_LUNG, OESOPHAGUS, L_KIDNEY)
        .chronic().progress(500).sev(1000, 3000).fn(FN_MANIP, 3000).pain(20));
    static final Aff SJOGREN = add(new Aff("Sjogren's syndrome", DISEASE)
        .rate(7).women(9).ages(30, 80).gene(G_AUTOIMM, 4).on(MOUTH).alsoHit(L_EYE, R_EYE).chronic().pain(5)
        .psy(PSY_ENERGY, -1500).effects(QUIET));
    static final Aff TENDON = add(new Aff("Tendinitis", DISEASE)       // overuse: elbow, shoulder, heel
        .rate(1500).ages(15, 120).risk(CH_LABOUR, 3).on(L_ELBOW, R_ELBOW, L_SHOULDER, R_SHOULDER, L_ANKLE, R_ANKLE)
        .lasts(30, 90).pain(25).fn(FN_MANIP, 500).effects(QUIET));

    // =================================================================================
    //  ENDOCRINE, BLOOD, IMMUNE (GBD B.12.4, B.12.5)
    // =================================================================================
    static { section(C_BLOOD); }
    static final Aff THAL_TRAIT = add(new Aff("Thalassaemia trait", NONE)   // carrier (Genes.express)
        .chronic().blood(B2_HB, -800).effects(QUIET).src("[src] mild anaemia; protects against severe malaria"));
    static final Aff SICKLE_TRAIT = add(new Aff("Sickle cell trait", NONE)  // carrier
        .chronic().effects(QUIET).src("[src] harmless at rest; ~90% protection from severe malaria"));
    static final Aff G6PD = add(new Aff("G6PD deficiency", NONE)          // genotype, X-linked (Genes)
        .chronic().leads("Haemolytic crisis", 3000).effects(QUIET)
        .src("[src] fava beans, infections and some drugs burst red cells; protects against malaria"));
    static final Aff G6PD_TRAIT = add(new Aff("G6PD trait", NONE)         // heterozygous women
        .chronic().leads("Haemolytic crisis", 300).effects(QUIET));
    static final Aff HAEMOLYSIS = add(new Aff("Haemolytic crisis", NONE)
        .on(MARROW).blood(B2_HB, -4000).alsoHit(L_KIDNEY, R_KIDNEY).dot(10).lasts(5, 10).pain(30).psy(PSY_ENERGY, -3000));
    static final Aff SPHEROCYTOSIS = add(new Aff("Hereditary spherocytosis", NEWBORN)  // other haemolytic anaemias
        .rate(50).on(SPLEEN).chronic().blood(B2_HB, -1500).leads("Gallstones", 2000).effects(QUIET)
        .src("[src~] ~1 in 2000 northern Europeans"));
    static final Aff HAEMOCHROM = add(new Aff("Haemochromatosis", DISEASE)  // iron overload
        .rate(10).ages(35, 120).men(3).on(LIVER).dot(1).alsoHit(PANCREAS, HEART).chronic()
        .leads("Cirrhosis", 300).leads("Type 2 diabetes", 500).effects(QUIET)
        .src("[src~] HFE C282Y homozygotes ~1/200 Europeans; a minority get organ damage"));
    static final Aff HYPOPIT = add(new Aff("Pituitary failure", DISEASE)  // hypopituitarism
        .rate(1).ages(15, 120).after("Traumatic brain injury", 20).after("Birth haemorrhage", 50)
        .on(PITUITARY).hit(6000).chronic().fert(10).psy(PSY_ENERGY, -1500)
        .src("[src~] Sheehan's syndrome after childbirth bleeding was the classic cause"));
    static final Aff PIT_TUMOUR = add(new Aff("Pituitary tumour", DISEASE)
        .rate(4).ages(20, 120).on(PITUITARY).dot(1).chronic().progress(200).sev(500, 2000).fn(FN_VISION, 1500)
        .fert(60).pain(10).effects(QUIET));
    static final Aff CUSHING = add(new Aff("Cushing's syndrome", DISEASE)
        .rate(1).women(3).ages(20, 70).on(ADRENALS).chronic().blood(B2_GLU, 300).blood(B2_BP, 25).bmd(20).immuno(25)
        .psy(PSY_MOOD, -2000).psy(PSY_ANXIETY, 1500).src("[src~] untreated 5-year mortality ~50% (Plotz 1952)"));
    static final Aff HYPERPARA = add(new Aff("Overactive parathyroid", DISEASE)
        .rate(20).women(3).ages(40, 120).on(THYROID).chronic().bmd(15).leads("Kidney stone", 2000)
        .psy(PSY_MOOD, -500).psy(PSY_COGNITION, -500).effects(QUIET).src("bones, stones, groans and psychic moans"));
    static final Aff ANAPHYLAXIS = add(new Aff("Anaphylaxis", DISEASE)  // stings, foods
        .rate(30).risk(CH_SNAKES, 2).on(AIRWAY).hit(7000).lasts(1, 1).caseFatal(500)
        .src("[src~] ~0.3-1% of episodes fatal without adrenaline"));
    static final Aff IMMUNODEF = add(new Aff("Inborn immune deficiency", NEWBORN)
        .rate(10).on(MARROW).chronic().immuno(70).src("[src~] severe forms ~1 in 50,000; most die young without care"));

    // =================================================================================
    //  KIDNEY, URINARY, REPRODUCTIVE (GBD B.12.2, B.12.3)
    // =================================================================================
    static { section(C_REPRO); }
    static final Aff MALE_INFERT = add(new Aff("Male infertility", DISEASE)
        .rate(150).menOnly().ages(15, 50).after("Mumps orchitis", 30).after("Gonorrhoea", 3).on(GONADS).chronic()
        .fert(5).effects(QUIET).src("[src~] ~7% of men; half of couple infertility"));
    static final Aff FEMALE_INFERT = add(new Aff("Female infertility", DISEASE)
        .rate(80).womenOnly().ages(15, 45).after("Pelvic inflammatory disease", 20).after("Chlamydia", 3)
        .on(UTERUS).chronic().fert(5).effects(QUIET)
        .src("[src~] infertility not explained by PCOS, endometriosis, PID or thyroid, which carry their own fert()"));
    static final Aff PROLAPSE = add(new Aff("Genital prolapse", DISEASE)
        .rate(150).womenOnly().doubles(12, 60).ages(25, 120).risk(CH_LABOUR, 2).after("Obstructed labour", 5)
        .on(UTERUS).chronic().pain(5).fn(FN_MOBILITY, 500).psy(PSY_SOCIAL, -500).effects(QUIET));
    static final Aff PMS = add(new Aff("Premenstrual syndrome", DISEASE)
        .rate(1500).womenOnly().ages(13, 50).on(UTERUS).chronic().episodes(4, 4, 22, 6).remits(5000).pain(10)
        .psy(PSY_MOOD, -1200).psy(PSY_ANXIETY, 800).psy(PSY_AGGRESSION, 600).psy(PSY_ENERGY, -800).effects(QUIET));
    static final Aff MENORRHAGIA = add(new Aff("Heavy periods", DISEASE)  // other gynaecological
        .rate(1000).womenOnly().ages(13, 52).after("Uterine fibroids", 4).on(UTERUS).chronic().remits(10000).bleeds(2)
        .blood(B2_HB, -800).effects(QUIET));
    static final Aff DYSMENORRHOEA = add(new Aff("Painful periods", DISEASE)
        .rate(3000).womenOnly().ages(12, 45).after("Endometriosis", 5).on(UTERUS).chronic().episodes(2, 2, 26, 4)
        .remits(5000).pain(35).psy(PSY_ENERGY, -1500).effects(QUIET));

    // =================================================================================
    //  MOUTH & TEETH (GBD B.12.6)
    // =================================================================================
    static { section(C_DIGEST); }
    static final Aff PERIODONTAL = add(new Aff("Gum disease", DISEASE)   // periodontitis
        .rate(2000).doubles(20, 40).ages(20, 120).risk(CH_GLU, 2).risk(CH_TOBACCO, 2).on(MOUTH).dot(1).chronic()
        .progress(300).sev(1000, 3000).pain(3).leads("Tooth abscess", 500).effects(QUIET)
        .src("[src] GBD: severe periodontitis affects ~1 in 5 adults; the main cause of tooth loss"));
    static final Aff EDENTULISM = add(new Aff("Tooth loss", NONE)       // label: mouth & teeth < 30%
        .chronic().fn(FN_SPEECH, 800).psy(PSY_SOCIAL, -500).effects(QUIET)
        .src("chewing: the food model has no food texture yet"));
    static final Aff MOUTH_ULCER = add(new Aff("Mouth ulcers", DISEASE)
        .rate(2000).on(MOUTH).lasts(7, 7).pain(10).effects(QUIET));
    static final Aff THRUSH = add(new Aff("Oral thrush", DISEASE)
        .rate(200).kids(5).risk(CH_IMMUNO, 10).after("AIDS", 50).after("Type 2 diabetes", 3).on(MOUTH).alsoHit(OESOPHAGUS)
        .lasts(7, 14).pain(15).effects(QUIET));

    // =================================================================================
    //  CONGENITAL (GBD B.12.1)
    // =================================================================================
    static { section(C_CONGEN); }
    static final Aff HYPOSPADIAS = add(new Aff("Hypospadias", NEWBORN)
        .rate(300).menOnly().on(BLADDER).chronic().fert(70).effects(QUIET));
    static final Aff CRYPTORCH = add(new Aff("Undescended testis", NEWBORN)  // ~3% at birth, ~1% at 1 year
        .rate(1000).menOnly().after("Premature birth", 5).on(GONADS).chronic().fert(50).leads("Testicular cancer", 20)
        .effects(QUIET));
    static final Aff PYLORIC = add(new Aff("Pyloric stenosis", NEWBORN)
        .rate(200).men(4).on(STOMACH).bleeds(250).lasts(21, 14).effects(FLUID | QUIET)
        .src("[src~] ~2-3/1000; projectile vomiting; before surgery (1912) most died"));
    static final Aff HIRSCHSPRUNG = add(new Aff("Hirschsprung disease", NEWBORN)
        .rate(20).men(4).after("Down syndrome", 40).on(COLON).dot(20).sepsis(60).chronic().leads("Bowel obstruction", 100000));
    static final Aff OES_ATRESIA = add(new Aff("Oesophageal atresia", NEWBORN)
        .rate(30).after("Down syndrome", 10).on(OESOPHAGUS).hit(10000).effects(QUIET)
        .src("[src~] ~1/3500; cannot swallow: fatal without surgery"));
    static final Aff CONG_HYPOTHY = add(new Aff("Congenital hypothyroidism", NEWBORN)
        .rate(40).on(THYROID).hit(8000).chronic().psy(PSY_COGNITION, -6000).fn(FN_MOBILITY, 1500)
        .src("[src] ~1/3000 births; untreated = severe intellectual disability"));
    static final Aff MICROCEPH = add(new Aff("Microcephaly", NEWBORN)
        .rate(20).risk(CH_M_ALCOHOL, 5).on(BRAIN).chronic().psy(PSY_COGNITION, -6000).leads("Epilepsy", 10000));
    static final Aff LIMB_DEFECT = add(new Aff("Limb reduction defect", NEWBORN)
        .rate(5).on(L_ARM_M, R_ARM_M, L_LEG_M, R_LEG_M).hit(10000).chronic().effects(QUIET));
    static final Aff POLYDACTYLY = add(new Aff("Extra fingers or toes", NEWBORN)
        .rate(100).on(SKIN).chronic().effects(QUIET));
    static final Aff KIDNEY_CONG = add(new Aff("Congenital kidney malformation", NEWBORN)
        .rate(50).on(L_KIDNEY).hit(10000).chronic().effects(QUIET).src("[src~] e.g. one kidney missing: harmless alone"));
    static final Aff CHROMO_OTHER = add(new Aff("Other chromosome disorder", NEWBORN)  // e.g. 22q11 deletion
        .rate(25).on(HEART).hit(2000).alsoHit(LYMPH).chronic()   // thymus is folded into the lymph nodes.psy(PSY_COGNITION, -3000).immuno(20)
        .src("[src~] 22q11.2 deletion ~1/4000: heart defects, low immunity, learning difficulty"));

    // =================================================================================
    //  INJURIES (GBD C) - the rest are in Catalogue (falls, axe cuts, trees, burns)
    // =================================================================================
    static { section(C_INJURY); }
    static final Aff DROWNING = add(new Aff("Drowning", DISEASE)
        .rate(30).kids(3).men(2).needs(CH_DEEP_WATER).after("Epilepsy", 15).after("Alcohol use disorder", 4)
        .on(L_LUNG).hit(6000).alsoHit(R_LUNG, BRAIN).lasts(2, 3).caseFatal(70000).effects(BEDRIDDEN)
        .src("[src] WHO: ~236,000 deaths a year, mostly children under 5 and young men"));
    static final Aff CO_POISON = add(new Aff("Carbon monoxide poisoning", DISEASE)  // smoky hut, closed up in winter
        .rate(10).needs(CH_SMOKE).risk(CH_COLD, 4).on(BRAIN).hit(2000).alsoHit(HEART).lasts(1, 2).caseFatal(15000)
        .psy(PSY_COGNITION, -3000).src("[guess] rate; [src~] survivors often have lasting memory problems"));
    static final Aff POISON_PLANT = add(new Aff("Plant or mushroom poisoning", DISEASE)  // other poisoning
        .rate(15).kids(3).on(LIVER).hit(2000).alsoHit(SMALL_INT, L_KIDNEY).bleeds(200).lasts(2, 5).caseFatal(5000)
        .effects(FLUID | BEDRIDDEN).src("[guess] rate; death cap mushrooms destroy the liver"));
    static final Aff SNAKEBITE = add(new Aff("Snakebite", DISEASE)   // venomous animal contact
        .rate(30).men(1.5).ages(3, 120).needs(CH_SNAKES).on(L_LEG_M, R_LEG_M, L_ARM_M, R_ARM_M).hit(3000).dot(50)
        .bleeds(300).lasts(3, 7).pain(70).caseFatal(4000).infects(30).permanent(5000, SAME)
        .src("[src] WHO: 1.8-2.7M envenomings, 81-138k deaths, ~400k disabled each year"));
    static final Aff ANIMAL = add(new Aff("Animal attack", DISEASE)   // non-venomous: livestock, dogs, wild animals
        .rate(150).men(2).needs(CH_LIVESTOCK).on(L_LEG_M, R_LEG_M, L_ARM_M, R_ARM_M, CORE_M).injury(M_BLOW, 60, 1)
        .hit(1500).bleeds(300).lasts(3, 7).pain(50).infects(40).src("[guess] rate: kicks, gorings, bites"));
    static final Aff CHOKING = add(new Aff("Choking", DISEASE)        // aspiration and foreign body in airway
        .rate(2).kids(8).doubles(10, 70).on(AIRWAY).hit(8000).lasts(1, 1).caseFatal(30000).effects(QUIET)
        .src("[src~] toddlers and the very old; fatal when nobody helps"));
    static final Aff EYE_FB = add(new Aff("Something in the eye", DISEASE)
        .rate(300).ages(5, 120).risk(CH_LABOUR, 2).on(L_EYE, R_EYE).hit(500).lasts(2, 3).pain(30).infects(5)
        .onset("Blinding eye damage", 300).effects(QUIET));
    static final Aff SPLINTER = add(new Aff("Splinter or thorn wound", DISEASE)  // foreign body in other part
        .rate(3000).on(L_ARM_M, R_ARM_M, L_LEG_M, R_LEG_M).hit(200).lasts(3, 7).pain(10).infects(15)
        .onset("Tetanus", 30).effects(QUIET));
    static final Aff LIGHTNING = add(new Aff("Lightning strike", DISEASE)  // exposure to forces of nature
        .rate(1).men(4).ages(5, 120).on(HEART).hit(6000).alsoHit(BRAIN, SKIN, L_EAR).lasts(2, 5).caseFatal(10000)
        .src("[src~] ~10% of strikes fatal (cardiac arrest)"));
    static final Aff ELECTROCUTION = add(new Aff("Electric shock", DISEASE)
        .rate(1).men(5).needs(CH_ELECTRICITY).on(HEART).hit(5000).alsoHit(SKIN).lasts(2, 5).caseFatal(20000));
    static final Aff ROAD = add(new Aff("Road injury", WALK)          // needs vehicles: GBD's biggest injury cause
        .rate(150).men(2.5).needs(CH_VEHICLES).on(CORE_M).injury(M_CRUSH, 90, 3).hit(2000).bleeds(800).lasts(3, 14)
        .pain(70).infects(20).effects(BEDRIDDEN).src("[src] WHO: ~1.19M deaths a year (2021)"));
    static final Aff GUNSHOT = add(new Aff("Gunshot wound", NONE)      // behaviour code or firearm accidents
        .on(CORE_M, L_LUNG, LIVER, L_LEG_M, R_ARM_M).hit(4000).bleeds(1500).lasts(5, 14).pain(80).infects(40)
        .effects(BEDRIDDEN));
    static final Aff STAB = add(new Aff("Stab wound", NONE)            // violence: given by behaviour code
        .on(CORE_M, L_LUNG, LIVER, SMALL_INT, L_ARM_M).injury(M_CUT, 30, 1).hit(3000).bleeds(1200).sepsis(40)
        .lasts(5, 14).pain(70).infects(40).effects(BEDRIDDEN));
    static final Aff BEATING = add(new Aff("Beating", NONE)            // violence by other means
        .on(CORE_M, BRAIN, L_ARM_M, R_ARM_M).injury(M_BLOW, 70, 3).hit(1500).bleeds(100).lasts(3, 10).pain(60));
    static final Aff MED_ADVERSE = add(new Aff("Harm from medical treatment", DISEASE)
        .rate(100).needs(CH_MEDICINE).on(LIVER, L_KIDNEY, SKIN).dot(20).lasts(7, 14).pain(10)
        .src("needs medicine to exist in the world (Config.ENV)"));
}
