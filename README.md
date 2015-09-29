# ChatServer

ChatServer is a channel-based instant messaging program, run off a Java servlet. It allows users to create chat "channels", which can be joined by groups of users in order to communicate with each other. Channel creators can also ban/block other users from joining their channel (temporarily or permanently), restrict access to the channel so only approved members can join, and assign members to groups which have varying permissions (such as blocking other users, sending messages in the channel, changing the groups of other users, and so on). 

ChatServer uses a JSON API via HTTP to communicate between clients and the server. A Javascript client is included in this application, but the API allows applications in other languages and on other platforms to to communicate with the server using HTTP POST and GET requests. The application currently saves data to an SQL database, but future releases will be capable of storing to either SQL databases, XML files, or any other desired persistancy layer which is developed.

Users require an account to create and manage chat channels. Guest access is available via the API, but is not currently supported via the Javascript client. Currently, only standard username/password authentication is supported, but future releases should add other authentication capabilities, such as social media (Google, Facebook) logins.
