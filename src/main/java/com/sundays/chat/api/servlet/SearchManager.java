/*******************************************************************************
 * Copyright (c) 2013, 2016 Francis G.
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sundays.chat.io.ChannelDetails;
import com.sundays.chat.io.ChannelIndex;
import com.sundays.chat.io.ChannelIndex.SearchType;
import com.sundays.chat.utils.HttpRequestTools;

/**
 * Servlet implementation class SearchManager
 */
public class SearchManager extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	private static final Logger logger = LoggerFactory.getLogger(SearchManager.class);
	
	private ServletLauncher server;
	
	private ChannelIndex channelIndex;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SearchManager() {
        super();
    }
    
    @Override
    public void init (ServletConfig config) throws ServletException {
    	super.init(config);
    	server = ServletLauncher.getInstance();
    	if (!server.initalised) {
    		server.init(config);
    	}
    	channelIndex = server.getIO().getChannelIndex();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse httpResponse) throws ServletException, IOException {
		if (request.getPathInfo() == null) {
			//No type specified
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		String[] requestInfo = request.getPathInfo().substring(1).split("/");
		if (requestInfo.length == 0) {
			//No type specified
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		switch (requestInfo[0]) {
		case "channel":
			processChannelSearch(request, requestInfo, httpResponse);
			return;
		default:
			httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.doGet(request, response);//Relay request as GET request
	}
	
	private void processChannelSearch (HttpServletRequest httpRequest, String[] requestInfo, HttpServletResponse httpResponse) throws IOException {
		Map<String, String[]> parameters = httpRequest.getParameterMap();	
		JSONObject responseJSON = new JSONObject();
		if (parameters.containsKey("name")) {
			String channelName = httpRequest.getParameter("name");
			Optional<ChannelDetails> searchResult;
			try {
				searchResult = channelIndex.lookupByName(channelName);
			} catch (IOException ex) {
				logger.error("Error looking up channel by name "+channelName, ex);		
				httpResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return;
			}
			if (!searchResult.isPresent()) {
				responseJSON.put("status", HttpServletResponse.SC_NOT_FOUND);
				responseJSON.put("message", "Channel not found.");
			} else {
				ChannelDetails channel = searchResult.get();
				JSONObject channelJSON = new  JSONObject(channel);
				channelJSON.put("isLoaded", server.getChannelManager().isLoaded(channel.getId()));
				
				responseJSON.put("status", HttpServletResponse.SC_OK);
				responseJSON.put("type", "exact");
				responseJSON.put("channel", channelJSON);
			}
		} else if (parameters.containsKey("contains")) {
			String searchTerm = parameters.get("contains")[0];
			Collection<ChannelDetails> channels = channelIndex.search(searchTerm, SearchType.CONTAINS, 100);
			List<JSONObject> matchingChannels = new ArrayList<JSONObject>();
			for (ChannelDetails channel : channels) {
				JSONObject returnChannel = new JSONObject(channel);
				returnChannel.put("loaded", server.getChannelManager().isLoaded(channel.getId()));
				matchingChannels.add(returnChannel);
			}
			responseJSON.put("status", HttpServletResponse.SC_OK);
			responseJSON.put("type", "contains");
			responseJSON.put("channels", matchingChannels);
		} else if (parameters.containsKey("all")) {
			Collection<ChannelDetails> channels = channelIndex.search("", SearchType.ALL, 100);
			List<JSONObject> matchingChannels = new ArrayList<JSONObject>();
			for (ChannelDetails channel : channels) {
				JSONObject returnChannel = new JSONObject(channel);
				returnChannel.put("loaded", server.getChannelManager().isLoaded(channel.getId()));
				matchingChannels.add(returnChannel);
			}
			responseJSON.put("status", HttpServletResponse.SC_OK);
			responseJSON.put("type", "all");
			responseJSON.put("channels", matchingChannels);
		} else {
			httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "No valid search term included in the request.");
		}
		HttpRequestTools.sendResponseJSON(httpResponse, responseJSON);
	}
}
