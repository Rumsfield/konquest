package konquest.display.wrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.display.icon.InfoIcon;
import konquest.display.icon.MenuIcon;
import konquest.display.icon.PlayerIcon;
import konquest.display.icon.TownIcon;
import konquest.display.icon.PlayerIcon.PlayerIconAction;
import konquest.manager.DisplayManager;
import konquest.model.KonKingdom;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

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
		
		ChatColor kingdomColor = getKonquest().getDisplayKingdomColor(observer.getKingdom(), infoKingdom);
		ChatColor titleColor = DisplayManager.titleColor;
		ChatColor loreColor = DisplayManager.loreColor;
		ChatColor valueColor = DisplayManager.valueColor;
		ChatColor hintColor = DisplayManager.hintColor;
		
		String pageLabel = "";
 		List<String> loreList;
 		InfoIcon info;
 		final int MAX_ICONS_PER_PAGE = 45;
 		
 		List<OfflinePlayer> allKingdomMembers = new ArrayList<OfflinePlayer>();
 		for(KonOfflinePlayer player : getKonquest().getPlayerManager().getAllPlayersInKingdom(infoKingdom)) {
 			allKingdomMembers.add(player.getOfflineBukkitPlayer());
 		}

 		// Page 0
		pageLabel = titleColor+MessagePath.COMMAND_INFO_NOTICE_KINGDOM_HEADER.getMessage(infoKingdom.getName());
		getMenu().addPage(0, 1, pageLabel);
		/* Member Info Icon (2) */
		int numKingdomPlayers = getKonquest().getPlayerManager().getPlayersInKingdom(infoKingdom).size();
    	int numAllKingdomPlayers = allKingdomMembers.size();
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_ONLINE_PLAYERS.getMessage()+": "+valueColor+numKingdomPlayers);
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL_PLAYERS.getMessage()+": "+valueColor+numAllKingdomPlayers);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_PLAYERS.getMessage(), loreList, Material.PLAYER_HEAD, 2, false);
    	getMenu().getPage(0).addIcon(info);
    	/* Properties Info Icon (3) */
    	String isPeaceful = DisplayManager.boolean2Symbol(infoKingdom.isPeaceful());
    	String isSmallest = DisplayManager.boolean2Symbol(infoKingdom.isSmallest());
    	String isProtected = DisplayManager.boolean2Symbol(infoKingdom.isOfflineProtected());
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_PEACEFUL.getMessage()+": "+isPeaceful);
    	loreList.add(loreColor+MessagePath.LABEL_SMALLEST.getMessage()+": "+isSmallest);
    	loreList.add(loreColor+MessagePath.LABEL_PROTECTED.getMessage()+": "+isProtected);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_PROPERTIES.getMessage(), loreList, Material.PAPER, 3, false);
    	getMenu().getPage(0).addIcon(info);
    	/* Favor Info Icon (4) */
    	ArrayList<KonOfflinePlayer> allPlayersInKingdom = getKonquest().getPlayerManager().getAllPlayersInKingdom(infoKingdom);
    	int numKingdomFavor = 0;
    	for(KonOfflinePlayer kingdomPlayer : allPlayersInKingdom) {
    		numKingdomFavor += (int) KonquestPlugin.getBalance(kingdomPlayer.getOfflineBukkitPlayer());
    	}
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+numKingdomFavor);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_FAVOR.getMessage(), loreList, Material.GOLD_BLOCK, 4, false);
    	getMenu().getPage(0).addIcon(info);
    	/* Towns Info Icon (5) */
    	int numKingdomTowns = infoKingdom.getTowns().size();
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+numKingdomTowns);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_TOWNS.getMessage(), loreList, getKonquest().getKingdomManager().getTownCriticalBlock(), 5, false);
    	getMenu().getPage(0).addIcon(info);
    	/* Land Info Icon (6) */
    	int numKingdomLand = 0;
    	for(KonTown town : infoKingdom.getTowns()) {
    		numKingdomLand += town.getChunkList().size();
    	}
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+numKingdomLand);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_LAND.getMessage(), loreList, Material.GRASS_BLOCK, 6, false);
    	getMenu().getPage(0).addIcon(info);
    	
    	// Page 1+
		List<KonTown> kingdomTowns = sortedTowns(infoKingdom);
		int pageTotal = (int)Math.ceil(((double)kingdomTowns.size())/MAX_ICONS_PER_PAGE);
		if(pageTotal == 0) {
			pageTotal = 1;
		}
		int pageNum = 1;
		ListIterator<KonTown> townIter = kingdomTowns.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int numPageRows = (int)Math.ceil(((double)(kingdomTowns.size() - i*MAX_ICONS_PER_PAGE))/9);
			if(numPageRows < 1) {
				numPageRows = 1;
			} else if(numPageRows > 5) {
				numPageRows = 5;
			}
			pageLabel = titleColor+infoKingdom.getName()+" "+MessagePath.LABEL_TOWNS.getMessage()+" "+(i+1)+"/"+pageTotal;
			getMenu().addPage(pageNum, numPageRows, pageLabel);
			int slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && townIter.hasNext()) {
				/* Town Icon (n) */
				KonTown currentTown = townIter.next();
				loreList = new ArrayList<String>();
				loreList.add(loreColor+MessagePath.LABEL_POPULATION.getMessage()+": "+valueColor+currentTown.getNumResidents());
		    	loreList.add(loreColor+MessagePath.LABEL_LAND.getMessage()+": "+valueColor+currentTown.getChunkList().size());
		    	boolean isFriendly = currentTown.getKingdom().equals(observer.getKingdom());
		    	boolean isArmistice = getKonquest().getGuildManager().isArmistice(observer, currentTown);
		    	TownIcon townIcon = new TownIcon(currentTown,isFriendly,isArmistice,getKonquest().getKingdomManager().getTownCriticalBlock(),loreList,slotIndex);
		    	getMenu().getPage(pageNum).addIcon(townIcon);
				slotIndex++;
			}
			pageNum++;
		}
		
		// Page 2+
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
			getMenu().addPage(pageNum, numPageRows, pageLabel);
			int slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && memberIter.hasNext()) {
				/* Player Icon (n) */
				OfflinePlayer currentMember = memberIter.next();
				loreList = new ArrayList<String>();
				loreList.add(loreColor+MessagePath.LABEL_INFORMATION.getMessage());
				loreList.add(ChatColor.WHITE+MessagePath.LABEL_PLAYER.getMessage());
		    	loreList.add(hintColor+MessagePath.MENU_SCORE_HINT.getMessage());
		    	PlayerIcon player = new PlayerIcon(kingdomColor+currentMember.getName(),loreList,currentMember,slotIndex,true,PlayerIconAction.DISPLAY_INFO);
		    	getMenu().getPage(pageNum).addIcon(player);
				slotIndex++;
			}
			pageNum++;
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
