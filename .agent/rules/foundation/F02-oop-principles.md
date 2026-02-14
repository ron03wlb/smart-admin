---
trigger: always_on
description: OOP Principles - SOLID, Inheritance, Polymorphism
tags: [oop, solid, design-principles]
positioning: current-standard
last_updated: 2025-01-12
---

# OOP Conventions

## Mandatory Rules

### 1. @Override Annotation
- **Rule**: Overriding methods must have @Override
```java
// ✅ Correct
@Override
public String toString() { return "User"; }

// ❌ Incorrect
public String toString() { return "User"; }
```

### 2. equals Call Order
- **Rule**: Constants or confirmed non-null objects call equals
```java
// ✅ Correct
"active".equals(status)
Objects.equals(status, "active")

// ❌ Incorrect - Possible NPE
status.equals("active")
```

### 3. Wrapper Class Comparison
- **Rule**: Use equals(), prohibit ==
```java
// ✅ Correct
Integer a = 128, b = 128;
if (a.equals(b)) { /* equal */ }

// ❌ Incorrect - Integer cache range -128~127
if (a == b) { /* may be false */ }
```

### 4. Floating Point Comparison
- **Rule**: Prohibit == or equals
```java
// ✅ Correct: Error range
float diff = 1e-6f;
if (Math.abs(a - b) < diff) { /* equal */ }

// ✅ Correct: BigDecimal
BigDecimal a = new BigDecimal("0.1");
BigDecimal b = new BigDecimal("0.10");
a.compareTo(b) == 0  // true

// ❌ Incorrect
float a = 0.1f, b = 0.1f;
if (a == b) { /* unreliable */ }
```

### 5. POJO Class Convention
- **Rule**: Member variables must use wrapper types
```java
// ✅ Correct
public class UserDO {
    private Long id;
    private Integer age;
    private Boolean active;
}

// ❌ Incorrect - Primitive types have default values, cannot distinguish unset
public class UserDO {
    private long id;    // Default 0
    private int age;    // Default 0
    private boolean active; // Default false
}
```

### 6. String Concatenation
- **Rule**: Use StringBuilder in loops
```java
// ✅ Correct
StringBuilder sb = new StringBuilder();
for (String item : items) {
    sb.append(item);
}

// ❌ Incorrect - Creates new StringBuilder each iteration
String result = "";
for (String item : items) {
    result = result + item;
}
```
