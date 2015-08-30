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
package com.sundays.chat.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.log4j.BasicConfigurator;

import com.sundays.chat.io.IOManager;
import com.sundays.chat.io.jdbc.JDBCIOManager;
import com.sundays.chat.server.channel.ChannelAPI;
import com.sundays.chat.server.channel.ChannelManager;

public final class ChatServer {

	private static final String DB_USERNAME = "sundays4_chat";
	private static final String DB_PASSWORD = "Sm4fztbVB3CdGwVa";
	private static final String DB_URL = "jdbc:mysql://localhost:3306/sundays4_chat";

	private static ChatServer server;
	private UserManager userManager;
	private IOManager ioManager;
	private ChannelManager channelManager;
	private ChannelAPI channelAPI;
	private ServerTaskQueue serverTaskScheduler;
	public boolean initalised = false;

	public static ChatServer getInstance() {
		if (server == null) {
			server = new ChatServer();
		}
		return server;
	}

	private ChatServer() {
		server = this;
	}

	public void init(ServletConfig config) throws ServletException {
		if (initalised) {
			return;
		}
		ServletContext context = config.getServletContext();
		context.log("Java chat server (beta). Version " + Settings.VERSION_NAME);
		String configFile = config.getInitParameter("configFile");
		if (configFile == null) {
			context.log("No configuration file specified; Using default configuration located at /WEB-INF/default.properties.");
			context.log("A different configuration can be specified by passing the 'configFile' init parameter.");
			configFile = "/WEB-INF/default.properties";
		}
		InputStream cfgFile = context.getResourceAsStream(configFile);
		Properties p = new Properties();
		BasicConfigurator.configure();
		try {
			p.load(cfgFile);
		} catch (IOException e) {
			context.log("Failed to load application configuration file; using built-in defaults", e);
		}
	}

	public IOManager getIO() {
		if (ioManager == null) {
			ioManager = new JDBCIOManager(DB_URL, DB_USERNAME, DB_PASSWORD);
		}
		return ioManager;
	}

	public ServerTaskQueue serverTaskScheduler() {
		if (serverTaskScheduler == null) {
			serverTaskScheduler = new ServerTaskQueue();
		}
		return serverTaskScheduler;
	}

	public ChannelManager channelManager() {
		if (channelManager == null) {
			channelManager = new ChannelManager(getIO().getChannelIO(), getIO().getChannelIndex());
		}
		return channelManager;
	}

	public ChannelAPI channelAPI() {
		if (channelAPI == null) {
			channelAPI = new ChannelAPI(channelManager());
		}
		return channelAPI;
	}

	public UserManager userManager() {
		if (userManager == null) {
			userManager = new UserManager(getIO().getUserIO());
		}
		return userManager;
	}

	public void shutdown() throws Exception {// Clean up resources here
		this.serverTaskScheduler().lock();// Prevents new tasks from being cued
		// channelManager().shutdown();//Run the final cleanup tasks
		this.getIO().close();// Shutdown the persistence layer resources (to
								// prevent exceptions from being thrown).
		this.serverTaskScheduler().shutdown();// Runs all pending server tasks
												// instantly then shuts down the
												// timer cue
	}
}
