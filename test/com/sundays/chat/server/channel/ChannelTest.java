package com.sundays.chat.server.channel;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sundays.chat.io.ChannelDataManager;
import com.sundays.chat.io.UserDetails;
import com.sundays.chat.server.Settings;
import com.sundays.chat.server.user.User;

public class ChannelTest {
	
	private ChannelDataManager channelIO = new DummyChannelDataManager();
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
		assertEquals(channel.getName(), "Test Name");
	}

	@Test
	public void testMessage() {
		channel.setOpeningMessage("Test Opening Message");
		assertEquals(channel.getOpeningMessage(), "Test Opening Message");
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
		assertEquals(channel.getUserRank(100), Settings.GUEST_RANK);
	}

	@Test
	public void testUpdateMember() {
		channel.addRank(100);
		assumeTrue(channel.getUserRank(100) == Settings.DEFAULT_RANK);
		channel.setRank(100, Settings.ADMIN_RANK);
		assertEquals(channel.getUserRank(100), Settings.ADMIN_RANK);
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
		assertNotEquals(channel.getBanExpireTime(102), 0L);
	}
	
	@Test
	public void testTempBanRemove () {
		channel.setTempBan(102, 20_000);
		assumeTrue(channel.getBanExpireTime(102) != 0L);
		channel.removeTempBan(102);
		assertNull(channel.getBanExpireTime(102));
	}

}
