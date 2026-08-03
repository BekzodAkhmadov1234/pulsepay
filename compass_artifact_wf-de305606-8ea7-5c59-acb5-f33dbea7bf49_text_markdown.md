# Business Rules & Business Logic Specification — Uzbekistan Money-Transfer / Payment Application (UZS, 2026)

## TL;DR
- **The single most important launch rule set is defined by the Central Bank of Uzbekistan's remote-financial-services regulation (approved 23 December 2025, registered by the Ministry of Justice 21 January 2026, with additional registration/verification requirements effective 22 April 2026):** a new user's phone number, PINFL (personal ID number) and bank card must all belong to the same person (or a documented close relative); OTPs must be six digits, valid 59 seconds, cancelled after 3 wrong attempts with a 15-minute lockout; biometric liveness identification is required at registration, password recovery and new-device login; and linked cards must be *deactivated* (not deleted) after a new-device login or password recovery and reactivated by OTP. These are MANDATORY/REGULATORY and non-negotiable for launch.
- **Money-movement safety comes from a small set of hard invariants**: an immutable double-entry ledger where balances are summed not stored, idempotency keys on every financial write, reserve-before-send (available vs pending balance) to prevent double-spend, a deterministic transfer state machine that rejects illegal transitions, and reconciliation that never silently edits the ledger. These are INDUSTRY-standard TECHNICAL_SAFETY invariants that belong in application code.
- **The AML backbone is regulatory and threshold-driven**: suspicious-activity criteria at 1,000 BCV (≈412 million UZS, single or aggregated over 30 days), customer due-diligence identification triggered at 500 BCV (≈206 million UZS) for one-off transactions, mandatory sanctions/terrorism-list screening at least every three months, and 5-year record retention — under Law No. 660-II. Exact STR filing deadlines and freeze durations sit in non-public sub-regulatory acts and MUST be obtained contractually from the CBU/FIU or counsel.

## Key Findings

1. **Uzbekistan already regulates the exact behaviours this task asks about.** The CBU's "minimum requirements for information security, cybersecurity and fraud prevention in remote financial services for individuals" (approved 23 December 2025, registered by the Ministry of Justice 21 January 2026) prescribes concrete behavioural rules — OTP length/lifetime/lockout, biometric triggers, card deactivation on security events, phone-PINFL matching, and fraud warnings before transactions. Many rules this brief treats as "illustrative examples" (new-device step-up, credential-change session handling) are therefore MANDATORY in Uzbekistan, not platform choices. **Critically, the regulation is in active flux:** a draft amendment was open for public consultation on the government legislation portal until 1 August 2026 (per Kun.uz, 22 July 2026) and would let institutions set their own OTP-confirmation criteria for P2P transfers and choose SMS-or-push fraud alerts — so the platform must build these as configurable, effective-dated rules rather than hard-coding.
2. **1 BCV (Base Calculation Value / BHM) = 412,000 UZS**, effective 1 August 2025 (raised from 375,000 UZS), per PwC Tax Summaries and the Golden Pages Directory of Uzbekistan. AML thresholds are denominated in BCV, so they must be stored as BCV multiples with an effective-dated BCV value, never hard-coded in UZS.
3. **UzCard and Humo are separate systems and must NOT be assumed identical.** UzCard is operated by "Common Republican Processing Centre" JSC; Humo (humocard.uz) is a separate national system. Both use 6-digit SMS OTP for card linking and online-transaction confirmation, but operating rules, dispute windows, and settlement details differ and are contractual.
4. **Two distinct transfer rails now exist**: P2P (card-number-based, via UzCard/Humo) and A2A (account-to-account, launched in Uzbekistan and reported by Gazeta.uz on 29 March 2026 under presidential decree PF-246 of 10 December 2025, running across all banks). C2B merchant payments will additionally run over a mandatory unified QR standard — the "UzQR" system operated by MUNIS across both Humo and Uzcard — with the CBU regulation registered by the Ministry of Justice on 15 April 2026 and acceptance becoming mandatory for all trade and service organisations from 1 July 2026 (per Kun.uz, 18 April and 23 June 2026); merchants pay a 0.65% per-transaction fee and buyers pay nothing.
5. **Bank liability for fraud is shifting to institutions.** A 2026 initiative (reported by Spot.uz, 13 March 2026) would establish administrative liability for banks and payment institutions for failing to meet information/cybersecurity requirements "regardless of whether consequences have occurred," and referenced that vulnerabilities in three banks' systems caused 3,025 clients to suffer 17 billion soums in damage. This raises the stakes on fraud controls and audit evidence — because proving customer gross negligence becomes the institution's main defence.

