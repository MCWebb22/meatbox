# Meatbox health model (v0.3): design notes and open questions

This is the baseline body, illness and mind model meant for testing societal ethics. It aims to be realistic enough that a society's choices (who gets fed, who is cared for, how the sick, disabled, old and grieving are treated) show up in who lives, who suffers and how people feel. It is not a medical simulator.

## What is modelled

- **48 body parts**, each 0-100 %, with their own healing, ageing, oxygen need and pain weight. Vital parts kill at zero; paired parts (lungs, kidneys) kill when both are gone.
- **All 206 bones**, with strength (durability), healing speed and spinal level. Fractures come from physics (impact force vs strength x density squared), with region-specific complications: bleeding, collapsed lung, brain injury, spinal cord injury, open-fracture infection, and malunion.
- **Bone density** over a lifetime: peak at 30, age and menopause loss, bed rest, hunger and disease.
- **Blood chemistry**: haemoglobin, glucose, blood pressure and metabolic rate, on top of the v0.1 values (volume, oxygen, temperature, toxins, infection, waste, pain).
- **9 functions** the behaviour code can read: walking, each hand, sight, hearing, stamina, thinking, speech and work capacity.
- **About 270 conditions** across 18 groups: injuries, infections, heart, lungs, metabolic, digestive, kidney, blood, cancers (with spread), bones and joints, neurological, eyes and ears, skin, pregnancy, nutrition, congenital and genetic, mental and behavioural, plus emergent states (anaemia, worn joints, osteoporosis, kidney disease, heart failure, blindness, deafness, delirium) that appear when the body gets there by any route.
- **Sources of illness**: age and sex curves, risk channels (crowding, dirty water, smoke, cold, hunger, immobility, pregnancy, other conditions, contagion between nearby people), events (chopping, walking, birth, being born, bereavement, severe injury), and genes.
- **Genes**: 10 polygenic risk families inherited from both parents, and 10 single-gene conditions with proper dominant, recessive and X-linked inheritance.
- **Mind**: 8 dimensions (mood, anxiety, energy, clarity, impulsivity, aggression, social drive, psychosis), rebuilt daily from temperament, stress, pain, disability, sickness, chemistry, hunger, cold and conditions. Grief spreads to close family. Suicide is a daily hazard driven by very low mood, impulsivity and psychosis.

## Calibration (6 seeds x 300 years, about 240,000 person-years)

| Measure | Sim | Real-world reference |
|---|---|---|
| Suicide per 100k per year | 9.6 | about 9-10 worldwide (WHO 2019) |
| Fractures per 100 person-years | 2.3 | about 1-2 in modern populations [src~]; higher is plausible for outdoor labour |
| Commonest broken bones | radius, clavicle, ulna | wrist and collarbone lead in most series |
| Mean age at death, those who reach 15 | 50-56 | about 50-60 for hunter-gatherers (Gurven & Kaplan 2007) [src~] |
| Infant deaths per 1000 births | about 180-230 | about 200 pre-modern |
| Leading causes | old age, starvation, pneumonia, gastroenteritis | infections and famine dominated pre-modern deaths |

Speed: about 550 ns per person-day, against about 200 for v0.2. A 200-person world runs 300 years in about 10 seconds.

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
6. **Speed.** About 2.7x slower than v0.2. Fine for hundreds of people. For thousands, the next step is to skip the daily function and mind rebuild for people whose state has not changed.
7. **Starvation crashes.** The population still swings (for example 230 down to 50 and back). That comes from the food model and was there before the health work; the health model does not soften or worsen it much.

## How to change things

- **Add or change an illness**: one chain in `Catalogue.java`. Rates are per 100,000 people per year; `src(...)` records where the number came from.
- **Change a society**: the environment switches in `Config.java`.
- **Change a person's genes or a population's gene pool**: `Genes.java` (allele frequencies and risk folds).
- **Read health from behaviour code**: `w.func[p * NFN + FN_x]` (functions), `w.psy[p * NPSY + PSY_x]` (mind), `w.stress[p]`, `w.bed[p]`, `Health.workCapacity` (writes `FN_WORK`).
