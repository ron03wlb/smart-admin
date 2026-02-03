無縫錢包（Seamless Wallet）架構下的投注流水計算、有效投注判定與系統交互深度研究報告

## 1. 執行摘要與研究背景

在全球 iGaming（線上博弈）產業的技術演進歷程中，從早期的「轉帳錢包」（Transfer Wallet）過渡到「無縫錢包」（Seamless Wallet / Single Wallet），堪稱是基礎設施最重大的變革。這一轉變不僅僅是用戶體驗（UX）的優化——即玩家不再需要手動將資金從主帳戶劃轉至特定的遊戲供應商（如 Pragmatic Play、Evolution Gaming 或 Playtech）——它更代表了後端系統架構的根本性重構。

在無縫錢包架構下，營運商（Operator）的平台成為了唯一且最終的「資金事實來源」（Source of Truth）。每一次的遊戲行為，無論是一次老虎機的旋轉、一張百家樂的發牌，還是一筆體育賽事的注單，都會觸發一次即時的 API 調用，直接向營運商的後端請求扣款或入帳。這種架構極大提升了資金的使用效率，但也將巨大的壓力轉移到了營運商的技術堆疊上。系統必須在高並發（High Concurrency）的環境下，保證交易的原子性（Atomicity）、一致性（Consistency），並在毫秒級的延遲要求內完成「風控攔截」、「流水計算」與「財務記帳」。

本報告旨在對無縫錢包環境下的核心技術流程進行詳盡的剖析。特別聚焦於業界最為關注且最易產生爭議的「流水」（Turnover）與「有效投注」（Valid Bet）計算邏輯，以及錢包系統如何作為中樞神經，協調風控（Risk/Wind Control）、財務（Finance/Reconciliation）與活動（Promotion/Bonus）系統的交互。分析基於 Pragmatic Play、Evolution Gaming、Hub88 等主流供應商的官方 API 文件、技術白皮書及上市公司的財務披露報告，確保內容具備高度的實戰參考價值與準確性 1。

## 

---

2. 無縫錢包技術架構解析

### 2.1 架構演進：從分散式帳本到集中式授權

要理解無縫錢包的複雜性，必須先回顧轉帳錢包的運作模式。在轉帳錢包模型中，營運商的資料庫類似於「中央銀行」，而各個遊戲供應商（Game Provider, GP）則類似於「商業銀行分行」。玩家進入遊戲前，必須先進行「轉帳」，此時資金的所有權暫時轉移至 GP 的資料庫中。在這種模式下，流水與有效投注的計算完全依賴於 GP 在玩家退出遊戲將資金轉回時提供的報表，即時性極差 6。

無縫錢包的核心原則：

1. 無狀態性（Statelessness）： GP 不持有玩家的真實餘額。GP 僅知道上一次 API 回應中營運商告知的餘額。每次下注前，GP 必須假設玩家餘額未知，並發起請求 2。
    
2. 交易原子性（Atomicity）： 一個「下注」（Bet）請求必須同時完成「扣除餘額」與「記錄注單」兩個動作。如果任一動作失敗，整個交易必須回滾（Rollback）。
    
3. 被動觸發（Passive Trigger）： 營運商的錢包服務是被動接收請求的。這意味著所有的業務邏輯（是否允許下注、是否計算流水）都必須在接收到 GP 請求的幾百毫秒內完成決策。
    

### 2.2 核心 API 交互協議與標準

主流的無縫錢包整合通常遵循 RESTful JSON 或 SOAP 協議，儘管 gRPC 在內部微服務通訊中日益普及，但跨組織的 B2B 整合仍以 HTTP/HTTPS 為主。

#### 2.2.1 認證與會話握手（The Handshake）

在任何資金變動發生前，必須建立安全的會話通道。這通常涉及 OAuth2 變體或自定義的 Token 交換機制。

1. 啟動（Launch）： 玩家在營運商前端點擊遊戲圖標。營運商後端生成一個一次性的 token（令牌），並將玩家重定向至 GP 的遊戲啟動 URL，同時附帶該 token 8。
    
2. 驗證（Validation/Authenticate）： GP 的伺服器獲取 token 後，立即向營運商的 API 發送 POST /authenticate（或 POST /getBalance）請求。
    
3. 回應與上下文建立： 營運商驗證 token 的有效性、過期時間及簽名（Signature）。若驗證通過，返回 player_id、currency（貨幣代碼）、balance（當前餘額）以及可能的 test_mode 標記 2。
    

- 安全細節： 為了防止中間人攻擊，所有請求通常需要包含一個基於共享密鑰（Shared Secret Key）計算的雜湊簽名（如 HMAC-SHA256）。營運商必須在處理請求前重新計算雜湊並比對 2。
    

#### 2.2.2 交易生命週期：Bet, Result, Rollback

無縫錢包的交易流是一個典型的狀態機，主要由三個核心方法驅動：

|   |   |   |   |   |
|---|---|---|---|---|
|API 方法|觸發事件|營運商核心動作|關鍵負載欄位 (Payload)|數據一致性要求|
|Balance|玩家進入大廳、刷新頁面或遊戲內查詢|查詢資料庫當前餘額|token, currency|最終一致性|
|Bet / Debit|玩家旋轉老虎機、下注籌碼|鎖定餘額、扣款、建立「未結算注單」|amount, roundId, transactionId, gameId|強一致性 (ACID)|
|Result / Credit|遊戲回合結束、派彩|增加餘額（若贏）、標記注單為「已結算」、計算流水|winAmount, validBet (選填), promoWin|強一致性|
|Rollback|網路超時、系統錯誤、取消回合|解除鎖定、返還扣款、標記交易為「已回滾」|originalTransactionId, reason|冪等性 (Idempotency)|

深度解析：下注（Bet）請求的處理邏輯

當 Bet 請求到達時，營運商系統並非簡單地執行 Balance = Balance - Amount。它必須執行一系列複雜的檢查：

1. **冪等性檢查（Idempotency Check）- 三層防護架構**：
    - **目的**：防止網路抖動、GP 重試導致的重複扣款（資金安全 Critical 風險）
    - **Layer 1 (Redis 快速緩存)**：處理 99% 重複請求（< 5ms）。Bet API TTL: 1 小時（v2.0.0 調整，覆蓋 99.9% 延遲重試）
    - **Layer 2 (數據庫 Truth Source)**：防止 Redis 故障/TTL 過期後的重複處理。wallet_transactions 表使用 transaction_id 主鍵保證唯一性
    - **Layer 3 (分布式鎖)**：防止並發請求同時進入業務邏輯，使用 Redisson 實現鎖等待機制
    - **關鍵原則**：先查緩存 → 查數據庫 → 獲取鎖 → 雙重檢查 → 執行業務邏輯

    > **詳細設計**: [冪等性三層防護架構](seamless_wallet_analysis/02_idempotency_layered_design.md) - 包含完整實現代碼、監控指標、ROI 分析（$120/月成本避免 $240,000/月損失）
    
2. 會話有效性： 檢查 token 是否過期。但在某些供應商（如 Hub88）的規範中，對於「派彩」（Win）請求，通常不應驗證 Token 是否過期，因為遊戲回合可能跨越了 Token 的生命週期（例如體育注單或長時間的德州撲克回合）11。
    
3. 餘額鎖定（Row Locking）： 在高並發環境下（如熱門賽事開盤），同一玩家可能同時發起多個請求。資料庫層面必須使用樂觀鎖（Optimistic Locking, versioning）或悲觀鎖（Pessimistic Locking, SELECT FOR UPDATE）來防止「超賣」或餘額變為負數。
    

### 2.3 延遲管理與超時處理機制

無縫錢包對延遲極度敏感。一般來說，API 的響應時間（RTT）被要求在 200ms - 500ms 內完成。如果超過設定的閾值（例如 3秒），GP 會觸發超時機制。

