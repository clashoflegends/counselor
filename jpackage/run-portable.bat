@echo off
rem ============================================================================
rem  Counselor - portable launcher (antivirus-safe)
rem
rem  Use this if your antivirus quarantines or deletes "Counselor.exe".
rem  Counselor.exe is an UNSIGNED jpackage launcher stub, which some corporate
rem  antivirus tools wrongly flag as a trojan (a known false positive). This
rem  script instead starts Counselor with the bundled, CODE-SIGNED Java runtime
rem  (runtime\bin\javaw.exe, signed by the Eclipse Adoptium / Temurin project),
rem  which antivirus does not flag. No separate Java installation is required.
rem
rem  How to use: keep this file inside the unzipped Counselor folder (next to the
rem  app\ and runtime\ folders) and double-click it. You can also drag a .egf
rem  turn file onto it to open that file directly.
rem
rem  Maintainer note: keep the Java options below in sync with the "Run jpackage
rem  app-image (Windows)" step in .github/workflows/jpackage.yml (source of truth).
rem ============================================================================
setlocal
set "ROOT=%~dp0"
set "JAVAW=%ROOT%runtime\bin\javaw.exe"
if not exist "%JAVAW%" set "JAVAW=%ROOT%runtime\bin\java.exe"
if not exist "%JAVAW%" (
  echo.
  echo  Could not find the bundled Java runtime under:
  echo    "%ROOT%runtime\bin"
  echo  Make sure you unzipped the ENTIRE windows-portable archive, not just this file.
  echo.
  pause
  exit /b 1
)
start "" "%JAVAW%" ^
  -splash:"%ROOT%app\teaser_whs01.png" ^
  -Xmx2048M ^
  -Dsun.java2d.d3d=false ^
  -Dsun.java2d.noddraw=true ^
  -Dclash.distro=windows-portable-safe ^
  --add-opens java.base/java.util=ALL-UNNAMED ^
  --add-opens java.base/java.lang=ALL-UNNAMED ^
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED ^
  --add-opens java.base/java.text=ALL-UNNAMED ^
  --add-opens java.base/java.util.concurrent=ALL-UNNAMED ^
  -cp "%ROOT%app\PbmCounselor.jar;%ROOT%app\lib\*" client.Main %*
endlocal
