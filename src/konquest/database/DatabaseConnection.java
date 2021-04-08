package konquest.database;

import java.sql.*;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


//import konquest.utility.ChatUtil;

public class DatabaseConnection {

    private Connection connection;
    private Properties properties;
    private ExecutorService queryExecutor;

    public DatabaseConnection() {
        queryExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        properties = new Properties();
    }

    public void connect() throws Exception {
        if (connection != null) {
            return;
        }
        String databaseName = "plugins/Konquest/KonquestDatabase";
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseName + ".db", properties);

            return;
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public void disconnect() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        connection = null;
    }

    public void executeUpdate(String query) {
        Statement statement = null;

        try {
            statement = connection.createStatement();
            //ChatUtil.printDebug("Executing SQLite Update: "+query);
            statement.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    public ResultSet executeQuery(String query) {
    	Statement statement = null;
    	ResultSet result = null;
    	
    	try {
            statement = connection.createStatement();
            //ChatUtil.printDebug("Executing SQLite Query: "+query);
            result = statement.executeQuery(query);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    	return result;
    }

    public PreparedStatement prepare(String sql) {
        if (connection == null) {
            return null;
        }

        try {
            return connection.prepareStatement(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public ResultSet scheduleQuery(String query) {
        Future<ResultSet> futureResult = queryExecutor.submit(new AsyncQuerySQL(this, query));

        try {
            return futureResult.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return null;
    }

    public void scheduleUpdate(String query) {
        queryExecutor.execute(new AsyncUpdateSQL(this, query));
    }

    public void pingDatabase() {
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.executeQuery("SELECT 1;");
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setProperties(String autoReconnect, String user, String password) {
        properties.put("autoReconnect", autoReconnect);
        properties.put("user", user);
        properties.put("password", password);
    }

    public Connection getConnection() {
        return connection;
    }
}
