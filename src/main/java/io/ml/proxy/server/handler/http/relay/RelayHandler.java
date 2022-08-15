package io.ml.proxy.server.handler.http.relay;

import io.ml.proxy.server.config.*;
import io.ml.proxy.server.handler.ExchangeHandler;
import io.ml.proxy.server.handler.http.HttpRequestInfo;
import io.ml.proxy.server.handler.http.relay.http.HttpRelayInitHandler;
import io.ml.proxy.server.handler.http.relay.socks5.Socks5RelayInitHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialRequest;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Relay中继， 连接到另一个HTTP/HTTPS/SOCKS5代理
 */
@Slf4j
public class RelayHandler extends ChannelInboundHandlerAdapter {

    HttpRequestInfo httpRequestInfo;
    final ProxyServerConfig serverConfig;

    public RelayHandler(ProxyServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    /**
     * 连接到远端代理机器
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        HttpRequest request = (HttpRequest) msg;

        RelayServerConfig relayServerConfig = serverConfig.getRelayServerConfig();
        NetAddress relayNetAddress = relayServerConfig.getRelayNetAddress();

        ctx.pipeline().remove(ctx.name());

        // 连接目标代理并响应200
        if(request.method() == HttpMethod.CONNECT) {
            connectTargetProxy(ctx, request).addListener((ChannelFutureListener) future -> {
                if(future.isSuccess()) {
                    Channel clientChannel = future.channel();
                    // 连接成功
                    log.debug("Successfully connected to {}!\r\n{}", clientChannel.remoteAddress(), clientChannel);

                    switch (serverConfig.getRelayServerConfig().getRelayProtocol()) {
                        case HTTP:
                        case HTTPS: {
                            // 设置远程代理服务器密码
                            UsernamePasswordAuth relayUsernamePasswordAuth = relayServerConfig.getRelayUsernamePasswordAuth();
                            if (relayUsernamePasswordAuth == null) {
                                request.headers().remove(HttpHeaderNames.PROXY_AUTHORIZATION.toString());
                            } else {
                                request.headers().set(HttpHeaderNames.PROXY_AUTHORIZATION.toString(),
                                        "Basic " + Base64.getEncoder().encodeToString(
                                                (relayUsernamePasswordAuth.getUsername() + ":" + relayUsernamePasswordAuth.getPassword()).getBytes(StandardCharsets.UTF_8)
                                        )
                                );
                            }

                            // 发送Connection请求到目标服务器, HTTPS握手交给{HttpsConnectedToShakeHandsHandler}做
                            log.debug("Write CONNECT request: {}\r\n{}", request, clientChannel);
                            clientChannel.writeAndFlush(request);
                            return;
                        }
                        case SOCKS5: {
                            // Socks5 initial request
                            UsernamePasswordAuth relayUsernamePasswordAuth = relayServerConfig.getRelayUsernamePasswordAuth();
                            DefaultSocks5InitialRequest socks5InitialRequest = new DefaultSocks5InitialRequest(relayUsernamePasswordAuth == null ? Socks5AuthMethod.NO_AUTH : Socks5AuthMethod.PASSWORD);
                            log.debug("Write socks5InitialRequest to {}\r\b{}", clientChannel.remoteAddress(), clientChannel);
                            clientChannel.writeAndFlush(socks5InitialRequest);
                            return;
                        }
                    }
                } else {
                    log.error("Failed connect to {}:{}\r\b{}", relayNetAddress.getRemoteHost(), relayNetAddress.getRemotePort(), ctx);
                    if (ctx.channel().isActive()) {
                        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                    } else {
                        ctx.close();
                    }
                }
            });

            return;
        }

        // 连接目标代理
        connectTargetProxy(ctx, request).addListener((ChannelFutureListener) future -> {
            if(future.isSuccess()) {
                Channel clientChannel = future.channel();
                log.debug("Successfully connected to {}!\r\n{}", clientChannel.remoteAddress(), clientChannel);

                // 添加Exchange
                ctx.pipeline().addLast(new ExchangeHandler(serverConfig, clientChannel));
                log.debug("Add ProxyExchangeHandler to proxy server pipeline.");

                switch (serverConfig.getRelayServerConfig().getRelayProtocol()) {
                    case HTTP:
                    case HTTPS: {
                        // 设置远程代理服务器密码
                        UsernamePasswordAuth relayUsernamePasswordAuth = relayServerConfig.getRelayUsernamePasswordAuth();
                        if (relayUsernamePasswordAuth == null) {
                            request.headers().remove(HttpHeaderNames.PROXY_AUTHORIZATION.toString());
                        } else {
                            request.headers().set(HttpHeaderNames.PROXY_AUTHORIZATION.toString(),
                                    "Basic " + Base64.getEncoder().encodeToString(
                                            (relayUsernamePasswordAuth.getUsername() + ":" + relayUsernamePasswordAuth.getPassword()).getBytes(StandardCharsets.UTF_8)
                                    )
                            );
                        }

                        // 转发消息给目标代理
                        log.debug("Write msg to {}", request.method() + " " + request.uri());
                        clientChannel.writeAndFlush(request);
                        return;
                    }
                    case SOCKS5: {
                        // Socks5 initial request
                        UsernamePasswordAuth relayUsernamePasswordAuth = relayServerConfig.getRelayUsernamePasswordAuth();
                        DefaultSocks5InitialRequest socks5InitialRequest = new DefaultSocks5InitialRequest(relayUsernamePasswordAuth == null ? Socks5AuthMethod.NO_AUTH : Socks5AuthMethod.PASSWORD);
                        log.debug("Write socks5InitialRequest to {}", clientChannel.remoteAddress());
                        clientChannel.writeAndFlush(socks5InitialRequest);
                        return;
                    }
                }
            } else {
                log.error("Failed connect to {}:{}\r\b{}", relayNetAddress.getRemoteHost(), relayNetAddress.getRemotePort(), ctx);
                if (ctx.channel().isActive()) {
                    ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                } else {
                    ctx.close();
                }
            }
        });
    }

    /**
     * 连接到目标代理， 支持HTTP与HTTPS协议
     */
    private ChannelFuture connectTargetProxy(ChannelHandlerContext ctx, HttpRequest request) {
        httpRequestInfo = new HttpRequestInfo(request);
        RelayServerConfig relayServerConfig = serverConfig.getRelayServerConfig();
        ProxyProtocolEnum relayProtocol = relayServerConfig.getRelayProtocol();


        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 45 * 1000)
                // Bind remote ip and port
                // .localAddress(serverIp, randomSystemPort)
                // Bind local ip and port
                // .remoteAddress(serverIp, randomSystemPort)
                ;
        switch (relayProtocol) {
            case HTTP:
            case HTTPS: bootstrap.handler(new HttpRelayInitHandler(ctx.channel(), serverConfig, httpRequestInfo)); break;
            case SOCKS5: bootstrap.handler(new Socks5RelayInitHandler(ctx.channel(), serverConfig, httpRequestInfo)); break;
            default:
                ByteBuf responseBody = ctx.alloc().buffer();
                responseBody.writeCharSequence("Unsupported relay protocol " + relayProtocol, StandardCharsets.UTF_8);
                DefaultFullHttpResponse defaultFullHttpResponse = new DefaultFullHttpResponse(request.protocolVersion(), HttpResponseStatus.NOT_IMPLEMENTED, responseBody);
                return ctx.writeAndFlush(defaultFullHttpResponse).addListener(ChannelFutureListener.CLOSE);
        }

        if(serverConfig.getLocalAddress() != null) {
            // Bind local net address
            bootstrap.remoteAddress(serverConfig.getLocalAddress());
        }

        NetAddress relayNetAddress = relayServerConfig.getRelayNetAddress();
        return bootstrap.connect(relayNetAddress.getRemoteHost(), relayNetAddress.getRemotePort());
    }
}
