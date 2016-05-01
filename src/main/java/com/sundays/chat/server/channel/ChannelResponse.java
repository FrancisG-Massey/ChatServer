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
