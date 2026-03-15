package net.lab1024.sa.igaming.activity.turnover.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Risk action type enumeration.
 *
 * <p>Defines actions taken by the risk engine when a player's risk level exceeds certain
 * thresholds. Each action has different effects on turnover calculation and betting permissions.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@AllArgsConstructor
@Getter
public enum RiskActionTypeEnum implements BaseEnum {
  /**
   * PASS: Normal operation.
   *
   * <ul>
   *   <li>Turnover Factor: 100%
   *   <li>Allow Bet: Yes
   *   <li>Create Proposal: No
   * </ul>
   */
  PASS(1, "通過"),

  /**
   * FLAG: Mark for manual review.
   *
   * <ul>
   *   <li>Turnover Factor: 100% (still counts toward turnover)
   *   <li>Allow Bet: Yes
   *   <li>Create Proposal: Yes (alert risk team)
   * </ul>
   */
  FLAG(2, "標記"),

  /**
   * BLOCK: Prevent betting and exclude from turnover.
   *
   * <ul>
   *   <li>Turnover Factor: 0% (does not count toward turnover)
   *   <li>Allow Bet: No
   *   <li>Create Proposal: No (already blocked)
   * </ul>
   */
  BLOCK(3, "阻擋"),
  ;

  private final Integer value;
  private final String desc;
}
