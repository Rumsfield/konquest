package konquest.display;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import konquest.model.KonKingdom;
import konquest.utility.MessagePath;

public class KingdomIcon implements MenuIcon {

	private KonKingdom kingdom;
	private String contextColor;
	private Material material;
	private List<String> lore;
	private int index;
	private ItemStack item;
	
	public KingdomIcon(KonKingdom kingdom, String contextColor, Material material, List<String> lore, int index) {
		this.kingdom = kingdom;
		this.contextColor = contextColor;
		this.material = material;
		this.lore = lore;
		this.index = index;
		this.item = initItem();
	}
	
	private ItemStack initItem() {
		ItemStack item = new ItemStack(material,1);
		ItemMeta meta = item.getItemMeta();
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		meta.setDisplayName(contextColor+kingdom.getName());
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}
	
	public KonKingdom getKingdom() {
		return kingdom;
	}
	
	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		String name = kingdom.getName();
		if(name.equalsIgnoreCase("barbarians")) {
			name = MessagePath.LABEL_BARBARIANS.getMessage();
		}
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
