package com.github.rumsfield.konquest.nms;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.FieldAccessException;
import com.github.rumsfield.konquest.utility.ChatUtil;
import net.minecraft.server.v1_16_R3.Entity;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.NBTTagList;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

public class
Handler_1_16_R3 implements VersionHandler {

	public Handler_1_16_R3() {}
	
	@Override
	public void applyTradeDiscount(double discountPercent, boolean isStack, MerchantInventory merchantInventory) {
		
		Villager villager = (Villager)merchantInventory.getHolder();
		Entity targetVillager = ((CraftEntity) villager).getHandle();
		NBTTagCompound tag = targetVillager.save(new NBTTagCompound());
		NBTTagList recipeData = (NBTTagList) tag.getCompound("Offers").get("Recipes");
		
		for (int i = 0; i < recipeData.size(); i++) {
			NBTTagCompound recipeTag = recipeData.getCompound(i);
            int currentDiscount = recipeTag.getInt("specialPrice");
            int currentAmount = recipeTag.getCompound("buy").getInt("Count");
            int applyDiscount = (int)(currentAmount*discountPercent*-1);
            if(isStack) {
            	applyDiscount += currentDiscount;
            }
            recipeTag.setInt("specialPrice", applyDiscount);
            ChatUtil.printDebug("  Applied 1.16.x special price "+applyDiscount);
        }
		targetVillager.load(tag);
	}
	
	@Override
	public boolean sendPlayerTeamPacket(Player player, List<String> teamNames, Team team) {
		// Create team packet
		boolean fieldNameSuccess = false;
		boolean fieldModeSuccess = false;
		boolean fieldPlayersSuccess = false;
		
		try {
			PacketContainer teamPacket = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);

			teamPacket.getStrings().write(0, team.getName());
			fieldNameSuccess = true;

			teamPacket.getIntegers().write(0, 3);
			fieldModeSuccess = true;

			teamPacket.getSpecificModifier(Collection.class).write(0,teamNames);
			fieldPlayersSuccess = true;

			ProtocolLibrary.getProtocolManager().sendServerPacket(player, teamPacket);
			return true;
		} catch(Exception e) {
			ChatUtil.printDebug("Failed to create team packet for player "+player.getName()+", field status is "+fieldNameSuccess+","+fieldModeSuccess+","+fieldPlayersSuccess+": "+e.getMessage());
			return false;
		}
	}
}
