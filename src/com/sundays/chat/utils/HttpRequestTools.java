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
package com.sundays.chat.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

public class HttpRequestTools {

	public static void sendResponseJSON (HttpServletResponse response, JSONObject responseJSON) throws IOException {
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		out.println(responseJSON.toString());
	}
	
	public static JSONObject getRequestJSON (HttpServletRequest request) throws IOException, JSONException {
		BufferedReader reader = request.getReader();
		StringBuilder sb = new StringBuilder();
		String line = reader.readLine();
		while (line != null) {
	        sb.append(line + "\n");
	        line = reader.readLine();
	    }
	    reader.close();
	    String data = sb.toString();
		return new JSONObject(data);
	}
}
