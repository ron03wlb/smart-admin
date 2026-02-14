# Ralph Wiggum + Claude Code：24 小時循環自主優化開發完整指南

**Ralph Wiggum 是 Anthropic 官方 Claude Code 插件**，它將 Claude Code 從「一問一答」的互動式工具，轉變為可以連續工作數小時的自主迴圈代理。本指南以 Spring Boot 3.x 微服務為場景，從安裝到 24 小時全自動優化流水線，逐步實戰講解。

---

## 一、Ralph Wiggum 是什麼？為什麼需要它？

### 核心問題

標準 Claude Code session 是「無狀態的」——Claude 完成一個任務後就停下來，等你給下一個指令。對於需要多輪迭代的大型重構、測試覆蓋補全、框架遷移等任務，你必須不斷手動干預，效率極低。

### Ralph 的解法

Ralph Wiggum 利用 Claude Code 的 **Stop Hook**（停止鉤子）攔截 Claude 的退出嘗試，將原始提示詞重新注入，迫使 Claude 繼續工作。每一輪迭代 Claude 都能看到上一輪修改過的文件和 Git 歷史，因此可以在前一輪的基礎上持續改進。

```
┌─────────────────────────────────────────────────────────────┐
│ 1. Claude 接收你的提示詞                                      │
│ 2. Claude 工作（編輯文件、執行命令、完成子任務）                  │
│ 3. Claude 認為自己「完成了」，嘗試退出                          │
│ 4. Stop Hook 攔截退出 → 檢查是否有完成標記                     │
│ 5. 未找到完成標記 → 將原始提示詞重新注入                        │
│ 6. Claude 看到修改過的文件，繼續工作                            │
│ 7. 重複直到達到完成條件或最大迭代次數                            │
└─────────────────────────────────────────────────────────────┘
```

### 哲學：「確定性地糟糕，在不確定性的世界裡」

Ralph 的命名來自《辛普森家庭》中那個永遠不放棄的角色。核心理念是：

- **不要追求一次完美**——讓迴圈去打磨結果
- **失敗是數據**——每次失敗都告訴 Claude 什麼方法行不通
- **持續迭代勝過完美首次嘗試**——複雜任務本來就需要多輪
- **成功取決於提示詞品質**，而非單純依賴模型能力

### 真實成果

| 案例                             | 結果                                            |
| -------------------------------- | ----------------------------------------------- |
| Geoffrey Huntley 連續運行 3 個月 | 建構了完整的編程語言 Cursed（含編譯器、標準庫） |
| YC 黑客松團隊                    | 一夜之間交付 6+ 個倉庫，API 成本僅 $297         |
| 整合測試優化                     | 測試運行時間從 4 分鐘降到 2 秒                  |
| $50K 合約交付                    | 使用 Ralph 循環完成，API 成本 $297              |

---

## 二、安裝與基礎配置（Step by Step）

### Step 1：確認前置條件

```bash
# 確認 Claude Code CLI 已安裝
claude --version

# 確認 Node.js 環境（Claude Code 需要）
node --version  # >= 18.x

# 確認 Git 已設定
git config user.name
git config user.email
```

### Step 2：安裝 Ralph Wiggum 官方插件

在 Claude Code CLI 中執行：

```bash
# 方法 1：添加 Anthropic 官方插件市場後安裝
/plugin marketplace add anthropics/claude-code
/plugin install ralph-wiggum@claude-plugins-official

# 方法 2：直接從官方倉庫安裝
/plugin install ralph-wiggum
```

### Step 3：配置權限

在 `.claude/settings.json` 中添加 Ralph 所需的權限：

```json
{
  "permissions": {
    "allow": [
      "Bash(**/ralph-wiggum/**)",
      "Read",
      "Write",
      "Edit",
      "Glob",
      "Grep",
      "Bash(mvn test*)",
      "Bash(mvn spotless:apply*)",
      "Bash(mvn checkstyle:check*)",
      "Bash(mvn spotbugs:check*)",
      "Bash(mvn verify*)",
      "Bash(mvn compile*)",
      "Bash(git add*)",
      "Bash(git commit*)",
      "Bash(git diff*)",
      "Bash(git log*)",
      "Bash(git status*)",
      "Bash(git branch*)",
      "Bash(cat *)",
      "Bash(find *)",
      "Bash(grep *)"
    ],
    "deny": [
      "Bash(rm -rf *)",
      "Bash(mvn deploy*)",
      "Bash(docker push*)",
      "Bash(kubectl apply*)",
      "Bash(DROP TABLE*)",
      "Bash(TRUNCATE*)"
    ]
  }
}
```

