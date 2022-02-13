package konquest.database;

import java.sql.SQLException;
import java.sql.Statement;

import konquest.utility.ChatUtil;

public class AsyncUpdateSQL implements Runnable {
    private DatabaseConnection connection;
    private String query;

    public AsyncUpdateSQL(DatabaseConnection connection, String query) {
        this.connection = connection;
        this.query = query;
    }

    /*
    public void run() {
        connection.executeUpdate(query);
    }
    */
    
    public void run() {
    	Statement statement = null;
        try {
        	ChatUtil.printDebug("Executing SQL Update: "+query);
            statement = connection.getConnection().createStatement();
            statement.executeUpdate(query);
        } catch (SQLException e) {
        	ChatUtil.printConsoleError("Failed to execute SQL update, is the connection closed?");
            e.printStackTrace();
        } finally {
        	if (statement != null) {
	        	try {
	                statement.close();
	            } catch (SQLException e) {
	            	ChatUtil.printConsoleError("Failed to close SQL update statement");
	            	e.printStackTrace();
	            }
        	}
        }
    }
}
