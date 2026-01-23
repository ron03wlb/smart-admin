---
trigger: always_on
description: Exception Handling and Logging Rules - SLF4J, Exception Chain
tags: [exception-handling, logging, slf4j]
positioning: current-standard
ai_role: code_reviewer_and_generator
auto_apply: true
ask_before_fix: false
related_rules:
  - rules/02-oop-principles.md
  - rules/10-architecture-rules.md
last_updated: 2025-01-21
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
// ✅ Correct - Use placeholders
log.debug("Processing trade id={}, symbol={}", id, symbol);

// ✅ Correct - Conditional output (performance sensitive scenario)
if (log.isDebugEnabled()) {
    log.debug("Heavy operation result: " + computeExpensive());
}

// ❌ Incorrect - String concatenation
log.debug("Processing trade id=" + id + ", symbol=" + symbol);
```

### 3. Exception Log Completeness
```java
// ✅ Correct - Include context and stack trace
log.error("Order creation failed, userId={}, orderId={}", userId, orderId, e);

// ❌ Incorrect - Only print message
log.error("Order creation failed: " + e.getMessage());
```
