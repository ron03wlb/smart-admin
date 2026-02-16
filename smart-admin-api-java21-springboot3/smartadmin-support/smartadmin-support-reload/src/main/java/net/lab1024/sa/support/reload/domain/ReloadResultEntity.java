package net.lab1024.sa.support.reload.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * reload结果 <br>
 * t_reload_result 数据表 实体类
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2015-03-02 19:11:52 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@TableName("t_reload_result")
public class ReloadResultEntity {

  /** 加载项标签 */
  @TableId(type = IdType.NONE)
  private String tag;

  /** 运行标识 */
  private String identification;

  /** 参数 */
  private String args;

  /** 运行结果 */
  private Boolean result;

  /** 异常 */
  private String exception;

  /** 创建时间 */
  private OffsetDateTime createTime;

  /** 租户ID */
  private Long tenantId;
}
