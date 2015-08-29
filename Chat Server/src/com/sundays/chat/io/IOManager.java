/*******************************************************************************
 * Copyright (c) 2015 Francis G.
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

/**
 * An interface used to centralise all different communication interfaces for the persistence layer.
 * 
 * Classes which implement this interface should ensure communication interfaces returned when methods are called remain <b>the same for every call to the method</b>, as the application is not guaranteed to only call each method once.
 * 
 * @author Francis
 */
public interface IOManager extends AutoCloseable {
	
	/**
	 * Returns the IO interface for loading and saving user data
	 * @return the {@linkplain UserDataManager} implementation for the persistance layer
	 */
	public UserDataManager getUserIO();
	
	/**
	 * Returns the IO interface for finding channels
	 * @return the {@linkplain ChannelIndex} implementation for the persistance layer
	 */
	public ChannelIndex getChannelIndex();
	
	/**
	 * Returns the IO interface for loading and saving channel data
	 * @return the {@linkplain ChannelDataManager} implementation for the persistance layer
	 */
	public ChannelDataManager getChannelIO();

}
