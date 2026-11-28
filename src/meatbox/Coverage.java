package meatbox;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static meatbox.Afflictions.*;
import static meatbox.Health.*;

/**
 * WHAT IS IN THE GAME, AND WHY: the inclusion lists, and where each entry lives in the catalogue.
 *
 * Two published lists decide what the sim models:
 *   - GBD 2021 cause list (IHME, Global Burden of Disease): every leaf cause of death and disability,
 *     310 of them, covering all of ICD-10/ICD-11 by mapping. This is the physical-illness and injury list.
 *   - DSM-5-TR (American Psychiatric Association, 2022): every named mental disorder.
 *
 * Every row names the sim conditions that stand for it, or says why there are none:
 *   folded:  covered by the named conditions (sub-types the sim does not separate)
 *   hook:    needs a system outside health (violence, transport, treatment): the behaviour code gives the
 *            injury condition when it happens
 *   n/a:     cannot occur in this world (needs modern medicine) or is not a disorder (see note)
 *
 * Add an illness: add it to a catalogue, then put its name in the row it belongs to.
 * CoverageTest fails if any name here is missing from the catalogue.
 * `java -cp out meatbox.Coverage > docs/coverage.md` rewrites the coverage document.
 */
final class Coverage {
    private Coverage() {}

    // { source code, source name, sim conditions ("a; b"), note }
    static final String[][] GBD = {
        // ---- A. Communicable, maternal, neonatal and nutritional
        {"A.1.1.1", "HIV/AIDS - Drug-susceptible tuberculosis", "HIV infection; AIDS; Tuberculosis", "co-infection: HIV raises TB risk"},
        {"A.1.1.2", "HIV/AIDS - Multidrug-resistant tuberculosis", "", "n/a: drug resistance needs TB drugs"},
        {"A.1.1.3", "HIV/AIDS - Extensively drug-resistant tuberculosis", "", "n/a: drug resistance needs TB drugs"},
        {"A.1.1.4", "HIV/AIDS resulting in other diseases", "HIV infection; AIDS; HIV infection (from mother)", ""},
        {"A.1.2.1", "Syphilis", "Syphilis; Latent syphilis; Neurosyphilis; Congenital syphilis", ""},
        {"A.1.2.2", "Chlamydial infection", "Chlamydia", ""},
        {"A.1.2.3", "Gonococcal infection", "Gonorrhoea; Pelvic inflammatory disease", ""},
        {"A.1.2.4", "Trichomoniasis", "Trichomoniasis", ""},
        {"A.1.2.5", "Genital herpes", "Genital herpes", ""},
        {"A.1.2.6", "Other sexually transmitted infections", "Chancroid", ""},
        {"A.2.1.1", "Latent tuberculosis infection", "Latent tuberculosis", ""},
        {"A.2.1.2", "Drug-susceptible tuberculosis", "Tuberculosis", ""},
        {"A.2.1.3", "Multidrug-resistant tuberculosis", "", "n/a: drug resistance needs TB drugs"},
        {"A.2.1.4", "Extensively drug-resistant tuberculosis", "", "n/a: drug resistance needs TB drugs"},
        {"A.2.2", "Lower respiratory infections", "Pneumonia; Influenza", ""},
        {"A.2.3", "Upper respiratory infections", "Common cold; Strep throat", ""},
        {"A.2.4", "Otitis media", "Ear infection; Chronic ear infection; Deafness from infection", ""},
        {"A.2.5", "COVID-19", "COVID-19; Post-COVID condition", ""},
        {"A.3.1", "Diarrheal diseases", "Gastroenteritis; Cholera; Dysentery; Amoebic dysentery", ""},
        {"A.3.2.1", "Typhoid fever", "Typhoid fever", ""},
        {"A.3.2.2", "Paratyphoid fever", "Paratyphoid fever", ""},
        {"A.3.3", "Invasive non-typhoidal Salmonella", "Invasive salmonella", ""},
        {"A.3.5", "Other intestinal infectious diseases", "Giardiasis; Amoebic liver abscess", ""},
        {"A.4.1", "Malaria", "Malaria", ""},
        {"A.4.2", "Chagas disease", "Chagas disease; Chagas heart disease; Chagas megacolon", ""},
        {"A.4.3.1", "Visceral leishmaniasis", "Visceral leishmaniasis", ""},
        {"A.4.3.2", "Cutaneous and mucocutaneous leishmaniasis", "Cutaneous leishmaniasis; Mucocutaneous leishmaniasis", ""},
        {"A.4.4", "African trypanosomiasis", "Sleeping sickness", ""},
        {"A.4.5", "Schistosomiasis", "Schistosomiasis", ""},
        {"A.4.6", "Cysticercosis", "Neurocysticercosis", ""},
        {"A.4.7", "Cystic echinococcosis", "Hydatid disease", ""},
        {"A.4.8", "Lymphatic filariasis", "Lymphatic filariasis; Elephantiasis; Hydrocele", ""},
        {"A.4.9", "Onchocerciasis", "River blindness", ""},
        {"A.4.10", "Trachoma", "Trachoma; Blinding eye damage", ""},
        {"A.4.11", "Dengue", "Dengue; Severe dengue", ""},
        {"A.4.12", "Yellow fever", "Yellow fever", ""},
        {"A.4.13", "Rabies", "Rabies", ""},
        {"A.4.14.1", "Ascariasis", "Roundworm infection", ""},
        {"A.4.14.2", "Trichuriasis", "Whipworm infection", ""},
        {"A.4.14.3", "Hookworm disease", "Hookworm disease", ""},
        {"A.4.15", "Food-borne trematodiases", "Liver fluke infection", ""},
        {"A.4.16", "Leprosy", "Leprosy", ""},
        {"A.4.17", "Ebola", "Ebola", ""},
        {"A.4.18", "Zika virus", "Zika; Congenital Zika syndrome", ""},
        {"A.4.19", "Guinea worm disease", "Guinea worm disease", ""},
        {"A.4.20", "Other neglected tropical diseases", "Yaws", ""},
        {"A.5.1", "Meningitis", "Meningitis", ""},
        {"A.5.2", "Encephalitis", "Encephalitis", ""},
        {"A.5.3", "Diphtheria", "Diphtheria", ""},
        {"A.5.4", "Pertussis", "Whooping cough", ""},
        {"A.5.5", "Tetanus", "Tetanus; Neonatal tetanus", ""},
        {"A.5.6", "Measles", "Measles", ""},
        {"A.5.7", "Varicella and herpes zoster", "Chickenpox; Shingles; Postherpetic neuralgia", ""},
        {"A.5.8.1", "Acute hepatitis A", "Hepatitis A", ""},
        {"A.5.8.2", "Acute hepatitis B", "Hepatitis B", ""},
        {"A.5.8.3", "Acute hepatitis C", "Hepatitis C", ""},
        {"A.5.8.4", "Acute hepatitis E", "Hepatitis E; Acute liver failure", ""},
        {"A.5.9", "Other unspecified infectious diseases",
            "Polio; Smallpox; Mumps; Mumps orchitis; Rubella; Plague; Epidemic typhus; Lyme disease; Lyme arthritis; "
            + "Brucellosis; Anthrax; Leptospirosis; Osteomyelitis; Chronic osteomyelitis; Wound infection", ""},
        {"A.6.1.1", "Maternal hemorrhage", "Birth haemorrhage", ""},
        {"A.6.1.2", "Maternal sepsis and other maternal infections", "Childbed fever", ""},
        {"A.6.1.3", "Maternal hypertensive disorders", "Pre-eclampsia; Eclampsia", ""},
        {"A.6.1.4", "Maternal obstructed labor and uterine rupture", "Obstructed labour; Uterine rupture; Obstetric fistula", ""},
        {"A.6.1.5", "Maternal abortion and miscarriage", "Miscarriage", ""},
        {"A.6.1.6", "Ectopic pregnancy", "Ectopic pregnancy", ""},
        {"A.6.1.7", "Indirect maternal deaths", "", "folded: pregnancy raises malaria, anaemia, TB and other risks (CH_PREGNANT)"},
        {"A.6.1.8", "Late maternal deaths", "", "folded: emerges from the conditions above that outlast birth"},
        {"A.6.1.9", "Maternal deaths aggravated by HIV/AIDS", "", "folded: emerges from HIV infection in pregnancy"},
        {"A.6.1.12", "Other direct maternal disorders", "Hyperemesis gravidarum; Amniotic fluid embolism; Gestational diabetes", ""},
        {"A.6.2.1", "Neonatal preterm birth", "Premature birth", ""},
        {"A.6.2.2", "Neonatal encephalopathy due to birth asphyxia and trauma", "Birth asphyxia; Birth injury (brachial plexus)", ""},
        {"A.6.2.3", "Neonatal sepsis and other neonatal infections", "Newborn sepsis", ""},
        {"A.6.2.4", "Hemolytic disease and other neonatal jaundice", "Severe newborn jaundice; Rhesus disease of the newborn", ""},
        {"A.6.2.5", "Other neonatal disorders", "Stillbirth", ""},
        {"A.7.1", "Protein-energy malnutrition", "Malnutrition; Kwashiorkor; Starvation", ""},
        {"A.7.2", "Iodine deficiency", "Goitre; Congenital iodine deficiency", ""},
        {"A.7.3", "Vitamin A deficiency", "Vitamin A blindness", ""},
        {"A.7.4", "Dietary iron deficiency", "Iron-deficiency anaemia", ""},
        {"A.7.5", "Other nutritional deficiencies", "Scurvy; Pellagra; Beriberi; Rickets; Osteomalacia", ""},
        // ---- B. Non-communicable: cancers
        {"B.1.1", "Lip and oral cavity cancer", "Mouth cancer", ""},
        {"B.1.2", "Nasopharynx cancer", "Nasopharynx cancer", ""},
        {"B.1.3", "Other pharynx cancer", "Throat cancer", ""},
        {"B.1.4", "Esophageal cancer", "Oesophageal cancer", ""},
        {"B.1.5", "Stomach cancer", "Stomach cancer", ""},
        {"B.1.6", "Colon and rectum cancer", "Bowel cancer", ""},
        {"B.1.7.1", "Liver cancer due to hepatitis B", "Liver cancer; Chronic hepatitis B", "one liver cancer, raised by each cause"},
        {"B.1.7.2", "Liver cancer due to hepatitis C", "Liver cancer; Chronic hepatitis C", ""},
        {"B.1.7.3", "Liver cancer due to alcohol use", "Liver cancer; Cirrhosis", ""},
        {"B.1.7.4", "Liver cancer due to NASH", "Liver cancer; Fatty liver disease", ""},
        {"B.1.7.5", "Hepatoblastoma", "Hepatoblastoma", ""},
        {"B.1.7.6", "Liver cancer due to other causes", "Liver cancer; Haemochromatosis", ""},
        {"B.1.8", "Gallbladder and biliary tract cancer", "Gallbladder and bile duct cancer", ""},
        {"B.1.9", "Pancreatic cancer", "Pancreatic cancer", ""},
        {"B.1.10", "Larynx cancer", "Larynx cancer", ""},
        {"B.1.11", "Tracheal, bronchus, and lung cancer", "Lung cancer", ""},
        {"B.1.12", "Malignant skin melanoma", "Melanoma", ""},
        {"B.1.13.1", "Non-melanoma skin cancer (squamous-cell carcinoma)", "Skin cancer (non-melanoma)", ""},
        {"B.1.13.2", "Non-melanoma skin cancer (basal-cell carcinoma)", "Basal cell carcinoma", ""},
        {"B.1.14", "Soft tissue and other extraosseous sarcomas", "Soft tissue sarcoma", ""},
        {"B.1.15", "Malignant neoplasm of bone and articular cartilage", "Bone cancer", ""},
        {"B.1.16", "Breast cancer", "Breast cancer", ""},
        {"B.1.17", "Cervical cancer", "Cervical cancer", ""},
        {"B.1.18", "Uterine cancer", "Womb cancer", ""},
        {"B.1.19", "Ovarian cancer", "Ovarian cancer", ""},
        {"B.1.20", "Prostate cancer", "Prostate cancer", ""},
        {"B.1.21", "Testicular cancer", "Testicular cancer", ""},
        {"B.1.22", "Kidney cancer", "Kidney cancer", ""},
        {"B.1.23", "Bladder cancer", "Bladder cancer", ""},
        {"B.1.24", "Brain and central nervous system cancer", "Brain tumour", ""},
        {"B.1.25.1", "Retinoblastoma", "Retinoblastoma", ""},
        {"B.1.25.2", "Other eye cancers", "Eye melanoma", ""},
        {"B.1.26", "Neuroblastoma and other peripheral nervous cell tumors", "Neuroblastoma", ""},
        {"B.1.27", "Thyroid cancer", "Thyroid cancer", ""},
        {"B.1.28", "Mesothelioma", "Mesothelioma", ""},
        {"B.1.29", "Hodgkin lymphoma", "Hodgkin lymphoma", ""},
        {"B.1.30.1", "Burkitt lymphoma", "Burkitt lymphoma", ""},
        {"B.1.30.2", "Other non-Hodgkin lymphoma", "Non-Hodgkin lymphoma", ""},
        {"B.1.31", "Multiple myeloma", "Myeloma", ""},
        {"B.1.32.1", "Acute lymphoid leukemia", "Acute lymphoid leukaemia", ""},
        {"B.1.32.2", "Chronic lymphoid leukemia", "Chronic lymphoid leukaemia", ""},
        {"B.1.32.3", "Acute myeloid leukemia", "Acute myeloid leukaemia", ""},
        {"B.1.32.4", "Chronic myeloid leukemia", "Chronic myeloid leukaemia", ""},
        {"B.1.32.5", "Other leukemia", "", "folded: rare types counted in the four named leukaemias"},
        {"B.1.33", "Other malignant neoplasms", "Cancer of other site", ""},
        {"(spread)", "Metastases (GBD counts them under the first cancer)", "Bone metastases; Liver metastases; Brain metastases; Lung metastases", "any cancer can spread to these"},
        {"B.1.34.1", "Myelodysplastic, myeloproliferative, and other hematopoietic neoplasms", "Myelodysplastic syndrome; Polycythaemia vera", ""},
        {"B.1.34.2", "Benign and in situ intestinal neoplasms", "Bowel polyps", ""},
        {"B.1.34.3", "Benign and in situ cervical and uterine neoplasms", "", "folded: the pre-cancer stage is part of Cervical cancer's slow start"},
        {"B.1.34.5", "Other benign and in situ neoplasms", "Meningioma; Benign skin and soft tissue lump; Pituitary tumour", ""},
        // ---- heart and circulation
        {"B.2.1", "Rheumatic heart disease", "Rheumatic fever; Rheumatic heart disease", ""},
        {"B.2.2", "Ischemic heart disease", "Heart attack; Angina", ""},
        {"B.2.3.1", "Ischemic stroke", "Ischaemic stroke", ""},
        {"B.2.3.2", "Intracerebral hemorrhage", "Intracerebral haemorrhage", ""},
        {"B.2.3.3", "Subarachnoid hemorrhage", "Subarachnoid haemorrhage", ""},
        {"B.2.4", "Hypertensive heart disease", "High blood pressure; Hypertensive heart disease", ""},
        {"B.2.5.1", "Non-rheumatic calcific aortic valve disease", "Aortic valve stenosis", ""},
        {"B.2.5.2", "Non-rheumatic degenerative mitral valve disease", "Mitral valve prolapse and regurgitation", ""},
        {"B.2.5.3", "Other non-rheumatic valve diseases", "Other heart valve disease", ""},
        {"B.2.6.1", "Myocarditis", "Myocarditis", ""},
        {"B.2.6.2", "Alcoholic cardiomyopathy", "Alcoholic cardiomyopathy", ""},
        {"B.2.6.3", "Other cardiomyopathy", "Cardiomyopathy; Chagas heart disease", ""},
        {"B.2.7", "Pulmonary arterial hypertension", "Pulmonary arterial hypertension", ""},
        {"B.2.8", "Atrial fibrillation and flutter", "Atrial fibrillation", ""},
        {"B.2.9", "Aortic aneurysm", "Aortic aneurysm", ""},
        {"B.2.10", "Lower extremity peripheral arterial disease", "Peripheral artery disease; Gangrene", ""},
        {"B.2.11", "Endocarditis", "Endocarditis", ""},
        {"B.2.12", "Other cardiovascular and circulatory diseases",
            "Varicose veins; Deep vein thrombosis; Pulmonary embolism; Raynaud's phenomenon; Leg ulcer; Heart failure; Hardened arteries", ""},
        // ---- lungs
        {"B.3.1", "Chronic obstructive pulmonary disease", "Chronic obstructive pulmonary disease", ""},
        {"B.3.2.1", "Silicosis", "Silicosis", ""},
        {"B.3.2.2", "Asbestosis", "Asbestosis", ""},
        {"B.3.2.3", "Coal workers pneumoconiosis", "Coal workers' lung disease", ""},
        {"B.3.2.4", "Other pneumoconiosis", "", "folded: other mineral dusts act like Silicosis (switch E_SILICA)"},
        {"B.3.3", "Asthma", "Asthma; Asthma attack", ""},
        {"B.3.4", "Interstitial lung disease and pulmonary sarcoidosis", "Pulmonary fibrosis; Sarcoidosis; Farmer's lung", ""},
        {"B.3.5", "Other chronic respiratory diseases", "Bronchiectasis; Chronic sinusitis; Hay fever; Sleep apnoea; Spontaneous collapsed lung", ""},
        // ---- digestive
        {"B.4.1.1", "Chronic hepatitis B including cirrhosis", "Chronic hepatitis B; Cirrhosis", ""},
        {"B.4.1.2", "Chronic hepatitis C including cirrhosis", "Chronic hepatitis C; Cirrhosis", ""},
        {"B.4.1.3", "Cirrhosis due to alcohol", "Cirrhosis; Alcohol use disorder", ""},
        {"B.4.1.4", "Nonalcoholic fatty liver disease including cirrhosis", "Fatty liver disease; Cirrhosis", ""},
        {"B.4.1.5", "Cirrhosis due to other causes", "Cirrhosis; Haemochromatosis", ""},
        {"B.4.2.1", "Peptic ulcer disease", "Stomach ulcer; Bleeding ulcer; Perforated ulcer", ""},
        {"B.4.2.2", "Gastritis and duodenitis", "Gastritis", ""},
        {"B.4.2.3", "Gastroesophageal reflux disease", "Acid reflux disease", ""},
        {"B.4.3", "Appendicitis", "Appendicitis", ""},
        {"B.4.4", "Paralytic ileus and intestinal obstruction", "Bowel obstruction", ""},
        {"B.4.5", "Inguinal, femoral, and abdominal hernia", "Inguinal hernia; Strangulated hernia; Femoral hernia", ""},
        {"B.4.6.1", "Ulcerative colitis", "Ulcerative colitis", ""},
        {"B.4.6.2", "Crohn's disease", "Crohn's disease", ""},
        {"B.4.7", "Vascular intestinal disorders", "Bowel ischaemia", ""},
        {"B.4.8", "Gallbladder and biliary diseases", "Gallstones; Biliary colic; Gallbladder infection", ""},
        {"B.4.9", "Pancreatitis", "Acute pancreatitis; Chronic pancreatitis", ""},
        {"B.4.11", "Other digestive diseases",
            "Coeliac disease; Irritable bowel syndrome; Diverticulitis; Haemorrhoids; Anal fissure; Chagas megacolon", ""},
        // ---- neurological
        {"B.5.1", "Alzheimer's disease and other dementias",
            "Alzheimer's disease; Vascular dementia; Dementia with Lewy bodies; Frontotemporal dementia; Parkinson's dementia", ""},
        {"B.5.2", "Parkinson's disease", "Parkinson's disease", ""},
        {"B.5.3", "Idiopathic epilepsy", "Epilepsy", ""},
        {"B.5.4", "Multiple sclerosis", "Multiple sclerosis", ""},
        {"B.5.5", "Motor neuron disease", "Motor neurone disease", ""},
        {"B.5.6.1", "Migraine", "Migraine", ""},
        {"B.5.6.2", "Tension-type headache", "Tension-type headache", ""},
        {"B.5.7", "Other neurological disorders",
            "Guillain-Barre syndrome; Myasthenia gravis; Trigeminal neuralgia; Essential tremor; Prion disease; Bell's palsy; "
            + "Peripheral neuropathy; Huntington's disease; Cerebral palsy; Delirium; Chronic fatigue syndrome", ""},
        // ---- mental (GBD's list; DSM-5-TR below is the full one)
        {"B.6.1", "Schizophrenia", "Schizophrenia; Psychotic episode", ""},
        {"B.6.2.1", "Major depressive disorder", "Major depressive disorder; Postpartum depression", ""},
        {"B.6.2.2", "Dysthymia", "Persistent depressive disorder", ""},
        {"B.6.3", "Bipolar disorder", "Bipolar disorder; Manic episode; Bipolar depression; Postpartum psychosis", ""},
        {"B.6.4", "Anxiety disorders", "Generalised anxiety disorder; Panic disorder; Social anxiety disorder; Specific phobia; Agoraphobia", ""},
        {"B.6.5.1", "Anorexia nervosa", "Anorexia nervosa", ""},
        {"B.6.5.2", "Bulimia nervosa", "Bulimia nervosa", ""},
        {"B.6.6", "Autism spectrum disorders", "Autism", ""},
        {"B.6.7", "Attention-deficit/hyperactivity disorder", "ADHD", ""},
        {"B.6.8", "Conduct disorder", "Conduct disorder", ""},
        {"B.6.9", "Idiopathic developmental intellectual disability", "Intellectual disability", ""},
        {"B.6.10", "Other mental disorders", "", "folded: every other DSM-5-TR disorder, see the DSM table"},
        {"B.7.1", "Alcohol use disorders", "Alcohol use disorder; Alcohol withdrawal; Alcohol-related brain damage", ""},
        {"B.7.2.1", "Opioid use disorders", "Opioid use disorder; Opioid withdrawal", ""},
        {"B.7.2.2", "Cocaine use disorders", "Cocaine use disorder", ""},
        {"B.7.2.3", "Amphetamine use disorders", "Amphetamine use disorder", ""},
        {"B.7.2.4", "Cannabis use disorders", "Cannabis use disorder", ""},
        {"B.7.2.5", "Other drug use disorders", "Hallucinogen use disorder; Inhalant use disorder; Sedative use disorder", ""},
        // ---- diabetes and kidney
        {"B.8.1.1", "Diabetes mellitus type 1", "Type 1 diabetes; Diabetic coma", ""},
        {"B.8.1.2", "Diabetes mellitus type 2", "Type 2 diabetes", ""},
        {"B.8.2.1", "Chronic kidney disease due to diabetes mellitus type 1", "Chronic kidney disease; Type 1 diabetes", "kidney disease is a label for kidneys below half, by any route"},
        {"B.8.2.2", "Chronic kidney disease due to diabetes mellitus type 2", "Chronic kidney disease; Type 2 diabetes", ""},
        {"B.8.2.3", "Chronic kidney disease due to hypertension", "Chronic kidney disease; High blood pressure", ""},
        {"B.8.2.4", "Chronic kidney disease due to glomerulonephritis", "Chronic kidney disease; Post-strep kidney inflammation; Lupus", ""},
        {"B.8.2.5", "Chronic kidney disease due to other and unspecified causes", "Chronic kidney disease; Polycystic kidney disease; Kidney stone disease", ""},
        {"B.8.3", "Acute glomerulonephritis", "Post-strep kidney inflammation", ""},
        // ---- skin
        {"B.9.1.1", "Atopic dermatitis", "Eczema", ""},
        {"B.9.1.2", "Contact dermatitis", "Contact dermatitis", ""},
        {"B.9.1.3", "Seborrhoeic dermatitis", "Seborrhoeic dermatitis", ""},
        {"B.9.2", "Psoriasis", "Psoriasis", ""},
        {"B.9.3.1", "Cellulitis", "Cellulitis", ""},
        {"B.9.3.2", "Pyoderma", "Impetigo", ""},
        {"B.9.4", "Scabies", "Scabies", ""},
        {"B.9.5", "Fungal skin diseases", "Fungal skin infection", ""},
        {"B.9.6", "Viral skin diseases", "Warts", ""},
        {"B.9.7", "Acne vulgaris", "Acne", ""},
        {"B.9.8", "Alopecia areata", "Alopecia areata", ""},
        {"B.9.9", "Pruritus", "Chronic itch", ""},
        {"B.9.10", "Urticaria", "Hives", ""},
        {"B.9.11", "Decubitus ulcer", "Pressure ulcer", ""},
        {"B.9.12", "Other skin and subcutaneous diseases", "Vitiligo; Albinism", ""},
        // ---- senses
        {"B.10.1.1", "Glaucoma", "Glaucoma", ""},
        {"B.10.1.2", "Cataract", "Cataract; Congenital cataract", ""},
        {"B.10.1.3", "Age-related macular degeneration", "Macular degeneration", ""},
        {"B.10.1.4", "Refraction disorders", "Short sight", ""},
        {"B.10.1.5", "Near vision loss", "Presbyopia", ""},
        {"B.10.1.7", "Other vision loss", "Blindness; Blinding eye damage", ""},
        {"B.10.2", "Age-related and other hearing loss", "Deafness; Congenital deafness", ""},
        {"B.10.3", "Other sense organ diseases", "Tinnitus; Vertigo; Meniere's disease; Conjunctivitis", ""},
        // ---- muscles, bones, joints
        {"B.11.1", "Rheumatoid arthritis", "Rheumatoid arthritis", ""},
        {"B.11.2.1", "Osteoarthritis hip", "Osteoarthritis; Worn joints", "one condition placed on the worn joint"},
        {"B.11.2.2", "Osteoarthritis knee", "Osteoarthritis; Worn joints", ""},
        {"B.11.2.3", "Osteoarthritis hand", "Osteoarthritis", ""},
        {"B.11.2.4", "Osteoarthritis other", "Osteoarthritis", ""},
        {"B.11.3", "Low back pain", "Low back pain; Chronic back pain", ""},
        {"B.11.4", "Neck pain", "Neck pain", ""},
        {"B.11.5", "Gout", "Gout; Gout attack", ""},
        {"B.11.6", "Other musculoskeletal disorders",
            "Osteoporosis; Ankylosing spondylitis; Lupus; Fibromyalgia; Carpal tunnel syndrome; Frozen shoulder; Polymyalgia rheumatica; "
            + "Systemic sclerosis; Sjogren's syndrome; Tendinitis; Paget's disease of bone; Scoliosis; Muscle wasting", ""},
        // ---- congenital
        {"B.12.1.1", "Neural tube defects", "Spina bifida; Anencephaly", ""},
        {"B.12.1.2", "Congenital heart anomalies", "Congenital heart defect", ""},
        {"B.12.1.3", "Orofacial clefts", "Cleft lip and palate", ""},
        {"B.12.1.4", "Down syndrome", "Down syndrome", ""},
        {"B.12.1.5", "Turner syndrome", "Turner syndrome", ""},
        {"B.12.1.6", "Klinefelter syndrome", "Klinefelter syndrome", ""},
        {"B.12.1.7", "Other chromosomal abnormalities", "Trisomy 13/18; Other chromosome disorder", ""},
        {"B.12.1.8", "Congenital musculoskeletal and limb anomalies",
            "Clubfoot; Hip dysplasia; Limb reduction defect; Extra fingers or toes; Achondroplasia; Brittle bone disease; Duchenne muscular dystrophy", ""},
        {"B.12.1.9", "Urogenital congenital anomalies", "Hypospadias; Undescended testis; Congenital kidney malformation", ""},
        {"B.12.1.10", "Digestive congenital anomalies", "Pyloric stenosis; Hirschsprung disease; Oesophageal atresia", ""},
        {"B.12.1.11", "Other congenital birth defects",
            "Hydrocephalus; Microcephaly; Congenital rubella syndrome; Fetal alcohol syndrome; Congenital hypothyroidism; Fragile X syndrome", ""},
        // ---- urinary, genital, blood, endocrine, mouth
        {"B.12.2.1", "Urinary tract infections and interstitial nephritis", "Urinary tract infection; Pyelonephritis", ""},
        {"B.12.2.2", "Urolithiasis", "Kidney stone; Kidney stone disease", ""},
        {"B.12.2.3", "Benign prostatic hyperplasia", "Enlarged prostate", ""},
        {"B.12.2.4", "Male infertility", "Male infertility", ""},
        {"B.12.2.5", "Other urinary diseases", "Incontinence", ""},
        {"B.12.3.1", "Uterine fibroids", "Uterine fibroids", ""},
        {"B.12.3.2", "Polycystic ovarian syndrome", "Polycystic ovary syndrome", ""},
        {"B.12.3.3", "Female infertility", "Female infertility", ""},
        {"B.12.3.4", "Endometriosis", "Endometriosis", ""},
        {"B.12.3.5", "Genital prolapse", "Genital prolapse", ""},
        {"B.12.3.6", "Premenstrual syndrome", "Premenstrual syndrome", ""},
        {"B.12.3.7", "Other gynecological diseases", "Heavy periods; Painful periods", ""},
        {"B.12.4.1", "Thalassemias", "Thalassaemia major", ""},
        {"B.12.4.2", "Thalassemias trait", "Thalassaemia trait", ""},
        {"B.12.4.3", "Sickle cell disorders", "Sickle cell disease; Sickle cell crisis", ""},
        {"B.12.4.4", "Sickle cell trait", "Sickle cell trait", ""},
        {"B.12.4.5", "G6PD deficiency", "G6PD deficiency; Haemolytic crisis", ""},
        {"B.12.4.6", "G6PD trait", "G6PD trait", ""},
        {"B.12.4.7", "Other hemoglobinopathies and hemolytic anemias", "Hereditary spherocytosis; Anaemia", ""},
        {"B.12.5.1", "Thyroid diseases", "Underactive thyroid; Overactive thyroid; Goitre", ""},
        {"B.12.5.2", "Other endocrine, metabolic, blood, and immune disorders",
            "Adrenal failure; Pituitary failure; Cushing's syndrome; Overactive parathyroid; Haemochromatosis; Haemophilia; Joint bleed; "
            + "Anaphylaxis; Inborn immune deficiency; Phenylketonuria; Cystic fibrosis", ""},
        {"B.12.6.1", "Caries of deciduous teeth", "Tooth decay", "one condition for milk and adult teeth"},
        {"B.12.6.2", "Caries of permanent teeth", "Tooth decay; Tooth abscess", ""},
        {"B.12.6.3", "Periodontal diseases", "Gum disease", ""},
        {"B.12.6.4", "Edentulism", "Tooth loss", ""},
        {"B.12.6.5", "Other oral disorders", "Mouth ulcers; Oral thrush", ""},
        {"B.12.7", "Sudden infant death syndrome", "Sudden infant death", ""},
        // ---- C. Injuries
        {"C.1.1.1", "Pedestrian road injuries", "Road injury", "needs road vehicles (E_VEHICLES)"},
        {"C.1.1.2", "Cyclist road injuries", "Road injury", ""},
        {"C.1.1.3", "Motorcyclist road injuries", "Road injury", ""},
        {"C.1.1.4", "Motor vehicle road injuries", "Road injury", ""},
        {"C.1.1.5", "Other road injuries", "Road injury", ""},
        {"C.1.2", "Other transport injuries", "", "hook: riding, carts and boats need a transport system; give Fall or Drowning"},
        {"C.2.1", "Falls", "Fall", ""},
        {"C.2.2", "Drowning", "Drowning", ""},
        {"C.2.3", "Fire, heat, and hot substances", "Burn", ""},
        {"C.2.4.1", "Poisoning by carbon monoxide", "Carbon monoxide poisoning", ""},
        {"C.2.4.2", "Poisoning by other means", "Plant or mushroom poisoning", ""},
        {"C.2.5.1", "Unintentional firearm injuries", "Gunshot wound", "needs firearms (E_FIREARMS)"},
        {"C.2.5.2", "Other exposure to mechanical forces",
            "Crushed by tree; Struck by branch; Axe cut; Splinter or thorn wound; Twisted ankle; Dislocated shoulder; Muscle strain", ""},
        {"C.2.6", "Adverse effects of medical treatment", "Harm from medical treatment", "needs medicine (E_MEDICINE)"},
        {"C.2.7.1", "Venomous animal contact", "Snakebite", ""},
        {"C.2.7.2", "Non-venomous animal contact", "Animal attack", ""},
        {"C.2.8.1", "Pulmonary aspiration and foreign body in airway", "Choking", ""},
        {"C.2.8.2", "Foreign body in eyes", "Something in the eye", ""},
        {"C.2.8.3", "Foreign body in other body part", "Splinter or thorn wound", ""},
        {"C.2.9", "Electrocution", "Electric shock", "needs mains power (E_ELECTRICITY)"},
        {"C.2.10", "Environmental heat and cold exposure", "Hypothermia; Frostbite; Heat stroke", ""},
        {"C.2.11", "Exposure to forces of nature", "Lightning strike", ""},
        {"C.2.12", "Other unintentional injuries", "Concussion; Traumatic brain injury; Collapsed lung; Paraplegia; Tetraplegia", ""},
        {"C.3.1.3", "Self-harm by firearm", "Suicide", "one suicide hazard; the means is not tracked"},
        {"C.3.1.6", "Self-harm by other specified means", "Suicide", ""},
        {"C.3.2.1", "Physical violence by firearm", "Gunshot wound", "hook: the behaviour code gives it when someone shoots"},
        {"C.3.2.2", "Physical violence by sharp object", "Stab wound", "hook: given by the behaviour code"},
        {"C.3.2.3", "Sexual violence", "Acute stress disorder; Post-traumatic stress disorder", "hook: the social code decides when it happens; these are its effects"},
        {"C.3.2.4", "Physical violence by other means", "Beating", "hook: given by the behaviour code"},
        {"C.3.3", "Conflict and terrorism", "Gunshot wound; Stab wound; Beating", "hook: war is a social system"},
        {"C.3.4", "Police conflict and executions", "", "hook: needs a state"},
    };

