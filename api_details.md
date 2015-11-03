#-------------------------------------------------------------------------------
# Copyright (c) 2013 Francis G.
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the GNU Public License v3.0
# which accompanies this distribution, and is available at
# http://www.gnu.org/licenses/gpl.html
# 
# Contributors:
#     Francis G - initial API and implementation
#-------------------------------------------------------------------------------
Each section of the API returns data in JSON form. Where data is required to be POSTed to the server, it must be in a JSON format.
If the 'user string' is set to 'null', it is assumed that guest access is required.
The API is divided into several main sections:

1) Channel requests (/channel/) - These requests relate directly to channel management. They include listing channel details, joining/leaving channels, alerting channel ranks and bans, and altering channel settings. The first paramater on all channel requests MUST be the channel ID (eg /channel/1/).

	GET requests:
	# /channel/{id}/ - Returns basic details about the channel such as the channel name, opening message, opening message colour, etc
	# /channel/{id}/list/ - Returns a list of people currently in the channel. The total number of users is returned, as well as the username and ID of each user
	# /channel/{id}/rankdetails/ - Returns the names and basic details of each rank type in the channel.
	# /channel/{id}/ranks/ - Returns a list of people who hold a rank in the channel. The total number of ranks is included, and for each rank, a username, userID, and rankID is included.
	# /channel/{id}/bans/ - Returns a list of people who are permanantly banned from the channel. The total number of bans is included, and for each user, a username and userID is included. [user not found] is used when the username for a user cannot be found.
	# /channel/{id}/permissions/ - Returns the details of permissions in the channel. Each permission provides and ID, a name, and a value (minimum rank holding this permission)

	POST requests:
	# /channel/{id}/join/ - Allows the user to join a channel. A JSONObject containing the session key for the user (with the key 'user') is required
	# /channel/{id}/leave/ - Causes the user to leave the channel they're currently in. A JSONObject containing the 'session' string is required
	# /channel/{id}/messages/send/ - Sends a standard message to all users currently in the specified channel. A JSONObject containing the 'session' string and a 'message' string containg the message is required.
	# /channel/{id}/messages/ - Returns the cued messages for the user from the selected channel. A JSONObject containing the 'session' string is required. When this is requested, the cued messages are also removed from the server. Returns 204 if there are no cued messages.
	# /channel/{id}/permissions/change/ - Allows a user with the appropriate permissions to alter the permissions of the channel. A JSONObject containing the 'session' string, 'permission' ID, and the new 'value' is required. Returns 403 if the user does not have the appropriate permissions
	# /channel/{id}/reset/ - Allows a user with the appropriate permissions to reset the channel, causing all temporary bans to be lifted and everyone who is currently in the channel to be kicked out. Requires a JSONObject with the 'session' string, in order to authenticate the user. Returns 403 if the user does not have the appropriate permissions
	# /channel/{id}/groups/add/ - Allows ranks with the appropriate permissions to add ranks to the rank list. Requires a JSONObject with the user's 'session' string and either the userID ('userID') of the user they want to rank, or the username ('username'). 
	# /channel/{id}/groups/remove/ - Allows ranks with the appropriate permissions to remove ranks from the rank list. Requires a JSONObject with the user's 'session' string and the userID ('userID') of the user they want to remove from the list. 
	# /channel/{id}/groups/update/ - Allows ranks with the appropriate permissions to modify the rank level of a user on the rank list. Only users currently on this list with a rank lower than the user's own rank may be altered. Requires a JSONObject with the user's 'session' string, the userID ('userID') of the user they want to modify, and the 'rank' they wish to change to. 
	# /channel/{id}/bans/add/ - Allows ranks with the appropriate permissions to add users to the ban list. Requires a JSONObject with the user's 'session' string and either the userID ('userID') of the user they want to ban, or the username ('username'). 
	# /channel/{id}/bans/remove/ - Allows ranks with the appropriate permissions to remove users from the permanant ban list. Requires a JSONObject with the user's 'session' string and the userID ('userID') of the user they want to remove from the list. 
	# /channel/{id}/kick/ - Removes the specifed user from the channel and applies a 60 second temporary ban. The sending user must hold the permission needed to kick users (permission ID = 2) and the user must hold a lower rank than the kicking user. Requires a JSONObject with ther user's 'session' string and the userID ('userID') of the person they wish to kick
	# /channel/{id}/tempban/ - Temporarily bans the specified user from the channel. The sending user must hold the permission needed to temp ban users (permission ID = 3) and the user that the ban is applied to must hold a lower rank than the banning user. Requires a JSONObject with ther user's 'session' string, the time frame ('duration') of the temp ban (in minutes), and either the userID ('userID') of the user they want to ban, or the username ('username').
	# /channel/{id}/rankdetails/change/ - Allows users with the appropriate permission to change the names for certain ranks in the channel. Requires a JSONObject with ther user's 'session' string, the rank ID ('rankID') of the name they want to change, and the new name ('rankName').
	# /channel/{id}/openingmessage/change/ - Allows users with the ability to change channel details to set the opening message. Requires a JSONObject with ther user's 'session' string and the desired message ('message'). Note that this section is likely to be changed in the future.

2) Search requests (/search/) - These requests are used for resolving strings to IDs. Currently included are: channel name searches, and username searches

	GET requests:
	# /search/channel/?name={channel name} - Returns the channel ID and name of the channel requested, or a status of 404 if the channel was not found.

3) User management requests (/user/) - This section deals with the management of users. It incorperates logins, log outs, password changes, account creations, etc.

	POST requests:
	# /user/login/standard/ - This section allows users to log into the server, using the normal username-password authentication. The JSONObject requires a 'username' and 'password' paramater. If 'username' is equal to 'guest', a new guest account is used (userID 10, username 'Guest'). Returns the user ID of the user, the username and a session key (which is sent back to the server whenever a POST request is made, requiring authentication)
	# /user/logout/ - Destroys the active session for the user, throwing them out of any channels they are in and preventing any further use of the session ID given. Only request paramater must be 'user', containing the session ID of the user.
	# /user/registergcm/ - Replaces the existing GCM ID with the specified ID, for the user specified. Requires the 'user' paramater with the user's session ID, and the 'GCMRegID' paramater containing the new GCM string.
	# /user/create/standard/ - Interface which allows the creation of an account, using the standard account creation details: 'username' for the desired display name, 'loginName' for the desired name used to log into the account (cannot be changes), and 'password' for the desired password. Returns a 200 status and the new user ID if successful, or an error code and message if not.
