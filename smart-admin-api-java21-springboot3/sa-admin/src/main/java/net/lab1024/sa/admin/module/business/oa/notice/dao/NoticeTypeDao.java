package net.lab1024.sa.admin.module.business.oa.notice.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.admin.module.business.oa.notice.domain.entity.NoticeTypeEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知公告类型
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-12 21:40:39 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface NoticeTypeDao extends BaseMapper<NoticeTypeEntity> {}
