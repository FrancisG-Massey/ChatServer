/*******************************************************************************
 * Copyright (c) 2015 Francis G.
 *
 * This file is part of ChatServer.
 *
 * ChatServer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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

import org.json.JSONObject;

public class ChannelJSONRequestManager {
	
	public enum Parameters {
		JOIN ("join", ""),
		LEAVE ("leave", ""), 
		GETMESSAGES ("messages", "get");
		
		private final String p1, p2;
		Parameters (String p1, String p2) {
			this.p1 = p1;
			this.p2 = p2;
		}
		
		public String parameter1 () {
			return this.p1;
		}
		
		public String parameter2 () {
			return this.p2;
		}
	}
	
	public static Parameters stringToParameter (String p1, String p2) {
		return Parameters.JOIN;		
	}
	
	protected static JSONObject manageBasicRequest (JSONObject request, Parameters section) {
		JSONObject response = new JSONObject();
		switch (section) {
		case JOIN:
			
			break;
		case LEAVE:
			
			break;
		default:
			
			break;
		}
		return response;
	}
}
