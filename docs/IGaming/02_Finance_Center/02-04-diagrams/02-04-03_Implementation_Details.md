# LockAmount 投注計算邏輯詳細文件

## 文件概述

本文件詳細說明投注交易流程中，lockAmount（鎖定金額）如何隨著投注下注、結算、取消等不同階段變化，以及有效投注（effectiveStake）的計算邏輯和業務規則。

---

## 1. 投注交易流程概覽

### 1.1 核心服務和類別

- **GridService**: 主要投注處理服務
  - 位置: `transaction-service/src/main/java/com/sit/ogp/transactionservice/service/GridService.java`
  - 負責: 投注下注、結算、取消、回滾等核心邏輯

- **GridAbstractService**: 抽象服務基類
  - 位置: `transaction-service/src/main/java/com/sit/ogp/transactionservice/service/GridAbstractService.java`
  - 提供: 有效投注計算、返水有效投注計算等共用方法

- **WalletTransaction**: 錢包交易模型
  - 位置: `transaction-service/src/main/java/com/sit/ogp/transactionservice/model/WalletTransaction.java`
  - 功能: 追蹤單次交易的錢包變化，包括 effectiveStake 和 lockAmount 的調整

- **PlayerWallet**: 玩家錢包實體
  - 位置: `common-lib/src/main/java/com/sit/ogp/common/lib/db/domain/PlayerWallet.java`
  - 欄位: cash, bonus, cleanAmount, lockAmount, effectiveStake, wagerRequirement 等

- **Transaction**: 投注交易記錄
  - 位置: `common-lib/src/main/java/com/sit/ogp/common/lib/db/domain/Transaction.java`
  - 記錄: 投注金額、派彩、有效投注、返水有效投注等

### 1.2 投注從 PlayerWallet 扣款邏輯

**檔案**: `GridAbstractService.java` (行 116-168)


**扣款優先順序**:
1. **優先使用 Cash（現金）**: 先從錢包的 cash 扣除
2. **次用 Bonus（紅利）**: 當 cash 不足時，才扣除 bonus
3. **支援多錢包**: 按錢包優先順序依次扣款
4. **主錢包保底**: 如果所有錢包餘額不足，允許主錢包透支（負值）


**範例**:

**範例 1: Cash 充足**
- Before: cash=1000, bonus=500, lockAmount=300, effectiveStake=0
- 投注金額: 100
- Deduct: deductCash=-100, deductBonus=0
- After: cash=900, bonus=500, lockAmount=300, effectiveStake=0

**範例 2: Cash 不足，需用 Bonus**
- Before: cash=50, bonus=500, lockAmount=20, effectiveStake=0
- 投注金額: 100
- Deduct: deductCash=-50, deductBonus=-50
- After: cash=0, bonus=450, lockAmount=20, effectiveStake=0

**範例 3: 混合錢包扣款**
- 主錢包: cash=50, bonus=0, lockAmount=20, effectiveStake=0
- 促銷錢包: cash=30, bonus=100, wagerRequirement=500, effectiveStake=0
- 投注金額: 150
- 步驟:
  1. 從促銷錢包扣: deductCash=-30, deductBonus=-100 (合計 130)
  2. 從主錢包扣: deductCash=-20, deductBonus=0 (剩餘 20)
- After 主錢包: cash=30, bonus=0, lockAmount=20
- After 促銷錢包: cash=0, bonus=0

---

## 2. 有效投注 (Effective Stake) 計算邏輯

### 2.1 什麼是有效投注？

**有效投注（Effective Stake，有效投注）** 是用於計算玩家是否滿足**流水要求（wagerRequirement）**的關鍵指標。它並非等同於投注金額，而是根據不同遊戲類型和投注結果計算出的「有效流水」。

