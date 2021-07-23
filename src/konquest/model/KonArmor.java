package konquest.model;

public class KonArmor {

	private String id;
	private int blocks;
	private int cost;
	
	public KonArmor(String id, int blocks, int cost) {
		this.id = id;
		this.blocks = blocks;
		this.cost = cost;
	}
	
	public String getId() {
		return id;
	}
	
	public int getBlocks() {
		return blocks;
	}
	
	public int getCost() {
		return cost;
	}
}
