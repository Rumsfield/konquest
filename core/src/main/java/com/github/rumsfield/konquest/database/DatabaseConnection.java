package com.github.rumsfield.konquest.database;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.bukkit.configuration.file.FileConfiguration;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.utility.ChatUtil;


//import com.github.rumsfield.konquest.utility.ChatUtil;

public class DatabaseConnection {

    private Connection connection;
    private ExecutorService queryExecutor;
    private DatabaseType type;

    public DatabaseConnection(DatabaseType type) {
        queryExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.type = type;
    }

    public void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
        	ChatUtil.printConsoleAlert("Could not connect to SQL database of type "+type.toString()+", connection is already open.");
            return;
        }
        Properties properties = new Properties();
        switch(type) {
        	case SQLITE:
        		try {
        			ChatUtil.printConsoleAlert("Connecting to SQLite database");
        			migrateDatabaseFile("KonquestDatabase.db","data/KonquestDatabase.db");
                	String databaseName = "plugins/Konquest/data/KonquestDatabase";
                    connection = DriverManager.getConnection("jdbc:sqlite:" + databaseName + ".db", properties);
                    return;
                } catch (SQLException e) {
                	ChatUtil.printConsoleAlert("Failed to connect to SQLite database!");
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
                	properties.put("user", username);
                	String password = coreConfig.getString("core.database.mysql.password","");
                    properties.put("password", password);
                    for(String nameValuePair : coreConfig.getStringList("core.database.mysql.properties")) {
                    	String[] propNameValue = nameValuePair.split("=",2);
                    	if(propNameValue.length == 2) {
                    		properties.put(propNameValue[0], propNameValue[1]);
                    	}
                    }
                    // DEBUG
                    ChatUtil.printDebug("Applying connection properties...");
                    for(String key : properties.stringPropertyNames()) {
                    	String value = properties.getProperty(key);
                    	ChatUtil.printDebug("  "+key+" = "+value);
                    }
                    // END DEBUG
                    connection = DriverManager.getConnection("jdbc:mysql://" + hostname + ":" + port + "/" + database, properties);
                    return;
                } catch (SQLException e) {
                	ChatUtil.printConsoleAlert("Failed to connect to MySQL database!");
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
			e.printStackTrace();
		}

        connection = null;
    }

    /*
    public void executeUpdate(String query) {
        Statement statement = null;

        try {
        	ChatUtil.printDebug("Executing SQL Update: "+query);
            statement = connection.createStatement();
            statement.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
            ChatUtil.printConsoleError("Failed to execute SQL update, is the connection closed?");
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                ChatUtil.printConsoleError("Failed to close SQL update statement, is the connection closed?");
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
            ChatUtil.printConsoleError("Failed to execute SQL query, is the connection closed?");
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                ChatUtil.printConsoleError("Failed to close SQL query statement, is the connection closed?");
            }
        }
    	return result;
    }
	*/
    
    public PreparedStatement prepare(String sql) {
        if (connection == null) {
            return null;
        }

        try {
            return connection.prepareStatement(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            ChatUtil.printConsoleError("Failed to prepare SQL statement, is the connection closed?");
        }

        return null;
    }

    public ResultSet scheduleQuery(String query) {
    	// Verify good connection, try to reconnect
    	if(testConnection(true)) {
    		ChatUtil.printConsoleAlert("Successfully reconnected to database");
    	}
    	
        Future<ResultSet> futureResult = queryExecutor.submit(new AsyncQuerySQL(this, query));
        try {
            return futureResult.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            ChatUtil.printConsoleError("Failed to schedule SQL query, InterruptedException");
        } catch (ExecutionException e) {
            e.printStackTrace();
            ChatUtil.printConsoleError("Failed to schedule SQL query, ExecutionException");
        }

        return null;
    }

    public void scheduleUpdate(String query) {
    	// Verify good connection, try to reconnect
    	if(testConnection(true)) {
    		ChatUtil.printConsoleAlert("Successfully reconnected to database");
    	}
    	
        queryExecutor.execute(new AsyncUpdateSQL(this, query));
    }

    public boolean pingDatabase() {
    	boolean result = false;
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.executeQuery("SELECT 1;");
            statement.close();
            result = true;
        } catch (SQLException e) {
        	ChatUtil.printDebug("Failed to ping SQL database, caught exception:");
        	ChatUtil.printDebug(e.getMessage());
        }
        return result;
    }

    public Connection getConnection() {
        return connection;
    }
    
    private boolean testConnection(boolean reconnect) {
    	boolean result = false;
    	Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.executeQuery("SELECT 1;");
            statement.close();
        } catch (SQLException e) {
        	if(reconnect) {
        		ChatUtil.printConsoleError("Failed to connect to database, trying to reconnect");
        		try {
        			connect();
        			result = true;
        		} catch(SQLException r) {
        			e.printStackTrace();
        			r.printStackTrace();
        		}
        	} else {
        		ChatUtil.printConsoleError("Failed to connect to database :(");
        		e.printStackTrace();
        	}
        }
    	return result;
    }
    
    private void migrateDatabaseFile(String oldPath, String newpath) {
		File oldFile = new File(Konquest.getInstance().getPlugin().getDataFolder(), oldPath);
		File newFile = new File(Konquest.getInstance().getPlugin().getDataFolder(), newpath);
		if(oldFile.exists()) {
			Path source = oldFile.toPath();
			Path destination = newFile.toPath();
			try {
				Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
				oldFile.delete();
				ChatUtil.printConsoleAlert("Migrated database file "+oldPath+" to "+newpath);
			} catch (IOException e) {
				e.printStackTrace();
				ChatUtil.printDebug("Failed to move database file "+oldPath+" to "+newpath);
			}
		}
	}
}
