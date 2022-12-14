package io.ml.proxy.server.handler.http.relay.socks5;

import io.ml.proxy.server.config.ProxyServerConfig;
import io.ml.proxy.server.config.RelayConfig;
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
    private final RelayConfig relayConfig;
    private final Channel proxyServerChannel;
    private final HttpRequestInfo httpRequestInfo;

    public Socks5RelayInitialResponseHandler(ProxyServerConfig serverConfig, RelayConfig relayConfig, Channel proxyServerChannel, HttpRequestInfo httpRequestInfo) {
        this.serverConfig = serverConfig;
        this.relayConfig = relayConfig;
        this.proxyServerChannel = proxyServerChannel;
        this.httpRequestInfo = httpRequestInfo;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Socks5InitialResponse socks5InitialResponse = (Socks5InitialResponse) msg;
        Socks5AuthMethod socks5AuthMethod = socks5InitialResponse.authMethod();
        if(socks5AuthMethod.compareTo(Socks5AuthMethod.NO_AUTH) == 0) {
            log.debug("Socks5 proxy server does not authentication required");
            // Socks5CommandResponseHandler
            ctx.pipeline().addAfter(ctx.name(), null, new Socks5CommandResponseHandler(serverConfig, proxyServerChannel, httpRequestInfo));
            ctx.pipeline().addAfter(ctx.name(), null, new Socks5CommandResponseDecoder());

            // Connection to website
            DefaultSocks5CommandRequest socks5CommandRequest = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT,
                    Socks5AddressType.DOMAIN, httpRequestInfo.getRemoteHost(), httpRequestInfo.getRemotePort());
            ctx.writeAndFlush(socks5CommandRequest);
        } else {
            ctx.pipeline().remove(Socks5InitialResponseDecoder.class);
            ctx.pipeline().addAfter(ctx.name(), null, new Socks5PasswordAuthResponseHandler(serverConfig, proxyServerChannel, httpRequestInfo));
            ctx.pipeline().addAfter(ctx.name(), null, new Socks5PasswordAuthResponseDecoder());
            ctx.pipeline().remove(ctx.name());

            UsernamePasswordAuth relayUsernamePasswordAuth = relayConfig.getRelayUsernamePasswordAuth();
            DefaultSocks5PasswordAuthRequest socks5PasswordAuthRequest = new DefaultSocks5PasswordAuthRequest(relayUsernamePasswordAuth.getUsername(), relayUsernamePasswordAuth.getPassword());
            ctx.writeAndFlush(socks5PasswordAuthRequest);
        }
    }
}
