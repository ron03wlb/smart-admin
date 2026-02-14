# JVM Memory and GC Tuning Guide

## JVM Memory Structure

```
Heap Memory:
├── Young Generation (Eden + Survivor)
│   ├── Eden Space (new objects)
│   └── Survivor Space (survived minor GC)
└── Old Generation (long-lived objects)

Non-Heap Memory:
├── Metaspace (class metadata)
├── Code Cache (JIT compiled code)
└── Thread Stacks
```

## Common OutOfMemoryError Types

### 1. Java heap space

**Symptom:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Causes:**
- Large result sets loaded into memory
- Memory leaks (objects not garbage collected)
- Heap size too small for workload

**Diagnosis:**
```bash
# Generate heap dump when OOM occurs
java -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/app/heapdump.hprof \
     -jar app.jar

# Analyze with Eclipse MAT or VisualVM
visualvm --openfile heapdump.hprof
```

**Fixes:**
```bash
# Increase heap size
java -Xms4g -Xmx4g -jar app.jar

# Or optimize code to use less memory
```

### 2. GC overhead limit exceeded

**Symptom:**
```
java.lang.OutOfMemoryError: GC overhead limit exceeded
```

**Causes:**
- Application spends > 98% time in GC
- Heap size too small
- Memory leak causing constant GC

**Fixes:**
```bash
# Increase heap size
java -Xms4g -Xmx4g -XX:+UseG1GC -jar app.jar

# Or disable GC overhead limit (temporary)
java -XX:-UseGCOverheadLimit -jar app.jar
```

### 3. Metaspace

**Symptom:**
```
java.lang.OutOfMemoryError: Metaspace
```

**Causes:**
- Too many classes loaded
- Classloader leaks (hot reload issues)

**Fixes:**
```bash
# Increase metaspace
java -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m -jar app.jar
```

## Garbage Collection Algorithms

### G1 GC (Recommended for SmartAdmin)

**When to use:**
- Heap size: 4GB - 32GB
- Target pause times: 200ms or less
- Production applications

**Configuration:**
```bash
java -Xms4g -Xmx4g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:G1HeapRegionSize=16m \
     -XX:InitiatingHeapOccupancyPercent=45 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/app/heapdump.hprof \
     -jar app.jar
```

**Tuning parameters:**
- `-XX:MaxGCPauseMillis=200` - Target pause time (not guaranteed)
- `-XX:G1HeapRegionSize=16m` - Region size (1-32 MB, power of 2)
- `-XX:InitiatingHeapOccupancyPercent=45` - Trigger concurrent GC at 45% heap usage

### ZGC (For ultra-low latency)

**When to use:**
- Heap size: > 8GB
- Target pause times: < 10ms
- Latency-sensitive applications

**Configuration:**
```bash
java -Xms8g -Xmx8g \
     -XX:+UseZGC \
     -XX:+ZGenerational \
     -jar app.jar
```

## JVM Heap Sizing Guidelines

### Formula for Heap Size

```
Recommended Heap = 50-75% of available RAM

Example for 8GB server:
-Xms4g -Xmx4g (50% of 8GB)
```

### Young Generation Sizing

```bash
# Let JVM auto-tune (recommended)
java -Xms4g -Xmx4g -XX:+UseG1GC -jar app.jar

# Or manually set (not recommended with G1GC)
java -Xms4g -Xmx4g -Xmn1g -XX:+UseG1GC -jar app.jar
```

## GC Logging and Monitoring

### Enable GC Logging (Java 21)

```bash
java -Xlog:gc*:file=/var/log/app/gc.log:time,uptime,level,tags \
     -Xms4g -Xmx4g \
     -XX:+UseG1GC \
     -jar app.jar
```

### Analyze GC Logs

```bash
# View GC log
tail -f /var/log/app/gc.log

# Look for:
# - Frequent Full GCs (indicates memory pressure)
# - Long GC pause times (> 200ms)
# - Heap occupancy before/after GC
```

**Example GC log entry:**
```
[2024-01-15T10:30:45.123+0000][0.234s][info][gc] GC(12) Pause Young (Normal) (G1 Evacuation Pause) 512M->128M(4096M) 15.234ms
```

**Interpretation:**
- Pause Type: Young generation collection
- Heap before: 512M
- Heap after: 128M
- Total heap: 4096M
- Pause time: 15.234ms ✅ (< 200ms target)

## Common Memory Issues and Fixes

### Issue 1: Frequent Full GCs

**Symptom:**
```
[gc] GC(45) Pause Full (Allocation Failure) 3800M->3700M(4096M) 500.123ms
```

**Cause:** Heap size too small, objects promoted to Old Gen too quickly

**Fix:**
```bash
# Increase heap size
java -Xms8g -Xmx8g -XX:+UseG1GC -jar app.jar
```

### Issue 2: Long GC Pauses

**Symptom:** GC pauses > 500ms

**Cause:** Large heap or inefficient GC algorithm

**Fix:**
```bash
# Switch to G1GC or ZGC
java -Xms4g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -jar app.jar
```

### Issue 3: Memory Leak

**Symptom:** Heap usage grows over time, never drops

**Diagnosis:**
```bash
# Generate heap dump
jmap -dump:live,format=b,file=heap.bin <pid>

# Analyze with Eclipse MAT
# Look for:
# - Dominator tree (largest objects)
# - Leak suspects report
```

**Common leak sources:**
- Static collections (never cleared)
- ThreadLocal not removed
- Event listeners not unregistered
- Unclosed resources (connections, streams)

**Fix example:**
```java
// BAD: Static collection grows forever
public class UserCache {
    private static Map<Long, User> cache = new HashMap<>();
    
    public void addUser(User user) {
        cache.put(user.getId(), user); // Never removed
    }
}

// GOOD: Use cache with eviction
@Configuration
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofMinutes(30)) // Auto-evict after 30 min
            ).build();
    }
}
```

## Production JVM Flags (SmartAdmin Recommended)

### For 8GB Server

```bash
java -server \
     -Xms4g -Xmx4g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/app/heapdump.hprof \
     -XX:+ExitOnOutOfMemoryError \
     -Xlog:gc*:file=/var/log/app/gc.log:time,uptime,level,tags \
     -jar smart-admin.jar
```

### For 16GB Server

```bash
java -server \
     -Xms8g -Xmx8g \
     -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=/var/log/app/heapdump.hprof \
     -XX:+ExitOnOutOfMemoryError \
     -Xlog:gc*:file=/var/log/app/gc.log:time,uptime,level,tags \
     -jar smart-admin.jar
```

## Monitoring with VisualVM

```bash
# Start VisualVM
jvisualvm

# Connect to running JVM
# Tools → Plugins → Install "Visual GC"

# Monitor:
# - Heap usage over time
# - GC activity
# - Thread count
# - CPU usage
```

## JFR (Java Flight Recorder) Profiling

```bash
# Start JFR recording
java -XX:StartFlightRecording=duration=60s,filename=recording.jfr \
     -jar app.jar

# Analyze with JDK Mission Control
jmc recording.jfr

# Look for:
# - Object allocation hotspots
# - GC events
# - Lock contention
```
