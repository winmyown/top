package org.top.java.concurrent.source.atomic;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 上午11:27
 */

import sun.misc.Unsafe;

import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

/**
 * 一个可以原子更新的 {@code long} 值。有关原子变量的属性说明，请参见
 * {@link java.util.concurrent.atomic} 包的规范。{@code AtomicLong} 用于
 * 诸如原子递增的序列号等应用中，但不能作为 {@link java.lang.Long} 的替代品。
 * 然而，该类继承了 {@code Number}，以便工具和实用程序可以统一访问
 * 基于数值的类。
 *
 * @since 1.5
 * @author Doug Lea
 */
public class AtomicLong extends Number implements java.io.Serializable {
    private static final long serialVersionUID = 1927816293512124184L;

    // 设置使用 Unsafe.compareAndSwapLong 进行更新
    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long valueOffset;

    /**
     * 记录底层 JVM 是否支持无锁的 long 类型的 compareAndSwap。
     * 尽管 Unsafe.compareAndSwapLong 方法在任意情况下都有效，
     * 但某些构造应在 Java 层处理，以避免锁定用户可见的锁。
     */
    static final boolean VM_SUPPORTS_LONG_CAS = VMSupportsCS8();

    /**
     * 返回底层 JVM 是否支持无锁的 long 类型的 CompareAndSet。
     * 该方法仅被调用一次，并缓存在 VM_SUPPORTS_LONG_CAS 中。
     */
    private static native boolean VMSupportsCS8();

    static {
        try {
            valueOffset = unsafe.objectFieldOffset
                    (AtomicLong.class.getDeclaredField("value"));
        } catch (Exception ex) { throw new Error(ex); }
    }

    private volatile long value;

    /**
     * 使用给定的初始值创建一个新的 AtomicLong。
     *
     * @param initialValue 初始值
     */
    public AtomicLong(long initialValue) {
        value = initialValue;
    }

    /**
     * 使用初始值 {@code 0} 创建一个新的 AtomicLong。
     */
    public AtomicLong() {
    }

    /**
     * 获取当前值。
     *
     * @return 当前值
     */
    public final long get() {
        return value;
    }

    /**
     * 设置为给定值。
     *
     * @param newValue 新值
     */
    public final void set(long newValue) {
        value = newValue;
    }

    /**
     * 最终设置为给定值。
     *
     * @param newValue 新值
     * @since 1.6
     */
    public final void lazySet(long newValue) {
        unsafe.putOrderedLong(this, valueOffset, newValue);
    }

    /**
     * 原子地设置为给定值，并返回旧值。
     *
     * @param newValue 新值
     * @return 先前的值
     */
    public final long getAndSet(long newValue) {
        return unsafe.getAndSetLong(this, valueOffset, newValue);
    }

    /**
     * 如果当前值等于预期值，则原子地将值设置为给定的更新值。
     *
     * @param expect 预期的值
     * @param update 新值
     * @return 如果成功则返回 {@code true}。返回 {@code false} 表示实际值不等于预期值。
     */
    public final boolean compareAndSet(long expect, long update) {
        return unsafe.compareAndSwapLong(this, valueOffset, expect, update);
    }

    /**
     * 如果当前值等于预期值，则原子地将值设置为给定的更新值。
     *
     * <p><a href="package-summary.html#weakCompareAndSet">可能会伪造失败，且不提供顺序保证</a>，
     * 因此，通常 {@code compareAndSet} 更合适。
     *
     * @param expect 预期的值
     * @param update 新值
     * @return 如果成功则返回 {@code true}
     */
    public final boolean weakCompareAndSet(long expect, long update) {
        return unsafe.compareAndSwapLong(this, valueOffset, expect, update);
    }

    /**
     * 原子地将当前值加 1。
     *
     * @return 先前的值
     */
    public final long getAndIncrement() {
        return unsafe.getAndAddLong(this, valueOffset, 1L);
    }

    /**
     * 原子地将当前值减 1。
     *
     * @return 先前的值
     */
    public final long getAndDecrement() {
        return unsafe.getAndAddLong(this, valueOffset, -1L);
    }

    /**
     * 原子地将给定值加到当前值上。
     *
     * @param delta 要添加的值
     * @return 先前的值
     */
    public final long getAndAdd(long delta) {
        return unsafe.getAndAddLong(this, valueOffset, delta);
    }

