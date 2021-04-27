package konquest.command.admin;

import konquest.Konquest;
import konquest.command.CommandBase;
import konquest.model.KonPlayer;
import konquest.utility.ChatUtil;
import konquest.utility.MessageStatic;

import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.libs.org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;


public class MakeKingdomAdminCommand extends CommandBase {
	
	public MakeKingdomAdminCommand(Konquest konquest, CommandSender sender, String[] args) {
        super(konquest, sender, args);
    }

    public void execute() {
        // k admin makekingdom kingdom1
    	if (getArgs().length != 3) {
            ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_PARAMETERS.toString());
            return;
        } else {
        	Player bukkitPlayer = (Player) getSender();
        	World bukkitWorld = bukkitPlayer.getWorld();

        	if(!bukkitWorld.getName().equals(getKonquest().getWorldName())) {
        		ChatUtil.sendError((Player) getSender(), MessageStatic.INVALID_WORLD.toString());
                return;
        	}
        	
        	Location playerLoc = bukkitPlayer.getLocation();
        	String kingdomName = getArgs()[2];
        	if(!StringUtils.isAlphanumeric(kingdomName)) {
        		ChatUtil.sendError((Player) getSender(), "Kingdom name must only contain letters and/or numbers");
                return;
        	}
        	boolean pass = getKonquest().getKingdomManager().addKingdom(playerLoc, kingdomName);
        	if(!pass) {
        		ChatUtil.sendError((Player) getSender(), MessageStatic.BAD_NAME.toString());
                return;
        	} else {
        		ChatUtil.sendNotice((Player) getSender(), "Successfully created new Kingdom Capital for "+kingdomName+".");
        		ChatUtil.sendNotice((Player) getSender(), "Claim additional land with \"/k admin claim\".");
        		ChatUtil.sendNotice((Player) getSender(), "Next, set up the Kingdom Monument with \"/k admin monument "+kingdomName+" create\".");
        		// Render border particles
        		KonPlayer player = getKonquest().getPlayerManager().getPlayer(bukkitPlayer);
        		getKonquest().getKingdomManager().updatePlayerBorderParticles(player, playerLoc);
        	}
        }
    }
    
    @Override
	public List<String> tabComplete() {
		// No arguments to complete
		return Collections.emptyList();
	}
}
