package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Risk decision enumeration — outcome of risk evaluation.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@AllArgsConstructor
@Getter
public enum RiskDecisionEnum implements BaseEnum {
  AUTO_APPROVED(1, "自動通過"),
  PENDING_REVIEW(2, "待人工審核"),
  AUTO_REJECTED(3, "自動拒絕"),
  ;

  private final Integer value;
  private final String desc;
}
