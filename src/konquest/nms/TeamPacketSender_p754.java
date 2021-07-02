package konquest.nms;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
		ChatUtil.printDebug("Creating new team packet for player "+player.getName());
		
		PacketContainer teamPacket = new PacketContainer(PacketType.Play.Server.SCOREBOARD_TEAM);
		try {
			for(Field f : teamPacket.getStrings().getFields()) {
				ChatUtil.printDebug("  Found team packet string field "+f.getName());
			}
			teamPacket.getStrings().write(0, team.getName());
			
			for(Field f : teamPacket.getBytes().getFields()) {
				ChatUtil.printDebug("  Found team packet byte field "+f.getName());
			}
			teamPacket.getBytes().write(0, (byte)3);
			
			for(Field f : teamPacket.getIntegers().getFields()) {
				ChatUtil.printDebug("  Found team packet integer field "+f.getName());
			}
			teamPacket.getIntegers().write(0,teamNames.size());
			
			for(Field f : teamPacket.getStringArrays().getFields()) {
				ChatUtil.printDebug("  Found team packet string array field "+f.getName());
			}
			teamPacket.getStringArrays().write(0,teamNames.toArray(new String[0]));
			
			try {
			    KonquestPlugin.getProtocolManager().sendServerPacket(player, teamPacket);
			    ChatUtil.printDebug("Successfully sent team packet");
			} catch (InvocationTargetException e) {
			    throw new RuntimeException(
			        "Cannot send packet " + teamPacket, e);
			}
			
		} catch(FieldAccessException e) {
			ChatUtil.printDebug("Failed to create team packet");
		}
	}

}
