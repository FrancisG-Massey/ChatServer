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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.sundays.chat.io.ChannelDataIO;
import com.sundays.chat.io.ChannelDetails;
import com.sundays.chat.io.ChannelGroupData;
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
    private String welcomeMessage = "Not in Channel";
    private final Map<Integer, Integer> members;
    private final Set<Integer> permBans;
    private boolean trackMessages = false;
    private final Map<Integer, ChannelGroup> groups;
    
    /*Instanced data*/
    private transient final Map<Integer, Long> tempBans = new ConcurrentHashMap<Integer, Long>();
    private transient final Set<User> users = Collections.newSetFromMap(new ConcurrentHashMap<User, Boolean>());
    private transient final LinkedList<MessagePayload> messageCache = new LinkedList<>();
    //Contains a specified number of recent messages from the channel (global system and normal messages)
    protected boolean unloadInitialised = false;
    protected boolean resetLaunched = false;
    protected boolean flushRequired = false;
    private int nextMessageID = 1;
    private int lockRank = -100;
    private int ownerID;
    private long lockExpires = 0L;
    private transient final ChannelDataIO io;
    
    /**
     * 
     * @param id The ID for the new channel
     * @param io The IO manager for saving channel data.
     */
    protected Channel (int id, ChannelDataIO io) {
    	this.id = id;
        this.io = io;
    	this.groups = loadGroups(new HashSet<ChannelGroupData>());
    	this.permBans = Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>());
    	this.members = new ConcurrentHashMap<>();
    }

    protected Channel(int id, ChannelDetails details, ChannelDataIO io) throws IOException {
        this.id = id;
        this.io = io;
        this.name = details.getName();
        this.ownerID = details.getOwner();
        this.welcomeMessage = details.getWelcomeMessage();
        this.channelAbbr = details.getAlias();
        this.trackMessages = details.isTrackMessages();
        this.groups = loadGroups(io.getChannelGroups(id));
        this.permBans = loadBanList();//Bans MUST be loaded before ranks. This ensures that people on the ban list take priority over people on the rank list
        this.members = loadMembers();
        logger.info("Successfully loaded channel: " + this.name);
    }
        
    /**
     * Gets the ID for this channel
	 * @return the channel ID
	 */
	public int getID() {
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
	 * Gets the message presented to users when joining the channel
	 * @return The welcome message
	 */
	public String getWelcomeMessage() {
        return this.welcomeMessage;
    }

	/**
	 * Sets the message presented to users when joining the channel to the provided string
	 * @param welcomeMessage The new opening message for the channel.
	 */
	protected void setWelcomeMessage(String welcomeMessage) {
		this.welcomeMessage = welcomeMessage;
		this.flushRequired = true;
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
    public boolean userHasPermission (User user, ChannelPermission permission) {
    	return getUserGroup(user).hasPermission(permission);
    }
    
    protected ChannelGroup getUserGroup (User u) {
    	return getUserGroup(u.getUserID());
    }
    
    protected ChannelGroup getGroup (int groupID) {
    	return groups.get(groupID);
    }
    
    protected ChannelGroup getUserGroup (int userID) {
    	ChannelGroup group = groups.get(ChannelGroup.GUEST_GROUP);//Guest
    	if (userID == ownerID) {
    		group = groups.get(ChannelGroup.OWNER_GROUP);
    	} else if (members.containsKey(userID)) {
        	group = groups.get(members.get(userID).intValue());//Manually selected group
        } else if (permBans.contains(userID)) {
        	group = new ChannelGroup(Settings.systemGroups.get(53));//Permanently banned users
        }
    	return (group==null?new ChannelGroup(Settings.systemGroups.get(53)):group);
    	//Returns the "Unknown" group if no other group was found
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
     * NOTE: This method is deprecated. Use {@link #getUserGroup(User)} instead.
     * @param user The user to find the rank of.
     * @return The rank the user holds in the channel (0 for guest).
     */
    @Deprecated
    public int getUserRank (User user) {
        return getUserRank(user.getUserID());
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
    
    public boolean isUserBanned (int uID) {
        return permBans.contains(uID);
    }    
    
    //Loading stages
    private Map<Integer, ChannelGroup> loadGroups (Set<ChannelGroupData> groupData) {
    	
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
    private Map<Integer, Integer> loadMembers() throws IOException {
    	Map<Integer, Integer> members = io.getChannelMembers(id);
        for (Entry<Integer, Integer> member : members.entrySet()) {
        	//Run through the members, removing any invalid sets
        	if (permBans.contains(member.getKey())) {
        		logger.warn("User "+member.getKey()+" from channel "+id+" is on the ban list. Removing from member list.");
        		
        		members.remove(member.getKey());
        		io.removeMember(id, member.getKey());
        	} else if (member.getValue() < 1 || member.getValue() > ChannelGroup.TOTAL_RANKS) {
        		//Rank was found to contain an invalid value. Convert to the default rank.
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
        if (!members.containsKey(ownerID) || members.get(ownerID) != ChannelGroup.OWNER_GROUP) {
        	logger.warn("Channel "+id+" owner "+ownerID+" is not in the member list; adding.");
        	members.put(ownerID, ChannelGroup.OWNER_GROUP);
        }
    	logger.info(members.size() + " member(s) found for this channel.");
    	return new HashMap<>(members);
    }
    
    private Set<Integer> loadBanList () throws IOException {
    	Set<Integer> bans = io.getChannelBans(id);//Load the bans from the back-end
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

    //Saving stages
    protected ChannelDetails getChannelDetails () {    	
    	return new ChannelDetails(id, name, welcomeMessage,
    			channelAbbr, trackMessages, ownerID);
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
        if (!users.contains(u)) {
            users.add(u);
        }
    }

    protected void removeUser(User u) {
        if (users.contains(u)) {
            users.remove(u);
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

    protected void setOpeningMessage(String message, Color c) {
        this.welcomeMessage = message;
        this.openingMessageColour = c;
        this.flushRequired = true;//Notifies the auto-save thread that the channel data requires flushing
    }
    
    /**
     * Adds the user to the channel's member list.
     * If the request to add the member in the persistence layer fails, no changes will be made to the channel.
     * @param userID The user ID of the member to add
     * @return true if the member was added, false if the member could not be added (either due to an error, because the user was already added, or the user is banned).
     */
    protected boolean addMember(int userID) {
    	synchronized (members) {//Make sure only one thread is trying to modify rank data at a time
	        if (members.containsKey(userID)) {
	            return false;
	        }
	        if (permBans.contains(userID)) {
	            return false;
	        }
	        try {
				io.addMember(id, userID, ChannelGroup.DEFAULT_GROUP);
			} catch (IOException ex) {
				logger.error("Failed to add member "+userID, ex);
				return false;
			}
	        members.put(userID, ChannelGroup.DEFAULT_GROUP);
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
    
    protected boolean addBan (Integer uID) {
    	synchronized (permBans) {//Make sure only one thread is trying to modify permanent bans at a time
	        if (permBans.contains(uID)) {
	            return false;
	        }        
	        try {
				io.addBan(id, uID);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        permBans.add(uID);
    	}
        return true;
    }
    
    protected boolean removeBan (Integer uID) {
    	synchronized (permBans) {//Make sure only one thread is trying to modify permanent bans at a time
		    if (!permBans.contains(uID)) {
		        return false;
		    }
		    try {
				io.removeBan(id, uID);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		    permBans.remove(uID);
    	}
        return true;
    }

    
    //Retrieve channel data
    protected Set<User> getUsers() {
        return Collections.unmodifiableSet(this.users);
    }

    protected Map<Integer, Integer> getMembers() {
        return Collections.unmodifiableMap(this.members);
    }
    
    protected Set<Integer> getBans() {
    	return Collections.unmodifiableSet(this.permBans);
    }
    
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
