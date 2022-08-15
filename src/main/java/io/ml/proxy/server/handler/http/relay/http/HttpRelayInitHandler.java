package io.ml.proxy.server.handler.http.relay.http;

import io.ml.proxy.server.config.ProxyProtocolEnum;
import io.ml.proxy.server.config.ProxyServerConfig;
import io.ml.proxy.server.handler.http.HttpRequestInfo;
import io.ml.proxy.server.handler.https.SslHandlerCreator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

/**
 * 与目标网站的Handler
 */
@Slf4j
public class HttpRelayInitHandler extends ChannelInitializer<Channel> {
    final Channel proxyServerChannel;
    final ProxyServerConfig serverConfig;
    final HttpRequestInfo httpRequestInfo;


    public HttpRelayInitHandler(Channel proxyServerChannel, ProxyServerConfig serverConfig, HttpRequestInfo httpRequestInfo) {
        this.proxyServerChannel = proxyServerChannel;
        this.serverConfig = serverConfig;
        this.httpRequestInfo = httpRequestInfo;
    }

    @Override
    protected void initChannel(Channel ch) throws SSLException, CertificateException {
        // ch.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
        if(serverConfig.getRelayServerConfig().getRelayProtocol() == ProxyProtocolEnum.HTTPS) {
            ch.pipeline().addLast(SslHandlerCreator.forClient(ch.alloc()));
        }
        log.debug("Add HttpClientCodec to pipeline");
        ch.pipeline().addLast(new HttpClientCodec());
        ch.pipeline().addLast(new HttpObjectAggregator(serverConfig.getHttpObjectAggregatorMaxContentLength()));

        // HTTP shake hands
        log.debug("Add ProxyExchangeHandler to pipeline");
        ch.pipeline().addLast(HttpsRelayShakeHandsHandler.class.getSimpleName(), new HttpsRelayShakeHandsHandler(serverConfig, proxyServerChannel));
    }
}
