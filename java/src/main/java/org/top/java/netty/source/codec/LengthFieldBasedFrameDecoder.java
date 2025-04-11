
package org.top.java.netty.source.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.*;

import java.nio.ByteOrder;
import java.util.List;

import static io.netty.util.internal.ObjectUtil.*;

/**
 * A decoder that splits the received {@link ByteBuf}s dynamically by the
 * value of the length field in the message.  It is particularly useful when you
 * decode a binary message which has an integer header field that represents the
 * length of the message body or the whole message.
 * <p>
 * {@link LengthFieldBasedFrameDecoder} has many configuration parameters so
 * that it can decode any message with a length field, which is often seen in
 * proprietary client-server protocols. Here are some example that will give
 * you the basic idea on which option does what.
 *
 * <h3>2 bytes length field at offset 0, do not strip header</h3>
 *
 * The value of the length field in this example is <tt>12 (0x0C)</tt> which
 * represents the length of "HELLO, WORLD".  By default, the decoder assumes
 * that the length field represents the number of the bytes that follows the
 * length field.  Therefore, it can be decoded with the simplistic parameter
 * combination.
 * <pre>
 * <b>lengthFieldOffset</b>   = <b>0</b>
 * <b>lengthFieldLength</b>   = <b>2</b>
 * lengthAdjustment    = 0
 * initialBytesToStrip = 0 (= do not strip header)
 *
 * BEFORE DECODE (14 bytes)         AFTER DECODE (14 bytes)
 * +--------+----------------+      +--------+----------------+
 * | Length | Actual Content |----->| Length | Actual Content |
 * | 0x000C | "HELLO, WORLD" |      | 0x000C | "HELLO, WORLD" |
 * +--------+----------------+      +--------+----------------+
 * </pre>
 *
 * <h3>2 bytes length field at offset 0, strip header</h3>
 *
 * Because we can get the length of the content by calling
 * {@link ByteBuf#readableBytes()}, you might want to strip the length
 * field by specifying <tt>initialBytesToStrip</tt>.  In this example, we
 * specified <tt>2</tt>, that is same with the length of the length field, to
 * strip the first two bytes.
 * <pre>
 * lengthFieldOffset   = 0
 * lengthFieldLength   = 2
 * lengthAdjustment    = 0
 * <b>initialBytesToStrip</b> = <b>2</b> (= the length of the Length field)
 *
 * BEFORE DECODE (14 bytes)         AFTER DECODE (12 bytes)
 * +--------+----------------+      +----------------+
 * | Length | Actual Content |----->| Actual Content |
 * | 0x000C | "HELLO, WORLD" |      | "HELLO, WORLD" |
 * +--------+----------------+      +----------------+
 * </pre>
 *
 * <h3>2 bytes length field at offset 0, do not strip header, the length field
 *     represents the length of the whole message</h3>
 *
 * In most cases, the length field represents the length of the message body
 * only, as shown in the previous examples.  However, in some protocols, the
 * length field represents the length of the whole message, including the
 * message header.  In such a case, we specify a non-zero
 * <tt>lengthAdjustment</tt>.  Because the length value in this example message
 * is always greater than the body length by <tt>2</tt>, we specify <tt>-2</tt>
 * as <tt>lengthAdjustment</tt> for compensation.
 * <pre>
 * lengthFieldOffset   =  0
 * lengthFieldLength   =  2
 * <b>lengthAdjustment</b>    = <b>-2</b> (= the length of the Length field)
 * initialBytesToStrip =  0
 *
 * BEFORE DECODE (14 bytes)         AFTER DECODE (14 bytes)
 * +--------+----------------+      +--------+----------------+
 * | Length | Actual Content |----->| Length | Actual Content |
 * | 0x000E | "HELLO, WORLD" |      | 0x000E | "HELLO, WORLD" |
 * +--------+----------------+      +--------+----------------+
 * </pre>
 *
 * <h3>3 bytes length field at the end of 5 bytes header, do not strip header</h3>
 *
 * The following message is a simple variation of the first example.  An extra
 * header value is prepended to the message.  <tt>lengthAdjustment</tt> is zero
 * again because the decoder always takes the length of the prepended data into
 * account during frame length calculation.
 * <pre>
 * <b>lengthFieldOffset</b>   = <b>2</b> (= the length of Header 1)
 * <b>lengthFieldLength</b>   = <b>3</b>
 * lengthAdjustment    = 0
 * initialBytesToStrip = 0
 *
 * BEFORE DECODE (17 bytes)                      AFTER DECODE (17 bytes)
 * +----------+----------+----------------+      +----------+----------+----------------+
 * | Header 1 |  Length  | Actual Content |----->| Header 1 |  Length  | Actual Content |
 * |  0xCAFE  | 0x00000C | "HELLO, WORLD" |      |  0xCAFE  | 0x00000C | "HELLO, WORLD" |
 * +----------+----------+----------------+      +----------+----------+----------------+
 * </pre>
 *
 * <h3>3 bytes length field at the beginning of 5 bytes header, do not strip header</h3>
 *
 * This is an advanced example that shows the case where there is an extra
 * header between the length field and the message body.  You have to specify a
 * positive <tt>lengthAdjustment</tt> so that the decoder counts the extra
 * header into the frame length calculation.
 * <pre>
 * lengthFieldOffset   = 0
 * lengthFieldLength   = 3
 * <b>lengthAdjustment</b>    = <b>2</b> (= the length of Header 1)
 * initialBytesToStrip = 0
 *
 * BEFORE DECODE (17 bytes)                      AFTER DECODE (17 bytes)
 * +----------+----------+----------------+      +----------+----------+----------------+
 * |  Length  | Header 1 | Actual Content |----->|  Length  | Header 1 | Actual Content |
 * | 0x00000C |  0xCAFE  | "HELLO, WORLD" |      | 0x00000C |  0xCAFE  | "HELLO, WORLD" |
 * +----------+----------+----------------+      +----------+----------+----------------+
 * </pre>
 *
 * <h3>2 bytes length field at offset 1 in the middle of 4 bytes header,
 *     strip the first header field and the length field</h3>
 *
 * This is a combination of all the examples above.  There are the prepended
 * header before the length field and the extra header after the length field.
 * The prepended header affects the <tt>lengthFieldOffset</tt> and the extra
 * header affects the <tt>lengthAdjustment</tt>.  We also specified a non-zero
 * <tt>initialBytesToStrip</tt> to strip the length field and the prepended
 * header from the frame.  If you don't want to strip the prepended header, you
 * could specify <tt>0</tt> for <tt>initialBytesToSkip</tt>.
 * <pre>
 * lengthFieldOffset   = 1 (= the length of HDR1)
 * lengthFieldLength   = 2
 * <b>lengthAdjustment</b>    = <b>1</b> (= the length of HDR2)
 * <b>initialBytesToStrip</b> = <b>3</b> (= the length of HDR1 + LEN)
 *
 * BEFORE DECODE (16 bytes)                       AFTER DECODE (13 bytes)
 * +------+--------+------+----------------+      +------+----------------+
 * | HDR1 | Length | HDR2 | Actual Content |----->| HDR2 | Actual Content |
 * | 0xCA | 0x000C | 0xFE | "HELLO, WORLD" |      | 0xFE | "HELLO, WORLD" |
 * +------+--------+------+----------------+      +------+----------------+
 * </pre>
 *
 * <h3>2 bytes length field at offset 1 in the middle of 4 bytes header,
 *     strip the first header field and the length field, the length field
 *     represents the length of the whole message</h3>
 *
 * Let's give another twist to the previous example.  The only difference from
 * the previous example is that the length field represents the length of the
 * whole message instead of the message body, just like the third example.
 * We have to count the length of HDR1 and Length into <tt>lengthAdjustment</tt>.
 * Please note that we don't need to take the length of HDR2 into account
 * because the length field already includes the whole header length.
 * <pre>
 * lengthFieldOffset   =  1
 * lengthFieldLength   =  2
 * <b>lengthAdjustment</b>    = <b>-3</b> (= the length of HDR1 + LEN, negative)
 * <b>initialBytesToStrip</b> = <b> 3</b>
 *
 * BEFORE DECODE (16 bytes)                       AFTER DECODE (13 bytes)
 * +------+--------+------+----------------+      +------+----------------+
 * | HDR1 | Length | HDR2 | Actual Content |----->| HDR2 | Actual Content |
 * | 0xCA | 0x0010 | 0xFE | "HELLO, WORLD" |      | 0xFE | "HELLO, WORLD" |
 * +------+--------+------+----------------+      +------+----------------+
 * </pre>
 * @see LengthFieldPrepender
 */

