---
trigger: always_on
---

# 異常日誌規範

## 【強制】異常處理

### 1. 禁止捕獲 RuntimeException 做流程控制
```java
// ❌ 錯誤
try {
    obj.method();
} catch (NullPointerException e) {
    // 用異常做流程控制
}

// ✅ 正確
if (obj != null) {
    obj.method();
}
```

### 2. 禁止空 catch 塊
```java
// ❌ 錯誤 - 吞掉異常
try {
    riskyOperation();
} catch (Exception e) {
    // 空的
}

// ✅ 正確
try {
    riskyOperation();
} catch (Exception e) {
    log.error("操作失敗, context={}", context, e);
    throw new ServiceException("操作失敗", e);
}
```

### 3. try-with-resources
```java
// ✅ 正確 (Java 7+)
try (InputStream is = new FileInputStream(file);
     BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
    return br.readLine();
}

// ❌ 錯誤 - 可能資源洩漏
InputStream is = new FileInputStream(file);
String line = new BufferedReader(new InputStreamReader(is)).readLine();
is.close();  // 若上一行拋異常，永遠不會執行
```

### 4. 事務回滾
```java
// ✅ 正確 - 明確指定回滾異常
@Transactional(rollbackFor = Exception.class)
public void createOrder(OrderDTO dto) {
    // ...
}

// ❌ 錯誤 - 默認只回滾 RuntimeException
@Transactional
public void createOrder(OrderDTO dto) throws IOException {
    // IOException 不會回滾
}
```

## 【強制】日誌規範

### 1. 使用 SLF4J 門面
```java
// ✅ 正確
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger log = LoggerFactory.getLogger(UserService.class);

// ❌ 錯誤 - 直接使用實現
import org.apache.log4j.Logger;
```

### 2. 佔位符方式
```java
// ✅ 正確 - 使用佔位符
log.debug("Processing trade id={}, symbol={}", id, symbol);

// ✅ 正確 - 條件輸出（性能敏感場景）
if (log.isDebugEnabled()) {
    log.debug("Heavy operation result: " + computeExpensive());
}

// ❌ 錯誤 - 字符串拼接
log.debug("Processing trade id=" + id + ", symbol=" + symbol);
```

### 3. 異常日誌完整性
```java
// ✅ 正確 - 包含上下文和堆棧
log.error("訂單創建失敗, userId={}, orderId={}", userId, orderId, e);

// ❌ 錯誤 - 只打印消息
log.error("訂單創建失敗: " + e.getMessage());
```