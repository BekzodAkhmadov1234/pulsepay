# Claude Code Prompt: Implement Business Rules for Uzbekistan Payment Platform

*Copy everything below the line into Claude Code (in this repo's root) to begin implementation.*

---

## Context

I'm building a production money-transfer/payment application for Uzbekistan (P2P, P2A, B2B, B2C, C2B) in UZS, integrating with UzCard and Humo. I have two reference documents that are the source of truth for this work — **read both completely before writing any code**:

1. **`docs/database-schema.md`** — the current database schema: a 42-table model built around a `parties`/`instruments`/`transfer_participants` supertype design (class-table inheritance) that replaced an earlier nullable-foreign-key `transfers` table. It defines `users`, `businesses`, `merchants` as subtypes of `parties`; `cards`, `bank_accounts`, `merchant_accounts` as subtypes of `instruments`; and `transfer_participants` as the join table carrying sender/recipient facts per transfer.
2. **`docs/business-rules-specification.md`** — a researched Business Rules & Business Logic Specification covering registration, authentication, device management, card lifecycle, beneficiary management, transfer decision flow, limits, fees, fraud, AML, KYC/KYB, refunds/reversals/disputes, settlement, reconciliation, notifications, audit, account lifecycle, admin roles, and edge cases. Every rule in it is tagged with a classification (REGULATORY/NETWORK/BANK/SECURITY/FRAUD/AML/KYC/etc.) and enforcement level (MANDATORY/RECOMMENDED/OPTIONAL/PLATFORM_DECISION).

**Before writing any code:**
1. Read both documents in full.
2. Explore this repository to identify the existing language, framework, ORM/query layer, test framework, and folder conventions already in use. Do not assume a stack — detect it. If the repository is empty or this is a greenfield setup, stop and ask me which stack to use before scaffolding anything.
3. Confirm you understand the `parties`/`instruments`/`transfer_participants` model and the composite-FK integrity pattern `(instrument_id, party_id) → instruments(id, owner_party_id)` before touching transfer logic — this is the backbone every rule below builds on.

---

## Ground rules for this implementation (do not violate these)

- **MANDATORY/REGULATORY rules are not optional and are not platform decisions.** Implement them exactly as specified (phone-PINFL-card binding, OTP discipline, biometric triggers, card deactivation on security events, AML thresholds). Do not water them down for developer convenience.
- **Every numeric threshold, cooldown, or limit must be configuration, not a hardcoded constant** — especially anything marked in the spec as "PLATFORM_DECISION," "no authoritative Uzbek figure exists," or "do not invent a duration." Where the spec explicitly says a value is unknown/contractual, implement the mechanism with a configurable value and a clearly named default, and leave a `// TODO(external-contract):` comment pointing at the relevant section of the business rules doc — do not silently pick a number and move on.
- **AML/BCV thresholds must be stored as BCV multiples**, resolved against a versioned, effective-dated BCV value (currently 412,000 UZS from 1 August 2025) — never hardcode a UZS amount for a regulatory threshold.
- **The ledger is append-only.** No code path may UPDATE or DELETE a posted `ledger_entries` row. Corrections are new, balanced compensating entries (REVERSAL/ADJUSTMENT), never edits.
- **Every financial write is idempotent.** Any endpoint/service method that moves money, adds/removes a card, creates a refund, or processes a webhook must accept and enforce an idempotency key with a uniqueness constraint at the database level, not just in application logic.
- **Reserve-before-send.** Funds move from available → pending (reserved) before any external network call, and only move to posted on confirmed success. Never call out to UzCard/Humo/a bank before the reservation is durably written.
- **State machines reject illegal transitions.** Implement each state machine (User, Card, Beneficiary, Transfer, Merchant, Business, Refund, Settlement, Dispute) as an explicit, enforced set of allowed transitions — not an unvalidated status string. An illegal transition must raise/reject, not silently succeed.
- **Do not assume UzCard and Humo behave identically**, and do not assume all banks share limits — anywhere the spec marks something as network- or bank-specific, implement it as configuration keyed by network/bank, not a single global value.
- **Separate regulatory/AML/KYC checks from ordinary fraud/risk checks from ordinary validation** in code structure (distinct modules/services), matching the spec's classification — don't collapse them into one big "canTransfer()" function that mixes concerns.
- **Every security-sensitive account change is evaluated for temporary restrictions** — do not implement password change, phone change, device change, or account recovery without also wiring the corresponding restriction/step-up/card-deactivation logic from the spec.
- **Write tests for every MANDATORY rule** as you implement it (unit test for the rule logic, integration test for the end-to-end path where feasible). A rule isn't "done" without a test that proves the illegal case is actually blocked.

---

## Implementation plan — work in this order

Implement in phases. Do not start a later phase until the previous phase's tests pass. After each phase, give me a short summary of what was implemented, what was deferred (with the spec section reference), and what still needs an external contract value (per the spec's "Rules Requiring External Contracts" section).

