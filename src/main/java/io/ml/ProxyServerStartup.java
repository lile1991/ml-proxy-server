package io.ml;

import io.ml.proxy.server.ProxyServer;
import io.ml.proxy.server.config.*;
import io.ml.proxy.utils.net.LocaleInetAddresses;
import io.ml.proxy.utils.net.Proxy;
import io.ml.proxy.utils.net.ProxyConfigList;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ProxyServerStartup {
    public static void main(String[] args) {
        int portBegin = 40000;
        // 打开代理服务器
        // 支持HTTP、HTTPS、SOCKS5代理协议, 自动识别
        {
            ProxyServer proxyServer = new ProxyServer();
            ProxyServerConfig proxyServerConfig = new ProxyServerConfig();
            proxyServerConfig.setProxyProtocols(Arrays.asList(ProxyProtocolEnum.HTTP,
                    ProxyProtocolEnum.HTTPS,
                    // Socks4a暂时没实现
                    // ProxyProtocolEnum.SOCKS4a,
                    ProxyProtocolEnum.SOCKS5));
            proxyServerConfig.setCodecMsg(false);
            proxyServerConfig.setPort(portBegin ++);
            proxyServerConfig.setUsernamePasswordAuth(new UsernamePasswordAuth("hl", "hl888888"));
            proxyServerConfig.setBossGroupThreads(5);
            proxyServerConfig.setWorkerGroupThreads(10);

            proxyServer.start(proxyServerConfig);
        }   // End 启动 HTTP/HTTPS/SOCKS5 代理服务器


        // 如果有配置代理服务器， 就中继过去
        // 支持HTTP、HTTPS 代理协议, 可中继到HTTP、HTTPS、SOCKS5代理服务器
        List<Proxy> proxies = ProxyConfigList.getProxies();
        for (Proxy proxy : proxies) {
            startRelayServer(proxy, portBegin++);
        }   // End 中继服务器

        {
            ProxyServer proxyServer = new ProxyServer();
            ProxyServerConfig proxyServerConfig = new ProxyServerConfig();
            proxyServerConfig.setProxyProtocols(Arrays.asList(ProxyProtocolEnum.HTTP,
                    ProxyProtocolEnum.HTTPS,
                    // Socks4a暂时没实现
                    // ProxyProtocolEnum.SOCKS4a,
                    ProxyProtocolEnum.SOCKS5));
            // Encryption proxy protocol.
            proxyServerConfig.setEncryptionProtocol(EncryptionProtocolEnum.MinusOne);
            proxyServerConfig.setCodecMsg(false);
            proxyServerConfig.setPort(portBegin ++);
            proxyServerConfig.setUsernamePasswordAuth(new UsernamePasswordAuth("hw", "hw888888"));
            proxyServerConfig.setBossGroupThreads(5);
            proxyServerConfig.setWorkerGroupThreads(10);
            proxyServer.start(proxyServerConfig);

            // 中继到本地代理
            startRelayServer(new Proxy(ProxyProtocolEnum.HTTP, proxyServerConfig.getEncryptionProtocol(), "127.0.0.1", proxyServerConfig.getPort(),
                    proxyServerConfig.getUsernamePasswordAuth().getUsername(), proxyServerConfig.getUsernamePasswordAuth().getPassword()), portBegin ++);
        }   // End 启动加密代理服务


        // 主机有多个IP时， 分别开启不同端口代理到不同IP
        /*InetAddress[] inetAddresses = LocaleInetAddresses.getInetAddresses();
        if(inetAddresses != null) {
            Random random = new Random();
            for (InetAddress inetAddress : inetAddresses) {
                ProxyServer proxyServer = new ProxyServer();
                ProxyServerConfig proxyServerConfig = new ProxyServerConfig();
                proxyServerConfig.setProxyProtocols(Arrays.asList(ProxyProtocolEnum.HTTP, ProxyProtocolEnum.HTTPS, ProxyProtocolEnum.SOCKS5));
                proxyServerConfig.setCodecMsg(false);
                proxyServerConfig.setPort(portBegin++);
                proxyServerConfig.setBossGroupThreads(5);
                proxyServerConfig.setWorkerGroupThreads(10);
                proxyServerConfig.setLocalAddress(new InetSocketAddress(inetAddress, 45000 + random.nextInt(5000)));
                proxyServer.start(proxyServerConfig);
            }
        }*/
    }

    private static void startRelayServer(Proxy proxy, int localPort) {
        ProxyServer proxyServer = new ProxyServer();
        // 配置代理服务器， 支持HTTP、HTTPS协议， 后续也会支持SOCKS5
        ProxyServerConfig proxyServerConfig = new ProxyServerConfig();
        proxyServerConfig.setProxyProtocols(Arrays.asList(ProxyProtocolEnum.HTTP,
                ProxyProtocolEnum.HTTPS
                // TODO Socks5中继代理开发中
                // ProxyProtocolEnum.SOCKS5
        ));
        proxyServerConfig.setCodecMsg(false);
        proxyServerConfig.setPort(localPort);
        proxyServerConfig.setUsernamePasswordAuth(new UsernamePasswordAuth("hl", "888888"));
        proxyServerConfig.setBossGroupThreads(5);
        proxyServerConfig.setWorkerGroupThreads(10);

        // 配置真实代理服务器， 中继到SOCKS5服务
        RelayServerConfig relayServerConfig = new RelayServerConfig();
        relayServerConfig.setRelayProtocol(proxy.getProxyProtocol());
        relayServerConfig.setEncryptionProtocol(proxy.getEncryptionProtocol());
        relayServerConfig.setRelayNetAddress(new NetAddress(proxy.getHost(), proxy.getPort()));
        relayServerConfig.setRelayUsernamePasswordAuth(new UsernamePasswordAuth(proxy.getUsername(), proxy.getPassword()));

        ReplayRuleConfig replayRuleConfig = new ReplayRuleConfig();
        replayRuleConfig.setDirectHosts(Arrays.asList("weixin", "qq", "tencent", "alibaba", "aliyun", "microsoft", "baidu", "hao123"));
        relayServerConfig.setReplayRuleConfig(replayRuleConfig);

        proxyServerConfig.setRelayServerConfig(relayServerConfig);

        // 启动中继服务
        proxyServer.start(proxyServerConfig);
    }
}
