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
		C_SPECIALIZE;
	}
	
	enum AccessType {
		REGULAR,
		OFFICER,
		MASTER;
	}
	
	private final int ROOT_SLOT_JOIN 			= 0;
	private final int ROOT_SLOT_LEAVE 			= 2;
	private final int ROOT_SLOT_INVITE 			= 4;
	private final int ROOT_SLOT_LIST 			= 6;
	private final int ROOT_SLOT_GUILD 			= 8;
	private final int ROOT_SLOT_RELATIONSHIPS 	= 12;
	private final int ROOT_SLOT_REQUESTS 		= 14;
	private final int ROOT_SLOT_PROMOTE 		= 18;
	private final int ROOT_SLOT_DEMOTE 			= 20;
	private final int ROOT_SLOT_OPEN 			= 24;
	private final int ROOT_SLOT_SPECIALIZE 		= 26;
	
	private final int LEAVE_SLOT_YES 			= 3;
	private final int LEAVE_SLOT_NO 			= 5;
	
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
		
		/* Specialize View */
		renderView = createSpecializeView();
		views.put(MenuState.C_SPECIALIZE, renderView);
		refreshNavigationButtons(MenuState.C_SPECIALIZE);
	}
	
	private DisplayMenu createRootView() {
		DisplayMenu result;
		InfoIcon icon;
		List<String> loreList;
		
		ChatColor loreColor = ChatColor.YELLOW;
		ChatColor valueColor = ChatColor.AQUA;
		ChatColor hintColor = ChatColor.GOLD;
		
		int rows = 2;
		switch(menuAccess) {
		case REGULAR:
			rows = 2;
		case OFFICER:
			rows = 3;
		case MASTER:
			rows = 4;
		}
		
		result = new DisplayMenu(rows, getTitle(MenuState.ROOT));
		
		icon = new InfoIcon(ChatColor.GREEN+"Join", Collections.emptyList(), Material.GRASS_BLOCK, ROOT_SLOT_JOIN, true);
		result.addIcon(icon);
		
		icon = new InfoIcon(ChatColor.GREEN+"Leave", Collections.emptyList(), Material.GRASS_BLOCK, ROOT_SLOT_LEAVE, true);
		result.addIcon(icon);

		icon = new InfoIcon(ChatColor.GREEN+"Invites", Collections.emptyList(), Material.GRASS_BLOCK, ROOT_SLOT_INVITE, true);
		result.addIcon(icon);
		
		icon = new InfoIcon(ChatColor.GREEN+"List", Collections.emptyList(), Material.GRASS_BLOCK, ROOT_SLOT_LIST, true);
		result.addIcon(icon);
		
		if(guild != null) {
			icon = new InfoIcon(ChatColor.GREEN+"Guild", Collections.emptyList(), Material.GRASS_BLOCK, ROOT_SLOT_GUILD, false);
			result.addIcon(icon);

			if(menuAccess.equals(AccessType.OFFICER) || menuAccess.equals(AccessType.MASTER)) {
				icon = new InfoIcon(ChatColor.GREEN+"Relationships", Collections.emptyList(), Material.GRASS_BLOCK, ROOT_SLOT_RELATIONSHIPS, true);
				result.addIcon(icon);
				
				icon = new InfoIcon(ChatColor.GREEN+"Requests", Collections.emptyList(), Material.GRASS_BLOCK, ROOT_SLOT_REQUESTS, true);
				result.addIcon(icon);
			}
			
			if(menuAccess.equals(AccessType.MASTER)) {
				icon = new InfoIcon(ChatColor.GREEN+"Promote", Collections.emptyList(), Material.GRASS_BLOCK, ROOT_SLOT_PROMOTE, true);
				result.addIcon(icon);
				
				icon = new InfoIcon(ChatColor.GREEN+"Demote", Collections.emptyList(), Material.GRASS_BLOCK, ROOT_SLOT_DEMOTE, true);
				result.addIcon(icon);
				
				/* Open/Close Button */
				String currentValue = DisplayManager.boolean2Lang(guild.isOpen())+" "+DisplayManager.boolean2Symbol(guild.isOpen());
				loreList = new ArrayList<String>();
		    	loreList.add(loreColor+MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor+currentValue));
		    	loreList.add(hintColor+MessagePath.MENU_OPTIONS_HINT.getMessage());
				icon = new InfoIcon(ChatColor.GREEN+"Open/Close", loreList, Material.GRASS_BLOCK, ROOT_SLOT_OPEN, true);
				result.addIcon(icon);
				
				icon = new InfoIcon(ChatColor.GREEN+"Specialization", Collections.emptyList(), Material.GRASS_BLOCK, ROOT_SLOT_SPECIALIZE, true);
				result.addIcon(icon);
			}
		}
		
		return result;
	}
	
	private DisplayMenu createLeaveView() {
		DisplayMenu result;
		InfoIcon icon;
		result = new DisplayMenu(2, getTitle(MenuState.A_LEAVE));
		icon = new InfoIcon(ChatColor.GOLD+"Yes", Collections.emptyList(), Material.GRASS_BLOCK, LEAVE_SLOT_YES, true);
		result.addIcon(icon);
		icon = new InfoIcon(ChatColor.GOLD+"No", Collections.emptyList(), Material.GRASS_BLOCK, LEAVE_SLOT_NO, true);
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
		for(Villager.Profession profession : Villager.Profession.values()) {
			if(guild == null || (guild != null && !profession.equals(guild.getSpecialization()))) {
				icon = new ProfessionIcon(ChatColor.GOLD+profession.name(),Collections.emptyList(),profession,index,true);
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
		String loreHintStr = "";
		boolean isClickable = false;
		List<KonGuild> guilds = new ArrayList<KonGuild>();
		
		// Determine list of guilds given context
		if(context.equals(MenuState.A_JOIN)) {
			// List of all friendly guilds able to join (sends request)
			guilds.addAll(manager.getKingdomGuilds(player.getKingdom()));
			if(guild != null) {
				guilds.remove(guild);
			}
			loreHintStr = "Click send a join request";
			isClickable = true;
		} else if(context.equals(MenuState.A_INVITE)) {
			// List of friendly guilds with valid join invite for player
			guilds.addAll(manager.getInviteGuilds(player));
			if(guild != null) {
				guilds.remove(guild);
			}
			loreHintStr = "Left-click to accept, Right-click to decline";
			isClickable = true;
		} else if(context.equals(MenuState.A_LIST)) {
			// List of all guilds, friendly and enemy, with normal info
			guilds.addAll(manager.getAllGuilds());
			loreHintStr = "No click";
			isClickable = false;
		} else if(context.equals(MenuState.B_RELATIONSHIP)) {
			// List of all guilds, friendly and enemy, with relationship status and click hints
			guilds.addAll(manager.getAllGuilds());
			loreHintStr = "Click to change status";
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
				loreList.add("Towns: "+currentGuild.getNumTowns());
				loreList.add("Members: "+currentGuild.getNumMembers());
				if(guild != null) {
					if(!player.getKingdom().equals(guild.getKingdom())) {
						guildColor = ChatColor.RED;
						String guildEnemyStatus = "Hostile";
						if(guild.isArmistice(currentGuild)) {
							guildEnemyStatus = "Armistice";
						}
						loreList.add("Status: "+guildEnemyStatus);
					} else {
						String guildFriendlyStatus = "Treaty";
						if(guild.isSanction(currentGuild)) {
							guildFriendlyStatus = "Sanction";
						}
						loreList.add("Status: "+guildFriendlyStatus);
					}
				}
				loreList.add(loreHintStr);
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
		String loreHintStr = "";
		PlayerIconAction iconAction = PlayerIconAction.GUILD;
		boolean isClickable = false;
		List<OfflinePlayer> players = new ArrayList<OfflinePlayer>();
		
		// Determine list of players given context
		if(context.equals(MenuState.B_REQUESTS)) {
			players.addAll(guild.getJoinRequests());
			loreHintStr = "Left-click to accept, Right-click to decline";
			isClickable = true;
		} else if(context.equals(MenuState.C_PROMOTE)) {
			players.addAll(guild.getPlayerMembersOnly());
			loreHintStr = "Click to promote to Guild Officer";
			isClickable = true;
		} else if(context.equals(MenuState.C_DEMOTE)) {
			players.addAll(guild.getPlayerOfficersOnly());
			loreHintStr = "Click to demote to Guild Member";
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
				loreList.add("Last Online: ?");
				loreList.add(loreHintStr);
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
	public DisplayMenu updateState(int slot) {
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
				result = views.get(MenuState.ROOT);
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
						currentState = MenuState.A_JOIN;
						result = goToGuildView(currentState);
						
					} else if(slot == ROOT_SLOT_LEAVE) {
						currentState = MenuState.A_LEAVE;
						result = views.get(currentState);
						
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
						
					} else if(slot == ROOT_SLOT_OPEN) {
						result = null; // Close menu
						manager.toggleGuildOpen(guild);
						
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
					}
					result = null; // Close menu
					break;
				case A_LEAVE:
					if(slot == LEAVE_SLOT_YES) {
						manager.leaveGuild(player, guild);
					} else if(slot == LEAVE_SLOT_NO) {
						// Do nothing, just close the menu
					}
					result = null; // Close menu
					break;
				case A_INVITE:
					if(clickedIcon != null && clickedIcon instanceof GuildIcon) {
						GuildIcon icon = (GuildIcon)clickedIcon;
						KonGuild clickGuild = icon.getGuild();
						manager.joinGuildInvite(player, clickGuild);
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
						manager.toggleGuildStatus(guild, clickGuild);
						result = goToGuildView(currentState);
					}
					break;
				case B_REQUESTS:
					if(clickedIcon != null && clickedIcon instanceof PlayerIcon) {
						//PlayerIcon icon = (PlayerIcon)clickedIcon;
						//OfflinePlayer clickPlayer = icon.getOfflinePlayer();
						//manager.joinGuildInvite(clickPlayer, guild);
						//TODO: Make method for accepting/rejecting join requests
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
				case C_SPECIALIZE:
					if(clickedIcon != null && clickedIcon instanceof ProfessionIcon) {
						ProfessionIcon icon = (ProfessionIcon)clickedIcon;
						Villager.Profession clickProfession = icon.getProfession();
						manager.changeSpecialization(clickProfession, guild);
					}
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
				result = color+"Leave your Guild";
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
				result = color+"Guild Member Requests";
				break;
			case C_PROMOTE:
				result = color+"Promote Guild Officers";
				break;
			case C_DEMOTE:
				result = color+"Demote Guild Officers";
				break;
			case C_SPECIALIZE:
				result = color+"Guild Specialization";
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
		DisplayMenu result = null;
		result = createGuildView(context);
		views.put(context, result);
		return result;
	}
	
	private DisplayMenu goToPlayerView(MenuState context) {
		DisplayMenu result = null;
		result = createPlayerView(context);
		views.put(context, result);
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
		} else if(context.equals(MenuState.A_LEAVE) || context.equals(MenuState.C_SPECIALIZE)) {
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
				context.equals(MenuState.B_RELATIONSHIP) || context.equals(MenuState.B_REQUESTS) || context.equals(MenuState.C_PROMOTE) || context.equals(MenuState.C_DEMOTE)) {
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
