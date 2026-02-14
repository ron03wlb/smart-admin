// ==========================================
// 遊戲平台投注規則引擎範例 (適用於你的領域)
// ==========================================

// === 1. build.gradle 依賴 ===
/*
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.evrete:evrete-core:4.0.3'
    implementation 'org.evrete:evrete-dsl-java:4.0.3'
    
    // 搭配 Vavr 使用
    implementation 'io.vavr:vavr:0.10.4'
    
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
*/

// === 2. 領域模型 ===

package com.example.gaming.domain;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class Player {
    private Long id;
    private String username;
    private String vipLevel;        // BRONZE, SILVER, GOLD, PLATINUM, DIAMOND
    private BigDecimal balance;
    private BigDecimal dailyBetTotal;
    private BigDecimal monthlyBetTotal;
    private int riskScore;          // 0-100
    private boolean selfExcluded;
    private LocalDateTime lastBetTime;
    private int consecutiveLosses;
}

@Data
@Builder
public class BetRequest {
    private Long playerId;
    private String gameType;        // SLOTS, POKER, BLACKJACK, SPORTS
    private BigDecimal amount;
    private BigDecimal odds;
    private LocalDateTime timestamp;
}

@Data
@Builder
public class BetDecision {
    private boolean approved;
    private String rejectReason;
    private BigDecimal adjustedAmount;
    private BigDecimal maxAllowedBet;
    private boolean requiresReview;
    private String riskLevel;       // LOW, MEDIUM, HIGH, CRITICAL
    private java.util.List<String> appliedRules;
    
    public void addAppliedRule(String ruleName) {
        if (appliedRules == null) {
            appliedRules = new java.util.ArrayList<>();
        }
        appliedRules.add(ruleName);
    }
}

// === 3. 規則上下文 (包裝多個物件) ===

package com.example.gaming.rules;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class BetContext {
    private Player player;
    private BetRequest request;
    private BetDecision decision;
    
    public static BetContext create(Player player, BetRequest request) {
        return new BetContext(
            player, 
            request, 
            BetDecision.builder()
                .approved(true)
                .adjustedAmount(request.getAmount())
                .maxAllowedBet(BigDecimal.valueOf(100000))
                .riskLevel("LOW")
                .appliedRules(new java.util.ArrayList<>())
                .build()
        );
    }
}

// === 4. 使用 Annotation 定義投注規則 ===

package com.example.gaming.rules;

import org.evrete.dsl.annotation.Fact;
import org.evrete.dsl.annotation.Rule;
import org.evrete.dsl.annotation.Where;
import java.math.BigDecimal;

public class BettingRules {
    
    // ========== 帳戶狀態檢查 (最高優先級) ==========
    
    @Rule(value = "self-excluded-player", salience = 1000)
    @Where("$ctx.player.selfExcluded == true")
    public void checkSelfExclusion(@Fact("$ctx") BetContext ctx) {
        ctx.getDecision().setApproved(false);
        ctx.getDecision().setRejectReason("玩家已自我排除，無法下注");
        ctx.getDecision().setRiskLevel("CRITICAL");
        ctx.getDecision().addAppliedRule("self-excluded-player");
    }
    
    @Rule(value = "insufficient-balance", salience = 900)
    @Where("$ctx.player.balance.compareTo($ctx.request.amount) < 0")
    public void checkBalance(@Fact("$ctx") BetContext ctx) {
        if (ctx.getDecision().isApproved()) {
            ctx.getDecision().setApproved(false);
            ctx.getDecision().setRejectReason("餘額不足");
            ctx.getDecision().addAppliedRule("insufficient-balance");
        }
    }
    
    // ========== VIP 等級限額規則 ==========
    
    @Rule(value = "bronze-bet-limit", salience = 800)
    @Where("$ctx.player.vipLevel.equals(\"BRONZE\") && $ctx.request.amount.compareTo(new java.math.BigDecimal(\"1000\")) > 0")
    public void bronzeBetLimit(@Fact("$ctx") BetContext ctx) {
        if (ctx.getDecision().isApproved()) {
            BigDecimal maxBet = new BigDecimal("1000");
            ctx.getDecision().setMaxAllowedBet(maxBet);
            ctx.getDecision().setAdjustedAmount(maxBet);
            ctx.getDecision().addAppliedRule("bronze-bet-limit");
        }
    }
    
