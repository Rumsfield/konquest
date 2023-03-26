package com.github.rumsfield.konquest.manager;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.api.event.player.KonquestPlayerExileEvent;
import com.github.rumsfield.konquest.api.event.player.KonquestPlayerKingdomEvent;
import com.github.rumsfield.konquest.api.manager.KonquestKingdomManager;
import com.github.rumsfield.konquest.api.model.*;
import com.github.rumsfield.konquest.display.icon.OptionIcon.optionAction;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.model.KonKingdomScoreAttributes.KonKingdomScoreAttribute;
import com.github.rumsfield.konquest.model.KonPlayerScoreAttributes.KonPlayerScoreAttribute;
import com.github.rumsfield.konquest.utility.Timer;
import com.github.rumsfield.konquest.utility.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;

//TODO: Split this class up into smaller manager classes, like
// - KingdomManager: All kingdom query, actions, memberships, etc
// - TownManager: All town query, actions, memberships, etc
// - DiplomacyManager: All kingdom relationships, diplomacy actions
public class KingdomManager implements KonquestKingdomManager, Timeable {

	/**
	 * KingdomManager.RelationRole
	 * These are all possible relationship states between kingdoms/towns/players.
	 */
	public enum RelationRole {
		/* Player/territory is barbarian */
		BARBARIAN,
		
		/* Territory has no kingdom (ruin, sanctuary, etc) */
		NEUTRAL,
		
		/* Player/territory are in the same kingdom */
		FRIENDLY,
		
		/* Player/territory are in kingdoms at war */
		ENEMY,

		/* Player/territory are in kingdoms at peace */
		PEACEFUL,

		/* Player/territory are in trading kingdoms */
		TRADE,

		/* Player/territory are in allied kingdoms */
		ALLY

	}
	
	private final Konquest konquest;
	private final HashMap<String, KonKingdom> kingdomMap;
	private KonKingdom barbarians;
	private KonKingdom neutrals;
	private final HashMap<PotionEffectType,Integer> townNerfs;
	private Material townCriticalBlock;
	private int maxCriticalHits;
	private final HashSet<Material> armorBlocks;
	private boolean isArmorBlockWhitelist;
	private int joinCooldownSeconds;
	private int exileCooldownSeconds;
	private final HashMap<UUID,Integer> joinPlayerCooldowns;
	private final HashMap<UUID,Integer> exilePlayerCooldowns;
	private ArrayList<DiplomacyTicket> diplomacyTickets;

	// Config Settings
	private long payIntervalSeconds;
	private double payPerChunk;
	private double payPerResident;
	private double payLimit;
	private int payPercentOfficer;
	private int payPercentMaster;
	private double costCreate;
	private double costRename;
	private double costTemplate;
	private boolean discountEnable;
	private int discountPercent;
	private boolean discountStack;
	private double costDiplomacyWar;
	private double costDiplomacyPeace;
	private double costDiplomacyTrade;
	private double costDiplomacyAlliance;

	private final Timer payTimer;
	
	public KingdomManager(Konquest konquest) {
		this.konquest = konquest;
		this.kingdomMap = new HashMap<>();
		this.barbarians = null;
		this.neutrals = null;
		this.townNerfs = new HashMap<>();
		this.townCriticalBlock = Material.OBSIDIAN;
		this.maxCriticalHits = 12;
		this.armorBlocks = new HashSet<>();
		this.isArmorBlockWhitelist = false;
		this.joinCooldownSeconds = 0;
		this.exileCooldownSeconds = 0;
		this.joinPlayerCooldowns = new HashMap<>();
		this.exilePlayerCooldowns = new HashMap<>();
		this.diplomacyTickets = new ArrayList<>();
		
		this.payIntervalSeconds = 0;
		this.payPerChunk = 0;
		this.payPerResident = 0;
		this.payLimit = 0;
		this.payPercentOfficer = 0;
		this.payPercentMaster = 0;
		this.costCreate = 0;
		this.costRename = 0;
		this.costTemplate = 0;
		this.costDiplomacyWar = 0;
		this.costDiplomacyPeace = 0;
		this.costDiplomacyTrade = 0;
		this.costDiplomacyAlliance = 0;
		this.payTimer = new Timer(this);
	}
	
	public void initialize() {
		barbarians = new KonKingdom(MessagePath.LABEL_BARBARIANS.getMessage(),konquest);
		neutrals = new KonKingdom(MessagePath.LABEL_NEUTRALS.getMessage(),konquest);
		loadCriticalBlocks();
		loadArmorBlacklist();
		loadJoinExileCooldowns();
		loadKingdoms();
		updateKingdomOfflineProtection();
		makeTownNerfs();
		ChatUtil.printDebug("Kingdom Manager is ready");
	}
	
	public void loadOptions() {
		// Kingdom Settings
		payIntervalSeconds 		= konquest.getCore().getLong(CorePath.FAVOR_KINGDOMS_PAY_INTERVAL_SECONDS.getPath());
		payPerChunk 			= konquest.getCore().getDouble(CorePath.FAVOR_KINGDOMS_PAY_PER_CHUNK.getPath());
		payPerResident 			= konquest.getCore().getDouble(CorePath.FAVOR_KINGDOMS_PAY_PER_RESIDENT.getPath());
		payLimit 				= konquest.getCore().getDouble(CorePath.FAVOR_KINGDOMS_PAY_LIMIT.getPath());
		payPercentOfficer   	= konquest.getCore().getInt(CorePath.FAVOR_KINGDOMS_BONUS_OFFICER_PERCENT.getPath());
		payPercentMaster    	= konquest.getCore().getInt(CorePath.FAVOR_KINGDOMS_BONUS_MASTER_PERCENT.getPath());
		costCreate 	        	= konquest.getCore().getDouble(CorePath.FAVOR_KINGDOMS_COST_CREATE.getPath());
		costRename 	        	= konquest.getCore().getDouble(CorePath.FAVOR_KINGDOMS_COST_RENAME.getPath());
		costTemplate        	= konquest.getCore().getDouble(CorePath.FAVOR_KINGDOMS_COST_TEMPLATE.getPath());
		costDiplomacyWar 		= konquest.getCore().getDouble(CorePath.FAVOR_DIPLOMACY_COST_WAR.getPath());
		costDiplomacyPeace 		= konquest.getCore().getDouble(CorePath.FAVOR_DIPLOMACY_COST_PEACE.getPath());
		costDiplomacyTrade 		= konquest.getCore().getDouble(CorePath.FAVOR_DIPLOMACY_COST_TRADE.getPath());
		costDiplomacyAlliance 	= konquest.getCore().getDouble(CorePath.FAVOR_DIPLOMACY_COST_ALLIANCE.getPath());
		
		payPerChunk = payPerChunk < 0 ? 0 : payPerChunk;
		payPerResident = payPerResident < 0 ? 0 : payPerResident;
		payLimit = payLimit < 0 ? 0 : payLimit;
		costCreate = costCreate < 0 ? 0 : costCreate;
		costRename = costRename < 0 ? 0 : costRename;
		costTemplate = costTemplate < 0 ? 0 : costTemplate;
		costDiplomacyWar = Math.max(costDiplomacyWar,0);
		costDiplomacyPeace = Math.max(costDiplomacyPeace,0);
		costDiplomacyTrade = Math.max(costDiplomacyTrade,0);
		costDiplomacyAlliance = Math.max(costDiplomacyAlliance,0);
		payPercentOfficer = Math.max(payPercentOfficer, 0);
		payPercentOfficer = Math.min(payPercentOfficer, 100);
		payPercentMaster = Math.max(payPercentMaster, 0);
		payPercentMaster = Math.min(payPercentMaster, 100);
		
		if(payIntervalSeconds > 0) {
			payTimer.stopTimer();
			payTimer.setTime((int)payIntervalSeconds);
			payTimer.startLoopTimer();
		}

		// Town Settings
		discountEnable 	    = konquest.getCore().getBoolean(CorePath.TOWNS_DISCOUNT_ENABLE.getPath());
		discountPercent		= konquest.getCore().getInt(CorePath.TOWNS_DISCOUNT_PERCENT.getPath());
		discountStack		= konquest.getCore().getBoolean(CorePath.TOWNS_DISCOUNT_STACK.getPath());

		discountPercent = Math.max(discountPercent,0);
		discountPercent = Math.min(discountPercent,100);
	}
	
	public double getCostDiplomacyWar() {
		return costDiplomacyWar;
	}

	public double getCostDiplomacyPeace() {
		return costDiplomacyPeace;
	}

	public double getCostDiplomacyTrade() {
		return costDiplomacyTrade;
	}

	public double getCostDiplomacyAlliance() {
		return costDiplomacyAlliance;
	}
	
	public double getCostCreate() {
		return costCreate;
	}
	
	public double getCostRename() {
		return costRename;
	}
	
	public double getCostTemplate() {
		return costTemplate;
	}

	public boolean getIsDiscountEnable() {
		return discountEnable;
	}

	public int getDiscountPercent() {
		return discountPercent;
	}

	public boolean getIsDiscountStack() {
		return discountStack;
	}
	
	@Override
	public void onEndTimer(int taskID) {
		if(taskID == 0) {
			ChatUtil.printDebug("Kingdom Pay Timer ended with null taskID!");
		} else if(taskID == payTimer.getTaskID()) {
			ChatUtil.printDebug("Kingdom Pay timer completed a new cycle");
			disbursePayments();
		}
		
	}
	
	/**
	 * Kingdom payments for members<br>
	 * Total payment is based on all towns & land.<br>
	 * Kingdom members only get paid based on the towns & land they are lord of.<br>
	 * Kingdom officers/master get a bonus = percentage of total payment.<br>
	 */
	private void disbursePayments() {
		HashMap<OfflinePlayer,Double> payments = new HashMap<>();
		HashMap<OfflinePlayer,KonKingdom> masters = new HashMap<>();
		HashMap<OfflinePlayer,KonKingdom> officers = new HashMap<>();
		HashMap<KonKingdom,Double> totalPay = new HashMap<>();
		// Initialize payment table
		for(KonKingdom kingdom : kingdomMap.values()) {
			totalPay.put(kingdom, 0.0);
			for(OfflinePlayer offlinePlayer : kingdom.getPlayerMembers()) {
				if(offlinePlayer.isOnline()) {
					payments.put(offlinePlayer, 0.0);
					if(kingdom.isMaster(offlinePlayer.getUniqueId())) {
						masters.put(offlinePlayer,kingdom);
					} else if(kingdom.isOfficer(offlinePlayer.getUniqueId())) {
						officers.put(offlinePlayer,kingdom);
					}
				}
			}
		}
		// Determine pay amounts by town
		OfflinePlayer lord;
		int land;
		int pop;
		double pay;
		for(KonKingdom kingdom : kingdomMap.values()) {
			for(KonTown town : kingdom.getTowns()) {
				lord = town.getPlayerLord();
				land = town.getChunkList().size();
				pop = town.getNumResidents();
				if(payments.containsKey(lord)) {
					double totalPrev = totalPay.get(kingdom);
					double lordPrev = payments.get(lord);
					pay = (land*payPerChunk) + (pop*payPerResident);
					payments.put(lord, lordPrev+pay);
					totalPay.put(kingdom, totalPrev+pay);
				}
			}
		}
		// Deposit payments
		double basePay;
		double bonusPay;
		double payAmount;
		for(OfflinePlayer offlinePlayer : payments.keySet()) {
			basePay = payments.get(offlinePlayer);
			bonusPay = 0;
			if(masters.containsKey(offlinePlayer)) {
				bonusPay = ((double)payPercentMaster/100) * totalPay.get(masters.get(offlinePlayer));
			} else if(officers.containsKey(offlinePlayer)) {
				bonusPay = ((double)payPercentOfficer/100) * totalPay.get(officers.get(offlinePlayer));
			}
			payAmount = basePay + bonusPay;
			if(payLimit > 0 && payAmount > payLimit) {
				payAmount = payLimit;
			}
			if(offlinePlayer.isOnline() && payAmount > 0) {
				Player player = (Player)offlinePlayer;
				if(KonquestPlugin.depositPlayer(player, payAmount)) {
	            	ChatUtil.sendNotice(player, MessagePath.COMMAND_KINGDOM_NOTICE_PAY.getMessage());
	            }
			}
		}
	}

