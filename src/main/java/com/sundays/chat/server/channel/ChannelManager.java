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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.sundays.chat.io.ChannelDataIO;
import com.sundays.chat.io.ChannelDetails;
import com.sundays.chat.io.ChannelIndex;
import com.sundays.chat.io.IOManager;
import com.sundays.chat.server.Settings;
import com.sundays.chat.server.TaskScheduler;
import com.sundays.chat.server.TaskScheduler.TaskPriority;
import com.sundays.chat.server.message.MessagePayload;
import com.sundays.chat.server.message.MessageType;
import com.sundays.chat.server.user.UserLookup;

/**
 * Java chat server Channel Manager
 * Used to create, remove, load, and run chat channels
 * 
 * @author Francis
 */
public class ChannelManager {
	
	/**
	 * 
	 */
	private static final Logger logger = Logger.getLogger(ChannelManager.class);
	
    /**
     * Creates a map linking the channel ID with the channel's class
     */
    private final ConcurrentHashMap<Integer, Channel> channels = new ConcurrentHashMap<Integer, Channel>();
    
    /**
     * A map which contains all valid channel IDs and names. It is used for resolving channel names into IDs
     */
    private final ChannelIndex channelIndex;
    
    private final UserLookup userManager;
    
    /**
     * A joinLock can be applied to prevent users from joining new channels.
     */
    protected boolean joinLock = false;
    
    /**
     * The interface used to connect to the channel permanent data back-end
     */
    private final ChannelDataIO channelIO;
    
    private final List<Channel> channelUnloadQueue = new ArrayList<Channel>();
	
	private final ChannelPacketFactory messageFactory;
    
    /**
     * Manager and channel initialisation section
     */
    public ChannelManager (IOManager ioHub, UserLookup userManager, TaskScheduler taskQueue) {
    	this.channelIO = ioHub.getChannelIO();
    	this.channelIndex = ioHub.getChannelIndex();
    	this.userManager = userManager;
    	setTasks(taskQueue);//Sets the tasks which need to be run when the server is shut down.
        messageFactory = ChannelPacketFactory.getInstance();
    }
    
