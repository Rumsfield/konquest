package com.github.rumsfield.konquest.nms;

import org.bukkit.entity.Player;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.scoreboard.Team;

import java.util.List;

public interface VersionHandler {

	void applyTradeDiscount(double discountPercent, boolean isStack, MerchantInventory merchantInventory);
	
	void sendPlayerTeamPacket(Player player, List<String> teamNames, Team team);
}
