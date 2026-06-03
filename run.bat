@echo off
java ^
  --add-opens java.base/java.util=ALL-UNNAMED ^
  --add-opens java.base/java.lang=ALL-UNNAMED ^
  --add-opens java.base/java.lang.reflect=ALL-UNNAMED ^
  --add-opens java.base/java.text=ALL-UNNAMED ^
  --add-opens java.base/java.util.concurrent=ALL-UNNAMED ^
  -jar "%~dp0dist\PbmCounselor.jar" %*
