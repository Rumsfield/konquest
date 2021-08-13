package konquest.display;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class OptionIcon implements MenuIcon {

	public enum optionAction {
		TOWN_OPEN,
		TOWN_REDSTONE;
	}
	
	private optionAction action;
	private String name;
	private List<String> lore;
	private Material mat;
	private int index;
	private ItemStack item;
	
	public OptionIcon(optionAction action, String name, List<String> lore, Material mat, int index) {
		this.action = action;
		this.name = name;
		this.lore = lore;
		this.mat = mat;
		this.index = index;
		this.item = initItem();
	}
	
	private ItemStack initItem() {
		ItemStack item = new ItemStack(mat);
		ItemMeta meta = item.getItemMeta();
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		meta.setDisplayName(getName());
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}
	
	public optionAction getAction() {
		return action;
	}
	
	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ItemStack getItem() {
		return item;
	}

	@Override
	public boolean isClickable() {
		return true;
	}

}
