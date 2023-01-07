package konquest.display.icon;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class InfoIcon implements MenuIcon{
	
	private String name;
	private List<String> lore;
	private Material mat;
	private String info;
	private int index;
	private ItemStack item;
	private boolean isClickable;
	
	public InfoIcon(String name, List<String> lore, Material mat, int index, boolean isClickable) {
		this.name = name;
		this.lore = lore;
		this.mat = mat;
		this.info = "";
		this.index = index;
		this.isClickable = isClickable;
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
	
	public void setInfo(String info) {
		this.info = info;
	}
	
	public String getInfo() {
		return info;
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
		return isClickable;
	}
	
}