---

## Executive Summary — Most Important Business Rules

The application must enforce, in strict priority order, a layered decision pipeline on every money movement: **(1) Regulatory/AML/KYC gates → (2) Network/Bank rules → (3) Platform fraud/risk → (4) Ordinary validation → (5) Ledger invariants.** Regulatory rules can only tighten, never loosen, what fraud/risk allow.

The seven most important security behaviours, all backed by Uzbek regulation or strong industry practice:

- **Identity-phone-card binding (REGULATORY/MANDATORY):** registration and card-linking are auto-rejected when phone, PINFL and card do not match one person (documented close-relative exception).
- **Biometric liveness step-up (REGULATORY/MANDATORY):** required at registration, password recovery, and new-device login.
- **OTP discipline (REGULATORY/MANDATORY):** 6 digits, 59-second validity, 3-attempt cap, 15-minute lockout.
- **Security-event card deactivation (REGULATORY/MANDATORY):** after new-device login or password recovery, linked cards move to inactive and must be reactivated by OTP.
- **Reserve-before-send + immutable double-entry ledger (TECHNICAL_SAFETY/INDUSTRY):** prevents double-spend and phantom money.
- **Idempotency on every financial write (TECHNICAL_SAFETY/INDUSTRY):** exactly-once money movement across retries and lost responses.
- **AML threshold monitoring + list screening (REGULATORY/MANDATORY):** 1,000 BCV suspicious threshold, 500 BCV identification, quarterly list screening, 5-year retention.

---

## Critical Security Rules (behavioural, with classification)

**New card added.** Card linking requires an OTP sent to the cardholder's registered phone (MANDATORY, NETWORK+REGULATORY). Under current CBU rules, card linking also required biometric verification (MANDATORY as of April 2026; the July 2026 draft would drop the biometric requirement and keep OTP-only). If the phone number registered to the card differs from the user's registered number, the process is blocked with SMS notification (MANDATORY). **A cooling-off period for a newly linked card before high-risk transfers is a PLATFORM_DECISION / FRAUD recommendation, not a published Uzbek requirement** — recommend treating a newly linked card as reduced-limit until it has been used for at least one successful step-up-verified transaction; hold this as a configurable value, do not hard-code a specific duration since no authoritative Uzbek figure exists.

**New device / device change.** New-device login requires biometric liveness step-up (MANDATORY — the CBU official statement at cbu.uz/en/press_center/news/3108969 confirms Face ID at "New Registration," "Password Recovery," and "Login from Another Device"). All linked cards are switched to inactive after a new-device login and must be reactivated by OTP (MANDATORY under current rules; the delete-history requirement is being removed by the July 2026 draft). A large transfer from a newly registered device SHOULD trigger step-up (FRAUD/RECOMMENDED).

**Password / PIN change.** Password recovery requires biometric liveness (MANDATORY). Session revocation on credential change is INDUSTRY best practice / SECURITY (RECOMMENDED). Temporarily reducing limits after credential change is a FRAUD/PLATFORM_DECISION.

**Phone-number change.** Because the phone-PINFL binding is regulatory, a phone-number change must re-verify the new number against PINFL (MANDATORY consequence of the binding rule) and should be treated like a new-device event (step-up, card reactivation) — RECOMMENDED.

**Beneficiary added / changed.** No published Uzbek cooling-off period exists; a new-beneficiary cooling-off / reduced-limit window is a FRAUD/PLATFORM_DECISION. P2P transfers themselves currently require OTP each time (MANDATORY under current rule; the July 2026 draft would let institutions set thresholds).

