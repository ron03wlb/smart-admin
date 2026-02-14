package net.lab1024.sa.api.oa.contract;

import io.vavr.control.Option;
import java.util.List;
import net.lab1024.sa.api.oa.dto.NoticeTypeDTO;

/**
 * 通知類型服務 API 契約
 *
 * <p>設計原則：
 *
 * <ul>
 *   <li>使用 Vavr Option 保證類型安全
 *   <li>適配 Feign 遠程調用
 *   <li>無副作用的查詢操作
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
public interface NoticeTypeContract {

  /**
   * 獲取所有通知類型
   *
   * @return 通知類型列表（非空）
   */
  List<NoticeTypeDTO> getAll();

  /**
   * 根據 ID 查詢通知類型
   *
   * @param noticeTypeId 通知類型 ID
   * @return Option 包裝的通知類型對象（不存在返回 Option.none()）
   * @throws IllegalArgumentException 如果 noticeTypeId 為 null
   */
  Option<NoticeTypeDTO> getById(Long noticeTypeId);
}
