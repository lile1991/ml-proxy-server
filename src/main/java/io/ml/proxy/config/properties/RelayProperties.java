package io.ml.proxy.config.properties;

import io.ml.proxy.server.config.EncryptionProtocolEnum;
import io.ml.proxy.server.config.NetAddress;
import io.ml.proxy.server.config.ProxyProtocolEnum;
import io.ml.proxy.server.config.UsernamePasswordAuth;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class RelayProperties {
    /** 代理用户名密码 */
    private UsernamePasswordAuth usernamePasswordAuth;

    /** 中继协议 */
    private ProxyProtocolEnum relayProtocol;

    /** 加密协议 */
    private EncryptionProtocolEnum encryptionProtocol;

    /** 中继地址 */
    private NetAddress relayNetAddress;
    /** 中继鉴权 */
    private UsernamePasswordAuth relayUsernamePasswordAuth;
}
