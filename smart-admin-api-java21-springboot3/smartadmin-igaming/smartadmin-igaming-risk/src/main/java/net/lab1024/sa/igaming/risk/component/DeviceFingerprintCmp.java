package net.lab1024.sa.igaming.risk.component;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.risk.domain.RiskContext;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;

/**
 * Device fingerprint component — detects device anomalies.
 *
 * <p>Flags new devices, multi-account per device, and emulators. Score range: 0-70.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@LiteflowComponent("deviceFingerprint")
@RequiredArgsConstructor
public class DeviceFingerprintCmp extends NodeComponent {

  private static final String COMPONENT_ID = "deviceFingerprint";
  private static final String DEVICE_PLAYERS_KEY = "risk:device:players:";
  private static final String PLAYER_DEVICES_KEY = "risk:player:devices:";
  private static final int MULTI_ACCOUNT_THRESHOLD = 3;
  private static final Duration DEVICE_TTL = Duration.ofDays(30);

  private final RedissonClient redissonClient;

  @Override
  public void process() throws Exception {
    RiskContext ctx = this.getContextBean(RiskContext.class);
    String deviceId = ctx.getDeviceId();
    Long playerId = ctx.getPlayerId();
    Long tenantId = ctx.getTenantId();

    int score = 0;

    if (deviceId == null || deviceId.isBlank()) {
      // Missing device fingerprint is suspicious
      score = 30;
    } else {
      // Track device -> player mapping
      String deviceKey = DEVICE_PLAYERS_KEY + tenantId + ":" + deviceId;
      RSet<String> devicePlayers = redissonClient.getSet(deviceKey);
      devicePlayers.add(String.valueOf(playerId));
      devicePlayers.expire(DEVICE_TTL);

      // Track player -> device mapping
      String playerKey = PLAYER_DEVICES_KEY + tenantId + ":" + playerId;
      RSet<String> playerDevices = redissonClient.getSet(playerKey);
      boolean isNewDevice = playerDevices.add(deviceId);
      playerDevices.expire(DEVICE_TTL);

      // Multi-account detection
      int accountsOnDevice = devicePlayers.size();
      if (accountsOnDevice >= MULTI_ACCOUNT_THRESHOLD) {
        score = 70;
      } else if (isNewDevice && playerDevices.size() > 1) {
        // New device for existing player
        score = 25;
      }
    }

    log.debug("DeviceFingerprint: playerId={}, deviceId={}, score={}", playerId, deviceId, score);
    ctx.addRuleScore(COMPONENT_ID, score);
  }
}
