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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sundays.chat.io.ChannelDataIO;
import com.sundays.chat.io.UserDetails;
import com.sundays.chat.server.user.User;

public class ChannelTest {
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();//Initialise logging
	}
	
	private ChannelDataIO channelIO;
	private Channel channel;

	@Before
	public void setUp() throws Exception {
		channelIO = mock(ChannelDataIO.class);
		channel = new Channel(100, channelIO);
	}

	@After
	public void tearDown() throws Exception {
		channel = null;
	}

	@Test
	public void testId() {
		assertEquals(100, channel.getId());
	}

	@Test
	public void testOwner() {
		channel.setOwnerID(102);
		assertEquals(102, channel.getOwnerID());
	}

	@Test
	public void testName() {
		channel.setName("Test Name");
		assertEquals("Test Name", channel.getName());
	}

	@Test
	public void testAlias() {
		channel.setAlias("Test Name");
		assertEquals("Test Name", channel.getAlias());
	}

	@Test
	public void testOwnerGroup() {
		channel.setOwnerID(102);
		assertEquals(ChannelGroup.OWNER_GROUP, channel.getUserGroup(102).getId());
	}

	@Test
	public void testGuestGroup() {
		assumeFalse(channel.getMembers().containsKey(103));
		assertEquals(ChannelGroup.GUEST_GROUP, channel.getUserGroup(103).getId());
	}

	@Test
	public void testAttributeDefault() {
		assumeTrue(channel.getAttribute("test.attr") == null);
		
		assertEquals("DefaultValue", channel.getAttribute("test.attr", "DefaultValue"));
	}

	@Test
	public void testNewAttribute() {
		assumeTrue(channel.getAttribute("test.attr") == null);
		channel.setAttribute("test.attr", "Some Value");
		assertEquals("Some Value", channel.getAttribute("test.attr"));
	}

	@Test
	public void testNewAttributeError() throws IOException {
		doThrow(new IOException()).when(channelIO).addAttribute(100, "test.attr", "Some Value");
		assumeTrue(channel.getAttribute("test.attr") == null);
		assertFalse(channel.setAttribute("test.attr", "Some Value"));
		assertEquals(null, channel.getAttribute("test.attr"));
	}

	@Test
	public void testUpdateAttribute() {
		channel.setAttribute("test.attr", "Some Value");
		assumeFalse(channel.getAttribute("test.attr") == null);
		channel.setAttribute("test.attr", "New Value");
		assertEquals("New Value", channel.getAttribute("test.attr"));
	}

	@Test
	public void testUpdateAttributeError() throws IOException {
		doThrow(new IOException()).when(channelIO).updateAttribute(100, "test.attr", "New Value");
		channel.setAttribute("test.attr", "Some Value");
		assumeFalse(channel.getAttribute("test.attr") == null);
		assertFalse(channel.setAttribute("test.attr", "New Value"));
		assertEquals("Some Value", channel.getAttribute("test.attr"));
	}

	@Test
	public void testMemberBanRemove() throws IOException {
		Map<Integer, Integer> members = new HashMap<>();
		members.put(103, ChannelGroup.DEFAULT_GROUP);
		when(channelIO.getChannelMembers(100)).thenReturn(members);
		channel.addBan(103);
		members = channel.loadMembers();
		assertFalse(members.containsKey(103));
	}

	@Test
	public void testMemberOwnerAdd() throws IOException {
		Map<Integer, Integer> members = new HashMap<>();
		when(channelIO.getChannelMembers(100)).thenReturn(members);
		channel.setOwnerID(102);
		members = channel.loadMembers();
		assertTrue(members.containsKey(102));
		assertEquals(ChannelGroup.OWNER_GROUP, members.get(102).intValue());
	}

	@Test
	public void testFakeOwnerReset() throws IOException {
		Map<Integer, Integer> members = new HashMap<>();
		members.put(103, ChannelGroup.OWNER_GROUP);
		when(channelIO.getChannelMembers(100)).thenReturn(members);
		channel.setOwnerID(102);
		members = channel.loadMembers();
		assertNotEquals(ChannelGroup.OWNER_GROUP, members.get(103).intValue());
	}

	@Test
	public void testFakeGroupReset() throws IOException {
		Map<Integer, Integer> members = new HashMap<>();
		members.put(103, 999);
		assumeFalse(channel.getGroups().containsKey(999));
		
		when(channelIO.getChannelMembers(100)).thenReturn(members);
		members = channel.loadMembers();
		assertNotEquals(999, members.get(103).intValue());
	}

	@Test
	public void testAddMember() {
		channel.addMember(100);
		assertEquals(ChannelGroup.DEFAULT_GROUP, channel.getUserGroup(100).getId());
	}

	@Test
	public void testRemoveMember() {
		channel.addMember(100);
		assumeTrue(channel.getUserGroup(100).getId() == ChannelGroup.DEFAULT_GROUP);
		channel.removeMember(100);
		assertEquals(ChannelGroup.GUEST_GROUP, channel.getUserGroup(100).getId());
	}

	@Test
	public void testUpdateMember() {
		channel.addMember(100);
		assumeTrue(channel.getUserGroup(100).getId() == ChannelGroup.DEFAULT_GROUP);
		channel.setMemberGroup(100, ChannelGroup.ADMIN_GROUP);
		assertEquals(ChannelGroup.ADMIN_GROUP, channel.getUserGroup(100).getId());
	}

	@Test
	public void testAddBan() {
		assertTrue(channel.addBan(100));
		assertTrue(channel.isUserBanned(100));
	}

	@Test
	public void testAddBanDuplicate() {
		channel.addBan(100);
		assumeTrue(channel.isUserBanned(100));
		
		assertFalse(channel.addBan(100));
	}

	@Test
	public void testAddBanException() throws IOException {
		doThrow(new IOException()).when(channelIO).addBan(100, 100);
		
		assertFalse(channel.addBan(100));//The addition should fail
		assertFalse(channel.getBans().contains(100));//And the ban should not have been added
	}

	@Test
	public void testRemoveBan() {
		channel.addBan(100);
		assumeTrue(channel.isUserBanned(100));
		
		assertTrue(channel.removeBan(100));//The removal should succeed
		assertFalse(channel.isUserBanned(100));
	}

	@Test
	public void testRemoveNonBan() {
		assumeFalse(channel.isUserBanned(100));
		
		assertFalse(channel.removeBan(100));
	}

	@Test
	public void testRemoveBanException() throws IOException {
		doThrow(new IOException()).when(channelIO).removeBan(100, 100);
		
		channel.addBan(100);
		assumeTrue(channel.isUserBanned(100));
		
		assertFalse(channel.removeBan(100));//The removal should fail
		assertTrue(channel.getBans().contains(100));//And the ban should still apply
	}

	@Test
	public void testAddUser() {
		ChannelUser u = new User(102, new UserDetails());
		channel.addUser(u);
		assertTrue(channel.getUsers().contains(u));
	}

	@Test
	public void testRemoveUser() {
		ChannelUser u = new User(102, new UserDetails());
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
	public void testTempBanReset () {
		channel.setTempBan(102, 20_000);
		long oldExpires = channel.getBanExpireTime(102);
		assumeFalse(0L == oldExpires);
		
		channel.setTempBan(102, 20_100);
		assertTrue("New ban has not replaced old ban. New="+channel.getBanExpireTime(102)+", old="+oldExpires, channel.getBanExpireTime(102) > oldExpires);
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
