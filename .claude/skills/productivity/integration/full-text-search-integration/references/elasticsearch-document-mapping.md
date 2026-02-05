# Elasticsearch Document Mapping Guide

**Skill:** full-text-search-integration
**Component:** Elasticsearch / Document Mapping
**Purpose:** Map MyBatis Plus entities to Elasticsearch documents for SmartAdmin

---

## Dependencies

```gradle
dependencies {
    // Spring Data Elasticsearch
    implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch:3.2.2'

    // Elasticsearch Java client
    implementation 'co.elastic.clients:elasticsearch-java:8.12.0'
}
```

---

## Configuration

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

---

## Pattern 1: Entity to Document Mapping

### MyBatis Entity

```java
@Data
@TableName("t_product")
public class ProductEntity {
    @TableId(type = IdType.AUTO)
    private Long productId;
    private String productName;
    private String description;
    private BigDecimal price;
    private String category;
    private Integer stock;
    private LocalDateTime createdAt;
}
```

### Elasticsearch Document

```java
package net.lab1024.sa.business.product.domain.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "products")  // ES index name
@Setting(
    shards = 3,           // Number of primary shards
    replicas = 1,         // Number of replicas
    refreshInterval = "1s"  // Refresh interval
)
public class ProductDocument {

    @Id
    @Field(type = FieldType.Long)
    private Long productId;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String productName;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String description;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Keyword)  // Exact match, aggregations
    private String category;

    @Field(type = FieldType.Integer)
    private Integer stock;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private LocalDateTime createdAt;
}
```

---

## Pattern 2: ES Repository

```java
package net.lab1024.sa.business.product.repository;

import net.lab1024.sa.business.product.domain.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, Long> {

    /**
     * Full-text search by product name
     */
    List<ProductDocument> findByProductNameContaining(String keyword);

    /**
     * Search by category
     */
    List<ProductDocument> findByCategory(String category);

    /**
     * Search by price range
     */
    List<ProductDocument> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
}
```

---

## Pattern 3: Search Service

```java
package net.lab1024.sa.business.product.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.product.domain.document.ProductDocument;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ElasticsearchClient esClient;
    private final ProductSearchRepository productSearchRepository;

    /**
     * Simple full-text search
     */
    public List<ProductDocument> search(String keyword) {
        return productSearchRepository.findByProductNameContaining(keyword);
    }

    /**
     * Advanced search with multiple criteria
     */
    public List<ProductDocument> advancedSearch(String keyword, String category,
                                                BigDecimal minPrice, BigDecimal maxPrice) {
        try {
            SearchResponse<ProductDocument> response = esClient.search(s -> s
                .index("products")
                .query(q -> q
                    .bool(b -> b
                        .must(m -> m
                            .multiMatch(mm -> mm
                                .query(keyword)
                                .fields("productName", "description")
                            )
                        )
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
                                .lte(JsonData.of(maxPrice))
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
            log.error("Advanced search failed", e);
            return List.of();
        }
    }

    /**
     * Fuzzy search (typo tolerance)
     */
    public List<ProductDocument> fuzzySearch(String keyword) {
        try {
            SearchResponse<ProductDocument> response = esClient.search(s -> s
                .index("products")
                .query(q -> q
                    .fuzzy(f -> f
                        .field("productName")
                        .value(keyword)
                        .fuzziness("AUTO")  // Allow 1-2 character edits
                    )
                ),
                ProductDocument.class
            );

            return response.hits().hits().stream()
                .map(hit -> hit.source())
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Fuzzy search failed", e);
            return List.of();
        }
    }

    /**
     * Autocomplete (prefix search)
     */
    public List<String> autocomplete(String prefix) {
        try {
            SearchResponse<ProductDocument> response = esClient.search(s -> s
                .index("products")
                .query(q -> q
                    .prefix(p -> p
                        .field("productName")
                        .value(prefix)
                    )
                )
                .size(10),
                ProductDocument.class
            );

            return response.hits().hits().stream()
                .map(hit -> hit.source().getProductName())
                .distinct()
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Autocomplete failed", e);
            return List.of();
        }
    }
}
```

---

## Pattern 4: Aggregations

