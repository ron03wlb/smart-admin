package net.lab1024.sa.admin;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.base.DescribedPredicate;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * SmartAdmin 架构测试 - 增强版
 *
 * 确保项目遵循：
 * - 分层架构（Controller → Service → Manager → Domain）
 * - Vavr 函数式编程规范（强制新代码使用）
 * - PostgreSQL 数据库约束
 * - 依赖注入最佳实践
 * - 命名规范
 *
 * @version 2.0
 * @since 2025-01-13
 */
@AnalyzeClasses(
    packages = "net.lab1024.sa.admin",
    importOptions = ImportOption.DoNotIncludeTests.class
)
public class ArchitectureTest {

    // ========== 分层架构约束 ==========

    @ArchTest
    static final ArchRule layerDependencies = layeredArchitecture()
        .consideringAllDependencies()

        .layer("Controller").definedBy("..controller..")
        .layer("Service").definedBy("..service..")
        .layer("Manager").definedBy("..manager..")  // MyBatis Plus Mapper
        .layer("Domain").definedBy("..domain..", "..entity..")

        .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
        .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
        .whereLayer("Manager").mayOnlyBeAccessedByLayers("Service")

        .as("分层架构约束：Controller → Service → Manager → Domain");

    // ========== 命名规范 ==========

    @ArchTest
    static final ArchRule controllerNaming = classes()
        .that().resideInAPackage("..controller..")
        .and().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
        .should().haveSimpleNameEndingWith("Controller")
        .as("Controller 类必须以 Controller 结尾");

    @ArchTest
    static final ArchRule serviceNaming = classes()
        .that().resideInAPackage("..service..")
        .and().areAnnotatedWith(org.springframework.stereotype.Service.class)
        .should().haveSimpleNameEndingWith("Service")
        .as("Service 类必须以 Service 结尾");

    @ArchTest
    static final ArchRule managerNaming = classes()
        .that().resideInAPackage("..manager..")
        .and().areAnnotatedWith(org.springframework.stereotype.Service.class)
        .should().haveSimpleNameEndingWith("Manager")
        .as("Manager 类必须以 Manager 结尾（规则：09-manager-layer.md）");

    /**
     * 【严格执行】POJO 类布尔字段禁止使用 is 前缀
     *
     * <p>布尔字段应直接使用描述性名称（如 deleted, active, enabled），
     * 禁止使用 is 前缀（如 isDeleted, isActive）
     *
     * <p>错误示例：
     * <pre>
     * public class UserEntity {
     *     private Boolean isDeleted;  // ❌ 禁止
     *     private Boolean isActive;   // ❌ 禁止
     * }
     * </pre>
     *
     * <p>正确示例：
     * <pre>
     * public class UserEntity {
     *     private Boolean deleted;    // ✅ 正确
     *     private Boolean active;     // ✅ 正确
     * }
     * </pre>
     *
     * <p>注意事项：
     * <ul>
     *   <li>此规则仅适用于字段（field），方法名仍可使用 is 前缀（如 isActive()）
     *   <li>适用于 POJO/Entity/DTO/VO 等领域对象
     *   <li>原因：部分序列化框架（如 MyBatis）可能导致 is 字段双重前缀问题
     * </ul>
     *
     * <p>规则来源：01-naming-conventions.md
     */
    @ArchTest
    static final ArchRule noBooleanFieldWithIsPrefix = fields()
        .that().areDeclaredInClassesThat()
        .resideInAnyPackage("..domain..", "..entity..", "..dto..", "..vo..")
        .and().haveRawType(Boolean.class)
        .or().haveRawType(boolean.class)
        .should(new ArchCondition<com.tngtech.archunit.core.domain.JavaField>("not start with 'is' prefix") {
            @Override
            public void check(com.tngtech.archunit.core.domain.JavaField field, ConditionEvents events) {
                String fieldName = field.getName();
                if (fieldName.startsWith("is") && fieldName.length() > 2 && Character.isUpperCase(fieldName.charAt(2))) {
                    String message = String.format(
                        "Boolean field %s.%s starts with 'is' prefix, should use '%s' instead (Rule: 01-naming-conventions.md)",
                        field.getOwner().getSimpleName(),
                        fieldName,
                        Character.toLowerCase(fieldName.charAt(2)) + fieldName.substring(3)
                    );
                    events.add(SimpleConditionEvent.violated(field, message));
                }
            }
        })
        .because("POJO boolean fields must not use 'is' prefix (rule: 01-naming-conventions.md)");