**Account recovery.** Recovery = biometric liveness + OTP; cards deactivated pending OTP reactivation (MANDATORY). A post-recovery security cooling-off restricting high-value transfers is RECOMMENDED (FRAUD).

**High-risk transaction.** Step-up (OTP/biometric/anti-fraud check) driven by amount, velocity, new recipient, unusual device/location — the specific criteria are being devolved to each institution's fraud-risk policy by the July 2026 draft (PLATFORM_DECISION within a MANDATORY framework).

---

## Complete Business Rule Catalog (grouped; format: ID / Name / Applies / Trigger / Rule / Decision / Source / Type / Confidence)

### Registration & Onboarding
- **REG-01 Phone-PINFL-Card match** / new user / on registration & card link / phone, PINFL, card must belong to one person or documented close relative, else auto-reject + SMS / DENY / CBU remote-services reg. (eff. 22 Apr 2026) / REGULATORY-MANDATORY / HIGH.
- **REG-02 Registration biometric** / new user / first registration / mandatory remote biometric liveness identification (MyID / equivalent, ISO 30107-3 liveness) / STEP_UP / CBU reg. / REGULATORY-MANDATORY / HIGH.
- **REG-03 OTP discipline** / all users / any OTP / 6 digits, 59s validity, 3 wrong attempts → cancel, repeated failures → 15-min activity restriction / LIMIT / CBU reg. / REGULATORY-MANDATORY / HIGH.
- **REG-04 Age** / minors / onboarding / 14–18 may independently open bank accounts (CBU order amending card issuance/circulation rules, registered by Ministry of Justice 11 November 2025); banks set strict transfer/withdrawal limits for junior cards; parental oversight / LIMIT / CBU + bank product / REGULATORY+BANK / HIGH.
- **REG-05 Duplicate identity** / applicant / identity already registered / block second account on same PINFL / DENY / Platform + KYC / PLATFORM_DECISION / MEDIUM.

### Authentication & Login
- **AUTH-01 MFA login** / all / every login / password + OTP / STEP_UP / CBU reg. / REGULATORY-MANDATORY / HIGH.
- **AUTH-02 New-device biometric** / all / login from new device / biometric liveness required / STEP_UP / CBU reg. / REGULATORY-MANDATORY / HIGH.
- **AUTH-03 Failed-login lockout** / all / repeated OTP/password failure / temporary 15-min restriction after repeated wrong OTP; UzCard PIN 3-wrong-tries → block, unblockable via bank app / LIMIT / CBU reg. + network / REGULATORY+NETWORK / HIGH.

### Device management
- **DEV-01 Card deactivation on new device** / all / new-device login or recovery / linked cards → inactive, reactivate by OTP (delete-history requirement being removed by July 2026 draft) / LIMIT / CBU reg. / REGULATORY-MANDATORY / HIGH.
- **DEV-02 New-device large-transfer step-up** / all / big transfer from new device / require step-up / STEP_UP / Fraud practice / FRAUD-RECOMMENDED / MEDIUM.

### KYC
- **KYC-01 Identification threshold** / one-off transactions / amount ≥ 500 BCV (≈206M UZS) / full identification (name, ID series/number, DOB) mandatory / STEP_UP/DENY-if-absent / Law 660-II sub-reg (2025 amendment) / REGULATORY-MANDATORY / HIGH.
- **KYC-02 Extended info on large transfers** / transfers / large transfers / request extended sender+recipient identification data; if recipient bank already holds sender data, PINFL/account/transaction ID suffices / REVIEW / CBU internal-control amendment (registered 4 Apr 2025) / REGULATORY-MANDATORY / MEDIUM.

