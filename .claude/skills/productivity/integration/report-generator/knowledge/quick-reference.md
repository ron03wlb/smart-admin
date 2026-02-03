# Report Generator - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Skill**: report-generator (P2 - Productivity/Integration)

---

## Command Quick Reference

| Command | Purpose | Duration |
|---------|---------|----------|
| Excel Export (POI) | Generate Excel reports | ~12 min |
| PDF Export (iText) | Generate PDF reports | ~15 min |
| CSV Export | Generate CSV files | ~5 min |
| Template-based Export | Use templates for reports | ~10 min |
| Streaming Export | Export large datasets | ~15 min |

---

## Export Technology Selection

### Option 1: Apache POI (Excel - Recommended)

**Use When**: Excel reports with formatting, charts, formulas

**Pros**:
- ✅ Rich formatting (colors, fonts, borders)
- ✅ Formula support
- ✅ Multiple sheets
- ✅ Charts and images

**Cons**:
- ⚠️ Memory intensive for large files (>100K rows)
- ⚠️ Slow for complex formatting

**Setup**:
```gradle
dependencies {
    implementation 'org.apache.poi:poi:5.2.5'
    implementation 'org.apache.poi:poi-ooxml:5.2.5'
}
```

**Basic Excel Export**:
```java
@Service
@RequiredArgsConstructor
public class EmployeeReportService {

    private final EmployeeDao employeeDao;

    /**
     * Export employee list to Excel
     */
    public byte[] exportToExcel(EmployeeQueryForm form) {
        List<EmployeeVO> employees = employeeDao.queryList(form);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Employees");

            // Create header row
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);

            String[] headers = {"ID", "Name", "Email", "Department", "Status", "Created At"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            int rowNum = 1;
            CellStyle dateStyle = createDateStyle(workbook);

            for (EmployeeVO emp : employees) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(emp.getId());
                row.createCell(1).setCellValue(emp.getName());
                row.createCell(2).setCellValue(emp.getEmail());
                row.createCell(3).setCellValue(emp.getDepartmentName());
                row.createCell(4).setCellValue(emp.getStatus().name());

                Cell dateCell = row.createCell(5);
                dateCell.setCellValue(emp.getCreatedAt());
                dateCell.setCellStyle(dateStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new BusinessException("Excel export failed", e);
        }
    }

    /**
     * Create header cell style
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        // Background color
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Font
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);

        // Border
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        // Alignment
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    /**
     * Create date cell style
     */
    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss"));
        return style;
    }
}
```

**Controller Endpoint**:
```java
@RestController
@RequestMapping("/api/employee")
@RequiredArgsConstructor
@Tag(name = "Employee Report API")
public class EmployeeReportController {

    private final EmployeeReportService reportService;

    /**
     * Export employee list to Excel
     */
    @PostMapping("/export/excel")
    @Operation(summary = "Export to Excel")
    public void exportToExcel(
        @RequestBody EmployeeQueryForm form,
        HttpServletResponse response
    ) throws IOException {
        byte[] excelBytes = reportService.exportToExcel(form);

        // Set response headers
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
            "attachment; filename=" + URLEncoder.encode("employees.xlsx", StandardCharsets.UTF_8));
        response.setContentLength(excelBytes.length);

        // Write to response
        response.getOutputStream().write(excelBytes);
        response.getOutputStream().flush();
    }
}
```

**Time to Implement**: 12-15 minutes

---

### Option 2: iText (PDF)

**Use When**: PDF reports with tables, images, headers/footers

**Pros**:
- ✅ Professional layout
- ✅ Headers and footers
- ✅ Images and watermarks
- ✅ Digital signatures

**Cons**:
- ⚠️ Complex API
- ⚠️ License restrictions (AGPL)

**Setup**:
```gradle
dependencies {
    implementation 'com.itextpdf:itext7-core:7.2.5'
}
```

