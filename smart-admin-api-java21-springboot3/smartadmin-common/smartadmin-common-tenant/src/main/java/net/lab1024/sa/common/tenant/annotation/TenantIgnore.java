package net.lab1024.sa.common.tenant.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a Dao method to skip tenant filtering.
 *
 * <p>For MyBatis-Plus Mapper methods, prefer using {@code @InterceptorIgnore(tenantLine = "true")}
 * directly. This annotation serves as a semantic marker for Service/Controller layer to indicate
 * cross-tenant operations.
 *
 * @author SmartAdmin
 * @since 2026-02-15
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantIgnore {}