/**
 * 一个根据消息中长度字段的值动态分割接收到的 {@link ByteBuf} 的解码器。它在解码具有表示消息体或整个消息长度的整数头字段的二进制消息时特别有用。
 * <p>
 * {@link LengthFieldBasedFrameDecoder} 有许多配置参数，因此它可以解码任何具有长度字段的消息，这通常在专有的客户端-服务器协议中看到。以下是一些示例，可以帮助你了解每个选项的作用。
 *
 * <h3>2字节长度字段在偏移量0处，不剥离头部</h3>
 *
 * 此示例中长度字段的值为 <tt>12 (0x0C)</tt>，表示 "HELLO, WORLD" 的长度。默认情况下，解码器假定长度字段表示长度字段后面的字节数。因此，可以使用简化的参数组合进行解码。
 * <pre>
 * <b>lengthFieldOffset</b>   = <b>0</b>
 * <b>lengthFieldLength</b>   = <b>2</b>
 * lengthAdjustment    = 0
 * initialBytesToStrip = 0 (= 不剥离头部)
 *
 * 解码前 (14字节)         解码后 (14字节)
 * +--------+----------------+      +--------+----------------+
 * | 长度 | 实际内容 |----->| 长度 | 实际内容 |
 * | 0x000C | "HELLO, WORLD" |      | 0x000C | "HELLO, WORLD" |
 * +--------+----------------+      +--------+----------------+
 * </pre>
 *
 * <h3>2字节长度字段在偏移量0处，剥离头部</h3>
 *
 * 因为我们可以通过调用 {@link ByteBuf#readableBytes()} 来获取内容的长度，你可能希望通过指定 <tt>initialBytesToStrip</tt> 来剥离长度字段。在此示例中，我们指定了 <tt>2</tt>，即长度字段的长度，以剥离前两个字节。
 * <pre>
 * lengthFieldOffset   = 0
 * lengthFieldLength   = 2
 * lengthAdjustment    = 0
 * <b>initialBytesToStrip</b> = <b>2</b> (= 长度字段的长度)
 *
 * 解码前 (14字节)         解码后 (12字节)
 * +--------+----------------+      +----------------+
 * | 长度 | 实际内容 |----->| 实际内容 |
 * | 0x000C | "HELLO, WORLD" |      | "HELLO, WORLD" |
 * +--------+----------------+      +----------------+
 * </pre>
 *
 * <h3>2字节长度字段在偏移量0处，不剥离头部，长度字段表示整个消息的长度</h3>
 *
 * 在大多数情况下，长度字段仅表示消息体的长度，如前面的示例所示。然而，在某些协议中，长度字段表示整个消息的长度，包括消息头。在这种情况下，我们指定一个非零的 <tt>lengthAdjustment</tt>。因为此示例消息中的长度值总是比消息体长度大 <tt>2</tt>，所以我们指定 <tt>-2</tt> 作为 <tt>lengthAdjustment</tt> 进行补偿。
 * <pre>
 * lengthFieldOffset   =  0
 * lengthFieldLength   =  2
 * <b>lengthAdjustment</b>    = <b>-2</b> (= 长度字段的长度)
 * initialBytesToStrip =  0
 *
 * 解码前 (14字节)         解码后 (14字节)
 * +--------+----------------+      +--------+----------------+
 * | 长度 | 实际内容 |----->| 长度 | 实际内容 |
 * | 0x000E | "HELLO, WORLD" |      | 0x000E | "HELLO, WORLD" |
 * +--------+----------------+      +--------+----------------+
 * </pre>
 *
 * <h3>3字节长度字段在5字节头部的末尾，不剥离头部</h3>
 *
 * 以下消息是第一个示例的简单变体。在消息前添加了一个额外的头值。由于解码器在帧长度计算中始终考虑前置数据的长度，因此 <tt>lengthAdjustment</tt> 再次为零。
 * <pre>
 * <b>lengthFieldOffset</b>   = <b>2</b> (= Header 1 的长度)
 * <b>lengthFieldLength</b>   = <b>3</b>
 * lengthAdjustment    = 0
 * initialBytesToStrip = 0
 *
 * 解码前 (17字节)                     解码后 (17字节)
 * +----------+----------+----------------+      +----------+----------+----------------+
 * | Header 1 |  长度  | 实际内容 |----->| Header 1 |  长度  | 实际内容 |
 * |  0xCAFE  | 0x00000C | "HELLO, WORLD" |      |  0xCAFE  | 0x00000C | "HELLO, WORLD" |
 * +----------+----------+----------------+      +----------+----------+----------------+
 * </pre>
 *
 * <h3>3字节长度字段在5字节头部的开头，不剥离头部</h3>
 *
 * 这是一个高级示例，展示了在长度字段和消息体之间有额外头的情况。你必须指定一个正的 <tt>lengthAdjustment</tt>，以便解码器将额外的头计入帧长度计算中。
 * <pre>
 * lengthFieldOffset   = 0
 * lengthFieldLength   = 3
 * <b>lengthAdjustment</b>    = <b>2</b> (= Header 1 的长度)
 * initialBytesToStrip = 0
 *
 * 解码前 (17字节)                     解码后 (17字节)
 * +----------+----------+----------------+      +----------+----------+----------------+
 * |  长度  | Header 1 | 实际内容 |----->|  长度  | Header 1 | 实际内容 |
 * | 0x00000C |  0xCAFE  | "HELLO, WORLD" |      | 0x00000C |  0xCAFE  | "HELLO, WORLD" |
 * +----------+----------+----------------+      +----------+----------+----------------+
 * </pre>
 *
 * <h3>2字节长度字段在4字节头部的中间偏移量1处，剥离第一个头字段和长度字段</h3>
 *
 * 这是上述所有示例的组合。在长度字段前有前置头，在长度字段后有额外的头。前置头影响 <tt>lengthFieldOffset</tt>，额外的头影响 <tt>lengthAdjustment</tt>。我们还指定了一个非零的 <tt>initialBytesToStrip</tt>，以从帧中剥离长度字段和前置头。如果你不想剥离前置头，可以指定 <tt>0</tt> 作为 <tt>initialBytesToSkip</tt>。
 * <pre>
 * lengthFieldOffset   = 1 (= HDR1 的长度)
 * lengthFieldLength   = 2
 * <b>lengthAdjustment</b>    = <b>1</b> (= HDR2 的长度)
 * <b>initialBytesToStrip</b> = <b>3</b> (= HDR1 + LEN 的长度)
 *
 * 解码前 (16字节)                      解码后 (13字节)
 * +------+--------+------+----------------+      +------+----------------+
 * | HDR1 | 长度 | HDR2 | 实际内容 |----->| HDR2 | 实际内容 |
 * | 0xCA | 0x000C | 0xFE | "HELLO, WORLD" |      | 0xFE | "HELLO, WORLD" |
 * +------+--------+------+----------------+      +------+----------------+
 * </pre>
 *
 * <h3>2字节长度字段在4字节头部的中间偏移量1处，剥离第一个头字段和长度字段，长度字段表示整个消息的长度</h3>
 *
 * 让我们对前面的示例进行另一个变化。与前一个示例的唯一区别是长度字段表示整个消息的长度，而不是消息体的长度，就像第三个示例一样。我们必须将 HDR1 和 Length 的长度计入 <tt>lengthAdjustment</tt>。请注意，我们不需要考虑 HDR2 的长度，因为长度字段已经包括整个头的长度。
 * <pre>
 * lengthFieldOffset   =  1
 * lengthFieldLength   =  2
 * <b>lengthAdjustment</b>    = <b>-3</b> (= HDR1 + LEN 的长度，负数)
 * <b>initialBytesToStrip</b> = <b> 3</b>
 *
 * 解码前 (16字节)                      解码后 (13字节)
 * +------+--------+------+----------------+      +------+----------------+
 * | HDR1 | 长度 | HDR2 | 实际内容 |----->| HDR2 | 实际内容 |
 * | 0xCA | 0x0010 | 0xFE | "HELLO, WORLD" |      | 0xFE | "HELLO, WORLD" |
 * +------+--------+------+----------------+      +------+----------------+
 * </pre>
 * @see LengthFieldPrepender
 */
