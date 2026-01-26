#!/bin/bash
# SmartAdmin AI 系統快速部署腳本
# 實施 P0-3 安全修復

set -e  # 遇到錯誤立即退出

# 顏色輸出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日誌函數
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 檢查前置條件
check_prerequisites() {
    log_info "檢查前置條件..."

    # 檢查 kubectl
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl 未安裝，請先安裝 kubectl"
        exit 1
    fi

    # 檢查 kubectl 連接
    if ! kubectl cluster-info &> /dev/null; then
        log_error "無法連接到 Kubernetes 集群，請檢查 kubeconfig"
        exit 1
    fi

    # 檢查 docker
    if ! command -v docker &> /dev/null; then
        log_warn "Docker 未安裝，將跳過鏡像構建步驟"
        SKIP_BUILD=true
    fi

    log_info "前置條件檢查通過 ✅"
}

# 創建命名空間
create_namespace() {
    log_info "創建命名空間 smartadmin..."

    if kubectl get namespace smartadmin &> /dev/null; then
        log_warn "命名空間 smartadmin 已存在，跳過創建"
    else
        kubectl create namespace smartadmin
        log_info "命名空間創建成功 ✅"
    fi

    # 設置默認命名空間
    kubectl config set-context --current --namespace=smartadmin
}

# 部署 Secrets
deploy_secrets() {
    log_info "部署 Secrets..."

    # Telegram Bot 憑證
    log_info "部署 Telegram Bot 憑證..."
    if kubectl get secret telegram-bot-credentials -n smartadmin &> /dev/null; then
        log_warn "Secret telegram-bot-credentials 已存在"
        read -p "是否覆蓋？(y/N) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            kubectl delete secret telegram-bot-credentials -n smartadmin
            kubectl apply -f ../k8s-manifests/secrets/telegram-credentials.yaml
        fi
    else
        kubectl apply -f ../k8s-manifests/secrets/telegram-credentials.yaml
    fi

    # Claude API 憑證
    log_info "部署 Claude API 憑證..."
    if kubectl get secret claude-api-credentials -n smartadmin &> /dev/null; then
        log_warn "Secret claude-api-credentials 已存在"
        read -p "是否覆蓋？(y/N) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            kubectl delete secret claude-api-credentials -n smartadmin
            kubectl apply -f ../k8s-manifests/secrets/claude-api-credentials.yaml
        fi
    else
        kubectl apply -f ../k8s-manifests/secrets/claude-api-credentials.yaml
    fi

    log_info "Secrets 部署完成 ✅"
}

# 部署 RBAC
deploy_rbac() {
    log_info "部署 RBAC 配置..."

    kubectl apply -f ../k8s-manifests/rbac/agent-role.yaml

    # 驗證
    if kubectl get serviceaccount smartadmin-agent -n smartadmin &> /dev/null; then
        log_info "ServiceAccount 創建成功 ✅"
    else
        log_error "ServiceAccount 創建失敗"
        exit 1
    fi

    log_info "RBAC 部署完成 ✅"
}

# 創建 PostgreSQL 表
create_postgres_tables() {
    log_info "創建 PostgreSQL 審計表..."

    # 檢查是否在 SmartAdmin 根目錄
    if [ ! -f "../smart-admin-api-java21-springboot3/gradlew" ]; then
        log_error "未找到 SmartAdmin Gradle 項目，請確認路徑"
        exit 1
    fi

    log_info "運行 Flyway 遷移..."
    cd ../smart-admin-api-java21-springboot3
    ./gradlew :sa-admin:flywayMigrate

    if [ $? -eq 0 ]; then
        log_info "PostgreSQL 表創建成功 ✅"
    else
        log_error "PostgreSQL 表創建失敗"
        exit 1
    fi

    cd ../k8s-agents
}

# 構建 Telegram Webhook 鏡像
build_telegram_webhook() {
    if [ "$SKIP_BUILD" = true ]; then
        log_warn "跳過鏡像構建"
        return
    fi

    log_info "構建 Telegram Webhook 鏡像..."

    cd telegram-webhook
    docker build -t smartadmin/telegram-webhook:latest .

    if [ $? -eq 0 ]; then
        log_info "Telegram Webhook 鏡像構建成功 ✅"
    else
        log_error "鏡像構建失敗"
        exit 1
    fi

    cd ..
}

