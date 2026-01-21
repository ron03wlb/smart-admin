package net.lab1024.sa.base.module.support.job.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.base.module.support.job.api.domain.SmartJobLogQueryForm;
import net.lab1024.sa.base.module.support.job.api.domain.SmartJobLogVO;
import net.lab1024.sa.base.module.support.job.repository.domain.SmartJobLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 定时任务-执行记录 dao
 *
 * @author huke
 * @since 2024/6/17 21:30
 */
@Mapper
public interface SmartJobLogDao extends BaseMapper<SmartJobLogEntity> {

  /**
   * 定时任务-执行记录-分页查询
   *
   * @param page
   * @param queryForm
   * @return
   */
  List<SmartJobLogVO> query(Page<?> page, @Param("query") SmartJobLogQueryForm queryForm);
}
