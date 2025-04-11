
package org.top.java.netty.source.util.internal.logging;


import org.apache.commons.logging.LogFactory;

/**
 * Logger factory which creates an
 * <a href="https://commons.apache.org/logging/">Apache Commons Logging</a>
 * logger.
 *
 * @deprecated Please use {@link Log4J2LoggerFactory} or {@link Log4JLoggerFactory} or
 * {@link Slf4JLoggerFactory}.
 */

/**
 * 日志记录器工厂，用于创建
 * <a href="https://commons.apache.org/logging/">Apache Commons Logging</a>
 * 日志记录器。
 *
 * @deprecated 请使用 {@link Log4J2LoggerFactory} 或 {@link Log4JLoggerFactory} 或
 * {@link Slf4JLoggerFactory}。
 */
@Deprecated
public class CommonsLoggerFactory extends InternalLoggerFactory {

    public static final InternalLoggerFactory INSTANCE = new CommonsLoggerFactory();

    /**
     * @deprecated Use {@link #INSTANCE} instead.
     */

    /**
     * @deprecated 请使用 {@link #INSTANCE} 代替。
     */
    @Deprecated
    public CommonsLoggerFactory() {
    }

    @Override
    public InternalLogger newInstance(String name) {
        return new CommonsLogger(LogFactory.getLog(name), name);
    }
}
