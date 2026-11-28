package meatbox;

import static meatbox.Afflictions.*;
import static meatbox.Body.*;
import static meatbox.Health.*;
import static meatbox.Genes.*;
import static meatbox.World.*;

/**
 * =============================================================================
 *  CATALOGUE (DSM)  -  mental disorders added in v0.4 so that every disorder in
 *  the DSM-5-TR (APA 2022) has a place in the sim. The older ones (depression,
 *  anxiety, psychosis, addiction, dementia...) are in Catalogue.
 * =============================================================================
 *  Effects go on the 8 mind dimensions (World.psy), body functions and stress.
 *  What a person DOES because of a disorder (setting fires, hoarding, avoiding
 *  people, offending) belongs to the behaviour code, which can read any of these
 *  with Health.hasAff(p, id). Rates target the 12-month prevalence the DSM-5-TR
 *  text gives, through prevalence ~ rate x mean duration [src~ unless marked].
 *
 *  Not modelled as illness on purpose: gender dysphoria (ICD-11 moved gender
 *  incongruence out of the mental disorders chapter; see docs/coverage.md).
 */
final class CatalogueDsm {
    private CatalogueDsm() {}
    static void register() {}

    static { section(C_MENTAL); }

    // ---- neurodevelopmental --------------------------------------------------------------
    static final Aff LANGUAGE = add(new Aff("Language disorder", NEWBORN)       // ~7% at school entry
        .rate(5000).men(1.5).gene(G_NEURODEV, 3).on(BRAIN).chronic().remits(5000).fn(FN_SPEECH, 3000)
        .psy(PSY_SOCIAL, -1000).effects(QUIET));
    static final Aff SPEECH_SOUND = add(new Aff("Speech sound disorder", NEWBORN)
        .rate(3000).men(2).on(MOUTH).chronic().remits(20000).fn(FN_SPEECH, 1500).effects(QUIET));
    static final Aff STUTTER = add(new Aff("Stuttering", NEWBORN)              // childhood-onset fluency disorder
        .rate(5000).men(3).gene(G_NEURODEV, 3).on(BRAIN).chronic().remits(20000).fn(FN_SPEECH, 2000)
        .psy(PSY_SOCIAL, -1000).psy(PSY_ANXIETY, 500).effects(QUIET).src("[src~] ~5% of children, ~80% recover"));
    static final Aff PRAGMATIC = add(new Aff("Social communication disorder", NEWBORN)
        .rate(500).men(2).gene(G_NEURODEV, 4).on(BRAIN).chronic().remits(3000).psy(PSY_SOCIAL, -2000).effects(QUIET));
    static final Aff LEARNING = add(new Aff("Specific learning disorder", NEWBORN)  // dyslexia, dyscalculia
        .rate(5000).men(2).gene(G_NEURODEV, 4).on(BRAIN).chronic().psy(PSY_COGNITION, -800).psy(PSY_MOOD, -300)
        .effects(QUIET).src("[src~] 5-15% of school children; matters only where there is schooling"));
    static final Aff DCD = add(new Aff("Developmental coordination disorder", NEWBORN)
        .rate(5000).men(2).after("Premature birth", 2).on(BRAIN).chronic().remits(5000).fn(FN_MANIP, 1000)
        .fn(FN_MOBILITY, 500).effects(QUIET));
    static final Aff STEREOTYPIC = add(new Aff("Stereotypic movement disorder", NEWBORN)
        .rate(300).after("Intellectual disability", 10).after("Autism", 10).on(BRAIN).chronic().remits(5000)
        .psy(PSY_SOCIAL, -500).effects(QUIET));
    static final Aff TIC_PERSIST = add(new Aff("Persistent tic disorder", NEWBORN)
        .rate(1000).men(2).gene(G_NEURODEV, 3).on(BRAIN).chronic().remits(10000).psy(PSY_SOCIAL, -300).effects(QUIET));
    static final Aff TIC_PROV = add(new Aff("Provisional tic disorder", DISEASE)
        .rate(3000).men(2).ages(4, 12).on(BRAIN).lasts(30, 300).effects(QUIET));

