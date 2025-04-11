package org.top.java.netty;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.CharsetUtil;

import java.util.List;

/**
 * 自定义消息对象
 */
public class Message {
    private int type;       // 消息类型
    private String content; // 消息内容

    public Message(int type, String content) {
        this.type = type;
        this.content = content;
    }

    public int getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "Message{type=" + type + ", content='" + content + "'}";
    }
}

/**
 * 自定义编码器 - 将Message对象编码为ByteBuf
 */
class MessageEncoder extends MessageToByteEncoder<Message> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
        // 1. 写入消息类型（4字节整数）
        out.writeInt(msg.getType());

        // 2. 写入消息内容
        byte[] contentBytes = msg.getContent().getBytes(CharsetUtil.UTF_8);

        // 3. 写入内容长度（4字节整数）
        out.writeInt(contentBytes.length);

        // 4. 写入内容
        out.writeBytes(contentBytes);

        System.out.println("编码器: 已将Message对象编码为ByteBuf");
    }
}

/**
 * 自定义解码器 - 将ByteBuf解码为Message对象
 */
class MessageDecoder extends ByteToMessageDecoder {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 检查是否有足够的字节可读
        if (in.readableBytes() < 8) { // 至少需要8字节（类型4字节+长度4字节）
            return;
        }

        // 标记当前读取位置
        in.markReaderIndex();

        // 1. 读取消息类型
        int type = in.readInt();

        // 2. 读取内容长度
        int length = in.readInt();

        // 检查是否有足够的字节可读
        if (in.readableBytes() < length) {
            // 重置读取位置，等待更多数据
            in.resetReaderIndex();
            return;
        }

        // 3. 读取内容
        byte[] contentBytes = new byte[length];
        in.readBytes(contentBytes);
        String content = new String(contentBytes, CharsetUtil.UTF_8);

        // 4. 创建Message对象并添加到输出列表
        Message message = new Message(type, content);
        out.add(message);

        System.out.println("解码器: 已将ByteBuf解码为Message对象");
    }
}

/**
 * 使用自定义编解码器的示例
 * 在实际应用中，将这些编解码器添加到ChannelPipeline中:
 *
 * pipeline.addLast(new MessageDecoder()); // 入站处理器
 * pipeline.addLast(new MessageEncoder()); // 出站处理器
 */