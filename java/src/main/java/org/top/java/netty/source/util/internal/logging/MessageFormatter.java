
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

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;

// contributors: lizongbo: proposed special treatment of array parameter values

// 贡献者：lizongbo：建议对数组参数值进行特殊处理
// Joern Huxhorn: pointed out double[] omission, suggested deep array copy
// Joern Huxhorn: 指出了 double[] 的遗漏，建议进行深拷贝数组

/**
 * Formats messages according to very simple substitution rules. Substitutions
 * can be made 1, 2 or more arguments.
 * <p/>
 * <p/>
 * For example,
 * <p/>
 * <pre>
 * MessageFormatter.format(&quot;Hi {}.&quot;, &quot;there&quot;)
 * </pre>
 * <p/>
 * will return the string "Hi there.".
 * <p/>
 * The {} pair is called the <em>formatting anchor</em>. It serves to designate
 * the location where arguments need to be substituted within the message
 * pattern.
 * <p/>
 * In case your message contains the '{' or the '}' character, you do not have
 * to do anything special unless the '}' character immediately follows '{'. For
 * example,
 * <p/>
 * <pre>
 * MessageFormatter.format(&quot;Set {1,2,3} is not equal to {}.&quot;, &quot;1,2&quot;);
 * </pre>
 * <p/>
 * will return the string "Set {1,2,3} is not equal to 1,2.".
 * <p/>
 * <p/>
 * If for whatever reason you need to place the string "{}" in the message
 * without its <em>formatting anchor</em> meaning, then you need to escape the
 * '{' character with '', that is the backslash character. Only the '{'
 * character should be escaped. There is no need to escape the '}' character.
 * For example,
 * <p/>
 * <pre>
 * MessageFormatter.format(&quot;Set \{} is not equal to {}.&quot;, &quot;1,2&quot;);
 * </pre>
 * <p/>
 * will return the string "Set {} is not equal to 1,2.".
 * <p/>
 * <p/>
 * The escaping behavior just described can be overridden by escaping the escape
 * character ''. Calling
 * <p/>
 * <pre>
 * MessageFormatter.format(&quot;File name is C:\\{}.&quot;, &quot;file.zip&quot;);
 * </pre>
 * <p/>
 * will return the string "File name is C:file.zip".
 * <p/>
 * <p/>
 * The formatting conventions are different than those of {@link MessageFormat}
 * which ships with the Java platform. This is justified by the fact that
 * SLF4J's implementation is 10 times faster than that of {@link MessageFormat}.
 * This local performance difference is both measurable and significant in the
 * larger context of the complete logging processing chain.
 * <p/>
 * <p/>
 * See also {@link #format(String, Object)},
 * {@link #format(String, Object, Object)} and
 * {@link #arrayFormat(String, Object[])} methods for more details.
 */

/**
 * 根据非常简单的替换规则格式化消息。替换可以使用1个、2个或多个参数。
 * <p/>
 * <p/>
 * 例如，
 * <p/>
 * <pre>
 * MessageFormatter.format(&quot;Hi {}.&quot;, &quot;there&quot;)
 * </pre>
 * <p/>
 * 将返回字符串 "Hi there."。
 * <p/>
 * {} 对被称为<em>格式化锚点</em>。它用于指定在消息模式中需要替换参数的位置。
 * <p/>
 * 如果您的消息中包含 '{' 或 '}' 字符，您不需要做任何特殊处理，除非 '}' 字符紧跟在 '{' 之后。例如，
 * <p/>
 * <pre>
 * MessageFormatter.format(&quot;Set {1,2,3} is not equal to {}.&quot;, &quot;1,2&quot;);
 * </pre>
 * <p/>
 * 将返回字符串 "Set {1,2,3} is not equal to 1,2."。
 * <p/>
 * <p/>
 * 如果出于某种原因您需要在消息中放置字符串 "{}" 而不使用其<em>格式化锚点</em>的含义，那么您需要使用 '\' 转义 '{' 字符，即反斜杠字符。只有 '{' 字符需要转义。不需要转义 '}' 字符。例如，
 * <p/>
 * <pre>
 * MessageFormatter.format(&quot;Set \\{} is not equal to {}.&quot;, &quot;1,2&quot;);
 * </pre>
 * <p/>
 * 将返回字符串 "Set {} is not equal to 1,2."。
 * <p/>
 * <p/>
 * 上述的转义行为可以通过转义转义字符 '\' 来覆盖。调用
 * <p/>
 * <pre>
 * MessageFormatter.format(&quot;File name is C:\\\\{}.&quot;, &quot;file.zip&quot;);
 * </pre>
 * <p/>
 * 将返回字符串 "File name is C:\file.zip"。
 * <p/>
 * <p/>
 * 格式化约定与 Java 平台自带的 {@link MessageFormat} 不同。这由 SLF4J 的实现比 {@link MessageFormat} 快 10 倍的事实所证明。这种局部性能差异在完整的日志处理链的更大背景下是可测量且显著的。
 * <p/>
 * <p/>
 * 另请参阅 {@link #format(String, Object)},
 * {@link #format(String, Object, Object)} 和
 * {@link #arrayFormat(String, Object[])} 方法以获取更多详细信息。
 */
