package io.ml.proxy.server.handler;

import io.ml.proxy.server.config.ProxyServerConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * 交换机
 */
@Slf4j
public class ExchangeHandler extends ChannelInboundHandlerAdapter {
    private final ProxyServerConfig serverConfig;
    private final Channel exchangeChannel;

    public ExchangeHandler(ProxyServerConfig serverConfig, Channel exchangeChannel) {
        this.serverConfig = serverConfig;
        this.exchangeChannel = exchangeChannel;
    }

    /**
     * 将读取到的数据转发
     * @param ctx channel
     * @param msg 数据包
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        log.debug("Forward the message. \r\n{}\r\n{}", msg, exchangeChannel);
        exchangeChannel.writeAndFlush(msg);
    }
}
