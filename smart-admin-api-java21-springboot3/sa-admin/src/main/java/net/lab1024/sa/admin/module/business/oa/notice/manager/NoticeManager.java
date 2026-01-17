package net.lab1024.sa.admin.module.business.oa.notice.manager;

import jakarta.annotation.Resource;
import java.util.List;
import net.lab1024.sa.admin.module.business.oa.notice.dao.NoticeDao;
import net.lab1024.sa.admin.module.business.oa.notice.domain.entity.NoticeEntity;
import net.lab1024.sa.admin.module.business.oa.notice.domain.form.NoticeVisibleRangeForm;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通知、公告 manager
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-12 21:40:39 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
public class NoticeManager {

  @Resource private NoticeDao noticeDao;

  /** 保存（事务管理，仅操作 DAO） */
  @Transactional(rollbackFor = Throwable.class)
  public void save(NoticeEntity noticeEntity, List<NoticeVisibleRangeForm> visibleRangeFormList) {
    noticeDao.insert(noticeEntity);
    Long noticeId = noticeEntity.getNoticeId();
    // 保存可见范围
    if (CollectionUtils.isNotEmpty(visibleRangeFormList)) {
      noticeDao.insertVisibleRange(noticeId, visibleRangeFormList);
    }
    // DataTracer 调用已移至 NoticeService 层
  }

  /** 更新（事务管理，仅操作 DAO） */
  @Transactional(rollbackFor = Throwable.class)
  public void update(
      NoticeEntity old, NoticeEntity noticeEntity, List<NoticeVisibleRangeForm> visibleRangeList) {
    noticeDao.updateById(noticeEntity);
    Long noticeId = noticeEntity.getNoticeId();
    // 保存可见范围
    if (CollectionUtils.isNotEmpty(visibleRangeList)) {
      noticeDao.deleteVisibleRange(noticeId);
      noticeDao.insertVisibleRange(noticeId, visibleRangeList);
    }
    // DataTracer 调用已移至 NoticeService 层
  }
}
