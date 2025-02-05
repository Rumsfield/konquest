package com.github.rumsfield.konquest.utility;

import com.github.rumsfield.konquest.Konquest;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.concurrent.ThreadLocalRandom;

public class RandomWildLocationSearchTask extends BukkitRunnable {

    private final World world;
    private final Consumer<Location> locationFoundCallback;
    private int triesCount;
    private int numWater;
    private final Iterator<Point> wildIterator;

    // Determine all possible chunk points within the radius boundary
    public RandomWildLocationSearchTask(Konquest konquest, World world, Consumer<Location> locationFoundCallback) {
        this.world = world;
        this.locationFoundCallback = locationFoundCallback;
        this.triesCount = 0;
        this.numWater = 0;
        // Search boundary
        int radius = konquest.getCore().getInt(CorePath.TRAVEL_WILD_RADIUS.getPath(),500);
        radius = Math.max(radius,16); // Lower bound
        int offsetX = konquest.getCore().getInt(CorePath.TRAVEL_WILD_CENTER_X.getPath(),0);
        int offsetZ = konquest.getCore().getInt(CorePath.TRAVEL_WILD_CENTER_Z.getPath(),0);
        // Derived point bounds
        int pMinX = (-1*(radius) + offsetX) / 16;
        int pMaxX = ((radius+1) + offsetX) / 16;
        int pMinZ = (-1*(radius) + offsetZ) / 16;
        int pMaxZ = ((radius+1) + offsetZ) / 16;
        // Populate wild chunks used to search
        ArrayList<Point> searchPoints = new ArrayList<>();
        for (int x = pMinX; x < pMaxX; x++) {
            for (int z = pMinZ; z < pMaxZ; z++) {
                Point searchPoint = new Point(x,z);
                if (!konquest.getTerritoryManager().isChunkClaimed(searchPoint,world)) {
                    searchPoints.add(searchPoint);
                }
            }
        }
        Collections.shuffle(searchPoints);
        wildIterator = searchPoints.iterator();
        ChatUtil.printDebug("Starting wild location search task with "+searchPoints.size()+" wild chunks.");
    }

    /**
     * This task is ran periodically to check for a valid wild location until one is found, or times out.
     * Get the next wild chunk from the randomized list generated in the task constructor.
     * Check for solid blocks at the highest Y elevation.
     * Skip the search if the highest block is liquid.
     */
    @Override
    public void run() {
        ChatUtil.printDebug("Running wild location search, try "+triesCount);
        // Check for timeout
        int MAX_TRIES = 100;
        if(triesCount > MAX_TRIES || !wildIterator.hasNext()) {
            ChatUtil.printDebug("Failed to get a random wilderness location. Water attempts: " + numWater+"; Wild chunks remain: "+wildIterator.hasNext());
            locationFoundCallback.accept(null);
            cancel();
            return;
        }
        // Get the next randomized wild chunk point
        Point wildPoint = wildIterator.next();
        // Choose a random location within the chunk
        int randBlockX = ThreadLocalRandom.current().nextInt(0, 16);
        int randBlockZ = ThreadLocalRandom.current().nextInt(0, 16);
        int locX = wildPoint.x*16 + randBlockX;
        int locZ = wildPoint.y*16 + randBlockZ;
        // Get the highest block at the chosen location
        Block wildBlock = world.getHighestBlockAt(locX, locZ);
        if(!wildBlock.getType().isSolid()) {
            // This location is liquid
            numWater++;
            triesCount++;
            return;
        }
        int locY = wildBlock.getY() + 2;
        Location wildLoc = new Location(world, locX, locY, locZ);
        // Passed all checks
        ChatUtil.printDebug("Got wilderness location "+wildLoc.getX()+","+wildLoc.getY()+","+wildLoc.getZ()+". Water attempts: "+numWater);
        locationFoundCallback.accept(wildLoc);
        cancel();
    }
}
