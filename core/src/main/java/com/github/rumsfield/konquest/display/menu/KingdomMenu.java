package com.github.rumsfield.konquest.display.menu;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.api.model.KonquestDiplomacyType;
import com.github.rumsfield.konquest.command.CommandType;
import com.github.rumsfield.konquest.display.DisplayMenu;
import com.github.rumsfield.konquest.display.StateMenu;
import com.github.rumsfield.konquest.display.icon.*;
import com.github.rumsfield.konquest.manager.DisplayManager;
import com.github.rumsfield.konquest.manager.KingdomManager;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.*;
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
public class KingdomMenu extends StateMenu {

	enum MenuState implements State {
		ROOT,
		A_JOIN,
		A_CREATE,
		A_EXILE,
		A_INFO,
		A_INVITE,
		A_LIST,
		B_RELATIONSHIP,
		B_DIPLOMACY,
		B_REQUESTS,
		C_PROMOTE,
		C_DEMOTE,
		C_TRANSFER,
		C_OPEN,
		C_DESTROY,
		C_CAPITAL,
		C_TEMPLATE,
		C_DISBAND,
		CONFIRM_YES,
		CONFIRM_NO
	}

	/*
	 * Menu Layout
	 * Labels with * are a new menu state.
	 * Labels with + are icon buttons.
	 *
	 * Access  | Labels...
	 * ----------------------------------------------------------------------------------
	 * Regular | *Join   		*Leave		+Info		*Invites	*List
	 * Officer | *Relationship	*Requests
	 * Master  | *Promote 		*Demote 	*Transfer 	+Open		*Template		*Disband	*Destroy	*Capital
	 *
	 * Relationship selects other kingdom and opens diplomacy view, which selects new status (enemy, ally, etc)
	 */

	enum AccessType implements Access {
		REGULAR,
		OFFICER,
		MASTER
	}

	private final KingdomManager manager;
	private final KonPlayer player;
	private final KonKingdom kingdom;
	private KonKingdom diplomacyKingdom;
	private boolean isCreatedKingdom;
	private final boolean isAdmin;
	private final boolean isViewer;
	
	public KingdomMenu(Konquest konquest, KonPlayer player, KonKingdom kingdom, boolean isAdmin) {
		super(konquest, MenuState.ROOT, AccessType.REGULAR);
		this.manager = konquest.getKingdomManager();
		this.player = player;
		this.kingdom = kingdom;
		this.diplomacyKingdom = null;
		this.isCreatedKingdom = false; // Is this kingdom created by players, i.e. not barbarians or neutrals
		this.isAdmin = isAdmin; // Is player viewing the menu as an admin?
		this.isViewer = kingdom.equals(player.getKingdom()); // Is the player viewing their own kingdom?

		/* Initialize menu access */
		if(kingdom.isCreated()) {
			isCreatedKingdom = true;
			UUID id = player.getBukkitPlayer().getUniqueId();
			if(isAdmin) {
				setAccess(AccessType.MASTER);
			} else {
				if(kingdom.isMaster(id)) {
					setAccess(AccessType.MASTER);
				} else if(kingdom.isOfficer(id)) {
					setAccess(AccessType.OFFICER);
				} else if(kingdom.isMember(id)) {
					setAccess(AccessType.REGULAR);
				}
			}
		}

		/* Initialize menu view */
		setCurrentView(MenuState.ROOT);

	}
	
