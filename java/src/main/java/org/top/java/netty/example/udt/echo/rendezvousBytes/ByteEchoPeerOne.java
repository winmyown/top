
package org.top.java.netty.example.udt.echo.rendezvousBytes;

import org.top.java.netty.example.udt.echo.rendezvous.Config;
import io.netty.util.internal.SocketUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * UDT Byte Stream Peer
 * <p/>
 * Sends one message when a connection is open and echoes back any received data
 * to the server. Simply put, the echo client initiates the ping-pong traffic
 * between the echo client and server by sending the first message to the
 * server.
 * <p/>
 */

/**
 * UDT 字节流对等体
 * <p/>
 * 当连接打开时发送一条消息，并将接收到的任何数据回显到服务器。简而言之，回显客户端通过向服务器发送第一条消息来启动回显客户端和服务器之间的乒乓通信。
 * <p/>
 */
public class ByteEchoPeerOne extends ByteEchoPeerBase {

    public ByteEchoPeerOne(int messageSize, SocketAddress myAddress, SocketAddress peerAddress) {
        super(messageSize, myAddress, peerAddress);
    }

    public static void main(String[] args) throws Exception {
        final int messageSize = 64 * 1024;
        final InetSocketAddress myAddress = SocketUtils.socketAddress(Config.hostOne, Config.portOne);
        final InetSocketAddress peerAddress = SocketUtils.socketAddress(Config.hostTwo, Config.portTwo);
        new ByteEchoPeerOne(messageSize, myAddress, peerAddress).run();
    }
}
