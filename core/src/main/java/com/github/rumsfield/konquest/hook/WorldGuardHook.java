package com.github.rumsfield.konquest.hook;

import com.github.rumsfield.konquest.Konquest;
import com.github.rumsfield.konquest.utility.CorePath;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class WorldGuardHook implements PluginHook {

    private final Konquest konquest;
    private WorldGuard wgAPI;
    private boolean isEnabled;

    public WorldGuardHook(Konquest konquest) {
        this.konquest = konquest;
        this.wgAPI = null;
        this.isEnabled = false;
    }

    @Override
    public int reload() {
        // Attempt to integrate WorldGuard
        Plugin worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if(worldGuard == null){
            return 1;
        }
        if(!worldGuard.isEnabled()){
            return 2;
        }
        if(!konquest.getCore().getBoolean(CorePath.INTEGRATION_WORLDGUARD.getPath(),false)) {
            return 3;
        }
        wgAPI = WorldGuard.getInstance();
        isEnabled = true;
        return 0;
    }

    @Override
    public void shutdown() {
        // Do Nothing
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public String getPluginName() {
        return "WorldGuard";
    }

    @Nullable
    public WorldGuard getAPI() {
        return wgAPI;
    }

    private ProtectedCuboidRegion toChunkRegion(World world, Point point) {
        int chunkBlockX = point.x << 4;
        int chunkBlockZ = point.y << 4;
        int chunkMinY = world.getMinHeight();
        int chunkMaxY = world.getMaxHeight();
        BlockVector3 pt1 = BlockVector3.at(chunkBlockX, chunkMinY, chunkBlockZ);
        BlockVector3 pt2 = BlockVector3.at(chunkBlockX + 15, chunkMaxY, chunkBlockZ + 15);
        return new ProtectedCuboidRegion("konquest_chunk_temporary", true, pt1, pt2);
    }

    public boolean isFlagAllowed(StateFlag flag, Location loc, Player player) {
        assert loc.getWorld() != null;
        return isFlagAllowed(flag, loc.getWorld(), Konquest.toPoint(loc), player);
    }

    public boolean isFlagAllowed(StateFlag flag, World world, Point point, Player player) {
        if(!isEnabled) {
            // WorldGuard integration is disabled, always allow
            return true;
        }
        if(!WorldGuardRegistry.isAvailable) {
            // WorldGuard integration did not register custom flags, always allow
            return true;
        }
        RegionContainer container = wgAPI.getPlatform().getRegionContainer();
        RegionManager regions = container.get(BukkitAdapter.adapt(world));
        if(regions == null) {
            // No regions available in this world, always allow
            return true;
        }
        ApplicableRegionSet overlappingChunkRegions = regions.getApplicableRegions(toChunkRegion(world, point));
        return overlappingChunkRegions.testState(WorldGuardPlugin.inst().wrapPlayer(player), flag);
    }
}
