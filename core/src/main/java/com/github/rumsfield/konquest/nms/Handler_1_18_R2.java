package com.github.rumsfield.konquest.nms;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.utility.ChatUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Handler_1_18_R2 implements VersionHandler {

	public Handler_1_18_R2() {}
	
	@Override
	public void applyTradeDiscount(double discountPercent, boolean isStack, MerchantInventory merchantInventory) {
		// Get and set special price with API methods
		int amount;
		int discount;
		Merchant tradeHost = merchantInventory.getMerchant();
		List<MerchantRecipe> tradeListDiscounted = new ArrayList<>();
		for(MerchantRecipe trade : tradeHost.getRecipes()) {
			List<ItemStack> ingredientList = trade.getIngredients();
			if(!ingredientList.isEmpty()) {
				amount = ingredientList.get(0).getAmount();
				discount = (int)(amount*discountPercent*-1);
				if(isStack) {
					discount += trade.getSpecialPrice();
				}
				trade.setSpecialPrice(discount);
				ChatUtil.printDebug("  Applied 1.18.x special price "+discount);
			}
			tradeListDiscounted.add(trade);
		}
		tradeHost.setRecipes(tradeListDiscounted);
	}
	
	@Override
	public void sendPlayerTeamPacket(Player player, List<String> teamNames, Team team) {
		// Create team packet
		boolean fieldNameSuccess = false;
		boolean fieldModeSuccess = false;
		boolean fieldPlayersSuccess = false;
		
		PacketContainer teamPacket = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
		try {
			
			teamPacket.getStrings().write(0, team.getName());
			fieldNameSuccess = true;

			teamPacket.getIntegers().write(0, 3);
			fieldModeSuccess = true;

			teamPacket.getSpecificModifier(Collection.class).write(0,teamNames);
			fieldPlayersSuccess = true;
			
			try {
			    KonquestPlugin.getProtocolManager().sendServerPacket(player, teamPacket);
			} catch (InvocationTargetException e) {
			    throw new RuntimeException(
			        "Cannot send packet " + teamPacket, e);
			}
			
		} catch(FieldAccessException e) {
			ChatUtil.printDebug("Failed to create team packet for player "+player.getName()+", field status is "+fieldNameSuccess+","+fieldModeSuccess+","+fieldPlayersSuccess+": "+e.getMessage());
		}
	}
}
