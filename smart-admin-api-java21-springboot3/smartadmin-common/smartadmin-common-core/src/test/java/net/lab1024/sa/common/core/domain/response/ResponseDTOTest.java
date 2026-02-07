package net.lab1024.sa.common.core.domain.response;

import static org.assertj.core.api.Assertions.assertThat;

import net.lab1024.sa.common.core.domain.code.SystemErrorCode;
import net.lab1024.sa.common.core.domain.code.UnexpectedErrorCode;
import net.lab1024.sa.common.core.domain.code.UserErrorCode;
import net.lab1024.sa.common.core.domain.enumeration.DataTypeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ResponseDTO 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>ok() - 成功響應工廠方法
 *   <li>error() - 錯誤響應工廠方法
 *   <li>errorWithLevelPrefix() - Java 21 Sealed Switch 測試
 *   <li>userErrorParam() - 參數錯誤快捷方法
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@DisplayName("ResponseDTO 單元測試")
class ResponseDTOTest {

  // ==================== ok() 工廠方法測試 ====================

  @Nested
  @DisplayName("ok 成功響應測試")
  class OkTest {

    @Test
    @DisplayName("ok()：無參數時返回成功響應，data 為 null")
    void ok_shouldReturnSuccessWithNullData() {
      // When
      ResponseDTO<Void> result = ResponseDTO.ok();

      // Then
      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getOk()).isTrue();
      assertThat(result.getCode()).isEqualTo(ResponseDTO.OK_CODE);
      assertThat(result.getMsg()).isEqualTo(ResponseDTO.OK_MSG);
      assertThat(result.getData()).isNull();
      assertThat(result.getDataType()).isEqualTo(DataTypeEnum.NORMAL.getValue());
    }

