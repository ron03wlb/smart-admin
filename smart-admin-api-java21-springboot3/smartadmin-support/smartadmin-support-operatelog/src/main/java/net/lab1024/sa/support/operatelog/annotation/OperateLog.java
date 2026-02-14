package net.lab1024.sa.support.operatelog.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用户操作日志 注解
 *
 * @author 1024创新实验室: 罗伊
 * @since 2021-12-08 20:48:52 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface OperateLog {
  // empty
}
