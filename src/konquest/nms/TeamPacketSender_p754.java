package konquest.nms;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.FieldAccessException;

import konquest.KonquestPlugin;
import konquest.utility.ChatUtil;

/**
 * Support for packets defined in the Minecraft protocol version 754 (1.16.5, 1.16.4) using ProtocolLib
 * https://wiki.vg/Protocol
 * @author Rumsfield
 *
 */
public class TeamPacketSender_p754 implements TeamPacketSender {

	@Override
	public void sendPlayerTeamPacket(Player player, List<String> teamNames, Team team) {
		// Create team packet
		boolean fieldNameSuccess = false;
		boolean fieldModeSuccess = false;
		boolean fieldPlayersSuccess = false;
		
		PacketContainer teamPacket = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
		try {
			
			teamPacket.getStrings().write(0, team.getName());
			fieldNameSuccess = true;

			teamPacket.getIntegers().write(0, 3);
			fieldModeSuccess = true;

			teamPacket.getSpecificModifier(Collection.class).write(0,teamNames);
			fieldPlayersSuccess = true;
			
			try {
			    KonquestPlugin.getProtocolManager().sendServerPacket(player, teamPacket);
			} catch (InvocationTargetException e) {
			    throw new RuntimeException(
			        "Cannot send packet " + teamPacket, e);
			}
			
		} catch(FieldAccessException e) {
			ChatUtil.printDebug("Failed to create team packet for player "+player.getName()+", field status is "+fieldNameSuccess+","+fieldModeSuccess+","+fieldPlayersSuccess+": "+e.getMessage());
		}
	}

}
