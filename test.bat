@echo off
rem Fixed-seed regression test, then the food/cold shortage test. Pass --update to accept new numbers as the baseline.
rem Needs a JDK 17 or newer (javac on PATH).
if exist out-test rmdir /s /q out-test
javac --release 17 -d out-test src\meatbox\*.java test\meatbox\*.java || goto :fail
java -cp out-test meatbox.RegressionTest %*
if errorlevel 1 exit /b 1
if "%1"=="--update" exit /b 0
java -cp out-test meatbox.ShortageTest
exit /b %errorlevel%
:fail
echo BUILD FAILED
exit /b 1
