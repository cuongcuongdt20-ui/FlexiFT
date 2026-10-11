@echo off
setlocal
rem Dong goi ung dung thanh thu muc dist\FlexiFT (co FlexiFT.exe, kem san Java)
rem va file dist\FlexiFT-1.0.0-windows.zip de gui cho nguoi dung.
rem Chay trong thu muc source\:  package-app.bat

set VERSION=1.0.0

rem Tim jpackage: uu tien JAVA_HOME, sau do PATH, cuoi cung la thu muc cai JDK mac dinh.
set "JPACKAGE="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jpackage.exe" set "JPACKAGE=%JAVA_HOME%\bin\jpackage.exe"
if not defined JPACKAGE for /f "delims=" %%P in ('where jpackage 2^>nul') do if not defined JPACKAGE set "JPACKAGE=%%P"
if not defined JPACKAGE for /d %%D in ("C:\Program Files\Java\jdk-*") do if exist "%%D\bin\jpackage.exe" set "JPACKAGE=%%D\bin\jpackage.exe"
if not defined JPACKAGE (
    echo Khong tim thay jpackage. Hay cai JDK 21 tro len va dat bien JAVA_HOME.
    exit /b 1
)
echo Dung jpackage: %JPACKAGE%

echo [1/3] Build flexift.jar
call mvn -q -pl app -am clean package -DskipTests
if errorlevel 1 exit /b 1

echo [2/3] Tao thu muc dist\FlexiFT kem Java
if exist dist rmdir /s /q dist
mkdir dist\in
copy /y app\target\flexift.jar dist\in\ >nul
"%JPACKAGE%" --type app-image --name FlexiFT --app-version %VERSION% ^
    --input dist\in --main-jar flexift.jar --main-class vn.flexift.app.Launcher ^
    --add-modules java.base,java.desktop,java.logging,java.xml,jdk.unsupported ^
    --dest dist
if errorlevel 1 exit /b 1
rmdir /s /q dist\in

echo [3/3] Nen thanh dist\FlexiFT-%VERSION%-windows.zip
rem tar.exe co san trong Windows 10 tro len; -a chon dinh dang zip theo duoi file.
tar -a -c -f dist\FlexiFT-%VERSION%-windows.zip -C dist FlexiFT
if errorlevel 1 (
    echo Nen zip that bai. Thu muc dist\FlexiFT van dung duoc; hay nen tay bang chuot phai.
    exit /b 1
)

echo Xong. Gui file dist\FlexiFT-%VERSION%-windows.zip cho nguoi dung.
endlocal
