@echo off
rem Compile all sources into out\ and pack a double-clickable meatbox.jar
rem Needs a JDK 17 or newer (javac + jar on PATH).
if exist out rmdir /s /q out
javac --release 17 -d out src\meatbox\*.java || goto :fail
jar --create --file meatbox.jar --main-class meatbox.Main -C out . || goto :fail
echo Built meatbox.jar
goto :eof
:fail
echo BUILD FAILED
exit /b 1
