
package org.top.java.netty.source.buffer;

import io.netty.util.ReferenceCounted;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;

import java.io.*;

import static io.netty.util.internal.ObjectUtil.checkPositiveOrZero;

/**
 * An {@link InputStream} which reads data from a {@link ByteBuf}.
 * <p>
 * A read operation against this stream will occur at the {@code readerIndex}
 * of its underlying buffer and the {@code readerIndex} will increase during
 * the read operation.  Please note that it only reads up to the number of
 * readable bytes determined at the moment of construction.  Therefore,
 * updating {@link ByteBuf#writerIndex()} will not affect the return
 * value of {@link #available()}.
 * <p>
 * This stream implements {@link DataInput} for your convenience.
 * The endianness of the stream is not always big endian but depends on
 * the endianness of the underlying buffer.
 *
 * @see ByteBufOutputStream
 */

/**
 * 一个从 {@link ByteBuf} 读取数据的 {@link InputStream}。
 * <p>
 * 对该流的读取操作将在其底层缓冲区的 {@code readerIndex} 处进行，
 * 并且在读取操作期间 {@code readerIndex} 会增加。请注意，它只会读取到构造时确定的可读字节数。
 * 因此，更新 {@link ByteBuf#writerIndex()} 不会影响 {@link #available()} 的返回值。
 * <p>
 * 为了方便使用，该流实现了 {@link DataInput}。
 * 流的字节顺序不总是大端序，而是取决于底层缓冲区的字节顺序。
 *
 * @see ByteBufOutputStream
 */
public class ByteBufInputStream extends InputStream implements DataInput {
    private final ByteBuf buffer;
    private final int startIndex;
    private final int endIndex;
    private boolean closed;
    /**
     * To preserve backwards compatibility (which didn't transfer ownership) we support a conditional flag which
     * indicates if {@link #buffer} should be released when this {@link InputStream} is closed.
     * However in future releases ownership should always be transferred and callers of this class should call
     * {@link ReferenceCounted#retain()} if necessary.
     */
    /**
     * 为了保持向后兼容性（之前没有转移所有权），我们支持一个条件标志，
     * 用于指示在关闭此 {@link InputStream} 时是否应释放 {@link #buffer}。
     * 然而，在未来的版本中，所有权应始终被转移，此类的调用者应在必要时调用
     * {@link ReferenceCounted#retain()}。
     */
    private final boolean releaseOnClose;

    /**
     * Creates a new stream which reads data from the specified {@code buffer}
     * starting at the current {@code readerIndex} and ending at the current
     * {@code writerIndex}.
     * @param buffer The buffer which provides the content for this {@link InputStream}.
     */

    /**
     * 创建一个新的流，该流从指定的 {@code buffer} 中读取数据，
     * 起始位置为当前的 {@code readerIndex}，结束位置为当前的 {@code writerIndex}。
     * @param buffer 为该 {@link InputStream} 提供内容的缓冲区。
     */
    public ByteBufInputStream(ByteBuf buffer) {
        this(buffer, buffer.readableBytes());
    }

    /**
     * Creates a new stream which reads data from the specified {@code buffer}
     * starting at the current {@code readerIndex} and ending at
     * {@code readerIndex + length}.
     * @param buffer The buffer which provides the content for this {@link InputStream}.
     * @param length The length of the buffer to use for this {@link InputStream}.
     * @throws IndexOutOfBoundsException
     *         if {@code readerIndex + length} is greater than
     *            {@code writerIndex}
     */

    /**
     * 创建一个新的流，该流从指定的 {@code buffer} 中读取数据，
     * 从当前的 {@code readerIndex} 开始，到 {@code readerIndex + length} 结束。
     * @param buffer 为该 {@link InputStream} 提供内容的缓冲区。
     * @param length 用于该 {@link InputStream} 的缓冲区长度。
     * @throws IndexOutOfBoundsException
     *         如果 {@code readerIndex + length} 大于 {@code writerIndex}
     */
    public ByteBufInputStream(ByteBuf buffer, int length) {
        this(buffer, length, false);
    }

    /**
     * Creates a new stream which reads data from the specified {@code buffer}
     * starting at the current {@code readerIndex} and ending at the current
     * {@code writerIndex}.
     * @param buffer The buffer which provides the content for this {@link InputStream}.
     * @param releaseOnClose {@code true} means that when {@link #close()} is called then {@link ByteBuf#release()} will
     *                       be called on {@code buffer}.
     */

    /**
     * 创建一个新的流，该流从指定的 {@code buffer} 中读取数据，起始于当前的 {@code readerIndex}，结束于当前的 {@code writerIndex}。
     * @param buffer 为该 {@link InputStream} 提供内容的缓冲区。
     * @param releaseOnClose {@code true} 表示当调用 {@link #close()} 时，将在 {@code buffer} 上调用 {@link ByteBuf#release()}。
     */
    public ByteBufInputStream(ByteBuf buffer, boolean releaseOnClose) {
        this(buffer, buffer.readableBytes(), releaseOnClose);
    }

    /**
     * Creates a new stream which reads data from the specified {@code buffer}
     * starting at the current {@code readerIndex} and ending at
     * {@code readerIndex + length}.
     * @param buffer The buffer which provides the content for this {@link InputStream}.
     * @param length The length of the buffer to use for this {@link InputStream}.
     * @param releaseOnClose {@code true} means that when {@link #close()} is called then {@link ByteBuf#release()} will
     *                       be called on {@code buffer}.
     * @throws IndexOutOfBoundsException
     *         if {@code readerIndex + length} is greater than
     *            {@code writerIndex}
     */

