

package org.top.java.netty.source.util.collection;

import static io.netty.util.internal.MathUtil.safeFindNextPositivePowerOfTwo;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A hash map implementation of {@link ByteObjectMap} that uses open addressing for keys.
 * To minimize the memory footprint, this class uses open addressing rather than chaining.
 * Collisions are resolved using linear probing. Deletions implement compaction, so cost of
 * remove can approach O(N) for full maps, which makes a small loadFactor recommended.
 *
 * @param <V> The value type stored in the map.
 */

/**
 * 一个使用开放寻址的 {@link ByteObjectMap} 哈希映射实现。
 * 为了最小化内存占用，该类使用开放寻址而不是链式处理。
 * 冲突通过线性探测解决。删除操作实现了压缩，因此对于满映射，删除操作的代价可能接近 O(N)，因此建议使用较小的负载因子。
 *
 * @param <V> 存储在映射中的值类型。
 */
public class ByteObjectHashMap<V> implements ByteObjectMap<V> {

    /** Default initial capacity. Used if not specified in the constructor */

    /** 默认初始容量。如果未在构造函数中指定，则使用此值 */
    public static final int DEFAULT_CAPACITY = 8;

    /** Default load factor. Used if not specified in the constructor */

    /** 默认加载因子。如果在构造函数中未指定，则使用此值 */
    public static final float DEFAULT_LOAD_FACTOR = 0.5f;

    /**
     * Placeholder for null values, so we can use the actual null to mean available.
     * (Better than using a placeholder for available: less references for GC processing.)
     */

    /**
     * 用于表示空值的占位符，这样我们就可以使用实际的null来表示可用。
     * （比使用占位符表示可用更好：减少GC处理的引用。）
     */
    private static final Object NULL_VALUE = new Object();

    /** The maximum number of elements allowed without allocating more space. */

    /** 在不分配更多空间的情况下允许的最大元素数量。 */
    private int maxSize;

    /** The load factor for the map. Used to calculate {@link #maxSize}. */

    /** 映射的负载因子。用于计算 {@link #maxSize}。 */
    private final float loadFactor;

    private byte[] keys;
    private V[] values;
    private int size;
    private int mask;

    private final Set<Byte> keySet = new KeySet();
    private final Set<Entry<Byte, V>> entrySet = new EntrySet();
    private final Iterable<PrimitiveEntry<V>> entries = new Iterable<PrimitiveEntry<V>>() {
        @Override
        public Iterator<PrimitiveEntry<V>> iterator() {
            return new PrimitiveIterator();
        }
    };

