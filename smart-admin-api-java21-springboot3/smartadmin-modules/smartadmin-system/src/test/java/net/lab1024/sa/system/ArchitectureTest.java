package net.lab1024.sa.system;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameContaining;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
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

@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.AvoidDuplicateLiterals"})
@AnalyzeClasses(
    packages = "net.lab1024.sa.system",
    importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

  private static final String LAYER_CONTROLLER = "Controller";
  private static final String LAYER_SERVICE = "Service";
  private static final String LAYER_MANAGER = "Manager";
  private static final String LAYER_DAO = "Dao";

  // Architecture: Controller -> Service -> Manager -> Dao
  // Service can also directly access Dao
  // Note: Adapter layer excluded (API contract pattern requires cross-layer access)
  @ArchTest
  static final ArchRule layerDependencies =
      layeredArchitecture()
          .consideringAllDependencies()
          // Exclude non-standard layers from strict layering checks
          .ignoreDependency(
              resideInAPackage("..adapter.."), alwaysTrue()) // Adapter (API contract layer)
          .ignoreDependency(resideInAPackage("..interceptor.."), alwaysTrue()) // Interceptor (AOP)
          .ignoreDependency(resideInAPackage("..advice.."), alwaysTrue()) // Advice (AOP)
          .ignoreDependency(resideInAPackage("..config.."), alwaysTrue()) // Config (infrastructure)
          .ignoreDependency(
              resideInAPackage("..datascope.."), alwaysTrue()) // DataScope (dynamic permission AOP)
          .ignoreDependency(simpleNameContaining("MyBatisPlugin"), alwaysTrue()) // MyBatis plugins
          .ignoreDependency(
              simpleNameContaining("DataScopeController"), alwaysTrue()) // Legacy structure
          // Exclude MFA auxiliary services (data access layer helpers)
          .ignoreDependency(
              resideInAPackage("..manager.."),
              simpleNameContaining("MfaBackupCodeService")) // MFA backup code data access helper
          .ignoreDependency(
              resideInAPackage("..manager.."),
              simpleNameContaining(
                  "MfaTrustedDeviceService")) // MFA trusted device data access helper
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
   *   <li>MfaBackupCodeService - 數據訪問層輔助類（封裝備份碼生成/驗證邏輯，包含多步驟 Dao 操作）
   *   <li>MfaTrustedDeviceService - 數據訪問層輔助類（封裝信任設備管理邏輯，包含多步驟 Dao 操作）
   * </ul>
   *
   * <p>規則來源：09-manager-layer.md
   */
  @ArchTest
  static final ArchRule managerShouldNotAccessBusinessService =
      classes()
          .that()
          .resideInAPackage("..manager..")
          .should(
              new ArchCondition<com.tngtech.archunit.core.domain.JavaClass>(
                  "not depend on business Service classes (except MFA auxiliary services)") {
                @Override
                public void check(
                    com.tngtech.archunit.core.domain.JavaClass managerClass,
                    ConditionEvents events) {
                  managerClass
                      .getDirectDependenciesFromSelf()
                      .forEach(
                          dependency -> {
                            com.tngtech.archunit.core.domain.JavaClass targetClass =
                                dependency.getTargetClass();
                            String targetPackageName = targetClass.getPackageName();
                            String targetSimpleName = targetClass.getSimpleName();

                            // Check if target is in Service layer (business service)
                            if (targetPackageName.contains(".service.")) {
                              // Exclude MyBatis-Plus framework classes
                              if (targetPackageName.startsWith("com.baomidou")) {
                                return; // MyBatis-Plus ServiceImpl allowed
                              }

                              // Exclude MFA auxiliary services (data access helpers)
                              if (targetSimpleName.equals("MfaBackupCodeService")
                                  || targetSimpleName.equals("MfaTrustedDeviceService")) {
                                return; // Allowed exception
                              }

                              String message =
                                  String.format(
                                      "Manager class %s depends on Service class %s (Rule: 09-manager-layer.md)",
                                      managerClass.getSimpleName(), targetClass.getSimpleName());
                              events.add(SimpleConditionEvent.violated(dependency, message));
                            }
                          });
                }
              })
          .because("Manager 層禁止調用業務 Service 層（嚴格執行，規則：09-manager-layer.md）");

  /**
   * 【嚴格執行】Manager 層 @Transactional 註解必須使用 rollbackFor = Throwable.class
   *
   * <p>Manager 層所有使用 @Transactional 註解的方法必須明確指定 rollbackFor = Throwable.class， 以確保所有異常（包括 Error 和
   * RuntimeException）都會觸發事務回滾。
   *
   * <p>錯誤示例：
   *
   * <pre>{@code
   * @Transactional  // ❌ 未指定 rollbackFor
   * public void saveEmployee(Employee employee) { }
   *
   * @Transactional(rollbackFor = Exception.class)  // ❌ 無法捕獲 Error
   * public void updateEmployee(Employee employee) { }
   * }</pre>
   *
   * <p>正確示例：
   *
   * <pre>{@code
   * @Transactional(rollbackFor = Throwable.class)  // ✅ 正確
   * public void saveEmployee(Employee employee) { }
   * }</pre>
   *
   * <p>規則來源：09-manager-layer.md
   */
  @ArchTest
  static final ArchRule transactionalMustUseRollbackForThrowable =
      methods()
          .that()
          .areAnnotatedWith(Transactional.class)
          .and()
          .areDeclaredInClassesThat()
          .haveSimpleNameEndingWith("Manager")
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
                            "@Transactional in %s.%s() must use rollbackFor = Throwable.class (Rule: 09-manager-layer.md)",
                            method.getOwner().getSimpleName(), method.getName());
                    events.add(SimpleConditionEvent.violated(method, message));
                  }
                }
              })
          .because(
              "@Transactional in Manager layer must use rollbackFor = Throwable.class (rule: 09-manager-layer.md)");

  // ========== Foundation Package Naming Standards (REMOVED for System module) ==========
  //
  // Note: The following 3 rules were removed from System module ArchitectureTest because they
  // validate old sa-admin package migration patterns that don't apply to the new modular structure.
  //
  // In the new smartadmin-modules structure:
  // - smartadmin-common-captcha, smartadmin-common-cache, etc. are the CORRECT package names
  // - No "foundation.*" migration is needed - these modules ARE the final structure
  //
  // Removed rules (originally from sa-admin/ArchitectureTest.java):
  // 1. systemCodeShouldUseFoundationPackages - checked for old common.* deprecation
  // 2. noNewCodeShouldUseLegacyCommonPackages - checked for foundation.* migration
  // 3. noBridgeClassesInV4 - checked for removed bridge classes in common.core.*
  //
  // These rules remain valid in sa-admin but are not applicable to independent modules.

  // ========== 命名规范 ==========

  /**
   * 【严格执行】POJO 类布尔字段禁止使用 is 前缀
   *
   * <p>布尔字段应直接使用描述性名称（如 deleted, active, enabled）， 禁止使用 is 前缀（如 isDeleted, isActive）
   *
   * <p>错误示例：
   *
   * <pre>
   * public class UserEntity {
   *     private Boolean isDeleted;  // ❌ 禁止
   *     private Boolean isActive;   // ❌ 禁止
   * }
   * </pre>
   *
   * <p>正确示例：
   *
   * <pre>
   * public class UserEntity {
   *     private Boolean deleted;    // ✅ 正确
   *     private Boolean active;     // ✅ 正确
   * }
   * </pre>
   *
   * <p>注意事项：
   *
   * <ul>
   *   <li>此规则仅适用于字段（field），方法名仍可使用 is 前缀（如 isActive()）
   *   <li>适用于 POJO/Entity/DTO/VO 等领域对象
   *   <li>原因：部分序列化框架（如 MyBatis）可能导致 is 字段双重前缀问题
   * </ul>
   *
   * <p>规则来源：01-naming-conventions.md
   */
  @ArchTest
  static final ArchRule noBooleanFieldWithIsPrefix =
      fields()
          .that()
          .areDeclaredInClassesThat()
          .resideInAnyPackage("..domain..", "..entity..", "..dto..", "..vo..")
          .and()
          .haveRawType(Boolean.class)
          .or()
          .haveRawType(boolean.class)
          .should(
              new ArchCondition<com.tngtech.archunit.core.domain.JavaField>(
                  "not start with 'is' prefix") {
                @Override
                public void check(
                    com.tngtech.archunit.core.domain.JavaField field, ConditionEvents events) {
                  String fieldName = field.getName();
                  if (fieldName.startsWith("is")
                      && fieldName.length() > 2
                      && Character.isUpperCase(fieldName.charAt(2))) {
                    String message =
                        String.format(
                            "Boolean field %s.%s starts with 'is' prefix, should use '%s' instead (Rule: 01-naming-conventions.md)",
                            field.getOwner().getSimpleName(),
                            fieldName,
                            Character.toLowerCase(fieldName.charAt(2)) + fieldName.substring(3));
                    events.add(SimpleConditionEvent.violated(field, message));
                  }
                }
              })
          .because("POJO boolean fields must not use 'is' prefix (rule: 01-naming-conventions.md)");

  // ========== 日志记录约束 ==========

  /**
   * 【严格执行】使用 SLF4J 日志门面，禁止直接使用 Log4j/Logback 实现
   *
   * <p>所有业务代码必须使用 org.slf4j.Logger，不能直接依赖日志实现框架
   *
   * <p>正确示例：
   *
   * <pre>
   * import org.slf4j.Logger;
   * import org.slf4j.LoggerFactory;
   *
   * private static final Logger log = LoggerFactory.getLogger(UserService.class);
   * </pre>
   *
   * <p>禁止使用：
   *
   * <ul>
   *   <li>org.apache.log4j.Logger - Log4j 1.x 直接实现
   *   <li>org.apache.logging.log4j.Logger - Log4j 2.x 直接实现
   *   <li>ch.qos.logback.classic.Logger - Logback 直接实现
   * </ul>
   *
   * <p>规则来源：04-exception-logging.md
   */
  @ArchTest
  static final ArchRule useSLF4JFacade =
      noClasses()
          .that()
          .resideInAnyPackage("..controller..", "..service..", "..manager..", "..domain..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.apache.log4j..", "org.apache.logging.log4j..", "ch.qos.logback.classic..")
          .because(
              "Must use SLF4J facade (org.slf4j.Logger), prohibit direct logging implementation (rule: 04-exception-logging.md)");

  // ========== Dependency Injection Constraints (Added: v4.1.0) ==========

  /**
   * 【嚴格執行】禁止 @Resource 字段注入
   *
   * <p>SmartAdmin 項目要求所有依賴注入使用構造函數注入模式，通過 @RequiredArgsConstructor + private final 實現
   *
   * <p>錯誤示例：
   *
   * <pre>
   * &#64;Service
   * public class GoodsService {
   *     &#64;Resource private GoodsDao goodsDao;  // ❌ 禁止
   *     &#64;Resource private GoodsManager goodsManager;  // ❌ 禁止
   * }
   * </pre>
   *
   * <p>正確示例：
   *
   * <pre>
   * &#64;Service
   * &#64;RequiredArgsConstructor
   * public class GoodsService {
   *     private final GoodsDao goodsDao;  // ✅ 正確
   *     private final GoodsManager goodsManager;  // ✅ 正確
   * }
   * </pre>
   *
   * <p>規則來源：foundation/10-architecture-rules.md
   *
   * @since 4.1.0
   */
  @ArchTest
  static final ArchRule noResourceFieldInjection =
      fields()
          .that()
          .areDeclaredInClassesThat()
          .resideInAnyPackage("..controller..", "..service..", "..manager..")
          .should()
          .notBeAnnotatedWith(jakarta.annotation.Resource.class)
          .as(
              "禁止@Resource字段注入，使用@RequiredArgsConstructor構造函數注入（規則：foundation/10-architecture-rules.md）");

  /**
   * 【嚴格執行】Service 層完全禁止依賴 java.util.Optional（包含 private 方法）
   *
   * <p>此規則檢測所有 Optional 依賴，包括：
   *
   * <ul>
   *   <li>Private 方法的返回值
   *   <li>方法參數類型
   *   <li>字段類型
   *   <li>Import 語句
   * </ul>
   *
   * <p>錯誤示例（GoodsService.java）：
   *
   * <pre>
   * import java.util.Optional;  // ❌ 禁止 import
   *
   * private Optional&lt;CategoryEntity&gt; queryCategory(Long id) {  // ❌ 禁止返回 Optional
   *     if (id == null) return Optional.empty();
   *     return Optional.of(entity);
   * }
   * </pre>
   *
   * <p>正確示例：
   *
   * <pre>
   * import io.vavr.control.Option;  // ✅ 使用 Vavr Option
   *
   * private Option&lt;CategoryEntity&gt; queryCategory(Long id) {  // ✅ 返回 Option
   *     return Option.of(id)
   *         .flatMap(categoryId -&gt; Option.of(categoryCacheManager.queryCategory(categoryId)))
   *         .filter(entity -&gt; !entity.getDeletedFlag());
   * }
   * </pre>
   *
   * <p>規則來源：technology/functional/08-vavr-fundamentals.md
   *
   * @since 4.1.0
   */
  @ArchTest
  static final ArchRule noJavaOptionalInServiceStrict =
      noClasses()
          .that()
          .resideInAPackage("..service..")
          .should()
          .dependOnClassesThat()
          .haveFullyQualifiedName("java.util.Optional")
          .as(
              "Service 層完全禁止使用 java.util.Optional（包含 private 方法），必須使用 io.vavr.control.Option（規則：technology/functional/08-vavr-fundamentals.md）");

  /**
   * 【推薦】Manager 事務方法命名約定
   *
   * <p>所有帶 @Transactional 註解的 Manager 方法應以 "Transaction" 結尾，便於識別事務邊界
   *
   * <p>正確示例：
   *
   * <pre>
   * &#64;Service
   * public class GoodsManager {
   *     &#64;Transactional(rollbackFor = Throwable.class)
   *     public void addGoodsTransaction(GoodsAddForm form) {  // ✅ 以 Transaction 結尾
   *         // 事務邏輯
   *     }
   * }
   * </pre>
   *
   * <p>不推薦示例：
   *
   * <pre>
   * &#64;Transactional(rollbackFor = Throwable.class)
   * public void addGoods(GoodsAddForm form) {  // ⚠️ 未以 Transaction 結尾
   *     // 事務邏輯
   * }
   * </pre>
   *
   * <p>規則來源：foundation/09-manager-layer.md
   *
   * @since 4.1.0
   */
  @ArchTest
  static final ArchRule managerTransactionMethodNaming =
      methods()
          .that()
          .areAnnotatedWith(org.springframework.transaction.annotation.Transactional.class)
          .and()
          .areDeclaredInClassesThat()
          .haveSimpleNameEndingWith("Manager")
          .should()
          .haveNameMatching(".*Transaction$")
          .as("Manager 事務方法應以 Transaction 結尾，便於識別事務邊界（規則：foundation/09-manager-layer.md）");
}
