
package org.top.java.netty.example.stomp.websocket;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrameAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.HandshakeComplete;
import io.netty.handler.codec.stomp.StompSubframe;
import io.netty.handler.codec.stomp.StompSubframeAggregator;
import io.netty.handler.codec.stomp.StompSubframeDecoder;

import java.util.List;

@Sharable
public class StompWebSocketProtocolCodec extends MessageToMessageCodec<WebSocketFrame, StompSubframe> {

    private final StompChatHandler stompChatHandler = new StompChatHandler();
    private final StompWebSocketFrameEncoder stompWebSocketFrameEncoder = new StompWebSocketFrameEncoder();

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            StompVersion stompVersion = StompVersion.findBySubProtocol(((HandshakeComplete) evt).selectedSubprotocol());
            ctx.channel().attr(StompVersion.CHANNEL_ATTRIBUTE_KEY).set(stompVersion);
            ctx.pipeline()
               .addLast(new WebSocketFrameAggregator(65536))
               .addLast(new StompSubframeDecoder())
               .addLast(new StompSubframeAggregator(65536))
               .addLast(stompChatHandler)
               .remove(StompWebSocketClientPageHandler.INSTANCE);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, StompSubframe stompFrame, List<Object> out) throws Exception {
        stompWebSocketFrameEncoder.encode(ctx, stompFrame, out);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame, List<Object> out) {
        if (webSocketFrame instanceof TextWebSocketFrame || webSocketFrame instanceof BinaryWebSocketFrame) {
            out.add(webSocketFrame.content().retain());
        } else {
            ctx.close();
        }
    }
}