### AML
- **AML-01 Suspicious threshold** / all / single or 30-day-aggregated ≥ 1,000 BCV (≈412M UZS) / classify suspicious, monitor, report to FIU / REVIEW/HOLD / CBU Board criteria (2026) + Law 660-II / REGULATORY-MANDATORY / HIGH.
- **AML-02 List screening** / all customers / ongoing / screen against terrorism/WMD-proliferation list at each list update and at least every 3 months / HOLD/DECLINE on match / Prosecutor-General Resolution No. 3327 (19 Oct 2021) / REGULATORY-MANDATORY / HIGH.
- **AML-03 FATF-jurisdiction monitoring** / cross-border / transfers to/from FATF-listed jurisdictions / enhanced monitoring / REVIEW / CBU criteria (2026) / REGULATORY-MANDATORY / MEDIUM.
- **AML-04 Retention** / all / record keeping / retain identification/transaction records ≥ 5 years / — / Law 660-II / UzCard reg. §4.3.7.3 / REGULATORY-MANDATORY / HIGH.
- **AML-05 Freeze/hold** / suspicious txn / on suspicion / suspend transaction pending FIU decision; release authority = FIU/court for list-based freezes, institution auto-resumes if no seizure follows within the statutory window / HOLD / Law 660-II sub-reg / REGULATORY-MANDATORY / MEDIUM (exact durations non-public — CONTRACTUAL; a pre-2019 State Dept figure of 3+2=5 business days is indicative only and unverified for the current redaction).

### Cards
- **CARD-01 Link OTP** / all / card add / OTP to cardholder's registered phone required / STEP_UP / network + CBU / NETWORK+REGULATORY / HIGH.
- **CARD-02 Ownership mismatch** / all / card phone ≠ user phone / block link + SMS / DENY / CBU reg. / REGULATORY-MANDATORY / HIGH.
- **CARD-03 Removal** / all / remove card / block removal while transaction pending; historical ledger entries retained (immutable); revoke external token / DENY-if-pending / Industry + ledger invariant / TECHNICAL_SAFETY-MANDATORY / HIGH.
- **CARD-04 Status set** / all / card lifecycle / ACTIVE/PENDING_VERIFICATION/INACTIVE/BLOCKED/EXPIRED/REMOVED transitions govern which operations allowed / — / Platform + network / PLATFORM_DECISION / HIGH.

### Beneficiaries
- **BEN-01 P2P OTP** / all / each P2P transfer / OTP confirmation (current rule); institutions may set thresholds after July 2026 draft; P2P via website remains prohibited / STEP_UP / CBU reg. / REGULATORY-MANDATORY(→PLATFORM) / HIGH.
- **BEN-02 New-beneficiary cooling-off** / all / newly added payee / reduced limit / step-up window / LIMIT / Fraud practice / FRAUD-RECOMMENDED / LOW (no Uzbek figure — do not invent a duration).

### Transfers (P2P / P2A / A2A / B2B / B2C)
- **TRF-01 Lifecycle** / all transfers / creation→settlement / enforce deterministic state machine; reject illegal transitions / — / Industry / TECHNICAL_SAFETY-MANDATORY / HIGH.
- **TRF-02 Reserve funds** / all / on authorization / move amount+fee from available to pending (hold) before external call / — / Industry / TECHNICAL_SAFETY-MANDATORY / HIGH.
- **TRF-03 Idempotency** / all / create/retry / idempotency key + DB unique constraint written before external call; replay stored result / — / Industry / TECHNICAL_SAFETY-MANDATORY / HIGH.

### Limits & Fees
- **LIM-01 Config-driven limits** / all / any transfer / per-txn/daily/weekly/monthly/velocity limits keyed by user, card, network, txn type, KYC level; stored as config with effective dates / LIMIT / Platform + bank / PLATFORM_DECISION+BANK / HIGH.
- **LIM-02 Bank/network limits are contractual** / all / — / actual UZS limits vary by bank and app (e.g. Ipoteka junior-card cash-withdrawal 1M UZS/month; free-P2P monthly thresholds differ per app — HUMANS 1M/5M/10M UZS by plan, MAVRID 5M UZS) and must be obtained from each bank/network / — / Bank tariffs / BANK-MANDATORY / HIGH.
- **FEE-01 Fee lifecycle** / all / calc→reserve→post→refund / reserve fee with principal; post on completion; if fee posting fails after success, compensate via ledger adjustment not silent edit; if txn fails after fee reserved, release reserve / — / Industry / ACCOUNTING-MANDATORY / HIGH.
- **FEE-02 No deduction from amount** / payment services / — / commission must not be deducted from the payment amount unless contractually agreed with the beneficiary / DENY / UzCard reg. §4.3.6 / NETWORK-MANDATORY / HIGH.

