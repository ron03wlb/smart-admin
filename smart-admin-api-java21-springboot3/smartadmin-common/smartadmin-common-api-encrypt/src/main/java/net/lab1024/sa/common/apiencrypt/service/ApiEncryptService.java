package net.lab1024.sa.common.apiencrypt.service;

/**
 * 接口加密、解密 Service
 *
 * @author 1024创新实验室-主任:卓大
 * @since 2023/10/21 11:41:46 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public interface ApiEncryptService {

  /**
   * 解密
   *
   * @param data 加密數據
   * @return 解密後的數據
   */
  String decrypt(String data);

  /**
   * 加密
   *
   * @param data 原始數據
   * @return 加密後的數據
   */
  String encrypt(String data);
}
