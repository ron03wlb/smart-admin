#!/bin/bash

# Phase 1 基礎設施驗證腳本
# 版本: v2.0.0
# 日期: 2026-03-28
# 說明: 驗證 OpenSpec、Claude Code 外掛、MCP 伺服器、專案結構

set -e  # 遇到錯誤立即退出

# 顏色定義
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 顯示標題
echo -e "${BLUE}=========================================="
echo "Phase 1 基礎設施驗證"
echo -e "==========================================${NC}\n"

# 驗證計數器
PASS=0
FAIL=0
WARN=0

# ============================================
# 驗證 1: OpenSpec CLI
# ============================================
echo -e "${YELLOW}[1/8] 驗證 OpenSpec CLI...${NC}"
if command -v openspec &> /dev/null; then
    echo -e "${GREEN}✓${NC} OpenSpec CLI 已安裝"
    OPENSPEC_VERSION=$(openspec --version 2>&1 || echo "已安裝但無法取得版本")
    echo "   版本: $OPENSPEC_VERSION"
    ((PASS++))
else
    echo -e "${RED}✗${NC} OpenSpec CLI 未安裝"
    echo "   請執行: npm install -g @fission-ai/openspec@latest"
    ((FAIL++))
fi
echo ""

# ============================================
# 驗證 2: OpenSpec 專案結構
# ============================================
echo -e "${YELLOW}[2/8] 驗證 OpenSpec 專案結構...${NC}"
DIRS_OK=true
for dir in ".specs/proposed" ".specs/applied" ".specs/archived" ".specs/templates"; do
    if [ -d "$dir" ]; then
        echo -e "${GREEN}✓${NC} $dir 存在"
    else
        echo -e "${RED}✗${NC} $dir 不存在"
        DIRS_OK=false
    fi
done

if [ "$DIRS_OK" = true ]; then
    ((PASS++))
else
    echo -e "${RED}✗${NC} OpenSpec 目錄結構不完整"
    echo "   請執行: mkdir -p .specs/{proposed,applied,archived,templates}"
    ((FAIL++))
fi
echo ""

# ============================================
# 驗證 3: OpenSpec 配置檔案
# ============================================
echo -e "${YELLOW}[3/8] 驗證 OpenSpec 配置檔案...${NC}"
if [ -f ".specs/opsx.config.json" ]; then
    echo -e "${GREEN}✓${NC} opsx.config.json 存在"
    # 驗證 JSON 語法
    if jq empty .specs/opsx.config.json &> /dev/null; then
        echo -e "${GREEN}✓${NC} JSON 語法正確"
        ((PASS++))
    else
        echo -e "${RED}✗${NC} JSON 語法錯誤"
        echo "   請檢查 .specs/opsx.config.json 的 JSON 格式"
        ((FAIL++))
    fi
else
    echo -e "${RED}✗${NC} opsx.config.json 不存在"
    echo "   請參考 Phase 1 安裝指南建立此檔案"
    ((FAIL++))
fi
echo ""

# ============================================
# 驗證 4: SmartAdmin Spec 模板
# ============================================
echo -e "${YELLOW}[4/8] 驗證 SmartAdmin Spec 模板...${NC}"
if [ -f ".specs/templates/smartadmin-feature.spec.md" ]; then
    echo -e "${GREEN}✓${NC} SmartAdmin 規格模板存在"

    # 驗證必要區段
    REQUIRED_SECTIONS=(
        "Executive Summary"
        "SmartAdmin Architecture Mapping"
        "Database Schema"
        "Test Strategy"
        "Acceptance Criteria"
    )

    SECTIONS_OK=true
    for section in "${REQUIRED_SECTIONS[@]}"; do
        if grep -q "## .*$section" .specs/templates/smartadmin-feature.spec.md; then
            echo -e "${GREEN}✓${NC} 包含區段: $section"
        else
            echo -e "${RED}✗${NC} 缺少區段: $section"
            SECTIONS_OK=false
        fi
    done

    if [ "$SECTIONS_OK" = true ]; then
        ((PASS++))
    else
        echo -e "${RED}✗${NC} 模板缺少必要區段"
        ((FAIL++))
    fi
