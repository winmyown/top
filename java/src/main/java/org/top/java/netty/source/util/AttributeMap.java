
package org.top.java.netty.source.util;

/**
 * Holds {@link Attribute}s which can be accessed via {@link AttributeKey}.
 *
 * Implementations must be Thread-safe.
 */

/**
 * 持有可以通过 {@link AttributeKey} 访问的 {@link Attribute}。
 *
 * 实现必须是线程安全的。
 */
public interface AttributeMap {
    /**
     * Get the {@link Attribute} for the given {@link AttributeKey}. This method will never return null, but may return
     * an {@link Attribute} which does not have a value set yet.
     */
    /**
     * 获取给定 {@link AttributeKey} 对应的 {@link Attribute}。此方法永远不会返回 null，但可能返回一个尚未设置值的 {@link Attribute}。
     */
    <T> Attribute<T> attr(AttributeKey<T> key);

    /**
     * Returns {@code true} if and only if the given {@link Attribute} exists in this {@link AttributeMap}.
     */

    /**
     * 当且仅当给定的 {@link Attribute} 存在于该 {@link AttributeMap} 中时返回 {@code true}。
     */
    <T> boolean hasAttr(AttributeKey<T> key);
}
