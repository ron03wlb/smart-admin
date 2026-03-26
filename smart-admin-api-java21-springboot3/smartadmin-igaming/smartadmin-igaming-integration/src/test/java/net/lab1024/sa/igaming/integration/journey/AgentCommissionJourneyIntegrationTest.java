package net.lab1024.sa.igaming.integration.journey;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.agent.commission.dao.AgentCommissionConfigDao;
import net.lab1024.sa.igaming.agent.commission.dao.AgentCommissionRecordDao;
import net.lab1024.sa.igaming.agent.commission.dao.AgentCommissionSettlementDao;
import net.lab1024.sa.igaming.agent.commission.dao.AgentRelationshipDao;
import net.lab1024.sa.igaming.agent.commission.domain.entity.AgentCommissionConfigEntity;
import net.lab1024.sa.igaming.agent.commission.domain.entity.AgentCommissionRecordEntity;
import net.lab1024.sa.igaming.agent.commission.domain.entity.AgentCommissionSettlementEntity;
import net.lab1024.sa.igaming.agent.commission.domain.entity.AgentRelationshipEntity;
import net.lab1024.sa.igaming.agent.commission.service.AgentCommissionCalculationService;
import net.lab1024.sa.igaming.agent.commission.service.AgentCommissionQueryService;
import net.lab1024.sa.igaming.agent.commission.service.AgentCommissionSettlementService;
import net.lab1024.sa.igaming.agent.commission.service.AgentRelationshipService;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Agent Commission Journey Integration Test - End-to-end agent commission system tests.
 *
 * <p>Tests the complete agent commission flow from player binding to commission settlement.
 *
 * <p>Test Coverage: - Player-agent binding (single and multi-level) - Commission calculation based
 * on negative profit - Commission settlement with duplicate prevention - Commission freeze/unfreeze
 * - Settlement cancellation
 *
 * @author iGaming Team
 * @since 2026-03-25
 */
@DisplayName("Agent Commission Journey Integration Test")
class AgentCommissionJourneyIntegrationTest extends BaseIntegrationTest {

  @Autowired private AgentRelationshipService agentRelationshipService;

  @Autowired private AgentCommissionCalculationService commissionCalculationService;

  @Autowired private AgentCommissionSettlementService commissionSettlementService;

  @Autowired private AgentCommissionQueryService commissionQueryService;

  @Autowired private PlayerDao playerDao;

  @Autowired private AgentRelationshipDao agentRelationshipDao;

  @Autowired private AgentCommissionConfigDao commissionConfigDao;

  @Autowired private AgentCommissionRecordDao commissionRecordDao;

  @Autowired private AgentCommissionSettlementDao commissionSettlementDao;

  private Long testPlayerId;
  private Long testAgentId;
  private LocalDate testSettlementDate;

  @BeforeEach
  void setUpTestData() {
    testSettlementDate = LocalDate.of(2026, 3, 25);

    // Create test player
    PlayerEntity player = new PlayerEntity();
    player.setUsername("test_player_commission");
    player.setPasswordHash("hash");
    player.setEmailEncrypted("player@test.com");
    player.setStatus(1); // ACTIVE
    player.setVipLevel(1); // VIP1
    player.setDeleted(false);
    playerDao.insert(player);
    testPlayerId = player.getPlayerId();

    // Create test agent
    PlayerEntity agent = new PlayerEntity();
    agent.setUsername("test_agent_commission");
    agent.setPasswordHash("hash");
    agent.setEmailEncrypted("agent@test.com");
    agent.setStatus(1); // ACTIVE
    agent.setVipLevel(2); // VIP2
    agent.setDeleted(false);
    playerDao.insert(agent);
    testAgentId = agent.getPlayerId();

    // Create commission config for testing
    AgentCommissionConfigEntity config =
        AgentCommissionConfigEntity.builder()
            .agentLevel(1)
            .productType("SPORTS")
            .commissionRate(new BigDecimal("0.3000")) // 30%
            .minNegativeProfit(BigDecimal.ZERO)
            .effectiveFrom(LocalDate.of(2026, 1, 1))
            .effectiveTo(null) // Permanent
            .status(1) // ENABLED
            .build();
    commissionConfigDao.insert(config);
  }

