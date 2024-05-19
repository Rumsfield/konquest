package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.admin.AdminCommandType;
import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.Labeler;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.*;

public abstract class CommandBase {

    private final String cmdName;
    private final boolean isPlayerOnly;
    private final boolean isAdmin;
    private final ArrayList<CommandArgument> arguments; // first level of argument options
    private boolean hasOptionalArgs; // Can this command accept no arguments?

    public CommandBase(String name, boolean isPlayerOnly, boolean isAdmin) {
        this.cmdName = name;
        this.isPlayerOnly = isPlayerOnly;
        this.isAdmin = isAdmin;
        this.arguments = new ArrayList<>();
        this.hasOptionalArgs = false;
    }

    public String getName() {
        return cmdName;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public boolean hasOptionalArgs() {
        return hasOptionalArgs;
    }

    public void setOptionalArgs(boolean val) {
        hasOptionalArgs = val;
    }

    public boolean isSenderAllowed(CommandSender sender) {
        // Sender is allowed when command is player only and sender is player,
        // Otherwise if command is not player only, then sender can be player or console.
        if (isPlayerOnly) {
            return sender instanceof Player;
        } else {
            return (sender instanceof Player) || (sender instanceof ConsoleCommandSender);
        }
    }

    // Convenience method for making new arguments
    public CommandArgument newArg(String arg, boolean isLiteral, boolean hasOptional) {
        return new CommandArgument(arg, isLiteral, hasOptional);
    }
    public CommandArgument newArg(List<String> arg, boolean isLiteral, boolean hasOptional) {
        return new CommandArgument(arg, isLiteral, hasOptional);
    }

    public void addArgument(CommandArgument argument) {
        arguments.add(argument);
    }

    private String getBase() {
        if (isAdmin) {
            return "/k admin ";
        } else {
            return "/k ";
        }
    }

    public String getBaseUsage() {
        return ChatColor.GOLD+getBase()+cmdName.toLowerCase();
    }

    public List<String> getArgumentUsage() {
        List<String> usageStrings = new ArrayList<>();
        if (hasOptionalArgs || arguments.isEmpty()) {
            usageStrings.add(getBaseUsage());
        }
        for (CommandArgument cmdArg : arguments) {
            for (String argUsage : cmdArg.getUsageStrings(hasOptionalArgs)) {
                usageStrings.add(getBaseUsage()+" "+ChatColor.AQUA+formatUsageString(argUsage));
            }
        }
        return usageStrings;
    }

    private String formatUsageString(String inArgs) {
        return inArgs
                .replaceAll("<", ChatColor.GRAY+"<"+ChatColor.AQUA)
                .replaceAll(">", ChatColor.GRAY+">"+ChatColor.AQUA)
                .replaceAll("\\|", ChatColor.GRAY+"|"+ChatColor.AQUA)
                .replaceAll("]", ChatColor.GRAY+"]"+ChatColor.AQUA)
                .replaceAll("\\[", ChatColor.GRAY+"["+ChatColor.AQUA);
    }

    public abstract void execute(Konquest konquest, CommandSender sender, List<String> args);
    
    public abstract List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args);

    public void sendInvalidArgMessage(CommandSender sender) {
        if (isAdmin) {
            ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS_ADMIN.getMessage());
        } else {
            ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
        }
        //ChatUtil.sendMessage(sender, getUsageString());
    }

    public void sendInvalidSenderMessage(CommandSender sender) {
        ChatUtil.printDebug("Command executed with null player", true);
        ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    }

    public List<String> matchLastArgToList(List<String> completions, List<String> args) {
        List<String> matchedCompletions = new ArrayList<>();
        // Trim down completion options based on current input
        if (!args.isEmpty()) {
            StringUtil.copyPartialMatches(args.get(args.size()-1), completions, matchedCompletions);
            Collections.sort(matchedCompletions);
        }
        return matchedCompletions;
    }

    public String formatStringListLimited(List<String> entries, int limit) {
        StringBuilder entryListBuilder = new StringBuilder();
        String nameColor = ""+ChatColor.GOLD;
        String sepColor = ""+ChatColor.GRAY;
        for (int i = 0; i < entries.size(); i++) {
            if (i < limit) {
                // Add name to list
                entryListBuilder.append(nameColor).append(entries.get(i));
                if (i != entries.size()-1) {
                    entryListBuilder.append(sepColor).append(", ");
                }
            } else {
                // Stop listing, show remaining count
                int numRemaining = entries.size() - limit;
                entryListBuilder.append(sepColor).append("... (").append(numRemaining).append(")");
                break;
            }
        }
        return entryListBuilder.toString();
    }

    public boolean validateSender(CommandSender sender) {
        // Check if sender is implemented for this command
        return !isPlayerOnly || sender instanceof Player;
    }

    /**
     * Check the input args against expected arguments.
     * @param args The argument string list from the command sender.
     * @return Tru when args match expected pattern, else false
     */
    /*
    public boolean validateArgs(List<String> args) {
        // Args is a list that includes all arguments after /k <command>
        // For example, /k kingdom create MyTemplate MyKingdom -> args = {create, MyTemplate, MyKingdom}

        if (args.isEmpty() && !hasOptionalArg) {
            // No arguments were given, but this command does not accept optionals
            return false;
        }

        // Check each given argument, level by level
        ArrayList<CommandArgument> testArguments = arguments;
        ListIterator<String> argIter = args.listIterator();
        while (argIter.hasNext()) {
            String checkArg = argIter.next();
            boolean foundNonLiteral = false;
            for (CommandArgument cmdArg : testArguments) {
                if (cmdArg.isArgLiteral() && cmdArg.matchesName(checkArg)) {
                    // Matched the arg to a literal

                } else if (!cmdArg.isArgLiteral()) {
                    foundNonLiteral = true;
                }
            }
            // No literal matches, check for non-literals
            if (foundNonLiteral) {

            }
        }

        // Passed all checks
        return true;
    }
     */

}
