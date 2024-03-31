package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.command.admin.AdminCommandType;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.Labeler;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public abstract class CommandBase {

    private final ArrayList<CommandArgument> arguments = new ArrayList<>();
    private final String name;
    private final boolean isPlayerOnly;

    public CommandBase(String name, boolean isPlayerOnly) {
        this.name = name;
        this.isPlayerOnly = isPlayerOnly;
    }

    public String getName() {
        return name;
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

    public void addArgument(CommandArgument argument) {
        arguments.add(argument);
    }

    public CommandArgument getArgument(int index) {
        if (index >= 0 && index < arguments.size()) {
            return arguments.get(index);
        } else {
            return null;
        }
    }

    public int getNumArguments() {
        return arguments.size();
    }

    /**
     * Convenience method for checking a command argument string
     * has a valid argument at the specified index.
     * First array element should be sub-command. Ignore it.
     * Index 0 starts at second array element:
     * [sub-command, arg0, arg1, arg2, ...]
     * @param args The argument string array used to execute a command
     * @param index The index of the argument to check
     * @return True when a valid argument exists at the given index, else false.
     */
    public boolean hasValidArg(String[] args, int index) {
        // Check array limits
        if (index < 0 || index > args.length-2) {
            return false;
        }
        // Check command argument exists
        CommandArgument cmdArg = getArgument(index);
        if (cmdArg == null) {
            return false;
        }
        // Check given arg matches expected command argument
        if (!cmdArg.isArgMatch(args[index+1])) {
            return false;
        }
        // Passed all checks
        return true;
    }

    public String getArg(String[] args, int index) {
        if (index >= 0 && index < args.length-1) {
            return args[index+1];
        } else {
            return "";
        }
    }

    public int getLastArgIndex(String[] args) {
        // Ignore first element
        return args.length-2;
    }

    public String getSingleArgumentString(int index) {
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

    public String getFullArgumentString() {
        if (arguments.isEmpty()) {
            return "";
        }
        // Build all formatted arguments
        StringBuilder argBuilder = new StringBuilder();
        for(int i = 0; i < arguments.size(); i++) {
            argBuilder.append(getSingleArgumentString(i));
            if(i != arguments.size()-1) {
                argBuilder.append(" ");
            }
        }
        return argBuilder.toString();
    }

    public String getUsageString() {
        String cmdArgsFormatted = getFullArgumentString()
                .replaceAll("<", ChatColor.GRAY+"<"+ChatColor.AQUA)
                .replaceAll(">", ChatColor.GRAY+">"+ChatColor.AQUA)
                .replaceAll("\\|", ChatColor.GRAY+"|"+ChatColor.AQUA)
                .replaceAll("]", ChatColor.GRAY+"]"+ChatColor.AQUA)
                .replaceAll("\\[", ChatColor.GRAY+"["+ChatColor.AQUA);
        return ChatColor.GOLD+"/k "+name.toLowerCase()+" "+ChatColor.AQUA+cmdArgsFormatted;
    }

    public abstract void execute(Konquest konquest, CommandSender sender, String[] args);
    
    public abstract List<String> tabComplete(Konquest konquest, CommandSender sender, String[] args);

    public void sendInvalidArgMessage(CommandSender sender, boolean isAdmin) {
        if (isAdmin) {
            ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS_ADMIN.getMessage());
        } else {
            ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
        }
        ChatUtil.sendMessage(sender, getUsageString());
    }

    /*
    public KonPlayer getPlayerCheck() {
        if(!(sender instanceof Player)) {
            ChatUtil.printConsoleError(MessagePath.GENERIC_ERROR_NO_PLAYER.getMessage());
            return null;
        }
        Player bukkitPlayer = (Player) sender;
        if(!konquest.getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
            ChatUtil.printDebug("Failed to find non-existent player as command sender");
            ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
            return null;
        }
        return konquest.getPlayerManager().getPlayer(bukkitPlayer);
    }
    */

    public boolean validateSender(CommandSender sender) {
        // Check if sender is implemented for this command
        return !isPlayerOnly || sender instanceof Player;
    }

    /**
     * Check the input args against expected arguments.
     * @param args The argument string array from the command sender.
     * @return Status code:
     *          0 = Passed validation
     *          1+ = 1-indexed argument index that failed validation
     *          -1 = no arguments given
     */
    public int validateArgs(String[] args) {
        // Args is an array that includes everything after /k, delimited by spaces.
        // For example, /k help 1 -> args[] = {help, 1}
        // Make sure the arguments are valid
        // Maximum Arguments, including name
        int maxNumArgs = arguments.size()+1;
        int numOptional = 0;
        for(CommandArgument check : arguments) {
            if(check.isArgOptional()) {
                numOptional++;
            }
        }
        // Minimum Arguments, including name
        int minNumArgs = maxNumArgs - numOptional;
        // Check for empty args
        if(args.length == 0) {
            return -1;
        }
        // Check for min limit,
        if(args.length < minNumArgs) {
            return args.length;
        }
        // Check for max limit
        if(args.length > maxNumArgs) {
            return maxNumArgs-1;
        }
        // Check for matching arguments
        for(int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (i == 0) {
                // First argument must be command name
                if (!arg.equalsIgnoreCase(name)) {
                    return -1;
                }
            } else {
                // Does it match any literals or is there a non-literal option
                CommandArgument argOptions = arguments.get(i-1);
                if(!argOptions.isArgMatch(arg)) {
                    // No matching options
                    return i;
                }
            }
        }
        // Passed all checks
        return 0;
    }


}
