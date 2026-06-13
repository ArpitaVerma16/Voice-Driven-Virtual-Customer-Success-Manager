@echo off
setlocal enabledelayedexpansion

set MAVEN_PROJECTBASEDIR=%CD%
set MAVEN_HOME=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper

if not exist "%MAVEN_HOME%\maven-wrapper.jar" (
    echo Maven wrapper not found. Please install Maven manually.
    exit /b 1
)

"%JAVA_HOME%\bin\java.exe" -jar "%MAVEN_HOME%\maven-wrapper.jar" %*
if ERRORLEVEL 1 goto error
goto end

:error
echo Error occurred while running Maven
exit /b 1

:end
exit /b 0
