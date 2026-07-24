# Inovio Gateway SDK — Java

Port of the Node/TS reference (**W4** of the internal SDK plan). Structurally
identical to the other SDKs; only ergonomics differ.

> **Status: alpha, local only.** Not published to Maven Central.

## Build / test

```bash
mvn test        # 19 conformance tests
python3 scripts/generate_enums.py   # regenerate enums from spec/spec-enums.json
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

Identical semantics to the Node reference — see the Node reference SDK's README for the full rationale:

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
`spec/spec-enums.json` (decision **D1**). Do not edit. The
`retryable`/`terminal`/`stopRecurring` and AVS/CVV classifications are
**derived, not from the spec** — see [`spec/README.md`](spec/README.md).

## Tokenization (spec §4.8)

`tokenize()` exchanges a PAN for a single-use `TOKEN_GUID` that replaces
`PMT_NUMB` on a later sale or authorize. It hits a **different endpoint**
(`token_service.cfm`) with **different auth** — HMAC headers, not
username/password.

You need a **site key**: a per-site HMAC secret issued by Inovio support. It is
*not* your gateway password. Without it the service answers error 121.

Two things the SDK handles that the spec will mislead you on:

**1. The signed message excludes the PAN.** The v4.14 PDF's §4.8.1.2 note says
the HMAC covers `card_pan`, and its worked example agrees — but the gateway
does not. `CRPT.TOKEN_PKG` validates:

```
hmac_sha256(timestamp || unique_id || site_id, site_key)
```

Signing with the PAN included fails with error 121. This SDK follows the
gateway, verified against live T1.

**2. A token replaces the PAN only.** The transaction still needs the expiry
(and CVV where the processor asks), so `tokenize()` carries them forward onto
the returned token. Sending a bare `TOKEN_GUID` yields API 110 `Required field`
on `REF_FIELD=pmt_expiry`.

BIN metadata (`brand`, `bank`, `country`, ...) is best-effort: the service
returns those keys **empty** when the BIN is not in its lookup table, and the
SDK normalizes blanks to null/undefined so you can test for presence.

⚠️ This is a **server-side** call — the PAN passes through your infrastructure,
so you remain in PCI scope. The low-scope path is the browser Hosted Fields
client, which is not built yet.

## Vendored spec artifacts

This repo **stands alone**: `spec/spec-enums.json` and
`spec/conformance-fixtures.json` are committed copies, so a fresh clone builds,
tests and regenerates with no sibling checkout, submodule or network fetch.

They are not the editable source — they are produced upstream in the internal
`inoviov2` workspace (`api-sdk/spec/`), where the extraction pipeline and its
validator live. To pull an upstream change in:

```bash
./scripts/sync-spec.sh /path/to/inoviov2/api-sdk/spec
```

Then regenerate the enums, run the suite, and commit the spec change together
with the generated code it produces.

**This is a coordinated change.** The other Inovio SDK repos vendor the same two
files; if they are not synced in step, the SDKs silently stop agreeing — which
is exactly what the shared conformance corpus exists to prevent.

## Conformance

`ConformanceTest` mirrors the shared corpus in
`spec/conformance-fixtures.json`. Java has no zero-dependency JSON reader in
test scope, so the fixtures are transcribed as literal cases with test names
matching the fixture names — assertions are identical to the Node and Python
suites. If a fixture changes, update this file in the same commit.
