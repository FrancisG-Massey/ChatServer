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
package com.sundays.chat.server.channel.dummy;

import java.util.Arrays;
import java.util.List;

public class CallEvent {
	private String method;
	private List<Object> args;

	public CallEvent(String method, Object... args) {
		this.method = method;
		this.args = Arrays.asList(args);
	}

	public String getMethod() {
		return method;
	}

	public List<Object> getArgs() {
		return args;
	}

	public Object getArg(int slot) {
		return args.get(slot);
	}
}