**業務用途**:
- **促銷錢包解鎖**: 玩家必須累積足夠的 effectiveStake 才能將促銷錢包的資金解鎖
- **返水計算**: 根據 rebateEffectiveStake 計算玩家可獲得的返水金額
- **lockAmount 減少**: 每增加 effectiveStake，lockAmount 會相應減少（對主錢包而言）

### 2.2 有效投注計算時機

**檔案**: `GridService.java` (行 356-430)

有效投注**僅在結算時**（SETTLE）才會計算和累加到錢包，而**不是在下注時**。

**關鍵時機**:
- **投注下注時**: 不計算 effectiveStake
- **投注結算時（SETTLE）**: 計算並累加 effectiveStake
- **部分結算時（PARTIAL_PAYOUT）**: 計算差額並累加
- **投注取消時（CANCEL）**: 扣除已累加的 effectiveStake


### 2.3 有效投注計算公式

**檔案**: `GridAbstractService.java` (行 170-192)

計算邏輯根據**遊戲類型**和**投注結果**而異：

#### 2.3.1 體育類（SPORTS / E-SPORTS）


**公式**: `effectiveStake = |winAmount + lossAmount|`

**範例**:
- betAmount=100, payout=180, winAmount=80, lossAmount=0
  - effectiveStake = |80 + 0| = **80**
- betAmount=100, payout=0, winAmount=0, lossAmount=100
  - effectiveStake = |0 + 100| = **100**

#### 2.3.2 娛樂場類（CASINO）


**公式**:
- **和局** (payout == betAmount): `effectiveStake = 0`
- **贏錢** (winAmount > 0): `effectiveStake = min(winAmount, betAmount)`
- **輸錢** (winAmount == 0): `effectiveStake = betAmount`

**範例**:
- betAmount=100, payout=100 (和局)
  - effectiveStake = **0**
- betAmount=100, payout=180, winAmount=80
  - effectiveStake = min(80, 100) = **80**
- betAmount=100, payout=200, winAmount=100
  - effectiveStake = min(100, 100) = **100**
- betAmount=100, payout=0, winAmount=0, lossAmount=100
  - effectiveStake = **100**

#### 2.3.3 其他遊戲類型


**公式**: `effectiveStake = betAmount`

### 2.4 有效投注與 lockAmount 的關係

**檔案**: `WalletTransaction.java` (行 119-125)


**關鍵規則**:
- **effectiveStake 增加時，lockAmount 減少相同金額**
- 公式: `lockAmount -= effectiveStake` (當 effectiveStake >= 0)
- 這確保玩家完成流水要求後，鎖定金額會相應解鎖

**範例**:
- Before: lockAmount=500, effectiveStake=100
- 投注結算產生 effectiveStake=200
- After: lockAmount=300, effectiveStake=300

---

## 3. 不同遊戲場景的 lockAmount 變化

### 3.1 一般投注（主錢包，無 lockAmount）

**場景**: 玩家使用主錢包投注，且主錢包 lockAmount=0

**檔案**: `GridService.java` (行 65-84)

#### 投注下注


**Before 狀態**:
```
主錢包:
- cash: 1000
- bonus: 0
- lockAmount: 0
- effectiveStake: 0
- cleanAmount: 1000
```

**步驟 1: 投注 100**
- deductCash = -100
- 更新: cash = 900

**After 狀態**:
```
主錢包:
- cash: 900
- bonus: 0
- lockAmount: 0  (不變)
- effectiveStake: 0  (下注時不計算)
- cleanAmount: 900
```

#### 投注結算（贏錢）

**Before 狀態**:
```
主錢包:
- cash: 900
- lockAmount: 0
- effectiveStake: 0
```

**步驟 2: 結算 payout=180 (贏 80)**
- 計算: winAmount=80, lossAmount=0
- 計算 effectiveStake (假設 CASINO 類型) = min(80, 100) = 80
- 加回 payout: cash += 180
- 累加 effectiveStake: effectiveStake += 80
- 更新 lockAmount: lockAmount -= 80 (因為 effectiveStake 增加)

