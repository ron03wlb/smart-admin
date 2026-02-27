package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * KYC document verification status enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@AllArgsConstructor
@Getter
public enum KycVerificationStatusEnum implements BaseEnum {
  PENDING(1, "待審核"),
  APPROVED(2, "通過"),
  REJECTED(3, "拒絕"),
  ;

  private final Integer value;
  private final String desc;
}
