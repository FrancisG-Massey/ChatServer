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
/**
 * Provides interfaces and classes necessary to save and load permanent data for the ChatServer application.<br /><br />
 * 
 * The persistence layer is attached to the application via an implementation of the {@link com.sundays.chat.io.IOManager} class.
 * This package includes both interfaces used to communicate with the persistence layer and concrete classes which are used by these interfaces to send data between the application and the persistence layer.
 */
package com.sundays.chat.io;