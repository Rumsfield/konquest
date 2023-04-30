package com.github.rumsfield.konquest.database;

import com.github.rumsfield.konquest.utility.ChatUtil;

import java.util.ArrayList;


public class Table {
    private final String name;
    private final Database database;
    private final ArrayList<Column> columns;

    public Table(String name, Database database) {
        this.name = name;
        this.database = database;
        columns = new ArrayList<>();
    }

    public void add(Column column) {
        column.setTable(this);
        columns.add(column);
    }
    
    public void execute() {
    	if(database.exists(name)) {
    		// Attempt to add all missing columns
    		for(Column col : columns) {
    			if(database.exists(name,col.getName())) continue;
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
            return;
    	}
        ChatUtil.printConsole("SQL database is missing table '"+name+"', creating it now.");
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
