package io.ml.proxy.config.properties;

import io.ml.proxy.server.config.EncryptionProtocolEnum;
import io.ml.proxy.server.config.ProxyProtocolEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;

@Setter
@Getter
@ToString
public class ProxyServerProperties {
    protected List<ProxyProtocolEnum> proxyProtocols = Collections.singletonList(ProxyProtocolEnum.HTTP);
    /** 加密协议 */
    private EncryptionProtocolEnum encryptionProtocol;
    private Integer port;
    private List<ProxyProperties> proxyPropertiesList;
    private List<RelayProperties> relayPropertiesList;
}
