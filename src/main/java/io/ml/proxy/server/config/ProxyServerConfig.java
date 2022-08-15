package io.ml.proxy.server.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;

@Setter
@Getter
@ToString
public class ProxyServerConfig {
    /** 绑定的端口 */
    protected int port;

    /** 是否解码数据包 */
    protected boolean codecMsg = false;

    /** HTTP合包的最大大小 15MB */
    protected int httpObjectAggregatorMaxContentLength = 15 * 1024 * 128;

    protected int bossGroupThreads;
    protected int workerGroupThreads;

    private SocketAddress localAddress;

    /** 代理鉴权 */
    private UsernamePasswordAuth usernamePasswordAuth;

    /** 代理协议 */
    protected List<ProxyProtocolEnum> proxyProtocols = Collections.singletonList(ProxyProtocolEnum.HTTP);

    /** 中继服务配置 */
    private RelayServerConfig relayServerConfig;

}