```java
/**
 * Category facet aggregation
 */
public Map<String, Long> getCategoryFacets() {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .size(0)  // Don't return documents
            .aggregations("categories", a -> a
                .terms(t -> t
                    .field("category")
                    .size(100)
                )
            ),
            ProductDocument.class
        );

        return response.aggregations().get("categories")
            .sterms().buckets().array().stream()
            .collect(Collectors.toMap(
                bucket -> bucket.key().stringValue(),
                bucket -> bucket.docCount()
            ));

    } catch (Exception e) {
        log.error("Category facets failed", e);
        return Map.of();
    }
}

/**
 * Price statistics aggregation
 */
public Map<String, Double> getPriceStatistics() {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .size(0)
            .aggregations("price_stats", a -> a
                .stats(st -> st.field("price"))
            ),
            ProductDocument.class
        );

        var stats = response.aggregations().get("price_stats").stats();
        return Map.of(
            "min", stats.min(),
            "max", stats.max(),
            "avg", stats.avg(),
            "sum", stats.sum()
        );

    } catch (Exception e) {
        log.error("Price statistics failed", e);
        return Map.of();
    }
}
```

---

## Pattern 5: Highlighting

```java
/**
 * Search with highlighting
 */
public List<Map<String, Object>> searchWithHighlight(String keyword) {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .query(q -> q
                .multiMatch(m -> m
                    .query(keyword)
                    .fields("productName", "description")
                )
            )
            .highlight(h -> h
                .fields("productName", hf -> hf
                    .preTags("<em>")
                    .postTags("</em>")
                    .fragmentSize(150)
                )
                .fields("description", hf -> hf
                    .preTags("<em>")
                    .postTags("</em>")
                    .fragmentSize(200)
                )
            ),
            ProductDocument.class
        );

        return response.hits().hits().stream()
            .map(hit -> {
                Map<String, Object> result = new HashMap<>();
                result.put("source", hit.source());
                result.put("highlights", hit.highlight());
                return result;
            })
            .collect(Collectors.toList());

    } catch (Exception e) {
        log.error("Search with highlight failed", e);
        return List.of();
    }
}
```

---

## Pattern 6: SmartAdmin Controller Integration

```java
@RestController
@RequestMapping("/api/product/search")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearchService searchService;

    @GetMapping("/fulltext")
    public ResponseDTO<List<ProductDocument>> fullTextSearch(@RequestParam String keyword) {
        List<ProductDocument> results = searchService.search(keyword);
        return ResponseDTO.ok(results);
    }

    @PostMapping("/advanced")
    public ResponseDTO<List<ProductDocument>> advancedSearch(@RequestBody ProductSearchForm form) {
        List<ProductDocument> results = searchService.advancedSearch(
            form.getKeyword(),
            form.getCategory(),
            form.getMinPrice(),
            form.getMaxPrice()
        );
        return ResponseDTO.ok(results);
    }

    @GetMapping("/autocomplete")
    public ResponseDTO<List<String>> autocomplete(@RequestParam String prefix) {
        List<String> suggestions = searchService.autocomplete(prefix);
        return ResponseDTO.ok(suggestions);
    }

    @GetMapping("/facets/category")
    public ResponseDTO<Map<String, Long>> getCategoryFacets() {
        Map<String, Long> facets = searchService.getCategoryFacets();
        return ResponseDTO.ok(facets);
    }
}
```

---

## Best Practices

1. **Field Types:**
   - `text`: Full-text search fields (analyzed)
   - `keyword`: Exact match, aggregations, sorting
   - `date`: Date/time fields with format
   - `long/double`: Numeric fields for range queries

2. **Analyzers:**
   - Chinese: `ik_max_word` (indexing), `ik_smart` (searching)
   - English: `english`, `standard`
   - Custom: Define in index settings

3. **Sharding:**
   - Small indexes (< 10GB): 1-3 shards
   - Large indexes (> 10GB): Calculate based on data size
   - Each shard: 20-50GB recommended

4. **Performance:**
   - Use keyword fields for exact matches
   - Avoid wildcard queries on large datasets
   - Use pagination (from/size or search_after)
   - Cache frequently used searches

---

**Next:** [Search API Patterns](search-api-patterns.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
