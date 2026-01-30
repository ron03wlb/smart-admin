# Scheduled Reports Guide

**Skill:** report-generator-skill
**Component:** XXL-Job / Spring @Scheduled
**Purpose:** Automate periodic report generation and delivery

---

## Pattern 1: Daily Reports with XXL-Job

```java
import com.xxl.job.core.handler.annotation.XxlJob;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyReportJob {

    private final UserExportService exportService;
    private final EmailService emailService;

    @XxlJob("dailyUserReport")
    public void generateDailyReport() {
        log.info("Starting daily user report generation");

        try {
            // Generate report for yesterday
            LocalDate yesterday = LocalDate.now().minusDays(1);
            UserQueryForm form = new UserQueryForm();
            form.setStartDate(yesterday.atStartOfDay());
            form.setEndDate(yesterday.plusDays(1).atStartOfDay());

            // Export to Excel
            String filePath = exportService.exportUsers(form);

            // Send via email
            emailService.sendWithAttachment(
                "admin@example.com",
                "Daily User Report - " + yesterday,
                "Please find attached the daily user report.",
                filePath
            );

            log.info("Daily report sent successfully");

        } catch (Exception e) {
            log.error("Daily report generation failed", e);
            throw new RuntimeException(e);
        }
    }
}
```

---

## Pattern 2: Weekly Summary with @Scheduled

```java
@Component
@RequiredArgsConstructor
public class WeeklySummaryJob {

    private final OrderExportService exportService;

    @Scheduled(cron = "0 0 9 * * MON")  // Every Monday 9 AM
    public void generateWeeklySummary() {
        LocalDate lastMonday = LocalDate.now().minusWeeks(1);
        LocalDate lastSunday = lastMonday.plusDays(6);

        OrderQueryForm form = new OrderQueryForm();
        form.setStartDate(lastMonday.atStartOfDay());
        form.setEndDate(lastSunday.plusDays(1).atStartOfDay());

        String filePath = exportService.exportOrders(form);

        // Upload to OSS or send email
        fileService.upload(filePath);
    }
}
```

---

## Pattern 3: On-Demand Reports with Cache

```java
@Service
@RequiredArgsConstructor
public class CachedReportService {

    private final RedisTemplate<String, String> redisTemplate;

    public ResponseDTO<String> getMonthlyReport(YearMonth month) {
        String cacheKey = "report:monthly:" + month;

        // Check cache
        String cachedUrl = redisTemplate.opsForValue().get(cacheKey);
        if (cachedUrl != null) {
            return ResponseDTO.ok(cachedUrl);
        }

        // Generate report
        String filePath = generateMonthlyReport(month);
        String downloadUrl = fileService.upload(filePath);

        // Cache for 24 hours
        redisTemplate.opsForValue().set(cacheKey, downloadUrl, Duration.ofHours(24));

        return ResponseDTO.ok(downloadUrl);
    }
}
```

---

**Version:** 1.0.0
**Last Updated:** 2026-01-26
