package net.lab1024.sa.igaming.risk.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Risk rule parameter configuration entity.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_risk_rule_param")
public class RiskRuleParamEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long ruleParamId;

  /** Rule type. See {@link net.lab1024.sa.igaming.common.constant.RiskRuleTypeEnum}. */
  private Integer ruleType;

  private String ruleName;

  private String ruleDescription;

  /** Threshold value DECIMAL(19,4). */
  private BigDecimal thresholdValue;

  /** Time window in seconds. */
  private Integer timeWindowSeconds;

  private Integer maxCount;

  /** Rule weight DECIMAL(8,4). */
  private BigDecimal weight;

  private Boolean enabled;

  /** Extra parameters stored as JSONB. */
  private String paramsJson;

  private Boolean deleted;

  @Version private Integer version;
}
