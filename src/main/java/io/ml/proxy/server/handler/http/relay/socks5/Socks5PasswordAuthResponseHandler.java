package io.ml.proxy.server.handler.http.relay.socks5;

import io.ml.proxy.server.config.ProxyServerConfig;
import io.ml.proxy.server.handler.http.HttpAcceptConnectHandler;
import io.ml.proxy.server.handler.http.HttpRequestInfo;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.socksx.v5.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Socks5PasswordAuthResponseHandler extends ChannelInboundHandlerAdapter {
    private final ProxyServerConfig serverConfig;
    private final Channel proxyServerChannel;
    private final HttpRequestInfo httpRequestInfo;
    public Socks5PasswordAuthResponseHandler(ProxyServerConfig serverConfig, Channel proxyServerChannel, HttpRequestInfo httpRequestInfo) {
        this.serverConfig = serverConfig;
        this.proxyServerChannel = proxyServerChannel;
        this.httpRequestInfo = httpRequestInfo;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Socks5PasswordAuthResponse response = (Socks5PasswordAuthResponse) msg;
        if(response.status() == Socks5PasswordAuthStatus.SUCCESS) {
            log.debug("Socks5 auth success");

            log.debug("Add Socks5CommandResponseHandler to relay server pipeline.");
            // 增加CommandHandler
            ctx.pipeline().addAfter(ctx.name(), null, new Socks5CommandResponseHandler(serverConfig, proxyServerChannel, httpRequestInfo));
            ctx.pipeline().addAfter(ctx.name(), null, new Socks5CommandResponseDecoder());

            // The socks5 auth is completed!
            ctx.pipeline().remove(Socks5PasswordAuthResponseDecoder.class);
            ctx.pipeline().remove(ctx.name());

            // Connection to website
            DefaultSocks5CommandRequest socks5CommandRequest = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT,
                    Socks5AddressType.DOMAIN, httpRequestInfo.getRemoteHost(), httpRequestInfo.getRemotePort());
            ctx.writeAndFlush(socks5CommandRequest);
        } else {
            log.debug("Socks5 auth failure");
            HttpAcceptConnectHandler.response407ProxyAuthenticationRequired(proxyServerChannel, httpRequestInfo.getHttpRequest().protocolVersion(), "Incorrect socks5 proxy username or password")
                    .addListener(ChannelFutureListener.CLOSE);
            ctx.close();
        }
    }
}
