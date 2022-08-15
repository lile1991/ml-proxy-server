package io.ml.proxy.server.handler.http.relay.socks5;

import io.ml.proxy.server.config.ProxyServerConfig;
import io.ml.proxy.server.handler.ExchangeHandler;
import io.ml.proxy.server.handler.http.HttpAcceptConnectHandler;
import io.ml.proxy.server.handler.http.HttpRequestInfo;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5ClientEncoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponseDecoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Socks5CommandResponseHandler extends ChannelInboundHandlerAdapter {
    private final ProxyServerConfig serverConfig;
    private final Channel proxyServerChannel;
    private final HttpRequestInfo httpRequestInfo;
    public Socks5CommandResponseHandler(ProxyServerConfig serverConfig, Channel proxyServerChannel, HttpRequestInfo httpRequestInfo) {
        this.serverConfig = serverConfig;
        this.proxyServerChannel = proxyServerChannel;
        this.httpRequestInfo = httpRequestInfo;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("channelRead Socks5CommandResponse {}.\r\n{}", msg, ctx);
        DefaultSocks5CommandResponse socks5Command = (DefaultSocks5CommandResponse) msg;
        if(socks5Command.decoderResult() == DecoderResult.SUCCESS) {
            ctx.pipeline().remove(Socks5ClientEncoder.class);
            ctx.pipeline().remove(Socks5CommandResponseDecoder.class);

            if(httpRequestInfo.isSsl()) {
                ctx.pipeline().remove(ctx.name());
                // If the first is an HTTPS connection request,
                // response with 200(Connection Established).
                ChannelPromise proxyServerChannelPromise = proxyServerChannel.newPromise().addListener(f -> {
                    // 中继都不解码消息， 移除代理服务器的解码器
                    log.debug("Remove HttpServerCodec from pipeline.\r\n{}", proxyServerChannel);
                    proxyServerChannel.pipeline().remove(HttpServerCodec.class);
                    proxyServerChannel.pipeline().remove(HttpObjectAggregator.class);

                    log.debug("Add ExchangeHandler to proxy server pipeline.\r\n{}", proxyServerChannel);
                    proxyServerChannel.pipeline().addLast(new ExchangeHandler(serverConfig, ctx.channel()));

                    log.debug("Add ExchangeHandler to relay server pipeline.\r\n{}", ctx);
                    ctx.pipeline().addLast(new ExchangeHandler(serverConfig, proxyServerChannel));
                });
                HttpAcceptConnectHandler.response200ProxyEstablished(proxyServerChannel, httpRequestInfo.getHttpRequest().protocolVersion(), proxyServerChannelPromise);
            } else {
                // If the first is an HTTP request, forward it to the website.
                log.debug("Add HttpClientCodec to relay client pipeline.");
                ctx.pipeline().addBefore(ctx.name(), null, new HttpRequestEncoder());
                ctx.pipeline().remove(ctx.name());

                HttpRequest request = httpRequestInfo.getHttpRequest();
                request.headers().remove(HttpHeaderNames.PROXY_AUTHORIZATION.toString());
                String connection = request.headers().get(HttpHeaderNames.PROXY_CONNECTION.toString());
                if(connection != null) {
                    request.headers().remove(HttpHeaderNames.PROXY_CONNECTION.toString());
                    request.headers().add(HttpHeaderNames.CONNECTION.toString(), connection);
                }
                log.debug("Write the first http request to remote.\r\n{}\r\n{}", httpRequestInfo.getHttpRequest(), ctx);
                ctx.channel().writeAndFlush(httpRequestInfo.getHttpRequest(), ctx.newPromise().addListener(f -> {
                    if(!f.isSuccess()) {
                        log.error("Write the first http request to remote error: ", f.cause());
                        ctx.close();
                        proxyServerChannel.close();
                        return;
                    }

                    log.debug("Remove HttpServerCodec from proxy server pipeline.\r\n{}", proxyServerChannel);
                    proxyServerChannel.pipeline().remove(HttpServerCodec.class);
                    proxyServerChannel.pipeline().remove(HttpObjectAggregator.class);

                    ctx.pipeline().remove(HttpRequestEncoder.class);

                    // log.debug("Add ExchangeHandler to proxy server pipeline.");
                    // proxyServerChannel.pipeline().addLast(new ExchangeHandler(serverConfig, ctx.channel()));

                    log.debug("Add ExchangeHandler to relay server pipeline.\r\n{}", ctx);
                    ctx.pipeline().addLast(new ExchangeHandler(serverConfig, proxyServerChannel));
                }));
            }
        } else {
            log.error("decoderResult is {}, close channel", socks5Command.decoderResult());
            ctx.close();
            proxyServerChannel.close();
        }
    }
}
