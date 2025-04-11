
/**
 * Copyright (c) 2004-2011 QOS.ch
 * All rights reserved.
 *
 * Permission is hereby granted, free  of charge, to any person obtaining
 * a  copy  of this  software  and  associated  documentation files  (the
 * "Software"), to  deal in  the Software without  restriction, including
 * without limitation  the rights to  use, copy, modify,  merge, publish,
 * distribute,  sublicense, and/or sell  copies of  the Software,  and to
 * permit persons to whom the Software  is furnished to do so, subject to
 * the following conditions:
 *
 * The  above  copyright  notice  and  this permission  notice  shall  be
 * included in all copies or substantial portions of the Software.
 *
 * THE  SOFTWARE IS  PROVIDED  "AS  IS", WITHOUT  WARRANTY  OF ANY  KIND,
 * EXPRESS OR  IMPLIED, INCLUDING  BUT NOT LIMITED  TO THE  WARRANTIES OF
 * MERCHANTABILITY,    FITNESS    FOR    A   PARTICULAR    PURPOSE    AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE,  ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

/**
 * 版权所有 (c) 2004-2011 QOS.ch
 * 保留所有权利。
 *
 * 特此授予任何人免费的许可，以获取本软件及其相关文档文件（“软件”）的副本，
 * 并无限制地处理软件，包括但不限于使用、复制、修改、合并、出版、分发、再许可
 * 和/或销售软件的副本，并允许获得软件的人员在遵守以下条件的情况下使用软件：
 *
 * 上述版权声明和本许可声明应包含在软件的所有副本或主要部分中。
 *
 * 本软件按“原样”提供，不提供任何形式的明示或暗示的担保，包括但不限于对适销性、
 * 特定用途适用性和非侵权性的担保。在任何情况下，作者或版权持有人均不对任何索赔、
 * 损害或其他责任负责，无论是在合同、侵权或其他行为中产生的，还是与软件或软件的使用
 * 或其他交易相关的。
 *
 */
package org.top.java.netty.source.util.internal.logging;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * <a href="https://logging.apache.org/log4j/1.2/index.html">Apache Log4J</a>
 * logger.
 */

/**
 * <a href="https://logging.apache.org/log4j/1.2/index.html">Apache Log4J</a>
 * 日志记录器。
 */
class Log4JLogger extends AbstractInternalLogger {

    private static final long serialVersionUID = 2851357342488183058L;

    final transient Logger logger;

    /**
     * Following the pattern discussed in pages 162 through 168 of "The complete
     * log4j manual".
     */

    /**
     * 遵循《The complete log4j manual》第162到168页中讨论的模式。
     */
    static final String FQCN = Log4JLogger.class.getName();

    // Does the log4j version in use recognize the TRACE level?

    // 使用的 log4j 版本是否识别 TRACE 级别？
    // The trace level was introduced in log4j 1.2.12.
    // 跟踪级别在log4j 1.2.12中引入。
    final boolean traceCapable;

    Log4JLogger(Logger logger) {
        super(logger.getName());
        this.logger = logger;
        traceCapable = isTraceCapable();
    }

    private boolean isTraceCapable() {
        try {
            logger.isTraceEnabled();
            return true;
        } catch (NoSuchMethodError ignored) {
            return false;
        }
    }

    /**
     * Is this logger instance enabled for the TRACE level?
     *
     * @return True if this Logger is enabled for level TRACE, false otherwise.
     */

    /**
     * 这个日志记录器实例是否启用了 TRACE 级别？
     *
     * @return 如果这个日志记录器启用了 TRACE 级别，则返回 true，否则返回 false。
     */
    @Override
    public boolean isTraceEnabled() {
        if (traceCapable) {
            return logger.isTraceEnabled();
        } else {
            return logger.isDebugEnabled();
        }
    }

    /**
     * Log a message object at level TRACE.
     *
     * @param msg
     *          - the message object to be logged
     */

    /**
     * 在 TRACE 级别记录消息对象。
     *
     * @param msg
     *          - 要记录的消息对象
     */
    @Override
    public void trace(String msg) {
        logger.log(FQCN, traceCapable ? Level.TRACE : Level.DEBUG, msg, null);
    }

    /**
     * Log a message at level TRACE according to the specified format and
     * argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for level TRACE.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg
     *          the argument
     */