### Step 4：驗證安裝

```bash
# 在 Claude Code 中測試
/ralph-loop "Echo 'Hello Ralph' and create a file test.txt with 'works'. Output <promise>DONE</promise>" --max-iterations 3 --completion-promise "DONE"
```

如果看到 Claude 創建了 test.txt 並成功退出，安裝完成。

---

## 三、兩種 Ralph 模式：官方插件 vs Bash 迴圈

Ralph Wiggum 有兩種實現方式，各有優缺點。根據你的場景選擇：

### 模式 A：官方插件（Stop Hook 方式）

```bash
/ralph-loop "你的任務描述" --max-iterations 30 --completion-promise "DONE"
```

**運作方式**：在同一個 session 內循環，上下文窗口持續累積。

| 優點                           | 缺點                           |
| ------------------------------ | ------------------------------ |
| 安裝簡單，一條命令             | 上下文窗口持續膨脹             |
| session 內狀態完整保留         | 長時間運行會觸發 `/compact`    |
| 不需要外部腳本                 | 超過 ~50 輪迭代後品質可能下降  |
| 適合中等複雜度任務（10-30 輪） | 不適合超長時間（24 小時+）任務 |

### 模式 B：Bash 迴圈（Fresh Context 方式）

```bash
#!/bin/bash
# ralph.sh - 每輪迭代使用全新的上下文窗口

MAX_ITERATIONS=50
PROMPT_FILE="PROMPT.md"

for i in $(seq 1 $MAX_ITERATIONS); do
  echo "=== Ralph 迭代 #$i / $MAX_ITERATIONS ==="
  
  # 每輪啟動全新的 Claude Code session
  OUTPUT=$(claude --print --dangerously-skip-permissions \
    "$(cat $PROMPT_FILE)")
  
  # 檢查完成條件
  if echo "$OUTPUT" | grep -q "RALPH_COMPLETE"; then
    echo "✅ 任務完成於迭代 #$i"
    exit 0
  fi
  
  echo "迭代 #$i 完成，繼續下一輪..."
  sleep 2
done

echo "⚠️ 達到最大迭代次數 $MAX_ITERATIONS"
```

**運作方式**：每輪迭代啟動全新 session，上下文完全乾淨。狀態透過文件和 Git 歷史傳遞。

| 優點                            | 缺點                         |
| ------------------------------- | ---------------------------- |
| 每輪上下文全新，無膨脹          | 需要外部腳本                 |
| 可以真正跑 24 小時+             | 每輪有 session 啟動開銷      |
| 狀態存在文件和 Git 中，不會丟失 | 需要更精心設計 PROMPT.md     |
| 適合超長時間自主任務            | 上下文切換時可能遺失細微推理 |

### 推薦策略

| 任務類型                     | 推薦模式                  | 原因                     |
| ---------------------------- | ------------------------- | ------------------------ |
| 單個 Service 優化（< 30 輪） | 官方插件                  | 上下文保持完整，品質最佳 |
| 整個模組重構（30-100 輪）    | Bash 迴圈                 | 避免上下文膨脹           |
| 24 小時持續優化              | Bash 迴圈 + Git Worktrees | 可並行，可恢復           |
| 多模組並行（同時 3+ 個代理） | Bash 迴圈 + Git Worktrees | 必須隔離                 |

---

## 四、為 Spring Boot 微服務配置 24 小時優化環境

### Step 1：建立專案規則 CLAUDE.md

這是 Claude 在每輪迭代中自動載入的規則：

