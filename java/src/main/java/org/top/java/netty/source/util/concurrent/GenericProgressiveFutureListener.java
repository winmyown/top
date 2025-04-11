

package org.top.java.netty.source.util.concurrent;

public interface GenericProgressiveFutureListener<F extends ProgressiveFuture<?>> extends GenericFutureListener<F> {
    /**
     * Invoked when the operation has progressed.
     *
     * @param progress the progress of the operation so far (cumulative)
     * @param total the number that signifies the end of the operation when {@code progress} reaches at it.
     *              {@code -1} if the end of operation is unknown.
     */
    /**
     * 当操作有进展时调用。
     *
     * @param progress 操作到目前的进度（累计）
     * @param total 表示操作结束的数字，当 {@code progress} 达到该值时操作结束。
     *              如果操作结束未知，则为 {@code -1}。
     */
    void operationProgressed(F future, long progress, long total) throws Exception;
}
