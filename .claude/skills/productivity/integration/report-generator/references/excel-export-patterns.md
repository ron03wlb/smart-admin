# Excel Export Patterns Guide

**Skill:** report-generator
**Component:** Apache POI / EasyExcel
**Purpose:** Generate Excel reports with templates, styling, formulas, and charts

---

## Library Comparison

| Feature | Apache POI | EasyExcel | Recommendation |
|---------|-----------|-----------|----------------|
| Memory Usage | High (entire file in memory) | Low (streaming) | EasyExcel for large files |
| Performance | Moderate | Fast | EasyExcel for > 10K rows |
| Features | Rich (formulas, charts, macros) | Basic (data export) | POI for complex reports |
| API Complexity | Complex | Simple | EasyExcel for simple exports |

**Rule of Thumb:**
- **< 10K rows:** Use Apache POI (richer features)
- **> 10K rows:** Use EasyExcel (better performance)
- **Complex formatting:** Use Apache POI
- **Simple data export:** Use EasyExcel

---

## Pattern 1: EasyExcel Simple Export

**Use Case:** Export list data quickly (most common)

### Basic Export

```java
package net.lab1024.sa.business.user.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserExportService {

    private final UserManager userManager;

    /**
     * Export users to Excel
     */
    public String exportUsers(UserQueryForm form) {
        // 1. Query data
        List<UserExportVO> users = userManager.listForExport(form);

        // 2. Generate file path
        String fileName = "users_" + System.currentTimeMillis() + ".xlsx";
        String filePath = "/tmp/exports/" + fileName;

        // 3. Write to Excel
        EasyExcel.write(filePath, UserExportVO.class)
                .sheet("Users")
                .doWrite(users);

        log.info("Exported {} users to {}", users.size(), filePath);
        return filePath;
    }
}

/**
 * Export VO with EasyExcel annotations
 */
@Data
public class UserExportVO {

    @ExcelProperty(value = "User ID", index = 0)
    private Long userId;

    @ExcelProperty(value = "Username", index = 1)
    private String username;

    @ExcelProperty(value = "Email", index = 2)
    private String email;

    @ExcelProperty(value = "Phone", index = 3)
    private String phone;

    @ExcelProperty(value = "Status", index = 4)
    private String statusName;

    @ExcelProperty(value = "Created Time", index = 5)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
```

### Export with Custom Styling

```java
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;

/**
 * Export with auto-width columns and styling
 */
public String exportWithStyle(List<UserExportVO> users) {
    String filePath = "/tmp/exports/users_styled.xlsx";

    // Header style
    WriteCellStyle headWriteCellStyle = new WriteCellStyle();
    headWriteCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    headWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);

    // Content style
    WriteCellStyle contentWriteCellStyle = new WriteCellStyle();
    contentWriteCellStyle.setHorizontalAlignment(HorizontalAlignment.LEFT);

    HorizontalCellStyleStrategy styleStrategy = new HorizontalCellStyleStrategy(
        headWriteCellStyle,
        contentWriteCellStyle
    );

    EasyExcel.write(filePath, UserExportVO.class)
            .registerWriteHandler(styleStrategy)
            .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())  // Auto-width
            .sheet("Users")
            .doWrite(users);

    return filePath;
}
```

---

## Pattern 2: EasyExcel Streaming (Large Datasets)

**Use Case:** Export millions of rows without memory issues

```java
@Service
@RequiredArgsConstructor
public class LargeDatasetExportService {

    private final UserManager userManager;

    /**
     * Export large dataset with pagination
     */
    public String exportLargeDataset() {
        String filePath = "/tmp/exports/users_large.xlsx";

        try (ExcelWriter excelWriter = EasyExcel.write(filePath, UserExportVO.class).build()) {
            WriteSheet writeSheet = EasyExcel.writerSheet("Users").build();

            int pageNum = 1;
            int pageSize = 1000;
            long totalExported = 0;

            while (true) {
                // Fetch data in batches
                List<UserExportVO> users = userManager.listByPage(pageNum, pageSize);

                if (users.isEmpty()) {
                    break;
                }

                // Write batch to Excel
                excelWriter.write(users, writeSheet);
                totalExported += users.size();

                log.info("Exported batch {}: {} rows (total: {})", pageNum, users.size(), totalExported);

                if (users.size() < pageSize) {
                    break;  // Last batch
                }

                pageNum++;
            }

            log.info("Export completed: {} total rows", totalExported);
        }

        return filePath;
    }
}
```

---

## Pattern 3: Apache POI Complex Reports

