package com.sundays.chat.server.channel;

import java.util.HashMap;
import java.util.Map;

public enum ChannelAttributeType {
	WELCOME_MESSAGE("welcomeMessage");
	
	private String name;
	
	ChannelAttributeType (String name) {
		this.name = name;
	}
	
	private static Map<String, ChannelAttributeType> lookupMap;
	
	public static ChannelAttributeType getByName (String name) {
		if (lookupMap == null) {
			lookupMap = new HashMap<>();
			for (ChannelAttributeType attr : values()) {
				lookupMap.put(attr.name, attr);
			}
		}
		return lookupMap.get(name);
	}
}
