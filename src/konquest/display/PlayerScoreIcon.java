package konquest.display;

import java.util.List;

//import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
//import org.bukkit.inventory.meta.SkullMeta;

import konquest.Konquest;

public class PlayerScoreIcon implements MenuIcon {

	private String name;
	private List<String> lore;
	private OfflinePlayer player;
	private int index;
	//private ItemStack item;
	
	public PlayerScoreIcon(String name, List<String> lore, OfflinePlayer player, int index) {
		this.name = name;
		this.lore = lore;
		this.player = player;
		this.index = index;
		//this.item = initItem();
	}

	/*private ItemStack initItem() {
		ItemStack item = Konquest.getInstance().getPlayerHead(player);
		ItemMeta meta = item.getItemMeta();
		//ItemStack item = new ItemStack(Material.PLAYER_HEAD);
		//SkullMeta meta = (SkullMeta)item.getItemMeta();
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		meta.setDisplayName(getName());
		meta.setLore(lore);
		//meta.setOwningPlayer(player);
		item.setItemMeta(meta);
		return item;
	}*/
	
	public OfflinePlayer getOfflinePlayer() {
		return player;
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
		ItemStack item = Konquest.getInstance().getPlayerHead(player);
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
	
	@Override
	public boolean isClickable() {
		return true;
	}
}