else
    echo -e "${RED}✗${NC} SmartAdmin 規格模板不存在"
    echo "   請參考 Phase 1 安裝指南建立此檔案"
    ((FAIL++))
fi
echo ""

# ============================================
# 驗證 5: Claude Code 外掛（需手動確認）
# ============================================
echo -e "${YELLOW}[5/8] 驗證 Claude Code 外掛...${NC}"
echo -e "${YELLOW}⚠${NC}  此項目需要手動確認"
echo ""
echo "請在 Claude CLI 中執行以下指令："
echo -e "${BLUE}/plugin list${NC}"
echo ""
echo "確認以下11個外掛已安裝："
echo "  1. superpowers              (Layer 2: 方法論執行)"
echo "  2. ralph-loop             (Layer 3: 自主執行)"
echo "  3. skill-creator            (Layer 4: 技能生命週期管理)"
echo "  4. developer-kit-java       (Layer 5: Java 開發工具 - 11 Commands + 52 Skills)"
echo "  5. context7                 (Layer 5: 文檔查詢)"
echo "  6. java-lsp                 (Layer 5: Java 語言伺服器)"
echo "  7. frontend-design          (Layer 6: React 設計系統)"
echo "  8. typescript-lsp           (Layer 6: TypeScript 語言伺服器)"
echo "  9. security-guidance        (Layer 7: 安全指引)"
echo " 10. code-review              (Layer 7: 程式碼審查)"
echo " 11. (加上其他可能已安裝的外掛)"
echo ""
read -p "所有外掛是否已安裝？(y/n): " plugin_check

if [ "$plugin_check" = "y" ] || [ "$plugin_check" = "Y" ]; then
    echo -e "${GREEN}✓${NC} Claude Code 外掛已確認安裝"
    ((PASS++))
else
    echo -e "${RED}✗${NC} Claude Code 外掛未完全安裝"
    echo "   請參考 Phase 1 安裝指南逐一安裝外掛"
    ((FAIL++))
fi
echo ""

# ============================================
# 驗證 6: MCP 伺服器配置檔案
# ============================================
echo -e "${YELLOW}[6/8] 驗證 MCP 伺服器配置...${NC}"
MCP_CONFIG="$HOME/.config/claude/mcp.json"

if [ -f "$MCP_CONFIG" ]; then
    echo -e "${GREEN}✓${NC} MCP 配置檔案存在: $MCP_CONFIG"

    # 驗證 JSON 語法
    if jq empty "$MCP_CONFIG" &> /dev/null; then
        echo -e "${GREEN}✓${NC} JSON 語法正確"

        # 檢查 Context7 配置
        if jq -e '.mcpServers.context7' "$MCP_CONFIG" &> /dev/null; then
            echo -e "${GREEN}✓${NC} Context7 MCP 伺服器已配置"
        else
            echo -e "${YELLOW}⚠${NC}  Context7 MCP 伺服器未配置"
            ((WARN++))
        fi

        # 檢查 PostgreSQL 配置
        if jq -e '.mcpServers.postgres' "$MCP_CONFIG" &> /dev/null; then
            echo -e "${GREEN}✓${NC} PostgreSQL MCP 伺服器已配置"
        else
            echo -e "${YELLOW}⚠${NC}  PostgreSQL MCP 伺服器未配置"
            echo "   請參考 Phase 1 安裝指南新增 PostgreSQL 配置"
            ((WARN++))
        fi

        ((PASS++))
    else
        echo -e "${RED}✗${NC} JSON 語法錯誤"
        echo "   請檢查 $MCP_CONFIG 的 JSON 格式"
        ((FAIL++))
    fi
else
    echo -e "${RED}✗${NC} MCP 配置檔案不存在: $MCP_CONFIG"
    echo "   請執行: ./scripts/phase1-quick-install.sh"
    ((FAIL++))
