package net.lab1024.sa.support.reload.core.domain;

import lombok.Data;

/**
 * reload项目
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2015-03-02 19:11:52 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class SmartReloadItem {

  /** 项名称 */
  private String tag;

  /** 参数 */
  private String args;

  /** 标识 */
  private String identification;
}
