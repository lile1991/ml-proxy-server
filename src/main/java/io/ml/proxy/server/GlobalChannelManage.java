package io.ml.proxy.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GlobalChannelManage {
    private final Map<ChannelId, Channel> channelMap = new ConcurrentHashMap<>();

    public void add(Channel channel) {
        channelMap.put(channel.id(), channel);
    }

    public Channel get(ChannelId channelId) {
        return channelMap.get(channelId);
    }
}
