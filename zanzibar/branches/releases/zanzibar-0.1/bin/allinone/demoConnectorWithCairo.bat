@echo off

set PACKAGE=org.speechforge.zanzibar.server
set CLASS=SpeechletServerMain

start "%CLASS%" ..\launch %PACKAGE%.%CLASS% "democonfig-withcairo.xml"
