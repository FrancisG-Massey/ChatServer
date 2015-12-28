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

public enum ChannelGroupType {
	GUEST("guest", -1),
	NORMAL("normal", 0),
	MODERATOR("moderator", 1),
	ADMINISTRATOR("admin", 2),
	OWNER("owner", 3),
	SYSTEM("system", 4);
	
	private final String simpleName;
	
	private final int level;

	ChannelGroupType (String name, int level) {
		this.simpleName = name;
		this.level = level;
	}

	public String getSimpleName() {
		return simpleName;
	}
	
	public int getLevel () {
		return level;
	}
	
	public static ChannelGroupType getByName (String name) {
		for (ChannelGroupType groupType : values()) {
			if (groupType.simpleName.equalsIgnoreCase(name)) {
				return groupType;
			}
		}
		return null;
	}
}