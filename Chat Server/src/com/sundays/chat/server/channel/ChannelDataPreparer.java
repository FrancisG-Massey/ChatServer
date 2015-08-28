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
import com.sundays.chat.server.User;
import com.sundays.chat.server.Settings.Message;


/**
 *
 * @author francis
 */
public class ChannelDataPreparer {
    
    
    protected static JSONObject prepareChannelPermissions (Channel c) {
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
    protected static JSONObject preparePermissionChange (Channel c, Permission p) {
    	
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
    
    protected static JSONObject prepareChannelDetails (Channel c) {
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
    protected static JSONObject prepareRankNames (Channel c) {
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
    protected static JSONObject prepareRankNameChange (Channel c, int rank) {
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
    
    private static JSONObject getBasicGroupDetails (ChannelGroup g) throws JSONException {
    	/**
         * @param g, the group to retrieve data from
         * @description prepares a JSON object containing basic information (name, id, icon, type) about a specified channel group
         * 				 for use only within other methods in this class 
         */
    	JSONObject responseJSON = new JSONObject();
    	responseJSON.put("id", g.overrides);
    	responseJSON.put("name", g.getName());
    	responseJSON.put("icon", g.getIconUrl());
    	responseJSON.put("type", g.groupType);
    	return responseJSON;
    }
    
    protected static JSONObject prepareChannelList (Channel c) {
        /**
         * @param c, the channel to retrieve data from
         * @description prepares a JSON object containing data about all the users currently in the channel
         */
    	if (c == null) {
    		return null;
    	}
    	JSONObject responseJSON = new JSONObject();
        try {
        	responseJSON.put("id", c.channelID);
        	responseJSON.put("totalUsers", c.getNoUsers());
        	if (c.getNoUsers() > 0) {
        		JSONObject[] members = new JSONObject[c.getNoUsers()];
        		int i = 0;
        		for (User u1 : c.getUsers()) {
        			JSONObject member = new JSONObject();
        			member.put("userID", u1.getUserID());
        			member.put("username", u1.getUsername());
        			member.put("group", getBasicGroupDetails(c.getUserGroup(u1.getUserID())));
        			member.put("rank", c.getUserRank(u1));
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
    
    protected static JSONObject sendUserInChannel (User u, Channel c) {
        /**
         * @param u, the user of the person who joined the channel
         * @param c, the channel to retrieve data from
         * @description creates a message telling the recipient to add the specified user to the list
         */
    	JSONObject messageObject = new JSONObject();
        try {
			messageObject.put("id", c.getNextMessageID());
			messageObject.put("type", Message.CHANNEL_LIST_ADDITION);//Type 6 = channel list addition
			messageObject.put("username", u.getUsername());//Username of the user joining the channel
			messageObject.put("userID", u.getUserID());//User ID of the user joining the channel
			messageObject.put("group", getBasicGroupDetails(c.getUserGroup(u.getUserID())));
			messageObject.put("rank", c.getUserRank(u));//Rank of the user joining the channel
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return messageObject;            
    }
    
    protected static JSONObject removeUserFromChannel (User u, Channel c) {
    	/**
         * @param u, the user object of the person who left the channel
         * @param c, the channel to retrieve data from
         * @description creates a message telling the recipient to remove the specified user from the list
         */
    	JSONObject messageObject = new JSONObject();    	
		try {
			messageObject.put("id", c.getNextMessageID());
			messageObject.put("type", Message.CHANNEL_LIST_REMOVAL);//Type 7 = channel list removal
			messageObject.put("userID", u.getUserID());//User ID of the user leaving the channel
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return messageObject;
    }
    
    protected static JSONObject updateUserInChannel (User u, Channel c) {
        /**
         * @param u, the user of the person to update
         * @param c, the channel to retrieve data from
         * @description creates a message containing details to update a user already in the channel (including rank and username)
         */
    	JSONObject messageObject = new JSONObject();
    	try {
			messageObject.put("id", c.getNextMessageID());
			messageObject.put("type", Message.CHANNEL_LIST_UPDATE);//Type 8 = channel list member update
			messageObject.put("username", u.getUsername());
			messageObject.put("userID", u.getUserID());
			messageObject.put("group", getBasicGroupDetails(c.getUserGroup(u.getUserID())));//New information about the group the user is in
			messageObject.put("rank", c.getUserRank(u));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return messageObject;
    }
    
    protected static JSONObject prepareRankList (Channel c) {
        /**
         * @param c, the channel to retrieve data from
         * @description prepares a JSON object containing data about all the users ranked in the channel
         */
    	if (c == null) {
    		return null;
    	}
    	JSONObject responseJSON = new JSONObject();
    	Map<Integer, Byte> ranksList = c.getRanks();//Picks up the rank data for the channel
        try {
        	responseJSON.put("id", c.channelID);
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
        			rank.put("group", getBasicGroupDetails(c.getUserGroup(userID)));
        			rank.put("rank", c.getUserRank(userID));
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
    
    protected static JSONObject sendRankOnList (int uID, Channel c) {
        /**
         * @param uID, the ID of the user to add to the rank list
         * @param c, the channel to retrieve data from
         * @description creates a message telling the recipient to add the specified user to the rank list
         */
    	JSONObject responseJSON = new JSONObject();
    	String un = ChatServer.getInstance().userManager().getUsername(uID);//UserID
        if (un == null) {
            un = "[user not found]";
        }
    	try {
			responseJSON.put("id", c.getNextMessageID());
	    	responseJSON.put("type", Message.RANK_LIST_ADDITION);//Type 11 = rank list addition
	    	responseJSON.put("userID", uID);//User ID of the user to add to the list
            responseJSON.put("username", un);//Username of the user joining the channel
			responseJSON.put("group", getBasicGroupDetails(c.getUserGroup(uID)));//Information about the group of the user being added to the rank list
            responseJSON.put("rank", c.getUserRank(uID));//Rank of the user joining the channel
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return responseJSON;            
    }
    
    protected static JSONObject removeRankFromList (int uID, Channel c) {
    	/**
         * @param uID, the ID of the user to remove from the rank list
         * @param c, the channel to retrieve data from
         * @description creates a message telling the recipient to remove the specified user from the rank list
         */
    	JSONObject responseJSON = new JSONObject();
    	try {
			responseJSON.put("id", c.getNextMessageID());
			responseJSON.put("type", Message.RANK_LIST_REMOVAL);//Type 12 = rank list removal
	    	responseJSON.put("userID", uID);//User ID of the user to remove from the rank list
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return responseJSON;
    }
    
    protected static JSONObject updateRankOnList (int uID, Channel c) {
        /**
         * @param uID, the ID of the user to update the rank of
         * @param c, the channel to retrieve data from
         * @description creates a message telling the recipient to update the details of the specified user on the rank list
         */
    	JSONObject responseJSON = new JSONObject();
    	String un = ChatServer.getInstance().userManager().getUsername(uID);
    	if (un == null) {
    		un = "[user not found]";
        }
    	try {
			responseJSON.put("id", c.getNextMessageID());
			responseJSON.put("type", Message.RANK_LIST_UPDATE);//Type 13 = rank list update
			responseJSON.put("userID", uID);//User ID of the user to update on the list
            responseJSON.put("username", un);//Username to change the user on the list to
            responseJSON.put("group", getBasicGroupDetails(c.getUserGroup(uID)));//New information about the group the user is in
            responseJSON.put("rank", c.getUserRank(uID));//Rank to change the user on the list to
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return responseJSON;
    }
    
    protected static JSONObject prepareBanList (Channel c) {
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
    
    protected static JSONObject sendBanOnList (int uID, Channel c) {
        /**
         * @param uID, the id of the user being permanently banned
         * @param c, the channel to retrieve data from
         * @description creates a message telling the recipient to add the specified user to the ban list
         */
    	JSONObject responseJSON = new JSONObject();
    	String un = ChatServer.getInstance().userManager().getUsername(uID);
    	if (un == null) {
    		un = "[user not found]";
        }
    	try {
			responseJSON.put("id", c.getNextMessageID());
			responseJSON.put("type", Message.BAN_LIST_ADDITION);//Type 14 = ban list addition
			responseJSON.put("userID", uID);//User ID of the user to add to the list
            responseJSON.put("username", un);//Username of the user to add to the list
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return responseJSON;
            
    }
    
    protected static JSONObject removeBanFromList (int uID, Channel c) {
    	/**
         * @param uID, the id of the user to remove the permanent ban from
         * @param c, the channel to retrieve data from
         * @description creates a message telling the recipient to remove the specified user from the ban list
         */
    	JSONObject responseJSON = new JSONObject();
    	try {
			responseJSON.put("id", c.getNextMessageID());
			responseJSON.put("type", Message.BAN_LIST_REMOVAL);//Type 15 = ban list removal
			responseJSON.put("userID", uID);//User ID of the user to remove from the list
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return responseJSON;
    }
    
    protected static JSONObject prepareGroupList (Channel c) {
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
