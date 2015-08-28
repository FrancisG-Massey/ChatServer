package com.sundays.chat.io.database;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.sundays.chat.interfaces.ReportBackend;
import com.sundays.chat.server.User;

public class ReportDatabaseManager implements ReportBackend {

    //A list which contains any channel reports to be sent to a global moderator
    private final LinkedList<JSONObject> channelReportCue = new LinkedList<JSONObject>();
	
	@Override
	public void sendChannelReport(List<JSONObject> channelMessages,
			User reporter, int channelID, String message) throws JSONException {
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
