
package org.top.java.netty.source.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.ChannelInputShutdownEvent;
import io.netty.handler.codec.*;
import io.netty.util.IllegalReferenceCountException;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.StringUtil;

import java.util.List;

import static io.netty.util.internal.ObjectUtil.checkPositive;
import static java.lang.Integer.MAX_VALUE;

/**
 * {@link ChannelInboundHandlerAdapter} which decodes bytes in a stream-like fashion from one {@link ByteBuf} to an
 * other Message type.
 *
 * For example here is an implementation which reads all readable bytes from
 * the input {@link ByteBuf} and create a new {@link ByteBuf}.
 *
 * <pre>
 *     public class SquareDecoder extends {@link ByteToMessageDecoder} {
 *         {@code @Override}
 *         public void decode({@link ChannelHandlerContext} ctx, {@link ByteBuf} in, List&lt;Object&gt; out)
 *                 throws {@link Exception} {
 *             out.add(in.readBytes(in.readableBytes()));
 *         }
 *     }
 * </pre>
 *
 * <h3>Frame detection</h3>
 * <p>
 * Generally frame detection should be handled earlier in the pipeline by adding a
 * {@link DelimiterBasedFrameDecoder}, {@link FixedLengthFrameDecoder}, {@link LengthFieldBasedFrameDecoder},
 * or {@link LineBasedFrameDecoder}.
 * <p>
 * If a custom frame decoder is required, then one needs to be careful when implementing
 * one with {@link ByteToMessageDecoder}. Ensure there are enough bytes in the buffer for a
 * complete frame by checking {@link ByteBuf#readableBytes()}. If there are not enough bytes
 * for a complete frame, return without modifying the reader index to allow more bytes to arrive.
 * <p>
 * To check for complete frames without modifying the reader index, use methods like {@link ByteBuf#getInt(int)}.
 * One <strong>MUST</strong> use the reader index when using methods like {@link ByteBuf#getInt(int)}.
 * For example calling <tt>in.getInt(0)</tt> is assuming the frame starts at the beginning of the buffer, which
 * is not always the case. Use <tt>in.getInt(in.readerIndex())</tt> instead.
 * <h3>Pitfalls</h3>
 * <p>
 * Be aware that sub-classes of {@link ByteToMessageDecoder} <strong>MUST NOT</strong>
 * annotated with {@link @Sharable}.
 * <p>
 * Some methods such as {@link ByteBuf#readBytes(int)} will cause a memory leak if the returned buffer
 * is not released or added to the <tt>out</tt> {@link List}. Use derived buffers like {@link ByteBuf#readSlice(int)}
 * to avoid leaking memory.
 */

/**
 * {@link ChannelInboundHandlerAdapter} 以流式方式从一个 {@link ByteBuf} 解码字节到另一种消息类型。
 *
 * 例如，这里有一个实现，它从输入的 {@link ByteBuf} 中读取所有可读字节并创建一个新的 {@link ByteBuf}。
 *
 * <pre>
 *     public class SquareDecoder extends {@link ByteToMessageDecoder} {
 *         {@code @Override}
 *         public void decode({@link ChannelHandlerContext} ctx, {@link ByteBuf} in, List&lt;Object&gt; out)
 *                 throws {@link Exception} {
 *             out.add(in.readBytes(in.readableBytes()));
 *         }
 *     }
 * </pre>
 *
 * <h3>帧检测</h3>
 * <p>
 * 通常，帧检测应该通过添加 {@link DelimiterBasedFrameDecoder}、{@link FixedLengthFrameDecoder}、{@link LengthFieldBasedFrameDecoder} 或 {@link LineBasedFrameDecoder} 在管道的早期处理。
 * <p>
 * 如果需要自定义帧解码器，则在实现 {@link ByteToMessageDecoder} 时需要小心。通过检查 {@link ByteBuf#readableBytes()} 确保缓冲区中有足够的字节用于完整帧。如果没有足够的字节用于完整帧，则返回而不修改读取器索引，以允许更多字节到达。
 * <p>
 * 要检查完整帧而不修改读取器索引，请使用 {@link ByteBuf#getInt(int)} 等方法。在使用 {@link ByteBuf#getInt(int)} 等方法时，<strong>必须</strong>使用读取器索引。例如，调用 <tt>in.getInt(0)</tt> 是假设帧从缓冲区的开头开始，这并不总是正确的。请改用 <tt>in.getInt(in.readerIndex())</tt>。
 * <h3>陷阱</h3>
 * <p>
 * 请注意，{@link ByteToMessageDecoder} 的子类 <strong>不得</strong> 使用 {@link @Sharable} 注解。
 * <p>
 * 某些方法，如 {@link ByteBuf#readBytes(int)}，如果返回的缓冲区未释放或未添加到 <tt>out</tt> {@link List} 中，将导致内存泄漏。使用派生缓冲区，如 {@link ByteBuf#readSlice(int)}，以避免内存泄漏。
 */