**After 狀態**:
```
主錢包:
- cash: 1080
- lockAmount: 0 - 80 = -80 → max(0, -80) = 0  (不會低於 0)
- effectiveStake: 80
- cleanAmount: 1080
```

**注意**: 由於 lockAmount 初始為 0，減去 effectiveStake 後會變負數，但資料庫更新時使用 `GREATEST(lock_amount + ?, 0)` 確保不低於 0。

#### 投注結算（輸錢）

**Before 狀態**:
```
主錢包:
- cash: 900
- lockAmount: 0
- effectiveStake: 0
```

**步驟 2: 結算 payout=0 (輸 100)**
- 計算: winAmount=0, lossAmount=100
- 計算 effectiveStake = 100
- payout=0，不加回任何金額
- 累加 effectiveStake: effectiveStake += 100
- 更新 lockAmount: lockAmount -= 100

**After 狀態**:
```
主錢包:
- cash: 900
- lockAmount: 0 - 100 = -100 → 0
- effectiveStake: 100
- cleanAmount: 900
```

### 3.2 促銷錢包投注（有 wagerRequirement）

**場景**: 玩家使用促銷錢包投注，需完成流水要求

**Before 狀態**:
```
促銷錢包:
- cash: 100
- bonus: 200
- wagerRequirement: 1500  (需完成 1500 的有效投注)
- effectiveStake: 0
- lockAmount: N/A  (促銷錢包沒有 lockAmount)
```

#### 投注下注

**步驟 1: 投注 100**
- deductCash = -100
- 更新: cash = 0

**After 狀態**:
```
促銷錢包:
- cash: 0
- bonus: 200
- wagerRequirement: 1500
- effectiveStake: 0  (下注時不計算)
```

#### 投注結算

**步驟 2: 結算 payout=150 (贏 50)**
- 計算: winAmount=50
- 計算 effectiveStake = min(50, 100) = 50
- 加回 payout: cash += 150
- 累加 effectiveStake: effectiveStake += 50

**After 狀態**:
```
促銷錢包:
- cash: 150
- bonus: 200
- wagerRequirement: 1500
- effectiveStake: 50
- 剩餘流水: 1500 - 50 = 1450
```

**進度**: 玩家需要再累積 1450 的 effectiveStake 才能完成流水要求並將此錢包轉換為可提款的資金。

### 3.3 主錢包投注（有 lockAmount）

**場景**: 玩家使用主錢包投注，但主錢包有 lockAmount（通常來自存款優惠）

**檔案**: `WalletTransaction.java` (行 62-88)

當主錢包收到存款、促銷等資金時，會增加 lockAmount：


**Before 狀態**:
```
主錢包:
- cash: 1000
- lockAmount: 500  (來自存款優惠)
- cleanAmount: 500  (1000 - 500)
- effectiveStake: 0
```

#### 投注下注

**步驟 1: 投注 200**
- deductCash = -200
- 更新: cash = 800

**After 狀態**:
```
主錢包:
- cash: 800
- lockAmount: 500  (不變)
- cleanAmount: 300  (800 - 500)
- effectiveStake: 0
```

#### 投注結算

**步驟 2: 結算 payout=300 (贏 100)**
- 計算: winAmount=100
- 計算 effectiveStake = min(100, 200) = 100
- 加回 payout: cash += 300
- 累加 effectiveStake: effectiveStake += 100
- 更新 lockAmount: lockAmount -= 100

**After 狀態**:
```
主錢包:
- cash: 1100
- lockAmount: 400  (500 - 100)
- cleanAmount: 700  (1100 - 400)
- effectiveStake: 100
```

**說明**: 每當玩家產生有效投注，lockAmount 就會減少，可提款金額（cleanAmount）就會增加。

### 3.4 混合錢包投注（Cash + Bonus）

**場景**: 投注金額跨越多個錢包，同時扣除 cash 和 bonus

