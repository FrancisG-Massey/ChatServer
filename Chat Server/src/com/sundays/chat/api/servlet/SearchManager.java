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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.sundays.chat.io.ChannelIndex;
import com.sundays.chat.io.ChannelIndex.SearchType;
import com.sundays.chat.server.channel.ChannelManager;
import com.sundays.chat.utils.HttpRequestTools;

/**
 * Servlet implementation class SearchManager
 */
public class SearchManager extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private ServletChatServer server;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SearchManager() {
        super();
    }
    
    @Override
    public void init (ServletConfig config) throws ServletException {
    	super.init(config);
    	server = ServletChatServer.getInstance();
    	if (!server.initalised) {
    		server.init(config);
    	}
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (request.getPathInfo() == null) {
			//No type specified
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		String[] requestInfo = request.getPathInfo().substring(1).split("/");
		if (requestInfo.length == 0) {
			//No type specified
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		JSONObject responseJSON = null;
		
		try {
			responseJSON = processRequest(request, requestInfo);
			if (responseJSON.length() == 0) {
				responseJSON.put("status", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} else if (!responseJSON.has("status")) {
				responseJSON.put("status", HttpServletResponse.SC_OK);
			}				
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
		HttpRequestTools.sendResponseJSON(response, responseJSON);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.doGet(request, response);//Relay request as GET request
	}
	
	private JSONObject processRequest (HttpServletRequest request, String[] requestInfo) throws JSONException {
		JSONObject responseJSON = new JSONObject();
		if (requestInfo[0].equalsIgnoreCase("channel")) {
			Map<String, String[]> parameters = request.getParameterMap();
			ChannelManager cm = server.getChannelManager();
			
			if (parameters.containsKey("name")) {
				String channelName = request.getParameter("name");
				int cID = cm.getChannelID(channelName);
				if (cID == 0) {
					responseJSON.put("status", HttpServletResponse.SC_NOT_FOUND);
					responseJSON.put("message", "Channel not found.");
				} else {
					responseJSON = cm.getChannelInformation(cID);
					responseJSON.put("status", HttpServletResponse.SC_OK);
					responseJSON.put("type", "exact");
					responseJSON.put("keyName", channelName);
					responseJSON.put("id", cID);
				}
			} else if (parameters.containsKey("contains")) {
				String searchTerm = parameters.get("contains")[0];
				Map<String, Integer> channels = getChannelIndex().search(searchTerm, SearchType.CONTAINS, 100);
				List<JSONObject> matchingChannels = new ArrayList<JSONObject>();
				for (Map.Entry<String, Integer> c : channels.entrySet()) {
					JSONObject returnChannel = cm.getChannelInformation(c.getValue());
					returnChannel.put("keyName", c.getKey());
					returnChannel.put("id", c.getValue());
					matchingChannels.add(returnChannel);
				}
				responseJSON.put("status", HttpServletResponse.SC_OK);
				responseJSON.put("type", "contains");
				responseJSON.put("channels", matchingChannels);
			} else if (parameters.containsKey("all")) {
				Map<String, Integer> channels = getChannelIndex().search("", SearchType.ALL, 100);
				List<JSONObject> matchingChannels = new ArrayList<JSONObject>();
				for (Map.Entry<String, Integer> c : channels.entrySet()) {
					JSONObject returnChannel = cm.getChannelInformation(c.getValue());
					returnChannel.put("keyName", c.getKey());
					returnChannel.put("id", c.getValue());
					matchingChannels.add(returnChannel);
				}
				responseJSON.put("status", HttpServletResponse.SC_OK);
				responseJSON.put("type", "all");
				responseJSON.put("channels", matchingChannels);
			} else {
				responseJSON.put("status", HttpServletResponse.SC_NOT_FOUND);
				responseJSON.put("message", "Channel not found.");
			}
		}
		return responseJSON;
	}
	
	private ChannelIndex getChannelIndex () {
		return server.getIO().getChannelIndex();
	}

}
