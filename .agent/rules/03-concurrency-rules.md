---
trigger: always_on
---

# 併發處理規範

## 【強制】線程池創建

### 禁止使用 Executors
```java
// ❌ 嚴禁 - OOM 風險
ExecutorService fixed = Executors.newFixedThreadPool(10);
// 原因: LinkedBlockingQueue 無界，可能 OOM

ExecutorService cached = Executors.newCachedThreadPool();
// 原因: maximumPoolSize = Integer.MAX_VALUE，可能創建大量線程

// ✅ 正確 - 使用 ThreadPoolExecutor
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5,                      // corePoolSize
    10,                     // maximumPoolSize
    60L, TimeUnit.SECONDS,  // keepAliveTime
    new LinkedBlockingQueue<>(1000),  // 有界隊列
    new ThreadFactoryBuilder()
        .setNameFormat("order-pool-%d")
        .build(),
    new ThreadPoolExecutor.CallerRunsPolicy()
);
```

### 拒絕策略選擇
| 策略                | 行為                          | 適用場景         |
| ------------------- | ----------------------------- | ---------------- |
| AbortPolicy         | 拋 RejectedExecutionException | 默認，需處理異常 |
| CallerRunsPolicy    | 調用線程執行                  | 不能丟棄任務     |
| DiscardOldestPolicy | 丟棄隊首任務                  | 允許丟棄舊任務   |
| DiscardPolicy       | 靜默丟棄                      | 可忽略的任務     |

## 【強制】Lock 使用規範
```java
// ✅ 正確 - lock() 在 try 外面
Lock lock = new ReentrantLock();
lock.lock();
try {
    // 業務邏輯
} finally {
    lock.unlock();
}

// ❌ 錯誤 - lock() 在 try 裡面
try {
    lock.lock();  // 若拋異常，finally 會釋放未獲取的鎖
    // ...
} finally {
    lock.unlock();
}
```

## 【強制】雙重檢查單例
```java
public class Singleton {
    // 必須 volatile - 防止指令重排序
    private static volatile Singleton instance;
    
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

## 【強制】ThreadLocal 清理
```java
// ✅ 正確 - 使用後必須 remove
private static final ThreadLocal<User> userContext = new ThreadLocal<>();

public void process() {
    userContext.set(currentUser);
    try {
        // 業務邏輯
    } finally {
        userContext.remove();  // 必須清理，防止內存洩漏
    }
}
```