# Report Generator - Quick Reference

## EasyExcel 匯出模型

```java
@Data
public class EmployeeExportVO {
    @ExcelProperty("員工編號")
    private Long employeeId;

    @ExcelProperty("姓名")
    private String employeeName;

    @ExcelProperty("部門")
    private String departmentName;

    @ExcelProperty("入職日期")
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate hireDate;

    @ExcelProperty("薪資")
    @NumberFormat("#,##0.00")
    private BigDecimal salary;
}
```

## 基本匯出

```java
@Service
@RequiredArgsConstructor
public class EmployeeExportService {
    private final EmployeeDao employeeDao;

    public void export(HttpServletResponse response) throws IOException {
        List<EmployeeExportVO> data = employeeDao.selectForExport();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=employees.xlsx");

        EasyExcel.write(response.getOutputStream(), EmployeeExportVO.class)
            .sheet("員工列表")
            .doWrite(data);
    }
}
```

## 大檔案分頁匯出

```java
public void exportLargeFile(HttpServletResponse response) throws IOException {
    try (ExcelWriter writer = EasyExcel.write(response.getOutputStream(), EmployeeExportVO.class).build()) {
        WriteSheet sheet = EasyExcel.writerSheet("員工列表").build();

        int pageNum = 1;
        int pageSize = 10000;

        while (true) {
            List<EmployeeExportVO> data = employeeDao.selectPage(pageNum, pageSize);
            if (data.isEmpty()) break;

            writer.write(data, sheet);
            pageNum++;
        }
    }
}
```

## 異步匯出

```java
@Async
public CompletableFuture<String> exportAsync(ExportRequest request) {
    String fileName = "export_" + System.currentTimeMillis() + ".xlsx";
    Path filePath = Paths.get(tempDir, fileName);

    try (ExcelWriter writer = EasyExcel.write(filePath.toString(), EmployeeExportVO.class).build()) {
        // 分頁寫入...
    }

    return CompletableFuture.completedFuture(fileName);
}
```

## PDF 匯出 (iText)

```java
public void exportPdf(HttpServletResponse response) throws Exception {
    response.setContentType("application/pdf");
    response.setHeader("Content-Disposition", "attachment;filename=report.pdf");

    Document document = new Document();
    PdfWriter.getInstance(document, response.getOutputStream());
    document.open();

    // 添加標題
    document.add(new Paragraph("員工報表", chineseFont));

    // 添加表格
    PdfPTable table = new PdfPTable(4);
    table.addCell(new PdfPCell(new Phrase("姓名", chineseFont)));
    // ... 添加資料

    document.add(table);
    document.close();
}
```

## Controller API

```java
@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
public class ExportController {
    private final EmployeeExportService exportService;

    @GetMapping("/employees")
    @SaCheckPermission("employee:export")
    public void exportEmployees(HttpServletResponse response) throws IOException {
        exportService.export(response);
    }
}
```
