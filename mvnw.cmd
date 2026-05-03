@ECHO OFF
SETLOCAL

SET MAVEN_PROJECTBASEDIR=%~dp0
IF "%MAVEN_PROJECTBASEDIR:~-1%"=="\" SET MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%

IF NOT EXIST "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar" (
  ECHO Maven wrapper jar not found.
  EXIT /B 1
)

IF "%JAVA_HOME%"=="" (
  SET JAVA_EXEC=java
) ELSE (
  SET JAVA_EXEC=%JAVA_HOME%\bin\java
)

"%JAVA_EXEC%" -classpath "%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar" -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" -Dmaven.home="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper" -Dmaven.wrapper.properties="%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.properties" org.apache.maven.wrapper.MavenWrapperMain %*

ENDLOCAL
