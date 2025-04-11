
package org.top.java.netty.source.util.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Utilities for short-based primitive collections.
 */

/**
 * 基于short类型的原始集合工具类。
 */
public final class ShortCollections {

    private static final ShortObjectMap<Object> EMPTY_MAP = new EmptyMap();

    private ShortCollections() {
    }

    /**
     * Returns an unmodifiable empty {@link ShortObjectMap}.
     */

    /**
     * 返回一个不可修改的空 {@link ShortObjectMap}。
     */
    @SuppressWarnings("unchecked")
    public static <V> ShortObjectMap<V> emptyMap() {
        return (ShortObjectMap<V>) EMPTY_MAP;
    }

    /**
     * Creates an unmodifiable wrapper around the given map.
     */

    /**
     * 创建一个围绕给定映射的不可修改的包装器。
     */
    public static <V> ShortObjectMap<V> unmodifiableMap(final ShortObjectMap<V> map) {
        return new UnmodifiableMap<V>(map);
    }

    /**
     * An empty map. All operations that attempt to modify the map are unsupported.
     */

    /**
     * 一个空的映射。所有试图修改映射的操作都不被支持。
     */
    private static final class EmptyMap implements ShortObjectMap<Object> {
        @Override
        public Object get(short key) {
            return null;
        }

        @Override
        public Object put(short key, Object value) {
            throw new UnsupportedOperationException("put");
        }

        @Override
        public Object remove(short key) {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public void clear() {
            // Do nothing.
            // 什么都不做。
        }

        @Override
        public Set<Short> keySet() {
            return Collections.emptySet();
        }

        @Override
        public boolean containsKey(short key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public Iterable<PrimitiveEntry<Object>> entries() {
            return Collections.emptySet();
        }

        @Override
        public Object get(Object key) {
            return null;
        }

        @Override
        public Object put(Short key, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<? extends Short, ?> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<Object> values() {
            return Collections.emptyList();
        }

        @Override
        public Set<Entry<Short, Object>> entrySet() {
            return Collections.emptySet();
        }
    }

    /**
     * An unmodifiable wrapper around a {@link ShortObjectMap}.
     *
     * @param <V> the value type stored in the map.
     */

    /**
     * 一个不可修改的 {@link ShortObjectMap} 包装器。
     *
     * @param <V> 存储在映射中的值类型。
     */
    private static final class UnmodifiableMap<V> implements ShortObjectMap<V> {
        private final ShortObjectMap<V> map;
        private Set<Short> keySet;
        private Set<Entry<Short, V>> entrySet;
        private Collection<V> values;
        private Iterable<PrimitiveEntry<V>> entries;

        UnmodifiableMap(ShortObjectMap<V> map) {
            this.map = map;
        }

        @Override
        public V get(short key) {
            return map.get(key);
        }

        @Override
        public V put(short key, V value) {
            throw new UnsupportedOperationException("put");
        }

        @Override
        public V remove(short key) {
            throw new UnsupportedOperationException("remove");
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("clear");
        }

        @Override
        public boolean containsKey(short key) {
            return map.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return map.containsValue(value);
        }

        @Override
        public boolean containsKey(Object key) {
            return map.containsKey(key);
        }

        @Override
        public V get(Object key) {
            return map.get(key);
        }

        @Override
        public V put(Short key, V value) {
            throw new UnsupportedOperationException("put");
        }

        @Override
        public V remove(Object key) {
            throw new UnsupportedOperationException("remove");
        }

        @Override
        public void putAll(Map<? extends Short, ? extends V> m) {
            throw new UnsupportedOperationException("putAll");
        }

        @Override
        public Iterable<PrimitiveEntry<V>> entries() {
            if (entries == null) {
                entries = new Iterable<PrimitiveEntry<V>>() {
                    @Override
                    public Iterator<PrimitiveEntry<V>> iterator() {
                        return new IteratorImpl(map.entries().iterator());
                    }
                };
            }

            return entries;
        }

        @Override
        public Set<Short> keySet() {
            if (keySet == null) {
                keySet = Collections.unmodifiableSet(map.keySet());
            }
            return keySet;
        }

        @Override
        public Set<Entry<Short, V>> entrySet() {
            if (entrySet == null) {
                entrySet = Collections.unmodifiableSet(map.entrySet());
            }
            return entrySet;
        }

        @Override
        public Collection<V> values() {
            if (values == null) {
                values = Collections.unmodifiableCollection(map.values());
            }
            return values;
        }

        /**
         * Unmodifiable wrapper for an iterator.
         */

        /**
         * 不可修改的迭代器包装类。
         */
        private class IteratorImpl implements Iterator<PrimitiveEntry<V>> {
            final Iterator<PrimitiveEntry<V>> iter;

            IteratorImpl(Iterator<PrimitiveEntry<V>> iter) {
                this.iter = iter;
            }

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public PrimitiveEntry<V> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return new EntryImpl(iter.next());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("remove");
            }
        }

        /**
         * Unmodifiable wrapper for an entry.
         */

        /**
         * 条目的不可修改包装器。
         */
        private class EntryImpl implements PrimitiveEntry<V> {
            private final PrimitiveEntry<V> entry;

            EntryImpl(PrimitiveEntry<V> entry) {
                this.entry = entry;
            }

            @Override
            public short key() {
                return entry.key();
            }

            @Override
            public V value() {
                return entry.value();
            }

            @Override
            public void setValue(V value) {
                throw new UnsupportedOperationException("setValue");
            }
        }
    }
}
