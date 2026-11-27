package meatbox;

import static meatbox.Config.*;
import static meatbox.World.*;

/**
 * =============================================================================
 *  FOOD  -  wild food on the map, gathering, and the shared store.
 * =============================================================================
 *  Every tile holds some standing food (w.forage, kcal). Each day it:
 *    - grows: 15 % of the tile's yearly yield is spread evenly over the year
 *      (game can be hunted in winter), 85 % follows growing degree days, so
 *      almost nothing grows in winter and most grows in high summer,
 *    - rots a little (1/256 per day), and
 *    - is capped at FORAGE_CAP_PCT of a year's yield (berries don't pile up).
 *  The year's harvest luck (w.harvestPct) scales the growth.
 *
 *  Gatherers carry what they find to one shared tribe store (w.food).
 *  Everyone eats rations from that store in Sim.update(); when it runs short,
 *  everyone gets the same share of their need (w.foodQ).
 *
 *  Simplification: the store is one pile for the whole tribe, and carrying
 *  food back costs nothing however far away it was found.
 */
final class Food {
    private Food() {}

    static final int[] CAP = new int[FORAGE_YIELD.length];
    static {
        for (int t = 0; t < CAP.length; t++) CAP[t] = FORAGE_YIELD[t] * FORAGE_CAP_PCT / 100;
    }

    /** Grow and rot every tile for one day at outdoor temperature outT. */
    static void growDay(World w) {
        long gdd = Math.max(0, w.outT - GDD_BASE);
        int[] grow = new int[FORAGE_YIELD.length];
        for (int t = 0; t < grow.length; t++) {
            long y = FORAGE_YIELD[t];
            long g = y * FORAGE_FLOOR_PCT / (100 * 365)
                   + y * (100 - FORAGE_FLOOR_PCT) * gdd / (100 * Climate.GDD_YEAR);
            grow[t] = (int) (g * w.harvestPct / 100);
        }
        int[] f = w.forage;
        byte[] map = w.map;
        for (int i = 0; i < MAP_SIZE; i++) {
            int t = map[i];
            int v = f[i] - (f[i] >>> FORAGE_DECAY_SH) + grow[t];
            f[i] = Math.min(CAP[t], v);
        }
    }

    /** Take up to maxKcal from one tile into the store. Returns kcal taken. */
    static int gather(World w, int tile, int maxKcal) {
        int take = Math.min(maxKcal, w.forage[tile]);
        w.forage[tile] -= take;
        w.food += take;
        w.gathered += take;
        return take;
    }

    /**
     * One day of foraging by person p: own tile first, then the 8 around it.
     * Children (8-13) bring half; one usable arm or one usable leg halves it.
     * Returns kcal brought in.
     */
    static int forageAct(World w, int p, int years, int cap) {
        int amount = GATHER_ADULT;
        if (years < 14) amount >>= 1;
        if ((cap & Sim.ARMS) != Sim.ARMS) amount >>= 1;
        if ((cap & Sim.LEGS) != Sim.LEGS) amount >>= 1;
        w.gatherTry += amount;
        int px = w.x[p] & 0xFF, py = w.y[p] & 0xFF;
        int got = gather(w, py * MAP_W + px, amount);
        for (int d = 0; d < 8 && got < amount; d++) {
            int nx = px + DIR_DX[d], ny = py + DIR_DY[d];
            if (Integer.compareUnsigned(nx, MAP_W) >= 0) continue;
            if (Integer.compareUnsigned(ny, MAP_H) >= 0) continue;
            got += gather(w, ny * MAP_W + nx, amount - got);
        }
        w.gatherGot += got;
        return got;
    }

    /**
     * Step to the neighbouring tile with the most food, if it beats this one.
     * Returns false (and does not move) if no neighbour is better.
     */
    static boolean stepToFood(World w, int p) {
        int px = w.x[p] & 0xFF, py = w.y[p] & 0xFF;
        int best = w.forage[py * MAP_W + px], bx = -1, by = -1;
        for (int d = 0; d < 8; d++) {
            int nx = px + DIR_DX[d], ny = py + DIR_DY[d];
            if (Integer.compareUnsigned(nx, MAP_W) >= 0) continue;
            if (Integer.compareUnsigned(ny, MAP_H) >= 0) continue;
            int v = w.forage[ny * MAP_W + nx];
            if (v > best) { best = v; bx = nx; by = ny; }
        }
        if (bx < 0) return false;
        w.x[p] = (byte) bx;
        w.y[p] = (byte) by;
        return true;
    }

    /** Daily need in kcal before cold and refeeding. */
    static int baseNeed(World w, int p, int years) {
        int n;
        if (years < 1) n = NEED_INFANT;
        else if (years < 4) n = NEED_1_3;
        else if (years < 9) n = NEED_4_8;
        else if (years < 14) n = NEED_9_13;
        else if (years < 18) n = NEED_TEEN;
        else n = w.sex[p] == SEX_M ? NEED_M : NEED_F;
        if (years >= 60) n = n * 85 / 100;
        int pr = w.preg[p];
        if (pr > 0) n += NEED_PREG;
        else if (pr < 0) n += NEED_NURSE;          // breastfeeding (postpartum pause)
        if (w.bed[p]) n = n * 85 / 100;            // lying still burns less
        return n;
    }

    /** Days of full need a person's full reserve lasts. */
    static int reserveDays(int years) {
        return years < 1 ? RES_INFANT : years < 5 ? RES_TODDLER : years < 15 ? RES_CHILD : RES_ADULT;
    }
}
