package com.github.rumsfield.konquest.display.wrapper;

import java.util.List;
import java.util.ListIterator;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.display.icon.ArmorIcon;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.display.icon.ShieldIcon;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonArmor;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonShield;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.utility.MessagePath;

public class TownShieldMenuWrapper extends MenuWrapper {

	private KonTown town;
	
	public TownShieldMenuWrapper(Konquest konquest, KonTown town) {
		super(konquest);
		this.town = town;
	}

	@Override
	public void constructMenu() {
		String pageLabel = "";
		ChatColor titleColor = DisplayManager.titleColor;
		int pageTotal = 0;
		final int MAX_ICONS_PER_PAGE = 45;
		
		// Page 0+
		List<KonShield> allShields = getKonquest().getShieldManager().getShields();
		boolean isShieldsEnabled = getKonquest().getShieldManager().isShieldsEnabled();
		pageTotal = (int)Math.ceil(((double)allShields.size())/MAX_ICONS_PER_PAGE);
		if(pageTotal == 0) {
			pageTotal = 1;
		}
		int pageNum = 0;
		if(isShieldsEnabled) {
			ListIterator<KonShield> shieldIter = allShields.listIterator();
			for(int i = 0; i < pageTotal; i++) {
				int numPageRows = (int)Math.ceil(((double)(allShields.size() - i*MAX_ICONS_PER_PAGE))/9);
				if(numPageRows < 1) {
					numPageRows = 1;
				} else if(numPageRows > 5) {
					numPageRows = 5;
				}
				pageLabel = titleColor+town.getName()+" "+MessagePath.LABEL_SHIELDS.getMessage()+" "+(i+1)+"/"+pageTotal;
				getMenu().addPage(pageNum, numPageRows, pageLabel);
				int slotIndex = 0;
				while(slotIndex < MAX_ICONS_PER_PAGE && shieldIter.hasNext()) {
					/* Shield Icon (n) */
					KonShield currentShield = shieldIter.next();
			    	ShieldIcon shieldIcon = new ShieldIcon(currentShield, true, town.getNumResidents(), slotIndex);
			    	getMenu().getPage(pageNum).addIcon(shieldIcon);
					slotIndex++;
				}
				pageNum++;
			}
		}
		
		// Page N+
		List<KonArmor> allArmors = getKonquest().getShieldManager().getArmors();
		boolean isArmorsEnabled = getKonquest().getShieldManager().isArmorsEnabled();
		pageTotal = (int)Math.ceil(((double)allArmors.size())/MAX_ICONS_PER_PAGE);
		if(pageTotal == 0) {
			pageTotal = 1;
		}
		if(isArmorsEnabled) {
			ListIterator<KonArmor> armorIter = allArmors.listIterator();
			for(int i = 0; i < pageTotal; i++) {
				int numPageRows = (int)Math.ceil(((double)(allArmors.size() - i*MAX_ICONS_PER_PAGE))/9);
				if(numPageRows < 1) {
					numPageRows = 1;
				} else if(numPageRows > 5) {
					numPageRows = 5;
				}
				pageLabel = titleColor+town.getName()+" "+MessagePath.LABEL_ARMORS.getMessage()+" "+(i+1)+"/"+pageTotal;
				getMenu().addPage(pageNum, numPageRows, pageLabel);
				int slotIndex = 0;
				while(slotIndex < MAX_ICONS_PER_PAGE && armorIter.hasNext()) {
					/* Armor Icon (n) */
					KonArmor currentArmor = armorIter.next();
			    	ArmorIcon armorIcon = new ArmorIcon(currentArmor, true, town.getNumResidents(), slotIndex);
			    	getMenu().getPage(pageNum).addIcon(armorIcon);
					slotIndex++;
				}
				pageNum++;
			}
		}
		
		getMenu().refreshNavigationButtons();
		getMenu().setPageIndex(0);
	}

	@Override
	public void onIconClick(KonPlayer clickPlayer, MenuIcon clickedIcon) {
		Player bukkitPlayer = clickPlayer.getBukkitPlayer();
		if(clickedIcon instanceof ShieldIcon) {
			// Shield Icons close the GUI and attempt to activate a town shield
			ShieldIcon icon = (ShieldIcon)clickedIcon;
			boolean status = getKonquest().getShieldManager().activateTownShield(icon.getShield(), town, bukkitPlayer);
			if(status) {
				Konquest.playSuccessSound(bukkitPlayer);
			} else {
				Konquest.playFailSound(bukkitPlayer);
			}
		} else if(clickedIcon instanceof ArmorIcon) {
			// Armor Icons close the GUI and attempt to activate a town armor
			ArmorIcon icon = (ArmorIcon)clickedIcon;
			boolean status = getKonquest().getShieldManager().activateTownArmor(icon.getArmor(), town, bukkitPlayer);
			if(status) {
				Konquest.playSuccessSound(bukkitPlayer);
			} else {
				Konquest.playFailSound(bukkitPlayer);
			}
		}
	}

}
