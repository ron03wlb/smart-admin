# iGaming Industry Glossary

> **Canonical Source**: [source/00_Foundation/guides/00-07_Industry_Terminology.md](../../source-archive/00_Foundation/guides/00-07_Industry_Terminology.md)
> **Audience**: Executives, Product Managers, Compliance Officers
> **Related Doc**: None (pure business)
> **Last Synced**: 2026-02-08

---

## Overview

The iGaming industry exceeded **$660 billion GGR** globally in 2025. From platform technology to payment processing, compliance to data analytics, the industry has developed a complex and specialized terminology framework. This glossary covers 8 core domains with 200+ professional terms, providing practitioners with a comprehensive industry knowledge reference.

---

## 1. Game Type Terminology

### 1.1 Sports Betting

| Term | Chinese | Definition |
|------|---------|------------|
| **Asian Handicap** | 亞洲讓球盤 | Betting system using half-goal handicaps (0.25, 0.5, 0.75) to eliminate draws. Originated from Indonesian "hang cheng" betting, named by journalist Joe Saumarez Smith in 1998. Dominates Asia-Pacific football betting, offering only two outcomes (win/lose). Industry standard requires split-bet functionality (e.g., -0.25 line splits stake between 0 and -0.5). |
| **In-Play Betting** | 滾球投注 | Real-time betting during live events with dynamically updated odds. Accounts for over **70%** of total wagering in some markets. Key challenges: latency control, collusion detection, rapid settlement. |
| **Parlay / Accumulator** | 過關投注 | Combining multiple independent selections into a single bet; all selections must win to profit. Industry standard caps at **10-15 selections**. |
| **Bookmaker Margin (Vigorish)** | 莊家水位 | Commission built into odds ensuring profitability regardless of outcome. Industry norm: **2-10%**; more competitive markets offer lower margins. |

### 1.2 Poker

| Term | Chinese | Definition |
|------|---------|------------|
| **Rake** | 抽水 | Commission taken from each pot or tournament entry fee. Cash tables: **2.5-10%** of pot (capped); tournaments: fixed fee model (e.g., $10+$1). "No flop, no drop" rule applies. |
| **Rakeback** | 返水 | Returning a percentage of paid rake as cash or bonus. Ranges from **5%** (new players) to **45%** (top VIP). Key incentive for high-frequency players. |

### 1.3 Slots

| Term | Chinese | Definition |
|------|---------|------------|
| **Volatility** | 波動性 | Game risk and payout distribution characteristic. Low volatility: frequent small wins (20-30% hit rate); high volatility: rare large wins (10-15% hit rate). |
| **Progressive Jackpot** | 累積獎金 | Portion of each bet feeds a prize pool until won. Multiple casinos can link to the same pool; prizes can reach millions. These games typically have lower base RTP. |
| **Megaways** | 萬種賠付方式 | Big Time Gaming licensed dynamic reel system with variable paylines per spin, up to **117,649 ways** to win. Uses cascading mechanic. |

### 1.4 Live Casino

| Term | Chinese | Definition |
|------|---------|------------|
| **Live Dealer** | 真人荷官 | Games operated by real dealers via HD video streaming, 24/7. Evolution Gaming is the market leader. Supports "One Wallet" and "Fund Transfer" integration modes. |
| **Baccarat Squeeze** | 百家樂揭牌 | Traditional slow card reveal ceremony using **15+ HD cameras** (Evolution Gaming). Control Squeeze allows virtual player-initiated reveal. Deep cultural roots in Macau VIP rooms. |
| **Roads** | 路單 | Chart system displaying baccarat historical results, including Big Road, Big Eye Boy, Small Road, Cockroach Road, and Bead Plate. Used to identify patterns for betting decisions. |

---

## 2. Technology Architecture Terminology

### 2.1 Platform Types

| Term | Chinese | Definition |
|------|---------|------------|
| **White-Label Platform** | 白牌平台 (包網) | Pre-built complete platform operated under the operator's own brand. Supplier provides infrastructure, games, licensing, payments, and sometimes customer service. Typical launch time: **4-6 weeks**. Revenue share: **20-50% of GGR**; setup fee: $15,000-$50,000. |
| **Turnkey Solution** | 交鑰匙解決方案 | Complete pre-built platform where the operator must obtain their own license. Greater customization flexibility than white-label. Build time: **2-3 months**. |
| **Multi-Tenant Architecture** | 多租戶架構 | Single software instance serving multiple operator brands ("tenants"). Each tenant has independent branding, configuration, and user experience while sharing underlying infrastructure. |

