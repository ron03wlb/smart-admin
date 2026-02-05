# Elasticsearch Aggregation Query Patterns

**Skill:** full-text-search-integration
**Component:** Elasticsearch / Aggregations / Analytics
**Purpose:** Data aggregation and analytics patterns for SmartAdmin

---

## Aggregation Types

1. **Bucket Aggregations:** Group documents into buckets
   - Terms, Histogram, Date Histogram, Range, Filters

2. **Metrics Aggregations:** Calculate metrics on documents
   - Sum, Avg, Min, Max, Stats, Cardinality, Percentiles

3. **Pipeline Aggregations:** Aggregate on other aggregation results
   - Derivative, Cumulative Sum, Moving Average, Bucket Sort

---

## Pattern 1: Terms Aggregation (Faceted Search)

### Category Facets

```java
package net.lab1024.sa.business.product.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.product.domain.document.ProductDocument;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductAggregationService {

    private final ElasticsearchClient esClient;

    /**
     * Category facets (count products by category)
     */
    public Map<String, Long> getCategoryFacets() {
        try {
            SearchResponse<ProductDocument> response = esClient.search(s -> s
                .index("products")
                .size(0)  // Don't return documents, only aggregations
                .aggregations("categories", a -> a
                    .terms(t -> t
                        .field("category")
                        .size(50)  // Return top 50 categories
                        .order(o -> o.key(SortOrder.Asc))
                    )
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

    /**
     * Nested facets with filtering
     * Example: Get category facets for products matching a keyword
     */
    public Map<String, Long> getCategoryFacetsWithFilter(String keyword) {
        try {
            SearchResponse<ProductDocument> response = esClient.search(s -> s
                .index("products")
                .size(0)
                .query(q -> q
                    .match(m -> m
                        .field("productName")
                        .query(keyword)
                    )
                )
                .aggregations("categories", a -> a
                    .terms(t -> t
                        .field("category")
                        .size(50)
                    )
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
            log.error("Filtered category facets failed", e);
            return Map.of();
        }
    }
}
```

---

## Pattern 2: Histogram Aggregation

### Price Histogram

```java
/**
 * Price histogram (group products by price ranges)
 */
public Map<String, Long> getPriceHistogram(double interval) {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .size(0)
            .aggregations("price_histogram", a -> a
                .histogram(h -> h
                    .field("price")
                    .interval(interval)  // e.g., 100 for $0-100, $100-200, etc.
                    .minDocCount(1)      // Only return non-empty buckets
                )
            ),
            ProductDocument.class
        );

        Map<String, Long> histogram = new LinkedHashMap<>();
        response.aggregations().get("price_histogram").histogram().buckets().array()
            .forEach(bucket -> {
                double from = bucket.key();
                double to = from + interval;
                String range = String.format("%.2f-%.2f", from, to);
                histogram.put(range, bucket.docCount());
            });

        return histogram;

    } catch (Exception e) {
        log.error("Price histogram failed", e);
        return Map.of();
    }
}
```

### Date Histogram

```java
/**
 * Date histogram (group products by time intervals)
 */
public Map<String, Long> getProductsByMonth() {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .size(0)
            .aggregations("products_by_month", a -> a
                .dateHistogram(dh -> dh
                    .field("createdAt")
                    .calendarInterval(CalendarInterval.Month)
                    .format("yyyy-MM")
                    .minDocCount(0)  // Include empty months
                )
            ),
            ProductDocument.class
        );

        Map<String, Long> histogram = new LinkedHashMap<>();
        response.aggregations().get("products_by_month").dateHistogram().buckets().array()
            .forEach(bucket -> {
                histogram.put(bucket.keyAsString(), bucket.docCount());
            });

        return histogram;

    } catch (Exception e) {
        log.error("Date histogram failed", e);
        return Map.of();
    }
}
```

---

## Pattern 3: Range Aggregation

