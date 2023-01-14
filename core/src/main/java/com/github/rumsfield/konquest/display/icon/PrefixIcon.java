package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.model.KonPrefixType;
import com.github.rumsfield.konquest.utility.MessagePath;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class PrefixIcon implements MenuIcon {

	public enum PrefixIconAction {
		APPLY_PREFIX,
		DISABLE_PREFIX
    }
	
	private final PrefixIconAction action;
	private final List<String> lore;
	private final int index;
	private final boolean isClickable;
	private final KonPrefixType prefix;
	private final ItemStack item;
	
	public PrefixIcon(KonPrefixType prefix, List<String> lore, int index, boolean isClickable, PrefixIconAction action) {
		this.prefix = prefix;
		this.lore = lore;
		this.index = index;
		this.isClickable = isClickable;
		this.action = action;
		this.item = initItem();
	}
	
	private ItemStack initItem() {
		ItemStack item = new ItemStack(Material.IRON_BARS);
		ItemMeta meta = item.getItemMeta();
		assert meta != null;
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		ChatColor iconColor = ChatColor.GRAY;
		if(isClickable) {
			item.setType(prefix.category().getMaterial());
			iconColor = ChatColor.DARK_GREEN;
		}
		if(action.equals(PrefixIconAction.DISABLE_PREFIX)) {
			item.setType(Material.MILK_BUCKET);
			meta.setDisplayName(ChatColor.DARK_RED+MessagePath.MENU_PREFIX_DISABLE.getMessage());
		} else {
			meta.setDisplayName(iconColor+prefix.getName());
		}
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}
	
	public PrefixIconAction getAction() {
		return action;
	}
	
	public KonPrefixType getPrefix() {
		return prefix;
	}
	
	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return prefix.getName();
	}

	@Override
	public ItemStack getItem() {
		return item;
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}

}
