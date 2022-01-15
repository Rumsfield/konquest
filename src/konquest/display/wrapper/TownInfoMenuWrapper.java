package konquest.display.wrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.display.GuildIcon;
import konquest.display.InfoIcon;
import konquest.display.KingdomIcon;
import konquest.display.MenuIcon;
import konquest.display.PlayerIcon;
import konquest.display.PlayerIcon.PlayerIconAction;
import konquest.manager.DisplayManager;
import konquest.model.KonGuild;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonTown;
import konquest.model.KonUpgrade;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class TownInfoMenuWrapper extends MenuWrapper {

	private KonTown infoTown;
	private KonPlayer observer;
	
	public TownInfoMenuWrapper(Konquest konquest, KonTown infoTown, KonPlayer observer) {
		super(konquest);
		this.infoTown = infoTown;
		this.observer = observer;
	}

	@Override
	public void constructMenu() {

		String pageLabel = "";
 		List<String> loreList;
 		InfoIcon info;
 		final int MAX_ICONS_PER_PAGE = 45;
		int pageTotal = 1;
		boolean isFriendly = observer.getKingdom().equals(infoTown.getKingdom());
		boolean isArmistice = getKonquest().getGuildManager().isArmistice(observer, infoTown);
		
		ChatColor kingdomColor = Konquest.getDisplayPrimaryColor(observer, infoTown, isArmistice);
 		ChatColor titleColor = DisplayManager.titleColor;
		ChatColor loreColor = DisplayManager.loreColor;
		ChatColor valueColor = DisplayManager.valueColor;
		ChatColor hintColor = DisplayManager.hintColor;
		
		List<OfflinePlayer> townKnights = new ArrayList<OfflinePlayer>();
		List<OfflinePlayer> townResidents = new ArrayList<OfflinePlayer>();
		for(OfflinePlayer resident : infoTown.getPlayerResidents()) {
			if(!infoTown.isPlayerLord(resident)) {
				if(infoTown.isPlayerElite(resident)) {
					townKnights.add(resident);
				} else {
					townResidents.add(resident);
				}
			}
		}

 		// Page 0
		pageLabel = titleColor+MessagePath.COMMAND_INFO_NOTICE_TOWN_HEADER.getMessage(infoTown.getName());
		getMenu().addPage(0, 1, pageLabel);
		/* Kingdom Info Icon (0) */
		int numKingdomPlayers = getKonquest().getPlayerManager().getPlayersInKingdom(infoTown.getKingdom()).size();
    	int numAllKingdomPlayers = getKonquest().getPlayerManager().getAllPlayersInKingdom(infoTown.getKingdom()).size();
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_PLAYERS.getMessage()+": "+valueColor+numKingdomPlayers+"/"+numAllKingdomPlayers);
    	if(infoTown.getKingdom().isOfflineProtected()) {
    		loreList.add(ChatColor.LIGHT_PURPLE+""+ChatColor.ITALIC+MessagePath.LABEL_PROTECTED.getMessage());
    	}
    	loreList.add(hintColor+MessagePath.MENU_SCORE_HINT.getMessage());
    	KingdomIcon kingdom = new KingdomIcon(infoTown.getKingdom(),kingdomColor,Material.GOLDEN_HELMET,loreList,0);
    	getMenu().getPage(0).addIcon(kingdom);
    	/* Guild Icon (1) */
		KonGuild guild = getKonquest().getGuildManager().getTownGuild(infoTown);
		if(guild != null) {
			loreList = new ArrayList<String>();
			loreList.add(hintColor+MessagePath.MENU_SCORE_HINT.getMessage());
			GuildIcon guildIcon = new GuildIcon(guild, isFriendly, isArmistice, loreList, 1, true);
			getMenu().getPage(0).addIcon(guildIcon);
		}
		/* Lord Player Info Icon (2) */
		loreList = new ArrayList<String>();
		if(infoTown.isLordValid()) {
			OfflinePlayer lordPlayer = infoTown.getPlayerLord();
			loreList.add(loreColor+MessagePath.LABEL_INFORMATION.getMessage());
			loreList.add(ChatColor.DARK_PURPLE+MessagePath.LABEL_LORD.getMessage());
			loreList.add(hintColor+MessagePath.MENU_SCORE_HINT.getMessage());
			PlayerIcon playerInfo = new PlayerIcon(kingdomColor+lordPlayer.getName(),loreList,lordPlayer,2,true,PlayerIconAction.DISPLAY_INFO);
			getMenu().getPage(0).addIcon(playerInfo);
		} else {
			for(String line : Konquest.stringPaginate(MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(infoTown.getName(), infoTown.getName(), observer.getBukkitPlayer().getName()))) {
				loreList.add(ChatColor.RED+line);
			}
			info = new InfoIcon(ChatColor.DARK_PURPLE+MessagePath.LABEL_LORD.getMessage(),loreList,Material.BARRIER,2,false);
			getMenu().getPage(0).addIcon(info);
		}
		/* Invites Info Icon (3) */
		loreList = new ArrayList<String>();
		for(OfflinePlayer invitee : infoTown.getJoinInvites()) {
			loreList.add(loreColor+invitee.getName());
		}
		info = new InfoIcon(kingdomColor+MessagePath.LABEL_INVITES.getMessage(),loreList,Material.DIAMOND,3,false);
		getMenu().getPage(0).addIcon(info);
		/* Requests Info Icon (4) */
		loreList = new ArrayList<String>();
		for(OfflinePlayer requestee : infoTown.getJoinRequests()) {
			loreList.add(loreColor+requestee.getName());
		}
		info = new InfoIcon(kingdomColor+MessagePath.LABEL_REQUESTS.getMessage(),loreList,Material.EMERALD,4,false);
		getMenu().getPage(0).addIcon(info);
		/* Properties Info Icon (5) */
    	String isOpen = DisplayManager.boolean2Symbol(infoTown.isOpen());
    	String isPlotOnly = DisplayManager.boolean2Symbol(infoTown.isPlotOnly());
    	String isRedstone = DisplayManager.boolean2Symbol(infoTown.isEnemyRedstoneAllowed());
    	String isProtected = DisplayManager.boolean2Symbol((infoTown.isCaptureDisabled() || infoTown.getKingdom().isOfflineProtected() || infoTown.isTownWatchProtected()));
    	String isAttacked = DisplayManager.boolean2Symbol(infoTown.isAttacked());
    	String isShielded = DisplayManager.boolean2Symbol(infoTown.isShielded());
    	String isArmored = DisplayManager.boolean2Symbol(infoTown.isArmored());
    	String isPeaceful = DisplayManager.boolean2Symbol(infoTown.getKingdom().isPeaceful());
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_OPEN.getMessage()+": "+isOpen);
    	loreList.add(loreColor+MessagePath.LABEL_PLOT.getMessage()+": "+isPlotOnly);
    	loreList.add(loreColor+MessagePath.LABEL_ENEMY_REDSTONE.getMessage()+": "+isRedstone);
    	loreList.add(loreColor+MessagePath.PROTECTION_NOTICE_ATTACKED.getMessage()+": "+isAttacked);
    	loreList.add(loreColor+MessagePath.LABEL_PEACEFUL.getMessage()+": "+isPeaceful);
    	loreList.add(loreColor+MessagePath.LABEL_SHIELD.getMessage()+": "+isShielded);
    	loreList.add(loreColor+MessagePath.LABEL_ARMOR.getMessage()+": "+isArmored);
    	loreList.add(loreColor+MessagePath.LABEL_PROTECTED.getMessage()+": "+isProtected);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_PROPERTIES.getMessage(), loreList, Material.PAPER, 5, false);
    	getMenu().getPage(0).addIcon(info);
    	/* Health Info Icon (6) */
    	int maxCriticalhits = getKonquest().getConfigManager().getConfig("core").getInt("core.monuments.destroy_amount");
		int townHealth = maxCriticalhits - infoTown.getMonument().getCriticalHits();
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+townHealth+"/"+maxCriticalhits);
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_HEALTH.getMessage(), loreList, Material.GOLDEN_APPLE, 6, false);
    	getMenu().getPage(0).addIcon(info);
    	/* Land Info Icon (7) */
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+infoTown.getChunkList().size());
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_LAND.getMessage(), loreList, Material.GRASS_BLOCK, 7, false);
    	getMenu().getPage(0).addIcon(info);
    	/* Population Info Icon (8) */
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+infoTown.getNumResidents());
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_POPULATION.getMessage(), loreList, Material.WHITE_BED, 8, false);
    	getMenu().getPage(0).addIcon(info);
    	
    	// Page 1
		pageLabel = titleColor+infoTown.getName()+" "+MessagePath.LABEL_UPGRADES.getMessage();
		getMenu().addPage(1, 1, pageLabel);
		int index = 0;
		for(KonUpgrade upgrade : KonUpgrade.values()) {
			int currentLevel = infoTown.getRawUpgradeLevel(upgrade);
			if(currentLevel > 0) {
				String formattedUpgrade = ChatColor.LIGHT_PURPLE+upgrade.getDescription()+" "+currentLevel;
				int level = currentLevel;
				if(infoTown.isUpgradeDisabled(upgrade)) {
					int reducedLevel = infoTown.getUpgradeLevel(upgrade);
					level = reducedLevel;
					if(reducedLevel > 0) {
						formattedUpgrade = ChatColor.LIGHT_PURPLE+upgrade.getDescription()+" "+ChatColor.GRAY+reducedLevel;
					} else {
						formattedUpgrade = ChatColor.GRAY+""+ChatColor.STRIKETHROUGH+upgrade.getDescription()+" "+reducedLevel;
					}
				}
				loreList = new ArrayList<String>();
				for(String line : Konquest.stringPaginate(upgrade.getLevelDescription(level))) {
					loreList.add(ChatColor.RED+line);
				}
				// Create info icon with upgrade info
				info = new InfoIcon(formattedUpgrade, loreList, upgrade.getIcon(), index, false);
				getMenu().getPage(1).addIcon(info);
				index++;
			}
		}

		// Page 2+
		int pageNum = 2;
		pageTotal = (int)Math.ceil(((double)townKnights.size())/MAX_ICONS_PER_PAGE);
		if(pageTotal == 0) {
			pageTotal = 1;
		}
		ListIterator<OfflinePlayer> knightIter = townKnights.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int numPageRows = (int)Math.ceil(((double)(townKnights.size() - i*MAX_ICONS_PER_PAGE))/9);
			if(numPageRows < 1) {
				numPageRows = 1;
			} else if(numPageRows > 5) {
				numPageRows = 5;
			}
			pageLabel = titleColor+infoTown.getName()+" "+MessagePath.LABEL_KNIGHTS.getMessage()+" "+(i+1)+"/"+pageTotal;
			getMenu().addPage(pageNum, numPageRows, pageLabel);
			int slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && knightIter.hasNext()) {
				/* Player Icon (n) */
				OfflinePlayer currentKnight = knightIter.next();
				loreList = new ArrayList<String>();
				loreList.add(loreColor+MessagePath.LABEL_INFORMATION.getMessage());
				loreList.add(ChatColor.DARK_BLUE+MessagePath.LABEL_KNIGHT.getMessage());
		    	loreList.add(hintColor+MessagePath.MENU_SCORE_HINT.getMessage());
		    	PlayerIcon player = new PlayerIcon(kingdomColor+currentKnight.getName(),loreList,currentKnight,slotIndex,true,PlayerIconAction.DISPLAY_INFO);
		    	getMenu().getPage(pageNum).addIcon(player);
				slotIndex++;
			}
			pageNum++;
		}
		
		// Page 3+
		pageTotal = (int)Math.ceil(((double)townResidents.size())/MAX_ICONS_PER_PAGE);
		if(pageTotal == 0) {
			pageTotal = 1;
		}
		ListIterator<OfflinePlayer> residentIter = townResidents.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int numPageRows = (int)Math.ceil(((double)(townResidents.size() - i*MAX_ICONS_PER_PAGE))/9);
			if(numPageRows < 1) {
				numPageRows = 1;
			} else if(numPageRows > 5) {
				numPageRows = 5;
			}
			pageLabel = titleColor+infoTown.getName()+" "+MessagePath.LABEL_RESIDENTS.getMessage()+" "+(i+1)+"/"+pageTotal;
			getMenu().addPage(pageNum, numPageRows, pageLabel);
			int slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && residentIter.hasNext()) {
				/* Player Icon (n) */
				OfflinePlayer currentResident = residentIter.next();
				loreList = new ArrayList<String>();
				loreList.add(loreColor+MessagePath.LABEL_INFORMATION.getMessage());
				loreList.add(ChatColor.WHITE+MessagePath.LABEL_RESIDENT.getMessage());
		    	loreList.add(hintColor+MessagePath.MENU_SCORE_HINT.getMessage());
		    	PlayerIcon player = new PlayerIcon(kingdomColor+currentResident.getName(),loreList,currentResident,slotIndex,true,PlayerIconAction.DISPLAY_INFO);
		    	getMenu().getPage(pageNum).addIcon(player);
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
		} else if(clickedIcon instanceof KingdomIcon) {
			// Kingdom Icons open a new kingdom info menu for the associated player
			KingdomIcon icon = (KingdomIcon)clickedIcon;
			getKonquest().getDisplayManager().displayKingdomInfoMenu(clickPlayer,icon.getKingdom());
			result = false;
		} else if(clickedIcon instanceof PlayerIcon) {
			// Player Head Icons open a new info menu for the associated player
			PlayerIcon icon = (PlayerIcon)clickedIcon;
			KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayer(icon.getOfflinePlayer());
			if(clickPlayer != null && offlinePlayer != null && icon.getAction().equals(PlayerIconAction.DISPLAY_INFO)) {
				getKonquest().getDisplayManager().displayPlayerInfoMenu(clickPlayer, offlinePlayer);
			}
			result = false;
		} else if(clickedIcon instanceof GuildIcon) {
			// Guild Icons open a new guild info menu for the associated player
			GuildIcon icon = (GuildIcon)clickedIcon;
			getKonquest().getDisplayManager().displayGuildInfoMenu(clickPlayer,icon.getGuild());
			result = false;
		}
		return result;
	}

}