```java
/**
 * Price range aggregation (predefined ranges)
 */
public Map<String, Long> getPriceRanges() {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .size(0)
            .aggregations("price_ranges", a -> a
                .range(r -> r
                    .field("price")
                    .ranges(range -> range.to(50.0).key("Under $50"))
                    .ranges(range -> range.from(50.0).to(100.0).key("$50-$100"))
                    .ranges(range -> range.from(100.0).to(200.0).key("$100-$200"))
                    .ranges(range -> range.from(200.0).key("Over $200"))
                )
            ),
            ProductDocument.class
        );

        Map<String, Long> ranges = new LinkedHashMap<>();
        response.aggregations().get("price_ranges").range().buckets().array()
            .forEach(bucket -> {
                ranges.put(bucket.key(), bucket.docCount());
            });

        return ranges;

    } catch (Exception e) {
        log.error("Price ranges failed", e);
        return Map.of();
    }
}
```

---

## Pattern 4: Metrics Aggregations

### Statistics

```java
/**
 * Calculate price statistics (min, max, avg, sum)
 */
@Data
public class PriceStatistics {
    private Double min;
    private Double max;
    private Double avg;
    private Double sum;
    private Long count;
}

public PriceStatistics getPriceStatistics() {
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

        PriceStatistics result = new PriceStatistics();
        result.setMin(stats.min());
        result.setMax(stats.max());
        result.setAvg(stats.avg());
        result.setSum(stats.sum());
        result.setCount(stats.count());

        return result;

    } catch (Exception e) {
        log.error("Price statistics failed", e);
        return new PriceStatistics();
    }
}
```

### Extended Statistics

```java
/**
 * Extended statistics (includes variance, std deviation, sum of squares)
 */
@Data
public class ExtendedPriceStatistics extends PriceStatistics {
    private Double variance;
    private Double stdDeviation;
    private Double sumOfSquares;
}

public ExtendedPriceStatistics getExtendedPriceStatistics() {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .size(0)
            .aggregations("extended_price_stats", a -> a
                .extendedStats(est -> est.field("price"))
            ),
            ProductDocument.class
        );

        var stats = response.aggregations().get("extended_price_stats").extendedStats();

        ExtendedPriceStatistics result = new ExtendedPriceStatistics();
        result.setMin(stats.min());
        result.setMax(stats.max());
        result.setAvg(stats.avg());
        result.setSum(stats.sum());
        result.setCount(stats.count());
        result.setVariance(stats.variance());
        result.setStdDeviation(stats.stdDeviation());
        result.setSumOfSquares(stats.sumOfSquares());

        return result;

    } catch (Exception e) {
        log.error("Extended price statistics failed", e);
        return new ExtendedPriceStatistics();
    }
}
```

### Cardinality (Unique Count)

```java
/**
 * Count unique categories
 */
public Long getUniqueCategoryCount() {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .size(0)
            .aggregations("unique_categories", a -> a
                .cardinality(c -> c
                    .field("category")
                    .precisionThreshold(100)  // Accuracy threshold
                )
            ),
            ProductDocument.class
        );

        return response.aggregations().get("unique_categories").cardinality().value();

    } catch (Exception e) {
        log.error("Cardinality calculation failed", e);
        return 0L;
    }
}
```

### Percentiles

```java
/**
 * Calculate price percentiles (P50, P95, P99)
 */
public Map<String, Double> getPricePercentiles() {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .size(0)
            .aggregations("price_percentiles", a -> a
                .percentiles(p -> p
                    .field("price")
                    .percents(50.0, 95.0, 99.0)
                )
            ),
            ProductDocument.class
        );

        Map<String, Double> percentiles = new LinkedHashMap<>();
        response.aggregations().get("price_percentiles").percentiles().values().keyed()
            .forEach((key, value) -> {
                percentiles.put("P" + key, value);
            });

        return percentiles;

    } catch (Exception e) {
        log.error("Percentiles calculation failed", e);
        return Map.of();
    }
}
```

---

## Pattern 5: Nested Aggregations (Sub-Aggregations)