public final class MessageFormatter {
    private static final String DELIM_STR = "{}";
    private static final char ESCAPE_CHAR = '\\';

    /**
     * Performs single argument substitution for the 'messagePattern' passed as
     * parameter.
     * <p/>
     * For example,
     * <p/>
     * <pre>
     * MessageFormatter.format(&quot;Hi {}.&quot;, &quot;there&quot;);
     * </pre>
     * <p/>
     * will return the string "Hi there.".
     * <p/>
     *
     * @param messagePattern The message pattern which will be parsed and formatted
     * @param arg            The argument to be substituted in place of the formatting anchor
     * @return The formatted message
     */

    /**
     * 对传入的 'messagePattern' 参数执行单参数替换。
     * <p/>
     * 例如，
     * <p/>
     * <pre>
     * MessageFormatter.format(&quot;Hi {}.&quot;, &quot;there&quot;);
     * </pre>
     * <p/>
     * 将返回字符串 "Hi there."。
     * <p/>
     *
     * @param messagePattern 将被解析和格式化的消息模式
     * @param arg            将被替换到格式化锚点位置的参数
     * @return 格式化后的消息
     */
    public static FormattingTuple format(String messagePattern, Object arg) {
        return arrayFormat(messagePattern, new Object[]{arg});
    }

    /**
     * Performs a two argument substitution for the 'messagePattern' passed as
     * parameter.
     * <p/>
     * For example,
     * <p/>
     * <pre>
     * MessageFormatter.format(&quot;Hi {}. My name is {}.&quot;, &quot;Alice&quot;, &quot;Bob&quot;);
     * </pre>
     * <p/>
     * will return the string "Hi Alice. My name is Bob.".
     *
     * @param messagePattern The message pattern which will be parsed and formatted
     * @param argA           The argument to be substituted in place of the first formatting
     *                       anchor
     * @param argB           The argument to be substituted in place of the second formatting
     *                       anchor
     * @return The formatted message
     */

    /**
     * 对传入的 'messagePattern' 执行两个参数的替换。
     * <p/>
     * 例如，
     * <p/>
     * <pre>
     * MessageFormatter.format(&quot;Hi {}. My name is {}.&quot;, &quot;Alice&quot;, &quot;Bob&quot;);
     * </pre>
     * <p/>
     * 将返回字符串 "Hi Alice. My name is Bob."。
     *
     * @param messagePattern 将被解析和格式化的消息模式
     * @param argA           将替换第一个格式化锚点的参数
     * @param argB           将替换第二个格式化锚点的参数
     * @return 格式化后的消息
     */
    public static FormattingTuple format(final String messagePattern,
                                         Object argA, Object argB) {
        return arrayFormat(messagePattern, new Object[]{argA, argB});
    }

    /**
     * Same principle as the {@link #format(String, Object)} and
     * {@link #format(String, Object, Object)} methods except that any number of
     * arguments can be passed in an array.
     *
     * @param messagePattern The message pattern which will be parsed and formatted
     * @param argArray       An array of arguments to be substituted in place of formatting
     *                       anchors
     * @return The formatted message
     */