    // ---- schizophrenia spectrum and other psychotic --------------------------------------
    static final Aff SCHIZOTYPAL = add(new Aff("Schizotypal personality disorder", DISEASE)
        .rate(60).ages(14, 30).gene(G_PSYCHOSIS, 4).on(BRAIN).chronic().remits(2000)
        .psy(PSY_SOCIAL, -3000).psy(PSY_REALITY, 1000).psy(PSY_ANXIETY, 1500).leads("Schizophrenia", 300));
    static final Aff DELUSIONAL = add(new Aff("Delusional disorder", DISEASE)   // [src~] ~0.2% lifetime
        .rate(3).ages(25, 120).peak(45, 15).gene(G_PSYCHOSIS, 2).after("Deafness", 3).on(BRAIN).chronic().remits(5000)
        .psy(PSY_REALITY, 4000).psy(PSY_ANXIETY, 1500).psy(PSY_AGGRESSION, 800));
    static final Aff BRIEF_PSY = add(new Aff("Brief psychotic disorder", DISEASE)
        .rate(10).ages(15, 120).risk(CH_STRESS, 4).risk(CH_POSTPARTUM, 5).on(BRAIN).lasts(5, 25)
        .psy(PSY_REALITY, 6000).psy(PSY_COGNITION, -2000).psy(PSY_ANXIETY, 3000));
    static final Aff SCHIZOPHRENIFORM = add(new Aff("Schizophreniform disorder", DISEASE)
        .rate(5).peak(22, 6).gene(G_PSYCHOSIS, 6).on(BRAIN).lasts(30, 150).psy(PSY_REALITY, 6000)
        .psy(PSY_COGNITION, -2500).psy(PSY_SOCIAL, -3000).then("Schizophrenia", 35000)
        .src("[src~] about 2/3 go on to schizophrenia or schizoaffective disorder"));
    static final Aff SCHIZOAFFECTIVE = add(new Aff("Schizoaffective disorder", DISEASE)
        .rate(3).peak(25, 8).women(1.5).gene(G_PSYCHOSIS, 6).on(BRAIN).chronic().leads("Psychotic episode", 40000)
        .leads("Manic episode", 15000).leads("Bipolar depression", 30000).psy(PSY_SOCIAL, -2000).psy(PSY_COGNITION, -1500));
    static final Aff DRUG_PSYCHOSIS = add(new Aff("Drug-induced psychosis", NONE)
        .on(BRAIN).lasts(7, 30).psy(PSY_REALITY, 5500).psy(PSY_ANXIETY, 3000).psy(PSY_AGGRESSION, 1500));
    static final Aff CATATONIA = add(new Aff("Catatonia", DISEASE)
        .rate(1).after("Schizophrenia", 50).after("Bipolar disorder", 20).on(BRAIN).lasts(14, 60)
        .fn(FN_MOBILITY, 6000).fn(FN_SPEECH, 8000).psy(PSY_SOCIAL, -6000).effects(BEDRIDDEN)
        .src("[src~] untreated malignant catatonia can kill via dehydration and clots (bed rest does that here)"));

    // ---- bipolar and related --------------------------------------------------------------
    static final Aff BIPOLAR2 = add(new Aff("Bipolar II disorder", DISEASE)
        .rate(20).peak(22, 8).gene(G_MOOD, 6).risk(CH_STRESS, 2).on(BRAIN).chronic()
        .leads("Hypomanic episode", 30000).leads("Bipolar depression", 50000));
    static final Aff HYPOMANIA = add(new Aff("Hypomanic episode", NONE)
        .on(BRAIN).lasts(4, 10).psy(PSY_MOOD, 3000).psy(PSY_ENERGY, 3500).psy(PSY_IMPULSE, 2500).psy(PSY_SOCIAL, 2500));
    static final Aff CYCLOTHYMIA = add(new Aff("Cyclothymic disorder", DISEASE)
        .rate(30).ages(12, 40).gene(G_MOOD, 3).on(BRAIN).chronic().remits(3000).psy(PSY_IMPULSE, 1000)
        .psy(PSY_MOOD, -800).leads("Hypomanic episode", 100000).leads("Bipolar II disorder", 500));

