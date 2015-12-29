package com.sundays.chat.server.channel;

public enum ChannelAttributeType {
	/**
	 * An attribute representing information about the channel.
	 * Changing these attributes should not have any impact on the functionality of the channel.
	 */
	INFO,
	
	/**
	 * An attribute used to change the functionality of the channel.
	 */
	SETTING,
	
	/**
	 * An attribute used by the system to track the state of some channel aspect.
	 * These attributes should not be changed by any channel user; only by the application itself.
	 */
	SYSTEM;
}
