package org.top.java.source.concurrent;

/**
 * @Author zack
 * @Description
 * @Date 2024/10/16 下午3:06
 */
/**
 * 在尝试获取因抛出异常而中止的任务结果时抛出的异常。可以使用 {@link #getCause()} 方法检查此异常。
 *
 * @see Future
 * @since 1.5
 * 作者 Doug Lea
 */
public class ExecutionException extends Exception {
    private static final long serialVersionUID = 7830266012832686185L;

    /**
     * 构造一个没有详细消息的 {@code ExecutionException}。
     * 原因未初始化，之后可以通过调用 {@link #initCause(Throwable) initCause} 初始化。
     */
    protected ExecutionException() { }

    /**
     * 构造一个带有指定详细消息的 {@code ExecutionException}。
     * 原因未初始化，之后可以通过调用 {@link #initCause(Throwable) initCause} 初始化。
     *
     * @param message 详细消息
     */
    protected ExecutionException(String message) {
        super(message);
    }

    /**
     * 构造一个带有指定详细消息和原因的 {@code ExecutionException}。
     *
     * @param  message 详细消息
     * @param  cause 原因（保存以供稍后通过 {@link #getCause()} 方法获取）
     */
    public ExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造一个带有指定原因的 {@code ExecutionException}。
     * 详细消息被设置为 {@code (cause == null ? null : cause.toString())}
     * （通常包含 {@code cause} 的类名和详细消息）。
     *
     * @param  cause 原因（保存以供稍后通过 {@link #getCause()} 方法获取）
     */
    public ExecutionException(Throwable cause) {
        super(cause);
    }
}

