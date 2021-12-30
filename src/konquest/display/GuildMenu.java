package konquest.display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Villager;

import konquest.Konquest;
import konquest.display.PlayerIcon.PlayerIconAction;
import konquest.manager.DisplayManager;
import konquest.manager.GuildManager;
import konquest.model.KonGuild;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

public class GuildMenu implements StateMenu {

	/*
	 * State Views
	 * Regular players: A_*
	 * Officer players: B_*
	 * Master players: C_*
	 */
	enum MenuState {
		ROOT,
		A_JOIN,
		A_LEAVE,
		A_INVITE,
		A_LIST,
		B_RELATIONSHIP,
		B_REQUESTS,
		C_PROMOTE,
		C_DEMOTE,
		C_TRANSFER,
		C_SPECIALIZE,
		C_DISBAND;
	}
	
	enum AccessType {
		REGULAR,
		OFFICER,
		MASTER;
	}
	
	private final int ROOT_SLOT_JOIN 			= 0;
	private final int ROOT_SLOT_LEAVE 			= 2;
	private final int ROOT_SLOT_GUILD 			= 4;
	private final int ROOT_SLOT_INVITE 			= 6;
	private final int ROOT_SLOT_LIST 			= 8;
	private final int ROOT_SLOT_RELATIONSHIPS 	= 12;
	private final int ROOT_SLOT_REQUESTS 		= 14;
	private final int ROOT_SLOT_PROMOTE 		= 19;
	private final int ROOT_SLOT_DEMOTE 			= 20;
	private final int ROOT_SLOT_TRANSFER		= 21;
	private final int ROOT_SLOT_SPECIALIZE 		= 23;
	private final int ROOT_SLOT_OPEN 			= 24;
	private final int ROOT_SLOT_DISBAND			= 25;
	
	private final int SLOT_YES 					= 3;
	private final int SLOT_NO 					= 5;
	
	private final ChatColor loreColor = ChatColor.YELLOW;
	private final ChatColor valueColor = ChatColor.AQUA;
	private final ChatColor hintColor = ChatColor.GOLD;
	
	private HashMap<MenuState,DisplayMenu> views;
	private ArrayList<DisplayMenu> pages;
	private int currentPage;
	private MenuState currentState;
	private GuildManager manager;
	private KonPlayer player;
	private KonGuild guild;
	private AccessType menuAccess;
	
	public GuildMenu(GuildManager manager, KonPlayer player, KonGuild guild) {
		this.views = new HashMap<MenuState,DisplayMenu>();
		this.pages = new ArrayList<DisplayMenu>();
		this.currentPage = 0;
		this.currentState = MenuState.ROOT;
		this.manager = manager;
		this.player = player;
		this.guild = guild;
		this.menuAccess = AccessType.REGULAR;
		
		initializeMenu();
		renderDefaultViews();
	}
	
	
	private void initializeMenu() {
		if(guild != null) {
			UUID id = player.getBukkitPlayer().getUniqueId();
			if(guild.isMaster(id)) {
				menuAccess = AccessType.MASTER;
			} else if(guild.isOfficer(id)) {
				menuAccess = AccessType.OFFICER;
			} else if(guild.isMember(id)) {
				menuAccess = AccessType.REGULAR;
			}
		}
	}
	
	private void renderDefaultViews() {
		DisplayMenu renderView;
		
		/* Root View */
		renderView = createRootView();
		views.put(MenuState.ROOT, renderView);
		refreshNavigationButtons(MenuState.ROOT);
		
		/* Leave View */
		renderView = createLeaveView();
		views.put(MenuState.A_LEAVE, renderView);
		refreshNavigationButtons(MenuState.A_LEAVE);
		
		/* Disband View */
		renderView = createDisbandView();
		views.put(MenuState.C_DISBAND, renderView);
		refreshNavigationButtons(MenuState.C_DISBAND);
		
		/* Specialize View */
		renderView = createSpecializeView();
		views.put(MenuState.C_SPECIALIZE, renderView);
		refreshNavigationButtons(MenuState.C_SPECIALIZE);
	}
	
