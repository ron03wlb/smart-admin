package net.lab1024.sa.support.serialnumber.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.OffsetDateTime;
import net.lab1024.sa.support.serialnumber.domain.SerialNumberEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 单据序列号
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-03-25 21:46:07 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface SerialNumberDao extends BaseMapper<SerialNumberEntity> {

  /**
   * 排他锁查询
   *
   * @param serialNumberId
   * @return
   */
  SerialNumberEntity selectForUpdate(@Param("serialNumberId") Integer serialNumberId);

  /**
   * 更新上一次的 数值和时间
   *
   * @param serialNumberId
   * @param lastNumber
   * @param lastTime
   */
  void updateLastNumberAndTime(
      @Param("serialNumberId") Integer serialNumberId,
      @Param("lastNumber") Long lastNumber,
      @Param("lastTime") OffsetDateTime lastTime);
}