    /**
     * 与 {@link #format(String, Object)} 和
     * {@link #format(String, Object, Object)} 方法原理相同，只是可以将任意数量的
     * 参数以数组形式传递。
     *
     * @param messagePattern 将被解析和格式化的消息模式
     * @param argArray       用于替换格式化锚点的参数数组
     * @return 格式化后的消息
     */
    public static FormattingTuple arrayFormat(final String messagePattern,
                                              final Object[] argArray) {
        if (argArray == null || argArray.length == 0) {
            return new FormattingTuple(messagePattern, null);
        }

        int lastArrIdx = argArray.length - 1;
        Object lastEntry = argArray[lastArrIdx];
        Throwable throwable = lastEntry instanceof Throwable? (Throwable) lastEntry : null;

        if (messagePattern == null) {
            return new FormattingTuple(null, throwable);
        }

        int j = messagePattern.indexOf(DELIM_STR);
        if (j == -1) {
            // this is a simple string
            // 这是一个简单的字符串
            return new FormattingTuple(messagePattern, throwable);
        }

        StringBuilder sbuf = new StringBuilder(messagePattern.length() + 50);
        int i = 0;
        int L = 0;
        do {
            boolean notEscaped = j == 0 || messagePattern.charAt(j - 1) != ESCAPE_CHAR;
            if (notEscaped) {
                // normal case
                // 正常情况
                sbuf.append(messagePattern, i, j);
            } else {
                sbuf.append(messagePattern, i, j - 1);
                // check that escape char is not is escaped: "abc x:\{}"
                // 检查转义字符是否未被转义: "abc x:\\{}"
                notEscaped = j >= 2 && messagePattern.charAt(j - 2) == ESCAPE_CHAR;
            }

            i = j + 2;
            if (notEscaped) {
                deeplyAppendParameter(sbuf, argArray[L], null);
                L++;
                if (L > lastArrIdx) {
                    break;
                }
            } else {
                sbuf.append(DELIM_STR);
            }
            j = messagePattern.indexOf(DELIM_STR, i);
        } while (j != -1);

        // append the characters following the last {} pair.

        // 在最后一个{}对之后追加字符。
        sbuf.append(messagePattern, i, messagePattern.length());
        return new FormattingTuple(sbuf.toString(), L <= lastArrIdx? throwable : null);
    }

    // special treatment of array values was suggested by 'lizongbo'

    // 数组值的特殊处理由 'lizongbo' 建议
    private static void deeplyAppendParameter(StringBuilder sbuf, Object o,
                                              Set<Object[]> seenSet) {
        if (o == null) {
            sbuf.append("null");
            return;
        }
        Class<?> objClass = o.getClass();
        if (!objClass.isArray()) {
            if (Number.class.isAssignableFrom(objClass)) {
                // Prevent String instantiation for some number types
                // 防止某些数字类型的字符串实例化
                if (objClass == Long.class) {
                    sbuf.append(((Long) o).longValue());
                } else if (objClass == Integer.class || objClass == Short.class || objClass == Byte.class) {
                    sbuf.append(((Number) o).intValue());
                } else if (objClass == Double.class) {
                    sbuf.append(((Double) o).doubleValue());
                } else if (objClass == Float.class) {
                    sbuf.append(((Float) o).floatValue());
                } else {
                    safeObjectAppend(sbuf, o);
                }
            } else {
                safeObjectAppend(sbuf, o);
            }
        } else {
            // check for primitive array types because they
            // 检查原始数组类型，因为它们
            // unfortunately cannot be cast to Object[]
            // 不幸的是，无法转换为 Object[]
            sbuf.append('[');
            if (objClass == boolean[].class) {
                booleanArrayAppend(sbuf, (boolean[]) o);
            } else if (objClass == byte[].class) {
                byteArrayAppend(sbuf, (byte[]) o);
            } else if (objClass == char[].class) {
                charArrayAppend(sbuf, (char[]) o);
            } else if (objClass == short[].class) {
                shortArrayAppend(sbuf, (short[]) o);
            } else if (objClass == int[].class) {
                intArrayAppend(sbuf, (int[]) o);
            } else if (objClass == long[].class) {
                longArrayAppend(sbuf, (long[]) o);
            } else if (objClass == float[].class) {
                floatArrayAppend(sbuf, (float[]) o);
            } else if (objClass == double[].class) {
                doubleArrayAppend(sbuf, (double[]) o);
            } else {
                objectArrayAppend(sbuf, (Object[]) o, seenSet);
            }
            sbuf.append(']');
        }
    }