**Use Case:** Rich formatting, formulas, charts

### Excel with Formulas

```java
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
public class AdvancedExcelService {

    /**
     * Generate sales report with formulas
     */
    public String generateSalesReport(List<OrderVO> orders) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Sales Report");

        // Create header
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Order ID", "Product", "Quantity", "Unit Price", "Total"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(createHeaderStyle(workbook));
        }

        // Write data rows with formulas
        int rowNum = 1;
        for (OrderVO order : orders) {
            Row row = sheet.createRow(rowNum);
            row.createCell(0).setCellValue(order.getOrderId());
            row.createCell(1).setCellValue(order.getProductName());
            row.createCell(2).setCellValue(order.getQuantity());
            row.createCell(3).setCellValue(order.getUnitPrice().doubleValue());

            // Formula: Quantity * Unit Price
            Cell totalCell = row.createCell(4);
            totalCell.setCellFormula("C" + (rowNum + 1) + "*D" + (rowNum + 1));

            rowNum++;
        }

        // Add summary row with SUM formula
        Row summaryRow = sheet.createRow(rowNum);
        summaryRow.createCell(3).setCellValue("Total:");
        Cell sumCell = summaryRow.createCell(4);
        sumCell.setCellFormula("SUM(E2:E" + rowNum + ")");
        sumCell.setCellStyle(createBoldStyle(workbook));

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write to file
        String filePath = "/tmp/exports/sales_report.xlsx";
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            workbook.write(fos);
        }
        workbook.close();

        return filePath;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle createBoldStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}
```

### Excel with Charts

```java
import org.apache.poi.ss.usermodel.charts.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.*;

/**
 * Generate Excel with embedded chart
 */
public String generateChartReport(List<MonthlySalesVO> salesData) throws IOException {
    XSSFWorkbook workbook = new XSSFWorkbook();
    XSSFSheet sheet = workbook.createSheet("Monthly Sales");

    // Write data
    Row headerRow = sheet.createRow(0);
    headerRow.createCell(0).setCellValue("Month");
    headerRow.createCell(1).setCellValue("Sales Amount");

    int rowNum = 1;
    for (MonthlySalesVO data : salesData) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(data.getMonth());
        row.createCell(1).setCellValue(data.getAmount().doubleValue());
    }

    // Create chart
    XSSFDrawing drawing = sheet.createDrawingPatriarch();
    XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 4, 0, 14, 20);

    XSSFChart chart = drawing.createChart(anchor);
    chart.setTitleText("Monthly Sales Trend");
    chart.setTitleOverlay(false);

    // Create data sources
    XDDFDataSource<String> months = XDDFDataSourcesFactory.fromStringCellRange(
        sheet, new CellRangeAddress(1, rowNum - 1, 0, 0)
    );
    XDDFNumericalDataSource<Double> amounts = XDDFDataSourcesFactory.fromNumericCellRange(
        sheet, new CellRangeAddress(1, rowNum - 1, 1, 1)
    );

    // Create line chart
    XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
    XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);

    XDDFLineChartData lineChartData = (XDDFLineChartData) chart.createData(
        ChartTypes.LINE, bottomAxis, leftAxis
    );

    XDDFLineChartData.Series series = (XDDFLineChartData.Series) lineChartData.addSeries(months, amounts);
    series.setTitle("Sales", null);

    chart.plot(lineChartData);

    // Write file
    String filePath = "/tmp/exports/sales_chart.xlsx";
    try (FileOutputStream fos = new FileOutputStream(filePath)) {
        workbook.write(fos);
    }
    workbook.close();

    return filePath;
}
```

---

## Pattern 4: Multi-Sheet Workbook

```java
/**
 * Generate multi-sheet report
 */
public String generateMultiSheetReport() {
    String filePath = "/tmp/exports/multi_sheet_report.xlsx";

    try (ExcelWriter excelWriter = EasyExcel.write(filePath).build()) {
        // Sheet 1: User Summary
        List<UserExportVO> users = userManager.listForExport();
        WriteSheet sheet1 = EasyExcel.writerSheet(0, "Users").head(UserExportVO.class).build();
        excelWriter.write(users, sheet1);

        // Sheet 2: Order Summary
        List<OrderExportVO> orders = orderManager.listForExport();
        WriteSheet sheet2 = EasyExcel.writerSheet(1, "Orders").head(OrderExportVO.class).build();
        excelWriter.write(orders, sheet2);

        // Sheet 3: Statistics
        List<StatsExportVO> stats = statsManager.calculate();
        WriteSheet sheet3 = EasyExcel.writerSheet(2, "Statistics").head(StatsExportVO.class).build();
        excelWriter.write(stats, sheet3);
    }

    return filePath;
}
```

