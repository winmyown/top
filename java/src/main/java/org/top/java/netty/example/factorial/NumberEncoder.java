
package org.top.java.netty.example.factorial;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.math.BigInteger;

/**
 * Encodes a {@link Number} into the binary representation prepended with
 * a magic number ('F' or 0x46) and a 32-bit length prefix.  For example, 42
 * will be encoded to { 'F', 0, 0, 0, 1, 42 }.
 */

/**
 * 将 {@link Number} 编码为二进制表示，并在前面添加一个魔数（'F' 或 0x46）和一个 32 位长度前缀。例如，42
 * 将被编码为 { 'F', 0, 0, 0, 1, 42 }。
 */
public class NumberEncoder extends MessageToByteEncoder<Number> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Number msg, ByteBuf out) {
        // Convert to a BigInteger first for easier implementation.
        // 首先转换为 BigInteger 以便于实现。
        BigInteger v;
        if (msg instanceof BigInteger) {
            v = (BigInteger) msg;
        } else {
            v = new BigInteger(String.valueOf(msg));
        }

        // Convert the number into a byte array.

        // 将数字转换为字节数组。
        byte[] data = v.toByteArray();
        int dataLength = data.length;

        // Write a message.

        // 写一条消息。
        out.writeByte((byte) 'F'); // magic number
        out.writeInt(dataLength);  // data length
        out.writeBytes(data);      // data
    }
}
