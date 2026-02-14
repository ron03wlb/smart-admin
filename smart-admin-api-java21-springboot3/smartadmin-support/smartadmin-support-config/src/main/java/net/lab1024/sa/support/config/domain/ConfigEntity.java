package net.lab1024.sa.support.config.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * 系统配置参数 实体类
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-03-14 20:46:27 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_config")
public class ConfigEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long configId;

  /** 参数key */
  private String configKey;

  /** 参数的值 */
  private String configValue;

  /** 参数名称 */
  private String configName;

  /** 备注 */
  private String remark;
}
