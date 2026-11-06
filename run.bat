@echo off
REM ============================================================
REM  YEYODRA - Game Launcher (pure Java + SQLite)
REM  Klik dua kali. Driver di-download otomatis kalau belum ada.
REM ============================================================
cd /d "%~dp0"

if not exist lib mkdir lib

if not exist "lib\sqlite-jdbc-3.47.1.0.jar" (
    echo Downloading SQLite JDBC driver...
    curl.exe -L --max-time 180 -o "lib\sqlite-jdbc-3.47.1.0.jar" "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.47.1.0/sqlite-jdbc-3.47.1.0.jar"
)
if not exist "lib\slf4j-api-2.0.16.jar" (
    curl.exe -L --max-time 120 -o "lib\slf4j-api-2.0.16.jar" "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.16/slf4j-api-2.0.16.jar"
)
if not exist "lib\slf4j-simple-2.0.16.jar" (
    curl.exe -L --max-time 120 -o "lib\slf4j-simple-2.0.16.jar" "https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.16/slf4j-simple-2.0.16.jar"
)

echo Compile...
if not exist bin mkdir bin
dir /s /b src\*.java > sources.txt
javac -cp "lib\*" -d bin @sources.txt
del sources.txt
if errorlevel 1 (
    echo [GAGAL] Ada error saat compile.
    pause
    exit /b
)

echo Jalankan Yeyodra...
java --enable-native-access=ALL-UNNAMED -cp "bin;lib\*" yeyodra.Main
