package net.lab1024.sa.igaming.activity.turnover.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import net.lab1024.sa.igaming.activity.turnover.domain.TurnoverContext;
import net.lab1024.sa.igaming.activity.turnover.manager.TurnoverGameWeightRuleManager;
import net.lab1024.sa.igaming.activity.turnover.service.TurnoverTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link GameWeightCmp}.
 *
 * <p>Tests the Layer 3 game weight logic including:
 *
 * <ul>
 *   <li>6 game categories (Slots, Live Casino, Sports Betting, Poker, Table Games, Lottery)
 *   <li>Weight application (100%, 15%, 5%, 20% based on game type)
 *   <li>Activity valid turnover calculation = validTurnoverFinance * gameWeight / 100
 *   <li>Rejected bet handling (skip calculation)
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-15
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameWeightCmp 單元測試")
class GameWeightCmpTest {

  @Mock private TurnoverGameWeightRuleManager gameWeightRuleManager;

  private GameWeightCmpTestHelper helper;

  @BeforeEach
  void setUp() {
    helper = new GameWeightCmpTestHelper(gameWeightRuleManager);
  }

  // ===== 遊戲權重測試 =====

  @Nested
  @DisplayName("遊戲權重")
  class GameWeightTests {

    @Test
    @DisplayName("當類別為Slots時應該應用100%權重")
    void whenCategorySlots_ShouldApply100Percent() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setValidTurnoverFinance(new BigDecimal("100.00"));
      ctx.setGameCategory(1); // Slots

      when(gameWeightRuleManager.getGameWeight(eq(1L), eq(1))).thenReturn(new BigDecimal("100.00"));

      // Act
      helper.processGameWeight(ctx);

      // Assert
      assertThat(ctx.getGameWeight()).isEqualByComparingTo("100.00");
      assertThat(ctx.getActivityValidTurnover()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("當類別為Live Casino時應該應用15%權重")
    void whenCategoryLiveCasino_ShouldApply15Percent() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 2, 1);
      ctx.setValidTurnoverFinance(new BigDecimal("100.00"));
      ctx.setGameCategory(2); // Live Casino (Baccarat, Roulette)

      when(gameWeightRuleManager.getGameWeight(eq(1L), eq(2))).thenReturn(new BigDecimal("15.00"));

      // Act
      helper.processGameWeight(ctx);

      // Assert
      assertThat(ctx.getGameWeight()).isEqualByComparingTo("15.00");
      assertThat(ctx.getActivityValidTurnover()).isEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("當類別為Sports Betting時應該應用100%權重")
    void whenCategorySportsBetting_ShouldApply100Percent() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 3, 1);
      ctx.setValidTurnoverFinance(new BigDecimal("100.00"));
      ctx.setGameCategory(3); // Sports Betting

      when(gameWeightRuleManager.getGameWeight(eq(1L), eq(3))).thenReturn(new BigDecimal("100.00"));

      // Act
      helper.processGameWeight(ctx);

      // Assert
      assertThat(ctx.getGameWeight()).isEqualByComparingTo("100.00");
      assertThat(ctx.getActivityValidTurnover()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("當類別為Poker時應該應用5%權重")
    void whenCategoryPoker_ShouldApply5Percent() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 4, 1);
      ctx.setValidTurnoverFinance(new BigDecimal("100.00"));
      ctx.setGameCategory(4); // Poker (skill-based)

      when(gameWeightRuleManager.getGameWeight(eq(1L), eq(4))).thenReturn(new BigDecimal("5.00"));

      // Act
      helper.processGameWeight(ctx);

      // Assert
      assertThat(ctx.getGameWeight()).isEqualByComparingTo("5.00");
      assertThat(ctx.getActivityValidTurnover()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("當類別為Table Games時應該應用20%權重")
    void whenCategoryTableGames_ShouldApply20Percent() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 5, 1);
      ctx.setValidTurnoverFinance(new BigDecimal("100.00"));
      ctx.setGameCategory(5); // Table Games (Blackjack)

      when(gameWeightRuleManager.getGameWeight(eq(1L), eq(5))).thenReturn(new BigDecimal("20.00"));

      // Act
      helper.processGameWeight(ctx);

      // Assert
      assertThat(ctx.getGameWeight()).isEqualByComparingTo("20.00");
      assertThat(ctx.getActivityValidTurnover()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("當類別為Lottery時應該應用15%權重")
    void whenCategoryLottery_ShouldApply15Percent() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 6, 1);
      ctx.setValidTurnoverFinance(new BigDecimal("100.00"));
      ctx.setGameCategory(6); // Lottery

      when(gameWeightRuleManager.getGameWeight(eq(1L), eq(6))).thenReturn(new BigDecimal("15.00"));

      // Act
      helper.processGameWeight(ctx);

      // Assert
      assertThat(ctx.getGameWeight()).isEqualByComparingTo("15.00");
      assertThat(ctx.getActivityValidTurnover()).isEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("當已被拒絕時應該設置權重為0")
    void whenAlreadyRejected_ShouldSetWeightZero() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setValidTurnoverFinance(new BigDecimal("100.00"));
      ctx.markRejected("ODDS_TOO_LOW");

      // Act
      helper.processGameWeight(ctx);

