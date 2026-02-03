package net.lab1024.sa.support.serialnumber.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上次生成信息
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-03-25 21:46:07 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SerialNumberLastGenerateBO {

  /** 序号id */
  private Integer serialNumberId;

  /** 上次生成的数字 */
  private Long lastNumber;

  /** 上次生成的时间 */
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  private LocalDateTime lastTime;
}
