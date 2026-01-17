package net.lab1024.sa.base.module.support.codegenerator.dao;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.base.module.support.codegenerator.domain.form.TableQueryForm;
import net.lab1024.sa.base.module.support.codegenerator.domain.vo.TableColumnVO;
import net.lab1024.sa.base.module.support.codegenerator.domain.vo.TableVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * @author 1024创新实验室: 罗伊
 * @since 2022-06-30 22:15:38 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface CodeGeneratorDao {

  /** 分页查询表 */
  List<TableVO> queryTableList(Page page, @Param("queryForm") TableQueryForm queryForm);

  /**
   * 查询表是否存在
   *
   * @param tableName
   * @return
   */
  long countByTableName(@Param("tableName") String tableName);

  /**
   * 查询表列信息
   *
   * @param tableName
   * @return
   */
  List<TableColumnVO> selectTableColumn(@Param("tableName") String tableName);
}
