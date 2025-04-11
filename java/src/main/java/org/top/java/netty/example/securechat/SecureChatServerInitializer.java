
package org.top.java.netty.example.securechat;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;

/**
 * Creates a newly configured {@link ChannelPipeline} for a new channel.
 */

/**
 * 为新通道创建一个新配置的 {@link ChannelPipeline}。
 */
public class SecureChatServerInitializer extends ChannelInitializer<SocketChannel> {

    private final SslContext sslCtx;

    public SecureChatServerInitializer(SslContext sslCtx) {
        this.sslCtx = sslCtx;
    }

    @Override
    public void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // Add SSL handler first to encrypt and decrypt everything.

        // 首先添加SSL处理程序以加密和解密所有内容。
        // In this example, we use a bogus certificate in the server side
        // 在这个示例中，我们在服务器端使用了一个伪造的证书
        // and accept any invalid certificates in the client side.
        // 并在客户端接受任何无效的证书。
        // You will need something more complicated to identify both
        // You will need something more complicated to identify both
        // and server in the real world.
        // 以及现实世界中的服务器。
        pipeline.addLast(sslCtx.newHandler(ch.alloc()));

        // On top of the SSL handler, add the text line codec.

        // 在 SSL 处理器之上，添加文本行编解码器。
        pipeline.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
        pipeline.addLast(new StringDecoder());
        pipeline.addLast(new StringEncoder());

        // and then business logic.

        // 然后是业务逻辑。
        pipeline.addLast(new SecureChatServerHandler());
    }
}
