package com.github.rumsfield.konquest.command;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.manager.TerritoryManager;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MapCommand extends CommandBase {

	private int mapSize = TerritoryManager.DEFAULT_MAP_SIZE;
	
	public MapCommand() {
		// Define name and sender support
		super("map",true, false);
		// None
		setOptionalArgs(true);
		// [far|auto]
		List<String> argNames = Arrays.asList("far", "auto");
		addArgument(
				newArg(argNames,true,false)
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
		// Display formatted text in chat as chunk map
		// 10 lines of text by default
		if(args.size() == 1) {
			String subCmd = args.get(0);
			if(subCmd.equalsIgnoreCase("far") || subCmd.equalsIgnoreCase("f")) {
				mapSize = TerritoryManager.FAR_MAP_SIZE;
			} else if(subCmd.equalsIgnoreCase("auto") || subCmd.equalsIgnoreCase("a")) {
				if(player.isMapAuto()) {
					player.setIsMapAuto(false);
					ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_DISABLE_AUTO.getMessage());
					return;
				} else {
					player.setIsMapAuto(true);
					ChatUtil.sendNotice(sender, MessagePath.GENERIC_NOTICE_ENABLE_AUTO.getMessage());
				}
			} else {
				sendInvalidArgMessage(sender);
				return;
			}
		}
		// Display map in chat
		Bukkit.getScheduler().runTaskAsynchronously(konquest.getPlugin(),
				() -> konquest.getTerritoryManager().printPlayerMap(player, mapSize));
	}
	
	@Override
	public List<String> tabComplete(Konquest konquest, CommandSender sender, List<String> args) {
		List<String> tabList = new ArrayList<>();
		// Give suggestions
		if(args.size() == 1) {
			tabList.add("far");
			tabList.add("auto");
		}
		return matchLastArgToList(tabList,args);
	}
}
