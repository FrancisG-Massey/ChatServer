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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONObject;

import com.sundays.chat.io.ChannelDataManager;
import com.sundays.chat.io.ChannelDetails;
import com.sundays.chat.io.ChannelGroupData;
import com.sundays.chat.server.Permission;
import com.sundays.chat.server.Settings;
import com.sundays.chat.server.User;

/**
 *
 * @author Francis
 */
public final class Channel {

	public final int channelID;
	
	/*Permanent data*/
    private Color openingMessageColour = Color.BLACK;
    private String channelName = "Not in Channel",
    		channelAbbr = "undefined";
    private String openingMessage = "Not in Channel";
    private final Map<Integer, Byte> ranks;
    private final List<Integer> permBans;
    private final Map<Integer, String> rankNames;
    private final EnumMap<Permission, Integer> permissions;
    private boolean trackMessages = false;
    private final Map<Integer, ChannelGroup> groups;
    
    /*Instanced data*/
    private transient final Map<Integer, Date> tempBans = new ConcurrentHashMap<Integer, Date>();
    private transient final List<User> members = new CopyOnWriteArrayList<User>();
    private transient final LinkedList<JSONObject> messageCache = new LinkedList<JSONObject>();
    //Contains a specified number of recent messages from the channel (global system and normal messages)
    protected boolean unloadInitialised = false,
    		resetLaunched = false,
            flushRequired = false;
    private int nextMessageID = 1;
    private int lockRank = -100;
    private int channelOwner;
    private Date lockDuration = null;
    private ChannelDataManager channelBackEnd;

    protected Channel(int id, ChannelDataManager backEnd) {
        this.channelID = id;
        this.channelBackEnd = backEnd;
        ChannelDetails details = channelBackEnd.getChannelDetails(channelID);
        this.channelName = details.name;
        this.channelOwner = details.owner;
        this.openingMessage = details.openingMessage;
        this.channelAbbr = details.abbreviation;
        this.permissions = validatePermissions(details.permissions);
        this.rankNames = validateRankNames(details.rankNames);
        this.trackMessages = details.trackMessages;
        groups = loadGroups();
        permBans = loadBanList();//Bans MUST be loaded before ranks. This ensures that people on the ban list take priority over people on the rank list
        ranks = loadRanks();
        System.out.println("Successfully loaded channel: " + this.channelName);
    }
    
    /*
     * Reads basic details (fully public methods, make sure you only return read-only variables which cannot be modified)
     */
    public String getName () {
    	return this.channelName;
    }

    public String getOpeningMessage() {
        return this.openingMessage;
    }
    
    public Color getOMColour () {
        return this.openingMessageColour;
    }
    
    public int getLockRank () {
    	return this.lockRank;
    }
    
    public Date getLockExpireDate () {
    	return this.lockDuration;
    }
    
    public int getOwnerID () {
    	return this.channelOwner;
    }

    public int getNoUsers() {
        return this.members.size();
    }

    /*
     * New grouping system
     */
    public boolean userHasPermission (User u, Permission p) {
        return getUserGroup(u).permissions.contains(p);
    }
    
    public boolean groupCanActionGroup (ChannelGroup source, ChannelGroup target) {    	
    	return source.childGroups.contains(target.groupID);
    }
    
    private ChannelGroup getUserGroup (User u) {
    	return getUserGroup(u.getUserID());
    }
    
    protected ChannelGroup getUserGroup (int uID) {
    	ChannelGroup group = groups.get(Settings.GUEST_RANK);//Guest
    	if (uID == channelOwner) {
    		group = groups.get(Settings.OWNER_RANK);
    	} else if (ranks.containsKey(uID)) {
        	group = groups.get(ranks.get(uID).intValue());//Manually selected group
        } else if (permBans.contains(uID)) {
        	group = new ChannelGroup(Settings.systemGroups.get(53));//Permanently banned users
        }
    	return (group==null?new ChannelGroup(Settings.systemGroups.get(53)):group);
    	//Returns the "Unknown" group if no other group was found
    }
    
