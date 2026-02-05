# Test Fixture Generator - Examples

## 範例 1: 完整 Employee Fixture

```java
package net.lab1024.sa.system.employee.support;

import net.lab1024.sa.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.system.employee.domain.form.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EmployeeTestFixture {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    // ==================== Entity ====================

    public static EmployeeEntity createEntity() {
        int id = COUNTER.incrementAndGet();
        EmployeeEntity entity = new EmployeeEntity();
        entity.setFirstName("Test" + id);
        entity.setLastName("Employee" + id);
        entity.setEmail("test" + id + "@example.com");
        entity.setPhone("1380000" + String.format("%04d", id % 10000));
        entity.setDepartmentId(1L);
        entity.setDeleted(false);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        return entity;
    }

    public static EmployeeEntity createEntityWithDepartment(Long departmentId) {
        EmployeeEntity entity = createEntity();
        entity.setDepartmentId(departmentId);
        return entity;
    }

    public static List<EmployeeEntity> createEntities(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> createEntity())
            .collect(Collectors.toList());
    }

    // ==================== Forms ====================

    public static EmployeeAddForm createAddForm() {
        int id = COUNTER.incrementAndGet();
        EmployeeAddForm form = new EmployeeAddForm();
        form.setFirstName("Test" + id);
        form.setLastName("Employee" + id);
        form.setEmail("test" + id + "@example.com");
        form.setPhone("1380000" + String.format("%04d", id % 10000));
        form.setDepartmentId(1L);
        return form;
    }

    public static EmployeeUpdateForm createUpdateForm(Long employeeId) {
        EmployeeUpdateForm form = new EmployeeUpdateForm();
        form.setEmployeeId(employeeId);
        form.setFirstName("Updated");
        form.setLastName("Name");
        form.setEmail("updated@example.com");
        return form;
    }

    public static EmployeeQueryForm createQueryForm() {
        EmployeeQueryForm form = new EmployeeQueryForm();
        form.setPageNum(1);
        form.setPageSize(10);
        return form;
    }

    public static EmployeeQueryForm createQueryForm(String keyword) {
        EmployeeQueryForm form = createQueryForm();
        form.setKeyword(keyword);
        return form;
    }
}
```

## 範例 2: 複雜關聯 Fixture

```java
public class OrderTestFixture {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    public static OrderEntity createEntity() {
        int id = COUNTER.incrementAndGet();
        OrderEntity order = new OrderEntity();
        order.setOrderNo("ORD" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + String.format("%06d", id));
        order.setCustomerId(1L);
        order.setTotalAmount(BigDecimal.valueOf(100.00 + id));
        order.setStatus(OrderStatus.PENDING);
        order.setCreateTime(LocalDateTime.now());
        return order;
    }

    public static OrderEntity createEntityWithItems(int itemCount) {
        OrderEntity order = createEntity();
        List<OrderItemEntity> items = OrderItemTestFixture.createItems(order.getOrderId(), itemCount);
        order.setItems(items);
        BigDecimal total = items.stream()
            .map(OrderItemEntity::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalAmount(total);
        return order;
    }
}

public class OrderItemTestFixture {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    public static OrderItemEntity createItem(Long orderId) {
        int id = COUNTER.incrementAndGet();
        OrderItemEntity item = new OrderItemEntity();
        item.setOrderId(orderId);
        item.setProductId((long) id);
        item.setProductName("Product" + id);
        item.setQuantity(id % 5 + 1);
        item.setUnitPrice(BigDecimal.valueOf(10.00 + id));
        item.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        return item;
    }

    public static List<OrderItemEntity> createItems(Long orderId, int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> createItem(orderId))
            .collect(Collectors.toList());
    }
}
```

## 範例 3: 使用 Fixture 的測試

```java
class OrderServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderDao orderDao;

    @Test
    @Transactional
    void createOrder_WithItems_CalculatesTotalCorrectly() {
        // Given - 使用 Fixture 創建帶商品的訂單
        OrderAddForm form = OrderTestFixture.createAddFormWithItems(3);

        // When
        ResponseDTO<Long> result = orderService.createOrder(form);

        // Then
        assertThat(result.getOk()).isTrue();

        OrderEntity saved = orderDao.selectById(result.getData());
        assertThat(saved.getItems()).hasSize(3);
        assertThat(saved.getTotalAmount()).isPositive();
    }

    @Test
    @Transactional
    void queryOrders_WithPagination_Works() {
        // Given - 批量創建訂單
        List<OrderEntity> orders = OrderTestFixture.createEntities(15);
        orders.forEach(orderDao::insert);

        OrderQueryForm form = OrderTestFixture.createQueryForm();

        // When
        ResponseDTO<PageResult<OrderVO>> result = orderService.query(form);

        // Then
        assertThat(result.getOk()).isTrue();
        assertThat(result.getData().getList()).hasSize(10);
        assertThat(result.getData().getTotal()).isEqualTo(15);
    }
}
```
