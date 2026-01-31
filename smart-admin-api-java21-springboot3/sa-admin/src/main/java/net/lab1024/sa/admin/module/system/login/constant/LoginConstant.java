package net.lab1024.sa.admin.module.system.login.constant;

/**
 * 登錄相關常數定義
 *
 * <p>定義登錄模塊使用的所有時間、長度等常數值，避免魔法數字
 *
 * @author Claude Sonnet 4.5
 * @since 2026-01-30
 */
public final class LoginConstant {

  private LoginConstant() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  // ==================== 登錄超時時間 ====================

  /** 萬能密碼登錄超時時間（秒）- 30分鐘 */
  public static final int SUPER_PASSWORD_LOGIN_TIMEOUT_SECONDS = 1800;

  // ==================== 驗證碼相關 ====================

  /** 驗證碼發送間隔（毫秒）- 1分鐘 */
  public static final long VERIFICATION_CODE_SEND_INTERVAL_MILLIS = 60 * 1000;

  /** 驗證碼過期時間（秒）- 5分鐘 */
  public static final int VERIFICATION_CODE_EXPIRE_SECONDS = 300;

  /** 驗證碼長度 - 4位數字 */
  public static final int VERIFICATION_CODE_LENGTH = 4;

  // ==================== 登錄名驗證規則 ====================

  /** 登錄名最小長度 */
  public static final int LOGIN_NAME_MIN_LENGTH = 3;

  /** 登錄名最大長度 */
  public static final int LOGIN_NAME_MAX_LENGTH = 50;

  /** 登錄名驗證正則表達式 - 僅包含字母、數字、下劃線和連字符 */
  public static final String LOGIN_NAME_PATTERN = "^[a-zA-Z0-9_-]{3,50}$";

  // ==================== 提示消息 ====================

  /** 验证码发送频率限制提示 */
  public static final String VERIFICATION_CODE_SEND_TOO_FREQUENTLY = "邮箱验证码已发送，一分钟内请勿重复发送";

  /** 登录名格式错误提示 */
  public static final String LOGIN_NAME_FORMAT_ERROR = "登录名必须为 3-50 个字符，仅包含字母、数字、下划线和连字符";
}
