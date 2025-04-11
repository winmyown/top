
package org.top.java.netty.source.channel.socket.nio;

import io.netty.channel.socket.InternetProtocolFamily;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SuppressJava6Requirement;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.Channel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;

final class SelectorProviderUtil {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(SelectorProviderUtil.class);

    @SuppressJava6Requirement(reason = "Usage guarded by java version check")
    static Method findOpenMethod(String methodName) {
        if (PlatformDependent.javaVersion() >= 15) {
            try {
                return SelectorProvider.class.getMethod(methodName, java.net.ProtocolFamily.class);
            } catch (Throwable e) {
                logger.debug("SelectorProvider.{}(ProtocolFamily) not available, will use default", methodName, e);
            }
        }
        return null;
    }

    @SuppressJava6Requirement(reason = "Usage guarded by java version check")
    static <C extends Channel> C newChannel(Method method, SelectorProvider provider,
                                                    InternetProtocolFamily family) throws IOException {
        /**
         *  Use the {@link SelectorProvider} to open {@link SocketChannel} and so remove condition in
         *  {@link SelectorProvider#provider()} which is called by each SocketChannel.open() otherwise.
         *
         *  See <a href="https://github.com/netty/netty/issues/2308">#2308</a>.
         */
        /**
         * 使用 {@link SelectorProvider} 打开 {@link SocketChannel}，从而消除在 {@link SelectorProvider#provider()} 中的条件，否则每次调用 SocketChannel.open() 时都会调用该条件。
         *
         * 参见 <a href="https://github.com/netty/netty/issues/2308">#2308</a>。
         */
        if (family != null && method != null) {
            try {
                @SuppressWarnings("unchecked")
                C channel = (C) method.invoke(
                        provider, ProtocolFamilyConverter.convert(family));
                return channel;
            } catch (InvocationTargetException e) {
                throw new IOException(e);
            } catch (IllegalAccessException e) {
                throw new IOException(e);
            }
        }
        return null;
    }

    private SelectorProviderUtil() { }
}
