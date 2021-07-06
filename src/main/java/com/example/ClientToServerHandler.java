package com.example;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Sinks;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

import java.util.function.BiFunction;

public class ClientToServerHandler implements BiFunction<WebsocketInbound, WebsocketOutbound, Publisher<Void>> {

    private final Sinks.Many<String> sink;

    public ClientToServerHandler(Sinks.Many<String> sink) {
        this.sink = sink;
    }

    @Override
    public Publisher<Void> apply(WebsocketInbound in, WebsocketOutbound out) {
        return in.aggregateFrames()
            .receive()
            .asString()
            .doOnNext(sink::tryEmitNext)
            .then(out.sendClose());
    }
}
