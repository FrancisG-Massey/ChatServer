/*******************************************************************************
 * Copyright (c) 2013, 2016 Francis G.
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
package com.sundays.chat.server.channel.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sundays.chat.server.channel.ChannelUser;
import com.sundays.chat.server.message.MessagePayload;
import com.sundays.chat.server.user.UserLookup;

/**
 *
 * @author Francis
 */
public class ChannelPacketFactory {
	
	private static ChannelPacketFactory instance;
	
	public static ChannelPacketFactory getInstance() {
		if (instance == null) {
			instance = new ChannelPacketFactory();
		}
		return instance;
	}
    
    /**
     * Packs the basic details (name, openingMessage, owner) of a channel into a message.
     * @param channel The channel object to retrieve details from
     * @param userManager The user manager for the server
     * @return A message payload containing the channel details
     */
    public MessagePayload createDetailsMessage (Channel channel, UserLookup userManager) {
    	if (channel == null) {
    		throw new IllegalArgumentException("channel must not be null.");
    	}
    	MessagePayload message = new MessagePayload();
    	
    	message.put("memberCount", channel.getUserCount());
    	message.put("guestsCanJoin", channel.getGroup(ChannelGroup.GUEST_GROUP).hasPermission(ChannelPermission.JOIN));
		message.put("name", channel.getName());
    	message.put("welcomeMessage", channel.getAttribute(ChannelAttribute.WELCOME_MESSAGE));
    	message.put("messageColour", channel.getAttribute(ChannelAttribute.WELCOME_MESSAGE_COLOUR));
    	
    	MessagePayload owner = new MessagePayload();
    	owner.put("id", channel.getOwnerID());
    	owner.put("name", userManager.getUsername(channel.getOwnerID()));
    	message.put("owner", owner);
    	
        return message;
    }
    
    /**
     * Packs the basic details (name, id, icon, type) of a channel group into a message payload.
     * @param group The group to pack basic details from
     * @return A message payload containing the group details
     */
    public MessagePayload createGroupDetails (ChannelGroup group) {
    	MessagePayload message = new MessagePayload();
    	message.put("id", group.getId());
    	message.put("name", group.getName());
    	message.put("icon", group.getIconUrl());
    	message.put("type", group.getType());
    	return message;
    }
    
    /**
     * Packs a {@link MessagePayload} containing data about all the users currently in the channel.
     * @param channel The channel to retrieve data from
     * @return The payload of the new message.
     */
    public MessagePayload createChannelUserList (Channel channel) {
    	if (channel == null) {
    		throw new IllegalArgumentException("channel must not be null.");
    	}
    	MessagePayload message = new MessagePayload();
    	message.put("id", channel.getId());
    	message.put("totalUsers", channel.getUserCount());
    	
    	List<MessagePayload> userData = new ArrayList<>();
    	for (ChannelUser u1 : channel.getUsers()) {
    		MessagePayload member = new MessagePayload();
			member.put("userID", u1.getId());
			member.put("username", u1.getName());
			ChannelGroup group = channel.getUserGroup(u1.getId());
			member.put("group", createGroupDetails(group));
			userData.add(member);
		}
    	message.put("users", (Serializable) userData);
        return message;
    }
    
    /**
     * Creates a message notifying the recipient to add the specified user to the list
     * @param user The user joining the channel
     * @param channel The channel to retrieve data from
     * @return The payload of the new message.
     */
    public MessagePayload createChannelUserAddition (ChannelUser user, Channel channel) {
    	MessagePayload message = new MessagePayload();
    	
    	message.put("userID", user.getId());//User ID of the user joining the channel
    	message.put("username", user.getName());//Username of the user joining the channel
    	
    	ChannelGroup group = channel.getUserGroup(user.getId());
    	message.put("group", createGroupDetails(group));
		return message;            
    }
    
