
package org.top.java.netty.source.util.internal;

import org.top.java.netty.source.util.concurrent.Promise;
import org.top.java.netty.source.util.internal.logging.InternalLogger;

/**
 * Internal utilities to notify {@link Promise}s.
 */

/**
 * 用于通知 {@link Promise} 的内部工具。
 */
public final class PromiseNotificationUtil {

    private PromiseNotificationUtil() { }

    /**
     * Try to cancel the {@link Promise} and log if {@code logger} is not {@code null} in case this fails.
     */

    /**
     * 尝试取消 {@link Promise}，如果 {@code logger} 不为 {@code null}，则在失败时记录日志。
     */
    public static void tryCancel(Promise<?> p, InternalLogger logger) {
        if (!p.cancel(false) && logger != null) {
            Throwable err = p.cause();
            if (err == null) {
                logger.warn("Failed to cancel promise because it has succeeded already: {}", p);
            } else {
                logger.warn(
                        "Failed to cancel promise because it has failed already: {}, unnotified cause:",
                        p, err);
            }
        }
    }

    /**
     * Try to mark the {@link Promise} as success and log if {@code logger} is not {@code null} in case this fails.
     */

    /**
     * 尝试将 {@link Promise} 标记为成功，如果失败且 {@code logger} 不为 {@code null}，则记录日志。
     */
    public static <V> void trySuccess(Promise<? super V> p, V result, InternalLogger logger) {
        if (!p.trySuccess(result) && logger != null) {
            Throwable err = p.cause();
            if (err == null) {
                logger.warn("Failed to mark a promise as success because it has succeeded already: {}", p);
            } else {
                logger.warn(
                        "Failed to mark a promise as success because it has failed already: {}, unnotified cause:",
                        p, err);
            }
        }
    }

    /**
     * Try to mark the {@link Promise} as failure and log if {@code logger} is not {@code null} in case this fails.
     */

    /**
     * 尝试将 {@link Promise} 标记为失败，并在 {@code logger} 不为 {@code null} 时记录日志，如果此操作失败。
     */
    public static void tryFailure(Promise<?> p, Throwable cause, InternalLogger logger) {
        if (!p.tryFailure(cause) && logger != null) {
            Throwable err = p.cause();
            if (err == null) {
                logger.warn("Failed to mark a promise as failure because it has succeeded already: {}", p, cause);
            } else if (logger.isWarnEnabled()) {
                logger.warn(
                        "Failed to mark a promise as failure because it has failed already: {}, unnotified cause: {}",
                        p, ThrowableUtil.stackTraceToString(err), cause);
            }
        }
    }

}
