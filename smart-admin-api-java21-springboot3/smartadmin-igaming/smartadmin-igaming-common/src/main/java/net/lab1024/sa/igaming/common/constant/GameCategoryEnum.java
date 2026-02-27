package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Game category enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@AllArgsConstructor
@Getter
public enum GameCategoryEnum implements BaseEnum {
  SLOTS(1, "老虎機"),
  LIVE_CASINO(2, "真人娛樂城"),
  SPORTS(3, "體育"),
  POKER(4, "撲克"),
  TABLE_GAMES(5, "桌遊"),
  LOTTERY(6, "彩票"),
  ;

  private final Integer value;
  private final String desc;
}
