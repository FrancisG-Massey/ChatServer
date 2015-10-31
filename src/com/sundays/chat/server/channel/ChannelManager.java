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
package com.sundays.chat.server.channel;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import com.sundays.chat.io.ChannelDataIO;
import com.sundays.chat.io.ChannelDetails;
import com.sundays.chat.io.ChannelIndex;
import com.sundays.chat.server.Launcher;
import com.sundays.chat.server.TaskScheduler;
import com.sundays.chat.server.TaskScheduler.TaskPriority;
import com.sundays.chat.server.Settings;
import com.sundays.chat.server.message.MessagePayload;
import com.sundays.chat.server.message.MessageType;

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
    
    private final Launcher server;
    
    //A joinLock can be applied to prevent users from joining new channels.
    protected boolean joinLock = false;
    
    //A thread which runs automatically, cleaning up unused data and performing channel maintenance tasks
    //private ChannelCleanup cleanupThread = new ChannelCleanup();
    
    //The interface used to connect to the channel permanent data back-end
    private final ChannelDataIO permDataUpdater;
    
    private final ArrayList<Channel> channelUnloadQueue = new ArrayList<Channel>();
    
    /**
     * Manager and channel initialisation section
     */
    public ChannelManager (Launcher server) {
    	this.permDataUpdater = server.getIO().getChannelIO();
    	this.channelResolver = server.getIO().getChannelIndex();
    	this.server = server;
        //loadChannelIndex();//Initialise the channel index
    	server.serverTaskScheduler().scheduleStandardTask(getDefaultCleanups(),//Adds the default tasks to the cleanup thread.
        		5, Settings.channelCleanupThreadFrequency, TimeUnit.SECONDS, true);//Schedule the channel cleanup thread, which removes any obsolete information on a regular basis and saves the channel permanent data.
        setShutdownTasks(server.serverTaskScheduler());//Sets the tasks which need to be run when the server is shut down.
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
    
    private void setShutdownTasks (TaskScheduler taskCue) {
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
    			
    			try {
					permDataUpdater.commitChanges();
				} catch (IOException e) {
					e.printStackTrace();
				}
    		}
    	}, TaskPriority.HIGH);//Sets synchronising the channel permanent data as a high-priority shutdown task
    }
	
	private Runnable getDefaultCleanups () {
		return new Runnable () {
			@Override
			public void run() {
				//Channel-specific tasks
				for (Channel c : channels.values()) {
					if (c.getUserCount() == 0) {
						//Unloads any empty channels that were not automatically unloaded
						queueChannelUnload(c.getId());
						/*try {
							unloadChannel(c);
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}*/
					} else {
						//Runs through other cleanup tasks
						for (Entry<Integer, Long> ban : c.getTempBans().entrySet()) {
							//Removes all expired temporary bans
							if (ban.getValue() < System.currentTimeMillis()) {
								c.getTempBans().remove(ban.getKey());
							}
						}
						if (c.flushRequired) {
							//If the channel data is required to be flushed, flush the details for the channel.
							//c.flushPermissions();
							try {
								permDataUpdater.updateDetails(c.getId(), c.getChannelDetails());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							c.flushRequired = false;
						}
						
						if (c.getLockExpireTime() < System.currentTimeMillis()) {
							//If the existing channel lock has expired, remove it.
							c.removeLock();
						}
					}
				}
				try {
					permDataUpdater.commitChanges();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
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
    	Channel channel = channels.get(cID);
    	if (channel == null) {
    		response.put("isLoaded", false);//Channel is not loaded
    	} else {
    		response.put("isLoaded", true);
    		response.put("memberCount", channel.getUserCount());
    		response.put("guestsCanJoin", channel.getGroup(ChannelGroup.GUEST_GROUP).hasPermission(ChannelPermission.JOIN));
    		response.put("details", ChannelMessageFactory.getInstance().createDetailsMessage(channel, server.getUserManager()));
    	}    	
    	return response;
    }

    protected void loadChannel (int channelID) throws IOException {
        if (!channels.containsKey(channelID)) {
            ChannelDetails details = permDataUpdater.getChannelDetails(channelID);
            channels.put(channelID, new Channel(channelID, details, permDataUpdater));
            //System.out.println("Channel '"+channels.get(channelID).getName() +"' has been loaded onto the server.");
        }
        
    }
    
    protected boolean queueChannelUnload (int channelID) {
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
            for (ChannelUser u : c.getUsers()) {
            	this.sendChannelLocalMessage(u, "You have been removed from the channel.", 155, c.getId(), Color.RED);
            	server.getChannelAPI().leaveChannel(u);
            }            
            channels.remove(c.getId());
            System.out.println("Channel '"+c.getName()+"' has been unloaded from the server.");
        }
    }
    
    protected Channel getChannel (int cID) {
    	return channels.get(cID);
    }
    
    /**
     * Sends a system notification to all members currently in the channel.
     * 
     * @param message The message string to be sent
     * @param messageCode The numerical code linked to the message being sent
     * @param channelID The ID for the channel for which this message is related to.
     */
    protected void sendChannelGlobalMessage (String message, int messageCode, int channelID) {
    	this.sendChannelGlobalMessage(message, messageCode, channelID, Color.BLACK);
    }
    
    protected void sendChannelGlobalMessage (String message, int messageCode, int channelID, Color msgColour) {
    	//Sends a channel system notification to all members of the channel
    	Channel channel = channels.get(channelID);
        if (channel == null) {
            return;
        }
        MessagePayload messagePayload = new MessagePayload();
        
    	messagePayload.put("message", message);
    	messagePayload.put("code", messageCode);
    	messagePayload.put("colour", "#"+Integer.toHexString(msgColour.getRGB()));
    	
        channel.addToMessageCache(messagePayload);
        
        for (ChannelUser u1 : channel.getUsers()) {
        	u1.sendMessage(MessageType.CHANNEL_SYSTEM_GLOBAL, channelID, messagePayload);
        }
    }
    
    /**
     * Sends a system notification to only the specified member of the channel.
     * 
     * @param user		The user to send the desired message to
     * @param message	The message string to send to the desired user
     * @param messageCode	The numerical code linked to the message being sent (allows for localisation on the receiving device)
     * @param channelID		The ID for the channel for which this message is related to.
     */ 
    protected void sendChannelLocalMessage (ChannelUser user, String message, int messageCode, int channelID) {
    	this.sendChannelLocalMessage(user, message, messageCode, channelID, Color.BLACK);
    }
    
    /**
     * Sends a system notification to only the specified member of the channel.
     * 
     * @param user The user to send the desired message to
     * @param message The message string to send to the desired user
     * @param messageCode The numerical code linked to the message being sent (allows for localisation on the receiving device)
     * @param channelID The ID for the channel for which this message is related to.
     * @param msgColour The colour for the message
     */
    protected void sendChannelLocalMessage (ChannelUser user, String message, int messageCode, int channelID, Color msgColour) {
    	Channel channel = channels.get(channelID);
        if (channel == null) {
            return;
        }    	
        MessagePayload messagePayload = new MessagePayload();

        messagePayload.put("message", message);
        messagePayload.put("code", messageCode);
        messagePayload.put("colour", "#"+Integer.toHexString(msgColour.getRGB()));
        
        user.sendMessage(MessageType.CHANNEL_SYSTEM_LOCAL, channelID, messagePayload);
    }
    
    
    protected void logChannelAction (int channelID, ChannelUser reporter, String message) throws JSONException {

    }
}
