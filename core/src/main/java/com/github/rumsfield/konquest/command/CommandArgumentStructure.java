package com.github.rumsfield.konquest.command;

import java.util.ArrayList;

public class CommandArgumentStructure {

    private final String argumentString;
    private final ArrayList<CommandArgument> arguments;

    // Derived
    private int minNumArgs;
    private int maxNumArgs;

    /**
     * Specify arguments in a string that uses the following syntax:
     * - Each argument is separated by spaces
     * - [] = argument is optional
     * - <> = argument is a placeholder for user-given input
     * - | = argument OR another
     * For example: "[kingdom|town|ruin|sanctuary] [<page>]"
     * @param argumentDefinition The argument definition string that uses the above syntax.
     */
    public CommandArgumentStructure(String argumentDefinition) {
        this.argumentString = argumentDefinition;
        this.arguments = new ArrayList<>();
        this.minNumArgs = 0;
        this.maxNumArgs = 0;
        initArguments(argumentDefinition);
        deriveArguments();
    }

    private void initArguments(String argumentDefinition) {
        // Find arguments delimited by space
        String[] argStrings = argumentDefinition.split("\\s+");
        for(String arg : argStrings) {
            // Check for optional [] markers
            boolean isOptional = false;
            if(arg.matches("\\[.+\\]")) {
                isOptional = true;
                arg = arg.replace("[","");
                arg = arg.replace("]","");
            }
            // Find options delimited by |
            CommandArgument argOptions = new CommandArgument();
            String[] optionStrings = arg.split("\\|");
            for(String option : optionStrings) {
                // Check for literal <> markers
                boolean isLiteral = false;
                if(option.matches("\\<.+\\>")) {
                    isLiteral = true;
                    option = option.replace("<","");
                    option = option.replace(">","");
                }
                // Create argument options structure
                argOptions.addArgOption(option,isLiteral);
            }
            // Create argument list structure
            argOptions.setIsArgOptional(isOptional);
            arguments.add(argOptions);
        }
    }

    private void deriveArguments() {
        // Maximum Arguments
        this.maxNumArgs = arguments.size();
        int numOptional = 0;
        for(CommandArgument check : arguments) {
            if(check.isArgOptional()) {
                numOptional++;
            }
        }
        // Minimum Arguments
        this.minNumArgs = arguments.size() - numOptional;
    }

    public String getArgumentString() {
        return argumentString;
    }

    public int getMinNumArgs() {
        return minNumArgs;
    }

    public int getMaxNumArgs() {
        return maxNumArgs;
    }

    /**
     * Check the input args against expected arguments.
     * @param args The argument string array from the command sender.
     * @return Status code:
     *          0 = Passed validation
     *          1+ = 1-indexed argument index that failed validation
     */
    public int validateArgs(String[] args) {
        // Check for literal matches and min/max arg limits
        if(args.length < minNumArgs || args.length > maxNumArgs) {
            return args.length;
        }
        int argIndex = 0;
        for(String arg : args) {
            // Does it match any literals or is there a non-literal option
            if(argIndex >= arguments.size()) {
                // Too many arguments
                return argIndex+1;
            }
            CommandArgument argOptions = arguments.get(argIndex);
            if(!argOptions.isArgMatch(arg)) {
                // No matching options
                return argIndex+1;
            }
            argIndex++;
        }
        // Passed all checks
        return 0;
    }

    public String getArgument(int index) {
        if(index < 0 || index >= arguments.size()) {
            return "";
        }
        CommandArgument argOptions = arguments.get(index);
        StringBuilder argBuilder = new StringBuilder();
        if(argOptions.isArgOptional()) {
            argBuilder.append("[");
        }
        for(int i = 0; i < argOptions.getNumOptions(); i++) {
            String option = argOptions.getArgOption(i);
            boolean isLiteral = argOptions.isArgOptionLiteral(i);
            if(isLiteral) {
                // Literal
                argBuilder.append("<").append(option).append(">");
            } else {
                // Non-literal
                argBuilder.append(option);
            }
            if(i != argOptions.getNumOptions()-1) {
                argBuilder.append("|");
            }
        }
        if(argOptions.isArgOptional()) {
            argBuilder.append("]");
        }
        return argBuilder.toString();
    }

}
