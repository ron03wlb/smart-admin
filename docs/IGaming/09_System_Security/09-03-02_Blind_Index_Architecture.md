# 09-03-02 Blind Index 盲索引架構 (Blind Index Architecture)

## 1. 系統概述

由於 AES 加密後的密文無法被資料庫索引，導致無法進行精確查詢（如 `WHERE encrypted_phone = '+886912345678'`）。**Blind Index（盲索引）** 是一種可檢索加密技術，通過 HMAC 單向雜湊生成可索引的雜湊值，在不洩露明文的前提下實現精確匹配查詢。

---

## 2. 架構總覽

### 2.1 數據寫入路徑（Write Path）

```
[Data Write Path]
┌──────────────────────────────────────────────────────────────┐
│  1. User Input: +886912345678                                │
│  2. Parallel Processing:                                     │
│     ┌─────────────────────┬─────────────────────┐            │
│     │ Path A: Encryption  │ Path B: Blind Index │            │
│     ├─────────────────────┼─────────────────────┤            │
│     │ AES-256-GCM(phone)  │ HMAC-SHA256(phone,  │            │
│     │ → encrypted_phone   │ blind_key) →        │            │
│     │                     │ phone_index         │            │
│     └─────────────────────┴─────────────────────┘            │
│  3. Store both in database:                                  │
│     encrypted_phone: "v1:Y3J5cHRv:ZW5jcnlwdGVk..."          │
│     phone_index: "a3f8d9e2c1b4..."  (64 chars)              │
└──────────────────────────────────────────────────────────────┘
```

### 2.2 數據查詢路徑（Query Path）

```
[Data Query Path]
┌──────────────────────────────────────────────────────────────┐
│  1. CS inputs search term: +886912345678                     │
│  2. Backend computes: HMAC-SHA256(input, blind_key)          │
│     → computed_index: "a3f8d9e2c1b4..."                     │
│  3. Query: SELECT * FROM players                             │
│           WHERE phone_index = 'a3f8d9e2c1b4...'              │
│  4. Found match → Decrypt encrypted_phone for display        │
│     → Output: +886912345678                                  │
└──────────────────────────────────────────────────────────────┘
```

**關鍵特性**：
- **單向性**：無法從 `phone_index` 反推明文 `+886912345678`
- **確定性**：相同輸入永遠生成相同 Index（可索引）
- **不可比較性**：無法進行 `LIKE`、`>`、`<` 等模糊查詢

---

## 3. 實作規格

### 3.1 需要 Blind Index 的 PII 欄位

| PII 欄位 | 加密欄位 | Blind Index 欄位 | 查詢場景 | 優先級 |
|---------|---------|-----------------|---------|--------|
| **手機號** | `encrypted_phone` | `phone_index` | 客服查找玩家、重複帳號檢查 | P0 |
| **Email** | `encrypted_email` | `email_index` | 登入驗證、找回密碼、重複帳號檢查 | P0 |
| **身分證字號** | `encrypted_id_number` | `id_number_index` | KYC 驗證、重複帳號檢查 | P1 |
| **銀行帳號** | `encrypted_bank_account` | `bank_account_index` | 提款驗證、重複綁定檢查 | P1 |
| **IP 地址** | - | `ip_index` | 風控查詢、多帳戶偵測 | P2 |

### 3.2 資料庫 Schema 設計

```sql
CREATE TABLE players (
    player_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,

    -- ========== 加密 PII（不可索引）==========
    encrypted_phone VARCHAR(255),          -- AES-256-GCM 加密
    encrypted_email VARCHAR(255),
    encrypted_real_name VARCHAR(255),
    encrypted_id_number VARCHAR(255),
    encrypted_bank_account VARCHAR(255),

    -- ========== Blind Indexes（可索引）==========
    phone_index CHAR(64) UNIQUE,           -- HMAC-SHA256 輸出（Hex編碼）
    email_index CHAR(64) UNIQUE,
    id_number_index CHAR(64) UNIQUE,
    bank_account_index CHAR(64),           -- 允許 NULL（未綁定銀行卡）

    -- ========== 元數據 ==========
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    -- ========== 索引定義 ==========
    INDEX idx_phone_index (phone_index),   -- B-Tree 索引
    INDEX idx_email_index (email_index),
    INDEX idx_id_number_index (id_number_index)
);

-- 約束說明
-- 1. phone_index/email_index/id_number_index 設為 UNIQUE：防止重複註冊
-- 2. bank_account_index 允許 NULL：玩家可能尚未綁定銀行卡
```

