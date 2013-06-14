@REM
@echo off

REM test id JAVA_HOME is defined
if NOT DEFINED JAVA_HOME goto err

REM get the CASSANDRA_UNIT_HOME
set CASSANDRA_UNIT_HOME=%~dp0..

REM compute the CLASSPATH
set CLASSPATH="%CASSANDRA_UNIT_HOME%\conf"
for %%i in ("%CASSANDRA_UNIT_HOME%\lib\*.jar") do call :append %%~fi
goto runCli

:append
set CLASSPATH=%CLASSPATH%;%1%2
goto :eof

:runCli
"%JAVA_HOME%\bin\java" -Dlog4j.configuration=log4j-cu-loader.xml -cp "%CLASSPATH%" org.cassandraunit.cli.CassandraUnitCommandLineLoader %*
goto finally


:err
echo JAVA_HOME environment variable must be set!
pause

:finally
