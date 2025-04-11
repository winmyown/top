

/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * https://creativecommons.org/publicdomain/zero/1.0/
 */


/*
 * 由 Doug Lea 编写，并得到了 JCP JSR-166 专家组成员的协助，
 * 发布到公共领域，解释见
 * https://creativecommons.org/publicdomain/zero/1.0/
 */

package org.top.java.netty.source.util.internal;

import static io.netty.util.internal.ObjectUtil.checkPositive;

import org.top.java.netty.source.util.internal.logging.InternalLogger;
import org.top.java.netty.source.util.internal.logging.InternalLoggerFactory;

import java.lang.Thread.UncaughtExceptionHandler;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A random number generator isolated to the current thread.  Like the
 * global {@link Random} generator used by the {@link
 * Math} class, a {@code ThreadLocalRandom} is initialized
 * with an internally generated seed that may not otherwise be
 * modified. When applicable, use of {@code ThreadLocalRandom} rather
 * than shared {@code Random} objects in concurrent programs will
 * typically encounter much less overhead and contention.  Use of
 * {@code ThreadLocalRandom} is particularly appropriate when multiple
 * tasks (for example, each a {@link io.netty.util.internal.chmv8.ForkJoinTask}) use random numbers
 * in parallel in thread pools.
 *
 * <p>Usages of this class should typically be of the form:
 * {@code ThreadLocalRandom.current().nextX(...)} (where
 * {@code X} is {@code Int}, {@code Long}, etc).
 * When all usages are of this form, it is never possible to
 * accidently share a {@code ThreadLocalRandom} across multiple threads.
 *
 * <p>This class also provides additional commonly used bounded random
 * generation methods.
 *
 * //since 1.7
 * //author Doug Lea
 */

/**
 * 一个隔离到当前线程的随机数生成器。与{@link Math}类使用的全局{@link Random}生成器类似，
 * {@code ThreadLocalRandom}使用内部生成的种子进行初始化，该种子不可修改。在适用的情况下，
 * 在并发程序中使用{@code ThreadLocalRandom}而不是共享的{@code Random}对象通常会遇到更少的开销和争用。
 * 当多个任务（例如，每个任务都是{@link io.netty.util.internal.chmv8.ForkJoinTask}）在线程池中并行使用随机数时，
 * 使用{@code ThreadLocalRandom}尤其合适。
 *
 * <p>使用此类时通常应采用以下形式：
 * {@code ThreadLocalRandom.current().nextX(...)}（其中{@code X}是{@code Int}、{@code Long}等）。
 * 当所有使用都采用这种形式时，永远不会意外地在多个线程之间共享{@code ThreadLocalRandom}。
 *
 * <p>此类还提供了其他常用的有界随机生成方法。
 *
 * //since 1.7
 * //author Doug Lea
 */
