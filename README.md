## 代理模式
### 开启代理服务器：
1. 端口绑定为40000
2. 该端口同时支持HTTPS、HTTP、SOCKS5代理协议
3. 代理用户名密码: auh / 123123
```java
// 支持HTTP、HTTPS、SOCKS5代理协议, 自动识别
HttpProxyServer proxyServer = new HttpProxyServer();
ProxyServerConfig httpProxyServerConfig = new ProxyServerConfig();
httpProxyServerConfig.setProxyProtocols(Arrays.asList(ProxyProtocolEnum.HTTP,
        ProxyProtocolEnum.HTTPS,
        ProxyProtocolEnum.SOCKS5));
httpProxyServerConfig.setCodecMsg(false);
httpProxyServerConfig.setPort(40000);
httpProxyServerConfig.setUsernamePasswordAuth(new UsernamePasswordAuth("auh", "123123"));
httpProxyServerConfig.setBossGroupThreads(5);
httpProxyServerConfig.setWorkerGroupThreads(10);

proxyServer.start(httpProxyServerConfig);
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


## 中继模式
中继模式下， 只需额外配置RelayServerConfig即可， 通过中继模式， 可以实现代理协议转换、无密码代理等。

使用场景： Chrome浏览器只支持无密码socks5代理， 可以通过中继模式， 将socks5代理暴露为HTTP代理或无密码的socks5代理
```java
// 打开中继服务器
// 支持HTTP、HTTPS 代理协议, 可中继到HTTP、HTTPS、SOCKS5代理服务器
HttpProxyServer proxyServer = new HttpProxyServer();
// 暴露HTTP、HTTPS代理服务协议， 后续也会支持SOCKS5
ProxyServerConfig httpProxyServerConfig = new ProxyServerConfig();
httpProxyServerConfig.setProxyProtocols(Arrays.asList(ProxyProtocolEnum.HTTP,
    ProxyProtocolEnum.HTTPS
    // TODO Socks5中继代理开发中
    // ProxyProtocolEnum.SOCKS5
));
httpProxyServerConfig.setCodecMsg(false);
httpProxyServerConfig.setPort(40001);
httpProxyServerConfig.setUsernamePasswordAuth(new UsernamePasswordAuth("auh", "456789"));
httpProxyServerConfig.setBossGroupThreads(5);
httpProxyServerConfig.setWorkerGroupThreads(10);

// 配置真实代理服务器， 中继到SOCKS5服务
RelayServerConfig relayServerConfig = new RelayServerConfig();
relayServerConfig.setRelayProtocol(ProxyProtocolEnum.SOCKS5);
relayServerConfig.setRelayNetAddress(new NetAddress("127.0.0.1", 40000));
relayServerConfig.setRelayUsernamePasswordAuth(new UsernamePasswordAuth("auh", "123123"));
httpProxyServerConfig.setRelayServerConfig(relayServerConfig);

// 启动中继服务
proxyServer.start(httpProxyServerConfig);
```
#### 中继测试
```shell
# HTTP代理协议测试
curl --insecure -v -x http://auh:123123@127.0.0.1:40001 https://ipinfo.io

# HTTPS代理协议测试   --proxy-insecure 跳过https代理证书校验
curl --proxy-insecure -v -x https://auh:123123@127.0.0.1:40001 https://ipinfo.io -k

# socks5代理协议测试
curl -v -x socks5://auh:123123@127.0.0.1:40001 https://ipinfo.io
```
ipinfo将获取到RelayServerConfig配置的代理服务器IP