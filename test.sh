#!/bin/sh
# Fixed-seed regression test, then the food/cold shortage test.
# Pass --update to accept new numbers as the regression baseline.
set -e
cd "$(dirname "$0")"
rm -rf out-test
javac --release 17 -d out-test src/meatbox/*.java test/meatbox/*.java
java -cp out-test meatbox.RegressionTest "$@"
if [ "$1" != "--update" ]; then java -cp out-test meatbox.ShortageTest; fi
