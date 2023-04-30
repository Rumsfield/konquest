package com.github.rumsfield.konquest.database;

import com.github.rumsfield.konquest.utility.ChatUtil;

import java.sql.ResultSet;


public abstract class Database {
    private final DatabaseConnection databaseConnection;
    private final DatabaseType type;

    public Database(DatabaseType type) {
        this.type = type;
    	databaseConnection = new DatabaseConnection(type);
    }

    public abstract void initialize();

    public DatabaseConnection getDatabaseConnection() {
        return databaseConnection;
    }
    
    public DatabaseType getType() {
    	return type;
    }

    public boolean exists(String table) {
    	String query;
    	if(type.equals(DatabaseType.SQLITE)) {
    		query = "SELECT name FROM sqlite_master WHERE type='table' AND name='"+table+"';";
    	} else if(type.equals(DatabaseType.MYSQL)) {
    		query = "SHOW TABLES LIKE '"+table+"';";
    	} else {
    		ChatUtil.printDebug("Failed to check for existing table in unknown database type: "+ type);
    		return false;
    	}
    	ResultSet result = databaseConnection.scheduleQuery(query);
    	boolean hasRow = false;
        try {
        	hasRow = result.next();
        } catch (Exception e) {
            ChatUtil.printDebug("Got null SQL result: "+e.getMessage());
        }
        return hasRow;
    }
    
    public boolean exists(String table, String column) {
    	String query;
    	int nameIndex;
    	if(type.equals(DatabaseType.SQLITE)) {
    		query = "PRAGMA table_info('"+table+"');";
    		nameIndex = 2;
    	} else if(type.equals(DatabaseType.MYSQL)) {
    		query = "DESCRIBE "+table+";";
    		nameIndex = 1;
    	} else {
    		ChatUtil.printDebug("Failed to check for existing table column in unknown database type: "+ type);
    		return false;
    	}
    	ResultSet result = databaseConnection.scheduleQuery(query);
    	boolean hasColumn = false;
        try {
        	while(result.next()) {
        		if(column.equalsIgnoreCase(result.getString(nameIndex))) {
        			hasColumn = true;
        			break;
        		}
        	}
        } catch (Exception e) {
            ChatUtil.printDebug("Got null SQL result: "+e.getMessage());
        }
        return hasColumn;
    }

    public boolean exists(String table, String column, String search) {
        String query = "SELECT COUNT(1) FROM " + table + " WHERE " + column + " = '" + search +"';";
        ResultSet result = databaseConnection.scheduleQuery(query);
        int count = 0;
        try {
            if(result.next()){
                count = result.getInt(1);
            }
        } catch (Exception e) {
            ChatUtil.printDebug("Got null SQL result: "+e.getMessage());
            return false;
        }

        return count != 0;
    }

    public void insert(String table, String[] columns, String[] values) {
        StringBuilder sql = new StringBuilder("INSERT INTO " + table + " (");

        for (int index = 0; index < columns.length; index++) {
            sql.append(columns[index]);
            if (index != (columns.length - 1)) {
                sql.append(",");
                sql.append(" ");
            }
        }

        sql.append(") VALUES (");

        for (int index = 0; index < values.length; index++) {
            sql.append("'").append(values[index]).append("'");
            if (index != (values.length - 1)) {
                sql.append(",");
                sql.append(" ");
            }
        }

        sql.append(");");
        databaseConnection.scheduleUpdate(sql.toString());
    }

    public ResultSet select(String table, String column, String search) {
        String sql = "SELECT * FROM " + table + " WHERE " + column + " = '" + search + "';";
        return databaseConnection.scheduleQuery(sql);
    }
    
    public ResultSet select(String table, String column) {
        String sql = "SELECT " + column + " FROM " + table + ";";
        return databaseConnection.scheduleQuery(sql);
    }
    
    public ResultSet selectAll(String table) {
        String sql = "SELECT * FROM " + table + ";";
        return databaseConnection.scheduleQuery(sql);
    }

    public void add(String table, String update, String add, String column, String search) {
        String sql = "UPDATE " + table + " SET " + update + " = " + update + " + " + add + " WHERE " + column + " = '" + search + "';";
        databaseConnection.scheduleUpdate(sql);
    }

    public void subtract(String table, String update, String subtract, String column, String search) {
        String sql = "UPDATE " + table + " SET " + update + " = " + update + " - " + subtract + " WHERE " + column + " = '" + search + "';";
        databaseConnection.scheduleUpdate(sql);
    }

    public void increment(String table, String update, String column, String search) {
        String sql = "UPDATE " + table + " SET " + update + " = " + update + " + 1 WHERE " + column + " = '" + search + "';";
        databaseConnection.scheduleUpdate(sql);
    }

    public void decrement(String table, String update, String column, String search) {
        String sql = "UPDATE " + table + " SET " + update + " = " + update + " - 1 WHERE " + column + " = '" + search + "';";
        databaseConnection.scheduleUpdate(sql);
    }

    public void set(String table, String update, String set, String column, String search) {
        String sql = "UPDATE " + table + " SET " + update + " = " + set + " WHERE " + column + " = '" + search + "'";
        databaseConnection.scheduleUpdate(sql);
    }
    
    public void set(String table, String[] update, String[] set, String column, String search) {
        //String sql = "UPDATE " + table + " SET " + update + " = " + set + " WHERE " + column + " = '" + search + "'";
    	StringBuilder sql = new StringBuilder("UPDATE " + table + " SET ");
        for (int index = 0; index < update.length; index++) {
            sql.append(update[index]).append(" = ").append(set[index]);
            if (index != (update.length - 1)) {
                sql.append(",");
                sql.append(" ");
            }
        }
        sql.append(" WHERE ");
        sql.append(column).append(" = '").append(search).append("';");
        databaseConnection.scheduleUpdate(sql.toString());
    }
}