    @Test
    @DisplayName("ok(data)：帶數據時返回成功響應")
    void ok_shouldReturnSuccessWithData() {
      // Given
      String testData = "測試數據";

      // When
      ResponseDTO<String> result = ResponseDTO.ok(testData);

      // Then
      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getCode()).isEqualTo(ResponseDTO.OK_CODE);
      assertThat(result.getData()).isEqualTo("測試數據");
    }

    @Test
    @DisplayName("okMsg()：自定義成功消息")
    void okMsg_shouldReturnCustomMessage() {
      // Given
      String customMsg = "操作已完成";

      // When
      ResponseDTO<Void> result = ResponseDTO.okMsg(customMsg);

      // Then
      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getMsg()).isEqualTo("操作已完成");
      assertThat(result.getData()).isNull();
    }
  }

  // ==================== error() 工廠方法測試 ====================

  @Nested
  @DisplayName("error 錯誤響應測試")
  class ErrorTest {

    @Test
    @DisplayName("error(ErrorCode)：使用 ErrorCode 創建錯誤響應")
    void error_shouldReturnErrorWithCode() {
      // When
      ResponseDTO<Void> result = ResponseDTO.error(UserErrorCode.PARAM_ERROR);

      // Then
      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getCode()).isEqualTo(30001);
      assertThat(result.getMsg()).isEqualTo("参数错误");
      assertThat(result.getLevel()).isEqualTo("user");
    }

    @Test
    @DisplayName("error(ErrorCode, msg)：自定義錯誤消息覆蓋默認")
    void error_shouldOverrideDefaultMessage() {
      // When
      ResponseDTO<Void> result = ResponseDTO.error(UserErrorCode.PARAM_ERROR, "自定義錯誤消息");

      // Then
      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getCode()).isEqualTo(30001);
      assertThat(result.getMsg()).isEqualTo("自定義錯誤消息");
    }

    @Test
    @DisplayName("errorData()：錯誤響應帶數據")
    void errorData_shouldReturnErrorWithData() {
      // Given
      String errorDetail = "詳細錯誤信息";

      // When
      ResponseDTO<String> result = ResponseDTO.errorData(UserErrorCode.PARAM_ERROR, errorDetail);

      // Then
      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getData()).isEqualTo("詳細錯誤信息");
    }

    @Test
    @DisplayName("error(ResponseDTO)：從其他 ResponseDTO 創建錯誤響應")
    void error_shouldCopyFromAnotherResponseDTO() {
      // Given
      ResponseDTO<String> original = ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);

      // When
      ResponseDTO<Integer> result = ResponseDTO.error(original);

      // Then
      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getCode()).isEqualTo(original.getCode());
      assertThat(result.getMsg()).isEqualTo(original.getMsg());
      assertThat(result.getData()).isNull();
    }

    @Test
    @DisplayName("error(ErrorCode, success)：可以設置 success 為 true（特殊情況）")
    void error_shouldAllowSuccessTrue() {
      // When
      ResponseDTO<Void> result = ResponseDTO.error(UserErrorCode.PARAM_ERROR, true);

      // Then
      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getCode()).isEqualTo(30001);
    }
  }

  // ==================== errorWithLevelPrefix() Java 21 Switch 測試 ====================

  @Nested
  @DisplayName("errorWithLevelPrefix Java 21 Sealed Switch 測試")
  class ErrorWithLevelPrefixTest {

    @Test
    @DisplayName("SystemErrorCode：應該添加 [系統錯誤] 前綴")
    void shouldAddSystemPrefix() {
      // When
      ResponseDTO<Void> result = ResponseDTO.errorWithLevelPrefix(SystemErrorCode.SYSTEM_ERROR);

      // Then
      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getMsg()).startsWith("[系統錯誤]");
      assertThat(result.getMsg()).contains("系统似乎出现了点小问题");
    }

    @Test
    @DisplayName("UserErrorCode：應該添加 [用戶錯誤] 前綴")
    void shouldAddUserPrefix() {
      // When
      ResponseDTO<Void> result = ResponseDTO.errorWithLevelPrefix(UserErrorCode.PARAM_ERROR);

      // Then
      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getMsg()).startsWith("[用戶錯誤]");
      assertThat(result.getMsg()).contains("参数错误");
    }

    @Test
    @DisplayName("UnexpectedErrorCode：應該添加 [未預期錯誤] 前綴")
    void shouldAddUnexpectedPrefix() {
      // When
      ResponseDTO<Void> result =
          ResponseDTO.errorWithLevelPrefix(UnexpectedErrorCode.BUSINESS_HANDING);

      // Then
      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getMsg()).startsWith("[未預期錯誤]");
      assertThat(result.getMsg()).contains("业务繁忙");
    }
  }

  // ==================== userErrorParam() 測試 ====================

  @Nested
  @DisplayName("userErrorParam 參數錯誤快捷方法測試")
  class UserErrorParamTest {

    @Test
    @DisplayName("userErrorParam()：返回參數錯誤響應")
    void userErrorParam_shouldReturnParamError() {
      // When
      ResponseDTO<Void> result = ResponseDTO.userErrorParam();

      // Then
      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getCode()).isEqualTo(UserErrorCode.PARAM_ERROR.getCode());
      assertThat(result.getMsg()).isEqualTo(UserErrorCode.PARAM_ERROR.getMsg());
    }

    @Test
    @DisplayName("userErrorParam(msg)：返回帶自定義消息的參數錯誤響應")
    void userErrorParam_shouldReturnCustomMessage() {
      // When
      ResponseDTO<Void> result = ResponseDTO.userErrorParam("用戶名不能為空");

      // Then
      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getCode()).isEqualTo(UserErrorCode.PARAM_ERROR.getCode());
      assertThat(result.getMsg()).isEqualTo("用戶名不能為空");
    }
  }

  // ==================== dataType 測試 ====================

  @Nested
  @DisplayName("dataType 數據類型測試")
  class DataTypeTest {

    @Test
    @DisplayName("默認 dataType 應該為 NORMAL")
    void dataType_shouldDefaultToNormal() {
      // When
      ResponseDTO<Void> result = ResponseDTO.ok();

      // Then
      assertThat(result.getDataType()).isEqualTo(DataTypeEnum.NORMAL.getValue());
    }
  }
}
