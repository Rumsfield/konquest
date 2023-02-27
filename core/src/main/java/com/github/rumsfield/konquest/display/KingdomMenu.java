package com.github.rumsfield.konquest.display;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestRelationship;
import com.github.rumsfield.konquest.display.icon.*;
import com.github.rumsfield.konquest.display.icon.PlayerIcon.PlayerIconAction;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.manager.KingdomManager;
import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.model.KonMonumentTemplate;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.MessagePath;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;

import java.util.*;

/**
 * State Views
 * Regular players: A_*
 * Officer players: B_*
 * Master players: C_*
 */
public class KingdomMenu extends StateMenu implements ViewableMenu {
	//TODO: Update all message paths to replace guild
	

	enum MenuState implements State {
		ROOT,
		A_JOIN,
		A_EXILE,
		A_INVITE,
		A_LIST,
		B_RELATIONSHIP,
		B_DIPLOMACY,
		B_REQUESTS,
		C_PROMOTE,
		C_DEMOTE,
		C_TRANSFER,
		C_TEMPLATE,
		C_DISBAND
	}

	/*
	 * Menu Layout
	 * Labels with * are a new menu state.
	 * Labels with + are icon buttons.
	 *
	 * Access  | Labels...
	 * ----------------------------------------------------------------------------------
	 * Regular | *Join   		*Leave		+Info		*Invites	*List
	 * Officer | *Relationship	*Requests<br>
	 * Master  | *Promote 		*Demote 	*Transfer 	+Open		*Template 	 *Disband
	 *
	 * Relationship selects other kingdom and opens diplomacy view, which selects new status (enemy, ally, etc)
	 */

	enum AccessType implements Access {
		REGULAR,
		OFFICER,
		MASTER
	}
	
	/* Icon slot indexes */
	private final int ROOT_SLOT_JOIN 			= 0;
	private final int ROOT_SLOT_EXILE 			= 2;
	private final int ROOT_SLOT_INFO 			= 4;
	private final int ROOT_SLOT_INVITE 			= 6;
	private final int ROOT_SLOT_LIST 			= 8;
	private final int ROOT_SLOT_RELATIONSHIPS 	= 12;
	private final int ROOT_SLOT_REQUESTS 		= 14;
	private final int ROOT_SLOT_PROMOTE 		= 19;
	private final int ROOT_SLOT_DEMOTE 			= 20;
	private final int ROOT_SLOT_TRANSFER		= 21;
	private final int ROOT_SLOT_OPEN 			= 23;
	private final int ROOT_SLOT_TEMPLATE 		= 24;
	private final int ROOT_SLOT_DISBAND			= 25;
	
	private final int SLOT_YES 					= 3;
	private final int SLOT_NO 					= 5;

	private final String loreColor = DisplayManager.loreFormat;
	private final String valueColor = DisplayManager.valueFormat;
	private final String hintColor = DisplayManager.hintFormat;

	private final KingdomManager manager;
	private final KonPlayer player;
	private final KonKingdom kingdom;
	private KonKingdom diplomacyKingdom;
	private boolean isCreatedKingdom;
	private boolean isServerKingdom;
	private final boolean isAdmin;
	private Comparator<KonKingdom> kingdomComparator;
	
	public KingdomMenu(Konquest konquest, KonPlayer player, KonKingdom kingdom, boolean isAdmin) {
		super(konquest, MenuState.ROOT, AccessType.REGULAR);
		this.manager = konquest.getKingdomManager();
		this.player = player;
		this.kingdom = kingdom;
		this.diplomacyKingdom = null;
		this.isCreatedKingdom = false; // Is this kingdom created by players, i.e. not barbarians or neutrals
		this.isServerKingdom = false; // Is this kingdom created by an admin for the server
		this.isAdmin = isAdmin;
		this.kingdomComparator = null;
		
		initializeMenu();
		renderDefaultViews();
	}
	
	
	private void initializeMenu() {
		if(kingdom != null) {
			if(kingdom.isCreated()) {
				isCreatedKingdom = true;
				UUID id = player.getBukkitPlayer().getUniqueId();
				if(isAdmin) {
					menuAccess = AccessType.MASTER;
				} else {
					if(kingdom.isMaster(id)) {
						menuAccess = AccessType.MASTER;
					} else if(kingdom.isOfficer(id)) {
						menuAccess = AccessType.OFFICER;
					} else if(kingdom.isMember(id)) {
						menuAccess = AccessType.REGULAR;
					}
				}
			}
			if(kingdom.isAdminOperated()) {
				isServerKingdom = true;
			}
		}
		kingdomComparator = (kingdomOne, kingdomTwo) -> {
			// sort by land, then population
			int result = 0;
			int g1Land = kingdomOne.getNumLand();
			int g2Land = kingdomTwo.getNumLand();
			if(g1Land < g2Land) {
				result = 1;
			} else if(g1Land > g2Land) {
				result = -1;
			} else {
				int g1Pop = kingdomOne.getNumMembers();
				int g2Pop = kingdomTwo.getNumMembers();
				if(g1Pop < g2Pop) {
					result = 1;
				} else if(g1Pop > g2Pop) {
					result = -1;
				}
			}
			return result;
		};
	}
	
