
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

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * <a href="https://java.sun.com/javase/6/docs/technotes/guides/logging/index.html">java.util.logging</a>
 * logger.
 */

/**
 * <a href="https://java.sun.com/javase/6/docs/technotes/guides/logging/index.html">java.util.logging</a>
 * 日志记录器。
 */
class JdkLogger extends AbstractInternalLogger {

    private static final long serialVersionUID = -1767272577989225979L;

    final transient Logger logger;

    JdkLogger(Logger logger) {
        super(logger.getName());
        this.logger = logger;
    }

    /**
     * Is this logger instance enabled for the FINEST level?
     *
     * @return True if this Logger is enabled for level FINEST, false otherwise.
     */

    /**
     * 此日志记录器实例是否启用了 FINEST 级别？
     *
     * @return 如果此日志记录器启用了 FINEST 级别，则返回 true，否则返回 false。
     */
    @Override
    public boolean isTraceEnabled() {
        return logger.isLoggable(Level.FINEST);
    }

    /**
     * Log a message object at level FINEST.
     *
     * @param msg
     *          - the message object to be logged
     */

    /**
     * 以 FINEST 级别记录消息对象。
     *
     * @param msg
     *          - 要记录的消息对象
     */
    @Override
    public void trace(String msg) {
        if (logger.isLoggable(Level.FINEST)) {
            log(SELF, Level.FINEST, msg, null);
        }
    }

    /**
     * Log a message at level FINEST according to the specified format and
     * argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for level FINEST.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg
     *          the argument
     */

