/*******************************************************************************
 * Copyright (c) 2015 Francis G.
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
import static org.junit.Assume.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sundays.chat.server.Settings;
import com.sundays.chat.server.channel.dummy.CallEvent;
import com.sundays.chat.server.channel.dummy.DummyChannelDataIO;

public class ChannelSyncTest {
	
	private DummyChannelDataIO channelIO;
	private Channel channel;

	@Before
	public void setUp() throws Exception {
		channelIO = new DummyChannelDataIO();
		channel = new Channel(100, channelIO);
	}

	@After
	public void tearDown() throws Exception {
		channelIO = null;
		channel = null;
	}

	@Test
	public void testAddMember() {
		channel.addRank(102);
		assumeTrue(channel.getUserRank(102) == Settings.DEFAULT_RANK);//Assume the addition succeeded
		
		CallEvent call = channelIO.calls.get(0);
		assertEquals("addRank", call.getMethod());
		assertEquals("channelID in io call does not match actual channel ID! Found: "+call.getArg(0), 100, call.getArg(0));
		assertEquals("userID in io call does not match actual user ID! Found: "+call.getArg(1), 102, call.getArg(1));
	}

	@Test
	public void testUpdateMember() {
		channel.addRank(102);
		assumeTrue(channel.getUserRank(102) == Settings.DEFAULT_RANK);
		channelIO.calls.clear();//Remove the "addRank" event, as this is tested in a different test case
		
		channel.setRank(102, Settings.MOD_RANK);
		assumeTrue(channel.getUserRank(102) == Settings.MOD_RANK);//Assume the change succeeded
		CallEvent call = channelIO.calls.get(0);
		assertEquals("changeRank", call.getMethod());
		assertEquals("channelID in io call does not match actual channel ID! Found: "+call.getArg(0), 100, call.getArg(0));
		assertEquals("userID in io call does not match actual user ID! Found: "+call.getArg(1), 102, call.getArg(1));
		assertEquals("rankID in io call does not match actual rank ID! Found: "+call.getArg(2), Settings.MOD_RANK, call.getArg(2));
	}

	@Test
	public void testRemoveMember() {
		channel.addRank(102);
		assumeTrue(channel.getUserRank(102) == Settings.DEFAULT_RANK);
		channelIO.calls.clear();//Remove the "addRank" event, as this is tested in a different test case
		
		channel.removeRank(102);
		assumeTrue(channel.getUserRank(102) == Settings.GUEST_RANK);//Assume the removal succeeded
		CallEvent call = channelIO.calls.get(0);
		assertEquals("removeRank", call.getMethod());
		assertEquals("channelID in io call does not match actual channel ID! Found: "+call.getArg(0), 100, call.getArg(0));
		assertEquals("userID in io call does not match actual user ID! Found: "+call.getArg(1), 102, call.getArg(1));
	}

	@Test
	public void testAddBan() {
		channel.addBan(102);
		assumeTrue(channel.isUserBanned(102));//Assume the ban addition succeeded
		
		CallEvent call = channelIO.calls.get(0);
		assertEquals("addBan", call.getMethod());
		assertEquals("channelID in io call does not match actual channel ID! Found: "+call.getArg(0), 100, call.getArg(0));
		assertEquals("userID in io call does not match actual user ID! Found: "+call.getArg(1), 102, call.getArg(1));
	}

	@Test
	public void testRemoveBan() {
		channel.addBan(102);
		assumeTrue(channel.isUserBanned(102));
		channelIO.calls.clear();//Remove the "addBan" event, as this is tested in a different test case
		
		channel.removeBan(102);
		assumeFalse(channel.isUserBanned(102));//Assume the removal succeeded
		CallEvent call = channelIO.calls.get(0);
		assertEquals("removeBan", call.getMethod());
		assertEquals("channelID in io call does not match actual channel ID! Found: "+call.getArg(0), 100, call.getArg(0));
		assertEquals("userID in io call does not match actual user ID! Found: "+call.getArg(1), 102, call.getArg(1));
	}

}
