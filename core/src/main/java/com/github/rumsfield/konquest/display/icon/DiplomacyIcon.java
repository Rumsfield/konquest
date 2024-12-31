package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestDiplomacyType;
import com.github.rumsfield.konquest.utility.CompatibilityUtil;
import com.github.rumsfield.konquest.utility.Labeler;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

public class DiplomacyIcon extends MenuIcon {

	private final KonquestDiplomacyType relation;
	private final boolean isClickable;
	
	public DiplomacyIcon(KonquestDiplomacyType relation, int index, boolean isClickable) {
		super(index);
		this.relation = relation;
		this.isClickable = isClickable;
	}
	
	public KonquestDiplomacyType getRelation() {
		return relation;
	}

	@Override
	public String getName() {
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
		return nameColor+Labeler.lookup(relation);
	}

	@Override
	public ItemStack getItem() {
		return CompatibilityUtil.buildItem(relation.getIcon(), getName(), getLore());
	}

	@Override
	public boolean isClickable() {
		return isClickable;
	}

}
