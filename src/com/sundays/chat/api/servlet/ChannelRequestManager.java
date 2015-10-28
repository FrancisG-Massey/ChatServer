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
package com.sundays.chat.api.servlet;

import java.awt.Color;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sundays.chat.server.Permission;
import com.sundays.chat.server.Settings;
import com.sundays.chat.server.channel.ChannelAPI;
import com.sundays.chat.server.message.MessageWrapper;
import com.sundays.chat.server.user.User;
import com.sundays.chat.utils.HttpRequestTools;

/**
 * Servlet implementation class ChannelManager
 */
public class ChannelRequestManager extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private ServletLauncher server;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ChannelRequestManager() {
        super();
    }

    @Override
    public void init (ServletConfig config) throws ServletException {
    	super.init(config);
    	server = ServletLauncher.getInstance();
    	if (!server.initalised) {
    		server.init(config);
    	}
    	/*System.out.println("Testing properties file retrieval");
    	InputStream is = config.getServletContext().getResourceAsStream("/WEB-INF/default.properties");
    	Properties p = new Properties();
    	try {
			p.load(is);
		} catch (IOException e) {
			config.getServletContext().log("Problem loading application configuration file: ", e);
		}
    	System.out.println(p.getProperty("test"));*/
    }
    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (request.getPathInfo() == null) {
			//No channel ID specified
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No channel ID specified");
			return;
		}
		String[] requestInfo = request.getPathInfo().substring(1).split("/");
		if (requestInfo.length == 0) {
			//No channel ID specified
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No channel ID specified");
			return;
		}
		int channelID = 0;
		try {
			channelID = Integer.parseInt(requestInfo[0]);
		} catch (NumberFormatException e) {
			//Channel ID is not a number
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid channel ID specified");
			return;
		}
		JSONObject responseJSON = new JSONObject();
		ChannelAPI cm = server.getChannelAPI();
		if (!server.getChannelManager().channelExists(channelID)) {
			try {
				responseJSON.put("status", HttpServletResponse.SC_BAD_REQUEST);
				responseJSON.put("message", "Channel not found.");		
				HttpRequestTools.sendResponseJSON(response, responseJSON);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		
		try {
			responseJSON = processGetRequest(requestInfo, channelID, cm, response);
		} catch (JSONException e1) {
			e1.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		
		try {
			if (responseJSON == null) {
				responseJSON = new JSONObject();
				responseJSON.put("status", HttpServletResponse.SC_NOT_FOUND);
			} else if (responseJSON.length() == 0) {
				responseJSON.put("status", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} else if (!responseJSON.has("status")) {
				responseJSON.put("status", HttpServletResponse.SC_OK);
			}				
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		HttpRequestTools.sendResponseJSON(response, responseJSON);
		return;
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (request.getPathInfo() == null) {
			//No channel ID specified
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		String[] requestInfo = request.getPathInfo().substring(1).split("/");
		if (requestInfo.length == 0) {
			//No channel ID specified
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		int channelID = 0;
		try {
			channelID = Integer.parseInt(requestInfo[0]);
		} catch (NumberFormatException e) {
			//Channel ID is not a number
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		JSONObject responseJSON = new JSONObject();
		if (!server.getChannelManager().channelExists(channelID)) {
			try {
				responseJSON.put("status", HttpServletResponse.SC_BAD_REQUEST);
				responseJSON.put("message", "Channel not found.");		
				HttpRequestTools.sendResponseJSON(response, responseJSON);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		if (requestInfo.length == 1) {
			//Request for channel details
			doGet(request, response);//Relays the request as a get request			
		} else if (request.getContentType().split(";")[0].equals("application/json")) {	
			JSONObject requestJSON = null;
			try {
				requestJSON = HttpRequestTools.getRequestJSON(request);
			} catch (JSONException e) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			}
			try {				
				User u = server.getUserManager().getUserSession(requestJSON.optString("session", null));
				if (u == null) {
					//User string does not match a valid, currently logged in user
					responseJSON.put("status", HttpServletResponse.SC_FORBIDDEN);
					responseJSON.put("message", "You must be logged in to use this method,");
				} else {
					responseJSON = processPostRequest(requestInfo, channelID, u, requestJSON, response);
				}
				if (responseJSON == null) {
					responseJSON = new JSONObject();
					responseJSON.put("status", HttpServletResponse.SC_NOT_FOUND);
				} else if (responseJSON.length() == 0) {
					responseJSON.put("status", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				} else if (!responseJSON.has("status")) {
					responseJSON.put("status", HttpServletResponse.SC_OK);
				}
				HttpRequestTools.sendResponseJSON(response, responseJSON);	
				
			} catch (JSONException e) {
				e.printStackTrace();
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return;
			}
		} else {
			response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
		}
	}
	
	private JSONObject processGetRequest (String[] requestInfo, int cID, ChannelAPI api, HttpServletResponse response) throws JSONException, IOException {
		JSONObject responseJSON = new JSONObject();
		if (requestInfo.length == 1) {
			//Request for channel details
			responseJSON = new JSONObject(api.getChannelDetails(cID));			
		} else if ("userlist".equalsIgnoreCase(requestInfo[1])) {
			//Request for channel list
			responseJSON = new JSONObject(api.getChannelList(cID));	
		} else if ("rankdetails".equalsIgnoreCase(requestInfo[1])) {
			//Request for channel rank details
			responseJSON = api.getRankNames(cID);
		} else if ("ranks".equalsIgnoreCase(requestInfo[1])) {
			//Request for channel rank list
			responseJSON = new JSONObject(api.getRankList(cID));			
		} else if ("permissions".equalsIgnoreCase(requestInfo[1])) {
			//Request for channel permissions
			responseJSON = api.getPermissions(cID);
		} else if ("bans".equalsIgnoreCase(requestInfo[1])) {
			//Request for channel permissions
			responseJSON = api.getBanList(cID);
		} else if ("groups".equalsIgnoreCase(requestInfo[1])) {
			//Request for channel groups
			responseJSON = api.getChannelGroups(cID);
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			//responseJSON.put("status", HttpServletResponse.SC_NOT_FOUND);
		}
		return responseJSON;
	}
	
	private JSONObject processPostRequest (String[] requestInfo, int cID, User u, JSONObject requestJSON, HttpServletResponse response) throws ServletException, JSONException, IOException {
		JSONObject responseJSON = new JSONObject();
		ChannelAPI api = server.getChannelAPI();
		
		if (requestInfo.length > 2) {//Process any double-parameter requests first
			if ("messages".equalsIgnoreCase(requestInfo[1]) && "send".equalsIgnoreCase(requestInfo[2])) {
					//Request to send a message in the channel
				String message = requestJSON.optString("message", null);
				if (message == null) {
					responseJSON.put("status", HttpServletResponse.SC_BAD_REQUEST);
					responseJSON.put("msgArgs", "arg=message,expected=String,found=null");
					responseJSON.put("msgCode", 177);
					responseJSON.put("message", "Invalid or missing parameter for message; expected: String, found: null."); 
				} else {
					responseJSON = api.sendMessage(u, message);
				}
			} else if ("messages".equalsIgnoreCase(requestInfo[1]) && "get".equalsIgnoreCase(requestInfo[2])) {
				//Request to collect cued messages (NOTE: This will remove everything currently in the cue)
				if (u.hasCuedMessages(cID)) {
					responseJSON.put("status", HttpServletResponse.SC_OK);
					List<MessageWrapper> messages = u.getQueuedMessages(cID, true);
					if (messages == null) {
						responseJSON.put("messages", JSONObject.NULL);
					} else {
						JSONArray packedMessages = new JSONArray();
						JSONObject jsonObject;
						for (MessageWrapper message : messages) {
							jsonObject = new JSONObject(message.getPayload());
							jsonObject.put("orderID", message.getOrderID());
							jsonObject.put("type", message.getType().getID());
							jsonObject.put("timestamp", message.getTimestamp());
							packedMessages.put(jsonObject);
						}
						responseJSON.put("messages", packedMessages);
					}
				} else {
					responseJSON.put("status", HttpServletResponse.SC_NO_CONTENT);
				}
			} else if ("permissions".equalsIgnoreCase(requestInfo[1]) && "change".equalsIgnoreCase(requestInfo[2])) {
				//Request to change channel permissions
				String permissionName = requestJSON.optString("permissionName", null);
				int value = requestJSON.optInt("value", Byte.MIN_VALUE);
				if (value == Byte.MIN_VALUE) {
					responseJSON.put("status", HttpServletResponse.SC_BAD_REQUEST);
					responseJSON.put("msgArgs", "arg=value,expected=byte,found=none");
					responseJSON.put("msgCode", 177);
					responseJSON.put("message", "Invalid or missing parameter for value; expected: byte, found: none."); 
				} else {
					Permission p;
					try {
						p = Permission.valueOf(permissionName.toUpperCase(Locale.ENGLISH));						
					} catch (IllegalArgumentException e) {
						responseJSON.put("status", 400);
			        	responseJSON.put("msgCode", 175);
			        	responseJSON.put("message", "The permission you have specified for this channel does not exist.");
			        	return responseJSON;
					} catch (NullPointerException e) {
						responseJSON.put("status", HttpServletResponse.SC_BAD_REQUEST);
						responseJSON.put("msgArgs", "arg=permissionName,expected=String,found=null");
						responseJSON.put("msgCode", 177);
						responseJSON.put("message", "Invalid or missing parameter for permissionName; expected: String, found: null."); 
			        	return responseJSON;
					}
					responseJSON = api.updatePermission(u, cID, p, value);
				}
				
			} else if ("rankdetails".equalsIgnoreCase(requestInfo[1]) && "changename".equalsIgnoreCase(requestInfo[2])) {
				//Request to change channel permissions
				int rank = requestJSON.optInt("rankID", Integer.MAX_VALUE);
				String name = requestJSON.optString("rankName", null);
				if (rank > Byte.MAX_VALUE || rank < Byte.MIN_VALUE) {
					responseJSON.put("status", HttpServletResponse.SC_BAD_REQUEST);
					responseJSON.put("msgArgs", "arg=rankID,expected=byte,found=null");
					responseJSON.put("msgCode", 177);
					responseJSON.put("message", "Invalid or missing parameter for rankID; expected: byte, found: null."); 
				} else if (name == null) {
					responseJSON.put("status", HttpServletResponse.SC_BAD_REQUEST);
					responseJSON.put("msgArgs", "arg=rankName,expected=String,found=null");
					responseJSON.put("msgCode", 177);
					responseJSON.put("message", "Invalid or missing parameter for rankName; expected: String, found: null."); 
				} else {
					responseJSON = api.changeRankName(u, cID, (byte) rank, name);
				}				
			} else if ("openingmessage".equalsIgnoreCase(requestInfo[1]) && "change".equalsIgnoreCase(requestInfo[2])) {
				//Request to change channel permissions
				String message = requestJSON.optString("message", null);
				if (message == null) {
					responseJSON.put("status", HttpServletResponse.SC_BAD_REQUEST);
					responseJSON.put("msgArgs", "arg=message,expected=String,found=null");
					responseJSON.put("msgCode", 177);
					responseJSON.put("message", "Invalid or missing parameter for message; expected: String, found: null."); 
				} else {
					responseJSON = api.chageOpeningMessage(u, cID, message, Color.black);
				}				
			} else if ("ranks".equalsIgnoreCase(requestInfo[1]) && "add".equalsIgnoreCase(requestInfo[2])) {
				//Request to add a rank to the rank list
				int uID = requestJSON.optInt("userID", 0);//Tries to extract the user ID. If no userID is found, returns 0
				if (uID == 0) {
					//If no user ID was specified, checks for a username.
					String un = requestJSON.optString("username", null);
					if (un != null) {
						//If a username was specified, attempt to resolve it to an ID
						uID = server.getUserManager().getUserID(un);
						if (uID != 0) {
							//If a userID was found, use the specified ID
							responseJSON = api.addRank(u, cID, uID);
						} else {
							responseJSON.put("status", 404);
							responseJSON.put("msgCode", 159);
							responseJSON.put("message", "The user you have attempted to rank was not found.");
						}
					} else {
						//If no userID or username was specified, send back an error message.
						responseJSON.put("status", HttpServletResponse.SC_BAD_REQUEST);
						responseJSON.put("msgArgs", "arg=userID OR username,expected=Integer OR String,found=neither");
						responseJSON.put("msgCode", 177);
						responseJSON.put("message", "Invalid or missing parameter for userID OR username; expected: Integer OR String, found: neither.");
					}
				} else {
					responseJSON = api.addRank(u, cID, uID);
				}
			} else if ("ranks".equalsIgnoreCase(requestInfo[1]) && "remove".equalsIgnoreCase(requestInfo[2])) {
				//Request to add a rank to the rank list
				int uID = requestJSON.optInt("userID", 0);
				if (uID == 0) {
					responseJSON.put("status", HttpServletResponse.SC_BAD_REQUEST);
					responseJSON.put("msgArgs", "arg=userID,expected=Integer,found=null");
					responseJSON.put("msgCode", 177);
					responseJSON.put("message", "Invalid or missing parameter for userID; expected: Integer, found: null."); 
				} else {
					responseJSON = api.removeRank(u, cID, uID);
				}					
			} else if ("ranks".equalsIgnoreCase(requestInfo[1]) && "update".equalsIgnoreCase(requestInfo[2])) {
				//Request to add a rank to the rank list
				int uID = requestJSON.optInt("userID", 0);
				int rankID = requestJSON.optInt("rankID", Integer.MAX_VALUE);
				if (uID == 0) {
					responseJSON.put("status", HttpServletResponse.SC_BAD_REQUEST);
					responseJSON.put("msgArgs", "arg=userID,expected=Integer,found=null");
					responseJSON.put("msgCode", 177);
					responseJSON.put("message", "Invalid or missing parameter for userID; expected: Integer, found: null."); 
				} else if (rankID > Byte.MAX_VALUE || rankID < Byte.MIN_VALUE) {
					responseJSON.put("status", HttpServletResponse.SC_BAD_REQUEST);
					responseJSON.put("msgArgs", "arg=rankID,expected=byte,found=null");
					responseJSON.put("msgCode", 177);
					responseJSON.put("message", "Invalid or missing parameter for rankID; expected: byte, found: null."); 
				} else {
					responseJSON = api.updateRank(u, cID, uID, (byte) rankID);
				}
				
			} else if ("bans".equalsIgnoreCase(requestInfo[1]) && "add".equalsIgnoreCase(requestInfo[2])) {
				//Request to add a rank to the rank list
				int uID = requestJSON.optInt("userID", 0);//Tries to extract the user ID. If no userID is found, returns 0
				if (uID == 0) {
					//If no user ID was specified, checks for a username.
					String un = requestJSON.optString("username", null);
					if (un != null) {
						//If a username was specified, attempt to resolve it to an ID
						uID = server.getUserManager().getUserID(un);
						if (uID != 0) {
							//If a userID was found, use the specified ID
							responseJSON = api.addBan(u, cID, uID);
						} else {
							responseJSON.put("status", 404);
							responseJSON.put("msgCode", 160);
							responseJSON.put("message", "The user you have attempted to ban was not found.");
						}
					} else {
						//If no userID or username was specified, send back an error message.
						responseJSON.put("status", HttpServletResponse.SC_BAD_REQUEST);
						responseJSON.put("msgArgs", "arg=userID OR username,expected=Integer OR String,found=neither");
						responseJSON.put("msgCode", 177);
						responseJSON.put("message", "Invalid or missing parameter for userID OR username; expected: Integer OR String, found: neither.");
					}
				} else {
					responseJSON = api.addBan(u, cID, uID);
				}
			} else if ("bans".equalsIgnoreCase(requestInfo[1]) && "remove".equalsIgnoreCase(requestInfo[2])) {
				//Request to add a rank to the rank list
				int uID = requestJSON.optInt("userID", 0);
				if (uID == 0) {
					responseJSON.put("status", HttpServletResponse.SC_BAD_REQUEST);
					responseJSON.put("msgArgs", "arg=userID,expected=Integer,found=null");
					responseJSON.put("msgCode", 177);
					responseJSON.put("message", "Invalid or missing parameter for userID; expected: Integer, found: null."); 
				} else {
					responseJSON = api.removeBan(u, cID, uID);
				}				
			} else {
				responseJSON = processGetRequest(requestInfo, cID, api, response);//Relays the request as a get request
			} 
		} else if (requestInfo.length > 1) {
			if ("join".equalsIgnoreCase(requestInfo[1])) {
				//Request to join channel
				responseJSON = api.joinChannel(u, cID);			
			} else if ("leave".equalsIgnoreCase(requestInfo[1])) {
				//Request to leave channel
				responseJSON = api.leaveChannel(u);
			} else if ("reset".equalsIgnoreCase(requestInfo[1])) {
				//Request to reset the channel
				responseJSON = api.resetChannel(u, cID);
			} else if ("kick".equalsIgnoreCase(requestInfo[1])) {
				//Request to kick a user from the channel (also applies a 60 second ban)
				int kUID = requestJSON.optInt("userID", 0);
				if (kUID == 0) {
					responseJSON.put("status", HttpServletResponse.SC_BAD_REQUEST);
					responseJSON.put("msgArgs", "arg=userID,expected=Integer,found=null");
					responseJSON.put("msgCode", 177);
					responseJSON.put("message", "Invalid or missing parameter for userID; expected: Integer, found: null."); 
				} else {
					responseJSON = api.kickUser(u, cID, kUID);
				}					
			} else if ("tempban".equalsIgnoreCase(requestInfo[1])) {
				//Request to temporarily ban a user from the channel
				int uID = requestJSON.optInt("userID", 0),//Tries to extract the user ID. If no userID is found, returns 0
					duration = requestJSON.getInt("duration");
				if (uID == 0) {
					//If no user ID was specified, checks for a username.
					String un = requestJSON.optString("username", null);
					if (un != null) {
						//If a username was specified, attempt to resolve it to an ID
						uID = server.getUserManager().getUserID(un);
						if (uID != 0) {
							//If a userID was found, use the specified ID
							responseJSON = api.tempBanUser(u, cID, uID, duration);
						} else {
							//Send an error message otherwise
							responseJSON.put("status", 404);
							responseJSON.put("msgCode", 162);
							responseJSON.put("message", "The user you have attempted to temporarily ban was not found.");
						}
					} else {
						responseJSON.put("status", HttpServletResponse.SC_BAD_REQUEST);
						responseJSON.put("msgArgs", "arg=userID OR username,expected=Integer OR String,found=neither");
						responseJSON.put("msgCode", 177);
						responseJSON.put("message", "Invalid or missing parameter for userID OR username; expected: Integer OR String, found: neither."); 
					}
				} else {
					responseJSON = api.tempBanUser(u, cID, uID, duration);
				}
			} else if ("lock".equalsIgnoreCase(requestInfo[1])) {
				//Request to lock the channel down, preventing new people of a certain rank from entering while keeping all existing members with that rank
				int rank = requestJSON.optInt("rank", Settings.GUEST_RANK);
				int durationMins = requestJSON.optInt("duration", 15);
				//If no parameters (or invalid parameters) are supplied, default to locking out new guests for 15 minutes.
				responseJSON = api.lockChannel(u, cID, rank, durationMins);
			} else {
				responseJSON = processGetRequest(requestInfo, cID, api, response);//Relays the request as a get request
			}
		} else {
			responseJSON = processGetRequest(requestInfo, cID, api, response);//Relays the request as a get request
		}		
		return responseJSON;
	}

}
