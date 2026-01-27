#!/bin/bash
# SmartAdmin Auto-Coding System One-Click Deployment Script
# Version: 1.0.0
# Author: SmartAdmin Auto-Coding System
# Date: 2026-01-27

set -e  # Exit immediately on error

# ============================================================================
# Color Configuration
# ============================================================================

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# ============================================================================
# Utility Functions
# ============================================================================

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[OK]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# ============================================================================
# Step 1: Prerequisites Check
# ============================================================================

check_prerequisites() {
    log_info "Checking prerequisites..."

    local has_error=0

    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl not found. Please install kubectl first."
        log_info "Install: https://kubernetes.io/docs/tasks/tools/"
        has_error=1
    else
        local kubectl_version=$(kubectl version --client --short 2>/dev/null || kubectl version --client 2>&1 | head -n 1)
        log_success "kubectl: $kubectl_version"
    fi

    # Check docker
    if ! command -v docker &> /dev/null; then
        log_error "docker not found. Please install docker first."
        log_info "Install: https://docs.docker.com/get-docker/"
        has_error=1
    else
        log_success "docker: $(docker --version)"
    fi

    # Check python3
    if ! command -v python3 &> /dev/null; then
        log_error "python3 not found. Please install python3 first."
        has_error=1
    else
        log_success "python3: $(python3 --version)"
    fi

    # Check psql or Python psycopg2
    if ! command -v psql &> /dev/null && ! python3 -c "import psycopg2" &> /dev/null 2>&1; then
        log_warn "Neither psql nor psycopg2 found. Database setup may fail."
        log_info "Install psql: sudo apt-get install postgresql-client (Linux)"
        log_info "Install psycopg2: pip3 install psycopg2-binary"
    else
        if command -v psql &> /dev/null; then
            log_success "psql: $(psql --version)"
        else
            log_success "Python psycopg2 module found"
        fi
    fi

    if [ $has_error -eq 1 ]; then
        log_error "Prerequisites check failed. Please install missing tools."
        exit 1
    fi

    log_success "Prerequisites check completed"
}

# ============================================================================
# Step 2: Create Namespace
# ============================================================================

create_namespace() {
    log_info "Creating Kubernetes namespace..."

    # Create namespace (ignore error if already exists)
    if kubectl create namespace smartadmin &> /dev/null; then
        log_success "Namespace 'smartadmin' created"
    else
        log_success "Namespace 'smartadmin' already exists"
    fi

    log_success "Namespace creation completed"
}

# ============================================================================
# Step 3: Deploy Secrets (Interactive)
# ============================================================================

deploy_secrets() {
    log_info "Deploying Secrets..."

    echo ""
    log_info "Please provide the following credentials:"
    echo ""

    # Prompt for Claude API Key
    read -p "Enter Claude API Key: " claude_api_key
    if [ -z "$claude_api_key" ]; then
        log_error "Claude API Key cannot be empty"
        exit 1
    fi

    # Prompt for Telegram Bot Token
    read -p "Enter Telegram Bot Token: " telegram_bot_token
    if [ -z "$telegram_bot_token" ]; then
        log_error "Telegram Bot Token cannot be empty"
        exit 1
    fi

    # Prompt for Telegram Chat ID
    read -p "Enter Telegram Chat ID: " telegram_chat_id
    if [ -z "$telegram_chat_id" ]; then
        log_error "Telegram Chat ID cannot be empty"
        exit 1
    fi

    echo ""
    log_info "Creating Kubernetes Secrets..."

    # Create Claude API credentials secret
    kubectl create secret generic claude-api-credentials \
        --from-literal=api-key="$claude_api_key" \
        --from-literal=base-url="https://api.anthropic.com" \
        --from-literal=default-model="claude-sonnet-4.5" \
        --from-literal=fallback-model="claude-haiku-3.5" \
        --namespace=smartadmin \
        --dry-run=client -o yaml | kubectl apply -f -

    if [ $? -eq 0 ]; then
        log_success "Claude API credentials created"
    else
        log_error "Failed to create Claude API credentials"
        exit 1
    fi

    # Create Telegram Bot credentials secret
    kubectl create secret generic telegram-bot-credentials \
        --from-literal=bot-token="$telegram_bot_token" \
        --from-literal=chat-id="$telegram_chat_id" \
        --namespace=smartadmin \
        --dry-run=client -o yaml | kubectl apply -f -

    if [ $? -eq 0 ]; then
        log_success "Telegram Bot credentials created"
    else
        log_error "Failed to create Telegram Bot credentials"
        exit 1
    fi

    log_success "Secrets deployment completed"
}

