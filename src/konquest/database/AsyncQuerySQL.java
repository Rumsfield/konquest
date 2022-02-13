package konquest.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Callable;

import konquest.utility.ChatUtil;

public class AsyncQuerySQL implements Callable<ResultSet> {
    private DatabaseConnection connection;
    private String query;

    AsyncQuerySQL(DatabaseConnection connection, String query) {
        this.connection = connection;
        this.query = query;
    }

    public ResultSet call() {
    	PreparedStatement statement = null;
    	try {
        	ChatUtil.printDebug("Executing SQL Query: "+query);
            statement = connection.prepare(query);
            ResultSet result = statement.executeQuery();
            return result;
        } catch (SQLException e) {
        	ChatUtil.printConsoleError("Failed to execute SQL query, is the connection closed?");
            e.printStackTrace();
        }
        return null;
    }
}
