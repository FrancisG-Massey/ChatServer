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

/**
 * An interface used between the server layer and the persistence layer to return the relevant IO managers for the percistance layer used.
 * @author Francis
 */
public interface IOManager {
	
	public UserDataManager getUserIO();
	
	public ChannelIndex getChannelIndex();
	
	public ChannelDataManager getChannelIO();
	
	public void shutdown() throws Exception;

}
