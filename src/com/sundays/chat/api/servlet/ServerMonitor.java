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
package com.sundays.chat.api.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ServerMonitor implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		event.getServletContext().log("Shutting down ChatServer...");
		try {
			ServletChatServer.getInstance().shutdown();
		} catch (Exception ex) {
			event.getServletContext().log("Error shutting down ChatServer", ex);
		}
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		event.getServletContext().log("Starting server...");
		ServletChatServer.getInstance();
	}

}
