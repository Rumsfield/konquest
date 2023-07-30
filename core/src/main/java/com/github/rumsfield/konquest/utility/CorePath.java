package com.github.rumsfield.konquest.utility;

public enum CorePath {

	DEBUG                                                 ("core.debug"),
	WORLD_NAME                                            ("core.world_name"),
	WORLD_BLACKLIST                                       ("core.world_blacklist"),
	WORLD_BLACKLIST_REVERSE                               ("core.world_blacklist_reverse"),
	WORLD_BLACKLIST_IGNORE                                ("core.world_blacklist_ignore"),
	SAVE_INTERVAL                                         ("core.save_interval"),
	COMMUNITY_LINK                                        ("core.community_link"),
	ACCOMPLISHMENT_PREFIX                                 ("core.accomplishment_prefix"),
	DIRECTIVE_QUESTS                                      ("core.directive_quests"),
	RESET_LEGACY_HEALTH                                   ("core.reset_legacy_health"),
	PLACEHOLDER_REQUEST_LIMIT                             ("core.placeholder_request_limit"),
	PLAYER_NAMETAG_FORMAT                                 ("core.player_nametag_format"),
	PLAYER_NAMETAG_SUFFIX_RELATION                        ("core.player_nametag_suffix_relation"),
	BACKUP_DATA_AMOUNT                                    ("core.backup_data_amount"),

	CHAT_TAG                                              ("core.chat.tag"),
	CHAT_MESSAGE                                          ("core.chat.message"),
	CHAT_KINGDOM_TEAM_COLOR                               ("core.chat.kingdom_team_color"),
	CHAT_NAME_TEAM_COLOR                                  ("core.chat.name_team_color"),
	CHAT_ENABLE_FORMAT                                    ("core.chat.enable_format"),
	CHAT_PRIORITY                                         ("core.chat.priority"),

	COLORS_PRIMARY_FRIENDLY                               ("core.colors.primary.friendly"),
	COLORS_PRIMARY_ENEMY                                  ("core.colors.primary.enemy"),
	COLORS_PRIMARY_TRADE                                  ("core.colors.primary.trade"),
	COLORS_PRIMARY_PEACEFUL                               ("core.colors.primary.peaceful"),
	COLORS_PRIMARY_ALLY                                   ("core.colors.primary.ally"),
	COLORS_PRIMARY_BARBARIAN                              ("core.colors.primary.barbarian"),
	COLORS_PRIMARY_NEUTRAL                                ("core.colors.primary.neutral"),
	COLORS_SECONDARY_FRIENDLY                             ("core.colors.secondary.friendly"),
	COLORS_SECONDARY_ENEMY                                ("core.colors.secondary.enemy"),
	COLORS_SECONDARY_TRADE                                ("core.colors.secondary.trade"),
	COLORS_SECONDARY_PEACEFUL                             ("core.colors.secondary.peaceful"),
	COLORS_SECONDARY_ALLY                                 ("core.colors.secondary.ally"),
	COLORS_SECONDARY_BARBARIAN                            ("core.colors.secondary.barbarian"),
	COLORS_SECONDARY_NEUTRAL                              ("core.colors.secondary.neutral"),

	DATABASE_CONNECTION                                   ("core.database.connection"),
	DATABASE_MYSQL_HOSTNAME                               ("core.database.mysql.hostname"),
	DATABASE_MYSQL_PORT                                   ("core.database.mysql.port"),
	DATABASE_MYSQL_DATABASE                               ("core.database.mysql.database"),
	DATABASE_MYSQL_USERNAME                               ("core.database.mysql.username"),
	DATABASE_MYSQL_PASSWORD                               ("core.database.mysql.password"),
	DATABASE_MYSQL_PROPERTIES                             ("core.database.mysql.properties"),

