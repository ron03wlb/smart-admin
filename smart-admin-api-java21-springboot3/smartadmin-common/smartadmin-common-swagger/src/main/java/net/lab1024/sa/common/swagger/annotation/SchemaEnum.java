package net.lab1024.sa.common.swagger.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * 枚举类字段属性的 自定义 swagger 注解
 *
 * @author 1024创新实验室: 胡克
 * @since 2019/05/16 23:18 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SchemaEnum {

  /** 枚举类对象 */
  Class<? extends BaseEnum> value();

  String example() default "";

  boolean hidden() default false;

  boolean required() default true;

  String dataType() default "";

  String desc() default "";
}