**Basic PDF Export**:
```java
@Service
@RequiredArgsConstructor
public class EmployeeReportService {

    private final EmployeeDao employeeDao;

    /**
     * Export employee list to PDF
     */
    public byte[] exportToPDF(EmployeeQueryForm form) {
        List<EmployeeVO> employees = employeeDao.queryList(form);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);

            // Add title
            Paragraph title = new Paragraph("Employee Report")
                .setFont(PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD))
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
            document.add(title);

            // Add generation date
            Paragraph date = new Paragraph("Generated: " + LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .setFontSize(10)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(10);
            document.add(date);

            // Create table
            float[] columnWidths = {1, 3, 4, 3, 2, 3};
            Table table = new Table(UnitValue.createPercentArray(columnWidths))
                .useAllAvailableWidth();

            // Add header
            String[] headers = {"ID", "Name", "Email", "Department", "Status", "Created At"};
            for (String header : headers) {
                table.addHeaderCell(new Cell()
                    .add(new Paragraph(header))
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));
            }

            // Add data rows
            for (EmployeeVO emp : employees) {
                table.addCell(new Cell().add(new Paragraph(emp.getId().toString())));
                table.addCell(new Cell().add(new Paragraph(emp.getName())));
                table.addCell(new Cell().add(new Paragraph(emp.getEmail())));
                table.addCell(new Cell().add(new Paragraph(emp.getDepartmentName())));
                table.addCell(new Cell().add(new Paragraph(emp.getStatus().name())));
                table.addCell(new Cell().add(new Paragraph(
                    emp.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))));
            }

            document.add(table);

            // Add footer
            Paragraph footer = new Paragraph("Total: " + employees.size() + " employees")
                .setFontSize(10)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(20);
            document.add(footer);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            throw new BusinessException("PDF export failed", e);
        }
    }
}
```

**Time to Implement**: 15-20 minutes

---

### Option 3: CSV Export (Lightweight)

**Use When**: Simple data export, large datasets

**Pros**:
- ✅ Very fast
- ✅ Minimal memory usage
- ✅ No external dependencies

**Cons**:
- ⚠️ No formatting
- ⚠️ No formulas

**Implementation**:
```java
@Service
@RequiredArgsConstructor
public class EmployeeReportService {

    private final EmployeeDao employeeDao;

    /**
     * Export employee list to CSV
     */
    public byte[] exportToCSV(EmployeeQueryForm form) {
        List<EmployeeVO> employees = employeeDao.queryList(form);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {

            // BOM for Excel UTF-8 compatibility
            writer.write('\ufeff');

            // Write header
            writer.write("ID,Name,Email,Department,Status,Created At\n");

            // Write data
            for (EmployeeVO emp : employees) {
                writer.write(String.format("%d,%s,%s,%s,%s,%s\n",
                    emp.getId(),
                    escapeCsv(emp.getName()),
                    escapeCsv(emp.getEmail()),
                    escapeCsv(emp.getDepartmentName()),
                    emp.getStatus().name(),
                    emp.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                ));
            }

            writer.flush();
            return out.toByteArray();

        } catch (IOException e) {
            throw new BusinessException("CSV export failed", e);
        }
    }

    /**
     * Escape CSV special characters
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
```

**Time to Implement**: 5-8 minutes

---

## Advanced Patterns

### Pattern 1: Streaming Export (Large Datasets)

**Problem**: OutOfMemoryError when exporting >100K rows

**Solution**: Stream data to response without buffering

```java
@Service
@RequiredArgsConstructor
public class EmployeeReportService {

    private final EmployeeDao employeeDao;

    /**
     * Stream large Excel export (memory-efficient)
     */
    public void streamExcelExport(
        EmployeeQueryForm form,
        HttpServletResponse response
    ) throws IOException {
        // Set response headers
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=employees.xlsx");

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {  // Keep 100 rows in memory
            Sheet sheet = workbook.createSheet("Employees");

            // Create header
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Name", "Email", "Department", "Status", "Created At"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            // Stream data in pages
            int pageNum = 1;
            int pageSize = 1000;
            int rowNum = 1;

            while (true) {
                form.setPageNum(pageNum);
                form.setPageSize(pageSize);

                PageResult<EmployeeVO> page = employeeDao.queryPage(form);
                if (page.getList().isEmpty()) break;

                for (EmployeeVO emp : page.getList()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(emp.getId());
                    row.createCell(1).setCellValue(emp.getName());
                    row.createCell(2).setCellValue(emp.getEmail());
                    row.createCell(3).setCellValue(emp.getDepartmentName());
                    row.createCell(4).setCellValue(emp.getStatus().name());
                    row.createCell(5).setCellValue(emp.getCreatedAt().toString());
                }

                pageNum++;

                // Break if last page
                if (page.getList().size() < pageSize) break;
            }

            // Write directly to response
            workbook.write(response.getOutputStream());
            workbook.dispose();  // Clean up temp files
        }
    }
}
```

