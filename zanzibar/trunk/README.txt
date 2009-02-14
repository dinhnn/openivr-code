=====================================================
Readme for Zanzibar Speech Application Server v${project.version}
=====================================================

Overview
--------
This is the first release of the Zanzibar Speech Application Server.  
Zanzibar provides the framwork to deploy and run speech applications.   
Zanzibar integrates with telphony platforms via SIP
and RTP.  Speech applications use mrcpv2 to obtain and use speech 
resources.  You can write speech applications in java using the MRCP4j library 
or the higher level cairo-client speech library.  Zanzibar also provides the 
capability to deploy and run your speech applications using VoiceXML. 


Limitations for Zanzibar v${project.version}
--------------------------
This is the first release of zanzibar.  See General limitation section.

General limitations of this release:
------------------------------------
* Thread pooling and Object pooling is not supported
* JSGF is supported but other grammar languages are not.
* NLSML is not supported (although a simple semantic representation is extracted from the raw results based on the grammar)
* Multiple grammars are not supported
* Queueing recogniton requests is not supported
* DTMF signals in the rtp stream is not supported (although DTMF via SIP Info messages is supported)

Prerequisites
-------------

1. Zanzibar requires an MRCPv2 compliant speech server (such as cairo-server) to be installed in a network accessible location.

2. Zanzibar requires Java Runtime Environment (JRE) 5.0 or higher which can be downloaded here:

  http://java.sun.com/javase/downloads/

If you have not already, you will need to set your JAVA_HOME environment variable to point to the installed location of your JRE/JDK.


Installation
------------

1. Extract Zanzibar

  To install Zanzibar, extract all files from the binary distribution archive to a directory of your choosing.

2. Download and Install JMF 2.1.1

  Cairo requires Java Media Framework (JMF) version 2.1.1. which can be downloaded here:

  http://java.sun.com/products/java-media/jmf/2.1.1/download.html

  Download and run the JMF installer that corresponds to your specific operating system.  This will install jmf.jar and sound.jar to 
  the lib/ext directory of your installed JRE(s) as well as performing the configurations specific to your operating system.

3. Install JSAPI

  Run the JSAPI installer found in the lib directory of your installation (either jsapi.exe or jsapi.sh depending upon your operating 
  system) and accept the Sun Microsystems license agreement.  This will extract the jsapi.jar to your Cairo lib directory.  (If you run the 
  JSAPI installer from a different directory you will need to move the jsapi.jar from that directory to your Cairo installation's lib directory 
  in order for it to be included in the Cairo classpath.)

  Note: Extracting jsapi.jar to the lib directory is sufficient for this single Cairo installation.  However to avoid this step during 
  future installations you can permanently install JSAPI by moving the jsapi.jar to the lib/ext directory of your installed JRE(s).

4.  To run speech application demos. you will need a sip phone.  Xlite works well for this purpose. To configure xlite:

   +------------------------------------------------------------------------------------------+
   |    * domain=localhost                                                                    |
   |    * uncheck register with domain and receive incoming calls                             |
   |    * check proxy with your address and port of the machine running zanzibar sip service  |
   |    * ip address use localhost                                                            |
   +------------------------------------------------------------------------------------------+


Getting Started
---------------

Once Zanzibar is successfully installed (and you have an MRCPv2 server -- like cairo-server) you can follow the instructions below to run the demos.

Note: You must run an MRCPv2 server for the demos to work.  If you are using cairo-server, see the cairo-server "Getting Started" instructions for starting up the server processes.


Starting the Zanzibar Server
----------------------------
Zanzibar can be run in two different configurations.  Asterisk integration mode or demo mode.

In demo mode you can run the demos using a sip phone communicating directly with zanzibar speech application server.  In asterisk mode you 
call the asterisk pbx which can route the call (via the dialplan) to the speech application server.  Note that you can use any pbx or other 
telephony platform that uses SIP and RTP for signalling and streaming respectively WITH THE ONE EXCEPTION OF TRANSFERING CALLS.  At present 
we are using the Asterisk Manager Interface (AMI) for redirecting calls using Asterisk-java.  In the future we plan to do this with SIP and 
support call redirection in standard way too.

Launching Server Process
--------------------------
The Speech Application Server process is started by passing appropriate parameters to the launch.bat (or launch.sh) script in the bin subdirectory
of your installation.  However the launch.bat script should not be invoked directly.  Instead batch files (and shell scripts) are supplied for starting 
the server in either of the preconfigured modes:  Asterisk Integration Mode and Demo mode.

Note that the only diffrence in the asteriskConnector and demoConnector scripts is the configuration xml file passed in on the command line.
 
