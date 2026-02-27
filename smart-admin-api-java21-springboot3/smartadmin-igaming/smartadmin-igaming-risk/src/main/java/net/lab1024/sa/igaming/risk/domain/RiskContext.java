package net.lab1024.sa.igaming.risk.domain;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

/**
 * Shared context passed through LiteFlow risk assessment chain.
 *
 * <p>Each component reads event data and writes its individual score into {@code ruleScores}.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class RiskContext {

  // ---- Input: event metadata ----

  private Long playerId;

  private Long tenantId;

  private String eventType;

  private String eventId;

  private BigDecimal amount;

  private BigDecimal playerBalance;

  // ---- Input: device / geo data ----

  private String deviceId;

  private String ipAddress;

  private String countryCode;

  private Map<String, Object> deviceMetadata;

  // ---- Input: game data ----

  private String gameType;

  private String gameCode;

  // ---- Output: accumulated by components ----

  /** Component ID -> individual score (0-100). */
  private Map<String, Integer> ruleScores = new HashMap<>();

  /**
   * Add a component's individual risk score.
   *
   * @param componentId LiteFlow component ID
   * @param score score value (0-100)
   */
  public void addRuleScore(String componentId, int score) {
    ruleScores.put(componentId, Math.max(0, Math.min(100, score)));
  }

  /**
   * Calculate the weighted total score from all component scores.
   *
   * @param weights component ID -> weight mapping
   * @return weighted total score (0-100)
   */
  public int calculateTotalScore(Map<String, BigDecimal> weights) {
    BigDecimal totalWeightedScore = BigDecimal.ZERO;
    BigDecimal totalWeight = BigDecimal.ZERO;

    for (Map.Entry<String, Integer> entry : ruleScores.entrySet()) {
      BigDecimal weight = weights.getOrDefault(entry.getKey(), BigDecimal.ONE);
      totalWeightedScore =
          totalWeightedScore.add(BigDecimal.valueOf(entry.getValue()).multiply(weight));
      totalWeight = totalWeight.add(weight);
    }

    if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
      return 0;
    }

    return totalWeightedScore.divide(totalWeight, 0, java.math.RoundingMode.HALF_UP).intValue();
  }
}
