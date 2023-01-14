package com.github.rumsfield.konquest.utility;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Snow;

public class BlockPaster {
	private final Chunk pasteChunk;
	private final World templateWorld;
    private int y;
    private final int y_offset;
    private final int bottomBlockY;
    private final int topBlockX;
    private final int topBlockZ;
    private final int bottomBlockX;
    private final int bottomBlockZ;
    
    public BlockPaster(Chunk pasteChunk, World templateWorld, int y, int y_offset, int bottomBlockY, int topBlockX, int topBlockZ, int bottomBlockX, int bottomBlockZ) {

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
                //Remove snow
                if(monumentBlock.getBlockData() instanceof Snow) {
                	monumentBlock.setType(Material.AIR);
                }
            }
        }
    }

}
