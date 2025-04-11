
package org.top.java.netty.source.channel;

import io.netty.util.internal.ObjectUtil;

import java.util.ArrayDeque;
import java.util.Queue;

import static io.netty.util.internal.ObjectUtil.checkPositiveOrZero;

/**
 * This implementation allows to register {@link io.netty.channel.ChannelFuture} instances which will get notified once some amount of
 * data was written and so a checkpoint was reached.
 */

/**
 * 该实现允许注册 {@link io.netty.channel.ChannelFuture} 实例，这些实例将在写入一定量的数据并达到检查点时收到通知。
 */
public final class ChannelFlushPromiseNotifier {

    private long writeCounter;
    private final Queue<FlushCheckpoint> flushCheckpoints = new ArrayDeque<FlushCheckpoint>();
    private final boolean tryNotify;

    /**
     * Create a new instance
     *
     * @param tryNotify if {@code true} the {@link io.netty.channel.ChannelPromise}s will get notified with
     *                  {@link io.netty.channel.ChannelPromise#trySuccess()} and {@link io.netty.channel.ChannelPromise#tryFailure(Throwable)}.
     *                  Otherwise {@link io.netty.channel.ChannelPromise#setSuccess()} and {@link io.netty.channel.ChannelPromise#setFailure(Throwable)}
     *                  is used
     */

    /**
     * 创建新实例
     *
     * @param tryNotify 如果为 {@code true}，则 {@link io.netty.channel.ChannelPromise} 将使用
     *                  {@link io.netty.channel.ChannelPromise#trySuccess()} 和 {@link io.netty.channel.ChannelPromise#tryFailure(Throwable)} 进行通知。
     *                  否则将使用 {@link io.netty.channel.ChannelPromise#setSuccess()} 和 {@link io.netty.channel.ChannelPromise#setFailure(Throwable)}
     */
    public ChannelFlushPromiseNotifier(boolean tryNotify) {
        this.tryNotify = tryNotify;
    }

    /**
     * Create a new instance which will use {@link io.netty.channel.ChannelPromise#setSuccess()} and
     * {@link io.netty.channel.ChannelPromise#setFailure(Throwable)} to notify the {@link io.netty.channel.ChannelPromise}s.
     */

    /**
     * 创建一个新实例，该实例将使用 {@link io.netty.channel.ChannelPromise#setSuccess()} 和
     * {@link io.netty.channel.ChannelPromise#setFailure(Throwable)} 来通知 {@link io.netty.channel.ChannelPromise}s。
     */
    public ChannelFlushPromiseNotifier() {
        this(false);
    }

    /**
     * @deprecated use {@link #add(io.netty.channel.ChannelPromise, long)}
     */

    /**
     * @deprecated 使用 {@link #add(io.netty.channel.ChannelPromise, long)}
     */
    @Deprecated
    public ChannelFlushPromiseNotifier add(ChannelPromise promise, int pendingDataSize) {
        return add(promise, (long) pendingDataSize);
    }

    /**
     * Add a {@link io.netty.channel.ChannelPromise} to this {@link ChannelFlushPromiseNotifier} which will be notified after the given
     * {@code pendingDataSize} was reached.
     */

    /**
     * 将一个 {@link io.netty.channel.ChannelPromise} 添加到此 {@link ChannelFlushPromiseNotifier} 中，该 promise 将在达到给定的 {@code pendingDataSize} 后被通知。
     */
    public ChannelFlushPromiseNotifier add(ChannelPromise promise, long pendingDataSize) {
        ObjectUtil.checkNotNull(promise, "promise");
        checkPositiveOrZero(pendingDataSize, "pendingDataSize");
        long checkpoint = writeCounter + pendingDataSize;
        if (promise instanceof FlushCheckpoint) {
            FlushCheckpoint cp = (FlushCheckpoint) promise;
            cp.flushCheckpoint(checkpoint);
            flushCheckpoints.add(cp);
        } else {
            flushCheckpoints.add(new DefaultFlushCheckpoint(checkpoint, promise));
        }
        return this;
    }
    /**
     * Increase the current write counter by the given delta
     */
    /**
     * 将当前写入计数器增加给定的增量
     */
    public ChannelFlushPromiseNotifier increaseWriteCounter(long delta) {
        checkPositiveOrZero(delta, "delta");
        writeCounter += delta;
        return this;
    }

    /**
     * Return the current write counter of this {@link ChannelFlushPromiseNotifier}
     */

