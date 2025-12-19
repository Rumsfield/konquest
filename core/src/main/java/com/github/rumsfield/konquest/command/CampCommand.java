package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CampCommand extends CommandBase {

    /*
     * TODO
     *  - Implement place, stowing mechanic
     *  - Configured camp block, critical hits
     *  - Claiming/unclaiming land for camp, spawn setting
     *  - Guest management, permissions
     *  - Camp options, peace
     *  - Camp upgrades, levels, displays
     *  - Info menu details
     *  - Camp menu for management
     */

    /*
     * Planned commands
     * /k camp [menu] - Opens the camp management menu
     * /k camp place - Places a new camp, or the currently stowed camp
     * /k camp move - Stows the current camp into an item for relocation
     * /k camp break - Destroy the owner's camp, if it exists
     * /k camp spawn - Set the camp's spawn point for travel
     * /k camp upgrade list|info|buy [<level>] - Increase the tier of camp with currency
     * /k camp guest add|remove <player> - Add or remove a guest barbarian
     * /k camp option <key> true|false - Change a camp option setting
     *
     * Ideas
     * Camp tier is a level that determines how difficult it is to destroy the camp by enemies.
     * It increases toughness and number of breaks required to fully destroy the camp.
     * Should each tier add more land? Or increase max number of guests?
     * Upgrades:
     *  Increase max guests
     *  Increase critical hits
     *  Add mining fatigue to enemies
     *  Prevent explosions
     * Make a config that defines each level of camp, and what that level has?
     * Like level1 has 3 guests, 2 hits, no fatigue, no tnt protection.
     * Level 5 has 10 guests, 10 hits, fatigue II, TNT protection... ?
     *
     * When a camp is peaceful, it cannot be attacked, and there is no PVP inside its land.
     * Changing peaceful setting has a warmup timer (config) until change occurs
     * Have a core.yml setting that makes all camps immune to attacks, and a setting to enable peaceful camp option.
     *
     * Guests are other barbarians that can 1) travel to the camp, 2) build in its land, and 3) use its chests.
     * Each ability is a config setting.
     *
     * Use info command/menu for details - Displays detailed info about camp: Tier, health, placed/stowed, guests, peace
     *
     * You can only claim/unclaim land for your own camp.
     */

    public CampCommand() {
        // Define name and sender support
        super("camp",true, false);
        // None
        setOptionalArgs(true);
        // menu
        addArgument(
                newArg("menu",true,false)
        );
        // place|move|break|spawn
        List<String> argNames = Arrays.asList("place", "move", "break", "spawn");
        addArgument(
                newArg(argNames,true,false)
        );
        // upgrade list|info|buy [<level>]
        List<String> upgradeArgNames = Arrays.asList("list", "info", "buy");
        addArgument(
                newArg("upgrade",true,false)
                        .sub( newArg(upgradeArgNames,true,true)
                                .sub( newArg("level",false,false) ) )
        );
        // option <key> [true|false]
        List<String> optionsArgNames = Arrays.asList("true", "false");
        addArgument(
                newArg("option",true,false)
                        .sub( newArg("key",false,true)
                                .sub( newArg(optionsArgNames,true,false) ) )
        );
        // guest add|remove <player>
        List<String> guestArgNames = Arrays.asList("add", "remove");
        addArgument(
                newArg("guest",true,false)
                        .sub( newArg(guestArgNames,true,false)
                                .sub( newArg("player",false,false) ) )
        );
    }

    @Override
    public void execute(Konquest konquest, CommandSender sender, List<String> args) {

        // Sender must be player
        KonPlayer player = konquest.getPlayerManager().getPlayer(sender);
        if (player == null) {
            ChatUtil.printDebug("Command executed with null player", true);
            ChatUtil.sendError(sender, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
            return;
        }
        Player bukkitPlayer = player.getBukkitPlayer();
        UUID playerID = bukkitPlayer.getUniqueId();
        // Verify player is a barbarian
        if (!player.isBarbarian()) {
            ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
            return;
        }

        // Parse arguments
        if(args.isEmpty()) {
            // No arguments, open general camp menu
            //TODO make menu
            //konquest.getDisplayManager().displayCampMenu(player);
            return;
        }
        // Has arguments
        String subCmd = args.get(0);

        switch(subCmd.toLowerCase()) {
            case "menu":
                // Open the general menu
                //konquest.getDisplayManager().displayCampMenu(player);
                break;
            case "place":
                // Place a new camp, or the stowed camp
                if (args.size() != 1) {
                    sendInvalidArgMessage(bukkitPlayer);
                    return;
                }
                // Check for camp creation permission
                if (!player.getBukkitPlayer().hasPermission("konquest.create.camp")) {
                    ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
                    return;
                }



                boolean status = campManager.addCampForPlayer(event.getBlock().getLocation(), player);


                break;
            case "move":
                // Stow the existing camp
                if (args.size() != 1) {
                    sendInvalidArgMessage(bukkitPlayer);
                    return;
                }
                //TODO
                break;
            case "break":
                // Destroy the placed camp
                if (args.size() != 1) {
                    sendInvalidArgMessage(bukkitPlayer);
                    return;
                }
                //TODO
                break;
            case "spawn":
                // Change the spawn location of the camp
                if (args.size() != 1) {
                    sendInvalidArgMessage(bukkitPlayer);
                    return;
                }
                //TODO
                break;



            default:
                sendInvalidArgMessage(bukkitPlayer);
                break;
        }
    }

    @Override
    public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
        return List.of();
    }
}
