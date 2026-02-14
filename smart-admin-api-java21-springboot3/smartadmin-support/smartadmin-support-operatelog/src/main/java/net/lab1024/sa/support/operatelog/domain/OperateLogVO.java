package net.lab1024.sa.support.operatelog.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.Data;
import net.lab1024.sa.common.core.domain.enumeration.UserTypeEnum;
import net.lab1024.sa.common.swagger.annotation.SchemaEnum;

/**
 * 操作日志信息
 *
 * @author 1024创新实验室: 罗伊
 * @since 2021-12-08 20:48:52 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class OperateLogVO {

  @Schema(description = "主键")
  private Long operateLogId;

  @Schema(description = "用户id")
  private Long operateUserId;

  @SchemaEnum(value = UserTypeEnum.class, desc = "用户类型")
  private Integer operateUserType;

  @Schema(description = "用户名称")
  private String operateUserName;

  @Schema(description = "操作模块")
  private String module;

  @Schema(description = "操作内容")
  private String content;

  @Schema(description = "请求路径")
  private String url;

  @Schema(description = "请求方法")
  private String method;

  @Schema(description = "请求参数")
  private String param;

  @Schema(description = "返回值")
  private String response;

  @Schema(description = "客户ip")
  private String ip;

  @Schema(description = "客户ip地区")
  private String ipRegion;

  @Schema(description = "user-agent")
  private String userAgent;

  @Schema(description = "请求结果 0失败 1成功")
  private Integer successFlag;

  @Schema(description = "失败原因")
  private String failReason;

  @Schema(description = "更新时间")
  private OffsetDateTime updateTime;

  @Schema(description = "创建时间")
  private OffsetDateTime createTime;
}
