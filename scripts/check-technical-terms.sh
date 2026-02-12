#!/bin/bash
# check-technical-terms.sh
# 檢測 iGaming 文檔中技術術語是否保留英文（不應被翻譯成中文）
#
# 使用方式:
#   bash scripts/check-technical-terms.sh <file_or_directory>
#   bash scripts/check-technical-terms.sh docs/iGaming/requirements/01_Player/Player_Lifecycle.md
#   bash scripts/check-technical-terms.sh docs/iGaming/requirements/
#
# Exit Codes:
#   0 - PASS (所有技術術語保留英文)
#   1 - FAIL (發現技術術語被翻譯成中文)

set -euo pipefail

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 錯誤計數器
ERROR_COUNT=0

# 檢測技術術語誤翻的函數
check_file() {
    local file="$1"
    local file_errors=0

    # 跳過非 Markdown 文件
    if [[ ! "$file" =~ \.md$ ]]; then
        return 0
    fi

    # 跳過 TRANSLATION_GLOSSARY.md（詞彙表本身包含中文術語）
    if [[ "$file" =~ TRANSLATION_GLOSSARY\.md$ ]]; then
        return 0
    fi

    # 定義常見誤翻模式（技術術語的中文翻譯，應保持英文）
    # 格式: "中文誤翻|正確英文術語"
    declare -a MISUSE_PATTERNS=(
        # SmartAdmin 架構層（在類名上下文中不應翻譯）
        "控制器類|Controller"
        "服務類|Service"
        "管理類|Manager"
        "數據訪問類|Dao"
        "倉儲類|Repository"
        "實體類|Entity class"
        "視圖對象|VO"
        "數據傳輸對象|DTO"
        "表單對象|Form"
        "查詢表單|QueryForm"
        "更新表單|UpdateForm"
        "響應對象|ResponseDTO"
        "分頁結果|PageResult"

        # Spring 注解（不應翻譯）
        "事務注解|@Transactional"
        "構造器注入|@RequiredArgsConstructor"
        "權限檢查|@SaCheckPermission"
        "無需登入|@NoNeedLogin"
        "緩存注解|@Cacheable"
        "服務類標註|@Service"
        "組件標註|@Component"
        "控制器標註|@RestController"

        # 常見技術術語（在代碼/技術上下文中不應翻譯）
        "應用程式介面|API"
        "超文本傳輸協議|HTTP"
        "安全超文本傳輸協議|HTTPS"
        "數據交換格式|JSON"
        "配置文件格式|YAML"
        "數據庫系統|PostgreSQL"
        "緩存數據庫|Redis"
        "消息隊列|Kafka"
        "多租戶架構|Multi-Tenant"
        "行級安全|Row-Level Security"
        "令牌|Token"
        "授權協議|OAuth"
        "單點登入|SSO"
        "傳輸層安全|TLS"

        # Vavr 類型（不應翻譯）
        "可選類型|Option"
        "異常處理|Try"
        "二選一類型|Either"
    )

    # 檢測每個誤翻模式
    for pattern in "${MISUSE_PATTERNS[@]}"; do
        local chinese_term="${pattern%%|*}"
        local english_term="${pattern##*|}"

        # 使用 grep 搜尋中文術語（排除代碼塊和表格）
        # 注意：這是簡化檢測，實際使用中可能需要更精確的過濾
        local matches=$(grep -n "$chinese_term" "$file" 2>/dev/null || true)

        if [[ -n "$matches" ]]; then
            # 檢查是否在合法上下文中（例如表格、註釋、說明文字）
            # 簡化版本：如果出現在 | (表格) 或 > (引用) 或 - (列表) 開頭，視為合法
            while IFS= read -r match; do
                local line_num="${match%%:*}"
                local line_content="${match#*:}"

                # 跳過表格行（包含 |）
                if [[ "$line_content" =~ \| ]]; then
                    continue
                fi

                # 跳過註釋行（以 > 或 # 開頭）
                if [[ "$line_content" =~ ^[[:space:]]*[\>\#] ]]; then
                    continue
                fi

                # 跳過代碼塊（包含 ```)
                if [[ "$line_content" =~ \`\`\` ]]; then
                    continue
                fi

                # 報告錯誤
                echo -e "${RED}[ERROR]${NC} $file:$line_num"
                echo -e "  Found: ${YELLOW}\"$chinese_term\"${NC}"
                echo -e "  Should be: ${GREEN}\"$english_term\"${NC}"
                echo -e "  Line: $line_content"
                echo ""
                ((file_errors++))
            done <<< "$matches"
        fi
    done

    # 檢測類名誤翻模式（例如：PlayerService 不應翻成"玩家服務"）
    # 模式：中文 + Service/Controller/Manager/Dao/Repository/Entity/VO/DTO
    local class_misuse=$(grep -nE '[\u4e00-\u9fff]+(Service|Controller|Manager|Dao|Repository|Entity|VO|DTO|Form)' "$file" 2>/dev/null || true)

    if [[ -n "$class_misuse" ]]; then
        while IFS= read -r match; do
            local line_num="${match%%:*}"
            local line_content="${match#*:}"

            # 跳過代碼塊
            if [[ "$line_content" =~ \`\`\` ]]; then
                continue
            fi

            echo -e "${RED}[ERROR]${NC} $file:$line_num"
            echo -e "  Found: ${YELLOW}Possible class name mistranslation${NC}"
            echo -e "  Line: $line_content"
            echo -e "  Hint: Class names (PlayerService, UserController, etc.) should remain in English"
            echo ""
            ((file_errors++))
        done <<< "$class_misuse"
    fi

    if [[ $file_errors -gt 0 ]]; then
        echo -e "${RED}✗ FAIL${NC}: $file ($file_errors errors)"
        ((ERROR_COUNT += file_errors))
        return 1
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
    echo "Technical Terms Validation"
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
        echo -e "${GREEN}✓ PASS${NC}: All technical terms preserved in English"
        exit 0
    else
        echo -e "${RED}✗ FAIL${NC}: Found $ERROR_COUNT technical term mistranslations"
        echo ""
        echo "Please ensure:"
        echo "  1. Class names remain in English (PlayerService, UserController, etc.)"
        echo "  2. Technical terms remain in English (API, PostgreSQL, Redis, etc.)"
        echo "  3. Annotations remain in English (@Transactional, @Service, etc.)"
        echo ""
        echo "Reference: docs/iGaming/TRANSLATION_GLOSSARY.md"
        exit 1
    fi
}

# 執行主函數
main "$@"
