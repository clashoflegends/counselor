@echo off
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
