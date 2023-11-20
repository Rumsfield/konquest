package com.github.rumsfield.konquest.hook;

import com.github.rumsfield.konquest.utility.ChatUtil;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.awt.*;

/**
 * Executor class with only static methods using WorldGuard API.
 */
public class WorldGuardExec {

    private static StateFlag ARENA;
    private static StateFlag CLAIM;
    private static StateFlag UNCLAIM;
    private static StateFlag TRAVEL_ENTER;
    private static StateFlag TRAVEL_EXIT;

    private static boolean isAvailable = false; // Is the WorldGuard plugin loaded and all flags registered?

    /**
     * Load custom WorldGuard flags into the registry.
     * @return True when flags were successfully registered, else false (like when WorldGuard is not present).
     */
    public static boolean load() {
        isAvailable = false;
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        String errorMessageFormat = "Konquest failed to register a custom flag with WorldGuard, %s, because another plugin has already registered it.";

        /* Arena Flag */
        String flagNameArena = "konquest-arena";
        try {
            StateFlag flag = new StateFlag(flagNameArena, false);
            registry.register(flag);
            ARENA = flag;
        } catch (FlagConflictException e) {
            // some other plugin registered a flag by the same name already.
            ChatUtil.printConsoleError(String.format(errorMessageFormat, flagNameArena));
            return false;
        }

        /* Claim Flag */
        String flagNameClaim = "konquest-claim";
        try {
            StateFlag flag = new StateFlag(flagNameClaim, true);
            registry.register(flag);
            CLAIM = flag;
        } catch (FlagConflictException e) {
            // some other plugin registered a flag by the same name already.
            ChatUtil.printConsoleError(String.format(errorMessageFormat, flagNameClaim));
            return false;
        }

        /* Unclaim Flag */
        String flagNameUnclaim = "konquest-unclaim";
        try {
            StateFlag flag = new StateFlag(flagNameUnclaim, true);
            registry.register(flag);
            UNCLAIM = flag;
        } catch (FlagConflictException e) {
            // some other plugin registered a flag by the same name already.
            ChatUtil.printConsoleError(String.format(errorMessageFormat, flagNameUnclaim));
            return false;
        }

        /* Travel Enter Flag */
        String flagNameTravelEnter = "konquest-travel-enter";
        try {
            StateFlag flag = new StateFlag(flagNameTravelEnter, true);
            registry.register(flag);
            TRAVEL_ENTER = flag;
        } catch (FlagConflictException e) {
            // some other plugin registered a flag by the same name already.
            ChatUtil.printConsoleError(String.format(errorMessageFormat, flagNameTravelEnter));
            return false;
        }

        /* Travel Exit Flag */
        String flagNameTravelExit = "konquest-travel-exit";
        try {
            StateFlag flag = new StateFlag(flagNameTravelExit, true);
            registry.register(flag);
            TRAVEL_EXIT = flag;
        } catch (FlagConflictException e) {
            // some other plugin registered a flag by the same name already.
            ChatUtil.printConsoleError(String.format(errorMessageFormat, flagNameTravelExit));
            return false;
        }

        isAvailable = true;
        return true;
    }

    static boolean isChunkClaimAllowed(World world, Point point, Player player) {
        return isChunkFlagAllowed(CLAIM, world, point, player);
    }

    static boolean isChunkUnclaimAllowed(World world, Point point, Player player) {
        return isChunkFlagAllowed(UNCLAIM, world, point, player);
    }

    private static boolean isChunkFlagAllowed(StateFlag flag, World world, Point point, Player player) {
        if(!isAvailable) {
            // WorldGuard integration did not register custom flags, always allow
            return true;
        }
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(world));
        if(regions == null) {
            // No regions available in this world, always allow
            return true;
        }
        int chunkBlockX = point.x << 4;
        int chunkBlockZ = point.y << 4;
        int chunkMinY = world.getMinHeight();
        int chunkMaxY = world.getMaxHeight();
        BlockVector3 pt1 = BlockVector3.at(chunkBlockX, chunkMinY, chunkBlockZ);
        BlockVector3 pt2 = BlockVector3.at(chunkBlockX + 15, chunkMaxY, chunkBlockZ + 15);
        ProtectedCuboidRegion chunkRegion = new ProtectedCuboidRegion("konquest_chunk_temporary", true, pt1, pt2);
        ApplicableRegionSet overlappingChunkRegions = regions.getApplicableRegions(chunkRegion);
        return overlappingChunkRegions.testState(WorldGuardPlugin.inst().wrapPlayer(player), flag);
    }

    static boolean isLocationTravelEnterAllowed(Location loc, Player player) {
        return isLocationFlagAllowed(TRAVEL_ENTER, loc, player, true);
    }

    static boolean isLocationTravelExitAllowed(Location loc, Player player) {
        return isLocationFlagAllowed(TRAVEL_EXIT, loc, player, true);
    }

    static boolean isLocationArenaAllowed(Location loc, Player player) {
        return isLocationFlagAllowed(ARENA, loc, player, false);
    }

    private static boolean isLocationFlagAllowed(StateFlag flag, Location loc, Player player, boolean defaultResult) {
        if(!isAvailable) {
            // WorldGuard integration did not register custom flags, use default result
            return defaultResult;
        }
        assert loc.getWorld() != null;
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(loc.getWorld()));
        if(regions == null) {
            // No regions available in this world, use default
            return defaultResult;
        }
        BlockVector3 pos = BlockVector3.at(loc.getX(),loc.getY(),loc.getZ());
        ApplicableRegionSet overlappingChunkRegions = regions.getApplicableRegions(pos);
        return overlappingChunkRegions.testState(WorldGuardPlugin.inst().wrapPlayer(player), flag);
    }

}
