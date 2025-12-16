@echo off
rem --------------------------------------------------------------------------------
rem  SpringBoot App Launcher & Chrome Auto-Start
rem --------------------------------------------------------------------------------

rem Set console encoding to UTF-8
chcp 65001 > nul

rem --------------------------------------------------------
rem  Set Java Path (Pleiades Environment)
rem --------------------------------------------------------
set "JAVA_HOME=C:\pleiades\2025-06\java\21"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo ========================================================
echo  Starting AdminMainApplication...
echo.
echo  * Chrome (Kiosk Mode) will launch in 10 seconds.
echo  * To close Chrome: Press [Alt] + [F4]
echo ========================================================

rem --------------------------------------------------------
rem  Chrome Launcher (Background Timer)
rem  - Waits 10 seconds
rem  - Uses independent user profile (--user-data-dir)
rem  - Starts in Kiosk mode (--kiosk)
rem --------------------------------------------------------
start "Chrome Launcher" /min cmd /c "timeout 10 >nul & start """" chrome --user-data-dir=""%TEMP%\admin_main_kiosk"" --no-first-run --kiosk http://localhost:8080/alert"

rem --------------------------------------------------------
rem  Run Application (JAR)
rem --------------------------------------------------------
rem Check Java version
java -version
echo.

rem Execute JAR
java -jar admin_main-0.0.1-SNAPSHOT.jar

rem Keep window open after exit
pause