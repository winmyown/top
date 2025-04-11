
package org.top.java.netty.example.stomp.websocket;

import io.netty.channel.Channel;
import io.netty.util.internal.ObjectUtil;

public final class StompSubscription {

    private final String id;
    private final String destination;
    private final Channel channel;

    public StompSubscription(String id, String destination, Channel channel) {
        this.id = ObjectUtil.checkNotNull(id, "id");
        this.destination = ObjectUtil.checkNotNull(destination, "destination");
        this.channel = ObjectUtil.checkNotNull(channel, "channel");
    }

    public String id() {
        return id;
    }

    public String destination() {
        return destination;
    }

    public Channel channel() {
        return channel;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        StompSubscription that = (StompSubscription) obj;

        if (!id.equals(that.id)) {
            return false;
        }

        if (!destination.equals(that.destination)) {
            return false;
        }

        return channel.equals(that.channel);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + destination.hashCode();
        result = 31 * result + channel.hashCode();
        return result;
    }
}
