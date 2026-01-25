package net.lab1024.sa.admin;

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

  /**
   * 【嚴格執行】Manager 層 @Transactional 註解必須使用 rollbackFor = Throwable.class
   *
   * <p>Manager 層所有使用 @Transactional 註解的方法必須明確指定 rollbackFor = Throwable.class， 以確保所有異常（包括 Error 和
   * RuntimeException）都會觸發事務回滾。
   *
   * <p>錯誤示例：
   *
   * <pre>
   * @Transactional  // ❌ 未指定 rollbackFor
   * public void saveEmployee(Employee employee) { }
   *
   * @Transactional(rollbackFor = Exception.class)  // ❌ 無法捕獲 Error
   * public void updateEmployee(Employee employee) { }
   * </pre>
   *
   * <p>正確示例：
   *
   * <pre>
   * @Transactional(rollbackFor = Throwable.class)  // ✅ 正確
   * public void saveEmployee(Employee employee) { }
   * </pre>
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
   *   <li>Bridge classes in foundation/core (net.lab1024.sa.common.core.*) - REMOVED in v4.0.0
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
   *   <li>Bridge classes in foundation/core - REMOVED in v4.0.0
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

  // ========== v4.0.0 Breaking Change Validation ==========

  /**
   * v4.0.0+ Bridge classes have been removed
   *
   * <p>In v4.0.0, all bridge classes in net.lab1024.sa.common.core.* were removed as part of the
   * foundation package migration. Code must use net.lab1024.sa.foundation.domain.* packages
   * instead.
   *
   * <p>Removed bridge packages:
   *
   * <ul>
   *   <li>net.lab1024.sa.common.core.code.* (ErrorCode, SystemErrorCode, UserErrorCode, etc.)
   *   <li>net.lab1024.sa.common.core.config.* (CoreAutoConfiguration)
   *   <li>net.lab1024.sa.common.core.constant.* (StringConst, RequestHeaderConst)
   *   <li>net.lab1024.sa.common.core.domain.* (ResponseDTO, PageResult, PageParam, RequestUser)
   *   <li>net.lab1024.sa.common.core.enumeration.* (BaseEnum)
   *   <li>net.lab1024.sa.common.core.exception.* (BusinessException)
   * </ul>
   *
   * <p>Exception: SmartBeanUtil remains in net.lab1024.sa.common.core.util (documented in
   * build.gradle.kts).
   *
   * <p>This rule validates that no code depends on the deleted bridge packages. Any violations
   * indicate code that was not migrated and will fail to compile.
   */
  @ArchTest
  static final ArchRule noBridgeClassesInV4 =
      noClasses()
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "net.lab1024.sa.common.core.code..",
              "net.lab1024.sa.common.core.config..",
              "net.lab1024.sa.common.core.constant..",
              "net.lab1024.sa.common.core.domain..",
              "net.lab1024.sa.common.core.enumeration..",
              "net.lab1024.sa.common.core.exception..")
          .because(
              "v4.0.0 removed all bridge classes. Use net.lab1024.sa.foundation.domain.* instead."
                  + " (Exception: SmartBeanUtil in common.core.util remains)");

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
}
