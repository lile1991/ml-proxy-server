package io.ml.proxy.server.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class ReplayRuleConfig {
    private ProxyMode proxyMode = ProxyMode.DEFAULT;
    // 直连
    private List<String> directHosts;

    // 代理
    private List<String> proxyHosts;

    @AllArgsConstructor
    public enum ProxyMode {
        // 只代理白名单
        ONLY("ONLY", "Proxy only specified hosts"),
        // 默认
        DEFAULT("DEFAULT", "Default");

        public final String value;
        public final String desc;

        public static ProxyMode enumOf(String value) {
            ProxyMode[] proxyModes = ProxyMode.values();
            for(ProxyMode proxyMode: proxyModes) {
                if(proxyMode.value.equals(value)) {
                    return proxyMode;
                }
            }
            return null;
        }

        public static ProxyMode enumOfDesc(String desc) {
            ProxyMode[] proxyModes = ProxyMode.values();
            for(ProxyMode proxyMode: proxyModes) {
                if(proxyMode.desc.equals(desc)) {
                    return proxyMode;
                }
            }
            return null;
        }
    }
}
