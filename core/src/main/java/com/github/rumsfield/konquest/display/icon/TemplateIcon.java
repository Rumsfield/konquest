package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonMonumentTemplate;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class TemplateIcon implements MenuIcon {

	private final KonMonumentTemplate template;
	private final ChatColor contextColor;
	private final List<String> lore;
	private final int index;
	private final boolean isClickable;

	private final String loreColor = DisplayManager.loreFormat;
	private final String valueColor = DisplayManager.valueFormat;
	private final String alertColor = DisplayManager.alertFormat;

	public TemplateIcon(KonMonumentTemplate template, ChatColor contextColor, List<String> lore, int index, boolean isClickable) {
		this.template = template;
		this.contextColor = contextColor;
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
		if(template != null) {
			if(!template.isValid()) {
				itemLore.add(alertColor + "Invalid");
			} else if(template.isBlanking()) {
				itemLore.add(alertColor + "Temporarily Disabled");
			}
			itemLore.add(loreColor+"Name: "+valueColor+template.getName());
			itemLore.add(loreColor+"Critical Hits: "+valueColor+template.getNumCriticals());
			itemLore.add(loreColor+"Loot Chests: "+valueColor+template.getNumLootChests());
		}
		itemLore.addAll(lore);
		meta.setDisplayName(contextColor+"Monument Template");
		meta.setLore(itemLore);
		item.setItemMeta(meta);
		return item;
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}

}