	INTEGRATION_CHESTSHOP                                 ("core.integration.chestshop"),
	INTEGRATION_QUICKSHOP                                 ("core.integration.quickshop"),
	INTEGRATION_LUCKPERMS                                 ("core.integration.luckperms"),
	INTEGRATION_DYNMAP                                    ("core.integration.dynmap"),
	INTEGRATION_BLUEMAP                                   ("core.integration.bluemap"),
	INTEGRATION_MAP_OPTIONS_ENABLE_KINGDOMS               ("core.integration.map_options.enable_kingdoms"),
	INTEGRATION_MAP_OPTIONS_ENABLE_CAMPS                  ("core.integration.map_options.enable_camps"),
	INTEGRATION_MAP_OPTIONS_ENABLE_SANCTUARIES            ("core.integration.map_options.enable_sanctuaries"),
	INTEGRATION_MAP_OPTIONS_ENABLE_RUINS                  ("core.integration.map_options.enable_ruins"),
	INTEGRATION_DISCORDSRV                                ("core.integration.discordsrv"),
	INTEGRATION_DISCORDSRV_OPTIONS_RAID_ALERT_DIRECT      ("core.integration.discordsrv_options.raid_alert_direct"),
	INTEGRATION_DISCORDSRV_OPTIONS_RAID_ALERT_CHANNEL     ("core.integration.discordsrv_options.raid_alert_channel"),
	
	TRAVEL_ENABLE_SANCTUARY                               ("core.travel.enable.sanctuary"),
	TRAVEL_ENABLE_TOWNS                                   ("core.travel.enable.towns"),
	TRAVEL_ENABLE_CAPITAL                                 ("core.travel.enable.capital"),
	TRAVEL_ENABLE_HOME                                    ("core.travel.enable.home"),
	TRAVEL_ENABLE_CAMP                                    ("core.travel.enable.camp"),
	TRAVEL_ENABLE_WILD                                    ("core.travel.enable.wild"),
	TRAVEL_WILD_RADIUS                                    ("core.travel.wild_radius"),
	TRAVEL_WILD_CENTER_X                                  ("core.travel.wild_center_x"),
	TRAVEL_WILD_CENTER_Z                                  ("core.travel.wild_center_z"),
	TRAVEL_WARMUP                                         ("core.travel.warmup"),
	TRAVEL_CANCEL_ON_MOVE                                 ("core.travel.cancel_on_move"),
	
	KINGDOMS_CAPITAL_SUFFIX                               ("core.kingdoms.capital_suffix"),
	KINGDOMS_CAPITAL_RESPAWN                              ("core.kingdoms.capital_respawn"),
	KINGDOMS_CAPITAL_IMMUNITY_TOWNS                       ("core.kingdoms.capital_immunity_towns"),
	KINGDOMS_ALLY_DEFENSE_PACT                            ("core.kingdoms.ally_defense_pact"),
	KINGDOMS_INSTANT_WAR                                  ("core.kingdoms.instant_war"),
	KINGDOMS_INSTANT_PEACE                                ("core.kingdoms.instant_peace"),
	KINGDOMS_ALLOW_PEACEFUL_PVP                           ("core.kingdoms.allow_peaceful_pvp"),
	KINGDOMS_CREATE_ADMIN_ONLY                            ("core.kingdoms.create_admin_only"),
	KINGDOMS_WEB_COLOR_ADMIN_ONLY                         ("core.kingdoms.web_color_admin_only"),
	KINGDOMS_PROTECT_CONTAINERS_USE                       ("core.kingdoms.protect_containers_use"),
	KINGDOMS_PROTECT_CONTAINERS_BREAK                     ("core.kingdoms.protect_containers_break"),
	KINGDOMS_PROTECT_CONTAINERS_EXPLODE                   ("core.kingdoms.protect_containers_explode"),
	KINGDOMS_NO_ENEMY_TRAVEL                              ("core.kingdoms.no_enemy_travel"),
	KINGDOMS_NO_ENEMY_EDIT_OFFLINE                        ("core.kingdoms.no_enemy_edit_offline"),
	KINGDOMS_NO_ENEMY_EDIT_OFFLINE_WARMUP                 ("core.kingdoms.no_enemy_edit_offline_warmup"),
	KINGDOMS_NO_ENEMY_EDIT_OFFLINE_MINIMUM                ("core.kingdoms.no_enemy_edit_offline_minimum"),
	KINGDOMS_NO_ENEMY_ENDER_PEARL                         ("core.kingdoms.no_enemy_ender_pearl"),
	KINGDOMS_SMALLEST_EXP_BOOST_PERCENT                   ("core.kingdoms.smallest_exp_boost_percent"),
	KINGDOMS_OFFLINE_TIMEOUT_DAYS                         ("core.kingdoms.offline_timeout_days"),
	KINGDOMS_OFFLINE_TIMEOUT_EXILE                        ("core.kingdoms.offline_timeout_exile"),
	KINGDOMS_GOLEM_ATTACK_ENEMIES                         ("core.kingdoms.golem_attack_enemies"),
	KINGDOMS_ATTACK_FRIENDLY_GOLEMS                       ("core.kingdoms.attack_friendly_golems"),
	KINGDOMS_MAX_PLAYER_DIFF                              ("core.kingdoms.max_player_diff"),
	KINGDOMS_ALLOW_EXILE_SWITCH                           ("core.kingdoms.allow_exile_switch"),
	KINGDOMS_EXILE_COOLDOWN                               ("core.kingdoms.exile_cooldown"),
	KINGDOMS_JOIN_COOLDOWN                                ("core.kingdoms.join_cooldown"),
	KINGDOMS_PER_KINGDOM_JOIN_PERMISSIONS                 ("core.kingdoms.per_kingdom_join_permissions"),
	KINGDOMS_WILD_PVP                                     ("core.kingdoms.wild_pvp"),
	KINGDOMS_WILD_USE                                     ("core.kingdoms.wild_use"),
	KINGDOMS_WILD_BUILD                                   ("core.kingdoms.wild_build"),
	