    @Rule(value = "silver-bet-limit", salience = 800)
    @Where("$ctx.player.vipLevel.equals(\"SILVER\") && $ctx.request.amount.compareTo(new java.math.BigDecimal(\"5000\")) > 0")
    public void silverBetLimit(@Fact("$ctx") BetContext ctx) {
        if (ctx.getDecision().isApproved()) {
            BigDecimal maxBet = new BigDecimal("5000");
            ctx.getDecision().setMaxAllowedBet(maxBet);
            ctx.getDecision().setAdjustedAmount(maxBet);
            ctx.getDecision().addAppliedRule("silver-bet-limit");
        }
    }
    
    @Rule(value = "gold-bet-limit", salience = 800)
    @Where("$ctx.player.vipLevel.equals(\"GOLD\") && $ctx.request.amount.compareTo(new java.math.BigDecimal(\"20000\")) > 0")
    public void goldBetLimit(@Fact("$ctx") BetContext ctx) {
        if (ctx.getDecision().isApproved()) {
            BigDecimal maxBet = new BigDecimal("20000");
            ctx.getDecision().setMaxAllowedBet(maxBet);
            ctx.getDecision().setAdjustedAmount(maxBet);
            ctx.getDecision().addAppliedRule("gold-bet-limit");
        }
    }
    
    // ========== 風險控管規則 ==========
    
    @Rule(value = "high-risk-player", salience = 700)
    @Where("$ctx.player.riskScore > 70")
    public void highRiskPlayer(@Fact("$ctx") BetContext ctx) {
        if (ctx.getDecision().isApproved()) {
            ctx.getDecision().setRequiresReview(true);
            ctx.getDecision().setRiskLevel("HIGH");
            // 高風險玩家投注限額減半
            BigDecimal reduced = ctx.getDecision().getMaxAllowedBet().divide(BigDecimal.valueOf(2));
            ctx.getDecision().setMaxAllowedBet(reduced);
            if (ctx.getDecision().getAdjustedAmount().compareTo(reduced) > 0) {
                ctx.getDecision().setAdjustedAmount(reduced);
            }
            ctx.getDecision().addAppliedRule("high-risk-player");
        }
    }
    
    @Rule(value = "consecutive-loss-protection", salience = 600)
    @Where("$ctx.player.consecutiveLosses >= 5")
    public void consecutiveLossProtection(@Fact("$ctx") BetContext ctx) {
        if (ctx.getDecision().isApproved()) {
            ctx.getDecision().setRequiresReview(true);
            ctx.getDecision().setRiskLevel("MEDIUM");
            ctx.getDecision().addAppliedRule("consecutive-loss-protection");
        }
    }
    
    // ========== 每日/每月限額 ==========
    
    @Rule(value = "daily-limit-exceeded", salience = 500)
    @Where("$ctx.player.dailyBetTotal.add($ctx.request.amount).compareTo(new java.math.BigDecimal(\"50000\")) > 0")
    public void dailyLimitExceeded(@Fact("$ctx") BetContext ctx) {
        if (ctx.getDecision().isApproved()) {
            BigDecimal remaining = new BigDecimal("50000").subtract(ctx.getPlayer().getDailyBetTotal());
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                ctx.getDecision().setApproved(false);
                ctx.getDecision().setRejectReason("已達每日投注上限");
            } else {
                ctx.getDecision().setAdjustedAmount(remaining);
            }
            ctx.getDecision().addAppliedRule("daily-limit-exceeded");
        }
    }
    
    // ========== 遊戲類型特定規則 ==========
    
    @Rule(value = "sports-high-odds-limit", salience = 400)
    @Where("$ctx.request.gameType.equals(\"SPORTS\") && $ctx.request.odds.compareTo(new java.math.BigDecimal(\"10\")) > 0")
    public void sportsHighOddsLimit(@Fact("$ctx") BetContext ctx) {
        if (ctx.getDecision().isApproved()) {
            // 高賠率體育投注限額為 500
            BigDecimal maxBet = new BigDecimal("500");
            if (ctx.getDecision().getAdjustedAmount().compareTo(maxBet) > 0) {
                ctx.getDecision().setAdjustedAmount(maxBet);
            }
            ctx.getDecision().setRequiresReview(true);
            ctx.getDecision().addAppliedRule("sports-high-odds-limit");
        }
    }
}

