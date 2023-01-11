package com.github.rumsfield.konquest.display.icon;

import java.util.List;

//import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
//import org.bukkit.inventory.meta.SkullMeta;

import com.github.rumsfield.konquest.Konquest;

public class PlayerIcon implements MenuIcon {

	public enum PlayerIconAction {
		DISPLAY_SCORE,
		DISPLAY_INFO,
		GUILD;
	}
	
	private String name;
	private List<String> lore;
	private OfflinePlayer player;
	private int index;
	private boolean isClickable;
	private PlayerIconAction action;
	
	public PlayerIcon(String name, List<String> lore, OfflinePlayer player, int index, boolean isClickable, PlayerIconAction action) {
		this.name = name;
		this.lore = lore;
		this.player = player;
		this.index = index;
		this.isClickable = isClickable;
		this.action = action;
	}

	/*private ItemStack initItem() {
		ItemStack item = Konquest.getInstance().getPlayerHead(player);
		ItemMeta meta = item.getItemMeta();
		//ItemStack item = new ItemStack(Material.PLAYER_HEAD);
		//SkullMeta meta = (SkullMeta)item.getItemMeta();
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		meta.setDisplayName(getName());
		meta.setLore(lore);
		//meta.setOwningPlayer(player);
		item.setItemMeta(meta);
		return item;
	}*/
	
	public PlayerIconAction getAction() {
		return action;
	}
	
	public OfflinePlayer getOfflinePlayer() {
		return player;
	}

	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ItemStack getItem() {
		ItemStack item = Konquest.getInstance().getPlayerHead(player);
		ItemMeta meta = item.getItemMeta();
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		meta.setDisplayName(getName());
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}
	
	@Override
	public boolean isClickable() {
		return isClickable;
	}
}