### 2.2 Wallet Integration

| Term | Chinese | Definition |
|------|---------|------------|
| **Seamless Wallet** | 無縫錢包 | Single unified player balance usable across all games and verticals (casino, sports, poker) in real time. Preferred modern architecture providing the **best player experience**. |
| **Transfer Wallet** | 轉帳錢包 | Players must manually or programmatically transfer funds from a main wallet to GP-specific wallets before playing. Simpler to implement than seamless wallet. |

### 2.3 Game Aggregation

| Term | Chinese | Definition |
|------|---------|------------|
| **Game Aggregator** | 遊戲聚合平台 | Platform integrating thousands of games from multiple studios into a single API entry point. Hub88, SOFTSWISS, Groove offer **15,000+ games** from **100+ providers** via a single API. Integration typically completes in **3-7 days**. |
| **Unified API** | 統一API | Standardized interface integrating multiple game providers into a single access point. Zenith's OneAPI provides 10,000+ games from 150+ providers. RESTful architecture, JSON responses, webhook support. |

### 2.4 Backend Systems

| Term | Chinese | Definition |
|------|---------|------------|
| **PAM (Player Account Management)** | 玩家帳戶管理系統 | Core backend system managing user accounts, wallets, payments, registration, sessions, and KYC/AML controls. EveryMatrix GamMatrix PAM achieves **99%+ uptime**. |
| **Back Office** | 後台管理系統 | Administrative interface providing financial management, marketing operations, customer support, business intelligence, player management, and compliance reporting tools. |

### 2.5 Performance Metrics

| Term | Chinese | Definition |
|------|---------|------------|
| **Latency** | 延遲 | Round-trip time in milliseconds. Live casino requires **<50ms latency**. Google research: load time increase from 1s to 3s raises bounce rate by **32%**. |
| **Uptime SLA** | 運行時間服務等級協議 | Contractual availability guarantee. Leading platforms guarantee **99.95-99.99% uptime**. 99.99% ("four nines") means only **52.56 minutes** downtime per year. Large enterprises lose $300,000-$1M per hour of downtime. |

---

## 3. Payment Terminology

### 3.1 Payment Infrastructure

| Term | Chinese | Definition |
|------|---------|------------|
| **PSP (Payment Service Provider)** | 支付服務提供商 | Third-party enabling merchants to accept cards, bank transfers, e-wallets, and alternative payments. iGaming-specialized PSPs (Paysafe, Nuvei, Corefy) understand high-risk merchant requirements. Must maintain **PCI DSS compliance**. |
| **Payment Gateway** | 支付閘道 | Technical solution securely transmitting transaction data between merchant website and acquiring bank. Encrypts cardholder data, validates transactions, returns authorization results. |
| **Payment Orchestration** | 支付編排 | Technology layer integrating multiple PSPs, acquirers, and payment methods via single API. Platforms like Praxis Tech, Corefy connect **200+ payment providers**, enabling smart routing and auto-failover for **85%+ acceptance rates**. |
| **Smart Routing** | 智能路由 | Automatically directing each transaction to the optimal processor based on rules and real-time analytics. Considers approval rates, processing costs, geography, currency, and provider status. Modern engines analyze **100+ data points** per transaction. |

### 3.2 Payment Methods

| Term | Chinese | Definition |
|------|---------|------------|
| **3D Secure (3DS)** | 3D安全驗證 | Authentication protocol adding verification for card-not-present transactions. "3D" refers to acquirer, issuer, and interoperability domains. Provides **liability shift** (fraud chargebacks become issuer responsibility), reducing fraud-related chargebacks by **70%+**. Brand names: Visa Secure, Mastercard Identity Check. |
| **Chargeback** | 退款 (拒付) | Cardholder disputes transaction with issuer, causing reversal and merchant deduction. Fees: **$15-$70+** per chargeback. Card network thresholds: **0.9-1%**; exceeding triggers fines or service termination. |
| **A2A Payments** | 帳戶對帳戶支付 | Direct bank-to-bank transfers without intermediary card networks. Benefits: lower costs (no interchange fees), reduced chargeback risk (irreversible transactions), built-in KYC via bank account verification. A2A processes **17%** of European e-commerce payments. |
| **Open Banking** | 開放銀行 | Regulatory framework enabling banks to share financial data with authorized third parties via API. In iGaming: instant deposits (Pay by Bank), instant KYC verification, affordability checks for responsible gambling. Key providers: TrueLayer, Yaspa, Trustly. |

