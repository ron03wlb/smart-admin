# Elasticsearch Search API Patterns

**Skill:** full-text-search-integration
**Component:** Elasticsearch / Search API / Query DSL
**Purpose:** Advanced search patterns for SmartAdmin applications

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

## Pattern 1: Multi-Field Search

### Match Query (Single Field)

```java
package net.lab1024.sa.admin.module.business.product.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.product.domain.document.ProductDocument;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ElasticsearchClient esClient;

    /**
     * Simple match query on single field
     */
    public List<ProductDocument> searchByName(String keyword) {
        try {
            SearchResponse<ProductDocument> response = esClient.search(s -> s
                .index("products")
                .query(q -> q
                    .match(m -> m
                        .field("productName")
                        .query(keyword)
                    )
                ),
                ProductDocument.class
            );

            return response.hits().hits().stream()
                .map(hit -> hit.source())
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Search by name failed", e);
            return List.of();
        }
    }
}
```

### Multi-Match Query (Multiple Fields)

```java
/**
 * Search across multiple fields with boosting
 */
public List<ProductDocument> searchMultiField(String keyword) {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .query(q -> q
                .multiMatch(m -> m
                    .query(keyword)
                    .fields("productName^3", "description^2", "category^1")  // Boost factors
                    .type(TextQueryType.BestFields)
                    .fuzziness("AUTO")
                    .prefixLength(2)
                )
            ),
            ProductDocument.class
        );

        return response.hits().hits().stream()
            .map(hit -> hit.source())
            .collect(Collectors.toList());

    } catch (Exception e) {
        log.error("Multi-field search failed", e);
        return List.of();
    }
}
```

---

## Pattern 2: Bool Query (Complex Conditions)

### Bool Query with Must/Should/Filter

```java
/**
 * Complex search with multiple conditions
 * - Must: Required conditions
 * - Should: Optional conditions (at least one should match)
 * - Filter: Exact match conditions (no scoring)
 * - Must Not: Exclusion conditions
 */
public List<ProductDocument> advancedSearch(String keyword, String category,
                                            BigDecimal minPrice, BigDecimal maxPrice,
                                            List<String> tags) {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .query(q -> q
                .bool(b -> b
                    // Must: Full-text search (required)
                    .must(m -> m
                        .multiMatch(mm -> mm
                            .query(keyword)
                            .fields("productName^2", "description")
                        )
                    )
                    // Filter: Category exact match (no scoring)
                    .filter(f -> f
                        .term(t -> t
                            .field("category")
                            .value(category)
                        )
                    )
                    // Filter: Price range
                    .filter(f -> f
                        .range(r -> r
                            .field("price")
                            .gte(JsonData.of(minPrice))
                            .lte(JsonData.of(maxPrice))
                        )
                    )
                    // Should: Tag matching (boost relevance)
                    .should(sh -> sh
                        .terms(t -> t
                            .field("tags")
                            .terms(tt -> tt.value(tags.stream()
                                .map(FieldValue::of)
                                .collect(Collectors.toList())))
                        )
                    )
                    .minimumShouldMatch("1")  // At least one 'should' clause must match
                )
            )
            .sort(so -> so
                .score(sc -> sc.order(SortOrder.Desc))
            )
            .size(20),
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
```

---

## Pattern 3: Fuzzy Search (Typo Tolerance)

```java
/**
 * Fuzzy search with automatic typo tolerance
 * Fuzziness levels:
 * - AUTO: 0 for 1-2 chars, 1 for 3-5 chars, 2 for >5 chars
 * - 0, 1, 2: Explicit edit distance
 */
public List<ProductDocument> fuzzySearch(String keyword) {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .query(q -> q
                .fuzzy(f -> f
                    .field("productName")
                    .value(keyword)
                    .fuzziness("AUTO")
                    .maxExpansions(50)
                    .prefixLength(0)
                    .transpositions(true)  // Allow character transpositions (ab → ba)
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
```

---

## Pattern 4: Phrase Search (Exact Order)

```java
/**
 * Match phrase query (exact word order)
 */
public List<ProductDocument> phraseSearch(String phrase) {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .query(q -> q
                .matchPhrase(mp -> mp
                    .field("description")
                    .query(phrase)
                    .slop(2)  // Allow 2 word gaps between terms
                )
            ),
            ProductDocument.class
        );

        return response.hits().hits().stream()
            .map(hit -> hit.source())
            .collect(Collectors.toList());

    } catch (Exception e) {
        log.error("Phrase search failed", e);
        return List.of();
    }
}
```

