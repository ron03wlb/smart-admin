package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Risk proposal status enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@AllArgsConstructor
@Getter
public enum RiskProposalStatusEnum implements BaseEnum {
  PENDING(1, "待分配"),
  ASSIGNED(2, "已分配"),
  REVIEWED(3, "已審核"),
  APPROVED(4, "已批准"),
  REJECTED(5, "已拒絕"),
  ESCALATED(6, "已升級"),
  ;

  private final Integer value;
  private final String desc;
}
