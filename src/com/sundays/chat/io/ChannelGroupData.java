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
package com.sundays.chat.io;

public class ChannelGroupData {
	
	public final int groupID, channelID;
	public String groupName;
	public String permissions;
	public String type;

	public String groupIconUrl;
	public int overrides;
	
	public ChannelGroupData (int channelID, int groupID, String groupName, String permissions, 
			String type, String groupIconUrl) {
		this.channelID = channelID;
		this.groupID = groupID;
		this.groupName = groupName;
		this.permissions = permissions;
		this.type = type;
		this.groupIconUrl = groupIconUrl;
	}
	
	public ChannelGroupData overrides (int overrides) {
		this.overrides = overrides;
		return this;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ChannelGroupData [groupID=" + groupID + ", channelID="
				+ channelID + ", groupName=" + groupName + ", permissions="
				+ permissions + ", type=" + type + ", groupIconUrl="
				+ groupIconUrl + ", overrides=" + overrides + "]";
	}
}
