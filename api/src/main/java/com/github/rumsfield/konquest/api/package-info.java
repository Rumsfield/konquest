/**
 * The base API package for Konquest.
 * <p>
 * Obtain an instance of the KonquestAPI interface in order to access the rest of the API.
 * For example:
 * </p>
 * <pre>
 * <code>
 * KonquestAPI api = null;
 * Plugin konquest = Bukkit.getPluginManager().getPlugin("Konquest");
 * if (konquest != null {@literal&}{@literal&} konquest.isEnabled()) {
 *     RegisteredServiceProvider{@literal<}KonquestAPI{@literal>} provider = Bukkit.getServicesManager().getRegistration(KonquestAPI.class);
 *     if (provider != null) {
 *         api = provider.getProvider();
 *         Bukkit.getServer().getConsoleSender().sendMessage("Successfully enabled Konquest API");
 *     } else {
 *         Bukkit.getServer().getConsoleSender().sendMessage("Failed to enable Konquest API, invalid provider");
 *     }
 * } else {
 *     Bukkit.getServer().getConsoleSender().sendMessage("Failed to enable Konquest API, plugin not found or disabled");
 * }
 * </code>
 * </pre>
 * 
 * @author Rumsfield
 *
 */
package com.github.rumsfield.konquest.api;