    static final String[][] DSM = {
        // ---- Neurodevelopmental
        {"ND", "Intellectual developmental disorder", "Intellectual disability", ""},
        {"ND", "Language disorder", "Language disorder", ""},
        {"ND", "Speech sound disorder", "Speech sound disorder", ""},
        {"ND", "Childhood-onset fluency disorder (stuttering)", "Stuttering", ""},
        {"ND", "Social (pragmatic) communication disorder", "Social communication disorder", ""},
        {"ND", "Autism spectrum disorder", "Autism", ""},
        {"ND", "Attention-deficit/hyperactivity disorder", "ADHD", ""},
        {"ND", "Specific learning disorder", "Specific learning disorder", ""},
        {"ND", "Developmental coordination disorder", "Developmental coordination disorder", ""},
        {"ND", "Stereotypic movement disorder", "Stereotypic movement disorder", ""},
        {"ND", "Tourette's disorder", "Tourette syndrome", ""},
        {"ND", "Persistent (chronic) motor or vocal tic disorder", "Persistent tic disorder", ""},
        {"ND", "Provisional tic disorder", "Provisional tic disorder", ""},
        // ---- Schizophrenia spectrum and other psychotic
        {"PS", "Schizotypal personality disorder", "Schizotypal personality disorder", ""},
        {"PS", "Delusional disorder", "Delusional disorder", ""},
        {"PS", "Brief psychotic disorder", "Brief psychotic disorder", ""},
        {"PS", "Schizophreniform disorder", "Schizophreniform disorder", ""},
        {"PS", "Schizophrenia", "Schizophrenia; Psychotic episode", ""},
        {"PS", "Schizoaffective disorder", "Schizoaffective disorder", ""},
        {"PS", "Substance/medication-induced psychotic disorder", "Drug-induced psychosis", ""},
        {"PS", "Psychotic disorder due to another medical condition", "Neurosyphilis; Delirium", "folded: psychosis effects of the illness itself"},
        {"PS", "Catatonia", "Catatonia", ""},
        // ---- Bipolar
        {"BP", "Bipolar I disorder", "Bipolar disorder; Manic episode; Bipolar depression; Postpartum psychosis", "postpartum psychosis is mostly bipolar with peripartum onset"},
        {"BP", "Bipolar II disorder", "Bipolar II disorder; Hypomanic episode", ""},
        {"BP", "Cyclothymic disorder", "Cyclothymic disorder", ""},
        // ---- Depressive
        {"DE", "Disruptive mood dysregulation disorder", "Disruptive mood dysregulation disorder", ""},
        {"DE", "Major depressive disorder", "Major depressive disorder; Postpartum depression", ""},
        {"DE", "Persistent depressive disorder", "Persistent depressive disorder", ""},
        {"DE", "Premenstrual dysphoric disorder", "Premenstrual dysphoric disorder", ""},
        {"DE", "Depressive disorder due to another medical condition", "", "folded: mood effects of the illness itself (psy())"},
        // ---- Anxiety
        {"AX", "Separation anxiety disorder", "Separation anxiety disorder", ""},
        {"AX", "Selective mutism", "Selective mutism", ""},
        {"AX", "Specific phobia", "Specific phobia", ""},
        {"AX", "Social anxiety disorder", "Social anxiety disorder", ""},
        {"AX", "Panic disorder", "Panic disorder", ""},
        {"AX", "Agoraphobia", "Agoraphobia", ""},
        {"AX", "Generalized anxiety disorder", "Generalised anxiety disorder", ""},
        // ---- Obsessive-compulsive and related
        {"OC", "Obsessive-compulsive disorder", "Obsessive-compulsive disorder", ""},
        {"OC", "Body dysmorphic disorder", "Body dysmorphic disorder", ""},
        {"OC", "Hoarding disorder", "Hoarding disorder", ""},
        {"OC", "Trichotillomania (hair-pulling disorder)", "Hair-pulling disorder", ""},
        {"OC", "Excoriation (skin-picking) disorder", "Skin-picking disorder", ""},
        // ---- Trauma and stressor-related
        {"TR", "Reactive attachment disorder", "Reactive attachment disorder", ""},
        {"TR", "Disinhibited social engagement disorder", "Disinhibited social engagement disorder", ""},
        {"TR", "Posttraumatic stress disorder", "Post-traumatic stress disorder; Post-traumatic stress (bereavement)", ""},
        {"TR", "Acute stress disorder", "Acute stress disorder", ""},
        {"TR", "Adjustment disorders", "Adjustment disorder", ""},
        {"TR", "Prolonged grief disorder", "Grief; Prolonged grief disorder", ""},
        // ---- Dissociative
        {"DI", "Dissociative identity disorder", "Dissociative identity disorder", ""},
        {"DI", "Dissociative amnesia", "Dissociative amnesia", ""},
        {"DI", "Depersonalization/derealization disorder", "Depersonalisation-derealisation disorder", ""},
        // ---- Somatic symptom and related
        {"SO", "Somatic symptom disorder", "Somatic symptom disorder", ""},
        {"SO", "Illness anxiety disorder", "Illness anxiety disorder", ""},
        {"SO", "Functional neurological symptom disorder", "Functional neurological disorder", ""},
        {"SO", "Psychological factors affecting other medical conditions", "", "folded: stress worsens immunity and the heart (Health.immunity, Sim body loop)"},
        {"SO", "Factitious disorder", "Factitious disorder", ""},
        // ---- Feeding and eating
        {"FE", "Pica", "Pica", ""},
        {"FE", "Rumination disorder", "Rumination disorder", ""},
        {"FE", "Avoidant/restrictive food intake disorder", "Avoidant-restrictive food intake disorder", ""},
        {"FE", "Anorexia nervosa", "Anorexia nervosa", ""},
        {"FE", "Bulimia nervosa", "Bulimia nervosa", ""},
        {"FE", "Binge-eating disorder", "Binge-eating disorder", ""},
        // ---- Elimination
        {"EL", "Enuresis", "Bedwetting", ""},
        {"EL", "Encopresis", "Soiling", ""},
        // ---- Sleep-wake
        {"SL", "Insomnia disorder", "Insomnia disorder", ""},
        {"SL", "Hypersomnolence disorder", "Hypersomnolence disorder", ""},
        {"SL", "Narcolepsy", "Narcolepsy", ""},
        {"SL", "Obstructive sleep apnea hypopnea", "Sleep apnoea", ""},
        {"SL", "Central sleep apnea", "Central sleep apnoea", ""},
        {"SL", "Sleep-related hypoventilation", "Sleep-related hypoventilation", ""},
        {"SL", "Circadian rhythm sleep-wake disorders", "Circadian rhythm sleep disorder", ""},
        {"SL", "Non-rapid eye movement sleep arousal disorders", "Sleepwalking and night terrors", ""},
        {"SL", "Nightmare disorder", "Nightmare disorder", ""},
        {"SL", "Rapid eye movement sleep behavior disorder", "REM sleep behaviour disorder", ""},
        {"SL", "Restless legs syndrome", "Restless legs syndrome", ""},
        // ---- Sexual dysfunctions
        {"SX", "Delayed ejaculation", "Delayed ejaculation", ""},
        {"SX", "Erectile disorder", "Erectile disorder", ""},
        {"SX", "Female orgasmic disorder", "Female orgasmic disorder", ""},
        {"SX", "Female sexual interest/arousal disorder", "Female sexual interest/arousal disorder", ""},
        {"SX", "Genito-pelvic pain/penetration disorder", "Genito-pelvic pain disorder", ""},
        {"SX", "Male hypoactive sexual desire disorder", "Male low sexual desire disorder", ""},
        {"SX", "Premature (early) ejaculation", "Premature ejaculation", ""},
        // ---- Gender dysphoria
        {"GD", "Gender dysphoria", "", "n/a: left out on purpose; ICD-11 moved gender incongruence out of mental disorders. Your call: see the docs"},
        // ---- Disruptive, impulse-control and conduct
        {"DC", "Oppositional defiant disorder", "Oppositional defiant disorder", ""},
        {"DC", "Intermittent explosive disorder", "Intermittent explosive disorder", ""},
        {"DC", "Conduct disorder", "Conduct disorder", ""},
        {"DC", "Antisocial personality disorder", "Antisocial personality disorder", ""},
        {"DC", "Pyromania", "Pyromania", ""},
        {"DC", "Kleptomania", "Kleptomania", ""},
        // ---- Substance-related and addictive
        {"SU", "Alcohol use disorder", "Alcohol use disorder; Alcohol withdrawal; Alcohol-related brain damage", ""},
        {"SU", "Caffeine withdrawal", "Caffeine withdrawal", ""},
        {"SU", "Cannabis use disorder", "Cannabis use disorder", ""},
        {"SU", "Hallucinogen use disorders", "Hallucinogen use disorder", ""},
        {"SU", "Inhalant use disorder", "Inhalant use disorder", ""},
        {"SU", "Opioid use disorder", "Opioid use disorder; Opioid withdrawal", ""},
        {"SU", "Sedative, hypnotic, or anxiolytic use disorder", "Sedative use disorder", ""},
        {"SU", "Stimulant use disorder", "Cocaine use disorder; Amphetamine use disorder", ""},
        {"SU", "Tobacco use disorder", "Tobacco use disorder", ""},
        {"SU", "Gambling disorder", "Gambling disorder", ""},
        // ---- Neurocognitive
        {"NC", "Delirium", "Delirium", ""},
        {"NC", "Major or mild neurocognitive disorder",
            "Alzheimer's disease; Vascular dementia; Dementia with Lewy bodies; Frontotemporal dementia; Parkinson's dementia; "
            + "Prion disease; Huntington's disease; Traumatic brain injury; Alcohol-related brain damage", ""},
        // ---- Personality
        {"PD", "Paranoid personality disorder", "Paranoid personality disorder", ""},
        {"PD", "Schizoid personality disorder", "Schizoid personality disorder", ""},
        {"PD", "Borderline personality disorder", "Borderline personality disorder", ""},
        {"PD", "Histrionic personality disorder", "Histrionic personality disorder", ""},
        {"PD", "Narcissistic personality disorder", "Narcissistic personality disorder", ""},
        {"PD", "Avoidant personality disorder", "Avoidant personality disorder", ""},
        {"PD", "Dependent personality disorder", "Dependent personality disorder", ""},
        {"PD", "Obsessive-compulsive personality disorder", "Obsessive-compulsive personality disorder", ""},
        {"PD", "Personality change due to another medical condition", "", "folded: personality effects of brain injury and dementia (psy())"},
        // ---- Paraphilic
        {"PA", "Voyeuristic disorder", "Voyeuristic disorder", "behaviour left to the social code"},
        {"PA", "Exhibitionistic disorder", "Exhibitionistic disorder", ""},
        {"PA", "Frotteuristic disorder", "Frotteuristic disorder", ""},
        {"PA", "Sexual masochism disorder", "Sexual masochism disorder", ""},
        {"PA", "Sexual sadism disorder", "Sexual sadism disorder", ""},
        {"PA", "Pedophilic disorder", "Pedophilic disorder", ""},
        {"PA", "Fetishistic disorder", "Fetishistic disorder", ""},
        {"PA", "Transvestic disorder", "Transvestic disorder", ""},
        // ---- Medication-induced movement disorders
        {"MI", "Medication-induced movement disorders and adverse effects", "", "n/a: no medication yet"},
    };

