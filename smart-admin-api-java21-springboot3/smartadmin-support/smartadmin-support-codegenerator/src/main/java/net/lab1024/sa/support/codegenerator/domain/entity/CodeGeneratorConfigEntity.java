package net.lab1024.sa.support.codegenerator.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * 代码生成-配置
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022/6/23 21:59:22 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_code_generator_config")
public class CodeGeneratorConfigEntity extends SmartAdminBaseEntity {

  /** 表名 */
  @TableId(type = IdType.NONE)
  private String tableName;

  /** 基础命名信息 */
  private String basic;

  /** 字段列表 */
  private String fields;

  /** 增加、修改 信息 */
  private String insertAndUpdate;

  /** 删除 信息 */
  private String deleteInfo;

  /** 查询字段 */
  private String queryFields;

  /** 列表字段 */
  private String tableFields;

  /** 详情 */
  private String detail;
}
