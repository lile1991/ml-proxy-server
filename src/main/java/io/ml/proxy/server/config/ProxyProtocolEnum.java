package io.ml.proxy.server.config;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ProxyProtocolEnum {
    HTTP("HTTP"), HTTPS("HTTPS")/*, SOCKS4a*/, SOCKS5(new String[]{"SOCKS5", "SOCKS"});

    public final String[] alias;

    ProxyProtocolEnum(String alias) {
        this.alias = new String[]{alias};
    }

    public static ProxyProtocolEnum enumOf(String value) {
        ProxyProtocolEnum[] values = ProxyProtocolEnum.values();
        for(ProxyProtocolEnum v: values) {
            for(String a: v.alias) {
                if(a.equals(value)) {
                    return v;
                }
            }
        }
        return null;
    }
}
