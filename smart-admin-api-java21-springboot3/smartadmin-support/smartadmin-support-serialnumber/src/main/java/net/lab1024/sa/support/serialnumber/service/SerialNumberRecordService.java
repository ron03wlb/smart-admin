package net.lab1024.sa.support.serialnumber.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.support.serialnumber.dao.SerialNumberRecordDao;
import net.lab1024.sa.support.serialnumber.domain.SerialNumberRecordEntity;
import net.lab1024.sa.support.serialnumber.domain.SerialNumberRecordQueryForm;
import net.lab1024.sa.common.mybatis.util.SmartPageUtil;
import net.lab1024.sa.common.core.domain.response.PageResult;
import org.springframework.stereotype.Service;

/**
 * 单据序列号 记录
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-03-25 21:46:07 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.LongVariable")
public class SerialNumberRecordService {

  private final SerialNumberRecordDao serialNumberRecordDao;

  public PageResult<SerialNumberRecordEntity> query(final SerialNumberRecordQueryForm queryForm) {
    final Page page = SmartPageUtil.convert2PageQuery(queryForm);
    final List<SerialNumberRecordEntity> recordList = serialNumberRecordDao.query(page, queryForm);
    return SmartPageUtil.convert2PageResult(page, recordList);
  }
}
