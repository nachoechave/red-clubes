package com.redclubes.backend.netpagos;

import com.fasterxml.jackson.databind.*;
import java.net.URI;
import java.net.http.*;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Flow;

/** Fixed staging origin. No POST retries or redirects; no production host exists in this preparation. */
public class NetPagosHttpGateway implements NetPagosGateway {
    static final URI STAGING = URI.create("https://stg-service-payment.provincianet.com.ar/api/v1/service/preorder");
    private final URI endpoint;
    private final String apiKey;
    private final ObjectMapper json;
    private final HttpClient client;
    NetPagosHttpGateway(String apiKey, ObjectMapper json) {
        this(STAGING, apiKey, json, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build());
    }
    // Package-private dependency injection for a local HTTP contract server in tests.
    NetPagosHttpGateway(URI endpoint, String apiKey, ObjectMapper json, HttpClient client) {
        this.endpoint = endpoint; this.apiKey = apiKey; this.json = json; this.client = client;
    }
    @Override public Created create(Create payload) {
        try {
            JsonNode data = exchange(HttpRequest.newBuilder(endpoint).POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload))), 201);
            String uuid = uuid(text(data, "preorder_uuid"));
            String url = text(data, "url");
            URI checkout = URI.create(url);
            if (!"https".equals(checkout.getScheme()) || !"stg-netpagos.provincianet.com.ar".equals(checkout.getHost())
                    || checkout.getUserInfo() != null || checkout.getFragment() != null
                    || (checkout.getPort() != -1 && checkout.getPort() != 443)) throw new Failure();
            return new Created(uuid, url);
        } catch (Failure e) { throw e; }
        catch (Exception e) { throw new Failure(); }
    }
    @Override public Status status(String uuid) {
        JsonNode data = exchange(HttpRequest.newBuilder(URI.create(endpoint + "/" + uuid(uuid))).GET(), 200);
        var items = new ArrayList<Item>();
        JsonNode payments = data.get("payments");
        if (payments == null || !payments.isArray() || payments.isEmpty() || payments.size() > 10) throw new Failure();
        for (var p : payments) {
            JsonNode paid = p.get("paid");
            if (paid != null && !paid.isBoolean()) throw new Failure();
            items.add(new Item(text(p, "amount"), text(p, "barcode"), text(p, "service"), paid == null ? null : paid.asBoolean()));
        }
        return new Status(uuid(text(data, "uuid")), text(data, "status"), text(data, "total_amount"),
                text(data.path("payer"), "document_number"), List.copyOf(items));
    }
    private JsonNode exchange(HttpRequest.Builder request, int expectedStatus) {
        CompletableFuture<HttpResponse<byte[]>> pending = null;
        try {
            pending = client.sendAsync(request.timeout(Duration.ofSeconds(10)).header("x-api-key", apiKey)
                    .header("Content-Type", "application/json").header("Accept", "application/json").build(),
                    info -> new LimitedBody());
            var response = pending.get(10, TimeUnit.SECONDS);
            if (response.statusCode() != expectedStatus) throw new Failure();
            JsonNode root = json.readTree(response.body());
            if (root == null || root.path("code").asInt(-1) != expectedStatus || !root.path("data").isObject()) throw new Failure();
            return root.get("data");
        } catch (InterruptedException e) { if (pending != null) pending.cancel(true); Thread.currentThread().interrupt(); throw new Failure(); }
        catch (TimeoutException e) { if (pending != null) pending.cancel(true); throw new Failure(); }
        catch (Failure e) { throw e; }
        catch (Exception e) { throw new Failure(); }
    }
    private static class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final CompletableFuture<byte[]> body = new CompletableFuture<>();
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private Flow.Subscription subscription;
        @Override public CompletionStage<byte[]> getBody() { return body; }
        @Override public void onSubscribe(Flow.Subscription value) { subscription = value; value.request(1); }
        @Override public void onNext(List<ByteBuffer> buffers) {
            for (var buffer : buffers) {
                if (bytes.size() + buffer.remaining() > 65536) { subscription.cancel(); body.completeExceptionally(new Failure()); return; }
                byte[] chunk = new byte[buffer.remaining()]; buffer.get(chunk); bytes.writeBytes(chunk);
            }
            subscription.request(1);
        }
        @Override public void onError(Throwable error) { body.completeExceptionally(new Failure()); }
        @Override public void onComplete() { body.complete(bytes.toByteArray()); }
    }
    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().length() > 2048) throw new Failure();
        return value.asText();
    }
    static String uuid(String value) {
        try { if (!UUID.fromString(value).toString().equals(value)) throw new Failure(); return value; }
        catch (IllegalArgumentException e) { throw new Failure(); }
    }
}
