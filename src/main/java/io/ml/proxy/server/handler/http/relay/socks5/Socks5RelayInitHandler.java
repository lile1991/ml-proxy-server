package io.ml.proxy.server.handler.http.relay.socks5;

import io.ml.proxy.server.config.ProxyServerConfig;
import io.ml.proxy.server.config.UsernamePasswordAuth;
import io.ml.proxy.server.handler.codec.EncryptionCodecManage;
import io.ml.proxy.server.handler.http.HttpAcceptConnectHandler;
import io.ml.proxy.server.handler.http.HttpRequestInfo;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.socksx.v5.Socks5ClientEncoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialResponseDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * 与目标网站的Handler
 */
@Slf4j
public class Socks5RelayInitHandler extends ChannelInitializer<Channel> {
    private final Channel proxyServerChannel;
    private final ProxyServerConfig serverConfig;
    private final HttpRequestInfo httpRequestInfo;
    private final UsernamePasswordAuth relayUsernamePasswordAuth;


    public Socks5RelayInitHandler(Channel proxyServerChannel, ProxyServerConfig serverConfig, HttpRequestInfo httpRequestInfo, UsernamePasswordAuth relayUsernamePasswordAuth) {
        this.proxyServerChannel = proxyServerChannel;
        this.serverConfig = serverConfig;
        this.httpRequestInfo = httpRequestInfo;
        this.relayUsernamePasswordAuth = relayUsernamePasswordAuth;
    }

    @Override
    protected void initChannel(Channel ch) {
        log.debug("Relay to socks5 proxy server.");
        ch.attr(HttpAcceptConnectHandler.AUTH_ATTRIBUTE_KEY).set(relayUsernamePasswordAuth);

        // ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
        if(serverConfig.getRelayServerConfig().getEncryptionProtocol() != null) {
            ch.pipeline().addLast(EncryptionCodecManage.newClientCodec(serverConfig.getRelayServerConfig().getEncryptionProtocol()));
        }
        // Socks5MessageByteBuf
        ch.pipeline().addLast(Socks5ClientEncoder.DEFAULT);
        // sock5 init
        ch.pipeline().addLast(new Socks5InitialResponseDecoder());
        // sock5 init
        ch.pipeline().addLast(new Socks5RelayInitialResponseHandler(serverConfig, proxyServerChannel, httpRequestInfo));
        log.debug("Add Socks5ClientEncoder to pipeline");
    }
}