	/**
	 * Checks location constraints for new towns/capitals
	 * @param loc - The location of the new territory
	 * @return Status Code
	 * 			0 - Success
	 * 			1 - Error, location is in an invalid world
	 * 			2 - Error, location too close to another territory
	 * 			3 - Error, location too far from other territories
	 * 			4 - Error, location overlaps with other territories
	 */
	private int validateTownLocationConstraints(Location loc) {
		// Verify valid world
		if(!konquest.isWorldValid(loc.getWorld())) {
			ChatUtil.printDebug("Failed to add town, invalid world: "+loc.getWorld().getName());
			return 1;
		}
		// Verify proximity to other territories
		int min_distance_sanc = konquest.getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_SANCTUARY.getPath());
		int min_distance_town = konquest.getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_TOWN.getPath());
		int searchDistance;
		int minDistance = Integer.MAX_VALUE;
		for(KonKingdom kingdom : kingdomMap.values()) {
			searchDistance = Konquest.chunkDistance(loc, kingdom.getCapital().getCenterLoc());
			if(searchDistance != -1 && min_distance_town > 0 && searchDistance < min_distance_town) {
				ChatUtil.printDebug("Failed to add town, too close to capital "+kingdom.getCapital().getName());
				return 2;
			}
			if(searchDistance != -1 && searchDistance < minDistance) {
				minDistance = searchDistance;
			}
			for(KonTown town : kingdom.getTowns()) {
				searchDistance = Konquest.chunkDistance(loc, town.getCenterLoc());
				if(searchDistance != -1 && min_distance_town > 0 && searchDistance < min_distance_town) {
					ChatUtil.printDebug("Failed to add town, too close to town "+town.getName());
					return 2;
				}
				if(searchDistance != -1 && searchDistance < minDistance) {
					minDistance = searchDistance;
				}
			}
		}
		for(KonRuin ruin : konquest.getRuinManager().getRuins()) {
			searchDistance = Konquest.chunkDistance(loc, ruin.getCenterLoc());
			if(searchDistance != -1 && min_distance_town > 0 && searchDistance < min_distance_town) {
				ChatUtil.printDebug("Failed to add town, too close to ruin "+ruin.getName());
				return 2;
			}
			if(searchDistance != -1 && searchDistance < minDistance) {
				minDistance = searchDistance;
			}
		}
		for(KonSanctuary sanctuary : konquest.getSanctuaryManager().getSanctuaries()) {
			searchDistance = Konquest.chunkDistance(loc, sanctuary.getCenterLoc());
			if(searchDistance != -1 && min_distance_sanc > 0 && searchDistance < min_distance_sanc) {
				ChatUtil.printDebug("Failed to add town, too close to sanctuary "+sanctuary.getName());
				return 2;
			}
			if(searchDistance != -1 && searchDistance < minDistance) {
				minDistance = searchDistance;
			}
		}
		// Verify max distance
		int maxLimit = konquest.getCore().getInt(CorePath.TOWNS_MAX_DISTANCE_ALL.getPath(),0);
		if(maxLimit > 0 && minDistance > maxLimit) {
			ChatUtil.printDebug("Failed to add town, too far from other towns and capitals");
			return 3;
		}
		// Verify no overlapping init chunks
		int radius = konquest.getCore().getInt(CorePath.TOWNS_INIT_RADIUS.getPath(),2);
		if(radius < 1) {
			radius = 1;
		}
		ChatUtil.printDebug("Checking for chunk conflicts with radius "+radius);
		for(Point point : konquest.getAreaPoints(loc, radius)) {
			if(konquest.getTerritoryManager().isChunkClaimed(point,loc.getWorld())) {
				ChatUtil.printDebug("Found a chunk conflict");
				return 4;
			}
		}
		// Return successfully
		return 0;
	}
	
	public void teleportAwayFromCenter(KonTown town) {
		// Teleport player to safe place around monument, facing monument
		for(KonPlayer occupant : konquest.getPlayerManager().getPlayersOnline()) {
			if(town.isLocInsideCenterChunk(occupant.getBukkitPlayer().getLocation())) {
				Location tpLoc = konquest.getSafeRandomCenteredLocation(town.getCenterLoc(), 2);
        		double x0,x1,z0,z1;
        		x0 = tpLoc.getX();
        		x1 = town.getCenterLoc().getX();
        		z0 = tpLoc.getZ();
        		z1 = town.getCenterLoc().getZ();
        		float yaw = (float)(180-(Math.atan2((x0-x1),(z0-z1))*180/Math.PI));
        		//ChatUtil.printDebug("Settle teleport used x0,z0;x1,z1: "+x0+","+z0+";"+x1+","+z1+" and calculated yaw degrees: "+yaw);
        		tpLoc.setYaw(yaw);
        		if(occupant.getBukkitPlayer().isInsideVehicle()) {
        			ChatUtil.printDebug("Settling occupant player is in a vehicle, type "+ occupant.getBukkitPlayer().getVehicle().getType());
        			Entity vehicle = occupant.getBukkitPlayer().getVehicle();
        			List<Entity> passengers = vehicle.getPassengers();
        			occupant.getBukkitPlayer().leaveVehicle();
        			occupant.getBukkitPlayer().teleport(tpLoc,TeleportCause.PLUGIN);
        			new BukkitRunnable() {
        				public void run() {
        					vehicle.teleport(tpLoc,TeleportCause.PLUGIN);
        					for (Entity e : passengers) {
        						vehicle.addPassenger(e);
        					}
        				}
        			}.runTaskLater(konquest.getPlugin(), 10L);
        			
        		} else {
        			occupant.getBukkitPlayer().teleport(tpLoc,TeleportCause.PLUGIN);
        		}
			}
		}
	}
	
	/*
	 * =================================================
	 * Kingdom Management Methods
	 * =================================================
	 */
	
	/**
	 * Creates a new kingdom.<br>
	 * The new kingdom must perform checks for creating the capital, similar to a new town.<br>
	 * The isAdmin argument controls whether the kingdom is admin operated (true) or player operated (false).<br>
	 * When the kingdom is player operated:<br>
	 * The player must be a barbarian, and is made the master of the kingdom;<br>
	 * The player must pay favor to create the kingdom.<br>
	 * The player is made the kingdom master, and lord of the capital.<br>
	 * When the kingdom is admin operated:<br>
	 * There is no cost to create, and there is no kingdom master.<br>
	 * 
	 * @param centerLocation - The center location of the kingdom, used for capital
	 * @param kingdomName - Kingdom name
	 * @param templateName - Template name
	 * @param master - Player who is the kingdom master
	 * @param isAdmin - Is the kingdom admin operated (true) or player operated (false)
	 * @return 0	- Success
	 * 	       1	- Bad name
	 * 		   2	- Master is not a barbarian
	 *         3    - Not enough favor
	 *         4	- Invalid monument template
	 *         5	- Invalid world
	 *         6 	- Too close to another territory
	 *         7	- Too far from other territories
	 *         8	- Will overlap with other territories
	 *         21 - error, town init fail, invalid monument
	 * 		   22 - error, town init fail, bad monument gradient
	 * 		   23 - error, town init fail, monument placed on bedrock
	 *   	   12 - error, town init fail, bad town height
	 *		   13 - error, town init fail, bad chunks
	 * 		   14 - error, town init fail, too much air below town
	 * 		   15 - error, town init fail, too much water below town
	 * 		   16 - error, town init fail, containers below monument
	 *         -1	- Internal error
	 */
	public int createKingdom(Location centerLocation, String kingdomName, String templateName, KonPlayer master, boolean isAdmin) {
		/*
		 * Criteria for creating a kingdom:
		 * 
		 * Player must be a barbarian and not a member of another kingdom.
		 * Player must have favor to pay the cost to create.
		 * Player must provide valid name.
		 * Player must specify valid template.
		 * The location must be valid for creating the capital territory.
		 * 
		 */
		Player bukkitPlayer = master.getBukkitPlayer();
		// Verify name
		if(konquest.validateNameConstraints(kingdomName) != 0) {
			return 1;
		}
		// Verify player is barbarian, not a member of another kingdom
		if(!isAdmin) {
			if(!master.isBarbarian()) {
				return 2;
			}
			for(KonKingdom kingdom : kingdomMap.values()) {
				if(kingdom.isMember(bukkitPlayer.getUniqueId())) {
					return 2;
				}
			}
		}
		// Verify valid monument template
		if(!konquest.getSanctuaryManager().isValidTemplate(templateName)) {
			return 4;
		}
		KonMonumentTemplate template = konquest.getSanctuaryManager().getTemplate(templateName);
		// Verify player can cover cost
		double totalCost = costCreate + template.getCost();
		if(!isAdmin && totalCost > 0 && KonquestPlugin.getBalance(bukkitPlayer) < totalCost) {
			return 3;
    	}
		
		// Verify position (return codes 4 - 8)
		/*
		 * 			0 - Success
		 * 			1 - Error, location is in an invalid world
		 * 			2 - Error, location too close to another territory
		 * 			3 - Error, location too far from other territories
		 * 			4 - Error, location overlaps with other territories
		 */
		int locStatus = validateTownLocationConstraints(centerLocation);
		if(locStatus != 0) {
			// Offset error code to fit in this method's return codes
			return locStatus+4;
		}
		
		/* Passed all checks, try to create kingdom */
		
		kingdomMap.put(kingdomName, new KonKingdom(centerLocation, kingdomName, konquest));
		
		KonKingdom newKingdom = kingdomMap.get(kingdomName);
		
		if(newKingdom != null) {
			newKingdom.setMonumentTemplate(template);
			// Initialize the capital territory, evaluate status
			// This may fail by internal town checks for gradient, height, etc.
			int initStatus = newKingdom.initCapital();
			
			if(initStatus == 0) {
				// Capital territory initialized successfully, finish kingdom setup
				konquest.getTerritoryManager().addAllTerritory(centerLocation.getWorld(),newKingdom.getCapital().getChunkList());
				konquest.getMapHandler().drawDynmapUpdateTerritory(newKingdom.getCapital());
				if(isAdmin) {
					// This kingdom is operated by admins only
					newKingdom.setIsAdminOperated(true);
				} else {
					// This kingdom is operated by players
					newKingdom.setIsAdminOperated(false);
					// Withdraw cost
					if(totalCost > 0 && KonquestPlugin.withdrawPlayer(bukkitPlayer, totalCost)) {
			            konquest.getAccomplishmentManager().modifyPlayerStat(master,KonStatsType.FAVOR,(int)totalCost);
					}
					// Assign player to kingdom
					int assignStatus = assignPlayerKingdom(bukkitPlayer.getUniqueId(), kingdomName, true);
					if(assignStatus != 0) {
						ChatUtil.printDebug("Failed to assign player "+bukkitPlayer.getName()+" to created kingdom "+kingdomName+", status code "+assignStatus);
					}
					// Make player master of kingdom
					newKingdom.forceMaster(bukkitPlayer.getUniqueId());
					// Make player lord of capital
					newKingdom.getCapital().setPlayerLord(bukkitPlayer);
				}
				// Update border particles
				konquest.getTerritoryManager().updatePlayerBorderParticles(master);
				newKingdom.getCapital().updateBarPlayers();
			} else {
				// Failed to pass all init checks, remove the kingdom
				newKingdom.removeCapital();
				kingdomMap.remove(kingdomName);
				newKingdom = null;
				return 10+initStatus;
			}
		} else {
			return -1;
		}
		return 0;
	}
	
	/**
	 * Deletes a kingdom (plus towns) and exiles all members.
	 * 
	 * @param name - The name of the kingdom to delete
	 * @return True when the deletion was successful, else false
	 */
	public boolean removeKingdom(String name) {
		if(kingdomMap.containsKey(name)) {
			KonKingdom kingdom = kingdomMap.get(name);
			// Clear all membership
			kingdom.clearMaster();
			kingdom.clearMembers();
			// Manually exile all members
			//TODO: Keep an eye on this. 
			// The allPlayers map should contain online KonPlayers as well.
			// Modifying a KonOfflinePlayer from the allPlayers map should reflect on any KonPlayers in
			// the onlinePlayer map too.
			for(KonOfflinePlayer member : konquest.getPlayerManager().getAllPlayersInKingdom(kingdom)) {
				String playerName = member.getOfflineBukkitPlayer().getName();
				member.setKingdom(getBarbarians());
				member.setExileKingdom(getBarbarians());
				member.setBarbarian(true);
				if(member instanceof KonPlayer) {
					ChatUtil.printDebug("Removing online KonPlayer "+playerName+" to barbarian.");
					KonPlayer onlinePlayer = (KonPlayer)member;
					// Online-only updates
					onlinePlayer.getPlayerPrefix().setEnable(false);
					konquest.updateNamePackets(onlinePlayer);
				} else {
					ChatUtil.printDebug("Removing offline KonOfflinePlayer "+playerName+" to barbarian.");
				}
				konquest.getDatabaseThread().getDatabase().setOfflinePlayer(member); // push to database
			}
			// Remove all towns
			for(KonTown town : kingdom.getTowns()) {
				removeTown(town.getName(),kingdom.getName());
			}
			// Clear all relationships
			kingdom.clearActiveRelations();
			kingdom.clearRelationRequests();
			for(KonKingdom otherKingdom : kingdomMap.values()) {
				otherKingdom.removeActiveRelation(kingdom);
				otherKingdom.removeRelationRequest(kingdom);
			}
			// Delete the kingdom
			kingdom = null;
			KonKingdom oldKingdom = kingdomMap.remove(name);
			if(oldKingdom != null) {
				// Remove capital
				oldKingdom.removeCapital();
				konquest.getTerritoryManager().removeAllTerritory(oldKingdom.getCapital().getWorld(),oldKingdom.getCapital().getChunkList().keySet());
				konquest.getMapHandler().drawDynmapRemoveTerritory(oldKingdom.getCapital());
				oldKingdom = null;
				// Update particle borders of everyone
				//TODO: optimize this to only nearby players?
				for(KonPlayer player : konquest.getPlayerManager().getPlayersOnline()) {
					konquest.getTerritoryManager().updatePlayerBorderParticles(player);
				}
				ChatUtil.printDebug("Removed Kingdom "+name);
				return true;
			}
			ChatUtil.printDebug("Failed to remove null Kingdom "+name);
		}
		ChatUtil.printDebug("Failed to remove unknown Kingdom "+name);
		return false;
	}
	
	/**
	 * Renames a kingdom
	 * @param oldName		The original name of the kingdom
	 * @param newName		The new name for the kingdom
	 * @param player		The player performing the name change
	 * @param ignoreCost	Bypass cost checks when true
	 * @return Status code
	 * 			0 - Success
	 * 			1 - Unknown kingdom
	 * 			2 - Invalid new name
	 * 			3 - Not enough favor
	 */
	public int renameKingdom(String oldName, String newName, KonPlayer player, boolean ignoreCost) {
		Player bukkitPlayer = player.getBukkitPlayer();
		
		if(!isKingdom(oldName)) {
			return 1;
		}
				
		if(konquest.validateNameConstraints(newName) == 0) {
			return 2;
		}
		
		// Check cost
		if(!ignoreCost && costRename > 0 && KonquestPlugin.getBalance(bukkitPlayer) < costRename) {
            return 3;
    	}
		
		// Perform rename
		KonKingdom oldKingdom = getKingdom(oldName);
		for (KonTown town : oldKingdom.getTowns()) {
			konquest.getMapHandler().drawDynmapRemoveTerritory(town);
		}
		konquest.getMapHandler().drawDynmapRemoveTerritory(oldKingdom.getCapital());
		getKingdom(oldName).setName(newName);
		KonKingdom kingdom = kingdomMap.remove(oldName);
		kingdomMap.put(newName, kingdom);
		kingdom.getCapital().updateName();
		konquest.getMapHandler().drawDynmapUpdateTerritory(kingdom.getCapital());
		for (KonTown town : kingdom.getTowns()) {
			konquest.getMapHandler().drawDynmapUpdateTerritory(town);
		}
		konquest.getDatabaseThread().getDatabase().setOfflinePlayers(konquest.getPlayerManager().getAllPlayersInKingdom(kingdom));
		
		// Withdraw cost
		if(!ignoreCost && costRename > 0 && KonquestPlugin.withdrawPlayer(bukkitPlayer, costRename)) {
            konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)costRename);
		}
		return 0;
	}

	public boolean isKingdomMembershipFull(KonKingdom kingdom) {
		// Check if max_player_diff is disabled, or if the desired kingdom is within the max diff
		int config_max_player_diff = konquest.getCore().getInt(CorePath.KINGDOMS_MAX_PLAYER_DIFF.getPath(),0);
		int smallestKingdomPlayerCount = 0;
		int targetKingdomPlayerCount = 0;
		if(config_max_player_diff > 0) {
			// Find the smallest kingdom, compare player count to this kingdom's
			String smallestKingdomName = getSmallestKingdomName();
			String checkKingdomName = kingdom.getName();
			smallestKingdomPlayerCount = konquest.getPlayerManager().getAllPlayersInKingdom(smallestKingdomName).size();
			targetKingdomPlayerCount = konquest.getPlayerManager().getAllPlayersInKingdom(checkKingdomName).size();
			return targetKingdomPlayerCount > smallestKingdomPlayerCount + config_max_player_diff;
		}
		return false;
	}

	/**
	 * Checks whether an online player may join the given kingdom,
	 * using common checks. This method is intended for these scenarios:
	 * - Player sending join request (closed kingdom) or trying to join (open kingdom).
	 * - Player accepting a join invite.
	 * The player must be online.
	 * @param player - The online player that is attempting to join
	 * @param kingdom - The target kingdom for joining
	 * @return True when the player may join, else false
	 */
	public boolean isPlayerJoinKingdomAllowed(KonPlayer player, KonKingdom kingdom) {
		UUID id = player.getBukkitPlayer().getUniqueId();
		// Check that player is not a member
		if(kingdom.equals(player.getKingdom()) || kingdom.isMember(id)) {
			return false;
		}
		// Check for player that's already a kingdom master
		for(KonKingdom otherKingdom : getKingdoms()) {
			if(otherKingdom.isMaster(id)) {
				return false;
			}
		}
		// Check for cool-down
		if(isPlayerJoinCooldown(player.getBukkitPlayer())) {
			return false;
		}
		// Check for permission (online only)
		boolean isPerKingdomJoin = konquest.getCore().getBoolean(CorePath.KINGDOMS_PER_KINGDOM_JOIN_PERMISSIONS.getPath(),false);
		String permission = "konquest.join."+kingdom.getName().toLowerCase();
		if(isPerKingdomJoin && !player.getBukkitPlayer().hasPermission(permission)) {
			return false;
		}
		// Check switching criteria
		boolean isAllowSwitch = konquest.getCore().getBoolean(CorePath.KINGDOMS_ALLOW_EXILE_SWITCH.getPath(),false);
		if(!isAllowSwitch && !kingdom.equals(player.getExileKingdom()) && !getBarbarians().equals(player.getExileKingdom())) {
			return false;
		}
		// Check if max_player_diff is disabled, or if the desired kingdom is within the max diff (both online & offline)
		if(isKingdomMembershipFull(kingdom)) {
			return false;
		}
		// Finished all checks
		return true;
	}

	/*
	 * =================================================
	 * Player Assignment Methods
	 * =================================================
	 */

	/*
	 * 1. Assign player to kingdom
	 * 		Player can be barbarian or kingdom member, but NOT master
	 * 2. Exile player to barbarians
	 * 		Player MUST be kingdom member, but NOT master
	 * 
	 * Players have two states: Barbarian or Kingdom Member.
	 * When a player wants to join a kingdom, the assignment method handles any 
	 * previous kingdom and joins the player to the new one.
	 * When a player wants to just leave a kingdom and have no kingdom, they become 
	 * barbarian by the exile method.
	 * 
	 * Players that are kingdom masters cannot exile/become barbarians.
	 * If the player is initiating the exile, they must first transfer master or disband.
	 * If an admin or konquest initiates a forced exile, then master is auto
	 * transferred to another member, or if no members exist, disband the kingdom.
	 * 
	 * Similarly, players that are kingdom masters cannot join other kingdoms.
	 * If the player tries to join another kingdom, they must first transfer master or disband.
	 * If an admin or konquest forces the player to join, then master is auto
	 * transferred to another member, or if no members exist, disband the kingdom.
	 */
	
	
	/**
	 * Primary method for adding a player member to a kingdom.
	 * Assign a player to a kingdom.
	 * Optionally checks for join permissions based on Konquest configuration.
	 * Optionally enforces maximum kingdom membership difference based on Konquest configuration.
	 * Applies join cool-down.
	 * 
	 * @param id The player to assign, by UUID
	 * @param kingdomName The kingdom name, case-sensitive
	 * @param force Ignore permission and max membership limits when true
	 * @return status
	 * 				0	- success
	 *  			1 	- kingdom name does not exist
	 *  			2 	- the kingdom is full (config option max_player_diff)
	 *  			3 	- missing permission
	 *  			4 	- cancelled
	 *  			5 	- joining is denied, player is already member
	 *  			6	- joining is denied, player is a kingdom master
	 *  			7	- joining is denied, failed switch criteria
	 *  			8   - joining is denied, cool-down
	 *  			9 	- Unknown player ID
	 *             -1 	- internal error
	 */
	public int assignPlayerKingdom(UUID id, String kingdomName, boolean force) {
		/*
		 * Constraints for joining a kingdom
		 * 
		 * Player may or may not be a barbarian.
		 * Player must not already be a member of target kingdom.
		 * Player must not be the master of another kingdom, they have to transfer master or disband kingdom first.
		 * Player can join a kingdom if...
		 * 		ExileKingdom is Barbarians (new player)
		 * 		ExileKingdom is the target kingdom (returning from exile)
		 * 		AllowSwitch flag is true
		 * (Optional) Player has permission for the kingdom to join
		 * (Optional) The target kingdom is below max player diff count
		 * 
		 * Joining a new kingdom must ensure the following...
		 * 		Removes player membership from previous kingdom
		 * 		Adds player membership to new kingdom
		 * 		Sets their new ExileKingdom to the target kingdom
		 * 		Removes old town residencies and updates territory bars
		 * 		Updates name packets for the player
		 * 		Updates dynmap label for old & new capitals
		 * 		Updates the smallest kingdom
		 * 		Updates offline protections
		 * 		Updates border particles for player
		 * 		
		 */
    	
		// Verify kingdom exists
		if(!isKingdom(kingdomName)) return 1;
		// Determine offline/online player
		// When an online player is found, modify their KonPlayer object.
		// When an offline player is found, update their KonOfflinePlayer object and push to the database.
		KonOfflinePlayer offlinePlayer = null;
		KonPlayer onlinePlayer = null;
		boolean isOnline = true;
		onlinePlayer = konquest.getPlayerManager().getPlayerFromID(id);
		if(onlinePlayer == null) {
			// No online player found, check offline players
			offlinePlayer = konquest.getPlayerManager().getOfflinePlayerFromID(id);
			if(offlinePlayer == null) {
				// No player found for the given UUID
				return 9;
			}
			isOnline = false;
		}
		// At this point, either onlinePlayer or offlinePlayer is null
		// isOnline = true -> onlinePlayer is not null
		// isOnline = false -> offlinePlayer is not null
		
		KonKingdom joinKingdom = getKingdom(kingdomName);
		
		// Check for existing membership (both online & offline)
		KonKingdom playerKingdom = isOnline ? onlinePlayer.getKingdom() : offlinePlayer.getKingdom();
		KonKingdom playerExileKingdom = isOnline ? onlinePlayer.getExileKingdom() : offlinePlayer.getExileKingdom();
		if(joinKingdom.equals(playerKingdom) || joinKingdom.isMember(id)) {
			return 5;
		}
		// These checks may be bypassed when forced
		if(!force) {
			// Check for player that's already a kingdom master
			for(KonKingdom kingdom : getKingdoms()) {
				if(kingdom.isMaster(id)) {
					return 6;
				}
			}
			// Check for cool-down
			boolean isCooldown = isOnline ? isPlayerJoinCooldown(onlinePlayer.getBukkitPlayer()) : isPlayerJoinCooldown(offlinePlayer.getOfflineBukkitPlayer());
			if(isCooldown) {
				return 8;
			}
			// Check for permission (online only)
			boolean isPerKingdomJoin = konquest.getCore().getBoolean(CorePath.KINGDOMS_PER_KINGDOM_JOIN_PERMISSIONS.getPath(),false);
			if(isOnline && isPerKingdomJoin) {
				String permission = "konquest.join."+joinKingdom.getName().toLowerCase();
				if(!onlinePlayer.getBukkitPlayer().hasPermission(permission)) {
					return 3;
				}
			}
			// Check switching criteria
			boolean isAllowSwitch = konquest.getCore().getBoolean(CorePath.KINGDOMS_ALLOW_EXILE_SWITCH.getPath(),false);
			if(!isAllowSwitch && !joinKingdom.equals(playerExileKingdom) && !getBarbarians().equals(playerExileKingdom)) {
				return 7;
			}
			// Check if max_player_diff is disabled, or if the desired kingdom is within the max diff (both online & offline)
			if(isKingdomMembershipFull(joinKingdom)) {
				return 2;
			}
			// Fire event (online only)
			if(isOnline) {
				KonquestPlayerKingdomEvent invokeEvent = new KonquestPlayerKingdomEvent(konquest, onlinePlayer, joinKingdom, onlinePlayer.getExileKingdom());
				Konquest.callKonquestEvent(invokeEvent);
				if(invokeEvent.isCancelled()) {
					return 4;
				}
			}
		}
		
		/* Passed all checks */
		// playerKingdom could be Barbarians
		
		// Remove any barbarian camps
		konquest.getCampManager().removeCamp(id.toString());

		// Only perform membership updates on created kingdoms
		if(playerKingdom.isCreated()) {
			// Remove membership from old kingdom
			boolean removeStatus = false;
	    	if(force) {
	    		// Force the removal of membership
	    		// The player could be master here
	    		// Potentially transfer master or disband old kingdom
	    		removeStatus = removeKingdomMemberForced(id,playerKingdom);
	    	} else {
	    		// The player shouldn't be master due to checks
	    		removeStatus = playerKingdom.removeMember(id);
	    	}
			if(removeStatus) {
				if(isOnline) {
					ChatUtil.sendNotice(onlinePlayer.getBukkitPlayer(), "Removed from kingdom and all towns");
				}
	    	} else {
	    		// Something went very wrong, the prior checks should have avoided this
	    		ChatUtil.printDebug("Failed to remove member "+ id +" from kingdom "+playerKingdom.getName());
	    		return -1;
	    	}
		}

		// Add membership to new kingdom
    	boolean memberStatus = joinKingdom.addMember(id, false);
    	if(!memberStatus) {
    		ChatUtil.printDebug("Failed to add member "+ id +" to kingdom "+joinKingdom.getName());
    		return -1;
    	}
    	
    	if(isOnline) {
    		onlinePlayer.setKingdom(joinKingdom);
    		onlinePlayer.setExileKingdom(joinKingdom);
    		onlinePlayer.setBarbarian(false);
    		// Refresh any territory bars
    		KonTerritory territory = konquest.getTerritoryManager().getChunkTerritory(onlinePlayer.getBukkitPlayer().getLocation());
    		if(territory != null && territory instanceof KonBarDisplayer) {
    			((KonBarDisplayer)territory).removeBarPlayer(onlinePlayer);
    			((KonBarDisplayer)territory).addBarPlayer(onlinePlayer);
    		}
        	// Updates
        	updateKingdomOfflineProtection();
        	konquest.updateNamePackets(onlinePlayer);
        	konquest.getTerritoryManager().updatePlayerBorderParticles(onlinePlayer);
        	// Apply cool-down
        	if(!force) {
        		applyPlayerJoinCooldown(onlinePlayer.getBukkitPlayer());
        	}
			// Broadcast
			ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_JOIN.getMessage(onlinePlayer.getBukkitPlayer().getName(),joinKingdom.getName()));
    	} else {
    		offlinePlayer.setKingdom(joinKingdom);
    		offlinePlayer.setExileKingdom(joinKingdom);
    		offlinePlayer.setBarbarian(false);
    		konquest.getDatabaseThread().getDatabase().setOfflinePlayer(offlinePlayer);
    	}
    	
    	konquest.getMapHandler().drawDynmapLabel(joinKingdom.getCapital());
    	updateSmallestKingdom();
    	
		return 0;
	}
	
	/**
	 * Exiles a player to be a Barbarian.
	 * Sets their exileKingdom value to their current Kingdom.
	 * (Optionally) Teleports to a random Wild location.
	 * (Optionally) Removes all stats and disables prefix.
	 * (Optionally) Removes all favor.
	 * (Optionally) Resets their exileKingdom to Barbarians, making them look like a new player to Konquest.
	 * Applies exile cool-down timer.
	 * 
	 * @param id The UUID of the player who is being exiled
	 * @param teleport Teleport the player based on Konquest configuration when true
	 * @param clearStats Remove all player stats and prefix when true
	 * @param isFull Perform a full exile such that the player has no exile kingdom, like they just joined the server
	 * @param force Ignore most checks when true
	 * @return status
	 * 				0	- success
	 * 				1	- Player is already a barbarian
	 * 				4	- cancelled by event
	 * 				6	- Exile denied, player is a kingdom master
	 * 				8   - Cool-down remaining
	 * 				9   - Unknown player ID
	 *             -1 	- internal error
	 */
	public int exilePlayerBarbarian(UUID id, boolean teleport, boolean clearStats, boolean isFull, boolean force) {
		
		// Determine offline/online player
		KonOfflinePlayer offlinePlayer = null;
		KonPlayer onlinePlayer = null;
		boolean isOnline = false;
		onlinePlayer = konquest.getPlayerManager().getPlayerFromID(id);
		if(onlinePlayer == null) {
			// No online player found, check offline players
			offlinePlayer = konquest.getPlayerManager().getOfflinePlayerFromID(id);
			if(offlinePlayer == null) {
				// No player found for the given UUID
				return 9;
			}
		} else {
			// Found an online player
			isOnline = true;
		}
		// Check for barbarian player or default kingdom
		KonKingdom oldKingdom = isOnline ? onlinePlayer.getKingdom() : offlinePlayer.getKingdom();
		boolean isPlayerBarbarian = isOnline ? onlinePlayer.isBarbarian() : offlinePlayer.isBarbarian();
		if(isPlayerBarbarian || !oldKingdom.isCreated()) {
    		return 1;
    	}
		// These checks are bypassed when forced
		if(!force) {
			// Check for player that's already a kingdom master
			for(KonKingdom kingdom : getKingdoms()) {
				if(kingdom.isMaster(id)) {
					return 6;
				}
			}
			// Check for cool-down
			if(isOnline && isPlayerExileCooldown(onlinePlayer.getBukkitPlayer())) {
				return 8;
			}
			// Fire event (online and offline)
			KonquestPlayerExileEvent invokeEvent = new KonquestPlayerExileEvent(konquest, offlinePlayer, oldKingdom);
			Konquest.callKonquestEvent(invokeEvent);
			if(invokeEvent.isCancelled()) {
				return 4;
			}
		}

		/* At this point, oldKingdom should be a player-created kingdom and most checks passed */
    	
		// Remove membership from old kingdom
    	// Now, the player should be in a created kingdom with a membership.
    	boolean removeStatus = false;
    	if(force) {
    		// Force the removal of membership
    		// The player could be master here
    		// Potentially transfer master or disband old kingdom
    		removeStatus = removeKingdomMemberForced(id,oldKingdom);
    	} else {
    		// The player shouldn't be master due to checks
    		removeStatus = oldKingdom.removeMember(id);
    	}
		if(!removeStatus) {
    		// Something went very wrong, the prior checks should have avoided this
    		ChatUtil.printDebug("Failed to remove member "+id.toString()+" from kingdom "+oldKingdom.getName());
    		return -1;
    	}
		
		/* Now, oldKingdom is possibly removed. Re-acquire current kingdom of player. */
		oldKingdom = isOnline ? onlinePlayer.getKingdom() : offlinePlayer.getKingdom();
		
    	// Try to teleport the player (online only)
		World playerWorld = onlinePlayer.getBukkitPlayer().getLocation().getWorld();
    	if(isOnline && teleport && playerWorld != null && konquest.isWorldValid(playerWorld)) {
    		boolean doWorldSpawn = konquest.getCore().getBoolean(CorePath.EXILE_TELEPORT_WORLD_SPAWN.getPath(), false);
    		boolean doWildTeleport = konquest.getCore().getBoolean(CorePath.EXILE_TELEPORT_WILD.getPath(), true);
    		Location exileLoc = null;
    		if(doWorldSpawn) {
    			exileLoc = playerWorld.getSpawnLocation();
    		} else if(doWildTeleport) {
    			exileLoc = konquest.getRandomWildLocation(playerWorld);
    		} else {
    			ChatUtil.printDebug("Teleport for player "+onlinePlayer.getBukkitPlayer().getName()+" on exile is disabled.");
    		}
	    	if(exileLoc != null) {
	    		konquest.telePlayerLocation(onlinePlayer.getBukkitPlayer(), exileLoc);
	    		onlinePlayer.getBukkitPlayer().setBedSpawnLocation(exileLoc, true);
	    	} else {
	    		ChatUtil.printDebug("Could not teleport player "+onlinePlayer.getBukkitPlayer().getName()+" on exile, disabled or null location.");
	    	}
    	}
    	
    	// Clear stats and prefix
    	boolean doRemoveStats = konquest.getCore().getBoolean(CorePath.EXILE_REMOVE_STATS.getPath(), true);
    	if(doRemoveStats && clearStats) {
    		if(isOnline) {
    			onlinePlayer.getPlayerStats().clearStats();
    			onlinePlayer.getPlayerPrefix().setEnable(false);
    		} else {
    			KonStats stats = konquest.getDatabaseThread().getDatabase().pullPlayerStats(offlinePlayer.getOfflineBukkitPlayer());
    			stats.clearStats();
    			konquest.getDatabaseThread().getDatabase().pushPlayerStats(offlinePlayer.getOfflineBukkitPlayer(), stats);
    			//TODO: KR Add means to clear prefix in database
    		}
    	}
    	
    	// Remove favor
    	boolean doRemoveFavor = konquest.getCore().getBoolean(CorePath.EXILE_REMOVE_FAVOR.getPath(), true);
    	OfflinePlayer offlineBukkitPlayer = isOnline ? onlinePlayer.getOfflineBukkitPlayer() : offlinePlayer.getOfflineBukkitPlayer();
    	if(doRemoveFavor) {
			double balance = KonquestPlugin.getBalance(offlineBukkitPlayer);
			KonquestPlugin.withdrawPlayer(offlineBukkitPlayer, balance);
		}
    	
    	KonKingdom exileKingdom = isFull ? getBarbarians() : oldKingdom;
    	if(isOnline) {
    		// Force into global chat mode
    		onlinePlayer.setIsGlobalChat(true);
        	// Make into barbarian
    		onlinePlayer.setKingdom(getBarbarians());
    		onlinePlayer.setExileKingdom(exileKingdom);
    		onlinePlayer.setBarbarian(true);
    		// Territory updates at player's location
    		KonTerritory territory = konquest.getTerritoryManager().getChunkTerritory(onlinePlayer.getBukkitPlayer().getLocation());
    		if(territory instanceof KonBarDisplayer) {
				// Update display bars
				KonBarDisplayer barDisplayer = (KonBarDisplayer)territory;
				barDisplayer.removeBarPlayer(onlinePlayer);
				barDisplayer.addBarPlayer(onlinePlayer);
    		}
			if(territory instanceof KonTown) {
				// Send a raid alert (barbarians are always a threat to towns)
				KonTown town = (KonTown)territory;
				town.sendRaidAlert();
			}
    		// General Updates
    		konquest.updateNamePackets(onlinePlayer);
    		konquest.getTerritoryManager().updatePlayerBorderParticles(onlinePlayer);
    		// Cool-down
    		if(!force) {
    			applyPlayerExileCooldown(onlinePlayer.getBukkitPlayer());
    		}
			ChatUtil.sendNotice(onlinePlayer.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_NOTICE_EXILE.getMessage());
    	} else {
    		offlinePlayer.setKingdom(getBarbarians());
    		offlinePlayer.setExileKingdom(exileKingdom);
    		offlinePlayer.setBarbarian(true);
    		konquest.getDatabaseThread().getDatabase().setOfflinePlayer(offlinePlayer);
    	}
    	
    	// Common updates
    	konquest.getMapHandler().drawDynmapLabel(oldKingdom.getCapital());
    	updateSmallestKingdom();
    	updateKingdomOfflineProtection();
    	
    	return 0;
	}
	
	/*
	 * ===================================
	 * Menu Membership Methods
	 * ===================================
	 * These methods are used by menu icons for membership management.
	 * Players requesting/invited to join, kicking members, promotions/demotions, etc.
	 */

	/**
	 * Online Player requests to join the kingdom to the officers.
	 * This method is meant to be called via the GUI menu.
	 * When the kingdom is closed, a join request is sent to officers.
	 * When open, the player instantly joins.
	 * @param player - The online player requesting to join the kingdom
	 * @param kingdom - The target kingdom that the player requests to join
	 */
	public boolean menuJoinKingdomRequest(KonPlayer player, KonKingdom kingdom) {
		if(kingdom == null) return false;
		UUID id = player.getBukkitPlayer().getUniqueId();
		// Player can be barbarians, or members of other kingdoms, when requesting to join a kingdom.
		
		// Cannot request to join your own current kingdom
		if(player.getKingdom().equals(kingdom) || kingdom.isMember(id)) {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}
		
		if(kingdom.isOpen() || kingdom.isJoinInviteValid(id)) {
			// There is already a valid invite, assign the player to the kingdom
			int status = assignPlayerKingdom(id,kingdom.getName(),false);
			if(status != 0) {
				switch(status) {
					case 2:
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_LIMIT.getMessage(kingdom.getName()));
						break;
					case 3:
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage());
						break;
					case 6:
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_MASTER.getMessage());
						break;
					case 7:
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_SWITCH.getMessage());
						break;
					case 8:
						String remainCooldown = getJoinCooldownRemainingTimeFormat(player.getBukkitPlayer(),ChatColor.RED);
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_COOLDOWN.getMessage(remainCooldown));
						break;
					default:
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
						break;
				}
				return false;
			}
			kingdom.removeJoinRequest(id);
			// Join broadcast is sent within assignPlayerKingdom, when successful
		} else if(!kingdom.isJoinRequestValid(id)){
			// Request to join if not already requested
			kingdom.addJoinRequest(id, false);
			ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_NOTICE_REQUEST_SENT.getMessage(kingdom.getName()));
			broadcastOfficers(kingdom, MessagePath.COMMAND_KINGDOM_NOTICE_REQUEST_RECEIVED.getMessage());
		} else {
			// A join request has already been sent, do nothing
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_REQUEST_EXISTS.getMessage(kingdom.getName()));
			return false;
		}
		return true;
	}
	
	/**
	 * Officer approves or denies join request of (offline) player.
	 * Check to ensure target player is not already a member.
	 * @param player - The officer player responding to the request
	 * @param requester - The offline player target of the response
	 * @param kingdom - The target kingdom that player wants to join
	 * @param resp - True to approve, false to reject request
	 * @return True when request response was successful, else false when target is already in this kingdom.
	 */
	public boolean menuRespondKingdomRequest(KonPlayer player, OfflinePlayer requester, KonKingdom kingdom, boolean resp) {
		if(kingdom == null) return true;
		UUID id = requester.getUniqueId();

		// Check the requester is not already a kingdom member
		if(kingdom.isMember(id)) {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_INVITE_MEMBER.getMessage());
			return false;
		}
		// Check for valid request
		if(!kingdom.isJoinRequestValid(id)) {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_FAILED.getMessage());
			return false;
		}
		// Is the request denied?
		if(!resp){
			// Denied request
			kingdom.removeJoinRequest(id);
			// Notify officer
			ChatUtil.sendNotice(player.getBukkitPlayer(),MessagePath.COMMAND_KINGDOM_NOTICE_REQUEST_DECLINED.getMessage(requester.getName()));
			// Notify requester
			if(requester.isOnline()) {
				ChatUtil.sendError((Player)requester, MessagePath.COMMAND_KINGDOM_ERROR_REQUEST_DENY.getMessage(kingdom.getName()));
			}
			return false;
		}

		int status = assignPlayerKingdom(id,kingdom.getName(),false);
		if(status != 0) {
			switch(status) {
				case 2:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_LIMIT.getMessage(kingdom.getName()));
					break;
				case 3:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage());
					break;
				case 6:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_MASTER.getMessage());
					break;
				case 7:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_SWITCH.getMessage());
					break;
				case 8:
					String remainCooldown = getJoinCooldownRemainingTimeFormat(requester,ChatColor.RED);
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_COOLDOWN.getMessage(remainCooldown));
					break;
				default:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
					break;
			}
			return false;
		}
		kingdom.removeJoinRequest(id);
		return true;
	}
	
	/**
	 * Officer invites (offline) player to join (with /kingdom add command).
	 * Kingdom must be closed to send invites.
	 * Allow invites to players already in another kingdom.
	 * Inviting a player who has already requested to join always lets them join.
	 * @param player - The officer doing the invite
	 * @param requester - The offline player that the officer invites to join their kingdom
	 * @param kingdom - The target kingdom for the join invite
	 * @return True when the invite was successful
	 */
	public boolean joinKingdomInvite(KonPlayer player, OfflinePlayer requester, KonKingdom kingdom) {
		if(kingdom == null) return false;
		UUID id = requester.getUniqueId();
		
		// Cannot invite a member of this kingdom
		if(kingdom.isMember(id)) {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_INVITE_MEMBER.getMessage(requester.getName()));
			return false;
		}
		// Cannot invite members to an open kingdom
		if(kingdom.isOpen()) {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_INVITE_OPEN.getMessage());
			return false;
		}
		
		if(kingdom.isJoinRequestValid(id)) {
			// There is already a valid request, add the player to the kingdom
			int status = assignPlayerKingdom(id,kingdom.getName(),false);
			if(status != 0) {
				switch(status) {
					case 2:
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_LIMIT.getMessage(kingdom.getName()));
						break;
					case 3:
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage());
						break;
					case 6:
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_MASTER.getMessage());
						break;
					case 7:
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_SWITCH.getMessage());
						break;
					case 8:
						String remainCooldown = getJoinCooldownRemainingTimeFormat(requester,ChatColor.RED);
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_COOLDOWN.getMessage(remainCooldown));
						break;
					default:
						ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
						break;
				}
				return false;
			}
			kingdom.removeJoinRequest(id);
		} else if(!kingdom.isJoinInviteValid(id)) {
			// Invite to join if not already invited
			kingdom.addJoinRequest(id, true);
			if(requester.isOnline()) {
				ChatUtil.sendNotice((Player)requester, MessagePath.COMMAND_KINGDOM_NOTICE_INVITE_RECEIVED.getMessage());
			}
		} else {
			// The invite has already been sent, still pending
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_INVITE_EXISTS.getMessage(requester.getName()));
			return false;
		}
		return true;
	}
	
	/**
	 * Player accepts or declines join invite
	 * @param player - The online player responding to the invite to join
	 * @param kingdom - The target kingdom that the player is responding to
	 * @param resp - True to accept and join kingdom, false to decline invite
	 */
	public boolean menuRespondKingdomInvite(KonPlayer player, KonKingdom kingdom, boolean resp) {
		if(kingdom == null) return false;
		UUID id = player.getBukkitPlayer().getUniqueId();
		
		// Cannot accept invite from a member of this kingdom
		if(kingdom.isMember(id)) {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_MEMBER.getMessage());
			return false;
		}
		// Check for valid invitation to join
		if(!kingdom.isJoinInviteValid(id)) {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_FAILED.getMessage());
			return false;
		}
		if(!resp){
			// Denied join request
			kingdom.removeJoinRequest(id);
			ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_NOTICE_INVITE_DECLINED.getMessage(kingdom.getName()));
			return false;
		}

		int status = assignPlayerKingdom(id,kingdom.getName(),false);
		if(status != 0) {
			switch(status) {
				case 2:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_LIMIT.getMessage(kingdom.getName()));
					break;
				case 3:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_PERMISSION.getMessage());
					break;
				case 6:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_MASTER.getMessage());
					break;
				case 7:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_SWITCH.getMessage());
					break;
				case 8:
					String remainCooldown = getJoinCooldownRemainingTimeFormat(player.getBukkitPlayer(),ChatColor.RED);
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_JOIN_COOLDOWN.getMessage(remainCooldown));
					break;
				default:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
					break;
			}
			return false;
		}
		kingdom.removeJoinRequest(id);
		return true;
	}
	
	// Effectively just a wrapper for exile for online players by their choice.
	// This method is called when a player chooses the exile button in the kingdom menu.
	public boolean menuExileKingdom(KonPlayer player) {
		UUID id = player.getBukkitPlayer().getUniqueId();
		// Attempt to exile player, sends notice
		int status = exilePlayerBarbarian(id,true,true,false,false);
		if(status == 0) {
			return true;
		} else {
			switch(status) {
				case 1:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
					break;
				case 4:
					// Cancelled by event, do nothing
					break;
				case 6:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_KICK_MASTER.getMessage());
					break;
				case 8:
					String remainCooldown = getExileCooldownRemainingTimeFormat(player.getBukkitPlayer(), ChatColor.RED);
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_EXILE_COOLDOWN.getMessage(remainCooldown));
					break;
				default:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
					break;
			}
			return false;
		}
	}
	
	// Wrapper for exile for offline players
	// Used by officers to remove players from their kingdom
	public boolean kickKingdomMember(KonPlayer player, OfflinePlayer member) {
		int status = exilePlayerBarbarian(member.getUniqueId(),true,true,false,false);
		if(status == 0) {
			return true;
		} else {
			switch (status) {
				case 1:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_KICK_BARBARIAN.getMessage());
					break;
				case 4:
					// Cancelled by event, do nothing
					break;
				case 6:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_KICK_MASTER.getMessage());
					break;
				case 8:
					String remainCooldown = getExileCooldownRemainingTimeFormat(member, ChatColor.RED);
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_ERROR_EXILE_COOLDOWN.getMessage(remainCooldown));
					break;
				default:
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
					break;
			}
			return false;
		}
	}
	
	/**
	 * Removes a kingdom member and handles master transfers.<br>
	 * This is mainly used for forcibly removing players from kingdoms, like for offline pruning.<br>
	 * If player is master, attempts to transfer master to officers first, then members.<br>
	 * If no other members than master, removes the kingdom.<br>
	 * <br>
	 * When there are other members, this method returns with the player removed from kingdom membership,<br>
	 * but the player's kingdom fields are unchanged.<br>
	 * When there are no other members, the kingdom is removed and the player's kingdom fields
	 * are modified to be barbarian.
	 * 
	 * @param id - The UUID of the player to remove
	 * @param kingdom - the kingdom to remove the player from
	 */
	private boolean removeKingdomMemberForced(UUID id, KonKingdom kingdom) {
		boolean result = false;
		if(kingdom == null || !kingdom.isCreated())return result;
		// Attempt to handle master transfer
		if(kingdom.isMaster(id)) {
			// Player is master, transfer if possible
			List<OfflinePlayer> officers = kingdom.getPlayerOfficersOnly();
			if(!officers.isEmpty()) {
				// Make the first officer into the master
				kingdom.setMaster(officers.get(0).getUniqueId());
				// Now remove the player
				result = kingdom.removeMember(id);
			} else {
				// There are no officers
				List<OfflinePlayer> members = kingdom.getPlayerMembersOnly();
				if(!members.isEmpty()) {
					// Make the first member into the master
					kingdom.setMaster(members.get(0).getUniqueId());
					// Now remove the player
					result = kingdom.removeMember(id);
				} else {
					// There are no members to transfer master to, remove the kingdom
					// This will also exile all players to barbarians, including the given ID
					String name = kingdom.getName();
					result = removeKingdom(name);
				}
			}
		} else {
			// Player is not the master, remove
			result = kingdom.removeMember(id);
		}

		return result;
	}

	// Get all kingdoms that have invited the player to join
	public List<KonKingdom> getInviteKingdoms(KonPlayer player) {
		List<KonKingdom> result = new ArrayList<>();
		for(KonKingdom kingdom : kingdomMap.values()) {
			if(!player.getKingdom().equals(kingdom) && kingdom.isJoinInviteValid(player.getBukkitPlayer().getUniqueId())) {
				result.add(kingdom);
			}
		}
		return result;
	}

	// Get all towns within the player's kingdom that have invited the player to join as a resident
	public List<KonTown> getInviteTowns(KonOfflinePlayer player) {
		List<KonTown> result = new ArrayList<>();
		for(KonTown town : player.getKingdom().getTowns()) {
			if(!town.isPlayerResident(player.getOfflineBukkitPlayer()) && town.isJoinInviteValid(player.getOfflineBukkitPlayer().getUniqueId())) {
				result.add(town);
			}
		}
		return result;
	}

	// Get all towns within the player's kingdom, of which the player is a knight or lord, which have resident join requests
	public List<KonTown> getRequestTowns(KonOfflinePlayer player) {
		List<KonTown> result = new ArrayList<>();
		for(KonTown town : player.getKingdom().getTowns()) {
			if(town.isPlayerKnight(player.getOfflineBukkitPlayer()) && !town.getJoinRequests().isEmpty()) {
				result.add(town);
			}
		}
		return result;
	}
	
	public boolean menuPromoteOfficer(OfflinePlayer player, KonKingdom kingdom) {
		UUID id = player.getUniqueId();
		// Check for player that's already an officer
		if(kingdom.isOfficer(id)) return false;
		// Attempt to promote
		if(kingdom.setOfficer(id, true)) {
			broadcastMembers(kingdom, MessagePath.COMMAND_KINGDOM_BROADCAST_PROMOTE.getMessage(player.getName(),kingdom.getName()));
			return true;
		}
		return false;
	}
	
	public boolean menuDemoteOfficer(OfflinePlayer player, KonKingdom kingdom) {
		UUID id = player.getUniqueId();
		// Check for player that's already a member
		if(!kingdom.isOfficer(id)) return false;
		// Attempt to demote
		if(kingdom.setOfficer(id, false)) {
			broadcastMembers(kingdom,MessagePath.COMMAND_KINGDOM_BROADCAST_DEMOTE.getMessage(player.getName(),kingdom.getName()));
			return true;
		}
		return false;
	}
	
	public boolean menuTransferMaster(OfflinePlayer master, KonKingdom kingdom, KonPlayer sender) {
		UUID id = master.getUniqueId();
		if(kingdom.isMember(id) && kingdom.setMaster(id)) {
			broadcastMembers(kingdom,MessagePath.COMMAND_KINGDOM_BROADCAST_TRANSFER.getMessage(master.getName(),kingdom.getName()));
			return true;
		} else {
			ChatUtil.sendError(sender.getBukkitPlayer(), MessagePath.GENERIC_ERROR_FAILED.getPath());
			return false;
		}
	}
	
	private void broadcastMembers(KonKingdom kingdom, String message) {
		if(kingdom == null)return;
		for(OfflinePlayer offlinePlayer : kingdom.getPlayerMembers()) {
			if(offlinePlayer.isOnline()) {
				Player player = (Player)offlinePlayer;
				ChatUtil.sendNotice(player, message);
			}
		}
	}
	
	private void broadcastOfficers(KonKingdom kingdom, String message) {
		if(kingdom == null) return;
		for(OfflinePlayer offlinePlayer : kingdom.getPlayerOfficers()) {
			if(offlinePlayer.isOnline()) {
				Player player = (Player)offlinePlayer;
				ChatUtil.sendNotice(player, message);
			}
		}
	}

	public void menuToggleKingdomOpen(KonKingdom kingdom, KonPlayer player) {
		if(kingdom == null || !kingdom.isCreated())return;
		if(kingdom.isOpen()) {
			kingdom.setIsOpen(false);
			ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_NOTICE_CLOSED.getMessage(kingdom.getName()));
		} else {
			kingdom.setIsOpen(true);
			ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_NOTICE_OPEN.getMessage(kingdom.getName()));
		}
	}
	
	public boolean menuDisbandKingdom(KonKingdom kingdom, KonPlayer player) {
		if(kingdom == null || !kingdom.isCreated()) return false;
		String kingdomName = kingdom.getName();
		boolean status = removeKingdom(kingdomName);
		if(status) {
			ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_DISBAND.getMessage(kingdomName));
			// Update stats
			konquest.getAccomplishmentManager().modifyPlayerStat(player, KonStatsType.KINGDOMS, -1);
			return true;
		} else {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return false;
		}
	}
	
	// The primary way to change a kingdom's template, from the kingdom GUI menu
	public boolean menuChangeKingdomTemplate(KonKingdom kingdom, KonMonumentTemplate template, KonPlayer player, boolean ignoreCost) {
		if(kingdom == null || !kingdom.isCreated()) return false;
		Player bukkitPlayer = player.getBukkitPlayer();
		// Verify template is valid
		if(template == null || !template.isValid()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_FAILED.getMessage());
			return false;
		}
		// Check capital is not under attack
		if(kingdom.getCapital().isAttacked()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_KINGDOM_ERROR_TEMPLATE_ATTACK.getMessage());
			return false;
		}
		// Check cost
		boolean hasTemplate = kingdom.hasMonumentTemplate();
		double totalCost = costTemplate + template.getCost();
		if(!ignoreCost && hasTemplate && totalCost > 0 && KonquestPlugin.getBalance(bukkitPlayer) < totalCost) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(totalCost));
			return false;
    	}
		// Update template
		kingdom.updateMonumentTemplate(template);
		// Withdraw cost
		if(!ignoreCost && hasTemplate && totalCost > 0 && KonquestPlugin.withdrawPlayer(bukkitPlayer, totalCost)) {
            konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)totalCost);
		}
		// Success
		ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_KINGDOM_NOTICE_TEMPLATE.getMessage(template.getName()));
		return true;
	}

	// The primary way to change a town's specialization
	public boolean menuChangeTownSpecialization(KonTown town, Villager.Profession profession, KonPlayer player, boolean isAdmin) {
		Player bukkitPlayer = player.getBukkitPlayer();
		double costSpecial = konquest.getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SPECIALIZE.getPath());
		// Check cost
		if(costSpecial > 0 && !isAdmin) {
			if(KonquestPlugin.getBalance(bukkitPlayer) < costSpecial) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(costSpecial));
				return false;
			}
		}
		town.setSpecialization(profession);
		// Withdraw cost
		if(costSpecial > 0 && !isAdmin) {
			if(KonquestPlugin.withdrawPlayer(bukkitPlayer, costSpecial)) {
				konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)costSpecial);
			}
		}
		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_SPECIALIZE.getMessage(town.getName(),profession.name()));
		return true;
	}
	
	
	/*
	 * =================================================
	 * Relationship Methods
	 * =================================================
	 */

	/**
	 * This method attempts to update the diplomatic states of two kingdoms.
	 * It handles validation of state changes, instant state changes and diplomatic requests.
	 * @param kingdom		The source kingdom initiating the change (ours)
	 * @param otherKingdom	The target kingdom in question (theirs)
	 * @param relation		The new diplomatic state to change to
	 * @param player		The player performing the change
	 * @param isAdmin		Whether the player is an admin (to bypass some checks)
	 * @return				True when the change was successful, else false when the change could not be processed
	 */
	public boolean menuChangeKingdomRelation(@Nullable KonKingdom kingdom, @Nullable  KonKingdom otherKingdom, @NotNull KonquestDiplomacyType relation, @NotNull KonPlayer player, boolean isAdmin) {

		if(kingdom == null || otherKingdom == null) return false;
		
		Player bukkitPlayer = player.getBukkitPlayer();
		// Check cost
		double costRelation = getRelationCost(relation);
		if(costRelation > 0 && !isAdmin) {
			if(KonquestPlugin.getBalance(bukkitPlayer) < costRelation) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(costRelation));
                return false;
			}
    	}

		// Our current state with the other kingdom
		KonquestDiplomacyType ourActiveState = kingdom.getActiveRelation(otherKingdom);
		// Their current request of us (if any)
		KonquestDiplomacyType ourRequestState = kingdom.getRelationRequest(otherKingdom);
		// Their current state with us
		KonquestDiplomacyType theirActiveState = otherKingdom.getActiveRelation(kingdom);
		// Our current diplomatic request of them (if any)
		KonquestDiplomacyType theirRequestState = otherKingdom.getRelationRequest(kingdom);

		ChatUtil.printDebug("Attempting to change kingdom "+kingdom.getName()+" relation to "+otherKingdom.getName()+": "+relation.toString());
		ChatUtil.printDebug("Our states ["+ourActiveState+","+ourRequestState+"]; Their states ["+theirActiveState+","+theirRequestState+"]");

		// Check for valid relation change given current states
		if(!isValidRelationChoice(kingdom, otherKingdom, relation)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_FAILED.getMessage());
			return false;
		}

		// Verify allowable current state combination
		if(!ourActiveState.equals(theirActiveState)) {
			// There is a relation mismatch, revert to default
			ChatUtil.printDebug("Found invalid state combination, reverting to default.");
			kingdom.removeActiveRelation(otherKingdom);
			otherKingdom.removeActiveRelation(kingdom);
			ChatUtil.sendError(bukkitPlayer,MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return false;
		}

		// Does any change to war by one side instantly force the other into war?
		boolean isInstantWar = konquest.getCore().getBoolean(CorePath.KINGDOMS_INSTANT_WAR.getPath(), false);
		// Does a change from war to peace by one side instantly force the other into peace?
		boolean isInstantPeace = konquest.getCore().getBoolean(CorePath.KINGDOMS_INSTANT_PEACE.getPath(), false);
		// Does a declaration of war also include the target kingdom's allies?
		boolean isDefensePact = konquest.getCore().getBoolean(CorePath.KINGDOMS_ALLY_DEFENSE_PACT.getPath(), false);

		/* At this point, the active states are valid and the change request can be processed */

		// Relationship transition logic
		switch(relation) {
			case PEACE:
				// Our kingdom wants to make peace
				if(ourActiveState.equals(KonquestDiplomacyType.WAR)) {
					// We are enemies, try to make peace
					if(isInstantPeace) {
						// Instantly change to active peace, force them to peace too
						setJointActiveRelation(kingdom,otherKingdom,relation);
						ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_PEACE.getMessage(kingdom.getName(),otherKingdom.getName()));
					} else {
						// Request peace from them
						if(kingdom.hasRelationRequest(otherKingdom) && kingdom.getRelationRequest(otherKingdom).equals(relation)) {
							// Both sides request peace, change to active peace
							setJointActiveRelation(kingdom,otherKingdom,relation);
							ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_PEACE.getMessage(kingdom.getName(),otherKingdom.getName()));
						} else {
							// We request peace, still at war
							if(otherKingdom.hasRelationRequest(kingdom) && otherKingdom.getRelationRequest(kingdom).equals(KonquestDiplomacyType.PEACE)) {
								ChatUtil.sendError(bukkitPlayer,MessagePath.COMMAND_KINGDOM_ERROR_DIPLOMACY_EXISTS.getMessage());
								return false;
							}
							otherKingdom.setRelationRequest(kingdom,relation);
							broadcastMembers(otherKingdom,MessagePath.COMMAND_KINGDOM_BROADCAST_PEACE_REQUEST.getMessage(kingdom.getName()));
						}
					}
				} else {
					// We are not enemies
					if(ourActiveState.equals(KonquestDiplomacyType.ALLIANCE)) {
						// We are allies, instantly break alliance and set both to peace
						setJointActiveRelation(kingdom,otherKingdom,relation);
						ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_NO_ALLY.getMessage(kingdom.getName(),otherKingdom.getName()));
					} else if(ourActiveState.equals(KonquestDiplomacyType.TRADE)) {
						// We are trade partners, instantly go back to peace
						setJointActiveRelation(kingdom,otherKingdom,relation);
						ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_NO_TRADE.getMessage(kingdom.getName(),otherKingdom.getName()));
					}
				}
				break;
			case TRADE:
				// Our kingdom wants to trade with the other
				if(ourActiveState.equals(KonquestDiplomacyType.PEACE)) {
					// We are at peace, request to trade
					if(kingdom.hasRelationRequest(otherKingdom) && kingdom.getRelationRequest(otherKingdom).equals(relation)) {
						// Both sides request trade, change to active trading
						setJointActiveRelation(kingdom,otherKingdom,relation);
						ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_TRADE.getMessage(kingdom.getName(),otherKingdom.getName()));
					} else {
						// We request trade
						if(otherKingdom.hasRelationRequest(kingdom) && otherKingdom.getRelationRequest(kingdom).equals(KonquestDiplomacyType.TRADE)) {
							ChatUtil.sendError(bukkitPlayer,MessagePath.COMMAND_KINGDOM_ERROR_DIPLOMACY_EXISTS.getMessage());
							return false;
						}
						otherKingdom.setRelationRequest(kingdom,relation);
						broadcastMembers(otherKingdom,MessagePath.COMMAND_KINGDOM_BROADCAST_TRADE_REQUEST.getMessage(kingdom.getName()));
					}
				} else if(ourActiveState.equals(KonquestDiplomacyType.ALLIANCE)) {
					// We are allies, instantly break alliance and set both to trade
					removeJointActiveRelation(kingdom,otherKingdom);
					ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_NO_ALLY.getMessage(kingdom.getName(),otherKingdom.getName()));
				}
				break;
			case ALLIANCE:
				// Our kingdom wants to ally the other
				if(kingdom.hasRelationRequest(otherKingdom) && kingdom.getRelationRequest(otherKingdom).equals(relation)) {
					// Both sides request allied, change to active alliance
					setJointActiveRelation(kingdom,otherKingdom,relation);
					ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_ALLY.getMessage(kingdom.getName(),otherKingdom.getName()));
				} else {
					// We request allied
					if(otherKingdom.hasRelationRequest(kingdom) && otherKingdom.getRelationRequest(kingdom).equals(KonquestDiplomacyType.ALLIANCE)) {
						ChatUtil.sendError(bukkitPlayer,MessagePath.COMMAND_KINGDOM_ERROR_DIPLOMACY_EXISTS.getMessage());
						return false;
					}
					otherKingdom.setRelationRequest(kingdom,relation);
					broadcastMembers(otherKingdom,MessagePath.COMMAND_KINGDOM_BROADCAST_ALLY_REQUEST.getMessage(kingdom.getName()));
				}
				break;
			case WAR:
				// Our kingdom wants to declare war
				boolean isWarDeclared = false;
				if(isInstantWar) {
					// Instantly change to active enemy, force them to enemy too
					setJointActiveRelation(kingdom,otherKingdom,relation);
					ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_WAR.getMessage(kingdom.getName(),otherKingdom.getName()));
					isWarDeclared = true;
				} else {
					// Request war
					// Check if they already request enemy
					if(kingdom.hasRelationRequest(otherKingdom) && kingdom.getRelationRequest(otherKingdom).equals(relation)) {
						// Both sides request enemy, change to active war
						setJointActiveRelation(kingdom,otherKingdom,relation);
						ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_WAR.getMessage(kingdom.getName(),otherKingdom.getName()));
						isWarDeclared = true;
					} else {
						// We request enemy
						if(otherKingdom.hasRelationRequest(kingdom) && otherKingdom.getRelationRequest(kingdom).equals(KonquestDiplomacyType.WAR)) {
							ChatUtil.sendError(bukkitPlayer,MessagePath.COMMAND_KINGDOM_ERROR_DIPLOMACY_EXISTS.getMessage());
							return false;
						}
						otherKingdom.setRelationRequest(kingdom, relation);
						broadcastMembers(otherKingdom,MessagePath.COMMAND_KINGDOM_BROADCAST_WAR_REQUEST.getMessage(kingdom.getName()));
					}
				}
				// Check for defense pact with allies
				if(isWarDeclared && isDefensePact) {
					// Set war with all allies of target kingdom
					for(KonKingdom otherAlly : otherKingdom.getActiveRelationKingdoms(KonquestDiplomacyType.ALLIANCE)) {
						if(!otherAlly.equals(kingdom)) {
							// Declare war on their allies
							setJointActiveRelation(kingdom,otherAlly,relation);
							ChatUtil.sendBroadcast(MessagePath.COMMAND_KINGDOM_BROADCAST_WAR.getMessage(kingdom.getName(),otherAlly.getName()));
						}
					}
				}
				break;
			default:
				break;
		}
		
		// Debug messaging
		// Our current state with the other kingdom
		KonquestDiplomacyType ourNewActiveState = kingdom.getActiveRelation(otherKingdom);
		// Their current request of us (if any)
		KonquestDiplomacyType ourNewRequestState = kingdom.getRelationRequest(otherKingdom);
		// Their current state with us
		KonquestDiplomacyType theirNewActiveState = otherKingdom.getActiveRelation(kingdom);
		// Our current diplomatic request of them (if any)
		KonquestDiplomacyType theirNewRequestState = otherKingdom.getRelationRequest(kingdom);
		ChatUtil.printDebug("Finished; Our states ["+ourNewActiveState+","+ourNewRequestState+"]; Their states ["+theirNewActiveState+","+theirNewRequestState+"]");

		// Update all kingdoms & players for new relationship (when it changes)
		// 1. Update all player name colors
		// 2. Update all border particles
		// 3. Update all territory display bars
		if(!ourActiveState.equals(ourNewActiveState)) {
			konquest.updateNamePackets(kingdom);
			konquest.getTerritoryManager().updatePlayerBorderParticles(kingdom);
			konquest.getTerritoryManager().updateTownDisplayBars(kingdom);
		}
		if(!theirActiveState.equals(theirNewActiveState)) {
			konquest.updateNamePackets(otherKingdom);
			konquest.getTerritoryManager().updatePlayerBorderParticles(otherKingdom);
			konquest.getTerritoryManager().updateTownDisplayBars(otherKingdom);
		}

		// Withdraw cost
		if(costRelation > 0 && !isAdmin) {
            if(KonquestPlugin.withdrawPlayer(bukkitPlayer, costRelation)) {
            	konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)costRelation);
            }
		}

		return true;
	}
	
	public boolean isValidRelationChoice(@NotNull KonKingdom kingdom1,@NotNull KonKingdom kingdom2,@NotNull KonquestDiplomacyType relation) {
		KonquestDiplomacyType ourRelation = kingdom1.getActiveRelation(kingdom2);
		if(ourRelation.equals(relation)) {
			// Cannot change to existing active relation
			return false;
		} else if(ourRelation.equals(KonquestDiplomacyType.WAR) && !relation.equals(KonquestDiplomacyType.PEACE)) {
			// Can only change to peace when currently enemy
			return false;
		} else if(ourRelation.equals(KonquestDiplomacyType.ALLIANCE) && relation.equals(KonquestDiplomacyType.WAR)) {
			// Cannot change to war when currently Allied
			return false;
		}
		return true;
	}

	public double getRelationCost(@NotNull KonquestDiplomacyType relation) {
		switch(relation) {
			case WAR:
				return costDiplomacyWar;
			case PEACE:
				return costDiplomacyPeace;
			case TRADE:
				return costDiplomacyTrade;
			case ALLIANCE:
				return costDiplomacyAlliance;
			default:
				return 0;
		}
	}
	
	private void setJointActiveRelation(@NotNull KonKingdom kingdom1,@NotNull KonKingdom kingdom2,@NotNull KonquestDiplomacyType relation) {
		// Force both kingdoms to have the given active relationship
		// Remove any relation requests
		kingdom1.setActiveRelation(kingdom2, relation);
		kingdom1.removeRelationRequest(kingdom2);
		kingdom2.setActiveRelation(kingdom1, relation);
		kingdom2.removeRelationRequest(kingdom1);
	}
	
	private void removeJointActiveRelation(@NotNull KonKingdom kingdom1,@NotNull KonKingdom kingdom2) {
		kingdom1.removeActiveRelation(kingdom2);
		kingdom1.removeRelationRequest(kingdom2);
		kingdom2.removeActiveRelation(kingdom1);
		kingdom2.removeRelationRequest(kingdom1);
	}

	/**
	 * Get the shared diplomatic type of two kingdoms.
	 * Performs error checking to ensure both kingdoms have the same active relation.
	 * If there is a mismatch, they are reset to default (peace).
	 * @param kingdom1 The reference kingdom
	 * @param kingdom2 The target kingdom
	 * @return The diplomatic type
	 */
	public KonquestDiplomacyType getDiplomaticState(@NotNull KonquestKingdom kingdom1, @NotNull KonquestKingdom kingdom2) {
		// Default kingdoms are always at war (barbarian & neutral)
		if(!kingdom1.isCreated() || !kingdom2.isCreated()) {
			return KonquestDiplomacyType.WAR;
		}
		// Evaluate two created kingdoms
		if(kingdom1.getActiveRelation(kingdom2).equals(kingdom2.getActiveRelation(kingdom1))) {
			// Both kingdoms have a matching active relation
			return kingdom1.getActiveRelation(kingdom2);
		} else {
			// There is a relation mismatch, revert to default
			if(kingdom1 instanceof KonKingdom) {
				((KonKingdom)kingdom1).removeActiveRelation(kingdom2);
			}
			if(kingdom2 instanceof KonKingdom) {
				((KonKingdom)kingdom2).removeActiveRelation(kingdom1);
			}
			return KonquestDiplomacyType.getDefault();
		}
	}

	public boolean isKingdomWar(@NotNull KonquestKingdom kingdom1, @NotNull KonquestKingdom kingdom2) {
		return getDiplomaticState(kingdom1, kingdom2).equals(KonquestDiplomacyType.WAR);
	}

	public boolean isKingdomPeace(@NotNull KonquestKingdom kingdom1, @NotNull KonquestKingdom kingdom2) {
		return getDiplomaticState(kingdom1, kingdom2).equals(KonquestDiplomacyType.PEACE);
	}

	public boolean isKingdomTrade(@NotNull KonquestKingdom kingdom1, @NotNull KonquestKingdom kingdom2) {
		return getDiplomaticState(kingdom1, kingdom2).equals(KonquestDiplomacyType.TRADE);
	}

	public boolean isKingdomAlliance(@NotNull KonquestKingdom kingdom1, @NotNull KonquestKingdom kingdom2) {
		return getDiplomaticState(kingdom1, kingdom2).equals(KonquestDiplomacyType.ALLIANCE);
	}
	
	public RelationRole getRelationRole(@Nullable KonquestKingdom displayKingdom, @Nullable KonquestKingdom contextKingdom) {
    	if(displayKingdom == null || contextKingdom == null) {
    		ChatUtil.printDebug("Failed to evaluate relation of null kingdom");
    		return RelationRole.NEUTRAL;
    	}
		RelationRole result;
    	if(contextKingdom.equals(getBarbarians())) {
    		result = RelationRole.BARBARIAN;
		} else if(contextKingdom.equals(getNeutrals())) {
    		result = RelationRole.NEUTRAL;
		} else {
			if(displayKingdom.equals(contextKingdom)) {
				result = RelationRole.FRIENDLY;
    		} else if (displayKingdom.equals(getBarbarians())) {
    			result = RelationRole.ENEMY;
    		} else {
    			if(isKingdomWar(displayKingdom, contextKingdom)) {
    				result = RelationRole.ENEMY;
				} else if(isKingdomAlliance(displayKingdom, contextKingdom)) {
					result = RelationRole.ALLY;
				} else if(isKingdomTrade(displayKingdom, contextKingdom)) {
					result = RelationRole.TRADE;
				} else if(isKingdomPeace(displayKingdom, contextKingdom)) {
					result = RelationRole.PEACEFUL;
				} else {
					result = RelationRole.NEUTRAL;
				}
    		}
		}
    	return result;
    }

    public boolean isPlayerEnemy(@NotNull KonOfflinePlayer offlinePlayer,@NotNull KonKingdom kingdom) {
    	return getRelationRole(offlinePlayer.getKingdom(),kingdom).equals(RelationRole.ENEMY);
    }

	public boolean isPlayerAlly(@NotNull KonOfflinePlayer offlinePlayer,@NotNull KonKingdom kingdom) {
		return getRelationRole(offlinePlayer.getKingdom(),kingdom).equals(RelationRole.ALLY);
	}
    
    public boolean isPlayerFriendly(@NotNull KonOfflinePlayer offlinePlayer,@NotNull KonKingdom kingdom) {
    	return getRelationRole(offlinePlayer.getKingdom(),kingdom).equals(RelationRole.FRIENDLY);
    }

	public boolean isPlayerTrade(@NotNull KonOfflinePlayer offlinePlayer,@NotNull KonKingdom kingdom) {
		return getRelationRole(offlinePlayer.getKingdom(),kingdom).equals(RelationRole.TRADE);
	}

	public boolean isPlayerPeace(@NotNull KonOfflinePlayer offlinePlayer,@NotNull KonKingdom kingdom) {
		return getRelationRole(offlinePlayer.getKingdom(),kingdom).equals(RelationRole.PEACEFUL);
	}

	public boolean isPlayerForeign(@NotNull KonOfflinePlayer offlinePlayer,@NotNull KonKingdom kingdom) {
		return !getRelationRole(offlinePlayer.getKingdom(),kingdom).equals(RelationRole.FRIENDLY);
	}

	public boolean isKingdomBarbarian(@NotNull KonKingdom kingdom) {
		return kingdom.equals(barbarians);
	}

	public boolean isKingdomNeutral(@NotNull KonKingdom kingdom) {
		return kingdom.equals(neutrals);
	}

	/*
	 * =================================================
	 * Town General Methods
	 * =================================================
	 */
	
	
	/**
	 * createTown - Primary method for adding a town
	 * @param loc - location of town center
	 * @param name - name of town
	 * @param kingdomName - name of kingdom that the town belongs to
	 * @return  0 - success
	 * 			1 - error, chunk claim conflict
	 * 			2 - error, bad monument placement
	 * 			3 - error, bad name
	 * 			4 - error, invalid monument template
	 * 			5 - error, bad town placement, invalid world
	 * 			6 - error, bad town placement, too close
	 * 			7 - error, bad town placement, too far
	 * 		   21 - error, town init fail, invalid monument
	 * 		   22 - error, town init fail, bad monument gradient
	 * 		   23 - error, town init fail, monument placed on bedrock
	 *   	   12 - error, town init fail, bad town height
	 *		   13 - error, town init fail, bad chunks
	 * 		   14 - error, town init fail, too much air below town
	 * 		   15 - error, town init fail, too much water below town
	 * 		   16 - error, town init fail, containers below monument
	 */
	public int createTown(@NotNull Location loc, String name, String kingdomName) {
		ChatUtil.printDebug("Attempting to add new town "+name+" for kingdom "+kingdomName);
		
		// Verify name
		if(konquest.validateNameConstraints(name) != 0) {
			ChatUtil.printDebug("Town name "+name+" failed name validation");
			return 3;
		}
		if(!isKingdom(kingdomName)) {
			ChatUtil.printDebug("Kingdom name "+kingdomName+" failed name validation");
			return 3;
		}
		
		// Verify position
		int locStatus = validateTownLocationConstraints(loc);
		// Map original error codes
		/*
		 * 			0 - Success
		 * 			1 - Error, location is in an invalid world
		 * 			2 - Error, location too close to another territory
		 * 			3 - Error, location too far from other territories
		 * 			4 - Error, location overlaps with other territories
		 */
		if(locStatus == 1) {
			return 5;
		} else if(locStatus == 2) {
			return 6;
		} else if(locStatus == 3) {
			return 7;
		} else if(locStatus == 4) {
			return 1;
		}
		
		// Verify valid monument template
		if(!getKingdom(kingdomName).isMonumentTemplateValid()) {
			ChatUtil.printDebug("Failed to create town with invalid or blanked monument template.");
			return 4;
		}	
			
		/* Passed checks, start to create town */
		
		// Modify location to max Y at given X,Z
		Point point = Konquest.toPoint(loc);
		int xLocal = loc.getBlockX() - (point.x*16);
		int zLocal = loc.getBlockZ() - (point.y*16);
		Chunk chunk = loc.getChunk();
		int yFloor = chunk.getChunkSnapshot(true, false, false).getHighestBlockYAt(xLocal, zLocal);
		while((chunk.getBlock(xLocal, yFloor, zLocal).isPassable() || !chunk.getBlock(xLocal, yFloor, zLocal).getType().isOccluding()) && yFloor > 0) {
			yFloor--;
		}
		loc.setY(yFloor+1);
		// Attempt to add the town
		if(getKingdom(kingdomName).addTown(loc, name)) {
			// Attempt to initialize the town
			int initStatus = getKingdom(kingdomName).initTown(name);
			if(initStatus == 0) {
				// When a town is added successfully, update the chunk cache
				konquest.getTerritoryManager().addAllTerritory(loc.getWorld(),getKingdom(kingdomName).getTown(name).getChunkList());
				konquest.getMapHandler().drawDynmapUpdateTerritory(getKingdom(kingdomName).getTown(name));
				// Update territory bar
				getKingdom(kingdomName).getTown(name).updateBarPlayers();
				return 0;
			} else {
				// Remove town if init fails, exit code 10+
				getKingdom(kingdomName).removeTown(name);
				return 10+initStatus;
			}
		} else {
			return 3;
		}
	}
	
	/**
	 * Primary method for removing a town
	 * @param name - name of town
	 * @param kingdomName - name of kingdom that the town belongs to
	 * @return true on success, false on failure
	 */
	public boolean removeTown(String name, String kingdomName) {
		if(!isKingdom(kingdomName) || !getKingdom(kingdomName).hasTown(name))return false;
		KonKingdom kingdom = getKingdom(kingdomName);
		KonTown town = kingdom.getTown(name);
		ArrayList<Point> townPoints = new ArrayList<>(town.getChunkList().keySet());
		clearAllTownNerfs(town);
		clearAllTownHearts(town);
		town.removeAllBarPlayers();
		if(!kingdom.removeTown(name)) return false;
		// When a town is removed successfully, update the chunk cache
		//updateTerritoryCache();
		konquest.getTerritoryManager().removeAllTerritory(town.getWorld(),townPoints);
		konquest.getMapHandler().drawDynmapRemoveTerritory(town);
		konquest.getMapHandler().drawDynmapLabel(town.getKingdom().getCapital());
		konquest.getIntegrationManager().getQuickShop().deleteShopsInPoints(townPoints,town.getWorld());
		return true;
	}

	/**
	 * Primary method for renaming a town
	 * @param oldName - old name of town
	 * @param newName - new name of town
	 * @param kingdomName - name of kingdom town belongs to
	 * @return true on success, false on failure
	 */
	public boolean renameTown(String oldName, String newName, String kingdomName) {
		if(!isKingdom(kingdomName) || !getKingdom(kingdomName).hasTown(oldName))return false;
		KonKingdom kingdom = getKingdom(kingdomName);
		konquest.getMapHandler().drawDynmapRemoveTerritory(kingdom.getTown(oldName));
		boolean success = kingdom.renameTown(oldName, newName);
		if(!success) return false;
		KonTown town = kingdom.getTown(newName);
		if (town != null) {
			konquest.getMapHandler().drawDynmapUpdateTerritory(town);
		}
		return true;
	}
	
	/**
	 * Primary method for transferring ownership of a town between kingdoms
	 * @param name - Town name
	 * @param oldKingdomName - Kingdom name of conquered Town
	 * @param conquerPlayerArg - Player who initiated the conquering
	 * @return the captured town
	 */
	@Nullable
	public KonquestTown captureTownForPlayer(String name, String oldKingdomName,@Nullable KonquestPlayer conquerPlayerArg) {
		if(conquerPlayerArg == null) return null;
		if(!(conquerPlayerArg instanceof KonPlayer)) return null;
		KonPlayer conquerPlayer = (KonPlayer)conquerPlayerArg;
		
		if(conquerPlayer.isBarbarian()) return null;
		KonTown town = captureTown(name, oldKingdomName, conquerPlayer.getKingdom());
		if(town != null) {
			town.setPlayerLord(conquerPlayer.getOfflineBukkitPlayer());
			return town;
		}
		return null;
	}

	@Nullable
	public KonTown captureTown(String name, String oldKingdomName,@NotNull KonKingdom conquerKingdom) {
		if(!conquerKingdom.isMonumentTemplateValid()) {
			ChatUtil.printDebug("Failed to capture town for kingdom with invalid template: "+conquerKingdom.getName());
			return null;
		}
		boolean captureUpgrades = konquest.getCore().getBoolean(CorePath.TOWNS_CAPTURE_UPGRADES.getPath(),true);
		if(isKingdom(oldKingdomName) && getKingdom(oldKingdomName).hasTown(name)) {
			konquest.getMapHandler().drawDynmapRemoveTerritory(getKingdom(oldKingdomName).getTown(name));
			getKingdom(oldKingdomName).getTown(name).purgeResidents();
			getKingdom(oldKingdomName).getTown(name).clearShieldsArmors();
			getKingdom(oldKingdomName).getTown(name).setKingdom(conquerKingdom);
			conquerKingdom.addTownConquer(name, getKingdom(oldKingdomName).removeTownConquer(name));
			conquerKingdom.getTown(name).refreshMonument();
			refreshTownNerfs(conquerKingdom.getTown(name));
			refreshTownHearts(conquerKingdom.getTown(name));
			conquerKingdom.getTown(name).updateBarPlayers();
			if(captureUpgrades) {
				konquest.getUpgradeManager().updateTownDisabledUpgrades(conquerKingdom.getTown(name));
			} else {
				conquerKingdom.getTown(name).clearUpgrades();
			}
			conquerKingdom.getTown(name).clearPlots();
			konquest.getMapHandler().drawDynmapUpdateTerritory(conquerKingdom.getTown(name));
			konquest.getMapHandler().drawDynmapLabel(getKingdom(oldKingdomName).getCapital());
			konquest.getMapHandler().drawDynmapLabel(conquerKingdom.getCapital());
			konquest.getIntegrationManager().getQuickShop().deleteShopsInPoints(conquerKingdom.getTown(name).getChunkList().keySet(),conquerKingdom.getTown(name).getWorld());
			return conquerKingdom.getTown(name);
		}
		return null;
	}
	
	/**
	 * Primary method for transferring ownership of a capital between kingdoms
	 * @param oldKingdomName - Kingdom name of conquered Town
	 * @param conquerPlayerArg - Player who initiated the conquering
	 * @return KonquestTown if all names exist, else null
	 */
	@Nullable
	public KonquestTown captureCapitalForPlayer(String oldKingdomName,@Nullable KonquestPlayer conquerPlayerArg) {
		if(conquerPlayerArg == null) {
			return null;
		}
		if(!(conquerPlayerArg instanceof KonPlayer)) {
			return null;
		}
		KonPlayer conquerPlayer = (KonPlayer)conquerPlayerArg;
		
		if(conquerPlayer.isBarbarian()) {
			return null;
		}
		KonTown town = captureCapital(oldKingdomName, conquerPlayer.getKingdom());
		if(town != null) {
			town.setPlayerLord(conquerPlayer.getOfflineBukkitPlayer());
			return town;
		}
		return null;
	}

	@Nullable
	public KonTown captureCapital(String oldKingdomName, @NotNull KonKingdom conquerKingdom) {
		if(!conquerKingdom.isMonumentTemplateValid()) {
			ChatUtil.printDebug("Failed to capture capital for kingdom with invalid template: "+conquerKingdom.getName());
			return null;
		}
		boolean captureUpgrades = konquest.getCore().getBoolean(CorePath.TOWNS_CAPTURE_UPGRADES.getPath(),true);
		if(isKingdom(oldKingdomName)) {
			// Capture the capital of the old kingdom as a new town for the conquering kingdom.
			Location townLoc = getKingdom(oldKingdomName).getCapital().getCenterLoc();
			String townKingdom = conquerKingdom.getName();
			Set<Point> townLand = getKingdom(oldKingdomName).getCapital().getChunkPoints();
			Map<KonPropertyFlag,Boolean> townProperties = getKingdom(oldKingdomName).getCapital().getAllProperties();
			Map<KonquestUpgrade,Integer> townUpgrades = getKingdom(oldKingdomName).getCapital().getUpgrades();
			// Remove the old kingdom
			removeKingdom(oldKingdomName);
			// Validate town name
			int nameStatus = konquest.validateNameConstraints(oldKingdomName);
			if(nameStatus != 0) {
				ChatUtil.printDebug("Failed to name captured capital to "+ oldKingdomName +", validation failed with status code "+nameStatus);
				return null;
			}
			// Create a new town
			int status = createTown(townLoc, oldKingdomName,townKingdom);
			if(status != 0) {
				// Successfully created new town, update it
				KonTown town = conquerKingdom.getTown(oldKingdomName);
				// Add land
				town.addPoints(townLand);
				// Update territory cache
				konquest.getTerritoryManager().addAllTerritory(town.getWorld(),town.getChunkList());
				// Add properties
            	for(KonPropertyFlag flag : townProperties.keySet()) {
            		town.setPropertyValue(flag, townProperties.get(flag));
            	}
            	// Add upgrades
            	if(captureUpgrades) {
	            	for(KonquestUpgrade upgrade : townUpgrades.keySet()) {
	            		town.addUpgrade(upgrade, townUpgrades.get(upgrade));
	            	}
	            	// Update upgrade status
	            	konquest.getUpgradeManager().updateTownDisabledUpgrades(town);
            	}
            	// Update display bar, nerfs, etc
            	refreshTownNerfs(town);
    			refreshTownHearts(town);
				town.updateBarPlayers();
				konquest.getMapHandler().drawDynmapUpdateTerritory(town);
				konquest.getMapHandler().drawDynmapLabel(conquerKingdom.getCapital());
				konquest.getIntegrationManager().getQuickShop().deleteShopsInPoints(town.getChunkList().keySet(),town.getWorld());
				return town;
			} else {
				ChatUtil.printDebug("Failed to create new town over captured capital, status code "+status);
				return null;
			}
		}
		return null;
	}

	/*
	 * =================================================
	 * Town Management Methods
	 * =================================================
	 */

	private void broadcastResidents(KonTown town, String message) {
		if(town == null)return;
		for(OfflinePlayer offlinePlayer : town.getPlayerResidents()) {
			if(offlinePlayer.isOnline()) {
				Player player = (Player)offlinePlayer;
				ChatUtil.sendNotice(player, message);
			}
		}
	}

	private void broadcastKnights(KonTown town, String message) {
		if(town == null) return;
		for(OfflinePlayer offlinePlayer : town.getPlayerKnights()) {
			if(offlinePlayer.isOnline()) {
				Player player = (Player)offlinePlayer;
				ChatUtil.sendNotice(player, message);
			}
		}
	}

	// A player requests to join a town from the menu
	public boolean menuJoinTownRequest(KonPlayer player, KonTown town) {
		Player bukkitPlayer = player.getBukkitPlayer();
		if(player.isBarbarian()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
			return false;
		}
		if(town.isPlayerResident(bukkitPlayer)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_JOIN_MEMBER.getMessage());
			return false;
		}
		UUID myId = bukkitPlayer.getUniqueId();
		if(town.isOpen() || town.isJoinInviteValid(myId)) {
			// There is an existing invite to join, or the town is open, add the player as a resident
			town.removeJoinRequest(myId);
			// Add the player as a resident
			if(town.addPlayerResident(bukkitPlayer,false)) {
				for(OfflinePlayer resident : town.getPlayerResidents()) {
					if(resident.isOnline()) {
						ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_NEW_RESIDENT.getMessage(bukkitPlayer.getName(),town.getName()));
					}
				}
				updatePlayerMembershipStats(player);
			}
		} else {
			if(town.isJoinRequestValid(myId)) {
				// There is already an existing join request
				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_REQUEST_EXISTS.getMessage(town.getName()));
				return false;
			}
			if(town.getNumResidents() == 0) {
				// The town is empty, make the player into the lord
				town.setPlayerLord(bukkitPlayer);
				town.removeJoinRequest(myId);
				updatePlayerMembershipStats(player);
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_NEW_RESIDENT.getMessage(bukkitPlayer.getName(),town.getName()));
			} else {
				// Create a new join request
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_REQUEST_SENT.getMessage(town.getName()));
				town.addJoinRequest(myId, false);
				broadcastKnights(town, MessagePath.COMMAND_TOWN_NOTICE_REQUEST_RECEIVED.getMessage());
			}
		}
		return true;
	}

	// A player leaves a town from the menu
	public boolean menuLeaveTown(KonPlayer player, KonTown town) {
		Player bukkitPlayer = player.getBukkitPlayer();
		if(player.isBarbarian()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
			return false;
		}
		if(!town.isPlayerResident(bukkitPlayer)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_FAILED.getMessage());
			return false;
		}
		// Remove the player as a resident
		if(town.removePlayerResident(bukkitPlayer)) {
			for(OfflinePlayer resident : town.getPlayerResidents()) {
				if(resident.isOnline()) {
					ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_LEAVE_RESIDENT.getMessage(bukkitPlayer.getName(),town.getName()));
				}
			}
			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_LEAVE_RESIDENT.getMessage(bukkitPlayer.getName(),town.getName()));
			updatePlayerMembershipStats(player);
		} else {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_FAILED.getMessage());
			return false;
		}
		return true;
	}

	// A player responds to an invite to join a town from the menu
	public boolean menuRespondTownInvite(KonPlayer player, KonTown town, boolean resp) {
		Player bukkitPlayer = player.getBukkitPlayer();
		if(player.isBarbarian()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_DENY_BARBARIAN.getMessage());
			return false;
		}
		if(town.isPlayerResident(bukkitPlayer)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_JOIN_MEMBER.getMessage(town.getName()));
			return false;
		}
		UUID myId = bukkitPlayer.getUniqueId();
		if(!town.isJoinInviteValid(myId)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_FAILED.getMessage());
			return false;
		}
		town.removeJoinRequest(myId);
		if(resp) {
			// Accept the invite
			if(town.addPlayerResident(bukkitPlayer,false)) {
				for(OfflinePlayer resident : town.getPlayerResidents()) {
					if(resident.isOnline()) {
						ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_NEW_RESIDENT.getMessage(bukkitPlayer.getName(),town.getName()));
					}
				}
				updatePlayerMembershipStats(player);
			}
		} else {
			// Decline the invite
			ChatUtil.sendNotice(bukkitPlayer,MessagePath.COMMAND_TOWN_NOTICE_INVITE_DECLINED.getMessage(town.getName()));
		}
		return true;
	}

	// A town lord/knight responds to a join request by the given player
	public boolean menuRespondTownRequest(KonPlayer sender, OfflinePlayer requester, KonTown town, boolean resp) {
		Player bukkitPlayer = sender.getBukkitPlayer();
		UUID id = requester.getUniqueId();
		if(!town.isPlayerLord(bukkitPlayer) && !town.isPlayerKnight(bukkitPlayer)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}
		KonOfflinePlayer offlinePlayer = konquest.getPlayerManager().getOfflinePlayer(requester);
		if(offlinePlayer == null) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(requester.getName()));
			return false;
		}
		if(!offlinePlayer.getKingdom().equals(town.getKingdom())) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
			return false;
		}
		if(town.isPlayerResident(requester)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_INVITE_MEMBER.getMessage(requester.getName()));
			return false;
		}
		if(!town.isJoinRequestValid(id)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_FAILED.getMessage());
			return false;
		}
		KonPlayer onlinePlayer = konquest.getPlayerManager().getPlayerFromID(id);
		town.removeJoinRequest(id);
		if(resp) {
			// Accepted the request
			if(town.addPlayerResident(requester,false)) {
				for(OfflinePlayer resident : town.getPlayerResidents()) {
					if(resident.isOnline()) {
						ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_NEW_RESIDENT.getMessage(requester.getName(),town.getName()));
					}
				}
				if(onlinePlayer != null) {
					updatePlayerMembershipStats(onlinePlayer);
				}
			}
		} else {
			// Denied the request
			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_REQUEST_DECLINED.getMessage(requester.getName()));
			if(onlinePlayer != null) {
				ChatUtil.sendError(onlinePlayer.getBukkitPlayer(), MessagePath.COMMAND_TOWN_ERROR_REQUEST_DENY.getMessage(town.getName()));
			}
		}
		return true;
	}

	// A town lord/knight invites (offline) player to join (with /town add command)
	public void addTownPlayer(KonPlayer sender, KonOfflinePlayer player, KonTown town) {
		Player bukkitPlayer = sender.getBukkitPlayer();
		if(!town.isPlayerLord(bukkitPlayer) && !town.isPlayerKnight(bukkitPlayer)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return;
		}
		if(player == null) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return;
		}
		OfflinePlayer offlinePlayer = player.getOfflineBukkitPlayer();
		UUID id = offlinePlayer.getUniqueId();
		if(!player.getKingdom().equals(town.getKingdom())) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
			return;
		}
		// Cannot invite a resident of this town
		if(town.isPlayerResident(offlinePlayer)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_INVITE_MEMBER.getMessage(offlinePlayer.getName()));
			return;
		}
		// Cannot invite to an open town
		if(town.isOpen()) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_INVITE_OPEN.getMessage());
			return;
		}
		KonPlayer onlinePlayer = konquest.getPlayerManager().getPlayerFromID(id);

		// Check for join request and add as resident
		// Otherwise create join invite
		if(town.isJoinInviteValid(id)) {
			// There is already a join invite for this player
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_INVITE_EXISTS.getMessage(offlinePlayer.getName()));
		} else {
			// Check for join request
			if(town.isJoinRequestValid(id)) {
				// This player has already requested to join, add them
				town.removeJoinRequest(id);
				// Add the player as a resident
				if(town.addPlayerResident(offlinePlayer,false)) {
					for(OfflinePlayer resident : town.getPlayerResidents()) {
						if(resident.isOnline()) {
							ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_NEW_RESIDENT.getMessage(offlinePlayer.getName(),town.getName()));
						}
					}
					if(onlinePlayer != null) {
						updatePlayerMembershipStats(onlinePlayer);
					}
				}
			} else {
				// Create a new join invite
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_INVITE_SENT.getMessage(offlinePlayer.getName(),town.getName()));
				town.addJoinRequest(id, true);
				if(onlinePlayer != null) {
					ChatUtil.sendNotice(onlinePlayer.getBukkitPlayer(), MessagePath.COMMAND_TOWN_NOTICE_INVITE_RECEIVED.getMessage());
				}
			}
		}
	}

	// A town lord/knight kicks (offline) player (with /town kick command)
	public void kickTownPlayer(KonPlayer sender, KonOfflinePlayer player, KonTown town) {
		Player bukkitPlayer = sender.getBukkitPlayer();
		if(!town.isPlayerLord(bukkitPlayer) && !town.isPlayerKnight(bukkitPlayer)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return;
		}
		if(player == null) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			return;
		}
		OfflinePlayer offlinePlayer = player.getOfflineBukkitPlayer();
		// Check for existing resident
		if(!town.isPlayerResident(offlinePlayer)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_KICK_FAIL.getMessage(offlinePlayer.getName(),town.getName()));
			return;
		}
		// Prevent kicking the town lord
		if(town.isPlayerLord(player.getOfflineBukkitPlayer())) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return;
		}
		UUID id = offlinePlayer.getUniqueId();
		KonPlayer onlinePlayer = konquest.getPlayerManager().getPlayerFromID(offlinePlayer.getUniqueId());
		// Attempt to kick current resident
		if(town.removePlayerResident(offlinePlayer)) {
			town.removeJoinRequest(id);
			for(OfflinePlayer resident : town.getPlayerResidents()) {
				if(resident.isOnline()) {
					ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_KICK_RESIDENT.getMessage(offlinePlayer.getName(),town.getName()));
				}
			}
			if(onlinePlayer != null) {
				ChatUtil.sendError(onlinePlayer.getBukkitPlayer(), MessagePath.COMMAND_TOWN_ERROR_KICKED.getMessage(town.getName()));
				updatePlayerMembershipStats(onlinePlayer);
			}
		} else {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_KICK_FAIL.getMessage(offlinePlayer.getName(),town.getName()));
		}
	}

	// A town lord/knight promotes or demotes a resident
	public boolean menuPromoteDemoteTownKnight(KonPlayer sender, OfflinePlayer member, KonTown town, boolean action) {
		Player bukkitPlayer = sender.getBukkitPlayer();
		UUID id = member.getUniqueId();
		if(!town.isPlayerLord(bukkitPlayer)) {
			// Only town lord can promote & demote
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}
		KonOfflinePlayer offlinePlayer = konquest.getPlayerManager().getOfflinePlayer(member);
		if(offlinePlayer == null) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(member.getName()));
			return false;
		}
		if(!offlinePlayer.getKingdom().equals(town.getKingdom())) {
			// Target player must be a kingdom member
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
			return false;
		}
		if(id.equals(bukkitPlayer.getUniqueId()) || town.isLord(id)) {
			// Target player cannot be the town lord, or the sender
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}
		if(!town.isPlayerResident(member)) {
			// Target player must be a town resident
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_KNIGHT_RESIDENT.getMessage());
			return false;
		}
		// Set resident's elite status
		KonPlayer onlinePlayer = konquest.getPlayerManager().getPlayerFromID(id);
		if(action) {
			// Action is true, try to promote
			if(town.isPlayerKnight(member)) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_PROMOTE_KNIGHT.getMessage(member.getName()));
				return false;
			}
			town.setPlayerKnight(member, true);
			for(OfflinePlayer resident : town.getPlayerResidents()) {
				if(resident.isOnline()) {
					ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_KNIGHT_SET.getMessage(member.getName(),town.getName()));
				}
			}
		} else {
			// Action is false, try to demote
			if(!town.isPlayerKnight(member)) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_TOWN_ERROR_DEMOTE_RESIDENT.getMessage(member.getName()));
				return false;
			}
			town.setPlayerKnight(offlinePlayer.getOfflineBukkitPlayer(), false);
			for(OfflinePlayer resident : town.getPlayerResidents()) {
				if(resident.isOnline()) {
					ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_KNIGHT_CLEAR.getMessage(member.getName(),town.getName()));
				}
			}
		}
		if(onlinePlayer != null) {
			updatePlayerMembershipStats(onlinePlayer);
		}
		return true;
	}

	// A town lord or admin is transferring town lordship to another member
	public boolean menuTransferTownLord(KonPlayer sender, OfflinePlayer member, KonTown town, boolean isAdmin) {
		// In this method, the sender must be the current town lord, or there is no valid lord, or an admin is transferring.
		// The member player will be the new town lord.
		Player bukkitPlayer = sender.getBukkitPlayer();
		UUID id = member.getUniqueId();
		if(town.isLordValid()) {
			// Lord exists, only permit access to owner
			if(!isAdmin && !town.isPlayerLord(bukkitPlayer)) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
				return false;
			}
		}
		KonOfflinePlayer offlinePlayer = konquest.getPlayerManager().getOfflinePlayer(member);
		if(offlinePlayer == null) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_UNKNOWN_NAME.getMessage(member.getName()));
			return false;
		}
		// Check for enemy new lord
		if(!offlinePlayer.getKingdom().equals(town.getKingdom())) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_ENEMY_PLAYER.getMessage());
			return false;
		}
		// Check for member resident
		if(!town.isPlayerResident(member)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}
		// Check for self new lord
		if(id.equals(bukkitPlayer.getUniqueId())) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}
		// Check for kingdom capital, cannot transfer lord (lord must be kingdom master)
		if(town.getTerritoryType().equals(KonquestTerritoryType.CAPITAL)) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return false;
		}
		// Transfer lordship
		town.setPlayerLord(member);
		for(OfflinePlayer resident : town.getPlayerResidents()) {
			if(resident.isOnline()) {
				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_LORD_SUCCESS.getMessage(town.getName(),member.getName()));
			}
		}
		KonPlayer onlinePlayer = konquest.getPlayerManager().getPlayerFromID(id);
		if(onlinePlayer != null) {
			updatePlayerMembershipStats(onlinePlayer);
		}
		return true;
	}

	// An online player tries to become lord of an abandoned town
	public void lordTownTakeover(KonPlayer player, KonTown town) {
		Player bukkitPlayer = player.getBukkitPlayer();
		// Verify conditions for takeover
		if(town.isLordValid()) {
			// Lord exists, cannot take over town
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
			return;
		}
		// Lord does not exist
		if(!town.isOpen()) {
			// Check player roles in closed towns only, allow any member to claim lordship in open towns
			if(!town.getPlayerKnights().isEmpty()) {
				// Elite residents exist, permit access to them
				if(!town.isPlayerKnight(bukkitPlayer)) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
					return;
				}
			} else {
				// No Elite residents exist
				if(!town.getPlayerResidents().isEmpty()) {
					// Residents for this town exist, permit access to them
					if(!town.isPlayerResident(bukkitPlayer)) {
						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_ALLOW.getMessage());
						return;
					}
				} else {
					// No residents exist, permit access to any Kingdom member
					if(!town.getKingdom().equals(player.getKingdom())) {
						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_ENEMY_TOWN.getMessage());
						return;
					}
				}
			}
		}

		// At this point, the player may take over the town as lord
		town.setPlayerLord(bukkitPlayer);
		town.removeJoinRequest(bukkitPlayer.getUniqueId());
		for(OfflinePlayer resident : town.getPlayerResidents()) {
			if(resident.isOnline()) {
				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_LORD_CLAIM.getMessage(bukkitPlayer.getName(),town.getName()));
			}
		}
		updatePlayerMembershipStats(player);
	}

	/*
	 * More methods... (a lot more)
	 */
	
	public String getSmallestKingdomName() {
		String smallestName = "";
		for(KonKingdom kingdom : kingdomMap.values()) {
			if(kingdom.isSmallest()) {
				smallestName = kingdom.getName();
			}
		}
		return smallestName;
	}
	
	public void updateSmallestKingdom() {
		KonKingdom smallestKingdom = null;
		int smallestSize = Integer.MAX_VALUE;
		for(KonKingdom kingdom : kingdomMap.values()) {
			kingdom.setSmallest(false);
			int size = konquest.getPlayerManager().getAllPlayersInKingdom(kingdom.getName()).size();
			if(size < smallestSize) {
				smallestKingdom = kingdom;
				smallestSize = size;
			}
		}
		if(smallestKingdom == null) {
			ChatUtil.printDebug("Failed to update smallest kingdom, got null!");
			return;
		}
		smallestKingdom.setSmallest(true);
		ChatUtil.printDebug("Updated smallest kingdom: "+smallestKingdom.getName());
	}
	
	// This can return null!
	public KonTerritory getClosestEnemyTown(Location loc, KonKingdom friendlyKingdom) {
		KonTerritory closestTerritory = null;
		int minDistance = Integer.MAX_VALUE;
		for(KonKingdom kingdom : kingdomMap.values()) {
			if(!kingdom.equals(friendlyKingdom)) {
				for(KonTown town : kingdom.getTowns()) {
					int townDist = Konquest.chunkDistance(loc, town.getCenterLoc());
					if(townDist != -1 && townDist < minDistance) {
						minDistance = townDist;
						closestTerritory = town;
					}
				}
			}
		}
		return closestTerritory;
	}
	
	public ArrayList<String> getKingdomNames() {
		return new ArrayList<>(kingdomMap.keySet());
	}
	
	public ArrayList<KonKingdom> getKingdoms() {
		return new ArrayList<>(kingdomMap.values());
	}
	
	public KonKingdom getKingdom(String name) {
		if(kingdomMap.containsKey(name)) {
			return kingdomMap.get(name);
		} else if(name.equalsIgnoreCase("barbarians")){
			return barbarians;
		} else {
			// Check for case-insensitive name
			for(String kingdomName : kingdomMap.keySet()) {
				if(name.equalsIgnoreCase(kingdomName)) {
					return kingdomMap.get(kingdomName);
				}
			}
		}
		// Could not find a kingdom by name, return barbarians
		ChatUtil.printDebug("KingdomManager couldn't get Kingdom! "+name);
		return barbarians;
	}
	
	public boolean isKingdom(String name) {
		// Check for exact String key match
		if(kingdomMap.containsKey(name)) {
			return true;
		} else {
			// Check for case-insensitive name
			for(String kingdomName : kingdomMap.keySet()) {
				if(name.equalsIgnoreCase(kingdomName)) {
					return true;
				}
			}
		}
		// Could not find a kingdom by name
		return false;
	}

	@NotNull
	public ArrayList<String> getTownNames() {
		ArrayList<String> names = new ArrayList<>();
		for(KonKingdom kingdom : kingdomMap.values()) {
			names.addAll(kingdom.getTownNames());
		}
		return names;
	}

	@Nullable
	public KonTown getTown(String name) {
		KonTown result = null;
		for(KonKingdom kingdom : kingdomMap.values()) {
			if(kingdom.hasTown(name)) {
				result = kingdom.getTown(name);
			}
		}
		return result;
	}

	public boolean isTown(String name) {
		for(KonKingdom kingdom : kingdomMap.values()) {
			if(kingdom.hasTown(name)) {
				return true;
			}
		}
		return false;
	}

	@Nullable
	public KonCapital getCapital(String name) {
		KonCapital result = null;
		for(KonKingdom kingdom : kingdomMap.values()) {
			if(kingdom.hasCapital(name)) {
				result = kingdom.getCapital();
			}
		}
		return result;
	}
	
	public boolean isCapital(String name) {
		for(KonKingdom kingdom : kingdomMap.values()) {
			if(kingdom.hasCapital(name)) {
				return true;
			}
		}
		return false;
	}
	
	public KonKingdom getBarbarians() {
		return barbarians;
	}
	
	public KonKingdom getNeutrals() {
		return neutrals;
	}
	
	public void removeAllRabbits() {
		for(KonKingdom kingdom : getKingdoms()) {
			for(KonTown town : kingdom.getCapitalTowns()) {
				town.removeRabbit();
			}
		}
	}
	
	public void reloadMonumentsForTemplate(KonMonumentTemplate template) {
		for(KonKingdom kingdom : getKingdoms()) {
			if(kingdom.isMonumentTemplateValid() && kingdom.getMonumentTemplate().equals(template)) {
				kingdom.reloadLoadedTownMonuments();
			}
		}
	}
	
	/**
	 * Updates a town option based on action
	 * @param action - The action to perform
	 * @param town - The town to update
	 * @param bukkitPlayer - the player who is performing the action
	 * @return true if action was successful
	 */
	public boolean changeTownOption(optionAction action, KonTown town, Player bukkitPlayer) {
		boolean result = false;
		switch(action) {
			case TOWN_OPEN:
				if(town.isOpen()) {
					// Close the town
            		town.setIsOpen(false);
            		for(OfflinePlayer resident : town.getPlayerResidents()) {
		    			if(resident.isOnline()) {
		    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_CLOSE.getMessage(town.getName()));
		    			}
		    		}
				} else {
					// Open the town
					town.setIsOpen(true);
            		for(OfflinePlayer resident : town.getPlayerResidents()) {
		    			if(resident.isOnline()) {
		    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_OPEN.getMessage(town.getName()));
		    			}
		    		}
				}
				result = true;
				break;
			case TOWN_PLOT_ONLY:
				if(town.isPlotOnly()) {
					// Disable plot only mode
					town.setIsPlotOnly(false);
					for(OfflinePlayer resident : town.getPlayerResidents()) {
		    			if(resident.isOnline()) {
		    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_PLOT_DISABLE.getMessage(town.getName()));
		    			}
		    		}
				} else {
					// Enable plot only mode
					town.setIsPlotOnly(true);
					for(OfflinePlayer resident : town.getPlayerResidents()) {
		    			if(resident.isOnline()) {
		    				ChatUtil.sendNotice((Player) resident, MessagePath.COMMAND_TOWN_NOTICE_PLOT_ENABLE.getMessage(town.getName()));
		    			}
		    		}
				}
				result = true;
				break;
			case TOWN_REDSTONE:
				if(town.isEnemyRedstoneAllowed()) {
					// Disable enemy redstone
            		town.setIsEnemyRedstoneAllowed(false);
            		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_REDSTONE_DISABLE.getMessage(town.getName()));
				} else {
					// Enable enemy redstone
					town.setIsEnemyRedstoneAllowed(true);
					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_REDSTONE_ENABLE.getMessage(town.getName()));
				}
				result = true;
				break;
			case TOWN_GOLEM:
				if(town.isGolemOffensive()) {
					// Disable golem offense
            		town.setIsGolemOffensive(false);
            		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_GOLEM_DISABLE.getMessage(town.getName()));
				} else {
					// Enable golem offense
					town.setIsGolemOffensive(true);
					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_GOLEM_ENABLE.getMessage(town.getName()));
				}
				result = true;
				break;
			default:
				break;
		}
		return result;
	}
	
	private void makeTownNerfs() {
		townNerfs.put(PotionEffectType.SLOW_DIGGING, 0);
	}
	
	public void applyTownNerf(KonPlayer player, KonTown town) {
		if(!player.isAdminBypassActive()) {
			for(PotionEffectType pot : townNerfs.keySet()) {
				int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.FATIGUE);
				int upgradedPotLevel = townNerfs.get(pot) + upgradeLevel;
				player.getBukkitPlayer().addPotionEffect(new PotionEffect(pot, 9999999, upgradedPotLevel));
			}
		}
	}
	
	public void clearTownNerf(KonPlayer player) {
		for(PotionEffectType pot : townNerfs.keySet()) {
			player.getBukkitPlayer().removePotionEffect(pot);
		}
	}
	
	public void clearAllTownNerfs(KonTown town) {
		for(KonPlayer player : konquest.getPlayerManager().getPlayersOnline()) {
			if(town.isLocInside(player.getBukkitPlayer().getLocation())) {
				clearTownNerf(player);
			}
		}
	}
	
	public void refreshTownNerfs(KonTown town) {
		for(KonPlayer player : konquest.getPlayerManager().getPlayersOnline()) {
			if(town.isLocInside(player.getBukkitPlayer().getLocation())) {
				if(player.getKingdom().equals(town.getKingdom())) {
					clearTownNerf(player);
				} else {
					applyTownNerf(player, town);
				}
			}
		}
	}

	@NotNull
	public Set<PotionEffectType> getTownNerfs() {
		return townNerfs.keySet();
	}
	
	public boolean isTownNerf(PotionEffectType pot) {
		return townNerfs.containsKey(pot);
	}
	
	public void applyTownHearts(KonPlayer player, KonTown town) {
		if(player.getKingdom().equals(town.getKingdom())) {
			int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonUpgrade.HEALTH);
			if(upgradeLevel >= 1) {
				//double upgradedBaseHealth = 20 + (upgradeLevel*2);
				//player.getBukkitPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(upgradedBaseHealth);
				double modifier = (upgradeLevel*2);
				String modName = Konquest.healthModName;
				// Check for existing modifier
				boolean isModActive = false;
				AttributeInstance playerHealthAtt = player.getBukkitPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH);
				for(AttributeModifier mod : playerHealthAtt.getModifiers()) {
					if(mod.getName().equals(modName)) {
						isModActive = true;
						break;
					}
				}
				// Apply modifier
				double baseHealth = playerHealthAtt.getBaseValue();
				if(!isModActive) {
					playerHealthAtt.addModifier(new AttributeModifier(modName,modifier,AttributeModifier.Operation.ADD_NUMBER));
					ChatUtil.printDebug("Applied max health attribute modifier "+modifier+" to player "+player.getBukkitPlayer().getName()+" with base health "+baseHealth);
				} else {
					ChatUtil.printDebug("Could not apply max health attribute modifier to player "+player.getBukkitPlayer().getName()+" with base health "+baseHealth);
				}
			} else {
				clearTownHearts(player);
			}
		}
	}
	
	public void clearTownHearts(KonPlayer player) {
		String modName = Konquest.healthModName;
		// Search for modifier and remove
		AttributeInstance playerHealthAtt = player.getBukkitPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH);
		for(AttributeModifier mod : playerHealthAtt.getModifiers()) {
			if(mod.getName().equals(modName)) {
				playerHealthAtt.removeModifier(mod);
				ChatUtil.printDebug("Removed max health attribute modifier for player "+player.getBukkitPlayer().getName());
			}
		}
	}
	
	public void clearAllTownHearts(KonTown town) {
		for(KonPlayer player : konquest.getPlayerManager().getPlayersOnline()) {
			if(town.isLocInside(player.getBukkitPlayer().getLocation())) {
				clearTownHearts(player);
			}
		}
	}
	
	public void refreshTownHearts(KonTown town) {
		for(KonPlayer player : konquest.getPlayerManager().getPlayersOnline()) {
			if(town.isLocInside(player.getBukkitPlayer().getLocation())) {
				clearTownHearts(player);
				if(player.getKingdom().equals(town.getKingdom())) {
					applyTownHearts(player, town);
				}
			}
		}
	}

	public void updateKingdomOfflineProtection() {
		boolean isOfflineProtectedEnabled = konquest.getCore().getBoolean(CorePath.KINGDOMS_NO_ENEMY_EDIT_OFFLINE.getPath(),true);
		int offlineProtectedWarmupSeconds = konquest.getCore().getInt(CorePath.KINGDOMS_NO_ENEMY_EDIT_OFFLINE_WARMUP.getPath(),0);
		int offlineProtectedMinimumPlayers = konquest.getCore().getInt(CorePath.KINGDOMS_NO_ENEMY_EDIT_OFFLINE_MINIMUM.getPath(),0);
		if(isOfflineProtectedEnabled) {
			// For each kingdom, start protection warmup timer if no players are online, else ensure the timer is stopped
			for(KonKingdom kingdom : getKingdoms()) {
				ArrayList<KonPlayer> onlineKingdomPlayers = konquest.getPlayerManager().getPlayersInKingdom(kingdom.getName());
				int numOnlineKingdomPlayers = onlineKingdomPlayers.size();
				Timer protectedWarmupTimer = kingdom.getProtectedWarmupTimer();
				if((offlineProtectedMinimumPlayers <= 0 && onlineKingdomPlayers.isEmpty()) || (numOnlineKingdomPlayers < offlineProtectedMinimumPlayers)) {
					if(offlineProtectedWarmupSeconds > 0 && protectedWarmupTimer.getTime() == -1 && !kingdom.isOfflineProtected()) {
						// start timer
						ChatUtil.printDebug("Starting kingdom protection warmup timer for "+offlineProtectedWarmupSeconds+" seconds: "+kingdom.getName());
						protectedWarmupTimer.stopTimer();
						protectedWarmupTimer.setTime(offlineProtectedWarmupSeconds);
						protectedWarmupTimer.startTimer();
						int warmupMinutes = offlineProtectedWarmupSeconds / 60;
						int warmupSeconds = offlineProtectedWarmupSeconds % 60;
						String warmupTime = String.format("%d:%02d", warmupMinutes , warmupSeconds);
						ChatUtil.sendBroadcast(ChatColor.LIGHT_PURPLE+MessagePath.PROTECTION_NOTICE_KINGDOM_WARMUP.getMessage(kingdom.getName(),warmupTime));
					}
				} else {
					// stop timer, clear protection
					kingdom.setOfflineProtected(false);
					protectedWarmupTimer.stopTimer();
				}
			}
		}
	}
	
	public int getPlayerLordships(KonOfflinePlayer player) {
		int count = 0;
		for(KonTown town : player.getKingdom().getCapitalTowns()) {
			if(town.isPlayerLord(player.getOfflineBukkitPlayer())) {
				count = count + 1;
			}
		}
		return count;
	}

	public int getPlayerResidencies(KonOfflinePlayer player) {
		int count = 0;
		for(KonTown town : player.getKingdom().getCapitalTowns()) {
			if(town.isPlayerResident(player.getOfflineBukkitPlayer())) {
				count++;
			}
		}
		return count;
	}
	
	public List<KonTown> getPlayerLordshipTowns(KonquestOfflinePlayer player) {
		List<KonTown> towns = new ArrayList<>();
		if(player.getKingdom() instanceof KonKingdom) {
			KonKingdom kingdom = (KonKingdom)player.getKingdom();
			for(KonTown town : kingdom.getCapitalTowns()) {
				if(town.isPlayerLord(player.getOfflineBukkitPlayer())) {
					towns.add(town);
				}
			}
		}
		return towns;
	}

	@NotNull
	public List<KonTown> getPlayerKnightTowns(KonquestOfflinePlayer player) {
		List<KonTown> towns = new ArrayList<>();
		if(player.getKingdom() instanceof KonKingdom) {
			KonKingdom kingdom = (KonKingdom)player.getKingdom();
			for(KonTown town : kingdom.getCapitalTowns()) {
				if(town.isPlayerKnight(player.getOfflineBukkitPlayer()) && !town.isPlayerLord(player.getOfflineBukkitPlayer())) {
					towns.add(town);
				}
			}
		}
		return towns;
	}

	@NotNull
	public List<KonTown> getPlayerResidenceTowns(KonquestOfflinePlayer player) {
		List<KonTown> towns = new ArrayList<>();
		if(player.getKingdom() instanceof KonKingdom) {
			KonKingdom kingdom = (KonKingdom)player.getKingdom();
			for(KonTown town : kingdom.getCapitalTowns()) {
				if(town.isPlayerResident(player.getOfflineBukkitPlayer())) {
					towns.add(town);
				}
			}
		}
		return towns;
	}
	
	public void updatePlayerMembershipStats(KonPlayer player) {
		int countLord = 0;
		int countElite = 0;
		int countResident = 0;
		for(KonTown town : player.getKingdom().getCapitalTowns()) {
			if(town.isPlayerLord(player.getOfflineBukkitPlayer())) {
				countLord = countLord + 1;
			} else if(town.isPlayerKnight(player.getOfflineBukkitPlayer())) {
				countElite = countElite + 1;
			} else if(town.isPlayerResident(player.getOfflineBukkitPlayer())) {
				countResident = countResident + 1;
			}
		}
		player.getPlayerStats().setStat(KonStatsType.LORDS, countLord);
		player.getPlayerStats().setStat(KonStatsType.KNIGHTS, countElite);
		player.getPlayerStats().setStat(KonStatsType.RESIDENTS, countResident);
	}

	@NotNull
	public KonLeaderboard getKingdomLeaderboard(KonKingdom kingdom) {
		KonLeaderboard leaderboard = new KonLeaderboard();
		if(kingdom.equals(barbarians)) return leaderboard;
		// Determine scores for all players within towns
		HashMap<OfflinePlayer,KonPlayerScoreAttributes> memberScores = new HashMap<>();
		int numTownLords;
		int numTownKnights;
		int numTownResidents;
		int numLordLand;
		int numKnightLand;
		int numResidentLand;
		for(KonTown town : kingdom.getCapitalTowns()) {
			for(OfflinePlayer offlinePlayer : town.getPlayerResidents()) {
				numTownLords = 0;
				numTownKnights = 0;
				numTownResidents = 0;
				numLordLand = 0;
				numKnightLand = 0;
				numResidentLand = 0;
				if(town.isPlayerLord(offlinePlayer)) {
					numTownLords++;
					numLordLand += town.getChunkList().size();
				} else if(town.isPlayerKnight(offlinePlayer)) {
					numTownKnights++;
					numKnightLand += town.getChunkList().size();
				} else {
					numTownResidents++;
					numResidentLand += town.getChunkList().size();
				}
				if(memberScores.containsKey(offlinePlayer)) {
					memberScores.get(offlinePlayer).addAttribute(KonPlayerScoreAttribute.TOWN_LORDS, numTownLords);
					memberScores.get(offlinePlayer).addAttribute(KonPlayerScoreAttribute.TOWN_KNIGHTS, numTownKnights);
					memberScores.get(offlinePlayer).addAttribute(KonPlayerScoreAttribute.TOWN_RESIDENTS, numTownResidents);
					memberScores.get(offlinePlayer).addAttribute(KonPlayerScoreAttribute.LAND_LORDS, numLordLand);
					memberScores.get(offlinePlayer).addAttribute(KonPlayerScoreAttribute.LAND_KNIGHTS, numKnightLand);
					memberScores.get(offlinePlayer).addAttribute(KonPlayerScoreAttribute.LAND_RESIDENTS, numResidentLand);
				} else {
					KonOfflinePlayer validPlayer = konquest.getPlayerManager().getOfflinePlayer(offlinePlayer);
					if(validPlayer != null && validPlayer.getKingdom().equals(kingdom)) {
						KonPlayerScoreAttributes newMemberAttributes = new KonPlayerScoreAttributes();
						newMemberAttributes.setAttribute(KonPlayerScoreAttribute.TOWN_LORDS, numTownLords);
						newMemberAttributes.setAttribute(KonPlayerScoreAttribute.TOWN_KNIGHTS, numTownKnights);
						newMemberAttributes.setAttribute(KonPlayerScoreAttribute.TOWN_RESIDENTS, numTownResidents);
						newMemberAttributes.setAttribute(KonPlayerScoreAttribute.LAND_LORDS, numLordLand);
						newMemberAttributes.setAttribute(KonPlayerScoreAttribute.LAND_KNIGHTS, numKnightLand);
						newMemberAttributes.setAttribute(KonPlayerScoreAttribute.LAND_RESIDENTS, numResidentLand);
						memberScores.put(offlinePlayer, newMemberAttributes);
					}
				}
			}
		}
		// Find top scoring members
		final int NUM_TOP_PLAYERS = 9;
		int topScore = 0;
		OfflinePlayer topPlayer = null;
		for(int i = 0;i<NUM_TOP_PLAYERS;i++) {
			if(!memberScores.isEmpty()) {
				for(OfflinePlayer offlinePlayer : memberScores.keySet()) {
					int playerScore = memberScores.get(offlinePlayer).getScore();
					if(playerScore > topScore) {
						topScore = playerScore;
						topPlayer = offlinePlayer;
					}
				}
				if(topPlayer != null) {
					leaderboard.addEntry(topPlayer,topScore);
					//ChatUtil.printDebug("Found leaderboard member "+topPlayer.getName()+", score: "+topScore);
					memberScores.remove(topPlayer);
					topScore = 0;
					topPlayer = null;
				}
			}
		}
		return leaderboard;
	}

	@NotNull
	public KonKingdomScoreAttributes getKingdomScoreAttributes(KonKingdom kingdom) {
		KonKingdomScoreAttributes scoreAttributes = new KonKingdomScoreAttributes();
		//if(!kingdom.equals(barbarians) && !kingdom.isPeaceful()) {
		if(kingdom.isCreated() && !kingdom.isPeaceful()) {
			// Gather Kingdom metrics
			int numKingdomTowns = kingdom.getTowns().size();
	    	ArrayList<KonOfflinePlayer> allPlayersInKingdom = konquest.getPlayerManager().getAllPlayersInKingdom(kingdom.getName());
	    	int numAllKingdomPlayers = allPlayersInKingdom.size();
	    	int numKingdomLand = 0;
	    	for(KonTown town : kingdom.getCapitalTowns()) {
	    		numKingdomLand += town.getChunkList().size();
	    	}
	    	int numKingdomFavor = 0;
	    	for(KonOfflinePlayer kingdomPlayer : allPlayersInKingdom) {
	    		numKingdomFavor += (int) KonquestPlugin.getBalance(kingdomPlayer.getOfflineBukkitPlayer());
	    	}
	    	// Gather favor costs
	    	int cost_settle = (int)konquest.getCore().getDouble(CorePath.FAVOR_TOWNS_COST_SETTLE.getPath());
	    	int cost_claim = (int)konquest.getCore().getDouble(CorePath.FAVOR_COST_CLAIM.getPath());
	    	// Set attributes
	    	scoreAttributes.setAttributeWeight(KonKingdomScoreAttribute.TOWNS, cost_settle+2);
	    	scoreAttributes.setAttributeWeight(KonKingdomScoreAttribute.LAND, cost_claim+1);
	    	scoreAttributes.setAttribute(KonKingdomScoreAttribute.TOWNS, numKingdomTowns);
	    	scoreAttributes.setAttribute(KonKingdomScoreAttribute.LAND, numKingdomLand);
	    	scoreAttributes.setAttribute(KonKingdomScoreAttribute.FAVOR, numKingdomFavor);
	    	scoreAttributes.setAttribute(KonKingdomScoreAttribute.POPULATION, numAllKingdomPlayers);
		}
		return scoreAttributes;
	}
	
	public int getKingdomScore(KonKingdom kingdom) {
		return getKingdomScoreAttributes(kingdom).getScore();
	}

	@NotNull
	public KonPlayerScoreAttributes getPlayerScoreAttributes(KonOfflinePlayer offlinePlayer) {
		KonPlayerScoreAttributes scoreAttributes = new KonPlayerScoreAttributes();
		if(!offlinePlayer.isBarbarian() && !offlinePlayer.getKingdom().isPeaceful()) {
			// Gather player metrics
			int numTownLords = 0;
			int numTownKnights = 0;
			int numTownResidents = 0;
			int numLordLand = 0;
			int numKnightLand = 0;
			int numResidentLand = 0;
			for(KonTown town : offlinePlayer.getKingdom().getCapitalTowns()) {
				if(town.isPlayerLord(offlinePlayer.getOfflineBukkitPlayer())) {
					numTownLords++;
					numLordLand += town.getChunkList().size();
				} else if(town.isPlayerKnight(offlinePlayer.getOfflineBukkitPlayer())) {
					numTownKnights++;
					numKnightLand += town.getChunkList().size();
				} else if(town.isPlayerResident(offlinePlayer.getOfflineBukkitPlayer())) {
					numTownResidents++;
					numResidentLand += town.getChunkList().size();
				}
			}
			scoreAttributes.setAttribute(KonPlayerScoreAttribute.TOWN_LORDS, numTownLords);
			scoreAttributes.setAttribute(KonPlayerScoreAttribute.TOWN_KNIGHTS, numTownKnights);
			scoreAttributes.setAttribute(KonPlayerScoreAttribute.TOWN_RESIDENTS, numTownResidents);
			scoreAttributes.setAttribute(KonPlayerScoreAttribute.LAND_LORDS, numLordLand);
			scoreAttributes.setAttribute(KonPlayerScoreAttribute.LAND_KNIGHTS, numKnightLand);
			scoreAttributes.setAttribute(KonPlayerScoreAttribute.LAND_RESIDENTS, numResidentLand);
		}
		return scoreAttributes;
	}
	
	public int getPlayerScore(KonOfflinePlayer offlinePlayer) {
		return getPlayerScoreAttributes(offlinePlayer).getScore();
	}

	public void updateAllTownDisabledUpgrades() {
		for(KonKingdom kingdom : getKingdoms()) {
			for(KonTown town : kingdom.getTowns()) {
				konquest.getUpgradeManager().updateTownDisabledUpgrades(town);
			}
		}
	}
	
	// Returns true when the player is still in cool-down, else false
	public boolean isPlayerJoinCooldown(OfflinePlayer player) {
		// Check if player is currently in cool-down.
		// If cool-down has expired, remove it.
		UUID id = player.getUniqueId();
		if(!joinPlayerCooldowns.containsKey(id))return false;
		long endSeconds = joinPlayerCooldowns.get(id);
		Date now = new Date();
		if(now.after(new Date(endSeconds*1000))) {
			// The cool-down has expired
			joinPlayerCooldowns.remove(id);
			return false;
		}

		return true;
	}
	
	public void applyPlayerJoinCooldown(OfflinePlayer player) {
		if(joinCooldownSeconds <= 0)return;
		UUID id = player.getUniqueId();
		int endTimeSeconds = (int)((new Date()).getTime()/1000) + joinCooldownSeconds;
		joinPlayerCooldowns.put(id, endTimeSeconds);
	}
	
	public int getJoinCooldownRemainingSeconds(OfflinePlayer player) {
		UUID id = player.getUniqueId();
		if(!joinPlayerCooldowns.containsKey(id))return 0;
		int endSeconds = joinPlayerCooldowns.get(id);
		Date now = new Date();
		if(now.after(new Date((long)endSeconds*1000))){
			return 0;
		}
		return endSeconds - (int)(now.getTime()/1000);
	}

	public String getJoinCooldownRemainingTimeFormat(OfflinePlayer player, ChatColor color) {
		String remainCooldown = "";
		int cooldown = getJoinCooldownRemainingSeconds(player);
		if(cooldown > 0) {
			remainCooldown = Konquest.getTimeFormat(cooldown, color);
		}
		return remainCooldown;
	}
	
	// Returns true when the player is still in cool-down, else false
	public boolean isPlayerExileCooldown(OfflinePlayer player) {
		// Check if player is currently in cool-down.
		// If cool-down has expired, remove it.
		UUID id = player.getUniqueId();
		if(!exilePlayerCooldowns.containsKey(id))return false;
		long endSeconds = exilePlayerCooldowns.get(id);
		Date now = new Date();
		if(now.after(new Date(endSeconds*1000))) {
			// The cooldown has expired
			exilePlayerCooldowns.remove(id);
			return false;
		}
		// The player is still in cool-down
		return true;
	}
	
	public void applyPlayerExileCooldown(OfflinePlayer player) {
		if(exileCooldownSeconds <= 0)return;
		UUID id = player.getUniqueId();
		int endTimeSeconds = (int)((new Date()).getTime()/1000) + exileCooldownSeconds;
		exilePlayerCooldowns.put(id, endTimeSeconds);
	}
	
	public int getExileCooldownRemainingSeconds(OfflinePlayer player) {
		UUID id = player.getUniqueId();
		if(!exilePlayerCooldowns.containsKey(id))return 0;
		int endSeconds = exilePlayerCooldowns.get(id);
		Date now = new Date();
		if(now.after(new Date((long)endSeconds*1000))){
			return 0;
		}
		return endSeconds - (int)(now.getTime()/1000);
	}

	public String getExileCooldownRemainingTimeFormat(OfflinePlayer player, ChatColor color) {
		String remainCooldown = "";
		int cooldown = getExileCooldownRemainingSeconds(player);
		if(cooldown > 0) {
			remainCooldown = Konquest.getTimeFormat(cooldown, color);
		}
		return remainCooldown;
	}
	
	public void loadJoinExileCooldowns() {
		joinCooldownSeconds = konquest.getCore().getInt(CorePath.KINGDOMS_JOIN_COOLDOWN.getPath(),0);
		exileCooldownSeconds = konquest.getCore().getInt(CorePath.KINGDOMS_EXILE_COOLDOWN.getPath(),0);
		// Check any existing cool-downs, ensure less than config values.
		// Join cool-down map
		if(!joinPlayerCooldowns.isEmpty()) {
			HashMap<UUID,Integer> updateJoinMap = new HashMap<>();
			if(joinCooldownSeconds > 0) {
				int nowSeconds = (int)((new Date()).getTime()/1000);
				int maxEndSeconds = nowSeconds + joinCooldownSeconds;
				for(UUID u : joinPlayerCooldowns.keySet()) {
					int endSeconds = joinPlayerCooldowns.get(u);
					// Existing cool-down is longer than new config setting, shorten it.
					// Existing cool-down is under config, preserve it.
					updateJoinMap.put(u, Math.min(endSeconds, maxEndSeconds));
				}
			}
			joinPlayerCooldowns.clear();
			joinPlayerCooldowns.putAll(updateJoinMap);
		}
		// Exile cool-down map
		if(!exilePlayerCooldowns.isEmpty()) {
			HashMap<UUID,Integer> updateExileMap = new HashMap<>();
			if(exileCooldownSeconds > 0) {
				int nowSeconds = (int)((new Date()).getTime()/1000);
				int maxEndSeconds = nowSeconds + exileCooldownSeconds;
				for(UUID u : exilePlayerCooldowns.keySet()) {
					int endSeconds = exilePlayerCooldowns.get(u);
					// Existing cool-down is longer than new config setting, shorten it.
					// Existing cool-down is under config, preserve it.
					updateExileMap.put(u, Math.min(endSeconds, maxEndSeconds));
				}
			}
			exilePlayerCooldowns.clear();
			exilePlayerCooldowns.putAll(updateExileMap);
		}
	}
	
	public Material getTownCriticalBlock() {
		return townCriticalBlock;
	}
	
	public int getMaxCriticalHits() {
		return maxCriticalHits;
	}
	
	private void loadCriticalBlocks() {
		maxCriticalHits = konquest.getCore().getInt(CorePath.MONUMENTS_DESTROY_AMOUNT.getPath(),12);
		String townCriticalBlockTypeName = konquest.getCore().getString(CorePath.MONUMENTS_CRITICAL_BLOCK.getPath(),"");
		try {
			townCriticalBlock = Material.valueOf(townCriticalBlockTypeName);
		} catch(IllegalArgumentException e) {
			String message = "Invalid monument critical block \""+townCriticalBlockTypeName+"\" given in core.monuments.critical_block, using default OBSIDIAN";
    		ChatUtil.printConsoleError(message);
    		konquest.opStatusMessages.add(message);
		}
	}
	
	public boolean isArmorValid(Material mat) {
		boolean result = false;
		if(isArmorBlockWhitelist) {
			result = armorBlocks.contains(mat);
		} else {
			result = !armorBlocks.contains(mat);
		}
		return result;
	}
	
	public void loadArmorBlacklist() {
		armorBlocks.clear();
		// True = list is a whitelist, False = list is a blacklist (Default false)
		isArmorBlockWhitelist = konquest.getCore().getBoolean(CorePath.TOWNS_ARMOR_BLACKLIST_REVERSE.getPath(),false);
		boolean isListEnabled = konquest.getCore().getBoolean(CorePath.TOWNS_ARMOR_BLACKLIST_ENABLE.getPath(),false);
		if(isListEnabled) {
			List<String> armorList = konquest.getCore().getStringList(CorePath.TOWNS_ARMOR_BLACKLIST.getPath());
			Material mat;
			for(String entry : armorList) {
				mat = Material.matchMaterial(entry);
				if(mat != null) {
					armorBlocks.add(mat);
				}else{
					ChatUtil.printConsoleAlert("Invalid material \""+entry+"\" in "+CorePath.TOWNS_ARMOR_BLACKLIST.getPath()+", ignoring.");
				}
			}
		}
	}

	/**
	 * This method is meant to run after all kingdoms have been loaded from file
	 */
	private void assignDiplomacyTickets() {
		// Go through all tickets and set relations
		for(DiplomacyTicket ticket : diplomacyTickets) {
			// Get owner kingdom
			KonKingdom kingdom  = getKingdom(ticket.getOwnerKingdomName());
			if(!kingdom.isCreated()) {
				// Must be a created kingdom (not barbarian, neutral) to have relationships
				continue;
			}
			// Get other kingdom
			KonKingdom otherKingdom = getKingdom(ticket.getOtherKingdomName());
			if(!otherKingdom.isCreated()) {
				// Must be a created kingdom (not barbarian, neutral) to have relationships
				continue;
			}
			// Get diplomatic relation
			KonquestDiplomacyType relation = ticket.getRelation();
			if(relation == null) {
				continue;
			}
			// Check for active vs request
			if(ticket.isActive()) {
				// Relation is active
				kingdom.setActiveRelation(otherKingdom, relation);
			} else {
				// Relation is request
				kingdom.setRelationRequest(otherKingdom, relation);
			}
		}
		// Finished, clear tickets
		diplomacyTickets.clear();
	}
	
	private void loadKingdoms() {
		FileConfiguration kingdomsConfig = konquest.getConfigManager().getConfig("kingdoms");
        if (kingdomsConfig.get("kingdoms") == null) {
        	ChatUtil.printDebug("There is no kingdoms section in kingdoms.yml");
            return;
        }
        boolean isShieldsEnabled = konquest.getCore().getBoolean(CorePath.TOWNS_ENABLE_SHIELDS.getPath(),false);
        boolean isArmorsEnabled = konquest.getCore().getBoolean(CorePath.TOWNS_ENABLE_ARMOR.getPath(),false);
        double x,y,z;
        float pitch,yaw;
        List<Double> sectionList;
        String worldName;
        String defaultWorldName = konquest.getCore().getString(CorePath.WORLD_NAME.getPath(),"world");
		diplomacyTickets.clear();
        // Count all towns
        int numTowns = 0;
        for(String kingdomName : kingdomsConfig.getConfigurationSection("kingdoms").getKeys(false)) {
        	numTowns += kingdomsConfig.getConfigurationSection("kingdoms."+kingdomName+".towns").getKeys(false).size();
        }
        LoadingPrinter loadBar = new LoadingPrinter(numTowns,"Loading "+numTowns+" Towns");
        // Load all Kingdoms
        for(String kingdomName : kingdomsConfig.getConfigurationSection("kingdoms").getKeys(false)) {
        	//ChatUtil.printDebug("Loading Kingdom: "+kingdomName);
        	ConfigurationSection kingdomSection = kingdomsConfig.getConfigurationSection("kingdoms."+kingdomName);
        	// Check for center capital field
        	ConfigurationSection capitalSection = kingdomSection.getConfigurationSection("towns.capital");
        	if(capitalSection != null) {
        		worldName = capitalSection.getString("world",defaultWorldName);
        		World capitalWorld = Bukkit.getWorld(worldName);
        		if(capitalWorld != null) {
        			sectionList = capitalSection.getDoubleList("center");
            		x = sectionList.get(0);
            		y = sectionList.get(1);
            		z = sectionList.get(2);
    	        	Location capitalCenter = new Location(capitalWorld,x,y,z);
    	        	// Add new kingdom
    	        	kingdomMap.put(kingdomName, new KonKingdom(capitalCenter, kingdomName, konquest));
    	    		KonKingdom newKingdom = kingdomMap.get(kingdomName);
    	    		// Kingdom Operating Mode
    	    		boolean isAdmin = kingdomSection.getBoolean("admin", false);
    	    		newKingdom.setIsAdminOperated(isAdmin);
    	    		// Kingdom Template
    	        	String templateName = kingdomSection.getString("template","");
    	    		if(konquest.getSanctuaryManager().isTemplate(templateName)) {
    	    			KonMonumentTemplate template = konquest.getSanctuaryManager().getTemplate(templateName);
    	        		newKingdom.setMonumentTemplate(template);
    	        	} else {
    	        		ChatUtil.printConsoleError("Missing monument template \""+templateName+"\" for Kingdom "+kingdomName+" in loaded data file!");
    	        		konquest.opStatusMessages.add("Missing monument template for Kingdom "+kingdomName+"! Use \"/k admin kingdom\" to set a monument template for this Kingdom.");
    	        	}
    	    		// Kingdom Properties
        			ConfigurationSection kingdomPropertiesSection = kingdomSection.getConfigurationSection("properties");
        			for(String propertyName : kingdomPropertiesSection.getKeys(false)) {
        				boolean value = kingdomPropertiesSection.getBoolean(propertyName);
        				KonPropertyFlag property = KonPropertyFlag.getFlag(propertyName);
        				boolean status = newKingdom.setPropertyValue(property, value);
        				if(!status) {
        					ChatUtil.printDebug("Failed to set invalid property "+propertyName+" to Kingdom "+kingdomName);
        				}
        			}
					// Kingdom Membership
					// Assign Master
					String masterUUID = kingdomSection.getString("master","");
					if(!masterUUID.equalsIgnoreCase("")) {
						UUID playerID = Konquest.idFromString(masterUUID);
						if(playerID != null) {
							newKingdom.forceMaster(playerID);
						} else {
							ChatUtil.printDebug("Kingdom "+kingdomName+" has a null UUID! Master remains invalid");
						}
					} else {
						ChatUtil.printDebug("Kingdom "+kingdomName+" does not have a stored Master ID");
					}
					// Populate Members
					if(kingdomSection.contains("members")) {
						for(String residentUUID : kingdomSection.getConfigurationSection("members").getKeys(false)) {
							boolean isOfficer = kingdomSection.getBoolean("members."+residentUUID);
							newKingdom.addMember(UUID.fromString(residentUUID),isOfficer);
						}
					}
					// Add invite requests
					if(kingdomSection.contains("requests")) {
						for(String requestUUID : kingdomSection.getConfigurationSection("requests").getKeys(false)) {
							boolean type = kingdomSection.getBoolean("requests."+requestUUID);
							newKingdom.addJoinRequest(UUID.fromString(requestUUID), type);
						}
					}
					// Kingdom Diplomacy
					// Submit names and relationships to a queue for later assignment, once all kingdoms have been loaded
					if(kingdomSection.contains("diplomacy_active")) {
						for(String diplomacyKingdomName : kingdomSection.getConfigurationSection("diplomacy_active").getKeys(false)) {
							String relationName = kingdomSection.getConfigurationSection("diplomacy_active").getString(diplomacyKingdomName);
							KonquestDiplomacyType relation = KonquestDiplomacyType.getDefault();
							try {
								relation = KonquestDiplomacyType.valueOf(relationName);
							} catch(Exception e) {
								ChatUtil.printConsoleAlert("Failed to parse active diplomacy relation "+relationName+" between kingdoms "+kingdomName+" and "+diplomacyKingdomName);
							}
							DiplomacyTicket ticket = new DiplomacyTicket(kingdomName, diplomacyKingdomName, relation, true);
							diplomacyTickets.add(ticket);
						}
					}
					if(kingdomSection.contains("diplomacy_request")) {
						for(String diplomacyKingdomName : kingdomSection.getConfigurationSection("diplomacy_request").getKeys(false)) {
							String relationName = kingdomSection.getConfigurationSection("diplomacy_request").getString(diplomacyKingdomName);
							KonquestDiplomacyType relation = KonquestDiplomacyType.getDefault();
							try {
								relation = KonquestDiplomacyType.valueOf(relationName);
							} catch(Exception e) {
								ChatUtil.printConsoleAlert("Failed to parse request diplomacy relation "+relationName+" between kingdoms "+kingdomName+" and "+diplomacyKingdomName);
							}
							DiplomacyTicket ticket = new DiplomacyTicket(kingdomName, diplomacyKingdomName, relation, false);
							diplomacyTickets.add(ticket);
						}
					}
        			// Capital and Town Loading all towns
                	boolean isMissingMonuments = false;
                	boolean isMissingCapital = true;
                	for(String townName : kingdomSection.getConfigurationSection("towns").getKeys(false)) {
                		boolean isCapital = false;
                		if(townName.equals("capital")) {
                			isCapital = true;
                			isMissingCapital = false;
                		}
                    	ConfigurationSection townSection = kingdomSection.getConfigurationSection("towns."+townName);
                    	if(townSection != null) {
                    		worldName = townSection.getString("world",defaultWorldName);
                    		World townWorld = Bukkit.getServer().getWorld(worldName);
                    		if(townWorld != null) {
                    			// Gather town data
        	            		int base = townSection.getInt("base");
        	            		sectionList = townSection.getDoubleList("spawn");
        	            		x = sectionList.get(0);
        	            		y = sectionList.get(1);
        	            		z = sectionList.get(2);
        	            		if(sectionList.size() > 3) {
        	            			double val = sectionList.get(3);
        	            			pitch = (float)val;
        	            		} else {
        	            			pitch = 0;	
        	            		}
        	            		if(sectionList.size() > 4) {
        	            			double val = sectionList.get(4);
        	            			yaw = (float)val;
        	            		} else {
        	            			yaw = 0;	
        	            		}
        	                	Location townSpawn = new Location(townWorld,x,y,z,yaw,pitch);
        		            	sectionList = townSection.getDoubleList("center");
        	            		x = sectionList.get(0);
        	            		y = sectionList.get(1);
        	            		z = sectionList.get(2);
        		            	Location townCenter = new Location(townWorld,x,y,z);
        		            	// Create Town/Capital
        		            	KonTown town;
        		            	if(isCapital) {
        		            		town = kingdomMap.get(kingdomName).getCapital();
        		            	} else {
        		            		kingdomMap.get(kingdomName).addTown(townCenter, townName);
            		            	town = kingdomMap.get(kingdomName).getTown(townName);
        		            	}
        		            	// Town Properties
        	        			ConfigurationSection townPropertiesSection = townSection.getConfigurationSection("properties");
        	        			for(String propertyName : townPropertiesSection.getKeys(false)) {
        	        				boolean value = townPropertiesSection.getBoolean(propertyName);
        	        				KonPropertyFlag property = KonPropertyFlag.getFlag(propertyName);
        	        				boolean status = town.setPropertyValue(property, value);
        	        				if(!status) {
        	        					ChatUtil.printDebug("Failed to set invalid property "+propertyName+" to Town "+townName);
        	        				}
        	        			}
        		            	// Set spawn point
        		            	town.setSpawn(townSpawn);
        		            	// Setup town monument parameters from template
        		            	int monumentStatus = town.loadMonument(base, kingdomMap.get(kingdomName).getMonumentTemplate());
        		            	if(monumentStatus != 0) {
        		            		isMissingMonuments = true;
        		            		ChatUtil.printConsoleError("Failed to load monument for Town "+townName+" in kingdom "+kingdomName+" from invalid template");
        		            	}
        		            	// Add all Town chunk claims
        		            	town.addPoints(konquest.formatStringToPoints(townSection.getString("chunks")));
        		            	// Update territory cache
        		            	konquest.getTerritoryManager().addAllTerritory(townWorld,town.getChunkList());
								// Set specialization
								String specializationName = townSection.getString("specialization","NONE");
								Villager.Profession profession = Villager.Profession.NONE;
								try {
									profession = Villager.Profession.valueOf(specializationName);
								} catch(Exception e) {
									ChatUtil.printConsoleAlert("Failed to parse profession "+specializationName+" for town "+townName);
								}
								town.setSpecialization(profession);
        			        	// Set shield
        			        	boolean isShieldActive = townSection.getBoolean("shield",false);
        			        	int shieldTime = townSection.getInt("shield_time",0);
        			        	Date now = new Date();
        			        	if(isShieldsEnabled && isShieldActive && shieldTime > (now.getTime()/1000)) {
        			        		town.activateShield(shieldTime);
        			        	}
        			        	// Set armor
        			        	boolean isArmorActive = townSection.getBoolean("armor",false);
        			        	int armorBlocks = townSection.getInt("armor_blocks",0);
        			        	if(isArmorsEnabled && isArmorActive && armorBlocks > 0) {
        			        		town.activateArmor(armorBlocks);
        			        	}
        		            	// Set open flag
        		            	boolean isOpen = townSection.getBoolean("open",false);
        		            	town.setIsOpen(isOpen);
        		            	// Set plot flag
        		            	boolean isPlotOnly = townSection.getBoolean("plot",false);
        		            	town.setIsPlotOnly(isPlotOnly);
        		            	// Set redstone flag
        		            	boolean isRedstone = townSection.getBoolean("redstone",false);
        		            	town.setIsEnemyRedstoneAllowed(isRedstone);
        		            	// Set golem offensive flag
        		            	boolean isGolemOffensive = townSection.getBoolean("golem_offensive",false);
        		            	town.setIsGolemOffensive(isGolemOffensive);
        		            	// Assign Lord
        		            	String lordUUID = townSection.getString("lord","");
        		            	if(!lordUUID.equalsIgnoreCase("")) {
        		            		UUID playerID = Konquest.idFromString(lordUUID);
        		            		if(playerID != null) {
        		            			town.setLord(playerID);
        		            		} else {
        		            			ChatUtil.printDebug("Town "+townName+" in kingdom "+kingdomName+" has a null UUID! Lord remains invalid");
        		            		}
        		            	} else {
        		            		ChatUtil.printDebug("Town "+townName+" in kingdom "+kingdomName+" does not have a stored Lord ID");
        		            	}
        		            	// Populate Residents
        		            	if(townSection.contains("residents")) {
        			            	for(String residentUUID : townSection.getConfigurationSection("residents").getKeys(false)) {
        			            		boolean isElite = townSection.getBoolean("residents."+residentUUID);
        			            		town.addPlayerResident(Bukkit.getOfflinePlayer(UUID.fromString(residentUUID)),isElite);
        			            	}
        		            	}
        		            	// Add invite requests
        		            	if(townSection.contains("requests")) {
        		            		for(String requestUUID : townSection.getConfigurationSection("requests").getKeys(false)) {
        		            			boolean type = townSection.getBoolean("requests."+requestUUID);
        		            			town.addJoinRequest(UUID.fromString(requestUUID), type);
        		            		}
        		            	}
        		            	// Add upgrades
        		            	if(townSection.contains("upgrades")) {
        		            		for(String upgradeName : townSection.getConfigurationSection("upgrades").getKeys(false)) {
        		            			int level = townSection.getInt("upgrades."+upgradeName);
        		            			KonUpgrade upgrade = KonUpgrade.getUpgrade(upgradeName);
        		            			if(upgrade != null) {
        		            				town.addUpgrade(upgrade, level);
        		            			}
        		            		}
        		            	}
        		            	// Update upgrade status
        		            	konquest.getUpgradeManager().updateTownDisabledUpgrades(town);
        		            	// Create plots
        		            	if(townSection.contains("plots")) {
        		            		for(String plotIndex : townSection.getConfigurationSection("plots").getKeys(false)) {
        		            			//ChatUtil.printDebug("Creating plot "+plotIndex+" for town "+town.getName());
										HashSet<Point> points = new HashSet<>(konquest.formatStringToPoints(townSection.getString("plots." + plotIndex + ".chunks")));
        		            			ArrayList<UUID> users = new ArrayList<>();
        		            			for(String user : townSection.getStringList("plots."+plotIndex+".members")) {
        		            				users.add(UUID.fromString(user));
        		            			}
        		            			KonPlot plot = new KonPlot(points,users);
        		            			if(!konquest.getPlotManager().addPlot(town, plot)) {
        		            				ChatUtil.printConsoleError("Failed to add incompatible plot to town "+town.getName());
        		            			}
        		            		}
        		            	}
        	        			// Update loading bar
        		            	loadBar.addProgress(1);
                    		} else {
                    			String message = "Failed to load town "+townName+" in an unloaded world, "+townWorld+". Check plugin load order.";
                    			ChatUtil.printConsoleError(message);
                    			konquest.opStatusMessages.add(message);
                    		}
        	            } else {
        	            	ChatUtil.printDebug("Internal error, null town section \""+townName+"\" in kingdoms.yml for kingdom "+kingdomName);
        	            }
                	}
                	if(isMissingCapital) {
                		String message = "Kingdom "+kingdomName+" is missing a defined capital. Try removing and re-making the kingdom.";
            			ChatUtil.printConsoleError(message);
            			konquest.opStatusMessages.add(message);
                	}
                	if(isMissingMonuments) {
                		konquest.opStatusMessages.add("Kingdom "+kingdomName+" has Towns with invalid Monuments. You must set up a valid Monument Template and restart the server.");
                	}
        		} else {
        			// Error, world is not loaded
        			String message = "Failed to load kingdom "+kingdomName+" capital in an unloaded world, "+capitalWorld+". Check plugin load order.";
        			ChatUtil.printConsoleError(message);
        			konquest.opStatusMessages.add(message);
        		}
        	} else {
        		// Error, there is no capital section
        		ChatUtil.printDebug("Internal error, missing capital section in kingdoms.yml for kingdom "+kingdomName);
        	}
        }
        if(kingdomMap.isEmpty()) {
			ChatUtil.printDebug("No Kingdoms to load!");
		} else {
			// Some kingdoms exist, assign diplomacy relationships
			assignDiplomacyTickets();
			ChatUtil.printDebug("Loaded Kingdoms");
		}
	}
	
	public void saveKingdoms() {
		FileConfiguration kingdomsConfig = konquest.getConfigManager().getConfig("kingdoms");
		kingdomsConfig.set("kingdoms", null); // reset kingdoms config
		ConfigurationSection root = kingdomsConfig.createSection("kingdoms");
		for(KonKingdom kingdom : kingdomMap.values()) {
			ConfigurationSection kingdomSection = root.createSection(kingdom.getName());
			// Kingdom Template Name
			kingdomSection.set("template",kingdom.getMonumentTemplateName());
			// Kingdom Operating Mode
			kingdomSection.set("admin",kingdom.isAdminOperated());
			// Kingdom Properties
			ConfigurationSection kingdomPropertiesSection = kingdomSection.createSection("properties");
			for(KonPropertyFlag flag : kingdom.getAllProperties().keySet()) {
				kingdomPropertiesSection.set(flag.toString(), kingdom.getAllProperties().get(flag));
			}
			// Kingdom Membership
			kingdomSection.set("master", "");
			ConfigurationSection kingdomMemberSection = kingdomSection.createSection("members");
			for(OfflinePlayer member : kingdom.getPlayerMembers()) {
				String uuid = member.getUniqueId().toString();
				if(kingdom.isMaster(member.getUniqueId())) {
					kingdomSection.set("master", uuid);
				} else if(kingdom.isOfficer(member.getUniqueId())) {
					kingdomMemberSection.set(uuid, true);
				} else {
					kingdomMemberSection.set(uuid, false);
				}
			}
			ConfigurationSection kingdomRequestsSection = kingdomSection.createSection("requests");
			for(OfflinePlayer requestee : kingdom.getJoinRequests()) {
				String uuid = requestee.getUniqueId().toString();
				kingdomRequestsSection.set(uuid, false);
			}
			for(OfflinePlayer invitee : kingdom.getJoinInvites()) {
				String uuid = invitee.getUniqueId().toString();
				kingdomRequestsSection.set(uuid, true);
			}
			// Kingdom Diplomacy
			ConfigurationSection kingdomRelationActiveSection = kingdomSection.createSection("diplomacy_active");
			for(KonquestKingdom diplomacyKingdom : kingdom.getActiveRelationKingdoms()) {
				kingdomRelationActiveSection.set(diplomacyKingdom.getName(), kingdom.getActiveRelation(diplomacyKingdom).toString());
			}
			ConfigurationSection kingdomRelationRequestSection = kingdomSection.createSection("diplomacy_request");
			for(KonquestKingdom diplomacyKingdom : kingdom.getRelationRequestKingdoms()) {
				kingdomRelationRequestSection.set(diplomacyKingdom.getName(), kingdom.getRelationRequest(diplomacyKingdom).toString());
			}
			// Towns + Capital
			List<KonTown> allTowns = new ArrayList<>();
			allTowns.add(kingdom.getCapital());
			allTowns.addAll(kingdom.getTowns());
			ConfigurationSection townsSection = kingdomSection.createSection("towns");
			for(KonTown town : allTowns) {
				ConfigurationSection townInstanceSection;
				if(town.getTerritoryType().equals(KonquestTerritoryType.CAPITAL)) {
					townInstanceSection = townsSection.createSection("capital");
				} else {
					townInstanceSection = townsSection.createSection(town.getName());
				}
				// Town Properties
				ConfigurationSection townPropertiesSection = townInstanceSection.createSection("properties");
				for(KonPropertyFlag flag : town.getAllProperties().keySet()) {
					townPropertiesSection.set(flag.toString(), town.getAllProperties().get(flag));
				}
            	townInstanceSection.set("world", town.getWorld().getName());
            	townInstanceSection.set("base", town.getMonument().getBaseY());
            	townInstanceSection.set("spawn", new int[] {town.getSpawnLoc().getBlockX(),
						town.getSpawnLoc().getBlockY(),
						town.getSpawnLoc().getBlockZ(),
						   									(int) town.getSpawnLoc().getPitch(),
						   									(int) town.getSpawnLoc().getYaw()});
            	townInstanceSection.set("center", new int[] {town.getCenterLoc().getBlockX(),
						town.getCenterLoc().getBlockY(),
						town.getCenterLoc().getBlockZ()});
                townInstanceSection.set("chunks", konquest.formatPointsToString(town.getChunkList().keySet()));
				townInstanceSection.set("specialization", town.getSpecialization().toString());
				townInstanceSection.set("open", town.isOpen());
                townInstanceSection.set("plot", town.isPlotOnly());
                townInstanceSection.set("redstone", town.isEnemyRedstoneAllowed());
                townInstanceSection.set("golem_offensive", town.isGolemOffensive());
                townInstanceSection.set("shield", town.isShielded());
                townInstanceSection.set("shield_time", town.getShieldEndTime());
                townInstanceSection.set("armor", town.isArmored());
                townInstanceSection.set("armor_blocks", town.getArmorBlocks());
                townInstanceSection.set("lord", "");
                ConfigurationSection townInstanceResidentSection = townInstanceSection.createSection("residents");
                for(OfflinePlayer resident : town.getPlayerResidents()) {
                	String uuid = resident.getUniqueId().toString();
                	if(town.isPlayerLord(resident)) {
                		townInstanceSection.set("lord", uuid);
                	} else if(town.isPlayerKnight(resident)) {
                		townInstanceResidentSection.set(uuid, true);
                	} else {
                		townInstanceResidentSection.set(uuid, false);
                	}
                }
                ConfigurationSection townInstanceRequestsSection = townInstanceSection.createSection("requests");
                for(OfflinePlayer requestee : town.getJoinRequests()) {
                	String uuid = requestee.getUniqueId().toString();
                	townInstanceRequestsSection.set(uuid, false);
                }
                for(OfflinePlayer invitee : town.getJoinInvites()) {
                	String uuid = invitee.getUniqueId().toString();
                	townInstanceRequestsSection.set(uuid, true);
                }
                ConfigurationSection townInstanceUpgradeSection = townInstanceSection.createSection("upgrades");
                for(KonUpgrade upgrade : KonUpgrade.values()) {
                	int level = town.getRawUpgradeLevel(upgrade);
                	if(level > 0) {
                		townInstanceUpgradeSection.set(upgrade.toString(), level);
                	}
                }
                ConfigurationSection townInstancePlotSection = townInstanceSection.createSection("plots");
                int plotIndex = 0;
                for(KonPlot plot : town.getPlots()) {
                	ConfigurationSection plotInstanceSection = townInstancePlotSection.createSection("plot_"+plotIndex);
                	plotInstanceSection.set("chunks", konquest.formatPointsToString(plot.getPoints()));
                	plotInstanceSection.set("members",plot.getUserStrings());
                	plotIndex++;
                }
            }
		}
		if(kingdomMap.isEmpty()) {
			ChatUtil.printDebug("No Kingdoms to save!");
		} else {
			ChatUtil.printDebug("Saved Kingdoms");
		}
	}
	
}
