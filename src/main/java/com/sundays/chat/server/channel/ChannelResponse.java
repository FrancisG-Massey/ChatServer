package com.sundays.chat.server.channel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ChannelResponse {
	
	private final ChannelResponseType type;
	private final Map<String, Serializable> params;
	
	public ChannelResponse(ChannelResponseType type) {
		this(type, new HashMap<String, Serializable>());
	}
	
	/**
	 * @param type
	 * @param params
	 */
	public ChannelResponse(ChannelResponseType type, Map<String, Serializable> params) {
		this.type = type;
		this.params = params;
	}
	
	/**
	 * @return The response type
	 */
	public ChannelResponseType getType() {
		return type;
	}
	
	/**
	 * @return The message paramaters
	 */
	public Map<String, Serializable> getParams() {
		return params;
	}
	
	

}