```markdown
# 專案規則（每輪 Ralph 迭代自動載入）

## 技術棧
- Spring Boot 3.x + Java 17+ + Vavr 函數式編程
- PostgreSQL + Citus 分散式資料庫
- Liquibase 資料庫遷移

## 編碼規範（必須遵守）
- 所有 Service 方法返回 Vavr Either<AppError, T>，禁止返回 null
- 所有 Repository 查詢必須包含 tenantId 過濾
- 禁止 N+1 查詢：使用 JOIN FETCH 或 @EntityGraph
- 批次操作使用 saveAll()，禁止迴圈中 save()
- 零悲觀鎖：使用 optimistic locking + version 欄位
- 日誌使用 SLF4J + MDC（traceId + tenantId）

## Ralph 迴圈指令
- 每完成一個子任務後，執行 `mvn test -pl :當前模組`
- 如果測試失敗，立即修復再繼續
- 將進度更新到 .ralph/progress.md
- 將經驗教訓更新到 .ralph/guardrails.md
- 每完成一個 Service 類的優化後 git commit
```

### Step 2：建立 Ralph 狀態目錄

```bash
mkdir -p .ralph
```

建立 `.ralph/progress.md`：

```markdown
# 優化進度追蹤

## 待處理
- [ ] UserService - null 返回值改 Either
- [ ] OrderService - 移除悲觀鎖
- [ ] PaymentService - 補充 tenantId 過濾
- [ ] NotificationService - N+1 查詢修復
- [ ] ReportService - 批次操作優化

## 進行中
（由 Claude 自動更新）

## 已完成
（由 Claude 自動更新）
```

建立 `.ralph/guardrails.md`：

```markdown
# 經驗教訓（Signs）

Claude 在迭代過程中發現的問題和經驗會記錄在這裡。
每輪新 session 啟動時讀取，避免重複犯錯。

## 已知陷阱
（由 Claude 自動更新）
```

### Step 3：編寫核心提示詞 PROMPT.md

這是 Ralph 迴圈中每輪注入的提示詞，**品質決定成敗**：

```markdown
# Spring Boot 微服務優化任務

## 你的角色
你是 Spring Boot 3.x 企業級應用的優化專家，精通 Vavr 函數式編程模式。

## 工作流程
1. **讀取狀態**：先讀取 `.ralph/progress.md` 了解當前進度
2. **讀取經驗**：讀取 `.ralph/guardrails.md` 了解已知陷阱
3. **選擇任務**：從 progress.md 中選擇下一個「待處理」的任務
4. **執行優化**：
   - 讀取目標 Service 類及其依賴
   - 將 null 返回改為 Vavr Either<AppError, T>
   - 將 try-catch 改為 Vavr Try.of()
   - 確認所有查詢包含 tenantId 過濾
   - 將 N+1 查詢重構為 JOIN FETCH
   - 將迴圈 save() 改為 saveAll()
5. **驗證**：
   - 執行 `mvn test -pl :目標模組`
   - 如果失敗，分析原因並修復
   - 執行 `mvn checkstyle:check -pl :目標模組`
6. **提交**：
   - `git add -A`
   - `git commit -m "refactor(模組): 優化 XxxService - Either 模式 + tenantId"`
7. **更新進度**：
   - 在 `.ralph/progress.md` 中將此任務標記為 ✅ 已完成
   - 如果發現新的經驗教訓，更新 `.ralph/guardrails.md`

## 完成條件
- 當 `.ralph/progress.md` 中所有任務都標記為 ✅ 時
- 並且 `mvn verify` 全部通過
- 輸出 `<promise>RALPH_COMPLETE</promise>`

## 卡住時的處理
如果連續 3 次嘗試修復同一個問題仍然失敗：
1. 在 `.ralph/guardrails.md` 記錄問題詳情
2. 跳過此任務，標記為 ⚠️ 需人工介入
3. 繼續處理下一個任務

## 安全規則
- 絕不修改 application-prod.yml
- 絕不執行 DROP TABLE / TRUNCATE
- 保持所有 public API 向後相容
- 每次只修改一個 Service 類
```

### Step 4：配置 Hooks（與 Ralph 協同工作）

在 `.claude/settings.json` 中配置自動化鉤子：

