package net.lab1024.sa.oa.enterprise.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.oa.enterprise.domain.entity.EnterpriseEntity;
import net.lab1024.sa.oa.enterprise.domain.form.EnterpriseQueryForm;
import net.lab1024.sa.oa.enterprise.domain.vo.EnterpriseExcelVO;
import net.lab1024.sa.oa.enterprise.domain.vo.EnterpriseListVO;
import net.lab1024.sa.oa.enterprise.domain.vo.EnterpriseVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 企业
 *
 * @author 1024创新实验室: 开云
 * @since 2022/7/28 20:37:15 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface EnterpriseDao extends BaseMapper<EnterpriseEntity> {

  String DELETED_FLAG = "deletedFlag";

  /** 根据企业名称查询 */
  EnterpriseEntity queryByEnterpriseName(
      @Param("enterpriseName") String enterpriseName,
      @Param("excludeEnterpriseId") Long excludeEnterpriseId,
      @Param(DELETED_FLAG) Boolean deletedFlag);

  /** 删除企业 */
  void deleteEnterprise(
      @Param("enterpriseId") Long enterpriseId, @Param(DELETED_FLAG) Boolean deletedFlag);

  /** 企业分页查询 */
  List<EnterpriseVO> queryPage(Page page, @Param("queryForm") EnterpriseQueryForm queryForm);

  /** 查询导出的数据 */
  List<EnterpriseExcelVO> selectExcelExportData(@Param("queryForm") EnterpriseQueryForm queryForm);

  /** 查询企业详情 */
  EnterpriseVO getDetail(
      @Param("enterpriseId") Long enterpriseId, @Param(DELETED_FLAG) Boolean deletedFlag);

  /** 查询列表 */
  List<EnterpriseListVO> queryList(
      @Param("type") Integer type,
      @Param("disabledFlag") Boolean disabledFlag,
      @Param(DELETED_FLAG) Boolean deletedFlag);
}
