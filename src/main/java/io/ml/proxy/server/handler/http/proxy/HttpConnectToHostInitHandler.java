package io.ml.proxy.server.handler.http.proxy;

import io.ml.proxy.server.config.ProxyServerConfig;
import io.ml.proxy.server.handler.ExchangeHandler;
import io.ml.proxy.server.handler.http.HttpRequestInfo;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

/**
 * 与目标网站的Handler
 */
public class HttpConnectToHostInitHandler extends ChannelInitializer<Channel> {
    private final Channel proxyServerChannel;
    private final ProxyServerConfig serverConfig;
    private final HttpRequestInfo httpRequestInfo;


    public HttpConnectToHostInitHandler(Channel proxyServerChannel, ProxyServerConfig serverConfig, HttpRequestInfo httpRequestInfo) {
        this.proxyServerChannel = proxyServerChannel;
        this.serverConfig = serverConfig;
        this.httpRequestInfo = httpRequestInfo;
    }

    @Override
    protected void initChannel(Channel ch) {
        ch.pipeline().addLast(ExchangeHandler.class.getSimpleName(), new ExchangeHandler(serverConfig, proxyServerChannel));
    }
}
