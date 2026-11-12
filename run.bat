@echo off
cd /d "%~dp0"
echo Starting Visual Realism Suite...
call mvn clean compile exec:java
if errorlevel 1 pause
