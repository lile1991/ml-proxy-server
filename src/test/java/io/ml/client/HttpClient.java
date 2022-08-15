package io.ml.client;

import io.ml.proxy.utils.http.HttpObjectUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HttpClient {
    public static void main(String[] args) {
        Bootstrap bootstrap = new Bootstrap();
        NioEventLoopGroup group = new NioEventLoopGroup(5);
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                // .option(ChannelOption.AUTO_READ, false)
                .handler(new ChannelInitializer<Channel>() {

                    @Override
                    protected void initChannel(Channel ch) throws Exception {

                        SslContext sslCtx = SslContextBuilder
                                .forClient()
                                .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                        // 网上大都用的这个方法创建newHandler, 但是配上cloudflare这种网站的域名会报"SSLV3_ALERT_HANDSHAKE_FAILURE"异常
                        // ch.pipeline().addFirst(sslCtx.newHandler(ch.alloc()));
                        // 改用下面这种就没问题
                        ch.pipeline().addFirst(sslCtx.newHandler(ch.alloc(), "ckadmin.grights.club", 443));

                        // 用JDK自带的SSLEngine
                        /*SSLContext sslCtx = SSLContext.getInstance("TLS");
                        sslCtx.init(null, null, null);
                        SSLEngine sslEngine = sslCtx.createSSLEngine("ckadmin.grights.club", 443);
                        sslEngine.setUseClientMode(true);
                        sslEngine.beginHandshake();
                        ch.pipeline().addFirst(new SslHandler(sslEngine));*/

                        ch.pipeline().addLast("loggingHandler", new LoggingHandler(LogLevel.DEBUG));
                        ch.pipeline().addLast("httpClientCodec", new HttpClientCodec());
                        // ch.pipeline().addLast(new HttpObjectAggregator(serverConfig.getHttpObjectAggregatorMaxContentLength()));
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                                log.debug("read from origin: \r\n{}", HttpObjectUtils.stringOf(msg));
                                /*if(msg instanceof ByteBuf) {
                                    ch.pipeline().fireChannelRead(msg);
                                }*/
                            }
                        });
                    }
                });
        ChannelFuture clientChannelFuture = bootstrap.connect("ckadmin.grights.club", 443);

        clientChannelFuture.addListener((ChannelFutureListener) future -> {
            DefaultFullHttpRequest getRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/hw-facebook-admin/pubIpInfo/");
            getRequest.headers().set("Host", "ckadmin.grights.club");
            getRequest.headers().set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");
            getRequest.headers().set("Connection", "keep-alive");
            getRequest.headers().set(HttpHeaderNames.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.69 Safari/537.36");
            log.debug("GET TO ckadmin.grights.club");
            future.channel().writeAndFlush(getRequest);
        });
    }
}
