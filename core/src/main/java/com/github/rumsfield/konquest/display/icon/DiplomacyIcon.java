package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestDiplomacyType;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.Labeler;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DiplomacyIcon extends MenuIcon {

	private final KonquestDiplomacyType relation;
	private final List<String> lore;
	private final boolean isClickable;
	
	public DiplomacyIcon(KonquestDiplomacyType relation, List<String> lore, int index, boolean isClickable) {
		super(index);
		this.relation = relation;
		this.lore = lore;
		this.isClickable = isClickable;
	}
	
	public KonquestDiplomacyType getRelation() {
		return relation;
	}

	@Override
	public String getName() {
		return Labeler.lookup(relation);
	}

	@Override
	public ItemStack getItem() {
		List<String> itemLore = new ArrayList<>(lore);
		String nameColor = ""+ChatColor.GOLD;
		switch(relation) {
			case WAR:
				nameColor = Konquest.enemyColor2;
				break;
			case PEACE:
				nameColor = Konquest.peacefulColor2;
				break;
			case TRADE:
				nameColor = Konquest.tradeColor2;
				break;
			case ALLIANCE:
				nameColor = Konquest.alliedColor2;
				break;
			default:
				break;
		}
		String name = nameColor+getName();
		return CompatibilityUtil.buildItem(relation.getIcon(), name, itemLore);
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}

}