    /**
     * 返回此 {@link ChannelFlushPromiseNotifier} 的当前写计数器
     */
    public long writeCounter() {
        return writeCounter;
    }

    /**
     * Notify all {@link io.netty.channel.ChannelFuture}s that were registered with {@link #add(io.netty.channel.ChannelPromise, int)} and
     * their pendingDatasize is smaller after the current writeCounter returned by {@link #writeCounter()}.
     *
     * After a {@link io.netty.channel.ChannelFuture} was notified it will be removed from this {@link ChannelFlushPromiseNotifier} and
     * so not receive anymore notification.
     */

    /**
     * 通知所有通过 {@link #add(io.netty.channel.ChannelPromise, int)} 注册的 {@link io.netty.channel.ChannelFuture}，
     * 并且它们的 pendingDatasize 在当前 {@link #writeCounter()} 返回的 writeCounter 之后变小。
     *
     * 当一个 {@link io.netty.channel.ChannelFuture} 被通知后，它将被从该 {@link ChannelFlushPromiseNotifier} 中移除，
     * 因此不会再接收到任何通知。
     */
    public ChannelFlushPromiseNotifier notifyPromises() {
        notifyPromises0(null);
        return this;
    }

    /**
     * @deprecated use {@link #notifyPromises()}
     */

    /**
     * @deprecated 使用 {@link #notifyPromises()}
     */
    @Deprecated
    public ChannelFlushPromiseNotifier notifyFlushFutures() {
        return notifyPromises();
    }

    /**
     * Notify all {@link io.netty.channel.ChannelFuture}s that were registered with {@link #add(io.netty.channel.ChannelPromise, int)} and
     * their pendingDatasize isis smaller then the current writeCounter returned by {@link #writeCounter()}.
     *
     * After a {@link io.netty.channel.ChannelFuture} was notified it will be removed from this {@link ChannelFlushPromiseNotifier} and
     * so not receive anymore notification.
     *
     * The rest of the remaining {@link io.netty.channel.ChannelFuture}s will be failed with the given {@link Throwable}.
     *
     * So after this operation this {@link ChannelFutureListener} is empty.
     */

    /**
     * 通知所有通过 {@link #add(io.netty.channel.ChannelPromise, int)} 注册的 {@link io.netty.channel.ChannelFuture}，并且它们的 pendingDatasize 小于 {@link #writeCounter()} 返回的当前 writeCounter 的值。
     *
     * 当一个 {@link io.netty.channel.ChannelFuture} 被通知后，它将被从该 {@link ChannelFlushPromiseNotifier} 中移除，因此不会再收到任何通知。
     *
     * 剩余的 {@link io.netty.channel.ChannelFuture} 将会以给定的 {@link Throwable} 失败。
     *
     * 因此，在此操作之后，该 {@link ChannelFutureListener} 将为空。
     */
    public ChannelFlushPromiseNotifier notifyPromises(Throwable cause) {
        notifyPromises();
        for (;;) {
            FlushCheckpoint cp = flushCheckpoints.poll();
            if (cp == null) {
                break;
            }
            if (tryNotify) {
                cp.promise().tryFailure(cause);
            } else {
                cp.promise().setFailure(cause);
            }
        }
        return this;
    }

    /**
     * @deprecated use {@link #notifyPromises(Throwable)}
     */

    /**
     * @deprecated 使用 {@link #notifyPromises(Throwable)}
     */
    @Deprecated
    public ChannelFlushPromiseNotifier notifyFlushFutures(Throwable cause) {
        return notifyPromises(cause);
    }

    /**
     * Notify all {@link io.netty.channel.ChannelFuture}s that were registered with {@link #add(io.netty.channel.ChannelPromise, int)} and
     * their pendingDatasize is smaller then the current writeCounter returned by {@link #writeCounter()} using
     * the given cause1.
     *
     * After a {@link io.netty.channel.ChannelFuture} was notified it will be removed from this {@link ChannelFlushPromiseNotifier} and
     * so not receive anymore notification.
     *
     * The rest of the remaining {@link io.netty.channel.ChannelFuture}s will be failed with the given {@link Throwable}.
     *
     * So after this operation this {@link ChannelFutureListener} is empty.
     *
     * @param cause1    the {@link Throwable} which will be used to fail all of the {@link io.netty.channel.ChannelFuture}s which
     *                  pendingDataSize is smaller then the current writeCounter returned by {@link #writeCounter()}
     * @param cause2    the {@link Throwable} which will be used to fail the remaining {@link ChannelFuture}s
     */

