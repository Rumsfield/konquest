package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonPlayer;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class MenuCommand extends CommandBase {

    public MenuCommand() {
        // Define name and sender support
        super("menu",true, false);
        // None
        setOptionalArgs(true);
        // [dashboard]
        addArgument(
                newArg("dashboard",true,false)
        );
    }

    @Override
    public void execute(Konquest konquest, CommandSender sender, List<String> args) {
        // Sender must be player
        KonPlayer player = konquest.getPlayerManager().getPlayer(sender);
        if (player == null) {
            sendInvalidSenderMessage(sender);
            return;
        }

        if (args.isEmpty()) {
            // Display the main menu to the player
            konquest.getDisplayManager().displayMainMenu(player);
        } else if(args.size() == 1) {
            String subCmd = args.get(0);
            if(subCmd.equalsIgnoreCase("dashboard")) {
                // Display the dashboard view to the player
                konquest.getDisplayManager().displayMainMenuDashboard(player);
            } else {
                sendInvalidArgMessage(sender);
            }
        } else {
            sendInvalidArgMessage(sender);
        }
    }

    @Override
    public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
        List<String> tabList = new ArrayList<>();
        // Give suggestions
        if(args.size() == 1) {
            tabList.add("dashboard");
        }
        return matchLastArgToList(tabList,args);
    }
}