---

## Pattern 5: Prefix/Wildcard Search

### Prefix Search (Autocomplete)

```java
/**
 * Prefix search for autocomplete
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
```

### Wildcard Search

```java
/**
 * Wildcard search (* for multiple chars, ? for single char)
 * WARNING: Wildcard queries are slow, use sparingly
 */
public List<ProductDocument> wildcardSearch(String pattern) {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .query(q -> q
                .wildcard(w -> w
                    .field("productName")
                    .value("*" + pattern + "*")
                    .caseInsensitive(true)
                )
            ),
            ProductDocument.class
        );

        return response.hits().hits().stream()
            .map(hit -> hit.source())
            .collect(Collectors.toList());

    } catch (Exception e) {
        log.error("Wildcard search failed", e);
        return List.of();
    }
}
```

---

## Pattern 6: Range Queries

```java
/**
 * Range query for numeric/date fields
 */
public List<ProductDocument> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .query(q -> q
                .range(r -> r
                    .field("price")
                    .gte(JsonData.of(minPrice))
                    .lte(JsonData.of(maxPrice))
                )
            ),
            ProductDocument.class
        );

        return response.hits().hits().stream()
            .map(hit -> hit.source())
            .collect(Collectors.toList());

    } catch (Exception e) {
        log.error("Price range search failed", e);
        return List.of();
    }
}

/**
 * Date range query
 */
public List<ProductDocument> searchByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .query(q -> q
                .range(r -> r
                    .field("createdAt")
                    .gte(JsonData.of(startDate.toString()))
                    .lte(JsonData.of(endDate.toString()))
                    .format("strict_date_time")
                )
            ),
            ProductDocument.class
        );

        return response.hits().hits().stream()
            .map(hit -> hit.source())
            .collect(Collectors.toList());

    } catch (Exception e) {
        log.error("Date range search failed", e);
        return List.of();
    }
}
```

---

## Pattern 7: Function Score (Custom Relevance)

```java
/**
 * Function score query - custom relevance boosting
 * Use cases: Promote featured products, boost by popularity, recency scoring
 */
public List<ProductDocument> searchWithBoost(String keyword) {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .query(q -> q
                .functionScore(fs -> fs
                    .query(fq -> fq
                        .multiMatch(mm -> mm
                            .query(keyword)
                            .fields("productName", "description")
                        )
                    )
                    // Boost by stock availability
                    .functions(f -> f
                        .filter(fi -> fi
                            .range(r -> r
                                .field("stock")
                                .gte(JsonData.of(10))
                            )
                        )
                        .weight(2.0)  // 2x boost for products with stock >= 10
                    )
                    // Boost by price (cheaper products rank higher)
                    .functions(f -> f
                        .fieldValueFactor(fv -> fv
                            .field("price")
                            .factor(0.01)
                            .modifier(FieldValueFactorModifier.Reciprocal)
                            .missing(1.0)
                        )
                    )
                    // Boost recent products
                    .functions(f -> f
                        .gauss(g -> g
                            .field("createdAt")
                            .placement(p -> p
                                .origin(JsonData.of(LocalDateTime.now().toString()))
                                .scale(JsonData.of("30d"))  // Decay over 30 days
                                .offset(JsonData.of("7d"))   // No decay for first 7 days
                                .decay(0.5)
                            )
                        )
                    )
                    .scoreMode(FunctionScoreMode.Sum)
                    .boostMode(FunctionBoostMode.Multiply)
                )
            ),
            ProductDocument.class
        );

        return response.hits().hits().stream()
            .map(hit -> hit.source())
            .collect(Collectors.toList());

    } catch (Exception e) {
        log.error("Function score search failed", e);
        return List.of();
    }
}
```

---

## Pattern 8: Pagination Strategies

### Standard Pagination (from/size)

