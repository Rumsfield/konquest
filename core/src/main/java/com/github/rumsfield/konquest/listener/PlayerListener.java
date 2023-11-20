package com.github.rumsfield.konquest.listener;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.api.event.territory.KonquestTerritoryMoveEvent;
import com.github.rumsfield.konquest.api.model.KonquestRelationshipType;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.manager.CampManager;
import com.github.rumsfield.konquest.manager.KingdomManager;
import com.github.rumsfield.konquest.manager.PlayerManager;
import com.github.rumsfield.konquest.manager.TerritoryManager;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.model.KonPlayer.FollowType;
import com.github.rumsfield.konquest.model.KonPlayer.RegionType;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import com.github.rumsfield.konquest.utility.Timer;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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

import java.util.ArrayList;
import java.util.UUID;


public class PlayerListener implements Listener {

	private final Konquest konquest;
	private final PlayerManager playerManager;
	private final KingdomManager kingdomManager;
	private final TerritoryManager territoryManager;
	private final CampManager campManager;
	
	public PlayerListener(KonquestPlugin plugin) {
		this.konquest = plugin.getKonquestInstance();
		this.playerManager = konquest.getPlayerManager();
		this.kingdomManager = konquest.getKingdomManager();
		this.territoryManager = konquest.getTerritoryManager();
		this.campManager = konquest.getCampManager();
	}
	
