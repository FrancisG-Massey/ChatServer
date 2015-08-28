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
