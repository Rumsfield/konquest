package konquest.display;

public interface StateMenu {

	public DisplayMenu getCurrentView();
	
	public DisplayMenu updateState(int slot, boolean clickType);
}
