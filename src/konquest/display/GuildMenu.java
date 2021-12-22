package konquest.display;

import java.util.ArrayList;
import java.util.HashMap;

import konquest.model.KonPlayer;

public class GuildMenu implements StateMenu {

	enum MenuState {
		ROOT,
		A_JOIN,
		A_LEAVE,
		A_INVITE,
		A_LIST,
		B_RELATIONSHIP,
		B_REQUESTS,
		C_PROMOTE,
		C_DEMOTE,
		C_SPECIALIZE;
	}
	/*
	 * State Views
	 * Regular players: A_*
	 * Officer players: B_*
	 * Master players: C_*
	 */
	
	private HashMap<MenuState,DisplayMenu> views;
	private ArrayList<DisplayMenu> pages;
	private int currentPage;
	private MenuState currentState;
	
	public GuildMenu(KonPlayer player) {
		this.views = new HashMap<MenuState,DisplayMenu>();
		this.pages = new ArrayList<DisplayMenu>();
		this.currentPage = 0;
		this.currentState = MenuState.ROOT;
		
		initializeMenu();
	}
	
	
	private void initializeMenu() {
		//TODO: Determine which icons to render based on player
		
	}
	
	private DisplayMenu createRootView() {
		
		return null;
	}


	@Override
	public DisplayMenu getCurrentView() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public DisplayMenu updateState(int slot) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