public class LengthFieldBasedFrameDecoder extends io.netty.handler.codec.ByteToMessageDecoder {

    private final ByteOrder byteOrder;
    private final int maxFrameLength;
    private final int lengthFieldOffset;
    private final int lengthFieldLength;
    private final int lengthFieldEndOffset;
    private final int lengthAdjustment;
    private final int initialBytesToStrip;
    private final boolean failFast;
    private boolean discardingTooLongFrame;
    private long tooLongFrameLength;
    private long bytesToDiscard;
    private int frameLengthInt = -1;

    /**
     * Creates a new instance.
     *
     * @param maxFrameLength
     *        the maximum length of the frame.  If the length of the frame is
     *        greater than this value, {@link TooLongFrameException} will be
     *        thrown.
     * @param lengthFieldOffset
     *        the offset of the length field
     * @param lengthFieldLength
     *        the length of the length field
     */

    /**
     * 创建一个新实例。
     *
     * @param maxFrameLength
     *        帧的最大长度。如果帧的长度大于此值，将抛出 {@link TooLongFrameException}。
     * @param lengthFieldOffset
     *        长度字段的偏移量
     * @param lengthFieldLength
     *        长度字段的长度
     */
    public LengthFieldBasedFrameDecoder(
            int maxFrameLength,
            int lengthFieldOffset, int lengthFieldLength) {
        this(maxFrameLength, lengthFieldOffset, lengthFieldLength, 0, 0);
    }

