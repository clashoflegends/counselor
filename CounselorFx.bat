@echo off
del counselor.log.2*.log 

SET FX_LIBS=javafx-sdk-14/lib
SET	FX_MODULES=javafx.controls,javafx.swing

for /f tokens^=2-5^ delims^=.-_^" %%j in ('java.exe -fullversion 2^>^&1') do set "jver=%%j%%k%%l%%m"
echo %jver%
IF %jver% EQU 8 (
	start javaw -Xmx512M -jar Counselor.jar %1
) ELSE (

	IF EXIST %FX_LIBS% (
		start javaw  --module-path %FX_LIBS% --add-modules %FX_MODULES% -Xmx512M -jar Counselor.jar %1
	
	) ELSE (
		CALL:DisplayErrorMessage
		
	)
	
)

IF %ERRORLEVEL% NEQ 0 Echo An error was found
EXIT /B 0


:DisplayErrorMessage
	echo msgbox "There is a problem with the Counselor installation. Please, download a fresh Counselor from https://sites.google.com/site/clashlegends/file-cabinet.", 16, "Error message" > %tmp%\tmp.vbs		
	cscript %tmp%\tmp.vbs
	del %tmp%\tmp.vbs
EXIT /B 1