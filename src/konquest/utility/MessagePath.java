package konquest.utility;

import konquest.Konquest;

public enum MessagePath {

	LABEL_ONLINE_PLAYERS                        (0, "label.online-players"),
	LABEL_TOTAL_PLAYERS                         (0, "label.total-players"),
	LABEL_PLAYERS                               (0, "label.players"),
	LABEL_KINGDOMS                              (0, "label.kingdoms"),
	LABEL_PLAYER                                (0, "label.player"),
	LABEL_KINGDOM                               (0, "label.kingdom"),
	LABEL_BARBARIAN                             (0, "label.barbarian"),
	LABEL_RESIDENCIES                           (0, "label.residencies"),
	LABEL_TOWNS                                 (0, "label.towns"),
	LABEL_LAND                                  (0, "label.land"),
	LABEL_FAVOR                                 (0, "label.favor"),
	LABEL_OPEN                                  (0, "label.open"),
	LABEL_HEALTH                                (0, "label.health"),
	LABEL_UPGRADES                              (0, "label.upgrades"),
	LABEL_RESIDENTS                             (0, "label.residents"),
	LABEL_INVITES                               (0, "label.invites"),
	LABEL_REQUESTS                              (0, "label.requests"),

	GENERIC_ERROR_DENY_BARBARIAN                (0, "generic.error.deny-barbarian"),
	GENERIC_ERROR_INTERNAL                      (0, "generic.error.internal"),
	GENERIC_ERROR_INTERNAL_MESSAGE              (1, "generic.error.internal-message"),
	GENERIC_ERROR_INVALID_PARAMETERS            (0, "generic.error.invalid-parameters"),
	GENERIC_ERROR_INVALID_WORLD                 (0, "generic.error.invalid-world"),
	GENERIC_ERROR_NO_PERMISSION                 (0, "generic.error.no-permission"),
	GENERIC_ERROR_NO_ALLOW                      (0, "generic.error.no-allow"),
	GENERIC_ERROR_NO_PLAYER                     (0, "generic.error.no-player"),
	GENERIC_ERROR_UNKNOWN_NAME                  (1, "generic.error.unknown-name"),
	GENERIC_ERROR_BAD_NAME                      (1, "generic.error.bad-name"),
	GENERIC_ERROR_NO_FAVOR                      (1, "generic.error.no-favor"),
	GENERIC_ERROR_ENEMY_PLAYER                  (0, "generic.error.enemy-player"),
	GENERIC_ERROR_ENEMY_TOWN                    (0, "generic.error.enemy-town"),
	GENERIC_ERROR_DISABLED                      (0, "generic.error.disabled"),
	GENERIC_NOTICE_ENABLE_AUTO                  (0, "generic.notice.enable-auto"),
	GENERIC_NOTICE_DISABLE_AUTO                 (0, "generic.notice.disable-auto"),
	GENERIC_NOTICE_REDUCE_FAVOR                 (2, "generic.notice.reduce-favor"),
	GENERIC_NOTICE_REWARD_FAVOR                 (2, "generic.notice.reward-favor"),

