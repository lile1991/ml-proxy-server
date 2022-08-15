package io.ml.proxy.server.handler.http;

import io.ml.proxy.server.config.ProxyServerConfig;
import io.ml.proxy.server.config.UsernamePasswordAuth;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 接收代理请求
 */
@Slf4j
public class HttpAcceptConnectHandler extends ChannelInboundHandlerAdapter {

    private final ProxyServerConfig serverConfig;
    // private final List<Object> messageQueue = new ArrayList<>();

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
        String proxyAuthorization = request.headers().get(HttpHeaderNames.PROXY_AUTHORIZATION.toString());
        if(serverConfig.getUsernamePasswordAuth() != null) {
            if(proxyAuthorization == null || proxyAuthorization.isEmpty()) {
                log.debug("Please provide Proxy-Authorization\r\n{}", ctx);
                response407ProxyAuthenticationRequired(ctx.channel(), request.protocolVersion(), "Please provide Proxy-Authorization")
                        .addListener(ChannelFutureListener.CLOSE);
                return;
            }

            UsernamePasswordAuth usernamePasswordAuth = serverConfig.getUsernamePasswordAuth();
            String usernamePassword = usernamePasswordAuth.getUsername() + ":" + usernamePasswordAuth.getPassword();

            if(!proxyAuthorization.equals("Basic " + Base64.getEncoder().encodeToString(usernamePassword.getBytes(StandardCharsets.UTF_8)))) {
                log.debug("Incorrect proxy username or password\r\n{}", ctx);
                response407ProxyAuthenticationRequired(ctx.channel(), request.protocolVersion(), "Incorrect proxy username or password")
                        .addListener(ChannelFutureListener.CLOSE);
                return;
            }
        }

        // 移除自己
        ctx.pipeline().remove(ctx.name());
        ctx.fireChannelRead(msg);
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
