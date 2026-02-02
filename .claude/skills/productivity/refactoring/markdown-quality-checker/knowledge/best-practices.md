# 文檔品質最佳實踐

## Mermaid 圖表

### ✅ DO（推薦做法）

1. **正確的閉合標記**
   ```markdown
   \```mermaid
   graph TD
       A --> B
   \```         ← 永遠使用純三個反引號
   ```

2. **節點數量控制**
   - 單個圖表 ≤ 20 個節點
   - 超過 20 個節點使用 subgraph 分組

3. **使用顏色標記**
   ```mermaid
   graph TD
       A[重要節點]
       B[普通節點]
       style A fill:#ff6b6b
   ```

4. **添加註解**
   ```mermaid
   sequenceDiagram
       Note over Alice,Bob: 重要互動
       Alice->>Bob: Hello
   ```

### ❌ DON'T（避免）

1. **錯誤的閉合標記**
   ```markdown
   \```mermaid
   graph TD
   \```text    ← 錯誤！不要使用語言標識符
   ```

2. **過多節點**
   ```mermaid
   graph TD
       A --> B --> C --> ... --> Z  ← 超過 20 個節點
   ```

3. **缺少圖例**
   - 使用顏色但沒有說明含義

4. **箭頭方向不清晰**
   ```mermaid
   graph TD
       A --- B    ← 沒有方向
   ```
   應該使用:
   ```mermaid
   graph TD
       A --> B    ← 有方向
   ```

---

## Markdown 規範

### ✅ DO（推薦做法）

1. **遵循 CommonMark 規範**
   - 標題層級連續（不跳級）
   - 表格對齊一致
   - 程式碼塊指定語言

2. **正確的標題層級**
   ```markdown
   # H1 標題
   ## H2 標題
   ### H3 標題
   ```

3. **程式碼塊指定語言**
   \```markdown
   \```java
   public class Example {}
   \```
   \```

4. **表格對齊一致**
   ```markdown
   | 欄位 | 值 |
   |------|-----|
   | A    | 1   |
   | B    | 2   |
   ```

### ❌ DON'T（避免）

1. **跳過標題層級**
   ```markdown
   # H1
   ### H3    ← 跳過了 H2
   ```

2. **程式碼塊沒有語言**
   \```markdown
   \```
   public class Example {}
   \```
   \```

3. **表格格式不一致**
   ```markdown
   |欄位|值|
   | A | 1   |
   |B|2|
   ```

---

## 文檔連結

### ✅ DO（推薦做法）

1. **使用相對路徑**
   ```markdown
   [相關文檔](./related-doc.md)
   ```

2. **錨點連結**
   ```markdown
   [跳到章節](#section-name)
   ```

3. **圖片路徑**
   ```markdown
   ![架構圖](../images/architecture.png)
   ```

### ❌ DON'T（避免）

1. **絕對路徑**
   ```markdown
   [文檔](/home/user/docs/file.md)  ← 其他機器無法訪問
   ```

2. **斷裂連結**
   ```markdown
   [不存在](./non-existent.md)
   ```

---

## Mermaid 圖表類型選擇

參考 igaming-multi-tenant-wallet-pm skill 的 Mermaid 策略：

| 圖表類型 | 使用場景 | 節點數量限制 | 示例 |
|---------|---------|------------|------|
| **flowchart** | 決策流程、業務流程、狀態機 | ≤ 20 個節點 | Token 驗證決策樹 |
| **sequenceDiagram** | API 調用時序、系統交互 | ≤ 8 個參與者 | Bet API 時序圖 |
| **erDiagram** | 數據庫設計、實體關係 | ≤ 10 個實體 | 錢包交易表設計 |
| **architecture（C4）** | 系統架構、模組關係 | ≤ 15 個組件 | 多商戶架構圖 |
| **stateDiagram** | 狀態轉換、工作流 | ≤ 12 個狀態 | 訂單狀態流轉 |

---

## 參考資料

- **Markdown 規範**: [CommonMark Spec](https://commonmark.org/)
- **Mermaid 文檔**: [Mermaid Documentation](https://mermaid-js.github.io/mermaid/)
- **SmartAdmin iGaming Skill**: `.claude/skills/extended/domain/igaming-multi-tenant-wallet-pm/SKILL.md` (Lines 766-844)

---

**Last Updated**: 2026-02-02
