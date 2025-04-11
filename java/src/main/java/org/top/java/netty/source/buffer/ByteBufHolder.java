
package org.top.java.netty.source.buffer;

import io.netty.util.ReferenceCounted;

/**
 * A packet which is send or receive.
 */

/**
 * 发送或接收的数据包。
 */
public interface ByteBufHolder extends ReferenceCounted {

    /**
     * Return the data which is held by this {@link ByteBufHolder}.
     */

    /**
     * 返回此 {@link ByteBufHolder} 持有的数据。
     */
    ByteBuf content();

    /**
     * Creates a deep copy of this {@link ByteBufHolder}.
     */

    /**
     * 创建此 {@link ByteBufHolder} 的深拷贝。
     */
    ByteBufHolder copy();

    /**
     * Duplicates this {@link ByteBufHolder}. Be aware that this will not automatically call {@link #retain()}.
     */

    /**
     * 复制此 {@link ByteBufHolder}。请注意，这不会自动调用 {@link #retain()}。
     */
    ByteBufHolder duplicate();

    /**
     * Duplicates this {@link ByteBufHolder}. This method returns a retained duplicate unlike {@link #duplicate()}.
     *
     * @see ByteBuf#retainedDuplicate()
     */

    /**
     * 复制此 {@link ByteBufHolder}。与 {@link #duplicate()} 不同，此方法返回一个保留的副本。
     *
     * @see ByteBuf#retainedDuplicate()
     */
    ByteBufHolder retainedDuplicate();

    /**
     * Returns a new {@link ByteBufHolder} which contains the specified {@code content}.
     */

    /**
     * 返回一个新的 {@link ByteBufHolder}，其中包含指定的 {@code content}。
     */
    ByteBufHolder replace(ByteBuf content);

    @Override
    ByteBufHolder retain();

    @Override
    ByteBufHolder retain(int increment);

    @Override
    ByteBufHolder touch();

    @Override
    ByteBufHolder touch(Object hint);
}
