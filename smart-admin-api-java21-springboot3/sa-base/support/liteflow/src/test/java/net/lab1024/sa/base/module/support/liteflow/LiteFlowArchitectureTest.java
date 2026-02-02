package net.lab1024.sa.base.module.support.liteflow;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

/**
 * LiteFlow 模塊架構測試
 *
 * <p>驗證 LiteFlow 模塊遵循 SmartAdmin 分層架構和編碼規範
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.AvoidDuplicateLiterals"})
@AnalyzeClasses(
    packages = "net.lab1024.sa.base.module.support.liteflow",
    importOptions = ImportOption.DoNotIncludeTests.class)
public class LiteFlowArchitectureTest {

  private static final String LAYER_CONTROLLER = "Controller";
  private static final String LAYER_SERVICE = "Service";
  private static final String LAYER_MANAGER = "Manager";
  private static final String LAYER_DAO = "Dao";

  // ========== 分層架構：Controller → Service → Manager → Dao ==========

  /**
   * 【嚴格執行】LiteFlow 分層架構依賴規則
   *
   * <p>架構：Controller → Service → Manager → Dao → Entity
   *
   * <p>允許：
   *
   * <ul>
   *   <li>Service 可以直接調用 Dao（單表 CRUD 無需事務）
   *   <li>Service 需要事務時必須委派給 Manager
   * </ul>
   */
  @ArchTest
  static final ArchRule liteFlowLayerDependencies =
      layeredArchitecture()
          .consideringAllDependencies()
          // 定義層
          .layer(LAYER_CONTROLLER)
          .definedBy("..liteflow.controller..")
          .layer(LAYER_SERVICE)
          .definedBy("..liteflow.service..")
          .layer(LAYER_MANAGER)
          .definedBy("..liteflow.manager..")
          .layer(LAYER_DAO)
          .definedBy("..liteflow.dao..")
          // 層訪問規則
          .whereLayer(LAYER_CONTROLLER)
          .mayNotBeAccessedByAnyLayer()
          .whereLayer(LAYER_SERVICE)
          .mayOnlyBeAccessedByLayers(LAYER_CONTROLLER)
          .whereLayer(LAYER_MANAGER)
          .mayOnlyBeAccessedByLayers(LAYER_SERVICE)
          .whereLayer(LAYER_DAO)
          .mayOnlyBeAccessedByLayers(LAYER_MANAGER, LAYER_SERVICE)
          .because("LiteFlow 必須遵循分層架構：Controller → Service → Manager → Dao");

  // ========== 命名規範 ==========

  /**
   * 【嚴格執行】Controller 命名規範
   *
   * <p>所有 Controller 類必須以 "Controller" 結尾
   */
  @ArchTest
  static final ArchRule liteFlowControllerNaming =
      classes()
          .that()
          .resideInAPackage("..liteflow.controller..")
          .should()
          .haveSimpleNameEndingWith(LAYER_CONTROLLER)
          .because("LiteFlow Controller 類必須以 Controller 結尾");

  /**
   * 【嚴格執行】Service 命名規範
   *
   * <p>所有 Service 類必須以 "Service" 結尾
   */
  @ArchTest
  static final ArchRule liteFlowServiceNaming =
      classes()
          .that()
          .resideInAPackage("..liteflow.service..")
          .and()
          .areNotInterfaces()
          .should()
          .haveSimpleNameEndingWith(LAYER_SERVICE)
          .because("LiteFlow Service 類必須以 Service 結尾");

  /**
   * 【嚴格執行】Manager 命名規範
   *
   * <p>所有 Manager 類必須以 "Manager" 結尾
   */
  @ArchTest
  static final ArchRule liteFlowManagerNaming =
      classes()
          .that()
          .resideInAPackage("..liteflow.manager..")
          .should()
          .haveSimpleNameEndingWith(LAYER_MANAGER)
          .because("LiteFlow Manager 類必須以 Manager 結尾");

  /**
   * 【嚴格執行】Dao 命名規範
   *
   * <p>所有 Dao 接口必須以 "Dao" 結尾
   */
  @ArchTest
  static final ArchRule liteFlowDaoNaming =
      classes()
          .that()
          .resideInAPackage("..liteflow.dao..")
          .should()
          .haveSimpleNameEndingWith(LAYER_DAO)
          .because("LiteFlow Dao 接口必須以 Dao 結尾");

  // ========== 註解規範 ==========

  /**
   * 【嚴格執行】Service 類必須添加 @Service 註解
   *
   * <p>所有 Service 實現類必須標註 @Service 註解
   */
  @ArchTest
  static final ArchRule liteFlowServiceAnnotation =
      classes()
          .that()
          .resideInAPackage("..liteflow.service..")
          .and()
          .areNotInterfaces()
          .should()
          .beAnnotatedWith(Service.class)
          .because("LiteFlow Service 類必須添加 @Service 註解");

  /**
   * 【嚴格執行】Controller 類必須添加 @RestController 註解
   *
   * <p>所有 Controller 類必須標註 @RestController 註解
   */
  @ArchTest
  static final ArchRule liteFlowControllerAnnotation =
      classes()
          .that()
          .resideInAPackage("..liteflow.controller..")
          .should()
          .beAnnotatedWith(RestController.class)
          .because("LiteFlow Controller 類必須添加 @RestController 註解");

  // ========== Manager 層約束 ==========

  /**
   * 【嚴格執行】Manager 層禁止調用 Service 層
   *
   * <p>Manager 只能向下調用 Dao，不能向上調用 Service
   */
  @ArchTest
  static final ArchRule liteFlowManagerShouldNotAccessService =
      noClasses()
          .that()
          .resideInAPackage("..liteflow.manager..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..liteflow.service..")
          .because("LiteFlow Manager 層禁止調用 Service 層");

  /**
   * 【嚴格執行】Manager 層 @Transactional 必須使用 rollbackFor = Throwable.class
   *
   * <p>所有 Manager 層使用 @Transactional 註解的方法必須指定 rollbackFor = Throwable.class
   */
  @ArchTest
  static final ArchRule liteFlowManagerTransactionalMustUseRollbackForThrowable =
      methods()
          .that()
          .areAnnotatedWith(Transactional.class)
          .and()
          .areDeclaredInClassesThat()
          .resideInAPackage("..liteflow.manager..")
          .should(
              new ArchCondition<com.tngtech.archunit.core.domain.JavaMethod>(
                  "have @Transactional with rollbackFor = Throwable.class") {
                @Override
                public void check(
                    com.tngtech.archunit.core.domain.JavaMethod method, ConditionEvents events) {
                  boolean hasCorrectRollbackFor = false;

                  for (JavaAnnotation<?> annotation : method.getAnnotations()) {
                    if (annotation
                        .getRawType()
                        .isEquivalentTo(
                            org.springframework.transaction.annotation.Transactional.class)) {
                      Object rollbackForValue = annotation.get("rollbackFor").orElse(null);

                      if (rollbackForValue
                          instanceof
                          com.tngtech.archunit.core.domain.JavaClass[] rollbackForClasses) {
                        if (rollbackForClasses.length == 1
                            && rollbackForClasses[0].isEquivalentTo(Throwable.class)) {
                          hasCorrectRollbackFor = true;
                          break;
                        }
                      }
                    }
                  }

                  if (!hasCorrectRollbackFor) {
                    String message =
                        String.format(
                            "@Transactional in %s.%s() must use rollbackFor = Throwable.class",
                            method.getOwner().getSimpleName(), method.getName());
                    events.add(SimpleConditionEvent.violated(method, message));
                  }
                }
              })
          .because("LiteFlow Manager 層 @Transactional 必須使用 rollbackFor = Throwable.class");

  // ========== Service 層約束 ==========

  /**
   * 【嚴格執行】Service 層禁止使用 @Transactional
   *
   * <p>Service 層需要事務時必須委派給 Manager 層，自身不能標註 @Transactional
   */
  @ArchTest
  static final ArchRule liteFlowServiceShouldNotUseTransactional =
      methods()
          .that()
          .areDeclaredInClassesThat()
          .resideInAPackage("..liteflow.service..")
          .should()
          .notBeAnnotatedWith(Transactional.class)
          .because("LiteFlow Service 層禁止使用 @Transactional，需要事務時委派給 Manager 層");

  /**
   * 【嚴格執行】Service 層禁止使用 java.util.Optional
   *
   * <p>Service 層必須使用 io.vavr.control.Option 代替 java.util.Optional
   */
  @ArchTest
  static final ArchRule liteFlowServiceShouldNotUseJavaOptional =
      noClasses()
          .that()
          .resideInAPackage("..liteflow.service..")
          .should()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.util.Optional")
          .because("LiteFlow Service 層必須使用 io.vavr.control.Option 代替 java.util.Optional");

  // ========== LiteFlow 核心組件約束 ==========

  /**
   * 【推薦】LiteFlow 執行器命名規範
   *
   * <p>LiteFlow 相關執行器類應包含 "Executor" 或 "FlowExecutor" 字樣
   */
  @ArchTest
  static final ArchRule liteFlowExecutorNaming =
      classes()
          .that()
          .resideInAPackage("..liteflow.core.executor..")
          .should()
          .haveSimpleNameContaining("Executor")
          .because("LiteFlow 執行器類應包含 Executor 字樣");

  /**
   * 【推薦】LiteFlow 監聽器命名規範
   *
   * <p>LiteFlow 監聽器類應包含 "Listener" 字樣
   */
  @ArchTest
  static final ArchRule liteFlowListenerNaming =
      classes()
          .that()
          .resideInAPackage("..liteflow.core.listener..")
          .should()
          .haveSimpleNameContaining("Listener")
          .because("LiteFlow 監聽器類應包含 Listener 字樣");

  /**
   * 【推薦】LiteFlow 數據源命名規範
   *
   * <p>LiteFlow 數據源類應包含 "DataSource" 字樣
   */
  @ArchTest
  static final ArchRule liteFlowDataSourceNaming =
      classes()
          .that()
          .resideInAPackage("..liteflow.core.datasource..")
          .should()
          .haveSimpleNameContaining("DataSource")
          .because("LiteFlow 數據源類應包含 DataSource 字樣");
}
