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
package com.sundays.chat.server.channel;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChannelCleanup extends Timer {

	CopyOnWriteArrayList<Runnable> tasks = new CopyOnWriteArrayList<Runnable>();
	TimerTask currentTask = null;
	
	protected ChannelCleanup () {
		super("ChannelCleanup");
	}
	
	protected void addCleanupTask (Runnable task) {
		tasks.add(task);
	}
	
	protected void scheduleTasks (long delay, long rate) {
		synchronized (this) {
			if (currentTask != null) {
				currentTask.cancel();
			}		
			currentTask = new TimerTask () {
				@Override
				public void run() {
					Logger.getLogger(this.getClass().getName()).log(Level.INFO, "Running channel cleanup tasks...");
					for (Runnable task : tasks) {
						task.run();
					}
				}
			};
			this.scheduleAtFixedRate(currentTask, delay, rate);
		}
	}
}
