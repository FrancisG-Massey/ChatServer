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
import java.io.PrintWriter;
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
import com.sundays.chat.server.channel.ChannelResponse;
import com.sundays.chat.server.channel.ChannelResponseType;
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
		int channelId = 0;
		try {
			channelId = Integer.parseInt(requestInfo[0]);
		} catch (NumberFormatException e) {//Channel ID is not a number
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid channel ID specified");
			return;
		}
		if (!channelManager.channelExists(channelId)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND, "Channel not found: "+channelId);
			return;
		}
		
		try {
			processGetRequest(requestInfo, channelId, response);
		} catch (JSONException ex) {
			logger.error("Failed to process request.", ex);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse httpResponse) throws ServletException, IOException {
		if (request.getPathInfo() == null) {//No channel ID specified
			httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		String[] requestParams = request.getPathInfo().substring(1).split("/");
		if (requestParams.length == 0) {//No channel ID specified
			httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		int channelID = 0;
		try {
			channelID = Integer.parseInt(requestParams[0]);
		} catch (NumberFormatException e) {//Channel ID is not a number
			httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		if (!channelManager.channelExists(channelID)) {
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "Channel not found: "+channelID);
			return;
		}
		if (requestParams.length == 1) {
			//Request for channel details
			doGet(request, httpResponse);//Relays the request as a get request			
		} else if (request.getContentType().split(";")[0].equals("application/json")) {	
			JSONObject requestJSON = null;
			try {
				requestJSON = HttpRequestTools.getRequestJSON(request);
			} catch (JSONException e) {
				httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
			}
			try {				
				User user = userManager.getUserSession(requestJSON.optString("session", null));
				if (user == null) {
					//User string does not match a valid, currently logged in user
					httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "You must be logged in to use this method.");
				} else {
					processPostRequest(requestParams, channelID, user, requestJSON, httpResponse);
				}	
				
			} catch (JSONException ex) {
				logger.error("Failed to pack response for channel request "+channelID, ex);
				httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return;
			}
		} else {
			httpResponse.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
		}
	}
	
	private void processGetRequest (String[] requestParams, int channelID, HttpServletResponse httpResponse) throws JSONException, IOException {
		JSONObject responseMessage;
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
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		HttpRequestTools.sendResponseJSON(httpResponse, responseMessage);
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
			HttpRequestTools.sendResponseJSON(response, responseMessage);
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
	private boolean processMemberRequest (String[] requestParams, JSONObject requestMessage, int channelID, User user, HttpServletResponse httpResponse) throws IOException {
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
						uID = userManager.getUserID(un);
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
					responseMessage.put("msgArgs", "arg=user,expected=int,found=null");
					responseMessage.put("msgCode", 177);
					responseMessage.put("message", "Invalid or missing parameter for user; expected: Integer, found: null."); 
				} else if (groupID > Byte.MAX_VALUE || groupID < Byte.MIN_VALUE) {
					responseMessage.put("status", HttpServletResponse.SC_BAD_REQUEST);
					responseMessage.put("msgArgs", "arg=group,expected=int,found=null");
					responseMessage.put("msgCode", 177);
					responseMessage.put("message", "Invalid or missing parameter for group; expected: byte, found: null."); 
				} else {
					responseMessage = channelManager.updateMember(user, channelID, uID, (byte) groupID);
				}
				
			} else {
				return false;
			}
			HttpRequestTools.sendResponseJSON(httpResponse, responseMessage);
			return true;
		} catch (JSONException ex) {
			httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return true;
		}
	}
	
	private boolean processBanRequest (String[] requestParams, JSONObject requestMessage, int channelID, User user, HttpServletResponse httpResponse) throws IOException {
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
						uID = userManager.getUserID(un);
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
			HttpRequestTools.sendResponseJSON(httpResponse, responseMessage);
			return true;
		} catch (JSONException ex) {
			httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return true;
		}
	}
	
	private void processPostRequest (String[] requestParams, int channelID, User user, JSONObject requestJSON, HttpServletResponse httpResponse) throws ServletException, JSONException, IOException {
		JSONObject responseJSON = new JSONObject();
		
		if ("messages".equalsIgnoreCase(requestParams[1])) {
			if (processMessageRequest(requestParams, requestJSON, channelID, user, httpResponse)) {
				return;
			}
		} else if ("members".equalsIgnoreCase(requestParams[1])) {
			if (processMemberRequest(requestParams, requestJSON, channelID, user, httpResponse)) {
				return;
			}
		} else if ("bans".equalsIgnoreCase(requestParams[1])) {
			if (processBanRequest(requestParams, requestJSON, channelID, user, httpResponse)) {
				return;
			}
		}
		ChannelResponse response = null;
		
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
				processGetRequest(requestParams, channelID, httpResponse);//Relays the request as a get request
				return;
			} 
		} else if (requestParams.length > 1) {
			switch (requestParams[1]) {
			case "join"://Request to join channel
				response = channelManager.joinChannel(user, channelID);					
				break;
			case "leave"://Request to leave channel
				response = channelManager.leaveChannel(user);
				break;
			case "reset"://Request to reset the channel
				response = channelManager.resetChannel(user, channelID);
				break;
			case "kick"://Request to kick a user from the channel (also applies a 60 second ban)
				int kickTargetId = requestJSON.optInt("userId", -1);
				if (kickTargetId == -1) {
					//177 Invalid or missing parameter for userID; expected: Integer, found: null.
					httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing userID from kick request arguments.");
					return;
				}
				response = channelManager.kickUser(user, channelID, kickTargetId);
				break;
			case "tempban":
				int banTargetId = requestJSON.optInt("userId", -1);//Tries to extract the user ID. If no userID is found, returns -1
				int banDuration = requestJSON.optInt("duration", -1);
				if (banTargetId == -1) {
					//If no user ID was specified, checks for a username.
					String banTargetName = requestJSON.optString("username", null);
					if (banTargetName == null) {
						httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing userID or username from temporary ban request arguments.");
						return;
					}
					//If a username was specified, attempt to resolve it to an ID
					banTargetId = launcher.getUserManager().getUserID(banTargetName);
				}
				if (banTargetId == -1) {
					response = new ChannelResponse(ChannelResponseType.USER_NOT_FOUND, "tempBanUserNotFound");
				} else {
					response = channelManager.tempBanUser(user, channelID, banTargetId, banDuration);
				}
				break;
			case "lock":
				//Request to lock the channel down, preventing new people of a certain rank from entering while keeping all existing members with that rank
				int rank = requestJSON.optInt("rank", ChannelGroup.GUEST_GROUP);
				int durationMins = requestJSON.optInt("duration", 15);
				//If no parameters (or invalid parameters) are supplied, default to locking out new guests for 15 minutes.
				response = channelManager.lockChannel(user, channelID, rank, durationMins);
				break;
			default:
				processGetRequest(requestParams, channelID, httpResponse);//Relays the request as a get request
				return;
			}
		} else {
			processGetRequest(requestParams, channelID, httpResponse);//Relays the request as a get request
			return;
		}	
		if (response != null) {
			sendResponseAsJSON(httpResponse, response);
		} else {
			HttpRequestTools.sendResponseJSON(httpResponse, responseJSON);
		}
	}
	
	private void sendResponseAsJSON(HttpServletResponse httpResponse, ChannelResponse response) {
		JSONObject json = new JSONObject(response.getParams());
		try {
			json.put("status", response.getType().getId());
			json.put("messageTemplate", response.getMessageTemplate());
			
			httpResponse.setContentType("application/json");
			PrintWriter out = httpResponse.getWriter();
			out.println(json.toString());
		} catch (JSONException | IOException ex) {
			logger.error("Error sending response", ex);
		}
	}

}