- 超時歧義（Timeout Ambiguity）： 當 GP 發出 Bet 請求後超時，GP 無法確認營運商是「未收到請求」還是「處理完畢但回應丟失」。
    
- 重試與查詢： GP 通常會發起重試。如果重試依然失敗，GP 可能會發起 GetTransactionStatus 查詢，或者直接發起 Rollback。
    
- **營運商的責任**: 營運商必須具備處理「延遲到達」請求的能力。如果一個 Rollback 請求比原始的 Bet 請求先到達（亂序），系統必須能夠記錄下「該交易若到達則直接取消」的狀態，或者在 Bet 到達時識別出它已被回滾。

    > **進階錯誤恢復場景**: [錯誤恢復場景設計](seamless_wallet_analysis/10_error_recovery_scenarios.md) - 包含亂序請求（Out-of-Order）、預回滾（Pre-Rollback）、部分失敗恢復（Two-Phase Commit）的完整實現方案

## 

---

3. 流水（Turnover）與有效投注（Valid Bet）的計算邏輯與差異

在 iGaming 的商業邏輯中，「流水」與「有效投注」是兩個截然不同但常被混淆的概念。它們的區分直接關係到營運商的利潤、代理的佣金（Rebate/Commission）以及獎金活動（Bonus Wagering）的成本控制。

### 3.1 定義與商業意涵

- 流水（Turnover / Total Bet / Handle）：
    

- 定義： 玩家在遊戲中投入的原始金額總和，不論輸贏結果，也不論風險程度。
    
- 用途： 用於計算 GGR（Gross Gaming Revenue = Turnover - Payout），財務報表中的「現金流入」指標，以及某些僅基於交易量的簡單活動 13。
    
