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
/**
 * An implementation of the ChatServer persistence layer which uses JDBC to store and retrieve data.<br /><br />
 * 
 * This implementation is designed for heavy use applications where thousands of users and hundreds of channels are in uses simultaneously.
 * However, it is not suited for portable applications. The {@link com.sundays.chat.io.xml XML persistence layer} should be used instead for these cases
 */
package com.sundays.chat.io.jdbc;