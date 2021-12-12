package konquest.listener;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.event.KonquestEnterTerritoryEvent;
import konquest.manager.CampManager;
import konquest.manager.KingdomManager;
import konquest.manager.PlayerManager;
import konquest.model.KonCamp;
import konquest.model.KonCapital;
import konquest.model.KonKingdom;
import konquest.model.KonPlayer;
import konquest.model.KonRuin;
import konquest.model.KonStatsType;
import konquest.model.KonTerritory;
import konquest.model.KonTerritoryType;
import konquest.model.KonTown;
import konquest.model.KonPlayer.RegionType;
import konquest.model.KonPlot;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.Switch;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.vehicle.VehicleEnterEvent;


//TODO prevent Barbarians from earning money
public class PlayerListener implements Listener{

	private Konquest konquest;
	private PlayerManager playerManager;
	private KingdomManager kingdomManager;
	private CampManager campManager;
	
	public PlayerListener(KonquestPlugin plugin) {
		this.konquest = plugin.getKonquestInstance();
		this.playerManager = konquest.getPlayerManager();
		this.kingdomManager = konquest.getKingdomManager();
		this.campManager = konquest.getCampManager();
	}
	
	/**
     * Fires when a player joins the server
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
    	//ChatUtil.printDebug("EVENT: Player Joined");
    	Player bukkitPlayer = event.getPlayer();
    	KonPlayer player = konquest.initPlayer(bukkitPlayer);
    	// Schedule messages to display after 10-tick delay (0.5 second)
    	Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
            @Override
            public void run() {
            	// Actions to run after delay
            	if(player == null) {
            		ChatUtil.printDebug("Failed to use player from null import on player join");
            		return;
            	}
            	// Send helpful messages
            	if(player.getKingdom().isSmallest()) {
            		int boostPercent = konquest.getConfigManager().getConfig("core").getInt("core.kingdoms.smallest_exp_boost_percent");
            		if(boostPercent > 0) {
            			ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_SMALL_KINGDOM.getMessage(boostPercent), ChatColor.ITALIC);
            		}
            	}
            	if(bukkitPlayer.hasPermission("konquest.command.admin")) {
            		for(String msg : konquest.opStatusMessages) {
            			ChatUtil.sendError(bukkitPlayer, msg);
            		}
            		konquest.opStatusMessages.clear();
            	}
            	// Messages for town joining
            	for(KonTown town : player.getKingdom().getTowns()) {
            		if(town.isJoinInviteValid(player.getBukkitPlayer().getUniqueId())) {
            			//ChatUtil.sendNotice(bukkitPlayer, "You're invited to join the town of "+town.getName()+", use \"/k join "+town.getName()+"\" to accept or \"/k leave "+town.getName()+"\" to decline", ChatColor.LIGHT_PURPLE);
            			ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_JOIN_INVITE.getMessage(town.getName(),town.getName(),town.getName()), ChatColor.LIGHT_PURPLE);
            		}
            		if(town.isPlayerElite(bukkitPlayer)) {
            			for(OfflinePlayer invitee : town.getJoinRequests()) {
            				//ChatUtil.sendNotice(bukkitPlayer, invitee.getName()+" wants to join "+town.getName()+", use \"/k town "+town.getName()+" add "+invitee.getName()+"\" to allow, \"/k town "+town.getName()+" kick "+invitee.getName()+"\" to deny", ChatColor.LIGHT_PURPLE);
            				ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_JOIN_REQUEST.getMessage(invitee.getName(),town.getName(),town.getName(),invitee.getName(),town.getName(),invitee.getName()), ChatColor.LIGHT_PURPLE);
            			}
            		}
            	}
            }
        }, 10);
    }
    
    /**
     * Fires when a player quits the server
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
    	//ChatUtil.printDebug("EVENT: Player Left");
    	//playerManager.savePlayers();
    	// remove player from cache
    	Player bukkitPlayer = event.getPlayer();
    	KonPlayer player = playerManager.getPlayer(bukkitPlayer);
    	if(player != null) {
    		player.stopTimers();
        	player.clearAllMobAttackers();
        	konquest.getDatabaseThread().getDatabase().flushPlayerData(bukkitPlayer);
        	playerManager.removePlayer(bukkitPlayer);
    	} else {
    		ChatUtil.printDebug("Null Player Left!");
    	}
    	//playerManager.savePlayer(player);
    	// Update offline protections
    	kingdomManager.updateKingdomOfflineProtection();
    	//playerManager.updateNumKingdomPlayersOnline();
    	//playerManager.updateAllSavedPlayers();
    	if(player.isBarbarian()) {
    		KonCamp camp = campManager.getCamp(player);
    		if(camp != null) {
    			camp.setProtected(true);
    		}
    	}
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPlayerChatLowest(AsyncPlayerChatEvent event) {
    	if(konquest.getChatPriority().equals(EventPriority.LOWEST)) {
    		//ChatUtil.printDebug("Using chat event priority LOWEST");
    		onAsyncPlayerChat(event);
    	}
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onAsyncPlayerChatLow(AsyncPlayerChatEvent event) {
    	if(konquest.getChatPriority().equals(EventPriority.LOW)) {
    		//ChatUtil.printDebug("Using chat event priority LOW");
    		onAsyncPlayerChat(event);
    	}
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onAsyncPlayerChatNormal(AsyncPlayerChatEvent event) {
    	if(konquest.getChatPriority().equals(EventPriority.NORMAL)) {
    		//ChatUtil.printDebug("Using chat event priority NORMAL");
    		onAsyncPlayerChat(event);
    	}
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPlayerChatHigh(AsyncPlayerChatEvent event) {
    	if(konquest.getChatPriority().equals(EventPriority.HIGH)) {
    		//ChatUtil.printDebug("Using chat event priority HIGH");
    		onAsyncPlayerChat(event);
    	}
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerChatHighest(AsyncPlayerChatEvent event) {
    	if(konquest.getChatPriority().equals(EventPriority.HIGHEST)) {
    		//ChatUtil.printDebug("Using chat event priority HIGHEST");
    		onAsyncPlayerChat(event);
    	}
    }
    
    /**
     * Fires on chat events
     * All players always see global chat messages and team chat messages
     * @param event
     */
    private void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        //Check if the event was caused by a player
        if(event.isAsynchronous() && !event.isCancelled()) {
        	boolean enable = konquest.getConfigManager().getConfig("core").getBoolean("core.chat.enable_format",true);
        	boolean formatName = konquest.getConfigManager().getConfig("core").getBoolean("core.chat.name_team_color",true);
        	if(enable) {
	        	// Format chat messages
        		Player bukkitPlayer = event.getPlayer();
	        	if(!konquest.getPlayerManager().isPlayer(bukkitPlayer)) {
					ChatUtil.printDebug("Failed to handle onAsyncPlayerChat for non-existent player");
					return;
				}
	            KonPlayer player = playerManager.getPlayer(bukkitPlayer);
	            KonKingdom kingdom = player.getKingdom();
	            event.setCancelled(true);
	            
	            String title = "";
	            if(player.getPlayerPrefix().isEnabled()) {
	            	title = ChatUtil.parseHex(player.getPlayerPrefix().getMainPrefixName());
	            }
	            String prefix = ChatUtil.parseHex(konquest.getIntegrationManager().getLuckPermsPrefix(bukkitPlayer));
	            String suffix = ChatUtil.parseHex(konquest.getIntegrationManager().getLuckPermsSuffix(bukkitPlayer));
	            String kingdomName = kingdom.getName();
	            String name = bukkitPlayer.getName();
	            
	            if(player.isGlobalChat()) {
	            	//Global chat, all players see this format
	            	ChatUtil.printConsole(ChatColor.GOLD + kingdom.getName() + " | " + bukkitPlayer.getName()+": "+ChatColor.DARK_GRAY+event.getMessage());
	            	for(KonPlayer globalPlayer : playerManager.getPlayersOnline()) {
	            		ChatColor teamColor = ChatColor.WHITE;
	            		ChatColor titleColor = ChatColor.WHITE;
	            		if(player.isBarbarian()) {
	            			teamColor = ChatColor.YELLOW;
	            		} else {
	            			if(globalPlayer.getKingdom().equals(kingdom)) {
	                			// Message sender is in same kingdom as receiver
	                			teamColor = ChatColor.GREEN;
	                			titleColor = ChatColor.DARK_GREEN;
	                		} else {
	                			// Message sender is in different kingdom as receiver
	            				teamColor = ChatColor.RED;
	                			titleColor = ChatColor.DARK_RED;
	                		}
	            		}
	            		globalPlayer.getBukkitPlayer().sendMessage(
	            				ChatUtil.parseFormat(Konquest.getChatMessage(),
	            						prefix,
	            						suffix,
	            						kingdomName,
	            						title,
	            						name,
	            						teamColor,
	            						titleColor,
	            						formatName) +
	        					Konquest.chatDivider + ChatColor.RESET + " " + event.getMessage());
	            	}
	            } else {
	            	//Team chat only (and admins)
	            	ChatUtil.printConsole(ChatColor.GOLD + kingdom.getName() + " | " + "[K] "+bukkitPlayer.getName()+": "+ChatColor.DARK_GRAY+event.getMessage());
	            	for(KonPlayer teamPlayer : playerManager.getPlayersOnline()) {
	            		if(teamPlayer.getKingdom().equals(kingdom)) {
	            			teamPlayer.getBukkitPlayer().sendMessage(
		            				ChatUtil.parseFormat(Konquest.getChatMessage(),
		            						prefix,
		            						suffix,
		            						kingdomName,
		            						title,
		            						name,
		            						ChatColor.GREEN,
		            						ChatColor.GREEN,
		            						true) +
		            				Konquest.chatDivider + ChatColor.RESET + " " + ChatColor.GREEN+ChatColor.ITALIC+event.getMessage());
	            		} else if(teamPlayer.isAdminBypassActive()) {
	            			teamPlayer.getBukkitPlayer().sendMessage(
		            				ChatUtil.parseFormat(Konquest.getChatMessage(),
		            						prefix,
		            						suffix,
		            						kingdomName,
		            						title,
		            						name,
		            						ChatColor.GOLD,
		            						ChatColor.GOLD,
		            						true) +
		            				Konquest.chatDivider + ChatColor.RESET + " " + ChatColor.GOLD+ChatColor.ITALIC+event.getMessage());
	            		}
	            	}
	            }
        	}
        }
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
    	KonPlayer player = playerManager.getPlayer(event.getPlayer());
    	if(player != null && player.isCombatTagged()) {
    		for(String cmd : playerManager.getBlockedCommands()) {
    			if(event.getMessage().toLowerCase().startsWith("/"+cmd.toLowerCase())) {
    				//ChatUtil.sendError(event.getPlayer(), "This command is blocked while in combat");
    				ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_TAG_BLOCKED.getMessage());
    				event.setCancelled(true);
    				return;
    			}
    		}
    	}
    }
    
    /**
     * Handles when players are setting regions and clicking signs
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
    	// TODO Allow players to use boats in capital?
//    	if(konquest.isWorldIgnored(event.getPlayer().getLocation().getWorld())) {
//			return;
//		}
    	Player bukkitPlayer = event.getPlayer();
    	if(!konquest.getPlayerManager().isPlayer(bukkitPlayer)) {
			ChatUtil.printDebug("Failed to handle onPlayerInteract for non-existent player");
			return;
		}
        KonPlayer player = playerManager.getPlayer(bukkitPlayer);
        // When a player is setting regions...
        if (player.isSettingRegion()) {
        	if (event.getClickedBlock() == null) {
             	//ChatUtil.sendNotice(bukkitPlayer, "Clicked in Air, cancelled region creation!");
             	ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_CLICKED_AIR.getMessage());
             	player.setRegionCornerOneBuffer(null);
                player.setRegionCornerTwoBuffer(null);
                player.settingRegion(RegionType.NONE);
                event.setCancelled(true);
                return;
            }
        	// Different region cases...
        	Location location = event.getClickedBlock().getLocation();
        	String ruinName = "";
        	switch (player.getRegionType()) {
	        	case MONUMENT:
	                if (player.getRegionCornerOneBuffer() == null) {
	                	player.setRegionCornerOneBuffer(location);
	                    //ChatUtil.sendNotice(bukkitPlayer, "Click on the second corner block of the region.");
	                    //ChatUtil.sendNotice(bukkitPlayer, "Base area must be 16x16 (green particles).");
	                    ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_CREATE_2.getMessage());
	                } else if (player.getRegionCornerTwoBuffer() == null) {
	                	player.setRegionCornerTwoBuffer(location);
	                    //ChatUtil.sendNotice(bukkitPlayer, "Click on the travel point block.");
	                    ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_CREATE_3.getMessage());
	                } else {
	                	KonKingdom kingdom = kingdomManager.getKingdom(player.getRegionKingdomName());
	                	int createMonumentStatus = kingdom.createMonumentTemplate(player.getRegionCornerOneBuffer(), player.getRegionCornerTwoBuffer(), location);
	                	switch(createMonumentStatus) {
	    				case 0:
	    					//ChatUtil.sendNotice(bukkitPlayer, "Successfully created new Monument Template for kingdom "+player.getRegionKingdomName());
	    					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_SUCCESS.getMessage(player.getRegionKingdomName()));
	    					kingdom.startMonumentBlanking();
	    					break;
	    				case 1:
	    					int diffX = (int)Math.abs(player.getRegionCornerOneBuffer().getX()-player.getRegionCornerTwoBuffer().getX())+1;
	    					int diffZ = (int)Math.abs(player.getRegionCornerOneBuffer().getZ()-player.getRegionCornerTwoBuffer().getZ())+1;
	    					//ChatUtil.sendError(bukkitPlayer, "Failed to create Monument Template, base must be 16x16 blocks but got "+diffX+"x"+diffZ);
	    					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_ERROR_FAIL_BASE.getMessage(diffX,diffZ));
	    					break;
	    				case 2:
	    					String criticalBlockTypeName = konquest.getConfigManager().getConfig("core").getString("core.monuments.critical_block");
	    					int maxCriticalhits = konquest.getConfigManager().getConfig("core").getInt("core.monuments.destroy_amount");
	    					//ChatUtil.sendError(bukkitPlayer, "Failed to create Monument Template, it must contain at least "+maxCriticalhits+" "+criticalBlockTypeName+" blocks");
	    					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_ERROR_FAIL_CRITICAL.getMessage(maxCriticalhits,criticalBlockTypeName));
	    					break;
	    				case 3:
	    					//ChatUtil.sendError(bukkitPlayer, "Failed to create Monument Template, travel point must be inside of the region");
	    					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_ERROR_FAIL_TRAVEL.getMessage());
	    					break;
	    				case 4:
	    					//ChatUtil.sendError(bukkitPlayer, "Failed to create Monument Template, region must be within "+player.getRegionKingdomName()+" Capital territory");
	    					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_ERROR_FAIL_CAPITAL.getMessage(player.getRegionKingdomName()));
	    					break;
	    				default:
	    					//ChatUtil.sendError(bukkitPlayer, "Could not create Monument Template: Unknown cause = "+createMonumentStatus);
	    					ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(createMonumentStatus));
	    					break;
	    				}
	                    player.setRegionCornerOneBuffer(null);
	                    player.setRegionCornerTwoBuffer(null);
	                    player.settingRegion(RegionType.NONE);
	                }
	        		break;
	        	case RUIN_CRITICAL:
	        		boolean validCriticalBlock = false;
	        		if(kingdomManager.isChunkClaimed(location)) {
	        			KonTerritory territory = kingdomManager.getChunkTerritory(location);
	        			if(territory.getTerritoryType().equals(KonTerritoryType.RUIN)) {
	        				Material criticalType = konquest.getRuinManager().getRuinCriticalBlock();
	        				if(event.getClickedBlock().getType().equals(criticalType)) {
	        					((KonRuin)territory).addCriticalLocation(location);
		        				ruinName = territory.getName();
		        				validCriticalBlock = true;
	        				} else {
	        					//ChatUtil.sendError(bukkitPlayer, "Clicked block does not match type: "+criticalType.toString());
	        					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_ADMIN_RUIN_ERROR_MATCH.getMessage(criticalType.toString()));
	        				}
	        			}
	        		}
	        		if(validCriticalBlock) {
	        			//ChatUtil.sendNotice(bukkitPlayer, "Added critical block to Ruin "+ruinName);
	        			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_RUIN_NOTICE_ADD.getMessage(ruinName));
	        		} else {
	        			//ChatUtil.sendError(bukkitPlayer, "Could not add this block to a valid Ruin");
	        			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_ADMIN_RUIN_ERROR_INVALID.getMessage());
	        		}
	        		break;
	        	case RUIN_SPAWN:
	        		boolean validSpawnBlock = false;
	        		if(kingdomManager.isChunkClaimed(location)) {
	        			KonTerritory territory = kingdomManager.getChunkTerritory(location);
	        			if(territory.getTerritoryType().equals(KonTerritoryType.RUIN)) {
	        				((KonRuin)territory).addSpawnLocation(location);
	        				ruinName = territory.getName();
	        				validSpawnBlock = true;
	        			}
	        		}
	        		if(validSpawnBlock) {
	        			//ChatUtil.sendNotice(bukkitPlayer, "Added spawn block to Ruin "+ruinName);
	        			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_RUIN_NOTICE_ADD.getMessage(ruinName));
	        		} else {
	        			//ChatUtil.sendError(bukkitPlayer, "Could not add this block to a valid Ruin");
	        			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_ADMIN_RUIN_ERROR_INVALID.getMessage());
	        		}
	        		break;
	        	default:
	        		break;
        	}
            event.setCancelled(true);
        } else {
        	// When a player is not setting regions...
        	if(!player.isAdminBypassActive() && event.hasBlock()) {
        		BlockState clickedState = event.getClickedBlock().getState();
        		
        		//ChatUtil.printDebug("EVENT player interaction with block "+clickedState.getType().toString()+" using action "+event.getAction().toString());
        		// Check for territory
        		if(kingdomManager.isChunkClaimed(clickedState.getLocation())) {
        			// Interaction occurred within claimed territory
	        		KonTerritory territory = kingdomManager.getChunkTerritory(clickedState.getLocation());
	        		// Prevent players from interacting with blocks in Capitals
	        		if(territory instanceof KonCapital) {
	        			boolean isCapitalUseEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.capital_use",false);
	        			// Allow interaction with signs or everything when config allows it
	        			if(!(clickedState instanceof Sign || isCapitalUseEnabled)) {
	        				//ChatUtil.printDebug("  running preventUse: capital");
	        				preventUse(event,player);
	        			}
	        		}
	        		// Ruin actions...
	        		if(territory instanceof KonRuin) {
	        			KonRuin ruin = (KonRuin)territory;
	        			// Target player who interacts with critical blocks
	        			if(ruin.isCriticalLocation(event.getClickedBlock().getLocation())) {
	        				ruin.targetAllGolemsToPlayer(bukkitPlayer);
	        				//ChatUtil.printDebug("EVENT player interaction with ruin critical block");
	        			}
	        		}
	        		// Town protections...
	        		if(territory instanceof KonTown) {
	        			KonTown town = (KonTown) territory;
	        			//ChatUtil.printDebug("EVENT player interaction within town "+town.getName());
	        			// Prevent enemies from interacting with things like buttons, levers, pressure plates...
	        			if(!player.getKingdom().equals(town.getKingdom()) && !town.isEnemyRedstoneAllowed()) {
	        				//ChatUtil.printDebug("  running preventUse: town");
	        				preventUse(event,player);
	    				}
	        			// Prevent enemies and non-residents from interacting with item frames
	        			if(!player.getKingdom().equals(town.getKingdom()) || (!town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer()))) {
	        				Material clickedMat = event.getClickedBlock().getType();
	        				//ChatUtil.printDebug("Player identified as enemy or non-resident, clicked block "+clickedMat.toString());
	        				if(clickedMat.equals(Material.ITEM_FRAME)) {
	        					//ChatUtil.printDebug("EVENT: Enemy or non-resident interacted with item frame");
	        					ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
	        					event.setCancelled(true);
	    						return;
	        				}
	        			}
	        		}
        		} else {
        			// Interaction occurred in the wild
        			boolean isWildUse = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.wild_use", true);
        			if(!isWildUse && !konquest.isWorldIgnored(clickedState.getLocation())) {
        				//ChatUtil.printDebug("  running preventUse: wild");
        				preventUse(event,player);
        			}
        		}
        	}
        	// Check for item...
        	if(event.hasItem()) {
            	if(event.getItem().getType().isRecord() && event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            		if(event.hasBlock() && event.getClickedBlock().getType().equals(Material.JUKEBOX) &&
            				!konquest.isWorldIgnored(event.getClickedBlock().getLocation())) {
            			// Update music stat when not on record cooldown
            			if(player.isRecordPlayCooldownOver()) {
            				konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.MUSIC,1);
            				player.markRecordPlayCooldown();
            			}
            		}
            	}
        	}
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerEnterVehicle(VehicleEnterEvent event) {
    	Entity ent = event.getEntered();
    	if(ent instanceof Player && konquest.getKingdomManager().isChunkClaimed(event.getVehicle().getLocation())) {
    		KonPlayer player = konquest.getPlayerManager().getPlayer((Player)ent);
    		KonTerritory territory = konquest.getKingdomManager().getChunkTerritory(event.getVehicle().getLocation());
    		boolean isCapitalUseEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.capital_use",false);
    		if(player != null && territory != null && !territory.getKingdom().equals(player.getKingdom()) && 
    				territory.getTerritoryType().equals(KonTerritoryType.CAPITAL) && !isCapitalUseEnabled) {
    			ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
    			event.setCancelled(true);
				return;
    		}
    	}
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerArrowInteract(EntityInteractEvent event) {
    	if(konquest.isWorldIgnored(event.getEntity().getLocation())) {
			return;
		}
    	// prevent player-shot arrows from interacting with things
    	Player bukkitPlayer = null;
		if (event.getEntity() instanceof Arrow) {
            Arrow arrow = (Arrow) event.getEntity();
            //ChatUtil.printDebug("...Attacker was an Arrow");
            if (arrow.getShooter() instanceof Player) {
            	bukkitPlayer = (Player) arrow.getShooter();
            	//ChatUtil.printDebug("...Arrow shooter was a Player");
            } else {
            	return;
            }
        } else { // not an arrow
            return;
        }
		//ChatUtil.printDebug("Caught player arrow interaction...");
		if(!event.isCancelled() && kingdomManager.isChunkClaimed(event.getBlock().getLocation())) {
			KonPlayer player = playerManager.getPlayer(bukkitPlayer);
	        KonTerritory territory = kingdomManager.getChunkTerritory(event.getBlock().getLocation());
	        // Protect territory from arrow interaction
	        if(territory instanceof KonCapital) {
	        	//ChatUtil.printDebug("Cancelling to protect capital");
	        	event.setCancelled(true);
    			return;
    		}
	        if(territory instanceof KonTown) {
    			KonTown town = (KonTown) territory;
    			if(player != null && !player.getKingdom().equals(town.getKingdom())) {
    				//ChatUtil.printDebug("Cancelling to protect town");
    				event.setCancelled(true);
        			return;
    			}
    			
	        }
		}
	}

    
    /**
     * Handles when players are right clicking entities
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    	if(konquest.isWorldIgnored(event.getPlayer().getLocation())) {
			return;
		}
    	Entity clicked = event.getRightClicked();
    	Player bukkitPlayer = event.getPlayer();
        KonPlayer player = playerManager.getPlayer(bukkitPlayer);
        
        if(player != null && !player.isAdminBypassActive() && kingdomManager.isChunkClaimed(event.getRightClicked().getLocation())) {
        	KonTerritory territory = kingdomManager.getChunkTerritory(event.getRightClicked().getLocation());
        	
        	ChatUtil.printDebug("Player interacted at entity of type: "+clicked.getType().toString());
        	boolean isEntityAllowed = (clicked.getType().equals(EntityType.PLAYER));
        	
        	// Capital protections...
        	boolean isCapitalUseEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.capital_use",false);
        	if(territory instanceof KonCapital && !isCapitalUseEnabled && !isEntityAllowed) {
    			//ChatUtil.sendNotice(player.getBukkitPlayer(), "You cannot do that in the Kingdom Capital", ChatColor.DARK_RED);
    			//ChatUtil.printDebug("EVENT player interaction within capital");
				ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
    			event.setCancelled(true);
    			return;
    		}
        	// Town protections...
    		if(territory instanceof KonTown && !isEntityAllowed) {
    			KonTown town = (KonTown) territory;
    			//ChatUtil.printDebug("EVENT player entity interaction within town "+town.getName());
    			// Prevent enemies and non-residents from interacting with entities
    			if(!player.getKingdom().equals(town.getKingdom()) || (!town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer()))) {
    				/*
    				EntityType eType = event.getRightClicked().getType();
    				//ChatUtil.printDebug("Player identified as enemy or non-resident, clicked entity "+eType.toString());
    				if(eType.equals(EntityType.ITEM_FRAME)) {
    					event.setCancelled(true);
						return;
    				}
    				*/
    				ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
    				event.setCancelled(true);
					return;
    			}
    		}
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
    	if(konquest.isWorldIgnored(event.getPlayer().getLocation())) {
			return;
		}
    	Player bukkitPlayer = event.getPlayer();
        KonPlayer player = playerManager.getPlayer(bukkitPlayer);
        if(player != null && !player.isAdminBypassActive() && kingdomManager.isChunkClaimed(event.getRightClicked().getLocation())) {
        	KonTerritory territory = kingdomManager.getChunkTerritory(event.getRightClicked().getLocation());
        	// Capital protections...
        	boolean isCapitalUseEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.capital_use",false);
        	if(territory instanceof KonCapital && !isCapitalUseEnabled) {
				ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
    			event.setCancelled(true);
    			return;
    		}
        }
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerFish(PlayerFishEvent event) {
    	if(!event.isCancelled()) {
    		if(konquest.isWorldIgnored(event.getPlayer().getLocation())) {
    			return;
    		}
    		KonPlayer player = konquest.getPlayerManager().getPlayer(event.getPlayer());
    		if(player != null && event.getState().equals(PlayerFishEvent.State.CAUGHT_FISH)) {
    			Entity caughtEntity = event.getCaught();
    			if(caughtEntity instanceof Item) {
    				Item caughtItem = (Item)caughtEntity;
    				Material caughtType = caughtItem.getItemStack().getType();
    				if(caughtType.equals(Material.COD) ||
    						caughtType.equals(Material.SALMON) ||
    						caughtType.equals(Material.TROPICAL_FISH) ||
    						caughtType.equals(Material.PUFFERFISH)) {
    					konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FISH,1);
    				} else if(caughtType.equals(Material.BOW) ||
    						caughtType.equals(Material.ENCHANTED_BOOK) ||
    						caughtType.equals(Material.FISHING_ROD) ||
    						caughtType.equals(Material.NAME_TAG) ||
    						caughtType.equals(Material.NAUTILUS_SHELL) ||
    						caughtType.equals(Material.SADDLE)) {
    					konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FISH,5);
    				}
    			}
    		}
    	}
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
    	if(!event.isCancelled()) {
    		if(konquest.isWorldIgnored(event.getPlayer().getLocation())) {
    			return;
    		}
    		KonPlayer player = konquest.getPlayerManager().getPlayer(event.getPlayer());
    		// Check for potion usage and update accomplishment
    		if(player != null && event.getItem().getType().equals(Material.POTION)) {
    			konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.POTIONS,1);
    		}
    	}
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
    	onBucketUse(event);
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
    	onBucketUse(event);
    }    
    
    private void onBucketUse(PlayerBucketEvent event) {
    	if(kingdomManager.isChunkClaimed(event.getBlock().getLocation())) {
			KonTerritory territory = kingdomManager.getChunkTerritory(event.getBlock().getLocation());
			// Prevent all players from placing or picking up liquids inside of monuments
			if(territory instanceof KonTown && ((KonTown) territory).isLocInsideCenterChunk(event.getBlock().getLocation())) {
				// The block is located inside a monument, cancel
				//ChatUtil.printDebug("EVENT: Bucket used inside monument with block "+event.getBlock().getType().toString()+", cancelling");
				event.setCancelled(true);
				return;
			}
			// Prevent enemy players from placing or picking up liquids inside of towns
			if(!konquest.getPlayerManager().isPlayer(event.getPlayer())) {
				ChatUtil.printDebug("Failed to handle onBucketUse for non-existent player");
				return;
			}
			KonPlayer player = konquest.getPlayerManager().getPlayer(event.getPlayer());
			if(territory instanceof KonTown && !player.getKingdom().equals(territory.getKingdom())) {
				// The block is located inside an enemy town, cancel
				//ChatUtil.printDebug("EVENT: Bucket used inside enemy town with block "+event.getBlock().getType().toString()+", cancelling");
				event.setCancelled(true);
				return;
			}
			// Prevent all bucket use inside of Ruins
			if(territory.getTerritoryType().equals(KonTerritoryType.RUIN)) {
				event.setCancelled(true);
				return;
			}
			// Prevent non-owners from using buckets in camps
			if(territory instanceof KonCamp) {
				KonCamp camp = (KonCamp)territory;
				if(!camp.isPlayerOwner(event.getPlayer())) {
					event.setCancelled(true);
					return;
				}
			}
		}
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
    	//ChatUtil.printDebug("EVENT: Player respawned");
    	Location currentLoc = event.getPlayer().getLocation();
    	Location respawnLoc = event.getRespawnLocation();
    	if(!konquest.getPlayerManager().isPlayer(event.getPlayer())) {
			ChatUtil.printDebug("Failed to handle onPlayerRespawn for non-existent player");
			return;
		}
    	KonPlayer player = playerManager.getPlayer(event.getPlayer());
    	// Send respawn to capital if no bed exists
    	if(!event.isBedSpawn()) {
    		if(!player.isBarbarian()) {
    			event.setRespawnLocation(player.getKingdom().getCapital().getSpawnLoc());
    		}
    	}
    	// Update town bars
    	//ChatUtil.printDebug("Player "+event.getPlayer().getName()+" died at "+currentLoc.toString());
    	//ChatUtil.printDebug("Player "+event.getPlayer().getName()+" respawns at "+respawnLoc.toString());
		if(kingdomManager.isChunkClaimed(currentLoc)) {
			KonTerritory territoryFrom = kingdomManager.getChunkTerritory(currentLoc);
			// Update bars
			if(territoryFrom.getTerritoryType().equals(KonTerritoryType.TOWN)) {
				((KonTown) territoryFrom).removeBarPlayer(player);
				player.clearAllMobAttackers();
				// Command all nearby Iron Golems to target nearby enemy players, ignore triggering player
				updateGolemTargetsForTerritory(territoryFrom,player,false);
			} else if(territoryFrom.getTerritoryType().equals(KonTerritoryType.RUIN)) {
				((KonRuin) territoryFrom).removeBarPlayer(player);
			} else if(territoryFrom.getTerritoryType().equals(KonTerritoryType.CAPITAL)) {
				((KonCapital) territoryFrom).removeBarPlayer(player);
			} else if(territoryFrom.getTerritoryType().equals(KonTerritoryType.CAMP)) {
				((KonCamp) territoryFrom).removeBarPlayer(player);
			}
			// Remove potion effects for all players
			kingdomManager.clearTownNerf(player);
		}
		if(kingdomManager.isChunkClaimed(respawnLoc)) {
			KonTerritory territoryTo = kingdomManager.getChunkTerritory(respawnLoc);
			// Update bars
			if(territoryTo.getTerritoryType().equals(KonTerritoryType.TOWN)) {
	    		((KonTown) territoryTo).addBarPlayer(player);
				updateGolemTargetsForTerritory(territoryTo,player,true);
			} else if (territoryTo.getTerritoryType().equals(KonTerritoryType.RUIN)) {
				((KonRuin) territoryTo).addBarPlayer(player);
			} else if (territoryTo.getTerritoryType().equals(KonTerritoryType.CAPITAL)) {
				((KonCapital) territoryTo).addBarPlayer(player);
			} else if (territoryTo.getTerritoryType().equals(KonTerritoryType.CAMP)) {
				((KonCamp) territoryTo).addBarPlayer(player);
			}
		}
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
    	if(konquest.isWorldIgnored(event.getPlayer().getLocation())) {
			return;
		}
    	//ChatUtil.printDebug("EVENT: Player exp changed");
    	Player bukkitPlayer = event.getPlayer();
    	KonPlayer player = playerManager.getPlayer(bukkitPlayer);
    	int boostPercent = konquest.getConfigManager().getConfig("core").getInt("core.kingdoms.smallest_exp_boost_percent");
    	if(boostPercent > 0 && player != null && player.getKingdom().isSmallest()) {
    		int baseAmount = event.getAmount();
    		int boostAmount = ((boostPercent*baseAmount)/100)+baseAmount;
    		//ChatUtil.printDebug("Boosting "+baseAmount+" exp for "+bukkitPlayer.getName()+" to "+boostAmount);
    		event.setAmount(boostAmount);
    	}
    }
    /*
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
    	// Prevent enemies from consuming milk buckets in Towns
    	if(event.getItem().getType().equals(Material.MILK_BUCKET) && konquest.getKingdomManager().isChunkClaimed(event.getPlayer().getLocation().getChunk())) {
    		KonTerritory territory = konquest.getKingdomManager().getChunkTerritory(event.getPlayer().getLocation().getChunk());
    		if(territory instanceof KonTown) {
    			KonPlayer player = playerManager.getPlayer(event.getPlayer());
    			if(!player.getKingdom().equals(territory.getKingdom())) {
    				ChatUtil.sendError(event.getPlayer(), "That milk cannot save you here!");
    				event.setCancelled(true);
    			}
    		}
    	}
    }*/
    /*
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
    	// Set a player's compass target to nearest enemy town
    	ItemStack heldItem = event.getPlayer().getInventory().getItemInMainHand();
    	if(heldItem.getType().equals(Material.COMPASS) && event.getPlayer().hasPermission("konquest.compass")) {
    		KonPlayer player = playerManager.getPlayer(event.getPlayer());
    		Location nearestEnemyTownLoc = kingdomManager.getClosestEnemyTown(event.getPlayer().getLocation().getChunk(),player.getKingdom()).getCenterLoc();
    		event.getPlayer().setCompassTarget(nearestEnemyTownLoc);
    	}
    }*/
    
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerPortal(PlayerPortalEvent event) {
    	Location portalToLoc = event.getTo();
    	// Ensure the portal event is not sending the player to a null location, like if the end is disabled
    	if(portalToLoc != null) {
	    	ChatUtil.printDebug("EVENT: Player portal to world "+portalToLoc.getWorld().getName()+" because "+event.getCause()+", location: "+portalToLoc.toString());
	    	Player bukkitPlayer = event.getPlayer();
	    	// When portal into valid world...
	    	if(konquest.isWorldValid(portalToLoc.getWorld())) {
				// Protections for territory
	    		if(konquest.getKingdomManager().isChunkClaimed(portalToLoc)) {
		    		KonTerritory territory = konquest.getKingdomManager().getChunkTerritory(portalToLoc);
					/*
					KonquestPortalTerritoryEvent invokeEvent = new KonquestPortalTerritoryEvent(konquest, kingdomManager.getChunkTerritory(event.getTo().getChunk()), event.getPortalTravelAgent());
		            Bukkit.getServer().getPluginManager().callEvent(invokeEvent);
					*/
					if(territory instanceof KonCapital) {
						boolean isPortalAllowed = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.capital_portal",false);
						if(!isPortalAllowed) {
							ChatUtil.printDebug("EVENT: Portal creation stopped inside of capital "+territory.getName());
							event.setCanCreatePortal(false);
							event.setCancelled(true);
							//ChatUtil.sendError(bukkitPlayer, "Your exit portal is inside the Capital of "+territory.getName()+", move your portal!");
							ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_PORTAL_EXIT.getMessage());
							return;
						}
					}
					if(territory instanceof KonTown) {
						KonTown town = (KonTown) territory;
						if(town.isLocInsideCenterChunk(event.getTo())) {
							ChatUtil.printDebug("EVENT: Portal creation stopped inside of town monument "+territory.getName());
							event.setCanCreatePortal(false);
							event.setCancelled(true);
							//ChatUtil.sendError(bukkitPlayer, "Your exit portal is inside the Monument of "+territory.getName()+", move your portal!");
							ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_PORTAL_EXIT.getMessage());
							return;
						}
					}
	    		}
			}
    	}
    }
    
    /**
     * Checks for players moving into chunks
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
    	onPlayerEnterLeaveChunk(event);
    }
    
    /**
     * Checks for players teleporting into chunks
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
    	// Check for inter-chunk ender pearl
    	boolean isEnemyPearlBlocked = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.no_enemy_ender_pearl", false);
    	boolean isTerritoryTo = kingdomManager.isChunkClaimed(event.getTo());
    	Player bukkitPlayer = event.getPlayer();
		if(!konquest.getPlayerManager().isPlayer(bukkitPlayer)) {
			//ChatUtil.printDebug("Failed to handle onPlayerEnterLeaveChunk for non-existent player");
			return;
		}
		KonPlayer player = playerManager.getPlayer(bukkitPlayer);
		// Inter-chunk checks
		if(isEnemyPearlBlocked && isTerritoryTo) {
			KonTerritory territoryTo = kingdomManager.getChunkTerritory(event.getTo());
			if(event.getCause().equals(TeleportCause.ENDER_PEARL) && !player.getKingdom().equals(territoryTo.getKingdom())) {
				ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
				event.setCancelled(true);
				return;
			}
		}
		// General chunk transition handler
    	onPlayerEnterLeaveChunk(event);
    }
    
    private void onPlayerEnterLeaveChunk(PlayerMoveEvent event) {
    	// Evaluate chunk territory transitions only when players move between chunks
    	// event.getTo().getWorld().equals(Bukkit.getWorld(konquest.getWorldName()))
    	if(!event.getTo().getChunk().equals(event.getFrom().getChunk()) || !event.getTo().getWorld().equals(event.getFrom().getWorld())) {
    		
    		Player bukkitPlayer = event.getPlayer();
    		if(!konquest.getPlayerManager().isPlayer(bukkitPlayer)) {
				//ChatUtil.printDebug("Failed to handle onPlayerEnterLeaveChunk for non-existent player");
				return;
			}
        	KonPlayer player = playerManager.getPlayer(bukkitPlayer);
        	Location chunkTo = event.getTo();
    		Location chunkFrom = event.getFrom();
    		boolean isTerritoryTo = kingdomManager.isChunkClaimed(chunkTo);
    		boolean isTerritoryFrom = kingdomManager.isChunkClaimed(chunkFrom);
    		KonTerritory territoryTo = null;
			KonTerritory territoryFrom = null;
			if(isTerritoryTo) {
				territoryTo = kingdomManager.getChunkTerritory(chunkTo);
			}
			if(isTerritoryFrom) {
				territoryFrom = kingdomManager.getChunkTerritory(chunkFrom);
			}
    		
    		if(event.getTo().getWorld().equals(event.getFrom().getWorld())) {
    			// Player moved within the same world
    			//long start = System.currentTimeMillis();
        		//String chunkCoordsTo = chunkTo.getX()+", "+chunkTo.getZ();
        		//String chunkCoordsFrom = chunkFrom.getX()+", "+chunkFrom.getZ();
        		
        		// Auto map
        		if(player.isMapAuto()) {
        			// Schedule delayed task to print map
        			Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
        	            @Override
        	            public void run() {
        	            	kingdomManager.printPlayerMap(player, KingdomManager.DEFAULT_MAP_SIZE, event.getTo());
        	            }
        	        },1);
        		}
        		//long step1 = System.currentTimeMillis();

        		//long step2 = System.currentTimeMillis();
        		
        		// Auto claiming
        		if(!isTerritoryTo) {
        			if(player.isAdminClaimingFollow()) {
        				// Admin claiming takes priority
        				kingdomManager.claimForAdmin(bukkitPlayer, event.getTo());
        				// Update territory variables for chunk boundary checks below
        				isTerritoryTo = kingdomManager.isChunkClaimed(chunkTo);
        				isTerritoryFrom = kingdomManager.isChunkClaimed(chunkFrom);
        			} else if(player.isClaimingFollow()) {
        				// Player is claim following
        				boolean isClaimSuccess = kingdomManager.claimForPlayer(bukkitPlayer, event.getTo());
            			if(!isClaimSuccess) {
            				player.setIsClaimingFollow(false);
            				//ChatUtil.sendNotice(bukkitPlayer, "Could not claim, disabled auto claim.");
            				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_CLAIM_NOTICE_FAIL_AUTO.getMessage());
            			} else {
            				ChatUtil.sendKonTitle(player, "", ChatColor.GREEN+MessagePath.COMMAND_CLAIM_NOTICE_PASS_AUTO.getMessage(), 15);
            			}
            			// Update territory variables for chunk boundary checks below
        				isTerritoryTo = kingdomManager.isChunkClaimed(chunkTo);
        				isTerritoryFrom = kingdomManager.isChunkClaimed(chunkFrom);
        			}
        		}
        		//long step3 = System.currentTimeMillis();
        		
        		// Border particle update
        		kingdomManager.updatePlayerBorderParticles(player,event.getTo());
        		//long step4 = System.currentTimeMillis();
        		
        		boolean isEnemyPearlBlocked = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.no_enemy_ender_pearl", false);
        		
        		// Chunk transition checks
        		if(!isTerritoryTo && isTerritoryFrom) { // When moving into the wild
        			// Prevent enemy ender pearl out of territory into wild (optional)
        			if(isEnemyPearlBlocked && event instanceof PlayerTeleportEvent) {
        				PlayerTeleportEvent tpEvent = (PlayerTeleportEvent)event;
        				if(tpEvent.getCause().equals(TeleportCause.ENDER_PEARL) && !player.getKingdom().equals(territoryFrom.getKingdom())) {
        					ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
        					event.setCancelled(true);
        					return;
        				}
        			}
        			// Display WILD
        			//ChatUtil.sendNotice(bukkitPlayer, "The Wild: "+chunkCoordsTo);
        			ChatUtil.sendKonTitle(player, "", MessagePath.GENERIC_NOTICE_WILD.getMessage());
        			//ChatUtil.printDebug("    Moved from Territory to Wild");
        			if(territoryFrom.getTerritoryType().equals(KonTerritoryType.TOWN)) {
        				((KonTown) territoryFrom).removeBarPlayer(player);
        				player.clearAllMobAttackers();
        				// Command all nearby Iron Golems to target nearby enemy players, ignore triggering player
    					updateGolemTargetsForTerritory(territoryFrom,player,false);
        			} else if(territoryFrom.getTerritoryType().equals(KonTerritoryType.RUIN)) {
        				((KonRuin) territoryFrom).removeBarPlayer(player);
        				((KonRuin) territoryFrom).stopTargetingPlayer(bukkitPlayer);
        			} else if(territoryFrom.getTerritoryType().equals(KonTerritoryType.CAPITAL)) {
        				((KonCapital) territoryFrom).removeBarPlayer(player);
        			} else if(territoryFrom.getTerritoryType().equals(KonTerritoryType.CAMP)) {
        				((KonCamp) territoryFrom).removeBarPlayer(player);
        			}
        			// Remove potion effects for all players
        			kingdomManager.clearTownNerf(player);
        			kingdomManager.clearTownHearts(player);
        			// Begin fly disable warmup
        			player.setFlyDisableWarmup(true);
        		} else if(isTerritoryTo && !isTerritoryFrom) { // When moving out of the wild
        			// TEST
        			//bukkitPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.GOLD+"Hello!"));
        			// END TEST
        			KonquestEnterTerritoryEvent invokeEvent = new KonquestEnterTerritoryEvent(konquest, player, kingdomManager.getChunkTerritory(chunkTo), event);
                    if(invokeEvent != null) {
                    	Bukkit.getServer().getPluginManager().callEvent(invokeEvent);
                    }
                    if(!event.isCancelled()) {
                    	// Prevent enemy ender pearl into territory from wild (optional)
            			if(isEnemyPearlBlocked && event instanceof PlayerTeleportEvent) {
            				PlayerTeleportEvent tpEvent = (PlayerTeleportEvent)event;
            				if(tpEvent.getCause().equals(TeleportCause.ENDER_PEARL) && !player.getKingdom().equals(territoryTo.getKingdom())) {
            					ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
            					event.setCancelled(true);
            					return;
            				}
            			}
                    	// Set message color based on enemy territory
    	                String color = ""+ChatColor.RED;
    	                if(territoryTo.getKingdom().equals(kingdomManager.getBarbarians())) {
    	                	color = ""+ChatColor.YELLOW;
    	                } else {
    	                	if(player.getKingdom().equals(territoryTo.getKingdom())) {
	    	                	color = ""+ChatColor.GREEN;
	    	                } else if(territoryTo.getKingdom().equals(kingdomManager.getNeutrals())) {
	    	                	color = ""+ChatColor.GRAY;
	    	                }
    	                }
    	                // Display Territory Name
    	    			String territoryName = territoryTo.getName();
    	    			//ChatUtil.sendNotice(bukkitPlayer, color+territoryName+": "+chunkCoordsTo);
    	    			ChatUtil.sendKonTitle(player, "", color+territoryName);
    	    			if(territoryTo.getTerritoryType().equals(KonTerritoryType.TOWN)) {
    	    				KonTown town = (KonTown) territoryTo;
    	    				town.addBarPlayer(player);
    	    				// Notify player if town is abandoned
    	    				if(town.getPlayerResidents().isEmpty() && town.getKingdom().equals(player.getKingdom())) {
    	    					//ChatUtil.sendNotice(bukkitPlayer, "This Town is abandoned! Use \"/k town "+territoryName+" lord "+bukkitPlayer.getName()+"\" to claim Lordship.",ChatColor.RED);
    	    					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(town.getName(),town.getName(),bukkitPlayer.getName()));
    	    				}
    	    				// Display plot message to friendly players
    	    				displayPlotMessage(town, chunkTo, chunkFrom, player);
    	    				// Command all nearby Iron Golems to target enemy player, if no other closer player is present
    						updateGolemTargetsForTerritory(territoryTo,player,true);
    	    			} else if(territoryTo.getTerritoryType().equals(KonTerritoryType.RUIN)) {
    	    				((KonRuin) territoryTo).addBarPlayer(player);
    	    			} else if(territoryTo.getTerritoryType().equals(KonTerritoryType.CAPITAL)) {
    	    				((KonCapital) territoryTo).addBarPlayer(player);
    	    			} else if(territoryTo.getTerritoryType().equals(KonTerritoryType.CAMP)) {
    	    				((KonCamp) territoryTo).addBarPlayer(player);
    	    			}
    	    			// Try to stop fly disable warmup, or disable immediately
    	    			if(territoryTo.getKingdom().equals(player.getKingdom())) {
    	    				player.setFlyDisableWarmup(false);
    	    			} else {
    	    				player.setIsFlyEnabled(false);
    	    			}
                    }
        		} else if(isTerritoryTo && isTerritoryFrom) { // When moving between two claimed territories
        			// Check for differing territories, if true then display new Territory Name and send message to enemies
        			if(!territoryTo.equals(territoryFrom)) { // moving between different territories
        				
        				// Prevent enemy ender pearl into territory from other territory (optional)
            			if(isEnemyPearlBlocked && event instanceof PlayerTeleportEvent) {
            				PlayerTeleportEvent tpEvent = (PlayerTeleportEvent)event;
            				if(tpEvent.getCause().equals(TeleportCause.ENDER_PEARL) && !player.getKingdom().equals(territoryTo.getKingdom())) {
            					ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
            					event.setCancelled(true);
            					return;
            				}
            			}
            			
        				// Set message color based on enemy territory
                        String color = ""+ChatColor.RED;
    	                if(territoryTo.getKingdom().equals(kingdomManager.getBarbarians())) {
    	                	color = ""+ChatColor.YELLOW;
    	                } else {
    	                	if(player.getKingdom().equals(territoryTo.getKingdom())) {
	    	                	color = ""+ChatColor.GREEN;
	    	                } else if(territoryTo.getKingdom().equals(kingdomManager.getNeutrals())) {
	    	                	color = ""+ChatColor.GRAY;
	    	                }
    	                }
                        
        				KonquestEnterTerritoryEvent invokeEvent = new KonquestEnterTerritoryEvent(konquest, player, kingdomManager.getChunkTerritory(chunkTo), event);
        	            Bukkit.getServer().getPluginManager().callEvent(invokeEvent);
        	            if(!event.isCancelled()) {
        	            	//ChatUtil.sendNotice(bukkitPlayer, color+territoryTo.getName()+": "+chunkCoordsTo);
        	            	ChatUtil.sendKonTitle(player, "", color+territoryTo.getName());
        	            	// Entry Territory
        	            	if(territoryTo.getTerritoryType().equals(KonTerritoryType.TOWN)) {
        	            		KonTown town = (KonTown) territoryTo;
        	            		town.addBarPlayer(player);
        	            		// Notify player if town is abandoned
        	    				if(town.getPlayerResidents().isEmpty() && town.getKingdom().equals(player.getKingdom())) {
        	    					//ChatUtil.sendNotice(bukkitPlayer, "This Town is abandoned! Use \"/k town "+territoryTo.getName()+" lord "+bukkitPlayer.getName()+"\" to claim Lordship.",ChatColor.RED);
        	    					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(town.getName(),town.getName(),bukkitPlayer.getName()));
        	    				}
        	    				// Display plot message to friendly players
        	    				displayPlotMessage(town, chunkTo, chunkFrom, player);
        	    				updateGolemTargetsForTerritory(territoryTo,player,true);
        	    			} else if (territoryTo.getTerritoryType().equals(KonTerritoryType.RUIN)) {
        	    				((KonRuin) territoryTo).addBarPlayer(player);
        	    			} else if (territoryTo.getTerritoryType().equals(KonTerritoryType.CAPITAL)) {
        	    				((KonCapital) territoryTo).addBarPlayer(player);
        	    			} else if (territoryTo.getTerritoryType().equals(KonTerritoryType.CAMP)) {
        	    				((KonCamp) territoryTo).addBarPlayer(player);
        	    			}
        	            	// Exit Territory
        	            	if(territoryFrom.getTerritoryType().equals(KonTerritoryType.TOWN)) {
        	    				((KonTown) territoryFrom).removeBarPlayer(player);
        	    				updateGolemTargetsForTerritory(territoryFrom,player,true);
        	    			} else if (territoryFrom.getTerritoryType().equals(KonTerritoryType.RUIN)) {
        	    				((KonRuin) territoryFrom).removeBarPlayer(player);
        	    				((KonRuin) territoryFrom).stopTargetingPlayer(bukkitPlayer);
        	    			} else if (territoryFrom.getTerritoryType().equals(KonTerritoryType.CAPITAL)) {
        	    				((KonCapital) territoryFrom).removeBarPlayer(player);
        	    			} else if (territoryFrom.getTerritoryType().equals(KonTerritoryType.CAMP)) {
        	    				((KonCamp) territoryFrom).removeBarPlayer(player);
        	    			}
        	            	//updateGolemTargetsForTerritory(territoryFrom,player,true);
        					//updateGolemTargetsForTerritory(territoryTo,player,true);
        	            	// Try to stop or start fly disable warmup
        	    			if(territoryTo.getKingdom().equals(player.getKingdom())) {
        	    				player.setFlyDisableWarmup(false);
        	    			} else {
        	    				player.setIsFlyEnabled(false);
        	    			}
        	            }
        			} else { // moving between the same territory
        				//ChatUtil.sendNotice(bukkitPlayer, "(Debug) "+territoryTo.getName()+": "+chunkCoordsTo);
        				
        				// Prevent enemy ender pearl into territory from same territory (optional)
            			if(isEnemyPearlBlocked && event instanceof PlayerTeleportEvent) {
            				PlayerTeleportEvent tpEvent = (PlayerTeleportEvent)event;
            				if(tpEvent.getCause().equals(TeleportCause.ENDER_PEARL) && !player.getKingdom().equals(territoryTo.getKingdom())) {
            					ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
            					event.setCancelled(true);
            					return;
            				}
            			}
            			
        				if(!event.isCancelled()) {
        					if(territoryTo.getTerritoryType().equals(KonTerritoryType.TOWN)) {
        						KonTown town = (KonTown) territoryTo;
        						if(!territoryTo.getKingdom().equals(player.getKingdom())) {
            						// Enemy player
        							kingdomManager.applyTownNerf(player, town);
            						updateGolemTargetsForTerritory(territoryTo,player,true);
            					} else {
            						// Friendly player
            						// Display plot message to friendly players
            						displayPlotMessage(town, chunkTo, chunkFrom, player);
            					}
        					}
        					//updateGolemTargetsForTerritory(territoryTo,player,true);
        				}
        			}
        			//ChatUtil.printDebug("    Moved from Territory to Territory");
        			
        		} else { // Otherwise, moving between Wild chunks
        			//ChatUtil.sendNotice(bukkitPlayer, "(Debug) The Wild: "+chunkCoordsTo);
        			//ChatUtil.printDebug("    Moved from Wild to Wild");
        		}
        		//long step5 = System.currentTimeMillis();
        		//                                         Auto map ms       compass ms         auto claim ms   border particle ms    chunk transition ms
        		//                                            0                  23 - 30           0                 0 - 2                0
        		//ChatUtil.printDebug("Chunk transition: "+(step1-start)+", "+(step2-step1)+", "+(step3-step2)+", "+(step4-step3)+", "+(step5-step4));
        	
    			
    		//} else if(event.getFrom().getWorld().equals(Bukkit.getWorld(konquest.getWorldName()))){
    		} else {
    			// Player moved between worlds
    			
    			if(player.isAdminClaimingFollow()) {
    				player.setIsAdminClaimingFollow(false);
    				//ChatUtil.sendNotice(bukkitPlayer, "Could not claim, disabled auto claim.");
    				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_CLAIM_NOTICE_FAIL_AUTO.getMessage());
    			}
    			if(player.isClaimingFollow()) {
    				player.setIsClaimingFollow(false);
    				//ChatUtil.sendNotice(bukkitPlayer, "Could not claim, disabled auto claim.");
    				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_CLAIM_NOTICE_FAIL_AUTO.getMessage());
    			}
    			if(player.isMapAuto()) {
    				player.setIsMapAuto(false);
    			}
    			// Disable flying
    			player.setIsFlyEnabled(false);
    			
    			//kingdomManager.stopPlayerBorderParticles(player);
    			kingdomManager.updatePlayerBorderParticles(player,chunkTo);
    			
    			if(isTerritoryFrom) {
    				if(territoryFrom.getTerritoryType().equals(KonTerritoryType.TOWN)) {
        				((KonTown) territoryFrom).removeBarPlayer(player);
        				player.clearAllMobAttackers();
        				// Command all nearby Iron Golems to target nearby enemy players, ignore triggering player
    					updateGolemTargetsForTerritory(territoryFrom,player,false);
        			} else if(territoryFrom.getTerritoryType().equals(KonTerritoryType.RUIN)) {
        				((KonRuin) territoryFrom).removeBarPlayer(player);
        				((KonRuin) territoryFrom).stopTargetingPlayer(bukkitPlayer);
        			} else if(territoryFrom.getTerritoryType().equals(KonTerritoryType.CAPITAL)) {
        				((KonCapital) territoryFrom).removeBarPlayer(player);
        			} else if(territoryFrom.getTerritoryType().equals(KonTerritoryType.CAMP)) {
        				((KonCamp) territoryFrom).removeBarPlayer(player);
        			}
        			// Remove potion effects for all players
        			kingdomManager.clearTownNerf(player);
        			kingdomManager.clearTownHearts(player);
    			}
    			
    			if(isTerritoryTo) {
    				KonquestEnterTerritoryEvent invokeEvent = new KonquestEnterTerritoryEvent(konquest, player, kingdomManager.getChunkTerritory(chunkTo), event);
                    if(invokeEvent != null) {
                    	Bukkit.getServer().getPluginManager().callEvent(invokeEvent);
                    }
                    if(!event.isCancelled()) {
    	                // Set message color based on enemy territory
    	                String color = ""+ChatColor.RED;
    	                if(territoryTo.getKingdom().equals(kingdomManager.getBarbarians())) {
    	                	color = ""+ChatColor.YELLOW;
    	                } else {
    	                	if(player.getKingdom().equals(territoryTo.getKingdom())) {
	    	                	color = ""+ChatColor.GREEN;
	    	                } else if(territoryTo.getKingdom().equals(kingdomManager.getNeutrals())) {
	    	                	color = ""+ChatColor.GRAY;
	    	                }
    	                }
    	                // Display Territory Name
    	    			String territoryName = territoryTo.getName();
    	    			//ChatUtil.sendNotice(bukkitPlayer, color+territoryName+": "+chunkCoordsTo);
    	    			ChatUtil.sendKonTitle(player, "", color+territoryName);
    	    			if(territoryTo.getTerritoryType().equals(KonTerritoryType.TOWN)) {
    	    				KonTown town = (KonTown) territoryTo;
    	    				town.addBarPlayer(player);
    	    				// Notify player if town is abandoned
    	    				if(town.getPlayerResidents().isEmpty() && town.getKingdom().equals(player.getKingdom())) {
    	    					//ChatUtil.sendNotice(bukkitPlayer, "This Town is abandoned! Use \"/k town "+territoryName+" lord "+bukkitPlayer.getName()+"\" to claim Lordship.",ChatColor.RED);
    	    					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(town.getName(),town.getName(),bukkitPlayer.getName()));
    	    				}
    	    				// Display plot message to friendly players
    	    				displayPlotMessage(town, chunkTo, chunkFrom, player);
    	    				// Command all nearby Iron Golems to target enemy player, if no other closer player is present
    						updateGolemTargetsForTerritory(territoryTo,player,true);
    	    			} else if(territoryTo.getTerritoryType().equals(KonTerritoryType.RUIN)) {
    	    				((KonRuin) territoryTo).addBarPlayer(player);
    	    			} else if(territoryTo.getTerritoryType().equals(KonTerritoryType.CAPITAL)) {
    	    				((KonCapital) territoryTo).addBarPlayer(player);
    	    			} else if(territoryTo.getTerritoryType().equals(KonTerritoryType.CAMP)) {
    	    				((KonCamp) territoryTo).addBarPlayer(player);
    	    			}
                    }
    			}
    		}
    	}
    }
    
    private void updateGolemTargetsForTerritory(KonTerritory territory, KonPlayer triggerPlayer, boolean useDefault) {
    	// Command all nearby Iron Golems to target closest player, if enemy exists nearby, else don't change target
    	// Find iron golems within the town max radius
    	boolean isGolemAttackEnemies = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.golem_attack_enemies");
		if(isGolemAttackEnemies && !triggerPlayer.isAdminBypassActive() && !triggerPlayer.getKingdom().equals(territory.getKingdom())) {
			Location centerLoc = territory.getCenterLoc();
			int golumSearchRange = konquest.getConfigManager().getConfig("core").getInt("core.towns.max_size",1); // chunks
			int radius = 16*16;
			if(golumSearchRange > 1) {
				radius = golumSearchRange*16;
			}
			for(Entity e : centerLoc.getWorld().getNearbyEntities(centerLoc,radius,256,radius,(e) -> e.getType() == EntityType.IRON_GOLEM)) {
				IronGolem golem = (IronGolem)e;
				// Check for golem inside given territory or in wild
				if(territory.isLocInside(golem.getLocation()) || !kingdomManager.isChunkClaimed(golem.getLocation())) {
					
					// Check for closest enemy player
					boolean isNearbyPlayer = false;
					double minDistance = 99;
					KonPlayer nearestPlayer = null;
					for(Entity p : golem.getNearbyEntities(32,32,32)) {
						if(p instanceof Player) {
							KonPlayer nearbyPlayer = playerManager.getPlayer((Player)p);
							if(nearbyPlayer != null && !nearbyPlayer.isAdminBypassActive() && !nearbyPlayer.getKingdom().equals(territory.getKingdom()) && territory.isLocInside(p.getLocation()) &&
									(useDefault || !nearbyPlayer.equals(triggerPlayer))) {
								double distance = golem.getLocation().distance(p.getLocation());
								if(distance < minDistance) {
									minDistance = distance;
									isNearbyPlayer = true;
									nearestPlayer = nearbyPlayer;
								}
							}
						}
					}
					
					// Attempt to remove current target
					LivingEntity currentTarget = golem.getTarget();
					//ChatUtil.printDebug("Golem: Evaluating new targets in territory "+territory.getName());
					if(currentTarget != null && currentTarget instanceof Player) {
						KonPlayer previousTargetPlayer = playerManager.getPlayer((Player)currentTarget);
						if(previousTargetPlayer != null) {
							previousTargetPlayer.removeMobAttacker(golem);
							//ChatUtil.printDebug("Golem: Removed mob attacker from player "+previousTargetPlayer.getBukkitPlayer().getName());
						}
					} else {
						//ChatUtil.printDebug("Golem: Bad current target");
					}
					
					// Attempt to apply new target, either closest player or default trigger player
					if(isNearbyPlayer) {
						//ChatUtil.printDebug("Golem: Found nearby player "+nearestPlayer.getBukkitPlayer().getName());
						golem.setTarget(nearestPlayer.getBukkitPlayer());
						nearestPlayer.addMobAttacker(golem);
					} else if(useDefault){
						//ChatUtil.printDebug("Golem: Targeting default player "+triggerPlayer.getBukkitPlayer().getName());
						golem.setTarget(triggerPlayer.getBukkitPlayer());
						triggerPlayer.addMobAttacker(golem);
					}
					
				} else {
					ChatUtil.printDebug("Golem: Not in this territory or wild");
				}
			}
		}
    }
    
    private void displayPlotMessage(KonTown town, Location toLoc, Location fromLoc, KonPlayer player) {
    	if(town.getKingdom().equals(player.getKingdom())) {
    		// Player is friendly
    		boolean isPlotTo = town.hasPlot(toLoc);
    		boolean isPlotFrom = town.hasPlot(fromLoc);
    		/*
    		 * Display messages:
    		 * 		into plot from non-plot (territory or wild)
    		 * 		into plot from other plot
    		 * 		out of plot, to town
    		 * 	    out of wild, to town
    		 */
    		String plotMessage = "";
    		ChatColor plotMessageColor = ChatColor.GOLD;
    		boolean doDisplay = false;
    		// Display conditions
    		if(isPlotTo) {
    			KonPlot plotTo = town.getPlot(toLoc);
    			if(!isPlotFrom || (isPlotFrom && !plotTo.equals(town.getPlot(fromLoc)))) {
    				plotMessage = plotTo.getDisplayText();
    				plotMessageColor = ChatColor.GOLD;
    				if(plotTo.hasUser(player.getBukkitPlayer())) {
    					plotMessageColor = ChatColor.DARK_GREEN;
    				}
    				doDisplay = true;
    			}
    		} else {
    			if((isPlotFrom || !town.isLocInside(fromLoc)) && town.isLocInside(toLoc)) {
    				// Moved out of plot or other territory or wild into town land
    				plotMessage = MessagePath.MENU_PLOTS_TOWN_LAND.getMessage();
    				plotMessageColor = ChatColor.DARK_GREEN;
    				doDisplay = true;
    			}
    		}
    		// Display message
    		if(doDisplay) {
    			player.getBukkitPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(plotMessageColor+plotMessage));
    		}
    	}
    }
    
    private void preventUse(PlayerInteractEvent event, KonPlayer player) {
    	if(event.getAction().equals(Action.PHYSICAL)) {
			// Prevent all physical stepping interaction
			event.setUseInteractedBlock(Event.Result.DENY);
			ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
		} else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			// Prevent use of specific usable blocks
			BlockState clickedState = event.getClickedBlock().getState();
			BlockData clickedBlockData = clickedState.getBlockData();
			//ChatUtil.printDebug("  checking block data of type "+clickedBlockData.getMaterial());
			boolean sendAdminHint = false;
			if(clickedBlockData instanceof Door ||
					clickedBlockData instanceof Gate ||
					clickedBlockData instanceof Switch ||
					clickedBlockData instanceof TrapDoor) {
				event.setUseInteractedBlock(Event.Result.DENY);
				ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
				sendAdminHint = true;
			} else if(clickedState.getType().isInteractable()) {
				event.setUseInteractedBlock(Event.Result.DENY);
				ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
				sendAdminHint = true;
			}
			if(sendAdminHint && event.getPlayer().hasPermission("konquest.command.admin")) {
				ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_IGNORE.getMessage());
			}
		}
    }
    
}
