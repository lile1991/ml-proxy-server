package io.ml.proxy.server;

import io.ml.proxy.server.config.ProxyServerConfig;
import io.ml.proxy.server.handler.ProxyUnificationServerHandler;
import io.ml.proxy.server.handler.codec.EncryptionCodecManage;
import io.ml.proxy.utils.lang.StringUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ProxyServer {

    private final AtomicBoolean running = new AtomicBoolean(false);
    public static volatile EncryptionCodecManage encryptionCodecManage;

    @Setter
    private ProxyServerConfig serverConfig;

    public ProxyServer() {
    }

    public ProxyServer(ProxyServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    /**
     * 启动代理服务器
     *  绑定端口
     * @param serverConfig 服务器配置
     */
    public static void start(ProxyServerConfig serverConfig) {
        new ProxyServer(serverConfig).start();
    }

    /**
     * 启动代理服务器
     *  绑定端口
     */
    public void start() {
        if(!running.compareAndSet(false, true)) {
            log.error("Proxy server already running!");
        }

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        NioEventLoopGroup bossGroup = new NioEventLoopGroup(serverConfig.getBossGroupThreads());
        NioEventLoopGroup workerGroup = new NioEventLoopGroup(serverConfig.getWorkerGroupThreads());

        log.debug("The server bind port: {}", serverConfig.getPort());
        if(serverConfig.getRelayConfigMap() == null || serverConfig.getRelayConfigMap().isEmpty()) {
            log.debug("Proxy server, the proxy protocol: {}, encryption method: {}", serverConfig.getProxyProtocols(), serverConfig.getEncryptionProtocol());
        } else {
            log.debug("Relay server, " +
                            "the proxy protocol: {}, encryption method: {}; " +
                            "the relay config: {}",
                    serverConfig.getProxyProtocols(), serverConfig.getEncryptionProtocol(),
                    StringUtils.abbreviate(serverConfig.getRelayConfigMap().toString(), 120));
        }

        if(encryptionCodecManage == null) {
            synchronized (this) {
                if(encryptionCodecManage == null) {
                    ServiceLoader<EncryptionCodecManage> encryptionCodecManageServiceLoader = ServiceLoader.load(EncryptionCodecManage.class);
                    encryptionCodecManage = encryptionCodecManageServiceLoader.iterator().hasNext() ? encryptionCodecManageServiceLoader.iterator().next() : new EncryptionCodecManage();
                }
            }
        }

        // GlobalChannelManage globalChannelManage = new GlobalChannelManage();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                // .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        if(serverConfig.getEncryptionProtocol() != null) {
                            log.debug("Added encryption codec to {}", ch);
                            ch.pipeline().addLast(encryptionCodecManage.newServerCodec(serverConfig.getEncryptionProtocol()));
                        }

                        ch.pipeline()
                            // .addLast(new LoggingHandler())
                            // .addLast(new ChannelRegisterHandler(globalChannelManage))
                            // .addLast(new ChannelTrafficShapingHandler(0, 0, 15 * 1000))
                            .addLast(new IdleStateHandler(3, 30, 0))
                            .addLast(new ProxyUnificationServerHandler(serverConfig))
                        ;
                    }
                }).bind(new InetSocketAddress(serverConfig.getPort()));
    }

    public boolean isRunning() {
        return running.get();
    }
}