**Before 狀態**:
```
促銷錢包:
- cash: 50
- bonus: 300
- wagerRequirement: 1000
- effectiveStake: 0

主錢包:
- cash: 200
- bonus: 0
- lockAmount: 100
- effectiveStake: 0
```

#### 投注下注

**步驟 1: 投注 500**
- 優先從促銷錢包扣:
  - deductCash = -50, deductBonus = -300 (合計 350)
- 剩餘 150 從主錢包扣:
  - deductCash = -150, deductBonus = 0

**After 狀態**:
```
促銷錢包:
- cash: 0
- bonus: 0
- wagerRequirement: 1000
- effectiveStake: 0

主錢包:
- cash: 50
- bonus: 0
- lockAmount: 100
- effectiveStake: 0
```

#### 投注結算

**步驟 2: 結算 payout=600 (贏 100)**
- 計算: winAmount=100, effectiveStake=100
- 派彩加回**最初扣款的錢包**（這裡是促銷錢包或主錢包，取決於實現邏輯）
- 假設加回促銷錢包: cash += 600
- 累加 effectiveStake 到促銷錢包: effectiveStake += 100

**After 狀態**:
```
促銷錢包:
- cash: 600
- bonus: 0
- wagerRequirement: 1000
- effectiveStake: 100
- 剩餘流水: 900

主錢包:
- cash: 50
- bonus: 0
- lockAmount: 100
- effectiveStake: 0
```

**注意**: 混合錢包投注的派彩分配邏輯較複雜，實際由 `getBetResultWallet` 方法決定。

---

## 4. 投注生命週期與 lockAmount 變化

### 4.1 投注下注 (Bet Placement)

**方法**: `GridService.bet(Transaction bet)` (行 63-84)

**lockAmount 變化**: **無變化**（下注時不影響 lockAmount）

**詳細步驟**:
1. 檢查餘額是否足夠
2. 從錢包扣除投注金額（cash/bonus）
3. 更新錢包餘額和 history
4. **不計算 effectiveStake**
5. **不更新 lockAmount**


### 4.2 投注結算 - 贏錢 (Settlement - Win)

**方法**: `GridService.result(Transaction tx, Transaction originBet)` (行 125-143)

**lockAmount 變化**: **減少** (減少量 = effectiveStake)

**詳細步驟**:
1. 取得投注結算的錢包（可能新建錢包以延續流水要求）
2. 加回派彩金額（payout）
3. 計算 winAmount, lossAmount, effectiveStake
4. **累加 effectiveStake 到錢包**
5. **lockAmount -= effectiveStake** (通過 `wallet.addEffectiveStake`)
6. 更新 winloss


**範例**:
- Before: cash=900, lockAmount=500, effectiveStake=0
- 結算: payout=180, effectiveStake=80
- After: cash=1080, lockAmount=420, effectiveStake=80

### 4.3 投注結算 - 輸錢 (Settlement - Loss)

**方法**: 同上 `GridService.result()`

**lockAmount 變化**: **減少** (減少量 = effectiveStake)

**詳細步驟**:
1. 不加回任何金額（payout=0 或小於 betAmount）
2. 計算 effectiveStake（通常等於 betAmount）
3. **累加 effectiveStake**
4. **lockAmount -= effectiveStake**

**範例**:
- Before: cash=900, lockAmount=500, effectiveStake=0
- 結算: payout=0, effectiveStake=100
- After: cash=900, lockAmount=400, effectiveStake=100

### 4.4 投注取消 (Bet Cancellation)

**方法**: `GridService.result()` with `TxType.CANCEL` (行 403-409)

**lockAmount 變化**: **增加** (增加量 = 原 effectiveStake)

**詳細步驟**:
1. 退回原投注金額
2. **扣除已累加的 effectiveStake**: `wallet.addEffectiveStake(bet.getEffectiveStake().negate())`
3. **lockAmount += effectiveStake** (因為 effectiveStake 為負數)
4. 更新訂單狀態為 CANCEL


