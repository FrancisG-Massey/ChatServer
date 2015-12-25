package com.sundays.chat.server.channel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ChannelResponse {
	
	private final ChannelResponseType type;
	private final String messageTemplate;
	private final Map<String, Serializable> params;
	
	public ChannelResponse(ChannelResponseType type, String messageTemplate) {
		this(type, messageTemplate, new HashMap<String, Serializable>());
	}
	
	/**
	 * @param type
	 * @param messageTemplate
	 * @param params
	 */
	public ChannelResponse(ChannelResponseType type, String messageTemplate, Map<String, Serializable> params) {
		this.type = type;
		this.messageTemplate = messageTemplate;
		this.params = params;
	}
	
	/**
	 * @return The response type
	 */
	public ChannelResponseType getType() {
		return type;
	}
	
	/**
	 * @return The name of the message template to use when displaying the error message
	 */
	public String getMessageTemplate() {
		return messageTemplate;
	}
	
	/**
	 * @return The message paramaters
	 */
	public Map<String, Serializable> getParams() {
		return params;
	}
	
	

}
