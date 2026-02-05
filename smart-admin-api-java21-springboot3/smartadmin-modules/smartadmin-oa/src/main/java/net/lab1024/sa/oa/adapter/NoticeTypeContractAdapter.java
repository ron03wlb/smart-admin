package net.lab1024.sa.oa.adapter;

import io.vavr.control.Option;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.api.oa.contract.NoticeTypeContract;
import net.lab1024.sa.api.oa.dto.NoticeTypeDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.oa.notice.service.NoticeTypeService;
import org.springframework.stereotype.Component;

/**
 * 通知類型契約適配器
 *
 * <p>Adapter Pattern: 將 NoticeTypeService 適配到 NoticeTypeContract API 契約。
 *
 * <p>設計原則：
 *
 * <ul>
 *   <li>使用 Vavr Option 替代 null 返回
 *   <li>參數驗證拋出 IllegalArgumentException
 *   <li>使用 SmartBeanUtil 進行對象轉換
 *   <li>無副作用的查詢操作
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Component
@RequiredArgsConstructor
public class NoticeTypeContractAdapter implements NoticeTypeContract {

  private final NoticeTypeService noticeTypeService;

  /**
   * 獲取所有通知類型
   *
   * @return 通知類型列表（非空）
   */
  @Override
  public List<NoticeTypeDTO> getAll() {
    return noticeTypeService.getAll().stream()
        .map(vo -> SmartBeanUtil.copy(vo, NoticeTypeDTO.class))
        .collect(Collectors.toList());
  }

  /**
   * 根據 ID 查詢通知類型
   *
   * @param noticeTypeId 通知類型 ID
   * @return Option 包裝的通知類型對象（不存在返回 Option.none()）
   * @throws IllegalArgumentException 如果 noticeTypeId 為 null
   */
  @Override
  public Option<NoticeTypeDTO> getById(Long noticeTypeId) {
    if (noticeTypeId == null) {
      throw new IllegalArgumentException("noticeTypeId cannot be null");
    }
    return Option.of(noticeTypeService.getByNoticeTypeId(noticeTypeId))
        .map(vo -> SmartBeanUtil.copy(vo, NoticeTypeDTO.class));
  }
}
