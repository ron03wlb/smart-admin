# Full-Text Search Integration - Quick Reference

## Document 映射

```java
@Document(indexName = "products")
public class ProductDocument {
    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String productName;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Double)
    private BigDecimal price;

    @CompletionField
    private Completion suggest;
}
```

## 搜索類型

| 類型 | 用途 | 查詢語法 |
|------|------|----------|
| match | 全文搜索 | `match: { field: "keyword" }` |
| fuzzy | 模糊搜索 | `fuzzy: { field: { value: "keywrod" } }` |
| phrase | 短語匹配 | `match_phrase: { field: "exact phrase" }` |
| prefix | 前綴搜索 | `prefix: { field: "key" }` |
| wildcard | 萬用字元 | `wildcard: { field: "*word" }` |

## 搜索服務模板

```java
@Service
@RequiredArgsConstructor
public class ProductSearchService {
    private final ElasticsearchRestTemplate esTemplate;

    public Page<ProductDocument> search(String keyword, Pageable pageable) {
        NativeSearchQuery query = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.multiMatchQuery(keyword)
                .field("productName", 2.0f)
                .field("description")
                .fuzziness(Fuzziness.AUTO))
            .withHighlightBuilder(new HighlightBuilder()
                .field("productName")
                .preTags("<em>")
                .postTags("</em>"))
            .withPageable(pageable)
            .build();

        return esTemplate.searchForPage(query, ProductDocument.class);
    }
}
```

## 聚合查詢

```java
// 按分類統計
TermsAggregationBuilder categoryAgg = AggregationBuilders
    .terms("by_category")
    .field("category")
    .size(10);

// 價格區間統計
RangeAggregationBuilder priceAgg = AggregationBuilders
    .range("by_price")
    .field("price")
    .addRange(0, 100)
    .addRange(100, 500)
    .addRange(500, Double.MAX_VALUE);
```

## 自動完成 (Completion Suggester)

```java
public List<String> autocomplete(String prefix) {
    SuggestBuilder suggest = new SuggestBuilder()
        .addSuggestion("product-suggest",
            SuggestBuilders.completionSuggestion("suggest")
                .prefix(prefix)
                .size(10));

    SearchResponse response = esTemplate.suggest(suggest, ProductDocument.class);
    return extractSuggestions(response);
}
```