    // ---- depressive -----------------------------------------------------------------------
    static final Aff DMDD = add(new Aff("Disruptive mood dysregulation disorder", DISEASE)
        .rate(600).men(1.5).ages(6, 17).risk(CH_ACE, 3).on(BRAIN).lasts(365, 730).psy(PSY_AGGRESSION, 3000)
        .psy(PSY_MOOD, -2000).psy(PSY_IMPULSE, 2000));
    static final Aff PMDD = add(new Aff("Premenstrual dysphoric disorder", DISEASE)  // [src~] ~2-5% of women
        .rate(300).womenOnly().ages(13, 50).gene(G_MOOD, 2).on(BRAIN).chronic().episodes(5, 3, 21, 5).remits(5000)
        .psy(PSY_MOOD, -4000).psy(PSY_ANXIETY, 3000).psy(PSY_AGGRESSION, 2500).psy(PSY_SOCIAL, -2000));

    // ---- anxiety --------------------------------------------------------------------------
    static final Aff SEPARATION = add(new Aff("Separation anxiety disorder", DISEASE)
        .rate(1500).ages(3, 14).gene(G_ANXIETY, 3).risk(CH_ACE, 3).risk(CH_STRESS, 2).on(BRAIN).lasts(180, 540)
        .psy(PSY_ANXIETY, 3000).psy(PSY_SOCIAL, -1000));
    static final Aff MUTISM = add(new Aff("Selective mutism", DISEASE)
        .rate(200).ages(3, 8).gene(G_ANXIETY, 3).on(BRAIN).lasts(365, 1095).fn(FN_SPEECH, 6000)
        .psy(PSY_SOCIAL, -3000).psy(PSY_ANXIETY, 2000));
    static final Aff AGORAPHOBIA = add(new Aff("Agoraphobia", DISEASE)
        .rate(50).ages(15, 60).women(2).gene(G_ANXIETY, 3).after("Panic disorder", 5).on(BRAIN).chronic().remits(5000)
        .psy(PSY_ANXIETY, 4000).psy(PSY_SOCIAL, -3000).src("[src~] ~1.7% 12-month; behaviour code: stays near home"));

    // ---- obsessive-compulsive and related ---------------------------------------------------
    static final Aff BDD = add(new Aff("Body dysmorphic disorder", DISEASE)
        .rate(100).peak(16, 5).gene(G_ANXIETY, 3).on(BRAIN).chronic().remits(3000)
        .psy(PSY_MOOD, -2500).psy(PSY_ANXIETY, 3000).psy(PSY_SOCIAL, -3000));
    static final Aff HOARDING = add(new Aff("Hoarding disorder", DISEASE)
        .rate(60).doubles(20, 50).ages(15, 120).gene(G_ANXIETY, 3).on(BRAIN).chronic()
        .psy(PSY_SOCIAL, -2000).psy(PSY_ANXIETY, 1500).effects(QUIET).src("[src~] ~2.5%; worsens with age"));
    static final Aff TRICHO = add(new Aff("Hair-pulling disorder", DISEASE)     // trichotillomania
        .rate(100).women(4).peak(13, 4).gene(G_ANXIETY, 2).on(SKIN).chronic().remits(5000)
        .psy(PSY_ANXIETY, 1000).psy(PSY_SOCIAL, -1000).effects(QUIET));
    static final Aff SKIN_PICK = add(new Aff("Skin-picking disorder", DISEASE)  // excoriation
        .rate(100).women(3).peak(15, 5).gene(G_ANXIETY, 2).on(SKIN).dot(1).chronic().remits(5000).infects(5)
        .psy(PSY_ANXIETY, 1000).psy(PSY_SOCIAL, -800).effects(QUIET));

    // ---- trauma and stressor-related ---------------------------------------------------------
    static final Aff RAD = add(new Aff("Reactive attachment disorder", DISEASE)  // severe neglect in infancy
        .rate(40).ages(0, 5).risk(CH_ACE, 20).on(BRAIN).chronic().remits(10000)
        .psy(PSY_SOCIAL, -5000).psy(PSY_MOOD, -2000).psy(PSY_AGGRESSION, 1000));
    static final Aff DSED = add(new Aff("Disinhibited social engagement disorder", DISEASE)
        .rate(40).ages(1, 10).risk(CH_ACE, 20).on(BRAIN).chronic().remits(10000)
        .psy(PSY_SOCIAL, 3000).psy(PSY_IMPULSE, 2000).src("indiscriminate friendliness to strangers"));
    static final Aff ASD_ACUTE = add(new Aff("Acute stress disorder", TRAUMA)
        .rate(15000).women(1.5).gene(G_ANXIETY, 2).on(BRAIN).lasts(3, 27).psy(PSY_ANXIETY, 5000)
        .psy(PSY_MOOD, -2500).psy(PSY_REALITY, 800).src("[src~] 13-21% after road crashes and assaults"));
    static final Aff ADJUST = add(new Aff("Adjustment disorder", DISEASE)
        .rate(1000).ages(5, 120).risk(CH_STRESS, 8).on(BRAIN).lasts(30, 150).psy(PSY_MOOD, -2000)
        .psy(PSY_ANXIETY, 2000).psy(PSY_SOCIAL, -1000).effects(QUIET));

