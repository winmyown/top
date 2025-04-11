
package org.top.java.netty.example.sctp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.sctp.SctpMessage;

/**
 * Handler implementation for the SCTP echo client.  It initiates the ping-pong
 * traffic between the echo client and server by sending the first message to
 * the server.
 */

/**
 * SCTP回声客户端的处理器实现。它通过向服务器发送第一条消息来启动回声客户端和服务器之间的ping-pong通信。
 */
public class SctpEchoClientHandler extends ChannelInboundHandlerAdapter {

    private final ByteBuf firstMessage;

    /**
     * Creates a client-side handler.
     */

    /**
     * 创建一个客户端处理器。
     */
    public SctpEchoClientHandler() {
        firstMessage = Unpooled.buffer(SctpEchoClient.SIZE);
        for (int i = 0; i < firstMessage.capacity(); i++) {
            firstMessage.writeByte((byte) i);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(new SctpMessage(0, 0, firstMessage));
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ctx.write(msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        // 当异常被抛出时关闭连接。
        cause.printStackTrace();
        ctx.close();
    }
}
