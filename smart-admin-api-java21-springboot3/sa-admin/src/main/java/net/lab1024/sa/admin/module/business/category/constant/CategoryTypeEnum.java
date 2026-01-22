package net.lab1024.sa.admin.module.business.category.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.foundation.domain.enumeration.BaseEnum;

/**
 * 分类类型 枚举
 *
 * @author 1024创新实验室: 胡克
 * @since 2021/08/05 21:26:58 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@AllArgsConstructor
@Getter
public enum CategoryTypeEnum implements BaseEnum {

  /** 1 商品 */
  GOODS(1, "商品"),

  /** 2 自定义 */
  CUSTOM(2, "自定义"),
  ;

  private final Integer value;

  private final String desc;
}
