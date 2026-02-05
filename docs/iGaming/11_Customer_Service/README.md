# ⚠️ 模塊已遷移 (Module Migrated)

> **DEPRECATED**: 本模塊已於 2026-02-04 合併至新位置
>
> **新位置**: [06_Analytics_Operations/](../06_Analytics_Operations/)
>
> **遷移原因**: Phase 4 模塊合併計劃（14 模塊 → 8 模塊）

---

## 📋 文件遷移對照表

| 舊文件 | 新位置 | 狀態 |
|--------|--------|------|
| [11-01_CS_Platform_Design.md](./11-01_CS_Platform_Design.md) | [06-02_Customer_Service.md](../06_Analytics_Operations/06-02_Customer_Service.md) | ✅ 已遷移 + 增強 |

---

## 🔄 內容增強

新合併文件包含以下增強內容：

### 06-02_Customer_Service.md (520 行)
**來源**:
- 11-01_CS_Platform_Design.md (328 行) - 完整內容

**內容去重**:
- §2: Player 360° 視圖 - 引用 [01-03 玩家分群與標籤](../01_Player_Center/01-03_Player_Segmentation.md) 避免重複分群算法

**新增章節**:
- §4: 知識庫管理系統（80 行）
  - 4 大類別：FAQ、遊戲指南、合規政策、內部操作手冊
  - 版本控制與審批流程
  - 多語言支持（6 種語言）
  - Elasticsearch 全文檢索
  - 權限控制（玩家 vs 客服分級）

- §5: AI 客服機器人整合（90 行）
  - NLU 引擎（13 種意圖分類）
  - 自動回覆規則引擎（基於知識庫）
  - 人工接管邏輯（5 種觸發條件）
  - 訓練數據管道（100K+ 歷史工單）

- §10: SmartAdmin 架構映射
  - TicketEntity (Entity 層)
  - TicketAssignmentManager (Manager 層，@Transactional，Redis 分佈式鎖)
  - TicketService (Service 層，Vavr Option<T>)

---

## ⏰ 過渡期安排

- **當前狀態**: 本目錄文件保留為只讀
- **過渡期**: 2026-02-04 至 2026-03-06（30 天）
- **刪除日期**: 2026-03-06
- **建議行動**: 請立即更新所有書籤和引用至新位置

---

## 📚 相關資源

- **新模塊首頁**: [06_Analytics_Operations/README.md](../06_Analytics_Operations/README.md)
- **玩家分群參考**: [01-03 玩家分群與標籤](../01_Player_Center/01-03_Player_Segmentation.md)
- **完整文檔地圖**: [00-00_Document_Map.md](../00_Foundation/concepts/00-00_Document_Map.md)

---

**遷移完成日期**: 2026-02-04
**Git Commit**: 8881e8bf
**維護團隊**: Customer Service Team & Product Team
