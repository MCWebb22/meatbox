package meatbox;

/**
 * =============================================================================
 *  RNG  -  xorshift64, the exact same generator as meatbox.asm.
 * =============================================================================
 *  Same seed + same algorithm + same order of calls = the Java version
 *  produces exactly the same world as the assembly version. That is how the
 *  port was checked: both print identical 300-year statistics.
 *
 *  Java has no unsigned int, so a 32-bit random number is returned as a
 *  long in the range 0 .. 4,294,967,295. Comparing two longs then works
 *  like the assembly's unsigned "jb" / "jae".
 */
final class Rng {
    private long state;

    Rng(long seed) {
        state = (seed == 0) ? 1 : seed;     // xorshift must never be 0
    }

    /** 0 .. 2^32-1. Three shifts and three XORs, nothing else. */
    long next32() {
        long x = state;
        x ^= x << 13;
        x ^= x >>> 7;                       // >>> = shift without sign (asm "shr")
        x ^= x << 17;
        state = x;
        return x >>> 32;                    // top 32 bits are the best quality
    }

    /** 0 .. n-1 without dividing: (random * n) >> 32  (Lemire's method). */
    int below(int n) {
        return (int) ((next32() * n) >>> 32);
    }
}
