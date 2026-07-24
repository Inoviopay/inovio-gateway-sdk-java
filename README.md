# Inovio Gateway SDK — Java

Port of the Node/TS reference (**W4** in [`../PLAN.md`](../PLAN.md)). Structurally
identical to the other SDKs; only ergonomics differ.

> **Status: alpha, local only.** Not published to Maven Central.

## Build / test

```bash
mvn test        # 19 conformance tests
python3 scripts/generate_enums.py   # regenerate enums from ../spec/spec-enums.json
```

**Java 11 baseline**, zero runtime dependencies (HTTP via `java.net.http`, and a
small flat-JSON reader rather than pulling in Jackson for a flat key/value
response).

## Java 11 and the sealed hierarchy (decision D3)

D3 was resolved to **Java 11 for enterprise reach** rather than 17+ ergonomics —
the gateway itself runs Java 8, and partners on older stacks can consume this.
The cost is real and worth knowing:

| Object model | Java 17+ would be | What this SDK does |
|---|---|---|
| `PaymentMethod` **sealed** (§3.2) | `sealed interface ... permits` | interface + `final` impls with **package-private constructors** |
| `Money`, refs, value types | `record` | hand-written `final` classes |
| Exhaustive status handling | switch expressions | `switch` + explicit default |

`PaymentMethod` is therefore **sealed by convention**: because every
implementation is `final` and its constructor is package-private, the only way
to build one is `PaymentMethods.card(...)` / `.token(...)` / `.savedCard(...)`.
Outside code cannot add a variant. What's lost is *compiler-enforced*
exhaustiveness — you get no error for an unhandled variant in a `switch`.

## Quick start

```java
InovioClient client = new InovioClient(
    new InovioClient.Credentials(user, password, "123"));

TransactionRequest req = new TransactionRequest(
    PaymentMethods.card("4111111111111111", "122030", "123"),
    new LineItem("SKU-1", 1, Money.of("10.00", "USD")))
    .idempotency("ORDER-555");          // retry-safe by default

TransactionResult r = client.sale(req);

switch (r.status()) {
    case APPROVED: /* fulfil */ break;
    case DECLINED: /* r.outcome().service(), r.serviceClassification() */ break;
    case PENDING:  /* r.nextAction() — 3DS challenge, redirect, voucher */ break;
    case RUNNING:
    case FAILED:   break;
}
```

## Five things that will surprise you

Identical semantics to the Node reference — see
[`../node/README.md`](../node/README.md) for the full rationale:

1. **A decline is not an exception.** `sale()` returns `DECLINED`. Exceptions
   mean you never got a payment answer. All exceptions are unchecked.
2. **No `approved()`/`declined()` accessors.** Only `status()` — so `PENDING`
   cannot be silently treated as failure.
3. **`settled()` is almost always `false` at response time** (batch flips it
   later); `conversion()` is non-null only on real FX.
4. **`status()` is the reconciliation primitive**, not just timeout recovery.
   `OrderStatus` gives `authorized/captured/refunded/net/outstanding`, derived
   the way the gateway's own `BATCH_PKG` derives them.
5. **`Money.of(1.25, "USD")` throws.** There is a `double` overload that exists
   purely to reject floating point with a clear message — pass `"1.25"` or a
   `BigDecimal`.

## Java-specific notes

- Amounts are `BigDecimal`; `Money.equals` uses `compareTo` so `"1.5"` equals
  `"1.50"`.
- `Card.toString()` deliberately prints only the last 4 digits.
- The timeout exception is **`GatewayTimeoutException`**, not
  `TimeoutException`, so it is never confused with
  `java.util.concurrent.TimeoutException`. It carries `xtlOrderId()` and a
  `recoveryHint()`.

## Enums are generated

`src/main/java/com/inoviopay/gateway/enums/` is generated from
`../spec/spec-enums.json` (decision **D1**). Do not edit. The
`retryable`/`terminal`/`stopRecurring` and AVS/CVV classifications are
**derived, not from the spec** — see [`../spec/README.md`](../spec/README.md).

## Conformance

`ConformanceTest` mirrors the shared corpus in
`../spec/conformance-fixtures.json`. Java has no zero-dependency JSON reader in
test scope, so the fixtures are transcribed as literal cases with test names
matching the fixture names — assertions are identical to the Node and Python
suites. If a fixture changes, update this file in the same commit.
