package konquest.manager;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Snow;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.api.event.player.KonquestPlayerExileEvent;
import konquest.api.event.player.KonquestPlayerKingdomEvent;
import konquest.api.event.territory.KonquestTerritoryChunkEvent;
import konquest.api.manager.KonquestKingdomManager;
import konquest.api.model.KonquestUpgrade;
import konquest.api.model.KonquestTerritoryType;
import konquest.api.model.KonquestOfflinePlayer;
import konquest.api.model.KonquestPlayer;
import konquest.display.OptionIcon.optionAction;
import konquest.manager.TravelManager.TravelDestination;
import konquest.model.KonBarDisplayer;
import konquest.model.KonCamp;
import konquest.model.KonDirective;
import konquest.model.KonGuild;
import konquest.model.KonKingdom;
import konquest.model.KonKingdomScoreAttributes;
import konquest.model.KonKingdomScoreAttributes.KonKingdomScoreAttribute;
import konquest.model.KonLeaderboard;
import konquest.model.KonMonumentTemplate;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonPlayerScoreAttributes;
import konquest.model.KonPlayerScoreAttributes.KonPlayerScoreAttribute;
import konquest.model.KonPlot;
import konquest.model.KonRuin;
import konquest.model.KonSanctuary;
import konquest.model.KonStats;
import konquest.model.KonStatsType;
import konquest.model.KonTerritory;
import konquest.model.KonTerritoryCache;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.CorePath;
import konquest.utility.LoadingPrinter;
import konquest.utility.MessagePath;
import konquest.utility.Timeable;
import konquest.utility.Timer;

public class KingdomManager implements KonquestKingdomManager, Timeable {

	private Konquest konquest;
	private TerritoryManager territoryManager;
	private HashMap<String, KonKingdom> kingdomMap;
	private KonKingdom barbarians;
	private KonKingdom neutrals;
	private HashMap<PotionEffectType,Integer> townNerfs;
	private Material townCriticalBlock;
	private int maxCriticalHits;
	private HashSet<Material> armorBlocks;
	private boolean isArmorBlockWhitelist;
	private int joinCooldownSeconds;
	private int exileCooldownSeconds;
	private HashMap<UUID,Integer> joinPlayerCooldowns;
	private HashMap<UUID,Integer> exilePlayerCooldowns;
	
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
	private Timer payTimer;
	