    public EnumSet<ChannelPermission> getUserPermissions (User u) {
    	return getUserGroup(u).permissions.clone();
    }
    
    public String getGroupName (int gID) {
    	if (rankNames.containsKey(gID)) {
    		return rankNames.get(gID);
    	} else {
    		throw new IllegalArgumentException("The requested rank does not exist.");
    	}
    }
    
    /*
     * Old grouping system
     */
    public int getPermissionValue (Permission p) {
    	if (permissions.get(p) != null) {
    		return this.permissions.get(p);
    	} else {
    		return -127;
    	}
    }

    @Deprecated
    public boolean userHasPermissionOld (User u, Permission p) {
        return getUserRank(u) >= getPermissionValue(p);
    }

    @Deprecated
    public byte getUserRank (User u) {
        return getUserRank(u.getUserID());
    }

    @Deprecated
    public byte getUserRank (int uID) {
    	byte rank = 0;
    	if (uID == channelOwner) {
    		rank = Settings.OWNER_RANK;
    	} else if (ranks.containsKey(uID)) {
        	rank = ranks.get(uID);
        } else if (permBans.contains(uID)) {
        	rank = -3;
        }
        return rank;
    }
    
    public String getRankName (int rID) {
    	if (rankNames.containsKey(rID)) {
    		return rankNames.get(rID);
    	} else {
    		throw new IllegalArgumentException("The requested rank does not exist.");
    	}
    }
    
    public boolean isUserBanned (int uID) {
        return permBans.contains(uID);
    }    
    
    //Loading stages
    private Map<Integer, ChannelGroup> loadGroups () {
    	//Pull the group data from the database
    	List<ChannelGroupData> groupData = channelBackEnd.getChannelGroups(channelID);
    	
    	Map<Integer, ChannelGroup> responseGroups = new ConcurrentHashMap<Integer, ChannelGroup>();
    	//ChannelGroup unknGroup = new ChannelGroup(50, 53);
    	//unknGroup.setName("Unknown");
    	//responseGroups.put(-2, unknGroup);
    	for (ChannelGroupData rawGroupData : groupData) {
    		//Loop through each group found, placing them in the responseGroups variable
    		//responseGroups.put(rawGroupData.groupID, new ChannelGroup(rawGroupData));
    		
    		//Temporary method, whereby group data overrides the existing groups. This will be used until group adding/removing/modifying is implemented
    		responseGroups.put(rawGroupData.overrides, new ChannelGroup(rawGroupData));
    	}
    	//Temporary override method:
    	for (Entry<Integer, String> rankName : rankNames.entrySet()) {
    		if (!responseGroups.containsKey(rankName.getKey())) {
    			ChannelGroup newGroup = new ChannelGroup(channelID, (int) Math.random());
    			newGroup.setName(rankName.getValue());
    			newGroup.overrides = rankName.getKey();
    			newGroup.setIconUrl("images/ranks/rank"+rankName.getKey()+".png");
    			responseGroups.put(rankName.getKey().intValue(), newGroup);
    		}
    	}
    	
		return responseGroups;    	
    }

