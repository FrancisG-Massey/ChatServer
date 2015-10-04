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
package com.sundays.chat.server.user;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.sundays.chat.api.UserSession;
import com.sundays.chat.io.UserDetails;
import com.sundays.chat.server.channel.Channel;
import com.sundays.chat.server.message.MessagePayload;
import com.sundays.chat.server.message.MessageType;
import com.sundays.chat.server.message.MessageWrapper;

/**
 * Represents a user within the chat system. 
 * 
 * @author Francis
 */
public class User {
    private final int userID;
    private Channel currentChannel;
    private UserSession session;
    private String sessionID;
    private ConcurrentHashMap<Integer, List<MessageWrapper>> queuedMessages = new ConcurrentHashMap<>();
    public Boolean connected = true;
    private int nextOrderID = 10;
    
    private String username;
    private int defaultChannel;

	public User (int id, UserDetails details) {
        this.userID = id;
        this.username = details.getUsername();
        this.defaultChannel = details.getDefaultChannel();
    }    
    
    private int getOrderID () {
    	nextOrderID++;
    	return nextOrderID;
    }  
    
    public String getUsername () {
        return this.username;
    }
    
    public int getUserID () {
        return this.userID;
    }
    
    public String getSessionID () {
    	return this.sessionID;
    }
    
    public void setSessionID (String sessionID) {
    	this.sessionID = sessionID;
    }
    
    public Channel getChannel () {
        return this.currentChannel;
    }
    
    public void setChannel (Channel newchannel) {
        this.currentChannel = newchannel;
        if (!connected) {
            return;
        }
        if (newchannel == null) {
        	//Disconnect from channel. Now handled in the ChannelManager.leaveChannel() method 
        } else {
        	//Connected to channel
        	if (this.queuedMessages.get(newchannel.getID()) == null) {
        		//If there is no message queue for this channel, create it.
        		this.queuedMessages.put(newchannel.getID(), new ArrayList<MessageWrapper>());
        	}
        }                
    }
    
    /**
     * Sends a message to the user. 
     * Depending on how the user is connected, this message will either be sent immediately to the user or added to their message queue.
     * @param type The type of message being sent.
     * @param channelID The ID of the channel the message is from.
     * @param payload The payload data for the message.
     */
    public void sendMessage (MessageType type, int channelID, MessagePayload payload) {
    	MessageWrapper wrapper = new MessageWrapper(getOrderID(), type, System.currentTimeMillis(), this.userID, payload);
    	
    	List<MessageWrapper> queuedMessages = this.queuedMessages.get(channelID);
    	if (queuedMessages == null) {
    		//If the channel queue does not exist for this user, create it.
    		queuedMessages = new CopyOnWriteArrayList<MessageWrapper>();
    		this.queuedMessages.put(channelID, queuedMessages);
    	}
    	queuedMessages.add(wrapper);
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

	public int getDefaultChannel() {
		return defaultChannel;
	}

	public void setDefaultChannel(int defaultChannel) {
		this.defaultChannel = defaultChannel;
	}
}
