# Evrete 規則引擎完整指南

## 1. 概述

Evrete 是一個輕量級、高效能的 Java 規則引擎，實現了 RETE 演算法並完全符合 Java Rule Engine 規範 (JSR 94)。相較於 Drools 等重量級方案，Evrete 提供更簡潔的 API 和零依賴的特性。

### 核心特點

| 特性 | 說明 |
|------|------|
| **輕量級** | 零依賴，JAR 檔案體積小 |
| **JSR 94 相容** | 完全符合 Java Rule Engine 規範 |
| **RETE 演算法** | 針對大規模數據和標記數據優化 |
| **多種規則編寫方式** | Fluent API、Annotation、外部檔案 |
| **型別系統** | 無縫處理 JSON、XML 等任意物件 |
| **Java 8+** | 支援 Lambda 表達式和函數式介面 |

---

## 2. Maven 依賴設定

```xml
<!-- 核心函式庫 -->
<dependency>
    <groupId>org.evrete</groupId>
    <artifactId>evrete-core</artifactId>
    <version>4.0.3</version>
</dependency>

<!-- Annotation DSL 支援（選用） -->
<dependency>
    <groupId>org.evrete</groupId>
    <artifactId>evrete-dsl-java</artifactId>
    <version>4.0.3</version>
</dependency>
```

Gradle 設定：
```groovy
implementation 'org.evrete:evrete-core:4.0.3'
implementation 'org.evrete:evrete-dsl-java:4.0.3'
```

---

## 3. 核心概念

### 3.1 Knowledge Service
`KnowledgeService` 是規則引擎的入口點，負責管理規則知識庫的建立。

### 3.2 Knowledge
`Knowledge` 代表編譯後的規則集合，可重複使用建立多個 Session。

### 3.3 Session 類型

| 類型 | 說明 | 使用場景 |
|------|------|----------|
| **StatefulSession** | 有狀態，事實可增刪改 | 複雜推理、長期運行 |
| **StatelessSession** | 無狀態，一次性執行 | 簡單驗證、計算 |

### 3.4 Fact (事實)
傳入規則引擎進行評估的 Java 物件。

---

## 4. 程式碼範例

### 4.1 基礎範例：Fluent Builder API

#### 範例 1：質數篩選器

```java
import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;

public class PrimeNumbersExample {
    
    public static void main(String[] args) {
        // 1. 建立 KnowledgeService
        KnowledgeService service = new KnowledgeService();
        
        try {
            // 2. 使用 Builder API 建立規則
            Knowledge knowledge = service
                .newKnowledge()
                .builder()
                .newRule("remove-non-primes")
                    .forEach(
                        "$i1", Integer.class,
                        "$i2", Integer.class,
                        "$i3", Integer.class
                    )
                    .where("$i1 * $i2 == $i3")  // 條件：i1 * i2 = i3
                    .execute(ctx -> ctx.deleteFact("$i3"))  // 動作：刪除 i3
                .build();
            
            // 3. 建立有狀態 Session
            try (StatefulSession session = knowledge.newStatefulSession()) {
                // 4. 插入候選數字 2-100
                for (int i = 2; i <= 100; i++) {
                    session.insert(i);
                }
                
                // 5. 執行規則
                session.fire();
                
                // 6. 輸出剩餘的質數
                System.out.println("質數列表：");
                session.forEachFact((handle, fact) -> 
                    System.out.print(fact + " ")
                );
            }
        } finally {
            service.shutdown();
        }
    }
}
```

輸出結果：
```
質數列表：
2 3 5 7 11 13 17 19 23 29 31 37 41 43 47 53 59 61 67 71 73 79 83 89 97
```

---

### 4.2 業務場景：折扣計算

#### 領域模型

```java
// 訂單類別
public class Order {
    private String orderId;
    private double totalAmount;
    private double discount;
    private String customerType; // VIP, REGULAR, NEW
    private int itemCount;
    
    public Order(String orderId, double totalAmount, String customerType, int itemCount) {
        this.orderId = orderId;
        this.totalAmount = totalAmount;
        this.customerType = customerType;
        this.itemCount = itemCount;
        this.discount = 0;
    }
    
    public void applyDiscount(double rate) {
        this.discount = this.totalAmount * rate;
    }
    
    public double getFinalAmount() {
        return totalAmount - discount;
    }
    
    // Getters and Setters...
    public String getOrderId() { return orderId; }
    public double getTotalAmount() { return totalAmount; }
    public double getDiscount() { return discount; }
    public String getCustomerType() { return customerType; }
    public int getItemCount() { return itemCount; }
    public void setDiscount(double discount) { this.discount = discount; }
}
```

