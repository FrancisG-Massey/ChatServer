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

import java.util.Arrays;
import java.util.Map;

/**
 * Used to communicate channel details between the persistence layer and the application layer.
 * 
 * @author Francis
 */
public class ChannelDetails {
	
	public static final int VERSION = 4;//The interface version
	public static final int MIN_VERSION = 3;//The minimum supported version
	
	//Basic string details
	public String name, openingMessage, abbreviation;
	public int id;//Channel ID	
	public Integer[] permissions;//Channel permissions
	public Map<Byte, String> rankNames;//Channel rank names (these replace the default "Rank One", "Rank Two", etc)
	public boolean trackMessages;
	public int owner;
	
	public ChannelDetails () {
		
	}
	
	public ChannelDetails (int id, String name, String openingMessage, 
			String abbreviation, Integer[] permissions, Map<Byte, String> rankNames, 
			boolean trackMessages, int owner) {
		//Full constructor used when data can be added all at once
		this.id = id;
		this.name = name;
		this.openingMessage = openingMessage;
		this.abbreviation = abbreviation;
		this.permissions = permissions;
		this.rankNames = rankNames;
		this.trackMessages = trackMessages;
		this.owner = owner;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ChannelDetails [name=" + name
				+ ", openingMessage=" + openingMessage
				+ ", abbreviation=" + abbreviation
				+ ", id=" + id + ", permissions="
				+ Arrays.toString(permissions) + ", rankNames=" + rankNames
				+ ", trackMessages=" + trackMessages + ", owner="
				+ owner + "]";
	}
}
