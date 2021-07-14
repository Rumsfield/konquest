package konquest;

import org.bukkit.entity.Player;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

/**
 * This class will be registered through the register-method in the 
 * plugins onEnable-method.
 */
public class KonquestPlaceholderExpansion extends PlaceholderExpansion {

	private KonquestPlugin plugin;
	
	public KonquestPlaceholderExpansion(KonquestPlugin plugin) {
		this.plugin = plugin;
	}
	
	/**
     * Because this is an internal class,
     * you must override this method to let PlaceholderAPI know to not unregister your expansion class when
     * PlaceholderAPI is reloaded
     *
     * @return true to persist through reloads
     */
	@Override
    public boolean persist(){
        return true;
    }
	
	/**
     * Because this is a internal class, this check is not needed
     * and we can simply return {@code true}
     *
     * @return Always true since it's an internal class.
     */
    @Override
    public boolean canRegister(){
        return true;
    }
    
    /**
     * The name of the person who created this expansion should go here.
     * For convienience do we return the author from the plugin.yml
     * 
     * @return The name of the author as a String.
     */
    @Override
    public String getAuthor(){
        return plugin.getDescription().getAuthors().toString();
    }

    /**
     * The placeholder identifier should go here.
     * This is what tells PlaceholderAPI to call our onRequest 
     * method to obtain a value if a placeholder starts with our 
     * identifier.
     * The identifier has to be lowercase and can't contain _ or %
     *
     * @return The identifier in {@code %<identifier>_<value>%} as String.
     */
    @Override
    public String getIdentifier(){
        return "konquest";
    }

    /**
     * This is the version of the expansion.
     * You don't have to use numbers, since it is set as a String.
     *
     * For convienience do we return the version from the plugin.yml
     *
     * @return The version as a String.
     */
    @Override
    public String getVersion(){
        return plugin.getDescription().getVersion();
    }
    
    /**
     * This is the method called when a placeholder with our identifier 
     * is found and needs a value.
     * We specify the value identifier in this method.
     * Since version 2.9.1 can you use OfflinePlayers in your requests.
     *
     * @param  player
     *         A {@link org.bukkit.Player Player}.
     * @param  identifier
     *         A String containing the identifier/value.
     *
     * @return possibly-null String of the requested identifier.
     */
    @Override
    public String onPlaceholderRequest(Player player, String identifier){

        if(player == null){
            return "";
        }

        // %someplugin_placeholder1%
        if(identifier.equals("placeholder1")){
            return plugin.getConfig().getString("placeholder1", "value doesnt exist");
        }

        // %someplugin_placeholder2%
        if(identifier.equals("placeholder2")){
            return plugin.getConfig().getString("placeholder2", "value doesnt exist");
        }
 
        // We return null if an invalid placeholder (f.e. %someplugin_placeholder3%) 
        // was provided
        return null;
    }

}
