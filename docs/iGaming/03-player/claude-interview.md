# Interview Transcript — 03-player (Player Domain)

## Q1: Implementation Scope

**Q**: The spec covers 6 modules (player-lifecycle, player-kyc, player-vip, player-segmentation, responsible-gambling, customer-service). Given the existing codebase already has some player/selfexclusion/vip code, what is the implementation scope for this plan?

**A**: Extend existing code to cover full spec. Build on existing PlayerEntity, PlayerStateManager, SelfExclusion services — add missing features (RFM, affordability, customer service, etc.)

## Q2: KYC Provider Integration

**Q**: For KYC OCR integration, which integration approach do you prefer?

**A**: Abstract adapter pattern (provider-agnostic). Define KycVerificationAdapter interface, implement one provider initially, swap later.

## Q3: AI Chatbot Depth

**Q**: For the AI Chatbot (Ch12 §7.3), what is the expected implementation depth?

**A**: Full AI integration design. Design the chatbot architecture with LLM/NLP integration, intent routing, and degradation strategy.

## Q4: State Machine Migration

**Q**: How should the player state machine expansion handle existing data? (Current: 6 states, Spec needs: 7+ states including REGISTERED, DORMANT, COOLING_OFF)

**A**: Clean break with new migration. New migration redefines states, assumes no legacy data constraints.

## Q5: Self-Exclusion External Integration

**Q**: What level of GAMSTOP / external self-exclusion integration is expected?

**A**: Full adapter interfaces + GAMSTOP stub. Define SelfExclusionGateway interface with adapters, implement GAMSTOP adapter with configurable mock/real mode.

## Q6: Problem Gambling Detection Architecture

**Q**: For problem gambling detection (6 real-time indicators), what computation architecture do you prefer?

**A**: Hybrid — critical indicators real-time + others batch. Loss chasing and rapid deposits via Kafka events; daily patterns (night betting) via scheduled batch.

## Q7: 360-Degree Player View Architecture

**Q**: For the 360-degree player view (Ch12), how should cross-domain data aggregation work?

**A**: API Gateway aggregation. 360° view endpoint calls other domain services at query time, aggregates response.

## Q8: Affordability Assessment Tiers

**Q**: Should we implement the spec's full 3-tier design or start with the currently-live tiers?

**A**: Full 3-tier as per spec. Implement all 3 tiers (Basic/Enhanced/Full) including Open Banking adapter for Full tier.

## Q9: Scale & Constraints

**Q**: What are the non-functional requirements or constraints?

**A**: Large scale (100K-1M+ players). High-volume operator, need to consider hot paths carefully.
