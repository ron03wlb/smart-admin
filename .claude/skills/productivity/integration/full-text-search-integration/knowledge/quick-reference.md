# Full-Text Search Integration - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Skill**: full-text-search-integration (P2 - Productivity/Integration)

---

## Command Quick Reference

| Command | Purpose | Duration |
|---------|---------|----------|
| Elasticsearch Setup | Setup Elasticsearch client | ~10 min |
| Index Creation | Create search index | ~5 min |
| Document Indexing | Index documents | ~8 min |
| Search Query | Implement search API | ~10 min |
| Aggregation | Statistics and analytics | ~12 min |

---

## Technology Selection

### Elasticsearch (Recommended)

**Use When**: Full-text search, analytics, log aggregation

**Pros**:
- ✅ Powerful full-text search
- ✅ Real-time indexing
- ✅ Scalable horizontally
- ✅ Rich query DSL

**Cons**:
- ⚠️ Memory intensive
- ⚠️ Complex cluster management

**Setup**:
```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch:3.5.4'
}
```

```yaml
spring:
  elasticsearch:
    uris: http://localhost:9200
    username: elastic
    password: changeme
    connection-timeout: 5s
    socket-timeout: 60s
```

**Time to Setup**: 10-12 minutes

---

## Pattern 1: Document Indexing

### Index Mapping Definition

```java
@Document(indexName = "employees")
@Setting(settingPath = "es-settings.json")
@RequiredArgsConstructor
public class EmployeeDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String name;

    @Field(type = FieldType.Keyword)
    private String email;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String departmentName;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Keyword)
    private List<String> skills;  // Multi-value field
}
```

**Index Settings** (`resources/es-settings.json`):
```json
{
  "analysis": {
    "analyzer": {
      "ik_max_word": {
        "type": "ik_max_word"
      },
      "ik_smart": {
        "type": "ik_smart"
      }
    }
  }
}
```

---

### Sync Data to Elasticsearch

```java
@Service
@RequiredArgsConstructor
public class EmployeeSearchService {

    private final EmployeeDao employeeDao;
    private final ElasticsearchRestTemplate elasticsearchTemplate;

    /**
     * Full sync - rebuild index
     */
    @Transactional(readOnly = true)
    public void fullSync() {
        // Delete and recreate index
        IndexOperations indexOps = elasticsearchTemplate.indexOps(EmployeeDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexOps.create();
        indexOps.putMapping(indexOps.createMapping());

        // Sync all employees
        int pageNum = 1;
        int pageSize = 1000;

        while (true) {
            EmployeeQueryForm form = new EmployeeQueryForm();
            form.setPageNum(pageNum);
            form.setPageSize(pageSize);

            PageResult<EmployeeEntity> page = employeeDao.queryPage(form);
            if (page.getList().isEmpty()) break;

            // Convert to documents
            List<EmployeeDocument> documents = page.getList().stream()
                .map(this::toDocument)
                .toList();

            // Bulk index
            elasticsearchTemplate.save(documents);

            log.info("Synced page {}: {} employees", pageNum, documents.size());

            pageNum++;
            if (page.getList().size() < pageSize) break;
        }

        log.info("Full sync completed");
    }

    /**
     * Incremental sync - single employee
     */
    public void syncEmployee(Long employeeId) {
        EmployeeEntity entity = employeeDao.selectById(employeeId);
        if (entity == null) {
            // Delete from ES if not found in DB
            elasticsearchTemplate.delete(employeeId.toString(), EmployeeDocument.class);
            return;
        }

        EmployeeDocument document = toDocument(entity);
        elasticsearchTemplate.save(document);
    }

    /**
     * Convert entity to document
     */
    private EmployeeDocument toDocument(EmployeeEntity entity) {
        return EmployeeDocument.builder()
            .id(entity.getId())
            .name(entity.getName())
            .email(entity.getEmail())
            .departmentName(entity.getDepartmentName())
            .status(entity.getStatus().name())
            .createdAt(entity.getCreatedAt())
            .skills(parseSkills(entity.getSkills()))
            .build();
    }
}
```

**Trigger Incremental Sync**:
```java
@Component
@RequiredArgsConstructor
public class EmployeeEventListener {

    private final EmployeeSearchService searchService;

    @EventListener
    public void onEmployeeCreated(EmployeeCreatedEvent event) {
        searchService.syncEmployee(event.getEmployeeId());
    }

    @EventListener
    public void onEmployeeUpdated(EmployeeUpdatedEvent event) {
        searchService.syncEmployee(event.getEmployeeId());
    }

    @EventListener
    public void onEmployeeDeleted(EmployeeDeletedEvent event) {
        searchService.syncEmployee(event.getEmployeeId());  // Will delete from ES
    }
}
```

