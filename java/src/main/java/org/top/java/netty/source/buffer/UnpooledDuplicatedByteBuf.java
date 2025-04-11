
package org.top.java.netty.source.buffer;

/**
 * {@link DuplicatedByteBuf} implementation that can do optimizations because it knows the duplicated buffer
 * is of type {@link AbstractByteBuf}.
 */

/**
 * {@link DuplicatedByteBuf} 实现，由于知道重复的缓冲区是 {@link AbstractByteBuf} 类型，因此可以进行优化。
 */
class UnpooledDuplicatedByteBuf extends DuplicatedByteBuf {
    UnpooledDuplicatedByteBuf(AbstractByteBuf buffer) {
        super(buffer);
    }

    @Override
    public AbstractByteBuf unwrap() {
        return (AbstractByteBuf) super.unwrap();
    }

    @Override
    protected byte _getByte(int index) {
        return unwrap()._getByte(index);
    }

    @Override
    protected short _getShort(int index) {
        return unwrap()._getShort(index);
    }

    @Override
    protected short _getShortLE(int index) {
        return unwrap()._getShortLE(index);
    }

    @Override
    protected int _getUnsignedMedium(int index) {
        return unwrap()._getUnsignedMedium(index);
    }

    @Override
    protected int _getUnsignedMediumLE(int index) {
        return unwrap()._getUnsignedMediumLE(index);
    }

    @Override
    protected int _getInt(int index) {
        return unwrap()._getInt(index);
    }

    @Override
    protected int _getIntLE(int index) {
        return unwrap()._getIntLE(index);
    }

    @Override
    protected long _getLong(int index) {
        return unwrap()._getLong(index);
    }

    @Override
    protected long _getLongLE(int index) {
        return unwrap()._getLongLE(index);
    }

    @Override
    protected void _setByte(int index, int value) {
        unwrap()._setByte(index, value);
    }

    @Override
    protected void _setShort(int index, int value) {
        unwrap()._setShort(index, value);
    }

    @Override
    protected void _setShortLE(int index, int value) {
        unwrap()._setShortLE(index, value);
    }

    @Override
    protected void _setMedium(int index, int value) {
        unwrap()._setMedium(index, value);
    }

    @Override
    protected void _setMediumLE(int index, int value) {
        unwrap()._setMediumLE(index, value);
    }

    @Override
    protected void _setInt(int index, int value) {
        unwrap()._setInt(index, value);
    }

    @Override
    protected void _setIntLE(int index, int value) {
        unwrap()._setIntLE(index, value);
    }

    @Override
    protected void _setLong(int index, long value) {
        unwrap()._setLong(index, value);
    }

    @Override
    protected void _setLongLE(int index, long value) {
        unwrap()._setLongLE(index, value);
    }
}