	/**
     * Fires when a player joins the server
	 */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
    	//ChatUtil.printDebug("EVENT: Player Joined");
    	Player bukkitPlayer = event.getPlayer();
    	KonPlayer player = konquest.initPlayer(bukkitPlayer);
    	// Schedule messages to display after 10-tick delay (0.5 second)
    	Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(), () -> {
			// Actions to run after delay
			if(player == null) {
				ChatUtil.printDebug("Failed to use player from null import on player join");
				return;
			}
			// Send helpful messages
			if(player.getKingdom().isSmallest()) {
				int boostPercent = konquest.getCore().getInt(CorePath.KINGDOMS_SMALLEST_EXP_BOOST_PERCENT.getPath());
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

			// Messages for kingdom diplomacy
			boolean isDiplomacyRequestsPending = !player.getKingdom().getRelationRequestKingdoms().isEmpty();
			boolean isPlayerKingdomOfficer = player.getKingdom().isOfficer(bukkitPlayer.getUniqueId());
			if(isDiplomacyRequestsPending && isPlayerKingdomOfficer) {
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_KINGDOM_NOTICE_DIPLOMACY_PENDING.getMessage(), ChatColor.GOLD);
			}

			// Messages for kingdom & town membership
			// Notify players with outstanding kingdom/town invites.
			// Notify town elites & kingdom officers of any pending join requests.
			ArrayList<String> townRequestNames = new ArrayList<>();
			ArrayList<String> townInviteNames = new ArrayList<>();

			boolean isKingdomInvites = false;
			boolean isKingdomRequests = false;

			UUID id = player.getBukkitPlayer().getUniqueId();
			for(KonKingdom kingdom : kingdomManager.getKingdoms()) {
				// Determine pending invite lists
				if(kingdom.isJoinInviteValid(id)) {
					isKingdomInvites = true;
				}
				for(KonTown town : kingdom.getTowns()) {
					if(town.isJoinInviteValid(id)) {
						townInviteNames.add(town.getName());
					}
					if(!town.getJoinRequests().isEmpty() && town.isPlayerKnight(player.getBukkitPlayer())) {
						townRequestNames.add(town.getName());
					}
				}
				if(!kingdom.getJoinRequests().isEmpty() && kingdom.isOfficer(id)) {
					isKingdomRequests = true;
				}
			}

			// Notify requests
			if(isKingdomRequests) {
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_KINGDOM_NOTICE_REQUEST_PENDING.getMessage(), ChatColor.LIGHT_PURPLE);
			}
			if(!townRequestNames.isEmpty()) {
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_REQUEST_PENDING.getMessage(), ChatColor.LIGHT_PURPLE);
				ChatUtil.sendCommaMessage(bukkitPlayer, townRequestNames, ChatColor.LIGHT_PURPLE);
			}
			// Notify invites
			if(isKingdomInvites) {
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_KINGDOM_NOTICE_INVITE_PENDING.getMessage());
			}
			if(!townInviteNames.isEmpty()) {
				ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_TOWN_NOTICE_INVITE_PENDING.getMessage());
				ChatUtil.sendCommaMessage(bukkitPlayer, townInviteNames);
			}

			// DiscordSRV
			if(konquest.getIntegrationManager().getDiscordSrv().isEnabled()) {
				String message = konquest.getIntegrationManager().getDiscordSrv().getLinkMessage(bukkitPlayer);
				ChatUtil.sendNotice(bukkitPlayer, message);
			}
		}, 10);
    }
    
    /**
     * Fires when a player quits the server
	 */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
    	ChatUtil.printDebug("EVENT: Player Quit");
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
    	// Update offline protections
    	kingdomManager.updateKingdomOfflineProtection();
    	// Protect camp
    	campManager.activateCampProtection(player);
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPlayerChatLowest(AsyncPlayerChatEvent event) {
    	if(!konquest.getChatPriority().equals(EventPriority.LOWEST)) return;
		onAsyncPlayerChat(event);
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onAsyncPlayerChatLow(AsyncPlayerChatEvent event) {
    	if(!konquest.getChatPriority().equals(EventPriority.LOW)) return;
		onAsyncPlayerChat(event);
    }
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onAsyncPlayerChatNormal(AsyncPlayerChatEvent event) {
    	if(!konquest.getChatPriority().equals(EventPriority.NORMAL)) return;
		onAsyncPlayerChat(event);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPlayerChatHigh(AsyncPlayerChatEvent event) {
    	if(!konquest.getChatPriority().equals(EventPriority.HIGH)) return;
		onAsyncPlayerChat(event);
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerChatHighest(AsyncPlayerChatEvent event) {
    	if(!konquest.getChatPriority().equals(EventPriority.HIGHEST)) return;
		onAsyncPlayerChat(event);
    }
    
    /**
     * Fires on chat events
     * Cancel the chat event and pass info to integrated plugins.
     * Send formatted messages to recipients.
	 */
    private void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        //Check if the event was caused by a player
		if(!event.isAsynchronous() || event.isCancelled())return;

		boolean enable = konquest.getCore().getBoolean(CorePath.CHAT_ENABLE_FORMAT.getPath(),true);
		if(!enable) return;
		// Format chat messages
		Player bukkitPlayer = event.getPlayer();
		if(!konquest.getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
			ChatUtil.printDebug("Failed to handle onAsyncPlayerChat for non-existent player");
			return;
		}
		KonPlayer player = playerManager.getPlayer(bukkitPlayer);
		assert player != null;
		KonKingdom kingdom = player.getKingdom();

		// Built-in format string
		boolean formatNameConfig = konquest.getCore().getBoolean(CorePath.CHAT_NAME_TEAM_COLOR.getPath(),true);
		boolean formatKingdomConfig = konquest.getCore().getBoolean(CorePath.CHAT_KINGDOM_TEAM_COLOR.getPath(),true);
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
		String parsedFormat;
		String chatMessage = event.getMessage();
		String chatChannel = "global";
		if(!player.isGlobalChat()) {
			chatChannel = player.getKingdom().getName();
		}

		// Send chat content to Discord first
		konquest.getIntegrationManager().getDiscordSrv().sendGameChatToDiscord(event.getPlayer(), event.getMessage(), chatChannel, event.isCancelled());
		// Then cancel the event. This causes DiscordSRV to not process the event on its own.
		// We need to cancel the event, so it doesn't execute on the server, and we can send custom format messages to players.
		event.setCancelled(true);

		// Send messages to players
		for(KonPlayer viewerPlayer : playerManager.getPlayersOnline()) {
			String teamColor = ""+ChatColor.GOLD;
			String nameColor = ""+ChatColor.GOLD;
			boolean doFormatName = true;
			boolean doFormatKingdom = true;
			String messageFormat = "";

			boolean sendMessage = false;
			if(player.isGlobalChat()) {
				// Sender is in global chat mode
				nameColor = ""+konquest.getDisplayPrimaryColor(viewerPlayer, player);
				teamColor = konquest.getDisplaySecondaryColor(viewerPlayer, player);
				doFormatName = formatNameConfig;
				doFormatKingdom = formatKingdomConfig;
				messageFormat = "";
				sendMessage = true;
			} else {
				// Sender is in kingdom chat mode
				if(viewerPlayer.getKingdom().equals(kingdom)) {
					nameColor = Konquest.friendColor1;
					teamColor = Konquest.friendColor1;
					messageFormat = ""+ChatColor.GREEN+ChatColor.ITALIC;
					sendMessage = true;
				} else if(viewerPlayer.isAdminBypassActive()) {
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
						nameColor,
						doFormatName,
						doFormatKingdom);
				// Attempt to use PAPI for external placeholders
				try {
					// Try to parse placeholders in the format string, if the JAR is present.
					parsedFormat = PlaceholderAPI.setPlaceholders(bukkitPlayer, parsedFormat);
					// Try to parse relational placeholders
					parsedFormat = PlaceholderAPI.setRelationalPlaceholders(viewerPlayer.getBukkitPlayer(), bukkitPlayer, parsedFormat);
				} catch (NoClassDefFoundError ignored) {}
				// Send the chat message
				viewerPlayer.getBukkitPlayer().sendMessage(parsedFormat + ChatColor.DARK_GRAY + Konquest.chatDivider + ChatColor.RESET + " " + messageFormat + chatMessage);
			}
		}

		// Send message to console
		if(player.isGlobalChat()) {
			ChatUtil.printConsole(ChatColor.GOLD + bukkitPlayer.getName()+": "+ChatColor.DARK_GRAY+event.getMessage());
		} else {
			ChatUtil.printConsole(ChatColor.GOLD + "["+kingdom.getName()+"] "+bukkitPlayer.getName()+": "+ChatColor.DARK_GRAY+event.getMessage());
		}

    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
    	KonPlayer player = playerManager.getPlayer(event.getPlayer());
		if(player == null || !player.isCombatTagged()) return;
		for(String cmd : playerManager.getBlockedCommands()) {
			if(event.getMessage().toLowerCase().startsWith("/"+cmd.toLowerCase())) {
				ChatUtil.sendError(event.getPlayer(), MessagePath.PROTECTION_ERROR_TAG_BLOCKED.getMessage());
				event.setCancelled(true);
				return;
			}
		}
    }
    
    /**
     * Handles when players are setting regions.
	 * This should run before any other handlers (Lowest priority).
	 */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerSetRegion(PlayerInteractEvent event) {
    	Player bukkitPlayer = event.getPlayer();
    	if(!konquest.getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
			ChatUtil.printDebug("Failed to handle onPlayerInteract for non-existent player");
			return;
		}
        KonPlayer player = playerManager.getPlayer(bukkitPlayer);
        // Check that the player is setting a region
        if (!player.isSettingRegion()) return;
		// Check if the player clicked air (cancel region setup)
		if (event.getClickedBlock() == null) {
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
					// Location is first corner, verify sanctuary
					KonTerritory territory = territoryManager.getChunkTerritory(location);
					if(territory != null && territory.getTerritoryType().equals(KonquestTerritoryType.SANCTUARY)) {
						// Location is inside a Sanctuary
						String sanctuaryName = territory.getName();
						player.setRegionSanctuaryName(sanctuaryName);
						player.setRegionCornerOneBuffer(location);
						ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_CREATE_2.getMessage(), ChatColor.LIGHT_PURPLE);
					} else {
						// The first corner is not in a sanctuary, end the region setting flow
						String templateName = player.getRegionTemplateName();
						ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_ERROR_FAIL_REGION.getMessage(templateName));
						ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_SANCTUARY.getMessage());
						player.setRegionCornerOneBuffer(null);
						player.setRegionCornerTwoBuffer(null);
						player.settingRegion(RegionType.NONE);
						ChatUtil.printDebug("Ended setting monument region, no sanctuary");
					}
				} else if (player.getRegionCornerTwoBuffer() == null) {
					// Location is second corner, save to player
					player.setRegionCornerTwoBuffer(location);
					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_CREATE_3.getMessage(), ChatColor.LIGHT_PURPLE);
				} else {
					// Location is travel point, create template using saved data
					KonSanctuary sanctuary = konquest.getSanctuaryManager().getSanctuary(player.getRegionSanctuaryName());
					String templateName = player.getRegionTemplateName();
					double templateCost = player.getRegionTemplateCost();
					Location templateCorner1 = player.getRegionCornerOneBuffer();
					Location templateCorner2 = player.getRegionCornerTwoBuffer();
					int createMonumentStatus = konquest.getSanctuaryManager().createMonumentTemplate(sanctuary, templateName, templateCorner1, templateCorner2, location, templateCost);
					switch(createMonumentStatus) {
					case 0:
						ChatUtil.sendBroadcast(MessagePath.PROTECTION_NOTICE_TEMPLATE_READY.getMessage(templateName));
						ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_NOTICE_SUCCESS.getMessage(templateName));
						kingdomManager.reloadMonumentsForTemplate(konquest.getSanctuaryManager().getTemplate(templateName));
						break;
					case 1:
						int diffX = (int)Math.abs(templateCorner1.getX()-templateCorner2.getX())+1;
						int diffZ = (int)Math.abs(templateCorner1.getZ()-templateCorner2.getZ())+1;
						ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_ERROR_FAIL_BASE.getMessage(templateName,diffX,diffZ));
						break;
					case 2:
						String criticalBlockTypeName = konquest.getCore().getString(CorePath.MONUMENTS_CRITICAL_BLOCK.getPath());
						int maxCriticalhits = konquest.getCore().getInt(CorePath.MONUMENTS_DESTROY_AMOUNT.getPath());
						ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_ERROR_FAIL_CRITICAL.getMessage(templateName,maxCriticalhits,criticalBlockTypeName));
						break;
					case 3:
						ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_ERROR_FAIL_TRAVEL.getMessage(templateName));
						break;
					case 4:
						ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_ERROR_FAIL_REGION.getMessage(templateName));
						break;
					case 5:
						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_BAD_NAME.getMessage(templateName));
						break;
					case 10:
						ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_ADMIN_MONUMENT_ERROR_FAIL_RESET.getMessage(templateName));
						break;
					default:
						ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(createMonumentStatus));
						break;
					}
					player.setRegionCornerOneBuffer(null);
					player.setRegionCornerTwoBuffer(null);
					player.setRegionTemplateCost(0.0);
					player.settingRegion(RegionType.NONE);
					ChatUtil.printDebug("Finished setting monument region");
				}
				break;
			case RUIN_CRITICAL:
				boolean validCriticalBlock = false;
				if(territoryManager.isChunkClaimed(location)) {
					KonTerritory territory = territoryManager.getChunkTerritory(location);
					if(territory instanceof KonRuin) {
						Material criticalType = konquest.getRuinManager().getRuinCriticalBlock();
						if(event.getClickedBlock().getType().equals(criticalType)) {
							((KonRuin)territory).addCriticalLocation(location);
							ruinName = territory.getName();
							validCriticalBlock = true;
						} else {
							ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_ADMIN_RUIN_ERROR_MATCH.getMessage(criticalType.toString()));
						}
					}
				}
				if(validCriticalBlock) {
					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_RUIN_NOTICE_ADD.getMessage(ruinName));
				} else {
					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_ADMIN_RUIN_ERROR_INVALID.getMessage());
				}
				break;
			case RUIN_SPAWN:
				boolean validSpawnBlock = false;
				if(territoryManager.isChunkClaimed(location)) {
					KonTerritory territory = territoryManager.getChunkTerritory(location);
					if(territory instanceof KonRuin) {
						((KonRuin)territory).addSpawnLocation(location);
						ruinName = territory.getName();
						validSpawnBlock = true;
					}
				}
				if(validSpawnBlock) {
					ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_ADMIN_RUIN_NOTICE_ADD.getMessage(ruinName));
				} else {
					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_ADMIN_RUIN_ERROR_INVALID.getMessage());
				}
				break;
			default:
				break;
		}
		event.setCancelled(true);
    }

	/**
	 * Handles when players use blocks
	 */
	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerUse(PlayerInteractEvent event) {
		Player bukkitPlayer = event.getPlayer();
		if(!konquest.getPlayerManager().isOnlinePlayer(bukkitPlayer)) {
			ChatUtil.printDebug("Failed to handle onPlayerInteract for non-existent player");
			return;
		}
		KonPlayer player = playerManager.getPlayer(bukkitPlayer);
		// Handle block interactions
		if(!player.isAdminBypassActive() && event.hasBlock() && !event.useInteractedBlock().equals(Event.Result.DENY)) {
			BlockState clickedState = event.getClickedBlock().getState();
			// Check for territory
			if(territoryManager.isChunkClaimed(clickedState.getLocation())) {
				// Interaction occurred within claimed territory
				KonTerritory territory = territoryManager.getChunkTerritory(clickedState.getLocation());
				// Property Flag Holders
				if(territory instanceof KonPropertyFlagHolder) {
					KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
					if(flagHolder.hasPropertyValue(KonPropertyFlag.USE)) {
						// Block non-sign uses
						if(!(flagHolder.getPropertyValue(KonPropertyFlag.USE) || clickedState instanceof Sign)) {
							preventUse(event,player);
						}
					}
				}
				// Ruin protections...
				if(territory instanceof KonRuin) {
					KonRuin ruin = (KonRuin)territory;
					// Target player who interacts with critical blocks
					if(ruin.isCriticalLocation(event.getClickedBlock().getLocation())) {
						ruin.targetAllGolemsToPlayer(bukkitPlayer);
					}
				}
				// Town protections...
				if(territory instanceof KonTown) {
					KonTown town = (KonTown) territory;
					KonquestRelationshipType playerRole = kingdomManager.getRelationRole(player.getKingdom(), territory.getKingdom());
					// Target player who interacts with monument blocks
					if(town.isLocInsideMonumentProtectionArea(event.getClickedBlock().getLocation())) {
						town.targetRabbitToPlayer(bukkitPlayer);
					}
					// Protections for friendly non-residents of closed towns
					if(playerRole.equals(KonquestRelationshipType.FRIENDLY) && !town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer())) {
						// Check for allowed usage like buttons, levers
						if(!town.isFriendlyRedstoneAllowed() && preventUse(event,player)) {
							event.setCancelled(true);
							return;
						}
						// Try to protected physical interaction like pressure plates, trampling farmland
						if(preventPhysical(event,player)) {
							event.setCancelled(true);
							return;
						}
					}
					// Protections for non-friendlies
					if(!playerRole.equals(KonquestRelationshipType.FRIENDLY)) {
						// Check for allowed usage like buttons, levers
						if(!town.isEnemyRedstoneAllowed() && preventUse(event,player)) {
							event.setCancelled(true);
							return;
						}
						// Try to protected physical interaction like pressure plates, trampling farmland
						if(preventPhysical(event,player)) {
							event.setCancelled(true);
							return;
						}
					}
					// Prevent enemies and non-residents from interacting with item frames
					if(!playerRole.equals(KonquestRelationshipType.FRIENDLY) || (!town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer()))) {
						Material clickedMat = event.getClickedBlock().getType();
						if(clickedMat.equals(Material.ITEM_FRAME)) {
							ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedProtectionColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
							event.setCancelled(true);
							return;
						}
					}
				}

			} else {
				// Interaction occurred in the wild
				boolean isWildUse = konquest.getCore().getBoolean(CorePath.KINGDOMS_WILD_USE.getPath(), true);
				boolean isWorldValid = konquest.isWorldValid(clickedState.getLocation());
				if(!isWildUse && isWorldValid) {
					if(preventUse(event,player)) {
						event.setCancelled(true);
						return;
					}
					if(preventPhysical(event,player)) {
						event.setCancelled(true);
						return;
					}
				}
			}
		}
		// Jukebox handler
		if(event.hasItem() &&
				!event.useItemInHand().equals(Event.Result.DENY) &&
				event.getItem().getType().isRecord() &&
				event.getAction().equals(Action.RIGHT_CLICK_BLOCK) &&
				event.hasBlock() &&
				event.getClickedBlock().getType().equals(Material.JUKEBOX) &&
				!konquest.isWorldIgnored(event.getClickedBlock().getLocation()) ) {
			// Update music stat when not on record cooldown
			if(player.isRecordPlayCooldownOver()) {
				konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.MUSIC,1);
				player.markRecordPlayCooldown();
			}
		}
	}
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerEnterVehicle(VehicleEnterEvent event) {
    	if(event.isCancelled()) return; // Do nothing if another plugin cancels this event

		Entity passenger = event.getEntered();
    	if(passenger instanceof Player) {
    		Player bukkitPlayer = (Player) passenger;
    		// General chunk transition handler
    		// Never force on vehicle entry
        	boolean status = onPlayerEnterLeaveChunk(event.getVehicle().getLocation(), bukkitPlayer.getLocation(), bukkitPlayer, false);
        	if(!status) {
        		event.setCancelled(true);
        	}
    	}
    }
    
    /**
     * Handles when players are right-clicking entities
	 */
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		if(event.isCancelled()) return; // Do nothing if another plugin cancels this event
    	if(konquest.isWorldIgnored(event.getPlayer().getLocation())) return;
    	Entity clicked = event.getRightClicked();
    	Player bukkitPlayer = event.getPlayer();
        KonPlayer player = playerManager.getPlayer(bukkitPlayer);
        if(player != null && !player.isAdminBypassActive() && territoryManager.isChunkClaimed(clicked.getLocation())) {
        	KonTerritory territory = territoryManager.getChunkTerritory(clicked.getLocation());
        	// Entity exceptions are always allowed to interact
        	//ChatUtil.printDebug("Player "+bukkitPlayer.getName()+" interacted at entity of type: "+ clicked.getType());
        	boolean isEntityAllowed = (clicked.getType().equals(EntityType.PLAYER) ||
					clicked.getType().equals(EntityType.VILLAGER) ||
					clicked.getType().equals(EntityType.BOAT));
        	// Property Flag Holders
			if(territory instanceof KonPropertyFlagHolder) {
				KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
				if(flagHolder.hasPropertyValue(KonPropertyFlag.USE)) {
					// Block non-allowed entity interaction
					if(!isEntityAllowed && !flagHolder.getPropertyValue(KonPropertyFlag.USE)) {
						ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedFlagColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
						event.setCancelled(true);
						return;
					}
				}
			}
        	// Town protections...
    		if(territory instanceof KonTown && !isEntityAllowed) {
    			KonTown town = (KonTown) territory;
				KonquestRelationshipType playerRole = kingdomManager.getRelationRole(player.getKingdom(), territory.getKingdom());
    			// Prevent enemies and non-residents from interacting with entities
    			if(!playerRole.equals(KonquestRelationshipType.FRIENDLY) || (!town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer()))) {
    				ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedProtectionColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
    				event.setCancelled(true);
				}
    		}
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
		if(event.isCancelled()) return; // Do nothing if another plugin cancels this event
    	if(konquest.isWorldIgnored(event.getPlayer().getLocation())) return;
    	Player bukkitPlayer = event.getPlayer();
        KonPlayer player = playerManager.getPlayer(bukkitPlayer);
        if(player != null && !player.isAdminBypassActive() && territoryManager.isChunkClaimed(event.getRightClicked().getLocation())) {
			KonTerritory territory = territoryManager.getChunkTerritory(event.getRightClicked().getLocation());
			//ChatUtil.printDebug("Player "+bukkitPlayer.getName()+" manipulated armor stand in territory "+ territory.getName());
        	// Property Flag Holders
			if(territory instanceof KonPropertyFlagHolder) {
				KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
				if(flagHolder.hasPropertyValue(KonPropertyFlag.USE)) {
					if(!flagHolder.getPropertyValue(KonPropertyFlag.USE)) {
						ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedFlagColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
						event.setCancelled(true);
					}
				}
			}
			// Specific territory protections
			if(territory instanceof KonTown) {
				KonTown town = (KonTown) territory;
				// Prevent non-friendlies (including enemies) and friendly non-residents
				KonquestRelationshipType playerRole = kingdomManager.getRelationRole(player.getKingdom(), territory.getKingdom());
				if(!playerRole.equals(KonquestRelationshipType.FRIENDLY) || (!town.isOpen() && !town.isPlayerResident(player.getOfflineBukkitPlayer()))) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_NOT_RESIDENT.getMessage(territory.getName()));
					event.setCancelled(true);
					return;
				}
			}
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerFish(PlayerFishEvent event) {
    	if(!event.isCancelled()) {
    		if(konquest.isWorldIgnored(event.getPlayer().getLocation())) return;
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
    
    @EventHandler(priority = EventPriority.MONITOR)
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
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
    	onBucketUse(event);
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
    	onBucketUse(event);
    }    
    
    private void onBucketUse(PlayerBucketEvent event) {
		if(event.isCancelled()) return; // Do nothing if another plugin cancels this event
		if(!konquest.getPlayerManager().isOnlinePlayer(event.getPlayer())) {
			ChatUtil.printDebug("Failed to handle onBucketUse for non-existent player");
			return;
		}
		KonPlayer player = konquest.getPlayerManager().getPlayer(event.getPlayer());
		assert player != null;
		if(player.isAdminBypassActive()) return; // skip this check if admin bypass
		// Check for territory
		boolean cancelUse = false;
		if(territoryManager.isChunkClaimed(event.getBlock().getLocation())) {
			// Interaction occured in claimed territory
			KonTerritory territory = territoryManager.getChunkTerritory(event.getBlock().getLocation());
			if(territory instanceof KonSanctuary && ((KonSanctuary) territory).isLocInsideTemplate(event.getBlock().getLocation())) {
				// Block is inside monument template
				cancelUse = true;
			} else if(territory instanceof KonTown && ((KonTown) territory).isLocInsideMonumentProtectionArea(event.getBlock().getLocation())) {
				// Block is inside town monument
				cancelUse = true;
			} else if(territory instanceof KonTown && !player.getKingdom().equals(territory.getKingdom())) {
				// The block is located inside an enemy town
				cancelUse = true;
			} else if(territory instanceof KonRuin) {
				// The block is inside a Ruin
				cancelUse = true;
			} else if(territory instanceof KonCamp && !((KonCamp)territory).isPlayerOwner(event.getPlayer())) {
				// Block is inside a non-owned camp
				cancelUse = true;
			}
			// General property flag
			if(territory instanceof KonPropertyFlagHolder) {
				KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
				if(flagHolder.hasPropertyValue(KonPropertyFlag.USE)) {
					if(!flagHolder.getPropertyValue(KonPropertyFlag.USE)) {
						cancelUse = true;
					}
				}
			}
		} else {
			// Interaction occurred in the wild
			boolean isWildUse = konquest.getCore().getBoolean(CorePath.KINGDOMS_WILD_USE.getPath(), true);
			boolean isWorldValid = konquest.isWorldValid(event.getBlock().getLocation());
			if(!isWildUse && isWorldValid) {
				// Block is in the wild of a valid world that has wild use disabled
				cancelUse = true;
			}
		}
		if(cancelUse) {
			ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedProtectionColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
			event.setCancelled(true);
		}
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawnCapital(PlayerRespawnEvent event) {
    	if(!konquest.getPlayerManager().isOnlinePlayer(event.getPlayer())) {
			ChatUtil.printDebug("Failed to handle onPlayerRespawnCapital for non-existent player");
			return;
		}
    	KonPlayer player = playerManager.getPlayer(event.getPlayer());
		if(player == null) return;
    	// Send respawn to capital if no bed exists
		boolean isCapitalRespawn = konquest.getCore().getBoolean(CorePath.KINGDOMS_CAPITAL_RESPAWN.getPath(),true);
    	if(!event.isBedSpawn() && !player.isBarbarian() && isCapitalRespawn) {
			event.setRespawnLocation(player.getKingdom().getCapital().getSpawnLoc());
    	}
    }

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerRespawnFinal(PlayerRespawnEvent event) {
		if(!konquest.getPlayerManager().isOnlinePlayer(event.getPlayer())) {
			ChatUtil.printDebug("Failed to handle onPlayerRespawnFinal for non-existent player");
			return;
		}
		KonPlayer player = playerManager.getPlayer(event.getPlayer());
		if(player == null) return;
		Location deathLoc = event.getPlayer().getLocation();
		Location respawnLoc = event.getRespawnLocation();
		boolean isTerritoryTo = territoryManager.isChunkClaimed(respawnLoc);
		boolean isTerritoryFrom = territoryManager.isChunkClaimed(deathLoc);
		KonTerritory territoryTo = null;
		KonTerritory territoryFrom = null;
		if(isTerritoryFrom) {
			territoryFrom = territoryManager.getChunkTerritory(deathLoc);
			// Exit Territory
			onExitTerritory(territoryFrom,player);
		}
		if(isTerritoryTo) {
			territoryTo = territoryManager.getChunkTerritory(respawnLoc);
			// Entry Territory
			onEnterTerritory(territoryTo,respawnLoc,deathLoc,player);
		}
	}
    
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
    	if(konquest.isWorldIgnored(event.getPlayer().getLocation())) return;
    	Player bukkitPlayer = event.getPlayer();
    	KonPlayer player = playerManager.getPlayer(bukkitPlayer);
    	int boostPercent = konquest.getCore().getInt(CorePath.KINGDOMS_SMALLEST_EXP_BOOST_PERCENT.getPath());
    	if(boostPercent > 0 && player != null && player.getKingdom().isSmallest()) {
    		int baseAmount = event.getAmount();
    		int boostAmount = ((boostPercent*baseAmount)/100)+baseAmount;
    		event.setAmount(boostAmount);
    	}
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDropMenuItem(PlayerDropItemEvent event) {
    	if(konquest.getDisplayManager().isPlayerViewingMenu(event.getPlayer())) {
    		ChatUtil.printDebug("Player "+event.getPlayer().getName()+" tried to drop an item from an inventory menu!");
    		//TODO: Destroy the item dropped from the menu GUI
    	}
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPortal(PlayerPortalEvent event) {
    	if(event.isCancelled()) return;// Do nothing if another plugin cancels this event
    	Location portalToLoc = event.getTo();
    	// Ensure the portal event is not sending the player to a null location, like if the end is disabled
    	if(portalToLoc != null) {
	    	ChatUtil.printDebug("EVENT: Player portal to world "+portalToLoc.getWorld().getName()+" because "+event.getCause()+", location: "+ portalToLoc);
	    	Player bukkitPlayer = event.getPlayer();
	    	// When portal into valid world...
	    	if(konquest.isWorldValid(portalToLoc.getWorld())) {
				// Protections for territory
	    		if(territoryManager.isChunkClaimed(portalToLoc)) {
		    		KonTerritory territory = territoryManager.getChunkTerritory(portalToLoc);
		    		// Property Flag Holders
					if(territory instanceof KonPropertyFlagHolder) {
						KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territory;
						if(flagHolder.hasPropertyValue(KonPropertyFlag.USE)) {
							if(!flagHolder.getPropertyValue(KonPropertyFlag.USE)) {
								ChatUtil.printDebug("EVENT: Portal creation stopped inside of territory "+territory.getName());
								event.setCanCreatePortal(false);
								event.setCancelled(true);
								ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_PORTAL_EXIT.getMessage());
								return;
							}
						}
					}
					// Monument checks
					boolean cancelPortal = false;
					if(territory instanceof KonSanctuary && ((KonSanctuary) territory).isLocInsideTemplate(event.getTo())) {
						cancelPortal = true;
					} else if(territory instanceof KonTown && ((KonTown) territory).isLocInsideMonumentProtectionArea(event.getTo())) {
						cancelPortal = true;
					}
					if(cancelPortal) {
						ChatUtil.printDebug("EVENT: Portal creation stopped inside of town monument "+territory.getName());
						event.setCanCreatePortal(false);
						event.setCancelled(true);
						ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_PORTAL_EXIT.getMessage());
						return;
					}
	    		}
			}
	    	// General chunk transition handler
	    	// Force for specific causes
	    	boolean forceEntryExit = event.getCause().equals(TeleportCause.COMMAND) ||
					event.getCause().equals(TeleportCause.PLUGIN) ||
					event.getCause().equals(TeleportCause.SPECTATE) ||
					event.getCause().equals(TeleportCause.UNKNOWN);
			boolean status = onPlayerEnterLeaveChunk(event.getTo(), event.getFrom(), event.getPlayer(), forceEntryExit);
	    	if(!status) {
	    		event.setCancelled(true);
	    	}
    	}
    }
    
    /**
     * Checks for players moving into chunks
	 */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
    	if(event.isCancelled()) return; // Do nothing if another plugin cancels this event
		if(event.getTo() == null)return;
		// General chunk transition handler
    	// Never force when moving normally
    	boolean status = onPlayerEnterLeaveChunk(event.getTo(), event.getFrom(), event.getPlayer(), false);
    	if(status) return;

    	event.setCancelled(true);

    }
    
    /**
     * Checks for players teleporting into chunks
	 */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
    	if(event.isCancelled()) return; // Do nothing if another plugin cancels this event
		if(event.getTo() == null)return;

    	// Check for inter-chunk ender pearl
    	boolean isEnemyPearlBlocked = konquest.getCore().getBoolean(CorePath.KINGDOMS_NO_ENEMY_ENDER_PEARL.getPath(), false);
    	Player bukkitPlayer = event.getPlayer();
		if(!konquest.getPlayerManager().isOnlinePlayer(bukkitPlayer)) return;
		KonPlayer player = playerManager.getPlayer(bukkitPlayer);
		// Inter-chunk checks
		// Prevent enemies teleporting to ender pearls thrown into enemy land or out of enemy land
		if(isEnemyPearlBlocked && event.getCause().equals(TeleportCause.ENDER_PEARL)) {
			boolean isEnemyTo = territoryManager.isChunkClaimed(event.getTo()) &&
					!kingdomManager.isPlayerFriendly(player, territoryManager.getChunkTerritory(event.getTo()).getKingdom());
			boolean isEnemyFrom = territoryManager.isChunkClaimed(event.getFrom()) &&
					!kingdomManager.isPlayerFriendly(player, territoryManager.getChunkTerritory(event.getFrom()).getKingdom());
			if(isEnemyTo || isEnemyFrom) {
				ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedProtectionColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
				event.setCancelled(true);
				return;
			}
		}

		// General chunk transition handler
		// Force for specific causes
    	boolean forceEntryExit = event.getCause().equals(TeleportCause.COMMAND) ||
				event.getCause().equals(TeleportCause.PLUGIN) ||
				event.getCause().equals(TeleportCause.SPECTATE) ||
				event.getCause().equals(TeleportCause.UNKNOWN);
		boolean status = onPlayerEnterLeaveChunk(event.getTo(), event.getFrom(), event.getPlayer(), forceEntryExit);
		if(status)return;

		event.setCancelled(true);
    }
    
    // Returns false when parent event should be cancelled
    private boolean onPlayerEnterLeaveChunk(Location moveTo, Location moveFrom, Player movePlayer, boolean force) {
    	// Evaluate player movement when they cross between blocks
    	if(!moveTo.getBlock().equals(moveFrom.getBlock()) || !moveTo.getWorld().equals(moveFrom.getWorld())) {
    		// Player moved to a new block
    		// Try to cancel any travel warmup
    		boolean doCancelTravelOnMove = konquest.getCore().getBoolean(CorePath.TRAVEL_CANCEL_ON_MOVE.getPath(), false);
    		if(doCancelTravelOnMove) {
    			boolean status = konquest.getTravelManager().cancelTravel(movePlayer);
    			if(status) {
    				ChatUtil.sendError(movePlayer, MessagePath.COMMAND_TRAVEL_ERROR_CANCELED.getMessage());
    			}
    		}
    	}
    	
    	// Evaluate chunk territory transitions only when players move between chunks
    	// Check if player moved between chunks or worlds
    	if(!moveTo.getChunk().equals(moveFrom.getChunk()) || !moveTo.getWorld().equals(moveFrom.getWorld())) {
    		
    		if(!konquest.getPlayerManager().isOnlinePlayer(movePlayer)) {
				return true;
			}
        	KonPlayer player = playerManager.getPlayer(movePlayer);
    		boolean isTerritoryTo = territoryManager.isChunkClaimed(moveTo);
    		boolean isTerritoryFrom = territoryManager.isChunkClaimed(moveFrom);
    		KonTerritory territoryTo = null;
			KonTerritory territoryFrom = null;
			if(isTerritoryTo) {
				territoryTo = territoryManager.getChunkTerritory(moveTo);
			}
			if(isTerritoryFrom) {
				territoryFrom = territoryManager.getChunkTerritory(moveFrom);
			}
			
			// Fire event when either entering or leaving a territory
			if(isTerritoryTo || isTerritoryFrom) {
	    		KonquestTerritoryMoveEvent invokeEvent = new KonquestTerritoryMoveEvent(konquest, territoryTo, territoryFrom, player);
	    		Konquest.callKonquestEvent(invokeEvent);
	    		if(invokeEvent.isCancelled()) {
	    			return false;
	    		}
			}
    		
			// Check world transition
    		if(moveTo.getWorld().equals(moveFrom.getWorld())) {
    			// Player moved within the same world
        		
    			// Auto claiming & un-claiming
        		if(player.isAutoFollowActive()) {
	        		if(!isTerritoryTo) {
	        			// Auto claim
	        			if(player.getAutoFollow().equals(FollowType.ADMIN_CLAIM)) {
	        				// Admin claiming takes priority
	        				territoryManager.claimForAdmin(player, moveTo);
	        			} else if(player.getAutoFollow().equals(FollowType.CLAIM)) {
	        				// Player is claim following
	        				boolean isClaimSuccess = territoryManager.claimForPlayer(player, moveTo);
	            			if(!isClaimSuccess) {
	            				player.setAutoFollow(FollowType.NONE);
	            				ChatUtil.sendNotice(movePlayer, MessagePath.COMMAND_CLAIM_NOTICE_FAIL_AUTO.getMessage());
	            			} else {
	            				ChatUtil.sendKonTitle(player, "", ChatColor.GREEN+MessagePath.COMMAND_CLAIM_NOTICE_PASS_AUTO.getMessage(), 15);
	            			}
	        			}
	        		} else {
	        			// Auto un-claim
	        			if(player.getAutoFollow().equals(FollowType.ADMIN_UNCLAIM)) {
	        				// Admin un-claiming takes priority
	        				territoryManager.unclaimForAdmin(movePlayer, moveTo);
	        			} else if(player.getAutoFollow().equals(FollowType.UNCLAIM)) {
	        				// Player is un-claim following
	        				boolean isUnclaimSuccess = territoryManager.unclaimForPlayer(movePlayer, moveTo);
	            			if(!isUnclaimSuccess) {
	            				player.setAutoFollow(FollowType.NONE);
	            				ChatUtil.sendNotice(movePlayer, MessagePath.COMMAND_UNCLAIM_NOTICE_FAIL_AUTO.getMessage());
	            			} else {
	            				ChatUtil.sendKonTitle(player, "", ChatColor.GREEN+MessagePath.COMMAND_UNCLAIM_NOTICE_PASS_AUTO.getMessage(), 15);
	            			}
	        			}
	        		}
	        		// Update territory variables for chunk boundary checks below
    				isTerritoryTo = territoryManager.isChunkClaimed(moveTo);
    				isTerritoryFrom = territoryManager.isChunkClaimed(moveFrom);
    				if(isTerritoryTo) {
    					territoryTo = territoryManager.getChunkTerritory(moveTo);
    				}
    				if(isTerritoryFrom) {
    					territoryFrom = territoryManager.getChunkTerritory(moveFrom);
    				}
        		}
        		
        		// Chunk transition checks
        		if(!isTerritoryTo && isTerritoryFrom) { // When moving into the wild
        			// Check if exit is allowed
        			if(isDeniedExitTerritory(territoryFrom,player,force)) return false;
        			// Display WILD
        			ChatUtil.sendKonTitle(player, "", MessagePath.GENERIC_NOTICE_WILD.getMessage());
        			// Do things appropriate to the type of territory
        			onExitTerritory(territoryFrom,player);
        			// Begin fly disable warmup
        			player.setFlyDisableWarmup(true);
        		} else if(isTerritoryTo && !isTerritoryFrom) { // When moving out of the wild
        			// Check if entry is allowed
        			if(isDeniedEnterTerritory(territoryTo,player,force)) return false;
        			// Set message color based on enemy territory
        			String color = konquest.getDisplaySecondaryColor(player, territoryTo);
	                // Display Territory Name
	    			ChatUtil.sendKonTitle(player, "", color+territoryTo.getName());
	    			// Do things appropriate to the type of territory
	    			onEnterTerritory(territoryTo,moveTo,moveFrom,player);
	    			// Try to stop fly disable warmup, or disable immediately
	    			if(territoryTo.getKingdom().equals(player.getKingdom())) {
	    				player.setFlyDisableWarmup(false);
	    			} else {
	    				player.setIsFlyEnabled(false);
	    			}
        		} else if(isTerritoryTo) { // When moving between two claimed territories
        			// Check for differing territories, if true then display new Territory Name and send message to enemies
        			if(!territoryTo.equals(territoryFrom)) { // moving between different territories
        				// Check if exit is allowed
            			if(isDeniedExitTerritory(territoryFrom,player,force)) {
            				return false;
            			}
        				// Check if entry is allowed
            			if(isDeniedEnterTerritory(territoryTo,player,force)) {
            				return false;
            			}
        				// Set message color based on To territory
            			String color = konquest.getDisplaySecondaryColor(player, territoryTo);
    	            	ChatUtil.sendKonTitle(player, "", color+territoryTo.getName());
    	            	// Do things appropriate to the type of territory
    	    			// Exit Territory
            			onExitTerritory(territoryFrom,player);
            			// Entry Territory
    	    			onEnterTerritory(territoryTo,moveTo,moveFrom,player);
    	            	// Try to stop or start fly disable warmup
    	    			if(territoryTo.getKingdom().equals(player.getKingdom())) {
    	    				player.setFlyDisableWarmup(false);
    	    			} else {
    	    				player.setIsFlyEnabled(false);
    	    			}
        			} else { // moving between the same territory
        				// Specific checks for territories
        				if(territoryTo instanceof KonTown) {
        					KonTown town = (KonTown) territoryTo;
        					if(kingdomManager.isPlayerEnemy(player, town.getKingdom())) {
        						// Apply town nerfs
        						kingdomManager.applyTownNerf(player, town);
        						// Update golem targets
        						town.updateGolemTargets(player,true);
        					} else {
								// Remove potion effects
								kingdomManager.clearTownNerf(player);
							}
							if(kingdomManager.isPlayerFriendly(player, town.getKingdom())) {
        						// Display plot message to friendly players
        						displayPlotMessage(town, moveTo, moveFrom, player);
        					}
        				}
        			}
        		}
        		
        		// Auto map
        		if(player.isMapAuto()) {
        			// Schedule delayed task to print map
        			Bukkit.getScheduler().scheduleSyncDelayedTask(konquest.getPlugin(),
							() -> territoryManager.printPlayerMap(player, TerritoryManager.DEFAULT_MAP_SIZE, moveTo),1);
        		}
        		
    		} else {
    			// Player moved between worlds
    			
    			// Check if exit is allowed
    			if(isTerritoryFrom && isDeniedExitTerritory(territoryFrom,player,force)) return false;
    			// Check if entry is allowed
    			if(isTerritoryTo && isDeniedEnterTerritory(territoryTo,player,force)) return false;
    			
    			// Disable movement-based flags
    			if(player.isAutoFollowActive()) {
    				player.setAutoFollow(FollowType.NONE);
    				ChatUtil.sendNotice(movePlayer, MessagePath.COMMAND_CLAIM_NOTICE_FAIL_AUTO.getMessage());
    			}
    			if(player.isMapAuto()) {
    				player.setIsMapAuto(false);
    			}
    			
    			// Disable flying
    			player.setIsFlyEnabled(false);
        		
    			if(isTerritoryFrom) {
    				onExitTerritory(territoryFrom,player);
    			}
    			
    			if(isTerritoryTo) {
	                // Set message color based on enemy territory
        			String color = konquest.getDisplaySecondaryColor(player, territoryTo);
	                // Display Territory Name
	    			String territoryName = territoryTo.getName();
	    			ChatUtil.sendKonTitle(player, "", color+territoryName);
	    			// Do things appropriate to the type of territory
	    			onEnterTerritory(territoryTo,moveTo,moveFrom,player);
    			}
    		}
    		
    		// Border particle update
    		territoryManager.updatePlayerBorderParticles(player,moveTo);
    	}
    	return true;
    }
    
    // Return true to deny entry, else false to allow entry
    private boolean isDeniedEnterTerritory(KonTerritory territoryTo, KonPlayer player, boolean force) {
    	if(territoryTo == null) return false;// Unknown territory, just allow it
		if(force) return false; // Forced entry, allow it
    	// Admin bypass always enter
    	if(player.isAdminBypassActive()) return false;
    	// Friendlies can always enter
    	if(territoryTo.getKingdom().equals(player.getKingdom())) return false;
    	// Property Flag Holders
		if(territoryTo instanceof KonPropertyFlagHolder) {
			KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territoryTo;
			if(flagHolder.hasPropertyValue(KonPropertyFlag.ENTER)) {
				if(!flagHolder.getPropertyValue(KonPropertyFlag.ENTER)) {
					// When Player is in a vehicle, reverse the velocity and eject
					if(player.getBukkitPlayer().isInsideVehicle()) {
						Vehicle vehicle = (Vehicle) player.getBukkitPlayer().getVehicle();
						vehicle.setVelocity(vehicle.getVelocity().multiply(-4));
						vehicle.eject();
					}
					ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedFlagColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
					if(player.getBukkitPlayer().hasPermission("konquest.command.admin")) {
						ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_IGNORE.getMessage());
					}
					// Cancel the movement
					return true;
				}
			}
		}
		return false;
    }
    
    private void onEnterTerritory(KonTerritory territoryTo, Location locTo, Location locFrom, KonPlayer player) {
    	if(territoryTo == null) return;
    	// Update bars
		if(territoryTo instanceof KonBarDisplayer) {
			((KonBarDisplayer)territoryTo).addBarPlayer(player);
		}
		// Decide what to do for specific territories
		if(territoryTo instanceof KonTown) {
			KonTown town = (KonTown) territoryTo;
			// Notify player if town is abandoned
			if(town.getPlayerResidents().isEmpty() && town.getKingdom().equals(player.getKingdom())) {
				ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.COMMAND_TOWN_NOTICE_NO_LORD.getMessage(town.getName(),town.getTravelName()));
			}
			// Display plot message to friendly players
			displayPlotMessage(town, locTo, locFrom, player);
			// Command all nearby Iron Golems to target enemy player, if no other closer player is present
			town.updateGolemTargets(player,true);
			// Try to apply heart adjustments
			kingdomManager.applyTownHearts(player,town);
			
			if(!player.isAdminBypassActive() && !player.getBukkitPlayer().getGameMode().equals(GameMode.SPECTATOR) && !player.getKingdom().isPeaceful() ) {
				// Evaluate player's relationship for town alerts/nerfs
				if(kingdomManager.isPlayerEnemy(player, town.getKingdom())) {
					// Attempt to start a raid alert
					town.sendRaidAlert();
					// Apply town nerfs
					kingdomManager.applyTownNerf(player, town);
				} else if(kingdomManager.isPlayerFriendly(player, town.getKingdom())) {
					// Players entering friendly towns...
					kingdomManager.clearTownNerf(player);
				}
				
			}
		} else if(territoryTo instanceof KonCamp) {
			KonCamp camp = (KonCamp)territoryTo;
			// Attempt to start a raid alert
			if(!camp.isRaidAlertDisabled() && !player.isAdminBypassActive() && !player.getKingdom().isPeaceful()) {
				// Verify online player
				if(camp.isOwnerOnline()) {
					boolean isMember = false;
					if(konquest.getCampManager().isCampGrouped(camp)) {
						isMember = konquest.getCampManager().getCampGroup(camp).isPlayerMember(player.getBukkitPlayer());
					}
					// Alert the camp owner if player is not a group member and online
					KonPlayer ownerOnlinePlayer = konquest.getPlayerManager().getPlayerFromID(camp.getOwner().getUniqueId());
					if(ownerOnlinePlayer != null && !isMember && !player.getBukkitPlayer().getUniqueId().equals(camp.getOwner().getUniqueId())) {
						Player ownerBukkitPlayer = ownerOnlinePlayer.getBukkitPlayer();
						ChatUtil.sendNotice(ownerBukkitPlayer, MessagePath.PROTECTION_NOTICE_RAID.getMessage(camp.getName(),"camp"),ChatColor.DARK_RED);
						ChatUtil.sendKonPriorityTitle(ownerOnlinePlayer, ChatColor.DARK_RED+MessagePath.PROTECTION_NOTICE_RAID_ALERT.getMessage(), ChatColor.DARK_RED+""+camp.getName(), 60, 1, 10);
						// Start Raid Alert disable timer for target town
						int raidAlertTimeSeconds = konquest.getCore().getInt(CorePath.TOWNS_RAID_ALERT_COOLDOWN.getPath());
						ChatUtil.printDebug("Starting raid alert timer for "+raidAlertTimeSeconds+" seconds");
						Timer raidAlertTimer = camp.getRaidAlertTimer();
						camp.setIsRaidAlertDisabled(true);
						raidAlertTimer.stopTimer();
						raidAlertTimer.setTime(raidAlertTimeSeconds);
						raidAlertTimer.startTimer();
					}
				}
			}
		} else if(territoryTo instanceof KonRuin) {
			KonRuin ruin = (KonRuin)territoryTo;
			// Spawn all ruin golems
			ruin.spawnAllGolems();
		}
	}
    
    // Return true to deny exit, else false to allow exit
    private boolean isDeniedExitTerritory(KonTerritory territoryFrom, KonPlayer player, boolean force) {
    	if(territoryFrom == null) return false; // Unknown territory, just allow it
		if(force) return false; // Forced exit, allow it
    	// Admin bypass always exit
    	if(player.isAdminBypassActive()) return false;
    	// Friendlies can always exit
    	if(territoryFrom.getKingdom().equals(player.getKingdom())) return false;
    	// Property Flag Holders
		if(territoryFrom instanceof KonPropertyFlagHolder) {
			KonPropertyFlagHolder flagHolder = (KonPropertyFlagHolder)territoryFrom;
			if(flagHolder.hasPropertyValue(KonPropertyFlag.EXIT)) {
				if(!flagHolder.getPropertyValue(KonPropertyFlag.EXIT)) {
					// When Player is in a vehicle, reverse the velocity and eject
					if(player.getBukkitPlayer().isInsideVehicle()) {
						Vehicle vehicle = (Vehicle) player.getBukkitPlayer().getVehicle();
						vehicle.setVelocity(vehicle.getVelocity().multiply(-4));
						vehicle.eject();
					}
					ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedFlagColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
					if(player.getBukkitPlayer().hasPermission("konquest.command.admin")) {
						ChatUtil.sendNotice(player.getBukkitPlayer(), MessagePath.PROTECTION_NOTICE_IGNORE.getMessage());
					}
					// Cancel the movement
					return true;
				}
			}
		}
		return false;
    }
    
    private void onExitTerritory(KonTerritory territoryFrom, KonPlayer player) {
    	if(territoryFrom == null) return;
    	// Update bars
		if(territoryFrom instanceof KonBarDisplayer) {
			((KonBarDisplayer)territoryFrom).removeBarPlayer(player);
		}
    	// Decide what to do for specific territories
		if(territoryFrom instanceof KonTown) {
			KonTown town = (KonTown) territoryFrom;
			player.clearAllMobAttackers();
			// Command all nearby Iron Golems to target nearby enemy players, ignore triggering player
			town.updateGolemTargets(player,false);
			// Try to clear heart adjustments
			kingdomManager.clearTownHearts(player);
			// Remove potion effects
			kingdomManager.clearTownNerf(player);
		} else if(territoryFrom instanceof KonRuin) {
			KonRuin ruin = (KonRuin)territoryFrom;
			ruin.stopTargetingPlayer(player.getBukkitPlayer());
		}
    }

	/**
	 * Display messages:<br>
	 * 		into plot from non-plot (territory or wild)<br>
	 * 		into plot from other plot<br>
	 * 		out of plot, to town<br>
	 * 	    out of wild, to town<br>
	 */
    private void displayPlotMessage(KonTown town, Location toLoc, Location fromLoc, KonPlayer player) {
    	if(town.getKingdom().equals(player.getKingdom())) {
    		// Player is friendly
    		boolean isPlotTo = town.hasPlot(toLoc);
    		boolean isPlotFrom = town.hasPlot(fromLoc);

    		String plotMessage = "";
    		ChatColor plotMessageColor = ChatColor.GOLD;
    		boolean doDisplay = false;
    		// Display conditions
    		if(isPlotTo) {
    			KonPlot plotTo = town.getPlot(toLoc);
    			if(!isPlotFrom || !plotTo.equals(town.getPlot(fromLoc))) {
    				plotMessage = plotTo.getDisplayText();
					if(plotTo.hasUser(player.getBukkitPlayer())) {
    					plotMessageColor = ChatColor.DARK_GREEN;
    				}
    				doDisplay = true;
    			}
    		} else {
    			if((isPlotFrom || !town.isLocInside(fromLoc)) && town.isLocInside(toLoc)) {
    				// Moved out of plot or other territory or wild into town/capital land
					if(town instanceof KonCapital) {
						plotMessage = MessagePath.MENU_PLOTS_CAPITAL_LAND.getMessage();
					} else {
						plotMessage = MessagePath.MENU_PLOTS_TOWN_LAND.getMessage();
					}
    				plotMessageColor = ChatColor.DARK_GREEN;
    				doDisplay = true;
    			}
    		}
    		// Display message
    		if(!doDisplay) return;
			player.getBukkitPlayer().spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(plotMessageColor+plotMessage));
    	}
    }

	// Returns true if use was canceled, else false
    private boolean preventUse(PlayerInteractEvent event, KonPlayer player) {
    	if(event.hasBlock() && event.getClickedBlock() != null) {
			// Prevent use of specific usable blocks
			BlockState clickedState = event.getClickedBlock().getState();
			BlockData clickedBlockData = clickedState.getBlockData();
			if(clickedBlockData instanceof AnaloguePowerable ||
					clickedBlockData instanceof Powerable ||
					clickedState.getType().isInteractable()) {
				event.setUseInteractedBlock(Event.Result.DENY);
				if(!(clickedState instanceof Sign)) {
					// Only display "Blocked" title when not interacting with a sign
					// This is mainly for using chest shops
					ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedProtectionColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
				}
				return true;
			}
		}
		return false;
    }

	// Returns true if physical interaction was canceled, else false
	private boolean preventPhysical(PlayerInteractEvent event, KonPlayer player) {
		if(event.hasBlock() && event.getClickedBlock() != null) {
			BlockData clickedBlockData = event.getClickedBlock().getBlockData();
			if(event.getAction().equals(Action.PHYSICAL) && clickedBlockData instanceof Farmland) {
				// Prevent all physical stepping interaction, like trampling farmland
				event.setUseInteractedBlock(Event.Result.DENY);
				ChatUtil.sendKonPriorityTitle(player, "", Konquest.blockedProtectionColor+MessagePath.PROTECTION_ERROR_BLOCKED.getMessage(), 1, 10, 10);
				return true;
			}
		}
		return false;
	}
    
}
