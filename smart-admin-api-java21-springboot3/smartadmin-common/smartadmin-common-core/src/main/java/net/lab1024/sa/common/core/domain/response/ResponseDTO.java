package net.lab1024.sa.common.core.domain.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import net.lab1024.sa.common.core.domain.code.ErrorCode;
import net.lab1024.sa.common.core.domain.code.SystemErrorCode;
import net.lab1024.sa.common.core.domain.code.UnexpectedErrorCode;
import net.lab1024.sa.common.core.domain.code.UserErrorCode;
import net.lab1024.sa.common.core.domain.enumeration.DataTypeEnum;
import org.apache.commons.lang3.StringUtils;

/**
 * 请求返回对象
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2021-10-31 21:06:11 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@Schema
public class ResponseDTO<T> {

  public static final int OK_CODE = 0;

  public static final String OK_MSG = "操作成功";

  @Schema(description = "返回码")
  private Integer code;

  @Schema(description = "级别")
  private String level;

  private String msg;

  private Boolean success;

  @Schema(description = "返回数据")
  private T data;

  @Schema(description = "数据类型")
  private Integer dataType;

  @SuppressWarnings("PMD.BooleanGetMethodName") // 框架 API 设计
  public Boolean getOk() {
    return success;
  }

  public ResponseDTO(Integer code, String level, boolean success, String msg, T data) {
    this.code = code;
    this.level = level;
    this.success = success;
    this.msg = msg;
    this.data = data;
    this.dataType = DataTypeEnum.NORMAL.getValue();
  }

  public ResponseDTO(Integer code, String level, boolean success, String msg) {
    this.code = code;
    this.level = level;
    this.success = success;
    this.msg = msg;
    this.dataType = DataTypeEnum.NORMAL.getValue();
  }

  public ResponseDTO(ErrorCode errorCode, boolean success, String msg, T data) {
    this.code = errorCode.getCode();
    this.level = errorCode.getLevel();
    this.success = success;
    if (StringUtils.isNotBlank(msg)) {
      this.msg = msg;
    } else {
      this.msg = errorCode.getMsg();
    }
    this.data = data;
    this.dataType = DataTypeEnum.NORMAL.getValue();
  }

  @SuppressWarnings("PMD.ShortMethodName") // SmartAdmin 核心 API
  public static <T> ResponseDTO<T> ok() {
    return new ResponseDTO<>(OK_CODE, null, true, OK_MSG, null);
  }

  @SuppressWarnings("PMD.ShortMethodName") // SmartAdmin 核心 API
  public static <T> ResponseDTO<T> ok(T data) {
    return new ResponseDTO<>(OK_CODE, null, true, OK_MSG, data);
  }

  public static <T> ResponseDTO<T> okMsg(String msg) {
    return new ResponseDTO<>(OK_CODE, null, true, msg, null);
  }

  // -------------------------------------------- 最常用的 用户参数 错误码
  // --------------------------------------------

  public static <T> ResponseDTO<T> userErrorParam() {
    return new ResponseDTO<>(UserErrorCode.PARAM_ERROR, false, null, null);
  }

  public static <T> ResponseDTO<T> userErrorParam(String msg) {
    return new ResponseDTO<>(UserErrorCode.PARAM_ERROR, false, msg, null);
  }

  // -------------------------------------------- 错误码
  // --------------------------------------------

  public static <T> ResponseDTO<T> error(ErrorCode errorCode) {
    return new ResponseDTO<>(errorCode, false, null, null);
  }

  /**
   * 根據錯誤碼等級生成帶有等級前綴的錯誤消息
   *
   * <p>Java 21 Switch Expression + Sealed Interface: 編譯器保證所有分支都已覆蓋（exhaustiveness check）
   *
   * @param errorCode 錯誤碼
   * @param <T> 返回數據類型
   * @return ResponseDTO 實例
   */
  public static <T> ResponseDTO<T> errorWithLevelPrefix(ErrorCode errorCode) {
    String prefixedMsg =
        switch (errorCode) {
          case SystemErrorCode se -> "[系統錯誤] " + se.getMsg();
          case UserErrorCode ue -> "[用戶錯誤] " + ue.getMsg();
          case UnexpectedErrorCode une -> "[未預期錯誤] " + une.getMsg();
            // 編譯器保證所有 ErrorCode 子類型都已處理，無需 default 分支
        };
    return new ResponseDTO<>(errorCode, false, prefixedMsg, null);
  }

  public static <T> ResponseDTO<T> error(ErrorCode errorCode, boolean success) {
    return new ResponseDTO<>(errorCode, success, null, null);
  }

  public static <T> ResponseDTO<T> error(ResponseDTO<?> responseDTO) {
    return new ResponseDTO<>(
        responseDTO.getCode(),
        responseDTO.getLevel(),
        responseDTO.getSuccess(),
        responseDTO.getMsg(),
        null);
  }

  public static <T> ResponseDTO<T> error(ErrorCode errorCode, String msg) {
    return new ResponseDTO<>(errorCode, false, msg, null);
  }

  public static <T> ResponseDTO<T> errorData(ErrorCode errorCode, T data) {
    return new ResponseDTO<>(errorCode, false, null, data);
  }
}
