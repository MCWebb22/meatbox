#!/bin/sh
# Fixed-seed regression test. Pass --update to accept new numbers as the baseline.
set -e
cd "$(dirname "$0")"
rm -rf out-test
javac --release 17 -d out-test src/meatbox/*.java test/meatbox/*.java
exec java -cp out-test meatbox.RegressionTest "$@"
