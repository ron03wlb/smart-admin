# LiteFlow Rule Builder - Examples

## 範例 1: 訂單處理流程

**需求:** 創建訂單驗證流程

**Generated Chain:**
```xml
<chain name="orderProcess">
    THEN(
        validateUser,
        checkInventory,
        IF(isVip,
            applyVipDiscount,
            applyNormalPrice
        ),
        createOrder,
        sendNotification
    )
</chain>
```

**Generated Nodes:**
```java
@LiteflowComponent("validateUser")
public class ValidateUserCmp extends NodeComponent {
    @Resource
    private UserService userService;

    @Override
    public void process() throws Exception {
        OrderContext ctx = this.getContextBean(OrderContext.class);
        boolean valid = userService.validateUser(ctx.getUserId());
        if (!valid) {
            ctx.setError("用戶驗證失敗");
            this.setIsEnd(true);
        }
    }
}

@LiteflowComponent("isVip")
public class IsVipCmp extends NodeBooleanComponent {
    @Override
    public boolean processBoolean() throws Exception {
        OrderContext ctx = this.getContextBean(OrderContext.class);
        return ctx.getUser().isVip();
    }
}
```

## 範例 2: 審批流程

```xml
<chain name="approvalFlow">
    THEN(
        submitRequest,
        SWITCH(approvalLevel).to(
            level1Approval,
            level2Approval,
            level3Approval
        ),
        finalizeApproval
    )
</chain>
```
