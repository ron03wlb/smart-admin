package net.lab1024.sa.igaming.wallet.payment.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * PSP (Payment Service Provider) configuration entity.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_psp")
public class PspEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long pspId;

  private String pspCode;

  private String pspName;

  private String apiBaseUrl;

  private String apiKeyEncrypted;

  private String webhookSecretEncrypted;

  private Boolean enabled;

  private Integer priority;

  private String supportedCurrencies;

  private BigDecimal minDeposit;

  private BigDecimal maxDeposit;

  private BigDecimal minWithdrawal;

  private BigDecimal maxWithdrawal;
}