### 3.3 Cryptocurrency Payments

| Term | Chinese | Definition |
|------|---------|------------|
| **Stablecoin Payments** | 穩定幣支付 | Payments using fiat-pegged cryptocurrencies (typically USD). USDT (Tether) and USDC (Circle) most common. Eliminates volatility risk for both operators and players. **60%+** of leading operators' crypto volume is via stablecoins. |
| **Crypto Payment Processors** | 加密貨幣支付處理器 | Specialized providers enabling crypto acceptance with optional auto-conversion to fiat. NOWPayments (350+ coins, 0.5% fee), B2BINPAY (80+ coins, EU-regulated), CryptoProcessing/CoinsPaid (iGaming specialist, auto EUR conversion). |

### 3.4 Payment Operations

| Term | Chinese | Definition |
|------|---------|------------|
| **Rolling Reserve** | 滾動儲備金 | Processor retains **5-15%** of transaction revenue for **90-180 days** to cover potential chargebacks, refunds, or merchant default. Standard for high-risk gambling merchants. Significant cash flow impact. |
| **Payment Success Rate** | 支付成功率 | Percentage of successfully authorized and completed transactions. Industry benchmark: **85%+** with optimized setup. Each 1% improvement directly impacts revenue. Optimization: multi-PSP, local acquiring, smart routing, retry logic. |

---

## 4. Risk Control & Compliance Terminology

### 4.1 Player Verification