    // ---- dissociative -------------------------------------------------------------------------
    static final Aff DID = add(new Aff("Dissociative identity disorder", DISEASE)
        .rate(5).ages(10, 50).women(2).risk(CH_ACE, 20).on(BRAIN).chronic().remits(2000)
        .psy(PSY_REALITY, 2000).psy(PSY_COGNITION, -1000).psy(PSY_MOOD, -2000).psy(PSY_ANXIETY, 2000)
        .src("[src~] ~1.5% in DSM-5-TR community samples; almost always after severe childhood abuse"));
    static final Aff DISS_AMNESIA = add(new Aff("Dissociative amnesia", DISEASE)
        .rate(20).ages(10, 120).risk(CH_ACE, 5).risk(CH_STRESS, 4).on(BRAIN).lasts(30, 300).psy(PSY_COGNITION, -2500));
    static final Aff DEPERS = add(new Aff("Depersonalisation-derealisation disorder", DISEASE)
        .rate(60).peak(16, 6).risk(CH_ACE, 3).on(BRAIN).chronic().remits(10000).psy(PSY_REALITY, 1000)
        .psy(PSY_ANXIETY, 2000).psy(PSY_MOOD, -1000));

    // ---- somatic symptom and related ---------------------------------------------------------
    static final Aff SOMATIC = add(new Aff("Somatic symptom disorder", DISEASE)
        .rate(400).women(2).ages(12, 120).risk(CH_STRESS, 2).risk(CH_ACE, 2).on(BRAIN).chronic().remits(10000)
        .pain(20).psy(PSY_ANXIETY, 3000).psy(PSY_ENERGY, -1000));
    static final Aff ILLNESS_ANX = add(new Aff("Illness anxiety disorder", DISEASE)
        .rate(200).ages(15, 120).gene(G_ANXIETY, 2).on(BRAIN).chronic().remits(10000).psy(PSY_ANXIETY, 3500));
    static final Aff CONVERSION = add(new Aff("Functional neurological disorder", DISEASE)  // conversion disorder
        .rate(10).women(3).ages(10, 120).risk(CH_STRESS, 4).risk(CH_ACE, 2).on(BRAIN).lasts(30, 335)
        .fn(FN_MOBILITY, 4000).fn(FN_SPEECH, 1000).fn(FN_VISION, 1000).psy(PSY_ANXIETY, 1500));
    static final Aff FACTITIOUS = add(new Aff("Factitious disorder", DISEASE)
        .rate(3).women(2).ages(15, 60).risk(CH_ACE, 3).on(BRAIN).chronic().remits(5000).effects(QUIET)
        .src("behaviour code: feigns illness for care"));

    // ---- feeding and eating ---------------------------------------------------------------------
    static final Aff PICA = add(new Aff("Pica", DISEASE)
        .rate(1000).ages(1, 8).after("Iron-deficiency anaemia", 5).after("Intellectual disability", 5).on(BRAIN)
        .lasts(90, 270).leads("Roundworm infection", 20000).effects(QUIET).src("eating soil, clay, ice, paint"));
    static final Aff RUMINATION = add(new Aff("Rumination disorder", DISEASE)
        .rate(20).kids(5).after("Intellectual disability", 5).on(STOMACH).chronic().remits(10000).effects(QUIET));
    static final Aff ARFID = add(new Aff("Avoidant-restrictive food intake disorder", DISEASE)
        .rate(60).kids(3).men(1.5).gene(G_ANXIETY, 2).after("Autism", 5).on(BRAIN).chronic().remits(10000)
        .psy(PSY_ANXIETY, 1500).effects(QUIET).src("eating less needs the behaviour code to read this condition"));
    static final Aff BINGE = add(new Aff("Binge-eating disorder", DISEASE)
        .rate(150).women(2).peak(22, 8).gene(G_ANXIETY, 2).on(BRAIN).chronic().remits(10000)
        .psy(PSY_MOOD, -1500).psy(PSY_IMPULSE, 2000));

