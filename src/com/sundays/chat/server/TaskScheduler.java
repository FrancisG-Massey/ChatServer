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
package com.sundays.chat.server;

import java.util.LinkedList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TaskScheduler {

	//The mechanism for which to schedule delayed and repeating tasks
	private final ScheduledThreadPoolExecutor timerPool;
	
	//Contains references to the task objects of all tasks designated a 'normal' priority
	private final CopyOnWriteArrayList<ScheduledFuture<?>> normalTasks = new CopyOnWriteArrayList<ScheduledFuture<?>>();
	
	//Contains references to the task objects of all tasks designated an 'essential' priority
	private final CopyOnWriteArrayList<ScheduledFuture<?>> essentialTasks = new CopyOnWriteArrayList<ScheduledFuture<?>>();
	
	//Contains references to the task objects of all tasks which must be run if the server is shut down
	private final LinkedList<Runnable> shutdownTasks = new LinkedList<Runnable>();
	
	//Specifies whether or not new tasks can be scheduled
	private boolean newTaskLock = false;
	
	public TaskScheduler () {
		timerPool = new ScheduledThreadPoolExecutor(1);
	}
	
	/**
	 * Schedule a repeating task which will be run unless the server deems it unnecessary (eg sleep mode). 
	 * Tasks scheduled via this method may be delayed/stopped, so it is recommended to use this for non-essential repeating tasks.
	 * Defaults to not running the task one last time before cancelling (if possible).
	 * 
	 * @param task            the sequence of commands to be run
	 * @param delay           the period of time before the first command will be sent (in units specified by the 'unit' parameter)
	 * @param rate            the period of time between executions of this task.
	 * @param unit            the units of time in which the 'delay' and 'rate' parameters are given
	 * @return                a ScheduledFuture object with which the task can be cancelled or checked.
	 */
	public ScheduledFuture<?> scheduleStandardTask (Runnable task, long delay, long rate, TimeUnit unit) {
		return this.scheduleEssentialTask(task, delay, rate, unit, false);
	}
	
	/**
	 * Schedule a repeating task which will be run unless the server deems it unnecessary (eg sleep mode). 
	 * Tasks scheduled via this method may be delayed/stopped, so it is recommended to use this for non-essential repeating tasks.
	 * 
	 * @param task            the sequence of commands to be run
	 * @param delay           the period of time before the first command will be sent (in units specified by the 'unit' parameter)
	 * @param rate            the period of time between executions of this task.
	 * @param unit            the units of time in which the 'delay' and 'rate' parameters are given
	 * @param runBeforeCancel whether or not the task should be run before it is cancelled.
	 * @return                a ScheduledFuture object with which the task can be cancelled or checked.
	 */
	public ScheduledFuture<?> scheduleStandardTask (Runnable task, long delay, long rate, TimeUnit unit, boolean runBeforeCancel) {
		if (newTaskLock) {
			return null;
		}
		ScheduledFuture<?> taskObject = timerPool.scheduleAtFixedRate(task, delay, rate, unit);
		normalTasks.add(taskObject);
		if (runBeforeCancel) {
			shutdownTasks.add(task);
		}
		return taskObject;
	}
	
	/**
	 * Schedule a repeating task which will always run unless an exception is thrown in the task object, it is manually cancelled, or the system is shut down.
	 * Tasks scheduled via this method will not be delayed/stopped if deemed necessary by the server (eg entering 'sleep' mode), with the exception of the server shutting down.
	 * Defaults to not running the task one last time before cancelling (if possible).
	 * 
	 * @param task            the sequence of commands to be run
	 * @param delay           the period of time before the first command will be sent (in units specified by the 'unit' parameter)
	 * @param rate            the period of time between executions of this task.
	 * @param unit            the units of time in which the 'delay' and 'rate' parameters are given
	 * @return                a ScheduledFuture object with which the task can be cancelled or checked.
	 */
	public ScheduledFuture<?> scheduleEssentialTask (Runnable task, long delay, long rate, TimeUnit unit) {
		return this.scheduleEssentialTask(task, delay, rate, unit, false);
	}
	
	/**
	 * Schedule a repeating task which will always run unless an exception is thrown in the task object, it is manually cancelled, or the system is shut down.
	 * Tasks scheduled via this method will not be delayed/stopped if deemed necessary by the server (eg entering 'sleep' mode), with the exception of the server shutting down.
	 * 
	 * @param task            the sequence of commands to be run
	 * @param delay           the period of time before the first command will be sent (in units specified by the 'unit' parameter)
	 * @param rate            the period of time between executions of this task.
	 * @param unit            the units of time in which the 'delay' and 'rate' parameters are given
	 * @param runBeforeCancel whether or not the task should be run before it is cancelled.
	 * @return                a ScheduledFuture object with which the task can be cancelled or checked.
	 */
	public ScheduledFuture<?> scheduleEssentialTask (Runnable task, long delay, long rate, TimeUnit unit, boolean runBeforeCancel) {
		if (newTaskLock) {
			return null;
		}
		ScheduledFuture<?> taskObject = timerPool.scheduleAtFixedRate(task, delay, rate, unit);
		essentialTasks.add(taskObject);
		if (runBeforeCancel) {
			shutdownTasks.add(task);
		}
		return taskObject;
	}
	
	/**
	 * Cues a task to be run when the system is shut down. Note that tasks cued here cannot be guaranteed to be run.
	 * Any task that you cue here should not take long to complete, and care should be taken that the task does not rely on resources that may have been closed.
	 * Generally, closing resources should be made low priority so that high-priority tasks can run while the resources are still open.
	 * 
	 * @param task     the task to be run upon server shutdown
	 * @param priority sets the priority of the specified tasks. Higher-priority tasks will be run before lower-priority tasks, while lower-priority tasks may not be run at all.
	 */
	public void addShutdownTask (Runnable task, TaskPriority priority) {
		
	}
	
	/**
	 * Sets a task to be run once in the future. 
	 * If the 'runBeforeShutdown' variable is true, the task will be run immediately if the server is shut down. Otherwise, it may not be run.
	 * 
	 * @param task				the task to run
	 * @param delay				the delay before the task is run
	 * @param unit				the unit of time used to specify the delay
	 * @param runBeforeCancel	whether the task should run immediately if the server is shutdown
	 * @return                 a ScheduledFuture object with which the task can be cancelled or checked.
	 */
	public ScheduledFuture<?> setTimeoutTask (Runnable task, long delay, TimeUnit unit, boolean runBeforeCancel) {
		if (newTaskLock) {
			return null;
		}
		if (runBeforeCancel) {
			shutdownTasks.add(task);
		}
		return timerPool.schedule(task, delay, unit);
	}
	
	/**
	 * 
	 * @author Francis G
	 *
	 */
	public enum TaskPriority {
		ESSENTIAL,
		HIGH,
		NORMAL,
		LOW,
		MINOR
	}
	
	/**
	 * Prevents new tasks from being added to the cue.
	 * Usually called when the system is about to be shut down.
	 */
	public void lock () {
		newTaskLock = true;
	}
	
	/**
	 * Removes the new task lock previously applied by the lock() method.
	 */
	public void unlock () {
		newTaskLock = false;
	}
	
	/**
	 * Checks whether or not new tasks are allowed to be added
	 * 
	 * @return whether or not a newTaskLock is is effect
	 */
	public boolean isLocked () {
		return newTaskLock;
	}
	
	/**
	 * Initiates the shutdown procedure of the task cue, running each repeating task one more time while immediately running any cued tasks, then preventing any new tasks from running.
	 */
	public void shutdown () {
		lock();
		for (Runnable task : shutdownTasks) {
			task.run();
		}
		timerPool.shutdown();
	}
	
	/**
	 * Immediately shuts down the task cue, attempting to halt any currently running tasks while preventing any waiting tasks from being run.
	 * Should only be used for urgent shutdown.
	 */
	protected void immediateShutdown () {
		lock();
		timerPool.shutdownNow();
	}
}
