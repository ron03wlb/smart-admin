---
name: report-generator
description: "報表匯出生成 (Excel/PDF/CSV)"
priority: P2
category: integration
---

# Report Generator

為 SmartAdmin 應用程式生成報表匯出功能，支援 Excel、PDF、CSV 格式，包括異步大檔案處理。

## Usage

```
User: "Add Excel export for employee list with department grouping"
AI: [Generate EasyExcel export, async processing, download API]
```

## When to Use

- 列表資料匯出 Excel
- 生成 PDF 報表
- CSV 資料導出
- 大檔案異步處理
- 定時報表生成

## Generated Output

- EasyExcel 匯出服務
- PDF 模板 (iText/JasperReports)
- CSV 匯出工具
- 異步處理配置
- 下載 API 端點

## Workflow

1. **分析匯出需求**
   - 欄位映射
   - 格式要求 (日期、數字)

2. **生成 Excel 匯出**
   - EasyExcel 配置
   - 欄位註解 (@ExcelProperty)
   - 樣式設定

3. **實現大檔案處理**
   - 分頁查詢
   - 異步生成
   - 進度回報

4. **配置下載 API**
   - 文件暫存
   - 過期清理

## Related Rules

- [F04-architecture-rules.md](../../../rules/foundation/F04-architecture-rules.md)

## Example Session

**User:** 為員工列表添加 Excel 匯出，按部門分組

**AI Agent Actions:**
1. 創建 `EmployeeExportVO` 匯出模型
2. 添加 @ExcelProperty 欄位註解
3. 實現 `EmployeeExportService` 匯出服務
4. 配置部門分組 (Sheet 或 Section)
5. 創建下載 API 端點
6. 添加大檔案異步處理
