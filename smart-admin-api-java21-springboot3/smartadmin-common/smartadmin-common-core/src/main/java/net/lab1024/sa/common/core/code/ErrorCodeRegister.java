package net.lab1024.sa.common.core.code;

import static net.lab1024.sa.common.core.code.ErrorCodeRangeContainer.register;

import net.lab1024.sa.common.core.domain.code.SystemErrorCode;
import net.lab1024.sa.common.core.domain.code.UnexpectedErrorCode;
import net.lab1024.sa.common.core.domain.code.UserErrorCode;

/**
 * 注册code状态码 <br>
 * ps：为什么要在此处不那么优雅的手动注册？ 主要是为了能统一、清晰、浏览当前定义的所有状态码 方便后续维护
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2021/09/27 23:09 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public class ErrorCodeRegister {

  static {

    // 系统 错误码
    register(SystemErrorCode.class, 10001, 20000);

    // 意外 错误码
    register(UnexpectedErrorCode.class, 20001, 30000);

    // 用户 通用错误码 (包含 MFA 錯誤碼 40100-40199)
    register(UserErrorCode.class, 30001, 40200);
  }

  public static int initialize() {
    return ErrorCodeRangeContainer.initialize();
  }

  public static void main(String[] args) {
    ErrorCodeRegister.initialize();
  }
}