```json
{
  "hooks": {
    "SessionStart": [
      {
        "matcher": "*",
        "hooks": [
          {
            "type": "command",
            "command": "echo '🔄 Ralph Session 啟動' && echo '分支: '$(git branch --show-current) && echo '未提交: '$(git status --short | wc -l)' 個文件' && echo '已完成任務: '$(grep -c '✅' .ralph/progress.md 2>/dev/null || echo 0)"
          }
        ]
      }
    ],
    "PostToolUse": [
      {
        "matcher": "Write|Edit",
        "hooks": [
          {
            "type": "command",
            "command": "if echo \"$CLAUDE_TOOL_INPUT\" | grep -q '\\.java$'; then mvn spotless:apply -q 2>/dev/null; fi"
          }
        ]
      }
    ],
    "PreToolUse": [
      {
        "matcher": "Bash",
        "hooks": [
          {
            "type": "command",
            "command": "if echo \"$CLAUDE_TOOL_INPUT\" | grep -qE '(DROP TABLE|DELETE FROM.*WHERE 1|TRUNCATE|application-prod)'; then echo 'BLOCKED: 危險操作被阻止' && exit 1; fi"
          }
        ]
      }
    ],
    "Stop": [
      {
        "matcher": "*",
        "hooks": [
          {
            "type": "command",
            "command": "curl -s -X POST \"https://api.telegram.org/bot${TG_BOT_TOKEN}/sendMessage\" -d \"chat_id=${TG_CHAT_ID}&text=🔔 Ralph 迭代完成：$(grep -c '✅' .ralph/progress.md 2>/dev/null || echo 0) 個任務已完成\" 2>/dev/null || true"
          }
        ]
      }
    ]
  }
}
```

---

## 五、啟動 24 小時自主優化

### 方案 A：官方插件模式（適合中等任務）

```bash
# Step 1：建立工作分支
git checkout -b ralph/optimize-services

# Step 2：啟動 Ralph 迴圈
/ralph-loop "讀取 PROMPT.md 並執行其中的任務。每完成一個 Service 優化後更新 .ralph/progress.md。所有任務完成且 mvn verify 通過後，輸出 <promise>RALPH_COMPLETE</promise>" \
  --max-iterations 30 \
  --completion-promise "RALPH_COMPLETE"

# Step 3：離開去睡覺 💤
```

### 方案 B：Bash 迴圈模式（適合 24 小時+）

建立 `ralph-optimize.sh`：

```bash
#!/bin/bash
# ralph-optimize.sh - 24 小時自主優化腳本
# 使用方式：./ralph-optimize.sh [模組名稱] [最大迭代次數]

set -euo pipefail

MODULE=${1:-"user-service"}
MAX_ITERATIONS=${2:-100}
BRANCH="ralph/optimize-${MODULE}-$(date +%Y%m%d)"
PROMPT_FILE="PROMPT.md"
LOG_FILE=".ralph/ralph-$(date +%Y%m%d-%H%M%S).log"
START_TIME=$(date +%s)

# 顏色定義
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

# ===== 前置準備 =====
echo -e "${GREEN}🚀 Ralph 24 小時優化啟動${NC}"
echo "模組: $MODULE"
echo "最大迭代: $MAX_ITERATIONS"
echo "分支: $BRANCH"
echo "日誌: $LOG_FILE"

# 建立工作分支
git checkout -b "$BRANCH" 2>/dev/null || git checkout "$BRANCH"

# 確保狀態目錄存在
mkdir -p .ralph

# ===== 主迴圈 =====
for i in $(seq 1 $MAX_ITERATIONS); do
  ELAPSED=$(( $(date +%s) - START_TIME ))
  HOURS=$(( ELAPSED / 3600 ))
  MINS=$(( (ELAPSED % 3600) / 60 ))
  
  echo ""
  echo -e "${YELLOW}════════════════════════════════════════${NC}"
  echo -e "${YELLOW}  Ralph 迭代 #$i / $MAX_ITERATIONS${NC}"
  echo -e "${YELLOW}  已運行: ${HOURS}h ${MINS}m${NC}"
  echo -e "${YELLOW}════════════════════════════════════════${NC}"
  
  # 記錄迭代開始
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] 迭代 #$i 開始" >> "$LOG_FILE"
  
  # ===== 執行 Claude Code =====
  OUTPUT=$(claude --print --dangerously-skip-permissions \
    "$(cat $PROMPT_FILE)

補充上下文：
- 當前迭代：#$i / $MAX_ITERATIONS
- 已運行時間：${HOURS}h ${MINS}m
- 當前分支：$BRANCH
- 目標模組：$MODULE

請立即開始工作。先讀取 .ralph/progress.md 和 .ralph/guardrails.md。" \
    2>&1) || true
  
  # 記錄輸出摘要
  echo "$OUTPUT" | tail -20 >> "$LOG_FILE"
  
  # ===== 檢查完成條件 =====
  if echo "$OUTPUT" | grep -q "RALPH_COMPLETE"; then
    echo -e "${GREEN}✅ 所有任務完成於迭代 #$i！${NC}"
    
    # 運行最終驗證
    echo "運行最終驗證 mvn verify..."
    if mvn verify -pl ":$MODULE" -q; then
      echo -e "${GREEN}✅ mvn verify 通過${NC}"
      
      # 發送完成通知
      curl -s -X POST "https://api.telegram.org/bot${TG_BOT_TOKEN:-}/sendMessage" \
        -d "chat_id=${TG_CHAT_ID:-}&text=✅ Ralph 優化完成！模組: $MODULE, 迭代: $i, 時間: ${HOURS}h${MINS}m" \
        2>/dev/null || true
      
      echo "[$(date '+%Y-%m-%d %H:%M:%S')] ✅ 成功完成" >> "$LOG_FILE"
      exit 0
    else
      echo -e "${RED}❌ mvn verify 失敗，繼續修復...${NC}"
    fi
  fi
  
  # ===== 檢查人工介入標記 =====
  if echo "$OUTPUT" | grep -q "NEEDS_HUMAN"; then
    echo -e "${RED}🛑 Claude 請求人工介入${NC}"
    curl -s -X POST "https://api.telegram.org/bot${TG_BOT_TOKEN:-}/sendMessage" \
      -d "chat_id=${TG_CHAT_ID:-}&text=🛑 Ralph 需要人工介入！模組: $MODULE, 迭代: $i" \
      2>/dev/null || true
    
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] 🛑 需要人工介入" >> "$LOG_FILE"
    exit 2
  fi
  
  # ===== 安全閥：超過 24 小時自動停止 =====
  if [ $ELAPSED -gt 86400 ]; then
    echo -e "${YELLOW}⏰ 已超過 24 小時，自動停止${NC}"
    curl -s -X POST "https://api.telegram.org/bot${TG_BOT_TOKEN:-}/sendMessage" \
      -d "chat_id=${TG_CHAT_ID:-}&text=⏰ Ralph 超時停止。模組: $MODULE, 完成迭代: $i" \
      2>/dev/null || true
    exit 1
  fi
  
  # 迭代間休息（避免速率限制）
  sleep 5
done

echo -e "${YELLOW}⚠️ 達到最大迭代次數 $MAX_ITERATIONS${NC}"
```

