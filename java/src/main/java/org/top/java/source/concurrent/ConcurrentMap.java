package org.top.java.source.concurrent;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 上午9:10
 */

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 一个提供线程安全性和原子性保证的 {@link java.util.Map}。
 *
 * <p>内存一致性效果：与其他并发集合一样，在一个线程中将对象作为
 * 键或值放入 {@code ConcurrentMap} 中的操作
 * <a href="package-summary.html#MemoryVisibility"><i>先行发生</i></a>
 * 于随后另一个线程访问或移除该对象的操作。
 *
 * <p>该接口是
 * <a href="{@docRoot}/../technotes/guides/collections/index.html">
 * Java 集合框架</a> 的成员。
 *
 * @since 1.5
 * @author Doug Lea
 * @param <K> 该映射维护的键的类型
 * @param <V> 该映射值的类型
 */
public interface ConcurrentMap<K, V> extends Map<K, V> {

    /**
     * {@inheritDoc}
     *
     * @implNote 此实现假设 ConcurrentMap 不能包含 null 值，并且 {@code get()} 返回 null 明确表示
     * 键不存在。支持 null 值的实现<strong>必须</strong>覆盖此默认实现。
     *
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default V getOrDefault(Object key, V defaultValue) {
        V v;
        return ((v = get(key)) != null) ? v : defaultValue;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec 默认实现相当于对于这个 {@code map}：
     * <pre> {@code
     * for ((Map.Entry<K, V> entry : map.entrySet())
     *     action.accept(entry.getKey(), entry.getValue());
     * }</pre>
     *
     * @implNote 默认实现假设 {@code getKey()} 或 {@code getValue()} 抛出的 {@code IllegalStateException}
     * 表示该条目已被删除，且无法处理。操作会继续处理后续条目。
     *
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        for (Map.Entry<K, V> entry : entrySet()) {
            K k;
            V v;
            try {
                k = entry.getKey();
                v = entry.getValue();
            } catch (IllegalStateException ise) {
                // 这通常意味着条目不再存在于映射中。
                continue;
            }
            action.accept(k, v);
        }
    }

    /**
     * 如果指定的键当前没有关联值，则将其与给定值关联。
     * 这相当于：
     * <pre> {@code
     * if (!map.containsKey(key))
     *   return map.put(key, value);
     * else
     *   return map.get(key);
     * }</pre>
     *
     * 不同之处在于该操作是原子执行的。
     *
     * @implNote 此实现有意重新抽象了 {@code Map} 中不适当的默认提供的行为。
     *
     * @param key 需要关联值的键
     * @param value 要与指定键关联的值
     * @return 先前与指定键关联的值，或者如果该键没有映射，则返回 {@code null}。
     *         （返回 {@code null} 也可能表示映射先前将 {@code null} 与键关联，
     *         如果实现支持 null 值。）
     * @throws UnsupportedOperationException 如果此映射不支持 {@code put} 操作
     * @throws ClassCastException 如果指定键或值的类型不适合存储在该映射中
     * @throws NullPointerException 如果指定键或值为 null，且此映射不允许 null 键或值
     * @throws IllegalArgumentException 如果指定键或值的某些属性不允许其存储在此映射中
     */
    V putIfAbsent(K key, V value);

    /**
     * 仅当当前映射到给定值时才移除指定键的条目。
     * 这相当于：
     * <pre> {@code
     * if (map.containsKey(key) && Objects.equals(map.get(key), value)) {
     *   map.remove(key);
     *   return true;
     * } else
     *   return false;
     * }</pre>
     *
     * 不同之处在于该操作是原子执行的。
     *
     * @implNote 此实现有意重新抽象了 {@code Map} 中不适当的默认提供的行为。
     *
     * @param key 需要移除的条目对应的键
     * @param value 预期与指定键关联的值
     * @return 如果该值已被移除，则返回 {@code true}
     * @throws UnsupportedOperationException 如果此映射不支持 {@code remove} 操作
     * @throws ClassCastException 如果键或值的类型不适合存储在该映射中
     *         (<a href="../Collection.html#optional-restrictions">可选限制</a>)
     * @throws NullPointerException 如果指定的键或值为 null，且此映射不允许 null 键或值
     *         (<a href="../Collection.html#optional-restrictions">可选限制</a>)
     */
    boolean remove(Object key, Object value);

