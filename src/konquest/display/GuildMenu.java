package konquest.display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;

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
		
		renderView = createRootView();
		views.put(MenuState.ROOT, renderView);
		refreshNavigationButtons(MenuState.ROOT);
	}
	
	private DisplayMenu createRootView() {
		DisplayMenu result;
		InfoIcon icon;
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
		}
		
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
			
			icon = new InfoIcon(ChatColor.GREEN+"Open/Close", Collections.emptyList(), Material.GRASS_BLOCK, ROOT_SLOT_OPEN, true);
			result.addIcon(icon);
			
			icon = new InfoIcon(ChatColor.GREEN+"Specialization", Collections.emptyList(), Material.GRASS_BLOCK, ROOT_SLOT_SPECIALIZE, true);
			result.addIcon(icon);
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
		List<String> loreList = new ArrayList<String>();
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
				/* Info Icon (n) */
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
		    	InfoIcon guildIcon = new InfoIcon(guildColor+currentGuild.getName(),loreList,Material.GOLD_BLOCK,slotIndex,isClickable);
		    	pages.get(pageNum).addIcon(guildIcon);
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
			} else if(index == 6) {
				// Finish and commit
				result = null;
				//TODO: editGuild commit
			} else if(index == 8) {
				result = goPageNext();
			}
			
			/*
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
			*/
			
		} else if(slot < navMinIndex) {
			// Click in non-navigation slot
			
			MenuIcon clickedIcon = views.get(currentState).getIcon(slot);
			
			switch(currentState) {
				case ROOT:
					if(slot == ROOT_SLOT_JOIN) {
						currentState = MenuState.A_JOIN;
						result = createGuildView(currentState);
						
					} else if(slot == ROOT_SLOT_LEAVE) {
						currentState = MenuState.A_LEAVE;
						
					} else if(slot == ROOT_SLOT_INVITE) {
						currentState = MenuState.A_INVITE;
						result = createGuildView(currentState);
						
					} else if(slot == ROOT_SLOT_LIST) {
						currentState = MenuState.A_LIST;
						result = createGuildView(currentState);
						
					} else if(slot == ROOT_SLOT_RELATIONSHIPS) {
						currentState = MenuState.B_RELATIONSHIP;
						result = createGuildView(currentState);
					} else if(slot == ROOT_SLOT_REQUESTS) {
						currentState = MenuState.B_REQUESTS;
						
					} else if(slot == ROOT_SLOT_PROMOTE) {
						currentState = MenuState.C_PROMOTE;
						
					} else if(slot == ROOT_SLOT_DEMOTE) {
						currentState = MenuState.C_DEMOTE;
						
					} else if(slot == ROOT_SLOT_OPEN) {
						
						result = null;
						
					} else if(slot == ROOT_SLOT_SPECIALIZE) {
						currentState = MenuState.C_SPECIALIZE;
						
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
		} else if(context.equals(MenuState.A_JOIN) || context.equals(MenuState.A_INVITE) || context.equals(MenuState.A_LIST)) {
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
		} else if(context.equals(MenuState.B_RELATIONSHIP) || context.equals(MenuState.B_REQUESTS) || context.equals(MenuState.C_PROMOTE) || context.equals(MenuState.C_DEMOTE)) {
			// (back [0]) close [4], return [5], finish [6] (next [8])
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
			view.addIcon(navIconFinish(navStart+6));
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
	
	private InfoIcon navIconFinish(int index) {
		return new InfoIcon(ChatColor.GOLD+MessagePath.MENU_PLOTS_BUTTON_FINISH.getMessage(),Collections.emptyList(),Material.WRITTEN_BOOK,index,true);
	}
	
}