#### 使用 Fluent API 定義折扣規則

```java
import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatelessSession;

public class DiscountRulesExample {
    
    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        
        try {
            Knowledge knowledge = service
                .newKnowledge()
                .builder()
                
                // 規則 1：VIP 客戶享 15% 折扣
                .newRule("vip-discount")
                    .forEach("$order", Order.class)
                    .where("$order.customerType.equals(\"VIP\")")
                    .execute(ctx -> {
                        Order order = ctx.get("$order");
                        order.applyDiscount(0.15);
                        System.out.println("套用 VIP 折扣 15%: " + order.getOrderId());
                    })
                
                // 規則 2：訂單金額超過 1000 享 10% 折扣
                .newRule("large-order-discount")
                    .forEach("$order", Order.class)
                    .where("$order.totalAmount > 1000 && !$order.customerType.equals(\"VIP\")")
                    .execute(ctx -> {
                        Order order = ctx.get("$order");
                        order.applyDiscount(0.10);
                        System.out.println("套用大額訂單折扣 10%: " + order.getOrderId());
                    })
                
                // 規則 3：新客戶首購享 5% 折扣
                .newRule("new-customer-discount")
                    .forEach("$order", Order.class)
                    .where("$order.customerType.equals(\"NEW\") && $order.totalAmount <= 1000")
                    .execute(ctx -> {
                        Order order = ctx.get("$order");
                        order.applyDiscount(0.05);
                        System.out.println("套用新客戶折扣 5%: " + order.getOrderId());
                    })
                
                .build();
            
            // 建立測試訂單
            Order[] orders = {
                new Order("ORD-001", 500, "VIP", 3),
                new Order("ORD-002", 1500, "REGULAR", 5),
                new Order("ORD-003", 300, "NEW", 2),
                new Order("ORD-004", 2000, "VIP", 10)
            };
            
            // 執行無狀態 Session
            knowledge.newStatelessSession()
                .insert(orders)
                .fire();
            
            // 輸出結果
            System.out.println("\n========== 訂單折扣結果 ==========");
            for (Order order : orders) {
                System.out.printf("訂單 %s: 原價 $%.2f, 折扣 $%.2f, 實付 $%.2f%n",
                    order.getOrderId(),
                    order.getTotalAmount(),
                    order.getDiscount(),
                    order.getFinalAmount());
            }
            
        } finally {
            service.shutdown();
        }
    }
}
```

---

### 4.3 Annotation 方式定義規則

使用 `evrete-dsl-java` 模組可以用註解方式定義規則，更加結構化。

#### 規則類別

```java
import org.evrete.api.ActivationMode;
import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;

public class OrderDiscountRules {
    
    /**
     * VIP 客戶折扣規則
     */
    @Rule(value = "vip-discount", salience = 100)
    @Where("$order.customerType.equals(\"VIP\")")
    public void applyVipDiscount(@Fact("$order") Order order) {
        if (order.getDiscount() == 0) {
            order.applyDiscount(0.15);
            System.out.println("[VIP Rule] 訂單 " + order.getOrderId() + " 套用 15% 折扣");
        }
    }
    
    /**
     * 大額訂單折扣規則
     */
    @Rule(value = "large-order-discount", salience = 90)
    @Where("$order.totalAmount > 1000 && $order.discount == 0")
    public void applyLargeOrderDiscount(@Fact("$order") Order order) {
        order.applyDiscount(0.10);
        System.out.println("[Large Order Rule] 訂單 " + order.getOrderId() + " 套用 10% 折扣");
    }
    
    /**
     * 批量購買折扣規則
     */
    @Rule(value = "bulk-purchase-discount", salience = 80)
    @Where("$order.itemCount >= 5 && $order.discount == 0")
    public void applyBulkDiscount(@Fact("$order") Order order) {
        order.applyDiscount(0.08);
        System.out.println("[Bulk Rule] 訂單 " + order.getOrderId() + " 套用 8% 批量折扣");
    }
}
```

#### 使用 Annotation 規則

```java
import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;

public class AnnotationRulesExample {
    
    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        
        try {
            // 從 Annotation 類別建立 Knowledge
            Knowledge knowledge = service
                .newKnowledge("JAVA-CLASS", OrderDiscountRules.class);
            
            // 準備測試資料
            Order[] orders = {
                new Order("ORD-101", 800, "VIP", 2),
                new Order("ORD-102", 1500, "REGULAR", 3),
                new Order("ORD-103", 600, "REGULAR", 8)
            };
            
            // 執行規則
            knowledge.newStatelessSession()
                .insert(orders)
                .fire();
            
            // 顯示結果
            for (Order order : orders) {
                System.out.printf("訂單 %s: 最終折扣 %.0f%%\n",
                    order.getOrderId(),
                    (order.getDiscount() / order.getTotalAmount()) * 100);
            }
            
        } finally {
            service.shutdown();
        }
    }
}
```