	private void renderDefaultViews() {
		DisplayMenu renderView;
		
		/* Root View */
		renderView = createRootView();
		views.put(MenuState.ROOT, renderView);
		refreshNavigationButtons(MenuState.ROOT);
		
		/* Exile View */
		renderView = createExileView();
		views.put(MenuState.A_EXILE, renderView);
		refreshNavigationButtons(MenuState.A_EXILE);
	}
	
	private DisplayMenu createRootView() {
		DisplayMenu result;
		MenuIcon icon;
		List<String> loreList = new ArrayList<>();
		ChatColor regularColor = ChatColor.GREEN;
		ChatColor officerColor = ChatColor.BLUE;
		ChatColor masterColor = ChatColor.LIGHT_PURPLE;
		ChatColor kingdomColor = konquest.getDisplayPrimaryColor(player.getKingdom(), kingdom);
		
		int rows = 2; // default rows for regular
		if(menuAccess.equals(AccessType.OFFICER)) {
			rows = 3;
		} else if(menuAccess.equals(AccessType.MASTER)) {
			rows = 4;
		}
		
		result = new DisplayMenu(rows, getTitle(MenuState.ROOT));
		
		/* Join Icon */
		loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_JOIN.getMessage());
		String joinTip = "Or, create your own kingdom with /k kingdom create.";
		for(String line : Konquest.stringPaginate(joinTip)) {
			loreList.add(loreColor+line);
		}
		icon = new InfoIcon(regularColor+MessagePath.MENU_GUILD_JOIN.getMessage(), loreList, Material.SADDLE, ROOT_SLOT_JOIN, true);
		result.addIcon(icon);
		