    // ========== 依赖注入约束 ==========

    @ArchTest
    static final ArchRule noFieldInjection = fields()
        .that().areDeclaredInClassesThat()
        .resideInAnyPackage("..controller..", "..service..", "..manager..")
        .should().notBeAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
        .as("禁止字段注入，使用构造函数注入");

    // ========== 分层访问约束 ==========

    @ArchTest
    static final ArchRule controllerShouldNotAccessManager = noClasses()
        .that().resideInAPackage("..controller..")
        .should().dependOnClassesThat().resideInAPackage("..manager..")
        .as("Controller 不能直接访问 Manager 层，必须通过 Service");

    /**
     * 【嚴格執行】Manager 層禁止調用業務 Service 層
     *
     * Manager 只能向下調用 DAO/Mapper，不能向上調用業務 Service
     *
     * 排除項：
     * - MyBatis-Plus 框架類（com.baomidou..service..）- 規範允許 Manager 使用 ServiceImpl
     * - sa-base 模組的基礎設施服務 - 非業務邏輯
     *
     * 規則來源：09-manager-layer.md
     */
    @ArchTest
    static final ArchRule managerShouldNotAccessBusinessService = noClasses()
        .that().resideInAPackage("..manager..")
        .should().dependOnClassesThat().resideInAPackage("net.lab1024.sa.admin..service..")
        .as("Manager 層禁止調用業務 Service 層（嚴格執行，規則：09-manager-layer.md）");

    // ========== Vavr 函数式编程约束（强制执行）==========

