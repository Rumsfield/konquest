package konquest.display;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import konquest.utility.ChatUtil;

public class DisplayMenu {

	private Inventory inventory;
	private HashMap<Integer,MenuIcon> iconMap;
	
	public DisplayMenu (int rows, String label) {
		inventory = Bukkit.getServer().createInventory(null, rows*9, label);
		iconMap = new HashMap<Integer,MenuIcon>();
	}
	
	public Inventory getInventory() {
        return inventory;
    }
	
	public void addIcon(MenuIcon icon) {
		ItemStack iconItem = icon.getItem();
		if(iconItem == null) {
			ChatUtil.printDebug("Added null item to slot "+icon.getIndex()+" from icon "+icon.getName());
		}
		inventory.setItem(icon.getIndex(), iconItem);
		iconMap.put(icon.getIndex(), icon);
	}
	
	public MenuIcon getIcon(int index) {
		return iconMap.get(index);
	}
	
	public void updateIcons() {
		for(MenuIcon icon : iconMap.values()) {
			inventory.setItem(icon.getIndex(), icon.getItem());
		}
	}
	
}