    /**
     * 创建一个新的流，该流从指定的 {@code buffer} 中读取数据，起始位置为当前的 {@code readerIndex}，
     * 结束位置为 {@code readerIndex + length}。
     * @param buffer 为该 {@link InputStream} 提供内容的缓冲区。
     * @param length 用于该 {@link InputStream} 的缓冲区长度。
     * @param releaseOnClose 如果为 {@code true}，则表示当调用 {@link #close()} 时，
     *                       将在 {@code buffer} 上调用 {@link ByteBuf#release()}。
     * @throws IndexOutOfBoundsException
     *         如果 {@code readerIndex + length} 大于 {@code writerIndex}
     */
    public ByteBufInputStream(ByteBuf buffer, int length, boolean releaseOnClose) {
        ObjectUtil.checkNotNull(buffer, "buffer");
        if (length < 0) {
            if (releaseOnClose) {
                buffer.release();
            }
            checkPositiveOrZero(length, "length");
        }
        if (length > buffer.readableBytes()) {
            if (releaseOnClose) {
                buffer.release();
            }
            throw new IndexOutOfBoundsException("Too many bytes to be read - Needs "
                    + length + ", maximum is " + buffer.readableBytes());
        }

        this.releaseOnClose = releaseOnClose;
        this.buffer = buffer;
        startIndex = buffer.readerIndex();
        endIndex = startIndex + length;
        buffer.markReaderIndex();
    }

    /**
     * Returns the number of read bytes by this stream so far.
     */

    /**
     * 返回此流到目前为止读取的字节数。
     */
    public int readBytes() {
        return buffer.readerIndex() - startIndex;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            // The Closable interface says "If the stream is already closed then invoking this method has no effect."
            // Closable 接口表示“如果流已经关闭，则调用此方法不会有任何效果。”
            if (releaseOnClose && !closed) {
                closed = true;
                buffer.release();
            }
        }
    }

    @Override
    public int available() throws IOException {
        return endIndex - buffer.readerIndex();
    }

    // Suppress a warning since the class is not thread-safe

    // 抑制警告，因为该类不是线程安全的
    @Override
    public void mark(int readlimit) {   // lgtm[java/non-sync-override]
        buffer.markReaderIndex();
    }

    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public int read() throws IOException {
        int available = available();
        if (available == 0) {
            return -1;
        }
        return buffer.readByte() & 0xff;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int available = available();
        if (available == 0) {
            return -1;
        }

        len = Math.min(available, len);
        buffer.readBytes(b, off, len);
        return len;
    }

    // Suppress a warning since the class is not thread-safe

    // 抑制警告，因为该类不是线程安全的
    @Override
    public void reset() throws IOException {    // lgtm[java/non-sync-override]
        buffer.resetReaderIndex();
    }

    @Override
    public long skip(long n) throws IOException {
        if (n > Integer.MAX_VALUE) {
            return skipBytes(Integer.MAX_VALUE);
        } else {
            return skipBytes((int) n);
        }
    }

    @Override
    public boolean readBoolean() throws IOException {
        checkAvailable(1);
        return read() != 0;
    }

    @Override
    public byte readByte() throws IOException {
        int available = available();
        if (available == 0) {
            throw new EOFException();
        }
        return buffer.readByte();
    }

    @Override
    public char readChar() throws IOException {
        return (char) readShort();
    }

    @Override
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        checkAvailable(len);
        buffer.readBytes(b, off, len);
    }

    @Override
    public int readInt() throws IOException {
        checkAvailable(4);
        return buffer.readInt();
    }

    private StringBuilder lineBuf;

    @Override
    public String readLine() throws IOException {
        int available = available();
        if (available == 0) {
            return null;
        }

        if (lineBuf != null) {
            lineBuf.setLength(0);
        }

        loop: do {
            int c = buffer.readUnsignedByte();
            --available;
            switch (c) {
                case '\n':
                    break loop;

                case '\r':
                    if (available > 0 && (char) buffer.getUnsignedByte(buffer.readerIndex()) == '\n') {
                        buffer.skipBytes(1);
                        --available;
                    }
                    break loop;

                default:
                    if (lineBuf == null) {
                        lineBuf = new StringBuilder();
                    }
                    lineBuf.append((char) c);
            }
        } while (available > 0);

        return lineBuf != null && lineBuf.length() > 0 ? lineBuf.toString() : StringUtil.EMPTY_STRING;
    }

    @Override
    public long readLong() throws IOException {
        checkAvailable(8);
        return buffer.readLong();
    }

    @Override
    public short readShort() throws IOException {
        checkAvailable(2);
        return buffer.readShort();
    }

    @Override
    public String readUTF() throws IOException {
        return DataInputStream.readUTF(this);
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return readByte() & 0xff;
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return readShort() & 0xffff;
    }

    @Override
    public int skipBytes(int n) throws IOException {
        int nBytes = Math.min(available(), n);
        buffer.skipBytes(nBytes);
        return nBytes;
    }

    private void checkAvailable(int fieldSize) throws IOException {
        if (fieldSize < 0) {
            throw new IndexOutOfBoundsException("fieldSize cannot be a negative number");
        }
        if (fieldSize > available()) {
            throw new EOFException("fieldSize is too long! Length is " + fieldSize
                    + ", but maximum is " + available());
        }
    }
}
