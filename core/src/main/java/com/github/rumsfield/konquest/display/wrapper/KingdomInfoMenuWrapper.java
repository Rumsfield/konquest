package com.github.rumsfield.konquest.display.wrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.display.icon.InfoIcon;
import com.github.rumsfield.konquest.display.icon.MenuIcon;
import com.github.rumsfield.konquest.display.icon.PlayerIcon;
import com.github.rumsfield.konquest.display.icon.TownIcon;
import com.github.rumsfield.konquest.display.icon.PlayerIcon.PlayerIconAction;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.model.KonOfflinePlayer;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.model.KonTown;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;

public class KingdomInfoMenuWrapper extends MenuWrapper {

	private KonKingdom infoKingdom;
	private KonPlayer observer;
	
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
		
		String pageLabel = "";
 		List<String> loreList;
 		InfoIcon info;
 		final int MAX_ICONS_PER_PAGE = 45;
 		int slotIndex = 0;
 		int pageIndex = 0;
 		int pageRows = 0;
 		
 		List<OfflinePlayer> allKingdomMembers = new ArrayList<OfflinePlayer>();
 		allKingdomMembers.add(infoKingdom.getPlayerMaster());
 		allKingdomMembers.addAll(infoKingdom.getPlayerOfficersOnly());
 		allKingdomMembers.addAll(infoKingdom.getPlayerMembersOnly());

 		// Page 0
		pageIndex = 0;
		pageRows = 1;
		pageLabel = titleColor+MessagePath.COMMAND_INFO_NOTICE_KINGDOM_HEADER.getMessage(infoKingdom.getName());
		getMenu().addPage(pageIndex, pageRows, pageLabel);
		
		/* Master Player Info Icon (1) */
		slotIndex = 1;
		loreList = new ArrayList<String>();
		if(infoKingdom.isMasterValid()) {
			OfflinePlayer masterPlayer = infoKingdom.getPlayerMaster();
			loreList.add(ChatColor.LIGHT_PURPLE+MessagePath.LABEL_MASTER.getMessage());
			loreList.add(hintColor+MessagePath.MENU_SCORE_HINT.getMessage());
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
		
		/* Member Info Icon (2) */
		slotIndex = 2;
		int numKingdomPlayers = getKonquest().getPlayerManager().getPlayersInKingdom(infoKingdom).size();
    	int numAllKingdomPlayers = allKingdomMembers.size();
    	int numKingdomOfficers = infoKingdom.getPlayerOfficersOnly().size();
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_ONLINE_PLAYERS.getMessage()+": "+valueColor+numKingdomPlayers);
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL_PLAYERS.getMessage()+": "+valueColor+numAllKingdomPlayers);
    	loreList.add(loreColor+MessagePath.LABEL_OFFICERS.getMessage()+": "+valueColor+numKingdomOfficers);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_PLAYERS.getMessage(), loreList, Material.PLAYER_HEAD, slotIndex, false);
    	getMenu().getPage(pageIndex).addIcon(info);
    	
    	/* Properties Info Icon (3) */
    	slotIndex = 3;
    	String isPeaceful = DisplayManager.boolean2Symbol(infoKingdom.isPeaceful());
    	String isSmallest = DisplayManager.boolean2Symbol(infoKingdom.isSmallest());
    	String isProtected = DisplayManager.boolean2Symbol(infoKingdom.isOfflineProtected());
    	String isOpen = DisplayManager.boolean2Symbol(infoKingdom.isOpen());
    	String isAdminOperated = DisplayManager.boolean2Symbol(infoKingdom.isAdminOperated());
    	String templateName = infoKingdom.getMonumentTemplateName();
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_PEACEFUL.getMessage()+": "+isPeaceful);
    	loreList.add(loreColor+MessagePath.LABEL_SMALLEST.getMessage()+": "+isSmallest);
    	loreList.add(loreColor+MessagePath.LABEL_PROTECTED.getMessage()+": "+isProtected);
    	loreList.add(loreColor+MessagePath.LABEL_OPEN.getMessage()+": "+isOpen);
    	loreList.add(loreColor+MessagePath.LABEL_ADMIN_KINGDOM.getMessage()+": "+isAdminOperated);
    	loreList.add(loreColor+MessagePath.LABEL_MONUMENT.getMessage()+": "+templateName);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_PROPERTIES.getMessage(), loreList, Material.PAPER, slotIndex, false);
    	getMenu().getPage(pageIndex).addIcon(info);
    	
    	/* Favor Info Icon (4) */
    	slotIndex = 4;
    	ArrayList<KonOfflinePlayer> allPlayersInKingdom = getKonquest().getPlayerManager().getAllPlayersInKingdom(infoKingdom);
    	int numKingdomFavor = 0;
    	for(KonOfflinePlayer kingdomPlayer : allPlayersInKingdom) {
    		numKingdomFavor += (int) KonquestPlugin.getBalance(kingdomPlayer.getOfflineBukkitPlayer());
    	}
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+numKingdomFavor);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_FAVOR.getMessage(), loreList, Material.GOLD_BLOCK, slotIndex, false);
    	getMenu().getPage(pageIndex).addIcon(info);
    	
    	/* Towns Info Icon (5) */
    	slotIndex = 5;
    	int numKingdomTowns = infoKingdom.getTowns().size();
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+numKingdomTowns);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_TOWNS.getMessage(), loreList, getKonquest().getKingdomManager().getTownCriticalBlock(), slotIndex, false);
    	getMenu().getPage(pageIndex).addIcon(info);
    	
    	/* Land Info Icon (6) */
    	slotIndex = 6;
    	int numKingdomLand = 0;
    	for(KonTown town : infoKingdom.getTowns()) {
    		numKingdomLand += town.getChunkList().size();
    	}
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+numKingdomLand);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_LAND.getMessage(), loreList, Material.GRASS_BLOCK, slotIndex, false);
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
				loreList = new ArrayList<String>();
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
				loreList = new ArrayList<String>();
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
			if(clickPlayer != null && offlinePlayer != null && icon.getAction().equals(PlayerIconAction.DISPLAY_INFO)) {
				getKonquest().getDisplayManager().displayPlayerInfoMenu(clickPlayer, offlinePlayer);
			}
		}
	}

	// Sort kingdom town list by population then size
  	private List<KonTown> sortedTowns(KonKingdom kingdom) {
  		List<KonTown> sortedTowns = kingdom.getTowns();

  		// Sort each town list by population then size
  		Comparator<KonTown> townComparator = new Comparator<KonTown>() {
  			@Override
  			public int compare(final KonTown k1, KonTown k2) {
  				int result = 0;
  				if(k1.getNumResidents() < k2.getNumResidents()) {
  					result = 1;
  				} else if(k1.getNumResidents() > k2.getNumResidents()) {
  					result = -1;
  				} else {
  					if(k1.getChunkList().size() < k2.getChunkList().size()) {
  						result = 1;
  					} else if(k1.getChunkList().size() > k2.getChunkList().size()) {
  						result = -1;
  					}
  				}
  				return result;
  			}
  		};
  		Collections.sort(sortedTowns, townComparator);
  		
  		return sortedTowns;
  	}
}
