package org.top.java.concurrent.source.atomic;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 上午11:32
 */

import org.top.java.concurrent.source.ThreadLocalRandom;

import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;

/**
 * 一个包局部类，保存支持 64 位值动态分段的类的通用表示和机制。
 * 该类扩展了 Number，因此具体子类必须公开扩展它。
 */
@SuppressWarnings("serial")
abstract class Striped64 extends Number {
    /*
     * 该类维护一个延迟初始化的原子更新变量表外加一个额外的 "base" 字段。
     * 表的大小是 2 的幂。索引使用带掩码的每线程哈希码。该类中的几乎所有声明
     * 都是包私有的，由子类直接访问。
     *
     * 表条目是类 Cell 的实例；它是 AtomicLong 的变体，通过 @sun.misc.Contended 进行填充，
     * 以减少缓存争用。对于大多数 Atomics 来说，填充是过度的，因为它们通常在内存中
     * 分散，因此不会相互干扰太多。但位于数组中的 Atomic 对象往往会相互邻近放置，
     * 因此大多数情况下会共享缓存行（对性能有巨大负面影响），除非采取这种预防措施。
     *
     * 部分由于 Cells 相对较大，我们避免在不需要时创建它们。当没有竞争时，所有更新
     * 都是对 base 字段进行的。第一次竞争（在 base 更新失败的 CAS 操作中）时，表的大小
     * 初始化为 2。表大小会在进一步的竞争下加倍，直到达到大于等于 CPU 数量的最近的 2 的幂。
     * 表槽在需要之前保持为空（null）。
     *
     * 用于初始化和调整表大小以及使用新 Cells 填充槽的单个自旋锁 ("cellsBusy")。
     * 没有必要使用阻塞锁；当锁不可用时，线程会尝试其他槽（或 base）。在这些重试过程中，
     * 竞争加剧且局部性降低，但仍优于其他替代方案。
     *
     * 通过 ThreadLocalRandom 维护的线程探测字段用作每线程的哈希码。
     * 我们允许它们在 slot 0 发生竞争之前保持未初始化为零（如果它们以这种方式出现）。
     * 然后它们被初始化为通常不会与其他线程冲突的值。当执行更新操作时，
     * 失败的 CAS 操作指示竞争和/或表冲突。发生冲突时，如果表大小小于容量，
     * 则它会加倍，除非其他线程持有锁。如果哈希槽为空且锁可用，则会创建一个新的 Cell。
     * 否则，如果槽存在，则尝试 CAS 操作。通过 "双重哈希" 进行重试，使用二次哈希
     * （Marsaglia XorShift）尝试找到空闲槽。
     *
     * 表的大小有上限，因为当线程数量多于 CPU 时，假设每个线程都绑定到一个 CPU，
     * 存在一个完美的哈希函数将线程映射到槽中，从而消除冲突。当我们达到容量时，
     * 我们通过随机改变冲突线程的哈希码来寻找这种映射。由于搜索是随机的，冲突
     * 仅通过 CAS 失败变得可知，因此收敛可能较慢，而且由于线程通常不会永远绑定到 CPU，
     * 可能根本不会发生。然而，尽管有这些限制，在这些情况下观察到的竞争率通常较低。
     *
     * 当曾经哈希到它的线程终止时，或者表加倍导致没有线程在扩展掩码下哈希到它时，
     * Cell 可能变得未使用。我们不会尝试检测或删除这些 Cell，假设对于长时间运行的实例，
     * 观察到的竞争水平将再次发生，因此最终仍然需要这些 Cell；而对于短时间的实例，
     * 这并不重要。
     */

