package io.ml.proxy.server;

import io.ml.proxy.server.config.ProxyServerConfig;
import io.ml.proxy.server.handler.ProxyUnificationServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
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
            log.debug("Relay server bind to port: {}, proxy protocol: {}, relay protocol: {}", serverConfig.getPort(), serverConfig.getProxyProtocols(), serverConfig.getRelayServerConfig().getRelayProtocol());
        }
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                // .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws CertificateException, SSLException {
                        ch.pipeline()
                            // .addLast(new LoggingHandler())
                                // Time out process
                            // .addLast(new IdleStateHandler(3, 30, 0))
                            .addLast(new ProxyUnificationServerHandler(serverConfig));
                    }
                }).bind(serverConfig.getPort());
    }

    public boolean isRunning() {
        return running.get();
    }
}
