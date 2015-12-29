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

import com.sundays.chat.io.UserDetails;
import com.sundays.chat.server.channel.ChannelUser;
import com.sundays.chat.server.message.MessagePayload;
import com.sundays.chat.server.message.MessageType;

/**
 * Represents a user within the chat system. 
 * 
 * @author Francis
 */
public class User implements ChannelUser {
    private final int userID;
    private int currentChannel;
    //private UserSession session;
    private String sessionID;
    private ConcurrentHashMap<Integer, List<UserMessageWrapper>> queuedMessages = new ConcurrentHashMap<>();
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
    
    /* (non-Javadoc)
	 * @see com.sundays.chat.server.user.ChannelUser#getUsername()
	 */
    @Override
	public String getName () {
        return this.username;
    }
    
    /* (non-Javadoc)
	 * @see com.sundays.chat.server.user.ChannelUser#getUserID()
	 */
    @Override
	public int getId () {
        return this.userID;
    }
    
    public String getSessionID () {
    	return this.sessionID;
    }
    
    public void setSessionID (String sessionID) {
    	this.sessionID = sessionID;
    }
    
    /* (non-Javadoc)
	 * @see com.sundays.chat.server.user.ChannelUser#getChannel()
	 */
    @Override
	public int getChannelId () {
        return this.currentChannel;
    }
    
    /* (non-Javadoc)
	 * @see com.sundays.chat.server.user.ChannelUser#setChannel(com.sundays.chat.server.channel.Channel)
	 */
    @Override
	public void setChannel (int channelId) {
        this.currentChannel = channelId;
        if (!connected) {
            return;
        }
        if (channelId == -1) {
        	//Disconnect from channel. Now handled in the ChannelManager.leaveChannel() method 
        } else {
        	//Connected to channel
        	if (this.queuedMessages.get(channelId) == null) {
        		//If there is no message queue for this channel, create it.
        		this.queuedMessages.put(channelId, new ArrayList<UserMessageWrapper>());
        	}
        }                
    }
    
    /* (non-Javadoc)
	 * @see com.sundays.chat.server.user.ChannelUser#sendMessage(com.sundays.chat.server.message.MessageType, int, com.sundays.chat.server.message.MessagePayload)
	 */
    @Override
	public void sendMessage (MessageType type, int channelID, MessagePayload payload) {
    	UserMessageWrapper wrapper = new UserMessageWrapper(getOrderID(), type, System.currentTimeMillis(), this.userID, payload);
    	
    	List<UserMessageWrapper> queuedMessages = this.queuedMessages.get(channelID);
    	if (queuedMessages == null) {
    		//If the channel queue does not exist for this user, create it.
    		queuedMessages = new CopyOnWriteArrayList<UserMessageWrapper>();
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
    public List<UserMessageWrapper> getQueuedMessages (int channelID, boolean remove) {
    	// 'remove' is used to specify if the messages should be removed from the cue.
    	List<UserMessageWrapper> queuedMessages = this.queuedMessages.get(channelID);
    	if (queuedMessages == null) {
    		return null;
    	}
    	if (remove){
    		this.clearMessageQueue(channelID);
    	}
		return queuedMessages;
    }
    
    public boolean hasQueuedMessages (int channelID) {
    	List<UserMessageWrapper> cuedMessages = this.queuedMessages.get(channelID);
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
    	this.queuedMessages.replace(channelID, new CopyOnWriteArrayList<UserMessageWrapper>());
    }

	/* (non-Javadoc)
	 * @see com.sundays.chat.server.user.ChannelUser#getDefaultChannel()
	 */
	@Override
	public int getDefaultChannel() {
		return defaultChannel;
	}

	/* (non-Javadoc)
	 * @see com.sundays.chat.server.user.ChannelUser#setDefaultChannel(int)
	 */
	@Override
	public void setDefaultChannel(int defaultChannel) {
		this.defaultChannel = defaultChannel;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "User [userID=" + userID + ", connected=" + connected + ", username=" + username + "]";
	}
}
