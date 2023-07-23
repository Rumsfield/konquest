package com.github.rumsfield.konquest.manager;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.api.event.territory.KonquestTerritoryChunkEvent;
import com.github.rumsfield.konquest.api.manager.KonquestTerritoryManager;
import com.github.rumsfield.konquest.api.model.KonquestTerritoryType;
import com.github.rumsfield.konquest.model.*;
import com.github.rumsfield.konquest.utility.ChatUtil;
import com.github.rumsfield.konquest.utility.CorePath;
import com.github.rumsfield.konquest.utility.MessagePath;
import com.github.rumsfield.konquest.utility.Timer;
import org.bukkit.Color;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Snow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.*;
import java.util.function.Predicate;

public class TerritoryManager implements KonquestTerritoryManager {

	private final Konquest konquest;
	private final HashMap<World,KonTerritoryCache> territoryWorldCache;
	
	public static final int DEFAULT_MAP_SIZE = 9; // 10 lines of chat, minus one for header, odd so player's chunk is centered
	
	public TerritoryManager(Konquest konquest) {
		this.konquest = konquest;
		this.territoryWorldCache = new HashMap<>();
	}
	
	/*
	 * Generic territory methods
	 */
	
	public void addAllTerritory(World world, HashMap<Point,KonTerritory> pointMap) {
		if(territoryWorldCache.containsKey(world)) {
			territoryWorldCache.get(world).putAll(pointMap);
		} else {
			KonTerritoryCache cache = new KonTerritoryCache();
			cache.putAll(pointMap);
			territoryWorldCache.put(world, cache);
		}
	}
	
	public void addTerritory(World world, Point point, KonTerritory territory) {
		if(territoryWorldCache.containsKey(world)) {
			territoryWorldCache.get(world).put(point, territory);
		} else {
			KonTerritoryCache cache = new KonTerritoryCache();
			cache.put(point, territory);
			territoryWorldCache.put(world, cache);
		}
	}
	
	public boolean removeAllTerritory(World world, Collection<Point> points) {
		boolean result = false;
		if(territoryWorldCache.containsKey(world)) {
			if(territoryWorldCache.get(world).removeAll(points)) {
				result = true;
			} else {
				ChatUtil.printDebug("Failed to remove all points for territory from world cache: "+world.getName());
			}
		} else {
			ChatUtil.printDebug("Failed to find world cache for bulk point removal: "+world.getName());
		}
		return result;
	}
	
	public boolean removeTerritory(Location loc) {
		return removeTerritory(loc.getWorld(),Konquest.toPoint(loc));
	}
	
	public boolean removeTerritory(World world, Point point) {
		boolean result = false;
		if(territoryWorldCache.containsKey(world)) {
			if(territoryWorldCache.get(world).remove(point)) {
				result = true;
			} else {
				ChatUtil.printDebug("Failed to remove single point for territory from world cache: "+world.getName());
			}
		} else {
			ChatUtil.printDebug("Failed to find world cache for point removal: "+world.getName());
		}
		return result;
	}
	
	public boolean isChunkClaimed(Location loc) {
		return isChunkClaimed(Konquest.toPoint(loc),loc.getWorld());
	}
	
	public boolean isChunkClaimed(Point point, World world) {
		boolean result = false;
		if(territoryWorldCache.containsKey(world)) {
			result = territoryWorldCache.get(world).has(point);
		}
		return result;
	}
	
	@Nullable
	public KonTerritory getChunkTerritory(Location loc) {
		return getChunkTerritory(Konquest.toPoint(loc),loc.getWorld());
	}
	
	@Nullable
	public KonTerritory getChunkTerritory(Point point, World world) {
		if(territoryWorldCache.containsKey(world)) {
			return territoryWorldCache.get(world).get(point);
		} else {
			return null;
		}
	}
	
	public KonTerritory getAdjacentTerritory(Location loc) {
		// Find adjacent or current territory
		KonTerritory closestTerritory = null;
		if(isChunkClaimed(loc)) {
			closestTerritory = getChunkTerritory(loc);
		} else {
			KonTerritory currentTerritory;
			int searchDist;
			int minDistance = Integer.MAX_VALUE;
			for(Point areaPoint : konquest.getSidePoints(loc)) {
				if(isChunkClaimed(areaPoint,loc.getWorld())) {
					currentTerritory = getChunkTerritory(areaPoint,loc.getWorld());
					searchDist = Konquest.chunkDistance(loc, currentTerritory.getCenterLoc());
					if(searchDist != -1) {
						if(searchDist < minDistance) {
							minDistance = searchDist;
							closestTerritory = currentTerritory;
						}
					}
				}
			}
		}
		return closestTerritory;
	}
	
	public int getDistanceToClosestTerritory(Location loc) {
		int minDistance = Integer.MAX_VALUE;
		int searchDist = 0;
		for(KonKingdom kingdom : konquest.getKingdomManager().getKingdoms()) {
			searchDist = Konquest.chunkDistance(loc, kingdom.getCapital().getCenterLoc());
			if(searchDist != -1 && searchDist < minDistance) {
				minDistance = searchDist;
			}
			for(KonTown town : kingdom.getTowns()) {
				searchDist = Konquest.chunkDistance(loc, town.getCenterLoc());
				if(searchDist != -1 && searchDist < minDistance) {
					minDistance = searchDist;
				}
			}
		}
		for(KonRuin ruin : konquest.getRuinManager().getRuins()) {
			searchDist = Konquest.chunkDistance(loc, ruin.getCenterLoc());
			if(searchDist != -1 && searchDist < minDistance) {
				minDistance = searchDist;
			}
		}
		for(KonSanctuary sanctuary : konquest.getSanctuaryManager().getSanctuaries()) {
			searchDist = Konquest.chunkDistance(loc, sanctuary.getCenterLoc());
			if(searchDist != -1 && searchDist < minDistance) {
				minDistance = searchDist;
			}
		}
		return minDistance;
	}
	
	
	/*
	 * Claim / Unclaim Methods
	 */
	
