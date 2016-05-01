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

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sundays.chat.io.ChannelDataIO;
import com.sundays.chat.io.ChannelDetails;
import com.sundays.chat.io.ChannelGroupData;
import com.sundays.chat.io.ChannelGroupType;
import com.sundays.chat.server.channel.ChannelUser;
import com.sundays.chat.server.message.MessagePayload;

/**
 * Represents a chat channel on the server.
 *
 * @author Francis
 */
public final class Channel {
	
	private static final Logger logger = LoggerFactory.getLogger(Channel.class);

	private final int id;
	
	/*Permanent data*/
    private String name = "Not in Channel";
    private String alias = "undefined";
    private final Map<Integer, Integer> members;
    private final Set<Integer> permBans;
    private final Map<Integer, ChannelGroup> groups;
    private final Map<String, Serializable> attributes;
    
    /**
     * Represents the users who are temporarily blocked from joining this channel, and the time (as milliseconds since the Unix epoch) their ban will be lifted.
     * This information is not stored persistently, and will be removed when the channel is unloaded.
     */
    private transient final Map<Integer, Long> tempBans = new HashMap<Integer, Long>();
    
    /**
     * Represents the users who are currently in the channel.
     */
    private transient final Set<ChannelUser> users = new HashSet<>();
    private transient final Cache<Long, MessagePayload> messageCache = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();
    //Contains a specified number of recent messages from the channel (global system and normal messages)
    protected boolean unloadInitialised = false;
    protected boolean resetLaunched = false;
    protected boolean flushRequired = false;
    private int nextMessageID = 1;
    private int lockRank = -100;
    private int ownerID;
    private long lockExpires = 0L;
    
    /**
     * The link to the persistence layer for channel data. Used for loading and saving data.
     */
    private transient final ChannelDataIO io;
    
    /**
     * 
     * @param id The ID for the new channel
     * @param io The IO manager for saving channel data.
     */
    protected Channel (int id, ChannelDataIO io) {
    	this.id = id;
        this.io = io;
    	this.attributes = new HashMap<>();
    	this.groups = loadGroups(new HashSet<ChannelGroupData>());
    	this.permBans = new HashSet<>();
    	this.members = new HashMap<>();
    }

    protected Channel(int id, ChannelDetails details, ChannelDataIO io) throws IOException {
        this.id = id;
        this.io = io;
        this.name = details.getName();
        this.ownerID = details.getOwner();
        this.attributes = loadAttributes(io.getChannelAttributes(id));
        this.alias = details.getAlias();
        this.groups = loadGroups(io.getChannelGroups(id));
        this.permBans = loadBanList();//Bans MUST be loaded before ranks. This ensures that people on the ban list take priority over people on the rank list
        this.members = loadMembers();
    }
        
    /**
     * Indicates whether the channel details need to be sent to the persistence layer for an update.
	 * @return true if a flush of channel details is required, false otherwise
	 */
	public boolean isFlushRequired() {
		return flushRequired;
	}

	/**
     * Gets the ID for this channel
	 * @return the channel ID
	 */
	public int getId() {
		return id;
	}
	
	/**
	 * Gets the name of the channel
	 * @return The channel name
	 */
    public String getName () {
    	return this.name;
    }

    /**
     * Sets the channel name to the provided string
	 * @param name The new name for the channel
	 */
	protected void setName(String name) {
		this.name = name;
		this.flushRequired = true;
	}	

	/**
	 * @return the alias
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * @param alias the alias to set
	 */
	public void setAlias(String alias) {
		this.alias = alias;
		this.flushRequired = true;
	}
	
	/**
	 * Gets the channel attribute associated with the given key.
	 * @param attribute The attribute to lookup
	 * @return The attribute value, or null if the attribute has not yet been assigned.
	 */
	public Serializable getAttribute(ChannelAttribute attribute) {
        return getAttribute(attribute.getName(), attribute.getDefaultValue());
    }

	/**
	 * Gets the channel attribute associated with the given key.
	 * @param key The attribute key to lookup
	 * @param defaultValue The string to return if the attribute is not set
	 * @return The attribute value, or null if the attribute has not yet been assigned.
	 */
	public Serializable getAttribute(String key, Serializable defaultValue) {		
        return attributes.containsKey(key) ? attributes.get(key) : defaultValue;
    }
    
    public int getLockRank () {
    	return this.lockRank;
    }
    
    public long getLockExpireTime () {
    	return this.lockExpires;
    }
    
    /**
     * Gets the user ID of the channel owner
     * @return The owner ID
     */
    public int getOwnerID () {
    	return this.ownerID;
    }

    /**
     * Sets the owner of the channel to the user with the provided ID
	 * @param ownerID The user ID of the new channel owner
	 */
	protected void setOwnerID(int ownerID) {
		this.ownerID = ownerID;
		this.flushRequired = true;
	}