### Fraud
- **FRD-01 Fraud warnings** / all / before transactions / display fraud warning (currently every transfer; institutions may tailor by policy after July 2026 draft) / — / CBU reg. / REGULATORY-MANDATORY(→PLATFORM) / HIGH.
- **FRD-02 Anomaly decisioning** / all / velocity/amount/recipient/device/location anomalies / map to ALLOW / ALLOW_WITH_STEP_UP / LIMIT / HOLD / MANUAL_REVIEW / DECLINE / ACCOUNT_FREEZE / decision / CBU centralised anti-fraud (mandated by 2025 decree, target completion Sept 2025) + platform / REGULATORY+FRAUD / MEDIUM.
- **FRD-03 Card blocking on dubious transfers** / all / cards involved in dubious transfers / promptly block / HOLD / 2025 cybercrime resolution / REGULATORY-MANDATORY / MEDIUM.

### Payments / Refunds / Reversals / Disputes / Settlement / Reconciliation
- **PAY-01 C2B unified QR** / merchants / from 1 Jul 2026 / accept unified UzQR (static/dynamic); acquiring banks supply codes; 0.65% merchant fee, 0 buyer fee / — / CBU QR reg. (registered 15 Apr 2026) / REGULATORY-MANDATORY / HIGH.
- **REF-01 Refund to source** / all / refund / refund to original funding source; support full/partial/multiple-partial; post as new ledger entries, never edit originals / — / Industry / ACCOUNTING-MANDATORY / HIGH.
- **REV-01 Distinct operations** / all / — / VOID (before settlement), REVERSAL (system-initiated correction), REFUND (merchant/customer-initiated post-capture), CHARGEBACK (issuer-initiated dispute), ADJUSTMENT (reconciliation) — each has distinct ledger behaviour / — / Industry + network / TECHNICAL_SAFETY / HIGH.
- **DIS-01 Dispute/chargeback** / cardholder / dispute / route via issuer→network→acquirer→merchant; provisional credit; representment; deadlines/reason codes are network/bank contractual / REVIEW / UzCard/Humo rules + Visa/MC / NETWORK-MANDATORY / MEDIUM.
- **SET-01 Settlement** / participants / end-of-day / settlement bank debits/credits participant correspondent accounts on the net-position register by the end of each day / — / UzCard rules §8.5.5 / NETWORK-MANDATORY / HIGH.
- **REC-01 Reconciliation** / all / ledger ≠ external settlement / detect, investigate, post adjustment with approval + audit; never silently modify ledger to balance / REVIEW / Industry / TECHNICAL_SAFETY-MANDATORY / HIGH.

### Notifications, Audit, Account lifecycle, Recovery, Administration
- **NOT-01** security-critical notifications (new-device login, card add/remove, credential change, beneficiary add, transfer status, refund, suspicious activity, freeze, limit change) — mandatory for security events; may be SMS or push per July 2026 draft / SECURITY / HIGH.
- **AUD-01** immutable audit records for authentication, KYC/AML decisions, card/beneficiary changes, transfer decisions, limit/fee changes, status changes, refunds/disputes, admin actions; fields who/what/when/prev-state/new-state/reason/source-device/correlation-ID / TECHNICAL_SAFETY-MANDATORY / HIGH.
- **ACL-01** account states ACTIVE/RESTRICTED/FROZEN/SUSPENDED/CLOSED with capability matrix; freeze/unfreeze by compliance/fraud/admin per least-privilege / OPERATIONAL / HIGH.
- **ADM-01** role-based least privilege: support (view/limited), fraud analyst (hold/review), compliance officer (AML hold/release, merchant/KYB approval), finance (adjustments/reconciliation), admin (config); segregation of duties on money-affecting actions / OPERATIONAL-MANDATORY / HIGH.

---

## State Machines

**User:** UNREGISTERED → PENDING_VERIFICATION (biometric+OTP) → ACTIVE ↔ RESTRICTED ↔ FROZEN → SUSPENDED → CLOSED. Recovery re-enters via PENDING_VERIFICATION.