# ============================================================================
# Step 4: Deploy RBAC
# ============================================================================

deploy_rbac() {
    log_info "Deploying RBAC configuration..."

    # Apply RBAC configuration
    if kubectl apply -f ../k8s-manifests/rbac/agent-role.yaml; then
        log_success "RBAC configuration applied"
    else
        log_error "Failed to apply RBAC configuration"
        exit 1
    fi

    log_success "RBAC deployment completed"
}

# ============================================================================
# Step 5: Setup Database (Create PostgreSQL Audit Tables)
# ============================================================================

setup_database() {
    log_info "Creating PostgreSQL audit tables..."

    echo ""
    log_info "Choose database setup method:"
    echo "  1) Flyway migration (recommended)"
    echo "  2) Direct SQL execution"
    echo "  3) Skip database setup"
    read -p "Enter choice [1-3]: " db_choice

    case $db_choice in
        1)
            log_info "Running Flyway migration..."
            if [ -f "../smart-admin-api-java21-springboot3/gradlew" ]; then
                cd ../smart-admin-api-java21-springboot3
                if ./gradlew :sa-admin:flywayMigrate; then
                    log_success "Flyway migration completed"
                else
                    log_error "Flyway migration failed"
                    cd -
                    exit 1
                fi
                cd -
            else
                log_error "gradlew not found. Please run from correct directory."
                exit 1
            fi
            ;;
        2)
            log_info "Executing SQL directly..."
            read -p "Enter PostgreSQL host [localhost]: " pg_host
            pg_host=${pg_host:-localhost}
            read -p "Enter PostgreSQL port [5432]: " pg_port
            pg_port=${pg_port:-5432}
            read -p "Enter PostgreSQL database [smart_admin]: " pg_db
            pg_db=${pg_db:-smart_admin}
            read -p "Enter PostgreSQL user [postgres]: " pg_user
            pg_user=${pg_user:-postgres}

            SQL_FILE="../smart-admin-api-java21-springboot3/sa-admin/src/main/resources/db/migration/V999__ai_system_tables.sql"
            if [ -f "$SQL_FILE" ]; then
                if psql -h "$pg_host" -p "$pg_port" -U "$pg_user" -d "$pg_db" -f "$SQL_FILE"; then
                    log_success "SQL execution completed"
                else
                    log_error "SQL execution failed"
                    exit 1
                fi
            else
                log_error "SQL file not found: $SQL_FILE"
                exit 1
            fi
            ;;
        3)
            log_warn "Skipping database setup"
            ;;
        *)
            log_error "Invalid choice"
            exit 1
            ;;
    esac

    log_success "Database setup completed"
}

# ============================================================================
# Step 6: Build Telegram Webhook Image
# ============================================================================

build_webhook_image() {
    log_info "Building Telegram Webhook Docker image..."

    # Build Docker image
    cd telegram-webhook
    if docker build -t smartadmin/telegram-webhook:latest .; then
        log_success "Docker image built: smartadmin/telegram-webhook:latest"
    else
        log_error "Docker image build failed"
        cd -
        exit 1
    fi
    cd -

    # Verify image
    if docker images | grep -q "smartadmin/telegram-webhook"; then
        log_success "Docker image verified"
    else
        log_error "Docker image not found after build"
        exit 1
    fi

    log_success "Docker image build completed"
}

# ============================================================================
# Step 7: Deploy Telegram Webhook
# ============================================================================

deploy_webhook() {
    log_info "Deploying Telegram Webhook..."

    # Apply Telegram Webhook deployment
    if kubectl apply -f ../k8s-manifests/telegram-webhook/deployment.yaml; then
        log_success "Telegram Webhook deployed"
    else
        log_error "Failed to deploy Telegram Webhook"
        exit 1
    fi

    # Wait for pods to be ready
    log_info "Waiting for pods to be ready..."
    if kubectl wait --for=condition=ready pod -l component=telegram-webhook -n smartadmin --timeout=120s; then
        log_success "Pods are ready"
    else
        log_warn "Pods took longer than expected to be ready"
    fi

    log_success "Webhook deployment completed"
}

