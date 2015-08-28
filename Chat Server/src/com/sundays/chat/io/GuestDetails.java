package com.sundays.chat.io;

public class GuestDetails extends UserDetails {
	
	public GuestDetails (int guestID) {
		super(guestID, "Guest "+(Math.abs(guestID)-100), -1);
	}

}
