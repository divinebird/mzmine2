@echo off

rem This is necessary to access the TOTAL_MEMORY and ADDRESS_WIDTH variables inside the IF block
setlocal enabledelayedexpansion

rem Obtain the physical memory size and check if we are running on a 32-bit system.
if exist C:\Windows\System32\wbem\wmic.exe (
  echo Checking physical memory size...
  rem Get physical memory size from OS
  for /f "skip=1" %%p in ('C:\Windows\System32\wbem\wmic.exe os get totalvisiblememorysize') do if not defined TOTAL_MEMORY set /a TOTAL_MEMORY=%%p / 1024
  for /f "skip=1" %%x in ('C:\Windows\System32\wbem\wmic.exe cpu get addresswidth') do if not defined ADDRESS_WIDTH set ADDRESS_WIDTH=%%x
  echo Found !TOTAL_MEMORY! MB memory, !ADDRESS_WIDTH!-bit system
) else (
  echo Skipping memory size check, because wmic.exe could not be found
  set ADDRESS_WIDTH=32
)

rem The HEAP_SIZE variable defines the Java heap size in MB. 
rem That is the total amount of memory available to MZmine 2.
rem By default we set this to 1024 MB on 32-bit systems, or 
rem half of the physical memory on 64-bit systems.
rem Feel free to adjust the HEAP_SIZE according to your needs.
if %ADDRESS_WIDTH%==32 (
  set HEAP_SIZE=1024
) else (
  set /a HEAP_SIZE=%TOTAL_MEMORY% / 2
) 
echo Java heap size set to %HEAP_SIZE% MB

rem The TMP_FILE_DIRECTORY parameter defines the location where temporary 
rem files (parsed raw data) will be placed. Default is %TEMP%, which 
rem represents the system temporary directory.
set TMP_FILE_DIRECTORY=%TEMP%
rem DOS make sure to use 'short name' representation for temp directory
for %%f in ("%TMP_FILE_DIRECTORY%") do (set TMP_FILE_DIRECTORY=%%~sf)
rem rem and forward slashed (required by features not supporting spaces or '\').
rem set TMP_FILE_DIRECTORY=%TMP_FILE_DIRECTORY:\=/%
rem Make the temp working dir unique per MZmine instance.
for /f "skip=1" %%x in ('wmic os get localdatetime') do (set MZMINE_UNID="MZmine"%%x)
set TMP_FILE_DIRECTORY=%TMP_FILE_DIRECTORY%\\%MZMINE_UNID%
mkdir %TMP_FILE_DIRECTORY%


rem Set R environment variables.
set R_HOME=C:\Program Files\R\R-3.1.1
set R_SHARE_DIR=%R_HOME%\share 
set R_INCLUDE_DIR=%R_HOME%\include
set R_DOC_DIR=%R_HOME%\doc
set R_LIBS_USER=%R_HOME%\library


rem Include R DLLs in PATH.
set PATH=%PATH%;%R_HOME%\bin\i386

rem The directory holding the JRI shared library (libjri.so).
set JRI_LIB_PATH=%R_HOME%\library\rJava\jri\i386

rem It is usually not necessary to modify the JAVA_COMMAND parameter, but 
rem if you like to run a specific Java Virtual Machine, you may set the 
rem path to the java command of that JVM
set JAVA_COMMAND="C:\Program Files\Java\jdk1.7.0_65\jre\bin\java.exe"

rem It is not necessary to modify the following section
set JAVA_PARAMETERS=-showversion -classpath lib\* -Djava.ext.dirs= -XX:+UseParallelGC -Djava.io.tmpdir=%TMP_FILE_DIRECTORY% -Xms%HEAP_SIZE%m -Xmx%HEAP_SIZE%m -Djava.library.path="%JRI_LIB_PATH%"
set MAIN_CLASS=net.sf.mzmine.main.MZmineCore

rem This command starts the Java Virtual Machine
%JAVA_COMMAND% %JAVA_PARAMETERS% %MAIN_CLASS% %*


rem Kill/cleanup remaining Rserve instances
setlocal disableDelayedExpansion
:: Load the file path "array"
for /f "tokens=1* delims=:" %%A in ('dir /b %TMP_FILE_DIRECTORY%\rs_pid_*.pid^|findstr /n "^"') do (
  set "file.%%A=%TMP_FILE_DIRECTORY%\%%B"
  set "file.count=%%A"
)
:: Access the values
setlocal enableDelayedExpansion
for /l %%N in (1 1 %file.count%) do (
  echo !file.%%N!
  set /P pid=<!file.%%N!
  echo !pid!
  del !file.%%N!
  taskkill /PID !pid! /F 2>nul
)

rem If there was an error, give the user chance to see it
IF ERRORLEVEL 1 pause
