@echo off
setlocal

set JAVA_HOME=D:\Program Files\Java\jdk-21
set MAVEN_HOME=D:\spotter\apache-maven-3.9.11
set PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%

cd /d "%~dp0"
cd backend\app

echo 正在编译并运行应用...
"%MAVEN_HOME%\bin\mvn.cmd" -s "%MAVEN_HOME%\conf\settings.xml" -Dmaven.repo.local=D:\m2\repository spring-boot:run

pause

