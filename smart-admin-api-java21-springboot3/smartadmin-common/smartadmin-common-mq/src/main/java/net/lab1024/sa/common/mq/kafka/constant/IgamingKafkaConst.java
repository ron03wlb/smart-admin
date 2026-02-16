package net.lab1024.sa.common.mq.kafka.constant;

/**
 * iGaming Kafka topic and consumer group constants
 *
 * @author iGaming Team
 * @since 2026-02-14
 */
public final class IgamingKafkaConst {

  private IgamingKafkaConst() {}

  /** Topic constants */
  public static final class Topic {

    public static final String WALLET_EVENTS = "igaming.wallet.events";
    public static final String PLAYER_EVENTS = "igaming.player.events";
    public static final String RISK_EVENTS = "igaming.risk.events";
    public static final String GAME_EVENTS = "igaming.game.events";
    public static final String ACTIVITY_EVENTS = "igaming.activity.events";
    public static final String AUDIT_EVENTS = "igaming.audit.events";

    private Topic() {}
  }

  /** Consumer group constants */
  public static final class Group {

    public static final String WALLET = "igaming-wallet-group";
    public static final String RISK = "igaming-risk-group";
    public static final String ACTIVITY = "igaming-activity-group";
    public static final String AUDIT = "igaming-audit-group";
    public static final String RECONCILIATION = "igaming-reconciliation-group";
    public static final String ANALYTICS = "igaming-analytics-group";

    private Group() {}
  }
}