**Card:** PENDING_VERIFICATION → ACTIVE ↔ INACTIVE (security event, OTP-reactivatable) ↔ TEMPORARILY_RESTRICTED → BLOCKED / EXPIRED / REMOVED. (INACTIVE reflects the specific Uzbek security-event rule and is distinct from user- or issuer-BLOCKED.)

**Beneficiary:** ADDED (PENDING) → VERIFIED → ACTIVE ↔ RESTRICTED → DELETED.

**Transfer:** CREATED → VALIDATED → RISK_EVALUATED → (STEP_UP_REQUIRED →) AUTHORIZED → RESERVED → PROCESSING → COMPLETED; failure branches to DECLINED / TIMED_OUT / REVERSED / HELD_FOR_REVIEW; each non-terminal has a defined compensation path. Terminal states: COMPLETED, DECLINED, REVERSED, EXPIRED.

**Merchant:** ONBOARDING → UNDER_REVIEW → ACTIVE ↔ SUSPENDED ↔ UNDER_REVIEW → CLOSED.

**Business (KYB):** REGISTERED → DOCUMENTS_SUBMITTED → UNDER_REVIEW → VERIFIED/ACTIVE ↔ SUSPENDED → CLOSED.

**Refund:** REQUESTED → APPROVED → PROCESSING → COMPLETED / FAILED (retry) / PENDING.

**Settlement:** PENDING → BATCHED → SETTLED / REJECTED → (REVERSAL if needed).

**Dispute:** OPENED → EVIDENCE_REQUESTED → UNDER_REVIEW → (representment) → RESOLVED_MERCHANT / RESOLVED_CARDHOLDER / ESCALATED.

---

## Transfer Decision Flow (corrected sequence)

The example ordering in the brief is broadly right but should be corrected so that **authentication happens up front, cheap deterministic validation precedes expensive risk/AML checks, funds are reserved before any external call, and settlement/reconciliation are asynchronous post-completion stages**:

1. **Authenticate** (session valid; step-up if triggered by device/amount/new-payee).
2. **Idempotency check** (reject/replay duplicate key).
3. **Structural validation** (amount > 0, currency = UZS, route supported, sender account ACTIVE, card ACTIVE, recipient resolvable).
4. **KYC/KYB gate** (KYC level sufficient for this amount/type; full identification if ≥ 500 BCV).
5. **Limit check** (per-txn/daily/velocity available).
6. **Fraud/risk scoring** → ALLOW / STEP_UP / HOLD / REVIEW / DECLINE.
7. **AML screening** (threshold + sanctions/terrorism list) → may HOLD/REVIEW.
8. **Fee calculation.**
9. **Reserve funds** (debit available → pending, principal + fee) with ledger entry.
10. **Authorize with external network/bank** (UzCard/Humo/A2A) — idempotent, with timeout.
11. **Post to ledger** on confirmed success (balanced double entry); release reserve on failure.
12. **User-visible status update + notification.**
13. **Settlement** (async, batched, end-of-day net position).
14. **Reconciliation** (async; mismatch → adjustment with approval, never silent edit).

Note: step 6 (fraud) precedes step 7 (AML) operationally, but **an AML/regulatory HOLD outranks a fraud ALLOW** in precedence — a regulatory block cannot be overridden by a favourable risk score.

---

## Edge Cases (Scenario / Expected / State / Financial / Ledger / Retry / Review / Notify / Audit)

