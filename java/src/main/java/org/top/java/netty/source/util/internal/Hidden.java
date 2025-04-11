

package org.top.java.netty.source.util.internal;

import org.top.java.netty.source.util.concurrent.FastThreadLocalThread;
import reactor.blockhound.BlockHound;
import reactor.blockhound.integration.BlockHoundIntegration;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Contains classes that must have public visibility but are not public API.
 */

/**
 * 包含必须具有公共可见性但不是公共API的类。
 */
class Hidden {

    /**
     * This class integrates Netty with BlockHound.
     * <p>
     * It is public but only because of the ServiceLoader's limitations
     * and SHOULD NOT be considered a public API.
     */

    /**
     * 该类将Netty与BlockHound集成。
     * <p>
     * 它是公开的，但这仅由于ServiceLoader的限制，
     * 不应被视为公共API。
     */
    @UnstableApi
    @SuppressJava6Requirement(reason = "BlockHound is Java 8+, but this class is only loaded by it's SPI")
    public static final class NettyBlockHoundIntegration implements BlockHoundIntegration {

        @Override
        public void applyTo(BlockHound.Builder builder) {
            builder.allowBlockingCallsInside(
                    "io.netty.channel.nio.NioEventLoop",
                    "handleLoopException"
            );

            builder.allowBlockingCallsInside(
                    "io.netty.channel.kqueue.KQueueEventLoop",
                    "handleLoopException"
            );

            builder.allowBlockingCallsInside(
                    "io.netty.channel.epoll.EpollEventLoop",
                    "handleLoopException"
            );

            builder.allowBlockingCallsInside(
                    "io.netty.util.HashedWheelTimer",
                    "start"
            );

            builder.allowBlockingCallsInside(
                    "io.netty.util.HashedWheelTimer",
                    "stop"
            );

            builder.allowBlockingCallsInside(
                    "io.netty.util.HashedWheelTimer$Worker",
                    "waitForNextTick"
            );

            builder.allowBlockingCallsInside(
                    "io.netty.util.concurrent.SingleThreadEventExecutor",
                    "confirmShutdown"
            );

            builder.allowBlockingCallsInside(
                    "io.netty.buffer.PoolArena",
                    "lock"
            );

            builder.allowBlockingCallsInside(
                    "io.netty.buffer.PoolSubpage",
                    "lock"
            );

            builder.allowBlockingCallsInside(
                    "io.netty.buffer.PoolChunk",
                    "allocateRun"
            );

            builder.allowBlockingCallsInside(
                    "io.netty.buffer.PoolChunk",
                    "free"
            );

            builder.allowBlockingCallsInside(
                    "io.netty.handler.ssl.SslHandler",
                    "handshake"
            );

            builder.allowBlockingCallsInside(
                    "io.netty.handler.ssl.SslHandler",
                    "runAllDelegatedTasks"
            );
            builder.allowBlockingCallsInside(
                    "io.netty.handler.ssl.SslHandler",
                    "runDelegatedTasks"
            );
            builder.allowBlockingCallsInside(
                    "io.netty.util.concurrent.GlobalEventExecutor",
                    "takeTask");

            builder.allowBlockingCallsInside(
                    "io.netty.util.concurrent.GlobalEventExecutor",
                    "addTask");

            builder.allowBlockingCallsInside(
                    "io.netty.util.concurrent.SingleThreadEventExecutor",
                    "takeTask");

            builder.allowBlockingCallsInside(
                    "io.netty.util.concurrent.SingleThreadEventExecutor",
                    "addTask");

            builder.allowBlockingCallsInside(
                    "io.netty.handler.ssl.ReferenceCountedOpenSslClientContext$ExtendedTrustManagerVerifyCallback",
                    "verify");

            builder.allowBlockingCallsInside(
                    "io.netty.handler.ssl.JdkSslContext$Defaults",
                    "init");

            // Let's whitelist SSLEngineImpl.unwrap(...) for now as it may fail otherwise for TLS 1.3.

            // 我们暂时将 SSLEngineImpl.unwrap(...) 加入白名单，否则在 TLS 1.3 下可能会失败。
            // See https://mail.openjdk.java.net/pipermail/security-dev/2020-August/022271.html
            // 参见 https://mail.openjdk.java.net/pipermail/security-dev/2020-August/022271.html
            builder.allowBlockingCallsInside(
                    "sun.security.ssl.SSLEngineImpl",
                    "unwrap");

            builder.allowBlockingCallsInside(
                    "sun.security.ssl.SSLEngineImpl",
                    "wrap");

            builder.allowBlockingCallsInside(
                    "io.netty.resolver.dns.UnixResolverDnsServerAddressStreamProvider",
                    "parse");

            builder.allowBlockingCallsInside(
                    "io.netty.resolver.dns.UnixResolverDnsServerAddressStreamProvider",
                    "parseEtcResolverSearchDomains");

            builder.allowBlockingCallsInside(
                    "io.netty.resolver.dns.UnixResolverDnsServerAddressStreamProvider",
                    "parseEtcResolverOptions");

            builder.allowBlockingCallsInside(
                    "io.netty.resolver.HostsFileEntriesProvider$ParserImpl",
                    "parse");

            builder.allowBlockingCallsInside(
                    "io.netty.util.NetUtil$SoMaxConnAction",
                    "run");

            builder.nonBlockingThreadPredicate(new Function<Predicate<Thread>, Predicate<Thread>>() {
                @Override
                public Predicate<Thread> apply(final Predicate<Thread> p) {
                    return new Predicate<Thread>() {
                        @Override
                        @SuppressJava6Requirement(reason = "Predicate#test")
                        public boolean test(Thread thread) {
                            return p.test(thread) || thread instanceof FastThreadLocalThread;
                        }
                    };
                }
            });
        }

        @Override
        public int compareTo(BlockHoundIntegration o) {
            return 0;
        }
    }
}
