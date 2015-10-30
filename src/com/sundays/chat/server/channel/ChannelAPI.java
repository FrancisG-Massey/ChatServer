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

import org.json.JSONException;
import org.json.JSONObject;

import com.sundays.chat.server.Launcher;
import com.sundays.chat.server.Permission;
import com.sundays.chat.server.Settings;
import com.sundays.chat.server.message.MessagePayload;
import com.sundays.chat.server.message.MessageType;
import com.sundays.chat.server.user.User;
import com.sundays.chat.server.user.UserLookup;

public class ChannelAPI {
	
	private final ChannelManager channelManager;
	
	private final UserLookup userManager;
	
	private final ChannelMessageFactory messageFactory;
	
	public ChannelAPI (Launcher server) {
		this.channelManager = server.getChannelManager();
		this.userManager = server.getUserManager();
		this.messageFactory = ChannelMessageFactory.getInstance();
	}
    
    /*
     * Channel information requests
     */
    public MessagePayload getChannelDetails (int channelID) {
        Channel channel = channelManager.getChannel(channelID);
        if (channel == null) {
        	try {
				channelManager.loadChannel(channelID);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	channel = channelManager.getChannel(channelID);
        	if (channel == null) {
        		return null;
        	}
        }
        return messageFactory.createDetailsMessage(channel, userManager);
    }
    
    public MessagePayload getChannelList (int channelID) throws JSONException {
    	Channel channel = channelManager.getChannel(channelID);
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
    
    public MessagePayload getRankList (int channelID) {
        Channel channel = channelManager.getChannel(channelID);
        if (channel == null) {
        	try {
				channelManager.loadChannel(channelID);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	channel = channelManager.getChannel(channelID);
        	if (channel == null) {
        		return null;
        	}
        }
        return messageFactory.createMemberList(channel, userManager);
    }
    
    public MessagePayload getBanList (int channelID) {
        Channel channel = channelManager.getChannel(channelID);
        if (channel == null) {
        	try {
				channelManager.loadChannel(channelID);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	channel = channelManager.getChannel(channelID);
        	if (channel == null) {
        		return null;
        	}
        }
        return messageFactory.createBanList(channel, userManager);
    }
    
    public MessagePayload getChannelGroups (int channelID) {
    	Channel channel = channelManager.getChannel(channelID);
        if (channel == null) {
        	try {
				channelManager.loadChannel(channelID);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	channel = channelManager.getChannel(channelID);
        }
    	return messageFactory.createGroupList(channel);
    }
    
    /*
     * Basic functions (join, leave, send message)
     */
    public JSONObject joinChannel (User user, int cID) throws JSONException {
        Channel channel = channelManager.getChannel(cID);
        JSONObject response = new JSONObject();
        if (channel == null) {
            //Checks if the channel is currently loaded
            if (!channelManager.channelExists(cID)) {
                //Checks if the channel actually exists
            	response.put("status", 404);
            	response.put("msgCode", 100);
            	response.put("message", "The channel you have attempted to join does not exist.");
                return response;
            } else {
                //If the channel exists but is not loaded, load the channel
                try {
					channelManager.loadChannel(cID);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                channel = channelManager.getChannel(cID);
            }            
        }
        if (channel == user.getChannel()) {
        	response.put("status", 409);
        	response.put("msgCode", 166);
			response.put("message", "You are already in this channel.\nJoining it again will have no effect.");
			return response;
        }
        if (channel.unloadInitialised || channelManager.joinLock) {
            //Checks if the channel is currently being unloaded, or if a joinLock has been applied (we don't want people joining during this period, as it may stop the unload process from working)
        	response.put("status", 503);
        	response.put("msgCode", 101);
        	response.put("message", "You cannot join the channel at this time.\nPlease try again in a few minutes.");
            return response;
        }
        if (channel.isUserBanned(user.getUserID())) {
        	//User has been permanently banned from the channel
        	response.put("status", 403);
        	response.put("msgCode", 102);
        	response.put("message", "You are permanently banned from this channel.");
            if (channel.getUsers().isEmpty()) {                
                channelManager.queueChannelUnload(channel.getID());
            }
            return response;
        }
        if (!channel.userHasPermission(user, Permission.JOIN)) {
            //Check if user has permission to join (0 = join permission)
        	response.put("status", 403);
        	response.put("msgCode", 103);
        	response.put("message", "You do not have a high enough rank to join this channel.");
            /*if (channel.getUsers().isEmpty()) {                
                channelManager.cueChannelUnload(channel.channelID);
            }*/
            return response;
        }
        if (channel.getLockExpireTime() > System.currentTimeMillis() && channel.getUserRank(user) <= channel.getLockRank()) {
        	//Checks if the channel is locked to new users.
        	response.put("status", 403);
        	response.put("msgCode", 174);
        	response.put("message", "This channel has been locked for anyone holding the rank of "+channel.getRankNames().get(channel.getLockRank())+" or below.");
        	return response;
        }
        if (channel.getBanExpireTime(user.getUserID()) > System.currentTimeMillis()) {
            //Check if user is temporarily banned from the channel
            long timeRemaining = channel.getBanExpireTime(user.getUserID()) - System.currentTimeMillis();
            response.put("status", 403);
        	response.put("msgCode", 104);
        	response.put("message", "You are temporarily banned from the channel ("+(timeRemaining/(60*1000)+1)+" minute(s) remaining).");
            /*if (channel.getUsers().isEmpty()) {                
                channelManager.cueChannelUnload(channel.channelID);
            }*/
            return response;
        }
        channel.addUser(user);//Adds the user to the channel
        //Send a notification of the current user joining to all users currently in the channel
        MessagePayload userAdditionNotice = messageFactory.createChannelUserAddition(user, channel);
        for (User u1 : channel.getUsers()) {
        	u1.sendMessage(MessageType.CHANNEL_LIST_ADDITION, cID, userAdditionNotice);
        }
        user.setChannel(channel);//Sets the user's channel to the current one
        user.clearMessageQueue(cID);
        response.put("status", 200);
        response.put("rank", channel.getUserRank(user));
        response.put("details", messageFactory.createDetailsMessage(channel, userManager));
        channelManager.sendChannelLocalMessage(user, channel.getOpeningMessage(), 40, cID);//Sends the opening message to the user
        return response;
    }

    public JSONObject leaveChannel (User user) throws JSONException {
        Channel channel = user.getChannel();
        JSONObject response = new JSONObject();
        if (channel != null) {
        	channel.removeUser(user);
            if (channel.getUsers().isEmpty()) {                
                //channelManager.cueChannelUnload(c.channelID);//If the channel is empty, remove it from the server to save resources
            } else {
            	//Notify other users in the channel of this user's departure
            	MessagePayload departureNotice = messageFactory.createChannelUserRemoval(user, channel);
            	for (User u1 : channel.getUsers()) {
            		u1.sendMessage(MessageType.CHANNEL_LIST_REMOVAL, channel.getID(), departureNotice);
            	}
            }            
        }
        //Notifies the user of their removal
        user.setChannel(null);

        user.sendMessage(MessageType.CHANNEL_REMOVAL, channel.getID(), new MessagePayload());
        
        //Returns successful
        response.put("status", 200);
    	response.put("msgCode", 177);
        response.put("message", "You have left the channel.");
        return response;
    }
    
    public JSONObject sendMessage (User user, String message) throws JSONException {
    	JSONObject response = new JSONObject();
        Channel channel = user.getChannel();
        if (channel == null) {
        	response.put("status", 404);
        	response.put("msgCode", 105);
        	response.put("message", "Cannot send message: not currently in a channel.");
            return response;
        }
        if (!channel.userHasPermission(user, Permission.TALK)) {
        	//Checks if user has permission to talk in channel (1 = talk permission)
        	response.put("status", 403);
        	response.put("msgCode", 106);
        	response.put("message", "You do not have the appropriate permissions to send messages in this channel.");
            return response;
        }
        
        MessagePayload messagePayload = new MessagePayload();
        
        messagePayload.put("message", message);
        messagePayload.put("senderName", user.getUsername());
        messagePayload.put("senderGroup", channel.getUserGroup(user).getLegacyRank());
        messagePayload.put("senderID", user.getUserID()); 
        
        channel.addToMessageCache(messagePayload);
        
        for (User u1 : channel.getUsers()) {
        	//Loops through all the people currently in the channel, sending the message to each of them.
            u1.sendMessage(MessageType.CHANNEL_STANDARD, channel.getID(), messagePayload);
        }
        response.put("status", 200);
        return response;
    }
    
    /*
     * Moderator functions
     */
    public JSONObject resetChannel (final User u, final int channelID) throws JSONException {
    	JSONObject response = new JSONObject();
        Channel c = channelManager.getChannel(channelID);
        if (c == null) {
        	//The channel is not currently loaded
        	response.put("status", 404);
        	response.put("msgCode", 107);
        	response.put("message", "The channel you have attempted to reset is not loaded.\nResetting will have no effect.");
            return response;
        }
        if (!c.userHasPermission(u, Permission.RESET)) {
        	//Check if user has ability to reset the channel (5 = reset permission)
        	response.put("status", 403);
        	response.put("msgCode", 108);
        	response.put("message", "You do not have the appropriate permissions reset this channel.");
            return response;
        }
        channelManager.sendChannelGlobalMessage("This channel will be reset within the next "+Settings.channelCleanupThreadFrequency+" seconds.\n"
        		+ "All members will be removed.\nYou may join again after the reset.", 109, c.getID(), Color.BLUE);
        channelManager.queueChannelUnload(channelID);
        c.resetLaunched = true;
        //Unloads the channel, then loads it again
        
        //Returns successful
        response.put("status", 202);
    	response.put("msgCode", 110);
    	response.put("message", "Your request to reset this channel has been accepted.");
        return response;
    }
    
    public JSONObject kickUser (User u, int cID, int kUID) throws JSONException {
    	JSONObject response = new JSONObject();
        Channel c = channelManager.getChannel(cID);
        if (c == null) {
        	//The channel is not currently loaded
        	response.put("status", 404);
        	response.put("msgCode", 111);
        	response.put("message", "The channel you have attempted to kick this user from is not loaded.");
            return response;
        }
        if (!c.userHasPermission(u, Permission.KICK)) {
        	//Check if user has ability to kick users (2 = kick permission)
        	response.put("status", 403);
        	response.put("msgCode", 112);
        	response.put("message", "You do not have the appropriate permissions to kick people from this channel.");
            return response;
        }
        User kickedUser = userManager.getUser(kUID);//Retrieves the user object of the user being kicked
        if (kickedUser == null || !c.getUsers().contains(kickedUser)) {
        	//Checks if the user is currently logged in and is in the channel
        	response.put("status", 403);
        	response.put("msgCode", 113);
        	response.put("message", "This user is not currently in the channel.");
            return response;
        }
        response.put("kickedUser", kickedUser.getUsername());
        if (c.getUserRank(kickedUser) >= c.getUserRank(u)) {
        	//Checks if the user kicking holds a lower or equal rank than the user being kicked
        	response.put("status", 403);
        	response.put("msgCode", 114);
        	response.put("message", "You can only kick users with a lower rank level than yours.");
            return response;
        }
        leaveChannel(kickedUser);
        channelManager.sendChannelLocalMessage(kickedUser, "You have been kicked from the channel.", 115, cID, Color.RED);
        c.setTempBan(kUID, 60_000);//Sets a 60 second temporary ban (gives the user performing the kick a chance to choose whether or not to temporarily ban)
        
        response.put("status", 200);
    	response.put("msgCode", 116);
    	response.put("message", "Your attempt to kick "+kickedUser.getUsername()+" from this channel was successful.");
		return response;
    }
    
    public JSONObject tempBanUser (User u, int cID, int bannedUser, int durationMins) throws JSONException {
    	JSONObject response = new JSONObject();
        Channel c = channelManager.getChannel(cID);
        if (c == null) {
        	//The channel is not currently loaded
        	response.put("status", 404);
        	response.put("msgCode", 117);
        	response.put("message", "The channel you have attempted to temporarily ban this user from is not loaded.");
            return response;
        }
    	String bannedName = userManager.getUsername(bannedUser);
    	if (bannedName == null) {
    		bannedName = "[user not found]";
        }
    	response.put("bannedName", bannedName);
        if (!c.userHasPermission(u, Permission.TEMPBAN)) {
        	//Check if user has ability to temp-ban users (3 = temp-ban permission)
        	response.put("status", 403);
        	response.put("msgCode", 118);
        	response.put("message", "You cannot temporarily ban people from this channel.");
            return response;
        }
        if (c.getUserRank(bannedUser) >= c.getUserRank(u)) {
        	//Checks if the user banning holds a lower or equal rank than the user being banned
        	response.put("status", 403);
        	response.put("msgCode", 119);
        	response.put("message", "You can only temporarily ban users with a lower rank level than yours.");
            return response;
        }
        if (durationMins < 1) {
        	//1 minute ban minimum
            durationMins = 1;
        }
        if (durationMins > 60*6) {
        	//6 hour maximum ban
            durationMins = 60*6;
        }
        c.setTempBan(bannedUser, durationMins*60_000);//Sets a temporary ban as specified by durationMins
    	response.put("status", 200);
    	response.put("msgCode", 120);
    	response.put("message", "You have successfully temporarily banned "+bannedName+" from the channel for "+durationMins+" minutes.\n"
    			+"Their ban will be lifted after the specified time, or if the channel is reset.");
		return response;
    }
    
    public JSONObject lockChannel (User u, int cID, int highestRank, int durationMins) throws JSONException {
    	JSONObject response = new JSONObject();
    	Channel c = channelManager.getChannel(cID);
    	if (c == null) {
        	//The channel is not currently loaded
        	response.put("status", 404);
        	response.put("msgCode", 168);
        	response.put("message", "The channel you have attempted to lock is not currently loaded on this server.");
            return response;
        }
    	if (!c.userHasPermission(u, Permission.LOCKCHANNEL)) {
        	//Check if user has ability to lock the channel (9 = lock permission)
        	response.put("status", 403);
        	response.put("msgCode", 169);
        	response.put("message", "You cannot lock this channel.");
            return response;
        }
        if (highestRank >= c.getUserRank(u)) {
        	//Checks if the user is trying to lock the channel to a rank higher than or equal to their own
        	response.put("status", 403);
        	response.put("msgCode", 170);
        	response.put("message", "You can only lock the channel to ranks below your level.");
            return response;
        }
        if (c.getLockRank() >= c.getUserRank(u)) {
        	//Checks if there is an existing lock in place, which is set higher than the user's rank
        	response.put("status", 403);
        	response.put("msgCode", 171);
        	response.put("message", "An existing lock which affects your rank is already in place. This lock must be removed before you can place a new lock.");
            return response;
        }
        if (durationMins < 1) {
        	//1 minute lock minimum
            durationMins = 1;
        }
        if (durationMins > 60) {
        	//1 hour maximum lock
            durationMins = 60;
        }
        c.setChannelLock(highestRank, durationMins*60_000);
        channelManager.sendChannelGlobalMessage("This channel has been locked for all members with a rank of "
        +c.getRankNames().get(highestRank)+" and below.\nAnyone holding these ranks cannot rejoin the channel if they leave, until the lock is removed.", 173, cID);
        
        response.put("status", 200);
    	response.put("msgCode", 172);
    	response.put("highestRank", "{\"name\":\""+c.getRankNames().get(highestRank)+"\",\"id\":"+highestRank+"}");
    	response.put("duration", durationMins);
    	response.put("message", "A lock has been successfully placed on new members with the rank of "
    	+c.getRankNames().get(highestRank)+" or below entering the channel for "+durationMins+" minutes.");
		return response;
    }
    
    public JSONObject chageOpeningMessage (User user, int channelID, String message, Color newColour) throws JSONException {
    	JSONObject response = new JSONObject();
        Channel channel = channelManager.getChannel(channelID);
        if (channel == null) {
        	response.put("status", 404);
        	response.put("msgCode", 165);
        	response.put("message", "Cannot change channel details: channel not found.");
            return response;
        }
        if (!channel.userHasPermission(user, Permission.DETAILCHANGE)) {
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
        channel.setOpeningMessage(message, Color.BLACK);//Changes the opening message in the temporarily loaded version of the channel, sets the colour to black (default)
        channel.flushRequired = true;
        //c.flushChannelDetails();//Applies the message change to the channel database
        MessagePayload messagePayload = messageFactory.createDetailsMessage(channel, userManager);
        for (User u1 : channel.getUsers()) {
        	//Loops through all the people currently in the channel, sending the details change to all of them.
            u1.sendMessage(MessageType.CHANNEL_DETAIL_UPDATE, channelID, messagePayload);
        }
        
        //Populates the response as successful
        response.put("status", 200);//Opening message updated successfully
        response.put("msgCode", 129);
        response.put("message", "The opening message for this channel has been updated successfully.");
        return response;
    }
    
    public JSONObject addRank (User user, int channelID, int uID) throws JSONException {
    	JSONObject response = new JSONObject();
        Channel channel = channelManager.getChannel(channelID);
        if (channel == null) {
        	//If the channel was not found, send an error message
        	response.put("status", 404);
        	response.put("msgCode", 158);
        	response.put("message", "The channel must be loaded before you can modify rank data.\nTry joining the channel first.");
            return response;
        }
    	String rankedName = userManager.getUsername(uID);
    	if (rankedName == null) {
    		rankedName = "[user not found]";
        }
    	response.put("rankedName", rankedName);
        if (!channel.userHasPermission(user, Permission.RANKCHANGE)) {
        	//Check if user has ability to change ranks (6 = change ranks)
        	response.put("status", 403);
        	response.put("msgCode", 130);
        	response.put("message", "You do not have the ability to grant ranks to people in this channel.");
            return response;
        }
        if (channel.getUserRank(uID) > 0) {
        	//User already holds a rank.
        	response.put("status", 400);
        	response.put("msgCode", 131);
        	response.put("message", rankedName+" is already ranked in the channel.\nYou can change their rank using the channel settings interface.");
            return response;
        }
        if (channel.isUserBanned(uID)) {
        	//Cannot rank a banned user
        	response.put("status", 400);
        	response.put("msgCode", 132);        	
        	response.put("message", rankedName+" has been permanently banned from the channel.\nPlease remove their name from the permanent ban list first.");
            return response;
        }
        
        if (!channel.addRank(uID)) {        	
        	//Returns false if an error occurred in the rank changing process
        	response.put("status", 500);
        	response.put("msgCode", 134); 
        	response.put("message", "Could rank "+rankedName+" due to a system error.");
        	return response;
        }
        //Notifies everyone in the channel of the rank list addition
        MessagePayload messagePayload = messageFactory.createRankListAddition(uID, channel, userManager);
        for (User u1 : channel.getUsers()) {//Sends the updated rank to everyone in the channel
        	u1.sendMessage(MessageType.RANK_LIST_ADDITION, channelID, messagePayload);
        }
        if (userManager.getUser(uID) != null) {
        	User newRank = userManager.getUser(uID);
        	if (channel.getUsers().contains(newRank)) {
        		messagePayload = messageFactory.createChannelUserUpdate(newRank, channel);//Updates the user's channel rank
        		for (User u1 : channel.getUsers()) {
        			u1.sendMessage(MessageType.CHANNEL_LIST_UPDATE, channelID, messagePayload);
                }
        		//Sends a packet to the user, informing them of the rank change
        		messagePayload = new MessagePayload();        		
        		messagePayload.put("userID", newRank.getUserID());
        		messagePayload.put("rank", channel.getUserGroup(newRank).getLegacyRank());
        		messagePayload.put("notice", "This message type is deprecated and will be removed in future versions. Clients should use channel list updates to identify changes to their own rank.");
        		
        		newRank.sendMessage(MessageType.RANK_UPDATE, channelID, messagePayload);
        	}
        }
        response.put("status", 200);
    	response.put("msgCode", 133); 
    	response.put("message", rankedName+" has been successfully ranked in this channel.");
		return response;
    }
    
    public JSONObject removeRank (User u, int channelID, int uID) throws JSONException {
    	JSONObject response = new JSONObject();
        Channel channel = channelManager.getChannel(channelID);
        String targetUsername = userManager.getUsername(uID);
        if (channel == null) {
        	//If the channel was not found, send an error message
        	response.put("status", 404);
        	response.put("msgCode", 158);
        	response.put("message", "The channel must be loaded before you can modify rank data.\nTry joining the channel first.");
            return response;
        }
        if (!channel.userHasPermission(u, Permission.RANKCHANGE)) {
        	//Check if user has ability to change ranks (6 = change ranks)
        	response.put("status", 403);
        	response.put("msgCode", 135);
        	response.put("message", "You do not have the ability to revoke ranks from people in this channel.");
            return response;
        }
        if (channel.getUserRank(uID) <= 0) {
        	//Checks if the user is already off the rank list
        	response.put("status", 400);
        	response.put("msgCode", 136);
        	response.put("message", "This user does not currently have a rank in the channel.");
            return response;
        }
        if (channel.getUserRank(uID) >= channel.getUserRank(u)) {
        	//Checks if the user's current rank is higher than the user attempting to revoke the rank
        	response.put("status", 403);
        	response.put("msgCode", 137);
        	response.put("message", "You cannot revoke the rank of someone with the same or higher rank than your own.");
            return response;
        }
        if (!channel.removeRank(uID)) {
        	//Returns false if an error occurred in the rank changing process
        	response.put("status", 500);
        	response.put("msgCode", 138); 
        	response.put("message", "Could not remove rank due to a system error.");
        	return response;
        }
        //Notifies everyone in the channel of the rank list addition
        MessagePayload messagePayload = messageFactory.createRankListRemoval(uID, channel);
        for (User u1 : channel.getUsers()) {//Sends the rank removal to everyone in the channel
        	u1.sendMessage(MessageType.RANK_LIST_REMOVAL, channelID, messagePayload);
        }
        
        if (userManager.getUser(uID) != null) {//Checks if the user is online
        	User newRank = userManager.getUser(uID);
        	if (channel.getUsers().contains(newRank)) {//Checks if the user is in the channel
        		messagePayload = messageFactory.createChannelUserUpdate(newRank, channel);//Updates the user's channel rank
        		for (User u1 : channel.getUsers()) {
        			u1.sendMessage(MessageType.CHANNEL_LIST_UPDATE, channelID, messagePayload);
                }
        		//Sends a packet to the user, informing them of the rank change
        		messagePayload = new MessagePayload();        		
        		messagePayload.put("userID", newRank.getUserID());
        		messagePayload.put("rank", channel.getUserGroup(newRank).getLegacyRank());
        		messagePayload.put("notice", "This message type is deprecated and will be removed in future versions. Clients should use channel list updates to identify changes to their own rank.");
        		
        		newRank.sendMessage(MessageType.RANK_UPDATE, channelID, messagePayload);
        	}
        }
        response.put("status", 200);
    	response.put("msgCode", 138);
    	response.put("message", "The rank for "+targetUsername+" has been revoked successfully.");
		return response;
    }
    
    public JSONObject updateRank (User u, int channelID, int uID, byte rank) throws JSONException {
    	JSONObject response = new JSONObject();
        Channel channel = channelManager.getChannel(channelID);
        String targetUsername = userManager.getUsername(uID);
        if (channel == null) {
        	response.put("status", 404);
        	response.put("msgCode", 158);
        	response.put("message", "The channel must be loaded before you can modify rank data.\nTry joining the channel first.");
            return response;
        }
        if (!channel.userHasPermission(u, Permission.RANKCHANGE)) {
        	//Check if user has ability to change ranks (6 = change ranks)
        	response.put("status", 403);
        	response.put("msgCode", 141);
        	response.put("message", "You do not have the ability to change the ranks of other users in this channel.");
            return response;
        }
        int currentRank = channel.getUserRank(uID);
        if (channel.getUserRank(uID) <= 0) {
        	//Checks if the user is on the rank list
        	response.put("status", 400);
        	response.put("msgCode", 140);
        	response.put("message", "You must add this user to the rank list before setting their rank.");
            return response;
        }
        if (rank >= channel.getUserRank(u) || rank < 1) {
        	//Checks if a rank level BETWEEN the user's current rank level and 0 is selected
        	response.put("status", 400);
        	response.put("msgCode", 142);
        	response.put("message", "Invalid rank level specified.\nYou must choose a rank between your current level and 0");
            return response;
        }
        if (currentRank >= channel.getUserRank(u)) {
        	//Checks if the user's current rank is higher than the user attempting to change the rank
        	response.put("status", 403);
        	response.put("msgCode", 143);
        	response.put("message", "You cannot alter the rank of someone with the same or higher rank than your own.");
            return response;
        }
        if (!channel.setRank(uID, rank)) {
        	//Returns false if an error occurred in the rank changing process
        	response.put("status", 500);
        	response.put("msgCode", 145); 
        	response.put("message", "Could not change rank due to a system error.");
        	return response;
        }
        MessagePayload messagePayload = messageFactory.createRankListUpdate(uID, channel, userManager);
        for (User u1 : channel.getUsers()) {//Sends the rank update to everyone in the channel
        	u1.sendMessage(MessageType.RANK_LIST_UPDATE, channelID, messagePayload);
        }
        if (userManager.getUser(uID) != null) {//Checks if the user is online
        	User newRank = userManager.getUser(uID);
        	if (channel.getUsers().contains(newRank)) {//Checks if the user is in the channel
        		messagePayload = messageFactory.createChannelUserUpdate(newRank, channel);//Updates the user's channel rank
        		for (User u1 : channel.getUsers()) {
        			u1.sendMessage(MessageType.CHANNEL_LIST_UPDATE, channelID, messagePayload);
                }
        		//Sends a packet to the user, informing them of the rank change
        		messagePayload = new MessagePayload();        		
        		messagePayload.put("userID", newRank.getUserID());
        		messagePayload.put("rank", channel.getUserGroup(newRank).getLegacyRank());
        		messagePayload.put("notice", "This message type is deprecated and will be removed in future versions. Clients should use channel list updates to identify changes to their own rank.");
        		
        		newRank.sendMessage(MessageType.RANK_UPDATE, channelID, messagePayload);
        	}
        }
        response.put("status", 200);
    	response.put("msgCode", 144);
    	response.put("message", "The rank for "+targetUsername+" has been changed successfully.");
        return response;
    }
    
    public JSONObject addBan (User user, int channelID, int userID) throws JSONException {
    	JSONObject response = new JSONObject();
        Channel channel = channelManager.getChannel(channelID);
        if (channel == null) {
        	response.put("status", 404);
        	response.put("msgCode", 161);
        	response.put("message", "The channel must be loaded before you can modify ban data.\nTry joining the channel first.");
            return response;
        }
    	String bannedName = userManager.getUsername(userID);
    	if (bannedName == null) {
    		bannedName = "[user not found]";
        }
    	response.put("bannedName", bannedName);
        if (!channel.userHasPermission(user, Permission.PERMBAN)) {
        	//Check if user has ability to permanently ban (4 = modify bans)
        	response.put("status", 403);
        	response.put("msgCode", 146);        	
        	response.put("message", "You do not have the ability to permanently ban people from this channel.");
            return response;
        }
        if (channel.isUserBanned(userID)) {
        	//Check if the user is already permanently banned from the channel
        	response.put("status", 400);
        	response.put("msgCode", 147);
        	response.put("message", bannedName+" is already on the permanent ban list for this channel.");
            return response;
        }
        if (channel.getUserRank(userID) > 0) {
        	//Checks if user is currently ranked (must revoke rank before they can be permanently banned)
        	response.put("status", 400);
        	response.put("msgCode", 148);
        	response.put("message", bannedName+" currently holds a rank in the channel.\nPlease remove their name from the rank list first.");
            return response;
        }
        if (!channel.addBan(userID)) {
        	//Returns false if an error occurred in the ban changing process
        	response.put("status", 500);
        	response.put("msgCode", 150); 
        	response.put("message", "Could permanently ban this user due to a system error.");
        	return response;
        }        
        MessagePayload messagePayload = messageFactory.createBanListAddition(userID, channel, userManager);
        for (User u1 : channel.getUsers()) {//Sends the ban list addition to everyone in the channel
        	u1.sendMessage(MessageType.BAN_LIST_ADDITION, channelID, messagePayload);
        }
        response.put("status", 200);
    	response.put("msgCode", 149);
    	response.put("message", bannedName+" has been permanently banned from this channel.\nTheir ban will take effect when they leave the channel.");
		return response;
    }
    
    public JSONObject removeBan (User user, int channelID, int userID) throws JSONException {
    	JSONObject response = new JSONObject();
        Channel channel = channelManager.getChannel(channelID);
        if (channel == null) {
        	response.put("status", 404);
        	response.put("msgCode", 161);
        	response.put("message", "The channel must be loaded before you can modify ban data.\nTry joining the channel first.");
            return response;
        }
    	String bannedName = userManager.getUsername(userID);
    	if (bannedName == null) {
    		bannedName = "[user not found]";
        }
    	response.put("bannedName", bannedName);
        if (!channel.userHasPermission(user, Permission.PERMBAN)) {
        	//Check if user has ability to remove users from the permanent ban list (4 = modify bans)
        	response.put("status", 403);
        	response.put("msgCode", 151); 
        	response.put("message", "You do not have the ability to revoke permanent bans for people in this channel.");
            return response;
        }
        if (!channel.isUserBanned(userID)) {
        	//Checks if the specified user is already banned
        	response.put("status", 400);
        	response.put("msgCode", 152);
        	response.put("message", bannedName+" is not currently permanently banned from the channel.");
            return response;
        }
        if (!channel.removeBan(userID)) {
        	//Returns false if an error occurred in the ban changing process
            response.put("status", 200);
        	response.put("msgCode", 154);
        	response.put("message", "Could unban this user due to a system error.");
        	return response;
        } 
        MessagePayload messagePayload = messageFactory.createBanListRemoval(userID, channel);
        for (User u1 : channel.getUsers()) {//Sends the ban list removal to everyone in the channel
        	u1.sendMessage(MessageType.BAN_LIST_REMOVAL, channelID, messagePayload);
        }
        response.put("status", 200);
    	response.put("msgCode", 153);
    	response.put("message", "The permanent ban for "+bannedName+" has been removed successfully.");
        return response;
    }

}
