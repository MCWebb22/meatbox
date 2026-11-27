# Meatbox (Java)

A straight port of `meatbox.asm` to Java 17+. No libraries: the window is Swing, which ships with every JDK.

## Run it

| What | Command |
|---|---|
| Window | `run.bat` or `java -jar meatbox.jar` |
| 300-year statistics | `bench.bat` or `java -jar meatbox.jar --headless` |
| Any number of years | `java -jar meatbox.jar --headless 1000` |
| Rebuild after editing | `build.bat` (needs a JDK: `javac` and `jar` on PATH) |
| Regression test | `test.bat` (or `./test.sh`) |

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

To add an illness, add one `add(new Aff(...))` entry in `Afflictions.java`. Its id is its position in the list.

## Proof that the port is faithful

Both versions use the same seed, the same random generator and the same order of random draws. So the 300-year headless output of Java matches the assembly line for line: same births, same deaths, the same count for every cause. `asm_reference.txt` is the assembly output; compare it with `bench.bat`. The only line that differs is `elapsed ms`.

Once you change any rule, the numbers will stop matching. That is expected: the reference only proves the port. From then on, the Java is the master copy.

## Regression test

`test.bat` (or `./test.sh` on Linux/macOS) runs the 300-year fixed-seed headless simulation and compares it line by line with `test/baseline.txt`, ignoring the timing lines. It takes about 25 seconds and exits with 1 on any difference, printing the lines that moved. GitHub runs it on every push.

The first baseline is a copy of `asm_reference.txt`, so while the test passes, the Java still matches the assembly exactly.

When you change a rule on purpose, the test will fail. Check that the new numbers make sense, then accept them with `test.bat --update` and commit `test/baseline.txt` together with the rule change. `asm_reference.txt` stays as the record of the original port.
