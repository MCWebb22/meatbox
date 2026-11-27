package meatbox;

import static meatbox.Config.*;

/**
 * =============================================================================
 *  CLIMATE  -  the seasons.  EDIT Config (climate section) to change them.
 * =============================================================================
 *  MEAN[day of year] = the long-run average outdoor temperature: a cosine with
 *  its coldest point on CLIM_COLD_DAY. Each day the world adds a "weather
 *  anomaly" that drifts (keeps 7/8 of itself, plus a random kick), so cold
 *  snaps and warm spells last about a week instead of flipping daily.
 *
 *  Plants grow in proportion to "growing degree days": degrees above 5 C.
 *  GDD_YEAR is the average year's total, so one average year of growth adds
 *  up to exactly one FORAGE_YIELD.
 */
final class Climate {
    private Climate() {}

    static final int[] MEAN = new int[365];
    static final long GDD_YEAR;

    static {
        long g = 0;
        for (int d = 0; d < 365; d++) {
            double a = 2 * Math.PI * (d - CLIM_COLD_DAY) / 365.0;
            MEAN[d] = (int) Math.round(CLIM_MEAN - CLIM_AMP * Math.cos(a));
            g += Math.max(0, MEAN[d] - GDD_BASE);
        }
        GDD_YEAR = g;
    }

    static final String[] SEASON = {"Winter", "Spring", "Summer", "Autumn"};

    /** 0 winter (Dec-Feb), 1 spring, 2 summer, 3 autumn. Day 0 = 1 January. */
    static int season(int doy) {
        int m = doy * 12 / 365;              // month 0..11
        return ((m + 1) % 12) / 3;
    }
}
