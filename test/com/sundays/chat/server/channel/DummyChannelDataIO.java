package com.sundays.chat.server.channel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sundays.chat.io.ChannelDataManager;
import com.sundays.chat.io.ChannelDetails;
import com.sundays.chat.io.ChannelGroupData;

public class DummyChannelDataIO implements ChannelDataManager {
	
	public static class CallEvent {
		public String method;
		public List<Object> args;
		
		private CallEvent(String method, Object... args) {
			this.method = method;
			this.args = Arrays.asList(args);
		}
	}
	
	public List<CallEvent> calls = new ArrayList<>();

	public DummyChannelDataIO() {
		
	}

	@Override
	public void addRank(int channelID, int userID) {
		calls.add(new CallEvent("addRank", channelID, userID));
	}

	@Override
	public void changeRank(int channelID, int userID, int rankID) {
		calls.add(new CallEvent("changeRank", channelID, userID, rankID));
	}

	@Override
	public void removeRank(int channelID, int userID) {
		calls.add(new CallEvent("removeRank", channelID, userID));
	}

	@Override
	public void addBan(int channelID, int userID) {
		calls.add(new CallEvent("addBan", channelID, userID));
	}

	@Override
	public void removeBan(int channelID, int userID) {
		calls.add(new CallEvent("removeBan", channelID, userID));
	}

	@Override
	public void addGroup(int channelID, ChannelGroupData group) {
		calls.add(new CallEvent("addGroup", channelID, group));
	}

	@Override
	public void updateGroup(int channelID, ChannelGroupData group) {
		calls.add(new CallEvent("updateGroup", channelID, group));
	}

	@Override
	public void removeGroup(int channelID, int groupID) {
		calls.add(new CallEvent("removeGroup", channelID, groupID));
	}

	@Override
	public void syncDetails(int channelID, ChannelDetails details) {
		calls.add(new CallEvent("syncDetails", channelID, details));
	}

	@Override
	public ChannelDetails getChannelDetails(int channelID) {
		calls.add(new CallEvent("getChannelDetails", channelID));
		return new ChannelDetails();
	}

	@Override
	public List<Integer> getChannelBans(int channelID) {
		calls.add(new CallEvent("getChannelBans", channelID));
		return new ArrayList<>();
	}

	@Override
	public Map<Integer, Byte> getChannelRanks(int channelID) {
		calls.add(new CallEvent("getChannelRanks", channelID));
		return new HashMap<>();
	}

	@Override
	public List<ChannelGroupData> getChannelGroups(int channelID) {
		calls.add(new CallEvent("getChannelGroups", channelID));
		return new ArrayList<>();
	}

	@Override
	public void commitChanges() throws IOException {
		calls.add(new CallEvent("commitChanges"));
		
	}

}
