package com.redclubes.backend.netpagos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.net.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class NetPagosHttpGatewayTests {
    HttpServer server;
    NetPagosHttpGateway gateway;
    final ObjectMapper json = new ObjectMapper();
    final AtomicInteger calls = new AtomicInteger();
    final AtomicReference<String> receivedBody = new AtomicReference<>();
    final AtomicReference<String> receivedKey = new AtomicReference<>();
    final AtomicReference<String> receivedPath = new AtomicReference<>();
    final AtomicReference<String> reply = new AtomicReference<>();
    int code = 201;
    String uuid = "12345678-1234-1234-1234-123456789012";
    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/preorder", e -> {
            calls.incrementAndGet(); receivedPath.set(e.getRequestURI().getPath());
            receivedKey.set(e.getRequestHeaders().getFirst("x-api-key"));
            receivedBody.set(new String(e.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = reply.get().getBytes(StandardCharsets.UTF_8);
            e.getResponseHeaders().set("Content-Type", "application/json");
            e.getResponseHeaders().set("Location", "/preorder/redirected");
            e.sendResponseHeaders(code, bytes.length);
            try (var stream = e.getResponseBody()) { stream.write(bytes); }
        });
        server.start();
        gateway = new NetPagosHttpGateway(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/preorder"),
                "fictional-test-api-key", json, HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build());
        reply.set(created("https://stg-netpagos.provincianet.com.ar/v1/home/" + uuid));
    }
    @AfterEach void stop() { server.stop(0); }
    String created(String url) { return "{\"code\":201,\"data\":{\"preorder_uuid\":\"" + uuid + "\",\"url\":\"" + url + "\"}}"; }
    NetPagosGateway.Create payload() { return new NetPagosGateway.Create(new NetPagosGateway.Payer("Ana", "Ficticia", "test@example.invalid", "1", "12345678", "3", true),
            List.of(new NetPagosGateway.Payment("100.01", "Cuota ficticia", "00001234567", "TEST_001"))); }
    @Test void postsExactDocumentedContractWithStringAmountsAndSecretOnlyInHeader() throws Exception {
        var created = gateway.create(payload()); assertEquals(uuid, created.uuid());
        var body = json.readTree(receivedBody.get());
        assertTrue(body.path("payer").path("locked_payer").asBoolean());
        assertEquals("3", body.path("payer").path("gender").asText());
        assertEquals("1", body.path("payer").path("document_type").asText());
        assertTrue(body.path("payments").get(0).path("amount").isTextual());
        assertEquals("100.01", body.path("payments").get(0).path("amount").asText());
        assertEquals("fictional-test-api-key", receivedKey.get());
        assertFalse(receivedBody.get().contains("fictional-test-api-key")); assertEquals(1, calls.get());
    }
    @Test void getsStatusByUuidAndPreservesMissingPerItemPaidAsUnknown() {
        code = 200;
        reply.set("{\"code\":200,\"data\":{\"uuid\":\"" + uuid + "\",\"status\":\"parcial\",\"total_amount\":\"100.01\","
                + "\"payer\":{\"document_number\":\"12345678\"},\"payments\":[{\"amount\":\"100.01\",\"barcode\":\"00001234567\",\"service\":\"TEST_001\"}]}}");
        var result = gateway.status(uuid); assertEquals("parcial", result.status());
        assertNull(result.payments().getFirst().paid()); assertEquals("/preorder/" + uuid, receivedPath.get());
    }
    @Test void providerErrorsAreSanitizedAndPostIsNeverRetried() {
        code = 422; reply.set("{\"message\":\"sensitive provider detail\"}");
        var error = assertThrows(NetPagosGateway.Failure.class, () -> gateway.create(payload()));
        assertFalse(error.getMessage().contains("sensitive")); assertEquals(1, calls.get());
    }
    @Test void refusesRedirectAndDoesNotForwardApiKeyToAnotherEndpoint() {
        code = 302; assertThrows(NetPagosGateway.Failure.class, () -> gateway.create(payload())); assertEquals(1, calls.get());
    }
    @Test void refusesUnexpectedCheckoutHostsUserinfoAndNonHttps() {
        for (String url : List.of("https://evil.example/", "http://stg-netpagos.provincianet.com.ar/",
                "https://user@stg-netpagos.provincianet.com.ar/", "https://stg-netpagos.provincianet.com.ar.evil.example/")) {
            reply.set(created(url)); assertThrows(NetPagosGateway.Failure.class, () -> gateway.create(payload()));
        }
    }
    @Test void refusesOversizedBodyMalformedJsonAndInconsistentResponseCode() {
        for (String body : List.of(" ".repeat(70000), "not json", "{\"code\":200,\"data\":{}}")) {
            reply.set(body); assertThrows(NetPagosGateway.Failure.class, () -> gateway.create(payload()));
        }
    }
    @Test void refusesInvalidUuidBeforePerformingNetworkRequest() {
        assertThrows(NetPagosGateway.Failure.class, () -> gateway.status("../bad")); assertEquals(0, calls.get());
    }
}
