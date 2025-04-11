
package org.top.java.netty.source.buffer;

/**
 * An interface that can be implemented by any object that know how to turn itself into a {@link ByteBuf}.
 * All {@link ByteBuf} classes implement this interface, and return themselves.
 */

/**
 * 一个可以被任何知道如何将自己转换为 {@link ByteBuf} 的对象实现的接口。
 * 所有 {@link ByteBuf} 类都实现了此接口，并返回它们自身。
 */
public interface ByteBufConvertible {
    /**
     * Turn this object into a {@link ByteBuf}.
     * This does <strong>not</strong> increment the reference count of the {@link ByteBuf} instance.
     * The conversion or exposure of the {@link ByteBuf} must be idempotent, so that this method can be called
     * either once, or multiple times, without causing any change in program behaviour.
     *
     * @return A {@link ByteBuf} instance from this object.
     */
    /**
     * 将此对象转换为 {@link ByteBuf}。
     * 此操作<strong>不会</strong>增加 {@link ByteBuf} 实例的引用计数。
     * 转换或暴露 {@link ByteBuf} 必须是幂等的，以便此方法可以调用一次或多次，而不会导致程序行为发生任何变化。
     *
     * @return 从此对象获取的 {@link ByteBuf} 实例。
     */
    ByteBuf asByteBuf();
}