    /**
     * 原子地将当前值加 1。
     *
     * @return 更新后的值
     */
    public final long incrementAndGet() {
        return unsafe.getAndAddLong(this, valueOffset, 1L) + 1L;
    }

    /**
     * 原子地将当前值减 1。
     *
     * @return 更新后的值
     */
    public final long decrementAndGet() {
        return unsafe.getAndAddLong(this, valueOffset, -1L) - 1L;
    }

    /**
     * 原子地将给定值加到当前值上。
     *
     * @param delta 要添加的值
     * @return 更新后的值
     */
    public final long addAndGet(long delta) {
        return unsafe.getAndAddLong(this, valueOffset, delta) + delta;
    }

    /**
     * 使用给定的函数原子地更新当前值，并返回先前的值。
     * 由于该函数可能会在尝试更新失败时重新应用，因此该函数应无副作用。
     *
     * @param updateFunction 无副作用的函数
     * @return 先前的值
     * @since 1.8
     */
    public final long getAndUpdate(LongUnaryOperator updateFunction) {
        long prev, next;
        do {
            prev = get();
            next = updateFunction.applyAsLong(prev);
        } while (!compareAndSet(prev, next));
        return prev;
    }

    /**
     * 使用给定的函数原子地更新当前值，并返回更新后的值。
     * 由于该函数可能会在尝试更新失败时重新应用，因此该函数应无副作用。
     *
     * @param updateFunction 无副作用的函数
     * @return 更新后的值
     * @since 1.8
     */
    public final long updateAndGet(LongUnaryOperator updateFunction) {
        long prev, next;
        do {
            prev = get();
            next = updateFunction.applyAsLong(prev);
        } while (!compareAndSet(prev, next));
        return next;
    }

    /**
     * 使用给定的函数将当前值与给定值原子地组合，并返回先前的值。
     * 由于该函数可能会在尝试更新失败时重新应用，因此该函数应无副作用。
     * 该函数的第一个参数是当前值，第二个参数是给定的更新值。
     *
     * @param x 更新值
     * @param accumulatorFunction 无副作用的两个参数函数
     * @return 先前的值
     * @since 1.8
     */
    public final long getAndAccumulate(long x, LongBinaryOperator accumulatorFunction) {
        long prev, next;
        do {
            prev = get();
            next = accumulatorFunction.applyAsLong(prev, x);
        } while (!compareAndSet(prev, next));
        return prev;
    }

    /**
     * 使用给定的函数将当前值与给定值原子地组合，并返回更新后的值。
     * 由于该函数可能会在尝试更新失败时重新应用，因此该函数应无副作用。
     * 该函数的第一个参数是当前值，第二个参数是给定的更新值。
     *
     * @param x 更新值
     * @param accumulatorFunction 无副作用的两个参数函数
     * @return 更新后的值
     * @since 1.8
     */
    public final long accumulateAndGet(long x, LongBinaryOperator accumulatorFunction) {
        long prev, next;
        do {
            prev = get();
            next = accumulatorFunction.applyAsLong(prev, x);
        } while (!compareAndSet(prev, next));
        return next;
    }

    /**
     * 返回当前值的字符串表示形式。
     * @return 当前值的字符串表示
     */
    public String toString() {
        return Long.toString(get());
    }

    /**
     * 将此 {@code AtomicLong} 的值返回为 {@code int}，经过缩小的基本类型转换。
     * @jls 5.1.3 缩小的基本类型转换
     */
    public int intValue() {
        return (int)get();
    }

    /**
     * 将此 {@code AtomicLong} 的值返回为 {@code long}。
     */
    public long longValue() {
        return get();
    }

    /**
     * 将此 {@code AtomicLong} 的值返回为 {@code float}，经过扩大的基本类型转换。
     * @jls 5.1.2 扩大的基本类型转换
     */
    public float floatValue() {
        return (float)get();
    }

    /**
     * 将此 {@code AtomicLong} 的值返回为 {@code double}，经过扩大的基本类型转换。
     * @jls 5.1.2 扩大的基本类型转换
     */
    public double doubleValue() {
        return (double)get();
    }

}

