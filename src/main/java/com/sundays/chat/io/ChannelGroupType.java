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
	NORMAL("normal"),
	MODERATOR("moderator"),
	ADMINISTRATOR("admin"),
	OWNER("owner"),
	SYSTEM("system");
	
	private final String simpleName;

	ChannelGroupType (String name) {
		this.simpleName = name;
	}

	public String getSimpleName() {
		return simpleName;
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