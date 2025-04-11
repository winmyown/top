
package org.top.java.netty.source.util.internal.logging;

import org.apache.log4j.Logger;

/**
 * Logger factory which creates an
 * <a href="https://logging.apache.org/log4j/1.2/index.html">Apache Log4J</a>
 * logger.
 */

/**
 * 日志工厂，用于创建
 * <a href="https://logging.apache.org/log4j/1.2/index.html">Apache Log4J</a>
 * 日志记录器。
 */
public class Log4JLoggerFactory extends InternalLoggerFactory {

    public static final InternalLoggerFactory INSTANCE = new Log4JLoggerFactory();

    /**
     * @deprecated Use {@link #INSTANCE} instead.
     */

    /**
     * @deprecated 请使用 {@link #INSTANCE} 代替。
     */
    @Deprecated
    public Log4JLoggerFactory() {
    }

    @Override
    public InternalLogger newInstance(String name) {
        return new Log4JLogger(Logger.getLogger(name));
    }
}