```java
/**
 * Standard pagination with from/size
 * Good for: Small result sets (< 10,000 documents)
 * Limitation: Deep pagination is expensive
 */
public PageResult<ProductDocument> searchWithPagination(String keyword, int pageNum, int pageSize) {
    try {
        int from = (pageNum - 1) * pageSize;

        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .query(q -> q
                .match(m -> m
                    .field("productName")
                    .query(keyword)
                )
            )
            .from(from)
            .size(pageSize)
            .trackTotalHits(t -> t.enabled(true)),
            ProductDocument.class
        );

        long total = response.hits().total().value();
        List<ProductDocument> records = response.hits().hits().stream()
            .map(hit -> hit.source())
            .collect(Collectors.toList());

        return new PageResult<>(records, total, pageNum, pageSize);

    } catch (Exception e) {
        log.error("Paginated search failed", e);
        return PageResult.empty();
    }
}
```

### Search After (Efficient Deep Pagination)

```java
/**
 * Search after pagination - efficient for large result sets
 * Good for: Deep pagination, scrolling, real-time data
 */
public SearchAfterResult<ProductDocument> searchAfter(String keyword, List<Object> searchAfter, int size) {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> {
            var builder = s
                .index("products")
                .query(q -> q
                    .match(m -> m
                        .field("productName")
                        .query(keyword)
                    )
                )
                .sort(so -> so
                    .field(f -> f.field("createdAt").order(SortOrder.Desc))
                )
                .sort(so -> so
                    .field(f -> f.field("_id").order(SortOrder.Asc))  // Tie-breaker
                )
                .size(size);

            if (searchAfter != null && !searchAfter.isEmpty()) {
                builder.searchAfter(searchAfter);
            }

            return builder;
        }, ProductDocument.class);

        List<ProductDocument> records = response.hits().hits().stream()
            .map(hit -> hit.source())
            .collect(Collectors.toList());

        // Get last hit's sort values for next page
        List<Object> nextSearchAfter = null;
        if (!response.hits().hits().isEmpty()) {
            var lastHit = response.hits().hits().get(response.hits().hits().size() - 1);
            nextSearchAfter = lastHit.sort();
        }

        return new SearchAfterResult<>(records, nextSearchAfter);

    } catch (Exception e) {
        log.error("Search after failed", e);
        return SearchAfterResult.empty();
    }
}

// Result wrapper
public class SearchAfterResult<T> {
    private List<T> records;
    private List<Object> nextSearchAfter;

    // Constructor, getters...
}
```

### Scroll API (Large Export)

```java
/**
 * Scroll API for large result set export
 * Good for: Batch processing, large data export
 * WARNING: Resource-intensive, use for background jobs only
 */
public List<ProductDocument> scrollAll(String keyword) {
    List<ProductDocument> allResults = new ArrayList<>();

    try {
        // Initial search request with scroll context
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .query(q -> q
                .match(m -> m
                    .field("productName")
                    .query(keyword)
                )
            )
            .scroll(Time.of(t -> t.time("5m")))  // Keep scroll context for 5 minutes
            .size(1000),  // Fetch 1000 docs per batch
            ProductDocument.class
        );

        String scrollId = response.scrollId();

        while (true) {
            List<ProductDocument> batch = response.hits().hits().stream()
                .map(hit -> hit.source())
                .collect(Collectors.toList());

            if (batch.isEmpty()) {
                break;
            }

            allResults.addAll(batch);

            // Fetch next batch
            response = esClient.scroll(s -> s
                .scrollId(scrollId)
                .scroll(Time.of(t -> t.time("5m"))),
                ProductDocument.class
            );
        }

        // Clear scroll context
        esClient.clearScroll(c -> c.scrollId(scrollId));

    } catch (Exception e) {
        log.error("Scroll search failed", e);
    }

    return allResults;
}
```

---

## Pattern 9: Highlighting

```java
/**
 * Search with result highlighting
 */
public List<ProductSearchResult> searchWithHighlight(String keyword) {
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
                    .preTags("<em class='highlight'>")
                    .postTags("</em>")
                    .fragmentSize(150)
                    .numberOfFragments(1)
                )
                .fields("description", hf -> hf
                    .preTags("<em class='highlight'>")
                    .postTags("</em>")
                    .fragmentSize(200)
                    .numberOfFragments(3)
                )
            ),
            ProductDocument.class
        );

        return response.hits().hits().stream()
            .map(hit -> {
                ProductDocument source = hit.source();
                Map<String, List<String>> highlights = hit.highlight();

                ProductSearchResult result = new ProductSearchResult();
                result.setProduct(source);
                result.setHighlightedName(
                    highlights.containsKey("productName")
                        ? highlights.get("productName").get(0)
                        : source.getProductName()
                );
                result.setHighlightedDescription(
                    highlights.containsKey("description")
                        ? highlights.get("description")
                        : List.of(source.getDescription())
                );

                return result;
            })
            .collect(Collectors.toList());

    } catch (Exception e) {
        log.error("Search with highlight failed", e);
        return List.of();
    }
}

// Result wrapper
@Data
public class ProductSearchResult {
    private ProductDocument product;
    private String highlightedName;
    private List<String> highlightedDescription;
}
```

