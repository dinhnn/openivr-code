/*
 * Zanzibar - Open source speech application server.
 *
 * Copyright (C) 2008-2009 Spencer Lord 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Contact: salord@users.sourceforge.net
 *
 */
package org.speechforge.zanzibar.sip;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

import java.net.UnknownHostException;
import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpParseException;
import javax.sip.ObjectInUseException;
import javax.sip.SipException;
import javax.sip.TimeoutEvent;

import org.apache.commons.pool.ObjectPool;
import org.apache.log4j.Logger;
import org.mrcp4j.MrcpResourceType;
import org.mrcp4j.client.MrcpChannel;
import org.mrcp4j.client.MrcpFactory;
import org.mrcp4j.client.MrcpProvider;
import org.mrcp4j.message.header.IllegalValueException;
import org.speechforge.cairo.rtp.AudioFormats;
import org.speechforge.cairo.rtp.server.PortPairPool;
import org.speechforge.cairo.rtp.server.RTPStreamReplicator;

import org.speechforge.cairo.rtp.server.RTPStreamReplicatorFactory;
import org.speechforge.cairo.sip.ResourceUnavailableException;
import org.speechforge.cairo.sip.SdpMessage;
import org.speechforge.cairo.sip.SessionListener;
import org.speechforge.cairo.sip.SipAgent;
import org.speechforge.cairo.sip.SipSession;
import org.speechforge.cairo.util.CairoUtil;

import org.speechforge.zanzibar.speechlet.SessionProcessor;
import org.speechforge.zanzibar.speechlet.SpeechletContextMrcpProvider;
import org.speechforge.zanzibar.speechlet.SpeechletContext;
import org.speechforge.zanzibar.speechlet.SpeechletContextMrcpv2Impl;
import org.speechforge.zanzibar.speechlet.SpeechletService;

import com.spokentech.speechdown.client.rtp.RtpTransmitter;

