package io.lucasvalenteds.spring.reactive.ws;

import io.netty.handler.codec.http.HttpHeaderNames;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;
import reactor.test.StepVerifier;

class MainTest {

    private static final ClientToServerHandler clientToServerHandler = new ClientToServerHandler();
    private final HttpServer server = HttpServer.create()
        .host("localhost")
        .port(8080)
        .route(router ->
            router
                .ws("/client-to-server", clientToServerHandler)
                .ws("/server-to-client", new ServerToClientHandler())
                .ws("/duplex", new DuplexHandler())
                .ws("/duplex-infinite", new DuplexInfiniteHandler())
                .ws("/header", new HeaderHandler())
        );
    private DisposableServer disposable;

    @BeforeEach
    void startServer() {
        disposable = server.bindNow();
    }

    @AfterEach
    void stopServer() {
        disposable.disposeNow();
    }

    @Test
    void testItCanSendData() throws InterruptedException {
        assertNull(clientToServerHandler.getWordReceived());

        HttpClient.WebsocketSender client = HttpClient.create()
            .baseUrl("ws://localhost:8080")
            .websocket()
            .uri("/client-to-server");

        Flux<Void> response = client.handle((in, out) ->
            out.sendByteArray(Mono.just("Awesome".getBytes()))
        );

        StepVerifier.create(response)
            .expectComplete()
            .verify();

        Thread.sleep(500);
        assertEquals("Awesome", clientToServerHandler.getWordReceived());
    }

    @Test
    void testItCanReceiveData() {
        HttpClient.WebsocketSender client = HttpClient.create()
            .baseUrl("ws://localhost:8080")
            .websocket()
            .uri("/server-to-client");

        Flux<String> response = client.handle((in, out) -> Flux.from(in.receive().asString()));

        StepVerifier.create(response)
            .expectNext("Hello")
            .expectNext("World")
            .expectNext(":)")
            .expectComplete()
            .verify();
    }

    @Test
    void testItCanSendAndReceiveData() {
        HttpClient.WebsocketSender client = HttpClient.create()
            .baseUrl("ws://localhost:8080")
            .websocket()
            .uri("/duplex");

        Flux<String> response = client.handle((in, out) -> {
            out.sendString(Flux.just("1", "2", "3"))
                .then().subscribe();

            return in.receive()
                .asString()
                .take(3);
        });

        StepVerifier.create(response)
            .expectNext("2")
            .expectNext("4")
            .expectNext("6")
            .expectComplete()
            .verify();
    }

    @Test
    void testServerCanStreamDataForever() {
        HttpClient.WebsocketSender client = HttpClient.create()
            .baseUrl("ws://localhost:8080")
            .websocket()
            .uri("/duplex-infinite");

        Flux<String> response = client.handle((in, out) -> {
            out.sendString(Flux.just("hello", "world", ":)")).then().subscribe();

            return in.receive().asString();
        });

        StepVerifier.create(response)
            .expectNext("HELLO")
            .expectNext("WORLD")
            .expectNext(":)")
            .thenAwait(Duration.ofMillis(2000))
            .thenCancel()
            .verify();
    }

    static Stream<Arguments> headerFixtures() {
        return Stream.of(
            Arguments.of("text/plain", "Hello World!"),
            Arguments.of("application/json", "{\"message\":\"Hello World!\"}")
        );
    }

    @ParameterizedTest
    @MethodSource("headerFixtures")
    void testItCanInspectHeaders(String contentType, String message) {
        HttpClient.WebsocketSender client = HttpClient.create()
            .baseUrl("ws://localhost:8080")
            .headers(headers -> headers.set(HttpHeaderNames.CONTENT_TYPE, contentType))
            .websocket()
            .uri("/header");

        Flux<String> response = client.handle((in, out) ->
            in.receive()
                .asString()
                .take(1)
        );

        StepVerifier.create(response)
            .expectNext(message)
            .expectComplete()
            .verify();
    }
}