### 3.3 Blind Index 生成算法

**使用 HMAC-SHA256**：
```python
import hmac
import hashlib

def generate_blind_index(plaintext: str, blind_index_key: bytes) -> str:
    """
    生成 Blind Index
    :param plaintext: 明文（如 "+886912345678"）
    :param blind_index_key: Blind Index 專用金鑰（32 bytes，來自 KMS）
    :return: 64 字符的 Hex 編碼 HMAC
    """
    # 1. 正規化輸入（統一格式，避免大小寫/空格導致不同 Index）
    normalized_text = plaintext.strip().lower()

    # 2. 計算 HMAC-SHA256
    hmac_obj = hmac.new(
        blind_index_key,
        normalized_text.encode('utf-8'),
        hashlib.sha256
    )

    # 3. 返回 Hex 編碼（64 個字符）
    return hmac_obj.hexdigest()

# 範例
blind_key = kms_client.get_blind_index_key("alias/blind-index-key")
phone_index = generate_blind_index("+886912345678", blind_key)
# 輸出: "a3f8d9e2c1b4f7a6b5c8d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2"
```

**為何使用 HMAC 而非 SHA256？**
| 算法 | 優勢 | 劣勢 |
|------|------|------|
| **SHA256** | 簡單、快速 | ❌ 易受 Rainbow Table 攻擊（無金鑰保護）|
| **HMAC-SHA256** | ✅ 金鑰保護，抗 Rainbow Table | 需要額外管理 Blind Index Key |

### 3.4 完整實現範例

```python
class BlindIndexManager:
    def __init__(self, blind_index_key: bytes):
        """
        :param blind_index_key: 32 bytes Blind Index 專用金鑰
        """
        self.blind_key = blind_index_key

    def generate_index(self, plaintext: str) -> str:
        """生成 Blind Index"""
        normalized = plaintext.strip().lower()
        hmac_obj = hmac.new(self.blind_key, normalized.encode('utf-8'), hashlib.sha256)
        return hmac_obj.hexdigest()

    def store_player(self, phone: str, email: str, real_name: str):
        """儲存玩家資料（加密 + Blind Index）"""
        # 1. 加密 PII
        encrypted_phone = encryptor.encrypt(phone)
        encrypted_email = encryptor.encrypt(email)
        encrypted_name = encryptor.encrypt(real_name)

        # 2. 生成 Blind Indexes
        phone_index = self.generate_index(phone)
        email_index = self.generate_index(email)

        # 3. 寫入資料庫
        db.execute("""
            INSERT INTO players (encrypted_phone, encrypted_email, encrypted_real_name,
                                 phone_index, email_index)
            VALUES ($1, $2, $3, $4, $5)
        """, encrypted_phone, encrypted_email, encrypted_name, phone_index, email_index)

    def search_by_phone(self, phone: str):
        """透過手機號查詢玩家"""
        # 1. 計算 Blind Index
        phone_index = self.generate_index(phone)

        # 2. 查詢資料庫
        player = db.fetchrow("""
            SELECT player_id, encrypted_phone, encrypted_email
            FROM players
            WHERE phone_index = $1
        """, phone_index)

        if not player:
            return None

        # 3. 解密 PII
        return {
            "player_id": player['player_id'],
            "phone": encryptor.decrypt(player['encrypted_phone']),
            "email": encryptor.decrypt(player['encrypted_email'])
        }

# 使用範例
blind_manager = BlindIndexManager(kms_client.get_blind_index_key())

# 儲存玩家
blind_manager.store_player(
    phone="+886912345678",
    email="player@example.com",
    real_name="王小明"
)

# 查詢玩家
player = blind_manager.search_by_phone("+886912345678")
# 輸出: {"player_id": 123, "phone": "+886912345678", "email": "player@example.com"}
```

---

## 4. 雙金鑰系統 (Two-Key System)

### 4.1 為何需要分離金鑰？

**單金鑰系統的風險**：
- 若攻擊者同時獲得 **Encryption Key (EK)** + **Blind Index Key (BIK)** + **資料庫備份**
- 可建立 Rainbow Table：預先計算所有可能電話號碼的 HMAC
- 透過比對 Blind Index 找到對應密文，再用 EK 解密

