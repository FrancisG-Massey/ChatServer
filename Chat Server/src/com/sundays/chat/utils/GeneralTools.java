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
package com.sundays.chat.utils;

public final class GeneralTools {
	
	public static boolean isPositiveInteger (String s) {
		return (isInteger(s.trim(), 10) && s.charAt(0) != '-');
	}
	
	public static boolean isInteger (String s) {
		return isInteger(s.trim(), 10);
	}

	public static boolean isInteger (String s, int radix) {
		//Code retrieved from: http://stackoverflow.com/questions/5439529/determine-if-a-string-is-an-integer-in-java
		if(s.isEmpty()) return false;
		    for(int i = 0; i < s.length(); i++) {
		        if(i == 0 && s.charAt(i) == '-') {
		            if(s.length() == 1) return false;
		            else continue;
		        }
		        if(Character.digit(s.charAt(i),radix) < 0) return false;
		    }
	    return true;
	}
}
