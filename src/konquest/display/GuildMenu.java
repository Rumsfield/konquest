package konquest.display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
	private boolean isAdmin;
	private AccessType menuAccess;
	private Comparator<KonGuild> guildComparator;
	
	public GuildMenu(GuildManager manager, KonPlayer player, KonGuild guild, boolean isAdmin) {
		this.views = new HashMap<MenuState,DisplayMenu>();
		this.pages = new ArrayList<DisplayMenu>();
		this.currentPage = 0;
		this.currentState = MenuState.ROOT;
		this.manager = manager;
		this.player = player;
		this.guild = guild;
		this.isAdmin = isAdmin;
		this.menuAccess = AccessType.REGULAR;
		this.guildComparator = new Comparator<KonGuild>() {
   			@Override
   			public int compare(final KonGuild g1, KonGuild g2) {
   				// sort by land, then population
   				int result = 0;
   				int g1Land = g1.getNumLand();
   				int g2Land = g2.getNumLand();
   				if(g1Land < g2Land) {
   					result = 1;
   				} else if(g1Land > g2Land) {
   					result = -1;
   				} else {
   					int g1Pop = g1.getNumMembers();
   					int g2Pop = g2.getNumMembers();
   					if(g1Pop < g2Pop) {
   						result = 1;
   					} else if(g1Pop > g2Pop) {
   						result = -1;
   					}
   				}
   				return result;
   			}
   		};
		
		initializeMenu();
		renderDefaultViews();
	}
	
	
	private void initializeMenu() {
		if(guild != null) {
			UUID id = player.getBukkitPlayer().getUniqueId();
			if(isAdmin) {
				menuAccess = AccessType.MASTER;
			} else {
				if(guild.isMaster(id)) {
					menuAccess = AccessType.MASTER;
				} else if(guild.isOfficer(id)) {
					menuAccess = AccessType.OFFICER;
				} else if(guild.isMember(id)) {
					menuAccess = AccessType.REGULAR;
				}
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
		loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_JOIN.getMessage());
		icon = new InfoIcon(regularColor+MessagePath.MENU_GUILD_JOIN.getMessage(), loreList, Material.SADDLE, ROOT_SLOT_JOIN, true);
		result.addIcon(icon);
		
		loreList.clear();
		loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_LEAVE.getMessage());
		icon = new InfoIcon(regularColor+MessagePath.MENU_GUILD_LEAVE.getMessage(), loreList, Material.ARROW, ROOT_SLOT_LEAVE, true);
		result.addIcon(icon);

		loreList.clear();
		loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_INVITES.getMessage());
		int numInvites = manager.getInviteGuilds(player).size();
		if(numInvites > 0) {
			loreList.add(valueColor+""+numInvites);
		}
		icon = new InfoIcon(regularColor+MessagePath.MENU_GUILD_INVITES.getMessage(), loreList, Material.WRITABLE_BOOK, ROOT_SLOT_INVITE, true);
		result.addIcon(icon);
		
		loreList.clear();
		loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_LIST.getMessage());
		icon = new InfoIcon(regularColor+MessagePath.MENU_GUILD_LIST.getMessage(), loreList, Material.LECTERN, ROOT_SLOT_LIST, true);
		result.addIcon(icon);
		
		if(guild != null) {
			loreList = new ArrayList<String>();
			icon = new GuildIcon(guild,true,false,loreList,ROOT_SLOT_GUILD,false);
			result.addIcon(icon);

			if(menuAccess.equals(AccessType.OFFICER) || menuAccess.equals(AccessType.MASTER)) {
				loreList = new ArrayList<String>();
				loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_RELATION.getMessage());
				icon = new InfoIcon(officerColor+MessagePath.MENU_GUILD_RELATION.getMessage(), loreList, Material.GOLDEN_SWORD, ROOT_SLOT_RELATIONSHIPS, true);
				result.addIcon(icon);
				
				loreList.clear();
				loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_REQUESTS.getMessage());
				int numRequests = guild.getJoinRequests().size();
				if(numRequests > 0) {
					loreList.add(valueColor+""+numRequests);
				}
				icon = new InfoIcon(officerColor+MessagePath.MENU_GUILD_REQUESTS.getMessage(), loreList, Material.JUKEBOX, ROOT_SLOT_REQUESTS, true);
				result.addIcon(icon);
			}
			
			if(menuAccess.equals(AccessType.MASTER)) {
				loreList = new ArrayList<String>();
				loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_PROMOTE.getMessage());
				icon = new InfoIcon(masterColor+MessagePath.MENU_GUILD_PROMOTE.getMessage(), loreList, Material.DIAMOND_HORSE_ARMOR, ROOT_SLOT_PROMOTE, true);
				result.addIcon(icon);
				
				loreList.clear();
				loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_DEMOTE.getMessage());
				icon = new InfoIcon(masterColor+MessagePath.MENU_GUILD_DEMOTE.getMessage(), loreList, Material.LEATHER_HORSE_ARMOR, ROOT_SLOT_DEMOTE, true);
				result.addIcon(icon);
				
				loreList.clear();
				loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_TRANSFER.getMessage());
				icon = new InfoIcon(masterColor+MessagePath.MENU_GUILD_TRANSFER.getMessage(), loreList, Material.ELYTRA, ROOT_SLOT_TRANSFER, true);
				result.addIcon(icon);
				
				loreList.clear();
				loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_SPECIALIZE.getMessage());
				icon = new InfoIcon(masterColor+MessagePath.MENU_GUILD_SPECIAL.getMessage(), loreList, Material.SMITHING_TABLE, ROOT_SLOT_SPECIALIZE, true);
				result.addIcon(icon);
				
				/* Open/Close Button */
				String currentValue = DisplayManager.boolean2Lang(guild.isOpen())+" "+DisplayManager.boolean2Symbol(guild.isOpen());
				loreList.clear();
		    	loreList.add(loreColor+MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor+currentValue));
		    	loreList.add(hintColor+MessagePath.MENU_OPTIONS_HINT.getMessage());
				icon = new InfoIcon(masterColor+MessagePath.MENU_GUILD_OPEN.getMessage(), loreList, Material.IRON_DOOR, ROOT_SLOT_OPEN, true);
				result.addIcon(icon);
				
				loreList.clear();
				loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_DISBAND.getMessage());
				icon = new InfoIcon(masterColor+MessagePath.MENU_GUILD_DISBAND.getMessage(), loreList, Material.CREEPER_HEAD, ROOT_SLOT_DISBAND, true);
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
		loreList.add(loreColor+MessagePath.MENU_GUILD_HINT_LEAVE.getMessage());
		icon = new InfoIcon(DisplayManager.boolean2Symbol(true), loreList, Material.GLOWSTONE_DUST, SLOT_YES, true);
		result.addIcon(icon);
		
		loreList.clear();
		loreList.add(loreColor+MessagePath.MENU_GUILD_HINT_EXIT.getMessage());
		icon = new InfoIcon(DisplayManager.boolean2Symbol(false), loreList, Material.REDSTONE, SLOT_NO, true);
		result.addIcon(icon);
		return result;
	}
	
	private DisplayMenu createDisbandView() {
		DisplayMenu result;
		InfoIcon icon;
		List<String> loreList = new ArrayList<String>();
		result = new DisplayMenu(2, getTitle(MenuState.C_DISBAND));
		
		loreList.clear();
		loreList.add(hintColor+MessagePath.MENU_GUILD_HINT_DISBAND.getMessage());
		icon = new InfoIcon(DisplayManager.boolean2Symbol(true), loreList, Material.GLOWSTONE_DUST, SLOT_YES, true);
		result.addIcon(icon);
		
		loreList.clear();
		loreList.add(hintColor+MessagePath.MENU_GUILD_HINT_EXIT.getMessage());
		icon = new InfoIcon(DisplayManager.boolean2Symbol(false), loreList, Material.REDSTONE, SLOT_NO, true);
		result.addIcon(icon);
		return result;
	}
	
	private DisplayMenu createSpecializeView() {
		DisplayMenu result;
		ProfessionIcon icon;
		int numEntries = Villager.Profession.values().length - 1; // Subtract one to omit current specialization choice
		int numRows = (int)Math.ceil((double)numEntries / 9);
		result = new DisplayMenu(numRows+1, getTitle(MenuState.C_SPECIALIZE));
		int index = 0;
		List<String> loreList = new ArrayList<String>();
		if(!isAdmin) {
			String cost = String.format("%.2f",manager.getCostSpecial());
			loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+cost);
		}
		loreList.add(hintColor+MessagePath.MENU_GUILD_HINT_SPECIAL.getMessage());
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
			loreHintStr1 = MessagePath.MENU_GUILD_HINT_JOIN.getMessage();
			isClickable = true;
		} else if(context.equals(MenuState.A_INVITE)) {
			// List of friendly guilds with valid join invite for player
			guilds.addAll(manager.getInviteGuilds(player));
			if(guild != null) {
				guilds.remove(guild);
			}
			loreHintStr1 = MessagePath.MENU_GUILD_HINT_INVITE_1.getMessage();
			loreHintStr2 = MessagePath.MENU_GUILD_HINT_INVITE_2.getMessage();
			isClickable = true;
		} else if(context.equals(MenuState.A_LIST)) {
			// List of all guilds, friendly and enemy, with normal info
			guilds.addAll(manager.getAllGuilds());
			isClickable = false;
		} else if(context.equals(MenuState.B_RELATIONSHIP)) {
			// List of all guilds, friendly and enemy, with relationship status and click hints
			if(manager.isDiscountEnable()) {
				guilds.addAll(manager.getAllGuilds());
			} else {
				guilds.addAll(manager.getEnemyGuilds(player.getKingdom()));
			}
			if(guild != null) {
				guilds.remove(guild);
			}
			loreHintStr1 = MessagePath.MENU_GUILD_HINT_RELATION.getMessage();
			isClickable = true;
		} else {
			return null;
		}
		// Sort guild list by land then population
		Collections.sort(guilds, guildComparator);
		
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
				loreList = new ArrayList<String>();
				boolean isFriendly = false;
				boolean isArmistice = false;
				if(player.getKingdom().equals(currentGuild.getKingdom())) {
					isFriendly = true;
				}
				if(guild != null) {
					if(!player.getKingdom().equals(currentGuild.getKingdom())) {
						String theirEnemyStatus = currentGuild.isArmistice(guild) ? MessagePath.LABEL_ARMISTICE.getMessage() : MessagePath.LABEL_HOSTILE.getMessage();
						loreList.add(loreColor+MessagePath.MENU_GUILD_THEIR_STATUS.getMessage()+": "+valueColor+theirEnemyStatus);
						String guildEnemyStatus = guild.isArmistice(currentGuild) ? MessagePath.LABEL_ARMISTICE.getMessage() : MessagePath.LABEL_HOSTILE.getMessage();
						loreList.add(loreColor+MessagePath.MENU_GUILD_OUR_STATUS.getMessage()+": "+valueColor+guildEnemyStatus);
					} else {
						String theirFriendlyStatus = currentGuild.isSanction(guild) ? MessagePath.LABEL_SANCTION.getMessage() : MessagePath.LABEL_TREATY.getMessage();
						loreList.add(loreColor+MessagePath.MENU_GUILD_THEIR_STATUS.getMessage()+": "+valueColor+theirFriendlyStatus);
						String guildFriendlyStatus = guild.isSanction(currentGuild) ? MessagePath.LABEL_SANCTION.getMessage() : MessagePath.LABEL_TREATY.getMessage();
						loreList.add(loreColor+MessagePath.MENU_GUILD_OUR_STATUS.getMessage()+": "+valueColor+guildFriendlyStatus);
					}
					isArmistice = manager.isArmistice(guild, currentGuild);
					if(context.equals(MenuState.B_RELATIONSHIP) && !isAdmin) {
						String cost = String.format("%.2f",manager.getCostRelation());
						loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+cost);
					}
				}
				if(!loreHintStr1.equals("")) {
					loreList.add(hintColor+loreHintStr1);
				}
				if(!loreHintStr2.equals("")) {
					loreList.add(hintColor+loreHintStr2);
				}
		    	GuildIcon guildIcon = new GuildIcon(currentGuild,isFriendly,isArmistice,loreList,slotIndex,isClickable);
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
			loreHintStr1 = MessagePath.MENU_GUILD_HINT_REQUEST_1.getMessage();
			loreHintStr2 = MessagePath.MENU_GUILD_HINT_REQUEST_2.getMessage();
			isClickable = true;
		} else if(context.equals(MenuState.C_PROMOTE)) {
			players.addAll(guild.getPlayerMembersOnly());
			loreHintStr1 = MessagePath.MENU_GUILD_HINT_PROMOTE.getMessage();
			isClickable = true;
		} else if(context.equals(MenuState.C_DEMOTE)) {
			players.addAll(guild.getPlayerOfficersOnly());
			loreHintStr1 = MessagePath.MENU_GUILD_HINT_DEMOTE.getMessage();
			isClickable = true;
		} else if(context.equals(MenuState.C_TRANSFER)) {
			players.addAll(guild.getPlayerOfficersOnly());
			players.addAll(guild.getPlayerMembersOnly());
			loreHintStr1 = MessagePath.MENU_GUILD_HINT_TRANSFER.getMessage();
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
				String lastOnlineFormat = Konquest.getDateFormat(currentPlayer.getLastPlayed());
				loreList.add(valueColor+lastOnlineFormat);
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
							ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_GUILD_ERROR_OTHER_GUILD.getMessage());
							Konquest.playFailSound(player.getBukkitPlayer());
						}
					} else if(slot == ROOT_SLOT_LEAVE) {
						if(guild != null) {
							currentState = MenuState.A_LEAVE;
							result = views.get(currentState);
						} else {
							ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_GUILD_ERROR_NO_GUILD.getMessage());
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
						result = goToDisbandView();
						
					}  else if(slot == ROOT_SLOT_OPEN) {
						result = null; // Close menu
						manager.toggleGuildOpen(guild, player);
						Konquest.playSuccessSound(player.getBukkitPlayer());
						
					} else if(slot == ROOT_SLOT_SPECIALIZE) {
						currentState = MenuState.C_SPECIALIZE;
						result = goToSpecializeView();
						
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
						boolean status = manager.toggleGuildStatus(guild, clickGuild, player, isAdmin);
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
							ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_GUILD_ERROR_PLAYER_GUILD.getMessage(clickPlayer.getName()));
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
						boolean status = manager.changeSpecialization(clickProfession, guild, player, isAdmin);
						if(status) {
							ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_GUILD_NOTICE_SPECIALIZE.getMessage(guild.getName(),clickProfession.name()));
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
		if(isAdmin) {
			color = ChatColor.GOLD;
		}
		String name = "";
		if(guild != null) {
			name = guild.getName();
		}
		switch(context) {
			case ROOT:
				if(guild != null) {
					result = color+MessagePath.MENU_GUILD_TITLE_ROOT_NAME.getMessage(name);
				} else {
					result = color+MessagePath.MENU_GUILD_TITLE_ROOT.getMessage();
				}
				break;
			case A_JOIN:
				result = color+MessagePath.MENU_GUILD_TITLE_JOIN.getMessage();
				break;
			case A_LEAVE:
				result = color+MessagePath.MENU_GUILD_TITLE_CONFIRM.getMessage();
				break;
			case A_INVITE:
				result = color+MessagePath.MENU_GUILD_TITLE_INVITES.getMessage();
				break;
			case A_LIST:
				result = color+MessagePath.MENU_GUILD_TITLE_LIST.getMessage();
				break;
			case B_RELATIONSHIP:
				result = color+MessagePath.MENU_GUILD_TITLE_RELATIONS.getMessage();
				break;
			case B_REQUESTS:
				result = color+MessagePath.MENU_GUILD_TITLE_REQUESTS.getMessage();
				break;
			case C_PROMOTE:
				result = color+MessagePath.MENU_GUILD_TITLE_PROMOTION.getMessage();
				break;
			case C_DEMOTE:
				result = color+MessagePath.MENU_GUILD_TITLE_DEMOTION.getMessage();
				break;
			case C_TRANSFER:
				result = color+MessagePath.MENU_GUILD_TITLE_TRANSFER.getMessage();
				break;
			case C_SPECIALIZE:
				result = color+MessagePath.MENU_GUILD_TITLE_SPECIALIZE.getMessage();
				break;
			case C_DISBAND:
				result = color+MessagePath.MENU_GUILD_TITLE_DISBAND.getMessage(name);
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
	
	private DisplayMenu goToDisbandView() {
		DisplayMenu result = createDisbandView();
		views.put(MenuState.C_DISBAND, result);
		return result;
	}
	
	private DisplayMenu goToSpecializeView() {
		DisplayMenu result = createSpecializeView();
		views.put(MenuState.C_SPECIALIZE, result);
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
