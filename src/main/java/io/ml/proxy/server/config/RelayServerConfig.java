package io.ml.proxy.server.config;

import lombok.Getter;
import lombok.Setter;

import java.util.function.Function;

@Setter
@Getter
public class RelayServerConfig {

    /** 中继协议 */
    private ProxyProtocolEnum relayProtocol;

    /** 加密协议 */
    private EncryptionProtocolEnum encryptionProtocol;

    /** 中继地址 */
    private Function<UsernamePasswordAuth, NetAddress> relayNetAddress;

    /** 中继鉴权 */
    private UsernamePasswordAuth relayUsernamePasswordAuth;

    /** 中继器规则配置 */
    private ReplayRuleConfig replayRuleConfig = new ReplayRuleConfig();
}
