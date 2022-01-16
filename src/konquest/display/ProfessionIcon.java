package konquest.display;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ProfessionIcon implements MenuIcon {

	private String name;
	private List<String> lore;
	private Villager.Profession profession;
	private int index;
	private boolean isClickable;
	
	public ProfessionIcon(String name, List<String> lore, Villager.Profession profession, int index, boolean isClickable) {
		this.name = name;
		this.lore = lore;
		this.profession = profession;
		this.index = index;
		this.isClickable = isClickable;
	}
	
	public Villager.Profession getProfession() {
		return profession;
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
		ItemStack item = new ItemStack(getMat(),1);
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
		return isClickable;
	}
	
	private Material getMat() {
		Material result = Material.EMERALD;
		switch(profession) {
			case ARMORER:
				result = Material.BLAST_FURNACE;
				break;
			case BUTCHER:
				result = Material.SMOKER;
				break;
			case CARTOGRAPHER:
				result = Material.CARTOGRAPHY_TABLE;
				break;
			case CLERIC:
				result = Material.BREWING_STAND;
				break;
			case FARMER:
				result = Material.COMPOSTER;
				break;
			case FISHERMAN:
				result = Material.BARREL;
				break;
			case FLETCHER:
				result = Material.FLETCHING_TABLE;
				break;
			case LEATHERWORKER:
				result = Material.CAULDRON;
				break;
			case LIBRARIAN:
				result = Material.LECTERN;
				break;
			case MASON:
				result = Material.STONECUTTER;
				break;
			case NITWIT:
				result = Material.PUFFERFISH_BUCKET;
				break;
			case NONE:
				result = Material.GRAVEL;
				break;
			case SHEPHERD:
				result = Material.LOOM;
				break;
			case TOOLSMITH:
				result = Material.SMITHING_TABLE;
				break;
			case WEAPONSMITH:
				result = Material.GRINDSTONE;
				break;
			default:
				break;
		}
		return result;
	}
}
