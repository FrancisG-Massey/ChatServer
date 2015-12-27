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
    	ServletLauncher launcher = ServletLauncher.getInstance();
    	if (!launcher.initalised) {
    		launcher.init(config);
    	}
    	channelManager = launcher.getChannelManager();
    	userManager = launcher.getUserManager();
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
		int channelId = -1;
		try {
			channelId = Integer.parseInt(requestParams[0]);
		} catch (NumberFormatException e) {//Channel ID is not a number
			httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		if (!channelManager.channelExists(channelId)) {
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "Channel not found: "+channelId);
			return;
		}
		if (!request.getContentType().split(";")[0].equals("application/json")) {
			httpResponse.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
		} else {
			JSONObject jsonRequest = null;
			try {
				jsonRequest = HttpRequestTools.getRequestJSON(request);
			} catch (JSONException e) {
				httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			User user = userManager.getUserSession(jsonRequest.optString("session", null));
			if (user == null) {
				//User string does not match a valid, currently logged in user
				httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "You must be logged in to use this method.");
				return;
			}
			if (requestParams.length == 1) {
				processChannelRequest(channelId, user, jsonRequest, httpResponse);
			} else {
				switch (requestParams[1]) {
				case "members":
					processMemberRequest(jsonRequest, channelId, user, httpResponse);
					return;
				case "bans":
					processBanRequest(jsonRequest, channelId, user, httpResponse);
					return;
				case "messages":
					processMessageRequest(requestParams, jsonRequest, channelId, user, httpResponse);
					return;
				}
			}
		}
	}
	
	private void processGetRequest (String[] requestParams, int channelID, HttpServletResponse httpResponse) throws JSONException, IOException {
		JSONObject responseMessage;
		if (requestParams.length == 1) {
			//Request for channel details
			responseMessage = new JSONObject(channelManager.getChannelDetails(channelID));			
		} else {
			switch (requestParams[1]) {
			case "users"://Request for channel list
				responseMessage = new JSONObject(channelManager.getUserList(channelID));
				break;
			case "members"://Request for channel member list
				responseMessage = new JSONObject(channelManager.getMemberList(channelID));
				break;
			case "bans"://Request for channel permissions
				responseMessage = new JSONObject(channelManager.getBanList(channelID));
				break;
			case "groups"://Request for channel groups
				responseMessage = new JSONObject(channelManager.getChannelGroups(channelID));
				break;
			default:
				httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}			
		}
		sendResponseJSON(httpResponse, responseMessage);
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
				//Request to collect queued messages (NOTE: This will remove everything currently in the queue)
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
			sendResponseJSON(response, responseMessage);
			return true;
		} catch (JSONException ex) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return true;
		}
	}
	
	private void processChannelRequest(int channelId, User user, JSONObject jsonRequest, HttpServletResponse httpResponse) throws IOException {
		String action = jsonRequest.optString("action");
		if (action == null) {
			JSONObject response = new JSONObject(channelManager.getChannelDetails(channelId));
			sendResponseJSON(httpResponse, response);
			return;
		}
		ChannelResponse response;
		switch (action) {
		case "join"://Request to join channel
			response = channelManager.joinChannel(user, channelId);					
			break;
		case "leave"://Request to leave channel
			response = channelManager.leaveChannel(user, user.getChannelId());
			break;
		case "reset"://Request to reset the channel
			response = channelManager.resetChannel(user, channelId);
			break;
		case "kick"://Request to kick a user from the channel (also applies a 60 second ban)
			int kickTargetId = jsonRequest.optInt("userId", -1);
			if (kickTargetId == -1) {
				//177 Invalid or missing parameter for userID; expected: Integer, found: null.
				httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing userID from kick request arguments.");
				return;
			}
			response = channelManager.kickUser(user, channelId, kickTargetId);
			break;
		case "tempban":
			int banTargetId = jsonRequest.optInt("userId", -1);//Tries to extract the user ID. If no userID is found, returns -1
			int banDuration = jsonRequest.optInt("duration", -1);
			if (banTargetId == -1) {
				//If no user ID was specified, checks for a username.
				String banTargetName = jsonRequest.optString("username", null);
				if (banTargetName == null) {
					httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing userID or username from temporary ban request arguments.");
					return;
				}
				//If a username was specified, attempt to resolve it to an ID
				banTargetId = userManager.getUserID(banTargetName);
			}
			if (banTargetId == -1) {
				response = new ChannelResponse(ChannelResponseType.USER_NOT_FOUND, "tempBanUserNotFound");
			} else {
				response = channelManager.tempBanUser(user, channelId, banTargetId, banDuration);
			}
			break;
		case "lock":
			//Request to lock the channel down, preventing new people of a certain rank from entering while keeping all existing members with that rank
			int rank = jsonRequest.optInt("rank", ChannelGroup.GUEST_GROUP);
			int durationMins = jsonRequest.optInt("duration", 15);
			//If no parameters (or invalid parameters) are supplied, default to locking out new guests for 15 minutes.
			response = channelManager.lockChannel(user, channelId, rank, durationMins);
			break;
		default:
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		sendResponseAsJSON(httpResponse, response);
	}
	
	/**
	 * Processes a request relating to the channel member list.
	 * 
	 * @param jsonRequest A JSON object containing the POST request body
	 * @param channelID The channel ID
	 * @param user The user who commited the request
	 * @param httpResponse A {@link HttpServletResponse} object used for sending the reply to the user 
	 * @return True if the the request was handled (even if an error occured), false otherwise
	 * @throws IOException If the response could not be sent due to an error.
	 */
	private void processMemberRequest (JSONObject jsonRequest, int channelID, User user, HttpServletResponse httpResponse) throws IOException {
		String action = jsonRequest.optString("action");
		if (action == null) {
			JSONObject response = new JSONObject(channelManager.getMemberList(channelID));
			sendResponseJSON(httpResponse, response);
			return;
		}
		ChannelResponse response;
		int targetMemberId;
		switch (action) {
		case "add"://Request to add a user to the member list
			targetMemberId = jsonRequest.optInt("userId", -1);//Tries to extract the user ID. If no userID is found, returns -1
			if (targetMemberId == -1) {
				//If no user ID was specified, checks for a username.
				String memberTargetName = jsonRequest.optString("username", null);
				if (memberTargetName == null) {
					httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing userId or username from member request arguments.");
					return;
				}
				//If a username was specified, attempt to resolve it to an ID
				targetMemberId = userManager.getUserID(memberTargetName);
			}
			if (targetMemberId == -1) {
				//159 The user you have attempted to rank was not found.
				response = new ChannelResponse(ChannelResponseType.USER_NOT_FOUND, "memberUserNotFound");
			} else {
				response = channelManager.addMember(user, channelID, targetMemberId);
			}
			break;
		case "remove"://Request to remove a user from the member list
			targetMemberId = jsonRequest.optInt("userId", -1);
			if (targetMemberId == -1) {
				httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing userId from member removal request arguments.");
				return;
			} else {
				response = channelManager.removeMember(user, channelID, targetMemberId);
			}
			break;
		case "update"://Request to add a rank to the rank list
			if (!jsonRequest.has("userId")) {
				httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing userId from member update request arguments.");
				return;
			}
			if (!jsonRequest.has("groupId")) {
				httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing groupId from member update request arguments.");
				return;
			}
			targetMemberId = jsonRequest.optInt("userId", -1);
			int groupId = jsonRequest.optInt("groupId", -1);
			response = channelManager.updateMember(user, channelID, targetMemberId, groupId);
			break;
		default:
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		sendResponseAsJSON(httpResponse, response);
	}
	
	private void processBanRequest (JSONObject jsonRequest, int channelID, User user, HttpServletResponse httpResponse) throws IOException {
		String action = jsonRequest.optString("action");
		if (action == null) {
			JSONObject response = new JSONObject(channelManager.getBanList(channelID));
			sendResponseJSON(httpResponse, response);
			return;
		}
		ChannelResponse response;
		switch (action) {
		case "add"://Request to add a user to the ban list
			int banTargetId = jsonRequest.optInt("userId", -1);//Tries to extract the user ID. If no userID is found, returns -1
			if (banTargetId == -1) {
				//If no user ID was specified, checks for a username.
				String banTargetName = jsonRequest.optString("username", null);
				if (banTargetName == null) {
					httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing userID or username from ban request arguments.");
					return;
				}
				//If a username was specified, attempt to resolve it to an ID
				banTargetId = userManager.getUserID(banTargetName);
			}
			
			if (banTargetId == -1) {
				//160 The user you have attempted to ban was not found.
				response = new ChannelResponse(ChannelResponseType.USER_NOT_FOUND, "banUserNotFound");
			} else {
				response = channelManager.addBan(user, channelID, banTargetId);
			}		
			break;
		case "remove":
			int banRemoveTargetId = jsonRequest.optInt("userId", -1);
			if (banRemoveTargetId == -1) {
				//177 Invalid or missing parameter for userID; expected: Integer, found: null.
				httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing userID from ban removal request arguments.");
				return;
			}
			response = channelManager.removeBan(user, channelID, banRemoveTargetId);
			break;
		default:
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		sendResponseAsJSON(httpResponse, response);
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
	
	private void sendResponseJSON (HttpServletResponse response, JSONObject responseJSON) {
		try {
			response.setContentType("application/json");
			PrintWriter out = response.getWriter();
			out.println(responseJSON.toString());
		} catch (IOException ex) {
			logger.error("Error sending response", ex);
		}
	}

}
