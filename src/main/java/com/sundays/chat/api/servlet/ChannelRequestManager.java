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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sundays.chat.server.channel.ChannelGroup;
import com.sundays.chat.server.channel.ChannelManager;
import com.sundays.chat.server.message.StatusMessage;
import com.sundays.chat.server.user.User;
import com.sundays.chat.server.user.UserManager;
import com.sundays.chat.server.user.UserMessageWrapper;
import com.sundays.chat.utils.HttpRequestTools;

/**
 * Servlet implementation class ChannelManager
 */
public class ChannelRequestManager extends HttpServlet {
	
	private static final long serialVersionUID = 5326141926392670401L;

	private static final Logger logger = Logger.getLogger(ChannelRequestManager.class);
	
	private ServletLauncher launcher;
	
	private ChannelManager channelManager;
	
	private UserManager userManager;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ChannelRequestManager() {
        super();
    }

    @Override
    public void init (ServletConfig config) throws ServletException {
    	super.init(config);
    	launcher = ServletLauncher.getInstance();
    	if (!launcher.initalised) {
    		launcher.init(config);
    	}
    	channelManager = launcher.getChannelManager();
    }
    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (request.getPathInfo() == null) {//No channel ID specified
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No channel ID specified");
			return;
		}
		String[] requestInfo = request.getPathInfo().substring(1).split("/");
		if (requestInfo.length == 0) {//No channel ID specified
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No channel ID specified");
			return;
		}
		int channelID = 0;
		try {
			channelID = Integer.parseInt(requestInfo[0]);
		} catch (NumberFormatException e) {//Channel ID is not a number
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid channel ID specified");
			return;
		}
		JSONObject responseMessage = new JSONObject();
		if (!channelManager.channelExists(channelID)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Channel not found: "+channelID);
			return;
		}
		
