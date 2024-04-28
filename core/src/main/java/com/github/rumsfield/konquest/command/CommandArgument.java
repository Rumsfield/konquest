package com.github.rumsfield.konquest.command;

import java.util.ArrayList;
import java.util.List;

public class CommandArgument {

    private final ArrayList<String> argNames = new ArrayList<>();
    private final boolean isArgLiteral;
    private final boolean hasOptionalSubArg; // Can this argument be used without a sub-argument?
    private final ArrayList<CommandArgument> subArguments = new ArrayList<>();

    public CommandArgument(String arg, boolean isLiteral, boolean hasOptional) {
        this.argNames.add(arg);
        this.isArgLiteral = isLiteral;
        this.hasOptionalSubArg = hasOptional;
    }

    public CommandArgument(List<String> arg, boolean isLiteral, boolean hasOptional) {
        this.argNames.addAll(arg);
        this.isArgLiteral = isLiteral;
        this.hasOptionalSubArg = hasOptional;
    }

    public ArrayList<String> getNames() {
        return argNames;
    }

    // Check if name matches any list entries for argument validation
    public boolean matchesName(String name) {
        for (String testName : argNames) {
            if (testName.equalsIgnoreCase(name)) {
                return true;
            }
        }
        // Found no matching names inside of loop
        return false;
    }

    public boolean isArgLiteral() {
        return isArgLiteral;
    }

    public boolean hasOptionalSubArg() {
        return hasOptionalSubArg;
    }

    // Adds a sub-argument and returns itself for chaining methods
    public CommandArgument sub(CommandArgument subArg) {
        if (subArg != null) {
            if (!subArg.getNames().isEmpty() && !subArguments.contains(subArg)) {
                subArguments.add(subArg);
            }
        }
        return this;
    }

    // Recursive string formatting
    public List<String> getUsageStrings(boolean isOptional) {
        // when sub-arg is optional, and more than 1, only put [] around first
        // for each sub-argument, call this function to return list of usage strings
        List<String> usageStrings = new ArrayList<>();
        // Make this argument's usage string
        StringBuilder localBuilder = new StringBuilder();
        if (isOptional) {
            localBuilder.append("[");
        }
        for(int i = 0; i < argNames.size(); i++) {
            String option = argNames.get(i);
            if(isArgLiteral) {
                // Literal
                localBuilder.append(option);
            } else {
                // Non-literal
                localBuilder.append("<").append(option).append(">");
            }
            if(i != argNames.size()-1) {
                localBuilder.append("|");
            }
        }
        if (isOptional) {
            localBuilder.append("]");
        }
        String localArgUsage = localBuilder.toString();
        // Apply any sub-arguments
        if (subArguments.isEmpty()) {
            // No sub-arguments, just add this one
            usageStrings.add(localArgUsage);
        } else {
            // Get sub-arguments
            List<String> subUsageStrings = new ArrayList<>();
            for (int i = 0; i < subArguments.size(); i++) {
                // Only use optional syntax for first sub-argument
                if (i == 0) {
                    subUsageStrings.addAll(subArguments.get(i).getUsageStrings(hasOptionalSubArg));
                } else {
                    subUsageStrings.addAll(subArguments.get(i).getUsageStrings(false));
                }
            }
            // Apply sub arguments to this argument with space in between
            for (String subArg : subUsageStrings) {
                usageStrings.add(localArgUsage + " " + subArg);
            }
        }
        return usageStrings;
    }

}
