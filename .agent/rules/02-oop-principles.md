---
trigger: always_on
---

# OOP 規約

## 強制規則

### 1. @Override 註解
- **規則**: 重寫方法必須加 @Override
```java
// ✅ 正確
@Override
public String toString() { return "User"; }

// ❌ 錯誤
public String toString() { return "User"; }
```

### 2. equals 調用順序
- **規則**: 常量或確定非空對象調用 equals
```java
// ✅ 正確
"active".equals(status)
Objects.equals(status, "active")

// ❌ 錯誤 - 可能 NPE
status.equals("active")
```

### 3. 包裝類比較
- **規則**: 用 equals()，禁止 ==
```java
// ✅ 正確
Integer a = 128, b = 128;
if (a.equals(b)) { /* 相等 */ }

// ❌ 錯誤 - Integer 緩存範圍 -128~127
if (a == b) { /* 可能為 false */ }
```

### 4. 浮點數比較
- **規則**: 禁止 == 或 equals
```java
// ✅ 正確：誤差範圍
float diff = 1e-6f;
if (Math.abs(a - b) < diff) { /* 相等 */ }

// ✅ 正確：BigDecimal
BigDecimal a = new BigDecimal("0.1");
BigDecimal b = new BigDecimal("0.10");
a.compareTo(b) == 0  // true

// ❌ 錯誤
float a = 0.1f, b = 0.1f;
if (a == b) { /* 不可靠 */ }
```

### 5. POJO 類規範
- **規則**: 成員變量必須用包裝類型
```java
// ✅ 正確
public class UserDO {
    private Long id;
    private Integer age;
    private Boolean active;
}

// ❌ 錯誤 - 基本類型有默認值，無法區分未設置
public class UserDO {
    private long id;    // 默認 0
    private int age;    // 默認 0
    private boolean active; // 默認 false
}
```

### 6. 字符串拼接
- **規則**: 循環內用 StringBuilder
```java
// ✅ 正確
StringBuilder sb = new StringBuilder();
for (String item : items) {
    sb.append(item);
}

// ❌ 錯誤 - 每次創建新 StringBuilder
String result = "";
for (String item : items) {
    result = result + item;
}
```