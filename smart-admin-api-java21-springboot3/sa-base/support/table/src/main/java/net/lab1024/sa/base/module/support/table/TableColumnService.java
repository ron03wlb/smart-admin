package net.lab1024.sa.base.module.support.table;

import jakarta.annotation.Resource;
import net.lab1024.sa.base.core.json.JsonUtil;
import net.lab1024.sa.base.module.support.table.domain.TableColumnEntity;
import net.lab1024.sa.base.module.support.table.domain.TableColumnUpdateForm;
import net.lab1024.sa.common.core.domain.RequestUser;
import net.lab1024.sa.common.core.domain.ResponseDTO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

/**
 * 表格自定义列（前端用户自定义表格列，并保存到数据库里）
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-12 22:52:21 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
public class TableColumnService {

  @Resource private TableColumnDao tableColumnDao;

  /**
   * 获取 - 表格列
   *
   * @param requestUser current request user
   * @param tableId table id
   * @return columns json string
   */
  public String getTableColumns(final RequestUser requestUser, final Integer tableId) {
    final TableColumnEntity tableColumnEntity =
        tableColumnDao.selectByUserIdAndTableId(
            requestUser.getUserId(), requestUser.getUserType().getValue(), tableId);
    return tableColumnEntity == null ? null : tableColumnEntity.getColumns();
  }

  /**
   * 更新表格列
   *
   * @param requestUser current request user
   * @param updateForm update form
   * @return response
   */
  public ResponseDTO<String> updateTableColumns(
      final RequestUser requestUser, final TableColumnUpdateForm updateForm) {
    if (CollectionUtils.isEmpty(updateForm.getColumnList())) {
      return ResponseDTO.ok();
    }
    final Integer tableId = updateForm.getTableId();
    TableColumnEntity tableColumnEntity =
        tableColumnDao.selectByUserIdAndTableId(
            requestUser.getUserId(), requestUser.getUserType().getValue(), tableId);
    if (tableColumnEntity == null) {
      tableColumnEntity = new TableColumnEntity();
      tableColumnEntity.setTableId(tableId);
      tableColumnEntity.setUserId(requestUser.getUserId());
      tableColumnEntity.setUserType(requestUser.getUserType().getValue());

      tableColumnEntity.setColumns(JsonUtil.toJson(updateForm.getColumnList()));
      tableColumnDao.insert(tableColumnEntity);
    } else {
      tableColumnEntity.setColumns(JsonUtil.toJson(updateForm.getColumnList()));
      tableColumnDao.updateById(tableColumnEntity);
    }
    return ResponseDTO.ok();
  }

  /**
   * 删除表格列
   *
   * @param requestUser current request user
   * @param tableId table id
   * @return response
   */
  public ResponseDTO<String> deleteTableColumn(
      final RequestUser requestUser, final Integer tableId) {
    tableColumnDao.deleteTableColumn(
        requestUser.getUserId(), requestUser.getUserType().getValue(), tableId);
    return ResponseDTO.ok();
  }
}
