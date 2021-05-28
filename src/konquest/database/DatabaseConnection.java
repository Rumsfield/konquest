package konquest.database;

import java.sql.*;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.bukkit.configuration.file.FileConfiguration;

import konquest.Konquest;
import konquest.utility.ChatUtil;


//import konquest.utility.ChatUtil;

public class DatabaseConnection {

    private Connection connection;
    private Properties properties;
    private ExecutorService queryExecutor;
    private DatabaseType type;

    public DatabaseConnection(DatabaseType type) {
        queryExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        properties = new Properties();
        this.type = type;
    }

    public void connect() throws Exception {
        if (connection != null && !connection.isClosed()) {
        	ChatUtil.printConsoleAlert("Could not connect to SQL database of type "+type.toString()+", connection is already open.");
            return;
        }
        
        switch(type) {
        	case SQLITE:
        		try {
        			ChatUtil.printConsoleAlert("Connecting to SQLite database");
                	String databaseName = "plugins/Konquest/KonquestDatabase";
                    connection = DriverManager.getConnection("jdbc:sqlite:" + databaseName + ".db", properties);
                    return;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
        		break;
        	case MYSQL:
        		try {
        			ChatUtil.printConsoleAlert("Connecting to MySQL database");
        			FileConfiguration coreConfig = Konquest.getInstance().getConfigManager().getConfig("core");
                	String hostname = coreConfig.getString("core.database.mysql.hostname");
                	String port = coreConfig.getString("core.database.mysql.port");
                	String database = coreConfig.getString("core.database.mysql.database");
                	String username = coreConfig.getString("core.database.mysql.username","");
                	String password = coreConfig.getString("core.database.mysql.password","");
                    connection = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + database, username, password);
                    return;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
        		break;
        	default:
        		ChatUtil.printConsoleError("Could not connect to unknown database type "+type.toString());
        }
        
    }

    public void disconnect() {
        try {
            if (connection != null) {
                queryExecutor.shutdown();
                queryExecutor.awaitTermination(5, TimeUnit.SECONDS);
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        connection = null;
    }

    public void executeUpdate(String query) {
        Statement statement = null;

        try {
            statement = connection.createStatement();
            ChatUtil.printDebug("Executing SQL Update: "+query);
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
            ChatUtil.printDebug("Executing SQL Query: "+query);
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
