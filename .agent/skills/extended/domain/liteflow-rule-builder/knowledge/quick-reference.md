# LiteFlow Rule Builder - Quick Reference

## EL 表達式語法

```
THEN(a, b, c)       # 串行執行
WHEN(a, b, c)       # 並行執行
SWITCH(x).to(a, b)  # 條件分支
IF(cond, a, b)      # 條件判斷
```

## Node 組件範例

```java
@LiteflowComponent("validateUser")
public class ValidateUserCmp extends NodeComponent {
    @Override
    public void process() throws Exception {
        Long userId = this.getRequestData();
        // 驗證用戶邏輯
        if (valid) {
            this.setIsEnd(false);
        } else {
            this.setIsEnd(true);
        }
    }
}
```

## Chain 定義

```xml
<!-- 訂單驗證流程 -->
<chain name="orderValidation">
    THEN(
        validateUser,
        WHEN(checkInventory, checkCredit).any(),
        calculatePrice,
        createOrder
    )
</chain>
```

## 資料庫存儲

```sql
INSERT INTO liteflow_chain (chain_name, el_data, enable)
VALUES ('orderValidation',
        'THEN(validateUser, WHEN(checkInventory, checkCredit).any(), calculatePrice, createOrder)',
        1);
```
