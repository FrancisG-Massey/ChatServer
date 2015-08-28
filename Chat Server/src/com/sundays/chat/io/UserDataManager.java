package com.sundays.chat.io;

import java.io.IOException;

/**
 * The link between the user management system and the persistence layer of the application
 * Used to store and fetch permanent data relating to user management
 * 
 * @author Francis
 */
public interface UserDataManager {
	
	/**
	 * Stores data for a newly created user
	 * @param username The desired username
	 * @param hashedPassword The desired password, in encrypted form using a character array
	 * @return An int representing the user ID of the new user
	 * @throws IOException If something goes wrong with the creation of the account
	 */
	public int createUser (String username, char[] hashedPassword) throws IOException;
	
	public void saveUserData (UserDetails user) throws IOException;
	
	public int lookupByUsername (String username) throws IOException;
	
	public UserDetails getUserDetails (int id) throws IOException;
	
	public boolean userExists (int id) throws IOException;
}
