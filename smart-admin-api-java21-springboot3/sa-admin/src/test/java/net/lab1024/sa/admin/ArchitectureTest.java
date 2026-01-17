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
}
