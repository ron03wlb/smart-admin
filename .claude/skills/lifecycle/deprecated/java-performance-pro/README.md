# java-performance-pro - Quick Reference

**Version**: 1.0.0
**Priority**: P2
**Category**: Analysis
**Status**: ⚠️ Soft Deprecated (建議使用 smartadmin-performance-suite)

---

## 🚀 快速觸發

**關鍵字**:
- "分析性能瓶頸"
- "JVM 優化建議"
- "N+1 查詢檢測"
- "CPU hotspot"
- "記憶體洩漏"

---

## 📊 核心功能

1. **N+1 Query Detection** - 識別 MyBatis Plus N+1 查詢模式
2. **JVM Memory Tuning** - 堆大小、GC 策略、記憶體洩漏檢測
3. **CPU Hotspot Analysis** - 方法分析、執行緒競爭
4. **Redis Cache Optimization** - 快取命中率、TTL 調優
5. **SQL Performance Analysis** - 慢查詢分析、索引建議

---

## ⚠️ 棄用說明

此 skill 已進入 soft deprecation 階段（遷移日期: 2026-01-27）。

**建議替代方案**: [smartadmin-performance-suite](../../composite/smartadmin-performance-suite/)

**替代 skill 優勢**:
- 整合 12 種性能分析工具（包含本 skill 所有功能）
- 支援多模式執行（診斷、深度分析、持續監控）
- 自動生成 Grafana 儀表板
- 包含壓力測試整合（JMeter, Gatling）

---

## 📖 詳細文檔

參考 [SKILL.md](SKILL.md) 完整指南

---

**Last Updated**: 2026-01-30
