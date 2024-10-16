package org.top.java.concurrent.source;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 上午10:44
 */

import java.io.ObjectStreamField;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

/**
 * 一个与当前线程隔离的随机数生成器。类似于 {@link java.lang.Math} 类使用的全局 {@link java.util.Random} 生成器，
 * {@code ThreadLocalRandom} 是通过内部生成的种子初始化的，该种子不能被修改。
 * 在并发程序中，当适用时，使用 {@code ThreadLocalRandom} 而不是共享的 {@code Random} 对象，通常会遇到更少的开销和争用。
 * 当多个任务（例如每个 {@link ForkJoinTask}）在线程池中并行使用随机数时，使用 {@code ThreadLocalRandom} 特别合适。
 *
 * <p>通常应以以下形式使用此类：
 * {@code ThreadLocalRandom.current().nextX(...)}（其中 {@code X} 是 {@code Int}、{@code Long} 等）。
 * 当所有使用都采用这种形式时，不可能在多个线程之间意外共享 {@code ThreadLocalRandom}。
 *
 * <p>此类还提供了一些常用的有界随机生成方法。
 *
 * <p>{@code ThreadLocalRandom} 的实例不具有加密安全性。相反，在安全敏感的应用程序中可以考虑使用 {@link java.security.SecureRandom}。
 * 此外，默认构造的实例不会使用加密随机种子，除非通过 {@linkplain System#getProperty 系统属性} 设置 {@code java.util.secureRandomSeed} 为 {@code true}。
 *
 * @since 1.7
 * @author Doug Lea
 */
public class ThreadLocalRandom extends Random {
    /*
     * 该类实现了 java.util.Random API（并继承自 Random），使用了一个静态实例来访问存储在 Thread 类中的随机数状态（主要是字段 threadLocalRandomSeed）。
     * 在这样做的同时，它还提供了一个管理包私有实用程序的场所，这些实用程序依赖于与维护 ThreadLocalRandom 实例所需的状态完全相同的状态。
     * 我们利用初始化标志字段的需求，还将其用作 "probe"——一个自我调整的线程哈希，用于避免争用，以及一个保守使用的简单（xorShift）随机种子，
     * 以避免通过劫持 ThreadLocalRandom 序列给用户带来意外。这种双重用途是便利性的结合，但也是一种简单有效的方式，减少了大多数并发程序的应用层开销和占用。
     *
     * 尽管该类继承自 java.util.Random，但它使用的基本算法与 java.util.SplittableRandom 相同。（有关说明，请参见其内部文档，这里不再重复。）
     * 因为 ThreadLocalRandom 不是可分割的，所以我们仅使用一个 64 位的伽马值。
     *
     * 由于该类与 Thread 类位于不同的包中，因此字段访问方法使用 Unsafe 来绕过访问控制规则。
     * 为了符合 Random 超类构造函数的要求，公共静态 ThreadLocalRandom 维护了一个 "initialized" 字段，用于拒绝用户对 setSeed 的调用，同时仍然允许构造函数的调用。
     * 请注意，序列化是完全不必要的，因为它只有一个静态单例。但是我们生成了一个包含 "rnd" 和 "initialized" 字段的序列表单，以确保跨版本的兼容性。
     *
     * 非核心方法的实现与 SplittableRandom 中的大致相同，它们部分源自此类的早期版本。
     *
     * nextLocalGaussian ThreadLocal 支持很少使用的 nextGaussian 方法，通过为其中一对提供一个持有者。
     * 与该方法的基类版本一样，这种时间/空间权衡可能从未真正有价值，但我们提供了相同的统计属性。
     */

    /** 生成每个线程初始化/探测字段 */
    private static final AtomicInteger probeGenerator =
            new AtomicInteger();

    /**
     * 默认构造函数的下一个种子。
     */
    private static final AtomicLong seeder = new AtomicLong(initialSeed());

    private static long initialSeed() {
        String pp = java.security.AccessController.doPrivileged(
                new sun.security.action.GetPropertyAction(
                        "java.util.secureRandomSeed"));
        if (pp != null && pp.equalsIgnoreCase("true")) {
            byte[] seedBytes = java.security.SecureRandom.getSeed(8);
            long s = (long)(seedBytes[0]) & 0xffL;
            for (int i = 1; i < 8; ++i)
                s = (s << 8) | ((long)(seedBytes[i]) & 0xffL);
            return s;
        }
        return (mix64(System.currentTimeMillis()) ^
                mix64(System.nanoTime()));
    }