**Memory Usage**: ~10MB (vs 500MB+ for buffered approach)

**Time to Implement**: 15-20 minutes

---

### Pattern 2: Template-based Export

**Problem**: Complex report layouts hard to code

**Solution**: Use Excel templates with placeholders

```java
@Service
@RequiredArgsConstructor
public class EmployeeReportService {

    private final EmployeeDao employeeDao;

    /**
     * Generate report from template
     */
    public byte[] exportFromTemplate(EmployeeQueryForm form) throws IOException {
        // Load template from classpath
        InputStream template = getClass().getResourceAsStream("/templates/employee-report.xlsx");

        try (Workbook workbook = new XSSFWorkbook(template);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.getSheetAt(0);

            // Replace placeholders in header
            Row titleRow = sheet.getRow(0);
            Cell titleCell = titleRow.getCell(0);
            String title = titleCell.getStringCellValue();
            titleCell.setCellValue(title.replace("{{DATE}}", LocalDate.now().toString()));

            // Start data from row 3 (after header rows)
            int rowNum = 3;
            List<EmployeeVO> employees = employeeDao.queryList(form);

            for (EmployeeVO emp : employees) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(emp.getId());
                row.createCell(1).setCellValue(emp.getName());
                row.createCell(2).setCellValue(emp.getEmail());
                row.createCell(3).setCellValue(emp.getDepartmentName());
                row.createCell(4).setCellValue(emp.getStatus().name());
            }

            // Update total formula
            Row totalRow = sheet.getRow(1);
            Cell totalCell = totalRow.getCell(1);
            totalCell.setCellValue(employees.size());

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
```

**Template Structure** (`templates/employee-report.xlsx`):
```
Row 1: Employee Report - {{DATE}}
Row 2: Total: [Formula: =COUNTA(A4:A1000)]
Row 3: [Headers: ID, Name, Email, Department, Status]
Row 4+: [Data rows]
```

**Time to Implement**: 10-12 minutes

---

### Pattern 3: Multi-Sheet Export

**Use Case**: Export related data across multiple sheets

```java
@Service
@RequiredArgsConstructor
public class DepartmentReportService {

    private final DepartmentDao departmentDao;
    private final EmployeeDao employeeDao;

    /**
     * Export department summary with employee details
     */
    public byte[] exportDepartmentSummary() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Sheet 1: Department Summary
            Sheet summarySheet = workbook.createSheet("Department Summary");
            List<DepartmentVO> departments = departmentDao.selectAll();

            Row headerRow = summarySheet.createRow(0);
            headerRow.createCell(0).setCellValue("Department ID");
            headerRow.createCell(1).setCellValue("Department Name");
            headerRow.createCell(2).setCellValue("Employee Count");

            int rowNum = 1;
            for (DepartmentVO dept : departments) {
                Row row = summarySheet.createRow(rowNum++);
                row.createCell(0).setCellValue(dept.getId());
                row.createCell(1).setCellValue(dept.getName());

                // Count employees
                int empCount = employeeDao.countByDeptId(dept.getId());
                row.createCell(2).setCellValue(empCount);
            }

            // Sheet 2+: Employee details per department
            for (DepartmentVO dept : departments) {
                Sheet deptSheet = workbook.createSheet(dept.getName());

                Row deptHeaderRow = deptSheet.createRow(0);
                deptHeaderRow.createCell(0).setCellValue("ID");
                deptHeaderRow.createCell(1).setCellValue("Name");
                deptHeaderRow.createCell(2).setCellValue("Email");
                deptHeaderRow.createCell(3).setCellValue("Status");

                List<EmployeeVO> employees = employeeDao.selectByDeptId(dept.getId());
                int deptRowNum = 1;

                for (EmployeeVO emp : employees) {
                    Row row = deptSheet.createRow(deptRowNum++);
                    row.createCell(0).setCellValue(emp.getId());
                    row.createCell(1).setCellValue(emp.getName());
                    row.createCell(2).setCellValue(emp.getEmail());
                    row.createCell(3).setCellValue(emp.getStatus().name());
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
```

