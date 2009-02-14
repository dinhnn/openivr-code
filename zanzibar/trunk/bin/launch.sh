#!/bin/bash

error() {
   echo "Error occurred"
   exit 1
}
#if [$OSTYPE != "Linux"] 
#   echo
#   echo "ERROR: this script not supported for $OS."
#   echo "You will need to modify this script for it to work with"
#   echo "your operating system."
#   echo
#   error();

#init
ZANZIBAR_VERSION=${project.version}

#startValidation
if [ -z "$1" ]
then
   echo "ERROR: improper call to launch script"
   echo "launch.bat should not be executed directly, please see README for"
   echo "proper application launching instructions."
   error
fi


#chkZanzibarHome
if [ -z "$ZANZIBAR_HOME" ]
then
   echo "ZANZIBAR_HOME not found in your environment."
   echo "Please set the ZANZIBAR_HOME variable in your environment to match the"
   echo "location of the Zanzibar installation"
   echo "using pwd"
   ZANZIBAR_HOME=$(pwd)/..
fi

#validate ZanzibarHome
ZANZIBAR_JAR="$ZANZIBAR_HOME/zanzibar-$ZANZIBAR_VERSION.jar"
if [ ! -e "$ZANZIBAR_JAR" ] ; then
   echo
   echo "ERROR: ZANZIBAR_HOME is set to an invalid directory."
   echo "$ZANZIBAR_HOME \n"
   echo "ZANZIBAR_JAR not found!"
   echo "Please set the ZANZIBAR_HOME variable in your environment to match the"
   echo "location of the ZANZIBAR installation"
   echo
   error
fi

#chkJavaHome
if [ -z "$JAVA_HOME" ] ; then 
   echo
   echo "ERROR: JAVA_HOME not found in your environment."
   echo "Please set the JAVA_HOME variable in your environment to match the"
   echo "location of your Java installation"
   echo
   error
fi
   
#valJavaHome
if [ ! -e "$JAVA_HOME/bin/java" ] ; then
   echo "ERROR: JAVA_HOME is set to an invalid directory.
   echo "JAVA_HOME = $JAVA_HOME"
   echo "Please set the JAVA_HOME variable in your environment to match the
   echo "location of your Java installation"
   error
fi

#chkJMF
if [ ! -e "$JAVA_HOME/jre/lib/ext/jmf.jar" ] ; then
if [ ! -e "$JAVA_HOME/lib/ext/jmf.jar" ] ; then
if [ ! -e "$ZANZIBAR_HOME/lib/jmf.jar" ] ; then
   echo "ERROR: Java Media Framework (JMF) is not installed."
   echo "Please download and install JMF from Sun Java web site:"
   echo "http://java.sun.com/products/java-media/jmf/"
   error
fi
fi
fi

#chkJSAPI

if [ ! -e "$JAVA_HOME/jre/lib/ext/jsapi.jar" ] ; then
if [ ! -e "$JAVA_HOME/lib/ext/jsapi.jar" ] ; then
if [ ! -e "$ZANZIBAR_HOME/lib/jsapi.jar" ] ; then

   echo
   echo "ERROR: Java Speech API (JSAPI) is not installed."
   echo "Please run jsapi.exe or jsapi.sh and place the extracted"
   echo "jsapi.jar in %JAVA_HOME%/jre/lib/ext"
   echo "The install file can be downloaded from here:"
   echo "http://www.speechforge.org/downloads/jsapi"
   echo
   error
fi
fi
fi

#setClasspath
CPATH=$ZANZIBAR_JAR
for file in $( find $ZANZIBAR_HOME -name '*.jar' |sort)
do
   # classpath delimiter different in windows
   #if [ $OS = "Windows_NT" ]; then
   #    CPATH="$CPATH;$file"
   #else 
      CPATH="$CPATH:$file"
   #fi
   #echo $file
done

# classpath delimiter different in windows
#if [ $OS = "Windows_NT" ]; then
    #CPATH="$CPATH;$ZANZIBAR_HOME/config"
#else 
   CPATH="$CPATH:$ZANZIBAR_HOME/config"
#fi

echo CPATH=$CPATH
#export CLASSPATH=$CPATH

#run
#Some command parmas that may be useful
#-XX:+UseParallelGC  -XX:+UseConcMarkSweepGC -Xincgc 
#-Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000
#-Xms100m -Xmx200m 
#-verbose:gc

"$JAVA_HOME/bin/java" -cp $CPATH  -Xmx200m  -XX:+UseConcMarkSweepGC -Dlog4j.configuration=log4j.xml -Dsun.rmi.dgc.client.gcInterval=3600000 -Dsun.rmi.dgc.server.gcInterval=3600000 "$@"
exit 0

