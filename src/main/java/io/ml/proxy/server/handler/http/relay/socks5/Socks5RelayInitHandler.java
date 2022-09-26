package io.ml.proxy.server.handler.http.relay.socks5;

import io.ml.proxy.server.config.ProxyServerConfig;
import io.ml.proxy.server.config.RelayConfig;
import io.ml.proxy.server.config.UsernamePasswordAuth;
import io.ml.proxy.server.handler.http.HttpAcceptConnectHandler;
import io.ml.proxy.server.handler.http.HttpRequestInfo;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.socksx.v5.Socks5ClientEncoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialResponseDecoder;
import io.netty.util.Attribute;
import lombok.extern.slf4j.Slf4j;

import static io.ml.proxy.server.ProxyServer.encryptionCodecManage;

/**
 * 与目标网站的Handler
 */
@Slf4j
public class Socks5RelayInitHandler extends ChannelInitializer<Channel> {
    private final Channel proxyServerChannel;
    private final ProxyServerConfig serverConfig;
    private final RelayConfig relayConfig;
    private final HttpRequestInfo httpRequestInfo;


    public Socks5RelayInitHandler(Channel proxyServerChannel, ProxyServerConfig serverConfig, RelayConfig relayConfig, HttpRequestInfo httpRequestInfo) {
        this.proxyServerChannel = proxyServerChannel;
        this.serverConfig = serverConfig;
        this.relayConfig = relayConfig;
        this.httpRequestInfo = httpRequestInfo;
    }

    @Override
    protected void initChannel(Channel ch) {
        log.debug("Relay to socks5 proxy server.");
        // Attribute<UsernamePasswordAuth> authAttribute = proxyServerChannel.attr(HttpAcceptConnectHandler.AUTH_ATTRIBUTE_KEY);
        // UsernamePasswordAuth usernamePasswordAuth = authAttribute.get();
        // ch.attr(HttpAcceptConnectHandler.AUTH_ATTRIBUTE_KEY).set(usernamePasswordAuth);

        // ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
        if(relayConfig.getEncryptionProtocol() != null) {
            ch.pipeline().addLast(encryptionCodecManage.newClientCodec(relayConfig.getEncryptionProtocol()));
        }
        // Socks5MessageByteBuf
        ch.pipeline().addLast(Socks5ClientEncoder.DEFAULT);
        // sock5 init
        ch.pipeline().addLast(new Socks5InitialResponseDecoder());
        // sock5 init
        ch.pipeline().addLast(new Socks5RelayInitialResponseHandler(serverConfig, relayConfig, proxyServerChannel, httpRequestInfo));
        log.debug("Add Socks5ClientEncoder to pipeline");
    }
}
