package net.lab1024.sa.support.codegenerator.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ZipUtil;
import com.google.common.base.CaseFormat;
import io.vavr.control.Option;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.util.SmartStringUtil;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.support.codegenerator.domain.entity.CodeGeneratorConfigEntity;
import net.lab1024.sa.support.codegenerator.domain.form.CodeGeneratorConfigForm;
import net.lab1024.sa.support.codegenerator.domain.model.CodeBasic;
import net.lab1024.sa.support.codegenerator.domain.model.CodeDelete;
import net.lab1024.sa.support.codegenerator.domain.model.CodeField;
import net.lab1024.sa.support.codegenerator.domain.model.CodeInsertAndUpdate;
import net.lab1024.sa.support.codegenerator.domain.model.CodeQueryField;
import net.lab1024.sa.support.codegenerator.domain.model.CodeTableField;
import net.lab1024.sa.support.codegenerator.service.variable.CodeGenerateBaseVariableService;
import net.lab1024.sa.support.codegenerator.service.variable.backend.ControllerVariableService;
import net.lab1024.sa.support.codegenerator.service.variable.backend.DaoVariableService;
import net.lab1024.sa.support.codegenerator.service.variable.backend.ManagerVariableService;
import net.lab1024.sa.support.codegenerator.service.variable.backend.MenuVariableService;
import net.lab1024.sa.support.codegenerator.service.variable.backend.ServiceVariableService;
import net.lab1024.sa.support.codegenerator.service.variable.backend.domain.AddFormVariableService;
import net.lab1024.sa.support.codegenerator.service.variable.backend.domain.EntityVariableService;
import net.lab1024.sa.support.codegenerator.service.variable.backend.domain.MapperVariableService;
import net.lab1024.sa.support.codegenerator.service.variable.backend.domain.QueryFormVariableService;
import net.lab1024.sa.support.codegenerator.service.variable.backend.domain.UpdateFormVariableService;
import net.lab1024.sa.support.codegenerator.service.variable.backend.domain.VOVariableService;
import net.lab1024.sa.support.codegenerator.service.variable.front.ApiVariableService;
import net.lab1024.sa.support.codegenerator.service.variable.front.ConstVariableService;
import net.lab1024.sa.support.codegenerator.service.variable.front.FormVariableService;
import net.lab1024.sa.support.codegenerator.service.variable.front.ListVariableService;
import net.lab1024.sa.support.codegenerator.util.CodeGeneratorTool;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.velocity.Template;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.ToolContext;
import org.apache.velocity.tools.ToolManager;
import org.springframework.stereotype.Service;

