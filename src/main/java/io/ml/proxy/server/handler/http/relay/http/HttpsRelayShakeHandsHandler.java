package io.ml.proxy.server.handler.http.relay.http;

import io.ml.proxy.server.config.ProxyServerConfig;
import io.ml.proxy.server.handler.ExchangeHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

/**
 * 与真实Proxy Server握手处理器
 */
@Slf4j
public class HttpsRelayShakeHandsHandler extends ChannelInboundHandlerAdapter {
    private final ProxyServerConfig serverConfig;
    private final Channel proxyServerChannel;

    public HttpsRelayShakeHandsHandler(ProxyServerConfig serverConfig, Channel proxyServerChannel) {
        this.serverConfig = serverConfig;
        this.proxyServerChannel = proxyServerChannel;
    }

    /**
     * 处理真实代理服务器的响应
     * @param ctx 中继连接  relay server -> real proxy server
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        HttpResponse response = (HttpResponse) msg;
        log.debug("Connection status: {}", response.status());

        if(HttpResponseStatus.OK.code() == response.status().code()) {
            proxyServerChannel.writeAndFlush(response, proxyServerChannel.newPromise().addListener(f -> {
                // 中继都不解码消息， 移除代理服务器的解码器
                log.debug("Remove HttpServerCodec from pipeline\r\n{}", proxyServerChannel);
                proxyServerChannel.pipeline().remove(HttpServerCodec.class);
                proxyServerChannel.pipeline().remove(HttpObjectAggregator.class);
                log.debug("Add ProxyExchangeHandler to proxy server pipeline.");
                proxyServerChannel.pipeline().addLast(new ExchangeHandler(serverConfig, ctx.channel()));

                log.debug("Remove HttpClientCodec from pipeline\r\n{}", ctx);
                ctx.pipeline().remove(ctx.name());
                ctx.pipeline().remove(HttpClientCodec.class);
                ctx.pipeline().remove(HttpObjectAggregator.class);

                log.debug("Add ProxyExchangeHandler to relay server pipeline.");
                ctx.pipeline().addLast(new ExchangeHandler(serverConfig, proxyServerChannel));
            }));
        } else {
            proxyServerChannel.writeAndFlush(response).addListener(f -> {
                proxyServerChannel.close();
                ctx.close();
            });
        }
    }
}
