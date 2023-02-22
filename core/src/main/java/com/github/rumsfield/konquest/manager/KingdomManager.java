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
		
		/* Player/territory are in an enemy kingdom */
		ENEMY,
		
		/* Player/territory are in an allied kingdom */
		ALLIED,
		
		/* Player/territory are in a sanctioned kingdom */
		SANCTIONED,
		
		/* Player/territory are in a peaceful kingdom */
		PEACEFUL
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
	
	private long payIntervalSeconds;
	private double payPerChunk;
	private double payPerResident;
	private double payLimit;
	private int payPercentOfficer;
	private int payPercentMaster;
	private double costRelation;
	private double costCreate;
	private double costRename;
	private double costTemplate;
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
		
		this.payIntervalSeconds = 0;
		this.payPerChunk = 0;
		this.payPerResident = 0;
		this.payLimit = 0;
		this.payPercentOfficer = 0;
		this.payPercentMaster = 0;
		this.costRelation = 0;
		this.costCreate = 0;
		this.costRename = 0;
		this.costTemplate = 0;
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
		payIntervalSeconds 	= konquest.getCore().getLong(CorePath.FAVOR_KINGDOMS_PAY_INTERVAL_SECONDS.getPath());
		payPerChunk 		= konquest.getCore().getDouble(CorePath.FAVOR_KINGDOMS_PAY_PER_CHUNK.getPath());
		payPerResident 		= konquest.getCore().getDouble(CorePath.FAVOR_KINGDOMS_PAY_PER_RESIDENT.getPath());
		payLimit 			= konquest.getCore().getDouble(CorePath.FAVOR_KINGDOMS_PAY_LIMIT.getPath());
		payPercentOfficer   = konquest.getCore().getInt(CorePath.FAVOR_KINGDOMS_BONUS_OFFICER_PERCENT.getPath());
		payPercentMaster    = konquest.getCore().getInt(CorePath.FAVOR_KINGDOMS_BONUS_MASTER_PERCENT.getPath());
		costCreate 	        = konquest.getCore().getDouble(CorePath.FAVOR_KINGDOMS_COST_CREATE.getPath());
		costRename 	        = konquest.getCore().getDouble(CorePath.FAVOR_KINGDOMS_COST_RENAME.getPath());
		costTemplate        = konquest.getCore().getDouble(CorePath.FAVOR_KINGDOMS_COST_TEMPLATE.getPath());
		costRelation 	    = konquest.getCore().getDouble(CorePath.FAVOR_KINGDOMS_COST_RELATIONSHIP.getPath());
		
		payPerChunk = payPerChunk < 0 ? 0 : payPerChunk;
		payPerResident = payPerResident < 0 ? 0 : payPerResident;
		payLimit = payLimit < 0 ? 0 : payLimit;
		costCreate = costCreate < 0 ? 0 : costCreate;
		costRename = costRename < 0 ? 0 : costRename;
		costTemplate = costTemplate < 0 ? 0 : costTemplate;
		costRelation = costRelation < 0 ? 0 : costRelation;
		payPercentOfficer = Math.max(payPercentOfficer, 0);
		payPercentOfficer = Math.min(payPercentOfficer, 100);
		payPercentMaster = Math.max(payPercentMaster, 0);
		payPercentMaster = Math.min(payPercentMaster, 100);
		
		if(payIntervalSeconds > 0) {
			payTimer.stopTimer();
			payTimer.setTime((int)payIntervalSeconds);
			payTimer.startLoopTimer();
		}
	}
	
	public double getCostRelation() {
		return costRelation;
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
	            	ChatUtil.sendNotice(player, MessagePath.COMMAND_GUILD_NOTICE_PAY.getMessage());
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
				if(member instanceof KonPlayer) {
					ChatUtil.printDebug("Removing online KonPlayer "+playerName+" to barbarian.");
					KonPlayer onlinePlayer = (KonPlayer)member;
					// Online-only updates
					onlinePlayer.getPlayerPrefix().setEnable(false);
					konquest.updateNamePackets(onlinePlayer);
				} else {
					ChatUtil.printDebug("Removing offline KonOfflinePlayer "+playerName+" to barbarian.");
				}
				member.setKingdom(getBarbarians());
				member.setExileKingdom(getBarbarians());
				member.setBarbarian(true);
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

	/*
	 * =================================================
	 * Player Assignment Methods
	 * =================================================
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
		// At this point, either onlinePlayer or offlinePlayer is null
		// isOnline = true -> onlinePlayer is not null
		// isOnline = false -> offlinePlayer is not null
		
		KonKingdom joinKingdom = getKingdom(kingdomName);
		
		// Check for existing membership (both online & offline)
		KonKingdom playerKingdom = isOnline ? onlinePlayer.getKingdom() : offlinePlayer.getKingdom();
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
    		if(isOnline && isPlayerJoinCooldown(onlinePlayer.getBukkitPlayer())) {
    			return 8;
    		}
			// Check for permission (online only)
			boolean isPerKingdomJoin = konquest.getCore().getBoolean(CorePath.KINGDOMS_PER_KINGDOM_JOIN_PERMISSIONS.getPath(),false);
			if(isOnline && isPerKingdomJoin) {
				String permission = "konquest.join."+getKingdom(kingdomName).getName().toLowerCase();
				if(!onlinePlayer.getBukkitPlayer().hasPermission(permission)) {
					return 3;
				}
			}
			// Check switching criteria (online only)
			boolean isAllowSwitch = konquest.getCore().getBoolean(CorePath.KINGDOMS_ALLOW_EXILE_SWITCH.getPath(),false);
			if(isOnline && !joinKingdom.equals(onlinePlayer.getExileKingdom()) && !getBarbarians().equals(onlinePlayer.getExileKingdom()) && !isAllowSwitch) {
				return 7;
			}
			// Check if max_player_diff is disabled, or if the desired kingdom is within the max diff (both online & offline)
			int config_max_player_diff = konquest.getCore().getInt(CorePath.KINGDOMS_MAX_PLAYER_DIFF.getPath(),0);
			int smallestKingdomPlayerCount = 0;
			int targetKingdomPlayerCount = 0;
			if(config_max_player_diff != 0) {
				// Find the smallest kingdom, compare player count to this kingdom's
				String smallestKingdomName = getSmallestKingdomName();
				smallestKingdomPlayerCount = konquest.getPlayerManager().getAllPlayersInKingdom(smallestKingdomName).size();
				targetKingdomPlayerCount = konquest.getPlayerManager().getAllPlayersInKingdom(kingdomName).size();
			}
			if(config_max_player_diff != 0 && !(targetKingdomPlayerCount < smallestKingdomPlayerCount + config_max_player_diff)) {
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
	 * 				2	- Invalid world
	 * 				3	- Failed to find valid teleport location
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
			// Check for valid world (online only)
			if(isOnline && !konquest.isWorldValid(onlinePlayer.getBukkitPlayer().getLocation().getWorld())) {
		    	return 2;
			}
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
		if(removeStatus) {
			if(isOnline) {
				ChatUtil.sendNotice(onlinePlayer.getBukkitPlayer(), "Removed from kingdom and all towns");
			}
    	} else {
    		// Something went very wrong, the prior checks should have avoided this
    		ChatUtil.printDebug("Failed to remove member "+id.toString()+" from kingdom "+oldKingdom.getName());
    		return -1;
    	}
		
		/* Now, oldKingdom is possibly removed. Re-acquire current kingdom of player. */
		oldKingdom = isOnline ? onlinePlayer.getKingdom() : offlinePlayer.getKingdom();
		
    	// Try to teleport the player (online only)
    	if(isOnline && teleport) {
    		boolean doWorldSpawn = konquest.getCore().getBoolean(CorePath.EXILE_TELEPORT_WORLD_SPAWN.getPath(), false);
    		boolean doWildTeleport = konquest.getCore().getBoolean(CorePath.EXILE_TELEPORT_WILD.getPath(), true);
    		World playerWorld = onlinePlayer.getBukkitPlayer().getLocation().getWorld();
    		Location exileLoc = null;
    		if(doWorldSpawn) {
    			exileLoc = playerWorld.getSpawnLocation();
    		} else if(doWildTeleport) {
    			exileLoc = konquest.getRandomWildLocation(playerWorld);
    			if(exileLoc == null) {
    	    		return 3;
    	    	}
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
    		// Refresh any territory bars
    		KonTerritory territory = konquest.getTerritoryManager().getChunkTerritory(onlinePlayer.getBukkitPlayer().getLocation());
    		if(territory instanceof KonBarDisplayer) {
    			((KonBarDisplayer)territory).removeBarPlayer(onlinePlayer);
    			((KonBarDisplayer)territory).addBarPlayer(onlinePlayer);
    		}
    		// Updates
    		konquest.updateNamePackets(onlinePlayer);
    		konquest.getTerritoryManager().updatePlayerBorderParticles(onlinePlayer);
    		// Cool-down
    		if(!force) {
    			applyPlayerExileCooldown(onlinePlayer.getBukkitPlayer());
    		}
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
	//TODO: KR Replace GUILD command messages with KINGDOM
	
	//TODO: Ensure all menu methods appropriately provide messaging to player and success/fail sounds
	
	/**
	 * Online Player requests to join the kingdom to the officers.
	 * This method is meant to be called via the GUI menu.
	 * When the kingdom is closed, a join request is sent to officers.
	 * When open, the player instantly joins.
	 * @param player - The online player requesting to join the kingdom
	 * @param kingdom - The target kingdom that the player requests to join
	 */
	public void menuJoinKingdomRequest(KonPlayer player, KonKingdom kingdom) {
		if(kingdom == null) {
			return;
		}
		UUID id = player.getBukkitPlayer().getUniqueId();
		// Player can be barbarians, or members of other kingdoms, when requesting to join a kingdom.
		
		// Cannot request to join your own current kingdom
		if(player.getKingdom().equals(kingdom) || kingdom.isMember(id)) {
			Konquest.playFailSound(player.getBukkitPlayer());
			return;
		}
		
		if(kingdom.isOpen() || kingdom.isJoinInviteValid(id)) {
			
			// There is already a valid invite, assign the player to the kingdom
			int status = assignPlayerKingdom(id,kingdom.getName(),false);
			//TODO: expand error messaging
			if(status != 0) {
				Konquest.playFailSound(player.getBukkitPlayer());
				return;
			}
			
			kingdom.removeJoinRequest(id);
			ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_GUILD_NOTICE_JOINED.getMessage(kingdom.getName()));
			Konquest.playSuccessSound(player.getBukkitPlayer());
		} else if(!kingdom.isJoinRequestValid(id)){
			// Request to join if not already requested
			kingdom.addJoinRequest(id, false);
			ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_GUILD_NOTICE_REQUEST_JOIN.getMessage(kingdom.getName()));
			Konquest.playSuccessSound(player.getBukkitPlayer());
			broadcastOfficers(kingdom, MessagePath.COMMAND_GUILD_NOTICE_REQUEST_NEW.getMessage());
		} else {
			// A join request has already been sent, do nothing
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_GUILD_ERROR_REQUEST_SENT.getMessage(kingdom.getName()));
			Konquest.playFailSound(player.getBukkitPlayer());
		}
	}
	
	/**
	 * Officer approves or denies join request of (offline) player.
	 * Check to ensure target player is not already a member.
	 * Approving the request always lets the player join.
	 * @param player - The offline player target of the response
	 * @param kingdom - The target kingdom that player wants to join
	 * @param resp - True to approve, false to reject request
	 * @return True when request response was successful, else false when target is already in this kingdom.
	 */
	public boolean menuRespondKingdomRequest(OfflinePlayer player, KonKingdom kingdom, boolean resp) {
		//TODO: menu sounds/messaging
		if(kingdom == null) {
			return true;
		}
		UUID id = player.getUniqueId();
		
		if(!kingdom.isJoinRequestValid(id)) return true;
		if(resp) {
			// Ensure the player is not already a kingdom member
			if(kingdom.isMember(id)) {
				return false;
			}
			// Approved join request, add player as member
			int status = assignPlayerKingdom(id,kingdom.getName(),false);
			if(status != 0) {
				return false;
			}

			if(player.isOnline()) {
				ChatUtil.sendNotice((Player)player, MessagePath.COMMAND_GUILD_NOTICE_JOINED.getMessage(kingdom.getName()));
			}
		} else {
			// Denied join request
			if(player.isOnline()) {
				ChatUtil.sendError((Player)player, MessagePath.COMMAND_GUILD_ERROR_JOIN_DENY.getMessage(kingdom.getName()));
			}
		}
		kingdom.removeJoinRequest(id);

		return true;
	}
	
	/**
	 * Officer invites (offline) player to join (with /kingdom add command).
	 * Kingdom must be closed to send invites.
	 * Allow invites to players already in another kingdom.
	 * Inviting a player who has already requested to join always lets them join.
	 * @param player - The offline player that the officer invites to join their kingdom
	 * @param kingdom - The target kingdom for the join invite
	 * @return Error code:  0 - Success
	 * 						1 - Player is already a member
	 * 						2 - Player has already been invited
	 * 						3 - Kingdom must be closed to send invites
	 * 						-1 - Internal error
	 */
	public int joinKingdomInvite(OfflinePlayer player, KonKingdom kingdom) {
		if(kingdom == null) {
			return -1;
		}
		UUID id = player.getUniqueId();
		
		// Cannot invite a member of this kingdom
		if(kingdom.isMember(id)) return 1;
		
		// Cannot invite members to an open kingdom
		if(kingdom.isOpen()) return 3;
		
		if(kingdom.isJoinRequestValid(id)) {
			// There is already a valid request, add the player to the kingdom
			//TODO: Handle error conditions for assignment
			int status = assignPlayerKingdom(id,kingdom.getName(),false);
			if(status != 0) {
				
				return -1;
			}
			
			kingdom.removeJoinRequest(id);
			if(player.isOnline()) {
				ChatUtil.sendNotice((Player)player, MessagePath.COMMAND_GUILD_NOTICE_JOINED.getMessage(kingdom.getName()));
			}
		} else if(!kingdom.isJoinInviteValid(id)) {
			// Invite to join if not already invited
			kingdom.addJoinRequest(id, true);
			if(player.isOnline()) {
				ChatUtil.sendNotice((Player)player, MessagePath.COMMAND_GUILD_NOTICE_INVITE_NEW.getMessage());
			}
		} else {
			// The invite has already been sent, still pending
			return 2;
		}
		
		return 0;
	}
	
	/**
	 * Player accepts or declines join invite
	 * Accepting always lets them join
	 * @param player - The online player responding to the invite to join
	 * @param kingdom - The target kingdom that the player is responding to
	 * @param resp - True to accept and join kingdom, false to decline invite
	 */
	public boolean menuRespondKingdomInvite(KonPlayer player, KonKingdom kingdom, boolean resp) {
		if(kingdom == null) return false;
		UUID id = player.getBukkitPlayer().getUniqueId();
		
		// Cannot accept invite from a member of this kingdom
		if(kingdom.isMember(id)) return false;
		
		if(!kingdom.isJoinInviteValid(id)) return false;
		if(!resp){
			// Denied join request
			kingdom.removeJoinRequest(id);
			ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_GUILD_NOTICE_INVITE_DECLINE.getMessage(kingdom.getName()));
			return false;
		}

		int status = assignPlayerKingdom(id,kingdom.getName(),false);
		if(status != 0) {
			Konquest.playFailSound(player.getBukkitPlayer());
			return false;
		}

		kingdom.removeJoinRequest(id);
		ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_GUILD_NOTICE_JOINED.getMessage(kingdom.getName()));
		Konquest.playSuccessSound(player.getBukkitPlayer());
		return true;

	}
	
	// Effectively just a wrapper for exile for online players by their choice
	public void menuExileKingdom(KonPlayer player, KonKingdom kingdom) {
		if(kingdom == null || !kingdom.isCreated())return;
		UUID id = player.getBukkitPlayer().getUniqueId();
		int status = exilePlayerBarbarian(id,true,true,false,false);
		if(status == 0) {
			ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_GUILD_NOTICE_LEAVE.getMessage(kingdom.getName()));
			Konquest.playSuccessSound(player.getBukkitPlayer());
		} else {
			//TODO: Expand error messaging for each return code of exilePlayerBarbarian
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_GUILD_ERROR_LEAVE_FAIL.getMessage());
			Konquest.playFailSound(player.getBukkitPlayer());
		}
	}
	
	// Wrapper for exile for offline players
	// Used by officers to remove players from their kingdom
	public boolean kickKingdomMember(OfflinePlayer player, KonKingdom kingdom) {
		boolean result = false;
		if(kingdom == null || !kingdom.isCreated())return result;

		UUID id = player.getUniqueId();
		int status = exilePlayerBarbarian(id,true,true,false,false);
		if(status == 0) {
			result = true;
		}
		//TODO: expand messaging or return more error codes

		return result;
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
	
	public List<KonKingdom> getInviteKingdoms(KonPlayer player) {
		List<KonKingdom> result = new ArrayList<>();
		for(KonKingdom kingdom : kingdomMap.values()) {
			if(!player.getKingdom().equals(kingdom) && kingdom.isJoinInviteValid(player.getBukkitPlayer().getUniqueId())) {
				result.add(kingdom);
			}
		}
		return result;
	}
	
	public boolean menuPromoteOfficer(OfflinePlayer player, KonKingdom kingdom) {
		UUID id = player.getUniqueId();
		if(kingdom.isOfficer(id))return false;

		if(kingdom.setOfficer(id, true)) {
			broadcastMembers(kingdom,MessagePath.COMMAND_GUILD_BROADCAST_PROMOTE.getMessage(player.getName(),kingdom.getName()));
			return true;
		}

		return false;
	}
	
	public boolean menuDemoteOfficer(OfflinePlayer player, KonKingdom kingdom) {
		UUID id = player.getUniqueId();
		if(!kingdom.isOfficer(id)) {
			return false;
		}

		if(kingdom.setOfficer(id, false)) {
			broadcastMembers(kingdom,MessagePath.COMMAND_GUILD_BROADCAST_DEMOTE.getMessage(player.getName(),kingdom.getName()));
			return true;
		}

		return false;
	}
	
	public void menuTransferMaster(OfflinePlayer master, KonKingdom kingdom, KonPlayer sender) {
		UUID id = master.getUniqueId();
		if(kingdom.isMember(id) && kingdom.setMaster(id)) {
			broadcastMembers(kingdom,MessagePath.COMMAND_GUILD_BROADCAST_TRANSFER.getMessage(master.getName(),kingdom.getName()));
			Konquest.playSuccessSound(sender.getBukkitPlayer());
		} else {
			Konquest.playFailSound(sender.getBukkitPlayer());
			ChatUtil.sendError(sender.getBukkitPlayer(), MessagePath.GENERIC_ERROR_FAILED.getPath());
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
	
	//TODO: Update messages from guild to kingdom
	public void menuToggleKingdomOpen(KonKingdom kingdom, KonPlayer player) {
		if(kingdom == null || !kingdom.isCreated())return;
		if(kingdom.isOpen()) {
			kingdom.setIsOpen(false);
			ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_GUILD_NOTICE_CLOSED.getMessage(kingdom.getName()));
		} else {
			kingdom.setIsOpen(true);
			ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_GUILD_NOTICE_OPEN.getMessage(kingdom.getName()));
		}
	}
	
	public void menuDisbandKingdom(KonKingdom kingdom, KonPlayer player) {
		if(kingdom == null || !kingdom.isCreated())return;
		boolean status = removeKingdom(kingdom.getName());
		if(status) {
			//TODO: update message path
			ChatUtil.sendNotice(player.getBukkitPlayer(), "Disbanded kingdom");
			Konquest.playSuccessSound(player.getBukkitPlayer());
		} else {
			ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
			Konquest.playFailSound(player.getBukkitPlayer());
		}
	}
	
	// The primary way to change a kingdom's template, from the kingdom GUI menu
	public void menuChangeKingdomTemplate(KonKingdom kingdom, KonMonumentTemplate template, KonPlayer player, boolean ignoreCost) {
		//TODO: Update message paths
		Player bukkitPlayer = player.getBukkitPlayer();
		// Verify template is valid
		if(template == null || !template.isValid()) {
			ChatUtil.sendError(bukkitPlayer, "Template is invalid");
			Konquest.playFailSound(player.getBukkitPlayer());
			return;
		}
		// Check capital is not under attack
		if(kingdom.getCapital().isAttacked()) {
			ChatUtil.sendError(bukkitPlayer, "Cannot change template while capital is under attack");
			Konquest.playFailSound(player.getBukkitPlayer());
			return;
		}
		// Check cost
		double totalCost = costTemplate + template.getCost();
		if(!ignoreCost && totalCost > 0 && KonquestPlugin.getBalance(bukkitPlayer) < totalCost) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(totalCost));
			Konquest.playFailSound(player.getBukkitPlayer());
            return;
    	}
		// Update template
		kingdom.updateMonumentTemplate(template);
		// Withdraw cost
		if(!ignoreCost && totalCost > 0 && KonquestPlugin.withdrawPlayer(bukkitPlayer, totalCost)) {
            konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)totalCost);
		}
		// Success
		ChatUtil.sendNotice(player.getBukkitPlayer(), "Updated template to "+template.getName());
		Konquest.playSuccessSound(player.getBukkitPlayer());
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
		return true;
	}
	
	
	/*
	 * =================================================
	 * Relationship Methods
	 * =================================================
	 */
	
	/*
	 * Each kingdom has an internal map of its active relationship to other kingdoms, plus current relationship requests.
	 * The default relation is peace.
	 * Relationships are: enemy, sanction, peace, allied.
	 * 
	 * 
	 * State flow for [kingdom1, kingdom2]...
	 * 
	 * JOINT STATE: PEACE
	 * (Includes sanction limits)
	 * [peace,peace] 			-> both kingdoms are at peace
	 * - [allied,peace] 		-> kingdom1 requests alliance, kingdom2 must also change to allied
	 *   - [allied,enemy]       -> kingdom2 becomes enemy, go to OPTION A or B
	 *   - [allied,sanction]	-> kingdom2 sanctions kingdom1 instead of allied, no alliance bonuses
	 *   - [allied,allied] 		-> both kingdoms allied, apply ALLIANCE
	 * - [sanction,peace] 		-> if either kingdom is sanction, apply limits to other
	 * OPTION A (instant war)
	 * - [enemy,enemy]          -> if any kingdom changes to enemy, the other kingdom is forced to enemy as well, apply WAR
	 * OPTION B (mutual war)
	 * - [enemy,peace]			-> both kingdoms must change to enemy for war conditions, no war yet, resets other kingdom to default if allied
	 *   - [enemy,allied]       -> INVALID: kingdoms cannot be allied with enemy kingdoms
	 *   - [enemy,sanction]     -> kingdom2 sanctions kingdom1, still no war
	 *   - [enemy,enemy]		-> when both kingdoms change to enemy, apply war
	 *   
	 * JOINT STATE: ALLIANCE
	 * [allied,allied] 		    -> both kingdoms are in an alliance
	 * - [peace,peace]      	-> kingdom2 changes to peace, breaks alliance, resets kingdom1 to default, apply PEACE
	 * - [peace,sanction]   	-> kingdom2 changes to sanction, breaks alliance, resets kingdom1 to default, apply PEACE
	 * - [peace,enemy]      	-> if either kingdom changes to enemy, apply PEACE then go to OPTION A or B (reset other to default)
	 * 
	 * JOINT STATE: WAR
	 * [enemy,enemy]			-> both kingdoms are at war
	 * - [enemy,allied]         -> INVALID: kingdoms cannot be allied in war
	 * - [enemy,sanction]       -> INVALID: kingdoms cannot sanction in war
	 * OPTION C (instant peace)
	 * - [peace,peace]			-> if either kingdom at war changes to peace, other is forced to peace as well, apply PEACE
	 * OPTION D (mutual peace)
	 * - [peace,enemy]			-> both kingdoms must change to peace to end war conditions
	 *   - [peace,allied]       -> INVALID: kingdoms cannot be allied in war
	 *   - [peace,sanction] 	-> INVALID: kingdoms cannot sanction in war
	 *   - [peace,peace] 		-> both kingdoms change to peace, apply PEACE
	 * 
	 */
	 
	//TODO: menu messaging
	public boolean menuChangeKingdomRelation(@Nullable KonKingdom kingdom,@Nullable  KonKingdom otherKingdom,@NotNull KonquestRelationship relation,@NotNull KonPlayer player, boolean isAdmin) {
		
		if(kingdom == null || otherKingdom == null) return false;
		
		Player bukkitPlayer = player.getBukkitPlayer();
		// Check cost
		if(costRelation > 0 && !isAdmin) {
			if(KonquestPlugin.getBalance(bukkitPlayer) < costRelation) {
				Konquest.playFailSound(player.getBukkitPlayer());
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(costRelation));
                return false;
			}
    	}
		
		KonquestRelationship ourRelation = kingdom.getActiveRelation(otherKingdom);
		KonquestRelationship theirRelation = otherKingdom.getActiveRelation(kingdom);
		
		if(ourRelation.equals(relation)) {
			// Cannot change to existing active relation
			Konquest.playFailSound(player.getBukkitPlayer());
			return false;
		}
		
		ChatUtil.printDebug("Changing kingdom "+kingdom.getName()+" relation to "+otherKingdom.getName()+", "+relation.toString());
		ChatUtil.printDebug("Our relationship is currently "+ ourRelation);
		ChatUtil.printDebug("Their relationship is currently "+theirRelation.toString());
		
		boolean isInstantWar = konquest.getCore().getBoolean(CorePath.KINGDOMS_INSTANT_WAR.getPath(), false);
		boolean isInstantPeace = konquest.getCore().getBoolean(CorePath.KINGDOMS_INSTANT_PEACE.getPath(), false);
		
		// Relationship transition logic
		if(relation.equals(KonquestRelationship.PEACE)) {
			// Change to peace
			if(ourRelation.equals(KonquestRelationship.ENEMY)) {
				// We are enemies, try to make peace
				if(isInstantPeace) {
					// Instantly change to active peace, force them to peace too
					setJointActiveRelation(kingdom,otherKingdom,relation);
				} else {
					// Check if they already request peace
					if(otherKingdom.hasRelationRequest(kingdom) && otherKingdom.getRelationRequest(kingdom).equals(relation)) {
						// Both sides request peace, change to active peace
						setJointActiveRelation(kingdom,otherKingdom,relation);
					} else {
						// We request peace, still at war
						kingdom.setRelationRequest(otherKingdom, relation);
					}
				}
			} else {
				// We are not enemies
				if(ourRelation.equals(KonquestRelationship.ALLIED)) {
					// We are allies
					// Instantly break alliance, reset both of us to default
					removeJointActiveRelation(kingdom,otherKingdom);
				}
				// Apply relation
				kingdom.setActiveRelation(otherKingdom, relation);
				kingdom.removeRelationRequest(otherKingdom);
			}
			
		} else if (relation.equals(KonquestRelationship.SANCTIONED)) {
			// Change to sanctioned
			if(!ourRelation.equals(KonquestRelationship.ENEMY)) {
				// Cannot sanction enemies
				if(ourRelation.equals(KonquestRelationship.ALLIED)) {
					// We are allies
					// Instantly break alliance, reset both of us to default
					removeJointActiveRelation(kingdom,otherKingdom);
				}
				// Apply relation
				kingdom.setActiveRelation(otherKingdom, relation);
				kingdom.removeRelationRequest(otherKingdom);
			}
			
		} else if(relation.equals(KonquestRelationship.ENEMY)) {
			// Attempt to declare war
			if(isInstantWar) {
				// Apply joint enemy
				setJointActiveRelation(kingdom,otherKingdom,relation);
			} else {
				// Request war
				// Check if they already request enemy
				if(otherKingdom.hasRelationRequest(kingdom) && otherKingdom.getRelationRequest(kingdom).equals(relation)) {
					// Both sides request enemy, change to active war
					setJointActiveRelation(kingdom,otherKingdom,relation);
				} else {
					if(ourRelation.equals(KonquestRelationship.ALLIED)) {
						// Instantly break alliance, reset both of us to default
						removeJointActiveRelation(kingdom,otherKingdom);
					}
					// We request enemy
					kingdom.setRelationRequest(otherKingdom, relation);
				}
			}
			
		} else if(relation.equals(KonquestRelationship.ALLIED)) {
			// Attempt to form an alliance
			if(!ourRelation.equals(KonquestRelationship.ENEMY)) {
				// Cannot allay enemies
				// Check if they already request allied
				if(otherKingdom.hasRelationRequest(kingdom) && otherKingdom.getRelationRequest(kingdom).equals(relation)) {
					// Both sides request allied, change to active alliance
					setJointActiveRelation(kingdom,otherKingdom,relation);
				} else {
					// We request allied
					kingdom.setRelationRequest(otherKingdom, relation);
				}
			}
		}
		
		// Debug messaging
		KonquestRelationship ourActiveRelation = kingdom.getActiveRelation(otherKingdom);
		KonquestRelationship theirActiveRelation = otherKingdom.getActiveRelation(kingdom);
		KonquestRelationship ourRelationRequest = kingdom.getRelationRequest(otherKingdom);
		KonquestRelationship theirRelationRequest = otherKingdom.getRelationRequest(kingdom);
		ChatUtil.printDebug("Finished: "+kingdom.getName()+" ["+ourActiveRelation.toString()+","+ourRelationRequest.toString()+"]; "+otherKingdom.getName()+" ["+theirActiveRelation.toString()+","+theirRelationRequest.toString()+"]");
		
		// Withdraw cost
		if(costRelation > 0 && !isAdmin) {
            if(KonquestPlugin.withdrawPlayer(bukkitPlayer, costRelation)) {
            	konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)costRelation);
            }
		}
		
		Konquest.playSuccessSound(player.getBukkitPlayer());
		return true;
	}
	
	public boolean isValidRelationChoice(@NotNull KonKingdom kingdom1,@NotNull  KonKingdom kingdom2,@NotNull KonquestRelationship relation) {
		boolean result = false;
		KonquestRelationship ourRelation = kingdom1.getActiveRelation(kingdom2);
		// Relationship choice logic
		switch(relation) {
			case PEACE:
			case ENEMY:
				// Can always choose enemy
				// Can always choose peace
				result = true;
				break;
			case SANCTIONED:
			case ALLIED:
				// Cannot ally enemies
				// Cannot sanction enemies
				result = !ourRelation.equals(KonquestRelationship.ENEMY);
				break;
			default:
				break;
		}
		if(ourRelation.equals(relation)) {
			result = false;
		}
		return result;
	}
	
	private void setJointActiveRelation(@NotNull KonKingdom kingdom1,@NotNull KonKingdom kingdom2,@NotNull KonquestRelationship relation) {
		kingdom1.setActiveRelation(kingdom2, relation);
		kingdom1.setRelationRequest(kingdom2, relation);
		kingdom2.setActiveRelation(kingdom1, relation);
		kingdom2.setRelationRequest(kingdom1, relation);
	}
	
	private void removeJointActiveRelation(@NotNull KonKingdom kingdom1,@NotNull KonKingdom kingdom2) {
		kingdom1.removeActiveRelation(kingdom2);
		kingdom1.removeRelationRequest(kingdom2);
		kingdom2.removeActiveRelation(kingdom1);
		kingdom2.removeRelationRequest(kingdom1);
	}

	/**
	 * Check to see if a kingdom is an enemy
	 * @param kingdom1 The reference kingdom
	 * @param kingdom2 The target kingdom
	 * @return True when both kingdoms have an active enemy relationship with each other, or either kingdom is Barbarians
	 */
	public boolean isBothKingdomsEnemy(@NotNull KonquestKingdom kingdom1,@NotNull KonquestKingdom kingdom2) {
		// Barbarians are always enemies to other kingdoms
		if(kingdom1.equals(getBarbarians()) || kingdom2.equals(getBarbarians())) {
			return true;
		}
		return kingdom1.getActiveRelation(kingdom2).equals(KonquestRelationship.ENEMY) && kingdom2.getActiveRelation(kingdom1).equals(KonquestRelationship.ENEMY);
	}

	/**
	 * Check to see if a kingdom is an ally
	 * @param kingdom1 The reference kingdom
	 * @param kingdom2 The target kingdom
	 * @return True when both kingdoms have an active allied relationship with each other, and neither kingdom is Barbarians
	 */
	public boolean isBothKingdomsAllied(@NotNull KonquestKingdom kingdom1,@NotNull KonquestKingdom kingdom2) {
		// Barbarians are never allied to other kingdoms
		if(kingdom1.equals(getBarbarians()) || kingdom2.equals(getBarbarians())) {
			return false;
		}
		return kingdom1.getActiveRelation(kingdom2).equals(KonquestRelationship.ALLIED) && kingdom2.getActiveRelation(kingdom1).equals(KonquestRelationship.ALLIED);
	}

	/**
	 * Check to see if a kingdom is sanctioned
	 * @param kingdom1 The reference kingdom
	 * @param kingdom2 The target kingdom
	 * @return True when the reference kingdom sanctions the target kingdom
	 */
	public boolean isKingdomSanctioned(@NotNull KonquestKingdom kingdom1,@NotNull KonquestKingdom kingdom2) {
		return kingdom1.getActiveRelation(kingdom2).equals(KonquestRelationship.SANCTIONED);
	}

	/**
	 * Check to see if a kingdom is peaceful
	 * @param kingdom1 The reference kingdom
	 * @param kingdom2 The target kingdom
	 * @return True when the reference kingdom is peaceful with the target kingdom
	 */
	public boolean isKingdomPeaceful(@NotNull KonquestKingdom kingdom1,@NotNull KonquestKingdom kingdom2) {
		return kingdom1.getActiveRelation(kingdom2).equals(KonquestRelationship.PEACE);
	}
	
	public RelationRole getRelationRole(@Nullable KonquestKingdom displayKingdom,@Nullable KonquestKingdom contextKingdom) {
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
    			if(isBothKingdomsEnemy(displayKingdom, contextKingdom)) {
    				result = RelationRole.ENEMY;
				} else if(isBothKingdomsAllied(displayKingdom, contextKingdom)) {
					result = RelationRole.ALLIED;
				} else if(isKingdomSanctioned(displayKingdom, contextKingdom)) {
					result = RelationRole.SANCTIONED;
				} else if(isKingdomPeaceful(displayKingdom, contextKingdom)) {
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
    
    public boolean isPlayerFriendly(@NotNull KonOfflinePlayer offlinePlayer,@NotNull KonKingdom kingdom) {
    	return getRelationRole(offlinePlayer.getKingdom(),kingdom).equals(RelationRole.FRIENDLY);
    }
    
    public boolean isPlayerBarbarian(@NotNull KonOfflinePlayer offlinePlayer,@NotNull KonKingdom kingdom) {
    	return getRelationRole(offlinePlayer.getKingdom(),kingdom).equals(RelationRole.BARBARIAN);
    }
    
	/*
	 * 
	 * Town Methods
	 * 
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
			for(KonTown town : kingdom.getTowns()) {
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
	
	public int getPlayerLordships(KonPlayer player) {
		int count = 0;
		for(KonTown town : player.getKingdom().getTowns()) {
			if(town.isPlayerLord(player.getOfflineBukkitPlayer())) {
				count = count + 1;
			}
		}
		return count;
	}
	
	public List<KonTown> getPlayerLordshipTowns(KonquestOfflinePlayer player) {
		List<KonTown> townNames = new ArrayList<>();
		if(player.getKingdom() instanceof KonKingdom) {
			KonKingdom kingdom = (KonKingdom)player.getKingdom();
			for(KonTown town : kingdom.getTowns()) {
				if(town.isPlayerLord(player.getOfflineBukkitPlayer())) {
					townNames.add(town);
				}
			}
		}
		return townNames;
	}

	@NotNull
	public List<KonTown> getPlayerKnightTowns(KonquestOfflinePlayer player) {
		List<KonTown> townNames = new ArrayList<>();
		if(player.getKingdom() instanceof KonKingdom) {
			KonKingdom kingdom = (KonKingdom)player.getKingdom();
			for(KonTown town : kingdom.getTowns()) {
				if(town.isPlayerKnight(player.getOfflineBukkitPlayer()) && !town.isPlayerLord(player.getOfflineBukkitPlayer())) {
					townNames.add(town);
				}
			}
		}
		return townNames;
	}

	@NotNull
	public List<KonTown> getPlayerResidenceTowns(KonquestOfflinePlayer player) {
		List<KonTown> townNames = new ArrayList<>();
		if(player.getKingdom() instanceof KonKingdom) {
			KonKingdom kingdom = (KonKingdom)player.getKingdom();
			for(KonTown town : kingdom.getTowns()) {
				if(town.isPlayerResident(player.getOfflineBukkitPlayer())) {
					townNames.add(town);
				}
			}
		}
		return townNames;
	}
	
	public void updatePlayerMembershipStats(KonPlayer player) {
		int countLord = 0;
		int countElite = 0;
		int countResident = 0;
		for(KonTown town : player.getKingdom().getTowns()) {
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
		for(KonTown town : kingdom.getTowns()) {
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
	    	for(KonTown town : kingdom.getTowns()) {
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
			for(KonTown town : offlinePlayer.getKingdom().getTowns()) {
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
	public boolean isPlayerJoinCooldown(Player player) {
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
	
	public void applyPlayerJoinCooldown(Player player) {
		if(joinCooldownSeconds <= 0)return;
		UUID id = player.getUniqueId();
		int endTimeSeconds = (int)((new Date()).getTime()/1000) + joinCooldownSeconds;
		joinPlayerCooldowns.put(id, endTimeSeconds);
	}
	
	public int getJoinCooldownRemainingSeconds(Player player) {
		int result = 0;
		UUID id = player.getUniqueId();
		if(!joinPlayerCooldowns.containsKey(id))return 0;
		int endSeconds = joinPlayerCooldowns.get(id);
		Date now = new Date();
		if(now.after(new Date((long)endSeconds*1000))){
			return 0;
		}
		return endSeconds - (int)(now.getTime()/1000);
	}
	
	// Returns true when the player is still in cool-down, else false
	public boolean isPlayerExileCooldown(Player player) {
		boolean result = false;
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
	
	public void applyPlayerExileCooldown(Player player) {
		if(exileCooldownSeconds <= 0)return;
		UUID id = player.getUniqueId();
		int endTimeSeconds = (int)((new Date()).getTime()/1000) + exileCooldownSeconds;
		exilePlayerCooldowns.put(id, endTimeSeconds);
	}
	
	public int getExileCooldownRemainingSeconds(Player player) {
		int result = 0;
		UUID id = player.getUniqueId();
		if(!exilePlayerCooldowns.containsKey(id))return 0;
		int endSeconds = exilePlayerCooldowns.get(id);
		Date now = new Date();
		if(now.after(new Date((long)endSeconds*1000))){
			return 0;
		}
		return endSeconds - (int)(now.getTime()/1000);
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
	
	//TODO: When loading kingdom relations, if equals default, don't add
	
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
    	        		konquest.opStatusMessages.add("Missing monument template for Kingdom "+kingdomName+"! Use \"TBD\" to specify a monument template for this Kingdom.");
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
                		konquest.opStatusMessages.add("Kingdom "+kingdomName+" has Towns with invalid Monuments. You must create a new Monument Template and restart the server.");
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
			ChatUtil.printDebug("Loaded Kingdoms");
		}
	}
	
	//TODO: Save kingdom relationships (active and request)
	
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
