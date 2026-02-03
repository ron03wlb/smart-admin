package net.lab1024.sa.support.datatracer.domain.bo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Field;
import lombok.Data;

/**
 * 变动内容
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-07-23 19:38:52 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class DataTracerContentBO {

  /** 变动字段 */
  private Field field;

  /** 变动字段的值 */
  private Object fieldValue;

  /** 变动字段描述 */
  private String fieldDesc;

  /** 变动内容 */
  private String fieldContent;
}
