package io.lucasvalenteds.spring.reactive.ws;

import java.util.function.BiFunction;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

public class ServerToClientHandler implements BiFunction<WebsocketInbound, WebsocketOutbound, Publisher<Void>> {
    @Override
    public Publisher<Void> apply(WebsocketInbound in, WebsocketOutbound out) {
        return out.send(
            ByteBufFlux.fromString(
                Flux.just("Hello", "World", ":)")
            )
        );
    }
}
