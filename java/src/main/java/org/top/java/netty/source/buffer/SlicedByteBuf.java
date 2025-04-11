
package org.top.java.netty.source.buffer;

/**
 * A derived buffer which exposes its parent's sub-region only.  It is
 * recommended to use {@link ByteBuf#slice()} and
 * {@link ByteBuf#slice(int, int)} instead of calling the constructor
 * explicitly.
 *
 * @deprecated Do not use.
 */

/**
 * 一个派生缓冲区，仅暴露其父缓冲区的子区域。建议使用
 * {@link ByteBuf#slice()} 和
 * {@link ByteBuf#slice(int, int)}，而不是显式调用构造函数。
 *
 * @deprecated 请勿使用。
 */
@Deprecated
public class SlicedByteBuf extends AbstractUnpooledSlicedByteBuf {

    private int length;

    public SlicedByteBuf(ByteBuf buffer, int index, int length) {
        super(buffer, index, length);
    }

    @Override
    final void initLength(int length) {
        this.length = length;
    }

    @Override
    final int length() {
        return length;
    }

    @Override
    public int capacity() {
        return length;
    }
}
