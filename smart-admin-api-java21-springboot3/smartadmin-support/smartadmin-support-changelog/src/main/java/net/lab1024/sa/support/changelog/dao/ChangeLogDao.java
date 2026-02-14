package net.lab1024.sa.support.changelog.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.support.changelog.domain.entity.ChangeLogEntity;
import net.lab1024.sa.support.changelog.domain.form.ChangeLogQueryForm;
import net.lab1024.sa.support.changelog.domain.vo.ChangeLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统更新日志 Dao
 *
 * @author 卓大
 * @since 2022-09-26 14:53:50 Copyright 1024创新实验室
 */
@Mapper
public interface ChangeLogDao extends BaseMapper<ChangeLogEntity> {

  /** 分页 查询 */
  List<ChangeLogVO> queryPage(Page page, @Param("queryForm") ChangeLogQueryForm queryForm);

  /** 根据版本查询 ChangeLog */
  ChangeLogEntity selectByVersion(@Param("version") String version);
}
