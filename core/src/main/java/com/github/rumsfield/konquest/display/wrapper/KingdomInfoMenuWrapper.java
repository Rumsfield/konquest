package com.github.rumsfield.konquest.display.wrapper;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.display.icon.InfoIcon;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.display.icon.PlayerIcon;
import com.github.rumsfield.konquest.display.icon.PlayerIcon.PlayerIconAction;
import com.github.rumsfield.konquest.display.icon.TownIcon;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class KingdomInfoMenuWrapper extends MenuWrapper {

	private final KonKingdom infoKingdom;
	private final KonPlayer observer;
	
	public KingdomInfoMenuWrapper(Konquest konquest, KonKingdom infoKingdom, KonPlayer observer) {
		super(konquest);
		this.infoKingdom = infoKingdom;
		this.observer = observer;
	}

	@Override
	public void constructMenu() {
		
		ChatColor kingdomColor = getKonquest().getDisplayPrimaryColor(observer.getKingdom(), infoKingdom);
		ChatColor titleColor = DisplayManager.titleColor;
		ChatColor loreColor = DisplayManager.loreColor;
		ChatColor valueColor = DisplayManager.valueColor;
		ChatColor hintColor = DisplayManager.hintColor;
		
		String pageLabel;
 		List<String> loreList;
 		InfoIcon info;
		final int MAX_ICONS_PER_PAGE = 45;
		int slotIndex;
		int pageIndex = 0;
		int pageRows = 1;

		OfflinePlayer masterPlayer = infoKingdom.getPlayerMaster();
 		List<OfflinePlayer> allKingdomMembers = new ArrayList<>();
		if(masterPlayer != null) {
			allKingdomMembers.add(masterPlayer);
		}
 		allKingdomMembers.addAll(infoKingdom.getPlayerOfficersOnly());
 		allKingdomMembers.addAll(infoKingdom.getPlayerMembersOnly());

 		// Page 0
		pageLabel = titleColor+MessagePath.COMMAND_INFO_NOTICE_KINGDOM_HEADER.getMessage(infoKingdom.getName());
		getMenu().addPage(pageIndex, pageRows, pageLabel);

		/* Capital Info Icon (1) */
		slotIndex = 1;
		KonCapital capital = infoKingdom.getCapital();
		loreList = new ArrayList<>();
		loreList.add(loreColor+"Capital");
		loreList.add(loreColor+MessagePath.LABEL_POPULATION.getMessage()+": "+valueColor+capital.getNumResidents());
		loreList.add(loreColor+MessagePath.LABEL_LAND.getMessage()+": "+valueColor+capital.getChunkList().size());
		TownIcon capitalIcon = new TownIcon(capital,kingdomColor,getKonquest().getKingdomManager().getTownCriticalBlock(),loreList,slotIndex);
		getMenu().getPage(pageIndex).addIcon(capitalIcon);

		/* Master Player Info Icon (2) */
		slotIndex = 2;
		loreList = new ArrayList<>();
		if(infoKingdom.isMasterValid()) {
			loreList.add(ChatColor.LIGHT_PURPLE+MessagePath.LABEL_MASTER.getMessage());
			loreList.add(hintColor+MessagePath.MENU_SCORE_HINT.getMessage());
			assert masterPlayer != null;
			PlayerIcon playerInfo = new PlayerIcon(kingdomColor+masterPlayer.getName(),loreList,masterPlayer,slotIndex,true,PlayerIconAction.DISPLAY_INFO);
			getMenu().getPage(pageIndex).addIcon(playerInfo);
		} else {
			//for(String line : Konquest.stringPaginate(MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(infoTown.getName(), infoTown.getName(), observer.getBukkitPlayer().getName()))) {
			//	loreList.add(ChatColor.RED+line);
			//}
			//TODO: Add message for no master?
			loreList.add(ChatColor.RED+"No master");
			info = new InfoIcon(ChatColor.DARK_PURPLE+MessagePath.LABEL_LORD.getMessage(),loreList,Material.BARRIER,slotIndex,false);
			getMenu().getPage(pageIndex).addIcon(info);
		}
		
		/* Member Info Icon (3) */
		slotIndex = 3;
		int numKingdomPlayers = getKonquest().getPlayerManager().getPlayersInKingdom(infoKingdom).size();
    	int numAllKingdomPlayers = allKingdomMembers.size();
    	int numKingdomOfficers = infoKingdom.getPlayerOfficersOnly().size();
    	loreList = new ArrayList<>();
    	loreList.add(loreColor+MessagePath.LABEL_ONLINE_PLAYERS.getMessage()+": "+valueColor+numKingdomPlayers);
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL_PLAYERS.getMessage()+": "+valueColor+numAllKingdomPlayers);
    	loreList.add(loreColor+MessagePath.LABEL_OFFICERS.getMessage()+": "+valueColor+numKingdomOfficers);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_PLAYERS.getMessage(), loreList, Material.PLAYER_HEAD, slotIndex, false);
    	getMenu().getPage(pageIndex).addIcon(info);
    	
    	/* Properties Info Icon (5) */
    	slotIndex = 5;
    	String isPeaceful = DisplayManager.boolean2Symbol(infoKingdom.isPeaceful());
    	String isSmallest = DisplayManager.boolean2Symbol(infoKingdom.isSmallest());
    	String isProtected = DisplayManager.boolean2Symbol(infoKingdom.isOfflineProtected());
    	String isOpen = DisplayManager.boolean2Symbol(infoKingdom.isOpen());
    	String isAdminOperated = DisplayManager.boolean2Symbol(infoKingdom.isAdminOperated());
    	loreList = new ArrayList<>();
    	loreList.add(loreColor+MessagePath.LABEL_PEACEFUL.getMessage()+": "+isPeaceful);
    	loreList.add(loreColor+MessagePath.LABEL_SMALLEST.getMessage()+": "+isSmallest);
    	loreList.add(loreColor+MessagePath.LABEL_PROTECTED.getMessage()+": "+isProtected);
    	loreList.add(loreColor+MessagePath.LABEL_OPEN.getMessage()+": "+isOpen);
    	loreList.add(loreColor+MessagePath.LABEL_ADMIN_KINGDOM.getMessage()+": "+isAdminOperated);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_PROPERTIES.getMessage(), loreList, Material.PAPER, slotIndex, false);
    	getMenu().getPage(pageIndex).addIcon(info);

		/* Stats Info Icon (6) */
		slotIndex = 6;
		ArrayList<KonOfflinePlayer> allPlayersInKingdom = getKonquest().getPlayerManager().getAllPlayersInKingdom(infoKingdom);
		int numKingdomFavor = 0;
		for(KonOfflinePlayer kingdomPlayer : allPlayersInKingdom) {
			numKingdomFavor += (int) KonquestPlugin.getBalance(kingdomPlayer.getOfflineBukkitPlayer());
		}
		int numKingdomTowns = infoKingdom.getTowns().size();
		int numKingdomLand = 0;
		for(KonTown town : infoKingdom.getTowns()) {
			numKingdomLand += town.getChunkList().size();
		}
		loreList = new ArrayList<>();
		loreList.add(loreColor+MessagePath.LABEL_FAVOR.getMessage()+": "+valueColor+numKingdomFavor);
		loreList.add(loreColor+MessagePath.LABEL_TOWNS.getMessage()+": "+valueColor+numKingdomTowns);
		loreList.add(loreColor+MessagePath.LABEL_LAND.getMessage()+": "+valueColor+numKingdomLand);
		info = new InfoIcon(kingdomColor+MessagePath.LABEL_INFORMATION.getMessage(), loreList, Material.ENDER_EYE, slotIndex, false);
		getMenu().getPage(pageIndex).addIcon(info);

		/* Template Info Icon (7) */
		slotIndex = 7;
		loreList = new ArrayList<>();
		// TODO message paths
		if(infoKingdom.hasMonumentTemplate()) {
			KonMonumentTemplate template = infoKingdom.getMonumentTemplate();
			loreList.add(loreColor+"Name"+": "+valueColor+template.getName());
			loreList.add(loreColor+"Critical Hits"+": "+valueColor+template.getNumCriticals());
			loreList.add(loreColor+"Loot Chests"+": "+valueColor+template.getNumLootChests());
			if(template.isBlanking()) {
				loreList.add(ChatColor.RED+"Temporarily Disabled");
			}
			info = new InfoIcon(kingdomColor+"Monument Template", loreList, Material.CRAFTING_TABLE, slotIndex, false);
		} else {
			String invalidMessage = "Kingdom master must choose a template from the kingdom menu.";
			for(String line : Konquest.stringPaginate(invalidMessage)) {
				loreList.add(loreColor+line);
			}
			info = new InfoIcon(ChatColor.RED+"Invalid",loreList,Material.BARRIER,slotIndex,false);
		}
		getMenu().getPage(pageIndex).addIcon(info);
    	
    	// Page 1+
    	// Town List
		List<KonTown> kingdomTowns = sortedTowns(infoKingdom);
		int pageTotal = (int)Math.ceil(((double)kingdomTowns.size())/MAX_ICONS_PER_PAGE);
		if(pageTotal == 0) {
			pageTotal = 1;
		}
		pageIndex = 1;
		ListIterator<KonTown> townIter = kingdomTowns.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int numPageRows = (int)Math.ceil(((double)(kingdomTowns.size() - i*MAX_ICONS_PER_PAGE))/9);
			if(numPageRows < 1) {
				numPageRows = 1;
			} else if(numPageRows > 5) {
				numPageRows = 5;
			}
			pageLabel = titleColor+infoKingdom.getName()+" "+MessagePath.LABEL_TOWNS.getMessage()+" "+(i+1)+"/"+pageTotal;
			getMenu().addPage(pageIndex, numPageRows, pageLabel);
			slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && townIter.hasNext()) {
				/* Town Icon (n) */
				KonTown currentTown = townIter.next();
				loreList = new ArrayList<>();
				loreList.add(loreColor+MessagePath.LABEL_POPULATION.getMessage()+": "+valueColor+currentTown.getNumResidents());
		    	loreList.add(loreColor+MessagePath.LABEL_LAND.getMessage()+": "+valueColor+currentTown.getChunkList().size());
		    	TownIcon townIcon = new TownIcon(currentTown,kingdomColor,getKonquest().getKingdomManager().getTownCriticalBlock(),loreList,slotIndex);
		    	getMenu().getPage(pageIndex).addIcon(townIcon);
				slotIndex++;
			}
			pageIndex++;
		}
		
		// Page 2+
		// All kingdom members
		pageTotal = (int)Math.ceil(((double)allKingdomMembers.size())/MAX_ICONS_PER_PAGE);
		if(pageTotal == 0) {
			pageTotal = 1;
		}
		ListIterator<OfflinePlayer> memberIter = allKingdomMembers.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int numPageRows = (int)Math.ceil(((double)(allKingdomMembers.size() - i*MAX_ICONS_PER_PAGE))/9);
			if(numPageRows < 1) {
				numPageRows = 1;
			} else if(numPageRows > 5) {
				numPageRows = 5;
			}
			pageLabel = titleColor+infoKingdom.getName()+" "+MessagePath.LABEL_PLAYERS.getMessage()+" "+(i+1)+"/"+pageTotal;
			getMenu().addPage(pageIndex, numPageRows, pageLabel);
			slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && memberIter.hasNext()) {
				/* Player Icon (n) */
				OfflinePlayer currentMember = memberIter.next();
				if(currentMember != null) {
					loreList = new ArrayList<>();
					loreList.add(loreColor+MessagePath.LABEL_INFORMATION.getMessage());
					String playerType = ChatColor.WHITE+MessagePath.LABEL_MEMBER.getMessage();
					if(infoKingdom.isMaster(currentMember.getUniqueId())) {
						playerType = ChatColor.LIGHT_PURPLE+MessagePath.LABEL_MASTER.getMessage();
					} else if(infoKingdom.isOfficer(currentMember.getUniqueId())) {
						playerType = ChatColor.BLUE+MessagePath.LABEL_OFFICER.getMessage();
					}
					loreList.add(playerType);
					loreList.add(hintColor+MessagePath.MENU_SCORE_HINT.getMessage());
					PlayerIcon player = new PlayerIcon(kingdomColor+currentMember.getName(),loreList,currentMember,slotIndex,true,PlayerIconAction.DISPLAY_INFO);
					getMenu().getPage(pageIndex).addIcon(player);
					slotIndex++;
				}
			}
			pageIndex++;
		}
				
		getMenu().refreshNavigationButtons();
		getMenu().setPageIndex(0);
	}

	@Override
	public void onIconClick(KonPlayer clickPlayer, MenuIcon clickedIcon) {
		Player bukkitPlayer = clickPlayer.getBukkitPlayer();
		if(clickedIcon instanceof InfoIcon) {
			// Info Icons close the GUI and print their info in chat
			InfoIcon icon = (InfoIcon)clickedIcon;
			ChatUtil.sendNotice(bukkitPlayer, icon.getInfo());
		} else if(clickedIcon instanceof TownIcon) {
			// Town Icons open a new town info menu for the associated player
			TownIcon icon = (TownIcon)clickedIcon;
			getKonquest().getDisplayManager().displayTownInfoMenu(clickPlayer,icon.getTown());
		} else if(clickedIcon instanceof PlayerIcon) {
			// Player Head Icons open a new info menu for the associated player
			PlayerIcon icon = (PlayerIcon)clickedIcon;
			KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayer(icon.getOfflinePlayer());
			if(offlinePlayer != null && icon.getAction().equals(PlayerIconAction.DISPLAY_INFO)) {
				getKonquest().getDisplayManager().displayPlayerInfoMenu(clickPlayer, offlinePlayer);
			}
		}
	}

	// Sort kingdom town list by population then size
  	private List<KonTown> sortedTowns(KonKingdom kingdom) {
  		List<KonTown> sortedTowns = kingdom.getTowns();

  		// Sort each town list by population then size
  		Comparator<KonTown> townComparator = (townOne, townTwo) -> {
			  int result = 0;
			  if(townOne.getNumResidents() < townTwo.getNumResidents()) {
				  result = 1;
			  } else if(townOne.getNumResidents() > townTwo.getNumResidents()) {
				  result = -1;
			  } else {
				  if(townOne.getChunkList().size() < townTwo.getChunkList().size()) {
					  result = 1;
				  } else if(townOne.getChunkList().size() > townTwo.getChunkList().size()) {
					  result = -1;
				  }
			  }
			  return result;
		  };
  		sortedTowns.sort(townComparator);
  		
  		return sortedTowns;
  	}
}