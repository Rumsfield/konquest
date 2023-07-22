package com.github.rumsfield.konquest.shop;

import com.Acrobot.ChestShop.Signs.ChestShopSign;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import java.util.ArrayList;
import java.util.List;

public class ChestShopFinder {

    public ChestShopFinder() {}

    public static List<Sign> findShopSigns(World world, int x, int z) {
        List<Sign> result = new ArrayList<>();

        Chunk chunk = world.getChunkAt(x, z);
        for(BlockState bs : chunk.getTileEntities()) {
            if(bs instanceof Sign) {
                Sign sign = (Sign)bs;
                if (ChestShopSign.isValid(sign)) {
                    result.add(sign);
                }
            }
        }

        return result;
    }

}