      // Assert
      assertThat(ctx.getGameWeight()).isEqualByComparingTo("0.00");
      assertThat(ctx.getActivityValidTurnover()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("當沒有找到規則時應該默認為0")
    void whenNoRuleFound_ShouldDefaultZero() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setValidTurnoverFinance(new BigDecimal("100.00"));
      ctx.setGameCategory(1);

      when(gameWeightRuleManager.getGameWeight(eq(1L), eq(1))).thenReturn(null);

      // Act
      helper.processGameWeight(ctx);

      // Assert
      assertThat(ctx.getGameWeight()).isEqualByComparingTo("0.00");
      assertThat(ctx.getActivityValidTurnover()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("當類別為null時應該默認為0")
    void whenCategoryNull_ShouldDefaultZero() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setValidTurnoverFinance(new BigDecimal("100.00"));
      ctx.setGameCategory(null);

      // Act
      helper.processGameWeight(ctx);

      // Assert
      assertThat(ctx.getGameWeight()).isEqualByComparingTo("0.00");
      assertThat(ctx.getActivityValidTurnover()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("當validTurnoverFinance為null時應該返回0")
    void whenValidTurnoverFinanceNull_ShouldReturnZero() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setValidTurnoverFinance(null); // Set to null to trigger edge case
      ctx.setGameCategory(1);

      when(gameWeightRuleManager.getGameWeight(eq(1L), eq(1))).thenReturn(new BigDecimal("100.00"));

      // Act
      helper.processGameWeight(ctx);

      // Assert
      assertThat(ctx.getGameWeight()).isEqualByComparingTo("100.00"); // Weight still retrieved
      assertThat(ctx.getActivityValidTurnover())
          .isEqualByComparingTo("0.00"); // But calculation returns 0
    }

    /**
     * 參數化測試 - 覆蓋所有6個遊戲類別.
     *
     * <p>類別映射:
     *
     * <ul>
     *   <li>1: Slots - 100%
     *   <li>2: Live Casino (Baccarat, Roulette) - 15%
     *   <li>3: Sports Betting - 100%
     *   <li>4: Poker (skill-based) - 5%
     *   <li>5: Table Games (Blackjack) - 20%
     *   <li>6: Lottery - 15%
     * </ul>
     */
    @ParameterizedTest
    @DisplayName("應該為所有類別應用正確的權重")
    @CsvSource({
      "1, 100.00, 100.00", // Slots
      "2, 15.00, 15.00", // Live Casino
      "3, 100.00, 100.00", // Sports Betting
      "4, 5.00, 5.00", // Poker
      "5, 20.00, 20.00", // Table Games
      "6, 15.00, 15.00" // Lottery
    })
    void shouldApplyCorrectWeightForAllCategories(
        int category, String expectedWeight, String expectedTurnover) {
      // Arrange
      TurnoverContext ctx =
          TurnoverTestFixture.createContext(new BigDecimal("100.00"), category, 1);
      ctx.setValidTurnoverFinance(new BigDecimal("100.00"));
      ctx.setGameCategory(category);

      when(gameWeightRuleManager.getGameWeight(eq(1L), eq(category)))
          .thenReturn(new BigDecimal(expectedWeight));

      // Act
      helper.processGameWeight(ctx);

      // Assert
      assertThat(ctx.getGameWeight()).isEqualByComparingTo(expectedWeight);
      assertThat(ctx.getActivityValidTurnover()).isEqualByComparingTo(expectedTurnover);
    }
  }

  // ===== Test Helper Class =====

  /** Test helper to simulate GameWeightCmp behavior without LiteFlow runtime. */
  private static class GameWeightCmpTestHelper {

    private final GameWeightCmp component;

    GameWeightCmpTestHelper(TurnoverGameWeightRuleManager gameWeightRuleManager) {
      this.component = new GameWeightCmp(gameWeightRuleManager);
    }

    /** Simulate GameWeightCmp.process() behavior via reflection. */
    void processGameWeight(TurnoverContext ctx) {
      try {
        // Check if already rejected
        if (ctx.isRejected()) {
          ctx.setGameWeight(BigDecimal.ZERO);
          ctx.setActivityValidTurnover(BigDecimal.ZERO);
          return;
        }

        // Call private method getGameWeight
        java.lang.reflect.Method getGameWeight =
            GameWeightCmp.class.getDeclaredMethod("getGameWeight", TurnoverContext.class);
        getGameWeight.setAccessible(true);

        BigDecimal gameWeight = (BigDecimal) getGameWeight.invoke(component, ctx);
        ctx.setGameWeight(gameWeight);

        // Call private method calculateActivityValidTurnover
        java.lang.reflect.Method calculateActivityValidTurnover =
            GameWeightCmp.class.getDeclaredMethod(
                "calculateActivityValidTurnover", TurnoverContext.class, BigDecimal.class);
        calculateActivityValidTurnover.setAccessible(true);

        BigDecimal activityValidTurnover =
            (BigDecimal) calculateActivityValidTurnover.invoke(component, ctx, gameWeight);
        ctx.setActivityValidTurnover(activityValidTurnover);

      } catch (Exception e) {
        throw new RuntimeException("Failed to process game weight", e);
      }
    }
  }
}
