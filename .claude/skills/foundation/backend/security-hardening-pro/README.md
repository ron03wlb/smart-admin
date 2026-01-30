# Security Hardening Pro

> 企業級安全強化：SM2/SM3/SM4 國密算法 + 數據脫敏 + XSS/CSRF 防護 + 審計日誌

## 🚀 快速開始

```bash
# 場景：支付 API 需要加密
User: "Add encryption to the payment API"

# 自動配置：
# 1. 啟用 SM4 加密（api-encrypt 模塊）
# 2. 配置密鑰管理
# 3. 添加 @ApiEncrypt 註解
# 4. 生成加密/解密示例
```

## 核心功能

### 1. API 加密（SM2/SM3/SM4）

**使用 SmartAdmin `api-encrypt` 模塊**：

```java
@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    @PostMapping("/withdraw")
    @ApiEncrypt  // 自動加密請求和響應
    public ResponseDTO<String> withdraw(@RequestBody WithdrawalForm form) {
        return paymentService.processWithdrawal(form);
    }
}
```

**配置**：
```yaml
sa:
  api-encrypt:
    enabled: true
    algorithm: SM4  # SM2, SM3, SM4 國密算法
    secret-key: ${ENCRYPT_SECRET_KEY}
```

---

### 2. 數據脫敏（PII 保護）

**使用 SmartAdmin `data-masking` 模塊**：

```java
public class UserVO {
    @DataMask(type = DataMaskType.PHONE)  // 138****8000
    private String phone;

    @DataMask(type = DataMaskType.ID_CARD)  // 430***********1234
    private String idCard;

    @DataMask(type = DataMaskType.BANK_CARD)  // 6222 **** **** 1234
    private String bankCard;

    @DataMask(type = DataMaskType.EMAIL)  // a***@example.com
    private String email;
}
```

**自定義脫敏規則**：
```java
@DataMask(startKeep = 3, endKeep = 4, replaceChar = '*')
private String customField;
```

---

### 3. XSS/CSRF 防護

**使用 SmartAdmin `security-protect` 模塊**：

```java
// XSS 過濾（自動）
@PostMapping("/comment/add")
public ResponseDTO<Void> addComment(@RequestBody CommentForm form) {
    // form.getContent() 已自動過濾 <script> 等危險標籤
    return commentService.add(form);
}
```

**CSRF Token 配置**：
```yaml
sa:
  security-protect:
    xss:
      enabled: true
      exclude-urls: ["/api/webhook/*"]
    csrf:
      enabled: true
      token-header: X-CSRF-TOKEN
```

---

### 4. 審計日誌

**使用 SmartAdmin `operatelog` 模塊**：

```java
@RestController
public class PaymentController {
    @PostMapping("/withdraw")
    @OperateLog(module = "資金管理", content = "申請提款", type = OperateLogTypeEnum.UPDATE)
    public ResponseDTO<String> withdraw(@RequestBody WithdrawalForm form) {
        // 自動記錄：操作人、操作時間、操作內容、IP地址
        return paymentService.processWithdrawal(form);
    }
}
```

**審計日誌查詢**：
```sql
SELECT * FROM t_operate_log
WHERE operate_user_id = 123
  AND operate_time >= '2026-01-01'
ORDER BY operate_time DESC;
```

---

## 使用場景

### ✅ When to Use

1. **金融級安全要求**
   - 場景：支付、提款、充值 API
   - 選擇：SM4 加密 + 數據脫敏 + 審計日誌

2. **合規性審計（等保三級）**
   - 場景：需要通過安全審計
   - 選擇：完整安全強化（加密 + 脫敏 + 日誌 + 防護）

3. **敏感數據保護**
   - 場景：手機號、身份證、銀行卡
   - 選擇：數據脫敏

4. **防止攻擊**
   - 場景：SQL 注入、XSS、CSRF
   - 選擇：輸入驗證 + XSS 過濾 + CSRF Token

### ❌ When NOT to Use

1. **內部管理系統（低風險）**
   - 原因：過度安全化影響性能

2. **公開只讀 API**
   - 原因：無敏感數據，無需加密

---

## 常見問題

### Q1: SM2/SM3/SM4 與 RSA/SHA/AES 的區別？

**A**:
- **SM 系列**：中國國家密碼局標準（國密算法），等保三級必須
- **RSA/SHA/AES**：國際標準，廣泛使用

**推薦**：
- 國內業務、政府項目：使用 SM 系列
- 國際業務：使用 RSA/AES

---

### Q2: 如何自定義脫敏規則？

**A**: 使用 `@DataMask` 的參數：

```java
@DataMask(
    startKeep = 3,     // 保留前 3 個字符
    endKeep = 4,       // 保留後 4 個字符
    replaceChar = '*'  // 中間替換為 *
)
private String customField;

// 輸入：13800138000
// 輸出：138****8000
```

---

### Q3: 審計日誌的存儲和查詢？

**A**:
- **存儲**：`t_operate_log` 表（PostgreSQL）
- **查詢**：使用 `OperateLogController` 提供的查詢接口
- **歸檔**：定期歸檔至對象存儲（MinIO）

```java
// 查詢審計日誌
PageResult<OperateLogVO> logs = operateLogService.queryByPage(queryForm);
```

---

## 相關資源

- [SKILL.md](SKILL.md) - 詳細技術規格
- [SmartAdmin Patterns](../../../shared/knowledge/smartadmin-patterns.md)
- [國密算法文檔](https://www.oscca.gov.cn/)

---

**Version**: 1.0.0
**Last Updated**: 2026-01-30
**Status**: Stable
