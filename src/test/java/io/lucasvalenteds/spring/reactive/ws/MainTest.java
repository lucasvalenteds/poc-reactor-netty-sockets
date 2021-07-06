package io.lucasvalenteds.spring.reactive.ws;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MainTest {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    private static final String SERVER_URL = "ws://" + SERVER_HOST + ":" + SERVER_PORT;

    private static final Sinks.Many<String> SINK = Sinks.many()
        .unicast()
        .onBackpressureBuffer();

    private static DisposableServer disposable;

    @BeforeAll
    static void beforeAll() {
        disposable = HttpServer.create()
            .host(SERVER_HOST)
            .port(SERVER_PORT)
            .route(router ->
                router
                    .ws("/client-to-server", new ClientToServerHandler(SINK))
                    .ws("/server-to-client", new ServerToClientHandler())
                    .ws("/duplex", new DuplexHandler())
                    .ws("/duplex-infinite", new DuplexInfiniteHandler())
                    .ws("/header", new HeaderHandler())
            )
            .bindNow();
    }

    @AfterAll
    static void afterAll() {
        disposable.disposeNow();
    }

    @DisplayName("Client sending data to server (client-to-server)")
    @Test
    void testClientToServer() {
        var clientDisposable = HttpClient.create()
            .baseUrl(SERVER_URL)
            .websocket()
            .uri("/client-to-server")
            .handle((in, out) ->
                out.sendByteArray(Mono.just("Awesome".getBytes()))
            )
            .subscribe();

        StepVerifier.create(SINK.asFlux().take(1))
            .assertNext(word -> assertEquals("Awesome", word))
            .expectComplete()
            .verify();

        clientDisposable.dispose();
    }

    @DisplayName("Client receiving data from server (server-to-client)")
    @Test
    void testServerToClient() {
        var client = HttpClient.create()
            .baseUrl(SERVER_URL)
            .websocket()
            .uri("/server-to-client");

        var response = client.handle((in, out) -> Flux.from(in.receive().asString()));

        StepVerifier.create(response)
            .expectNext("Hello")
            .expectNext("World")
            .expectNext(":)")
            .expectComplete()
            .verify();
    }

    @DisplayName("Client sending and receiving data from server (duplex)")
    @Test
    void testDuplex() {
        var client = HttpClient.create()
            .baseUrl(SERVER_URL)
            .websocket()
            .uri("/duplex");

        var response = client.handle((in, out) -> {
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

    @DisplayName("Client sending and receiving data from server infinitely (duplex)")
    @Test
    void testDuplexInfinite() {
        var client = HttpClient.create()
            .baseUrl(SERVER_URL)
            .websocket()
            .uri("/duplex-infinite");

        var response = client.handle((in, out) -> {
            out.sendString(Flux.just("hello", "world", ":)"))
                .then()
                .subscribe();

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

    @DisplayName("Server responding according to client headers")
    @ParameterizedTest(name = "Header Content-Type: {0}")
    @MethodSource("headerFixtures")
    void testHeaders(String contentType, String message) {
        var client = HttpClient.create()
            .baseUrl(SERVER_URL)
            .headers(headers -> headers.set(HttpHeaderNames.CONTENT_TYPE, contentType))
            .websocket()
            .uri("/header");

        var response = client.handle((in, out) ->
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
