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
package org.speechforge.zanzibar.speechlet;

import javax.sip.SipException;

import org.speechforge.cairo.sip.SipSession;
import org.speechforge.cairo.client.SpeechClient;
import org.speechforge.cairo.client.SpeechClientImpl;
import org.speechforge.zanzibar.telephony.TelephonyClient;
import org.speechforge.zanzibar.telephony.TelephonyClientImpl;

public class SpeechletContextImpl implements SpeechletContext {
    
    private SpeechletService container;
    private SessionProcessor speechlet;
    SipSession internalSession;
    SipSession externalSession;
    
    SpeechClient speechClient;
    TelephonyClient telephonyClient;

    
    public void init() throws InvalidContextException {
        if (internalSession == null )
            throw new InvalidContextException();
        
        this.speechClient = new SpeechClientImpl(internalSession.getTtsChannel(),internalSession.getRecogChannel());
        this.telephonyClient = new TelephonyClientImpl(externalSession.getChannelName());
    }
    
    public void cleanup() {
        internalSession = null;
        externalSession = null;
        speechClient = null;
        telephonyClient = null;
    }
    
    public void dialogCompleted() throws InvalidContextException {
        
        if (container == null)
            //TODO add an exception for uninitialized context
            throw new InvalidContextException();
                
        try {    
            // send the bye request (to the platform -- not speech server)
            // only need to do this if dialog completed gracefully
            // like here whne the speech applet notifies the container that it completed
            //other scenario is that a bye received from teh remote side (phone was hungup)
            externalSession.getAgent().sendBye(externalSession);
            //platformSession.getAgent().dispose();
            
            //clean up the dialog (the speech server session is cleaned up here)
            container.StopDialog(externalSession);
        } catch (SipException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }


    /**
     * @return the container
     */
    public SpeechletService getContainer() {
        return container;
    }


    /**
     * @param container the container to set
     */
    public void setContainer(SpeechletService container) {
        this.container = container;
    }


    /**
     * @return the speechlet
     */
    public SessionProcessor getSpeechlet() {
        return speechlet;
    }


    /**
     * @param speechlet the speechlet to set
     */
    public void setSpeechlet(SessionProcessor speechlet) {
        this.speechlet = speechlet;
    }


    /**
     * @return the externalSession
     */
    public SipSession getExternalSession() {
        return externalSession;
    }


    /**
     * @param externalSession the externalSession to set
     */
    public void setExternalSession(SipSession externalSession) {
        this.externalSession = externalSession;
    }


    /**
     * @return the internalSession
     */
    public SipSession getInternalSession() {
        return internalSession;
    }


    /**
     * @param internalSession the internalSession to set
     */
    public void setInternalSession(SipSession internalSession) {
        this.internalSession = internalSession;
    }

    /**
     * @return the speechClient
     */
    public SpeechClient getSpeechClient() {
        return speechClient;
    }

    /**
     * @return the telephonyClient
     */
    public TelephonyClient getTelephonyClient() {
        return telephonyClient;
    }



}