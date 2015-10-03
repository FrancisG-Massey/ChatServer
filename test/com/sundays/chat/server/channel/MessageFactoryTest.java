package com.sundays.chat.server.channel;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sundays.chat.io.UserDetails;
import com.sundays.chat.server.Settings;
import com.sundays.chat.server.Settings.GroupType;
import com.sundays.chat.server.channel.dummy.DummyChannelDataIO;
import com.sundays.chat.server.message.MessagePayload;
import com.sundays.chat.server.user.User;

public class MessageFactoryTest {
	
	ChannelMessageFactory factory;
	Channel dummyChannel;
	User testUser;

	@Before
	public void setUp() throws Exception {
		factory = new ChannelMessageFactory();
		dummyChannel = new Channel(100, new DummyChannelDataIO());
		UserDetails details = new UserDetails(102, "Test", 0);
		testUser = new User(102, details);
	}

	@After
	public void tearDown() throws Exception {
		factory = null;
	}
	
	@Test
	public void testGroupMessage () {
		ChannelGroup group = new ChannelGroup(100, 1, "Member", "http://example.com/icon.png", GroupType.NORM);
		MessagePayload message = factory.createGroupDetails(group);
		assertEquals("Member", message.get("name"));
		assertEquals("http://example.com/icon.png", message.get("icon"));
		assertEquals(GroupType.NORM, message.get("type"));
	}

	@Test
	public void testUserRemoval() {
		MessagePayload message = factory.createChannelUserRemoval(testUser, dummyChannel);
		assertEquals(102, message.get("userID"));
	}

	@Test
	public void testUserAddition() {
		dummyChannel.addUser(testUser);
		MessagePayload message = factory.createChannelUserAddition(testUser, dummyChannel);
		
		assertEquals(102, message.get("userID"));
		assertEquals("Test", message.get("username"));
		assertEquals(Settings.GUEST_RANK, message.get("rank"));
	}

	@Test
	public void testUserUpdate() {
		dummyChannel.addUser(testUser);
		MessagePayload message = factory.createChannelUserUpdate(testUser, dummyChannel);
		
		assertEquals(102, message.get("userID"));
		assertEquals("Test", message.get("username"));
		assertEquals(Settings.GUEST_RANK, message.get("rank"));
	}
	
	

}
