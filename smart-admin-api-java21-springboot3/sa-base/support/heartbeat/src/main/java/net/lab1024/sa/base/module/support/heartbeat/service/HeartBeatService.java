package net.lab1024.sa.base.module.support.heartbeat.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.base.module.support.heartbeat.dao.HeartBeatRecordDao;
import net.lab1024.sa.base.module.support.heartbeat.domain.HeartBeatRecordQueryForm;
import net.lab1024.sa.base.module.support.heartbeat.domain.HeartBeatRecordVO;
import net.lab1024.sa.base.mybatis.util.SmartPageUtil;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.springframework.stereotype.Service;

/**
 * 心跳记录
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-01-09 20:57:24 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.LongVariable")
public class HeartBeatService {

  private final HeartBeatRecordDao heartBeatRecordDao;

  public ResponseDTO<PageResult<HeartBeatRecordVO>> pageQuery(
      final HeartBeatRecordQueryForm pageParam) {
    final Page pageQueryInfo = SmartPageUtil.convert2PageQuery(pageParam);
    final List<HeartBeatRecordVO> recordVOList =
        heartBeatRecordDao.pageQuery(pageQueryInfo, pageParam);
    final PageResult<HeartBeatRecordVO> pageResult =
        SmartPageUtil.convert2PageResult(pageQueryInfo, recordVOList);
    return ResponseDTO.ok(pageResult);
  }
}
