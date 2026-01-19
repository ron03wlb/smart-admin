package net.lab1024.sa.common.cache;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheGetResult;
import com.alicp.jetcache.MultiGetResult;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 统一缓存服务接口
 *
 * <p>封装JetCache操作，提供统一的缓存访问方法
 *
 * @author 1024创新实验室
 * @since 2025-01-19 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public interface CacheService {

  /**
   * 获取或创建两级缓存实例（本地+远程）
   *
   * @param name 缓存名称
   * @param keyType Key类型
   * @param valueType Value类型
   * @param <K> Key泛型
   * @param <V> Value泛型
   * @return 缓存实例
   */
  <K, V> Cache<K, V> getOrCreateCache(String name, Class<K> keyType, Class<V> valueType);

  /**
   * 获取或创建两级缓存实例（本地+远程），指定过期时间
   *
   * @param name 缓存名称
   * @param keyType Key类型
   * @param valueType Value类型
   * @param localExpire 本地缓存过期时间
   * @param remoteExpire 远程缓存过期时间
   * @param <K> Key泛型
   * @param <V> Value泛型
   * @return 缓存实例
   */
  <K, V> Cache<K, V> getOrCreateCache(
      String name,
      Class<K> keyType,
      Class<V> valueType,
      Duration localExpire,
      Duration remoteExpire);

  /**
   * 获取或创建仅本地缓存实例
   *
   * @param name 缓存名称
   * @param keyType Key类型
   * @param valueType Value类型
   * @param <K> Key泛型
   * @param <V> Value泛型
   * @return 缓存实例
   */
  <K, V> Cache<K, V> getOrCreateLocalCache(String name, Class<K> keyType, Class<V> valueType);

  /**
   * 获取或创建仅远程缓存实例
   *
   * @param name 缓存名称
   * @param keyType Key类型
   * @param valueType Value类型
   * @param <K> Key泛型
   * @param <V> Value泛型
   * @return 缓存实例
   */
  <K, V> Cache<K, V> getOrCreateRemoteCache(String name, Class<K> keyType, Class<V> valueType);

  /**
   * 获取缓存值
   *
   * @param cacheName 缓存名称
   * @param key 缓存key
   * @param valueType 值类型
   * @param <K> Key泛型
   * @param <V> Value泛型
   * @return 缓存值，不存在返回empty
   */
  <K, V> Optional<V> get(String cacheName, K key, Class<V> valueType);

  /**
   * 获取缓存值，带详细结果
   *
   * @param cacheName 缓存名称
   * @param key 缓存key
   * @param valueType 值类型
   * @param <K> Key泛型
   * @param <V> Value泛型
   * @return 缓存获取结果
   */
  <K, V> CacheGetResult<V> getWithResult(String cacheName, K key, Class<V> valueType);

  /**
   * 批量获取缓存值
   *
   * @param cacheName 缓存名称
   * @param keys 缓存key集合
   * @param valueType 值类型
   * @param <K> Key泛型
   * @param <V> Value泛型
   * @return 批量获取结果
   */
  <K, V> MultiGetResult<K, V> getAll(String cacheName, Set<K> keys, Class<V> valueType);

  /**
   * 设置缓存值（使用默认过期时间）
   *
   * @param cacheName 缓存名称
   * @param key 缓存key
   * @param value 缓存值
   * @param <K> Key泛型
   * @param <V> Value泛型
   */
  <K, V> void put(String cacheName, K key, V value);

  /**
   * 设置缓存值（指定过期时间）
   *
   * @param cacheName 缓存名称
   * @param key 缓存key
   * @param value 缓存值
   * @param expire 过期时间
   * @param timeUnit 时间单位
   * @param <K> Key泛型
   * @param <V> Value泛型
   */
  <K, V> void put(String cacheName, K key, V value, long expire, TimeUnit timeUnit);

  /**
   * 批量设置缓存值
   *
   * @param cacheName 缓存名称
   * @param map 缓存键值对
   * @param <K> Key泛型
   * @param <V> Value泛型
   */
  <K, V> void putAll(String cacheName, Map<K, V> map);

  /**
   * 移除缓存
   *
   * @param cacheName 缓存名称
   * @param key 缓存key
   * @param <K> Key泛型
   * @return 是否移除成功
   */
  <K> boolean remove(String cacheName, K key);

  /**
   * 批量移除缓存
   *
   * @param cacheName 缓存名称
   * @param keys 缓存key集合
   * @param <K> Key泛型
   */
  <K> void removeAll(String cacheName, Set<K> keys);

  /**
   * 如果不存在则设置缓存
   *
   * @param cacheName 缓存名称
   * @param key 缓存key
   * @param value 缓存值
   * @param expire 过期时间
   * @param timeUnit 时间单位
   * @param <K> Key泛型
   * @param <V> Value泛型
   * @return 是否设置成功
   */
  <K, V> boolean putIfAbsent(String cacheName, K key, V value, long expire, TimeUnit timeUnit);

  /**
   * 获取缓存值，如果不存在则通过loader加载
   *
   * @param cacheName 缓存名称
   * @param key 缓存key
   * @param valueType 值类型
   * @param loader 加载函数
   * @param <K> Key泛型
   * @param <V> Value泛型
   * @return 缓存值
   */
  <K, V> V computeIfAbsent(String cacheName, K key, Class<V> valueType, Function<K, V> loader);

  /**
   * 获取缓存值，如果不存在则通过loader加载（指定过期时间）
   *
   * @param cacheName 缓存名称
   * @param key 缓存key
   * @param valueType 值类型
   * @param loader 加载函数
   * @param expire 过期时间
   * @param timeUnit 时间单位
   * @param <K> Key泛型
   * @param <V> Value泛型
   * @return 缓存值
   */
  <K, V> V computeIfAbsent(
      String cacheName,
      K key,
      Class<V> valueType,
      Function<K, V> loader,
      long expire,
      TimeUnit timeUnit);

  /**
   * 清空指定缓存的所有数据
   *
   * @param cacheName 缓存名称
   */
  void clear(String cacheName);

  // ==================== 缓存管理方法 ====================

  /**
   * 获取所有缓存名称
   *
   * @return 缓存名称列表
   */
  List<String> cacheNames();

  /**
   * 获取指定缓存下的所有key
   *
   * @param cacheName 缓存名称
   * @return key列表
   */
  List<String> cacheKey(String cacheName);

  /**
   * 移除指定缓存（清空并关闭）
   *
   * @param cacheName 缓存名称
   */
  void removeCache(String cacheName);
}
