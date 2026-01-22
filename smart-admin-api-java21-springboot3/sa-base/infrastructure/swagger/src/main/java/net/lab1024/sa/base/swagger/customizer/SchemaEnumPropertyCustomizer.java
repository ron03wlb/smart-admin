package net.lab1024.sa.base.swagger.customizer;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.annotation.Annotation;
import net.lab1024.sa.base.swagger.annotation.SchemaEnum;
import net.lab1024.sa.foundation.domain.enumeration.BaseEnum;
import net.lab1024.sa.foundation.validation.annotation.CheckEnum;
import net.lab1024.sa.foundation.validation.util.SmartEnumUtil;
import org.springdoc.core.customizers.PropertyCustomizer;
import org.springframework.stereotype.Component;

/**
 * 自定义枚举类文档
 *
 * @author 1024创新实验室-主任:卓大
 * @since 2023/12/25 23:28:51 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Component
public class SchemaEnumPropertyCustomizer implements PropertyCustomizer {

  @Override
  public Schema customize(Schema schema, AnnotatedType type) {
    if (type.getCtxAnnotations() == null) {
      return schema;
    }

    // Initial capacity of 64 for typical enum descriptions with HTML formatting
    StringBuilder description = new StringBuilder(64);
    for (Annotation ctxAnnotation : type.getCtxAnnotations()) {
      if (ctxAnnotation.annotationType().equals(CheckEnum.class)
          && ((CheckEnum) ctxAnnotation).required()) {
        description.append("<font style=\"color: red;\">【必填】</font>");
      }
    }

    for (Annotation ctxAnnotation : type.getCtxAnnotations()) {
      if (ctxAnnotation.annotationType().equals(SchemaEnum.class)) {
        description.append(((SchemaEnum) ctxAnnotation).desc());
        Class<? extends BaseEnum> clazz = ((SchemaEnum) ctxAnnotation).value();
        description.append(SmartEnumUtil.getEnumDesc(clazz));
      }
    }

    if (description.length() > 0) {
      schema.setDescription(description.toString());
    }
    return schema;
  }
}
