# Meatbox health model (v0.4): design notes and open questions

This is the baseline body, illness and mind model meant for testing societal ethics. It aims to be realistic enough that a society's choices (who gets fed, who is cared for, how the sick, disabled, old and grieving are treated) show up in who lives, who suffers and how people feel. It is not a medical simulator.

## What is modelled

- **52 body parts**, each 0-100 %, with their own healing, ageing, oxygen need and pain weight. Vital parts kill at zero; paired parts (lungs, kidneys) kill when both are gone.
- **All 206 bones**, with strength (durability), healing speed and spinal level. Fractures come from physics (impact force vs strength x density squared), with region-specific complications: bleeding, collapsed lung, brain injury, spinal cord injury, open-fracture infection, and malunion.
- **Bone density** over a lifetime: peak at 30, age and menopause loss, bed rest, hunger and disease.
- **Blood chemistry**: haemoglobin, glucose, blood pressure and metabolic rate, on top of the v0.1 values (volume, oxygen, temperature, toxins, infection, waste, pain).
- **9 functions** the behaviour code can read: walking, each hand, sight, hearing, stamina, thinking, speech and work capacity.
- **550 conditions**, chosen from the GBD 2021 cause list and DSM-5-TR (see `docs/coverage.md`), across 18 groups: injuries, infections, heart, lungs, metabolic, digestive, kidney, blood, cancers (with spread), bones and joints, neurological, eyes and ears, skin, pregnancy, nutrition, congenital and genetic, mental and behavioural, plus emergent states (anaemia, worn joints, osteoporosis, kidney disease, heart failure, blindness, deafness, delirium) that appear when the body gets there by any route.
- **Sources of illness**: age and sex curves, risk channels (crowding, dirty water, smoke, cold, hunger, immobility, pregnancy, other conditions, contagion between nearby people), events (chopping, walking, birth, being born, bereavement, severe injury), and genes.
- **Genes**: 10 polygenic risk families inherited from both parents, and 11 single-gene conditions with proper dominant, recessive and X-linked inheritance.
- **Mind**: 8 dimensions (mood, anxiety, energy, clarity, impulsivity, aggression, social drive, psychosis), rebuilt daily from temperament, stress, pain, disability, sickness, chemistry, hunger, cold and conditions. Grief spreads to close family. Suicide is a daily hazard driven by very low mood, impulsivity and psychosis.

## What goes in, and what "every disease" means

The inclusion list is the GBD 2021 cause hierarchy (305 leaf causes, which together cover all of ICD-10) for physical illness and injury, and DSM-5-TR for mental disorders. `Coverage.java` maps every entry to its sim conditions or gives a reason, and `CoverageTest` keeps the two in step. Today 292 of 305 GBD causes and 114 of 119 DSM-5-TR disorders have conditions. The gaps are drug resistance and drug side effects (no medicine yet), transport other than roads, executions, and "other" rows folded into named conditions.

Taken literally, every disease is ICD-11's roughly 17,000 codes. Most of those are sub-types that behave the same in a body (which side, which strain, which stage). Modelling them one by one would add names, not behaviour, so the sim works at GBD's level and splits further only where the body or mind behaves differently.

**Where rates come from.** Rates and fatalities marked `[src]` or `[src~]` are recalled from WHO fact sheets, GBD summaries and the papers named in the code. The GBD results data itself (healthdata.org) could not be downloaded from this environment. Uploading a GBD Results Tool CSV (incidence and prevalence by cause, age and sex) would let every rate be set from data.

**Gender dysphoria** is in DSM-5-TR but is deliberately not modelled as an illness, following ICD-11, which moved gender incongruence out of mental disorders. Adding it is a one-line choice in `CatalogueDsm.java` if your ethics tests need it.

## Calibration (6 seeds x 300 years, about 240,000 person-years)

| Measure | Sim | Real-world reference |
|---|---|---|
| Suicide per 100k per year | 9.6 | about 9-10 worldwide (WHO 2019) |
| Fractures per 100 person-years | 2.3 | about 1-2 in modern populations [src~]; higher is plausible for outdoor labour |
| Commonest broken bones | radius, clavicle, ulna | wrist and collarbone lead in most series |
| Mean age at death, those who reach 15 | 50-56 | about 50-60 for hunter-gatherers (Gurven & Kaplan 2007) [src~] |
| Infant deaths per 1000 births | about 180-230 | about 200 pre-modern |
| Leading causes | old age, starvation, pneumonia, gastroenteritis | infections and famine dominated pre-modern deaths |

