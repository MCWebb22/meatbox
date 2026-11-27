# Meatbox (Java)

Started as a straight port of `meatbox.asm` to Java 17+. No libraries: the window is Swing, which ships with every JDK.

v0.2 adds seasons, wild food, a shared food store, firewood heating, and what hunger and cold do to the body. See "Food, seasons and cold" below.

## Run it

| What | Command |
|---|---|
| Window | `run.bat` or `java -jar meatbox.jar` |
| 300-year statistics | `bench.bat` or `java -jar meatbox.jar --headless` |
| Any number of years | `java -jar meatbox.jar --headless 1000` |
| Rebuild after editing | `build.bat` (needs a JDK: `javac` and `jar` on PATH) |
| Regression test + shortage test | `test.bat` (or `./test.sh`) |

**Keys:** Space = pause · `+`/`-` = speed (1, 7, 30, 365 days per frame) · N/P, arrows or Tab = next/previous person · Q/Esc = quit.

## Where things live

| File | What it holds | The asm section it replaces |
|---|---|---|
| `Config.java` | every tuning number | section 1 (`equ`s) |
| `Body.java` | the 16 body parts: regen, vital, ageing, O2 need, pain | `part_table` |
| `Afflictions.java` | all 24 diseases and injuries, one chain each | `aff_table` and its macros |
| `World.java` | all state: map, people (one array per field), counters, log | `.bss`, `new_person`, `kill_person` |
| `Sim.java` | the rules: one day, one person's day, damage, disease rolls, acting, birth | sections 7 and up |
| `Rng.java` | the xorshift random generator | `rand32`, `rand_below` |
| `Screen.java` | the window: the same 120x30 layout as the console | `render`, `poll_input` |
| `Headless.java` | the statistics report | `headless_run` |
| `Climate.java` | v0.2: seasonal temperature curve, growing degree days | (new) |
| `Food.java` | v0.2: wild food on tiles, gathering, daily need, reserve size | (new) |

To add an illness, add one `add(new Aff(...))` entry in `Afflictions.java`. Its id is its position in the list.

## Proof that the port is faithful

Both versions use the same seed, the same random generator and the same order of random draws. So the 300-year headless output of Java matches the assembly line for line: same births, same deaths, the same count for every cause. `asm_reference.txt` is the assembly output; compare it with `bench.bat`. The only line that differs is `elapsed ms`.

Once you change any rule, the numbers will stop matching. That is expected: the reference only proves the port. From then on, the Java is the master copy.

## Regression test

`test.bat` (or `./test.sh` on Linux/macOS) runs the 300-year fixed-seed headless simulation and compares it line by line with `test/baseline.txt`, ignoring the timing lines. It takes about 25 seconds and exits with 1 on any difference, printing the lines that moved. GitHub runs it on every push.

The first baseline was a copy of `asm_reference.txt`. v0.2 changed the rules, so the baseline now holds the v0.2 output.

After the regression test, `ShortageTest` runs 10 other seeds for 300 years each (the tribe must never die out, and someone must starve at some point), then takes away all firewood for 5 winters (people must die of hypothermia).

When you change a rule on purpose, the test will fail. Check that the new numbers make sense, then accept them with `test.bat --update` and commit `test/baseline.txt` together with the rule change. `asm_reference.txt` stays as the record of the original port.

## Food, seasons and cold (v0.2)

Every number below lives in `Config.java`. `[src]` means it comes from a real-world figure, `[src~]` loosely so, and `[tune]` is a guess chosen to make the world behave plausibly.

**Seasons.** Day 0 is 1 January. The outdoor temperature follows a cosine (mean 8 C, about -5 C in late January and 21 C in late July) plus weather that drifts day to day, so cold snaps last about a week. Each March the year's harvest luck is drawn: 75-125 %, with 1 year in 12 a bad one at 55 %.

**Wild food.** Each tile holds standing food (kcal) that grows with growing degree days (degrees above 5 C), so almost nothing grows in winter. 15 % of a tile's yield is spread evenly across the year to stand in for hunting. Forest edge and young growth are richest, mature forest is poorest. Standing food rots slowly and is capped, so the land can't hoard a surplus. One tile is about 0.25 km2, and the yields are tuned so the map feeds roughly 150-250 people.

**Gathering and the store.** People aged 8 and up with a usable arm forage the 3x3 tiles around them while the shared store holds under 120 days of food. Children, the one-armed and the one-legged bring in half. Foragers step toward the richest neighbouring tile half the time. The store loses 1/800 a day to spoilage.

**Eating.** Everyone eats from the store every day. Need depends on age and sex (roughly FAO/WHO figures), plus pregnancy, breastfeeding and shivering in the cold. Babies under one live on their mother's milk, which dries up when she starves. When the store holds under 30 days, rations shrink to stretch it, and short rations are not shared equally: each person has a random standing, and the low-standing go hungry first. Equal sharing made everyone starve at once and wiped the tribe out. Unequal sharing lets a core survive a famine.

**Energy reserve ("fat", 0-100 %)** is what a person has stored. Eating less than need drains it. An adult's full reserve lasts about 35 days (children less). Eating enough rebuilds it slowly. It feeds the body model like this:
- under 50 %: immune clearing falls (to 40 % at zero), fertility falls (to zero at 20 %), and metabolism slows by up to 20 %
- under 30 %: healing slows (to 25 % at zero), and hunger adds pain
- under 10 %: the body wastes its own organs (heart, torso, gut, liver). If that kills, the cause is "Starvation".
- under 5 %: too weak to get up (bedridden)

Fertility also falls when foraging returns drop (gatherers finding under 95 % of what they look for). This is the early warning that the land is overused, and it softens the boom-and-bust.

**Fire and cold.** A hut plus clothing adds 8 C over the outdoor temperature. Below 12 C inside, the body cools. The tribe's fire burns firewood for cooking all year and for heating in proportion to the cold, and if the stock runs out the fire only covers part of the gap. Core temperature drops 0.25 C per degree of uncovered cold for an adult, 3x for babies, 2x for toddlers, 1.5x for over-70s, and up to 2x more for people with a low reserve. Under 35 C this adds systemic stress to the organs, under 32 C the person is bedridden, and under 30 C the heart is damaged directly. Deaths that are mostly from cold are blamed on "Hypothermia".

**Firewood and forests.** One day of chopping gives 12 loads, and there is a 1 in 12 chance it uses up the tree tile (about 150 loads per tile). People chop while the stock is under 8 loads per person. Forests regrow: 75 % of stumps resprout as saplings (coppicing), open ground gets wind-blown seed about 2.5 % a year, and seed to mature tree takes about 16 years.

**Reading the headless report.** See the comment at the top of `Headless.java` for the columns. `-Devery=1` prints every year.