@SuppressWarnings("all")
public final class ThreadLocalRandom extends Random {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ThreadLocalRandom.class);

    private static final AtomicLong seedUniquifier = new AtomicLong();

    private static volatile long initialSeedUniquifier;

    private static final Thread seedGeneratorThread;
    private static final BlockingQueue<Long> seedQueue;
    private static final long seedGeneratorStartTime;
    private static volatile long seedGeneratorEndTime;

    static {
        initialSeedUniquifier = SystemPropertyUtil.getLong("io.netty.initialSeedUniquifier", 0);
        if (initialSeedUniquifier == 0) {
            boolean secureRandom = SystemPropertyUtil.getBoolean("java.util.secureRandomSeed", false);
            if (secureRandom) {
                seedQueue = new LinkedBlockingQueue<Long>();
                seedGeneratorStartTime = System.nanoTime();

                // Try to generate a real random number from /dev/random.

                // 尝试从 /dev/random 生成一个真正的随机数。
                // Get from a different thread to avoid blocking indefinitely on a machine without much entropy.
                // 从不同的线程获取，以避免在没有太多熵的机器上无限期阻塞。
                seedGeneratorThread = new Thread("initialSeedUniquifierGenerator") {
                    @Override
                    public void run() {
                        final SecureRandom random = new SecureRandom(); // Get the real random seed from /dev/random
                        final byte[] seed = random.generateSeed(8);
                        seedGeneratorEndTime = System.nanoTime();
                        long s = ((long) seed[0] & 0xff) << 56 |
                                 ((long) seed[1] & 0xff) << 48 |
                                 ((long) seed[2] & 0xff) << 40 |
                                 ((long) seed[3] & 0xff) << 32 |
                                 ((long) seed[4] & 0xff) << 24 |
                                 ((long) seed[5] & 0xff) << 16 |
                                 ((long) seed[6] & 0xff) <<  8 |
                                 (long) seed[7] & 0xff;
                        seedQueue.add(s);
                    }
                };
                seedGeneratorThread.setDaemon(true);
                seedGeneratorThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        logger.debug("An exception has been raised by {}", t.getName(), e);
                    }
                });
                seedGeneratorThread.start();
            } else {
                initialSeedUniquifier = mix64(System.currentTimeMillis()) ^ mix64(System.nanoTime());
                seedGeneratorThread = null;
                seedQueue = null;
                seedGeneratorStartTime = 0L;
            }
        } else {
            seedGeneratorThread = null;
            seedQueue = null;
            seedGeneratorStartTime = 0L;
        }
    }

    public static void setInitialSeedUniquifier(long initialSeedUniquifier) {
        ThreadLocalRandom.initialSeedUniquifier = initialSeedUniquifier;
    }

    public static long getInitialSeedUniquifier() {
        // Use the value set via the setter.
        // 使用通过 setter 设置的值。
        long initialSeedUniquifier = ThreadLocalRandom.initialSeedUniquifier;
        if (initialSeedUniquifier != 0) {
            return initialSeedUniquifier;
        }

        synchronized (ThreadLocalRandom.class) {
            initialSeedUniquifier = ThreadLocalRandom.initialSeedUniquifier;
            if (initialSeedUniquifier != 0) {
                return initialSeedUniquifier;
            }

            // Get the random seed from the generator thread with timeout.

            // 从生成器线程获取随机种子，带超时。
            final long timeoutSeconds = 3;
            final long deadLine = seedGeneratorStartTime + TimeUnit.SECONDS.toNanos(timeoutSeconds);
            boolean interrupted = false;
            for (;;) {
                final long waitTime = deadLine - System.nanoTime();
                try {
                    final Long seed;
                    if (waitTime <= 0) {
                        seed = seedQueue.poll();
                    } else {
                        seed = seedQueue.poll(waitTime, TimeUnit.NANOSECONDS);
                    }

                    if (seed != null) {
                        initialSeedUniquifier = seed;
                        break;
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                    logger.warn("Failed to generate a seed from SecureRandom due to an InterruptedException.");
                    break;
                }

                if (waitTime <= 0) {
                    seedGeneratorThread.interrupt();
                    logger.warn(
                            "Failed to generate a seed from SecureRandom within {} seconds. " +
                            "Not enough entropy?", timeoutSeconds
                    );
                    break;
                }
            }

            // Just in case the initialSeedUniquifier is zero or some other constant

            // 以防 initialSeedUniquifier 为零或其他常量
            initialSeedUniquifier ^= 0x3255ecdc33bae119L; // just a meaningless random number
            initialSeedUniquifier ^= Long.reverse(System.nanoTime());

            ThreadLocalRandom.initialSeedUniquifier = initialSeedUniquifier;

            if (interrupted) {
                // Restore the interrupt status because we don't know how to/don't need to handle it here.
                // 恢复中断状态，因为我们不知道如何处理/不需要在这里处理它。
                Thread.currentThread().interrupt();

                // Interrupt the generator thread if it's still running,

                // 如果生成器线程仍在运行，则中断它
                // in the hope that the SecureRandom provider raises an exception on interruption.
                // 希望 SecureRandom 提供者在中断时抛出异常。
                seedGeneratorThread.interrupt();
            }

            if (seedGeneratorEndTime == 0) {
                seedGeneratorEndTime = System.nanoTime();
            }

            return initialSeedUniquifier;
        }
    }

    private static long newSeed() {
        for (;;) {
            final long current = seedUniquifier.get();
            final long actualCurrent = current != 0? current : getInitialSeedUniquifier();

            // L'Ecuyer, "Tables of Linear Congruential Generators of Different Sizes and Good Lattice Structure", 1999

            // L'Ecuyer, "Tables of Linear Congruential Generators of Different Sizes and Good Lattice Structure", 1999
            final long next = actualCurrent * 181783497276652981L;

            if (seedUniquifier.compareAndSet(current, next)) {
                if (current == 0 && logger.isDebugEnabled()) {
                    if (seedGeneratorEndTime != 0) {
                        logger.debug(String.format(
                                "-Dio.netty.initialSeedUniquifier: 0x%016x (took %d ms)",
                                actualCurrent,
                                TimeUnit.NANOSECONDS.toMillis(seedGeneratorEndTime - seedGeneratorStartTime)));
                    } else {
                        logger.debug(String.format("-Dio.netty.initialSeedUniquifier: 0x%016x", actualCurrent));
                    }
                }
                return next ^ System.nanoTime();
            }
        }
    }

    // Borrowed from

    // 从  借来的
    // http://gee.cs.oswego.edu/cgi-bin/viewcvs.cgi/jsr166/src/main/java/util/concurrent/ThreadLocalRandom.java

/**
 * 一个专为多线程设计的高效随机数生成器。
 * 这个类是{@link java.util.Random}的一个子类，专门用于多线程环境。
 * 它在每个线程中维护一个独立的随机数生成器，从而避免了线程间的竞争。
 * 
 * <p>ThreadLocalRandom通常比{@link java.util.Random}在并发环境中更高效，
 * 因为它避免了全局锁的开销。
 * 
 * <p>使用示例：
 * <pre> {@code
 * int random = ThreadLocalRandom.current().nextInt(100);
 * }</pre>
 * 
 * <p>ThreadLocalRandom的实例不适合用于加密目的。
 * 
 * @since 1.7
 * @author Doug Lea
 */

    private static long mix64(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }

    // same constants as Random, but must be redeclared because private

    // 与Random相同的常量，但必须重新声明因为它们是私有的
    private static final long multiplier = 0x5DEECE66DL;
    private static final long addend = 0xBL;
    private static final long mask = (1L << 48) - 1;

    /**
     * The random seed. We can't use super.seed.
     */

    /**
     * 随机种子。我们不能使用super.seed。
     */
    private long rnd;

    /**
     * Initialization flag to permit calls to setSeed to succeed only
     * while executing the Random constructor.  We can't allow others
     * since it would cause setting seed in one part of a program to
     * unintentionally impact other usages by the thread.
     */

    /**
     * 初始化标志，允许仅在执行Random构造函数时调用setSeed成功。
     * 我们不能允许其他情况，因为这会导致在程序的一部分设置种子时，
     * 无意中影响线程的其他使用。
     */
    boolean initialized;

    // Padding to help avoid memory contention among seed updates in

    // 填充以帮助避免种子更新之间的内存争用
    // different TLRs in the common case that they are located near
    // 在通常情况下，它们位于附近的不同的TLRs
    // each other.
    // 彼此。
    private long pad0, pad1, pad2, pad3, pad4, pad5, pad6, pad7;

    /**
     * Constructor called only by localRandom.initialValue.
     */

    /**
     * 仅由 localRandom.initialValue 调用的构造函数。
     */
    ThreadLocalRandom() {
        super(newSeed());
        initialized = true;
    }

    /**
     * Returns the current thread's {@code ThreadLocalRandom}.
     *
     * @return the current thread's {@code ThreadLocalRandom}
     */

    /**
     * 返回当前线程的 {@code ThreadLocalRandom}。
     *
     * @return 当前线程的 {@code ThreadLocalRandom}
     */
    public static ThreadLocalRandom current() {
        return InternalThreadLocalMap.get().random();
    }

    /**
     * Throws {@code UnsupportedOperationException}.  Setting seeds in
     * this generator is not supported.
     *
     * @throws UnsupportedOperationException always
     */

    /**
     * 抛出 {@code UnsupportedOperationException}。在此生成器中设置种子不受支持。
     *
     * @throws UnsupportedOperationException 总是抛出
     */
    @Override
    public void setSeed(long seed) {
        if (initialized) {
            throw new UnsupportedOperationException();
        }
        rnd = (seed ^ multiplier) & mask;
    }

    @Override
    protected int next(int bits) {
        rnd = (rnd * multiplier + addend) & mask;
        return (int) (rnd >>> (48 - bits));
    }

    /**
     * Returns a pseudorandom, uniformly distributed value between the
     * given least value (inclusive) and bound (exclusive).
     *
     * @param least the least value returned
     * @param bound the upper bound (exclusive)
     * @throws IllegalArgumentException if least greater than or equal
     * to bound
     * @return the next value
     */

    /**
     * 返回一个在给定最小值（包含）和最大值（不包含）之间的伪随机、均匀分布的值。
     *
     * @param least 返回的最小值
     * @param bound 上限（不包含）
     * @throws IllegalArgumentException 如果最小值大于或等于最大值
     * @return 下一个值
     */
    public int nextInt(int least, int bound) {
        if (least >= bound) {
            throw new IllegalArgumentException();
        }
        return nextInt(bound - least) + least;
    }

    /**
     * Returns a pseudorandom, uniformly distributed value
     * between 0 (inclusive) and the specified value (exclusive).
     *
     * @param n the bound on the random number to be returned.  Must be
     *        positive.
     * @return the next value
     * @throws IllegalArgumentException if n is not positive
     */

    /**
     * 返回一个伪随机的、均匀分布的值，介于0（包含）和指定值（不包含）之间。
     *
     * @param n 要返回的随机数的上限。必须为正数。
     * @return 下一个值
     * @throws IllegalArgumentException 如果n不为正数
     */
    public long nextLong(long n) {
        checkPositive(n, "n");

        // Divide n by two until small enough for nextInt. On each

        // 将 n 除以二，直到足够小以用于 nextInt。在每次
        // iteration (at most 31 of them but usually much less),
        // 迭代（最多31次，但通常少得多）
        // randomly choose both whether to include high bit in result
        // 随机选择是否在结果中包含高位
        // (offset) and whether to continue with the lower vs upper
        // (偏移量) 以及是否继续使用下界还是上界
        // half (which makes a difference only if odd).
        // 一半（仅在奇数时产生影响）。
        long offset = 0;
        while (n >= Integer.MAX_VALUE) {
            int bits = next(2);
            long half = n >>> 1;
            long nextn = ((bits & 2) == 0) ? half : n - half;
            if ((bits & 1) == 0) {
                offset += n - nextn;
            }
            n = nextn;
        }
        return offset + nextInt((int) n);
    }

    /**
     * Returns a pseudorandom, uniformly distributed value between the
     * given least value (inclusive) and bound (exclusive).
     *
     * @param least the least value returned
     * @param bound the upper bound (exclusive)
     * @return the next value
     * @throws IllegalArgumentException if least greater than or equal
     * to bound
     */

    /**
     * 返回一个伪随机的、均匀分布的值，介于给定的最小值（包含）和最大值（不包含）之间。
     *
     * @param least 返回的最小值
     * @param bound 上限（不包含）
     * @return 下一个值
     * @throws IllegalArgumentException 如果最小值大于或等于最大值
     */
    public long nextLong(long least, long bound) {
        if (least >= bound) {
            throw new IllegalArgumentException();
        }
        return nextLong(bound - least) + least;
    }

    /**
     * Returns a pseudorandom, uniformly distributed {@code double} value
     * between 0 (inclusive) and the specified value (exclusive).
     *
     * @param n the bound on the random number to be returned.  Must be
     *        positive.
     * @return the next value
     * @throws IllegalArgumentException if n is not positive
     */

    /**
     * 返回一个伪随机的、均匀分布的 {@code double} 值，
     * 介于 0（包含）和指定值（不包含）之间。
     *
     * @param n 要返回的随机数的上限。必须为正数。
     * @return 下一个值
     * @throws IllegalArgumentException 如果 n 不是正数
     */
    public double nextDouble(double n) {
        checkPositive(n, "n");
        return nextDouble() * n;
    }

    /**
     * Returns a pseudorandom, uniformly distributed value between the
     * given least value (inclusive) and bound (exclusive).
     *
     * @param least the least value returned
     * @param bound the upper bound (exclusive)
     * @return the next value
     * @throws IllegalArgumentException if least greater than or equal
     * to bound
     */

    /**
     * 返回一个伪随机的、均匀分布的值，介于给定的最小值（包含）和最大值（不包含）之间。
     *
     * @param least 返回的最小值
     * @param bound 上限（不包含）
     * @return 下一个值
     * @throws IllegalArgumentException 如果最小值大于或等于最大值
     */
    public double nextDouble(double least, double bound) {
        if (least >= bound) {
            throw new IllegalArgumentException();
        }
        return nextDouble() * (bound - least) + least;
    }

    private static final long serialVersionUID = -5851777807851030925L;
}
