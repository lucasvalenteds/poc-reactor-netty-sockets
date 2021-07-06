package com.example;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

import java.util.function.BiFunction;

public class HeaderHandler implements BiFunction<WebsocketInbound, WebsocketOutbound, Publisher<Void>> {

    @Override
    public Publisher<Void> apply(WebsocketInbound in, WebsocketOutbound out) {
        return Mono.just(in.headers())
            .map(headers -> headers.getAsString(HttpHeaderNames.CONTENT_TYPE))
            .filter(contentType -> contentType.equals(HttpHeaderValues.TEXT_PLAIN.toString()))
            .flatMap(it -> Mono.just("Hello World!"))
            .switchIfEmpty(Mono.just("{\"message\":\"Hello World!\"}"))
            .flatMap(responseBody -> out.sendString(Flux.just(responseBody)).then());
    }
}