    public ByteObjectHashMap() {
        this(DEFAULT_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    public ByteObjectHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public ByteObjectHashMap(int initialCapacity, float loadFactor) {
        if (loadFactor <= 0.0f || loadFactor > 1.0f) {
            // Cannot exceed 1 because we can never store more than capacity elements;
            // 不能超过1，因为我们永远无法存储超过容量的元素；
            // using a bigger loadFactor would trigger rehashing before the desired load is reached.
            // 使用更大的负载因子会在达到期望负载之前触发重新哈希。
            throw new IllegalArgumentException("loadFactor must be > 0 and <= 1");
        }

        this.loadFactor = loadFactor;

        // Adjust the initial capacity if necessary.

        // 如有必要，调整初始容量。
        int capacity = safeFindNextPositivePowerOfTwo(initialCapacity);
        mask = capacity - 1;

        // Allocate the arrays.

        // 分配数组。
        keys = new byte[capacity];
        @SuppressWarnings({ "unchecked", "SuspiciousArrayCast" })
        V[] temp = (V[]) new Object[capacity];
        values = temp;

        // Initialize the maximum size value.

        // 初始化最大尺寸值。
        maxSize = calcMaxSize(capacity);
    }

    private static <T> T toExternal(T value) {
        assert value != null : "null is not a legitimate internal value. Concurrent Modification?";
        return value == NULL_VALUE ? null : value;
    }

    @SuppressWarnings("unchecked")
    private static <T> T toInternal(T value) {
        return value == null ? (T) NULL_VALUE : value;
    }

    @Override
    public V get(byte key) {
        int index = indexOf(key);
        return index == -1 ? null : toExternal(values[index]);
    }

    @Override
    public V put(byte key, V value) {
        int startIndex = hashIndex(key);
        int index = startIndex;

        for (;;) {
            if (values[index] == null) {
                // Found empty slot, use it.
                // 找到空槽，使用它。
                keys[index] = key;
                values[index] = toInternal(value);
                growSize();
                return null;
            }
            if (keys[index] == key) {
                // Found existing entry with this key, just replace the value.
                // 找到具有此键的现有条目，只需替换值。
                V previousValue = values[index];
                values[index] = toInternal(value);
                return toExternal(previousValue);
            }

            // Conflict, keep probing ...

            // 冲突，继续探测...
            if ((index = probeNext(index)) == startIndex) {
                // Can only happen if the map was full at MAX_ARRAY_SIZE and couldn't grow.
                // 只有在映射在 MAX_ARRAY_SIZE 时已满且无法扩展时才会发生。
                throw new IllegalStateException("Unable to insert");
            }
        }
    }

    @Override
    public void putAll(Map<? extends Byte, ? extends V> sourceMap) {
        if (sourceMap instanceof ByteObjectHashMap) {
            // Optimization - iterate through the arrays.
            // 优化 - 遍历数组。
            @SuppressWarnings("unchecked")
            ByteObjectHashMap<V> source = (ByteObjectHashMap<V>) sourceMap;
            for (int i = 0; i < source.values.length; ++i) {
                V sourceValue = source.values[i];
                if (sourceValue != null) {
                    put(source.keys[i], sourceValue);
                }
            }
            return;
        }

        // Otherwise, just add each entry.

        // 否则，只需添加每个条目。
        for (Entry<? extends Byte, ? extends V> entry : sourceMap.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public V remove(byte key) {
        int index = indexOf(key);
        if (index == -1) {
            return null;
        }

        V prev = values[index];
        removeAt(index);
        return toExternal(prev);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public void clear() {
        Arrays.fill(keys, (byte) 0);
        Arrays.fill(values, null);
        size = 0;
    }

    @Override
    public boolean containsKey(byte key) {
        return indexOf(key) >= 0;
    }

    @Override
    public boolean containsValue(Object value) {
        @SuppressWarnings("unchecked")
        V v1 = toInternal((V) value);
        for (V v2 : values) {
            // The map supports null values; this will be matched as NULL_VALUE.equals(NULL_VALUE).
            // 该映射支持空值；这将匹配为 NULL_VALUE.equals(NULL_VALUE)。
            if (v2 != null && v2.equals(v1)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterable<PrimitiveEntry<V>> entries() {
        return entries;
    }

    @Override
    public Collection<V> values() {
        return new AbstractCollection<V>() {
            @Override
            public Iterator<V> iterator() {
                return new Iterator<V>() {
                    final PrimitiveIterator iter = new PrimitiveIterator();

                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    @Override
                    public V next() {
                        return iter.next().value();
                    }

                    @Override
                    public void remove() {
                        iter.remove();
                    }
                };
            }

            @Override
            public int size() {
                return size;
            }
        };
    }

    @Override
    public int hashCode() {
        // Hashcode is based on all non-zero, valid keys. We have to scan the whole keys
        // Hashcode 基于所有非零、有效的键。我们必须扫描整个键
        // array, which may have different lengths for two maps of same size(), so the
        // 数组，对于相同size()的两个映射，其长度可能不同，因此
        // capacity cannot be used as input for hashing but the size can.
        // 容量不能用作哈希的输入，但大小可以。
        int hash = size;
        for (byte key : keys) {
            // 0 can be a valid key or unused slot, but won't impact the hashcode in either case.
            // 0 可以是一个有效的键或未使用的槽，但在任何情况下都不会影响哈希码。
            // This way we can use a cheap loop without conditionals, or hard-to-unroll operations,
            // 这样我们就可以使用一个没有条件判断的廉价循环，或者难以展开的操作，
            // or the devastatingly bad memory locality of visiting value objects.
            // 或者访问值对象的灾难性内存局部性。
            // Also, it's important to use a hash function that does not depend on the ordering
            // 同样，重要的是使用一个不依赖于顺序的哈希函数
            // of terms, only their values; since the map is an unordered collection and
            // 术语，仅它们的值；由于映射是一个无序集合
            // entries can end up in different positions in different maps that have the same
            // 条目可能会在不同映射中最终处于不同位置，而这些映射具有相同的
            // elements, but with different history of puts/removes, due to conflicts.
            // 元素，但由于冲突，具有不同的放置/移除历史。
            hash ^= hashCode(key);
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ByteObjectMap)) {
            return false;
        }
        @SuppressWarnings("rawtypes")
        ByteObjectMap other = (ByteObjectMap) obj;
        if (size != other.size()) {
            return false;
        }
        for (int i = 0; i < values.length; ++i) {
            V value = values[i];
            if (value != null) {
                byte key = keys[i];
                Object otherValue = other.get(key);
                if (value == NULL_VALUE) {
                    if (otherValue != null) {
                        return false;
                    }
                } else if (!value.equals(otherValue)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean containsKey(Object key) {
        return containsKey(objectToKey(key));
    }

    @Override
    public V get(Object key) {
        return get(objectToKey(key));
    }

    @Override
    public V put(Byte key, V value) {
        return put(objectToKey(key), value);
    }

    @Override
    public V remove(Object key) {
        return remove(objectToKey(key));
    }

    @Override
    public Set<Byte> keySet() {
        return keySet;
    }

    @Override
    public Set<Entry<Byte, V>> entrySet() {
        return entrySet;
    }

    private byte objectToKey(Object key) {
        return (byte) ((Byte) key).byteValue();
    }

    /**
     * Locates the index for the given key. This method probes using double hashing.
     *
     * @param key the key for an entry in the map.
     * @return the index where the key was found, or {@code -1} if no entry is found for that key.
     */

    /**
     * 定位给定键的索引。该方法使用双重哈希进行探测。
     *
     * @param key 映射中某个条目的键。
     * @return 找到键的索引，如果未找到该键的条目，则返回 {@code -1}。
     */
    private int indexOf(byte key) {
        int startIndex = hashIndex(key);
        int index = startIndex;

        for (;;) {
            if (values[index] == null) {
                // It's available, so no chance that this value exists anywhere in the map.
                // 它是可用的，所以该值不可能存在于映射中的任何位置。
                return -1;
            }
            if (key == keys[index]) {
                return index;
            }

            // Conflict, keep probing ...

            // 冲突，继续探测...
            if ((index = probeNext(index)) == startIndex) {
                return -1;
            }
        }
    }

    /**
     * Returns the hashed index for the given key.
     */

    /**
     * 返回给定键的哈希索引。
     */
    private int hashIndex(byte key) {
        // The array lengths are always a power of two, so we can use a bitmask to stay inside the array bounds.
        // 数组长度始终是2的幂，因此我们可以使用位掩码来保持在数组边界内。
        return hashCode(key) & mask;
    }

    /**
     * Returns the hash code for the key.
     */

    /**
     * 返回键的哈希码。
     */
    private static int hashCode(byte key) {
       return (int) key;
    }

    /**
     * Get the next sequential index after {@code index} and wraps if necessary.
     */

    /**
     * 获取 {@code index} 之后的下一个顺序索引，并在必要时进行回绕。
     */
    private int probeNext(int index) {
        // The array lengths are always a power of two, so we can use a bitmask to stay inside the array bounds.
        // 数组长度始终是2的幂，因此我们可以使用位掩码来保持在数组边界内。
        return (index + 1) & mask;
    }

    /**
     * Grows the map size after an insertion. If necessary, performs a rehash of the map.
     */

    /**
     * 在插入后扩大映射的大小。如果必要，执行映射的重新哈希。
     */
    private void growSize() {
        size++;

        if (size > maxSize) {
            if(keys.length == Integer.MAX_VALUE) {
                throw new IllegalStateException("Max capacity reached at size=" + size);
            }

            // Double the capacity.

            // 将容量加倍。
            rehash(keys.length << 1);
        }
    }

    /**
     * Removes entry at the given index position. Also performs opportunistic, incremental rehashing
     * if necessary to not break conflict chains.
     *
     * @param index the index position of the element to remove.
     * @return {@code true} if the next item was moved back. {@code false} otherwise.
     */

    /**
     * 移除给定索引位置的条目。如果必要，还会执行机会性、增量式重新哈希，以避免破坏冲突链。
     *
     * @param index 要移除元素的索引位置。
     * @return {@code true} 如果下一个项目被移回。否则返回 {@code false}。
     */
    private boolean removeAt(final int index) {
        --size;
        // Clearing the key is not strictly necessary (for GC like in a regular collection),
        // 清除键并不是严格必要的（对于像常规集合中的 GC 来说），
        // but recommended for security. The memory location is still fresh in the cache anyway.
        // 但出于安全考虑，建议这样做。内存位置在缓存中仍然是新鲜的。
        keys[index] = 0;
        values[index] = null;

        // In the interval from index to the next available entry, the arrays may have entries

        // 在从索引到下一个可用条目的区间内，数组可能包含条目
        // that are displaced from their base position due to prior conflicts. Iterate these
        // 由于之前的冲突，这些从它们的基本位置被移除了。迭代这些
        // entries and move them back if possible, optimizing future lookups.
        // 条目并将它们移回（如果可能），以优化未来的查找。
        // Knuth Section 6.4 Algorithm R, also used by the JDK's IdentityHashMap.
        // Knuth 第6.4节算法R，也被JDK的IdentityHashMap使用。

        int nextFree = index;
        int i = probeNext(index);
        for (V value = values[i]; value != null; value = values[i = probeNext(i)]) {
            byte key = keys[i];
            int bucket = hashIndex(key);
            if (i < bucket && (bucket <= nextFree || nextFree <= i) ||
                bucket <= nextFree && nextFree <= i) {
                // Move the displaced entry "back" to the first available position.
                // 将移位的条目“移回”到第一个可用的位置。
                keys[nextFree] = key;
                values[nextFree] = value;
                // Put the first entry after the displaced entry
                // 将第一个条目放在被替换的条目之后
                keys[i] = 0;
                values[i] = null;
                nextFree = i;
            }
        }
        return nextFree != index;
    }

    /**
     * Calculates the maximum size allowed before rehashing.
     */

    /**
     * 计算在重新哈希之前允许的最大大小。
     */
    private int calcMaxSize(int capacity) {
        // Clip the upper bound so that there will always be at least one available slot.
        // 剪裁上限，以确保始终至少有一个可用槽位。
        int upperBound = capacity - 1;
        return Math.min(upperBound, (int) (capacity * loadFactor));
    }

    /**
     * Rehashes the map for the given capacity.
     *
     * @param newCapacity the new capacity for the map.
     */

    /**
     * 为给定的容量重新哈希映射。
     *
     * @param newCapacity 映射的新容量。
     */
    private void rehash(int newCapacity) {
        byte[] oldKeys = keys;
        V[] oldVals = values;

        keys = new byte[newCapacity];
        @SuppressWarnings({ "unchecked", "SuspiciousArrayCast" })
        V[] temp = (V[]) new Object[newCapacity];
        values = temp;

        maxSize = calcMaxSize(newCapacity);
        mask = newCapacity - 1;

        // Insert to the new arrays.

        // 插入到新数组中。
        for (int i = 0; i < oldVals.length; ++i) {
            V oldVal = oldVals[i];
            if (oldVal != null) {
                // Inlined put(), but much simpler: we don't need to worry about
                // 内联 put()，但更简单：我们不需要担心
                // duplicated keys, growing/rehashing, or failing to insert.
                // 重复的键，扩展/重新哈希，或插入失败。
                byte oldKey = oldKeys[i];
                int index = hashIndex(oldKey);

                for (;;) {
                    if (values[index] == null) {
                        keys[index] = oldKey;
                        values[index] = oldVal;
                        break;
                    }

                    // Conflict, keep probing. Can wrap around, but never reaches startIndex again.

                    // 冲突，继续探测。可以环绕，但永远不会再次到达startIndex。
                    index = probeNext(index);
                }
            }
        }
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder(4 * size);
        sb.append('{');
        boolean first = true;
        for (int i = 0; i < values.length; ++i) {
            V value = values[i];
            if (value != null) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(keyToString(keys[i])).append('=').append(value == this ? "(this Map)" :
                    toExternal(value));
                first = false;
            }
        }
        return sb.append('}').toString();
    }

    /**
     * Helper method called by {@link #toString()} in order to convert a single map key into a string.
     * This is protected to allow subclasses to override the appearance of a given key.
     */

    /**
     * 由 {@link #toString()} 调用的辅助方法，用于将单个映射键转换为字符串。
     * 此方法被保护以允许子类覆盖给定键的外观。
     */
    protected String keyToString(byte key) {
        return Byte.toString(key);
    }

    /**
     * Set implementation for iterating over the entries of the map.
     */

    /**
     * 用于迭代映射条目的集合实现。
     */
    private final class EntrySet extends AbstractSet<Entry<Byte, V>> {
        @Override
        public Iterator<Entry<Byte, V>> iterator() {
            return new MapIterator();
        }

        @Override
        public int size() {
            return ByteObjectHashMap.this.size();
        }
    }

    /**
     * Set implementation for iterating over the keys.
     */

    /**
     * 用于遍历键的Set实现。
     */
    private final class KeySet extends AbstractSet<Byte> {
        @Override
        public int size() {
            return ByteObjectHashMap.this.size();
        }

        @Override
        public boolean contains(Object o) {
            return ByteObjectHashMap.this.containsKey(o);
        }

        @Override
        public boolean remove(Object o) {
            return ByteObjectHashMap.this.remove(o) != null;
        }

        @Override
        public boolean retainAll(Collection<?> retainedKeys) {
            boolean changed = false;
            for(Iterator<PrimitiveEntry<V>> iter = entries().iterator(); iter.hasNext(); ) {
                PrimitiveEntry<V> entry = iter.next();
                if (!retainedKeys.contains(entry.key())) {
                    changed = true;
                    iter.remove();
                }
            }
            return changed;
        }

        @Override
        public void clear() {
            ByteObjectHashMap.this.clear();
        }

        @Override
        public Iterator<Byte> iterator() {
            return new Iterator<Byte>() {
                private final Iterator<Entry<Byte, V>> iter = entrySet.iterator();

                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public Byte next() {
                    return iter.next().getKey();
                }

                @Override
                public void remove() {
                    iter.remove();
                }
            };
        }
    }

    /**
     * Iterator over primitive entries. Entry key/values are overwritten by each call to {@link #next()}.
     */

    /**
     * 遍历原始条目的迭代器。每次调用 {@link #next()} 都会覆盖条目的键/值。
     */
    private final class PrimitiveIterator implements Iterator<PrimitiveEntry<V>>, PrimitiveEntry<V> {
        private int prevIndex = -1;
        private int nextIndex = -1;
        private int entryIndex = -1;

        private void scanNext() {
            while (++nextIndex != values.length && values[nextIndex] == null) {
            }
        }

        @Override
        public boolean hasNext() {
            if (nextIndex == -1) {
                scanNext();
            }
            return nextIndex != values.length;
        }

        @Override
        public PrimitiveEntry<V> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            prevIndex = nextIndex;
            scanNext();

            // Always return the same Entry object, just change its index each time.

            // 始终返回相同的 Entry 对象，只是每次更改其索引。
            entryIndex = prevIndex;
            return this;
        }

        @Override
        public void remove() {
            if (prevIndex == -1) {
                throw new IllegalStateException("next must be called before each remove.");
            }
            if (removeAt(prevIndex)) {
                // removeAt may move elements "back" in the array if they have been displaced because their spot in the
                // removeAt 可能会将数组中的元素“向后”移动，如果它们的位置因为被占用而发生了位移。
                // array was occupied when they were inserted. If this occurs then the nextIndex is now invalid and
                // 当它们被插入时，数组已被占用。如果发生这种情况，那么 nextIndex 现在无效了。
                // should instead point to the prevIndex which now holds an element which was "moved back".
                // 应该指向 prevIndex，它现在持有一个被“移回”的元素。
                nextIndex = prevIndex;
            }
            prevIndex = -1;
        }

        // Entry implementation. Since this implementation uses a single Entry, we coalesce that

        // 入口实现。由于此实现使用单个入口，我们将其合并
        // into the Iterator object (potentially making loop optimization much easier).
        // 转换为 Iterator 对象（可能使循环优化更加容易）。

        @Override
        public byte key() {
            return keys[entryIndex];
        }

        @Override
        public V value() {
            return toExternal(values[entryIndex]);
        }

        @Override
        public void setValue(V value) {
            values[entryIndex] = toInternal(value);
        }
    }

    /**
     * Iterator used by the {@link Map} interface.
     */

    /**
     * 用于 {@link Map} 接口的迭代器。
     */
    private final class MapIterator implements Iterator<Entry<Byte, V>> {
        private final PrimitiveIterator iter = new PrimitiveIterator();

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public Entry<Byte, V> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            iter.next();

            return new MapEntry(iter.entryIndex);
        }

        @Override
        public void remove() {
            iter.remove();
        }
    }

    /**
     * A single entry in the map.
     */

    /**
     * 映射中的单个条目。
     */
    final class MapEntry implements Entry<Byte, V> {
        private final int entryIndex;

        MapEntry(int entryIndex) {
            this.entryIndex = entryIndex;
        }

        @Override
        public Byte getKey() {
            verifyExists();
            return keys[entryIndex];
        }

        @Override
        public V getValue() {
            verifyExists();
            return toExternal(values[entryIndex]);
        }

        @Override
        public V setValue(V value) {
            verifyExists();
            V prevValue = toExternal(values[entryIndex]);
            values[entryIndex] = toInternal(value);
            return prevValue;
        }

        private void verifyExists() {
            if (values[entryIndex] == null) {
                throw new IllegalStateException("The map entry has been removed");
            }
        }
    }
}