	public KingdomManager(Konquest konquest) {
		this.konquest = konquest;
		this.territoryManager = konquest.getTerritoryManager();
		this.kingdomMap = new HashMap<String, KonKingdom>();
		this.barbarians = null;
		this.neutrals = null;
		this.townNerfs = new HashMap<PotionEffectType,Integer>();
		this.townCriticalBlock = Material.OBSIDIAN;
		this.maxCriticalHits = 12;
		this.armorBlocks = new HashSet<Material>();
		this.isArmorBlockWhitelist = false;
		this.joinCooldownSeconds = 0;
		this.exileCooldownSeconds = 0;
		this.joinPlayerCooldowns = new HashMap<UUID,Integer>();
		this.exilePlayerCooldowns = new HashMap<UUID,Integer>();
		
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
		payIntervalSeconds 	= konquest.getConfigManager().getConfig("core").getLong(CorePath.FAVOR_KINGDOMS_PAY_INTERVAL_SECONDS.getPath());
		payPerChunk 		= konquest.getConfigManager().getConfig("core").getDouble(CorePath.FAVOR_KINGDOMS_PAY_PER_CHUNK.getPath());
		payPerResident 		= konquest.getConfigManager().getConfig("core").getDouble(CorePath.FAVOR_KINGDOMS_PAY_PER_RESIDENT.getPath());
		payLimit 			= konquest.getConfigManager().getConfig("core").getDouble(CorePath.FAVOR_KINGDOMS_PAY_LIMIT.getPath());
		payPercentOfficer   = konquest.getConfigManager().getConfig("core").getInt(CorePath.FAVOR_KINGDOMS_BONUS_OFFICER_PERCENT.getPath());
		payPercentMaster    = konquest.getConfigManager().getConfig("core").getInt(CorePath.FAVOR_KINGDOMS_BONUS_MASTER_PERCENT.getPath());
		costCreate 	        = konquest.getConfigManager().getConfig("core").getDouble(CorePath.FAVOR_KINGDOMS_COST_CREATE.getPath());
		costRename 	        = konquest.getConfigManager().getConfig("core").getDouble(CorePath.FAVOR_KINGDOMS_COST_RENAME.getPath());
		costTemplate        = konquest.getConfigManager().getConfig("core").getDouble(CorePath.FAVOR_KINGDOMS_COST_TEMPLATE.getPath());
		costRelation 	    = konquest.getConfigManager().getConfig("core").getDouble(CorePath.FAVOR_KINGDOMS_COST_RELATIONSHIP.getPath());
		
		payPerChunk = payPerChunk < 0 ? 0 : payPerChunk;
		payPerResident = payPerResident < 0 ? 0 : payPerResident;
		payLimit = payLimit < 0 ? 0 : payLimit;
		costCreate = costCreate < 0 ? 0 : costCreate;
		costRename = costRename < 0 ? 0 : costRename;
		costTemplate = costTemplate < 0 ? 0 : costTemplate;
		costRelation = costRelation < 0 ? 0 : costRelation;
		payPercentOfficer = payPercentOfficer < 0 ? 0 : payPercentOfficer;
		payPercentOfficer = payPercentOfficer > 100 ? 100 : payPercentOfficer;
		payPercentMaster = payPercentMaster < 0 ? 0 : payPercentMaster;
		payPercentMaster = payPercentMaster > 100 ? 100 : payPercentMaster;
		
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
	
	/*
	 * Kingdom payments for members
	 * Total payment is based on all towns & land.
	 * Kingdom members only get paid based on the towns & land they are lord of.
	 * Kingdom officers/master get a bonus = percentage of total payment.
	 */
	private void disbursePayments() {
		HashMap<OfflinePlayer,Double> payments = new HashMap<OfflinePlayer,Double>();
		HashMap<OfflinePlayer,KonKingdom> memberships = new HashMap<OfflinePlayer,KonKingdom>();
		HashMap<OfflinePlayer,KonKingdom> masters = new HashMap<OfflinePlayer,KonKingdom>();
		HashMap<OfflinePlayer,KonKingdom> officers = new HashMap<OfflinePlayer,KonKingdom>();
		HashMap<KonKingdom,Double> totalPay = new HashMap<KonKingdom,Double>();
		// Initialize payment table
		for(KonKingdom kingdom : kingdomMap.values()) {
			totalPay.put(kingdom, 0.0);
			for(OfflinePlayer offlinePlayer : kingdom.getPlayerMembers()) {
				if(offlinePlayer.isOnline()) {
					payments.put(offlinePlayer, 0.0);
					memberships.put(offlinePlayer, kingdom);
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
		int min_distance_capital = konquest.getConfigManager().getConfig("core").getInt(CorePath.TOWNS_MIN_DISTANCE_CAPITAL.getPath());
		int min_distance_town = konquest.getConfigManager().getConfig("core").getInt(CorePath.TOWNS_MIN_DISTANCE_TOWN.getPath());
		int searchDistance = 0;
		int minDistance = Integer.MAX_VALUE;
		for(KonKingdom kingdom : kingdomMap.values()) {
			searchDistance = Konquest.chunkDistance(loc, kingdom.getCapital().getCenterLoc());
			if(searchDistance != -1 && min_distance_capital > 0 && searchDistance < min_distance_capital) {
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
			if(searchDistance != -1 && min_distance_town > 0 && searchDistance < min_distance_town) {
				ChatUtil.printDebug("Failed to add town, too close to sanctuary "+sanctuary.getName());
				return 2;
			}
			if(searchDistance != -1 && searchDistance < minDistance) {
				minDistance = searchDistance;
			}
		}
		// Verify max distance
		int maxLimit = konquest.getConfigManager().getConfig("core").getInt(CorePath.TOWNS_MAX_DISTANCE_ALL.getPath(),0);
		if(minDistance != 0 && maxLimit > 0 && minDistance > maxLimit) {
			ChatUtil.printDebug("Failed to add town, too far from other towns and capitals");
			return 3;
		}
		// Verify no overlapping init chunks
		int radius = konquest.getConfigManager().getConfig("core").getInt(CorePath.TOWNS_INIT_RADIUS.getPath(),2);
		if(radius < 1) {
			radius = 1;
		}
		ChatUtil.printDebug("Checking for chunk conflicts with radius "+radius);
		for(Point point : konquest.getAreaPoints(loc, radius)) {
			if(territoryManager.isChunkClaimed(point,loc.getWorld())) {
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
        			ChatUtil.printDebug("Settling occupant player is in a vehicle, type "+occupant.getBukkitPlayer().getVehicle().getType().toString());
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
	
	// This method directly adds a kingdom without doing any checks (override)
	private void addKingdom(Location loc, String name, KonMonumentTemplate template) {
		kingdomMap.put(name, new KonKingdom(loc, name, konquest));
		kingdomMap.get(name).setMonumentTemplate(template); // template can be null
		kingdomMap.get(name).initCapital();
		kingdomMap.get(name).getCapital().updateBarPlayers();
		updateSmallestKingdom();
		territoryManager.addAllTerritory(loc.getWorld(),kingdomMap.get(name).getCapital().getChunkList());
		konquest.getMapHandler().drawDynmapUpdateTerritory(kingdomMap.get(name).getCapital());
	}
	
	/**
	 * Creates a new kingdom.
	 * The new kingdom must perform checks for creating the capital, similar to a new town.
	 * The isAdmin argument controls whether the kingdom is admin operated (true) or player operated (false).
	 * When the kingdom is player operated:
	 * The player must be a barbarian, and is made the master of the kingdom;
	 * The player must pay favor to create the kingdom.
	 * When the kingdom is admin operated:
	 * There is no cost to create, and there is no kingdom master.
	 * 
	 * @param loc - The center location of the kingdom, used for capital
	 * @param name - Kingdom name
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
	 *         -1	- Internal error
	 */
	public int createKingdom(Location centerLocation, String kingdomName, String templateName, KonPlayer master, boolean isAdmin) {
		Player bukkitPlayer = master.getBukkitPlayer();
		// Verify name
		if(konquest.validateNameConstraints(kingdomName) != 0) {
			return 1;
		}
		// Verify player is barbarian
		if(!isAdmin && !master.isBarbarian()) {
			return 2;
		}
		// Verify player can cover cost
		if(!isAdmin && costCreate > 0 && KonquestPlugin.getBalance(bukkitPlayer) < costCreate) {
			return 3;
    	}
		// Verify valid monument template
		if(!konquest.getSanctuaryManager().isValidTemplate(templateName)) {
			return 4;
		}
		
		// Verify position
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
		
		KonMonumentTemplate template = konquest.getSanctuaryManager().getTemplate(templateName);
		addKingdom(centerLocation, kingdomName, template);
		KonKingdom newKingdom = getKingdom(kingdomName);
		if(newKingdom != null) {
			if(isAdmin) {
				// This kingdom is operated by admins only
				newKingdom.setIsAdminOperated(true);
			} else {
				// This kingdom is operated by players
				newKingdom.setIsAdminOperated(false);
				// Withdraw cost
				if(costCreate > 0 && KonquestPlugin.withdrawPlayer(bukkitPlayer, costCreate)) {
		            konquest.getAccomplishmentManager().modifyPlayerStat(master,KonStatsType.FAVOR,(int)costCreate);
				}
				// Move player(s) out of center chunk (monument)
				//teleportAwayFromCenter(newKingdom.getCapital());
				// Assign player to kingdom
				assignPlayerKingdom(bukkitPlayer.getUniqueId(), kingdomName, true);
				// Make player master of kingdom
				newKingdom.forceMaster(bukkitPlayer.getUniqueId());
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
			// Exile all members
			for(KonOfflinePlayer member : konquest.getPlayerManager().getAllPlayersInKingdom(kingdom)) {
				exilePlayerBarbarian(member.getOfflineBukkitPlayer().getUniqueId(),false,false,true);
			}
			// Remove all towns
			for(KonTown town : kingdom.getTowns()) {
				removeTown(town.getName(),kingdom.getName());
			}
			kingdom = null;
			KonKingdom oldKingdom = kingdomMap.remove(name);
			if(oldKingdom != null) {
				// Remove capital
				oldKingdom.getCapital().removeAllBarPlayers();
				territoryManager.removeAllTerritory(oldKingdom.getCapital().getWorld(),oldKingdom.getCapital().getChunkList().keySet());
				konquest.getMapHandler().drawDynmapRemoveTerritory(oldKingdom.getCapital());
				oldKingdom = null;
				ChatUtil.printDebug("Removed Kingdom "+name);
				return true;
			}
		}
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
		oldKingdom = null;
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
	
	/**
	 * Changes a kingdom's monument template and updates all towns/capital
	 * @param kingdomName
	 * @param template
	 * @param player
	 * @param ignoreCost
	 * @return Status code
	 * 			0 - Success
	 * 			1 - Unknown kingdom name
	 * 			2 - Invalid template
	 * 			3 - Not enough favor
	 */
	public int changeKingdomTemplate(String kingdomName, KonMonumentTemplate template, KonPlayer player, boolean ignoreCost) {
		// Verify kingdom exists
		if(!isKingdom(kingdomName)) {
			return 1;
		}
		// Verify template is valid
		if(template == null || !template.isValid()) {
			return 2;
		}
		Player bukkitPlayer = player.getBukkitPlayer();
		// Check cost
		if(!ignoreCost && costTemplate > 0 && KonquestPlugin.getBalance(bukkitPlayer) < costTemplate) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(costTemplate));
            return 3;
    	}
		// Update template
		KonKingdom kingdom = getKingdom(kingdomName);
		kingdom.updateMonumentTemplate(template);
		// Withdraw cost
		if(!ignoreCost && costTemplate > 0 && KonquestPlugin.withdrawPlayer(bukkitPlayer, costTemplate)) {
            konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)costTemplate);
		}
		return 0;
	}
	
	/*
	 * =================================================
	 * Player Assignment Methods
	 * =================================================
	 * 1. Assign player to kingdom
	 * 		Player can be barbarian or kingdom member
	 * 2. Exile player to barbarians
	 * 		Player MUST be kingdom member
	 * 
	 * Players have two states: Barbarian or Kingdom Member.
	 * When a player wants to join a kingdom, the assignment method handles any 
	 * previous kingdom and joins the player to the new one.
	 * When a player wants to just leave a kingdom and have no kingdom, they become 
	 * barbarian by the exile method.
	 */
	
	
	/**
	 * Primary method for adding a player member to a kingdom.
	 * Assign a player to a kingdom and teleport them to the capital spawn point.
	 * Optionally checks for join permissions based on Konquest configuration.
	 * Optionally enforces maximum kingdom membership difference based on Konquest configuration.
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
	 *  			8 	- Unknown player ID
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
		 * 		Updates smallest kingdom
		 * 		Updates offline protections
		 * 		Updates border particles for player
		 * 		
		 */
    	
		// Verify kingdom exists
		if(!isKingdom(kingdomName)) {
			return 1;
		}
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
				return 8;
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
		// Check for player that's already a kingdom master
		for(KonKingdom kingdom : getKingdoms()) {
			if(kingdom.isMaster(id)) {
				return 6;
			}
		}
		// These checks may be bypassed when forced
		if(!force) {
			// Check for permission (online only)
			boolean isPerKingdomJoin = konquest.getConfigManager().getConfig("core").getBoolean(CorePath.KINGDOMS_PER_KINGDOM_JOIN_PERMISSIONS.getPath(),false);
			if(isOnline && isPerKingdomJoin) {
				String permission = "konquest.join."+getKingdom(kingdomName).getName().toLowerCase();
				if(!onlinePlayer.getBukkitPlayer().hasPermission(permission)) {
					return 3;
				}
			}
			// Check switching criteria (online only)
			boolean isAllowSwitch = konquest.getConfigManager().getConfig("core").getBoolean(CorePath.KINGDOMS_ALLOW_EXILE_SWITCH.getPath(),false);
			if(isOnline && !joinKingdom.equals(onlinePlayer.getExileKingdom()) && !getBarbarians().equals(onlinePlayer.getExileKingdom()) && !isAllowSwitch) {
				return 7;
			}
			// Check if max_player_diff is disabled, or if the desired kingdom is within the max diff (both online & offline)
			int config_max_player_diff = konquest.getConfigManager().getConfig("core").getInt(CorePath.KINGDOMS_MAX_PLAYER_DIFF.getPath(),0);
			int smallestKingdomPlayerCount = 0;
			int targetKingdomPlayerCount = 0;
			if(config_max_player_diff != 0) {
				// Find smallest kingdom, compare player count to this kingdom's
				String smallestKingdomName = getSmallestKingdomName();
				smallestKingdomPlayerCount = konquest.getPlayerManager().getAllPlayersInKingdom(smallestKingdomName).size();
				targetKingdomPlayerCount = konquest.getPlayerManager().getAllPlayersInKingdom(kingdomName).size();
			}
			if(config_max_player_diff != 0 && !(config_max_player_diff != 0 && targetKingdomPlayerCount < (smallestKingdomPlayerCount+config_max_player_diff))) {
				return 2;
			}
		}
		
		/* Passed all checks */
		
		// Fire event (online only)
		if(isOnline) {
			KonquestPlayerKingdomEvent invokeEvent = new KonquestPlayerKingdomEvent(konquest, onlinePlayer, joinKingdom, onlinePlayer.getExileKingdom());
			Konquest.callKonquestEvent(invokeEvent);
			if(invokeEvent.isCancelled()) {
				return 4;
			}
		}
		// Remove player from any enemy towns (both online & offline)
		OfflinePlayer townPlayer = isOnline ? onlinePlayer.getOfflineBukkitPlayer() : offlinePlayer.getOfflineBukkitPlayer();
		for(KonKingdom kingdom : kingdomMap.values()) {
			if(!kingdom.equals(joinKingdom)) {
				for(KonTown town : kingdom.getTowns()) {
		    		if(town.removePlayerResident(townPlayer)) {
		    			konquest.getMapHandler().drawDynmapLabel(town);
		    			if(isOnline) {
		    				ChatUtil.sendNotice(onlinePlayer.getBukkitPlayer(), MessagePath.COMMAND_EXILE_NOTICE_TOWN.getMessage(town.getName()));
		    			}
		    		}
		    	}
			}
		}
		// Remove any barbarian camps
		konquest.getCampManager().removeCamp(id.toString());
		// Remove membership from old kingdom
		boolean removeStatus = playerKingdom.removeMember(id);
		if(!removeStatus) {
    		ChatUtil.printDebug("Failed to remove member "+id.toString()+" from kingdom "+playerKingdom.getName());
    		return -1;
    	}
		// Add membership to new kingdom
    	boolean memberStatus = joinKingdom.addMember(id, false);
    	if(!memberStatus) {
    		ChatUtil.printDebug("Failed to add member "+id.toString()+" to kingdom "+joinKingdom.getName());
    		return -1;
    	}
    	
    	if(isOnline) {
    		onlinePlayer.setKingdom(joinKingdom);
    		onlinePlayer.setExileKingdom(joinKingdom);
    		onlinePlayer.setBarbarian(false);
        	// Teleport to capital
        	Location spawn = joinKingdom.getCapital().getSpawnLoc();
        	konquest.telePlayerLocation(onlinePlayer.getBukkitPlayer(), spawn); // the teleport should get caught by the movement listener and handle territory bars
        	onlinePlayer.getBukkitPlayer().setBedSpawnLocation(spawn, true);
        	// Updates
        	joinKingdom.getCapital().addBarPlayer(onlinePlayer);
        	updateKingdomOfflineProtection();
        	konquest.updateNamePackets(onlinePlayer);
        	territoryManager.updatePlayerBorderParticles(onlinePlayer);
        	if(!force) {
        		konquest.getDirectiveManager().displayBook(onlinePlayer);
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
	 * (Optionally) Resets their exileKingdom to Barbarians, making them look like a new player to Konquest.
	 * 
	 * @param player The player to exile
	 * @param teleport Teleport the player based on Konquest configuration when true
	 * @param clearStats Remove all player stats and prefix when true
	 * @param isFull Perform a full exile such that the player has no exile kingdom, like they just joined the server
	 * @return status
	 * 				0	- success
	 * 				1	- Player is already a barbarian
	 * 				2	- Invalid world
	 * 				3	- Failed to find valid teleport location
	 * 				4	- cancelled by event
	 * 				5	- Exile denied, player is a kingdom master
	 */
	public int exilePlayerBarbarian(UUID id, boolean teleport, boolean clearStats, boolean isFull) {
		
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
				return 7;
			}
		} else {
			// Found an online player
			isOnline = true;
		}
		// Check for barbarian player
		boolean isPlayerBarbarian = isOnline ? onlinePlayer.isBarbarian() : offlinePlayer.isBarbarian();
		if(isPlayerBarbarian) {
    		return 1;
    	}
		// Check for valid world (online only)
		if(isOnline && !konquest.isWorldValid(onlinePlayer.getBukkitPlayer().getLocation().getWorld())) {
	    	return 2;
		}
		// Check for player that's already a kingdom master
		for(KonKingdom kingdom : getKingdoms()) {
			if(kingdom.isMaster(id)) {
				return 5;
			}
		}
		
    	KonKingdom oldKingdom = isOnline ? onlinePlayer.getKingdom() : offlinePlayer.getKingdom();
    	
    	// Fire event (online and offline)
		KonquestPlayerExileEvent invokeEvent = new KonquestPlayerExileEvent(konquest, offlinePlayer, oldKingdom);
		Konquest.callKonquestEvent(invokeEvent);
		if(invokeEvent.isCancelled()) {
			return 4;
		}
    	// Try to teleport the player (online only)
    	if(isOnline && teleport) {
    		boolean doWorldSpawn = konquest.getConfigManager().getConfig("core").getBoolean(CorePath.EXILE_TELEPORT_WORLD_SPAWN.getPath(), false);
    		boolean doWildTeleport = konquest.getConfigManager().getConfig("core").getBoolean(CorePath.EXILE_TELEPORT_WILD.getPath(), true);
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
    	boolean doRemoveStats = konquest.getConfigManager().getConfig("core").getBoolean(CorePath.EXILE_REMOVE_STATS.getPath(), true);
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
    	
    	// Remove membership from old kingdom
		boolean removeStatus = oldKingdom.removeMember(id);
		if(!removeStatus) {
    		ChatUtil.printDebug("Failed to remove member "+id.toString()+" from kingdom "+oldKingdom.getName());
    		return -1;
    	}
    	
    	KonKingdom exileKingdom = isFull ? getBarbarians() : oldKingdom;
    	if(isOnline) {
    		// Force into global chat mode
    		onlinePlayer.setIsGlobalChat(true);
        	// Make into barbarian
    		onlinePlayer.setKingdom(getBarbarians());
    		onlinePlayer.setExileKingdom(exileKingdom);
    		onlinePlayer.setBarbarian(true);
    		// Updates
    		konquest.updateNamePackets(onlinePlayer);
    		territoryManager.updatePlayerBorderParticles(onlinePlayer);
    	} else {
    		offlinePlayer.setKingdom(getBarbarians());
    		offlinePlayer.setExileKingdom(exileKingdom);
    		offlinePlayer.setBarbarian(true);
    		konquest.getDatabaseThread().getDatabase().setOfflinePlayer(offlinePlayer);
    	}
    	// Remove residencies and refresh title bars, AFTER change to barbarian
    	OfflinePlayer townPlayer = isOnline ? onlinePlayer.getOfflineBukkitPlayer() : offlinePlayer.getOfflineBukkitPlayer();
    	for(KonTown town : oldKingdom.getTowns()) {
    		if(town.removePlayerResident(townPlayer)) {
    			konquest.getMapHandler().drawDynmapLabel(town);
    			if(isOnline) {
    				ChatUtil.sendNotice(onlinePlayer.getBukkitPlayer(), MessagePath.COMMAND_EXILE_NOTICE_TOWN.getMessage(town.getName()));
    			}
    		}
    		town.updateBarPlayers();
    	}
    	// Common updates
    	konquest.getMapHandler().drawDynmapLabel(oldKingdom.getCapital());
    	updateSmallestKingdom();
    	updateKingdomOfflineProtection();
    	
    	return 0;
	}
	
	/*
	 * ===================================
	 * Membership Methods
	 * ===================================
	 * These methods are used by commands for membership management.
	 * Players requesting/invited to join, kicking members, promotions/demotions, etc.
	 */
	//TODO: KR Replace GUILD command messages with KINGDOM
	
	/**
	 * Online Player requests to join the kingdom to the officers.
	 * This method is meant to be called via the GUI menu.
	 * When the kingdom is closed, a join request is sent to officers.
	 * When open, the player instantly joins.
	 * @param player - The online player requesting to join the kingdom
	 * @param kingdom - The target kingdom that the player requests to join
	 */
	public void joinKingdomRequest(KonPlayer player, KonKingdom kingdom) {
		
		//TODO: KR Refactor this to return codes, and do sounds at higher level in menu classes.
		
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
	public boolean respondKingdomRequest(OfflinePlayer player, KonKingdom kingdom, boolean resp) {
		if(kingdom == null) {
			return true;
		}
		UUID id = player.getUniqueId();
		
		if(kingdom.isJoinRequestValid(id)) {
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
		}
		
		return true;
	}
	
	/**
	 * Officer invites (offline) player to join (with /kingdom add command).
	 * Allow invites to players already in another kingdom.
	 * Inviting a player who has already requested to join always lets them join.
	 * @param player - The offline player that the officer invites to join their kingdom
	 * @param kingdom - The target kingdom for the join invite
	 * @return Error code:  0 - Success
	 * 						1 - Player is already a member
	 * 						2 - Player has already been invited
	 * 						-1 - Internal error
	 */
	public int joinKingdomInvite(OfflinePlayer player, KonKingdom kingdom) {
		if(kingdom == null) {
			return -1;
		}
		UUID id = player.getUniqueId();
		
		// Cannot invite a member of this kingdom
		if(kingdom.isMember(id)) {
			return 1;
		}
		
		if(kingdom.isJoinRequestValid(id)) {
			// There is already a valid request, add the player to the kingdom
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
	 * @param resp - True to accept, false to decline invite
	 */
	public boolean respondKingdomInvite(KonPlayer player, KonKingdom kingdom, boolean resp) {
		if(kingdom == null) {
			return false;
		}
		UUID id = player.getBukkitPlayer().getUniqueId();
		
		// Cannot accept invite from a member of this kingdom
		if(kingdom.isMember(id)) {
			return false;
		}
		
		if(kingdom.isJoinInviteValid(id)) {
			if(resp) {
				int status = assignPlayerKingdom(id,kingdom.getName(),false);
				if(status != 0) {
					return false;
				}
				
				kingdom.removeJoinRequest(id);
				ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_GUILD_NOTICE_JOINED.getMessage(kingdom.getName()));
				Konquest.playSuccessSound(player.getBukkitPlayer());
				return true;
			} else {
				// Denied join request
				kingdom.removeJoinRequest(id);
				ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_GUILD_NOTICE_INVITE_DECLINE.getMessage(kingdom.getName()));
			}
		}
		
		return false;
	}
	
	/*
	public void leaveKingdom(KonPlayer player, KonKingdom kingdom) {
		if(kingdom != null) {
			UUID id = player.getBukkitPlayer().getUniqueId();
			boolean status = removeMember(kingdom, id);
			if(status) {
				ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_GUILD_NOTICE_LEAVE.getMessage(kingdom.getName()));
				Konquest.playSuccessSound(player.getBukkitPlayer());
			} else {
				ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.COMMAND_GUILD_ERROR_LEAVE_FAIL.getMessage());
				Konquest.playFailSound(player.getBukkitPlayer());
			}
		}
	}
	
	public boolean kickKingdomMember(OfflinePlayer player, KonKingdom kingdom) {
		boolean result = false;
		if(kingdom != null) {
			UUID id = player.getUniqueId();
			result = removeMember(kingdom, id);
		}
		return result;
	}
	*/
	
	/**
	 * Removes a kingdom member and handles master transfers.
	 * If player is master, attempts to transfer master to officers first, then members.
	 * If no other members than master, removes the kingdom.
	 * @param player
	 * @param kingdom
	 */
	private boolean removeKingdomMember(OfflinePlayer player, KonKingdom kingdom) {
		boolean result = false;
		UUID id = player.getUniqueId();
		if(kingdom != null) {
			// Found kingdom where target player is a member
			if(kingdom.isMaster(id)) {
				// Player is master, transfer if possible
				List<OfflinePlayer> officers = kingdom.getPlayerOfficersOnly();
				if(!officers.isEmpty()) {
					// Make the first officer into the master
					transferMaster(officers.get(0),kingdom);
					// Now remove the player
					result = kingdom.removeMember(id);
				} else {
					// There are no officers
					List<OfflinePlayer> members = kingdom.getPlayerMembersOnly();
					if(!members.isEmpty()) {
						// Make the first member into the master
						transferMaster(members.get(0),kingdom);
						// Now remove the player
						result = kingdom.removeMember(id);
					} else {
						// There are no members to transfer master to, remove the kingdom
						String name = kingdom.getName();
						removeKingdom(name);
					}
				}
			} else {
				// Player is not the master, remove
				result = kingdom.removeMember(id);
			}
		}
		return result;
	}
	
	private boolean addKingdomMember(OfflinePlayer player, KonKingdom kingdom, boolean isOfficer) {
		UUID id = player.getUniqueId();
		return kingdom.addMember(id, isOfficer);
	}
	
	public boolean promoteOfficer(OfflinePlayer player, KonKingdom kingdom) {
		boolean result = false;
		UUID id = player.getUniqueId();
		if(!kingdom.isOfficer(id)) {
			if(kingdom.setOfficer(id, true)) {
				broadcastMembers(kingdom,MessagePath.COMMAND_GUILD_BROADCAST_PROMOTE.getMessage(player.getName(),kingdom.getName()));
				result = true;
			}
		}
		return result;
	}
	
	public boolean demoteOfficer(OfflinePlayer player, KonKingdom kingdom) {
		boolean result = false;
		UUID id = player.getUniqueId();
		if(kingdom.isOfficer(id)) {
			if(kingdom.setOfficer(id, false)) {
				broadcastMembers(kingdom,MessagePath.COMMAND_GUILD_BROADCAST_DEMOTE.getMessage(player.getName(),kingdom.getName()));
				result = true;
			}
		}
		return result;
	}
	
	public void transferMaster(OfflinePlayer player, KonKingdom kingdom) {
		UUID id = player.getUniqueId();
		if(kingdom.isMember(id)) {
			if(kingdom.setMaster(id)) {
				broadcastMembers(kingdom,MessagePath.COMMAND_GUILD_BROADCAST_TRANSFER.getMessage(player.getName(),kingdom.getName()));
			}
		}
	}
	
	private void broadcastMembers(KonKingdom kingdom, String message) {
		if(kingdom != null) {
			for(OfflinePlayer offlinePlayer : kingdom.getPlayerMembers()) {
				if(offlinePlayer.isOnline()) {
					Player player = (Player)offlinePlayer;
					ChatUtil.sendNotice(player, message);
				}
			}
		}
	}
	
	private void broadcastOfficers(KonKingdom kingdom, String message) {
		if(kingdom != null) {
			for(OfflinePlayer offlinePlayer : kingdom.getPlayerOfficers()) {
				if(offlinePlayer.isOnline()) {
					Player player = (Player)offlinePlayer;
					ChatUtil.sendNotice(player, message);
				}
			}
		}
	}
	
	/*
	private boolean removeMember(KonKingdom kingdom, UUID id) {
		return kingdom.removeMember(id);
	}
	
	private boolean addMember(KonKingdom kingdom, UUID id, boolean isOfficer) {
		return kingdom.addMember(id, isOfficer);
	}
	*/
	
	/*
	 * 
	 * Town Methods
	 * 
	 */
	
	
	/**
	 * addTown - Primary method for adding a town
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
	public int addTown(Location loc, String name, String kingdomName) {
		ChatUtil.printDebug("Attempting to add new town "+name+" for kingdom "+kingdomName);
		
		// Verify name
		if(konquest.validateNameConstraints(name) != 0) {
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
		if(!getKingdom(kingdomName).getMonumentTemplate().isValid()) {
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
				//updateTerritoryCache();
				territoryManager.addAllTerritory(loc.getWorld(),getKingdom(kingdomName).getTown(name).getChunkList());
				konquest.getMapHandler().drawDynmapUpdateTerritory(getKingdom(kingdomName).getTown(name));

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
	 * @param name
	 * @param kingdomName
	 * @return
	 */
	public boolean removeTown(String name, String kingdomName) {
		if(isKingdom(kingdomName) && getKingdom(kingdomName).hasTown(name)) {
			KonKingdom kingdom = getKingdom(kingdomName);
			KonTown town = kingdom.getTown(name);
			ArrayList<Point> townPoints = new ArrayList<Point>();
			townPoints.addAll(town.getChunkList().keySet());
			clearAllTownNerfs(town);
			clearAllTownHearts(town);
			town.removeAllBarPlayers();
			if(kingdom.removeTown(name)) {
				// When a town is removed successfully, update the chunk cache
				//updateTerritoryCache();
				territoryManager.removeAllTerritory(town.getWorld(),townPoints);
				konquest.getMapHandler().drawDynmapRemoveTerritory(town);
				konquest.getMapHandler().drawDynmapLabel(town.getKingdom().getCapital());
				konquest.getIntegrationManager().getQuickShop().deleteShopsInPoints(townPoints,town.getWorld());
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * Primary method for renaming a town
	 * @param oldName
	 * @param newName
	 * @param kingdomName
	 * @return
	 */
	public boolean renameTown(String oldName, String newName, String kingdomName) {
		if(isKingdom(kingdomName) && getKingdom(kingdomName).hasTown(oldName)) {
			KonKingdom kingdom = getKingdom(kingdomName);
			konquest.getMapHandler().drawDynmapRemoveTerritory(kingdom.getTown(oldName));
			boolean success = kingdom.renameTown(oldName, newName);
			if (success) {
				KonTown town = kingdom.getTown(newName);
				if (town != null) {
					konquest.getMapHandler().drawDynmapUpdateTerritory(town);
				}
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Primary method for transferring ownership of a town between kingdoms
	 * @param name - Town name
	 * @param oldKingdomName - Kingdom name of conquered Town
	 * @param conquerPlayer - Player who initiated the conquering
	 * @return true if all names exist, else false
	 */
	public boolean captureTownForPlayer(String name, String oldKingdomName, KonquestPlayer conquerPlayerArg) {
		if(conquerPlayerArg == null) {
			return false;
		}
		if(!(conquerPlayerArg instanceof KonPlayer)) {
			return false;
		}
		KonPlayer conquerPlayer = (KonPlayer)conquerPlayerArg;
		
		if(conquerPlayer.isBarbarian()) {
			return false;
		}
		if(captureTown(name, oldKingdomName, conquerPlayer.getKingdom())) {
			conquerPlayer.getKingdom().getTown(name).setPlayerLord(conquerPlayer.getOfflineBukkitPlayer());
			return true;
		}
		return false;
	}
	
	public boolean captureTown(String name, String oldKingdomName, KonKingdom conquerKingdom) {
		if(!conquerKingdom.getMonumentTemplate().isValid()) {
			return false;
		}
		boolean captureUpgrades = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.capture_upgrades",true);
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
			return true;
		}
		return false;
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
		ArrayList<String> names = new ArrayList<String>(kingdomMap.keySet());
		return names;
	}
	
	public ArrayList<KonKingdom> getKingdoms() {
		ArrayList<KonKingdom> kingdoms = new ArrayList<KonKingdom>(kingdomMap.values());
		return kingdoms;
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
		boolean isKingdom = false;
		// Check for exact String key match
		isKingdom = kingdomMap.containsKey(name);
		if(isKingdom) {
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
	
	public boolean isTown(String name) {
		for(KonKingdom kingdom : kingdomMap.values()) {
			if(kingdom.hasTown(name)) {
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
	 * @param action
	 * @param town
	 * @param bukkitPlayer
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
				int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonquestUpgrade.FATIGUE);
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
	
	public Set<PotionEffectType> getTownNerfs() {
		return townNerfs.keySet();
	}
	
	public boolean isTownNerf(PotionEffectType pot) {
		return townNerfs.containsKey(pot);
	}
	
	public void applyTownHearts(KonPlayer player, KonTown town) {
		if(player.getKingdom().equals(town.getKingdom())) {
			int upgradeLevel = konquest.getUpgradeManager().getTownUpgradeLevel(town, KonquestUpgrade.HEALTH);
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
		//double defaultValue = player.getBukkitPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue();
		//player.getBukkitPlayer().getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(defaultValue);
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
		boolean isOfflineProtectedEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.no_enemy_edit_offline",true);
		int offlineProtectedWarmupSeconds = konquest.getConfigManager().getConfig("core").getInt("core.kingdoms.no_enemy_edit_offline_warmup",0);
		int offlineProtectedMinimumPlayers = konquest.getConfigManager().getConfig("core").getInt("core.kingdoms.no_enemy_edit_offline_minimum",0);
		if(isOfflineProtectedEnabled) {
			// For each kingdom, start protection warmup timer if no players are online, else ensure the timer is stopped
			for(KonKingdom kingdom : getKingdoms()) {
				ArrayList<KonPlayer> onlineKingdomPlayers = konquest.getPlayerManager().getPlayersInKingdom(kingdom.getName());
				int numOnlineKingdomPlayers = onlineKingdomPlayers.size();
				if((offlineProtectedMinimumPlayers <= 0 && onlineKingdomPlayers.isEmpty()) || (numOnlineKingdomPlayers < offlineProtectedMinimumPlayers)) {
					Timer protectedWarmupTimer = kingdom.getProtectedWarmupTimer();
					if(offlineProtectedWarmupSeconds > 0 && protectedWarmupTimer.getTime() == -1 && !kingdom.isOfflineProtected()) {
						// start timer
						ChatUtil.printDebug("Starting kingdom protection warmup timer for "+offlineProtectedWarmupSeconds+" seconds: "+kingdom.getName());
						protectedWarmupTimer.stopTimer();
						protectedWarmupTimer.setTime(offlineProtectedWarmupSeconds);
						protectedWarmupTimer.startTimer();
						int warmupMinutes = offlineProtectedWarmupSeconds / 60;
						int warmupSeconds = offlineProtectedWarmupSeconds % 60;
						String warmupTime = String.format("%d:%02d", warmupMinutes , warmupSeconds);
						//ChatUtil.sendBroadcast(ChatColor.LIGHT_PURPLE+"The Kingdom of "+ChatColor.RED+kingdom.getName()+ChatColor.LIGHT_PURPLE+" will be protected in "+ChatColor.AQUA+warmupTime+ChatColor.LIGHT_PURPLE+" minutes.");
						ChatUtil.sendBroadcast(ChatColor.LIGHT_PURPLE+MessagePath.PROTECTION_NOTICE_KINGDOM_WARMUP.getMessage(kingdom.getName(),warmupTime));
					}
				} else {
					// stop timer, clear protection
					//ChatUtil.printDebug("Clearing kingdom protection: "+kingdom.getName());
					Timer protectedWarmupTimer = kingdom.getProtectedWarmupTimer();
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
		List<KonTown> townNames = new ArrayList<KonTown>();
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
	
	public List<KonTown> getPlayerKnightTowns(KonquestOfflinePlayer player) {
		List<KonTown> townNames = new ArrayList<KonTown>();
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
	
	public List<KonTown> getPlayerResidenceTowns(KonquestOfflinePlayer player) {
		List<KonTown> townNames = new ArrayList<KonTown>();
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
	
	public KonLeaderboard getKingdomLeaderboard(KonKingdom kingdom) {
		KonLeaderboard leaderboard = new KonLeaderboard();
		if(kingdom.equals(barbarians)) {
			return leaderboard;
		}
		// Determine scores for all players within towns
		HashMap<OfflinePlayer,KonPlayerScoreAttributes> memberScores = new HashMap<OfflinePlayer,KonPlayerScoreAttributes>();
		int numTownLords = 0;
		int numTownKnights = 0;
		int numTownResidents = 0;
		int numLordLand = 0;
		int numKnightLand = 0;
		int numResidentLand = 0;
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
	
	public KonKingdomScoreAttributes getKingdomScoreAttributes(KonKingdom kingdom) {
		KonKingdomScoreAttributes scoreAttributes = new KonKingdomScoreAttributes();
		if(!kingdom.equals(barbarians) && !kingdom.isPeaceful()) {
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
	    	int cost_settle = (int)konquest.getConfigManager().getConfig("core").getDouble("core.favor.cost_settle");
	    	int cost_claim = (int)konquest.getConfigManager().getConfig("core").getDouble("core.favor.cost_claim");
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
		int score = getKingdomScoreAttributes(kingdom).getScore();
		return score;
	}
	
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
		int score = getPlayerScoreAttributes(offlinePlayer).getScore();
		return score;
	}
	
	
	
	public void updateAllTownDisabledUpgrades() {
		for(KonKingdom kingdom : getKingdoms()) {
			for(KonTown town : kingdom.getTowns()) {
				konquest.getUpgradeManager().updateTownDisabledUpgrades(town);
			}
		}
	}
	
	// Returns true when the player is still in cooldown, else false
	public boolean isPlayerJoinCooldown(Player player) {
		boolean result = false;
		// Check if player is currently in cooldown.
		// If cooldown has expired, remove it.
		UUID id = player.getUniqueId();
		if(joinPlayerCooldowns.containsKey(id)) {
			long endSeconds = joinPlayerCooldowns.get(id);
			Date now = new Date();
			if(now.after(new Date(endSeconds*1000))) {
				// The cooldown has expired
				joinPlayerCooldowns.remove(id);
			} else {
				// The player is still in cooldown
				result = true;
			}
		}
		return result;
	}
	
	public void applyPlayerJoinCooldown(Player player) {
		if(joinCooldownSeconds > 0) {
			UUID id = player.getUniqueId();
			int endTimeSeconds = (int)((new Date()).getTime()/1000) + joinCooldownSeconds;
			joinPlayerCooldowns.put(id, endTimeSeconds);
		}
	}
	
	public int getJoinCooldownRemainingSeconds(Player player) {
		int result = 0;
		UUID id = player.getUniqueId();
		if(joinPlayerCooldowns.containsKey(id)) {
			int endSeconds = joinPlayerCooldowns.get(id);
			Date now = new Date();
			if(now.before(new Date((long)endSeconds*1000))) {
				result = endSeconds - (int)(now.getTime()/1000);
			}
		}
		return result;
	}
	
	// Returns true when the player is still in cooldown, else false
	public boolean isPlayerExileCooldown(Player player) {
		boolean result = false;
		// Check if player is currently in cooldown.
		// If cooldown has expired, remove it.
		UUID id = player.getUniqueId();
		if(exilePlayerCooldowns.containsKey(id)) {
			long endSeconds = exilePlayerCooldowns.get(id);
			Date now = new Date();
			if(now.after(new Date(endSeconds*1000))) {
				// The cooldown has expired
				exilePlayerCooldowns.remove(id);
			} else {
				// The player is still in cooldown
				result = true;
			}
		}
		return result;
	}
	
	public void applyPlayerExileCooldown(Player player) {
		if(exileCooldownSeconds > 0) {
			UUID id = player.getUniqueId();
			int endTimeSeconds = (int)((new Date()).getTime()/1000) + exileCooldownSeconds;
			exilePlayerCooldowns.put(id, endTimeSeconds);
		}
	}
	
	public int getExileCooldownRemainingSeconds(Player player) {
		int result = 0;
		UUID id = player.getUniqueId();
		if(exilePlayerCooldowns.containsKey(id)) {
			int endSeconds = exilePlayerCooldowns.get(id);
			Date now = new Date();
			if(now.before(new Date((long)endSeconds*1000))) {
				result = endSeconds - (int)(now.getTime()/1000);
			}
		}
		return result;
	}
	
	public void loadJoinExileCooldowns() {
		joinCooldownSeconds = konquest.getConfigManager().getConfig("core").getInt("core.kingdoms.join_cooldown",0);
		exileCooldownSeconds = konquest.getConfigManager().getConfig("core").getInt("core.kingdoms.exile_cooldown",0);
		// Check any existing cooldowns, ensure less than config values.
		// Join cooldown map
		if(!joinPlayerCooldowns.isEmpty()) {
			HashMap<UUID,Integer> updateJoinMap = new HashMap<UUID,Integer>();
			if(joinCooldownSeconds > 0) {
				int nowSeconds = (int)((new Date()).getTime()/1000);
				int maxEndSeconds = nowSeconds + joinCooldownSeconds;
				for(UUID u : joinPlayerCooldowns.keySet()) {
					int endSeconds = joinPlayerCooldowns.get(u);
					if(endSeconds > maxEndSeconds) {
						// Existing cooldown is longer than new config setting, shorten it.
						updateJoinMap.put(u, maxEndSeconds);
					} else {
						// Existing cooldown is under config, preserve it.
						updateJoinMap.put(u, endSeconds);
					}
				}
			}
			joinPlayerCooldowns.clear();
			joinPlayerCooldowns.putAll(updateJoinMap);
		}
		// Exile cooldown map
		if(!exilePlayerCooldowns.isEmpty()) {
			HashMap<UUID,Integer> updateExileMap = new HashMap<UUID,Integer>();
			if(exileCooldownSeconds > 0) {
				int nowSeconds = (int)((new Date()).getTime()/1000);
				int maxEndSeconds = nowSeconds + exileCooldownSeconds;
				for(UUID u : exilePlayerCooldowns.keySet()) {
					int endSeconds = exilePlayerCooldowns.get(u);
					if(endSeconds > maxEndSeconds) {
						// Existing cooldown is longer than new config setting, shorten it.
						updateExileMap.put(u, maxEndSeconds);
					} else {
						// Existing cooldown is under config, preserve it.
						updateExileMap.put(u, endSeconds);
					}
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
		maxCriticalHits = konquest.getConfigManager().getConfig("core").getInt("core.monuments.destroy_amount",12);
		String townCriticalBlockTypeName = konquest.getConfigManager().getConfig("core").getString("core.monuments.critical_block","");
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
		isArmorBlockWhitelist = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.armor_blacklist_reverse",false);
		boolean isListEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.armor_blacklist_enable",false);
		if(isListEnabled) {
			List<String> armorList = konquest.getConfigManager().getConfig("core").getStringList("core.towns.armor_blacklist");
			Material mat = null;
			for(String entry : armorList) {
				try {
					mat = Material.valueOf(entry);
					if(mat != null && !armorBlocks.contains(mat)) {
						armorBlocks.add(mat);
					}
				} catch(IllegalArgumentException e) {
					ChatUtil.printConsoleError("Invalid armor list entry, "+entry+", ignoring.");
				}
			}
		}
	}
	
	private void loadKingdoms() {
		//ScheduledThreadPoolExecutor service = new ScheduledThreadPoolExecutor(1);
		FileConfiguration kingdomsConfig = konquest.getConfigManager().getConfig("kingdoms");
        if (kingdomsConfig.get("kingdoms") == null) {
        	ChatUtil.printDebug("There is no kingdoms section in kingdoms.yml");
            return;
        }
        boolean isShieldsEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.enable_shields",false);
        boolean isArmorsEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.towns.enable_armor",false);
        double x,y,z;
        float pitch,yaw;
        List<Double> sectionList;
        String worldName;
        String defaultWorldName = konquest.getConfigManager().getConfig("core").getString("core.world_name","world");
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
        	boolean isPeaceful = kingdomSection.getBoolean("peaceful", false);
        	ConfigurationSection monumentSection = kingdomsConfig.getConfigurationSection("kingdoms."+kingdomName+".monument");
        	// Create Kingdoms and Capitals
        	ConfigurationSection capitalSection = kingdomsConfig.getConfigurationSection("kingdoms."+kingdomName+".capital");
        	World capitalWorld = null;
        	if(capitalSection != null) {
        		worldName = capitalSection.getString("world",defaultWorldName);
        		capitalWorld = Bukkit.getWorld(worldName);
        		if(capitalWorld != null) {
        			// Create capital territory
	        		sectionList = capitalSection.getDoubleList("spawn");
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
		        	Location capital_spawn = new Location(capitalWorld,x,y,z,yaw,pitch);
		        	sectionList = capitalSection.getDoubleList("center");
	        		x = sectionList.get(0);
	        		y = sectionList.get(1);
	        		z = sectionList.get(2);
		        	Location capital_center = new Location(capitalWorld,x,y,z);
		        	// Create Kingdom and capital
		        	addKingdom(capital_center, kingdomName);
		        	// Set Peaceful mode
		        	kingdomMap.get(kingdomName).setPeaceful(isPeaceful);
		        	// Set Capital spawn point
		        	kingdomMap.get(kingdomName).getCapital().setSpawn(capital_spawn);
		        	// Add all Capital chunk claims
		        	kingdomMap.get(kingdomName).getCapital().addPoints(konquest.formatStringToPoints(capitalSection.getString("chunks")));
		        	// Update territory cache
		        	territoryManager.addAllTerritory(capitalWorld,kingdomMap.get(kingdomName).getCapital().getChunkList());
		        	// Create Monument Templates
		        	if(monumentSection != null) {
		        		sectionList = monumentSection.getDoubleList("travel");
		        		x = sectionList.get(0);
		        		y = sectionList.get(1);
		        		z = sectionList.get(2);
			        	Location monument_travel = new Location(capitalWorld,x,y,z);
			        	sectionList = monumentSection.getDoubleList("cornerone");
		        		x = sectionList.get(0);
		        		y = sectionList.get(1);
		        		z = sectionList.get(2);
			        	Location monument_cornerone = new Location(capitalWorld,x,y,z);
			        	sectionList = monumentSection.getDoubleList("cornertwo");
		        		x = sectionList.get(0);
		        		y = sectionList.get(1);
		        		z = sectionList.get(2);
			        	Location monument_cornertwo = new Location(capitalWorld,x,y,z);
			        	// Apply missing criticals, if any
			        	for(Location loc : konquest.formatStringToLocations(monumentSection.getString("criticals",""),capitalWorld)) {
			        		capitalWorld.getBlockAt(loc).setType(townCriticalBlock);
	            		}
			        	// Create a Monument Template region for current Kingdom, avoid saving
			        	// Creation of template will update critical locations within kingdom object
			        	
			        	//TODO: KR update this
		        		//int status = kingdomMap.get(kingdomName).createMonumentTemplate(monument_cornerone, monument_cornertwo, monument_travel, false);
			        	int status = 0;
		        		if(status != 0) {
		        			String message = "Failed to load Monument Template for Kingdom "+kingdomName+", ";
		        			switch(status) {
			        			case 1:
			        				message = message+"base dimensions are not 16x16 blocks.";
			        				break;
			        			case 2:
			        				message = message+"region does not contain enough critical blocks.";
			        				break;
			        			case 3:
			        				message = message+"region does not contain a travel point.";
			        				break;
			        			case 4:
			        				message = message+"region is not within Capital territory.";
			        				break;
		        				default:
		        					message = message+"unknown reason.";
		        					break;
		        			}
		        			ChatUtil.printConsoleError(message);
		        			konquest.opStatusMessages.add(message);
		        		}
		        	} else {
		        		ChatUtil.printConsoleError("Null monument template for Kingdom "+kingdomName+" in config file!");
		        		konquest.opStatusMessages.add("Missing monument template for Kingdom "+kingdomName+" in kingdoms.yml config file. Use \"/k admin monument\" to define the monument template for this Kingdom.");
		        	}
        		} else {
        			String message = "Failed to load territory "+kingdomName+" Capital in an unloaded world, "+worldName+". Check plugin load order.";
        			ChatUtil.printConsoleError(message);
        			konquest.opStatusMessages.add(message);
        		}
        	} else {
        		String message = "Failed to load capital for Kingdom "+kingdomName+", is one created?";
        		ChatUtil.printConsoleError(message);
        		konquest.opStatusMessages.add(message);
        	}
        	
        	// Load all towns
        	boolean isMissingMonuments = false;
        	for(String townName : kingdomsConfig.getConfigurationSection("kingdoms."+kingdomName+".towns").getKeys(false)) {
        		//ChatUtil.printDebug("Loading Town: "+townName);
            	ConfigurationSection townSection = kingdomsConfig.getConfigurationSection("kingdoms."+kingdomName+".towns."+townName);
            	if(townSection != null) {
            		worldName = townSection.getString("world",defaultWorldName);
            		World townWorld = Bukkit.getServer().getWorld(worldName);
            		if(townWorld != null) {
            			// Create town
	            		int base = townSection.getInt("base");
	            		sectionList = townSection.getDoubleList("spawn");
	            		x = sectionList.get(0);
	            		y = sectionList.get(1);
	            		z = sectionList.get(2);
		            	Location town_spawn = new Location(townWorld,x,y,z);
		            	sectionList = townSection.getDoubleList("center");
	            		x = sectionList.get(0);
	            		y = sectionList.get(1);
	            		z = sectionList.get(2);
		            	Location town_center = new Location(townWorld,x,y,z);
		            	// Create Town
		            	kingdomMap.get(kingdomName).addTown(town_center, townName);
		            	KonTown town = kingdomMap.get(kingdomName).getTown(townName);
		            	// Set town spawn point
		            	town.setSpawn(town_spawn);
		            	// Setup town monument parameters from template
		            	town.loadMonument(base, kingdomMap.get(kingdomName).getMonumentTemplate());
		            	if(!kingdomMap.get(kingdomName).getMonumentTemplate().isValid()) {
		            		isMissingMonuments = true;
		            		ChatUtil.printConsoleError("Failed to load monument for Town "+townName+" in kingdom "+kingdomName+" from invalid template");
		            	}
		            	// Add all Town chunk claims
		            	town.addPoints(konquest.formatStringToPoints(townSection.getString("chunks")));
		            	// Update territory cache
		            	territoryManager.addAllTerritory(townWorld,town.getChunkList());
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
		            		//ChatUtil.printDebug("Town "+townName+" Lord ID is: \""+lordUUID+"\"");
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
		            	} else {
		            		//ChatUtil.printDebug("Town "+townName+" does not have any stored residents");
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
		            			KonquestUpgrade upgrade = KonquestUpgrade.getUpgrade(upgradeName);
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
		            			HashSet<Point> points = new HashSet<Point>();
		            			points.addAll(konquest.formatStringToPoints(townSection.getString("plots."+plotIndex+".chunks")));
		            			ArrayList<UUID> users = new ArrayList<UUID>();
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
	            }
        	}
        	if(isMissingMonuments) {
        		konquest.opStatusMessages.add("Kingdom "+kingdomName+" has Towns with invalid Monuments. You must create a new Monument Template and restart the server.");
        	}
        }
		ChatUtil.printDebug("Loaded Kingdoms");
	}
	
	//TODO Save and update only items which have changed, do not delete everything and write all from memory
	public void saveKingdoms() {
		FileConfiguration kingdomsConfig = konquest.getConfigManager().getConfig("kingdoms");
		kingdomsConfig.set("kingdoms", null); // reset kingdoms config
		ConfigurationSection root = kingdomsConfig.createSection("kingdoms");
		for(KonKingdom kingdom : kingdomMap.values()) {
			ConfigurationSection kingdomSection = root.createSection(kingdom.getName());
			kingdomSection.set("peaceful",kingdom.isPeaceful());
            ConfigurationSection capitalSection = kingdomSection.createSection("capital");
            capitalSection.set("world", kingdom.getCapital().getWorld().getName());
            capitalSection.set("spawn", new int[] {(int) kingdom.getCapital().getSpawnLoc().getBlockX(),
												   (int) kingdom.getCapital().getSpawnLoc().getBlockY(),
												   (int) kingdom.getCapital().getSpawnLoc().getBlockZ(),
												   (int) kingdom.getCapital().getSpawnLoc().getPitch(),
												   (int) kingdom.getCapital().getSpawnLoc().getYaw()});
            capitalSection.set("center", new int[] {(int) kingdom.getCapital().getCenterLoc().getBlockX(),
					 								(int) kingdom.getCapital().getCenterLoc().getBlockY(),
					 								(int) kingdom.getCapital().getCenterLoc().getBlockZ()});
            capitalSection.set("chunks", konquest.formatPointsToString(kingdom.getCapital().getChunkList().keySet()));
            if(kingdom.getMonumentTemplate().isValid()) {
	            ConfigurationSection monumentSection = kingdomSection.createSection("monument");
	            monumentSection.set("travel", new int[] {(int) kingdom.getMonumentTemplate().getTravelPoint().getBlockX(),
														 (int) kingdom.getMonumentTemplate().getTravelPoint().getBlockY(),
														 (int) kingdom.getMonumentTemplate().getTravelPoint().getBlockZ()});
	            monumentSection.set("cornerone", new int[] {(int) kingdom.getMonumentTemplate().getCornerOne().getBlockX(),
						 								 	(int) kingdom.getMonumentTemplate().getCornerOne().getBlockY(),
						 								 	(int) kingdom.getMonumentTemplate().getCornerOne().getBlockZ()});
	            monumentSection.set("cornertwo", new int[] {(int) kingdom.getMonumentTemplate().getCornerTwo().getBlockX(),
						 								 	(int) kingdom.getMonumentTemplate().getCornerTwo().getBlockY(),
						 								 	(int) kingdom.getMonumentTemplate().getCornerTwo().getBlockZ()});
	            // TODO: KR update this
	            //monumentSection.set("criticals", konquest.formatLocationsToString(kingdom.getCriticals()));
			} else {
				ChatUtil.printConsoleError("Failed to save invalid monument template for Kingdom "+kingdom.getName());
			}
            ConfigurationSection townsSection = kingdomSection.createSection("towns");
            for(KonTown town : kingdom.getTowns()) {
            	ConfigurationSection townInstanceSection = townsSection.createSection(town.getName());
            	townInstanceSection.set("world", town.getWorld().getName());
            	townInstanceSection.set("base", town.getMonument().getBaseY());
            	townInstanceSection.set("spawn", new int[] {(int) town.getSpawnLoc().getBlockX(),
						 								 	(int) town.getSpawnLoc().getBlockY(),
						 								 	(int) town.getSpawnLoc().getBlockZ()});
            	townInstanceSection.set("center", new int[] {(int) town.getCenterLoc().getBlockX(),
						 									 (int) town.getCenterLoc().getBlockY(),
						 									 (int) town.getCenterLoc().getBlockZ()});
                townInstanceSection.set("chunks", konquest.formatPointsToString(town.getChunkList().keySet()));
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
                for(KonquestUpgrade upgrade : KonquestUpgrade.values()) {
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
		ChatUtil.printDebug("Saved Kingdoms");
	}
	
}
