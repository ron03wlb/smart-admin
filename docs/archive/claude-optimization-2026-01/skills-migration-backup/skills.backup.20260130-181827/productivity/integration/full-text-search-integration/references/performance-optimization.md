# Elasticsearch Performance Optimization

**Skill:** full-text-search-integration
**Component:** Elasticsearch / Performance / Optimization
**Purpose:** Performance tuning and optimization for SmartAdmin Elasticsearch integration

---

## Performance Checklist

### Index Design
- ✅ Appropriate shard count (1-3 shards for < 10GB, calculate for larger)
- ✅ Correct field types (text vs keyword)
- ✅ Disable unused features (_source, norms, doc_values)
- ✅ Use routing for tenant-based applications

### Query Optimization
- ✅ Use filter context instead of query context when possible
- ✅ Avoid deep pagination (use search_after)
- ✅ Cache frequently used filters
- ✅ Limit result size and fields returned

### Hardware & Resources
- ✅ Adequate heap size (50% of RAM, max 32GB)
- ✅ SSD for faster I/O
- ✅ Sufficient file descriptors (65535+)
- ✅ Dedicated master nodes for large clusters

---

## Pattern 1: Index Optimization

### Shard Sizing

```java
/**
 * Calculate optimal shard count
 * Rule of thumb:
 * - Each shard: 20-50GB
 * - Max shards per node: 20-25
 * - Total shards = (estimated_data_size / target_shard_size)
 */
@Document(indexName = "products")
@Setting(
    shards = 3,           // 3 shards for 60-150GB data
    replicas = 1,         // 1 replica for high availability
    refreshInterval = "5s" // Refresh every 5s (trade-off: indexing speed vs search freshness)
)
public class ProductDocument {
    // ...
}
```

**Dynamic Shard Calculation:**

```java
public int calculateOptimalShardCount(long estimatedDataSizeGB) {
    long targetShardSizeGB = 30;  // 30GB per shard
    int shardCount = (int) Math.ceil((double) estimatedDataSizeGB / targetShardSizeGB);

    // Min: 1, Max: 10 (reasonable limits)
    return Math.max(1, Math.min(10, shardCount));
}
```

### Field Type Optimization

```java
@Document(indexName = "products_optimized")
public class OptimizedProductDocument {

    @Id
    @Field(type = FieldType.Long)
    private Long productId;

    // Text field: Full-text search (analyzed)
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String productName;

    // Keyword field: Exact match, aggregations, sorting (not analyzed)
    @Field(type = FieldType.Keyword)
    private String category;

    // Keyword with normalizer: Case-insensitive exact match
    @Field(type = FieldType.Keyword, normalizer = "lowercase")
    private String sku;

    // Numeric field: Range queries, aggregations
    @Field(type = FieldType.Double)
    private BigDecimal price;

    // Date field: Date range queries
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createdAt;

    // Disable _source for large text fields (save storage)
    // Note: Cannot retrieve original value, only use for search
    @Field(type = FieldType.Text, store = false)
    private String longDescription;

    // Disable norms for fields not used in scoring (save memory)
    @Field(type = FieldType.Keyword, norms = false)
    private String internalCode;
}
```

### Index Template

```java
/**
 * Create index template for time-series data
 * Example: product_2024-01, product_2024-02, etc.
 */
public void createIndexTemplate() throws IOException {
    PutIndexTemplateRequest request = PutIndexTemplateRequest.of(t -> t
        .name("products_template")
        .indexPatterns("products_*")
        .template(tp -> tp
            .settings(s -> s
                .numberOfShards("3")
                .numberOfReplicas("1")
                .refreshInterval(Time.of(time -> time.time("5s")))
            )
            .mappings(m -> m
                .properties("productId", p -> p.long_(l -> l))
                .properties("productName", p -> p.text(txt -> txt
                    .analyzer("ik_max_word")
                    .searchAnalyzer("ik_smart")
                ))
                .properties("category", p -> p.keyword(k -> k))
                .properties("price", p -> p.double_(d -> d))
                .properties("createdAt", p -> p.date(dt -> dt
                    .format("strict_date_hour_minute_second")
                ))
            )
        )
    );

    esClient.indices().putTemplate(request);
}
```

---

## Pattern 2: Query Optimization

