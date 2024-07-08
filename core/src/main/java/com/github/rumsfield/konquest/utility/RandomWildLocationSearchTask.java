package com.github.rumsfield.konquest.utility;

import com.github.rumsfield.konquest.Konquest;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.function.Consumer;
import java.util.concurrent.ThreadLocalRandom;

public class RandomWildLocationSearchTask extends BukkitRunnable {

    private final Konquest konquest;
    private final World world;
    private final Consumer<Location> locationFoundCallback;
    private int triesCount;

    private int radius;
    private final int offsetX;
    private final int offsetZ;

    private int numClaimed;
    private int numWater;

    public RandomWildLocationSearchTask(Konquest konquest, World world, Consumer<Location> locationFoundCallback) {
        this.konquest = konquest;
        this.world = world;
        this.locationFoundCallback = locationFoundCallback;
        this.triesCount = 0;

        this.radius = konquest.getCore().getInt(CorePath.TRAVEL_WILD_RADIUS.getPath(),500);
        radius = Math.max(radius,16); // Lower bound
        this.offsetX = konquest.getCore().getInt(CorePath.TRAVEL_WILD_CENTER_X.getPath(),0);
        this.offsetZ = konquest.getCore().getInt(CorePath.TRAVEL_WILD_CENTER_Z.getPath(),0);

        // Metrics for criteria checking
        this.numClaimed = 0;
        this.numWater = 0;
    }

    @Override
    public void run() {
        int MAX_TRIES = 60;
        if(triesCount > MAX_TRIES) {
            ChatUtil.printDebug("Failed to get a random wilderness location. Claimed attempts: " + numClaimed + "; Water attempts: " + numWater);
            locationFoundCallback.accept(null);
            cancel();
            return;
        }

        int randomNumX = ThreadLocalRandom.current().nextInt(-1*(radius), (radius) + 1) + offsetX;
        int randomNumZ = ThreadLocalRandom.current().nextInt(-1*(radius), (radius) + 1) + offsetZ;
        Block randomBlock = world.getHighestBlockAt(randomNumX,randomNumZ);
        int randomNumY = randomBlock.getY() + 2;
        Location wildLoc = new Location(world, randomNumX, randomNumY, randomNumZ);
        ChatUtil.printDebug("Searching new location "+wildLoc.getX()+","+wildLoc.getY()+","+wildLoc.getZ()+"...");

        // Check for valid location criteria
        if(konquest.getTerritoryManager().isChunkClaimed(wildLoc)) {
            // This location is claimed
            numClaimed++;
            triesCount++;
            return;
        }
        if(!randomBlock.getType().isSolid()) {
            // This location is liquid
            numWater++;
            triesCount++;
            return;
        }

        // Passed all checks
        ChatUtil.printDebug("Got wilderness location "+wildLoc.getX()+","+wildLoc.getY()+","+wildLoc.getZ()+". Claimed attempts: "+numClaimed+"; Water attempts: "+numWater);
        locationFoundCallback.accept(wildLoc);
        cancel();
    }
}
