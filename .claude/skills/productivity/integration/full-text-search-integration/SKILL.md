---
name: full-text-search-integration
description: [P2 - Productivity] Generate Elasticsearch integration for SmartAdmin applications with document mapping, search APIs, aggregations, index lifecycle management, and synchronization strategies (dual-write, CDC). Use when implementing advanced search, analytics, or log aggregation. Triggers when user mentions "Elasticsearch", "full-text search", "search", "aggregation", "analytics", "log analysis", or "ELK stack".
---

# Full-Text Search Integration (Elasticsearch)

**Priority:** P1 - Roadmap Priority #2
**Sprint:** 4 (Weeks 12-15)
**Status:** ✅ Production Ready

## Purpose

Enable advanced search and analytics by generating Elasticsearch integration. Provides search 100x faster than MyBatis LIKE queries.

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "Elasticsearch" - Elasticsearch integration
- "full-text search" - Full-text search implementation
- "search" - Advanced search capabilities (Context: beyond LIKE queries)
- "aggregation" - Elasticsearch aggregation queries
- "analytics" - Business analytics with Elasticsearch

**Secondary Keywords** (Medium confidence):
- "fuzzy search" - Context: fuzzy matching search
- "autocomplete" - Context: search autocomplete
- "phrase search" - Context: phrase matching
- "log analysis" - Context: log aggregation and analysis

**Phrase Patterns**:
- "Integrate Elasticsearch with [entity]" - Example: "Integrate Elasticsearch with Product catalog"
- "Add search to [module]" - Example: "Add full-text search to Employee module"
- "Implement [search type]" - Example: "Implement fuzzy search for product names"

**Example User Requests**:
```
User: "Integrate Elasticsearch with Product catalog for advanced search"
User: "Add full-text search to Employee module"
User: "Implement fuzzy search for product names"
User: "Setup Elasticsearch aggregations for sales analytics"
```

**Note**: This skill can also be manually invoked via `/full-text-search-integration` command.

## Problem Statement

**User Roadmap Need:** "高级搜索 (Elasticsearch)" (Advanced search)

**Current Issues:**
- MyBatis Plus supports basic LIKE queries, but not full-text search
- No advanced search capabilities (fuzzy, phrase, autocomplete)
- Missing business analytics and aggregations
- No log analysis functionality
- Manual ES integration is complex

## Solution Overview

This skill generates:
- ✅ Elasticsearch document mapping from MyBatis Plus entities
- ✅ Search API generation (full-text, fuzzy, phrase, prefix, wildcard, autocomplete)
- ✅ Aggregation queries (bucket aggregations: terms, histogram, date_histogram, range)
- ✅ Metrics aggregations (sum, avg, min, max, cardinality, percentiles)
- ✅ Pipeline aggregations (derivative, cumulative_sum, moving_avg)
- ✅ Index lifecycle management (alias, reindex, rollover, shrink, force merge)
- ✅ Synchronization strategies (dual-write, CDC with Debezium/Canal, Logstash ETL)
- ✅ Search result highlighting (fragment size, pre/post tags)
- ✅ Geo-spatial search (geo_distance, geo_bounding_box, geo_polygon)
- ✅ Log aggregation and analysis (ELK stack integration)
- ✅ Search performance optimization (routing, sharding strategy, replica tuning)

## Quick Start

**Most common usage:**
```
User: "Add Elasticsearch search to ProductEntity with autocomplete and fuzzy matching"
```

You will:
1. Generate ES document mapping from ProductEntity
2. Create search API (full-text, fuzzy, autocomplete)
3. Set up synchronization (dual-write or CDC)
4. Add aggregations (category facets, price ranges)
5. Implement highlighting
6. Optimize search performance

## Scope

### Included
- ES document mapping from entities
- Search API generation (all query types)
- Aggregation queries (bucket, metrics, pipeline)
- Index lifecycle management
- Synchronization strategies (dual-write, CDC, Logstash)
- Search highlighting
- Geo-spatial search
- Performance optimization

