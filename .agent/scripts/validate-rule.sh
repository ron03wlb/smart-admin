#!/bin/bash
# 验证 .agent/rules 和 .agent/workflows 文件格式规范
#
# 使用方式:
#   ./validate-rule.sh <rule-file>              # 验证单个文件
#   ./validate-rule.sh rules/01-naming.md       # 相对路径
#   ./validate-rule.sh                          # 验证所有文件

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 计数器
TOTAL_FILES=0
PASSED_FILES=0
FAILED_FILES=0
WARNINGS=0

# 打印分隔线
print_separator() {
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

# 打印成功消息
print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# 打印错误消息
print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# 打印警告消息
print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# 打印信息消息
print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# 验证单个文件
validate_file() {
    local file=$1
    local file_errors=0

    echo ""
    print_info "验证文件: $file"
    echo ""

    # 检查文件是否存在
    if [ ! -f "$file" ]; then
        print_error "文件不存在: $file"
        return 1
    fi

    # 检查文件是否为 markdown
    if [[ ! "$file" =~ \.md$ ]]; then
        print_warning "文件不是 Markdown 格式: $file"
        ((WARNINGS++))
    fi

    # 1. 检查 front matter 存在
    if ! grep -q "^---$" "$file"; then
        print_error "缺少 front matter 分隔符 (---)"
        ((file_errors++))
    else
        print_success "Front matter 格式正确"
    fi

    # 2. 检查必填字段
    required_fields=(
        "trigger"
        "description"
        "tags"
        "positioning"
        "ai_role"
        "last_updated"
    )

    echo ""
    print_info "检查必填字段..."
    for field in "${required_fields[@]}"; do
        if ! grep -q "^${field}:" "$file"; then
            print_error "缺少必填字段: $field"
            ((file_errors++))
        else
            print_success "字段存在: $field"
        fi
    done

    # 3. 检查 AI 指令区块
    echo ""
    print_info "检查 AI 指令区块..."
    if ! grep -q "## 🤖 AI 指令區塊" "$file"; then
        print_error "缺少 AI 指令区块标题"
        ((file_errors++))
    else
        print_success "AI 指令区块标题存在"

        # 检查 AI 指令区块的子部分
        if ! grep -q "### 何時應用此規則" "$file"; then
            print_error "AI 指令区块缺少: 何時應用此規則"
            ((file_errors++))
        else
            print_success "包含: 何時應用此規則"
        fi

        if ! grep -q "### 強制執行檢查清單" "$file"; then
            print_error "AI 指令区块缺少: 強制執行檢查清單"
            ((file_errors++))
        else
            print_success "包含: 強制執行檢查清單"
        fi

        if ! grep -q "### AI 決策樹" "$file"; then
            print_warning "建議添加: AI 決策樹"
            ((WARNINGS++))
        else
            print_success "包含: AI 決策樹"
        fi

        if ! grep -q "### 錯誤模式檢測" "$file" && ! grep -q "### 生成代碼" "$file"; then
            print_warning "建議添加: 錯誤模式檢測 或 生成代碼模板"
            ((WARNINGS++))
        else
            print_success "包含: 錯誤模式檢測/生成代碼模板"
        fi
    fi

    # 4. 檢查文件大小（≤ 11000 字元）
    echo ""
    print_info "檢查文件大小..."
    char_count=$(wc -c < "$file" | tr -d ' ')

    # 豁免索引文件（00- 開頭的文件）
    if [[ "$file" =~ 00-(ai-decision-matrix|workflow-index)\.md$ ]]; then
        print_warning "索引文件豁免大小限制: $char_count 字元"
        ((WARNINGS++))
    elif [ "$char_count" -gt 11000 ]; then
        print_error "文件過大: $char_count 字元（限制 11000 字元）"
        print_info "建議拆分為多個文件或精簡內容"
        ((file_errors++))
    else
        print_success "文件大小合適: $char_count 字元"
    fi

    # 5. 檢查 positioning 值是否合法
    echo ""
    print_info "檢查 positioning 值..."
    positioning=$(grep "^positioning:" "$file" | cut -d':' -f2 | tr -d ' ')
    valid_positions=("current-standard" "ideal" "migration")

    if [[ " ${valid_positions[@]} " =~ " ${positioning} " ]]; then
        print_success "Positioning 值合法: $positioning"
    else
        print_error "Positioning 值非法: $positioning"
        print_info "合法值: ${valid_positions[*]}"
        ((file_errors++))
    fi

    # 6. 檢查 ai_role 是否存在
    echo ""
    print_info "檢查 ai_role 配置..."
    if grep -q "^ai_role:" "$file"; then
        ai_role=$(grep "^ai_role:" "$file" | cut -d':' -f2 | tr -d ' ')
        print_success "AI 角色已配置: $ai_role"
    else
        print_error "未配置 ai_role"
        ((file_errors++))
    fi

    # 7. 檢查日期格式（YYYY-MM-DD）
    echo ""
    print_info "檢查日期格式..."
    if grep -q "^last_updated: [0-9]\{4\}-[0-9]\{2\}-[0-9]\{2\}$" "$file"; then
        last_updated=$(grep "^last_updated:" "$file" | cut -d':' -f2 | tr -d ' ')
        print_success "日期格式正確: $last_updated"
    else
        print_error "日期格式錯誤，應為 YYYY-MM-DD 格式"
        ((file_errors++))
    fi

    # 總結
    echo ""
    print_separator
    if [ $file_errors -eq 0 ]; then
        print_success "文件驗證通過: $file"
        return 0
    else
        print_error "文件驗證失敗: $file（發現 $file_errors 個錯誤）"
        return 1
    fi
}

# 主函數
main() {
    print_separator
    echo -e "${BLUE}📋 .agent 文檔格式驗證工具${NC}"
    print_separator

    # 獲取項目根目錄
    SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
    AGENT_DIR="$(dirname "$SCRIPT_DIR")"

    # 如果指定了文件，驗證單個文件
    if [ $# -gt 0 ]; then
        file="$1"

        # 處理相對路徑
        if [[ ! "$file" =~ ^/ ]]; then
            # 如果路徑不以 / 開頭，嘗試不同的位置
            if [ -f "$file" ]; then
                file="$file"
            elif [ -f "$AGENT_DIR/$file" ]; then
                file="$AGENT_DIR/$file"
            elif [ -f "$AGENT_DIR/rules/$file" ]; then
                file="$AGENT_DIR/rules/$file"
            elif [ -f "$AGENT_DIR/workflows/$file" ]; then
                file="$AGENT_DIR/workflows/$file"
            else
                print_error "找不到文件: $file"
                exit 1
            fi
        fi

        TOTAL_FILES=1
        if validate_file "$file"; then
            PASSED_FILES=1
        else
            FAILED_FILES=1
        fi
    else
        # 驗證所有 rules 和 workflows 文件
        print_info "驗證所有規則文件..."
        echo ""

        # 驗證 rules
        for file in "$AGENT_DIR/rules"/*.md; do
            if [ -f "$file" ]; then
                ((TOTAL_FILES++))
                if validate_file "$file"; then
                    ((PASSED_FILES++))
                else
                    ((FAILED_FILES++))
                fi
            fi
        done

        # 驗證 workflows
        for file in "$AGENT_DIR/workflows"/*.md; do
            if [ -f "$file" ]; then
                ((TOTAL_FILES++))
                if validate_file "$file"; then
                    ((PASSED_FILES++))
                else
                    ((FAILED_FILES++))
                fi
            fi
        done
    fi

    # 最終統計
    echo ""
    print_separator
    echo -e "${BLUE}📊 驗證統計${NC}"
    print_separator
    echo "總文件數: $TOTAL_FILES"
    echo -e "${GREEN}通過: $PASSED_FILES${NC}"
    echo -e "${RED}失敗: $FAILED_FILES${NC}"
    echo -e "${YELLOW}警告: $WARNINGS${NC}"
    print_separator

    # 退出碼
    if [ $FAILED_FILES -gt 0 ]; then
        print_error "驗證失敗！請修復上述錯誤"
        exit 1
    else
        print_success "所有文件驗證通過！"
        if [ $WARNINGS -gt 0 ]; then
            print_warning "建議處理上述警告"
        fi
        exit 0
    fi
}

# 執行主函數
main "$@"