```java
/**
 * Category facets with average price per category
 */
@Data
public class CategoryStats {
    private String category;
    private Long productCount;
    private Double avgPrice;
    private Double minPrice;
    private Double maxPrice;
}

public List<CategoryStats> getCategoryStatsWithPrice() {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .size(0)
            .aggregations("categories", a -> a
                .terms(t -> t
                    .field("category")
                    .size(50)
                )
                .aggregations("avg_price", sa -> sa
                    .avg(avg -> avg.field("price"))
                )
                .aggregations("min_price", sa -> sa
                    .min(min -> min.field("price"))
                )
                .aggregations("max_price", sa -> sa
                    .max(max -> max.field("price"))
                )
            ),
            ProductDocument.class
        );

        List<CategoryStats> stats = new ArrayList<>();
        response.aggregations().get("categories").sterms().buckets().array()
            .forEach(bucket -> {
                CategoryStats stat = new CategoryStats();
                stat.setCategory(bucket.key().stringValue());
                stat.setProductCount(bucket.docCount());
                stat.setAvgPrice(bucket.aggregations().get("avg_price").avg().value());
                stat.setMinPrice(bucket.aggregations().get("min_price").min().value());
                stat.setMaxPrice(bucket.aggregations().get("max_price").max().value());
                stats.add(stat);
            });

        return stats;

    } catch (Exception e) {
        log.error("Category stats with price failed", e);
        return List.of();
    }
}
```

---

## Pattern 6: Pipeline Aggregations

### Moving Average

```java
/**
 * Calculate moving average of product counts over time
 */
public Map<String, Double> getProductCountMovingAverage() {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .size(0)
            .aggregations("products_by_day", a -> a
                .dateHistogram(dh -> dh
                    .field("createdAt")
                    .calendarInterval(CalendarInterval.Day)
                    .format("yyyy-MM-dd")
                )
                .aggregations("moving_avg", sa -> sa
                    .movingAvg(ma -> ma
                        .bucketsPath("_count")  // Use document count
                        .window(7)              // 7-day window
                    )
                )
            ),
            ProductDocument.class
        );

        Map<String, Double> movingAvg = new LinkedHashMap<>();
        response.aggregations().get("products_by_day").dateHistogram().buckets().array()
            .forEach(bucket -> {
                var maValue = bucket.aggregations().get("moving_avg");
                if (maValue != null && maValue.movingAvg() != null) {
                    movingAvg.put(bucket.keyAsString(), maValue.movingAvg().value());
                }
            });

        return movingAvg;

    } catch (Exception e) {
        log.error("Moving average calculation failed", e);
        return Map.of();
    }
}
```

### Cumulative Sum

```java
/**
 * Calculate cumulative product count over time
 */
public Map<String, Long> getCumulativeProductCount() {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .size(0)
            .aggregations("products_by_month", a -> a
                .dateHistogram(dh -> dh
                    .field("createdAt")
                    .calendarInterval(CalendarInterval.Month)
                    .format("yyyy-MM")
                )
                .aggregations("cumulative_count", sa -> sa
                    .cumulativeSum(cs -> cs
                        .bucketsPath("_count")
                    )
                )
            ),
            ProductDocument.class
        );

        Map<String, Long> cumulativeCount = new LinkedHashMap<>();
        response.aggregations().get("products_by_month").dateHistogram().buckets().array()
            .forEach(bucket -> {
                var csValue = bucket.aggregations().get("cumulative_count");
                if (csValue != null && csValue.cumulativeSum() != null) {
                    cumulativeCount.put(bucket.keyAsString(),
                        csValue.cumulativeSum().value().longValue());
                }
            });

        return cumulativeCount;

    } catch (Exception e) {
        log.error("Cumulative sum calculation failed", e);
        return Map.of();
    }
}
```

### Bucket Sort (Top N)

