package com.github.rumsfield.konquest.utility;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.*;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Snow;
import org.bukkit.potion.PotionEffect;

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
                    // Get block states
                    BlockState templateState = templateBlock.getState();
                    BlockState monumentState = monumentBlock.getState();
                    // Set specific state info
                    if(templateState instanceof Sign && monumentState instanceof Sign) {
                        Sign templateStateSign = (Sign)templateState;
                        Sign monumentStateSign = (Sign)monumentState;
                        monumentStateSign.setEditable(templateStateSign.isEditable());
                        monumentStateSign.setGlowingText(templateStateSign.isGlowingText());
                        monumentStateSign.setColor(templateStateSign.getColor());
                        String [] lines = templateStateSign.getLines();
                        for(int i = 0; i < lines.length; i++) {
                            try {
                                monumentStateSign.setLine(i, lines[i]);
                            } catch(Exception ignored) {}
                        }
                    } else if(templateState instanceof Beacon && monumentState instanceof Beacon) {
                        Beacon templateStateBeacon = (Beacon)templateState;
                        Beacon monumentStateBeacon = (Beacon)monumentState;
                        PotionEffect primaryEffect = templateStateBeacon.getPrimaryEffect();
                        if(primaryEffect != null) {
                            monumentStateBeacon.setPrimaryEffect(primaryEffect.getType());
                        }
                        PotionEffect secondaryEffect = templateStateBeacon.getSecondaryEffect();
                        if(secondaryEffect != null) {
                            monumentStateBeacon.setSecondaryEffect(secondaryEffect.getType());
                        }
                    } else if(templateState instanceof Banner && monumentState instanceof Banner) {
                        Banner templateStateBanner = (Banner)templateState;
                        Banner monumentStateBanner = (Banner)monumentState;
                        monumentStateBanner.setBaseColor(templateStateBanner.getBaseColor());
                        monumentStateBanner.setPatterns(templateStateBanner.getPatterns());
                    }
                    // Update local block state
                    monumentState.update(true,false);
                }
                //Remove snow
                if(monumentBlock.getBlockData() instanceof Snow) {
                	monumentBlock.setType(Material.AIR);
                }
            }
        }
    }

}
