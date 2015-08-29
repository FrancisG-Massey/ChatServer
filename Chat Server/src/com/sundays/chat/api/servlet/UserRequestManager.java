/*******************************************************************************
 * Copyright (c) 2015 Francis G.
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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.sundays.chat.server.ChatServer;
import com.sundays.chat.server.UserManager;
import com.sundays.chat.utils.HttpRequestTools;

/**
 * Servlet implementation class UserRequestManager
 */
public class UserRequestManager extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public UserRequestManager() {
        super();        
    }
    
    @Override
    public void init (ServletConfig config) throws ServletException {
    	ChatServer server = ChatServer.getInstance();
    	if (!server.initalised) {
    		server.init(config);
    	}
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    @Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
    @Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (request.getPathInfo() == null) {
			//No request specified
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		String[] requestInfo = request.getPathInfo().substring(1).split("/");
		if (requestInfo.length == 0) {
			//No request specified
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		if ("application/json".equals(request.getContentType().split(";")[0])) {
			manageJSONRequest(request, response, requestInfo);
		} else {
			response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
		}
		
	}
	private void manageJSONRequest (HttpServletRequest request, HttpServletResponse response, String[] requestInfo) throws ServletException, IOException {
		JSONObject requestJSON,
		responseJSON = new JSONObject();
		int status = 0;
		UserManager um = ChatServer.getInstance().userManager();
		try {
			requestJSON = HttpRequestTools.getRequestJSON(request);			
		} catch (Exception e) {
			//Invalid JSON string sent
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		if (requestInfo[0].equalsIgnoreCase("login")) {
			if (requestInfo.length >= 2) {
				try {
					responseJSON = um.manageLogin(requestInfo[1], requestJSON);
				} catch (JSONException e) {
					//A JSONException will only be thrown if there is something missing or wrong with the request
					response.sendError(HttpServletResponse.SC_BAD_REQUEST);
					return;
				}
			} else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
		} else if (requestInfo[0].equalsIgnoreCase("logout")) {
			try {
				String sessionID = requestJSON.optString("session", null);//Retrieves the session ID
				if (sessionID == null) {
					responseJSON.put("status", HttpServletResponse.SC_BAD_REQUEST);//No session ID provided
					responseJSON.put("message", "You have not provided a session ID, which is required in order to log out.");
				} else {
					um.manageLogout(um.getUserSession(sessionID).getUserID());//Logs out the user
					responseJSON.put("status", 200);//Returns successful
				}				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else if (requestInfo[0].equalsIgnoreCase("create")) {
			if (requestInfo.length >= 2) {
				try {
					responseJSON = um.managedAccountCreate(requestInfo[1], requestJSON, request.getRemoteAddr());
				} catch (JSONException e) {
					//A JSONException will only be thrown if there is something missing or wrong with the request
					response.sendError(HttpServletResponse.SC_BAD_REQUEST);
					return;
				}
			} else {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
		}
		
		try {
			if (status != 0) {
				responseJSON.put("status", status);//If a manual status has been entered, place it in the json object
			} else if (responseJSON == null || responseJSON.length() == 0) {
				responseJSON = new JSONObject().put("status", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);//If the object has not been filled, an error must have occurred. Return an error code.
			} else if (!responseJSON.has("status")) {
				responseJSON.put("status", HttpServletResponse.SC_OK);
			}				
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		HttpRequestTools.sendResponseJSON(response, responseJSON);//Sends the response to the client
	}

}