    /**
     * 根据指定的格式和参数记录一条 FINEST 级别的消息。
     *
     * <p>
     * 当记录器对 FINEST 级别禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param arg
     *          参数
     */
    @Override
    public void trace(String format, Object arg) {
        if (logger.isLoggable(Level.FINEST)) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            log(SELF, Level.FINEST, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level FINEST according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the FINEST level.
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
     * 根据指定的格式和参数记录 FINEST 级别的日志消息。
     *
     * <p>
     * 当 FINEST 级别的日志被禁用时，此形式避免了不必要的对象创建。
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
        if (logger.isLoggable(Level.FINEST)) {
            FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            log(SELF, Level.FINEST, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level FINEST according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the FINEST level.
     * </p>
     *
     * @param format
     *          the format string
     * @param argArray
     *          an array of arguments
     */

    /**
     * 根据指定的格式和参数记录 FINEST 级别的日志消息。
     *
     * <p>
     * 当日志器在 FINEST 级别被禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param argArray
     *          参数数组
     */
    @Override
    public void trace(String format, Object... argArray) {
        if (logger.isLoggable(Level.FINEST)) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            log(SELF, Level.FINEST, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at level FINEST with an accompanying message.
     *
     * @param msg
     *          the message accompanying the exception
     * @param t
     *          the exception (throwable) to log
     */

    /**
     * 以FINEST级别记录异常（throwable）及附带的消息。
     *
     * @param msg
     *          伴随异常的消息
     * @param t
     *          要记录的异常（throwable）
     */
    @Override
    public void trace(String msg, Throwable t) {
        if (logger.isLoggable(Level.FINEST)) {
            log(SELF, Level.FINEST, msg, t);
        }
    }

    /**
     * Is this logger instance enabled for the FINE level?
     *
     * @return True if this Logger is enabled for level FINE, false otherwise.
     */

    /**
     * 这个日志记录器实例是否启用了 FINE 级别？
     *
     * @return 如果这个日志记录器启用了 FINE 级别，则返回 true，否则返回 false。
     */
    @Override
    public boolean isDebugEnabled() {
        return logger.isLoggable(Level.FINE);
    }

    /**
     * Log a message object at level FINE.
     *
     * @param msg
     *          - the message object to be logged
     */

    /**
     * 在 FINE 级别记录消息对象。
     *
     * @param msg
     *          - 要记录的消息对象
     */
    @Override
    public void debug(String msg) {
        if (logger.isLoggable(Level.FINE)) {
            log(SELF, Level.FINE, msg, null);
        }
    }

    /**
     * Log a message at level FINE according to the specified format and argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for level FINE.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg
     *          the argument
     */

    /**
     * 根据指定的格式和参数记录一条 FINE 级别的消息。
     *
     * <p>
     * 当记录器对 FINE 级别禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param arg
     *          参数
     */
    @Override
    public void debug(String format, Object arg) {
        if (logger.isLoggable(Level.FINE)) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            log(SELF, Level.FINE, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level FINE according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the FINE level.
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
     * 根据指定的格式和参数记录一条 FINE 级别的消息。
     *
     * <p>
     * 当记录器对 FINE 级别禁用时，此形式避免了不必要的对象创建。
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
        if (logger.isLoggable(Level.FINE)) {
            FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            log(SELF, Level.FINE, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level FINE according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the FINE level.
     * </p>
     *
     * @param format
     *          the format string
     * @param argArray
     *          an array of arguments
     */

    /**
     * 根据指定的格式和参数记录一条 FINE 级别的消息。
     *
     * <p>
     * 当记录器对 FINE 级别禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param argArray
     *          参数数组
     */
    @Override
    public void debug(String format, Object... argArray) {
        if (logger.isLoggable(Level.FINE)) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            log(SELF, Level.FINE, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at level FINE with an accompanying message.
     *
     * @param msg
     *          the message accompanying the exception
     * @param t
     *          the exception (throwable) to log
     */

    /**
     * 以 FINE 级别记录异常（Throwable）及附带的消息。
     *
     * @param msg
     *          伴随异常的消息
     * @param t
     *          要记录的异常（Throwable）
     */
    @Override
    public void debug(String msg, Throwable t) {
        if (logger.isLoggable(Level.FINE)) {
            log(SELF, Level.FINE, msg, t);
        }
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
        return logger.isLoggable(Level.INFO);
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
        if (logger.isLoggable(Level.INFO)) {
            log(SELF, Level.INFO, msg, null);
        }
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
        if (logger.isLoggable(Level.INFO)) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            log(SELF, Level.INFO, ft.getMessage(), ft.getThrowable());
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
        if (logger.isLoggable(Level.INFO)) {
            FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            log(SELF, Level.INFO, ft.getMessage(), ft.getThrowable());
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
        if (logger.isLoggable(Level.INFO)) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            log(SELF, Level.INFO, ft.getMessage(), ft.getThrowable());
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
        if (logger.isLoggable(Level.INFO)) {
            log(SELF, Level.INFO, msg, t);
        }
    }

    /**
     * Is this logger instance enabled for the WARNING level?
     *
     * @return True if this Logger is enabled for the WARNING level, false
     *         otherwise.
     */

    /**
     * 此日志记录器实例是否启用了 WARNING 级别？
     *
     * @return 如果此日志记录器启用了 WARNING 级别，则返回 true，否则返回 false。
     */
    @Override
    public boolean isWarnEnabled() {
        return logger.isLoggable(Level.WARNING);
    }

    /**
     * Log a message object at the WARNING level.
     *
     * @param msg
     *          - the message object to be logged
     */

    /**
     * 在 WARNING 级别记录消息对象。
     *
     * @param msg
     *          - 要记录的消息对象
     */
    @Override
    public void warn(String msg) {
        if (logger.isLoggable(Level.WARNING)) {
            log(SELF, Level.WARNING, msg, null);
        }
    }

    /**
     * Log a message at the WARNING level according to the specified format and
     * argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the WARNING level.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg
     *          the argument
     */

    /**
     * 根据指定的格式和参数记录一个 WARNING 级别的消息。
     *
     * <p>
     * 当日志记录器对 WARNING 级别禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param arg
     *          参数
     */
    @Override
    public void warn(String format, Object arg) {
        if (logger.isLoggable(Level.WARNING)) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            log(SELF, Level.WARNING, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at the WARNING level according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the WARNING level.
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
     * 根据指定的格式和参数，以 WARNING 级别记录消息。
     *
     * <p>
     * 当日志器对 WARNING 级别禁用时，此形式避免了多余的对象创建。
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
        if (logger.isLoggable(Level.WARNING)) {
            FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            log(SELF, Level.WARNING, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level WARNING according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the WARNING level.
     * </p>
     *
     * @param format
     *          the format string
     * @param argArray
     *          an array of arguments
     */

    /**
     * 根据指定的格式和参数记录一条 WARNING 级别的日志。
     *
     * <p>
     * 当日志器的 WARNING 级别被禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param argArray
     *          参数数组
     */
    @Override
    public void warn(String format, Object... argArray) {
        if (logger.isLoggable(Level.WARNING)) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
            log(SELF, Level.WARNING, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at the WARNING level with an accompanying
     * message.
     *
     * @param msg
     *          the message accompanying the exception
     * @param t
     *          the exception (throwable) to log
     */

    /**
     * 以 WARNING 级别记录异常（throwable）及附带的消息。
     *
     * @param msg
     *          伴随异常的消息
     * @param t
     *          要记录的异常（throwable）
     */
    @Override
    public void warn(String msg, Throwable t) {
        if (logger.isLoggable(Level.WARNING)) {
            log(SELF, Level.WARNING, msg, t);
        }
    }

    /**
     * Is this logger instance enabled for level SEVERE?
     *
     * @return True if this Logger is enabled for level SEVERE, false otherwise.
     */

    /**
     * 这个日志记录器实例是否启用了 SEVERE 级别？
     *
     * @return 如果这个 Logger 启用了 SEVERE 级别，则返回 true，否则返回 false。
     */
    @Override
    public boolean isErrorEnabled() {
        return logger.isLoggable(Level.SEVERE);
    }

    /**
     * Log a message object at the SEVERE level.
     *
     * @param msg
     *          - the message object to be logged
     */

    /**
     * 在SEVERE级别记录消息对象。
     *
     * @param msg
     *          - 要记录的消息对象
     */
    @Override
    public void error(String msg) {
        if (logger.isLoggable(Level.SEVERE)) {
            log(SELF, Level.SEVERE, msg, null);
        }
    }

    /**
     * Log a message at the SEVERE level according to the specified format and
     * argument.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the SEVERE level.
     * </p>
     *
     * @param format
     *          the format string
     * @param arg
     *          the argument
     */

    /**
     * 根据指定的格式和参数记录一条 SEVERE 级别的日志消息。
     *
     * <p>
     * 当记录器对 SEVERE 级别禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param arg
     *          参数
     */
    @Override
    public void error(String format, Object arg) {
        if (logger.isLoggable(Level.SEVERE)) {
            FormattingTuple ft = MessageFormatter.format(format, arg);
            log(SELF, Level.SEVERE, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at the SEVERE level according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the SEVERE level.
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
     * 根据指定的格式和参数记录一条SEVERE级别的消息。
     *
     * <p>
     * 当记录器对SEVERE级别禁用时，此形式避免了不必要的对象创建。
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
        if (logger.isLoggable(Level.SEVERE)) {
            FormattingTuple ft = MessageFormatter.format(format, argA, argB);
            log(SELF, Level.SEVERE, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log a message at level SEVERE according to the specified format and
     * arguments.
     *
     * <p>
     * This form avoids superfluous object creation when the logger is disabled
     * for the SEVERE level.
     * </p>
     *
     * @param format
     *          the format string
     * @param arguments
     *          an array of arguments
     */

    /**
     * 根据指定的格式和参数记录一条 SEVERE 级别的日志消息。
     *
     * <p>
     * 当日志器在 SEVERE 级别被禁用时，此形式避免了不必要的对象创建。
     * </p>
     *
     * @param format
     *          格式字符串
     * @param arguments
     *          参数数组
     */
    @Override
    public void error(String format, Object... arguments) {
        if (logger.isLoggable(Level.SEVERE)) {
            FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
            log(SELF, Level.SEVERE, ft.getMessage(), ft.getThrowable());
        }
    }

    /**
     * Log an exception (throwable) at the SEVERE level with an accompanying
     * message.
     *
     * @param msg
     *          the message accompanying the exception
     * @param t
     *          the exception (throwable) to log
     */

    /**
     * 以 SEVERE 级别记录异常（throwable）并附带消息。
     *
     * @param msg
     *          伴随异常的消息
     * @param t
     *          要记录的异常（throwable）
     */
    @Override
    public void error(String msg, Throwable t) {
        if (logger.isLoggable(Level.SEVERE)) {
            log(SELF, Level.SEVERE, msg, t);
        }
    }

    /**
     * Log the message at the specified level with the specified throwable if any.
     * This method creates a LogRecord and fills in caller date before calling
     * this instance's JDK14 logger.
     *
     * See bug report #13 for more details.
     */

    /**
     * 使用指定的级别记录消息，并附带指定的可抛出对象（如果有）。
     * 此方法创建一个LogRecord，并在调用此实例的JDK14记录器之前填充调用者日期。
     *
     * 有关更多详细信息，请参阅错误报告#13。
     */
    private void log(String callerFQCN, Level level, String msg, Throwable t) {
        // millis and thread are filled by the constructor
        // millis和thread由构造函数填充
        LogRecord record = new LogRecord(level, msg);
        record.setLoggerName(name());
        record.setThrown(t);
        fillCallerData(callerFQCN, record);
        logger.log(record);
    }

    static final String SELF = JdkLogger.class.getName();
    static final String SUPER = AbstractInternalLogger.class.getName();

    /**
     * Fill in caller data if possible.
     *
     * @param record
     *          The record to update
     */

    /**
     * 如果可能，填充调用者数据。
     *
     * @param record
     *          要更新的记录
     */
    private static void fillCallerData(String callerFQCN, LogRecord record) {
        StackTraceElement[] steArray = new Throwable().getStackTrace();

        int selfIndex = -1;
        for (int i = 0; i < steArray.length; i++) {
            final String className = steArray[i].getClassName();
            if (className.equals(callerFQCN) || className.equals(SUPER)) {
                selfIndex = i;
                break;
            }
        }

        int found = -1;
        for (int i = selfIndex + 1; i < steArray.length; i++) {
            final String className = steArray[i].getClassName();
            if (!(className.equals(callerFQCN) || className.equals(SUPER))) {
                found = i;
                break;
            }
        }

        if (found != -1) {
            StackTraceElement ste = steArray[found];
            // setting the class name has the side effect of setting
            // 设置类名具有设置这些的副作用
            // the needToInferCaller variable to false.
            // 将 needToInferCaller 变量设置为 false。
            record.setSourceClassName(ste.getClassName());
            record.setSourceMethodName(ste.getMethodName());
        }
    }
}