fi
echo ""

# ============================================
# 驗證 7: MCP 伺服器運行狀態（需手動確認）
# ============================================
echo -e "${YELLOW}[7/8] 驗證 MCP 伺服器運行狀態...${NC}"
echo -e "${YELLOW}⚠${NC}  此項目需要手動確認"
echo ""
echo "請在 Claude CLI 中執行以下指令："
echo -e "${BLUE}claude mcp list${NC}"
echo ""
echo "確認以下 MCP 伺服器正在運行："
echo "  1. context7  (running)"
echo "  2. postgres  (running)"
echo ""
read -p "MCP 伺服器是否正在運行？(y/n): " mcp_check

if [ "$mcp_check" = "y" ] || [ "$mcp_check" = "Y" ]; then
    echo -e "${GREEN}✓${NC} MCP 伺服器已確認運行"
    ((PASS++))
else
    echo -e "${YELLOW}⚠${NC}  MCP 伺服器未運行或配置不正確"
    echo "   請檢查 $MCP_CONFIG 中的配置"
    echo "   重新啟動 Claude CLI: pkill claude && claude"
    ((WARN++))
fi
echo ""

# ============================================
# 驗證 8: Git 配置
# ============================================
echo -e "${YELLOW}[8/8] 驗證 Git 配置...${NC}"
if git rev-parse --git-dir > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} 位於 Git 倉庫中"

    # 檢查 .specs/ 是否加入版本控制
    if git ls-files --error-unmatch .specs/ &> /dev/null; then
        echo -e "${GREEN}✓${NC} .specs/ 已加入 Git 版本控制"
        ((PASS++))
    else
        echo -e "${YELLOW}⚠${NC}  .specs/ 尚未加入 Git 版本控制"
        echo "   建議執行: git add .specs/"
        ((WARN++))
    fi
else
    echo -e "${RED}✗${NC} 不在 Git 倉庫中"
    ((FAIL++))
fi
echo ""

# ============================================
# 總結報告
# ============================================
echo -e "${BLUE}=========================================="
echo "驗證結果"
echo -e "==========================================${NC}\n"

echo -e "${GREEN}通過: $PASS / 8${NC}"
if [ $FAIL -gt 0 ]; then
    echo -e "${RED}失敗: $FAIL${NC}"
fi
if [ $WARN -gt 0 ]; then
    echo -e "${YELLOW}警告: $WARN${NC}"
fi
echo ""

# 決定整體狀態
if [ $FAIL -eq 0 ]; then
    if [ $WARN -eq 0 ]; then
        echo -e "${GREEN}✓✓✓ Phase 1 基礎設施安裝完成！${NC}"
        echo ""
        echo -e "${BLUE}下一步：${NC}"
        echo "  1. 繼續 Phase 1.2 - 優化 hooks.json 配置"
        echo "  2. 參考文檔: docs/skills/phase1-complete-installation-guide.md"
        exit 0
    else
        echo -e "${YELLOW}⚠ Phase 1 基礎設施大部分完成，但有 $WARN 個警告項目${NC}"
        echo ""
        echo -e "${BLUE}建議：${NC}"
        echo "  1. 解決上述警告項目"
        echo "  2. 重新執行驗證: ./scripts/verify-phase1-infrastructure.sh"
        echo "  3. 或直接繼續 Phase 1.2（警告項目不會阻礙後續步驟）"
        exit 0
    fi
else
    echo -e "${RED}✗ Phase 1 基礎設施安裝未完成，請檢查 $FAIL 個失敗項目${NC}"
    echo ""
    echo -e "${BLUE}建議：${NC}"
    echo "  1. 解決上述失敗項目"
    echo "  2. 參考文檔: docs/skills/phase1-complete-installation-guide.md"
    echo "  3. 重新執行驗證: ./scripts/verify-phase1-infrastructure.sh"
    exit 1
fi
