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
package com.sundays.chat.server.channel;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import com.sundays.chat.io.ChannelIndex;
import com.sundays.chat.io.ChannelDataManager;
import com.sundays.chat.server.ChatServer;
import com.sundays.chat.server.Permission;
import com.sundays.chat.server.ServerTaskQueue;
import com.sundays.chat.server.ServerTaskQueue.TaskPriority;
import com.sundays.chat.server.Settings;
import com.sundays.chat.server.Settings.Message;
import com.sundays.chat.server.User;

/**
 * Java chat server Channel Manager
 * Used to create, remove, load, and run chat channels
 * 
 * @author Francis
 */
public class ChannelManager {
	
    //Creates a map linking the channel ID with the channel's class
    private final ConcurrentHashMap<Integer, Channel> channels = new ConcurrentHashMap<Integer, Channel>();
    
    //A map which contains all valid channel IDs and names. It is used for resolving channel names into IDs
    private final ChannelIndex channelResolver;
    
    //A joinLock can be applied to prevent users from joining new channels.
    protected boolean joinLock = false;
    
    //A thread which runs automatically, cleaning up unused data and performing channel maintenance tasks
    //private ChannelCleanup cleanupThread = new ChannelCleanup();
    
    //The interface used to connect to the channel permanent data back-end
    private final ChannelDataManager permDataUpdater;
    
    private final ArrayList<Channel> channelUnloadQueue = new ArrayList<Channel>();
    
    /**
     * Manager and channel initialisation section
     */
    public ChannelManager (ChannelDataManager dataUpdater, ChannelIndex channelResolver) {
    	this.permDataUpdater = dataUpdater;
    	this.channelResolver = channelResolver;
        //loadChannelIndex();//Initialise the channel index
        ChatServer.getInstance().serverTaskScheduler().scheduleStandardTask(getDefaultCleanups(),//Adds the default tasks to the cleanup thread.
        		5, Settings.channelCleanupThreadFrequency, TimeUnit.SECONDS, true);//Schedule the channel cleanup thread, which removes any obsolete information on a regular basis and saves the channel permanent data.
        setShutdownTasks(ChatServer.getInstance().serverTaskScheduler());//Sets the tasks which need to be run when the server is shut down.
    }
    
    /*protected void loadChannelIndex () {
    	String query = "SELECT `channelID`, `channelName` FROM channels";
        try {
            SelectQuery q = new SelectQuery(dbCon, query);//Fetch the names and IDs of all the channels avaliable
            ResultSet res = q.execute();
            channelResolver.clear();//Reset the index
            while (res.next()) {
            	//Loop through all the channels received and placed them in the index
                System.out.println("Channel "+res.getInt(1)+": "+res.getString(2));
                channelResolver.put(res.getString(2).toLowerCase(), res.getInt(1));
            }
        } catch (SQLException e) {
            System.err.println("Failed to load channel index: "+e.getMessage());
        }
        System.out.println("Found "+channelResolver.size()+" channels");
    }*/
    
