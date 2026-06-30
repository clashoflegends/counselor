@echo off
set "ROOT=%~dp0"
if exist "%ROOT%.update\dist" call :applyUpdate
start "" javaw ^
  -splash:"%~dp0dist\teaser_whs01.png" ^
  -Dsun.java2d.d3d=false ^
  -Dsun.java2d.noddraw=true ^
  -Dclash.distro=portable-jar ^
  --add-opens java.base/java.util=ALL-UNNAMED ^
  --add-opens java.base/java.lang=ALL-UNNAMED ^
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED ^
  --add-opens java.base/java.text=ALL-UNNAMED ^
  --add-opens java.base/java.util.concurrent=ALL-UNNAMED ^
  -cp "%~dp0dist\*;%~dp0dist\lib\*" client.Main %*
goto :eof

rem Apply a staged portable update (UpdateDownloader put the new dist/ at .update\dist). Runs before
rem the JVM starts so no files are locked. The old dist/ is kept as dist.old until the new one is
rem verified complete (jar + lib present); only then is the backup dropped. On any failure the broken
rem new dist is discarded and dist.old restored, so a bad swap rolls back instead of bricking.
:applyUpdate
echo Applying Counselor update...
if exist "%ROOT%dist.old" rmdir /s /q "%ROOT%dist.old"
if exist "%ROOT%dist" ren "%ROOT%dist" dist.old
move "%ROOT%.update\dist" "%ROOT%dist" >nul
set "UPDOK="
if exist "%ROOT%dist\PbmCounselor.jar" if exist "%ROOT%dist\lib" set "UPDOK=1"
if defined UPDOK (
  rmdir /s /q "%ROOT%.update" 2>nul
  if exist "%ROOT%dist.old" rmdir /s /q "%ROOT%dist.old"
) else (
  if exist "%ROOT%dist" rmdir /s /q "%ROOT%dist"
  if exist "%ROOT%dist.old" ren "%ROOT%dist.old" dist
)
goto :eof
