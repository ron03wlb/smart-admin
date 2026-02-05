# Scheduled Task Manager - Examples

## 範例 1: 每日報表生成任務

**需求**: 每日凌晨 3 點生成銷售報表

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyReportJobHandler {
    private final ReportService reportService;
    private final EmailService emailService;

    @XxlJob("dailySalesReportJob")
    public void execute() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("生成 {} 銷售報表", yesterday);

        try {
            // 生成報表
            ReportResult result = reportService.generateDailySalesReport(yesterday);

            // 發送郵件
            emailService.sendReportEmail(result);

            XxlJobHelper.handleSuccess("報表生成完成, 銷售額=" + result.getTotalAmount());
        } catch (Exception e) {
            log.error("報表生成失敗", e);
            XxlJobHelper.handleFail("失敗: " + e.getMessage());
        }
    }
}
```

**Cron**: `0 0 3 * * ?` (每日 03:00)

---

## 範例 2: 過期資料清理任務

**需求**: 每小時清理過期的驗證碼

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiredDataCleanupJobHandler {
    private final VerificationCodeDao verificationCodeDao;

    @XxlJob("expiredDataCleanupJob")
    public void execute() {
        log.info("開始清理過期驗證碼");

        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(10);
        int deletedCount = verificationCodeDao.deleteExpired(expireTime);

        log.info("清理完成, 刪除 {} 條記錄", deletedCount);
        XxlJobHelper.handleSuccess("已刪除 " + deletedCount + " 條過期記錄");
    }
}
```

**Cron**: `0 0 * * * ?` (每小時整點)

---

## 範例 3: 分片處理大量資料

**需求**: 每日同步 100 萬用戶資料 (分片執行)

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class UserSyncShardingJobHandler {
    private final UserDao userDao;
    private final UserSyncService syncService;

    @XxlJob("userSyncShardingJob")
    public void execute() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        log.info("分片任務執行: {}/{}", shardIndex, shardTotal);

        // 按分片取數據
        List<UserEntity> users = userDao.findBySharding(shardIndex, shardTotal);
        log.info("本分片處理 {} 條用戶", users.size());

        int successCount = 0;
        for (UserEntity user : users) {
            try {
                syncService.syncUser(user);
                successCount++;
            } catch (Exception e) {
                log.error("用戶同步失敗: userId={}", user.getUserId(), e);
            }
        }

        XxlJobHelper.handleSuccess("分片 " + shardIndex + " 完成, 成功 " + successCount + "/" + users.size());
    }
}
```

**配置**: 啟動 4 個執行器實例, 每個實例處理 1/4 的資料

---

## 範例 4: Snail-Job 配置

```yaml
# application.yml
snail-job:
  enabled: true
  host: 127.0.0.1
  port: 1788
  namespace: smartadmin-prod
  group: smartadmin-group
```

```java
@Component
@JobExecutor(name = "dailyReportJob")
public class DailyReportSnailJobExecutor implements JobHandler {

    @Override
    public ExecuteResult execute(JobArgs args) {
        String param = args.getTaskParams();
        // 業務邏輯
        return ExecuteResult.success("完成");
    }
}
```
