package org.top.java.source.concurrent.atomic;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 上午11:25
 */


import org.top.java.source.sun.misc.Unsafe;

import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

/**
 * 一个可能被原子更新的 {@code int} 值。请参阅
 * {@link java.util.concurrent.atomic} 包规范，了解有关原子变量属性的描述。
 * {@code AtomicInteger} 用于诸如原子递增计数器的应用程序中，
 * 不能作为 {@link java.lang.Integer} 的替代品。然而，该类继承了
 * {@code Number}，以便工具和处理基于数值的类的实用程序能够统一访问。
 *
 * @since 1.5
 * 作者：Doug Lea
 */
public class AtomicInteger extends Number implements java.io.Serializable {
    private static final long serialVersionUID = 6214790243416807050L;

    // 设置使用 Unsafe.compareAndSwapInt 进行更新
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long valueOffset;

    static {
        try {
            valueOffset = unsafe.objectFieldOffset
                    (AtomicInteger.class.getDeclaredField("value"));
        } catch (Exception ex) { throw new Error(ex); }
    }

    private volatile int value;

    /**
     * 使用给定的初始值创建一个新的 AtomicInteger。
     *
     * @param initialValue 初始值
     */
    public AtomicInteger(int initialValue) {
        value = initialValue;
    }

    /**
     * 创建一个初始值为 {@code 0} 的新 AtomicInteger。
     */
    public AtomicInteger() {
    }

    /**
     * 获取当前值。
     *
     * @return 当前值
     */
    public final int get() {
        return value;
    }

    /**
     * 设置为给定的值。
     *
     * @param newValue 新值
     */
    public final void set(int newValue) {
        value = newValue;
    }

    /**
     * 最终设置为给定值。
     *
     * @param newValue 新值
     * @since 1.6
     */
    public final void lazySet(int newValue) {
        unsafe.putOrderedInt(this, valueOffset, newValue);
    }

    /**
     * 原子地设置为给定值并返回旧值。
     *
     * @param newValue 新值
     * @return 先前的值
     */
    public final int getAndSet(int newValue) {
        return unsafe.getAndSetInt(this, valueOffset, newValue);
    }

    /**
     * 如果当前值 {@code ==} 预期值，则原子地将值设置为给定的更新值。
     *
     * @param expect 预期值
     * @param update 新值
     * @return {@code true} 如果成功。返回 false 表示实际值与预期值不相等。
     */
    public final boolean compareAndSet(int expect, int update) {
        return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
    }

    /**
     * 如果当前值 {@code ==} 预期值，则原子地将值设置为给定的更新值。
     *
     * <p><a href="package-summary.html#weakCompareAndSet">可能偶发失败，
     * 并且不提供排序保证</a>，因此很少作为 {@code compareAndSet} 的替代方法。
     *
     * @param expect 预期值
     * @param update 新值
     * @return {@code true} 如果成功
     */
    public final boolean weakCompareAndSet(int expect, int update) {
        return unsafe.compareAndSwapInt(this, valueOffset, expect, update);
    }

    /**
     * 原子地将当前值加 1。
     *
     * @return 先前的值
     */
    public final int getAndIncrement() {
        return unsafe.getAndAddInt(this, valueOffset, 1);
    }

    /**
     * 原子地将当前值减 1。
     *
     * @return 先前的值
     */
    public final int getAndDecrement() {
        return unsafe.getAndAddInt(this, valueOffset, -1);
    }

    /**
     * 原子地将给定值加到当前值。
     *
     * @param delta 要添加的值
     * @return 先前的值
     */
    public final int getAndAdd(int delta) {
        return unsafe.getAndAddInt(this, valueOffset, delta);
    }

    /**
     * 原子地将当前值加 1。
     *
     * @return 更新后的值
     */
    public final int incrementAndGet() {
        return unsafe.getAndAddInt(this, valueOffset, 1) + 1;
    }