    // ---- elimination -------------------------------------------------------------------------------
    static final Aff ENURESIS = add(new Aff("Bedwetting", DISEASE)          // enuresis
        .rate(3000).men(2).ages(5, 12).on(BLADDER).lasts(365, 730).psy(PSY_SOCIAL, -800).psy(PSY_MOOD, -300)
        .effects(QUIET));
    static final Aff ENCOPRESIS = add(new Aff("Soiling", DISEASE)           // encopresis
        .rate(500).men(3).ages(4, 12).on(COLON).lasts(180, 540).psy(PSY_SOCIAL, -1500).effects(QUIET));

    // ---- sleep-wake ---------------------------------------------------------------------------------
    static final Aff HYPERSOMNIA = add(new Aff("Hypersomnolence disorder", DISEASE)
        .rate(30).peak(20, 8).on(BRAIN).chronic().psy(PSY_ENERGY, -3000).effects(QUIET));
    static final Aff NARCOLEPSY = add(new Aff("Narcolepsy", DISEASE)        // [src~] ~0.03-0.05%
        .rate(1).peak(17, 6).on(BRAIN).chronic().psy(PSY_ENERGY, -3000).psy(PSY_COGNITION, -1000)
        .leads("Fall", 20000).leads("Burn", 1000).effects(QUIET));
    static final Aff CENTRAL_APNOEA = add(new Aff("Central sleep apnoea", DISEASE)
        .rate(10).doubles(15, 65).ages(40, 120).men(3).after("Heart failure", 10).after("Ischaemic stroke", 5)
        .on(BRAIN).chronic().psy(PSY_ENERGY, -1500).effects(QUIET));
    static final Aff HYPOVENT = add(new Aff("Sleep-related hypoventilation", DISEASE)
        .rate(2).after("Chronic obstructive pulmonary disease", 20).after("Motor neurone disease", 30)
        .on(CORE_M).chronic().psy(PSY_ENERGY, -1500).psy(PSY_COGNITION, -500).effects(QUIET));
    static final Aff CIRCADIAN = add(new Aff("Circadian rhythm sleep disorder", DISEASE)
        .rate(300).peak(18, 6).on(BRAIN).chronic().remits(20000).psy(PSY_ENERGY, -1000).effects(QUIET));
    static final Aff SLEEPWALK = add(new Aff("Sleepwalking and night terrors", DISEASE)  // NREM arousal disorder
        .rate(500).kids(8).on(BRAIN).lasts(365, 365).leads("Fall", 5000).effects(QUIET));
    static final Aff NIGHTMARE = add(new Aff("Nightmare disorder", DISEASE)
        .rate(400).after("Post-traumatic stress disorder", 10).risk(CH_STRESS, 2).on(BRAIN).chronic().remits(20000)
        .psy(PSY_ENERGY, -800).psy(PSY_ANXIETY, 1000).effects(QUIET));
    static final Aff RBD = add(new Aff("REM sleep behaviour disorder", DISEASE)
        .rate(20).doubles(10, 65).ages(40, 120).men(3).on(BRAIN).chronic().leads("Parkinson's disease", 5000)
        .leads("Dementia with Lewy bodies", 1500).leads("Fall", 3000).effects(QUIET)
        .src("[src] most go on to Parkinson's or Lewy body dementia within ~15 yr (Postuma 2019)"));
    static final Aff RLS = add(new Aff("Restless legs syndrome", DISEASE)
        .rate(300).women(2).doubles(20, 50).ages(15, 120).after("Iron-deficiency anaemia", 3)
        .after("Chronic kidney disease", 3).risk(CH_PREGNANT, 3).on(NERVES).chronic().remits(5000)
        .psy(PSY_ENERGY, -1000).effects(QUIET));

