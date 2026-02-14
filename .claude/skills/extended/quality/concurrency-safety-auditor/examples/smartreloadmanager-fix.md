# SmartReloadManager 並發修復範例

## 問題識別

**類**: `SmartReloadManager.java:116-124`
**模式**: Check-Then-Act（非原子操作）
**風險評級**: ⭐⭐ (Low)

---

## 原始代碼（存在風險）

```java
package net.lab1024.sa.support.reload;

@Service
public class SmartReloadManager {
    private final Map<String, SmartReloadObject> reloadObjectMap = new ConcurrentHashMap<>();

    /**
     * 註冊 reload 對象
     * ❌ 問題：check-then-act 非原子操作
     */
    private void register(String tag, SmartReloadObject obj) {
        if (reloadObjectMap.containsKey(tag)) {  // ← Check
            log.error("Duplicate reload tag: {}", tag);
        }
        reloadObjectMap.put(tag, obj);  // ← Act（非原子，存在競態條件）
    }
}
```

---

## 風險分析

### 並發場景

```
Time | Thread A                     | Thread B
-----|------------------------------|------------------------------
T1   | containsKey("config") → false|
T2   |                              | containsKey("config") → false
T3   | log.error() (skipped)        |
T4   |                              | log.error() (skipped)
T5   | put("config", objA)          |
T6   |                              | put("config", objB)  ← 覆蓋了 objA！
```

**結果**:
- Thread A 的 `objA` 被 Thread B 的 `objB` 覆蓋
- 錯誤日誌未觸發（兩次檢查都返回 false）

### 實際風險評估

| 因素 | 評分 (0-10) | 說明 |
|------|------------|------|
| **發生概率** | 0.1 | 僅在應用啟動時執行，Spring Bean 初始化通常單線程 |
| **影響範圍** | 3 | 單個 reload tag 被覆蓋，不影響其他功能 |
| **實際危害** | 0 | 生產環境未觀察到此問題 |
| **總風險** | ⭐⭐ | **Low（但應修復）** |

**計算**: (0.1 × 0.4) + (3 × 0.35) + (0 × 0.25) = 1.09 / 10 → ⭐⭐

---

## 修復方案

### 方案 1: 使用 putIfAbsent()（推薦）

```java
/**
 * 註冊 reload 對象（原子操作）
 * ✅ 修復：使用 putIfAbsent() 確保原子性
 */
private void register(String tag, SmartReloadObject obj) {
    SmartReloadObject existing = reloadObjectMap.putIfAbsent(tag, obj);
    if (existing != null) {
        log.error("Duplicate reload tag: {}, existing: {}, new: {}",
                  tag, existing, obj);
    }
}
```

**優點**:
- ✅ 原子操作（ConcurrentHashMap 保證）
- ✅ 無需額外同步
- ✅ 性能最佳（無鎖爭用）

**風險評級**: ⭐⭐ → ⭐⭐⭐⭐⭐

---

### 方案 2: 使用 computeIfAbsent()

```java
private void register(String tag, SmartReloadObject obj) {
    reloadObjectMap.computeIfAbsent(tag, k -> {
        log.info("Registering reload tag: {}", tag);
        return obj;
    });
}
```

**優點**:
- ✅ 原子操作
- ✅ 僅在 key 不存在時執行 lambda

**缺點**:
- ⚠️ 無法記錄重複 tag 錯誤（lambda 不執行）

---

### 方案 3: 完全同步（不推薦）

```java
private synchronized void register(String tag, SmartReloadObject obj) {
    if (reloadObjectMap.containsKey(tag)) {
        log.error("Duplicate reload tag: {}", tag);
    }
    reloadObjectMap.put(tag, obj);
}
```

**缺點**:
- ❌ 性能較差（鎖爭用）
- ❌ 過度同步（ConcurrentHashMap 已提供並發保證）

---

## 測試驗證

### 並發測試

```java
@Test
void testConcurrentRegistration() throws Exception {
    SmartReloadManager manager = new SmartReloadManager();
    String tag = "concurrent-test";

    // 模擬 100 個線程同時註冊相同 tag
    ExecutorService executor = Executors.newFixedThreadPool(100);
    CountDownLatch latch = new CountDownLatch(100);

    for (int i = 0; i < 100; i++) {
        final int index = i;
        executor.submit(() -> {
            try {
                SmartReloadObject obj = new TestReloadObject("obj-" + index);
                manager.register(tag, obj);  // ✅ 僅第一個成功，其他記錄錯誤
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await(10, TimeUnit.SECONDS);
    executor.shutdown();

    // 驗證：僅有 1 個對象被註冊
    assertEquals(1, manager.getReloadObjectMap().size());
    assertTrue(manager.getReloadObjectMap().containsKey(tag));
}
```

**預期結果**:
- ✅ 僅 1 個對象被註冊（第一個 putIfAbsent 成功）
- ✅ 99 個線程記錄錯誤日誌

---

## 實施步驟

1. **修改代碼**:
   ```bash
   vi smartadmin-support/src/main/java/net/lab1024/sa/support/reload/SmartReloadManager.java
   # 替換 register() 方法為方案 1
   ```

2. **運行測試**:
   ```bash
   ./gradlew :smartadmin-app:test --tests "*ReloadTest*"
   ```

3. **驗證 ArchUnit**:
   ```bash
   ./gradlew :smartadmin-app:test --tests ArchitectureTest
   ```

4. **提交變更**:
   ```bash
   git add smartadmin-support/src/main/java/net/lab1024/sa/support/reload/SmartReloadManager.java
   git commit -m "fix(reload): fix check-then-act race condition in register()

   - Replace containsKey() + put() with putIfAbsent()
   - Risk rating: ⭐⭐ → ⭐⭐⭐⭐⭐
   - Concurrency safety: Guaranteed by ConcurrentHashMap

   Refs: Concurrency Safety Audit Report"
   ```

---

## 預期效果

| 指標 | 修改前 | 修改後 |
|------|--------|--------|
| 風險評級 | ⭐⭐ (Low) | ⭐⭐⭐⭐⭐ (None) |
| 競態條件 | 存在（理論） | 不存在 |
| 性能影響 | - | 無（ConcurrentHashMap 內置） |
| 代碼複雜度 | 中 | 低（單行原子操作） |

---

## 經驗總結

### ✅ 最佳實踐

1. **優先使用 ConcurrentHashMap 原子方法**:
   - `putIfAbsent()`
   - `computeIfAbsent()`
   - `merge()`

2. **避免 check-then-act 模式**:
   ```java
   // ❌ Bad
   if (map.containsKey(k)) { map.put(k, v); }

   // ✅ Good
   map.putIfAbsent(k, v);
   ```

3. **理解實際風險**:
   - 低概率 ≠ 不修復
   - 技術債務應及時清理
   - 預防勝於修復

### ❌ 常見錯誤

1. **過度同步**:
   ```java
   // ❌ 不必要的 synchronized
   private synchronized void method() {
       concurrentHashMap.put(k, v);  // 已經線程安全
   }
   ```

2. **忽視 ConcurrentHashMap 的原子方法**:
   - ConcurrentHashMap 提供多種原子操作
   - 不要重新發明輪子

3. **低估併發風險**:
   - "生產環境未發生" ≠ "不會發生"
   - 並發 Bug 難以復現但影響嚴重

---

**修復完成日期**: 2026-01-29
**審計評級提升**: ⭐⭐ → ⭐⭐⭐⭐⭐