**範例**:
- Before (已結算): cash=1080, lockAmount=420, effectiveStake=80
- 取消投注: 退回 betAmount=100, 扣除 effectiveStake=-80
- After: cash=980, lockAmount=500, effectiveStake=0

---

## 5. 特殊情況

### 5.1 返水計算與 lockAmount 考量

**方法**: `GridAbstractService.getRebateEffectiveStake(Transaction bet, BigDecimal effectiveStake)` (行 194-238)

**目的**: 計算玩家**可獲得返水**的有效投注金額（rebateEffectiveStake）

**業務規則**:
- 如果投注使用的是**促銷錢包**（bet.isPromotion=true），需要扣除**未完成的流水要求**
- 如果投注使用的是**主錢包但有 lockAmount**，也可能需要扣除 lockAmount（取決於系統配置）
- 目的是避免玩家在未完成流水要求前，就獲得返水


**公式**:
- **非促銷投注**: `rebateEffectiveStake = effectiveStake`
- **促銷投注**:
  - `totalRequirement = sum(wagerRequirement - effectiveStake) + (openSts ? 0 : lockAmount)`
  - `rebateEffectiveStake = max(0, effectiveStake - totalRequirement)`

**範例 1: 促銷錢包未完成流水**
- effectiveStake = 100
- 促銷錢包: wagerRequirement=1000, effectiveStake=50 → 剩餘 950
- 主錢包: lockAmount=200 (假設 openSts=false)
- totalRequirement = 950 + 200 = 1150
- rebateEffectiveStake = max(0, 100 - 1150) = **0** (無返水)

**範例 2: 促銷錢包已完成流水**
- effectiveStake = 100
- 促銷錢包: wagerRequirement=500, effectiveStake=500 → 剩餘 0
- 主錢包: lockAmount=0
- totalRequirement = 0 + 0 = 0
- rebateEffectiveStake = max(0, 100 - 0) = **100** (全額返水)

### 5.2 手續費/佣金扣除

**場景**: 部分遊戲會從派彩中扣除手續費或佣金

**處理**:
- 目前系統中手續費主要記錄在 `Transaction.ante` 和 `Transaction.tip` 欄位
- ante: 底注
- tip: 小費
- 這些費用**不影響 effectiveStake 的計算**，但會影響實際派彩金額


### 5.3 作廢投注 (Void Bets)

**方法**: `GridService.internalVoid(Transaction tx)` (行 229-264)

**lockAmount 變化**: **回復** (恢復到投注前狀態)

**詳細步驟**:
1. 退回 (betAmount - payout)
2. **扣除已累加的 effectiveStake**: `wallet.addEffectiveStake(tx.getEffectiveStake().negate())`
3. **lockAmount 增加** (因為 effectiveStake 減少)
4. 設定訂單狀態為 CANCEL，但保留原 payout 和 winAmount


**範例**:
- Before (已結算): cash=1080, lockAmount=420, effectiveStake=80, payout=180
- 作廢: 退回 (100-180)=-80, 扣除 effectiveStake=-80
- After: cash=1000, lockAmount=500, effectiveStake=0

### 5.4 部分結算 (Partial Settlement)

**方法**: `GridService.singleBetMultipleResult()` (行 147-162, 436-490)

**lockAmount 變化**: **僅在狀態改變時調整**

**業務場景**: 一個投注可能分多次派彩（例如多關投注、分段結算）


**範例**:
- 第一次派彩: payout=50, 狀態仍為 UNSETTLE
  - effectiveStake 不變, lockAmount 不變
- 第二次派彩: payout=100 (累計 150), 狀態變為 SETTLE
  - 計算 effectiveStake=100, lockAmount -= 100

---

## 6. 資料庫更新邏輯

### 6.1 PlayerWallet 更新 SQL

**檔案**: `PlayerWalletServiceImpl.java` (行 69-86)