    static final String[][] DSM_CHAPTER = {
        {"ND", "Neurodevelopmental"}, {"PS", "Schizophrenia spectrum and other psychotic"}, {"BP", "Bipolar and related"},
        {"DE", "Depressive"}, {"AX", "Anxiety"}, {"OC", "Obsessive-compulsive and related"},
        {"TR", "Trauma- and stressor-related"}, {"DI", "Dissociative"}, {"SO", "Somatic symptom and related"},
        {"FE", "Feeding and eating"}, {"EL", "Elimination"}, {"SL", "Sleep-wake"}, {"SX", "Sexual dysfunctions"},
        {"GD", "Gender dysphoria"}, {"DC", "Disruptive, impulse-control and conduct"}, {"SU", "Substance-related and addictive"},
        {"NC", "Neurocognitive"}, {"PD", "Personality"}, {"PA", "Paraphilic"}, {"MI", "Medication-induced"},
    };

    static int leaves() { int n = 0; for (String[] r : GBD) if (!r[0].startsWith("(")) n++; return n; }

    static String[] names(String[] row) {
        if (row[2].isEmpty()) return new String[0];
        String[] n = row[2].split(";");
        for (int i = 0; i < n.length; i++) n[i] = n[i].trim();
        return n;
    }

    /** Problems with the table: names not in the catalogue, rows with neither names nor a reason,
     *  and catalogue conditions that no row mentions. Empty = all good. */
    static List<String> check() {
        Object init = Catalogue.STARVATION;   // builds the catalogue
        List<String> bad = new ArrayList<>();
        boolean[] seen = new boolean[COUNT];
        for (String[][] t : new String[][][] {GBD, DSM})
            for (String[] r : t) {
                if (r[2].isEmpty() && r[3].isEmpty()) bad.add(r[0] + " " + r[1] + ": no conditions and no reason");
                for (String n : names(r)) {
                    Aff a = BY_NAME.get(n);
                    if (a == null) bad.add(r[0] + " " + r[1] + ": no condition called \"" + n + "\"");
                    else seen[a.id] = true;
                }
            }
        for (int i = 1; i < COUNT; i++)
            if (!seen[i] && !UNLISTED.contains(TABLE[i].name)) bad.add("not in any row: " + TABLE[i].name);
        return bad;
    }

