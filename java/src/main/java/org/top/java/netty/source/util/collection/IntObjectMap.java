
package org.top.java.netty.source.util.collection;

import java.util.Map;

/**
 * Interface for a primitive map that uses {@code int}s as keys.
 *
 * @param <V> the value type stored in the map.
 */

/**
 * 使用{@code int}作为键的原始映射接口。
 *
 * @param <V> 存储在映射中的值类型。
 */
public interface IntObjectMap<V> extends Map<Integer, V> {

    /**
     * A primitive entry in the map, provided by the iterator from {@link #entries()}
     *
     * @param <V> the value type stored in the map.
     */

    /**
     * 映射中的一个原始条目，由 {@link #entries()} 的迭代器提供
     *
     * @param <V> 存储在映射中的值类型。
     */
    interface PrimitiveEntry<V> {
        /**
         * Gets the key for this entry.
         */
        /**
         * 获取此条目的键。
         */
        int key();

        /**
         * Gets the value for this entry.
         */

        /**
         * 获取此条目的值。
         */
        V value();

        /**
         * Sets the value for this entry.
         */

        /**
         * 设置此条目的值。
         */
        void setValue(V value);
    }

    /**
     * Gets the value in the map with the specified key.
     *
     * @param key the key whose associated value is to be returned.
     * @return the value or {@code null} if the key was not found in the map.
     */

    /**
     * 获取与指定键关联的值。
     *
     * @param key 要返回其关联值的键。
     * @return 值，如果键在映射中未找到则返回 {@code null}。
     */
    V get(int key);

    /**
     * Puts the given entry into the map.
     *
     * @param key the key of the entry.
     * @param value the value of the entry.
     * @return the previous value for this key or {@code null} if there was no previous mapping.
     */

    /**
     * 将给定的条目放入映射中。
     *
     * @param key 条目的键。
     * @param value 条目的值。
     * @return 此键的先前值，如果没有先前的映射，则返回 {@code null}。
     */
    V put(int key, V value);

    /**
     * Removes the entry with the specified key.
     *
     * @param key the key for the entry to be removed from this map.
     * @return the previous value for the key, or {@code null} if there was no mapping.
     */

    /**
     * 移除具有指定键的条目。
     *
     * @param key 要从此映射中移除的条目的键。
     * @return 键的先前值，如果没有映射则返回 {@code null}。
     */
    V remove(int key);

    /**
     * Gets an iterable to traverse over the primitive entries contained in this map. As an optimization,
     * the {@link PrimitiveEntry}s returned by the {@link Iterator} may change as the {@link Iterator}
     * progresses. The caller should not rely on {@link PrimitiveEntry} key/value stability.
     */

    /**
     * 获取一个可遍历此映射中包含的原始条目的迭代器。作为优化，
     * {@link Iterator}返回的{@link PrimitiveEntry}可能会随着{@link Iterator}的
     * 推进而改变。调用者不应依赖{@link PrimitiveEntry}键/值的稳定性。
     */
    Iterable<PrimitiveEntry<V>> entries();

    /**
     * Indicates whether or not this map contains a value for the specified key.
     */

    /**
     * 指示此映射是否包含指定键的值。
     */
    boolean containsKey(int key);
}
