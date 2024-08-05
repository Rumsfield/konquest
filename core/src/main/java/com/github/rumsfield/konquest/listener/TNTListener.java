package com.github.rumsfield.konquest.listener;

import com.github.rumsfield.konquest.KonquestPlugin;
import com.github.rumsfield.konquest.utility.ChatUtil;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.AnaloguePowerable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Fire;
import org.bukkit.block.data.type.TNT;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;

import java.util.HashMap;
import java.util.UUID;

public class TNTListener implements Listener {

    private final String metaOwnerId = "konquest-tnt-owner";
    private final KonquestPlugin plugin;
    private final HashMap<Player,Location> lastPowered;

    public TNTListener(KonquestPlugin plugin) {
        this.plugin = plugin;
        this.lastPowered = new HashMap<>();
    }

    /* TNT Debug */
    /*
     * The Plan:
     * Monitor all the ways that players can possibly ignite TNT.
     * Keep a record of all player's last ignition locations, similar to last block placement.
     * When a TNT block explodes, look for the closest ignition location within a bound radius to make a best guess
     * at which player ignited it.
     * Use the found player for relationship checks.
     *
     * TNT ignitions by players:
     * - Flint & steel - player interact
     * - Fire charge - player interact
     * - Redstone power - ???
     * - Shot with arrow on fire (flame enchantment, through lava)
     * - Other explosions (TNT, creeper, bed, respawn anchor, end crystal)
     * - Contact with fire or lava
     * - Dispenser with TNT
     * - Dispenser with flint & steel
     * - Dispenser with fire charge
     *
     * Owner Tracking
     * - Assign block metadata of player UUID when they do things that can cause TNT to ignite
     * - When a TNT entity spawns, search surrounding blocks & entities for metadata owner
     *
     * Protections (No TNT explosions when...)
     * - owner's kingdom is offline protected
     * - owner is peace, trade, or ally with target town (friendly players can use TNT in their own towns)
     *
     *
     */

