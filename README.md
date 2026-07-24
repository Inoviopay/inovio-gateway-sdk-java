# Inovio Gateway SDK — Java

The Inovio payment gateway for Java 11+. Card transactions — authorize, capture,
refund, tokenize — with a typed API and no runtime dependencies.

> **Status: alpha.** Not yet published to Maven Central.

## Build / test

```bash
mvn test        # 19 conformance tests
python3 scripts/generate_enums.py   # regenerate enums from spec/spec-enums.json
```

**Java 11 baseline**, zero runtime dependencies (HTTP via `java.net.http`, and a
small flat-JSON reader rather than pulling in Jackson for a flat key/value
response).

## Java 11 and the sealed hierarchy

This SDK targets **Java 11** for broad reach rather than newer-JDK ergonomics.
The cost is real and worth knowing:

| Type | Java 17+ would be | What this SDK does |
|---|---|---|
| `PaymentMethod` **sealed** | `sealed interface ... permits` | interface + `final` impls with **package-private constructors** |
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

The behaviours below are worth internalizing before you integrate:

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

## Classifier fields are our interpretation, not the spec

Some fields the SDK gives you are **derived by us from the response codes, not
returned by the gateway** — and you will branch real logic on them, so it is
worth knowing which:

- **`serviceClassification().retryable()` / `terminal()` / `stopRecurring()`** —
  your dunning logic decides whether to re-try a declined charge based on these.
  We set them from the service response code; the gateway does not send them.
- **`avs().classification()`** — `positive` / `partial` / `negative` /
  `neutral`. `partial` means some elements matched and some did not (e.g. street
  matches but postal code does not). **Whether a partial AVS result is
  acceptable is your risk decision** — the SDK reports the classification and
  deliberately does not accept or reject for you.

If you need the raw gateway value instead of our label, every result carries a
`raw()` map with the verbatim response fields.

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
does not. The gateway actually validates:

```
hmac_sha256(timestamp || unique_id || site_id, site_key)
```

Signing with the card number included fails with error 121. This SDK signs
the way the gateway expects.

**2. A token replaces the PAN only.** The transaction still needs the expiry
(and CVV where the processor asks), so `tokenize()` carries them forward onto
the returned token. Sending a bare `TOKEN_GUID` yields API 110 `Required field`
on `REF_FIELD=pmt_expiry`.

BIN metadata (`brand`, `bank`, `country`, ...) is best-effort: the service
returns those keys **empty** when the BIN is not in its lookup table, and the
SDK normalizes blanks to null/undefined so you can test for presence.

⚠️ `tokenize()` runs on your server, so the card number passes through it. To
keep the number in the cardholder's browser instead, use the browser Hosted
Fields client (not yet available).
