package org.top.java.source.concurrent.atomic;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 上午10:59
 */

import sun.misc.Unsafe;

import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

/**
 * 一个可以原子更新的对象引用。有关原子变量的属性描述，请参阅
 * {@link java.util.concurrent.atomic} 包规范。
 * @since 1.5
 * 作者：Doug Lea
 * @param <V> 该引用所指对象的类型
 */
public class AtomicReference<V> implements java.io.Serializable {
    private static final long serialVersionUID = -1848883965231344442L;

    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long valueOffset;

    static {
        try {
            valueOffset = unsafe.objectFieldOffset
                    (AtomicReference.class.getDeclaredField("value"));
        } catch (Exception ex) { throw new Error(ex); }
    }

    private volatile V value;

    /**
     * 使用给定的初始值创建一个新的 AtomicReference。
     *
     * @param initialValue 初始值
     */
    public AtomicReference(V initialValue) {
        value = initialValue;
    }

    /**
     * 创建一个初始值为 null 的新 AtomicReference。
     */
    public AtomicReference() {
    }

    /**
     * 获取当前值。
     *
     * @return 当前值
     */
    public final V get() {
        return value;
    }

    /**
     * 设置为给定值。
     *
     * @param newValue 新值
     */
    public final void set(V newValue) {
        value = newValue;
    }

    /**
     * 最终设置为给定值。
     *
     * @param newValue 新值
     * @since 1.6
     */
    public final void lazySet(V newValue) {
        unsafe.putOrderedObject(this, valueOffset, newValue);
    }

    /**
     * 如果当前值 {@code ==} 预期值，则原子地将值设置为给定的更新值。
     * @param expect 预期值
     * @param update 新值
     * @return {@code true} 如果成功。返回 false 表示实际值与预期值不相等。
     */
    public final boolean compareAndSet(V expect, V update) {
        return unsafe.compareAndSwapObject(this, valueOffset, expect, update);
    }

    /**
     * 如果当前值 {@code ==} 预期值，则原子地将值设置为给定的更新值。
     *
     * <p><a href="package-summary.html#weakCompareAndSet">可能会偶发失败，
     * 并且不提供排序保证</a>，因此很少作为 {@code compareAndSet} 的替代方法。
     *
     * @param expect 预期值
     * @param update 新值
     * @return {@code true} 如果成功
     */
    public final boolean weakCompareAndSet(V expect, V update) {
        return unsafe.compareAndSwapObject(this, valueOffset, expect, update);
    }

    /**
     * 原子地设置为给定值并返回旧值。
     *
     * @param newValue 新值
     * @return 之前的值
     */
    @SuppressWarnings("unchecked")
    public final V getAndSet(V newValue) {
        return (V)unsafe.getAndSetObject(this, valueOffset, newValue);
    }

    /**
     * 使用给定函数原子地更新当前值，并返回之前的值。
     * 函数应无副作用，因为在由于线程间竞争导致更新尝试失败时可能会重新应用该函数。
     *
     * @param updateFunction 一个无副作用的函数
     * @return 之前的值
     * @since 1.8
     */
    public final V getAndUpdate(UnaryOperator<V> updateFunction) {
        V prev, next;
        do {
            prev = get();
            next = updateFunction.apply(prev);
        } while (!compareAndSet(prev, next));
        return prev;
    }

    /**
     * 使用给定函数原子地更新当前值，并返回更新后的值。
     * 函数应无副作用，因为在由于线程间竞争导致更新尝试失败时可能会重新应用该函数。
     *
     * @param updateFunction 一个无副作用的函数
     * @return 更新后的值
     * @since 1.8
     */
    public final V updateAndGet(UnaryOperator<V> updateFunction) {
        V prev, next;
        do {
            prev = get();
            next = updateFunction.apply(prev);
        } while (!compareAndSet(prev, next));
        return next;
    }

    /**
     * 使用给定函数对当前值和给定值进行操作，原子地更新当前值，并返回之前的值。
     * 函数应无副作用，因为在由于线程间竞争导致更新尝试失败时可能会重新应用该函数。
     * 函数的第一个参数是当前值，第二个参数是给定的更新值。
     *
     * @param x 更新值
     * @param accumulatorFunction 一个无副作用的双参数函数
     * @return 之前的值
     * @since 1.8
     */
    public final V getAndAccumulate(V x,
                                    BinaryOperator<V> accumulatorFunction) {
        V prev, next;
        do {
            prev = get();
            next = accumulatorFunction.apply(prev, x);
        } while (!compareAndSet(prev, next));
        return prev;
    }

    /**
     * 使用给定函数对当前值和给定值进行操作，原子地更新当前值，并返回更新后的值。
     * 函数应无副作用，因为在由于线程间竞争导致更新尝试失败时可能会重新应用该函数。
     * 函数的第一个参数是当前值，第二个参数是给定的更新值。
     *
     * @param x 更新值
     * @param accumulatorFunction 一个无副作用的双参数函数
     * @return 更新后的值
     * @since 1.8
     */
    public final V accumulateAndGet(V x,
                                    BinaryOperator<V> accumulatorFunction) {
        V prev, next;
        do {
            prev = get();
            next = accumulatorFunction.apply(prev, x);
        } while (!compareAndSet(prev, next));
        return next;
    }

    /**
     * 返回当前值的字符串表示形式。
     * @return 当前值的字符串表示形式
     */
    public String toString() {
        return String.valueOf(get());
    }

}

