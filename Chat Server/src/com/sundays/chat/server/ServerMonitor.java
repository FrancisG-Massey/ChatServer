package com.sundays.chat.server;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ServerMonitor implements ServletContextListener {

	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		System.out.println("Shutting down ChatServer...");
		try {
			ChatServer.getInstance().shutdown();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		System.out.println("Starting server...");
	}

}
