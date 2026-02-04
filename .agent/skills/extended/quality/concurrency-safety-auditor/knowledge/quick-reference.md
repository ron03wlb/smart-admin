# Concurrency Safety Auditor - Quick Reference

## 常見並發問題

### 1. Check-then-act (⭐⭐⭐⭐⭐)

```java
// ❌ 非原子操作
if (map.containsKey(key)) {
    return map.get(key);  // 可能已被其他線程移除
}

// ✅ 原子操作
return map.computeIfAbsent(key, k -> createValue());
```

### 2. Double-checked Locking (⭐⭐⭐⭐)

```java
// ❌ 錯誤實現
if (instance == null) {
    synchronized (lock) {
        if (instance == null) {
            instance = new Singleton();  // 可能部分初始化
        }
    }
}

// ✅ 使用 volatile
private static volatile Singleton instance;
```

### 3. ConcurrentHashMap 複合操作 (⭐⭐⭐⭐)

```java
// ❌ 非原子複合操作
ConcurrentMap<String, Integer> map = new ConcurrentHashMap<>();
Integer count = map.get(key);
if (count == null) {
    map.put(key, 1);
} else {
    map.put(key, count + 1);
}

// ✅ 原子操作
map.merge(key, 1, Integer::sum);
```

## 風險評級標準

| 等級 | 分數 | 描述 |
|------|------|------|
| ⭐⭐⭐⭐⭐ | 8-10 | 嚴重：可能導致數據損壞或系統崩潰 |
| ⭐⭐⭐⭐ | 6-7.9 | 高：可能導致不一致狀態 |
| ⭐⭐⭐ | 4-5.9 | 中：偶發問題，難以重現 |
| ⭐⭐ | 2-3.9 | 低：性能問題為主 |
| ⭐ | 0-1.9 | 最低：代碼異味 |

## 相關規則

- [P04-concurrency-rules.md](../../../../rules/technology/patterns/P04-concurrency-rules.md)
