@echo off
REM ============================================
REM  YEYODRA - Hot now carousel (pure Java)
REM  Klik dua kali untuk compile & jalankan.
REM ============================================
cd /d "%~dp0"

echo Compile...
if not exist bin mkdir bin
dir /s /b src\*.java > sources.txt
javac -d bin @sources.txt
del sources.txt
if errorlevel 1 (
    echo [GAGAL] Ada error saat compile.
    pause
    exit /b
)

echo Jalankan...
java -cp bin yeyodra.Main
