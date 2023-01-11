package com.github.rumsfield.konquest.utility;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Snow;

public class BlockPaster {

	//private BukkitScheduler scheduler;
    //private Runnable task;
    //private BukkitRunnable task;
    //private BukkitTask task;
    //private int taskID;
	private Chunk pasteChunk;
	private World templateWorld;
    private int y;
    private int y_offset;
    private int bottomBlockY;
    private int topBlockX;
    private int topBlockZ;
    private int bottomBlockX;
    private int bottomBlockZ;
    
    public BlockPaster(Chunk pasteChunk, World templateWorld, int y, int y_offset, int bottomBlockY, int topBlockX, int topBlockZ, int bottomBlockX, int bottomBlockZ) {
    	//this.taskID = 0;
    	//this.scheduler = Bukkit.getScheduler();
    	this.pasteChunk = pasteChunk;
    	this.templateWorld = templateWorld;
    	this.y = y;
    	this.y_offset = y_offset;
    	this.bottomBlockY = bottomBlockY;
    	this.topBlockX = topBlockX;
    	this.topBlockZ = topBlockZ;
    	this.bottomBlockX = bottomBlockX;
    	this.bottomBlockZ = bottomBlockZ;
    }
    
    
    public void setY(int y) {
    	this.y = y;
    }
    
    public void startPaste() {
    	for (int x = bottomBlockX; x <= topBlockX; x++) {
            for (int z = bottomBlockZ; z <= topBlockZ; z++) {
                Block templateBlock = templateWorld.getBlockAt(x, y, z);
                Block monumentBlock = pasteChunk.getBlock(x-bottomBlockX, y-bottomBlockY+y_offset, z-bottomBlockZ);
                if(!monumentBlock.getBlockData().matches(templateBlock.getBlockData())) {
                	// Set local block to monument template block
                	if(templateBlock.getBlockData() instanceof Bisected) {
                		monumentBlock.setType(templateBlock.getType(),false);
                	} else {
                		monumentBlock.setType(templateBlock.getType());
                	}
                    monumentBlock.setBlockData(templateBlock.getBlockData().clone());
                }
                //ChatUtil.printDebug("Pasting block at "+monumentBlock.getLocation().toString()+" with template from "+templateBlock.getLocation().toString());
                //Remove snow
                if(monumentBlock.getBlockData() instanceof Snow) {
                	monumentBlock.setType(Material.AIR);
                }
            }
        }
    }

    /*public void startPaste() {

    	BukkitRunnable task = new BukkitRunnable() {
            public void run() {
            	//ChatUtil.printDebug("Running task at Y "+y+" from template X "+bottomBlockX+" to "+topBlockX+" and Z "+bottomBlockZ+" to "+topBlockZ);
            	for (int x = bottomBlockX; x <= topBlockX; x++) {
                    for (int z = bottomBlockZ; z <= topBlockZ; z++) {
                        Block templateBlock = Bukkit.getServer().getWorld(Konquest.getInstance().getWorldName()).getBlockAt(x, y, z);
                        Block monumentBlock = Bukkit.getServer().getWorld(Konquest.getInstance().getWorldName()).getChunkAt(centerLoc).getBlock(x-bottomBlockX, y-bottomBlockY+y_offset, z-bottomBlockZ);
                        // Set local block to monument template block
                        monumentBlock.setType(templateBlock.getType());
                        monumentBlock.setBlockData(templateBlock.getBlockData().clone());
                        //ChatUtil.printDebug("Pasting block at "+monumentBlock.getLocation().toString()+" with template from "+templateBlock.getLocation().toString());
                        //Remove snow
                        if(monumentBlock.getBlockData() instanceof Snow) {
                        	monumentBlock.setType(Material.AIR);
                        }
                    }
                }
            	//ChatUtil.printDebug("Finished running task at Y "+y+" from template X "+bottomBlockX+" to "+topBlockX+" and Z "+bottomBlockZ+" to "+topBlockZ);
            	
            }
        };
        //taskID = scheduler.scheduleSyncDelayedTask(Konquest.getInstance().getPlugin(), task, 20);
        //BukkitTask t = task.runTaskLater(Konquest.getInstance().getPlugin(), 20);
        task.runTaskLater(Konquest.getInstance().getPlugin(), 20);
        //this.taskID = t.getTaskId();
        //ChatUtil.printDebug("Started BlockPaster task with y "+y+" and taskID "+t.getTaskId());
    }*/
    
}
