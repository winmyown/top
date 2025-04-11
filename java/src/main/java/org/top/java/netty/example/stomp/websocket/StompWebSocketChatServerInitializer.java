
package org.top.java.netty.example.stomp.websocket;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.util.internal.ObjectUtil;

public class StompWebSocketChatServerInitializer extends ChannelInitializer<SocketChannel> {

    private final String chatPath;
    private final StompWebSocketProtocolCodec stompWebSocketProtocolCodec;

    public StompWebSocketChatServerInitializer(String chatPath) {
        this.chatPath = ObjectUtil.checkNotNull(chatPath, "chatPath");
        stompWebSocketProtocolCodec = new StompWebSocketProtocolCodec();
    }

    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        channel.pipeline()
               .addLast(new HttpServerCodec())
               .addLast(new HttpObjectAggregator(65536))
               .addLast(StompWebSocketClientPageHandler.INSTANCE)
               .addLast(new WebSocketServerProtocolHandler(chatPath, StompVersion.SUB_PROTOCOLS))
               .addLast(stompWebSocketProtocolCodec);
    }
}