**關鍵點**:
- **cleanAmount** 和 **lockAmount** 使用 `GREATEST(..., 0)` 確保不會低於 0
- **effectiveStake** 直接設定為新值（非累加）
- **cash** 和 **bonus** 直接設定為新值

### 6.2 WalletTransaction 到 PlayerWallet 的映射

**檔案**: `GridAbstractService.java` (行 75-114)


**注意**:
- `cleanAmount` 和 `lockAmount` 傳入的是**增量**（+ 或 -）
- `effectiveStake` 傳入的是**絕對值**
- 資料庫更新時會使用 `GREATEST` 確保不低於 0

---

## 7. 完整範例：促銷錢包流水完成流程

### 場景說明

玩家申請促銷並獲得促銷錢包，需要完成流水要求才能提款。我們追蹤整個過程中 lockAmount 和 effectiveStake 的變化。

### 初始狀態

**主錢包**:
```
- cash: 500
- bonus: 0
- lockAmount: 0
- cleanAmount: 500
- effectiveStake: 0
```

**促銷申請**: 玩家存款 200，申請促銷獲得 100 紅利，流水要求 5 倍 (300 的 wagerRequirement)

### 步驟 1: 促銷申請 (Promotion Apply)

**檔案**: `PromotionService.java` (行 69-136)

**操作**:
1. 從主錢包扣除 applyAmount=200
2. 主錢包 lockAmount += 200 (因為是 PROMOTION 類型的 deduct)
3. 新建促銷錢包


**After 狀態**:
```
主錢包:
- cash: 300
- lockAmount: 200  (新增)
- cleanAmount: 100  (300 - 200)
- effectiveStake: 0

促銷錢包:
- cash: 200
- bonus: 100
- wagerRequirement: 1500  (5 * 300)
- effectiveStake: 0
- isClosed: false
```

### 步驟 2: 促銷錢包投注 #1

**投注**: 使用促銷錢包投注 150

**After 狀態**:
```
促銷錢包:
- cash: 50
- bonus: 100
- wagerRequirement: 1500
- effectiveStake: 0  (下注時不計算)
```

### 步驟 3: 促銷錢包結算 #1

**結算**: payout=200 (贏 50), effectiveStake=100 (假設 CASINO 遊戲，取 min(50, 150)=50 但這裡假設其他規則為 100)

**After 狀態**:
```
促銷錢包:
- cash: 250
- bonus: 100
- wagerRequirement: 1500
- effectiveStake: 100
- 剩餘流水: 1400
```

### 步驟 4-15: 持續投注並結算

**假設玩家持續投注並累積 effectiveStake**:
- 總投注: 2000
- 總 effectiveStake: 1500

**After 狀態**:
```
促銷錢包:
- cash: 280  (假設最終餘額)
- bonus: 120
- wagerRequirement: 1500
- effectiveStake: 1500  (已完成!)
- 剩餘流水: 0
```

### 步驟 16: 促銷錢包關閉並轉回主錢包

**觸發**: 當 effectiveStake >= wagerRequirement 時，系統自動或手動關閉促銷錢包

**操作** (假設):
1. 關閉促銷錢包: `isClosed = true`
2. 將餘額轉回主錢包: TRANSFER 類型交易
3. 主錢包 lockAmount 減少 (因為流水已完成)

**After 狀態**:
```
主錢包:
- cash: 700  (300 + 400)
- lockAmount: 0  (200 - 200，因為完成了流水)
- cleanAmount: 700
- effectiveStake: 0

促銷錢包:
- isClosed: true
- cash: 0
- bonus: 0
```

**注意**: 實際的轉帳邏輯較複雜，lockAmount 的減少可能基於系統配置或手動調整。

---

## 8. 程式碼參考索引

### 8.1 核心檔案位置