	private DisplayMenu createRootView() {
		DisplayMenu result;
		MenuIcon icon;
		List<String> loreList = new ArrayList<String>();
		ChatColor regularColor = ChatColor.GREEN;
		ChatColor officerColor = ChatColor.BLUE;
		ChatColor masterColor = ChatColor.LIGHT_PURPLE;
		
		int rows = 2;
		switch(menuAccess) {
			case REGULAR:
				rows = 2;
				break;
			case OFFICER:
				rows = 3;
				break;
			case MASTER:
				rows = 4;
				break;
			default:
				break;
		}
		
		result = new DisplayMenu(rows, getTitle(MenuState.ROOT));
		
		loreList.clear();
		loreList.add(loreColor+"Request to join a guild");
		icon = new InfoIcon(regularColor+"Join", loreList, Material.SADDLE, ROOT_SLOT_JOIN, true);
		result.addIcon(icon);
		
		loreList.clear();
		loreList.add(loreColor+"Leave your current guild");
		icon = new InfoIcon(regularColor+"Leave", loreList, Material.ARROW, ROOT_SLOT_LEAVE, true);
		result.addIcon(icon);

		loreList.clear();
		loreList.add(loreColor+"View your guild invites");
		int numInvites = manager.getInviteGuilds(player).size();
		if(numInvites > 0) {
			loreList.add(valueColor+""+numInvites);
		}
		icon = new InfoIcon(regularColor+"Invites", loreList, Material.WRITABLE_BOOK, ROOT_SLOT_INVITE, true);
		result.addIcon(icon);
		
		loreList.clear();
		loreList.add(loreColor+"View all guilds");
		icon = new InfoIcon(regularColor+"List", loreList, Material.LECTERN, ROOT_SLOT_LIST, true);
		result.addIcon(icon);
		
		if(guild != null) {
			loreList = new ArrayList<String>();
			loreList.add(loreColor+"Towns: "+valueColor+guild.getNumTowns());
			loreList.add(loreColor+"Land: "+valueColor+guild.getNumLand());
			loreList.add(loreColor+"Members: "+valueColor+guild.getNumMembers());
			loreList.add(loreColor+"Specialization: "+valueColor+guild.getSpecialization().name());
			icon = new GuildIcon(regularColor+guild.getName()+" Guild",loreList,guild,ROOT_SLOT_GUILD,false);
			result.addIcon(icon);

			if(menuAccess.equals(AccessType.OFFICER) || menuAccess.equals(AccessType.MASTER)) {
				loreList = new ArrayList<String>();
				loreList.add(loreColor+"Modify guild status");
				icon = new InfoIcon(officerColor+"Relationships", loreList, Material.GOLDEN_SWORD, ROOT_SLOT_RELATIONSHIPS, true);
				result.addIcon(icon);
				
				loreList.clear();
				loreList.add(loreColor+"View guild membership requests");
				int numRequests = guild.getJoinRequests().size();
				if(numRequests > 0) {
					loreList.add(valueColor+""+numRequests);
				}
				icon = new InfoIcon(officerColor+"Requests", loreList, Material.JUKEBOX, ROOT_SLOT_REQUESTS, true);
				result.addIcon(icon);
			}
			
			if(menuAccess.equals(AccessType.MASTER)) {
				loreList = new ArrayList<String>();
				loreList.add(loreColor+"Promote members to officers");
				icon = new InfoIcon(masterColor+"Promote", loreList, Material.DIAMOND_HORSE_ARMOR, ROOT_SLOT_PROMOTE, true);
				result.addIcon(icon);
				
				loreList.clear();
				loreList.add(loreColor+"Demote officers to members");
				icon = new InfoIcon(masterColor+"Demote", loreList, Material.LEATHER_HORSE_ARMOR, ROOT_SLOT_DEMOTE, true);
				result.addIcon(icon);
				
				loreList.clear();
				loreList.add(loreColor+"Transfer master to another member");
				icon = new InfoIcon(masterColor+"Transfer", loreList, Material.ELYTRA, ROOT_SLOT_TRANSFER, true);
				result.addIcon(icon);
				
				loreList.clear();
				loreList.add(loreColor+"Change guild specialization");
				icon = new InfoIcon(masterColor+"Specialization", loreList, Material.SMITHING_TABLE, ROOT_SLOT_SPECIALIZE, true);
				result.addIcon(icon);
				
				/* Open/Close Button */
				String currentValue = DisplayManager.boolean2Lang(guild.isOpen())+" "+DisplayManager.boolean2Symbol(guild.isOpen());
				loreList.clear();
		    	loreList.add(loreColor+MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor+currentValue));
		    	loreList.add(hintColor+MessagePath.MENU_OPTIONS_HINT.getMessage());
				icon = new InfoIcon(masterColor+"Open/Close", loreList, Material.IRON_DOOR, ROOT_SLOT_OPEN, true);
				result.addIcon(icon);
				
				loreList.clear();
				loreList.add(loreColor+"Disband and delete your guild");
				icon = new InfoIcon(masterColor+"Disband", loreList, Material.CREEPER_HEAD, ROOT_SLOT_DISBAND, true);
				result.addIcon(icon);
			}
		}
		
