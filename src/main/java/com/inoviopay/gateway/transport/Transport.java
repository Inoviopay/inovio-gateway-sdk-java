package com.inoviopay.gateway.transport;

import com.inoviopay.gateway.errors.GatewayTimeoutException;
import com.inoviopay.gateway.errors.TransportException;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transport — form-encoded POST to pmt_service.cfm, plus response normalization.
 *
 * <p>Wire quirks are normalized ONCE, here, and never leak to the partner
 * (object model §2 principle 8):
 * <ul>
 *   <li>responses are case-inconsistent -&gt; keys upper-cased
 *   <li>{@code XTL_ORDER_ID} / {@code XTL_PO_ID} name the same thing
 *   <li>{@code PMT_L4} / {@code PMT_LAST4} name the same thing
 * </ul>
 */
public final class Transport {

    /** Spec §2.1. Sandbox host is configurable — confirm before non-local use. */
    public static final String PRODUCTION_ENDPOINT =
        "https://api.inoviopay.com/payment/pmt_service.cfm";
    public static final String SANDBOX_ENDPOINT =
        "https://api-uap.inoviopay.com/payment/pmt_service.cfm";

    private static final Map<String, String> ALIASES = new HashMap<>();

    static {
        ALIASES.put("XTL_PO_ID", "XTL_ORDER_ID");
        ALIASES.put("PMT_LAST4", "PMT_L4");
    }

    private Transport() {}

    public enum Environment {
        SANDBOX(SANDBOX_ENDPOINT),
        PRODUCTION(PRODUCTION_ENDPOINT);

        private final String endpoint;

        Environment(String endpoint) { this.endpoint = endpoint; }

        public String endpoint() { return endpoint; }
    }

    /** Spec §2.2: URL-encoded form body. */
    public static String formEncode(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (e.getValue() == null || e.getValue().isEmpty()) continue;
            if (sb.length() > 0) sb.append('&');
            sb.append(encode(e.getKey())).append('=').append(encode(e.getValue()));
        }
        return sb.toString();
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Normalize a raw gateway response into an upper-cased field map. Accepts
     * JSON (what we request) and falls back to form-encoded text.
     */
    public static Map<String, String> normalizeResponse(String body) {
        Map<String, String> out = new HashMap<>();
        String trimmed = body == null ? "" : body.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            // CCSTATUS answers with a COLUMNS/DATA table rather than flat
            // fields. Flattening would destroy the row structure, so pass it
            // through untouched for the client to expand.
            if (trimmed.contains("\"COLUMNS\"") && trimmed.contains("\"DATA\"")) {
                out.put("__TABULAR__", trimmed);
                return out;
            }
            parseFlatJson(trimmed, out);
            return out;
        }
        for (String pair : trimmed.split("&")) {
            if (pair.isEmpty()) continue;
            int i = pair.indexOf('=');
            String k = i == -1 ? pair : pair.substring(0, i);
            String v = i == -1 ? "" : pair.substring(i + 1);
            put(out, decode(k), decode(v));
        }
        return out;
    }

    private static String decode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void put(Map<String, String> out, String key, String value) {
        if (key == null || value == null) return;
        String k = key.toUpperCase().trim();
        out.put(k, value);
        String alias = ALIASES.get(k);
        if (alias != null && !out.containsKey(alias)) {
            out.put(alias, value);
        }
    }

    /**
     * Minimal flat JSON reader.
     *
     * <p>The gateway returns a flat object of string/number values, so a full
     * JSON library would be a dependency bought for nothing. Nested objects are
     * flattened with an underscore-joined prefix, matching the other SDKs.
     */
    static void parseFlatJson(String json, Map<String, String> out) {
        int i = 0;
        List<String> prefix = new ArrayList<>();
        try {
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '"') {
                    int[] end = new int[1];
                    String key = readString(json, i, end);
                    i = end[0];
                    i = skipWs(json, i);
                    if (i < json.length() && json.charAt(i) == ':') {
                        i = skipWs(json, i + 1);
                        char v = json.charAt(i);
                        if (v == '{') {
                            prefix.add(key);
                            i++;
                        } else if (v == '"') {
                            String val = readString(json, i, end);
                            i = end[0];
                            put(out, join(prefix, key), val);
                        } else {
                            int j = i;
                            while (j < json.length() && ",}]".indexOf(json.charAt(j)) == -1) j++;
                            put(out, join(prefix, key), json.substring(i, j).trim());
                            i = j;
                        }
                    }
                } else {
                    if (c == '}' && !prefix.isEmpty()) prefix.remove(prefix.size() - 1);
                    i++;
                }
            }
        } catch (RuntimeException e) {
            throw new TransportException("gateway returned malformed JSON", e);
        }
    }

    private static String join(List<String> prefix, String key) {
        if (prefix.isEmpty()) return key;
        return String.join("_", prefix) + "_" + key;
    }

    private static int skipWs(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        return i;
    }

    private static String readString(String s, int start, int[] endOut) {
        StringBuilder sb = new StringBuilder();
        int i = start + 1;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'u':
                        sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                        i += 4;
                        break;
                    default: sb.append(n);
                }
            } else if (c == '"') {
                endOut[0] = i + 1;
                return sb.toString();
            } else {
                sb.append(c);
            }
            i++;
        }
        endOut[0] = i;
        return sb.toString();
    }

    public static Map<String, String> send(
        String endpoint, HttpClient client, long timeoutMs,
        Map<String, String> params, String idempotencyKey) {

        String body = formEncode(params);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/x-www-form-urlencoded");
        headers.put("Accept", "application/json");

        HttpClient.Response res;
        try {
            res = client.post(endpoint, body, headers, timeoutMs);
        } catch (HttpClient.TimeoutSignal e) {
            throw new GatewayTimeoutException(
                "gateway did not respond within " + timeoutMs
                    + "ms — transaction state is UNKNOWN",
                timeoutMs, idempotencyKey);
        } catch (RuntimeException e) {
            throw new TransportException("gateway request failed: " + e.getMessage(), e);
        }

        if (res.status() < 200 || res.status() >= 300) {
            throw new TransportException("gateway returned HTTP " + res.status());
        }
        return normalizeResponse(res.body());
    }
}
