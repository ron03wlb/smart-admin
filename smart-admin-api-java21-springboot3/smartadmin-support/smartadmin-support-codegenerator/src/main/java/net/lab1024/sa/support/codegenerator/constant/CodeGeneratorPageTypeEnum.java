package net.lab1024.sa.support.codegenerator.constant;

import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * 页面类型
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-06-29 19:11:22 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public enum CodeGeneratorPageTypeEnum implements BaseEnum {
  MODAL("modal", "弹窗"),
  DRAWER("drawer", "抽屉"),
  PAGE("page", "新页面");

  private String value;

  private String desc;

  CodeGeneratorPageTypeEnum(String value, String desc) {
    this.value = value;
    this.desc = desc;
  }

  @Override
  public Object getValue() {
    return value;
  }

  @Override
  public String getDesc() {
    return desc;
  }
}
