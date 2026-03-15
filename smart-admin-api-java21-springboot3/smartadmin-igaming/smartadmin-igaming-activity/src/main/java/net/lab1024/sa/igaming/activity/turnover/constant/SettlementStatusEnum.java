package net.lab1024.sa.igaming.activity.turnover.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Settlement status enumeration.
 *
 * <p>Represents the final settlement status of a game round, which determines whether the bet
 * counts toward valid turnover calculation.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@AllArgsConstructor
@Getter
public enum SettlementStatusEnum implements BaseEnum {
  WIN(1, "贏"),
  LOSS(2, "輸"),
  HALF_WIN(3, "半贏"),
  HALF_LOSS(4, "半輸"),
  DRAW(5, "平局"),
  TIE(6, "和局"),
  VOID(7, "作廢"),
  CANCEL(8, "取消"),
  RUNNING(9, "進行中"),
  ;

  private final Integer value;
  private final String desc;
}
