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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;
import org.json.JSONObject;

import com.sundays.chat.server.ChatServer;
import com.sundays.chat.server.Permission;
import com.sundays.chat.server.Settings;
import com.sundays.chat.server.message.MessagePayload;
import com.sundays.chat.server.user.User;


/**
 *
 * @author Francis
 */
public class ChannelMessageFactory {
	
	private static ChannelMessageFactory instance;
	
	public static ChannelMessageFactory getInstance() {
		if (instance == null) {
			instance = new ChannelMessageFactory();
		}
		return instance;
	}
    
    
    public JSONObject prepareChannelPermissions (Channel c) {
        /**
         * @param c, the channel object to retrieve permission data from
         * @description prepares a JSON object containing the permission details for the specified channel
         */
    	JSONObject responseJSON = new JSONObject();
    	if (c == null) {
    		return null;
    	}
    	try {
			responseJSON.put("permissionVersion", Settings.PERMISSION_VERSION);
			Map<String, JSONObject> permissionArray = new HashMap<String, JSONObject>();
			for (Permission p : Permission.values()) {
        		JSONObject permission = new JSONObject();
        		permission.put("permissionID", p.id());
        		permission.put("name", p.toString().toLowerCase(Locale.ENGLISH));
        		permission.put("value", c.getPermissionValue(p));
        		permission.put("minValue", p.minValue());
        		permission.put("maxValue", p.maxValue());
        		permissionArray.put(p.toString().toLowerCase(Locale.ENGLISH), permission);
        	}
			responseJSON.put("permissions", permissionArray);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return responseJSON;
    }
    
    /**
     * Prepares a JSON object containing the details for the specified permission (usually used to update data on a permission)
     * 
     * @param c, the channel object to retrieve permission data from
     * @param p, the permission to send an update notification for
     */
    public JSONObject preparePermissionChange (Channel c, Permission p) {
    	
    	JSONObject responseJSON = new JSONObject();
    	if (c == null) {
    		return null;
    	}
    	int permissionValue = c.getPermissionValue(p);
    	if (permissionValue == -127) {
    		return null;//Permission does not exist
    	}
		try {
			responseJSON.put("permissionID", p.id());
			responseJSON.put("name", p.toString());
			responseJSON.put("value", permissionValue);
			responseJSON.put("minValue", p.minValue());
			responseJSON.put("maxValue", p.maxValue());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return responseJSON;
    }
    
    public JSONObject prepareChannelDetails (Channel c) {
        /**
         * @param c, the channel object to retrieve details from
         * @description prepares a JSON object containing the basic details about the specified channel
         */
    	JSONObject responseJSON = new JSONObject();
    	if (c == null) {
    		return null;
    	}
        try {
        	responseJSON.put("name", c.getName());
        	responseJSON.put("openingMessage", c.getOpeningMessage());
        	responseJSON.put("messageColour", c.getOMColour().getRGB());
        	JSONObject owner = new JSONObject();
        	owner.put("id", c.getOwnerID());
        	owner.put("name", ChatServer.getInstance().userManager().getUsername(c.getOwnerID()));
        	responseJSON.put("owner", owner);
        } catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return responseJSON;
    }
    
    @Deprecated
    public JSONObject prepareRankNames (Channel c) {
        /**
         * @param c, the channel object to retrieve rank names from
         * @description prepares a JSON object containing the rank names for the specified channel
         */
    	JSONObject responseJSON = new JSONObject();
    	if (c == null) {
    		return null;
    	}
        Map<Integer, String> rankNames = c.getRankNames();
        try {
        	responseJSON.put("notice", "This API method is deprecated. Please use /channel/[x]/groups instead.");
        	responseJSON.put("numberRanks", rankNames.size());
        	List<JSONObject> ranks = new ArrayList<JSONObject>(rankNames.size());
        	for (Entry<Integer, String> name : rankNames.entrySet()) {
        		JSONObject rank = new JSONObject();
        		rank.put("rankID", name.getKey());
        		rank.put("rankName",name.getValue());
        		ranks.add(rank);
        	}
        	responseJSON.put("rankNames", ranks);
        } catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return responseJSON;
    }
    
    @Deprecated
    public JSONObject prepareRankNameChange (Channel c, int rank) {
        /**
         * @param c, the channel object to retrieve rank names from
         * @param pID, the ID of the rank to send an update notification for
         * @description prepares a JSON object updating the rank name of a specific rank for the specified channel
         */
    	JSONObject responseJSON = new JSONObject();
    	if (c == null) {
    		return null;
    	}
        try {
        	responseJSON.put("notice", "This API method is deprecated. Please use /channel/[x]/groups instead.");
        	responseJSON.put("rankID", rank);
        	responseJSON.put("rankName", c.getRankNames().get(rank));
        } catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return responseJSON;
    }
    
    /**
     * Packs the basic details (name, id, icon, type) of a channel group into a message payload.
     * @param group The group to pack basic details from
     * @return A message payload containing the group details
     */
    private MessagePayload createBasicGroupDetails (ChannelGroup group) {
    	MessagePayload message = new MessagePayload();
    	message.put("id", group.overrides);
    	message.put("name", group.getName());
    	message.put("icon", group.getIconUrl());
    	message.put("type", group.groupType);
    	return message;
    }
    
    /**
     * Packs a {@link MessagePayload} containing data about all the users currently in the channel.
     * @param channel The channel to retrieve data from
     * @return
     */
    public JSONObject prepareChannelList (Channel channel) {
    	if (channel == null) {
    		return null;
    	}
    	JSONObject responseJSON = new JSONObject();
        try {
        	responseJSON.put("id", channel.channelID);
        	responseJSON.put("totalUsers", channel.getUserCount());
        	if (channel.getUserCount() > 0) {
        		JSONObject[] members = new JSONObject[channel.getUserCount()];
        		int i = 0;
        		for (User u1 : channel.getUsers()) {
        			JSONObject member = new JSONObject();
        			member.put("userID", u1.getUserID());
        			member.put("username", u1.getUsername());
        			ChannelGroup group = channel.getUserGroup(u1.getUserID());
        			member.put("group", createBasicGroupDetails(group));
        			member.put("rank", group.getLegacyRank());
        			members[i] = member;
        			i++;
        		}
        		responseJSON.put("users", members);
        	}
        } catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return responseJSON;
    }
    
    /**
     * Creates a message notifying the recipient to add the specified user to the list
     * @param user The user joining the channel
     * @param channel The channel to retrieve data from
     * @return The payload of the new message.
     */
    public MessagePayload createChannelUserAddition (User user, Channel channel) {
    	MessagePayload message = new MessagePayload();
    	
    	message.put("userID", user.getUserID());//User ID of the user joining the channel
    	message.put("username", user.getUsername());//Username of the user joining the channel
    	
    	ChannelGroup group = channel.getUserGroup(user.getUserID());
    	message.put("group", createBasicGroupDetails(group));
    	message.put("rank", group.getLegacyRank());//Rank of the user joining the channel
		return message;            
    }
    
    /**
     * Creates a message notifying the recipient to remove the specified user from the list
     * @param user The user who left the channel
     * @param channel The channel to retrieve data from
     * @return The payload of the new message.
     */
    public MessagePayload createChannelUserRemoval (User user, Channel channel) {
    	MessagePayload message = new MessagePayload();
    	
    	message.put("userID", user.getUserID());
		return message;
    }
    
    /**
     * Creates a message notifying the recipient to update a user in the channel.
     * @param user The user to update
     * @param channel The channel to retrieve data from
     * @return The payload of the new message.
     */
    public MessagePayload createChannelUserUpdate (User user, Channel channel) {
    	MessagePayload message = new MessagePayload();
    	
		message.put("userID", user.getUserID());
		message.put("username", user.getUsername());
		
		ChannelGroup group = channel.getUserGroup(user.getUserID());
		message.put("group", createBasicGroupDetails(group));//New information about the group the user is in
		message.put("rank", group.getLegacyRank());
		return message;
    }
    
    public JSONObject prepareRankList (Channel channel) {
        /**
         * @param c, the channel to retrieve data from
         * @description prepares a JSON object containing data about all the users ranked in the channel
         */
    	if (channel == null) {
    		return null;
    	}
    	JSONObject responseJSON = new JSONObject();
    	Map<Integer, Byte> ranksList = channel.getRanks();//Picks up the rank data for the channel
        try {
        	responseJSON.put("id", channel.channelID);
        	responseJSON.put("totalUsers", ranksList.size());
        	if (ranksList.size() > 0) {
        		JSONObject[] ranks = new JSONObject[ranksList.size()];
        		//System.out.println("Ranks: "+ranksList.size());
        		int i = 0;
        		for (Integer userID : ranksList.keySet()) {
        			JSONObject rank = new JSONObject();
        			rank.put("userID", userID);
        			String un = ChatServer.getInstance().userManager().getUsername(userID);//UserID
                    if (un == null) {
                        un = "[user not found]";//If there was no username, apply '[user not found]' as username
                    }
                    //System.out.println("User: "+un);
        			rank.put("username", un);
        			ChannelGroup group = channel.getUserGroup(userID);
        			rank.put("group", createBasicGroupDetails(group));
        			rank.put("rank", group.getLegacyRank());
        			ranks[i] = rank;
        			i++;
        		}
        		responseJSON.put("ranks", ranks);
        	}
        } catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return responseJSON;
    } 
    
    /**
     * Creates a message notifying the recipient to add the specified user to the rank list
     * @param userID The ID of the user to add to the rank list
     * @param channel The channel to retrieve data from
     * @return The payload of the new message.
     */
    public MessagePayload createRankListAddition (int userID, Channel channel) {
    	MessagePayload message = new MessagePayload();
    	String username = ChatServer.getInstance().userManager().getUsername(userID);//UserID
        if (username == null) {
        	username = "[user not found]";
        }
    	message.put("userID", userID);//User ID of the user to add to the list
    	message.put("username", username);//Username of the user joining the channel
    	
    	ChannelGroup group = channel.getUserGroup(userID);
        message.put("group", createBasicGroupDetails(group));//Information about the group of the user being added to the rank list
		message.put("rank", group.getLegacyRank());//Rank of the user joining the channel
		
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
     * @return The payload of the new message.
     */
    public MessagePayload createRankListUpdate (int userID, Channel channel) {
    	MessagePayload message = new MessagePayload();
    	
    	String username = ChatServer.getInstance().userManager().getUsername(userID);
    	if (username == null) {
    		username = "[user not found]";
        }

		message.put("userID", userID);//User ID of the user to update on the list
		message.put("username", username);//Username to change the user on the list to
		
		ChannelGroup group = channel.getUserGroup(userID);
		message.put("group", createBasicGroupDetails(group));//New information about the group the user is in
		message.put("rank", group.getLegacyRank());//Rank to change the user on the list to
		
		return message;
    }
    
    public JSONObject prepareBanList (Channel c) {
        /**
         * @param c, the channel to retrieve data from
         * @description returns a JSONObject containing all the users permanently banned from the channel
         */
    	if (c == null) {
    		return null;
    	}
    	JSONObject responseJSON = new JSONObject();
    	List<Integer> bans = c.getBans();
        try {
			responseJSON.put("id", c.channelID);
			responseJSON.put("totalBans", bans.size());
			if (bans.size() > 0) {
				JSONObject[] banList = new JSONObject[bans.size()];
        		int i = 0;
				for (int ban : bans) {
					JSONObject banObject = new JSONObject();
					banObject.put("userID", ban);
					String un = ChatServer.getInstance().userManager().getUsername(ban);
					if (un == null) {
	                    un = "[user not found]";
	                }
					banObject.put("username", un);
					banList[i] = banObject;
					i++;
				}
				responseJSON.put("bans", banList);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return responseJSON;
    } 
    
    /**
     * Creates a message notifying the recipient to add the specified user to the ban list
     * @param userID The ID of the user to add to the ban list
     * @param channel The channel to retrieve data from
     * @return The payload of the new message.
     */
    public MessagePayload createBanListAddition (int userID, Channel channel) {
    	MessagePayload message = new MessagePayload();

    	String username = ChatServer.getInstance().userManager().getUsername(userID);
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
    
    public JSONObject prepareGroupList (Channel c) {
    	/**
         * @param c, the channel to retrieve data from
         * @description returns a JSONObject containing all the groups in the channel
         */
    	if (c == null) {
    		return null;
    	}
    	JSONObject responseJSON = new JSONObject();
    	Map<Integer, ChannelGroup> channelGroups = c.getGroups();//Picks up the rank data for the channel
        try {
        	responseJSON.put("channelID", c.channelID);
        	responseJSON.put("totalGroups", channelGroups.size());
        	if (channelGroups.size() > 0) {
        		List<JSONObject> groups = new ArrayList<JSONObject>(channelGroups.size());
        		
        		for (Entry<Integer, ChannelGroup> g : channelGroups.entrySet()) {
        			JSONObject group = new JSONObject();
        			group.put("id", g.getKey());
        			group.put("name", g.getValue().getName());
        			group.put("permissions", JSONObject.NULL);
        			group.put("iconUrl", (g.getValue().getIconUrl() == null ? JSONObject.NULL : g.getValue().getIconUrl()));
        			group.put("type", g.getValue().groupType);
        			groups.add(group);
        		}
        		responseJSON.put("groups", groups);
        	}
        } catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return responseJSON;
    }
}
