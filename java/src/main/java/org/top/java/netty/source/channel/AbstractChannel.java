
package org.top.java.netty.source.channel;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.ChannelOutputShutdownEvent;
import io.netty.channel.socket.ChannelOutputShutdownException;
import io.netty.util.DefaultAttributeMap;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.UnstableApi;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * A skeletal {@link Channel} implementation.
 */

/**
 * 一个骨架 {@link Channel} 实现。
 */
public abstract class AbstractChannel extends DefaultAttributeMap implements Channel {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractChannel.class);

    private final Channel parent;
    private final ChannelId id;
    private final Unsafe unsafe;
    private final DefaultChannelPipeline pipeline;
    private final VoidChannelPromise unsafeVoidPromise = new VoidChannelPromise(this, false);
    private final CloseFuture closeFuture = new CloseFuture(this);

    private volatile SocketAddress localAddress;
    private volatile SocketAddress remoteAddress;
    private volatile EventLoop eventLoop;
    private volatile boolean registered;
    private boolean closeInitiated;
    private Throwable initialCloseCause;

    /** Cache for the string representation of this channel */

    /** 该通道的字符串表示的缓存 */
    private boolean strValActive;
    private String strVal;

    /**
     * Creates a new instance.
     *
     * @param parent
     *        the parent of this channel. {@code null} if there's no parent.
     */

    /**
     * 创建新实例。
     *
     * @param parent
     *        此通道的父级。如果没有父级，则为 {@code null}。
     */
    protected AbstractChannel(Channel parent) {
        this.parent = parent;
        id = newId();
        unsafe = newUnsafe();
        pipeline = newChannelPipeline();
    }

    /**
     * Creates a new instance.
     *
     * @param parent
     *        the parent of this channel. {@code null} if there's no parent.
     */

    /**
     * 创建新实例。
     *
     * @param parent
     *        此通道的父级。如果没有父级，则为 {@code null}。
     */
    protected AbstractChannel(Channel parent, ChannelId id) {
        this.parent = parent;
        this.id = id;
        unsafe = newUnsafe();
        pipeline = newChannelPipeline();
    }

    protected final int maxMessagesPerWrite() {
        ChannelConfig config = config();
        if (config instanceof DefaultChannelConfig) {
            return ((DefaultChannelConfig) config).getMaxMessagesPerWrite();
        }
        Integer value = config.getOption(ChannelOption.MAX_MESSAGES_PER_WRITE);
        if (value == null) {
            return Integer.MAX_VALUE;
        }
        return value;
    }

    @Override
    public final ChannelId id() {
        return id;
    }

    /**
     * Returns a new {@link DefaultChannelId} instance. Subclasses may override this method to assign custom
     * {@link ChannelId}s to {@link Channel}s that use the {@link AbstractChannel#AbstractChannel(Channel)} constructor.
     */

    /**
     * 返回一个新的 {@link DefaultChannelId} 实例。子类可以重写此方法，以便为使用 {@link AbstractChannel#AbstractChannel(Channel)} 构造函数的 {@link Channel} 分配自定义的 {@link ChannelId}。
     */
    protected ChannelId newId() {
        return DefaultChannelId.newInstance();
    }

    /**
     * Returns a new {@link DefaultChannelPipeline} instance.
     */

    /**
     * 返回一个新的 {@link DefaultChannelPipeline} 实例。
     */
    protected DefaultChannelPipeline newChannelPipeline() {
        return new DefaultChannelPipeline(this);
    }

    @Override
    public boolean isWritable() {
        ChannelOutboundBuffer buf = unsafe.outboundBuffer();
        return buf != null && buf.isWritable();
    }

    @Override
    public long bytesBeforeUnwritable() {
        ChannelOutboundBuffer buf = unsafe.outboundBuffer();
        // isWritable() is currently assuming if there is no outboundBuffer then the channel is not writable.
        // isWritable() 当前假设如果没有 outboundBuffer，则通道不可写。
        // We should be consistent with that here.
        // 我们应该在这里保持一致。
        return buf != null ? buf.bytesBeforeUnwritable() : 0;
    }

    @Override
    public long bytesBeforeWritable() {
        ChannelOutboundBuffer buf = unsafe.outboundBuffer();
        // isWritable() is currently assuming if there is no outboundBuffer then the channel is not writable.
        // isWritable() 当前假设如果没有 outboundBuffer，则通道不可写。
        // We should be consistent with that here.
        // 我们应该在这里保持一致。
        return buf != null ? buf.bytesBeforeWritable() : Long.MAX_VALUE;
    }

    @Override
    public Channel parent() {
        return parent;
    }

    @Override
    public ChannelPipeline pipeline() {
        return pipeline;
    }

    @Override
    public ByteBufAllocator alloc() {
        return config().getAllocator();
    }

    @Override
    public EventLoop eventLoop() {
        EventLoop eventLoop = this.eventLoop;
        if (eventLoop == null) {
            throw new IllegalStateException("channel not registered to an event loop");
        }
        return eventLoop;
    }

    @Override
    public SocketAddress localAddress() {
        SocketAddress localAddress = this.localAddress;
        if (localAddress == null) {
            try {
                this.localAddress = localAddress = unsafe().localAddress();
            } catch (Error e) {
                throw e;
            } catch (Throwable t) {
                // Sometimes fails on a closed socket in Windows.
                // 有时在Windows上会在关闭的套接字上失败。
                return null;
            }
        }
        return localAddress;
    }

    /**
     * @deprecated no use-case for this.
     */

    /**
     * @deprecated 无使用场景。
     */
    @Deprecated
    protected void invalidateLocalAddress() {
        localAddress = null;
    }

    @Override
    public SocketAddress remoteAddress() {
        SocketAddress remoteAddress = this.remoteAddress;
        if (remoteAddress == null) {
            try {
                this.remoteAddress = remoteAddress = unsafe().remoteAddress();
            } catch (Error e) {
                throw e;
            } catch (Throwable t) {
                // Sometimes fails on a closed socket in Windows.
                // 有时在Windows上会在关闭的套接字上失败。
                return null;
            }
        }
        return remoteAddress;
    }

    /**
     * @deprecated no use-case for this.
     */

    /**
     * @deprecated 无使用场景。
     */
    @Deprecated
    protected void invalidateRemoteAddress() {
        remoteAddress = null;
    }

    @Override
    public boolean isRegistered() {
        return registered;
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress) {
        return pipeline.bind(localAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress) {
        return pipeline.connect(remoteAddress);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress) {
        return pipeline.connect(remoteAddress, localAddress);
    }

    @Override
    public ChannelFuture disconnect() {
        return pipeline.disconnect();
    }

    @Override
    public ChannelFuture close() {
        return pipeline.close();
    }

    @Override
    public ChannelFuture deregister() {
        return pipeline.deregister();
    }

    @Override
    public Channel flush() {
        pipeline.flush();
        return this;
    }

    @Override
    public ChannelFuture bind(SocketAddress localAddress, ChannelPromise promise) {
        return pipeline.bind(localAddress, promise);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, ChannelPromise promise) {
        return pipeline.connect(remoteAddress, promise);
    }

    @Override
    public ChannelFuture connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        return pipeline.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public ChannelFuture disconnect(ChannelPromise promise) {
        return pipeline.disconnect(promise);
    }

    @Override
    public ChannelFuture close(ChannelPromise promise) {
        return pipeline.close(promise);
    }

    @Override
    public ChannelFuture deregister(ChannelPromise promise) {
        return pipeline.deregister(promise);
    }

    @Override
    public Channel read() {
        pipeline.read();
        return this;
    }

    @Override
    public ChannelFuture write(Object msg) {
        return pipeline.write(msg);
    }

    @Override
    public ChannelFuture write(Object msg, ChannelPromise promise) {
        return pipeline.write(msg, promise);
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg) {
        return pipeline.writeAndFlush(msg);
    }

    @Override
    public ChannelFuture writeAndFlush(Object msg, ChannelPromise promise) {
        return pipeline.writeAndFlush(msg, promise);
    }

    @Override
    public ChannelPromise newPromise() {
        return pipeline.newPromise();
    }

    @Override
    public ChannelProgressivePromise newProgressivePromise() {
        return pipeline.newProgressivePromise();
    }

    @Override
    public ChannelFuture newSucceededFuture() {
        return pipeline.newSucceededFuture();
    }

    @Override
    public ChannelFuture newFailedFuture(Throwable cause) {
        return pipeline.newFailedFuture(cause);
    }

    @Override
    public ChannelFuture closeFuture() {
        return closeFuture;
    }

    @Override
    public Unsafe unsafe() {
        return unsafe;
    }

    /**
     * Create a new {@link AbstractUnsafe} instance which will be used for the life-time of the {@link Channel}
     */

    /**
     * 创建一个新的 {@link AbstractUnsafe} 实例，该实例将用于 {@link Channel} 的整个生命周期
     */
    protected abstract AbstractUnsafe newUnsafe();

    /**
     * Returns the ID of this channel.
     */

    /**
     * 返回此通道的ID。
     */
    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    /**
     * Returns {@code true} if and only if the specified object is identical
     * with this channel (i.e: {@code this == o}).
     */

    /**
     * 当且仅当指定对象与此通道相同时返回 {@code true}（即：{@code this == o}）。
     */
    @Override
    public final boolean equals(Object o) {
        return this == o;
    }

    @Override
    public final int compareTo(Channel o) {
        if (this == o) {
            return 0;
        }

        return id().compareTo(o.id());
    }

    /**
     * Returns the {@link String} representation of this channel.  The returned
     * string contains the {@linkplain #hashCode() ID}, {@linkplain #localAddress() local address},
     * and {@linkplain #remoteAddress() remote address} of this channel for
     * easier identification.
     */

    /**
     * 返回此通道的 {@link String} 表示形式。返回的字符串包含此通道的
     * {@linkplain #hashCode() ID}、{@linkplain #localAddress() 本地地址}、
     * 和 {@linkplain #remoteAddress() 远程地址}，以便于识别。
     */
    @Override
    public String toString() {
        boolean active = isActive();
        if (strValActive == active && strVal != null) {
            return strVal;
        }

        SocketAddress remoteAddr = remoteAddress();
        SocketAddress localAddr = localAddress();
        if (remoteAddr != null) {
            StringBuilder buf = new StringBuilder(96)
                .append("[id: 0x")
                .append(id.asShortText())
                .append(", L:")
                .append(localAddr)
                .append(active? " - " : " ! ")
                .append("R:")
                .append(remoteAddr)
                .append(']');
            strVal = buf.toString();
        } else if (localAddr != null) {
            StringBuilder buf = new StringBuilder(64)
                .append("[id: 0x")
                .append(id.asShortText())
                .append(", L:")
                .append(localAddr)
                .append(']');
            strVal = buf.toString();
        } else {
            StringBuilder buf = new StringBuilder(16)
                .append("[id: 0x")
                .append(id.asShortText())
                .append(']');
            strVal = buf.toString();
        }

        strValActive = active;
        return strVal;
    }

    @Override
    public final ChannelPromise voidPromise() {
        return pipeline.voidPromise();
    }

    /**
     * {@link Unsafe} implementation which sub-classes must extend and use.
     */

    /**
     * {@link Unsafe} 实现，子类必须扩展并使用。
     */
    protected abstract class AbstractUnsafe implements Unsafe {

        private volatile ChannelOutboundBuffer outboundBuffer = new ChannelOutboundBuffer(AbstractChannel.this);
        private RecvByteBufAllocator.Handle recvHandle;
        private boolean inFlush0;
        /** true if the channel has never been registered, false otherwise */
        /** 如果频道从未被注册过，则返回true，否则返回false */
        private boolean neverRegistered = true;

        private void assertEventLoop() {
            assert !registered || eventLoop.inEventLoop();
        }

        @Override
        public RecvByteBufAllocator.Handle recvBufAllocHandle() {
            if (recvHandle == null) {
                recvHandle = config().getRecvByteBufAllocator().newHandle();
            }
            return recvHandle;
        }

        @Override
        public final ChannelOutboundBuffer outboundBuffer() {
            return outboundBuffer;
        }

        @Override
        public final SocketAddress localAddress() {
            return localAddress0();
        }

        @Override
        public final SocketAddress remoteAddress() {
            return remoteAddress0();
        }

        @Override
        public final void register(EventLoop eventLoop, final ChannelPromise promise) {
            ObjectUtil.checkNotNull(eventLoop, "eventLoop");
            if (isRegistered()) {
                promise.setFailure(new IllegalStateException("registered to an event loop already"));
                return;
            }
            if (!isCompatible(eventLoop)) {
                promise.setFailure(
                        new IllegalStateException("incompatible event loop type: " + eventLoop.getClass().getName()));
                return;
            }

            AbstractChannel.this.eventLoop = eventLoop;

            if (eventLoop.inEventLoop()) {
                register0(promise);
            } else {
                try {
                    eventLoop.execute(new Runnable() {
                        @Override
                        public void run() {
                            register0(promise);
                        }
                    });
                } catch (Throwable t) {
                    logger.warn(
                            "Force-closing a channel whose registration task was not accepted by an event loop: {}",
                            AbstractChannel.this, t);
                    closeForcibly();
                    closeFuture.setClosed();
                    safeSetFailure(promise, t);
                }
            }
        }

        private void register0(ChannelPromise promise) {
            try {
                // check if the channel is still open as it could be closed in the mean time when the register
                // 检查通道是否仍然打开，因为在注册过程中它可能已经被关闭
                // call was outside of the eventLoop
                // 调用在eventLoop之外
                if (!promise.setUncancellable() || !ensureOpen(promise)) {
                    return;
                }
                boolean firstRegistration = neverRegistered;
                doRegister();
                neverRegistered = false;
                registered = true;

                // Ensure we call handlerAdded(...) before we actually notify the promise. This is needed as the

                // 确保在实际通知 promise 之前调用 handlerAdded(...)。这是必要的，因为
                // user may already fire events through the pipeline in the ChannelFutureListener.
                // 用户可能已经通过 ChannelFutureListener 中的管道触发了事件。
                pipeline.invokeHandlerAddedIfNeeded();

                safeSetSuccess(promise);
                pipeline.fireChannelRegistered();
                // Only fire a channelActive if the channel has never been registered. This prevents firing
                // 仅在通道从未注册过时触发 channelActive。这可以防止重复触发
                // multiple channel actives if the channel is deregistered and re-registered.
                // 如果通道被注销并重新注册，则多个通道处于活动状态。
                if (isActive()) {
                    if (firstRegistration) {
                        pipeline.fireChannelActive();
                    } else if (config().isAutoRead()) {
                        // This channel was registered before and autoRead() is set. This means we need to begin read
                        // 该通道之前已注册，并且 autoRead() 已设置。这意味着我们需要开始读取
                        // again so that we process inbound data.
                        // 再次处理入站数据。
                        //
                        // See https://github.com/netty/netty/issues/4805

                        // 请参阅 https://github.com/netty/netty/issues/4805

                        beginRead();
                    }
                }
            } catch (Throwable t) {
                // Close the channel directly to avoid FD leak.
                // 直接关闭通道以避免文件描述符泄漏。
                closeForcibly();
                closeFuture.setClosed();
                safeSetFailure(promise, t);
            }
        }

        @Override
        public final void bind(final SocketAddress localAddress, final ChannelPromise promise) {
            assertEventLoop();

            if (!promise.setUncancellable() || !ensureOpen(promise)) {
                return;
            }

            // See: https://github.com/netty/netty/issues/576


            // 该问题是由于在关闭连接时，ChannelOutboundBuffer 中的消息被释放，但 ChannelOutboundBuffer 本身并未被清空。
            // 这导致了在重新使用 Channel 时，旧的未发送的消息仍然存在于 ChannelOutboundBuffer 中，从而导致问题。
            // 解决方法是在关闭连接时清空 ChannelOutboundBuffer。

            if (Boolean.TRUE.equals(config().getOption(ChannelOption.SO_BROADCAST)) &&
                localAddress instanceof InetSocketAddress &&
                !((InetSocketAddress) localAddress).getAddress().isAnyLocalAddress() &&
                !PlatformDependent.isWindows() && !PlatformDependent.maybeSuperUser()) {
                // Warn a user about the fact that a non-root user can't receive a
                // 警告用户非root用户无法接收
                // broadcast packet on *nix if the socket is bound on non-wildcard address.
                // 在 *nix 系统上，如果套接字绑定在非通配符地址上，则广播数据包。
                logger.warn(
                        "A non-root user can't receive a broadcast packet if the socket " +
                        "is not bound to a wildcard address; binding to a non-wildcard " +
                        "address (" + localAddress + ") anyway as requested.");
            }

            boolean wasActive = isActive();
            try {
                doBind(localAddress);
            } catch (Throwable t) {
                safeSetFailure(promise, t);
                closeIfClosed();
                return;
            }

            if (!wasActive && isActive()) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        pipeline.fireChannelActive();
                    }
                });
            }

            safeSetSuccess(promise);
        }

        @Override
        public final void disconnect(final ChannelPromise promise) {
            assertEventLoop();

            if (!promise.setUncancellable()) {
                return;
            }

            boolean wasActive = isActive();
            try {
                doDisconnect();
                // Reset remoteAddress and localAddress
                // 重置remoteAddress和localAddress
                remoteAddress = null;
                localAddress = null;
            } catch (Throwable t) {
                safeSetFailure(promise, t);
                closeIfClosed();
                return;
            }

            if (wasActive && !isActive()) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        pipeline.fireChannelInactive();
                    }
                });
            }

            safeSetSuccess(promise);
            closeIfClosed(); // doDisconnect() might have closed the channel
        }

        @Override
        public void close(final ChannelPromise promise) {
            assertEventLoop();

        }

        /**
         * Shutdown the output portion of the corresponding {@link Channel}.
         * For example this will clean up the {@link ChannelOutboundBuffer} and not allow any more writes.
         */

        /**
         * 关闭对应 {@link Channel} 的输出部分。
         * 例如，这将清理 {@link ChannelOutboundBuffer} 并且不再允许任何写入。
         */
        @UnstableApi
        public final void shutdownOutput(final ChannelPromise promise) {
            assertEventLoop();
            shutdownOutput(promise, null);
        }

        /**
         * Shutdown the output portion of the corresponding {@link Channel}.
         * For example this will clean up the {@link ChannelOutboundBuffer} and not allow any more writes.
         * @param cause The cause which may provide rational for the shutdown.
         */

        /**
         * 关闭对应 {@link Channel} 的输出部分。
         * 例如，这将清理 {@link ChannelOutboundBuffer} 并且不再允许任何写入。
         * @param cause 可能提供关闭原因的原因。
         */
        private void shutdownOutput(final ChannelPromise promise, Throwable cause) {
            if (!promise.setUncancellable()) {
                return;
            }

            final ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
            if (outboundBuffer == null) {
                promise.setFailure(new ClosedChannelException());
                return;
            }
            this.outboundBuffer = null; // Disallow adding any messages and flushes to outboundBuffer.

            final Throwable shutdownCause = cause == null ?
                    new ChannelOutputShutdownException("Channel output shutdown") :
                    new ChannelOutputShutdownException("Channel output shutdown", cause);

            // When a side enables SO_LINGER and calls showdownOutput(...) to start TCP half-closure

            // 当一端启用SO_LINGER并调用shutdownOutput(...)以启动TCP半关闭时
            // we can not call doDeregister here because we should ensure this side in fin_wait2 state
            // 我们不能在这里调用 doDeregister，因为需要确保这一端处于 fin_wait2 状态
            // can still receive and process the data which is send by another side in the close_wait state。
            // 在 close_wait 状态下仍然可以接收和处理由另一方发送的数据。
            // See https://github.com/netty/netty/issues/11981
            // 参见 https://github.com/netty/netty/issues/11981
            try {
                // The shutdown function does not block regardless of the SO_LINGER setting on the socket
                // shutdown函数不会阻塞，无论套接字上的SO_LINGER设置如何
                // so we don't need to use GlobalEventExecutor to execute the shutdown
                // 所以我们不需要使用 GlobalEventExecutor 来执行关闭
                doShutdownOutput();
                promise.setSuccess();
            } catch (Throwable err) {
                promise.setFailure(err);
            } finally {
                closeOutboundBufferForShutdown(pipeline, outboundBuffer, shutdownCause);
            }
        }

        private void closeOutboundBufferForShutdown(
                ChannelPipeline pipeline, ChannelOutboundBuffer buffer, Throwable cause) {
            buffer.failFlushed(cause, false);
            buffer.close(cause, true);
            pipeline.fireUserEventTriggered(ChannelOutputShutdownEvent.INSTANCE);
        }

        private void close(final ChannelPromise promise, final Throwable cause,
                           final ClosedChannelException closeCause, final boolean notify) {
            if (!promise.setUncancellable()) {
                return;
            }

            if (closeInitiated) {
                if (closeFuture.isDone()) {
                    // Closed already.
                    // 已关闭。
                    safeSetSuccess(promise);
                }
                return;
            }

            closeInitiated = true;

            final boolean wasActive = isActive();
            final ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
            this.outboundBuffer = null; // Disallow adding any messages and flushes to outboundBuffer.
            Executor closeExecutor = prepareToClose();
            if (closeExecutor != null) {
                closeExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Execute the close.
                            // 执行关闭。
                            doClose0(promise);
                        } finally {
                            // Call invokeLater so closeAndDeregister is executed in the EventLoop again!
                            // 调用 invokeLater 以便 closeAndDeregister 再次在 EventLoop 中执行！
                            invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    if (outboundBuffer != null) {
                                        // Fail all the queued messages
                                        // 使所有排队的消息失败
                                        outboundBuffer.failFlushed(cause, notify);
                                        outboundBuffer.close(closeCause);
                                    }
                                    fireChannelInactiveAndDeregister(wasActive);
                                }
                            });
                        }
                    }
                });
            } else {
                try {
                    // Close the channel and fail the queued messages in all cases.
                    // 关闭通道并在所有情况下使排队消息失败。
                    doClose0(promise);
                } finally {
                    if (outboundBuffer != null) {
                        // Fail all the queued messages.
                        // 使所有排队的消息失败。
                        outboundBuffer.failFlushed(cause, notify);
                        outboundBuffer.close(closeCause);
                    }
                }
                if (inFlush0) {
                    invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            fireChannelInactiveAndDeregister(wasActive);
                        }
                    });
                } else {
                    fireChannelInactiveAndDeregister(wasActive);
                }
            }
        }

        private void doClose0(ChannelPromise promise) {
            try {
                doClose();
                closeFuture.setClosed();
                safeSetSuccess(promise);
            } catch (Throwable t) {
                closeFuture.setClosed();
                safeSetFailure(promise, t);
            }
        }

        private void fireChannelInactiveAndDeregister(final boolean wasActive) {
            deregister(voidPromise(), wasActive && !isActive());
        }

        @Override
        public final void closeForcibly() {
            assertEventLoop();

            try {
                doClose();
            } catch (Exception e) {
                logger.warn("Failed to close a channel.", e);
            }
        }

        @Override
        public final void deregister(final ChannelPromise promise) {
            assertEventLoop();

            deregister(promise, false);
        }

        private void deregister(final ChannelPromise promise, final boolean fireChannelInactive) {
            if (!promise.setUncancellable()) {
                return;
            }

            if (!registered) {
                safeSetSuccess(promise);
                return;
            }

            // As a user may call deregister() from within any method while doing processing in the ChannelPipeline,

            // 由于用户可能在任何方法中调用deregister()，同时在ChannelPipeline中进行处理，
            // we need to ensure we do the actual deregister operation later. This is needed as for example,
            // 我们需要确保稍后执行实际的注销操作。这是必需的，因为例如，
            // we may be in the ByteToMessageDecoder.callDecode(...) method and so still try to do processing in
            // 我们可能位于 ByteToMessageDecoder.callDecode(...) 方法中，因此仍然尝试进行处理
            // the old EventLoop while the user already registered the Channel to a new EventLoop. Without delay,
            // 旧的EventLoop，而用户已经将Channel注册到新的EventLoop。没有延迟，
            // the deregister operation this could lead to have a handler invoked by different EventLoop and so
            // 注销操作可能导致处理程序被不同的EventLoop调用，因此
            // threads.
            // 线程。
            //
            // See:
            // 参见：
            // https://github.com/netty/netty/issues/4435

            // 问题描述：
            // 在使用 Netty 的 HTTP/2 客户端时，当服务器发送一个带有 `RST_STREAM` 帧的响应时，客户端会抛出 `Http2Exception$StreamException` 异常。
            // 这会导致客户端无法正确处理服务器的响应，并且无法继续处理后续的请求。

            // 复现步骤：
            // 1. 启动一个支持 HTTP/2 的服务器。
            // 2. 使用 Netty 的 HTTP/2 客户端向服务器发送请求。
            // 3. 服务器响应时发送一个 `RST_STREAM` 帧。
            // 4. 客户端抛出 `Http2Exception$StreamException` 异常。

            // 预期行为：
            // 客户端应该能够正确处理 `RST_STREAM` 帧，并继续处理后续请求。

            // 实际行为：
            // 客户端抛出 `Http2Exception$StreamException` 异常，无法继续处理后续请求。

            // 相关代码：
            // 以下是触发异常的代码片段：
            // Http2StreamChannelBootstrap bootstrap = new Http2StreamChannelBootstrap(channel);
            // bootstrap.handler(new ChannelInitializer<Channel>() {
            //     @Override
            //     protected void initChannel(Channel ch) throws Exception {
            //         ch.pipeline().addLast(new Http2FrameCodecBuilder(true).build());
            //         ch.pipeline().addLast(new SimpleChannelInboundHandler<Http2DataFrame>() {
            //             @Override
            //             protected void channelRead0(ChannelHandlerContext ctx, Http2DataFrame msg) throws Exception {
            //                 // 处理数据帧
            //             }
            //         });
            //     }
            // });
            // ChannelFuture future = bootstrap.open().sync();
            // future.channel().writeAndFlush(new DefaultHttp2DataFrame(Unpooled.wrappedBuffer("Hello".getBytes())));

            // 环境信息：
            // - Netty 版本：4.1.34.Final
            // - Java 版本：1.8.0_191
            // - 操作系统：macOS Mojave 10.14.2

            invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        doDeregister();
                    } catch (Throwable t) {
                        logger.warn("Unexpected exception occurred while deregistering a channel.", t);
                    } finally {
                        if (fireChannelInactive) {
                            pipeline.fireChannelInactive();
                        }
                        // Some transports like local and AIO does not allow the deregistration of
                        // 一些传输方式如local和AIO不允许注销
                        // an open channel.  Their doDeregister() calls close(). Consequently,
                        // 一个开放通道。它们的 doDeregister() 方法调用了 close()。因此，
                        // close() calls deregister() again - no need to fire channelUnregistered, so check
                        // close() 再次调用了 deregister() - 不需要触发 channelUnregistered，因此检查
                        // if it was registered.
                        // 如果它已被注册。
                        if (registered) {
                            registered = false;
                            pipeline.fireChannelUnregistered();
                        }
                        safeSetSuccess(promise);
                    }
                }
            });
        }

        @Override
        public final void beginRead() {
            assertEventLoop();

            try {
                doBeginRead();
            } catch (final Exception e) {
                invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        pipeline.fireExceptionCaught(e);
                    }
                });
                close(voidPromise());
            }
        }

        @Override
        public final void write(Object msg, ChannelPromise promise) {
            assertEventLoop();

            ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
            if (outboundBuffer == null) {
                try {
                    // release message now to prevent resource-leak
                    // 立即释放消息以防止资源泄漏
                    ReferenceCountUtil.release(msg);
                } finally {
                    // If the outboundBuffer is null we know the channel was closed and so
                    // 如果 outboundBuffer 为 null，我们知道通道已关闭，因此
                    // need to fail the future right away. If it is not null the handling of the rest
                    // 需要立即使 future 失败。如果它不为 null，则处理其余部分
                    // will be done in flush0()
                    // 将在 flush0() 中完成
                    // See https://github.com/netty/netty/issues/2362
                    // 参见 https://github.com/netty/netty/issues/2362
                    safeSetFailure(promise,
                            newClosedChannelException(initialCloseCause, "write(Object, ChannelPromise)"));
                }
                return;
            }

            int size;
            try {
                msg = filterOutboundMessage(msg);
                size = pipeline.estimatorHandle().size(msg);
                if (size < 0) {
                    size = 0;
                }
            } catch (Throwable t) {
                try {
                    ReferenceCountUtil.release(msg);
                } finally {
                    safeSetFailure(promise, t);
                }
                return;
            }

            outboundBuffer.addMessage(msg, size, promise);
        }

        @Override
        public final void flush() {
            assertEventLoop();

            ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
            if (outboundBuffer == null) {
                return;
            }

            outboundBuffer.addFlush();
            flush0();
        }

        @SuppressWarnings("deprecation")
        protected void flush0() {
            if (inFlush0) {
                // Avoid re-entrance
                // 避免重入
                return;
            }

            final ChannelOutboundBuffer outboundBuffer = this.outboundBuffer;
            if (outboundBuffer == null || outboundBuffer.isEmpty()) {
                return;
            }

            inFlush0 = true;

            // Mark all pending write requests as failure if the channel is inactive.

            // 如果通道处于非活动状态，将所有待处理的写请求标记为失败。
            if (!isActive()) {
                try {
                    // Check if we need to generate the exception at all.
                    // 检查我们是否需要生成异常。
                    if (!outboundBuffer.isEmpty()) {
                        if (isOpen()) {
                            outboundBuffer.failFlushed(new NotYetConnectedException(), true);
                        } else {
                            // Do not trigger channelWritabilityChanged because the channel is closed already.
                            // 不要触发 channelWritabilityChanged，因为通道已经关闭。
                            outboundBuffer.failFlushed(newClosedChannelException(initialCloseCause, "flush0()"), false);
                        }
                    }
                } finally {
                    inFlush0 = false;
                }
                return;
            }

            try {
                doWrite(outboundBuffer);
            } catch (Throwable t) {
                handleWriteError(t);
            } finally {
                inFlush0 = false;
            }
        }

        protected final void handleWriteError(Throwable t) {
            if (t instanceof IOException && config().isAutoClose()) {
                /**
                 * Just call {@link #close(ChannelPromise, Throwable, boolean)} here which will take care of
                 * failing all flushed messages and also ensure the actual close of the underlying transport
                 * will happen before the promises are notified.
                 *
                 * This is needed as otherwise {@link #isActive()} , {@link #isOpen()} and {@link #isWritable()}
                 * may still return {@code true} even if the channel should be closed as result of the exception.
                 */
                /**
                 * 只需调用 {@link #close(ChannelPromise, Throwable, boolean)}，该方法将负责
                 * 使所有已刷新的消息失败，并确保在实际关闭底层传输之前通知承诺。
                 *
                 * 这是必要的，因为否则 {@link #isActive()}、{@link #isOpen()} 和 {@link #isWritable()}
                 * 可能仍然返回 {@code true}，即使由于异常应关闭通道。
                 */
                initialCloseCause = t;
                close(voidPromise(), t, newClosedChannelException(t, "flush0()"), false);
            } else {
                try {
                    shutdownOutput(voidPromise(), t);
                } catch (Throwable t2) {
                    initialCloseCause = t;
                    close(voidPromise(), t2, newClosedChannelException(t, "flush0()"), false);
                }
            }
        }

        private ClosedChannelException newClosedChannelException(Throwable cause, String method) {
            ClosedChannelException exception =new ClosedChannelException();
            return exception;
        }

        @Override
        public final ChannelPromise voidPromise() {
            assertEventLoop();

            return unsafeVoidPromise;
        }

        protected final boolean ensureOpen(ChannelPromise promise) {
            if (isOpen()) {
                return true;
            }

            safeSetFailure(promise, newClosedChannelException(initialCloseCause, "ensureOpen(ChannelPromise)"));
            return false;
        }

        /**
         * Marks the specified {@code promise} as success.  If the {@code promise} is done already, log a message.
         */

        /**
         * 将指定的 {@code promise} 标记为成功。如果 {@code promise} 已完成，则记录一条消息。
         */
        protected final void safeSetSuccess(ChannelPromise promise) {
            if (!(promise instanceof VoidChannelPromise) && !promise.trySuccess()) {
                logger.warn("Failed to mark a promise as success because it is done already: {}", promise);
            }
        }

        /**
         * Marks the specified {@code promise} as failure.  If the {@code promise} is done already, log a message.
         */

        /**
         * 将指定的 {@code promise} 标记为失败。如果 {@code promise} 已经完成，则记录一条消息。
         */
        protected final void safeSetFailure(ChannelPromise promise, Throwable cause) {
            if (!(promise instanceof VoidChannelPromise) && !promise.tryFailure(cause)) {
                logger.warn("Failed to mark a promise as failure because it's done already: {}", promise, cause);
            }
        }

        protected final void closeIfClosed() {
            if (isOpen()) {
                return;
            }
            close(voidPromise());
        }

        private void invokeLater(Runnable task) {
            try {
                // This method is used by outbound operation implementations to trigger an inbound event later.
                // 该方法被出站操作实现用于稍后触发入站事件。
                // They do not trigger an inbound event immediately because an outbound operation might have been
                // 它们不会立即触发入站事件，因为可能正在进行出站操作
                // triggered by another inbound event handler method.  If fired immediately, the call stack
                // 由另一个入站事件处理方法触发。如果立即触发，调用栈
                // will look like this for example:
                // 例如，看起来会像这样：
                //
                //   handlerA.inboundBufferUpdated() - (1) an inbound handler method closes a connection.
                //   handlerA.inboundBufferUpdated() - (1) 一个入站处理器方法关闭了连接。
                //   -> handlerA.ctx.close()
                //   -> handlerA.ctx.close()
                //      -> channel.unsafe.close()
                //      -> channel.unsafe.close()
                //         -> handlerA.channelInactive() - (2) another inbound handler method called while in (1) yet
                //         -> handlerA.channelInactive() - (2) 另一个入站处理程序方法在 (1) 中调用
                //
                // which means the execution of two inbound handler methods of the same handler overlap undesirably.
                // 这意味着同一个处理程序的两个入站处理方法重叠执行，这是不希望的。
                eventLoop().execute(task);
            } catch (RejectedExecutionException e) {
                logger.warn("Can't invoke task later as EventLoop rejected it", e);
            }
        }

        /**
         * Appends the remote address to the message of the exceptions caused by connection attempt failure.
         */

        /**
         * 将远程地址附加到因连接尝试失败引起的异常消息中。
         */
        protected final Throwable annotateConnectException(Throwable cause, SocketAddress remoteAddress) {
            if (cause instanceof ConnectException) {
                return new AnnotatedConnectException((ConnectException) cause, remoteAddress);
            }
            if (cause instanceof NoRouteToHostException) {
                return new AnnotatedNoRouteToHostException((NoRouteToHostException) cause, remoteAddress);
            }
            if (cause instanceof SocketException) {
                return new AnnotatedSocketException((SocketException) cause, remoteAddress);
            }

            return cause;
        }

        /**
         * Prepares to close the {@link Channel}. If this method returns an {@link Executor}, the
         * caller must call the {@link Executor#execute(Runnable)} method with a task that calls
         * {@link #doClose()} on the returned {@link Executor}. If this method returns {@code null},
         * {@link #doClose()} must be called from the caller thread. (i.e. {@link EventLoop})
         */

        /**
         * 准备关闭 {@link Channel}。如果此方法返回一个 {@link Executor}，调用者必须在返回的 {@link Executor} 上调用 {@link Executor#execute(Runnable)} 方法，
         * 并传入一个任务来调用 {@link #doClose()}。如果此方法返回 {@code null}，则必须从调用者线程（即 {@link EventLoop}）中调用 {@link #doClose()}。
         */
        protected Executor prepareToClose() {
            return null;
        }
    }

    /**
     * Return {@code true} if the given {@link EventLoop} is compatible with this instance.
     */

    /**
     * 如果给定的 {@link EventLoop} 与此实例兼容，则返回 {@code true}。
     */
    protected abstract boolean isCompatible(EventLoop loop);

    /**
     * Returns the {@link SocketAddress} which is bound locally.
     */

    /**
     * 返回本地绑定的 {@link SocketAddress}。
     */
    protected abstract SocketAddress localAddress0();

    /**
     * Return the {@link SocketAddress} which the {@link Channel} is connected to.
     */

    /**
     * 返回 {@link Channel} 连接到的 {@link SocketAddress}。
     */
    protected abstract SocketAddress remoteAddress0();

    /**
     * Is called after the {@link Channel} is registered with its {@link EventLoop} as part of the register process.
     *
     * Sub-classes may override this method
     */

    /**
     * 在 {@link Channel} 被注册到其 {@link EventLoop} 后调用，作为注册过程的一部分。
     *
     * 子类可以重写此方法
     */
    protected void doRegister() throws Exception {
        // NOOP
        // NOOP
    }

    /**
     * Bind the {@link Channel} to the {@link SocketAddress}
     */

    /**
     * 将 {@link Channel} 绑定到 {@link SocketAddress}
     */
    protected abstract void doBind(SocketAddress localAddress) throws Exception;

    /**
     * Disconnect this {@link Channel} from its remote peer
     */

    /**
     * 断开此 {@link Channel} 与其远程对等方的连接
     */
    protected abstract void doDisconnect() throws Exception;

    /**
     * Close the {@link Channel}
     */

    /**
     * 关闭 {@link Channel}
     */
    protected abstract void doClose() throws Exception;

    /**
     * Called when conditions justify shutting down the output portion of the channel. This may happen if a write
     * operation throws an exception.
     */

    /**
     * 当条件证明需要关闭通道的输出部分时调用。如果写操作抛出异常，可能会发生这种情况。
     */
    @UnstableApi
    protected void doShutdownOutput() throws Exception {
        doClose();
    }

    /**
     * Deregister the {@link Channel} from its {@link EventLoop}.
     *
     * Sub-classes may override this method
     */

    /**
     * 从 {@link EventLoop} 中注销 {@link Channel}。
     *
     * 子类可以重写此方法
     */
    protected void doDeregister() throws Exception {
        // NOOP
        // NOOP
    }

    /**
     * Schedule a read operation.
     */

    /**
     * 安排一个读取操作。
     */
    protected abstract void doBeginRead() throws Exception;

    /**
     * Flush the content of the given buffer to the remote peer.
     */

    /**
     * 将给定缓冲区的内容刷新到远程对等端。
     */
    protected abstract void doWrite(ChannelOutboundBuffer in) throws Exception;

    /**
     * Invoked when a new message is added to a {@link ChannelOutboundBuffer} of this {@link AbstractChannel}, so that
     * the {@link Channel} implementation converts the message to another. (e.g. heap buffer -> direct buffer)
     */

    /**
     * 当新消息被添加到该 {@link AbstractChannel} 的 {@link ChannelOutboundBuffer} 时调用，以便
     * {@link Channel} 实现将消息转换为另一种形式。（例如：堆缓冲区 -> 直接缓冲区）
     */
    protected Object filterOutboundMessage(Object msg) throws Exception {
        return msg;
    }

    protected void validateFileRegion(DefaultFileRegion region, long position) throws IOException {
    }

    static final class CloseFuture extends DefaultChannelPromise {

        CloseFuture(AbstractChannel ch) {
            super(ch);
        }

        @Override
        public ChannelPromise setSuccess() {
            throw new IllegalStateException();
        }

        @Override
        public ChannelPromise setFailure(Throwable cause) {
            throw new IllegalStateException();
        }

        @Override
        public boolean trySuccess() {
            throw new IllegalStateException();
        }

        @Override
        public boolean tryFailure(Throwable cause) {
            throw new IllegalStateException();
        }

        boolean setClosed() {
            return super.trySuccess();
        }
    }

    private static final class AnnotatedConnectException extends ConnectException {

        private static final long serialVersionUID = 3901958112696433556L;

        AnnotatedConnectException(ConnectException exception, SocketAddress remoteAddress) {
            super(exception.getMessage() + ": " + remoteAddress);
            initCause(exception);
        }

        // Suppress a warning since this method doesn't need synchronization

        // 由于此方法不需要同步，因此抑制警告
        @Override
        public Throwable fillInStackTrace() {   // lgtm[java/non-sync-override]
            return this;
        }
    }

    private static final class AnnotatedNoRouteToHostException extends NoRouteToHostException {

        private static final long serialVersionUID = -6801433937592080623L;

        AnnotatedNoRouteToHostException(NoRouteToHostException exception, SocketAddress remoteAddress) {
            super(exception.getMessage() + ": " + remoteAddress);
            initCause(exception);
        }

        // Suppress a warning since this method doesn't need synchronization

        // 由于此方法不需要同步，因此抑制警告
        @Override
        public Throwable fillInStackTrace() {   // lgtm[java/non-sync-override]
            return this;
        }
    }

    private static final class AnnotatedSocketException extends SocketException {

        private static final long serialVersionUID = 3896743275010454039L;

        AnnotatedSocketException(SocketException exception, SocketAddress remoteAddress) {
            super(exception.getMessage() + ": " + remoteAddress);
            initCause(exception);
        }

        // Suppress a warning since this method doesn't need synchronization

        // 由于此方法不需要同步，因此抑制警告
        @Override
        public Throwable fillInStackTrace() {   // lgtm[java/non-sync-override]
            return this;
        }
    }
}