### Use Filter Context (No Scoring)

```java
/**
 * Filter context queries are cached and faster
 * Use for: exact match, range queries, exists queries
 */
public List<ProductDocument> searchWithFilter(String keyword, String category, BigDecimal minPrice) {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .query(q -> q
                .bool(b -> b
                    // Query context: Contributes to relevance score
                    .must(m -> m
                        .match(ma -> ma
                            .field("productName")
                            .query(keyword)
                        )
                    )
                    // Filter context: No scoring, cached, faster
                    .filter(f -> f
                        .term(t -> t
                            .field("category")
                            .value(category)
                        )
                    )
                    .filter(f -> f
                        .range(r -> r
                            .field("price")
                            .gte(JsonData.of(minPrice))
                        )
                    )
                )
            ),
            ProductDocument.class
        );

        return response.hits().hits().stream()
            .map(hit -> hit.source())
            .collect(Collectors.toList());

    } catch (Exception e) {
        log.error("Search failed", e);
        return List.of();
    }
}
```

### Limit Fields Returned

```java
/**
 * Return only necessary fields to reduce network and serialization overhead
 */
public List<ProductDocument> searchWithSourceFiltering(String keyword) {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .query(q -> q
                .match(m -> m
                    .field("productName")
                    .query(keyword)
                )
            )
            .source(src -> src
                .filter(f -> f
                    .includes("productId", "productName", "price", "category")
                    .excludes("description", "longDescription")  // Exclude large fields
                )
            )
            .size(20),
            ProductDocument.class
        );

        return response.hits().hits().stream()
            .map(hit -> hit.source())
            .collect(Collectors.toList());

    } catch (Exception e) {
        log.error("Search failed", e);
        return List.of();
    }
}
```

### Bulk Operations

```java
/**
 * Use bulk API for batch operations (faster than individual requests)
 */
public void bulkInsert(List<ProductEntity> products) {
    List<BulkOperation> operations = products.stream()
        .map(entity -> {
            ProductDocument document = SmartBeanUtil.copy(entity, ProductDocument.class);
            return BulkOperation.of(b -> b
                .index(i -> i
                    .index("products")
                    .id(document.getProductId().toString())
                    .document(document)
                )
            );
        })
        .collect(Collectors.toList());

    try {
        BulkResponse response = esClient.bulk(r -> r
            .operations(operations)
            .refresh(Refresh.False)  // Don't refresh immediately (faster)
        );

        if (response.errors()) {
            log.error("Bulk insert had errors");
            response.items().forEach(item -> {
                if (item.error() != null) {
                    log.error("Failed to index document {}: {}",
                        item.id(), item.error().reason());
                }
            });
        } else {
            log.info("Bulk inserted {} products", products.size());
        }

    } catch (Exception e) {
        log.error("Bulk insert failed", e);
    }
}
```

---

## Pattern 3: Search After (Efficient Deep Pagination)

```java
/**
 * Search after is more efficient than from/size for deep pagination
 * Good for: Exporting large result sets, infinite scroll
 */
public List<ProductDocument> exportAllProducts() {
    List<ProductDocument> allProducts = new ArrayList<>();
    List<Object> searchAfter = null;

    while (true) {
        try {
            SearchResponse<ProductDocument> response = esClient.search(s -> {
                var builder = s
                    .index("products")
                    .query(q -> q.matchAll(m -> m))
                    .sort(so -> so.field(f -> f.field("productId").order(SortOrder.Asc)))
                    .size(1000)  // Fetch 1000 docs per batch
                    .source(src -> src.filter(f -> f
                        .includes("productId", "productName", "price")  // Only necessary fields
                    ));

                if (searchAfter != null) {
                    builder.searchAfter(searchAfter);
                }

                return builder;
            }, ProductDocument.class);

            List<ProductDocument> batch = response.hits().hits().stream()
                .map(hit -> hit.source())
                .collect(Collectors.toList());

            if (batch.isEmpty()) {
                break;  // No more results
            }

            allProducts.addAll(batch);

            // Get last hit's sort values for next page
            var hits = response.hits().hits();
            searchAfter = hits.get(hits.size() - 1).sort();

            log.info("Exported batch: {} products (total: {})", batch.size(), allProducts.size());

        } catch (Exception e) {
            log.error("Export failed at offset: {}", allProducts.size(), e);
            break;
        }
    }

    return allProducts;
}
```

