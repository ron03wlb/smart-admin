# 09 基礎設施（Infrastructure）

> **目標讀者**: Architects, Backend Developers, DevOps
> **狀態**: 21 份架構文件

---

## 拆分文件

| 文件 | 說明 |
|------|------|
| [基礎設施實作](Infrastructure_Implementation.md) | API Gateway 配置、Blue-Green K8s/Istio 部署、Redisson 限流 |

## 部署與運維

| 文件 | 說明 |
|------|------|
| [部署架構](Deployment_Architecture.md) | 基礎設施部署架構 |
| [QA 標準](QA_Standards.md) | 品質保證標準與測試 |
| [系統維護](Maintenance_Architecture.md) | 系統維護程序 |

## API Gateway

| 文件 | 說明 |
|------|------|
| [Gateway 核心](Gateway_Core.md) | API Gateway 核心架構 |
| [Gateway 限流](Gateway_Rate_Limiting.md) | 限流與節流控制 |
| [Gateway 安全](Gateway_Security.md) | Gateway 安全配置 |

## API 設計

| 文件 | 說明 |
|------|------|
| [API 設計原則](API_Design_Principles.md) | API 設計原則與標準 |
| [身份驗證架構](Authentication_Architecture.md) | API 身份驗證機制 |
| [通用模式](Common_Patterns.md) | API 通用模式與慣例 |
| [領域 API](Domain_APIs.md) | 領域特定 API 規格 |

## 性能與優化

| 文件 | 說明 |
|------|------|
| [性能監控](Performance_Monitoring.md) | 系統性能監控 |
| [性能優化](Performance_Optimization.md) | 性能調校指南 |
| [串流處理架構](Stream_Processing_Architecture.md) | 串流處理架構 |
| [緩存策略](Caching_Strategy.md) | 緩存架構與策略 |
| [成本優化架構](Cost_Optimization_Architecture.md) | 基礎設施成本優化 |

## 安全與 Token 管理

| 文件 | 說明 |
|------|------|
| [OAuth Refresh Token](OAuth_Refresh_Token.md) | OAuth Refresh Token 實施方案 |
| [多主體 Token 安全](Multi_Actor_Token_Security.md) | 多主體 Token 安全模型 |

## Token 驗證服務

| 文件 | 說明 |
|------|------|
| [Token 驗證服務](Token_Validation_Service.md) | Token 驗證服務總覽 |
| [Token 驗證架構](Token_Validation_Architecture.md) | Token 驗證服務架構 |
| [Token 緩存性能](Token_Cache_Performance.md) | Token 緩存性能優化 |
| [Token 邊緣部署](Token_Edge_Deployment.md) | 邊緣部署模式 |

---

**最後更新**: 2026-02-12
