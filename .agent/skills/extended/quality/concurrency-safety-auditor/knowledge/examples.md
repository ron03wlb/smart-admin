# Concurrency Safety Auditor - Examples

## 範例 1: 審計報告

```markdown
# Concurrency Safety Audit Report

## Summary
- Files Scanned: 45
- Issues Found: 3
- Critical (⭐⭐⭐⭐⭐): 1
- High (⭐⭐⭐⭐): 2

## Issue 1: Check-then-act in OrderService
**Risk**: ⭐⭐⭐⭐⭐ (9.2)
**Location**: OrderService.java:156

**Problem:**
​```java
if (!orderDao.existsByOrderNo(orderNo)) {
    orderDao.insert(order);  // Race condition
}
​```

**Fix:**
​```java
try {
    orderDao.insert(order);
} catch (DuplicateKeyException e) {
    return ResponseDTO.error("訂單號已存在");
}
​```
```

## 範例 2: 修復 Check-then-act

**Before:**
```java
@Service
public class InventoryService {

    public boolean decreaseStock(Long productId, int quantity) {
        Product product = productDao.selectById(productId);
        if (product.getStock() >= quantity) {
            product.setStock(product.getStock() - quantity);
            productDao.updateById(product);  // 競態條件!
            return true;
        }
        return false;
    }
}
```

**After:**
```java
@Service
public class InventoryService {

    public boolean decreaseStock(Long productId, int quantity) {
        // 使用樂觀鎖或原子更新
        int affected = productDao.decreaseStock(productId, quantity);
        return affected > 0;
    }
}

// Dao 層
@Update("UPDATE product SET stock = stock - #{quantity} " +
        "WHERE product_id = #{productId} AND stock >= #{quantity}")
int decreaseStock(@Param("productId") Long productId,
                  @Param("quantity") int quantity);
```
