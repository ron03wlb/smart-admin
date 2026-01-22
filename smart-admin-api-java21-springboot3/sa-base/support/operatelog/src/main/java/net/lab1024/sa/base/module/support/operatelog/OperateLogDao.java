package net.lab1024.sa.base.module.support.operatelog;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.base.module.support.operatelog.domain.OperateLogEntity;
import net.lab1024.sa.base.module.support.operatelog.domain.OperateLogQueryForm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 操作日志
 *
 * @author 1024创新实验室: 罗伊
 * @since 2021-12-08 20:48:52 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface OperateLogDao extends BaseMapper<OperateLogEntity> {

  /**
   * 分页查询
   *
   * @param page
   * @param queryForm
   * @return UserOperateLogEntity
   */
  List<OperateLogEntity> queryByPage(Page page, @Param("query") OperateLogQueryForm queryForm);
}
