---
trigger: always_on
description: Exception Handling and Logging Rules - SLF4J, Exception Chain
tags: [exception-handling, logging, slf4j]
positioning: current-standard
ai_role: code_reviewer_and_generator
auto_apply: true
ask_before_fix: false
related_rules:
  - foundation/F02-oop-principles.md
  - foundation/F04-architecture-rules.md
archunit_test: ArchitectureTest#useSLF4JFacade
last_updated: 2025-01-25
---

# Exception and Logging Rules

## 【Mandatory】Exception Handling

### 1. Prohibit Catching RuntimeException for Flow Control
```java
// ❌ Incorrect
try {
    obj.method();
} catch (NullPointerException e) {
    // Using exception for flow control
}

// ✅ Correct
if (obj != null) {
    obj.method();
}
```

### 2. Prohibit Empty Catch Blocks
```java
// ❌ Incorrect - Swallowing exception
try {
    riskyOperation();
} catch (Exception e) {
    // Empty
}

// ✅ Correct
try {
    riskyOperation();
} catch (Exception e) {
    log.error("Operation failed, context={}", context, e);
    throw new ServiceException("Operation failed", e);
}
```

### 3. try-with-resources
```java
// ✅ Correct (Java 7+)
try (InputStream is = new FileInputStream(file);
     BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
    return br.readLine();
}

// ❌ Incorrect - Possible resource leak
InputStream is = new FileInputStream(file);
String line = new BufferedReader(new InputStreamReader(is)).readLine();
is.close();  // Will never execute if above line throws exception
```

### 4. Transaction Rollback
```java
// ✅ Correct - Explicitly specify rollback exception
@Transactional(rollbackFor = Exception.class)
public void createOrder(OrderDTO dto) {
    // ...
}

// ❌ Incorrect - Default only rolls back RuntimeException
@Transactional
public void createOrder(OrderDTO dto) throws IOException {
    // IOException will not rollback
}
```

## 【Mandatory】Logging Rules

### 1. Use SLF4J Facade
```java
// ✅ Correct
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger log = LoggerFactory.getLogger(UserService.class);

// ❌ Incorrect - Direct use of implementation
import org.apache.log4j.Logger;
```

### 2. Placeholder Style
```java
// ✅ Correct - Use placeholders (SLF4J lazy evaluation)
log.debug("Processing trade id={}, symbol={}", id, symbol);

// ✅ Correct - Complex computation with Supplier
log.debug("Heavy operation result: {}", () -> computeExpensive());

// ❌ Incorrect - Guard check (unnecessary with SLF4J placeholders)
if (log.isDebugEnabled()) {
    log.debug("Processing id={}", id);
}

// ❌ Incorrect - String concatenation
log.debug("Processing trade id=" + id + ", symbol=" + symbol);
```

### SmartAdmin Decision (v4.1.0)

SmartAdmin 統一**不使用** `log.isXxxEnabled()` 檢查，原因如下：

1. **SLF4J 延遲格式化**：使用 `{}` 佔位符時，字串格式化只在日誌級別啟用時才執行
2. **程式碼簡潔性**：移除冗餘的 if 包裹可提高可讀性
3. **一致性**：專案統一採用同一風格

**PMD 規則**：`GuardLogStatement` 已在 ruleset.xml 中排除

### 3. Exception Log Completeness
```java
// ✅ Correct - Include context and stack trace
log.error("Order creation failed, userId={}, orderId={}", userId, orderId, e);

// ❌ Incorrect - Only print message
log.error("Order creation failed: " + e.getMessage());
```
