package konquest.nms;

import org.bukkit.entity.Villager;

public interface VersionHandler {

	public void applyTradeDiscount(double discountPercent, boolean isStack, Villager villager);
	
}