---

## Pattern 4: Caching

### Request Cache

```java
/**
 * Request cache: Caches aggregation results and size=0 requests
 * Enabled by default for size=0 queries
 */
public Map<String, Long> getCategoryFacetsWithCache() {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .size(0)  // size=0 queries are cached by default
            .requestCache(true)  // Explicitly enable cache
            .aggregations("categories", a -> a
                .terms(t -> t.field("category").size(50))
            ),
            ProductDocument.class
        );

        Map<String, Long> facets = new LinkedHashMap<>();
        response.aggregations().get("categories").sterms().buckets().array()
            .forEach(bucket -> {
                facets.put(bucket.key().stringValue(), bucket.docCount());
            });

        return facets;

    } catch (Exception e) {
        log.error("Category facets failed", e);
        return Map.of();
    }
}
```

### Application-Level Cache (Caffeine)

```java
@Configuration
public class SearchCacheConfiguration {

    @Bean
    public Cache<String, List<ProductDocument>> productSearchCache() {
        return Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .recordStats()
            .build();
    }
}

@Service
@RequiredArgsConstructor
public class CachedProductSearchService {

    private final ProductSearchService searchService;
    private final Cache<String, List<ProductDocument>> searchCache;

    public List<ProductDocument> searchWithCache(String keyword) {
        return searchCache.get(keyword, key -> searchService.searchByName(key));
    }
}
```

---

## Pattern 5: Index Lifecycle Management

### Index Rollover

```java
/**
 * Rollover index when it reaches size/age threshold
 * Example: products-000001 → products-000002
 */
public void createRolloverPolicy() throws IOException {
    // Create index template with alias
    esClient.indices().putIndexTemplate(t -> t
        .name("products_template")
        .indexPatterns("products-*")
        .template(tp -> tp
            .settings(s -> s.numberOfShards("3").numberOfReplicas("1"))
            .aliases("products_current", a -> a.isWriteIndex(true))
        )
    );

    // Create initial index
    esClient.indices().create(c -> c
        .index("products-000001")
        .aliases("products_current", a -> a.isWriteIndex(true))
    );
}

public void rolloverIndex() throws IOException {
    RolloverResponse response = esClient.indices().rollover(r -> r
        .alias("products_current")
        .conditions(c -> c
            .maxSize("50gb")
            .maxAge(Time.of(t -> t.time("30d")))
            .maxDocs(10_000_000L)
        )
    );

    if (response.rolledOver()) {
        log.info("Index rolled over: {} → {}",
            response.oldIndex(), response.newIndex());
    }
}
```

### Force Merge (Optimize)

```java
/**
 * Force merge segments to reduce overhead
 * Run during off-peak hours for read-only or rarely updated indexes
 */
public void forceMergeIndex() throws IOException {
    ForcemergeResponse response = esClient.indices().forcemerge(f -> f
        .index("products")
        .maxNumSegments(1)  // Merge to single segment (optimal for read-only)
        .onlyExpungeDeletes(false)
        .flush(true)
    );

    log.info("Force merge completed: {} shards", response.shards().successful());
}
```

---

## Pattern 6: Monitoring and Metrics

### Cluster Health

```java
@Service
@RequiredArgsConstructor
public class ElasticsearchMonitoringService {

    private final ElasticsearchClient esClient;

    /**
     * Check cluster health
     */
    public String getClusterHealth() {
        try {
            HealthResponse health = esClient.cluster().health();
            return health.status().jsonValue();  // green, yellow, red
        } catch (Exception e) {
            log.error("Failed to get cluster health", e);
            return "UNKNOWN";
        }
    }

    /**
     * Get index statistics
     */
    public Map<String, Object> getIndexStats(String indexName) {
        try {
            IndicesStatsResponse stats = esClient.indices().stats(s -> s.index(indexName));

            Map<String, Object> result = new HashMap<>();
            var indexStats = stats.indices().get(indexName);

            result.put("documentCount", indexStats.primaries().docs().count());
            result.put("storeSizeBytes", indexStats.primaries().store().sizeInBytes());
            result.put("searchTotal", indexStats.primaries().search().queryTotal());
            result.put("searchTimeMs", indexStats.primaries().search().queryTimeInMillis());
            result.put("indexTotal", indexStats.primaries().indexing().indexTotal());
            result.put("indexTimeMs", indexStats.primaries().indexing().indexTimeInMillis());

            return result;

        } catch (Exception e) {
            log.error("Failed to get index stats", e);
            return Map.of();
        }
    }
}
```

