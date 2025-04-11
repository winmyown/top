
package org.top.java.netty.source.util.concurrent;

/**
 * An {@link IllegalStateException} which is raised when a user performed a blocking operation
 * when the user is in an event loop thread.  If a blocking operation is performed in an event loop
 * thread, the blocking operation will most likely enter a dead lock state, hence throwing this
 * exception.
 */

/**
 * 一个{@link IllegalStateException}，当用户在事件循环线程中执行阻塞操作时抛出。如果在事件循环线程中执行阻塞操作，
 * 阻塞操作很可能会进入死锁状态，因此抛出此异常。
 */
public class BlockingOperationException extends IllegalStateException {

    private static final long serialVersionUID = 2462223247762460301L;

    public BlockingOperationException() { }

    public BlockingOperationException(String s) {
        super(s);
    }

    public BlockingOperationException(Throwable cause) {
        super(cause);
    }

    public BlockingOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
