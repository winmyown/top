
package org.top.java.netty.source.util.internal.logging;


import java.util.logging.Logger;

/**
 * Logger factory which creates a
 * <a href="https://docs.oracle.com/javase/7/docs/technotes/guides/logging/">java.util.logging</a>
 * logger.
 */

/**
 * 日志记录器工厂，用于创建
 * <a href="https://docs.oracle.com/javase/7/docs/technotes/guides/logging/">java.util.logging</a>
 * 日志记录器。
 */
public class JdkLoggerFactory extends InternalLoggerFactory {

    public static final InternalLoggerFactory INSTANCE = new JdkLoggerFactory();

    /**
     * @deprecated Use {@link #INSTANCE} instead.
     */

    /**
     * @deprecated 请使用 {@link #INSTANCE} 代替。
     */
    @Deprecated
    public JdkLoggerFactory() {
    }

    @Override
    public InternalLogger newInstance(String name) {
        return new JdkLogger(Logger.getLogger(name));
    }
}
