# Cron Expression Guide

**Skill:** scheduled-task-manager
**Component:** Cron Expression / Scheduling
**Purpose:** Master cron expressions for job scheduling in SmartAdmin

---

## Cron Expression Format

```
┌───────────── second (0-59)
│ ┌───────────── minute (0-59)
│ │ ┌───────────── hour (0-23)
│ │ │ ┌───────────── day of month (1-31)
│ │ │ │ ┌───────────── month (1-12 or JAN-DEC)
│ │ │ │ │ ┌───────────── day of week (0-6 or SUN-SAT, 0=Sunday)
│ │ │ │ │ │
│ │ │ │ │ │
* * * * * *
```

---

## Common Patterns

### Every Minute
```
0 * * * * ?     # Every minute at second 0
```

### Every Hour
```
0 0 * * * ?     # Every hour at minute 0
0 30 * * * ?    # Every hour at minute 30
```

### Daily
```
0 0 2 * * ?     # Every day at 2:00 AM
0 30 8 * * ?    # Every day at 8:30 AM
0 0 0 * * ?     # Every day at midnight
```

### Weekly
```
0 0 2 * * MON   # Every Monday at 2:00 AM
0 0 0 * * FRI   # Every Friday at midnight
0 0 10 * * 1-5  # Weekdays at 10:00 AM (Monday-Friday)
```

### Monthly
```
0 0 2 1 * ?     # 1st day of every month at 2:00 AM
0 0 2 15 * ?    # 15th day of every month at 2:00 AM
0 0 2 L * ?     # Last day of every month at 2:00 AM
```

### Yearly
```
0 0 0 1 1 ?     # January 1st at midnight every year
0 0 9 1 4 ?     # April 1st at 9:00 AM every year
```

---

## Special Characters

### Asterisk (*)
**Meaning:** All values
```
* * * * * ?     # Every second
0 * * * * ?     # Every minute
0 0 * * * ?     # Every hour
```

### Question Mark (?)
**Meaning:** No specific value (used for day-of-month or day-of-week)
```
0 0 2 15 * ?    # 15th of month (day-of-week doesn't matter)
0 0 2 ? * MON   # Monday (day-of-month doesn't matter)
```

### Hyphen (-)
**Meaning:** Range
```
0 0 9-17 * * ?  # Every hour from 9 AM to 5 PM
0 0 0 * * 1-5   # Midnight on weekdays (Mon-Fri)
```

### Comma (,)
**Meaning:** List of values
```
0 0 0 1,15 * ?  # 1st and 15th of month at midnight
0 0 9 * * MON,WED,FRI  # Mon, Wed, Fri at 9 AM
```

### Slash (/)
**Meaning:** Increment
```
0 */15 * * * ?  # Every 15 minutes
0 0 */2 * * ?   # Every 2 hours
0 0 0 */3 * ?   # Every 3 days
```

### L
**Meaning:** Last
```
0 0 2 L * ?     # Last day of month at 2 AM
0 0 2 ? * 5L    # Last Friday of month at 2 AM
```

### W
**Meaning:** Weekday (nearest weekday to given day)
```
0 0 2 15W * ?   # Nearest weekday to 15th at 2 AM
```

### Hash (#)
**Meaning:** Nth occurrence of day in month
```
0 0 2 ? * 2#1   # First Monday of month at 2 AM
0 0 2 ? * 5#3   # Third Thursday of month at 2 AM
```

---

## SmartAdmin Use Cases

### User Statistics Job
```java
/**
 * Calculate daily user statistics at 2 AM
 * Cron: 0 0 2 * * ?
 */
@JobExecutor(name = "userStatisticsJob")
public ExecuteResult execute(String param) {
    // Statistics logic
}
```

### Data Cleanup Job
```java
/**
 * Clean up expired data every Sunday at 3 AM
 * Cron: 0 0 3 ? * SUN
 */
@JobExecutor(name = "dataCleanupJob")
public ExecuteResult execute(String param) {
    // Cleanup logic
}
```

### Report Generation Job
```java
/**
 * Generate monthly report on 1st day at 1 AM
 * Cron: 0 0 1 1 * ?
 */
@JobExecutor(name = "monthlyReportJob")
public ExecuteResult execute(String param) {
    // Report generation
}
```

### Cache Warm-up Job
```java
/**
 * Warm up cache every 30 minutes during business hours
 * Cron: 0 0,30 9-17 * * ?
 */
@JobExecutor(name = "cacheWarmupJob")
public ExecuteResult execute(String param) {
    // Cache warm-up
}
```

### Notification Job
```java
/**
 * Send notifications every 5 minutes
 * Cron: 0 */5 * * * ?
 */
@JobExecutor(name = "notificationJob")
public ExecuteResult execute(String param) {
    // Notification logic
}
```

---

## Cron Expression Examples

### Business Hours
```
0 0 9-17 * * ?          # Every hour during business hours (9 AM - 5 PM)
0 */15 9-17 * * ?       # Every 15 minutes during business hours
0 0 9-17 * * 1-5        # Every hour on weekdays during business hours
```