    /**
     * Creates a message notifying the recipient to remove the specified user from the list
     * @param user The user who left the channel
     * @param channel The channel to retrieve data from
     * @return The payload of the new message.
     */
    public MessagePayload createChannelUserRemoval (ChannelUser user, Channel channel) {
    	MessagePayload message = new MessagePayload();
    	
    	message.put("userID", user.getId());
		return message;
    }
    
    /**
     * Creates a message notifying the recipient to update a user in the channel.
     * @param user The user to update
     * @param channel The channel to retrieve data from
     * @return The payload of the new message.
     */
    public MessagePayload createChannelUserUpdate (ChannelUser user, Channel channel) {
    	MessagePayload message = new MessagePayload();
    	
		message.put("userID", user.getId());
		message.put("username", user.getName());
		
		ChannelGroup group = channel.getUserGroup(user.getId());
		message.put("group", createGroupDetails(group));//New information about the group the user is in
		message.put("rank", group.getId());
		return message;
    }
    
    /**
     * Packs a {@link MessagePayload} containing data about all the channel members
     * @param channel The channel to retrieve data from
     * @param userLookup The user lookup service for the server
     * @return The payload of the new message.
     */
    public MessagePayload createMemberList (Channel channel, UserLookup userLookup) {
    	if (channel == null) {
    		throw new IllegalArgumentException("channel must not be null.");
    	}
    	MessagePayload message = new MessagePayload();
    	
    	Map<Integer, Integer> ranksList = channel.getMembers();//Picks up the rank data for the channel
    	
    	message.put("id", channel.getId());
    	message.put("totalUsers", ranksList.size());
    	
    	List<MessagePayload> memberData = new ArrayList<>();
    	for (Integer userID : ranksList.keySet()) {
    		MessagePayload member = new MessagePayload();
    		member.put("userID", userID);
			String username = userLookup.getUsername(userID);
            if (username == null) {
            	username = "[user not found]";//If there was no username, apply '[user not found]' as username
            }

            member.put("username", username);
			ChannelGroup group = channel.getUserGroup(userID);
			member.put("group", createGroupDetails(group));
			member.put("rank", group.getId());
			memberData.add(member);
		}
    	message.put("ranks", (Serializable) memberData);
        return message;
    } 
    
    /**
     * Creates a message notifying the recipient to add the specified user to the rank list
     * @param userID The ID of the user to add to the rank list
     * @param channel The channel to retrieve data from
     * @param userLookup The user lookup service for the server
     * @return The payload of the new message.
     */
    public MessagePayload createRankListAddition (int userID, Channel channel, UserLookup userLookup) {
    	MessagePayload message = new MessagePayload();
    	String username = userLookup.getUsername(userID);//UserID
        if (username == null) {
        	username = "[user not found]";
        }
    	message.put("userID", userID);//User ID of the user to add to the list
    	message.put("username", username);//Username of the user joining the channel
    	
    	ChannelGroup group = channel.getUserGroup(userID);
        message.put("group", createGroupDetails(group));//Information about the group of the user being added to the rank list
		message.put("rank", group.getId());//Rank of the user joining the channel
		
		return message;            
    }
    
    /**
     * Creates a message notifying the recipient to remove the specified user from the rank list
     * @param userID The ID of the user to remove from the rank list
     * @param channel The channel to retrieve data from
     * @return The payload of the new message.
     */
    public MessagePayload createRankListRemoval (int userID, Channel channel) {
    	MessagePayload message = new MessagePayload();

		message.put("userID", userID);//User ID of the user to remove from the rank list
		return message;
    }
    
