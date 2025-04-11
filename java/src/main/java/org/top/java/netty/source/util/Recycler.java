
package org.top.java.netty.source.util;

import org.top.java.netty.source.util.concurrent.FastThreadLocal;
import org.top.java.netty.source.util.internal.ObjectPool;
import org.top.java.netty.source.util.internal.PlatformDependent;
import org.top.java.netty.source.util.internal.SystemPropertyUtil;
import org.top.java.netty.source.util.internal.logging.InternalLogger;
import org.top.java.netty.source.util.internal.logging.InternalLoggerFactory;
import org.top.java.netty.source.util.internal.shaded.org.jctools.queues.MessagePassingQueue;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static org.top.java.netty.source.util.internal.PlatformDependent.newMpscQueue;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Light-weight object pool based on a thread-local stack.
 *
 * @param <T> the type of the pooled object
 */

/**
 * 基于线程本地栈的轻量级对象池。
 *
 * @param <T> 池化对象的类型
 */
public abstract class Recycler<T> {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(Recycler.class);
    private static final Handle<?> NOOP_HANDLE = new Handle<Object>() {
        @Override
        public void recycle(Object object) {
            // NOOP
            // NOOP
        }

        @Override
        public String toString() {
            return "NOOP_HANDLE";
        }
    };
    private static final int DEFAULT_INITIAL_MAX_CAPACITY_PER_THREAD = 4 * 1024; // Use 4k instances as default.
    private static final int DEFAULT_MAX_CAPACITY_PER_THREAD;
    private static final int RATIO;
    private static final int DEFAULT_QUEUE_CHUNK_SIZE_PER_THREAD;
    private static final boolean BLOCKING_POOL;

    static {
        // In the future, we might have different maxCapacity for different object types.
        // 在未来，我们可能会为不同的对象类型设置不同的 maxCapacity。
        // e.g. io.netty.recycler.maxCapacity.writeTask
        // 例如 io.netty.recycler.maxCapacity.writeTask
        //      io.netty.recycler.maxCapacity.outboundBuffer
        //      io.netty.recycler.maxCapacity.outboundBuffer
        int maxCapacityPerThread = SystemPropertyUtil.getInt("io.netty.recycler.maxCapacityPerThread",
                SystemPropertyUtil.getInt("io.netty.recycler.maxCapacity", DEFAULT_INITIAL_MAX_CAPACITY_PER_THREAD));
        if (maxCapacityPerThread < 0) {
            maxCapacityPerThread = DEFAULT_INITIAL_MAX_CAPACITY_PER_THREAD;
        }

        DEFAULT_MAX_CAPACITY_PER_THREAD = maxCapacityPerThread;
        DEFAULT_QUEUE_CHUNK_SIZE_PER_THREAD = SystemPropertyUtil.getInt("io.netty.recycler.chunkSize", 32);

        // By default we allow one push to a Recycler for each 8th try on handles that were never recycled before.

        // 默认情况下，对于从未回收过的句柄，我们允许每8次尝试向回收器推送一次。
        // This should help to slowly increase the capacity of the recycler while not be too sensitive to allocation
        // 这应该有助于逐步增加回收器的容量，同时对分配不太敏感
        // bursts.
        // 突发。
        RATIO = max(0, SystemPropertyUtil.getInt("io.netty.recycler.ratio", 8));

        BLOCKING_POOL = SystemPropertyUtil.getBoolean("io.netty.recycler.blocking", false);

        if (logger.isDebugEnabled()) {
            if (DEFAULT_MAX_CAPACITY_PER_THREAD == 0) {
                logger.debug("-Dio.netty.recycler.maxCapacityPerThread: disabled");
                logger.debug("-Dio.netty.recycler.ratio: disabled");
                logger.debug("-Dio.netty.recycler.chunkSize: disabled");
                logger.debug("-Dio.netty.recycler.blocking: disabled");
            } else {
                logger.debug("-Dio.netty.recycler.maxCapacityPerThread: {}", DEFAULT_MAX_CAPACITY_PER_THREAD);
                logger.debug("-Dio.netty.recycler.ratio: {}", RATIO);
                logger.debug("-Dio.netty.recycler.chunkSize: {}", DEFAULT_QUEUE_CHUNK_SIZE_PER_THREAD);
                logger.debug("-Dio.netty.recycler.blocking: {}", BLOCKING_POOL);
            }
        }
    }

