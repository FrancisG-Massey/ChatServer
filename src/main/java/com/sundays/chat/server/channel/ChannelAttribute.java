package com.sundays.chat.server.channel;

import java.util.HashMap;
import java.util.Map;

public enum ChannelAttribute {
	WELCOME_MESSAGE("welcomeMessage", ChannelAttributeType.INFO, "Welcome to the channel."),
	WELCOME_MESSAGE_COLOUR("welcomeMessage.colour", ChannelAttributeType.INFO, 0);
	
	private String name;
	private ChannelAttributeType type;
	
	ChannelAttribute (String name, ChannelAttributeType type, Object defaultValue) {
		this.name = name;
		this.type = type;
	}
	
	public String getName() {
		return name;
	}

	public ChannelAttributeType getType() {
		return type;
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
