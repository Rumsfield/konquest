package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class KingdomIcon implements MenuIcon {

	private KonKingdom kingdom;
	private ChatColor contextColor;
	private List<String> lore;
	private int index;
	private ItemStack item;
	
	public KingdomIcon(KonKingdom kingdom, ChatColor contextColor, List<String> lore, int index, boolean isClickable) {
		this.kingdom = kingdom;
		this.contextColor = contextColor;
		this.lore = lore;
		this.index = index;
		this.item = initItem();
	}
	
	private ItemStack initItem() {
		ItemStack item = new ItemStack(Material.GOLDEN_HELMET,1);
		ItemMeta meta = item.getItemMeta();
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		meta.setDisplayName(contextColor+kingdom.getName());
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}
	
	public KonKingdom getKingdom() {
		return kingdom;
	}
	
	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		String name = kingdom.getName();
		if(name.equalsIgnoreCase("barbarians")) {
			name = MessagePath.LABEL_BARBARIANS.getMessage();
		}
		return name;
	}

	@Override
	public ItemStack getItem() {
		return item;
	}

	@Override
	public boolean isClickable() {
		return true;
	}

}
