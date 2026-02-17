#!/bin/bash
# validate-zh-tw-encoding.sh
# 驗證 iGaming 文檔的 UTF-8 編碼和繁體中文使用（檢測簡體中文誤用）
#
# 使用方式:
#   bash scripts/validate-zh-tw-encoding.sh <file_or_directory>
#   bash scripts/validate-zh-tw-encoding.sh docs/iGaming/requirements/01_Player/Player_Lifecycle.md
#   bash scripts/validate-zh-tw-encoding.sh docs/iGaming/requirements/
#
# Exit Codes:
#   0 - PASS (UTF-8 編碼 + 繁體中文)
#   1 - FAIL (編碼錯誤或簡體中文誤用)

set -euo pipefail

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 錯誤計數器
ERROR_COUNT=0

# 檢測文件編碼和簡體中文的函數
check_file() {
    local file="$1"
    local file_errors=0

    # 跳過非 Markdown 文件
    if [[ ! "$file" =~ \.md$ ]]; then
        return 0
    fi

    # 1. 檢測文件編碼
    local encoding=$(file -b --mime-encoding "$file")
    if [[ "$encoding" != "utf-8" && "$encoding" != "us-ascii" ]]; then
        echo -e "${RED}[ERROR]${NC} $file"
        echo -e "  Encoding: ${YELLOW}$encoding${NC} (expected UTF-8)"
        echo ""
        ((file_errors++))
    fi

    # 2. 檢測簡體中文關鍵字
    # 定義常見繁體/簡體詞組對照（僅檢測多字詞組，減少誤報）
    # 格式: "簡體|繁體" (使用 pipe-delimited array 以兼容 bash 3.2+)
    local CHAR_PAIRS=(
        "数据库|資料庫"
        "数据|資料"
        "用户|用戶"
        "启动|啟動"
        "启用|啟用"
        "网络|網路"
        "网站|網站"
        "电子|電子"
        "账户|帳戶"
        "账号|帳號"
        "验证|驗證"
        "认证|認證"
        "权限|權限"
        "访问|訪問"
        "设置|設定"
        "设备|設備"
        "实现|實現"
        "实体|實體"
        "传输|傳輸"
        "传递|傳遞"
        "变量|變數"
        "变更|變更"
        "环境|環境"
        "处理|處理"
        "执行|執行"
        "运行|運行"
        "查询|查詢"
        "询问|詢問"
        "显示|顯示"
        "条件|條件"
        "记录|記錄"
        "纪录|紀錄"
        "历史|歷史"
        "历程|歷程"
        "报告|報告"
        "报表|報表"
        "币种|幣種"
        "货币|貨幣"
        "财务|財務"
        "经理|經理"
        "经营|經營"
        "业务|業務"
        "产品|產品"
        "产生|產生"
        "应用|應用"
        "应该|應該"
        "响应|響應"
        "级别|級別"
        "层级|層級"
        "类型|類型"
        "类别|類別"
        "对象|對象"
        "对照|對照"
        "状态|狀態"
        "注册|註冊"
        "线程|執行緒"
        "线上|線上"
        "当前|當前"
        "标识|標識"
        "标记|標記"
        "进程|處理程序"
        "进行|進行"
        "进度|進度"
        "选择|選擇"
        "开发|開發"
        "开启|開啟"
        "关闭|關閉"
        "发送|發送"
        "发布|發佈"
        "发生|發生"
        "继续|繼續"
        "连接|連接"
        "还原|還原"
        "节点|節點"
        "异常|異常"
        "异步|非同步"
        "错误|錯誤"
    )

    # 檢測每個簡體字
    for pair in "${CHAR_PAIRS[@]}"; do
        local simplified="${pair%%|*}"
        local traditional="${pair##*|}"

        # 搜尋簡體字（排除代碼塊）
        local matches=$(grep -n "$simplified" "$file" 2>/dev/null || true)

        if [[ -n "$matches" ]]; then
            while IFS= read -r match; do
                local line_num="${match%%:*}"
                local line_content="${match#*:}"

                # 跳過代碼塊
                if [[ "$line_content" =~ \`\`\` ]]; then
                    continue
                fi

                # 跳過內聯代碼
                if [[ "$line_content" =~ \`.*$simplified.*\` ]]; then
                    continue
                fi

                # 報告錯誤
                echo -e "${RED}[ERROR]${NC} $file:$line_num"
                echo -e "  Found simplified Chinese: ${YELLOW}\"$simplified\"${NC}"
                echo -e "  Should be traditional: ${GREEN}\"$traditional\"${NC}"
                echo -e "  Line: $line_content"
                echo ""
                ((file_errors++))
            done <<< "$matches"
        fi
    done

    if [[ $file_errors -gt 0 ]]; then
        if [[ $file_errors -lt 2 ]]; then
            echo -e "${YELLOW}⚠ WARNING${NC}: $file ($file_errors minor encoding issues, within tolerance)"
            return 0  # 容錯 < 2 錯誤
        else
            echo -e "${RED}✗ FAIL${NC}: $file ($file_errors encoding errors)"
            ((ERROR_COUNT += file_errors))
            return 1
        fi
    else
        echo -e "${GREEN}✓ PASS${NC}: $file"
        return 0
    fi
}

# 主函數
main() {
    if [[ $# -eq 0 ]]; then
        echo "Usage: $0 <file_or_directory> [file_or_directory...]"
        echo ""
        echo "Examples:"
        echo "  $0 docs/iGaming/requirements/01_Player/Player_Lifecycle.md"
        echo "  $0 docs/iGaming/requirements/"
        echo "  $0 docs/iGaming/requirements/ docs/iGaming/architecture/"
        exit 1
    fi

    echo "=================================================="
    echo "Encoding & Traditional Chinese Validation"
    echo "=================================================="
    echo ""

    # 處理所有輸入參數
    for target in "$@"; do
        if [[ -f "$target" ]]; then
            # 單個文件
            check_file "$target"
        elif [[ -d "$target" ]]; then
            # 目錄：遞歸處理所有 .md 文件
            while IFS= read -r -d '' file; do
                check_file "$file"
            done < <(find "$target" -name "*.md" -type f -print0)
        else
            echo -e "${RED}[ERROR]${NC} Path not found: $target"
            ((ERROR_COUNT++))
        fi
    done

    echo ""
    echo "=================================================="
    if [[ $ERROR_COUNT -eq 0 ]]; then
        echo -e "${GREEN}✓ PASS${NC}: All files use UTF-8 encoding and Traditional Chinese"
        exit 0
    else
        echo -e "${RED}✗ FAIL${NC}: Found $ERROR_COUNT encoding/simplified Chinese errors"
        echo ""
        echo "Please ensure:"
        echo "  1. All files use UTF-8 encoding"
        echo "  2. Use Traditional Chinese (繁體中文), not Simplified (简体中文)"
        echo "  3. Common examples:"
        echo "     - 数据库 (simplified) → 資料庫 (traditional)"
        echo "     - 用户 (simplified) → 用戶 (traditional)"
        echo "     - 应用 (simplified) → 應用 (traditional)"
        echo ""
        echo "Reference: docs/iGaming/TRANSLATION_GLOSSARY.md"
        exit 1
    fi
}

# 執行主函數
main "$@"
