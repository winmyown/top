package org.top.java.netty.example.stomp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.stomp.DefaultStompFrame;
import io.netty.handler.codec.stomp.StompCommand;
import io.netty.handler.codec.stomp.StompFrame;
import io.netty.handler.codec.stomp.StompHeaders;

/**
 * STOMP client inbound handler implementation, which just passes received messages to listener
 */

/**
 * STOMP客户端入站处理程序实现，仅将接收到的消息传递给监听器
 */
public class StompClientHandler extends SimpleChannelInboundHandler<StompFrame> {

    private enum ClientState {
        AUTHENTICATING,
        AUTHENTICATED,
        SUBSCRIBED,
        DISCONNECTING
    }

    private ClientState state;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        state = ClientState.AUTHENTICATING;
        StompFrame connFrame = new DefaultStompFrame(StompCommand.CONNECT);
        connFrame.headers().set(StompHeaders.ACCEPT_VERSION, "1.2");
        connFrame.headers().set(StompHeaders.HOST, StompClient.HOST);
        connFrame.headers().set(StompHeaders.LOGIN, StompClient.LOGIN);
        connFrame.headers().set(StompHeaders.PASSCODE, StompClient.PASSCODE);
        ctx.writeAndFlush(connFrame);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, StompFrame frame) throws Exception {
        String subscrReceiptId = "001";
        String disconReceiptId = "002";
        switch (frame.command()) {
            case CONNECTED:
                StompFrame subscribeFrame = new DefaultStompFrame(StompCommand.SUBSCRIBE);
                subscribeFrame.headers().set(StompHeaders.DESTINATION, StompClient.TOPIC);
                subscribeFrame.headers().set(StompHeaders.RECEIPT, subscrReceiptId);
                subscribeFrame.headers().set(StompHeaders.ID, "1");
                System.out.println("connected, sending subscribe frame: " + subscribeFrame);
                state = ClientState.AUTHENTICATED;
                ctx.writeAndFlush(subscribeFrame);
                break;
            case RECEIPT:
                String receiptHeader = frame.headers().getAsString(StompHeaders.RECEIPT_ID);
                if (state == ClientState.AUTHENTICATED && receiptHeader.equals(subscrReceiptId)) {
                    StompFrame msgFrame = new DefaultStompFrame(StompCommand.SEND);
                    msgFrame.headers().set(StompHeaders.DESTINATION, StompClient.TOPIC);
                    msgFrame.content().writeBytes("some payload".getBytes());
                    System.out.println("subscribed, sending message frame: " + msgFrame);
                    state = ClientState.SUBSCRIBED;
                    ctx.writeAndFlush(msgFrame);
                } else if (state == ClientState.DISCONNECTING && receiptHeader.equals(disconReceiptId)) {
                    System.out.println("disconnected");
                    ctx.close();
                } else {
                    throw new IllegalStateException("received: " + frame + ", while internal state is " + state);
                }
                break;
            case MESSAGE:
                if (state == ClientState.SUBSCRIBED) {
                    System.out.println("received frame: " + frame);
                    StompFrame disconnFrame = new DefaultStompFrame(StompCommand.DISCONNECT);
                    disconnFrame.headers().set(StompHeaders.RECEIPT, disconReceiptId);
                    System.out.println("sending disconnect frame: " + disconnFrame);
                    state = ClientState.DISCONNECTING;
                    ctx.writeAndFlush(disconnFrame);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
