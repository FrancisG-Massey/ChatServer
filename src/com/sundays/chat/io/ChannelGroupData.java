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
package com.sundays.chat.io;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ChannelGroupData {
	
	private final int groupID;
	private final int channelID;
	private boolean overridesDefault;
	private String name;
	private ChannelGroupType type;
	private String description;

	public String groupIconUrl;
	
	private Set<String> permissions;
	
	public ChannelGroupData(int groupID, int channelID) {
		this.groupID = groupID;
		this.channelID = channelID;
	}
	
	public ChannelGroupData (int channelID, int groupID, String groupName, String[] permissions, 
			ChannelGroupType type, String groupIconUrl) {
		this(channelID, groupID, groupName, new HashSet<>(Arrays.asList(permissions)), type, groupIconUrl);
	}
	
	public ChannelGroupData (int channelID, int groupID, String groupName, Set<String> permissions, 
			ChannelGroupType type, String groupIconUrl) {
		this.channelID = channelID;
		this.groupID = groupID;
		this.name = groupName;
		this.permissions = permissions;
		this.type = type;
		this.groupIconUrl = groupIconUrl;
	}

	/**
	 * Gets the ID for this channel group. If the group overrides a default group, the ID will be the same as the default.
	 * 
	 * @return The ID for the group.
	 */
	public int getGroupID() {
		return groupID;
	}

	/**
	 * @return the channelID
	 */
	public int getChannelID() {
		return channelID;
	}

	/**
	 * @return the overridesDefault
	 */
	public boolean overridesDefault() {
		return overridesDefault;
	}

	/**
	 * @param overridesDefault the overridesDefault to set
	 */
	public void setOverridesDefault(boolean overridesDefault) {
		this.overridesDefault = overridesDefault;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the type
	 */
	public ChannelGroupType getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(ChannelGroupType type) {
		this.type = type;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the permissions
	 */
	public Set<String> getPermissions() {
		return permissions;
	}

	/**
	 * @param permissions the permissions to set
	 */
	public void setPermissions(Set<String> permissions) {
		this.permissions = permissions;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ChannelGroupData [groupID=" + groupID + ", channelID=" + channelID + ", overridesDefault=" + overridesDefault + ", name="
				+ name + ", type=" + type + ", description=" + description + ", groupIconUrl=" + groupIconUrl + ", permissions="
				+ permissions + "]";
	}
}
