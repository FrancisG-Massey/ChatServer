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
package com.sundays.chat.server.channel.dummy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sundays.chat.io.ChannelDataIO;
import com.sundays.chat.io.ChannelDetails;
import com.sundays.chat.io.ChannelGroupData;

/**
 * A dummy channel IO class. Used in test packages to ensure classes which use the channel IO manager are working correctly.
 * 
 * 
 * 
 * @author Francis
 */
public class DummyChannelDataIO implements ChannelDataIO {
	
	public List<CallEvent> calls = new ArrayList<>();

	public DummyChannelDataIO() {
		
	}

	@Override
	public void addMember(int channelID, int userID, int group) {
		calls.add(new CallEvent("addRank", channelID, userID, group));
	}

	@Override
	public void updateMember(int channelID, int userID, int group) {
		calls.add(new CallEvent("changeRank", channelID, userID, group));
	}

	@Override
	public void removeMember(int channelID, int userID) {
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
	public void updateDetails(int channelID, ChannelDetails details) {
		calls.add(new CallEvent("syncDetails", channelID, details));
	}

	@Override
	public ChannelDetails getChannelDetails(int channelID) {
		calls.add(new CallEvent("getChannelDetails", channelID));
		return new ChannelDetails();
	}

	@Override
	public Set<Integer> getChannelBans(int channelID) {
		calls.add(new CallEvent("getChannelBans", channelID));
		return new HashSet<>();
	}

	@Override
	public Map<Integer, Integer> getChannelMembers(int channelID) {
		calls.add(new CallEvent("getChannelRanks", channelID));
		return new HashMap<>();
	}

	@Override
	public Set<ChannelGroupData> getChannelGroups(int channelID) {
		calls.add(new CallEvent("getChannelGroups", channelID));
		return new HashSet<>();
	}

	@Override
	public void commitChanges() throws IOException {
		calls.add(new CallEvent("commitChanges"));		
	}

	@Override
	public void close() throws Exception {
		calls.add(new CallEvent("close"));
	}

	@Override
	public int createChannel(ChannelDetails details) throws IOException {
		calls.add(new CallEvent("createChannel", details));
		return 0;
	}

	@Override
	public void removeChannel(int channelID) throws IOException {
		calls.add(new CallEvent("removeChannel", channelID));
	}

	@Override
	public Map<String, String> getChannelAttributes(int channelID) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addAttribute(String attribute, String value) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateAttribute(String attribute, String value) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearAttribute(String attribute) throws IOException {
		// TODO Auto-generated method stub
		
	}

}
