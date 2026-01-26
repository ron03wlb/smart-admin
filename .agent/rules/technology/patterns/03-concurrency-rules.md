---
trigger: always_on
description: Concurrency Programming Rules - Thread Pool, Lock, Thread Safety
tags: [concurrency, thread-safety, executor]
positioning: current-standard
last_updated: 2025-01-12
---

# Concurrency Rules

## 【Mandatory】Thread Pool Creation

### Prohibit Using Executors
```java
// ❌ Strictly Prohibited - OOM Risk
ExecutorService fixed = Executors.newFixedThreadPool(10);
// Reason: LinkedBlockingQueue unbounded, may cause OOM

ExecutorService cached = Executors.newCachedThreadPool();
// Reason: maximumPoolSize = Integer.MAX_VALUE, may create massive threads

// ✅ Correct - Use ThreadPoolExecutor
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    5,                      // corePoolSize
    10,                     // maximumPoolSize
    60L, TimeUnit.SECONDS,  // keepAliveTime
    new LinkedBlockingQueue<>(1000),  // Bounded queue
    new ThreadFactoryBuilder()
        .setNameFormat("order-pool-%d")
        .build(),
    new ThreadPoolExecutor.CallerRunsPolicy()
);
```

### Rejection Policy Selection
| Policy              | Behavior                       | Use Case                   |
| ------------------- | ------------------------------ | -------------------------- |
| AbortPolicy         | Throw RejectedExecutionException | Default, need handle exception |
| CallerRunsPolicy    | Caller thread executes         | Cannot discard tasks       |
| DiscardOldestPolicy | Discard oldest task in queue   | Allow discarding old tasks |
| DiscardPolicy       | Silently discard               | Ignorable tasks            |

## 【Mandatory】Lock Usage Rules
```java
// ✅ Correct - lock() outside try
Lock lock = new ReentrantLock();
lock.lock();
try {
    // Business logic
} finally {
    lock.unlock();
}

// ❌ Incorrect - lock() inside try
try {
    lock.lock();  // If throws exception, finally will unlock an unacquired lock
    // ...
} finally {
    lock.unlock();
}
```

## 【Mandatory】Double-Checked Singleton
```java
public class Singleton {
    // Must be volatile - Prevent instruction reordering
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

## 【Mandatory】ThreadLocal Cleanup
```java
// ✅ Correct - Must remove after use
private static final ThreadLocal<User> userContext = new ThreadLocal<>();

public void process() {
    userContext.set(currentUser);
    try {
        // Business logic
    } finally {
        userContext.remove();  // Must cleanup, prevent memory leak
    }
}
```
