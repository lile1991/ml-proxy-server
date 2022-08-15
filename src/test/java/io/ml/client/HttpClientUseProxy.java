package io.ml.client;

import io.ml.client.handler.HttpClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AsciiString;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public class HttpClientUseProxy {
    public static void main(String[] args) {
        Bootstrap bootstrap = new Bootstrap();
        NioEventLoopGroup group = new NioEventLoopGroup(5);
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                // .option(ChannelOption.AUTO_READ, false)
                .handler(new ChannelInitializer<Channel>() {

                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast("loggingHandler", new LoggingHandler(LogLevel.DEBUG));
                        ch.pipeline().addLast("httpClientCodec", new HttpClientCodec());
                        // ch.pipeline().addLast(new HttpObjectAggregator(serverConfig.getHttpObjectAggregatorMaxContentLength()));
                        ch.pipeline().addLast(new HttpClientHandler());
                    }
                });
        ChannelFuture clientChannelFuture = bootstrap.connect("sprint.ikeatw.ltd", 46000);
        // ChannelFuture clientChannelFuture = bootstrap.connect("www.google.com", 443);
        // bootstrap.resolver(NoopAddressResolverGroup.INSTANCE);
        // bootstrap.resolver(DefaultAddressResolverGroup.INSTANCE);

        // ChannelFuture clientChannelFuture = bootstrap.connect("www.baidu.com", 443);
        clientChannelFuture.addListener((ChannelFutureListener) future -> {
            DefaultFullHttpRequest connectRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, "https://www.google.com");
            connectRequest.headers().set(HttpHeaderNames.PROXY_AUTHORIZATION.toString(), "Basic " + Base64.getEncoder().encodeToString("sprint:FtMM7EvG".getBytes(StandardCharsets.UTF_8)));
            connectRequest.headers().set("Host", "www.google.com");
            connectRequest.headers().set("Proxy-Connection", "keep-alive");
            connectRequest.headers().set(HttpHeaderNames.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.69 Safari/537.36");
            connectRequest.headers().set(AsciiString.of("content-length"), "0");
            log.debug("CONNECT TO level3");
            // future.channel().config().setAutoRead(false);
            future.channel().writeAndFlush(connectRequest);
            // future.channel().pipeline().addLast(SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build().newHandler( future.channel().alloc()));
            // future.channel().config().setAutoRead(true);
            // ch.read();

            // Thread.sleep(5000);
            // future.channel().writeAndFlush(connectRequest.copy().retain());
        });
    }
}
