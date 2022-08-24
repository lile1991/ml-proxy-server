package io.ml.proxy.server.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

@ToString
public class ProxyServerConfig {
    /** 绑定的端口 */
    @Setter
    @Getter
    protected int port;

    /** 是否解码数据包 */
    @Setter
    @Getter
    protected boolean codecMsg = false;

    /** HTTP合包的最大大小 15MB */
    @Setter
    @Getter
    protected int httpObjectAggregatorMaxContentLength = 15 * 1024 * 128;

    @Setter
    @Getter
    protected int bossGroupThreads;
    @Setter
    @Getter
    protected int workerGroupThreads;

    @Setter
    @Getter
    private SocketAddress localAddress;

    /** 代理鉴权 */
    @Getter
    private List<UsernamePasswordAuth> usernamePasswordAuths;

    private Map<String, UsernamePasswordAuth> usernamePasswordAuthMap;

    /** 代理协议 */
    @Setter
    @Getter
    protected List<ProxyProtocolEnum> proxyProtocols = Collections.singletonList(ProxyProtocolEnum.HTTP);
    /** 加密协议 */
    @Setter
    @Getter
    private EncryptionProtocolEnum encryptionProtocol;

    /** 中继服务配置 */
    @Setter
    @Getter
    private RelayServerConfig relayServerConfig;

    public void setUsernamePasswordAuths(List<UsernamePasswordAuth> usernamePasswordAuths) {
        this.usernamePasswordAuths = usernamePasswordAuths;
        if(usernamePasswordAuths != null) {
            usernamePasswordAuthMap = new HashMap<>();
            usernamePasswordAuths.forEach(usernamePasswordAuth -> {
                String usernamePassword = usernamePasswordAuth.getUsername() + ":" + usernamePasswordAuth.getPassword();
                usernamePasswordAuthMap.put("Basic " + Base64.getEncoder().encodeToString(usernamePassword.getBytes(StandardCharsets.UTF_8)), usernamePasswordAuth);
            });
        }
    }

    public boolean isNeedAuthorization() {
        return usernamePasswordAuths != null && !usernamePasswordAuths.isEmpty();
    }

    public UsernamePasswordAuth getUsernamePasswordAuth(String basicAuthorization) {
        return usernamePasswordAuthMap == null ? null : usernamePasswordAuthMap.get(basicAuthorization);
    }
}
