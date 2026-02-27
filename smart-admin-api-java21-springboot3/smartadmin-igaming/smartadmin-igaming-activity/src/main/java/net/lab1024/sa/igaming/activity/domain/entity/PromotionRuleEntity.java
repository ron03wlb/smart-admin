package net.lab1024.sa.igaming.activity.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Promotion rule entity.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_promotion_rule")
public class PromotionRuleEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long ruleId;

  private String promotionCode;

  private String promotionName;

  /** See {@link net.lab1024.sa.igaming.common.constant.PromotionTypeEnum}. */
  private Integer promotionType;

  /** See {@link net.lab1024.sa.igaming.common.constant.PromotionStatusEnum}. */
  private Integer status;

  private OffsetDateTime startTime;

  private OffsetDateTime endTime;

  private BigDecimal minDeposit;

  /** Bonus rate (1.0000 = 100%). */
  private BigDecimal bonusRate;

  private BigDecimal maxBonus;

  /** Wagering multiplier (e.g. 20.00 = 20x turnover). */
  private BigDecimal wageringMultiplier;

  private Integer maxClaimsPerPlayer;

  private Integer bonusExpiryDays;

  /** Game restriction rules as JSONB string. */
  private String gameRestriction;

  private Boolean deleted;

  @Version private Integer version;
}