/**
 * 代码生成器 模板 Service
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-06-30 22:15:38 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
@Slf4j
public class CodeGeneratorTemplateService {

  private Map<String, CodeGenerateBaseVariableService> map = new HashMap<>();

  @PostConstruct
  public void init() {
    // 后端
    map.put("java/domain/entity/Entity.java", new EntityVariableService());
    map.put("java/domain/form/AddForm.java", new AddFormVariableService());
    map.put("java/domain/form/UpdateForm.java", new UpdateFormVariableService());
    map.put("java/domain/form/QueryForm.java", new QueryFormVariableService());
    map.put("java/domain/vo/VO.java", new VOVariableService());
    map.put("java/controller/Controller.java", new ControllerVariableService());
    map.put("java/service/Service.java", new ServiceVariableService());
    map.put("java/manager/Manager.java", new ManagerVariableService());
    map.put("java/dao/Dao.java", new DaoVariableService());
    map.put("java/mapper/Mapper.xml", new MapperVariableService());
    // 菜单 SQL
    map.put("java/sql/Menu.sql", new MenuVariableService());
    // 前端
    map.put("js/api.js", new ApiVariableService());
    map.put("js/const.js", new ConstVariableService());
    map.put("js/list.vue", new ListVariableService());
    map.put("js/form.vue", new FormVariableService());
    // ts前端
    map.put("ts/api.ts", new ApiVariableService());
    map.put("ts/const.ts", new ConstVariableService());
    map.put("ts/list.vue", new ListVariableService());
    map.put("ts/form.vue", new FormVariableService());
  }

  public void zipGeneratedFiles(
      OutputStream outputStream,
      String tableName,
      CodeGeneratorConfigEntity codeGeneratorConfigEntity) {
    String uuid = IdUtil.fastSimpleUUID();
    File dir = new File(uuid);

    // 1、生产文件
    CodeBasic basic = JsonUtil.fromJson(codeGeneratorConfigEntity.getBasic(), CodeBasic.class);
    String moduleName = basic.getModuleName();

    for (Map.Entry<String, CodeGenerateBaseVariableService> entry : map.entrySet()) {
      try {
        String templateFile = entry.getKey();
        String upperCamel = new CodeGeneratorTool().lowerCamel2UpperCamel(moduleName);
        String lowerHyphen = new CodeGeneratorTool().lowerCamel2LowerHyphen(moduleName);
        String[] templateSplit = templateFile.split("/");
        String fileName =
            templateFile.startsWith("java")
                ? upperCamel + templateSplit[templateSplit.length - 1]
                : lowerHyphen + "-" + templateSplit[templateSplit.length - 1];
        String fullPathFileName =
            templateFile.replaceAll(templateSplit[templateSplit.length - 1], fileName);
        fullPathFileName =
            fullPathFileName.replaceAll(
                "java/", "java/" + basic.getModuleName().toLowerCase(java.util.Locale.ROOT) + "/");
        fullPathFileName = fullPathFileName.replaceAll("js/", "js/" + lowerHyphen + "/");

        String fileContent = generate(tableName, templateFile, codeGeneratorConfigEntity);
        File file = new File(uuid + "/" + fullPathFileName);
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
          boolean created = parentFile.mkdirs();
          if (!created && log.isWarnEnabled()) {
            log.warn("create directory failed: {}", parentFile.getAbsolutePath());
          }
        }
        FileUtil.appendUtf8String(fileContent, file);
      } catch (IORuntimeException e) {
        log.error(e.getMessage(), e);
      }
    }

    // 2、后端的枚举文件
    List<CodeField> fields =
        JsonUtil.fromJsonArray(codeGeneratorConfigEntity.getFields(), CodeField.class);
    if (CollectionUtils.isNotEmpty(fields)) {
      List<CodeField> enumFiledList =
          fields.stream()
              .filter(e -> SmartStringUtil.isNotBlank(e.getEnumName()))
              .collect(Collectors.toList());
      for (CodeField codeField : enumFiledList) {
        Map<String, Object> variablesMap = new HashMap<>();

        String enumName =
            CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, codeField.getEnumName());
        if (!enumName.endsWith("Enum")) {
          enumName = enumName + "Enum";
        }
        variablesMap.put("enumName", enumName);
        variablesMap.put("enumDesc", codeField.getColumnComment());
        variablesMap.put("enumJavaType", codeField.getJavaType());
        variablesMap.put("basic", basic);
        variablesMap.put("packageName", basic.getJavaPackageName() + ".constant");

        String fileContent =
            render("code-generator-template/java/constant/enum.java.vm", variablesMap);
        File file =
            new File(
                uuid
                    + "/java/"
                    + basic.getModuleName().toLowerCase(java.util.Locale.ROOT)
                    + "/constant/"
                    + enumName
                    + ".java");
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
          boolean created = parentFile.mkdirs();
          if (!created && log.isWarnEnabled()) {
            log.warn("create directory failed: {}", parentFile.getAbsolutePath());
          }
        }
        FileUtil.appendUtf8String(fileContent, file);
      }
    }

    ZipUtil.zip(outputStream, StandardCharsets.UTF_8, false, null, dir);

    FileUtil.del(dir);
  }

  public String generate(
      String tableName, String file, CodeGeneratorConfigEntity codeGeneratorConfigEntity) {

    // -------------------- 1 校验不支持的代码生成，比如增加、删除等 --------------------

    String finalFile = file;
    Option<String> optional =
        Option.ofOptional(map.keySet().stream().filter(e -> e.contains(finalFile)).findFirst());
    if (optional.isEmpty()) {
      return "不存在此模板！";
    }

    String templateFile = optional.get();
    CodeGenerateBaseVariableService codeGenerateBaseVariableService = map.get(templateFile);
    if (codeGenerateBaseVariableService == null) {
      return "代码生成Service不存在，请检查相关代码！";
    }

    CodeBasic basic = JsonUtil.fromJson(codeGeneratorConfigEntity.getBasic(), CodeBasic.class);
    List<CodeField> fields =
        JsonUtil.fromJsonArray(codeGeneratorConfigEntity.getFields(), CodeField.class);
    CodeInsertAndUpdate insertAndUpdate =
        JsonUtil.fromJson(
            codeGeneratorConfigEntity.getInsertAndUpdate(), CodeInsertAndUpdate.class);
    CodeDelete deleteInfo =
        JsonUtil.fromJson(codeGeneratorConfigEntity.getDeleteInfo(), CodeDelete.class);
    List<CodeQueryField> queryFields =
        JsonUtil.fromJsonArray(codeGeneratorConfigEntity.getQueryFields(), CodeQueryField.class);
    List<CodeTableField> tableFields =
        JsonUtil.fromJsonArray(codeGeneratorConfigEntity.getTableFields(), CodeTableField.class);
    tableFields.forEach(e -> e.setWidth(e.getWidth() == null ? Integer.valueOf(0) : e.getWidth()));

    CodeGeneratorConfigForm form =
        CodeGeneratorConfigForm.builder()
            .basic(basic)
            .fields(fields)
            .insertAndUpdate(insertAndUpdate)
            .deleteInfo(deleteInfo)
            .queryFields(queryFields)
            .tableFields(tableFields)
            .deleteInfo(deleteInfo)
            .build();
    form.setTableName(tableName);
    if (!codeGenerateBaseVariableService.isSupport(form)) {
      return "业务不需要此功能，故没有生成代码；";
    }

    // -------------------- 2 通用模板的变量 --------------------
    Map<String, Object> variablesMap = new HashMap<>();

    Map<String, Object> basicMap = BeanUtil.beanToMap(basic);
    basicMap.put("frontDate", DateUtil.formatLocalDateTime(basic.getFrontDate().toLocalDateTime()));
    basicMap.put(
        "backendDate", DateUtil.formatLocalDateTime(basic.getBackendDate().toLocalDateTime()));

    variablesMap.put("basic", basicMap);
    variablesMap.put("fields", fields);
    variablesMap.put("insertAndUpdate", insertAndUpdate);
    variablesMap.put("deleteInfo", deleteInfo);
    variablesMap.put("queryFields", queryFields);
    variablesMap.put("tableFields", tableFields);
    variablesMap.put("tableName", tableName);

    // 名词的大写开头和小写开头
    Map<String, String> names = new HashMap<>();
    names.put(
        "lowerCamel", CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, basic.getModuleName()));
    names.put(
        "upperCamel", CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_CAMEL, basic.getModuleName()));
    names.put(
        "lowerHyphenCamel",
        CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, basic.getModuleName()));
    variablesMap.put("name", names);

    // 主键字段名称和java类型
    CodeField primaryKeycodeField =
        fields.stream().filter(e -> e.getPrimaryKeyFlag()).findFirst().get();
    if (primaryKeycodeField != null) {
      variablesMap.put("primaryKeyJavaType", primaryKeycodeField.getJavaType());
      variablesMap.put("primaryKeyFieldName", primaryKeycodeField.getFieldName());
      variablesMap.put("primaryKeyColumnName", primaryKeycodeField.getColumnName());
    }

    // -------------------- 3、针对此 模板 的特殊变量 --------------------

    Map<String, Object> specialVariables =
        codeGenerateBaseVariableService.getInjectVariablesMap(form);
    variablesMap.putAll(specialVariables);

    // -------------------- 4、模板 生成代码 --------------------

    return render("code-generator-template/" + templateFile + ".vm", variablesMap);
  }

  /**
   * 渲染
   *
   * @param templateFile
   * @param variablesMap
   * @return
   */
  private String render(String templateFile, Map<String, Object> variablesMap) {
    VelocityEngine engine = new VelocityEngine();
    engine.setProperty(Velocity.FILE_RESOURCE_LOADER_CACHE, true);
    engine.setProperty(Velocity.INPUT_ENCODING, "UTF-8");
    engine.setProperty(
        "resource.loader.file.class",
        "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
    engine.init();
    Template template = engine.getTemplate(templateFile);

    // 加载tools.xml配置文件
    ToolManager toolManager = new ToolManager();
    toolManager.configure("code-generator-template/tools.xml");

    // 注入变量
    ToolContext context = toolManager.createContext();
    context.putAll(variablesMap);

    StringWriter sw = new StringWriter();
    template.merge(context, sw);
    return sw.toString();
  }
}
