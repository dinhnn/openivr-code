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

import javax.media.rtp.InvalidSessionAddressException;

import org.mrcp4j.client.MrcpInvocationException;
import org.mrcp4j.message.MrcpEvent;
import org.mrcp4j.message.header.IllegalValueException;

import org.speechforge.cairo.client.recog.RuleMatch;
import org.speechforge.zanzibar.speechlet.InvalidContextException;
import org.speechforge.zanzibar.speechlet.Speechlet;
import org.speechforge.cairo.client.NoMediaControlChannelException;
import org.speechforge.cairo.client.SpeechClient;
import org.speechforge.cairo.client.SpeechEventListener;
import org.speechforge.cairo.client.recog.RecognitionResult;

/**
 * Jukebox Speech application.  Plays songs (audio files) bases upon the spoken request.  Demonstartes the use of preprecorded prompts.
 * 
 * @author Spencer Lord {@literal <}<a href="mailto:salord@users.sourceforge.net">salord@users.sourceforge.net</a>{@literal >}
 */
public class Jukebox extends Speechlet implements SpeechEventListener {
    
    private String firstPrompt = "Hi.  Welcome to the speechforge Jukebox.  What would you like to hear Bob Dylan, Radiohead, Amy Winehouse, Rolling Stones";
    private String laterPrompts = "Welcome back.  What would you like to hear next, Bob Dylan, Radiohead, Amy Winehouse, Rolling Stones";
    private String greetingGrammar = "file:../demo/grammar/jukeboxWelcome.gram";
    private String playGrammar = "file:../demo/grammar/jukeboxPlay.gram"; 
    private String dylan = "file:../../audio/jukebox/03RollinandTumblin.au";
    private String amy = "file:../../audio/jukebox/11YouKnowImNoGoodRemix.au";
    private String stones = "file:../../audio/jukebox/01FancyManBlues.au";
    private String radiohead = "file:../../audio/jukebox/08HouseofCards.au";
    
    
    public Jukebox() {
        super();
    }
    
    protected  void  runApplication() throws NoMediaControlChannelException {
        try {

            SpeechClient sClient = this.getContext().getSpeechClient();
            sClient.turnOnBargeIn();
            
            
            String prompt = firstPrompt;
            while (!stopFlag) {  
                RecognitionResult r = sClient.playAndRecognizeBlocking(false, prompt, greetingGrammar, false);
                prompt = laterPrompts;
                //client.play(prompt);
                _logger.debug("Calling play and Recognize...");

                if ((r != null) &&  (!r.isOutOfGrammar())) {
                    _logger.debug("Got a result and calling playBlocking "+r.getText());
                    sClient.playBlocking(false,r.getText());

                    for (RuleMatch rule : r.getRuleMatches()) {
                        System.out.println(rule.getTag()+ ":"+rule.getRule());
                        if (rule.getRule().equals("main")) {
                            if (rule.getTag().equals("DYLAN"))  {
                                sClient.playAndRecognizeBlocking(true, dylan, playGrammar, true);
                            } else if (rule.getTag().equals("AMY"))  {
                                    sClient.playAndRecognizeBlocking(true, amy, playGrammar, true);
                            } else if (rule.getTag().equals("STONES"))  {
                                sClient.playAndRecognizeBlocking(true, stones, playGrammar, true);
                            } else if (rule.getTag().equals("RADIOHEAD"))  {
                                sClient.playAndRecognizeBlocking(true, radiohead, playGrammar, true);                         
                            } else if (rule.getTag().equals("QUIT")) {
                                //CallControl.amiHangup(session.getForward().getChannelName());
                                stopFlag = true;
                                try {
                                    this.getContext().dialogCompleted();
                                } catch (InvalidContextException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                } else {
                    _logger.debug("No recognition result...");
                    sClient.playBlocking(false,"I did not understand");
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
        } catch (InvalidSessionAddressException e) {
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
        _logger.info("Character Event! status= "+ status+" code= "+c);
        
    }

    
    //getters and setters for Configuring the demos prompts and grammars.
    
    /**
     * @return the amy
     */
    public String getAmy() {
        return amy;
    }

    /**
     * @param amy the amy to set
     */
    public void setAmy(String amy) {
        this.amy = amy;
    }

    /**
     * @return the dylan
     */
    public String getDylan() {
        return dylan;
    }

    /**
     * @param dylan the dylan to set
     */
    public void setDylan(String dylan) {
        this.dylan = dylan;
    }

    /**
     * @return the firstPrompt
     */
    public String getFirstPrompt() {
        return firstPrompt;
    }

    /**
     * @param firstPrompt the firstPrompt to set
     */
    public void setFirstPrompt(String firstPrompt) {
        this.firstPrompt = firstPrompt;
    }

    /**
     * @return the greetingGrammar
     */
    public String getGreetingGrammar() {
        return greetingGrammar;
    }

    /**
     * @param greetingGrammar the greetingGrammar to set
     */
    public void setGreetingGrammar(String greetingGrammar) {
        this.greetingGrammar = greetingGrammar;
    }

    /**
     * @return the laterPrompts
     */
    public String getLaterPrompts() {
        return laterPrompts;
    }

    /**
     * @param laterPrompts the laterPrompts to set
     */
    public void setLaterPrompts(String laterPrompts) {
        this.laterPrompts = laterPrompts;
    }

    /**
     * @return the playGrammar
     */
    public String getPlayGrammar() {
        return playGrammar;
    }

    /**
     * @param playGrammar the playGrammar to set
     */
    public void setPlayGrammar(String playGrammar) {
        this.playGrammar = playGrammar;
    }

    /**
     * @return the radiohead
     */
    public String getRadiohead() {
        return radiohead;
    }

    /**
     * @param radiohead the radiohead to set
     */
    public void setRadiohead(String radiohead) {
        this.radiohead = radiohead;
    }

    /**
     * @return the stones
     */
    public String getStones() {
        return stones;
    }

    /**
     * @param stones the stones to set
     */
    public void setStones(String stones) {
        this.stones = stones;
    }

}
