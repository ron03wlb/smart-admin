package net.lab1024.sa.base.module.support.helpdoc.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import java.util.List;
import net.lab1024.sa.base.module.support.helpdoc.dao.HelpDocDao;
import net.lab1024.sa.base.module.support.helpdoc.domain.entity.HelpDocEntity;
import net.lab1024.sa.base.module.support.helpdoc.domain.form.HelpDocViewRecordQueryForm;
import net.lab1024.sa.base.module.support.helpdoc.domain.vo.HelpDocDetailVO;
import net.lab1024.sa.base.module.support.helpdoc.domain.vo.HelpDocVO;
import net.lab1024.sa.base.module.support.helpdoc.domain.vo.HelpDocViewRecordVO;
import net.lab1024.sa.base.mybatis.util.SmartPageUtil;
import net.lab1024.sa.foundation.domain.request.RequestUser;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.util.SmartBeanUtil;
import org.springframework.stereotype.Service;

/**
 * 用户查看 帮助文档
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-20 23:11:42 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
@SuppressWarnings("PMD.LongVariable")
public class HelpDocUserService {

  @Resource private HelpDocDao helpDocDao;

  /**
   * 查询全部 帮助文档
   *
   * @return
   */
  public ResponseDTO<List<HelpDocVO>> queryAllHelpDocList() {
    return ResponseDTO.ok(helpDocDao.queryAllHelpDocList());
  }

  /**
   * 查询我的 待查看的 帮助文档清单
   *
   * @return
   */
  public ResponseDTO<HelpDocDetailVO> view(final RequestUser requestUser, final Long helpDocId) {
    final HelpDocEntity helpDocEntity = helpDocDao.selectById(helpDocId);
    if (helpDocEntity == null) {
      return ResponseDTO.userErrorParam("帮助文档不存在");
    }

    final HelpDocDetailVO helpDocDetailVO =
        SmartBeanUtil.copy(helpDocEntity, HelpDocDetailVO.class);
    final long viewCount = helpDocDao.viewRecordCount(helpDocId, requestUser.getUserId());
    if (viewCount == 0) {
      helpDocDao.insertViewRecord(
          helpDocId,
          requestUser.getUserId(),
          requestUser.getUserName(),
          requestUser.getIp(),
          requestUser.getUserAgent(),
          1);
      helpDocDao.updateViewCount(helpDocId, 1, 1);
      helpDocDetailVO.setPageViewCount(helpDocDetailVO.getPageViewCount() + 1);
      helpDocDetailVO.setUserViewCount(helpDocDetailVO.getUserViewCount() + 1);
    } else {
      helpDocDao.updateViewRecord(
          helpDocId, requestUser.getUserId(), requestUser.getIp(), requestUser.getUserAgent());
      helpDocDao.updateViewCount(helpDocId, 0, 1);
      helpDocDetailVO.setPageViewCount(helpDocDetailVO.getPageViewCount() + 1);
    }

    return ResponseDTO.ok(helpDocDetailVO);
  }

  /**
   * 分页查询 查看记录
   *
   * @param helpDocViewRecordQueryForm
   * @return
   */
  public PageResult<HelpDocViewRecordVO> queryViewRecord(
      final HelpDocViewRecordQueryForm helpDocViewRecordQueryForm) {
    final Page<?> page = SmartPageUtil.convert2PageQuery(helpDocViewRecordQueryForm);
    final List<HelpDocViewRecordVO> noticeViewRecordVOS =
        helpDocDao.queryViewRecordList(page, helpDocViewRecordQueryForm);
    return SmartPageUtil.convert2PageResult(page, noticeViewRecordVOS);
  }
}
