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
package com.sundays.chat.io.jdbc;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.sundays.chat.io.ReportBackend;
import com.sundays.chat.io.UserDetails;

public class JDBCReportManager implements ReportBackend {

    //A list which contains any channel reports to be sent to a global moderator
    private final LinkedList<JSONObject> channelReportCue = new LinkedList<JSONObject>();
	
	@Override
	public void sendChannelReport(List<JSONObject> channelMessages,
			UserDetails reporter, int channelID, String message) throws JSONException {
    	JSONObject report = new JSONObject();
    	report.put("reporter", reporter.getUserID());
    	report.put("channelID", channelID);
    	report.put("channelMessages", channelMessages);
    	report.put("userMessage", message);
    	this.channelReportCue.add(report);
	}

	@Override
	public void sendUserReport(List<JSONObject> userMessages,
			JSONObject report, String message) throws JSONException {
		throw new UnsupportedOperationException("ReportDatabaseManager.sendUserReport() has not yet been implemented.");
	}

}