    /**
     * 种子增量
     */
    private static final long GAMMA = 0x9e3779b97f4a7c15L;

    /**
     * 用于生成探测值的增量
     */
    private static final int PROBE_INCREMENT = 0x9e3779b9;

    /**
     * 每个新实例的种子增量
     */
    private static final long SEEDER_INCREMENT = 0xbb67ae8584caa73bL;

    // 从 SplittableRandom 获取的常量
    private static final double DOUBLE_UNIT = 0x1.0p-53;  // 1.0  / (1L << 53)
    private static final float  FLOAT_UNIT  = 0x1.0p-24f; // 1.0f / (1 << 24)

    /** 很少使用的保存一对高斯分布数的持有者 */
    private static final ThreadLocal<Double> nextLocalGaussian =
            new ThreadLocal<Double>();

    private static long mix64(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }

    private static int mix32(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        return (int)(((z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L) >>> 32);
    }

    /**
     * 仅在单例初始化期间使用的字段。
     * 构造函数完成时为 true。
     */
    boolean initialized;

    /** 仅用于静态单例的构造函数 */
    private ThreadLocalRandom() {
        initialized = true; // 在 super() 调用期间为 false
    }

    /** 公共的 ThreadLocalRandom 实例 */
    static final ThreadLocalRandom instance = new ThreadLocalRandom();

    /**
     * 为当前线程初始化 Thread 字段。仅当 Thread.threadLocalRandomProbe 为零时调用，表明需要生成线程本地种子值。
     * 注意，即使初始化是纯粹的线程本地，我们也需要依赖（静态的）原子生成器来初始化这些值。
     */
    static final void localInit() {
        int p = probeGenerator.addAndGet(PROBE_INCREMENT);
        int probe = (p == 0) ? 1 : p; // 跳过 0
        long seed = mix64(seeder.getAndAdd(SEEDER_INCREMENT));
        Thread t = Thread.currentThread();
        UNSAFE.putLong(t, SEED, seed);
        UNSAFE.putInt(t, PROBE, probe);
    }

    /**
     * 返回当前线程的 {@code ThreadLocalRandom} 实例。
     *
     * @return 当前线程的 {@code ThreadLocalRandom} 实例
     */
    public static ThreadLocalRandom current() {
        if (UNSAFE.getInt(Thread.currentThread(), PROBE) == 0)
            localInit();
        return instance;
    }

    /**
     * 抛出 {@code UnsupportedOperationException}。不支持在此生成器中设置种子。
     *
     * @throws UnsupportedOperationException 始终抛出
     */
    public void setSeed(long seed) {
        // 仅允许来自 super() 构造函数的调用
        if (initialized)
            throw new UnsupportedOperationException();
    }

    final long nextSeed() {
        Thread t; long r; // 读取并更新每线程的种子
        UNSAFE.putLong(t = Thread.currentThread(), SEED,
                r = UNSAFE.getLong(t, SEED) + GAMMA);
        return r;
    }

    // 我们必须定义这个，但从不使用它。
    protected int next(int bits) {
        return (int)(mix64(nextSeed()) >>> (64 - bits));
    }

    // IllegalArgumentException 消息
    static final String BadBound = "bound 必须为正数";
    static final String BadRange = "bound 必须大于 origin";
    static final String BadSize  = "size 必须为非负数";

    /**
     * 用于 LongStream Spliterator 的 nextLong 形式。如果 origin 大于 bound，则作为未绑定的 nextLong 形式，否则作为有界形式。
     *
     * @param origin 最小值，除非大于 bound
     * @param bound 上限（排他性），不能等于 origin
     * @return 伪随机值
     */
    final long internalNextLong(long origin, long bound) {
        long r = mix64(nextSeed());
        if (origin < bound) {
            long n = bound - origin, m = n - 1;
            if ((n & m) == 0L)  // 2 的幂
                r = (r & m) + origin;
            else if (n > 0L) {  // 拒绝过度表示的候选者
                for (long u = r >>> 1;            // 确保非负
                     u + m - (r = u % n) < 0L;    // 拒绝检查
                     u = mix64(nextSeed()) >>> 1) // 重试
                    ;
                r += origin;
            }
            else {              // 范围不可表示为 long
                while (r < origin || r >= bound)
                    r = mix64(nextSeed());
            }
        }
        return r;
    }

    /**
     * 用于 IntStream Spliterator 的 nextInt 形式。
     * 与 long 版本完全相同，除了类型。
     *
     * @param origin 最小值，除非大于 bound
     * @param bound 上限（排他性），不能等于 origin
     * @return 伪随机值
     */
    final int internalNextInt(int origin, int bound) {
        int r = mix32(nextSeed());
        if (origin < bound) {
            int n = bound - origin, m = n - 1;
            if ((n & m) == 0)
                r = (r & m) + origin;
            else if (n > 0) {
                for (int u = r >>> 1;
                     u + m - (r = u % n) < 0;
                     u = mix32(nextSeed()) >>> 1)
                    ;
                r += origin;
            }
            else {
                while (r < origin || r >= bound)
                    r = mix32(nextSeed());
            }
        }
        return r;
    }

    /**
     * 用于 DoubleStream Spliterator 的 nextDouble 形式。
     *
     * @param origin 最小值，除非大于 bound
     * @param bound 上限（排他性），不能等于 origin
     * @return 伪随机值
     */
    final double internalNextDouble(double origin, double bound) {
        double r = (nextLong() >>> 11) * DOUBLE_UNIT;
        if (origin < bound) {
            r = r * (bound - origin) + origin;
            if (r >= bound) // 修正舍入误差
                r = Double.longBitsToDouble(Double.doubleToLongBits(bound) - 1);
        }
        return r;
    }

    /**
     * 返回一个伪随机的 {@code int} 值。
     *
     * @return 伪随机的 {@code int} 值
     */
    public int nextInt() {
        return mix32(nextSeed());
    }

    /**
     * 返回一个伪随机的 {@code int} 值，范围在 0（含）到指定上限（不含）之间。
     *
     * @param bound 上限（排他）。必须为正数。
     * @return 伪随机的 {@code int} 值，范围在 0（含）到上限（不含）之间
     * @throws IllegalArgumentException 如果 {@code bound} 为非正数
     */
    public int nextInt(int bound) {
        if (bound <= 0)
            throw new IllegalArgumentException(BadBound);
        int r = mix32(nextSeed());
        int m = bound - 1;
        if ((bound & m) == 0) // 2 的幂
            r &= m;
        else { // 拒绝过度表示的候选者
            for (int u = r >>> 1;
                 u + m - (r = u % bound) < 0;
                 u = mix32(nextSeed()) >>> 1)
                ;
        }
        return r;
    }

    /**
     * 返回一个伪随机的 {@code int} 值，范围在指定的起点（含）到上限（不含）之间。
     *
     * @param origin 最小值（含）
     * @param bound 上限（不含）
     * @return 伪随机的 {@code int} 值，范围在起点（含）和上限（不含）之间
     * @throws IllegalArgumentException 如果 {@code origin} 大于或等于 {@code bound}
     */
    public int nextInt(int origin, int bound) {
        if (origin >= bound)
            throw new IllegalArgumentException(BadRange);
        return internalNextInt(origin, bound);
    }

    /**
     * 返回一个伪随机的 {@code long} 值。
     *
     * @return 伪随机的 {@code long} 值
     */
    public long nextLong() {
        return mix64(nextSeed());
    }

    /**
     * 返回一个伪随机的 {@code long} 值，范围在 0（含）到指定上限（不含）之间。
     *
     * @param bound 上限（不含）。必须为正数。
     * @return 伪随机的 {@code long} 值，范围在 0（含）到上限（不含）之间
     * @throws IllegalArgumentException 如果 {@code bound} 为非正数
     */
    public long nextLong(long bound) {
        if (bound <= 0)
            throw new IllegalArgumentException(BadBound);
        long r = mix64(nextSeed());
        long m = bound - 1;
        if ((bound & m) == 0L) // 2 的幂
            r &= m;
        else { // 拒绝过度表示的候选者
            for (long u = r >>> 1;
                 u + m - (r = u % bound) < 0L;
                 u = mix64(nextSeed()) >>> 1)
                ;
        }
        return r;
    }

    /**
     * 返回一个伪随机的 {@code long} 值，范围在指定的起点（含）到上限（不含）之间。
     *
     * @param origin 最小值（含）
     * @param bound 上限（不含）
     * @return 伪随机的 {@code long} 值，范围在起点（含）和上限（不含）之间
     * @throws IllegalArgumentException 如果 {@code origin} 大于或等于 {@code bound}
     */
    public long nextLong(long origin, long bound) {
        if (origin >= bound)
            throw new IllegalArgumentException(BadRange);
        return internalNextLong(origin, bound);
    }

    /**
     * 返回一个伪随机的 {@code double} 值，范围在 0.0（含）到 1.0（不含）之间。
     *
     * @return 伪随机的 {@code double} 值，范围在 0.0（含）到 1.0（不含）之间
     */
    public double nextDouble() {
        return (mix64(nextSeed()) >>> 11) * DOUBLE_UNIT;
    }

    /**
     * 返回一个伪随机的 {@code double} 值，范围在 0.0（含）到指定上限（不含）之间。
     *
     * @param bound 上限（不含）。必须为正数。
     * @return 伪随机的 {@code double} 值，范围在 0.0（含）到上限（不含）之间
     * @throws IllegalArgumentException 如果 {@code bound} 为非正数
     */
    public double nextDouble(double bound) {
        if (!(bound > 0.0))
            throw new IllegalArgumentException(BadBound);
        double result = (mix64(nextSeed()) >>> 11) * DOUBLE_UNIT * bound;
        return (result < bound) ?  result : // 修正舍入误差
                Double.longBitsToDouble(Double.doubleToLongBits(bound) - 1);
    }

    /**
     * 返回一个伪随机的 {@code double} 值，范围在指定的起点（含）到上限（不含）之间。
     *
     * @param origin 最小值（含）
     * @param bound 上限（不含）
     * @return 伪随机的 {@code double} 值，范围在起点（含）和上限（不含）之间
     * @throws IllegalArgumentException 如果 {@code origin} 大于或等于 {@code bound}
     */
    public double nextDouble(double origin, double bound) {
        if (!(origin < bound))
            throw new IllegalArgumentException(BadRange);
        return internalNextDouble(origin, bound);
    }

    /**
     * 返回一个伪随机的 {@code boolean} 值。
     *
     * @return 伪随机的 {@code boolean} 值
     */
    public boolean nextBoolean() {
        return mix32(nextSeed()) < 0;
    }

    /**
     * 返回一个伪随机的 {@code float} 值，范围在 0.0（含）到 1.0（不含）之间。
     *
     * @return 伪随机的 {@code float} 值，范围在 0.0（含）到 1.0（不含）之间
     */
    public float nextFloat() {
        return (mix32(nextSeed()) >>> 8) * FLOAT_UNIT;
    }

    /**
     * 返回一个伪随机的 {@code double} 高斯分布值。
     *
     * @return 伪随机的 {@code double} 高斯分布值
     */
    public double nextGaussian() {
        // 使用 nextLocalGaussian 而不是 nextGaussian 字段
        Double d = nextLocalGaussian.get();
        if (d != null) {
            nextLocalGaussian.set(null);
            return d.doubleValue();
        }
        double v1, v2, s;
        do {
            v1 = 2 * nextDouble() - 1; // 范围在 -1 和 1 之间
            v2 = 2 * nextDouble() - 1; // 范围在 -1 和 1 之间
            s = v1 * v1 + v2 * v2;
        } while (s >= 1 || s == 0);
        double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
        nextLocalGaussian.set(new Double(v2 * multiplier));
        return v1 * multiplier;
    }
    // 流方法，以一种旨在更好地隔离维护目的的方式编码，不同形式之间的差异很小。

    /**
     * 返回一个流，该流生成指定数量的伪随机 {@code int} 值。
     *
     * @param streamSize 生成的值的数量
     * @return 一个伪随机 {@code int} 值的流
     * @throws IllegalArgumentException 如果 {@code streamSize} 小于 0
     * @since 1.8
     */
    public IntStream ints(long streamSize) {
        if (streamSize < 0L)
            throw new IllegalArgumentException(BadSize);
        return StreamSupport.intStream
                (new RandomIntsSpliterator
                                (0L, streamSize, Integer.MAX_VALUE, 0),
                        false);
    }

    /**
     * 返回一个几乎无限的伪随机 {@code int} 值流。
     *
     * @implNote 该方法的实现等同于 {@code ints(Long.MAX_VALUE)}。
     *
     * @return 一个伪随机 {@code int} 值的流
     * @since 1.8
     */
    public IntStream ints() {
        return StreamSupport.intStream
                (new RandomIntsSpliterator
                                (0L, Long.MAX_VALUE, Integer.MAX_VALUE, 0),
                        false);
    }

    /**
     * 返回一个流，该流生成指定数量的伪随机 {@code int} 值，每个值都符合指定的起点（含）和上限（不含）。
     *
     * @param streamSize 生成的值的数量
     * @param randomNumberOrigin 每个随机值的起点（含）
     * @param randomNumberBound 每个随机值的上限（不含）
     * @return 一个伪随机 {@code int} 值的流，每个值符合给定的起点（含）和上限（不含）
     * @throws IllegalArgumentException 如果 {@code streamSize} 小于 0，或 {@code randomNumberOrigin} 大于或等于 {@code randomNumberBound}
     * @since 1.8
     */
    public IntStream ints(long streamSize, int randomNumberOrigin,
                          int randomNumberBound) {
        if (streamSize < 0L)
            throw new IllegalArgumentException(BadSize);
        if (randomNumberOrigin >= randomNumberBound)
            throw new IllegalArgumentException(BadRange);
        return StreamSupport.intStream
                (new RandomIntsSpliterator
                                (0L, streamSize, randomNumberOrigin, randomNumberBound),
                        false);
    }

    /**
     * 返回一个几乎无限的伪随机 {@code int} 值流，每个值都符合指定的起点（含）和上限（不含）。
     *
     * @implNote 该方法的实现等同于 {@code ints(Long.MAX_VALUE, randomNumberOrigin, randomNumberBound)}。
     *
     * @param randomNumberOrigin 每个随机值的起点（含）
     * @param randomNumberBound 每个随机值的上限（不含）
     * @return 一个伪随机 {@code int} 值的流，每个值符合给定的起点（含）和上限（不含）
     * @throws IllegalArgumentException 如果 {@code randomNumberOrigin} 大于或等于 {@code randomNumberBound}
     * @since 1.8
     */
    public IntStream ints(int randomNumberOrigin, int randomNumberBound) {
        if (randomNumberOrigin >= randomNumberBound)
            throw new IllegalArgumentException(BadRange);
        return StreamSupport.intStream
                (new RandomIntsSpliterator
                                (0L, Long.MAX_VALUE, randomNumberOrigin, randomNumberBound),
                        false);
    }

    /**
     * 返回一个流，该流生成指定数量的伪随机 {@code long} 值。
     *
     * @param streamSize 生成的值的数量
     * @return 一个伪随机 {@code long} 值的流
     * @throws IllegalArgumentException 如果 {@code streamSize} 小于 0
     * @since 1.8
     */
    public LongStream longs(long streamSize) {
        if (streamSize < 0L)
            throw new IllegalArgumentException(BadSize);
        return StreamSupport.longStream
                (new RandomLongsSpliterator
                                (0L, streamSize, Long.MAX_VALUE, 0L),
                        false);
    }

    /**
     * 返回一个几乎无限的伪随机 {@code long} 值流。
     *
     * @implNote 该方法的实现等同于 {@code longs(Long.MAX_VALUE)}。
     *
     * @return 一个伪随机 {@code long} 值的流
     * @since 1.8
     */
    public LongStream longs() {
        return StreamSupport.longStream
                (new RandomLongsSpliterator
                                (0L, Long.MAX_VALUE, Long.MAX_VALUE, 0L),
                        false);
    }

    /**
     * 返回一个流，该流生成指定数量的伪随机 {@code long} 值，每个值都符合指定的起点（含）和上限（不含）。
     *
     * @param streamSize 生成的值的数量
     * @param randomNumberOrigin 每个随机值的起点（含）
     * @param randomNumberBound 每个随机值的上限（不含）
     * @return 一个伪随机 {@code long} 值的流，每个值符合给定的起点（含）和上限（不含）
     * @throws IllegalArgumentException 如果 {@code streamSize} 小于 0，或 {@code randomNumberOrigin} 大于或等于 {@code randomNumberBound}
     * @since 1.8
     */
    public LongStream longs(long streamSize, long randomNumberOrigin,
                            long randomNumberBound) {
        if (streamSize < 0L)
            throw new IllegalArgumentException(BadSize);
        if (randomNumberOrigin >= randomNumberBound)
            throw new IllegalArgumentException(BadRange);
        return StreamSupport.longStream
                (new RandomLongsSpliterator
                                (0L, streamSize, randomNumberOrigin, randomNumberBound),
                        false);
    }

    /**
     * 返回一个几乎无限的伪随机 {@code long} 值流，每个值都符合指定的起点（含）和上限（不含）。
     *
     * @implNote 该方法的实现等同于 {@code longs(Long.MAX_VALUE, randomNumberOrigin, randomNumberBound)}。
     *
     * @param randomNumberOrigin 每个随机值的起点（含）
     * @param randomNumberBound 每个随机值的上限（不含）
     * @return 一个伪随机 {@code long} 值的流，每个值符合给定的起点（含）和上限（不含）
     * @throws IllegalArgumentException 如果 {@code randomNumberOrigin} 大于或等于 {@code randomNumberBound}
     * @since 1.8
     */
    public LongStream longs(long randomNumberOrigin, long randomNumberBound) {
        if (randomNumberOrigin >= randomNumberBound)
            throw new IllegalArgumentException(BadRange);
        return StreamSupport.longStream
                (new RandomLongsSpliterator
                                (0L, Long.MAX_VALUE, randomNumberOrigin, randomNumberBound),
                        false);
    }

    /**
     * 返回一个流，该流生成指定数量的伪随机 {@code double} 值，每个值都在 0.0（含）到 1.0（不含）之间。
     *
     * @param streamSize 生成的值的数量
     * @return 一个 {@code double} 值的流
     * @throws IllegalArgumentException 如果 {@code streamSize} 小于 0
     * @since 1.8
     */
    public DoubleStream doubles(long streamSize) {
        if (streamSize < 0L)
            throw new IllegalArgumentException(BadSize);
        return StreamSupport.doubleStream
                (new RandomDoublesSpliterator
                                (0L, streamSize, Double.MAX_VALUE, 0.0),
                        false);
    }

    /**
     * 返回一个几乎无限的伪随机 {@code double} 值流，每个值都在 0.0（含）到 1.0（不含）之间。
     *
     * @implNote 该方法的实现等同于 {@code doubles(Long.MAX_VALUE)}。
     *
     * @return 一个伪随机 {@code double} 值的流
     * @since 1.8
     */
    public DoubleStream doubles() {
        return StreamSupport.doubleStream
                (new RandomDoublesSpliterator
                                (0L, Long.MAX_VALUE, Double.MAX_VALUE, 0.0),
                        false);
    }

    /**
     * 返回一个流，该流生成指定数量的伪随机 {@code double} 值，每个值符合指定的起点（含）和上限（不含）。
     *
     * @param streamSize 生成的值的数量
     * @param randomNumberOrigin 每个随机值的起点（含）
     * @param randomNumberBound 每个随机值的上限（不含）
     * @return 一个伪随机 {@code double} 值的流，每个值符合给定的起点（含）和上限（不含）
     * @throws IllegalArgumentException 如果 {@code streamSize} 小于 0
     * @throws IllegalArgumentException 如果 {@code randomNumberOrigin} 大于或等于 {@code randomNumberBound}
     * @since 1.8
     */
    public DoubleStream doubles(long streamSize, double randomNumberOrigin,
                                double randomNumberBound) {
        if (streamSize < 0L)
            throw new IllegalArgumentException(BadSize);
        if (!(randomNumberOrigin < randomNumberBound))
            throw new IllegalArgumentException(BadRange);
        return StreamSupport.doubleStream
                (new RandomDoublesSpliterator
                                (0L, streamSize, randomNumberOrigin, randomNumberBound),
                        false);
    }

    /**
     * 返回一个几乎无限的伪随机 {@code double} 值流，每个值符合指定的起点（含）和上限（不含）。
     *
     * @implNote 该方法的实现等同于 {@code doubles(Long.MAX_VALUE, randomNumberOrigin, randomNumberBound)}。
     *
     * @param randomNumberOrigin 每个随机值的起点（含）
     * @param randomNumberBound 每个随机值的上限（不含）
     * @return 一个伪随机 {@code double} 值的流，每个值符合给定的起点（含）和上限（不含）
     * @throws IllegalArgumentException 如果 {@code randomNumberOrigin} 大于或等于 {@code randomNumberBound}
     * @since 1.8
     */
    public DoubleStream doubles(double randomNumberOrigin, double randomNumberBound) {
        if (!(randomNumberOrigin < randomNumberBound))
            throw new IllegalArgumentException(BadRange);
        return StreamSupport.doubleStream
                (new RandomDoublesSpliterator
                                (0L, Long.MAX_VALUE, randomNumberOrigin, randomNumberBound),
                        false);
    }

    /**
     * Spliterator for int streams. 我们通过将 bound 小于 origin 视为未绑定，
     * 并且通过将“无限”视为等同于 Long.MAX_VALUE 来将四个 int 版本合并为一个类。
     * 对于分割，它使用标准的二分法。
     * 该类的 long 和 double 版本除了类型之外完全相同。
     */
    static final class RandomIntsSpliterator implements Spliterator.OfInt {
        long index;
        final long fence;
        final int origin;
        final int bound;
        RandomIntsSpliterator(long index, long fence, int origin, int bound) {
            this.index = index;
            this.fence = fence;
            this.origin = origin;
            this.bound = bound;
        }

        public RandomIntsSpliterator trySplit() {
            long i = index, m = (i + fence) >>> 1;
            return (m <= i) ? null : new RandomIntsSpliterator(i, index = m, origin, bound);
        }

        public long estimateSize() {
            return fence - index;
        }

        public int characteristics() {
            return (Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.NONNULL | Spliterator.IMMUTABLE);
        }

        public boolean tryAdvance(IntConsumer consumer) {
            if (consumer == null) throw new NullPointerException();
            long i = index, f = fence;
            if (i < f) {
                consumer.accept(ThreadLocalRandom.current().internalNextInt(origin, bound));
                index = i + 1;
                return true;
            }
            return false;
        }

        public void forEachRemaining(IntConsumer consumer) {
            if (consumer == null) throw new NullPointerException();
            long i = index, f = fence;
            if (i < f) {
                index = f;
                int o = origin, b = bound;
                ThreadLocalRandom rng = ThreadLocalRandom.current();
                do {
                    consumer.accept(rng.internalNextInt(o, b));
                } while (++i < f);
            }
        }
    }

    /**
     * 用于 long 流的 Spliterator。
     */
    static final class RandomLongsSpliterator implements Spliterator.OfLong {
        long index;
        final long fence;
        final long origin;
        final long bound;
        RandomLongsSpliterator(long index, long fence, long origin, long bound) {
            this.index = index;
            this.fence = fence;
            this.origin = origin;
            this.bound = bound;
        }

        public RandomLongsSpliterator trySplit() {
            long i = index, m = (i + fence) >>> 1;
            return (m <= i) ? null : new RandomLongsSpliterator(i, index = m, origin, bound);
        }

        public long estimateSize() {
            return fence - index;
        }

        public int characteristics() {
            return (Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.NONNULL | Spliterator.IMMUTABLE);
        }

        public boolean tryAdvance(LongConsumer consumer) {
            if (consumer == null) throw new NullPointerException();
            long i = index, f = fence;
            if (i < f) {
                consumer.accept(ThreadLocalRandom.current().internalNextLong(origin, bound));
                index = i + 1;
                return true;
            }
            return false;
        }

        public void forEachRemaining(LongConsumer consumer) {
            if (consumer == null) throw new NullPointerException();
            long i = index, f = fence;
            if (i < f) {
                index = f;
                long o = origin, b = bound;
                ThreadLocalRandom rng = ThreadLocalRandom.current();
                do {
                    consumer.accept(rng.internalNextLong(o, b));
                } while (++i < f);
            }
        }
    }

    /**
     * 用于 double 流的 Spliterator。
     */
    static final class RandomDoublesSpliterator implements Spliterator.OfDouble {
        long index;
        final long fence;
        final double origin;
        final double bound;
        RandomDoublesSpliterator(long index, long fence, double origin, double bound) {
            this.index = index;
            this.fence = fence;
            this.origin = origin;
            this.bound = bound;
        }

        public RandomDoublesSpliterator trySplit() {
            long i = index, m = (i + fence) >>> 1;
            return (m <= i) ? null : new RandomDoublesSpliterator(i, index = m, origin, bound);
        }

        public long estimateSize() {
            return fence - index;
        }

        public int characteristics() {
            return (Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.NONNULL | Spliterator.IMMUTABLE);
        }

        public boolean tryAdvance(DoubleConsumer consumer) {
            if (consumer == null) throw new NullPointerException();
            long i = index, f = fence;
            if (i < f) {
                consumer.accept(ThreadLocalRandom.current().internalNextDouble(origin, bound));
                index = i + 1;
                return true;
            }
            return false;
        }

        public void forEachRemaining(DoubleConsumer consumer) {
            if (consumer == null) throw new NullPointerException();
            long i = index, f = fence;
            if (i < f) {
                index = f;
                double o = origin, b = bound;
                ThreadLocalRandom rng = ThreadLocalRandom.current();
                do {
                    consumer.accept(rng.internalNextDouble(o, b));
                } while (++i < f);
            }
        }
    }

    // 包内工具

    /*
     * 以下方法的使用说明可以在使用它们的类中找到。简而言之，一个线程的“探针”值是一个非零的哈希码，
     * 可能不会与任何 2 的幂碰撞空间中的其他现有线程发生冲突。当发生碰撞时，它会以伪随机方式调整（使用 Marsaglia XorShift 算法）。
     * nextSecondarySeed 方法与 ThreadLocalRandom 用于相同的上下文，但仅用于瞬态使用，例如自适应自旋/阻塞序列的随机调整，
     * 为此便宜的 RNG 足够了，原则上它可能会破坏主 ThreadLocalRandom 的用户可见统计属性，如果我们使用它。
     *
     * 注意：由于包保护问题，某些这些方法的版本也出现在某些子包类中。
     */

    /**
     * 返回当前线程的探针值，而不强制初始化。注意，调用 ThreadLocalRandom.current() 可用于在返回 0 时强制初始化。
     */
    static final int getProbe() {
        return UNSAFE.getInt(Thread.currentThread(), PROBE);
    }

    /**
     * 伪随机地推进并记录给定线程的探针值。
     */
    static final int advanceProbe(int probe) {
        probe ^= probe << 13;   // xorshift
        probe ^= probe >>> 17;
        probe ^= probe << 5;
        UNSAFE.putInt(Thread.currentThread(), PROBE, probe);
        return probe;
    }

    /**
     * 返回伪随机初始化或更新的次级种子。
     */
    static final int nextSecondarySeed() {
        int r;
        Thread t = Thread.currentThread();
        if ((r = UNSAFE.getInt(t, SECONDARY)) != 0) {
            r ^= r << 13;   // xorshift
            r ^= r >>> 17;
            r ^= r << 5;
        }
        else {
            localInit();
            if ((r = (int)UNSAFE.getLong(t, SEED)) == 0)
                r = 1; // 避免为 0
        }
        UNSAFE.putInt(t, SECONDARY, r);
        return r;
    }

    // 序列化支持

    private static final long serialVersionUID = -5851777807851030925L;

    /**
     * @serialField rnd long
     *              随机计算的种子
     * @serialField initialized boolean
     *              总是 true
     */
    private static final ObjectStreamField[] serialPersistentFields = {
            new ObjectStreamField("rnd", long.class),
            new ObjectStreamField("initialized", boolean.class),
    };

    /**
     * 将 {@code ThreadLocalRandom} 保存到流（即，将其序列化）。
     * @param s 流
     * @throws java.io.IOException 如果发生 I/O 错误
     */
    private void writeObject(java.io.ObjectOutputStream s)
            throws java.io.IOException {

        java.io.ObjectOutputStream.PutField fields = s.putFields();
        fields.put("rnd", UNSAFE.getLong(Thread.currentThread(), SEED));
        fields.put("initialized", true);
        s.writeFields();
    }

    /**
     * 返回 {@link #current() current} 线程的 {@code ThreadLocalRandom}。
     * @return 当前线程的 {@code ThreadLocalRandom}
     */
    private Object readResolve() {
        return current();
    }

    // Unsafe 机制
    private static final sun.misc.Unsafe UNSAFE;
    private static final long SEED;
    private static final long PROBE;
    private static final long SECONDARY;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            SEED = UNSAFE.objectFieldOffset
                    (tk.getDeclaredField("threadLocalRandomSeed"));
            PROBE = UNSAFE.objectFieldOffset
                    (tk.getDeclaredField("threadLocalRandomProbe"));
            SECONDARY = UNSAFE.objectFieldOffset
                    (tk.getDeclaredField("threadLocalRandomSecondarySeed"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}




