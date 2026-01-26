package net.lab1024.sa.foundation.cache.impl;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheGetResult;
import com.alicp.jetcache.MultiGetResult;
import com.alicp.jetcache.embedded.CaffeineCacheBuilder;
import com.alicp.jetcache.redis.lettuce.RedisLettuceCacheBuilder;
import com.alicp.jetcache.support.FastjsonKeyConvertor;
import com.alicp.jetcache.support.JavaValueDecoder;
import com.alicp.jetcache.support.JavaValueEncoder;
import io.lettuce.core.RedisClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.foundation.cache.CacheService;
import org.springframework.stereotype.Service;

/**
 * JetCache缓存服务实现
 *
 * @author 1024创新实验室
 * @since 2025-01-19 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.CloseResource") // Cache 对象由 cacheMap 管理，生命周期由 Spring 容器控制
public class JetCacheServiceImpl implements CacheService {

  private static final long DEFAULT_LOCAL_EXPIRE_SECONDS = 30 * 60;
  private static final long DEFAULT_REMOTE_EXPIRE_SECONDS = 2 * 60 * 60;
  private static final int DEFAULT_LOCAL_LIMIT = 1000;

  private final RedisClient redisClient;

  /** 缓存 Key 前缀，格式为 {projectName}:{environment}: */
  private final String cacheKeyPrefix;

  /** 缓存实例管理器 */
  private final Map<String, Cache<?, ?>> cacheMap = new ConcurrentHashMap<>();

  @Override
  @SuppressWarnings("unchecked")
  public <K, V> Cache<K, V> getOrCreateCache(String name, Class<K> keyType, Class<V> valueType) {
    return getOrCreateCache(
        name,
        keyType,
        valueType,
        Duration.ofSeconds(DEFAULT_LOCAL_EXPIRE_SECONDS),
        Duration.ofSeconds(DEFAULT_REMOTE_EXPIRE_SECONDS));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <K, V> Cache<K, V> getOrCreateCache(
      String name,
      Class<K> keyType,
      Class<V> valueType,
      Duration localExpire,
      Duration remoteExpire) {
    return (Cache<K, V>)
        cacheMap.computeIfAbsent(
            name, k -> createRemoteCache(name, remoteExpire.toSeconds(), TimeUnit.SECONDS));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <K, V> Cache<K, V> getOrCreateLocalCache(
      String name, Class<K> keyType, Class<V> valueType) {
    String localName = name + ":local";
    return (Cache<K, V>)
        cacheMap.computeIfAbsent(
            localName,
            k ->
                CaffeineCacheBuilder.createCaffeineCacheBuilder()
                    .keyConvertor(FastjsonKeyConvertor.INSTANCE)
                    .limit(DEFAULT_LOCAL_LIMIT)
                    .expireAfterWrite(DEFAULT_LOCAL_EXPIRE_SECONDS, TimeUnit.SECONDS)
                    .buildCache());
  }

  @Override
  @SuppressWarnings("unchecked")
  public <K, V> Cache<K, V> getOrCreateRemoteCache(
      String name, Class<K> keyType, Class<V> valueType) {
    String remoteName = name + ":remote";
    return (Cache<K, V>)
        cacheMap.computeIfAbsent(
            remoteName,
            k -> createRemoteCache(name, DEFAULT_REMOTE_EXPIRE_SECONDS, TimeUnit.SECONDS));
  }

  @SuppressWarnings("unchecked")
  private <K, V> Cache<K, V> createRemoteCache(String name, long expire, TimeUnit timeUnit) {
    return RedisLettuceCacheBuilder.createRedisLettuceCacheBuilder()
        .keyConvertor(FastjsonKeyConvertor.INSTANCE)
        .valueEncoder(JavaValueEncoder.INSTANCE)
        .valueDecoder(JavaValueDecoder.INSTANCE)
        .redisClient(redisClient)
        .keyPrefix(cacheKeyPrefix + name + ":")
        .expireAfterWrite(expire, timeUnit)
        .buildCache();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <K, V> Optional<V> get(String cacheName, K key, Class<V> valueType) {
    Cache<K, V> cache = getOrCreateCache(cacheName, (Class<K>) key.getClass(), valueType);
    return Optional.ofNullable(cache.get(key));
  }

  @Override
  @SuppressWarnings("unchecked")
  public <K, V> CacheGetResult<V> getWithResult(String cacheName, K key, Class<V> valueType) {
    Cache<K, V> cache = getOrCreateCache(cacheName, (Class<K>) key.getClass(), valueType);
    return cache.GET(key);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <K, V> MultiGetResult<K, V> getAll(String cacheName, Set<K> keys, Class<V> valueType) {
    if (keys == null || keys.isEmpty()) {
      return null;
    }
    K firstKey = keys.iterator().next();
    Cache<K, V> cache = getOrCreateCache(cacheName, (Class<K>) firstKey.getClass(), valueType);
    return cache.GET_ALL(keys);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <K, V> void put(String cacheName, K key, V value) {
    Cache<K, V> cache =
        getOrCreateCache(cacheName, (Class<K>) key.getClass(), (Class<V>) value.getClass());
    cache.put(key, value);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <K, V> void put(String cacheName, K key, V value, long expire, TimeUnit timeUnit) {
    Cache<K, V> cache =
        getOrCreateCache(cacheName, (Class<K>) key.getClass(), (Class<V>) value.getClass());
    cache.put(key, value, expire, timeUnit);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <K, V> void putAll(String cacheName, Map<K, V> map) {
    if (map == null || map.isEmpty()) {
      return;
    }
    Map.Entry<K, V> firstEntry = map.entrySet().iterator().next();
    Cache<K, V> cache =
        getOrCreateCache(
            cacheName,
            (Class<K>) firstEntry.getKey().getClass(),
            (Class<V>) firstEntry.getValue().getClass());
    cache.putAll(map);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <K> boolean remove(String cacheName, K key) {
    Cache<K, Object> cache = getOrCreateCache(cacheName, (Class<K>) key.getClass(), Object.class);
    return cache.remove(key);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <K> void removeAll(String cacheName, Set<K> keys) {
    if (keys == null || keys.isEmpty()) {
      return;
    }
    K firstKey = keys.iterator().next();
    Cache<K, Object> cache =
        getOrCreateCache(cacheName, (Class<K>) firstKey.getClass(), Object.class);
    cache.removeAll(keys);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <K, V> boolean putIfAbsent(
      String cacheName, K key, V value, long expire, TimeUnit timeUnit) {
    Cache<K, V> cache =
        getOrCreateCache(cacheName, (Class<K>) key.getClass(), (Class<V>) value.getClass());
    return cache.putIfAbsent(key, value);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <K, V> V computeIfAbsent(
      String cacheName, K key, Class<V> valueType, Function<K, V> loader) {
    Cache<K, V> cache = getOrCreateCache(cacheName, (Class<K>) key.getClass(), valueType);
    return cache.computeIfAbsent(key, loader);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <K, V> V computeIfAbsent(
      String cacheName,
      K key,
      Class<V> valueType,
      Function<K, V> loader,
      long expire,
      TimeUnit timeUnit) {
    Cache<K, V> cache = getOrCreateCache(cacheName, (Class<K>) key.getClass(), valueType);
    return cache.computeIfAbsent(key, loader, false, expire, timeUnit);
  }

  @Override
  public void clear(String cacheName) {
    Cache<?, ?> cache = cacheMap.get(cacheName);
    if (cache != null) {
      cache.close();
      cacheMap.remove(cacheName);
    }
  }

  // ==================== 缓存管理方法实现 ====================

  @Override
  public List<String> cacheNames() {
    return new ArrayList<>(cacheMap.keySet());
  }

  @Override
  @SuppressWarnings("PMD.UseExplicitTypes")
  public List<String> cacheKey(String cacheName) {
    // 使用Redis KEYS命令扫描指定缓存的所有key
    String prefix = cacheKeyPrefix + cacheName + ":";
    String pattern = prefix + "*";
    try (var connection = redisClient.connect()) {
      var keys = connection.sync().keys(pattern);
      if (keys == null || keys.isEmpty()) {
        return new ArrayList<>();
      }
      // 提取key的最后部分（去除前缀）
      return keys.stream().map(key -> key.substring(prefix.length())).collect(Collectors.toList());
    }
  }

  @Override
  public void removeCache(String cacheName) {
    clear(cacheName);
  }
}
