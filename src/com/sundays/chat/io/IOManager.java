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

import java.util.Properties;

import com.sundays.chat.utils.ConfigurationException;

/**
 * An interface used to centralise all different communication interfaces for the persistence layer.
 * 
 * Classes which implement this interface should ensure communication interfaces returned when methods are called remain <b>the same for every call to the method</b>, as the application is not guaranteed to only call each method once.
 * 
 * @author Francis
 */
public interface IOManager extends AutoCloseable {
	
	/**
	 * Initialises the IO Manager with the specified properties.<br />
	 * This method should only be called once, and should be used to initialise all IO managers linked to this class.
	 * @param properties The properties used to initialise the IO manager, such as usernames, passwords, filenames, etc.
	 * @throws ConfigurationException If there was an issue with the configuration provided (such as missing or invalid items)
	 * @throws Exception If there was any other issue with the initialisation of the IO manager
	 */
	public void init (Properties properties) throws ConfigurationException, Exception;
	
	/**
	 * Returns the IO interface for loading and saving user data
	 * @return the {@linkplain UserDataManager} implementation for the persistance layer
	 * @throws IllegalStateException If this method was called before {@link #init(Properties)}, or if an exception thrown during initialisation prevented the user IO manager from initialising
	 */
	public UserDataManager getUserIO() throws IllegalStateException;
	
	/**
	 * Returns the IO interface for finding channels
	 * @return the {@linkplain ChannelIndex} implementation for the persistance layer
	 * @throws IllegalStateException If this method was called before {@link #init(Properties)}, or if an exception thrown during initialisation prevented the user IO manager from initialising
	 */
	public ChannelIndex getChannelIndex() throws IllegalStateException;
	
	/**
	 * Returns the IO interface for loading and saving channel data
	 * @return the {@linkplain ChannelDataManager} implementation for the persistance layer
	 * @throws IllegalStateException If this method was called before {@link #init(Properties)}, or if an exception thrown during initialisation prevented the user IO manager from initialising
	 */
	public ChannelDataManager getChannelIO() throws IllegalStateException;

}
