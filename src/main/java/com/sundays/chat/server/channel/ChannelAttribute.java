package com.sundays.chat.server.channel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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
