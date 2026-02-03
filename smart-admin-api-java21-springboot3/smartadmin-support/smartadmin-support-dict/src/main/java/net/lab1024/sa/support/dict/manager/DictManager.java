package net.lab1024.sa.support.dict.manager;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.cache.constant.CacheKeyConst;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.support.dict.dao.DictDao;
import net.lab1024.sa.support.dict.dao.DictDataDao;
import net.lab1024.sa.support.dict.domain.entity.DictDataEntity;
import net.lab1024.sa.support.dict.domain.entity.DictEntity;
import net.lab1024.sa.support.dict.domain.vo.DictDataVO;
import org.springframework.stereotype.Service;

/**
 * 数据字典 缓存
 *
 * @author 1024创新实验室-主任-卓大
 * @since 2025-03-25 22:25:04 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
@RequiredArgsConstructor
public class DictManager {

  private final DictDao dictDao;

  private final DictDataDao dictDataDao;

  /** 获取字典 */
  @Cached(
      name = CacheKeyConst.Dict.DICT_DATA,
      key = "#dictCode + '_' + #dataValue",
      cacheType = CacheType.BOTH,
      localExpire = 30,
      expire = 120,
      timeUnit = TimeUnit.MINUTES)
  public DictDataVO getDictData(String dictCode, String dataValue) {
    DictEntity dictEntity = dictDao.selectByCode(dictCode);
    if (dictEntity == null) {
      return null;
    }

    DictDataEntity dictDataEntity =
        dictDataDao.selectByDictIdAndValue(dictEntity.getDictId(), dataValue);
    return SmartBeanUtil.copy(dictDataEntity, DictDataVO.class);
  }
}