	COMMAND_CHAT_NOTICE_ENABLE                  (0, "command.chat.notice.enable"),
	COMMAND_CHAT_NOTICE_DISABLE                 (0, "command.chat.notice.disable"),
	COMMAND_CLAIM_ERROR_RADIUS                  (2, "command.claim.error.radius"),
	COMMAND_EXILE_NOTICE_PROMPT_1               (0, "command.exile.notice.prompt-line-1"),
	COMMAND_EXILE_NOTICE_PROMPT_2               (0, "command.exile.notice.prompt-line-2"),
	COMMAND_EXILE_NOTICE_PROMPT_3A              (0, "command.exile.notice.prompt-line-3a"),
	COMMAND_EXILE_NOTICE_PROMPT_3B              (0, "command.exile.notice.prompt-line-3b"),
	COMMAND_EXILE_NOTICE_PROMPT_4               (0, "command.exile.notice.prompt-line-4"),
	COMMAND_EXILE_NOTICE_CONFIRMED              (0, "command.exile.notice.confirmed"),
	COMMAND_FAVOR_NOTICE_MESSAGE                (1, "command.favor.notice.message"),
	COMMAND_HELP_NOTICE_MESSAGE                 (0, "command.help.notice.message"),
	COMMAND_INFO_NOTICE_KINGDOM_HEADER          (1, "command.info.notice.kingdom-header"),
	COMMAND_INFO_NOTICE_TOWN_HEADER             (1, "command.info.notice.town-header"),
	COMMAND_JOIN_BROADCAST_PLAYER_JOIN          (2, "command.join.broadcast.player-join"),
	COMMAND_JOIN_NOTICE_KINGDOM_JOIN            (1, "command.join.notice.kingdom-join"),
	COMMAND_JOIN_NOTICE_TOWN_RESIDENT           (2, "command.join.notice.town-resident"),
	COMMAND_JOIN_NOTICE_TOWN_JOIN_LORD          (1, "command.join.notice.town-join-lord"),
	COMMAND_JOIN_NOTICE_TOWN_REQUEST            (1, "command.join.notice.town-request"),
	COMMAND_JOIN_ERROR_NO_KINGDOMS              (0, "command.join.error.no-kingdoms"),
	COMMAND_JOIN_ERROR_KINGDOM_LIMIT            (1, "command.join.error.kingdom-limit"),
	COMMAND_JOIN_ERROR_KINGDOM_DENIED           (0, "command.join.error.kingdom-denied"),
	COMMAND_JOIN_ERROR_KINGDOM_MEMBER           (0, "command.join.error.kingdom-member"),
	COMMAND_JOIN_ERROR_KINGDOM_EXILE            (0, "command.join.error.kingdom-exile"),
	COMMAND_JOIN_ERROR_TOWN_MEMBER              (1, "command.join.error.town-member"),
	COMMAND_JOIN_ERROR_TOWN_REQUEST             (1, "command.join.error.town-request"),
	COMMAND_LEAVE_NOTICE_TOWN_RESIDENT          (2, "command.leave.notice.town-resident"),
	COMMAND_LEAVE_NOTICE_PLAYER                 (1, "command.leave.notice.player"),
	COMMAND_LEAVE_NOTICE_INVITE_DECLINE         (1, "command.leave.notice.invite-decline"),
	COMMAND_LEAVE_ERROR_NO_RESIDENT             (1, "command.leave.error.no-resident"),
	COMMAND_LIST_NOTICE_TOWNS                   (0, "command.list.notice.towns"),
	COMMAND_PREFIX_NOTICE_OFF                   (0, "command.prefix.notice.off"),
	COMMAND_PREFIX_NOTICE_NEW                   (1, "command.prefix.notice.new"),
	COMMAND_PREFIX_ERROR_OFF                    (0, "command.prefix.error.off"),
	COMMAND_PREFIX_ERROR_NEW                    (1, "command.prefix.error.new"),
	COMMAND_SCORE_NOTICE_ALL_HEADER             (0, "command.score.notice.all-header"),
	COMMAND_SCORE_NOTICE_SCORE                  (3, "command.score.notice.score"),
	COMMAND_SCORE_NOTICE_PLAYER                 (4, "command.score.notice.player"),
	COMMAND_SCORE_ERROR_PEACEFUL                (1, "command.score.error.peaceful"),
	COMMAND_SCORE_ERROR_BARBARIAN               (1, "command.score.error.barbarian"),
	COMMAND_SETTLE_BROADCAST_SETTLE             (3, "command.settle.broadcast.settle"),
	COMMAND_SETTLE_NOTICE_SUCCESS               (1, "command.settle.notice.success"),
	COMMAND_SETTLE_NOTICE_MAP_HINT              (0, "command.settle.notice.map-hint"),
	COMMAND_SETTLE_ERROR_MISSING_NAME           (0, "command.settle.error.missing-name"),
	COMMAND_SETTLE_ERROR_SPACE_NAME             (0, "command.settle.error.space-name"),
	COMMAND_SETTLE_ERROR_BAD_NAME               (0, "command.settle.error.bad-name"),
	COMMAND_SETTLE_ERROR_FAIL_NAME              (0, "command.settle.error.fail-name"),
	COMMAND_SETTLE_ERROR_FAIL_OVERLAP           (0, "command.settle.error.fail-overlap"),
	COMMAND_SETTLE_ERROR_FAIL_PLACEMENT         (0, "command.settle.error.fail-placement"),
	COMMAND_SETTLE_ERROR_FAIL_TEMPLATE          (0, "command.settle.error.fail-template"),
	COMMAND_SETTLE_ERROR_FAIL_PROXIMITY         (2, "command.settle.error.fail-proximity"),
	COMMAND_SETTLE_ERROR_FAIL_FLAT              (0, "command.settle.error.fail-flat"),
	COMMAND_SETTLE_ERROR_FAIL_HEIGHT            (0, "command.settle.error.fail-height"),
	COMMAND_SETTLE_ERROR_FAIL_AIR               (0, "command.settle.error.fail-air"),
	COMMAND_SETTLE_ERROR_FAIL_INIT              (0, "command.settle.error.fail-init"),
	COMMAND_SPY_NOTICE_NEARBY                   (0, "command.spy.notice.nearby"),
	COMMAND_SPY_NOTICE_REGIONAL                 (0, "command.spy.notice.regional"),
	COMMAND_SPY_NOTICE_FARAWAY                  (0, "command.spy.notice.faraway"),
	COMMAND_SPY_NOTICE_VERY_DISTANT             (0, "command.spy.notice.very-distant"),
	COMMAND_SPY_NOTICE_SUCCESS                  (1, "command.spy.notice.success"),
	COMMAND_SPY_ERROR_INVENTORY                 (0, "command.spy.error.inventory"),
	COMMAND_SPY_ERROR_TOWN                      (0, "command.spy.error.town"),
	COMMAND_TOWN_NOTICE_OPEN                    (1, "command.town.notice.open"),
	COMMAND_TOWN_NOTICE_CLOSE                   (1, "command.town.notice.close"),
	COMMAND_TOWN_NOTICE_ADD_INVITE_REMINDER     (1, "command.town.notice.add-invite-reminder"),
	COMMAND_TOWN_NOTICE_ADD_SUCCESS             (2, "command.town.notice.add-success"),
	COMMAND_TOWN_NOTICE_ADD_INVITE              (2, "command.town.notice.add-invite"),
	COMMAND_TOWN_NOTICE_ADD_INVITE_PLAYER       (3, "command.town.notice.add-invite-player"),
	COMMAND_TOWN_NOTICE_KICK_REMOVE             (2, "command.town.notice.kick-remove"),
	COMMAND_TOWN_NOTICE_KICK_RESIDENT           (2, "command.town.notice.kick-resident"),
	COMMAND_TOWN_NOTICE_LORD_PROMPT_1           (1, "command.town.notice.lord-prompt-1"),
	COMMAND_TOWN_NOTICE_LORD_PROMPT_2           (0, "command.town.notice.lord-prompt-2"),
	COMMAND_TOWN_NOTICE_LORD_PROMPT_3           (0, "command.town.notice.lord-prompt-3"),
	COMMAND_TOWN_NOTICE_LORD_SUCCESS            (2, "command.town.notice.lord-success"),
	COMMAND_TOWN_NOTICE_LORD_CLAIM              (2, "command.town.notice.lord-claim"),
	COMMAND_TOWN_NOTICE_KNIGHT_SET              (2, "command.town.notice.knight-set"),
	COMMAND_TOWN_NOTICE_KNIGHT_CLEAR            (2, "command.town.notice.knight-clear"),
	COMMAND_TOWN_NOTICE_RENAME                  (3, "command.town.notice.rename"),
	COMMAND_TOWN_NOTICE_NO_LORD                 (3, "command.town.notice.no-lord"),
	COMMAND_TOWN_ERROR_OPEN                     (1, "command.town.error.open"),
	COMMAND_TOWN_ERROR_CLOSE                    (1, "command.town.error.close"),
	COMMAND_TOWN_ERROR_ADD_RESIDENT             (2, "command.town.error.add-resident"),
	COMMAND_TOWN_ERROR_ADD_INVITE_RESIDENT      (2, "command.town.error.add-invite-resident"),
	COMMAND_TOWN_ERROR_KICK_FAIL                (2, "command.town.error.kick-fail"),
	COMMAND_TOWN_ERROR_LORD_SELF                (0, "command.town.error.lord-self"),
	COMMAND_TOWN_ERROR_KNIGHT_RESIDENT          (0, "command.town.error.knight-resident"),
	COMMAND_TRAVEL_NOTICE_TOWN_TRAVEL           (2, "command.travel.notice.town-travel"),
	COMMAND_TRAVEL_NOTICE_WILD_TRAVEL           (0, "command.travel.notice.wild-travel"),
	COMMAND_TRAVEL_ERROR_ENEMY_TERRITORY        (0, "command.travel.error.enemy-territory"),
	COMMAND_TRAVEL_ERROR_NO_HOME                (0, "command.travel.error.no-home"),
	COMMAND_TRAVEL_ERROR_NO_CAMP                (0, "command.travel.error.no-camp"),
	COMMAND_TRAVEL_ERROR_NO_TOWN                (0, "command.travel.error.no-town"),
	COMMAND_TRAVEL_ERROR_COOLDOWN               (2, "command.travel.error.cooldown"),
	
	NULL_MESSAGE	(0,"");
	
	private String path;
	private int formats;
	
	MessagePath(int formats, String path) {
		this.path = path;
		this.formats = formats;
	}
	
	public int getFormats() {
		return formats;
	}
	
	public String getPath() {
		return path;
    }
	
	public String getMessage(Object ...args) {
		return Konquest.getInstance().lang().get(this, args);
	}
	
}
