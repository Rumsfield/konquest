package konquest.display;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
//import org.bukkit.inventory.ItemStack;

import konquest.Konquest;
//import konquest.utility.ChatUtil;
import konquest.display.icon.MenuIcon;
import konquest.display.icon.PlayerIcon;

public class DisplayMenu {

	private Inventory inventory;
	private HashMap<Integer,MenuIcon> iconMap;
	
	public DisplayMenu (int rows, String label) {
		inventory = Bukkit.createInventory(null, rows*9, label);
		iconMap = new HashMap<Integer,MenuIcon>();
	}
	
	public Inventory getInventory() {
        return inventory;
    }
	
	public void addIcon(MenuIcon icon) {
		/*
		ItemStack iconItem = icon.getItem();
		if(iconItem == null) {
			ChatUtil.printDebug("Added null item to slot "+icon.getIndex()+" from icon "+icon.getName());
		}
		if(icon instanceof PlayerIcon) {
			putItemAsync(inventory, icon.getIndex(), iconItem);
		} else {
			inventory.setItem(icon.getIndex(), iconItem);
		}
		*/
		iconMap.put(icon.getIndex(), icon);
	}
	
	public MenuIcon getIcon(int index) {
		return iconMap.get(index);
	}
	
	public void updateIcons() {
		Bukkit.getScheduler().runTaskAsynchronously(Konquest.getInstance().getPlugin(), new Runnable() {
            @Override
            public void run() {
            	// Set non-player heads first
            	for(MenuIcon icon : iconMap.values()) {
            		if(!(icon instanceof PlayerIcon)) {
            			inventory.setItem(icon.getIndex(), icon.getItem());
            		}
        		}
            	// Set player heads last
            	for(MenuIcon icon : iconMap.values()) {
            		if((icon instanceof PlayerIcon)) {
            			inventory.setItem(icon.getIndex(), icon.getItem());
            		}
        		}
            }
        });
	}
	
	/*
	public void updateIcons() {
		for(MenuIcon icon : iconMap.values()) {
			if(icon instanceof PlayerIcon) {
				putItemAsync(inventory, icon.getIndex(), icon.getItem());
			} else {
				inventory.setItem(icon.getIndex(), icon.getItem());
			}
		}
	}
	
	private void putItemAsync(Inventory inv, int slot, ItemStack item) {
		Bukkit.getScheduler().runTaskAsynchronously(Konquest.getInstance().getPlugin(), new Runnable() {
            @Override
            public void run() {
            	inv.setItem(slot, item);
            }
        });
	}
	*/
}
