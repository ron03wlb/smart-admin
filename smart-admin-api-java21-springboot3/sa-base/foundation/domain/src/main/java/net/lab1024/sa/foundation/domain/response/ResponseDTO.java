package net.lab1024.sa.foundation.domain.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import net.lab1024.sa.foundation.domain.code.ErrorCode;
import net.lab1024.sa.foundation.domain.code.UserErrorCode;
import net.lab1024.sa.foundation.domain.enumeration.DataTypeEnum;
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
