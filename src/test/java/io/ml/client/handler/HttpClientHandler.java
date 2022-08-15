package io.ml.client.handler;

import io.ml.proxy.utils.http.HttpObjectUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public class HttpClientHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        log.debug("read from proxy: \r\n{}", HttpObjectUtils.stringOf(msg));
        /*if(ctx.channel().pipeline().get("clientSshHandler") == null) {
            SslContext clientSslCtx = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            ctx.channel().pipeline().addAfter("httpClientCodec", "clientSshHandler", clientSslCtx.newHandler(ctx.channel().alloc()));
        }*/

        DefaultFullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
        request.headers().set(HttpHeaderNames.HOST, "ckadmin.grights.club:443");
        request.headers().set(HttpHeaderNames.PROXY_AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString("sprint:FtMM7EvG".getBytes(StandardCharsets.UTF_8)));
        // request.headers().set(HttpHeaderNames.PROXY_CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        request.headers().set("accept-type", StandardCharsets.UTF_8);
        request.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        // request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());
        log.debug("Request google");
        ctx.channel().writeAndFlush(request);
    }
}
