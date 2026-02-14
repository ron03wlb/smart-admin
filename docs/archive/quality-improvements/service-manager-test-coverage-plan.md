# Service/Manager层测试覆盖率提升计划

**创建日期**: 2026-01-30
**目标**: Service 29% → 80%, Manager 24% → 90%
**优先级**: P1 (高优先级)
**预计时间**: 2-4周
**执行人**: Claude Sonnet 4.5

---

## 执行摘要

当前Service层和Manager层测试覆盖率严重不足，需要系统性补充测试以达到质量标准。

### 当前状态

| 层级 | 主要类数 | 测试类数 | 覆盖率 | 目标 |
|------|---------|---------|--------|------|
| **Service** | 24个 | 7个 | **29%** | 80% |
| **Manager** | 17个 | 4个 | **24%** | 90% |

---

## 已有测试文件 (11个)

### Service层测试 (7个) ✅

1. **BrandServiceIntegrationTest** - 集成测试
2. **NoticeServiceN1QueryFixTest** - N+1查询修复专项测试
3. **RoleServiceTest** - 角色服务测试
4. **DepartmentServiceTest** - 部门服务测试
5. **MenuServiceTest** - 菜单服务测试
6. **EmployeeServiceTest** - 员工服务测试 (14方法, 50+用例)
7. **LoginServiceTest** - 登录服务测试 (54个测试)

### Manager层测试 (4个) ✅

1. **RoleMenuManagerTest** - 角色菜单管理测试
2. **DepartmentCacheManagerTest** - 部门缓存管理测试
3. **EmployeeManagerTest** - 员工管理测试
4. **LoginManagerTest** - 登录管理测试

---

## 缺失测试的Service (17个)

### 🔴 P0 - 极高优先级 (6个)

#### 1. GoodsService (商品服务)
**重要性**: ⭐⭐⭐⭐⭐ 核心业务逻辑
**复杂度**: 高
**方法数**: 10+
**关键功能**:
- 商品CRUD
- 商品导入/导出 (已修复N+1查询)
- 分类关联
- 库存管理

**测试重点**:
- 导入/导出性能验证
- 分类关联正确性
- 边界条件 (空数据、大批量)
- N+1查询修复验证

---

#### 2. InvoiceService (发票服务)
**重要性**: ⭐⭐⭐⭐⭐ 财务关键业务
**复杂度**: 高
**方法数**: 8+
**关键功能**:
- 发票CRUD
- 发票审批流程
- 状态变更

**测试重点**:
- 发票状态流转
- 审批权限验证
- 数据一致性

---

#### 3. EnterpriseService (企业服务)
**重要性**: ⭐⭐⭐⭐⭐ 跨模块协调
**复杂度**: 高
**方法数**: 10+
**关键功能**:
- 企业CRUD
- 企业员工关联
- 跨模块调用

**测试重点**:
- 跨模块交互
- 员工关联正确性
- 事务一致性

---

#### 4. NoticeService (公告服务)
**重要性**: ⭐⭐⭐⭐ 消息系统核心
**复杂度**: 高
**方法数**: 12+
**关键功能**:
- 公告CRUD
- 可见范围控制 (员工/部门)
- 已修复N+1查询

**测试重点**:
- 可见范围验证 (已重构validateVisibleRangeIds)
- N+1查询修复验证
- 批量查询正确性

---

#### 5. BankService (银行账户服务)
**重要性**: ⭐⭐⭐⭐ 财务相关
**复杂度**: 中
**方法数**: 6+
**关键功能**:
- 银行账户CRUD
- 账户关联

**测试重点**:
- 账户唯一性验证
- 关联数据完整性

---

#### 6. PositionService (职位服务)
**重要性**: ⭐⭐⭐⭐ 组织管理
**复杂度**: 中
**方法数**: 6+
**关键功能**:
- 职位CRUD
- 职位查询

**测试重点**:
- 职位唯一性
- 分页查询正确性 (已优化UnnecessaryLocalBeforeReturn)

---

### 🟠 P1 - 高优先级 (6个)

#### 7. CategoryService (类目服务)
**重要性**: ⭐⭐⭐⭐ 商品关联
**复杂度**: 中
**方法数**: 8+
**关键功能**:
- 类目CRUD
- 类目树结构
- 已修复UnnecessaryBoxing

**测试重点**:
- 树结构构建
- 父子关系验证
- 排序逻辑

---

#### 8-11. RoleMenuService, RoleDataScopeService, RoleEmployeeService, DataScopeService
**重要性**: ⭐⭐⭐ 权限系统
**复杂度**: 中-高
**关键功能**:
- 角色权限映射
- 数据范围控制
- 员工角色分配

**测试重点**:
- 权限分配正确性
- 数据范围过滤
- 角色变更影响

---

#### 12. CategoryQueryService (类目查询服务)
**重要性**: ⭐⭐⭐ 复杂查询
**复杂度**: 中
**方法数**: 4+

**测试重点**:
- 复杂查询条件
- 性能验证

---

### 🟡 P2 - 中优先级 (5个)

13-17. 其他业务Service (待详细分析)

---

## 缺失测试的Manager (13个)

### 🔴 P0 - 极高优先级 (6个)