v0.4 (12 seeds x 300 years, about 380,000 person-years): suicide about 7-13 per 100k depending on the seeds (single runs of 150 people swing a lot), fractures 2.4 per 100 person-years, adult mean age at death 52-56, and every seed survives. Point prevalences checked against published figures include presbyopia 19 % (near-universal over 50), severe gum disease 17 %, tension-type headache 17 %, migraine 12 %, asthma 11 %, depression 9 %, anxiety 6 % and specific phobia 5 %.

Speed: about 600 ns per person-day with 550 conditions (v0.3: about 580 with 270). A 200-person world runs 300 years in about 13 seconds.

## Guesses that matter most

Each of these is marked `[guess]` in the code. They are the numbers most worth replacing with a source or with your own choice.

1. **Malunion**: 40 % of displaced fractures heal crooked without bone-setting, losing 5-32 % of the region's function for good.
2. **Open fractures**: 25 % of severe fractures break the skin.
3. **Burns**: rate and child weighting for open hearths.
4. **Frostbite**: 1 in 10 lose the digits.
5. **Sexually transmitted infections** (syphilis, gonorrhoea, hepatitis B): flat rates, because there is no sexual network yet.
6. **Neonatal tetanus**: depends on how the cord is cut.
7. **Environment defaults** in `Config.java`: indoor smoke, water contamination, animal bites, sunlight, UV and iodine for a pre-modern temperate village.
8. **Fatality of some conditions** (sudden cardiac death in cardiomyopathy, overdose in drug use disorder, feeding problems in severe congenital conditions).

There are also 59 `[tune]` numbers, set so the world behaves plausibly (for example the suicide base rate, stress decay, bed-rest bone loss and joint ageing).

## Open questions

1. **Medicine and care.** There is no treatment yet: no bone-setting, no herbal remedies, no caregiving. For ethics tests, care is probably the most important lever: who looks after the bedridden, the disabled and the old, and at what cost to the carer. Should the health model expose a "cared for" input (for example faster healing, fewer pressure ulcers and lower death risk when someone tends you)?
2. **Behaviour from mind.** The mind values are outputs only. The mind and needs work decides what people do with them, such as withdrawing, lashing out or failing to work. Is the 8-dimension set the right interface?
3. **Stigma and social effects.** Visible conditions (deformity, albinism, epilepsy, psychosis) could change how others treat a person. Should that live in the health code (a "visible difference" flag) or in the social code?
4. **Sexual network.** STIs, and some mental-health effects of relationships, need a partner model. Do you want one?
5. **Substances.** Alcohol, tobacco and opiates are switches in `Config.java`, not behaviour. Should using them become a choice people make (with addiction as the feedback)?
6. **Speed.** About 3x slower than v0.2. Fine for hundreds of people. For thousands, the next steps are to skip the daily function and mind rebuild for people whose state has not changed, and to flatten the hot condition fields into arrays (fetching each condition's definition is now the largest single cost).
7. **Starvation crashes.** The population still swings (for example 230 down to 50 and back). That comes from the food model and was there before the health work; the health model does not soften or worsen it much.
8. **Tropical and modern worlds.** 36 environment switches in `Config.ENV` bring in vectors, pathogens, jobs and drugs. Their defaults are a guess for a temperate pre-modern village. A different society means choosing these.

## How to change things

- **Add or change an illness**: one chain in `Catalogue.java` (or `CatalogueGbd.java`, `CatalogueDsm.java`), then name it in its row in `Coverage.java` and rerun `java -cp out meatbox.Coverage > docs/coverage.md`. Rates are per 100,000 people per year; `src(...)` records where the number came from.
- **Change a society**: the environment switches in `Config.java`.
- **Change a person's genes or a population's gene pool**: `Genes.java` (allele frequencies and risk folds).
- **Read health from behaviour code**: `w.func[p * NFN + FN_x]` (functions), `w.psy[p * NPSY + PSY_x]` (mind), `w.stress[p]`, `w.bed[p]`, `Health.workCapacity` (writes `FN_WORK`).
