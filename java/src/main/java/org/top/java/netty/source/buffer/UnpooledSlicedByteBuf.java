
package org.top.java.netty.source.buffer;

/**
 * A special {@link AbstractUnpooledSlicedByteBuf} that can make optimizations because it knows the sliced buffer is of
 * type {@link AbstractByteBuf}.
 */

/**
 * 一种特殊的 {@link AbstractUnpooledSlicedByteBuf}，由于它知道切片缓冲区的类型为 {@link AbstractByteBuf}，因此可以进行优化。
 */
class UnpooledSlicedByteBuf extends AbstractUnpooledSlicedByteBuf {
    UnpooledSlicedByteBuf(AbstractByteBuf buffer, int index, int length) {
        super(buffer, index, length);
    }

    @Override
    public int capacity() {
        return maxCapacity();
    }

    @Override
    public AbstractByteBuf unwrap() {
        return (AbstractByteBuf) super.unwrap();
    }

    @Override
    protected byte _getByte(int index) {
        return unwrap()._getByte(idx(index));
    }

    @Override
    protected short _getShort(int index) {
        return unwrap()._getShort(idx(index));
    }

    @Override
    protected short _getShortLE(int index) {
        return unwrap()._getShortLE(idx(index));
    }

    @Override
    protected int _getUnsignedMedium(int index) {
        return unwrap()._getUnsignedMedium(idx(index));
    }

    @Override
    protected int _getUnsignedMediumLE(int index) {
        return unwrap()._getUnsignedMediumLE(idx(index));
    }

    @Override
    protected int _getInt(int index) {
        return unwrap()._getInt(idx(index));
    }

    @Override
    protected int _getIntLE(int index) {
        return unwrap()._getIntLE(idx(index));
    }

    @Override
    protected long _getLong(int index) {
        return unwrap()._getLong(idx(index));
    }

    @Override
    protected long _getLongLE(int index) {
        return unwrap()._getLongLE(idx(index));
    }

    @Override
    protected void _setByte(int index, int value) {
        unwrap()._setByte(idx(index), value);
    }

    @Override
    protected void _setShort(int index, int value) {
        unwrap()._setShort(idx(index), value);
    }

    @Override
    protected void _setShortLE(int index, int value) {
        unwrap()._setShortLE(idx(index), value);
    }

    @Override
    protected void _setMedium(int index, int value) {
        unwrap()._setMedium(idx(index), value);
    }

    @Override
    protected void _setMediumLE(int index, int value) {
        unwrap()._setMediumLE(idx(index), value);
    }

    @Override
    protected void _setInt(int index, int value) {
        unwrap()._setInt(idx(index), value);
    }

    @Override
    protected void _setIntLE(int index, int value) {
        unwrap()._setIntLE(idx(index), value);
    }

    @Override
    protected void _setLong(int index, long value) {
        unwrap()._setLong(idx(index), value);
    }

    @Override
    protected void _setLongLE(int index, long value) {
        unwrap()._setLongLE(idx(index), value);
    }
}