    private static void safeObjectAppend(StringBuilder sbuf, Object o) {
        try {
            String oAsString = o.toString();
            sbuf.append(oAsString);
        } catch (Throwable t) {
            System.err
                    .println("SLF4J: Failed toString() invocation on an object of type ["
                            + o.getClass().getName() + ']');
            t.printStackTrace();
            sbuf.append("[FAILED toString()]");
        }
    }

    private static void objectArrayAppend(StringBuilder sbuf, Object[] a, Set<Object[]> seenSet) {
        if (a.length == 0) {
            return;
        }
        if (seenSet == null) {
            seenSet = new HashSet<Object[]>(a.length);
        }
        if (seenSet.add(a)) {
            deeplyAppendParameter(sbuf, a[0], seenSet);
            for (int i = 1; i < a.length; i++) {
                sbuf.append(", ");
                deeplyAppendParameter(sbuf, a[i], seenSet);
            }
            // allow repeats in siblings
            // 允许在兄弟节点中重复
            seenSet.remove(a);
        } else {
            sbuf.append("...");
        }
    }

    private static void booleanArrayAppend(StringBuilder sbuf, boolean[] a) {
        if (a.length == 0) {
            return;
        }
        sbuf.append(a[0]);
        for (int i = 1; i < a.length; i++) {
            sbuf.append(", ");
            sbuf.append(a[i]);
        }
    }

    private static void byteArrayAppend(StringBuilder sbuf, byte[] a) {
        if (a.length == 0) {
            return;
        }
        sbuf.append(a[0]);
        for (int i = 1; i < a.length; i++) {
            sbuf.append(", ");
            sbuf.append(a[i]);
        }
    }

    private static void charArrayAppend(StringBuilder sbuf, char[] a) {
        if (a.length == 0) {
            return;
        }
        sbuf.append(a[0]);
        for (int i = 1; i < a.length; i++) {
            sbuf.append(", ");
            sbuf.append(a[i]);
        }
    }

    private static void shortArrayAppend(StringBuilder sbuf, short[] a) {
        if (a.length == 0) {
            return;
        }
        sbuf.append(a[0]);
        for (int i = 1; i < a.length; i++) {
            sbuf.append(", ");
            sbuf.append(a[i]);
        }
    }

    private static void intArrayAppend(StringBuilder sbuf, int[] a) {
        if (a.length == 0) {
            return;
        }
        sbuf.append(a[0]);
        for (int i = 1; i < a.length; i++) {
            sbuf.append(", ");
            sbuf.append(a[i]);
        }
    }

    private static void longArrayAppend(StringBuilder sbuf, long[] a) {
        if (a.length == 0) {
            return;
        }
        sbuf.append(a[0]);
        for (int i = 1; i < a.length; i++) {
            sbuf.append(", ");
            sbuf.append(a[i]);
        }
    }

    private static void floatArrayAppend(StringBuilder sbuf, float[] a) {
        if (a.length == 0) {
            return;
        }
        sbuf.append(a[0]);
        for (int i = 1; i < a.length; i++) {
            sbuf.append(", ");
            sbuf.append(a[i]);
        }
    }

    private static void doubleArrayAppend(StringBuilder sbuf, double[] a) {
        if (a.length == 0) {
            return;
        }
        sbuf.append(a[0]);
        for (int i = 1; i < a.length; i++) {
            sbuf.append(", ");
            sbuf.append(a[i]);
        }
    }

    private MessageFormatter() {
    }
}
