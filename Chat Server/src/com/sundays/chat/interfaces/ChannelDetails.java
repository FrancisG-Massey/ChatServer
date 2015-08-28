/*******************************************************************************
 * Copyright (c) 2013 Francis G.
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
 ******************************************************************************/
package com.sundays.chat.interfaces;

import java.util.Arrays;
import java.util.Map;

/**
 * This class is used in conjunction with the {@link ChannelDataSync} in order to pass channel details between the ChatServer module and the specified back-end
 */	
public class ChannelDetails {
	
	public static final int VERSION = 4;//The interface version
	public static final int MIN_VERSION = 3;//The minimum supported version
	
	//Basic string details
	public String channelName, openingMessage, channelAbbreviation;
	public final int channelID;//Channel ID	
	public Integer[] permissions;//Channel permissions
	public Map<Integer, String> rankNames;//Channel rank names (these replace the default "Rank One", "Rank Two", etc)
	public boolean trackMessages;
	public int channelOwner;
	
	public ChannelDetails (int channelID, String channelName, String openingMessage, 
			String channelAbbreviation, Integer[] permissions, Map<Integer, String> rankNames, 
			boolean trackMessages, int channelOwner) {
		//Full constructor used when data can be added all at once
		this.channelID = channelID;
		this.channelName = channelName;
		this.openingMessage = openingMessage;
		this.channelAbbreviation = channelAbbreviation;
		this.permissions = permissions;
		this.rankNames = rankNames;
		this.trackMessages = trackMessages;
		this.channelOwner = channelOwner;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ChannelDetails [channelName=" + channelName
				+ ", openingMessage=" + openingMessage
				+ ", channelAbbreviation=" + channelAbbreviation
				+ ", channelID=" + channelID + ", permissions="
				+ Arrays.toString(permissions) + ", rankNames=" + rankNames
				+ ", trackMessages=" + trackMessages + ", channelOwner="
				+ channelOwner + "]";
	}
}