    private Map<Integer, Byte> loadRanks() {
    	Map<Integer, Byte> ranks = channelBackEnd.getChannelRanks(channelID);
        for (Entry<Integer, Byte> r : ranks.entrySet()) {
        	//Run through the ranks, removing any invalid sets
        	if (permBans.contains(r.getKey())) {
        		Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "User "+r.getKey()+" from channel "+channelID+" is on the ban list. Removing from rank list.");
        		ranks.remove(r);
        		channelBackEnd.removeRank(channelID, r.getKey());
        	} else if (r.getValue() < 1 || r.getValue() > Settings.TOTAL_RANKS) {
        		//Rank was found to contain an invalid value. Convert to the default rank.
        		Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "User "+r.getKey()+" from channel "+channelID+" holds an invalid rank of:"+r.getValue()+"." +
        				" Swapping to the default rank of: "+Settings.DEFAULT_RANK+".");
        		r.setValue((byte) Settings.DEFAULT_RANK);
        		channelBackEnd.changeRank(channelID, r.getKey(), Settings.DEFAULT_RANK);
        	} else if (r.getValue() == Settings.OWNER_RANK && r.getKey() != channelOwner) {
        		Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "User "+r.getKey()+" from channel "+channelID+" holds a rank of owner(11), but is not specified as the channel owner." +
        				" Swapping to the default rank of: "+Settings.DEFAULT_RANK+".");
        		r.setValue((byte) Settings.DEFAULT_RANK);
        		channelBackEnd.changeRank(channelID, r.getKey(), Settings.DEFAULT_RANK);
        	}
        }
    	System.out.println(ranks.size() + " rank(s) found for this channel.");
    	return new ConcurrentHashMap<Integer, Byte>(ranks);
    }
    
    private List<Integer> loadBanList () {
    	List<Integer> bans = channelBackEnd.getChannelBans(channelID);//Load the bans from the back-end
    	/*for (int ban : bans) {
    		//Validates all entries, removing any names which are on the rank list
    		if (ranks.containsKey(ban)) {
    			bans.remove((Object) ban);
    		}
    	}*/
    	System.out.println(bans.size() + " permanent ban(s) found for this channel.");
    	return new CopyOnWriteArrayList<Integer>(bans);//Make a concurrent version
    }

    private Map<Integer, String> validateRankNames (Map<Integer, String> names) {
    	//Merges the custom rank names for the channel with the default rank names, checking that each name is valid
    	Map<Integer, String> rankNamesArray = new LinkedHashMap<Integer, String>(Settings.defaultRanks);
    	if (names == null || names.size() == 0) {
    		//If no names were sent, log a message and use defaults.
    		Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Invalid rank names sent for channel "+channelID+"; using defaults.");
    	} else {
    		/*if (names.size() < rankNamesArray.size()) {
    			//If the sent names are less than the total number of ranks, log a warning that missing names will use defaults
    			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Rank names received for channel "+channelID+" hold only "+names.size()+" names, " +
    					"while there are "+rankNamesArray.size()+" rank names in total. Missing names will use the default values.");
    		}*/
    		for (Entry<Integer, String> nameMapping : names.entrySet()) {
    			//For each additional name, check it's validility.
    			String name = nameMapping.getValue();
    			int rID = nameMapping.getKey();
    			if (rID > Settings.OWNER_RANK || rID < Settings.GUEST_RANK) {
    				//Custom rank names cannot override the names for global ranks (ranks above owner). Also, any ranks below 'guest' will not be able to enter the channel, and thus are irrelevant.
    				Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Name for rank id="+rID+" in the retrieved rank names specifies a name for a system rank, which is not allowed. Using default name.");
    				continue;
    			} else if (name == null || name.length() < Settings.rankNameMin || name.length() > Settings.rankNameMax) {
    				//Names cannot be null, nor can they be shorter than the minimum or longer than the maximum length specified
    				Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Name for rank id="+rID+" in the retrieved rank names is invalid or missing. Using default name.");
    				continue;
    			} else {
    				//If all the checks pass, put the name into the rank name table (overriding the default if applicable).
    				rankNamesArray.put((int) rID, name);
    			}
    		}
    	}
		return rankNamesArray;
    }

    private EnumMap<Permission, Integer> validatePermissions (Integer[] permissions) {
    	//Converts the permission int[] into an EnumMap object, and verifies that all permissions are valid.
    	EnumMap<Permission, Integer> permissionArray = new EnumMap<Permission, Integer>(Permission.class);
    	if (permissions == null) {
    		//If there are no permissions sent at all, throw an exception
    		throw new NullPointerException("permissions[] arg of Channel.validatePermissions() cannot be null.");
    	}
    	
    	for (Permission p : Permission.values()) {
    		int value = p.defaultValue();//Set the value to the default from the start
    		if (permissions.length > p.id()) {
    			if (permissions[p.id()] == null) {
    				//If the specified permission does not exist, log a warning then use the default value
    				Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "The "+p.toString()+" permission in the retrieved permissions " +
    						"is not available. Defaulting to: "+value);
    			} else if (permissions[p.id()] > p.maxValue()) {
    				//If the permission is above the maximum value allowed, log a warning then use the default value
    				Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "The "+p.toString()+" permission in the retrieved permissions " +
    						"(of value "+permissions[p.id()]+") is above the maximum allowed value. Defaulting to: "+value);
    			} else if (permissions[p.id()] < p.minValue()) {
    				//If the permission is below the minimum value allowed, log a warning then use the default value
    				Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "The "+p.toString()+" permission in the retrieved permissions " +
    						"(of value "+permissions[p.id()]+") is below the minimum allowed value. Defaulting to: "+value);
    			} else {
    				value = permissions[p.id()];
    			}    			
    		} else {
    			//If the permission is not in the array, log a warning about the permission missing and use the default value.
    			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "The "+p.toString()+
    					" permission was not found in the retrieved permissions. Defaulting to: "+value);
    		}
    		permissionArray.put(p, value);//Place the permission in the final map
    	}
		return permissionArray;
    }

    //Saving stages
    protected ChannelDetails getChannelDetails () {
    	Integer[] permissionsArray = new Integer[permissions.size()];
    	permissionsArray = permissions.values().toArray(permissionsArray);
    	
    	/*String[] rankNamesArray = new String[rankNames.size()];
    	rankNamesArray = rankNames.values().toArray(rankNamesArray);*/
    	
    	return new ChannelDetails(channelID, channelName, openingMessage,
    			channelAbbr, permissionsArray, rankNames, trackMessages, channelOwner);
    }
    
    //Alter temporary data
    protected void setTempBan(int userID, int timeM) {
        if (tempBans.containsKey(userID)) {
            tempBans.remove(userID);
        }
        Date expires = new Date();
        expires.setTime(expires.getTime() + timeM);//Sets the expires time to current time plus offset given
        tempBans.put(userID, expires);//Places the ban in the tempBans map
    }

    protected Date getBanExpireDate(int userID) {
        if (!tempBans.containsKey(userID)) {
            return null;
        }
        return tempBans.get(userID);
    }

    protected void removeTempBan(int userID) {
        if (!tempBans.containsKey(userID)) {
            return;
        }
        tempBans.remove(userID);
    }
    
    protected void setChannelLock (int rank, int timeM) {
    	this.lockRank = rank;
    	Date expires = new Date();
    	expires.setTime(expires.getTime() + timeM);
    	this.lockDuration = expires;
    }
    
    protected void removeLock () {
    	this.lockDuration = null;
    	this.lockRank = -100;
    }

    protected void addUser(User u) {
        if (!members.contains(u)) {
            members.add(u);
        }
    }

    protected void removeUser(User u) {
        if (members.contains(u)) {
            members.remove(u);
        }
    }
    
    /**
     * Adds the specified JSONObject to the recent messages cache. 
     * If the cache is currently full (above the maximum size), removes the oldest message from the cache.
     * 
     * @param messageObject the message to add to the cache.
     */
    protected void addToMessageCache (JSONObject messageObject) {
    	synchronized (messageCache) {
	    	if (messageCache.size() >= Settings.CHANNEL_CACHE_SIZE) {
	    		messageCache.removeFirst();
	    	}
	    	messageCache.addLast(messageObject);
    	}
    }
    
    //Alter permanent data
    protected boolean setPermission(Permission p, int value) {
    	boolean successful;
        if (value > p.maxValue()) {
        	successful = false;//No change, as value is too large
        } else if (value < p.minValue()) {
        	successful = false;//No change, as value is too small
        } else/* if (permID > Settings.defaultPermissions.size()) {
        	successful = false;//Permission does not exist
        } else*/ if (this.permissions.get(p) != null && value == this.permissions.get(p)) {        	
        	successful = true;//Value is identical to current value, no change required
        } else {
        	this.permissions.put(p, value);//.set(p, value);        	
        	successful = true;
        	this.flushRequired = true;//Notifies the auto-save thread that the channel data requires flushing
        	//this.flushPermissions();//If an auto-save thread does not exist, manually flush details each time [removed if auto clean-up thread is created]
        }
        return successful;
    }

    protected void setOpeningMessage(String message, Color c) {
        this.openingMessage = message;
        this.openingMessageColour = c;
        this.flushRequired = true;//Notifies the auto-save thread that the channel data requires flushing
    }

    protected void setRankName (int rank, String name) {
    	this.rankNames.put(rank, name);
    	this.flushRequired = true;
    }
    
    protected boolean addRank(int uID) {
    	synchronized (ranks) {//Make sure only one thread is trying to modify rank data at a time
	        if (ranks.containsKey(uID)) {
	            return false;
	        }
	        if (permBans.contains(uID)) {
	            return false;
	        }
	        channelBackEnd.addRank(channelID, uID);
	        ranks.put(uID, (byte) 1);
    	}
        return true;
    }

    protected boolean setRank(int uID, int rank) {
    	synchronized (ranks) {//Make sure only one thread is trying to modify rank data at a time
	        if (!ranks.containsKey(uID)) {
	            return false;
	        }
	        if (permBans.contains(uID)) {
	            return false;
	        }
	        channelBackEnd.changeRank(channelID, uID, rank);
	        ranks.put(uID, (byte) rank);
    	}
        return true;
    }

    protected boolean removeRank(int uID) {
    	synchronized (ranks) {//Make sure only one thread is trying to modify rank data at a time
	        if (!ranks.containsKey(uID)) {
	            return false;
	        }
	        channelBackEnd.removeRank(channelID, uID);
	        ranks.remove(uID);
    	}
        return true;
    }
    
    protected boolean addBan (Integer uID) {
    	synchronized (permBans) {//Make sure only one thread is trying to modify permanent bans at a time
	        if (permBans.contains(uID)) {
	            return false;
	        }        
	        channelBackEnd.addBan(channelID, uID);
	        permBans.add(uID);
    	}
        return true;
    }
    
    protected boolean removeBan (Integer uID) {
    	synchronized (permBans) {//Make sure only one thread is trying to modify permanent bans at a time
		    if (!permBans.contains(uID)) {
		        return false;
		    }
		    channelBackEnd.removeBan(channelID, uID);
		    permBans.remove(uID);
    	}
        return true;
    }

    
    //Retrieve channel data
    protected List<User> getUsers() {
        return this.members;
    }

    protected Map<Integer, String> getRankNames() {
        return this.rankNames;
    }

    protected Map<Integer, Byte> getRanks() {
        return this.ranks;
    }
    
    protected List<Integer> getBans() {
    	return this.permBans;
    }
    
    protected Map<Integer, Date> getTempBans () {
    	return this.tempBans;
    }
    
    protected Map<Integer, ChannelGroup> getGroups () {
    	return this.groups;
    }
    
    protected int getNextMessageID () {
    	this.nextMessageID++;
    	return this.nextMessageID;
    }
    
    /**
     * Fetches an exact copy of the current message cache, in the format of an ArrayList (to save space).
     * The fetched cache is not a reference to the active cache, but a new object instead, to prevent accidental unsynchronised access.
     * 
     * @return an ArrayList copy of the current message cache
     */
    protected List<JSONObject> getCurrentCache () {
    	return new ArrayList<JSONObject>(this.messageCache);
    }
}