啟動：

```bash
chmod +x ralph-optimize.sh

# 在 tmux/screen 中運行（關閉終端不會中斷）
tmux new-session -d -s ralph "./ralph-optimize.sh user-service 100"

# 查看即時日誌
tmux attach -t ralph

# 或者直接後台運行
nohup ./ralph-optimize.sh user-service 100 > ralph-output.log 2>&1 &
```

---

## 六、進階：多代理並行 24 小時優化

### 使用 Git Worktrees 隔離多個代理

```bash
#!/bin/bash
# ralph-parallel.sh - 多代理並行優化

MODULES=("user-service" "order-service" "payment-service")
MAX_ITERATIONS=50

echo "🚀 啟動 ${#MODULES[@]} 個並行 Ralph 代理"

for MODULE in "${MODULES[@]}"; do
  WORKTREE_DIR="../worktrees/ralph-${MODULE}"
  BRANCH="ralph/optimize-${MODULE}"
  
  echo "📁 建立工作樹: $WORKTREE_DIR"
  
  # 建立獨立工作樹
  git worktree add "$WORKTREE_DIR" -b "$BRANCH" 2>/dev/null || {
    git worktree remove "$WORKTREE_DIR" --force 2>/dev/null
    git branch -D "$BRANCH" 2>/dev/null
    git worktree add "$WORKTREE_DIR" -b "$BRANCH"
  }
  
  # 在獨立 tmux session 中啟動
  tmux new-session -d -s "ralph-${MODULE}" \
    "cd $WORKTREE_DIR && ./ralph-optimize.sh $MODULE $MAX_ITERATIONS"
  
  echo "✅ 代理 ralph-${MODULE} 已啟動"
  sleep 2  # 避免同時啟動衝突
done

echo ""
echo "📊 監控面板："
echo "  tmux attach -t ralph-user-service    # 查看 user-service"
echo "  tmux attach -t ralph-order-service   # 查看 order-service"
echo "  tmux attach -t ralph-payment-service # 查看 payment-service"
echo ""
echo "🔀 完成後合併："
echo "  git merge ralph/optimize-user-service"
echo "  git merge ralph/optimize-order-service"
echo "  git merge ralph/optimize-payment-service"
```

