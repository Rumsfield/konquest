package com.github.rumsfield.konquest.display;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestDiplomacyType;
import com.github.rumsfield.konquest.display.icon.*;
import com.github.rumsfield.konquest.display.icon.PlayerIcon.PlayerIconAction;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.manager.KingdomManager;
import com.github.rumsfield.konquest.model.KonKingdom;
import com.github.rumsfield.konquest.model.KonMonumentTemplate;
import com.github.rumsfield.konquest.model.KonPlayer;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.Labeler;
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

	private final String propertyColor = DisplayManager.propertyFormat;
	private final String alertColor = DisplayManager.alertFormat;
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
	
	public KingdomMenu(Konquest konquest, KonPlayer player, KonKingdom kingdom, boolean isAdmin) {
		super(konquest, MenuState.ROOT, AccessType.REGULAR);
		this.manager = konquest.getKingdomManager();
		this.player = player;
		this.kingdom = kingdom;
		this.diplomacyKingdom = null;
		this.isCreatedKingdom = false; // Is this kingdom created by players, i.e. not barbarians or neutrals
		this.isServerKingdom = false; // Is this kingdom created by an admin for the server
		this.isAdmin = isAdmin;
		
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
		ChatColor kingdomColor = Konquest.friendColor1;
		
		int rows = 2; // default rows for regular
		if(menuAccess.equals(AccessType.OFFICER)) {
			rows = 3;
		} else if(menuAccess.equals(AccessType.MASTER)) {
			rows = 4;
		}
		
		result = new DisplayMenu(rows, getTitle(MenuState.ROOT));
		
		/* Join Icon */
		loreList.addAll(Konquest.stringPaginate(MessagePath.MENU_KINGDOM_DESCRIPTION_JOIN.getMessage(),loreColor));
		loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_OPEN.getMessage());
		icon = new InfoIcon(kingdomColor+MessagePath.MENU_KINGDOM_JOIN.getMessage(), loreList, Material.SADDLE, ROOT_SLOT_JOIN, true);
		result.addIcon(icon);
		
		/* Exile Icon */
		loreList.clear();
		loreList.addAll(Konquest.stringPaginate(MessagePath.MENU_KINGDOM_DESCRIPTION_EXILE.getMessage(),loreColor));
		loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_OPEN.getMessage());
		icon = new InfoIcon(kingdomColor+MessagePath.MENU_KINGDOM_EXILE.getMessage(), loreList, Material.ARROW, ROOT_SLOT_EXILE, true);
		result.addIcon(icon);

		/* Invites Icon */
		loreList.clear();
		loreList.addAll(Konquest.stringPaginate(MessagePath.MENU_KINGDOM_DESCRIPTION_INVITES.getMessage(),loreColor));
		int numInvites = manager.getInviteKingdoms(player).size();
		Material inviteMat = Material.BOOK;
		if(numInvites > 0) {
			loreList.add(valueColor+""+numInvites);
			inviteMat = Material.WRITABLE_BOOK;
		}
		loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_OPEN.getMessage());
		icon = new InfoIcon(kingdomColor+MessagePath.MENU_KINGDOM_INVITES.getMessage(), loreList, inviteMat, ROOT_SLOT_INVITE, true);
		result.addIcon(icon);
		
		/* List Icon */
		loreList.clear();
		loreList.addAll(Konquest.stringPaginate(MessagePath.MENU_KINGDOM_DESCRIPTION_LIST.getMessage(),loreColor));
		loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_OPEN.getMessage());
		icon = new InfoIcon(kingdomColor+MessagePath.MENU_KINGDOM_LIST.getMessage(), loreList, Material.PAPER, ROOT_SLOT_LIST, true);
		result.addIcon(icon);
		
		// These icons only appear for created kingdoms
		if(isCreatedKingdom) {
			/* Kingdom Info Icon */
			loreList = new ArrayList<>();
			loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_LIST.getMessage());
			icon = new KingdomIcon(kingdom,kingdomColor,loreList,ROOT_SLOT_INFO,true);
			result.addIcon(icon);

			if(menuAccess.equals(AccessType.OFFICER) || menuAccess.equals(AccessType.MASTER)) {
				/* Relations Icon */
				loreList = new ArrayList<>();
				loreList.add(propertyColor+MessagePath.LABEL_OFFICER.getMessage());
				loreList.addAll(Konquest.stringPaginate(MessagePath.MENU_KINGDOM_DESCRIPTION_RELATION.getMessage(),loreColor));
				loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_OPEN.getMessage());
				icon = new InfoIcon(kingdomColor+MessagePath.MENU_KINGDOM_RELATION.getMessage(), loreList, Material.GOLDEN_SWORD, ROOT_SLOT_RELATIONSHIPS, true);
				result.addIcon(icon);
				
				/* Requests Icon */
				loreList.clear();
				loreList.add(propertyColor+MessagePath.LABEL_OFFICER.getMessage());
				loreList.addAll(Konquest.stringPaginate(MessagePath.MENU_KINGDOM_DESCRIPTION_REQUESTS.getMessage(),loreColor));
				int numRequests = kingdom.getJoinRequests().size();
				Material requestMat = Material.GLASS_BOTTLE;
				if(numRequests > 0) {
					loreList.add(valueColor+""+numRequests);
					requestMat = Material.HONEY_BOTTLE;
				}
				loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_OPEN.getMessage());
				icon = new InfoIcon(kingdomColor+MessagePath.MENU_KINGDOM_REQUESTS.getMessage(), loreList, requestMat, ROOT_SLOT_REQUESTS, true);
				result.addIcon(icon);
			}
			
			if(menuAccess.equals(AccessType.MASTER)) {
				/* Promote Icon */
				loreList = new ArrayList<>();
				loreList.add(propertyColor+MessagePath.LABEL_MASTER.getMessage());
				loreList.addAll(Konquest.stringPaginate(MessagePath.MENU_KINGDOM_DESCRIPTION_PROMOTE.getMessage(),loreColor));
				loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_OPEN.getMessage());
				icon = new InfoIcon(kingdomColor+MessagePath.MENU_KINGDOM_PROMOTE.getMessage(), loreList, Material.DIAMOND_HORSE_ARMOR, ROOT_SLOT_PROMOTE, true);
				result.addIcon(icon);
				
				/* Demote Icon */
				loreList.clear();
				loreList.add(propertyColor+MessagePath.LABEL_MASTER.getMessage());
				loreList.addAll(Konquest.stringPaginate(MessagePath.MENU_KINGDOM_DESCRIPTION_DEMOTE.getMessage(),loreColor));
				loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_OPEN.getMessage());
				icon = new InfoIcon(kingdomColor+MessagePath.MENU_KINGDOM_DEMOTE.getMessage(), loreList, Material.LEATHER_HORSE_ARMOR, ROOT_SLOT_DEMOTE, true);
				result.addIcon(icon);
				
				/* Transfer Icon */
				if(!isServerKingdom) {
					loreList.clear();
					loreList.add(propertyColor+MessagePath.LABEL_MASTER.getMessage());
					loreList.addAll(Konquest.stringPaginate(MessagePath.MENU_KINGDOM_DESCRIPTION_TRANSFER.getMessage(),loreColor));
					loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_OPEN.getMessage());
					icon = new InfoIcon(kingdomColor+MessagePath.MENU_KINGDOM_TRANSFER.getMessage(), loreList, Material.ELYTRA, ROOT_SLOT_TRANSFER, true);
					result.addIcon(icon);
				}
				
				/* Open/Close Button */
				String currentValue = DisplayManager.boolean2Lang(kingdom.isOpen())+" "+DisplayManager.boolean2Symbol(kingdom.isOpen());
				loreList.clear();
				loreList.add(propertyColor+MessagePath.LABEL_MASTER.getMessage());
		    	loreList.add(loreColor+MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor+currentValue));
		    	loreList.add(hintColor+MessagePath.MENU_OPTIONS_HINT.getMessage());
				loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_OPEN.getMessage());
				icon = new InfoIcon(kingdomColor+MessagePath.LABEL_OPEN.getMessage(), loreList, Material.IRON_DOOR, ROOT_SLOT_OPEN, true);
				result.addIcon(icon);
				
				/* Template Icon */
				loreList.clear();
				loreList.add(propertyColor+MessagePath.LABEL_MASTER.getMessage());
				loreList.addAll(Konquest.stringPaginate(MessagePath.MENU_KINGDOM_DESCRIPTION_TEMPLATE.getMessage(),loreColor));
				loreList.add(loreColor+MessagePath.MENU_OPTIONS_CURRENT.getMessage(valueColor+kingdom.getMonumentTemplateName()));
				loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_OPEN.getMessage());
				icon = new InfoIcon(kingdomColor+MessagePath.MENU_KINGDOM_TEMPLATE.getMessage(), loreList, Material.ANVIL, ROOT_SLOT_TEMPLATE, true);
				result.addIcon(icon);
				
				/* Disband Icon */
				loreList.clear();
				loreList.add(propertyColor+MessagePath.LABEL_MASTER.getMessage());
				loreList.addAll(Konquest.stringPaginate(MessagePath.MENU_KINGDOM_DESCRIPTION_DISBAND.getMessage(),loreColor));
				loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_OPEN.getMessage());
				icon = new InfoIcon(kingdomColor+MessagePath.MENU_KINGDOM_DISBAND.getMessage(), loreList, Material.CREEPER_HEAD, ROOT_SLOT_DISBAND, true);
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

		loreList.add(loreColor+MessagePath.MENU_KINGDOM_HINT_EXILE.getMessage());
		icon = new InfoIcon(DisplayManager.boolean2Symbol(true), loreList, Material.GLOWSTONE_DUST, SLOT_YES, true);
		result.addIcon(icon);
		
		loreList.clear();
		loreList.add(loreColor+MessagePath.MENU_KINGDOM_HINT_EXIT.getMessage());
		icon = new InfoIcon(DisplayManager.boolean2Symbol(false), loreList, Material.REDSTONE, SLOT_NO, true);
		result.addIcon(icon);
		return result;
	}
	
	private DisplayMenu createDisbandView() {
		DisplayMenu result;
		InfoIcon icon;
		List<String> loreList = new ArrayList<>();
		result = new DisplayMenu(2, getTitle(MenuState.C_DISBAND));

		loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_DISBAND.getMessage());
		icon = new InfoIcon(DisplayManager.boolean2Symbol(true), loreList, Material.GLOWSTONE_DUST, SLOT_YES, true);
		result.addIcon(icon);
		
		loreList.clear();
		loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_EXIT.getMessage());
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
					if(!isAdmin && kingdom.hasMonumentTemplate()) {
						double totalCost = manager.getCostTemplate() + template.getCost();
						String cost = String.format("%.2f",totalCost);
						loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+cost);
					}
					loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_TEMPLATE.getMessage());
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

	private DisplayMenu createDiplomacyView() {
		// diplomacyKingdom is the global variable to keep track of current kingdom changing status
		
		DisplayMenu result;
		DiplomacyIcon icon;
		
		int numIcons = KonquestDiplomacyType.values().length + 2; // Add 2 for kingdom info and spacer
		int numRows = (int)Math.ceil((double)numIcons / 9);
		result = new DisplayMenu(numRows+1, getTitle(MenuState.B_DIPLOMACY));
		int index = 0;
		List<String> loreList;

		KonquestDiplomacyType currentDiplomacy = manager.getDiplomacy(kingdom,diplomacyKingdom);
		
		// Create kingdom info
		ChatColor contextColor = konquest.getDisplayPrimaryColor(kingdom, diplomacyKingdom);
		loreList = new ArrayList<>();
		String diplomacyState = Labeler.lookup(currentDiplomacy);
		loreList.add(loreColor+MessagePath.LABEL_DIPLOMACY.getMessage()+": "+valueColor+diplomacyState);
		if(kingdom.hasRelationRequest(diplomacyKingdom)) {
			// They have sent a valid diplomacy change request to us
			String ourRequestStatus = Labeler.lookup(kingdom.getRelationRequest(diplomacyKingdom));
			loreList.add(alertColor+MessagePath.MENU_KINGDOM_THEY_REQUESTED.getMessage()+": "+valueColor+ourRequestStatus);
		}
		if(diplomacyKingdom.hasRelationRequest(kingdom)) {
			// We have sent a valid diplomacy change request to them
			String theirRequestStatus = Labeler.lookup(diplomacyKingdom.getRelationRequest(kingdom));
			loreList.add(alertColor+MessagePath.MENU_KINGDOM_WE_REQUESTED.getMessage()+": "+valueColor+theirRequestStatus);
		}
		KingdomIcon kingdomIcon = new KingdomIcon(diplomacyKingdom,contextColor,loreList,index,false);
		result.addIcon(kingdomIcon);
		
		// Only create relation options for created kingdoms
		index = 2;
		if(isCreatedKingdom) {
			// Does any change to war by one side instantly force the other into war?
			boolean isInstantWar = konquest.getCore().getBoolean(CorePath.KINGDOMS_INSTANT_WAR.getPath(), false);
			// Does a change from war to peace by one side instantly force the other into peace?
			boolean isInstantPeace = konquest.getCore().getBoolean(CorePath.KINGDOMS_INSTANT_PEACE.getPath(), false);

			for(KonquestDiplomacyType relation : KonquestDiplomacyType.values()) {
				// Determine context lore for this relation and the stance of kingdom with diplomacyKingdom
				loreList = new ArrayList<>();
				// Is this relation a valid option in the current relationship?
				boolean isValidChoice = manager.isValidRelationChoice(kingdom, diplomacyKingdom, relation);
				ChatColor relationColor = ChatColor.GRAY;
				String description = MessagePath.LABEL_UNAVAILABLE.getMessage();
				String detailedInfo = "";
				boolean isClickable = false;
				if(isValidChoice) {
					relationColor = ChatColor.GOLD;
					switch(relation) {
						case PEACE:
							// Context descriptions
							if(currentDiplomacy.equals(KonquestDiplomacyType.WAR)) {
								if(isInstantPeace) {
									description = MessagePath.MENU_KINGDOM_DIPLOMACY_PEACE_WAR_INSTANT.getMessage();
								} else {
									description = MessagePath.MENU_KINGDOM_DIPLOMACY_PEACE_WAR_REQUEST.getMessage();
								}
							} else if(currentDiplomacy.equals(KonquestDiplomacyType.TRADE)) {
								description = MessagePath.MENU_KINGDOM_DIPLOMACY_PEACE_TRADE.getMessage();
							} else if(currentDiplomacy.equals(KonquestDiplomacyType.ALLIANCE)) {
								description = MessagePath.MENU_KINGDOM_DIPLOMACY_PEACE_ALLIANCE.getMessage();
							}
							// Detailed Info
							detailedInfo = MessagePath.MENU_KINGDOM_DIPLOMACY_PEACE_INFO.getMessage();
							break;
						case TRADE:
							if(currentDiplomacy.equals(KonquestDiplomacyType.PEACE)) {
								description = MessagePath.MENU_KINGDOM_DIPLOMACY_TRADE_PEACE.getMessage();
							} else if(currentDiplomacy.equals(KonquestDiplomacyType.ALLIANCE)) {
								description = MessagePath.MENU_KINGDOM_DIPLOMACY_TRADE_ALLIANCE.getMessage();
							}
							// Detailed Info
							detailedInfo = MessagePath.MENU_KINGDOM_DIPLOMACY_TRADE_INFO.getMessage();
							break;
						case WAR:
							if(isInstantWar) {
								description = MessagePath.MENU_KINGDOM_DIPLOMACY_WAR_INSTANT.getMessage();
							} else {
								description = MessagePath.MENU_KINGDOM_DIPLOMACY_WAR_REQUEST.getMessage();
							}
							// Detailed Info
							detailedInfo = MessagePath.MENU_KINGDOM_DIPLOMACY_WAR_INFO.getMessage();
							break;
						case ALLIANCE:
							description = MessagePath.MENU_KINGDOM_DIPLOMACY_ALLIANCE.getMessage();
							// Detailed Info
							detailedInfo = MessagePath.MENU_KINGDOM_DIPLOMACY_ALLIANCE_INFO.getMessage();
							break;
						default:
							break;
					}
				}
				loreList.addAll(Konquest.stringPaginate(description,relationColor));
				if(isValidChoice) {
					loreList.add(" "); // spacer
					loreList.addAll(Konquest.stringPaginate(detailedInfo,ChatColor.LIGHT_PURPLE));
					if(!isAdmin) {
						double costRelation = manager.getRelationCost(relation);
						String cost = String.format("%.2f",costRelation);
						loreList.add(loreColor+MessagePath.LABEL_COST.getMessage()+": "+valueColor+cost);
					}
					loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_DIPLOMACY.getMessage());
					isClickable = true;
				}
				// Create icon
				icon = new DiplomacyIcon(relation,loreList,index,isClickable);
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
		boolean isClickable = false;
		List<KonKingdom> kingdoms = new ArrayList<>();
		
		// Determine list of kingdoms given context
		if(context.equals(MenuState.A_JOIN)) {
			// List of all valid kingdoms able to join (sends request)
			kingdoms.addAll(manager.getKingdoms());
			if(isCreatedKingdom) {
				kingdoms.remove(kingdom);
			}
			isClickable = true;
		} else if(context.equals(MenuState.A_INVITE)) {
			// List of kingdoms with valid join invite for player
			kingdoms.addAll(manager.getInviteKingdoms(player));
			if(isCreatedKingdom) {
				kingdoms.remove(kingdom);
			}
			isClickable = true;
		} else if(context.equals(MenuState.A_LIST)) {
			// List of all kingdoms, friendly and enemy, with normal info
			kingdoms.addAll(manager.getKingdoms());
			isClickable = true;
		} else if(context.equals(MenuState.B_RELATIONSHIP)) {
			// List of all kingdoms, friendly and enemy, with relationship status and click hints
			kingdoms.addAll(manager.getKingdoms());
			if(isCreatedKingdom) {
				kingdoms.remove(kingdom);
			}
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
					if(!currentKingdom.equals(kingdom)) {
						// Show diplomacy state for other kingdoms
						String diplomacyState = Labeler.lookup(manager.getDiplomacy(kingdom,currentKingdom));
						loreList.add(loreColor+MessagePath.LABEL_DIPLOMACY.getMessage()+": "+valueColor+diplomacyState);
					}
					if(kingdom.hasRelationRequest(currentKingdom)) {
						// They have sent a valid diplomacy change request to us
						String ourRequestStatus = Labeler.lookup(kingdom.getRelationRequest(currentKingdom));
						loreList.add(alertColor+MessagePath.MENU_KINGDOM_THEY_REQUESTED.getMessage()+": "+valueColor+ourRequestStatus);
					}
					if(currentKingdom.hasRelationRequest(kingdom)) {
						// We have sent a valid diplomacy change request to them
						String theirRequestStatus = Labeler.lookup(currentKingdom.getRelationRequest(kingdom));
						loreList.add(alertColor+MessagePath.MENU_KINGDOM_WE_REQUESTED.getMessage()+": "+valueColor+theirRequestStatus);
					}
				}
				// Context-specific lore + click conditions
				switch(context) {
					case A_JOIN:
						// Check if the player can join the current kingdom
						if(manager.isPlayerJoinKingdomAllowed(player, currentKingdom) != 0) {
							// The kingdom is unavailable to join at this time
							loreList.add(alertColor+MessagePath.LABEL_UNAVAILABLE.getMessage());
						}
						if(currentKingdom.isOpen()) {
							loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_JOIN_NOW.getMessage());
						} else {
							loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_JOIN.getMessage());
						}
						break;
					case A_INVITE:
						// Check if the player can join the current kingdom
						if(manager.isPlayerJoinKingdomAllowed(player, currentKingdom) != 0) {
							// The kingdom is unavailable to join at this time
							loreList.add(alertColor+MessagePath.LABEL_UNAVAILABLE.getMessage());
						}
						loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_ACCEPT.getMessage());
						loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_DECLINE.getMessage());
						break;
					case A_LIST:
						loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_LIST.getMessage());
						break;
					case B_RELATIONSHIP:
						loreList.add(hintColor+MessagePath.MENU_KINGDOM_HINT_RELATION.getMessage());
						break;
					default:
						break;
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
			loreHintStr1 = MessagePath.MENU_KINGDOM_HINT_ACCEPT.getMessage();
			loreHintStr2 = MessagePath.MENU_KINGDOM_HINT_DECLINE.getMessage();
			isClickable = true;
		} else if(context.equals(MenuState.C_PROMOTE)) {
			players.addAll(kingdom.getPlayerMembersOnly());
			loreHintStr1 = MessagePath.MENU_KINGDOM_HINT_PROMOTE.getMessage();
			isClickable = true;
		} else if(context.equals(MenuState.C_DEMOTE)) {
			players.addAll(kingdom.getPlayerOfficersOnly());
			loreHintStr1 = MessagePath.MENU_KINGDOM_HINT_DEMOTE.getMessage();
			isClickable = true;
		} else if(context.equals(MenuState.C_TRANSFER)) {
			players.addAll(kingdom.getPlayerOfficersOnly());
			players.addAll(kingdom.getPlayerMembersOnly());
			loreHintStr1 = MessagePath.MENU_KINGDOM_HINT_TRANSFER.getMessage();
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
					loreList.add(propertyColor+kingdomRole);
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
		MenuState currentMenuState = (MenuState)currentState;
		int navMaxIndex = getCurrentView().getInventory().getSize()-1;
		int navMinIndex = getCurrentView().getInventory().getSize()-9;
		if(slot <= navMaxIndex && slot >= navMinIndex) {
			// Clicked in navigation bar
			int index = slot-navMinIndex;
			// (back [0]) close [4], return [5], (next [8])
			if(index == 0) {
				result = goPageBack();
			} else if(index == 5) {
				// Return to previous
				if(currentMenuState.equals(MenuState.B_DIPLOMACY)) {
					// Return to relationship from diplomacy
					currentState = MenuState.B_RELATIONSHIP;
					result = goToKingdomView(MenuState.B_RELATIONSHIP);
				} else {
					// Default return to root
					result = goToRootView();
					currentState = MenuState.ROOT;
				}
			} else if(index == 8) {
				result = goPageNext();
			}
		} else if(slot < navMinIndex) {
			// Click in non-navigation slot
			MenuIcon clickedIcon = views.get(currentState).getIcon(slot);
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
							ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
							Konquest.playFailSound(player.getBukkitPlayer());
						}
						
					}  else if(slot == ROOT_SLOT_INFO) {
						// Open the kingdom info menu
						konquest.getDisplayManager().displayKingdomInfoMenu(player, kingdom);

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
						boolean status = manager.menuJoinKingdomRequest(player, clickKingdom);
						playStatusSound(player.getBukkitPlayer(),status);
					}
					break;
				case A_EXILE:
					if(slot == SLOT_YES) {
						// Exile the player
						boolean status = manager.menuExileKingdom(player);
						playStatusSound(player.getBukkitPlayer(),status);
					}
					break;
				case A_INVITE:
					if(clickedIcon instanceof KingdomIcon) {
						KingdomIcon icon = (KingdomIcon)clickedIcon;
						KonKingdom clickKingdom = icon.getKingdom();
						boolean status = manager.menuRespondKingdomInvite(player, clickKingdom, clickType);
						playStatusSound(player.getBukkitPlayer(),status);
						if(!status) {
							// Invite declined, player assignment unchanged
							result = goToKingdomView(MenuState.A_INVITE);
						}
					}
					break;
				case A_LIST:
					// Clicking opens a kingdom info menu
					if(clickedIcon instanceof KingdomIcon) {
						KingdomIcon icon = (KingdomIcon)clickedIcon;
						KonKingdom clickKingdom = icon.getKingdom();
						konquest.getDisplayManager().displayKingdomInfoMenu(player, clickKingdom);
					}
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
					if(clickedIcon instanceof DiplomacyIcon) {
						DiplomacyIcon icon = (DiplomacyIcon)clickedIcon;
						KonquestDiplomacyType clickRelation = icon.getRelation();
						boolean status = manager.menuChangeKingdomRelation(kingdom, diplomacyKingdom, clickRelation, player, isAdmin);
						playStatusSound(player.getBukkitPlayer(),status);
						diplomacyKingdom = null;
						if(status) {
							// Return to relationship view
							currentState = MenuState.B_RELATIONSHIP;
							result = goToKingdomView(MenuState.B_RELATIONSHIP);
						}
					}
					break;
				case B_REQUESTS:
					if(clickedIcon instanceof PlayerIcon) {
						PlayerIcon icon = (PlayerIcon)clickedIcon;
						OfflinePlayer clickPlayer = icon.getOfflinePlayer();
						boolean status = manager.menuRespondKingdomRequest(player, clickPlayer, kingdom, clickType);
						playStatusSound(player.getBukkitPlayer(),status);
						result = goToPlayerView(MenuState.B_REQUESTS);
					}
					break;
				case C_PROMOTE:
					if(clickedIcon instanceof PlayerIcon) {
						PlayerIcon icon = (PlayerIcon)clickedIcon;
						OfflinePlayer clickPlayer = icon.getOfflinePlayer();
						boolean status = manager.menuPromoteOfficer(clickPlayer, kingdom);
						playStatusSound(player.getBukkitPlayer(),status);
						result = goToPlayerView(MenuState.C_PROMOTE);
					}
					break;
				case C_DEMOTE:
					if(clickedIcon instanceof PlayerIcon) {
						PlayerIcon icon = (PlayerIcon)clickedIcon;
						OfflinePlayer clickPlayer = icon.getOfflinePlayer();
						boolean status = manager.menuDemoteOfficer(clickPlayer, kingdom);
						playStatusSound(player.getBukkitPlayer(),status);
						result = goToPlayerView(MenuState.C_DEMOTE);
					}
					break;
				case C_TRANSFER:
					if(clickedIcon instanceof PlayerIcon) {
						PlayerIcon icon = (PlayerIcon)clickedIcon;
						OfflinePlayer clickPlayer = icon.getOfflinePlayer();
						boolean status = manager.menuTransferMaster(clickPlayer, kingdom, player);
						playStatusSound(player.getBukkitPlayer(),status);
					}
					break;
				case C_TEMPLATE:
					if(clickedIcon instanceof TemplateIcon) {
						TemplateIcon icon = (TemplateIcon)clickedIcon;
						KonMonumentTemplate template = icon.getTemplate();
						boolean status = manager.menuChangeKingdomTemplate(kingdom, template, player, isAdmin);
						playStatusSound(player.getBukkitPlayer(),status);
						result = goToTemplateView();
					}
					break;
				case C_DISBAND:
					if(slot == SLOT_YES) {
						boolean status = manager.menuDisbandKingdom(kingdom,player);
						playStatusSound(player.getBukkitPlayer(),status);
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
				result = color+name+" "+MessagePath.MENU_KINGDOM_TITLE_ROOT.getMessage();
				break;
			case A_JOIN:
				result = color+MessagePath.MENU_KINGDOM_TITLE_JOIN.getMessage();
				break;
			case A_EXILE:
				result = color+MessagePath.MENU_KINGDOM_TITLE_CONFIRM.getMessage();
				break;
			case A_INVITE:
				result = color+MessagePath.MENU_KINGDOM_TITLE_INVITES.getMessage();
				break;
			case A_LIST:
				result = color+MessagePath.MENU_KINGDOM_TITLE_LIST.getMessage();
				break;
			case B_RELATIONSHIP:
				result = color+MessagePath.MENU_KINGDOM_TITLE_RELATIONS.getMessage();
				break;
			case B_DIPLOMACY:
				result = color+MessagePath.MENU_KINGDOM_TITLE_DIPLOMACY.getMessage();
				break;
			case B_REQUESTS:
				result = color+MessagePath.MENU_KINGDOM_TITLE_REQUESTS.getMessage();
				break;
			case C_PROMOTE:
				result = color+MessagePath.MENU_KINGDOM_TITLE_PROMOTION.getMessage();
				break;
			case C_DEMOTE:
				result = color+MessagePath.MENU_KINGDOM_TITLE_DEMOTION.getMessage();
				break;
			case C_TRANSFER:
				result = color+MessagePath.MENU_KINGDOM_TITLE_TRANSFER.getMessage();
				break;
			case C_TEMPLATE:
				result = color+MessagePath.MENU_KINGDOM_TITLE_TEMPLATE.getMessage();
				break;
			case C_DISBAND:
				result = color+MessagePath.MENU_KINGDOM_TITLE_DISBAND.getMessage();
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
			ChatUtil.printDebug("Kingdom menu nav buttons failed to refresh in context "+context.toString());
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
