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

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sundays.chat.io.ChannelDataIO;

public class ChannelSyncTest {
	
	private ChannelDataIO channelIO;
	private Channel channel;

	@Before
	public void setUp() throws Exception {
		channelIO = mock(ChannelDataIO.class);
		channel = new Channel(100, channelIO);
	}

	@After
	public void tearDown() throws Exception {
		channelIO = null;
		channel = null;
	}

	@Test
	public void testAddMember() throws IOException {
		channel.addMember(102, ChannelGroup.DEFAULT_GROUP);
		assumeTrue(channel.getUserGroup(102).getId() == ChannelGroup.DEFAULT_GROUP);//Assume the addition succeeded
		
		verify(channelIO).addMember(100, 102, ChannelGroup.DEFAULT_GROUP);
	}

	@Test
	public void testUpdateMember() throws IOException {
		channel.addMember(102, ChannelGroup.DEFAULT_GROUP);
		assumeTrue(channel.getUserGroup(102).getId() == ChannelGroup.DEFAULT_GROUP);
		
		channel.setMemberGroup(102, ChannelGroup.MOD_GROUP);
		assumeTrue(channel.getUserGroup(102).getId() == ChannelGroup.MOD_GROUP);//Assume the change succeeded

		verify(channelIO).updateMember(100, 102, ChannelGroup.MOD_GROUP);
	}

	@Test
	public void testRemoveMember() throws IOException {
		channel.addMember(102, ChannelGroup.DEFAULT_GROUP);
		assumeTrue(channel.getUserGroup(102).getId() == ChannelGroup.DEFAULT_GROUP);
		
		channel.removeMember(102);
		assumeTrue(channel.getUserGroup(102).getId() == ChannelGroup.GUEST_GROUP);//Assume the removal succeeded

		verify(channelIO).removeMember(100, 102);
	}

	@Test
	public void testAddBan() throws IOException {
		channel.addBan(102);
		assumeTrue(channel.isUserBanned(102));//Assume the ban addition succeeded
		
		verify(channelIO).addBan(100, 102);
	}

	@Test
	public void testRemoveBan() throws IOException {
		channel.addBan(102);
		assumeTrue(channel.isUserBanned(102));
		
		channel.removeBan(102);
		assumeFalse(channel.isUserBanned(102));//Assume the removal succeeded
		
		verify(channelIO).removeBan(100, 102);
	}

}
