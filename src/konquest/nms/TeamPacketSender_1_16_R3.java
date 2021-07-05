package konquest.nms;
/*
import java.util.List;

import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import net.minecraft.server.v1_16_R3.PacketPlayOutScoreboardTeam;
import net.minecraft.server.v1_16_R3.ScoreboardTeam;
*/
public class TeamPacketSender_1_16_R3 {}
/*
public class TeamPacketSender_1_16_R3 implements TeamPacketSender {
	
	@Override
	public void setPlayersToFriendlies(Player player, List<String> friendlies, Team friendlyTeam) {
        net.minecraft.server.v1_16_R3.Scoreboard nmsScoreboard = new net.minecraft.server.v1_16_R3.Scoreboard();
        ScoreboardTeam nmsTeam = new ScoreboardTeam(nmsScoreboard, friendlyTeam.getName());
        PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam(nmsTeam, friendlies, 3);
        CraftPlayer craftPlayer = (CraftPlayer) player;
        craftPlayer.getHandle().playerConnection.sendPacket(packet);
    }
 
	@Override
    public void setPlayersToEnemies(Player player, List<String> enemies, Team enemyTeam) {
        net.minecraft.server.v1_16_R3.Scoreboard nmsScoreboard = new net.minecraft.server.v1_16_R3.Scoreboard();
        ScoreboardTeam nmsTeam = new ScoreboardTeam(nmsScoreboard, enemyTeam.getName());
        PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam(nmsTeam, enemies, 3);
        CraftPlayer craftPlayer = (CraftPlayer) player;
        craftPlayer.getHandle().playerConnection.sendPacket(packet);
    }
    
	@Override
    public void setPlayersToBarbarians(Player player, List<String> barbarians, Team barbarianTeam) {
        net.minecraft.server.v1_16_R3.Scoreboard nmsScoreboard = new net.minecraft.server.v1_16_R3.Scoreboard();
        ScoreboardTeam nmsTeam = new ScoreboardTeam(nmsScoreboard, barbarianTeam.getName());
        PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam(nmsTeam, barbarians, 3);
        CraftPlayer craftPlayer = (CraftPlayer) player;
        craftPlayer.getHandle().playerConnection.sendPacket(packet);
    }

}
*/