	private DisplayMenu createRootView() {
		DisplayMenu result;
		MenuIcon icon;

		/* Icon slot indexes */
		// Row 0: 0 1 2 3 4 5 6 7 8
		int ROOT_SLOT_JOIN 			= 0;
		int ROOT_SLOT_CREATE		= 1;
		int ROOT_SLOT_EXILE 		= 2;
		int ROOT_SLOT_INFO 			= 4;
		int ROOT_SLOT_INVITE 		= 6;
		int ROOT_SLOT_LIST 			= 8;
		// Row 1: 9 10 11 12 13 14 15 16 17
		int ROOT_SLOT_RELATIONSHIPS = 11;
		int ROOT_SLOT_REQUESTS 		= 15;
		// Row 2: 18 19 20 21 22 23 24 25 26
		int ROOT_SLOT_PROMOTE 		= 18;
		int ROOT_SLOT_DEMOTE 		= 19;
		int ROOT_SLOT_TRANSFER		= 20;
		int ROOT_SLOT_OPEN 			= 21;
		int ROOT_SLOT_TEMPLATE 		= 23;
		int ROOT_SLOT_DISBAND		= 24;
		int ROOT_SLOT_DESTROY		= 25;
		int ROOT_SLOT_CAPITAL		= 26;
		
		int rows = 1; // default rows for regular
		if(getAccess().equals(AccessType.OFFICER)) {
			rows = 2;
		} else if(getAccess().equals(AccessType.MASTER)) {
			rows = 3;
		}
		
		result = new DisplayMenu(rows, getTitle(MenuState.ROOT));
		
		/* Join Icon */
		boolean isJoinClickable = !isAdmin;
		icon = new InfoIcon(MessagePath.MENU_KINGDOM_JOIN.getMessage(), Material.SADDLE, ROOT_SLOT_JOIN, isJoinClickable);
		icon.addDescription(MessagePath.MENU_KINGDOM_DESCRIPTION_JOIN.getMessage());
		if(isJoinClickable) {
			icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
		} else {
			icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
		}
		icon.setState(MenuState.A_JOIN);
		result.addIcon(icon);

		/* Create Icon */
		if (!getKonquest().getKingdomManager().isKingdomCreateAdminOnly()) {
			double cost_create = getKonquest().getCore().getDouble(CorePath.FAVOR_KINGDOMS_COST_CREATE.getPath(),0.0);
			icon = new InfoIcon(MessagePath.MENU_KINGDOM_CREATE.getMessage(), Material.PUFFERFISH_SPAWN_EGG, ROOT_SLOT_CREATE, true);
			icon.addDescription(MessagePath.MENU_KINGDOM_DESCRIPTION_CREATE.getMessage());
			icon.addNameValue(MessagePath.LABEL_COST.getMessage(), (int)cost_create);
			icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
			icon.setState(MenuState.A_CREATE);
			result.addIcon(icon);
		}

		/* Exile Icon */
		boolean isExileClickable = !isAdmin && isCreatedKingdom;
		icon = new InfoIcon(MessagePath.MENU_KINGDOM_EXILE.getMessage(), Material.ARROW, ROOT_SLOT_EXILE, isExileClickable);
		icon.addDescription(MessagePath.MENU_KINGDOM_DESCRIPTION_EXILE.getMessage());
		if(isExileClickable) {
			icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
		} else {
			icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
		}
		icon.setState(MenuState.A_EXILE);
		result.addIcon(icon);

		/* Invites Icon */
		boolean isInvitesClickable = !isAdmin;
		int numInvites = manager.getInviteKingdoms(player).size();
		Material inviteMat = numInvites > 0 ? Material.WRITABLE_BOOK : Material.BOOK;
		icon = new InfoIcon(MessagePath.MENU_KINGDOM_INVITES.getMessage(), inviteMat, ROOT_SLOT_INVITE, isInvitesClickable);
		icon.addDescription(MessagePath.MENU_KINGDOM_DESCRIPTION_INVITES.getMessage());
		if(isInvitesClickable) {
			icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
		} else {
			icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
		}
		if(numInvites > 0) {
			icon.addNameValue(MessagePath.MENU_KINGDOM_INVITES.getMessage(), numInvites);
		}
		icon.setState(MenuState.A_INVITE);
		result.addIcon(icon);
		
		/* List Icon */
		icon = new InfoIcon(MessagePath.MENU_KINGDOM_LIST.getMessage(), Material.PAPER, ROOT_SLOT_LIST, true);
		icon.addDescription(MessagePath.MENU_KINGDOM_DESCRIPTION_LIST.getMessage());
		icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
		icon.setState(MenuState.A_LIST);
		result.addIcon(icon);

		// These icons only appear for created kingdoms
		if(isCreatedKingdom) {

			/* Kingdom Info Icon */
			icon = new KingdomIcon(kingdom,DisplayManager.nameFormat,ROOT_SLOT_INFO,true,isViewer);
			icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
			icon.setState(MenuState.A_INFO);
			result.addIcon(icon);

			// These icons only appear for officers and masters
			if(getAccess().equals(AccessType.OFFICER) || getAccess().equals(AccessType.MASTER)) {

				/* Relations Icon */
				boolean isRelationsClickable = !kingdom.isPeaceful();
				icon = new InfoIcon(MessagePath.MENU_KINGDOM_RELATION.getMessage(), Material.GOLDEN_SWORD, ROOT_SLOT_RELATIONSHIPS, isRelationsClickable);
				icon.addDescription(MessagePath.MENU_KINGDOM_DESCRIPTION_RELATION.getMessage());
				icon.addProperty(MessagePath.LABEL_OFFICER.getMessage());
				if(isRelationsClickable) {
					icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
				} else {
					icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
				}
				icon.setState(MenuState.B_RELATIONSHIP);
				result.addIcon(icon);
				
				/* Requests Icon */
				int numRequests = kingdom.getJoinRequests().size();
				Material requestMat = Material.GLASS_BOTTLE;
				if(numRequests > 0) {
					requestMat = Material.HONEY_BOTTLE;
				}
				icon = new InfoIcon(MessagePath.MENU_KINGDOM_REQUESTS.getMessage(), requestMat, ROOT_SLOT_REQUESTS, true);
				icon.addDescription(MessagePath.MENU_KINGDOM_DESCRIPTION_REQUESTS.getMessage());
				icon.addProperty(MessagePath.LABEL_OFFICER.getMessage());
				icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
				if(numRequests > 0) {
					icon.addNameValue(MessagePath.MENU_KINGDOM_REQUESTS.getMessage(), numRequests);
				}
				icon.setState(MenuState.B_REQUESTS);
				result.addIcon(icon);
			}

			// These icons only appear for masters
			if(getAccess().equals(AccessType.MASTER)) {

				/* Promote Icon */
				boolean isPromoteClickable = kingdom.isPromoteable() || isAdmin;
				icon = new InfoIcon(MessagePath.MENU_KINGDOM_PROMOTE.getMessage(), Material.IRON_HORSE_ARMOR, ROOT_SLOT_PROMOTE, isPromoteClickable);
				icon.addDescription(MessagePath.MENU_KINGDOM_DESCRIPTION_PROMOTE.getMessage());
				icon.addProperty(MessagePath.LABEL_MASTER.getMessage());
				if(isPromoteClickable) {
					icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
				} else {
					icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
				}
				icon.setState(MenuState.C_PROMOTE);
				result.addIcon(icon);

				/* Demote Icon */
				boolean isDemoteClickable = kingdom.isDemoteable() || isAdmin;
				icon = new InfoIcon(MessagePath.MENU_KINGDOM_DEMOTE.getMessage(), Material.LEATHER_CHESTPLATE, ROOT_SLOT_DEMOTE, isDemoteClickable);
				icon.addDescription(MessagePath.MENU_KINGDOM_DESCRIPTION_DEMOTE.getMessage());
				icon.addProperty(MessagePath.LABEL_MASTER.getMessage());
				if(isDemoteClickable) {
					icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
				} else {
					icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
				}
				icon.setState(MenuState.C_DEMOTE);
				result.addIcon(icon);

				/* Transfer Icon */
				if(!kingdom.isAdminOperated()) {
					boolean isTransferClickable = kingdom.isTransferable() || isAdmin;
					icon = new InfoIcon(MessagePath.MENU_KINGDOM_TRANSFER.getMessage(), Material.IRON_HELMET, ROOT_SLOT_TRANSFER, isTransferClickable);
					icon.addDescription(MessagePath.MENU_KINGDOM_DESCRIPTION_TRANSFER.getMessage());
					icon.addProperty(MessagePath.LABEL_MASTER.getMessage());
					if(isTransferClickable) {
						icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
					} else {
						icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
					}
					icon.setState(MenuState.C_TRANSFER);
					result.addIcon(icon);
				}

				/* Destroy Icon */
				if(getKonquest().getKingdomManager().getIsTownDestroyMasterEnable()) {
					icon = new InfoIcon(MessagePath.MENU_TOWN_DESTROY.getMessage(), Material.FLINT_AND_STEEL, ROOT_SLOT_DESTROY, true);
					icon.addDescription(MessagePath.MENU_TOWN_DESCRIPTION_DESTROY.getMessage());
					icon.addProperty(MessagePath.LABEL_MASTER.getMessage());
					icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
					icon.setState(MenuState.C_DESTROY);
					result.addIcon(icon);
				}

				/* Capital Icon */
				if(getKonquest().getKingdomManager().getIsCapitalSwapEnable()) {
					icon = new InfoIcon(MessagePath.MENU_KINGDOM_CAPITAL.getMessage(), Material.PISTON, ROOT_SLOT_CAPITAL, true);
					icon.addDescription(MessagePath.MENU_KINGDOM_DESCRIPTION_CAPITAL.getMessage());
					icon.addProperty(MessagePath.LABEL_MASTER.getMessage());
					icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
					icon.setState(MenuState.C_CAPITAL);
					result.addIcon(icon);
				}

				/* Open/Close Button */
				String currentValue = DisplayManager.boolean2Lang(kingdom.isOpen())+" "+DisplayManager.boolean2Symbol(kingdom.isOpen());
				icon = new InfoIcon(MessagePath.LABEL_OPEN.getMessage(), Material.IRON_DOOR, ROOT_SLOT_OPEN, true);
				icon.addProperty(MessagePath.LABEL_MASTER.getMessage());
				icon.addNameValue(MessagePath.LABEL_CURRENT.getMessage(), currentValue);
				icon.addHint(MessagePath.MENU_HINT_CHANGE.getMessage());
				icon.setState(MenuState.C_OPEN);
				result.addIcon(icon);

				/* Template Icon */
				icon = new InfoIcon(MessagePath.MENU_KINGDOM_TEMPLATE.getMessage(), Material.CRAFTING_TABLE, ROOT_SLOT_TEMPLATE, true);
				icon.addProperty(MessagePath.LABEL_MASTER.getMessage());
				icon.addDescription(MessagePath.MENU_KINGDOM_DESCRIPTION_TEMPLATE.getMessage());
				icon.addNameValue(MessagePath.LABEL_MONUMENT_TEMPLATE.getMessage(), kingdom.getMonumentTemplateName());
				icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
				icon.setState(MenuState.C_TEMPLATE);
				result.addIcon(icon);

				/* Disband Icon */
				if(!kingdom.isAdminOperated()) {
					icon = new InfoIcon(MessagePath.MENU_KINGDOM_DISBAND.getMessage(), Material.BONE, ROOT_SLOT_DISBAND, true);
					icon.addProperty(MessagePath.LABEL_MASTER.getMessage());
					icon.addDescription(MessagePath.MENU_KINGDOM_DESCRIPTION_DISBAND.getMessage());
					icon.addHint(MessagePath.MENU_HINT_OPEN.getMessage());
					icon.setState(MenuState.C_DISBAND);
					result.addIcon(icon);
				}
			}
		}

		/* Navigation */
		addNavEmpty(result);
		addNavHome(result);
		addNavClose(result);
		
		return result;
	}
	
