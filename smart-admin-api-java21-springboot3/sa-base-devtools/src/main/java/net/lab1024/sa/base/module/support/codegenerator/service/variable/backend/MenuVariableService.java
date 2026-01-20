package net.lab1024.sa.base.module.support.codegenerator.service.variable.backend;

import java.util.HashMap;
import java.util.Map;
import net.lab1024.sa.base.module.support.codegenerator.domain.form.CodeGeneratorConfigForm;
import net.lab1024.sa.base.module.support.codegenerator.service.variable.CodeGenerateBaseVariableService;

/**
 * 目前暂时没用到 这是一个空实现
 *
 * @author zhoumingfa
 * @since 2024/8/13
 */
public class MenuVariableService extends CodeGenerateBaseVariableService {

  @Override
  public boolean isSupport(CodeGeneratorConfigForm form) {
    return true;
  }

  @Override
  public Map<String, Object> getInjectVariablesMap(CodeGeneratorConfigForm form) {
    return new HashMap<>(2);
  }
}
