package net.lab1024.sa.igaming.game.domain.vo;

import java.math.BigDecimal;
import lombok.Data;

/**
 * GP callback response VO — returned to GP after debit/credit/rollback.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class CallbackResponseVO {

  private String transactionId;
  private BigDecimal balance;
  private Integer status;
}