    private final int maxCapacityPerThread;
    private final int interval;
    private final int chunkSize;
    private final FastThreadLocal<LocalPool<T>> threadLocal = new FastThreadLocal<LocalPool<T>>() {
        @Override
        protected LocalPool<T> initialValue() {
            return new LocalPool<T>(maxCapacityPerThread, interval, chunkSize);
        }

        @Override
        protected void onRemoval(LocalPool<T> value) throws Exception {
            super.onRemoval(value);
            MessagePassingQueue<DefaultHandle<T>> handles = value.pooledHandles;
            value.pooledHandles = null;
            handles.clear();
        }
    };

    protected Recycler() {
        this(DEFAULT_MAX_CAPACITY_PER_THREAD);
    }

    protected Recycler(int maxCapacityPerThread) {
        this(maxCapacityPerThread, RATIO, DEFAULT_QUEUE_CHUNK_SIZE_PER_THREAD);
    }

    /**
     * @deprecated Use one of the following instead:
     * {@link #Recycler()}, {@link #Recycler(int)}, {@link #Recycler(int, int, int)}.
     */

    /**
     * @deprecated 请使用以下方法之一代替：
     * {@link #Recycler()}, {@link #Recycler(int)}, {@link #Recycler(int, int, int)}.
     */
    @Deprecated
    @SuppressWarnings("unused") // Parameters we can't remove due to compatibility.
    protected Recycler(int maxCapacityPerThread, int maxSharedCapacityFactor) {
        this(maxCapacityPerThread, RATIO, DEFAULT_QUEUE_CHUNK_SIZE_PER_THREAD);
    }

    /**
     * @deprecated Use one of the following instead:
     * {@link #Recycler()}, {@link #Recycler(int)}, {@link #Recycler(int, int, int)}.
     */

    /**
     * @deprecated 请使用以下方法之一代替：
     * {@link #Recycler()}, {@link #Recycler(int)}, {@link #Recycler(int, int, int)}.
     */
    @Deprecated
    @SuppressWarnings("unused") // Parameters we can't remove due to compatibility.
    protected Recycler(int maxCapacityPerThread, int maxSharedCapacityFactor,
                       int ratio, int maxDelayedQueuesPerThread) {
        this(maxCapacityPerThread, ratio, DEFAULT_QUEUE_CHUNK_SIZE_PER_THREAD);
    }

    /**
     * @deprecated Use one of the following instead:
     * {@link #Recycler()}, {@link #Recycler(int)}, {@link #Recycler(int, int, int)}.
     */

    /**
     * @deprecated 请使用以下方法之一代替：
     * {@link #Recycler()}, {@link #Recycler(int)}, {@link #Recycler(int, int, int)}.
     */
    @Deprecated
    @SuppressWarnings("unused") // Parameters we can't remove due to compatibility.
    protected Recycler(int maxCapacityPerThread, int maxSharedCapacityFactor,
                       int ratio, int maxDelayedQueuesPerThread, int delayedQueueRatio) {
        this(maxCapacityPerThread, ratio, DEFAULT_QUEUE_CHUNK_SIZE_PER_THREAD);
    }

    protected Recycler(int maxCapacityPerThread, int ratio, int chunkSize) {
        interval = max(0, ratio);
        if (maxCapacityPerThread <= 0) {
            this.maxCapacityPerThread = 0;
            this.chunkSize = 0;
        } else {
            this.maxCapacityPerThread = max(4, maxCapacityPerThread);
            this.chunkSize = max(2, min(chunkSize, this.maxCapacityPerThread >> 1));
        }
    }