    /**
     * Creates a new instance.
     *
     * @param maxFrameLength
     *        the maximum length of the frame.  If the length of the frame is
     *        greater than this value, {@link TooLongFrameException} will be
     *        thrown.
     * @param lengthFieldOffset
     *        the offset of the length field
     * @param lengthFieldLength
     *        the length of the length field
     * @param lengthAdjustment
     *        the compensation value to add to the value of the length field
     * @param initialBytesToStrip
     *        the number of first bytes to strip out from the decoded frame
     */

    /**
     * 创建新实例。
     *
     * @param maxFrameLength
     *        帧的最大长度。如果帧的长度大于此值，将抛出 {@link TooLongFrameException}。
     * @param lengthFieldOffset
     *        长度字段的偏移量
     * @param lengthFieldLength
     *        长度字段的长度
     * @param lengthAdjustment
     *        长度字段值的补偿值
     * @param initialBytesToStrip
     *        从解码帧中剥离的前几个字节数
     */
    public LengthFieldBasedFrameDecoder(
            int maxFrameLength,
            int lengthFieldOffset, int lengthFieldLength,
            int lengthAdjustment, int initialBytesToStrip) {
        this(
                maxFrameLength,
                lengthFieldOffset, lengthFieldLength, lengthAdjustment,
                initialBytesToStrip, true);
    }