    // ---- sexual dysfunctions (DSM: only with lasting distress) ---------------------------------------
    static final Aff ED = add(new Aff("Erectile disorder", DISEASE)
        .rate(800).menOnly().doubles(10, 60).ages(20, 120).risk(CH_GLU, 3).risk(CH_ATHERO, 3).on(VESSELS).chronic()
        .fert(50).psy(PSY_MOOD, -500).effects(QUIET));
    static final Aff PE_EJAC = add(new Aff("Premature ejaculation", DISEASE)
        .rate(400).menOnly().ages(16, 120).on(NERVES).chronic().remits(5000).effects(QUIET));
    static final Aff DELAYED_EJAC = add(new Aff("Delayed ejaculation", DISEASE)
        .rate(50).menOnly().ages(16, 120).on(NERVES).chronic().remits(5000).fert(50).effects(QUIET));
    static final Aff MALE_HSDD = add(new Aff("Male low sexual desire disorder", DISEASE)
        .rate(200).menOnly().doubles(20, 50).ages(18, 120).risk(CH_STRESS, 2).on(BRAIN).chronic().remits(10000)
        .fert(70).effects(QUIET));
    static final Aff FEMALE_SIAD = add(new Aff("Female sexual interest/arousal disorder", DISEASE)
        .rate(600).womenOnly().ages(18, 120).risk(CH_STRESS, 2).on(BRAIN).chronic().remits(10000).fert(80)
        .effects(QUIET));
    static final Aff FEMALE_ORG = add(new Aff("Female orgasmic disorder", DISEASE)
        .rate(400).womenOnly().ages(18, 120).on(BRAIN).chronic().remits(10000).effects(QUIET));
    static final Aff GPPPD = add(new Aff("Genito-pelvic pain disorder", DISEASE)
        .rate(200).womenOnly().ages(16, 120).after("Endometriosis", 4).after("Obstetric fistula", 5).on(UTERUS)
        .chronic().remits(10000).pain(5).fert(60).psy(PSY_ANXIETY, 1000).effects(QUIET));

    // ---- disruptive, impulse-control and conduct ---------------------------------------------------------
    static final Aff ODD = add(new Aff("Oppositional defiant disorder", DISEASE)
        .rate(1500).men(1.5).ages(4, 14).gene(G_EXTERNAL, 3).risk(CH_ACE, 3).on(BRAIN).lasts(365, 730)
        .psy(PSY_AGGRESSION, 2000).psy(PSY_IMPULSE, 1500).psy(PSY_SOCIAL, -500).then("Conduct disorder", 10000));
    static final Aff IED = add(new Aff("Intermittent explosive disorder", DISEASE)
        .rate(250).men(1.5).peak(16, 6).gene(G_EXTERNAL, 3).risk(CH_ACE, 3).on(BRAIN).chronic().remits(5000)
        .psy(PSY_AGGRESSION, 4500).psy(PSY_IMPULSE, 3500));
    static final Aff PYROMANIA = add(new Aff("Pyromania", DISEASE)
        .rate(5).men(5).ages(10, 60).on(BRAIN).chronic().remits(10000).psy(PSY_IMPULSE, 2000).effects(QUIET)
        .src("behaviour code: deliberate fire-setting"));
    static final Aff KLEPTOMANIA = add(new Aff("Kleptomania", DISEASE)
        .rate(10).women(3).ages(12, 70).on(BRAIN).chronic().remits(5000).psy(PSY_IMPULSE, 2000).effects(QUIET)
        .src("behaviour code: steals things not needed"));

