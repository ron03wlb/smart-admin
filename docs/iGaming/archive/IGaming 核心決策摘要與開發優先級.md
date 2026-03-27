---
title: IGaming 核心決策摘要與開發優先級
created: 2026-03-24
tags:
  - igaming
  - decisions
  - priorities
description: 從 290+ 份文檔中提煉的核心決策點、待辦事項與開發優先級
---

# IGaming 核心決策摘要與開發優先級

> **產生日期**: 2026-03-24
> **適用範圍**: Phase 2 (進行中) → Phase 3 → Phase 4+

---

## 一、已確立的核心架構決策

### 1.1 決策地圖

```mermaid
mindmap
  root((iGaming<br/>架構決策))
    架構模式
      進化式模組化單體
      事件驅動 (Kafka)
      CQRS 讀寫分離
    資料層
      PostgreSQL + Citus 分片
      Redis 二級快取
      ClickHouse OLAP
    安全
      AES-256-GCM 加密
      HMAC-SHA256 盲索引
      三層冪等防禦
    風控
      LiteFlow 規則引擎
      Flag 模式 (不阻斷投注)
      Flink CEP 串流
    開發規範
      SmartAdmin 四層
      Manager 層獨佔事務
      Vavr 函數式
```

### 1.2 ADR 決策清單

| ADR | 決策主題 | 結論 | 不可逆程度 | 狀態 |
|-----|---------|------|----------|------|
| ADR-001 | 命名規範 | 單數標準 (t_player, PlayerEntity) | 高 (全域影響) | ✅ 已執行 |
| ADR-012 | 異步風控模式 | Flag 模式：投注不阻斷，提款攔截 | 中 | ✅ 已確立 |
| ADR-013 | Manager 層邊界 | 分佈式鎖 + @Transactional 限定 Manager | 高 | ✅ 已執行 |
| ADR-014 | @TenantIgnore 策略 | 白名單制度，禁止跨租戶訪問敏感數據 | 高 | ✅ 已執行 |
| ADR-015 | 三層冪等 | Redis → DB UNIQUE → Fallback | 中 | ✅ 已執行 |
| — | Kafka KRaft | 去 ZooKeeper，故障轉移 <1s | 高 | ✅ 已選定 |
| — | Flink vs Kafka Streams | Flink (CEP 核心，獨立集群) | 高 | ✅ 已選定 |
| — | PG+Citus vs NewSQL | Citus (成本低 40%，PG 生態) | 高 | ✅ 已選定 |

### 1.3 待決策事項

| 決策點 | 選項 | 相關模組 | 建議時間 |
|--------|------|---------|---------|
| API Gateway 選型確認 | Kong 3.5 vs Apache APISIX | 基礎設施 | Phase 2 中期 |
| 體育博彩賠率引擎 | 自建 vs 第三方整合 | 遊戲服務 | Phase 4 規劃時 |
| 真人荷官整合模式 | Seamless vs Transfer Wallet | 遊戲服務 | Phase 4 規劃時 |
| ML 模型部署方式 | Sidecar vs 獨立微服務 | 風控引擎 | Phase 3 初期 |
| 前端框架統一 | React (Web) + Vue 3 (CMS 渲染) 共存 vs 統一 | 前端 | Phase 3 |

---

## 二、開發階段與里程碑

### 2.1 總體路線圖