// === 5. 規則引擎服務 ===

package com.example.gaming.service;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;

@Service
public class BettingRuleService {
    
    private KnowledgeService knowledgeService;
    private Knowledge bettingKnowledge;
    
    @PostConstruct
    public void init() {
        this.knowledgeService = new KnowledgeService();
        
        // 從 Annotation 類別載入規則
        this.bettingKnowledge = knowledgeService
            .newKnowledge("JAVA-CLASS", BettingRules.class);
    }
    
    @PreDestroy
    public void cleanup() {
        if (knowledgeService != null) {
            knowledgeService.shutdown();
        }
    }
    
    /**
     * 評估單一投注請求
     */
    public BetDecision evaluateBet(Player player, BetRequest request) {
        BetContext ctx = BetContext.create(player, request);
        
        bettingKnowledge.newStatelessSession()
            .insert(ctx)
            .fire();
        
        return ctx.getDecision();
    }
    
    /**
     * 批量評估投注請求
     */
    public List<BetDecision> evaluateBets(List<BetContext> contexts) {
        bettingKnowledge.newStatelessSession()
            .insert(contexts.toArray())
            .fire();
        
        return contexts.stream()
            .map(BetContext::getDecision)
            .collect(java.util.stream.Collectors.toList());
    }
}

// === 6. REST Controller ===

package com.example.gaming.controller;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bets")
@RequiredArgsConstructor
public class BettingController {
    
    private final BettingRuleService bettingRuleService;
    private final PlayerService playerService; // 假設已實現
    
    @PostMapping("/evaluate")
    public BetDecision evaluateBet(@RequestBody BetRequest request) {
        Player player = playerService.findById(request.getPlayerId());
        return bettingRuleService.evaluateBet(player, request);
    }
    
    @PostMapping("/place")
    public BetResponse placeBet(@RequestBody BetRequest request) {
        Player player = playerService.findById(request.getPlayerId());
        BetDecision decision = bettingRuleService.evaluateBet(player, request);
        
        if (!decision.isApproved()) {
            return BetResponse.rejected(decision.getRejectReason());
        }
        
        if (decision.isRequiresReview()) {
            // 標記為需要審核
            return BetResponse.pendingReview(decision);
        }
        
        // 執行投注（使用調整後的金額）
        // ... 實際投注邏輯 ...
        
        return BetResponse.success(decision);
    }
}

// === 7. 使用 Fluent API 的替代實現 (不使用 Annotation) ===

package com.example.gaming.service;

import org.evrete.KnowledgeService;
import org.evrete.api.Knowledge;
import java.math.BigDecimal;

public class BettingRuleServiceFluentApi {
    