**Time to Implement**: 15-20 minutes

---

## Pattern 2: Full-Text Search

### Basic Search

```java
@Service
@RequiredArgsConstructor
public class EmployeeSearchService {

    private final ElasticsearchRestTemplate elasticsearchTemplate;

    /**
     * Simple keyword search
     */
    public PageResult<EmployeeDocument> search(String keyword, int pageNum, int pageSize) {
        // Build query
        Criteria criteria = new Criteria("name").contains(keyword)
            .or("departmentName").contains(keyword)
            .or("email").contains(keyword);

        Query query = new CriteriaQuery(criteria)
            .setPageable(PageRequest.of(pageNum - 1, pageSize))
            .addSort(Sort.by(Sort.Direction.DESC, "createdAt"));

        // Execute search
        SearchHits<EmployeeDocument> hits = elasticsearchTemplate.search(query, EmployeeDocument.class);

        // Convert to PageResult
        List<EmployeeDocument> documents = hits.stream()
            .map(SearchHit::getContent)
            .toList();

        return PageResult.<EmployeeDocument>builder()
            .list(documents)
            .total(hits.getTotalHits())
            .pageNum(pageNum)
            .pageSize(pageSize)
            .build();
    }
}
```

---

### Advanced Search with Filters

```java
@Service
@RequiredArgsConstructor
public class EmployeeSearchService {

    private final ElasticsearchRestTemplate elasticsearchTemplate;

    /**
     * Advanced search with multiple filters
     */
    public PageResult<EmployeeDocument> advancedSearch(EmployeeSearchForm form) {
        // Build bool query
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // Full-text search on name and department
        if (StringUtils.isNotBlank(form.getKeyword())) {
            MultiMatchQueryBuilder multiMatch = QueryBuilders.multiMatchQuery(
                form.getKeyword(),
                "name", "departmentName"
            ).type(MultiMatchQueryBuilder.Type.BEST_FIELDS);

            boolQuery.must(multiMatch);
        }

        // Filter by status
        if (form.getStatus() != null) {
            boolQuery.filter(QueryBuilders.termQuery("status", form.getStatus()));
        }

        // Filter by department
        if (StringUtils.isNotBlank(form.getDepartmentName())) {
            boolQuery.filter(QueryBuilders.termQuery("departmentName", form.getDepartmentName()));
        }

        // Filter by date range
        if (form.getStartDate() != null || form.getEndDate() != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("createdAt");
            if (form.getStartDate() != null) {
                rangeQuery.gte(form.getStartDate());
            }
            if (form.getEndDate() != null) {
                rangeQuery.lte(form.getEndDate());
            }
            boolQuery.filter(rangeQuery);
        }

        // Build native query
        NativeSearchQuery query = new NativeSearchQueryBuilder()
            .withQuery(boolQuery)
            .withPageable(PageRequest.of(form.getPageNum() - 1, form.getPageSize()))
            .withSort(SortBuilders.scoreSort().order(SortOrder.DESC))  // Sort by relevance
            .withSort(SortBuilders.fieldSort("createdAt").order(SortOrder.DESC))
            .build();

        // Execute search
        SearchHits<EmployeeDocument> hits = elasticsearchTemplate.search(query, EmployeeDocument.class);

        // Convert to PageResult
        List<EmployeeDocument> documents = hits.stream()
            .map(hit -> {
                EmployeeDocument doc = hit.getContent();
                doc.setScore(hit.getScore());  // Include relevance score
                return doc;
            })
            .toList();

        return PageResult.<EmployeeDocument>builder()
            .list(documents)
            .total(hits.getTotalHits())
            .pageNum(form.getPageNum())
            .pageSize(form.getPageSize())
            .build();
    }
}
```

**Time to Implement**: 15-20 minutes

---

## Pattern 3: Highlighting

### Search with Highlighted Results

