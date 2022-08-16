package io.ml.proxy.server;

import io.ml.proxy.server.config.ProxyServerConfig;
import io.ml.proxy.server.handler.ProxyUnificationServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class HttpProxyServer {

    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 启动代理服务器
     *  绑定端口
     * @param serverConfig 服务器配置
     */
    public void start(ProxyServerConfig serverConfig) {
        if(!running.compareAndSet(false, true)) {
            log.error("Proxy server already running!");
        }

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(serverConfig.getBossGroupThreads());
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(serverConfig.getWorkerGroupThreads());
        if(serverConfig.getRelayServerConfig() == null) {
            log.debug("Proxy server bind to port: {}, protocol: {}", serverConfig.getPort(), serverConfig.getProxyProtocols());
        } else {
            log.debug("Relay server bind to port: {}, the proxy protocol: {}, relay to protocol: {}", serverConfig.getPort(), serverConfig.getProxyProtocols(), serverConfig.getRelayServerConfig().getRelayProtocol());
        }

        GlobalChannelManage globalChannelManage = new GlobalChannelManage();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                // .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline()
                            // .addLast(new LoggingHandler())
                            // .addLast(new ChannelRegisterHandler(globalChannelManage))
                            // .addLast(new ChannelTrafficShapingHandler(0, 0, 15 * 1000))
                            .addLast(new IdleStateHandler(3, 30, 0))
                            .addLast(new ProxyUnificationServerHandler(serverConfig))
                        ;
                    }
                }).bind(serverConfig.getPort());
    }

    public boolean isRunning() {
        return running.get();
    }
}
