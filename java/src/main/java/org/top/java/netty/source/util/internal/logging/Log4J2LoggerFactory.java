
package org.top.java.netty.source.util.internal.logging;

import org.apache.logging.log4j.LogManager;

public final class Log4J2LoggerFactory extends InternalLoggerFactory {

    public static final InternalLoggerFactory INSTANCE = new Log4J2LoggerFactory();

    /**
     * @deprecated Use {@link #INSTANCE} instead.
     */

    /**
     * @deprecated 请使用 {@link #INSTANCE} 代替。
     */
    @Deprecated
    public Log4J2LoggerFactory() {
    }

    @Override
    public InternalLogger newInstance(String name) {
        return new Log4J2Logger(LogManager.getLogger(name));
    }
}
