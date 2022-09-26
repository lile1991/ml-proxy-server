## 代理服务器
### 开启代理服务器：
1. 端口绑定为40000

2. 该端口同时支持HTTPS、HTTP、SOCKS5代理协议

3. 代理用户名密码: auh / 123123

   ```mermaid
   graph LR;
   Client1 --http--> ProxyServer
   Client2 --https--> ProxyServer
   Client3 --socks5--> ProxyServer
   ```
```java
// 支持HTTP、HTTPS、SOCKS5代理协议, 自动识别
ProxyServer proxyServer = new ProxyServer();
ProxyServerConfig proxyServerConfig = new ProxyServerConfig();
proxyServerConfig.setProxyProtocols(Arrays.asList(ProxyProtocolEnum.HTTP,
        ProxyProtocolEnum.HTTPS,
        ProxyProtocolEnum.SOCKS5));
proxyServerConfig.setCodecMsg(false);
proxyServerConfig.setPort(40000);
// 代理服务器帐号密码
proxyServerConfig.setUsernamePasswordAuth(new UsernamePasswordAuth("auh", "123123"));
proxyServerConfig.setBossGroupThreads(5);
proxyServerConfig.setWorkerGroupThreads(10);

proxyServer.start(proxyServerConfig);
```

#### 测试
```shell
# HTTP代理协议测试
curl --insecure -v -x http://auh:123123@127.0.0.1:40000 https://ipinfo.io

# HTTPS代理协议测试   --proxy-insecure 跳过https代理证书校验
curl --proxy-insecure -v -x https://auh:123123@127.0.0.1:40000 https://ipinfo.io -k

# socks5代理协议测试
curl -v -x socks5://auh:123123@127.0.0.1:40000 https://ipinfo.io
```



## 中继服务器

可以通过中继服务为客户端提供统一的代理协议。

使用场景1： Chrome浏览器只支持无密码socks5代理， 可以通过中继服务将其暴露成http代理或无密码的socks5代理。

```mermaid
graph LR;
Client --http/https/socks5代理协议--> 中继服务器1
中继服务器1 --http--> ProxyServer1
中继服务器1 --https--> ProxyServer2
中继服务器1 --socks5--> ProxyServer3
```

中继模式下， 只需额外配置RelayServerConfig即可

```java
// 打开中继服务器
// 支持HTTP、HTTPS 代理协议, 可中继到HTTP、HTTPS、SOCKS5代理服务器
ProxyServer proxyServer = new ProxyServer();
// 暴露HTTP、HTTPS代理服务协议， 后续也会支持SOCKS5
ProxyServerConfig proxyServerConfig = new ProxyServerConfig();
proxyServerConfig.setProxyProtocols(Arrays.asList(ProxyProtocolEnum.HTTP,
    ProxyProtocolEnum.HTTPS
    // TODO Socks5中继代理开发中
    // ProxyProtocolEnum.SOCKS5
));
proxyServerConfig.setCodecMsg(false);
proxyServerConfig.setPort(40002);
// 中继服务器的用户名和密码
proxyServerConfig.setUsernamePasswordAuth(new UsernamePasswordAuth("auh", "456789"));
proxyServerConfig.setBossGroupThreads(5);
proxyServerConfig.setWorkerGroupThreads(10);

// 配置真实代理服务器， 中继到SOCKS5服务
RelayServerConfig relayConfig = new RelayServerConfig();
relayConfig.setRelayProtocol(ProxyProtocolEnum.SOCKS5);
relayConfig.setRelayNetAddress(new NetAddress("rolayxy.io", 8888));
// 真实代理服务器的用户名密码
relayConfig.setRelayUsernamePasswordAuth(new UsernamePasswordAuth("abcd", "1111111"));
proxyServerConfig.setRelayServerConfig(relayConfig);

// 启动中继服务
proxyServer.start(proxyServerConfig);
```
#### 中继测试
```shell
# HTTP代理协议测试
curl --insecure -v -x http://auh:123123@127.0.0.1:40002 https://ipinfo.io

# HTTPS代理协议测试   --proxy-insecure 跳过https代理证书校验
curl --proxy-insecure -v -x https://auh:123123@127.0.0.1:40002 https://ipinfo.io -k

# socks5代理协议测试
curl -v -x socks5://auh:123123@127.0.0.1:40002 https://ipinfo.io
```
ipinfo将获取到RelayServerConfig配置的代理服务器IP