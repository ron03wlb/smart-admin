package net.lab1024.sa.base.module.support.feedback.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.base.module.support.feedback.dao.FeedbackDao;
import net.lab1024.sa.base.module.support.feedback.domain.FeedbackAddForm;
import net.lab1024.sa.base.module.support.feedback.domain.FeedbackEntity;
import net.lab1024.sa.base.module.support.feedback.domain.FeedbackQueryForm;
import net.lab1024.sa.base.module.support.feedback.domain.FeedbackVO;
import net.lab1024.sa.base.mybatis.util.SmartPageUtil;
import net.lab1024.sa.foundation.domain.request.RequestUser;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.util.SmartBeanUtil;
import org.springframework.stereotype.Service;

/**
 * 意见反馈
 *
 * @author 1024创新实验室: 开云
 * @since 2022-08-11 20:48:09 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
@RequiredArgsConstructor
public class FeedbackService {

  private final FeedbackDao feedbackDao;

  /** 分页查询 */
  public ResponseDTO<PageResult<FeedbackVO>> query(final FeedbackQueryForm queryForm) {
    final Page page = SmartPageUtil.convert2PageQuery(queryForm);
    final List<FeedbackVO> list = feedbackDao.queryPage(page, queryForm);
    final PageResult<FeedbackVO> pageResultDTO = SmartPageUtil.convert2PageResult(page, list);
    if (pageResultDTO.getEmptyFlag()) {
      return ResponseDTO.ok(pageResultDTO);
    }
    return ResponseDTO.ok(pageResultDTO);
  }

  /** 新建 */
  public ResponseDTO<String> add(final FeedbackAddForm addForm, final RequestUser requestUser) {
    final FeedbackEntity feedback = SmartBeanUtil.copy(addForm, FeedbackEntity.class);
    feedback.setUserType(requestUser.getUserType().getValue());
    feedback.setUserId(requestUser.getUserId());
    feedback.setUserName(requestUser.getUserName());
    feedbackDao.insert(feedback);
    return ResponseDTO.ok();
  }
}
