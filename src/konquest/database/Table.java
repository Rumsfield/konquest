package konquest.database;

import java.util.ArrayList;

import konquest.utility.ChatUtil;


public class Table {
    private String name;
    //private String tempName;
    private Database database;
    private ArrayList<Column> columns;

    public Table(String name, Database database) {
        this.name = name;
        //this.tempName = name+"_tmp";
        this.database = database;
        columns = new ArrayList<Column>();
    }

    public void add(Column column) {
        column.setTable(this);
        columns.add(column);
    }
    
    public void execute() {
    	//ChatUtil.printDebug("Searching for table entries...");
    	if(database.exists(name)) {
    		//ChatUtil.printDebug("Found existing table "+name);
    		// Attempt to add all missing columns
    		for(Column col : columns) {
    			if(database.exists(name,col.getName())) {
    				//ChatUtil.printDebug("Found existing column "+col.getName());
    			} else {
    				//ChatUtil.printDebug("Missing column "+col.getName()+", adding now");
    				ChatUtil.printConsole("SQL database is missing column '"+col.getName()+"' in table '"+name+"', adding it now.");
    				StringBuilder addBuffer = new StringBuilder("ALTER TABLE ");
    	        	addBuffer.append(name).append(" ADD COLUMN ");
    	        	addBuffer.append(col.getName()).append(" ");
    	        	addBuffer.append(col.getType()).append(" ");
    	        	if (!col.getDefaultValue().isEmpty()) {
    	        		addBuffer.append("DEFAULT ");
    	        		addBuffer.append(col.getDefaultValue()).append(" ");
    	            }
    	        	database.getDatabaseConnection().scheduleUpdate(addBuffer.toString());
    			}
    		}
    	} else {
    		//ChatUtil.printDebug("Missing table "+name);
    		ChatUtil.printConsole("SQL database is missing table '"+name+"', creating it now.");
    		// Creates a new table with all current columns.
            StringBuilder buffer = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
            buffer.append(name);
            buffer.append(" ( ");
            for (int i = 0; i < columns.size(); i++) {
                Column column = columns.get(i);

                buffer.append(column.getName()).append(" ");
                buffer.append(column.getType()).append(" ");

                if (column.isPrimary()) {
                    buffer.append("PRIMARY KEY ");
                }

                if (!column.getDefaultValue().isEmpty()) {
                    buffer.append("DEFAULT ");
                    buffer.append(column.getDefaultValue()).append(" ");
                }

                if (i != (columns.size() - 1)) {
                    buffer.append(",");
                    buffer.append(" ");
                }
            }
            buffer.append(" );");
            database.getDatabaseConnection().scheduleUpdate(buffer.toString());
    	}
    }
    
    
    /**
     * Performs following procedure to update table with all necessary columns.
     * 
     * Creates a new temporary table with all current columns.
     * Copies all data from previous table, if exists.
     * Drops previous table, if exists.
     * Renames new temporary table to current name.
     */
    /*public void execute() {
    	// Drops previous table, if exists.
    	StringBuilder buffer = new StringBuilder("DROP TABLE IF EXISTS ");
        buffer.append(tempName);
        buffer.append(";");
    	// Creates a new temporary table with all current columns.
        buffer.append("CREATE TABLE ");
        buffer.append(tempName);
        buffer.append(" ( ");
        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);

            buffer.append(column.getName()).append(" ");
            buffer.append(column.getType()).append(" ");

            if (column.isPrimary()) {
                buffer.append("PRIMARY KEY ");
            }

            if (!column.getDefaultValue().isEmpty()) {
                buffer.append("DEFAULT ");
                buffer.append(column.getDefaultValue()).append(" ");
            }

            if (i != (columns.size() - 1)) {
                buffer.append(",");
                buffer.append(" ");
            }
        }
        buffer.append(" );");
        // Copies all data from previous table, if exists.
        buffer.append("INSERT INTO ");
        buffer.append(tempName);
        buffer.append(" SELECT ");
        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);

            buffer.append(column.getName());

            if (i != (columns.size() - 1)) {
                buffer.append(",");
                buffer.append(" ");
            }
        }
        buffer.append(" FROM ");
        buffer.append(name);
        buffer.append(";");
        // Drops previous table, if exists.
        buffer.append("DROP TABLE IF EXISTS ");
        buffer.append(name);
        buffer.append(";");
        // Renames new temporary table to current name.
        buffer.append("ALTER TABLE ");
        buffer.append(tempName);
        buffer.append(" RENAME TO ");
        buffer.append(name);
        buffer.append(";");
        
        database.getDatabaseConnection().scheduleUpdate(buffer.toString());
    }
    */

}
