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
import java.util.Set;

import org.apache.log4j.Logger;

import com.sundays.chat.io.ChannelGroupData;
import com.sundays.chat.io.ChannelGroupType;

public class ChannelGroup {
	
	private static final Logger logger = Logger.getLogger(ChannelGroup.class);
	
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
}
