
package com.sundays.chat.server.channel;

import java.util.UUID;

/**
 * 
 * @author Francis
 */
public class ChannelNotFoundException extends RuntimeException {
	private static final long serialVersionUID = -6753183338129646079L;

	public ChannelNotFoundException() {
		
	}

	public ChannelNotFoundException(String message) {
		super(message);
	}
	
	public ChannelNotFoundException(int id) {
		super("Channel not found with internal ID: "+Integer.toString(id));
	}
	
	public ChannelNotFoundException(UUID id) {
		super("Channel not found with UUID: "+id);
	}
}