**Time to Implement**: 20-25 minutes

---

### Pattern 4: Excel with Charts

**Use Case**: Visual reports with charts

```java
@Service
@RequiredArgsConstructor
public class EmployeeReportService {

    /**
     * Export department employee count with chart
     */
    public byte[] exportWithChart() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("Department Stats");

            // Create data
            String[] departments = {"IT", "HR", "Sales", "Marketing"};
            int[] counts = {45, 12, 28, 15};

            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Department");
            headerRow.createCell(1).setCellValue("Employee Count");

            for (int i = 0; i < departments.length; i++) {
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue(departments[i]);
                row.createCell(1).setCellValue(counts[i]);
            }

            // Create chart
            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 4, 1, 14, 20);

            XSSFChart chart = drawing.createChart(anchor);
            chart.setTitleText("Employees by Department");

            // Chart data
            XDDFDataSource<String> categories = XDDFDataSourcesFactory.fromStringCellRange(
                sheet, new CellRangeAddress(1, departments.length, 0, 0));

            XDDFNumericalDataSource<Integer> values = XDDFDataSourcesFactory.fromNumericCellRange(
                sheet, new CellRangeAddress(1, departments.length, 1, 1));

            // Create bar chart
            XDDFChartData data = chart.createData(ChartTypes.BAR, null, null);
            data.addSeries(categories, values);
            chart.plot(data);

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
```

**Time to Implement**: 15-20 minutes

---

## Common Errors and Quick Fixes

### Error 1: OutOfMemoryError

**Symptom**: `java.lang.OutOfMemoryError: Java heap space`

**Cause**: Loading entire dataset into memory

**Fix**: Use streaming export with `SXSSFWorkbook`
```java
// ❌ BAD - Loads all rows
Workbook workbook = new XSSFWorkbook();

// ✅ GOOD - Streams with window size
SXSSFWorkbook workbook = new SXSSFWorkbook(100);
```

---

### Error 2: Excel File Corrupted

**Symptom**: "Excel cannot open the file"

**Cause**: Not closing workbook properly

**Fix**: Use try-with-resources
```java
try (Workbook workbook = new XSSFWorkbook();
     ByteArrayOutputStream out = new ByteArrayOutputStream()) {
    // ...
    workbook.write(out);
}  // Auto-close
```

---

### Error 3: Chinese Characters Garbled in CSV

**Symptom**: CSV shows 乱码 when opened in Excel

**Cause**: Missing BOM for UTF-8

**Fix**: Add BOM at start
```java
writer.write('\ufeff');  // UTF-8 BOM
```

---

## Time Estimates

| Task | Implementation | Testing | Total |
|------|---------------|---------|-------|
| Excel Basic Export | 10 min | 2 min | 12 min |
| PDF Export | 12 min | 3 min | 15 min |
| CSV Export | 4 min | 1 min | 5 min |
| Streaming Export | 12 min | 3 min | 15 min |
| Template-based Export | 8 min | 2 min | 10 min |
| Multi-sheet Export | 18 min | 5 min | 23 min |
| Excel with Charts | 15 min | 5 min | 20 min |

---

**See Also**:
- [SmartAdmin Patterns](../../../../shared/knowledge/smartadmin-patterns.md) - ResponseDTO pattern
- [CI/CD Pipeline Builder](../../devops/cicd-pipeline-builder/) - Deploy report services
