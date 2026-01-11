package net.lab1024.sa.admin;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.base.DescribedPredicate;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * SmartAdmin 架构测试
 *
 * 确保项目遵循：
 * - 分层架构（Controller → Service → Manager → Domain）
 * - Vavr 函数式编程规范
 * - PostgreSQL 数据库约束
 * - 依赖注入最佳实践
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

    // ========== Vavr 函数式编程约束 ==========

    @ArchTest
    static final ArchRule preferVavrOptionOverOptional = noMethods()
        .that().areDeclaredInClassesThat().resideInAnyPackage("..service..")
        .and().arePublic()
        .should().haveRawReturnType(java.util.Optional.class)
        .as("Service 层推荐使用 Vavr Option 代替 Java Optional");

    // ========== 事务管理约束 ==========

    @ArchTest
    static final ArchRule transactionalOnlyInService = methods()
        .that().areAnnotatedWith(org.springframework.transaction.annotation.Transactional.class)
        .should().beDeclaredInClassesThat().resideInAPackage("..service..")
        .as("@Transactional 只应在 Service 层使用");

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
}
