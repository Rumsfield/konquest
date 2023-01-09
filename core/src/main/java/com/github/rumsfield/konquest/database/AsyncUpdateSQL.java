package com.github.rumsfield.konquest.database;

import java.sql.SQLException;
import java.sql.Statement;

import com.github.rumsfield.konquest.utility.ChatUtil;

public class AsyncUpdateSQL implements Runnable {
    private DatabaseConnection connection;
    private String query;

    public AsyncUpdateSQL(DatabaseConnection connection, String query) {
        this.connection = connection;
        this.query = query;
    }
    
    public void run() {
    	Statement statement = null;
    	Statement backupStatement = null;
        try {
        	ChatUtil.printDebug("Executing SQL Update: "+query);
        	// First attempt
        	statement = connection.getConnection().createStatement();
            statement.executeUpdate(query);
        } catch (SQLException e) {
            ChatUtil.printConsoleError("Failed to execute SQL update, attempting to reconnect");
        	ChatUtil.printDebug(e.getMessage());
        	try {
        		// Second attempt
        		connection.connect();
        		backupStatement = connection.getConnection().createStatement();
        		backupStatement.executeUpdate(query);
        	} catch(SQLException r) {
        		ChatUtil.printConsoleError("Failed to execute SQL query after reconnect. Check your database settings.");
        		r.printStackTrace();
        	} finally {
        		if (backupStatement != null) {
    	        	try {
    	        		backupStatement.close();
    	            } catch (SQLException s2) {
    	            	ChatUtil.printConsoleError("Failed to close SQL update backup statement");
    	            	s2.printStackTrace();
    	            }
            	}
        	}
        } finally {
        	if (statement != null) {
	        	try {
	                statement.close();
	            } catch (SQLException s1) {
	            	ChatUtil.printConsoleError("Failed to close SQL update statement");
	            	s1.printStackTrace();
	            }
        	}
        }
    }
}