    /**
     * Creates a message notifying the recipient to update the details of the specified user on the rank list
     * @param userID The ID of the user on the rank list to update
     * @param channel The channel to retrieve data from
     * @param userManager The user manager for the server
     * @return The payload of the new message.
     */
    public MessagePayload createRankListUpdate (int userID, Channel channel, UserLookup userManager) {
    	MessagePayload message = new MessagePayload();
    	
    	String username = userManager.getUsername(userID);
    	if (username == null) {
    		username = "[user not found]";
        }

		message.put("userID", userID);//User ID of the user to update on the list
		message.put("username", username);//Username to change the user on the list to
		
		ChannelGroup group = channel.getUserGroup(userID);
		message.put("group", createGroupDetails(group));//New information about the group the user is in
		message.put("rank", group.getId());//Rank to change the user on the list to
		
		return message;
    }
    
    /**
     * Packs a {@link MessagePayload} containing data about all users permanently banned from the channel
     * @param channel The channel to retrieve data from
     * @param userManager The user manager for the server
     * @return The payload of the new message.
     */
    public MessagePayload createBanList (Channel channel, UserLookup userManager) {
    	if (channel == null) {
    		throw new IllegalArgumentException("channel must not be null.");
    	}
    	MessagePayload message = new MessagePayload();
    	
    	Set<Integer> bans = channel.getBans();
    	message.put("id", channel.getId());
    	message.put("totalBans", bans.size());
    	List<MessagePayload> banList = new ArrayList<>();
		for (int ban : bans) {
			MessagePayload banData = new MessagePayload();
			banData.put("userID", ban);
			String username = userManager.getUsername(ban);
			if (username == null) {
				username = "[user not found]";
            }
			banData.put("username", username);
			banList.add(banData);
		}
		message.put("bans", (Serializable) banList);
		return message;
    } 
    
    /**
     * Creates a message notifying the recipient to add the specified user to the ban list
     * @param userID The ID of the user to add to the ban list
     * @param channel The channel to retrieve data from
     * @param userManager The user manager for the server
     * @return The payload of the new message.
     */
    public MessagePayload createBanListAddition (int userID, Channel channel, UserLookup userManager) {
    	MessagePayload message = new MessagePayload();

    	String username = userManager.getUsername(userID);
    	if (username == null) {
    		username = "[user not found]";
        }
    	
		message.put("userID", userID);//User ID of the user to add to the list
		message.put("username", username);//Username of the user to add to the list
		
		return message;
            
    }
    
    /**
     * Creates a message notifying the recipient to remove the specified user from the ban list
     * @param userID The id of the user to remove from the ban list
     * @param channel The channel to retrieve data from
     * @return The payload of the new message.
     */
    public MessagePayload createBanListRemoval (int userID, Channel channel) {
    	MessagePayload message = new MessagePayload();

    	message.put("userID", userID);//User ID of the user to remove from the list
    	
		return message;
    }
    
    public MessagePayload createAttributeUpdate (Channel channel, ChannelAttribute attribute) {
    	MessagePayload message = new MessagePayload();

    	message.put("key", attribute.getName());
    	message.put("value", channel.getAttribute(attribute.getName(), attribute.getDefaultValue()));
    	
		return message;
    }
    
    /**
     * Packs a {@link MessagePayload} containing data about all groups in the channel
     * @param channel The channel to retrieve data from
     * @return The payload of the new message.
     */
    public MessagePayload createGroupList (Channel channel) {
    	if (channel == null) {
    		throw new IllegalArgumentException("channel must not be null.");
    	}
    	MessagePayload message = new MessagePayload();

    	Map<Integer, ChannelGroup> channelGroups = channel.getGroups();//Picks up the rank data for the channel
    	message.put("id", channel.getId());
    	message.put("totalGroups", channelGroups.size());
    	
    	List<MessagePayload> groupList = new ArrayList<>();		
		for (ChannelGroup group : channelGroups.values()) {
			MessagePayload groupData = new MessagePayload();
			groupData.put("id", group.getId());
			groupData.put("name", group.getName());
			groupData.put("permissions", group.getPermissions().toString());
			groupData.put("iconUrl", group.getIconUrl());
			groupData.put("type", group.getType());
			groupList.add(groupData);
		}
		message.put("groups", (Serializable) groupList);
    	return message;
    }
}
