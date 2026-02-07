package net.lab1024.sa.support.liteflow.manager;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.CreateCache;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.support.liteflow.domain.entity.LiteFlowChainEntity;
import net.lab1024.sa.support.liteflow.domain.entity.LiteFlowScriptEntity;
import org.springframework.stereotype.Component;

/**
 * LiteFlow 緩存管理器
 *
 * <p>使用 JetCache 兩級緩存（Caffeine + Redis）
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LiteFlowCacheManager {

  @CreateCache(
      name = "liteflow:chain:",
      cacheType = CacheType.BOTH, // Caffeine + Redis 兩級緩存
      expire = 120,
      timeUnit = TimeUnit.MINUTES,
      localExpire = 30,
      localLimit = 1000)
  private Cache<String, LiteFlowChainEntity> chainCache;

  @CreateCache(
      name = "liteflow:script:",
      cacheType = CacheType.BOTH,
      expire = 120,
      timeUnit = TimeUnit.MINUTES,
      localExpire = 30,
      localLimit = 1000)
  private Cache<String, LiteFlowScriptEntity> scriptCache;

  /**
   * 清除 Chain 緩存
   *
   * @param chainCode 流程編碼
   */
  public void evictChain(String chainCode) {
    log.debug("清除 Chain 緩存: chainCode={}", chainCode);
    chainCache.remove(chainCode);
  }

  /**
   * 清除 Script 緩存
   *
   * @param scriptCode 腳本編碼
   */
  public void evictScript(String scriptCode) {
    log.debug("清除 Script 緩存: scriptCode={}", scriptCode);
    scriptCache.remove(scriptCode);
  }

  /**
   * 清除所有 LiteFlow 緩存
   *
   * <p>用於流程規則重載時清空所有緩存
   */
  public void evictAll() {
    log.info("清除所有 LiteFlow 緩存");
    chainCache.unwrap(com.github.benmanes.caffeine.cache.Cache.class).invalidateAll();
    scriptCache.unwrap(com.github.benmanes.caffeine.cache.Cache.class).invalidateAll();
  }
}
