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
package com.sundays.chat.server.message;

import java.io.Serializable;

public class StatusMessage implements Serializable {

	private static final long serialVersionUID = 1931631285566801908L;
	
	private final int code;
	private final String messageTemplate;
	private final Serializable[] args;
	
	
	/**
	 * @param code
	 * @param args
	 * @param messageTemplate
	 */
	public StatusMessage(int code, String messageTemplate, Serializable... args) {
		super();
		this.code = code;
		this.messageTemplate = messageTemplate;
		this.args = args;
	}

	public int getCode() {
		return code;
	}

	public String getMessageTemplate() {
		return messageTemplate;
	}
	
	public Serializable[] getArgs() {
		return args;
	}
}