### Slow Query Logging

```yaml
# elasticsearch.yml
index.search.slowlog.threshold.query.warn: 10s
index.search.slowlog.threshold.query.info: 5s
index.search.slowlog.threshold.query.debug: 2s
index.search.slowlog.threshold.fetch.warn: 1s
index.indexing.slowlog.threshold.index.warn: 10s
```

### Micrometer Metrics

```java
@Service
@RequiredArgsConstructor
public class MetricsProductSearchService {

    private final ElasticsearchClient esClient;
    private final MeterRegistry meterRegistry;

    public List<ProductDocument> searchWithMetrics(String keyword) {
        Timer.Sample sample = Timer.start(meterRegistry);

        try {
            SearchResponse<ProductDocument> response = esClient.search(s -> s
                .index("products")
                .query(q -> q.match(m -> m.field("productName").query(keyword))),
                ProductDocument.class
            );

            List<ProductDocument> results = response.hits().hits().stream()
                .map(hit -> hit.source())
                .collect(Collectors.toList());

            // Record success metrics
            sample.stop(Timer.builder("elasticsearch.search.duration")
                .tag("index", "products")
                .tag("status", "success")
                .register(meterRegistry));

            meterRegistry.counter("elasticsearch.search.count",
                "index", "products",
                "status", "success"
            ).increment();

            return results;

        } catch (Exception e) {
            // Record failure metrics
            sample.stop(Timer.builder("elasticsearch.search.duration")
                .tag("index", "products")
                .tag("status", "failure")
                .register(meterRegistry));

            meterRegistry.counter("elasticsearch.search.count",
                "index", "products",
                "status", "failure"
            ).increment();

            log.error("Search failed", e);
            return List.of();
        }
    }
}
```

---

## Pattern 7: JVM Tuning

### Heap Size Configuration

```bash
# elasticsearch.yml or jvm.options
-Xms4g  # Initial heap size
-Xmx4g  # Maximum heap size (same as Xms for stability)

# Rules:
# - Set Xms = Xmx (avoid heap resizing)
# - Use 50% of available RAM
# - Max 32GB (compressed pointers limit)
# - Leave 50% RAM for OS file cache
```

### GC Configuration

```bash
# Use G1GC (default in Elasticsearch 8.x)
-XX:+UseG1GC
-XX:G1ReservePercent=25
-XX:InitiatingHeapOccupancyPercent=30

# GC logging
-Xlog:gc*,gc+age=trace,safepoint:file=logs/gc.log:utctime,pid,tags:filecount=32,filesize=64m
```

---

## Best Practices Summary

### Index Design
1. **Shard sizing:** 20-50GB per shard
2. **Field types:** Use keyword for exact match/aggregations, text for full-text search
3. **Disable unused features:** _source, norms, doc_values when not needed
4. **Refresh interval:** Increase (e.g., 5s or 30s) for write-heavy workloads

### Query Optimization
1. **Filter context:** Use for non-scoring queries (faster, cacheable)
2. **Source filtering:** Return only necessary fields
3. **Pagination:** Use search_after for deep pagination
4. **Bulk operations:** Batch insert/update/delete requests

### Caching
1. **Request cache:** Automatically caches size=0 and aggregation results
2. **Field data cache:** Monitor usage, increase heap if needed
3. **Application cache:** Use Caffeine/Redis for frequently accessed data

### Hardware
1. **Heap:** 50% of RAM, max 32GB
2. **Storage:** Use SSD for I/O intensive workloads
3. **Network:** Low latency network for cluster communication
4. **File descriptors:** Increase to 65535+ (`ulimit -n`)

### Monitoring
1. **Cluster health:** Monitor status (green/yellow/red)
2. **Slow queries:** Enable slow query logging
3. **Metrics:** Export to Prometheus + Grafana
4. **Index stats:** Track document count, storage size, search/indexing time

---

**Version:** 1.0.0
**Last Updated:** 2026-01-26