### 多代理監控腳本

```bash
#!/bin/bash
# ralph-monitor.sh - 即時監控所有 Ralph 代理

watch -n 10 '
echo "═══════ Ralph 多代理監控面板 ═══════"
echo ""
for dir in ../worktrees/ralph-*/; do
  MODULE=$(basename $dir | sed "s/ralph-//")
  if [ -f "$dir/.ralph/progress.md" ]; then
    DONE=$(grep -c "✅" "$dir/.ralph/progress.md" 2>/dev/null || echo 0)
    TODO=$(grep -c "\- \[ \]" "$dir/.ralph/progress.md" 2>/dev/null || echo 0)
    WARN=$(grep -c "⚠️" "$dir/.ralph/progress.md" 2>/dev/null || echo 0)
    BRANCH=$(cd "$dir" && git branch --show-current 2>/dev/null)
    COMMITS=$(cd "$dir" && git rev-list --count HEAD 2>/dev/null || echo "?")
    echo "📦 $MODULE ($BRANCH)"
    echo "   ✅ 完成: $DONE | 📋 待處理: $TODO | ⚠️ 卡住: $WARN | 📝 提交: $COMMITS"
    echo ""
  fi
done
echo "最後更新: $(date)"
'
```

---

## 七、提示詞工程：Ralph 成功的關鍵

Ralph 的成敗 80% 取決於提示詞品質。以下是經過驗證的模式：

### 模式 1：任務清單驅動（推薦）

```markdown
# PROMPT.md - 任務清單驅動模式

## 指令
1. 讀取 .ralph/progress.md 獲取任務清單
2. 讀取 .ralph/guardrails.md 獲取已知陷阱
3. 選擇第一個未完成的 `- [ ]` 任務
4. 執行該任務
5. 運行測試驗證
6. 通過後 git commit 並更新 progress.md 為 `- [x]`
7. 失敗 3 次則標記 `⚠️` 並跳過

## 完成條件
所有任務都是 `- [x]` 或 `⚠️`，且 mvn verify 通過。
輸出 <promise>RALPH_COMPLETE</promise>
```

### 模式 2：TDD 驅動（高品質保證）

```markdown
# PROMPT.md - TDD 驅動模式

## 指令
對 .ralph/progress.md 中每個待處理的 Service：
1. 先寫失敗的測試（定義預期行為）
2. 實現最小代碼讓測試通過
3. 運行 `mvn test -pl :模組`
4. 通過 → 重構 → 再測試
5. 全部綠燈 → git commit
6. 更新進度

## 品質門檻
- 測試覆蓋率 > 80%（使用 JaCoCo 檢查）
- 零 Checkstyle 警告
- 零 SpotBugs 嚴重問題

## 卡住處理
如果 5 輪迭代仍無法讓測試通過：
- 記錄問題到 .ralph/guardrails.md
- 跳過此任務
- 繼續下一個

輸出 <promise>RALPH_COMPLETE</promise> 表示完成
```

### 模式 3：分階段執行（大型任務）

```bash
# overnight-phases.sh - 分階段批次執行

#!/bin/bash

echo "🌙 開始夜間批次優化"

# 階段 1：分析和計劃（較少迭代）
/ralph-loop "Phase 1: 分析所有 Service 類，生成優化計劃到 .ralph/plan.md。
列出每個 Service 需要的具體改動。
完成後輸出 <promise>PHASE1_DONE</promise>" \
  --max-iterations 10 \
  --completion-promise "PHASE1_DONE"

# 階段 2：核心重構（最多迭代）
/ralph-loop "Phase 2: 按照 .ralph/plan.md 逐個重構 Service。
每個 Service 修改後必須 mvn test 通過再繼續。
完成後輸出 <promise>PHASE2_DONE</promise>" \
  --max-iterations 50 \
  --completion-promise "PHASE2_DONE"

# 階段 3：驗證和清理
/ralph-loop "Phase 3: 運行完整測試套件 mvn verify。
修復所有失敗的測試。運行 Checkstyle 和 SpotBugs。
生成最終報告到 .ralph/final-report.md。
完成後輸出 <promise>PHASE3_DONE</promise>" \
  --max-iterations 20 \
  --completion-promise "PHASE3_DONE"

echo "☀️ 所有階段完成"
```