```mermaid
gantt
    title iGaming 開發路線圖
    dateFormat YYYY-MM-DD
    axisFormat %Y-%m

    section Phase 0 基礎設施
    Multi-Tenant 全域改造     :done, 2025-10-01, 2026-01-15
    Event-Driven 架構         :done, 2025-11-01, 2026-01-15
    DB Schema + Flyway        :done, 2025-12-01, 2026-01-15

    section Phase 1 核心金融
    Seamless Wallet           :done, 2026-01-15, 2026-02-28
    Payment Gateway           :done, 2026-01-15, 2026-02-28
    Turnover Engine           :done, 2026-02-01, 2026-03-10

    section Phase 2 玩家與遊戲
    Player Service + KYC      :active, 2026-03-12, 2026-04-30
    Game Integration          :active, 2026-03-12, 2026-05-15
    Activity Engine (Bonus)   :2026-04-01, 2026-05-30
    Phase 2 整合測試          :2026-05-15, 2026-06-15

    section Phase 3 風控與分析
    LiteFlow 風控規則         :2026-06-15, 2026-08-15
    ML Model Pipeline         :2026-07-01, 2026-09-01
    BI 報表 + ClickHouse      :2026-07-15, 2026-09-15

    section Phase 4+ 擴展
    Agent Credit Network      :2026-09-15, 2026-11-15
    Sports Betting            :2026-10-01, 2027-01-31
    Live Dealer Integration   :2026-11-01, 2027-02-28
```

### 2.2 Phase 2 當前狀態 (2026-03-24)

| 任務 | 狀態 | 備註 |
|------|------|------|
| Player Service 資料庫遷移 | ✅ 完成 | V23-V24 已驗證 |
| Game Provider Adapter 骨架 | 🔄 進行中 | PGSoft + Evolution 優先 |
| Turnover Engine 單測 | ✅ 完成 | 120/120 通過, 覆蓋率 80%+ |
| 測試環境配置 | ⚠️ 70% | SecurityConfigProvider 阻塞 |
| MFA 架構違規修復 | 📋 延後 | 17 項，Phase 2 期間處理 |
| Stripe/PayPal Adapter | ❌ 跳過 | 建議 Phase 2 後期再做 |

---

## 三、技術債與風險

### 3.1 技術債清單

```mermaid
quadrantChart
    title 技術債優先級矩陣
    x-axis 低影響 --> 高影響
    y-axis 低緊急 --> 高緊急
    quadrant-1 立即處理
    quadrant-2 計劃處理
    quadrant-3 觀察
    quadrant-4 改善

    MFA 17項違規: [0.7, 0.8]
    測試環境阻塞: [0.8, 0.9]
    SLA目標不一致: [0.5, 0.6]
    文檔格式不統一: [0.2, 0.3]
    前端框架共存: [0.6, 0.3]
    缺少E2E壓測: [0.7, 0.5]
    API版本策略缺失: [0.5, 0.4]
```

| 技術債 | 影響 | 緊急度 | 建議處理時間 |
|--------|------|--------|------------|
| 測試環境 SecurityConfigProvider 缺失 | P0 阻塞整合測試 | 🔴 高 | 本週 |
| MFA 17 項架構違規 | 安全合規風險 | 🟡 中 | Phase 2 中期 |
| SLA 目標不一致 (99.9% vs 99.99%) | 營運承諾混亂 | 🟡 中 | Phase 2 |
| 缺少全鏈路 E2E 壓測 | 上線風險 | 🟡 中 | Phase 2 驗收 |
| API 版本管理策略缺失 | 未來升級困難 | 🟢 低 | Phase 3 前 |
| 前端 React + Vue 3 共存 | 維護成本高 | 🟢 低 | Phase 3 評估 |

### 3.2 風險矩陣

| 風險 | 可能性 | 影響 | 緩解措施 |
|------|--------|------|---------|
| Phase 2 遊戲整合延期 | 中 | 高 | 優先完成 PGSoft + Evolution 兩家 |
| UKGC 2025 可負擔性新規不合規 | 高 | 極高 | Phase 3 初期優先實作 |
| 單體架構擴展瓶頸 | 低 | 高 | Kafka 事件解耦保留微服務萃取能力 |
| 第三方 KYC 供應商中斷 | 中 | 中 | 手動審查 Fallback + 多供應商備援 |
| 加密貨幣法規變化 | 中 | 中 | CoinsPaid 適配器可快速切換 |

---

## 四、合規優先級

### 4.1 各牌照關鍵合規要求

