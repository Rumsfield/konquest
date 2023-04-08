package com.github.rumsfield.konquest.display.wrapper;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.api.model.KonquestDiplomacyType;
import com.github.rumsfield.konquest.display.icon.*;
import com.github.rumsfield.konquest.display.icon.PlayerIcon.PlayerIconAction;
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
		String titleColor = DisplayManager.titleFormat;
		String loreColor = DisplayManager.loreFormat;
		String valueColor = DisplayManager.valueFormat;
		String hintColor = DisplayManager.hintFormat;
		String propertyColor = DisplayManager.propertyFormat;
		
		String pageLabel;
 		List<String> loreList;
 		MenuIcon info;
		int slotIndex;
		int pageIndex = 0;
		int pageRows = 1;
		int pageTotal = 1;

		// Limit which icons appear if the kingdom is not created (e.g. a default Neutrals or Barbarian)

		OfflinePlayer masterPlayer = infoKingdom.getPlayerMaster();
 		List<OfflinePlayer> allKingdomMembers = new ArrayList<>();
		if(infoKingdom.isCreated()) {
			if(masterPlayer != null) {
				allKingdomMembers.add(masterPlayer);
			}
			allKingdomMembers.addAll(infoKingdom.getPlayerOfficersOnly());
			allKingdomMembers.addAll(infoKingdom.getPlayerMembersOnly());
		} else {
			allKingdomMembers.addAll(getKonquest().getPlayerManager().getAllBukkitPlayersInKingdom(infoKingdom));
		}

 		// Page 0
		pageLabel = titleColor+MessagePath.COMMAND_INFO_NOTICE_KINGDOM_HEADER.getMessage(infoKingdom.getName());
		getMenu().addPage(pageIndex, pageRows, pageLabel);

		/* Capital Info Icon (1) */
		if(infoKingdom.isCreated()) {
			slotIndex = 1;
			KonCapital capital = infoKingdom.getCapital();
			loreList = new ArrayList<>();
			loreList.add(hintColor + MessagePath.MENU_SCORE_HINT.getMessage());
			TownIcon capitalIcon = new TownIcon(capital, kingdomColor, loreList, slotIndex, true);
			getMenu().getPage(pageIndex).addIcon(capitalIcon);
		}

		/* Master Player Info Icon (2) */
		if(infoKingdom.isCreated()) {
			slotIndex = 2;
			loreList = new ArrayList<>();
			if (infoKingdom.isMasterValid()) {
				loreList.add(propertyColor + MessagePath.LABEL_MASTER.getMessage());
				loreList.add(hintColor + MessagePath.MENU_SCORE_HINT.getMessage());
				assert masterPlayer != null;
				PlayerIcon playerInfo = new PlayerIcon(kingdomColor + masterPlayer.getName(), loreList, masterPlayer, slotIndex, true, PlayerIconAction.DISPLAY_INFO);
				getMenu().getPage(pageIndex).addIcon(playerInfo);
			} else {
				info = new InfoIcon(ChatColor.DARK_RED + MessagePath.LABEL_MASTER.getMessage(), loreList, Material.BARRIER, slotIndex, false);
				getMenu().getPage(pageIndex).addIcon(info);
			}
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
		loreList.add(loreColor+MessagePath.LABEL_ADMIN_KINGDOM.getMessage()+": "+isAdminOperated);
		loreList.add(loreColor+MessagePath.LABEL_PROTECTED.getMessage()+": "+isProtected);
		loreList.add(loreColor+MessagePath.LABEL_OPEN.getMessage()+": "+isOpen);
		loreList.add(loreColor+MessagePath.LABEL_SMALLEST.getMessage()+": "+isSmallest);
		loreList.add(loreColor+MessagePath.LABEL_PEACEFUL.getMessage()+": "+isPeaceful);
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
		for(KonTown town : infoKingdom.getCapitalTowns()) {
			numKingdomLand += town.getNumLand();
		}
		loreList = new ArrayList<>();
		loreList.add(loreColor+MessagePath.LABEL_FAVOR.getMessage()+": "+valueColor+numKingdomFavor);
		loreList.add(loreColor+MessagePath.LABEL_TOWNS.getMessage()+": "+valueColor+numKingdomTowns);
		loreList.add(loreColor+MessagePath.LABEL_LAND.getMessage()+": "+valueColor+numKingdomLand);
		info = new InfoIcon(kingdomColor+MessagePath.LABEL_INFORMATION.getMessage(), loreList, Material.ENDER_EYE, slotIndex, false);
		getMenu().getPage(pageIndex).addIcon(info);

		/* Template Info Icon (7) */
		if(infoKingdom.isCreated()) {
			slotIndex = 7;
			loreList = new ArrayList<>();
			if (infoKingdom.hasMonumentTemplate()) {
				KonMonumentTemplate template = infoKingdom.getMonumentTemplate();
				info = new TemplateIcon(template,kingdomColor,loreList,slotIndex,false);
			} else {
				info = new InfoIcon(ChatColor.RED+MessagePath.LABEL_INVALID.getMessage(), loreList, Material.BARRIER, slotIndex, false);
			}
			getMenu().getPage(pageIndex).addIcon(info);
		}
		pageIndex++;

    	// Pages for created kingdoms
		if(infoKingdom.isCreated()) {

			/* Enemy Kingdoms */
			List<KonKingdom> enemyKingdoms = infoKingdom.getActiveRelationKingdoms(KonquestDiplomacyType.WAR);
			enemyKingdoms.sort(kingdomComparator);
			pageTotal = getTotalPages(enemyKingdoms.size());
			ListIterator<KonKingdom> enemyIterator = enemyKingdoms.listIterator();
			for (int i = 0; i < pageTotal; i++) {
				int numPageRows = getNumPageRows(enemyKingdoms.size(),i);
				pageLabel = titleColor + infoKingdom.getName() + " " + MessagePath.DIPLOMACY_WAR.getMessage() + " " + (i + 1) + "/" + pageTotal;
				getMenu().addPage(pageIndex, numPageRows, pageLabel);
				slotIndex = 0;
				while (slotIndex < MAX_ICONS_PER_PAGE && enemyIterator.hasNext()) {
					/* Kingdom Icon (n) */
					KonKingdom currentKingdom = enemyIterator.next();
					loreList = new ArrayList<>();
					loreList.add(hintColor + MessagePath.MENU_SCORE_HINT.getMessage());
					KingdomIcon kingdomIcon = new KingdomIcon(currentKingdom,Konquest.enemyColor1,loreList,slotIndex,true);
					getMenu().getPage(pageIndex).addIcon(kingdomIcon);
					slotIndex++;
				}
				pageIndex++;
			}

			/* Allied Kingdoms */
			List<KonKingdom> allyKingdoms = infoKingdom.getActiveRelationKingdoms(KonquestDiplomacyType.ALLIANCE);
			allyKingdoms.sort(kingdomComparator);
			pageTotal = getTotalPages(allyKingdoms.size());
			ListIterator<KonKingdom> allyIterator = allyKingdoms.listIterator();
			for (int i = 0; i < pageTotal; i++) {
				int numPageRows = getNumPageRows(allyKingdoms.size(),i);
				pageLabel = titleColor + infoKingdom.getName() + " " + MessagePath.DIPLOMACY_ALLIANCE.getMessage() + " " + (i + 1) + "/" + pageTotal;
				getMenu().addPage(pageIndex, numPageRows, pageLabel);
				slotIndex = 0;
				while (slotIndex < MAX_ICONS_PER_PAGE && allyIterator.hasNext()) {
					/* Kingdom Icon (n) */
					KonKingdom currentKingdom = allyIterator.next();
					loreList = new ArrayList<>();
					loreList.add(hintColor + MessagePath.MENU_SCORE_HINT.getMessage());
					KingdomIcon kingdomIcon = new KingdomIcon(currentKingdom,Konquest.alliedColor1,loreList,slotIndex,true);
					getMenu().getPage(pageIndex).addIcon(kingdomIcon);
					slotIndex++;
				}
				pageIndex++;
			}

			/* Trade Kingdoms */
			List<KonKingdom> tradeKingdoms = infoKingdom.getActiveRelationKingdoms(KonquestDiplomacyType.TRADE);
			tradeKingdoms.sort(kingdomComparator);
			pageTotal = getTotalPages(tradeKingdoms.size());
			ListIterator<KonKingdom> tradeIterator = tradeKingdoms.listIterator();
			for (int i = 0; i < pageTotal; i++) {
				int numPageRows = getNumPageRows(tradeKingdoms.size(),i);
				pageLabel = titleColor + infoKingdom.getName() + " " + MessagePath.DIPLOMACY_TRADE.getMessage() + " " + (i + 1) + "/" + pageTotal;
				getMenu().addPage(pageIndex, numPageRows, pageLabel);
				slotIndex = 0;
				while (slotIndex < MAX_ICONS_PER_PAGE && tradeIterator.hasNext()) {
					/* Kingdom Icon (n) */
					KonKingdom currentKingdom = tradeIterator.next();
					loreList = new ArrayList<>();
					loreList.add(hintColor + MessagePath.MENU_SCORE_HINT.getMessage());
					KingdomIcon kingdomIcon = new KingdomIcon(currentKingdom,Konquest.tradeColor1,loreList,slotIndex,true);
					getMenu().getPage(pageIndex).addIcon(kingdomIcon);
					slotIndex++;
				}
				pageIndex++;
			}

			/* Town List */
			List<KonTown> kingdomTowns = infoKingdom.getTowns();
			kingdomTowns.sort(townComparator);
			pageTotal = getTotalPages(kingdomTowns.size());
			ListIterator<KonTown> townIter = kingdomTowns.listIterator();
			for (int i = 0; i < pageTotal; i++) {
				int numPageRows = getNumPageRows(kingdomTowns.size(),i);
				pageLabel = titleColor + infoKingdom.getName() + " " + MessagePath.LABEL_TOWNS.getMessage() + " " + (i + 1) + "/" + pageTotal;
				getMenu().addPage(pageIndex, numPageRows, pageLabel);
				slotIndex = 0;
				while (slotIndex < MAX_ICONS_PER_PAGE && townIter.hasNext()) {
					/* Town Icon (n) */
					KonTown currentTown = townIter.next();
					loreList = new ArrayList<>();
					loreList.add(hintColor + MessagePath.MENU_SCORE_HINT.getMessage());
					TownIcon townIcon = new TownIcon(currentTown, kingdomColor, loreList, slotIndex, true);
					getMenu().getPage(pageIndex).addIcon(townIcon);
					slotIndex++;
				}
				pageIndex++;
			}
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
					loreList.add(propertyColor+infoKingdom.getPlayerRoleName(currentMember));
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
		} else if(clickedIcon instanceof KingdomIcon) {
			// Kingdom Icons open a new kingdom info menu for the associated player
			KingdomIcon icon = (KingdomIcon)clickedIcon;
			getKonquest().getDisplayManager().displayKingdomInfoMenu(clickPlayer,icon.getKingdom());
		}
	}

}
