package net.lab1024.sa.app;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * iGaming module architecture boundary tests
 *
 * <p>Enforces module boundaries defined in 00-module-structure.md:
 *
 * <ol>
 *   <li>Layered architecture (Controller -> Service -> Manager -> Dao)
 *   <li>No cyclic dependencies between iGaming modules
 *   <li>Wallet / Risk isolation (Kafka events only)
 *   <li>Cross-module service isolation (use API contract layer)
 * </ol>
 *
 * <p>Note: iGaming modules are currently skeleton (package-info.java only). Rules use {@code
 * optionalLayer()} and {@code allowEmptyShould(true)} to pass when modules have no classes yet.
 * Once business code is added, these rules will actively enforce boundaries.
 *
 * @since 4.1.0
 */
@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.AvoidDuplicateLiterals"})
@AnalyzeClasses(
    packages = "net.lab1024.sa.igaming",
    importOptions = ImportOption.DoNotIncludeTests.class)
public class IgamingArchitectureTest {

  // ========== Section 1: Layered Architecture ==========
  // Uses optionalLayer() to allow empty layers during skeleton phase.

  @ArchTest
  static final ArchRule igamingLayeredArchitecture =
      layeredArchitecture()
          .consideringAllDependencies()
          .optionalLayer("Controller")
          .definedBy("..igaming..controller..")
          .optionalLayer("Job")
          .definedBy("..igaming..job..")
          .optionalLayer("Adapter")
          .definedBy("..igaming..adapter..")
          .optionalLayer("Service")
          .definedBy("..igaming..service..")
          .optionalLayer("Manager")
          .definedBy("..igaming..manager..")
          .optionalLayer("Dao")
          .definedBy("..igaming..dao..")
          .whereLayer("Controller")
          .mayNotBeAccessedByAnyLayer()
          .whereLayer("Job")
          .mayNotBeAccessedByAnyLayer()
          .whereLayer("Adapter")
          .mayOnlyBeAccessedByLayers("Service", "Manager")
          .whereLayer("Service")
          .mayOnlyBeAccessedByLayers("Controller", "Job")
          .whereLayer("Manager")
          .mayOnlyBeAccessedByLayers("Service", "Manager", "Job")
          .whereLayer("Dao")
          .mayOnlyBeAccessedByLayers("Manager", "Service", "Adapter")
          .as(
              "iGaming modules must follow SmartAdmin layered architecture"
                  + " (Controller/Job -> Service -> Manager -> Dao, Adapter -> Dao)");

  // ========== Section 2: No Cyclic Dependencies ==========

  @ArchTest
  static final ArchRule igamingModulesNoCyclicDependencies =
      slices()
          .matching("net.lab1024.sa.igaming.(*)..")
          .should()
          .beFreeOfCycles()
          .as("iGaming modules must be free of cyclic dependencies");

  // ========== Section 3: Wallet / Risk Isolation (Kafka-only) ==========
  // Risk module communicates with other modules exclusively through Kafka events.
  // No direct Java imports between Risk and Wallet/Game/Player are allowed.
  // allowEmptyShould(true): modules are currently empty skeletons.

  @ArchTest
  static final ArchRule walletMustNotDependOnRisk =
      noClasses()
          .that()
          .resideInAPackage("..igaming.wallet..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..igaming.risk..")
          .allowEmptyShould(true)
          .as(
              "Wallet module must not depend on Risk module"
                  + " (communicate via Kafka events only, see 00-module-structure.md)");

  @ArchTest
  static final ArchRule riskMustNotDependOnWallet =
      noClasses()
          .that()
          .resideInAPackage("..igaming.risk..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..igaming.wallet..")
          .allowEmptyShould(true)
          .as(
              "Risk module must not depend on Wallet module"
                  + " (communicate via Kafka events only, see 00-module-structure.md)");

  @ArchTest
  static final ArchRule riskMustNotDependOnGame =
      noClasses()
          .that()
          .resideInAPackage("..igaming.risk..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..igaming.game..")
          .allowEmptyShould(true)
          .as(
              "Risk module must not depend on Game module"
                  + " (consume Kafka events only, see 00-module-structure.md)");

  @ArchTest
  static final ArchRule riskMustNotDependOnPlayer =
      noClasses()
          .that()
          .resideInAPackage("..igaming.risk..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..igaming.player..")
          .allowEmptyShould(true)
          .as(
              "Risk module must not depend on Player module"
                  + " (consume Kafka events only, see 00-module-structure.md)");

  // ========== Section 4: Cross-Module Service Isolation ==========
  // Business modules cannot directly depend on each other's internal packages.
  // Allowed dependencies:
  //   - igaming-common (net.lab1024.sa.igaming.common) - shared layer
  //   - smartadmin-api-igaming (net.lab1024.sa.api.igaming) - contract layer
  // API contract package (net.lab1024.sa.api.igaming) does NOT match "..igaming.X.." patterns.
  // allowEmptyShould(true): modules are currently empty skeletons.

  @ArchTest
  static final ArchRule walletModuleIsolation =
      noClasses()
          .that()
          .resideInAPackage("..igaming.wallet..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..igaming.player..", "..igaming.game..", "..igaming.activity..", "..igaming.agent..")
          .allowEmptyShould(true)
          .as(
              "Wallet module must not directly depend on other iGaming business modules"
                  + " (use smartadmin-api-igaming contract interfaces)");

  @ArchTest
  static final ArchRule playerModuleIsolation =
      noClasses()
          .that()
          .resideInAPackage("..igaming.player..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..igaming.game..", "..igaming.activity..", "..igaming.risk..", "..igaming.agent..")
          .allowEmptyShould(true)
          .as(
              "Player module must not directly depend on other iGaming business modules"
                  + " (except Wallet via API contract)");

  @ArchTest
  static final ArchRule gameModuleIsolation =
      noClasses()
          .that()
          .resideInAPackage("..igaming.game..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..igaming.player..", "..igaming.activity..", "..igaming.risk..", "..igaming.agent..")
          .allowEmptyShould(true)
          .as(
              "Game module must not directly depend on other iGaming business modules"
                  + " (except Wallet via API contract)");

  @ArchTest
  static final ArchRule activityModuleIsolation =
      noClasses()
          .that()
          .resideInAPackage("..igaming.activity..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..igaming.risk..", "..igaming.agent..")
          .allowEmptyShould(true)
          .as(
              "Activity module must not depend on Risk/Agent modules"
                  + " (Activity is an aggregation module, allowed: Wallet, Game, Player)");

  @ArchTest
  static final ArchRule riskModuleIsolation =
      noClasses()
          .that()
          .resideInAPackage("..igaming.risk..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..igaming.wallet..",
              "..igaming.player..",
              "..igaming.game..",
              "..igaming.activity..",
              "..igaming.agent..")
          .allowEmptyShould(true)
          .as(
              "Risk module must be fully isolated from all other iGaming business modules"
                  + " (Kafka events only, see 00-module-structure.md)");

  @ArchTest
  static final ArchRule agentModuleIsolation =
      noClasses()
          .that()
          .resideInAPackage("..igaming.agent..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "..igaming.wallet..", "..igaming.game..", "..igaming.activity..", "..igaming.risk..")
          .allowEmptyShould(true)
          .as(
              "Agent module must not directly depend on other iGaming business modules"
                  + " (except Player via API contract)");
}
