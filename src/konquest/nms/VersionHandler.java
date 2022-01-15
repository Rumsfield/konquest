package konquest.nms;

import org.bukkit.inventory.MerchantInventory;

public interface VersionHandler {

	public void applyTradeDiscount(double discountPercent, boolean isStack, MerchantInventory merchantInventory);
	
}
