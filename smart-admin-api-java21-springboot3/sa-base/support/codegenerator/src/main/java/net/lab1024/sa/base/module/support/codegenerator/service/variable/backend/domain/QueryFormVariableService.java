package net.lab1024.sa.base.module.support.codegenerator.service.variable.backend.domain;

import cn.hutool.core.bean.BeanUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import net.lab1024.sa.base.core.util.SmartStringUtil;
import net.lab1024.sa.base.module.support.codegenerator.constant.CodeQueryFieldQueryTypeEnum;
import net.lab1024.sa.base.module.support.codegenerator.domain.form.CodeGeneratorConfigForm;
import net.lab1024.sa.base.module.support.codegenerator.domain.model.CodeField;
import net.lab1024.sa.base.module.support.codegenerator.domain.model.CodeQueryField;
import net.lab1024.sa.base.module.support.codegenerator.service.variable.CodeGenerateBaseVariableService;
import net.lab1024.sa.foundation.validation.util.SmartEnumUtil;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * @author 1024创新实验室-主任:卓大
 * @since 2022/9/29 17:20:41 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public class QueryFormVariableService extends CodeGenerateBaseVariableService {
  private static final String JAVA_TYPE = "javaType";
  private static final String STRING_TYPE = "String";
  private static final String SCHEMA_IMPORT = "import io.swagger.v3.oas.annotations.media.Schema;";

  @Override
  public boolean isSupport(CodeGeneratorConfigForm form) {
    return true;
  }

  @Override
  public Map<String, Object> getInjectVariablesMap(CodeGeneratorConfigForm form) {
    Map<String, Object> variablesMap = new HashMap<>();
    ImmutablePair<List<String>, List<Map<String, Object>>> packageListAndFields =
        getPackageListAndFields(form);
    variablesMap.put("packageName", form.getBasic().getJavaPackageName() + ".domain.form");
    variablesMap.put("importPackageList", packageListAndFields.getLeft());
    variablesMap.put("fields", packageListAndFields.getRight());
    return variablesMap;
  }

  public ImmutablePair<List<String>, List<Map<String, Object>>> getPackageListAndFields(
      CodeGeneratorConfigForm form) {

    List<CodeQueryField> fields = form.getQueryFields();

    java.util.Set<String> packageList = new HashSet<>();

    /** 1、LocalDate、LocalDateTime、BigDecimal 类型的包名 2、排序 */
    List<Map<String, Object>> finalFieldList = new ArrayList<>();

    for (CodeQueryField field : fields) {

      // CodeField 和 InsertAndUpdateField 合并
      Map<String, Object> finalFieldMap = BeanUtil.beanToMap(field);
      finalFieldMap.putAll(BeanUtil.beanToMap(field));

      String queryTypeEnumStr = field.getQueryTypeEnum();
      CodeQueryFieldQueryTypeEnum queryTypeEnum =
          SmartEnumUtil.getEnumByValue(queryTypeEnumStr, CodeQueryFieldQueryTypeEnum.class);
      if (queryTypeEnum == null) {
        continue;
      }

      String apiModelProperty = "@Schema(description = \"" + field.getLabel() + "\")";
      finalFieldMap.put("apiModelProperty", apiModelProperty);
      packageList.add(SCHEMA_IMPORT);

      CodeField codeField;
      switch (queryTypeEnum) {
        case EQUAL:
          codeField = getCodeFieldByColumnName(field.getColumnNameList().get(0), form);
          if (codeField == null) {
            finalFieldMap.put(JAVA_TYPE, STRING_TYPE);
          } else {
            finalFieldMap.put(JAVA_TYPE, codeField.getJavaType());
          }
          break;
        case DATE_RANGE:
        case DATE:
          packageList.add(IMPORT_PREFIX + "java.time.LocalDate;");
          finalFieldMap.put(JAVA_TYPE, "LocalDate");
          break;
        case ENUM:
          codeField = getCodeFieldByColumnName(field.getColumnNameList().get(0), form);
          if (codeField == null) {
            continue;
          }

          packageList.add(IMPORT_PREFIX + "net.lab1024.sa.base.swagger.annotation.SchemaEnum;");
          packageList.add(IMPORT_PREFIX + "net.lab1024.sa.base.core.validator.CheckEnum;");
          packageList.add(
              IMPORT_PREFIX
                  + form.getBasic().getJavaPackageName()
                  + ".constant."
                  + codeField.getEnumName()
                  + ";");

          // enum check
          String checkEnum =
              "@CheckEnum(value = "
                  + codeField.getEnumName()
                  + ".class, message = \""
                  + codeField.getLabel()
                  + " 错误\")";
          finalFieldMap.put(
              "apiModelProperty",
              "@SchemaEnum(value = "
                  + codeField.getEnumName()
                  + ".class, desc = \""
                  + codeField.getLabel()
                  + "\")");
          finalFieldMap.put("checkEnum", checkEnum);
          finalFieldMap.put("isEnum", true);

          finalFieldMap.put(JAVA_TYPE, codeField.getJavaType());
          break;
        case DICT:
          codeField = getCodeFieldByColumnName(field.getColumnNameList().get(0), form);
          if (SmartStringUtil.isNotEmpty(codeField.getDict())) {
            finalFieldMap.put("dict", "\n    @JsonDeserialize(using = DictDataDeserializer.class)");
            packageList.add(
                IMPORT_PREFIX + "com.fasterxml.jackson.databind.annotation.JsonDeserialize;");
            packageList.add(
                IMPORT_PREFIX
                    + "net.lab1024.sa.foundation.json.deserializer.DictDataDeserializer;");
          }
          finalFieldMap.put(JAVA_TYPE, STRING_TYPE);
          break;
        default:
          finalFieldMap.put(JAVA_TYPE, STRING_TYPE);
      }

      finalFieldList.add(finalFieldMap);
    }

    // lombok
    packageList.add(IMPORT_PREFIX + "lombok.Data;");
    packageList.add(IMPORT_PREFIX + "lombok.EqualsAndHashCode;");

    List<String> packageNameList =
        packageList.stream().filter(Objects::nonNull).sorted().collect(Collectors.toList());
    return ImmutablePair.of(packageNameList, finalFieldList);
  }
}
