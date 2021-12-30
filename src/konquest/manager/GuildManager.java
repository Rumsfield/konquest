package konquest.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.model.KonGuild;
import konquest.model.KonKingdom;
import konquest.model.KonOfflinePlayer;
import konquest.model.KonPlayer;
import konquest.model.KonStatsType;
import konquest.model.KonTown;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;
import konquest.utility.Timeable;
import konquest.utility.Timer;

public class GuildManager implements Timeable {

	private Konquest konquest;
	private boolean isEnabled;
	private long payIntervalSeconds;
	private double payPerChunk;
	private double payPerResident;
	private double payLimit;
	private int payPercentOfficer;
	private int payPercentMaster;
	private boolean isDiscountEnable;
	private int discountPercent;
    private boolean isDiscountStack;
	private double costSpecial;
	private double costRelation;
	private double costCreate;
	private double costRename;
	private Timer payTimer;
	private HashSet<KonGuild> guilds;
	private HashMap<UUID,KonGuild> playerGuildCache;
	
	//TODO: Specialization trade discounts
	/* - Check version to handle exceptions in MerchantRecipe special price methods
	 * 
	 */
	
	public GuildManager(Konquest konquest) {
		this.konquest = konquest;
		this.isEnabled = false;
		this.payIntervalSeconds = 900;
		this.payPerChunk = 0.25;
		this.payPerResident = 0.1;
		this.payLimit = 100;
		this.payPercentOfficer = 20;
		this.payPercentMaster = 80;
		this.isDiscountEnable = false;
		this.discountPercent = 0;
		this.isDiscountStack = false;
		this.costSpecial = 200;
		this.costRelation = 50;
		this.costCreate = 100;
		this.costRename = 50;
		this.payTimer = new Timer(this);
		this.guilds = new HashSet<KonGuild>();
		this.playerGuildCache = new HashMap<UUID,KonGuild>();
	}
	
	public void initialize() {
		isEnabled 			= konquest.getConfigManager().getConfig("core").getBoolean("core.guilds.enable",false);
		payIntervalSeconds 	= konquest.getConfigManager().getConfig("core").getLong("core.guilds.pay_interval_seconds");
		payPerChunk 		= konquest.getConfigManager().getConfig("core").getDouble("core.guilds.pay_per_chunk");
		payPerResident 		= konquest.getConfigManager().getConfig("core").getDouble("core.guilds.pay_per_resident");
		payLimit 			= konquest.getConfigManager().getConfig("core").getDouble("core.guilds.pay_limit");
		payPercentOfficer   = konquest.getConfigManager().getConfig("core").getInt("core.guilds.bonus_officer_percent");
		payPercentMaster    = konquest.getConfigManager().getConfig("core").getInt("core.guilds.bonus_master_percent");
		isDiscountEnable    = konquest.getConfigManager().getConfig("core").getBoolean("core.guilds.discount_enable",false);
		discountPercent 	= konquest.getConfigManager().getConfig("core").getInt("core.guilds.discount_percent");
		isDiscountStack 	= konquest.getConfigManager().getConfig("core").getBoolean("core.guilds.discount_stack");
		costCreate 	        = konquest.getConfigManager().getConfig("core").getDouble("core.favor.guilds.cost_create");
		costRename 	        = konquest.getConfigManager().getConfig("core").getDouble("core.favor.guilds.cost_rename");
		costSpecial 	    = konquest.getConfigManager().getConfig("core").getDouble("core.favor.guilds.cost_specialize");
		costRelation 	    = konquest.getConfigManager().getConfig("core").getDouble("core.favor.guilds.cost_relationship");
		
		payPercentOfficer = payPercentOfficer < 0 ? 0 : payPercentOfficer;
		payPercentOfficer = payPercentOfficer > 100 ? 100 : payPercentOfficer;
		payPercentMaster = payPercentMaster < 0 ? 0 : payPercentMaster;
		payPercentMaster = payPercentMaster > 100 ? 100 : payPercentMaster;
		discountPercent = discountPercent < 0 ? 0 : discountPercent;
		discountPercent = discountPercent > 100 ? 100 : discountPercent;
		
		if(isEnabled && payIntervalSeconds > 0) {
			payTimer.stopTimer();
			payTimer.setTime((int)payIntervalSeconds);
			payTimer.startLoopTimer();
		}
		
		loadGuilds();
		validateGuilds();
		ChatUtil.printDebug("Guild Manager is ready, enabled: "+isEnabled+" with "+guilds.size()+" guilds");
	}
	
