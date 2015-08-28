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
package com.sundays.chat.interfaces;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.sundays.chat.server.User;

public interface ReportBackend {

	/**
     * Logs a report applied by one of the channel users to the global moderator cue
     * 
     * @param channelMessages 	the messages currently in the channel cache
     * @param reporter			the user who initiated the report
     * @param channelID			the ID of the channel from which this report originated
     * @param message			a custom message sent by the reporter, explaining the issue
     * @throws JSONException	throws an exception if the data could not be inserted into a JSONObject, for whatever reason.
     */
	public void sendChannelReport(List<JSONObject> channelMessages, User reporter, int channelID, String message) throws JSONException;
	
	public void sendUserReport(List<JSONObject> userMessages, JSONObject report, String message) throws JSONException;
}
