package org.top.java.concurrent.source.atomic;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 上午10:56
 */
/**
 * {@code AtomicStampedReference} 维护一个对象引用
 * 以及一个整数"印章"，可以原子地进行更新。
 *
 * <p>实现说明：该实现通过创建表示"封装"
 * [引用，整数]对的内部对象来维护带有印章的引用。
 *
 * @since 1.5
 * 作者：Doug Lea
 * @param <V> 该引用所指对象的类型
 */
public class AtomicStampedReference<V> {

    private static class Pair<T> {
        final T reference;
        final int stamp;
        private Pair(T reference, int stamp) {
            this.reference = reference;
            this.stamp = stamp;
        }
        static <T> Pair<T> of(T reference, int stamp) {
            return new Pair<T>(reference, stamp);
        }
    }

    private volatile Pair<V> pair;

    /**
     * 使用给定的初始值创建一个新的{@code AtomicStampedReference}。
     *
     * @param initialRef 初始引用
     * @param initialStamp 初始印章
     */
    public AtomicStampedReference(V initialRef, int initialStamp) {
        pair = Pair.of(initialRef, initialStamp);
    }

    /**
     * 返回当前引用的值。
     *
     * @return 当前引用的值
     */
    public V getReference() {
        return pair.reference;
    }

    /**
     * 返回当前印章的值。
     *
     * @return 当前印章的值
     */
    public int getStamp() {
        return pair.stamp;
    }

    /**
     * 返回引用和印章的当前值。
     * 典型用法是 {@code int[1] holder; ref = v.get(holder); }。
     *
     * @param stampHolder 长度至少为一的数组。返回时，{@code stampholder[0]}
     * 将保存印章的值。
     * @return 当前引用的值
     */
    public V get(int[] stampHolder) {
        Pair<V> pair = this.pair;
        stampHolder[0] = pair.stamp;
        return pair.reference;
    }

    /**
     * 如果当前引用是{@code ==} 预期引用且当前印章等于预期印章，
     * 则原子地将引用和印章的值设置为给定的更新值。
     *
     * <p><a href="package-summary.html#weakCompareAndSet">可能会偶发失败，并且不提供排序保证</a>，
     * 因此很少作为{@code compareAndSet}的替代方法。
     *
     * @param expectedReference 预期的引用值
     * @param newReference 新的引用值
     * @param expectedStamp 预期的印章值
     * @param newStamp 新的印章值
     * @return {@code true} 如果成功
     */
    public boolean weakCompareAndSet(V   expectedReference,
                                     V   newReference,
                                     int expectedStamp,
                                     int newStamp) {
        return compareAndSet(expectedReference, newReference,
                expectedStamp, newStamp);
    }

    /**
     * 如果当前引用是{@code ==} 预期引用且当前印章等于预期印章，
     * 则原子地将引用和印章的值设置为给定的更新值。
     *
     * @param expectedReference 预期的引用值
     * @param newReference 新的引用值
     * @param expectedStamp 预期的印章值
     * @param newStamp 新的印章值
     * @return {@code true} 如果成功
     */
    public boolean compareAndSet(V   expectedReference,
                                 V   newReference,
                                 int expectedStamp,
                                 int newStamp) {
        Pair<V> current = pair;
        return
                expectedReference == current.reference &&
                        expectedStamp == current.stamp &&
                        ((newReference == current.reference &&
                                newStamp == current.stamp) ||
                                casPair(current, Pair.of(newReference, newStamp)));
    }

    /**
     * 无条件地设置引用和印章的值。
     *
     * @param newReference 新的引用值
     * @param newStamp 新的印章值
     */
    public void set(V newReference, int newStamp) {
        Pair<V> current = pair;
        if (newReference != current.reference || newStamp != current.stamp)
            this.pair = Pair.of(newReference, newStamp);
    }

    /**
     * 如果当前引用是{@code ==} 预期引用，则原子地将印章的值
     * 设置为给定的更新值。任何给定的操作可能会偶发失败（返回{@code false}），
     * 但当当前值保持预期值且没有其他线程也尝试设置该值时，
     * 重复调用最终会成功。
     *
     * @param expectedReference 预期的引用值
     * @param newStamp 新的印章值
     * @return {@code true} 如果成功
     */
    public boolean attemptStamp(V expectedReference, int newStamp) {
        Pair<V> current = pair;
        return
                expectedReference == current.reference &&
                        (newStamp == current.stamp ||
                                casPair(current, Pair.of(expectedReference, newStamp)));
    }

    // Unsafe mechanics

    private static final sun.misc.Unsafe UNSAFE = sun.misc.Unsafe.getUnsafe();
    private static final long pairOffset =
            objectFieldOffset(UNSAFE, "pair", AtomicStampedReference.class);

    private boolean casPair(Pair<V> cmp, Pair<V> val) {
        return UNSAFE.compareAndSwapObject(this, pairOffset, cmp, val);
    }

    static long objectFieldOffset(sun.misc.Unsafe UNSAFE,
                                  String field, Class<?> klazz) {
        try {
            return UNSAFE.objectFieldOffset(klazz.getDeclaredField(field));
        } catch (NoSuchFieldException e) {
            // 将异常转换为相应的错误
            NoSuchFieldError error = new NoSuchFieldError(field);
            error.initCause(e);
            throw error;
        }
    }
}

