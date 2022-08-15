package io.ml.relay.handler.http;

import io.ml.proxy.server.config.ProxyProtocolEnum;
import io.ml.proxy.server.handler.http.HttpRequestInfo;
import io.ml.relay.config.HttpProxyRelayServerConfig;
import io.ml.relay.handler.codec.lee.LeeClientCodec;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;

/**
 * 与代理服务器InitHandler
 */
public class HttpRealProxyInitHandler extends ChannelInitializer<Channel> {
    private final Channel relayChannel;
    private final HttpProxyRelayServerConfig serverConfig;
    private final HttpRequestInfo httpRequestInfo;


    public HttpRealProxyInitHandler(Channel relayChannel, HttpProxyRelayServerConfig serverConfig, HttpRequestInfo httpRequestInfo) {
        this.relayChannel = relayChannel;
        this.serverConfig = serverConfig;
        this.httpRequestInfo = httpRequestInfo;
    }

    @Override
    protected void initChannel(Channel ch) {
        // ch.pipeline().addLast("loggingHandler", new LoggingHandler(LogLevel.DEBUG));
        if (serverConfig.getRelayProtocols().contains(ProxyProtocolEnum.LEE)) {
            ch.pipeline().addLast(new LeeClientCodec());
        }
        ch.pipeline().addLast(new HttpClientCodec());
        ch.pipeline().addLast(new HttpObjectAggregator(serverConfig.getHttpObjectAggregatorMaxContentLength()));
        ch.pipeline().addLast(new HttpRealProxyHandler(relayChannel, serverConfig, httpRequestInfo));
    }
}
