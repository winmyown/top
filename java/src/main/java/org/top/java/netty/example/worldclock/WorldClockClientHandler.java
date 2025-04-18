
package org.top.java.netty.example.worldclock;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.top.java.netty.example.worldclock.WorldClockProtocol.Continent;
import org.top.java.netty.example.worldclock.WorldClockProtocol.LocalTime;
import org.top.java.netty.example.worldclock.WorldClockProtocol.LocalTimes;
import org.top.java.netty.example.worldclock.WorldClockProtocol.Location;
import org.top.java.netty.example.worldclock.WorldClockProtocol.Locations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Pattern;

public class WorldClockClientHandler extends SimpleChannelInboundHandler<LocalTimes> {

    private static final Pattern DELIM = Pattern.compile("/");

    // Stateful properties

    // 有状态属性
    private volatile Channel channel;
    private final BlockingQueue<LocalTimes> answer = new LinkedBlockingQueue<LocalTimes>();

    public WorldClockClientHandler() {
        super(false);
    }

    public List<String> getLocalTimes(Collection<String> cities) {
        Locations.Builder builder = Locations.newBuilder();

        for (String c: cities) {
            String[] components = DELIM.split(c);
            builder.addLocation(Location.newBuilder().
                setContinent(Continent.valueOf(components[0].toUpperCase())).
                setCity(components[1]).build());
        }

        channel.writeAndFlush(builder.build());

        LocalTimes localTimes;
        boolean interrupted = false;
        for (;;) {
            try {
                localTimes = answer.take();
                break;
            } catch (InterruptedException ignore) {
                interrupted = true;
            }
        }

        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        List<String> result = new ArrayList<String>();
        for (LocalTime lt: localTimes.getLocalTimeList()) {
            result.add(
                    new Formatter().format(
                            "%4d-%02d-%02d %02d:%02d:%02d %s",
                            lt.getYear(),
                            lt.getMonth(),
                            lt.getDayOfMonth(),
                            lt.getHour(),
                            lt.getMinute(),
                            lt.getSecond(),
                            lt.getDayOfWeek().name()).toString());
        }

        return result;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        channel = ctx.channel();
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, LocalTimes times) throws Exception {
        answer.add(times);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
