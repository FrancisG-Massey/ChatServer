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
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.sundays.chat.io.ChannelDataManager;
import com.sundays.chat.io.ChannelDetails;
import com.sundays.chat.io.ChannelGroupData;
import com.sundays.chat.server.Permission;
import com.sundays.chat.server.Settings;
import com.sundays.chat.server.message.MessagePayload;
import com.sundays.chat.server.user.User;

/**
 * Represents a chat channel on the server.
 *
 * @author Francis
 */
public final class Channel {
	
	private static final Logger logger = Logger.getLogger(Channel.class);

	private final int id;
	
	/*Permanent data*/
    private Color openingMessageColour = Color.BLACK;
    private String name = "Not in Channel";
    private String channelAbbr = "undefined";
    private String openingMessage = "Not in Channel";
    private final Map<Integer, Byte> ranks;
    private final Set<Integer> permBans;
    private final Map<Byte, String> rankNames;
    private final EnumMap<Permission, Integer> permissions;
    private boolean trackMessages = false;
    private final Map<Integer, ChannelGroup> groups;
    
    /*Instanced data*/
    private transient final Map<Integer, Long> tempBans = new ConcurrentHashMap<Integer, Long>();
    private transient final Set<User> members = Collections.newSetFromMap(new ConcurrentHashMap<User, Boolean>());
    private transient final LinkedList<MessagePayload> messageCache = new LinkedList<>();
    //Contains a specified number of recent messages from the channel (global system and normal messages)
    protected boolean unloadInitialised = false;
    protected boolean resetLaunched = false;
    protected boolean flushRequired = false;
    private int nextMessageID = 1;
    private int lockRank = -100;
    private int channelOwner;
    private long lockExpires = 0L;
    private transient final ChannelDataManager io;
    
    /**
     * 
     * @param id The ID for the new channel
     * @param io The IO manager for saving channel data.
     */
    protected Channel (int id, ChannelDataManager io) {
    	this.id = id;
        this.io = io;
    	this.permissions = new EnumMap<Permission, Integer>(Permission.class);
    	for (Permission p : Permission.values()) {
    		this.permissions.put(p, p.defaultValue());
    	}
    	this.rankNames = new LinkedHashMap<>(Settings.defaultRanks);
    	this.groups = loadGroups(new ArrayList<ChannelGroupData>());
    	this.permBans = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
    	this.ranks = new ConcurrentHashMap<>();
    }

    protected Channel(int id, ChannelDetails details, ChannelDataManager io) {
        this.id = id;
        this.io = io;
        this.name = details.name;
        this.channelOwner = details.owner;
        this.openingMessage = details.openingMessage;
        this.channelAbbr = details.abbreviation;
        this.permissions = validatePermissions(details.permissions);
        this.rankNames = validateRankNames(details.rankNames);
        this.trackMessages = details.trackMessages;
        this.groups = loadGroups(io.getChannelGroups(id));
        this.permBans = loadBanList();//Bans MUST be loaded before ranks. This ensures that people on the ban list take priority over people on the rank list
        this.ranks = loadRanks();
        logger.info("Successfully loaded channel: " + this.name);
    }
        
    /**
     * Gets the ID for this channel
	 * @return the channel ID
	 */
	public int getID() {
		return id;
	}

	/*
     * Reads basic details (fully public methods, make sure you only return read-only variables which cannot be modified)
     */
    public String getName () {
    	return this.name;
    }

    /**
	 * @param name the name to set
	 */
	protected void setName(String name) {
		this.name = name;
	}

	public String getOpeningMessage() {
        return this.openingMessage;
    }

	/**
	 * @param openingMessage the openingMessage to set
	 */
	protected void setOpeningMessage(String openingMessage) {
		this.openingMessage = openingMessage;
	}
    
    public Color getOMColour () {
        return this.openingMessageColour;
    }
    
    public int getLockRank () {
    	return this.lockRank;
    }
    
    public long getLockExpireTime () {
    	return this.lockExpires;
    }
    
    public int getOwnerID () {
    	return this.channelOwner;
    }

    public int getUserCount() {
        return this.members.size();
    }

    /*
     * New grouping system
     */
    public boolean userHasPermission (User user, Permission p) {
    	return userHasPermissionOld(user, p);
        //return getUserGroup(user).permissions.contains(p);
    }
    
    public boolean groupCanActionGroup (ChannelGroup source, ChannelGroup target) {    	
    	return source.childGroups.contains(target.groupID);
    }
    
