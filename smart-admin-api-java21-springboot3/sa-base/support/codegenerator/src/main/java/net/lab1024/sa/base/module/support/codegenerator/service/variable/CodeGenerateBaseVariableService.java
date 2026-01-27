package net.lab1024.sa.base.module.support.codegenerator.service.variable;

import com.google.common.base.CaseFormat;
import io.vavr.control.Option;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.lab1024.sa.base.module.support.codegenerator.constant.CodeFrontComponentEnum;
import net.lab1024.sa.base.module.support.codegenerator.domain.form.CodeGeneratorConfigForm;
import net.lab1024.sa.base.module.support.codegenerator.domain.model.CodeField;
import net.lab1024.sa.base.module.support.codegenerator.domain.model.CodeInsertAndUpdate;
import net.lab1024.sa.base.module.support.codegenerator.domain.model.CodeInsertAndUpdateField;
import net.lab1024.sa.util.SmartStringUtil;
import org.apache.commons.collections4.CollectionUtils;

/**
 * @author 1024创新实验室-主任:卓大
 * @since 2022/9/29 17:20:41 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public abstract class CodeGenerateBaseVariableService {
  protected static final String IMPORT_PREFIX = "import ";

  private static final String BIG_DECIMAL = "BigDecimal";
  private static final String LOCAL_DATE = "LocalDate";
  private static final String LOCAL_DATE_TIME = "LocalDateTime";

  public abstract Map<String, Object> getInjectVariablesMap(CodeGeneratorConfigForm form);

  /**
   * 是否支持 : 1、增加、修改 2、删除
   *
   * @param form
   * @return
   */
  public abstract boolean isSupport(CodeGeneratorConfigForm form);

  /**
   * 获取所有javabean的 import 包名
   *
   * @param form
   * @return
   */
  public List<String> getJavaBeanImportClass(CodeGeneratorConfigForm form) {
    String upperCamelName =
        CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_CAMEL, form.getBasic().getModuleName());
    List<String> list = new ArrayList<>();

    list.add(
        IMPORT_PREFIX
            + form.getBasic().getJavaPackageName()
            + ".domain.entity."
            + upperCamelName
            + "Entity;");

    list.add(
        IMPORT_PREFIX
            + form.getBasic().getJavaPackageName()
            + ".domain.form."
            + upperCamelName
            + "AddForm;");
    list.add(
        IMPORT_PREFIX
            + form.getBasic().getJavaPackageName()
            + ".domain.form."
            + upperCamelName
            + "UpdateForm;");
    list.add(
        IMPORT_PREFIX
            + form.getBasic().getJavaPackageName()
            + ".domain.form."
            + upperCamelName
            + "QueryForm;");

    list.add(
        IMPORT_PREFIX
            + form.getBasic().getJavaPackageName()
            + ".domain.vo."
            + upperCamelName
            + "VO;");
    return list;
  }

  /** 根据列名查找 CodeField */
  public CodeField getCodeFieldByColumnName(String columnName, CodeGeneratorConfigForm form) {
    List<CodeField> fields = form.getFields();
    if (CollectionUtils.isEmpty(fields)) {
      return null;
    }

    return fields.stream()
        .filter(e -> SmartStringUtil.equals(columnName, e.getColumnName()))
        .findFirst()
        .orElse(null);
  }

  /** 是否为文件上传字段 */
  protected boolean isFile(String columnName, CodeGeneratorConfigForm form) {
    CodeInsertAndUpdate insertAndUpdate = form.getInsertAndUpdate();
    if (insertAndUpdate == null) {
      return false;
    }

    List<CodeInsertAndUpdateField> fieldList = insertAndUpdate.getFieldList();
    if (CollectionUtils.isEmpty(fieldList)) {
      return false;
    }

    Option<CodeInsertAndUpdateField> first =
        Option.ofOptional(
            fieldList.stream().filter(e -> columnName.equals(e.getColumnName())).findFirst());
    if (first.isEmpty()) {
      return false;
    }

    CodeInsertAndUpdateField field = first.get();
    return CodeFrontComponentEnum.FILE_UPLOAD.equalsValue(field.getFrontComponent());
  }

  /** 是否为 字典 */
  protected boolean isDict(String columnName, CodeGeneratorConfigForm form) {
    CodeField codeField = getCodeField(columnName, form);
    return codeField != null && codeField.getDict() != null;
  }

  /** 是否为 枚举 */
  protected boolean isEnum(String columnName, CodeGeneratorConfigForm form) {
    CodeField codeField = getCodeField(columnName, form);
    return codeField != null && codeField.getEnumName() != null;
  }

  private CodeField getCodeField(String columnName, CodeGeneratorConfigForm form) {
    List<CodeField> fields = form.getFields();
    if (CollectionUtils.isEmpty(fields)) {
      return null;
    }

    return fields.stream()
        .filter(e -> columnName.equals(e.getColumnName()))
        .findFirst()
        .orElse(null);
  }

  /**
   * 获取字段集合
   *
   * @param form
   * @return
   */
  protected Map<String, CodeField> getFieldMap(CodeGeneratorConfigForm form) {
    List<CodeField> fields = form.getFields();
    if (fields == null) {
      return new HashMap<>();
    }

    return fields.stream().collect(Collectors.toMap(CodeField::getColumnName, Function.identity()));
  }

  /**
   * 获取java类型
   *
   * @return
   */
  protected String getJavaPackageName(String javaType) {
    if (BIG_DECIMAL.equals(javaType)) {
      return IMPORT_PREFIX + "java.math.BigDecimal;";
    } else if (LOCAL_DATE.equals(javaType)) {
      return IMPORT_PREFIX + "java.time.LocalDate;";
    } else if (LOCAL_DATE_TIME.equals(javaType)) {
      return IMPORT_PREFIX + "java.time.LocalDateTime;";
    } else {

      return null;
    }
  }
}
