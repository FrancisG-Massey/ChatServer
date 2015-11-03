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
=====Channel 'messages' types=====
1	Unused
2	Unused
3	Channel system message (local)
4	Channel system message (global)
5	Standard message
6	Channel list addition
7	Channel list removal
8	Channel list update
9	Permission update
10	Left/removed from channel 
11	Rank list addition
12	Rank list removal
13	Rank list update
14	Ban list addition
15	Ban list removal
16	Rank name update
17	Channel details update
18	Change to individual user's rank

=====Channel system messages=====
-----Special-----
40	Channel opening message

-----General-----
100	The channel you have attempted to join does not exist.
101	You cannot join the channel at this time. Please try again in a few seconds.
102	You are permanently banned from this channel.
103	You do not have a high enough rank to join this channel.
104	You are temporarily banned from the channel (%time minute(s) remaining). [Note: %time = time in mins remaining]
105	Cannot send message: not currently in a channel.
106	You do not have the appropriate permissions to send messages in this channel.
107	The channel you have attempted to reset is not loaded.\n Resetting will have no effect.
108	You do not have the ability reset this channel.
109	This channel is being reset.\n All members will be removed.\n You may join again in a few minutes.
110	Your request to reset this channel has been accepted.
111	The channel you have attempted to kick this user from is not loaded.
112	You cannot kick people from this channel.
113	%username is not currently in the channel. [Note: %username = username of the kicked user]
114	You can only kick users with a lower rank level than yours.
115	You have been kicked from the channel.
116	Your attempt to kick %username from this channel was successful. [Note: %username = username of the kicked user]
117	The channel you have attempted to temporarily ban this user from is not loaded.
118	You cannot temporarilly ban people from this channel.
119	You can only temporarilly ban users with a lower rank level than yours.
120	You have successfully temporarilly banned this user from the channel for %time minutes. [Note: %time = time in mins of ban]
121	Their ban will be lifted after the specified time, or if the channel is reset.
122	You do not have the ability to change permissions in this channel.
123	An invalid new permission for this channel has been sent.
124	Permissions for this channel have changed; you no longer have the ability to join.
125	You do not have the ability to change the details of this channel.
126	Invalid rank level specified.
127	The rank name you have specified is too long.\n Please use a shorter name.
128	The opening message you have specified is too long.\n Please use a shorter message.
129	The opening message for this channel has been updated successfully.
130	You do not have the ability to grant ranks to people in this channel.
131	%username is already ranked in the channel.\nYou can change their rank using the channel settings interface.
132	%username has been permanently banned from the channel.\n Please remove their name from the permanent ban list first.
133	%username has been successfully given a rank in this channel.
134	Could rank %username due to a system error.
135	You do not have the ability to revoke ranks from people in this channel.
136	This user does not currently have a rank in the channel.
137	You cannot revoke the rank of someone with the same or higher rank than your own.
138	This user's rank has been revoked successfully.
139	Could not remove rank due to a system error.
140	You must add this user to the rank list before setting their rank.
141	You do not have the ability to change the ranks of other users in this channel.
142	Invalid rank level specified.
143	You cannot alter the rank of someone with the same or higher rank than your own.
144	This user's rank has been changed successfully.
145	Could not change rank due to a system error.
146	You do not have the ability to permanently ban people from this channel.
147	This user is already permanently banned from the channel.
148	This user currently holds a rank in the channel.\n Please remove their name from the rank list first.
149	This user has been permanently banned from this channel.\n Their ban will take effect when they leave the channel.
150	Could permanently ban this user due to a system error.
151	You do not have the ability to revoke permanent bans for people in this channel.
152	This user is not currently permanently banned from the channel.
153	This user's permanent ban has been removed successfully.
154	Could unban this user due to a system error.
155	You have been removed from the channel. [Caused by channel resetting]
156	Cannot change permissions: channel not found.
157	You cannot set permissions to a higher rank than your own.
158	The channel must be loaded before you can modify rank data.\n Try joining the channel first.
159	The user you have attempted to rank was not found.
160	The user you have attempted to ban was not found.
161	The channel must be loaded before you can modify ban data.\n Try joining the channel first.
162	The user you have attempted to temporarily ban was not found.
163	Cannot change rank names: channel not found.
164	The name for rank %id have been successfully updated for this channel.
165	Cannot change channel details: channel not found.
166	You are already in a channel. You need to leave your current channel first.
167	You need to be in a channel to perform that action.
168	The channel you have attempted to lock is not currently loaded on this server.
169	You cannot lock this channel.
170	You can only lock the channel to ranks below your level.
171	An existing lock which affects your rank is already in place. This lock must be removed before you can place a new lock.
172	A lock has been successfully placed on new members with the rank of %rank or below entering the channel for %duration minutes.
173	This channel has been locked for all members with a rank of %rank and below.\nAnyone holding these ranks cannot rejoin the channel if they leave, until the lock is removed.
174	This channel has been locked for anyone holding the rank of %rank or below.
175	The permission you have specified for this channel does not exist.
176 Invalid or missing parameter for %arg; expected: %expected, found: %found.