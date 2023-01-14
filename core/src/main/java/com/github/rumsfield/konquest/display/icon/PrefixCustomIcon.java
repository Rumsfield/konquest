package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.model.KonCustomPrefix;
import com.github.rumsfield.konquest.utility.ChatUtil;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class PrefixCustomIcon implements MenuIcon {

	private final List<String> lore;
	private final int index;
	private final boolean isClickable;
	private final KonCustomPrefix prefix;
	private final ItemStack item;
	
	public PrefixCustomIcon(KonCustomPrefix prefix, List<String> lore, int index, boolean isClickable) {
		this.prefix = prefix;
		this.lore = lore;
		this.index = index;
		this.isClickable = isClickable;
		this.item = initItem();
	}
	
	private ItemStack initItem() {
		ItemStack item = new ItemStack(Material.IRON_BARS);
		ItemMeta meta = item.getItemMeta();
		assert meta != null;
		if(isClickable) {
			meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
		}
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		if(isClickable) {
			item.setType(Material.GOLD_BLOCK);
		}
		String displayName = ChatUtil.parseHex(prefix.getName());
		meta.setDisplayName(displayName);
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}
	
	public KonCustomPrefix getPrefix() {
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