    /** Catalogue entries that are not diseases in either list: causes of death the sim needs, and
     *  complications that belong to a parent row. */
    static final java.util.Set<String> UNLISTED = java.util.Set.of(
        "Organ failure", "Old age");

    /** Why a condition cannot happen in the default world, or "" if it can. Reads only switches that are
     *  the same for everyone (Config), so per-person gates (age, sex, other conditions) are not reported. */
    static String offReason(Aff a) {
        for (int i = 0; i < a.nch; i++) {
            if (a.chMode[i] != 1) continue;               // only needs() gates
            int c = a.ch[i];
            int v;
            String what;
            if (c >= CH_ENV && c < CH_ENV + Config.NENV) { v = Config.ENV[c - CH_ENV]; what = Config.ENV_NAME[c - CH_ENV]; }
            else if (c == CH_ALCOHOL) { v = Config.ALCOHOL; what = "alcohol (Config.ALCOHOL)"; }
            else if (c == CH_OPIATES) { v = Config.OPIATES; what = "opiates (Config.OPIATES)"; }
            else if (c == CH_TOBACCO) { v = Config.TOBACCO; what = "tobacco (Config.TOBACCO)"; }
            else if (c == CH_OBESE) { v = Config.OBESITY; what = "obesity (Config.OBESITY)"; }
            else if (c == CH_MOSQUITO) { v = Config.MOSQUITO; what = "malaria mosquitoes (Config.MOSQUITO)"; }
            else if (c == CH_RODENTS) { v = Config.RODENTS; what = "plague rats (Config.RODENTS)"; }
            else continue;
            if (v == 0) return "off: needs " + what;
        }
        for (int i = 0; i < a.nafter; i++)
            if (a.afterMul[i] == -1) {
                String r = offReason(TABLE[a.afterId[i]]);
                if (!r.isEmpty()) return r + " (via " + a.afterName[i] + ")";
            }
        return "";
    }

