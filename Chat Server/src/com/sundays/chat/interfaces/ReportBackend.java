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
