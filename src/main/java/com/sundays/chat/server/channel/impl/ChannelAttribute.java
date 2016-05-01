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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.sundays.chat.server.channel.ChannelAttributeType;

public enum ChannelAttribute {
	WELCOME_MESSAGE("welcomeMessage", ChannelAttributeType.INFO, "Welcome to the channel."),
	WELCOME_MESSAGE_COLOUR("welcomeMessage.colour", ChannelAttributeType.INFO, 0);
	
	private String name;
	private ChannelAttributeType type;
	private Serializable defaultValue;
	
	ChannelAttribute (String name, ChannelAttributeType type, Serializable defaultValue) {
		this.name = name;
		this.type = type;
		this.defaultValue = defaultValue;
	}
	
	public String getName() {
		return name;
	}

	public ChannelAttributeType getType() {
		return type;
	}
	
	public Serializable getDefaultValue() {
		return defaultValue;
	}

	private static Map<String, ChannelAttribute> lookupMap;
	
	public static ChannelAttribute getByName (String name) {
		if (lookupMap == null) {
			lookupMap = new HashMap<>();
			for (ChannelAttribute attr : values()) {
				lookupMap.put(attr.name, attr);
			}
		}
		return lookupMap.get(name);
	}
}
