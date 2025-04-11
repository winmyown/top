
package org.top.java.netty.source.util.concurrent;

import org.top.java.netty.source.util.internal.PromiseNotificationUtil;
import org.top.java.netty.source.util.internal.logging.InternalLogger;
import org.top.java.netty.source.util.internal.logging.InternalLoggerFactory;

import static io.netty.util.internal.ObjectUtil.checkNotNull;
import static io.netty.util.internal.ObjectUtil.checkNotNullWithIAE;

/**
 * {@link GenericFutureListener} implementation which takes other {@link Promise}s
 * and notifies them on completion.
 *
 * @param <V> the type of value returned by the future
 * @param <F> the type of future
 */

/**
 * {@link GenericFutureListener} 的实现，它接收其他 {@link Promise} 并在完成时通知它们。
 *
 * @param <V> 未来返回值的类型
 * @param <F> 未来的类型
 */
public class PromiseNotifier<V, F extends Future<V>> implements GenericFutureListener<F> {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(PromiseNotifier.class);
    private final Promise<? super V>[] promises;
    private final boolean logNotifyFailure;

    /**
     * Create a new instance.
     *
     * @param promises  the {@link Promise}s to notify once this {@link GenericFutureListener} is notified.
     */

    /**
     * 创建新实例。
     *
     * @param promises  一旦此 {@link GenericFutureListener} 被通知时，需要通知的 {@link Promise}s。
     */
    @SafeVarargs
    public PromiseNotifier(Promise<? super V>... promises) {
        this(true, promises);
    }

    /**
     * Create a new instance.
     *
     * @param logNotifyFailure {@code true} if logging should be done in case notification fails.
     * @param promises  the {@link Promise}s to notify once this {@link GenericFutureListener} is notified.
     */

    /**
     * 创建新实例。
     *
     * @param logNotifyFailure 如果通知失败时应进行日志记录，则为 {@code true}。
     * @param promises 一旦此 {@link GenericFutureListener} 被通知时要通知的 {@link Promise}。
     */
    @SafeVarargs
    public PromiseNotifier(boolean logNotifyFailure, Promise<? super V>... promises) {
        checkNotNull(promises, "promises");
        for (Promise<? super V> promise: promises) {
            checkNotNullWithIAE(promise, "promise");
        }
        this.promises = promises.clone();
        this.logNotifyFailure = logNotifyFailure;
    }

    /**
     * Link the {@link Future} and {@link Promise} such that if the {@link Future} completes the {@link Promise}
     * will be notified. Cancellation is propagated both ways such that if the {@link Future} is cancelled
     * the {@link Promise} is cancelled and vise-versa.
     *
     * @param future    the {@link Future} which will be used to listen to for notifying the {@link Promise}.
     * @param promise   the {@link Promise} which will be notified
     * @param <V>       the type of the value.
     * @param <F>       the type of the {@link Future}
     * @return          the passed in {@link Future}
     */

    /**
     * 将 {@link Future} 和 {@link Promise} 进行链接，使得如果 {@link Future} 完成，{@link Promise}
     * 将会被通知。取消操作会双向传播，即如果 {@link Future} 被取消，{@link Promise} 也会被取消，反之亦然。
     *
     * @param future    用于监听的 {@link Future}，以便通知 {@link Promise}。
     * @param promise   将被通知的 {@link Promise}
     * @param <V>       值的类型。
     * @param <F>       {@link Future} 的类型
     * @return          传入的 {@link Future}
     */
    public static <V, F extends Future<V>> F cascade(final F future, final Promise<? super V> promise) {
        return cascade(true, future, promise);
    }

    /**
     * Link the {@link Future} and {@link Promise} such that if the {@link Future} completes the {@link Promise}
     * will be notified. Cancellation is propagated both ways such that if the {@link Future} is cancelled
     * the {@link Promise} is cancelled and vise-versa.
     *
     * @param logNotifyFailure  {@code true} if logging should be done in case notification fails.
     * @param future            the {@link Future} which will be used to listen to for notifying the {@link Promise}.
     * @param promise           the {@link Promise} which will be notified
     * @param <V>               the type of the value.
     * @param <F>               the type of the {@link Future}
     * @return                  the passed in {@link Future}
     */

    /**
     * 将 {@link Future} 和 {@link Promise} 进行关联，使得如果 {@link Future} 完成，{@link Promise}
     * 将被通知。取消操作会双向传播，即如果 {@link Future} 被取消，{@link Promise} 也会被取消，反之亦然。
     *
     * @param logNotifyFailure  如果通知失败时是否记录日志。
     * @param future            用于监听并通知 {@link Promise} 的 {@link Future}。
     * @param promise           将被通知的 {@link Promise}
     * @param <V>               值的类型。
     * @param <F>               {@link Future} 的类型
     * @return                  传入的 {@link Future}
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <V, F extends Future<V>> F cascade(boolean logNotifyFailure, final F future,
                                                     final Promise<? super V> promise) {
        promise.addListener(new FutureListener() {
            @Override
            public void operationComplete(Future f) {
                if (f.isCancelled()) {
                    future.cancel(false);
                }
            }
        });
        future.addListener(new PromiseNotifier(logNotifyFailure, promise) {
            @Override
            public void operationComplete(Future f) throws Exception {
                if (promise.isCancelled() && f.isCancelled()) {
                    // Just return if we propagate a cancel from the promise to the future and both are notified already
                    // 仅返回是否将取消从promise传播到future，并且两者都已收到通知
                    return;
                }
                super.operationComplete(future);
            }
        });
        return future;
    }

    @Override
    public void operationComplete(F future) throws Exception {
        InternalLogger internalLogger = logNotifyFailure ? logger : null;
        if (future.isSuccess()) {
            V result = future.get();
            for (Promise<? super V> p: promises) {
                PromiseNotificationUtil.trySuccess(p, result, internalLogger);
            }
        } else if (future.isCancelled()) {
            for (Promise<? super V> p: promises) {
                PromiseNotificationUtil.tryCancel(p, internalLogger);
            }
        } else {
            Throwable cause = future.cause();
            for (Promise<? super V> p: promises) {
                PromiseNotificationUtil.tryFailure(p, cause, internalLogger);
            }
        }
    }
}