    /**
     * Creates a new instance.
     *
     * @param maxFrameLength
     *        the maximum length of the frame.  If the length of the frame is
     *        greater than this value, {@link TooLongFrameException} will be
     *        thrown.
     * @param lengthFieldOffset
     *        the offset of the length field
     * @param lengthFieldLength
     *        the length of the length field
     * @param lengthAdjustment
     *        the compensation value to add to the value of the length field
     * @param initialBytesToStrip
     *        the number of first bytes to strip out from the decoded frame
     * @param failFast
     *        If <tt>true</tt>, a {@link TooLongFrameException} is thrown as
     *        soon as the decoder notices the length of the frame will exceed
     *        <tt>maxFrameLength</tt> regardless of whether the entire frame
     *        has been read.  If <tt>false</tt>, a {@link TooLongFrameException}
     *        is thrown after the entire frame that exceeds <tt>maxFrameLength</tt>
     *        has been read.
     */

    /**
     * 创建新实例。
     *
     * @param maxFrameLength
     *        帧的最大长度。如果帧的长度超过此值，将抛出 {@link TooLongFrameException}。
     * @param lengthFieldOffset
     *        长度字段的偏移量
     * @param lengthFieldLength
     *        长度字段的长度
     * @param lengthAdjustment
     *        长度字段值的补偿值
     * @param initialBytesToStrip
     *        解码帧时要剥离的初始字节数
     * @param failFast
     *        如果为 <tt>true</tt>，解码器一旦发现帧的长度将超过 <tt>maxFrameLength</tt>，无论是否已读取整个帧，都会立即抛出 {@link TooLongFrameException}。
     *        如果为 <tt>false</tt>，则在读取完超过 <tt>maxFrameLength</tt> 的整个帧后抛出 {@link TooLongFrameException}。
     */
    public LengthFieldBasedFrameDecoder(
            int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
            int lengthAdjustment, int initialBytesToStrip, boolean failFast) {
        this(
                ByteOrder.BIG_ENDIAN, maxFrameLength, lengthFieldOffset, lengthFieldLength,
                lengthAdjustment, initialBytesToStrip, failFast);
    }