---

### 4.4 多物件關聯規則

#### 場景：客戶與發票匹配計算銷售總額

```java
// 客戶類別
public class Customer {
    private String name;
    private double totalSales;
    
    public Customer(String name) {
        this.name = name;
        this.totalSales = 0;
    }
    
    public String getName() { return name; }
    public double getTotalSales() { return totalSales; }
    public void addSales(double amount) { this.totalSales += amount; }
    public void resetSales() { this.totalSales = 0; }
}

// 發票類別
public class Invoice {
    private String customerId;
    private double amount;
    
    public Invoice(String customerId, double amount) {
        this.customerId = customerId;
        this.amount = amount;
    }
    
    public String getCustomerId() { return customerId; }
    public double getAmount() { return amount; }
}
```

#### 規則實現

```java
import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import java.util.Arrays;
import java.util.List;

public class CustomerSalesExample {
    
    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        
        try {
            Knowledge knowledge = service
                .newKnowledge()
                .builder()
                
                // 規則 1：重置客戶銷售總額（優先執行）
                .newRule("reset-totals")
                    .salience(100)  // 高優先級
                    .forEach("$customer", Customer.class)
                    .execute(ctx -> {
                        Customer customer = ctx.get("$customer");
                        customer.resetSales();
                    })
                
                // 規則 2：匹配客戶與發票，累計銷售額
                .newRule("calculate-sales")
                    .salience(50)
                    .forEach(
                        "$customer", Customer.class,
                        "$invoice", Invoice.class
                    )
                    .where("$invoice.customerId.equals($customer.name)")
                    .execute(ctx -> {
                        Customer customer = ctx.get("$customer");
                        Invoice invoice = ctx.get("$invoice");
                        customer.addSales(invoice.getAmount());
                    })
                
                .build();
            
            // 準備測試資料
            List<Customer> customers = Arrays.asList(
                new Customer("Alice"),
                new Customer("Bob"),
                new Customer("Charlie")
            );
            
            List<Invoice> invoices = Arrays.asList(
                new Invoice("Alice", 1500.00),
                new Invoice("Alice", 2300.50),
                new Invoice("Bob", 800.00),
                new Invoice("Charlie", 3200.00),
                new Invoice("Bob", 1100.00),
                new Invoice("Alice", 950.00)
            );
            
            // 合併所有物件到 Session
            Object[] sessionData = new Object[customers.size() + invoices.size()];
            int index = 0;
            for (Customer c : customers) sessionData[index++] = c;
            for (Invoice i : invoices) sessionData[index++] = i;
            
            // 執行
            knowledge.newStatelessSession()
                .insert(sessionData)
                .fire();
            
            // 輸出結果
            System.out.println("========== 客戶銷售總額 ==========");
            for (Customer customer : customers) {
                System.out.printf("%s:\t$%,.2f%n", 
                    customer.getName(), 
                    customer.getTotalSales());
            }
            
        } finally {
            service.shutdown();
        }
    }
}
```

輸出：
```
========== 客戶銷售總額 ==========
Alice:	$4,750.50
Bob:	$1,900.00
Charlie:	$3,200.00
```

---

### 4.5 有狀態 Session 進階用法

```java
import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatefulSession;
import org.evrete.api.FactHandle;

public class StatefulSessionExample {
    
    public static void main(String[] args) {
        KnowledgeService service = new KnowledgeService();
        
        try {
            Knowledge knowledge = service
                .newKnowledge()
                .builder()
                .newRule("high-value-alert")
                    .forEach("$order", Order.class)
                    .where("$order.totalAmount > 5000")
                    .execute(ctx -> {
                        Order order = ctx.get("$order");
                        System.out.println("⚠️ 高價值訂單警報: " + order.getOrderId());
                    })
                .build();
            
            try (StatefulSession session = knowledge.newStatefulSession()) {
                
                // 第一批訂單
                System.out.println("=== 第一批訂單 ===");
                FactHandle handle1 = session.insert(new Order("ORD-001", 6000, "VIP", 5));
                session.insert(new Order("ORD-002", 3000, "REGULAR", 2));
                session.fire();
                
                // 動態新增訂單
                System.out.println("\n=== 新增訂單 ===");
                session.insert(new Order("ORD-003", 8000, "VIP", 10));
                session.fire();
                
                // 更新事實
                System.out.println("\n=== 更新訂單 ===");
                Order updatedOrder = new Order("ORD-001-UPDATED", 10000, "VIP", 15);
                session.delete(handle1);  // 刪除舊事實
                session.insert(updatedOrder);
                session.fire();
                
                // 查詢當前所有事實
                System.out.println("\n=== 當前 Session 中的訂單 ===");
                session.forEachFact((handle, fact) -> {
                    if (fact instanceof Order) {
                        Order o = (Order) fact;
                        System.out.println("  - " + o.getOrderId() + ": $" + o.getTotalAmount());
                    }
                });
            }
            
        } finally {
            service.shutdown();
        }
    }
}
```

