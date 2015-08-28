/*******************************************************************************
 * Copyright (c) 2013 Francis G.
 * 
 * This file is part of ChatServer.
 * 
 * ChatServer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ChatServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with ChatServer.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
$("body").load(function () {
	chatClient.init();
});

$(window).bind('beforeunload', function () {
	if (chatClient.session != null) {
		chatClient.signOutSync();
	}		
	//return "Make sure you have logged out before navigating away from the page.";
});

function sortElements (parentElement) {
	var elems = $(parentElement).children('li').detach();
    elems.sort(function(a,b){
    	return $(a).text().localeCompare($(b).text());
    });
    $(parentElement).append(elems);
}

function javaColourToCss (colourInt) {
	var alpha = ((colourInt >> 24) & 0xFF)/255,
	red = (colourInt >> 16) & 0xFF,
	green = (colourInt >> 8) & 0xFF,
	blue = colourInt & 0xFF;
	return "rgba("+red+","+green+","+blue+","+alpha+")";
}

var chatClient = {
		dialogTypes : Object.freeze({
			NONE : 0,
			RANKLIST : 1,
			BANLIST : 2,
			PERMISSIONS : 3,
			CHANNELLIST : 4,
			GROUPLIST : 5
		}),
		session : null,
		userID : -1,
		username : "[Not signed in]",
		channelID : -1,
		channelDetails : { },
		channelRankDetails : { },
		channelGroups : { },
		userPermissions : { },
		userRank : -1,
		messageChecking : false,
		stopCheckingMessages : false,
		messageCheckingTimer : null,
		init : function () {
			chatClient.dialog.openDialog = chatClient.dialogTypes.NONE;
			$("#message_input").keyup(function(event){
			    if(event.keyCode == 13){
			        chatClient.sendMessage($(this).val());
			    }
			}).prop('disabled', true).val("");
			$("#menu_bar").empty();
			$("<li />").text("Account").appendTo("#menu_bar")
			.append("<ul id='account_menu'></ul>");
			$("<li />").text("Channel").css({display: "none"}).appendTo("#menu_bar")
			.append("<ul id='channel_menu'></ul>");
			$("<li id='login_button' />").text("Login").click(function () {
				chatClient.signIn(prompt("Please enter your username", "test1"), 
					prompt("Please enter your password","test"),
					null);
			}).appendTo("#account_menu");
			$("<li id='accountcreate_button' />").text("Create").click(function () {
				var name = prompt("Please enter your desired username"),
				password = prompt("Please enter your desired password");
				if (password != prompt("Please confirm your desired password")) {
					alert("Passwords do not match.");
					return;
				}
				chatClient.createAccount(name, name, password);
			}).appendTo("#account_menu");
			$("<li id='joinchannel_button' />").text("Join").click(function () {
				chatClient.serverChannelsList.open();
				/*var channelName = prompt("Enter a channel name to join", "Test Channel");
				if (channelName == null)
					return;
				chatClient.joinChannelFromName(channelName);*/
				/*channelID = parseInt(channelID);
				if (isNaN(channelID)) {
					alert("You must enter the channel as an ID. Channel name resolving has not yet been implemented.");
					return;
				}
				chatClient.joinChannel(channelID);*/
			}).appendTo("#channel_menu");			
		},
		dialog : {
			openDialog : null,
			onCloseEvents : [],
			showDialog : function (heading, body, dialogType) {
				if (chatClient.dialog.openDialog !== chatClient.dialogTypes.NONE) {
					chatClient.dialog.closeDialog();
				}
				if ($("#dialog_filter").length == 0) {
	                $("<div id='dialog_filter' />").appendTo("body");
	                $("<div id='dialog' />").appendTo("#dialog_filter");
	                $("<input type='button' value='Close' id='dialog_close' />").click(function () {
	                	chatClient.dialog.closeDialog();
	                }).appendTo("#dialog");
	                $("<h2 id='dialog_heading' />").appendTo("#dialog");
	                $("<div id='dialog_content' />").appendTo("#dialog");
	                $("<div id='dialog_controls' />").appendTo("#dialog");
	            } else {	            	
	                $("#dialog_filter").appendTo("body");
	            }
				$("#dialog_heading").html(heading);
				$("#dialog_content").html(body);
	            $("body").css({ overflow: "hidden" });
	            chatClient.dialog.openDialog = dialogType;
			}, 
			closeDialog : function () {
				var dialog = chatClient.dialog;
				$("#dialog_filter").detach();
		        $("body").css({ overflow: "auto" });
		        dialog.openDialog = chatClient.dialogTypes.NONE;
	        	$(dialog.onCloseEvents).each(function (i, callback) {
	        		callback();	        		
	        	});
	        	dialog.onCloseEvents = [];
		        /*if (dialog.onCloseCallback.length < 0) {
		        	dialog.onCloseCallback();
		        	dialog.onCloseCallback = null;
		        }*/
		        chatClient.rankList.isOpen = false;
		        chatClient.banList.isOpen = false;
		        chatClient.permissionList.isOpen = false;
			},
			addCloseEvent : function (event) {
				chatClient.dialog.onCloseEvents.push(event);
			}
		},
		signIn : function (username, password, callback) {
			chatClient.sendJSONRequest("user/login/standard/", {
				"username": username, "password": password
			}, function (response) {
				if (response.status === 200) {
					//alert(JSON.stringify(response));
					chatClient.session = response.session;
					chatClient.userID = response.userID;
					chatClient.username = response.username;
					$("#username").text(chatClient.username);
					if (response.defaultChannel >= 100) {
						chatClient.joinChannel(response.defaultChannel);
					}
					$("#channel_menu").parent().css({display : ""});
					$("#login_button").text("Logout").unbind('click').click(function () {
						chatClient.signOut(null);
					});
					$("#accountcreate_button").remove();
					if (callback)
						callback();
				} else {
					alert(response.message);
				}
			});
		},
		signOut : function (callback) {
			chatClient.sendJSONRequest("user/logout/", {
				"session": chatClient.session
			}, function (response) {
				if (response.status === 200) {
					chatClient.session = null;
					chatClient.userID = -1;
					chatClient.username = "[Not signed in]";
					$("#username").text("");
					$("#login_button").text("Login").unbind('click').click(function () {
						chatClient.signIn(prompt("Please enter your username", "test1"), 
								prompt("Please enter your password","test"),
								null);
					});
					$("<li id='accountcreate_button' />").text("Create").click(function () {
						var name = prompt("Please enter your desired username"),
						password = prompt("Please enter your desired password");
						if (password != prompt("Please confirm your desired password")) {
							alert("Passwords do not match.");
							return;
						}
						chatClient.createAccount(name, name, password);
					}).appendTo("#account_menu");
					$("#channel_menu").parent().css({display : "none"});
					chatClient.clearChannel();
					if (callback)
						callback();
				} else {
					alert(response.message);
				}
			});
		},
		signOutSync : function () {
			$.ajax({
				url: "user/logout/",
				async: false,
				dataType: "json",
				type: "POST",
				headers: {
					"Content-Type": "application/json"
				},
				data: JSON.stringify({
					"session": chatClient.session
				})
			}).done(function () {
				chatClient.session = null;
				chatClient.userID = -1;
			});
		},
		createAccount : function (loginName, username, password) {
			chatClient.sendJSONRequest("user/create/standard/", {
				"username": username,
				"loginName": loginName,
				"password": password
			}, function (response) {
				if (response.status === 200) {
					alert(response.message);
					chatClient.signIn(loginName, password, null);
				} else {
					alert(response.message);
				}
			});
		},
		serverChannelsList : {
			isOpen : false,
			open : function () {
				var thisList = chatClient.serverChannelsList; 
				chatClient.dialog.showDialog("Channels on Server", "<ul id='channellist' class='dialogList'></ul>", chatClient.dialogTypes.CHANNELLIST);
				$("#dialog").css({
					height: "25em",
					marginTop: "-12.5em"
				});
				chatClient.dialog.addCloseEvent(function () {
					thisList.isOpen = false;
				});
				chatClient.sendJSONRequest("search/channel/?all", {
					"session": chatClient.session
				}, function (response) {
					if (response.status === 200) {
						thisList.isOpen = true;
						$("#channellist").empty();					
						$.each(response.channels, function (index, channel) {
							thisList.addToList(channel.id, (channel.isLoaded ? channel.details.name : channel.keyName), 
									(channel.isLoaded ? channel.memberCount : 0));
						});
						thisList.sort();
					} else {
						chatClient.dialog.closeDialog();
						alert(response.message);
					}
				});
				$("<input type='button' value='Join by name' />").appendTo("#dialog_controls")
				.click(function () {
					var channelName = prompt("Enter the name of the channel you wish to join:");
					if (channelName == null || channelName.length < 1) {
						return;
					}
					chatClient.joinChannelFromName(channelName);
					chatClient.dialog.closeDialog();
				});
			},
			addToList : function (channelID, channelName, memberCount) {
				if ($("#channel_"+channelID).length === 0) {
					$("<li id='channel_"+channelID+"' class='channel_user' />").appendTo("#channellist")
					.append("<span>"+channelName+" ("+memberCount+")</span>")
					.css({width: "200px"})
					.data({
						channelID: channelID,
						channelName: channelName,
						memberCount: memberCount
					}).click(function () {
						chatClient.joinChannel(parseInt($(this).data("channelID")));
						chatClient.dialog.closeDialog();
						//chatClient.serverChannelsList.createOptionList(this);
					});
				}
			},
			sort : function () {
				sortElements("#channellist");
			}, 
			createOptionList : function (sender) {
				$("<ul class='dropdown' id='channellist_dropdown' />").appendTo(sender)
				.css({width: $(sender).width()});
				$(sender).unbind('click').click(function () {
					$("#channellist_dropdown").remove();
					$(this).unbind('click').click(function () {
						chatClient.serverChannelsList.createOptionList(this);
					});
				});
				$("<li />").text("Join").click(function () {
					chatClient.joinChannel(parseInt($(sender).data("channelID")));
					chatClient.dialog.closeDialog();
				}).appendTo("#channellist_dropdown");
			}
		},
		joinChannelFromName : function (channelName) {
			chatClient.sendJSONRequest("search/channel/?name="+channelName, {
				"session": chatClient.session
			}, function (response) {
				if (response.status === 200) {
					chatClient.joinChannel(response.id);
				} else {
					alert(response.message);
				}
			});	
		},
		joinChannel : function (channelID) {
			chatClient.sendJSONRequest("channel/"+channelID+"/join/", {
				"session": chatClient.session
			}, function (response) {
				if (response.status === 200) {
					chatClient.channelID = channelID;
					chatClient.channelDetails = response.details;
					chatClient.userRank = response.rank;
					$("#channel_name").html(response.details.name);
					chatClient.runMessageChecking();
					chatClient.getMemberList();
					chatClient.getPermissions();
					//chatClient.getRankDetails();
					chatClient.getGroups();
					$("#joinchannel_button").text("Leave").unbind('click').click(function () {
						chatClient.leaveChannel(chatClient.channelID);
					});					
				} else {
					alert(response.message);
				}
			});	
		},
		leaveChannel : function (channelID) {
			chatClient.sendJSONRequest("channel/"+channelID+"/leave/", {
				"session": chatClient.session
			}, function (response) {
				if (response.status === 200) {
					$("<div class='message_text red' />").text(response.message).appendTo("#messages");
					chatClient.clearChannel();
				} else {
					var clearChannel = confirm("An error occured when trying to leave the channel: "+response.message
							+"\nDo you want to clear the channel details anyway? Doing so will allow you to join other channels, but it may mean your account still appears to be in the channel.");
					//alert(response.message);
					if (clearChannel) {
						chatClient.clearChannel();
					}
				}
			});	
		},
		clearChannel : function () {
			chatClient.stopMessageChecking();
			chatClient.channelID = -1;
			chatClient.channelDetails = {};
			chatClient.userRank = -1;
			chatClient.userPermissions = {};
			chatClient.channelGroups = {};
			$("#channel_name").text("");
			$("#userlist").empty().css({visibility: "hidden"});
			$("#message_input").prop('disabled', true);
			$("#joinchannel_button").text("Join").unbind('click').click(function () {
				chatClient.serverChannelsList.open();
				/*var channelName = prompt("Enter a channel name to join", "Test Channel");
				if (channelName == null || channelName.length < 1)
					return;
				chatClient.joinChannelFromName(channelName);*/
			});
			$("#tempban_button").remove();
			$("#reset_button").remove();
			$("#ranks_button").remove();
			$("#bans_button").remove();
		},
		runMessageChecking : function () {
			chatClient.stopCheckingMessages = false;
			if (!chatClient.messageChecking)
				chatClient.checkMessages(true, chatClient.channelID);
		},
		stopMessageChecking : function () {
			chatClient.stopCheckingMessages = true;
			chatClient.checkMessages(false, chatClient.channelID);
			clearTimeout(chatClient.messageCheckingTimer);
		},
		checkMessages : function (loop, channelID) {			
			chatClient.sendJSONRequest("channel/"+channelID+"/messages/get/", {
				"session": chatClient.session
			}, function (response) {
				if (response.status === 200) {
					$(response.messages).each(function (i, v) {
						switch (parseInt(v.type)) {
						case 3://Local channel system message
							$("<div class='message_text' />").html(v.message)
							.css("color", javaColourToCss(v.messageColour))
							.appendTo("#messages");
							break;
						case 4://Global channel system message
							$("<div class='message_text b' />").html(v.message)
							.css("color", javaColourToCss(v.messageColour))
							.appendTo("#messages");
							break;
						case 5://General channel user message
							$("<div class='message_text' />").appendTo("#messages")
							.data("userID", v.userID)
							.append("<img src='images/ranks/rank"+v.senderRank+".png' alt='' title='Rank: "+
							chatClient.channelRankDetails[v.senderRank].rankName+"' />")
							.append("<span>"+v.senderName+": </span>")
							.append($("<span />").text(v.message));
							break;
						case 6://Channel user list addition
							chatClient.channelList.addToList(v.userID, v.username, v.group);
							chatClient.channelList.sort();
							break;
						case 7://Channel user list removal
							chatClient.channelList.removeFromList(v.userID);
							break;
						case 8://Channel user list update
							chatClient.channelList.updateOnList(v.userID, v.username, v.group);
							break;
						case 9://Permission change
							chatClient.permissionList.updateOnList(v.permissionID, v.name, v.value);
							//alert(JSON.stringify(v));
							delete chatClient.userPermissions[v.name];
							chatClient.userPermissions[v.name] = {
									permissionID : v.permissionID,
									name : v.name,
									value : v.value
							};
							chatClient.updateUserPermissions();
							break;
						case 10://Left/removed from channel
							if (chatClient.channelID == channelID) {
								chatClient.clearChannel();
							}							
							break;
						case 11://Add user to rank list
							chatClient.rankList.addToList(v.userID, v.username, v.group);
							chatClient.rankList.sort();
							break;
						case 12://Remove user from rank list
							chatClient.rankList.removeFromList(v.userID);
							break;
						case 13://Update rank list
							chatClient.rankList.updateOnList(v.userID, v.username, v.group);
							break;
						case 14://Ban list addition
							chatClient.banList.addToList(v.userID, v.username);
							chatClient.banList.sort();
							break;
						case 15://Ban list removal
							chatClient.banList.removeFromList(v.userID);
							break;
						case 18://User's rank has been changed
							if (chatClient.userID == v.userID) {
								chatClient.userRank = v.rank;
								chatClient.updateUserPermissions();
							}							
							break;
						default:
							$("#error_txt").append("<div>Unhandled message: "+JSON.stringify(v)+"</div>");
							break;
						}
					});
					chatClient.messageChecking = false;
					if (!chatClient.stopCheckingMessages && loop) {
						chatClient.messageChecking = true;
						chatClient.messageCheckingTimer = setTimeout(chatClient.checkMessages, 2000, true, channelID);
					}
					return;
				} else if (response.status === 204) {
					chatClient.messageChecking = false;
					if (!chatClient.stopCheckingMessages && loop) {
						chatClient.messageChecking = true;
						chatClient.messageCheckingTimer = setTimeout(chatClient.checkMessages, 5000, true, channelID);
					}
				} else {
					alert(response.message);
					chatClient.messageChecking = false;
					chatClient.stopCheckingMessages = true;
				}	
			});	
		},
		sendMessage : function (message) {
			chatClient.sendJSONRequest("channel/"+chatClient.channelID+"/messages/send/", {
				"session": chatClient.session,
				"message": message
			}, function (response) {
				if (response.status === 200) {
					$("#message_input").val("");
					chatClient.checkMessages(false, chatClient.channelID);
					//$("#error_txt").append("<div>"+JSON.stringify(response)+"</div>");
				} else {
					alert(response.message);
				}
			});
		},
		getMemberList : function () {
			chatClient.sendJSONRequest("channel/"+chatClient.channelID+"/userlist/", {
				"session": chatClient.session
			}, function (response) {
				if (response.status === 200) {
					$("#userlist").empty().css({visibility: ""});
					$(response.users).each(function (index, value) {
						chatClient.channelList.addToList(value.userID, value.username, value.group);
					});
					chatClient.channelList.sort();
				} else {
					alert(response.message);
				}
			});
		},
		getRankDetails : function () {
			chatClient.sendJSONRequest("channel/"+chatClient.channelID+"/rankdetails/", {
				"session": chatClient.session
			}, function (response) {
				if (response.status === 200) {
					chatClient.channelRankDetails = response.rankNames;
					//alert(JSON.stringify(response));
				} else {
					alert(response.message);
				}
			});
		},
		getGroups : function () {
			chatClient.sendJSONRequest("channel/"+chatClient.channelID+"/groups/", {
				"session": chatClient.session
			}, function (response) {
				if (response.status === 200) {
					chatClient.channelGroups = response.groups;
					//alert(JSON.stringify(response));
				} else {
					alert(response.message);
				}
			});
		},
		hasPermission : function (permissionName) {
			var returnValue = false;
			$.each(chatClient.userPermissions, function (i, v) {
				if (permissionName.toLowerCase() == v.name.toLowerCase()) {
					if (chatClient.userRank >= v.value) {
						returnValue = true;
						return;
					}
					return;
				}
			});
			return returnValue;
		},
		getPermissions : function () {
			chatClient.sendJSONRequest("channel/"+chatClient.channelID+"/permissions/", {
				"session": chatClient.session
			}, function (response) {
				if (response.status === 200) {
					chatClient.userPermissions = response.permissions;
					chatClient.updateUserPermissions();
				} else {
					alert(response.message);
				}
			});	
		},
		updateUserPermissions : function () {
			$("#message_input").prop('disabled', !chatClient.hasPermission("talk"));			
			if (chatClient.hasPermission("tempban")) {
				if ($("#tempban_button").length === 0) {
					$("<li id='tempban_button' />").text("Temp Ban User").click(function () {
						var username = prompt("Enter name of the person to temporarily ban from the channel.");
						if (username == null || username.length == 0)
							return;
						chatClient.tempBanUser(username);
					}).appendTo("#channel_menu");
				}
			} else {
				$("#tempban_button").remove();
			}
			if (chatClient.hasPermission("reset")) {
				if ($("#reset_button").length === 0) {
					$("<li id='reset_button' />").text("Reset").click(function () {
						if (confirm("Are you sure you want to reset this channel?\nResetting the channel will remove all the occupants and remove any temporary bans")) {
							chatClient.resetChannel();
						}							
					}).appendTo("#channel_menu");	
				}
			} else {
				$("#reset_button").remove();
			}
			if (chatClient.hasPermission("RANKCHANGE")) {
				if ($("#ranks_button").length === 0) {
					$("<li id='ranks_button' />").text("Change ranks").click(function () {
						chatClient.rankList.open();
					}).appendTo("#channel_menu");
				}
			} else {
				$("#ranks_button").remove();
			}
			if (chatClient.hasPermission("PERMBAN")) {
				if ($("#bans_button").length === 0) {
					$("<li id='bans_button' />").text("Change bans").click(function () {
						chatClient.banList.open();
					}).appendTo("#channel_menu");
				}					
			} else {
				$("#bans_button").remove();
			}
			if (chatClient.hasPermission("PERMISSIONCHANGE")) {
				if ($("#permission_button").length === 0) {
					$("<li id='permission_button' />").text("Change permissions").click(function () {
						chatClient.permissionList.open();
					}).appendTo("#channel_menu");	
				}
				if ($("#groups_button").length === 0) {
					$("<li id='groups_button' />").text("Change groups").click(function () {
						chatClient.groupList.open();
					}).appendTo("#channel_menu");	
				}
			} else {
				$("#permission_button").remove();
			}
		},
		sendJSONRequest : function (url, dataObject, callback) {
			return $.ajax({
			  url: url,
			  dataType: "json",
			  type: "POST",
			  headers: {
				  "Content-Type": "application/json"
			  },
			  data: JSON.stringify(dataObject)
			}).done(function(response,text,responseObject) { 
				callback(response);
			}).fail(function (jqXHR, statusText, errorThrown) {
				$("#error_txt").append("<div>Url: "+url+", status: "+errorThrown+"</div>");
			});
			/*var response;
			ajax = new XMLHttpRequest();
			ajax.open("POST", url, false);
			ajax.setRequestHeader("Content-Type", "application/json");
			ajax.send('{"session":"'+chatClient.session+'"}');
			if (response=JSON.parse(ajax.responseText)) {			
			} else {
				document.getElementById("error_txt").innerHTML = ajax.responseText;
			}*/
		},
		kickUser : function (user) {
			chatClient.sendJSONRequest("channel/"+chatClient.channelID+"/kick/", (typeof user === "number" ? {
				"session": chatClient.session,
				"userID" : user
			} : {
				"session": chatClient.session,
				"username" : user
			}), function (response) {
				if (response.status === 200) {
					$("<div class='message_text green' />").text(response.message).appendTo("#messages");
					chatClient.checkMessages(false, chatClient.channelID);
				} else {
					alert(response.message);
				}
			});
		},
		tempBanUser : function (user) {
			var stringDuration = prompt("Set a duration for the temporary ban, in minutes (between 1 and 360).", 60);
			if (stringDuration == null)
				return;
			var duration = parseInt(stringDuration);
			if (!$.isNumeric(duration) || duration > 360 || duration < 1) {
				alert("Invalid argument entered. You must specify a number between 1 and 360. ");
				chatClient.tempBanUser(user);
				return;
			}
			chatClient.sendJSONRequest("channel/"+chatClient.channelID+"/tempban/", (typeof user === "number" ? {
				"session": chatClient.session,
				"userID" : user,
				"duration": duration
			} : {
				"session": chatClient.session,
				"username" : user,
				"duration": duration
			}), function (response) {
				if (response.status === 200) {
					$("<div class='message_text green' />").text(response.message).appendTo("#messages");
					chatClient.checkMessages(false, chatClient.channelID);
				} else {
					alert(response.message);
				}
			});
		},
		resetChannel : function () {
			chatClient.sendJSONRequest("channel/"+chatClient.channelID+"/reset/", {
				"session": chatClient.session
			}, function (response) {
				if (response.status === 202) {
					$("<div class='message_text green' />").text(response.message).appendTo("#messages");
					chatClient.checkMessages(false, chatClient.channelID);
				} else {
					alert(response.message);
				}
			});
		},
		channelList : {
			addToList : function (userID, username, group) {
				if ($("#user_"+userID).length === 0) {
					$("<li id='user_"+userID+"' class='channel_user' />").appendTo("#userlist")
					.append("<img src='"
							+(group.icon ? group.icon : "images/ranks/default.png")
							+"' alt='' title='Group: "+group.name+"' />")
							//+(chatClient.channelRankDetails.length >= rank ? chatClient.channelRankDetails[rank].rankName : "")
							//+"("+rank+")' />")
					.append("<span>"+username+"</span>")
					.data({
						userID: userID,
						group: group,
						username: username
					}).click(function () {
						chatClient.channelList.createOptionList(this);
					});
				}
			},
			removeFromList : function (userID) {
				$("#user_"+userID).remove();
			},
			updateOnList : function (userID, username, group) {
				if ($("#user_"+userID).length > 0) {
					$("#user_"+userID).data({
						userID: userID,
						group: group,
						username: username
					});
					$("#user_"+userID+" span").text(username);
					$("#user_"+userID+" img")
					.attr("src", (group.icon ? group.icon : "images/ranks/default.png"))
					.attr("title", "Rank: "+group.name);
				}
			},
			sort : function () {
				sortElements("#userlist");				
			},
			createOptionList : function (sender) {
				$("<ul class='dropdown' id='userlist_dropdown' />").appendTo(sender)
				.css({width: $(sender).width()});
				$(sender).unbind('click').click(function () {
					$("#userlist_dropdown").remove();
					$(this).unbind('click').click(function () {
						chatClient.channelList.createOptionList(this);
					});
				});
				var senderGroup = $(sender).data("group");
				if (chatClient.userRank > senderGroup.id && chatClient.hasPermission("kick")) {
					//If the user has permission to kick (and the target user is of a lower rank), add an option to remove the user
					$("<li />").text("Kick").click(function () {
						$("<div class='message_text' />").text("Attempting to kick "+$(sender).data("username")+" from this channel.").appendTo("#messages");
						chatClient.kickUser($(sender).data("userID"));
					}).appendTo("#userlist_dropdown");
				}
				if (chatClient.userRank > senderGroup.id && chatClient.hasPermission("kick") && chatClient.hasPermission("tempban")) {
					//If the user has permission to kick (and the target user is of a lower rank), add an option to remove the user
					$("<li />").text("Kick and Temp Ban").click(function () {
						$("<div class='message_text' />").text("Attempting to kick "+$(sender).data("username")+" from this channel.").appendTo("#messages");
						chatClient.kickUser($(sender).data("userID"));
						chatClient.tempBanUser($(sender).data("userID"));
					}).appendTo("#userlist_dropdown");
				}
				if (senderGroup.id == 0 && chatClient.hasPermission("RANKCHANGE")) {
					//If the user has permission to kick (and the target user is of a lower rank), add an option to remove the user
					$("<li />").text("Rank user").click(function () {
						chatClient.addRank($(sender).data("userID"));
					}).appendTo("#userlist_dropdown");
				}
			}
		},
		changeRank : function (userID, newRank) {
			chatClient.sendJSONRequest("channel/"+chatClient.channelID+"/ranks/update/", {
				"session": chatClient.session,
				"userID" : userID,
				"rankID" : newRank
			}, function (response) {
				if (response.status === 200) {
					//alert(response.message);
					$("<div class='message_text green' />").text(response.message).appendTo("#messages");
					chatClient.checkMessages(false, chatClient.channelID);
				} else {
					alert(response.message);
				}
			});
		},
		removeRank : function (userID) {
			chatClient.sendJSONRequest("channel/"+chatClient.channelID+"/ranks/remove/", {
				"session": chatClient.session,
				"userID" : userID
			}, function (response) {
				if (response.status === 200) {
					//alert(response.message);
					$("<div class='message_text green' />").text(response.message).appendTo("#messages");
					chatClient.checkMessages(false, chatClient.channelID);
				} else {
					alert(response.message);
				}
			});
		},
		addRank : function (user) {
			chatClient.sendJSONRequest("channel/"+chatClient.channelID+"/ranks/add/", 
					(typeof user === "number" ? {
				"session": chatClient.session,
				"userID" : user
			} : {
				"session": chatClient.session,
				"username" : user
			}), function (response) {
				if (response.status === 200) {
					$("<div class='message_text green' />").text(response.message).appendTo("#messages");
					chatClient.checkMessages(false, chatClient.channelID);
				} else {
					alert(response.message);
				}
			});
		},
		rankList : {
			isOpen : false,
			open : function () {	
				var thisList = chatClient.rankList;
				chatClient.dialog.showDialog("Ranks", "<ul id='ranklist' class='dialogList'></ul>", chatClient.dialogTypes.RANKLIST);
				$("#dialog").css({
					height: "25em",
					marginTop: "-12.5em"
				});
				chatClient.dialog.addCloseEvent(function () {
					thisList.isOpen = false;
				});
				chatClient.sendJSONRequest("channel/"+chatClient.channelID+"/ranks/", {
					"session": chatClient.session
				}, function (response) {
					if (response.status === 200) {
						thisList.isOpen = true;
						$("#ranklist").empty();
						$(response.ranks).each(function (index, selectedUser) {
							thisList.addToList(selectedUser.userID, selectedUser.username, selectedUser.group);
						});
						thisList.sort();
						$("<input type='button' value='Add Rank' />").appendTo("#dialog_controls")
						.click(function () {
							var rankName = prompt("Enter the name of the user you wish to rank:");
							if (rankName == null || rankName.length < 1) {
								return;
							}
							chatClient.addRank(rankName);
						});
					} else {
						alert(response.message);
					}
				});				
			},
			addToList : function (userID, username, group) {
				if (chatClient.rankList.isOpen && $("#ranklist_"+userID).length === 0) {
					$("<li id='ranklist_"+userID+"' />").appendTo("#ranklist");
					$("<div class='channel_user' />").appendTo("#ranklist_"+userID)
					.css({display: "inline-block", width: "200px"})
					//.append("<img src='images/ranks/rank"+rank+".png' alt='' />")
					.append("<img src='"+(group.icon ? group.icon : "images/ranks/default.png")
							+"' alt='' title='Group: "+group.name+"' />")
					.append("<span>"+username+"</span>")
					.data({
						userID: userID,
						group: group,
						username: username
					}).click(function () {
						chatClient.rankList.createOptionList(this);
					});
					var groupSelect = $("<select />").appendTo("#ranklist_"+userID);
					$(chatClient.channelGroups).each(function (i, groupDeatils) {
						if (groupDeatils.id == 0)
							return;
						$("<option value='"+groupDeatils.id+"' />").appendTo(groupSelect)
						.text(groupDeatils.name)
						.prop("disabled", ((chatClient.userRank > groupDeatils.id) ? false : true));
						
					});
					var elems = $(groupSelect).children('option').detach();
				    elems.sort(function(a,b){
				    	return (parseInt($(a).val()) > parseInt($(b).val())) ? 1 : -1;
				    });
				    $(groupSelect).append(elems)
					.val(group.id)
					.prop("disabled", ((chatClient.userRank > group.id) ? false : true))
					.change(function() {
						chatClient.changeRank(userID, $(this).val());
						//alert("Call to change user: "+userID+" to rank: "+$(this).val());
					});	
				}
			},
			removeFromList : function (userID) {
				if (chatClient.rankList.isOpen) {
					$("#ranklist_"+userID).remove();
				}
			},
			updateOnList : function (userID, username, group) {
				if (chatClient.rankList.isOpen	&& $("#ranklist_"+userID).length > 0) {
					$("#ranklist_"+userID+" div").data({
						userID: userID,
						group: group,
						username: username
					});
					$("#ranklist_"+userID+" div span").text(username);
					$("#ranklist_"+userID+" div img")
					.attr("src", "images/ranks/rank"+group.id+".png");
					$("#ranklist_"+userID+" select").val(group.id)
					.prop("disabled", ((chatClient.userRank > group.id) ? false : true));
				}
			},
			sort : function () {
				sortElements("#ranklist");				
			},
			createOptionList : function (sender) {
				var menu;
				if ($("#ranklist_dropdown").length < 1) {
					menu = "<ul class='dropdown' id='ranklist_dropdown' />";
				} else {
					menu = "#ranklist_dropdown";
				}
				$(menu).empty().appendTo(sender)
				.css({width: $(sender).width()});
				$(sender).unbind('click').click(function () {
					$("#ranklist_dropdown").remove();
					$(this).unbind('click').click(function () {
						chatClient.rankList.createOptionList(this);
					});
				});
				if ($(sender).data("group").id < chatClient.userRank) {
					$("<li />").text("Remove rank").click(function () {
						if (confirm("Are you sure you wish to remove the rank of: "+$(sender).data("username"))) {
							chatClient.removeRank($(sender).data("userID"));
						}						
					}).appendTo("#ranklist_dropdown");	
				}
				
			}
		},
		addBan : function (user) {
			chatClient.sendJSONRequest("channel/"+chatClient.channelID+"/bans/add/", 
					(typeof user === "number" ? {
				"session": chatClient.session,
				"userID" : user
			} : {
				"session": chatClient.session,
				"username" : user
			}), function (response) {
				if (response.status === 200) {
					$("<div class='message_text green' />").text(response.message).appendTo("#messages");
					chatClient.checkMessages(false, chatClient.channelID);
				} else {
					alert(response.message);
				}
			});
		},
		removeBan : function (userID) {
			chatClient.sendJSONRequest("channel/"+chatClient.channelID+"/bans/remove/", {
				"session": chatClient.session,
				"userID" : userID
			}, function (response) {
				if (response.status === 200) {
					//alert(response.message);
					$("<div class='message_text green' />").text(response.message).appendTo("#messages");
					chatClient.checkMessages(false, chatClient.channelID);
				} else {
					alert(response.message);
				}
			});
		},
		banList : {
			isOpen : false,
			open : function () {
				var thisList = chatClient.banList;
				chatClient.dialog.showDialog("Bans", "<ul id='banlist' class='dialogList'></ul>", chatClient.dialogTypes.BANLIST);
				$("#dialog").css({
					height: "25em",
					marginTop: "-12.5em"
				});
				chatClient.dialog.addCloseEvent(function () {
					thisList.isOpen = false;
				});
				chatClient.sendJSONRequest("channel/"+chatClient.channelID+"/bans/", {
					"session": chatClient.session
				}, function (response) {
					if (response.status === 200) {
						thisList.isOpen = true;
						$("#banlist").empty();
						$(response.bans).each(function (index, selectedUser) {
							thisList.addToList(selectedUser.userID, selectedUser.username);
						});
						thisList.sort();
						$("<input type='button' value='Add Ban' />").appendTo("#dialog_controls")
						.click(function () {
							var banName = prompt("Enter the name of the user you wish to ban:");
							if (banName == null || banName.length < 1) {
								return;
							}
							chatClient.addBan(banName);
						});
					} else {
						alert(response.message);
					}
				});
			},
			addToList : function (userID, username) {
				if (chatClient.banList.isOpen && $("#banlist_"+userID).length === 0) {
					$("<li id='banlist_"+userID+"' />").appendTo("#banlist");
					$("<div class='channel_user' />").appendTo("#banlist_"+userID)
					.css({display: "inline-block", width: "200px", color:"#FF0000"})
					.append("<div sytle='display: inline-block;width:1em;' />")
					.append("<span>"+username+"</span>")
					.data({
						userID: userID,
						username: username
					}).click(function () {
						chatClient.banList.createOptionList(this);
					});	
				}
			},
			removeFromList : function (userID) {
				if (chatClient.banList.isOpen) {
					$("#banlist_"+userID).remove();
				}
			},
			updateOnList : function (userID, username) {
				if (chatClient.banList.isOpen && $("#banlist_"+userID).length > 0) {
					$("#banlist_"+userID+" div").data({
						userID: userID,
						username: username
					});
					$("#banlist_"+userID+" div span").text(username);
				}
			},
			sort : function () {
				sortElements("#banlist");		
			},
			createOptionList : function (sender) {
				var menu;
				if ($("#banlist_dropdown").length < 1) {
					menu = "<ul class='dropdown' id='banlist_dropdown' />";
				} else {
					menu = "#banlist_dropdown";
				}
				$(menu).empty().appendTo(sender)
				.css({width: $(sender).width()});
				$(sender).unbind('click').click(function () {
					$("#banlist_dropdown").remove();
					$(this).unbind('click').click(function () {
						chatClient.banList.createOptionList(this);
					});
				});
				$("<li />").text("Remove ban").click(function () {
					if (confirm("Are you sure you wish to remove the ban for: "+$(sender).data("username"))) {
						chatClient.removeBan($(sender).data("userID"));
					}						
				}).appendTo("#banlist_dropdown");
			}
		},
		changePermission : function (name, value) {
			chatClient.sendJSONRequest("channel/"+chatClient.channelID+"/permissions/change/", {
				"session": chatClient.session,
				"permissionName" : name,
				"value" : value
			}, function (response) {
				if (response.status === 200) {
					//alert(response.message);
					$("<div class='message_text green' />").text(response.message).appendTo("#messages");
					chatClient.checkMessages(false, chatClient.channelID);
				} else {
					alert(response.message);
				}
			});
		},
		permissionList : {
			isOpen : false,
			open : function () {
				var thisList = chatClient.permissionList;
				chatClient.dialog.showDialog("Permissions", "<ul id='permissionlist'></ul>", chatClient.dialogTypes.PERMISSIONS);
				$("#dialog").css({
					height: "25em",
					marginTop: "-12.5em"
				});
				chatClient.dialog.addCloseEvent(function () {
					thisList.isOpen = false;
				});
				chatClient.sendJSONRequest("channel/"+chatClient.channelID+"/permissions/", {
					"session": chatClient.session
				}, function (response) {
					if (response.status === 200) {
						thisList.isOpen = true;
						$("#permissionlist").empty();					
						$.each(response.permissions, function (index, permission) {
							thisList.addToList(permission.permissionID, permission.name, 
								permission.value, permission.minValue, permission.maxValue);
						});
						thisList.sort();
					} else {
						chatClient.dialog.closeDialog();
						alert(response.message);
					}
				});				
			},
			addToList : function (id, name, value, minValue, maxValue) {
				if (chatClient.permissionList.isOpen && $("#permissionlist_"+id).length === 0) {
					$("<li id='permissionlist_"+id+"' />").appendTo("#permissionlist");
					$("<b />").appendTo("#permissionlist_"+id)
					.css({display: "inline-block", width: "200px", textTransform : "capitalize"})
					.append("<span>"+name+": </span>")
					.data({
						permissionID: id,
						permissionName: name,
						permissionValue: value
					});
					var rankSelect = $("<select id='permissionselect_"+id+"+' />").appendTo("#permissionlist_"+id);
					$(chatClient.channelGroups).each(function (i, groupDetails) {
						if (groupDetails.id < minValue)
							return;
						$("<option value='"+groupDetails.id+"' />").appendTo(rankSelect)
						.text(groupDetails.name+"("+groupDetails.id+")")
						.prop("disabled", ((chatClient.userRank >= groupDetails.id) ? false : true));
					});
					var elems = $(rankSelect).children('option').detach();
				    elems.sort(function(a,b){
				    	return (parseInt($(a).val()) > parseInt($(b).val())) ? 1 : -1;
				    });
					$(rankSelect).append(elems)
					.val(value)
					.prop("disabled", ((chatClient.userRank >= value) ? false : true))
					.change(function() {
						chatClient.changePermission(name, $(this).val());
						//alert("Call to change user: "+userID+" to rank: "+$(this).val());
					});
				}
			},
			removeFromList : function (permissionID) {
				if (chatClient.permissionList.isOpen) {
					$("#permissionlist_"+permissionID).remove();
				}
			},
			updateOnList : function (id, name, value) {
				//alert($("#permissionlist_"+id).length);
				if (chatClient.permissionList.isOpen && $("#permissionlist_"+id).length > 0) {
					$("#permissionlist_"+id+" b").data({
						permissionID: id,
						permissionName: name,
						permissionValue: value
					});
					$("#permissionlist_"+id+" b span").text(name.toLowerCase());
					$("#permissionlist_"+id+" select").val(value)
					.prop("disabled", ((chatClient.userRank >= value) ? false : true));
				}
			},
			sort : function () {
				sortElements("#permissionlist");		
			}
		},
		groupList : {
			isOpen : false,
			open : function () {
				var thisList = chatClient.groupList;
				chatClient.dialog.showDialog("Groups", "<ul id='grouplist'></ul>", chatClient.dialogTypes.GROUPLIST);
				$("#dialog").css({
					height: "25em",
					marginTop: "-12.5em"
				});
				chatClient.dialog.addCloseEvent(function () {
					thisList.isOpen = false;
				});
				chatClient.sendJSONRequest("channel/"+chatClient.channelID+"/groups/", {
					"session": chatClient.session
				}, function (response) {
					if (response.status === 200) {
						thisList.isOpen = true;
						$("#grouplist").empty();					
						$.each(response.groups, function (index, group) {
							thisList.addToList(group);
						});
						thisList.sort();
					} else {
						chatClient.dialog.closeDialog();
						alert(response.message);
					}
				});	
			},
			addToList : function (groupData) {
				var thisList = chatClient.groupList;
				if (thisList.isOpen && $("#grouplist_"+groupData.id).length === 0) {
					$("<li id='grouplist_"+groupData.id+"' />").appendTo("#grouplist");
					$("<b />").appendTo("#grouplist_"+groupData.id)
					.css({display: "inline-block", width: "200px"})
					.append("<span>"+groupData.name+": </span>")
					.data({
						id: groupData.id,
						name: groupData.name,
						type: groupData.type
					});
					/*var rankSelect = $("<select id='permissionselect_"+id+"+' />").appendTo("#permissionlist_"+id);
					$(chatClient.channelRankDetails).each(function (i, rankValue) {
						if (i < minValue)
							return;
						$("<option value='"+rankValue.rankID+"' />").appendTo(rankSelect)
						.text(rankValue.rankName+"("+rankValue.rankID+")")
						.prop("disabled", ((chatClient.userRank >= rankValue.rankID) ? false : true));
					});
					$(rankSelect).val(value)
					.prop("disabled", ((chatClient.userRank >= value) ? false : true))
					.change(function() {
						chatClient.changePermission(name, $(this).val());
						//alert("Call to change user: "+userID+" to rank: "+$(this).val());
					});*/
				}
			},
			sort : function () {
				sortElements("#grouplist");		
			}
		}
};