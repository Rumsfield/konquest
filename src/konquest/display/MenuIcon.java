package konquest.display;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public abstract class MenuIcon {

	private ItemStack item;
	private String name;
	private int index;
	
	public MenuIcon(String name, int index) {
		this.name = name;
		this.index = index;
		this.item =  new ItemStack(Material.DIRT, 1);
	}
	
	public String getName() {
		return name;
	}
	
	public int getIndex() {
		return index;
	}
	
	public ItemStack getItem() {
		return item;
	}
	
	public void setItem(ItemStack item) {
		this.item = item;
	}
	
	public abstract ItemStack initItem();
	
}