    /**
     * 原子地将当前值减 1。
     *
     * @return 更新后的值
     */
    public final int decrementAndGet() {
        return unsafe.getAndAddInt(this, valueOffset, -1) - 1;
    }

    /**
     * 原子地将给定值加到当前值。
     *
     * @param delta 要添加的值
     * @return 更新后的值
     */
    public final int addAndGet(int delta) {
        return unsafe.getAndAddInt(this, valueOffset, delta) + delta;
    }

    /**
     * 使用给定函数原子地更新当前值，并返回先前的值。函数应无副作用，
     * 因为当由于线程竞争导致尝试更新失败时，可能会重新应用该函数。
     *
     * @param updateFunction 一个无副作用的函数
     * @return 先前的值
     * @since 1.8
     */
    public final int getAndUpdate(IntUnaryOperator updateFunction) {
        int prev, next;
        do {
            prev = get();
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSet(prev, next));
        return prev;
    }

    /**
     * 使用给定函数原子地更新当前值，并返回更新后的值。函数应无副作用，
     * 因为当由于线程竞争导致尝试更新失败时，可能会重新应用该函数。
     *
     * @param updateFunction 一个无副作用的函数
     * @return 更新后的值
     * @since 1.8
     */
    public final int updateAndGet(IntUnaryOperator updateFunction) {
        int prev, next;
        do {
            prev = get();
            next = updateFunction.applyAsInt(prev);
        } while (!compareAndSet(prev, next));
        return next;
    }

    /**
     * 使用给定函数对当前值和给定值进行操作，原子地更新当前值，并返回先前的值。
     * 函数应无副作用，因为当由于线程竞争导致尝试更新失败时，可能会重新应用该函数。
     * 函数的第一个参数是当前值，第二个参数是给定的更新值。
     *
     * @param x 更新值
     * @param accumulatorFunction 一个无副作用的双参数函数
     * @return 先前的值
     * @since 1.8
     */
    public final int getAndAccumulate(int x, IntBinaryOperator accumulatorFunction) {
        int prev, next;
        do {
            prev = get();
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSet(prev, next));
        return prev;
    }

    /**
     * 使用给定函数对当前值和给定值进行操作，原子地更新当前值，并返回更新后的值。
     * 函数应无副作用，因为当由于线程竞争导致尝试更新失败时，可能会重新应用该函数。
     * 函数的第一个参数是当前值，第二个参数是给定的更新值。
     *
     * @param x 更新值
     * @param accumulatorFunction 一个无副作用的双参数函数
     * @return 更新后的值
     * @since 1.8
     */
    public final int accumulateAndGet(int x, IntBinaryOperator accumulatorFunction) {
        int prev, next;
        do {
            prev = get();
            next = accumulatorFunction.applyAsInt(prev, x);
        } while (!compareAndSet(prev, next));
        return next;
    }

    /**
     * 返回当前值的字符串表示形式。
     * @return 当前值的字符串表示形式
     */
    public String toString() {
        return Integer.toString(get());
    }

    /**
     * 以 {@code int} 形式返回此 {@code AtomicInteger} 的值。
     */
    public int intValue() {
        return get();
    }

    /**
     * 在扩展基本类型转换之后，以 {@code long} 形式返回此 {@code AtomicInteger} 的值。
     * @jls 5.1.2 扩展基本类型转换
     */
    public long longValue() {
        return (long)get();
    }

    /**
     * 在扩展基本类型转换之后，以 {@code float} 形式返回此 {@code AtomicInteger} 的值。
     * @jls 5.1.2 扩展基本类型转换
     */
    public float floatValue() {
        return (float)get();
    }

    /**
     * 在扩展基本类型转换之后，以 {@code double} 形式返回此 {@code AtomicInteger} 的值。
     * @jls 5.1.2 扩展基本类型转换
     */
    public double doubleValue() {
        return (double)get();
    }
}
