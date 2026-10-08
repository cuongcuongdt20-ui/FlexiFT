@echo off
setlocal
rem Chay cac thuc nghiem E1..E4 va ghi ket qua vao ..\statics\results\*.csv
rem May nhan chay truoc:  java -jar cli\target\flexift-cli.jar recv --port 7777 --dir nhan
rem May gui chay:         run-experiments.bat <dia-chi-may-nhan> [dung-luong, mac dinh 100M] [so lan lap, mac dinh 3]

if "%~1"=="" (
    echo Cach dung: run-experiments.bat ^<dia-chi-may-nhan^> [dung-luong] [so-lan-lap]
    exit /b 1
)
set HOST=%~1
set SIZE=%~2
if "%SIZE%"=="" set SIZE=100M
set REPEAT=%~3
if "%REPEAT%"=="" set REPEAT=3

set CLI=java -jar cli\target\flexift-cli.jar
set OUT=..\statics\results
set FILE=%TEMP%\flexift-test-%SIZE%.bin

if not exist cli\target\flexift-cli.jar (
    echo Chua co cli\target\flexift-cli.jar. Hay chay: mvn clean package
    exit /b 1
)
%CLI% gen --file "%FILE%" --size %SIZE%
if errorlevel 1 exit /b 1

echo === E1: tuan tu va song song 2, 4, 8 kenh ===
for /l %%R in (1,1,%REPEAT%) do (
    %CLI% send --host %HOST% --file "%FILE%" --mode SEQ --csv %OUT%\e1.csv --label seq
    for %%C in (2 4 8) do %CLI% send --host %HOST% --file "%FILE%" --mode PAR --channels %%C --csv %OUT%\e1.csv --label par%%C
)

echo === E2: che do tu dong ===
for /l %%R in (1,1,%REPEAT%) do %CLI% send --host %HOST% --file "%FILE%" --mode AUTO --csv %OUT%\e2.csv --label auto

echo === E3: dut ket noi o 30, 60, 90 phan tram roi tu noi lai ===
for /l %%R in (1,1,%REPEAT%) do (
    for %%P in (30 60 90) do %CLI% send --host %HOST% --file "%FILE%" --mode PAR --channels 4 --drop-at %%P --csv %OUT%\e3.csv --label drop%%P
)

echo === E4: khoi bi hong voi xac suat 1, 5, 10 phan tram ===
for /l %%R in (1,1,%REPEAT%) do (
    for %%P in (0.01 0.05 0.10) do %CLI% send --host %HOST% --file "%FILE%" --mode PAR --channels 4 --corrupt %%P --csv %OUT%\e4.csv --label corrupt%%P
)

echo Xong. Ket qua nam trong %OUT%
endlocal
