package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Agent status enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@AllArgsConstructor
@Getter
public enum AgentStatusEnum implements BaseEnum {
  ACTIVE(1, "活躍"),
  FROZEN(2, "凍結"),
  SUSPENDED(3, "停用"),
  ;

  private final Integer value;
  private final String desc;
}