    static void table(PrintStream o, String[][] rows, boolean dsm) {
        String chapter = "";
        o.println("| " + (dsm ? "Chapter" : "GBD code") + " | " + (dsm ? "DSM-5-TR disorder" : "GBD 2021 cause")
            + " | In the sim | Default world |");
        o.println("|---|---|---|---|");
        for (String[] r : rows) {
            String code = r[0];
            if (dsm) {
                String ch = "";
                for (String[] c : DSM_CHAPTER) if (c[0].equals(r[0])) ch = c[1];
                code = ch.equals(chapter) ? "" : ch;
                chapter = ch;
            }
            StringBuilder sim = new StringBuilder(), off = new StringBuilder();
            for (String n : names(r)) {
                if (sim.length() > 0) sim.append(", ");
                sim.append(n);
                Aff a = BY_NAME.get(n);
                String why = a == null ? "MISSING" : offReason(a);
                if (!why.isEmpty()) off.append(off.length() > 0 ? "; " : "").append(n).append(": ").append(why);
            }
            String note = r[3];
            if (sim.length() == 0) sim.append("*").append(note).append("*");
            else if (!note.isEmpty()) sim.append(" (").append(note).append(")");
            o.println("| " + code + " | " + r[1] + " | " + sim + " | " + (off.length() == 0 ? "on" : off) + " |");
        }
    }

