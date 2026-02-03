package net.lab1024.sa.support.datatracer.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.support.datatracer.domain.entity.DataTracerEntity;
import net.lab1024.sa.support.datatracer.domain.form.DataTracerQueryForm;
import net.lab1024.sa.support.datatracer.domain.vo.DataTracerVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * dao： t_data_tracker
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-07-23 19:38:52 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface DataTracerDao extends BaseMapper<DataTracerEntity> {

  /** 操作记录查询 */
  List<DataTracerVO> selectRecord(
      @Param("dataId") Long dataId, @Param("dataType") Integer dataType);

  /** 分页查询 */
  List<DataTracerVO> query(Page page, @Param("query") DataTracerQueryForm queryForm);
}