---

## Pattern 5: Template-Based Export

**Use Case:** Use pre-designed Excel template, fill data

```java
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.fill.FillConfig;

/**
 * Fill template with data
 */
public String exportWithTemplate(OrderVO order) {
    String templatePath = "/templates/invoice_template.xlsx";
    String outputPath = "/tmp/exports/invoice_" + order.getOrderId() + ".xlsx";

    // Prepare data
    Map<String, Object> data = new HashMap<>();
    data.put("orderNumber", order.getOrderNumber());
    data.put("customerName", order.getCustomerName());
    data.put("orderDate", order.getCreatedAt().format(DateTimeFormatter.ISO_DATE));
    data.put("totalAmount", order.getTotalAmount());

    // Fill template
    EasyExcel.write(outputPath)
            .withTemplate(templatePath)
            .sheet()
            .doFill(data);

    return outputPath;
}
```

---

## Pattern 6: Async Export with Download

**Use Case:** Long-running exports, return download link

```java
@Service
@RequiredArgsConstructor
public class AsyncExportService {

    private final UserManager userManager;
    private final FileService fileService;
    private final Executor asyncExecutor;

    /**
     * Initiate async export, return task ID
     */
    public ResponseDTO<String> initiateExport(UserQueryForm form, RequestUser requestUser) {
        String taskId = UUID.randomUUID().toString();

        // Submit async task
        CompletableFuture.runAsync(() -> {
            try {
                // Export data
                String localPath = exportUsers(form);

                // Upload to OSS/S3
                String downloadUrl = fileService.upload(new File(localPath));

                // Notify user (via WebSocket or save to DB for polling)
                notifyExportComplete(requestUser.getUserId(), taskId, downloadUrl);

                log.info("Async export completed: taskId={}, url={}", taskId, downloadUrl);

            } catch (Exception e) {
                log.error("Async export failed: taskId={}", taskId, e);
                notifyExportFailed(requestUser.getUserId(), taskId, e.getMessage());
            }
        }, asyncExecutor);

        return ResponseDTO.ok(taskId);
    }

    /**
     * Check export status
     */
    public ResponseDTO<ExportTaskVO> checkStatus(String taskId) {
        // Query from database or cache
        ExportTaskVO task = exportTaskDao.selectByTaskId(taskId);
        return ResponseDTO.ok(task);
    }
}
```

---

## SmartAdmin Integration Example

```java
package net.lab1024.sa.business.user.controller;

import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.OutputStream;

@Tag(name = "User Export")
@RestController
@RequestMapping("/api/user/export")
@RequiredArgsConstructor
public class UserExportController {

    private final UserExportService exportService;

    @Operation(summary = "Export users to Excel")
    @PostMapping("/excel")
    public void exportExcel(@RequestBody UserQueryForm form, HttpServletResponse response) {
        try {
            // Generate Excel
            String filePath = exportService.exportUsers(form);

            // Set response headers
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=users.xlsx");

            // Stream file to response
            try (FileInputStream fis = new FileInputStream(filePath);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

        } catch (Exception e) {
            log.error("Excel export failed", e);
            throw new BusinessException("Export failed: " + e.getMessage());
        }
    }

    @Operation(summary = "Initiate async export")
    @PostMapping("/async")
    public ResponseDTO<String> exportAsync(@RequestBody UserQueryForm form,
                                           @RequestUser RequestUser requestUser) {
        return exportService.initiateExport(form, requestUser);
    }

    @Operation(summary = "Check export status")
    @GetMapping("/status/{taskId}")
    public ResponseDTO<ExportTaskVO> checkStatus(@PathVariable String taskId) {
        return exportService.checkStatus(taskId);
    }
}
```

---

## Best Practices

### DO's ✅

1. **Use EasyExcel for large datasets**
2. **Stream responses** for immediate download
3. **Async export** for > 100K rows
4. **Add timeout** to prevent memory leaks
5. **Clean up temp files** after export

### DON'Ts ❌

1. **Don't load all data** into memory at once
2. **Don't block user** for long exports
3. **Don't forget** to close workbook/streams
4. **Don't expose** file system paths to users

---

**Next:** [PDF Generation](pdf-generation-patterns.md)
**Related:** [CSV Streaming](csv-streaming-patterns.md), [Scheduled Reports](scheduled-reports.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
