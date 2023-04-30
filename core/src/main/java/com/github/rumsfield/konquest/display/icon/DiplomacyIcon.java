package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestDiplomacyType;
import com.github.rumsfield.konquest.utility.Labeler;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class DiplomacyIcon implements MenuIcon {

	private final KonquestDiplomacyType relation;
	private final List<String> lore;
	private final int index;
	private final boolean isClickable;
	
	public DiplomacyIcon(KonquestDiplomacyType relation, List<String> lore, int index, boolean isClickable) {
		this.relation = relation;
		this.lore = lore;
		this.index = index;
		this.isClickable = isClickable;
	}
	
	public KonquestDiplomacyType getRelation() {
		return relation;
	}
	
	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return Labeler.lookup(relation);
	}

	@Override
	public ItemStack getItem() {
		ItemStack item = new ItemStack(relation.getIcon(),1);
		ItemMeta meta = item.getItemMeta();
		assert meta != null;
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		List<String> itemLore = new ArrayList<>(lore);
		ChatColor nameColor = ChatColor.GOLD;
		switch(relation) {
			case WAR:
				nameColor = Konquest.enemyColor1;
				break;
			case PEACE:
				nameColor = Konquest.peacefulColor1;
				break;
			case TRADE:
				nameColor = Konquest.tradeColor1;
				break;
			case ALLIANCE:
				nameColor = Konquest.alliedColor1;
				break;
			default:
				break;
		}
		meta.setDisplayName(nameColor+getName());
		meta.setLore(itemLore);
		item.setItemMeta(meta);
		return item;
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}

}
