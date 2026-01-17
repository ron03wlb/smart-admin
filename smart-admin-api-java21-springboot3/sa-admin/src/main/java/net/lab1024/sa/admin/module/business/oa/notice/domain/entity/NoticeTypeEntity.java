package net.lab1024.sa.admin.module.business.oa.notice.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知公告类型
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-12 21:40:39 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@TableName("t_notice_type")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeTypeEntity {

  @TableId(type = IdType.AUTO)
  private Long noticeTypeId;

  /** 名称 */
  private String noticeTypeName;

  private LocalDateTime updateTime;

  private LocalDateTime createTime;
}