    // ---- substance-related and addictive -------------------------------------------------------------------
    static final Aff TOBACCO_UD = add(new Aff("Tobacco use disorder", DISEASE)  // harm comes through Config.TOBACCO
        .rate(3000).ages(12, 120).men(1.5).needs(CH_TOBACCO).gene(G_EXTERNAL, 2).on(BRAIN).chronic().remits(3000)
        .psy(PSY_ANXIETY, 300).effects(QUIET));
    static final Aff CANNABIS_UD = add(new Aff("Cannabis use disorder", DISEASE)
        .rate(600).men(2).peak(20, 6).needs(CH_CANNABIS).gene(G_EXTERNAL, 3).on(BRAIN).chronic().remits(10000)
        .psy(PSY_ENERGY, -1000).psy(PSY_COGNITION, -800).psy(PSY_MOOD, -500).leads("Drug-induced psychosis", 300)
        .src("[src~] heavy use roughly doubles psychosis risk (Marconi 2016)"));
    static final Aff COCAINE_UD = add(new Aff("Cocaine use disorder", DISEASE)
        .rate(300).men(2).ages(14, 120).needs(CH_COCAINE).gene(G_EXTERNAL, 4).on(HEART).dot(1).chronic().remits(5000)
        .blood(B2_BP, 15).fatal(500).psy(PSY_IMPULSE, 2500).psy(PSY_AGGRESSION, 1500).psy(PSY_ANXIETY, 1500)
        .psy(PSY_MOOD, -1500).leads("Heart attack", 300).leads("Intracerebral haemorrhage", 100)
        .leads("Drug-induced psychosis", 1000));
    static final Aff AMPHET_UD = add(new Aff("Amphetamine use disorder", DISEASE)
        .rate(300).men(1.5).ages(14, 120).needs(CH_AMPHETAMINE).gene(G_EXTERNAL, 4).on(HEART).dot(1).chronic()
        .remits(5000).blood(B2_BP, 15).fatal(500).psy(PSY_IMPULSE, 2500).psy(PSY_AGGRESSION, 2000)
        .psy(PSY_ANXIETY, 1500).psy(PSY_MOOD, -1500).leads("Drug-induced psychosis", 3000).leads("Gum disease", 5000));
    static final Aff HALLUC_UD = add(new Aff("Hallucinogen use disorder", DISEASE)
        .rate(50).men(2).ages(14, 60).needs(CH_HALLUCINOGENS).on(BRAIN).chronic().remits(20000)
        .psy(PSY_REALITY, 500).leads("Drug-induced psychosis", 500).effects(QUIET));
    static final Aff INHALANT_UD = add(new Aff("Inhalant use disorder", DISEASE)
        .rate(300).men(1.5).ages(10, 30).needs(CH_INHALANTS).risk(CH_ACE, 3).on(BRAIN).dot(2).alsoHit(LIVER, L_KIDNEY)
        .chronic().remits(15000).fatal(300).psy(PSY_COGNITION, -2000).psy(PSY_IMPULSE, 1500)
        .src("fatal(): sudden sniffing death (arrhythmia)"));
    static final Aff SEDATIVE_UD = add(new Aff("Sedative use disorder", DISEASE)
        .rate(200).women(1.5).ages(15, 120).needs(CH_SEDATIVES).risk(CH_STRESS, 1).on(BRAIN).chronic().remits(5000)
        .psy(PSY_COGNITION, -1500).psy(PSY_ENERGY, -1500).leads("Fall", 30000).fatal(200));
    static final Aff CAFFEINE_W = add(new Aff("Caffeine withdrawal", DISEASE)
        .rate(5000).ages(12, 120).needs(CH_CAFFEINE).on(BRAIN).lasts(2, 5).pain(20).psy(PSY_ENERGY, -2000).effects(QUIET));
    static final Aff GAMBLING = add(new Aff("Gambling disorder", DISEASE)
        .rate(100).men(2).ages(15, 120).needs(CH_GAMBLING).gene(G_EXTERNAL, 3).on(BRAIN).chronic().remits(10000)
        .psy(PSY_IMPULSE, 2500).psy(PSY_MOOD, -2000).psy(PSY_ANXIETY, 2000).stress(3)
        .src("[src~] ~0.5% 12-month; behaviour code: stakes and loses goods"));
    static final Aff OPIOID_W = add(new Aff("Opioid withdrawal", NONE)
        .on(SMALL_INT).bleeds(30).lasts(5, 5).pain(40).psy(PSY_ANXIETY, 4000).psy(PSY_AGGRESSION, 1500)
        .psy(PSY_ENERGY, -2000).effects(FLUID));
    static final Aff WKS = add(new Aff("Alcohol-related brain damage", DISEASE)  // Wernicke-Korsakoff
        .rate(300).requires("Alcohol use disorder").risk(CH_MALNUT, 3).on(BRAIN).dot(2).chronic()
        .psy(PSY_COGNITION, -5000).psy(PSY_REALITY, 1000).fn(FN_MOBILITY, 1500)
        .src("[src~] thiamine lack; ~1-2% of heavy drinkers at autopsy"));