Configuring the Server
----------------------
The server is configured via a spring configuration file that is passed in on the command line. 
  
You must configure the sip service.  the sip services is used for both receiving incoming calls and setting up MRCPv2 channels.  Below is an example of 
the SipService configuration.  This is where you specify where to find the mrcp Server (cairoSipHostName and cairoSipPort).  Also specify the sip 
transport (UDP or TCP).  Note that the same transport is used for both mrcp server and for incoming calls (pbx).  The port that the service uses for 
incoming calls and requests from the mrcp server  (the paramater port).
     
  +---------------------------------------------------------------------------------------+
  |	<bean id="sipService" class="org.speechforge.zanzibar.sip.SipServer"                  |
  |		init-method="startup" destroy-method="shutdown">                                  |
  |		<property name="dialogService"><ref bean="dialogService"/></property>             |
  |		<property name="mySipAddress">                                                    |
  |				    <value>sip:cairogate@speechforge.org</value>                          |
  |		</property>                                                                       |
  |		<property name="stackName">                                                       |
  |				    <value>Sip Stack</value>                                              |
  |		</property>                                                                       |
  |		<property name="port">                                                            |
  |				    <value>5090</value>                                                   |
  |		</property>                                                                       |
  |		<property name="transport">                                                       |
  |				    <value>UDP</value>                                                    |
  |		</property>                                                                       |
  |		<property name="cairoSipAddress">                                                 |
  |				    <value>sip:cairo@speechforge.org</value>                              |
  |		</property>                                                                       |
  |		<property name="cairoSipHostName">                                                |
  |				    <value>localhost</value>                                              |
  |		</property>                                                                       |
  |		<property name="cairoSipPort">                                                    |
  |				    <value>5050</value>                                                   |
  |		</property>                                                                       |
  |	</bean>                                                                               |
  +---------------------------------------------------------------------------------------+

You must setup a dialog service.  There are two dialogServices available with this release the Application by number service and the application by
sip header service.  the Application by number service is used in the demo configuration.  It selects an application based on the number dialed.
The application by sip header service is used in the asterisk integration mode.  This can be used with an asterisk dialplan.  Where you can select
an application in the dialplan and then specify the application to be run in a custom sip header.  See integration with Asterisk dialplan section 
below for more details.
	
  +-----------------------------------------------------------------------------------+	
  |	<bean id="dialogService"                                                          |
  |	      class="org.speechforge.zanzibar.speechlet.ApplicationByNumberService"       |
  |		  init-method="startup" destroy-method="shutdown">                            |
  |	</bean>                                                                           |
  +-----------------------------------------------------------------------------------+
  
  OR
  
  +------------------------------------------------------------------------------------+
  |	<bean id="dialogService"                                                           |
  |	      class="org.speechforge.zanzibar.speechlet.ApplicationBySipHeaderService"     |
  |		  init-method="startup" destroy-method="shutdown">                             |
  |	</bean>                                                                            |
  +------------------------------------------------------------------------------------+ 
  
  
When using the application by number dialog service.  You must configure you Speech applications with the beanid set to the "number"  
with an underscore prefix.  For instance if you want to run the Parrot demo when someone dials 1000 extension configure the demo as follows.

  +--------------------------------------------------------------------------------------------+
  |   <bean id="_1000"                                                                         |
  |	   class="org.speechforge.apps.demos.Parrot"                                               |
  |	   singleton="false">                                                                      |
  |	   	<property name="prompt">                                                               |
  |		    <value>You can start speaking any time.  Would you like to hear the weather,       |
  |		            get sports news or hear a stock quote?  Say goodbye to exit.</value>       |
  |		</property>                                                                            |
  |		<property name="grammar">                                                              |
  |	        <value>file:../demo/grammar/example-loop.gram</value>                              |
  |		</property>                                                                            |
  |	</bean>	                                                                                   |
  +--------------------------------------------------------------------------------------------+  

  
Running in Demo Mode
--------------------
Start the server using the democontext.xml configuration (run the demoConnector.bat script)

If your run the zanzibar speech appliction server in demo mode with the default configuration file, you have the following demo choices

   +-----------+----------------------------------+
   | Dial 1000 | for parrot demo (java version)   |
   +-----------+----------------------------------+
   | Dial 2000 | for DTMF demo                    |
   +-----------+----------------------------------+
   | Dial 3000 | for jukebox demo                 |
   +-----------+----------------------------------+
   | Dial 4000 | Call Transfer Demo*              |
   +-----------+----------------------------------+
   | Dial 5000 | for hello world voice xml demo   |
   +-----------+----------------------------------+
   | Dial 6000 | for yes/no voice xml demo        |
   +-----------+----------------------------------+
   | Dial 7000 | for parrot voice xml demo        |
   +-----------+----------------------------------+
   *Call Transfer demo only works in asterisk mode (needs a pbx for call routing).   

