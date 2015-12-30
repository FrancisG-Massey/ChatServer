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

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sundays.chat.io.ChannelGroupData;
import com.sundays.chat.io.ChannelGroupType;

public class ChannelGroup {

	private static final Logger logger = LoggerFactory.getLogger(ChannelGroup.class);
	
    public static final Map<Integer, ChannelGroup> defaultGroups = new HashMap<>(12);
    
    public static final ChannelGroup UNKNOWN_GROUP = new ChannelGroup(50, -2, "Unknown", ChannelGroupType.NORMAL);
	
	public static final int DEFAULT_GROUP = 1;//The group that all users will be automatically assigned when they are added as a channel member
    public static final int GUEST_GROUP = 0;//The group that any users who are not on the channel's member list will receive
    public static final int MOD_GROUP = 5;//A system group for the position of 'channel moderator'. This holds moderative permissions by default, but can be changed in each channel
    public static final int ADMIN_GROUP = 9;//A system group for the position of 'channel administrator'. Holds administrative permissions by default, but this can be changed in each channel
    public static final int OWNER_GROUP = 11;//The highest channel-specific group available. Can only be held by a single person at a time, and holds all available permissions.
    
    public static final int TOTAL_RANKS;
    
    static {       
    	defaultGroups.clear();
    	defaultGroups.put(GUEST_GROUP, new ChannelGroup(-1, GUEST_GROUP, "Guest", ChannelGroupType.GUEST));
    	defaultGroups.put(DEFAULT_GROUP, new ChannelGroup(-1, DEFAULT_GROUP, "Rank one", ChannelGroupType.NORMAL));
    	defaultGroups.put(2, new ChannelGroup(-1, 2, "Rank two", ChannelGroupType.NORMAL));
        defaultGroups.put(3, new ChannelGroup(-1, 3, "Rank three", ChannelGroupType.NORMAL));
        defaultGroups.put(4, new ChannelGroup(-1, 4, "Rank four", ChannelGroupType.NORMAL));
        defaultGroups.put(MOD_GROUP, new ChannelGroup(-1, MOD_GROUP, "Moderator", ChannelGroupType.MODERATOR));
        defaultGroups.put(6, new ChannelGroup(-1, 6, "Rank six", ChannelGroupType.MODERATOR));
        defaultGroups.put(7, new ChannelGroup(-1, 7, "Rank seven", ChannelGroupType.MODERATOR));
        defaultGroups.put(8, new ChannelGroup(-1, 8, "Rank eight", ChannelGroupType.MODERATOR));
        defaultGroups.put(ADMIN_GROUP, new ChannelGroup(-1, ADMIN_GROUP, "Administrator", ChannelGroupType.ADMINISTRATOR));
        defaultGroups.put(10, new ChannelGroup(-1, 10, "Rank ten", ChannelGroupType.ADMINISTRATOR));
        defaultGroups.put(OWNER_GROUP, new ChannelGroup(-1, OWNER_GROUP, "Owner", ChannelGroupType.OWNER));
		TOTAL_RANKS = (short) defaultGroups.size();
    }
	
	
	private final Set<ChannelPermission> permissions = EnumSet.noneOf(ChannelPermission.class);
	private String name;
	private String iconUrl;
	private final int id;
	private final int channelID;
	private ChannelGroupType type = ChannelGroupType.NORMAL;
	
	public ChannelGroup (int channelID, int groupID) {
		this.id = groupID;
		this.channelID = channelID;
	}
	
	public ChannelGroup (int channelID, int groupID, String name, ChannelGroupType type) {
		this.id = groupID;
		this.channelID = channelID;
		this.name = name;
		this.type = type;
	}
	
	public ChannelGroup (ChannelGroupData data) {
		//Fixed data fields
		this.id = data.getGroupID();
		this.channelID = data.getChannelID();
		
		//Variable simple fields
		this.name = data.getName();
		this.iconUrl = data.groupIconUrl;
		this.type = data.getType();
		
		//Group permissions
		boolean hasAllPermissions = false;
		for (String permissionName : data.getPermissions()) {
			ChannelPermission permission = ChannelPermission.getByName(permissionName.trim());
			if (permission == null) {
				logger.warn("Invalid permission: "+permissionName+".");
			}
			if (permission == ChannelPermission.ALL) {
				permissions.addAll(EnumSet.allOf(ChannelPermission.class));
				hasAllPermissions = true;
				break;
			}
			permissions.add(permission);		
		}
		for (ChannelPermission permission : permissions) {
			if (!permission.canHold(type)) {
				permissions.remove(permission);
				if (!hasAllPermissions) {
					logger.warn("Group "+id+" (of channel "+channelID+") cannot hold permission: "+permission.toString()+". It has been removed from the group.");
				}				
			}
		}
	}
	
	public ChannelGroup (ChannelGroup oldGroup) {
		this.id = oldGroup.id;
		this.channelID = oldGroup.channelID;
		this.name = oldGroup.name;
		this.iconUrl = oldGroup.iconUrl;
		this.type = oldGroup.type;
	}

	public int getId() {
		return id;
	}

	public String getName () {
		return this.name;
	}

	public ChannelGroupType getType() {
		return type;
	}
	
	public String getIconUrl () {
		return this.iconUrl;
	}
	
	public boolean hasPermission (ChannelPermission permission) {
		return permissions.contains(permission);
	}
	
	public Set<ChannelPermission> getPermissions () {
		return Collections.unmodifiableSet(permissions);
	}
	
	protected void setName (String newName) {
		this.name = newName;
	}
	
	protected void setIconUrl (String iconUrl) {
		this.iconUrl = iconUrl;
	}
	
	protected void setType(ChannelGroupType type) {
		this.type = type;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ChannelGroup [permissions=" + permissions + ", name=" + name + ", id=" + id + ", type=" + type + "]";
	}
	
	
}