- **User closes app / loses internet mid-transfer:** transfer stays in last durable state (RESERVED or PROCESSING); no double-charge because of idempotency key; funds remain reserved until external result or timeout; on timeout → reversal releases reserve. Retry: technical retry only with same key. Notify on final state. Audit each transition.
- **Authorization succeeds but response lost:** treat as unknown; do NOT re-issue as a new financial request; reconcile against network; idempotency ensures the eventual confirmation posts once. Manual review only if reconciliation cannot resolve.
- **Authorization times out:** mark PROCESSING/TIMED_OUT; poll/reconcile; release reserve only after confirmed non-settlement. Financial retry (new money movement) forbidden until state known.
- **Webhook arrives twice / out of order:** idempotent webhook processing (dedupe by event ID); out-of-order handled by a state machine that ignores backward transitions.
- **Settlement arrives late / reversal after completion:** post a new compensating ledger entry (REVERSAL/ADJUSTMENT); never edit the completed entry; notify; audit; may need manual review.
- **Sender balance changes / card removed / card expires during processing:** reservation already isolates funds; card removal blocked while pending; if card expires mid-flight and network declines, reverse and release.
- **Recipient account closed / card blocked / changed bank:** network returns decline → reverse, release reserve, notify sender, funds returned; audit.
- **DB commit succeeds but event publish fails:** transactional outbox pattern guarantees eventual publication; consumer crash → redelivery + idempotent consumer.
- **Service restart mid-transaction:** durable state + idempotency allow safe resume; no partial money movement because ledger writes are atomic with the state transition.
- **Reconciliation mismatch:** flag, investigate, post approved adjustment, escalate; ledger integrity preserved.

---

## Database Impact (against existing 42-table supertype schema)

The existing parties/instruments/transfer_participants model plus users/cards/businesses/merchants/bank_accounts/transfers/ledger/fee_rules/limit_rules/transfer_routes/banks/payment_networks/beneficiaries/settlement_batches/reconciliations covers most rules. The following **durable-state concepts are genuinely required** by the researched rules and are not representable in the current schema:

- **security_cooldowns / transfer_restrictions** — the CBU rules create durable post-event states (card INACTIVE after new device, reactivation pending OTP; post-recovery restrictions). Without a table these transient-but-durable restrictions cannot be enforced across sessions.
- **device_trust** — required to implement new-device detection, biometric-required-on-new-device, and card-deactivation triggers. Cannot be derived from existing tables.
- **card_verification** and **beneficiary_verification** — needed to record OTP/biometric verification state and the phone-PINFL-card match outcome per REG-01/CARD-01/CARD-02.
- **limit_counters** — rolling velocity/daily/monthly counters keyed by user/card/device/network; limit_rules holds config, but counters need their own durable store.
- **risk_events / risk_decisions** — to record FRD-02 decisions and support the mandatory audit trail and bank-liability defence.
- **manual_reviews / transaction_holds** — to model AML-05 holds and the FLAGGED→UNDER_REVIEW→APPROVED/DECLINED workflow with who-can-release.
- **account_restrictions** — account-level RESTRICTED/FROZEN capability enforcement.

**Do NOT add** speculative tables beyond these; OTP attempt counters and 15-minute lockouts can live in a cache/short-lived store rather than a new core table.

## Configuration vs Code vs External

- **Database configuration:** all limits, fees, effective dates, the BCV value, risk thresholds, supported transaction types, the KYC-level capability matrix, and cooldown durations.
- **Application code:** the ledger-balancing invariant, state-machine transition legality, idempotency enforcement, reserve-before-send, authorization rules, and session/credential invariants.
- **External configuration:** UzCard/Humo network rules and dispute windows, per-bank limits and settlement schedules, integration credentials, the sanctions-list feed, and MyID/biometric provider liability terms.

---

## KYC State × Capability Matrix (illustrative — states are the correct minimal set)

States: **UNVERIFIED, PENDING, VERIFIED, REJECTED, EXPIRED, REVIEW_REQUIRED, RESTRICTED** (these are supportable; avoid inventing others).

- **UNVERIFIED:** no money movement; registration/biometric not complete.
- **PENDING:** receive only; no send/withdraw/card-add pending verification.
- **VERIFIED:** full P2P/P2A/A2A/B2C/C2B and card-add/withdrawal within KYC-level limits; transactions ≥ 500 BCV still require full identification data on the transaction.
- **REVIEW_REQUIRED / RESTRICTED:** inbound allowed; outbound held pending review (AML-05).
- **REJECTED / EXPIRED:** no outbound money movement; must (re)verify.

---

## Recommendations (staged)