**雙金鑰系統的防護**：
```
┌────────────────────────────────────────────────────────────┐
│  Key Hierarchy                                             │
│  ┌─────────────────────┬─────────────────────┐            │
│  │ Encryption Key (EK) │ Blind Index Key     │            │
│  │                     │ (BIK)               │            │
│  ├─────────────────────┼─────────────────────┤            │
│  │ AWS KMS:            │ HashiCorp Vault:    │            │
│  │ alias/pii-encrypt   │ secret/blind-index  │            │
│  │                     │                     │            │
│  │ Physical Isolation: │ Physical Isolation: │            │
│  │ US-East-1           │ EU-West-1           │            │
│  └─────────────────────┴─────────────────────┘            │
│                                                            │
│  攻擊者需同時突破兩個獨立的 HSM 才能完成 Rainbow Table   │
│  攻擊難度 exponentially 增加                               │
└────────────────────────────────────────────────────────────┘
```

### 4.2 金鑰存儲建議

| 金鑰類型 | 存儲位置 | 存取權限 | 輪替頻率 |
|---------|---------|---------|---------|
| **Encryption Key (EK)** | AWS KMS (US-East-1) | Backend API 服務 | 每 365 天 |
| **Blind Index Key (BIK)** | HashiCorp Vault (EU-West-1) | Backend API 服務 | 每 730 天（2年）|
| **Master Encryption Key (MEK)** | AWS KMS CMK | AWS KMS 內部管理 | 自動輪替 |

**為何 BIK 輪替頻率較低？**
- 輪替 BIK 需要重新計算所有歷史資料的 Blind Index（成本極高）
- EK 輪替僅需重新加密（可延遲執行）

---

## 5. 金鑰輪替策略 (Key Rotation)

### 5.1 Blind Index Key 輪替流程

**完整四階段流程**：

```
[Phase 1: 準備新金鑰 (T-30天)]
┌──────────────────────────────────────────────────────────┐
│  1. 在 Vault 中生成 BIK_v2                               │
│  2. 更新應用配置（雙版本並存）:                           │
│     - BLIND_INDEX_KEY_V1 (active)                        │
│     - BLIND_INDEX_KEY_V2 (pending)                       │
│  3. 測試環境驗證新金鑰可用性                              │
└──────────────────────────────────────────────────────────┘

[Phase 2: 雙寫模式 (T → T+90天)]
┌──────────────────────────────────────────────────────────┐
│  新資料寫入時，同時計算兩個版本的 Index:                  │
│                                                          │
│  INSERT INTO players (                                   │
│    encrypted_phone,                                      │
│    phone_index_v1,   -- 舊金鑰計算                       │
│    phone_index_v2    -- 新金鑰計算                       │
│  ) VALUES (?, ?, ?);                                     │
│                                                          │
│  查詢時優先使用 v2，若未找到則 fallback 到 v1:          │
│  WHERE phone_index_v2 = ? OR phone_index_v1 = ?          │
└──────────────────────────────────────────────────────────┘

[Phase 3: 批次遷移 (T+90天 → T+120天)]
┌──────────────────────────────────────────────────────────┐
│  Background Job 重新計算所有舊記錄的 phone_index_v2:     │
│                                                          │
│  UPDATE players SET                                      │
│    phone_index_v2 = HMAC(encrypted_phone, BIK_v2)        │
│  WHERE phone_index_v2 IS NULL;                           │
│                                                          │
│  進度追蹤: processed_count / total_count                 │
│  預估時間: 1000萬用戶 × 0.1ms ≈ 16.7 分鐘               │
└──────────────────────────────────────────────────────────┘

[Phase 4: 切換完成 (T+120天)]
┌──────────────────────────────────────────────────────────┐
│  1. 移除 phone_index_v1 欄位（ALTER TABLE）              │
│  2. 將 phone_index_v2 重命名為 phone_index               │
│  3. 在 Vault 中將 BIK_v1 標記為 Deprecated               │
│  4. 更新應用配置（移除 v1 引用）                          │
└──────────────────────────────────────────────────────────┘
```

### 5.2 批次遷移實現