    /**
     * 填充的 AtomicLong 变体，只支持原始访问和 CAS 操作。
     *
     * JVM 内部说明：如果提供了仅发布形式的 CAS，则可以在此使用。
     */
    @sun.misc.Contended static final class Cell {
        volatile long value;
        Cell(long x) { value = x; }
        final boolean cas(long cmp, long val) {
            return UNSAFE.compareAndSwapLong(this, valueOffset, cmp, val);
        }

        // Unsafe机制
        private static final sun.misc.Unsafe UNSAFE;
        private static final long valueOffset;
        static {
            try {
                UNSAFE = sun.misc.Unsafe.getUnsafe();
                Class<?> ak = Cell.class;
                valueOffset = UNSAFE.objectFieldOffset(ak.getDeclaredField("value"));
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /** CPU数量，用于限制表的大小 */
    static final int NCPU = Runtime.getRuntime().availableProcessors();

    /**
     * Cell的表。当非空时，大小是 2 的幂。
     */
    transient volatile Cell[] cells;

    /**
     * 基础值，主要在没有竞争时使用，但在表初始化竞争时也用作后备。通过 CAS 更新。
     */
    transient volatile long base;

    /**
     * 用于调整大小和/或创建 Cell 的自旋锁（通过 CAS 锁定）。
     */
    transient volatile int cellsBusy;

    /**
     * 包私有默认构造函数
     */
    Striped64() {
    }

    /**
     * CAS 更新 base 字段。
     */
    final boolean casBase(long cmp, long val) {
        return UNSAFE.compareAndSwapLong(this, BASE, cmp, val);
    }

    /**
     * CAS 更新 cellsBusy 字段从 0 到 1 以获取锁。
     */
    final boolean casCellsBusy() {
        return UNSAFE.compareAndSwapInt(this, CELLSBUSY, 0, 1);
    }

    /**
     * 返回当前线程的探测值。
     * 因为打包限制从 ThreadLocalRandom 复制。
     */
    static final int getProbe() {
        return UNSAFE.getInt(Thread.currentThread(), PROBE);
    }

    /**
     * 伪随机推进并记录给定线程的探测值。
     * 因为打包限制从 ThreadLocalRandom 复制。
     */
    static final int advanceProbe(int probe) {
        probe ^= probe << 13;   // xorshift
        probe ^= probe >>> 17;
        probe ^= probe << 5;
        UNSAFE.putInt(Thread.currentThread(), PROBE, probe);
        return probe;
    }

    /**
     * 处理涉及初始化、调整大小、创建新 Cells 和/或竞争的更新情况。
     * 参见上文解释。此方法通常遇到乐观重试代码的非模块化问题，
     * 依赖于重新检查的一组读取操作。
     *
     * @param x 要累加的值
     * @param fn 更新函数，或者 null 表示加法（此约定避免了 LongAdder 中需要额外字段或函数的需求）。
     * @param wasUncontended 如果在调用前 CAS 失败，则为 false
     */
    final void longAccumulate(long x, LongBinaryOperator fn, boolean wasUncontended) {
        int h;
        if ((h = getProbe()) == 0) {
            ThreadLocalRandom.current(); // 强制初始化
            h = getProbe();
            wasUncontended = true;
        }
        boolean collide = false;  // 如果最后一个槽不为空，则为 true
        for (;;) {
            Cell[] as; Cell a; int n; long v;
            if ((as = cells) != null && (n = as.length) > 0) {
                if ((a = as[(n - 1) & h]) == null) {
                    if (cellsBusy == 0) {  // 尝试附加新的 Cell
                        Cell r = new Cell(x);  // 乐观地创建
                        if (cellsBusy == 0 && casCellsBusy()) {
                            boolean created = false;
                            try {  // 在锁下重新检查
                                Cell[] rs; int m, j;
                                if ((rs = cells) != null && (m = rs.length) > 0 &&
                                        rs[j = (m - 1) & h] == null) {
                                    rs[j] = r;
                                    created = true;
                                }
                            } finally {
                                cellsBusy = 0;
                            }
                            if (created)
                                break;
                            continue;  // 现在槽已非空
                        }
                    }
                    collide = false;
                } else if (!wasUncontended)  // 已知 CAS 失败
                    wasUncontended = true;  // 重新哈希后继续
                else if (a.cas(v = a.value, ((fn == null) ? v + x :
                        fn.applyAsLong(v, x))))
                    break;
                else if (n >= NCPU || cells != as)
                    collide = false;  // 达到最大大小或陈旧
                else if (!collide)
                    collide = true;
                else if (cellsBusy == 0 && casCellsBusy()) {
                    try {
                        if (cells == as) {  // 扩展表，除非陈旧
                            Cell[] rs = new Cell[n << 1];
                            for (int i = 0; i < n; ++i)
                                rs[i] = as[i];
                            cells = rs;
                        }
                    } finally {
                        cellsBusy = 0;
                    }
                    collide = false;
                    continue;  // 使用扩展表重试
                }
                h = advanceProbe(h);
            } else if (cellsBusy == 0 && cells == as && casCellsBusy()) {
                boolean init = false;
                try {  // 初始化表
                    if (cells == as) {
                        Cell[] rs = new Cell[2];
                        rs[h & 1] = new Cell(x);
                        cells = rs;
                        init = true;
                    }
                } finally {
                    cellsBusy = 0;
                }
                if (init)
                    break;
            } else if (casBase(v = base, ((fn == null) ? v + x :
                    fn.applyAsLong(v, x))))
                break;  // 退回到使用 base
        }
    }

    /**
     * 与 longAccumulate 相同，但涉及 long/double 的转换太多，
     * 无法合理地与 long 版本合并，考虑到此类的低开销要求。
     * 因此必须通过复制/粘贴/适配维护。
     */
    final void doubleAccumulate(double x, DoubleBinaryOperator fn, boolean wasUncontended) {
        int h;
        if ((h = getProbe()) == 0) {
            ThreadLocalRandom.current(); // 强制初始化
            h = getProbe();
            wasUncontended = true;
        }
        boolean collide = false;  // 如果最后一个槽不为空，则为 true
        for (;;) {
            Cell[] as; Cell a; int n; long v;
            if ((as = cells) != null && (n = as.length) > 0) {
                if ((a = as[(n - 1) & h]) == null) {
                    if (cellsBusy == 0) {  // 尝试附加新的 Cell
                        Cell r = new Cell(Double.doubleToRawLongBits(x));
                        if (cellsBusy == 0 && casCellsBusy()) {
                            boolean created = false;
                            try {  // 在锁下重新检查
                                Cell[] rs; int m, j;
                                if ((rs = cells) != null && (m = rs.length) > 0 &&
                                        rs[j = (m - 1) & h] == null) {
                                    rs[j] = r;
                                    created = true;
                                }
                            } finally {
                                cellsBusy = 0;
                            }
                            if (created)
                                break;
                            continue;  // 现在槽已非空
                        }
                    }
                    collide = false;
                } else if (!wasUncontended)  // 已知 CAS 失败
                    wasUncontended = true;  // 重新哈希后继续
                else if (a.cas(v = a.value,
                        ((fn == null) ?
                                Double.doubleToRawLongBits(Double.longBitsToDouble(v) + x) :
                                Double.doubleToRawLongBits(fn.applyAsDouble(Double.longBitsToDouble(v), x)))))
                    break;
                else if (n >= NCPU || cells != as)
                    collide = false;  // 达到最大大小或陈旧
                else if (!collide)
                    collide = true;
                else if (cellsBusy == 0 && casCellsBusy()) {
                    try {
                        if (cells == as) {  // 扩展表，除非陈旧
                            Cell[] rs = new Cell[n << 1];
                            for (int i = 0; i < n; ++i)
                                rs[i] = as[i];
                            cells = rs;
                        }
                    } finally {
                        cellsBusy = 0;
                    }
                    collide = false;
                    continue;  // 使用扩展表重试
                }
                h = advanceProbe(h);
            } else if (cellsBusy == 0 && cells == as && casCellsBusy()) {
                boolean init = false;
                try {  // 初始化表
                    if (cells == as) {
                        Cell[] rs = new Cell[2];
                        rs[h & 1] = new Cell(Double.doubleToRawLongBits(x));
                        cells = rs;
                        init = true;
                    }
                } finally {
                    cellsBusy = 0;
                }
                if (init)
                    break;
            } else if (casBase(v = base,
                    ((fn == null) ?
                            Double.doubleToRawLongBits(Double.longBitsToDouble(v) + x) :
                            Double.doubleToRawLongBits(fn.applyAsDouble(Double.longBitsToDouble(v), x)))))
                break;  // 退回到使用 base
        }
    }


    // Unsafe机制
    private static final sun.misc.Unsafe UNSAFE;
    private static final long BASE;
    private static final long CELLSBUSY;
    private static final long PROBE;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> sk = Striped64.class;
            BASE = UNSAFE.objectFieldOffset(sk.getDeclaredField("base"));
            CELLSBUSY = UNSAFE.objectFieldOffset(sk.getDeclaredField("cellsBusy"));
            Class<?> tk = Thread.class;
            PROBE = UNSAFE.objectFieldOffset(tk.getDeclaredField("threadLocalRandomProbe"));
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}

