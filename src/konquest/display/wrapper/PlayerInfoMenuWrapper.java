package konquest.display.wrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.display.GuildIcon;
import konquest.display.InfoIcon;
import konquest.display.KingdomIcon;
import konquest.display.MenuIcon;
import konquest.display.PlayerIcon;
import konquest.display.TownIcon;
import konquest.display.PlayerIcon.PlayerIconAction;
import konquest.manager.DisplayManager;
import konquest.model.KonGuild;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class PlayerInfoMenuWrapper extends MenuWrapper {

	private KonOfflinePlayer infoPlayer;
	private KonPlayer observer;
	
	public PlayerInfoMenuWrapper(Konquest konquest, KonOfflinePlayer infoPlayer, KonPlayer observer) {
		super(konquest);
		this.infoPlayer = infoPlayer;
		this.observer = observer;
	}

	@Override
	public void constructMenu() {
		boolean isFriendly = observer.getKingdom().equals(infoPlayer.getKingdom());
 		boolean isArmistice = getKonquest().getGuildManager().isArmistice(observer, infoPlayer);
 		
 		ChatColor kingdomColor = Konquest.getDisplayPrimaryColor(observer, infoPlayer, isArmistice);
		ChatColor titleColor = DisplayManager.titleColor;
		ChatColor loreColor = DisplayManager.loreColor;
		ChatColor valueColor = DisplayManager.valueColor;
		ChatColor hintColor = DisplayManager.hintColor;

		String pageLabel = "";
		List<String> loreList;
 		
 		// Page 0
		pageLabel = titleColor+MessagePath.LABEL_PLAYER.getMessage()+" "+infoPlayer.getOfflineBukkitPlayer().getName();
		getMenu().addPage(0, 1, pageLabel);
		
		/* Kingdom Icon (2) */
		int numKingdomPlayers = getKonquest().getPlayerManager().getPlayersInKingdom(infoPlayer.getKingdom()).size();
    	int numAllKingdomPlayers = getKonquest().getPlayerManager().getAllPlayersInKingdom(infoPlayer.getKingdom()).size();
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_PLAYERS.getMessage()+": "+valueColor+numKingdomPlayers+"/"+numAllKingdomPlayers);
    	if(infoPlayer.getKingdom().isOfflineProtected()) {
    		loreList.add(ChatColor.LIGHT_PURPLE+""+ChatColor.ITALIC+MessagePath.LABEL_PROTECTED.getMessage());
    	}
    	loreList.add(hintColor+MessagePath.MENU_SCORE_HINT.getMessage());
    	KingdomIcon kingdom = new KingdomIcon(infoPlayer.getKingdom(),kingdomColor,Material.GOLDEN_HELMET,loreList,2);
    	getMenu().getPage(0).addIcon(kingdom);
    	/* Guild Icon (3) */
		KonGuild guild = getKonquest().getGuildManager().getPlayerGuild(infoPlayer.getOfflineBukkitPlayer());
		if(guild != null) {
			loreList = new ArrayList<String>();
			if(guild.isMaster(infoPlayer.getOfflineBukkitPlayer().getUniqueId())) {
				loreList.add(ChatColor.LIGHT_PURPLE+MessagePath.LABEL_MASTER.getMessage());
			} else if(guild.isOfficer(infoPlayer.getOfflineBukkitPlayer().getUniqueId())) {
				loreList.add(ChatColor.BLUE+MessagePath.LABEL_OFFICER.getMessage());
			} else if(guild.isMember(infoPlayer.getOfflineBukkitPlayer().getUniqueId())) {
				loreList.add(ChatColor.WHITE+MessagePath.LABEL_MEMBER.getMessage());
			}
			loreList.add(hintColor+MessagePath.MENU_SCORE_HINT.getMessage());
			GuildIcon guildIcon = new GuildIcon(guild, isFriendly, isArmistice, loreList, 3, true);
			getMenu().getPage(0).addIcon(guildIcon);
		}
		/* Player Score Icon (4) */
		int score = getKonquest().getKingdomManager().getPlayerScore(infoPlayer);
		loreList = new ArrayList<String>();
		loreList.add(loreColor+MessagePath.MENU_SCORE_PLAYER_SCORE.getMessage()+": "+valueColor+score);
		loreList.add(hintColor+MessagePath.MENU_SCORE_HINT.getMessage());
		PlayerIcon playerScore = new PlayerIcon(kingdomColor+infoPlayer.getOfflineBukkitPlayer().getName(),loreList,infoPlayer.getOfflineBukkitPlayer(),4,true,PlayerIconAction.DISPLAY_SCORE);
		getMenu().getPage(0).addIcon(playerScore);
		/* Favor Info Icon (5) */
		String balanceF = String.format("%.2f",KonquestPlugin.getBalance(infoPlayer.getOfflineBukkitPlayer()));
		InfoIcon info = new InfoIcon(kingdomColor+MessagePath.LABEL_FAVOR.getMessage(), Arrays.asList(loreColor+MessagePath.LABEL_FAVOR.getMessage()+": "+valueColor+balanceF), Material.GOLD_INGOT, 5, false);
		getMenu().getPage(0).addIcon(info);
		// Page 1+
		List<KonTown> playerTowns = sortedTowns(infoPlayer);
		final int MAX_ICONS_PER_PAGE = 45;
		int pageTotal = (int)Math.ceil(((double)playerTowns.size())/MAX_ICONS_PER_PAGE);
		if(pageTotal == 0) {
			pageTotal = 1;
		}
		int pageNum = 1;
		ListIterator<KonTown> townIter = playerTowns.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int numPageRows = (int)Math.ceil(((double)(playerTowns.size() - i*MAX_ICONS_PER_PAGE))/9);
			if(numPageRows < 1) {
				numPageRows = 1;
			} else if(numPageRows > 5) {
				numPageRows = 5;
			}
			pageLabel = titleColor+infoPlayer.getOfflineBukkitPlayer().getName()+" "+MessagePath.LABEL_RESIDENCIES.getMessage()+" "+(i+1)+"/"+pageTotal;
			getMenu().addPage(pageNum, numPageRows, pageLabel);
			int slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && townIter.hasNext()) {
				/* Town Icon (n) */
				KonTown currentTown = townIter.next();
				loreList = new ArrayList<String>();
				if(currentTown.isPlayerLord(infoPlayer.getOfflineBukkitPlayer())) {
					loreList.add(ChatColor.DARK_PURPLE+MessagePath.LABEL_LORD.getMessage());
				} else if(currentTown.isPlayerElite(infoPlayer.getOfflineBukkitPlayer())) {
					loreList.add(ChatColor.DARK_BLUE+MessagePath.LABEL_KNIGHT.getMessage());
				} else {
					loreList.add(loreColor+MessagePath.LABEL_RESIDENT.getMessage());
				}
		    	loreList.add(loreColor+MessagePath.LABEL_POPULATION.getMessage()+": "+valueColor+currentTown.getNumResidents());
		    	loreList.add(loreColor+MessagePath.LABEL_LAND.getMessage()+": "+valueColor+currentTown.getChunkList().size());
		    	TownIcon town = new TownIcon(currentTown,isFriendly,isArmistice,getKonquest().getKingdomManager().getTownCriticalBlock(),loreList,slotIndex);
		    	getMenu().getPage(pageNum).addIcon(town);
				slotIndex++;
			}
			pageNum++;
		}
		
		getMenu().refreshNavigationButtons();
		getMenu().setPageIndex(0);
	}

	@Override
	public boolean onIconClick(KonPlayer clickPlayer, MenuIcon clickedIcon) {
		boolean result = false;
		Player bukkitPlayer = clickPlayer.getBukkitPlayer();
		if(clickedIcon instanceof InfoIcon) {
			// Info Icons close the GUI and print their info in chat
			InfoIcon icon = (InfoIcon)clickedIcon;
			ChatUtil.sendNotice(bukkitPlayer, icon.getInfo());
			result = true;
		} else if(clickedIcon instanceof PlayerIcon) {
			// Player Head Icons open a new score menu for the associated player
			PlayerIcon icon = (PlayerIcon)clickedIcon;
			KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayer(icon.getOfflinePlayer());
			if(clickPlayer != null && offlinePlayer != null && icon.getAction().equals(PlayerIconAction.DISPLAY_SCORE)) {
				getKonquest().getDisplayManager().displayScoreMenu(clickPlayer, offlinePlayer);
			}
			result = false;
		} else if(clickedIcon instanceof KingdomIcon) {
			// Kingdom Icons open a new kingdom info menu for the associated player
			KingdomIcon icon = (KingdomIcon)clickedIcon;
			getKonquest().getDisplayManager().displayKingdomInfoMenu(clickPlayer,icon.getKingdom());
			result = false;
		} else if(clickedIcon instanceof TownIcon) {
			// Town Icons open a new town info menu for the associated player
			TownIcon icon = (TownIcon)clickedIcon;
			getKonquest().getDisplayManager().displayTownInfoMenu(clickPlayer,icon.getTown());
			result = false;
		} else if(clickedIcon instanceof GuildIcon) {
			// Guild Icons open a new guild info menu for the associated player
			GuildIcon icon = (GuildIcon)clickedIcon;
			getKonquest().getDisplayManager().displayGuildInfoMenu(clickPlayer,icon.getGuild());
			result = false;
		}
		return result;
	}
	
	// Sort player town list by Lord, Knight, Resident, and then by population, and then by size
 	private List<KonTown> sortedTowns(KonOfflinePlayer player) {
 		List<KonTown> sortedTowns = new ArrayList<KonTown>();
 		// Determine town group lists
 		List<KonTown> lordTowns = new ArrayList<KonTown>();
 		List<KonTown> knightTowns = new ArrayList<KonTown>();
 		List<KonTown> residentTowns = new ArrayList<KonTown>();
 		for(KonTown town : getKonquest().getKingdomManager().getPlayerResidenceTowns(player)) {
 			if(town.isPlayerLord(player.getOfflineBukkitPlayer())) {
 				lordTowns.add(town);
 			} else if(town.isPlayerElite(player.getOfflineBukkitPlayer())) {
 				knightTowns.add(town);
 			} else {
 				residentTowns.add(town);
 			}
 		}
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
 		Collections.sort(lordTowns, townComparator);
 		Collections.sort(knightTowns, townComparator);
 		Collections.sort(residentTowns, townComparator);
 		// Add sorted towns to result list
 		sortedTowns.addAll(lordTowns);
 		sortedTowns.addAll(knightTowns);
 		sortedTowns.addAll(residentTowns);
 		
 		return sortedTowns;
 	}

}