**Python 批次處理腳本**：
```python
import asyncio
import asyncpg

async def migrate_blind_indexes(pool, new_blind_key: bytes, batch_size: int = 1000):
    """批次遷移 Blind Indexes 到新金鑰"""
    blind_manager_v2 = BlindIndexManager(new_blind_key)

    offset = 0
    total_migrated = 0

    while True:
        # 1. 批次讀取未遷移的資料
        async with pool.acquire() as conn:
            players = await conn.fetch("""
                SELECT player_id, encrypted_phone, encrypted_email
                FROM players
                WHERE phone_index_v2 IS NULL
                LIMIT $1 OFFSET $2
            """, batch_size, offset)

        if not players:
            break  # 所有資料已遷移

        # 2. 批次計算新 Blind Index
        updates = []
        for player in players:
            # 解密 → 計算新 Index
            phone_plaintext = encryptor.decrypt(player['encrypted_phone'])
            email_plaintext = encryptor.decrypt(player['encrypted_email'])

            phone_index_v2 = blind_manager_v2.generate_index(phone_plaintext)
            email_index_v2 = blind_manager_v2.generate_index(email_plaintext)

            updates.append((phone_index_v2, email_index_v2, player['player_id']))

        # 3. 批次更新資料庫
        async with pool.acquire() as conn:
            await conn.executemany("""
                UPDATE players
                SET phone_index_v2 = $1, email_index_v2 = $2
                WHERE player_id = $3
            """, updates)

        total_migrated += len(players)
        offset += batch_size

        print(f"Migrated {total_migrated} players...")
        await asyncio.sleep(0.1)  # 避免壓垮資料庫

    print(f"Migration complete! Total: {total_migrated}")

# 執行遷移
pool = await asyncpg.create_pool('postgresql://...')
new_blind_key = vault_client.get_secret('blind-index-key-v2')
await migrate_blind_indexes(pool, new_blind_key)
```

---

## 6. 碰撞處理 (Collision Handling)

### 6.1 碰撞機率分析

**理論計算**：
- HMAC-SHA256 輸出空間：**2^256** ≈ 1.16 × 10^77
- 根據生日悖論，需 **2^128** ≈ 3.4 × 10^38 次雜湊才有 50% 碰撞機率
- **IGaming 平台預估玩家數**：1000 萬 - 1 億（10^7 - 10^8）
- **實際碰撞機率**：幾乎為 0（< 10^-60）

**結論**：**碰撞極罕見，但仍需考慮處理策略**

### 6.2 處理策略

#### 策略 1：資料庫層 UNIQUE 約束（推薦）

```sql
-- 設定 UNIQUE 約束
ALTER TABLE players ADD CONSTRAINT unique_phone_index UNIQUE (phone_index);
ALTER TABLE players ADD CONSTRAINT unique_email_index UNIQUE (email_index);
```

**異常捕獲**：
```python
from asyncpg.exceptions import UniqueViolationError

def register_player(phone: str, email: str):
    phone_index = blind_manager.generate_index(phone)
    email_index = blind_manager.generate_index(email)

    try:
        db.execute("""
            INSERT INTO players (encrypted_phone, phone_index, encrypted_email, email_index)
            VALUES ($1, $2, $3, $4)
        """, encrypted_phone, phone_index, encrypted_email, email_index)

    except UniqueViolationError as e:
        # 檢查是否為真實重複註冊（極可能）或雜湊碰撞（極罕見）
        existing_player = db.fetchrow("SELECT encrypted_phone FROM players WHERE phone_index = $1", phone_index)

        # 解密現有玩家手機號
        existing_phone = encryptor.decrypt(existing_player['encrypted_phone'])

        if existing_phone == phone:
            # 真實重複註冊
            raise DuplicateAccountError("This phone number is already registered")
        else:
            # 雜湊碰撞（記錄 Alert）
            logger.critical(f"HMAC collision detected! Phone: {phone}, Existing: {existing_phone}")
            # 觸發人工審查或使用 Fallback 策略
            raise SystemError("Hash collision detected. Please contact support.")
```

#### 策略 2：加鹽值（Salt）避免碰撞

**在極罕見碰撞發生時，動態加入 Salt**：
```python
def generate_index_with_salt(plaintext: str, salt: str = "") -> str:
    """帶 Salt 的 Blind Index 生成"""
    salted_input = f"{plaintext}:{salt}"
    return hmac.new(blind_key, salted_input.encode(), hashlib.sha256).hexdigest()

# 若發生碰撞，使用玩家 ID 作為 Salt
player_id = get_next_player_id()
phone_index = generate_index_with_salt(phone, salt=str(player_id))
```

#### 策略 3：升級至 HMAC-SHA512（更大輸出空間）

```python
def generate_index_sha512(plaintext: str) -> str:
    """使用 SHA512（128 字符輸出）"""
    hmac_obj = hmac.new(blind_key, plaintext.encode(), hashlib.sha512)
    return hmac_obj.hexdigest()  # 128 個字符

# 碰撞機率降低至 < 10^-120
```

