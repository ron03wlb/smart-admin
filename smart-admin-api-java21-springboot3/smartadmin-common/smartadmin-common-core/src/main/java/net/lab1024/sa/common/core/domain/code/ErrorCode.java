package net.lab1024.sa.common.core.domain.code;

/**
 * 错误码<br>
 * 一共分为三种： 1）系统错误、2）用户级别错误、3）未预期到的错误
 *
 * <p>Java 21 Sealed Interface: 仅允许 SystemErrorCode, UserErrorCode, UnexpectedErrorCode 实现此接口，
 * 提供编译时类型安全保障。
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2021-09-02 20:21:10 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public sealed interface ErrorCode permits SystemErrorCode, UserErrorCode, UnexpectedErrorCode {

  /** 系统等级 */
  String LEVEL_SYSTEM = "system";

  /** 用户等级 */
  String LEVEL_USER = "user";

  /** 未预期到的等级 */
  String LEVEL_UNEXPECTED = "unexpected";

  /** 错误码 */
  int getCode();

  /** 错误消息 */
  String getMsg();

  /** 错误等级 */
  String getLevel();
}
