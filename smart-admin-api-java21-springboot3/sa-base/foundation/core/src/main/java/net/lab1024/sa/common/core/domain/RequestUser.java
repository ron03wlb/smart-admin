package net.lab1024.sa.common.core.domain;

import net.lab1024.sa.common.core.enumeration.UserTypeEnum;

/**
 * 请求用户
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2021-12-21 19:55:07 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public interface RequestUser {

  /**
   * 请求用户id
   *
   * @return Long
   */
  Long getUserId();

  /**
   * 请求用户名称
   *
   * @return String
   */
  String getUserName();

  /** 获取用户类型 */
  UserTypeEnum getUserType();

  /**
   * 获取请求的IP
   *
   * @return String
   */
  String getIp();

  /**
   * 获取请求 user-agent
   *
   * @return String
   */
  String getUserAgent();
}