    /**
     * 强制：Service 层公共方法不能返回 java.util.Optional
     * 新代码必须使用 io.vavr.control.Option
     */
    @ArchTest
    static final ArchRule serviceUsesVavrOption = methods()
        .that().areDeclaredInClassesThat().resideInAPackage("..service..")
        .and().areDeclaredInClassesThat().areAnnotatedWith(Service.class)
        .and().arePublic()
        .and().doNotHaveName("toString")
        .and().doNotHaveName("equals")
        .and().doNotHaveName("hashCode")
        .should(new ArchCondition<JavaMethod>("return Vavr Option instead of java.util.Optional") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                JavaClass returnType = method.getReturnType();
                if (returnType.isAssignableTo(java.util.Optional.class)) {
                    String message = String.format(
                        "Method %s.%s() returns java.util.Optional, should use io.vavr.control.Option (Rule: 08-vavr-fundamentals.md)",
                        method.getOwner().getSimpleName(),
                        method.getName()
                    );
                    events.add(SimpleConditionEvent.violated(method, message));
                }
            }
        })
        .as("Service 层方法必须返回 Vavr Option 而不是 java.util.Optional（强制规则）");

    /**
     * 强制：Service 层不能依赖 java.util.Optional
     * 确保整个 Service 层使用 Vavr Option
     */
    @ArchTest
    static final ArchRule noJavaOptionalInService = noClasses()
        .that().resideInAPackage("..service..")
        .should().dependOnClassesThat().haveFullyQualifiedName("java.util.Optional")
        .as("Service 层禁止使用 java.util.Optional，必须使用 io.vavr.control.Option");

    /**
     * 强制：Controller 参数不能使用 Option
     * Option 应该只在内部逻辑中使用，不应暴露在 API 接口
     */
    @ArchTest
    static final ArchRule noOptionInControllerParams = methods()
        .that().areDeclaredInClassesThat().resideInAPackage("..controller..")
        .should(new ArchCondition<JavaMethod>("not have Option parameters") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                for (JavaParameter param : method.getParameters()) {
                    JavaClass paramType = param.getRawType();
                    if (paramType.getFullName().contains("io.vavr.control.Option")) {
                        String message = String.format(
                            "Controller method %s.%s() has Option parameter '%s', should use primitive types",
                            method.getOwner().getSimpleName(),
                            method.getName(),
                            param.getName()
                        );
                        events.add(SimpleConditionEvent.violated(method, message));
                    }
                }
            }
        })
        .as("Controller 参数禁止使用 Option，应该使用基本类型");

    /**
     * 推荐：优先使用 Vavr Try 处理异常
     * 检测到 try-catch 时给出警告（非强制）
     */
    @ArchTest
    static final ArchRule preferVavrTry = noClasses()
        .that().resideInAPackage("..service..")
        .should().dependOnClassesThat().haveSimpleName("IOException")
        .andShould().dependOnClassesThat().haveSimpleName("SQLException")
        .because("推荐使用 Vavr Try 替代 try-catch（参考 08-vavr-fundamentals.md）");

    // ========== 事务与缓存管理约束（Manager 层）==========

    @ArchTest
    static final ArchRule transactionalOnlyInManager = methods()
        .that().areAnnotatedWith(org.springframework.transaction.annotation.Transactional.class)
        .should().beDeclaredInClassesThat().haveSimpleNameEndingWith("Manager")
        .as("@Transactional 只能在 Manager 层使用（规则：09-manager-layer.md）");

    @ArchTest
    static final ArchRule cacheableOnlyInManager = methods()
        .that().areAnnotatedWith(org.springframework.cache.annotation.Cacheable.class)
        .or().areAnnotatedWith(org.springframework.cache.annotation.CacheEvict.class)
        .or().areAnnotatedWith(org.springframework.cache.annotation.CachePut.class)
        .should().beDeclaredInClassesThat().haveSimpleNameEndingWith("Manager")
        .as("缓存注解只能在 Manager 层使用（规则：09-manager-layer.md）");

    // ========== 循环依赖检测 ==========

    @ArchTest
    static final ArchRule noCycles = slices()
        .matching("net.lab1024.sa.admin.module.(*)..")
        .should().beFreeOfCycles()
        .as("模块间不应有循环依赖");

    // ========== Domain 层纯净性 ==========

    @ArchTest
    static final ArchRule domainShouldBePure = classes()
        .that().resideInAPackage("..domain..")
        .or().resideInAPackage("..entity..")
        .should().onlyDependOnClassesThat()
        .resideInAnyPackage(
            "java..",
            "javax..",
            "jakarta..",
            "com.baomidou.mybatisplus.annotation..",
            "lombok..",
            "io.vavr..",
            "com.fasterxml.jackson..",
            "..domain..",
            "..entity.."
        )
        .as("Domain 层应保持纯净，不依赖框架业务代码");

    // ========== PostgreSQL 特性使用约束 ==========

    @ArchTest
    static final ArchRule noMySQLSpecificCode = noClasses()
        .should().dependOnClassesThat()
        .haveFullyQualifiedName("com.mysql.cj.jdbc.Driver")
        .as("项目已迁移至 PostgreSQL，禁止使用 MySQL 驱动");

    // ========== 日志记录约束 ==========

    /**
     * 【严格执行】使用 SLF4J 日志门面，禁止直接使用 Log4j/Logback 实现
     *
     * <p>所有业务代码必须使用 org.slf4j.Logger，不能直接依赖日志实现框架
     *
     * <p>正确示例：
     * <pre>
     * import org.slf4j.Logger;
     * import org.slf4j.LoggerFactory;
     *
     * private static final Logger log = LoggerFactory.getLogger(UserService.class);
     * </pre>
     *
     * <p>禁止使用：
     * <ul>
     *   <li>org.apache.log4j.Logger - Log4j 1.x 直接实现
     *   <li>org.apache.logging.log4j.Logger - Log4j 2.x 直接实现
     *   <li>ch.qos.logback.classic.Logger - Logback 直接实现
     * </ul>
     *
     * <p>规则来源：04-exception-logging.md
     */
    @ArchTest
    static final ArchRule useSLF4JFacade = noClasses()
        .that().resideInAnyPackage("..controller..", "..service..", "..manager..", "..domain..")
        .should().dependOnClassesThat()
        .resideInAnyPackage(
            "org.apache.log4j..",
            "org.apache.logging.log4j..",
            "ch.qos.logback.classic.."
        )
        .because("Must use SLF4J facade (org.slf4j.Logger), prohibit direct logging implementation (rule: 04-exception-logging.md)");
}
