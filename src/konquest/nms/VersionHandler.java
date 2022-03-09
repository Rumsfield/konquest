package konquest.nms;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.scoreboard.Team;

public interface VersionHandler {

	public void applyTradeDiscount(double discountPercent, boolean isStack, MerchantInventory merchantInventory);
	
	public void sendPlayerTeamPacket(Player player, List<String> teamNames, Team team);
}
