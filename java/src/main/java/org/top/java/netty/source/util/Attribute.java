
package org.top.java.netty.source.util;

/**
 * An attribute which allows to store a value reference. It may be updated atomically and so is thread-safe.
 *
 * @param <T>   the type of the value it holds.
 */

/**
 * 一个允许存储值引用的属性。它可以原子性地更新，因此是线程安全的。
 *
 * @param <T>   它持有的值的类型。
 */
public interface Attribute<T> {

    /**
     * Returns the key of this attribute.
     */

    /**
     * 返回此属性的键。
     */
    AttributeKey<T> key();

    /**
     * Returns the current value, which may be {@code null}
     */

    /**
     * 返回当前值，可能为 {@code null}
     */
    T get();

    /**
     * Sets the value
     */

    /**
     * 设置值
     */
    void set(T value);

    /**
     *  Atomically sets to the given value and returns the old value which may be {@code null} if non was set before.
     */

    /**
     * 原子性地设置为给定值并返回旧值，如果之前没有设置，则可能为 {@code null}。
     */
    T getAndSet(T value);

    /**
     *  Atomically sets to the given value if this {@link Attribute}'s value is {@code null}.
     *  If it was not possible to set the value as it contains a value it will just return the current value.
     */

    /**
     *  如果此 {@link Attribute} 的值为 {@code null}，则原子地设置为给定值。
     *  如果由于已包含值而无法设置，则直接返回当前值。
     */
    T setIfAbsent(T value);

    /**
     * Removes this attribute from the {@link AttributeMap} and returns the old value. Subsequent {@link #get()}
     * calls will return {@code null}.
     *
     * If you only want to return the old value and clear the {@link Attribute} while still keep it in the
     * {@link AttributeMap} use {@link #getAndSet(Object)} with a value of {@code null}.
     *
     * <p>
     * Be aware that even if you call this method another thread that has obtained a reference to this {@link Attribute}
     * via {@link AttributeMap#attr(AttributeKey)} will still operate on the same instance. That said if now another
     * thread or even the same thread later will call {@link AttributeMap#attr(AttributeKey)} again, a new
     * {@link Attribute} instance is created and so is not the same as the previous one that was removed. Because of
     * this special caution should be taken when you call {@link #remove()} or {@link #getAndRemove()}.
     *
     * @deprecated please consider using {@link #getAndSet(Object)} (with value of {@code null}).
     */

    /**
     * 从 {@link AttributeMap} 中移除该属性并返回旧值。后续的 {@link #get()} 调用将返回 {@code null}。
     *
     * 如果你只想返回旧值并清除 {@link Attribute}，同时仍然将其保留在 {@link AttributeMap} 中，请使用 {@link #getAndSet(Object)} 并将值设置为 {@code null}。
     *
     * <p>
     * 请注意，即使你调用了此方法，另一个通过 {@link AttributeMap#attr(AttributeKey)} 获取到此 {@link Attribute} 引用的线程仍将操作相同的实例。也就是说，如果现在另一个线程或甚至同一个线程稍后再次调用 {@link AttributeMap#attr(AttributeKey)}，将创建一个新的 {@link Attribute} 实例，因此与之前移除的那个实例不同。因此，在调用 {@link #remove()} 或 {@link #getAndRemove()} 时应特别小心。
     *
     * @deprecated 请考虑使用 {@link #getAndSet(Object)}（将值设置为 {@code null}）。
     */
    @Deprecated
    T getAndRemove();

    /**
     * Atomically sets the value to the given updated value if the current value == the expected value.
     * If it the set was successful it returns {@code true} otherwise {@code false}.
     */

    /**
     * 如果当前值等于预期值，则以原子方式将值设置为给定的更新值。
     * 如果设置成功，则返回 {@code true}，否则返回 {@code false}。
     */
    boolean compareAndSet(T oldValue, T newValue);

    /**
     * Removes this attribute from the {@link AttributeMap}. Subsequent {@link #get()} calls will return @{code null}.
     *
     * If you only want to remove the value and clear the {@link Attribute} while still keep it in
     * {@link AttributeMap} use {@link #set(Object)} with a value of {@code null}.
     *
     * <p>
     * Be aware that even if you call this method another thread that has obtained a reference to this {@link Attribute}
     * via {@link AttributeMap#attr(AttributeKey)} will still operate on the same instance. That said if now another
     * thread or even the same thread later will call {@link AttributeMap#attr(AttributeKey)} again, a new
     * {@link Attribute} instance is created and so is not the same as the previous one that was removed. Because of
     * this special caution should be taken when you call {@link #remove()} or {@link #getAndRemove()}.
     *
     * @deprecated please consider using {@link #set(Object)} (with value of {@code null}).
     */

    /**
     * 从 {@link AttributeMap} 中移除此属性。后续的 {@link #get()} 调用将返回 @{code null}。
     *
     * 如果你只想移除值并清除 {@link Attribute}，但仍将其保留在 {@link AttributeMap} 中，请使用 {@link #set(Object)} 并将值设为 {@code null}。
     *
     * <p>
     * 请注意，即使你调用了此方法，另一个线程通过 {@link AttributeMap#attr(AttributeKey)} 获取到此 {@link Attribute} 的引用仍将在同一实例上操作。也就是说，如果现在另一个线程或甚至同一线程稍后再次调用 {@link AttributeMap#attr(AttributeKey)}，将会创建一个新的 {@link Attribute} 实例，因此与之前移除的实例不同。因此，在调用 {@link #remove()} 或 {@link #getAndRemove()} 时应特别小心。
     *
     * @deprecated 请考虑使用 {@link #set(Object)}（将值设为 {@code null}）。
     */
    @Deprecated
    void remove();
}
