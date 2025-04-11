
package org.top.java.netty.source.buffer;

import io.netty.util.CharsetUtil;
import io.netty.util.internal.ObjectUtil;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An {@link OutputStream} which writes data to a {@link ByteBuf}.
 * <p>
 * A write operation against this stream will occur at the {@code writerIndex}
 * of its underlying buffer and the {@code writerIndex} will increase during
 * the write operation.
 * <p>
 * This stream implements {@link DataOutput} for your convenience.
 * The endianness of the stream is not always big endian but depends on
 * the endianness of the underlying buffer.
 *
 * @see ByteBufInputStream
 */

/**
 * 一个将数据写入 {@link ByteBuf} 的 {@link OutputStream}。
 * <p>
 * 对该流的写操作将在其底层缓冲区的 {@code writerIndex} 处进行，
 * 并且 {@code writerIndex} 会在写操作期间增加。
 * <p>
 * 为了方便使用，该流实现了 {@link DataOutput}。
 * 流的字节顺序不总是大端序，而是取决于底层缓冲区的字节顺序。
 *
 * @see ByteBufInputStream
 */
public class ByteBufOutputStream extends OutputStream implements DataOutput {

    private final ByteBuf buffer;
    private final int startIndex;
    private DataOutputStream utf8out; // lazily-instantiated
    private boolean closed;

    /**
     * Creates a new stream which writes data to the specified {@code buffer}.
     */

    /**
     * 创建一个新的流，将数据写入指定的 {@code buffer}。
     */
    public ByteBufOutputStream(ByteBuf buffer) {
        this.buffer = ObjectUtil.checkNotNull(buffer, "buffer");
        startIndex = buffer.writerIndex();
    }

    /**
     * Returns the number of written bytes by this stream so far.
     */

    /**
     * 返回此流迄今为止写入的字节数。
     */
    public int writtenBytes() {
        return buffer.writerIndex() - startIndex;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return;
        }

        buffer.writeBytes(b, off, len);
    }

    @Override
    public void write(byte[] b) throws IOException {
        buffer.writeBytes(b);
    }

    @Override
    public void write(int b) throws IOException {
        buffer.writeByte(b);
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        buffer.writeBoolean(v);
    }

    @Override
    public void writeByte(int v) throws IOException {
        buffer.writeByte(v);
    }

    @Override
    public void writeBytes(String s) throws IOException {
        buffer.writeCharSequence(s, CharsetUtil.US_ASCII);
    }

    @Override
    public void writeChar(int v) throws IOException {
        buffer.writeChar(v);
    }

    @Override
    public void writeChars(String s) throws IOException {
        int len = s.length();
        for (int i = 0 ; i < len ; i ++) {
            buffer.writeChar(s.charAt(i));
        }
    }

    @Override
    public void writeDouble(double v) throws IOException {
        buffer.writeDouble(v);
    }

    @Override
    public void writeFloat(float v) throws IOException {
        buffer.writeFloat(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        buffer.writeInt(v);
    }

    @Override
    public void writeLong(long v) throws IOException {
        buffer.writeLong(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        buffer.writeShort((short) v);
    }

    @Override
    public void writeUTF(String s) throws IOException {
        DataOutputStream out = utf8out;
        if (out == null) {
            if (closed) {
                throw new IOException("The stream is closed");
            }
            // Suppress a warning since the stream is closed in the close() method
            // 由于流在 close() 方法中被关闭，因此抑制警告
            utf8out = out = new DataOutputStream(this); // lgtm[java/output-resource-leak]
        }
        out.writeUTF(s);
    }

    /**
     * Returns the buffer where this stream is writing data.
     */

    /**
     * 返回此流正在写入数据的缓冲区。
     */
    public ByteBuf buffer() {
        return buffer;
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;

        try {
            super.close();
        } finally {
            if (utf8out != null) {
                utf8out.close();
            }
        }
    }
}
