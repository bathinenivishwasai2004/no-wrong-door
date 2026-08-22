@REM Maven Wrapper script for Windows
@REM Downloads Maven if not present and runs it

@echo off
setlocal

set "MAVEN_PROJECTBASEDIR=%~dp0"
set "MAVEN_WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties"

set "DIST_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9"

@REM Read distribution URL from properties file
if exist "%MAVEN_WRAPPER_PROPERTIES%" (
    for /f "tokens=1,* delims==" %%a in ('findstr "distributionUrl" "%MAVEN_WRAPPER_PROPERTIES%"') do (
        set "DIST_URL=%%b"
    )
)

@REM Download and extract Maven if not present
if not exist "%MAVEN_HOME%\apache-maven-3.9.9\bin\mvn.cmd" (
    echo Downloading Maven distribution...
    mkdir "%MAVEN_HOME%" 2>nul

    @REM Use PowerShell to download
    powershell -Command "& { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%MAVEN_HOME%\maven.zip' }"

    @REM Extract
    powershell -Command "& { Expand-Archive -Path '%MAVEN_HOME%\maven.zip' -DestinationPath '%MAVEN_HOME%' -Force }"
    del "%MAVEN_HOME%\maven.zip" 2>nul
)

@REM Run Maven
set "MVN_CMD=%MAVEN_HOME%\apache-maven-3.9.9\bin\mvn.cmd"
if exist "%MVN_CMD%" (
    "%MVN_CMD%" %*
) else (
    echo Error: Could not find mvn.cmd in %MAVEN_HOME%
    exit /b 1
)

endlocal
