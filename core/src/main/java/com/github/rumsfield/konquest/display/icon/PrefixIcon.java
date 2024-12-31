package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.model.KonPrefixType;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class PrefixIcon extends MenuIcon {

	public enum PrefixIconAction {
		APPLY_PREFIX,
		DISABLE_PREFIX
    }
	
	private final PrefixIconAction action;
	private final boolean isClickable;
	private final KonPrefixType prefix;
	
	public PrefixIcon(KonPrefixType prefix, int index, boolean isClickable, PrefixIconAction action) {
		super(index);
		this.prefix = prefix;
		this.isClickable = isClickable;
		this.action = action;
	}
	
	public PrefixIconAction getAction() {
		return action;
	}
	
	public KonPrefixType getPrefix() {
		return prefix;
	}

	@Override
	public String getName() {
		String name;
		if(action.equals(PrefixIconAction.DISABLE_PREFIX)) {
			name = ChatColor.DARK_RED+MessagePath.MENU_PREFIX_DISABLE.getMessage();
		} else {
			ChatColor nameColor = isClickable ? ChatColor.DARK_GREEN : ChatColor.GRAY;
			name = nameColor+prefix.getName();
		}
		return name;
	}

	@Override
	public ItemStack getItem() {
		Material material;
		if(action.equals(PrefixIconAction.DISABLE_PREFIX)) {
			material = Material.MILK_BUCKET;
		} else {
			material = isClickable ? prefix.category().getMaterial() : Material.IRON_BARS;
		}
		return CompatibilityUtil.buildItem(material, getName(), getLore());
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}

}
