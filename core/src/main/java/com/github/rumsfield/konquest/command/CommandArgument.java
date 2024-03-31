package com.github.rumsfield.konquest.command;

import java.util.ArrayList;

public class CommandArgument {

    private final ArrayList<String> argOptions;
    private final ArrayList<Boolean> isOptionLiteral;
    private boolean isArgOptional;

    public CommandArgument(boolean isOptional) {
        this.argOptions = new ArrayList<>();
        this.isOptionLiteral = new ArrayList<>();
        this.isArgOptional = isOptional;
    }

    public boolean isArgOptional() {
        return isArgOptional;
    }

    public void setIsArgOptional(boolean val) {
        isArgOptional = val;
    }

    public int getNumOptions() {
        return argOptions.size();
    }

    public String getArgOption(int index) {
        if(index < 0 || index >= argOptions.size()) {
            return "";
        }
        return argOptions.get(index);
    }

    public boolean isArgOptionLiteral(int index) {
        if(index < 0 || index >= isOptionLiteral.size()) {
            return false;
        }
        return isOptionLiteral.get(index);
    }

    public void addArgOption(String option, boolean isLiteral) {
        argOptions.add(option);
        isOptionLiteral.add(isLiteral);
    }

    /**
     * Checks the given argument string to see whether it matches the options
     * @param arg The string to check
     * @return True if the arg matches a literal option or there are any non-literal options, else false
     */
    public boolean isArgMatch(String arg) {
        for(int i = 0; i < getNumOptions(); i++) {
            String option = argOptions.get(i);
            boolean isLiteral = isOptionLiteral.get(i);
            if(isLiteral) {
                // Check if exact match to literal
                if(option.equalsIgnoreCase(arg)) {
                    return true;
                }
            } else {
                // There is a non-literal option, consider it a match.
                // It's up to the command implementation to validate non-literal arguments.
                return true;
            }
        }
        // Finished checking all options, if any, but no matches
        return false;
    }

}