```java
/**
 * Get top N categories by product count
 */
public List<CategoryStats> getTopCategories(int topN) {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .size(0)
            .aggregations("categories", a -> a
                .terms(t -> t
                    .field("category")
                    .size(1000)  // Collect all categories first
                )
                .aggregations("top_categories", sa -> sa
                    .bucketSort(bs -> bs
                        .sort(so -> so
                            .field(f -> f.field("_count").order(SortOrder.Desc))
                        )
                        .size(topN)  // Return only top N
                    )
                )
            ),
            ProductDocument.class
        );

        List<CategoryStats> topCategories = new ArrayList<>();
        response.aggregations().get("categories").sterms().buckets().array()
            .forEach(bucket -> {
                CategoryStats stat = new CategoryStats();
                stat.setCategory(bucket.key().stringValue());
                stat.setProductCount(bucket.docCount());
                topCategories.add(stat);
            });

        return topCategories;

    } catch (Exception e) {
        log.error("Top categories calculation failed", e);
        return List.of();
    }
}
```

---

## Pattern 7: Filters Aggregation

```java
/**
 * Filter aggregation (multiple predefined filters)
 */
@Data
public class ProductSegmentation {
    private Long inStock;
    private Long outOfStock;
    private Long featured;
    private Long onSale;
}

public ProductSegmentation getProductSegmentation() {
    try {
        SearchResponse<ProductDocument> response = esClient.search(s -> s
            .index("products")
            .size(0)
            .aggregations("segments", a -> a
                .filters(f -> f
                    .filters(Map.of(
                        "in_stock", Query.of(q -> q.range(r -> r.field("stock").gte(JsonData.of(1)))),
                        "out_of_stock", Query.of(q -> q.term(t -> t.field("stock").value(0))),
                        "featured", Query.of(q -> q.term(t -> t.field("featured").value(true))),
                        "on_sale", Query.of(q -> q.exists(e -> e.field("salePrice")))
                    ))
                )
            ),
            ProductDocument.class
        );

        var buckets = response.aggregations().get("segments").filters().buckets().keyed();

        ProductSegmentation segmentation = new ProductSegmentation();
        segmentation.setInStock(buckets.get("in_stock").docCount());
        segmentation.setOutOfStock(buckets.get("out_of_stock").docCount());
        segmentation.setFeatured(buckets.get("featured").docCount());
        segmentation.setOnSale(buckets.get("on_sale").docCount());

        return segmentation;

    } catch (Exception e) {
        log.error("Product segmentation failed", e);
        return new ProductSegmentation();
    }
}
```

---

## Pattern 8: SmartAdmin Controller Integration

