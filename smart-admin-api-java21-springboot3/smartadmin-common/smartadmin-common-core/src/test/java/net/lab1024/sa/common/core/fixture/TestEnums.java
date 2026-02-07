package net.lab1024.sa.common.core.fixture;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * 測試用枚舉類別
 *
 * <p>提供 SmartEnumUtil 測試所需的枚舉類
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
public final class TestEnums {

  private TestEnums() {}

  /** 性別枚舉 - 用於測試 BaseEnum 相關功能 */
  @Getter
  @AllArgsConstructor
  public enum GenderEnum implements BaseEnum {
    MALE(1, "男"),
    FEMALE(2, "女"),
    UNKNOWN(0, "未知");

    private final Integer value;
    private final String desc;

    @Override
    public Object getValue() {
      return value;
    }
  }

  /** 狀態枚舉 - 用於測試帶有更多選項的枚舉 */
  @Getter
  @AllArgsConstructor
  public enum StatusEnum implements BaseEnum {
    DRAFT(0, "草稿"),
    PENDING(1, "待審核"),
    APPROVED(2, "已審核"),
    REJECTED(3, "已拒絕");

    private final Integer value;
    private final String desc;

    @Override
    public Object getValue() {
      return value;
    }
  }
}
