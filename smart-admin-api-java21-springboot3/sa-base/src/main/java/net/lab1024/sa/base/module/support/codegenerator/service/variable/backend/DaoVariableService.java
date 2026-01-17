package net.lab1024.sa.base.module.support.codegenerator.service.variable.backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.lab1024.sa.base.module.support.codegenerator.domain.form.CodeGeneratorConfigForm;
import net.lab1024.sa.base.module.support.codegenerator.domain.model.CodeInsertAndUpdateField;
import net.lab1024.sa.base.module.support.codegenerator.service.variable.CodeGenerateBaseVariableService;
import org.apache.commons.collections4.CollectionUtils;

/**
 * @author 1024创新实验室-主任:卓大
 * @since 2022/9/29 17:20:41 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public class DaoVariableService extends CodeGenerateBaseVariableService {

  @Override
  public boolean isSupport(CodeGeneratorConfigForm form) {
    return true;
  }

  @Override
  public Map<String, Object> getInjectVariablesMap(CodeGeneratorConfigForm form) {
    Map<String, Object> variablesMap = new HashMap<>();

    List<CodeInsertAndUpdateField> updateFieldList =
        form.getInsertAndUpdate().getFieldList().stream()
            .filter(e -> Boolean.TRUE.equals(e.getInsertFlag()))
            .collect(Collectors.toList());
    List<String> packageList = getPackageList(updateFieldList, form);

    variablesMap.put("packageName", form.getBasic().getJavaPackageName() + ".dao");
    variablesMap.put("importPackageList", packageList);

    return variablesMap;
  }

  public List<String> getPackageList(
      List<CodeInsertAndUpdateField> fields, CodeGeneratorConfigForm form) {
    if (CollectionUtils.isEmpty(fields)) {
      return new ArrayList<>();
    }

    java.util.Set<String> packageSet = new HashSet<>();

    // 1、javabean相关的包
    packageSet.addAll(
        getJavaBeanImportClass(form).stream()
            .filter(e -> e.contains("QueryForm;") || e.contains("VO;") || e.contains("Entity;"))
            .collect(Collectors.toList()));

    // 2. util
    packageSet.add(IMPORT_PREFIX + "java.util.List;");

    // 3. 排序一下
    List<String> packageList = new ArrayList<>(packageSet);
    Collections.sort(packageList);
    return packageList;
  }
}
