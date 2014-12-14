@echo off

rem Obtain the physical memory size
for /f "skip=1" %%p in ('wmic os get totalvisiblememorysize') do if not defined TOTAL_MEMORY=set TOTAL_MEMORY=%%p 

rem The HEAP_SIZE variable defines the Java heap size in MB. 
rem That is the total amount of memory available to MZmine 2.
rem By default we set this to the half of the physical memory 
rem size, but feel free to adjust according to your needs. 
set /a HEAP_SIZE=%TOTAL_MEMORY% / 1024 / 2

rem Check if we are running on a 32-bit system. 
rem If yes, force the heap size to 1024 MB.
for /f "skip=1" %%x in ('wmic cpu get addresswidth') do if not defined ADDRESS_WIDTH set ADDRESS_WIDTH=%%x
if %ADDRESS_WIDTH%==32 (
  set HEAP_SIZE=1024
) 

rem The TMP_FILE_DIRECTORY parameter defines the location where temporary 
rem files (parsed raw data) will be placed. Default is %TEMP%, which 
rem represents the system temporary directory.
set TMP_FILE_DIRECTORY=%TEMP%

rem DOS short name for current directory, forward slashed.
set CD_SHORT_INV=
for %%f in ("%cd%") do (set CD_SHORT_INV=%%~sf)
set CD_SHORT_INV=%CD_SHORT_INV:\=/%
rem set CD_SHORT_INV


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
set JAVA_PARAMETERS=-XX:+UseParallelGC -Djava.io.tmpdir=%TMP_FILE_DIRECTORY% -Xms%HEAP_SIZE%m -Xmx%HEAP_SIZE%m -Djava.library.path="%JRI_LIB_PATH%"
set CLASS_PATH=lib\MZmine-2.11-EEE.jar
set MAIN_CLASS=net.sf.mzmine.main.MZmineCore

rem Show java version, in case a problem occurs
%JAVA_COMMAND% -version

rem This command starts the Java Virtual Machine
%JAVA_COMMAND% %JAVA_PARAMETERS% -classpath %CLASS_PATH% %MAIN_CLASS% %*


rem Kill/cleanup remaining Rserve instances
setlocal disableDelayedExpansion
:: Load the file path "array"
for /f "tokens=1* delims=:" %%A in ('dir /b *.txt^|findstr /n "^"') do (
  set "file.%%A=%%B"
  set "file.count=%%A"
)
:: Access the values
setlocal enableDelayedExpansion
for /l %%N in (1 1 %file.count%) do (
  echo !file.%%N!
  set /P pid=<!file.%%N!
  echo !pid!
  del !file.%%N!
  taskkill /PID !pid! /F
)


rem If there was an error, give the user chance to see it
IF ERRORLEVEL 1 pause
