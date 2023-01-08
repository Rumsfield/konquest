package konquest.display.icon;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import konquest.api.model.KonquestRelationship;

public class RelationIcon implements MenuIcon {

	private KonquestRelationship relation;
	private List<String> lore;
	private int index;
	private boolean isClickable;
	
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
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		List<String> itemLore = new ArrayList<String>();
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