| 檔案 | 路徑 | 主要功能 |
|------|------|----------|
| GridService.java | transaction-service/src/main/java/com/sit/ogp/transactionservice/service/GridService.java | 投注、結算、取消等核心邏輯 |
| GridAbstractService.java | transaction-service/src/main/java/com/sit/ogp/transactionservice/service/GridAbstractService.java | 有效投注計算、返水計算 |
| WalletTransaction.java | transaction-service/src/main/java/com/sit/ogp/transactionservice/model/WalletTransaction.java | 錢包交易模型 |
| PlayerWallet.java | common-lib/src/main/java/com/sit/ogp/common/lib/db/domain/PlayerWallet.java | 玩家錢包實體 |
| Transaction.java | common-lib/src/main/java/com/sit/ogp/common/lib/db/domain/Transaction.java | 投注交易記錄 |
| PromotionService.java | transaction-service/src/main/java/com/sit/ogp/transactionservice/service/PromotionService.java | 促銷申請、審核邏輯 |
| WalletTransactionService.java | transaction-service/src/main/java/com/sit/ogp/transactionservice/service/WalletTransactionService.java | 存款、提款、VIP 獎勵 |
| PlayerWalletServiceImpl.java | data-mysql/src/main/java/com/sit/ogp/data/mysql/service/impl/PlayerWalletServiceImpl.java | 錢包資料庫操作 |

### 8.2 關鍵方法索引

| 方法 | 檔案:行數 | 功能 |
|------|----------|------|
| `bet(Transaction bet)` | GridService.java:65-84 | 投注下注 |
| `result(Transaction tx, Transaction originBet)` | GridService.java:125-143 | 投注結算 |
| `internalVoid(Transaction tx)` | GridService.java:229-264 | 投注作廢 |
| `rollback(Transaction rollback, Transaction bet)` | GridService.java:164-208 | 投注回滾 |
| `deduct(...)` | GridAbstractService.java:116-168 | 扣款邏輯 |
| `getEffectiveStake(...)` | GridAbstractService.java:170-192 | 有效投注計算 |
| `getRebateEffectiveStake(...)` | GridAbstractService.java:194-238 | 返水有效投注計算 |
| `addEffectiveStake(BigDecimal)` | WalletTransaction.java:119-125 | 累加有效投注並調整 lockAmount |
| `updateWallets(...)` | GridAbstractService.java:75-114 | 更新錢包到資料庫 |
| `promotionApply(...)` | PromotionService.java:69-136 | 促銷申請 |
| `promotionApprove(...)` | PromotionService.java:139-224 | 促銷審核 |
| `deposit(...)` | WalletTransactionService.java:62-101 | 存款 |

---

## 9. 業務規則總結

### 9.1 lockAmount 核心規則

1. **產生時機**:
   - 主錢包接收 DEPOSIT, PROMOTION, VIP, RED_ENVELOPES 等資金時，lockAmount 增加相同金額

2. **減少時機**:
   - 投注結算時，**effectiveStake 增加，lockAmount 相應減少**
   - 公式: `lockAmount -= effectiveStake` (當 effectiveStake >= 0)

3. **增加時機**:
   - 投注取消或作廢時，**effectiveStake 減少，lockAmount 相應增加**

4. **最小值**:
   - lockAmount 不會低於 0 (資料庫使用 `GREATEST(lock_amount + ?, 0)`)

5. **提款限制**:
   - 可提款金額 = `cash - lockAmount` (即 cleanAmount)

### 9.2 effectiveStake 核心規則

1. **計算時機**:
   - **僅在投注結算時**計算，下注時不計算

2. **計算公式**:
   - **體育類**: `|winAmount + lossAmount|`
   - **娛樂場類**:
     - 和局: 0
     - 贏錢: `min(winAmount, betAmount)`
     - 輸錢: `betAmount`
   - **其他**: `betAmount`

3. **累加規則**:
   - 結算時: `effectiveStake += 計算值`
   - 取消時: `effectiveStake -= 原值`

