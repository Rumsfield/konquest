package konquest.command;

import konquest.Konquest;
import konquest.KonquestPlugin;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessagePath;

import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FavorCommand extends CommandBase {

	public FavorCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }
	
	public void execute() {
		// k favor
    	if (getArgs().length != 1) {
    		ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INVALID_PARAMETERS.getMessage());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	if(!getKonquest().getPlayerManager().isPlayer(bukkitPlayer)) {
    			ChatUtil.printDebug("Failed to find non-existent player");
    			ChatUtil.sendError((Player) getSender(), MessagePath.GENERIC_ERROR_INTERNAL.getMessage());
    			return;
    		}
        	KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);

        	double balance = KonquestPlugin.getBalance(bukkitPlayer);
        	double cost_spy = getKonquest().getConfigManager().getConfig("core").getDouble("core.favor.cost_spy",0.0);
        	double cost_settle = getKonquest().getConfigManager().getConfig("core").getDouble("core.favor.cost_settle",0.0);
        	double cost_settle_incr = getKonquest().getConfigManager().getConfig("core").getDouble("core.favor.cost_settle_increment",0.0);
        	int townCount = getKonquest().getKingdomManager().getPlayerLordships(player);
        	double cost_settle_adj = (((double)townCount)*cost_settle_incr) + cost_settle;
        	double cost_rename = getKonquest().getConfigManager().getConfig("core").getDouble("core.favor.cost_rename",0.0);
        	double cost_claim = getKonquest().getConfigManager().getConfig("core").getDouble("core.favor.cost_claim",0.0);
        	double cost_travel = getKonquest().getConfigManager().getConfig("core").getDouble("core.favor.cost_travel",0.0);
        	String balanceF = String.format("%.2f",balance);
        	ChatUtil.sendNotice(bukkitPlayer, MessagePath.COMMAND_FAVOR_NOTICE_MESSAGE.getMessage(balanceF));
        	ChatUtil.sendMessage(bukkitPlayer, "Spy: "+ChatColor.AQUA+cost_spy);
        	ChatUtil.sendMessage(bukkitPlayer, "Settle: "+ChatColor.AQUA+cost_settle_adj);
        	ChatUtil.sendMessage(bukkitPlayer, "Rename: "+ChatColor.AQUA+cost_rename);
        	ChatUtil.sendMessage(bukkitPlayer, "Claim: "+ChatColor.AQUA+cost_claim);
        	ChatUtil.sendMessage(bukkitPlayer, "Travel: "+ChatColor.AQUA+cost_travel);
        }
	}
	
	@Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