		try {
			responseMessage = processGetRequest(requestInfo, channelID, response);
		} catch (JSONException ex) {
			logger.error("Failed to process request.", ex);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		
		try {
			if (responseMessage == null) {
				responseMessage = new JSONObject();
				responseMessage.put("status", HttpServletResponse.SC_NOT_FOUND);
			} else if (responseMessage.length() == 0) {
				responseMessage.put("status", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} else if (!responseMessage.has("status")) {
				responseMessage.put("status", HttpServletResponse.SC_OK);
			}				
		} catch (JSONException ex) {
			logger.error("Failed to pack response for channel request "+channelID, ex);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		HttpRequestTools.sendResponseJSON(response, responseMessage);
		return;
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (request.getPathInfo() == null) {//No channel ID specified
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		String[] requestParams = request.getPathInfo().substring(1).split("/");
		if (requestParams.length == 0) {//No channel ID specified
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		int channelID = 0;
		try {
			channelID = Integer.parseInt(requestParams[0]);
		} catch (NumberFormatException e) {//Channel ID is not a number
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		JSONObject responseMessage = new JSONObject();
		if (!channelManager.channelExists(channelID)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Channel not found: "+channelID);
			return;
		}
		if (requestParams.length == 1) {
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
				User u = userManager.getUserSession(requestJSON.optString("session", null));
				if (u == null) {
					//User string does not match a valid, currently logged in user
					responseMessage.put("status", HttpServletResponse.SC_FORBIDDEN);
					responseMessage.put("message", "You must be logged in to use this method,");
				} else {
					responseMessage = processPostRequest(requestParams, channelID, u, requestJSON, response);
				}
				if (responseMessage == null) {
					return;
				} else if (responseMessage.length() == 0) {
					responseMessage.put("status", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				} else if (!responseMessage.has("status")) {
					responseMessage.put("status", HttpServletResponse.SC_OK);
				}
				HttpRequestTools.sendResponseJSON(response, responseMessage);	
				
			} catch (JSONException ex) {
				logger.error("Failed to pack response for channel request "+channelID, ex);
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return;
			}
		} else {
			response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
		}
	}
	
	private JSONObject processGetRequest (String[] requestParams, int channelID, HttpServletResponse response) throws JSONException, IOException {
		JSONObject responseMessage = new JSONObject();
		if (requestParams.length == 1) {
			//Request for channel details
			responseMessage = new JSONObject(channelManager.getChannelDetails(channelID));			
		} else if ("users".equalsIgnoreCase(requestParams[1])) {
			//Request for channel list
			responseMessage = new JSONObject(channelManager.getUserList(channelID));	
		} else if ("members".equalsIgnoreCase(requestParams[1])) {
			//Request for channel member list
			responseMessage = new JSONObject(channelManager.getMemberList(channelID));
		} else if ("bans".equalsIgnoreCase(requestParams[1])) {
			//Request for channel permissions
			responseMessage = new JSONObject(channelManager.getBanList(channelID));
		} else if ("groups".equalsIgnoreCase(requestParams[1])) {
			//Request for channel groups
			responseMessage = new JSONObject(channelManager.getChannelGroups(channelID));
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
		return responseMessage;
	}
	
	private boolean processMessageRequest (String[] requestParams, JSONObject requestMessage, int channelID, User user, HttpServletResponse response) throws IOException {
		if (requestParams.length < 3) {
			return false;
		}
		JSONObject responseMessage = new JSONObject();
		try {
			if ("send".equalsIgnoreCase(requestParams[2])) {
				//Request to send a message in the channel
				String message = requestMessage.optString("message", null);
				if (message == null) {
					responseMessage.put("status", HttpServletResponse.SC_BAD_REQUEST);
					responseMessage.put("msgArgs", "arg=message,expected=String,found=null");
					responseMessage.put("msgCode", 177);
					responseMessage.put("message", "Invalid or missing parameter for message; expected: String, found: null."); 
				} else {
					responseMessage = channelManager.sendMessage(user, message);
				}
			} else if ("get".equalsIgnoreCase(requestParams[2])) {
				//Request to collect cued messages (NOTE: This will remove everything currently in the cue)
				if (user.hasCuedMessages(channelID)) {
					responseMessage.put("status", HttpServletResponse.SC_OK);
					List<UserMessageWrapper> messages = user.getQueuedMessages(channelID, true);
					if (messages == null) {
						responseMessage.put("messages", JSONObject.NULL);
					} else {
						JSONArray packedMessages = new JSONArray();
						JSONObject jsonObject;
						for (UserMessageWrapper message : messages) {
							jsonObject = new JSONObject(message.getPayload());
							jsonObject.put("orderID", message.getOrderID());
							jsonObject.put("type", message.getType().getID());
							jsonObject.put("timestamp", message.getTimestamp());
							packedMessages.put(jsonObject);
						}
						responseMessage.put("messages", packedMessages);
					}
				} else {
					responseMessage.put("status", HttpServletResponse.SC_NO_CONTENT);
				}
			} else {
				return false;
			}
			HttpRequestTools.sendResponseJSON(response, responseMessage);;
			return true;
		} catch (JSONException ex) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return true;
		}
	}
	
	/**
	 * Processes a request relating to the channel member list.
	 * 
	 * @param requestParams A string array containing the request arguments (0=channelID, 1=arg1, 2=arg2)
	 * @param requestMessage A JSON object containing the POST request body
	 * @param channelID The channel ID
	 * @param user The user who commited the request
	 * @param response A {@link HttpServletResponse} object used for sending the reply to the user 
	 * @return True if the the request was handled (even if an error occured), false otherwise
	 * @throws IOException If the response could not be sent due to an error.
	 */
	private boolean processMemberRequest (String[] requestParams, JSONObject requestMessage, int channelID, User user, HttpServletResponse response) throws IOException {
		if (requestParams.length < 3) {
			return false;
		}
		JSONObject responseMessage = new JSONObject();
		try {
			if ("add".equalsIgnoreCase(requestParams[2])) {
				//Request to add a rank to the rank list
				int uID = requestMessage.optInt("user", 0);//Tries to extract the user ID. If no userID is found, returns 0
				if (uID == 0) {
					//If no user ID was specified, checks for a username.
					String un = requestMessage.optString("username", null);
					if (un != null) {
						//If a username was specified, attempt to resolve it to an ID
						uID = launcher.getUserManager().getUserID(un);
						if (uID != 0) {
							//If a userID was found, use the specified ID
							responseMessage = channelManager.addMember(user, channelID, uID);
						} else {
							responseMessage.put("status", 404);
							responseMessage.put("msgCode", 159);
							responseMessage.put("message", "The user you have attempted to rank was not found.");
						}
					} else {
						//If no userID or username was specified, send back an error message.
						responseMessage.put("status", HttpServletResponse.SC_BAD_REQUEST);
						responseMessage.put("msgArgs", "arg=userID OR username,expected=Integer OR String,found=neither");
						responseMessage.put("msgCode", 177);
						responseMessage.put("message", "Invalid or missing parameter for userID OR username; expected: Integer OR String, found: neither.");
					}
				} else {
					responseMessage = channelManager.addMember(user, channelID, uID);
				}
			} else if ("remove".equalsIgnoreCase(requestParams[2])) {
				//Request to add a rank to the rank list
				int uID = requestMessage.optInt("user", 0);
				if (uID == 0) {
					responseMessage.put("status", HttpServletResponse.SC_BAD_REQUEST);
					responseMessage.put("msgArgs", "arg=userID,expected=Integer,found=null");
					responseMessage.put("msgCode", 177);
					responseMessage.put("message", "Invalid or missing parameter for userID; expected: Integer, found: null."); 
				} else {
					responseMessage = channelManager.removeMember(user, channelID, uID);
				}					
			} else if ("update".equalsIgnoreCase(requestParams[2])) {
				//Request to add a rank to the rank list
				int uID = requestMessage.optInt("user", 0);
				int groupID = requestMessage.optInt("group", Integer.MAX_VALUE);
				if (uID == 0) {
					responseMessage.put("status", HttpServletResponse.SC_BAD_REQUEST);
					responseMessage.put("msgArgs", "arg=userID,expected=Integer,found=null");
					responseMessage.put("msgCode", 177);
					responseMessage.put("message", "Invalid or missing parameter for userID; expected: Integer, found: null."); 
				} else if (groupID > Byte.MAX_VALUE || groupID < Byte.MIN_VALUE) {
					responseMessage.put("status", HttpServletResponse.SC_BAD_REQUEST);
					responseMessage.put("msgArgs", "arg=rankID,expected=byte,found=null");
					responseMessage.put("msgCode", 177);
					responseMessage.put("message", "Invalid or missing parameter for rankID; expected: byte, found: null."); 
				} else {
					responseMessage = channelManager.updateMember(user, channelID, uID, (byte) groupID);
				}
				
			} else {
				return false;
			}
			HttpRequestTools.sendResponseJSON(response, responseMessage);
			return true;
		} catch (JSONException ex) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return true;
		}
	}
	
	private boolean processBanRequest (String[] requestParams, JSONObject requestMessage, int channelID, User user, HttpServletResponse response) throws IOException {
		if (requestParams.length < 3) {
			return false;
		}
		JSONObject responseMessage = new JSONObject();
		try {
			if ("add".equalsIgnoreCase(requestParams[2])) {
				//Request to add a rank to the rank list
				int uID = requestMessage.optInt("user", 0);//Tries to extract the user ID. If no userID is found, returns 0
				if (uID == 0) {
					//If no user ID was specified, checks for a username.
					String un = requestMessage.optString("username", null);
					if (un != null) {
						//If a username was specified, attempt to resolve it to an ID
						uID = launcher.getUserManager().getUserID(un);
						if (uID != 0) {
							//If a userID was found, use the specified ID
							responseMessage = channelManager.addBan(user, channelID, uID);
						} else {
							responseMessage.put("status", 404);
							responseMessage.put("msgCode", 160);
							responseMessage.put("message", "The user you have attempted to ban was not found.");
						}
					} else {
						//If no userID or username was specified, send back an error message.
						responseMessage.put("status", HttpServletResponse.SC_BAD_REQUEST);
						responseMessage.put("msgArgs", "arg=userID OR username,expected=Integer OR String,found=neither");
						responseMessage.put("msgCode", 177);
						responseMessage.put("message", "Invalid or missing parameter for userID OR username; expected: Integer OR String, found: neither.");
					}
				} else {
					responseMessage = channelManager.addBan(user, channelID, uID);
				}
			} else if ("remove".equalsIgnoreCase(requestParams[2])) {
				//Request to add a rank to the rank list
				int uID = requestMessage.optInt("user", 0);
				if (uID == 0) {
					responseMessage.put("status", HttpServletResponse.SC_BAD_REQUEST);
					responseMessage.put("msgArgs", "arg=userID,expected=Integer,found=null");
					responseMessage.put("msgCode", 177);
					responseMessage.put("message", "Invalid or missing parameter for userID; expected: Integer, found: null."); 
				} else {
					responseMessage = channelManager.removeBan(user, channelID, uID);
				}
			
			} else {
				return false;
			}
			HttpRequestTools.sendResponseJSON(response, responseMessage);
			return true;
		} catch (JSONException ex) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return true;
		}
	}
	
	private JSONObject processPostRequest (String[] requestParams, int channelID, User user, JSONObject requestJSON, HttpServletResponse response) throws ServletException, JSONException, IOException {
		JSONObject responseJSON = new JSONObject();
		
		if ("messages".equalsIgnoreCase(requestParams[1])) {
			if (processMessageRequest(requestParams, requestJSON, channelID, user, response)) {
				return null;
			}
		} else if ("members".equalsIgnoreCase(requestParams[1])) {
			if (processMemberRequest(requestParams, requestJSON, channelID, user, response)) {
				return null;
			}
		} else if ("bans".equalsIgnoreCase(requestParams[1])) {
			if (processBanRequest(requestParams, requestJSON, channelID, user, response)) {
				return null;
			}
		}
		
		if (requestParams.length > 2) {//Process any double-parameter requests first
			if ("openingmessage".equalsIgnoreCase(requestParams[1]) && "change".equalsIgnoreCase(requestParams[2])) {
				//Request to change channel permissions
				String message = requestJSON.optString("message", null);
				if (message == null) {
					responseJSON.put("status", HttpServletResponse.SC_BAD_REQUEST);
					responseJSON.put("msgArgs", "arg=message,expected=String,found=null");
					responseJSON.put("msgCode", 177);
					responseJSON.put("message", "Invalid or missing parameter for message; expected: String, found: null."); 
				} else {
					responseJSON = channelManager.setWelcomeMessage(user, channelID, message, Color.black);
				}
			} else {
				responseJSON = processGetRequest(requestParams, channelID, response);//Relays the request as a get request
			} 
		} else if (requestParams.length > 1) {
			StatusMessage status;
			if ("join".equalsIgnoreCase(requestParams[1])) {
				//Request to join channel
				responseJSON = channelManager.joinChannel(user, channelID);			
			} else if ("leave".equalsIgnoreCase(requestParams[1])) {
				//Request to leave channel
				status = channelManager.leaveChannel(user);
			} else if ("reset".equalsIgnoreCase(requestParams[1])) {
				//Request to reset the channel
				responseJSON = channelManager.resetChannel(user, channelID);
			} else if ("kick".equalsIgnoreCase(requestParams[1])) {
				//Request to kick a user from the channel (also applies a 60 second ban)
				int kUID = requestJSON.optInt("userID", 0);
				if (kUID == 0) {
					responseJSON.put("status", HttpServletResponse.SC_BAD_REQUEST);
					responseJSON.put("msgArgs", "arg=userID,expected=Integer,found=null");
					responseJSON.put("msgCode", 177);
					responseJSON.put("message", "Invalid or missing parameter for userID; expected: Integer, found: null."); 
				} else {
					responseJSON = channelManager.kickUser(user, channelID, kUID);
				}					
			} else if ("tempban".equalsIgnoreCase(requestParams[1])) {
				//Request to temporarily ban a user from the channel
				int uID = requestJSON.optInt("userID", 0),//Tries to extract the user ID. If no userID is found, returns 0
					duration = requestJSON.getInt("duration");
				if (uID == 0) {
					//If no user ID was specified, checks for a username.
					String un = requestJSON.optString("username", null);
					if (un != null) {
						//If a username was specified, attempt to resolve it to an ID
						uID = launcher.getUserManager().getUserID(un);
						if (uID != 0) {
							//If a userID was found, use the specified ID
							responseJSON = channelManager.tempBanUser(user, channelID, uID, duration);
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
					responseJSON = channelManager.tempBanUser(user, channelID, uID, duration);
				}
			} else if ("lock".equalsIgnoreCase(requestParams[1])) {
				//Request to lock the channel down, preventing new people of a certain rank from entering while keeping all existing members with that rank
				int rank = requestJSON.optInt("rank", ChannelGroup.GUEST_GROUP);
				int durationMins = requestJSON.optInt("duration", 15);
				//If no parameters (or invalid parameters) are supplied, default to locking out new guests for 15 minutes.
				responseJSON = channelManager.lockChannel(user, channelID, rank, durationMins);
			} else {
				responseJSON = processGetRequest(requestParams, channelID, response);//Relays the request as a get request
			}
		} else {
			responseJSON = processGetRequest(requestParams, channelID, response);//Relays the request as a get request
		}		
		return responseJSON;
	}

}
