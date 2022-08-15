package io.ml.relay.handler.http;

import io.ml.proxy.server.handler.http.HttpRequestInfo;
import io.ml.relay.config.HttpProxyRelayServerConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 中继代理服务器ConnectionHandler
 * 用于处理浏览器与代理服务器的连接， 连接成功后从pipeline中移除， 增加HttpProxyRelayServerDispatcherHandler处理浏览器的IO
 */
@Slf4j
public class HttpRelayConnectionHandler extends ChannelInboundHandlerAdapter {

    private HttpRequestInfo httpRequestInfo;
    private final HttpProxyRelayServerConfig serverConfig;
    private HttpRelayDispatcherHandler httpRelayDispatcherHandler;
    private ChannelFuture toProxyChannelFuture;

    public HttpRelayConnectionHandler(HttpProxyRelayServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    /**
     * 从客户端(比如浏览器)读数据
     * @param ctx 与客户端的连接
     * @param msg 消息 HttpConnect、HttpRequest、HttpContent、SSL请求
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if(toProxyChannelFuture == null) {
            if(msg instanceof HttpRequest) {
                // 建立或获取远端站点的连接， 转发数据
                toProxyChannelFuture = connectToProxyServer(ctx, (HttpRequest) msg);
            } else {
                log.error("收到莫名消息: {}", msg);
            }
        }
    }

    /**
     * 连接到代理服务器
     * @param ctx 与浏览器的channel
     * @param request CONNECT请求
     */
    private ChannelFuture connectToProxyServer(ChannelHandlerContext ctx, HttpRequest request) {
        httpRequestInfo = new HttpRequestInfo(request);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 45 * 1000)
                .handler(new HttpRealProxyInitHandler(ctx.channel(), serverConfig, httpRequestInfo));

        ChannelFuture realProxyChannelFuture = bootstrap.connect(serverConfig.getRealProxyHost(), serverConfig.getRealProxyPort());
        realProxyChannelFuture.addListener((ChannelFutureListener) future -> {
            if(future.isSuccess()) {
                Channel realProxyChannel = future.channel();
                // 连接成功， 移除Connector, 添加Dispatcher
                log.debug("RELAY {} successfully connected to real proxy server {}:{}!", realProxyChannel, serverConfig.getRealProxyHost(), serverConfig.getRealProxyPort());
                // 将客户端连接放到channel的AttributeMap中， 所有Handler都可以读到
                // 移除Connect
                ctx.pipeline().remove(HttpRelayConnectionHandler.class);
                // 添加Dispatcher
                httpRelayDispatcherHandler = new HttpRelayDispatcherHandler(httpRequestInfo, serverConfig, realProxyChannel);
                ctx.pipeline().addLast(httpRelayDispatcherHandler);
                log.debug("{} addLast HttpRelayDispatcherHandler", ctx.channel());

                // 消费掉消息
                log.debug("{} channel read message: {}", realProxyChannel, request);
                httpRelayDispatcherHandler.channelRead(ctx, request);
            } else {
                log.error("RELAY failed connect to real proxy server {}:{}", serverConfig.getRealProxyHost(), serverConfig.getRealProxyPort());
                if (ctx.channel().isActive()) {
                    ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                }
                ctx.close();
            }
        });
        return realProxyChannelFuture;
    }
}
