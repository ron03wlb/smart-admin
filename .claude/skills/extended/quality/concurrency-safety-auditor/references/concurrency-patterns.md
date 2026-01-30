# Java 並發模式參考指南

## 8 種常見並發模式

### 1. Check-Then-Act（⚠️ HIGH）
**問題**: 檢查和行動不是原子操作
**修復**: 使用原子方法（putIfAbsent, computeIfAbsent）

### 2. Double-Checked Locking（🚨 CRITICAL）
**問題**: 缺少 volatile 關鍵字
**修復**: 添加 volatile 或使用 Holder 模式

### 3. Inconsistent Synchronization（🚨 CRITICAL）
**問題**: 字段訪問同步不一致
**修復**: 所有訪問都同步

### 4. Non-Atomic Composite Action（⚠️ MEDIUM）
**問題**: 複合操作非原子（如 i++）
**修復**: 使用 AtomicInteger

### 5. Unsafe Publication（⚠️ HIGH）
**問題**: 對象未完全構造就發布
**修復**: 使用工廠方法或 final 字段

### 6. Unsynchronized Lazy Init（⚠️ HIGH）
**問題**: 懶初始化無同步
**修復**: Holder 模式或 synchronized

### 7. Wait Not in Loop（⚠️ MEDIUM）
**問題**: wait() 不在循環中（spurious wakeups）
**修復**: 使用 while 循環

### 8. Empty Synchronized Block（⚠️ LOW）
**問題**: 空同步塊（無意義）
**修復**: 刪除不必要的同步

---

## 並發集合選擇

| 場景 | 推薦 |
|------|------|
| 高併發讀寫 | ConcurrentHashMap |
| 寫少讀多 | CopyOnWriteArrayList |
| 有序訪問 | ConcurrentSkipListMap |
| 隊列 | ConcurrentLinkedQueue |

---

## 參考資料

- [Java Concurrency in Practice](https://jcip.net/)
- [SpotBugs Bug Descriptions](https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html)
