
package org.top.java.netty.source.buffer;

import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;

/**
 * Default implementation of a {@link ByteBufHolder} that holds it's data in a {@link ByteBuf}.
 *
 */

/**
 * 默认实现的 {@link ByteBufHolder}，它将数据保存在 {@link ByteBuf} 中。
 *
 */
public class DefaultByteBufHolder implements ByteBufHolder {

    private final ByteBuf data;

    public DefaultByteBufHolder(ByteBuf data) {
        this.data = ObjectUtil.checkNotNull(data, "data");
    }

    @Override
    public ByteBuf content() {
        return ByteBufUtil.ensureAccessible(data);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method calls {@code replace(content().copy())} by default.
     */

    /**
     * {@inheritDoc}
     * <p>
     * 此方法默认调用 {@code replace(content().copy())}。
     */
    @Override
    public ByteBufHolder copy() {
        return replace(data.copy());
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method calls {@code replace(content().duplicate())} by default.
     */

    /**
     * {@inheritDoc}
     * <p>
     * 此方法默认调用 {@code replace(content().duplicate())}。
     */
    @Override
    public ByteBufHolder duplicate() {
        return replace(data.duplicate());
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method calls {@code replace(content().retainedDuplicate())} by default.
     */

    /**
     * {@inheritDoc}
     * <p>
     * 此方法默认调用 {@code replace(content().retainedDuplicate())}。
     */
    @Override
    public ByteBufHolder retainedDuplicate() {
        return replace(data.retainedDuplicate());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Override this method to return a new instance of this object whose content is set to the specified
     * {@code content}. The default implementation of {@link #copy()}, {@link #duplicate()} and
     * {@link #retainedDuplicate()} invokes this method to create a copy.
     */

    /**
     * {@inheritDoc}
     * <p>
     * 重写此方法以返回一个新实例，该实例的内容设置为指定的
     * {@code content}。{@link #copy()}、{@link #duplicate()} 和
     * {@link #retainedDuplicate()} 的默认实现通过调用此方法来创建副本。
     */
    @Override
    public ByteBufHolder replace(ByteBuf content) {
        return new DefaultByteBufHolder(content);
    }

    @Override
    public int refCnt() {
        return data.refCnt();
    }

    @Override
    public ByteBufHolder retain() {
        data.retain();
        return this;
    }

    @Override
    public ByteBufHolder retain(int increment) {
        data.retain(increment);
        return this;
    }

    @Override
    public ByteBufHolder touch() {
        data.touch();
        return this;
    }

    @Override
    public ByteBufHolder touch(Object hint) {
        data.touch(hint);
        return this;
    }

    @Override
    public boolean release() {
        return data.release();
    }

    @Override
    public boolean release(int decrement) {
        return data.release(decrement);
    }

    /**
     * Return {@link ByteBuf#toString()} without checking the reference count first. This is useful to implement
     * {@link #toString()}.
     */

    /**
     * 返回 {@link ByteBuf#toString()} 而不首先检查引用计数。这对于实现 {@link #toString()} 很有用。
     */
    protected final String contentToString() {
        return data.toString();
    }

    @Override
    public String toString() {
        return StringUtil.simpleClassName(this) + '(' + contentToString() + ')';
    }

    /**
     * This implementation of the {@code equals} operation is restricted to
     * work only with instances of the same class. The reason for that is that
     * Netty library already has a number of classes that extend {@link DefaultByteBufHolder} and
     * override {@code equals} method with an additional comparison logic and we
     * need the symmetric property of the {@code equals} operation to be preserved.
     *
     * @param   o   the reference object with which to compare.
     * @return  {@code true} if this object is the same as the obj
     *          argument; {@code false} otherwise.
     */

    /**
     * 此 {@code equals} 操作的实现仅限于与相同类的实例一起工作。原因是 Netty 库已经有许多类扩展了 {@link DefaultByteBufHolder} 并重写了 {@code equals} 方法，添加了额外的比较逻辑，我们需要确保 {@code equals} 操作的对称性得以保留。
     *
     * @param   o   要与之比较的引用对象。
     * @return  如果此对象与 obj 参数相同，则返回 {@code true}；否则返回 {@code false}。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o != null && getClass() == o.getClass()) {
            return data.equals(((DefaultByteBufHolder) o).data);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }
}
