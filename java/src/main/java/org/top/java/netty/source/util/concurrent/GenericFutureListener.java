
package org.top.java.netty.source.util.concurrent;

import java.util.EventListener;

/**
 * Listens to the result of a {@link Future}.  The result of the asynchronous operation is notified once this listener
 * is added by calling {@link Future#addListener(GenericFutureListener)}.
 */

/**
 * 监听 {@link Future} 的结果。一旦通过调用 {@link Future#addListener(GenericFutureListener)} 添加此监听器，异步操作的结果将被通知。
 */
public interface GenericFutureListener<F extends Future<?>> extends EventListener {

    /**
     * Invoked when the operation associated with the {@link Future} has been completed.
     *
     * @param future  the source {@link Future} which called this callback
     */

    /**
     * 当与 {@link Future} 关联的操作完成时调用。
     *
     * @param future  调用此回调的源 {@link Future}
     */
    void operationComplete(F future) throws Exception;
}
