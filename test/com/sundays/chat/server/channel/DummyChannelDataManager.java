package com.sundays.chat.server.channel;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.sundays.chat.io.ChannelDataManager;
import com.sundays.chat.io.ChannelDetails;
import com.sundays.chat.io.ChannelGroupData;

public class DummyChannelDataManager implements ChannelDataManager {

	public DummyChannelDataManager() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void addRank(int channelID, int userID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void changeRank(int channelID, int userID, int rankID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeRank(int channelID, int userID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addBan(int channelID, int userID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeBan(int channelID, int userID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addGroup(int channelID, ChannelGroupData group) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateGroup(int channelID, ChannelGroupData group) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeGroup(int channelID, int groupID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void syncDetails(int channelID, ChannelDetails details) {
		// TODO Auto-generated method stub

	}

	@Override
	public ChannelDetails getChannelDetails(int channelID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Integer> getChannelBans(int channelID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Integer, Byte> getChannelRanks(int channelID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ChannelGroupData> getChannelGroups(int channelID) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void commitChanges() throws IOException {
		// TODO Auto-generated method stub

	}

}
