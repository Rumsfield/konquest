package com.github.rumsfield.konquest.display.icon;

import com.github.rumsfield.konquest.api.model.KonquestRelationship;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class RelationIcon implements MenuIcon {

	private final KonquestRelationship relation;
	private final List<String> lore;
	private final int index;
	private final boolean isClickable;
	
	public RelationIcon(KonquestRelationship relation, List<String> lore, int index, boolean isClickable) {
		this.relation = relation;
		this.lore = lore;
		this.index = index;
		this.isClickable = isClickable;
	}
	
	public KonquestRelationship getRelation() {
		return relation;
	}
	
	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return relation.getLabel();
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