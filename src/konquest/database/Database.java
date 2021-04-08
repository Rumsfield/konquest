package konquest.database;

import java.sql.ResultSet;
import java.sql.SQLException;

import konquest.Konquest;
//import konquest.utility.ChatUtil;

public abstract class Database {
    private DatabaseConnection databaseConnection;
    private Konquest konquest;

    public Database(Konquest konquest) {
        this.konquest = konquest;
    	databaseConnection = new DatabaseConnection();
    }

    public abstract void initialize();

    public DatabaseConnection getDatabaseConnection() {
        return databaseConnection;
    }
    
    public Konquest getKonquest() {
    	return konquest;
    }

    public boolean exists(String table) {
    	String query = "SELECT name FROM sqlite_master WHERE type='table' AND name='"+table+"';";
    	ResultSet result = databaseConnection.scheduleQuery(query);
    	//ResultSet result = databaseConnection.executeQuery(query);
    	boolean hasRow = false;
        try {
        	hasRow = result.next();
        	//ChatUtil.printDebug("SQL table "+table+" exists? "+hasRow);
        } catch (SQLException e) {
            e.printStackTrace();
            hasRow = false;
            //ChatUtil.printDebug("SQL table "+table+" exists encountered an exception! "+hasRow);
        }
        return hasRow;
    }
    
    public boolean exists(String table, String column) {
    	String query = "PRAGMA table_info('"+table+"');";
    	ResultSet result = databaseConnection.scheduleQuery(query);
    	boolean hasColumn = false;
        try {
        	while(result.next()) {
        		if(column.equalsIgnoreCase(result.getString(2))) {
        			hasColumn = true;
        			break;
        		}
        		//ChatUtil.printDebug("SQL table "+table+" column info: "+result.getString(1)+", "+result.getString(2));
        	}
        	//ChatUtil.printDebug("SQL table "+table+" column "+column+" exists? "+hasColumn);
        } catch (SQLException e) {
            e.printStackTrace();
            //ChatUtil.printDebug("SQL table "+table+" column "+column+" exist encountered an exception! "+hasColumn);
        }
        return hasColumn;
    }
    
    /*public boolean exists(String table, String column) {
    	String query = "SELECT * FROM "+table+" LIMIT 1;";
    	ResultSet result = databaseConnection.scheduleQuery(query);
    	//ResultSet result = databaseConnection.executeQuery(query);
    	boolean hasColumn = false;
    	int colIndex = -1;
        try {
        	if(result.next()){
        		colIndex = result.findColumn(column);
        		hasColumn = (colIndex != -1);
        	}
        	ChatUtil.printDebug("SQL table "+table+" column "+column+" exists? "+colIndex+", "+hasColumn);
        } catch (SQLException e) {
            e.printStackTrace();
            hasColumn = false;
            ChatUtil.printDebug("SQL table "+table+" column "+column+" exist encountered an exception! "+hasColumn);
        }
        return hasColumn;
    }*/

    /*public boolean exists(String table, String column) {
    	String query = "SELECT COUNT(*) AS CNTREC FROM pragma_table_info('"+table+"') WHERE name='"+column+"';";
    	ResultSet result = databaseConnection.scheduleQuery(query);
    	boolean hasColumn = false;
        try {
        	hasColumn = result.next();
        	ChatUtil.printDebug("SQL table "+table+" column "+column+" exists? "+hasColumn);
        } catch (SQLException e) {
            e.printStackTrace();
            ChatUtil.printDebug("SQL table "+table+" column "+column+" exist encountered an exception! "+hasColumn);
        }
        return hasColumn;
    }*/
    
    public boolean exists(String table, String column, String search) {
        String query = "SELECT COUNT(1) FROM " + table + " WHERE " + column + " = '" + search +"';";
        ResultSet result = databaseConnection.scheduleQuery(query);
        int count = 0;
        try {
            if(result.next()){
                count = result.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        if (count == 0) {
            return false;
        } else {
            return true;
        }
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
            sql.append("'" + values[index] + "'");
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