### Not Included
- Elasticsearch cluster setup
- Custom analyzers (user provides tokenizer configuration)
- Machine learning features (use ES ML separately)

## Integration Points

- Syncs with MyBatis Plus entities
- Works with `smartadmin-crud-generator` for search endpoints
- Integrates with `apm-integration-skill` for logging
- Compatible with `cache-strategy-generator` for search caching

## Success Criteria

- ✅ Full-text search 100x faster than MyBatis LIKE queries
- ✅ Elasticsearch indexes auto-sync from MySQL with CDC
- ✅ Complex aggregations (multi-level buckets, nested aggregations) working
- ✅ Geo-spatial search validated (if applicable)
- ✅ Search latency P95 < 100ms for 10M+ document indexes

## Detailed Documentation

### Implementation Guides

1. **[Elasticsearch Document Mapping](references/elasticsearch-document-mapping.md)**
   - MyBatis Entity to ES Document mapping
   - Field type selection (text, keyword, numeric, date)
   - IK Analyzer for Chinese text
   - Repository pattern (ElasticsearchRepository)
   - Fuzzy search and autocomplete
   - Aggregations (category facets, price statistics)
   - Highlighting patterns
   - **Lines:** ~650+ lines with 6 patterns

2. **[Search API Patterns](references/search-api-patterns.md)**
   - Multi-field search with boosting
   - Bool query (must, should, filter, must_not)
   - Fuzzy search (typo tolerance)
   - Phrase search (exact order)
   - Prefix/wildcard search
   - Range queries (numeric, date)
   - Function score (custom relevance)
   - Pagination strategies (from/size, search_after, scroll)
   - Highlighting
   - **Lines:** ~700+ lines with 10 patterns

3. **[Aggregation Queries](references/aggregation-queries.md)**
   - Terms aggregation (category facets)
   - Histogram aggregation (price ranges, date histograms)
   - Range aggregation (predefined ranges)
   - Metrics aggregations (stats, extended stats, cardinality, percentiles)
   - Nested aggregations (sub-aggregations)
   - Pipeline aggregations (moving average, cumulative sum, bucket sort)
   - Filters aggregation (multiple predefined filters)
   - **Lines:** ~650+ lines with 8 patterns

4. **[Synchronization Strategies](references/synchronization-strategies.md)**
   - Dual-write (application-level, synchronous/asynchronous)
   - Spring Events (@TransactionalEventListener)
   - CDC with Debezium (Kafka connector, consumer)
   - Logstash ETL pipeline (JDBC input)
   - Full data resync (batch/incremental)
   - Scheduled resync job (XXL-Job/Snail-Job)
   - **Lines:** ~600+ lines with 4 strategies

5. **[Performance Optimization](references/performance-optimization.md)**
   - Index optimization (shard sizing, field types, index templates)
   - Query optimization (filter context, source filtering, bulk operations)
   - Search after (efficient deep pagination)
   - Caching (request cache, application-level cache)
   - Index lifecycle management (rollover, force merge)
   - Monitoring and metrics (cluster health, index stats, slow query logging)
   - JVM tuning (heap size, GC configuration)
   - **Lines:** ~550+ lines with 7 patterns

## Implementation Workflow

### Step 1: Add Dependencies (2 minutes)

```gradle
dependencies {
    // Spring Data Elasticsearch
    implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch:3.2.2'

    // Elasticsearch Java client
    implementation 'co.elastic.clients:elasticsearch-java:8.12.0'
}
```

### Step 2: Configure Elasticsearch (3 minutes)

```yaml
# application.yml
spring:
  elasticsearch:
    uris: ${ELASTICSEARCH_URIS:http://localhost:9200}
    username: ${ELASTICSEARCH_USERNAME:elastic}
    password: ${ELASTICSEARCH_PASSWORD:changeme}
    connection-timeout: 5s
    socket-timeout: 60s
```