public abstract class ByteToMessageDecoder extends ChannelInboundHandlerAdapter {

    /**
     * Cumulate {@link ByteBuf}s by merge them into one {@link ByteBuf}'s, using memory copies.
     */

    /**
     * 通过内存拷贝将多个{@link ByteBuf}合并为一个{@link ByteBuf}。
     */
    public static final Cumulator MERGE_CUMULATOR = new Cumulator() {
        @Override
        public ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in) {
            if (!cumulation.isReadable() && in.isContiguous()) {
                // If cumulation is empty and input buffer is contiguous, use it directly
                // 如果累积为空且输入缓冲区是连续的，直接使用它
                cumulation.release();
                return in;
            }
            try {
                final int required = in.readableBytes();
                if (required > cumulation.maxWritableBytes() ||
                    required > cumulation.maxFastWritableBytes() && cumulation.refCnt() > 1 ||
                    cumulation.isReadOnly()) {
                    // Expand cumulation (by replacing it) under the following conditions:
                    // 在以下条件下展开累积（通过替换它）：
                    // - cumulation cannot be resized to accommodate the additional data
                    // - 累积无法调整大小以容纳额外的数据
                    // - cumulation can be expanded with a reallocation operation to accommodate but the buffer is
                    // - 累积可以通过重新分配操作进行扩展以适应，但缓冲区是
                    //   assumed to be shared (e.g. refCnt() > 1) and the reallocation may not be safe.
                    //   假设是共享的（例如 refCnt() > 1），重新分配可能不安全。
                    return expandCumulation(alloc, cumulation, in);
                }
                cumulation.writeBytes(in, in.readerIndex(), required);
                in.readerIndex(in.writerIndex());
                return cumulation;
            } finally {
                // We must release in all cases as otherwise it may produce a leak if writeBytes(...) throw
                // 我们必须始终释放资源，否则如果writeBytes(...)抛出异常可能会导致泄漏
                // for whatever release (for example because of OutOfMemoryError)
                // 无论出于何种原因（例如由于OutOfMemoryError）
                in.release();
            }
        }
    };

    /**
     * Cumulate {@link ByteBuf}s by add them to a {@link CompositeByteBuf} and so do no memory copy whenever possible.
     * Be aware that {@link CompositeByteBuf} use a more complex indexing implementation so depending on your use-case
     * and the decoder implementation this may be slower than just use the {@link #MERGE_CUMULATOR}.
     */

    /**
     * 通过将 {@link ByteBuf} 添加到 {@link CompositeByteBuf} 中来累积它们，从而尽可能避免内存复制。
     * 请注意，{@link CompositeByteBuf} 使用了更复杂的索引实现，因此根据您的使用场景和解码器实现，这可能比直接使用 {@link #MERGE_CUMULATOR} 更慢。
     */
    public static final Cumulator COMPOSITE_CUMULATOR = new Cumulator() {
        @Override
        public ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in) {
            if (!cumulation.isReadable()) {
                cumulation.release();
                return in;
            }
            CompositeByteBuf composite = null;
            try {
                if (cumulation instanceof CompositeByteBuf && cumulation.refCnt() == 1) {
                    composite = (CompositeByteBuf) cumulation;
                    // Writer index must equal capacity if we are going to "write"
                    // 如果要进行“写”操作，写索引必须等于容量
                    // new components to the end
                    // 将新组件添加到末尾
                    if (composite.writerIndex() != composite.capacity()) {
                        composite.capacity(composite.writerIndex());
                    }
                } else {
                    composite = alloc.compositeBuffer(Integer.MAX_VALUE).addFlattenedComponents(true, cumulation);
                }
                composite.addFlattenedComponents(true, in);
                in = null;
                return composite;
            } finally {
                if (in != null) {
                    // We must release if the ownership was not transferred as otherwise it may produce a leak
                    // 如果所有权未转移，我们必须释放，否则可能会导致泄漏
                    in.release();
                    // Also release any new buffer allocated if we're not returning it
                    // 如果未返回新分配的缓冲区，则释放它
                    if (composite != null && composite != cumulation) {
                        composite.release();
                    }
                }
            }
        }
    };

    private static final byte STATE_INIT = 0;
    private static final byte STATE_CALLING_CHILD_DECODE = 1;
    private static final byte STATE_HANDLER_REMOVED_PENDING = 2;

    ByteBuf cumulation;
    private Cumulator cumulator = MERGE_CUMULATOR;
    private boolean singleDecode;
    private boolean first;

    /**
     * This flag is used to determine if we need to call {@link ChannelHandlerContext#read()} to consume more data
     * when {@link ChannelConfig#isAutoRead()} is {@code false}.
     */

    /**
     * 此标志用于确定当 {@link ChannelConfig#isAutoRead()} 为 {@code false} 时，是否需要调用 {@link ChannelHandlerContext#read()} 来消费更多数据。
     */
    private boolean firedChannelRead;

    private boolean selfFiredChannelRead;

    /**
     * A bitmask where the bits are defined as
     * <ul>
     *     <li>{@link #STATE_INIT}</li>
     *     <li>{@link #STATE_CALLING_CHILD_DECODE}</li>
     *     <li>{@link #STATE_HANDLER_REMOVED_PENDING}</li>
     * </ul>
     */

    /**
     * 一个位掩码，其中位的定义如下：
     * <ul>
     *     <li>{@link #STATE_INIT}</li>
     *     <li>{@link #STATE_CALLING_CHILD_DECODE}</li>
     *     <li>{@link #STATE_HANDLER_REMOVED_PENDING}</li>
     * </ul>
     */
    private byte decodeState = STATE_INIT;
    private int discardAfterReads = 16;
    private int numReads;

    protected ByteToMessageDecoder() {
        ensureNotSharable();
    }

    /**
     * If set then only one message is decoded on each {@link #channelRead(ChannelHandlerContext, Object)}
     * call. This may be useful if you need to do some protocol upgrade and want to make sure nothing is mixed up.
     *
     * Default is {@code false} as this has performance impacts.
     */

    /**
     * 如果设置，则每次 {@link #channelRead(ChannelHandlerContext, Object)} 调用仅解码一条消息。
     * 这在需要进行某些协议升级并确保不会混淆时可能有用。
     *
     * 默认值为 {@code false}，因为这会影响性能。
     */
    public void setSingleDecode(boolean singleDecode) {
        this.singleDecode = singleDecode;
    }

    /**
     * If {@code true} then only one message is decoded on each
     * {@link #channelRead(ChannelHandlerContext, Object)} call.
     *
     * Default is {@code false} as this has performance impacts.
     */

    /**
     * 如果为 {@code true}，则每次 {@link #channelRead(ChannelHandlerContext, Object)} 调用时只解码一条消息。
     *
     * 默认值为 {@code false}，因为这会带来性能影响。
     */
    public boolean isSingleDecode() {
        return singleDecode;
    }

    /**
     * Set the {@link Cumulator} to use for cumulate the received {@link ByteBuf}s.
     */

    /**
     * 设置用于累积接收到的 {@link ByteBuf} 的 {@link Cumulator}。
     */
    public void setCumulator(Cumulator cumulator) {
        this.cumulator = ObjectUtil.checkNotNull(cumulator, "cumulator");
    }

    /**
     * Set the number of reads after which {@link ByteBuf#discardSomeReadBytes()} are called and so free up memory.
     * The default is {@code 16}.
     */

    /**
     * 设置读取次数，达到该次数后调用 {@link ByteBuf#discardSomeReadBytes()} 以释放内存。
     * 默认值为 {@code 16}。
     */
    public void setDiscardAfterReads(int discardAfterReads) {
        checkPositive(discardAfterReads, "discardAfterReads");
        this.discardAfterReads = discardAfterReads;
    }

    /**
     * Returns the actual number of readable bytes in the internal cumulative
     * buffer of this decoder. You usually do not need to rely on this value
     * to write a decoder. Use it only when you must use it at your own risk.
     * This method is a shortcut to {@link #internalBuffer() internalBuffer().readableBytes()}.
     */

    /**
     * 返回此解码器内部累积缓冲区中实际可读的字节数。通常您不需要依赖此值来编写解码器。
     * 仅在必须使用时自行承担风险。此方法是 {@link #internalBuffer() internalBuffer().readableBytes()} 的快捷方式。
     */
    protected int actualReadableBytes() {
        return internalBuffer().readableBytes();
    }

    /**
     * Returns the internal cumulative buffer of this decoder. You usually
     * do not need to access the internal buffer directly to write a decoder.
     * Use it only when you must use it at your own risk.
     */

    /**
     * 返回此解码器的内部累积缓冲区。通常你不需要直接访问内部缓冲区来编写解码器。
     * 仅在必须使用时使用，风险自负。
     */
    protected ByteBuf internalBuffer() {
        if (cumulation != null) {
            return cumulation;
        } else {
            return Unpooled.EMPTY_BUFFER;
        }
    }

    @Override
    public final void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        if (decodeState == STATE_CALLING_CHILD_DECODE) {
            decodeState = STATE_HANDLER_REMOVED_PENDING;
            return;
        }
        ByteBuf buf = cumulation;
        if (buf != null) {
            // Directly set this to null, so we are sure we not access it in any other method here anymore.
            // 直接将其设置为 null，以确保我们在此处的任何其他方法中都不会再访问它。
            cumulation = null;
            numReads = 0;
            int readable = buf.readableBytes();
            if (readable > 0) {
                ctx.fireChannelRead(buf);
                ctx.fireChannelReadComplete();
            } else {
                buf.release();
            }
        }
        handlerRemoved0(ctx);
    }

    /**
     * Gets called after the {@link ByteToMessageDecoder} was removed from the actual context and it doesn't handle
     * events anymore.
     */

    /**
     * 在 {@link ByteToMessageDecoder} 从实际上下文中移除且不再处理事件后调用。
     */
    protected void handlerRemoved0(ChannelHandlerContext ctx) throws Exception { }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            selfFiredChannelRead = true;
            CodecOutputList out = CodecOutputList.newInstance();
            try {
                first = cumulation == null;
                cumulation = cumulator.cumulate(ctx.alloc(),
                        first ? Unpooled.EMPTY_BUFFER : cumulation, (ByteBuf) msg);
                callDecode(ctx, cumulation, out);
            } catch (DecoderException e) {
                throw e;
            } catch (Exception e) {
                throw new DecoderException(e);
            } finally {
                try {
                    if (cumulation != null && !cumulation.isReadable()) {
                        numReads = 0;
                        try {
                            cumulation.release();
                        } catch (IllegalReferenceCountException e) {
                            //noinspection ThrowFromFinallyBlock
                            //noinspection ThrowFromFinallyBlock
                            throw new IllegalReferenceCountException(
                                    getClass().getSimpleName() + "#decode() might have released its input buffer, " +
                                            "or passed it down the pipeline without a retain() call, " +
                                            "which is not allowed.", e);
                        }
                        cumulation = null;
                    } else if (++numReads >= discardAfterReads) {
                        // We did enough reads already try to discard some bytes, so we not risk to see a OOME.
                        // 我们已经做了足够的读取操作，尝试丢弃一些字节，以免出现OOME的风险。
                        // See https://github.com/netty/netty/issues/4275
                        // 查看 https://github.com/netty/netty/issues/4275
                        numReads = 0;
                        discardSomeReadBytes();
                    }

                    int size = out.size();
                    firedChannelRead |= out.insertSinceRecycled();
                    fireChannelRead(ctx, out, size);
                } finally {
                    out.recycle();
                }
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    /**
     * Get {@code numElements} out of the {@link List} and forward these through the pipeline.
     */

    /**
     * 从 {@link List} 中获取 {@code numElements} 个元素并通过管道转发这些元素。
     */
    static void fireChannelRead(ChannelHandlerContext ctx, List<Object> msgs, int numElements) {
        if (msgs instanceof CodecOutputList) {
            fireChannelRead(ctx, (CodecOutputList) msgs, numElements);
        } else {
            for (int i = 0; i < numElements; i++) {
                ctx.fireChannelRead(msgs.get(i));
            }
        }
    }

    /**
     * Get {@code numElements} out of the {@link CodecOutputList} and forward these through the pipeline.
     */

    /**
     * 从 {@link CodecOutputList} 中获取 {@code numElements} 并通过管道转发这些元素。
     */
    static void fireChannelRead(ChannelHandlerContext ctx, CodecOutputList msgs, int numElements) {
        for (int i = 0; i < numElements; i ++) {
            ctx.fireChannelRead(msgs.getUnsafe(i));
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        numReads = 0;
        discardSomeReadBytes();
        if (selfFiredChannelRead && !firedChannelRead && !ctx.channel().config().isAutoRead()) {
            ctx.read();
        }
        firedChannelRead = false;
        ctx.fireChannelReadComplete();
    }

    protected final void discardSomeReadBytes() {
        if (cumulation != null && !first && cumulation.refCnt() == 1) {
            // discard some bytes if possible to make more room in the
            // 如果可能，丢弃一些字节以腾出更多空间
            // buffer but only if the refCnt == 1  as otherwise the user may have
            // 仅在 refCnt == 1 时进行缓冲，否则用户可能已经
            // used slice().retain() or duplicate().retain().
            // 使用了 slice().retain() 或 duplicate().retain()。
            //
            // See:
            // 参见：
            // - https://github.com/netty/netty/issues/2327

// - https://github.com/netty/netty/issues/2327
// 将这些Java注释以原格式翻译，保留注释字符。

            // - https://github.com/netty/netty/issues/1764

// 此问题与以下问题相关：
// - https://github.com/netty/netty/issues/1764

            cumulation.discardSomeReadBytes();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channelInputClosed(ctx, true);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof ChannelInputShutdownEvent) {
            // The decodeLast method is invoked when a channelInactive event is encountered.
            // decodeLast 方法在遇到 channelInactive 事件时被调用。
            // This method is responsible for ending requests in some situations and must be called
            // 该方法负责在某些情况下结束请求，必须被调用
            // when the input has been shutdown.
            // 当输入被关闭时。
            channelInputClosed(ctx, false);
        }
        super.userEventTriggered(ctx, evt);
    }

    private void channelInputClosed(ChannelHandlerContext ctx, boolean callChannelInactive) {
        CodecOutputList out = CodecOutputList.newInstance();
        try {
            channelInputClosed(ctx, out);
        } catch (DecoderException e) {
            throw e;
        } catch (Exception e) {
            throw new DecoderException(e);
        } finally {
            try {
                if (cumulation != null) {
                    cumulation.release();
                    cumulation = null;
                }
                int size = out.size();
                fireChannelRead(ctx, out, size);
                if (size > 0) {
                    // Something was read, call fireChannelReadComplete()
                    // 读取了某些内容，调用 fireChannelReadComplete()
                    ctx.fireChannelReadComplete();
                }
                if (callChannelInactive) {
                    ctx.fireChannelInactive();
                }
            } finally {
                // Recycle in all cases
                // 在所有情况下进行回收
                out.recycle();
            }
        }
    }

    /**
     * Called when the input of the channel was closed which may be because it changed to inactive or because of
     * {@link ChannelInputShutdownEvent}.
     */

    /**
     * 当通道的输入被关闭时调用，这可能是因为它变为非活动状态，或者是因为
     * {@link ChannelInputShutdownEvent}。
     */
    void channelInputClosed(ChannelHandlerContext ctx, List<Object> out) throws Exception {
        if (cumulation != null) {
            callDecode(ctx, cumulation, out);
            // If callDecode(...) removed the handle from the pipeline we should not call decodeLast(...) as this would
            // 如果 callDecode(...) 从管道中移除了句柄，我们不应该调用 decodeLast(...)，因为这会
            // be unexpected.
            // 出人意料。
            if (!ctx.isRemoved()) {
                // Use Unpooled.EMPTY_BUFFER if cumulation become null after calling callDecode(...).
                // 如果在调用 callDecode(...) 后 cumulation 变为 null，则使用 Unpooled.EMPTY_BUFFER。
                // See https://github.com/netty/netty/issues/10802.
                // 参见 https://github.com/netty/netty/issues/10802。
                ByteBuf buffer = cumulation == null ? Unpooled.EMPTY_BUFFER : cumulation;
                decodeLast(ctx, buffer, out);
            }
        } else {
            decodeLast(ctx, Unpooled.EMPTY_BUFFER, out);
        }
    }

    /**
     * Called once data should be decoded from the given {@link ByteBuf}. This method will call
     * {@link #decode(ChannelHandlerContext, ByteBuf, List)} as long as decoding should take place.
     *
     * @param ctx           the {@link ChannelHandlerContext} which this {@link ByteToMessageDecoder} belongs to
     * @param in            the {@link ByteBuf} from which to read data
     * @param out           the {@link List} to which decoded messages should be added
     */

    /**
     * 当需要从给定的 {@link ByteBuf} 中解码数据时调用。此方法将在需要解码时调用
     * {@link #decode(ChannelHandlerContext, ByteBuf, List)}。
     *
     * @param ctx           该 {@link ByteToMessageDecoder} 所属的 {@link ChannelHandlerContext}
     * @param in            从中读取数据的 {@link ByteBuf}
     * @param out           解码后的消息应添加到的 {@link List}
     */
    protected void callDecode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        try {
            while (in.isReadable()) {
                final int outSize = out.size();

                if (outSize > 0) {
                    fireChannelRead(ctx, out, outSize);
                    out.clear();

                    // Check if this handler was removed before continuing with decoding.

                    // 在继续解码之前，检查此处理程序是否已被移除。
                    // If it was removed, it is not safe to continue to operate on the buffer.
                    // 如果它被移除了，继续操作缓冲区是不安全的。
                    //
                    // See:
                    // 参见：
                    // - https://github.com/netty/netty/issues/4635

// 问题描述：
// 当使用 `HttpObjectAggregator` 时，如果请求体过大，Netty 会抛出 `TooLongFrameException` 异常。
// 但是，这个异常并没有被正确地传递给 `ChannelInboundHandler` 的 `exceptionCaught` 方法。
// 这导致开发者无法捕获并处理这个异常，从而无法向客户端返回一个合适的错误响应。

// 重现步骤：
// 1. 配置一个 `HttpObjectAggregator`，并设置一个较小的 `maxContentLength`。
// 2. 发送一个请求体超过 `maxContentLength` 的 HTTP 请求。
// 3. 观察 `ChannelInboundHandler` 的 `exceptionCaught` 方法是否被调用。

// 预期行为：
// `TooLongFrameException` 应该被传递给 `ChannelInboundHandler` 的 `exceptionCaught` 方法，
// 以便开发者可以捕获并处理这个异常。

// 实际行为：
// `TooLongFrameException` 没有被传递给 `ChannelInboundHandler` 的 `exceptionCaught` 方法，
// 导致开发者无法捕获并处理这个异常。

                    if (ctx.isRemoved()) {
                        break;
                    }
                }

                int oldInputLength = in.readableBytes();
                decodeRemovalReentryProtection(ctx, in, out);

                // Check if this handler was removed before continuing the loop.

                // 在继续循环之前检查此处理程序是否已被移除。
                // If it was removed, it is not safe to continue to operate on the buffer.
                // 如果它被移除了，继续操作缓冲区是不安全的。
                //
                // See https://github.com/netty/netty/issues/1664
                // 参见 https://github.com/netty/netty/issues/1664
                if (ctx.isRemoved()) {
                    break;
                }

                if (out.isEmpty()) {
                    if (oldInputLength == in.readableBytes()) {
                        break;
                    } else {
                        continue;
                    }
                }

                if (oldInputLength == in.readableBytes()) {
                    throw new DecoderException(
                            StringUtil.simpleClassName(getClass()) +
                                    ".decode() did not read anything but decoded a message.");
                }

                if (isSingleDecode()) {
                    break;
                }
            }
        } catch (DecoderException e) {
            throw e;
        } catch (Exception cause) {
            throw new DecoderException(cause);
        }
    }

    /**
     * Decode the from one {@link ByteBuf} to an other. This method will be called till either the input
     * {@link ByteBuf} has nothing to read when return from this method or till nothing was read from the input
     * {@link ByteBuf}.
     *
     * @param ctx           the {@link ChannelHandlerContext} which this {@link ByteToMessageDecoder} belongs to
     * @param in            the {@link ByteBuf} from which to read data
     * @param out           the {@link List} to which decoded messages should be added
     * @throws Exception    is thrown if an error occurs
     */

    /**
     * 将一个 {@link ByteBuf} 解码到另一个。此方法将被调用，直到从该方法返回时输入 {@link ByteBuf} 没有可读内容，或者直到从输入 {@link ByteBuf} 中未读取任何内容。
     *
     * @param ctx           此 {@link ByteToMessageDecoder} 所属的 {@link ChannelHandlerContext}
     * @param in            从中读取数据的 {@link ByteBuf}
     * @param out           应添加解码消息的 {@link List}
     * @throws Exception    如果发生错误，则抛出异常
     */
    protected abstract void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception;

    /**
     * Decode the from one {@link ByteBuf} to an other. This method will be called till either the input
     * {@link ByteBuf} has nothing to read when return from this method or till nothing was read from the input
     * {@link ByteBuf}.
     *
     * @param ctx           the {@link ChannelHandlerContext} which this {@link ByteToMessageDecoder} belongs to
     * @param in            the {@link ByteBuf} from which to read data
     * @param out           the {@link List} to which decoded messages should be added
     * @throws Exception    is thrown if an error occurs
     */

    /**
     * 将一个 {@link ByteBuf} 解码到另一个。此方法将被调用，直到从该方法返回时输入 {@link ByteBuf} 没有可读内容，或者直到从输入 {@link ByteBuf} 中未读取任何内容。
     *
     * @param ctx           此 {@link ByteToMessageDecoder} 所属的 {@link ChannelHandlerContext}
     * @param in            从中读取数据的 {@link ByteBuf}
     * @param out           应添加解码消息的 {@link List}
     * @throws Exception    如果发生错误，则抛出异常
     */
    final void decodeRemovalReentryProtection(ChannelHandlerContext ctx, ByteBuf in, List<Object> out)
            throws Exception {
        decodeState = STATE_CALLING_CHILD_DECODE;
        try {
            decode(ctx, in, out);
        } finally {
            boolean removePending = decodeState == STATE_HANDLER_REMOVED_PENDING;
            decodeState = STATE_INIT;
            if (removePending) {
                fireChannelRead(ctx, out, out.size());
                out.clear();
                handlerRemoved(ctx);
            }
        }
    }

    /**
     * Is called one last time when the {@link ChannelHandlerContext} goes in-active. Which means the
     * {@link #channelInactive(ChannelHandlerContext)} was triggered.
     *
     * By default, this will just call {@link #decode(ChannelHandlerContext, ByteBuf, List)} but sub-classes may
     * override this for some special cleanup operation.
     */

    /**
     * 当 {@link ChannelHandlerContext} 变为非活动状态时最后一次被调用。这意味着
     * {@link #channelInactive(ChannelHandlerContext)} 已被触发。
     *
     * 默认情况下，这将仅调用 {@link #decode(ChannelHandlerContext, ByteBuf, List)}，但子类可以
     * 重写此方法以执行一些特殊的清理操作。
     */
    protected void decodeLast(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.isReadable()) {
            // Only call decode() if there is something left in the buffer to decode.
            // 只有在缓冲区中还有内容需要解码时才调用 decode()。
            // See https://github.com/netty/netty/issues/4386
            // 参见 https://github.com/netty/netty/issues/4386
            decodeRemovalReentryProtection(ctx, in, out);
        }
    }

    static ByteBuf expandCumulation(ByteBufAllocator alloc, ByteBuf oldCumulation, ByteBuf in) {
        int oldBytes = oldCumulation.readableBytes();
        int newBytes = in.readableBytes();
        int totalBytes = oldBytes + newBytes;
        ByteBuf newCumulation = alloc.buffer(alloc.calculateNewCapacity(totalBytes, MAX_VALUE));
        ByteBuf toRelease = newCumulation;
        try {
            // This avoids redundant checks and stack depth compared to calling writeBytes(...)
            // 与调用 writeBytes(...) 相比，这避免了冗余检查和堆栈深度
            newCumulation.setBytes(0, oldCumulation, oldCumulation.readerIndex(), oldBytes)
                .setBytes(oldBytes, in, in.readerIndex(), newBytes)
                .writerIndex(totalBytes);
            in.readerIndex(in.writerIndex());
            toRelease = oldCumulation;
            return newCumulation;
        } finally {
            toRelease.release();
        }
    }

    /**
     * Cumulate {@link ByteBuf}s.
     */

    /**
     * 累积 {@link ByteBuf}s。
     */
    public interface Cumulator {
        /**
         * Cumulate the given {@link ByteBuf}s and return the {@link ByteBuf} that holds the cumulated bytes.
         * The implementation is responsible to correctly handle the life-cycle of the given {@link ByteBuf}s and so
         * call {@link ByteBuf#release()} if a {@link ByteBuf} is fully consumed.
         */
        /**
         * 累积给定的 {@link ByteBuf}s 并返回持有累积字节的 {@link ByteBuf}。
         * 实现负责正确处理给定 {@link ByteBuf}s 的生命周期，并在 {@link ByteBuf} 完全消耗时调用 {@link ByteBuf#release()}。
         */
        ByteBuf cumulate(ByteBufAllocator alloc, ByteBuf cumulation, ByteBuf in);
    }
}
