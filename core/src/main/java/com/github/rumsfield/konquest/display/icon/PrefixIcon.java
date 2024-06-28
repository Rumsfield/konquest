package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.model.KonPrefixType;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
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
		Material material = Material.IRON_BARS;
		ChatColor iconColor = ChatColor.GRAY;
		String name;
		if(isClickable) {
			material = prefix.category().getMaterial();
			iconColor = ChatColor.DARK_GREEN;
		}
		if(action.equals(PrefixIconAction.DISABLE_PREFIX)) {
			material = Material.MILK_BUCKET;
			name = ChatColor.DARK_RED+MessagePath.MENU_PREFIX_DISABLE.getMessage();
		} else {
			name = iconColor+prefix.getName();
		}
		return CompatibilityUtil.buildItem(material, name, lore);
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