	public int getUserCount() {
        return this.users.size();
    }

    /**
     * Checks whether the user has the specified permission
     * @param user The user to check
     * @param permission The permission to check
     * @return True if the user holds the permission, false otherwise
     */
    public boolean userHasPermission (ChannelUser user, ChannelPermission permission) {
    	return getUserGroup(user).hasPermission(permission);
    }
    
    protected ChannelGroup getUserGroup (ChannelUser user) {
    	return getUserGroup(user.getId());
    }
    
    protected ChannelGroup getGroup (int groupID) {
    	return groups.get(groupID);
    }
    
    protected ChannelGroup getUserGroup (int userID) {
    	ChannelGroup group = groups.get(ChannelGroup.GUEST_GROUP);//Guest
    	if (userID == ownerID) {
    		group = groups.get(ChannelGroup.OWNER_GROUP);
    	} else if (members.containsKey(userID)) {
        	group = groups.get(members.get(userID));//Manually selected group
        } else if (permBans.contains(userID)) {
        	group = ChannelGroup.UNKNOWN_GROUP;//Permanently banned users
        }
    	return group;
    }
    
    /**
     * Gets the name of a channel group
     * @param groupID The ID of the group
     * @return The group name
     */
    public String getGroupName (int groupID) {
    	if (groups.containsKey(groupID)) {
    		return groups.get(groupID).getName();
    	} else {
    		throw new IllegalArgumentException("The requested group does not exist.");
    	}
    }

    /**
     * Returns the rank held by the user within this channel.<br />
     * NOTE: This method is deprecated. Use {@link #getUserGroup(ChannelUser)} instead.
     * @param user The user to find the rank of.
     * @return The rank the user holds in the channel (0 for guest).
     */
    @Deprecated
    public int getUserRank (ChannelUser user) {
        return getUserRank(user.getId());
    }

    /**
     * Returns the rank held by the user within this channel.<br />
     * NOTE: This method is deprecated. Use {@link #getUserGroup(int)} instead.
     * @param userID The ID of the user to find the rank of.
     * @return The rank the user holds in the channel (0 for guest).
     */
    @Deprecated
    public int getUserRank (int userID) {
    	int rank = 0;
    	if (userID == ownerID) {
    		rank = ChannelGroup.OWNER_GROUP;
    	} else if (members.containsKey(userID)) {
        	rank = members.get(userID);
        } else if (permBans.contains(userID)) {
        	rank = -3;
        }
        return rank;
    }
    
    public boolean canActionUser (ChannelUser user, int targetId) {
    	return canActionGroup(user, getUserGroup(targetId));
    }
    
    public boolean isUserMember (int userID) {
        return members.containsKey(userID) || userID == ownerID;
    }  
    
    public boolean isUserBanned (int userID) {
        return permBans.contains(userID);
    }    
    
    public boolean canAssignGroup (ChannelUser user, ChannelGroup group) {
    	if (group.getType() == ChannelGroupType.GUEST) {
    		return false;
    	}
    	if (group.getType() == ChannelGroupType.OWNER) {
    		return false;
    	}
    	return canActionGroup(user, group);
    }
    
    public boolean canActionGroup (ChannelUser user, ChannelGroup group) {
    	ChannelGroupType userGroupType = getUserGroup(user).getType();
    	if (group.getType() == ChannelGroupType.OWNER && userGroupType == ChannelGroupType.OWNER) {
    		//Owner can action their own group
    		return true;
    	}
    	return userGroupType.getLevel() > group.getType().getLevel();
    }
    
    private Map<String, Serializable> loadAttributes (Map<String, String> attributeData) {
    	Map<String, Serializable> attributes = new HashMap<>();
    	for (Map.Entry<String, String> entry : attributeData.entrySet()) {
    		attributes.put(entry.getKey(), entry.getValue());
    	}
    	return attributes;
    }
    
    //Loading stages
    private Map<Integer, ChannelGroup> loadGroups (Collection<ChannelGroupData> groupData) {
    	
    	Map<Integer, ChannelGroup> responseGroups = new HashMap<>(ChannelGroup.defaultGroups);    	
    	
    	for (ChannelGroupData rawGroupData : groupData) {
    		//Loop through each group found, placing them in the responseGroups variable
    		
    		//Temporary method, whereby group data overrides the existing groups. This will be used until group adding/removing/modifying is implemented
    		responseGroups.put(rawGroupData.getGroupID(), new ChannelGroup(rawGroupData));
    	}
    	
		return responseGroups;    	
    }