/**
 * SipServer is the sip agent for the speech client.  It implements SessionListner, so it
 * is notified with Significant SIP events.  It also provides methods to initiate SIP 
 * requets (i.e. invite and bye).
 * 
 * Before using this agent, the following properties need to be set either
 * by calling setters and then startup()(perhaps with Spring) 
 * OR use the 4 parameter constructor and the setDaultCairoServer method
 *    mySipAddress     - the sip address of the agent
 *    stackName        - a name for the agents sip stack
 *    port             - the port used by this sip agent
 *    transport        - transport too use (udp or tcp)
 *    cairoSipHostName - the host name of the speech servers sip agent (RM in cairo)
 *    cairoSipPort     - the port used by the speech servers sip agent
 *    cairoSipAddress  - the sip address of the speech server
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class SipServer implements SessionListener {
 

		private static Logger _logger = Logger.getLogger(SipServer.class);
        
        // properties need to be set either
        //   - call setters and then the no arg constructor then startup()(perhaps with SPring) OR
        //   - use the 4 parameter constructor and the setDaultCairoServer method
        private String mySipAddress;
        private String stackName;
        private int port;
        private String transport;
        private String cairoSipHostName = null;
        private int cairoSipPort;
        private String cairoSipAddress;

        
        private String mode = "mccpv2";

		private int baseReceiverRtpPort;
		private int maxConnects;
		
		private int baseXmitRtpPort;

		//TODO: one of these must go!
		private String zanzibarHostName = null;
		private String host;

		private ObjectPool _replicatorPool;
        private PortPairPool _portPairPool;
        private String hostname;
        
        private SipAgent _sipAgent;
        
        boolean responded = false;
        SdpMessage _response;
        SipSession _session;

        //private int _retryCount = 0;
        //private static final int  MAXRETRIES = 3;
        
        private SpeechletService dialogService;

        private SdpMessage _message;

        Map<String, SessionPair> waitingList;
        
        Set<String> calleeRegistry = new HashSet<String>();
         
        public SipServer() {
            super();
        }

        public SipServer(String mySipAddress, String stackName, int port, String transport) throws SipException {

            this.mySipAddress = mySipAddress;
            this.stackName = stackName;
            this.port = port;
            this.transport = transport;
            
            
            // Construct a SIP agent to be used to send a SIP Invitation to the ciaro server
            _sipAgent =new SipAgent(this, mySipAddress, stackName, port, transport);
            waitingList = new HashMap<String, SessionPair>();
        }
        
        
        public void startup() {
        	_logger.info("Starting sip Server!");

        	try {
        		if (zanzibarHostName == null)
        			zanzibarHostName = CairoUtil.getLocalHost().getHostAddress();

        		if (cairoSipHostName == null)
        			cairoSipHostName = CairoUtil.getLocalHost().getHostAddress();
        	} catch (SocketException e2) {
        		// TODO Auto-generated catch block
        		e2.printStackTrace();
        	} catch (UnknownHostException e2) {
        		// TODO Auto-generated catch block
        		e2.printStackTrace();
        	}

            try {
	            _sipAgent = new SipAgent(this, mySipAddress, stackName,zanzibarHostName,null, port, transport);
            } catch (SipException e1) {
	            // TODO Auto-generated catch block
	            e1.printStackTrace();
            }
            waitingList = new HashMap<String, SessionPair>();
        	_logger.info("Returned from call to Start sip Server!");
            
            
            if (mode.equals("cloud")) {
               try {
				_replicatorPool = RTPStreamReplicatorFactory.createObjectPool(baseReceiverRtpPort, maxConnects,  InetAddress.getByName(zanzibarHostName));
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
               _portPairPool = new PortPairPool(baseXmitRtpPort, maxConnects);
            }
            
        }
        

        public boolean registerSipAddress(String sAddress) {
        	return calleeRegistry.add(sAddress);
        }

        public boolean unRegisterSipAddress(String sAddress) {
        	return calleeRegistry.remove(sAddress);
        }
        
        
        public String getZanzibarHostName() {
			return zanzibarHostName;
		}

		public void setZanzibarHostName(String zanzibarHostName) {
			this.zanzibarHostName = zanzibarHostName;
		}

        /**
         * @return the mode
         */
        public String getMode() {
        	return mode;
        }

		/**
         * @param mode the mode to set
         */
        public void setMode(String mode) {
        	this.mode = mode;
        }
        
        
    	
		/**
         * @return the baseXmitRtpPort
         */
        public int getBaseXmitRtpPort() {
        	return baseXmitRtpPort;
        }

		/**
         * @param baseXmitRtpPort the baseXmitRtpPort to set
         */
        public void setBaseXmitRtpPort(int baseXmitRtpPort) {
        	this.baseXmitRtpPort = baseXmitRtpPort;
        }
        
        /**
         * @return the baseRtpPort
         */
        public int getBaseReceiverRtpPort() {
        	return baseReceiverRtpPort;
        }

		/**
         * @param baseRtpPort the baseRtpPort to set
         */
        public void setBaseReceiverRtpPort(int baseReceiverRtpPort) {
        	this.baseReceiverRtpPort = baseReceiverRtpPort;
        }

        
        /**
         * @return the maxConnects
         */
        public int getMaxConnects() {
        	return maxConnects;
        }

		/**
         * @param maxConnects the maxConnects to set
         */
        public void setMaxConnects(int maxConnects) {
        	this.maxConnects = maxConnects;
        }

        
        public void setDefaultCairoServer(String cairoSipAddress, String cairoSipHostName, int cairoSipPort) {
            this.cairoSipAddress = cairoSipAddress;
            this.cairoSipHostName = cairoSipHostName;
            this.cairoSipPort = cairoSipPort;
            
        }
        
        public static InetAddress getLocalHost() throws SocketException, UnknownHostException {
               Enumeration<NetworkInterface> networkInterfaces =NetworkInterface.getNetworkInterfaces();
               while (networkInterfaces.hasMoreElements()) {
                       NetworkInterface networkInterface = networkInterfaces.nextElement();
                       if (!networkInterface.isLoopback()) {
                               Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                               if (inetAddresses.hasMoreElements())
                                       return inetAddresses.nextElement();
                       }
               }
               return InetAddress.getLocalHost();
        }
        

        public synchronized SdpMessage sendInviteWithoutProxy(String to, SdpMessage message, String peerAddress, int peerPort) throws SipException {
            //_retryCount = 0;
 
            //save the parameters in case a timeout occurs in which case a retry is needed.
            //_to = to;
            //_message = message;
            //_peerAddress = peerAddress;
            //_peerPort = peerPort;
            
            // Send the sip invitation
            SipSession session = _sipAgent.sendInviteWithoutProxy(to, message, peerAddress, peerPort);

            responded = false;
            //synchronized (this) {
                while (!responded) {
                    responded = false;
                   try {
                       this.wait(1000);
                   } catch (InterruptedException e) {
                       // TODO Auto-generated catch block
                       e.printStackTrace();
                   }
                }
            //}
            return _response;
        }
        
        public void shutdown() throws ObjectInUseException {
            _sipAgent.dispose();
        }
        

        public synchronized SdpMessage processInviteResponse(boolean ok, SdpMessage response,SipSession session) {
            _logger.debug("Gotta invite response, ok is: "+ok);
            SdpMessage pbxResponse = null;
            if (ok) {
                //_logger.debug(":::: "+session.getId()+" :::::::");
                SessionPair pair = waitingList.get(session.getCtx().toString());
                waitingList.remove(session.getCtx().toString());

                if (pair != null) {
                    // Get the MRCP media channels (need the port number and the channelID that are sent
                    // back from the server in the response in order to setup the MRCP channel)
                    String remoteHostName = null;
                    InetAddress remoteHostAdress = null;
                    try {
                        remoteHostName = response.getSessionDescription().getConnection().getAddress();
                        remoteHostAdress = InetAddress.getByName(remoteHostName);

                        List <MediaDescription> xmitterChans = response.getMrcpTransmitterChannels();
                        int xmitterPort = xmitterChans.get(0).getMedia().getMediaPort();
                        String xmitterChannelId = xmitterChans.get(0).getAttribute(SdpMessage.SDP_CHANNEL_ATTR_NAME);

                        List <MediaDescription> receiverChans = response.getMrcpReceiverChannels();
                        MediaDescription controlChan = receiverChans.get(0);
                        int receiverPort = controlChan.getMedia().getMediaPort();
                        String receiverChannelId = receiverChans.get(0).getAttribute(SdpMessage.SDP_CHANNEL_ATTR_NAME);

                        List <MediaDescription> rtpChans = response.getAudioChansForThisControlChan(controlChan);
                        int remoteRtpPort = -1;
                        Vector supportedFormats = null;
                        if (rtpChans.size() > 0) {
                            //TODO: What if there is more than 1 media channels?
                            //TODO: check if there is an override for the host attribute in the m block
                            //InetAddress remoteHost = InetAddress.getByName(rtpmd.get(1).getAttribute();
                            remoteRtpPort =  rtpChans.get(0).getMedia().getMediaPort();
                            //rtpmd.get(1).getMedia().setMediaPort(localPort);
                            supportedFormats = rtpChans.get(0).getMedia().getMediaFormats(true);    
                        } else {
                            _logger.warn("No Media channel specified in the invite request");
                            //TODO:  handle no media channel in the response corresponding tp the mrcp channel (sip/sdp error)
                        } 

                        

                        //Construct the MRCP Channels
                        String protocol = MrcpProvider.PROTOCOL_TCP_MRCPv2;
                        MrcpFactory factory = MrcpFactory.newInstance();
                        MrcpProvider provider = factory.createProvider();
                        
                        _logger.debug("New Xmitter chan: "+xmitterChannelId+" "+remoteHostAdress+" "+xmitterPort+" "+ protocol);
                        _logger.debug("New Receiver chan: "+receiverChannelId+" "+remoteHostAdress+" "+receiverPort+" "+protocol);
                        
                        MrcpChannel ttsChannel = provider.createChannel(xmitterChannelId, remoteHostAdress, xmitterPort, protocol);
                        MrcpChannel recogChannel = provider.createChannel(receiverChannelId, remoteHostAdress, receiverPort, protocol);
                        session.setTtsChannel(ttsChannel);
                        session.setRecogChannel(recogChannel);
                        
                        pbxResponse = startupSpeechlet(remoteHostName, remoteRtpPort, supportedFormats,pair.getMrcpSession(), pair.getPbxSession());
            
                        //_logger.debug(">>>>Here is the invite response:");
                        //_logger.debug(pbxResponse.getSessionDescription().toString());

                    } catch (UnknownHostException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (SdpParseException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IllegalArgumentException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IllegalValueException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (SdpException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    // TODO:  Need to keep track of the "forwarded" Session.  the one that wa created to get the resourses for MRCPv2.  The original
                    // Session came from the pbx
                    //session.setForward(forward);

                } else {
                    _logger.info("Could not find corresponding external request in waiting list");
                }
            } else {
                _logger.info("Invite Response not ok");
            }

            return pbxResponse;
        }

        private SdpMessage startupSpeechlet(String remoteHostName, int remoteRtpPort, Vector supportedFormats, SipSession mrcpSession, SipSession pbxSession) throws UnknownHostException, SdpException, Exception {
			SdpMessage pbxResponse;
			pbxResponse = constructInviteResponseToPbx(remoteRtpPort, remoteHostName, supportedFormats);
	
			_sipAgent.sendResponse(pbxSession, pbxResponse);
	
			dialogService.startNewMrcpDialog(pbxSession, mrcpSession);
			
			return pbxResponse;
		}
                        

        public synchronized void processTimeout(TimeoutEvent event) {
            _logger.debug("Timeout occurred");
           // _retryCount = _retryCount + 1;
           // _logger.info("Timeout # "+ _retryCount+" occured for SIP invite request.");
           // if (_retryCount < MAXRETRIES) {
           //     // Re-Send the sip invitation
           //     try {
           //         SipSession session = _sipAgent.sendInviteWithoutProxy(_to, _message, _peerAddress, _peerPort);
           //     } catch (SipException e) {
           //         // TODO Auto-generated catch block
           //         e.printStackTrace();
           //     }
           // } else {   
               _response = null;
               responded = true;
               this.notify();
           // }
        }
   


        public void processByeRequest(SipSession session) throws RemoteException, InterruptedException {
            //TODO:  This is certainly not corrrect.  There are no resources in the proxy
            //       this is a cut and paste error.  Should  send bye on to cairo so that it can clean up 
            //       - Also need to check take a careful look at what happens to the application here in the proxy when a bye is received
            //         and the special case of when the call was forwarded on -- should it be able to return to the voice app?
            //for (Resource r : session.getResources()) {
             //   r.bye(session.getId());
            //}
            _logger.info("Got a bye request");
            
            try {
                dialogService.StopDialog(session);
            } catch (SipException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        public SdpMessage processInviteRequest(SdpMessage request, SipSession session) throws SdpException, ResourceUnavailableException, RemoteException {
            
        	//TODO: Check the callee registry here.  But then do ai need to check for all requests
        	//TODO: Investigate Pushing the check down to sipAgent/Listener?
        	
            //get the rtp channels from the invitation
            Vector pbxFormats = null;
            int pbxRtpPort = 0;
            String pbxHost = request.getSessionDescription().getConnection().getAddress();
            String pbxSessionName = request.getSessionDescription().getSessionName().getValue();
            SdpMessage pbxResponse = null;
            SipSession internalSession = null;
            
            _logger.info("Got an invite request");
            try {
                for (MediaDescription md : request.getRtpChannels()) {
                    pbxRtpPort = md.getMedia().getMediaPort();
                    pbxFormats = md.getMedia().getMediaFormats(true);
                    //_logger.debug("Individual Media connection address: "+ md.getConnection().getAddress());
                }
            } catch (SdpException e) {
                _logger.debug(e, e);
                throw e;
            }
            if (_logger.isDebugEnabled()) {
	            for (int i=0; i<pbxFormats.size(); i++) {
	                _logger.debug("pbx format # "+i+" is: "+pbxFormats.get(i).toString());
	            }
            }

            if (mode.equals("cloud")) {

            	RTPStreamReplicator rtpReplicator = null;
                try {
                	rtpReplicator = (RTPStreamReplicator) _replicatorPool.borrowObject();
                    AudioFormats af = AudioFormats.constructWithSdpVector(pbxFormats);
                    _logger.info("Audio Format "+af);
	                Vector supportedFormats = af.filterOutUnSupportedFormatsInOffer();
	                if (_logger.isDebugEnabled()) {
		                for (int i=0; i<supportedFormats.size(); i++) {
		                    //_logger.debug(i+" format type is: "+supportedFormats.get(i).getClass().getCanonicalName());
		                	 _logger.debug("Supported format # "+i+" is: "+supportedFormats.get(i).toString());
		                }
	                }
                    
	        		pbxResponse = constructInviteResponseToPbx(rtpReplicator.getPort(), zanzibarHostName, supportedFormats);
	    			_sipAgent.sendResponse(session, pbxResponse);
   
	        		
	        		InetAddress address = InetAddress.getByName(pbxHost);
	        		int  localPort = _portPairPool.borrowPort();
	              	RtpTransmitter rtpTransmitter = new RtpTransmitter(localPort, address, pbxRtpPort,af );

	                dialogService.startNewCloudDialog(session,rtpReplicator,rtpTransmitter);
                } catch (Exception e) {
	                // TODO Auto-generated catch block
	                e.printStackTrace();
                }
            	

            } else if (mode.equals("mrcpv2")) {
            	
            	
             
	            //construct the invite request and send it to the cairo resource server to get the resources for the session
	            //TODO: check which resource needed in the original invite (from pbx).  the construct method below blindly gets a tts and recog resoruce
	            SdpMessage message = null;
	            SdpMessage inviteResponse = null;
	            try {
	                message = constructInviteRequestToCairo(pbxRtpPort,pbxHost,pbxSessionName,pbxFormats);
	                internalSession = _sipAgent.sendInviteWithoutProxy(cairoSipAddress, message, cairoSipHostName, cairoSipPort);
	            } catch (UnknownHostException e1) {
	                // TODO Auto-generated catch block
	                e1.printStackTrace();
	            } catch (SipException e1) {
	                // TODO Auto-generated catch block
	                e1.printStackTrace();
	            }
	            //these sessions are linked
	            //the forward attribute is not good enough (one side is the backwrds lookup)
	            //TODO: Session rediesign.  It has become a bucket of things that are sometmes needed in the client and sometimes in teh server
	            // and the proxy relationship is messy with the single reference "forward"
	            session.setForward(internalSession);
	            internalSession.setForward(session);
	            SessionPair sessionPair = new SessionPair(internalSession,session);
	            //_logger.debug(":::: ADDING  Internal "+internalSession.getId()+" :::::::");
	            //_logger.debug(":::: AND THE External "+session.getId()+" :::::::");
	            waitingList.put(internalSession.getCtx().toString(),sessionPair);
            } else {
            	_logger.warn("Unrecognized SipServer mode, "+mode);
            }
            return null;
        }
            
   
        
        private  SdpMessage constructInviteRequestToCairo(int pbxRtpPort,String pbxHost,String pbxSessionName,Vector pbxFormats) throws UnknownHostException, SdpException {
            SdpMessage sdpMessage = SdpMessage.createNewSdpSessionMessage(mySipAddress, zanzibarHostName, pbxSessionName);
            MediaDescription rtpChannel = SdpMessage.createRtpChannelRequest(pbxRtpPort,pbxFormats,pbxHost);
            //rtpChannel.getMedia().setMediaFormats(pbxFormats);
            MediaDescription synthControlChannel = SdpMessage.createMrcpChannelRequest(MrcpResourceType.SPEECHSYNTH);
            MediaDescription recogControlChannel = SdpMessage.createMrcpChannelRequest(MrcpResourceType.SPEECHRECOG);
            Vector v = new Vector();
            v.add(synthControlChannel);
            v.add(recogControlChannel);
            v.add(rtpChannel);
            sdpMessage.getSessionDescription().setMediaDescriptions(v);
            return sdpMessage;
        }
        
        private  SdpMessage constructInviteResponseToPbx(int cairoRtpPort, String cairoHost, Vector formats) throws UnknownHostException, SdpException {
            SdpMessage sdpMessage = SdpMessage.createNewSdpSessionMessage(mySipAddress, cairoHost, "The session Name");
            MediaDescription rtpChannel = SdpMessage.createRtpChannelRequest(cairoRtpPort,formats);
            Vector v = new Vector();
            //rtpChannel.setAttribute("ptime", "60");
            v.add(rtpChannel);
            sdpMessage.getSessionDescription().setMediaDescriptions(v);
            return sdpMessage;
        }

        /**
         * @return the cairoSipAddress
         */
        public String getCairoSipAddress() {
            return cairoSipAddress;
        }

        /**
         * @param cairoSipAddress the cairoSipAddress to set
         */
        public void setCairoSipAddress(String cairoSipAddress) {
            this.cairoSipAddress = cairoSipAddress;
        }

        /**
         * @return the cairoSipHostName
         */
        public String getCairoSipHostName() {
            return cairoSipHostName;
        }

        /**
         * @param cairoSipHostName the cairoSipHostName to set
         */
        public void setCairoSipHostName(String cairoSipHostName) {
            this.cairoSipHostName = cairoSipHostName;
        }

        /**
         * @return the cairoSipPort
         */
        public int getCairoSipPort() {
            return cairoSipPort;
        }

        /**
         * @param cairoSipPort the cairoSipPort to set
         */
        public void setCairoSipPort(int cairoSipPort) {
            this.cairoSipPort = cairoSipPort;
        }

        /**
         * @return the mySipAddress
         */
        public String getMySipAddress() {
            return mySipAddress;
        }

        /**
         * @param mySipAddress the mySipAddress to set
         */
        public void setMySipAddress(String mySipAddress) {
            this.mySipAddress = mySipAddress;
        }

        /**
         * @return the port
         */
        public int getPort() {
            return port;
        }

        /**
         * @param port the port to set
         */
        public void setPort(int port) {
            this.port = port;
        }

        /**
         * @return the host
         */
        public String getHost() {
        	return host;
        }

    	/**
         * @param host the host to set
         */
        public void setHost(String host) {
        	this.host = host;
        }
        
        /**
         * @return the stackName
         */
        public String getStackName() {
            return stackName;
        }

        /**
         * @param stackName the stackName to set
         */
        public void setStackName(String stackName) {
            this.stackName = stackName;
        }

        /**
         * @return the transport
         */
        public String getTransport() {
            return transport;
        }

        /**
         * @param transport the transport to set
         */
        public void setTransport(String transport) {
            this.transport = transport;
        }

        /**
         * @return the dialogService
         */
        public SpeechletService getDialogService() {
            return dialogService;
        }

        /**
         * @param dialogService the dialogService to set
         */
        public void setDialogService(SpeechletService dialogService) {
            this.dialogService = dialogService;
        }
        
        private class SessionPair {
            private SipSession mrcpSession;
            private SipSession pbxSession;
            
            public SessionPair(SipSession i, SipSession e) {
                mrcpSession = i;
                pbxSession = e;
            }
            
            /**
             * @return the pbx session
             */
            public SipSession getPbxSession() {
                return pbxSession;
            }
            /**
             * @param external the pbxSession to set
             */
            public void setPbxSession(SipSession external) {
                this.pbxSession = external;
            }
            /**
             * @return the mrcp sesion
             */
            public SipSession getMrcpSession() {
                return mrcpSession;
            }
            /**
             * @param internal the mrcp session to set
             */
            public void setMrcpSession(SipSession internal) {
                this.mrcpSession = internal;
            }
            
        }

        public void processInfoRequest(SipSession session, String contentType, String contentSubType, String content) {
            
            _logger.debug("SIP INFO request: "+contentType+"/"+contentSubType+"\n"+content);
           
            String code = null;
            int duration = 0 ;
            
            //if dtmf signal     
            if ((contentType.trim().equals("application")) &&(contentSubType.trim().equals("dtmf-relay"))) {

                //Handle the client side dtmf signaling
                if (content == null) {
                    _logger.warn("sip info request with a dtmf-relay content type with no content.");
                } else {

                    String lines[] = content.toString().split("\n");
                    for (int i=0; i<lines.length;i++) {
                        String parse[] = lines[i].toString().split("=");
                        if (parse[0].equals("Signal")) {
                            code = parse[1];
                        }
                        if (parse[0].equals("Duration")) {
                            duration = Integer.parseInt(parse[1].trim());
                        }
                    }
                    _logger.debug("The DTMF code : "+code);
                    _logger.debug("The duration: "+ duration);

                    dialogService.dtmf(session,code.charAt(0));


                    //TODO: Forward the info request on to the speech server
                    //_sipAgent.sendInfoRequest(session, contentType,contentSubType,content);
                }

            } else {
                _logger.warn("Unhandled SIP INFO request content type: "+contentType+"/"+contentSubType+"\n"+content);
            }

        }
}