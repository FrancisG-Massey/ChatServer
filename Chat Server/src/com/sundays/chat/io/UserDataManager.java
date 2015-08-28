/*******************************************************************************
 * Copyright (c) 2015 Francis G.
 *
 * This file is part of ${enclosing_project}.
 *
 * ${enclosing_project} is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ${enclosing_project} is distributed in the hope that it will be useful,
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
