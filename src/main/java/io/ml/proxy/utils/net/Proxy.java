package io.ml.proxy.utils.net;

import io.ml.proxy.server.config.EncryptionProtocolEnum;
import io.ml.proxy.server.config.ProxyProtocolEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Proxy {
    /** 代理协议 */
    private ProxyProtocolEnum proxyProtocol;
    private EncryptionProtocolEnum encryptionProtocol;
    /** 代理主机 */
    private String host;
    /** 代理端口 */
    private Integer port;
    /** 代理用户名 */
    private String username;
    /** 代理密码 */
    private String password;
}
