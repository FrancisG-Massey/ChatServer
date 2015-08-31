/*******************************************************************************
 * Copyright (c) 2015 Francis G.
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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.sundays.chat.io.UserDetails;
import com.sundays.chat.server.channel.Channel;
import com.sundays.chat.server.message.MessagePayload;
import com.sundays.chat.server.message.MessageType;

/**
 * Represents a user within the chat system. 
 * 
 * @author Francis
 */
public class User {
    private final int userID;
    private Channel currentChannel;
    private String sessionID;
    private ConcurrentHashMap<Integer, List<JSONObject>> queuedMessages = new ConcurrentHashMap<Integer, List<JSONObject>>();
    public Boolean connected = true;
    private int nextOrderID = 10;
    
    private String username;
    private int defaultChannel;
    
    private UserDetails details;

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
        		this.queuedMessages.put(newchannel.getID(), new ArrayList<JSONObject>());
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
    	JSONObject message = new JSONObject(payload);
    	try {
			message.put("type", type.getID());
			addQueuedMessage(channelID, message);
		} catch (JSONException e) {
			e.printStackTrace();
		}
    }
    
    /**
     * Adds a message to the user's message queue.<br />
     * NOTE: This method is deprecated. {@link #sendMessage(MessageType, int, Map)} should be used instead.
     * @param channelID The ID of the channel the message is from.
     * @param message The message object to queue.
     * @throws JSONException
     */
    @Deprecated
    private void addQueuedMessage (int channelID, JSONObject message) throws JSONException {
    	//Places a new message in the user's message cue. Message cue is channel-specific
    	if (message == null) {
    		//Don't bother with null messages; these are caused by a glitch in the system somewhere
    		return;
    	}
    	message.put("orderID", getOrderID());
    	message.put("timestamp", new Date().getTime());
    	List<JSONObject> cuedMessages = this.queuedMessages.get(channelID);
    	if (cuedMessages == null) {
    		//If the channel cue does not exist for this user, create it.
    		cuedMessages = new CopyOnWriteArrayList<JSONObject>();
    		this.queuedMessages.put(channelID, cuedMessages);
    	}
    	cuedMessages.add(message);
    }
    
    public List<JSONObject> getQueuedMessages (int channelID, boolean remove) {
    	//Retrieves the cued messages for the specified channel. 'remove' is used to specify if the messages should be removed from the cue.
    	List<JSONObject> cuedMessages = this.queuedMessages.get(channelID);
    	if (cuedMessages == null) {
    		return null;
    	}
    	if (remove){
    		this.clearMessageQueue(channelID);
    	}
		return cuedMessages;
    }
    
    public List<JSONObject> getQueuedMessages (int channelID, boolean remove, int[] types) {
    	//Retrieves the cued messages for the specified channel. 'remove' is used to specify if the messages should be removed from the cue.
    	List<JSONObject> cuedMessages = this.queuedMessages.get(channelID);
    	if (cuedMessages == null) {
    		return null;
    	}
    	if (remove) {
    		this.clearMessageQueue(channelID);
    	}
		return cuedMessages;
    }
    
    public boolean hasCuedMessages (int channelID) {
    	List<JSONObject> cuedMessages = this.queuedMessages.get(channelID);
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
    	this.queuedMessages.replace(channelID, new CopyOnWriteArrayList<JSONObject>());
    }

	public int getDefaultChannel() {
		return defaultChannel;
	}

	public void setDefaultChannel(int defaultChannel) {
		this.details.setDefaultChannel(defaultChannel);
	}
}
