package io.lucasvalenteds.spring.reactive.ws;

import java.util.function.BiFunction;
import org.reactivestreams.Publisher;
import reactor.netty.http.websocket.WebsocketInbound;
import reactor.netty.http.websocket.WebsocketOutbound;

public class ClientToServerHandler implements BiFunction<WebsocketInbound, WebsocketOutbound, Publisher<Void>> {

    private String word;

    @Override
    public Publisher<Void> apply(WebsocketInbound in, WebsocketOutbound out) {
        return in.aggregateFrames()
            .receive()
            .asString()
            .doOnNext(it -> word = it)
            .then(out.sendClose());
    }

    public String getWordReceived() {
        return word;
    }
}
