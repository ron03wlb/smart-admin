package net.lab1024.sa.common.datamasking.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.lab1024.sa.common.datamasking.constant.DataMaskingTypeEnum;
import net.lab1024.sa.common.datamasking.serializer.DataMaskingSerializer;

/**
 * 脱敏注解
 *
 * @author 罗伊
 * @since 2024/7/21 4:39 下午
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = DataMaskingSerializer.class, nullsUsing = DataMaskingSerializer.class)
public @interface DataMasking {

  DataMaskingTypeEnum value() default DataMaskingTypeEnum.COMMON;
}
