package net.lab1024.sa.base.module.support.operatelog;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import java.util.List;
import net.lab1024.sa.base.core.util.SmartPageUtil;
import net.lab1024.sa.base.module.support.operatelog.domain.OperateLogEntity;
import net.lab1024.sa.base.module.support.operatelog.domain.OperateLogQueryForm;
import net.lab1024.sa.base.module.support.operatelog.domain.OperateLogVO;
import net.lab1024.sa.common.core.code.UserErrorCode;
import net.lab1024.sa.common.core.domain.PageResult;
import net.lab1024.sa.common.core.domain.ResponseDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import org.springframework.stereotype.Service;

/**
 * 操作日志
 *
 * @author 1024创新实验室: 罗伊
 * @since 2021-12-08 20:48:52 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
public class OperateLogService {

  @Resource private OperateLogDao operateLogDao;

  /**
   * @author 罗伊
   * @description 分页查询
   */
  public ResponseDTO<PageResult<OperateLogVO>> queryByPage(OperateLogQueryForm queryForm) {
    Page page = SmartPageUtil.convert2PageQuery(queryForm);
    List<OperateLogEntity> logEntityList = operateLogDao.queryByPage(page, queryForm);
    PageResult<OperateLogVO> pageResult =
        SmartPageUtil.convert2PageResult(page, logEntityList, OperateLogVO.class);
    return ResponseDTO.ok(pageResult);
  }

  /**
   * 查询详情
   *
   * @param operateLogId
   * @return
   */
  public ResponseDTO<OperateLogVO> detail(Long operateLogId) {
    OperateLogEntity operateLogEntity = operateLogDao.selectById(operateLogId);
    if (operateLogEntity == null) {
      return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
    }
    OperateLogVO operateLogVO = SmartBeanUtil.copy(operateLogEntity, OperateLogVO.class);
    return ResponseDTO.ok(operateLogVO);
  }
}
