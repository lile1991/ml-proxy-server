package io.ml;

import io.ml.proxy.server.HttpProxyServer;
import io.ml.proxy.server.config.*;
import io.ml.proxy.utils.net.LocaleInetAddresses;
import io.ml.proxy.utils.net.Proxy;
import io.ml.proxy.utils.net.ProxyConfigList;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class HttpProxyServerStartup {
    public static void main(String[] args) {
        int portBegin = 40000;
        // 打开代理服务器
        // 支持HTTP、HTTPS、SOCKS5代理协议, 自动识别
        {
            HttpProxyServer httpProxyServer = new HttpProxyServer();
            ProxyServerConfig httpProxyServerConfig = new ProxyServerConfig();
            httpProxyServerConfig.setProxyProtocols(Arrays.asList(ProxyProtocolEnum.HTTP,
                    ProxyProtocolEnum.HTTPS,
                    // Socks4a暂时没实现
                    // ProxyProtocolEnum.SOCKS4a,
                    ProxyProtocolEnum.SOCKS5));
            httpProxyServerConfig.setCodecMsg(false);
            httpProxyServerConfig.setPort(portBegin ++);
            httpProxyServerConfig.setUsernamePasswordAuth(new UsernamePasswordAuth("auh", "123123"));
            httpProxyServerConfig.setBossGroupThreads(5);
            httpProxyServerConfig.setWorkerGroupThreads(10);

            httpProxyServer.start(httpProxyServerConfig);
        }

        // 中继代理
        // 支持HTTP、HTTPS 代理协议, 可中继到HTTP、HTTPS、SOCKS5代理服务器
        {
            List<Proxy> proxies = ProxyConfigList.getProxies();
            for (Proxy proxy : proxies) {
                HttpProxyServer httpProxyServer = new HttpProxyServer();
                // 配置代理服务器， 支持HTTP、HTTPS协议， 后续也会支持SOCKS5
                ProxyServerConfig httpProxyServerConfig = new ProxyServerConfig();
                httpProxyServerConfig.setProxyProtocols(Arrays.asList(ProxyProtocolEnum.HTTP,
                        ProxyProtocolEnum.HTTPS
                        // TODO Socks5中继代理开发中
                        // ProxyProtocolEnum.SOCKS5
                ));
                httpProxyServerConfig.setCodecMsg(false);
                httpProxyServerConfig.setPort(portBegin++);
                httpProxyServerConfig.setUsernamePasswordAuth(new UsernamePasswordAuth("auh", "456789"));
                httpProxyServerConfig.setBossGroupThreads(5);
                httpProxyServerConfig.setWorkerGroupThreads(10);

                // 配置真实代理服务器， 中继到SOCKS5服务
                RelayServerConfig relayServerConfig = new RelayServerConfig();
                relayServerConfig.setRelayProtocol(proxy.getProtocol());
                relayServerConfig.setRelayNetAddress(new NetAddress(proxy.getHost(), proxy.getPort()));
                relayServerConfig.setRelayUsernamePasswordAuth(new UsernamePasswordAuth(proxy.getUsername(), proxy.getPassword()));
                httpProxyServerConfig.setRelayServerConfig(relayServerConfig);

                // 启动中继服务
                httpProxyServer.start(httpProxyServerConfig);
            }
        }

        // 多IP出口
        InetAddress[] inetAddresses = LocaleInetAddresses.getInetAddresses();
        if(inetAddresses != null) {
            Random random = new Random();
            for (InetAddress inetAddress : inetAddresses) {
                HttpProxyServer httpProxyServer = new HttpProxyServer();
                ProxyServerConfig httpProxyServerConfig = new ProxyServerConfig();
                httpProxyServerConfig.setProxyProtocols(Arrays.asList(ProxyProtocolEnum.HTTP, ProxyProtocolEnum.HTTPS/*, ProxyProtocolEnum.LEE*/));
                httpProxyServerConfig.setCodecMsg(false);
                httpProxyServerConfig.setPort(portBegin++);
                httpProxyServerConfig.setBossGroupThreads(5);
                httpProxyServerConfig.setWorkerGroupThreads(10);
                httpProxyServerConfig.setLocalAddress(new InetSocketAddress(inetAddress, 45000 + random.nextInt(5000)));
                httpProxyServer.start(httpProxyServerConfig);
            }
        }
    }
}