```java
@Service
@RequiredArgsConstructor
public class EmployeeSearchService {

    private final ElasticsearchRestTemplate elasticsearchTemplate;

    /**
     * Search with highlighting
     */
    public PageResult<EmployeeSearchVO> searchWithHighlight(String keyword, int pageNum, int pageSize) {
        // Build query with highlighting
        NativeSearchQuery query = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.multiMatchQuery(keyword, "name", "departmentName"))
            .withPageable(PageRequest.of(pageNum - 1, pageSize))
            .withHighlightFields(
                new HighlightBuilder.Field("name").preTags("<em>").postTags("</em>"),
                new HighlightBuilder.Field("departmentName").preTags("<em>").postTags("</em>")
            )
            .build();

        // Execute search
        SearchHits<EmployeeDocument> hits = elasticsearchTemplate.search(query, EmployeeDocument.class);

        // Build result with highlights
        List<EmployeeSearchVO> results = hits.stream()
            .map(hit -> {
                EmployeeDocument doc = hit.getContent();
                Map<String, List<String>> highlights = hit.getHighlightFields();

                return EmployeeSearchVO.builder()
                    .id(doc.getId())
                    .name(getHighlight(highlights, "name", doc.getName()))
                    .email(doc.getEmail())
                    .departmentName(getHighlight(highlights, "departmentName", doc.getDepartmentName()))
                    .status(doc.getStatus())
                    .score(hit.getScore())
                    .build();
            })
            .toList();

        return PageResult.<EmployeeSearchVO>builder()
            .list(results)
            .total(hits.getTotalHits())
            .pageNum(pageNum)
            .pageSize(pageSize)
            .build();
    }

    /**
     * Get highlighted field or original value
     */
    private String getHighlight(Map<String, List<String>> highlights, String field, String original) {
        if (highlights != null && highlights.containsKey(field)) {
            List<String> fragments = highlights.get(field);
            if (!fragments.isEmpty()) {
                return fragments.get(0);  // Return first fragment with <em> tags
            }
        }
        return original;
    }
}
```

**Example Result**:
```json
{
  "id": 123,
  "name": "John <em>Smith</em>",
  "email": "john.smith@example.com",
  "departmentName": "IT <em>Department</em>",
  "status": "ACTIVE",
  "score": 2.45
}
```

**Time to Implement**: 10-12 minutes

---

## Pattern 4: Aggregations (Statistics)

### Department Statistics

```java
@Service
@RequiredArgsConstructor
public class EmployeeSearchService {

    private final ElasticsearchRestTemplate elasticsearchTemplate;

    /**
     * Get employee count by department
     */
    public Map<String, Long> getDepartmentStats() {
        // Build aggregation query
        NativeSearchQuery query = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.matchAllQuery())
            .addAggregation(AggregationBuilders.terms("by_department")
                .field("departmentName")
                .size(50))
            .build();

        // Execute aggregation
        SearchHits<EmployeeDocument> hits = elasticsearchTemplate.search(query, EmployeeDocument.class);

        // Extract aggregation results
        Aggregations aggregations = hits.getAggregations();
        if (aggregations == null) return Map.of();

        Terms terms = aggregations.get("by_department");
        return terms.getBuckets().stream()
            .collect(Collectors.toMap(
                Terms.Bucket::getKeyAsString,
                Terms.Bucket::getDocCount
            ));
    }

    /**
     * Get employee count by status
     */
    public Map<String, Long> getStatusStats() {
        NativeSearchQuery query = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.matchAllQuery())
            .addAggregation(AggregationBuilders.terms("by_status")
                .field("status")
                .size(10))
            .build();

        SearchHits<EmployeeDocument> hits = elasticsearchTemplate.search(query, EmployeeDocument.class);
        Aggregations aggregations = hits.getAggregations();

        if (aggregations == null) return Map.of();

        Terms terms = aggregations.get("by_status");
        return terms.getBuckets().stream()
            .collect(Collectors.toMap(
                Terms.Bucket::getKeyAsString,
                Terms.Bucket::getDocCount
            ));
    }

    /**
     * Get date histogram - employee growth trend
     */
    public List<DateHistogramBucket> getGrowthTrend() {
        NativeSearchQuery query = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.matchAllQuery())
            .addAggregation(AggregationBuilders.dateHistogram("by_month")
                .field("createdAt")
                .calendarInterval(DateHistogramInterval.MONTH)
                .format("yyyy-MM")
                .order(BucketOrder.key(true)))
            .build();

        SearchHits<EmployeeDocument> hits = elasticsearchTemplate.search(query, EmployeeDocument.class);
        Aggregations aggregations = hits.getAggregations();

        if (aggregations == null) return List.of();

        ParsedDateHistogram histogram = aggregations.get("by_month");
        return histogram.getBuckets().stream()
            .map(bucket -> DateHistogramBucket.builder()
                .date(bucket.getKeyAsString())
                .count(bucket.getDocCount())
                .build())
            .toList();
    }
}
```

