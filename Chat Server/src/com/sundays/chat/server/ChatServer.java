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
import com.sundays.chat.server.channel.ChannelAPI;
import com.sundays.chat.server.channel.ChannelManager;
import com.sundays.chat.server.user.UserManager;

/**
 * 
 * @author Francis
 */
public final class ChatServer {

	private static ChatServer instance;
	
	private UserManager userManager;
	private IOManager ioManager;
	private ChannelManager channelManager;
	private ChannelAPI channelAPI;
	private ServerTaskQueue serverTaskScheduler;
	public boolean initalised = false;

	public static ChatServer getInstance() {
		if (instance == null) {
			instance = new ChatServer();
		}
		return instance;
	}

	private ChatServer() {
		
	}

	@SuppressWarnings("unchecked")
	public void init(ServletConfig config) throws ServletException {
		if (initalised) {
			return;
		}
		ServletContext context = config.getServletContext();
		context.log("Java chat server (beta). Version " + Settings.VERSION_NAME);
		
		Properties properties = new Properties();
		BasicConfigurator.configure();
		
		String configFile = config.getInitParameter("configFile");
		if (configFile == null) {
			context.log("No configuration file specified; Using default configuration located at /WEB-INF/default.properties.");
			context.log("A different configuration can be specified by passing the 'configFile' init parameter.");
			configFile = "/WEB-INF/default.properties";
		} else {
			context.log("Loading server properties from "+configFile);
		}
		InputStream cfgFile = context.getResourceAsStream(configFile);
		
		try {
			properties.load(cfgFile);
		} catch (IOException e) {
			context.log("Failed to load application configuration file; using built-in defaults", e);
		}
		String ioClassname = properties.getProperty("io.class");
		if (ioClassname == null) {
			throw new ServletException("IO manager (io.class) not specified in properties!");
		}
		Class<? extends IOManager> ioClass;
		try {
			ioClass = (Class<? extends IOManager>) Class.forName(ioClassname);
		} catch (ClassNotFoundException | ClassCastException ex) {
			throw new ServletException("IO manager class not found or does not extend IOManager.", ex);
		}
		try {
			ioManager = ioClass.newInstance();
		} catch (InstantiationException | IllegalAccessException ex) {
			throw new ServletException("Error instanciating IO manager class.", ex);
		}
		try {
			ioManager.init(properties);
		} catch (Exception ex) {
			throw new ServletException("Error initialising IO manager.", ex);
		}
		context.log("Successfully initialised ChatServer IO manager "+ioManager.getClass().getName());
	}

	public IOManager getIO() {
		if (ioManager == null) {
			throw new IllegalStateException("IO manager not initialised!");
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
		if (ioManager != null) {
			ioManager.close();// Shutdown the persistence layer resources (to prevent exceptions from being thrown).
		}
		this.serverTaskScheduler().shutdown();// Runs all pending server tasks
												// instantly then shuts down the
												// timer cue
	}
}
