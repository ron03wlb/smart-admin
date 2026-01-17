package net.lab1024.sa.admin.module.system.datascope.strategy;

import java.util.Map;
import net.lab1024.sa.admin.module.system.datascope.constant.DataScopeViewTypeEnum;
import net.lab1024.sa.admin.module.system.datascope.domain.DataScopeSqlConfig;

/**
 * 数据范围策略 ,使用DataScopeWhereInTypeEnum.CUSTOM_STRATEGY类型，DataScope注解的joinSql属性无用
 *
 * @author 1024创新实验室: 罗伊
 * @since 2020/11/28 20:59:17 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public abstract class AbstractDataScopeStrategy {

  /** 获取joinsql 字符串 */
  public abstract String getCondition(
      DataScopeViewTypeEnum viewTypeEnum,
      Map<String, Object> paramMap,
      DataScopeSqlConfig sqlConfigDTO);
}
