package io.ml.relay.handler.http;

import io.ml.proxy.server.handler.http.HttpAcceptConnectHandler;
import io.ml.proxy.server.handler.http.HttpRequestInfo;
import io.ml.relay.config.HttpProxyRelayServerConfig;
import io.ml.relay.config.ReplayRuleConfig;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class HttpRelayFilterHandler extends ChannelInboundHandlerAdapter {
    final HttpProxyRelayServerConfig serverConfig;
    public HttpRelayFilterHandler(HttpProxyRelayServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    /**
     * 从客户端(比如浏览器)读数据
     * @param ctx 与客户端的连接
     * @param msg 消息 HttpConnect、HttpRequest、HttpContent、SSL请求
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg instanceof HttpRequest) {
            selector(ctx, (HttpRequest) msg);
        } else {
            log.error("收到莫名消息: {}", msg);
        }

    }

    private void selector(ChannelHandlerContext ctx, HttpRequest request) throws Exception {
        HttpRequestInfo requestInfo = new HttpRequestInfo(request);
        String remoteHost = requestInfo.getRemoteHost();
        ctx.pipeline().remove(HttpRelayFilterHandler.class);

        if(isDirect(requestInfo)) {
            log.debug("Host {} go direct!", remoteHost);
            HttpAcceptConnectHandler httpAcceptConnectHandler = new HttpAcceptConnectHandler(serverConfig);
            ctx.pipeline().addLast(httpAcceptConnectHandler);
            httpAcceptConnectHandler.channelRead(ctx, request);
        } else {
            HttpRelayConnectionHandler httpRelayConnectionHandler = new HttpRelayConnectionHandler(serverConfig);
            ctx.pipeline().addLast(httpRelayConnectionHandler);
            httpRelayConnectionHandler.channelRead(ctx, request);
        }
    }

    private boolean isDirect(HttpRequestInfo requestInfo) {
        ReplayRuleConfig replayRuleConfig = serverConfig.getReplayRuleConfig();
        if(replayRuleConfig == null) {
            return false;
        }

        switch (replayRuleConfig.getProxyMode()) {
            case ONLY:
                List<String> proxyHosts = replayRuleConfig.getProxyHosts();
                if(proxyHosts != null) {
                    for (String proxyHost : proxyHosts) {
                        if (requestInfo.getRemoteHost().contains(proxyHost)) {
                            // Proxy
                            return false;
                        }
                    }
                }
                // Direct
                return true;
            default:
                List<String> directHosts = replayRuleConfig.getDirectHosts();
                if(directHosts != null) {
                    for (String directHost : directHosts) {
                        if (requestInfo.getRemoteHost().contains(directHost)) {
                            // Direct
                            return true;
                        }
                    }
                }
                // Proxy
                return false;
        }
    }
}