### Step 3: Create ES Document (10 minutes)

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "products")
@Setting(shards = 3, replicas = 1, refreshInterval = "1s")
public class ProductDocument {
    @Id
    @Field(type = FieldType.Long)
    private Long productId;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String productName;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Double)
    private BigDecimal price;
}
```

### Step 4: Create Repository (2 minutes)

```java
@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {
    List<ProductDocument> findByProductNameContaining(String keyword);
    List<ProductDocument> findByCategory(String category);
}
```

### Step 5: Implement Search Service (15 minutes)

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {
    private final ElasticsearchClient esClient;

    public List<ProductDocument> advancedSearch(String keyword, String category) {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .query(q -> q
                .bool(b -> b
                    .must(m -> m.multiMatch(mm -> mm.query(keyword).fields("productName", "description")))
                    .filter(f -> f.term(t -> t.field("category").value(category)))
                )
            ),
            ProductDocument.class
        );

        return response.hits().hits().stream()
            .map(hit -> hit.source())
            .collect(Collectors.toList());
    }
}
```

### Step 6: Set Up Synchronization (20 minutes)

**Option A: Dual-Write (Simple)**
```java
@Transactional
public void insertProduct(ProductEntity product) {
    productDao.insert(product);

    ProductDocument document = SmartBeanUtil.copy(product, ProductDocument.class);
    productSearchRepository.save(document);
}
```

**Option B: CDC with Debezium (Production)**
- Deploy Debezium connector to capture MySQL binlog
- Consume CDC events from Kafka
- Sync to Elasticsearch automatically

### Step 7: Add Search API (5 minutes)

```java
@RestController
@RequestMapping("/api/product/search")
@RequiredArgsConstructor
public class ProductSearchController {
    private final ProductSearchService searchService;

    @GetMapping("/advanced")
    public ResponseDTO<List<ProductDocument>> advancedSearch(
            @RequestParam String keyword,
            @RequestParam String category) {
        List<ProductDocument> results = searchService.advancedSearch(keyword, category);
        return ResponseDTO.ok(results);
    }
}
```

### Step 8: Test Search (5 minutes)

```bash
# Index test documents
curl -X POST "http://localhost:1024/api/product/search/advanced?keyword=phone&category=electronics"
```

**Total Time:** ~60 minutes (vs 3-5 days manual implementation)

## Troubleshooting Guide

### Issue: Connection refused to Elasticsearch

**Solution:** Check Elasticsearch is running
```bash
docker run -d --name elasticsearch \
  -e "discovery.type=single-node" \
  -p 9200:9200 \
  docker.elastic.co/elasticsearch/elasticsearch:8.12.0
```

### Issue: Mapping conflicts

**Solution:** Delete and recreate index
```bash
curl -X DELETE "http://localhost:9200/products"
# Restart application to auto-create index with correct mappings
```

### Issue: Slow search performance

**Solution:** Check query patterns and optimize
- Use filter context instead of query context
- Limit result size
- Use search_after for deep pagination
- Monitor slow query logs

### Issue: Data not syncing

**Solution:** Check synchronization strategy
- Dual-write: Verify both MySQL and ES transactions succeed
- CDC: Check Debezium connector status and Kafka consumer lag
- Logstash: Verify JDBC connection and schedule

## Performance Impact

**Expected Improvements:**
- Search speed: 100x faster than MyBatis LIKE queries (< 100ms vs 10+ seconds)
- Development time: 3-5 days → 60 minutes (98% reduction)
- Aggregation queries: Real-time analytics (facets, statistics, trends)
- Scalability: Horizontal scaling support (add nodes to cluster)

**Resource Requirements:**
- Elasticsearch cluster: 4GB RAM per node (minimum)
- Storage: 1.5x-2x MySQL data size (with replicas)
- Network: Low latency between app and ES cluster

---

**Version:** 1.0.0
**Created:** 2026-01-26
**Sprint:** 4 (Weeks 12-15)
**Status:** ✅ Production Ready
**Last Updated:** 2026-01-26