---

## 7. 效能考量

### 7.1 查詢效能

**Blind Index vs 全表掃描解密**：
| 方法 | 時間複雜度 | 實際耗時（1000萬玩家）|
|------|-----------|---------------------|
| **Blind Index 查詢** | O(1) - B-Tree 索引 | < 5ms |
| **全表掃描解密** | O(n) - 逐行解密比對 | > 5000ms（1000倍慢）|

**SQL 執行計劃分析**：
```sql
-- 使用 Blind Index（快速）
EXPLAIN ANALYZE
SELECT * FROM players WHERE phone_index = 'a3f8d9e2...';
-- Output: Index Scan using idx_phone_index (cost=0.43..8.45 rows=1) (actual time=0.015..0.016 rows=1 loops=1)

-- 不使用 Blind Index（慢）
EXPLAIN ANALYZE
SELECT * FROM players WHERE encrypted_phone LIKE '%...%';
-- Output: Seq Scan on players (cost=0.00..250000.00 rows=1000000) (actual time=0.015..5234.567 rows=1)
```

### 7.2 寫入效能

**額外開銷分析**：
| 操作 | 耗時 |
|------|------|
| AES-256-GCM 加密 | ~0.05ms |
| HMAC-SHA256 計算 | ~0.01ms |
| 資料庫索引維護 | ~0.5ms |
| **總計** | **~0.56ms** |

**對比無加密的純寫入**：~0.2ms
**性能損失**：約 2.8倍（可接受範圍）

### 7.3 儲存開銷

**單個玩家的 Blind Index 儲存成本**：
- `phone_index`：64 bytes（CHAR(64)）
- `email_index`：64 bytes
- `id_number_index`：64 bytes
- **小計**：192 bytes/player

**1000萬玩家總儲存**：
- 192 bytes × 10,000,000 = **1.8 GB**（可接受）

### 7.4 快取策略

**不應快取 Blind Index 查詢結果**（安全風險）：
```python
# ❌ 錯誤：快取 Blind Index → PII 關聯（洩露風險）
redis.setex(f"phone_index:{phone_index}", 3600, player_id)

# ✅ 正確：快取解密後的 PII（短期，應用層記憶體）
from cachetools import TTLCache

pii_cache = TTLCache(maxsize=10000, ttl=300)  # 5 分鐘 TTL

def get_player_phone_cached(player_id: int) -> str:
    if player_id in pii_cache:
        return pii_cache[player_id]

    encrypted_phone = db.fetchval("SELECT encrypted_phone FROM players WHERE player_id = $1", player_id)
    plaintext_phone = encryptor.decrypt(encrypted_phone)

    pii_cache[player_id] = plaintext_phone
    return plaintext_phone
```

---

## 8. 安全考量

### 8.1 已知攻擊向量

#### 攻擊 1：Rainbow Table Attack（彩虹表攻擊）

**威脅描述**：
- 攻擊者預先計算所有台灣手機號（0900000000 - 0999999999）的 HMAC
- 建立 Rainbow Table：`{HMAC: 明文}`
- 取得資料庫備份後，透過比對 `phone_index` 反查明文

**緩解措施**：
1. **金鑰輪替**（每 2 年）：使 Rainbow Table 失效
2. **加入應用上下文**（Application-Specific Context）：
   ```python
   def generate_index_with_context(phone: str, tenant_id: int) -> str:
       """混入 Tenant ID，不同租戶的相同手機號產生不同 Index"""
       context_data = f"{phone}:{tenant_id}"
       return hmac.new(blind_key, context_data.encode(), hashlib.sha256).hexdigest()
   ```
3. **物理隔離 Blind Index Key**：存儲於不同 HSM

#### 攻擊 2：Timing Attack（時序攻擊）

**威脅描述**：
- 攻擊者透過觀察 HMAC 計算時間差異推測輸入
- 若 HMAC 實現非 constant-time，可洩露部分資訊

**緩解措施**：
```python
import hmac

# ✅ 正確：使用 constant-time 比較
def verify_blind_index(user_input: str, stored_index: str) -> bool:
    computed_index = generate_blind_index(user_input)
    return hmac.compare_digest(computed_index, stored_index)  # Constant-time

# ❌ 錯誤：使用 == 比較（易受 Timing Attack）
if computed_index == stored_index:  # 不安全
    ...
```

#### 攻擊 3：Inference Attack（推論攻擊）

