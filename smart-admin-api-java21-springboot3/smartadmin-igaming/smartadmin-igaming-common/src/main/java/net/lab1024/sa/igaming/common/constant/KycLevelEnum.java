package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * KYC verification level enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@AllArgsConstructor
@Getter
public enum KycLevelEnum implements BaseEnum {
  L0(0, "基礎驗證"),
  L1(1, "文件驗證"),
  L2(2, "完整驗證"),
  ;

  private final Integer value;
  private final String desc;
}