	TOWNS_ALLOW_UNCLAIM                                   ("core.towns.allow_unclaim"),
	TOWNS_ALLOW_CLAIM_RADIUS                              ("core.towns.allow_claim_radius"),
	TOWNS_ALLOW_CLAIM_AUTO                                ("core.towns.allow_claim_auto"),
	TOWNS_MIN_SETTLE_HEIGHT                               ("core.towns.min_settle_height"),
	TOWNS_MAX_SETTLE_HEIGHT                               ("core.towns.max_settle_height"),
	TOWNS_MIN_DISTANCE_TOWN                               ("core.towns.min_distance_town"),
	TOWNS_MIN_DISTANCE_SANCTUARY                          ("core.towns.min_distance_sanctuary"),
	TOWNS_MAX_DISTANCE_ALL                                ("core.towns.max_distance_all"),
	TOWNS_MAX_SIZE                                        ("core.towns.max_size"),
	TOWNS_SETTLE_CHECKS_DEPTH                             ("core.towns.settle_checks_depth"),
	TOWNS_SETTLE_CHECK_FLATNESS                           ("core.towns.settle_check_flatness"),
	TOWNS_INIT_RADIUS                                     ("core.towns.init_radius"),
	TOWNS_CAPTURE_UPGRADES                                ("core.towns.capture_upgrades"),
	TOWNS_CAPTURE_COOLDOWN                                ("core.towns.capture_cooldown"),
	TOWNS_RAID_ALERT_COOLDOWN                             ("core.towns.raid_alert_cooldown"),
	TOWNS_TRAVEL_COOLDOWN                                 ("core.towns.travel_cooldown"),
	TOWNS_ENABLE_UPGRADES                                 ("core.towns.enable_upgrades"),
	TOWNS_ENABLE_ARMOR                                    ("core.towns.enable_armor"),
	TOWNS_ENABLE_SHIELDS                                  ("core.towns.enable_shields"),
	TOWNS_ARMOR_TNT_DAMAGE                                ("core.towns.armor_tnt_damage"),
	TOWNS_SHIELDS_WHILE_ATTACKED                          ("core.towns.shields_while_attacked"),
	TOWNS_ARMOR_WHILE_ATTACKED                            ("core.towns.armor_while_attacked"),
	TOWNS_SHIELDS_ADD                                     ("core.towns.shields_add"),
	TOWNS_ARMOR_ADD                                       ("core.towns.armor_add"),
	TOWNS_MAX_SHIELDS                                     ("core.towns.max_shields"),
	TOWNS_MAX_ARMOR                                       ("core.towns.max_armor"),
	TOWNS_SHIELD_NEW_TOWNS                                ("core.towns.shield_new_towns"),
	TOWNS_BARBARIANS_DESTROY                              ("core.towns.barbarians_destroy"),
	TOWNS_ENEMY_GLOW                                      ("core.towns.enemy_glow"),
	TOWNS_ARMOR_BLACKLIST_ENABLE                          ("core.towns.armor_blacklist_enable"),
	TOWNS_ARMOR_BLACKLIST_REVERSE                         ("core.towns.armor_blacklist_reverse"),
	TOWNS_ARMOR_BLACKLIST                                 ("core.towns.armor_blacklist"),
	TOWNS_DISCOUNT_ENABLE                                 ("core.towns.discount_enable"),
	TOWNS_DISCOUNT_PERCENT                                ("core.towns.discount_percent"),
	TOWNS_DISCOUNT_STACK                                  ("core.towns.discount_stack"),
	