**Time to Implement**: 15-20 minutes

---

## Pattern 5: Suggestion (Autocomplete)

### Search Suggestion

```java
@Service
@RequiredArgsConstructor
public class EmployeeSearchService {

    private final ElasticsearchRestTemplate elasticsearchTemplate;

    /**
     * Get search suggestions (prefix match)
     */
    public List<String> suggest(String prefix) {
        if (StringUtils.isBlank(prefix)) return List.of();

        // Prefix query on name field
        NativeSearchQuery query = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.prefixQuery("name.keyword", prefix))
            .withPageable(PageRequest.of(0, 10))
            .withFields("name")
            .build();

        SearchHits<EmployeeDocument> hits = elasticsearchTemplate.search(query, EmployeeDocument.class);

        return hits.stream()
            .map(hit -> hit.getContent().getName())
            .distinct()
            .toList();
    }
}
```

**Controller Endpoint**:
```java
@RestController
@RequestMapping("/api/employee/search")
@RequiredArgsConstructor
public class EmployeeSearchController {

    private final EmployeeSearchService searchService;

    /**
     * Search employees
     */
    @PostMapping("/query")
    public ResponseDTO<PageResult<EmployeeDocument>> search(@RequestBody EmployeeSearchForm form) {
        PageResult<EmployeeDocument> result = searchService.advancedSearch(form);
        return ResponseDTO.ok(result);
    }

    /**
     * Search suggestions (autocomplete)
     */
    @GetMapping("/suggest")
    public ResponseDTO<List<String>> suggest(@RequestParam String q) {
        List<String> suggestions = searchService.suggest(q);
        return ResponseDTO.ok(suggestions);
    }

    /**
     * Department statistics
     */
    @GetMapping("/stats/department")
    public ResponseDTO<Map<String, Long>> getDepartmentStats() {
        Map<String, Long> stats = searchService.getDepartmentStats();
        return ResponseDTO.ok(stats);
    }
}
```

**Time to Implement**: 8-10 minutes

---

## Common Errors and Quick Fixes

### Error 1: No results for Chinese search

**Symptom**: English searches work, Chinese searches return nothing

**Cause**: Missing IK Analyzer plugin

**Fix**: Install IK Analyzer
```bash
# Install IK Analyzer plugin
cd /usr/share/elasticsearch
bin/elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.11.0/elasticsearch-analysis-ik-8.11.0.zip

# Restart Elasticsearch
systemctl restart elasticsearch
```

---

### Error 2: Index not found

**Symptom**: `IndexNotFoundException: no such index [employees]`

**Cause**: Index not created

**Fix**: Create index on startup
```java
@Component
@RequiredArgsConstructor
public class ElasticsearchInitializer {

    private final ElasticsearchRestTemplate elasticsearchTemplate;

    @PostConstruct
    public void init() {
        IndexOperations indexOps = elasticsearchTemplate.indexOps(EmployeeDocument.class);
        if (!indexOps.exists()) {
            indexOps.create();
            indexOps.putMapping(indexOps.createMapping());
        }
    }
}
```

---

### Error 3: Out of sync with database

**Symptom**: Search results outdated

**Cause**: ES not synced after DB changes

**Fix**: Use event-driven sync
```java
@Component
@RequiredArgsConstructor
public class EmployeeEventListener {

    private final EmployeeSearchService searchService;

    @EventListener
    @Async  // Non-blocking
    public void onEmployeeChanged(EmployeeChangedEvent event) {
        searchService.syncEmployee(event.getEmployeeId());
    }
}
```

---

## Time Estimates

| Task | Implementation | Testing | Total |
|------|---------------|---------|-------|
| Elasticsearch Setup | 8 min | 2 min | 10 min |
| Index Creation | 4 min | 1 min | 5 min |
| Document Indexing | 6 min | 2 min | 8 min |
| Basic Search | 8 min | 2 min | 10 min |
| Advanced Search | 12 min | 3 min | 15 min |
| Highlighting | 8 min | 2 min | 10 min |
| Aggregations | 12 min | 3 min | 15 min |
| Suggestions | 6 min | 2 min | 8 min |

---

**See Also**:
- [SmartAdmin Patterns](../../../../shared/knowledge/smartadmin-patterns.md) - ResponseDTO pattern
- [PostgreSQL Best Practices](../postgresql-best-practices/) - Database optimization
