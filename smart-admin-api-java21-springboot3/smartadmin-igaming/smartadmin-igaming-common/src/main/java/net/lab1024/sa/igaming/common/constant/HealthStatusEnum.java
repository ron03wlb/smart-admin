package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Game provider health status enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@AllArgsConstructor
@Getter
public enum HealthStatusEnum implements BaseEnum {
  HEALTHY(1, "健康"),
  DEGRADED(2, "降級"),
  DOWN(3, "下線"),
  ;

  private final Integer value;
  private final String desc;
}
