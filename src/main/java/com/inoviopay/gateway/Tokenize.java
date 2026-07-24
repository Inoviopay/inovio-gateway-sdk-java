package com.inoviopay.gateway;

import com.inoviopay.gateway.errors.ConfigurationException;
import com.inoviopay.gateway.errors.ValidationException;
import com.inoviopay.gateway.model.Card;
import com.inoviopay.gateway.model.PaymentMethods;
import com.inoviopay.gateway.model.Token;
import com.inoviopay.gateway.transport.HttpClient;
import com.inoviopay.gateway.transport.Transport;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ephemeral tokenization (spec §4.8).
 *
 * <p>This is NOT the transaction service: a different endpoint
 * ({@code token_service.cfm}), a different request shape, and HMAC header auth
 * instead of username/password. Exchanging a PAN here yields a single-use
 * {@code TOKEN_GUID} that replaces {@code PMT_NUMB} on a later sale/authorize.
 *
 * <p>Signature construction:
 * <pre>
 * X-SIGNATURE = hex(HMAC_SHA256(siteKey, timestamp + uniqueId + siteId))
 * X-TIMESTAMP = YYYYMMDDHHMMSS, UTC, valid for 300 seconds
 * </pre>
 *
 * <p><strong>⚠️ The v4.14 PDF is self-contradictory here.</strong> Its §4.8.1.2
 * note claims the message also includes {@code card_pan}, and the document's
 * worked example agrees. The gateway does NOT do this — {@code CRPT.TOKEN_PKG}
 * validates {@code hmac_sha256(utc || unique_id || site_id, site_key)}. Signing
 * with the PAN included yields error 121 "Get CCtoken GUID signature match
 * fail". Verified against the live T1 token service; this follows the gateway,
 * not the document.
 *
 * <p>The site key is provisioned per merchant site and is NOT the gateway
 * password — obtain it from Inovio support.
 */
public final class Tokenize {

    private static final DateTimeFormatter TS =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final SecureRandom RANDOM = new SecureRandom();

    private Tokenize() {}

    /** BIN metadata the token service returns alongside the token. */
    public static final class CardInfo {
        public final String brand;
        public final String type;
        public final String bank;
        public final String country;
        public final String accountFundSource;
        public final String cardClass;

        CardInfo(String brand, String type, String bank, String country,
                 String accountFundSource, String cardClass) {
            this.brand = brand;
            this.type = type;
            this.bank = bank;
            this.country = country;
            this.accountFundSource = accountFundSource;
            this.cardClass = cardClass;
        }
    }

    public static final class Result {
        private final Token token;
        private final CardInfo card;
        private final String tokenIp;
        private final String tokenReqId;
        private final Map<String, String> raw;

        Result(Token token, CardInfo card, String tokenIp, String tokenReqId,
               Map<String, String> raw) {
            this.token = token;
            this.card = card;
            this.tokenIp = tokenIp;
            this.tokenReqId = tokenReqId;
            this.raw = Collections.unmodifiableMap(new LinkedHashMap<>(raw));
        }

        public Token token() { return token; }
        public CardInfo card() { return card; }
        /** Gateway-side IP recorded for the token request. */
        public String tokenIp() { return tokenIp; }
        /** Token service request id — quote this to support. */
        public String tokenReqId() { return tokenReqId; }
        public Map<String, String> raw() { return raw; }
    }

    /** UTC timestamp in the token service's YYYYMMDDHHMMSS format. */
    public static String timestamp() {
        return ZonedDateTime.now(ZoneOffset.UTC).format(TS);
    }

    /**
     * Build the request signature.
     *
     * <p>Exposed so a caller can verify their site key without a live call.
     */
    public static String signRequest(String siteKey, String timestamp,
                                     String uniqueId, String siteId) {
        return hmacHex(siteKey, timestamp + uniqueId + siteId);
    }

    /**
     * Verify the response signature the token service returns.
     *
     * <p>Per {@code CRPT.TOKEN_PKG} the gateway signs
     * {@code timestamp + tokenReqId + rawResponseBody} with the same site key.
     */
    public static boolean verifyResponse(String siteKey, String timestamp,
                                         String tokenReqId, String rawBody,
                                         String signature) {
        String expected = hmacHex(siteKey, timestamp + tokenReqId + rawBody.trim());
        return signature != null && expected.equalsIgnoreCase(signature);
    }

    private static String hmacHex(String key, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(out.length * 2);
            for (byte b : out) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", e);
        }
    }

    static String randomUniqueId() {
        byte[] b = new byte[16];
        RANDOM.nextBytes(b);
        StringBuilder sb = new StringBuilder(32);
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    /** Blank values mean "BIN not in the lookup table", not "empty string". */
    private static String blankToNull(String v) {
        return (v == null || v.trim().isEmpty()) ? null : v;
    }

    static Result tokenize(Card card, String endpoint, HttpClient http, long timeoutMs,
                           String siteId, String siteKey, String apiVersion,
                           String uniqueId) {
        if (siteKey == null || siteKey.isEmpty()) {
            throw new ValidationException(
                "tokenize requires a siteKey — the per-site HMAC secret from Inovio "
                    + "support. It is NOT your gateway password.");
        }
        String uid = (uniqueId == null || uniqueId.isEmpty()) ? randomUniqueId() : uniqueId;
        if (uid.length() > 32) {
            throw new ValidationException("tokenize: uniqueId must be at most 32 characters");
        }
        String ts = timestamp();

        Map<String, String> params = new LinkedHashMap<>();
        // The token service takes CARD_PAN — not PMT_NUMB, and no expiry/CVV.
        params.put("CARD_PAN", card.number());
        params.put("SITE_ID", siteId);
        params.put("UNIQUE_ID", uid);
        params.put("REQUEST_API_VERSION", apiVersion);
        params.put("REQUEST_RESPONSE_FORMAT", "JSON");

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-SIGNATURE", signRequest(siteKey, ts, uid, siteId));
        headers.put("X-TIMESTAMP", ts);

        Map<String, String> raw =
            Transport.send(endpoint, http, timeoutMs, params, null, headers);

        String guid = raw.get("TOKEN_GUID");
        if (guid == null || guid.isEmpty()) {
            String message = raw.get("ERROR_MESSAGE");
            if (message == null) message = "token service did not return a TOKEN_GUID";
            if ("121".equals(raw.get("ERROR_CODE"))) {
                message += " (signature mismatch — check the site key, and that the "
                    + "signed message is timestamp+uniqueId+siteId with NO card_pan)";
            }
            throw new ConfigurationException(message, null, raw);
        }

        // BIN metadata is best-effort: the service returns these keys EMPTY when
        // the BIN is not in its lookup table (observed on live T1).
        CardInfo info = new CardInfo(
            blankToNull(raw.get("CARD_BRAND_NAME")),
            blankToNull(raw.get("CARD_TYPE")),
            blankToNull(raw.get("CARD_BANK")),
            blankToNull(raw.get("CARD_COUNTRY")),
            blankToNull(raw.get("CARD_ACCOUNT_FUND_SOURCE")),
            blankToNull(raw.get("CARD_CLASS")));

        // Carry expiry/cvv forward: the token replaces the PAN, but the
        // transaction service still needs them (§4.8.2).
        return new Result(
            PaymentMethods.token(guid, card.expiry(), card.cvv()),
            info, raw.get("TOKEN_IP"), raw.get("TOKEN_REQID"), raw);
    }
}