    /**
     * 仅当当前映射到给定值时，替换指定键的条目。
     * 这相当于：
     * <pre> {@code
     * if (map.containsKey(key) && Objects.equals(map.get(key), oldValue)) {
     *   map.put(key, newValue);
     *   return true;
     * } else
     *   return false;
     * }</pre>
     *
     * 不同之处在于该操作是原子执行的。
     *
     * @implNote 此实现有意重新抽象了 {@code Map} 中不适当的默认提供的行为。
     *
     * @param key 需要替换的条目对应的键
     * @param oldValue 预期与指定键关联的旧值
     * @param newValue 要与指定键关联的新值
     * @return 如果该值已被替换，则返回 {@code true}
     * @throws UnsupportedOperationException 如果此映射不支持 {@code put} 操作
     * @throws ClassCastException 如果指定键或值的类型不适合存储在该映射中
     * @throws NullPointerException 如果指定键或值为 null，且此映射不允许 null 键或值
     * @throws IllegalArgumentException 如果指定键或值的某些属性不允许其存储在此映射中
     */
    boolean replace(K key, V oldValue, V newValue);

    /**
     * 仅当当前映射到某个值时，替换指定键的条目。
     * 这相当于：
     * <pre> {@code
     * if (map.containsKey(key)) {
     *   return map.put(key, value);
     * } else
     *   return null;
     * }</pre>
     *
     * 不同之处在于该操作是原子执行的。
     *
     * @implNote 此实现有意重新抽象了 {@code Map} 中不适当的默认提供的行为。
     *
     * @param key 需要替换的条目对应的键
     * @param value 要与指定键关联的新值
     * @return 先前与指定键关联的值，或如果没有映射，则返回 {@code null}。
     *         （返回 {@code null} 也可能表示映射先前将 {@code null} 与键关联，
     *         如果实现支持 null 值。）
     * @throws UnsupportedOperationException 如果此映射不支持 {@code put} 操作
     * @throws ClassCastException 如果指定键或值的类型不适合存储在该映射中
     * @throws NullPointerException 如果指定键或值为 null，且此映射不允许 null 键或值
     * @throws IllegalArgumentException 如果指定键或值的某些属性不允许其存储在此映射中
     */
    V replace(K key, V value);

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * 默认实现相当于，对于此 {@code map}：
     * <pre> {@code
     * for ((Map.Entry<K, V> entry : map.entrySet())
     *     do {
     *        K k = entry.getKey();
     *        V v = entry.getValue();
     *     } while(!replace(k, v, function.apply(k, v)));
     * }</pre>
     *
     * 当多个线程尝试更新时，默认实现可能会重试这些步骤，
     * 包括潜在地为某个键多次调用该函数。
     *
     * <p>此实现假设 ConcurrentMap 不能包含 null 值，并且 {@code get()} 返回 null 明确表示该键不存在。
     * 支持 null 值的实现<strong>必须</strong>覆盖此默认实现。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws IllegalArgumentException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        forEach((k, v) -> {
            while (!replace(k, v, function.apply(k, v))) {
                // 值已更改或键已不存在
                if ((v = get(k)) == null) {
                    // 键已不再存在于映射中。
                    break;
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * 默认实现相当于执行以下步骤，对于此 {@code map}：
     *
     * <pre> {@code
     * if (map.get(key) == null) {
     *     V newValue = mappingFunction.apply(key);
     *     if (newValue != null)
     *         return map.putIfAbsent(key, newValue);
     * }
     * }</pre>
     *
     * 当多个线程尝试更新时，默认实现可能会重试这些步骤，
     * 包括潜在地多次调用映射函数。
     *
     * <p>此实现假设 ConcurrentMap 不能包含 null 值，并且 {@code get()} 返回 null 明确表示该键不存在。
     * 支持 null 值的实现<strong>必须</strong>覆盖此默认实现。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        Objects.requireNonNull(mappingFunction);
        V v, newValue;
        return ((v = get(key)) == null &&
                (newValue = mappingFunction.apply(key)) != null &&
                (v = putIfAbsent(key, newValue)) == null) ? newValue : v;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * 默认实现相当于执行以下步骤，对于此 {@code map}：
     *
     * <pre> {@code
     * if (map.get(key) != null) {
     *     V oldValue = map.get(key);
     *     V newValue = remappingFunction.apply(key, oldValue);
     *     if (newValue != null)
     *         map.replace(key, oldValue, newValue);
     *     else
     *         map.remove(key, oldValue);
     * }
     * }</pre>
     *
     * 当多个线程尝试更新时，默认实现可能会重试这些步骤，
     * 包括潜在地多次调用重新映射函数。
     *
     * <p>此实现假设 ConcurrentMap 不能包含 null 值，并且 {@code get()} 返回 null 明确表示该键不存在。
     * 支持 null 值的实现<strong>必须</strong>覆盖此默认实现。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        V oldValue;
        while ((oldValue = get(key)) != null) {
            V newValue = remappingFunction.apply(key, oldValue);
            if (newValue != null) {
                if (replace(key, oldValue, newValue))
                    return newValue;
            } else if (remove(key, oldValue))
                return null;
        }
        return oldValue;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * 默认实现相当于执行以下步骤，对于此 {@code map}，然后返回当前值或如果现在不存在则返回 {@code null}：
     *
     * <pre> {@code
     * V oldValue = map.get(key);
     * V newValue = remappingFunction.apply(key, oldValue);
     * if (oldValue != null ) {
     *    if (newValue != null)
     *       map.replace(key, oldValue, newValue);
     *    else
     *       map.remove(key, oldValue);
     * } else {
     *    if (newValue != null)
     *       map.putIfAbsent(key, newValue);
     *    else
     *       return null;
     * }
     * }</pre>
     *
     * 当多个线程尝试更新时，默认实现可能会重试这些步骤，
     * 包括潜在地多次调用重新映射函数。
     *
     * <p>此实现假设 ConcurrentMap 不能包含 null 值，并且 {@code get()} 返回 null 明确表示该键不存在。
     * 支持 null 值的实现<strong>必须</strong>覆盖此默认实现。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        V oldValue = get(key);
        for (;;) {
            V newValue = remappingFunction.apply(key, oldValue);
            if (newValue == null) {
                // 删除映射
                if (oldValue != null || containsKey(key)) {
                    // 有需要删除的内容
                    if (remove(key, oldValue)) {
                        // 按预期删除了旧值
                        return null;
                    }

                    // 其他值替换了旧值，重试
                    oldValue = get(key);
                } else {
                    // 无事可做，保持现状
                    return null;
                }
            } else {
                // 添加或替换旧映射
                if (oldValue != null) {
                    // 替换
                    if (replace(key, oldValue, newValue)) {
                        // 按预期替换
                        return newValue;
                    }

                    // 其他值替换了旧值，重试
                    oldValue = get(key);
                } else {
                    // 添加（如果旧值为 null 则替换）
                    if ((oldValue = putIfAbsent(key, newValue)) == null) {
                        // 替换成功
                        return newValue;
                    }

                    // 其他值替换了旧值，重试
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec
     * 默认实现相当于执行以下步骤，对于此 {@code map}，然后返回当前值或如果不存在则返回 {@code null}：
     *
     * <pre> {@code
     * V oldValue = map.get(key);
     * V newValue = (oldValue == null) ? value :
     *              remappingFunction.apply(oldValue, value);
     * if (newValue == null)
     *     map.remove(key);
     * else
     *     map.put(key, newValue);
     * }</pre>
     *
     * 当多个线程尝试更新时，默认实现可能会重试这些步骤，
     * 包括潜在地多次调用重新映射函数。
     *
     * <p>此实现假设 ConcurrentMap 不能包含 null 值，并且 {@code get()} 返回 null 明确表示该键不存在。
     * 支持 null 值的实现<strong>必须</strong>覆盖此默认实现。
     *
     * @throws UnsupportedOperationException {@inheritDoc}
     * @throws ClassCastException {@inheritDoc}
     * @throws NullPointerException {@inheritDoc}
     * @since 1.8
     */
    @Override
    default V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        Objects.requireNonNull(value);
        V oldValue = get(key);
        for (;;) {
            if (oldValue != null) {
                V newValue = remappingFunction.apply(oldValue, value);
                if (newValue != null) {
                    if (replace(key, oldValue, newValue))
                        return newValue;
                } else if (remove(key, oldValue)) {
                    return null;
                }
                oldValue = get(key);
            } else {
                if ((oldValue = putIfAbsent(key, value)) == null) {
                    return value;
                }
            }
        }
    }
}
