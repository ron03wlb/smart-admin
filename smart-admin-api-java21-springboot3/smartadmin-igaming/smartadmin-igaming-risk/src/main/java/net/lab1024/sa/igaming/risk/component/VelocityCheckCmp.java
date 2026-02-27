package net.lab1024.sa.igaming.risk.component;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.risk.domain.RiskContext;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;

/**
 * Velocity check component — Redis sliding window rate limiting.
 *
 * <p>Flags high-frequency transactions (e.g., bets > 100/min, withdrawals > 5/hour). Score range:
 * 0-50.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@LiteflowComponent("velocityCheck")
@RequiredArgsConstructor
public class VelocityCheckCmp extends NodeComponent {

  private static final String COMPONENT_ID = "velocityCheck";
  private static final String KEY_PREFIX = "risk:velocity:";
  private static final long WINDOW_SECONDS = 60;
  private static final int HIGH_FREQUENCY_THRESHOLD = 100;
  private static final int MEDIUM_FREQUENCY_THRESHOLD = 50;

  private final RedissonClient redissonClient;

  @Override
  public void process() throws Exception {
    RiskContext ctx = this.getContextBean(RiskContext.class);
    Long playerId = ctx.getPlayerId();
    Long tenantId = ctx.getTenantId();

    String key = KEY_PREFIX + tenantId + ":" + playerId;
    long now = System.currentTimeMillis();
    long windowStart = now - (WINDOW_SECONDS * 1000);

    RScoredSortedSet<String> sortedSet = redissonClient.getScoredSortedSet(key);
    sortedSet.removeRangeByScore(0, true, (double) windowStart, true);
    sortedSet.add((double) now, ctx.getEventId());
    sortedSet.expire(java.time.Duration.ofSeconds(WINDOW_SECONDS * 2));

    int count = sortedSet.size();
    int score = calculateScore(count);

    log.debug("VelocityCheck: playerId={}, count={}, score={}", playerId, count, score);
    ctx.addRuleScore(COMPONENT_ID, score);
  }

  private int calculateScore(int count) {
    if (count >= HIGH_FREQUENCY_THRESHOLD) {
      return 50;
    } else if (count >= MEDIUM_FREQUENCY_THRESHOLD) {
      return 30;
    } else if (count >= 20) {
      return 15;
    }
    return 0;
  }
}