    @SuppressWarnings("unchecked")
    public final T get() {
        if (maxCapacityPerThread == 0) {
            return newObject((Handle<T>) NOOP_HANDLE);
        }
        LocalPool<T> localPool = threadLocal.get();
        DefaultHandle<T> handle = localPool.claim();
        T obj;
        if (handle == null) {
            handle = localPool.newHandle();
            if (handle != null) {
                obj = newObject(handle);
                handle.set(obj);
            } else {
                obj = newObject((Handle<T>) NOOP_HANDLE);
            }
        } else {
            obj = handle.get();
        }

        return obj;
    }

    /**
     * @deprecated use {@link Handle#recycle(Object)}.
     */

    /**
     * @deprecated 使用 {@link Handle#recycle(Object)}。
     */
    @Deprecated
    public final boolean recycle(T o, Handle<T> handle) {
        if (handle == NOOP_HANDLE) {
            return false;
        }

        handle.recycle(o);
        return true;
    }

    final int threadLocalSize() {
        return threadLocal.get().pooledHandles.size();
    }

    protected abstract T newObject(Handle<T> handle);

    @SuppressWarnings("ClassNameSameAsAncestorName") // Can't change this due to compatibility.
    public interface Handle<T> extends ObjectPool.Handle<T>  { }

    private static final class DefaultHandle<T> implements Handle<T> {
        private static final int STATE_CLAIMED = 0;
        private static final int STATE_AVAILABLE = 1;
        private static final AtomicIntegerFieldUpdater<DefaultHandle<?>> STATE_UPDATER;
        static {
            AtomicIntegerFieldUpdater<?> updater = AtomicIntegerFieldUpdater.newUpdater(DefaultHandle.class, "state");
            //noinspection unchecked
            //不检查
            STATE_UPDATER = (AtomicIntegerFieldUpdater<DefaultHandle<?>>) updater;
        }

        @SuppressWarnings({"FieldMayBeFinal", "unused"}) // Updated by STATE_UPDATER.
        private volatile int state; // State is initialised to STATE_CLAIMED (aka. 0) so they can be released.
        private final LocalPool<T> localPool;
        private T value;

        DefaultHandle(LocalPool<T> localPool) {
            this.localPool = localPool;
        }

        @Override
        public void recycle(Object object) {
            if (object != value) {
                throw new IllegalArgumentException("object does not belong to handle");
            }
            localPool.release(this);
        }

        T get() {
            return value;
        }

        void set(T value) {
            this.value = value;
        }

        boolean availableToClaim() {
            if (state != STATE_AVAILABLE) {
                return false;
            }
            return STATE_UPDATER.compareAndSet(this, STATE_AVAILABLE, STATE_CLAIMED);
        }

        void toAvailable() {
            int prev = STATE_UPDATER.getAndSet(this, STATE_AVAILABLE);
            if (prev == STATE_AVAILABLE) {
                throw new IllegalStateException("Object has been recycled already.");
            }
        }
    }

    private static final class LocalPool<T> {
        private final int ratioInterval;
        private volatile MessagePassingQueue<DefaultHandle<T>> pooledHandles;
        private int ratioCounter;

        @SuppressWarnings("unchecked")
        LocalPool(int maxCapacity, int ratioInterval, int chunkSize) {
            this.ratioInterval = ratioInterval;
            if (BLOCKING_POOL) {
                pooledHandles = new BlockingMessageQueue<DefaultHandle<T>>(maxCapacity);
            } else {
                pooledHandles = (MessagePassingQueue<DefaultHandle<T>>) newMpscQueue(chunkSize, maxCapacity);
            }
            ratioCounter = ratioInterval; // Start at interval so the first one will be recycled.
        }

        DefaultHandle<T> claim() {
            MessagePassingQueue<DefaultHandle<T>> handles = pooledHandles;
            if (handles == null) {
                return null;
            }
            DefaultHandle<T> handle;
            do {
                handle = handles.relaxedPoll();
            } while (handle != null && !handle.availableToClaim());
            return handle;
        }

        void release(DefaultHandle<T> handle) {
            MessagePassingQueue<DefaultHandle<T>> handles = pooledHandles;
            handle.toAvailable();
            if (handles != null) {
                handles.relaxedOffer(handle);
            }
        }

