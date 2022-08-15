package io.ml.proxy.server.handler.http.relay.socks5;

import io.ml.proxy.server.config.ProxyServerConfig;
import io.ml.proxy.server.config.RelayServerConfig;
import io.ml.proxy.server.config.UsernamePasswordAuth;
import io.ml.proxy.server.handler.http.HttpRequestInfo;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.socksx.v5.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Socks5RelayInitialResponseHandler extends ChannelInboundHandlerAdapter {

    private final ProxyServerConfig serverConfig;
    private final Channel proxyServerChannel;
    private final HttpRequestInfo httpRequestInfo;

    public Socks5RelayInitialResponseHandler(ProxyServerConfig serverConfig, Channel proxyServerChannel, HttpRequestInfo httpRequestInfo) {
        this.serverConfig = serverConfig;
        this.proxyServerChannel = proxyServerChannel;
        this.httpRequestInfo = httpRequestInfo;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        RelayServerConfig relayServerConfig = serverConfig.getRelayServerConfig();
        Socks5InitialResponse socks5InitialResponse = (Socks5InitialResponse) msg;
        Socks5AuthMethod socks5AuthMethod = socks5InitialResponse.authMethod();
        if(socks5AuthMethod.compareTo(Socks5AuthMethod.NO_AUTH) == 0) {
            log.debug("Socks5 proxy server does not authentication required");
        } else {
            ctx.pipeline().remove(Socks5InitialResponseDecoder.class);
            ctx.pipeline().addAfter(ctx.name(), null, new Socks5PasswordAuthResponseHandler(serverConfig, proxyServerChannel, httpRequestInfo));
            ctx.pipeline().addAfter(ctx.name(), null, new Socks5PasswordAuthResponseDecoder());
            ctx.pipeline().remove(ctx.name());

            UsernamePasswordAuth relayUsernamePasswordAuth = relayServerConfig.getRelayUsernamePasswordAuth();
            DefaultSocks5PasswordAuthRequest socks5PasswordAuthRequest = new DefaultSocks5PasswordAuthRequest(relayUsernamePasswordAuth.getUsername(), relayUsernamePasswordAuth.getPassword());
            ctx.writeAndFlush(socks5PasswordAuthRequest);
        }
    }
}
