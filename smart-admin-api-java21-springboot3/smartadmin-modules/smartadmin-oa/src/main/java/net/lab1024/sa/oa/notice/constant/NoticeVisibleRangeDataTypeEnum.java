package net.lab1024.sa.oa.notice.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * 公告、通知 可见范围类型
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-12 21:40:39 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Getter
@AllArgsConstructor
public enum NoticeVisibleRangeDataTypeEnum implements BaseEnum {

  /** 员工 */
  EMPLOYEE(1, "员工"),

  /** 部门 */
  DEPARTMENT(2, "部门"),
  ;

  private final Integer value;

  private final String desc;
}