    /**
     * 通知所有通过 {@link #add(io.netty.channel.ChannelPromise, int)} 注册的 {@link io.netty.channel.ChannelFuture}，
     * 并且它们的 pendingDataSize 小于 {@link #writeCounter()} 返回的当前 writeCounter，使用给定的 cause1。
     *
     * 当一个 {@link io.netty.channel.ChannelFuture} 被通知后，它将被从该 {@link ChannelFlushPromiseNotifier} 中移除，
     * 因此不会再收到任何通知。
     *
     * 剩余的 {@link io.netty.channel.ChannelFuture} 将使用给定的 {@link Throwable} 失败。
     *
     * 因此，在此操作之后，该 {@link ChannelFutureListener} 为空。
     *
     * @param cause1    用于使所有 pendingDataSize 小于 {@link #writeCounter()} 返回的当前 writeCounter 的
     *                  {@link io.netty.channel.ChannelFuture} 失败的 {@link Throwable}
     * @param cause2    用于使剩余的 {@link ChannelFuture} 失败的 {@link Throwable}
     */
    public ChannelFlushPromiseNotifier notifyPromises(Throwable cause1, Throwable cause2) {
        notifyPromises0(cause1);
        for (;;) {
            FlushCheckpoint cp = flushCheckpoints.poll();
            if (cp == null) {
                break;
            }
            if (tryNotify) {
                cp.promise().tryFailure(cause2);
            } else {
                cp.promise().setFailure(cause2);
            }
        }
        return this;
    }

    /**
     * @deprecated use {@link #notifyPromises(Throwable, Throwable)}
     */

    /**
     * @deprecated 使用 {@link #notifyPromises(Throwable, Throwable)}
     */
    @Deprecated
    public ChannelFlushPromiseNotifier notifyFlushFutures(Throwable cause1, Throwable cause2) {
        return notifyPromises(cause1, cause2);
    }

    private void notifyPromises0(Throwable cause) {
        if (flushCheckpoints.isEmpty()) {
            writeCounter = 0;
            return;
        }

        final long writeCounter = this.writeCounter;
        for (;;) {
            FlushCheckpoint cp = flushCheckpoints.peek();
            if (cp == null) {
                // Reset the counter if there's nothing in the notification list.
                // 如果通知列表为空，重置计数器。
                this.writeCounter = 0;
                break;
            }

            if (cp.flushCheckpoint() > writeCounter) {
                if (writeCounter > 0 && flushCheckpoints.size() == 1) {
                    this.writeCounter = 0;
                    cp.flushCheckpoint(cp.flushCheckpoint() - writeCounter);
                }
                break;
            }

            flushCheckpoints.remove();
            ChannelPromise promise = cp.promise();
            if (cause == null) {
                if (tryNotify) {
                    promise.trySuccess();
                } else {
                    promise.setSuccess();
                }
            } else {
                if (tryNotify) {
                    promise.tryFailure(cause);
                } else {
                    promise.setFailure(cause);
                }
            }
        }

        // Avoid overflow

        // 避免溢出
        final long newWriteCounter = this.writeCounter;
        if (newWriteCounter >= 0x8000000000L) {
            // Reset the counter only when the counter grew pretty large
            // 仅在计数器增长到相当大时重置计数器
            // so that we can reduce the cost of updating all entries in the notification list.
            // 这样我们就可以减少更新通知列表中所有条目的成本。
            this.writeCounter = 0;
            for (FlushCheckpoint cp: flushCheckpoints) {
                cp.flushCheckpoint(cp.flushCheckpoint() - newWriteCounter);
            }
        }
    }

    interface FlushCheckpoint {
        long flushCheckpoint();
        void flushCheckpoint(long checkpoint);
        ChannelPromise promise();
    }

    private static class DefaultFlushCheckpoint implements FlushCheckpoint {
        private long checkpoint;
        private final ChannelPromise future;

        DefaultFlushCheckpoint(long checkpoint, ChannelPromise future) {
            this.checkpoint = checkpoint;
            this.future = future;
        }

        @Override
        public long flushCheckpoint() {
            return checkpoint;
        }

        @Override
        public void flushCheckpoint(long checkpoint) {
            this.checkpoint = checkpoint;
        }

        @Override
        public ChannelPromise promise() {
            return future;
        }
    }
}
