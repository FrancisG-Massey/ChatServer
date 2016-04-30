package com.sundays.chat.server.channel;

import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sundays.chat.io.ChannelDataIO;
import com.sundays.chat.io.ChannelDetails;
import com.sundays.chat.io.ChannelGroupData;
import com.sundays.chat.io.ChannelGroupType;
import com.sundays.chat.io.ChannelIndex;
import com.sundays.chat.io.IOManager;
import com.sundays.chat.server.TaskScheduler;
import com.sundays.chat.server.user.UserLookup;

/**
 * Performs unit tests on the {@link ChannelManager} class by sending requests and verifying the responses
 * These test cases don't check whether the channel is actually updated; only that the correct responses are correct.
 * This test also covers the {@link Channel} class
 * 
 * @author Francis
 */
public class ChannelRequestTest {
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();//Initialise logging
	}
	
	private ChannelManager channelManager;
	private ChannelDataIO channelIO;
	private UserLookup userManager;
	private ChannelIndex index;
	private ChannelUser user;
	
	private Set<ChannelGroupData> groups = new HashSet<>();
	private Set<Integer> bans = new HashSet<>();
	private Map<Integer, Integer> members = new HashMap<>();

	@Before
	public void setUp() throws Exception {
		IOManager ioHub = mock(IOManager.class);
		index = mock(ChannelIndex.class);
		channelIO = mock(ChannelDataIO.class);
		userManager = mock(UserLookup.class);
		user = mock(ChannelUser.class);
		when(user.getId()).thenReturn(150);
		when(user.getChannelId()).thenReturn(-1);
		
		when(ioHub.getChannelIndex()).thenReturn(index);
		when(ioHub.getChannelIO()).thenReturn(channelIO);
		
		channelManager = new ChannelManager(ioHub, userManager, mock(TaskScheduler.class));
	}
	
	private void mockChannelDetails (ChannelDataIO channelIO, int channelId) throws IOException {
		ChannelDetails details = new ChannelDetails();
		details.setId(channelId);
		details.setName("Test Channel");
		when(index.lookupById(101)).thenReturn(Optional.of(details));
		when(channelIO.getChannelDetails(channelId)).thenReturn(details);
	}
	
	private void mockChannelGroup (ChannelDataIO channelIO, int channelId, int groupId, ChannelGroupType type, String... permissions) throws IOException {
		ChannelGroupData groupData = new ChannelGroupData(groupId, channelId);
		groupData.setOverridesDefault(true);
		groupData.setPermissions(new HashSet<String>(Arrays.asList(permissions)));
		groupData.setType(type);
		groups.add(groupData);
		
		when(channelIO.getChannelGroups(channelId)).thenReturn(groups);
	}
	
	private void mockBan (ChannelDataIO channelIO, int channelId, int banId) throws IOException {
		bans.add(banId);
		
		when(channelIO.getChannelBans(channelId)).thenReturn(bans);
	}
	
	private void mockMember (ChannelDataIO channelIO, int channelId, int memberId, int group) throws IOException {
		members.put(memberId, group);
		
		when(channelIO.getChannelMembers(channelId)).thenReturn(members);
	}
	
	private void addChannelUser(ChannelUser user, int channelId) {
		ChannelResponse response = channelManager.joinChannel(user, channelId);

		assumeTrue(response.getType() == ChannelResponseType.SUCCESS);//Make sure the user actually joined the channel		
		Channel channel = channelManager.getChannel(channelId);
		assumeTrue(channel.getUsers().contains(user));
	}

	@After
	public void tearDown() throws Exception {
		channelManager = null;
		channelIO = null;
		groups.clear();
		bans.clear();
	}

	@Test
	public void testJoinNoChannel() throws IOException {
		when(index.lookupById(101)).thenReturn(Optional.empty());
		ChannelResponse response = channelManager.joinChannel(user, 101);
		assumeTrue(channelManager.getChannel(101) == null);
		
		assertEquals(ChannelResponseType.CHANNEL_NOT_FOUND, response.getType());
	}

	@Test
	public void testJoinBanned() throws IOException {
		mockChannelDetails(channelIO, 101);
		mockBan(channelIO, 101, 150);
		ChannelResponse response = channelManager.joinChannel(user, 101);
		assertEquals(ChannelResponseType.BANNED, response.getType());
		Channel channel = channelManager.getChannel(101);
		assertFalse(channel.getUsers().contains(user));
	}

	@Test
	public void testJoinTempBanned() throws IOException {
		mockChannelDetails(channelIO, 101);
		mockChannelGroup(channelIO, 101, ChannelGroup.GUEST_GROUP, ChannelGroupType.ADMINISTRATOR, "join");//Give the user join permission
		channelManager.loadChannel(101);//Make sure the channel is loaded
		
		Channel channel = channelManager.getChannel(101);
		channel.setTempBan(user.getId(), Integer.MAX_VALUE);
		assumeTrue(channel.getTempBans().containsKey(user.getId()));
		
		ChannelResponse response = channelManager.joinChannel(user, 101);
		assertEquals(ChannelResponseType.BANNED_TEMP, response.getType());
		assertFalse(channel.getUsers().contains(user));
	}

	@Test
	public void testJoinNoPermission() throws IOException {
		mockChannelDetails(channelIO, 101);
		mockChannelGroup(channelIO, 101, ChannelGroup.GUEST_GROUP, ChannelGroupType.ADMINISTRATOR);
		
		ChannelResponse response = channelManager.joinChannel(user, 101);
		assertEquals(ChannelResponseType.NOT_AUTHORISED_GENERAL, response.getType());
		Channel channel = channelManager.getChannel(101);
		assertFalse(channel.getUsers().contains(user));
	}

	@Test
	public void testJoinSuccess() throws IOException {
		mockChannelDetails(channelIO, 101);
		mockChannelGroup(channelIO, 101, ChannelGroup.GUEST_GROUP, ChannelGroupType.ADMINISTRATOR, "join");
		
		ChannelResponse response = channelManager.joinChannel(user, 101);
		assertEquals(ChannelResponseType.SUCCESS, response.getType());
		Channel channel = channelManager.getChannel(101);
		assertTrue(channel.getUsers().contains(user));
		verify(user).setChannel(101);
	}

	@Test
	public void testLeaveNotInChannel() throws IOException {
		mockChannelDetails(channelIO, 101);
		channelManager.loadChannel(101);//Make sure the channel is loaded
		
		Channel channel = channelManager.getChannel(101);
		assumeFalse(channel.getUsers().contains(user));
		
		ChannelResponse response = channelManager.leaveChannel(user, 101);
		assertEquals(ChannelResponseType.NO_CHANGE, response.getType());
	}

	@Test
	public void testLeaveSuccess() throws IOException {
		mockChannelDetails(channelIO, 101);
		mockChannelGroup(channelIO, 101, ChannelGroup.GUEST_GROUP,ChannelGroupType.ADMINISTRATOR, "join");
		ChannelResponse response = channelManager.joinChannel(user, 101);
		
		assumeTrue(response.getType() == ChannelResponseType.SUCCESS);//Make sure the user actually joined the channel		
		Channel channel = channelManager.getChannel(101);
		assumeTrue(channel.getUsers().contains(user));

		when(user.getChannelId()).thenReturn(101);//Make the user return 101 as the channel ID
		
		response = channelManager.leaveChannel(user, 101);
		assertEquals(ChannelResponseType.SUCCESS, response.getType());//Check the return code
		assertFalse(channel.getUsers().contains(user));//Make sure the user was removed
		verify(user).setChannel(-1);//Make sure the user isn't marked as in the channel
	}

	@Test
	public void testResetChannelNotLoaded() {
		assumeTrue(channelManager.getChannel(101) == null);
		ChannelResponse response = channelManager.resetChannel(user, 101);
		
		assertEquals(ChannelResponseType.CHANNEL_NOT_LOADED, response.getType());
	}

	@Test
	public void testResetNoPermission() throws IOException {
		mockChannelDetails(channelIO, 101);
		channelManager.loadChannel(101);
		assumeFalse(channelManager.getChannel(101) == null);
		ChannelResponse response = channelManager.resetChannel(user, 101);
		
		assertEquals(ChannelResponseType.NOT_AUTHORISED_GENERAL, response.getType());
		Channel channel = channelManager.getChannel(101);
		assertFalse(channel.resetLaunched);
	}

	@Test
	public void testResetSuccess() throws IOException {
		mockChannelDetails(channelIO, 101);
		mockMember(channelIO, 101, user.getId(), ChannelGroup.ADMIN_GROUP);
		mockChannelGroup(channelIO, 101, ChannelGroup.ADMIN_GROUP, ChannelGroupType.ADMINISTRATOR, "reset");
		channelManager.loadChannel(101);
		assumeFalse(channelManager.getChannel(101) == null);
		
		ChannelResponse response = channelManager.resetChannel(user, 101);
		
		assertEquals(ChannelResponseType.SUCCESS, response.getType());
		Channel channel = channelManager.getChannel(101);
		assertTrue(channel.resetLaunched);
	}

	@Test
	public void testKickChannelNotLoaded() {
		assumeTrue(channelManager.getChannel(101) == null);
		ChannelResponse response = channelManager.kickUser(user, 101, 88);
		
		assertEquals(ChannelResponseType.CHANNEL_NOT_LOADED, response.getType());
	}

	@Test
	public void testKickNoPermission() throws IOException {
		ChannelUser target = mock(ChannelUser.class);
		when(target.getId()).thenReturn(88);

		mockChannelGroup(channelIO, 101, ChannelGroup.GUEST_GROUP, ChannelGroupType.NORMAL, "join");
		mockChannelDetails(channelIO, 101);
		
		addChannelUser(target, 101);
		
		ChannelResponse response = channelManager.kickUser(user, 101, 88);
		
		assertEquals(ChannelResponseType.NOT_AUTHORISED_GENERAL, response.getType());
		Channel channel = channelManager.getChannel(101);
		assertTrue(channel.getUsers().contains(target));//Make sure the user wasn't actually removed
	}

	@Test
	public void testKickHigherUser() throws IOException {
		ChannelUser target = mock(ChannelUser.class);
		when(target.getId()).thenReturn(88);
		when(userManager.getUser(target.getId())).thenReturn(target);
		
		mockChannelDetails(channelIO, 101);
		mockMember(channelIO, 101, user.getId(), ChannelGroup.MOD_GROUP);
		mockMember(channelIO, 101, target.getId(), ChannelGroup.ADMIN_GROUP);
		mockChannelGroup(channelIO, 101, ChannelGroup.MOD_GROUP, ChannelGroupType.MODERATOR, "join", "kick");
		mockChannelGroup(channelIO, 101, ChannelGroup.ADMIN_GROUP, ChannelGroupType.ADMINISTRATOR, "join", "kick");
		
		addChannelUser(target, 101);
		Channel channel = channelManager.getChannel(101);
		
		ChannelResponse response = channelManager.kickUser(user, 101, 88);
		
		assertEquals(ChannelResponseType.NOT_AUTHORISED_SPECIFIC, response.getType());
		assertTrue(channel.getUsers().contains(target));//Make sure the user wasn't actually removed
	}

	@Test
	public void testKickSuccess() throws IOException {
		ChannelUser target = mock(ChannelUser.class);
		when(target.getId()).thenReturn(88);
		when(userManager.getUser(target.getId())).thenReturn(target);
		
		mockChannelDetails(channelIO, 101);
		mockMember(channelIO, 101, user.getId(), ChannelGroup.ADMIN_GROUP);
		mockMember(channelIO, 101, target.getId(), ChannelGroup.MOD_GROUP);
		mockChannelGroup(channelIO, 101, ChannelGroup.ADMIN_GROUP, ChannelGroupType.ADMINISTRATOR, "join", "kick");
		mockChannelGroup(channelIO, 101, ChannelGroup.MOD_GROUP, ChannelGroupType.MODERATOR, "join", "kick");
		
		addChannelUser(target, 101);
		Channel channel = channelManager.getChannel(101);
		
		ChannelResponse response = channelManager.kickUser(user, 101, 88);
		
		assertEquals(ChannelResponseType.SUCCESS, response.getType());
		assertFalse(channel.getUsers().contains(target));//Make sure the user was removed
		assertTrue(channel.getTempBans().containsKey(target.getId()));//Make sure the 60 second temp ban was applied
	}

	@Test
	public void testTempBanChannelNotLoaded() {
		assumeTrue(channelManager.getChannel(101) == null);
		ChannelResponse response = channelManager.tempBanUser(user, 101, 88, 60);
		
		assertEquals(ChannelResponseType.CHANNEL_NOT_LOADED, response.getType());
	}

	@Test
	public void testTempBanNoPermission() throws IOException {
		ChannelUser target = mock(ChannelUser.class);
		when(target.getId()).thenReturn(88);

		mockChannelGroup(channelIO, 101, ChannelGroup.GUEST_GROUP, ChannelGroupType.GUEST, "join");
		mockChannelDetails(channelIO, 101);
		channelManager.loadChannel(101);
		assumeFalse(channelManager.getChannel(101) == null);
		
		ChannelResponse response = channelManager.tempBanUser(user, 101, 88, 60);
		
		assertEquals(ChannelResponseType.NOT_AUTHORISED_GENERAL, response.getType());
		Channel channel = channelManager.getChannel(101);
		assertFalse(channel.getTempBans().containsKey(target.getId()));//Make sure the user wasn't temp banned
	}

	@Test
	public void testTempBanHigherUser() throws IOException {
		ChannelUser target = mock(ChannelUser.class);
		when(target.getId()).thenReturn(88);
		when(userManager.getUser(target.getId())).thenReturn(target);
		
		mockChannelDetails(channelIO, 101);
		mockMember(channelIO, 101, user.getId(), ChannelGroup.MOD_GROUP);
		mockMember(channelIO, 101, target.getId(), ChannelGroup.ADMIN_GROUP);
		mockChannelGroup(channelIO, 101, ChannelGroup.MOD_GROUP, ChannelGroupType.MODERATOR, "join", "tempban");
		mockChannelGroup(channelIO, 101, ChannelGroup.ADMIN_GROUP, ChannelGroupType.ADMINISTRATOR, "join", "tempban");
		channelManager.loadChannel(101);
		assumeFalse(channelManager.getChannel(101) == null);
		
		Channel channel = channelManager.getChannel(101);
		
		ChannelResponse response = channelManager.tempBanUser(user, 101, 88, 60);
		
		assertEquals(ChannelResponseType.NOT_AUTHORISED_SPECIFIC, response.getType());
		assertFalse(channel.getTempBans().containsKey(target.getId()));//Make sure the user wasn't temp banned
	}

	@Test
	public void testTempBanSuccess() throws IOException {
		ChannelUser target = mock(ChannelUser.class);
		when(target.getId()).thenReturn(88);
		when(userManager.getUser(target.getId())).thenReturn(target);
		
		mockChannelDetails(channelIO, 101);
		mockMember(channelIO, 101, user.getId(), ChannelGroup.ADMIN_GROUP);
		mockMember(channelIO, 101, target.getId(), ChannelGroup.MOD_GROUP);
		mockChannelGroup(channelIO, 101, ChannelGroup.ADMIN_GROUP, ChannelGroupType.ADMINISTRATOR, "join", "tempban");
		mockChannelGroup(channelIO, 101, ChannelGroup.MOD_GROUP, ChannelGroupType.MODERATOR, "join", "tempban");
		channelManager.loadChannel(101);
		assumeFalse(channelManager.getChannel(101) == null);
		
		Channel channel = channelManager.getChannel(101);
		
		ChannelResponse response = channelManager.tempBanUser(user, 101, 88, 60);
		
		assertEquals(ChannelResponseType.SUCCESS, response.getType());
		assertTrue(channel.getTempBans().containsKey(target.getId()));//Make sure the 60 temp ban was applied
	}

	@Test
	public void testMemberAddChannelNotLoaded() {
		assumeTrue(channelManager.getChannel(101) == null);
		ChannelResponse response = channelManager.addMember(user, 101, 88);
		
		assertEquals(ChannelResponseType.CHANNEL_NOT_LOADED, response.getType());
	}

	@Test
	public void testMemberAddNoPermission() throws IOException {
		mockChannelDetails(channelIO, 101);
		channelManager.loadChannel(101);
		Channel channel = channelManager.getChannel(101);
		assumeTrue(channel.getUserGroup(88).getId() == ChannelGroup.GUEST_GROUP);
		
		ChannelResponse response = channelManager.addMember(user, 101, 88);
		
		assertEquals(ChannelResponseType.NOT_AUTHORISED_GENERAL, response.getType());
		assertEquals(ChannelGroup.GUEST_GROUP, channel.getUserGroup(88).getId());//Make sure the user wasn't actually added
	}

	@Test
	public void testMemberAddBanned() throws IOException {
		mockMember(channelIO, 101, user.getId(), ChannelGroup.ADMIN_GROUP);
		mockChannelGroup(channelIO, 101, ChannelGroup.ADMIN_GROUP, ChannelGroupType.ADMINISTRATOR, "memberedit");
		mockBan(channelIO, 101, 88);
		mockChannelDetails(channelIO, 101);
		channelManager.loadChannel(101);
		Channel channel = channelManager.getChannel(101);
		assumeTrue(channel.isUserBanned(88));
		
		ChannelResponse response = channelManager.addMember(user, 101, 88);
		
		assertEquals(ChannelResponseType.TARGET_BANNED, response.getType());
		assertFalse(channel.isUserMember(88));//Make sure the user's group wasn't changed
	}

	@Test
	public void testMemberAddAlreadyMember() throws IOException {
		mockMember(channelIO, 101, user.getId(), ChannelGroup.ADMIN_GROUP);
		mockChannelGroup(channelIO, 101, ChannelGroup.ADMIN_GROUP, ChannelGroupType.ADMINISTRATOR, "memberedit");
		mockMember(channelIO, 101, 88, ChannelGroup.DEFAULT_GROUP);
		mockChannelDetails(channelIO, 101);
		channelManager.loadChannel(101);
		Channel channel = channelManager.getChannel(101);
		assumeTrue(channel.isUserMember(88));
		
		ChannelResponse response = channelManager.addMember(user, 101, 88);
		
		assertEquals(ChannelResponseType.NO_CHANGE, response.getType());
		assertEquals(ChannelGroup.DEFAULT_GROUP, channel.getUserGroup(88).getId());//Make sure the user's group wasn't changed
	}

	@Test
	public void testMemberAddSuccess() throws IOException {
		mockMember(channelIO, 101, user.getId(), ChannelGroup.ADMIN_GROUP);
		mockChannelGroup(channelIO, 101, ChannelGroup.ADMIN_GROUP, ChannelGroupType.ADMINISTRATOR, "memberedit");
		mockChannelDetails(channelIO, 101);
		
		channelManager.loadChannel(101);
		Channel channel = channelManager.getChannel(101);
		assumeTrue(channel.getUserGroup(88).getId() == ChannelGroup.GUEST_GROUP);
		
		ChannelResponse response = channelManager.addMember(user, 101, 88);
		
		assertEquals(ChannelResponseType.SUCCESS, response.getType());
		assertEquals(ChannelGroup.DEFAULT_GROUP, channel.getUserGroup(88).getId());//Make sure the user's group wasn't changed
	}

	@Test
	public void testMemberEditChannelNotLoaded() {
		assumeTrue(channelManager.getChannel(101) == null);
		ChannelResponse response = channelManager.updateMember(user, 101, 88, ChannelGroup.MOD_GROUP);
		
		assertEquals(ChannelResponseType.CHANNEL_NOT_LOADED, response.getType());
	}

	@Test
	public void testMemberEditNoPermission() throws IOException {
		mockMember(channelIO, 101, 88, ChannelGroup.DEFAULT_GROUP);
		mockChannelDetails(channelIO, 101);
		channelManager.loadChannel(101);
		Channel channel = channelManager.getChannel(101);
		assumeTrue(channel.getUserGroup(88).getId() == ChannelGroup.DEFAULT_GROUP);
		
		ChannelResponse response = channelManager.updateMember(user, 101, 88, ChannelGroup.MOD_GROUP);
		
		assertEquals(ChannelResponseType.NOT_AUTHORISED_GENERAL, response.getType());
		assertEquals(ChannelGroup.DEFAULT_GROUP, channel.getUserGroup(88).getId());//Make sure the user's group has not changed
	}

	@Test
	public void testMemberEditNotMember() throws IOException {
		mockMember(channelIO, 101, user.getId(), ChannelGroup.ADMIN_GROUP);
		mockChannelGroup(channelIO, 101, ChannelGroup.ADMIN_GROUP, ChannelGroupType.ADMINISTRATOR, "memberedit");
		mockChannelDetails(channelIO, 101);
		channelManager.loadChannel(101);
		Channel channel = channelManager.getChannel(101);
		assumeFalse(channel.isUserMember(88));
		
		ChannelResponse response = channelManager.updateMember(user, 101, 88, ChannelGroup.MOD_GROUP);
		
		assertEquals(ChannelResponseType.TARGET_INVALID_STATE, response.getType());
		assertFalse(channel.isUserMember(88));//Make sure the user's still isn't a member
	}

	@Test
	public void testMemberEditHigherRights() throws IOException {
		mockMember(channelIO, 101, user.getId(), ChannelGroup.ADMIN_GROUP);
		mockMember(channelIO, 101, 88, ChannelGroup.ADMIN_GROUP);
		mockChannelGroup(channelIO, 101, ChannelGroup.ADMIN_GROUP, ChannelGroupType.ADMINISTRATOR, "memberedit");
		mockChannelDetails(channelIO, 101);
		channelManager.loadChannel(101);
		Channel channel = channelManager.getChannel(101);
		assumeTrue(channel.getUserGroup(88).getId() == ChannelGroup.ADMIN_GROUP);
		
		ChannelResponse response = channelManager.updateMember(user, 101, 88, ChannelGroup.MOD_GROUP);
		
		assertEquals(ChannelResponseType.NOT_AUTHORISED_SPECIFIC, response.getType());
		assertEquals(ChannelGroup.ADMIN_GROUP, channel.getUserGroup(88).getId());//Make sure the user's group hasn't changed
	}

	@Test
	public void testMemberEditHigherGroup() throws IOException {
		mockMember(channelIO, 101, user.getId(), ChannelGroup.ADMIN_GROUP);
		mockMember(channelIO, 101, 88, ChannelGroup.MOD_GROUP);
		mockChannelGroup(channelIO, 101, ChannelGroup.ADMIN_GROUP, ChannelGroupType.ADMINISTRATOR, "memberedit");
		mockChannelDetails(channelIO, 101);
		channelManager.loadChannel(101);
		Channel channel = channelManager.getChannel(101);
		assumeTrue(channel.getUserGroup(88).getId() == ChannelGroup.MOD_GROUP);
		
		ChannelResponse response = channelManager.updateMember(user, 101, 88, ChannelGroup.OWNER_GROUP);
		
		assertEquals(ChannelResponseType.INVALID_ARGUMENT, response.getType());
		assertEquals(ChannelGroup.MOD_GROUP, channel.getUserGroup(88).getId());//Make sure the user's group hasn't changed
	}

	@Test
	public void testMemberEditSuccess() throws IOException {
		mockMember(channelIO, 101, user.getId(), ChannelGroup.ADMIN_GROUP);
		mockMember(channelIO, 101, 88, ChannelGroup.DEFAULT_GROUP);
		mockChannelGroup(channelIO, 101, ChannelGroup.ADMIN_GROUP, ChannelGroupType.ADMINISTRATOR, "memberedit");
		mockChannelDetails(channelIO, 101);
		channelManager.loadChannel(101);
		Channel channel = channelManager.getChannel(101);
		assumeTrue(channel.getUserGroup(88).getId() == ChannelGroup.DEFAULT_GROUP);
		
		ChannelResponse response = channelManager.updateMember(user, 101, 88, ChannelGroup.MOD_GROUP);
		
		assertEquals(ChannelResponseType.SUCCESS, response.getType());
		assertEquals(ChannelGroup.MOD_GROUP, channel.getUserGroup(88).getId());//Make sure the user belongs to the new group
	}

	@Test
	public void testMemberRemoveChannelNotLoaded() {
		assumeTrue(channelManager.getChannel(101) == null);
		ChannelResponse response = channelManager.removeMember(user, 101, 88);
		
		assertEquals(ChannelResponseType.CHANNEL_NOT_LOADED, response.getType());
	}

	@Test
	public void testMemberRemoveNoPermission() throws IOException {
		mockMember(channelIO, 101, 88, ChannelGroup.DEFAULT_GROUP);
		mockChannelDetails(channelIO, 101);
		channelManager.loadChannel(101);
		Channel channel = channelManager.getChannel(101);
		assumeTrue(channel.getUserGroup(88).getId() == ChannelGroup.DEFAULT_GROUP);
		
		ChannelResponse response = channelManager.removeMember(user, 101, 88);
		
		assertEquals(ChannelResponseType.NOT_AUTHORISED_GENERAL, response.getType());
		assertTrue(channel.isUserMember(88));//Make sure the user is still a member
	}

	@Test
	public void testMemberRemoveNotMember() throws IOException {
		mockMember(channelIO, 101, user.getId(), ChannelGroup.ADMIN_GROUP);
		mockChannelGroup(channelIO, 101, ChannelGroup.ADMIN_GROUP, ChannelGroupType.ADMINISTRATOR, "memberedit");
		mockChannelDetails(channelIO, 101);
		channelManager.loadChannel(101);
		Channel channel = channelManager.getChannel(101);
		assumeFalse(channel.isUserMember(88));
		
		ChannelResponse response = channelManager.removeMember(user, 101, 88);
		
		assertEquals(ChannelResponseType.NO_CHANGE, response.getType());
		assertFalse(channel.isUserMember(88));//Make sure the user's still isn't a member
	}

	@Test
	public void testMemberRemoveInvalidGroup() throws IOException {
		mockMember(channelIO, 101, user.getId(), ChannelGroup.ADMIN_GROUP);
		mockMember(channelIO, 101, 88, ChannelGroup.ADMIN_GROUP);
		mockChannelGroup(channelIO, 101, ChannelGroup.ADMIN_GROUP, ChannelGroupType.ADMINISTRATOR, "memberedit");
		mockChannelDetails(channelIO, 101);
		channelManager.loadChannel(101);
		Channel channel = channelManager.getChannel(101);
		assumeTrue(channel.getUserGroup(88).getId() == ChannelGroup.ADMIN_GROUP);
		
		ChannelResponse response = channelManager.removeMember(user, 101, 88);
		
		assertEquals(ChannelResponseType.NOT_AUTHORISED_SPECIFIC, response.getType());
		assertEquals(ChannelGroup.ADMIN_GROUP, channel.getUserGroup(88).getId());//Make sure the user's group hasn't changed
	}

	@Test
	public void testMemberRemoveSuccess() throws IOException {
		mockMember(channelIO, 101, user.getId(), ChannelGroup.ADMIN_GROUP);
		mockMember(channelIO, 101, 88, ChannelGroup.DEFAULT_GROUP);
		mockChannelGroup(channelIO, 101, ChannelGroup.ADMIN_GROUP, ChannelGroupType.ADMINISTRATOR, "memberedit");
		mockChannelDetails(channelIO, 101);
		channelManager.loadChannel(101);
		Channel channel = channelManager.getChannel(101);
		assumeTrue(channel.getUserGroup(88).getId() == ChannelGroup.DEFAULT_GROUP);
		
		ChannelResponse response = channelManager.removeMember(user, 101, 88);
		
		assertEquals(ChannelResponseType.SUCCESS, response.getType());
		assertFalse(channel.isUserMember(88));//Make sure the user is no longer a channel member
	}

	@Test
	public void testBanAddChannelNotLoaded() {
		assumeTrue(channelManager.getChannel(101) == null);
		ChannelResponse response = channelManager.addBan(user, 101, 88);
		
		assertEquals(ChannelResponseType.CHANNEL_NOT_LOADED, response.getType());
	}

	@Test
	public void testBanAddNoPermission() throws IOException {
		mockChannelDetails(channelIO, 101);
		channelManager.loadChannel(101);
		Channel channel = channelManager.getChannel(101);
		assumeFalse(channel.isUserBanned(88));
		
		ChannelResponse response = channelManager.addBan(user, 101, 88);
		
		assertEquals(ChannelResponseType.NOT_AUTHORISED_GENERAL, response.getType());
		assertFalse(channel.isUserBanned(88));//Make sure the user wasn't actually banned
	}

	@Test
	public void testBanAddMember() throws IOException {
		mockMember(channelIO, 101, user.getId(), ChannelGroup.ADMIN_GROUP);
		mockMember(channelIO, 101, 88, ChannelGroup.DEFAULT_GROUP);
		mockChannelGroup(channelIO, 101, ChannelGroup.ADMIN_GROUP, ChannelGroupType.ADMINISTRATOR, "banedit");
		mockChannelDetails(channelIO, 101);
		channelManager.loadChannel(101);
		Channel channel = channelManager.getChannel(101);
		assumeTrue(channel.isUserMember(88));
		
		ChannelResponse response = channelManager.addBan(user, 101, 88);
		
		assertEquals(ChannelResponseType.TARGET_INVALID_STATE, response.getType());
		assertFalse(channel.isUserBanned(88));//Make sure the user wasn't banned
	}

	@Test
	public void testBanAddAlreadyBanned() throws IOException {
		mockMember(channelIO, 101, user.getId(), ChannelGroup.ADMIN_GROUP);
		mockChannelGroup(channelIO, 101, ChannelGroup.ADMIN_GROUP, ChannelGroupType.ADMINISTRATOR, "banedit");
		mockBan(channelIO, 101, 88);
		mockChannelDetails(channelIO, 101);
		channelManager.loadChannel(101);
		Channel channel = channelManager.getChannel(101);
		assumeTrue(channel.isUserBanned(88));
		
		ChannelResponse response = channelManager.addBan(user, 101, 88);
		
		assertEquals(ChannelResponseType.NO_CHANGE, response.getType());
		assertTrue(channel.isUserBanned(88));//Make sure the user is still banned
	}

	@Test
	public void testBanAddSuccess() throws IOException {
		mockMember(channelIO, 101, user.getId(), ChannelGroup.ADMIN_GROUP);
		mockChannelGroup(channelIO, 101, ChannelGroup.ADMIN_GROUP, ChannelGroupType.ADMINISTRATOR, "banedit");
		mockChannelDetails(channelIO, 101);
		
		channelManager.loadChannel(101);
		Channel channel = channelManager.getChannel(101);
		assumeTrue(channel.getUserGroup(88).getId() == ChannelGroup.GUEST_GROUP);
		
		ChannelResponse response = channelManager.addBan(user, 101, 88);
		
		assertEquals(ChannelResponseType.SUCCESS, response.getType());
		assertTrue(channel.isUserBanned(88));//Make sure the user is banned
	}

	@Test
	public void testBanRemoveChannelNotLoaded() {
		assumeTrue(channelManager.getChannel(101) == null);
		ChannelResponse response = channelManager.removeBan(user, 101, 88);
		
		assertEquals(ChannelResponseType.CHANNEL_NOT_LOADED, response.getType());
	}

	@Test
	public void testBanRemoveNoPermission() throws IOException {
		mockBan(channelIO, 101, 88);
		mockChannelDetails(channelIO, 101);
		channelManager.loadChannel(101);
		Channel channel = channelManager.getChannel(101);
		assumeTrue(channel.isUserBanned(88));
		
		ChannelResponse response = channelManager.removeBan(user, 101, 88);
		
		assertEquals(ChannelResponseType.NOT_AUTHORISED_GENERAL, response.getType());
		assertTrue(channel.isUserBanned(88));//Make sure the user is still a member
	}

	@Test
	public void testBanRemoveNotBanned() throws IOException {
		mockMember(channelIO, 101, user.getId(), ChannelGroup.ADMIN_GROUP);
		mockChannelGroup(channelIO, 101, ChannelGroup.ADMIN_GROUP, ChannelGroupType.ADMINISTRATOR, "banedit");
		mockChannelDetails(channelIO, 101);
		channelManager.loadChannel(101);
		Channel channel = channelManager.getChannel(101);
		assumeFalse(channel.isUserBanned(88));
		
		ChannelResponse response = channelManager.removeBan(user, 101, 88);
		
		assertEquals(ChannelResponseType.NO_CHANGE, response.getType());
		assertFalse(channel.isUserBanned(88));//Make sure the user's still isn't a member
	}

	@Test
	public void testBanRemoveSuccess() throws IOException {
		mockMember(channelIO, 101, user.getId(), ChannelGroup.ADMIN_GROUP);
		mockBan(channelIO, 101, 88);
		mockChannelGroup(channelIO, 101, ChannelGroup.ADMIN_GROUP, ChannelGroupType.ADMINISTRATOR, "banedit");
		mockChannelDetails(channelIO, 101);
		channelManager.loadChannel(101);
		Channel channel = channelManager.getChannel(101);
		assumeTrue(channel.isUserBanned(88));
		
		ChannelResponse response = channelManager.removeBan(user, 101, 88);
		
		assertEquals(ChannelResponseType.SUCCESS, response.getType());
		assertFalse(channel.isUserBanned(88));//Make sure the user is no longer a channel member
	}

}
