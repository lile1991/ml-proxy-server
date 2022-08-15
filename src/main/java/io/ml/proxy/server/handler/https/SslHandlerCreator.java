package io.ml.proxy.server.handler.https;

import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.*;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

public class SslHandlerCreator {
    public static SslHandler forServer(ByteBufAllocator alloc) throws CertificateException, SSLException {
        // Support HTTPS proxy protocol
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        SslProvider provider =  SslProvider.isAlpnSupported(SslProvider.OPENSSL)  ? SslProvider.OPENSSL : SslProvider.JDK;
        SslContext sslCtx = SslContextBuilder
                .forServer(ssc.certificate(), ssc.privateKey())
                .protocols(SslProtocols.TLS_v1_2, SslProtocols.TLS_v1_3)
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .sslProvider(provider)
                //支持的cipher
                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .applicationProtocolConfig(new ApplicationProtocolConfig(
                        ApplicationProtocolConfig.Protocol.ALPN,
                        // 目前 OpenSsl 和 JDK providers只支持NO_ADVERTISE
                        ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                        // 目前 OpenSsl 和 JDK providers只支持ACCEPT
                        ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                        ApplicationProtocolNames.HTTP_2,
                        ApplicationProtocolNames.HTTP_1_1))
                .build();
        return sslCtx.newHandler(alloc);
    }

    public static SslHandler forClient(ByteBufAllocator alloc) throws CertificateException, SSLException {
        SslProvider provider =  SslProvider.isAlpnSupported(SslProvider.OPENSSL)  ? SslProvider.OPENSSL : SslProvider.JDK;
        SslContext sslCtx = SslContextBuilder
                .forClient()
                .protocols(SslProtocols.TLS_v1_2, SslProtocols.TLS_v1_3)
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .sslProvider(provider)
                //支持的cipher
                .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                .applicationProtocolConfig(new ApplicationProtocolConfig(
                        ApplicationProtocolConfig.Protocol.ALPN,
                        // 目前 OpenSsl 和 JDK providers只支持NO_ADVERTISE
                        ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                        // 目前 OpenSsl 和 JDK providers只支持ACCEPT
                        ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                        ApplicationProtocolNames.HTTP_2,
                        ApplicationProtocolNames.HTTP_1_1))
                .build();
        return sslCtx.newHandler(alloc);
    }
}