**MVP (must-have to launch safely):**
- REG-01/02/03 (phone-PINFL-card match, registration biometric, OTP discipline), AUTH-01/02 (MFA, new-device biometric), DEV-01 (card deactivation on security events), CARD-01/02 (link OTP + ownership mismatch block).
- Immutable double-entry ledger, reserve-before-send, idempotency on transfer/payment/refund, deterministic transfer state machine (TRF-01/02/03).
- AML-01/02/04 (threshold monitoring, quarterly list screening, 5-year retention), KYC-01 (500 BCV identification), FEE-01/02.
- Config-driven limits (LIM-01) with per-bank/network values obtained contractually (LIM-02), security-critical notifications (NOT-01), immutable audit (AUD-01), reconciliation that never silently edits (REC-01).
- Account/card state machines and freeze capability (ACL-01), least-privilege admin roles (ADM-01).

**Production Hardening (before serious real-world volume):**
- Full anomaly-based fraud decisioning (FRD-02) integrated with the CBU centralised anti-fraud system; new-device large-transfer and new-beneficiary step-up/cooling-off (DEV-02, BEN-02) as configurable rules; card-blocking on dubious transfers (FRD-03); the manual-review workflow and transaction_holds; transactional outbox + idempotent webhook consumers; dispute/chargeback handling (DIS-01).

**Phase 2:** KYB/merchant onboarding depth, merchant reserves/holds, partial-refund and multi-refund flows, unified-QR C2B (PAY-01) if merchant acquiring is in scope, A2A rail integration.

**Enterprise/Future:** PEP handling, cross-border/FATF-jurisdiction enhanced monitoring at scale, advanced velocity/ML risk models, tokenized-asset/stablecoin readiness.

**Benchmarks that change the plan:** if the July 2026 CBU draft is adopted, relax biometric-on-registration/card-link and P2P-per-transfer-OTP to institution-policy — so build these as dated, configurable rules now. If the bank cyber-fraud-compensation liability is enacted, elevate FRD-02/AUD-01 to MVP-critical because the institution's only defence becomes provable customer gross negligence, which depends on complete audit and risk-decision records.

## Rules Requiring External Contracts (cannot be determined publicly)
- Exact STR filing deadline to the FIU and precise suspicious-transaction suspension/freeze durations and extension mechanics (sub-regulatory, non-public; a pre-2019 "3+2 = 5 business days" figure is indicative only).
- UzCard and Humo per-transaction/daily network limits, dispute/chargeback windows and reason codes, and settlement timing/reversal mechanics (differ between the two networks).
- Per-bank card limits, junior-card limits, and acquiring settlement schedules (T+n) and payout thresholds.
- MyID / biometric provider SLAs and liability allocation for verification failures.
- A2A rail message formats, timeouts, and reconciliation cutoffs.
- Merchant onboarding/KYB document requirements and reserve/hold policies per acquiring bank.

## Caveats
- **Regulatory flux:** the governing CBU remote-services regulation (approved 23 December 2025) was under active amendment with public consultation running until 1 August 2026 (Kun.uz, 22 July 2026); several MANDATORY rules cited here (registration biometric, per-transfer P2P OTP, mandatory pre-transaction fraud warnings) may become institution-discretionary. Build all as dated, configurable rules.
- **BCV dependency:** all AML thresholds are BCV multiples (1 BCV = 412,000 UZS since 1 August 2025); the soum figures shift when the BCV is revised — one older source quoted 500 BCV ≈ 187.5M UZS reflecting a prior BCV of 375,000, whereas at the current rate 500 BCV = 206M UZS (Kun.uz). Store thresholds as BCV, not UZS.
- **UzCard ≠ Humo ≠ each bank:** do not assume identical behaviour; treat network- and bank-specific rules as external configuration.
- **Non-public sub-regulation:** AML freeze durations and STR deadlines could not be confirmed from Tier-1 public sources and must be obtained from the CBU/FIU or counsel; the FIU sits inside the Prosecutor General's Office (Department for Combating Economic Crimes), giving holds and information requests unusual speed and reach.
- **Data localization changed in 2026:** amendments to the Law on Personal Data took effect 27 March 2026, relaxing the strict local-storage rule while retaining mandatory local storage for defined sensitive categories — card and identity data handling must follow the amended regime, and full PAN/CVV should never be stored directly where tokenization/provider storage is available.