| Term | Chinese | Definition |
|------|---------|------------|
| **KYC (Know Your Customer)** | 認識你的客戶 | Comprehensive identity verification before allowing real-money gaming. Includes: identity verification, document verification (passport, driver's license, ID card), age verification (18+ or 21+), address verification (utility bills, bank statements). Automated systems: iDenfy, Sumsub, Persona. **UKGC requires pre-deposit verification in UK market**. |
| **EDD (Enhanced Due Diligence)** | 加強盡職調查 | Additional verification for high-risk customers (PEPs, high-net-worth individuals, unusual transaction patterns). Includes: source of funds verification, source of wealth documentation, enhanced monitoring, executive-level account approval, more frequent reviews. |

### 4.2 Anti-Money Laundering

| Term | Chinese | Definition |
|------|---------|------------|
| **AML (Anti-Money Laundering)** | 反洗錢 | Regulatory framework and procedures preventing criminals from disguising illegal proceeds as legitimate income through gambling. Includes: CDD, EDD, transaction monitoring, suspicious activity reporting, staff training, record keeping (**5+ years**). 2023 gambling AML fines exceeded **$475 million**. |
| **SOF (Source of Funds)** | 資金來源 | Verification that customer funds originate from legitimate sources. Triggered when deposits exceed thresholds or for high-risk customers. Required documents: bank statements, pay slips, tax returns, investment records, property sale documents. |
| **SAR (Suspicious Activity Report)** | 可疑活動報告 | Formal report to Financial Intelligence Unit (FIU) when transactions appear unusual or inconsistent with normal gambling behavior. Reportable scenarios: large cash deposits inconsistent with player profile, unusual betting patterns (deposit, minimal play, withdrawal), coordinated multi-account activity, rapid inter-account fund transfers. |

### 4.3 Responsible Gambling Tools

| Term | Chinese | Definition |
|------|---------|------------|
| **Self-Exclusion** | 自我排除 | Voluntary program banning individuals from gambling platforms for specified periods (6 months to lifetime). **GamStop** (UK) mandatory since March 2020; **83% of users** report reduced or stopped gambling. Winnings during exclusion may be voided. |
| **Cooling-Off Period** | 冷靜期 | Shorter, typically reversible gambling pause (24 hours to several weeks). Prevents impulsive decisions to immediately resume gambling. |
| **Deposit/Loss/Session Limits** | 存款/損失/時間限額 | Player self-imposed controls. Deposit limits set daily/weekly/monthly maximums; loss limits cap losses per time period; session limits restrict play duration. **Limit increases require 24-72 hour delay**; decreases take effect immediately. Operators offering customizable limits see **31% fewer** spending complaints. |
| **Reality Checks** | 現實提醒 | Periodic pop-up notifications reminding players of elapsed time, amount spent, and current session status. Typically set at 15, 30, or 60-minute intervals. |

### 4.4 Game Fairness

| Term | Chinese | Definition |
|------|---------|------------|
| **RTP (Return to Player)** | 玩家回報率 | Theoretical percentage of total wagered amount returned to players over the long term. Example: 96% RTP means a theoretical return of $96 per $100 wagered. Calculated over millions of game rounds, not single sessions. Set during game development; cannot be changed post-release. Regulatory minimums: Malta MGA **85%**, UK UKGC (slots since June 2023) **99.9%**. Online casinos typically range **95-98%**. |
| **RNG (Random Number Generator)** | 隨機數字生成器 | Software algorithm ensuring completely random and unpredictable game outcomes. Types: TRNG (physical events, rare in iGaming due to cost) and PRNG (statistically indistinguishable randomness). Must be certified by accredited testing labs with periodic audits. |
| **House Edge** | 莊家優勢 | Casino's mathematical advantage expressed as percentage retained per bet. Formula: House Edge = 100% - RTP. By game: Blackjack ~0.5-2% (optimal strategy), Baccarat (banker) ~1.06%, European Roulette ~2.7%, Slots 2-15%. |

### 4.5 Testing & Certification Bodies

| Body | Established | Coverage | Key Services |
|------|------------|----------|-------------|
| **eCOGRA** | 2003 (UK) | 25+ jurisdictions | RNG testing, RTP verification, dispute resolution. ISO/IEC 17025:2017 accredited. |
| **GLI (Gaming Laboratories International)** | 30+ years | 480 jurisdictions | Full-spectrum testing and certification. |
| **BMM Testlabs** | 1981 | Global | Oldest testing organization in the industry. |

---

## 5. Regulatory Authority Terminology

### 5.1 Tier-1 Regulators

| Regulator | Established | Processing Time | Initial Cost | Tax Rate | Reputation |
|-----------|------------|----------------|-------------|----------|------------|
| **UKGC** (UK Gambling Commission) | 2007 | ~16 weeks | 370 GBP application fee | 21% of gross profit | Gold Standard |
| **MGA** (Malta Gaming Authority) | 2001 | 3-6 months | 25,000+ EUR | Per license type | Tier-1 |
| **Curacao Gaming Authority** | 1996 | Varies | ANG 120,000/year | 2% net profit (to 2026) | Reforming |

**UKGC Details**: Governs all online and land-based gambling in England, Wales, Scotland. License types: operating, personal management (mandatory for key personnel), premises. Annual fee based on gross gambling yield (GGY). Mandatory GamStop participation since March 2020. Credit card deposits banned since April 2020. Fines since 2020 exceed **100 million GBP**.

**MGA Details**: First EU member state to regulate online gambling. Controls approximately **10% of global virtual casinos**, 305+ active licensees, contributing **12%+ of Malta's GDP**. License types: Type 1 (RNG games), Type 2 (fixed-odds betting), Type 3 (P2P games), Type 4 (skill games), B2B Key Gaming Supply License. Must be EU/EEA company; servers in Malta.

**Curacao Details**: New LOK (National Gaming Ordinance) framework implemented in 2024, eliminating sub-license system. No VAT. Restricted markets: US, Australia, Netherlands, France, Curacao. New framework aims to meet MGA/UKGC standards.

### 5.2 Other Notable Regulators

| Regulator | Established | Processing Time | Initial Cost | Tax Rate | Reputation |
|-----------|------------|----------------|-------------|----------|------------|
| **Gibraltar** | 2005 | 3-6 months | 85-100K GBP | 0.15% | High |
| **Isle of Man GSC** | 2001 | Varies | 5-50K GBP | 0.1-1.5% | High |
| **AGCC** (Alderney) | 2000 | Varies | 10K+ GBP | 0% | High |
| **Kahnawake** | 1996 | 6 months | $40,000 | 0% | Medium-High |
| **PAGCOR** (Philippines) | 1976 | Varies | $4-15K | 5% | Medium |

**Kahnawake Gaming Commission**: Located in Mohawk Territory, Quebec, Canada. One of the oldest iGaming regulators. Has authorized **250+ gambling websites**. No longer licenses US-facing operators since 2016.

---

## 6. Business Model Terminology

### 6.1 Core Business Models

| Term | Chinese | Definition |
|------|---------|------------|
| **B2B (Business-to-Business)** | 企業對企業 | Companies providing products, services, or technology to other businesses. In iGaming, B2B suppliers license technology, games, or services to operators who directly serve players. Evolution Gaming is a leading B2B live casino provider. Revenue from licensing fees, setup fees, and GGR percentage share. |
| **B2C (Business-to-Consumer)** | 企業對消費者 | Companies directly providing gambling services to individual players. B2C operators run online casinos, sportsbooks, and poker rooms, generating revenue through deposits, house edge, and gambling activity. Requires licenses in each accepting jurisdiction. |

### 6.2 Solutions & Partnership Models

| Term | Chinese | Definition |
|------|---------|------------|
| **White-Label** | 白牌方案 (包網) | Ready-to-use platform under operator's own brand with supplier's infrastructure, license, games, and payments. Launch: **1-2 months**. GGR share: **20-50%**. |
| **Revenue Sharing** | 營收分成模式 | B2B suppliers charge percentage of operator GGR rather than fixed fees. Aligns incentives. Game suppliers may charge **10-15%** of game-specific GGR. Tiered structures reduce percentage as volume increases. |
| **Aggregation Platforms** | 聚合平台 | B2B platforms providing unified access to multiple game suppliers, payment processors, or services via single integration. Hub88, SOFTSWISS aggregate **15,000+ games** from **100+ studios**. |

### 6.3 License Structures

| Term | Chinese | Definition |
|------|---------|------------|
| **Master License** | 主牌照 | Primary gambling license from a jurisdiction granting the right to conduct gambling activities and issue sub-licenses. Holders act as micro-regulators for sub-licensees. In Curacao, only **4 companies** held master licenses. 2025 Curacao reform eliminated the sub-license system. |
| **Sublicensing** | 子牌照 | Gambling permit issued by a master license holder to third-party operators. Grants same operating rights but cannot further sub-license. Historical Curacao sub-license cost: ~**$16,900**, 6-week processing. PAGCOR also offers sub-licensing. |

---

## 7. Data & Analytics Terminology

### 7.1 Core Revenue Metrics

| Metric | Formula | Industry Context |
|--------|---------|-----------------|
| **GGR (Gross Gaming Revenue)** | Total Wagers - Total Player Winnings | Primary tax base in most jurisdictions (Malta, UK, most US states). US 2023 gambling industry GGR: **$66.65 billion**. |
| **NGR (Net Gaming Revenue)** | GGR - Bonuses - Taxes - Commissions - Operating Costs - Fees | Reflects true operator profitability. Promotional costs should stay under **20% of GGR**. Strong operators maintain NGR margins of **50-70% of GGR**. |
| **Hold Percentage** | GGR / Total Wagers x 100% | Benchmarks: Slots 2.5-10%, Table games 15-25%, Sports betting theoretical hold 5-8%. |

### 7.2 Player Value Metrics

| Metric | Formula | Industry Context |
|--------|---------|-----------------|
| **LTV (Player Lifetime Value)** | ARPU x Average Player Lifespan - Acquisition & Retention Costs | LTV must exceed **3x CAC** for profitability. High-value players can be 10-100x average LTV. |
| **CAC (Customer Acquisition Cost)** | Total Sales & Marketing Expenses / New Customers Acquired | Benchmarks: US/UK iGaming CPA per FTD **$200-500+**; Asia-Pacific as low as $0.93. Low CAC ($50-80) may indicate low-quality "bonus hunter" traffic. |
| **LTV:CAC Ratio** | LTV / CAC | 1:1 = breakeven; **3:1 = healthy benchmark**; 4:1+ = excellent; 5:1+ = possibly underinvesting in growth. |

### 7.3 Conversion & Engagement Metrics

| Metric | Benchmark | Notes |
|--------|-----------|-------|
| **Visit-to-Registration Conversion** | **20-30%** healthy | Low rate indicates landing page/UX issues |
| **Registration-to-FTD Conversion** | **12-18%** typical; 15-18% strong | Low rate indicates KYC friction, payment issues, or weak bonus appeal |
| **FTD (First Time Depositor)** | Quality FTDs complete second deposit within **7 days** | Key metrics: FTD count, FTD rate, cost per FTD (CPFTD), time to first deposit (TTFD) |
| **Retention Rate** | 30-day retention **70-80%** = strong success | D1 retention (mobile) 25-30%; D30 minimum 3-5% to cover acquisition costs. **5% retention improvement can yield 25-95% profit increase**. |
| **DAU/MAU Stickiness** | iGaming casino typical **15-25%** | 20% = ~6 days/month; 50% = every other day; 70%+ = daily habit |

### 7.4 Marketing Analytics

| Metric | Formula | Context |
|--------|---------|---------|
| **CPA (Cost Per Acquisition)** | Fixed cost per qualifying customer | Primary affiliate payment model |
| **ROAS (Return on Ad Spend)** | Ad Revenue / Ad Spend x 100% | 200% (2:1) = minimum breakeven; 400-800% = good; 6-9 month breakeven is target; >12 months is concerning |
| **Cohort Analysis** | Group players by common attribute (typically acquisition date) | Applications: FTD-month cohort LTV analysis, acquisition channel quality comparison, bonus type promotional effectiveness |

### 7.5 Risk & Fraud Metrics

| Metric | Threshold / Benchmark | Impact |
|--------|----------------------|--------|
| **Chargeback Rate** | Card network threshold: **0.9-1.0%** | Exceeding triggers penalties or account termination. Merchant dispute win rate: only **32%**. |
| **Bonus Abuse Detection** | Multi-account creation speed, device/browser fingerprint matching, deposit-to-withdrawal cycles | Detection methods: device fingerprinting, IP/geo tracking, velocity checks, behavioral analysis |
| **Risk Scoring** | Scale: 0-100 or 0-1000 | Components: device trust, behavioral anomaly, transaction pattern, identity verification, historical activity. Thresholds for auto-approve, manual review, auto-reject. |

---

## 8. Operations & Marketing Terminology

### 8.1 Bonus Types

| Term | Chinese | Definition |
|------|---------|------------|
| **Welcome Bonus** | 歡迎獎金 | Promotional offer for new player registration or first deposit. Most commonly 100% deposit match. Critical for player acquisition. |
| **First Deposit Bonus (FDB)** | 首存紅利 | Bonus specifically for first deposit. Typically 100-200% match with a maximum cap. |
| **No Deposit Bonus (NDB)** | 免存款紅利 | Bonus requiring no deposit, enabling risk-free play. Usually small amounts ($10-50) or free spins with high wagering requirements. |
| **Cashback / Rakeback** | 返水 | Mechanism returning a percentage of player losses. Cashback: typically **5-15%** of net losses. Rakeback: poker-specific rake return, **5-45%** based on VIP tier. |

### 8.2 Wagering Requirements

| Concept | Standard | Notes |
|---------|----------|-------|
| **Wagering Requirement** | **20-40x** considered reasonable; 50x+ considered high | Calculation: Bonus x Multiplier = Required Turnover (e.g., $100 x 30x = $3,000) |
| **Game Weighting** | Slots 100%, Table games 10-20%, Live casino 10-20%, Video poker 0-10% | Prevents players from exploiting low house edge games for bonus clearing |
| **Bonus Abuse Types** | Multi-accounting (gnoming), collusive betting, chip dumping, arbitrage | Prevention: KYC verification, behavioral monitoring, strict terms, game weighting |

### 8.3 VIP & Loyalty Programs

| Term | Chinese | Definition |
|------|---------|------------|
| **VIP Program** | 貴賓計劃 | Exclusive reward program for highest-value players. Typically invitation-only with personalized service, higher limits, and exclusive events. |
| **Loyalty Points / Comp Points** | 忠誠積分 | Points earned through wagering, redeemable for rewards. Standard rate: 1 point per $10-20 wagered; 100 points = $1 bonus. Used in tiered systems (Bronze, Silver, Gold, Platinum, Diamond). |
| **High Roller / Whale** | 豪賭客/鯨魚 | High Roller: consistent large bettors, typically $10,000+ deposits, $300-500+ per bet. Whale: extreme high-stakes gamblers who may lose hundreds of thousands to millions per session. These players represent critical revenue but high volatility risk. |

### 8.4 Player Acquisition Models

| Model | Structure | Typical Terms |
|-------|-----------|---------------|
| **CPA (Cost Per Acquisition)** | Fixed amount per qualifying player | Requires FTD and minimum wager |
| **Revenue Share** | Percentage of referred player's net gaming revenue | Casino/poker: 20-60%; sports betting: 15-40% |
| **Hybrid** | Upfront CPA + ongoing revenue share | Example: $100 CPA + 20% RevShare |

### 8.5 Retention & Reactivation

| Concept | Benchmark | Key Drivers |
|---------|-----------|-------------|
| **Churn Rate** | First-year churn 50-80% is common; monthly active retention 70%+ is strong | Drivers: payment friction, slow withdrawals, poor mobile UX (70-80% of activity is mobile), bonus fatigue, insufficient game variety |
| **Reactivation Campaigns** | Segmented: 7-14 days inactive (warm), 30 days (cool), 90+ days (cold) | Strategies: personalized bonuses, limited-time offers, "we miss you" messaging. Cost lower than new acquisition. |
| **5% churn reduction** | Can yield **25-95% profit increase** | Retention is more cost-effective than acquisition |

---

## Appendix A: Internal Terminology Standards

### A.1 Standardization Principles

All platform documentation must use the following standardized terms. New terms must be defined in this table and submitted for architecture review before use.

### A.2 Core Term Mapping

| Concept | Recommended Term (EN) | Recommended Term (ZH) | Prohibited Alternatives | Scope |
|---------|----------------------|----------------------|------------------------|-------|
| Bonus | Bonus | 紅利 | ~~Promotion~~, ~~Reward~~, ~~Incentive~~ | Global |
| Valid Turnover | Valid Turnover | 有效流水 | ~~Valid Bet~~, ~~Wagering~~, ~~Betting Amount~~ | Finance, Risk, Activity |
| Tenant | Tenant | 租戶 | ~~Merchant~~, ~~Customer~~ | Multi-tenant modules |
| Game Provider | Game Provider (GP) | 遊戲商 | ~~Game Vendor~~, ~~Game Developer~~, ~~Game Studio~~ | Game modules |
| Player | Player | 玩家 | ~~User~~, ~~Member~~ | Global |
| Agent | Agent / Affiliate | 代理 | ~~Referrer~~, ~~Distributor~~ | Agent modules |
| Withdrawal | Withdrawal | 提款 | ~~Cash Out~~, ~~Payout~~ | Global |
| Deposit | Deposit | 存款 | ~~Top-up~~, ~~Fund In~~ | Global |
| Credit Limit | Credit Limit | 信用額度 | ~~Limit~~, ~~Credit~~ | Finance, Agent |
| Risk Control | Risk Control | 風控 | ~~Risk Management~~, ~~Anti-Fraud~~ | Global |
| Wallet | Wallet | 錢包 | ~~Account~~, ~~Balance~~ | Finance, Game |
| Playable Balance | Playable Balance | 可下注餘額 | ~~Available Balance~~ | Finance, Game |
| Wagering Requirement | Wagering Requirement | 流水要求 | ~~Rollover~~, ~~Play-through~~ | Activity |
| Approval Workflow | Approval Workflow | 審批工作流 | ~~Review Process~~ | Governance |
| Audit Log | Audit Log | 審計日誌 | ~~Activity Log~~, ~~Operation Log~~ | Global |
| Multi-Tenancy | Multi-Tenancy | 多租戶 | ~~Multi-Merchant~~, ~~SaaS Mode~~ | Architecture |
| Seamless Wallet | Seamless Wallet | 無縫錢包 | ~~Instant Wallet~~, ~~Single Wallet~~ | Game |
| Transfer Wallet | Transfer Wallet | 轉帳錢包 | ~~Independent Wallet~~, ~~Fund Transfer~~ | Game |
| Cashback / Rakeback | Cashback / Rakeback | 返水 | ~~Rebate~~ | Activity, VIP |
| PSP | PSP (Payment Service Provider) | 支付服務商 | ~~Payment Provider~~ | Global |
| Reconciliation | Reconciliation | 對帳 | ~~Settlement~~, ~~Account Matching~~ | Finance |

### A.3 Usage Rules

**First Occurrence**: Use "Chinese term (English abbreviation)" format. Subsequent references may use Chinese term or abbreviation alone.

**Consistency Checklist**:
- All core terms match this table
- First use of abbreviations includes full form
- No "prohibited" column terms are used
- New terms have been defined in this table

### A.4 New Term Introduction Process

1. Confirm necessity (check if existing terms suffice)
2. Add complete definition to this table
3. Submit for Architecture Team review
4. Record addition in CHANGELOG

---

## Appendix B: Terminology Consistency Guide

### B.1 Finance Domain

| Concept | Primary Term | Acceptable Variant | Avoid |
|---------|-------------|-------------------|-------|
| Withdrawal review process | **Withdrawal Approval** | Withdrawal Review (describing stages) | Withdrawal Check, Payout Approval |
| Risk evaluation | **Risk Assessment** | Risk Evaluation (scoring context) | Risk Check, Safety Check |
| Approval process | **Approval Workflow** | Approval Process (flow description) | Review Process, Check Process |
| Transaction handling | **Transaction Processing** | Payment Processing (payment context) | Transaction Handling |
| Account matching | **Reconciliation** | Settlement (settlement context) | Account Matching, Balance Check |
| Funds verification | **Source of Funds (SOF)** | Funds Verification | Money Source Check |

### B.2 Gaming Domain

| Concept | Primary Term | Acceptable Variant | Avoid |
|---------|-------------|-------------------|-------|
| Valid turnover | **Valid Turnover** | Effective Turnover (efficiency analysis) | Valid Bet, Real Bet |
| Valid bet amount | **Valid Bet Amount** | - | Effective Bet Amount |
| Hedge detection | **Hedging Detection** | Hedge Check (as verb) | Anti-Hedging, Hedging Prevention |
| Arbitrage detection | **Arbitrage Detection** | Arb Detection (abbreviation) | Arbitrage Check |
| Game weight | **Game Weight** | Contribution Rate | Game Factor, Weight Factor |
| Game supplier | **Game Provider (GP)** | Game Vendor (supplier context) | Game Studio, Game Developer |

### B.3 Risk Control Domain

| Concept | Primary Term | Acceptable Variant | Avoid |
|---------|-------------|-------------------|-------|
| Abnormal betting | **Anomaly Betting Pattern** | Abnormal Pattern (statistics) | Strange Bet, Weird Pattern |
| Multi-account linking | **Multi-Account Linkage** | Account Clustering (technical) | Duplicate Account, Fake Account |
| Risk factor | **Risk Factor** | Risk Score (scoring context) | Risk Value, Control Factor |
| Odds threshold | **Odds Threshold** | Odds Limit | Odds Boundary, Max Odds |
| Bet verification | **Bet Validation** | Wager Verification | Bet Check, Betting Verify |

### B.4 Document Writing Rules

**Headings**: Must use primary term.

**First occurrence**: Primary term + English full form.

**Subsequent references**: Primary term or acceptable variant.

**Cross-references**: Use primary term for consistency.

**Multilingual handling**:

| Context | Rule | Example |
|---------|------|---------|
| Chinese document | Chinese primary + English abbreviation (first use) | 提款審核流程 (Withdrawal Approval) |
| English document | English primary + Chinese annotation (optional) | Withdrawal Approval (提款審核流程) |
| Code comments | English primary + brief description | `// Withdrawal Approval: validates and processes withdrawal requests` |
| API documentation | English primary (RESTful) | `POST /api/withdrawals/approval` |

### B.5 Pre-Publication Checklist

- All section headings use primary terms
- Core concepts include full English form at first occurrence
- Code/API naming matches primary terms
- No "avoid" column terms are used
- Cross-references use primary terms
- Multilingual handling follows standards

### B.6 Maintenance

**Review Cycle**: Quarterly (synchronized with Appendix A)

**Responsible Team**: Architecture Team

**Update Process**:
1. Collect documentation usage feedback
2. Identify new terminology conflicts
3. Assess industry terminology evolution
4. Update primary term table
5. Execute global terminology unification

---

## Related Documents

- [Player Lifecycle](./Player_Lifecycle.md) - Player journey and account states
- [Business Flows](./Business_Flows.md) - Core business processes

---

**Document Version**: 4.0.0
**Last Updated**: 2026-02-08
**Maintainers**: Architecture Team
**Next Review**: 2026-05-08 (quarterly)
