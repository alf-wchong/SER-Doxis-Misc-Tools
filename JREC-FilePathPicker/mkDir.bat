@echo off
echo Creating directory structure for JREC-FilePathPicker...

:: Create main project directory
mkdir JREC-FilePathPicker
cd JREC-FilePathPicker

:: Create Maven standard directory structure
mkdir src
mkdir src\main
mkdir src\main\java
mkdir src\main\resources
mkdir src\test
mkdir src\test\java
mkdir src\test\resources

:: Create package directory structure
mkdir src\main\java\jll
mkdir src\main\java\jll\chongwm
mkdir src\main\java\jll\chongwm\doxis
mkdir src\main\java\jll\chongwm\doxis\utility
mkdir src\main\java\jll\chongwm\doxis\utility\ui
mkdir src\main\java\jll\chongwm\doxis\utility\model
mkdir src\main\java\jll\chongwm\doxis\utility\service
mkdir src\main\java\jll\chongwm\doxis\utility\utils

:: Create test directory structure
mkdir src\test\java\jll
mkdir src\test\java\jll\chongwm
mkdir src\test\java\jll\chongwm\doxis
mkdir src\test\java\jll\chongwm\doxis\utility

:: Create resource directories
mkdir src\main\resources\images
mkdir src\main\resources\config

echo Directory structure created successfully.
echo.
echo You can now copy the pom.xml file to the JREC-FilePathPicker directory.