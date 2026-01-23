package net.lab1024.sa.admin;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameContaining;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.AvoidDuplicateLiterals"})
@AnalyzeClasses(
    packages = "net.lab1024.sa.admin",
    importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

  private static final String LAYER_CONTROLLER = "Controller";
  private static final String LAYER_SERVICE = "Service";
  private static final String LAYER_MANAGER = "Manager";
  private static final String LAYER_DAO = "Dao";

  // Architecture: Controller -> Service -> Manager -> Dao
  // Service can also directly access Dao
  @ArchTest
  static final ArchRule layerDependencies =
      layeredArchitecture()
          .consideringAllDependencies()
          // Exclude Interceptor and MyBatisPlugin from architecture checks
          .ignoreDependency(resideInAPackage("..interceptor.."), alwaysTrue())
          .ignoreDependency(simpleNameContaining("MyBatisPlugin"), alwaysTrue())
          // Exclude DataScopeController (not in controller package - legacy structure)
          .ignoreDependency(simpleNameContaining("DataScopeController"), alwaysTrue())
          // Define layers
          .layer(LAYER_CONTROLLER)
          .definedBy("..controller..")
          .layer(LAYER_SERVICE)
          .definedBy("..service..")
          .layer(LAYER_MANAGER)
          .definedBy("..manager..")
          .layer(LAYER_DAO)
          .definedBy("..dao..")
          // Layer access rules
          .whereLayer(LAYER_CONTROLLER)
          .mayNotBeAccessedByAnyLayer()
          .whereLayer(LAYER_SERVICE)
          .mayOnlyBeAccessedByLayers(LAYER_CONTROLLER)
          .whereLayer(LAYER_MANAGER)
          .mayOnlyBeAccessedByLayers(LAYER_SERVICE)
          .whereLayer(LAYER_DAO)
          .mayOnlyBeAccessedByLayers(LAYER_MANAGER, LAYER_SERVICE);

  @ArchTest
  static final ArchRule controllerNaming =
      classes()
          .that()
          .resideInAPackage("..controller..")
          .should()
          .haveSimpleNameEndingWith(LAYER_CONTROLLER)
          .orShould()
          .haveSimpleNameEndingWith("Interceptor");

  @ArchTest
  static final ArchRule serviceAnnotation =
      classes()
          .that()
          .resideInAPackage("..service..")
          .and()
          .areNotInterfaces()
          .should()
          .beAnnotatedWith(Service.class);

  @ArchTest
  static final ArchRule controllerAnnotation =
      classes()
          .that()
          .resideInAPackage("..controller..")
          .should()
          .beAnnotatedWith(RestController.class);

  // ========== Manager 層調用約束（嚴格執行）==========

  /**
   * 【嚴格執行】Manager 層禁止調用業務 Service 層
   *
   * <p>Manager 只能向下調用 DAO/Mapper，不能向上調用業務 Service
   *
   * <p>排除項：
   *
   * <ul>
   *   <li>MyBatis-Plus 框架類（com.baomidou..service..）- 規範允許 Manager 使用 ServiceImpl
   *   <li>sa-base 模組的基礎設施服務 - 非業務邏輯
   * </ul>
   *
   * <p>規則來源：09-manager-layer.md
   */
  @ArchTest
  static final ArchRule managerShouldNotAccessBusinessService =
      noClasses()
          .that()
          .resideInAPackage("..manager..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("net.lab1024.sa.admin..service..")
          .because("Manager 層禁止調用業務 Service 層（嚴格執行，規則：09-manager-layer.md）");

  // ========== Foundation Package Naming Standards (Added: foundation migration) ==========

  /**
   * Admin code should only use migrated foundation packages (not legacy common.* packages)
   *
   * <p>As part of the foundation package naming standardization initiative, sa-admin code should
   * use the new foundation.* packages instead of deprecated common.* packages.
   *
   * <p>Allowed exceptions:
   *
   * <ul>
   *   <li>Bridge classes in foundation/core (net.lab1024.sa.common.core.*) - scheduled for removal
   *       in v4.0.0
   *   <li>SmartBeanUtil in common.core.util (intentionally not migrated - documented in
   *       build.gradle.kts)
   * </ul>
   *
   * <p>Migrated modules: api-encrypt, cache, captcha, data-masking, mq, redis-lock, repeat-submit,
   * security-protect
   */
  @ArchTest
  static final ArchRule adminCodeShouldUseFoundationPackages =
      noClasses()
          .that()
          .resideInAPackage("net.lab1024.sa.admin..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "net.lab1024.sa.common.apiencrypt..",
              "net.lab1024.sa.common.cache..",
              "net.lab1024.sa.common.captcha..",
              "net.lab1024.sa.common.datamasking..",
              "net.lab1024.sa.common.mq..",
              "net.lab1024.sa.common.redislock..",
              "net.lab1024.sa.common.repeatsubmit..",
              "net.lab1024.sa.common.securityprotect..")
          .because(
              "Admin code should use foundation.* packages instead of deprecated common.* packages"
                  + " (migration complete)");

  /**
   * No new code should use legacy common.* package naming
   *
   * <p>Legacy net.lab1024.sa.common.* packages are deprecated. Use net.lab1024.sa.foundation.*
   * instead.
   *
   * <p>Exceptions:
   *
   * <ul>
   *   <li>Bridge classes in foundation/core (scheduled for removal in v4.0.0)
   *   <li>SmartBeanUtil (intentionally not migrated - documented in build.gradle.kts)
   * </ul>
   */
  @ArchTest
  static final ArchRule noNewCodeShouldUseLegacyCommonPackages =
      noClasses()
          .that()
          .resideOutsideOfPackage("net.lab1024.sa.common.core..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "net.lab1024.sa.common.apiencrypt..",
              "net.lab1024.sa.common.cache..",
              "net.lab1024.sa.common.captcha..",
              "net.lab1024.sa.common.datamasking..",
              "net.lab1024.sa.common.mq..",
              "net.lab1024.sa.common.redislock..",
              "net.lab1024.sa.common.repeatsubmit..",
              "net.lab1024.sa.common.securityprotect..")
          .because(
              "Legacy common.* package naming is deprecated, use foundation.* instead (migration"
                  + " complete)");
}
