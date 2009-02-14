@echo off

set PACKAGE=org.speechforge.zanzibar.server
set CLASS=SpeechletServerMain

start "%CLASS% - %GRAMMAR%" ..\launch %PACKAGE%.%CLASS% "pbxconfig.xml"
