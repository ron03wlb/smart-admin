package net.lab1024.sa.common.core.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import net.lab1024.sa.common.core.domain.code.UserErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * BusinessException 單元測試
 *
 * <p>測試覆蓋所有構造函數
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@DisplayName("BusinessException 單元測試")
class BusinessExceptionTest {

  @Nested
  @DisplayName("構造函數測試")
  class ConstructorTest {

    @Test
    @DisplayName("默認構造函數：應該創建空異常")
    void defaultConstructor_shouldCreateEmpty() {
      // When
      BusinessException exception = new BusinessException();

      // Then
      assertThat(exception.getMessage()).isNull();
      assertThat(exception.getCode()).isNull();
    }

    @Test
    @DisplayName("ErrorCode 構造函數：應該設置 code 和 message")
    void constructor_shouldCreateFromErrorCode() {
      // When
      BusinessException exception = new BusinessException(UserErrorCode.PARAM_ERROR);

      // Then
      assertThat(exception.getCode()).isEqualTo(30001);
      assertThat(exception.getMessage()).isEqualTo("参数错误");
    }

    @Test
    @DisplayName("String 構造函數：應該設置 message")
    void constructor_shouldCreateFromMessage() {
      // When
      BusinessException exception = new BusinessException("自定義錯誤消息");

      // Then
      assertThat(exception.getMessage()).isEqualTo("自定義錯誤消息");
      assertThat(exception.getCode()).isNull();
    }

    @Test
    @DisplayName("String + Throwable 構造函數：應該設置 message 和 cause")
    void constructor_shouldCreateFromMessageAndCause() {
      // Given
      RuntimeException cause = new RuntimeException("原始異常");

      // When
      BusinessException exception = new BusinessException("包裝異常", cause);

      // Then
      assertThat(exception.getMessage()).isEqualTo("包裝異常");
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("Throwable 構造函數：應該設置 cause")
    void constructor_shouldCreateFromCause() {
      // Given
      RuntimeException cause = new RuntimeException("原始異常");

      // When
      BusinessException exception = new BusinessException(cause);

      // Then
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("完整參數構造函數：應該正確設置所有參數")
    void constructor_shouldCreateWithAllParams() {
      // Given
      RuntimeException cause = new RuntimeException("原始異常");

      // When
      BusinessException exception = new BusinessException("完整異常", cause, true, true);

      // Then
      assertThat(exception.getMessage()).isEqualTo("完整異常");
      assertThat(exception.getCause()).isEqualTo(cause);
    }
  }
}
