package io.ml.proxy.config.properties;

import io.ml.proxy.server.config.ProxyProtocolEnum;
import io.ml.proxy.server.config.UsernamePasswordAuth;
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
    private String host;
    private int port;
    /** 代理用户名密码 */
    UsernamePasswordAuth usernamePasswordAuth;

    private List<RelayProperties> relayPropertiesList;
}