Configuring the demos
------------------------
The demos are basically mrcp clients that are either programmed against the cairo client api or that are voicexml documents interpreted by the 
jvociexml interpreter.  The JvoiceXMl interpreter, in turn, uses the cairo-client api to do get and use speech resource in the MRCPv2 servers.

The demos can be configured for your environment as follows.  (Note these rules apply in both demo and asterisk mode.)

For Voice XML demos, copy the /vxml directory to a web server directory if you want to use http (you can also use a file:// url).  Then 
check your config file (democontext.xml).

Modify vxml file and grammar file urls in the config files according to where you copied the /vxml directory.  For example, the doc hello.vxml is expected to be found on 
in the vxml directory of web server running on the localhost (local to the zanzibar server) on port 8080.
 
  +---------------------------------------------------------------------------------+
  |  <bean id="HelloWorld"                                                          |
  |   class="org.speechforge.zanzibar.jvoicexml.impl.VoiceXmlSessionProcessor"      |
  |   singleton="false">                                                            |
  |   	 <property name="appUrl">                                                   |
  |			    <value>http://localhost:8080/voicexml/hello.vxml</value>            |
  |     </property>                                                                 |
  |  </bean>                                                                        |
  +---------------------------------------------------------------------------------+

For the java programs, grammar and prompt files are specified in the bean configuration.  The configuration should not need any changes, because those demos 
look for resources in a directory relative to the installation.  The exception is for the Jukebox demo -- becasue  we can not include any music in this 
distribution, so you will have to use your own audio files.

  +----------------------------------------------------------------------------------------------------------------------------------------------------+
  |    <bean id="Jukebox"                                                                                                                              |
  |	   class="org.speechforge.apps.demos.Jukebox"                                                                                                      |
  |     singleton="false">                                                                                                                             |
  |     <property name="firstPrompt">                                                                                                                  |
  |	        <value>Hi.  Welcome to the speechforge Jukebox.  What would you like to hear Bob Dylan, Radiohead, Amy Winehouse, Rolling Stones</value>   |
  |  	</property>                                                                                                                                    |
  |	    <property name="laterPrompts">                                                                                                                 |
  |      <value>Welcome back.  What would you like to hear next, Bob Dylan, Radiohead, Amy Winehouse, Rolling Stones</value>                           |
  |     </property>                                                                                                                                    |
  |     <property name="greetingGrammar">                                                                                                              |
  |        <value>file:../demo/grammar/jukeboxWelcome.gram</value>                                                                                     |
  |     </property>                                                                                                                                    |
  |     <property name="playGrammar">                                                                                                                  |
  |     <value>file:../demo/grammar/jukeboxPlay.gram</value>                                                                                           |
  |     </property>                                                                                                                                    |
  |     <property name="dylan">                                                                                                                        |
  |    <value>file:../../audio/jukebox/03RollinandTumblin.au</value>                                                                                   |
  |	   </property>                                                                                                                                     |
  |	   <property name="amy">                                                                                                                           |
  |        <value>file:../../audio/jukebox/11YouKnowImNoGoodRemix.au</value>                                                                           |
  |	   </property>                                                                                                                                     |
  | 	<property name="stones">                                                                                                                       |
  |        <value>file:../../audio/jukebox/01FancyManBlues.au</value>                                                                                  |
  |	   </property>                                                                                                                                     |
  | 	<property name="radiohead">                                                                                                                    |
  |        <value>file:../../audio/jukebox/08HouseofCards.au</value>                                                                                   |
  |	   </property>	                                                                                                                                   |
  |   </bean>                                                                                                                                          |
  +----------------------------------------------------------------------------------------------------------------------------------------------------+
   

Running in Asterisk Mode
------------------------

Start the server using the context.xml configuration (run the asteriskConnector.bat (or .sh) script)

1. Dialplan Integration

  To transfer a call to a java speech application in the zanzibar server add the following lines in your dialplan in the appropriate place. 
  Note the two sip extenion headers: x-channel amd x-application.  The channel passed to zanzibar in the x-channel header can be used later to redirect 
  calls back to asterisk and on to a new number.  The x-application header tells zanzibar which application to run on the callers behalf.    This example 
  runs a java appliction, org.speechforge.apps.demos.Parrot.  The class specified here must extend the speechlet abstract class (or implement the SessionProcessor
  interrace).

   +--------------------------------------------------------------------------------------+
   | exten => 1,n,SIPAddHeader(x-channel:${CHANNEL})                                      |
   | exten => 1,n,SIPAddHeader(x-application:beanId|Parrot)                               |
   | exten => 1,n,Dial(SIP/Zanzibar)                                                      |
   +--------------------------------------------------------------------------------------+
   
  The zanzibar platform also includes an voicexml intepreter.   If you wish to run a voicexml application, use a Sip x-application header should look like this:

   +-----------------------------------------------------------------------------------+
   | exten => 1,n,SIPAddHeader("x-application:vxml|http://localhost:8080/hello.vxml")  |
   +-----------------------------------------------------------------------------------+
   
  Note that in gerneal, the content of the x-application is in the form:

   +-----------------------------------------------------------+
   | type|applicationName                                      |
   |                                                           |
   | where:  type is either "beanid" "classname" or "vxml"     |
   +-----------------------------------------------------------+
   
   If  type=beanid, the application name is the beanid id as configured in spring config file, if type= classname then application name 
   is the fully qualified name of the java class that will run for the session else if  type = vxml then application name is the url of 
   the vxml document.

*Asterisk Manager Interface (AMI) Configuration

  The platform uses  AMI to transfer calls to other extensions or phone numbers.  For this to work you must add this to the manager.conf file 
  in your asterisk installation.  (Note that is is pretty insecure.  See asterisk documentation for information on how to secure an 
  asterisk installation.)

   +-------------------------------------------------------+
   | secret=password                                       |
   | permit=0.0.0.0/0.0.0.0                                |
   | read=system,call,log,verbose,agent,command,user       |
   | write=system,call,log,verbose,agent,command,user      |
   +-------------------------------------------------------+
   
   You must also configure Zanzibar so that it knows where asterisk manager is listening.  This is done in the context.xml file

   +-----------------------------------------------------------------------------------+
   |  <bean id="callControl" class="org.speechforge.zanzibar.asterisk.CallControl"     |
   |	init-method="startup" destroy-method="shutdown">                               |
   |	<property name="address">                                                      |
   |			    <value>192.168.0.101</value>                                       |
   |	</property>                                                                    |
   |	<property name="name">                                                         |
   |			    <value>manager</value>                                             |
   |	</property>                                                                    |
   |	<property name="password">                                                     |
   |			    <value>password</value>                                            |
   |	</property>                                                                    |
   |	<property name="disabled">                                                     |
   |	           <value>false</value>                                                |
   |	</property>                                                                    |
   | </bean>                                                                           |
   |                                                                                   |
   + ----------------------------------------------------------------------------------+


Writing your own speech applications
------------------------------------
One way to build your application is to use the voicexml support and specify the application in voiceXML.  

You can also write Speech applications in java.  Zanziabr speech applications must extend the speechlet abstract class.  Your application logic should be 
placed in the runApplication method.  This logic will run within its own thread in the zanzibar server.  

To use speech resources in your application get the speech client from the context:

   SpeechClient sClient = this.getContext().getSpeechClient();
   
You can use speechClient blocking or non-blocking calls.  If you choose to use the non-blocking calls, you can get recogntion and sythesis events.  
Implement a SpeechEventListener and add it as a listener to speechClient.

   SpeechEventListener myListener = new SpeechEventListenerImpl();
   sClient.addListener(myListerner);
   
The speech client provides you with a convenient playAndRecognize Method which will handle bargein logic for you.  It stops synthesis upon a speechStarted 
event or starts the noInput timer upon the completion of the synthesis.
   
To terminate the session (at the completion of the application) call dialogCompleted() on the context.  The context is available in the 
speechlet base class
   
   this.getContext().dialogCompleted();
   
To use the dtmf support be sure you are configured for sip based dtmf.  DTMF pattern matching is done by specifying a regex in the enable method.
The enableDTMF also needs a dtmf listener, the noInput timeout and nomatch timeout (both in ms).  The example below looks for a 4 digit dtmf sequence
with no timeouts.  Matches will invoke the characterEventReceived() method.
   
   sClient.enableDtmf("[0-9]{4}", this, 0, 0);

To transfer a call in your application, get the telephoney client from the context.

   SpeechClient tClient = this.getContext().getTelephonyClient();
   
You can transfer a call like this.

   channelName = this.getContext().getExternalSession().getChannelName();
   tClient.redirectBlocking(channelName, pbxContext, pbxExtenion);

Note that the context and extension must be configured on your pbx.  And the AMI is set up for receiving requests.


+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
+ Copyright (C) 2005-2008 SpeechForge. All Rights Reserved. +
+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 

