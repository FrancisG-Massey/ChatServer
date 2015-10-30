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
import static org.junit.Assume.*;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sundays.chat.io.ChannelGroupType;
import com.sundays.chat.io.UserDetails;
import com.sundays.chat.server.Settings;
import com.sundays.chat.server.channel.dummy.DummyChannelDataIO;
import com.sundays.chat.server.channel.dummy.DummyUserManager;
import com.sundays.chat.server.message.MessagePayload;
import com.sundays.chat.server.user.User;

public class MessageFactoryTest {
	
	ChannelMessageFactory factory;
	Channel dummyChannel;
	User testUser;
	DummyUserManager userLookup;

	@Before
	public void setUp() throws Exception {
		factory = new ChannelMessageFactory();
		dummyChannel = new Channel(100, new DummyChannelDataIO());
		UserDetails details = new UserDetails(102, "Test", 0);
		testUser = new User(102, details);
		userLookup = new DummyUserManager();
	}

	@After
	public void tearDown() throws Exception {
		factory = null;
		userLookup = null;
	}
	
	@Test
	public void testDetailMessage () {
		dummyChannel.setName("Test Channel");
		dummyChannel.setWelcomeMessage("Test Message");
		dummyChannel.setOwnerID(102);
		userLookup.nameLookup.put(102, "Test");
		
		assumeTrue("Test Channel".equals(dummyChannel.getName()));		
		assumeTrue("Test Message".equals(dummyChannel.getWelcomeMessage()));
		assumeTrue(102 == dummyChannel.getOwnerID());
		
		MessagePayload message = factory.createDetailsMessage(dummyChannel, userLookup);
		
		assertEquals("Test Channel", message.get("name"));
		assertEquals("Test Message", message.get("openingMessage"));
		
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
			assertEquals(Settings.GUEST_RANK, userMessage.get("rank"));
		}
	}

	@Test
	public void testUserAddition() {
		dummyChannel.addUser(testUser);
		
		assumeTrue(dummyChannel.getUsers().contains(testUser));
		
		MessagePayload message = factory.createChannelUserAddition(testUser, dummyChannel);
		
		assertEquals(102, message.get("userID"));
		assertEquals("Test", message.get("username"));
		assertEquals(Settings.GUEST_RANK, message.get("rank"));
	}

	@Test
	public void testUserUpdate() {
		dummyChannel.addUser(testUser);
		
		assumeTrue(dummyChannel.getUsers().contains(testUser));
		
		MessagePayload message = factory.createChannelUserUpdate(testUser, dummyChannel);
		
		assertEquals(102, message.get("userID"));
		assertEquals("Test", message.get("username"));
		assertEquals(Settings.GUEST_RANK, message.get("rank"));
	}

	@Test
	public void testUserRemoval() {
		MessagePayload message = factory.createChannelUserRemoval(testUser, dummyChannel);
		assertEquals(102, message.get("userID"));
	}
	
	@Test
	public void testMemberList () {
		for (int i=0;i<10;i++) {
			//UserDetails details = new UserDetails(110+i, "Test"+i, 0);
			//dummyChannel.addUser(new User(110+i, details));
			dummyChannel.addMember(110+i);
			userLookup.nameLookup.put(110+i, "Test"+i);
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
			assertEquals(Settings.DEFAULT_RANK, memberMessage.get("rank"));
		}
	}

	@Test
	public void testMemberAddition() {
		dummyChannel.addMember(102);
		userLookup.nameLookup.put(102, "Test");
		
		assumeTrue(dummyChannel.getUserRank(102) == Settings.DEFAULT_RANK);
		
		
		MessagePayload message = factory.createRankListAddition(102, dummyChannel, userLookup);
		
		assertEquals(102, message.get("userID"));
		assertEquals("Test", message.get("username"));
		assertEquals(Settings.DEFAULT_RANK, message.get("rank"));
	}

	@Test
	public void testMemberUpdate() {
		dummyChannel.addMember(102);
		dummyChannel.setMemberGroup(102, Settings.ADMIN_RANK);
		userLookup.nameLookup.put(102, "Test");
		
		assumeTrue(dummyChannel.getUserRank(102) == Settings.ADMIN_RANK);
		
		
		MessagePayload message = factory.createRankListUpdate(102, dummyChannel, userLookup);
		
		assertEquals(102, message.get("userID"));
		assertEquals("Test", message.get("username"));
		assertEquals(Settings.ADMIN_RANK, message.get("rank"));
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
			userLookup.nameLookup.put(110+i, "Test"+i);
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
		userLookup.nameLookup.put(102, "Test");
		
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
