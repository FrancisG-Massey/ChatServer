/**
 * An implementation of the ChatServer persistence layer which uses JDBC to store and retrieve data.<br /><br />
 * 
 * This implementation is designed for heavy use applications where thousands of users and hundreds of channels are in uses simultaneously.
 * However, it is not suited for portable applications. The {@link com.sundays.chat.io.xml XML persistence layer} should be used instead for these cases
 */
package com.sundays.chat.io.jdbc;