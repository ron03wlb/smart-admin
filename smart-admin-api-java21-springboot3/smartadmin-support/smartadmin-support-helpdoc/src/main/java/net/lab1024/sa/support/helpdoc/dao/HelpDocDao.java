package net.lab1024.sa.support.helpdoc.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.support.helpdoc.domain.entity.HelpDocEntity;
import net.lab1024.sa.support.helpdoc.domain.form.HelpDocQueryForm;
import net.lab1024.sa.support.helpdoc.domain.form.HelpDocRelationForm;
import net.lab1024.sa.support.helpdoc.domain.form.HelpDocViewRecordQueryForm;
import net.lab1024.sa.support.helpdoc.domain.vo.HelpDocRelationVO;
import net.lab1024.sa.support.helpdoc.domain.vo.HelpDocVO;
import net.lab1024.sa.support.helpdoc.domain.vo.HelpDocViewRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 帮助文档 dao
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-20 23:11:42 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
@SuppressWarnings({"PMD.LongVariable", "PMD.ShortVariable"})
public interface HelpDocDao extends BaseMapper<HelpDocEntity> {

  String HELP_DOC_ID = "helpDocId";

  // ================================= 帮助文档【主表 t_help_doc 】
  // =================================

  /**
   * 查询 全部相关文档
   *
   * @return 帮助文档列表
   */
  List<HelpDocVO> queryAllHelpDocList();

  /**
   * 后管分页查询帮助文档
   *
   * @param page 分页参数
   * @param queryForm 查询表单
   * @return 帮助文档列表
   */
  List<HelpDocVO> query(Page<?> page, @Param("query") HelpDocQueryForm queryForm);

  /**
   * 更新 阅读量
   *
   * @param helpDocId 文档ID
   * @param userViewCountIncrease 用户查看量增加
   * @param pageViewCountIncrease 页面查看量增加
   */
  void updateViewCount(
      @Param(HELP_DOC_ID) Long helpDocId,
      @Param("userViewCountIncrease") Integer userViewCountIncrease,
      @Param("pageViewCountIncrease") Integer pageViewCountIncrease);

  /**
   * 根据目录，查询文档
   *
   * @param helpDocCatalogId 目录ID
   * @return 帮助文档列表
   */
  List<HelpDocVO> queryHelpDocByCatalogId(@Param("helpDocCatalogId") Long helpDocCatalogId);

  /**
   * 根据关联文档id，查询文档
   *
   * @param relationId 关联ID
   * @return 帮助文档列表
   */
  List<HelpDocVO> queryHelpDocByRelationId(@Param("relationId") Long relationId);

  // ================================= 关联项目 【子表 t_help_doc_relation 】
  // =================================

  /**
   * 保存 关联
   *
   * @param helpDocId 文档ID
   * @param relationList 关联列表
   */
  void insertRelation(
      @Param(HELP_DOC_ID) Long helpDocId,
      @Param("relationList") List<HelpDocRelationForm> relationList);

  /**
   * 删除关联
   *
   * @param helpDocId 文档ID
   */
  void deleteRelation(@Param(HELP_DOC_ID) Long helpDocId);

  /**
   * 查询关联
   *
   * @param helpDocId 文档ID
   * @return 关联列表
   */
  List<HelpDocRelationVO> queryRelationByHelpDoc(@Param(HELP_DOC_ID) Long helpDocId);

  // ================================= 查看记录【子表 t_help_doc_view_record】
  // =================================

  /**
   * 查询某个用户的指定文档的阅读量
   *
   * @param helpDocId 文档ID
   * @param userId 用户ID
   * @return 阅读量
   */
  long viewRecordCount(@Param(HELP_DOC_ID) Long helpDocId, @Param("userId") Long userId);

  /**
   * 查询帮助文档的 查看记录
   *
   * @param page 分页参数
   * @param helpDocViewRecordQueryForm 查询表单
   * @return 查看记录列表
   */
  List<HelpDocViewRecordVO> queryViewRecordList(
      Page page, @Param("queryForm") HelpDocViewRecordQueryForm helpDocViewRecordQueryForm);

  /**
   * 保存查看记录
   *
   * @param helpDocId 文档ID
   * @param userId 用户ID
   * @param userName 用户名
   * @param ip IP地址
   * @param userAgent 浏览器标识
   * @param pageViewCount 页面查看量
   */
  void insertViewRecord(
      @Param(HELP_DOC_ID) Long helpDocId,
      @Param("userId") Long userId,
      @Param("userName") String userName,
      @Param("ip") String ip,
      @Param("userAgent") String userAgent,
      @Param("pageViewCount") Integer pageViewCount);

  /**
   * 更新查看记录
   *
   * @param helpDocId 文档ID
   * @param userId 用户ID
   * @param ip IP地址
   * @param userAgent 浏览器标识
   */
  void updateViewRecord(
      @Param(HELP_DOC_ID) Long helpDocId,
      @Param("userId") Long userId,
      @Param("ip") String ip,
      @Param("userAgent") String userAgent);
}
