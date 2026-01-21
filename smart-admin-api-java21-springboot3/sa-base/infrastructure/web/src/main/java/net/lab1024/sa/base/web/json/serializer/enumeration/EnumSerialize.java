package net.lab1024.sa.base.web.json.serializer.enumeration;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.lab1024.sa.common.core.enumeration.BaseEnum;

/**
 * 枚举类 序列化 注解
 *
 * @author huke
 * @since 2024年6月29日
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = EnumSerializer.class, nullsUsing = EnumSerializer.class)
public @interface EnumSerialize {

  Class<? extends BaseEnum> value();
}