	/**
	 * claimChunk - Primary method for claiming chunks for closest adjacent territories
	 * @param loc - Location of the chunk to claim
	 * @param force - Ignore some checks
	 * @return  0 - success
	 * 			1 - error, no adjacent territory
	 * 			2 - error, exceeds max distance
	 * 			3 - error, already claimed
	 * 			4 - error, cancelled by event
	 * 			5 - error, blocked by property flag
	 */
	public int claimChunk(Location loc, boolean force) {
		if(isChunkClaimed(loc)) {
			return 3;
		}
		boolean foundAdjTerr = false;
		KonTerritory closestAdjTerr = null;
		KonTerritory currentTerr;
		World claimWorld = loc.getWorld();
		int searchDist;
		int minDistance = Integer.MAX_VALUE;
		for(Point sidePoint : konquest.getSidePoints(loc)) {
			if(isChunkClaimed(sidePoint,claimWorld)) {
				currentTerr = getChunkTerritory(sidePoint,claimWorld);
				searchDist = Konquest.chunkDistance(loc, currentTerr.getCenterLoc());
				if(searchDist != -1) {
					if(searchDist < minDistance) {
						minDistance = searchDist;
						closestAdjTerr = currentTerr;
						foundAdjTerr = true;
					}
				}
			}
		}
		if(foundAdjTerr) {
			Point addPoint = Konquest.toPoint(loc);
			if(closestAdjTerr.getWorld().equals(loc.getWorld()) && closestAdjTerr.testChunk(addPoint)) {
				// Check property flag
				if(closestAdjTerr instanceof KonPropertyFlagHolder && !force) {
					KonPropertyFlagHolder holder = (KonPropertyFlagHolder)closestAdjTerr;
					if(holder.hasPropertyValue(KonPropertyFlag.CLAIM) && !holder.getPropertyValue(KonPropertyFlag.CLAIM)) {
						return 5;
					}
				}
				// Fire event
				Set<Point> points = new HashSet<>();
				points.add(addPoint);
				KonquestTerritoryChunkEvent invokeEvent = new KonquestTerritoryChunkEvent(konquest, closestAdjTerr, loc, points, true);
				Konquest.callKonquestEvent(invokeEvent);
				if(invokeEvent.isCancelled()) {
					return 4;
				}
				// Add territory
				closestAdjTerr.addChunk(addPoint);
				addTerritory(loc.getWorld(),addPoint,closestAdjTerr);
				konquest.getMapHandler().drawDynmapUpdateTerritory(closestAdjTerr);
				konquest.getMapHandler().drawDynmapLabel(closestAdjTerr.getKingdom().getCapital());
				// Display town info to players in the newly claimed chunk
	    		for(KonPlayer occupant : konquest.getPlayerManager().getPlayersOnline()) {
	    			if(occupant.getBukkitPlayer().getLocation().getChunk().equals(loc.getChunk())) {
						String color = konquest.getDisplayPrimaryColor(occupant, closestAdjTerr);
	    				ChatUtil.sendKonTitle(occupant, "", color+closestAdjTerr.getName());
	    				if(closestAdjTerr instanceof KonBarDisplayer) {
	    					((KonBarDisplayer)closestAdjTerr).addBarPlayer(occupant);
	    				}
	    				updatePlayerBorderParticles(occupant);
	    			}
	    		}
	    		
				return 0;
			} else {
				return 2;
			}
		}
		return 1;
	}
	