---

## 5. Spring Boot 整合

### 5.1 Configuration 類別

```java
import org.evrete.KnowledgeService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EvreteConfig {
    
    @Bean(destroyMethod = "shutdown")
    public KnowledgeService knowledgeService() {
        return new KnowledgeService();
    }
}
```

### 5.2 Service 層封裝

```java
import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.evrete.api.StatelessSession;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class OrderRuleService {
    
    private final KnowledgeService knowledgeService;
    private Knowledge discountKnowledge;
    
    public OrderRuleService(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }
    
    @PostConstruct
    public void initRules() {
        this.discountKnowledge = knowledgeService
            .newKnowledge()
            .builder()
            
            .newRule("vip-discount")
                .forEach("$order", Order.class)
                .where("$order.customerType.equals(\"VIP\")")
                .execute(ctx -> {
                    Order order = ctx.get("$order");
                    order.applyDiscount(0.15);
                })
            
            .newRule("large-order-discount")
                .forEach("$order", Order.class)
                .where("$order.totalAmount > 1000 && $order.discount == 0")
                .execute(ctx -> {
                    Order order = ctx.get("$order");
                    order.applyDiscount(0.10);
                })
            
            .build();
    }
    
    public Order applyDiscountRules(Order order) {
        discountKnowledge.newStatelessSession()
            .insert(order)
            .fire();
        return order;
    }
    
    public List<Order> applyDiscountRules(List<Order> orders) {
        discountKnowledge.newStatelessSession()
            .insert(orders.toArray())
            .fire();
        return orders;
    }
}
```

### 5.3 Controller 使用

```java
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    private final OrderRuleService orderRuleService;
    
    public OrderController(OrderRuleService orderRuleService) {
        this.orderRuleService = orderRuleService;
    }
    
    @PostMapping("/calculate-discount")
    public Order calculateDiscount(@RequestBody Order order) {
        return orderRuleService.applyDiscountRules(order);
    }
    
    @PostMapping("/batch-calculate")
    public List<Order> batchCalculate(@RequestBody List<Order> orders) {
        return orderRuleService.applyDiscountRules(orders);
    }
}
```

---

## 6. 實際應用場景

### 6.1 電商折扣系統
- 會員等級折扣
- 滿額減免
- 組合優惠
- 限時促銷

### 6.2 保險報價引擎
- 根據年齡、職業、健康狀況計算保費
- 風險評估
- 核保規則

### 6.3 信用評分系統
- 收入評估
- 負債比率
- 信用歷史
- 風險分級

### 6.4 醫療診斷輔助
- 症狀匹配
- 疾病推斷
- 用藥建議

### 6.5 遊戲平台
- 玩家等級獎勵
- 任務達成判定
- 反作弊規則
- 投注限額計算

### 6.6 金融交易規則
- 交易限額檢查
- 洗錢防制 (AML)
- 風險控管

---

## 7. 與 Drools 比較

| 面向 | Evrete | Drools |
|------|--------|--------|
| **依賴** | 零依賴 | 多個依賴 |
| **學習曲線** | 低 | 中高 |
| **規則語言** | Java / Annotation | DRL / DMN |
| **效能** | 優秀 | 優秀 |
| **功能豐富度** | 中等 | 非常豐富 |
| **社群支援** | 小眾 | 大型社群 |
| **適用場景** | 輕量級嵌入式 | 企業級 BRMS |

---

## 8. 最佳實踐

1. **重用 Knowledge 物件**：Knowledge 編譯成本高，應該快取重用
2. **選擇正確的 Session 類型**：簡單計算用 Stateless，複雜推理用 Stateful
3. **使用 salience 控制優先級**：數值越高優先級越高
4. **及時關閉 Session**：使用 try-with-resources 確保資源釋放
5. **規則原子化**：每個規則只做一件事，便於維護
6. **善用 Annotation**：複雜專案建議用 Annotation 方式組織規則

---

## 9. 參考資源

- 官方網站：https://www.evrete.org
- GitHub：https://github.com/evrete/evrete
- Baeldung 教學：https://www.baeldung.com/java-evrete-rule-engine
