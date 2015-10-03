package com.sundays.chat.server.user;

/**
 * 
 * 
 * @author Francis
 */
public interface UserLookup {
	
	public User getUser (int userID);
	
	public String getUsername (int userID);
	
	public int getUserID (String username);

}