	PLOTS_ENABLE                                          ("core.plots.enable"),
	PLOTS_MAX_SIZE                                        ("core.plots.max_size"),
	PLOTS_ALLOW_BUILD                                     ("core.plots.allow_build"),
	PLOTS_ALLOW_CONTAINERS                                ("core.plots.allow_containers"),
	PLOTS_IGNORE_KNIGHTS                                  ("core.plots.ignore_knights"),
	
	EXILE_REMOVE_FAVOR                                    ("core.exile.remove_favor"),
	EXILE_REMOVE_STATS                                    ("core.exile.remove_stats"),
	EXILE_TELEPORT_WILD                                   ("core.exile.teleport_wild"),
	EXILE_TELEPORT_WORLD_SPAWN                            ("core.exile.teleport_world_spawn"),
	
	CAMPS_ENABLE                                          ("core.camps.enable"),
	CAMPS_INIT_RADIUS                                     ("core.camps.init_radius"),
	CAMPS_PROTECT_CONTAINERS                              ("core.camps.protect_containers"),
	CAMPS_NO_ENEMY_EDIT_OFFLINE                           ("core.camps.no_enemy_edit_offline"),
	CAMPS_NO_ENEMY_EDIT_OFFLINE_WARMUP                    ("core.camps.no_enemy_edit_offline_warmup"),
	CAMPS_NO_ENEMY_TRAVEL                                 ("core.camps.no_enemy_travel"),
	CAMPS_CLAN_ENABLE                                     ("core.camps.clan_enable"),
	CAMPS_CLAN_ALLOW_CONTAINERS                           ("core.camps.clan_allow_containers"),
	CAMPS_CLAN_ALLOW_EDIT_OFFLINE                         ("core.camps.clan_allow_edit_offline"),
	CAMPS_CLAN_ALLOW_JOIN_OFFLINE                         ("core.camps.clan_allow_join_offline"),
	CAMPS_ENEMY_GLOW                                      ("core.camps.enemy_glow"),
	
	MONUMENTS_CRITICAL_BLOCK                              ("core.monuments.critical_block"),
	MONUMENTS_DESTROY_AMOUNT                              ("core.monuments.destroy_amount"),
	MONUMENTS_DAMAGE_REGEN                                ("core.monuments.damage_regen"),
	MONUMENTS_LOOT_REFRESH                                ("core.monuments.loot_refresh"),
	MONUMENTS_LOOT_COUNT                                  ("core.monuments.loot_count"),
	
	RUINS_CRITICAL_BLOCK                                  ("core.ruins.critical_block"),
	RUINS_CAPTURE_COOLDOWN                                ("core.ruins.capture_cooldown"),
	RUINS_RESPAWN_COOLDOWN                                ("core.ruins.respawn_cooldown"),
	RUINS_CAPTURE_REWARD_FAVOR                            ("core.ruins.capture_reward_favor"),
	RUINS_CAPTURE_REWARD_EXP                              ("core.ruins.capture_reward_exp"),
	RUINS_NO_GOLEM_DROPS                                  ("core.ruins.no_golem_drops"),
	
