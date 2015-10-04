/*******************************************************************************
 * Copyright (c) 2013, 2015 Francis G.
 *
 * This file is part of ChatServer.
 *
 * ChatServer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * ChatServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ChatServer.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.sundays.chat.api.servlet;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sundays.chat.api.UserSession;
import com.sundays.chat.server.message.MessageWrapper;

/**
 * 
 * @author Francis
 */
public class ServletUserSession implements UserSession {
	
	private String sessionID;
    private ConcurrentHashMap<Integer, List<MessageWrapper>> queuedMessages = new ConcurrentHashMap<>();

	public ServletUserSession() {
		// TODO Auto-generated constructor stub
	}
    
    public String getSessionID () {
    	return this.sessionID;
    }
    
    public void setSessionID (String sessionID) {
    	this.sessionID = sessionID;
    }

	@Override
	public void sendMessage(int channelID, MessageWrapper message) {		
    	List<MessageWrapper> queuedMessages = this.queuedMessages.get(channelID);
    	if (queuedMessages == null) {
    		//If the channel queue does not exist for this user, create it.
    		queuedMessages = new CopyOnWriteArrayList<MessageWrapper>();
    		this.queuedMessages.put(channelID, queuedMessages);
    	}
    	queuedMessages.add(message);
	}
    
    /**
     * Retrieves the queued messages for the user in the specified channel.
     * @param channelID The ID of the channel which the messages were sent from.
     * @param remove Whether the messages should be removed from the queue.
     * @return A list of wrapped messsages, or null if there are no queued messages from the channel
     */
    public List<MessageWrapper> getQueuedMessages (int channelID, boolean remove) {
    	// 'remove' is used to specify if the messages should be removed from the cue.
    	List<MessageWrapper> queuedMessages = this.queuedMessages.get(channelID);
    	if (queuedMessages == null) {
    		return null;
    	}
    	if (remove){
    		this.clearMessageQueue(channelID);
    	}
		return queuedMessages;
    }
    
    public List<MessageWrapper> getQueuedMessages (int channelID, boolean remove, int[] types) {
    	//Retrieves the cued messages for the specified channel. 'remove' is used to specify if the messages should be removed from the cue.
    	List<MessageWrapper> cuedMessages = this.queuedMessages.get(channelID);
    	if (cuedMessages == null) {
    		return null;
    	}
    	if (remove) {
    		this.clearMessageQueue(channelID);
    	}
		return cuedMessages;
    }
    
    public boolean hasCuedMessages (int channelID) {
    	List<MessageWrapper> cuedMessages = this.queuedMessages.get(channelID);
    	if (cuedMessages == null) {
    		return false;
    	}
    	if (cuedMessages.size() == 0) {
    		return false;
    	}
    	return true;
    }
    
    /**
     * Removes all the messages for the current user regarding the specified channel
     * 
     * @param channelID The ID for the channel to remove messages related to
     */
    public void clearMessageQueue (int channelID) {
    	this.queuedMessages.replace(channelID, new CopyOnWriteArrayList<MessageWrapper>());
    }

	@Override
	public void disconnectUser() {
		// TODO Auto-generated method stub

	}

}