	/*
	 * ===================================
	 * Getter Methods
	 * ===================================
	 */
	
	public boolean isEnabled() {
		return isEnabled;
	}
	
	public double getCostSpecial() {
		return costSpecial;
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
	
	/*
	 * ===================================
	 * Management Methods
	 * ===================================
	 */
	
	@Override
	public void onEndTimer(int taskID) {
		if(taskID == 0) {
			ChatUtil.printDebug("Guild Pay Timer ended with null taskID!");
		} else if(taskID == payTimer.getTaskID()) {
			ChatUtil.printDebug("Guild Pay timer completed a new cycle");
			disbursePayments();
		}
		
	}
	
	// Pays all guild members
	private void disbursePayments() {
		HashMap<OfflinePlayer,Double> payments = new HashMap<OfflinePlayer,Double>();
		HashMap<OfflinePlayer,KonGuild> memberships = new HashMap<OfflinePlayer,KonGuild>();
		HashMap<OfflinePlayer,KonGuild> masters = new HashMap<OfflinePlayer,KonGuild>();
		HashMap<OfflinePlayer,KonGuild> officers = new HashMap<OfflinePlayer,KonGuild>();
		HashMap<KonGuild,Double> totalPay = new HashMap<KonGuild,Double>();
		// Initialize payment table
		for(KonGuild guild : guilds) {
			totalPay.put(guild, 0.0);
			for(OfflinePlayer offlinePlayer : guild.getPlayerMembers()) {
				if(offlinePlayer.isOnline()) {
					payments.put(offlinePlayer, 0.0);
					memberships.put(offlinePlayer, guild);
					if(guild.isMaster(offlinePlayer.getUniqueId())) {
						masters.put(offlinePlayer,guild);
					} else if(guild.isOfficer(offlinePlayer.getUniqueId())) {
						officers.put(offlinePlayer,guild);
					}
				}
			}
		}
		// Determine pay amounts by town
		OfflinePlayer lord;
		KonGuild guild;
		int land;
		int pop;
		double pay;
		for(KonKingdom kingdom : konquest.getKingdomManager().getKingdoms()) {
			for(KonTown town : kingdom.getTowns()) {
				lord = town.getPlayerLord();
				land = town.getChunkList().size();
				pop = town.getNumResidents();
				if(payments.containsKey(lord)) {
					guild = memberships.get(lord);
					double guildPrev = totalPay.get(guild);
					double lordPrev = payments.get(lord);
					pay = (land*payPerChunk) + (pop*payPerResident);
					payments.put(lord, lordPrev+pay);
					totalPay.put(guild, guildPrev+pay);
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
	            	ChatUtil.sendNotice(player, "Received guild payment");
	            }
			}
		}
	}
	
	public void applyTradeDiscounts(KonPlayer player, KonTown town, Inventory inv) {
		/*
		 * Ensure inventory is from a not-null merchant
		 * Check that the player is either member or treaty with town guild, or has no guild
		 * Get all merchant trades in the inventory and apply special price
		 */
		if(!isDiscountEnable) {
			return;
		}
		if(inv != null && inv.getType().equals(InventoryType.MERCHANT) && inv instanceof MerchantInventory) {
			MerchantInventory merch = (MerchantInventory)inv;
			if(merch.getHolder() != null && merch.getMerchant() != null && merch.getHolder() instanceof Villager) {
				// The inventory belongs to a valid merchant villager entity
				// Check that the player can receive guild discounts
				boolean doDiscounts = false;
				KonGuild playerGuild = getPlayerGuild(player.getOfflineBukkitPlayer());
				KonGuild townGuild = getTownGuild(town);
				if(playerGuild != null) {
					// Player belongs to a guild, make sure it is the same or treaty with town's guild
					if(townGuild.equals(playerGuild) || !townGuild.isSanction(playerGuild)) {
						doDiscounts = true;
					}
				} else {
					doDiscounts = true;
				}
				// Check that the merchant is of the correct specialized profession
				Villager host = (Villager)merch.getHolder();
				if(host.getProfession().equals(townGuild.getSpecialization())) {
					if(!doDiscounts) {
						// TODO: use MessagePath
						ChatUtil.sendError(player.getBukkitPlayer(), townGuild.getName()+" Guild is blocking your guild from trade discounts with this merchant!");
						return;
					}
					// Proceed with discounts for the valid villager's profession
					double priceAdj = (double)discountPercent/100;
					int amount = 0;
					int discount = 0;
					Merchant tradeHost = merch.getMerchant();
					List<MerchantRecipe> tradeListDiscounted = new ArrayList<MerchantRecipe>();
					for(MerchantRecipe trade : tradeHost.getRecipes()) {
						ChatUtil.printDebug("Found trade for "+trade.getResult().getType().toString()+" with price mult "+trade.getPriceMultiplier()+
								", special "+trade.getSpecialPrice()+", uses "+trade.getUses()+", max "+trade.getMaxUses());
						List<ItemStack> ingredientList = trade.getIngredients();
						for(ItemStack ingredient : ingredientList) {
							ChatUtil.printDebug("  Has ingredient "+ingredient.getType().toString()+", amount: "+ingredient.getAmount());
						}
						if(!ingredientList.isEmpty()) {
							amount = ingredientList.get(0).getAmount();
							discount = (int)(amount*priceAdj*-1);
							if(isDiscountStack) {
								discount += trade.getSpecialPrice();
							}
							trade.setSpecialPrice(discount);
							ChatUtil.printDebug("  Applied special price "+discount);
						}
						tradeListDiscounted.add(trade);
					}
					tradeHost.setRecipes(tradeListDiscounted);
					if(discountPercent > 0) {
						Konquest.playDiscountSound(player.getBukkitPlayer());
						// TODO: use MessagePath
						ChatUtil.sendNotice(player.getBukkitPlayer(), townGuild.getName()+" Guild gives you a "+discountPercent+" percent discount with this merchant");
					}
				}
			}
		}
		return;
	}
	
	/**
	 * Creates a new guild
	 * @param name - Guild name
	 * @param master - Player who is the guild master
	 * @return 0	- Success
	 * 	       1	- Bad name
	 * 		   2	- Master already is a guild member
	 *         3    - Not enough favor
	 */
	public int createGuild(String name, KonPlayer master) {
		Player bukkitPlayer = master.getBukkitPlayer();
		if(costCreate > 0) {
			if(KonquestPlugin.getBalance(bukkitPlayer) < costCreate) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(costCreate));
                return 3;
			}
    	}
		if(!name.contains(" ") && !konquest.getKingdomManager().isKingdom(name) && !konquest.getKingdomManager().isTown(name)) {
			KonGuild currentGuild = getPlayerGuild(master.getOfflineBukkitPlayer());
			if(currentGuild == null) {
				KonGuild newGuild = new KonGuild(name, master.getBukkitPlayer().getUniqueId(), master.getKingdom());
				guilds.add(newGuild);
				// Withdraw cost
				if(costCreate > 0 && newGuild != null) {
		            if(KonquestPlugin.withdrawPlayer(bukkitPlayer, costCreate)) {
		            	konquest.getAccomplishmentManager().modifyPlayerStat(master,KonStatsType.FAVOR,(int)costCreate);
		            }
				}
			} else {
				return 2;
			}
		} else {
			return 1;
		}
		return 0;
	}
	
	public boolean renameGuild(KonGuild guild, String name, KonPlayer player) {
		boolean result = false;
		Player bukkitPlayer = player.getBukkitPlayer();
		// Check cost
		if(costRename > 0) {
			if(KonquestPlugin.getBalance(bukkitPlayer) < costRename) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(costRename));
                return false;
			}
    	}
		// Attempt to rename
		if(!name.contains(" ") && !konquest.getKingdomManager().isKingdom(name) && !konquest.getKingdomManager().isTown(name)) {
			guild.setName(name);
			result = true;
			// Withdraw cost
			if(costRename > 0) {
	            if(KonquestPlugin.withdrawPlayer(bukkitPlayer, costRename)) {
	            	konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)costRename);
	            }
			}
		}
		return result;
	}
	
	public void removeGuild(KonGuild guild) {
		guilds.remove(guild);
		// Remove the guild from all other relationships
		for(KonGuild currentGuild : guilds) {
			currentGuild.removeGuildRelationship(guild);
		}
	}
	
	public void toggleGuildOpen(KonGuild guild, KonPlayer player) {
		if(guild != null) {
			if(guild.isOpen()) {
				guild.setIsOpen(false);
				ChatUtil.sendNotice(player.getBukkitPlayer(), guild.getName()+" Guild changed to closed");
			} else {
				guild.setIsOpen(true);
				ChatUtil.sendNotice(player.getBukkitPlayer(), guild.getName()+" Guild changed to open");
			}
		}
	}
	
	public boolean toggleGuildStatus(KonGuild guild, KonGuild otherGuild, KonPlayer player) {
		Player bukkitPlayer = player.getBukkitPlayer();
		// Check cost
		if(costRelation > 0) {
			if(KonquestPlugin.getBalance(bukkitPlayer) < costRelation) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(costRelation));
                return false;
			}
    	}
		// Attempt to change relationship
		if(guild != null && otherGuild != null && !guild.equals(otherGuild)) {
			if(guild.getKingdom().equals(otherGuild.getKingdom())) {
				if(guild.isSanction(otherGuild)) {
					guild.removeSanction(otherGuild);
					broadcastGuild(guild, "Removed sanction for "+otherGuild.getName()+" Guild");
					broadcastGuild(otherGuild, guild.getName()+" Guild has removed the sanction on your guild!");
				} else {
					guild.addSanction(otherGuild);
					broadcastGuild(guild, "Added sanction for "+otherGuild.getName()+" Guild");
					broadcastGuild(otherGuild, guild.getName()+" Guild has added a sanction on your guild!");
				}
			} else {
				if(guild.isArmistice(otherGuild)) {
					// If either side breaks the armistice, make both hostile toward each other
					guild.removeArmistice(otherGuild);
					otherGuild.removeArmistice(guild);
					broadcastGuild(guild, "Broke the armistice with "+otherGuild.getName()+" Guild");
					broadcastGuild(otherGuild, guild.getName()+" Guild has broken the armistice with your guild!");
				} else {
					guild.addArmistice(otherGuild);
					broadcastGuild(guild, "Made an armistice with "+otherGuild.getName()+" Guild");
					broadcastGuild(otherGuild, guild.getName()+" Guild has offered an armistice with your guild!");
				}
			}
			// Withdraw cost
			if(costRelation > 0) {
	            if(KonquestPlugin.withdrawPlayer(bukkitPlayer, costRelation)) {
	            	konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)costRelation);
	            }
			}
		}
		return true;
	}
	
	public boolean changeSpecialization(Villager.Profession profession, KonGuild guild, KonPlayer player) {
		Player bukkitPlayer = player.getBukkitPlayer();
		// Check cost
		if(costSpecial > 0) {
			if(KonquestPlugin.getBalance(bukkitPlayer) < costSpecial) {
				ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(costSpecial));
                return false;
			}
    	}
		guild.setSpecialization(profession);
		// Withdraw cost
		if(costSpecial > 0) {
            if(KonquestPlugin.withdrawPlayer(bukkitPlayer, costSpecial)) {
            	konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)costSpecial);
            }
		}
		return true;
	}
	
	/*
	 * ===================================
	 * Membership Methods
	 * ===================================
	 */
	
	/**
	 * Online Player requests to join the guild to the officers
	 * @param player - The online player requesting to join the guild
	 * @param guild - The target guild that the player requests to join
	 */
	public void joinGuildRequest(KonPlayer player, KonGuild guild) {
		if(guild != null) {
			UUID id = player.getBukkitPlayer().getUniqueId();
			if(player.getKingdom().equals(guild.getKingdom())) {
				if(!guild.isMember(id)) {
					if(guild.isJoinInviteValid(id)) {
						// There is already a valid invite, add the player to the guild
						KonGuild playerGuild = getPlayerGuild(player.getOfflineBukkitPlayer());
						// Ensure the player is not already a guild member
						if(playerGuild == null) {
							addMember(guild, id, false);
							guild.removeJoinRequest(id);
							ChatUtil.sendNotice(player.getBukkitPlayer(), "Successfully joined guild");
							Konquest.playSuccessSound(player.getBukkitPlayer());
						} else {
							ChatUtil.sendError(player.getBukkitPlayer(), "Leave your current guild first!");
							Konquest.playFailSound(player.getBukkitPlayer());
						}
					} else if(!guild.isJoinRequestValid(id)){
						// Request to join if not already requested
						guild.addJoinRequest(id, false);
						ChatUtil.sendNotice(player.getBukkitPlayer(), "Sent request to join the guild");
						Konquest.playSuccessSound(player.getBukkitPlayer());
						broadcastOfficersGuild(guild,"New guild join request, type \"/k guild\" to review");
					} else {
						ChatUtil.sendError(player.getBukkitPlayer(), "You have already requested to join");
						Konquest.playFailSound(player.getBukkitPlayer());
					}
				}
			}
		}
		
	}
	
	/**
	 * Officer approves or denies join request of (offline) player.
	 * Check to ensure target player is not currently in a guild.
	 * @param player - The offline player target of the response
	 * @param guild - The target guild that player wants to join
	 * @param resp - True to approve, false to reject request
	 * @return True when request response was successful, else false when target is already in a guild.
	 */
	public boolean respondGuildRequest(OfflinePlayer player, KonGuild guild, boolean resp) {
		boolean result = true;
		if(guild != null) {
			UUID id = player.getUniqueId();
			if(guild.isJoinRequestValid(id)) {
				if(resp) {
					KonGuild playerGuild = getPlayerGuild(player);
					// Ensure the player is not already a guild member
					if(playerGuild == null) {
						// Approved join request, add player as member
						addMember(guild, id, false);
						if(player.isOnline()) {
							ChatUtil.sendNotice((Player)player, guild.getName()+" Guild join request accepted");
						}
					} else {
						result = false;
					}
				} else {
					// Denied join request
					if(player.isOnline()) {
						ChatUtil.sendError((Player)player, guild.getName()+" Guild join request denied");
					}
				}
				guild.removeJoinRequest(id);
			}
		}
		return result;
	}
	
	/**
	 * Officer invites (offline) player to join (with /guild add command).
	 * Allow invites to players already in a guild.
	 * @param player - The offline player that the officer invites to join their guild
	 * @param guild - The target guild for the join invite
	 */
	public boolean joinGuildInvite(KonOfflinePlayer player, KonGuild guild) {
		boolean result = false;
		if(guild != null) {
			OfflinePlayer offlineBukkitPlayer = player.getOfflineBukkitPlayer();
			UUID id = offlineBukkitPlayer.getUniqueId();
			if(player.getKingdom().equals(guild.getKingdom())) {
				if(!guild.isMember(id)) {
					if(guild.isJoinRequestValid(id)) {
						// There is already a valid request, add the player to the guild
						KonGuild playerGuild = getPlayerGuild(player.getOfflineBukkitPlayer());
						// Ensure the player is not already a guild member
						if(playerGuild == null) {
							addMember(guild, id, false);
							guild.removeJoinRequest(id);
							if(offlineBukkitPlayer.isOnline()) {
								ChatUtil.sendNotice((Player)offlineBukkitPlayer, guild.getName()+" Guild join request accepted");
							}
							result = true;
						}
					} else if(!guild.isJoinInviteValid(id)) {
						// Invite to join if not already invited
						guild.addJoinRequest(id, true);
						if(offlineBukkitPlayer.isOnline()) {
							ChatUtil.sendNotice((Player)offlineBukkitPlayer, "Received new guild invite. Type \"/k guild\" to respond to invites.");
						}
						result = true;
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * Player accepts or declines join invite
	 * @param player - The online player responding to the invite to join
	 * @param guild - The target guild that the player is responding to
	 * @param resp - True to accept, false to decline invite
	 */
	public boolean respondGuildInvite(KonPlayer player, KonGuild guild, boolean resp) {
		boolean result = false;
		if(guild != null) {
			UUID id = player.getBukkitPlayer().getUniqueId();
			if(guild.isJoinInviteValid(id)) {
				if(resp) {
					KonGuild playerGuild = getPlayerGuild(player.getOfflineBukkitPlayer());
					// Ensure the player is not already a guild member
					if(playerGuild == null) {
						// Accept join invite, add as member
						addMember(guild, id, false);
						guild.removeJoinRequest(id);
						ChatUtil.sendNotice(player.getBukkitPlayer(), guild.getName()+" Guild join invite accepted");
						Konquest.playSuccessSound(player.getBukkitPlayer());
					} else {
						ChatUtil.sendError(player.getBukkitPlayer(), "Leave your current guild first!");
						Konquest.playFailSound(player.getBukkitPlayer());
					}
					result = true;
				} else {
					// Denied join request
					guild.removeJoinRequest(id);
					ChatUtil.sendNotice(player.getBukkitPlayer(), guild.getName()+" Guild join invite declined");
				}
			}
		}
		return result;
	}
	
	public void leaveGuild(KonPlayer player, KonGuild guild) {
		if(guild != null) {
			UUID id = player.getBukkitPlayer().getUniqueId();
			boolean status = removeMember(guild, id);
			if(status) {
				ChatUtil.sendNotice(player.getBukkitPlayer(), "Successfully left the guild");
				Konquest.playSuccessSound(player.getBukkitPlayer());
			} else {
				ChatUtil.sendNotice(player.getBukkitPlayer(), "Failed to leave, try disbanding instead");
				Konquest.playFailSound(player.getBukkitPlayer());
			}
		}
	}
	
	public boolean kickGuildMember(OfflinePlayer player, KonGuild guild) {
		boolean result = false;
		UUID id = player.getUniqueId();
		result = removeMember(guild, id);
		return result;
	}
	
	public void removePlayerGuild(OfflinePlayer player) {
		removePlayerGuild(player,getPlayerGuild(player));
	}
	
	public void removePlayerGuild(OfflinePlayer player, KonGuild guild) {
		UUID id = player.getUniqueId();
		if(guild != null) {
			// Found guild where target player is a member
			if(guild.isMaster(id)) {
				// Player is master, transfer if possible
				List<OfflinePlayer> officers = guild.getPlayerOfficersOnly();
				if(!officers.isEmpty()) {
					// Make the first officer into the master
					transferMaster(officers.get(0),guild);
					// Now remove the player
					removeMember(guild, id);
				} else {
					// There are no officers
					List<OfflinePlayer> members = guild.getPlayerMembersOnly();
					if(!members.isEmpty()) {
						// Make the first member into the master
						transferMaster(members.get(0),guild);
						// Now remove the player
						removeMember(guild, id);
					} else {
						// There are no members to transfer master to, delete the guild
						removeGuild(guild);
					}
				}
			} else {
				// Player is not the master, remove
				removeMember(guild, id);
			}
		}
	}
	
	public void promoteOfficer(OfflinePlayer player, KonGuild guild) {
		UUID id = player.getUniqueId();
		if(!guild.isOfficer(id)) {
			guild.setOfficer(id, true);
		}
	}
	
	public void demoteOfficer(OfflinePlayer player, KonGuild guild) {
		UUID id = player.getUniqueId();
		if(guild.isOfficer(id)) {
			guild.setOfficer(id, false);
		}
	}
	
	public void transferMaster(OfflinePlayer player, KonGuild guild) {
		UUID id = player.getUniqueId();
		if(guild.isMember(id)) {
			guild.setMaster(id);
		}
	}
	
	private void broadcastGuild(KonGuild guild, String message) {
		if(guild != null) {
			for(OfflinePlayer offlinePlayer : guild.getPlayerMembers()) {
				if(offlinePlayer.isOnline()) {
					Player player = (Player)offlinePlayer;
					ChatUtil.sendNotice(player, message);
				}
			}
		}
	}
	
	private void broadcastOfficersGuild(KonGuild guild, String message) {
		if(guild != null) {
			for(OfflinePlayer offlinePlayer : guild.getPlayerOfficers()) {
				if(offlinePlayer.isOnline()) {
					Player player = (Player)offlinePlayer;
					ChatUtil.sendNotice(player, message);
				}
			}
		}
	}
	
	private boolean removeMember(KonGuild guild, UUID id) {
		boolean result = guild.removeMember(id);
		if(result) {
			playerGuildCache.remove(id);
		}
		return result;
	}
	
	private boolean addMember(KonGuild guild, UUID id, boolean isOfficer) {
		boolean result = guild.addMember(id, isOfficer);
		if(result) {
			playerGuildCache.remove(id);
		}
		return result;
	}

	/*
	 * ===================================
	 * Query Methods
	 * ===================================
	 */
	
	public List<KonGuild> getAllGuilds() {
		List<KonGuild> result = new ArrayList<KonGuild>();
		for(KonGuild guild : guilds) {
			result.add(guild);
		}
		return result;
	}
	
	public List<KonGuild> getKingdomGuilds(KonKingdom kingdom) {
		List<KonGuild> result = new ArrayList<KonGuild>();
		for(KonGuild guild : guilds) {
			if(kingdom.equals(guild.getKingdom())) {
				result.add(guild);
			}
		}
		return result;
	}
	
	public List<KonGuild> getInviteGuilds(KonPlayer player) {
		List<KonGuild> result = new ArrayList<KonGuild>();
		for(KonGuild guild : guilds) {
			if(player.getKingdom().equals(guild.getKingdom()) && guild.isJoinInviteValid(player.getBukkitPlayer().getUniqueId())) {
				result.add(guild);
			}
		}
		return result;
	}
	
	public KonGuild getTownGuild(KonTown town) {
		KonGuild result = null;
		UUID lordID = town.getLord();
		if(lordID != null) {
			result = getPlayerGuild(lordID);
		}
		return result;
	}
	
	public KonGuild getPlayerGuild(OfflinePlayer player) {
		return getPlayerGuild(player.getUniqueId());
	}
	
	public KonGuild getPlayerGuild(UUID id) {
		KonGuild result = null;
		if(id != null) {
			if(playerGuildCache.containsKey(id)) {
				// Use cached result
				result = playerGuildCache.get(id);
			} else {
				// Search for result, and cache it
				for(KonGuild guild : guilds) {
					if(guild.isMember(id)) {
						result = guild;
						break;
					}
				}
				playerGuildCache.put(id, result); // can be null!
			}
		}
		return result;
	}
	
	public KonGuild getGuild(String name) {
		KonGuild result = null;
		for(KonGuild guild : guilds) {
			if(guild.getName().equals(name)) {
				result = guild;
				break;
			}
		}
		return result;
	}
	
	public boolean isArmistice(KonGuild guild1, KonGuild guild2) {
		boolean result = false;
		if(guild1 != null && guild2 != null && guild1.isArmistice(guild2) && guild2.isArmistice(guild1)) {
			result = true;
		}
		return result;
	}
	
	public boolean isArmistice(KonPlayer player1, KonPlayer player2) {
		boolean result = false;
		KonGuild guild1 = getPlayerGuild(player1.getOfflineBukkitPlayer());
		KonGuild guild2 = getPlayerGuild(player2.getOfflineBukkitPlayer());
		result = isArmistice(guild1, guild2);
		return result;
	}
	
	public boolean isArmistice(KonPlayer player1, KonTown town2) {
		boolean result = false;
		KonGuild guild1 = getPlayerGuild(player1.getOfflineBukkitPlayer());
		KonGuild guild2 = getTownGuild(town2);
		result = isArmistice(guild1, guild2);
		return result;
	}
	
	/*
	 * ===================================
	 * Saving Methods
	 * ===================================
	 */

	private void validateGuilds() {
		// Ensure that players are not members of multiple guilds
		// Iterate over all guilds, cache members, check for multiple memberships and remove as necessary
		HashSet<OfflinePlayer> playerCache = new HashSet<OfflinePlayer>();
		HashMap<OfflinePlayer,KonGuild> removalList = new HashMap<OfflinePlayer,KonGuild>();
		// Determine players to be removed from duplicate guilds
		for(KonGuild guild : guilds) {
			for(OfflinePlayer p : guild.getPlayerMembers()) {
				// Is this guild member already accounted for?
				if(playerCache.contains(p)) {
					// This guild member has already been seen in another guild. Put them in the removal list.
					removalList.put(p,guild);
				} else {
					// This guild member is unaccounted for, cache them
					playerCache.add(p);
				}
			}
		}
		// Perform excess guild removal
		for(OfflinePlayer p : removalList.keySet()) {
			removePlayerGuild(p,removalList.get(p));
		}
	}
	
	private void loadGuilds() {
		if(!isEnabled) {
			ChatUtil.printConsoleAlert("Disabled guilds");
			return;
		}
		FileConfiguration guildsConfig = konquest.getConfigManager().getConfig("guilds");
        if (guildsConfig.get("guilds") == null) {
        	ChatUtil.printDebug("There is no guilds section in guilds.yml");
            return;
        }
        guilds.clear();
        ChatUtil.printDebug("  Beginning guild load, size "+guilds.size());
        ConfigurationSection guildsSection = guildsConfig.getConfigurationSection("guilds");
        Set<String> guildSet = guildsSection.getKeys(false);
        
        boolean isOpen;
        String specializationName;
        String kingdomName;
        String masterUUIDStr;
        KonKingdom kingdom;
        UUID playerUUID;
        
        // Load initial guild set
        for(String guildName : guildSet) {
        	if(guildsSection.contains(guildName)) {
        		ConfigurationSection guildInstanceSection = guildsSection.getConfigurationSection(guildName);
        		// Parse property fields
        		isOpen = guildInstanceSection.getBoolean("open",false);
        		specializationName = guildInstanceSection.getString("specialization","NONE");
        		kingdomName = guildInstanceSection.getString("kingdom");
        		masterUUIDStr = guildInstanceSection.getString("master");
        		kingdom = konquest.getKingdomManager().getKingdom(kingdomName);
        		playerUUID = Konquest.idFromString(masterUUIDStr);
        		// Check valid fields
        		if(!kingdom.equals(konquest.getKingdomManager().getBarbarians()) && playerUUID != null) {
        			// Create guild
        			KonGuild newGuild = new KonGuild(guildName, playerUUID, kingdom);
    				// Set open flag
        			newGuild.setIsOpen(isOpen);
        			// Set profession
        			Villager.Profession profession = Villager.Profession.NONE;
        			try {
        				profession = Villager.Profession.valueOf(specializationName);
        			} catch(Exception e) {
        				ChatUtil.printConsoleAlert("Failed to parse profession "+specializationName+" for guild "+guildName);
        			}
        			newGuild.setSpecialization(profession);
        			// Add members
        			if(guildInstanceSection.contains("members")) {
		            	for(String memberUUIDStr : guildInstanceSection.getConfigurationSection("members").getKeys(false)) {
		            		boolean isOfficer = guildInstanceSection.getBoolean("members."+memberUUIDStr);
		            		playerUUID = Konquest.idFromString(memberUUIDStr);
		            		if(playerUUID != null) {
		            			newGuild.addMember(playerUUID,isOfficer);
		            		}
		            	}
	            	}
        			// Add membership requests
        			if(guildInstanceSection.contains("requests")) {
	            		for(String requestUUIDStr : guildInstanceSection.getConfigurationSection("requests").getKeys(false)) {
	            			boolean type = guildInstanceSection.getBoolean("requests."+requestUUIDStr);
	            			playerUUID = Konquest.idFromString(requestUUIDStr);
		            		if(playerUUID != null) {
		            			newGuild.addJoinRequest(playerUUID, type);
		            		}
	            		}
	            	}
        			guilds.add(newGuild);
        			//ChatUtil.printDebug("  Added guild "+newGuild.getName()+", size "+guilds.size());
        		}
        	}
        }
        // Load subsequent guild relationships
        for(String guildName : guildSet) {
        	if(guildsSection.contains(guildName)) {
        		ConfigurationSection guildInstanceSection = guildsSection.getConfigurationSection(guildName);
        		KonGuild currentGuild = getGuild(guildName);
        		if(currentGuild != null) {
	        		// Add sanction and armistice lists
	    			if(guildInstanceSection.contains("sanction")) {
	    				List<String> sanctionList = guildInstanceSection.getStringList("sanction");
	    				for(String sanctionName : sanctionList) {
	    					KonGuild otherGuild = getGuild(sanctionName);
	    					if(otherGuild != null) {
	    						currentGuild.addSanction(otherGuild);
	    					} else {
	    						ChatUtil.printDebug("Failed to add sanction guild by name: "+sanctionName);
	    					}
	    				}
	    			}
	    			if(guildInstanceSection.contains("armistice")) {
	    				List<String> armisticeList = guildInstanceSection.getStringList("armistice");
	    				for(String armisticeName : armisticeList) {
	    					KonGuild otherGuild = getGuild(armisticeName);
	    					if(otherGuild != null) {
	    						currentGuild.addArmistice(otherGuild);
	    					} else {
	    						ChatUtil.printDebug("Failed to add armistice guild by name: "+armisticeName);
	    					}
	    				}
	    			}
        		} else {
        			ChatUtil.printDebug("Failed to find guild by name: "+guildName);
        		}
        	}
        }
        // Finished loading guilds
        ChatUtil.printDebug("  Finished guild load, size "+guilds.size());
	}
	
	public void saveGuilds() {
		FileConfiguration guildsConfig = konquest.getConfigManager().getConfig("guilds");
		guildsConfig.set("guilds", null); // reset guilds config
		ConfigurationSection root = guildsConfig.createSection("guilds");
		for(KonGuild guild : guilds) {
			ConfigurationSection guildSection = root.createSection(guild.getName());
			guildSection.set("open", guild.isOpen());
			guildSection.set("specialization", guild.getSpecialization().toString());
			guildSection.set("kingdom", guild.getKingdom().getName());
			ConfigurationSection guildMemberSection = guildSection.createSection("members");
            for(OfflinePlayer member : guild.getPlayerMembers()) {
            	String uuid = member.getUniqueId().toString();
            	if(guild.isMaster(member.getUniqueId())) {
            		guildSection.set("master", uuid);
            	} else if(guild.isOfficer(member.getUniqueId())) {
            		guildMemberSection.set(uuid, true);
            	} else {
            		guildMemberSection.set(uuid, false);
            	}
            }
            ConfigurationSection guildRequestsSection = guildSection.createSection("requests");
            for(OfflinePlayer requestee : guild.getJoinRequests()) {
            	String uuid = requestee.getUniqueId().toString();
            	guildRequestsSection.set(uuid, false);
            }
            for(OfflinePlayer invitee : guild.getJoinInvites()) {
            	String uuid = invitee.getUniqueId().toString();
            	guildRequestsSection.set(uuid, true);
            }
            guildSection.set("sanction", guild.getSanctionNames());
            guildSection.set("armistice", guild.getArmisticeNames());
		}
	}

}
