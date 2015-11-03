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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sundays.chat.io.UserDetails;
import com.sundays.chat.server.channel.ChannelUser;
import com.sundays.chat.server.user.User;
import com.sundays.chat.server.user.UserLookup;

public class DummyUserManager implements UserLookup {
	
	public List<CallEvent> calls = new ArrayList<>();
	
	public Map<Integer, String> nameLookup = new HashMap<>();
	public Map<String, Integer> userIdLookup = new HashMap<>();

	public DummyUserManager() {
		
	}

	@Override
	public ChannelUser getUser(int userID) {
		calls.add(new CallEvent("getUser", userID));
		return new User(userID, new UserDetails());
	}

	@Override
	public String getUsername(int userID) {
		calls.add(new CallEvent("getUsername", userID));
		return nameLookup.get(userID);
	}

	@Override
	public int getUserID(String username) {
		calls.add(new CallEvent("getUserID", username));
		return userIdLookup.get(username);
	}

}
