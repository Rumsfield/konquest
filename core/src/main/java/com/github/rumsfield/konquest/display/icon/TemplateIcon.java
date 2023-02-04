package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.model.KonMonumentTemplate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TemplateIcon implements MenuIcon {

	private final KonMonumentTemplate template;
	private final List<String> lore;
	private final int index;
	private final boolean isClickable;
	
	public TemplateIcon(KonMonumentTemplate template, List<String> lore, int index, boolean isClickable) {
		this.template = template;
		this.lore = lore;
		this.index = index;
		this.isClickable = isClickable;
	}
	
	public KonMonumentTemplate getTemplate() {
		return template;
	}
	
	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		String result = "";
		if(template != null) {
			result = template.getName();
		}
		return result;
	}

	@Override
	public ItemStack getItem() {
		ItemStack item = new ItemStack(Material.CRAFTING_TABLE,1);
		ItemMeta meta = item.getItemMeta();
		assert meta != null;
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		List<String> itemLore = new ArrayList<>();
		//TODO: Replace with message paths
		itemLore.add(ChatColor.YELLOW+"Total Blocks: "+ChatColor.AQUA+template.getNumBlocks());
		itemLore.add(ChatColor.YELLOW+"Critical Blocks: "+ChatColor.AQUA+template.getNumCriticals());
		itemLore.add(ChatColor.YELLOW+"Loot Chests: "+ChatColor.AQUA+template.getNumLootChests());
		itemLore.addAll(lore);
		meta.setDisplayName(getName());
		meta.setLore(itemLore);
		item.setItemMeta(meta);
		return item;
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}

}