		/* Exile Icon */
		loreList.clear();
		loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_LEAVE.getMessage());
		//TODO: context description
		String exileDescription = "Become a barbarian, lose all favor and stats.";
		for(String line : Konquest.stringPaginate(exileDescription)) {
			loreList.add(loreColor+line);
		}
		icon = new InfoIcon(regularColor+"Exile", loreList, Material.ARROW, ROOT_SLOT_EXILE, true);
		result.addIcon(icon);

		/* Invites Icon */
		loreList.clear();
		loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_INVITES.getMessage());
		int numInvites = manager.getInviteKingdoms(player).size();
		Material inviteMat = Material.BOOK;
		if(numInvites > 0) {
			loreList.add(valueColor+""+numInvites);
			inviteMat = Material.WRITABLE_BOOK;
		}
		icon = new InfoIcon(regularColor+MessagePath.MENU_GUILD_INVITES.getMessage(), loreList, inviteMat, ROOT_SLOT_INVITE, true);
		result.addIcon(icon);
		
		/* List Icon */
		loreList.clear();
		loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_LIST.getMessage());
		icon = new InfoIcon(regularColor+MessagePath.MENU_GUILD_LIST.getMessage(), loreList, Material.PAPER, ROOT_SLOT_LIST, true);
		result.addIcon(icon);
		
		// These icons only appear for created kingdoms
		if(isCreatedKingdom) {
			/* Kingdom Info Icon */
			loreList = new ArrayList<>();
			icon = new KingdomIcon(kingdom,kingdomColor,loreList,ROOT_SLOT_INFO,false);
			result.addIcon(icon);

			if(menuAccess.equals(AccessType.OFFICER) || menuAccess.equals(AccessType.MASTER)) {
				/* Relations Icon */
				loreList = new ArrayList<>();
				loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_RELATION.getMessage());
				icon = new InfoIcon(officerColor+MessagePath.MENU_GUILD_RELATION.getMessage(), loreList, Material.GOLDEN_SWORD, ROOT_SLOT_RELATIONSHIPS, true);
				result.addIcon(icon);
				
				/* Requests Icon */
				loreList.clear();
				loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_REQUESTS.getMessage());
				int numRequests = kingdom.getJoinRequests().size();
				Material requestMat = Material.GLASS_BOTTLE;
				if(numRequests > 0) {
					loreList.add(valueColor+""+numRequests);
					requestMat = Material.HONEY_BOTTLE;
				}
				icon = new InfoIcon(officerColor+MessagePath.MENU_GUILD_REQUESTS.getMessage(), loreList, requestMat, ROOT_SLOT_REQUESTS, true);
				result.addIcon(icon);
			}
			
			if(menuAccess.equals(AccessType.MASTER)) {
				/* Promote Icon */
				loreList = new ArrayList<>();
				loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_PROMOTE.getMessage());
				icon = new InfoIcon(masterColor+MessagePath.MENU_GUILD_PROMOTE.getMessage(), loreList, Material.DIAMOND_HORSE_ARMOR, ROOT_SLOT_PROMOTE, true);
				result.addIcon(icon);
				
				/* Demote Icon */
				loreList.clear();
				loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_DEMOTE.getMessage());
				icon = new InfoIcon(masterColor+MessagePath.MENU_GUILD_DEMOTE.getMessage(), loreList, Material.LEATHER_HORSE_ARMOR, ROOT_SLOT_DEMOTE, true);
				result.addIcon(icon);
				
				/* Transfer Icon */
				if(!isServerKingdom) {
					loreList.clear();
					loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_TRANSFER.getMessage());
					icon = new InfoIcon(masterColor+MessagePath.MENU_GUILD_TRANSFER.getMessage(), loreList, Material.ELYTRA, ROOT_SLOT_TRANSFER, true);
					result.addIcon(icon);
				}
				
				/* Open/Close Button */
				String currentValue = DisplayManager.boolean2Lang(kingdom.isOpen())+" "+DisplayManager.boolean2Symbol(kingdom.isOpen());
				loreList.clear();
		    	loreList.add(loreColor+MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor+currentValue));
		    	loreList.add(hintColor+MessagePath.MENU_OPTIONS_HINT.getMessage());
				icon = new InfoIcon(masterColor+MessagePath.MENU_GUILD_OPEN.getMessage(), loreList, Material.IRON_DOOR, ROOT_SLOT_OPEN, true);
				result.addIcon(icon);
				
				/* Template Icon */
				loreList.clear();
				loreList.add(loreColor+"Current: "+valueColor+kingdom.getMonumentTemplateName());
				loreList.add(loreColor+"Change the monument template for all towns.");
				icon = new InfoIcon(masterColor+"Monument Template", loreList, Material.ANVIL, ROOT_SLOT_TEMPLATE, true);
				result.addIcon(icon);
				
				/* Disband Icon */
				loreList.clear();
				loreList.add(loreColor+MessagePath.MENU_GUILD_DESCRIPTION_DISBAND.getMessage());
				icon = new InfoIcon(masterColor+MessagePath.MENU_GUILD_DISBAND.getMessage(), loreList, Material.CREEPER_HEAD, ROOT_SLOT_DISBAND, true);
				result.addIcon(icon);
			}
		}
		
		return result;
	}
	
	private DisplayMenu createExileView() {
		DisplayMenu result;
		InfoIcon icon;
		List<String> loreList = new ArrayList<>();
		result = new DisplayMenu(2, getTitle(MenuState.A_EXILE));

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
		List<String> loreList = new ArrayList<>();
		result = new DisplayMenu(2, getTitle(MenuState.C_DISBAND));

		loreList.add(hintColor+MessagePath.MENU_GUILD_HINT_DISBAND.getMessage());
		icon = new InfoIcon(DisplayManager.boolean2Symbol(true), loreList, Material.GLOWSTONE_DUST, SLOT_YES, true);
		result.addIcon(icon);
		
		loreList.clear();
		loreList.add(hintColor+MessagePath.MENU_GUILD_HINT_EXIT.getMessage());
		icon = new InfoIcon(DisplayManager.boolean2Symbol(false), loreList, Material.REDSTONE, SLOT_NO, true);
		result.addIcon(icon);
		return result;
	}
	
	private DisplayMenu createTemplateView() {
		DisplayMenu result;
		pages.clear();
		currentPage = 0;
		List<KonMonumentTemplate> templates = new ArrayList<>(konquest.getSanctuaryManager().getAllTemplates());
		templates.remove(kingdom.getMonumentTemplate());
		
		// Create page(s)
		String pageLabel;
		List<String> loreList;
		int pageTotal = getTotalPages(templates.size());
		int pageNum = 0;
		ListIterator<KonMonumentTemplate> listIter = templates.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int pageRows = getNumPageRows(templates.size(), i);
			pageLabel = getTitle(MenuState.C_TEMPLATE)+" "+(i+1)+"/"+pageTotal;
			pages.add(pageNum, new DisplayMenu(pageRows+1, pageLabel));
			int slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && listIter.hasNext()) {
				/* Template Icon (n) */
				KonMonumentTemplate template = listIter.next();
				loreList = new ArrayList<>();
				boolean isClickable = true;
				if(template.isValid()) {
					if(!isAdmin) {
						double totalCost = manager.getCostTemplate() + template.getCost();
						String cost = String.format("%.2f",totalCost);
						loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+cost);
					}
					loreList.add(hintColor+"Click to apply");
				} else {
					// Invalid template, check for blanking
					loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+"X");
					isClickable = false;
				}
				TemplateIcon templateIcon = new TemplateIcon(template,ChatColor.GOLD,loreList,slotIndex,isClickable);
		    	pages.get(pageNum).addIcon(templateIcon);
				slotIndex++;
			}
			pageNum++;
		}
		result = pages.get(currentPage);
		return result;
	}
	
	/*
	 * Visualizing diplomacy
	 *
	 * Relationships view lists all other kingdoms and their relationship, with icon format:
	 * 		Colored by current relation
	 * 		Name = Kingdom name
	 * 		Lore0 = Currently: <active relation>
	 * 		Lore1 = +-----------+
	 * 		Lore2 = Our Stance: <our request relation>
	 * 		Lore3 = Their Stance: <their request relation>
	 *      Lore4 = Click to change our stance
	 *
	 * Diplomacy view shows info on chosen kingdom, and all relationship options.
	 * Show same kingdom icon (not clickable) and all relationships with context tips.
	 * 		Name = <Relationship>
	 * 		Lore0 = <context description based on instant war/peace, their stance>
	 * 		Lore1 = <click to enact, if clickable based on context/conditions>
	 */


	private DisplayMenu createDiplomacyView() {
		//B_DIPLOMACY
		// diplomacyKingdom is the global variable to keep track of current kingdom changing status
		
		DisplayMenu result;
		RelationIcon icon;
		
		int numIcons = KonquestRelationship.values().length + 2; // Add 2 for kingdom info and spacer
		int numRows = (int)Math.ceil((double)numIcons / 9);
		result = new DisplayMenu(numRows+1, getTitle(MenuState.B_DIPLOMACY));
		int index = 0;
		List<String> loreList;
		
		// Create kingdom info
		ChatColor contextColor = konquest.getDisplayPrimaryColor(kingdom, diplomacyKingdom);
		loreList = new ArrayList<>();
		String ourStatus = kingdom.getActiveRelation(diplomacyKingdom).getLabel();
		String theirStatus = diplomacyKingdom.getActiveRelation(kingdom).getLabel();
		String ourRequestStatus = kingdom.getRelationRequest(diplomacyKingdom).getLabel();
		String theirRequestStatus = diplomacyKingdom.getRelationRequest(kingdom).getLabel();
		loreList.add(loreColor+"Our Stance"+": "+valueColor+ourStatus);
		loreList.add(loreColor+"Their Stance"+": "+valueColor+theirStatus);
		loreList.add(loreColor+"+-------------+");
		loreList.add(loreColor+"Our Request"+": "+valueColor+ourRequestStatus);
		loreList.add(loreColor+"Their Request"+": "+valueColor+theirRequestStatus);
		KingdomIcon kingdomIcon = new KingdomIcon(diplomacyKingdom,contextColor,loreList,index,false);
		result.addIcon(kingdomIcon);
		index = 2;
		
		// Only create relation options for created kingdoms
		if(isCreatedKingdom) {
			for(KonquestRelationship relation : KonquestRelationship.values()) {
				// Determine context lore for this relation and the stance of kingdom with diplomacyKingdom
				loreList = new ArrayList<>();
				// Is this relation a valid option in the current relationship?
				boolean isValidChoice = manager.isValidRelationChoice(kingdom, diplomacyKingdom, relation);
				ChatColor relationColor = ChatColor.GRAY;
				String description = "Unavailable";
				boolean isClickable = false;
				//TODO: Update context logic, message paths
				if(isValidChoice) {
					relationColor = ChatColor.GOLD;
					switch(relation) {
						case PEACE:
							description = "Request for peace";
							break;
						case SANCTIONED:
							description = "Stop trade, enable pvp?";
							break;
						case ENEMY:
							description = "Declare war on an enemy";
							break;
						case ALLIED:
							description = "Request for alliance";
							break;
						default:
							break;
					}
				}
				loreList.add(relationColor+description);
				if(isValidChoice) {
					if(!isAdmin) {
						String cost = String.format("%.2f",manager.getCostRelation());
						loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+cost);
					}
					loreList.add(hintColor+"Click to enact");
					isClickable = true;
				}
				// Create icon
				icon = new RelationIcon(relation,loreList,index,isClickable);
				result.addIcon(icon);
				index++;
			}
		}
		return result;
	}
	
	
	private DisplayMenu createKingdomView(MenuState context) {
		// A paged view of kingdoms, with lore based on context
		DisplayMenu result;
		pages.clear();
		currentPage = 0;
		String loreHintStr1 = "";
		String loreHintStr2 = "";
		boolean isClickable = false;
		List<KonKingdom> kingdoms = new ArrayList<>();
		
		// Determine list of kingdoms given context
		if(context.equals(MenuState.A_JOIN)) {
			// List of all valid kingdoms able to join (sends request)
			kingdoms.addAll(manager.getKingdoms());
			if(isCreatedKingdom) {
				kingdoms.remove(kingdom);
			}
			loreHintStr1 = MessagePath.MENU_GUILD_HINT_JOIN.getMessage();
			isClickable = true;
		} else if(context.equals(MenuState.A_INVITE)) {
			// List of kingdoms with valid join invite for player
			kingdoms.addAll(manager.getInviteKingdoms(player));
			if(isCreatedKingdom) {
				kingdoms.remove(kingdom);
			}
			loreHintStr1 = MessagePath.MENU_GUILD_HINT_INVITE_1.getMessage();
			loreHintStr2 = MessagePath.MENU_GUILD_HINT_INVITE_2.getMessage();
			isClickable = true;
		} else if(context.equals(MenuState.A_LIST)) {
			// List of all kingdoms, friendly and enemy, with normal info
			kingdoms.addAll(manager.getKingdoms());
		} else if(context.equals(MenuState.B_RELATIONSHIP)) {
			// List of all kingdoms, friendly and enemy, with relationship status and click hints
			kingdoms.addAll(manager.getKingdoms());
			if(isCreatedKingdom) {
				kingdoms.remove(kingdom);
			}
			loreHintStr1 = MessagePath.MENU_GUILD_HINT_RELATION.getMessage();
			isClickable = true;
		} else {
			return null;
		}
		// Sort list by land then population
		kingdoms.sort(kingdomComparator);
		
		// Create page(s)
		String pageLabel;
		List<String> loreList;
		int pageTotal = getTotalPages(kingdoms.size());
		int pageNum = 0;
		ListIterator<KonKingdom> listIter = kingdoms.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int pageRows = getNumPageRows(kingdoms.size(), i);
			pageLabel = getTitle(context)+" "+(i+1)+"/"+pageTotal;
			pages.add(pageNum, new DisplayMenu(pageRows+1, pageLabel));
			int slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && listIter.hasNext()) {
				/* Kingdom Icon (n) */
				KonKingdom currentKingdom = listIter.next();
				ChatColor contextColor = konquest.getDisplayPrimaryColor(kingdom, currentKingdom);
				loreList = new ArrayList<>();
				if(isCreatedKingdom) {
					String ourStatus = kingdom.getActiveRelation(currentKingdom).getLabel();
					String theirStatus = currentKingdom.getActiveRelation(kingdom).getLabel();
					loreList.add(loreColor+MessagePath.MENU_GUILD_OUR_STATUS.getMessage()+": "+valueColor+ourStatus);
					loreList.add(loreColor+MessagePath.MENU_GUILD_THEIR_STATUS.getMessage()+": "+valueColor+theirStatus);
				}
				if(!loreHintStr1.equals("")) {
					loreList.add(hintColor+loreHintStr1);
				}
				if(!loreHintStr2.equals("")) {
					loreList.add(hintColor+loreHintStr2);
				}
				KingdomIcon kingdomIcon = new KingdomIcon(currentKingdom,contextColor,loreList,slotIndex,isClickable);
		    	pages.get(pageNum).addIcon(kingdomIcon);
				slotIndex++;
			}
			pageNum++;
		}
		result = pages.get(currentPage);
		return result;
	}
	
	private DisplayMenu createPlayerView(MenuState context) {
		// A paged view of players, with lore based on context
		DisplayMenu result;
		final int MAX_ICONS_PER_PAGE = 45;
		pages.clear();
		currentPage = 0;
		String loreHintStr1;
		String loreHintStr2 = "";
		PlayerIconAction iconAction = PlayerIconAction.DISPLAY_INFO;
		boolean isClickable;
		List<OfflinePlayer> players = new ArrayList<>();
		
		// Determine list of players given context
		if(context.equals(MenuState.B_REQUESTS)) {
			players.addAll(kingdom.getJoinRequests());
			loreHintStr1 = MessagePath.MENU_GUILD_HINT_REQUEST_1.getMessage();
			loreHintStr2 = MessagePath.MENU_GUILD_HINT_REQUEST_2.getMessage();
			isClickable = true;
		} else if(context.equals(MenuState.C_PROMOTE)) {
			players.addAll(kingdom.getPlayerMembersOnly());
			loreHintStr1 = MessagePath.MENU_GUILD_HINT_PROMOTE.getMessage();
			isClickable = true;
		} else if(context.equals(MenuState.C_DEMOTE)) {
			players.addAll(kingdom.getPlayerOfficersOnly());
			loreHintStr1 = MessagePath.MENU_GUILD_HINT_DEMOTE.getMessage();
			isClickable = true;
		} else if(context.equals(MenuState.C_TRANSFER)) {
			players.addAll(kingdom.getPlayerOfficersOnly());
			players.addAll(kingdom.getPlayerMembersOnly());
			loreHintStr1 = MessagePath.MENU_GUILD_HINT_TRANSFER.getMessage();
			isClickable = true;
		} else {
			return null;
		}
		
		// Create page(s)
		String pageLabel;
		List<String> loreList;
		int pageTotal = getTotalPages(players.size());
		int pageNum = 0;
		ListIterator<OfflinePlayer> listIter = players.listIterator();
		for(int i = 0; i < pageTotal; i++) {
			int pageRows = getNumPageRows(players.size(), i);
			pageLabel = getTitle(context)+" "+(i+1)+"/"+pageTotal;
			pages.add(pageNum, new DisplayMenu(pageRows+1, pageLabel));
			int slotIndex = 0;
			while(slotIndex < MAX_ICONS_PER_PAGE && listIter.hasNext()) {
				/* Player Icon (n) */
				OfflinePlayer currentPlayer = listIter.next();
				loreList = new ArrayList<>();
				String kingdomRole = kingdom.getPlayerRoleName(currentPlayer);
				if(!kingdomRole.equals("")) {
					loreList.add(kingdomRole);
				}
				if(!loreHintStr1.equals("")) {
					loreList.add(hintColor+loreHintStr1);
				}
				if(!loreHintStr2.equals("")) {
					loreList.add(hintColor+loreHintStr2);
				}
		    	PlayerIcon playerIcon = new PlayerIcon(ChatColor.LIGHT_PURPLE+currentPlayer.getName(),loreList,currentPlayer,slotIndex,isClickable,iconAction);
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
			// (back [0]) close [4], return [5], (next [8])
			if(index == 0) {
				result = goPageBack();
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
			MenuState currentMenuState = (MenuState)currentState;
			switch(currentMenuState) {
				case ROOT:
					if(slot == ROOT_SLOT_JOIN) {
						// Clicked to join a kingdom
						// Allow any player to always go to the join view
						currentState = MenuState.A_JOIN;
						result = goToKingdomView(MenuState.A_JOIN);
						
					} else if(slot == ROOT_SLOT_EXILE) {
						// Clicked to exile from their kingdom
						if(isCreatedKingdom) {
							currentState = MenuState.A_EXILE;
							result = views.get(currentState);
						} else {
							ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_GUILD_ERROR_NO_GUILD.getMessage());
							Konquest.playFailSound(player.getBukkitPlayer());
						}
						
					} else if(slot == ROOT_SLOT_INVITE) {
						// Clicked to view invites
						currentState = MenuState.A_INVITE;
						result = goToKingdomView(MenuState.A_INVITE);
						
					} else if(slot == ROOT_SLOT_LIST) {
						// Clicked to view all kingdom list
						currentState = MenuState.A_LIST;
						result = goToKingdomView(MenuState.A_LIST);
						
					} else if(slot == ROOT_SLOT_RELATIONSHIPS) {
						// Clicked to modify relationships
						currentState = MenuState.B_RELATIONSHIP;
						result = goToKingdomView(MenuState.B_RELATIONSHIP);
						
					} else if(slot == ROOT_SLOT_REQUESTS) {
						// Clicked to view join requests
						currentState = MenuState.B_REQUESTS;
						result = goToPlayerView(MenuState.B_REQUESTS);
						
					} else if(slot == ROOT_SLOT_PROMOTE) {
						// Clicked to view members to promote
						currentState = MenuState.C_PROMOTE;
						result = goToPlayerView(MenuState.C_PROMOTE);
						
					} else if(slot == ROOT_SLOT_DEMOTE) {
						// Clicked to view officers to demote
						currentState = MenuState.C_DEMOTE;
						result = goToPlayerView(MenuState.C_DEMOTE);
						
					} else if(slot == ROOT_SLOT_TRANSFER) {
						// Clicked to view members to transfer master
						currentState = MenuState.C_TRANSFER;
						result = goToPlayerView(MenuState.C_TRANSFER);
						
					} else if(slot == ROOT_SLOT_DISBAND) {
						// Clicked to disband kingdom
						currentState = MenuState.C_DISBAND;
						result = goToDisbandView();
						
					}  else if(slot == ROOT_SLOT_OPEN) {
						// Clicked to toggle open/closed state
						manager.menuToggleKingdomOpen(kingdom, player);
						Konquest.playSuccessSound(player.getBukkitPlayer());
						
					} else if(slot == ROOT_SLOT_TEMPLATE) {
						// Clicked to view template selection
						currentState = MenuState.C_TEMPLATE;
						result = goToTemplateView();
						
					}
					break;
				case A_JOIN:
					if(clickedIcon instanceof KingdomIcon) {
						KingdomIcon icon = (KingdomIcon)clickedIcon;
						KonKingdom clickKingdom = icon.getKingdom();
						manager.menuJoinKingdomRequest(player, clickKingdom);
					}
					break;
				case A_EXILE:
					if(slot == SLOT_YES) {
						// Exile the player
						manager.menuExileKingdom(player, kingdom);
					}
					break;
				case A_INVITE:
					if(clickedIcon instanceof KingdomIcon) {
						KingdomIcon icon = (KingdomIcon)clickedIcon;
						KonKingdom clickKingdom = icon.getKingdom();
						boolean status = manager.menuRespondKingdomInvite(player, clickKingdom, clickType);
						if(!status) {
							// Invite declined, player assignment unchanged
							result = goToKingdomView(MenuState.A_INVITE);
						}
					}
					break;
				case A_LIST:
					// Do nothing for now
					break;
				case B_RELATIONSHIP:
					// Clicking icons goes to diplomacy view
					if(clickedIcon instanceof KingdomIcon) {
						KingdomIcon icon = (KingdomIcon)clickedIcon;
						diplomacyKingdom = icon.getKingdom();
						currentState = MenuState.B_DIPLOMACY;
						result = goToDiplomacyView();
					}
					break;
				case B_DIPLOMACY:
					// Clicking changes the relationship of kingdoms
					if(clickedIcon instanceof RelationIcon) {
						RelationIcon icon = (RelationIcon)clickedIcon;
						KonquestRelationship clickRelation = icon.getRelation();
						boolean status = manager.menuChangeKingdomRelation(kingdom, diplomacyKingdom, clickRelation, player, isAdmin);
						diplomacyKingdom = null;
						if(status) {
							// Return to relationship view
							currentState = MenuState.B_RELATIONSHIP;
							result = goToKingdomView(MenuState.B_RELATIONSHIP);
							Konquest.playSuccessSound(player.getBukkitPlayer());
						} else {
							//TODO: update messaging, in manager method
							Konquest.playFailSound(player.getBukkitPlayer());
						}
					}
					break;
				case B_REQUESTS:
					if(clickedIcon instanceof PlayerIcon) {
						PlayerIcon icon = (PlayerIcon)clickedIcon;
						OfflinePlayer clickPlayer = icon.getOfflinePlayer();
						boolean status = manager.menuRespondKingdomRequest(clickPlayer, kingdom, clickType);
						if(status) {
							if(clickType) {
								ChatUtil.sendNotice(player.getBukkitPlayer(),"Accepted kingdom join request for player "+clickPlayer.getName());
							} else {
								ChatUtil.sendNotice(player.getBukkitPlayer(),"Denied kingdom join request for player "+clickPlayer.getName());
							}
							Konquest.playSuccessSound(player.getBukkitPlayer());
						} else {
							ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_GUILD_ERROR_PLAYER_GUILD.getMessage(clickPlayer.getName()));
							Konquest.playFailSound(player.getBukkitPlayer());
						}
						result = goToPlayerView(MenuState.B_REQUESTS);
					}
					break;
				case C_PROMOTE:
					if(clickedIcon instanceof PlayerIcon) {
						PlayerIcon icon = (PlayerIcon)clickedIcon;
						OfflinePlayer clickPlayer = icon.getOfflinePlayer();
						boolean status = manager.menuPromoteOfficer(clickPlayer, kingdom);
						if(status) {
							Konquest.playSuccessSound(player.getBukkitPlayer());
						}
						result = goToPlayerView(MenuState.C_PROMOTE);
					}
					break;
				case C_DEMOTE:
					if(clickedIcon instanceof PlayerIcon) {
						PlayerIcon icon = (PlayerIcon)clickedIcon;
						OfflinePlayer clickPlayer = icon.getOfflinePlayer();
						boolean status = manager.menuDemoteOfficer(clickPlayer, kingdom);
						if(status) {
							Konquest.playSuccessSound(player.getBukkitPlayer());
						}
						result = goToPlayerView(MenuState.C_DEMOTE);
					}
					break;
				case C_TRANSFER:
					if(clickedIcon instanceof PlayerIcon) {
						PlayerIcon icon = (PlayerIcon)clickedIcon;
						OfflinePlayer clickPlayer = icon.getOfflinePlayer();
						manager.menuTransferMaster(clickPlayer, kingdom, player);
					}
					break;
				case C_TEMPLATE:
					if(clickedIcon instanceof TemplateIcon) {
						TemplateIcon icon = (TemplateIcon)clickedIcon;
						KonMonumentTemplate template = icon.getTemplate();
						manager.menuChangeKingdomTemplate(kingdom, template, player, isAdmin);
					}
					break;
				case C_DISBAND:
					if(slot == SLOT_YES) {
						manager.menuDisbandKingdom(kingdom,player);
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
		String result = "error";
		ChatColor color = ChatColor.BLACK;
		if(isAdmin) {
			color = ChatColor.GOLD;
		}
		String name = "";
		if(kingdom != null) {
			name = kingdom.getName();
		}
		switch(context) {
			case ROOT:
				if(isCreatedKingdom) {
					String titleAccess = "";
					if(menuAccess.equals(AccessType.REGULAR)) {
						titleAccess = MessagePath.LABEL_MEMBER.getMessage();
					} else if(menuAccess.equals(AccessType.OFFICER)) {
						titleAccess = MessagePath.LABEL_OFFICER.getMessage();
					} else if(menuAccess.equals(AccessType.MASTER)) {
						titleAccess = MessagePath.LABEL_MASTER.getMessage();
					}
					result = color+name+" "+MessagePath.LABEL_GUILD.getMessage()+" "+titleAccess;
				} else {
					result = color+MessagePath.MENU_GUILD_TITLE_ROOT.getMessage();
				}
				break;
			case A_JOIN:
				result = color+MessagePath.MENU_GUILD_TITLE_JOIN.getMessage();
				break;
			case A_EXILE:
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
			case B_DIPLOMACY:
				//TODO: path this
				result = color+"Diplomacy";
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
			case C_TEMPLATE:
				//TODO: update this
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
	
	private DisplayMenu goToKingdomView(MenuState context) {
		DisplayMenu result = createKingdomView(context);
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
	
	private DisplayMenu goToDiplomacyView() {
		DisplayMenu result = createDiplomacyView();
		views.put(MenuState.B_DIPLOMACY, result);
		return result;
	}
	
	private DisplayMenu goToDisbandView() {
		DisplayMenu result = createDisbandView();
		views.put(MenuState.C_DISBAND, result);
		return result;
	}
	
	private DisplayMenu goToTemplateView() {
		DisplayMenu result = createTemplateView();
		views.put(MenuState.C_TEMPLATE, result);
		return result;
	}
	
	/**
	 * Place all navigation button icons on view given context and update icons
	 */
	void refreshNavigationButtons(State context) {
		DisplayMenu view = views.get(context);
		int navStart = view.getInventory().getSize()-9;
		if(navStart < 0) {
			ChatUtil.printDebug("Guild menu nav buttons failed to refresh in context "+context.toString());
			return;
		}
		if(context.equals(MenuState.ROOT)) {
			// Close [4]
			view.addIcon(navIconEmpty(navStart));
			view.addIcon(navIconEmpty(navStart+1));
			view.addIcon(navIconEmpty(navStart+2));
			view.addIcon(navIconEmpty(navStart+3));
			view.addIcon(navIconClose(navStart+4));
			view.addIcon(navIconEmpty(navStart+5));
			view.addIcon(navIconEmpty(navStart+6));
			view.addIcon(navIconEmpty(navStart+7));
			view.addIcon(navIconEmpty(navStart+8));
		} else if(context.equals(MenuState.A_EXILE) || context.equals(MenuState.B_DIPLOMACY) || context.equals(MenuState.C_TEMPLATE) || context.equals(MenuState.C_DISBAND)) {
			// Close [4], Return [5]
			view.addIcon(navIconEmpty(navStart));
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
				view.addIcon(navIconBack(navStart));
			} else {
				view.addIcon(navIconEmpty(navStart));
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
	
}
