# Report Generator - Examples

## 範例 1: 員工列表 Excel 匯出

**匯出模型**:
```java
@Data
public class EmployeeExportVO {
    @ExcelProperty(value = "員工編號", index = 0)
    private Long employeeId;

    @ExcelProperty(value = "姓名", index = 1)
    @ColumnWidth(15)
    private String employeeName;

    @ExcelProperty(value = "部門", index = 2)
    @ColumnWidth(20)
    private String departmentName;

    @ExcelProperty(value = "職位", index = 3)
    private String position;

    @ExcelProperty(value = "入職日期", index = 4)
    @DateTimeFormat("yyyy-MM-dd")
    @ColumnWidth(15)
    private LocalDate hireDate;

    @ExcelProperty(value = "狀態", index = 5)
    private String statusText;
}
```

**匯出服務**:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeExportService {
    private final EmployeeDao employeeDao;

    public void exportByDepartment(HttpServletResponse response, EmployeeQueryForm form) throws IOException {
        // 設定響應頭
        String fileName = URLEncoder.encode("員工列表_" + LocalDate.now() + ".xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

        // 按部門分組匯出到不同 Sheet
        Map<String, List<EmployeeExportVO>> groupedData = employeeDao.selectForExport(form)
            .stream()
            .collect(Collectors.groupingBy(EmployeeExportVO::getDepartmentName));

        try (ExcelWriter writer = EasyExcel.write(response.getOutputStream(), EmployeeExportVO.class).build()) {
            for (Map.Entry<String, List<EmployeeExportVO>> entry : groupedData.entrySet()) {
                WriteSheet sheet = EasyExcel.writerSheet(entry.getKey()).build();
                writer.write(entry.getValue(), sheet);
            }
        }
    }
}
```

---

## 範例 2: 異步大檔案匯出

**異步任務服務**:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncExportService {
    private final EmployeeDao employeeDao;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${export.temp-dir:/tmp/exports}")
    private String tempDir;

    @Async
    public void asyncExport(Long userId, EmployeeQueryForm form) {
        String taskId = UUID.randomUUID().toString();
        String progressKey = "export:progress:" + taskId;

        try {
            // 初始化進度
            redisTemplate.opsForValue().set(progressKey, "0", 1, TimeUnit.HOURS);

            // 計算總數
            long total = employeeDao.count(form);
            int pageSize = 10000;
            int totalPages = (int) Math.ceil(total / (double) pageSize);

            String fileName = "employees_" + taskId + ".xlsx";
            Path filePath = Paths.get(tempDir, fileName);

            try (ExcelWriter writer = EasyExcel.write(filePath.toString(), EmployeeExportVO.class).build()) {
                WriteSheet sheet = EasyExcel.writerSheet("員工列表").build();

                for (int page = 1; page <= totalPages; page++) {
                    List<EmployeeExportVO> data = employeeDao.selectPage(form, page, pageSize);
                    writer.write(data, sheet);

                    // 更新進度
                    int progress = (int) (page * 100.0 / totalPages);
                    redisTemplate.opsForValue().set(progressKey, String.valueOf(progress));
                }
            }

            // 完成，保存文件路徑
            redisTemplate.opsForValue().set(progressKey, "100:" + fileName);
            log.info("Export completed: {}", fileName);

        } catch (Exception e) {
            log.error("Export failed", e);
            redisTemplate.opsForValue().set(progressKey, "error:" + e.getMessage());
        }
    }

    public ExportProgressVO getProgress(String taskId) {
        String value = redisTemplate.opsForValue().get("export:progress:" + taskId);
        if (value == null) return new ExportProgressVO(-1, null, "Not found");

        if (value.startsWith("100:")) {
            return new ExportProgressVO(100, value.substring(4), "Completed");
        } else if (value.startsWith("error:")) {
            return new ExportProgressVO(-1, null, value.substring(6));
        } else {
            return new ExportProgressVO(Integer.parseInt(value), null, "Processing");
        }
    }
}
```

---

## 範例 3: PDF 銷售報表

```java
@Service
@RequiredArgsConstructor
public class SalesReportPdfService {

    public void generateMonthlyReport(HttpServletResponse response, YearMonth month) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
            "attachment;filename=sales_report_" + month + ".pdf");

        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        // 標題
        Font titleFont = new Font(BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", false), 18, Font.BOLD);
        Paragraph title = new Paragraph(month + " 月銷售報表", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        // 統計摘要
        SalesSummaryVO summary = salesDao.selectMonthlySummary(month);
        document.add(new Paragraph("總銷售額: " + summary.getTotalAmount()));
        document.add(new Paragraph("訂單數: " + summary.getOrderCount()));

        // 詳細表格
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.addCell("日期");
        table.addCell("銷售額");
        table.addCell("訂單數");
        table.addCell("同比增長");

        List<DailySalesVO> dailyData = salesDao.selectDailySales(month);
        for (DailySalesVO day : dailyData) {
            table.addCell(day.getDate().toString());
            table.addCell(day.getAmount().toString());
            table.addCell(String.valueOf(day.getOrderCount()));
            table.addCell(day.getGrowthRate() + "%");
        }

        document.add(table);
        document.close();
    }
}
```