    /**
     * Fetches the members of this channel from the persistence layer and returns them as a map.
     * Also validates the member list as follows:
     * <ul>
     * <li>If a member is on the ban list, remove them from the member list.</li>
     * <li>If a member belongs to an invalid group, add them to the default group.</li>
     * <li>If a member is assigned as owner, but is not the owner (designated by ownerID), add them to the default group.</li>
     * <li>If the owner designated by ownerID is not in the member list (or is not assigned to the owner group), add them.</li>
     * </ul>
     * 
     * @return a {@link Map} matching the user IDs of channel members to their groups
     * @throws IOException If an error occurs while loading the members of the channel
     */
    protected Map<Integer, Integer> loadMembers() throws IOException {
    	Map<Integer, Integer> members = io.getChannelMembers(id);
        for (Entry<Integer, Integer> member : members.entrySet()) {
        	//Run through the members, removing any invalid sets
        	if (permBans.contains(member.getKey())) {
        		logger.warn("User "+member.getKey()+" from channel "+id+" is on the ban list. Removing from member list.");
        		
        		members.remove(member.getKey());
        		io.removeMember(id, member.getKey());
        	} else if (!groups.containsKey(member.getValue())) {
        		//Member was found to belong to an invalid group. Convert to the default group.
        		logger.warn("User "+member.getKey()+" from channel "+id+" belongs to an invalid group of "+member.getValue()+"." +
        				" Swapping to the default group of: "+ChannelGroup.DEFAULT_GROUP+".");
        		
        		member.setValue(ChannelGroup.DEFAULT_GROUP);
        		io.updateMember(id, member.getKey(), ChannelGroup.DEFAULT_GROUP);
        	} else if (member.getValue() == ChannelGroup.OWNER_GROUP && member.getKey() != ownerID) {
        		logger.warn("User "+member.getKey()+" from channel "+id+" belongs to the owner group, but is not specified as the channel owner." +
        				" Swapping to the default group of: "+ChannelGroup.DEFAULT_GROUP+".");
        		
        		member.setValue(ChannelGroup.DEFAULT_GROUP);
        		io.updateMember(id, member.getKey(), ChannelGroup.DEFAULT_GROUP);
        	}
        }
        if (ownerID != 0 && (!members.containsKey(ownerID) || members.get(ownerID) != ChannelGroup.OWNER_GROUP)) {
        	logger.warn("Channel "+id+" owner "+ownerID+" is not in the member list; adding.");
        	members.put(ownerID, ChannelGroup.OWNER_GROUP);
        }
    	logger.info(members.size() + " member(s) found for this channel.");
    	return new HashMap<>(members);
    }
    
    private Set<Integer> loadBanList () throws IOException {
    	return new HashSet<Integer>(io.getChannelBans(id));//Load the bans from the back-end
    	
    	/*logger.info(bans.size() + " permanent ban(s) found for this channel.");
    	Set<Integer> banSet = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
    	banSet.addAll(bans);
    	return banSet;//Make a concurrent version*/
    }

    //Saving stages
    protected ChannelDetails getChannelDetails () {    	
    	return new ChannelDetails(id, null, name, alias, ownerID);
    }
    