**威脅描述**：
- 若 Blind Index 對外暴露（如 API Response），攻擊者可透過已知明文-密文對推論未知記錄
- 範例：已知 `phone_index_A` 對應 `+886912345678`，若看到相同 Index 出現在其他資料，即可推斷

**緩解措施**：
```python
# ❌ 禁止：在 API Response 中返回 Blind Index
{
  "player_id": 123,
  "phone_index": "a3f8d9e2...",  # 洩露風險
  "email_index": "b4c7e1f3..."
}

# ✅ 正確：僅返回脫敏 PII
{
  "player_id": 123,
  "phone_masked": "091****678",
  "email_masked": "da***@gmail.com"
}
```

### 8.2 合規性評估

| 法規 | 要求 | Blind Index 合規性 | 說明 |
|------|------|-------------------|------|
| **GDPR** | PII 不可逆加密 | ✅ 合規 | Blind Index 為單向雜湊，無法反推明文 |
| **PCI-DSS** | 銀行帳號 Hashed & Truncated | ✅ 合規 | HMAC 符合 Hash 要求 |
| **CCPA** | 可搜尋性（Data Subject Request）| ✅ 合規 | 支援透過 Index 快速定位玩家資料 |
| **HIPAA**（醫療）| PHI 不可索引 | ⚠️ 需評估 | 醫療領域可能要求更嚴格的不可搜尋性 |

---

## 9. 模糊搜尋替代方案

### 9.1 問題定義

**Blind Index 限制**：
- ✅ 支援精確匹配（`WHERE phone_index = ?`）
- ❌ 不支援模糊搜尋（`WHERE phone LIKE '%886%'`）

**業務需求**：
- 客服需要模糊搜尋手機號（如輸入 "0912" 找出所有 0912 開頭的手機）

### 9.2 替代方案比較

| 方案 | 優勢 | 劣勢 | 適用場景 |
|------|------|------|---------|
| **PostgreSQL Trigram Index** | 支援模糊搜尋 | ❌ 需解密後建索引（安全風險）| 不適用於加密欄位 |
| **Phonetic Hashing** | 支援語音相似搜尋 | 僅適用於姓名（如 Metaphone）| 姓名模糊搜尋 |
| **Searchable Symmetric Encryption (SSE)** | 加密狀態下搜尋 | 效能差、實現複雜 | 學術研究，實務少用 |
| **N-Gram Blind Index** | 建立部分 Index（如前4碼）| 增加儲存成本、洩露部分資訊 | 可接受的折衷方案 |

### 9.3 推薦方案：N-Gram Blind Index

**原理**：為手機號的前綴建立多個 Blind Index

**範例**：
```python
def generate_ngram_indexes(phone: str, n: int = 4) -> list:
    """為手機號生成 N-Gram Blind Indexes"""
    indexes = []

    # 生成前4碼、前5碼、前6碼的 Index
    for i in range(n, len(phone) + 1):
        prefix = phone[:i]
        index = generate_blind_index(prefix)
        indexes.append((i, index))

    return indexes

# 範例：+886912345678
# 生成：
# - phone_index_prefix_4: HMAC("+886")
# - phone_index_prefix_5: HMAC("+8869")
# - phone_index_prefix_6: HMAC("+88691")
# ...
```

**查詢範例**：
```sql
-- 查詢所有 0912 開頭的手機號
SELECT * FROM players WHERE phone_index_prefix_4 = HMAC('0912');
```

**權衡**：
- ✅ 支援前綴模糊搜尋
- ⚠️ 增加儲存成本（每個手機號需儲存多個 Index）
- ⚠️ 洩露部分資訊（攻擊者可知哪些手機號有相同前綴）

**建議**：僅用於客服系統，不適用於公開 API

---

## 10. 相關文檔

### 系列文檔
- [09-03-01 加密策略](./09-03-01_Encryption_Strategy.md) - AES-256-GCM、密碼雜湊、金鑰管理
- [09-03-03 GDPR 數據刪除](./09-03-03_GDPR_Data_Deletion.md) - Crypto-Shredding、數據遺忘權

### 技術架構參考
- [09-02 審計日誌與審批](./09-02_Audit_Log_&_Approval.md) - Blind Index 查詢審計
- [11-01 客服中台設計](../11_Customer_Service/11-01_CS_Platform_Design.md) - 客服 PII 查詢場景

---

**文檔版本**: 1.0.0
**最後更新**: 2026-01-27
**維護團隊**: Security Team & Backend Team