	private DisplayMenu createExileView() {
		DisplayMenu result;
		MenuIcon icon;
		result = new DisplayMenu(1, getTitle(MenuState.A_EXILE));

		/* Icon slot indexes */
		// Row 0: 0 1 2 3 4 5 6 7 8
		int SLOT_YES 	= 3;
		int SLOT_NO 	= 5;

		/* Yes Button */
		icon = new ConfirmationIcon(true, SLOT_YES);
		icon.addHint(MessagePath.MENU_KINGDOM_HINT_EXILE.getMessage());
		icon.setState(MenuState.CONFIRM_YES);
		result.addIcon(icon);

		/* No Button */
		icon = new ConfirmationIcon(false, SLOT_NO);
		icon.addHint(MessagePath.MENU_HINT_EXIT.getMessage());
		icon.setState(MenuState.CONFIRM_NO);
		result.addIcon(icon);

		/* Navigation */
		addNavEmpty(result);
		addNavClose(result);
		addNavReturn(result);

		return result;
	}
	
	private DisplayMenu createDisbandView() {
		DisplayMenu result;
		MenuIcon icon;
		result = new DisplayMenu(1, getTitle(MenuState.C_DISBAND));

		/* Icon slot indexes */
		// Row 0: 0 1 2 3 4 5 6 7 8
		int SLOT_YES 	= 3;
		int SLOT_NO 	= 5;

		/* Yes Button */
		icon = new ConfirmationIcon(true, SLOT_YES);
		icon.addHint(MessagePath.MENU_KINGDOM_HINT_DISBAND.getMessage());
		icon.setState(MenuState.CONFIRM_YES);
		result.addIcon(icon);

		/* No Button */
		icon = new ConfirmationIcon(false, SLOT_NO);
		icon.addHint(MessagePath.MENU_HINT_EXIT.getMessage());
		icon.setState(MenuState.CONFIRM_NO);
		result.addIcon(icon);

		/* Navigation */
		addNavEmpty(result);
		addNavClose(result);
		addNavReturn(result);

		return result;
	}

