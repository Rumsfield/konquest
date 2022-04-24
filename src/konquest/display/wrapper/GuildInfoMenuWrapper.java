package konquest.display.wrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import konquest.Konquest;
import konquest.display.InfoIcon;
import konquest.display.KingdomIcon;
import konquest.display.MenuIcon;
import konquest.display.PlayerIcon;
import konquest.display.PlayerIcon.PlayerIconAction;
import konquest.manager.DisplayManager;
import konquest.model.KonGuild;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class GuildInfoMenuWrapper extends MenuWrapper {

	private KonGuild infoGuild;
	private KonPlayer observer;
	
	public GuildInfoMenuWrapper(Konquest konquest, KonGuild infoGuild, KonPlayer observer) {
		super(konquest);
		this.infoGuild = infoGuild;
		this.observer = observer;
	}

	@Override
	public void constructMenu() {
		if(infoGuild == null) {
			return;
		}
		// Observer may or may not belong to a guild
		KonGuild observerGuild = getKonquest().getGuildManager().getPlayerGuild(observer.getOfflineBukkitPlayer());
		
		String pageLabel = "";
 		List<String> loreList;
 		InfoIcon info;
 		final int MAX_ICONS_PER_PAGE = 45;
		int pageTotal = 1;

		//TODO: When observerGuild is null, get color by kingdoms
		ChatColor kingdomColor = getKonquest().getDisplayPrimaryColor(observerGuild, infoGuild);
		
 		ChatColor titleColor = DisplayManager.titleColor;
		ChatColor loreColor = DisplayManager.loreColor;
		ChatColor valueColor = DisplayManager.valueColor;
		ChatColor hintColor = DisplayManager.hintColor;
		
		List<OfflinePlayer> guildOfficers = infoGuild.getPlayerOfficersOnly();
		List<OfflinePlayer> guildMembers = infoGuild.getPlayerMembersOnly();
		ListIterator<OfflinePlayer> playerIter;
		
		// Page 0
		pageLabel = titleColor+infoGuild.getName()+" "+MessagePath.LABEL_GUILD.getMessage();
		getMenu().addPage(0, 1, pageLabel);
		/* Kingdom Info Icon (1) */
		int numKingdomPlayers = getKonquest().getPlayerManager().getPlayersInKingdom(infoGuild.getKingdom()).size();
    	int numAllKingdomPlayers = getKonquest().getPlayerManager().getAllPlayersInKingdom(infoGuild.getKingdom()).size();
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_PLAYERS.getMessage()+": "+valueColor+numKingdomPlayers+"/"+numAllKingdomPlayers);
    	if(infoGuild.getKingdom().isOfflineProtected()) {
    		loreList.add(ChatColor.LIGHT_PURPLE+""+ChatColor.ITALIC+MessagePath.LABEL_PROTECTED.getMessage());
    	}
    	loreList.add(ChatColor.GOLD+MessagePath.MENU_SCORE_HINT.getMessage());
    	KingdomIcon kingdom = new KingdomIcon(infoGuild.getKingdom(),kingdomColor,Material.GOLDEN_HELMET,loreList,1);
    	getMenu().getPage(0).addIcon(kingdom);
		/* Master Player Info Icon (2) */
		loreList = new ArrayList<String>();
		OfflinePlayer masterPlayer = infoGuild.getPlayerMaster();
		loreList.add(ChatColor.LIGHT_PURPLE+MessagePath.LABEL_MASTER.getMessage());
		loreList.add(ChatColor.GOLD+MessagePath.MENU_SCORE_HINT.getMessage());
		PlayerIcon playerInfo = new PlayerIcon(kingdomColor+masterPlayer.getName(),loreList,masterPlayer,2,true,PlayerIconAction.DISPLAY_INFO);
		getMenu().getPage(0).addIcon(playerInfo);
		/* Properties Info Icon (3) */
    	String isOpen = DisplayManager.boolean2Symbol(infoGuild.isOpen());
    	String special = infoGuild.getSpecialization().name();
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_OPEN.getMessage()+": "+isOpen);
    	loreList.add(loreColor+MessagePath.LABEL_SPECIALIZATION.getMessage()+": "+valueColor+special);
    	if(observerGuild != null) {
    		// Include relationship info with observer's guild
			if(!observerGuild.getKingdom().equals(infoGuild.getKingdom())) {
				String theirEnemyStatus = infoGuild.isArmistice(observerGuild) ? MessagePath.LABEL_ARMISTICE.getMessage() : MessagePath.LABEL_HOSTILE.getMessage();
				loreList.add(loreColor+MessagePath.MENU_GUILD_THEIR_STATUS.getMessage()+": "+valueColor+theirEnemyStatus);
				String guildEnemyStatus = observerGuild.isArmistice(infoGuild) ? MessagePath.LABEL_ARMISTICE.getMessage() : MessagePath.LABEL_HOSTILE.getMessage();
				loreList.add(loreColor+MessagePath.MENU_GUILD_OUR_STATUS.getMessage()+": "+valueColor+guildEnemyStatus);
			} else {
				String theirFriendlyStatus = infoGuild.isSanction(observerGuild) ? MessagePath.LABEL_SANCTION.getMessage() : MessagePath.LABEL_TREATY.getMessage();
				loreList.add(loreColor+MessagePath.MENU_GUILD_THEIR_STATUS.getMessage()+": "+valueColor+theirFriendlyStatus);
				String guildFriendlyStatus = observerGuild.isSanction(infoGuild) ? MessagePath.LABEL_SANCTION.getMessage() : MessagePath.LABEL_TREATY.getMessage();
				loreList.add(loreColor+MessagePath.MENU_GUILD_OUR_STATUS.getMessage()+": "+valueColor+guildFriendlyStatus);
			}
		}
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_PROPERTIES.getMessage(), loreList, Material.PAPER, 3, false);
    	getMenu().getPage(0).addIcon(info);
    	/* Towns Info Icon (5) */
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+infoGuild.getNumTowns());
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_TOWNS.getMessage(), loreList, Material.OBSIDIAN, 5, false);
    	getMenu().getPage(0).addIcon(info);
    	/* Land Info Icon (6) */
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL.getMessage()+": "+valueColor+infoGuild.getNumLand());
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_LAND.getMessage(), loreList, Material.GRASS_BLOCK, 6, false);
    	getMenu().getPage(0).addIcon(info);
    	/* Members Info Icon (7) */
    	loreList = new ArrayList<String>();
    	loreList.add(loreColor+MessagePath.LABEL_ONLINE_PLAYERS.getMessage()+": "+valueColor+infoGuild.getNumMembersOnline());
    	loreList.add(loreColor+MessagePath.LABEL_TOTAL_PLAYERS.getMessage()+": "+valueColor+infoGuild.getNumMembers());
    	info = new InfoIcon(kingdomColor+MessagePath.LABEL_MEMBERS.getMessage(), loreList, Material.BROWN_BED, 7, false);
    	getMenu().getPage(0).addIcon(info);
    	
    	// Page 1+
		int pageNum = 1;
		pageTotal = (int)Math.ceil(((double)guildOfficers.size())/MAX_ICONS_PER_PAGE);
		if(pageTotal == 0) {
			pageTotal = 1;
		}
		playerIter = guildOfficers.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int numPageRows = (int)Math.ceil(((double)(guildOfficers.size() - i*MAX_ICONS_PER_PAGE))/9);
			if(numPageRows < 1) {
				numPageRows = 1;
			} else if(numPageRows > 5) {
				numPageRows = 5;
			}
			pageLabel = titleColor+infoGuild.getName()+" "+MessagePath.LABEL_OFFICERS.getMessage()+" "+(i+1)+"/"+pageTotal;
			getMenu().addPage(pageNum, numPageRows, pageLabel);
			int slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && playerIter.hasNext()) {
				/* Player Icon (n) */
				OfflinePlayer currentOfficer = playerIter.next();
				loreList = new ArrayList<String>();
				loreList.add(ChatColor.BLUE+MessagePath.LABEL_OFFICER.getMessage());
		    	loreList.add(hintColor+MessagePath.MENU_SCORE_HINT.getMessage());
		    	PlayerIcon player = new PlayerIcon(kingdomColor+currentOfficer.getName(),loreList,currentOfficer,slotIndex,true,PlayerIconAction.DISPLAY_INFO);
		    	getMenu().getPage(pageNum).addIcon(player);
				slotIndex++;
			}
			pageNum++;
		}
		
		// Page 2+
		pageTotal = (int)Math.ceil(((double)guildMembers.size())/MAX_ICONS_PER_PAGE);
		if(pageTotal == 0) {
			pageTotal = 1;
		}
		playerIter = guildMembers.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int numPageRows = (int)Math.ceil(((double)(guildMembers.size() - i*MAX_ICONS_PER_PAGE))/9);
			if(numPageRows < 1) {
				numPageRows = 1;
			} else if(numPageRows > 5) {
				numPageRows = 5;
			}
			pageLabel = titleColor+infoGuild.getName()+" "+MessagePath.LABEL_MEMBERS.getMessage()+" "+(i+1)+"/"+pageTotal;
			getMenu().addPage(pageNum, numPageRows, pageLabel);
			int slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && playerIter.hasNext()) {
				/* Player Icon (n) */
				OfflinePlayer currentMember = playerIter.next();
				loreList = new ArrayList<String>();
				loreList.add(ChatColor.WHITE+MessagePath.LABEL_MEMBER.getMessage());
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
		} else if(clickedIcon instanceof KingdomIcon) {
			// Kingdom Icons open a new kingdom info menu for the associated player
			KingdomIcon icon = (KingdomIcon)clickedIcon;
			getKonquest().getDisplayManager().displayKingdomInfoMenu(clickPlayer,icon.getKingdom());
		} else if(clickedIcon instanceof PlayerIcon) {
			// Player Head Icons open a new info menu for the associated player
			PlayerIcon icon = (PlayerIcon)clickedIcon;
			KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayer(icon.getOfflinePlayer());
			if(clickPlayer != null && offlinePlayer != null && icon.getAction().equals(PlayerIconAction.DISPLAY_INFO)) {
				getKonquest().getDisplayManager().displayPlayerInfoMenu(clickPlayer, offlinePlayer);
			}
		}
	}

}
