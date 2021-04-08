package konquest.display;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;


public class InfoIcon extends MenuIcon{
	
	private List<String> lore;
	private Material mat;
	private String info;
	
	public InfoIcon(String name, List<String> lore, Material mat, int index) {
		super(name, index);
		this.lore = lore;
		this.mat = mat;
		this.info = "";
		setItem(initItem());
	}

	@Override
	public ItemStack initItem() {
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
	
}
