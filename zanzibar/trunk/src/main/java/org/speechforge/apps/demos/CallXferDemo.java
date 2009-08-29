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
package org.speechforge.apps.demos;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.asteriskjava.manager.TimeoutException;
import org.mrcp4j.client.MrcpInvocationException;
import org.mrcp4j.message.MrcpEvent;
import org.mrcp4j.message.header.IllegalValueException;
import org.speechforge.cairo.client.recog.RecognitionResult;
import org.speechforge.cairo.client.recog.RuleMatch;
import org.speechforge.zanzibar.speechlet.InvalidContextException;
import org.speechforge.zanzibar.speechlet.Speechlet;
import org.speechforge.zanzibar.telephony.TelephonyClient;
import org.speechforge.cairo.client.NoMediaControlChannelException;
import org.speechforge.cairo.client.SpeechClient;
import org.speechforge.cairo.client.SpeechEventListener;

/**
 * DTMF Demo Speech Application.  Looks for 4 digit dtmf sequences.  Shows the use of regex for pattern matching. (until you say quit or stop).
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class CallXferDemo extends Speechlet implements SpeechEventListener {
    private static Logger _logger = Logger.getLogger(CallXferDemo.class);
      
    private String prompt;
    private String grammar;
    private String pbxContext;


	SpeechClient sClient;
	TelephonyClient tClient;
	String channelName;
	
    public CallXferDemo() {
        super();
    }

    protected  void  runApplication() throws NoMediaControlChannelException {
        try {
            sClient = this.getContext().getSpeechClient();
            tClient = this.getContext().getTelephonyClient();
            channelName = this.getContext().getPBXSession().getChannelName();
            sClient.turnOnBargeIn();
            String result = "";  // results string to prepend to the prompt
            sClient.enableDtmf("[0-9]{4}", this, 0, 0);
            while (!stopFlag) {  

                _logger.debug("Calling play and Recognize...");
                
                RecognitionResult r = sClient.playAndRecognizeBlocking(false, result + prompt,grammar, false);
                if ((r != null) &&  (!r.isOutOfGrammar())) {
                    _logger.debug("Got a result: "+r.getText());
                    
                    // get all the tags from the list (in this case main and phoneType)
                    String main = null;
                    String phoneType = null;
                    for (RuleMatch rule : r.getRuleMatches()) {
                        _logger.info(rule.getTag()+ ":"+rule.getRule());
                        if (rule.getRule().equals("main")) {
                            main = rule.getTag();
                        } else if (rule.getRule().equals("phoneType")) {
                            phoneType = rule.getTag();
                        } else {
                            //TODO: Grammar error
                        }
                    }
                    
                    //processing is different for different main tags (either quit, help, repeat or else the employee id of the person to call)
                    if (main != null) {
                    	if (main.equals("QUIT"))  {
                            stopFlag = true;
                            try {
                                this.getContext().dialogCompleted();
                            } catch (InvalidContextException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                    	} else {
                            
                    		//if not quit, help or repeat, the main tah comtains the employee id that was spoken
                            String employee = main;
                            
                            // if the phonetpe is not null, the phone type was specified in the utterance
                            // this demo requires extensions defined in the pbx of the form name-phonetype
                            // the phonetype is optional (examples:  larry-mobile, larry-work, larry)
                            if (phoneType != null) {
                                _logger.info("phonetype is sepecified and it is: "+phoneType);
                                employee = employee+"-"+phoneType;
                            } 
                            
                            // Use the telephony client to redirect the call.
                            tClient.redirectBlocking(channelName, pbxContext, employee);
                            stopFlag = true;

                    	}
                    	
                	} else {
                    	_logger.warn("Invalid results, could not intepret the utterance.");
                	}


                } else {
                    _logger.debug("No recognition result...");
                    result=  "I did not understand.  ";
                }

            }
        } catch (MrcpInvocationException e) {
            _logger.info("MRCP Response status code is: "+e.getResponse().getStatusCode());
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalValueException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TimeoutException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
    }

    public void recognitionEventReceived(MrcpEvent event, RecognitionResult r) {
        _logger.info("Recog Event Received: "+event.toString()+ "\nResult: "+r.getText());  
    }

    public void speechSynthEventReceived(MrcpEvent event) {
        _logger.info("Speech Synth Event Received: "+event.toString());         
    }

    public void characterEventReceived(String c, EventType status) {
    	_logger.info("Character Event.  Status is "+ status+".  Code is "+c);

    	// Use the telephony client to redirect the call to the extension entered with DTMF
    	// Here in this part of the demo, the pbx needs a extension configured in the given context (i.e 1000, 2000,...)
    	if (c != null) {
    		try {
    			tClient.redirectBlocking(channelName, pbxContext, c);
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		} catch (TimeoutException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    		stopFlag = true;
    	} else {
    		_logger.warn("Could not determine a phone number for the employee");
    	}
    }

    /**
     * @return the grammar
     */
    public String getGrammar() {
        return grammar;
    }

    /**
     * @param grammar the grammar to set
     */
    public void setGrammar(String grammar) {
        this.grammar = grammar;
    }

    /**
     * @return the prompt
     */
    public String getPrompt() {
        return prompt;
    }

    /**
     * @param prompt the prompt to set
     */
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }


	/**
     * @return the pbxContext
     */
    public String getPbxContext() {
    	return pbxContext;
    }

	/**
     * @param pbxContext the pbxContext to set
     */
    public void setPbxContext(String pbxContext) {
    	this.pbxContext = pbxContext;
    }

}