    /**
     * Creates a new instance.
     *
     * @param byteOrder
     *        the {@link ByteOrder} of the length field
     * @param maxFrameLength
     *        the maximum length of the frame.  If the length of the frame is
     *        greater than this value, {@link TooLongFrameException} will be
     *        thrown.
     * @param lengthFieldOffset
     *        the offset of the length field
     * @param lengthFieldLength
     *        the length of the length field
     * @param lengthAdjustment
     *        the compensation value to add to the value of the length field
     * @param initialBytesToStrip
     *        the number of first bytes to strip out from the decoded frame
     * @param failFast
     *        If <tt>true</tt>, a {@link TooLongFrameException} is thrown as
     *        soon as the decoder notices the length of the frame will exceed
     *        <tt>maxFrameLength</tt> regardless of whether the entire frame
     *        has been read.  If <tt>false</tt>, a {@link TooLongFrameException}
     *        is thrown after the entire frame that exceeds <tt>maxFrameLength</tt>
     *        has been read.
     */

    /**
     * 创建新实例。
     *
     * @param byteOrder
     *        长度字段的 {@link ByteOrder}
     * @param maxFrameLength
     *        帧的最大长度。如果帧的长度大于此值，将抛出 {@link TooLongFrameException}。
     * @param lengthFieldOffset
     *        长度字段的偏移量
     * @param lengthFieldLength
     *        长度字段的长度
     * @param lengthAdjustment
     *        添加到长度字段值的补偿值
     * @param initialBytesToStrip
     *        从解码帧中剥离的初始字节数
     * @param failFast
     *        如果为 <tt>true</tt>，解码器一旦注意到帧的长度将超过 <tt>maxFrameLength</tt>，
     *        无论是否已读取整个帧，都会立即抛出 {@link TooLongFrameException}。
     *        如果为 <tt>false</tt>，则在读取完超过 <tt>maxFrameLength</tt> 的整个帧后抛出 {@link TooLongFrameException}。
     */
    public LengthFieldBasedFrameDecoder(
            ByteOrder byteOrder, int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
            int lengthAdjustment, int initialBytesToStrip, boolean failFast) {

        this.byteOrder = checkNotNull(byteOrder, "byteOrder");

        checkPositive(maxFrameLength, "maxFrameLength");

        checkPositiveOrZero(lengthFieldOffset, "lengthFieldOffset");

        checkPositiveOrZero(initialBytesToStrip, "initialBytesToStrip");

        if (lengthFieldOffset > maxFrameLength - lengthFieldLength) {
            throw new IllegalArgumentException(
                    "maxFrameLength (" + maxFrameLength + ") " +
                    "must be equal to or greater than " +
                    "lengthFieldOffset (" + lengthFieldOffset + ") + " +
                    "lengthFieldLength (" + lengthFieldLength + ").");
        }

        this.maxFrameLength = maxFrameLength;
        this.lengthFieldOffset = lengthFieldOffset;
        this.lengthFieldLength = lengthFieldLength;
        this.lengthAdjustment = lengthAdjustment;
        this.lengthFieldEndOffset = lengthFieldOffset + lengthFieldLength;
        this.initialBytesToStrip = initialBytesToStrip;
        this.failFast = failFast;
    }

