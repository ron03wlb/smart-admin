# Full-Text Search Integration - Examples

## 範例 1: 商品搜索服務

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchService {
    private final ElasticsearchRestTemplate esTemplate;
    private final ProductDao productDao;

    public PageResult<ProductSearchVO> search(ProductSearchForm form) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        // 關鍵字搜索
        if (StringUtils.hasText(form.getKeyword())) {
            boolQuery.must(QueryBuilders.multiMatchQuery(form.getKeyword())
                .field("productName", 3.0f)
                .field("description")
                .field("brand")
                .fuzziness(Fuzziness.AUTO)
                .minimumShouldMatch("75%"));
        }

        // 分類過濾
        if (form.getCategoryId() != null) {
            boolQuery.filter(QueryBuilders.termQuery("categoryId", form.getCategoryId()));
        }

        // 價格區間
        if (form.getMinPrice() != null || form.getMaxPrice() != null) {
            RangeQueryBuilder priceRange = QueryBuilders.rangeQuery("price");
            if (form.getMinPrice() != null) priceRange.gte(form.getMinPrice());
            if (form.getMaxPrice() != null) priceRange.lte(form.getMaxPrice());
            boolQuery.filter(priceRange);
        }

        NativeSearchQuery query = new NativeSearchQueryBuilder()
            .withQuery(boolQuery)
            .withHighlightBuilder(new HighlightBuilder()
                .field("productName")
                .field("description")
                .preTags("<mark>")
                .postTags("</mark>"))
            .withSort(SortBuilders.scoreSort())
            .withPageable(PageRequest.of(form.getPageNum() - 1, form.getPageSize()))
            .build();

        SearchHits<ProductDocument> hits = esTemplate.search(query, ProductDocument.class);
        return convertToPageResult(hits, form.getPageNum(), form.getPageSize());
    }
}
```

---

## 範例 2: 資料同步 (雙寫策略)

```java
@Service
@RequiredArgsConstructor
public class ProductSyncService {
    private final ProductRepository productRepository; // ES Repository
    private final ProductDao productDao;

    @Transactional(rollbackFor = Throwable.class)
    public void saveProduct(ProductEntity entity) {
        // 1. 保存到 PostgreSQL
        productDao.insert(entity);

        // 2. 同步到 Elasticsearch
        ProductDocument document = convertToDocument(entity);
        productRepository.save(document);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void deleteProduct(Long id) {
        productDao.deleteById(id);
        productRepository.deleteById(id);
    }

    // 全量同步 (定時任務)
    public void fullSync() {
        int page = 0;
        int size = 1000;

        while (true) {
            List<ProductEntity> products = productDao.selectPage(page, size);
            if (products.isEmpty()) break;

            List<ProductDocument> documents = products.stream()
                .map(this::convertToDocument)
                .collect(Collectors.toList());

            productRepository.saveAll(documents);
            page++;
        }
    }
}
```

---

## 範例 3: 銷售聚合分析

```java
public SalesAnalyticsVO analyzeByCategory() {
    NativeSearchQuery query = new NativeSearchQueryBuilder()
        .addAggregation(AggregationBuilders.terms("by_category")
            .field("category")
            .subAggregation(AggregationBuilders.sum("total_sales").field("salesAmount"))
            .subAggregation(AggregationBuilders.avg("avg_price").field("price")))
        .build();

    SearchHits<OrderDocument> hits = esTemplate.search(query, OrderDocument.class);
    Aggregations aggs = hits.getAggregations();

    Terms categoryTerms = aggs.get("by_category");
    List<CategorySalesVO> categorySales = categoryTerms.getBuckets().stream()
        .map(bucket -> {
            Sum totalSales = bucket.getAggregations().get("total_sales");
            Avg avgPrice = bucket.getAggregations().get("avg_price");
            return new CategorySalesVO(
                bucket.getKeyAsString(),
                bucket.getDocCount(),
                totalSales.getValue(),
                avgPrice.getValue()
            );
        })
        .collect(Collectors.toList());

    return new SalesAnalyticsVO(categorySales);
}
```
