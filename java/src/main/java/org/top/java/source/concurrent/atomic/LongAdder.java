package org.top.java.source.concurrent.atomic;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 上午11:27
 */

import java.io.Serializable;

/**
 * 一个或多个变量共同维护一个初始为零的 {@code long} 累加和。当跨线程的更新
 * （方法 {@link #add}）产生竞争时，变量集可能会动态增长以减少竞争。
 * 方法 {@link #sum}（或者等效的 {@link #longValue}）返回所有变量中当前的
 * 总和。
 *
 * <p>当多个线程更新一个用于收集统计信息等用途的公共累加和时，此类通常优于
 * {@link AtomicLong}，而不是用于细粒度同步控制。在低竞争的更新场景下，这两类
 * 具有相似的特性。但在高竞争的情况下，该类的预期吞吐量显著更高，代价是
 * 较高的空间消耗。
 *
 * <p>LongAdder 可与 {@link java.util.concurrent.ConcurrentHashMap} 一起使用，
 * 维护一个可扩展的频率映射（类似直方图或多重集）。例如，要向
 * {@code ConcurrentHashMap<String,LongAdder> freqs} 中添加计数，若不存在则
 * 初始化，您可以使用 {@code freqs.computeIfAbsent(k -> new LongAdder()).increment();}
 *
 * <p>此类扩展 {@link Number}，但不定义诸如 {@code equals}、{@code hashCode}
 * 和 {@code compareTo} 等方法，因为实例是可变的，因此不适合用作集合的键。
 *
 * @since 1.8
 * 作者：Doug Lea
 */
public class LongAdder extends Striped64 implements Serializable {
    private static final long serialVersionUID = 7249069246863182397L;

    /**
     * 创建一个初始值为零的新累加器。
     */
    public LongAdder() {
    }

    /**
     * 添加给定的值。
     *
     * @param x 要添加的值
     */
    public void add(long x) {
        Cell[] as; long b, v; int m; Cell a;
        if ((as = cells) != null || !casBase(b = base, b + x)) {
            boolean uncontended = true;
            if (as == null || (m = as.length - 1) < 0 ||
                    (a = as[getProbe() & m]) == null ||
                    !(uncontended = a.cas(v = a.value, v + x)))
                longAccumulate(x, null, uncontended);
        }
    }

    /**
     * 等效于 {@code add(1)}。
     */
    public void increment() {
        add(1L);
    }

    /**
     * 等效于 {@code add(-1)}。
     */
    public void decrement() {
        add(-1L);
    }

    /**
     * 返回当前的累加和。返回的值不是原子快照；如果没有并发更新，则返回的
     * 结果是准确的，但在计算累加和时发生的并发更新可能不会被包括在内。
     *
     * @return 累加和
     */
    public long sum() {
        Cell[] as = cells; Cell a;
        long sum = base;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    sum += a.value;
            }
        }
        return sum;
    }

    /**
     * 将维护累加和的变量重置为零。此方法可能是创建新累加器的有用替代方案，
     * 但仅在没有并发更新的情况下有效。由于此方法本质上是竞争的，因此只有
     * 在已知没有线程同时更新时才应使用。
     */
    public void reset() {
        Cell[] as = cells; Cell a;
        base = 0L;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    a.value = 0L;
            }
        }
    }

    /**
     * 等效于 {@link #sum} 然后是 {@link #reset}。例如，在多线程计算的
     * 静止点期间可以应用此方法。如果此方法与更新同时发生，则返回的值
     * 不保证是重置前的最终值。
     *
     * @return 累加和
     */
    public long sumThenReset() {
        Cell[] as = cells; Cell a;
        long sum = base;
        base = 0L;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null) {
                    sum += a.value;
                    a.value = 0L;
                }
            }
        }
        return sum;
    }

    /**
     * 返回 {@link #sum} 的字符串表示形式。
     * @return {@link #sum} 的字符串表示形式
     */
    public String toString() {
        return Long.toString(sum());
    }

    /**
     * 等效于 {@link #sum}。
     *
     * @return 累加和
     */
    public long longValue() {
        return sum();
    }

    /**
     * 在缩小基本类型转换之后，以 {@code int} 形式返回 {@link #sum}。
     */
    public int intValue() {
        return (int)sum();
    }

    /**
     * 在扩展基本类型转换之后，以 {@code float} 形式返回 {@link #sum}。
     */
    public float floatValue() {
        return (float)sum();
    }

    /**
     * 在扩展基本类型转换之后，以 {@code double} 形式返回 {@link #sum}。
     */
    public double doubleValue() {
        return (double)sum();
    }

    /**
     * 序列化代理，用于避免在序列化形式中引用非公共的 Striped64 超类。
     * @serial include
     */
    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 7249069246863182397L;

        /**
         * sum() 返回的当前值。
         * @serial
         */
        private final long value;

        SerializationProxy(LongAdder a) {
            value = a.sum();
        }

        /**
         * 返回一个具有此代理所持有初始状态的 {@code LongAdder} 对象。
         *
         * @return 一个具有此代理所持有初始状态的 {@code LongAdder} 对象
         */
        private Object readResolve() {
            LongAdder a = new LongAdder();
            a.base = value;
            return a;
        }
    }

    /**
     * 返回表示此实例状态的
     * <a href="../../../../serialized-form.html#java.util.concurrent.atomic.LongAdder.SerializationProxy">
     * SerializationProxy</a>。
     *
     * @return 表示此实例状态的 {@link SerializationProxy}
     */
    private Object writeReplace() {
        return new SerializationProxy(this);
    }

    /**
     * @param s 输入流
     * @throws java.io.InvalidObjectException 始终抛出
     */
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.InvalidObjectException {
        throw new java.io.InvalidObjectException("需要代理");
    }

}

