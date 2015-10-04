package com.sundays.chat.server.channel.dummy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sundays.chat.io.UserDetails;
import com.sundays.chat.server.user.User;
import com.sundays.chat.server.user.UserLookup;

public class DummyUserManager implements UserLookup {
	
	public List<CallEvent> calls = new ArrayList<>();
	
	public Map<Integer, String> nameLookup = new HashMap<>();
	public Map<String, Integer> userIdLookup = new HashMap<>();

	public DummyUserManager() {
		
	}

	@Override
	public User getUser(int userID) {
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