	// Main method for admins to claim an individual chunk.
	// Calls event.
	public void claimForAdmin(@NotNull KonPlayer player, @NotNull Location claimLoc) {
		Player bukkitPlayer = player.getBukkitPlayer();
		int claimStatus = claimChunk(claimLoc,true);
    	switch(claimStatus) {
    	case 0:
    		KonTerritory territory = getChunkTerritory(claimLoc);
    		ChatUtil.sendNotice(bukkitPlayer, MessagePath.GENERIC_NOTICE_SUCCESS.getMessage());
    		// Push claim to admin register
    		Set<Point> claimSet = new HashSet<>();
    		claimSet.add(Konquest.toPoint(claimLoc));
    		player.getAdminClaimRegister().push(claimSet, territory);
    		break;
    	case 1:
    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_FAIL_ADJACENT.getMessage());
    		break;
    	case 2:
    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_FAIL_FAR.getMessage());
    		break;
    	case 3:
    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_FAIL_CLAIMED.getMessage());
    		break;
    	case 4:
			// Cancelled by external event handler, skip error message
    		break;
    	default:
    		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(claimStatus));
    		break;
    	}
    }
	
	// Main method for players to claim an individual chunk.
	// Calls event.
	public boolean claimForPlayer(@NotNull KonPlayer player, @NotNull Location claimLoc) {
		Player bukkitPlayer = player.getBukkitPlayer();
		World claimWorld = claimLoc.getWorld();
		// Verify no surrounding territory from other kingdoms
    	for(Point point : konquest.getAreaPoints(claimLoc,2)) {
			if(isChunkClaimed(point,claimWorld)) {
				KonTerritory territory = getChunkTerritory(point,claimWorld);
				if(territory != null && !player.getKingdom().equals(territory.getKingdom())) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_PROXIMITY.getMessage());
					return false;
				}
			}
    	}
    	// Ensure player can cover the cost
    	double cost = konquest.getCore().getDouble(CorePath.FAVOR_COST_CLAIM.getPath());
    	if(KonquestPlugin.getBalance(bukkitPlayer) < cost) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(cost));
            return false;
		}
    	// Attempt to claim the current chunk
    	int claimStatus = claimChunk(claimLoc,false);
    	switch(claimStatus) {
    	case 0:
    		KonTerritory territory = getChunkTerritory(claimLoc);
    		String territoryName = territory.getName();
    		// Send message
    		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_CLAIM_NOTICE_SUCCESS.getMessage("1",territoryName));
    		konquest.getDirectiveManager().updateDirectiveProgress(player, KonDirective.CLAIM_LAND);
    		konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CLAIMED,1);
    		break;
    	case 1:
    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_FAIL_ADJACENT.getMessage());
    		break;
    	case 2:
    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_FAIL_FAR.getMessage());
    		break;
    	case 3:
    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_FAIL_CLAIMED.getMessage());
    		break;
    	case 4:
			// Cancelled by external event handler, skip error message
    		break;
		case 5:
			ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_BLOCKED.getMessage());
			break;
    	default:
    		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(claimStatus));
    		break;
    	}
    	// Reduce the player's favor
		if(cost > 0 && claimStatus == 0) {
            if(KonquestPlugin.withdrawPlayer(bukkitPlayer, cost)) {
            	konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)cost);
            }
		}
		return claimStatus == 0;
	}
	
	/**
	 * claimChunkRadius - Primary method for claiming area of chunks for closest adjacent territories
	 * @return  0 - success
	 * 			1 - error, no adjacent territory
	 * 			2 - error, no chunks claimed
	 * 			4 - error, event cancelled
	 * 			5 - error, 	blocked by property flag
	 */
	public int claimChunkRadius(Location loc, int radius, boolean force) {
		World claimWorld = loc.getWorld();
		
		// Find adjacent or current territory
		KonTerritory closestTerritory = getAdjacentTerritory(loc);
		if(closestTerritory == null || !closestTerritory.getWorld().equals(claimWorld)) {
			return 1;
		}
		
    	// Find all unclaimed chunks to claim
		HashSet<Point> unclaimedChunks = new HashSet<>();
    	for(Point point : konquest.getAreaPoints(loc,radius)) {
    		if(!isChunkClaimed(point,claimWorld)) {
    			unclaimedChunks.add(point);
    		}
    	}
    	
    	// Test chunks to see if they are allowed to be added to the territory
		boolean isAnyChunkBeyondDistanceLimit = false;
    	HashSet<Point> claimedChunks = new HashSet<>();
    	for(Point newChunk : unclaimedChunks) {
			if(closestTerritory.testChunk(newChunk)) {
				claimedChunks.add(newChunk);
			} else {
				isAnyChunkBeyondDistanceLimit = true;
			}
		}
		if(isAnyChunkBeyondDistanceLimit) {
			ChatUtil.printDebug("Not all chunks could be claimed due to max distance limit");
		}
    	
    	if(claimedChunks.isEmpty()) {
    		return 2;
    	} else {
			// Check property flag
			if(closestTerritory instanceof KonPropertyFlagHolder && !force) {
				KonPropertyFlagHolder holder = (KonPropertyFlagHolder)closestTerritory;
				if(holder.hasPropertyValue(KonPropertyFlag.CLAIM) && !holder.getPropertyValue(KonPropertyFlag.CLAIM)) {
					return 5;
				}
			}
    		// Fire event
			Set<Point> points = new HashSet<>(claimedChunks);
			KonquestTerritoryChunkEvent invokeEvent = new KonquestTerritoryChunkEvent(konquest, closestTerritory, loc, points, true);
			Konquest.callKonquestEvent(invokeEvent);
			if(invokeEvent.isCancelled()) {
				return 4;
			}
			// Add territory
			for(Point claimChunk : claimedChunks) {
				closestTerritory.addChunk(claimChunk);
				addTerritory(claimWorld,claimChunk,closestTerritory);
			}
	    	// Update map render
			konquest.getMapHandler().drawDynmapUpdateTerritory(closestTerritory);
			konquest.getMapHandler().drawDynmapLabel(closestTerritory.getKingdom().getCapital());
			// Display territory info to players in the newly claimed chunks
			for(KonPlayer occupant : konquest.getPlayerManager().getPlayersOnline()) {
				//if(occupant.getBukkitPlayer().getWorld().equals(claimWorld) && claimedChunks.contains(Konquest.toPoint(occupant.getBukkitPlayer().getLocation()))) {
				if(closestTerritory.isLocInside(occupant.getBukkitPlayer().getLocation())) {
					String color = konquest.getDisplayPrimaryColor(occupant, closestTerritory);
					ChatUtil.sendKonTitle(occupant, "", color+closestTerritory.getName());
					if(closestTerritory instanceof KonBarDisplayer) {
						((KonBarDisplayer)closestTerritory).addBarPlayer(occupant);
					}
					updatePlayerBorderParticles(occupant);
				}
			}
    	}

		return 0;
	}
	
	public boolean claimRadiusForAdmin(@NotNull KonPlayer player, Location claimLoc, int radius) {
		World claimWorld = claimLoc.getWorld();
		Player bukkitPlayer = player.getBukkitPlayer();
		// Find adjacent or current territory
		KonTerritory claimTerritory = getAdjacentTerritory(claimLoc);
		if(claimTerritory == null) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_MISSING.getMessage());
    		return false;
		}
		
		// Verify no overlapping territories
		for(Point point : konquest.getAreaPoints(claimLoc,radius+1)) {
    		if(isChunkClaimed(point,claimWorld)) {
    			if(!claimTerritory.getKingdom().equals(getChunkTerritory(point,claimWorld).getKingdom())) {
    				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_PROXIMITY.getMessage());
    				return false;
    			}
    		}
    	}
		
		// Find all unclaimed chunks to claim
		HashSet<Point> toClaimChunks = new HashSet<>();
    	for(Point point : konquest.getAreaPoints(claimLoc,radius)) {
    		if(!isChunkClaimed(point,claimWorld) && claimTerritory.testChunk(point)) {
    			toClaimChunks.add(point);
    		}
    	}
    	
		// Attempt to claim
    	int preClaimLand = claimTerritory.getChunkPoints().size();
    	int postClaimLand;
    	int claimStatus = claimChunkRadius(claimLoc, radius, true);
    	switch(claimStatus) {
	    	case 0:
	    		postClaimLand = claimTerritory.getChunkPoints().size();
	    		int numChunksClaimed = postClaimLand - preClaimLand;
	    		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_CLAIM_NOTICE_SUCCESS.getMessage(numChunksClaimed,claimTerritory.getName()));
	    		// Push claim to admin register
	    		Set<Point> claimSet = new HashSet<>(toClaimChunks);
	    		player.getAdminClaimRegister().push(claimSet, claimTerritory);
	    		break;
	    	case 1:
	    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_FAIL_ADJACENT.getMessage());
	    		return false;
	    	case 2:
	    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_FAIL_FAR.getMessage());
	    		return false;
	    	case 4:
				// Cancelled by external event handler, skip error message
	    		return false;
	    	default:
	    		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(claimStatus));
	    		return false;
    	}
		return true;
	}
	
	public boolean claimRadiusForPlayer(@NotNull KonPlayer player, Location claimLoc, int radius) {
		Player bukkitPlayer = player.getBukkitPlayer();
		World claimWorld = claimLoc.getWorld();
		// Verify no surrounding territory from other kingdoms
		for(Point point : konquest.getAreaPoints(claimLoc,radius+1)) {
			if(isChunkClaimed(point,claimWorld)) {
				KonTerritory territory = getChunkTerritory(point,claimWorld);
				if(territory != null && !player.getKingdom().equals(territory.getKingdom())) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_PROXIMITY.getMessage());
					return false;
				}
			}
		}
    	// Find adjacent or current territory
		KonTerritory claimTerritory = getAdjacentTerritory(claimLoc);
		if(claimTerritory == null) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_MISSING.getMessage());
    		return false;
		}
    	// Find all unclaimed chunks to claim
		HashSet<Point> toClaimChunks = new HashSet<>();
    	for(Point point : konquest.getAreaPoints(claimLoc,radius)) {
    		if(!isChunkClaimed(point,claimWorld) && claimTerritory.testChunk(point)) {
    			toClaimChunks.add(point);
    		}
    	}
    	// Ensure player can cover the cost
    	int unclaimedChunkAmount = toClaimChunks.size();
    	double cost = konquest.getCore().getDouble(CorePath.FAVOR_COST_CLAIM.getPath());
    	double totalCost = unclaimedChunkAmount * cost;
    	if(KonquestPlugin.getBalance(bukkitPlayer) < totalCost) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_NO_FAVOR.getMessage(totalCost));
            return false;
		}
    	// Attempt to claim
    	int preClaimLand = claimTerritory.getChunkPoints().size();
    	int postClaimLand;
    	int claimStatus = claimChunkRadius(claimLoc, radius, false);
    	switch(claimStatus) {
	    	case 0:
	    		postClaimLand = claimTerritory.getChunkPoints().size();
	    		int numChunksClaimed = postClaimLand - preClaimLand;
	    		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_CLAIM_NOTICE_SUCCESS.getMessage(numChunksClaimed,claimTerritory.getName()));
	    		konquest.getDirectiveManager().updateDirectiveProgress(player, KonDirective.CLAIM_LAND);
	    		konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CLAIMED,numChunksClaimed);
	    		// Reduce the player's favor
	    		if(cost > 0 && numChunksClaimed > 0) {
	    			double finalCost = numChunksClaimed*cost;
	    	        if(KonquestPlugin.withdrawPlayer(bukkitPlayer, finalCost)) {
	    	        	konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.FAVOR,(int)finalCost);
	    	        }
	    		}
	    		break;
	    	case 1:
	    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_FAIL_ADJACENT.getMessage());
	    		return false;
	    	case 2:
	    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_CLAIM_ERROR_FAIL_FAR.getMessage());
	    		return false;
	    	case 4:
				// Cancelled by external event handler, skip error message
	    		return false;
			case 5:
				ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_BLOCKED.getMessage());
				return false;
	    	default:
	    		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(claimStatus));
	    		return false;
    	}
		return true;
	}
	
	/*
	 * Main method for removing/un-claiming chunks of territory.
	 * Handles territory spawn set to center if chunk contains spawn.
	 */
	// Returns true when any points have been removed.
	// Returns false when no points were removed.
	private boolean removeClaims(Set<Point> points, KonTerritory territory) {
		
		if(territory == null || points == null || points.isEmpty()) {
			return false;
		}
		
		World territoryWorld = territory.getWorld();
		Point territorySpawn = Konquest.toPoint(territory.getSpawnLoc());
		
		// Player occupants
		Set<KonPlayer> occupants = new HashSet<>();
		for(KonPlayer occupant : konquest.getPlayerManager().getPlayersOnline()) {
			if(territory.isLocInside(occupant.getBukkitPlayer().getLocation())) {
				occupants.add(occupant);
			}
		}
		
		// Remove territory
		boolean doUpdates = false;
		boolean allowUnclaim;
		for(Point unclaimPoint : points) {
			allowUnclaim = true;
			// Check for allowed conditions
			if(territory.getChunkList().containsKey(unclaimPoint)) {
				if(territory.getTerritoryType().equals(KonquestTerritoryType.TOWN)) {
					// Remove any town plot points
					konquest.getPlotManager().removePlotPoint((KonTown)territory, unclaimPoint, territoryWorld);
				} else if(territory.getTerritoryType().equals(KonquestTerritoryType.SANCTUARY)) {
					// Prevent removing claims over monument templates
					KonSanctuary sanctuary = (KonSanctuary)territory;
					boolean status = sanctuary.isChunkOnTemplate(unclaimPoint, territoryWorld);
					if(status) {
						allowUnclaim = false;
					}
				}
			}
			// Attempt to remove
			if(allowUnclaim && territory.removeChunk(unclaimPoint)) {
				// Check for spawn
				if(territorySpawn.equals(unclaimPoint)) {
					// Chunk containing spawn is being removed
					// TODO: refresh from monument template if town?
					territory.setSpawn(territory.getCenterLoc());
				}
				removeTerritory(territoryWorld,unclaimPoint);
				doUpdates = true;
			}
		}
		if(doUpdates) {
			for(KonPlayer occupant : occupants) {
				updatePlayerBorderParticles(occupant);
    		}
			if(territory instanceof KonBarDisplayer) {
				((KonBarDisplayer)territory).updateBarPlayers();
			}
			konquest.getMapHandler().drawDynmapUpdateTerritory(territory);
			konquest.getMapHandler().drawDynmapLabel(territory.getKingdom().getCapital());
		}

		return doUpdates;
	}
	
	
	/**
	 * @return True when undo was successful, False when undo failed (nothing to undo)
	 */
	public boolean claimUndoForAdmin(KonPlayer player) {
		
		if(player == null) return false;
		
		Set<Point> claimPoints = player.getAdminClaimRegister().getClaim();
		KonTerritory claimTerritory = player.getAdminClaimRegister().getTerritory();
		player.getAdminClaimRegister().pop();
		
		// Attempt removal

		return removeClaims(claimPoints, claimTerritory);
	}
	
	/**
	 * primary method for un-claiming a chunk for a territory
	 * @param loc Location of the chunk to un-claim
	 * @return  0 - success
	 * 			1 - error, no territory at location
	 * 			2 - error, center chunk
	 * 			3 - error, territory chunk not found
	 * 			4 - error, cancelled by event
	 * 			5 - error, blocked by property flag
	 */
	public int unclaimChunk(Location loc, boolean force) {
		if(!isChunkClaimed(loc)) {
			return 1;
		}
		
		KonTerritory territory = getChunkTerritory(loc);
		if(territory.isLocInCenter(loc)) {
			return 2;
		}
		// Check property flag
		if(territory instanceof KonPropertyFlagHolder && !force) {
			KonPropertyFlagHolder holder = (KonPropertyFlagHolder)territory;
			if(holder.hasPropertyValue(KonPropertyFlag.UNCLAIM) && !holder.getPropertyValue(KonPropertyFlag.UNCLAIM)) {
				return 5;
			}
		}
		// Fire event
		Set<Point> points = new HashSet<>();
		points.add(Konquest.toPoint(loc));
		KonquestTerritoryChunkEvent invokeEvent = new KonquestTerritoryChunkEvent(konquest, territory, loc, points, false);
		Konquest.callKonquestEvent(invokeEvent);
		if(invokeEvent.isCancelled()) {
			return 4;
		}
		
		// Attempt removal
		boolean status = removeClaims(points, territory);

		return status ? 0 : 3;
	}
	
	// Main method for admins to un-claim an individual chunk.
    // Calls event.
	public boolean unclaimForAdmin(Player bukkitPlayer, Location claimLoc) {
		int unclaimStatus = unclaimChunk(claimLoc, true);
    	switch(unclaimStatus) {
    	case 0:
			String territoryName = getChunkTerritory(claimLoc).getName();
			ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_UNCLAIM_NOTICE_SUCCESS.getMessage("1",territoryName));
    		break;
    	case 1:
    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_UNCLAIM_ERROR_FAIL_UNCLAIMED.getMessage());
    		break;
    	case 2:
    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_UNCLAIM_ERROR_FAIL_CENTER.getMessage());
    		break;
    	case 3:
    		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    		break;
    	case 4:
			// Cancelled by external event handler, skip error message
    		break;
    	default:
    		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(unclaimStatus));
    		break;
    	}
    	
    	return unclaimStatus == 0;
	}
	
	public boolean unclaimForPlayer(Player bukkitPlayer, Location claimLoc) {
		// Verify town at location, and player is lord
		if(this.isChunkClaimed(claimLoc)) {
			KonTerritory territory = this.getChunkTerritory(claimLoc);
			if(territory instanceof KonTown) {
				KonTown town = (KonTown)territory;
				if(!town.isPlayerLord(bukkitPlayer)) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_UNCLAIM_ERROR_FAIL_LORD.getMessage());
					return false;
				}
			} else {
				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_UNCLAIM_ERROR_FAIL_TOWN.getMessage());
				return false;
			}
		} else {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_UNCLAIM_ERROR_FAIL_UNCLAIMED.getMessage());
			return false;
		}
		
		// Get territory name
		KonTerritory territory = getChunkTerritory(claimLoc);
		String territoryName = territory.getName();
		
		// Attempt to unclaim the current chunk
    	int unclaimStatus = unclaimChunk(claimLoc,false);
    	switch(unclaimStatus) {
    	case 0:
    		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_UNCLAIM_NOTICE_SUCCESS.getMessage("1",territoryName));
    		break;
    	case 1:
    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_UNCLAIM_ERROR_FAIL_UNCLAIMED.getMessage());
    		break;
    	case 2:
    		ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_UNCLAIM_ERROR_FAIL_CENTER.getMessage());
    		break;
    	case 3:
    		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    		break;
		case 4:
			// Cancelled by external event handler, skip error message
			break;
		case 5:
			ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_BLOCKED.getMessage());
			break;
    	default:
    		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(unclaimStatus));
    		break;
    	}
		
		return unclaimStatus == 0;
	}
	
	/**
	 * Primary method for un-claiming area of chunks for territory at location
	 * @return  0 - success
	 * 			1 - error, no territory at location
	 * 			2 - error, no chunks unclaimed
	 * 			3 - error, chunks do not belong to territory
	 * 			4 - error, event cancelled
	 * 			5 - error, blocked by property flag
	 */
	public int unclaimChunkRadius(Location loc, int radius, boolean force) {
		if(!isChunkClaimed(loc)) return 1;
		
		KonTerritory territory = getChunkTerritory(loc);
		
		// Find all claimed chunks to unclaim (not center)
		HashSet<Point> claimedChunks = new HashSet<>();
		Point territoryCenter = Konquest.toPoint(territory.getCenterLoc());
    	for(Point point : konquest.getAreaPoints(loc,radius)) {
    		if(territory.getChunkPoints().contains(point) && !territoryCenter.equals(point)) {
    			claimedChunks.add(point);
    		}
    	}
    	
    	if(claimedChunks.isEmpty()) {
    		return 2;
    	}
		// Check property flag
		if(territory instanceof KonPropertyFlagHolder && !force) {
			KonPropertyFlagHolder holder = (KonPropertyFlagHolder)territory;
			if(holder.hasPropertyValue(KonPropertyFlag.UNCLAIM) && !holder.getPropertyValue(KonPropertyFlag.UNCLAIM)) {
				return 5;
			}
		}
		// Fire event
		Set<Point> points = new HashSet<>(claimedChunks);
		KonquestTerritoryChunkEvent invokeEvent = new KonquestTerritoryChunkEvent(konquest, territory, loc, points, false);
		Konquest.callKonquestEvent(invokeEvent);
		if(invokeEvent.isCancelled()) {
			return 4;
		}
		
		// Attempt removal
		boolean status = removeClaims(points, territory);

		return status ? 0 : 3;
	}
	
	public boolean unclaimRadiusForAdmin(Player bukkitPlayer, Location claimLoc, int radius) {

		// Find adjacent or current territory
		KonTerritory unclaimTerritory = getChunkTerritory(claimLoc);
		if(unclaimTerritory == null) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_UNCLAIM_ERROR_FAIL_UNCLAIMED.getMessage());
    		return false;
		}
		
		// Attempt to unclaim
    	int preClaimLand = unclaimTerritory.getChunkPoints().size();
    	int postClaimLand;
    	int unclaimStatus = unclaimChunkRadius(claimLoc, radius, true);
    	switch(unclaimStatus) {
	    	case 0:
	    		postClaimLand = unclaimTerritory.getChunkPoints().size();
	    		int numChunksClaimed = preClaimLand - postClaimLand;
	    		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_UNCLAIM_NOTICE_SUCCESS.getMessage(numChunksClaimed,unclaimTerritory.getName()));
	    		break;
	    	case 1:
			case 2:
				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_UNCLAIM_ERROR_FAIL_UNCLAIMED.getMessage());
				break;
			case 3:
	    		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
				break;
	    	case 4:
				// Cancelled by external event handler, skip error message
				break;
	    	default:
	    		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(unclaimStatus));
				break;
    	}
		
		return unclaimStatus == 0;
	}
	
	public boolean unclaimRadiusForPlayer(Player bukkitPlayer, Location claimLoc, int radius) {
		//KonPlayer player = konquest.getPlayerManager().getPlayer(bukkitPlayer);
		// Verify town at location, and player is lord
		if(this.isChunkClaimed(claimLoc)) {
			KonTerritory territory = this.getChunkTerritory(claimLoc);
			if(territory instanceof KonTown) {
				KonTown town = (KonTown)territory;
				if(!town.isPlayerLord(bukkitPlayer)) {
					ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_UNCLAIM_ERROR_FAIL_LORD.getMessage());
					return false;
				}
			} else {
				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_UNCLAIM_ERROR_FAIL_TOWN.getMessage());
				return false;
			}
		} else {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_UNCLAIM_ERROR_FAIL_UNCLAIMED.getMessage());
			return false;
		}
		
		// Find adjacent or current territory
		KonTerritory unclaimTerritory = getChunkTerritory(claimLoc);
		if(unclaimTerritory == null) {
			ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_UNCLAIM_ERROR_FAIL_UNCLAIMED.getMessage());
    		return false;
		}
		
		// Attempt to un-claim
    	int preClaimLand = unclaimTerritory.getChunkPoints().size();
    	int postClaimLand;
    	int unclaimStatus = unclaimChunkRadius(claimLoc, radius, false);
    	switch(unclaimStatus) {
	    	case 0:
	    		postClaimLand = unclaimTerritory.getChunkPoints().size();
	    		int numChunksClaimed = preClaimLand - postClaimLand;
	    		ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_UNCLAIM_NOTICE_SUCCESS.getMessage(numChunksClaimed,unclaimTerritory.getName()));
	    		//konquest.getAccomplishmentManager().modifyPlayerStat(player,KonStatsType.CLAIMED,-1*numChunksClaimed);
	    		break;
	    	case 1:
			case 2:
				ChatUtil.sendError(bukkitPlayer, MessagePath.COMMAND_UNCLAIM_ERROR_FAIL_UNCLAIMED.getMessage());
				break;
			case 3:
	    		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
				break;
			case 4:
				// Cancelled by external event handler, skip error message
				break;
			case 5:
				ChatUtil.sendError(bukkitPlayer, MessagePath.PROTECTION_ERROR_BLOCKED.getMessage());
				break;
	    	default:
	    		ChatUtil.sendError(bukkitPlayer, MessagePath.GENERIC_ERROR_INTERNAL_MESSAGE.getMessage(unclaimStatus));
				break;
    	}
		
		return unclaimStatus == 0;
	}
	
	/*
	 * Border methods
	 */
	
	/**
	 * 
	 * @param renderChunks - List of chunks to check for borders
	 * @param player - Player to render borders for
	 * @return A map of border particle locations with colors
	 */
	public HashMap<Location,Color> getBorderLocationMap(ArrayList<Chunk> renderChunks, KonPlayer player) {
		HashMap<Location,Color> locationMap = new HashMap<>();
		//Location playerLoc = player.getBukkitPlayer().getLocation();
		final int Y_MIN_LIMIT = player.getBukkitPlayer().getWorld().getMinHeight()+1;
		final int Y_MAX_LIMIT = player.getBukkitPlayer().getWorld().getMaxHeight()-1;
		final double X_MOD = 0.5;
		final double Y_MOD = 1;
		final double Y_MOD_SNOW = 1.1;
		final double Z_MOD = 0.5;
		// Search pattern look-up tables
		final int[] sideLUTX     = {0,  0,  1, -1};
		final int[] sideLUTZ     = {1, -1,  0,  0};
		final int[] blockLUTXmin = {0,  0,  15, 0};
		final int[] blockLUTXmax = {15, 15, 15, 0};
		final int[] blockLUTZmin = {15, 0,  0,  0};
		final int[] blockLUTZmax = {15, 0,  15, 15};
		// Iterative variables
		Point sidePoint;
		boolean isClaimed;
		int y_max;
		double y_mod;
		// Evaluate every chunk in the provided list. If it's claimed, check each adjacent chunk and determine border locations
		for(Chunk chunk : renderChunks) {
			Point point = Konquest.toPoint(chunk);
			World renderWorld = chunk.getWorld();
			if(isChunkClaimed(point,renderWorld)) {
				KonTerritory territory = getChunkTerritory(point,renderWorld);
				KonKingdom chunkKingdom = territory.getKingdom();
				Location renderLoc;
				Color renderColor = ChatUtil.lookupColor(konquest.getDisplayPrimaryColor(player, territory));

				// Iterate all 4 sides of the chunk
				// x+0,z+1 side: traverse x 0 -> 15 when z is 15
				// x+0,z-1 side: traverse x 0 -> 15 when z is 0
				// x+1,z+0 side: traverse z 0 -> 15 when x is 15
				// x-1,z+0 side: traverse z 0 -> 15 when x is 0
				for(int i = 0; i < 4; i++) {
					sidePoint = new Point(point.x + sideLUTX[i], point.y + sideLUTZ[i]);
					isClaimed = isChunkClaimed(sidePoint,renderWorld);
					if(!isClaimed || !getChunkTerritory(sidePoint, renderWorld).getKingdom().equals(chunkKingdom)) {
						// This side of the render chunk is a border
						ChunkSnapshot chunkSnap = chunk.getChunkSnapshot(true,false,false);
						y_max = 0;
						for(int x = blockLUTXmin[i]; x <= blockLUTXmax[i]; x++) {
							for(int z = blockLUTZmin[i]; z <= blockLUTZmax[i]; z++) {
								// Determine Y level of border
								y_max = chunkSnap.getHighestBlockYAt(x, z);
								y_max = Math.min(y_max, Y_MAX_LIMIT);
								y_max = Math.max(y_max, Y_MIN_LIMIT);
								// Descend through passable blocks like grass, non-occluding blocks like leaves
								while((chunk.getBlock(x, y_max, z).isPassable() || !chunk.getBlock(x, y_max, z).getType().isOccluding()) && y_max > Y_MIN_LIMIT) {
									y_max--;
								}
								// Ascend through liquids
								while(chunk.getBlock(x, y_max+1, z).isLiquid() && y_max < Y_MAX_LIMIT) {
									y_max++;
								}
								// Increase Y a little when there's snow on the border
								Block renderBlock = chunk.getBlock(x, y_max, z);
								Block aboveBlock = chunk.getBlock(x, y_max+1, z);
								y_mod = Y_MOD;
								if(aboveBlock.getBlockData() instanceof Snow) {
									Snow snowBlock = (Snow)aboveBlock.getBlockData();
									if(snowBlock.getLayers() >= snowBlock.getMinimumLayers()) {
										y_mod = Y_MOD_SNOW;
									}
								}
								// Add border location
								renderLoc = new Location(renderBlock.getWorld(),renderBlock.getLocation().getX()+X_MOD,renderBlock.getLocation().getY()+y_mod,renderBlock.getLocation().getZ()+Z_MOD);
								locationMap.put(renderLoc, renderColor);
							}
						}
					}
				}
			}
		}
		return locationMap;
	}
	
	public HashMap<Location,Color> getPlotBorderLocationMap(ArrayList<Chunk> renderChunks, KonPlayer player) {
		HashMap<Location,Color> locationMap = new HashMap<>();
		final int Y_MIN_LIMIT = player.getBukkitPlayer().getWorld().getMinHeight()+1;
		final int Y_MAX_LIMIT = player.getBukkitPlayer().getWorld().getMaxHeight()-1;
		final double X_MOD = 0.5;
		final double Y_MOD = 1;
		final double Y_MOD_SNOW = 1.1;
		final double Z_MOD = 0.5;
		// Search pattern look-up tables
		final int[] sideLUTX     = {0,  0,  1, -1};
		final int[] sideLUTZ     = {1, -1,  0,  0};
		final int[] blockLUTXmin = {0,  0,  15, 0};
		final int[] blockLUTXmax = {15, 15, 15, 0};
		final int[] blockLUTZmin = {15, 0,  0,  0};
		final int[] blockLUTZmax = {15, 0,  15, 15};
		// Iterative variables
		Point sidePoint;
		boolean isClaimed;
		boolean isPlot;
		int y_max;
		double y_mod;
		KonTerritory territory;
		KonTown town;
		KonPlot chunkPlot;
		Location renderLoc;
		Color renderColor;
		// Evaluate every chunk in the provided list. If it's claimed and a town plot, check each adjacent chunk and determine border locations
		for(Chunk chunk : renderChunks) {
			Point point = Konquest.toPoint(chunk);
			World renderWorld = chunk.getWorld();
			if(isChunkClaimed(point,renderWorld)) {
				territory = getChunkTerritory(point,renderWorld);
				if(territory.getKingdom().equals(player.getKingdom()) && territory instanceof KonTown && ((KonTown)territory).hasPlot(point,renderWorld)) {
					// This render chunk is a friendly town plot
					town = ((KonTown)territory);
					chunkPlot = town.getPlot(point,renderWorld);
					renderColor = Color.ORANGE;
					if(chunkPlot != null && chunkPlot.hasUser(player.getOfflineBukkitPlayer())) {
						renderColor = Color.LIME;
					}
					// Iterate all 4 sides of the chunk
					// x+0,z+1 side: traverse x 0 -> 15 when z is 15
					// x+0,z-1 side: traverse x 0 -> 15 when z is 0
					// x+1,z+0 side: traverse z 0 -> 15 when x is 15
					// x-1,z+0 side: traverse z 0 -> 15 when x is 0
					for(int i = 0; i < 4; i++) {
						sidePoint = new Point(point.x + sideLUTX[i], point.y + sideLUTZ[i]);
						isClaimed = isChunkClaimed(sidePoint,renderWorld);
						isPlot = town.hasPlot(sidePoint,renderWorld);
						if(!isClaimed || !isPlot || (chunkPlot != null && !chunkPlot.equals(town.getPlot(sidePoint, renderWorld)))) {
							// This side of the render chunk is a border
							ChunkSnapshot chunkSnap = chunk.getChunkSnapshot(true,false,false);
							y_max = 0;
							for(int x = blockLUTXmin[i]; x <= blockLUTXmax[i]; x++) {
								for(int z = blockLUTZmin[i]; z <= blockLUTZmax[i]; z++) {
									// Determine Y level of border
									y_max = chunkSnap.getHighestBlockYAt(x, z);
									y_max = Math.min(y_max, Y_MAX_LIMIT);
									y_max = Math.max(y_max, Y_MIN_LIMIT);
									// Descend through passable blocks like grass, non-occluding blocks like leaves
									while((chunk.getBlock(x, y_max, z).isPassable() || !chunk.getBlock(x, y_max, z).getType().isOccluding()) && y_max > Y_MIN_LIMIT) {
										y_max--;
									}
									// Ascend through liquids
									while(chunk.getBlock(x, y_max+1, z).isLiquid() && y_max < Y_MAX_LIMIT) {
										y_max++;
									}
									// Increase Y a little when there's snow on the border
									Block renderBlock = chunk.getBlock(x, y_max, z);
									Block aboveBlock = chunk.getBlock(x, y_max+1, z);
									y_mod = Y_MOD;
									if(aboveBlock.getBlockData() instanceof Snow) {
										Snow snowBlock = (Snow)aboveBlock.getBlockData();
										if(snowBlock.getLayers() >= snowBlock.getMinimumLayers()) {
											y_mod = Y_MOD_SNOW;
										}
									}
									// Add border location
									renderLoc = new Location(renderBlock.getWorld(),renderBlock.getLocation().getX()+X_MOD,renderBlock.getLocation().getY()+y_mod,renderBlock.getLocation().getZ()+Z_MOD);
									locationMap.put(renderLoc, renderColor);
								}
							}
						}
					}
				}
			}
		}
		return locationMap;
	}

	public void updateTownDisplayBars(KonKingdom kingdom) {
		for(KonTown town : kingdom.getCapitalTowns()) {
			town.updateBarPlayers();
		}
	}

	// Find all players near the given center location and update their border particles.
	public void updatePlayerBorderParticles(Location center) {
		if(center == null) return;
		World world = center.getWorld();
		if(world == null) return;
		for(Entity nearbyEntity : world.getNearbyEntities(center, 64, 256, 64,(e) -> e instanceof Player)) {
			try {
				Player player = (Player)nearbyEntity;
				KonPlayer onlinePlayer = konquest.getPlayerManager().getPlayer(player);
				updatePlayerBorderParticles(onlinePlayer);
			} catch(Exception ignored) {}
		}
	}

	// Update border particles for all players in the given kingdom.
	public void updatePlayerBorderParticles(KonKingdom kingdom) {
		for(KonPlayer player : konquest.getPlayerManager().getPlayersInKingdom(kingdom)) {
			updatePlayerBorderParticles(player);
		}
	}

	// Update border particles for a single player at their current location.
	public void updatePlayerBorderParticles(KonPlayer player) {
		updatePlayerBorderParticles(player, player.getBukkitPlayer().getLocation());
	}
	
	public void updatePlayerBorderParticles(KonPlayer player, Location loc) {
    	if(player != null && player.isBorderDisplay()) {
    		// Border particle update
			ArrayList<Chunk> nearbyChunks = konquest.getAreaChunks(loc, 2);
			boolean isTerritoryNearby = false;
			for(Chunk chunk : nearbyChunks) {
				if(isChunkClaimed(Konquest.toPoint(chunk),chunk.getWorld())) {
					isTerritoryNearby = true;
					break;
				}
			}
			if(isTerritoryNearby) {
				// Player is nearby a territory, render border particles
				Timer borderTimer = player.getBorderUpdateLoopTimer();
				borderTimer.stopTimer();
	    		borderTimer.setTime(1);
				HashMap<Location,Color> borderTerritoryMap = getBorderLocationMap(nearbyChunks, player);
	    		player.removeAllBorders();
	    		player.addTerritoryBorders(borderTerritoryMap);
	    		HashMap<Location,Color> plotBorderTerritoryMap = getPlotBorderLocationMap(nearbyChunks, player);
	    		player.removeAllPlotBorders();
	    		player.addTerritoryPlotBorders(plotBorderTerritoryMap);
	        	borderTimer.startLoopTimer(10);
			} else {
				// Player is not nearby a territory, stop rendering
				stopPlayerBorderParticles(player);
			}
    	}
    }
	
	public void stopPlayerBorderParticles(KonPlayer player) {
		Timer borderTimer = player.getBorderUpdateLoopTimer();
		if(borderTimer.isRunning()) {
			borderTimer.stopTimer();
		}
		player.removeAllBorders();
		player.removeAllPlotBorders();
	}
	
	/*
	 * Map methods
	 */
	
	public void printPlayerMap(KonPlayer player, int mapSize) {
		printPlayerMap(player, mapSize, player.getBukkitPlayer().getLocation());
	}
	
	public void printPlayerMap(KonPlayer player, int mapSize, Location center) {
		Player bukkitPlayer = player.getBukkitPlayer();
		// Generate Map
    	Point originPoint = Konquest.toPoint(center);
    	String mapWildSymbol = "-";
    	String mapTownSymbol = "+";
    	String mapCampSymbol = "=";
    	String mapRuinSymbol = "%";
    	String mapSanctuarySymbol = "$";
    	String mapCapitalSymbol = "#";
    	String[][] map = new String[mapSize][mapSize];
    	// Determine player's direction
    	BlockFace playerFace = bukkitPlayer.getFacing();
    	String mapPlayer = "!";
		String mapSymbolColor = ""+ChatColor.WHITE;
    	String playerColor = ""+ChatColor.WHITE;
		// Note: Unicode characters do not render correctly in game, must use escape sequence code.
    	if(playerFace.equals(BlockFace.NORTH)) {
    		mapPlayer = "\u25B2";// "^"
    	} else if(playerFace.equals(BlockFace.EAST)) {
    		mapPlayer = "\u25B6";// ">"
    	} else if(playerFace.equals(BlockFace.SOUTH)) {
    		mapPlayer = "\u25BC";// "v"
    	} else if(playerFace.equals(BlockFace.WEST)) {
    		mapPlayer = "\u25C0";// "<"
    	}
    	// Determine settlement status and proximity
    	KonTerritory closestTerritory = null;
    	boolean isLocValidSettle = true;
		int minDistance = Integer.MAX_VALUE;
		int proximity = Integer.MAX_VALUE;
		int searchDist;
		int min_distance_sanc = konquest.getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_SANCTUARY.getPath());
		int min_distance_town = konquest.getCore().getInt(CorePath.TOWNS_MIN_DISTANCE_TOWN.getPath());
		if(!konquest.isWorldValid(center.getWorld())) {
			isLocValidSettle = false;
		}
		for(KonKingdom kingdom : konquest.getKingdomManager().getKingdoms()) {
			searchDist = Konquest.chunkDistance(center, kingdom.getCapital().getCenterLoc());
			if(searchDist != -1) {
				if(searchDist < minDistance) {
					minDistance = searchDist;
					proximity = searchDist;
					closestTerritory = kingdom.getCapital();
				}
				if(searchDist < min_distance_town) {
					isLocValidSettle = false;
				}
			}
			for(KonTown town : kingdom.getTowns()) {
				searchDist = Konquest.chunkDistance(center, town.getCenterLoc());
				if(searchDist != -1) {
					if(searchDist < minDistance) {
						minDistance = searchDist;
						proximity = searchDist;
						closestTerritory = town;
					}
					if(searchDist < min_distance_town) {
						isLocValidSettle = false;
					}
				}
			}
		}
		for(KonRuin ruin : konquest.getRuinManager().getRuins()) {
			searchDist = Konquest.chunkDistance(center, ruin.getCenterLoc());
			if(searchDist != -1) {
				if(searchDist < minDistance) {
					minDistance = searchDist;
					proximity = searchDist;
					closestTerritory = ruin;
				}
				if(searchDist < min_distance_town) {
					isLocValidSettle = false;
				}
			}
		}
		for(KonSanctuary sanctuary : konquest.getSanctuaryManager().getSanctuaries()) {
			searchDist = Konquest.chunkDistance(center, sanctuary.getCenterLoc());
			if(searchDist != -1) {
				if(searchDist < minDistance) {
					minDistance = searchDist;
					proximity = searchDist;
					closestTerritory = sanctuary;
				}
				if(searchDist < min_distance_sanc) {
					isLocValidSettle = false;
				}
			}
		}
		for(KonCamp camp : konquest.getCampManager().getCamps()) {
			searchDist = Konquest.chunkDistance(center, camp.getCenterLoc());
			if(searchDist != -1) {
				if(searchDist < minDistance) {
					proximity = searchDist;
					closestTerritory = camp;
				}
			}
		}
		// Verify max distance
		int max_distance_all = konquest.getCore().getInt(CorePath.TOWNS_MAX_DISTANCE_ALL.getPath());
		if(max_distance_all > 0 && closestTerritory != null && minDistance > max_distance_all) {
			isLocValidSettle = false;
		}
		// Verify no overlapping init chunks
		int radius = konquest.getCore().getInt(CorePath.TOWNS_INIT_RADIUS.getPath());
		for(Point point : konquest.getAreaPoints(center, radius)) {
			if(isChunkClaimed(point,center.getWorld())) {
				isLocValidSettle = false;
			}
		}
    	String settleTip = MessagePath.MENU_MAP_SETTLE_HINT.getMessage();
    	if(isLocValidSettle) {
    		settleTip = ChatColor.GOLD+settleTip;
    	} else {
    		settleTip = ChatColor.STRIKETHROUGH+settleTip;
    		settleTip = ChatColor.GRAY+settleTip;
    	}
    	// Evaluate surrounding chunks
    	for(Point mapPoint: konquest.getAreaPoints(center, ((mapSize-1)/2)+1)) {
    		int diffOriginX = (int)(mapPoint.getX() - originPoint.getX());
    		int diffOriginY = (int)(mapPoint.getY() - originPoint.getY());
    		int mapY = (mapSize-1)/2 - diffOriginX; // Swap X & Y to translate map to be oriented up as north
    		int mapX = (mapSize-1)/2 - diffOriginY;
    		if(mapX < 0 || mapX >= mapSize) {
    			ChatUtil.printDebug("Internal Error: Map X was "+mapX);
    			return;
    		}
    		if(mapY < 0 || mapY >= mapSize) {
    			ChatUtil.printDebug("Internal Error: Map Y was "+mapY);
    			return;
    		}
    		if(isChunkClaimed(mapPoint,center.getWorld())) {
    			KonTerritory territory = getChunkTerritory(mapPoint,center.getWorld());
				assert territory != null;
				mapSymbolColor = konquest.getDisplayPrimaryColor(player,territory);
				playerColor = konquest.getDisplaySecondaryColor(player,territory);
    			switch(territory.getTerritoryType()) {
        		case WILD:
        			map[mapX][mapY] = ChatColor.WHITE+mapWildSymbol;
        			break;
        		case CAPITAL:
        			map[mapX][mapY] = mapSymbolColor+mapCapitalSymbol;
        			break;
        		case TOWN:
        			map[mapX][mapY] = mapSymbolColor+mapTownSymbol;
        			break;
        		case CAMP:
        			map[mapX][mapY] = mapSymbolColor+mapCampSymbol;
        			break;
        		case RUIN:
        			map[mapX][mapY] = mapSymbolColor+mapRuinSymbol;
        			break;
        		case SANCTUARY:
        			map[mapX][mapY] = mapSymbolColor+mapSanctuarySymbol;
        			break;
        		default:
        			map[mapX][mapY] = ChatColor.WHITE+mapWildSymbol;
        			break;
        		}
    		} else {
    			map[mapX][mapY] = ChatColor.WHITE+mapWildSymbol;
    			playerColor = ""+ChatColor.GRAY;
    		}
    		// Override origin symbol with player
    		if(mapX == (mapSize-1)/2 && mapY == (mapSize-1)/2) {
    			map[mapX][mapY] = playerColor+mapPlayer;
    		}
    	}
    	// Determine distance to the closest territory
    	String closestTerritoryColor = ""+ChatColor.WHITE;
    	int distance = 0;
    	if(closestTerritory != null) {
    		distance = proximity;
			closestTerritoryColor = konquest.getDisplayPrimaryColor(player,closestTerritory);
    	}
    	String distStr;
    	int maxDist = 99;
    	if(max_distance_all > 0) {
    		maxDist = max_distance_all;
    	}
    	if(distance > maxDist) {
    		distStr = ""+maxDist+"+";
    		closestTerritoryColor = ""+ChatColor.GRAY;
    	} else {
    		distStr = ""+distance;
    	}
    	// Display map
    	String header = ChatColor.GOLD+MessagePath.MENU_MAP_CENTER.getMessage()+": "+(int)originPoint.getX()+","+(int)originPoint.getY();
    	ChatUtil.sendNotice(bukkitPlayer, header);
    	String[] compassRose = {"   N  ",
    	                        " W * E",
    	                        "   S  "};
    	for(int i = mapSize-1; i >= 0; i--) {
    		StringBuilder mapLine = new StringBuilder(ChatColor.GOLD + "|| ");
    		for(int j = mapSize-1; j >= 0; j--) {
    			mapLine.append(map[i][j]).append(" ");
    		}
    		if(i <= mapSize-1 && i >= mapSize-3) {
    			mapLine.append(ChatColor.GOLD).append(compassRose[mapSize - 1 - i]);
    		}
    		if(i == mapSize-5) {
    			mapLine.append(" ").append(settleTip);
    		}
    		if(i == mapSize-7) {
    			mapLine.append(" ").append(closestTerritoryColor).append(MessagePath.MENU_MAP_PROXIMITY.getMessage()).append(": ").append(distStr);
    		}
    		ChatUtil.sendMessage(bukkitPlayer, mapLine.toString());
    	}
	}

	/**
	 * Protection Methods
	 */

	public boolean canPlayerBreakBlocks(Player player, Location loc) {

		return true;
	}

	public boolean canPlayerPlaceBlocks(Player player, Location loc) {

		return true;
	}
	
}