  @Test
  @DisplayName("Test 1: Bind player to agent successfully")
  void testBindPlayerToAgent() {
    // When
    ResponseDTO<Void> response =
        agentRelationshipService.bindPlayerToAgent(testPlayerId, testAgentId);

    // Then
    assertThat(response.getOk()).isTrue();

    // Verify binding in database
    AgentRelationshipEntity binding =
        agentRelationshipDao.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                    AgentRelationshipEntity>()
                .eq(AgentRelationshipEntity::getPlayerId, testPlayerId)
                .eq(AgentRelationshipEntity::getAgentId, testAgentId));

    assertThat(binding).isNotNull();
    assertThat(binding.getLevel()).isEqualTo(1); // Direct agent (Level 1)
    assertThat(binding.getStatus()).isEqualTo("ACTIVE");
  }

  @Test
  @DisplayName("Test 2: Prevent duplicate player binding")
  void testPreventDuplicateBinding() {
    // Given: Player already bound to agent
    agentRelationshipService.bindPlayerToAgent(testPlayerId, testAgentId);

    // When: Try to bind again
    ResponseDTO<Void> response =
        agentRelationshipService.bindPlayerToAgent(testPlayerId, testAgentId);

    // Then: Should fail
    assertThat(response.getOk()).isFalse();
    assertThat(response.getMsg()).contains("已綁定代理");
  }

  @Test
  @DisplayName("Test 3: Calculate commission for player losses")
  void testCalculateCommissionForPlayerLosses() {
    // Given: Player bound to agent
    agentRelationshipService.bindPlayerToAgent(testPlayerId, testAgentId);

    // When: Player loses (negative profit = $1000)
    BigDecimal betAmount = new BigDecimal("5000.00");
    BigDecimal payoutAmount = new BigDecimal("4000.00"); // Player lost $1000
    BigDecimal validTurnover = new BigDecimal("5000.00");

    List<AgentCommissionRecordEntity> records =
        commissionCalculationService.calculateCommission(
            testPlayerId, "SPORTS", testSettlementDate, betAmount, payoutAmount, validTurnover);

    // Then: Commission should be calculated
    assertThat(records).hasSize(1);

    AgentCommissionRecordEntity record = records.get(0);
    assertThat(record.getAgentId()).isEqualTo(testAgentId);
    assertThat(record.getTotalNegativeProfit()).isEqualByComparingTo("1000.0000");

    // Commission = 1000 * 0.30 = 300
    assertThat(record.getCommissionAmount()).isEqualByComparingTo("300.0000");

    // Platform cost = 300 * 0.05 = 15
    assertThat(record.getPlatformCost()).isEqualByComparingTo("15.0000");

    // Net commission = 300 - 15 = 285
    assertThat(record.getNetCommission()).isEqualByComparingTo("285.0000");

    assertThat(record.getStatus()).isEqualTo("PENDING");
  }

  @Test
  @DisplayName("Test 4: No commission for player wins")
  void testNoCommissionForPlayerWins() {
    // Given: Player bound to agent
    agentRelationshipService.bindPlayerToAgent(testPlayerId, testAgentId);

    // When: Player wins (negative profit = -$1000)
    BigDecimal betAmount = new BigDecimal("4000.00");
    BigDecimal payoutAmount = new BigDecimal("5000.00"); // Player won $1000
    BigDecimal validTurnover = new BigDecimal("4000.00");

    List<AgentCommissionRecordEntity> records =
        commissionCalculationService.calculateCommission(
            testPlayerId, "SPORTS", testSettlementDate, betAmount, payoutAmount, validTurnover);

    // Then: No commission records created
    assertThat(records).isEmpty();
  }

  @Test
  @DisplayName("Test 5: Execute settlement successfully")
  void testExecuteSettlement() {
    // Given: Commission records exist
    agentRelationshipService.bindPlayerToAgent(testPlayerId, testAgentId);
    commissionCalculationService.calculateCommission(
        testPlayerId,
        "SPORTS",
        testSettlementDate,
        new BigDecimal("5000.00"),
        new BigDecimal("4000.00"),
        new BigDecimal("5000.00"));

    // When: Execute settlement
    ResponseDTO<Long> response = commissionSettlementService.executeSettlement(testSettlementDate);

    // Then: Settlement should succeed
    assertThat(response.getOk()).isTrue();
    Long batchId = response.getData();
    assertThat(batchId).isNotNull();

    // Verify settlement batch
    AgentCommissionSettlementEntity settlement = commissionSettlementDao.selectById(batchId);
    assertThat(settlement).isNotNull();
    assertThat(settlement.getStatus()).isEqualTo("COMPLETED");
    assertThat(settlement.getTotalAgentsCount()).isEqualTo(1);
    assertThat(settlement.getTotalCommissionAmount()).isEqualByComparingTo("285.0000");

    // Verify commission record status updated
    List<AgentCommissionRecordEntity> records =
        commissionQueryService.getCommissionRecordsByStatus(testAgentId, "SETTLED");
    assertThat(records).hasSize(1);
    assertThat(records.get(0).getSettlementBatchId()).isEqualTo(batchId);
  }

  @Test
  @DisplayName("Test 6: Prevent duplicate settlement")
  void testPreventDuplicateSettlement() {
    // Given: Settlement already executed
    agentRelationshipService.bindPlayerToAgent(testPlayerId, testAgentId);
    commissionCalculationService.calculateCommission(
        testPlayerId,
        "SPORTS",
        testSettlementDate,
        new BigDecimal("5000.00"),
        new BigDecimal("4000.00"),
        new BigDecimal("5000.00"));
    commissionSettlementService.executeSettlement(testSettlementDate);

    // When: Try to settle again
    ResponseDTO<Long> response = commissionSettlementService.executeSettlement(testSettlementDate);

    // Then: Should fail
    assertThat(response.getOk()).isFalse();
    assertThat(response.getMsg()).contains("已存在");
  }

  @Test
  @DisplayName("Test 7: Freeze commission record")
  void testFreezeCommissionRecord() {
    // Given: Commission record exists
    agentRelationshipService.bindPlayerToAgent(testPlayerId, testAgentId);
    List<AgentCommissionRecordEntity> records =
        commissionCalculationService.calculateCommission(
            testPlayerId,
            "SPORTS",
            testSettlementDate,
            new BigDecimal("5000.00"),
            new BigDecimal("4000.00"),
            new BigDecimal("5000.00"));
    Long recordId = records.get(0).getRecordId();

    // When: Freeze the record
    ResponseDTO<Void> response =
        commissionSettlementService.freezeCommission(recordId, "Suspected fraud");

    // Then: Record should be frozen
    assertThat(response.getOk()).isTrue();

    AgentCommissionRecordEntity record = commissionRecordDao.selectById(recordId);
    assertThat(record.getStatus()).isEqualTo("FROZEN");
    assertThat(record.getFrozenReason()).isEqualTo("Suspected fraud");
  }

  @Test
  @DisplayName("Test 8: Unfreeze commission record")
  void testUnfreezeCommissionRecord() {
    // Given: Frozen commission record
    agentRelationshipService.bindPlayerToAgent(testPlayerId, testAgentId);
    List<AgentCommissionRecordEntity> records =
        commissionCalculationService.calculateCommission(
            testPlayerId,
            "SPORTS",
            testSettlementDate,
            new BigDecimal("5000.00"),
            new BigDecimal("4000.00"),
            new BigDecimal("5000.00"));
    Long recordId = records.get(0).getRecordId();
    commissionSettlementService.freezeCommission(recordId, "Suspected fraud");

    // When: Unfreeze the record
    ResponseDTO<Void> response = commissionSettlementService.unfreezeCommission(recordId);

    // Then: Record should be unfrozen
    assertThat(response.getOk()).isTrue();

    AgentCommissionRecordEntity record = commissionRecordDao.selectById(recordId);
    assertThat(record.getStatus()).isEqualTo("PENDING");
    assertThat(record.getFrozenReason()).isNull();
  }

  @Test
  @DisplayName("Test 9: Cancel settlement batch")
  void testCancelSettlementBatch() {
    // Given: Completed settlement
    agentRelationshipService.bindPlayerToAgent(testPlayerId, testAgentId);
    commissionCalculationService.calculateCommission(
        testPlayerId,
        "SPORTS",
        testSettlementDate,
        new BigDecimal("5000.00"),
        new BigDecimal("4000.00"),
        new BigDecimal("5000.00"));
    ResponseDTO<Long> settlementResponse =
        commissionSettlementService.executeSettlement(testSettlementDate);
    Long batchId = settlementResponse.getData();

    // When: Cancel settlement
    ResponseDTO<Void> response = commissionSettlementService.cancelSettlement(batchId);

    // Then: Settlement should be cancelled
    assertThat(response.getOk()).isTrue();

    // Verify settlement status
    AgentCommissionSettlementEntity settlement = commissionSettlementDao.selectById(batchId);
    assertThat(settlement.getStatus()).isEqualTo("FAILED");
    assertThat(settlement.getErrorMessage()).contains("cancelled");

    // Verify records reverted to PENDING
    List<AgentCommissionRecordEntity> records =
        commissionQueryService.getCommissionRecordsByStatus(testAgentId, "PENDING");
    assertThat(records).hasSize(1);
    assertThat(records.get(0).getSettlementBatchId()).isNull();
  }

  @Test
  @DisplayName("Test 10: Query commission records by date range")
  void testQueryCommissionRecordsByDateRange() {
    // Given: Multiple commission records
    agentRelationshipService.bindPlayerToAgent(testPlayerId, testAgentId);

    LocalDate date1 = LocalDate.of(2026, 3, 25);
    LocalDate date2 = LocalDate.of(2026, 3, 26);

    commissionCalculationService.calculateCommission(
        testPlayerId,
        "SPORTS",
        date1,
        new BigDecimal("5000.00"),
        new BigDecimal("4000.00"),
        new BigDecimal("5000.00"));

    commissionCalculationService.calculateCommission(
        testPlayerId,
        "SPORTS",
        date2,
        new BigDecimal("3000.00"),
        new BigDecimal("2000.00"),
        new BigDecimal("3000.00"));

    // When: Query by date range
    List<AgentCommissionRecordEntity> records =
        commissionQueryService.getCommissionRecords(
            testAgentId, LocalDate.of(2026, 3, 25), LocalDate.of(2026, 3, 26));

    // Then: Should return both records
    assertThat(records).hasSize(2);
  }

  @Test
  @DisplayName("Test 11: Calculate total commission by date range")
  void testCalculateTotalCommission() {
    // Given: Multiple commission records
    agentRelationshipService.bindPlayerToAgent(testPlayerId, testAgentId);

    commissionCalculationService.calculateCommission(
        testPlayerId,
        "SPORTS",
        LocalDate.of(2026, 3, 25),
        new BigDecimal("5000.00"),
        new BigDecimal("4000.00"),
        new BigDecimal("5000.00"));

    commissionCalculationService.calculateCommission(
        testPlayerId,
        "SPORTS",
        LocalDate.of(2026, 3, 26),
        new BigDecimal("3000.00"),
        new BigDecimal("2000.00"),
        new BigDecimal("3000.00"));

    // When: Calculate total commission
    BigDecimal total =
        commissionQueryService.calculateTotalCommission(
            testAgentId, LocalDate.of(2026, 3, 25), LocalDate.of(2026, 3, 26), null);

    // Then: Total = (1000 * 0.30 - 15) + (1000 * 0.30 - 15) = 285 + 285 = 570
    assertThat(total).isEqualByComparingTo("570.0000");
  }

  @Test
  @DisplayName("Test 12: Prevent settlement of frozen commission")
  void testPreventSettlementOfFrozenCommission() {
    // Given: Frozen commission record
    agentRelationshipService.bindPlayerToAgent(testPlayerId, testAgentId);
    List<AgentCommissionRecordEntity> records =
        commissionCalculationService.calculateCommission(
            testPlayerId,
            "SPORTS",
            testSettlementDate,
            new BigDecimal("5000.00"),
            new BigDecimal("4000.00"),
            new BigDecimal("5000.00"));
    Long recordId = records.get(0).getRecordId();
    commissionSettlementService.freezeCommission(recordId, "Suspected fraud");

    // When: Execute settlement
    ResponseDTO<Long> response = commissionSettlementService.executeSettlement(testSettlementDate);

    // Then: Settlement should succeed but frozen record not included
    assertThat(response.getOk()).isTrue();

    // Verify frozen record still frozen
    AgentCommissionRecordEntity record = commissionRecordDao.selectById(recordId);
    assertThat(record.getStatus()).isEqualTo("FROZEN");
    assertThat(record.getSettlementBatchId()).isNull();
  }
}