---

## 八、與前述 L1-L5 自動化層級的整合

Ralph 不是獨立工具，它和之前指南中的六個層級形成完整體系：

```
┌─────────────────────────────────────────────────────┐
│                  Ralph Wiggum 迴圈                    │
│  ┌───────────────────────────────────────────────┐  │
│  │ L1: CLAUDE.md → 每輪迭代自動載入編碼規範        │  │
│  │ L2: Auto-Accept → Ralph 內自動套用變更          │  │
│  │ L3: Hooks                                      │  │
│  │     SessionStart → 注入 Git 狀態和進度          │  │
│  │     PostToolUse → 自動 Spotless 格式化          │  │
│  │     PreToolUse → 攔截危險操作                   │  │
│  │     Stop → Telegram 通知（與 Ralph 共存）       │  │
│  │ L4: Subagents → Ralph 內部可啟動子代理並行      │  │
│  │ L5: Skills → Ralph 可調用 optimize-service 等   │  │
│  └───────────────────────────────────────────────┘  │
│                                                      │
│  Ralph 在外層持續驅動迭代                              │
│  L1-L5 在內層保障每輪迭代的品質                        │
└─────────────────────────────────────────────────────┘
```

### 整合配置範例

```json
{
  "hooks": {
    "SessionStart": [
      {
        "matcher": "*",
        "hooks": [
          {
            "type": "command",
            "command": "echo '=== Ralph Context ===' && cat .ralph/progress.md 2>/dev/null | head -20 && echo '=== Guardrails ===' && cat .ralph/guardrails.md 2>/dev/null | head -10"
          }
        ]
      }
    ],
    "PostToolUse": [
      {
        "matcher": "Write|Edit",
        "hooks": [
          {
            "type": "command",
            "command": "if echo \"$CLAUDE_TOOL_INPUT\" | grep -q '\\.java$'; then mvn spotless:apply -q 2>/dev/null; fi"
          }
        ]
      }
    ],
    "Stop": [
      {
        "matcher": "*",
        "hooks": [
          {
            "type": "command",
            "command": "DONE=$(grep -c '✅' .ralph/progress.md 2>/dev/null || echo 0) && TODO=$(grep -c '\\- \\[ \\]' .ralph/progress.md 2>/dev/null || echo 0) && curl -s -X POST \"https://api.telegram.org/bot${TG_BOT_TOKEN}/sendMessage\" -d \"chat_id=${TG_CHAT_ID}&text=🔄 Ralph: ✅${DONE} 📋${TODO}\" 2>/dev/null || true"
          }
        ]
      }
    ]
  }
}
```

---

## 九、安全守則與成本控制

### 成本預估

| 場景              | 預估迭代 | 預估成本（API） | 預估成本（Max 訂閱） |
| ----------------- | -------- | --------------- | -------------------- |
| 單個 Service 優化 | 5-10     | $5-15           | $0（含在訂閱中）     |
| 整個模組重構      | 20-50    | $30-80          | $0（但消耗配額更快） |
| 24 小時多模組     | 50-200   | $100-300        | 可能觸發 5 小時限制  |

### 必備安全措施

1. **永遠設定 `--max-iterations`**——這是你的成本控制閥
2. **永遠在獨立 Git 分支上運行**——隨時可以回退
3. **永遠配置 `deny` 清單**——禁止 deploy、push、DROP 等危險操作
4. **使用 tmux/screen**——防止終端斷開導致任務中斷
5. **配置 Telegram/Slack 通知**——遠離電腦也能監控進度
6. **24 小時自動停止閥**——防止無限制燒錢

### 5 小時 API 限制處理

Claude Max 訂閱有 5 小時使用量限制。Ralph 社群版（frankbria/ralph-claude-code）內建了檢測和處理：