4. **用途**:
   - 判斷促銷錢包流水是否完成: `effectiveStake >= wagerRequirement`
   - 計算返水金額: 基於 `rebateEffectiveStake`
   - 減少主錢包 lockAmount

### 9.3 返水計算規則

1. **一般投注** (isPromotion=false):
   - `rebateEffectiveStake = effectiveStake`

2. **促銷投注** (isPromotion=true):
   - 計算未完成流水: `totalRequirement = sum(wagerRequirement - effectiveStake) + lockAmount`
   - `rebateEffectiveStake = max(0, effectiveStake - totalRequirement)`
   - 只有在流水完成後才有返水

3. **系統配置**:
   - `REBATE_BETTING_LOCKED`: 控制是否在計算返水時考慮 lockAmount

---

## 10. 常見問題 (FAQ)

### Q1: 為什麼投注下注時 lockAmount 不變，而結算時才減少？

**A**:
- lockAmount 的減少與**有效投注（effectiveStake）**的累加綁定
- 有效投注只有在**結算時**才能確定（因為需要知道輸贏結果）
- 因此 lockAmount 也只有在結算時才會調整

### Q2: 如果玩家投注輸錢，lockAmount 還會減少嗎？

**A**:
- **會**！即使輸錢，effectiveStake 仍會增加（通常等於 betAmount）
- 因此 lockAmount 仍會相應減少
- 這確保玩家即使輸錢也能累積流水並解鎖資金

### Q3: 促銷錢包和主錢包的 lockAmount 處理有什麼不同？

**A**:
- **促銷錢包**: 沒有 lockAmount 欄位，使用 `wagerRequirement` 和 `effectiveStake` 來控制流水
- **主錢包**: 有 lockAmount 欄位，當接收促銷資金時會增加 lockAmount
- 兩者的共同目的都是**確保玩家完成一定的投注量才能提款**

### Q4: 混合錢包投注時，effectiveStake 如何分配？

**A**:
- effectiveStake **不按比例分配**，而是**全部累加到派彩錢包**
- 派彩錢包由 `getBetResultWallet` 方法決定：
  - 如果有未關閉的促銷錢包，優先派彩到促銷錢包
  - 如果促銷錢包已關閉或不存在，派彩到主錢包

### Q5: 投注取消後，lockAmount 會完全恢復到投注前嗎？

**A**:
- **是的**，投注取消會執行以下操作：
  1. 退回投注金額
  2. 扣除已累加的 effectiveStake（使用負數）
  3. lockAmount 增加（因為 effectiveStake 減少）
- 最終效果是 lockAmount 恢復到投注前的狀態

### Q6: 為什麼有些投注的 rebateEffectiveStake 為 0？

**A**:
- 當玩家使用促銷錢包投注且**流水未完成**時，rebateEffectiveStake 會被扣減或歸零
- 這是為了**防止玩家在未完成流水前就獲得返水**
- 只有當 `effectiveStake >= wagerRequirement` 時，返水才會正常計算

---

## 11. 相關文件

- [T1 Migration Checking Flowchart](T1MigrationController_checking_flowchart.md)
- [Player Wallet Management](./player_wallet_management.md) (待補充)
- [Promotion System Overview](./promotion_system_overview.md) (待補充)

---

**文件版本**: 1.0
**最後更新**: 2025-11-28
**維護團隊**: Development Team
---

**文檔版本**: 1.0.0
**最後更新**: 2026-01-28
**維護團隊**: Finance Team & Backend Team

---

## 📚 相關文檔

### 上層文檔
- [02-04 流水與對帳分析](../02-04_Turnover_and_Game_Reconciliation_Analysis.md) - 完整分析

### 相關文檔
- [02-04-01 流程圖](./02-04-01_Flowcharts_and_Sequences.md) - Mermaid 流程圖
- [02-04-02 計算邏輯](./02-04-02_Calculation_Logic.md) - 流水計算公式
