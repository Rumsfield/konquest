package com.github.rumsfield.konquest.database;

public class Column {
    private String name;
    private String type;
    private String defaultValue;
    private Table table;
    private boolean primary;

    public Column(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getDefaultValue() {
        return defaultValue == null ? "" : defaultValue;
    }

    public String getType() {
        return type;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public void setTable(Table table) {
        if (this.table == null) {
            this.table = table;
        }
    }

    public void setType(String type) {
        this.type = type;
    }

}
