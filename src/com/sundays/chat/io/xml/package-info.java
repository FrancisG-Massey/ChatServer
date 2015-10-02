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
 * An implementation of the ChatServer persistence layer which uses XML files to store and retrieve data.<br /><br />
 * 
 * This implementation is designed for smaller, portable applications, where the ability to move the application data is more important than the ability to sustain large numbers of users.<br />
 * If it is necessary to sustain a large number of users and channels, the {@link com.sundays.chat.io.jdbc JDBC persistence layer} should be used instead.
 */
package com.sundays.chat.io.xml;