		return result;
	}
	
	private DisplayMenu createLeaveView() {
		DisplayMenu result;
		InfoIcon icon;
		List<String> loreList = new ArrayList<String>();
		result = new DisplayMenu(2, getTitle(MenuState.A_LEAVE));
		
		loreList.clear();
		loreList.add(loreColor+"Click to leave your guild");
		icon = new InfoIcon(ChatColor.GOLD+"Yes", loreList, Material.GLOWSTONE_DUST, SLOT_YES, true);
		result.addIcon(icon);
		
		loreList.clear();
		loreList.add(loreColor+"Exit the menu");
		icon = new InfoIcon(ChatColor.GOLD+"No", loreList, Material.REDSTONE, SLOT_NO, true);
		result.addIcon(icon);
		return result;
	}
	
	private DisplayMenu createDisbandView() {
		DisplayMenu result;
		InfoIcon icon;
		List<String> loreList = new ArrayList<String>();
		result = new DisplayMenu(2, getTitle(MenuState.C_DISBAND));
		
		loreList.clear();
		loreList.add(hintColor+"Click to delete your guild");
		icon = new InfoIcon(ChatColor.GOLD+"Yes", loreList, Material.GLOWSTONE_DUST, SLOT_YES, true);
		result.addIcon(icon);
		
		loreList.clear();
		loreList.add(hintColor+"Exit the menu");
		icon = new InfoIcon(ChatColor.GOLD+"No", loreList, Material.REDSTONE, SLOT_NO, true);
		result.addIcon(icon);
		return result;
	}
	
	private DisplayMenu createSpecializeView() {
		DisplayMenu result;
		ProfessionIcon icon;
		String cost = String.format("%.2f",manager.getCostSpecial());
		int numEntries = Villager.Profession.values().length - 1; // Subtract one to omit current specialization choice
		int numRows = (int)Math.ceil((double)numEntries / 9);
		result = new DisplayMenu(numRows+1, getTitle(MenuState.C_SPECIALIZE));
		int index = 0;
		List<String> loreList = new ArrayList<String>();
		loreList.add(loreColor+"Cost: "+valueColor+cost);
		loreList.add(hintColor+"Click to choose");
		for(Villager.Profession profession : Villager.Profession.values()) {
			if(guild == null || (guild != null && !profession.equals(guild.getSpecialization()))) {
				icon = new ProfessionIcon(ChatColor.GOLD+profession.name(),loreList,profession,index,true);
				result.addIcon(icon);
				index++;
			}
		}
		return result;
	}
	
	private DisplayMenu createGuildView(MenuState context) {
		// A paged view of guilds, with lore based on context
		DisplayMenu result = null;
		final int MAX_ICONS_PER_PAGE = 45;
		pages.clear();
		currentPage = 0;
		String loreHintStr1 = "";
		String loreHintStr2 = "";
		boolean isClickable = false;
		List<KonGuild> guilds = new ArrayList<KonGuild>();
		
		// Determine list of guilds given context
		if(context.equals(MenuState.A_JOIN)) {
			// List of all friendly guilds able to join (sends request)
			guilds.addAll(manager.getKingdomGuilds(player.getKingdom()));
			if(guild != null) {
				guilds.remove(guild);
			}
			loreHintStr1 = "Click send a join request";
			isClickable = true;
		} else if(context.equals(MenuState.A_INVITE)) {
			// List of friendly guilds with valid join invite for player
			guilds.addAll(manager.getInviteGuilds(player));
			if(guild != null) {
				guilds.remove(guild);
			}
			loreHintStr1 = "Left-click to accept";
			loreHintStr2 = "Right-click to decline";
			isClickable = true;
		} else if(context.equals(MenuState.A_LIST)) {
			// List of all guilds, friendly and enemy, with normal info
			guilds.addAll(manager.getAllGuilds());
			isClickable = false;
		} else if(context.equals(MenuState.B_RELATIONSHIP)) {
			// List of all guilds, friendly and enemy, with relationship status and click hints
			guilds.addAll(manager.getAllGuilds());
			if(guild != null) {
				guilds.remove(guild);
			}
			loreHintStr1 = "Click to change our status";
			isClickable = true;
		} else {
			return null;
		}
		
		// Create page(s)
		String pageLabel = "";
		List<String> loreList;
		int pageTotal = (int)Math.ceil(((double)guilds.size())/MAX_ICONS_PER_PAGE);
		if(pageTotal == 0) {
			pageTotal = 1;
		}
		int pageNum = 0;
		ListIterator<KonGuild> listIter = guilds.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int numPageRows = (int)Math.ceil(((double)(guilds.size() - i*MAX_ICONS_PER_PAGE))/9);
			if(numPageRows < 1) {
				numPageRows = 1;
			} else if(numPageRows > 5) {
				numPageRows = 5;
			}
			pageLabel = getTitle(context)+" "+(i+1)+"/"+pageTotal;
			pages.add(pageNum, new DisplayMenu(numPageRows+1, pageLabel));
			int slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && listIter.hasNext()) {
				/* Guild Icon (n) */
				KonGuild currentGuild = listIter.next();
				ChatColor guildColor = ChatColor.GREEN;
				loreList = new ArrayList<String>();
				loreList.add(loreColor+"Towns: "+valueColor+currentGuild.getNumTowns());
				loreList.add(loreColor+"Land: "+valueColor+currentGuild.getNumLand());
				loreList.add(loreColor+"Members: "+valueColor+currentGuild.getNumMembers());
				loreList.add(loreColor+"Specialization: "+valueColor+currentGuild.getSpecialization().name());
				if(guild != null) {
					if(!player.getKingdom().equals(currentGuild.getKingdom())) {
						guildColor = ChatColor.RED;
						String theirEnemyStatus = currentGuild.isArmistice(guild) ? "Armistice" : "Hostile";
						loreList.add(loreColor+"Their Status: "+valueColor+theirEnemyStatus);
						String guildEnemyStatus = guild.isArmistice(currentGuild) ? "Armistice" : "Hostile";
						loreList.add(loreColor+"Our Status: "+valueColor+guildEnemyStatus);
					} else {
						String theirFriendlyStatus = currentGuild.isSanction(guild) ? "Sanction" : "Treaty";
						loreList.add(loreColor+"Their Status: "+valueColor+theirFriendlyStatus);
						String guildFriendlyStatus = guild.isSanction(currentGuild) ? "Sanction" : "Treaty";
						loreList.add(loreColor+"Our Status: "+valueColor+guildFriendlyStatus);
					}
					if(manager.isArmistice(guild, currentGuild)) {
						guildColor = ChatColor.LIGHT_PURPLE;
					}
					if(context.equals(MenuState.B_RELATIONSHIP)) {
						String cost = String.format("%.2f",manager.getCostRelation());
						loreList.add(loreColor+"Cost: "+valueColor+cost);
					}
				}
				if(!loreHintStr1.equals("")) {
					loreList.add(hintColor+loreHintStr1);
				}
				if(!loreHintStr2.equals("")) {
					loreList.add(hintColor+loreHintStr2);
				}
		    	GuildIcon guildIcon = new GuildIcon(guildColor+currentGuild.getName(),loreList,currentGuild,slotIndex,isClickable);
		    	pages.get(pageNum).addIcon(guildIcon);
				slotIndex++;
			}
			pageNum++;
		}
		result = pages.get(currentPage);
		return result;
	}
	
	private DisplayMenu createPlayerView(MenuState context) {
		// A paged view of players, with lore based on context
		DisplayMenu result = null;
		final int MAX_ICONS_PER_PAGE = 45;
		pages.clear();
		currentPage = 0;
		String loreHintStr1 = "";
		String loreHintStr2 = "";
		PlayerIconAction iconAction = PlayerIconAction.GUILD;
		boolean isClickable = false;
		List<OfflinePlayer> players = new ArrayList<OfflinePlayer>();
		
		// Determine list of players given context
		if(context.equals(MenuState.B_REQUESTS)) {
			players.addAll(guild.getJoinRequests());
			loreHintStr1 = "Left-click to accept";
			loreHintStr2 = "Right-click to decline";
			isClickable = true;
		} else if(context.equals(MenuState.C_PROMOTE)) {
			players.addAll(guild.getPlayerMembersOnly());
			loreHintStr1 = "Click to promote to Guild Officer";
			isClickable = true;
		} else if(context.equals(MenuState.C_DEMOTE)) {
			players.addAll(guild.getPlayerOfficersOnly());
			loreHintStr1 = "Click to demote to Guild Member";
			isClickable = true;
		} else if(context.equals(MenuState.C_TRANSFER)) {
			players.addAll(guild.getPlayerOfficersOnly());
			players.addAll(guild.getPlayerMembersOnly());
			loreHintStr1 = "Click to make Guild Master";
			isClickable = true;
		} else {
			return null;
		}
		
		// Create page(s)
		String pageLabel = "";
		List<String> loreList;
		int pageTotal = (int)Math.ceil(((double)players.size())/MAX_ICONS_PER_PAGE);
		if(pageTotal == 0) {
			pageTotal = 1;
		}
		int pageNum = 0;
		ListIterator<OfflinePlayer> listIter = players.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int numPageRows = (int)Math.ceil(((double)(players.size() - i*MAX_ICONS_PER_PAGE))/9);
			if(numPageRows < 1) {
				numPageRows = 1;
			} else if(numPageRows > 5) {
				numPageRows = 5;
			}
			pageLabel = getTitle(context)+" "+(i+1)+"/"+pageTotal;
			pages.add(pageNum, new DisplayMenu(numPageRows+1, pageLabel));
			int slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && listIter.hasNext()) {
				/* Player Icon (n) */
				OfflinePlayer currentPlayer = listIter.next();
				ChatColor guildColor = ChatColor.GREEN;
				loreList = new ArrayList<String>();
				if(currentPlayer.isOnline()) {
					loreList.add(loreColor+"Online");
				} else {
					String lastOnlineFormat = Konquest.getDateFormat(currentPlayer.getLastPlayed());
					loreList.add(loreColor+"Seen: "+valueColor+lastOnlineFormat);
				}
				if(!loreHintStr1.equals("")) {
					loreList.add(hintColor+loreHintStr1);
				}
				if(!loreHintStr2.equals("")) {
					loreList.add(hintColor+loreHintStr2);
				}
		    	PlayerIcon playerIcon = new PlayerIcon(guildColor+currentPlayer.getName(),loreList,currentPlayer,slotIndex,isClickable,iconAction);
		    	pages.get(pageNum).addIcon(playerIcon);
				slotIndex++;
			}
			pageNum++;
		}
		result = pages.get(currentPage);
		return result;
	}

	@Override
	public DisplayMenu getCurrentView() {
		return views.get(currentState);
	}


	@Override
	public DisplayMenu updateState(int slot, boolean clickType) {
		// Assume a clickable icon was clicked
		// Do something based on current state and clicked slot
		DisplayMenu result = null;
		int navMaxIndex = getCurrentView().getInventory().getSize()-1;
		int navMinIndex = getCurrentView().getInventory().getSize()-9;
		if(slot <= navMaxIndex && slot >= navMinIndex) {
			// Clicked in navigation bar
			int index = slot-navMinIndex;
			// (back [0]) close [4], return [5], finish [6], (next [8])
			if(index == 0) {
				result = goPageBack();
			} else if(index == 4) {
				// Close
				result = null;
			} else if(index == 5) {
				// Return to previous root
				//result = views.get(MenuState.ROOT);
				result = goToRootView();
				currentState = MenuState.ROOT;
			} else if(index == 8) {
				result = goPageNext();
			}
		} else if(slot < navMinIndex) {
			// Click in non-navigation slot
			MenuIcon clickedIcon = views.get(currentState).getIcon(slot);
			switch(currentState) {
				case ROOT:
					if(slot == ROOT_SLOT_JOIN) {
						if(guild == null) {
							currentState = MenuState.A_JOIN;
							result = goToGuildView(currentState);
						} else {
							ChatUtil.sendNotice(player.getBukkitPlayer(), "You are already a member of a guild");
							Konquest.playFailSound(player.getBukkitPlayer());
						}
					} else if(slot == ROOT_SLOT_LEAVE) {
						if(guild != null) {
							currentState = MenuState.A_LEAVE;
							result = views.get(currentState);
						} else {
							ChatUtil.sendNotice(player.getBukkitPlayer(), "You are not a member of any guild");
							Konquest.playFailSound(player.getBukkitPlayer());
						}
					} else if(slot == ROOT_SLOT_INVITE) {
						currentState = MenuState.A_INVITE;
						result = goToGuildView(currentState);
						
					} else if(slot == ROOT_SLOT_LIST) {
						currentState = MenuState.A_LIST;
						result = goToGuildView(currentState);
						
					} else if(slot == ROOT_SLOT_RELATIONSHIPS) {
						currentState = MenuState.B_RELATIONSHIP;
						result = goToGuildView(currentState);
						
					} else if(slot == ROOT_SLOT_REQUESTS) {
						currentState = MenuState.B_REQUESTS;
						result = goToPlayerView(currentState);
						
					} else if(slot == ROOT_SLOT_PROMOTE) {
						currentState = MenuState.C_PROMOTE;
						result = goToPlayerView(currentState);
						
					} else if(slot == ROOT_SLOT_DEMOTE) {
						currentState = MenuState.C_DEMOTE;
						result = goToPlayerView(currentState);
						
					} else if(slot == ROOT_SLOT_TRANSFER) {
						currentState = MenuState.C_TRANSFER;
						result = goToPlayerView(currentState);
						
					} else if(slot == ROOT_SLOT_DISBAND) {
						currentState = MenuState.C_DISBAND;
						result = views.get(currentState);
						
					}  else if(slot == ROOT_SLOT_OPEN) {
						result = null; // Close menu
						manager.toggleGuildOpen(guild, player);
						Konquest.playSuccessSound(player.getBukkitPlayer());
						
					} else if(slot == ROOT_SLOT_SPECIALIZE) {
						currentState = MenuState.C_SPECIALIZE;
						result = views.get(currentState);
					}
					break;
				case A_JOIN:
					if(clickedIcon != null && clickedIcon instanceof GuildIcon) {
						GuildIcon icon = (GuildIcon)clickedIcon;
						KonGuild clickGuild = icon.getGuild();
						manager.joinGuildRequest(player, clickGuild);
						result = null; // Close menu
					}
					break;
				case A_LEAVE:
					if(slot == SLOT_YES) {
						manager.leaveGuild(player, guild);
					} else if(slot == SLOT_NO) {
						// Do nothing, just close the menu
					}
					result = null; // Close menu
					break;
				case A_INVITE:
					if(clickedIcon != null && clickedIcon instanceof GuildIcon) {
						GuildIcon icon = (GuildIcon)clickedIcon;
						KonGuild clickGuild = icon.getGuild();
						boolean status = manager.respondGuildInvite(player, clickGuild, clickType);
						if(status) {
							// Invite accepted
							result = null; // Close menu
						} else {
							// Invite declined
							result = goToGuildView(currentState);
						}
					}
					break;
				case A_LIST:
					// Do nothing for now
					//TODO: Open separate guild info menu
					break;
				case B_RELATIONSHIP:
					if(clickedIcon != null && clickedIcon instanceof GuildIcon) {
						GuildIcon icon = (GuildIcon)clickedIcon;
						KonGuild clickGuild = icon.getGuild();
						boolean status = manager.toggleGuildStatus(guild, clickGuild, player);
						if(status) {
							result = goToGuildView(currentState);
							Konquest.playSuccessSound(player.getBukkitPlayer());
						} else {
							result = null;
							Konquest.playFailSound(player.getBukkitPlayer());
						}
					}
					break;
				case B_REQUESTS:
					if(clickedIcon != null && clickedIcon instanceof PlayerIcon) {
						PlayerIcon icon = (PlayerIcon)clickedIcon;
						OfflinePlayer clickPlayer = icon.getOfflinePlayer();
						boolean status = manager.respondGuildRequest(clickPlayer, guild, clickType);
						if(status) {
							Konquest.playSuccessSound(player.getBukkitPlayer());
						} else {
							//TODO: Change to MessagePath
							ChatUtil.sendError(player.getBukkitPlayer(), clickPlayer.getName()+" is already in another guild.");
							Konquest.playFailSound(player.getBukkitPlayer());
						}
						result = goToPlayerView(currentState);
					}
					break;
				case C_PROMOTE:
					if(clickedIcon != null && clickedIcon instanceof PlayerIcon) {
						PlayerIcon icon = (PlayerIcon)clickedIcon;
						OfflinePlayer clickPlayer = icon.getOfflinePlayer();
						manager.promoteOfficer(clickPlayer, guild);
						result = goToPlayerView(currentState);
					}
					break;
				case C_DEMOTE:
					if(clickedIcon != null && clickedIcon instanceof PlayerIcon) {
						PlayerIcon icon = (PlayerIcon)clickedIcon;
						OfflinePlayer clickPlayer = icon.getOfflinePlayer();
						manager.demoteOfficer(clickPlayer, guild);
						result = goToPlayerView(currentState);
					}
					break;
				case C_TRANSFER:
					if(clickedIcon != null && clickedIcon instanceof PlayerIcon) {
						PlayerIcon icon = (PlayerIcon)clickedIcon;
						OfflinePlayer clickPlayer = icon.getOfflinePlayer();
						manager.transferMaster(clickPlayer, guild);
						result = null; // Close menu
						Konquest.playSuccessSound(player.getBukkitPlayer());
					}
					break;
				case C_SPECIALIZE:
					if(clickedIcon != null && clickedIcon instanceof ProfessionIcon) {
						ProfessionIcon icon = (ProfessionIcon)clickedIcon;
						Villager.Profession clickProfession = icon.getProfession();
						boolean status = manager.changeSpecialization(clickProfession, guild, player);
						if(status) {
							ChatUtil.sendNotice(player.getBukkitPlayer(), "Changed "+guild.getName()+" Guild specialization to "+clickProfession.name());
							Konquest.playSuccessSound(player.getBukkitPlayer());
						} else {
							Konquest.playFailSound(player.getBukkitPlayer());
						}
						result = null; // Close menu
					}
					break;
				case C_DISBAND:
					if(slot == SLOT_YES) {
						manager.removeGuild(guild);
						Konquest.playSuccessSound(player.getBukkitPlayer());
					} else if(slot == SLOT_NO) {
						// Do nothing, just close the menu
					}
					result = null; // Close menu
					break;
				default:
					break;
			}
		}
		refreshNavigationButtons(currentState);
		return result;
	}
	
	private String getTitle(MenuState context) {
		String result = "";
		ChatColor color = ChatColor.BLACK;
		switch(context) {
			case ROOT:
				if(guild != null) {
					result = color+guild.getName()+" Guild Menu";
				} else {
					result = color+"Guild Menu";
				}
				break;
			case A_JOIN:
				result = color+"Join a Guild";
				break;
			case A_LEAVE:
				result = color+"Are you sure?";
				break;
			case A_INVITE:
				result = color+"Guild Invites";
				break;
			case A_LIST:
				result = color+"Guild List";
				break;
			case B_RELATIONSHIP:
				result = color+"Guild Relationships";
				break;
			case B_REQUESTS:
				result = color+"Member Requests";
				break;
			case C_PROMOTE:
				result = color+"Promote Officers";
				break;
			case C_DEMOTE:
				result = color+"Demote Officers";
				break;
			case C_TRANSFER:
				result = color+"Transfer Guild Master";
				break;
			case C_SPECIALIZE:
				result = color+"Guild Specialization";
				break;
			case C_DISBAND:
				result = color+"Disband your Guild";
				break;
			default:
				break;
		}
		return result;
	}
	
	private DisplayMenu goPageBack() {
		DisplayMenu result = null;
		int newIndex = currentPage-1;
		if(newIndex >= 0) {
			currentPage = newIndex;
		}
		result = pages.get(currentPage);
		views.put(currentState, result);
		return result;
	}
	
	private DisplayMenu goPageNext() {
		DisplayMenu result = null;
		int newIndex = currentPage+1;
		if(newIndex < pages.size()) {
			currentPage = newIndex;
		}
		result = pages.get(currentPage);
		views.put(currentState, result);
		return result;
	}
	
	private DisplayMenu goToGuildView(MenuState context) {
		DisplayMenu result = createGuildView(context);
		views.put(context, result);
		return result;
	}
	
	private DisplayMenu goToPlayerView(MenuState context) {
		DisplayMenu result = createPlayerView(context);
		views.put(context, result);
		return result;
	}
	
	private DisplayMenu goToRootView() {
		DisplayMenu result = createRootView();
		views.put(MenuState.ROOT, result);
		return result;
	}
	
	/**
	 * Place all navigation button icons on view given context and update icons
	 */
	private void refreshNavigationButtons(MenuState context) {
		DisplayMenu view = views.get(context);
		int navStart = view.getInventory().getSize()-9;
		if(navStart < 0) {
			ChatUtil.printDebug("Guild menu nav buttons failed to refresh in context "+context.toString());
			return;
		}
		if(context.equals(MenuState.ROOT)) {
			// Close [4]
			view.addIcon(navIconEmpty(navStart+0));
			view.addIcon(navIconEmpty(navStart+1));
			view.addIcon(navIconEmpty(navStart+2));
			view.addIcon(navIconEmpty(navStart+3));
			view.addIcon(navIconClose(navStart+4));
			view.addIcon(navIconEmpty(navStart+5));
			view.addIcon(navIconEmpty(navStart+6));
			view.addIcon(navIconEmpty(navStart+7));
			view.addIcon(navIconEmpty(navStart+8));
		} else if(context.equals(MenuState.A_LEAVE) || context.equals(MenuState.C_SPECIALIZE) || context.equals(MenuState.C_DISBAND)) {
			// Close [4], Return [5]
			view.addIcon(navIconEmpty(navStart+0));
			view.addIcon(navIconEmpty(navStart+1));
			view.addIcon(navIconEmpty(navStart+2));
			view.addIcon(navIconEmpty(navStart+3));
			view.addIcon(navIconClose(navStart+4));
			view.addIcon(navIconReturn(navStart+5));
			view.addIcon(navIconEmpty(navStart+6));
			view.addIcon(navIconEmpty(navStart+7));
			view.addIcon(navIconEmpty(navStart+8));
		} else if(context.equals(MenuState.A_JOIN) || context.equals(MenuState.A_INVITE) || context.equals(MenuState.A_LIST) ||
				context.equals(MenuState.B_RELATIONSHIP) || context.equals(MenuState.B_REQUESTS) || 
				context.equals(MenuState.C_PROMOTE) || context.equals(MenuState.C_DEMOTE) || context.equals(MenuState.C_TRANSFER)) {
			// (back [0]) close [4], return [5] (next [8])
			if(currentPage > 0) {
				// Place a back button
				view.addIcon(navIconBack(navStart+0));
			} else {
				view.addIcon(navIconEmpty(navStart+0));
			}
			if(currentPage < pages.size()-1) {
				// Place a next button
				view.addIcon(navIconNext(navStart+8));
			} else {
				view.addIcon(navIconEmpty(navStart+8));
			}
			view.addIcon(navIconEmpty(navStart+1));
			view.addIcon(navIconEmpty(navStart+2));
			view.addIcon(navIconEmpty(navStart+3));
			view.addIcon(navIconClose(navStart+4));
			view.addIcon(navIconReturn(navStart+5));
			view.addIcon(navIconEmpty(navStart+6));
			view.addIcon(navIconEmpty(navStart+7));
		}
		view.updateIcons();
	}
	
	private InfoIcon navIconClose(int index) {
		return new InfoIcon(ChatColor.GOLD+MessagePath.LABEL_CLOSE.getMessage(),Collections.emptyList(),Material.STRUCTURE_VOID,index,true);
	}
	
	private InfoIcon navIconBack(int index) {
		return new InfoIcon(ChatColor.GOLD+MessagePath.LABEL_BACK.getMessage(),Collections.emptyList(),Material.ENDER_PEARL,index,true);
	}
	
	private InfoIcon navIconNext(int index) {
		return new InfoIcon(ChatColor.GOLD+MessagePath.LABEL_NEXT.getMessage(),Collections.emptyList(),Material.ENDER_PEARL,index,true);
	}
	
	private InfoIcon navIconEmpty(int index) {
		return new InfoIcon(" ",Collections.emptyList(),Material.GRAY_STAINED_GLASS_PANE,index,false);
	}
	
	private InfoIcon navIconReturn(int index) {
		return new InfoIcon(ChatColor.GOLD+MessagePath.MENU_PLOTS_BUTTON_RETURN.getMessage(),Collections.emptyList(),Material.FIREWORK_ROCKET,index,true);
	}
	
}
