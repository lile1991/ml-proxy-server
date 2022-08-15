package io.ml.relay;

import io.ml.proxy.server.config.ProxyProtocolEnum;
import io.ml.relay.config.HttpProxyRelayServerConfig;
import io.ml.relay.handler.codec.lee.LeeServerCodec;
import io.ml.relay.handler.http.HttpRelayFilterHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class HttpProxyRelayServer {

    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 启动代理中继服务器
     *  绑定端口
     * @param serverConfig 服务器配置
     */
    public void start(HttpProxyRelayServerConfig serverConfig) {
        if(!running.compareAndSet(false, true)) {
            log.error("HTTP proxy relay server already running!");
        }

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(serverConfig.getBossGroupThreads());
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(serverConfig.getWorkerGroupThreads());
        log.debug("HTTP proxy relay server bind to port: {}", serverConfig.getPort());
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                // .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        if (serverConfig.getProxyProtocols().contains(ProxyProtocolEnum.LEE)) {
                            ch.pipeline().addLast(new LeeServerCodec());
                        }

                        ch.pipeline()
                            .addLast(new HttpServerCodec())
                            .addLast(new HttpObjectAggregator(serverConfig.getHttpObjectAggregatorMaxContentLength()))
                            .addLast(new HttpRelayFilterHandler(serverConfig))
                            // .addLast(new HttpRelayConnectionHandler(serverConfig))
                            ;
                    }
                })
                .bind(serverConfig.getPort());
    }

    public boolean isRunning() {
        return running.get();
    }
}
