/*******************************************************************************
 * Copyright (c) 2015 Francis G.
 *
 * This file is part of ChatServer.
 *
 * ChatServer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.sundays.chat.server.ChatServer;
import com.sundays.chat.server.Permission;
import com.sundays.chat.server.Settings;
import com.sundays.chat.server.Settings.Message;
import com.sundays.chat.server.User;

public class ChannelAPI {
	
	private final ChannelManager channelManager;
	
	public ChannelAPI (ChannelManager cm) {
		this.channelManager = cm;
	}
    
    /*
     * Channel information requests
     */
    public JSONObject getChannelDetails (int cID) {
        Channel channel = channelManager.getChannel(cID);
        JSONObject channelData = new JSONObject();
        if (channel == null) {
        	channelManager.loadChannel(cID);
        	channel = channelManager.getChannel(cID);
        }
        channelData = ChannelDataPreparer.prepareChannelDetails(channel);
        return channelData;
    }
    
    public JSONObject getChannelList (int cID) throws JSONException {
    	Channel channel = channelManager.getChannel(cID);
        JSONObject channelList = new JSONObject();
        if (channel == null) {
        	channelList.put("id", cID);
			channelList.put("totalUsers", 0);	
        } else {
        	channelList = ChannelDataPreparer.prepareChannelList(channel);
        }
        return channelList;
    }
    
    public JSONObject getRankNames (int cID) throws JSONException {
        Channel channel = channelManager.getChannel(cID);
        JSONObject rankNames = new JSONObject();
        if (channel == null) {
        	channelManager.loadChannel(cID);
        	channel = channelManager.getChannel(cID);
        }
        rankNames = ChannelDataPreparer.prepareRankNames(channel);
        return rankNames;
    }
    
    public JSONObject getPermissions (int cID) {
    	Channel channel = channelManager.getChannel(cID);
    	if (channel == null) {
    		channelManager.loadChannel(cID);
        	channel = channelManager.getChannel(cID);
        }
		return ChannelDataPreparer.prepareChannelPermissions(channel);
    }
    
    public JSONObject getRankList (int cID) {
        Channel channel = channelManager.getChannel(cID);
        if (channel == null) {
        	channelManager.loadChannel(cID);
        	channel = channelManager.getChannel(cID);
        }
        return ChannelDataPreparer.prepareRankList(channel);
    }
    
    public JSONObject getBanList (int cID) {
        Channel c = channelManager.getChannel(cID);
        if (c == null) {
        	channelManager.loadChannel(cID);
        	c = channelManager.getChannel(cID);
        }
        return ChannelDataPreparer.prepareBanList(c);
    }
    
    public JSONObject getChannelGroups (int cID) {
    	Channel c = channelManager.getChannel(cID);
        if (c == null) {
        	channelManager.loadChannel(cID);
        	c = channelManager.getChannel(cID);
        }
    	return ChannelDataPreparer.prepareGroupList(c);
    }
    
    /*
     * Basic functions (join, leave, send message)
     */
    public JSONObject joinChannel (User u, int cID) throws JSONException {
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
                channelManager.loadChannel(cID);
                channel = channelManager.getChannel(cID);
            }            
        }
        if (channel == u.getChannel()) {
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
        if (channel.isUserBanned(u.getUserID())) {
        	//User has been permanently banned from the channel
        	response.put("status", 403);
        	response.put("msgCode", 102);
        	response.put("message", "You are permanently banned from this channel.");
            if (channel.getUsers().isEmpty()) {                
                channelManager.cueChannelUnload(channel.channelID);
            }
            return response;
        }
        if (!channel.userHasPermissionOld(u, Permission.JOIN)) {
            //Check if user has permission to join (0 = join permission)
        	response.put("status", 403);
        	response.put("msgCode", 103);
        	response.put("message", "You do not have a high enough rank to join this channel.");
            /*if (channel.getUsers().isEmpty()) {                
                channelManager.cueChannelUnload(channel.channelID);
            }*/
            return response;
        }
        if (channel.getLockExpireDate() != null && channel.getLockExpireDate().after(new Date()) && channel.getUserRank(u) <= channel.getLockRank()) {
        	//Checks if the channel is locked to new users.
        	response.put("status", 403);
        	response.put("msgCode", 174);
        	response.put("message", "This channel has been locked for anyone holding the rank of "+channel.getRankNames().get(channel.getLockRank())+" or below.");
        	return response;
        }
        if (channel.getBanExpireDate(u.getUserID()) != null && channel.getBanExpireDate(u.getUserID()).after(new Date())) {
            //Check if user is temporarily banned from the channel
            long timeRemaining = channel.getBanExpireDate(u.getUserID()).getTime() - new Date().getTime();
            response.put("status", 403);
        	response.put("msgCode", 104);
        	response.put("message", "You are temporarily banned from the channel ("+(timeRemaining/(60*1000)+1)+" minute(s) remaining).");
            /*if (channel.getUsers().isEmpty()) {                
                channelManager.cueChannelUnload(channel.channelID);
            }*/
            return response;
        }
        channel.addUser(u);//Adds the user to the channel
        //Send a notification of the current user joining to all users currently in the channel
        JSONObject userAdditionNotice = ChannelDataPreparer.sendUserInChannel(u, channel);
        for (User u1 : channel.getUsers()) {
        	u1.addQueuedMessage(cID, userAdditionNotice);
        }
        u.setChannel(channel);//Sets the user's channel to the current one
        u.clearMessageQueue(cID);
        response.put("status", 200);
        response.put("rank", channel.getUserRank(u));
        response.put("details", ChannelDataPreparer.prepareChannelDetails(channel));
        channelManager.sendChannelLocalMessage(u, channel.getOpeningMessage(), 40, cID);//Sends the opening message to the user
        return response;
    }

    public JSONObject leaveChannel (User u) throws JSONException {
        Channel c = u.getChannel();
        JSONObject response = new JSONObject();
        if (c != null) {
            c.removeUser(u);
            if (c.getUsers().isEmpty()) {                
                //channelManager.cueChannelUnload(c.channelID);//If the channel is empty, remove it from the server to save resources
            } else {
            	//Notify other users in the channel of this user's departure
            	JSONObject departureNotice = ChannelDataPreparer.removeUserFromChannel(u, c);
            	for (User u1 : c.getUsers()) {
            		u1.addQueuedMessage(c.channelID, departureNotice);
            	}
            }            
        }
        //Notifies the user of their removal
        u.setChannel(null);
        JSONObject messageObject = new JSONObject();
    	messageObject.put("id", c.getNextMessageID());
        messageObject.put("type", Message.CHANNEL_REMOVAL);
        u.addQueuedMessage(c.channelID, messageObject);
        
        //Returns successful
        response.put("status", 200);
    	response.put("msgCode", 177);
        response.put("message", "You have left the channel.");
        return response;
    }
    
    public JSONObject sendMessage (User u, String message) throws JSONException {
    	JSONObject response = new JSONObject();
        Channel c = u.getChannel();
        if (c == null) {
        	response.put("status", 404);
        	response.put("msgCode", 105);
        	response.put("message", "Cannot send message: not currently in a channel.");
            return response;
        }
        if (!c.userHasPermissionOld(u, Permission.TALK)) {
        	//Checks if user has permission to talk in channel (1 = talk permission)
        	response.put("status", 403);
        	response.put("msgCode", 106);
        	response.put("message", "You do not have the appropriate permissions to send messages in this channel.");
            return response;
        }
        JSONObject messageObject = new JSONObject();
        messageObject.put("id", c.getNextMessageID());
        messageObject.put("type", Message.CHANNEL_STANDARD);//Type 5 = normal channel message
        messageObject.put("message", message);
        messageObject.put("senderName", u.getUsername());
        messageObject.put("senderRank", c.getUserRank(u));
        messageObject.put("senderID", u.getUserID()); 
        c.addToMessageCache(messageObject);
        for (User u1 : c.getUsers()) {
        	//Loops through all the people currently in the channel, sending the message to each of them.
            u1.addQueuedMessage(c.channelID, messageObject);
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
        if (!c.userHasPermissionOld(u, Permission.RESET)) {
        	//Check if user has ability to reset the channel (5 = reset permission)
        	response.put("status", 403);
        	response.put("msgCode", 108);
        	response.put("message", "You do not have the appropriate permissions reset this channel.");
            return response;
        }
        channelManager.sendChannelGlobalMessage("This channel will be reset within the next "+Settings.channelCleanupThreadFrequency+" seconds.\n"
        		+ "All members will be removed.\nYou may join again after the reset.", 109, c.channelID, Color.BLUE);
        channelManager.cueChannelUnload(channelID);
        c.resetLaunched = true;
        //Unloads the channel, then loads it again
        /*new Timer().schedule(new TimerTask () {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					//.unloadChannel(channelID);
					//channelManager.loadChannel(channelID);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}		        
			}        	
        }, 10000);//Cue the reset to occur in another 10 seconds*/
        
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
        if (!c.userHasPermissionOld(u, Permission.KICK)) {
        	//Check if user has ability to kick users (2 = kick permission)
        	response.put("status", 403);
        	response.put("msgCode", 112);
        	response.put("message", "You do not have the appropriate permissions to kick people from this channel.");
            return response;
        }
        User kickedUser = ChatServer.getInstance().userManager().getUser(kUID);//Retrieves the user object of the user being kicked
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
        c.setTempBan(kUID, 60*1000);//Sets a 60 second temporary ban (gives the user performing the kick a chance to choose whether or not to temporarily ban)
        
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
    	String bannedName = ChatServer.getInstance().userManager().getUsername(bannedUser);
    	if (bannedName == null) {
    		bannedName = "[user not found]";
        }
    	response.put("bannedName", bannedName);
        if (!c.userHasPermissionOld(u, Permission.TEMPBAN)) {
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
        c.setTempBan(bannedUser, durationMins*60*1000);//Sets a temporary ban as specified by durationMins
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
    	if (!c.userHasPermissionOld(u, Permission.LOCKCHANNEL)) {
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
        c.setChannelLock(highestRank, durationMins);
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
    
    /*
     * Channel permanent data updates (Administrative functions)
     */
    public JSONObject updatePermission (User u, int cID, Permission p, int newV) throws JSONException {
    	JSONObject response = new JSONObject();
        Channel channel = channelManager.getChannel(cID);        
        if (channel == null) {
        	//If the channel was not found, send an error message
        	response.put("status", 404);
        	response.put("msgCode", 156);
        	response.put("message", "Cannot change permissions: channel not found.");
            return response;
        }
        if (!channel.userHasPermissionOld(u, Permission.PERMISSIONCHANGE)) {
        	//Check if user has ability to change permissions (7 = change permissions)
        	response.put("status", 403);
        	response.put("msgCode", 122);
        	response.put("message", "You do not have the ability to change permissions in this channel.");
            return response;
        }
        if (newV > channel.getUserRank(u)) {
        	//If the permission is set higher than the users rank, return an error
        	response.put("status", 403);
        	response.put("msgCode", 157);
        	response.put("message", "You cannot set permissions to a higher rank than your own.");
            return response;
        }
        if (newV > Byte.MAX_VALUE || newV < Byte.MIN_VALUE) {
        	//If the value submitted is not a valid byte value, stop here.
        	response.put("status", 400);
        	response.put("msgCode", 123);
        	response.put("message", "An invalid new permission for this channel has been sent.");
            return response;
        }
        if (!channel.setPermission(p, (byte) newV)) {
        	//Attempts to change the permission, will return false if the permission was invalid (too large, too small, non-existent)
        	response.put("status", 400);
        	response.put("msgCode", 123);
        	response.put("message", "An invalid new permission for this channel has been sent.");
            return response;
        }
        switch (p) {
            case JOIN://Join permission
                for (User u1 : channel.getUsers()) {
                    if (channel.getUserRank(u1) < newV) {//Removes any users which no longer have the ability to be in the channel
                    	channelManager.sendChannelLocalMessage(u1, "Permissions for this channel have changed; you no longer have the ability to join.", 124, cID);
                        this.leaveChannel(u1);
                    }
                }
                break;
            default:
            	
            	break;
        }        
    	//Sends out the permission change notification to all users currently in the channel
    	JSONObject messageObject = ChannelDataPreparer.preparePermissionChange(channel, p);
    	messageObject.put("id", channel.getNextMessageID());
        messageObject.put("type", Message.PERMISSION_UPDATE);//Type 9 = permission update
        for (User u1 : channel.getUsers()) {
        	//Loops through all the people currently in the channel, sending the permission change to all of them.
            u1.addQueuedMessage(channel.channelID, messageObject);
        }

        //Populates the response as successful
        response.put("status", 200);//Permissions updated successfully
        response.put("msgCode", 158);
    	response.put("message", "Permissions have been successfully updated for this channel.");
    	//System.out.println("Updated permission "+pID+" for channel "+cID+" ("+oldV+" to "+newV+")");
        return response;
    }
    
    public JSONObject changeRankName (User u, int cID, byte rank, String name) throws JSONException {
    	JSONObject response = new JSONObject();
        response.put("id", rank);
        Channel channel = channelManager.getChannel(cID);
        if (channel == null) {
        	response.put("status", 404);
        	response.put("msgCode", 163);
        	response.put("message", "Cannot change rank names: channel not found.");
            return response;
        }
        if (!channel.userHasPermissionOld(u, Permission.DETAILCHANGE)) {
        	//Check if user has ability to change details (8 = change channel details)
        	response.put("status", 403);
        	response.put("msgCode", 125);
        	response.put("message", "You do not have the ability to change the details of this channel.");
            return response;
        }
        if (rank > Settings.TOTAL_RANKS || rank < 0) {
        	//Invalid rank level
        	response.put("status", 400);
        	response.put("msgCode", 126);
        	response.put("message", "Invalid rank level specified.");
            return response;
        }
        if (name.length() > 15) {
        	//Checks the name length (must be less than 15 characters)
        	response.put("status", 400);
        	response.put("msgCode", 127);
        	response.put("message", "The rank name you have specified is too long.\nRank names are limited to 15 characters.");
            return response;
        }
        //Add additional checks (such as removing/encoding special characters, filtering bad language, etc) here
        channel.setRankName(rank, name);//Changes the name in the temporarily loaded version of the channel
        //channel.flushChannelDetails();//Applies the name change to the channel database
        channel.flushRequired = true;
        JSONObject messageObject = ChannelDataPreparer.prepareRankNameChange(channel, rank);
        messageObject.put("id", channel.getNextMessageID());
        messageObject.put("type", Message.RANK_NAME_UPDATE);//Type 16 = rank name update
        for (User u1 : channel.getUsers()) {
        	//Loops through all the people currently in the channel, sending the rank name change to all of them.
            u1.addQueuedMessage(channel.channelID, messageObject);
        }
        
        //Populates the response as successful
        response.put("status", 200);//Permissions updated successfully
        response.put("msgCode", 164);
    	response.put("message", "The name for rank "+rank+" have been successfully updated for this channel.");
        return response;
    }
    
    public JSONObject chageOpeningMessage (User u, int cID, String message, Color newColour) throws JSONException {
    	JSONObject response = new JSONObject();
        Channel c = channelManager.getChannel(cID);
        if (c == null) {
        	response.put("status", 404);
        	response.put("msgCode", 165);
        	response.put("message", "Cannot change channel details: channel not found.");
            return response;
        }
        if (!c.userHasPermissionOld(u, Permission.DETAILCHANGE)) {
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
        c.setOpeningMessage(message, Color.BLACK);//Changes the opening message in the temporarily loaded version of the channel, sets the colour to black (default)
        c.flushRequired = true;
        //c.flushChannelDetails();//Applies the message change to the channel database
        JSONObject messageObject = ChannelDataPreparer.prepareChannelDetails(c);
        messageObject.put("id", c.getNextMessageID());
        messageObject.put("type", Message.CHANNEL_DETAIL_UPDATE);//Type 17 = channel details update
        for (User u1 : c.getUsers()) {
        	//Loops through all the people currently in the channel, sending the details change to all of them.
            u1.addQueuedMessage(c.channelID, messageObject);
        }
        
        //Populates the response as successful
        response.put("status", 200);//Opening message updated successfully
        response.put("msgCode", 129);
        response.put("message", "The opening message for this channel has been updated successfully.");
        return response;
    }
    
    public JSONObject addRank (User u, int cID, int uID) throws JSONException {
    	JSONObject response = new JSONObject();
        Channel channel = channelManager.getChannel(cID);
        if (channel == null) {
        	//If the channel was not found, send an error message
        	response.put("status", 404);
        	response.put("msgCode", 158);
        	response.put("message", "The channel must be loaded before you can modify rank data.\nTry joining the channel first.");
            return response;
        }
    	String rankedName = ChatServer.getInstance().userManager().getUsername(uID);
    	if (rankedName == null) {
    		rankedName = "[user not found]";
        }
    	response.put("rankedName", rankedName);
        if (!channel.userHasPermissionOld(u, Permission.RANKCHANGE)) {
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
        JSONObject messageObject = ChannelDataPreparer.sendRankOnList(uID, channel);
        for (User u1 : channel.getUsers()) {//Sends the updated rank to everyone in the channel
        	u1.addQueuedMessage(cID, messageObject);
        }
        if (ChatServer.getInstance().userManager().getUser(uID) != null) {
        	User newRank = ChatServer.getInstance().userManager().getUser(uID);
        	if (channel.getUsers().contains(newRank)) {
        		messageObject = ChannelDataPreparer.updateUserInChannel(newRank, channel);//Updates the user's channel rank
        		for (User u1 : channel.getUsers()) {
                	u1.addQueuedMessage(cID, messageObject);
                }
        		//Sends a packet to the user, informing them of the rank change
        		messageObject = new JSONObject().put("id", channel.getNextMessageID()).put("type", Message.RANK_UPDATE);//Rank update type
        		messageObject.put("userID", newRank.getUserID());
        		messageObject.put("rank", channel.getUserRank(newRank));
        		newRank.addQueuedMessage(cID, messageObject);
        	}
        }
        response.put("status", 200);
    	response.put("msgCode", 133); 
    	response.put("message", rankedName+" has been successfully ranked in this channel.");
		return response;
    }
    
    public JSONObject removeRank (User u, int cID, int uID) throws JSONException {
    	JSONObject response = new JSONObject();
        Channel channel = channelManager.getChannel(cID);
        String targetUsername = ChatServer.getInstance().userManager().getUsername(uID);
        if (channel == null) {
        	//If the channel was not found, send an error message
        	response.put("status", 404);
        	response.put("msgCode", 158);
        	response.put("message", "The channel must be loaded before you can modify rank data.\nTry joining the channel first.");
            return response;
        }
        if (!channel.userHasPermissionOld(u, Permission.RANKCHANGE)) {
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
        JSONObject messageObject = ChannelDataPreparer.removeRankFromList(uID, channel);
        for (User u1 : channel.getUsers()) {//Sends the rank removal to everyone in the channel
        	u1.addQueuedMessage(cID, messageObject);
        }
        if (ChatServer.getInstance().userManager().getUser(uID) != null) {//Checks if the user is online
        	User newRank = ChatServer.getInstance().userManager().getUser(uID);
        	if (channel.getUsers().contains(newRank)) {//Checks if the user is in the channel
        		messageObject = ChannelDataPreparer.updateUserInChannel(newRank, channel);//Updates the user's channel rank
        		for (User u1 : channel.getUsers()) {
                	u1.addQueuedMessage(cID, messageObject);
                }
        		//Sends a packet to the user, informing them of the rank change
        		messageObject = new JSONObject().put("id", channel.getNextMessageID()).put("type", Message.RANK_UPDATE);//Rank update type
        		messageObject.put("userID", newRank.getUserID());
        		messageObject.put("rank", channel.getUserRank(newRank));
        		newRank.addQueuedMessage(cID, messageObject);
        	}
        }
        response.put("status", 200);
    	response.put("msgCode", 138);
    	response.put("message", "The rank for "+targetUsername+" has been revoked successfully.");
		return response;
    }
    
    public JSONObject updateRank (User u, int cID, int uID, int rank) throws JSONException {
    	JSONObject response = new JSONObject();
        Channel channel = channelManager.getChannel(cID);
        String targetUsername = ChatServer.getInstance().userManager().getUsername(uID);
        if (channel == null) {
        	response.put("status", 404);
        	response.put("msgCode", 158);
        	response.put("message", "The channel must be loaded before you can modify rank data.\nTry joining the channel first.");
            return response;
        }
        if (!channel.userHasPermissionOld(u, Permission.RANKCHANGE)) {
        	//Check if user has ability to change ranks (6 = change ranks)
        	response.put("status", 403);
        	response.put("msgCode", 141);
        	response.put("message", "You do not have the ability to change the ranks of other users in this channel.");
            return response;
        }
        byte currentRank = channel.getUserRank(uID);
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
        JSONObject messageObject = ChannelDataPreparer.updateRankOnList(uID, channel);
        for (User u1 : channel.getUsers()) {//Sends the rank update to everyone in the channel
        	u1.addQueuedMessage(cID, messageObject);
        }
        if (ChatServer.getInstance().userManager().getUser(uID) != null) {//Checks if the user is online
        	User newRank = ChatServer.getInstance().userManager().getUser(uID);
        	if (channel.getUsers().contains(newRank)) {//Checks if the user is in the channel
        		messageObject = ChannelDataPreparer.updateUserInChannel(newRank, channel);//Updates the user's channel rank
        		for (User u1 : channel.getUsers()) {
                	u1.addQueuedMessage(cID, messageObject);
                }
        		//Sends a packet to the user, informing them of the rank change
        		messageObject = new JSONObject().put("id", channel.getNextMessageID()).put("type", Message.RANK_UPDATE);
        		messageObject.put("userID", newRank.getUserID());
        		messageObject.put("rank", channel.getUserRank(newRank));
        		newRank.addQueuedMessage(cID, messageObject);
        	}
        }
        response.put("status", 200);
    	response.put("msgCode", 144);
    	response.put("message", "The rank for "+targetUsername+" has been changed successfully.");
        return response;
    }
    
    public JSONObject addBan (User u, int cID, int uID) throws JSONException {
    	JSONObject response = new JSONObject();
        Channel channel = channelManager.getChannel(cID);
        if (channel == null) {
        	response.put("status", 404);
        	response.put("msgCode", 161);
        	response.put("message", "The channel must be loaded before you can modify ban data.\nTry joining the channel first.");
            return response;
        }
    	String bannedName = ChatServer.getInstance().userManager().getUsername(uID);
    	if (bannedName == null) {
    		bannedName = "[user not found]";
        }
    	response.put("bannedName", bannedName);
        if (!channel.userHasPermissionOld(u, Permission.PERMBAN)) {
        	//Check if user has ability to permanently ban (4 = modify bans)
        	response.put("status", 403);
        	response.put("msgCode", 146);        	
        	response.put("message", "You do not have the ability to permanently ban people from this channel.");
            return response;
        }
        if (channel.isUserBanned(uID)) {
        	//Check if the user is already permanently banned from the channel
        	response.put("status", 400);
        	response.put("msgCode", 147);
        	response.put("message", bannedName+" is already on the permanent ban list for this channel.");
            return response;
        }
        if (channel.getUserRank(uID) > 0) {
        	//Checks if user is currently ranked (must revoke rank before they can be permanently banned)
        	response.put("status", 400);
        	response.put("msgCode", 148);
        	response.put("message", bannedName+" currently holds a rank in the channel.\nPlease remove their name from the rank list first.");
            return response;
        }
        if (!channel.addBan(uID)) {
        	//Returns false if an error occurred in the ban changing process
        	response.put("status", 500);
        	response.put("msgCode", 150); 
        	response.put("message", "Could permanently ban this user due to a system error.");
        	return response;
        }        
        JSONObject messageObject = ChannelDataPreparer.sendBanOnList(uID, channel);
        for (User u1 : channel.getUsers()) {//Sends the ban list addition to everyone in the channel
        	u1.addQueuedMessage(cID, messageObject);
        }
        response.put("status", 200);
    	response.put("msgCode", 149);
    	response.put("message", bannedName+" has been permanently banned from this channel.\nTheir ban will take effect when they leave the channel.");
		return response;
    }
    
    public JSONObject removeBan (User u, int cID, int uID) throws JSONException {
    	JSONObject response = new JSONObject();
        Channel channel = channelManager.getChannel(cID);
        if (channel == null) {
        	response.put("status", 404);
        	response.put("msgCode", 161);
        	response.put("message", "The channel must be loaded before you can modify ban data.\nTry joining the channel first.");
            return response;
        }
    	String bannedName = ChatServer.getInstance().userManager().getUsername(uID);
    	if (bannedName == null) {
    		bannedName = "[user not found]";
        }
    	response.put("bannedName", bannedName);
        if (!channel.userHasPermissionOld(u, Permission.PERMBAN)) {
        	//Check if user has ability to remove users from the permanent ban list (4 = modify bans)
        	response.put("status", 403);
        	response.put("msgCode", 151); 
        	response.put("message", "You do not have the ability to revoke permanent bans for people in this channel.");
            return response;
        }
        if (!channel.isUserBanned(uID)) {
        	//Checks if the specified user is already banned
        	response.put("status", 400);
        	response.put("msgCode", 152);
        	response.put("message", bannedName+" is not currently permanently banned from the channel.");
            return response;
        }
        if (!channel.removeBan(uID)) {
        	//Returns false if an error occurred in the ban changing process
            response.put("status", 200);
        	response.put("msgCode", 154);
        	response.put("message", "Could unban this user due to a system error.");
        	return response;
        } 
        JSONObject messageObject = ChannelDataPreparer.removeBanFromList(uID, channel);
        for (User u1 : channel.getUsers()) {//Sends the ban list removal to everyone in the channel
        	u1.addQueuedMessage(cID, messageObject);
        }
        response.put("status", 200);
    	response.put("msgCode", 153);
    	response.put("message", "The permanent ban for "+bannedName+" has been removed successfully.");
        return response;
    }

}
