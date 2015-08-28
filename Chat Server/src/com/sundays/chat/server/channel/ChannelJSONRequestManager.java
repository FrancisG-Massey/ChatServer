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
