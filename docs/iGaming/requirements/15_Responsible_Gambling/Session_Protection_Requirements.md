# Session Protection Requirements (會話保護業務需求)

> **Canonical Source**: [15-03_Cooling_Off_Period.md](../../source-archive/15_Responsible_Gambling/15-03_Cooling_Off_Period.md), [15-04_Session_Management.md](../../source-archive/15_Responsible_Gambling/15-04_Session_Management.md), [15-05_Reality_Checks.md](../../source-archive/15_Responsible_Gambling/15-05_Reality_Checks.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, Compliance Officers
> **Related Architecture**: [Session_Protection_Architecture.md](../../architecture/15_Responsible_Gambling/Session_Protection_Architecture.md)
> **Last Synced**: 2026-02-09

---

## Business Value

This requirements document delivers strategic value by:
- **Regulatory Compliance**: Implements UKGC mandatory 60-min break after 10 deposits/24h and Germany GlüStV 60-min continuous play with 5-min break requirements
- **Early Intervention**: Establishes cooling-off periods (24h-6 weeks) as preventive tool distinct from interventional self-exclusion
- **Behavioral Awareness**: Defines reality check intervals (15-60 min) displaying session time and P&L with risk identification from player responses
- **Session Integrity**: Preserves game round integrity (wait for completion before popups) while enforcing mandatory breaks and idle timeouts

---

## Acceptance Criteria

- [ ] Cooling-off periods activate immediately for durations 24h-6 weeks
- [ ] Players can view balance, betting history, and request withdrawals during cooling-off
- [ ] Players cannot deposit, game, bet, or claim bonuses during cooling-off
- [ ] Auto-release occurs at cooling-off expiry with notification
- [ ] UK mandatory break triggers after 10 deposits in 24 hours (60-min break)
- [ ] Germany mandatory break triggers after 60 minutes continuous play (5-min break)
- [ ] Session duration limits (15/30/60/120 min) display popup on expiry
- [ ] Idle timeout (30 min default) triggers auto-logout, respecting active game rounds
- [ ] Reality check displays session time and P&L at configured interval
- [ ] Reality check waits for active game round completion before displaying

---

## 1. Overview

Session Protection encompasses three complementary player protection tools: Cooling-Off Periods, Session Time Management, and Reality Checks. Together, these tools help players manage their gaming activity duration and maintain awareness of their gambling behavior.

---

## 2. Business Value

This feature delivers value by:
- **Multi-Jurisdictional Compliance**: Satisfies UKGC, Sweden, and Germany mandatory session management regulations (UKGC 60-min break after 10 deposits, Germany 5-min break per 60-min play, Sweden session limit options), preventing penalties and maintaining operating licenses
- **Player Retention via Awareness**: Reality checks with session P&L display increase player self-awareness (target >20% stop rate), reducing impulsive gambling and supporting long-term player retention by fostering responsible play habits
- **Risk Detection Layer**: Identifies vulnerable behavior patterns (10+ fast reality check continues <5 sec, gaming time >4h, >50% deposit lost immediately) for proactive intervention via affordability assessment or care messages, preventing escalation to problem gambling
- **Flexible Protection Spectrum**: Offers graduated protection from light interventions (15-min reality checks, idle timeout) to strong controls (6-week cooling-off, mandatory breaks), allowing players to self-regulate while meeting regulatory minimums

---

## 3. Acceptance Criteria

- [ ] **Cooling-Off Immediate Activation**: Player-initiated cooling-off (24h-6 weeks) activates immediately with no additional confirmation - all gaming sessions closed, open bets preserved, withdrawals allowed, deposits/betting prohibited
- [ ] **Auto-Release on Expiry**: Cooling-off periods auto-release at expiry time (UTC 23:59:59 for custom durations) with email/push notification sent - account fully restored without player action required
- [ ] **UKGC Mandatory Break (10 Deposits)**: UK players with 10+ deposits within 24 hours trigger 60-minute mandatory break - gaming prohibited during break, countdown timer displayed
- [ ] **Germany Mandatory Break (60-Min Play)**: German players after 60 minutes continuous play trigger 5-minute mandatory break - must complete break before continuing
- [ ] **Reality Check Interval Options**: Players can select reality check intervals (15/30/60 minutes or not set) - popup displays session time + net P&L (clearly show losses as negative) + action options (continue/stop/set limits/view history)
- [ ] **Game Round Preservation**: Reality checks and idle timeout WAIT for active game round to complete before displaying - preserves game integrity, displays immediately after round completion
- [ ] **Auto-Play Pause on Reality Check**: During auto-play mode, reality check pauses auto-play and displays popup - player may resume auto-play after acknowledgment
- [ ] **Risk Behavior Flagging**: System detects and flags high-risk patterns - 10+ consecutive fast reality check continues (<5 sec), gaming time >4h, loss >$1,000 with continue, >50% deposit lost immediately - triggers suggested actions (increase frequency, care message, affordability assessment)
- [ ] **Idle Timeout Auto-Logout**: 30 minutes inactivity (configurable 15-60 min) triggers auto-logout - active game rounds excluded from idle time calculation

---

## 4. Cooling-Off Period (Time-Out)

### 2.1 Purpose and Distinction from Self-Exclusion

| Feature | Cooling-Off | Self-Exclusion |
|---------|------------|----------------|
| **Purpose** | Short-term break | Long-term/permanent ban |
| **Duration** | 24 hours ~ 6 weeks | 6 months ~ Lifetime |
| **Release** | Auto-release | Requires application + cooling-off |
| **Cross-platform** | This platform only | Can sync to Gamstop |
| **Severity** | Preventive | Interventional |

### 2.2 Duration Options

| Duration | Use Case | Auto-Release |
|----------|----------|-------------|
| 24 hours | Same-day cooling | Yes |
| 48 hours | Weekend break | Yes |
| 72 hours | Three-day cooling | Yes |
| 7 days | One-week break | Yes |
| 14 days | Two-week break | Yes |
| 30 days | Monthly break | Yes |
| 6 weeks | Maximum cooling-off | Yes |

### 2.3 Custom Duration Option (Optional)

- Minimum: 24 hours from selection
- Maximum: 6 weeks from selection
- End time: Fixed at selected date UTC 23:59:59

### 2.4 Business Rules

#### Activating Cooling-Off

1. Immediate activation, no additional confirmation needed
2. Preserve account balance
3. Preserve open bets (wait for results)
4. Preserve bonus progress (but cannot accumulate further)
5. Prohibit deposits, gaming, and betting
6. Allow withdrawal requests

#### Allowed Operations During Cooling-Off

- View account balance
- View betting history
- Request withdrawals
- Contact customer support
- View responsible gambling resources

#### Prohibited Operations During Cooling-Off

- Deposits
- Gaming
- Betting
- Claiming bonuses
- Participating in promotions

#### Release Rules

1. Auto-release on expiry, no action required
2. Release notification sent (email/push)
3. Account fully restored

#### Early Release (Optional Feature)

- Some jurisdictions allow early release
- Requires 24-hour confirmation period
- **UK license: early release NOT recommended**

---

## 3. Session Time Management

### 3.1 Regulatory Requirements

| Regulator | Clause | Requirement |
|-----------|--------|-------------|
| **UKGC** | LCCP SR 3.4.2 | Mandatory 60-min break after 10 deposits in 24h |
| **Sweden** | Gambling Act | Must provide session limit options |
| **Germany** | GlüStV 2021 | Mandatory 60-min continuous play then 5-min break |

### 3.2 Session Duration Limit

Players may set maximum duration per gaming session:

| Option | Description | On Expiry |
|--------|------------|-----------|
| 15 minutes | Brief play | Prompt + may continue |
| 30 minutes | Standard session | Prompt + may continue |
| 60 minutes | Extended session | Prompt + may continue |
| 120 minutes | Long session | Prompt + may continue |
| No limit | Unlimited | Per reality check settings |

### 3.3 Mandatory Break Rules

#### UK Rule
- 10+ deposits within 24 hours -> Mandatory 60-minute break
- No gaming during break period

#### Germany Rule
- 60 minutes continuous play -> Mandatory 5-minute break
- Must complete break before continuing

### 3.4 Idle Timeout

- Default: 30 minutes of inactivity
- Configurable: 15-60 minutes
- Active game rounds do not count as idle time
- Auto-logout on timeout

### 3.5 Session Lifecycle

```
Login/Start Game -> Create Session -> Start Timers
       |
       +-- Check session limit settings
       +-- Check mandatory break status
       +-- Initialize activity tracking

During Session:
       +-- Duration timer (player-set limit)
       +-- Activity tracker (idle detection)
       +-- Mandatory break checker (regulatory)

End Session:
       +-- Save session statistics
       +-- Record activity history
       +-- Clean up session resources
```

---

## 4. Reality Checks

### 4.1 Regulatory Requirements

| Regulator | Clause | Default Interval | Display Content |
|-----------|--------|-----------------|-----------------|
| **UKGC** | LCCP SR 3.4.2 | Player-selectable | Time, P&L, options |
| **Sweden** | Gambling Act | 60 minutes | Time, P&L |
| **Germany** | GlüStV 2021 | 60 minutes | Mandatory display |

### 4.2 Interval Options

| Option | Use Case | Description |
|--------|----------|-------------|
| 15 minutes | High-risk players | Frequent reminders |
| 30 minutes | Recommended setting | Moderate reminders |
| 60 minutes | Default setting | Standard interval |
| Not set | Player choice | Some jurisdictions prohibit this |

### 4.3 Display Content

**Must display**:
1. Current session gaming time
2. Current session net profit/loss (clearly show losses as negative)

**Recommended display**:
3. Today's total deposit amount
4. Today's total bet amount
5. Account balance

**Action options**:
- Continue playing
- Stop playing (logout)
- Set deposit limits (quick access)
- View account history

### 4.4 Game Round Interaction Rules

**During active game round**:
- Do not display immediately, wait for round to complete
- Display immediately after round completes
- Preserve game integrity

**During auto-play mode**:
- Pause auto-play
- Display reality check
- Player may resume after acknowledgment

### 4.5 Risk Identification from Reality Check Behavior

| Behavior Pattern | Risk Level | Suggested Action |
|-----------------|-----------|-----------------|
| 10+ consecutive fast continues (<5 sec) | Medium | Increase reality check frequency |
| Loss > $1,000 and continues | Medium | Send care message |
| Gaming time > 4 hours | High | Suggest break |
| >50% deposit lost immediately | High | Trigger affordability assessment |

---

## 5. Notification Requirements

| Event | Channel | Content |
|-------|---------|---------|
| Cooling-off activated | Email + Push | Duration, end time, allowed operations |
| Cooling-off ended | Email + Push | Account restored notification |
| Session duration limit reached | In-app popup | Time played, continue/stop options |
| Mandatory break triggered | In-app popup | Reason, break duration, countdown |
| Reality check triggered | In-app popup | Session stats, P&L, action options |
| Idle timeout | In-app popup | Inactivity notice, session ended |

---

## 6. Effectiveness Metrics

| Metric | Target | Description |
|--------|--------|-------------|
| Cooling-off usage | Monitor trend | Monthly cooling-off activations |
| Session duration limit adoption | > 20% | Players with session limits set |
| Reality check configuration rate | > 20% | Players with reality checks configured |
| Reality check stop rate | > 20% | Players who stop after reality check |
| Mandatory break trigger count | Monitor | Frequency of regulatory mandatory breaks |
| Average session duration | Monitor | Track trends for player welfare |

---

## 7. Testing Scenarios

### Cooling-Off

| Scenario | Expected Result |
|----------|----------------|
| Activate 24h cooling-off | Immediate activation, sessions closed |
| Attempt login during cooling-off | Show cooling-off message with end time |
| Cooling-off expires | Auto-release, notification sent |
| Request early release (if enabled) | 24h confirmation period |

### Session Management

| Scenario | Expected Result |
|----------|----------------|
| Session reaches duration limit | Popup shown, player can continue or stop |
| 10 deposits in 24 hours (UK) | 60-minute mandatory break |
| 30 minutes idle | Auto-logout |
| Game in progress at idle timeout | Wait for round, then timeout |

### Reality Checks

| Scenario | Expected Result |
|----------|----------------|
| 60 minutes elapsed | Reality check popup with stats |
| Player in active game round | Wait for round end, then show popup |
| Auto-play active | Pause auto-play, show popup |
| Player clicks continue quickly 10 times | Flag for risk assessment |

---

## Related Documents

- [Self_Exclusion_Requirements.md](Self_Exclusion_Requirements.md) - Self-exclusion
- [Deposit_Limits_Requirements.md](Deposit_Limits_Requirements.md) - Deposit limits
- [Affordability_Requirements.md](Affordability_Requirements.md) - Affordability assessment
- [Session_Protection_Architecture.md](../../architecture/15_Responsible_Gambling/Session_Protection_Architecture.md) - Technical architecture

---

**Return**: [Responsible Gambling Module](../../source-archive/15_Responsible_Gambling/README.md) | [iGaming Home](../../source-archive/README.md)
