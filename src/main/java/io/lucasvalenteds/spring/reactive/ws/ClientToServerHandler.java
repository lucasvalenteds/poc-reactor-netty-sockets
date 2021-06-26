package io.lucasvalenteds.spring.reactive.ws;

import org.reactivestreams.Publisher;
import reactor.core.publisher.DirectProcessor;
import reactor.core.publisher.FluxSink;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

import java.util.function.BiFunction;

public class ClientToServerHandler implements BiFunction<WebsocketInbound, WebsocketOutbound, Publisher<Void>> {

    private final FluxSink<String> sink;

    public ClientToServerHandler(DirectProcessor<String> processor) {
        this.sink = processor.sink();
    }

    @Override
    public Publisher<Void> apply(WebsocketInbound in, WebsocketOutbound out) {
        return in.aggregateFrames()
            .receive()
            .asString()
            .doOnNext(sink::next)
            .then(out.sendClose());
    }
}
