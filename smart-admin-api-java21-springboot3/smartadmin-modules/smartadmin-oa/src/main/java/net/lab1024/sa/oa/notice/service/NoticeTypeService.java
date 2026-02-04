package net.lab1024.sa.oa.notice.service;

import cn.hutool.core.util.StrUtil;
import io.vavr.control.Option;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.oa.notice.dao.NoticeTypeDao;
import net.lab1024.sa.oa.notice.domain.entity.NoticeTypeEntity;
import net.lab1024.sa.oa.notice.domain.vo.NoticeTypeVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

/**
 * 通知。公告 类型
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-12 21:40:39 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RequiredArgsConstructor
@Service
public class NoticeTypeService {

  private final NoticeTypeDao noticeTypeDao;

  /**
   * 查询全部
   *
   * @return
   */
  public List<NoticeTypeVO> getAll() {
    return SmartBeanUtil.copyList(noticeTypeDao.selectList(null), NoticeTypeVO.class);
  }

  public NoticeTypeVO getByNoticeTypeId(Long noticceTypeId) {
    return SmartBeanUtil.copy(noticeTypeDao.selectById(noticceTypeId), NoticeTypeVO.class);
  }

  public synchronized ResponseDTO<String> add(String name) {
    if (StrUtil.isBlank(name)) {
      return ResponseDTO.userErrorParam("类型名称不能为空");
    }

    List<NoticeTypeEntity> noticeTypeEntityList = noticeTypeDao.selectList(null);
    if (!CollectionUtils.isEmpty(noticeTypeEntityList)) {
      boolean exist =
          noticeTypeEntityList.stream()
              .map(NoticeTypeEntity::getNoticeTypeName)
              .collect(Collectors.toSet())
              .contains(name);
      if (exist) {
        return ResponseDTO.userErrorParam("类型名称已经存在");
      }
    }
    noticeTypeDao.insert(NoticeTypeEntity.builder().noticeTypeName(name).build());
    return ResponseDTO.ok();
  }

  public synchronized ResponseDTO<String> update(Long noticeTypeId, String name) {
    if (StrUtil.isBlank(name)) {
      return ResponseDTO.userErrorParam("类型名称不能为空");
    }

    NoticeTypeEntity noticeTypeEntity = noticeTypeDao.selectById(noticeTypeId);
    if (noticeTypeEntity == null) {
      return ResponseDTO.userErrorParam("类型名称不存在");
    }

    List<NoticeTypeEntity> noticeTypeEntityList = noticeTypeDao.selectList(null);
    if (!CollectionUtils.isEmpty(noticeTypeEntityList)) {
      Option<NoticeTypeEntity> optionalNoticeTypeEntity =
          Option.ofOptional(
              noticeTypeEntityList.stream()
                  .filter(e -> e.getNoticeTypeName().equals(name))
                  .findFirst());
      if (optionalNoticeTypeEntity.isDefined()
          && !optionalNoticeTypeEntity.get().getNoticeTypeId().equals(noticeTypeId)) {
        return ResponseDTO.userErrorParam("类型名称已经存在");
      }
    }
    noticeTypeEntity.setNoticeTypeName(name);
    noticeTypeDao.updateById(noticeTypeEntity);
    return ResponseDTO.ok();
  }

  public synchronized ResponseDTO<String> delete(Long noticeTypeId) {
    noticeTypeDao.deleteById(noticeTypeId);
    return ResponseDTO.ok();
  }
}
