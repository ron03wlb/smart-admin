package net.lab1024.sa.support.datatracer.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;
import net.lab1024.sa.common.swagger.annotation.SchemaEnum;
import net.lab1024.sa.common.core.domain.enumeration.UserTypeEnum;
import net.lab1024.sa.support.datatracer.constant.DataTracerTypeEnum;

/**
 * 变动记录
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-07-23 19:38:52 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class DataTracerVO {

  @Schema(description = "日志id")
  private Long dataTracerId;

  @Schema(description = "单据id")
  private Long dataId;

  @SchemaEnum(value = DataTracerTypeEnum.class, desc = "业务类型")
  private Integer type;

  @Schema(description = "操作内容")
  private String content;

  @Schema(description = "diff 差异：旧的数据")
  private String diffOld;

  @Schema(description = "差异：新的数据")
  private String diffNew;

  @Schema(description = "扩展字段")
  private String extraData;

  @Schema(description = "操作人")
  private Long userId;

  @SchemaEnum(value = UserTypeEnum.class, desc = "用户类型")
  private Integer userType;

  @Schema(description = "操作人名称")
  private String userName;

  @Schema(description = "userAgent")
  private String userAgent;

  @Schema(description = "ip")
  private String ip;

  @Schema(description = "ip地区")
  private String ipRegion;

  @Schema(description = "操作时间")
  private LocalDateTime createTime;
}