	FAVOR_COST_SPY                                        ("core.favor.cost_spy"),
	FAVOR_COST_CLAIM                                      ("core.favor.cost_claim"),
	FAVOR_COST_TRAVEL                                     ("core.favor.cost_travel"),
	FAVOR_COST_TRAVEL_PER_CHUNK                           ("core.favor.cost_travel_per_chunk"),
	FAVOR_COST_TRAVEL_WORLD                               ("core.favor.cost_travel_world"),
	FAVOR_COST_TRAVEL_CAMP                                ("core.favor.cost_travel_camp"),
	FAVOR_ALLOW_TRAVEL_ALWAYS                             ("core.favor.allow_travel_always"),
	FAVOR_TOWNS_COST_SETTLE                               ("core.favor.towns.cost_settle"),
	FAVOR_TOWNS_COST_SETTLE_INCREMENT                     ("core.favor.towns.cost_settle_increment"),
	FAVOR_TOWNS_COST_RENAME                               ("core.favor.towns.cost_rename"),
	FAVOR_TOWNS_COST_SPECIALIZE                           ("core.favor.towns.cost_specialize"),
	FAVOR_KINGDOMS_COST_CREATE                            ("core.favor.kingdoms.cost_create"),
	FAVOR_KINGDOMS_COST_RENAME                            ("core.favor.kingdoms.cost_rename"),
	FAVOR_KINGDOMS_COST_TEMPLATE                          ("core.favor.kingdoms.cost_template"),
	FAVOR_KINGDOMS_PAY_INTERVAL_SECONDS                   ("core.favor.kingdoms.pay_interval_seconds"),
	FAVOR_KINGDOMS_PAY_PER_CHUNK                          ("core.favor.kingdoms.pay_per_chunk"),
	FAVOR_KINGDOMS_PAY_PER_RESIDENT                       ("core.favor.kingdoms.pay_per_resident"),
	FAVOR_KINGDOMS_PAY_LIMIT                              ("core.favor.kingdoms.pay_limit"),
	FAVOR_KINGDOMS_BONUS_OFFICER_PERCENT                  ("core.favor.kingdoms.bonus_officer_percent"),
	FAVOR_KINGDOMS_BONUS_MASTER_PERCENT                   ("core.favor.kingdoms.bonus_master_percent"),
	FAVOR_DIPLOMACY_COST_WAR                              ("core.favor.diplomacy.cost_war"),
	FAVOR_DIPLOMACY_COST_PEACE                            ("core.favor.diplomacy.cost_peace"),
	FAVOR_DIPLOMACY_COST_TRADE                            ("core.favor.diplomacy.cost_trade"),
	FAVOR_DIPLOMACY_COST_ALLIANCE                         ("core.favor.diplomacy.cost_alliance"),
	FAVOR_REWARDS_SETTLE_TOWN                             ("core.favor.rewards.settle_town"),
	FAVOR_REWARDS_CLAIM_LAND                              ("core.favor.rewards.claim_land"),
	FAVOR_REWARDS_BUILD_TOWN                              ("core.favor.rewards.build_town"),
	FAVOR_REWARDS_CREATE_GOLEM                            ("core.favor.rewards.create_golem"),
	FAVOR_REWARDS_CRAFT_ARMOR                             ("core.favor.rewards.craft_armor"),
	FAVOR_REWARDS_ENCHANT_ITEM                            ("core.favor.rewards.enchant_item"),
	FAVOR_REWARDS_ATTACK_TOWN                             ("core.favor.rewards.attack_town"),
	FAVOR_REWARDS_CAPTURE_TOWN                            ("core.favor.rewards.capture_town"),
	FAVOR_REWARDS_KILL_ENEMY                              ("core.favor.rewards.kill_enemy"),
	FAVOR_REWARDS_DEFEND_RAID                             ("core.favor.rewards.defend_raid"),
	
	COMBAT_PLACEHOLDER_TAG                                ("core.combat.placeholder_tag"),
	COMBAT_PREVENT_COMMAND_ON_DAMAGE                      ("core.combat.prevent_command_on_damage"),
	COMBAT_ENEMY_DAMAGE_COOLDOWN_SECONDS                  ("core.combat.enemy_damage_cooldown_seconds"),
	COMBAT_PREVENT_COMMAND_LIST                           ("core.combat.prevent_command_list"),
	
	NONE									  ("");
	
	
	private final String path;
	
	CorePath(String path) {
		this.path = path;
	}
	
	public String getPath() {
		return path;
    }
}