### Phase 0 — Foundations (blocks everything else)
- Config layer for all thresholds/limits/cooldowns (versioned, effective-dated rows — not env vars for business-facing values).
- BCV value table (versioned, effective-dated) and a helper to resolve "N BCV" → current UZS amount at a given point in time.
- Idempotency key enforcement infrastructure (shared middleware/decorator + DB unique constraint pattern) reusable across transfer, card, refund, and webhook endpoints.
- Ledger invariant enforcement: append-only entries, balanced double-entry check, a repository layer that makes direct mutation of posted entries structurally impossible (not just discouraged by convention).
- The transfer state machine as an explicit, testable module (states + legal transitions from the spec's Transfer state machine), independent of any HTTP/API layer.

### Phase 1 — Registration & Authentication (MANDATORY/REGULATORY)
- Phone-PINFL-card matching rule (REG-01): reject registration/card-link when they don't belong to one person, with the documented close-relative exception path.
- Registration biometric liveness step-up (REG-02) — implement as a pluggable verification step (interface + provider adapter), since the spec notes provider SLAs are external/contractual — do not hardcode a specific vendor's API without confirming which one I'm integrating with.
- OTP discipline (REG-03/AUTH-03): 6-digit codes, 59-second expiry, 3-attempt cap, 15-minute lockout — as configurable values with these as defaults, enforced server-side with rate limiting.
- MFA login (AUTH-01) and new-device biometric step-up (AUTH-02).
- Age/minor handling (REG-04) if in scope for this MVP — confirm with me before implementing if unclear whether minors are in scope.

### Phase 2 — Device, Card, and Beneficiary Lifecycle
- Device trust tracking and new-device detection (DEV-01/DEV-02).
- **Card deactivation on security events**: new-device login or password recovery → linked cards move to INACTIVE, reactivate only via OTP (this is MANDATORY — do not skip even though it adds friction).
- Card add/ownership verification (CARD-01/CARD-02): OTP to cardholder's registered phone; block + notify on phone mismatch.
- Card removal rules (CARD-03): block removal while a transaction is pending on that card; preserve historical ledger linkage; revoke external tokens.
- Card and Beneficiary state machines as explicit modules, matching the spec.
- Beneficiary add/change rules (BEN-01/BEN-02), including the current P2P-per-transfer-OTP requirement, built as a configurable rule (the spec notes this may become institution-discretionary).

### Phase 3 — Transfer Decision Pipeline
Implement the corrected decision flow from the spec as a single, ordered pipeline (each stage a separate, independently testable unit):
1. Authenticate (+ step-up if triggered)
2. Idempotency check
3. Structural validation
4. KYC/KYB gate (including the 500 BCV full-identification trigger)
5. Limit check
6. Fraud/risk scoring
7. AML screening (threshold + sanctions/terrorism list)
8. Fee calculation
9. Reserve funds (ledger write)
10. Authorize with external network (idempotent, timeout-bounded)
11. Post to ledger on success / release reserve on failure
12. Notify + status update
13. Settlement (async)
14. Reconciliation (async)

Implement each transfer type (P2P/P2A/B2B/B2C/C2B) as a thin configuration of this shared pipeline (different party/instrument type constraints per the `transfer_types.allowed_sender_party_types`/`allowed_recipient_party_types` pattern from the schema doc), not as five duplicated pipelines.

Explicitly implement: **a regulatory/AML HOLD outranks a favorable fraud/risk score** — write a test proving this precedence.

### Phase 4 — Limits, Fees, Fraud, AML
- Limit engine: per-transaction/daily/weekly/monthly/velocity, keyed by user/card/device/network/transaction-type/KYC-level, backed by durable rolling counters (per the spec's `limit_counters` concept) — not recomputed by scanning transaction history on every check.
- Fee lifecycle (FEE-01/FEE-02): calculate → reserve → post → refund/reverse, including the two edge cases explicitly called out in the spec (transaction succeeds but fee posting fails; transaction fails but fee was already reserved) — write tests for both.
- Fraud anomaly scoring (FRD-02) mapping to ALLOW/ALLOW_WITH_STEP_UP/LIMIT/HOLD/MANUAL_REVIEW/DECLINE/ACCOUNT_FREEZE — implement as a scoring module with pluggable rule evaluators (velocity, amount anomaly, new-recipient, device/location anomaly) so new fraud signals can be added without rewriting the pipeline.
- AML threshold monitoring (AML-01) and sanctions/terrorism list screening (AML-02) — implement list screening as a pluggable external check (the actual list source is likely a government feed you'll need to integrate separately), with a HOLD decision and a `manual_reviews`/`transaction_holds` workflow (FLAGGED → UNDER_REVIEW → APPROVED/DECLINED/ESCALATED) and an explicit "who can release a hold" authorization check.

### Phase 5 — Refunds, Reversals, Disputes, Settlement, Reconciliation
- Implement REFUND, REVERSAL, VOID, CHARGEBACK, ADJUSTMENT as distinct operations with distinct ledger behavior per the spec — do not let "refund" and "reversal" collapse into one code path.
- Settlement batch processing and reconciliation (never auto-correct the ledger silently — mismatches produce a reviewable adjustment with an approval step and audit trail).

### Phase 6 — Notifications, Audit, Account Lifecycle, Admin
- Security-critical notification triggers (NOT-01) — implement as an event-driven hook off the state machines/pipeline stages, not scattered ad-hoc calls.
- Immutable audit logging (AUD-01) for every action the spec lists — implement as a cross-cutting concern (e.g., decorator/middleware/event listener) so it can't be forgotten on new endpoints; required fields: who, what, when, previous state, new state, reason, source/device, correlation ID.
- Account lifecycle states (ACTIVE/RESTRICTED/FROZEN/SUSPENDED/CLOSED) with the capability matrix from the spec enforced at the authorization layer.
- Admin role-based access (ADM-01) with least privilege — support/fraud-analyst/compliance-officer/finance/admin roles, each restricted to the specific actions the spec assigns them.

### Phase 7 — Edge Cases
Go through the spec's edge-case table (app closed mid-transfer, lost connectivity, authorization response lost, timeout, webhook arrives twice/out of order, late settlement, reversal after completion, DB-commit-but-event-publish-fails, consumer crash, service restart mid-transaction, reconciliation mismatch) and write an explicit test or handling path for each one. Use a transactional outbox pattern for the DB-commit/event-publish case if the stack supports it; otherwise flag the gap explicitly rather than leaving it silently unhandled.

---

## Deliverable format for each phase

For each phase, provide:
1. The code changes (following this repo's existing conventions).
2. Migrations for any new tables/columns needed (only what's justified — reference the schema doc's existing tables/fields first; only add what's missing, matching the "Database Impact" section of the business rules spec: `security_cooldowns`/`transfer_restrictions`, `device_trust`, `card_verification`/`beneficiary_verification`, `limit_counters`, `risk_events`/`risk_decisions`, `manual_reviews`/`transaction_holds`, `account_restrictions`).
3. Tests proving each MANDATORY rule is enforced and each illegal case is blocked.
4. A short list of anything deferred to a later phase, anything requiring an external contract/vendor decision from me, and any assumption you made that I should confirm.

Start with **Phase 0**. Read both reference documents fully first, report back on the detected stack and repo conventions, then begin.
