

package org.top.java.netty.source.util;

import static io.netty.util.internal.ObjectUtil.checkNotNull;
import static io.netty.util.internal.ObjectUtil.checkNonEmpty;

import org.top.java.netty.source.util.internal.PlatformDependent;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A pool of {@link Constant}s.
 *
 * @param <T> the type of the constant
 */

/**
 * 一个{@link Constant}的池。
 *
 * @param <T> 常量的类型
 */
public abstract class ConstantPool<T extends Constant<T>> {

    private final ConcurrentMap<String, T> constants = PlatformDependent.newConcurrentHashMap();

    private final AtomicInteger nextId = new AtomicInteger(1);

    /**
     * Shortcut of {@link #valueOf(String) valueOf(firstNameComponent.getName() + "#" + secondNameComponent)}.
     */

    /**
     * {@link #valueOf(String) valueOf(firstNameComponent.getName() + "#" + secondNameComponent)} 的快捷方式。
     */
    public T valueOf(Class<?> firstNameComponent, String secondNameComponent) {
        return valueOf(
                checkNotNull(firstNameComponent, "firstNameComponent").getName() +
                '#' +
                checkNotNull(secondNameComponent, "secondNameComponent"));
    }

    /**
     * Returns the {@link Constant} which is assigned to the specified {@code name}.
     * If there's no such {@link Constant}, a new one will be created and returned.
     * Once created, the subsequent calls with the same {@code name} will always return the previously created one
     * (i.e. singleton.)
     *
     * @param name the name of the {@link Constant}
     */

    /**
     * 返回分配给指定 {@code name} 的 {@link Constant}。
     * 如果没有这样的 {@link Constant}，将会创建一个新的并返回。
     * 一旦创建，后续使用相同 {@code name} 的调用将始终返回之前创建的那个（即单例）。
     *
     * @param name {@link Constant} 的名称
     */
    public T valueOf(String name) {
        return getOrCreate(checkNonEmpty(name, "name"));
    }

    /**
     * Get existing constant by name or creates new one if not exists. Threadsafe
     *
     * @param name the name of the {@link Constant}
     */

    /**
     * 根据名称获取现有常量，如果不存在则创建新常量。线程安全
     *
     * @param name {@link Constant} 的名称
     */
    private T getOrCreate(String name) {
        T constant = constants.get(name);
        if (constant == null) {
            final T tempConstant = newConstant(nextId(), name);
            constant = constants.putIfAbsent(name, tempConstant);
            if (constant == null) {
                return tempConstant;
            }
        }

        return constant;
    }

    /**
     * Returns {@code true} if a {@link AttributeKey} exists for the given {@code name}.
     */

    /**
     * 如果给定的 {@code name} 存在 {@link AttributeKey}，则返回 {@code true}。
     */
    public boolean exists(String name) {
        return constants.containsKey(checkNonEmpty(name, "name"));
    }

    /**
     * Creates a new {@link Constant} for the given {@code name} or fail with an
     * {@link IllegalArgumentException} if a {@link Constant} for the given {@code name} exists.
     */

    /**
     * 为给定的 {@code name} 创建一个新的 {@link Constant}，如果给定 {@code name} 的 {@link Constant} 已存在，则抛出 {@link IllegalArgumentException}。
     */
    public T newInstance(String name) {
        return createOrThrow(checkNonEmpty(name, "name"));
    }

    /**
     * Creates constant by name or throws exception. Threadsafe
     *
     * @param name the name of the {@link Constant}
     */

    /**
     * 通过名称创建常量或抛出异常。线程安全
     *
     * @param name {@link Constant} 的名称
     */
    private T createOrThrow(String name) {
        T constant = constants.get(name);
        if (constant == null) {
            final T tempConstant = newConstant(nextId(), name);
            constant = constants.putIfAbsent(name, tempConstant);
            if (constant == null) {
                return tempConstant;
            }
        }

        throw new IllegalArgumentException(String.format("'%s' is already in use", name));
    }

    protected abstract T newConstant(int id, String name);

    @Deprecated
    public final int nextId() {
        return nextId.getAndIncrement();
    }
}