    private Knowledge buildBettingRules(KnowledgeService service) {
        return service.newKnowledge()
            .builder()
            
            // 自我排除檢查
            .newRule("self-excluded-player")
                .salience(1000)
                .forEach("$ctx", BetContext.class)
                .where("$ctx.player.selfExcluded == true")
                .execute(ctx -> {
                    BetContext betCtx = ctx.get("$ctx");
                    betCtx.getDecision().setApproved(false);
                    betCtx.getDecision().setRejectReason("玩家已自我排除");
                })
            
            // 餘額檢查
            .newRule("insufficient-balance")
                .salience(900)
                .forEach("$ctx", BetContext.class)
                .where("$ctx.player.balance.compareTo($ctx.request.amount) < 0 && $ctx.decision.approved == true")
                .execute(ctx -> {
                    BetContext betCtx = ctx.get("$ctx");
                    betCtx.getDecision().setApproved(false);
                    betCtx.getDecision().setRejectReason("餘額不足");
                })
            
            // 高風險玩家
            .newRule("high-risk-player")
                .salience(700)
                .forEach("$ctx", BetContext.class)
                .where("$ctx.player.riskScore > 70 && $ctx.decision.approved == true")
                .execute(ctx -> {
                    BetContext betCtx = ctx.get("$ctx");
                    betCtx.getDecision().setRequiresReview(true);
                    betCtx.getDecision().setRiskLevel("HIGH");
                    BigDecimal reduced = betCtx.getDecision().getMaxAllowedBet()
                        .divide(BigDecimal.valueOf(2));
                    betCtx.getDecision().setMaxAllowedBet(reduced);
                })
            
            .build();
    }
}

// === 8. 單元測試範例 ===

package com.example.gaming.rules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

class BettingRulesTest {
    
    private BettingRuleService ruleService;
    
    @BeforeEach
    void setUp() {
        ruleService = new BettingRuleService();
        ruleService.init();
    }
    
    @Test
    @DisplayName("自我排除玩家應被拒絕")
    void selfExcludedPlayerShouldBeRejected() {
        Player player = Player.builder()
            .id(1L)
            .username("test_user")
            .vipLevel("GOLD")
            .balance(BigDecimal.valueOf(10000))
            .selfExcluded(true)
            .build();
        
        BetRequest request = BetRequest.builder()
            .playerId(1L)
            .gameType("SLOTS")
            .amount(BigDecimal.valueOf(100))
            .build();
        
        BetDecision decision = ruleService.evaluateBet(player, request);
        
        assertThat(decision.isApproved()).isFalse();
        assertThat(decision.getRejectReason()).contains("自我排除");
    }
    
    @Test
    @DisplayName("餘額不足應被拒絕")
    void insufficientBalanceShouldBeRejected() {
        Player player = Player.builder()
            .id(1L)
            .vipLevel("GOLD")
            .balance(BigDecimal.valueOf(50))
            .selfExcluded(false)
            .build();
        
        BetRequest request = BetRequest.builder()
            .playerId(1L)
            .amount(BigDecimal.valueOf(100))
            .build();
        
        BetDecision decision = ruleService.evaluateBet(player, request);
        
        assertThat(decision.isApproved()).isFalse();
        assertThat(decision.getRejectReason()).contains("餘額不足");
    }
    
    @Test
    @DisplayName("高風險玩家應標記為需審核")
    void highRiskPlayerShouldRequireReview() {
        Player player = Player.builder()
            .id(1L)
            .vipLevel("GOLD")
            .balance(BigDecimal.valueOf(10000))
            .riskScore(85)
            .selfExcluded(false)
            .build();
        
        BetRequest request = BetRequest.builder()
            .playerId(1L)
            .amount(BigDecimal.valueOf(1000))
            .build();
        
        BetDecision decision = ruleService.evaluateBet(player, request);
        
        assertThat(decision.isApproved()).isTrue();
        assertThat(decision.isRequiresReview()).isTrue();
        assertThat(decision.getRiskLevel()).isEqualTo("HIGH");
    }
    
    @Test
    @DisplayName("Bronze 玩家超過限額應被調整")
    void bronzePlayerBetShouldBeAdjusted() {
        Player player = Player.builder()
            .id(1L)
            .vipLevel("BRONZE")
            .balance(BigDecimal.valueOf(5000))
            .selfExcluded(false)
            .riskScore(20)
            .build();
        
        BetRequest request = BetRequest.builder()
            .playerId(1L)
            .amount(BigDecimal.valueOf(3000))
            .build();
        
        BetDecision decision = ruleService.evaluateBet(player, request);
        
        assertThat(decision.isApproved()).isTrue();
        assertThat(decision.getAdjustedAmount())
            .isEqualByComparingTo(BigDecimal.valueOf(1000));
    }
}
