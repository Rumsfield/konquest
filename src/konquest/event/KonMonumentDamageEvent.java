package konquest.event;

import konquest.Konquest;
import konquest.model.KonPlayer;
import konquest.model.KonTerritory;

import org.bukkit.event.Cancellable;
import org.bukkit.event.block.BlockBreakEvent;

public class KonMonumentDamageEvent extends KonEvent implements Cancellable {
	
	private KonPlayer player;
	private KonTerritory territory;
	private BlockBreakEvent blockEvent;
	private boolean isCancelled;
	
	public KonMonumentDamageEvent(Konquest konquest, KonPlayer player, KonTerritory territory, BlockBreakEvent blockEvent) {
		super(konquest);
		this.blockEvent = blockEvent;
		this.territory = territory;
		this.player = player;
		this.isCancelled = false;
	}
	
	public BlockBreakEvent getBlockEvent() {
		return blockEvent;
	}
	
	public KonTerritory getTerritory() {
		return territory;
	}
	
	public KonPlayer getPlayer() {
		return player;
	}
	
	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean val) {
		isCancelled = val;
		blockEvent.setCancelled(true);
	}

}