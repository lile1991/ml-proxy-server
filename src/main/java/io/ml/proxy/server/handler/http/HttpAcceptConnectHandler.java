package io.ml.proxy.server.handler.http;

import io.ml.proxy.server.config.ProxyServerConfig;
import io.ml.proxy.server.config.UsernamePasswordAuth;
import io.ml.proxy.utils.io.FileUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 接收代理请求
 */
@Slf4j
public class HttpAcceptConnectHandler extends ChannelInboundHandlerAdapter {

    private final ProxyServerConfig serverConfig;
    // private final List<Object> messageQueue = new ArrayList<>();
    public static final AttributeKey<UsernamePasswordAuth> AUTH_ATTRIBUTE_KEY = AttributeKey.newInstance("usernamePasswordAuth");

    public HttpAcceptConnectHandler(ProxyServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    /**
     * 收到客户端(比如浏览器)的连接
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.debug("CONN {}", ctx);
    }

    /**
     * 处理代理连接请求
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("Read: {}\r\n{}", msg, ctx.channel());
        HttpRequest request = (HttpRequest) msg;

        if(isLocalToLocal(ctx.channel())) {
            // Response to local html
            String responseBody = "Server listening at " + ctx.channel().localAddress() + "...";
            ByteBuf buffer = ctx.alloc().buffer();
            buffer.writeCharSequence(responseBody, StandardCharsets.UTF_8);
            DefaultFullHttpResponse httpResponse = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.OK, buffer);
            ctx.channel().writeAndFlush(httpResponse);
            return;
        }

        String proxyAuthorization = request.headers().get(HttpHeaderNames.PROXY_AUTHORIZATION.toString());
        if(serverConfig.isNeedAuthorization()) {
            if(proxyAuthorization == null || proxyAuthorization.isEmpty()) {
                log.debug("Please provide Proxy-Authorization\r\n{}", ctx);
                response407ProxyAuthenticationRequired(ctx.channel(), request.protocolVersion(), "Please provide Proxy-Authorization")
                        .addListener(ChannelFutureListener.CLOSE);
                return;
            }

            UsernamePasswordAuth usernamePasswordAuth = serverConfig.getUsernamePasswordAuth(proxyAuthorization);
            if(usernamePasswordAuth == null) {
                log.debug("Incorrect proxy username or password\r\n{}", ctx);
                response407ProxyAuthenticationRequired(ctx.channel(), request.protocolVersion(), "Incorrect proxy username or password")
                        .addListener(ChannelFutureListener.CLOSE);
                return;
            }


            // 保存用户名密码到当前channelContext
            Attribute<UsernamePasswordAuth> attribute = ctx.channel().attr(AUTH_ATTRIBUTE_KEY);
            attribute.set(usernamePasswordAuth);
        }

        // 移除自己
        ctx.pipeline().remove(ctx.name());
        ctx.fireChannelRead(msg);
    }

    private boolean isLocalToLocal(Channel channel) {
        InetSocketAddress localSocketAddress = (InetSocketAddress) channel.localAddress();
        InetSocketAddress remoteSocketAddress = (InetSocketAddress) channel.remoteAddress();
        return localSocketAddress.getHostName().equals(remoteSocketAddress.getHostName());
    }


    public static ChannelFuture response200ProxyEstablished(Channel ch, HttpVersion httpVersion) {
        return response200ProxyEstablished(ch, httpVersion, null);
    }
    public static ChannelFuture response200ProxyEstablished(Channel ch, HttpVersion httpVersion, ChannelPromise proxyServerChannelPromise) {
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(httpVersion,
                new HttpResponseStatus(HttpResponseStatus.OK.code(), "Connection Established"));
        return proxyServerChannelPromise == null ? ch.writeAndFlush(fullHttpResponse) : ch.writeAndFlush(fullHttpResponse, proxyServerChannelPromise);
    }

    public static ChannelFuture response407ProxyAuthenticationRequired(Channel ch, HttpVersion httpVersion, String reasonPhrase) {
        FullHttpResponse fullHttpResponse = new DefaultFullHttpResponse(httpVersion,
                new HttpResponseStatus(HttpResponseStatus.PROXY_AUTHENTICATION_REQUIRED.code(),
                        reasonPhrase)
        );
        fullHttpResponse.headers().set(HttpHeaderNames.PROXY_AUTHENTICATE, "Basic realm=\"Access to the staging site\"");
        return ch.writeAndFlush(fullHttpResponse);
    }
}
