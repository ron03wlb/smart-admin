package net.lab1024.sa.support.file.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Collection;
import java.util.List;
import net.lab1024.sa.support.file.domain.entity.FileEntity;
import net.lab1024.sa.support.file.domain.form.FileQueryForm;
import net.lab1024.sa.support.file.domain.vo.FileVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 文件服务
 *
 * @author 1024创新实验室: 罗伊
 * @since 2019年10月11日 15:34:47 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface FileDao extends BaseMapper<FileEntity> {

  /**
   * 文件key单个查询
   *
   * @param fileKey
   * @return
   */
  FileVO getByFileKey(@Param("fileKey") String fileKey);

  /** 批量获取 */
  List<FileVO> selectByFileKeyList(@Param("fileKeyList") Collection<String> fileKeyList);

  /**
   * 分页 查询
   *
   * @param page
   * @param queryForm
   * @return
   */
  List<FileVO> queryPage(Page page, @Param("queryForm") FileQueryForm queryForm);
}
