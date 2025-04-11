
package org.top.java.netty.source.util.collection;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Utilities for byte-based primitive collections.
 */

/**
 * 用于基于字节的原始集合的工具类。
 */
public final class ByteCollections {

    private static final ByteObjectMap<Object> EMPTY_MAP = new EmptyMap();

    private ByteCollections() {
    }

    /**
     * Returns an unmodifiable empty {@link ByteObjectMap}.
     */

    /**
     * 返回一个不可修改的空 {@link ByteObjectMap}。
     */
    @SuppressWarnings("unchecked")
    public static <V> ByteObjectMap<V> emptyMap() {
        return (ByteObjectMap<V>) EMPTY_MAP;
    }

    /**
     * Creates an unmodifiable wrapper around the given map.
     */

    /**
     * 创建一个围绕给定映射的不可修改的包装器。
     */
    public static <V> ByteObjectMap<V> unmodifiableMap(final ByteObjectMap<V> map) {
        return new UnmodifiableMap<V>(map);
    }

    /**
     * An empty map. All operations that attempt to modify the map are unsupported.
     */

    /**
     * 一个空的映射。所有试图修改映射的操作都不被支持。
     */
    private static final class EmptyMap implements ByteObjectMap<Object> {
        @Override
        public Object get(byte key) {
            return null;
        }

        @Override
        public Object put(byte key, Object value) {
            throw new UnsupportedOperationException("put");
        }

        @Override
        public Object remove(byte key) {
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
        public Set<Byte> keySet() {
            return Collections.emptySet();
        }

        @Override
        public boolean containsKey(byte key) {
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
        public Object put(Byte key, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<? extends Byte, ?> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<Object> values() {
            return Collections.emptyList();
        }

        @Override
        public Set<Entry<Byte, Object>> entrySet() {
            return Collections.emptySet();
        }
    }

    /**
     * An unmodifiable wrapper around a {@link ByteObjectMap}.
     *
     * @param <V> the value type stored in the map.
     */

    /**
     * 一个围绕 {@link ByteObjectMap} 的不可修改的包装器。
     *
     * @param <V> 存储在映射中的值类型。
     */
    private static final class UnmodifiableMap<V> implements ByteObjectMap<V> {
        private final ByteObjectMap<V> map;
        private Set<Byte> keySet;
        private Set<Entry<Byte, V>> entrySet;
        private Collection<V> values;
        private Iterable<PrimitiveEntry<V>> entries;

        UnmodifiableMap(ByteObjectMap<V> map) {
            this.map = map;
        }

        @Override
        public V get(byte key) {
            return map.get(key);
        }

        @Override
        public V put(byte key, V value) {
            throw new UnsupportedOperationException("put");
        }

        @Override
        public V remove(byte key) {
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
        public boolean containsKey(byte key) {
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
        public V put(Byte key, V value) {
            throw new UnsupportedOperationException("put");
        }

        @Override
        public V remove(Object key) {
            throw new UnsupportedOperationException("remove");
        }

        @Override
        public void putAll(Map<? extends Byte, ? extends V> m) {
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
        public Set<Byte> keySet() {
            if (keySet == null) {
                keySet = Collections.unmodifiableSet(map.keySet());
            }
            return keySet;
        }

        @Override
        public Set<Entry<Byte, V>> entrySet() {
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
            public byte key() {
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
