package konquest.nms;

import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.entity.Villager;

import konquest.utility.ChatUtil;

import net.minecraft.server.v1_16_R3.Entity;
import net.minecraft.server.v1_16_R3.NBTTagCompound;
import net.minecraft.server.v1_16_R3.NBTTagList;

public class Handler_1_16_R3 implements VersionHandler {

public Handler_1_16_R3() {}
	
	@Override
	public void applyTradeDiscount(double discountPercent, boolean isStack, Villager villager) {
		
		Entity targetVillager = ((CraftEntity) villager).getHandle();
		NBTTagCompound tag = targetVillager.save(new NBTTagCompound());
		NBTTagList recipeData = (NBTTagList) tag.getCompound("Offers").get("Recipes");
		
		for (int i = 0; i < recipeData.size(); i++) {
			NBTTagCompound recipeTag = recipeData.getCompound(i);
            int currentDiscount = recipeTag.getInt("specialPrice");
            int currentAmount = recipeTag.getCompound("buy").getInt("Count");
            int applyDiscount = (int)(currentAmount*discountPercent*-1);
            if(isStack) {
            	applyDiscount += currentDiscount;
            }
            recipeTag.setInt("specialPrice", applyDiscount);
            ChatUtil.printDebug("  Applied 1.16.x special price "+applyDiscount);
        }
		targetVillager.load(tag);
	}
}