    @EventHandler(priority = EventPriority.NORMAL)
    public void onTNTPrimed(ExplosionPrimeEvent event) {
        if (event.getEntity() instanceof TNTPrimed) {
            ChatUtil.printDebug("TNT Entity exploded!");
            if (event.getEntity().hasMetadata(metaOwnerId)) {
                UUID ownerId = UUID.fromString(event.getEntity().getMetadata(metaOwnerId).get(0).asString());
                Player owner = Bukkit.getPlayer(ownerId);
                String ownerName = owner == null ? "unknown" : owner.getName();
                ChatUtil.printDebug("  Found owner! "+ownerName);
            } else {
                ChatUtil.printDebug("  Unknown owner");
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onTNTSpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof TNTPrimed) {
            Block spawnBlock = event.getLocation().getBlock();
            TNTPrimed tnt = (TNTPrimed)event.getEntity();
            String sourceName = tnt.getSource() == null ? "null" : tnt.getSource().getName();
            ChatUtil.printDebug("TNT Entity spawned on block "+spawnBlock.getType()+" from source "+sourceName);
            World spawnWorld = event.getLocation().getWorld();
            assert spawnWorld != null;
            boolean hasOwner = false;
            Player nearestPlayer = null;
            double nearestDistance = 0;
            // Search surrounding blocks for metadata
            int x = spawnBlock.getX()-2;
            int y = spawnBlock.getY()-2;
            int z = spawnBlock.getZ()-2;
            for (int ix = 0; ix < 5; ix++) {
                for (int iy = 0; iy < 5; iy++) {
                    for (int iz = 0; iz < 5; iz++) {
                        BlockState checkState = spawnWorld.getBlockAt(new Location(spawnWorld,x+ix,y+iy,z+iz)).getState();
                        if (checkState.hasMetadata(metaOwnerId)) {
                            for (MetadataValue meta : checkState.getMetadata(metaOwnerId)) {
                                event.getEntity().setMetadata(metaOwnerId,meta);
                            }
                            checkState.removeMetadata(metaOwnerId,plugin);
                            ChatUtil.printDebug("  Found nearby block with owner " + checkState.getType());
                            hasOwner = true;
                            break;
                        }
                    }
                }
            }
            // Search surrounding entities for metadata, and nearest player
            if (!hasOwner) {
                for (Entity checkEntity : spawnWorld.getNearbyEntities(event.getLocation(), 32, 32, 32)) {
                    if (checkEntity.hasMetadata(metaOwnerId)) {
                        for (MetadataValue meta : checkEntity.getMetadata(metaOwnerId)) {
                            event.getEntity().setMetadata(metaOwnerId, meta);
                        }
                        ChatUtil.printDebug("  Found nearby entity with owner " + checkEntity.getName());
                        hasOwner = true;
                        break;
                    } else if (checkEntity instanceof Player) {
                        // Find nearest player
                        double checkDistance = event.getLocation().distance(checkEntity.getLocation());
                        if (nearestPlayer == null || checkDistance < nearestDistance) {
                            nearestPlayer = (Player)checkEntity;
                            nearestDistance = checkDistance;
                        }
                    }
                }
            }
            // Search for nearby powered activation
            if (!hasOwner) {
                Player nearestPowerPlayer = null;
                double nearestPowerDistance = 0;
                for (Player checkPlayer : lastPowered.keySet()) {
                    Location checkLocation = lastPowered.get(checkPlayer);
                    if (event.getLocation().getWorld().equals(checkLocation.getWorld())) {
                        double checkDistance = event.getLocation().distance(checkLocation);
                        if (checkDistance < 256) {
                            // Distance must be within 256 blocks (16 chunks)
                            if (nearestPowerPlayer == null || checkDistance < nearestPowerDistance) {
                                nearestPowerPlayer = checkPlayer;
                                nearestPowerDistance = checkDistance;
                            }
                        }
                    }
                }
                if (nearestPowerPlayer != null) {
                    ChatUtil.printDebug("Found nearby power source by "+nearestPowerPlayer.getName()+", "+nearestPowerDistance);
                    hasOwner = true;
                    updateMetaOwner(event.getEntity(),nearestPowerPlayer);
                }
            }
            // Last resort, check for nearest player
            if (!hasOwner && nearestPlayer != null) {
                ChatUtil.printDebug("  Defaulting to nearby player "+nearestPlayer.getName());
                hasOwner = true;
                updateMetaOwner(event.getEntity(),nearestPlayer);
            }
            if (hasOwner) {
                ChatUtil.printDebug("  TNT has owner!");
            }
        }
    }

    /* Player Tracking */

    /*
     * - Track all arrow entities shot by players
     * - Track all fire placed by players
     * - Track redstone torches and blocks placed by players
     *
     * Record all redstone activations by players & location:
     * - Switch toggles
     * - Button presses
     * - Tripline
     * - Pressure plates
     * - Placing redstone torches, blocks
     *
     * What about clocks or periodic activations that trigger dispensers?
     * - Maybe best to add persistent metadata to all dispensers for who owns it.
     */

    /**
     * Track when players light TNT by clicking with Flint & Steel or Fire Charge
     * @param event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTNTInteractPlayer(PlayerInteractEvent event) {
        Block clickBlock = event.getClickedBlock();
        if (event.hasBlock() && clickBlock != null) {
            if (clickBlock.getBlockData() instanceof TNT && event.hasItem() && event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                ChatUtil.printDebug("TNT block interaction by player!");
                updateMetaOwner(clickBlock.getState(),event.getPlayer());
            } else {
                // Record all powered interactions
                if (clickBlock.getBlockData() instanceof AnaloguePowerable || clickBlock.getBlockData() instanceof Powerable) {
                    updateLastPowered(event.getPlayer(),clickBlock.getLocation());
                }
            }
        }
    }

    /**
     * Track when players shoot arrows or explosives
     * @param event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTNTPlayerShoot(ProjectileLaunchEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof AbstractArrow || event.getEntity() instanceof Explosive)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;
        updateMetaOwner(event.getEntity(),((Player)event.getEntity().getShooter()));
    }

    /**
     * Track when players place redstone blocks and torches, and fire
     * @param event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTNTPlayerPlaceRedstone(BlockPlaceEvent event) {
        if (event.isCancelled()) return;
        Material placeType = event.getBlockPlaced().getType();
        if (!(placeType.equals(Material.REDSTONE_BLOCK) ||
                placeType.equals(Material.REDSTONE_TORCH) ||
                placeType.equals(Material.REDSTONE_WALL_TORCH) ||
                placeType.equals(Material.FIRE))) return;
        updateMetaOwner(event.getBlockPlaced().getState(),event.getPlayer());
        // Redstone power sources
        if (placeType.equals(Material.REDSTONE_BLOCK) ||
                placeType.equals(Material.REDSTONE_TORCH) ||
                placeType.equals(Material.REDSTONE_WALL_TORCH)) {
            updateLastPowered(event.getPlayer(),event.getBlockPlaced().getLocation());
        }
    }

    /**
     * Track when players break redstone blocks and torches
     * @param event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTNTPlayerBreakRedstone(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        Material breakType = event.getBlock().getType();
        // Redstone power sources
        if (breakType.equals(Material.REDSTONE_BLOCK) ||
                breakType.equals(Material.REDSTONE_TORCH) ||
                breakType.equals(Material.REDSTONE_WALL_TORCH)) {
            updateLastPowered(event.getPlayer(),event.getBlock().getLocation());
        }
    }

    /**
     * Track when fire spreads
     * @param event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onTNTFireSpread(BlockSpreadEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getBlock().getBlockData() instanceof Fire)) return;
        if (!(event.getSource().getBlockData() instanceof Fire)) return;
        if (event.getSource().getState().hasMetadata(metaOwnerId)) {
            for (MetadataValue meta : event.getSource().getState().getMetadata(metaOwnerId)) {
                event.getBlock().getState().setMetadata(metaOwnerId, meta);
                event.getBlock().getState().update();
            }
        }
    }

    /* Helpers */

    private void updateMetaOwner(Metadatable meta, Player owner) {
        meta.setMetadata(metaOwnerId,new FixedMetadataValue(plugin,owner.getUniqueId().toString()));
        if (meta instanceof BlockState) {
            if (!((BlockState)meta).update(true)) {
                ChatUtil.printDebug("Failed to add metadata to block state for TNT owner.");
            }
        }
    }

    private void updateLastPowered(Player player, Location loc) {
        lastPowered.put(player,loc);
        // 30 second timeout
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
                () -> lastPowered.remove(player), 20*30);
    }

}