    @Override
    protected final void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Object decoded = decode(ctx, in);
        if (decoded != null) {
            out.add(decoded);
        }
    }

    private void discardingTooLongFrame(ByteBuf in) {
        long bytesToDiscard = this.bytesToDiscard;
        int localBytesToDiscard = (int) Math.min(bytesToDiscard, in.readableBytes());
        in.skipBytes(localBytesToDiscard);
        bytesToDiscard -= localBytesToDiscard;
        this.bytesToDiscard = bytesToDiscard;

        failIfNecessary(false);
    }

    private static void failOnNegativeLengthField(ByteBuf in, long frameLength, int lengthFieldEndOffset) {
        in.skipBytes(lengthFieldEndOffset);
        throw new CorruptedFrameException(
           "negative pre-adjustment length field: " + frameLength);
    }

    private static void failOnFrameLengthLessThanLengthFieldEndOffset(ByteBuf in,
                                                                      long frameLength,
                                                                      int lengthFieldEndOffset) {
        in.skipBytes(lengthFieldEndOffset);
        throw new CorruptedFrameException(
           "Adjusted frame length (" + frameLength + ") is less " +
              "than lengthFieldEndOffset: " + lengthFieldEndOffset);
    }

    private void exceededFrameLength(ByteBuf in, long frameLength) {
        long discard = frameLength - in.readableBytes();
        tooLongFrameLength = frameLength;

        if (discard < 0) {
            // buffer contains more bytes then the frameLength so we can discard all now
            // 缓冲区包含的字节数超过了帧长度，因此现在可以全部丢弃
            in.skipBytes((int) frameLength);
        } else {
            // Enter the discard mode and discard everything received so far.
            // 进入丢弃模式并丢弃到目前为止收到的所有内容。
            discardingTooLongFrame = true;
            bytesToDiscard = discard;
            in.skipBytes(in.readableBytes());
        }
        failIfNecessary(true);
    }

    private static void failOnFrameLengthLessThanInitialBytesToStrip(ByteBuf in,
                                                                     long frameLength,
                                                                     int initialBytesToStrip) {
        in.skipBytes((int) frameLength);
        throw new CorruptedFrameException(
           "Adjusted frame length (" + frameLength + ") is less " +
              "than initialBytesToStrip: " + initialBytesToStrip);
    }

    /**
     * Create a frame out of the {@link ByteBuf} and return it.
     *
     * @param   ctx             the {@link ChannelHandlerContext} which this {@link ByteToMessageDecoder} belongs to
     * @param   in              the {@link ByteBuf} from which to read data
     * @return  frame           the {@link ByteBuf} which represent the frame or {@code null} if no frame could
     *                          be created.
     */

    /**
     * 从 {@link ByteBuf} 中创建一个帧并返回它。
     *
     * @param   ctx             该 {@link ByteToMessageDecoder} 所属的 {@link ChannelHandlerContext}
     * @param   in              从中读取数据的 {@link ByteBuf}
     * @return  frame           表示帧的 {@link ByteBuf}，如果无法创建帧，则返回 {@code null}。
     */
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        long frameLength = 0;
        if (frameLengthInt == -1) { // new frame

            if (discardingTooLongFrame) {
                discardingTooLongFrame(in);
            }

            if (in.readableBytes() < lengthFieldEndOffset) {
                return null;
            }

            int actualLengthFieldOffset = in.readerIndex() + lengthFieldOffset;
            frameLength = getUnadjustedFrameLength(in, actualLengthFieldOffset, lengthFieldLength, byteOrder);

            if (frameLength < 0) {
                failOnNegativeLengthField(in, frameLength, lengthFieldEndOffset);
            }

            frameLength += lengthAdjustment + lengthFieldEndOffset;

            if (frameLength < lengthFieldEndOffset) {
                failOnFrameLengthLessThanLengthFieldEndOffset(in, frameLength, lengthFieldEndOffset);
            }

            if (frameLength > maxFrameLength) {
                exceededFrameLength(in, frameLength);
                return null;
            }
            // never overflows because it's less than maxFrameLength
            // 永远不会溢出，因为它小于 maxFrameLength
            frameLengthInt = (int) frameLength;
        }
        if (in.readableBytes() < frameLengthInt) { // frameLengthInt exist , just check buf
            return null;
        }
        if (initialBytesToStrip > frameLengthInt) {
            failOnFrameLengthLessThanInitialBytesToStrip(in, frameLength, initialBytesToStrip);
        }
        in.skipBytes(initialBytesToStrip);

        // extract frame

        // 提取帧
        int readerIndex = in.readerIndex();
        int actualFrameLength = frameLengthInt - initialBytesToStrip;
        ByteBuf frame = extractFrame(ctx, in, readerIndex, actualFrameLength);
        in.readerIndex(readerIndex + actualFrameLength);
        frameLengthInt = -1; // start processing the next frame
        return frame;
    }

    /**
     * Decodes the specified region of the buffer into an unadjusted frame length.  The default implementation is
     * capable of decoding the specified region into an unsigned 8/16/24/32/64 bit integer.  Override this method to
     * decode the length field encoded differently.  Note that this method must not modify the state of the specified
     * buffer (e.g. {@code readerIndex}, {@code writerIndex}, and the content of the buffer.)
     *
     * @throws DecoderException if failed to decode the specified region
     */

    /**
     * 将缓冲区的指定区域解码为未调整的帧长度。默认实现能够将指定区域解码为无符号的8/16/24/32/64位整数。
     * 重写此方法以解码不同编码的长度字段。注意，此方法不得修改指定缓冲区的状态（例如，{@code readerIndex}，
     * {@code writerIndex}，以及缓冲区的内容。）
     *
     * @throws DecoderException 如果解码指定区域失败
     */
    protected long getUnadjustedFrameLength(ByteBuf buf, int offset, int length, ByteOrder order) {
        buf = buf.order(order);
        long frameLength;
        switch (length) {
        case 1:
            frameLength = buf.getUnsignedByte(offset);
            break;
        case 2:
            frameLength = buf.getUnsignedShort(offset);
            break;
        case 3:
            frameLength = buf.getUnsignedMedium(offset);
            break;
        case 4:
            frameLength = buf.getUnsignedInt(offset);
            break;
        case 8:
            frameLength = buf.getLong(offset);
            break;
        default:
            throw new DecoderException(
                    "unsupported lengthFieldLength: " + lengthFieldLength + " (expected: 1, 2, 3, 4, or 8)");
        }
        return frameLength;
    }

    private void failIfNecessary(boolean firstDetectionOfTooLongFrame) {
        if (bytesToDiscard == 0) {
            // Reset to the initial state and tell the handlers that
            // 重置到初始状态并通知处理程序
            // the frame was too large.
            // 框架太大了。
            long tooLongFrameLength = this.tooLongFrameLength;
            this.tooLongFrameLength = 0;
            discardingTooLongFrame = false;
            if (!failFast || firstDetectionOfTooLongFrame) {
                fail(tooLongFrameLength);
            }
        } else {
            // Keep discarding and notify handlers if necessary.
            // 持续丢弃并在必要时通知处理程序。
            if (failFast && firstDetectionOfTooLongFrame) {
                fail(tooLongFrameLength);
            }
        }
    }

    /**
     * Extract the sub-region of the specified buffer.
     */

    /**
     * 提取指定缓冲区的子区域。
     */
    protected ByteBuf extractFrame(ChannelHandlerContext ctx, ByteBuf buffer, int index, int length) {
        return buffer.retainedSlice(index, length);
    }

    private void fail(long frameLength) {
        if (frameLength > 0) {
            throw new TooLongFrameException(
                            "Adjusted frame length exceeds " + maxFrameLength +
                            ": " + frameLength + " - discarded");
        } else {
            throw new TooLongFrameException(
                            "Adjusted frame length exceeds " + maxFrameLength +
                            " - discarding");
        }
    }
}
