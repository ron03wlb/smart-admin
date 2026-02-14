package net.lab1024.sa.support.table.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 自定义表格列
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-12 22:52:21 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@TableName("t_table_column")
public class TableColumnEntity {

  @TableId(type = IdType.AUTO)
  private Long tableColumnId;

  /** 用户id */
  private Long userId;

  /** 用户类型 */
  private Integer userType;

  /** 表id */
  private Integer tableId;

  /** 表列 */
  private String columns;

  private LocalDateTime createTime;

  private LocalDateTime updateTime;
}