    //Alter temporary data
    protected void setTempBan(int userID, long timeMillis) {
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

    protected void addUser(ChannelUser user) {
    	users.add(user);
    }

    protected void removeUser(ChannelUser user) {
    	users.remove(user);
    }
    
    /**
     * Adds the specified message to the recent messages cache. 
     * If the cache is currently full (above the maximum size), removes the oldest message from the cache.
     * 
     * @param message the message to add to the cache.
     */
    protected void addToMessageCache (MessagePayload message) {
    	synchronized (messageCache) {
    		messageCache.put(System.currentTimeMillis(), message);
    	}
    }
	
    /**
     * Sets a channel attribute to the given value. If the attribute does not exist, add it.<br />
     * NOTE: This method does <em>not</em> verify that the key and value are valid - this <em>must</em> be done externally to prevent issues.<br />
     * Keys must conform to the following constraints:
     * <ul>
     * <li>Between 2 and 100 characters</li>
     * <li>Only contain alpha-numeric characters, underscores, hyphens, or dots.</li>
     * </ul>
     * Values must be less than 2^16 characters in length
     * @param key The attribute key
     * @param value The attribute value
     * @return True if the attribute was set successfully, false if it could not be set due to an error at the persistence layer.
     */
    protected boolean setAttribute (String key, Serializable value) {
		synchronized (attributes) {
			try {
				if (attributes.containsKey(key)) {
					io.updateAttribute(id, key, value.toString());
				} else {
					io.addAttribute(id, key, value.toString());
				}
			} catch (IOException ex) {
				logger.error("Failed to set attribute "+key+" to "+value, ex);
				return false;
			}
			attributes.put(key, value);
			return true;
		}
	}
    
    /**
     * Adds the user to the channel's member list.
     * If the request to add the member in the persistence layer fails, no changes will be made to the channel.
     * @param userID The user ID of the member to add
     * @param groupId The desired group of the member to add
     * @return true if the member was added, false if the member could not be added (either due to an error, because the user was already added, or the user is banned).
     */
    protected boolean addMember(int userID, int groupId) {
    	synchronized (members) {//Make sure only one thread is trying to modify rank data at a time
	        if (members.containsKey(userID)) {
	            return false;
	        }
	        if (permBans.contains(userID)) {
	            return false;
	        }
	        try {
				io.addMember(id, userID, groupId);
			} catch (IOException ex) {
				logger.error("Failed to add member "+userID, ex);
				return false;
			}
	        members.put(userID, groupId);
    	}
        return true;
    }

    /**
     * Changes the group the specified user belongs to. 
     * This method does not verify that the group exists or whether the user can be assigned to this group; these must be checked separately.
     * If the request to remove the member from the persistence layer fails, no changes will be made to the channel.
     * @param userID The user ID of the member to update
     * @param groupID The ID of the group to change to
     * @return true if the member was updated, false if the member could not be updated (either due to an error or because the member does not exist).
     */
    protected boolean setMemberGroup(int userID, int groupID) {
    	synchronized (members) {//Make sure only one thread is trying to modify member data at a time
	        if (!members.containsKey(userID)) {
	            return false;
	        }
	        try {
				io.updateMember(id, userID, groupID);
			} catch (IOException ex) {
				logger.error("Failed to update member group for "+userID, ex);
				return false;
			}
	        members.put(userID, groupID);
    	}
        return true;
    }

    /**
     * Removes the user as a member of the channel. Also sends this updates the persistence layer.
     * If the request to remove the member from the persistence layer fails, no changes will be made to the channel.
     * @param userID The user ID of the member to remove
     * @return true if the member was removed, false if the member could not be removed (either due to an error or because the member was already removed).
     */
    protected boolean removeMember(int userID) {
    	synchronized (members) {//Make sure only one thread is trying to modify member data at a time
	        if (!members.containsKey(userID)) {
	            return false;
	        }
	        try {
				io.removeMember(id, userID);
			} catch (IOException ex) {
				logger.error("Failed to remove member "+userID, ex);
				return false;
			}
	        members.remove(userID);
    	}
        return true;
    }
    
    /**
     * Adds the user to the channel's ban list.
     * If the request to add the ban in the persistence layer fails, no changes will be made to the channel.
     * @param userID The user ID of the ban to add
     * @return true if the ban was added, false if the ban could not be added (either due to an error or because the user is already banned).
     */
    protected boolean addBan (int userID) {
    	synchronized (permBans) {//Make sure only one thread is trying to modify permanent bans at a time
	        if (permBans.contains(userID)) {
	            return false;
	        }
	        try {
				io.addBan(id, userID);
			} catch (IOException ex) {
				logger.error("Failed to add ban "+userID, ex);
				return false;
			}
	        permBans.add(userID);
    	}
        return true;
    }
    
    /**
     * Removes the user from the channel's ban list.
     * If the request to remove the ban in the persistence layer fails, no changes will be made to the channel.
     * @param userID The user ID of the ban to remove
     * @return true if the ban was removed, false if the ban could not be removed (either due to an error or because the user is not banned).
     */
    protected boolean removeBan (int userID) {
    	synchronized (permBans) {//Make sure only one thread is trying to modify permanent bans at a time
		    if (!permBans.contains(userID)) {
		        return false;
		    }
		    try {
				io.removeBan(id, userID);
			} catch (IOException ex) {
				logger.error("Failed to remove ban "+userID, ex);
				return false;
			}
		    permBans.remove(userID);
    	}
        return true;
    }

    
    //Retrieve channel data
    protected Set<ChannelUser> getUsers() {
        return this.users;
    }

    /**
     * Gets an unmodifiable map containing the user IDs of all users who are members in the channel, and the IDs of the groups they belong to.
     * NOTE: The map returned by this method is <em>not thread safe</em>, so external synchronisation is required. 
     * @return An unmodifiable map containing user IDs as keys and group IDs as values
     */
    protected Map<Integer, Integer> getMembers() {
        return Collections.unmodifiableMap(this.members);
    }
    
    protected Set<Integer> getBans() {
    	return this.permBans;
    }
    
    /**
     * Gets an unmodifiable map containing the user IDs of all users who are temporarily banned from the channel, and the time their bans will be lifted.
     * NOTE: The map returned by this method is <em>not thread safe</em>, so external synchronisation is required. 
     * @return An unmodifiable map containing user IDs as keys and ban expire times as values
     */
    protected Map<Integer, Long> getTempBans () {
    	return Collections.unmodifiableMap(this.tempBans);
    }
    
    protected Map<Integer, ChannelGroup> getGroups () {
    	return Collections.unmodifiableMap(this.groups);
    }
    
    protected int getNextMessageID () {
    	this.nextMessageID++;
    	return this.nextMessageID;
    }
}
