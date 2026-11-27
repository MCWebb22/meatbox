package meatbox;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * =============================================================================
 *  REGRESSION TEST  -  pin the fixed-seed headless output
 * =============================================================================
 *  Runs the same 300-year, fixed-seed headless simulation as  bench.bat  and
 *  compares it line by line with  test/baseline.txt . Timing lines
 *  ("elapsed ms", "ns per person-day") are ignored because they depend on the
 *  machine.
 *
 *  The first baseline is a copy of  asm_reference.txt , the assembly version's
 *  output. So while this passes, the Java still matches the asm exactly.
 *
 *  When you change a rule ON PURPOSE the numbers will move. Check the new
 *  numbers look right, then accept them as the new baseline:
 *      test.bat --update        (or  ./test.sh --update )
 *  and commit test/baseline.txt together with the rule change.
 *
 *  No libraries: run with  test.bat  /  test.sh . Exit code 0 = pass, 1 = fail.
 */
public final class RegressionTest {
    static final int YEARS = 300;
    static final Path BASELINE = Path.of("test", "baseline.txt");

    public static void main(String[] args) throws Exception {
        boolean update = args.length > 0 && args[0].equals("--update");

        List<String> actual = run(Main.FIXED_SEED, YEARS);

        if (update) {
            Files.writeString(BASELINE, String.join("\n", actual) + "\n", StandardCharsets.UTF_8);
            System.out.println("Baseline updated: " + BASELINE + " (" + actual.size() + " lines)");
            return;
        }

        List<String> expected = stable(Files.readString(BASELINE, StandardCharsets.UTF_8));
        int n = Math.max(expected.size(), actual.size());
        int diffs = 0;
        for (int i = 0; i < n; i++) {
            String e = i < expected.size() ? expected.get(i) : "<missing>";
            String a = i < actual.size()   ? actual.get(i)   : "<missing>";
            if (!e.equals(a)) {
                if (diffs == 0) System.out.println("FAIL: output differs from " + BASELINE);
                if (diffs < 20) {
                    System.out.println("  line " + (i + 1) + ":");
                    System.out.println("    expected: " + e);
                    System.out.println("    actual:   " + a);
                }
                diffs++;
            }
        }
        if (diffs > 0) {
            if (diffs > 20) System.out.println("  ... and " + (diffs - 20) + " more");
            System.out.println("If this change was on purpose, accept it with: test.bat --update");
            System.exit(1);
        }
        System.out.println("PASS: " + YEARS + "-year fixed-seed run matches " + BASELINE
                + " (" + actual.size() + " lines)");
    }

    /** One headless run, captured, with the machine-dependent lines removed. */
    static List<String> run(long seed, int years) {
        World w = Main.newWorld(seed);
        Sim sim = new Sim(w);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (PrintStream ps = new PrintStream(buf, true, StandardCharsets.UTF_8)) {
            Headless.run(w, sim, years, ps);
        }
        return stable(buf.toString(StandardCharsets.UTF_8));
    }

    /** Split into lines (CRLF or LF) and drop timing lines and trailing blanks. */
    static List<String> stable(String text) {
        List<String> out = new ArrayList<>();
        for (String line : text.split("\r?\n", -1)) {
            if (line.startsWith("elapsed ms:") || line.startsWith("ns per person-day:")) continue;
            out.add(line);
        }
        while (!out.isEmpty() && out.get(out.size() - 1).isEmpty()) out.remove(out.size() - 1);
        return out;
    }
}
