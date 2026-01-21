package net.lab1024.sa.common.apiencrypt.util;

/**
 * API 加密模組字串工具類
 *
 * @author 1024创新实验室-主任:卓大
 * @since 2023/10/21 11:41:46 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public final class EncryptStringUtil {

  private EncryptStringUtil() {
    // 防止實例化
  }

  /**
   * 判斷字串是否為空
   *
   * @param str 字串
   * @return 是否為空
   */
  public static boolean isEmpty(CharSequence str) {
    return str == null || str.isEmpty();
  }

  /**
   * 判斷字串是否不為空
   *
   * @param str 字串
   * @return 是否不為空
   */
  public static boolean isNotEmpty(CharSequence str) {
    return !isEmpty(str);
  }
}