	private List<DisplayMenu> createTemplateView() {
		MenuIcon icon;
		ArrayList<MenuIcon> icons = new ArrayList<>();

		/* Template Icons */
		for (KonMonumentTemplate template : getKonquest().getSanctuaryManager().getAllTemplates()) {
			if (kingdom.getMonumentTemplate() != null && kingdom.getMonumentTemplate().equals(template)) {
				// Skip this kingdom's template
				continue;
			}
			boolean isClickable = true;
			String cost = "0";
			if(template.isValid()) {
				if(!isAdmin && kingdom.hasMonumentTemplate()) {
					double totalCost = manager.getCostTemplate() + template.getCost();
					cost = String.format("%.2f",totalCost);
				}
			} else {
				// Invalid template, check for blanking
				isClickable = false;
			}
			icon = new TemplateIcon(template,0,isClickable);
			icon.addNameValue(MessagePath.LABEL_COST.getMessage(), cost);
			if (isClickable) {
				icon.addHint(MessagePath.MENU_KINGDOM_HINT_TEMPLATE.getMessage());
			}
			icons.add(icon);
		}

		/* Make Pages (includes navigation) */
		return new ArrayList<>(makePages(icons, getTitle(MenuState.C_TEMPLATE)));
	}

	private DisplayMenu createDiplomacyView() {
		// diplomacyKingdom is the global variable to keep track of current kingdom changing status
		if (diplomacyKingdom == null || diplomacyKingdom.equals(kingdom)) return null;

		int numIcons = KonquestDiplomacyType.values().length + 2; // Add 2 for kingdom info and spacer
		int numRows = (int)Math.ceil((double)numIcons / MAX_ROW_SIZE);
		int index = 0;
		MenuIcon icon;
		DisplayMenu result = new DisplayMenu(numRows, getTitle(MenuState.B_DIPLOMACY));

		KonquestDiplomacyType currentDiplomacy = manager.getDiplomacy(kingdom,diplomacyKingdom);

		/* Kingdom Info Icon */
		String contextColor = getKonquest().getDisplaySecondaryColor(kingdom, diplomacyKingdom);
		String diplomacyState = Labeler.lookup(currentDiplomacy);
		icon = new KingdomIcon(diplomacyKingdom,contextColor,index,false,isViewer);
		icon.addNameValue(MessagePath.LABEL_DIPLOMACY.getMessage(), diplomacyState);
		if(kingdom.hasRelationRequest(diplomacyKingdom) || diplomacyKingdom.hasRelationRequest(kingdom)) {
			icon.addAlert(MessagePath.MENU_KINGDOM_REQUESTS.getMessage());
		}
		if(kingdom.hasRelationRequest(diplomacyKingdom)) {
			// They have sent a valid diplomacy change request to us
			String ourRequestStatus = Labeler.lookup(kingdom.getRelationRequest(diplomacyKingdom));
			icon.addNameValue(MessagePath.MENU_KINGDOM_THEY_REQUESTED.getMessage(), ourRequestStatus);
		}
		if(diplomacyKingdom.hasRelationRequest(kingdom)) {
			// We have sent a valid diplomacy change request to them
			String theirRequestStatus = Labeler.lookup(diplomacyKingdom.getRelationRequest(kingdom));
			icon.addNameValue(MessagePath.MENU_KINGDOM_WE_REQUESTED.getMessage(), theirRequestStatus);
		}
		result.addIcon(icon);

		/* Relationship Icons */
		// Only create relation options for created kingdoms
		index = 2;
		if(isCreatedKingdom) {
			// Does any change to war by one side instantly force the other into war?
			boolean isInstantWar = getKonquest().getCore().getBoolean(CorePath.KINGDOMS_INSTANT_WAR.getPath(), false);
			// Does a change from war to peace by one side instantly force the other into peace?
			boolean isInstantPeace = getKonquest().getCore().getBoolean(CorePath.KINGDOMS_INSTANT_PEACE.getPath(), false);

			for(KonquestDiplomacyType relation : KonquestDiplomacyType.values()) {
				// Determine context lore for this relation and the stance of kingdom with diplomacyKingdom
				// Is this relation a valid option in the current relationship?
				boolean isValidChoice = manager.isValidRelationChoice(kingdom, diplomacyKingdom, relation);
				ChatColor relationColor = ChatColor.GRAY;
				String description = MessagePath.LABEL_UNAVAILABLE.getMessage();
				String detailedInfo = "";
				boolean isClickable = false;
				if(isValidChoice) {
					isClickable = true;
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
				// Create icon
				icon = new DiplomacyIcon(relation,index,isClickable);
				icon.addDescription(description,relationColor);
				if(isValidChoice) {
					icon.addDescription(detailedInfo);
					if(!isAdmin) {
						double costRelation = manager.getRelationCost(relation);
						String cost = String.format("%.2f",costRelation);
						icon.addNameValue(MessagePath.LABEL_COST.getMessage(), cost);
					}
					icon.addHint(MessagePath.MENU_KINGDOM_HINT_DIPLOMACY.getMessage());
				}
				result.addIcon(icon);
				index++;
			}
		}

		/* Navigation */
		addNavEmpty(result);
		addNavClose(result);
		addNavReturn(result);

		return result;
	}

	private List<DisplayMenu> createKingdomView(MenuState context) {
		MenuIcon icon;
		ArrayList<MenuIcon> icons = new ArrayList<>();
		boolean isClickable = false;
		List<KonKingdom> kingdoms = new ArrayList<>();

		// Gather kingdom list and sort
		switch (context) {
			case A_JOIN:
				// List of all valid kingdoms able to join (sends request)
				kingdoms.addAll(manager.getKingdoms());
				if(isCreatedKingdom) {
					kingdoms.remove(kingdom);
				}
				isClickable = true;
				break;
			case A_INVITE:
				// List of kingdoms with valid join invite for player
				kingdoms.addAll(manager.getInviteKingdoms(player));
				if(isCreatedKingdom) {
					kingdoms.remove(kingdom);
				}
				isClickable = true;
				break;
			case A_LIST:
				// List of all kingdoms, friendly and enemy, with normal info
				kingdoms.addAll(manager.getKingdoms());
				isClickable = true;
				break;
			case B_RELATIONSHIP:
				// List of all kingdoms, friendly and enemy, with relationship status and click hints
				for(KonKingdom otherKingdom : manager.getKingdoms()) {
					if(!otherKingdom.equals(kingdom) && !otherKingdom.isPeaceful()) {
						kingdoms.add(otherKingdom);
					}
				}
				isClickable = true;
				break;
			default:
				break;
		}
		// Sort list by land then population
		kingdoms.sort(kingdomComparator);

		/* Kingdom Icons */
		for (KonKingdom currentKingdom : kingdoms) {
			String contextColor = getKonquest().getDisplaySecondaryColor(kingdom, currentKingdom);
			boolean isViewer = currentKingdom.equals(player.getKingdom());
			icon = new KingdomIcon(currentKingdom,contextColor,0,isClickable,isViewer);
			if(isCreatedKingdom) {
				if(!currentKingdom.equals(kingdom)) {
					// Show diplomacy state for other kingdoms
					String diplomacyState = Labeler.lookup(manager.getDiplomacy(kingdom,currentKingdom));
					icon.addNameValue(MessagePath.LABEL_DIPLOMACY.getMessage(), diplomacyState);
				}
				if(kingdom.hasRelationRequest(currentKingdom) || currentKingdom.hasRelationRequest(kingdom)) {
					icon.addAlert(MessagePath.MENU_KINGDOM_REQUESTS.getMessage());
				}
				if(kingdom.hasRelationRequest(currentKingdom)) {
					// They have sent a valid diplomacy change request to us
					String ourRequestStatus = Labeler.lookup(kingdom.getRelationRequest(currentKingdom));
					icon.addNameValue(MessagePath.MENU_KINGDOM_THEY_REQUESTED.getMessage(), ourRequestStatus);
				}
				if(currentKingdom.hasRelationRequest(kingdom)) {
					// We have sent a valid diplomacy change request to them
					String theirRequestStatus = Labeler.lookup(currentKingdom.getRelationRequest(kingdom));
					icon.addNameValue(MessagePath.MENU_KINGDOM_WE_REQUESTED.getMessage(), theirRequestStatus);
				}
			}
			// Context-specific lore + click conditions
			switch(context) {
				case A_JOIN:
					// Check if the player can join the current kingdom
					if(manager.isPlayerJoinKingdomAllowed(player, currentKingdom) != 0 ||
							!currentKingdom.isJoinable() ||
							(kingdom.isCreated() && !kingdom.isLeaveable())) {
						// The kingdom is unavailable to join at this time
						icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
					}
					if(currentKingdom.isOpen()) {
						icon.addHint(MessagePath.MENU_KINGDOM_HINT_JOIN_NOW.getMessage());
					} else {
						icon.addHint(MessagePath.MENU_KINGDOM_HINT_JOIN.getMessage());
					}
					break;
				case A_INVITE:
					// Check if the player can join the current kingdom
					if(manager.isPlayerJoinKingdomAllowed(player, currentKingdom) != 0 ||
							!currentKingdom.isJoinable() ||
							(kingdom.isCreated() && !kingdom.isLeaveable())) {
						// The kingdom is unavailable to join at this time
						icon.addAlert(MessagePath.LABEL_UNAVAILABLE.getMessage());
					}
					icon.addHint(MessagePath.MENU_KINGDOM_HINT_ACCEPT.getMessage());
					icon.addHint(MessagePath.MENU_KINGDOM_HINT_DECLINE.getMessage());
					break;
				case A_LIST:
					icon.addHint(MessagePath.MENU_HINT_VIEW.getMessage());
					break;
				case B_RELATIONSHIP:
					icon.addHint(MessagePath.MENU_KINGDOM_HINT_RELATION.getMessage());
					break;
				default:
					break;
			}
			icons.add(icon);
		}

		/* Make Pages (includes navigation) */
		return new ArrayList<>(makePages(icons, getTitle(context)));
	}

	private List<DisplayMenu> createTownView(MenuState context) {
		MenuIcon icon;
		ArrayList<MenuIcon> icons = new ArrayList<>();
		// List of all towns in kingdom
		List<KonTown> towns = new ArrayList<>(kingdom.getTowns());
		// Sort list
		towns.sort(townComparator);
		// Name color
		String contextColor = isAdmin ? DisplayManager.adminFormat : Konquest.friendColor2;

		/* Town Icons */
		for (KonTown currentTown : towns) {
			icon = new TownIcon(currentTown,player.getBukkitPlayer(),contextColor,0,true);
			// Context-specific lore + click conditions
			switch(context) {
				case C_DESTROY:
					icon.addHint(MessagePath.MENU_TOWN_HINT_DESTROY.getMessage());
					break;
				case C_CAPITAL:
					icon.addHint(MessagePath.MENU_KINGDOM_HINT_CAPITAL.getMessage());
					break;
				default:
					break;
			}
			icons.add(icon);
		}

		/* Make Pages (includes navigation) */
		return new ArrayList<>(makePages(icons, getTitle(context)));
	}

	private List<DisplayMenu> createPlayerView(MenuState context) {
		MenuIcon icon;
		ArrayList<MenuIcon> icons = new ArrayList<>();
		String loreHintStr1 = "";
		String loreHintStr2 = "";
		boolean isClickable = true;
		List<OfflinePlayer> players = new ArrayList<>();

		// Gather players
		switch (context) {
			case B_REQUESTS:
				players.addAll(kingdom.getJoinRequests());
				loreHintStr1 = MessagePath.MENU_KINGDOM_HINT_ACCEPT.getMessage();
				loreHintStr2 = MessagePath.MENU_KINGDOM_HINT_DECLINE.getMessage();
				break;
			case C_PROMOTE:
				players.addAll(kingdom.getPlayerMembersOnly());
				loreHintStr1 = MessagePath.MENU_KINGDOM_HINT_PROMOTE.getMessage();
				break;
			case C_DEMOTE:
				players.addAll(kingdom.getPlayerOfficersOnly());
				loreHintStr1 = MessagePath.MENU_KINGDOM_HINT_DEMOTE.getMessage();
				break;
			case C_TRANSFER:
				players.addAll(kingdom.getPlayerOfficersOnly());
				players.addAll(kingdom.getPlayerMembersOnly());
				loreHintStr1 = MessagePath.MENU_KINGDOM_HINT_TRANSFER.getMessage();
				break;
			default:
				break;
		}

		/* Player Icons */
		for (OfflinePlayer currentPlayer : players) {
			String contextColor = DisplayManager.nameFormat;
			KonOfflinePlayer offlinePlayer = getKonquest().getPlayerManager().getOfflinePlayer(currentPlayer);
			if (offlinePlayer != null) {
				contextColor = getKonquest().getDisplaySecondaryColor(kingdom, offlinePlayer.getKingdom());
			}
			icon = new PlayerIcon(currentPlayer,contextColor,0,isClickable);
			String kingdomRole = kingdom.getPlayerRoleName(currentPlayer);
			if(!kingdomRole.isEmpty()) {
				icon.addProperty(kingdomRole);
			}
			if(!loreHintStr1.isEmpty()) {
				icon.addHint(loreHintStr1);
			}
			if(!loreHintStr2.isEmpty()) {
				icon.addHint(loreHintStr2);
			}
			icons.add(icon);
		}

		/* Make Pages (includes navigation) */
		return new ArrayList<>(makePages(icons, getTitle(context)));
	}

	/**
	 * Create a list of views for a given menu state.
	 * This creates new views, specific to each menu.
	 * @param context The menu state for the corresponding view
	 * @return The list of menu views to be displayed to the player
	 */
	@Override
	public ArrayList<DisplayMenu> createView(State context) {
		ArrayList<DisplayMenu> result = new ArrayList<>();
		MenuState currentState = (MenuState)context;
		switch (currentState) {
			case ROOT:
				result.add(createRootView());
				break;
			case A_EXILE:
				result.add(createExileView());
				break;
			case B_DIPLOMACY:
				result.add(createDiplomacyView());
				break;
			case C_DISBAND:
				result.add(createDisbandView());
				break;
			case C_TEMPLATE:
				result.addAll(createTemplateView());
				break;
			case A_JOIN:
			case A_INVITE:
			case A_LIST:
			case B_RELATIONSHIP:
				result.addAll(createKingdomView(currentState));
				break;
			case B_REQUESTS:
			case C_PROMOTE:
			case C_DEMOTE:
			case C_TRANSFER:
				result.addAll(createPlayerView(currentState));
				break;
			case C_DESTROY:
			case C_CAPITAL:
				result.addAll(createTownView(currentState));
				break;
			default:
				break;
		}
		return result;
	}

	/**
	 * Change the menu's state based on the clicked inventory slot and type of click (right or left mouse).
	 * Assume a clickable icon was clicked and visible to the player.
	 * Returning a null value will close the menu.
	 * @param slot The inventory slot of the current view that was clicked
	 * @param clickType The type of click, true for left-click, false for right click
	 * @return The new view state of the menu, or null to close the menu
	 */
	@Override
	public DisplayMenu updateState(int slot, boolean clickType) {
		DisplayMenu result = null;
		if (isCurrentNavSlot(slot)) {
			// Clicked in navigation bar
			if (isNavClose(slot)) {
				// Close the menu by returning a null view
				return null;
			} else if (isNavHome(slot)) {
				// Go to main menu
				getKonquest().getDisplayManager().displayMainMenu(player);
			} else if (isNavReturn(slot)) {
				// Return to previous
				if (isState(MenuState.B_DIPLOMACY)) {
					// Return to refreshed relationship from diplomacy
					result = setCurrentView(MenuState.B_RELATIONSHIP);
				} else {
					// Return to root
					result = setCurrentView(MenuState.ROOT);
				}
			} else if (isNavBack(slot)) {
				// Page back
				result = goPageBack();
			} else if (isNavNext(slot)) {
				// Page next
				result = goPageNext();
			}
		} else if (isCurrentMenuSlot(slot)) {
			// Clicked in menu
			DisplayMenu view = getCurrentView();
			if (view == null) return null;
			MenuIcon clickedIcon = view.getIcon(slot);
			MenuState currentState = (MenuState)getCurrentState();
			if (currentState == null) return null;
			MenuState nextState = (MenuState)clickedIcon.getState(); // could be null in some states
			switch (currentState) {
				case ROOT:
					// Use stored icon state
					if (nextState == null) return null;
					switch (nextState) {
						case A_JOIN:
						case A_EXILE:
						case A_INVITE:
						case A_LIST:
						case B_RELATIONSHIP:
						case B_REQUESTS:
						case C_PROMOTE:
						case C_DEMOTE:
						case C_TRANSFER:
						case C_DESTROY:
						case C_CAPITAL:
						case C_DISBAND:
						case C_TEMPLATE:
							// Go to next state as defined by icon
							result = setCurrentView(nextState);
							break;
						case A_CREATE:
							// Display command usage
							for (String usageLine : CommandType.KINGDOM.command().getArgumentUsage("create")) {
								ChatUtil.sendNotice(player.getBukkitPlayer(), usageLine);
							}
							result = view;
							break;
						case A_INFO:
							// Open the kingdom info menu, close this menu
							getKonquest().getDisplayManager().displayInfoKingdomMenu(player, kingdom);
							break;
						case C_OPEN:
							// Clicked to toggle open/closed state, refresh this view
							manager.menuToggleKingdomOpen(kingdom, player);
							playStatusSound(player.getBukkitPlayer(), true);
							result = refreshCurrentView();
							break;
					}
					break;
				case A_JOIN:
					// Join view, close this menu
					if (clickedIcon instanceof KingdomIcon) {
						KingdomIcon icon = (KingdomIcon)clickedIcon;
						KonKingdom clickKingdom = icon.getKingdom();
						boolean status = manager.menuJoinKingdomRequest(player, clickKingdom);
						playStatusSound(player.getBukkitPlayer(),status);
					}
					break;
				case A_EXILE:
					// Exile view, close this menu
					if (nextState == null) return null;
					if (nextState.equals(MenuState.CONFIRM_YES)) {
						// Exile the player
						boolean status = manager.menuExileKingdom(player);
						playStatusSound(player.getBukkitPlayer(),status);
					}
					break;
				case A_INVITE:
					if (clickedIcon instanceof KingdomIcon) {
						KingdomIcon icon = (KingdomIcon)clickedIcon;
						KonKingdom clickKingdom = icon.getKingdom();
						boolean status = manager.menuRespondKingdomInvite(player, clickKingdom, clickType);
						playStatusSound(player.getBukkitPlayer(),status);
						if (!status) {
							// Invite declined, player assignment unchanged
							result = refreshCurrentView();
						}
					}
					break;
				case A_LIST:
					// Clicking opens a kingdom info menu
					if (clickedIcon instanceof KingdomIcon) {
						KingdomIcon icon = (KingdomIcon)clickedIcon;
						KonKingdom clickKingdom = icon.getKingdom();
						getKonquest().getDisplayManager().displayInfoKingdomMenu(player, clickKingdom);
					}
					break;
				case B_RELATIONSHIP:
					// Clicking icons goes to diplomacy view
					if (clickedIcon instanceof KingdomIcon) {
						KingdomIcon icon = (KingdomIcon)clickedIcon;
						diplomacyKingdom = icon.getKingdom();
						// Go to diplomacy view
						result = refreshNewView(MenuState.B_DIPLOMACY);
					}
					break;
				case B_DIPLOMACY:
					// Clicking changes the relationship of kingdoms
					if (clickedIcon instanceof DiplomacyIcon) {
						DiplomacyIcon icon = (DiplomacyIcon)clickedIcon;
						KonquestDiplomacyType clickRelation = icon.getRelation();
						boolean status = manager.menuChangeKingdomRelation(kingdom, diplomacyKingdom, clickRelation, player, isAdmin);
						playStatusSound(player.getBukkitPlayer(),status);
						diplomacyKingdom = null;
						if (status) {
							// Return to relationship view
							result = refreshNewView(MenuState.B_RELATIONSHIP);
						}
					}
					break;
				case B_REQUESTS:
					if (clickedIcon instanceof PlayerIcon) {
						PlayerIcon icon = (PlayerIcon)clickedIcon;
						KonOfflinePlayer clickPlayer = getKonquest().getPlayerManager().getOfflinePlayer(icon.getOfflinePlayer());
						boolean status = manager.menuRespondKingdomRequest(player, clickPlayer, kingdom, clickType);
						playStatusSound(player.getBukkitPlayer(),status);
						result = refreshCurrentView();
					}
					break;
				case C_PROMOTE:
					if (clickedIcon instanceof PlayerIcon) {
						PlayerIcon icon = (PlayerIcon)clickedIcon;
						OfflinePlayer clickPlayer = icon.getOfflinePlayer();
						boolean status = manager.menuPromoteOfficer(clickPlayer, kingdom);
						playStatusSound(player.getBukkitPlayer(),status);
						result = refreshCurrentView();
					}
					break;
				case C_DEMOTE:
					if (clickedIcon instanceof PlayerIcon) {
						PlayerIcon icon = (PlayerIcon)clickedIcon;
						OfflinePlayer clickPlayer = icon.getOfflinePlayer();
						boolean status = manager.menuDemoteOfficer(clickPlayer, kingdom);
						playStatusSound(player.getBukkitPlayer(),status);
						result = refreshCurrentView();
					}
					break;
				case C_TRANSFER:
					if (clickedIcon instanceof PlayerIcon) {
						PlayerIcon icon = (PlayerIcon)clickedIcon;
						OfflinePlayer clickPlayer = icon.getOfflinePlayer();
						boolean status = manager.menuTransferMaster(clickPlayer, kingdom, player);
						playStatusSound(player.getBukkitPlayer(),status);
					}
					break;
				case C_DESTROY:
					if (clickedIcon instanceof TownIcon) {
						TownIcon icon = (TownIcon)clickedIcon;
						KonTown clickTown = icon.getTown();
						boolean status = manager.menuDestroyTown(clickTown, player);
						playStatusSound(player.getBukkitPlayer(),status);
					}
					break;
				case C_CAPITAL:
					if (clickedIcon instanceof TownIcon) {
						TownIcon icon = (TownIcon)clickedIcon;
						KonTown clickTown = icon.getTown();
						boolean status = manager.menuCapitalSwap(clickTown, player, isAdmin);
						playStatusSound(player.getBukkitPlayer(),status);
					}
					break;
				case C_TEMPLATE:
					if (clickedIcon instanceof TemplateIcon) {
						TemplateIcon icon = (TemplateIcon)clickedIcon;
						KonMonumentTemplate template = icon.getTemplate();
						boolean status = manager.menuChangeKingdomTemplate(kingdom, template, player, isAdmin);
						playStatusSound(player.getBukkitPlayer(),status);
						result = refreshCurrentView();
					}
					break;
				case C_DISBAND:
					// Disband view, close this menu
					if (nextState == null) return null;
					if (nextState.equals(MenuState.CONFIRM_YES)) {
						// Disband the kingdom
						boolean status = manager.menuDisbandKingdom(kingdom,player);
						playStatusSound(player.getBukkitPlayer(),status);
					}
					break;
				default:
					break;
			}
		}
		return result;
	}
	
	private String getTitle(MenuState context) {
		String result = "error";
		switch(context) {
			case ROOT:
				result = MessagePath.MENU_MAIN_KINGDOM.getMessage();
				break;
			case A_JOIN:
				result = MessagePath.MENU_KINGDOM_TITLE_JOIN.getMessage();
				break;
			case A_EXILE:
				result = MessagePath.MENU_KINGDOM_TITLE_CONFIRM.getMessage();
				break;
			case A_INVITE:
				result = MessagePath.MENU_KINGDOM_TITLE_INVITES.getMessage();
				break;
			case A_LIST:
				result = MessagePath.MENU_KINGDOM_TITLE_LIST.getMessage();
				break;
			case B_RELATIONSHIP:
				result = MessagePath.MENU_KINGDOM_TITLE_RELATIONS.getMessage();
				break;
			case B_DIPLOMACY:
				result = MessagePath.MENU_KINGDOM_TITLE_DIPLOMACY.getMessage();
				break;
			case B_REQUESTS:
				result = MessagePath.MENU_KINGDOM_TITLE_REQUESTS.getMessage();
				break;
			case C_PROMOTE:
				result = MessagePath.MENU_KINGDOM_TITLE_PROMOTION.getMessage();
				break;
			case C_DEMOTE:
				result = MessagePath.MENU_KINGDOM_TITLE_DEMOTION.getMessage();
				break;
			case C_TRANSFER:
				result = MessagePath.MENU_KINGDOM_TITLE_TRANSFER.getMessage();
				break;
			case C_DESTROY:
				result = MessagePath.MENU_TOWN_TITLE_DESTROY.getMessage();
				break;
			case C_TEMPLATE:
				result = MessagePath.MENU_KINGDOM_TITLE_TEMPLATE.getMessage();
				break;
			case C_DISBAND:
				result = MessagePath.MENU_KINGDOM_TITLE_DISBAND.getMessage();
				break;
			case C_CAPITAL:
				result = MessagePath.MENU_KINGDOM_TITLE_CAPITAL.getMessage();
				break;
			default:
				break;
		}
		if (isAdmin) {
			result = DisplayManager.adminFormat + MessagePath.LABEL_ADMIN.getMessage() + " - " + result;
		}
		return result;
	}
	
}
