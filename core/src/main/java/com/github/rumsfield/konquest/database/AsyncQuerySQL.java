package com.github.rumsfield.konquest.database;

import com.github.rumsfield.konquest.utility.ChatUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;

public class AsyncQuerySQL implements Callable<ResultSet> {
    private final DatabaseConnection connection;
    private final String query;

    AsyncQuerySQL(DatabaseConnection connection, String query) {
        this.connection = connection;
        this.query = query;
    }

    public ResultSet call() {
    	PreparedStatement statement;
    	try {
        	ChatUtil.printDebug("Executing SQL Query: "+query);
        	// First attempt
            statement = connection.prepare(query);
            return statement.executeQuery();
        } catch (SQLException e) {
        	ChatUtil.printConsoleError("Failed to execute SQL query, attempting to reconnect");
        	ChatUtil.printDebug(e.getMessage());
        	try {
        		// Second attempt
        		connection.connect();
        		statement = connection.prepare(query);
                return statement.executeQuery();
        	} catch(SQLException r) {
        		ChatUtil.printConsoleError("Failed to execute SQL query after reconnect. Check your database settings.");
        		r.printStackTrace();
        	}
        }
        return null;
    }
}
