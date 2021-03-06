/*******************************************************************************
 * Copyright (c) 2013, 2016 Francis G.
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
package com.sundays.chat.server.channel.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sundays.chat.io.ChannelDataIO;
import com.sundays.chat.io.ChannelGroupType;
import com.sundays.chat.io.UserDetails;
import com.sundays.chat.server.channel.ChannelUser;
import com.sundays.chat.server.channel.impl.Channel;
import com.sundays.chat.server.channel.impl.ChannelAttribute;
import com.sundays.chat.server.channel.impl.ChannelGroup;
import com.sundays.chat.server.channel.impl.ChannelPacketFactory;
import com.sundays.chat.server.message.MessagePayload;
import com.sundays.chat.server.user.User;
import com.sundays.chat.server.user.UserLookup;

public class ChannelPacketFactoryTest {
	
	ChannelPacketFactory factory;
	Channel dummyChannel;
	ChannelUser testUser;
	UserLookup userLookup;

	@Before
	public void setUp() throws Exception {
		factory = new ChannelPacketFactory();
		dummyChannel = new Channel(100, mock(ChannelDataIO.class));
		UserDetails details = new UserDetails(102, "Test", 0);
		testUser = new User(102, details);
		userLookup = mock(UserLookup.class);
	}

	@After
	public void tearDown() throws Exception {
		factory = null;
		userLookup = null;
	}
	
	@Test
	public void testDetailMessage () {
		dummyChannel.setName("Test Channel");
		dummyChannel.setAttribute("welcomeMessage", "Test Message");
		dummyChannel.setOwnerID(102);
		when(userLookup.getUsername(102)).thenReturn("Test");
		
		assumeTrue("Test Channel".equals(dummyChannel.getName()));		
		assumeTrue("Test Message".equals(dummyChannel.getAttribute(ChannelAttribute.WELCOME_MESSAGE)));
		assumeTrue(102 == dummyChannel.getOwnerID());
		
		MessagePayload message = factory.createDetailsMessage(dummyChannel, userLookup);
		
		assertEquals("Test Channel", message.get("name"));
		assertEquals("Test Message", message.get("welcomeMessage"));
		
		MessagePayload owner = (MessagePayload) message.get("owner");
		assertEquals(102, owner.get("id"));
		assertEquals("Test", owner.get("name"));
	}
	
	@Test
	public void testGroupMessage () {
		ChannelGroup group = new ChannelGroup(100, 1, "Member", ChannelGroupType.NORMAL);
		group.setIconUrl("http://example.com/icon.png");
		MessagePayload message = factory.createGroupDetails(group);
		assertEquals("Member", message.get("name"));
		assertEquals("http://example.com/icon.png", message.get("icon"));
		assertEquals(ChannelGroupType.NORMAL, message.get("type"));
	}
	
	@Test
	public void testUserList () {
		for (int i=0;i<10;i++) {
			UserDetails details = new UserDetails(110+i, "Test"+i, 0);
			dummyChannel.addUser(new User(110+i, details));
		}
		assumeTrue(dummyChannel.getUsers().size() == 10);//Assume all users were added properly.
		
		MessagePayload message = factory.createChannelUserList(dummyChannel);
		assertEquals(100, message.get("id"));
		assertEquals(10, message.get("totalUsers"));
		
		@SuppressWarnings("unchecked")
		List<MessagePayload> userList = (List<MessagePayload>) message.get("users");
		
		for (MessagePayload userMessage : userList) {
			//Get the user ID, since the order of the user list is not guaranteed
			int i = ((Integer) userMessage.get("userID"))-110;
			assertEquals("Test"+i, userMessage.get("username"));
			assertEquals(ChannelGroup.GUEST_GROUP, ((MessagePayload) userMessage.get("group")).get("id"));
		}
	}

	@Test
	public void testUserAddition() {
		dummyChannel.addUser(testUser);
		
		assumeTrue(dummyChannel.getUsers().contains(testUser));
		
		MessagePayload message = factory.createChannelUserAddition(testUser, dummyChannel);
		
		assertEquals(102, message.get("userID"));
		assertEquals("Test", message.get("username"));
		assertEquals(ChannelGroup.GUEST_GROUP, ((MessagePayload) message.get("group")).get("id"));
	}

	@Test
	public void testUserUpdate() {
		dummyChannel.addUser(testUser);
		
		assumeTrue(dummyChannel.getUsers().contains(testUser));
		
		MessagePayload message = factory.createChannelUserUpdate(testUser, dummyChannel);
		
		assertEquals(102, message.get("userID"));
		assertEquals("Test", message.get("username"));
		assertEquals(ChannelGroup.GUEST_GROUP, message.get("rank"));
	}

	@Test
	public void testUserRemoval() {
		MessagePayload message = factory.createChannelUserRemoval(testUser, dummyChannel);
		assertEquals(102, message.get("userID"));
	}
	
	@Test
	public void testMemberList () {
		for (int i=0;i<10;i++) {
			dummyChannel.addMember(110+i, ChannelGroup.DEFAULT_GROUP);
			when(userLookup.getUsername(110+i)).thenReturn("Test"+i);
		}
		assumeTrue(dummyChannel.getMembers().size() == 10);//Assume all members were added properly.
		
		MessagePayload message = factory.createMemberList(dummyChannel, userLookup);
		assertEquals(100, message.get("id"));
		assertEquals(10, message.get("totalUsers"));
		
		@SuppressWarnings("unchecked")
		List<MessagePayload> memberList = (List<MessagePayload>) message.get("ranks");
		
		for (MessagePayload memberMessage : memberList) {
			//Get the user ID, since the order of the user list is not guaranteed
			int i = ((Integer) memberMessage.get("userID"))-110;
			assertEquals("Test"+i, memberMessage.get("username"));
			assertEquals(ChannelGroup.DEFAULT_GROUP, memberMessage.get("rank"));
		}
	}

	@Test
	public void testMemberAddition() {
		dummyChannel.addMember(102, ChannelGroup.DEFAULT_GROUP);
		when(userLookup.getUsername(102)).thenReturn("Test");
		
		assumeTrue(dummyChannel.getUserGroup(102).getId() == ChannelGroup.DEFAULT_GROUP);
		
		
		MessagePayload message = factory.createRankListAddition(102, dummyChannel, userLookup);
		
		assertEquals(102, message.get("userID"));
		assertEquals("Test", message.get("username"));
		assertEquals(ChannelGroup.DEFAULT_GROUP, message.get("rank"));
	}

	@Test
	public void testMemberUpdate() {
		dummyChannel.addMember(102, ChannelGroup.DEFAULT_GROUP);
		dummyChannel.setMemberGroup(102, ChannelGroup.ADMIN_GROUP);
		when(userLookup.getUsername(102)).thenReturn("Test");
		
		assumeTrue(dummyChannel.getUserGroup(102).getId() == ChannelGroup.ADMIN_GROUP);
		
		
		MessagePayload message = factory.createRankListUpdate(102, dummyChannel, userLookup);
		
		assertEquals(102, message.get("userID"));
		assertEquals("Test", message.get("username"));
		assertEquals(ChannelGroup.ADMIN_GROUP, message.get("rank"));
	}

	@Test
	public void testMemberRemoval() {		
		MessagePayload message = factory.createRankListRemoval(102, dummyChannel);
		
		assertEquals(102, message.get("userID"));
	}
	
	@Test
	public void testBanList () {
		for (int i=0;i<10;i++) {
			dummyChannel.addBan(110+i);
			when(userLookup.getUsername(110+i)).thenReturn("Test"+i);
		}
		assumeTrue(dummyChannel.getBans().size() == 10);//Assume all bans were added properly.
		
		MessagePayload message = factory.createBanList(dummyChannel, userLookup);
		assertEquals(100, message.get("id"));
		assertEquals(10, message.get("totalBans"));
		
		@SuppressWarnings("unchecked")
		List<MessagePayload> memberList = (List<MessagePayload>) message.get("bans");
		
		for (MessagePayload memberMessage : memberList) {
			//Get the user ID, since the order of the user list is not guaranteed
			int i = ((Integer) memberMessage.get("userID"))-110;
			assertEquals("Test"+i, memberMessage.get("username"));
		}
	}

	@Test
	public void testBanAddition() {
		dummyChannel.addBan(102);
		when(userLookup.getUsername(102)).thenReturn("Test");
		
		assumeTrue(dummyChannel.isUserBanned(102));		
		
		MessagePayload message = factory.createBanListAddition(102, dummyChannel, userLookup);
		
		assertEquals(102, message.get("userID"));
		assertEquals("Test", message.get("username"));
	}

	@Test
	public void testBanRemoval() {		
		MessagePayload message = factory.createBanListRemoval(102, dummyChannel);
		
		assertEquals(102, message.get("userID"));
	}	

}