    public static void main(String[] args) {
        PrintStream o = System.out;
        List<String> bad = check();
        if (!bad.isEmpty()) { for (String b : bad) System.err.println(b); System.exit(1); }
        int gbdOn = 0, dsmOn = 0;
        for (String[] r : GBD) if (!r[2].isEmpty() && !r[0].startsWith("(")) gbdOn++;
        for (String[] r : DSM) if (!r[2].isEmpty()) dsmOn++;
        o.println("# Coverage: what is in the sim, and why");
        o.println();
        o.println("Generated by `java -cp out meatbox.Coverage` from `src/meatbox/Coverage.java`. Do not edit by hand.");
        o.println();
        o.println("Two published lists decide what goes in. The **GBD 2021 cause list** (IHME's Global Burden of Disease study)"
            + " has " + leaves() + " leaf causes of illness, injury and death, and together they cover every ICD-10 code."
            + " The **DSM-5-TR** (American Psychiatric Association, 2022) names every recognised mental disorder."
            + " The catalogue holds " + (COUNT - 1) + " conditions.");
        o.println();
        o.println("- GBD causes with at least one sim condition: " + gbdOn + " of " + leaves()
            + ". The rest are folded into other rows, or need a system that is not there yet (drugs, transport, a state).");
        o.println("- DSM-5-TR disorders with a sim condition: " + dsmOn + " of " + DSM.length + ".");
        o.println("- \"Default world\" says whether the condition can happen in the stock temperate village. Turn a switch"
            + " on in `Config.java` (for example `E_AEDES` for dengue) to bring it in.");
        o.println("- *folded*: counted inside the named conditions. *hook*: the behaviour or social code gives the condition"
            + " when the event happens. *n/a*: cannot happen here, with the reason.");
        o.println();
        o.println("## Physical illness and injury (GBD 2021)");
        o.println();
        table(o, GBD, false);
        o.println();
        o.println("## Mental disorders (DSM-5-TR)");
        o.println();
        table(o, DSM, true);
    }
}