### Off-Peak Hours
```
0 0 0-5,22-23 * * ?     # Every hour during off-peak (midnight-5 AM, 10 PM-11 PM)
0 0 2 * * ?             # 2 AM (typical off-peak time)
```

### Batch Processing
```
0 0 1 * * ?             # Daily batch at 1 AM
0 0 1 ? * SUN           # Weekly batch on Sunday at 1 AM
0 0 1 1 * ?             # Monthly batch on 1st day at 1 AM
```

### High Frequency
```
0 */1 * * * ?           # Every minute
0 */5 * * * ?           # Every 5 minutes
0 */10 * * * ?          # Every 10 minutes
```

### Low Frequency
```
0 0 0 */3 * ?           # Every 3 days at midnight
0 0 0 ? * SUN           # Weekly on Sunday
0 0 0 1 * ?             # Monthly on 1st day
0 0 0 1 1 ?             # Yearly on January 1st
```

---

## Cron Expression Builder

### Java Code

```java
package net.lab1024.sa.foundation.util;

import org.springframework.scheduling.support.CronExpression;

public class CronExpressionBuilder {

    /**
     * Validate cron expression
     */
    public static boolean isValid(String cron) {
        return CronExpression.isValidExpression(cron);
    }

    /**
     * Get human-readable description
     */
    public static String describe(String cron) {
        if ("0 0 2 * * ?".equals(cron)) {
            return "Every day at 2:00 AM";
        } else if ("0 */5 * * * ?".equals(cron)) {
            return "Every 5 minutes";
        } else if ("0 0 0 1 * ?".equals(cron)) {
            return "1st day of every month at midnight";
        }
        // Add more descriptions...
        return "Custom schedule: " + cron;
    }

    /**
     * Build daily cron at specific hour
     */
    public static String daily(int hour) {
        return String.format("0 0 %d * * ?", hour);
    }

    /**
     * Build hourly cron
     */
    public static String hourly() {
        return "0 0 * * * ?";
    }

    /**
     * Build every N minutes cron
     */
    public static String everyMinutes(int minutes) {
        return String.format("0 */%d * * * ?", minutes);
    }

    /**
     * Build weekly cron (Monday = 2, Sunday = 1)
     */
    public static String weekly(int dayOfWeek, int hour) {
        return String.format("0 0 %d ? * %d", hour, dayOfWeek);
    }

    /**
     * Build monthly cron (1-31)
     */
    public static String monthly(int dayOfMonth, int hour) {
        return String.format("0 0 %d %d * ?", hour, dayOfMonth);
    }
}
```

### Usage Example

```java
// Daily at 2 AM
String cron = CronExpressionBuilder.daily(2);  // "0 0 2 * * ?"

// Every 15 minutes
String cron = CronExpressionBuilder.everyMinutes(15);  // "0 */15 * * * ?"

// Weekly on Monday at 9 AM
String cron = CronExpressionBuilder.weekly(2, 9);  // "0 0 9 ? * 2"

// Validate
boolean valid = CronExpressionBuilder.isValid(cron);

// Get description
String description = CronExpressionBuilder.describe(cron);
```

---

## Online Tools

- [Cron Expression Generator](https://crontab.guru/)
- [Cron Expression Descriptor](https://cronexpressiondescriptor.azurewebsites.net/)
- [Spring Cron Expression Validator](https://spring.io/blog/2020/11/10/new-in-spring-5-3-improved-cron-expressions)

---

## Common Mistakes

### ❌ Wrong: Using 7 for Sunday
```
0 0 2 * * 7  # ❌ Invalid, use 0 or SUN
```
✅ Correct:
```
0 0 2 * * 0  # ✅ Sunday
0 0 2 * * SUN  # ✅ Sunday
```

### ❌ Wrong: Using both day-of-month and day-of-week
```
0 0 2 15 * MON  # ❌ Ambiguous
```
✅ Correct:
```
0 0 2 15 * ?    # ✅ 15th of month (use ? for day-of-week)
0 0 2 ? * MON   # ✅ Monday (use ? for day-of-month)
```

### ❌ Wrong: Invalid range
```
0 0 25 * * ?  # ❌ Hour 25 doesn't exist
0 0 0 32 * ?  # ❌ Day 32 doesn't exist
```
✅ Correct:
```
0 0 23 * * ?  # ✅ 11 PM (max hour is 23)
0 0 0 31 * ?  # ✅ 31st day (max day is 31)
```

---

## Testing Cron Expressions

```java
@Test
void testCronExpression() {
    String cron = "0 0 2 * * ?";

    // Validate
    assertTrue(CronExpression.isValidExpression(cron));

    // Parse
    CronExpression expression = CronExpression.parse(cron);

    // Get next execution time
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime next = expression.next(now);

    System.out.println("Next execution: " + next);
    // Output: Next execution: 2024-01-27T02:00:00
}
```

---

**Next:** [Job Monitoring Patterns](job-monitoring-patterns.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