    private void setShutdownTasks (ServerTaskQueue taskCue) {
    	taskCue.addShutdownTask(new Runnable () {
    		@Override
    		public void run () {
		    	Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Server is shutting down. Running final channel cleanup tasks.");
		    	joinLock = true;
		    	for (Channel c : channels.values()) {
		    		try {
						unloadChannel(c);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		    	}
		    	
    		}
    	}, TaskPriority.NORMAL);
    	
    	taskCue.addShutdownTask(new Runnable () {
    		@Override
    		public void run () {
    			joinLock = true;
    			
    			permDataUpdater.commitPendingChanges();
    		}
    	}, TaskPriority.HIGH);//Sets synchronising the channel permanent data as a high-priority shutdown task
    }
	
	private Runnable getDefaultCleanups () {
		return new Runnable () {
			@Override
			public void run() {
				//Channel-specific tasks
				for (Channel c : channels.values()) {
					if (c.getNoUsers() == 0) {
						//Unloads any empty channels that were not automatically unloaded
						cueChannelUnload(c.channelID);
						/*try {
							unloadChannel(c);
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}*/
					} else {
						//Runs through other cleanup tasks
						Date currentTime = new Date();
						for (Entry<Integer, Date> ban : c.getTempBans().entrySet()) {
							//Removes all expired temporary bans
							if (ban.getValue().before(currentTime)) {
								c.getTempBans().remove(ban.getKey());
							}
						}
						if (c.flushRequired) {
							//If the channel data is required to be flushed, flush the details for the channel.
							//c.flushPermissions();
							permDataUpdater.syncDetails(c.channelID, c.getChannelDetails());
							c.flushRequired = false;
						}
						
						if (c.getLockExpireDate() != null && c.getLockExpireDate().before(new Date())) {
							//If the existing channel lock has expired, remove it.
							c.removeLock();
						}
					}
				}
				permDataUpdater.commitPendingChanges();
				synchronized (channelUnloadQueue) {
					for (Channel c : channelUnloadQueue) {
						try {
							unloadChannel(c);
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					channelUnloadQueue.clear();
				}
			}			
		};
	}
    
    public boolean channelExists (int channelID) {
    	return channelResolver.channelExists(channelID);
    }
    
    public boolean channelLoaded (int channelID) {
    	return channels.containsKey(channelID);
    }
    
    public int getChannelID (String name) {
    	return channelResolver.lookupByName(name);
    }
    
    public JSONObject getChannelInformation (int cID) throws JSONException {
    	JSONObject response = new JSONObject();
    	Channel c = channels.get(cID);
    	if (c == null) {
    		response.put("isLoaded", false);//Channel is not loaded
    	} else {
    		response.put("isLoaded", true);
    		response.put("memberCount", c.getNoUsers());
    		response.put("guestsCanJoin", (c.getPermissionValue(Permission.JOIN) <= Settings.GUEST_RANK));
    		response.put("details", ChannelDataPreparer.prepareChannelDetails(c));
    	}    	
    	return response;
    }

    protected void loadChannel (int channelID) {
        if (!channels.containsKey(channelID)) {
            channels.put(channelID, new Channel(channelID, permDataUpdater));
            //System.out.println("Channel '"+channels.get(channelID).getName() +"' has been loaded onto the server.");
        }
        
    }
    
    protected boolean cueChannelUnload (int channelID) {
    	Channel c = channels.get(channelID);
    	if (c == null) {
    		return false;//Channel not found
    	}
    	synchronized (channelUnloadQueue) {
	    	if (channelUnloadQueue.contains(c)) {
	    		c.unloadInitialised = true;
	    		return true;//Unload already cued
	    	}
	    	channelUnloadQueue.add(c);
    		c.unloadInitialised = true;
    	}
    	return true;
    }
    
    protected boolean removeChannelUnload (Channel c) {
    	synchronized (channelUnloadQueue) {
	    	if (channelUnloadQueue.contains(c)) {
		    	channelUnloadQueue.remove(c);
		    	c.unloadInitialised = false;
		    	return true;
	    	}
    	}
    	return false;
    }

    private void unloadChannel (Channel c) throws JSONException {    
        if (c != null) {
	    	c.unloadInitialised = true;
            for (User u : c.getUsers()) {
            	this.sendChannelLocalMessage(u, "You have been removed from the channel.", 155, c.channelID, Color.RED);
                ChatServer.getInstance().channelAPI().leaveChannel(u);
            }            
            channels.remove(c.channelID);
            System.out.println("Channel '"+c.getName()+"' has been unloaded from the server.");
        }
    }
    
    protected Channel getChannel (int cID) {
    	return channels.get(cID);
    }
    
    /**
     * Function for sending data to channel users
     */ 
    protected void sendChannelGlobalMessage (String message, int messageCode, int channelID) throws JSONException {
    	this.sendChannelGlobalMessage(message, messageCode, channelID, Color.BLACK);
    }
    protected void sendChannelGlobalMessage (String message, int messageCode, int channelID, Color msgColour) throws JSONException {
    	//Sends a channel system notification to all members of the channel
    	Channel c = channels.get(channelID);
        if (c == null) {
            return;
        }    	
    	JSONObject messageObject = new JSONObject();
        messageObject.put("id", c.getNextMessageID());
        messageObject.put("type", Message.CHANNEL_SYSTEM_GLOBAL);//Type 3 = channel system message (global)
        messageObject.put("message", message);
        messageObject.put("messageCode", messageCode);
        messageObject.put("messageColour", msgColour.getRGB());
        c.addToMessageCache(messageObject);
        for (User u1 : c.getUsers()) {
        	u1.addQueuedMessage(channelID, messageObject);
        }
    }
    
    /**
     * Sends a channel system notification to only the specified member of the channel
     * 
     * @param u		The user to send the desired message to
     * @param message	The message string to send to the desired user
     * @param messageCode	The numerical code linked to the message being sent (allows for localisation on the receiving device)
     * @param channelID		The ID for the channel for which this message is related to.
     * @throws JSONException	If an error occurs in attempting to place message data into a JSONObject, throw an exception (this should never occur).
     */ 
    protected void sendChannelLocalMessage (User u, String message, int messageCode, int channelID) throws JSONException {
    	this.sendChannelLocalMessage(u, message, messageCode, channelID, Color.BLACK);
    }
    protected void sendChannelLocalMessage (User u, String message, int messageCode, int channelID, Color msgColour) throws JSONException {
    	Channel c = channels.get(channelID);
        if (c == null) {
            return;
        }    	
    	JSONObject messageObject = new JSONObject();
        messageObject.put("id", c.getNextMessageID());
        messageObject.put("type", Message.CHANNEL_SYSTEM_LOCAL);//Type 4 = channel system message (local)
        messageObject.put("message", message);
        messageObject.put("messageCode", messageCode);
        messageObject.put("messageColour", msgColour.getRGB());
        u.addQueuedMessage(channelID, messageObject);
    }
    
    
    protected void logChannelAction (int channelID, User reporter, String message) throws JSONException {

    }
}