---

## Pattern 10: SmartAdmin Controller Integration

```java
package net.lab1024.sa.admin.module.business.product.controller;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.admin.module.business.product.domain.document.ProductDocument;
import net.lab1024.sa.admin.module.business.product.domain.form.ProductSearchForm;
import net.lab1024.sa.admin.module.business.product.service.ProductSearchService;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/product/search")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductSearchService searchService;

    /**
     * Simple keyword search
     */
    @GetMapping("/simple")
    public ResponseDTO<List<ProductDocument>> simpleSearch(@RequestParam String keyword) {
        List<ProductDocument> results = searchService.searchByName(keyword);
        return ResponseDTO.ok(results);
    }

    /**
     * Multi-field search
     */
    @GetMapping("/multi-field")
    public ResponseDTO<List<ProductDocument>> multiFieldSearch(@RequestParam String keyword) {
        List<ProductDocument> results = searchService.searchMultiField(keyword);
        return ResponseDTO.ok(results);
    }

    /**
     * Advanced search with filters
     */
    @PostMapping("/advanced")
    public ResponseDTO<List<ProductDocument>> advancedSearch(@Valid @RequestBody ProductSearchForm form) {
        List<ProductDocument> results = searchService.advancedSearch(
            form.getKeyword(),
            form.getCategory(),
            form.getMinPrice(),
            form.getMaxPrice(),
            form.getTags()
        );
        return ResponseDTO.ok(results);
    }

    /**
     * Fuzzy search (typo tolerance)
     */
    @GetMapping("/fuzzy")
    public ResponseDTO<List<ProductDocument>> fuzzySearch(@RequestParam String keyword) {
        List<ProductDocument> results = searchService.fuzzySearch(keyword);
        return ResponseDTO.ok(results);
    }

    /**
     * Autocomplete suggestions
     */
    @GetMapping("/autocomplete")
    public ResponseDTO<List<String>> autocomplete(@RequestParam String prefix) {
        List<String> suggestions = searchService.autocomplete(prefix);
        return ResponseDTO.ok(suggestions);
    }

    /**
     * Paginated search
     */
    @GetMapping("/paginated")
    public ResponseDTO<PageResult<ProductDocument>> paginatedSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        PageResult<ProductDocument> results = searchService.searchWithPagination(keyword, pageNum, pageSize);
        return ResponseDTO.ok(results);
    }

    /**
     * Search with highlighting
     */
    @GetMapping("/highlight")
    public ResponseDTO<List<ProductSearchResult>> searchWithHighlight(@RequestParam String keyword) {
        List<ProductSearchResult> results = searchService.searchWithHighlight(keyword);
        return ResponseDTO.ok(results);
    }
}
```

---

## Best Practices

1. **Query Selection:**
   - Simple keyword: Use `match` query
   - Multiple fields: Use `multi_match` query with boosting
   - Exact match: Use `term` query (keyword fields)
   - Complex conditions: Use `bool` query

2. **Performance:**
   - Use `filter` context for non-scoring queries (category, price range)
   - Avoid wildcard queries at start of string (`*keyword`)
   - Use pagination (`from/size` or `search_after`)
   - Cache frequently used filters

3. **Relevance Tuning:**
   - Boost important fields (`productName^3`)
   - Use function score for custom ranking
   - Analyze search logs to improve relevance

4. **Pagination:**
   - `from/size`: Good for < 10,000 results
   - `search_after`: Good for deep pagination
   - `scroll`: Only for background jobs (exports)

5. **Error Handling:**
   - Always catch exceptions
   - Return empty results on failure
   - Log errors with sufficient context

---

**Next:** [Aggregation Queries](aggregation-queries.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