```java
package net.lab1024.sa.business.product.controller;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.business.product.domain.vo.CategoryStats;
import net.lab1024.sa.business.product.domain.vo.PriceStatistics;
import net.lab1024.sa.business.product.domain.vo.ProductSegmentation;
import net.lab1024.sa.business.product.service.ProductAggregationService;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/product/analytics")
@RequiredArgsConstructor
public class ProductAnalyticsController {

    private final ProductAggregationService aggregationService;

    /**
     * Get category facets (count by category)
     */
    @GetMapping("/facets/category")
    public ResponseDTO<Map<String, Long>> getCategoryFacets() {
        Map<String, Long> facets = aggregationService.getCategoryFacets();
        return ResponseDTO.ok(facets);
    }

    /**
     * Get category facets with search filter
     */
    @GetMapping("/facets/category/filtered")
    public ResponseDTO<Map<String, Long>> getCategoryFacetsWithFilter(@RequestParam String keyword) {
        Map<String, Long> facets = aggregationService.getCategoryFacetsWithFilter(keyword);
        return ResponseDTO.ok(facets);
    }

    /**
     * Get price histogram
     */
    @GetMapping("/histogram/price")
    public ResponseDTO<Map<String, Long>> getPriceHistogram(
            @RequestParam(defaultValue = "100") double interval) {
        Map<String, Long> histogram = aggregationService.getPriceHistogram(interval);
        return ResponseDTO.ok(histogram);
    }

    /**
     * Get products by month
     */
    @GetMapping("/histogram/monthly")
    public ResponseDTO<Map<String, Long>> getProductsByMonth() {
        Map<String, Long> histogram = aggregationService.getProductsByMonth();
        return ResponseDTO.ok(histogram);
    }

    /**
     * Get price ranges
     */
    @GetMapping("/ranges/price")
    public ResponseDTO<Map<String, Long>> getPriceRanges() {
        Map<String, Long> ranges = aggregationService.getPriceRanges();
        return ResponseDTO.ok(ranges);
    }

    /**
     * Get price statistics
     */
    @GetMapping("/stats/price")
    public ResponseDTO<PriceStatistics> getPriceStatistics() {
        PriceStatistics stats = aggregationService.getPriceStatistics();
        return ResponseDTO.ok(stats);
    }

    /**
     * Get extended price statistics
     */
    @GetMapping("/stats/price/extended")
    public ResponseDTO<ExtendedPriceStatistics> getExtendedPriceStatistics() {
        ExtendedPriceStatistics stats = aggregationService.getExtendedPriceStatistics();
        return ResponseDTO.ok(stats);
    }

    /**
     * Get price percentiles
     */
    @GetMapping("/percentiles/price")
    public ResponseDTO<Map<String, Double>> getPricePercentiles() {
        Map<String, Double> percentiles = aggregationService.getPricePercentiles();
        return ResponseDTO.ok(percentiles);
    }

    /**
     * Get unique category count
     */
    @GetMapping("/cardinality/category")
    public ResponseDTO<Long> getUniqueCategoryCount() {
        Long count = aggregationService.getUniqueCategoryCount();
        return ResponseDTO.ok(count);
    }

    /**
     * Get category stats with price metrics
     */
    @GetMapping("/stats/category")
    public ResponseDTO<List<CategoryStats>> getCategoryStatsWithPrice() {
        List<CategoryStats> stats = aggregationService.getCategoryStatsWithPrice();
        return ResponseDTO.ok(stats);
    }

    /**
     * Get top N categories
     */
    @GetMapping("/top-categories")
    public ResponseDTO<List<CategoryStats>> getTopCategories(
            @RequestParam(defaultValue = "10") int topN) {
        List<CategoryStats> topCategories = aggregationService.getTopCategories(topN);
        return ResponseDTO.ok(topCategories);
    }

    /**
     * Get product segmentation
     */
    @GetMapping("/segmentation")
    public ResponseDTO<ProductSegmentation> getProductSegmentation() {
        ProductSegmentation segmentation = aggregationService.getProductSegmentation();
        return ResponseDTO.ok(segmentation);
    }

    /**
     * Get moving average (7-day)
     */
    @GetMapping("/trends/moving-average")
    public ResponseDTO<Map<String, Double>> getProductCountMovingAverage() {
        Map<String, Double> movingAvg = aggregationService.getProductCountMovingAverage();
        return ResponseDTO.ok(movingAvg);
    }

    /**
     * Get cumulative product count
     */
    @GetMapping("/trends/cumulative")
    public ResponseDTO<Map<String, Long>> getCumulativeProductCount() {
        Map<String, Long> cumulative = aggregationService.getCumulativeProductCount();
        return ResponseDTO.ok(cumulative);
    }
}
```

---

## Best Practices

1. **Performance:**
   - Use `size: 0` when only aggregations are needed
   - Set appropriate bucket sizes (avoid loading millions of buckets)
   - Use `min_doc_count` to filter empty buckets

2. **Accuracy:**
   - Terms aggregation is approximate for high-cardinality fields
   - Use `shard_size` > `size` for better accuracy
   - Cardinality uses HyperLogLog (approximate but fast)

3. **Nested Aggregations:**
   - Keep nesting depth reasonable (< 4 levels)
   - Consider performance impact of deep nesting
   - Use pipeline aggregations for post-processing

4. **Date Histograms:**
   - Use calendar intervals (month, week, day) for time-based analysis
   - Set `min_doc_count: 0` to include empty time buckets
   - Use timezone parameter for user-specific time zones

5. **Memory Management:**
   - Large aggregations can be memory-intensive
   - Monitor heap usage in production
   - Use field data cache wisely

---

**Next:** [Synchronization Strategies](synchronization-strategies.md)
**Version:** 1.0.0
**Last Updated:** 2026-01-26
