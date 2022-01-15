package konquest.nms;

import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity;
import org.bukkit.entity.Villager;

import konquest.utility.ChatUtil;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.entity.Entity;

public class Handler_1_17_R1 implements VersionHandler {

	public Handler_1_17_R1() {}
	
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
            targetVillager.load(recipeTag);
            ChatUtil.printDebug("  Applied 1.17.1 special price "+applyDiscount);
        }
	}

}
