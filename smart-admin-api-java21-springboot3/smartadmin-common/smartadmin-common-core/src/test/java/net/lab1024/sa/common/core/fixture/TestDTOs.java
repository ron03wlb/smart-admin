package net.lab1024.sa.common.core.fixture;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 測試用 DTO 類別
 *
 * <p>提供 SmartBeanUtil 和其他工具類測試所需的源對象和目標對象
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
public final class TestDTOs {

  private TestDTOs() {}

  /** 源對象 - 用於測試 Bean 複製 */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SourceDTO {
    private Long id;
    private String name;
    private Integer age;
    private String email;
  }

  /** 目標對象 - 用於測試 Bean 複製 */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TargetVO {
    private Long id;
    private String name;
    private Integer age;
    private String email;
  }

  /** 帶驗證註解的對象 - 用於測試 verify() 方法 */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ValidatedDTO {
    @NotNull(message = "ID不能為空")
    private Long id;

    @NotBlank(message = "名稱不能為空")
    @Size(min = 2, max = 50, message = "名稱長度必須在2-50之間")
    private String name;

    @NotNull(message = "年齡不能為空")
    private Integer age;
  }

  /** 類型不匹配的目標對象 - 用於測試類型不匹配時的行為 */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MismatchedVO {
    private String id; // Long -> String 類型不匹配
    private String name;
    private String age; // Integer -> String 類型不匹配
  }
}