```bash
# 在 ralph-optimize.sh 中添加限制檢測
if echo "$OUTPUT" | grep -qE "(rate limit|usage limit|5-hour)"; then
  echo "⏳ 觸發 API 限制，等待 30 分鐘..."
  curl -s -X POST "https://api.telegram.org/bot${TG_BOT_TOKEN}/sendMessage" \
    -d "chat_id=${TG_CHAT_ID}&text=⏳ Ralph 觸發速率限制，等待中..." \
    2>/dev/null || true
  sleep 1800  # 等待 30 分鐘
fi
```

### 七個常見失敗原因（避坑指南）

| #   | 錯誤                    | 後果                          | 解決方案                      |
| --- | ----------------------- | ----------------------------- | ----------------------------- |
| 1   | 提示詞太模糊            | Claude 每輪做不同的事，不收斂 | 用清單明確列出每個子任務      |
| 2   | 沒有設定 max-iterations | Token 無限制消耗              | 永遠設定上限                  |
| 3   | 沒有完成條件            | 迴圈永遠不會結束              | 定義精確的 completion-promise |
| 4   | 沒有 Git commit         | 下一輪迭代看不到進度          | 每完成一步就 commit           |
| 5   | 沒有測試驗證            | 優化可能引入 Bug              | 每步都跑 mvn test             |
| 6   | 同一上下文跑太久        | 上下文汙染，品質下降          | 超過 30 輪改用 Bash 迴圈      |
| 7   | 沒有卡住處理策略        | 在同一問題上無限迴圈          | 設定失敗 N 次則跳過的規則     |

---

## 十、完整端到端範例：睡前啟動，醒來收穫

```bash
# ===== 晚上 11 點：啟動前準備（5 分鐘）=====

# 1. 確認代碼庫乾淨
git stash  # 暫存未提交的工作
git checkout main
git pull origin main

# 2. 確認測試基線通過
mvn test -pl :user-service
# ✅ All tests passed

# 3. 確認 PROMPT.md 和 progress.md 已準備好
cat PROMPT.md       # 檢查任務描述
cat .ralph/progress.md  # 檢查任務清單

# 4. 啟動 Ralph（在 tmux 中）
tmux new-session -d -s ralph-overnight \
  "./ralph-optimize.sh user-service 80"

# 5. 確認已啟動
tmux ls
# ralph-overnight: 1 windows (created ...)

# 6. 去睡覺 💤

# ===== 早上 8 點：查看結果 =====

# 1. 查看進度
cat .ralph/progress.md
# ✅ UserService - Either 模式重構
# ✅ OrderService - 移除悲觀鎖
# ✅ PaymentService - tenantId 過濾
# ⚠️ ReportService - 需人工介入（複雜的聚合查詢）
# ✅ NotificationService - N+1 修復

# 2. 查看 Git 歷史
git log --oneline -10
# abc1234 refactor(notification): 修復 N+1 查詢
# def5678 refactor(payment): 補充 tenantId 過濾
# ghi9012 refactor(order): 移除悲觀鎖，改 optimistic
# jkl3456 refactor(user): null 返回改 Either

# 3. 查看經驗教訓
cat .ralph/guardrails.md
# ## 已知陷阱
# - ReportService 的聚合查詢涉及跨租戶統計，不能簡單加 tenantId
# - OrderService 的 version 欄位需要 @Version 註解而非手動管理

# 4. 運行最終驗證
mvn verify -pl :user-service
# ✅ BUILD SUCCESS

# 5. 創建 Pull Request
git push origin ralph/optimize-user-service
# → 在 GitHub/GitLab 上創建 PR 進行代碼審查
```

---

## 快速命令速查

| 命令/操作                                | 功能                       |
| ---------------------------------------- | -------------------------- |
| `/plugin install ralph-wiggum`           | 安裝官方 Ralph 插件        |
| `/ralph-loop "任務" --max-iterations 30` | 啟動 Ralph 迴圈            |
| `/cancel-ralph`                          | 停止當前 Ralph 迴圈        |
| `--completion-promise "DONE"`            | 設定完成標記               |
| `--max-iterations N`                     | 設定最大迭代次數（必設！） |
| `<promise>DONE</promise>`                | 提示詞中的完成信號格式     |
| `tmux new-session -d -s ralph`           | 在後台啟動                 |
| `tmux attach -t ralph`                   | 查看 Ralph 即時狀態        |
| `cat .ralph/progress.md`                 | 查看任務進度               |
| `cat .ralph/guardrails.md`               | 查看經驗教訓               |
