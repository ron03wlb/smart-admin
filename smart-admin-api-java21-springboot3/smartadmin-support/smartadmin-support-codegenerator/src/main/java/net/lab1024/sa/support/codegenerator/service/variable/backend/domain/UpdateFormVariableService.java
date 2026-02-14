package net.lab1024.sa.support.codegenerator.service.variable.backend.domain;

import cn.hutool.core.bean.BeanUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import net.lab1024.sa.common.core.util.SmartStringUtil;
import net.lab1024.sa.support.codegenerator.constant.CodeFrontComponentEnum;
import net.lab1024.sa.support.codegenerator.domain.form.CodeGeneratorConfigForm;
import net.lab1024.sa.support.codegenerator.domain.model.CodeField;
import net.lab1024.sa.support.codegenerator.domain.model.CodeInsertAndUpdate;
import net.lab1024.sa.support.codegenerator.domain.model.CodeInsertAndUpdateField;
import net.lab1024.sa.support.codegenerator.service.variable.CodeGenerateBaseVariableService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * @author 1024创新实验室-主任:卓大
 * @since 2022/9/29 17:20:41 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public class UpdateFormVariableService extends CodeGenerateBaseVariableService {

  @Override
  public boolean isSupport(CodeGeneratorConfigForm form) {
    CodeInsertAndUpdate insertAndUpdate = form.getInsertAndUpdate();
    return insertAndUpdate != null
        && insertAndUpdate.getIsSupportInsertAndUpdate() != null
        && insertAndUpdate.getIsSupportInsertAndUpdate();
  }

  @Override
  public Map<String, Object> getInjectVariablesMap(CodeGeneratorConfigForm form) {
    Map<String, Object> variablesMap = new HashMap<>();

    Map<String, CodeField> fieldMap = getFieldMap(form);
    List<CodeInsertAndUpdateField> updateFieldList =
        form.getInsertAndUpdate().getFieldList().stream()
            .filter(
                e -> {
                  boolean isUpdate = Boolean.TRUE.equals(e.getUpdateFlag());
                  CodeField codeField = fieldMap.get(e.getColumnName());
                  if (codeField == null) {
                    return false;
                  }

                  if (Boolean.TRUE.equals(codeField.getPrimaryKeyFlag())) {
                    e.setRequiredFlag(true);
                  }

                  return isUpdate || Boolean.TRUE.equals(codeField.getPrimaryKeyFlag());
                })
            .collect(Collectors.toList());

    ImmutablePair<List<String>, List<Map<String, Object>>> packageListAndFields =
        getPackageListAndFields(updateFieldList, form);

    variablesMap.put("packageName", form.getBasic().getJavaPackageName() + ".domain.form");
    variablesMap.put("importPackageList", packageListAndFields.getLeft());
    variablesMap.put("fields", packageListAndFields.getRight());

    return variablesMap;
  }

  public ImmutablePair<List<String>, List<Map<String, Object>>> getPackageListAndFields(
      List<CodeInsertAndUpdateField> fields, CodeGeneratorConfigForm form) {
    if (CollectionUtils.isEmpty(fields)) {
      return ImmutablePair.of(new ArrayList<>(), new ArrayList<>());
    }

    Map<String, CodeField> fieldMap = getFieldMap(form);
    java.util.Set<String> packageList = new HashSet<>();

    /** 1、LocalDate、LocalDateTime、BigDecimal 类型的包名 2、排序 */
    List<Map<String, Object>> finalFieldList = new ArrayList<>();

    for (CodeInsertAndUpdateField field : fields) {
      CodeField codeField = fieldMap.get(field.getColumnName());
      if (codeField == null) {
        continue;
      }

      // CodeField 和 InsertAndUpdateField 合并
      Map<String, Object> finalFieldMap = BeanUtil.beanToMap(field);
      finalFieldMap.putAll(BeanUtil.beanToMap(codeField));

      // 枚举
      if (SmartStringUtil.isNotEmpty(codeField.getEnumName())) {
        packageList.add("import net.lab1024.sa.common.swagger.annotation.SchemaEnum;");
        packageList.add("import net.lab1024.sa.common.validation.annotation.CheckEnum;");
        packageList.add(
            "import "
                + form.getBasic().getJavaPackageName()
                + ".constant."
                + codeField.getEnumName()
                + ";");

        // enum check
        String checkEnumPrefix =
            "@CheckEnum(value = "
                + codeField.getEnumName()
                + ".class, message = \""
                + codeField.getLabel()
                + " 错误\"";
        String checkEnum = checkEnumPrefix + (field.getRequiredFlag() ? ", required = true)" : ")");

        finalFieldMap.put(
            "apiModelProperty",
            "@SchemaEnum(value = "
                + codeField.getEnumName()
                + ".class, desc = \""
                + codeField.getLabel()
                + "\")");
        finalFieldMap.put("checkEnum", checkEnum);
        finalFieldMap.put("isEnum", true);

      } else {
        String prefix = "@Schema(description = \"" + codeField.getLabel() + "\"";
        String apiModelProperty =
            prefix
                + (field.getRequiredFlag()
                    ? ", requiredMode = Schema.RequiredMode.REQUIRED)"
                    : ")");
        finalFieldMap.put("apiModelProperty", apiModelProperty);

        packageList.add(IMPORT_PREFIX + "io.swagger.v3.oas.annotations.media.Schema;");

        if (Boolean.TRUE.equals(field.getRequiredFlag())) {
          String notEmptyPrefix =
              "String".equals(codeField.getJavaType()) ? "@NotBlank" : "@NotNull";
          finalFieldMap.put(
              "notEmpty",
              "\n    " + notEmptyPrefix + "(message = \"" + codeField.getLabel() + " 不能为空\")");
          packageList.add(
              "String".equals(codeField.getJavaType())
                  ? IMPORT_PREFIX + "jakarta.validation.constraints.NotBlank;"
                  : IMPORT_PREFIX + "jakarta.validation.constraints.NotNull;");
        }
      }

      // 字典
      if (SmartStringUtil.isNotEmpty(codeField.getDict())) {
        finalFieldMap.put("dict", "\n    @JsonDeserialize(using = DictDataDeserializer.class)");
        packageList.add(
            IMPORT_PREFIX + "com.fasterxml.jackson.databind.annotation.JsonDeserialize;");
        packageList.add(
            IMPORT_PREFIX + "net.lab1024.sa.common.json.deserializer.DictDataDeserializer;");
      }

      // 文件上传
      if (CodeFrontComponentEnum.FILE_UPLOAD.equalsValue(field.getFrontComponent())) {
        finalFieldMap.put("file", "\n    @JsonDeserialize(using = FileKeyVoDeserializer.class)");
        packageList.add(
            IMPORT_PREFIX + "com.fasterxml.jackson.databind.annotation.JsonDeserialize;");
        packageList.add(
            IMPORT_PREFIX + "net.lab1024.sa.common.json.deserializer.FileKeyVoDeserializer;");
      }

      packageList.add(getJavaPackageName(codeField.getJavaType()));
      finalFieldList.add(finalFieldMap);
    }

    // lombok
    packageList.add(IMPORT_PREFIX + "lombok.Data;");

    List<String> packageNameList =
        packageList.stream().filter(Objects::nonNull).collect(Collectors.toList());
    Collections.sort(packageNameList);
    return ImmutablePair.of(packageNameList, finalFieldList);
  }
}