```mermaid
graph TB
    subgraph 已實現["✅ 已實現"]
        C1["GDPR 五項玩家權利"]
        C2["基礎 KYC L0-L2"]
        C3["自我排除框架"]
        C4["AES-256-GCM 加密"]
    end

    subgraph 進行中["🔄 進行中"]
        C5["PCI-DSS v4.0 (67%)"]
        C6["ISO 27001 (13/15)"]
        C7["UKGC RTS 環境分離"]
    end

    subgraph 待實作["📋 待實作"]
        C8["UKGC 2025 可負擔性評估"]
        C9["Gamstop 即時同步"]
        C10["巴西 .bet.br CPF 驗證"]
        C11["德國 60 分鐘會話限制"]
        C12["荷蘭默認限額配置"]
    end

    style 已實現 fill:#4CAF50,color:#fff
    style 進行中 fill:#FF9800,color:#fff
    style 待實作 fill:#F44336,color:#fff
```

### 4.2 合規實作優先級

| 優先級 | 合規要求 | 牌照 | 建議階段 |
|--------|---------|------|---------|
| **P0** | UKGC 可負擔性評估 (£125/£500/£2000) | UKGC | Phase 3 初期 |
| **P0** | PCI-DSS v4.0 完成 (67% → 95%) | 全部 | Phase 2-3 |
| **P1** | Gamstop 即時查詢 + 每日批次同步 | UKGC | Phase 3 |
| **P1** | ISO 27001 剩餘 2 項控制 | 全部 | Phase 3 |
| **P1** | 德國 60 分鐘會話強制中斷 | 德國牌照 | Phase 3 |
| **P2** | 巴西 CPF 驗證 + PIX 支付整合 | Brazil SPA | Phase 4 |
| **P2** | 荷蘭默認限額 (日€200/週€700/月€2000) | CRUKS | Phase 4 |

---

## 五、下一步行動建議

### 5.1 本週 (2026-03-24 ~ 03-28)

1. ✅ 修復 Sprint 3 測試環境 SecurityConfigProvider (0.1 人天)
2. ✅ 統一 SLA 可用性目標文檔
3. ✅ 統一風險評分邊界值定義

### 5.2 Phase 2 剩餘 (2026-04 ~ 06)

1. 完成 PGSoft + Evolution 遊戲整合
2. 完成 Activity Engine (紅利引擎) 核心功能
3. 修復 MFA 17 項架構違規
4. 補充缺失的 UX 需求 + 紅利計算規則文檔
5. 執行 Phase 2 整合測試 + 壓測
6. 建立 00_Navigation 導航索引

### 5.3 Phase 3 準備 (2026-06 前確認)

1. 確定 ML 模型部署方式 (Sidecar vs 微服務)
2. 確定 API Gateway 選型 (Kong vs APISIX)
3. 規劃 UKGC 可負擔性評估實作方案
4. 建立全鏈路 E2E 壓測基準

---

## 附錄：關鍵 KPI 追蹤表

| KPI | 目標 | 當前 | 差距 | 負責模組 |
|-----|------|------|------|---------|
| 錢包計算精確度 | 99.99% | ✅ 100% (Phase 1.5 驗證) | — | 財務 |
| 有效投注額精確度 | 100% | ✅ 120/120 測試通過 | — | 財務 |
| TPS | ≥100 | ✅ 168 (超 68%) | — | 基礎設施 |
| P95 回應 | <100ms | ✅ 68ms | — | 基礎設施 |
| ArchUnit 合規 | 100% | ✅ 5/5 | — | 開發 |
| PCI-DSS 覆蓋 | ≥95% | 67% | -28% | 安全 |
| ISO 27001 | 15/15 | 13/15 | -2 項 | 安全 |
| 測試環境就緒 | 100% | 70% | -30% | QA |

---

> **備註**: 本文件為 2026-03-24 快照。建議每兩週在 Sprint Review 中更新進度。
