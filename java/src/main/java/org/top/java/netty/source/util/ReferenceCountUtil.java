
package org.top.java.netty.source.util;

import org.top.java.netty.source.util.internal.ObjectUtil;
import org.top.java.netty.source.util.internal.StringUtil;
import org.top.java.netty.source.util.internal.logging.InternalLogger;
import org.top.java.netty.source.util.internal.logging.InternalLoggerFactory;

/**
 * Collection of method to handle objects that may implement {@link ReferenceCounted}.
 */

/**
 * 用于处理可能实现 {@link ReferenceCounted} 的对象的工具方法集合。
 */
public final class ReferenceCountUtil {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ReferenceCountUtil.class);

    static {
        ResourceLeakDetector.addExclusions(ReferenceCountUtil.class, "touch");
    }

    /**
     * Try to call {@link ReferenceCounted#retain()} if the specified message implements {@link ReferenceCounted}.
     * If the specified message doesn't implement {@link ReferenceCounted}, this method does nothing.
     */

    /**
     * 尝试调用 {@link ReferenceCounted#retain()}，如果指定的消息实现了 {@link ReferenceCounted}。
     * 如果指定的消息没有实现 {@link ReferenceCounted}，此方法不执行任何操作。
     */
    @SuppressWarnings("unchecked")
    public static <T> T retain(T msg) {
        if (msg instanceof ReferenceCounted) {
            return (T) ((ReferenceCounted) msg).retain();
        }
        return msg;
    }

    /**
     * Try to call {@link ReferenceCounted#retain(int)} if the specified message implements {@link ReferenceCounted}.
     * If the specified message doesn't implement {@link ReferenceCounted}, this method does nothing.
     */

    /**
     * 尝试调用 {@link ReferenceCounted#retain(int)}，如果指定的消息实现了 {@link ReferenceCounted}。
     * 如果指定的消息没有实现 {@link ReferenceCounted}，此方法不执行任何操作。
     */
    @SuppressWarnings("unchecked")
    public static <T> T retain(T msg, int increment) {
        ObjectUtil.checkPositive(increment, "increment");
        if (msg instanceof ReferenceCounted) {
            return (T) ((ReferenceCounted) msg).retain(increment);
        }
        return msg;
    }

    /**
     * Tries to call {@link ReferenceCounted#touch()} if the specified message implements {@link ReferenceCounted}.
     * If the specified message doesn't implement {@link ReferenceCounted}, this method does nothing.
     */

    /**
     * 尝试调用 {@link ReferenceCounted#touch()}，如果指定的消息实现了 {@link ReferenceCounted}。
     * 如果指定的消息没有实现 {@link ReferenceCounted}，则此方法不执行任何操作。
     */
    @SuppressWarnings("unchecked")
    public static <T> T touch(T msg) {
        if (msg instanceof ReferenceCounted) {
            return (T) ((ReferenceCounted) msg).touch();
        }
        return msg;
    }

    /**
     * Tries to call {@link ReferenceCounted#touch(Object)} if the specified message implements
     * {@link ReferenceCounted}.  If the specified message doesn't implement {@link ReferenceCounted},
     * this method does nothing.
     */

    /**
     * 尝试调用 {@link ReferenceCounted#touch(Object)}，如果指定的消息实现了
     * {@link ReferenceCounted}。如果指定的消息没有实现 {@link ReferenceCounted}，
     * 则此方法不执行任何操作。
     */
    @SuppressWarnings("unchecked")
    public static <T> T touch(T msg, Object hint) {
        if (msg instanceof ReferenceCounted) {
            return (T) ((ReferenceCounted) msg).touch(hint);
        }
        return msg;
    }

    /**
     * Try to call {@link ReferenceCounted#release()} if the specified message implements {@link ReferenceCounted}.
     * If the specified message doesn't implement {@link ReferenceCounted}, this method does nothing.
     */

    /**
     * 如果指定的消息实现了 {@link ReferenceCounted}，则尝试调用 {@link ReferenceCounted#release()}。
     * 如果指定的消息没有实现 {@link ReferenceCounted}，则此方法不执行任何操作。
     */
    public static boolean release(Object msg) {
        if (msg instanceof ReferenceCounted) {
            return ((ReferenceCounted) msg).release();
        }
        return false;
    }

    /**
     * Try to call {@link ReferenceCounted#release(int)} if the specified message implements {@link ReferenceCounted}.
     * If the specified message doesn't implement {@link ReferenceCounted}, this method does nothing.
     */

    /**
     * 尝试调用 {@link ReferenceCounted#release(int)}，如果指定的消息实现了 {@link ReferenceCounted}。
     * 如果指定的消息没有实现 {@link ReferenceCounted}，此方法不执行任何操作。
     */
    public static boolean release(Object msg, int decrement) {
        ObjectUtil.checkPositive(decrement, "decrement");
        if (msg instanceof ReferenceCounted) {
            return ((ReferenceCounted) msg).release(decrement);
        }
        return false;
    }

    /**
     * Try to call {@link ReferenceCounted#release()} if the specified message implements {@link ReferenceCounted}.
     * If the specified message doesn't implement {@link ReferenceCounted}, this method does nothing.
     * Unlike {@link #release(Object)} this method catches an exception raised by {@link ReferenceCounted#release()}
     * and logs it, rather than rethrowing it to the caller.  It is usually recommended to use {@link #release(Object)}
     * instead, unless you absolutely need to swallow an exception.
     */

    /**
     * 尝试调用 {@link ReferenceCounted#release()}，如果指定的消息实现了 {@link ReferenceCounted}。
     * 如果指定的消息没有实现 {@link ReferenceCounted}，此方法不执行任何操作。
     * 与 {@link #release(Object)} 不同，此方法捕获 {@link ReferenceCounted#release()} 引发的异常
     * 并记录它，而不是将其重新抛出给调用者。通常建议使用 {@link #release(Object)}，
     * 除非你确实需要吞下异常。
     */
    public static void safeRelease(Object msg) {
        try {
            release(msg);
        } catch (Throwable t) {
            logger.warn("Failed to release a message: {}", msg, t);
        }
    }

    /**
     * Try to call {@link ReferenceCounted#release(int)} if the specified message implements {@link ReferenceCounted}.
     * If the specified message doesn't implement {@link ReferenceCounted}, this method does nothing.
     * Unlike {@link #release(Object)} this method catches an exception raised by {@link ReferenceCounted#release(int)}
     * and logs it, rather than rethrowing it to the caller.  It is usually recommended to use
     * {@link #release(Object, int)} instead, unless you absolutely need to swallow an exception.
     */

    /**
     * 如果指定的消息实现了 {@link ReferenceCounted}，则尝试调用 {@link ReferenceCounted#release(int)}。
     * 如果指定的消息没有实现 {@link ReferenceCounted}，则此方法不执行任何操作。
     * 与 {@link #release(Object)} 不同，此方法会捕获 {@link ReferenceCounted#release(int)} 引发的异常并记录它，而不是将其重新抛出给调用者。
     * 通常建议使用 {@link #release(Object, int)}，除非您绝对需要吞下异常。
     */
    public static void safeRelease(Object msg, int decrement) {
        try {
            ObjectUtil.checkPositive(decrement, "decrement");
            release(msg, decrement);
        } catch (Throwable t) {
            if (logger.isWarnEnabled()) {
                logger.warn("Failed to release a message: {} (decrement: {})", msg, decrement, t);
            }
        }
    }

    /**
     * Schedules the specified object to be released when the caller thread terminates. Note that this operation is
     * intended to simplify reference counting of ephemeral objects during unit tests. Do not use it beyond the
     * intended use case.
     *
     * @deprecated this may introduce a lot of memory usage so it is generally preferable to manually release objects.
     */

    /**
     * 在调用线程终止时安排释放指定的对象。请注意，此操作旨在简化单元测试期间临时对象的引用计数。请勿超出其预期用途使用。
     *
     * @deprecated 这可能会引入大量内存使用，因此通常更倾向于手动释放对象。
     */
    @Deprecated
    public static <T> T releaseLater(T msg) {
        return releaseLater(msg, 1);
    }

    /**
     * Schedules the specified object to be released when the caller thread terminates. Note that this operation is
     * intended to simplify reference counting of ephemeral objects during unit tests. Do not use it beyond the
     * intended use case.
     *
     * @deprecated this may introduce a lot of memory usage so it is generally preferable to manually release objects.
     */

    /**
     * 在调用线程终止时安排释放指定的对象。请注意，此操作旨在简化单元测试期间临时对象的引用计数。请勿超出其预期用途使用。
     *
     * @deprecated 这可能会引入大量内存使用，因此通常更倾向于手动释放对象。
     */
    @Deprecated
    public static <T> T releaseLater(T msg, int decrement) {
        ObjectUtil.checkPositive(decrement, "decrement");
        if (msg instanceof ReferenceCounted) {
            ThreadDeathWatcher.watch(Thread.currentThread(), new ReleasingTask((ReferenceCounted) msg, decrement));
        }
        return msg;
    }

    /**
     * Returns reference count of a {@link ReferenceCounted} object. If object is not type of
     * {@link ReferenceCounted}, {@code -1} is returned.
     */

    /**
     * 返回一个 {@link ReferenceCounted} 对象的引用计数。如果对象不是 {@link ReferenceCounted} 类型，
     * 则返回 {@code -1}。
     */
    public static int refCnt(Object msg) {
        return msg instanceof ReferenceCounted ? ((ReferenceCounted) msg).refCnt() : -1;
    }

    /**
     * Releases the objects when the thread that called {@link #releaseLater(Object)} has been terminated.
     */

    /**
     * 当调用 {@link #releaseLater(Object)} 的线程终止时，释放对象。
     */
    private static final class ReleasingTask implements Runnable {

        private final ReferenceCounted obj;
        private final int decrement;

        ReleasingTask(ReferenceCounted obj, int decrement) {
            this.obj = obj;
            this.decrement = decrement;
        }

        @Override
        public void run() {
            try {
                if (!obj.release(decrement)) {
                    logger.warn("Non-zero refCnt: {}", this);
                } else {
                    logger.debug("Released: {}", this);
                }
            } catch (Exception ex) {
                logger.warn("Failed to release an object: {}", obj, ex);
            }
        }

        @Override
        public String toString() {
            return StringUtil.simpleClassName(obj) + ".release(" + decrement + ") refCnt: " + obj.refCnt();
        }
    }

    private ReferenceCountUtil() { }
}
