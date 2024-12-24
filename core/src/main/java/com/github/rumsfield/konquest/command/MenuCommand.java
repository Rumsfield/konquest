package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonPlayer;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class MenuCommand extends CommandBase {

    public MenuCommand() {
        // Define name and sender support
        super("menu",true, false);
        // No Arguments
    }

    @Override
    public void execute(Konquest konquest, CommandSender sender, List<String> args) {
        // Sender must be player
        KonPlayer player = konquest.getPlayerManager().getPlayer(sender);
        if (player == null) {
            sendInvalidSenderMessage(sender);
            return;
        }
        // Check for no arguments
        if (!args.isEmpty()) {
            sendInvalidArgMessage(sender);
            return;
        }
        // Display the main menu to the player
        konquest.getDisplayManager().displayMainMenu(player);
    }

    @Override
    public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
        // No arguments to complete
        return Collections.emptyList();
    }
}
