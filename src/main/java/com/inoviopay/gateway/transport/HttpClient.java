package com.inoviopay.gateway.transport;

import java.util.Map;

/** Injectable so hosts can supply their own client (and tests can mock). */
public interface HttpClient {

    Response post(String url, String body, Map<String, String> headers, long timeoutMs);

    /** Implementations throw this to signal a timeout rather than a generic failure. */
    class TimeoutSignal extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public TimeoutSignal(String message, Throwable cause) {
            super(message, cause);
        }
    }

    final class Response {
        private final int status;
        private final String body;

        public Response(int status, String body) {
            this.status = status;
            this.body = body;
        }

        public int status() { return status; }
        public String body() { return body; }
    }
}
