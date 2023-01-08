package konquest.display.icon;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import konquest.model.KonTown;
import konquest.utility.MessagePath;

public class TownIcon implements MenuIcon {

	private KonTown town;
	private ChatColor contextColor;
	private Material material;
	private List<String> lore;
	private int index;
	private ItemStack item;
	
	public TownIcon(KonTown town, ChatColor contextColor, Material material, List<String> lore, int index) {
		this.town = town;
		this.contextColor = contextColor;
		this.material = material;
		this.lore = lore;
		this.index = index;
		this.item = initItem();
	}
	
	private ItemStack initItem() {
		// Determine material override
		if(town.isAttacked()) {
			material = Material.RED_WOOL;
		} else if(town.isArmored()) {
			material = Material.STONE_BRICKS;
			
		}
		ItemStack item = new ItemStack(material,1);
		ItemMeta meta = item.getItemMeta();
		// Add applicable labels
		if(town.isAttacked()) {
			lore.add(ChatColor.DARK_RED+""+ChatColor.ITALIC+MessagePath.PROTECTION_NOTICE_ATTACKED.getMessage());
		}
		if(town.isOpen()) {
			lore.add(ChatColor.LIGHT_PURPLE+""+ChatColor.ITALIC+MessagePath.LABEL_OPEN.getMessage());
		}
		if(town.isArmored()) {
			lore.add(ChatColor.LIGHT_PURPLE+""+ChatColor.ITALIC+MessagePath.LABEL_ARMOR.getMessage());
		}
		if(town.isShielded()) {
			meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
			lore.add(ChatColor.LIGHT_PURPLE+""+ChatColor.ITALIC+MessagePath.LABEL_SHIELD.getMessage());
		}
		lore.add(ChatColor.GOLD+MessagePath.MENU_SCORE_HINT.getMessage());
		for(ItemFlag flag : ItemFlag.values()) {
			if(!meta.hasItemFlag(flag)) {
				meta.addItemFlags(flag);
			}
		}
		meta.setDisplayName(contextColor+town.getName());
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}
	
	public KonTown getTown() {
		return town;
	}
	
	@Override
	public int getIndex() {
		return index;
	}

	@Override
	public String getName() {
		return town.getName();
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
