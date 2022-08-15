package io.ml.relay.handler.http;

import io.ml.proxy.server.handler.http.HttpRequestInfo;
import io.ml.proxy.server.ssl.BouncyCastleCertificateGenerator;
import io.ml.proxy.utils.http.HttpObjectUtils;
import io.ml.relay.config.HttpProxyRelayServerConfig;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.AttributeKey;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.operator.OperatorCreationException;

import javax.net.ssl.SSLException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * 连接建立后， 由该Handler处理与浏览器的IO， HttpProxyServerConnectorHandler会从pipeline中移除
 */
@Slf4j
public class HttpRelayDispatcherHandler extends ChannelInboundHandlerAdapter {
    @Getter
    private final HttpRequestInfo httpRequestInfo;
    private final HttpProxyRelayServerConfig serverConfig;
    private final Channel realProxyChannel;
    public static final AttributeKey<Object> REQUEST_ATTRIBUTE_KEY = AttributeKey.newInstance("request");

    public HttpRelayDispatcherHandler(HttpRequestInfo httpRequestInfo, HttpProxyRelayServerConfig serverConfig, Channel realProxyChannel) {
        this.httpRequestInfo = httpRequestInfo;
        this.serverConfig = serverConfig;
        this.realProxyChannel = realProxyChannel;
    }

    /**
     * 从Browser客户端读取数据， 转发给代理服务器
     * @param ctx Browser <-> Relay Server
     * @param msg 读取到的请求 HttpRequest
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws CertificateException, CertIOException, OperatorCreationException, SSLException {
        // HTTPS请求 && 服务端配置是需要解码
        if(msg instanceof ByteBuf) {
            if(serverConfig.isCodecMsg()) {
                // 自签证书
                X509Certificate x509Certificate = BouncyCastleCertificateGenerator.generateServerCert(httpRequestInfo.getRemoteHost());
                SslContext sslCtx = SslContextBuilder
                        .forServer(BouncyCastleCertificateGenerator.serverPriKey, x509Certificate).build();
                ctx.pipeline().addFirst(new HttpObjectAggregator(serverConfig.getHttpObjectAggregatorMaxContentLength()));
                ctx.pipeline().addFirst(new HttpServerCodec());
                ctx.pipeline().addFirst(sslCtx.newHandler(ctx.alloc()));
                log.debug("{} Add ssl handler to RelayChannel, ctx.pipeline: {}", ctx.channel(), ctx.pipeline().names());

                // toProxyChannel增加SSLHandler
                SslContext clientSslCtx = SslContextBuilder
                        .forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
                realProxyChannel.pipeline().addFirst(clientSslCtx.newHandler(realProxyChannel.alloc(), httpRequestInfo.getRemoteHost(), httpRequestInfo.getRemotePort()));
                log.debug("{} Add ssl handler  to ToProxyChannel, pipeline: {}", realProxyChannel, realProxyChannel.pipeline().names());

                // 重走一遍 channelRead ， 解析HTTPS请求包
                ctx.pipeline().fireChannelRead(msg);
                return;
            }
        }

        // 如果有配置代理用户名
        if(serverConfig.getProxyUsername() != null && !serverConfig.getProxyUsername().isEmpty()) {
            // 就设置代理授权信息到消息头
            setProxyAuthorization(msg);
        }

        // 转发消息给代理服务器
        if(log.isDebugEnabled()) {
            if(serverConfig.isCodecMsg()) {
                log.debug("{} RELAY send {} to proxy: \r\n{}", realProxyChannel.toString(), msg.getClass().getSimpleName(), HttpObjectUtils.stringOf(msg));
            } else {
                log.debug("{} RELAY send {} to proxy: \r\n{}", realProxyChannel.toString(), msg.getClass().getSimpleName(), msg);
            }
        }

        ctx.channel().attr(REQUEST_ATTRIBUTE_KEY).set(msg);
        realProxyChannel.writeAndFlush(msg);
    }

    private void setProxyAuthorization(Object msg) {
        if(msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            if (request.method() == HttpMethod.CONNECT || request.uri().startsWith("http://")) {
                String proxyAuthorization = serverConfig.getProxyUsername() + ":" + serverConfig.getProxyPassword();
                request.headers().set(HttpHeaderNames.PROXY_AUTHORIZATION.toString(), "Basic " + Base64.getEncoder().encodeToString(proxyAuthorization.getBytes(StandardCharsets.UTF_8)));
                // request.headers().set("Proxy-Connection", "Keep-Alive");
            }
        }
    }
}
