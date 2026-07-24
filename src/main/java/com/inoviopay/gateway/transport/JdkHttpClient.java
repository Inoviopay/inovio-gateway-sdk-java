package com.inoviopay.gateway.transport;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;

/** Default client — {@code java.net.http} from the JDK 11 baseline, no dependency. */
public final class JdkHttpClient implements HttpClient {

    private final java.net.http.HttpClient delegate = java.net.http.HttpClient.newHttpClient();

    @Override
    public Response post(String url, String body, Map<String, String> headers, long timeoutMs) {
        HttpRequest.Builder b = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofMillis(timeoutMs))
            .POST(HttpRequest.BodyPublishers.ofString(body));
        headers.forEach(b::header);
        try {
            HttpResponse<String> res =
                delegate.send(b.build(), HttpResponse.BodyHandlers.ofString());
            return new Response(res.statusCode(), res.body());
        } catch (HttpTimeoutException e) {
            throw new TimeoutSignal("request timed out", e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
