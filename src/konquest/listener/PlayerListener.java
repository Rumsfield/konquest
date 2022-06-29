package konquest.listener;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.api.event.territory.KonquestTerritoryMoveEvent;
import konquest.api.model.KonquestTerritoryType;
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
import konquest.model.KonTown;
import konquest.model.KonPlayer.RegionType;
import konquest.model.KonPlot;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;
import konquest.utility.Timer;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
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
import org.bukkit.entity.Vehicle;
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
import org.bukkit.event.player.PlayerDropItemEvent;
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
            		if(town.isPlayerKnight(bukkitPlayer)) {
            			for(OfflinePlayer invitee : town.getJoinRequests()) {
            				//ChatUtil.sendNotice(bukkitPlayer, invitee.getName()+" wants to join "+town.getName()+", use \"/k town "+town.getName()+" add "+invitee.getName()+"\" to allow, \"/k town "+town.getName()+" kick "+invitee.getName()+"\" to deny", ChatColor.LIGHT_PURPLE);
            				ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_JOIN_REQUEST.getMessage(invitee.getName(),town.getName(),town.getName(),invitee.getName(),town.getName(),invitee.getName()), ChatColor.LIGHT_PURPLE);
            			}
            		}
            	}
            	// DiscordSRV
            	if(konquest.getIntegrationManager().getDiscordSrv().isEnabled()) {
            		String message = konquest.getIntegrationManager().getDiscordSrv().getLinkMessage(bukkitPlayer);
            		ChatUtil.sendNotice(bukkitPlayer, message);
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
     * Cancel and re-throw chat events for global and kingdom modes.
     * @param event
     */
    private void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        //Check if the event was caused by a player
        if(event.isAsynchronous() && !event.isCancelled()) {
        	
        	boolean enable = konquest.getConfigManager().getConfig("core").getBoolean("core.chat.enable_format",true);
        	if(enable) {
	        	// Format chat messages
        		Player bukkitPlayer = event.getPlayer();
	        	if(!konquest.getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
					ChatUtil.printDebug("Failed to handle onAsyncPlayerChat for non-existent player");
					return;
				}
	            KonPlayer player = playerManager.getPlayer(bukkitPlayer);
	            KonKingdom kingdom = player.getKingdom();
	            
	            // Built-in format string
	            boolean formatNameConfig = konquest.getConfigManager().getConfig("core").getBoolean("core.chat.name_team_color",true);
	        	boolean formatKingdomConfig = konquest.getConfigManager().getConfig("core").getBoolean("core.chat.kingdom_team_color",true);
	            /* %TITLE% */
	            String title = "";
	            if(player.getPlayerPrefix().isEnabled()) {
	            	title = ChatUtil.parseHex(player.getPlayerPrefix().getMainPrefixName());
	            }
	            /* %PREFIX% */
	            String prefix = ChatUtil.parseHex(konquest.getIntegrationManager().getLuckPerms().getPrefix(bukkitPlayer));
	            /* %SUFFIX% */
	            String suffix = ChatUtil.parseHex(konquest.getIntegrationManager().getLuckPerms().getSuffix(bukkitPlayer));
	            /* %KINGDOM% */
	            String kingdomName = kingdom.getName();
	            /* %NAME% */
	            String name = bukkitPlayer.getName();
	        	
	        	String rawFormat = Konquest.getChatMessage();
	        	String parsedFormat = "";
	        	String chatMessage = event.getMessage();
	        	String chatChannel = "global";
	        	if(!player.isGlobalChat()) {
	        		chatChannel = player.getKingdom().getName();
	        	}
	        	
	        	// Send chat content to Discord first
	        	konquest.getIntegrationManager().getDiscordSrv().sendGameChatToDiscord(event.getPlayer(), event.getMessage(), chatChannel, event.isCancelled());
	        	// Then cancel the event. This causes DiscordSRV to not process the event on its own.
	        	// We need to cancel the event so it doesn't execute on the server, and we can send custom format messages to players.
	        	event.setCancelled(true);
	        	
	        	// Send messages to players
	        	for(KonPlayer viewerPlayer : playerManager.getPlayersOnline()) {
	        		ChatColor teamColor = ChatColor.GOLD;
	        		ChatColor titleColor = ChatColor.GOLD;
	        		boolean doFormatName = true;
	        		boolean doFormatKingdom = true;
	        		String messageFormat = "";
	        		
	        		boolean sendMessage = false;
	        		if(player.isGlobalChat()) {
	        			// Sender is in global chat mode
	        			teamColor = konquest.getDisplayPrimaryColor(viewerPlayer, player);
	            		titleColor = konquest.getDisplaySecondaryColor(viewerPlayer, player);
	            		doFormatName = formatNameConfig;
	            		doFormatKingdom = formatKingdomConfig;
	            		messageFormat = "";
	            		sendMessage = true;
	        		} else {
	        			// Sender is in kingdom chat mode
	        			if(viewerPlayer.getKingdom().equals(kingdom)) {
	        				teamColor = Konquest.friendColor1;
		            		titleColor = Konquest.friendColor1;
		            		doFormatName = true;
		            		doFormatKingdom = true;
		            		messageFormat = ""+ChatColor.GREEN+ChatColor.ITALIC;
		            		sendMessage = true;
	        			} else if(viewerPlayer.isAdminBypassActive()) {
	        				teamColor = ChatColor.GOLD;
		            		titleColor = ChatColor.GOLD;
		            		doFormatName = true;
		            		doFormatKingdom = true;
		            		messageFormat = ""+ChatColor.GOLD+ChatColor.ITALIC;
		            		sendMessage = true;
	        			}
	        		}
	        		
	        		if(sendMessage) {
	        			// Parse built-in placeholders
	        			parsedFormat = ChatUtil.parseFormat(rawFormat,
        						prefix,
        						suffix,
        						kingdomName,
        						title,
        						name,
        						teamColor,
        						titleColor,
        						doFormatName,
        						doFormatKingdom);
	        			// Attempt to use PAPI for external placeholders
	        			try {
	    	        		// Try to parse placeholders in the format string, if the JAR is present.
	    	        		parsedFormat = PlaceholderAPI.setPlaceholders(bukkitPlayer, parsedFormat);
	    	        		// Try to parse relational placeholders
	    	        		parsedFormat = PlaceholderAPI.setRelationalPlaceholders(viewerPlayer.getBukkitPlayer(), bukkitPlayer, parsedFormat);
	    	        	} catch (Exception ignored) {}
	        			// Send the chat message
		        		viewerPlayer.getBukkitPlayer().sendMessage(parsedFormat + Konquest.chatDivider + ChatColor.RESET + " " + messageFormat + chatMessage);
	        		}
	        	}
	        	
	        	// Send message to console
	        	if(player.isGlobalChat()) {
	        		ChatUtil.printConsole(ChatColor.GOLD + bukkitPlayer.getName()+": "+ChatColor.DARK_GRAY+event.getMessage());
	        	} else {
	        		ChatUtil.printConsole(ChatColor.GOLD + "["+kingdom.getName()+"] "+bukkitPlayer.getName()+": "+ChatColor.DARK_GRAY+event.getMessage());
	        	}
	        	
	        	/*
	        	// Original chat message replacement
	            if(player.isGlobalChat()) {
	            	//Global chat, all players see this format
	            	ChatUtil.printConsole(ChatColor.GOLD + kingdom.getName() + " | " + bukkitPlayer.getName()+": "+ChatColor.DARK_GRAY+event.getMessage());
	            	for(KonPlayer globalPlayer : playerManager.getPlayersOnline()) {
	            		ChatColor teamColor = konquest.getDisplayPrimaryColor(globalPlayer, player);
	            		ChatColor titleColor = konquest.getDisplaySecondaryColor(globalPlayer, player);
	            		globalPlayer.getBukkitPlayer().sendMessage(
	            				ChatUtil.parseFormat(Konquest.getChatMessage(),
	            						prefix,
	            						suffix,
	            						kingdomName,
	            						title,
	            						name,
	            						teamColor,
	            						titleColor,
	            						formatNameConfig,
	            						formatKingdomConfig) +
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
		            						Konquest.friendColor1,
		            						Konquest.friendColor1,
		            						true,
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
		            						true,
		            						true) +
		            				Konquest.chatDivider + ChatColor.RESET + " " + ChatColor.GOLD+ChatColor.ITALIC+event.getMessage());
	            		}
	            	}
	            }
	            */
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
//    	if(konquest.isWorldIgnored(event.getPlayer().getLocation().getWorld())) {
//			return;
//		}
    	Player bukkitPlayer = event.getPlayer();
    	if(!konquest.getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
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
	                    ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_CREATE_2.getMessage(), ChatColor.LIGHT_PURPLE);
	                } else if (player.getRegionCornerTwoBuffer() == null) {
	                	player.setRegionCornerTwoBuffer(location);
	                    //ChatUtil.sendNotice(bukkitPlayer, "Click on the travel point block.");
	                    ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_CREATE_3.getMessage(), ChatColor.LIGHT_PURPLE);
	                } else {
	                	KonKingdom kingdom = kingdomManager.getKingdom(player.getRegionKingdomName());
	                	int createMonumentStatus = kingdom.createMonumentTemplate(player.getRegionCornerOneBuffer(), player.getRegionCornerTwoBuffer(), location);
	                	switch(createMonumentStatus) {
	    				case 0:
	    					//ChatUtil.sendNotice(bukkitPlayer, "Successfully created new Monument Template for kingdom "+player.getRegionKingdomName());
	    					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_SUCCESS.getMessage(player.getRegionKingdomName()));
	    					kingdom.reloadLoadedTownMonuments();
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
	                    ChatUtil.printDebug("Finished setting monument region");
	                }
	        		break;
	        	case RUIN_CRITICAL:
	        		boolean validCriticalBlock = false;
	        		if(kingdomManager.isChunkClaimed(location)) {
	        			KonTerritory territory = kingdomManager.getChunkTerritory(location);
	        			if(territory.getTerritoryType().equals(KonquestTerritoryType.RUIN)) {
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
	        			if(territory.getTerritoryType().equals(KonquestTerritoryType.RUIN)) {
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
    	if(event.isCancelled()) {
    		// Do nothing if another plugin cancels this event
    		return;
    	}
    	Entity passenger = event.getEntered();
    	if(passenger instanceof Player) {
    		Player bukkitPlayer = (Player) passenger;
    		// Prevent entering vehicles in capitals
    		if(konquest.getKingdomManager().isChunkClaimed(event.getVehicle().getLocation())) {
    			KonPlayer player = konquest.getPlayerManager().getPlayer(bukkitPlayer);
        		KonTerritory territory = konquest.getKingdomManager().getChunkTerritory(event.getVehicle().getLocation());
        		if(player != null && territory != null && territory.getTerritoryType().equals(KonquestTerritoryType.CAPITAL)) {
        			boolean isEnemy = !territory.getKingdom().equals(player.getKingdom());
        			boolean isCapitalUseEnabled = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.capital_use",false);
        			if(isEnemy || (!isEnemy && !isCapitalUseEnabled)) {
    	    			ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
    	    			event.setCancelled(true);
    					return;
        			}
        		}
    		}
    		// General chunk transition handler
        	boolean status = onPlayerEnterLeaveChunk(event.getVehicle().getLocation(), bukkitPlayer.getLocation(), bukkitPlayer);
        	if(!status) {
        		event.setCancelled(true);
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
        	
        	ChatUtil.printDebug("Player "+bukkitPlayer.getName()+" interacted at entity of type: "+clicked.getType().toString());
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
			if(!konquest.getPlayerManager().isOnlinePlayer(event.getPlayer())) {
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
			if(territory.getTerritoryType().equals(KonquestTerritoryType.RUIN)) {
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
    	if(!konquest.getPlayerManager().isOnlinePlayer(event.getPlayer())) {
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
			if(territoryFrom.getTerritoryType().equals(KonquestTerritoryType.TOWN)) {
				((KonTown) territoryFrom).removeBarPlayer(player);
				boolean isArmisticeFrom = konquest.getGuildManager().isArmistice(player, (KonTown)territoryFrom);
				player.clearAllMobAttackers();
				// Command all nearby Iron Golems to target nearby enemy players, ignore triggering player
				updateGolemTargetsForTerritory(territoryFrom,player,false,isArmisticeFrom);
			} else if(territoryFrom.getTerritoryType().equals(KonquestTerritoryType.RUIN)) {
				((KonRuin) territoryFrom).removeBarPlayer(player);
			} else if(territoryFrom.getTerritoryType().equals(KonquestTerritoryType.CAPITAL)) {
				((KonCapital) territoryFrom).removeBarPlayer(player);
			} else if(territoryFrom.getTerritoryType().equals(KonquestTerritoryType.CAMP)) {
				((KonCamp) territoryFrom).removeBarPlayer(player);
			}
			// Remove potion effects for all players
			kingdomManager.clearTownNerf(player);
		}
		if(kingdomManager.isChunkClaimed(respawnLoc)) {
			KonTerritory territoryTo = kingdomManager.getChunkTerritory(respawnLoc);
			// Update bars
			if(territoryTo.getTerritoryType().equals(KonquestTerritoryType.TOWN)) {
				boolean isArmisticeTo = konquest.getGuildManager().isArmistice(player, (KonTown)territoryTo);
	    		((KonTown) territoryTo).addBarPlayer(player);
				updateGolemTargetsForTerritory(territoryTo,player,true,isArmisticeTo);
			} else if (territoryTo.getTerritoryType().equals(KonquestTerritoryType.RUIN)) {
				((KonRuin) territoryTo).addBarPlayer(player);
			} else if (territoryTo.getTerritoryType().equals(KonquestTerritoryType.CAPITAL)) {
				((KonCapital) territoryTo).addBarPlayer(player);
			} else if (territoryTo.getTerritoryType().equals(KonquestTerritoryType.CAMP)) {
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
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
    	if(konquest.getDisplayManager().isPlayerViewingMenu(event.getPlayer())) {
    		ChatUtil.printDebug("Player "+event.getPlayer().getName()+" tried to drop an item from an inventory menu!");
    		//event.setCancelled(true);
    		//event.getItemDrop().getItemStack().setType(Material.AIR);
    		//event.getItemDrop().remove();
    		//TODO: Destroy the item dropped from the menu GUI
    	}
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerPortal(PlayerPortalEvent event) {
    	if(event.isCancelled()) {
    		// Do nothing if another plugin cancels this event
    		return;
    	}
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
	    	// General chunk transition handler
	    	boolean status = onPlayerEnterLeaveChunk(event.getTo(), event.getFrom(), event.getPlayer());
	    	if(!status) {
	    		event.setCancelled(true);
	    	}
    	}
    }
    
    /**
     * Checks for players moving into chunks
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerMove(PlayerMoveEvent event) {
    	if(event.isCancelled()) {
    		// Do nothing if another plugin cancels this event
    		return;
    	}
    	// General chunk transition handler
    	boolean status = onPlayerEnterLeaveChunk(event.getTo(), event.getFrom(), event.getPlayer());
    	if(!status) {
    		event.setCancelled(true);
    	}
    }
    
    /**
     * Checks for players teleporting into chunks
     * @param event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
    	if(event.isCancelled()) {
    		// Do nothing if another plugin cancels this event
    		return;
    	}
    	// Check for inter-chunk ender pearl
    	boolean isEnemyPearlBlocked = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.no_enemy_ender_pearl", false);
    	boolean isTerritoryTo = kingdomManager.isChunkClaimed(event.getTo());
    	boolean isTerritoryFrom = kingdomManager.isChunkClaimed(event.getFrom());
    	Player bukkitPlayer = event.getPlayer();
		if(!konquest.getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
			//ChatUtil.printDebug("Failed to handle onPlayerEnterLeaveChunk for non-existent player");
			return;
		}
		KonPlayer player = playerManager.getPlayer(bukkitPlayer);
		// Inter-chunk checks
		// Prevent enemies teleporting to ender pearls thrown into enemy land or out of enemy land
		if(isEnemyPearlBlocked && event.getCause().equals(TeleportCause.ENDER_PEARL)) {
			boolean isEnemyTerritory = false;
			if(isTerritoryTo) {
				KonTerritory territoryTo = kingdomManager.getChunkTerritory(event.getTo());
				if(!player.getKingdom().equals(territoryTo.getKingdom())) {
					isEnemyTerritory = true;
				}
			}
			if(isTerritoryFrom) {
				KonTerritory territoryFrom = kingdomManager.getChunkTerritory(event.getFrom());
				if(!player.getKingdom().equals(territoryFrom.getKingdom())) {
					isEnemyTerritory = true;
				}
			}
			if(isEnemyTerritory) {
				ChatUtil.sendKonPriorityTitle(player, "", ChatColor.DARK_RED+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
				event.setCancelled(true);
				return;
			}
		}
		// General chunk transition handler
    	boolean status = onPlayerEnterLeaveChunk(event.getTo(), event.getFrom(), event.getPlayer());
    	if(!status) {
    		event.setCancelled(true);
    	}
    }
    
    // Returns false when parent event should be cancelled
    private boolean onPlayerEnterLeaveChunk(Location moveTo, Location moveFrom, Player movePlayer) {
    	// Evaluate chunk territory transitions only when players move between chunks
    	
    	// Check if player moved between chunks or worlds
    	if(!moveTo.getChunk().equals(moveFrom.getChunk()) || !moveTo.getWorld().equals(moveFrom.getWorld())) {
    		
    		if(!konquest.getPlayerManager().isOnlinePlayer(movePlayer)) {
				//ChatUtil.printDebug("Failed to handle onPlayerEnterLeaveChunk for non-existent player");
				return true;
			}
        	KonPlayer player = playerManager.getPlayer(movePlayer);
    		boolean isTerritoryTo = kingdomManager.isChunkClaimed(moveTo);
    		boolean isTerritoryFrom = kingdomManager.isChunkClaimed(moveFrom);
    		KonTerritory territoryTo = null;
			KonTerritory territoryFrom = null;
			if(isTerritoryTo) {
				territoryTo = kingdomManager.getChunkTerritory(moveTo);
			}
			if(isTerritoryFrom) {
				territoryFrom = kingdomManager.getChunkTerritory(moveFrom);
			}
			
			boolean isArmisticeTo = false; // Is the player in an armistice with the to-territory?
			boolean isArmisticeFrom = false; // Is the player in an armistice with the from-territory?
			
			// Fire event when either entering or leaving a territory
			if(isTerritoryTo || isTerritoryFrom) {
	    		KonquestTerritoryMoveEvent invokeEvent = new KonquestTerritoryMoveEvent(konquest, territoryTo, territoryFrom, player);
	    		Konquest.callKonquestEvent(invokeEvent);
	    		if(invokeEvent.isCancelled()) {
	    			return false;
	    		}
			}
			
			// Check for armistice conditions
    		if(isTerritoryTo && territoryTo instanceof KonTown) {
				isArmisticeTo = konquest.getGuildManager().isArmistice(player, (KonTown)territoryTo);
			}
			if(isTerritoryFrom && territoryFrom instanceof KonTown) {
				isArmisticeFrom = konquest.getGuildManager().isArmistice(player, (KonTown)territoryFrom);
			}
    		
			// Check world transition
    		if(moveTo.getWorld().equals(moveFrom.getWorld())) {
    			// Player moved within the same world
        		
        		// Chunk transition checks
        		if(!isTerritoryTo && isTerritoryFrom) { // When moving into the wild
        			// Display WILD
        			ChatUtil.sendKonTitle(player, "", MessagePath.GENERIC_NOTICE_WILD.getMessage());
        			
        			// Do things appropriate to the type of territory
        			onExitTerritory(territoryFrom,player,isArmisticeFrom);

        			// Remove potion effects for all players
        			kingdomManager.clearTownNerf(player);
        			
        			// Begin fly disable warmup
        			player.setFlyDisableWarmup(true);
        			
        		} else if(isTerritoryTo && !isTerritoryFrom) { // When moving out of the wild
        			// Check if entry is allowed
        			if(!isAllowedEnterTerritory(territoryTo,player)) {
        				return false;
        			}
        			
        			// Set message color based on enemy territory
        			ChatColor color = konquest.getDisplayPrimaryColor(player, territoryTo);

	                // Display Territory Name
	    			ChatUtil.sendKonTitle(player, "", color+territoryTo.getName());
	    			
	    			// Do things appropriate to the type of territory
	    			onEnterTerritory(territoryTo,moveTo,moveFrom,player,isArmisticeTo);
	    			
	    			// Try to stop fly disable warmup, or disable immediately
	    			if(territoryTo.getKingdom().equals(player.getKingdom())) {
	    				player.setFlyDisableWarmup(false);
	    			} else {
	    				player.setIsFlyEnabled(false);
	    			}
	    			
        		} else if(isTerritoryTo && isTerritoryFrom) { // When moving between two claimed territories
        			// Check for differing territories, if true then display new Territory Name and send message to enemies
        			if(!territoryTo.equals(territoryFrom)) { // moving between different territories
        				// Check if entry is allowed
            			if(!isAllowedEnterTerritory(territoryTo,player)) {
            				return false;
            			}
        				
        				// Set message color based on enemy territory
            			ChatColor color = konquest.getDisplayPrimaryColor(player, territoryTo);
    	            	ChatUtil.sendKonTitle(player, "", color+territoryTo.getName());
    	            	
    	            	// Do things appropriate to the type of territory
    	    			// Exit Territory
            			onExitTerritory(territoryFrom,player,isArmisticeFrom);
            			// Entry Territory
    	    			onEnterTerritory(territoryTo,moveTo,moveFrom,player,isArmisticeTo);
    	    			
    	            	// Try to stop or start fly disable warmup
    	    			if(territoryTo.getKingdom().equals(player.getKingdom())) {
    	    				player.setFlyDisableWarmup(false);
    	    			} else {
    	    				player.setIsFlyEnabled(false);
    	    			}
    	    			
        			} else { // moving between the same territory
        				if(territoryTo.getTerritoryType().equals(KonquestTerritoryType.TOWN)) {
    						KonTown town = (KonTown) territoryTo;
    						if(!territoryTo.getKingdom().equals(player.getKingdom())) {
    							// If the town and enemy guilds share an armistice
    							if(!isArmisticeTo) {
    								// Enemy player
        							kingdomManager.applyTownNerf(player, town);
    							}
    							updateGolemTargetsForTerritory(territoryTo,player,true,isArmisticeTo);
        					} else {
        						// Friendly player
        						// Display plot message to friendly players
        						displayPlotMessage(town, moveTo, moveFrom, player);
        					}
    					}
        			}
        		} else { // Otherwise, moving between Wild chunks
        			//ChatUtil.sendNotice(bukkitPlayer, "(Debug) The Wild: "+chunkCoordsTo);
        			//ChatUtil.printDebug("    Moved from Wild to Wild");
        		}
        		
        		// Auto map
        		if(player.isMapAuto()) {
        			// Schedule delayed task to print map
        			Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), new Runnable() {
        	            @Override
        	            public void run() {
        	            	kingdomManager.printPlayerMap(player, KingdomManager.DEFAULT_MAP_SIZE, moveTo);
        	            }
        	        },1);
        		}
        		
        		// Auto claiming
        		if(!isTerritoryTo) {
        			if(player.isAdminClaimingFollow()) {
        				// Admin claiming takes priority
        				kingdomManager.claimForAdmin(movePlayer, moveTo);
        				// Update territory variables for chunk boundary checks below
        				isTerritoryTo = kingdomManager.isChunkClaimed(moveTo);
        				isTerritoryFrom = kingdomManager.isChunkClaimed(moveFrom);
        			} else if(player.isClaimingFollow()) {
        				// Player is claim following
        				boolean isClaimSuccess = kingdomManager.claimForPlayer(movePlayer, moveTo);
            			if(!isClaimSuccess) {
            				player.setIsClaimingFollow(false);
            				//ChatUtil.sendNotice(bukkitPlayer, "Could not claim, disabled auto claim.");
            				ChatUtil.sendNotice(movePlayer, MessagePath.COMMAND_CLAIM_NOTICE_FAIL_AUTO.getMessage());
            			} else {
            				ChatUtil.sendKonTitle(player, "", ChatColor.GREEN+MessagePath.COMMAND_CLAIM_NOTICE_PASS_AUTO.getMessage(), 15);
            			}
            			// Update territory variables for chunk boundary checks below
        				isTerritoryTo = kingdomManager.isChunkClaimed(moveTo);
        				isTerritoryFrom = kingdomManager.isChunkClaimed(moveFrom);
        			}
        		}
        		
    		} else {
    			// Player moved between worlds
    			
    			// Check if entry is allowed
    			if(isTerritoryTo && !isAllowedEnterTerritory(territoryTo,player)) {
    				return false;
    			}
    			
    			// Disable movement-based flags
    			if(player.isAdminClaimingFollow()) {
    				player.setIsAdminClaimingFollow(false);
    				//ChatUtil.sendNotice(bukkitPlayer, "Could not claim, disabled auto claim.");
    				ChatUtil.sendNotice(movePlayer, MessagePath.COMMAND_CLAIM_NOTICE_FAIL_AUTO.getMessage());
    			}
    			if(player.isClaimingFollow()) {
    				player.setIsClaimingFollow(false);
    				//ChatUtil.sendNotice(bukkitPlayer, "Could not claim, disabled auto claim.");
    				ChatUtil.sendNotice(movePlayer, MessagePath.COMMAND_CLAIM_NOTICE_FAIL_AUTO.getMessage());
    			}
    			if(player.isMapAuto()) {
    				player.setIsMapAuto(false);
    			}
    			
    			// Disable flying
    			player.setIsFlyEnabled(false);
        		
    			if(isTerritoryFrom) {
    				onExitTerritory(territoryFrom,player,isArmisticeFrom);
        			// Remove potion effects for all players
        			kingdomManager.clearTownNerf(player);
    			}
    			
    			if(isTerritoryTo) {
	                // Set message color based on enemy territory
        			ChatColor color = konquest.getDisplayPrimaryColor(player, territoryTo);
	                // Display Territory Name
	    			String territoryName = territoryTo.getName();
	    			ChatUtil.sendKonTitle(player, "", color+territoryName);
	    			// Do things appropriate to the type of territory
	    			onEnterTerritory(territoryTo,moveTo,moveFrom,player,isArmisticeTo);
    			}
    		}
    		
    		// Border particle update
    		kingdomManager.updatePlayerBorderParticles(player,moveTo);
    	}
    	return true;
    }
    
 // Return true to allow entry, else false to deny entry
    private boolean isAllowedEnterTerritory(KonTerritory territoryTo, KonPlayer player) {
    	if(territoryTo == null) {
    		// Unknown territory, just allow it
    		return true;
    	}
		// Decide what to do for specific territories
		switch(territoryTo.getTerritoryType()) {
			case TOWN:
				// Always allow entry
				break;
				
			case RUIN:
				// Always allow entry
				break;
				
			case CAPITAL:
				KonCapital capital = (KonCapital) territoryTo;
				// Optionally prevent players from entering
				boolean isEnemyAllowedDenied = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.no_enemy_enter");
				boolean isAdminBypassMode = player.isAdminBypassActive();
				if(!isAdminBypassMode && isEnemyAllowedDenied && !player.getKingdom().equals(capital.getKingdom())) {
					// When Player is in a vehicle, reverse the velocity and eject
					if(player.getBukkitPlayer().isInsideVehicle()) {
						Vehicle vehicle = (Vehicle) player.getBukkitPlayer().getVehicle();
						vehicle.setVelocity(vehicle.getVelocity().multiply(-4));
						vehicle.eject();
					}
					// Cancel the movement
					//ChatUtil.sendNotice(event.getPlayer().getBukkitPlayer(), "Cannot enter enemy Kingdom Capitals"+adminText, ChatColor.DARK_RED);
					ChatUtil.sendError(player.getBukkitPlayer(), MessagePath.PROTECTION_ERROR_CAPITAL_ENTER.getMessage());
					if(player.getBukkitPlayer().hasPermission("konquest.command.admin")) {
						ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_IGNORE.getMessage());
					}
					return false;
				}
				break;
				
			case CAMP:
				// Always allow entry
				break;
				
			default:
				break;
		}
		return true;
    }
    
    
    // Return true to allow entry, else false to deny entry
    private void onEnterTerritory(KonTerritory territoryTo, Location locTo, Location locFrom, KonPlayer player, boolean isArmisticeTo) {
    	if(territoryTo == null) {
    		return;
    	}
		// Decide what to do for specific territories
		switch(territoryTo.getTerritoryType()) {
			case TOWN:
				KonTown town = (KonTown) territoryTo;
				town.addBarPlayer(player);
				// Notify player if town is abandoned
				if(town.getPlayerResidents().isEmpty() && town.getKingdom().equals(player.getKingdom())) {
					ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(town.getName(),town.getName(),player.getBukkitPlayer().getName()));
				}
				// Display plot message to friendly players
				displayPlotMessage(town, locTo, locFrom, player);
				// Command all nearby Iron Golems to target enemy player, if no other closer player is present
				updateGolemTargetsForTerritory(territoryTo,player,true,isArmisticeTo);
				// Try to apply heart adjustments
				kingdomManager.applyTownHearts(player,town);
				// For an enemy player...
				if(!player.isAdminBypassActive() && !player.getBukkitPlayer().getGameMode().equals(GameMode.SPECTATOR) &&
						!player.getKingdom().equals(town.getKingdom()) && !player.getKingdom().isPeaceful() ) {
					// When there is no armistice...
					if(!isArmisticeTo) {
						// Attempt to start a raid alert
						town.sendRaidAlert();
						// Apply town nerfs
						kingdomManager.applyTownNerf(player, town);
					}
				} else {
					// Players entering friendly towns...
					kingdomManager.clearTownNerf(player);
				}
				break;
				
			case RUIN:
				KonRuin ruin = (KonRuin)territoryTo;
				// Add player to territory bar
				ruin.addBarPlayer(player);
				// Spawn all ruin golems
				ruin.spawnAllGolems();
				break;
				
			case CAPITAL:
				KonCapital capital = (KonCapital) territoryTo;
				// Add player to territory bar
				capital.addBarPlayer(player);
				break;
				
			case CAMP:
				KonCamp camp = (KonCamp)territoryTo;
				// Add player to territory bar
				camp.addBarPlayer(player);
				// Attempt to start a raid alert
				if(!camp.isRaidAlertDisabled() && !player.isAdminBypassActive() && !player.getKingdom().isPeaceful()) {
					// Verify online player
					if(camp.isOwnerOnline() && camp.getOwner() instanceof Player) {
						Player bukkitPlayer = (Player)camp.getOwner();
						boolean isMember = false;
						if(konquest.getCampManager().isCampGrouped(camp)) {
							isMember = konquest.getCampManager().getCampGroup(camp).isPlayerMember(player.getBukkitPlayer());
						}
						// Alert the camp owner if player is not a group member and online
						if(playerManager.isOnlinePlayer(bukkitPlayer) && !isMember && !player.getBukkitPlayer().getUniqueId().equals(camp.getOwner().getUniqueId())) {
							KonPlayer ownerPlayer = playerManager.getPlayer(bukkitPlayer);
							//ChatUtil.sendNotice((Player)camp.getOwner(), "Enemy spotted in "+event.getTerritory().getName()+", use \"/k travel camp\" to defend!", ChatColor.DARK_RED);
							ChatUtil.sendNotice(bukkitPlayer, MessagePath.PROTECTION_NOTICE_RAID.getMessage(camp.getName(),"camp"),ChatColor.DARK_RED);
							ChatUtil.sendKonPriorityTitle(ownerPlayer, ChatColor.DARK_RED+MessagePath.PROTECTION_NOTICE_RAID_ALERT.getMessage(), ChatColor.DARK_RED+""+camp.getName(), 60, 1, 10);
							// Start Raid Alert disable timer for target town
							int raidAlertTimeSeconds = konquest.getConfigManager().getConfig("core").getInt("core.towns.raid_alert_cooldown");
							ChatUtil.printDebug("Starting raid alert timer for "+raidAlertTimeSeconds+" seconds");
							Timer raidAlertTimer = camp.getRaidAlertTimer();
							camp.setIsRaidAlertDisabled(true);
							raidAlertTimer.stopTimer();
							raidAlertTimer.setTime(raidAlertTimeSeconds);
							raidAlertTimer.startTimer();
						}
					}
				}
				break;
				
			default:
				break;
		}
		return;
    }
    
    private void onExitTerritory(KonTerritory territoryFrom, KonPlayer player, boolean isArmisticeFrom) {
    	if(territoryFrom == null) {
    		return;
    	}
    	// Decide what to do for specific territories
		switch(territoryFrom.getTerritoryType()) {
			case TOWN:
				KonTown town = (KonTown) territoryFrom;
				town.removeBarPlayer(player);
				player.clearAllMobAttackers();
				// Command all nearby Iron Golems to target nearby enemy players, ignore triggering player
				updateGolemTargetsForTerritory(territoryFrom,player,false,isArmisticeFrom);
				// Try to clear heart adjustments
				kingdomManager.clearTownHearts(player);
				break;
				
			case RUIN:
				KonRuin ruin = (KonRuin)territoryFrom;
				ruin.removeBarPlayer(player);
				ruin.stopTargetingPlayer(player.getBukkitPlayer());
				break;
				
			case CAPITAL:
				KonCapital capital = (KonCapital) territoryFrom;
				capital.removeBarPlayer(player);
				break;
				
			case CAMP:
				KonCamp camp = (KonCamp)territoryFrom;
				camp.removeBarPlayer(player);
				break;
				
			default:
				break;
		}
    }
    
    private void updateGolemTargetsForTerritory(KonTerritory territory, KonPlayer triggerPlayer, boolean useDefault, boolean isArmistice) {
    	// Command all nearby Iron Golems to target closest player, if enemy exists nearby, else don't change target
    	// Find iron golems within the town max radius
    	boolean isGolemAttackEnemies = konquest.getConfigManager().getConfig("core").getBoolean("core.kingdoms.golem_attack_enemies");
		if(isGolemAttackEnemies && !triggerPlayer.isAdminBypassActive() && !triggerPlayer.getKingdom().equals(territory.getKingdom()) && !isArmistice) {
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