# 部署 Telegram Webhook
deploy_telegram_webhook() {
    log_info "部署 Telegram Webhook..."

    kubectl apply -f ../k8s-manifests/telegram-webhook/deployment.yaml

    # 等待 Pod 就緒
    log_info "等待 Pod 就緒..."
    kubectl wait --for=condition=ready pod -l app=telegram-webhook --timeout=60s

    if [ $? -eq 0 ]; then
        log_info "Telegram Webhook 部署成功 ✅"
    else
        log_error "Telegram Webhook 部署失敗"
        kubectl logs -l app=telegram-webhook --tail=50
        exit 1
    fi
}

# 驗證部署
verify_deployment() {
    log_info "驗證部署..."

    echo ""
    log_info "=== 命名空間 ==="
    kubectl get namespace smartadmin

    echo ""
    log_info "=== Secrets ==="
    kubectl get secrets -n smartadmin

    echo ""
    log_info "=== ServiceAccount ==="
    kubectl get serviceaccount smartadmin-agent -n smartadmin

    echo ""
    log_info "=== RoleBindings ==="
    kubectl get rolebindings -l app=smartadmin-ai -n smartadmin

    echo ""
    log_info "=== Pods ==="
    kubectl get pods -l app=telegram-webhook -n smartadmin

    echo ""
    log_info "=== Services ==="
    kubectl get services -l app=telegram-webhook -n smartadmin
}

# 測試 Telegram 通知
test_telegram() {
    log_info "測試 Telegram 通知..."

    log_info "創建端口轉發..."
    kubectl port-forward svc/telegram-webhook 8080:8080 &
    PORT_FORWARD_PID=$!
    sleep 3

    log_info "發送測試消息..."
    curl -X POST http://localhost:8080/workflow \
      -H "Content-Type: application/json" \
      -d '{
        "workflow_name": "deployment-test",
        "status": "success",
        "duration": "5s",
        "message": "✅ SmartAdmin AI 系統部署成功！P0-3 安全修復已完成。"
      }'

    # 停止端口轉發
    kill $PORT_FORWARD_PID 2>/dev/null

    echo ""
    log_info "請檢查 Telegram 是否收到測試消息"
}

# 主函數
main() {
    echo "========================================="
    echo " SmartAdmin AI 系統快速部署"
    echo " P0-3 安全修復"
    echo "========================================="
    echo ""

    # 檢查前置條件
    check_prerequisites

    # 詢問部署選項
    read -p "是否部署所有組件？(Y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Nn]$ ]]; then
        create_namespace
        deploy_secrets
        deploy_rbac
        create_postgres_tables
        build_telegram_webhook
        deploy_telegram_webhook
        verify_deployment
        test_telegram
    else
        echo "請選擇要部署的組件："
        echo "1. 創建命名空間"
        echo "2. 部署 Secrets"
        echo "3. 部署 RBAC"
        echo "4. 創建 PostgreSQL 表"
        echo "5. 構建 Telegram Webhook 鏡像"
        echo "6. 部署 Telegram Webhook"
        echo "7. 驗證部署"
        echo "8. 測試 Telegram 通知"
        read -p "請輸入選項 (1-8): " -n 1 -r
        echo

        case $REPLY in
            1) create_namespace ;;
            2) deploy_secrets ;;
            3) deploy_rbac ;;
            4) create_postgres_tables ;;
            5) build_telegram_webhook ;;
            6) deploy_telegram_webhook ;;
            7) verify_deployment ;;
            8) test_telegram ;;
            *) log_error "無效選項" && exit 1 ;;
        esac
    fi

    echo ""
    echo "========================================="
    log_info "🎉 部署完成！"
    echo "========================================="
    echo ""
    echo "下一步："
    echo "1. 檢查 Telegram 是否收到測試消息"
    echo "2. 查看部署狀態: kubectl get all -n smartadmin"
    echo "3. 查看 Telegram Webhook 日誌: kubectl logs -l app=telegram-webhook -n smartadmin"
    echo "4. 繼續實施其他組件（CrewAI Crews、Argo Workflows）"
    echo ""
}

# 執行主函數
main