# ============================================================================
# Step 8: Verify Deployment
# ============================================================================

verify_deployment() {
    log_info "Verifying deployment status..."

    echo ""
    log_info "=== All Resources ==="
    kubectl get all -n smartadmin

    echo ""
    log_info "=== Secrets ==="
    kubectl get secrets -n smartadmin

    echo ""
    log_info "=== Recent Logs ==="
    kubectl logs -l app=smartadmin-auto-coding -n smartadmin --tail=20 || log_warn "No logs available yet"

    echo ""
    log_info "=== Pod Status ==="
    POD_COUNT=$(kubectl get pods -l component=telegram-webhook -n smartadmin --no-headers 2>/dev/null | wc -l)
    RUNNING_COUNT=$(kubectl get pods -l component=telegram-webhook -n smartadmin --field-selector=status.phase=Running --no-headers 2>/dev/null | wc -l)

    if [ "$POD_COUNT" -gt 0 ] && [ "$RUNNING_COUNT" -gt 0 ]; then
        log_success "Pods running: $RUNNING_COUNT/$POD_COUNT"
    else
        log_warn "Pods not fully running yet: $RUNNING_COUNT/$POD_COUNT"
    fi

    log_success "Deployment verification completed"
}

# ============================================================================
# Step 9: Test Telegram Notification
# ============================================================================

test_telegram() {
    log_info "Testing Telegram notification..."

    echo ""
    read -p "Do you want to test Telegram notification? [y/N]: " test_choice
    if [[ ! "$test_choice" =~ ^[Yy]$ ]]; then
        log_info "Skipping Telegram notification test"
        return 0
    fi

    # Port forward in background
    log_info "Setting up port forward..."
    kubectl port-forward -n smartadmin svc/telegram-webhook 8080:8080 > /dev/null 2>&1 &
    PORT_FORWARD_PID=$!

    # Wait for port forward to establish
    sleep 3

    # Test /health endpoint first
    log_info "Testing /health endpoint..."
    HEALTH_RESPONSE=$(curl -s http://localhost:8080/health 2>/dev/null)
    if echo "$HEALTH_RESPONSE" | grep -q "healthy"; then
        log_success "Health check passed: $HEALTH_RESPONSE"
    else
        log_warn "Health check did not return expected response"
    fi

    # Test /workflow endpoint
    log_info "Testing /workflow endpoint..."
    WORKFLOW_RESPONSE=$(curl -s -X POST http://localhost:8080/workflow \
         -H "Content-Type: application/json" \
         -d '{"workflow_name":"deployment-test","status":"Succeeded","message":"Deployment completed successfully","duration":"3m","node_count":1}' 2>/dev/null)

    if echo "$WORKFLOW_RESPONSE" | grep -q "sent"; then
        log_success "Workflow notification sent"
        log_info "Check your Telegram chat for the message"
    else
        log_warn "Workflow notification may have failed: $WORKFLOW_RESPONSE"
    fi

    # Cleanup
    kill $PORT_FORWARD_PID 2>/dev/null || true
    wait $PORT_FORWARD_PID 2>/dev/null || true

    log_success "Telegram notification test completed"
}

# ============================================================================
# Main Function
# ============================================================================

main() {
    echo "========================================================================"
    echo "SmartAdmin Auto-Coding System Deployment"
    echo "Version: 1.0.0"
    echo "========================================================================"
    echo ""

    # Execute all steps
    check_prerequisites
    create_namespace
    deploy_secrets
    deploy_rbac
    setup_database
    build_webhook_image
    deploy_webhook
    verify_deployment
    test_telegram

    echo ""
    echo "========================================================================"
    log_success "Deployment completed!"
    echo "========================================================================"
    echo ""
    echo "Next steps:"
    echo "  1. Verify audit tables: SELECT * FROM t_ai_operation_audit LIMIT 1;"
    echo "  2. Test Telegram notification via webhook endpoint"
    echo "  3. Begin P0-3 code implementation (file access guard, webhook logic)"
    echo ""
}

# Execute main function
main "$@"
