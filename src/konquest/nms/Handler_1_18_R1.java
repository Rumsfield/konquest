package konquest.nms;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

import konquest.utility.ChatUtil;

public class Handler_1_18_R1 implements VersionHandler {

public Handler_1_18_R1() {}
	
	@Override
	public void applyTradeDiscount(double discountPercent, boolean isStack, MerchantInventory merchantInventory) {
		// Get and set special price with API methods
		int amount = 0;
		int discount = 0;
		Merchant tradeHost = merchantInventory.getMerchant();
		List<MerchantRecipe> tradeListDiscounted = new ArrayList<MerchantRecipe>();
		for(MerchantRecipe trade : tradeHost.getRecipes()) {
			//ChatUtil.printDebug("Found trade for "+trade.getResult().getType().toString()+" with price mult "+trade.getPriceMultiplier()+
			//		", special "+trade.getSpecialPrice()+", uses "+trade.getUses()+", max "+trade.getMaxUses());
			List<ItemStack> ingredientList = trade.getIngredients();
			//for(ItemStack ingredient : ingredientList) {
			//	ChatUtil.printDebug("  Has ingredient "+ingredient.getType().toString()+", amount: "+ingredient.getAmount());
			//}
			if(!ingredientList.isEmpty()) {
				amount = ingredientList.get(0).getAmount();
				discount = (int)(amount*discountPercent*-1);
				if(isStack) {
					discount += trade.getSpecialPrice();
				}
				trade.setSpecialPrice(discount);
				ChatUtil.printDebug("  Applied 1.18.x special price "+discount);
			}
			tradeListDiscounted.add(trade);
		}
		tradeHost.setRecipes(tradeListDiscounted);
	}
}
