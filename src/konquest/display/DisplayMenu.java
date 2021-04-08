package konquest.display;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;

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
		if(icon.getItem() == null) {
			ChatUtil.printDebug("Added null item to slot "+icon.getIndex()+" from icon "+icon.getName());
		}
		inventory.setItem(icon.getIndex(), icon.getItem());
		iconMap.put(icon.getIndex(), icon);
	}
	
	public MenuIcon getIcon(int index) {
		return iconMap.get(index);
	}
	
}
