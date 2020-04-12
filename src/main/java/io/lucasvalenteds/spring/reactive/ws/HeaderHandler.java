package io.lucasvalenteds.spring.reactive.ws;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import java.util.Optional;
import java.util.function.BiFunction;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

public class HeaderHandler implements BiFunction<WebsocketInbound, WebsocketOutbound, Publisher<Void>> {
    @Override
    public Publisher<Void> apply(WebsocketInbound in, WebsocketOutbound out) {
        String header = in.headers().getAsString(HttpHeaderNames.CONTENT_TYPE);
        String contentType = Optional.ofNullable(header).orElse(HttpHeaderValues.TEXT_PLAIN.toString());

        if (contentType.equals(HttpHeaderValues.TEXT_PLAIN.toString())) {
            return out.sendString(Flux.just("Hello World!"));
        } else {
            return out.sendString(Flux.just("{\"message\":\"Hello World!\"}"));
        }
    }
}