    // ---- personality disorders (cluster A, B, C) ----------------------------------------------------------
    static final Aff PARANOID_PD = add(new Aff("Paranoid personality disorder", DISEASE)
        .rate(100).ages(14, 30).gene(G_PSYCHOSIS, 2).risk(CH_ACE, 3).on(BRAIN).chronic().remits(2000)
        .psy(PSY_SOCIAL, -2500).psy(PSY_AGGRESSION, 1500).psy(PSY_ANXIETY, 1500).psy(PSY_REALITY, 500));
    static final Aff SCHIZOID_PD = add(new Aff("Schizoid personality disorder", DISEASE)
        .rate(40).men(1.5).ages(14, 30).gene(G_PSYCHOSIS, 2).on(BRAIN).chronic().remits(1000).psy(PSY_SOCIAL, -5000));
    static final Aff HISTRIONIC_PD = add(new Aff("Histrionic personality disorder", DISEASE)
        .rate(40).ages(14, 30).on(BRAIN).chronic().remits(2000).psy(PSY_SOCIAL, 2000).psy(PSY_IMPULSE, 1500)
        .psy(PSY_MOOD, -500));
    static final Aff NARCISSISTIC_PD = add(new Aff("Narcissistic personality disorder", DISEASE)
        .rate(40).men(2).ages(14, 30).on(BRAIN).chronic().remits(2000).psy(PSY_AGGRESSION, 1500).psy(PSY_SOCIAL, -500)
        .src("behaviour code: entitlement, exploits others"));
    static final Aff AVOIDANT_PD = add(new Aff("Avoidant personality disorder", DISEASE)
        .rate(100).ages(14, 30).gene(G_ANXIETY, 3).on(BRAIN).chronic().remits(2000).psy(PSY_SOCIAL, -4000)
        .psy(PSY_ANXIETY, 3000));
    static final Aff DEPENDENT_PD = add(new Aff("Dependent personality disorder", DISEASE)
        .rate(30).women(1.5).ages(14, 30).on(BRAIN).chronic().remits(2000).psy(PSY_ANXIETY, 2500).psy(PSY_SOCIAL, 1000));
    static final Aff OCPD = add(new Aff("Obsessive-compulsive personality disorder", DISEASE)
        .rate(150).ages(14, 30).gene(G_ANXIETY, 2).on(BRAIN).chronic().remits(1000).psy(PSY_ANXIETY, 1500)
        .psy(PSY_SOCIAL, -1000));

    // ---- paraphilic disorders (DSM: interest plus distress or harm to others) -------------------------------
    //  No body effects. Whether anyone acts on them, and what the society does about it, is behaviour code.
    static final Aff VOYEUR = add(new Aff("Voyeuristic disorder", DISEASE)
        .rate(15).men(10).ages(15, 60).on(BRAIN).chronic().remits(5000).psy(PSY_IMPULSE, 1000).effects(QUIET));
    static final Aff EXHIB = add(new Aff("Exhibitionistic disorder", DISEASE)
        .rate(10).men(10).ages(15, 60).on(BRAIN).chronic().remits(5000).psy(PSY_IMPULSE, 1500).effects(QUIET));
    static final Aff FROTTEUR = add(new Aff("Frotteuristic disorder", DISEASE)
        .rate(5).men(10).ages(15, 60).on(BRAIN).chronic().remits(5000).psy(PSY_IMPULSE, 1000).effects(QUIET));
    static final Aff MASOCHISM = add(new Aff("Sexual masochism disorder", DISEASE)
        .rate(3).ages(15, 60).on(BRAIN).chronic().remits(5000).psy(PSY_ANXIETY, 800).effects(QUIET));
    static final Aff SADISM = add(new Aff("Sexual sadism disorder", DISEASE)
        .rate(3).men(5).ages(15, 60).on(BRAIN).chronic().remits(3000).psy(PSY_AGGRESSION, 1000).effects(QUIET));
    static final Aff PEDOPHILIC = add(new Aff("Pedophilic disorder", DISEASE)
        .rate(5).menOnly().ages(16, 60).on(BRAIN).chronic().psy(PSY_ANXIETY, 800).effects(QUIET)
        .src("[src~] DSM-5-TR: up to 3-5% of men report the interest; the disorder is rarer. Lifelong."));
    static final Aff FETISHISTIC = add(new Aff("Fetishistic disorder", DISEASE)
        .rate(5).men(10).ages(15, 60).on(BRAIN).chronic().remits(5000).effects(QUIET));
    static final Aff TRANSVESTIC = add(new Aff("Transvestic disorder", DISEASE)
        .rate(3).menOnly().ages(15, 60).on(BRAIN).chronic().remits(5000).psy(PSY_ANXIETY, 800).effects(QUIET)
        .src("DSM-5-TR counts it only with distress; cross-dressing itself is not a disorder"));

    /** Sedatives are taken for anxiety: use the anxiety-family stress channel as a mild risk. */
}