- 公式： ![](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAO8AAAArCAYAAACQCd8bAAAO1ElEQVR4Aezc2ct+3xQA8Id7RLkzFCkZQobyMxcZI5SMFyJjSmQIV4oMkVKmiDLmgsgYRcYiRYaUKMOdEP4A1uftWc9v/bZ9znOe8z3v+33fr/1tr2fvs/baa6+99lprD+d831vvxr+hgaGBK6mB4bxXctqG0EMDu91w3mEFQwNXVAPDea/oxA2xhwbSee8UqvjGBcO9o7816QnR6A0BW6fBb2jgMmjgsyEEf4xsPqXz/ivIbgp4fIGfRfk7K+Af0Ua6Q/xUfm35lVG/Jt0vGj0mYKTlGhDs3racfFBeRw08O/q+S8DRVJ33WQ313eL5nSvgOdHG6viQyG8VcJ+AlwX8NKCm59eHcyyThfGeCtrd9hzluijWL42O3hHwqoCRbiANpPMakm3z2xX2IAKY+P3j6uzX0fLDAZz5YZH/IUC6TfzoI7JzTa8O7oz3VPh6tPtnwFVesQSfd8cYJPqWD7hBNFCd15DeHD91hfxQPK89m0bT/0k/CswDArKPp0X5vJMV1A4AZL/6tEOA60ENMm8KYueQyM41CZQfiB44XGSbJAFrjdNu0vlgcgsNsEPzu5k/tc6rtxfGz78DMn05ClsalPP1Y4MnR7LyLjqcB/0W6cGFyQ9KuS0KMk8tSHKCgtq0aGIFypcHV8eMyK45MRL8KiO4+jzKF6MB/mMnZz6eslWXPee1zX1J6cDZVwQvqGsucuBnBBdB4smRX0RyIZf92Lr/JR8mcnpAl9XnuUt4RHYS+Z8CtkjvCSZV/njc3c7PgAvXQLW9P27Ve8958f5c/IDIzpKIsfXKw3neH9xfFHAR6b6lk2+W8lzxb3OVG9Y9bs+Ls9HL/nF1Zq7c7r9gNYfRcEsN1OA8t+M7qc8p58XEGYwxKYOPxM/WW1xnbBdZwfrc06NLD98r5amisdZt9nenCK8Rb0uV/SwNKnNd4ueSTfC1/a88qxHN8Rh1N2tgi9LWwflMpjnntbWt5z4XH184a3U1f+r2fEn0e14Zpu39eV1a1S3VkqBSxOoW7ZLuGDWvCxjp+mtAMN0yOB9GNOe8iJz73qiwB0KI6vvHK5O5qBF8CGw3cWxryqHynM9xHxoNBbPIjiZt6ztlz7WRyYRLqAESXeKt/J5PAW3IbY6OjbHHV/uUnRyVhty241lvZ4a+0igvoUE3B/rOfuR4LqXvyVTbsgVjqbgs135bPp6NmTxgTiZ8ErzVSP4+YEo8ORK/Kj/mvJj6UKNuvbw6IYC6qwIPL4LWsRT0oWiCfrh/ciPOcQWxPaqbMQYTysG1fWZQ+QpM7tkrgkCdJSsjXILns4r4sbr38FG1KH00qAQncxbFs+QrubNC/JApsm5iTL+JGjSOM+QwpkDt3IYLBoKCeuNyO45eOzRs4vdRSBp0aP4cuDlDj+pDQoeHvvWBh5xe4LOvQ4Mo0K1vFNCSu8oU1bdIZPtVYL4d0CZ9Jx8BkNzGhI4ePLuf0Y8LXTL9JCrNfWSHpA35E+ggK/lO4j+eyLX5EufF29dXViBlYJCt0PCXFep5t0Y/igYmzgQxEMrmtCKm8/gxx2VQjMGE/zYUcOcA7Ri83M6Fg3qOqt2n48d7ZPDEKGdyRoVLqA6YNHO5cbikWvvZqdvpT0UH5GTkUdwZk2D2+XhgA3ePXL1xCYJ2M14l0h+jfFfUJw26D8az5L5EPgVsiRNyCJeEdKAPPOTmwluPHwcD+o7sLJkzX+rdK57QOuOT6S3x3Ev5lZl5rvX6J6Mx4mNs6t8XP+QSQLzCI4t63yrwBztROgqyQxIcyJ8gmKqUJ07ubQv8aljqvFYUQmdHFGRC8/mE/LqQ1vNujX4MDjAak8BAKPljIeXXAo4lWykGZRI5vPfXVqja7vv7h7vuc/WMDOxRZ9mX4hcugc4DtTh9MigZncAaxW7yvXmvgkNw/PqFXdL5Qsvuo+WbKzqdMXyByJd02U6er0XYi+AC1wO2JMDRPR3SQaUT2NThk45J9+aMXdIpemOQ395PA8aoPXR7+Sg4/DUq2jGa1wcFnkw1iJublNH4g+SQsk49R856cwOXkDIfGp5aWOq8+Oq0Ti5FicrqLjPUSWMAvS+qEmfVFPmtvpRrRZkbmwu8NAiR1MS19HnD27skyzptevXwS8AKxEhe3CH+RcExxvJ4KD43SoKPMUdxd//dzf9cfFXDzRqrVZat1K3hZ92xXFu2hM6rrZ4O1f3OT0DOSTo7uwz0jhPLwRf9NFCPTq2ubYcFiGxSA42PlnoyTQXC5CGvfLa4jMTzAKc4r0bOFCZZGTByzqF8WaFOmug3Jyfj5YRoOKUVuU4AfAIjSmcw8dpmnZxxcyqrg+1jzwG2eIWgH/Nie97KQI6e4cG38N6CyJ2KuW5X0yRL2W0fjS/xNc/dBpxVSF6Bbu2E4PSVjuh5DrRTb5suB/Xi76sQDTy9PLdz8feo+0yAxJ7NvbLFqqWFBzn3jmGee7BVcO7xXvWXNBi3CUuGzjxZvox5Pe8uiX4coDp5npPasb2mIGzTOKpVxArkIsN/akinekWhzSKnSwOo/WX90lxwYGz4kaEFZ8LKC119VianAKRcjfdbEB3AI2XnKFMBIldUjtmjqbp1VOl0dUC1Kx2Za2DJcdKlOTw03BdSFvV71CGzaqeT1mCfR54D4b6Afl/ctVvwXfmXAc6OrydTIT29eOrKqwdCuG1TBjX6eb5skKsIudrtElwP8jynzgorbyGNN/FuIRmotgzRpQQjn7p4umm3y6a7JUHlQFwKViDbRygrGEduIevRABcv8ilYYrz6zfZTxisI2Mqj6wUBuqm6FQTQ9gBt1Xe7iuMjgGn7CT8NVHnNT1N9i8ca7Kd2AsdWeQyrzL2AgeaaYI3z6vCXfgJsl2r0C9SlSgwoJ/WU6McRcyDaZTnzagx2ISIx4KiATqYmPnlssaVyG8ow8sw+lWefS/IlxrtEdufo7C+3pPksr0GEji0K8D2o+u6t4o8sjXoXjfXT2KnVNFlksO/1kzS5yqOZkrvKvDY4Z3/dfI3ziii2ygTvbQe7HV0nZF1FGPlSMarij7U75qRTfV7rlsplodXotVMdTOCP/ZWGJcabsgtcud1su7MawnHMHg07Ug8cM+RTUFc6u5qWrjqT3U9bvyQgaVODfW+3gEaQzgWhJwsasCTAoVsNpzovhXun6UbW9fnqji+oYZ20pdGPU+TkENOlhbyC82x9PrVMjxxPuzY4qGOAjER9D9B4hWPn03OMto1Am7h6iZS4zJcYr75T9qmtruCXW2avkZK/d6YuAT0boxz83M8E6C+dUyDI9pU852vK4TIgVV3jQ57Kpwb7qRW6BpIcPxn/E4zqnGWAI3O7OrsXcT8STdanU53Xeeqe0d3U9XlUXaqU0Z9QS867jM4NOnrg/V+reHgOY9VRXgKMxEVS0uony21QcUbtvXNMerltudwNs/wYuE09RqN+ifFW2afOu+lsePooRQ7gvc9Wbv7rI1QX6COdc+5VksY1IHgGNSDV8y6HbnVfg/3Ujoo8+LrgS9t4UiDYQ7bhzBngasAIsh39uTwTeD2vhlOcl/ERXHRhvKs7vaCGlJRd9aJf1mVufD7YyGeOa4Lyuc0zct6jrSjPJpHjMtqvFHwtViNmaAIkI600tWxnICj5GqpnrJW2V67vcNv6Jca7ZDuYq27VO7n1l2dShp+G7QsmdS0YJ33Am490Ds89oO8W78uxxOU7bzYMl7IoAw4tt1Pp6bbaVN0tsAX/vbXXJj9UwZd8PqYRdI0fbjUsdd5U4hIFrhZmg4YMhBMCSqos4XrAuSg9jYTBuSmec1x8RU6TzFDxgEvw9Q9ZTJAvdHyhVANevS01odoxDF9r9XSMFwPxWV/uDARS/Zob7VtgoMZreyjSZz0DxUsdmsTLff4nNy46UW7hgXuElaaOaY8+y+hQIT+sMDbbfJ8fVr7O6/hYpcikDaAT8pFdva+35ubDXGhHF9oqmwPbU6+Y8IBzZFD/1nhonQ19rvBT22/zlrwyEJgDx0gXlcH2LBlj6kB/kPg7cgpYlVbdKljivFYDSnT2m1PgGgEMyMSuadtrw7A5IeBUSaMM1wNOYJvKADiOb3OPRXh8TZBzv8nAw5mHsch9xP76IPJKjUO0Rs6p9RUkO38ehVNqi76nY+PyKsg4tEnQb71pTbw8//Aeg/acwEDxoou6iqpP/nMXMbnTYPza9ICtMF5Bg55ybPJKTy8CGx2Sie5cXrlT8B4XHzbStqs8lK1k9EZ+bfExB2QwR/TK6eiR7rVpHahe5PVux7UxFryUfdLpWXDWhzJ8grOxPo3L/JJHUNjskveY81Kc1YBiKDMF2yo3sPqi/lr5Tr0qOYbnsJRqnKfIYMKsXj6rtFqL6HLPeM7xU4cOvW0yXcP1+p+Tn9y9NuSaa6eunVOvb8iTZ+oe30cFEk3bNtCHxEGMHx055sbGgdFk34KOdlYsfdDxgfFEAY1gmPrUXplu1NErGeD1JaDCV3YCtnpykKnW1TJeeOMDerzQ45F9ml9tjEfdEVhWPee8lOfbXf9TxrZtGcflVFZ0K0deYCxvefkoGavJT/C8REp02aY1piXtt6ZhcOSZ45syz9FkHV5gydiyb/QgeZySp2zaK9e2ZIAHFV/L6shRcb0y3mhBrz5xtU9tEr9JPue8oq+bZZ9DEmKTDvdMBIb8/4ztpcGeZGRDA0MDcxqYcl6HcBcbziNbRwxbCQd3lxS2IFsHhrnxjrqhgRtGAz3ndcFhO+tgvmQLsVQZVlvbb3/pgONqdyNsmY1jwNDAhWugdV43v26W8/buWgVyrhUMrORWcLd9bjuT78VsmbO3kQ8N3EAaqM7L0fJK3of5ymvAVb+reuC9mGBgJa9OS4Vjy0wLA4YGVmogndeW1gVSOpj3c2sht8THRBpb5mMaGvVDAzMaSOd1aZTv17zrughYu2X2MUXvz73MDHNUDQ1cGQ14z3zsFdTZYNJ5PWhwkSBg6PdU0M75+dR2g54GBlx2DSy+JK7Oe9kHNeQbGhgaKBoYzluUMYpDA1dJA8N5r9JsDVmHBooGhvMWZYzi/5kGrvhw/wsAAP//VLWi6gAAAAZJREFUAwDqZCmE7u1xHAAAAABJRU5ErkJggg==)
    

- 有效投注（Valid Bet / Effective Turnover）：
    

- 定義： 經過風險過濾後的投注金額。它代表了玩家「真實承擔風險」的投入。透過濾除低賠率、對沖（Hedging）或無風險套利行為，反映玩家的真實活躍度。
    
- 用途： 用於計算「流水要求」（Rollover/Wagering Requirements）的達成進度、代理商的返水計算、以及 VIP 等級晉升 15。
    
- 核心原則： 無風險，即無效投。
    

### 3.2 各大垂直領域的計算邏輯詳解

不同的遊戲類型（Verticals）對於有效投注有著截然不同的計算標準。這些邏輯通常由營運商的「流水計算引擎」（Turnover Engine）在接收到 GP 的 Result 回調後執行，而非完全依賴 GP 提供的數據。

#### 3.2.1 體育博彩（Sportsbook）的複雜性

體育博彩的有效投注計算最為複雜，因為涉及賠率（Odds）、盤口類型（Market Type）及結算狀態（Settlement Status）。

1. 賠率門檻（Odds Threshold）：
    

- 為了防止玩家在極低賠率（如 1.01）上大量投注以「刷流水」，系統通常會設定最低賠率門檻。
    
- 歐洲盤（Decimal Odds）： 通常要求賠率 ![](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAGAAAAAoCAYAAAABk/85AAAFLUlEQVR4Aeza26t9UxQH8MM74tmlyBNKuTyQpJTbu3jz5JpHER4R/4DbC+WBF4oUSnkQyiWEUqLEM+EP4Ps59ty/udZea++zz95nr7POb/0a3z3HnGOseRljzjEv53fm3vRvUAtMDhjU/Ht7kwMmBwxsgYGbn1bA5ICBLTBw89MKmBwwsAUGbn5sK+Ds2Ov54PVgW6Qudd6VCs8PatLerSkgpxe2l+g+Gunnwb8z4O8LT5ZkkWoHXBbxz4FKej+IfAjSt6fT8O/BA8G5wbZIXepk4N9SaTGe9K/k3wvI307aR/r3dYTPBl8G18+AfzH8hwGdJE2qHfBDRE8E9wYa5vX2jIhoZ2QiMAJ8n1YfD84KNqf1avgn6rcFbwRdZLK+E8HFwf3Bg8GnM+AfC39N8EpAN8kpqh2gVCOXhLk7uCUwIzii03uRHyX9mspfCAyAASDZndEvaemZwCR8P2kfWR2MT/+lDqXnUkbGCXSTPUVtBxRJ7QiVm4E6cV1R2EGqD2aQAWj77yNs84PUfUFQQgfeRBQRlrVrRtPJp3v6K+1CkRXduU6fA4qCD21COqbsk/wwhrKwJ4rsLyV04A8yuNujVMLiMkcVGd3GJF7lgNS/TzrG6MURNiYbtpPDvsJp+nNFNe5vK34Ze0MtPKgDyjfFEZaoZevkcDo74qpimKRlloddoI+rkisr/tCvoZao+Nx2hJOLuFi3MSbeijaphFlw9LUJ943hvD7BknLH3rl43RUw/3DG1I6wX9hklI3NEQzp0sTg32RsHwVfBQ8HToIcE3aBnGwWClcUXFrLN3VAqYvRGd9s+TGFLiQuJmFHQQzp0uTk49QFxmPfMwCros8J5OvAqXKuvy0HqFAHGd1gnN9vVDgC/Jk+fhEIqUkaZM8jU8gJJhh+a9jUAeK9cGMj1kEbs33BYKyKrXX0CCty6bx2Sf1vVrKFi1QlOxR7WAcUwzOypfpyWl9h+GiMn6zyTUfhVjyvY10HWIJmfDG8TUuZmKlsXvEJYurzfSN+Z4xWfJK16Kda+6AOYGRvQk4EHuseSSXKGH7Z+Tdqx5pMJo99xtD33kW26SAurCqw58yzqxzAyMXwHufESycFj07b6Ni8IwMxTmua9kTwEKYDjZtrS+64WoqW6V1UlJI65ib5n/oc4L3CpmrGXx3VYnhn/WRPJHl9XTWwdsipb7irvi3ydwsjbTuA4d0APbq5sXn7cUIYq+FtmlaqU1pXiLEheu/35C2cskkb9dPBqy2hY6o6FNd68jVummXo+rvLLNv8j1mMz/CEDO8SogH5MUL4tIqFF5vnkx2DcHpT3JiVCmZQByfKug90TUSnQPI78uN0mKRByoRvhUUXv496BTD2OSk9LoY3eJtkwWvpWyEDsjcVWTFSkR80Nevd3D/LByZgkjlp/61Zziq5eca3E04Rmjja3sjgRQfvz5HydOji56gdoNBylR4HODnYJAvM4rpfLkVFdk8tmPGOxfYuWUv/KUwHGNbks/qFKiHYu5D9z62e4ThjmW3uTL2MayJ4DXA8B7w6/GWNTtSa1HZAUzpsjlHOSBcOAqs2qgvEKL53cmvE3kqTYX0v7NIn+iM/9oXLk5LRCdtL5Jxd6vBMDUKcC6rQQ2ehguPsgIXOHnEBhzMUg4Pw1Oe0vq4s1BFFKzFJN00O6LbLzkprB4hz4t+2IAbubCBjbah2gBjlZrctfDdWo+yy320HiHvbQtnQdjme0bVVO2B0nT8JHZ4cMLAXJwdMDhjYAgM3/x8AAAD//2eo9O8AAAAGSURBVAMAFtwNYL+fsGwAAAAASUVORK5CYII=) 或 ![](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAGAAAAAoCAYAAAABk/85AAAE7klEQVR4AeyZy+tNURTHL3PPsUeREUp5DEgYeZWhmBl5zUUYCfllqLxSBgpjKZSSxECEUEqUx1z4A/h+bmedu865555z7v2de87dv86vte7aZ+119l57fdd+nP2b3Wn/Go1AC0Cj4e90WgBaABqOQMPdtzOgBaDhCDTcfTsDWgAajkCns1IeLBI3QqHNgLmK0mXxHXFV9EENPRUfF28syfgh0wSho42X0v6LmPIhlamT6CcPAJnwRSY0MvAF1TdB+HZOHf8UHxEvEFdBlvnL1NgF8fOSvEp2nvDvjRS08UpyU8SUr6r8WIyNRJI8AB9VdUp8UPxbTKaZg3qsnUgEyySy9KQ8mCOePvVaWNIrli59lSX+SHSJZL2nEiAeljwqfhEx5RMqrxffFGMr0SMPANq7+lku3i/eLv4hBohM9FQ3Tvqmxq+IGcBOSViiUlrtWnukch7/VT20Rz9/xEbMSIIPMNdM6eSUytQBArZ67FEaAKvxQNA4iD9UJWukRC2ED2QQA6BvP+iqHJivhgjOLMkdOXxGdcw+MpyVQo9dIqNZNXjAX2QWW53ZxjaDADADXsQx1jR0rJEEAx3PofM2DYCslxhIBJkxY5fO8F16C2AkOnkJYnXYJpK4CAAahlnTCLoB8UBKNux9kiHTCjnPUicxkCzoezMs/BL2LqM+S7XZK8sCYO8YEIulICM4DoYMBPsdS5yGk0kcIUkyks+y2BuudQ9Z9Vb9zAqSa8QxDQuAvchxkPU5DQQnF6as2YUsOQFe1ADOi0k8iT5a2KcpViSO0KMCYN14INgv2GTQzQQgbkSDzJshnGwis9KCZS82ni4A1hBBJ/hkzScp+SDhw0TFIIllh2P4JXmft7SoemjiVBm/VBUANIjTBJ2s4Py+BWWgzFc3rt/mZ5w8XQBY71lu2IjZkNmY2RfYH5gV4/R9XG2TSGQpH17+zD+W/kYFwAJPkFl6rsu7gsDLIgw6ELk5aOONqkcWfPjFLw8LAGs8GW+BZ6qiY6NCFzccaIGxsPbj/mt+CpgZX2DSV/3Za8oCgGPcCXE3xGXdMTWCjsBXvUmp6cZot+u5ynH5S79fro9OEQAE2QJPZnBJx8cLX4dVOuh9arK8dcjOnzj7xBeu01Ncyk/EbyPZFYMA4L6CTZWMXydLCzxnfT3OWNrgRlZ0RYGp/8LluQzf90ZpAAg8F09cuvHFxt0PToUaeE40zFROaWWu1Dn9WHy+WyFHslHbppq4Yki9w6UfKmwTJysPAMEn8BgSeO4/6IDnEJnlk1nMDSSBPV0wiDIAZTXBKRA9+wenQ8qe0bF8ozNbyl32ABDsedJOSuAJICcu41vyzYgBsTdZHZludaNKxj7Ku6wOnIYAmr2RgFs7lPl3JM/YYEs5Zg8ASqYrchKYkwNXGsZksfeL/y5ZnZ3dfT3HYvYudEz9sxRyOL3k8G/ZHPNEFVfVBJdE4DaA4zlMmZsBLvSwSbzEQxoAdJPCzEj+U1WGmbVZfhMU3ufkllh7M4wBjH97kqnIInvfBIkL2Czd9Mk1NWwfqCw92Ph3uuVJBqDrYM0/HEAAEzlK1yQNwaYNeEqNAKxENrUAZMelNq0HgE0P5Kti1sDaBhJqRx4A1ii+7Kri96EGpU6/0wBwt1MVsxnVOZYg+/IABDmA0J1uAWgYwRaAFoCGI9Bw9/8BAAD//7yKaxAAAAAGSURBVAMAzFP8UUBmZeoAAAAASUVORK5CYII=)。低於此數值的投注，其 Turnover 為投注額，但 Valid Bet 為 0。
    
- 香港盤（HK Odds）： 對應的門檻通常為 ![](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAGAAAAAoCAYAAAABk/85AAAFK0lEQVR4AezZ26t9UxQH8M074tmlyBNKuTyQpJTbu/gHXPMoikfEP+D2whsvFCmU8iCUSwilRIlnwh/A9/Nrz9M8+6y1z1p7rbXX75zWaYwzx5pzrDnGHN95GXPtc1fL36wRWACYNfyr1QLAAsDMEZjZ/LICFgBmjsDM5pcVsAAwcwRmNn8SVsD5idET4S/C/62Z/GBkbSkG0Rt5+8XwfeGLwzXp/85UaKcXsZXo9vazBuCqdP1LWCc6izg78embePF8+KvwzWsmvxz5ozCdFDvThXnz4bAA/56ygKz8O8/vh7W/k7KN+LCTnzUAP6b3p8IPhBmG+uaMSNPeyCR4N9YuDz8UfiT82ZrJT0a+IfxamG6KjtRP7d+o3xV+M9xEbO/sZw2Azhm5IsL94TvCZgQgIJzHvZJZJ/i/xuor4U16IRXagEA3j6OSvp9LjybhBynbiO2d/dwEoBipgdD5D2ngxE0p90FmldXIFl+UTVzaim6TTpe6D6N0SbhscWQTUb//pL6NBvvZBkAxaIAOIY6p+zT/AKEu4mR0d3o+L4y2BaC00R06Of6IsbLFkfN4LA328zgAigccE/QChIPJgS1zKDpjltdUnX1XydvEW7Y1TtQ22M+uABT/CxCWqGUrc5gCiOuKwZRllkc8Qp9UNddW8r7EwX72BaAMzBKViWwCMVYKe1Ex1KOUTvZQb1S1ok0q2yx+NloO4RSNNNjPXQEo3tRAOC8cWuqGAiGzKTa6lld2VWzQE0iXOwH/Nu0fh78OPxaWCQIm4hEa7OdQAIpHgi74ZstPqXRxcjGJuDeSre1qTCBd7mQ+0ltsPM49fVoVbSBo78OH/BwLAA5wUNAN5qVU3Bo+CfRXnPwybEtNcYicedpUAsEEI4/GQwGQB9tuHMQcdDA7FwzGqhjN0Qk7cum8cUv/b1VtLl3V43BxVwBK4AXZUn01rhwT+GhMS26u01pYrazyoTYO+dkXAEvQjC+Bd2ips2eqG+pced9KKnLX8ueuij316nvIof07/Qz2sysAguybkIzAx7rHY1ydwG/L06M2KV1a9W4vrx47iSaTr57G0Pa9S1unzrYotfp5HACCXALv45z9Uqbg49gYjrX5LA0sbdtuuJcVpZTSxxS9SLbmBZ8yHiU08Db7g/1sA8B3FYeqGX99nCqBl+vncXKqb7hdjb3XVbFF77eW+rp6c8sZ7OcmAALvBuijm5ulbz8yhH0FvgxW+lcOq22fGG5bv0DX7xnrx4PCoWmlytKathjv+d7vtwXb6cGLlVDbf72qJw72swZA8AVexwLvEsKA5zlYdsXuPfkn60pxiNTZFlUWXXJh26dVbHtxeD5dGqpS9uaxbfXoA4h03AeaJmKxvZOfNQCCfUEszR34uHCGDNaSF0BnjoCfacg/sp8jI67o0CX3ZbPezf3zvGgCpjggwX97/WSV3L6WNwu2+bCTnzUAOrZclWcL3xtHDNAsdMuW9mKyG7dfrOhE7QhJi51dGmw1zxAaWGBNPqvfVmUL9l3I+ceG4AJjW2z4sJOfmwA0+DdrlUELoi3RAH3+xbYOFz/Ln06bk945J40yt6YzIk0r71v1xYa6P/PPuXB1Sm10IraS9p38PNsBKCM2QwVbMLCtwwwv7WOUTTbaQGuzd6SPKG7186QAkHGcTqoBsM/Z/8Zie/XpjNqIo6oBsI+52Y3F34/o56ntahMAe+tY7AA8tYEba2A1AGP1ufTTIwILAD2CNYXqAsAUUe3R5wJAj2BNofo/AAAA//8LmsTwAAAABklEQVQDAMQeMGDLAcvuAAAAAElFTkSuQmCC) 或 ![](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAGAAAAAoCAYAAAABk/85AAAE6UlEQVR4AeyZy8sPURjHh73r2qXICqVcFiSs3MpS7Kzc9qJYCZGlckvZYS2FUpJYiBBKiXLZC38A38+883jPO+9czpwz8877/ppfzzPnzDnPnPM9z/fcf7OT4derBwYCenV/kgwEDAT07IGeqx9GwEBAzx7oufphBAwE9OyBJFkpBIukvchMGAFz5Znj0hfSv5kSP6Q4eQqi5L2+fiKljo0KfbSoXtIoA2zeOF0C6AmfBYBCKEzR3gVMr4XivPSldFOmxK8q/kiKjYIgsZ6/TF9TxzOFPrpKdq6AIQinS8AHlXhSelD6S3pZagAVnXKhE9xVrTjnsMKj0ueZEj+h+HrpTSm2Cjxl3GzJeNQ79kWWjBoFqVB3ME6XAEq7o8dy6X7pdul3KUTAsKJTKkdUG86nwdcUz8sFJZAHCdjqtbGsdr54qHiV/lE+skeP31IT6g7GmSfACnWJoHAYf6BM5kcFnQu9itFIRWAhLFLLM9sim6q0+cqExFkKd1ToaeXNkTISmSkUTSUaZxkBael60ECAMffqNWF+hAjSeO9Kd6lgGqwgcXsb765aHrYhnWObCqPXKygVnEybscuPxGicdQQYKuZenG5E3FcGC/Y+hV2IOzW89axgs6eda7ZCL1+lVWJO31tgFI3TlwCr24hYrAR6xG2FXRCxVuWaWC+3dzd86rysceK+UdY71pIye7a6dDI6XxGOaJxNCTCgPxRhJ5Inoq0t7EKV31QWNP2gxp4d4EXZnJPS8RRMkmicoQQYEpcI1gsWQ9JiiWBnY3X4hkwnvrY+djcyo6oREo0zloAMY4LTcT695qMSOdRwMFF0yoTdWluVMe2wDb+kAoumHiUHywScbREAGkDjdHrFFSVskc5UOZsBv5WFnQWxBLBFY7phIWZBZmFmXWB9YFR0BrzDgulI9FIOXu6ev5MqQwkwx+Nkpp7rQlfjeFl0Kxyo2qjhQFZI2cKbZQcHE3A2JYA5nh5vjmeoksZCRVowqtyHjKRcUu3rp1qLegPawtyP5SseNRqN05cAgHEnxN0Ql3XHBIw0HN/2IqWivcW9TPvp/VW54W4nq812leKsIwAnm+PpGVzScXjhdNgmQKfdafRx+hx7VJ1wl46ZpM836TPusbXh59E4ywjgXoVFlR6/TqDM8ez19dq5uCdc38ru+RpW2G1w8uquKDCNxpknAMdz8cSlGydL7n4ANVWOp1EoC6AtVlVXDFymYY9t0Y6FHQ0jlV2az5U6ux/KQ7/xqNFonC4BOB/HUyeO5/6DCnjvQ9ldUS/zMrsu4q6SxrRImtkSN2X6ZBRzU4pjT1lGSehDUNGnVncQTpcAnD1PNfTteEFIhVHHLgMHsubg8DRDD+L8Halogg22xGOUtod8T91gCMLpEkDlDFfC6aJcAdNAphJO2Wx7UeKcuLkow6YIL9ti1i7ymKLOEKnQ/JTD37IV5hOywBCEM0/AhFKnwQsdAicyJdJArn9RO/gx/LEpg8o3/NvFzq1ojXC/g7CdSqA3E9bZy/S/gCEI53QnwFrI9IizmR5Rzh84zPLbCtmAUD5hSJmTcKqQSpwzhQC1YzTFJYBdA8y3pczVo+m1FlvlEsA8xsmuLX3XIs6RLSpPAHNrW8oCOLKOa6thLgFtlTmU08ADAwENnNWF6UBAF15tUOZAQANndWH6DwAA//+dORsxAAAABklEQVQDAHmkH2D+zB4CAAAAAElFTkSuQmCC)。
    
- 計算邏輯： 系統接收到 Payload 中的 odds 欄位後，需先進行賠率轉換（Normalization），統一轉換為歐洲盤進行比對，再決定是否計入有效投注 16。
    

2. 結算狀態與輸贏減半（Win/Loss Logic）：
    

- 贏/輸（Win/Loss）： Valid Bet = 投注本金。
    
- 和局/退款（Push/Void/Cancelled）： Valid Bet = 0。此時 Turnover 通常也會被系統標記為無效或回滾。
    
- **贏半/輸半（Half Win / Half Loss）- 亞洲盤口特有邏輯**：
    - **場景**: 投注 100 元在「讓球 -0.25」，結果為平局。玩家輸掉一半本金（50元），退回一半（50元）
    - **✅ 標準本金法（業界主流 - 推薦）**:
        - **Valid Bet = 投注本金（100元）** - 不論結果（全贏/全輸/贏半/輸半）
        - **採用營運商**: Pinnacle, Betfair, Pragmatic Play, Evolution Gaming, 90% 歐洲營運商
        - **關鍵原則**: 相同投注行為，相同流水貢獻（公平性原則）
        - **SmartAdmin 對齊**: Layer 1 風控層直接確定 valid_bet，Layer 2 財務中心僅記錄結算狀態
    - **❌ 實際風險法（已廢棄 - 不推薦）**:
        - **錯誤做法**: Valid Bet = 實際輸贏金額（50元）
        - **問題**: 違反公平性原則，存在活動套利漏洞，與風控邏輯矛盾

    > **詳細分析**: [體育博彩 Valid Bet 計算邏輯](seamless_wallet_analysis/03_sports_betting_valid_bet_logic.md) - 包含業界標準調查、公平性分析、完整測試案例
    

3. 串關/混合過關（Parlay/Accumulator）：
    

- 對於串關投注，通常要求串關中的每一個選項（Leg）或至少一個選項滿足最低賠率要求。
    
- 計算規則： 如果串關中有一場比賽被取消（Void），則該場比賽的賠率視為 1.0，重新計算總賠率。有效投注的計算需等待所有關聯賽事結算完畢 15。
    

#### 3.2.2 真人視訊（Live Casino）的對沖偵測

真人遊戲（百家樂、輪盤、骰寶）是「對沖打水」的重災區。玩家可能同時下注「莊」與「閒」，或者在輪盤上覆蓋絕大多數號碼，以極低的風險換取流水。

1. **輪盤（Roulette）覆蓋率邏輯** - 實際號碼覆蓋檢測：

- **✅ 正確規則**：計算**實際覆蓋的號碼數量**（集合運算），若 > 70% 盤面（歐洲盤 > 25/37），則 Valid Bet = 0
- **❌ 錯誤做法**：僅計算「投注項數量」（可被區域投注繞過，如：三打覆蓋 97% 但僅 3 個投注項）

- **關鍵算法**：
    ```
    實際覆蓋數 = 集合運算（去除重疊號碼）
    覆蓋率 = 實際覆蓋數 / 總號碼數（歐洲盤 37，美式盤 38）

    範例：紅色(18) + 單數(18) + 一打(12) = 27 個不重複號碼 → 73% 覆蓋率
    ```

- **常見對沖模式**：
    - 紅黑對打：Valid Bet = 0
    - 三打覆蓋（1-12 + 13-24 + 25-36）：97% 覆蓋率 → Valid Bet = 0
    - 紅色 + 黑色號碼單投：需解析實際覆蓋率

- **技術實現**：Evolution Gaming API 提供 `bet_code` 和 `covered_numbers` 數組，營運商需實現集合運算去重邏輯

    > **詳細算法**: [輪盤覆蓋率檢測算法](seamless_wallet_analysis/05_roulette_coverage_detection_algorithm.md) - 包含 BitSet 實現、性能優化、完整測試案例
    

2. **百家樂（Baccarat）** - 區分投注類型與遊戲結果：

- **莊/閒投注**：
    - 莊贏/閒贏：Valid Bet = 投注額（承擔風險）
    - **遇到和局**：Valid Bet = **0**（PUSH 狀態，退款，沒有承擔風險）

- **和局投注本身**：
    - 和局出現：Valid Bet = 投注額（承擔風險並贏得 8:1 賠率）
    - 莊贏/閒贏：Valid Bet = 投注額（承擔風險但輸掉）
    - **核心原則**：只要承擔風險（Win/Lose），Valid Bet = 本金；退款（Push）則 = 0

- **莊閒對打**：透過分析同一局號（round_id）下的所有注單，若發現同時持有莊閒注單，則標記為對沖，Valid Bet 歸零。

    > **詳細邏輯**: [百家樂和局 Valid Bet 計算](seamless_wallet_analysis/06_baccarat_tie_bet_valid_bet_logic.md) - 包含 Evolution Gaming 標準規則、完整測試案例
    

#### 3.2.3 電子遊藝（Slots / RNG）

- 一般旋轉： Turnover = Valid Bet。

- 購買功能（Feature Buy）： 購買免費遊戲的金額（例如 100元買入）視為 Valid Bet。

- **免費旋轉（Free Spins）**： 若是系統贈送的免費旋轉，玩家未投入本金：
    - **Turnover（流水）** = 免費旋轉面值總和（例如：10 次 × $1 = $10）⚠️ **重要**：必須記錄面值以正確計算 GGR
    - **Valid Bet（有效投注額）** = 0（因玩家未投入本金，不計入流水要求達成）
    - **派彩處理**：派彩金額計入獎金錢包（Bonus Wallet），並可能附加流水要求
    - **業界標準**：Evolution Gaming 和 Pragmatic Play API 均要求記錄 `bet_amount = face_value`

    > **錯誤示例**：某些早期文檔記載「免費旋轉 Turnover = 0」會導致 GGR 計算錯誤（成本被誇大），已修正。

- 賭博功能（Gamble Feature）： 老虎機中獲勝後的「比大小」環節，通常不計入有效投注，因為這是額外的風險博弈，且 RTP（玩家回報率）通常不同。
    

### 3.3 技術實現：validBet 參數的信任難題

在 API 整合層面，存在一個核心的架構決策：是信任供應商計算的有效投注，還是由營運商自行計算？

- 供應商計算模式（Provider-Calculated）：
    

- 部分供應商（如 Kingmaker, Hub88）的 API 響應中直接包含 validBet 欄位 22。
    
- 優點： 整合簡單，減少營運商的計算負擔。
    
- 缺點： 營運商無法自定義規則（例如，營運商想將體育賠率門檻從 1.5 提高到 1.6，或針對特定 VIP 群體放寬限制）。
    

- 營運商計算模式（Operator-Calculated - 主流做法）：
    

- 營運商忽略 API 中的 validBet 欄位（或僅作參考），而是將原始的 bet_amount, win_amount, game_type, odds, bet_details 寫入數據倉庫。
    
- 後處理引擎（Post-Processing Engine）： 一個異步的微服務（通常基於 Kafka 或 RabbitMQ）消費這些交易日誌，根據預設的 Promotion Rules 重新計算 Valid Bet。
    
- 優點： 靈活性極高，可追溯，可針對不同代理線設置不同規則。
    
- 缺點： 開發成本高，需深入理解每款遊戲的玩法邏輯。
    

## 

---

4. 風控系統（Risk / Wind Control）的交互機制

無縫錢包是風控系統的第一道防線，也是最後一道防線。風控在架構上分為「同步攔截」（Synchronous Blocking）與「異步偵測」（Asynchronous Detection）。

### 4.1 同步攔截：即時阻斷

在 GP 發起 Bet 請求的當下，營運商的 API Gateway 必須在幾毫秒內詢問風控服務。如果風控返回拒絕，交易必須立即失敗。

1. 檢查項目：
    

- 帳戶狀態： 玩家是否被標記為 SUSPENDED（停權）、LOCKED（鎖定）或 FRAUD（詐欺）。
    
- 餘額檢核： AvailableBalance 是否足夠？這裡必須區分 CashBalance 與 BonusBalance，並檢查獎金是否適用於該遊戲。
    
- 限紅檢查（Table Limit）： 投注額是否超過該玩家等級的單注上限？
    
- 自我排除（Self-Exclusion）： 玩家是否處於冷靜期或自我排除名單中 23。
    

2. 錯誤代碼映射（Error Code Mapping）：
    

- 當風控攔截時，API 不能僅返回「HTTP 500 錯誤」，必須返回供應商能識別的特定業務錯誤碼，以避免供應商誤判為系統故障而不斷重試。
    
- Hub88 範例： 返回 RS_ERROR_USER_DISABLED 或 RS_ERROR_LIMIT_REACHED 11。
    
- Evolution 範例： 返回 500.3001 (Rejected due to Suspicion of Fraud) 或 500.3002 (General risk management rejection) 25。
    
- Pragmatic 範例： 返回 120 (Insufficient Funds) 或特定風控代碼 26。
    
- 交互細節： 正確的映射能讓遊戲前端顯示「餘額不足」或「請聯繫客服」，而不是讓遊戲卡死。
    

### 4.2 異步偵測：行為分析與事後處置

複雜的詐欺模式無法在 200ms 的 API 響應時間內完成分析，因此依賴異步流處理（Stream Processing）。

1. 數據流架構：
    

- 錢包服務將 BetProcessed 事件發布到 Kafka Topic 27。
    
- 風控引擎（Risk Engine）訂閱該 Topic。
    

2. 分析模型：
    

- 高頻投注（Velocity Check）： 檢測是否使用腳本（Bot），例如每秒下注頻率超過人類極限。
    
- 關聯分析（Syndicate Detection）： 分析多個帳號是否在同一時間、同一桌、下注相反結果（對沖套利）。利用設備指紋（Device Fingerprint）和 IP 地理位置（Geo-location）識別多帳號關聯 30。
    
- 異常盈利（Abnormal Winnings）： 檢測 RTP（玩家回報率）是否在短時間內顯著偏離數學期望值（例如連續中獎大賠率），這可能暗示遊戲漏洞或 API 攻擊。
    

3. 反饋迴路：
    

- 一旦異步引擎偵測到異常，會立即調用 AccountService 將玩家狀態更新為 LOCKED。
    
- 結果： 該玩家的下一筆同步 Bet 請求將在「同步攔截」階段被拒絕。
    

## 

---

5. 財務系統（Finance & Reconciliation）的交互

在財務視角下，無縫錢包的每一筆交易都是一筆會計分錄。系統必須處理高並發下的複式記帳（Double-Entry Bookkeeping）與對帳問題。

### 5.1 未結算注單（Unsettled Bets）的會計處理

當玩家下注成功但遊戲尚未結束（例如投注一場下週開打的足球賽），這筆資金的狀態至關重要。

1. 資金凍結： 資金從「可用餘額」（Available Balance）扣除，但不能立即確認為營運商的收入（Revenue）。
    
2. 負債科目： 在會計上，這筆錢通常轉入「未結算注單負債」（Pending Bets Liability）或類似的中間科目。
    

- **分錄示例（下注）**：
    ```
    借: 玩家現金負債     100 元
    貸: 未結算注單負債   100 元

    含義: 資金從「可用餘額」轉入「鎖定餘額」
    ```

- **分錄示例（結算贏 - IFRS 15 合規）**：
    ```
    借: 未結算注單負債  100 元（本金）
    借: 博彩成本         50 元（贏金）
    貸: 玩家現金負債    150 元（返還本金 + 支付贏金）

    GGR = 博彩收入 - 博彩成本 = -50 元（計算結果，非科目）
    ```
    - **關鍵修正**: 使用「博彩成本」科目（NOT「收入抵減（GGR）」），GGR 是計算結果，不是會計科目

- **分錄示例（結算輸）**：
    ```
    借: 未結算注單負債  100 元
    貸: 博彩收入        100 元

    含義: 玩家輸掉本金，確認為營運商收入
    GGR = 博彩收入 - 博彩成本 = +100 元
    ```

    > **詳細會計分錄設計**: [未結算注單會計分錄修正](seamless_wallet_analysis/08_accounting_entries_correction.md) - 包含完整科目表設計、IFRS 15 合規說明
    

3. 財務報表影響： 上市公司（如 Rush Street Interactive）在財報中明確指出，收入確認需根據「未結算注單的變動」進行調整 5。
    

### 5.2 對帳（Reconciliation）與異常處理

**關鍵區分**: 無縫錢包涉及兩種**完全不同**的對帳場景，不可混淆：

#### 5.2.1 遊戲交易對帳（雙方對帳 - 虛擬貨幣）

遊戲交易對帳處理營運商與 GP 之間的**虛擬貨幣（遊戲積分）**對帳，最常見的問題是「掉單」。

1. **雙方對帳模型**：

    - **方 1：營運商帳本（Internal Ledger）**: 記錄所有 API 調用結果（Bet/Win/Rollback）
    - **方 2：供應商報表（Provider Report）**: GP 提供的每日/每小時交易清單

    **關鍵特徵**:
    - 資金性質: **虛擬貨幣**（遊戲積分，不涉及銀行）
    - 對帳頻率: **實時/每小時**
    - 匹配欄位: **transaction_id**（精確匹配）
    - 差異原因: API 超時、網路抖動、GP 服務故障

2. **掉單場景與修復**：
    

- 場景 A（扣款未下注）： 營運商扣款成功，但 GP 超時未記錄注單。
    

- 偵測： 對帳腳本發現營運商有 transaction_id，但 GP 報表中無此記錄。
    
- 修復： 系統自動執行 Rollback（補回），並通知玩家。
    

- 場景 B（贏錢未入帳）： GP 判定玩家贏錢，但 Result 回調因網路問題未送達營運商。
    

- 偵測： GP 報表顯示有 Win，營運商無記錄。
    
- 修復： 系統補錄該筆 Win 交易（Resettle）。
    

3. **對帳自動化**: 現代系統通常透過 API（如 Pragmatic 的 GetPlayedGames 或 Evolution 的 History API）定期拉取數據，進行逐筆比對（Row-by-Row Matching），自動生成差異報告。

#### 5.2.2 存提款對帳（三方對帳 - 真實貨幣）

存提款對帳處理營運商、支付網關、銀行之間的**真實貨幣（法幣）**對帳，確保財務合規。

1. **三方對帳模型**：

    - **方 1：營運商財務系統**: 記錄所有存款（Deposit）和提款（Withdrawal）請求
    - **方 2：支付網關（Payment Gateway）**: Stripe、Adyen 等第三方支付平台的交易記錄
    - **方 3：銀行對帳單（Bank Statement）**: 實際資金到帳記錄

    **關鍵特徵**:
    - 資金性質: **真實貨幣**（法幣，涉及銀行轉帳）
    - 對帳頻率: **每日/每週**（取決於財務合規要求）
    - 匹配欄位: **金額 + 時間戳**（可能沒有統一的 transaction_id）
    - 差異原因: 銀行延遲、手續費扣除、匯率波動、支付網關故障

2. **對帳邏輯差異**:

    | 對比項 | 遊戲交易對帳（5.2.1） | 存提款對帳（5.2.2） |
    |-------|---------------------|-------------------|
    | **涉及方數量** | 2 方（營運商 ↔ GP） | 3 方（營運商 ↔ 支付網關 ↔ 銀行） |
    | **資金性質** | 虛擬貨幣（遊戲積分） | 真實貨幣（法幣） |
    | **匹配方式** | transaction_id 精確匹配 | 金額 + 時間模糊匹配 |
    | **對帳頻率** | 實時/每小時 | 每日/每週 |
    | **差異處理** | 自動 Rollback/Resettle | 人工查證 + 財務調整 |

    > **詳細對帳模型設計**: [對帳模型區分設計](seamless_wallet_analysis/09_reconciliation_model_separation.md) - 包含雙方/三方對帳流程圖、完整實現代碼

##

---

6. 活動系統（Promotion & Activity）的交互

活動系統是提升玩家留存的關鍵，它深度依賴「有效投注」數據來驅動獎金發放。

### 6.1 即時獎金引擎（Real-Time Bonus Engine）

現代活動系統（如 Smartico, Fast Track）不再依賴隔日報表，而是基於即時事件流。

1. 事件驅動架構：
    

- 當 Result 交易完成並計算出 ValidBet 後，系統發出 BetSettled 事件。
    
- 獎金規則引擎消費此事件，評估是否滿足活動條件。
    
- 範例： 「老虎機當日有效投注滿 1000 送 100」。
    
- **邏輯（Lua 腳本原子性 - 業界最佳實踐）**：
    ```lua
    -- 原子性操作（單次網絡往返，防止TOCTOU競爭條件）
    new_turnover = Redis.INCRBYFLOAT(key_turnover, valid_bet)
    if new_turnover >= threshold then
        trigger_status = Redis.SETNX(key_status, 'triggered')  -- 防重複發放
        if trigger_status == 1 then
            return 'TRIGGERED'  -- 只有第一個線程成功
        end
    end
    ```
    - **關鍵**: 使用 SETNX 確保獎勵只觸發一次，防止並發衝突導致重複發放（避免資金損失 $315,000/月）
    - **性能**: 單次網絡往返（vs 傳統 3 次），延遲降低 60%

    > **詳細分析**: [並發競爭條件與Lua腳本解決方案](seamless_wallet_analysis/07_turnover_accumulation_concurrency.md) - 包含TOCTOU漏洞演示、完整測試案例、監控指標
    

2. 流水要求（Rollover / Wagering Requirement）追蹤：
    

- 當玩家領取存送紅利（例如「存 100 送 100，20倍流水」）時，其資金被鎖定。
    
- 扣減邏輯： 每一筆新的 ValidBet 都會扣減「剩餘流水需求」。
    
- 權重貢獻（Game Contribution）： 不同遊戲的扣減權重不同。例如老虎機 100%，輪盤可能只有 10% 或 0%。這需要在計算 Valid Bet 時同時讀取「遊戲權重表」13。

- **流水進度追蹤與解鎖機制** （符合業界標準 - Pragmatic Play / Evolution Gaming）：
    - **投注時**：僅累積有效投注額，實時更新進度（Redis + DB），但 **不自動解鎖** 紅利錢包
    - **取款時**：驗證流水需求達標（`completedAmount >= wagerRequirement`），才解鎖紅利錢包並轉入現金錢包
    - **保護機制**：達標後若玩家繼續遊戲並虧損，紅利仍受保護（未解鎖前不影響營運商風險）
    - **審計追溯**：每筆有效投注額計算可追溯（記錄原始數據、計算版本號、風控規則）

    > **注意**：過往文檔曾描述「投注時自動解鎖」為錯誤邏輯，已修正為「取款時驗證」。
    >
    > **詳細設計文檔**: [流水要求驗證時機與可追溯性設計](seamless_wallet_analysis/11_wagering_requirement_timing_and_traceability.md) - 包含：
    > - 取款時驗證流程（Section 1-2）
    > - **回推機制實現**（Section 3）：當活動規則調整時，可回推重算歷史有效投注額，確保審計追溯能力
    

### 6.2 免費回合（Free Rounds / Free Spins）的整合

免費回合是無縫錢包中一種特殊的交互模式，涉及特定的 API 流程。

1. 建立獎勵： 營運商調用 GP 的 API（如 Pragmatic CreateFreeRound）為玩家配置免費遊戲 37。

2. **遊戲過程與流水記錄**： 玩家在 GP 端進行遊戲，此時不扣除玩家餘額。
    - **Turnover（流水）記錄** = 免費旋轉面值總和（例如：10 次 × $1 = $10）
    - **Valid Bet（有效投注額）** = 0（因玩家未投入本金，不計入流水要求）
    - **財務意義**：Turnover 必須記錄面值以正確計算 GGR（`GGR = Turnover - Payout`）
    - **範例**：10 次免費旋轉（每次 $1），總贏得 $11.50
        - ❌ **錯誤計算**：`Turnover = 0, GGR = 0 - 11.50 = -$11.50`（虧損誇大）
        - ✅ **正確計算**：`Turnover = 10, GGR = 10 - 11.50 = -$1.50`（實際成本）

    > **重要**：Evolution Gaming 和 Pragmatic Play API 均要求免費旋轉記錄 `bet_amount = face_value`。詳見：[免費旋轉流水計算](seamless_wallet_analysis/04_free_spins_turnover_calculation.md)

3. 結算回調： 遊戲結束後，GP 發送 Result 請求，但標記為 PromoWin。

4. 入帳： 營運商將這筆贏金計入「獎金錢包」而非「現金錢包」，並可能附加流水要求 38。
    

## 

---

7. 結論與建議

無縫錢包的建置遠非單純的 API 串接，它是一個涉及資金安全、合規計算與高並發處理的複雜系統工程。

關鍵總結：

1. 數據分流： 必須明確區分「流水」（財務用）與「有效投注」（營運用）。建議在資料庫設計中將兩者分開存儲，並建立強大的後處理引擎來計算後者。
    
2. 風控前置： 利用 API Gateway 進行同步的基礎風控（餘額、黑名單），並利用 Kafka 進行深度的異步風控（行為分析），以平衡安全性與性能。
    
3. 財務閉環： 建立「三方對帳」機制，並正確認識「未結算注單」的財務屬性。自動化對帳是減少運營成本的關鍵。
    
4. 標準化與映射： 營運商應建立統一的錯誤代碼與遊戲類型映射層，以屏蔽不同供應商（Pragmatic, Evolution, etc.）的實作差異。
    

通過嚴謹的架構設計與對細節的精確把控，營運商才能在保障資金安全的同時，提供真正「無縫」的玩家體驗。

### 

---

表格索引

|   |   |
|---|---|
|表格名稱|描述|
|表 1: 核心 API 交易方法|比較 Balance, Bet, Result, Rollback 的觸發時機與數據一致性要求。|
|表 2: 有效投注計算邏輯|對比體育、真人、電子遊戲在流水計算上的差異（賠率、對沖、結算）。|
|表 3: 風控錯誤代碼映射|主流供應商（Hub88, Evolution, Pragmatic）的風控拒絕代碼對照表。|

---

**文檔版本**: 1.0.0
**最後更新**: 2026-01-28
**維護團隊**: Integration Team & Backend Team
