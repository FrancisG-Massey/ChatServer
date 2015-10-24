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
package com.sundays.chat.io;

import java.io.IOException;

/**
 * The persistence layer interface for fetching and storing user information.<br /><br />
 * 
 * Implementations of this interface should implement buffering and caching where applicable.
 * 
 * @author Francis
 */
public interface UserDataSave extends AutoCloseable {
	
	/**
	 * Stores data for a newly created user
	 * @param username The desired username
	 * @param hashedPassword The desired password, in encrypted form using a character array
	 * @return An integer representing the user ID of the new user
	 * @throws IOException If something goes wrong with the creation of the account
	 */
	public int createUser (String username, char[] hashedPassword) throws IOException;
	
	/**
	 * Saves the data for the specified user.
	 * @param user The details for the user which require saving
	 * @throws IOException If an error occurs during the saving of user data.
	 */
	public void saveUserData (UserDetails user) throws IOException;
	
	/**
	 * Fetches the ID of the user with the specified username.<br />
	 * Returns -1 if no user exists with the specified name. <br />
	 * This search should be case-insensitive, so "TEST" should return the same result as "test".
	 * @param username The username used as the lookup key
	 * @return The ID of the user who has the specified name, or -1 if no user exists.
	 * @throws IOException If an error occurs while looking up the user.
	 */
	public int lookupByUsername (String username) throws IOException;
	
	public UserDetails getUserDetails (int id) throws IOException;
	
	/**
	 * Checks whether a user exists with the provided user ID
	 * @param id The ID of the user to check
	 * @return true if the user exists, false otherwise
	 * @throws IOException If an error occurs during the existance check.
	 */
	public boolean userExists (int id) throws IOException;
	
	public void commitChanges () throws IOException;
}
