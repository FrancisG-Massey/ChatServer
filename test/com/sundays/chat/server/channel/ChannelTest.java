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
package com.sundays.chat.server.channel;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sundays.chat.io.ChannelDataManager;
import com.sundays.chat.io.UserDetails;
import com.sundays.chat.server.Settings;
import com.sundays.chat.server.channel.dummy.DummyChannelDataIO;
import com.sundays.chat.server.user.User;

public class ChannelTest {
	
	private ChannelDataManager channelIO = new DummyChannelDataIO();
	private Channel channel;

	@Before
	public void setUp() throws Exception {
		channel = new Channel(100, channelIO);
	}

	@After
	public void tearDown() throws Exception {
		channel = null;
	}

	@Test
	public void testName() {
		channel.setName("Test Name");
		assertEquals("Test Name", channel.getName());
	}

	@Test
	public void testMessage() {
		channel.setOpeningMessage("Test Opening Message");
		assertEquals("Test Opening Message", channel.getOpeningMessage());
	}

	@Test
	public void testAddMember() {
		channel.addRank(100);
		assertEquals(channel.getUserRank(100), Settings.DEFAULT_RANK);
	}

	@Test
	public void testRemoveMember() {
		channel.addRank(100);
		assumeTrue(channel.getUserRank(100) == Settings.DEFAULT_RANK);
		channel.removeRank(100);
		assertEquals(Settings.GUEST_RANK, channel.getUserRank(100));
	}

	@Test
	public void testUpdateMember() {
		channel.addRank(100);
		assumeTrue(channel.getUserRank(100) == Settings.DEFAULT_RANK);
		channel.setRank(100, Settings.ADMIN_RANK);
		assertEquals(Settings.ADMIN_RANK, channel.getUserRank(100));
	}

	@Test
	public void testAddBan() {
		channel.addBan(100);
		assertTrue(channel.isUserBanned(100));
	}

	@Test
	public void testRemoveBan() {
		channel.addBan(100);
		assumeTrue(channel.isUserBanned(100));
		channel.removeBan(100);
		assertFalse(channel.isUserBanned(100));
	}

	@Test
	public void testAddUser() {
		User u = new User(102, new UserDetails());
		channel.addUser(u);
		assertTrue(channel.getUsers().contains(u));
	}

	@Test
	public void testRemoveUser() {
		User u = new User(102, new UserDetails());
		channel.addUser(u);
		assumeTrue(channel.getUsers().contains(u));
		channel.removeUser(u);
		assertFalse(channel.getUsers().contains(u));		
	}
	
	@Test
	public void testTempBan () {
		channel.setTempBan(102, 20_000);
		assertNotEquals(0L, channel.getBanExpireTime(102));
	}
	
	@Test
	public void testTempBanRemove () {
		channel.setTempBan(102, 20_000);
		assumeTrue(channel.getBanExpireTime(102) != 0L);
		channel.removeTempBan(102);
		assertEquals(0L, channel.getBanExpireTime(102));
	}
	
	@Test
	public void testMessageCounter () {
		int lastMessageID = channel.getNextMessageID();
		assertEquals(lastMessageID+1, channel.getNextMessageID());
	}

}
