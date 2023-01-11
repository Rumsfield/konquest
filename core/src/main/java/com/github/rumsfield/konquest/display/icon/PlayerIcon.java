package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.Konquest;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class PlayerIcon implements MenuIcon {

	public enum PlayerIconAction {
		DISPLAY_SCORE,
		DISPLAY_INFO,
		GUILD
    }
	
	private final String name;
	private final List<String> lore;
	private final OfflinePlayer player;
	private final int index;
	private final boolean isClickable;
	private final PlayerIconAction action;
	
	public PlayerIcon(String name, List<String> lore, OfflinePlayer player, int index, boolean isClickable, PlayerIconAction action) {
		this.name = name;
		this.lore = lore;
		this.player = player;
		this.index = index;
		this.isClickable = isClickable;
		this.action = action;
	}

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
		assert meta != null;
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
