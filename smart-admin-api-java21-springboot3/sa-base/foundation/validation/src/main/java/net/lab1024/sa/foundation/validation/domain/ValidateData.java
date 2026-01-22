package net.lab1024.sa.foundation.validation.domain;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 校验数据是否为空的包装类
 *
 * @author 1024创新实验室: 胡克
 * @since 2020/10/16 21:06:11 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class ValidateData<T> {

  @NotNull(message = "数据不能为空哦")
  private T data;
}