    private void setTasks (TaskScheduler taskQueue) {
    	taskQueue.scheduleStandardTask(getDefaultCleanups(),//Adds the default tasks to the cleanup thread.
        		5, Settings.channelCleanupThreadFrequency, TimeUnit.MINUTES, true);//Schedule the channel cleanup thread, which removes any obsolete information on a regular basis and saves the channel permanent data.
    	taskQueue.addShutdownTask(new Runnable () {
    		@Override
    		public void run () {
    			logger.info("Server is shutting down. Running final channel cleanup tasks.");
		    	joinLock = true;
		    	for (Channel c : channels.values()) {
		    		unloadChannel(c);
		    	}
		    	
    		}
    	}, TaskPriority.NORMAL);
    	
    	taskQueue.addShutdownTask(new Runnable () {
    		@Override
    		public void run () {
    			joinLock = true;
    			
    			try {
					channelIO.commitChanges();
				} catch (IOException ex) {
					logger.error("Error commiting channel changes within the backend", ex);
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
								channelIO.updateDetails(c.getId(), c.getChannelDetails());
							} catch (IOException ex) {
								logger.error("Error updating details for channel "+c.getId(), ex);
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
					channelIO.commitChanges();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				synchronized (channelUnloadQueue) {
					for (Channel c : channelUnloadQueue) {
						unloadChannel(c);
					}
					channelUnloadQueue.clear();
				}
			}			
		};
	}
    
    public boolean channelExists (int channelID) {
    	return channelIndex.channelExists(channelID);
    }
    
    public boolean channelLoaded (int channelID) {
    	return channels.containsKey(channelID);
    }
    
    public int getChannelID (String name) {
    	return channelIndex.lookupByName(name);
    }

    protected void loadChannel (int channelID) throws IOException {
        if (!channels.containsKey(channelID)) {
            ChannelDetails details = channelIO.getChannelDetails(channelID);
            channels.put(channelID, new Channel(channelID, details, channelIO));
            
            logger.debug("Channel '"+channels.get(channelID).getName() +"' has been loaded onto the server.");
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
	    		return true;//Unload already queued
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

    private void unloadChannel (Channel c) {    
        if (c != null) {
	    	c.unloadInitialised = true;
            for (ChannelUser u : c.getUsers()) {
            	this.sendChannelLocalMessage(u, "You have been removed from the channel.", 155, c.getId(), Color.RED);
            	leaveChannel(u, c.getId());
            }            
            channels.remove(c.getId());
            logger.info("Channel '"+c.getName()+"' has been unloaded from the server.");
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
    
    
    protected void logChannelAction (int channelID, ChannelUser reporter, String message) {
    	//TODO: Implement
    }
    
    /**
     * Gets the basic details for this channel, including the following items:
     * <ul>
     * 	<li>memberCount - The number of members belonging to the channel</li>
     *  <li>guestsCanJoin - True if users who aren't channenl members can join, false otherwise</li>
     *  <li>name - The name of the channel</li>
     *  <li>welcomeMessage - The message displayed to users when they join the channel</li>
     *  <li>messageColour - The colour used to display the welcome message</li>
     *  <li>owner.id - The user ID of the channel owner</li>
     *  <li>owner.name - The username of the channel owner</li>
     * </ul>
     * @param channelID The ID of the channel to retrieve details for
     * @param load True if the channel should be loaded (if not already), false otherwise
     * @return A {@link MessagePayload} containing the channel details, or null if the details couldn't be found
     */
    public MessagePayload getChannelDetails (int channelID, boolean load) {
        Channel channel = getChannel(channelID);
        if (channel == null) {
        	if (!load) {
        		return null;
        	}
        	try {
				loadChannel(channelID);
			} catch (IOException ex) {
				logger.error("Failed to load channel "+channelID, ex);
				return null;
			}
        	channel = getChannel(channelID);
        	if (channel == null) {
        		return null;
        	}
        }
        return messageFactory.createDetailsMessage(channel, userManager);
    }
        
    public MessagePayload getUserList (int channelID) {
    	Channel channel = getChannel(channelID);
        MessagePayload channelList;
        if (channel == null) {
        	channelList = new MessagePayload();
        	channelList.put("id", channelID);
			channelList.put("totalUsers", 0);	
        } else {
        	channelList = messageFactory.createChannelUserList(channel);
        }
        return channelList;
    }
    
    public MessagePayload getMemberList (int channelID) {
        Channel channel = getChannel(channelID);
        if (channel == null) {
        	try {
				loadChannel(channelID);
			} catch (IOException ex) {
				logger.error("Failed to load channel "+channelID, ex);
				return null;
			}
        	channel = getChannel(channelID);
        	if (channel == null) {
        		return null;
        	}
        }
        return messageFactory.createMemberList(channel, userManager);
    }
    
    public MessagePayload getBanList (int channelID) {
        Channel channel = getChannel(channelID);
        if (channel == null) {
        	try {
				loadChannel(channelID);
			} catch (IOException ex) {
				logger.error("Failed to load channel "+channelID, ex);
				return null;
			}
        	channel = getChannel(channelID);
        	if (channel == null) {
        		return null;
        	}
        }
        return messageFactory.createBanList(channel, userManager);
    }
    
    public MessagePayload getChannelGroups (int channelID) {
    	Channel channel = getChannel(channelID);
        if (channel == null) {
        	try {
				loadChannel(channelID);
			} catch (IOException ex) {
				logger.error("Failed to load channel "+channelID, ex);
				return null;
			}
        	channel = getChannel(channelID);
        }
    	return messageFactory.createGroupList(channel);
    }
    
    /*
     * Basic functions (join, leave, send message)
     */
    public ChannelResponse joinChannel (ChannelUser user, int channelId) {
        Channel channel = getChannel(channelId);
        if (channel == null) {
            //Checks if the channel is currently loaded
            if (!channelExists(channelId)) {
                //Checks if the channel actually exists
            	//100 The channel you have attempted to join does not exist.
                return new ChannelResponse(ChannelResponseType.CHANNEL_NOT_FOUND);
            } else {
                //If the channel exists but is not loaded, load the channel
                try {
					loadChannel(channelId);
				} catch (IOException ex) {
					logger.error("Failed to load channel "+channelId, ex);
					return new ChannelResponse(ChannelResponseType.UNKNOWN_ERROR);
				}
                channel = getChannel(channelId);
            }            
        }
        if (channelId == user.getChannelId()) {
			//166 You are already in this channel.\nJoining it again will have no effect.
			return new ChannelResponse(ChannelResponseType.NO_CHANGE);
        }
        if (channel.unloadInitialised || joinLock) {
            //Checks if the channel is currently being unloaded, or if a joinLock has been applied (we don't want people joining during this period, as it may stop the unload process from working)
        	//101 You cannot join the channel at this time.\nPlease try again in a few minutes.
            return new ChannelResponse(ChannelResponseType.UNKNOWN_ERROR);
        }
        if (channel.isUserBanned(user.getId())) {
        	//User has been permanently banned from the channel
            //102 You are permanently banned from this channel.
            return new ChannelResponse(ChannelResponseType.BANNED);
        }
        if (!channel.userHasPermission(user, ChannelPermission.JOIN)) {
            //Check if user has permission to join (0 = join permission)
            //103 You do not have a high enough rank to join this channel.
            return new ChannelResponse(ChannelResponseType.NOT_AUTHORISED_GENERAL);
        }
        if (channel.getLockExpireTime() > System.currentTimeMillis() && channel.getUserRank(user) <= channel.getLockRank()) {
        	//Checks if the channel is locked to new users.
        	//174 This channel has been locked for anyone holding the rank of "+channel.getGroupName(channel.getLockRank())+" or below.
        	Map<String, Serializable> args = new HashMap<>();
        	args.put("lockGroup", channel.getGroupName(channel.getLockRank()));
        	return new ChannelResponse(ChannelResponseType.LOCKED, args);
        }
        if (channel.getBanExpireTime(user.getId()) > System.currentTimeMillis()) {
            //Check if user is temporarily banned from the channel
            //104 You are temporarily banned from the channel ("+(timeRemaining/(60*1000)+1)+" minute(s) remaining).
            Map<String, Serializable> args = new HashMap<>();
        	args.put("banExpires", channel.getBanExpireTime(user.getId())*1000);
        	return new ChannelResponse(ChannelResponseType.BANNED_TEMP, args);
        }
        channel.addUser(user);//Adds the user to the channel
        //Send a notification of the current user joining to all users currently in the channel
        MessagePayload userAdditionNotice = messageFactory.createChannelUserAddition(user, channel);
        for (ChannelUser u1 : channel.getUsers()) {
        	u1.sendMessage(MessageType.CHANNEL_LIST_ADDITION, channelId, userAdditionNotice);
        }
        user.setChannel(channel.getId());//Sets the user's channel to the current one

        sendChannelLocalMessage(user, channel.getAttribute("welcomeMessage"), 40, channelId);//Sends the opening message to the user
        
        Map<String, Serializable> args = new HashMap<>();
    	args.put("group", messageFactory.createGroupDetails(channel.getUserGroup(user)));
    	args.put("details", messageFactory.createDetailsMessage(channel, userManager));
        return new ChannelResponse(ChannelResponseType.SUCCESS, args);
    }
    
    public ChannelResponse sendMessage (ChannelUser user, int channelId, String message) {
        Channel channel = getChannel(channelId);
        if (channel == null) {
        	//105 Cannot send message: not currently in a channel.
        	return new ChannelResponse(ChannelResponseType.CHANNEL_NOT_LOADED);
        }
        if (!channel.getUsers().contains(user)) {
        	return new ChannelResponse(ChannelResponseType.NOT_IN_CHANNEL);
        }
        if (!channel.userHasPermission(user, ChannelPermission.TALK)) {
        	//Checks if user has permission to talk in channel (1 = talk permission)

        	//106 You do not have the appropriate permissions to send messages in this channel.
        	return new ChannelResponse(ChannelResponseType.NOT_AUTHORISED_GENERAL);
        }
        
        MessagePayload messagePayload = new MessagePayload();
        
        messagePayload.put("message", message);
        messagePayload.put("senderName", user.getName());
        messagePayload.put("senderGroup", channel.getUserGroup(user).getId());
        messagePayload.put("senderID", user.getId()); 
        
        channel.addToMessageCache(messagePayload);
        
        for (ChannelUser u1 : channel.getUsers()) {
        	//Loops through all the people currently in the channel, sending the message to each of them.
            u1.sendMessage(MessageType.CHANNEL_STANDARD, channel.getId(), messagePayload);
        }

        return new ChannelResponse(ChannelResponseType.SUCCESS);
    }

    public ChannelResponse leaveChannel (ChannelUser user, int channelId) {
        Channel channel = getChannel(channelId);
    	if (channelId == -1) {
    		return new ChannelResponse(ChannelResponseType.NO_CHANGE);
    	}
        if (channel != null) {
        	if (!channel.getUsers().contains(user)) {
        		return new ChannelResponse(ChannelResponseType.NO_CHANGE);
        	}
        	channel.removeUser(user);
            if (channel.getUsers().isEmpty()) {                
                queueChannelUnload(channel.getId());//If the channel is empty, remove it from the server to save resources
            } else {
            	//Notify other users in the channel of this user's departure
            	MessagePayload departureNotice = messageFactory.createChannelUserRemoval(user, channel);
            	for (ChannelUser u1 : channel.getUsers()) {
            		u1.sendMessage(MessageType.CHANNEL_LIST_REMOVAL, channel.getId(), departureNotice);
            	}
            }            
        }
        //Notifies the user of their removal
        user.setChannel(-1);

        user.sendMessage(MessageType.CHANNEL_REMOVAL, channelId, new MessagePayload());
        
        //Returns successful
        //177, "You have left the channel."
        return new ChannelResponse(ChannelResponseType.SUCCESS);
    }
    
    /*
     * Moderator functions
     */
    public ChannelResponse resetChannel (ChannelUser user, int channelId) {
        Channel channel = getChannel(channelId);
        if (channel == null) {//The channel is not currently loaded
        	//107 The channel you have attempted to reset is not loaded.\nResetting will have no effect.
            return new ChannelResponse(ChannelResponseType.CHANNEL_NOT_LOADED);
        }
        if (!channel.userHasPermission(user, ChannelPermission.RESET)) {
        	//Check if user has ability to reset the channel (5 = reset permission)
        	//108 You do not have the appropriate permissions reset this channel.
            return new ChannelResponse(ChannelResponseType.NOT_AUTHORISED_GENERAL);
        }
        sendChannelGlobalMessage("This channel will be reset within the next "+Settings.channelCleanupThreadFrequency+" seconds.\n"
        		+ "All members will be removed.\nYou may join again after the reset.", 109, channel.getId(), Color.BLUE);
        queueChannelUnload(channelId);
        channel.resetLaunched = true;
        //Unloads the channel, then loads it again
        
        //Returns successful
    	//110 Your request to reset this channel has been accepted.
        return new ChannelResponse(ChannelResponseType.SUCCESS);
    }
    
    public ChannelResponse kickUser (ChannelUser user, int channelId, int kickTargetId) {
        Channel channel = getChannel(channelId);
        if (channel == null) {//The channel is not currently loaded
        	//111 The channel you have attempted to kick this user from is not loaded.
        	return new ChannelResponse(ChannelResponseType.CHANNEL_NOT_LOADED);
        }
        if (!channel.userHasPermission(user, ChannelPermission.KICK)) {//Check if user has ability to kick users (2 = kick permission)
        	//112 You do not have the appropriate permissions to kick people from this channel.
        	return new ChannelResponse(ChannelResponseType.NOT_AUTHORISED_GENERAL);
        }
        ChannelUser kickedUser = userManager.getUser(kickTargetId);//Retrieves the user object of the user being kicked
        if (kickedUser == null || !channel.getUsers().contains(kickedUser)) {
        	//Checks if the user is currently logged in and is in the channel
        	
        	//113 This user is not currently in the channel.
            return new ChannelResponse(ChannelResponseType.NOT_IN_CHANNEL);
        }
        if (!channel.canActionUser(user, kickTargetId)) {
        	//Checks if the user banning is allowed to action the target user

        	//114 You can only kick users with a lower rank level than yours.
            return new ChannelResponse(ChannelResponseType.NOT_AUTHORISED_SPECIFIC);
        }
        leaveChannel(kickedUser, channelId);
        sendChannelLocalMessage(kickedUser, "You have been kicked from the channel.", 115, channelId, Color.RED);
        channel.setTempBan(kickTargetId, 60_000);//Sets a 60 second temporary ban (gives the user performing the kick a chance to choose whether or not to temporarily ban)
        
    	//116 Your attempt to kick "+kickedUser.getUsername()+" from this channel was successful.
    	Map<String, Serializable> args = new HashMap<>();
    	args.put("kickedUser", kickedUser.getName());
        return new ChannelResponse(ChannelResponseType.SUCCESS, args);
    }
    
    public ChannelResponse tempBanUser (ChannelUser user, int channelId, int banTargetId, int durationMins) {
        Channel channel = getChannel(channelId);
        if (channel == null) {//The channel is not currently loaded
        	//117 The channel you have attempted to temporarily ban this user from is not loaded.
            return new ChannelResponse(ChannelResponseType.CHANNEL_NOT_LOADED);
        }
    	String bannedName = userManager.getUsername(banTargetId);
    	if (bannedName == null) {
    		bannedName = "[user not found]";
        }

        if (!channel.userHasPermission(user, ChannelPermission.TEMPBAN)) {
        	//Check if user has ability to temp-ban users (3 = temp-ban permission)
        	//118 You cannot temporarily ban people from this channel.
        	return new ChannelResponse(ChannelResponseType.NOT_AUTHORISED_GENERAL);
        }
        if (!channel.canActionUser(user, banTargetId)) {
        	//Checks if the user banning is allowed to action the target user
        	//119 You can only temporarily ban users with a lower rank level than yours.
            return new ChannelResponse(ChannelResponseType.NOT_AUTHORISED_SPECIFIC);
        }
        if (durationMins < 1) {
        	//1 minute ban minimum
            durationMins = 1;
        }
        if (durationMins > 60*6) {
        	//6 hour maximum ban
            durationMins = 60*6;
        }
        channel.setTempBan(banTargetId, durationMins*60_000);//Sets a temporary ban as specified by durationMins

    	//120 You have successfully temporarily banned "+bannedName+" from the channel for "+durationMins+" minutes.\n"
		//+"Their ban will be lifted after the specified time, or if the channel is reset.
    	Map<String, Serializable> args = new HashMap<>();
    	args.put("durationMins", durationMins);
    	args.put("bannedName", bannedName);
		return new ChannelResponse(ChannelResponseType.SUCCESS, args);
    }
    
    public ChannelResponse lockChannel (ChannelUser user, int channelId, int highestRank, int durationMins) {
    	Channel channel = getChannel(channelId);
    	if (channel == null) {//The channel is not currently loaded
        	//168 The channel you have attempted to lock is not currently loaded on this server.
        	return new ChannelResponse(ChannelResponseType.CHANNEL_NOT_LOADED);
        }
    	if (!channel.userHasPermission(user, ChannelPermission.LOCKCHANNEL)) {
        	//Check if user has ability to lock the channel (9 = lock permission)
        	//169 You cannot lock this channel.
        	return new ChannelResponse(ChannelResponseType.NOT_AUTHORISED_GENERAL);
        }
        if (highestRank >= channel.getUserRank(user)) {
        	//Checks if the user is trying to lock the channel to a rank higher than or equal to their own
        	//170 You can only lock the channel to ranks below your level.
        	return new ChannelResponse(ChannelResponseType.NOT_AUTHORISED_SPECIFIC);
        }
        if (channel.getLockRank() >= channel.getUserRank(user)) {
        	//Checks if there is an existing lock in place, which is set higher than the user's rank
        	//171 An existing lock which affects your rank is already in place. This lock must be removed before you can place a new lock.
        	return new ChannelResponse(ChannelResponseType.UNKNOWN_ERROR);
        }
        if (durationMins < 1) {
        	//1 minute lock minimum
            durationMins = 1;
        }
        if (durationMins > 60) {
        	//1 hour maximum lock
            durationMins = 60;
        }
        channel.setChannelLock(highestRank, durationMins*60_000);
        sendChannelGlobalMessage("This channel has been locked for all members with a rank of "
        		+channel.getGroupName(highestRank)+" and below.\nAnyone holding these ranks cannot rejoin the channel if they leave, until the lock is removed.", 173, channelId);
        
        //172 "A lock has been successfully placed on new members with the rank of "
		//+channel.getGroupName(highestRank)+" or below entering the channel for "+durationMins+" minutes."
    	Map<String, Serializable> args = new HashMap<>();
    	args.put("durationMins", durationMins);
    	args.put("highestRank", highestRank);
		return new ChannelResponse(ChannelResponseType.SUCCESS, args);
    }
    
    @Deprecated
    public JSONObject setWelcomeMessage (ChannelUser user, int channelID, String message, Color newColour) throws JSONException {
    	JSONObject response = new JSONObject();
        Channel channel = getChannel(channelID);
        if (channel == null) {
        	response.put("status", 404);
        	response.put("msgCode", 165);
        	response.put("message", "Cannot change channel details: channel not found.");
            return response;
        }
        if (!channel.userHasPermission(user, ChannelPermission.DETAILEDIT)) {
        	//Check if user has ability to change details (8 = change channel details)
        	response.put("status", 403);
        	response.put("msgCode", 125);
        	response.put("message", "You do not have the ability to change the details of this channel.");
            return response;
        }
        if (message.length() > 250) {
        	//Checks the length of the opening message
        	response.put("status", 403);
        	response.put("msgCode", 127);
        	response.put("message", "The opening message you have specified is too long.\nPlease use a shorter message.");
            return response;
        }
        //Add additional checks (such as removing/encoding special characters, filtering bad language, etc) here
        channel.setAttribute("welcomeMessage", message);//Changes the opening message in the temporarily loaded version of the channel, sets the colour to black (default)
        channel.flushRequired = true;
        //c.flushChannelDetails();//Applies the message change to the channel database
        MessagePayload messagePayload = messageFactory.createDetailsMessage(channel, userManager);
        for (ChannelUser u1 : channel.getUsers()) {
        	//Loops through all the people currently in the channel, sending the details change to all of them.
            u1.sendMessage(MessageType.CHANNEL_DETAIL_UPDATE, channelID, messagePayload);
        }
        
        //Populates the response as successful
        response.put("status", 200);//Opening message updated successfully
        response.put("msgCode", 129);
        response.put("message", "The opening message for this channel has been updated successfully.");
        return response;
    }
    
    public ChannelResponse addMember (ChannelUser user, int channelID, int userId) {
    	Channel channel = getChannel(channelID);
        if (channel == null) {
        	//If the channel was not found, send an error message
        	//158 The channel must be loaded before you can modify member data.\nTry joining the channel first.
        	return new ChannelResponse(ChannelResponseType.CHANNEL_NOT_LOADED);
        }
        if (!channel.userHasPermission(user, ChannelPermission.MEMBEREDIT)) {
        	//Check if user has ability to change ranks (6 = change ranks)
        	//130 You do not have the ability to grant ranks to people in this channel.
        	return new ChannelResponse(ChannelResponseType.NOT_AUTHORISED_GENERAL);
        }
        String targetName = userManager.getUsername(userId);
    	if (targetName == null) {
    		targetName = "[user not found]";
        }
    	Map<String, Serializable> args = new HashMap<>();
    	args.put("memberName", targetName);
    	
        if (channel.isUserBanned(userId)) {
        	//Cannot add a banned user as member

        	//132 rankedName+" has been permanently banned from the channel.\nPlease remove their name from the permanent ban list first."
        	return new ChannelResponse(ChannelResponseType.TARGET_BANNED, args);
        }
    	
        if (channel.isUserMember(userId)) {
        	//User is already a member of the channel.
        	//131 rankedName+" is already ranked in the channel.\nYou can change their rank using the channel settings interface."
        	return new ChannelResponse(ChannelResponseType.NO_CHANGE, args);
        }
        
        if (!channel.addMember(userId, ChannelGroup.DEFAULT_GROUP)) {        	
        	//Returns false if an error occurred in the rank changing process

        	//134 Could add "+rankedName+" due to a system error.
        	return new ChannelResponse(ChannelResponseType.UNKNOWN_ERROR, args);
        }
        //Notifies everyone in the channel of the rank list addition
        MessagePayload messagePayload = messageFactory.createRankListAddition(userId, channel, userManager);
        for (ChannelUser u1 : channel.getUsers()) {//Sends the updated rank to everyone in the channel
        	u1.sendMessage(MessageType.MEMBER_LIST_ADDITION, channelID, messagePayload);
        }
        if (userManager.getUser(userId) != null) {
        	ChannelUser newMember = userManager.getUser(userId);
        	if (channel.getUsers().contains(newMember)) {
        		messagePayload = messageFactory.createChannelUserUpdate(newMember, channel);//Updates the user's channel rank
        		for (ChannelUser u1 : channel.getUsers()) {
        			u1.sendMessage(MessageType.CHANNEL_LIST_UPDATE, channelID, messagePayload);
                }
        	}
        }
    	//133 rankedName+" has been successfully ranked in this channel."
        return new ChannelResponse(ChannelResponseType.SUCCESS, args);
    }
    
    public ChannelResponse updateMember (ChannelUser user, int channelId, int userId, int groupId) {
        Channel channel = getChannel(channelId);
        if (channel == null) {
        	//158 The channel must be loaded before you can modify rank data.\nTry joining the channel first.
        	return new ChannelResponse(ChannelResponseType.CHANNEL_NOT_LOADED);
        }       
        if (!channel.userHasPermission(user, ChannelPermission.MEMBEREDIT)) {
        	//Check if user has ability to change ranks (6 = change ranks)
        	
        	//141 You do not have the ability to change the ranks of other users in this channel.
        	return new ChannelResponse(ChannelResponseType.NOT_AUTHORISED_GENERAL);
        }
        ChannelGroup targetGroup = channel.getGroup(groupId);
        if (targetGroup == null) {
        	return new ChannelResponse(ChannelResponseType.INVALID_ARGUMENT);
        }
        String targetName = userManager.getUsername(userId);
        if (targetName == null) {
    		targetName = "[user not found]";
        }
    	Map<String, Serializable> args = new HashMap<>();
    	args.put("memberName", targetName);
    	args.put("targetGroup", messageFactory.createGroupDetails(targetGroup));
        ChannelGroup currentGroup = channel.getUserGroup(userId);
    	args.put("currentGroup", messageFactory.createGroupDetails(currentGroup));
        
        if (!channel.isUserMember(userId)) {//Checks if the user is on the member list
        	//140 You must add this user to the rank list before setting their rank.
        	return new ChannelResponse(ChannelResponseType.TARGET_INVALID_STATE, args);
        }
        if (!channel.canActionUser(user, userId)) {
        	//Checks if the user is able to edit the target user
        	
        	//143 You cannot alter the rank of someone with the same or higher rank than your own.
        	return new ChannelResponse(ChannelResponseType.NOT_AUTHORISED_SPECIFIC, args);
        }
        if (!channel.canAssignGroup(user, targetGroup)) {
        	//Checks if a rank level BETWEEN the user's current rank level and 0 is selected
        	//142 Invalid rank level specified.\nYou must choose a rank between your current level and 0
        	return new ChannelResponse(ChannelResponseType.INVALID_ARGUMENT);
        }
        if (!channel.setMemberGroup(userId, groupId)) {
        	//Returns false if an error occurred in the rank changing process
        	//145 Could not change rank due to a system error.
        	return new ChannelResponse(ChannelResponseType.UNKNOWN_ERROR, args);
        }
        MessagePayload messagePayload = messageFactory.createRankListUpdate(userId, channel, userManager);
        for (ChannelUser u1 : channel.getUsers()) {//Sends the rank update to everyone in the channel
        	u1.sendMessage(MessageType.MEMBER_LIST_UPDATE, channelId, messagePayload);
        }
        if (userManager.getUser(userId) != null) {//Checks if the user is online
        	ChannelUser newRank = userManager.getUser(userId);
        	if (channel.getUsers().contains(newRank)) {//Checks if the user is in the channel
        		messagePayload = messageFactory.createChannelUserUpdate(newRank, channel);//Updates the user's channel rank
        		for (ChannelUser u1 : channel.getUsers()) {
        			u1.sendMessage(MessageType.CHANNEL_LIST_UPDATE, channelId, messagePayload);
                }
        	}
        }

    	//144 The rank for "+targetName+" has been changed successfully.
        return new ChannelResponse(ChannelResponseType.SUCCESS, args);
    }
    
    public ChannelResponse removeMember (ChannelUser user, int channelID, int userId) {
    	Channel channel = getChannel(channelID);
        if (channel == null) {//If the channel was not found, send an error message
        	//158 The channel must be loaded before you can modify rank data.\nTry joining the channel first.
        	return new ChannelResponse(ChannelResponseType.CHANNEL_NOT_LOADED);
        }
        if (!channel.userHasPermission(user, ChannelPermission.MEMBEREDIT)) {
        	//Check if user has ability to change ranks (6 = change ranks)

        	//135 You do not have the ability to revoke ranks from people in this channel.
        	return new ChannelResponse(ChannelResponseType.NOT_AUTHORISED_GENERAL);
        }
        
        String targetName = userManager.getUsername(userId);
        if (targetName == null) {
    		targetName = "[user not found]";
        }
    	Map<String, Serializable> args = new HashMap<>();
    	args.put("memberName", targetName);
    	
        if (!channel.isUserMember(userId)) {
        	//Checks if the user is already off the rank list

        	//136 This user does not currently have a rank in the channel.
        	return new ChannelResponse(ChannelResponseType.NO_CHANGE, args);
        }
        if (!channel.canActionUser(user, userId)) {
        	//Checks if the user's current rank is higher than the user attempting to revoke the rank

        	//137 You cannot revoke the rank of someone with the same or higher rank than your own.
        	return new ChannelResponse(ChannelResponseType.NOT_AUTHORISED_SPECIFIC, args);
        }
        if (!channel.removeMember(userId)) {
        	//Returns false if an error occurred in the rank changing process

        	//138 Could not remove rank due to a system error.
        	return new ChannelResponse(ChannelResponseType.UNKNOWN_ERROR, args);
        }
        //Notifies everyone in the channel of the rank list addition
        MessagePayload messagePayload = messageFactory.createRankListRemoval(userId, channel);
        for (ChannelUser u1 : channel.getUsers()) {//Sends the rank removal to everyone in the channel
        	u1.sendMessage(MessageType.MEMBER_LIST_REMOVAL, channelID, messagePayload);
        }
        
        if (userManager.getUser(userId) != null) {//Checks if the user is online
        	ChannelUser targetMember = userManager.getUser(userId);
        	if (channel.getUsers().contains(targetMember)) {//Checks if the user is in the channel
        		messagePayload = messageFactory.createChannelUserUpdate(targetMember, channel);//Updates the user's channel rank
        		for (ChannelUser u1 : channel.getUsers()) {
        			u1.sendMessage(MessageType.CHANNEL_LIST_UPDATE, channelID, messagePayload);
                }
        	}
        }

    	//138 The rank for "+targetUsername+" has been revoked successfully.
        return new ChannelResponse(ChannelResponseType.SUCCESS, args);
    }
    
    public ChannelResponse addBan (ChannelUser user, int channelID, int userID) {
        Channel channel = getChannel(channelID);
        if (channel == null) {
        	//161 The channel must be loaded before you can modify ban data.\nTry joining the channel first.
            return new ChannelResponse(ChannelResponseType.CHANNEL_NOT_LOADED);
        }
    	
        if (!channel.userHasPermission(user, ChannelPermission.BANEDIT)) {
        	//Check if user has ability to permanently ban (4 = modify bans)
        	//146 You do not have the ability to permanently ban people from this channel.
        	return new ChannelResponse(ChannelResponseType.NOT_AUTHORISED_GENERAL);
        }
        
    	String bannedName = userManager.getUsername(userID);
    	if (bannedName == null) {
    		bannedName = "[user not found]";
        }
    	Map<String, Serializable> args = new HashMap<>();
    	args.put("bannedName", bannedName);
    	
        if (channel.isUserBanned(userID)) {
        	//Check if the user is already permanently banned from the channel

        	//147 bannedName+" is already on the permanent ban list for this channel."
        	return new ChannelResponse(ChannelResponseType.NO_CHANGE, args);
        }
        if (channel.isUserMember(userID)) {
        	//Checks if user is currently ranked (must revoke rank before they can be permanently banned)

        	//148 bannedName+" currently holds a rank in the channel.\nPlease remove their name from the rank list first."
        	return new ChannelResponse(ChannelResponseType.TARGET_INVALID_STATE, args);
        }
        if (!channel.addBan(userID)) {
        	//Returns false if an error occurred in the ban changing process
        	//150 Could not permanently ban this user due to a system error.
        	return new ChannelResponse(ChannelResponseType.UNKNOWN_ERROR, args);
        }        
        MessagePayload messagePayload = messageFactory.createBanListAddition(userID, channel, userManager);
        for (ChannelUser u1 : channel.getUsers()) {//Sends the ban list addition to everyone in the channel
        	u1.sendMessage(MessageType.BAN_LIST_ADDITION, channelID, messagePayload);
        }
        
    	//149 bannedName+" has been permanently banned from this channel.\nTheir ban will take effect when they leave the channel."
        return new ChannelResponse(ChannelResponseType.SUCCESS, args);
    }
    
    public ChannelResponse removeBan (ChannelUser user, int channelID, int userID) {
        Channel channel = getChannel(channelID);
        if (channel == null) {
        	//161 The channel must be loaded before you can modify ban data.\nTry joining the channel first.
        	return new ChannelResponse(ChannelResponseType.CHANNEL_NOT_LOADED);
        }
        if (!channel.userHasPermission(user, ChannelPermission.BANEDIT)) {
        	//Check if user has ability to remove users from the permanent ban list (4 = modify bans)
        	
        	//151 You do not have the ability to revoke permanent bans for people in this channel.
        	return new ChannelResponse(ChannelResponseType.NOT_AUTHORISED_GENERAL);
        }        
        
    	String bannedName = userManager.getUsername(userID);
    	if (bannedName == null) {
    		bannedName = "[user not found]";
        }
    	Map<String, Serializable> args = new HashMap<>();
    	args.put("bannedName", bannedName);
    	
        if (!channel.isUserBanned(userID)) {
        	//Checks if the specified user is already banned

        	//152 bannedName+" is not currently permanently banned from the channel."
        	return new ChannelResponse(ChannelResponseType.NO_CHANGE, args);
        }
        if (!channel.removeBan(userID)) {
        	//Returns false if an error occurred in the ban changing process

        	//154 Could unban this user due to a system error.
        	return new ChannelResponse(ChannelResponseType.UNKNOWN_ERROR, args);
        } 
        MessagePayload messagePayload = messageFactory.createBanListRemoval(userID, channel);
        for (ChannelUser u1 : channel.getUsers()) {//Sends the ban list removal to everyone in the channel
        	u1.sendMessage(MessageType.BAN_LIST_REMOVAL, channelID, messagePayload);
        }

    	//153 The permanent ban for "+bannedName+" has been removed successfully.
        return new ChannelResponse(ChannelResponseType.SUCCESS, args);
    }
}
