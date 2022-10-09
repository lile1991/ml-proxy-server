package io.ml.proxy.config.properties;

import io.ml.proxy.server.config.UsernamePasswordAuth;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class ProxyProperties {
    /** 代理用户名密码 */
    private UsernamePasswordAuth usernamePasswordAuth;
    private String host;
}