    /**
     * 根据指定的格式和参数记录TRACE级别的日志消息。
     *
     * <p>
     * 当记录器对TRACE级别禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param arg
     *          参数
     */
    @Override
    public void trace(String format, Object arg) {
        if (isTraceEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            logger.log(FQCN, traceCapable ? Level.TRACE : Level.DEBUG, ft
                    .getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level TRACE according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the TRACE level.
     * </p>
     *
     * @param format
     *          the format string
     * @param argA
     *          the first argument
     * @param argB
     *          the second argument
     */

    /**
     * 根据指定的格式和参数记录TRACE级别的消息。
     *
     * <p>
     * 当TRACE级别的日志被禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param argA
     *          第一个参数
     * @param argB
     *          第二个参数
     */
    @Override
    public void trace(String format, Object argA, Object argB) {
        if (isTraceEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            logger.log(FQCN, traceCapable ? Level.TRACE : Level.DEBUG, ft
                    .getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level TRACE according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the TRACE level.
     * </p>
     *
     * @param format
     *          the format string
     * @param arguments
     *          an array of arguments
     */

    /**
     * 根据指定的格式和参数记录 TRACE 级别的消息。
     *
     * <p>
     * 当日志记录器对 TRACE 级别禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param arguments
     *          参数数组
     */
    @Override
    public void trace(String format, Object... arguments) {
        if (isTraceEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            logger.log(FQCN, traceCapable ? Level.TRACE : Level.DEBUG, ft
                    .getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at level TRACE with an accompanying message.
     *
     * @param msg
     *          the message accompanying the exception
     * @param t
     *          the exception (throwable) to log
     */

    /**
     * 在TRACE级别记录一个异常（throwable）并附带一条消息。
     *
     * @param msg
     *          伴随异常的消息
     * @param t
     *          要记录的异常（throwable）
     */
    @Override
    public void trace(String msg, Throwable t) {
        logger.log(FQCN, traceCapable ? Level.TRACE : Level.DEBUG, msg, t);
    }

    /**
     * Is this logger instance enabled for the DEBUG level?
     *
     * @return True if this Logger is enabled for level DEBUG, false otherwise.
     */

    /**
     * 此日志记录器实例是否启用了DEBUG级别？
     *
     * @return 如果此Logger启用了DEBUG级别，则返回true，否则返回false。
     */
    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    /**
     * Log a message object at level DEBUG.
     *
     * @param msg
     *          - the message object to be logged
     */

    /**
     * 在DEBUG级别记录消息对象。
     *
     * @param msg
     *          - 要记录的消息对象
     */
    @Override
    public void debug(String msg) {
        logger.log(FQCN, Level.DEBUG, msg, null);
    }

    /**
     * Log a message at level DEBUG according to the specified format and
     * argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for level DEBUG.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg
     *          the argument
     */

    /**
     * 根据指定的格式和参数记录DEBUG级别的日志消息。
     *
     * <p>
     * 当日志器对DEBUG级别禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param arg
     *          参数
     */
    @Override
    public void debug(String format, Object arg) {
        if (logger.isDebugEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            logger.log(FQCN, Level.DEBUG, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level DEBUG according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the DEBUG level.
     * </p>
     *
     * @param format
     *          the format string
     * @param argA
     *          the first argument
     * @param argB
     *          the second argument
     */

    /**
     * 根据指定的格式和参数记录一条 DEBUG 级别的日志消息。
     *
     * <p>
     * 当日志器对 DEBUG 级别禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param argA
     *          第一个参数
     * @param argB
     *          第二个参数
     */
    @Override
    public void debug(String format, Object argA, Object argB) {
        if (logger.isDebugEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            logger.log(FQCN, Level.DEBUG, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level DEBUG according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the DEBUG level.
     * </p>
     *
     * @param format
     *          the format string
     * @param arguments an array of arguments
     */

    /**
     * 根据指定的格式和参数记录 DEBUG 级别的日志消息。
     *
     * <p>
     * 当日志器在 DEBUG 级别被禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param arguments 参数数组
     */
    @Override
    public void debug(String format, Object... arguments) {
        if (logger.isDebugEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            logger.log(FQCN, Level.DEBUG, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at level DEBUG with an accompanying message.
     *
     * @param msg
     *          the message accompanying the exception
     * @param t
     *          the exception (throwable) to log
     */

    /**
     * 在DEBUG级别记录一个异常（throwable）以及附带的消息。
     *
     * @param msg
     *          伴随异常的消息
     * @param t
     *          要记录的异常（throwable）
     */
    @Override
    public void debug(String msg, Throwable t) {
        logger.log(FQCN, Level.DEBUG, msg, t);
    }

    /**
     * Is this logger instance enabled for the INFO level?
     *
     * @return True if this Logger is enabled for the INFO level, false otherwise.
     */

    /**
     * 此日志记录器实例是否启用了 INFO 级别？
     *
     * @return 如果此日志记录器启用了 INFO 级别，则返回 true，否则返回 false。
     */
    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    /**
     * Log a message object at the INFO level.
     *
     * @param msg
     *          - the message object to be logged
     */

    /**
     * 在INFO级别记录消息对象。
     *
     * @param msg
     *          - 要记录的消息对象
     */
    @Override
    public void info(String msg) {
        logger.log(FQCN, Level.INFO, msg, null);
    }

    /**
     * Log a message at level INFO according to the specified format and argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the INFO level.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg
     *          the argument
     */

    /**
     * 根据指定的格式和参数记录一条INFO级别的消息。
     *
     * <p>
     * 当INFO级别的日志被禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param arg
     *          参数
     */
    @Override
    public void info(String format, Object arg) {
        if (logger.isInfoEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            logger.log(FQCN, Level.INFO, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at the INFO level according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the INFO level.
     * </p>
     *
     * @param format
     *          the format string
     * @param argA
     *          the first argument
     * @param argB
     *          the second argument
     */

    /**
     * 根据指定的格式和参数记录INFO级别的日志消息。
     *
     * <p>
     * 当日志记录器在INFO级别被禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param argA
     *          第一个参数
     * @param argB
     *          第二个参数
     */
    @Override
    public void info(String format, Object argA, Object argB) {
        if (logger.isInfoEnabled()) {
            FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            logger.log(FQCN, Level.INFO, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level INFO according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the INFO level.
     * </p>
     *
     * @param format
     *          the format string
     * @param argArray
     *          an array of arguments
     */

    /**
     * 根据指定的格式和参数记录一条INFO级别的消息。
     *
     * <p>
     * 当记录器在INFO级别被禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param argArray
     *          参数数组
     */
    @Override
    public void info(String format, Object... argArray) {
        if (logger.isInfoEnabled()) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            logger.log(FQCN, Level.INFO, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at the INFO level with an accompanying
     * message.
     *
     * @param msg
     *          the message accompanying the exception
     * @param t
     *          the exception (throwable) to log
     */

    /**
     * 以INFO级别记录一个异常（throwable）并附带消息。
     *
     * @param msg
     *          伴随异常的消息
     * @param t
     *          要记录的异常（throwable）
     */
    @Override
    public void info(String msg, Throwable t) {
        logger.log(FQCN, Level.INFO, msg, t);
    }

    /**
     * Is this logger instance enabled for the WARN level?
     *
     * @return True if this Logger is enabled for the WARN level, false otherwise.
     */

    /**
     * 此日志记录器实例是否启用了 WARN 级别？
     *
     * @return 如果此 Logger 启用了 WARN 级别，则返回 true，否则返回 false。
     */
    @Override
    public boolean isWarnEnabled() {
        return logger.isEnabledFor(Level.WARN);
    }

    /**
     * Log a message object at the WARN level.
     *
     * @param msg
     *          - the message object to be logged
     */

    /**
     * 在 WARN 级别记录消息对象。
     *
     * @param msg
     *          - 要记录的消息对象
     */
    @Override
    public void warn(String msg) {
        logger.log(FQCN, Level.WARN, msg, null);
    }

    /**
     * Log a message at the WARN level according to the specified format and
     * argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the WARN level.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg
     *          the argument
     */

    /**
     * 根据指定的格式和参数记录一条WARN级别的消息。
     *
     * <p>
     * 当日志器对WARN级别禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param arg
     *          参数
     */
    @Override
    public void warn(String format, Object arg) {
        if (logger.isEnabledFor(Level.WARN)) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            logger.log(FQCN, Level.WARN, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at the WARN level according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the WARN level.
     * </p>
     *
     * @param format
     *          the format string
     * @param argA
     *          the first argument
     * @param argB
     *          the second argument
     */

    /**
     * 根据指定的格式和参数记录一条WARN级别的日志消息。
     *
     * <p>
     * 当日志器的WARN级别被禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param argA
     *          第一个参数
     * @param argB
     *          第二个参数
     */
    @Override
    public void warn(String format, Object argA, Object argB) {
        if (logger.isEnabledFor(Level.WARN)) {
            FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            logger.log(FQCN, Level.WARN, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level WARN according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the WARN level.
     * </p>
     *
     * @param format
     *          the format string
     * @param argArray
     *          an array of arguments
     */

    /**
     * 根据指定的格式和参数记录一条 WARN 级别的日志。
     *
     * <p>
     * 当日志器的 WARN 级别被禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param argArray
     *          参数数组
     */
    @Override
    public void warn(String format, Object... argArray) {
        if (logger.isEnabledFor(Level.WARN)) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            logger.log(FQCN, Level.WARN, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at the WARN level with an accompanying
     * message.
     *
     * @param msg
     *          the message accompanying the exception
     * @param t
     *          the exception (throwable) to log
     */

    /**
     * 在WARN级别记录一个异常（throwable）以及附带的消息。
     *
     * @param msg
     *          伴随异常的消息
     * @param t
     *          要记录的异常（throwable）
     */
    @Override
    public void warn(String msg, Throwable t) {
        logger.log(FQCN, Level.WARN, msg, t);
    }

    /**
     * Is this logger instance enabled for level ERROR?
     *
     * @return True if this Logger is enabled for level ERROR, false otherwise.
     */

    /**
     * 此日志记录器实例是否启用了ERROR级别？
     *
     * @return 如果此Logger启用了ERROR级别，则返回true，否则返回false。
     */
    @Override
    public boolean isErrorEnabled() {
        return logger.isEnabledFor(Level.ERROR);
    }

    /**
     * Log a message object at the ERROR level.
     *
     * @param msg
     *          - the message object to be logged
     */

    /**
     * 在ERROR级别记录消息对象。
     *
     * @param msg
     *          - 要记录的消息对象
     */
    @Override
    public void error(String msg) {
        logger.log(FQCN, Level.ERROR, msg, null);
    }

    /**
     * Log a message at the ERROR level according to the specified format and
     * argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the ERROR level.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg
     *          the argument
     */

    /**
     * 根据指定的格式和参数记录 ERROR 级别的消息。
     *
     * <p>
     * 当记录器对 ERROR 级别禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param arg
     *          参数
     */
    @Override
    public void error(String format, Object arg) {
        if (logger.isEnabledFor(Level.ERROR)) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            logger.log(FQCN, Level.ERROR, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at the ERROR level according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the ERROR level.
     * </p>
     *
     * @param format
     *          the format string
     * @param argA
     *          the first argument
     * @param argB
     *          the second argument
     */

    /**
     * 根据指定的格式和参数记录一个ERROR级别的日志消息。
     *
     * <p>
     * 当记录器对ERROR级别禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param argA
     *          第一个参数
     * @param argB
     *          第二个参数
     */
    @Override
    public void error(String format, Object argA, Object argB) {
        if (logger.isEnabledFor(Level.ERROR)) {
            FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            logger.log(FQCN, Level.ERROR, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level ERROR according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the ERROR level.
     * </p>
     *
     * @param format
     *          the format string
     * @param argArray
     *          an array of arguments
     */

    /**
     * 根据指定的格式和参数记录一条ERROR级别的日志消息。
     *
     * <p>
     * 当日志器对ERROR级别禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param argArray
     *          参数数组
     */
    @Override
    public void error(String format, Object... argArray) {
        if (logger.isEnabledFor(Level.ERROR)) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            logger.log(FQCN, Level.ERROR, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at the ERROR level with an accompanying
     * message.
     *
     * @param msg
     *          the message accompanying the exception
     * @param t
     *          the exception (throwable) to log
     */

    /**
     * 以 ERROR 级别记录异常（throwable）及其附带的消息。
     *
     * @param msg
     *          伴随异常的消息
     * @param t
     *          要记录的异常（throwable）
     */
    @Override
    public void error(String msg, Throwable t) {
        logger.log(FQCN, Level.ERROR, msg, t);
    }
}