        DefaultHandle<T> newHandle() {
            if (++ratioCounter >= ratioInterval) {
                ratioCounter = 0;
                return new DefaultHandle<T>(this);
            }
            return null;
        }
    }

    /**
     * This is an implementation of {@link MessagePassingQueue}, similar to what might be returned from
     * {@link PlatformDependent#newMpscQueue(int)}, but intended to be used for debugging purpose.
     * The implementation relies on synchronised monitor locks for thread-safety.
     * The {@code drain} and {@code fill} bulk operations are not supported by this implementation.
     */

    /**
     * 这是 {@link MessagePassingQueue} 的一个实现，类似于 {@link PlatformDependent#newMpscQueue(int)} 可能返回的结果，
     * 但此实现旨在用于调试目的。
     * 该实现依赖于同步的监视器锁来保证线程安全。
     * 此实现不支持 {@code drain} 和 {@code fill} 批量操作。
     */
    private static final class BlockingMessageQueue<T> implements MessagePassingQueue<T> {
        private final Queue<T> deque;
        private final int maxCapacity;

        BlockingMessageQueue(int maxCapacity) {
            this.maxCapacity = maxCapacity;
            // This message passing queue is backed by an ArrayDeque instance,
            // 该消息传递队列由ArrayDeque实例支持，
            // made thread-safe by synchronising on `this` BlockingMessageQueue instance.
            // 通过同步 `this` BlockingMessageQueue 实例实现线程安全。
            // Why ArrayDeque?
            // 为什么选择ArrayDeque？
            // We use ArrayDeque instead of LinkedList or LinkedBlockingQueue because it's more space efficient.
            // 我们使用 ArrayDeque 而不是 LinkedList 或 LinkedBlockingQueue，因为它更节省空间。
            // We use ArrayDeque instead of ArrayList because we need the queue APIs.
            // 我们使用 ArrayDeque 而不是 ArrayList，因为我们需要队列的 API。
            // We use ArrayDeque instead of ConcurrentLinkedQueue because CLQ is unbounded and has O(n) size().
            // 我们使用 ArrayDeque 而不是 ConcurrentLinkedQueue，因为 CLQ 是无界的并且 size() 的时间复杂度为 O(n)。
            // We use ArrayDeque instead of ArrayBlockingQueue because ABQ allocates its max capacity up-front,
            // 我们使用 ArrayDeque 而不是 ArrayBlockingQueue，因为 ABQ 会预先分配其最大容量，
            // and these queues will usually have large capacities, in potentially great numbers (one per thread),
            // 这些队列通常具有较大的容量，可能数量众多（每个线程一个），
            // but often only have comparatively few items in them.
            // 但通常其中只有相对较少的项目。
            deque = new ArrayDeque<T>();
        }

        @Override
        public synchronized boolean offer(T e) {
            if (deque.size() == maxCapacity) {
                return false;
            }
            return deque.offer(e);
        }

        @Override
        public synchronized T poll() {
            return deque.poll();
        }

        @Override
        public synchronized T peek() {
            return deque.peek();
        }

        @Override
        public synchronized int size() {
            return deque.size();
        }

        @Override
        public synchronized void clear() {
            deque.clear();
        }

        @Override
        public synchronized boolean isEmpty() {
            return deque.isEmpty();
        }

        @Override
        public int capacity() {
            return maxCapacity;
        }

        @Override
        public boolean relaxedOffer(T e) {
            return offer(e);
        }

        @Override
        public T relaxedPoll() {
            return poll();
        }

        @Override
        public T relaxedPeek() {
            return peek();
        }

        @Override
        public int drain(Consumer<T> c, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int fill(Supplier<T> s, int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int drain(Consumer<T> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int fill(Supplier<T> s) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void drain(Consumer<T> c, WaitStrategy wait, ExitCondition exit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void fill(Supplier<T> s, WaitStrategy wait, ExitCondition exit) {
            throw new UnsupportedOperationException();
        }
    }
}