    protected ChannelGroup getUserGroup (User u) {
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
    		throw new IllegalArgumentException("The requested group does not exist.");
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
    public boolean userHasPermissionOld (User user, Permission p) {
        return getUserRank(user) >= getPermissionValue(p);
    }

    /**
     * Returns the rank held by the user within this channel.<br />
     * NOTE: This method is deprecated. Use {@link #getUserGroup(User)} instead.
     * @param user The user to find the rank of.
     * @return The rank the user holds in the channel (0 for guest).
     */
    public byte getUserRank (User user) {
        return getUserRank(user.getUserID());
    }

    /**
     * Returns the rank held by the user within this channel.<br />
     * NOTE: This method is deprecated. Use {@link #getUserGroup(int)} instead.
     * @param userID The ID of the user to find the rank of.
     * @return The rank the user holds in the channel (0 for guest).
     */
    public byte getUserRank (int userID) {
    	byte rank = 0;
    	if (userID == channelOwner) {
    		rank = Settings.OWNER_RANK;
    	} else if (ranks.containsKey(userID)) {
        	rank = ranks.get(userID);
        } else if (permBans.contains(userID)) {
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
    private Map<Integer, ChannelGroup> loadGroups (List<ChannelGroupData> groupData) {
    	
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
    	for (Entry<Byte, String> rankName : rankNames.entrySet()) {
    		if (!responseGroups.containsKey(rankName.getKey())) {
    			ChannelGroup newGroup = new ChannelGroup(id, (int) Math.random(), rankName.getKey().byteValue());
    			newGroup.setName(rankName.getValue());
    			newGroup.overrides = rankName.getKey();
    			newGroup.setIconUrl("images/ranks/rank"+rankName.getKey()+".png");
    			responseGroups.put(rankName.getKey().intValue(), newGroup);
    		}
    	}
    	
		return responseGroups;    	
    }

    private Map<Integer, Byte> loadRanks() {
    	Map<Integer, Byte> ranks = io.getChannelRanks(id);
        for (Entry<Integer, Byte> r : ranks.entrySet()) {
        	//Run through the ranks, removing any invalid sets
        	if (permBans.contains(r.getKey())) {
        		logger.warn("User "+r.getKey()+" from channel "+id+" is on the ban list. Removing from rank list.");
        		ranks.remove(r);
        		io.removeRank(id, r.getKey());
        	} else if (r.getValue() < 1 || r.getValue() > Settings.TOTAL_RANKS) {
        		//Rank was found to contain an invalid value. Convert to the default rank.
        		logger.warn("User "+r.getKey()+" from channel "+id+" holds an invalid rank of:"+r.getValue()+"." +
        				" Swapping to the default rank of: "+Settings.DEFAULT_RANK+".");
        		r.setValue((byte) Settings.DEFAULT_RANK);
        		io.changeRank(id, r.getKey(), Settings.DEFAULT_RANK);
        	} else if (r.getValue() == Settings.OWNER_RANK && r.getKey() != channelOwner) {
        		logger.warn("User "+r.getKey()+" from channel "+id+" holds a rank of owner(11), but is not specified as the channel owner." +
        				" Swapping to the default rank of: "+Settings.DEFAULT_RANK+".");
        		r.setValue((byte) Settings.DEFAULT_RANK);
        		io.changeRank(id, r.getKey(), Settings.DEFAULT_RANK);
        	}
        }
    	System.out.println(ranks.size() + " rank(s) found for this channel.");
    	return new ConcurrentHashMap<Integer, Byte>(ranks);
    }
    
    private Set<Integer> loadBanList () {
    	List<Integer> bans = io.getChannelBans(id);//Load the bans from the back-end
    	/*for (int ban : bans) {
    		//Validates all entries, removing any names which are on the rank list
    		if (ranks.containsKey(ban)) {
    			bans.remove((Object) ban);
    		}
    	}*/
    	logger.info(bans.size() + " permanent ban(s) found for this channel.");
    	Set<Integer> banSet = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
    	banSet.addAll(bans);
    	return banSet;//Make a concurrent version
    }

    private Map<Byte, String> validateRankNames (Map<Byte, String> names) {
    	//Merges the custom rank names for the channel with the default rank names, checking that each name is valid
    	Map<Byte, String> rankNamesArray = new LinkedHashMap<>(Settings.defaultRanks);
    	if (names == null || names.size() == 0) {
    		//If no names were sent, log a message and use defaults.
    		logger.warn("Invalid rank names sent for channel "+id+"; using defaults.");
    	} else {
    		/*if (names.size() < rankNamesArray.size()) {
    			//If the sent names are less than the total number of ranks, log a warning that missing names will use defaults
    			Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Rank names received for channel "+channelID+" hold only "+names.size()+" names, " +
    					"while there are "+rankNamesArray.size()+" rank names in total. Missing names will use the default values.");
    		}*/
    		for (Entry<Byte, String> nameMapping : names.entrySet()) {
    			//For each additional name, check it's validility.
    			String name = nameMapping.getValue();
    			byte rID = nameMapping.getKey();
    			if (rID > Settings.OWNER_RANK || rID < Settings.GUEST_RANK) {
    				//Custom rank names cannot override the names for global ranks (ranks above owner). Also, any ranks below 'guest' will not be able to enter the channel, and thus are irrelevant.
    				logger.warn("Name for rank id="+rID+" in the retrieved rank names specifies a name for a system rank, which is not allowed. Using default name.");
    				continue;
    			} else if (name == null || name.length() < Settings.rankNameMin || name.length() > Settings.rankNameMax) {
    				//Names cannot be null, nor can they be shorter than the minimum or longer than the maximum length specified
    				logger.warn("Name for rank id="+rID+" in the retrieved rank names is invalid or missing. Using default name.");
    				continue;
    			} else {
    				//If all the checks pass, put the name into the rank name table (overriding the default if applicable).
    				rankNamesArray.put(rID, name);
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
    				logger.warn("The "+p.toString()+" permission in the retrieved permissions " +
    						"is not available. Defaulting to: "+value);
    			} else if (permissions[p.id()] > p.maxValue()) {
    				//If the permission is above the maximum value allowed, log a warning then use the default value
    				logger.warn("The "+p.toString()+" permission in the retrieved permissions " +
    						"(of value "+permissions[p.id()]+") is above the maximum allowed value. Defaulting to: "+value);
    			} else if (permissions[p.id()] < p.minValue()) {
    				//If the permission is below the minimum value allowed, log a warning then use the default value
    				logger.warn("The "+p.toString()+" permission in the retrieved permissions " +
    						"(of value "+permissions[p.id()]+") is below the minimum allowed value. Defaulting to: "+value);
    			} else {
    				value = permissions[p.id()];
    			}    			
    		} else {
    			//If the permission is not in the array, log a warning about the permission missing and use the default value.
    			logger.warn("The "+p.toString()+
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
    	
    	return new ChannelDetails(id, name, openingMessage,
    			channelAbbr, permissionsArray, rankNames, trackMessages, channelOwner);
    }
    
    //Alter temporary data
    protected void setTempBan(int userID, long timeMillis) {
        if (tempBans.containsKey(userID)) {
            tempBans.remove(userID);
        }
        //Sets the expires time to current time plus offset given
        tempBans.put(userID, System.currentTimeMillis()+timeMillis);//Places the ban in the tempBans map
    }

    protected long getBanExpireTime(int userID) {
        if (!tempBans.containsKey(userID)) {
            return 0L;
        }
        return tempBans.get(userID);
    }

    protected void removeTempBan(int userID) {
        if (!tempBans.containsKey(userID)) {
            return;
        }
        tempBans.remove(userID);
    }
    
    protected void setChannelLock (int rank, long timeMillis) {
    	this.lockRank = rank;
    	this.lockExpires = System.currentTimeMillis() + timeMillis;
    }
    
    protected void removeLock () {
    	this.lockExpires = 0L;
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
    protected void addToMessageCache (MessagePayload messageObject) {
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

    protected void setRankName (byte rank, String name) {
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
	        io.addRank(id, uID);
	        ranks.put(uID, (byte) Settings.DEFAULT_RANK);
    	}
        return true;
    }

    protected boolean setRank(int uID, byte rank) {
    	synchronized (ranks) {//Make sure only one thread is trying to modify rank data at a time
	        if (!ranks.containsKey(uID)) {
	            return false;
	        }
	        if (permBans.contains(uID)) {
	            return false;
	        }
	        io.changeRank(id, uID, rank);
	        ranks.put(uID, (byte) rank);
    	}
        return true;
    }

    protected boolean removeRank(int uID) {
    	synchronized (ranks) {//Make sure only one thread is trying to modify rank data at a time
	        if (!ranks.containsKey(uID)) {
	            return false;
	        }
	        io.removeRank(id, uID);
	        ranks.remove(uID);
    	}
        return true;
    }
    
    protected boolean addBan (Integer uID) {
    	synchronized (permBans) {//Make sure only one thread is trying to modify permanent bans at a time
	        if (permBans.contains(uID)) {
	            return false;
	        }        
	        io.addBan(id, uID);
	        permBans.add(uID);
    	}
        return true;
    }
    
    protected boolean removeBan (Integer uID) {
    	synchronized (permBans) {//Make sure only one thread is trying to modify permanent bans at a time
		    if (!permBans.contains(uID)) {
		        return false;
		    }
		    io.removeBan(id, uID);
		    permBans.remove(uID);
    	}
        return true;
    }

    
    //Retrieve channel data
    protected Set<User> getUsers() {
        return this.members;
    }

    protected Map<Byte, String> getRankNames() {
        return this.rankNames;
    }

    protected Map<Integer, Byte> getRanks() {
        return this.ranks;
    }
    
    protected Set<Integer> getBans() {
    	return this.permBans;
    }
    
    protected Map<Integer, Long> getTempBans () {
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
    protected List<MessagePayload> getCurrentCache () {
    	return Collections.unmodifiableList(this.messageCache);
    }
}
