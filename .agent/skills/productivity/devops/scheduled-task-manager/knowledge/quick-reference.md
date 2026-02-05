# Scheduled Task Manager - Quick Reference

## XXL-Job Handler 模板

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class UserStatisticsJobHandler {
    private final UserStatisticsService statisticsService;

    @XxlJob("userStatisticsJob")
    public void execute() {
        String param = XxlJobHelper.getJobParam();
        log.info("開始執行用戶統計任務, param={}", param);

        try {
            statisticsService.calculateDailyStatistics();
            XxlJobHelper.handleSuccess("統計完成");
        } catch (Exception e) {
            log.error("用戶統計任務失敗", e);
            XxlJobHelper.handleFail("統計失敗: " + e.getMessage());
        }
    }
}
```

## Cron 表達式速查

| 表達式 | 說明 |
|--------|------|
| `0 0 2 * * ?` | 每日 02:00 |
| `0 0 * * * ?` | 每小時整點 |
| `0 */5 * * * ?` | 每 5 分鐘 |
| `0 0 9-18 * * ?` | 每日 9-18 點整點 |
| `0 0 2 ? * MON` | 每週一 02:00 |
| `0 0 2 1 * ?` | 每月 1 日 02:00 |

## Cron 欄位說明

```
秒 分 時 日 月 週
0  0  2  *  *  ?
│  │  │  │  │  └─ 週 (0-7, SUN-SAT)
│  │  │  │  └──── 月 (1-12)
│  │  │  └─────── 日 (1-31)
│  │  └────────── 時 (0-23)
│  └───────────── 分 (0-59)
└──────────────── 秒 (0-59)
```

## 分片任務

```java
@XxlJob("shardingJob")
public void shardingExecute() {
    int shardIndex = XxlJobHelper.getShardIndex();
    int shardTotal = XxlJobHelper.getShardTotal();

    List<Long> userIds = userDao.findIdsByModulo(shardIndex, shardTotal);
    for (Long userId : userIds) {
        processUser(userId);
    }
}
```

## 重試策略

```java
@Retryable(
    value = {DataAccessException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 5000, multiplier = 2)
)
public void processWithRetry() {
    // 業務邏輯
}
```
