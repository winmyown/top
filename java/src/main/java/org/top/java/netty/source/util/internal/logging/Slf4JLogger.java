
package org.top.java.netty.source.util.internal.logging;

import org.slf4j.Logger;

/**
 * <a href="https://www.slf4j.org/">SLF4J</a> logger.
 */

/**
 * <a href="https://www.slf4j.org/">SLF4J</a> 日志记录器。
 */
final class Slf4JLogger extends AbstractInternalLogger {

    private static final long serialVersionUID = 108038972685130825L;

    private final transient Logger logger;

    Slf4JLogger(Logger logger) {
        super(logger.getName());
        this.logger = logger;
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public void trace(String msg) {
        logger.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        logger.trace(format, arg);
    }

    @Override
    public void trace(String format, Object argA, Object argB) {
        logger.trace(format, argA, argB);
    }

    @Override
    public void trace(String format, Object... argArray) {
        logger.trace(format, argArray);
    }

    @Override
    public void trace(String msg, Throwable t) {
        logger.trace(msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        logger.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        logger.debug(format, arg);
    }

    @Override
    public void debug(String format, Object argA, Object argB) {
        logger.debug(format, argA, argB);
    }

    @Override
    public void debug(String format, Object... argArray) {
        logger.debug(format, argArray);
    }

    @Override
    public void debug(String msg, Throwable t) {
        logger.debug(msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        logger.info(format, arg);
    }

    @Override
    public void info(String format, Object argA, Object argB) {
        logger.info(format, argA, argB);
    }

    @Override
    public void info(String format, Object... argArray) {
        logger.info(format, argArray);
    }

    @Override
    public void info(String msg, Throwable t) {
        logger.info(msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        logger.warn(format, arg);
    }

    @Override
    public void warn(String format, Object... argArray) {
        logger.warn(format, argArray);
    }

    @Override
    public void warn(String format, Object argA, Object argB) {
        logger.warn(format, argA, argB);
    }

    @Override
    public void warn(String msg, Throwable t) {
        logger.warn(msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    @Override
    public void error(String msg) {
        logger.error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        logger.error(format, arg);
    }

    @Override
    public void error(String format, Object argA, Object argB) {
        logger.error(format, argA, argB);
    }

    @Override
    public void error(String format, Object... argArray) {
        logger.error(format, argArray);
    }

    @Override
    public void error(String msg, Throwable t) {
        logger.error(msg, t);
    }
}