#### 1. GoodsManager
**重要性**: ⭐⭐⭐⭐⭐
**关键功能**:
- 事务管理
- 缓存协调

---

#### 2. InvoiceManager
**重要性**: ⭐⭐⭐⭐⭐
**关键功能**:
- 发票事务
- 状态更新

---

#### 3. EnterpriseManager
**重要性**: ⭐⭐⭐⭐⭐
**关键功能**:
- 企业事务
- 员工关联事务

---

#### 4-6. NoticeManager, BankManager, CategoryCacheManager
**重要性**: ⭐⭐⭐⭐
**关键功能**:
- 事务协调
- 缓存管理

---

### 🟠 P1 - 高优先级 (7个)

7-13. 其他Manager (待详细分析)

---

## 测试模式与规范

### SmartAdmin测试模式

**基础结构**:
```java
@DisplayName("XxxService Tests")
class XxxServiceTest extends BaseUnitTest {

  @InjectMocks private XxxService xxxService;

  // DAO Mocks
  @Mock private XxxDao xxxDao;

  // Manager Mocks
  @Mock private XxxManager xxxManager;

  // Service Mocks
  @Mock private YyyService yyyService;

  // Test Fixtures
  private XxxTestFixture fixture;

  @BeforeEach
  void setUp() {
    fixture = new XxxTestFixture();
  }

  @Nested
  @DisplayName("方法组测试")
  class MethodGroupTests {

    @Test
    @DisplayName("正常场景测试")
    void testNormalCase() {
      // Arrange
      // Act
      // Assert
    }

    @Test
    @DisplayName("异常场景测试")
    void testErrorCase() {
      // Arrange
      // Act
      // Assert
    }
  }
}
```

### 测试覆盖标准

**每个Service至少包含**:
1. **CRUD测试** (Create, Read, Update, Delete)
2. **参数验证测试** (空值、无效值、边界值)
3. **业务逻辑测试** (核心业务规则)
4. **异常处理测试** (预期异常、错误码)
5. **边界条件测试** (空列表、大批量数据)

**每个Manager至少包含**:
1. **事务测试** (@Transactional验证)
2. **缓存测试** (@Cacheable验证)
3. **多表操作测试** (数据一致性)

---

## 执行策略

### 阶段1: P0 Service测试补充 (Week 1-2)

**优先级**: GoodsService > InvoiceService > EnterpriseService > NoticeService > BankService > PositionService

**每个Service预计时间**: 2-4小时
**总计**: 6个Service × 3小时 = 18小时 ≈ 2周

**每周目标**:
- Week 1: GoodsService, InvoiceService, EnterpriseService (3个)
- Week 2: NoticeService, BankService, PositionService (3个)

---

### 阶段2: P0 Manager测试补充 (Week 2-3)

**优先级**: GoodsManager > InvoiceManager > EnterpriseManager > NoticeManager > BankManager > CategoryCacheManager

**每个Manager预计时间**: 1-2小时
**总计**: 6个Manager × 1.5小时 = 9小时 ≈ 1周

---

### 阶段3: P1 Service/Manager测试补充 (Week 3-4)

**优先级**: CategoryService > 权限相关Service > 其他Service

**预计时间**: 2周

---

## 验证标准

### 测试质量检查

每个测试必须满足：
1. ✅ 所有public方法都有测试
2. ✅ 至少包含正常场景和异常场景
3. ✅ 使用@DisplayName清晰描述
4. ✅ 使用@Nested组织测试结构
5. ✅ 遵循AAA模式 (Arrange-Act-Assert)
6. ✅ Mock验证完整 (verify关键方法调用)

### 覆盖率验证

```bash
./gradlew :sa-admin:jacocoTestReport
```

**目标**:
- Service层: 80%线覆盖率, 75%分支覆盖率
- Manager层: 90%线覆盖率, 80%分支覆盖率

---

## 风险与依赖

### 风险

1. **时间可能超出预期** (2-4周 → 4-6周)
   - 缓解: 分阶段交付, 优先P0

2. **测试可能发现新Bug**
   - 缓解: 记录Bug, 优先修复Critical级别

3. **依赖Mock可能不准确**
   - 缓解: 添加集成测试验证关键流程

### 依赖

1. 需要理解业务逻辑
2. 需要访问数据库Schema
3. 需要了解权限系统设计

---

## 立即开始的第一个任务

### Task 1: GoodsService测试补充

**优先级**: P0 ⭐⭐⭐⭐⭐
**预计时间**: 3-4小时
**方法数**: 约10个

**测试范围**:
1. ✅ add() - 商品添加
2. ✅ update() - 商品更新
3. ✅ delete() - 商品删除
4. ✅ queryById() - 商品查询
5. ✅ queryPage() - 分页查询
6. ✅ importGoods() - 商品导入 (已修复日志)
7. ✅ getAllGoods() - 商品导出 (已修复N+1查询)
8. ✅ 参数验证测试
9. ✅ 边界条件测试
10. ✅ 异常处理测试

**特别验证**:
- N+1查询修复是否有效 (批量查询分类)
- 导出性能 (10万笔商品内存使用)
- GuardLogStatement修复验证

---

**计划版本**: 1.0
**状态**: ✅ 计划完成，等待执行
**下一步**: 开始Task 1 - GoodsService测试补充
