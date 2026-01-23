package net.lab1024.sa.base.swagger.customizer;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import io.swagger.v3.oas.models.Operation;
import java.util.ArrayList;
import java.util.List;
import net.lab1024.sa.foundation.apiencrypt.annotation.ApiDecrypt;
import net.lab1024.sa.foundation.apiencrypt.annotation.ApiEncrypt;
import net.lab1024.sa.foundation.core.util.SmartStringUtil;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.web.method.HandlerMethod;

/**
 * 权限、接口加解密等
 *
 * @author 1024创新实验室-主任:卓大
 * @since 2023/12/26 13:47:39 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@SuppressWarnings(
    "PMD.ConsecutiveAppendsShouldReuse") // Separate appends improve readability for HTML
// construction
public class SmartOperationCustomizer implements OperationCustomizer {

  private static final String RED_FONT_START = "<font style=\"color:red\" class=\"light-red\">";

  private static final String FONT_BR_END = "</font></br>";

  @Override
  public Operation customize(Operation operation, HandlerMethod handlerMethod) {

    List<String> noteList = new ArrayList<>();

    // 请求参数加密
    List<String> encryptBuilderList = new ArrayList<>();

    if (handlerMethod.getMethodAnnotation(ApiDecrypt.class) != null
        || handlerMethod.getBeanType().getAnnotation(ApiDecrypt.class) != null) {
      encryptBuilderList.add("【请求参数加密】");
    }

    if (handlerMethod.getMethodAnnotation(ApiEncrypt.class) != null
        || handlerMethod.getBeanType().getAnnotation(ApiEncrypt.class) != null) {
      encryptBuilderList.add("【返回结果加密】");
    }

    if (!encryptBuilderList.isEmpty()) {
      noteList.add(
          "<br/>"
              + RED_FONT_START
              + "接口安全："
              + SmartStringUtil.join(",", encryptBuilderList)
              + "</font>");
    }

    // 权限
    noteList.addAll(getPermission(handlerMethod));

    // 更新
    operation.setDescription(SmartStringUtil.join("<br/>", noteList));

    return operation;
  }

  private List<String> getPermission(HandlerMethod handlerMethod) {
    List<String> values = new ArrayList<>();

    StringBuilder permissionStringBuilder = new StringBuilder();
    SaCheckPermission classPermissions =
        handlerMethod.getBeanType().getAnnotation(SaCheckPermission.class);
    if (classPermissions != null) {
      permissionStringBuilder.append(RED_FONT_START);
      permissionStringBuilder
          .append("类：")
          .append(getAnnotationNote(classPermissions.value(), classPermissions.mode()));
      permissionStringBuilder.append(FONT_BR_END);
    }

    SaCheckPermission methodPermission = handlerMethod.getMethodAnnotation(SaCheckPermission.class);
    if (methodPermission != null) {
      permissionStringBuilder.append(RED_FONT_START);
      permissionStringBuilder
          .append("方法：")
          .append(getAnnotationNote(methodPermission.value(), methodPermission.mode()));
      permissionStringBuilder.append(FONT_BR_END);
    }

    if (permissionStringBuilder.length() > 0) {
      permissionStringBuilder.insert(0, RED_FONT_START + "权限校验：</font></br>");
      values.add(permissionStringBuilder.toString());
    }

    StringBuilder roleStringBuilder = new StringBuilder();
    SaCheckRole classCheckRole = handlerMethod.getBeanType().getAnnotation(SaCheckRole.class);
    if (classCheckRole != null) {
      roleStringBuilder.append(RED_FONT_START);
      roleStringBuilder
          .append("类：")
          .append(getAnnotationNote(classCheckRole.value(), classCheckRole.mode()));
      roleStringBuilder.append(FONT_BR_END);
    }

    SaCheckPermission methodCheckRole = handlerMethod.getMethodAnnotation(SaCheckPermission.class);
    if (methodCheckRole != null) {
      roleStringBuilder.append(RED_FONT_START);
      roleStringBuilder
          .append("方法：")
          .append(getAnnotationNote(methodCheckRole.value(), methodCheckRole.mode()));
      roleStringBuilder.append(FONT_BR_END);
    }

    if (roleStringBuilder.length() > 0) {
      roleStringBuilder.insert(0, RED_FONT_START + "角色校验：</font></br>");
      values.add(roleStringBuilder.toString());
    }

    return values;
  }

  private String getAnnotationNote(String[] values, SaMode mode) {
    if (mode.equals(SaMode.AND)) {
      return String.join(" 且 ", values);
    } else {
      return String.join(" 或 ", values);
    }
  }
}
