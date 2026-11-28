package meatbox;

import java.util.List;

/**
 * =============================================================================
 *  COVERAGE TEST  -  the inclusion lists and the catalogue agree
 * =============================================================================
 *  Every GBD 2021 cause and DSM-5-TR disorder in Coverage.java must name real
 *  conditions (or give a reason for having none), and every condition in the
 *  catalogue must appear in some row. Exit 0 = pass.
 */
public final class CoverageTest {
    public static void main(String[] args) {
        List<String> bad = Coverage.check();
        for (String b : bad) System.out.println("COVERAGE: " + b);
        System.out.println(bad.isEmpty() ? "coverage: OK" : "coverage: " + bad.size() + " problem(s)");
        if (!bad.isEmpty()) System.exit(1);
    }
}
