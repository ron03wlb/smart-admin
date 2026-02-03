# CSV Streaming Export Patterns

**Skill:** report-generator
**Component:** OpenCSV / Apache Commons CSV
**Purpose:** Stream large datasets to CSV without memory issues

---

## Why CSV Streaming?

- ✅ Handles millions of rows
- ✅ Low memory footprint
- ✅ Fast export speed
- ✅ Universal compatibility

---

## Pattern 1: OpenCSV Streaming

```java
import com.opencsv.CSVWriter;

@Service
@RequiredArgsConstructor
public class CsvExportService {

    private final UserManager userManager;

    public void exportToStream(UserQueryForm form, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=users.csv");

        try (CSVWriter writer = new CSVWriter(response.getWriter())) {
            // Write header
            writer.writeNext(new String[]{"User ID", "Username", "Email", "Phone", "Status"});

            // Stream data in batches
            int pageNum = 1;
            int pageSize = 1000;

            while (true) {
                List<UserEntity> users = userManager.listByPage(pageNum, pageSize);
                if (users.isEmpty()) break;

                for (UserEntity user : users) {
                    writer.writeNext(new String[]{
                        String.valueOf(user.getUserId()),
                        user.getUsername(),
                        user.getEmail(),
                        user.getPhone(),
                        user.getStatus().toString()
                    });
                }

                if (users.size() < pageSize) break;
                pageNum++;
            }
        }
    }
}
```

---

## Pattern 2: Custom CSV Formatter

```java
public void exportCustomFormat(List<UserExportVO> users, Writer writer) throws IOException {
    // Write UTF-8 BOM for Excel compatibility
    writer.write('\uFEFF');

    // Write header
    writer.write("User ID,Username,Email,Phone,Status\n");

    // Write data
    for (UserExportVO user : users) {
        writer.write(String.format("%d,\"%s\",\"%s\",\"%s\",\"%s\"\n",
            user.getUserId(),
            escapeCSV(user.getUsername()),
            escapeCSV(user.getEmail()),
            escapeCSV(user.getPhone()),
            user.getStatusName()
        ));
    }
}

private String escapeCSV(String value) {
    if (value == null) return "";
    return value.replace("\"", "\"\"");  // Escape quotes
}
```

---

## SmartAdmin Controller Example

```java
@RestController
@RequestMapping("/api/user/export")
@RequiredArgsConstructor
public class UserCsvExportController {

    private final CsvExportService csvExportService;

    @PostMapping("/csv")
    public void exportCsv(@RequestBody UserQueryForm form, HttpServletResponse response) {
        try {
            csvExportService.exportToStream(form, response);
        } catch (IOException e) {
            throw new BusinessException("CSV export failed");
        }
    }
}
```

---

**Version:** 1.0.0
**Last Updated:** 2026-01-26
